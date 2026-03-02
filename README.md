# Ananta Towers - Parking & Visitor Entry App

## Backend Setup

### Prerequisites
- Node.js 18+
- PostgreSQL

### Steps
```bash
cd backend
npm install

# Edit .env with your DB credentials and set GUARD_PIN
# Default PIN is 1234 — CHANGE THIS

npm start
```

### DNS Setup
1. Add an **A record** in your DNS for `api.healthymealspot.com` pointing to your server IP
2. Copy `nginx.conf` to `/etc/nginx/sites-available/ananta-api`
3. Update the `alias` path in nginx.conf to your actual uploads folder path
4. Run: `sudo ln -s /etc/nginx/sites-available/ananta-api /etc/nginx/sites-enabled/`
5. Get SSL cert: `sudo certbot --nginx -d api.healthymealspot.com`
6. Reload nginx: `sudo nginx -s reload`

---

## Android App Setup

### Prerequisites
- Android Studio Hedgehog or newer
- Android SDK 26+

### Steps
1. Open `android/` folder in Android Studio
2. The backend URL is set to `https://api.healthymealspot.com/` in `ApiService.kt`
3. Build & run on device or emulator

---

## API Reference

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/login` | Guard PIN login → returns JWT |
| POST | `/entry` | New entry (multipart/form-data) |
| POST | `/exit/:id` | Mark exit |
| GET | `/active` | Currently inside |
| GET | `/history?type=visitor&date=2024-01-01` | Entry history |

## Entry Types & Fields

| Field | visitor | delivery | vehicle |
|-------|---------|----------|---------|
| name | ✅ | ✅ (company) | ✅ |
| flat_number | ✅ | ✅ | ✅ |
| host_name | ✅ | ✅ | - |
| vehicle_number | optional | optional | ✅ |
| has_sticker | - | - | ✅ |
| purpose | ✅ | - | - |
| id_proof | ✅ | - | - |
| photo | ✅ | ✅ | ✅ |
