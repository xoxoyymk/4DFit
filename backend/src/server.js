'use strict';
const crypto = require('crypto');
const path = require('path');
const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { Store } = require('./store');
const { SIGNS, signFor, generateHoroscope } = require('./horoscope');

const PORT = Number(process.env.PORT || 8080);
const IS_PROD = process.env.NODE_ENV === 'production';
const JWT_SECRET = process.env.JWT_SECRET || (() => {
  if (IS_PROD) throw new Error('JWT_SECRET must be set in production');
  console.warn('[4dfit] JWT_SECRET not set: using a random development secret (tokens reset on restart).');
  return crypto.randomBytes(48).toString('hex');
})();
const TOKEN_TTL = process.env.TOKEN_TTL || '30d';

const seedDir = path.join(__dirname, '..', 'data', 'seed');
const workouts = require(path.join(seedDir, 'workouts.json'));
const nutrition = require(path.join(seedDir, 'nutrition_30_days.json'));
const pools = require(path.join(seedDir, 'horoscope_pools.json'));
const store = new Store(process.env.DB_FILE || path.join(__dirname, '..', 'data', 'db.json'));

const ENUMS = {
  gender: ['FEMALE', 'MALE', 'NON_BINARY', 'PREFER_NOT_TO_SAY'],
  activityLevel: ['SEDENTARY', 'LIGHT', 'MODERATE', 'ACTIVE', 'VERY_ACTIVE'],
  fitnessGoal: ['GENERAL_FITNESS', 'BUILD_STRENGTH', 'IMPROVE_ENDURANCE', 'FLEXIBILITY', 'STRESS_RELIEF', 'HEALTHY_HABITS'],
  category: ['FULL_BODY', 'UPPER_BODY', 'LOWER_BODY', 'CORE', 'MOBILITY', 'STRETCHING', 'CARDIO'],
};
const EMAIL_RE = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
const NAME_RE = /^[\p{L} .'-]{2,60}$/u;
const MIN_AGE = 13;

// ------------------------------------------------------------------ helpers
const fail = (res, status, message) => res.status(status).json({ message });

function ageOn(isoDob, now = new Date()) {
  const [y, m, d] = isoDob.split('-').map(Number);
  let age = now.getUTCFullYear() - y;
  if (now.getUTCMonth() + 1 < m || (now.getUTCMonth() + 1 === m && now.getUTCDate() < d)) age--;
  return age;
}

function validateProfile(b) {
  if (typeof b.fullName !== 'string' || !NAME_RE.test(b.fullName.trim())) return 'Enter a valid full name.';
  if (typeof b.dateOfBirth !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(b.dateOfBirth) || Number.isNaN(Date.parse(b.dateOfBirth))) return 'Enter a valid date of birth.';
  const age = ageOn(b.dateOfBirth);
  if (age < MIN_AGE) return `You need to be at least ${MIN_AGE} to use 4D FIT.`;
  if (age > 120) return 'Check your date of birth.';
  if (!ENUMS.gender.includes(b.gender)) return 'Choose a gender option.';
  if (typeof b.heightCm !== 'number' || b.heightCm < 100 || b.heightCm > 250) return 'Height must be between 100 and 250 cm.';
  if (!ENUMS.activityLevel.includes(b.activityLevel)) return 'Choose an activity level.';
  if (!ENUMS.fitnessGoal.includes(b.fitnessGoal)) return 'Choose a fitness goal.';
  if (typeof b.country !== 'string' || !b.country.trim() || b.country.length > 80) return 'Choose your country.';
  return null;
}

function publicUser(u) {
  const { passwordHash, ...rest } = u; // never return the hash
  return { ...rest, zodiacSign: signFor(u.dateOfBirth) };
}

function issueToken(user) {
  return jwt.sign({ sub: user.id }, JWT_SECRET, { expiresIn: TOKEN_TTL, algorithm: 'HS256' });
}

function requireAuth(req, res, next) {
  const match = (req.get('authorization') || '').match(/^Bearer (.+)$/);
  if (!match) return fail(res, 401, 'Sign in to continue.');
  try {
    const payload = jwt.verify(match[1], JWT_SECRET, { algorithms: ['HS256'] });
    const user = store.findUserById(payload.sub);
    if (!user) return fail(res, 401, 'Your session has expired. Sign in again.');
    req.user = user;
    return next();
  } catch {
    return fail(res, 401, 'Your session has expired. Sign in again.');
  }
}

function rateLimit({ windowMs, max }) {
  const hits = new Map();
  return (req, res, next) => {
    const now = Date.now();
    const key = req.ip;
    const entry = hits.get(key) || { count: 0, reset: now + windowMs };
    if (now > entry.reset) { entry.count = 0; entry.reset = now + windowMs; }
    entry.count += 1;
    hits.set(key, entry);
    if (entry.count > max) return fail(res, 429, 'Too many attempts. Wait a few minutes and try again.');
    return next();
  };
}
const authLimiter = rateLimit({ windowMs: 15 * 60 * 1000, max: 20 });
// Compared against when an email isn't registered, so timing doesn't reveal which emails exist.
const DUMMY_HASH = bcrypt.hashSync(crypto.randomBytes(16).toString('hex'), 12);

// ------------------------------------------------------------------ app
const app = express();
app.disable('x-powered-by');
app.set('trust proxy', 1);
app.use(express.json({ limit: '50kb' }));
app.use((req, res, next) => {
  res.set({ 'X-Content-Type-Options': 'nosniff', 'X-Frame-Options': 'DENY', 'Referrer-Policy': 'no-referrer', 'Cache-Control': 'no-store' });
  next();
});

const v1 = express.Router();

v1.post('/register', authLimiter, async (req, res) => {
  const b = req.body || {};
  const email = typeof b.email === 'string' ? b.email.trim().toLowerCase() : '';
  if (!EMAIL_RE.test(email) || email.length > 254) return fail(res, 400, 'Enter a valid email address.');
  if (typeof b.password !== 'string' || b.password.length < 8 || b.password.length > 128 || !/[A-Za-z]/.test(b.password) || !/\d/.test(b.password)) {
    return fail(res, 400, 'Use at least 8 characters with a letter and a number.');
  }
  const problem = validateProfile(b);
  if (problem) return fail(res, 400, problem);
  if (store.findUserByEmail(email)) return fail(res, 409, 'An account with this email already exists. Sign in instead.');
  const now = new Date().toISOString();
  const user = store.createUser({
    id: `usr_${crypto.randomUUID()}`,
    email,
    passwordHash: await bcrypt.hash(b.password, 12),
    fullName: b.fullName.trim(),
    dateOfBirth: b.dateOfBirth,
    gender: b.gender,
    heightCm: b.heightCm,
    activityLevel: b.activityLevel,
    fitnessGoal: b.fitnessGoal,
    country: b.country.trim(),
    createdAt: now,
    updatedAt: now,
  });
  return res.status(201).json({ token: issueToken(user), user: publicUser(user) });
});

v1.post('/login', authLimiter, async (req, res) => {
  const b = req.body || {};
  const email = typeof b.email === 'string' ? b.email.trim().toLowerCase() : '';
  const user = store.findUserByEmail(email);
  const hash = user ? user.passwordHash : DUMMY_HASH;
  const ok = await bcrypt.compare(typeof b.password === 'string' ? b.password : '', hash);
  if (!user || !ok) return fail(res, 401, 'Incorrect email or password.');
  return res.json({ token: issueToken(user), user: publicUser(user) });
});

v1.get('/profile', requireAuth, (req, res) => res.json(publicUser(req.user)));

v1.put('/profile', requireAuth, (req, res) => {
  const b = req.body || {};
  const problem = validateProfile(b);
  if (problem) return fail(res, 400, problem);
  const updated = store.updateUser(req.user.id, {
    fullName: b.fullName.trim(),
    dateOfBirth: b.dateOfBirth,
    gender: b.gender,
    heightCm: b.heightCm,
    activityLevel: b.activityLevel,
    fitnessGoal: b.fitnessGoal,
    country: b.country.trim(),
  });
  return res.json(publicUser(updated));
});

v1.delete('/account', requireAuth, (req, res) => {
  store.deleteUser(req.user.id);
  return res.json({ message: 'Account deleted' });
});

v1.get('/workouts', (req, res) => res.json(workouts));

v1.get('/workouts/:id', (req, res) => {
  const program = workouts.programs.find((p) => p.id === req.params.id);
  return program ? res.json(program) : fail(res, 404, 'Workout not found.');
});

v1.get('/nutrition/30-days', (req, res) => res.json(nutrition));

v1.get('/horoscope/:zodiac', (req, res) => {
  const sign = String(req.params.zodiac || '').toUpperCase();
  if (!SIGNS[sign]) return fail(res, 404, 'Unknown zodiac sign.');
  const date = /^\d{4}-\d{2}-\d{2}$/.test(String(req.query.date || '')) ? req.query.date : new Date().toISOString().slice(0, 10);
  return res.json(generateHoroscope(sign, date, pools));
});

v1.post('/workout/history', requireAuth, (req, res) => {
  const b = req.body || {};
  if (typeof b.clientId !== 'string' || b.clientId.length > 64) return fail(res, 400, 'Invalid workout id.');
  if (typeof b.completedAt !== 'string' || Number.isNaN(Date.parse(b.completedAt))) return fail(res, 400, 'Invalid completion time.');
  if (!Number.isInteger(b.durationSeconds) || b.durationSeconds < 0 || b.durationSeconds > 6 * 3600) return fail(res, 400, 'Invalid duration.');
  const row = store.addHistory(req.user.id, {
    id: `wh_${crypto.randomUUID()}`,
    clientId: b.clientId,
    workoutId: String(b.workoutId || '').slice(0, 80),
    title: String(b.title || 'Workout').slice(0, 80),
    completedAt: new Date(b.completedAt).toISOString(),
    durationSeconds: b.durationSeconds,
    exercisesCompleted: Number.isInteger(b.exercisesCompleted) ? b.exercisesCompleted : 0,
    categories: Array.isArray(b.categories) ? b.categories.filter((c) => ENUMS.category.includes(c)) : [],
    completedFully: b.completedFully !== false,
  });
  const { userId, ...out } = row;
  return res.status(201).json(out);
});

v1.get('/progress', requireAuth, (req, res) => {
  const history = store.historyFor(req.user.id).map(({ userId, ...h }) => h);
  return res.json({
    totalWorkouts: history.length,
    totalActiveSeconds: history.reduce((s, h) => s + h.durationSeconds, 0),
    history,
  });
});

app.get('/health', (req, res) => res.json({ status: 'ok' }));
app.use('/v1', v1);
app.use((req, res) => fail(res, 404, 'Not found.'));
// eslint-disable-next-line no-unused-vars
app.use((err, req, res, next) => {
  if (err.type === 'entity.parse.failed') return fail(res, 400, 'Malformed JSON.');
  console.error(err);
  return fail(res, 500, 'Something went wrong on our side.');
});

if (require.main === module) {
  app.listen(PORT, () => console.log(`[4dfit] API listening on http://localhost:${PORT}/v1`));
}

module.exports = app;
