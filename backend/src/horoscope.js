'use strict';
// Deterministic, light-hearted daily readings built from curated pools (data/seed/horoscope_pools.json).
// Same sign + date => same reading for everyone. No medical, financial or other high-stakes predictions.

const SIGNS = {
  ARIES: 'Fire', TAURUS: 'Earth', GEMINI: 'Air', CANCER: 'Water', LEO: 'Fire', VIRGO: 'Earth',
  LIBRA: 'Air', SCORPIO: 'Water', SAGITTARIUS: 'Fire', CAPRICORN: 'Earth', AQUARIUS: 'Air', PISCES: 'Water',
};

const DISCLAIMER = 'Horoscope content is for entertainment/general-interest purposes only.';

function signFor(isoDate) {
  const [, m, d] = isoDate.split('-').map(Number);
  const md = m * 100 + d;
  if (md >= 1222 || md <= 119) return 'CAPRICORN';
  if (md <= 218) return 'AQUARIUS';
  if (md <= 320) return 'PISCES';
  if (md <= 419) return 'ARIES';
  if (md <= 520) return 'TAURUS';
  if (md <= 620) return 'GEMINI';
  if (md <= 722) return 'CANCER';
  if (md <= 822) return 'LEO';
  if (md <= 922) return 'VIRGO';
  if (md <= 1022) return 'LIBRA';
  if (md <= 1121) return 'SCORPIO';
  return 'SAGITTARIUS';
}

function fnv1a(str) {
  let h = 0x811c9dc5;
  for (let i = 0; i < str.length; i++) { h ^= str.charCodeAt(i); h = Math.imul(h, 0x01000193); }
  return h >>> 0;
}

function mulberry32(seed) {
  return function next() {
    seed |= 0; seed = (seed + 0x6D2B79F5) | 0;
    let t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

function generateHoroscope(sign, isoDate, pools) {
  const rng = mulberry32(fnv1a(`${sign}|${isoDate}`));
  const pick = (list, fallback) => (list && list.length ? list[Math.floor(rng() * list.length)] : fallback);
  const opener = pick((pools.elementOpeners || {})[SIGNS[sign]], 'Today is a good day to look after yourself.');
  const color = pick(pools.colors, { name: 'Electric blue', hex: '#3D7BFF' });
  return {
    sign,
    date: isoDate,
    general: `${opener} ${pick(pools.general, 'Small, steady steps feel rewarding today.')}`,
    motivation: pick(pools.motivation, 'Show up for five minutes.'),
    wellness: pick(pools.wellness, 'Keep water nearby.'),
    luckyColor: color.name,
    luckyColorHex: color.hex,
    luckyNumber: 1 + Math.floor(rng() * 99),
    disclaimer: DISCLAIMER,
  };
}

module.exports = { SIGNS, signFor, generateHoroscope, DISCLAIMER };
