import { diningCommonsName } from "main/utils/diningCommonsNames";

describe("diningCommonsName", () => {
  test("maps known codes to display names", () => {
    expect(diningCommonsName("carrillo")).toBe("Carrillo");
    expect(diningCommonsName("de-la-guerra")).toBe("De La Guerra");
    expect(diningCommonsName("ortega")).toBe("Ortega");
    expect(diningCommonsName("portola")).toBe("Portola");
  });

  test("falls back to the code for unknown commons", () => {
    expect(diningCommonsName("new-commons")).toBe("new-commons");
  });
});
