package edu.ucsb.cs156.dining.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.dining.entities.Moderator;
import edu.ucsb.cs156.dining.entities.Review;
import edu.ucsb.cs156.dining.repositories.ModeratorRepository;
import edu.ucsb.cs156.dining.services.wiremock.WiremockService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.client.MockRestServiceServer;

@RestClientTest(ModeratorNotificationService.class)
@AutoConfigureDataJpa
@ContextConfiguration(classes = {})
class ModeratorNotificationServiceTests {

  @MockBean private WiremockService wiremockService;

  @MockBean private OAuth2AuthorizedClientService authorizedClientService;

  @MockBean private ModeratorRepository moderatorRepository;

  @Autowired private MockRestServiceServer mockRestServiceServer;

  @Autowired private ModeratorNotificationService service;

  @Value("${app.name}")
  private String appName;

  @Value("${app.hostname}")
  private String appHostname;

  private static final String PRINCIPAL_NAME = "123456789";
  private static final String TOKEN_VALUE = "fake-access-token";

  private final ObjectMapper mapper = new ObjectMapper();

  private final Review review = Review.builder().id(42L).build();

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  // ---------- helpers ----------

  /** Puts an OAuth2 login for the given provider into the security context. */
  private void loginWithOAuth(String registrationId) {
    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
    DefaultOAuth2User user =
        new DefaultOAuth2User(authorities, Map.of("sub", PRINCIPAL_NAME), "sub");
    SecurityContextHolder.getContext()
        .setAuthentication(new OAuth2AuthenticationToken(user, authorities, registrationId));
  }

  /** Makes the authorized client service return a Google client whose token has these scopes. */
  private void givenGoogleClientWithScopes(Set<String> scopes) {
    ClientRegistration registration =
        ClientRegistration.withRegistrationId("google")
            .clientId("client-id")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost:8080/login/oauth2/code/google")
            .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
            .tokenUri("https://oauth2.googleapis.com/token")
            .build();
    OAuth2AccessToken token =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            TOKEN_VALUE,
            Instant.now(),
            Instant.now().plusSeconds(3600),
            scopes);
    OAuth2AuthorizedClient client = new OAuth2AuthorizedClient(registration, PRINCIPAL_NAME, token);
    when(authorizedClientService.loadAuthorizedClient("google", PRINCIPAL_NAME)).thenReturn(client);
  }

  /** A logged-in Google user whose token can send email. */
  private void givenUsableGoogleLogin() {
    loginWithOAuth("google");
    givenGoogleClientWithScopes(
        Set.of("openid", "email", ModeratorNotificationService.GMAIL_SEND_SCOPE));
  }

  private void givenModerators(String... emails) {
    List<Moderator> moderators = new ArrayList<>();
    for (String email : emails) {
      moderators.add(new Moderator(email));
    }
    when(moderatorRepository.findAll()).thenReturn(moderators);
  }

  /** Decodes the "raw" field of the JSON body sent to Gmail. */
  private String decodeRaw(MockClientHttpRequest request) throws IOException {
    JsonNode body = mapper.readTree(request.getBodyAsString());
    String raw = body.get("raw").asText();
    assertFalse(raw.contains("="), "raw should be base64url without padding");
    return new String(Base64.getUrlDecoder().decode(raw), StandardCharsets.UTF_8);
  }

  private String expectedMessage(String bcc) {
    return "Bcc: "
        + bcc
        + "\r\n"
        + "Subject: A new dining review needs moderation\r\n"
        + "Content-Type: text/plain; charset=UTF-8\r\n"
        + "\r\n"
        + "A new review (id 42) was submitted and is waiting for moderation.\r\n\r\n"
        + "Review it here: https://"
        + appName
        + "."
        + appHostname
        + "/moderate\r\n";
  }

  // ---------- happy path ----------

  @Test
  void sends_email_to_all_moderators_in_bcc() throws Exception {
    givenUsableGoogleLogin();
    givenModerators("mod1@ucsb.edu", "mod2@ucsb.edu");

    List<String> sentMessages = new ArrayList<>();
    mockRestServiceServer
        .expect(requestTo(ModeratorNotificationService.GMAIL_SEND_URL))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer " + TOKEN_VALUE))
        .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
        .andExpect(request -> sentMessages.add(decodeRaw((MockClientHttpRequest) request)))
        .andRespond(withSuccess("{\"id\":\"abc\"}", MediaType.APPLICATION_JSON));

    service.notifyModerators(review);

    mockRestServiceServer.verify();
    assertEquals(List.of(expectedMessage("mod1@ucsb.edu, mod2@ucsb.edu")), sentMessages);
  }

  @Test
  void skips_unsafe_moderator_emails() throws Exception {
    givenUsableGoogleLogin();
    givenModerators(
        null,
        "",
        "   ",
        "\rstarts-with-cr@ucsb.edu",
        "\nstarts-with-lf@ucsb.edu",
        "evil@ucsb.edu\r\nBcc: attacker@example.com",
        "evil2@ucsb.edu\nX-Header: x",
        "good@ucsb.edu");

    List<String> sentMessages = new ArrayList<>();
    mockRestServiceServer
        .expect(requestTo(ModeratorNotificationService.GMAIL_SEND_URL))
        .andExpect(request -> sentMessages.add(decodeRaw((MockClientHttpRequest) request)))
        .andRespond(withSuccess());

    service.notifyModerators(review);

    mockRestServiceServer.verify();
    assertEquals(List.of(expectedMessage("good@ucsb.edu")), sentMessages);
  }

  // ---------- cases where nothing should be sent ----------

  @Test
  void does_not_send_when_nobody_is_logged_in() {
    service.notifyModerators(review);

    verify(moderatorRepository, never()).findAll();
    mockRestServiceServer.verify();
  }

  @Test
  void does_not_send_when_login_is_not_oauth() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("user", "password"));

    service.notifyModerators(review);

    verify(authorizedClientService, never()).loadAuthorizedClient(anyString(), anyString());
    verify(moderatorRepository, never()).findAll();
    mockRestServiceServer.verify();
  }

  @Test
  void does_not_send_when_login_is_not_google() {
    loginWithOAuth("my-oauth-provider");

    service.notifyModerators(review);

    verify(authorizedClientService, never()).loadAuthorizedClient(anyString(), anyString());
    verify(moderatorRepository, never()).findAll();
    mockRestServiceServer.verify();
  }

  @Test
  void does_not_send_when_there_is_no_authorized_client() {
    loginWithOAuth("google");
    when(authorizedClientService.loadAuthorizedClient("google", PRINCIPAL_NAME)).thenReturn(null);

    service.notifyModerators(review);

    verify(moderatorRepository, never()).findAll();
    mockRestServiceServer.verify();
  }

  @Test
  void does_not_send_when_token_lacks_gmail_send_scope() {
    loginWithOAuth("google");
    givenGoogleClientWithScopes(Set.of("openid", "email", "profile"));

    service.notifyModerators(review);

    verify(moderatorRepository, never()).findAll();
    mockRestServiceServer.verify();
  }

  @Test
  void does_not_send_when_there_are_no_moderators() {
    givenUsableGoogleLogin();
    givenModerators();

    service.notifyModerators(review);

    verify(moderatorRepository).findAll();
    mockRestServiceServer.verify();
  }

  @Test
  void does_not_send_when_all_moderator_emails_are_unsafe() {
    givenUsableGoogleLogin();
    givenModerators(null, "", "bad@ucsb.edu\r\n");

    service.notifyModerators(review);

    mockRestServiceServer.verify();
  }

  // ---------- failures never escape ----------

  @Test
  void does_not_throw_when_gmail_returns_server_error() {
    givenUsableGoogleLogin();
    givenModerators("mod1@ucsb.edu");
    mockRestServiceServer
        .expect(requestTo(ModeratorNotificationService.GMAIL_SEND_URL))
        .andRespond(withServerError());

    service.notifyModerators(review);

    mockRestServiceServer.verify();
  }

  @Test
  void does_not_throw_when_gmail_returns_forbidden() {
    // e.g. "Gmail API has not been used in project ... or it is disabled"
    givenUsableGoogleLogin();
    givenModerators("mod1@ucsb.edu");
    mockRestServiceServer
        .expect(requestTo(ModeratorNotificationService.GMAIL_SEND_URL))
        .andRespond(withStatus(HttpStatus.FORBIDDEN));

    service.notifyModerators(review);

    mockRestServiceServer.verify();
  }

  @Test
  void does_not_throw_when_repository_throws() {
    givenUsableGoogleLogin();
    when(moderatorRepository.findAll()).thenThrow(new RuntimeException("database is down"));

    service.notifyModerators(review);

    mockRestServiceServer.verify();
  }

  // ---------- helper methods ----------

  @Test
  void currentUserGoogleAccessToken_returns_token_when_usable() {
    givenUsableGoogleLogin();
    assertEquals(TOKEN_VALUE, service.currentUserGoogleAccessToken());
  }

  @Test
  void currentUserGoogleAccessToken_returns_null_without_login() {
    assertNull(service.currentUserGoogleAccessToken());
  }

  @Test
  void moderationPageUrl_uses_app_name_and_hostname() {
    assertEquals(
        "https://" + appName + "." + appHostname + "/moderate", service.moderationPageUrl());
  }

  @Test
  void isSafeEmail_accepts_normal_address() {
    assertTrue(ModeratorNotificationService.isSafeEmail("cgaucho@ucsb.edu"));
  }

  @Test
  void isSafeEmail_rejects_bad_values() {
    assertFalse(ModeratorNotificationService.isSafeEmail(null));
    assertFalse(ModeratorNotificationService.isSafeEmail(""));
    assertFalse(ModeratorNotificationService.isSafeEmail(" "));
    assertFalse(ModeratorNotificationService.isSafeEmail("\rx@ucsb.edu"));
    assertFalse(ModeratorNotificationService.isSafeEmail("\nx@ucsb.edu"));
    assertFalse(ModeratorNotificationService.isSafeEmail("x@ucsb.edu\r"));
    assertFalse(ModeratorNotificationService.isSafeEmail("x@ucsb.edu\n"));
  }
}
