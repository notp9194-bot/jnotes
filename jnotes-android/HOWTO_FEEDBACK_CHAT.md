# Feedback & Chat — quick start

The user app now includes a feedback form and a two-way chat with the
admin. They both use a small Node.js relay server (**jnotes-backend**)
plus the companion **jnotes-admin** Android app.

## 1. Run the relay server

```bash
cd jnotes-backend
npm install
ADMIN_TOKEN=mychangethis PORT=8787 npm start
```

The server listens on `http://<your-LAN-ip>:8787`. Note the token —
you'll need it on the admin device.

## 2. Connect the user app

1. Open the user app → **Settings → "Feedback & chat server"**.
2. Set **Server URL** to e.g. `http://192.168.0.10:8787`.
3. Optionally set a **Display name** (else messages show as "Anon").
4. Save.

Now go to **About → Send Feedback** or **About → Message Admin**.

## 3. Connect the admin app

1. Build & install **jnotes-admin** on your admin device.
2. Open it → **Settings (gear)**.
3. Set the **same Server URL** and the **Admin token** from step 1.
4. Save & test → "Connected ✓".

You'll see incoming chats and feedback live in the admin inbox.
