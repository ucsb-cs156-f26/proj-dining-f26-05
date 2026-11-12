import React from "react";
import { fn } from "storybook/test";
import RoleEmailDeleteModal from "main/components/Users/RoleEmailDeleteModal";

export default {
  title: "components/Users/RoleEmailDeleteModal",
  component: RoleEmailDeleteModal,
};

const Template = (args) => {
  return <RoleEmailDeleteModal {...args} />;
};

export const Default = Template.bind({});
Default.args = {
  show: true,
  email: "admin1@ucsb.edu",
  onHide: fn(),
  onConfirm: fn(),
};
