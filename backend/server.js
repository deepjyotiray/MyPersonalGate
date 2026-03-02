require('dotenv').config();
const express = require('express');
const cors = require('cors');
const jwt = require('jsonwebtoken');
const multer = require('multer');
const path = require('path');
const db = require('./db');
const { uploadVehicleDocs, renameVehicleDocs } = require('./googleDrive');

const app = express();
app.use(cors());
app.use(express.json());
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));
app.use('/admin', express.static(path.join(__dirname, 'public')));

const storage = multer.diskStorage({
  destination: 'uploads/',
  filename: (req, file, cb) => cb(null, `${Date.now()}-${file.originalname}`)
});
const upload = multer({ storage, limits: { fileSize: 5 * 1024 * 1024 } });

const auth = (req, res, next) => {
  const token = req.headers.authorization?.split(' ')[1];
  if (!token) return res.status(401).json({ error: 'Unauthorized' });
  try { 
    const decoded = jwt.verify(token, process.env.JWT_SECRET);
    req.user = decoded;
    next(); 
  }
  catch { res.status(401).json({ error: 'Invalid token' }); }
};

const adminOnly = (req, res, next) => {
  if (req.user?.role !== 'admin' && req.user?.role !== 'superadmin') return res.status(403).json({ error: 'Admin only' });
  next();
};

const superadminOnly = (req, res, next) => {
  if (req.user?.role !== 'superadmin') return res.status(403).json({ error: 'Superadmin only' });
  next();
};

const r = express.Router();

const vehicleUpload = multer({ storage, limits: { fileSize: 5 * 1024 * 1024 } }).fields([
  { name: 'rc_book', maxCount: 1 },
  { name: 'rent_agreement', maxCount: 1 }
]);

const toVehicleJson = (row) => row ? ({
  id: String(row.id),
  vehicle_number: row.vehicle_number,
  owner_name: row.owner_name,
  mobile_number: row.mobile_number,
  building: row.building,
  flat_number: row.flat_number,
  vehicle_type: row.vehicle_type,
  tag_number: row.tag_number ?? null,
  society_sticker_number: row.society_sticker_number ?? null,
  is_tenant: row.is_tenant === 1,
  rc_book_url: row.rc_book_url ?? null,
  rent_agreement_url: row.rent_agreement_url ?? null,
  vehicle_photo_url: row.vehicle_photo_url ?? null,
  is_active: row.is_active === 1
}) : null;

r.get('/vehicles', auth, (req, res) => {
  const rows = db.prepare('SELECT * FROM vehicles WHERE is_active = 1 ORDER BY owner_name ASC').all();
  res.json(rows.map(toVehicleJson));
});

r.delete('/vehicles/:id', auth, (req, res) => {
  const result = db.prepare('UPDATE vehicles SET is_active = 0 WHERE id = ?').run(req.params.id);
  if (!result.changes) return res.status(404).json({ error: 'Vehicle not found' });
  res.json({ success: true });
});

r.patch('/vehicles/:id', auth, (req, res) => {
  vehicleUpload(req, res, (err) => {
    if (err) return res.status(400).json({ error: err.message });
    const existing = db.prepare('SELECT * FROM vehicles WHERE id = ?').get(req.params.id);
    if (!existing) return res.status(404).json({ error: 'Vehicle not found' });
    const { vehicle_number, owner_name, mobile_number, building, flat_number, vehicle_type, tag_number, society_sticker_number, is_tenant } = req.body;
    const rc_book_url = req.files?.rc_book?.[0] ? `/uploads/${req.files.rc_book[0].filename}` : undefined;
    const rent_agreement_url = req.files?.rent_agreement?.[0] ? `/uploads/${req.files.rent_agreement[0].filename}` : undefined;
    const fields = [];
    const params = [];
    if (vehicle_number) { fields.push('vehicle_number = ?'); params.push(vehicle_number.toUpperCase()); }
    if (owner_name) { fields.push('owner_name = ?'); params.push(owner_name); }
    if (mobile_number) { fields.push('mobile_number = ?'); params.push(mobile_number); }
    if (building) { fields.push('building = ?'); params.push(building); }
    if (flat_number) { fields.push('flat_number = ?'); params.push(flat_number); }
    if (vehicle_type) { fields.push('vehicle_type = ?'); params.push(vehicle_type); }
    if (tag_number !== undefined) { fields.push('tag_number = ?'); params.push(tag_number || null); }
    if (society_sticker_number !== undefined) { fields.push('society_sticker_number = ?'); params.push(society_sticker_number || null); }
    if (is_tenant !== undefined) { fields.push('is_tenant = ?'); params.push(is_tenant === '1' ? 1 : 0); }
    if (rc_book_url) { fields.push('rc_book_url = ?'); params.push(rc_book_url); }
    if (rent_agreement_url) { fields.push('rent_agreement_url = ?'); params.push(rent_agreement_url); }
    if (!fields.length) return res.status(400).json({ error: 'No fields to update' });
    params.push(req.params.id);
    db.prepare(`UPDATE vehicles SET ${fields.join(', ')} WHERE id = ?`).run(...params);
    const updated = db.prepare('SELECT * FROM vehicles WHERE id = ?').get(req.params.id);
    res.json(toVehicleJson(updated));
    // rename Drive files if vehicle_number or owner_name changed
    const nameChanged = (owner_name && owner_name !== existing.owner_name) || (vehicle_number && vehicle_number.toUpperCase() !== existing.vehicle_number);
    if (nameChanged) {
      renameVehicleDocs({
        building: updated.building,
        flat_number: updated.flat_number,
        old_owner_name: existing.owner_name,
        old_vehicle_number: existing.vehicle_number,
        new_owner_name: updated.owner_name,
        new_vehicle_number: updated.vehicle_number,
      }).catch(console.error);
    }
    // upload new docs to Google Drive in background
    if (req.files && Object.keys(req.files).length)
      uploadVehicleDocs({ building: updated.building, flat_number: updated.flat_number, owner_name: updated.owner_name, vehicle_number: updated.vehicle_number, files: req.files }).catch(console.error);
  });
});

r.get('/vehicles/:vehicleNumber', auth, (req, res) => {
  const row = db.prepare('SELECT * FROM vehicles WHERE vehicle_number = ? AND is_active = 1').get(req.params.vehicleNumber.toUpperCase());
  if (!row) return res.status(404).json({ error: 'Vehicle not found' });
  res.json(toVehicleJson(row));
});

r.post('/vehicles', auth, (req, res) => {
  vehicleUpload(req, res, (err) => {
    if (err) return res.status(400).json({ error: err.message });
    const { vehicle_number, owner_name, mobile_number, building, flat_number, vehicle_type, tag_number, society_sticker_number, is_tenant } = req.body;
    if (!vehicle_number || !owner_name || !mobile_number || !building || !flat_number || !vehicle_type)
      return res.status(400).json({ error: 'Missing required fields' });
    const rc_book_url = req.files?.rc_book?.[0] ? `/uploads/${req.files.rc_book[0].filename}` : null;
    const rent_agreement_url = req.files?.rent_agreement?.[0] ? `/uploads/${req.files.rent_agreement[0].filename}` : null;
    try {
      const existing = db.prepare('SELECT id FROM vehicles WHERE vehicle_number = ?').get(vehicle_number.toUpperCase());
      let row;
      if (existing) {
        db.prepare(`UPDATE vehicles SET owner_name=?, mobile_number=?, building=?, flat_number=?, vehicle_type=?, tag_number=?, society_sticker_number=?, is_tenant=?, rc_book_url=COALESCE(?,rc_book_url), rent_agreement_url=COALESCE(?,rent_agreement_url), is_active=1 WHERE id=?`)
          .run(owner_name, mobile_number, building, flat_number, vehicle_type, tag_number||null, society_sticker_number||null, is_tenant==='1'?1:0, rc_book_url, rent_agreement_url, existing.id);
        row = db.prepare('SELECT * FROM vehicles WHERE id = ?').get(existing.id);
      } else {
        const result = db.prepare(`INSERT INTO vehicles (vehicle_number, owner_name, mobile_number, building, flat_number, vehicle_type, tag_number, society_sticker_number, is_tenant, rc_book_url, rent_agreement_url) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`)
          .run(vehicle_number.toUpperCase(), owner_name, mobile_number, building, flat_number, vehicle_type, tag_number||null, society_sticker_number||null, is_tenant==='1'?1:0, rc_book_url, rent_agreement_url);
        row = db.prepare('SELECT * FROM vehicles WHERE id = ?').get(result.lastInsertRowid);
      }
      res.status(201).json(toVehicleJson(row));
      // upload docs to Google Drive in background
      if (req.files && Object.keys(req.files).length)
        uploadVehicleDocs({ building: row.building, flat_number: row.flat_number, owner_name: row.owner_name, vehicle_number: row.vehicle_number, files: req.files }).catch(console.error);
    } catch (e) {
      res.status(500).json({ error: e.message });
    }
  });
});

r.post('/register', (req, res) => {
  const { username, pin, name } = req.body;
  if (!username || !pin || !name) return res.status(400).json({ error: 'Missing fields' });
  try {
    db.prepare('INSERT INTO users (username, pin, role, name) VALUES (?, ?, ?, ?)').run(username, pin, 'guard', name);
    const user = db.prepare('SELECT * FROM users WHERE username = ?').get(username);
    const token = jwt.sign({ role: user.role, userId: user.id, name: user.name }, process.env.JWT_SECRET, { expiresIn: '30d' });
    res.status(201).json({ token, role: user.role, name: user.name });
  } catch (e) {
    if (e.message.includes('UNIQUE')) return res.status(409).json({ error: 'Username already exists' });
    res.status(500).json({ error: e.message });
  }
});

r.post('/login', (req, res) => {
  const { username, pin } = req.body;
  const user = db.prepare('SELECT * FROM users WHERE username = ? AND is_active = 1').get(username);
  if (!user || user.pin !== pin) return res.status(401).json({ error: 'Invalid username or PIN' });
  const token = jwt.sign({ role: user.role, userId: user.id, name: user.name }, process.env.JWT_SECRET, { expiresIn: '30d' });
  res.json({ token, role: user.role, name: user.name });
});

r.get('/users', auth, adminOnly, (req, res) => {
  const rows = db.prepare('SELECT id, username, role, name, is_active, created_at FROM users').all();
  res.json(rows);
});

r.post('/users', auth, adminOnly, (req, res) => {
  const { username, pin, role, name } = req.body;
  if (!username || !pin || !role || !name) return res.status(400).json({ error: 'Missing fields' });
  // superadmin role can only be set by superadmin
  if (role === 'superadmin') return res.status(403).json({ error: 'Cannot create superadmin' });
  try {
    db.prepare('INSERT INTO users (username, pin, role, name) VALUES (?, ?, ?, ?)').run(username, pin, role, name);
    res.status(201).json({ success: true });
  } catch (e) {
    if (e.message.includes('UNIQUE')) return res.status(409).json({ error: 'Username already exists' });
    res.status(500).json({ error: e.message });
  }
});

r.patch('/users/:id', auth, adminOnly, (req, res) => {
  const target = db.prepare('SELECT role FROM users WHERE id = ?').get(req.params.id);
  if (!target) return res.status(404).json({ error: 'User not found' });
  if (target.role === 'superadmin') return res.status(403).json({ error: 'Cannot modify superadmin' });
  if (req.body.is_active === false && String(req.user.userId) === String(req.params.id)) return res.status(403).json({ error: 'Cannot deactivate yourself' });
  // admin cannot promote to superadmin or admin
  if (req.user.role === 'admin' && req.body.role && req.body.role !== 'guard') return res.status(403).json({ error: 'Admin can only assign guard role' });
  const { pin, name, role, is_active } = req.body;
  const fields = [], params = [];
  if (pin) { fields.push('pin = ?'); params.push(pin); }
  if (name) { fields.push('name = ?'); params.push(name); }
  if (req.body.username) { fields.push('username = ?'); params.push(req.body.username); }
  if (role && role !== 'superadmin') { fields.push('role = ?'); params.push(role); }
  if (is_active !== undefined) { fields.push('is_active = ?'); params.push(is_active ? 1 : 0); }
  if (!fields.length) return res.status(400).json({ error: 'Nothing to update' });
  params.push(req.params.id);
  db.prepare(`UPDATE users SET ${fields.join(', ')} WHERE id = ?`).run(...params);
  res.json({ success: true });
});

r.delete('/users/:id', auth, adminOnly, (req, res) => {
  const target = db.prepare('SELECT role FROM users WHERE id = ?').get(req.params.id);
  if (!target) return res.status(404).json({ error: 'User not found' });
  if (target.role === 'superadmin') return res.status(403).json({ error: 'Cannot deactivate superadmin' });
  if (String(req.user.userId) === String(req.params.id)) return res.status(403).json({ error: 'Cannot deactivate yourself' });
  db.prepare('UPDATE users SET is_active = 0 WHERE id = ?').run(req.params.id);
  res.json({ success: true });
});

r.post('/entry', auth, upload.single('photo'), (req, res) => {
  const { type, name, flat_number, host_name, vehicle_number, has_sticker, purpose, id_proof } = req.body;
  const photo_url = req.file ? `/uploads/${req.file.filename}` : null;
  try {
    const stmt = db.prepare(`
      INSERT INTO entries (type, name, flat_number, host_name, vehicle_number, has_sticker, purpose, id_proof, photo_url)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    `);
    const result = stmt.run(type, name, flat_number, host_name || null, vehicle_number || null, has_sticker === 'true' ? 1 : 0, purpose || null, id_proof || null, photo_url);
    res.status(201).json(db.prepare('SELECT * FROM entries WHERE id = ?').get(result.lastInsertRowid));
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

r.post('/exit/:id', auth, (req, res) => {
  const result = db.prepare(`UPDATE entries SET exit_time = datetime('now','localtime'), is_active = 0 WHERE id = ?`).run(req.params.id);
  if (!result.changes) return res.status(404).json({ error: 'Entry not found' });
  res.json(toEntryJson(db.prepare('SELECT * FROM entries WHERE id = ?').get(req.params.id)));
});

const toEntryJson = (row) => row ? ({
  id: String(row.id),
  visitor_id: String(row.id),
  visitor: { id: String(row.id), visitor_name: row.name, mobile_number: row.mobile_number ?? '', photo_url: row.photo_url ? `https://ananta.healthymealspot.com${row.photo_url}` : null, id_photo_url: row.id_photo_url ? `https://ananta.healthymealspot.com${row.id_photo_url}` : null },
  vehicle_number: row.vehicle_number ?? null,
  building: row.building ?? '',
  flat_number: row.flat_number,
  entry_time: row.entry_time,
  exit_time: row.exit_time ?? null,
  guard_id: '1',
  status: row.is_active ? 'INSIDE' : 'EXITED',
  notes: row.purpose ?? null
}) : null;

r.get('/active', auth, (req, res) => {
  res.json(db.prepare('SELECT * FROM entries WHERE is_active = 1 ORDER BY entry_time DESC').all());
});

// --- visitor-entries aliases (used by Android app) ---
r.post('/visitor-entries', auth, upload.any(), (req, res) => {
  const { visitor_name, mobile_number, building, flat_number, vehicle_number, notes } = req.body;
  const photoFile = req.files?.find(f => f.fieldname === 'photo');
  const idPhotoFile = req.files?.find(f => f.fieldname === 'id_photo');
  const photo_url = photoFile ? `/uploads/${photoFile.filename}` : null;
  const id_photo_url = idPhotoFile ? `/uploads/${idPhotoFile.filename}` : null;
  try {
    // Auto-exit any active entry for the same vehicle
    if (vehicle_number) {
      db.prepare(`UPDATE entries SET exit_time = datetime('now','localtime'), is_active = 0 WHERE vehicle_number = ? AND is_active = 1`).run(vehicle_number.toUpperCase());
    }
    const result = db.prepare(`
      INSERT INTO entries (type, name, mobile_number, building, flat_number, vehicle_number, purpose, photo_url, id_photo_url)
      VALUES ('visitor', ?, ?, ?, ?, ?, ?, ?, ?)
    `).run(visitor_name, mobile_number, building, flat_number, vehicle_number || null, notes || null, photo_url, id_photo_url);
    res.status(201).json(toEntryJson(db.prepare('SELECT * FROM entries WHERE id = ?').get(result.lastInsertRowid)));
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

r.post('/visitors', auth, (req, res) => {
  const { visitor_name, mobile_number } = req.body;
  res.status(201).json({ id: String(Date.now()), visitor_name, mobile_number, id_photo_url: '' });
});

r.patch('/visitor-entries/:id/exit', auth, (req, res) => {
  const result = db.prepare(`UPDATE entries SET exit_time = datetime('now','localtime'), is_active = 0 WHERE id = ?`).run(req.params.id);
  if (!result.changes) return res.status(404).json({ error: 'Entry not found' });
  res.json(toEntryJson(db.prepare('SELECT * FROM entries WHERE id = ?').get(req.params.id)));
});

r.get('/visitor-entries/active', auth, (req, res) => {
  res.json(db.prepare('SELECT * FROM entries WHERE is_active = 1 ORDER BY entry_time DESC').all().map(toEntryJson));
});

r.get('/visitor-entries/vehicle/:vehicleNumber', auth, (req, res) => {
  const rows = db.prepare('SELECT * FROM entries WHERE vehicle_number = ? ORDER BY entry_time DESC').all(req.params.vehicleNumber.toUpperCase());
  res.json(rows.map(toEntryJson));
});

r.get('/history', auth, (req, res) => {
  const { date, type } = req.query;
  let query = 'SELECT * FROM entries WHERE 1=1';
  const params = [];
  if (date) { query += ' AND DATE(entry_time) = ?'; params.push(date); }
  if (type) { query += ' AND type = ?'; params.push(type); }
  query += ' ORDER BY entry_time DESC LIMIT 200';
  res.json(db.prepare(query).all(...params).map(toEntryJson));
});

app.use('/', r);

app.listen(process.env.PORT, () => console.log(`Ananta server running on port ${process.env.PORT}`));
