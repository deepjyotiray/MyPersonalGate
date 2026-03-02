const { google } = require('googleapis');
const fs = require('fs');
const readline = require('readline');

const { client_id, client_secret, redirect_uris } = JSON.parse(fs.readFileSync('./oauth-client.json')).installed;
const oAuth2Client = new google.auth.OAuth2(client_id, client_secret, redirect_uris[0]);

const url = oAuth2Client.generateAuthUrl({
  access_type: 'offline',
  scope: ['https://www.googleapis.com/auth/drive'],
});

console.log('Open this URL in your browser:\n', url);

const rl = readline.createInterface({ input: process.stdin, output: process.stdout });
rl.question('\nPaste the code here: ', async (code) => {
  const { tokens } = await oAuth2Client.getToken(code);
  fs.writeFileSync('./token.json', JSON.stringify(tokens));
  console.log('✅ token.json saved!');
  rl.close();
});
