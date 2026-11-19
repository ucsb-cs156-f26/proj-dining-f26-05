package edu.ucsb.cs156.dining.jobs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.dining.models.DiningCommons;
import edu.ucsb.cs156.dining.models.Entree;
import edu.ucsb.cs156.dining.services.DiningCommonsService;
import edu.ucsb.cs156.dining.services.UCSBDiningMenuItemsService;
import edu.ucsb.cs156.dining.services.UCSBDiningMenuService;
import edu.ucsb.cs156.jobs.services.JobContext;
import edu.ucsb.cs156.jobs.services.JobContextConsumer;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import lombok.Builder;
import lombok.Getter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

/**
 * Downloads the menus for every dining commons and meal on each day in [startDate, endDate] from
 * the UCSB Dining API, and saves any MenuItems not already in the database. Backs off exponentially
 * when the API responds with 429 Too Many Requests.
 */
@Builder
@Getter
public class LoadMenuItemsJob implements JobContextConsumer {

  /** Abstraction over Thread.sleep so tests don't have to actually wait. */
  @FunctionalInterface
  public interface Sleeper {
    void sleep(long ms) throws InterruptedException;
  }

  private DiningCommonsService diningCommonsService;
  private UCSBDiningMenuService menuService;
  private UCSBDiningMenuItemsService menuItemsService;
  private ObjectMapper objectMapper;
  private LocalDate startDate;
  private LocalDate endDate;

  @Builder.Default private Sleeper sleeper = Thread::sleep;
  @Builder.Default private int maxRetries = 6;
  @Builder.Default private long initialBackoffMs = 1000;

  @Override
  public void accept(JobContext ctx) throws Exception {
    ctx.log("Loading menu items from " + startDate + " to " + endDate);

    List<DiningCommons> commons = withBackoff(ctx, () -> diningCommonsService.get());

    int count = 0;
    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      for (DiningCommons dc : commons) {
        count += loadDay(ctx, date.toString(), dc.getCode());
      }
    }

    ctx.log("Done loading menu items; " + count + " menu items processed.");
  }

  private int loadDay(JobContext ctx, String date, String commonsCode) throws Exception {
    String mealsJson = withBackoff(ctx, () -> menuService.getJSON(date, commonsCode));
    if (mealsJson == null) {
      // getJSON returns null when the dining commons is closed that day
      return 0;
    }

    int count = 0;
    for (JsonNode meal : objectMapper.readTree(mealsJson)) {
      String mealCode = meal.get("code").asText();
      try {
        List<Entree> entrees =
            withBackoff(ctx, () -> menuItemsService.get(date, commonsCode, mealCode));
        if (!entrees.isEmpty()) {
          count += menuItemsService.getOrCreateMenuItems(commonsCode, mealCode, entrees).size();
        }
      } catch (HttpClientErrorException.TooManyRequests e) {
        throw e;
      } catch (RestClientException | JsonProcessingException e) {
        ctx.log("Skipping " + commonsCode + " " + mealCode + " on " + date + ": " + e.getMessage());
      }
    }
    return count;
  }

  /** Runs call, retrying with exponential backoff if the API responds 429 Too Many Requests. */
  <T> T withBackoff(JobContext ctx, Callable<T> call) throws Exception {
    long delayMs = initialBackoffMs;
    for (int attempt = 0; ; attempt++) {
      try {
        return call.call();
      } catch (HttpClientErrorException.TooManyRequests e) {
        if (attempt >= maxRetries) {
          ctx.log("Still rate limited after " + maxRetries + " retries; giving up.");
          throw e;
        }
        ctx.log("Rate limited by UCSB API (429); retrying in " + delayMs + " ms");
        sleeper.sleep(delayMs);
        delayMs *= 2;
      }
    }
  }
}
