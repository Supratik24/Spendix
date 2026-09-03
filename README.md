# Spendix 

An Android-first, privacy-first personal finance tracker for India. Spendix turns financial SMS alerts into local, structured INR transactions.

## What is included

- Android SMS broadcast receiver for future bank, card, UPI, wallet, transfer and ATM alerts
- Optional one-time local SMS import after the user grants permission
- Deterministic parser with OTP/statement filtering and SHA-256 duplicate protection
- On-device rules-based categorizer for common Indian merchants and transaction types
- Room database; no server, account, analytics SDK, or financial data upload
- Jetpack Compose dashboard with income, spending, balance, recent activity, and a top-category insight
- Live month-level pie breakdown, manual transactions, category corrections, and transaction deletion
- Idempotent SMS re-sync: already processed messages are ignored

## Privacy and Play policy note

The app asks for `READ_SMS` only to import historical messages and `RECEIVE_SMS` for automatic tracking. Google Play tightly restricts these permissions: before public Play distribution, ensure the product qualifies for the applicable SMS permission declaration and complete Play Console review. The app deliberately does not store raw SMS bodies after parsing.

## Open in Android Studio

Open this directory as a Gradle project, allow Android Studio to install the configured Android SDK/Gradle dependencies, then run on an Android 8.0+ device. Grant SMS access through the in-app prompt to enable automatic detection.
