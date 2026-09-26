# Phase 0: Full Audit Report of Eve App Backend & Data Layer

**Date**: September 26, 2026  
**Project**: Eve (Android Mock Test App — Kotlin, MVVM)  
**Scope**: Complete inventory of Firestore collections, documents, fields, subcollections, Firebase Storage paths, Cloud Functions, Repository methods, and Security Rules prior to Cloudflare Worker + D1 + Supabase Storage migration.

---

## 1. Firestore Collections & Field Schema Inventory

### 1.1 `exams`
- **Purpose**: Stores mock test exam definitions, scheduling, and syllabus metadata.
- **Document ID**: Auto-generated string ID (e.g. `doc.id`).
- **Fields**:
  - `id`: `String` (Document ID)
  - `examName`: `String` (Name of the exam, e.g., "SSC CGL 2024 Mock 1")
  - `timeLimitMinutes`: `Int` (Exam duration in minutes, default: 30)
  - `category`: `String` (e.g., "SSC", "UPSC", "Banking", "Railway", "State PSC", "Police", "Defence", "Teaching", "Other")
  - `syllabus`: `String` (Plaintext syllabus or syllabus overview)
  - `questionCount`: `Int` (Target question count, default: 20)
  - `customPromptNotes`: `String` (Additional instructions for AI question generator)
  - `autoGenerationEnabled` / `autoGenEnabled`: `Boolean` (Toggle for nightly automated question generation)
  - `autoGenTime`: `String` (IST trigger time in "HH:mm" format, default: "00:00")
  - `timezone`: `String` (Default: "Asia/Kolkata")
  - `testNumber`: `String` (Current/next test title counter, e.g., "Test 1")
  - `syllabusUrl`: `String` (Public/Download URL of the uploaded syllabus PDF)
  - `syllabusFileName`: `String` (Original filename of syllabus PDF)
  - `generationPrompt`: `String` (Custom prompt used for AI generation)
  - `imageUrl`: `String` (Base64 string or image URL for exam card)
  - `lastGeneratedDate`: `String` (IST date format "YYYY-MM-DD" of last run)
  - `lastGenerationStatus`: `String` ("running" | "success" | "failed")
  - `lastGenerationError`: `String` (Error message if generation failed)
  - `lastGenerationTime`: `Long` (Timestamp in milliseconds)
  - `generatingLockUntil`: `Long` (Lock expiry timestamp in milliseconds for idempotency)

### 1.2 `questions`
- **Purpose**: Global bank of questions used in mock tests, topic practice, and PYQ sets.
- **Document ID**: Auto-generated string ID.
- **Fields**:
  - `id`: `String` (Document ID)
  - `examId`: `String` (Foreign key referencing `exams.id`)
  - `questionText`: `String` (English question text)
  - `optionA`: `String` (English option A)
  - `optionB`: `String` (English option B)
  - `optionC`: `String` (English option C)
  - `optionD`: `String` (English option D)
  - `correctAnswer`: `String` ("A" | "B" | "C" | "D")
  - `explanation`: `String` (English solution explanation)
  - `topic`: `String` (Subject/topic tag, e.g. "Percentage", "Polity")
  - `isPyq`: `Boolean` (True if from Previous Year Question paper)
  - `pyqYear`: `Int` (Exam year, e.g. 2024; 0 if not PYQ)
  - `pyqPaper`: `String` (Shift/paper tag, e.g. "Prelims", "Tier 1", "Shift 2")
  - `questionTextHi`: `String` (Hindi translation of question text)
  - `optionAHi`: `String` (Hindi option A)
  - `optionBHi`: `String` (Hindi option B)
  - `optionCHi`: `String` (Hindi option C)
  - `optionDHi`: `String` (Hindi option D)
  - `explanationHi`: `String` (Hindi solution explanation)

### 1.3 `attempt_locks`
- **Purpose**: Anti-cheat single-submission lock per user per exam.
- **Document ID**: Deterministic `${userId}_${examId}`.
- **Fields**:
  - `userId`: `String` (Firebase Auth UID)
  - `examId`: `String` (Referenced exam ID)
  - `timestamp`: `Long` (Epoch millis when lock was acquired)
  - `source`: `String` ("submit" | "legacy_migration")

### 1.4 `attempts`
- **Purpose**: Completed mock test submissions, scores, and answer sheets.
- **Document ID**: Auto-generated string ID.
- **Fields**:
  - `id`: `String` (Document ID)
  - `userId`: `String` (Firebase Auth UID)
  - `displayName`: `String` (Student name at submission time)
  - `examId`: `String` (Referenced exam ID)
  - `examName`: `String` (Snapshot of exam name)
  - `category`: `String` (Exam category snapshot)
  - `score`: `Double` (Computed server-side: correct - wrong * NEGATIVE_MARK)
  - `total`: `Int` (Total questions submitted)
  - `correct`: `Int` (Number of correct answers)
  - `wrong`: `Int` (Number of incorrect answers)
  - `unattempted`: `Int` (Number of unattempted questions)
  - `timestamp`: `Long` (Epoch millis of submission)
  - `answers`: `Array<Map<String, Any>>` (Snapshot of answers evaluated by server):
    - `questionId`: `String`
    - `number`: `Int`
    - `questionText`: `String`
    - `selected`: `String` ("" | "A" | "B" | "C" | "D")
    - `selectedText`: `String`
    - `correct`: `String` ("A" | "B" | "C" | "D")
    - `correctText`: `String`
    - `explanation`: `String`
    - `isBookmarked`: `Boolean`
    - `topic`: `String`
    - `questionTextHi`: `String`
    - `selectedTextHi`: `String`
    - `correctTextHi`: `String`
    - `explanationHi`: `String`

### 1.5 `users`
- **Purpose**: User profile data, activity tracking, and device tokens.
- **Document ID**: Firebase Auth UID (`${userId}`).
- **Fields**:
  - `email`: `String`
  - `displayName`: `String`
  - `dob`: `String` (Date of birth)
  - `category`: `String` ("General" | "OBC" | "SC" | "ST")
  - `createdAt`: `Long`
  - `lastActive`: `Long` (Updated on app foregrounding & login)
  - `lastUpdated`: `Long`
  - `fcmToken`: `String` (FCM registration token)

#### Subcollections under `users/{userId}`:
1. `pinned_exams/{examId}`:
   - Document ID: `${examId}`
   - Fields:
     - `pinnedAt`: `Long`
2. `bookmarks/{questionId}`:
   - Document ID: Stable string `${examId}_q${number}_${textSnippet}` or `${questionId}`
   - Fields:
     - `questionId`: `String`
     - `examId`: `String`
     - `examName`: `String`
     - `questionNumber`: `Int`
     - `questionText`: `String`
     - `questionTextHi`: `String`
     - `optionA`: `String`
     - `optionB`: `String`
     - `optionC`: `String`
     - `optionD`: `String`
     - `optionAHi`: `String`
     - `optionBHi`: `String`
     - `optionCHi`: `String`
     - `optionDHi`: `String`
     - `correctAnswer`: `String`
     - `explanation`: `String`
     - `explanationHi`: `String`
     - `topic`: `String`
     - `isPyq`: `Boolean`
     - `pyqYear`: `Int`
     - `pyqPaper`: `String`
     - `bookmarkedAt`: `Long`

### 1.6 `leaderboard`
- **Purpose**: Per-exam top scores (one best score per student per exam).
- **Document ID**: Deterministic `${examId}_${userId}`.
- **Fields**:
  - `userId`: `String`
  - `examId`: `String`
  - `examName`: `String`
  - `category`: `String`
  - `displayName`: `String`
  - `score`: `Double`
  - `total`: `Int`
  - `timestamp`: `Long`

### 1.7 `overall_leaderboard`
- **Purpose**: All-time aggregated scores and rankings across all mock tests.
- **Document ID**: `${userId}`.
- **Fields**:
  - `userId`: `String`
  - `displayName`: `String`
  - `score` / `totalScore`: `Double`
  - `total`: `Int` (Sum of questions attempted)
  - `testsTaken`: `Int`
  - `totalCorrect`: `Int`
  - `accuracy`: `Int` (Percentage: `totalCorrect / total * 100`)
  - `timestamp`: `Long`

### 1.8 `admins`
- **Purpose**: Dynamic admin email whitelist (supplementing hardcoded admin list).
- **Document ID**: Lowercase email string (e.g. `user@example.com`).
- **Fields**:
  - `email`: `String` (Original casing email)

### 1.9 `app_content`
- **Purpose**: Static & dynamic page content managed by Admin.
- **Document ID**: `privacy` | `terms` | `contact`.
- **Fields**:
  - `title`: `String`
  - `body`: `String`
  - `updatedAt`: `Long`
  - `updatedBy`: `String`
  - `supportEmail`: `String` (for contact)
  - `phone`: `String`
  - `website`: `String`
  - `address`: `String`

### 1.10 `home_banners` & legacy `home_banner`
- **Purpose**: Promotional carousel banners for Home screen.
- **Document ID**: UUID or `active` (legacy singleton).
- **Fields**:
  - `id`: `String`
  - `imageUrl`: `String` (Image URL or base64)
  - `storagePath`: `String` (Storage object key, optional)
  - `order`: `Int` (Sort display index)
  - `uploadedAt`: `Long`
  - `uploadedBy`: `String` (Admin email)
  - `active`: `Boolean`

### 1.11 `notifications`
- **Purpose**: In-app broadcast notifications and push alert records.
- **Document ID**: Auto-generated string ID.
- **Fields**:
  - `id`: `String`
  - `title`: `String`
  - `message` / `body`: `String`
  - `sentAt`: `Long`
  - `sentBy`: `String`
  - `type`: `String` (e.g., "general", "new_exam")

### 1.12 `polls`
- **Purpose**: Community polls and feedback voting.
- **Document ID**: Auto-generated string ID.
- **Fields**:
  - `id`: `String`
  - `question`: `String`
  - `options`: `List<String>`
  - `createdAt`: `Long`
  - `endsAt`: `Long`
  - `active`: `Boolean`
  - `createdBy`: `String`
  - `voteCounts`: `Map<String, Long>` (e.g., `{"0": 12, "1": 5}`)

#### Subcollections under `polls/{pollId}`:
- `votes/{uid}`:
  - Document ID: `${uid}` (Enforces exactly 1 vote per user)
  - Fields:
    - `optionIndex`: `Int`
    - `votedAt`: `Long`

### 1.13 `feedback_messages`
- **Purpose**: Direct private feedback sent from student to Admin.
- **Document ID**: Auto-generated string ID.
- **Fields**:
  - `id`: `String`
  - `message`: `String`
  - `userId`: `String`
  - `userName`: `String`
  - `userEmail`: `String`
  - `timestamp`: `Long`
  - `read`: `Boolean`
  - `postId`: `String?` (Optional link to feedback post)
  - `postTitle`: `String?`

### 1.14 `feedback_posts`
- **Purpose**: Admin announcements and community discussion topics.
- **Document ID**: Auto-generated string ID.
- **Fields**:
  - `id`: `String`
  - `title`: `String`
  - `message`: `String`
  - `authorId`: `String`
  - `authorEmail`: `String`
  - `timestamp`: `Long`

#### Subcollections under `feedback_posts/{postId}`:
- `replies/{replyId}`:
  - Document ID: Auto-generated string ID
  - Fields:
    - `id`: `String`
    - `postId`: `String`
    - `uid`: `String`
    - `name`: `String`
    - `email`: `String`
    - `text`: `String`
    - `timestamp`: `Long`
    - `read`: `Boolean`

### 1.15 `generated_tests`
- **Purpose**: AI-generated tests waiting for approval or published live.
- **Document ID**: Auto-generated string ID.
- **Fields**:
  - `id`: `String`
  - `examId`: `String`
  - `examName`: `String`
  - `testNumber`: `String`
  - `title`: `String`
  - `generatedAt`: `Long`
  - `status`: `String` ("paused" | "live" | "rejected")
  - `questionCount`: `Int`
  - `syllabusUsed`: `String`
  - `promptUsed`: `String`
  - `questions`: `List<Map<String, Any>>` (Array of generated questions):
    - `questionText`: `String`
    - `optionA`: `String`
    - `optionB`: `String`
    - `optionC`: `String`
    - `optionD`: `String`
    - `correctAnswer`: `String` ("A" | "B" | "C" | "D")
    - `explanation`: `String`

### 1.16 `admin_analytics_exams`
- **Purpose**: Pre-aggregated exam engagement metrics for Admin dashboard.
- **Document ID**: `${examId}`.
- **Fields**:
  - `examId`: `String`
  - `examName`: `String`
  - `category`: `String`
  - `attemptCount`: `Long`
  - `uniqueUsers`: `Long`
  - `lastAttemptAt`: `Long`

### 1.17 `admin_analytics_questions`
- **Purpose**: Pre-aggregated question performance metrics (hardest/most failed questions).
- **Document ID**: `${examId}_${questionId}` (or SHA-256 hash).
- **Fields**:
  - `id`: `String`
  - `examId`: `String`
  - `examName`: `String`
  - `questionId`: `String`
  - `questionNumber`: `Int`
  - `questionText`: `String`
  - `topic`: `String`
  - `attempts`: `Long`
  - `correct`: `Long`
  - `wrong`: `Long`
  - `unattempted`: `Long`

### 1.18 `admin_analytics_exam_users`
- **Purpose**: Deduplication markers so a student is counted only once in `uniqueUsers`.
- **Document ID**: `${examId}_${userId}`.
- **Fields**:
  - `createdAt`: `Long`

### 1.19 `system_config`
- **Purpose**: Internal server-side config (e.g. `apiRotation` tracking index for Gemini keys).
- **Document ID**: `apiRotation`.
- **Fields**:
  - `lastUsedKeyIndex`: `Int`
  - `updatedAt`: `Long`

---

## 2. Firebase Storage Paths Inventory

1. `syllabi/{examId}_{timestamp}.pdf`:
   - Syllabus PDF files uploaded by Admin.
   - Max size: 20MB.
   - Content-Type: `application/pdf`.
   - Read: All signed-in users.
   - Write/Delete: Verified Admins only.
   - **Migration Target**: Supabase Storage bucket `eve-media` under path `syllabi/{examId}_{timestamp}.pdf`.

2. Home Banners:
   - Previously stored base64 in Firestore documents, with optional `storagePath`.
   - **Migration Target**: Supabase Storage bucket `eve-media` under path `banners/{bannerId}.jpg` / `.png`.

3. Profile Photos:
   - Handled completely on-device (`ProfilePhotoManager.kt` saves to internal storage `filesDir/profile_photo.jpg`), or loaded directly from Google profile photo URL via Coil. No Firebase Storage path is used for user avatars.

---

## 3. Existing Cloud Functions Inventory & Logic

### 3.1 `submitAttempt` (Callable HTTPS)
- **Role**: Secure, server-side score calculation and test submission.
- **Validation**:
  - Auth required (`request.auth.uid`).
  - `examId` non-empty, answers array non-empty and <= 300 items.
  - Answers sanitized: only `questionId`, `number`, and `selected` in `["A", "B", "C", "D"]` are trusted.
- **Anti-Cheat Lock**:
  - For standard mock tests (non-practice / non-PYQ): checks `attempt_locks/{uid}_{examId}`.
  - If locked, rejects with `already-exists` ("You have already completed this test.").
  - Admins bypass this lock for preview/testing.
- **Scoring**:
  - Fetches true answer keys from `questions/{questionId}` OR `generated_tests/{testId}.questions[index]`.
  - Compares `pick.selected == q.correctAnswer`.
  - Calculates `score = correct - wrong * NEGATIVE_MARK` (NEGATIVE_MARK = 0.0).
- **Writes**:
  - Sets lock in `attempt_locks/{uid}_{examId}`.
  - Creates document in `attempts`.
  - Triggers leaderboard & analytics updates (in Worker API, this is executed immediately in the same request transaction).
- **Returns**: `{ attemptId, score, total, correct, wrong, unattempted }`.

### 3.2 `updateLeaderboard` (Firestore Trigger on `attempts/{attemptId}`)
- **Logic**:
  - Reads `examId`, `userId`, `score`, `total`.
  - Transaction on `leaderboard/{examId}_{userId}`: updates only if new `score > existing.score`.
  - Transaction on `overall_leaderboard/{userId}`: increments `testsTaken`, adds `score`, increments `total` and `totalCorrect`, recalculates `accuracy`.

### 3.3 `updateAdminAnalytics` (Firestore Trigger on `attempts/{attemptId}`)
- **Logic**:
  - Exam Analytics: increments `attemptCount`, updates `lastAttemptAt`. Checks `admin_analytics_exam_users/{examId}_{userId}`; if new, increments `uniqueUsers`.
  - Question Analytics: for each answer item, increments `attempts`, and increments `correct`, `wrong`, or `unattempted`.

### 3.4 `broadcastNotification` (Firestore Trigger on `notifications/{notificationId}`)
- **Logic**:
  - Broadcasts FCM push message to topic `all_users` with channel `new_exam_channel_v2`.

### 3.5 `notifyNewExam` (Firestore Trigger on `exams/{examId}`)
- **Logic**:
  - Broadcasts FCM push message to topic `new_exams` notifying users of new exam availability.

### 3.6 `scheduledTestGeneration` (Cloud Scheduler: every 5 min IST)
- **Logic**:
  - Computes current IST date (`YYYY-MM-DD`) and time (`HH:mm`).
  - Scans `exams` where `autoGenerationEnabled == true` and `currentTime >= autoGenTime` and `lastGeneratedDate != todayDate`.
  - Acquires 10-minute lock (`generatingLockUntil = now + 600,000`).
  - Rotates through Gemini API keys; constructs system prompt with exam name, syllabus, and target question count.
  - Parses and validates JSON response (4 distinct options, valid answer A/B/C/D, explanations, no duplicates).
  - Inserts test into `generated_tests` with `status: "paused"` (pending admin approval).
  - Releases lock, sets `lastGeneratedDate = todayDate`, `lastGenerationStatus = "success"`, and increments `testNumber` (e.g. "Test 1" -> "Test 2").

### 3.7 `triggerAiTestGeneration` (Callable HTTPS)
- **Role**: On-demand test generation triggered by Admin from "Manage Exam" screen.
- **Logic**: Verifies admin status; runs question generator immediately; saves to `generated_tests` with `status: "paused"`; updates exam status.

### 3.8 `deleteUserAccount` (Callable HTTPS)
- **Role**: GDPR / privacy compliance: user account deletion.
- **Logic**:
  - Deletes `users/{uid}` and subcollections (`pinned_exams`, `bookmarks`).
  - Deletes user records in `attempts`, `attempt_locks`, `leaderboard`, `overall_leaderboard`, `feedback_messages`, and `polls/*/votes/{uid}`.
  - Deletes user from Firebase Auth.

---

## 4. Android App Repositories & ViewModel Methods Checklist

| Repository | Exposed Methods | Current Backend Target |
|---|---|---|
| **AdminRepository** | `isAdmin(email: String?): Boolean`<br>`getDynamicAdmins(): List<String>`<br>`addAdmin(email: String)`<br>`removeAdmin(email: String)` | `admins` collection + hardcoded check |
| **ExamRepository** | `getAttemptedExamIds(userId): Set<String>`<br>`hasAttemptLock(userId, examId): Boolean`<br>`getExams(): List<Exam>`<br>`getQuestions(examId): List<Question>`<br>`getMockQuestions(examId): List<Question>`<br>`getQuestionsForTopic(examId, topic): List<Question>`<br>`getPyqQuestions(examId, year, paper): List<Question>`<br>`getPyqSets(exam): List<PyqSet>`<br>`addExam(...)`<br>`updateExamImage(examId, imageUrl)`<br>`renameExam(examId, newName)`<br>`deleteExam(examId)`<br>`updateExamFullSettings(...)`<br>`uploadSyllabusPdf(examId, fileName, bytes)`<br>`removeSyllabusPdf(examId, syllabusUrl)`<br>`addQuestion(q)`<br>`addQuestions(questions: List<Question>)`<br>`updateQuestion(q)`<br>`deleteQuestion(questionId)`<br>`updateExamAiSettings(...)`<br>`getGeneratedTests(examId?): List<GeneratedTest>`<br>`getLiveGeneratedTests(examId): List<GeneratedTest>`<br>`updateGeneratedTestStatus(testId, status)`<br>`deleteGeneratedTest(testId)` | `exams`<br>`questions`<br>`attempt_locks`<br>`generated_tests`<br>Firebase Storage `syllabi/` |
| **HistoryRepository** | `saveAttempt(examId, examName, category, displayName, items)`<br>`hasAttempted(userId, examId): Boolean`<br>`getAttempts(userId): List<TestAttempt>` | Firebase Function `submitAttempt`<br>`attempt_locks`<br>`attempts` |
| **LeaderboardRepository** | `getTopScorers(examId, limit): List<LeaderboardEntry>`<br>`getUserRank(examId, userId): RankInfo?` | `leaderboard`<br>`overall_leaderboard` |
| **AppContentRepository** | `getContent(type: String): AppContent`<br>`saveContent(type: String, content: AppContent)`<br>`getDefaultContent(type: String): AppContent` | `app_content` (`privacy`, `terms`, `contact`) |
| **AdminAnalyticsRepository** | `getExamAnalytics(): List<ExamAnalytics>`<br>`getQuestionAnalytics(examId?): List<QuestionAnalytics>` | `admin_analytics_exams`<br>`admin_analytics_questions` |
| **UserStatsRepository** | `getTotalUserCount(): Long`<br>`getOnlineUserCount(): Long` | `users` count & `lastActive` aggregation |
| **PollRepository** | `getPolls(): List<Poll>`<br>`getActivePoll(): Poll?`<br>`createPoll(question, options, endsAt, active, createdBy): String`<br>`updatePollStatus(pollId, active)`<br>`deletePoll(pollId)`<br>`getUserVote(pollId, uid): Int?`<br>`submitVote(pollId, uid, optionIndex)` | `polls`<br>`polls/{pollId}/votes/{uid}` |
| **FeedbackRepository** | `sendFeedback(userId, userName, userEmail, message, postId?, postTitle?)`<br>`getFeedbackMessages(): List<FeedbackMessage>`<br>`markAsRead(id)`<br>`deleteFeedbackMessage(id)`<br>`createFeedbackPost(title, message, authorId, authorEmail)`<br>`getFeedbackPosts(): List<FeedbackPost>`<br>`observeFeedbackPosts(): Flow<List<FeedbackPost>>`<br>`deleteFeedbackPost(postId)`<br>`submitPostReply(postId, uid, name, email, text)`<br>`getRepliesForPost(postId): List<FeedbackPostReply>`<br>`markReplyAsRead(postId, replyId)`<br>`deleteSingleReply(postId, replyId)` | `feedback_messages`<br>`feedback_posts`<br>`feedback_posts/{postId}/replies` |
| **PinnedExamsRepository** | `observePinnedExamIds(userId): Flow<Set<String>>`<br>`pinExam(userId, examId)`<br>`unpinExam(userId, examId)`<br>`togglePin(userId, examId, currentlyPinned)` | `users/{userId}/pinned_exams/{examId}` |
| **HomeBannerRepository** | `observeBanners(): Flow<List<HomeBanner>>`<br>`getBanners(): List<HomeBanner>`<br>`uploadBanner(context, uri, userEmail)`<br>`deleteBanner(bannerId)`<br>`reorderBanner(bannerId, moveUp)` | `home_banners`<br>`home_banner/active` |
| **BookmarkRepository** | `getStableId(question, questionNumber)`<br>`observeBookmarkIds(userId): Flow<Set<String>>`<br>`observeBookmarkedQuestions(userId): Flow<List<BookmarkedQuestion>>`<br>`bookmarkQuestion(userId, question, examName, questionNumber)`<br>`unbookmarkQuestion(userId, questionId)`<br>`toggleBookmark(userId, question, examName, questionNumber, currentlyBookmarked)` | `users/{userId}/bookmarks/{questionId}` |

---

## 5. Direct Callers Outside Repositories Checklist

- **`SendNotificationActivity.kt`**:
  - Listens to / fetches sent broadcasts from `notifications` (ordered by `sentAt DESC`).
  - Sends a broadcast by creating a document in `notifications`.
  - Multi-select bulk delete and single delete from `notifications`.
- **`NotificationsActivity.kt`**:
  - Listens to / fetches notifications from `notifications`.
- **`MainActivity.kt`**:
  - Listens to latest document in `notifications` to display red dot and trigger animation.
  - Updates `lastActive` in `users/{uid}`.
- **`LoginActivity.kt`**:
  - Writes `users/{uid}` with `email`, `displayName`, `lastActive`, `createdAt`.
- **`ProfileActivity.kt`**:
  - Reads `users/{uid}` for `displayName`, `dob`, `category`.
  - Updates `users/{uid}` with edited fields.
  - Calls `deleteUserAccount` Cloud Function.
- **`ManageExamsActivity.kt`**:
  - Calls `triggerAiTestGeneration` Cloud Function.
- **`EveMessagingService.kt`**:
  - Updates `fcmToken` in `users/{uid}`.

---

## 6. Security Matrix (`firestore.rules` & `storage.rules`)

| Resource | Operation | Allowed Roles | Restrictions / Conditions |
|---|---|---|---|
| **`exams`** | Read | Signed-in users | Any authenticated student |
| **`exams`** | Write / Delete | Admin only | Hardcoded email OR in `admins` table |
| **`questions`** | Read | Signed-in users | All questions for exams/practice/PYQ |
| **`questions`** | Write / Delete | Admin only | Admin only |
| **`attempts`** | Read | Owner OR Admin | `resource.data.userId == request.auth.uid \|\| isAdmin()` |
| **`attempts`** | Create / Edit / Delete | Server-side only (Client: BLOCKED) | Only callable function `submitAttempt` calculates scores & writes |
| **`attempt_locks`** | Read | Owner OR Admin | Student can only check own lock |
| **`attempt_locks`** | Write | Server-side only (Client: BLOCKED) | Locked deterministically upon submission |
| **`users`** | Read | Owner OR Admin | Admin needs user count/metrics |
| **`users`** | Write | Owner only | `request.auth.uid == userId` |
| **`users/*/pinned_exams`** | Read / Write | Owner only | `request.auth.uid == userId` |
| **`users/*/bookmarks`** | Read / Write | Owner only | `request.auth.uid == userId` |
| **`leaderboard`** | Read | Signed-in users | Read-only |
| **`leaderboard`** | Write | Server-side only | Updates strictly from validated score submissions |
| **`overall_leaderboard`**| Read | Signed-in users | Read-only |
| **`overall_leaderboard`**| Write | Server-side only | Aggregated on validated attempt submissions |
| **`admins`** | Read | Signed-in users | Can check admin status |
| **`admins`** | Write | Admin only | Admin only |
| **`app_content`** | Read | Signed-in users | Read-only |
| **`app_content`** | Write | Admin only | Admin only |
| **`home_banners`** | Read | Signed-in users | Read-only |
| **`home_banners`** | Write | Admin only | Admin only |
| **`notifications`** | Read | Signed-in users | Read-only |
| **`notifications`** | Create / Delete | Admin only | Sent notifications immutable (`update: false`) |
| **`polls`** | Read | Signed-in users | Read-only |
| **`polls`** | Create / Delete | Admin only | Admin only |
| **`polls`** | Update | Admin OR Student | Students may only increment option counters |
| **`polls/*/votes`** | Create | Owner only | 1 vote per user; update/delete blocked |
| **`feedback_messages`** | Create | Owner only | `request.resource.data.userId == request.auth.uid` |
| **`feedback_messages`** | Read / Edit / Delete| Admin only | Private to Admin |
| **`feedback_posts`** | Read | Signed-in users | Read-only |
| **`feedback_posts`** | Write | Admin only | Admin only |
| **`feedback_posts/*/replies`** | Read | Owner OR Admin | Student can view own; Admin views all |
| **`feedback_posts/*/replies`** | Create | Owner only | `request.resource.data.uid == request.auth.uid` |
| **`feedback_posts/*/replies`** | Edit / Delete | Admin only | Admin only |
| **`generated_tests`** | Read | Student: "live" / "published" only; Admin: all | Draft/paused tests hidden from students |
| **`generated_tests`** | Write | Admin only | Status toggles, question edits |
| **`admin_analytics_*`** | Read | Admin only | Admin dashboard only |
| **`admin_analytics_*`** | Write | Server-side only | Computed by triggers |
| **`storage: syllabi/*`** | Read / Download | Signed-in users | PDF format |
| **`storage: syllabi/*`** | Upload / Delete | Admin only | PDF format only, size < 20MB |
| **`storage: banners/*`** | Read / Download | Signed-in users | Public images |
| **`storage: banners/*`** | Upload / Delete | Admin only | Admin only |

---

## 7. Migration Mapping Summary to Cloudflare Worker + D1 + Supabase Storage

1. **Database (Cloudflare D1)**:
   - Relational tables will map all 19 Firestore entities, converting nested subcollections (`pinned_exams`, `bookmarks`, `poll_votes`, `post_replies`) into relational child tables with foreign keys and cascade delete rules where appropriate.
   - Array fields (`options`, `answers`, `questions`) will be modeled cleanly: `options` in polls as JSON or normalized, `attempt_answers` as a dedicated answers table, and `generated_test_questions` as a dedicated questions table.
   - Indexes mapped from Firestore query patterns:
     - `questions(exam_id, is_pyq, pyq_year)`
     - `attempts(user_id, timestamp)`
     - `leaderboard(exam_id, score DESC)`
     - `notifications(sent_at DESC)`
     - `home_banners(order_index ASC)`
     - `exams(auto_gen_enabled, auto_gen_time)`

2. **File Storage (Supabase Storage bucket `eve-media`)**:
   - `syllabi/` PDFs and `banners/` images uploaded by Worker using `SUPABASE_SERVICE_ROLE_KEY`.
   - Client downloads/views files using Supabase Public URLs (`https://msfihtyllpodkfgxxxdd.supabase.co/storage/v1/object/public/eve-media/...`).

3. **Authentication & Authorization**:
   - Google Sign-In on Android generates Firebase ID Token.
   - All HTTP requests to Cloudflare Worker carry `Authorization: Bearer <token>`.
   - Worker verifies JWT using Google Firebase JWKS public keys.
   - Worker extracts `uid` and `email`; admin check tests email against hardcoded list and `admins` table in D1.

4. **Background AI Generation**:
   - Cloudflare Cron Trigger (every 5 minutes) executes `scheduledTestGeneration` logic.
   - Gemini API calls with key rotation via `GEMINI_API_KEY`.
   - Generated tests inserted into D1 `generated_tests` as `"paused"`.
