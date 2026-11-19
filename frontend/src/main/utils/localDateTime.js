// Formats a Date as "YYYY-MM-DDTHH:MM" in the user's local time zone, which is
// what a datetime-local input expects. (toISOString() alone would give UTC.)
export function localDateTimeString(date) {
  const offsetMs = date.getTimezoneOffset() * 60000;
  return new Date(date.getTime() - offsetMs).toISOString().slice(0, 16);
}
