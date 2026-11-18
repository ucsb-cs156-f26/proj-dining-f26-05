import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import ReviewForm from "main/components/MyReviews/ReviewForm";
import { vi } from "vitest";

describe("ReviewForm tests", () => {
  test("renders provided initial values and trims datetime-local seconds", () => {
    render(
      <ReviewForm
        initialItemName="Pasta"
        initialComments="Great texture"
        initialStars={4}
        initialDateServed="2024-04-01T12:34:56"
        submitAction={vi.fn()}
      />,
    );

    expect(screen.getByLabelText("Item Name")).toHaveValue("Pasta");
    expect(screen.getByLabelText("Item Name")).toBeDisabled();
    expect(screen.getByTestId("ReviewForm-itemName")).toHaveValue("Pasta");
    expect(screen.getByLabelText("Comments")).toHaveValue("Great texture");
    expect(screen.getByLabelText("Stars (1 to 5)")).toHaveValue("4");
    expect(screen.getByLabelText("Date and Time Item was Served")).toHaveValue(
      "2024-04-01T12:34",
    );
  });

  test("updates form state when initial values change", () => {
    const submitAction = vi.fn();
    const { rerender } = render(
      <ReviewForm
        initialItemName="Pasta"
        initialComments="First comment"
        initialStars={2}
        initialDateServed="2024-04-01T12:34:56"
        submitAction={submitAction}
      />,
    );

    fireEvent.change(screen.getByLabelText("Comments"), {
      target: { value: "Locally edited comment" },
    });
    fireEvent.change(screen.getByLabelText("Stars (1 to 5)"), {
      target: { value: "3" },
    });
    fireEvent.change(screen.getByLabelText("Date and Time Item was Served"), {
      target: { value: "2024-04-03T16:20" },
    });

    rerender(
      <ReviewForm
        initialItemName="Pasta"
        initialComments="Updated from backend"
        initialStars={5}
        initialDateServed="2024-04-02T13:45:59"
        submitAction={submitAction}
      />,
    );

    expect(screen.getByLabelText("Comments")).toHaveValue(
      "Updated from backend",
    );
    expect(screen.getByLabelText("Stars (1 to 5)")).toHaveValue("5");
    expect(screen.getByLabelText("Date and Time Item was Served")).toHaveValue(
      "2024-04-02T13:45",
    );
  });

  test("does not overwrite the current date when no new initial date is provided", async () => {
    const submitAction = vi.fn();
    const { rerender } = render(
      <ReviewForm
        initialItemName="Pasta"
        initialComments="First comment"
        initialStars={2}
        initialDateServed="2024-04-01T12:34:56"
        submitAction={submitAction}
      />,
    );

    fireEvent.change(screen.getByLabelText("Date and Time Item was Served"), {
      target: { value: "2024-04-03T16:20" },
    });

    rerender(
      <ReviewForm
        initialItemName="Pasta"
        initialComments="Updated from backend"
        initialStars={5}
        submitAction={submitAction}
      />,
    );

    expect(screen.getByLabelText("Comments")).toHaveValue(
      "Updated from backend",
    );
    expect(screen.getByLabelText("Stars (1 to 5)")).toHaveValue("5");
    expect(screen.getByLabelText("Date and Time Item was Served")).toHaveValue(
      "2024-04-03T16:20",
    );

    fireEvent.click(screen.getByRole("button", { name: "Submit Review" }));
    await waitFor(() => expect(submitAction).toHaveBeenCalled());
    expect(submitAction.mock.calls[0][0]).toEqual({
      reviewerComments: "Updated from backend",
      itemsStars: 5,
      dateItemServed: "2024-04-03T16:20",
    });
  });
  test("submits the entered values, with stars as a number", async () => {
    const submitAction = vi.fn();
    render(
      <ReviewForm
        initialItemName="Pasta"
        initialDateServed="2024-04-01T12:34:56"
        submitAction={submitAction}
      />,
    );

    fireEvent.change(screen.getByTestId("ReviewForm-reviewerComments"), {
      target: { value: "Tasty" },
    });
    fireEvent.change(screen.getByTestId("ReviewForm-itemsStars"), {
      target: { value: "3" },
    });
    fireEvent.change(screen.getByTestId("ReviewForm-dateItemServed"), {
      target: { value: "2024-04-05T18:30" },
    });
    fireEvent.click(screen.getByTestId("ReviewForm-submit"));

    await waitFor(() => expect(submitAction).toHaveBeenCalled());
    expect(submitAction.mock.calls[0][0]).toEqual({
      reviewerComments: "Tasty",
      itemsStars: 3,
      dateItemServed: "2024-04-05T18:30",
    });
  });

  test("shows an error and does not submit when the date is empty", async () => {
    const submitAction = vi.fn();
    render(<ReviewForm submitAction={submitAction} />);

    fireEvent.change(screen.getByTestId("ReviewForm-dateItemServed"), {
      target: { value: "" },
    });
    fireEvent.click(screen.getByTestId("ReviewForm-submit"));

    expect(
      await screen.findByText("Date and time served is required."),
    ).toBeInTheDocument();
    expect(screen.getByTestId("ReviewForm-dateItemServed")).toHaveClass(
      "is-invalid",
    );
    expect(submitAction).not.toHaveBeenCalled();
  });

  // The select only offers 1-5, so add an out-of-range option to exercise
  // the min/max/required rules.
  const submitWithStars = async (value) => {
    const submitAction = vi.fn();
    render(<ReviewForm submitAction={submitAction} />);
    const select = screen.getByTestId("ReviewForm-itemsStars");
    const option = document.createElement("option");
    option.value = value;
    option.textContent = value;
    select.appendChild(option);
    fireEvent.change(select, { target: { value } });
    fireEvent.click(screen.getByTestId("ReviewForm-submit"));
    return { submitAction, select };
  };

  test("shows an error when stars are above 5", async () => {
    const { submitAction, select } = await submitWithStars("6");
    expect(
      await screen.findByText("Stars must be at most 5."),
    ).toBeInTheDocument();
    expect(select).toHaveClass("is-invalid");
    expect(submitAction).not.toHaveBeenCalled();
  });

  test("shows an error when stars are below 1", async () => {
    const { submitAction } = await submitWithStars("0");
    expect(
      await screen.findByText("Stars must be at least 1."),
    ).toBeInTheDocument();
    expect(submitAction).not.toHaveBeenCalled();
  });

  test("shows an error when stars are missing", async () => {
    const { submitAction } = await submitWithStars("");
    expect(await screen.findByText("Stars are required.")).toBeInTheDocument();
    expect(submitAction).not.toHaveBeenCalled();
  });
});
