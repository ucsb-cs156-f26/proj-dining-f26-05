package edu.ucsb.cs156.dining.models;

import edu.ucsb.cs156.dining.entities.MenuItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight view of a MenuItem returned by the menu item search endpoint. It deliberately omits
 * reviews so that search results are small and never expose unmoderated reviews.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MenuItemSearchResult {
  private long id;
  private String name;
  private String diningCommonsCode;
  private String mealCode;
  private String station;

  public static MenuItemSearchResult from(MenuItem item) {
    return MenuItemSearchResult.builder()
        .id(item.getId())
        .name(item.getName())
        .diningCommonsCode(item.getDiningCommonsCode())
        .mealCode(item.getMealCode())
        .station(item.getStation())
        .build();
  }
}
