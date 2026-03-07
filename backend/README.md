# AvgångSthlm Backend

A lightweight caching proxy that sits between the Android app and the ResRobot API.
Caches departure responses for 30 seconds and stop-search responses for 1 hour,
reducing API quota usage and improving widget update reliability.

## Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/departures?siteId=ID&maxJourneys=4` | Next departures for a stop (cached 30 s) |
| GET | `/stops?q=QUERY` | Stop name autocomplete (cached 1 h) |
| GET | `/health` | Health check – returns `{ status: "ok", uptime: N }` |

## Local development

```bash
cd backend
npm install
cp .env.example .env   # then fill in your key
npm start
```

The server starts on `http://localhost:3000`.

When testing with an Android emulator, use `http://10.0.2.2:3000` as the backend URL
in the app's `local.properties`.

## Deploy to Railway.app

1. Push the project to GitHub
2. Go to [railway.app](https://railway.app) → **New Project** → **Deploy from GitHub repo**
3. Select the repository; set the **Root Directory** to `backend`
4. Add environment variable: `RESROBOT_API_KEY = <your key>`
5. Railway auto-detects Node.js and runs `npm start` — deploy is live in ~30 seconds
6. Copy the generated URL (e.g. `https://avgangsthim-backend-production.up.railway.app`)
7. Set `BACKEND_URL` in the app's `local.properties` to that URL

## Deploy to Render.com

1. Push the project to GitHub
2. Go to [render.com](https://render.com) → **New** → **Web Service**
3. Connect the repository; set **Root Directory** to `backend`
4. Build command: `npm install`  |  Start command: `npm start`
5. Add environment variable: `RESROBOT_API_KEY = <your key>`
6. Click **Create Web Service** — Render assigns a URL like `https://avgangsthim-backend.onrender.com`
7. Set `BACKEND_URL` in the app's `local.properties` to that URL

> **Note:** Render's free tier spins down after inactivity. The `/health` endpoint
> can be used with an uptime monitor (e.g. UptimeRobot) to keep the service warm.
