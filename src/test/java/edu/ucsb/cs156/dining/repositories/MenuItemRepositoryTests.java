package edu.ucsb.cs156.dining.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.ucsb.cs156.dining.entities.MenuItem;
import edu.ucsb.cs156.dining.services.wiremock.WiremockService;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataJpaTest
public class MenuItemRepositoryTests {

  @Autowired private MenuItemRepository menuItemRepository;

  @MockitoBean private WiremockService wmService;

  private MenuItem save(String dc, String meal, String name, String station) {
    return menuItemRepository.save(
        MenuItem.builder()
            .diningCommonsCode(dc)
            .mealCode(meal)
            .name(name)
            .station(station)
            .build());
  }

  @Test
  public void search_is_case_insensitive_substring_and_sorted_by_name_then_commons() {
    MenuItem ortegaTenders = save("ortega", "dinner", "Chicken Tenders", "Entrees");
    MenuItem carrilloTenders = save("carrillo", "lunch", "Chicken Tenders", "Grill");
    MenuItem bbq = save("portola", "dinner", "BBQ Chicken Pizza", "Pizza");
    save("de-la-guerra", "lunch", "Tofu Stir Fry", "Wok");

    List<MenuItem> results =
        menuItemRepository.findTop20ByNameContainingIgnoreCaseOrderByNameAscDiningCommonsCodeAsc(
            "cHiCkEn");

    assertEquals(
        List.of(bbq.getId(), carrilloTenders.getId(), ortegaTenders.getId()),
        results.stream().map(MenuItem::getId).toList());
  }

  @Test
  public void search_returns_at_most_20_results() {
    IntStream.range(0, 25).forEach(i -> save("carrillo", "lunch", "Soup " + i, "Soups"));

    List<MenuItem> results =
        menuItemRepository.findTop20ByNameContainingIgnoreCaseOrderByNameAscDiningCommonsCodeAsc(
            "soup");

    assertEquals(20, results.size());
  }
}
