package edu.ucsb.cs156.dining.services;

import edu.ucsb.cs156.dining.entities.Moderator;
import edu.ucsb.cs156.dining.entities.Review;
import edu.ucsb.cs156.dining.repositories.ModeratorRepository;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Emails all moderators when a review needs moderation.
 *
 * <p>The email is sent through the Gmail API from the Google account of the logged-in user who
 * submitted the review, using the OAuth access token they got when they logged in. This requires
 * the gmail.send scope (see docs/oauth.md).
 *
 * <p>Sending is best effort: every failure is logged, and {@link #notifyModerators(Review)} never
 * throws, so a problem with email can never stop a review from being saved.
 */
@Slf4j
@Service
public class ModeratorNotificationService {

  public static final String GMAIL_SEND_URL =
      "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";

  public static final String GMAIL_SEND_SCOPE = "https://www.googleapis.com/auth/gmail.send";

  public static final String SUBJECT = "A new dining review needs moderation";

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

  private final OAuth2AuthorizedClientService authorizedClientService;
  private final ModeratorRepository moderatorRepository;
  private final RestTemplate restTemplate;

  // On dokku the app's address is <app name>.<dokku host>, e.g. dining-qa.dokku-00.cs.ucsb.edu
  @Value("${app.name}")
  private String appName;

  @Value("${app.hostname}")
  private String appHostname;

  public ModeratorNotificationService(
      OAuth2AuthorizedClientService authorizedClientService,
      ModeratorRepository moderatorRepository,
      RestTemplateBuilder restTemplateBuilder) {
    this.authorizedClientService = authorizedClientService;
    this.moderatorRepository = moderatorRepository;
    // Finite timeouts so a slow Gmail API can't hang the review submission request.
    this.restTemplate =
        restTemplateBuilder.connectTimeout(CONNECT_TIMEOUT).readTimeout(READ_TIMEOUT).build();
  }

  /**
   * Emails every moderator that the given review is waiting for moderation. Never throws.
   *
   * @param review the review that needs moderation
   */
  public void notifyModerators(Review review) {
    try {
      String accessToken = currentUserGoogleAccessToken();
      if (accessToken == null) {
        log.warn(
            "Not emailing moderators about review {}: no usable Google access token for the"
                + " current user (they may need to log out and back in)",
            review.getId());
        return;
      }

      List<String> recipients =
          StreamSupport.stream(moderatorRepository.findAll().spliterator(), false)
              .map(Moderator::getEmail)
              .filter(ModeratorNotificationService::isSafeEmail)
              .toList();
      if (recipients.isEmpty()) {
        log.info(
            "Not emailing moderators about review {}: there are no moderators", review.getId());
        return;
      }

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(accessToken);
      Map<String, String> body = Map.of("raw", buildRawMessage(recipients, review));
      restTemplate.postForEntity(GMAIL_SEND_URL, new HttpEntity<>(body, headers), String.class);
      log.info("Emailed {} moderator(s) about review {}", recipients.size(), review.getId());
    } catch (Exception e) {
      log.error("Failed to email moderators about review {}", review.getId(), e);
    }
  }

  /**
   * Returns the current user's Google access token, or null if there isn't a usable one.
   *
   * @return the access token, or null
   */
  String currentUserGoogleAccessToken() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (!(auth instanceof OAuth2AuthenticationToken oauthToken)) {
      return null;
    }
    // Only the real Google login can call the Gmail API (not the mock provider used in tests).
    if (!"google".equals(oauthToken.getAuthorizedClientRegistrationId())) {
      return null;
    }
    OAuth2AuthorizedClient client =
        authorizedClientService.loadAuthorizedClient("google", oauthToken.getName());
    if (client == null) {
      return null;
    }
    // Users who logged in before the scope was added don't have it until they log in again.
    if (!client.getAccessToken().getScopes().contains(GMAIL_SEND_SCOPE)) {
      return null;
    }
    return client.getAccessToken().getTokenValue();
  }

  /**
   * Builds the plain-text RFC 2822 email, before encoding.
   *
   * <p>Moderators go in Bcc so the student who submitted the review doesn't see their addresses.
   *
   * @param recipients moderator email addresses
   * @param review the review that needs moderation
   * @return the email as a string
   */
  String buildMessage(List<String> recipients, Review review) {
    return "Bcc: "
        + String.join(", ", recipients)
        + "\r\n"
        + "Subject: "
        + SUBJECT
        + "\r\n"
        + "Content-Type: text/plain; charset=UTF-8\r\n"
        + "\r\n"
        + "A new review (id "
        + review.getId()
        + ") was submitted and is waiting for moderation.\r\n\r\n"
        + "Review it here: "
        + moderationPageUrl()
        + "\r\n";
  }

  /**
   * Returns the URL of the moderation page on this deployment.
   *
   * @return e.g. https://dining-qa.dokku-00.cs.ucsb.edu/moderate
   */
  String moderationPageUrl() {
    return "https://" + appName + "." + appHostname + "/moderate";
  }

  /** Encodes the email as base64url without padding, which is what the Gmail API expects. */
  private String buildRawMessage(List<String> recipients, Review review) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(buildMessage(recipients, review).getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Rejects values that would break the email or inject extra headers.
   *
   * @param email a moderator email address
   * @return true if it is safe to put in the Bcc header
   */
  static boolean isSafeEmail(String email) {
    return email != null && !email.isBlank() && email.indexOf('\r') < 0 && email.indexOf('\n') < 0;
  }
}
