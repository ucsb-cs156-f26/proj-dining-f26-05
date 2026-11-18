import React from "react";
import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

// eslint-disable-next-line react-refresh/only-export-components -- pure helper exported for tests
export function pivotRows(rows) {
  const byPeriod = {};
  rows.forEach(({ diningCommonsCode, period, averageStars }) => {
    byPeriod[period] = byPeriod[period] || { period };
    byPeriod[period][diningCommonsCode] = averageStars;
  });
  return Object.values(byPeriod).sort((a, b) =>
    a.period.localeCompare(b.period),
  );
}

export default function CommonsAveragesOverTimeChart({
  rows,
  testIdPrefix = "CommonsAveragesOverTimeChart",
}) {
  const COLORS = [
    "#1f77b4",
    "#d62728",
    "#2ca02c",
    "#ff7f0e",
    "#9467bd",
    "#8c564b",
  ];
  const data = pivotRows(rows);
  const codes = [...new Set(rows.map((row) => row.diningCommonsCode))];

  return (
    <div data-testid={testIdPrefix} style={{ width: "100%", height: 400 }}>
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={data}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="period" />
          <YAxis domain={[1, 5]} />
          <Tooltip />
          <Legend />
          {codes.map((code, index) => (
            <Line
              key={code}
              type="monotone"
              dataKey={code}
              stroke={COLORS[index % COLORS.length]}
              connectNulls
            />
          ))}
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
