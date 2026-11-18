import React from "react";
import CommonsMealAveragesTable from "main/components/Statistics/CommonsMealAveragesTable";
import { useBackend } from "main/utils/useBackend";

export default function CommonsMealAveragesSection({ code, period }) {
  const { data: averages } = useBackend(
    [`/api/statistics/commons/${code}/meals/averages`, period],
    {
      method: "GET",
      url: `/api/statistics/commons/${code}/meals/averages`,
      params: { period },
    },
    [],
  );

  if (averages && averages.length === 0) {
    return (
      <p data-testid="CommonsMealAveragesSection-empty">
        No reviews available yet.
      </p>
    );
  }

  return (
    <CommonsMealAveragesTable
      averages={averages}
      testIdPrefix="CommonsMealAveragesSection-table"
    />
  );
}
