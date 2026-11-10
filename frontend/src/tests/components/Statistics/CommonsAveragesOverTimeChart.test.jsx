import { render, screen } from "@testing-library/react";
import { vi } from "vitest";
import React from "react";

import CommonsAveragesOverTimeChart, {
  pivotRows,
} from "main/components/Statistics/CommonsAveragesOverTimeChart";
import { statisticsFixtures } from "fixtures/statisticsFixtures";

// ResponsiveContainer renders nothing in jsdom (zero size), so give the chart a fixed size.
vi.mock("recharts", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    ResponsiveContainer: ({ children }) =>
      React.cloneElement(children, { width: 600, height: 400 }),
  };
});

describe("pivotRows", () => {
  test("returns an empty array for no rows", () => {
    expect(pivotRows([])).toEqual([]);
  });

  test("pivots rows into one object per period, sorted by period", () => {
    expect(pivotRows(statisticsFixtures.commonsOverTime)).toEqual([
      { period: "2025-03", carrillo: 4.2 },
      { period: "2025-04", carrillo: 3.8, ortega: 4.6 },
    ]);
  });

  test("sorts periods even when rows are out of order", () => {
    expect(
      pivotRows([
        { diningCommonsCode: "a", period: "2025-05", averageStars: 1 },
        { diningCommonsCode: "b", period: "2025-01", averageStars: 2 },
        { diningCommonsCode: "a", period: "2025-03", averageStars: 3 },
      ]).map((d) => d.period),
    ).toEqual(["2025-01", "2025-03", "2025-05"]);
  });
});

describe("CommonsAveragesOverTimeChart tests", () => {
  test("renders a line per dining commons with default test id", () => {
    const { container } = render(
      <CommonsAveragesOverTimeChart
        rows={statisticsFixtures.commonsOverTime}
      />,
    );
    expect(
      screen.getByTestId("CommonsAveragesOverTimeChart"),
    ).toBeInTheDocument();
    expect(screen.getByTestId("CommonsAveragesOverTimeChart")).toHaveStyle({
      width: "100%",
      height: "400px",
    });
    expect(container.querySelectorAll(".recharts-line")).toHaveLength(2);
    expect(container.querySelector(".recharts-yAxis")).toHaveTextContent("1");
    expect(container.querySelector(".recharts-yAxis")).toHaveTextContent("5");
    expect(container.querySelector(".recharts-xAxis")).toHaveTextContent(
      "2025-03",
    );
    expect(screen.getByText("carrillo")).toBeInTheDocument();
    expect(screen.getByText("ortega")).toBeInTheDocument();
  });

  test("uses the testIdPrefix prop", () => {
    render(
      <CommonsAveragesOverTimeChart
        rows={statisticsFixtures.commonsOverTime}
        testIdPrefix="custom"
      />,
    );
    expect(screen.getByTestId("custom")).toBeInTheDocument();
  });

  test("cycles colors when there are more commons than colors", () => {
    const rows = Array.from({ length: 7 }, (_, i) =>
      ["2025-01", "2025-02"].map((period) => ({
        diningCommonsCode: `c${i}`,
        period,
        averageStars: 3,
        reviewCount: 1,
      })),
    ).flat();
    const { container } = render(<CommonsAveragesOverTimeChart rows={rows} />);
    const lines = container.querySelectorAll(".recharts-line-curve");
    expect(
      Array.from(lines)
        .slice(0, 6)
        .map((line) => line.getAttribute("stroke")),
    ).toEqual([
      "#1f77b4",
      "#d62728",
      "#2ca02c",
      "#ff7f0e",
      "#9467bd",
      "#8c564b",
    ]);
    expect(lines[0].getAttribute("stroke")).toBe(
      lines[6].getAttribute("stroke"),
    );
    expect(lines[0].getAttribute("stroke")).not.toBe(
      lines[1].getAttribute("stroke"),
    );
  });
});
