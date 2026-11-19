import { useEffect, useState } from "react";
import { Form, Button } from "react-bootstrap";
import { useForm } from "react-hook-form";
import { localDateTimeString } from "main/utils/localDateTime";

export default function ReviewForm({
  initialItemName = "",
  initialComments = "",
  initialStars = 5,
  initialDateServed,
  submitAction,
  submitButtonText = "Submit Review",
}) {
  const testIdPrefix = "ReviewForm";

  // useForm only reads defaultValues on the first render, so build them once.
  // The effect below fills in the other fields from the props.
  const [defaultValues] = useState(() => ({
    dateItemServed: localDateTimeString(new Date()),
  }));

  const {
    register,
    handleSubmit,
    reset,
    getValues,
    formState: { errors },
  } = useForm({ defaultValues });

  // When the initial values change (e.g. the review finishes loading from the
  // backend), reset the form to them. reset() also clears old validation errors
  // and makes these the new baseline for isDirty. If no new date is given, keep
  // the date that is currently in the form.
  useEffect(() => {
    reset({
      reviewerComments: initialComments ?? "",
      itemsStars: initialStars,
      // datetime-local wants "YYYY-MM-DDTHH:MM", so drop any seconds
      dateItemServed: initialDateServed
        ? initialDateServed.slice(0, 16)
        : getValues().dateItemServed,
    });
  }, [initialComments, initialDateServed, initialStars, reset, getValues]);

  return (
    <Form onSubmit={handleSubmit(submitAction)}>
      <Form.Group className="mb-3">
        <Form.Label htmlFor="review-item-name">Item Name</Form.Label>
        <Form.Control
          data-testid={`${testIdPrefix}-itemName`}
          id="review-item-name"
          type="text"
          value={initialItemName}
          disabled
        />
      </Form.Group>

      <Form.Group className="mb-3">
        <Form.Label htmlFor="review-comments">Comments</Form.Label>
        <Form.Control
          data-testid={`${testIdPrefix}-reviewerComments`}
          id="review-comments"
          as="textarea"
          rows={3}
          {...register("reviewerComments")}
        />
      </Form.Group>

      <Form.Group className="mb-3">
        <Form.Label htmlFor="review-stars">Stars (1 to 5)</Form.Label>
        <Form.Select
          data-testid={`${testIdPrefix}-itemsStars`}
          id="review-stars"
          isInvalid={Boolean(errors.itemsStars)}
          {...register("itemsStars", {
            valueAsNumber: true,
            required: "Stars are required.",
            min: { value: 1, message: "Stars must be at least 1." },
            max: { value: 5, message: "Stars must be at most 5." },
          })}
        >
          {[1, 2, 3, 4, 5].map((num) => (
            <option key={num} value={num}>
              {num}
            </option>
          ))}
        </Form.Select>
        <Form.Control.Feedback type="invalid">
          {errors.itemsStars?.message}
        </Form.Control.Feedback>
      </Form.Group>

      <Form.Group className="mb-3">
        <Form.Label htmlFor="review-date">
          Date and Time Item was Served
        </Form.Label>
        <Form.Control
          data-testid={`${testIdPrefix}-dateItemServed`}
          id="review-date"
          type="datetime-local"
          isInvalid={Boolean(errors.dateItemServed)}
          {...register("dateItemServed", {
            required: "Date and time served is required.",
          })}
        />
        <Form.Control.Feedback type="invalid">
          {errors.dateItemServed?.message}
        </Form.Control.Feedback>
      </Form.Group>

      <Button type="submit" data-testid={`${testIdPrefix}-submit`}>
        {submitButtonText}
      </Button>
    </Form>
  );
}
