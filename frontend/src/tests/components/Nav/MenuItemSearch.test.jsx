import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { vi } from "vitest";

import MenuItemSearch from "main/components/Nav/MenuItemSearch";
import { menuItemSearchFixtures } from "fixtures/menuItemSearchFixtures";

const mockNavigate = vi.fn();
vi.mock("react-router", async () => {
  const originalModule = await vi.importActual("react-router");
  return {
    __esModule: true,
    ...originalModule,
    useNavigate: () => mockNavigate,
  };
});

const mockToast = vi.fn();
vi.mock("react-toastify", async () => {
  const originalModule = await vi.importActual("react-toastify");
  return {
    __esModule: true,
    ...originalModule,
    toast: (x) => mockToast(x),
  };
});

const axiosMock = new AxiosMockAdapter(axios);

describe("MenuItemSearch tests", () => {
  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    mockNavigate.mockClear();
    mockToast.mockClear();
  });

  const renderComponent = () =>
    render(
      <MemoryRouter>
        <MenuItemSearch />
      </MemoryRouter>,
    );

  test("shows matching items with their dining commons, then navigates on click", async () => {
    axiosMock
      .onGet("/api/diningcommons/menuitem/search", {
        params: { query: "chick" },
      })
      .reply(200, menuItemSearchFixtures.chickenResults);

    renderComponent();
    const input = screen.getByTestId("MenuItemSearch-input");
    expect(input).toHaveAttribute("placeholder", "Search menu items...");
    expect(screen.queryByText("Loading...")).not.toBeInTheDocument();

    await userEvent.type(input, "chick");

    const first = await screen.findByTestId("MenuItemSearch-option-11");
    expect(first).toHaveTextContent("Chicken Tenders");
    expect(first).toHaveTextContent("Carrillo · lunch · Grill");
    expect(screen.getByTestId("MenuItemSearch-option-12")).toHaveTextContent(
      "De La Guerra · dinner · Entrees",
    );
    expect(axiosMock.history.get[0].params).toEqual({ query: "chick" });

    await userEvent.click(screen.getByTestId("MenuItemSearch-option-12"));
    expect(mockNavigate).toHaveBeenCalledWith("/reviews/12");
    // The input is cleared after selecting so the navbar is ready for the next search
    await waitFor(() => expect(input).toHaveValue(""));
  });

  test("shows a loading spinner while the search is in flight", async () => {
    let resolveSearch;
    axiosMock.onGet("/api/diningcommons/menuitem/search").reply(
      () =>
        new Promise((resolve) => {
          resolveSearch = () =>
            resolve([200, menuItemSearchFixtures.chickenResults]);
        }),
    );
    renderComponent();
    await userEvent.type(screen.getByTestId("MenuItemSearch-input"), "chick");

    expect(await screen.findByText("Loading...")).toBeInTheDocument();
    resolveSearch();
    await screen.findByTestId("MenuItemSearch-option-11");
    expect(screen.queryByText("Loading...")).not.toBeInTheDocument();
  });

  test("does not search until at least two characters are typed", async () => {
    renderComponent();
    await userEvent.type(screen.getByTestId("MenuItemSearch-input"), "c");
    // wait past the typeahead's debounce delay
    await new Promise((r) => setTimeout(r, 400));
    expect(axiosMock.history.get.length).toBe(0);
  });

  test("shows empty label when nothing matches", async () => {
    axiosMock.onGet("/api/diningcommons/menuitem/search").reply(200, []);
    renderComponent();
    await userEvent.type(screen.getByTestId("MenuItemSearch-input"), "zzz");
    expect(
      await screen.findByText("No matching menu items."),
    ).toBeInTheDocument();
    expect(mockNavigate).not.toHaveBeenCalled();
  });

  test("shows a toast and no options when the backend fails", async () => {
    axiosMock.onGet("/api/diningcommons/menuitem/search").reply(500);
    const errorSpy = vi.spyOn(console, "error").mockImplementation(() => {});
    renderComponent();
    await userEvent.type(screen.getByTestId("MenuItemSearch-input"), "chick");
    await waitFor(() =>
      expect(mockToast).toHaveBeenCalledWith("Error searching menu items"),
    );
    expect(errorSpy).toHaveBeenCalledWith(
      "Error searching menu items",
      expect.anything(),
    );
    expect(
      await screen.findByText("No matching menu items."),
    ).toBeInTheDocument();
    errorSpy.mockRestore();
  });
});
