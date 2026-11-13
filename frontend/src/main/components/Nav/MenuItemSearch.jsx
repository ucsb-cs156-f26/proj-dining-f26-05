import { useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router";
import { AsyncTypeahead } from "react-bootstrap-typeahead";
import { toast } from "react-toastify";
import { diningCommonsName } from "main/utils/diningCommonsNames";

import "react-bootstrap-typeahead/css/Typeahead.css";

export default function MenuItemSearch() {
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = useState(false);
  // Stryker disable next-line ArrayDeclaration : the menu is hidden until a search returns, so initial options are never shown
  const [options, setOptions] = useState([]);

  const handleSearch = async (query) => {
    setIsLoading(true);
    try {
      const response = await axios.get("/api/diningcommons/menuitem/search", {
        params: { query },
      });
      setOptions(response.data);
    } catch (e) {
      console.error("Error searching menu items", e);
      toast("Error searching menu items");
      setOptions([]);
    } finally {
      setIsLoading(false);
    }
  };

  // `selected` is held at [] (below), so onChange only fires when the user
  // picks an option, and always with exactly that one option.
  const handleChange = (selected) => {
    navigate(`/reviews/${selected[0].id}`);
  };

  return (
    <AsyncTypeahead
      id="menu-item-search"
      className="me-3"
      inputProps={{ "data-testid": "MenuItemSearch-input" }}
      isLoading={isLoading}
      minLength={2}
      onSearch={handleSearch}
      onChange={handleChange}
      options={options}
      // The backend already filters by name, so show every result it returns.
      filterBy={() => true}
      labelKey="name"
      placeholder="Search menu items..."
      emptyLabel="No matching menu items."
      selected={[]}
      renderMenuItemChildren={(item) => (
        <div data-testid={`MenuItemSearch-option-${item.id}`}>
          <div>{item.name}</div>
          <small className="text-muted">
            {diningCommonsName(item.diningCommonsCode)} · {item.mealCode} ·{" "}
            {item.station}
          </small>
        </div>
      )}
    />
  );
}
