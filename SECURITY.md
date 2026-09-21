# Security Policy

## Supported version

Only the latest GitHub Release receives security fixes.

## Reporting a vulnerability

Please use GitHub's private vulnerability reporting feature under **Security → Advisories → New draft security advisory**. Do not include API keys, signing keys, private user data, or exploitable details in a public issue.

Include the affected version, reproduction steps, expected impact, and any suggested mitigation. Maintainers should acknowledge a complete report within seven days.

## Secrets

Never commit `.env` files, provider API keys, Android signing keys, passwords, exported user backups, or production databases. Revoke and rotate any credential that is accidentally disclosed.
