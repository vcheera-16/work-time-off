## Default seeded accounts

When the application starts against an empty database, it will seed three default accounts for testing purposes (the seeder will NOT overwrite existing users).

Credentials:
- Admin: admin@example.com / AdminPass123!
- Manager: manager@example.com / ManagerPass123!
- Employee: employee@example.com / EmployeePass123!

Notes:
- After the first run the users will exist in the database and the seeder will skip.
- For additional test users, use the registration endpoint or insert directly into the DB.
