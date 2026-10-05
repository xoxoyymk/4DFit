'use strict';
// End-to-end smoke test: node src/smoke-test.js (uses a temporary database file).
const os = require('os');
const path = require('path');
process.env.DB_FILE = path.join(os.tmpdir(), `fourdfit-smoke-${Date.now()}.json`);
const app = require('./server');

(async () => {
  const server = app.listen(0);
  const base = `http://127.0.0.1:${server.address().port}/v1`;
  const call = async (method, url, body, token) => {
    const res = await fetch(base + url, {
      method,
      headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      body: body ? JSON.stringify(body) : undefined,
    });
    return { status: res.status, body: await res.json() };
  };
  const check = (cond, label) => { if (!cond) { console.error('FAIL', label); process.exitCode = 1; } else console.log('ok  ', label); };

  const profile = { fullName: 'Asha Verma', dateOfBirth: '1996-08-10', gender: 'FEMALE', heightCm: 165, activityLevel: 'MODERATE', fitnessGoal: 'GENERAL_FITNESS', country: 'India' };
  let r = await call('POST', '/register', { ...profile, email: 'asha@example.com', password: 'weak' });
  check(r.status === 400, 'weak password rejected');
  r = await call('POST', '/register', { ...profile, email: 'asha@example.com', password: 'strongPass1' });
  check(r.status === 201 && r.body.token && r.body.user.zodiacSign === 'LEO' && !r.body.user.passwordHash, 'register returns token, Leo, no hash');
  r = await call('POST', '/register', { ...profile, email: 'asha@example.com', password: 'strongPass1' });
  check(r.status === 409, 'duplicate email rejected');
  r = await call('POST', '/login', { email: 'asha@example.com', password: 'wrongPass1' });
  check(r.status === 401, 'wrong password rejected');
  r = await call('POST', '/login', { email: 'ASHA@example.com', password: 'strongPass1' });
  check(r.status === 200 && r.body.token, 'login works (case-insensitive email)');
  const token = r.body.token;
  r = await call('GET', '/profile');
  check(r.status === 401, 'profile requires auth');
  r = await call('PUT', '/profile', { ...profile, heightCm: 166 }, token);
  check(r.status === 200 && r.body.heightCm === 166, 'profile update');
  r = await call('GET', '/workouts');
  check(r.status === 200 && r.body.exercises.length >= 25, 'workout catalog');
  r = await call('GET', '/workouts/p_core_matrix');
  check(r.status === 200 && r.body.title === 'Core Matrix', 'single workout');
  r = await call('GET', '/nutrition/30-days');
  check(r.status === 200 && r.body.days.length === 30, '30-day nutrition plan');
  const h1 = await call('GET', '/horoscope/leo?date=2026-10-04');
  const h2 = await call('GET', '/horoscope/LEO?date=2026-10-04');
  check(h1.status === 200 && h1.body.general === h2.body.general && h1.body.disclaimer, 'horoscope deterministic with disclaimer');
  const entry = { clientId: 'c-1', workoutId: 'p_core_matrix', title: 'Core Matrix', completedAt: new Date().toISOString(), durationSeconds: 900, exercisesCompleted: 6, categories: ['CORE'], completedFully: true };
  r = await call('POST', '/workout/history', entry, token);
  check(r.status === 201 && r.body.id, 'history saved');
  await call('POST', '/workout/history', entry, token);
  r = await call('GET', '/progress', null, token);
  check(r.status === 200 && r.body.totalWorkouts === 1 && r.body.totalActiveSeconds === 900, 'progress deduplicates retries');
  r = await call('DELETE', '/account', null, token);
  check(r.status === 200, 'account deleted');
  r = await call('GET', '/profile', null, token);
  check(r.status === 401, 'token invalid after deletion');
  server.close();
})();
