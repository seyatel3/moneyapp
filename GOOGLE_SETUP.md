# Google API Setup Guide

This guide walks you through setting up Google API credentials for Google Sheets sync in MoneyAPP.

## Step 1: Create a Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Click **"Select a project"** → **"NEW PROJECT"**
3. Name it (e.g., "MoneyAPP") and click **"CREATE"**
4. Wait for the project to be created and select it

## Step 2: Enable APIs

1. In the left sidebar, go to **"APIs & Services"** → **"Library"**
2. Search for **"Google Sheets API"** and click **"Enable"**
3. Search for **"Google Drive API"** and click **"Enable"**

## Step 3: Configure OAuth Consent Screen

1. Go to **"APIs & Services"** → **"OAuth consent screen"**
2. Select **"External"** user type → **"CREATE"**
3. Fill in:
   - **App name:** MoneyAPP
   - **User support email:** Your email
   - **Developer contact email:** Your email
4. Click **"SAVE AND CONTINUE"**
5. **Scopes page:** Click **"ADD OR REMOVE SCOPES"**, add:
   - `https://www.googleapis.com/auth/spreadsheets`
   - `https://www.googleapis.com/auth/drive.file`
6. Click **"SAVE AND CONTINUE"**
7. **Test users:** Add your Google account email
8. Click **"SAVE AND CONTINUE"** → **"BACK TO DASHBOARD"**

## Step 4: Create OAuth 2.0 Client ID

1. Go to **"APIs & Services"** → **"Credentials"**
2. Click **"+ CREATE CREDENTIALS"** → **"OAuth client ID"**
3. Application type: **"Desktop app"**
4. Name: "MoneyAPP Desktop"
5. Click **"CREATE"**
6. Note the **Client ID** and **Client Secret**

## Step 5: Configure Redirect URI

The app uses `moneyapp://oauth2-callback` as the redirect URI.

1. In your OAuth client settings, add a redirect URI:
   - `moneyapp://oauth2-callback`
2. This is already configured in the app's `AndroidManifest.xml`

## Step 6: Enter Credentials in the App

1. Open MoneyAPP
2. Go to **Settings** (⚙️ icon)
3. Under **Google Синхронизация**:
   - Enter your **Client ID**
   - Enter your **Client Secret**
   - Click **"Подключить Google аккаунт"**
4. A WebView will open for OAuth authorization
5. Sign in with your Google account and grant permissions
6. The app will use the spreadsheet from the link in Settings — or create a new one if the field is empty — and start syncing

## Optional: sync into your own spreadsheet

Since v1.8.0 the app has an optional field "Ссылка на свою таблицу (необязательно)"
in Settings → Синхронизация. Paste the link of an existing Google Spreadsheet
(`https://docs.google.com/spreadsheets/d/…`) to sync into it instead of a new one.
On first connect the app:
1. checks the spreadsheet is accessible from this Google account;
2. adds a "Транзакции" tab (with the header row) if it doesn't exist;
3. syncs there. Your other tabs are left untouched.

## Scopes Used

| Scope | Purpose |
|-------|---------|
| `https://www.googleapis.com/auth/spreadsheets` | Read/write Google Sheets |
| `https://www.googleapis.com/auth/drive.readonly` | Read-only access to Drive metadata, so the app can open your existing spreadsheet (v1.8.0; previously `drive.file`, which only allowed app-created files) |

## Troubleshooting

### "Invalid client" error
- Double-check Client ID and Client Secret
- Make sure the OAuth client type is "Desktop app"

### "Redirect URI mismatch"
- The redirect URI must be exactly `moneyapp://oauth2-callback`
- Check the `AndroidManifest.xml` intent-filter

### "Access blocked"
- Your account may not be in the test users list
- Go to OAuth consent screen → Test users → Add your account

### "API not enabled"
- Make sure both Sheets API and Drive API are enabled
- Check in "APIs & Services" → "Dashboard"

## Security Notes

- Client Secret is stored in EncryptedSharedPreferences (Jetpack Security)
- Access tokens are automatically refreshed
- Tokens are encrypted at rest using AES-256
- No GMS/Play Services are used — all via raw HTTPS
