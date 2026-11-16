import { useBackend } from "main/utils/useBackend";
import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import { Link, useParams } from "react-router";
import { Button } from "react-bootstrap";
import ReviewsTable from "main/components/Reviews/ReviewsTable";
import { diningCommonsName } from "main/utils/diningCommonsNames";

export default function ReviewsPage() {
  const { itemid } = useParams();
  const {
    data: reviews,
    error: _error,
    status: _status,
  } = useBackend(
    // Stryker disable next-line all : don't test internal caching of React Query
    [`/api/reviews/approved/forItem/${itemid}`],
    // Stryker disable next-line all : don't test internal caching of React Query
    { method: "GET", url: `/api/reviews/approved/forItem/${itemid}` },
    [],
  );

  const { data: menuItem } = useBackend(
    // Stryker disable next-line all : don't test internal caching of React Query
    [`/api/diningcommons/menuitem?id=${itemid}`],
    // Stryker disable next-line all : the default method is get
    { method: "GET", url: `/api/diningcommons/menuitem?id=${itemid}` },
    null,
  );

  return (
    <BasicLayout>
      <div className="pt-2">
        {menuItem ? (
          <>
            <h1 data-testid="ReviewsPage-title">Reviews for {menuItem.name}</h1>
            <p data-testid="ReviewsPage-details" className="text-muted">
              {diningCommonsName(menuItem.diningCommonsCode)} ·{" "}
              {menuItem.mealCode} · {menuItem.station}
            </p>
          </>
        ) : (
          <h1 data-testid="ReviewsPage-title">
            Reviews for Menu Item {itemid}
          </h1>
        )}
        <Button
          as={Link}
          to={`/reviews/post/${itemid}`}
          variant="warning"
          className="mb-3"
          data-testid="ReviewsPage-review-button"
        >
          Review This Item
        </Button>
        <ReviewsTable reviews={reviews} />
      </div>
    </BasicLayout>
  );
}
