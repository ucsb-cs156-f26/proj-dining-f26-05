// Display names for dining commons codes used by the UCSB Dining API.
const diningCommonsNames = {
  carrillo: "Carrillo",
  "de-la-guerra": "De La Guerra",
  ortega: "Ortega",
  portola: "Portola",
};

export function diningCommonsName(code) {
  return diningCommonsNames[code] ?? code;
}
