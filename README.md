# VoteTrace

VoteTrace is a Java Swing demonstration of file integrity and digital
signatures for synthetic electoral-roll records. It is an academic prototype,
not an official Election Commission of India system.

## Features

- Sign-in using a username and password stored as a BCrypt hash in MySQL.
- SHA-256 digest generation and saving.
- RSA-2048 key-pair generation, file signing, and signature verification.
- MySQL activity logging for sign-in attempts, sign-out, file selection,
  hashing, saving digests, key generation, signing, verification, workspace
  reset, and application close.
- Activity records include the user, time, action, result, filename (not its
  full path), and a short detail. Passwords, file contents, signatures, and
  private-key material are not logged. Logs are not automatically deleted.

All signed files and keys remain local. The database stores account records and
activity metadata; it does not store electoral-roll contents or private keys.

## Requirements

- JDK 21
- Maven 3.9 or later
- MySQL 8 or later, available at `localhost:3306`

## MySQL setup

Use MySQL Workbench with your administrative MySQL account:

1. Open `database/setup.sql`. Replace
   `CHANGE_THIS_TO_A_STRONG_PASSWORD` with a strong password for the dedicated
   `votetrace_app` database account, then execute the script.
2. Open and execute `database/schema.sql`. It creates the `app_user` and
   `activity_log` tables and grants the application account only the access it
   needs.
3. Keep the database-account password available locally. Do not commit it or
   send it in chat.

If `votetrace_app` already exists, `CREATE USER IF NOT EXISTS` will not change
its password. Update it in Workbench with `ALTER USER` and use the same new
password in the local environment variable.

The application uses these environment variables. The URL and username have
local defaults; the password must be supplied:

```powershell
$env:VOTETRACE_DB_URL = "jdbc:mysql://localhost:3306/votetrace?serverTimezone=UTC"
$env:VOTETRACE_DB_USER = "votetrace_app"
$env:VOTETRACE_DB_PASSWORD = "the-password-you-set-in-MySQL"
```

## Create an application account

Application users are provisioned manually; there is no public registration or
role-based access. In an interactive PowerShell terminal, run:

```powershell
mvn exec:java "-Dexec.mainClass=com.votetrace.lite.UserProvisioner"
```

The command prompts for a username and password, confirms the password, stores
only a BCrypt hash, and writes an `ACCOUNT_CREATED` activity record. Usernames
must be 3–100 characters; passwords must be at least 8 characters and no more
than 72 UTF-8 bytes.

## Test, build, and run

From the project directory:

```powershell
mvn test
mvn clean package
mvn exec:java "-Dexec.mainClass=com.votetrace.lite.Main"
```

Set the MySQL environment variables in the same PowerShell window before
running either `exec:java` command.

To inspect recent activity in MySQL Workbench:

```sql
SELECT event_time, username, action, outcome, file_name, details
FROM votetrace.activity_log
ORDER BY event_time DESC;
```

## Security limitations

This desktop app connects directly to MySQL. Local authentication is useful for
a demonstration, but it is not strong protection against someone who can
inspect or modify the client or access the database. Use fictional records
only. Keep signing private keys local and secret.
