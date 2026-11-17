import React from "react";
import { http, HttpResponse } from "msw";
import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import { roleEmailFixtures } from "fixtures/roleEmailFixtures";
import AdminsIndexPage from "main/pages/Admin/AdminsIndexPage";

export default {
  title: "pages/Admin/AdminsIndexPage",
  component: AdminsIndexPage,
};

// In-memory admins list so that adding/deleting in the story updates the table.
// reset() runs before each story render so every visit starts from the fixture.
const makeMockBackend = (initialAdmins) => {
  let admins = [];
  return {
    reset: () => {
      admins = [...initialAdmins];
    },
    handlers: [
      http.get("/api/currentUser", () => {
        return HttpResponse.json(apiCurrentUserFixtures.adminUser);
      }),
      http.get("/api/systemInfo", () => {
        return HttpResponse.json(systemInfoFixtures.showingNeither);
      }),
      http.get("/api/admin/all", () => {
        return HttpResponse.json(admins);
      }),
      http.post("/api/admin/post", ({ request }) => {
        const email = new URL(request.url).searchParams.get("email");
        if (admins.some((admin) => admin.email === email)) {
          return HttpResponse.json(
            { message: `${email} is already an admin` },
            { status: 400 },
          );
        }
        admins = [...admins, { email, isInAdminEmails: false }];
        return HttpResponse.json({ email });
      }),
      http.delete("/api/admin/delete", ({ request }) => {
        const email = new URL(request.url).searchParams.get("email");
        admins = admins.filter((admin) => admin.email !== email);
        return HttpResponse.json({ message: `Admin with id ${email} deleted` });
      }),
    ],
  };
};

const Template = () => <AdminsIndexPage />;

const threeAdminsBackend = makeMockBackend(roleEmailFixtures.threeAdmins);
export const ThreeAdmins = Template.bind({});
ThreeAdmins.beforeEach = threeAdminsBackend.reset;
ThreeAdmins.parameters = {
  msw: threeAdminsBackend.handlers,
};
