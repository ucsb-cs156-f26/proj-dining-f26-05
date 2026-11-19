import { localDateTimeString } from "main/utils/localDateTime";
import { vi } from "vitest";

describe("localDateTimeString", () => {
  test("localDateTimeString formats a Date in local time, not UTC", () => {
    // 01:00 UTC on Nov 18 is 17:00 on Nov 17 in Pacific Standard Time (UTC-8)
    const pst = new Date(Date.UTC(2026, 10, 18, 1, 0));
    vi.spyOn(pst, "getTimezoneOffset").mockReturnValue(480);
    expect(localDateTimeString(pst)).toBe("2026-11-17T17:00");

    // 23:30 UTC on Nov 17 is 00:30 on Nov 18 in UTC+1
    const cet = new Date(Date.UTC(2026, 10, 17, 23, 30));
    vi.spyOn(cet, "getTimezoneOffset").mockReturnValue(-60);
    expect(localDateTimeString(cet)).toBe("2026-11-18T00:30");
  });
});
