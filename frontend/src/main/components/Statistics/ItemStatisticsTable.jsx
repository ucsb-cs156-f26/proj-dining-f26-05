import React from "react";
import OurTable from "main/components/OurTable";
import { Link } from "react-router";

export default function ItemStatisticsTable({
  items,
  testIdPrefix = "ItemStatisticsTable",
}) {
  const columns = [
    {
      Header: "Item",
      accessor: "itemName",
      Cell: ({ row }) => (
        <Link
          to={`/reviews/${row.original.itemId}`}
          data-testid={`${testIdPrefix}-link-row-${row.index}`}
        >
          {row.original.itemName}
        </Link>
      ),
    },
    {
      Header: "Dining Commons",
      accessor: "diningCommonsCode",
    },
    {
      Header: "Meal",
      accessor: "mealCode",
    },
    {
      Header: "Station",
      accessor: "station",
    },
    {
      Header: "Average Score",
      accessor: "averageStars",
      Cell: ({ value }) => value.toFixed(2),
    },
    {
      Header: "Reviews",
      accessor: "reviewCount",
    },
  ];

  return <OurTable data={items} columns={columns} testid={testIdPrefix} />;
}
