# Library Management System

A desktop Java application (Swing GUI) backed by a relational database (MySQL by default,
Postgres-compatible with minor tweaks) for managing books, members, and loans.

## Features

- **Books**: add, edit, delete, search by title/author/ISBN, track total vs. available copies.
- **Members**: add, edit, delete, search, status tracking (ACTIVE / SUSPENDED / EXPIRED).
- **Loans**: borrow a book (auto-decrements available copies, sets a 14-day due date), return a
  book (auto-increments available copies, calculates a $0.50/day overdue fine), view active,
  overdue, or full loan history.
- All borrow/return operations run inside a database transaction so book-copy counts and loan
  records never fall out of sync.

## Project Structure

```
library-management-system/
├── pom.xml                          Maven build file (MySQL JDBC driver + shade plugin)
├── sql/schema.sql                   Database schema + sample seed data
├── src/main/resources/db.properties Database connection settings — EDIT THIS
└── src/main/java/com/library/
    ├── LibraryManagementApp.java    Entry point
    ├── model/                       Book, Member, Loan (plain data classes)
    ├── dao/                         BookDAO, MemberDAO, LoanDAO (all SQL lives here)
    ├── util/DBConnection.java       JDBC connection helper
    └── gui/                         MainFrame, BookPanel, MemberPanel, LoanPanel (Swing)
```

## Setup

### 1. Create the database

Install MySQL 8+ if you don't have it, then run:

```bash
mysql -u root -p < sql/schema.sql
```

This creates the `library_db` database, its tables, and a few sample rows.

> **Using PostgreSQL instead?** Change `AUTO_INCREMENT` → `SERIAL`, drop the `ENUM(...)` types in
> favor of `VARCHAR` + a `CHECK` constraint, and swap the JDBC dependency in `pom.xml` for
> `org.postgresql:postgresql`.

### 2. Configure the connection

Edit `src/main/resources/db.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/library_db?useSSL=false&serverTimezone=UTC
db.user=root
db.password=changeme
```

### 3. Build and run

With Maven installed:

```bash
mvn clean package
java -jar target/library-management-system.jar
```

The shade plugin bundles the MySQL JDBC driver into the jar, so no separate classpath setup is
needed.

**Without Maven** (manual compile), download `mysql-connector-j` (a `.jar`) yourself and:

```bash
javac -cp mysql-connector-j-8.3.0.jar -d out $(find src/main/java -name "*.java")
cp src/main/resources/db.properties out/
java -cp "out:mysql-connector-j-8.3.0.jar" com.library.LibraryManagementApp
```

(On Windows, use `;` instead of `:` as the classpath separator.)

## Notes / Possible Extensions

- Add authentication (librarian login) — currently anyone running the app has full access.
- Add reservation/holds for books that are fully checked out.
- Add pagination for large catalogs (`getAllBooks()` currently loads everything at once).
- Export loan/fine reports to CSV or PDF.
- Automatically flip loan `status` to `OVERDUE` via a scheduled job or a DB event — currently
  "overdue" is computed on read (`due_date < CURDATE()`), which is simpler and always correct,
  but doesn't write the flag back to the row.
