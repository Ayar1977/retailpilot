# RetailPilot

RetailPilot is an offline-first Android point-of-sale and inventory management application for small retail businesses.

It combines sales processing, inventory tracking, purchasing, stock-batch management, analytics, barcode/OCR-assisted workflows, reminders, and local data persistence in a modular Kotlin application.

## Features

- Product catalogue and inventory management
- Cart-based sales and checkout
- Purchase and supplier workflows
- Stock batches and inventory movements
- Insufficient-stock validation
- Payment tracking
- Sales and business analytics
- Low-stock reminders with WorkManager
- Barcode-assisted product workflows
- ML Kit text recognition
- PDF invoice generation and receipt sharing
- Business profile management
- Adaptive layouts for different screen sizes
- Offline-first Room database
- Versioned database migrations
- Unit and instrumentation tests

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Room
- Hilt
- WorkManager
- ML Kit
- Gradle Kotlin DSL
- AndroidX
- Coroutines / Flow

## Architecture

```text
RetailPilot
├── app
│   ├── application entry point
│   ├── navigation
│   ├── ViewModels
│   └── background workers
├── core
│   ├── Room database
│   ├── entities
│   ├── DAOs
│   ├── repository layer
│   ├── dependency injection
│   └── shared utilities
├── feature:store
│   ├── sales
│   ├── inventory
│   ├── purchases
│   ├── checkout
│   └── adaptive retail UI
└── feature:reports
    └── analytics and reporting
```

## Data Model

RetailPilot models real retail operations using entities such as `Product`, `Sale`, `SaleLine`, `Purchase`, `PurchaseLine`, `Payment`, `Supplier`, `StockBatch`, `StockMovement`, `StockReminder`, and `ProductPriceHistory`.

Monetary values are stored using integer minor units instead of floating-point values for transactional consistency.

## Database Evolution

RetailPilot uses versioned Room schemas and explicit migration logic to preserve application data as the database evolves.

Exported schemas are retained under:

```text
core/schemas/com.salespro.data.AppDatabase/
```

Migration tests are available under:

```text
core/src/androidTest/java/com/salespro/data/
```

Historical debugging decisions are documented in:

```text
docs/debugging-history.md
```

## Validation

The current source has been validated successfully with:

```powershell
.\gradlew.bat clean assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
```

All three complete successfully on the current development setup.

## Screenshot

![RetailPilot](Screenshot_20260801_132822.png)

## Build Locally

Clone the repository and install the Android SDK.

Create a local `local.properties` file:

```properties
sdk.dir=C\:\\path\\to\\Android\\Sdk
```

Then run:

```powershell
.\gradlew.bat assembleDebug
```

## Release Signing

Release credentials are intentionally not stored in the repository.

The release build reads these environment variables:

```text
RELEASE_KEYSTORE_PATH
RELEASE_STORE_PASSWORD
RELEASE_KEY_ALIAS
RELEASE_KEY_PASSWORD
```

Keystores and `local.properties` must remain local and must never be committed.

## Development Notes

RetailPilot went through multiple database and inventory-model iterations during development. Important resolved issues and migration decisions are documented rather than retaining large raw crash/build logs in the public repository.

See `docs/debugging-history.md`.

## Status

Portfolio release. Debug build, unit tests, and Android lint currently complete successfully.
