package edu.ucsb.cs156.dining.startup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.dining.entities.Admin;
import edu.ucsb.cs156.dining.jobs.LoadMenuItemsJob;
import edu.ucsb.cs156.dining.repositories.AdminRepository;
import edu.ucsb.cs156.dining.services.DiningCommonsService;
import edu.ucsb.cs156.dining.services.UCSBDiningMenuItemsService;
import edu.ucsb.cs156.dining.services.UCSBDiningMenuService;
import edu.ucsb.cs156.jobs.services.JobService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

class DiningStartupTests {

  @Mock private AdminRepository adminRepository;

  @Mock private JobService jobService;
  @Mock private DiningCommonsService diningCommonsService;
  @Mock private UCSBDiningMenuService menuService;
  @Mock private UCSBDiningMenuItemsService menuItemsService;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private DiningStartup diningStartup;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    diningStartup = new DiningStartup();
    diningStartup.adminRepository = adminRepository;
    diningStartup.adminEmails = List.of("admin1@ucsb.edu", "admin2@ucsb.edu");
    diningStartup.jobService = jobService;
    diningStartup.diningCommonsService = diningCommonsService;
    diningStartup.menuService = menuService;
    diningStartup.menuItemsService = menuItemsService;
    diningStartup.objectMapper = objectMapper;
    diningStartup.apiKey = "real-key";
    // 2026-02-10 07:00 UTC is 2026-02-09 23:00 in Santa Barbara
    diningStartup.clock =
        Clock.fixed(Instant.parse("2026-02-10T07:00:00Z"), ZoneId.of("America/Los_Angeles"));
  }

  @Test
  void test_AlwaysRunOnStartup_saves_admins() {
    when(adminRepository.existsByEmail(anyString())).thenReturn(false);

    diningStartup.alwaysRunOnStartup();

    verify(adminRepository).save(new Admin("admin1@ucsb.edu"));
    verify(adminRepository).save(new Admin("admin2@ucsb.edu"));
  }

  @Test
  void test_AlwaysRunOnStartup_skips_existing_admins() {
    when(adminRepository.existsByEmail("admin1@ucsb.edu")).thenReturn(true);
    when(adminRepository.existsByEmail("admin2@ucsb.edu")).thenReturn(false);

    diningStartup.alwaysRunOnStartup();

    verify(adminRepository, never()).save(new Admin("admin1@ucsb.edu"));
    verify(adminRepository).save(new Admin("admin2@ucsb.edu"));
  }

  @Test
  void test_AlwaysRunOnStartup_handles_exception() {
    when(adminRepository.existsByEmail(anyString())).thenReturn(false);
    doThrow(new RuntimeException("Simulated error")).when(adminRepository).save(any(Admin.class));

    diningStartup.alwaysRunOnStartup();

    // Both emails are attempted despite the exception on the first one
    verify(adminRepository, times(2)).save(any(Admin.class));
  }

  @Test
  void test_default_clock_is_pacific_time() {
    assertEquals(ZoneId.of("America/Los_Angeles"), new DiningStartup().clock.getZone());
  }

  @Test
  void test_AlwaysRunOnStartup_launches_job_for_current_month() {
    diningStartup.alwaysRunOnStartup();

    ArgumentCaptor<LoadMenuItemsJob> captor = ArgumentCaptor.forClass(LoadMenuItemsJob.class);
    verify(jobService).runAsJob(captor.capture());
    LoadMenuItemsJob job = captor.getValue();
    assertEquals(LocalDate.of(2026, 2, 1), job.getStartDate());
    assertEquals(LocalDate.of(2026, 2, 28), job.getEndDate());
    assertSame(diningCommonsService, job.getDiningCommonsService());
    assertSame(menuService, job.getMenuService());
    assertSame(menuItemsService, job.getMenuItemsService());
    assertSame(objectMapper, job.getObjectMapper());
  }

  @Test
  void test_does_not_launch_job_when_api_key_is_placeholder() {
    diningStartup.apiKey = "see-instructions-in-readme";
    diningStartup.loadCurrentMonthMenuItems();
    verifyNoInteractions(jobService);
  }

  @Test
  void test_does_not_launch_job_when_api_key_is_blank() {
    diningStartup.apiKey = "  ";
    diningStartup.loadCurrentMonthMenuItems();
    verifyNoInteractions(jobService);
  }

  @Test
  void test_does_not_launch_job_when_api_key_is_null() {
    diningStartup.apiKey = null;
    diningStartup.loadCurrentMonthMenuItems();
    verifyNoInteractions(jobService);
  }

  @Test
  void test_handles_exception_launching_job() {
    when(jobService.runAsJob(any())).thenThrow(new RuntimeException("Simulated error"));
    diningStartup.loadCurrentMonthMenuItems();
    verify(jobService).runAsJob(any());
  }
}
