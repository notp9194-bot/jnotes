# jnotes Admin

Companion Android app for the **jnotes** notes app. It connects to the
`jnotes-backend` relay server and lets the admin:

- See a live list of conversations from users
- Reply to any user (two-way chat with read receipts)
- Read all submitted feedback in one place

## Setup

1. Run the **jnotes-backend** Node.js server (see `jnotes-backend/README.md`).
   Set an `ADMIN_TOKEN=<your-secret>` environment variable.
2. Install this app on your admin device.
3. Open **Settings** (gear icon) and enter:
   - **Server URL** — e.g. `http://192.168.0.10:8787`
   - **Admin token** — the same value as `ADMIN_TOKEN` on the server
4. Hit **Save & test** — you should see "Connected ✓".

The user app must be configured with the **same Server URL** in its
Settings → "Feedback & chat server" section.

## Build

```bash
./gradlew :app:assembleDebug
```

The APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.
