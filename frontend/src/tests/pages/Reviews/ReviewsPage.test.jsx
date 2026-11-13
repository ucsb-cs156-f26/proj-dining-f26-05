import { render, waitFor, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "react-query";
import { MemoryRouter, Route, Routes } from "react-router";
import ReviewsPage from "main/pages/Reviews/ReviewsPage";
import { vi } from "vitest";

import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import { ReviewFixtures } from "fixtures/reviewFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";

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

const menuItem = {
  id: 7,
  name: "Chicken Tenders",
  diningCommonsCode: "de-la-guerra",
  mealCode: "lunch",
  station: "Grill",
};
describe("ReviewsPage tests", () => {
  const testId = "Reviewstable";

  const setupUserOnly = () => {
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.userOnly);
  };

  beforeEach(() => {
    mockToast.mockClear();
    axiosMock.reset();
    axiosMock.resetHistory();
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
  });
  afterEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
  });

  const renderWithRoute = (itemid) => {
    const queryClient = new QueryClient();
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={[`/reviews/${itemid}`]}>
          <Routes>
            <Route path="/reviews/:itemid" element={<ReviewsPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );
  };

  test("renders empty table when backend is unavailable", async () => {
    const itemid = "7";
    setupUserOnly();
    axiosMock.onGet(`/api/reviews/approved/forItem/${itemid}`).timeout();

    // SPY ON THE ADAPTER, NOT axios.request
    const adapterSpy = vi.spyOn(axios.defaults, "adapter");

    renderWithRoute(itemid);

    await waitFor(() => {
      expect(adapterSpy).toHaveBeenCalled();
      expect(adapterSpy.mock.calls[0][0].method).toBe("get"); // MUTATION KILLER
    });

    await waitFor(() => {
      // Table should remain empty
      expect(
        screen.queryByTestId(`${testId}-cell-row-0-col-item.id`),
      ).not.toBeInTheDocument();
    });
  });

  test("renders table when backend is available", async () => {
    const itemid = "7";
    setupUserOnly();
    const adapterSpy = vi.spyOn(axios.defaults, "adapter");
    axiosMock
      .onGet(`/api/reviews/approved/forItem/${itemid}`)
      .reply(200, ReviewFixtures.threeReviews);

    renderWithRoute(itemid);

    await waitFor(() => {
      expect(adapterSpy).toHaveBeenCalled();
      expect(adapterSpy.mock.calls[0][0].method).toBe("get");
    });

    await waitFor(() => {
      expect(
        screen.getByTestId(`${testId}-cell-row-0-col-item.id`),
      ).toBeInTheDocument();
    });
  });

  test("Error message includes GET method when request fails", async () => {
    const itemId = "7";
    setupUserOnly();

    axiosMock.onGet(`/api/reviews/approved/forItem/${itemId}`).networkError();
    axiosMock
      .onGet(`/api/diningcommons/menuitem?id=${itemId}`)
      .reply(200, menuItem);

    renderWithRoute(itemId);

    await waitFor(() => {
      expect(mockToast).toHaveBeenCalled();
      const toastMessage = mockToast.mock.calls[0][0];
      expect(toastMessage).toContain("GET");
      expect(toastMessage).toContain(`/api/reviews/approved/forItem/${itemId}`);
    });
  });

  test("shows the item name, dining commons, and a link to review it", async () => {
    const itemid = "7";
    setupUserOnly();
    axiosMock
      .onGet(`/api/reviews/approved/forItem/${itemid}`)
      .reply(200, ReviewFixtures.threeReviews);
    axiosMock
      .onGet(`/api/diningcommons/menuitem?id=${itemid}`)
      .reply(200, menuItem);

    renderWithRoute(itemid);

    expect(screen.getByTestId("ReviewsPage-title")).toHaveTextContent(
      "Reviews for Menu Item 7",
    );
    await waitFor(() =>
      expect(screen.getByTestId("ReviewsPage-title")).toHaveTextContent(
        "Reviews for Chicken Tenders",
      ),
    );
    expect(screen.getByTestId("ReviewsPage-details")).toHaveTextContent(
      "De La Guerra · lunch · Grill",
    );
    const button = screen.getByTestId("ReviewsPage-review-button");
    expect(button).toHaveTextContent("Review This Item");
    expect(button).toHaveAttribute("href", "/reviews/post/7");
  });
});
