# RetailPilot Project Record

RetailPilot is an offline-first Android POS and inventory-management application built with Kotlin, Jetpack Compose, Room, and Hilt.

## Engineering Scope

- modular Android architecture
- sales and inventory workflows
- stock-batch tracking
- purchasing and supplier management
- barcode/OCR workflows
- Room database migrations
- WorkManager reminders
- automated tests and Android lint

## Validation

The public source was validated with:

- clean debug build
- debug unit tests
- Android lint

All three completed successfully before publication.

## Security

Release-signing credentials and local Android configuration are excluded from Git. Release signing uses environment variables.

## Engineering History

Resolved debugging work is summarized in `docs/debugging-history.md` rather than committing large raw build and crash logs.

## Status

Stable public portfolio release. Future changes should be meaningful, validated, and documented.
