// jnotes backend — tiny Express server that relays feedback and chat
// messages between the user app (jnotes) and the admin app (jnotes-admin).
//
// Storage: a single JSON file (data.json) co-located with the server.
//   {
//     "users":    { "<userId>": { "name": "Anon", "createdAt": 0, "lastSeen": 0 } },
//     "messages": { "<userId>": [ { "id": "...", "from": "user|admin", "text": "...", "ts": 0, "read": false } ] },
//     "feedback": [ { "id": "...", "userId": "...", "text": "...", "ts": 0 } ]
//   }
//
// Endpoints:
//   POST /api/register          { userId, name? }                       → { ok: true }
//   POST /api/feedback          { userId, name?, text }                 → { ok: true, id }
//   GET  /api/feedback                                                  → [ ... ]   (admin)
//   POST /api/chat/send         { userId, from, text }                  → { ok, message }
//   GET  /api/chat/messages?userId=...&since=ts                         → [ ... ]
//   POST /api/chat/read         { userId, who }                         → { ok }
//   GET  /api/chat/threads                                              → [ { userId, name, lastMessage, lastTs, unread } ]
//   GET  /api/healthz                                                    → "ok"

import express from "express";
import { promises as fs } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { randomUUID } from "node:crypto";

const __dirname = dirname(fileURLToPath(import.meta.url));
const DATA_FILE = join(__dirname, "data.json");
const PORT = Number(process.env.PORT || process.env.JNOTES_PORT || 8787);
const ADMIN_TOKEN = process.env.JNOTES_ADMIN_TOKEN || "change-me";

let db = { users: {}, messages: {}, feedback: [] };

async function load() {
  try {
    const raw = await fs.readFile(DATA_FILE, "utf8");
    db = JSON.parse(raw);
    db.users ??= {};
    db.messages ??= {};
    db.feedback ??= [];
  } catch {
    db = { users: {}, messages: {}, feedback: [] };
  }
}

let saveTimer = null;
function saveSoon() {
  if (saveTimer) return;
  saveTimer = setTimeout(async () => {
    saveTimer = null;
    try {
      await fs.writeFile(DATA_FILE, JSON.stringify(db, null, 2), "utf8");
    } catch (e) {
      console.error("save failed", e);
    }
  }, 200);
}

function ensureUser(userId, name) {
  if (!userId) return null;
  const u = db.users[userId] || { name: name || "Anon", createdAt: Date.now() };
  if (name && name.trim()) u.name = name.trim();
  u.lastSeen = Date.now();
  db.users[userId] = u;
  if (!db.messages[userId]) db.messages[userId] = [];
  saveSoon();
  return u;
}

function requireAdmin(req, res, next) {
  const token = req.header("X-Admin-Token");
  if (token !== ADMIN_TOKEN) {
    return res.status(401).json({ ok: false, error: "unauthorized" });
  }
  next();
}

const app = express();
app.use(express.json({ limit: "1mb" }));
app.use((req, _res, next) => {
  console.log(`${new Date().toISOString()} ${req.method} ${req.url}`);
  next();
});

app.get("/api/healthz", (_req, res) => res.type("text/plain").send("ok"));

// ── User registration ────────────────────────────────────────────────────
app.post("/api/register", (req, res) => {
  const { userId, name } = req.body || {};
  if (!userId) return res.status(400).json({ ok: false, error: "userId required" });
  ensureUser(userId, name);
  res.json({ ok: true });
});

// ── Feedback ─────────────────────────────────────────────────────────────
app.post("/api/feedback", (req, res) => {
  const { userId, name, text } = req.body || {};
  if (!userId || !text || !text.trim()) {
    return res.status(400).json({ ok: false, error: "userId and text required" });
  }
  ensureUser(userId, name);
  const item = { id: randomUUID(), userId, text: text.trim(), ts: Date.now() };
  db.feedback.unshift(item);
  saveSoon();
  res.json({ ok: true, id: item.id });
});

app.get("/api/feedback", requireAdmin, (_req, res) => {
  const out = db.feedback.map((f) => ({
    ...f,
    name: db.users[f.userId]?.name || "Anon",
  }));
  res.json(out);
});

// ── Chat ─────────────────────────────────────────────────────────────────
app.post("/api/chat/send", (req, res) => {
  const { userId, from, text, name } = req.body || {};
  if (!userId || !text || !text.trim() || !["user", "admin"].includes(from)) {
    return res.status(400).json({ ok: false, error: "userId, from, text required" });
  }
  // Admin-originated messages must include the admin token.
  if (from === "admin") {
    const token = req.header("X-Admin-Token");
    if (token !== ADMIN_TOKEN) return res.status(401).json({ ok: false });
  }
  ensureUser(userId, name);
  const msg = {
    id: randomUUID(),
    from,
    text: text.trim(),
    ts: Date.now(),
    read: false,
  };
  db.messages[userId].push(msg);
  saveSoon();
  res.json({ ok: true, message: msg });
});

app.get("/api/chat/messages", (req, res) => {
  const userId = String(req.query.userId || "");
  const since = Number(req.query.since || 0);
  if (!userId) return res.status(400).json({ ok: false, error: "userId required" });
  const list = db.messages[userId] || [];
  const filtered = since > 0 ? list.filter((m) => m.ts > since) : list;
  res.json(filtered);
});

app.post("/api/chat/read", (req, res) => {
  const { userId, who } = req.body || {};
  if (!userId || !["user", "admin"].includes(who)) {
    return res.status(400).json({ ok: false });
  }
  // who = "user" means the user app is acknowledging admin replies; mark all
  // admin messages as read. who = "admin" means the admin marks user messages.
  const target = who === "user" ? "admin" : "user";
  for (const m of db.messages[userId] || []) {
    if (m.from === target) m.read = true;
  }
  saveSoon();
  res.json({ ok: true });
});

app.get("/api/chat/threads", requireAdmin, (_req, res) => {
  const out = Object.entries(db.users).map(([uid, u]) => {
    const list = db.messages[uid] || [];
    const last = list[list.length - 1];
    const unread = list.filter((m) => m.from === "user" && !m.read).length;
    return {
      userId: uid,
      name: u.name || "Anon",
      lastMessage: last?.text || "",
      lastTs: last?.ts || u.lastSeen || u.createdAt,
      lastFrom: last?.from || null,
      unread,
    };
  });
  out.sort((a, b) => (b.lastTs || 0) - (a.lastTs || 0));
  res.json(out);
});

await load();
app.listen(PORT, () => {
  console.log(`jnotes-backend listening on :${PORT}`);
  console.log(`  data:        ${DATA_FILE}`);
  console.log(`  admin-token: ${ADMIN_TOKEN === "change-me" ? "(default! set JNOTES_ADMIN_TOKEN)" : "set"}`);
});
