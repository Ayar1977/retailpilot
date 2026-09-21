# Security Policy

## Sensitive files

Do not commit:

- Android keystores (`*.jks`, `*.keystore`)
- release-signing passwords or aliases
- `local.properties`
- `google-services.json`
- private API keys or service credentials

Release-signing values are expected to be supplied outside version control.

## Reporting a security issue

For a public portfolio repository, avoid posting credentials or exploit details in a public
issue. Contact the repository owner privately through an appropriate GitHub contact channel
when sensitive information is involved.

## Scope

RetailPilot is currently a portfolio project, not a hosted payment processor or managed
commerce service. Users evaluating it should review the code and deployment model for their
own security and compliance requirements.
