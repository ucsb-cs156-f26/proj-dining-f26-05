import React from "react";
import { http, HttpResponse } from "msw";
import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import { roleEmailFixtures } from "fixtures/roleEmailFixtures";
import ModeratorsIndexPage from "main/pages/Admin/ModeratorsIndexPage";

export default {
  title: "pages/Admin/ModeratorsIndexPage",
  component: ModeratorsIndexPage,
};

// In-memory moderators list so that adding/deleting in the story updates the table.
// reset() runs before each story render so every visit starts from the fixture.
const makeMockBackend = (initialModerators) => {
  let moderators = [];
  return {
    reset: () => {
      moderators = [...initialModerators];
    },
    handlers: [
      http.get("/api/currentUser", () => {
        return HttpResponse.json(apiCurrentUserFixtures.adminUser);
      }),
      http.get("/api/systemInfo", () => {
        return HttpResponse.json(systemInfoFixtures.showingNeither);
      }),
      http.get("/api/admin/moderators/all", () => {
        return HttpResponse.json(moderators);
      }),
      http.post("/api/admin/moderators/post", ({ request }) => {
        const email = new URL(request.url).searchParams.get("email");
        if (moderators.some((moderator) => moderator.email === email)) {
          return HttpResponse.json(
            { message: `${email} is already a moderator` },
            { status: 400 },
          );
        }
        moderators = [...moderators, { email }];
        return HttpResponse.json({ email });
      }),
      http.delete("/api/admin/moderators/delete", ({ request }) => {
        const email = new URL(request.url).searchParams.get("email");
        moderators = moderators.filter(
          (moderator) => moderator.email !== email,
        );
        return HttpResponse.json({
          message: `Moderator with id ${email} deleted`,
        });
      }),
    ],
  };
};

const Template = () => <ModeratorsIndexPage />;

const threeModeratorsBackend = makeMockBackend(
  roleEmailFixtures.threeModerators,
);
export const ThreeModerators = Template.bind({});
ThreeModerators.beforeEach = threeModeratorsBackend.reset;
ThreeModerators.parameters = {
  msw: threeModeratorsBackend.handlers,
};

const emptyBackend = makeMockBackend(roleEmailFixtures.empty);
export const Empty = Template.bind({});
Empty.beforeEach = emptyBackend.reset;
Empty.parameters = {
  msw: emptyBackend.handlers,
};
