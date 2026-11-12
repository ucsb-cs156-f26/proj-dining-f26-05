const roleEmailFixtures = {
  threeAdmins: [
    { email: "admin1@ucsb.edu", isInAdminEmails: false },
    { email: "admin2@ucsb.edu", isInAdminEmails: false },
    { email: "superadmin@ucsb.edu", isInAdminEmails: true },
  ],
  threeModerators: [
    { email: "mod1@ucsb.edu" },
    { email: "mod2@ucsb.edu" },
    { email: "mod3@ucsb.edu" },
  ],
  empty: [],
};

export { roleEmailFixtures };
