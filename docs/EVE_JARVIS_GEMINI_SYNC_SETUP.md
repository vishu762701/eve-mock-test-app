# EVE JARVIS Gemini Gem — Google Drive Synchronization Setup Guide

This guide details the architecture, configuration, and exact instructions for automated, event-driven GitHub-to-Google-Drive synchronization for the **EVE JARVIS Gemini Gem**.

---

## 1. Architecture Overview

```
[ Git Push to main / Build Eve APK Complete ]
                   │
                   ▼
  [ GitHub Actions: sync-jarvis-gemini.yml ]
                   │
                   ├── 1. Validates file security & sanitizes credentials (fail-closed)
                   ├── 2. Formats code with line numbers & commit SHA
                   ├── 3. Packages repository into 7 structured volumes (Gemini <= 10 files cap)
                   ├── 4. Queries GitHub REST API for verified CI/CD build status
                   │
                   ▼
  [ Dual-Mode Google Drive API v3 Client ]
       ├── Option A: Google User OAuth 2.0 (Personal @gmail.com accounts — uses user storage quota)
       └── Option B: Google Cloud Service Account (Google Workspace Shared Drive)
                   │
                   ├── Atomic publishing: Master index published ONLY after all 6 volumes succeed
                   ├── Idempotent files.update (Preserves stable Google Drive document IDs)
                   ├── Exponential backoff retry on transient 429 / 5xx rate limits
                   └── Least-privilege access & active deletion of any public permissions
                   │
                   ▼
      [ Google Drive Folder: EVE-JARVIS-KNOWLEDGE ]
                   │
                   ▼
      [ EVE JARVIS Gemini Gem Knowledge Base ]
```

### Critical Storage Quota Architecture Note
* **Personal Google Accounts (`@gmail.com`)**: In Google Drive, when a Service Account creates files in a shared folder, the file is owned by the Service Account. Because Google Cloud assigns 0 bytes of Drive storage quota to Service Accounts on personal Drive, uploads fail with `403 storageQuotaExceeded`. **Option A (Google User OAuth 2.0)** executes API requests under your personal Google account: files are owned by you, utilize your standard 15 GB+ Drive storage quota, and work seamlessly.
* **Google Workspace Shared Drives**: If using a corporate or educational Google Workspace Shared Drive (Team Drive), files are owned by the Shared Drive. **Option B (Google Cloud Service Account)** works seamlessly in Shared Drives.

---

## 2. Knowledge Volumes Structure (Gemini Gem <= 10 Files Cap)

Custom Gemini Gems support a **maximum of 10 persistent Knowledge files** (up to 100 MB each). The sync engine consolidates the entire repository (~78k lines, ~4.2 MB) into **7 structured volumes**, leaving 3 spare slots for user documents:

| Volume Name | Title | Contents |
| :--- | :--- | :--- |
| `00_INDEX_AND_SYSTEM_STATUS.md` | Master Index & System Status | Real CI build/test status, commit metadata, permanent invariants, full included/excluded file indices. |
| `01_ENGINEERING_RULES_AND_STANDARDS.md` | Engineering Rules & Architecture | `ENGINEERING_STANDARD.md`, `SOURCE_OF_TRUTH.md`, `AGENTS.md`, `GEMINI.md`, `CLAUDE.md`, `DECISIONS.md`, `BACKLOG.md`, `APPROVED_UI_IMPLEMENTATION_AUDIT.md`, `firestore.rules`. |
| `02_ANDROID_CORE_AND_UI.md` | Android Production Kotlin Code | All production Kotlin files: UI activities, view models, repositories, custom views, transitions (`app/src/main/java/**/*.kt`). |
| `03_ANDROID_LAYOUTS_AND_RESOURCES.md` | Android Layouts & Resources | All 95 Android XML layouts, Apple iOS 26 typography styles, colors, themes (`app/src/main/res/**/*.xml`). |
| `04_BACKEND_WORKER_AND_STORAGE.md` | Backend Cloudflare Worker & D1 | Hono TypeScript routes, server scoring, answer integrity, D1 schema, migrations (`backend/src/**/*.ts`, `backend/schema.sql`). |
| `05_TEST_SUITES_AND_VERIFICATION.md` | Automated Test Suites | Android unit tests, native UI tests, backend tests, script test runners (`app/src/test/`, `backend/test/`). |
| `06_CI_CD_WORKFLOWS_AND_SCRIPTS.md` | CI/CD Workflows & Scripts | GitHub Actions workflows (`.github/workflows/*.yml`), verification scripts, and Gradle build configurations. |

---

## 3. Setup Instructions

Choose either **Option A** (recommended for personal Google accounts) or **Option B** (for Google Workspace Shared Drives).

---

### OPTION A: Google User OAuth 2.0 (Recommended for Personal Google Accounts)

This approach authorizes synchronization under your own Google account, using your personal Google Drive storage quota.

#### Step A1: Enable Google Drive API in Google Cloud Console
1. Open the [Google Cloud Console](https://console.cloud.google.com/).
2. Create a project (e.g. `eve-jarvis-sync`) or select an existing one.
3. In the search box, search for **Google Drive API** and click **Enable**.

#### Step A2: Configure OAuth Consent Screen & Create OAuth Client ID
1. Navigate to **APIs & Services** > **OAuth consent screen**.
2. Select **External**, fill in the App name (`EVE JARVIS Sync`) and your email address.
3. Under **Scopes**, click **Add or Remove Scopes** and add:
   - `https://www.googleapis.com/auth/drive.file` (restricted to files created/opened by the app).
4. Under **Test users**, add your own Google email address. Click **Save and Continue**.
5. Navigate to **APIs & Services** > **Credentials**.
6. Click **Create Credentials** > **OAuth client ID**.
   - **Application type**: `Web application`
   - **Name**: `EVE JARVIS GitHub Actions Sync`
   - **Authorized redirect URIs**: Add `https://developers.google.com/oauthplayground`
7. Click **Create** and copy your **Client ID** and **Client Secret**.

#### Step A3: Generate Refresh Token
You can generate the refresh token in 1 minute using the built-in helper script:
```bash
# 1. Print authorization URL
node .github/scripts/gdrive-sync/oauth-helper.mjs --client-id=YOUR_CLIENT_ID

# 2. Open the URL in browser, sign in with your Google account, and grant permission.
# 3. Copy the "code" parameter returned on the redirect page, then run:
node .github/scripts/gdrive-sync/oauth-helper.mjs \
  --client-id=YOUR_CLIENT_ID \
  --client-secret=YOUR_CLIENT_SECRET \
  --code=YOUR_AUTHORIZATION_CODE
```
*(Alternatively, use [Google OAuth 2.0 Playground](https://developers.google.com/oauthplayground) with your Client ID and Client Secret, select `https://www.googleapis.com/auth/drive.file`, and exchange authorization code for tokens).*

#### Step A4: Create Destination Folder in Google Drive
1. In [Google Drive](https://drive.google.com/), create a new folder named `EVE-JARVIS-KNOWLEDGE`.
2. Open the folder and copy the **Folder ID** from your browser's address bar (the alphanumeric string after `/folders/`).

#### Step A5: Add GitHub Repository Secrets
Go to your GitHub repository: `https://github.com/vishu762701/eve-mock-test-app/settings/secrets/actions` and add:

| Secret Name | Value |
| :--- | :--- |
| `GDRIVE_CLIENT_ID` | Your Google OAuth Client ID |
| `GDRIVE_CLIENT_SECRET` | Your Google OAuth Client Secret |
| `GDRIVE_REFRESH_TOKEN` | The generated Google OAuth Refresh Token |
| `GDRIVE_FOLDER_ID` | The Google Drive Folder ID from Step A4 |
| `GDRIVE_AUTHORIZED_USERS` | (Optional) Comma-separated list of additional Google emails granted read access |

---

### OPTION B: Google Cloud Service Account (For Workspace Shared Drives)

Use this option if you are using a Google Workspace Shared Drive (Team Drive) where files are owned by the organization rather than individual personal accounts.

#### Step B1: Create Service Account & Key
1. In Google Cloud Console, navigate to **IAM & Admin** > **Service Accounts**.
2. Click **Create Service Account**:
   - Name: `eve-jarvis-sync`
   - Click **Done**.
3. Click the created service account email > **Keys** tab > **Add Key** > **Create new key** (JSON).
4. Download the JSON key file.

#### Step B2: Create Shared Drive Folder & Share with Service Account
1. In Google Drive, open your **Shared Drive**.
2. Create folder `EVE-JARVIS-KNOWLEDGE` and copy the Folder ID from the URL.
3. Share the folder with the Service Account email as **Content Manager** or **Editor**.

#### Step B3: Add GitHub Repository Secrets
Add to `https://github.com/vishu762701/eve-mock-test-app/settings/secrets/actions`:

| Secret Name | Value |
| :--- | :--- |
| `GDRIVE_SERVICE_ACCOUNT_KEY` | Entire JSON content of the downloaded service account key file |
| `GDRIVE_FOLDER_ID` | The Google Drive Folder ID from Step B2 |
| `GDRIVE_AUTHORIZED_USERS` | (Optional) Comma-separated list of Google emails granted access |

---

## 4. Connecting Google Drive Documents to EVE JARVIS Gemini Gem

Once secrets are configured and the sync workflow runs:
1. Open [Google Gemini](https://gemini.google.com/) and navigate to your **EVE JARVIS** custom Gem.
2. Click **Edit Gem** (pencil icon).
3. Under **Knowledge**, click **Drive** or **Add Files**.
4. Navigate to your `EVE-JARVIS-KNOWLEDGE` folder and select the 7 synchronized documents:
   - `00_INDEX_AND_SYSTEM_STATUS.md`
   - `01_ENGINEERING_RULES_AND_STANDARDS.md`
   - `02_ANDROID_CORE_AND_UI.md`
   - `03_ANDROID_LAYOUTS_AND_RESOURCES.md`
   - `04_BACKEND_WORKER_AND_STORAGE.md`
   - `05_TEST_SUITES_AND_VERIFICATION.md`
   - `06_CI_CD_WORKFLOWS_AND_SCRIPTS.md`
5. Click **Save**.

### Why Document Links Remain Permanently Stable
The sync engine uses Google Drive in-place updates (`PATCH /upload/drive/v3/files/{id}?uploadType=media`) which preserves the unique Google Drive file ID indefinitely across commits. You **never need to re-link** files in your Gemini Gem after pushes.

---

## 5. Verification and Safety Guarantees

* **Atomic Master Index**: The master index `00_INDEX_AND_SYSTEM_STATUS.md` is uploaded ONLY after all 6 domain volumes are successfully published and verified. If a network interruption or error occurs during volume upload, the sync halts immediately and does not publish a mismatched master index.
* **Dry-Run Safety**: If secrets are not yet configured in GitHub, the workflow automatically runs in pre-generation dry-run mode, verifying all 670+ files without failing the CI pipeline.
* **Strict Privacy**: Checks and deletes any public (`anyone`) access on the Google Drive folder.
