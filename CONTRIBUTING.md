# Contributing to RetailPilot

Thanks for considering a contribution.

## Development gate

Before opening a pull request, run:

```powershell
.\gradlew.bat clean assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
```

Keep pull requests focused. Avoid unrelated formatting churn.

## Database changes

Room schema and migration changes must preserve upgrade behavior. When changing database
structure, update/export schemas as required and add or update migration tests.

## Secrets

Never commit keystores, signing credentials, `local.properties`, Google service credentials,
or other private configuration.

## Release-impacting changes

Changes to `versionCode`, `versionName`, release signing, Room migrations, package identity,
or minimum/target SDK should be called out explicitly in the pull request.
