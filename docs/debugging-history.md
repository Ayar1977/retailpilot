# Debugging History

This document preserves important engineering issues encountered during
RetailPilot development without committing large raw terminal and crash logs.

## Room SaleLine Schema Mismatch

### Symptom
An earlier application build crashed during sale-line insertion with a SQLite
error indicating that a `barcode` column was missing.

### Cause
The persisted Room database schema and the evolving SaleLine entity were not
fully aligned during an earlier schema iteration.

### Resolution
The current model uses `barcodeSnapshot` for the sale-line barcode snapshot,
with the corresponding Room schema and migrations updated to match.

### Validation
The project contains dedicated Room migration tests covering multiple schema
versions and preservation of existing application data.

### Status
Resolved in the current source.

---

## Database Evolution

RetailPilot has evolved through multiple Room database schema versions rather
than resetting user data during development.

Migration tests are retained under:

`core/src/androidTest/java/com/salespro/data/MigrationTest.kt`

The repository also retains exported Room schemas under:

`core/schemas/com.salespro.data.AppDatabase/`

---

## Inventory and Sales Validation

The data layer contains tests covering important transactional behavior,
including:

- sale creation
- insufficient-stock handling
- stock allocation
- database migration
- legacy-data preservation
- inventory state transitions

Raw historical crash/build logs are intentionally kept outside the public
repository.
