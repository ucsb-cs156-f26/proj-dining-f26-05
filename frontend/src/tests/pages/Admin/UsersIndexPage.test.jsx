import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "react-query";
import { MemoryRouter } from "react-router";
import AxiosMockAdapter from "axios-mock-adapter";
import axios from "axios";
import UsersIndexPage from "main/pages/Admin/UsersIndexPage";
import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import usersFixtures from "fixtures/usersFixtures";

// Builds users user1..userN
const makeUsers = (n) =>
  Array.from({ length: n }, (_, i) => ({
    id: i + 1,
    email: `user${i + 1}@ucsb.edu`,
    givenName: `First${i + 1}`,
    familyName: `Last${i + 1}`,
    admin: false,
    moderator: false,
  }));

// Returns what GET /api/admin/users/paged would return for this page and size
// (the JSON form of a Spring Page).
const pageOf = (allUsers, page, size) => ({
  content: allUsers.slice(page * size, (page + 1) * size),
  totalElements: allUsers.length,
  totalPages: Math.ceil(allUsers.length / size),
  number: page,
  size: size,
});

describe("UsersIndexPage tests", () => {
  const axiosMock = new AxiosMockAdapter(axios);

  const setup = () => {
    axiosMock.reset();
    axiosMock.resetHistory();
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.adminUser);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
  };

  // Makes the mocked backend page through allUsers like the real endpoint does.
  const backendHasUsers = (allUsers) => {
    axiosMock
      .onGet("/api/admin/users/paged")
      .reply((config) => [
        200,
        pageOf(allUsers, config.params.page, config.params.size),
      ]);
  };

  const pagedRequests = () =>
    axiosMock.history.get.filter((r) => r.url === "/api/admin/users/paged");

  const renderPage = () =>
    render(
      <QueryClientProvider client={new QueryClient()}>
        <MemoryRouter>
          <UsersIndexPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

  test("renders without crashing", async () => {
    setup();
    backendHasUsers([]);

    renderPage();

    expect(await screen.findByText("Users")).toBeInTheDocument();
  });

  test("shows Page 1 of 1 and disables both buttons before data arrives and when there are no users", async () => {
    setup();
    backendHasUsers([]);

    renderPage();

    expect(
      await screen.findByTestId("UsersIndexPage-page-info"),
    ).toHaveTextContent("Page 1 of 1");
    expect(screen.getByTestId("UsersIndexPage-prev")).toBeDisabled();
    expect(screen.getByTestId("UsersIndexPage-next")).toBeDisabled();
    await waitFor(() => expect(pagedRequests().length).toBe(1));
  });

  test("shows an empty table while the first page is still loading", async () => {
    setup();
    // never answer, so the page keeps showing its initial (empty) data
    axiosMock
      .onGet("/api/admin/users/paged")
      .reply(() => new Promise(() => {}));

    renderPage();

    expect(
      await screen.findByTestId("UsersIndexPage-page-info"),
    ).toHaveTextContent("Page 1 of 1");
    expect(
      screen.queryByTestId("UsersTable-cell-row-0-col-id"),
    ).not.toBeInTheDocument();
  });

  test("requests the first page with the default size from the paged endpoint", async () => {
    setup();
    backendHasUsers(usersFixtures.threeUsers);

    renderPage();

    await waitFor(() => expect(pagedRequests().length).toBe(1));
    expect(pagedRequests()[0].method).toBe("get");
    expect(pagedRequests()[0].params).toEqual({ page: 0, size: 10 });
  });

  test("renders three users in the table", async () => {
    setup();
    backendHasUsers(usersFixtures.threeUsers);

    renderPage();

    expect(await screen.findByText("phtcon@ucsb.edu")).toBeInTheDocument();
    expect(screen.getByText("pconrad.cis@gmail.com")).toBeInTheDocument();
    expect(screen.getByText("craig.zzyzx@example.org")).toBeInTheDocument();
    expect(
      screen.getByTestId("UsersTable-cell-row-0-col-id"),
    ).toBeInTheDocument();
  });

  test("page size selector has correct options", async () => {
    setup();
    backendHasUsers([]);

    renderPage();

    const select = await screen.findByTestId("UsersIndexPage-pageSize");
    expect(select).toBeInTheDocument();
    expect(select).toHaveValue("10");

    const options = select.querySelectorAll("option");
    const values = Array.from(options).map((o) => o.value);
    expect(values).toEqual(["10", "25", "50", "100", "500"]);
  });

  test("next and previous load the next and previous page from the backend", async () => {
    setup();
    backendHasUsers(makeUsers(15));

    renderPage();

    expect(await screen.findByText("user1@ucsb.edu")).toBeInTheDocument();
    expect(screen.getByTestId("UsersIndexPage-page-info")).toHaveTextContent(
      "Page 1 of 2",
    );
    expect(screen.queryByText("user11@ucsb.edu")).not.toBeInTheDocument();

    fireEvent.click(screen.getByTestId("UsersIndexPage-next"));

    expect(await screen.findByText("user11@ucsb.edu")).toBeInTheDocument();
    expect(screen.getByTestId("UsersIndexPage-page-info")).toHaveTextContent(
      "Page 2 of 2",
    );
    expect(screen.queryByText("user1@ucsb.edu")).not.toBeInTheDocument();
    expect(screen.getByTestId("UsersIndexPage-next")).toBeDisabled();
    expect(screen.getByTestId("UsersIndexPage-prev")).not.toBeDisabled();
    expect(pagedRequests().map((r) => r.params)).toContainEqual({
      page: 1,
      size: 10,
    });

    fireEvent.click(screen.getByTestId("UsersIndexPage-prev"));

    expect(await screen.findByText("user1@ucsb.edu")).toBeInTheDocument();
    expect(screen.getByTestId("UsersIndexPage-page-info")).toHaveTextContent(
      "Page 1 of 2",
    );
    expect(screen.queryByText("user11@ucsb.edu")).not.toBeInTheDocument();
  });

  test("prev and next are disabled when everything fits on one page", async () => {
    setup();
    backendHasUsers(usersFixtures.threeUsers);

    renderPage();

    expect(await screen.findByText("phtcon@ucsb.edu")).toBeInTheDocument();
    expect(screen.getByTestId("UsersIndexPage-prev")).toBeDisabled();
    expect(screen.getByTestId("UsersIndexPage-next")).toBeDisabled();
  });

  test("prev from page 3 goes to page 2 (not page 1)", async () => {
    setup();
    // 21 users -> 3 pages at the default page size of 10
    backendHasUsers(makeUsers(21));

    renderPage();

    expect(await screen.findByText("user1@ucsb.edu")).toBeInTheDocument();

    fireEvent.click(screen.getByTestId("UsersIndexPage-next"));
    expect(await screen.findByText("user11@ucsb.edu")).toBeInTheDocument();
    fireEvent.click(screen.getByTestId("UsersIndexPage-next"));
    expect(await screen.findByText("user21@ucsb.edu")).toBeInTheDocument();
    expect(screen.getByTestId("UsersIndexPage-page-info")).toHaveTextContent(
      "Page 3 of 3",
    );

    fireEvent.click(screen.getByTestId("UsersIndexPage-prev"));

    expect(await screen.findByText("user11@ucsb.edu")).toBeInTheDocument();
    expect(screen.getByTestId("UsersIndexPage-page-info")).toHaveTextContent(
      "Page 2 of 3",
    );
    expect(screen.queryByText("user1@ucsb.edu")).not.toBeInTheDocument();
    expect(screen.queryByText("user21@ucsb.edu")).not.toBeInTheDocument();
  });

  test("changing page size resets to the first page and requests the new size", async () => {
    setup();
    backendHasUsers(makeUsers(15));

    renderPage();

    expect(await screen.findByText("user1@ucsb.edu")).toBeInTheDocument();

    // go to page 2
    fireEvent.click(screen.getByTestId("UsersIndexPage-next"));
    expect(await screen.findByText("user11@ucsb.edu")).toBeInTheDocument();
    expect(screen.getByTestId("UsersIndexPage-page-info")).toHaveTextContent(
      "Page 2 of 2",
    );

    // change page size to 25 (all 15 fit on one page)
    fireEvent.change(screen.getByTestId("UsersIndexPage-pageSize"), {
      target: { value: "25" },
    });

    expect(await screen.findByText("user1@ucsb.edu")).toBeInTheDocument();
    expect(screen.getByText("user15@ucsb.edu")).toBeInTheDocument();
    expect(screen.getByTestId("UsersIndexPage-page-info")).toHaveTextContent(
      "Page 1 of 1",
    );
    expect(screen.getByTestId("UsersIndexPage-pageSize")).toHaveValue("25");
    expect(pagedRequests().map((r) => r.params)).toContainEqual({
      page: 0,
      size: 25,
    });
  });
});
