package edu.ucsb.cs156.dining.web;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import edu.ucsb.cs156.dining.WebTestCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * End-to-end test of posting a review. The dining commons, meals and menu items come from the
 * WireMock stubs of the UCSB Developer API in UCSBDiningApiMocks.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("integration")
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class PostReviewWebIT extends WebTestCase {

  private Locator rowContaining(String text) {
    return page.getByRole(AriaRole.ROW).filter(new Locator.FilterOptions().setHasText(text));
  }

  @Test
  public void regular_user_can_post_a_review_of_a_menu_item() throws Exception {
    setupUser(false);

    // Home page: pick a meal at a dining commons from the stubbed API
    rowContaining("Carrillo")
        .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Dinner"))
        .click();

    // Menu items page: pick an item to review
    rowContaining("Chicken Tikka Masala")
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Review Item"))
        .click();

    // Post review page
    assertThat(page.getByLabel("Item Name")).hasValue("Chicken Tikka Masala");
    page.getByLabel("Comments").fill("Rich sauce and tender chicken");
    page.getByLabel("Stars (1 to 5)").selectOption("4");
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit Review")).click();

    assertThat(page.getByText("Review submitted for Chicken Tikka Masala")).isVisible();

    // My Reviews page: the review was saved, and awaits moderation since it has comments
    page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("My Reviews")).click();

    Locator review = rowContaining("Rich sauce and tender chicken");
    assertThat(review).containsText("Chicken Tikka Masala");
    assertThat(review).containsText("carrillo");
    assertThat(review).containsText("AWAITING_REVIEW");
    assertThat(page.getByTestId("Reviewstable-cell-row-0-col-itemsStars")).hasText("⭐".repeat(4));

    // The menu came from the WireMock server, not the real UCSB API
    ucsbApiWireMockServer.verify(
        getRequestedFor(urlPathMatching("/dining/menu/v1/[^/]+/carrillo/dinner")));
  }
}
