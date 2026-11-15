import React from "react";
import { http, HttpResponse } from "msw";
import RoleEmailTable from "main/components/Users/RoleEmailTable";
import { roleEmailFixtures } from "fixtures/roleEmailFixtures";

export default {
  title: "components/Users/RoleEmailTable",
  component: RoleEmailTable,
};

const Template = (args) => {
  return <RoleEmailTable {...args} />;
};

export const ThreeAdmins = Template.bind({});
ThreeAdmins.args = {
  data: roleEmailFixtures.threeAdmins,
  deleteEndpoint: "/api/admin/delete",
  getEndpoint: "/api/admin/all",
  testIdPrefix: "RoleEmailTable",
};
ThreeAdmins.parameters = {
  msw: [
    http.delete("/api/admin/delete", ({ request }) => {
      const email = new URL(request.url).searchParams.get("email");
      return HttpResponse.json({ message: `Admin with id ${email} deleted` });
    }),
  ],
};

export const ThreeModerators = Template.bind({});
ThreeModerators.args = {
  data: roleEmailFixtures.threeModerators,
  deleteEndpoint: "/api/admin/moderators/delete",
  getEndpoint: "/api/admin/moderators/all",
  testIdPrefix: "RoleEmailTable",
};
ThreeModerators.parameters = {
  msw: [
    http.delete("/api/admin/moderators/delete", ({ request }) => {
      const email = new URL(request.url).searchParams.get("email");
      return HttpResponse.json({
        message: `Moderator with id ${email} deleted`,
      });
    }),
  ],
};

export const Empty = Template.bind({});
Empty.args = {
  data: roleEmailFixtures.empty,
  deleteEndpoint: "/api/admin/delete",
  getEndpoint: "/api/admin/all",
  testIdPrefix: "RoleEmailTable",
};
