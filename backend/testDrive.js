const { google } = require('googleapis');
const fs = require('fs');
const path = require('path');

const ROOT_FOLDER_ID = '1TbDs-sT---XixrVE88VyXS3R4C1eF5T-';
const { client_id, client_secret, redirect_uris } = JSON.parse(fs.readFileSync('./oauth-client.json')).installed;
const oAuth2Client = new google.auth.OAuth2(client_id, client_secret, redirect_uris[0]);
oAuth2Client.setCredentials(JSON.parse(fs.readFileSync('./token.json')));
const drive = google.drive({ version: 'v3', auth: oAuth2Client });

async function listFolder(folderId, label) {
  const res = await drive.files.list({
    q: `'${folderId}' in parents and trashed=false`,
    fields: 'files(id, name, mimeType)',
  });
  console.log(`\n📁 ${label} (${folderId}):`);
  res.data.files.forEach(f => console.log(`  ${f.mimeType.includes('folder') ? '📁' : '📄'} ${f.name} [${f.id}]`));
  return res.data.files;
}

(async () => {
  const rootItems = await listFolder(ROOT_FOLDER_ID, 'Root');
  for (const item of rootItems) {
    if (item.mimeType.includes('folder')) {
      const subItems = await listFolder(item.id, item.name);
      for (const sub of subItems) {
        if (sub.mimeType.includes('folder')) await listFolder(sub.id, `${item.name}/${sub.name}`);
      }
    }
  }
})();
