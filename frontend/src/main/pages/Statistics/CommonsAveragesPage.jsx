import React, { useState } from "react";
import { Form } from "react-bootstrap";
import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import CommonsAveragesTable from "main/components/Statistics/CommonsAveragesTable";
import { useBackend } from "main/utils/useBackend";
import { PERIOD_OPTIONS } from "main/pages/Statistics/statisticsConstants";

export default function CommonsAveragesPage() {
  const [period, setPeriod] = useState("ALL");

  const { data: averages } = useBackend(
    ["/api/statistics/commons/averages", period],
    {
      method: "GET",
      url: "/api/statistics/commons/averages",
      params: { period },
    },
    [],
  );

  const onChangePeriod = (e) => {
    setPeriod(e.target.value);
  };

  return (
    <BasicLayout>
      <div className="pt-2">
        <h1>Dining Commons Averages</h1>
        <Form.Group className="mb-3">
          <Form.Label htmlFor="CommonsAveragesPage-period-select">
            Time period
          </Form.Label>
          <Form.Select
            id="CommonsAveragesPage-period-select"
            data-testid="CommonsAveragesPage-period-select"
            value={period}
            onChange={onChangePeriod}
          >
            {PERIOD_OPTIONS.map((opt) => (
              <option
                key={opt.value}
                value={opt.value}
                data-testid={`CommonsAveragesPage-period-option-${opt.value}`}
              >
                {opt.label}
              </option>
            ))}
          </Form.Select>
        </Form.Group>
        {averages && averages.length === 0 ? (
          <p data-testid="CommonsAveragesPage-empty">
            No reviews available yet.
          </p>
        ) : (
          <CommonsAveragesTable
            averages={averages}
            testIdPrefix="CommonsAveragesPage-table"
          />
        )}
      </div>
    </BasicLayout>
  );
}
