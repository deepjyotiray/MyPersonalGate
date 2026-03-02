const Database = require('better-sqlite3');
const path = require('path');

const db = new Database(path.join(__dirname, 'ananta.db'));

db.exec(`
  CREATE TABLE IF NOT EXISTS entries (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type TEXT NOT NULL CHECK(type IN ('visitor','delivery','vehicle')),
    name TEXT NOT NULL,
    mobile_number TEXT,
    building TEXT,
    flat_number TEXT NOT NULL,
    host_name TEXT,
    vehicle_number TEXT,
    has_sticker INTEGER DEFAULT 0,
    purpose TEXT,
    id_proof TEXT,
    photo_url TEXT,
    id_photo_url TEXT,
    entry_time TEXT DEFAULT (datetime('now','localtime')),
    exit_time TEXT,
    is_active INTEGER DEFAULT 1
  );

  CREATE TABLE IF NOT EXISTS vehicles (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    vehicle_number TEXT NOT NULL UNIQUE,
    owner_name TEXT NOT NULL,
    mobile_number TEXT NOT NULL,
    building TEXT NOT NULL,
    flat_number TEXT NOT NULL,
    vehicle_type TEXT NOT NULL,
    tag_number TEXT,
    society_sticker_number TEXT,
    is_tenant INTEGER DEFAULT 0,
    rc_book_url TEXT,
    rent_agreement_url TEXT,
    vehicle_photo_url TEXT,
    is_active INTEGER DEFAULT 1,
    created_at TEXT DEFAULT (datetime('now','localtime'))
  );
`);

// Migrate users table to support superadmin role if needed
const cols = db.prepare("PRAGMA table_info(users)").all();
if (!cols.length) {
  db.exec(`CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    pin TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'guard',
    name TEXT NOT NULL,
    is_active INTEGER DEFAULT 1,
    created_at TEXT DEFAULT (datetime('now','localtime'))
  )`);
} else {
  // Check if superadmin role is blocked by CHECK constraint
  const tableInfo = db.prepare("SELECT sql FROM sqlite_master WHERE type='table' AND name='users'").get();
  if (tableInfo && tableInfo.sql && tableInfo.sql.includes("CHECK")) {
    db.exec(`ALTER TABLE users RENAME TO users_old`);
    db.exec(`CREATE TABLE users (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      username TEXT NOT NULL UNIQUE,
      pin TEXT NOT NULL,
      role TEXT NOT NULL DEFAULT 'guard',
      name TEXT NOT NULL,
      is_active INTEGER DEFAULT 1,
      created_at TEXT DEFAULT (datetime('now','localtime'))
    )`);
    db.exec(`INSERT INTO users SELECT * FROM users_old`);
    db.exec(`DROP TABLE users_old`);
  }
}

// Seed superadmin if not exists
const existing = db.prepare("SELECT id FROM users WHERE username = 'superadmin'").get();
if (!existing) {
  db.prepare("INSERT INTO users (username, pin, role, name) VALUES ('superadmin', '0000', 'superadmin', 'Super Admin')").run();
}

module.exports = db;
