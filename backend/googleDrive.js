const { google } = require('googleapis');
const fs = require('fs');
const path = require('path');

const ROOT_FOLDER_ID = '1TbDs-sT---XixrVE88VyXS3R4C1eF5T-';

const { client_id, client_secret, redirect_uris } = JSON.parse(fs.readFileSync(path.join(__dirname, 'oauth-client.json'))).installed;
const oAuth2Client = new google.auth.OAuth2(client_id, client_secret, redirect_uris[0]);
oAuth2Client.setCredentials(JSON.parse(fs.readFileSync(path.join(__dirname, 'token.json'))));

const drive = google.drive({ version: 'v3', auth: oAuth2Client });

async function getOrCreateFolder(name, parentId) {
  const res = await drive.files.list({
    q: `name='${name}' and '${parentId}' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false`,
    fields: 'files(id)',
  });
  if (res.data.files.length) return res.data.files[0].id;
  const folder = await drive.files.create({
    requestBody: { name, mimeType: 'application/vnd.google-apps.folder', parents: [parentId] },
    fields: 'id',
  });
  return folder.data.id;
}

async function uploadVehicleDocs({ building, flat_number, owner_name, vehicle_number, files }) {
  const towerFolderId = await getOrCreateFolder(building, ROOT_FOLDER_ID);
  const flatFolderId = await getOrCreateFolder(flat_number, towerFolderId);

  const docTypes = { rc_book: 'RC', rent_agreement: 'RentAgreement' };
  const sanitized = `${owner_name}_${vehicle_number}`.replace(/\s+/g, '_');

  for (const [field, label] of Object.entries(docTypes)) {
    if (!files[field]?.[0]) continue;
    const file = files[field][0];
    const ext = path.extname(file.originalname) || path.extname(file.filename);
    await drive.files.create({
      requestBody: { name: `${sanitized}_${label}${ext}`, parents: [flatFolderId] },
      media: { mimeType: file.mimetype, body: fs.createReadStream(file.path) },
    });
  }
}

async function renameVehicleDocs({ building, flat_number, old_owner_name, old_vehicle_number, new_owner_name, new_vehicle_number }) {
  const towerFolderId = await getOrCreateFolder(building, ROOT_FOLDER_ID);
  const flatFolderId = await getOrCreateFolder(flat_number, towerFolderId);

  const oldSanitized = `${old_owner_name}_${old_vehicle_number}`.replace(/\s+/g, '_');
  const newSanitized = `${new_owner_name}_${new_vehicle_number}`.replace(/\s+/g, '_');

  const res = await drive.files.list({
    q: `'${flatFolderId}' in parents and name contains '${oldSanitized}' and trashed=false`,
    fields: 'files(id, name)',
  });

  for (const file of res.data.files) {
    const newName = file.name.replace(oldSanitized, newSanitized);
    await drive.files.update({ fileId: file.id, requestBody: { name: newName } });
  }
}

module.exports = { uploadVehicleDocs, renameVehicleDocs };
