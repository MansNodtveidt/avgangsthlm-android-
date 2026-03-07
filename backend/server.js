require('dotenv').config();
const express = require('express');
const NodeCache = require('node-cache');
const axios = require('axios');
const https = require('https');
const rateLimit = require('express-rate-limit');

const app = express();
const PORT = process.env.PORT || 3000;
const API_KEY = process.env.RESROBOT_API_KEY;
const RESROBOT_BASE = 'https://api.resrobot.se/v2.1';

const departuresCache = new NodeCache({ stdTTL: 30 });
const stopsCache = new NodeCache({ stdTTL: 3600 });

// ── Rate limiters ─────────────────────────────────────────────────────────────
const generalLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 60,
  message: { error: 'För många förfrågningar, försök igen om en minut.' },
  standardHeaders: true,
  legacyHeaders: false
});

const searchLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 20,
  message: { error: 'För många sökningar, vänta lite.' },
  standardHeaders: true,
  legacyHeaders: false
});

app.use(generalLimiter);
app.use('/stops', searchLimiter);

// ── Daily request counter ─────────────────────────────────────────────────────
let dailyRequestCount = 0;
let dailyResetTime = Date.now() + 24 * 60 * 60 * 1000;

app.use((req, res, next) => {
  if (Date.now() > dailyResetTime) {
    dailyRequestCount = 0;
    dailyResetTime = Date.now() + 24 * 60 * 60 * 1000;
  }
  dailyRequestCount++;
  if (dailyRequestCount % 100 === 0) {
    console.log(`Daily API calls so far: ${dailyRequestCount}`);
  }
  next();
});

// ── Health check ─────────────────────────────────────────────────────────────
app.get('/health', (req, res) => {
  res.json({ status: 'ok', uptime: process.uptime(), dailyRequests: dailyRequestCount });
});

// ── GET /departures?siteId=STOP_ID&maxJourneys=4 ─────────────────────────────
app.get('/departures', async (req, res) => {
  const { siteId, maxJourneys = 4 } = req.query;
  if (!siteId) return res.status(400).json({ error: 'siteId is required' });

  const cacheKey = `departures_${siteId}`;
  const cached = departuresCache.get(cacheKey);
  if (cached) return res.json(cached);

  try {
    const { data } = await axios.get(`${RESROBOT_BASE}/departureBoard`, {
      params: { id: siteId, format: 'json', accessId: API_KEY, maxJourneys }
    });
    departuresCache.set(cacheKey, data);
    res.json(data);
  } catch (err) {
    const status = err.response?.status ?? 502;
    res.status(status).json({ error: 'ResRobot error', details: err.message });
  }
});

// ── GET /stops?q=SEARCH_QUERY ─────────────────────────────────────────────────
app.get('/stops', async (req, res) => {
  const query = req.query.q;
  console.log('=== /stops called ===');
  console.log('query param:', query);
  console.log('API key exists:', !!process.env.RESROBOT_API_KEY);
  console.log('API key first 8 chars:', process.env.RESROBOT_API_KEY?.substring(0, 8));

  if (!query || query.length < 2) {
    console.log('Query too short, returning 400');
    return res.status(400).json({ error: 'Query too short' });
  }

  const cacheKey = `stops_${query.toLowerCase()}`;
  const cached = stopsCache.get(cacheKey);
  if (cached) {
    console.log('Cache hit for:', cacheKey);
    return res.json(cached);
  }

  const url = `https://api.resrobot.se/v2.1/location.name?input=${encodeURIComponent(query)}&format=json&accessId=${process.env.RESROBOT_API_KEY}&maxNo=10`;
  console.log('Calling URL:', url);

  try {
    const response = await axios.get(url);
    console.log('ResRobot status:', response.status);
    console.log('ResRobot data:', JSON.stringify(response.data).substring(0, 200));
    stopsCache.set(cacheKey, response.data);
    res.json(response.data);
  } catch (err) {
    console.log('ERROR status:', err.response?.status);
    console.log('ERROR data:', JSON.stringify(err.response?.data));
    console.log('ERROR message:', err.message);
    res.status(err.response?.status || 500)
       .json({ error: err.message, details: err.response?.data });
  }
});

app.listen(PORT, () => {
  console.log(`AvgångSthlm backend running on port ${PORT}`);
});

// ── Keep-alive ping (prevents Render free tier from spinning down) ────────────
const keepAlive = () => {
  const url = process.env.RENDER_EXTERNAL_URL ||
              'https://avgangsthim-backend.onrender.com';
  https.get(`${url}/health`, (res) => {
    console.log(`Keep-alive ping: ${res.statusCode}`);
  }).on('error', (err) => {
    console.log(`Keep-alive failed: ${err.message}`);
  });
};

setInterval(keepAlive, 10 * 60 * 1000);
