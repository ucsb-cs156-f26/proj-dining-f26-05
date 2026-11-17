package edu.ucsb.cs156.dining.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.dining.entities.MenuItem;
import edu.ucsb.cs156.dining.models.DiningCommons;
import edu.ucsb.cs156.dining.models.Entree;
import edu.ucsb.cs156.dining.services.DiningCommonsService;
import edu.ucsb.cs156.dining.services.UCSBDiningMenuItemsService;
import edu.ucsb.cs156.dining.services.UCSBDiningMenuService;
import edu.ucsb.cs156.jobs.services.JobContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

public class LoadMenuItemsJobTests {

  private static final String TWO_MEALS =
      "[{\"name\":\"Lunch\",\"code\":\"lunch\"},{\"name\":\"Dinner\",\"code\":\"dinner\"}]";

  private DiningCommonsService diningCommonsService;
  private UCSBDiningMenuService menuService;
  private UCSBDiningMenuItemsService menuItemsService;
  private JobContext ctx;
  private List<Long> sleeps;

  private final List<Entree> entrees = List.of(new Entree("Pizza", "Grill"));
  private final List<MenuItem> twoItems = List.of(new MenuItem(), new MenuItem());

  @BeforeEach
  void setUp() {
    diningCommonsService = mock(DiningCommonsService.class);
    menuService = mock(UCSBDiningMenuService.class);
    menuItemsService = mock(UCSBDiningMenuItemsService.class);
    ctx = mock(JobContext.class);
    sleeps = new ArrayList<>();
  }

  private LoadMenuItemsJob job(LocalDate start, LocalDate end) {
    return LoadMenuItemsJob.builder()
        .diningCommonsService(diningCommonsService)
        .menuService(menuService)
        .menuItemsService(menuItemsService)
        .objectMapper(new ObjectMapper())
        .startDate(start)
        .endDate(end)
        .sleeper(sleeps::add)
        .maxRetries(2)
        .initialBackoffMs(100)
        .build();
  }

  private static HttpClientErrorException tooMany() {
    return HttpClientErrorException.create(
        HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", new HttpHeaders(), new byte[0], null);
  }

  private static DiningCommons commons(String code) {
    return DiningCommons.builder().code(code).build();
  }

  @Test
  void builder_defaults() {
    LoadMenuItemsJob job = LoadMenuItemsJob.builder().build();
    assertEquals(6, job.getMaxRetries());
    assertEquals(1000, job.getInitialBackoffMs());
  }

  @Test
  void loads_every_day_commons_and_meal_in_range() throws Exception {
    when(diningCommonsService.get()).thenReturn(List.of(commons("ortega"), commons("carrillo")));
    when(menuService.getJSON(anyString(), anyString())).thenReturn(TWO_MEALS);
    when(menuItemsService.get(anyString(), anyString(), anyString())).thenReturn(entrees);
    when(menuItemsService.getOrCreateMenuItems(anyString(), anyString(), anyList()))
        .thenReturn(twoItems);

    job(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2)).accept(ctx);

    for (String date : List.of("2026-11-01", "2026-11-02")) {
      for (String dc : List.of("ortega", "carrillo")) {
        verify(menuService).getJSON(date, dc);
        verify(menuItemsService).get(date, dc, "lunch");
        verify(menuItemsService).get(date, dc, "dinner");
      }
    }
    verify(menuService, times(4)).getJSON(anyString(), anyString());
    verify(menuItemsService, times(2)).getOrCreateMenuItems("ortega", "lunch", entrees);
    verify(menuItemsService, times(8)).getOrCreateMenuItems(anyString(), anyString(), anyList());
    verify(ctx).log("Loading menu items from 2026-11-01 to 2026-11-02");
    // 2 days * 2 commons * 2 meals * 2 items
    verify(ctx).log("Done loading menu items; 16 menu items processed.");
  }

  @Test
  void skips_closed_commons_and_empty_meals() throws Exception {
    LocalDate day = LocalDate.of(2026, 11, 1);
    when(diningCommonsService.get()).thenReturn(List.of(commons("ortega"), commons("carrillo")));
    when(menuService.getJSON("2026-11-01", "ortega")).thenReturn(null);
    when(menuService.getJSON("2026-11-01", "carrillo")).thenReturn(TWO_MEALS);
    when(menuItemsService.get(anyString(), anyString(), anyString())).thenReturn(List.of());

    job(day, day).accept(ctx);

    verify(menuItemsService, never()).get("2026-11-01", "ortega", "lunch");
    verify(menuItemsService).get("2026-11-01", "carrillo", "lunch");
    verify(menuItemsService, never()).getOrCreateMenuItems(anyString(), anyString(), anyList());
    verify(ctx).log("Done loading menu items; 0 menu items processed.");
  }

  @Test
  void logs_and_continues_when_a_meal_fails() throws Exception {
    LocalDate day = LocalDate.of(2026, 11, 1);
    when(diningCommonsService.get()).thenReturn(List.of(commons("ortega")));
    when(menuService.getJSON(anyString(), anyString())).thenReturn(TWO_MEALS);
    when(menuItemsService.get("2026-11-01", "ortega", "lunch"))
        .thenThrow(
            HttpClientErrorException.create(
                HttpStatus.NOT_FOUND, "nf", new HttpHeaders(), new byte[0], null));
    when(menuItemsService.get("2026-11-01", "ortega", "dinner")).thenReturn(entrees);
    when(menuItemsService.getOrCreateMenuItems(anyString(), anyString(), anyList()))
        .thenReturn(twoItems);

    job(day, day).accept(ctx);

    verify(ctx).log("Skipping ortega lunch on 2026-11-01: 404 nf");
    verify(menuItemsService).getOrCreateMenuItems("ortega", "dinner", entrees);
    verify(ctx).log("Done loading menu items; 2 menu items processed.");
  }

  @Test
  void logs_and_continues_on_bad_json_from_meal() throws Exception {
    LocalDate day = LocalDate.of(2026, 11, 1);
    when(diningCommonsService.get()).thenReturn(List.of(commons("ortega")));
    when(menuService.getJSON(anyString(), anyString())).thenReturn(TWO_MEALS);
    when(menuItemsService.get("2026-11-01", "ortega", "lunch"))
        .thenThrow(new JsonProcessingException("bad json") {});
    when(menuItemsService.get("2026-11-01", "ortega", "dinner")).thenReturn(entrees);
    when(menuItemsService.getOrCreateMenuItems(anyString(), anyString(), anyList()))
        .thenReturn(twoItems);

    job(day, day).accept(ctx);

    verify(ctx).log("Skipping ortega lunch on 2026-11-01: bad json");
    verify(ctx).log("Done loading menu items; 2 menu items processed.");
  }

  @Test
  void retries_with_exponential_backoff_on_429() throws Exception {
    LocalDate day = LocalDate.of(2026, 11, 1);
    when(diningCommonsService.get()).thenThrow(tooMany()).thenReturn(List.of(commons("ortega")));
    when(menuService.getJSON(anyString(), anyString())).thenReturn(TWO_MEALS);
    when(menuItemsService.get("2026-11-01", "ortega", "lunch"))
        .thenThrow(tooMany())
        .thenThrow(tooMany())
        .thenReturn(entrees);
    when(menuItemsService.get("2026-11-01", "ortega", "dinner")).thenReturn(List.of());
    when(menuItemsService.getOrCreateMenuItems(anyString(), anyString(), anyList()))
        .thenReturn(twoItems);

    job(day, day).accept(ctx);

    assertEquals(List.of(100L, 100L, 200L), sleeps);
    verify(ctx, times(2)).log("Rate limited by UCSB API (429); retrying in 100 ms");
    verify(ctx).log("Rate limited by UCSB API (429); retrying in 200 ms");
    verify(ctx).log("Done loading menu items; 2 menu items processed.");
  }

  @Test
  void gives_up_after_max_retries_on_429() throws Exception {
    LocalDate day = LocalDate.of(2026, 11, 1);
    when(diningCommonsService.get()).thenReturn(List.of(commons("ortega")));
    when(menuService.getJSON(anyString(), anyString())).thenReturn(TWO_MEALS);
    when(menuItemsService.get(anyString(), anyString(), anyString())).thenThrow(tooMany());

    LoadMenuItemsJob job = job(day, day);
    assertThrows(HttpClientErrorException.TooManyRequests.class, () -> job.accept(ctx));

    assertEquals(List.of(100L, 200L), sleeps);
    verify(menuItemsService, times(3)).get("2026-11-01", "ortega", "lunch");
    verify(menuItemsService, never()).get("2026-11-01", "ortega", "dinner");
    verify(ctx).log("Still rate limited after 2 retries; giving up.");
    verify(ctx, never()).log("Done loading menu items; 0 menu items processed.");
  }

  @Test
  void default_sleeper_actually_sleeps() throws Exception {
    LoadMenuItemsJob job = LoadMenuItemsJob.builder().build();
    long start = System.nanoTime();
    job.getSleeper().sleep(50);
    long elapsedMs = (System.nanoTime() - start) / 1_000_000;
    assertTrue(elapsedMs >= 45, "slept only " + elapsedMs + " ms");
  }
}
