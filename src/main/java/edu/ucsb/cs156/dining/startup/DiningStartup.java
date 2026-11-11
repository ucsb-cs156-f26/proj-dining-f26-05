package edu.ucsb.cs156.dining.startup;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.dining.entities.Admin;
import edu.ucsb.cs156.dining.jobs.LoadMenuItemsJob;
import edu.ucsb.cs156.dining.repositories.AdminRepository;
import edu.ucsb.cs156.dining.services.DiningCommonsService;
import edu.ucsb.cs156.dining.services.UCSBDiningMenuItemsService;
import edu.ucsb.cs156.dining.services.UCSBDiningMenuService;
import edu.ucsb.cs156.jobs.services.JobService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** This class contains a method that is called once at application startup time. */
@Slf4j
@Component
public class DiningStartup {

  /** Default value of app.ucsb.api.consumer_key when UCSB_API_KEY is not set. */
  static final String API_KEY_PLACEHOLDER = "see-instructions-in-readme";

  @Value("#{'${app.admin.emails}'.split(',')}")
  List<String> adminEmails;

  @Value("${app.ucsb.api.consumer_key}")
  String apiKey;

  @Autowired AdminRepository adminRepository;
  @Autowired JobService jobService;
  @Autowired DiningCommonsService diningCommonsService;
  @Autowired UCSBDiningMenuService menuService;
  @Autowired UCSBDiningMenuItemsService menuItemsService;
  @Autowired ObjectMapper objectMapper;

  Clock clock = Clock.system(ZoneId.of("America/Los_Angeles"));

  /**
   * Called once at application startup time. Loads all emails in ADMIN_EMAILS into the Admin table,
   * then launches a background job to load the current month's MenuItems.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void alwaysRunOnStartup() {
    log.info("alwaysRunOnStartup called");

    adminEmails.forEach(
        (email) -> {
          try {
            if (!adminRepository.existsByEmail(email)) {
              Admin admin = new Admin(email);
              adminRepository.save(admin);
            }
          } catch (Exception e) {
            log.error("Error saving admin email {}: {}", email, e.getMessage());
          }
        });

    loadCurrentMonthMenuItems();
  }

  /**
   * Launches a LoadMenuItemsJob for every day of the current month, unless the UCSB API key is not
   * configured (in which case every request would fail, so we don't send any).
   */
  void loadCurrentMonthMenuItems() {
    if (apiKey == null || apiKey.isBlank() || apiKey.equals(API_KEY_PLACEHOLDER)) {
      log.warn("UCSB_API_KEY is not set; skipping loading of current month's menu items");
      return;
    }

    LocalDate today = LocalDate.now(clock);
    LocalDate start = today.withDayOfMonth(1);
    LocalDate end = today.withDayOfMonth(today.lengthOfMonth());

    LoadMenuItemsJob job =
        LoadMenuItemsJob.builder()
            .diningCommonsService(diningCommonsService)
            .menuService(menuService)
            .menuItemsService(menuItemsService)
            .objectMapper(objectMapper)
            .startDate(start)
            .endDate(end)
            .build();

    try {
      jobService.runAsJob(job);
      log.info("Launched job to load menu items from {} to {}", start, end);
    } catch (Exception e) {
      log.error("Error launching job to load menu items: {}", e.getMessage());
    }
  }
}
