package edu.ucsb.cs156.dining;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit.Stubbing;

/**
 * WireMock stubs for the UCSB Developer API endpoints this app calls, so that end-to-end tests can
 * run without a UCSB API key and without reaching api.ucsb.edu.
 *
 * <p>The response bodies are under src/test/resources/__files/ucsb-dining-api/. To keep tests
 * simple, every dining commons serves the same meals on every date, and every meal serves the same
 * menu items.
 */
public class UCSBDiningApiMocks {

  /**
   * Port for the WireMock server that stands in for the UCSB Developer API. It must match
   * app.ucsb.api.host in application-integration.properties, and must differ from the port of the
   * OAuth WireMock server (8090).
   */
  public static final int PORT = 8091;

  /**
   * Sets up the stubs for the UCSB Developer API
   *
   * @param s an instance of a WireMockServer or WireMockExtension
   */
  public static void setupUCSBDiningApiMocks(Stubbing s) {
    // DiningCommonsService: {apiHost}/dining/commons/v1/
    s.stubFor(
        get(urlPathEqualTo("/dining/commons/v1/")).willReturn(jsonBodyFile("dining-commons.json")));

    // UCSBDiningMenuService: {apiHost}/dining/menu/v1/{date-time}/{dining-common-code}
    s.stubFor(
        get(urlPathMatching("/dining/menu/v1/[^/]+/[^/]+")).willReturn(jsonBodyFile("meals.json")));

    // UCSBDiningMenuItemsService:
    // {apiHost}/dining/menu/v1/{date-time}/{dining-common-code}/{meal-code}
    s.stubFor(
        get(urlPathMatching("/dining/menu/v1/[^/]+/[^/]+/[^/]+"))
            .willReturn(jsonBodyFile("menu-items.json")));
  }

  private static ResponseDefinitionBuilder jsonBodyFile(String fileName) {
    return aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBodyFile("ucsb-dining-api/" + fileName);
  }
}
