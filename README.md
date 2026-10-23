# Fruit Shop Management

A native Kotlin Android application for a four-table SQLite university assignment. It uses XML layouts, Material components, RecyclerView, ViewBinding, `SQLiteOpenHelper`, and SharedPreferences. All data stays on the Android device. There is no server or network permission.

## Open and run

1. Install Android Studio with Android SDK platform 37 and a JDK compatible with Gradle 9.6 / Android Gradle Plugin 9.4.
2. Open this `FruitShopManagement` directory in Android Studio.
3. Let Gradle sync, then run the `app` configuration on an Android emulator or device with Android 8.0 (API 26) or newer.
4. On first launch, the database seeds three fruits and two demo accounts:
   - Admin: `admin` / `Admin123!`
   - Customer: `customer` / `Customer123!`

The demo passwords are development-only seed data. They are salted and hashed with PBKDF2-HMAC-SHA256 before being stored in SQLite. SharedPreferences stores the account ID, username, and role, never a password. Login checks the stored hash. This is a local demonstration: an attacker who can alter the app or its local data can bypass local role checks. It is not server-side authentication.

## Exact database schema

Database: `fruit_shop.db`, version 1. The application creates exactly four tables (SQLite may create its own internal `sqlite_sequence` table for `AUTOINCREMENT`):

```sql
CREATE TABLE Account (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL UNIQUE,
  password TEXT NOT NULL, role TEXT NOT NULL CHECK(role IN ('Admin','Customer')));
CREATE TABLE Fruit (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, category TEXT,
  price REAL NOT NULL, quantity INTEGER NOT NULL, expiryDate TEXT NOT NULL, description TEXT);
CREATE TABLE Customer (id INTEGER PRIMARY KEY AUTOINCREMENT, fullname TEXT NOT NULL,
  phone TEXT, email TEXT, accountId INTEGER NOT NULL,
  FOREIGN KEY(accountId) REFERENCES Account(id));
CREATE TABLE Orders (id INTEGER PRIMARY KEY AUTOINCREMENT, customerId INTEGER NOT NULL,
  orderDate TEXT NOT NULL, totalAmount REAL NOT NULL,
  status TEXT NOT NULL CHECK(status IN ('Pending','Confirmed','Completed')),
  FOREIGN KEY(customerId) REFERENCES Customer(id));
```

Foreign keys are enabled in `onConfigure`. Database version 1 is the initial schema; `onUpgrade` deliberately stops an unsupported upgrade instead of dropping user data. A future schema change needs a versioned migration.

## How ordering works within four tables

Each checkout purchases one fruit and a positive quantity. A confirmation dialog shows the fruit, quantity, and calculated total. At checkout the app reads current fruit data, checks that expiry is today or later, checks stock and price, inserts an `Orders` row, and reduces stock in one SQLite transaction. A failed check or update rolls back the order and stock change. Fruits expiring today are purchasable. Dates use `yyyy-MM-dd`.

**Assignment schema limitation:** `Orders` has no fruit ID, quantity, or line items. After checkout, the database cannot identify which fruit or quantity was purchased. The order detail screen can therefore show only the order ID, customer, date, total, and status. It cannot truthfully show purchased item details or reconstruct a historical price. This is inherent in the required schema. Add an order-detail table only if the assignment rules are relaxed.

## Project map

- `app/src/main/java/com/example/fruitshop/database/DatabaseHelper.kt`: schema, seed data, queries, transaction
- `app/src/main/java/com/example/fruitshop/model/`: Account, Fruit, Customer, Order
- `app/src/main/java/com/example/fruitshop/ui/`: login, dashboards, fruit and order screens
- `app/src/main/java/com/example/fruitshop/adapter/`: RecyclerView adapters
- `app/src/main/java/com/example/fruitshop/utils/`: password hashing, session, date and fruit validation
- `app/src/main/res/layout/`: XML screens and list rows

## Demo test checklist

- [ ] Admin login opens Admin dashboard; customer login opens Customer dashboard.
- [ ] Wrong credentials show an error. Relaunch restores the session. Logout clears it.
- [ ] Admin adds, edits, deletes, searches, and opens fruits. Delete asks for confirmation.
- [ ] Invalid name, price, quantity, or expiry date is rejected.
- [ ] Expired fruits have an Expired label; fruit expiring today stays purchasable.
- [ ] Customer can search and open fruits; expired and out-of-stock fruit cannot be ordered.
- [ ] Ordering more than stock fails; a valid order shows confirmation and reduces stock.
- [ ] Failed checkout leaves order count and stock unchanged.
- [ ] Customer history lists only that customer’s orders; admin sees all orders.
- [ ] Admin changes statuses among Pending, Confirmed, and Completed.
- [ ] Reopening an order displays its saved total and current status.
- [ ] Attempting an invalid foreign key insert is rejected by SQLite.

Screenshots to capture for submission: login, each dashboard, fruit list with expiry labels, add/edit form, fruit detail, checkout confirmation, customer history, order detail, and admin order management.

## GitHub

Create an empty repository on GitHub, then in this directory run:

```bash
git init
git add .
git commit -m "Build four-table Fruit Shop Management Android app"
git branch -M main
git remote add origin https://github.com/YOUR_NAME/fruit-shop-management.git
git push -u origin main
```

For collaboration, each contributor creates a branch (`git switch -c feature/name`), commits focused changes, pushes that branch, and opens a pull request into `main`. Suggested milestones: project setup; database and models; login and sessions; admin fruit management; customer browsing and checkout; order history and status management; test and polish. Never commit `local.properties`, build files, or credentials beyond the clearly marked demo seed accounts.
