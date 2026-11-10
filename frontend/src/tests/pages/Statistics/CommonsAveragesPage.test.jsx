import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "react-query";
import { MemoryRouter } from "react-router";
import { afterEach, vi } from "vitest";

import CommonsAveragesPage from "main/pages/Statistics/CommonsAveragesPage";
import { PERIOD_OPTIONS } from "main/pages/Statistics/statisticsConstants";
import * as useBackendModule from "main/utils/useBackend";
import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import { statisticsFixtures } from "fixtures/statisticsFixtures";

import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";

const useBackendSpy = vi.spyOn(useBackendModule, "useBackend");

describe("CommonsAveragesPage tests", () => {
  const axiosMock = new AxiosMockAdapter(axios);

  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.userOnly);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
  });

  afterEach(() => {
    useBackendSpy.mockClear();
  });

  test("renders the table with averages", async () => {
    const queryClient = new QueryClient();
    axiosMock
      .onGet("/api/statistics/commons/averages", { params: { period: "ALL" } })
      .reply(200, statisticsFixtures.commonsAverages);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CommonsAveragesPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(
      await screen.findByText("Dining Commons Averages"),
    ).toBeInTheDocument();

    await waitFor(() => {
      expect(
        screen.getByTestId(
          "CommonsAveragesPage-table-cell-row-0-col-diningCommonsCode",
        ),
      ).toHaveTextContent("carrillo");
    });
  });

  test("useBackend is called with correct cache query key, axios params, and initial data", async () => {
    const queryClient = new QueryClient();
    axiosMock
      .onGet("/api/statistics/commons/averages", { params: { period: "ALL" } })
      .reply(200, statisticsFixtures.commonsAverages);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CommonsAveragesPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByText("Dining Commons Averages");

    expect(useBackendSpy).toHaveBeenCalledWith(
      ["/api/statistics/commons/averages", "ALL"],
      {
        method: "GET",
        url: "/api/statistics/commons/averages",
        params: { period: "ALL" },
      },
      [],
    );
  });

  test("renders empty message when there are no averages", async () => {
    const queryClient = new QueryClient();
    axiosMock
      .onGet("/api/statistics/commons/averages", { params: { period: "ALL" } })
      .reply(200, []);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CommonsAveragesPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(
      await screen.findByTestId("CommonsAveragesPage-empty"),
    ).toHaveTextContent("No reviews available yet.");
  });

  test("renders an option for each period and defaults to ALL", async () => {
    const queryClient = new QueryClient();
    axiosMock
      .onGet("/api/statistics/commons/averages", { params: { period: "ALL" } })
      .reply(200, []);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CommonsAveragesPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(
      await screen.findByTestId("CommonsAveragesPage-period-select"),
    ).toHaveValue("ALL");
    for (const opt of PERIOD_OPTIONS) {
      const option = await screen.findByTestId(
        `CommonsAveragesPage-period-option-${opt.value}`,
      );
      expect(option).toHaveTextContent(opt.label);
      expect(option).toHaveValue(opt.value);
    }
  });

  test("refetches with the new period when the dropdown changes", async () => {
    const queryClient = new QueryClient();
    axiosMock
      .onGet("/api/statistics/commons/averages", { params: { period: "ALL" } })
      .reply(200, statisticsFixtures.commonsAverages);
    axiosMock
      .onGet("/api/statistics/commons/averages", { params: { period: "1W" } })
      .reply(200, [
        { ...statisticsFixtures.commonsAverages[0], reviewCount: 42 },
      ]);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <CommonsAveragesPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await screen.findByTestId(
      "CommonsAveragesPage-table-cell-row-0-col-diningCommonsCode",
    );

    const select = screen.getByTestId("CommonsAveragesPage-period-select");
    fireEvent.change(select, { target: { value: "1W" } });
    expect(select).toHaveValue("1W");

    await waitFor(() => {
      const callsForWeek = axiosMock.history.get.filter(
        (c) =>
          c.url === "/api/statistics/commons/averages" &&
          c.params &&
          c.params.period === "1W",
      );
      expect(callsForWeek.length).toBeGreaterThanOrEqual(1);
    });

    await waitFor(() => {
      expect(
        screen.getByTestId(
          "CommonsAveragesPage-table-cell-row-0-col-reviewCount",
        ),
      ).toHaveTextContent("42");
    });

    await waitFor(() => {
      expect(
        screen.queryByTestId(
          "CommonsAveragesPage-table-cell-row-1-col-diningCommonsCode",
        ),
      ).not.toBeInTheDocument();
    });

    expect(useBackendSpy).toHaveBeenCalledWith(
      ["/api/statistics/commons/averages", "1W"],
      {
        method: "GET",
        url: "/api/statistics/commons/averages",
        params: { period: "1W" },
      },
      [],
    );
  });
});
