import React from "react";
import { fn } from "storybook/test";
import RoleEmailAddModal from "main/components/Users/RoleEmailAddModal";

export default {
  title: "components/Users/RoleEmailAddModal",
  component: RoleEmailAddModal,
};

const Template = (args) => {
  return <RoleEmailAddModal {...args} />;
};

export const Default = Template.bind({});
Default.args = {
  show: true,
  onHide: fn(),
  onSubmitEmail: fn(),
};

export const AddModerator = Template.bind({});
AddModerator.args = {
  show: true,
  onHide: fn(),
  onSubmitEmail: fn(),
  title: "Add Moderator",
  buttonLabel: "Add",
};

export const WithServerError = Template.bind({});
WithServerError.args = {
  show: true,
  onHide: fn(),
  onSubmitEmail: fn(),
  title: "Add Admin",
  buttonLabel: "Add",
  serverError: "admin1@ucsb.edu is already an admin",
};
