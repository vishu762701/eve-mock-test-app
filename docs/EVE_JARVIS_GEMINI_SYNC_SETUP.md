# EVE JARVIS Gemini Gem — Google Drive Synchronization Setup Guide

This guide details the architecture and minimum exact steps to configure automated, event-driven GitHub-to-Google-Drive synchronization for the **EVE JARVIS Gemini Gem**.

---

## 1. Architecture Overview

```
[ Git Push to main ] 
        │
        ▼
[ GitHub Actions: sync-jarvis-gemini.yml ]
        │
        ├── 1. Validates file security & sanitizes credentials
        ├── 2. Formats code with line numbers & commit SHA
        ├── 3. Packages repository into 7 structured volumes (Gemini <= 10 files cap)
        ├── 4. Queries GitHub REST API for verified CI/CD build status
        │
        ▼
[ Google Cloud Service Account (Scope: drive.file) ]
        │
        ├── Idempotent files.update (Preserves stable Google Drive file IDs)
        └── Least-privilege sharing with authorized Google accounts (Zero public access)
        │
        ▼
[ Google Drive Folder: EVE-JARVIS-KNOWLEDGE ]
        │
        ▼
[ EVE JARVIS Gemini Gem Knowledge Base ]
```

### Why Event-Driven GitHub Actions Over Apps Script Polling
* **Immediate & Deterministic**: Runs directly when commits land on `main` or when builds finish, eliminating polling lag and unauthenticated rate limits.
* **Exact Commit Line Numbers**: Every file is formatted with standard line numbers (`1: ...`) and the exact commit SHA, allowing Gemini to cite code with absolute precision.
* **Stable Document IDs**: Uses Google Drive API in-place updates (`files.update`) rather than delete-and-recreate cycles, preserving stable document links in your Gemini Gem.
* **Strict Confidentiality**: Uses Google Cloud service account with least-privilege `drive.file` scope and explicitly checks that no public (`anyone`) access exists.

---

## 2. Knowledge Volumes Structure (Under Gemini Gem 10-File Limit)

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

## 3. Minimum Exact Manual Setup Instructions

Follow these 5 steps to authorize Google Drive synchronization.

### Step 1: Create a Google Cloud Project & Enable Google Drive API
1. Open the [Google Cloud Console](https://console.cloud.google.com/).
2. Create a new project (e.g., `eve-mock-test-sync`) or select an existing project.
3. In the search bar at the top, type **Google Drive API** and select it from Marketplace / APIs.
4. Click **Enable**.

### Step 2: Create a Service Account & Download Key
1. In the Google Cloud Console, navigate to **IAM & Admin** > **Service Accounts**.
2. Click **Create Service Account**.
   - **Service account name**: `eve-jarvis-sync`
   - **Service account ID**: `eve-jarvis-sync`
   - Click **Create and Continue**.
3. (Optional) Skip assigning project roles (no project-level roles are needed; Drive permissions will be granted directly at the folder level). Click **Done**.
4. In the Service Accounts list, click the newly created service account email.
5. Go to the **Keys** tab > **Add Key** > **Create new key**.
6. Select **JSON** and click **Create**.
7. A JSON file will download to your device (keep this confidential; do not commit it).

### Step 3: Create the Google Drive Destination Folder
1. Open [Google Drive](https://drive.google.com/) using your Google account.
2. Create a new folder named `EVE-JARVIS-KNOWLEDGE`.
3. Open the folder and copy the **Folder ID** from the browser URL:
   - URL: `https://drive.google.com/drive/folders/1a2b3c4d5e6f7g8h9i0j...`
   - The Folder ID is the alphanumeric string after `/folders/`.
4. Right-click the folder > **Share**:
   - In the "Add people and groups" field, paste the **Service Account email** (e.g., `eve-jarvis-sync@your-project.iam.gserviceaccount.com`).
   - Role: **Editor** (required so the service account can upload files using your storage quota).
   - Uncheck "Notify people" and click **Share**.

### Step 4: Configure GitHub Repository Secrets
1. Go to your GitHub repository: `https://github.com/vishu762701/eve-mock-test-app/settings/secrets/actions`
2. Add the following repository secrets:

| Secret Name | Value | Description |
| :--- | :--- | :--- |
| `GDRIVE_SERVICE_ACCOUNT_KEY` | Entire contents of the downloaded Service Account JSON file | Authenticates GitHub Actions to Google Drive via RS256 JWT. |
| `GDRIVE_FOLDER_ID` | The Google Drive Folder ID from Step 3 | Target folder where knowledge volumes are published. |
| `GDRIVE_AUTHORIZED_USERS` | (Optional) `your-email@gmail.com, colleague@example.com` | Comma-separated list of Google accounts granted read access. |

### Step 5: Connect Folder to EVE JARVIS Gemini Gem
1. Open [Google Gemini](https://gemini.google.com/) and go to your **EVE JARVIS** custom Gem.
2. Click **Edit Gem**.
3. Under **Knowledge**, click **Add Files** or **Drive**.
4. Select the 7 synchronized documents from your `EVE-JARVIS-KNOWLEDGE` folder (or select `00_INDEX_AND_SYSTEM_STATUS.md` and the volumes relevant to your session).
5. Click **Save**.

---

## 4. Verification and Safety Guarantees

* **Zero Breakage Guarantee**: If GitHub Secrets are not configured, the workflow executes a dry-run validation and exits with code 0 without failing the main branch.
* **Stable IDs**: Files in Google Drive are updated in-place. You do not need to re-link documents in your Gemini Gem after each commit.
* **Confidentiality**: Access is restricted strictly to the service account and emails listed in `GDRIVE_AUTHORIZED_USERS`. Public sharing (`anyone with link`) is prevented and actively scrubbed if detected.
