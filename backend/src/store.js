'use strict';
// Minimal JSON-file persistence. Fine for development and demos; use a real database
// (PostgreSQL, Firestore, etc.) in production.
const fs = require('fs');
const path = require('path');

class Store {
  constructor(file) {
    this.file = file;
    fs.mkdirSync(path.dirname(file), { recursive: true });
    this.data = { users: [], history: [] };
    if (fs.existsSync(file)) {
      try { this.data = JSON.parse(fs.readFileSync(file, 'utf8')); } catch (e) { console.warn('[store] Could not read db, starting empty:', e.message); }
    }
    this.data.users ||= [];
    this.data.history ||= [];
  }

  save() {
    const tmp = `${this.file}.tmp`;
    fs.writeFileSync(tmp, JSON.stringify(this.data, null, 2));
    fs.renameSync(tmp, this.file); // atomic replace
  }

  findUserByEmail(email) { return this.data.users.find((u) => u.email === email); }
  findUserById(id) { return this.data.users.find((u) => u.id === id); }

  createUser(user) { this.data.users.push(user); this.save(); return user; }

  updateUser(id, patch) {
    const user = this.findUserById(id);
    if (!user) return null;
    Object.assign(user, patch, { updatedAt: new Date().toISOString() });
    this.save();
    return user;
  }

  deleteUser(id) {
    this.data.users = this.data.users.filter((u) => u.id !== id);
    this.data.history = this.data.history.filter((h) => h.userId !== id);
    this.save();
  }

  addHistory(userId, entry) {
    const existing = this.data.history.find((h) => h.userId === userId && h.clientId === entry.clientId);
    if (existing) return existing; // idempotent: the app may retry offline uploads
    const row = { ...entry, userId };
    this.data.history.push(row);
    this.save();
    return row;
  }

  historyFor(userId) {
    return this.data.history
      .filter((h) => h.userId === userId)
      .sort((a, b) => b.completedAt.localeCompare(a.completedAt));
  }
}

module.exports = { Store };
