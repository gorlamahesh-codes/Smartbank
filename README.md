# SmartBank – Professional Banking Management System

Educational desktop project upgraded with a modern professional UI.  
**Stack:** Java 17 · Swing · FlatLaf · JDBC · MySQL · Maven · BCrypt

---

## Features

### Customer Portal
- Modern welcome dashboard with total balance, active accounts, monthly deposits & withdrawals
- Interactive monthly volume bar chart (deposits vs withdrawals)
- Recent transactions table
- Quick-action buttons (Deposit / Withdraw / Transfer)
- My Accounts view
- Searchable full transaction history
- Editable customer profile

### Administrator Portal
- Bank-wide metrics: customers, accounts, total balance, open fraud alerts
- Customer management table
- All accounts overview
- Transaction monitoring (searchable)
- Fraud alert review (mark Reviewed / Cleared)
- Audit log viewer
- Create bank accounts for customers

### Security & Backend
- BCrypt password hashing
- Role-based navigation (ADMIN / CUSTOMER)
- Customer account ownership enforcement on deposit / withdraw / transfer
- JDBC transactions + SELECT … FOR UPDATE row locks
- Negative-balance prevention & amount validation
- Large-transaction fraud flagging (> ₹50,000)
- PreparedStatement throughout
- Existing MySQL schema preserved

---

## Setup

1. Install **JDK 17+**, **Maven**, and **MySQL Server**.
2. Open MySQL Workbench (or CLI) and execute:
   ```sql
   SOURCE database/smartbank.sql;
   ```
3. Copy the example config:
   ```bash
   cp src/main/resources/db.properties.example src/main/resources/db.properties
   ```
4. Edit `src/main/resources/db.properties` with your MySQL credentials.
5. Build & run:
   ```bash
   mvn clean compile
   mvn exec:java
   ```
6. Register a customer from the login screen.

### Create the first bank account
Registration creates a user + customer record only.  
Find the `customer_id`:
```sql
SELECT customer_id, full_name FROM customers;
```
Then use **Create Account** (admin) or ask an administrator.

### Promote a user to administrator
```sql
UPDATE users SET role='ADMIN' WHERE username='your_username';
```
Log out and log in again — the admin dashboard loads automatically.

---

## Project structure

```
SmartBank/
├── pom.xml
├── database/smartbank.sql
├── src/main/java/com/smartbank/
│   ├── Main.java
│   ├── config/DBConnection.java
│   ├── dao/UserDao.java, BankDao.java
│   ├── model/SessionUser.java, AccountInfo.java, CustomerInfo.java, DashboardStats.java
│   ├── security/PasswordUtil.java
│   ├── service/BankService.java
│   └── ui/SmartBankFrame.java
└── src/main/resources/db.properties.example
```

---

## Important limitations

This is a learning / demo project, **not** production banking software.  
It does not implement KYC/AML, MFA, encryption key management, maker-checker workflows, rate limiting, or hardened deployment.  
Do **not** use real customer data or connect it to real funds.

---

## Design notes

- Palette: Navy (#1A2A40), accent blue (#2962B4), clean white cards, light gray background
- FlatLaf look-and-feel with subtle rounded corners
- Sidebar with active-page highlighting
- Card-based metric tiles and content panels
- Pure Swing bar chart (no extra charting library required)
- Searchable tables with live filter
