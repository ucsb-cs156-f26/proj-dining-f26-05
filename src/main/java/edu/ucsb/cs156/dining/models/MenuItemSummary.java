package edu.ucsb.cs156.dining.models;

import edu.ucsb.cs156.dining.entities.MenuItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight view of a MenuItem returned by the menu item search and get-by-id endpoints. It
 * deliberately omits reviews so that responses are small and never expose unmoderated reviews or
 * reviewers' user records.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MenuItemSummary {
  private long id;
  private String name;
  private String diningCommonsCode;
  private String mealCode;
  private String station;

  public static MenuItemSummary from(MenuItem item) {
    return MenuItemSummary.builder()
        .id(item.getId())
        .name(item.getName())
        .diningCommonsCode(item.getDiningCommonsCode())
        .mealCode(item.getMealCode())
        .station(item.getStation())
        .build();
  }
}
