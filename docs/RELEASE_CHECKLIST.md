# RetailPilot v1.0.0 release checklist

## Before committing

- [ ] Review Apache-2.0 license choice.
- [ ] Confirm `settings.gradle.kts` uses `rootProject.name = "RetailPilot"`.
- [ ] Confirm `app/build.gradle.kts` uses `versionCode = 1`.
- [ ] Confirm `app/build.gradle.kts` uses `versionName = "1.0.0"`.
- [ ] Confirm only intended productization files are modified.

## Local release gate

Run from the repository root on Windows:

```powershell
.\gradlew.bat clean assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
```

Then verify:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Do not commit the APK; CI/release automation publishes build artifacts.

## GitHub gate

1. Push the productization commit.
2. Wait for `Android CI` to complete successfully.
3. Download the `retailpilot-debug-apk` workflow artifact and test-install it on an Android
   emulator/device if available.
4. Only after CI is green, create GitHub Release:
   - tag: `v1.0.0`
   - target: verified `main`
   - title: `RetailPilot v1.0.0`
5. The `Publish demo APK` workflow should:
   - rebuild;
   - rerun unit tests and lint;
   - verify APK versionName == `1.0.0`;
   - attach `RetailPilot-v1.0.0-demo.apk`;
   - attach `RetailPilot-v1.0.0-demo.apk.sha256`.

## Release notes

Suggested:

RetailPilot v1.0.0 is the first versioned portfolio release of the offline-first Android POS and
inventory application.

Highlights:
- Offline-first sales and inventory workflows
- Product, supplier, purchase, payment, and stock-batch tracking
- Barcode-assisted workflows and ML Kit OCR
- Sales analytics and reports
- Room persistence with versioned migrations
- WorkManager low-stock reminders
- Adaptive Jetpack Compose UI
- CI-gated build, tests, and Android lint
- Downloadable demo APK with SHA-256 checksum

The attached APK is debug-signed for portfolio/demo evaluation and is not a Play Store production
artifact.
