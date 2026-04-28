# jnotes backend

A tiny Node.js Express server that relays **feedback** and **chat messages**
between the **jnotes** user app and the **jnotes-admin** Android app.

## Run

```bash
cd jnotes-backend
npm install
JNOTES_ADMIN_TOKEN="pick-a-strong-secret" PORT=8787 npm start
```

The server stores everything in a single `data.json` file beside `server.mjs`.

## Configure the apps

Open **Settings → Server** in either app and set:

* **Server URL** — for example `http://192.168.1.10:8787`
  (use your machine's LAN IP if testing on a phone, or your hosted URL).
* **Admin token** — only the **admin app** asks for this. It must match
  the `JNOTES_ADMIN_TOKEN` you exported above.

## Endpoints

| Method | Path                       | Who   | Purpose                            |
| -----: | -------------------------- | :---- | ---------------------------------- |
|   GET  | `/api/healthz`             | both  | quick "is it up?" check            |
|  POST  | `/api/register`            | user  | publish display-name once          |
|  POST  | `/api/feedback`            | user  | one-off feedback note              |
|   GET  | `/api/feedback`            | admin | list feedback                      |
|  POST  | `/api/chat/send`           | both  | send chat message (admin must auth)|
|   GET  | `/api/chat/messages`       | both  | poll messages for a userId         |
|  POST  | `/api/chat/read`           | both  | mark messages read                 |
|   GET  | `/api/chat/threads`        | admin | list user threads + unread counts  |

Admin endpoints require the header `X-Admin-Token: <your-token>`.
