# Security Policy

AirBeats takes the security and privacy of our users and their data seriously. This document outlines our security policies, supported versions, and how to responsibly report security vulnerabilities.

---

## Supported Versions

Security fixes are actively applied to the following releases:

| Version | Supported          | Security Updates Coverage |
| :------ | :----------------- | :------------------------ |
| 5.9.x   | :white_check_mark: | Full Active Support       |
| 5.8.x   | :white_check_mark: | Critical Security Fixes   |
| 5.0.x   | :x:                | End of Life               |
| < 5.0   | :x:                | End of Life               |

We strongly encourage all users to always update to the latest available release of AirBeats to ensure you have the latest security patches, bug fixes, and feature improvements.

---

## Reporting a Vulnerability

If you discover a potential security vulnerability in AirBeats, please report it to us responsibly before disclosing it publicly.

### How to Report

- **GitHub Private Vulnerability Reporting**: Submit a confidential advisory directly via the **Security** tab on the [AirBeats GitHub Repository](https://github.com/d0x-dev/AirBeats/security/advisories/new).
- **Email**: Contact the lead maintainer directly via GitHub or issue reports for non-critical security queries.

### What to Include in Your Report

To help us investigate and resolve the issue quickly, please include:
1. **Description**: A clear summary of the vulnerability and its potential impact.
2. **Steps to Reproduce**: Proof-of-concept (PoC) code, step-by-step instructions, or request/response details.
3. **Affected Versions**: AirBeats version and Android OS build tested.
4. **Mitigation Ideas**: Any proposed patches or workarounds (if available).

### Response SLA & Process

- **Acknowledgment**: We aim to acknowledge receipt of security reports within **24–48 hours**.
- **Assessment**: We will evaluate the report and confirm the vulnerability severity within **3–5 business days**.
- **Fix & Disclosure**: Confirmed security vulnerabilities will be patched promptly in an upcoming patch release (or immediate hotfix for high-severity issues). Coordinated public disclosure will take place after a patch is released.

---

## Privacy & Data Handling

AirBeats operates with a **privacy-first** design principle:
- **Local Storage First**: Your local music library, play counts, and playlists are stored locally on your device in an encrypted Room SQLite database.
- **Authentication Credentials**: OAuth tokens and API secrets (e.g., Discord Rich Presence tokens, Cloud Sync backup credentials) are stored securely in Android's `EncryptedSharedPreferences` / `DataStore`.
- **Network Requests**: All network traffic to music APIs (YouTube InnerTube, Spotify metadata, Lrclib lyrics, etc.) uses secure HTTPS/TLS 1.3 endpoints.

---

## Security Best Practices for Users

1. **Download Official Releases Only**: Always install AirBeats from official sources (GitHub Releases or verified app stores).
2. **Keep Your App Updated**: Enable automatic app updates or check for update notifications within AirBeats settings.
3. **Backup File Safety**: Keep your exported cloud backup `.zip` files safe and do not share backup tokens with untrusted parties.

---

## Acknowledgments

We appreciate the security research community for helping us keep AirBeats safe and secure. Security researchers who submit valid, responsibly disclosed vulnerabilities will be credited in our Release Notes (unless they prefer to remain anonymous).
