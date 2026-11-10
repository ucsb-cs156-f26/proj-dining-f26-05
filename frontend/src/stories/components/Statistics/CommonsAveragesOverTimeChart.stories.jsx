import React from "react";
import CommonsAveragesOverTimeChart from "main/components/Statistics/CommonsAveragesOverTimeChart";
import { statisticsFixtures } from "fixtures/statisticsFixtures";

export default {
  title: "components/Statistics/CommonsAveragesOverTimeChart",
  component: CommonsAveragesOverTimeChart,
};

const Template = (args) => {
  return <CommonsAveragesOverTimeChart {...args} />;
};

export const Default = Template.bind({});
Default.args = {
  rows: statisticsFixtures.commonsOverTime,
};
