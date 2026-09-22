# Eve — Free Mock Test App

Kotlin + MVVM + Firebase (Firestore + Google Sign-in). No custom backend.

## 1. Firebase project banao
1. https://console.firebase.google.com → **Add project** → naam "Eve" (ya kuch bhi).
2. Project ke andar → **Add app → Android**.
   - Package name: `com.eve.app` (exactly yehi likhna, code me yehi set hai)
   - SHA-1: `49:24:60:51:F9:0D:46:5F:B1:5E:DE:4F:EF:1E:77:00:8B:60:76:93`
     (yeh is repo ke fixed debug keystore ka SHA-1 hai — GitHub Actions isi keystore se APK banayega, isliye SHA-1 hamesha same rahega aur Google Sign-in kabhi nahi tootega)
3. `google-services.json` download karo → repo me **`app/google-services.json`** path par daal do (root me nahi, `app/` folder ke andar).
4. Firebase console me **Authentication → Sign-in method → Google → Enable**.
5. **Firestore Database → Create database** (production mode) → collections apne aap ban jayengi jab data add hoga: `exams`, `questions`, `admins`, aur `attempts` (Phase 10 — students ki test history).
6. Firestore **Rules** tab me yeh laga do (MVP ke liye — logged-in user read kar sake, sirf admins `exams`/`questions`/`admins` write karein; har student sirf apni khud ki `attempts` likh/padh sake):
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    function isAdmin() {
      return request.auth != null && (
        request.auth.token.email in [
          "pronlike9@gmail.com",
          "own.keni@gmail.com",
          "anyqueairdrop@gmail.com",
          "ghatisarkar56@gmail.com"
        ] ||
        exists(/databases/$(database)/documents/admins/$(request.auth.token.email))
      );
    }
    match /attempts/{attemptId} {
      allow read: if request.auth != null && resource.data.userId == request.auth.uid;
      allow create: if request.auth != null && request.resource.data.userId == request.auth.uid;
      allow update, delete: if false;
    }
    match /{document=**} {
      allow read: if request.auth != null;
      allow write: if isAdmin();
    }
  }
}
```
   Yeh 4 emails already `Constants.kt` me bhi hardcoded hain, dono jagah match hone chahiye. Agar app ke andar se koi naya admin add karo, wo `admins` collection me save hota hai aur is rule ka `exists(...)` check use hoke automatically usko bhi write access mil jata hai — Firestore rules dobara publish karne ki zaroorat nahi padti jab tak naye hardcoded emails na jodne ho.
   Firestore rules me matching blocks OR hoke evaluate hote hain (jo bhi block allow de de, wahi final hota hai), isliye `attempts` ka specific rule upar ho ya neeche — position se farak nahi padta, bas dono block hone chahiye.

## 2. GitHub par push karo
```
git init
git add .
git commit -m "Eve mock test app"
git branch -M main
git remote add origin <tumhara-repo-url>
git push -u origin main
```
Push hote hi **Actions** tab me build automatically chalega (`.github/workflows/build.yml`). 2-3 min me green tick aayega → us run ko open karo → neeche **Artifacts → Eve-debug-apk** se APK download karo.

Agar `google-services.json` daalna bhool gaye to build red ho jayega saaf error ke saath — file daal ke phir se push karna, ya Actions tab se "Re-run jobs".

## 3. Phone par install
APK download karke phone me transfer karo, install karo (Unknown sources allow karna padega ek baar).

## Project structure
- `data/model` — Exam, Question, AnswerItem (Firestore data classes)
- `data/repository/ExamRepository.kt` — saari Firestore calls (suspend functions)
- `ui/login` — Google Sign-in
- `ui/home` — exam list (RecyclerView) + hidden Admin button
- `ui/test` — ViewPager2 exam screen, timer, auto-submit
- `ui/result` — score + answer key
- `ui/admin` — sirf `Constants.ADMIN_EMAILS` (hardcoded) ya `admins` Firestore collection (app se add kiye gaye) wale email ko dikhta hai; naya exam + question upload/edit/delete, aur naye admins add/remove — sab yahin se

## Admin management (Phase 4)
- `Constants.kt` me diye 4 email hamesha admin rahenge, chahe internet na ho
- Admin Dashboard ke "5. Admins manage karo" section se koi bhi existing admin naya email add kar sakta hai — wo turant Firestore `admins` collection me save hota hai aur agli baar us email se login hote hi Admin Dashboard unlock ho jata hai, bina app update kiye
- "Remove" button se sirf Firestore wale (dynamically add kiye) admins hi hataye ja sakte hain — Constants.kt wale 4 hardcoded emails yahan se nahi hatenge (unhe hatane ke liye code edit karke rebuild karna padega)

## Offline support + Dark/Light toggle (Phase 7)
- Firestore ka offline persistent cache `EveApplication.kt` me explicitly enable kiya gaya hai (`FirebaseFirestoreSettings` + `PersistentCacheSettings`). Matlab ek baar exams/questions load ho jaane ke baad, weak network ya no-internet me bhi wahi (cached) data turant dikhta rahega — Home screen ke exams aur Test screen ke questions dono.
- Jab device offline ho, Home aur Test dono screens ke top par ek chhota amber banner dikhta hai: "No internet — cached data dikha rahe hain". Yeh `util/NetworkUtil.kt` (ConnectivityManager ka live Flow) se chalta hai.
- Agar kisi exam/question ka data pehle kabhi cache hi nahi hua aur internet bhi nahi hai, to error state me seedha "No internet connection" dikhega (generic Firestore error ki jagah) aur ek **Retry** button milega jo dobara try karta hai.
- Top-right corner me ek hi icon-button se Dark/Light mode toggle hota hai (Login aur Home screen dono par) — light mode me ☀️ sun icon dikhta hai (tap karke dark karo), dark mode me 🌙 moon icon dikhta hai (tap karke light karo). Choice `util/ThemeManager.kt` SharedPreferences me save hoti hai, isliye app dobara khulne par bhi wahi mode yaad rehta hai. Pehli baar (kabhi toggle na kiya ho) system ka apna dark/light setting follow hoti hai.

## Play Store ready — signed release build (Phase 8)
Ab tak jo bhi APK banta tha wo **debug** keystore se sign hota tha (testing ke liye theek hai, par Play Store isko accept nahi karta). Phase 8 me ek **naya, real release keystore** banate hain jo sirf tumhare paas rahega — GitHub ko sirf iska encrypted copy (Secret ke roop me) milta hai, repo me kabhi commit nahi hota.

### 1. Release keystore banao (sirf ek baar, apne Termux/computer par)
Termux me pehle Java install karo (agar nahi hai):
```
pkg install openjdk-17 -y
```
Phir keystore banao:
```
keytool -genkeypair -v -keystore eve-release.keystore -alias eve-release -keyalg RSA -keysize 2048 -validity 10000
```
Yeh kuch sawaal poochega (naam, organization, city, etc.) — kuch bhi bhar sakte ho, matter nahi karta. **Do password maangega**:
- Keystore password
- Key password (yahi rakh sakte ho jo keystore password hai — enter dabakar same use kar sakte ho)

**⚠️ Bahut important**: `eve-release.keystore` file aur dono passwords kahin surakshit save kar lo (jaise password manager me). Yeh khoya to Play Store par app update karna future me impossible ho jayega — Google naya keystore accept nahi karta ek baar publish hone ke baad.

### 2. Keystore ko base64 me convert karo
```
base64 -w 0 eve-release.keystore > eve-release.keystore.b64
cat eve-release.keystore.b64
```
Pura output (ek lambi single line) copy kar lo.

### 3. GitHub par 4 Secrets add karo
Repo → **Settings → Secrets and variables → Actions → New repository secret** — yeh 4 banao:

| Secret name | Value |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | Step 2 wala pura base64 output |
| `RELEASE_STORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | `eve-release` (ya jo alias diya tha) |
| `RELEASE_KEY_PASSWORD` | Key password |

### 4. Release build trigger karo
Do tareeke hain:
- **Git tag push karo** (recommended — automatically GitHub Release bhi ban jaata hai APK+AAB attached):
  ```
  git tag v1.0.0
  git push origin v1.0.0
  ```
- **Ya Actions tab se manually**: `Build Signed Release (APK + AAB)` workflow kholo → **Run workflow** button → version name type karo (jaise `1.0.0`) → Run.

Build complete hote hi:
- `Eve-release-apk` artifact — signed APK (phone par direct install ke liye)
- `Eve-release-aab` artifact — signed `.aab` (yehi file **Play Console** par upload hoti hai, APK nahi)
- Tag wale trigger me ek **GitHub Release** bhi ban jaata hai dono files ke saath

### Versioning
`versionCode` GitHub Actions run number se automatic aata hai, `versionName` tag (`v1.2.0` → `1.2.0`) ya manual input se. Local build me (tag/secrets ke bina) dono ki default values `app/build.gradle.kts` me hi set hain, kuch extra karne ki zaroorat nahi.

Naya version release karna ho to bas naya tag badha ke push karo (`v1.0.1`, `v1.1.0`, etc.) — code me kahin version number manually change karne ki zaroorat nahi.

## Test History (Phase 10)
- Har test submit hote hi (Home screen se attempt kiya ho tabhi, review/preview se nahi) `TestViewModel.saveAttempt()` uska poora record — exam, category, score, correct/wrong/unattempted, aur poori answer key — Firestore `attempts` collection me save kar deta hai, current logged-in user ki `uid` ke saath.
- Yeh write suspend function nahi hai (fire-and-forget): Firestore ka offline cache turant local write kar leta hai, isliye Result screen par navigate karne ke liye TestActivity turant `finish()` ho jaaye tab bhi save lost nahi hota.
- Home screen par naya **Test History** button — `HistoryActivity` me sirf usi user ki (apni `uid` wali) attempts naye-se-purane order me dikhti hain (`data/repository/HistoryRepository.kt`).
- Kisi bhi history row par tap karo to wahi Result screen (answer key + filters + explanation) dubara khulti hai, ab **read-only review mode** me — header me exam ka naam aur attempt ki date dikhti hai, aur "Close" button seedha History list par wapas le jaata hai (Home par nahi).
- Offline support yahan bhi kaam karta hai: pehle load ho chuki history cache se turant dikh jaati hai, No-internet banner aur Retry button Home/Test screens jaisa hi hai.

## Hindi/English toggle for questions (Phase 11)
- `data/model/Question.kt` me ab har question ke sath optional Hindi fields bhi hote hain (`questionTextHi`, `optionAHi..DHi`, `explanationHi`) — sab default blank, Firestore me bhi automatically map ho jaate hain kyunki field names Kotlin property names se exactly match karte hain.
- Admin Dashboard ke question form me ab ek collapsible **"+ Hindi translation add karo (optional)"** section hai — bharna zaroori nahi. Jis question ka Hindi bhara ho, edit karte waqt section apne aap khul jaata hai.
- Test screen (header, timer ke bagal) aur Result/Review screen (Score card, top-right) dono par ek chhota **हिं / EN** button hai — tap karte hi turant switch ho jaata hai, koi screen reload/reset nahi hota (`util/LanguageManager.kt` SharedPreferences me choice save karta hai, poore app me shared rehti hai).
- Fallback rule: agar kisi question/option/explanation ka Hindi translation nahi bhara, "हिं" select hone par bhi wahi field English me hi dikhta hai — kabhi khali nahi dikhta.
- Test History me bhi yeh translation preserved rehta hai — `AnswerItem` submit ke time dono languages (raw English + raw Hindi) save karta hai, isliye purane attempt ka review bhi language toggle ke saath kaam karta hai.

## Push Notifications (Phase 12)
Do tarah ke notifications hain, dono Home screen par apne aap set ho jaate hain (koi extra setup nahi chahiye, sirf Android 13+ par user ko ek baar permission popup allow karna hoga):

**1. Naya Exam Alert (FCM topic push)**
- Har device app khulte hi `"new_exams"` FCM topic subscribe kar leta hai (`MainActivity.setupPushNotifications()`).
- `service/EveMessagingService.kt` is topic par aane wale message ko receive karke notification dikhata hai (channel: "Naye Exam Alerts").
- **Important:** client se seedha doosre devices ko push bhejna possible nahi hai — asli automatic alert (admin ke naya exam add karte hi) ke liye ek chhota Cloud Function chahiye, jo `functions/index.js` me diya hua hai (optional bonus, deploy steps us file ke comments me hain, Blaze plan chahiye). Function deploy kiye bina bhi app crash nahi hogi — bas automatic alert nahi aayega; test karne ke liye Firebase Console → Cloud Messaging se seedha `new_exams` topic par ek test notification bhej sakte ho.

**2. Daily Practice Reminder (on-device, koi server nahi chahiye)**
- `util/ReminderScheduler.kt` WorkManager ke through roz shaam 7 baje ek local reminder notification schedule karta hai (channel: "Daily Practice Reminder"), jab tak app kam se kam ek baar khuli ho — WorkManager khud reboot ke baad bhi schedule yaad rakhta hai.
- Home screen ke header me naya **bell icon** (theme toggle ke bagal) is reminder ko ON/OFF karta hai — choice SharedPreferences me save hoti hai (default ON).

## Profile screen + Home header redesign
- Home screen ka header ab simple hai: **left** me profile photo icon + "Hi, {naam}" greeting, **right** me sirf notification bell (daily reminder toggle, Phase 12 wala).
- Theme toggle (dark/light) aur Logout Home screen se hata ke naye **Profile screen** (`ui/profile/ProfileActivity.kt`) me daal diye hain — Home ke profile icon par tap karke khulta hai. Profile screen ke header (top bar) ke right side me dark/light toggle hai, aur sabse niche ek "Logout" button.
- Profile photo: user gallery se apni photo pick kar sakta hai (camera icon overlay ya photo par hi tap karke) — `util/ProfilePhotoManager.kt` isko center-crop karke device ki apni internal storage me save karta hai (koi Firebase Storage/backend nahi chahiye). Koi custom photo na ho to Google account ki photo (agar hai) dikhti hai, warna ek default placeholder icon.
- Profile screen par user ka naam, email, aur (agar admin hai) ek "Admin" badge bhi dikhta hai.

## Firestore Security Rules (Phase 13)
Abhi tak Firestore "test mode" me tha (koi bhi console se seedha connect karke sab data padh/badal/delete kar sakta tha). Ab `firestore.rules` file me proper rules hain — **par yeh apply tabhi hongi jab deploy karoge** (naya file sirf repo me hone se kuch nahi hota, Firebase project ko batana padega):

### Deploy karne ke steps (Termux/laptop, ek baar)
```bash
npm install -g firebase-tools     # agar pehle se nahi hai
firebase login
cd eve-mock-test-app              # repo ka root, jahan firebase.json hai
firebase deploy --only firestore:rules
```
`firebase login` pehli baar browser khol ke Google account se login maangega — jo bhi Firebase project ka owner/editor hai wahi account use karo.

### Rules kya karti hain
| Collection | Read | Write |
|---|---|---|
| `exams`, `questions` | Koi bhi logged-in user | Sirf admin |
| `admins` | Koi bhi logged-in user (sirf emails hain, secret nahi) | Sirf admin |
| `attempts` | User sirf apni khud ki attempts | User sirf apni banayi (create), edit/delete kabhi nahi (result tamper-proof) |
| `users` (FCM token, Phase 12) | Sirf apna khud ka document | Sirf apna khud ka document |

**Naya admin email hardcode karna ho** to `Constants.kt` ke `ADMIN_EMAILS` ke saath-saath `firestore.rules` ke `isHardcodedAdmin()` list bhi update karo — dono jagah sync rehni chahiye, warna naya admin Firestore me write nahi kar payega (rules deny kar degi) chahe app UI me button dikh jaye.

**Test kaise karo**: deploy hone ke baad Firebase Console → Firestore → koi bhi document manually edit karne ki koshish karo (bina app khole) — ab "Missing or insufficient permissions" error aana chahiye. App normal chalti rahegi, kyunki wahan se requests hamesha logged-in user ke through hi jaati hain.

## Crashlytics + Analytics (Phase 15) + Night mode icon update

**Crash reports & analytics** — pehle koi crash ya usage data track nahi hoti thi. Ab Firebase Console me do naye sections active honge:
- **Crashlytics** — koi bhi crash ho to Console → Crashlytics me stack trace + kis user (UID) ke saath hua + admin tha ya student, sab dikhega.
- **Analytics** → **Events** — `exam_start` aur `exam_submit` events se pata chalega kaunsa exam sabse zyada attempt ho raha hai aur kahan users beech me hi chhod dete hain (start hua par submit nahi hua = drop-off).

**Setup ek baar zaroori hai**: Firebase Console kholo → apna project → left sidebar me **Crashlytics** par jao → "Enable Crashlytics" par click karo (pehli baar ek dummy crash bhejna maang sakta hai, wahi neeche wala test crash trick use karo). Analytics already-on hota hai naye Firebase projects me by default.

**Test karne ka tarika** (sirf debug build me kaam karega): App khol ke Profile → About screen kholo, version text (jaise "Version 1.0") ko **long-press** karo — app crash ho jayegi, aur 2-3 minute me Firebase Console → Crashlytics me woh report dikhne lagegi. Release build (Play Store wali) me yeh long-press kuch nahi karta, safe hai.

**Night mode icon** — Profile/Login screen ka dark-mode toggle icon (moon) ab Telegram-style crescent + sparkles wala hai (pehle ek plain crescent tha).

## Bug fix: build error + bell icon behavior (after Phase 14)
Do cheezein fix ki hain:

1. **Build error** — `activity_about.xml` me `app:tint` use ho raha tha par us file me `xmlns:app` namespace declare hi nahi tha (isiliye GitHub Actions build fail ho raha tha, "AttributePrefixUnbound" error). Ab fix hai.
2. **Bell icon** — pehle Home screen ka bell icon sirf daily-reminder ON/OFF toggle tha, tap karne par koi notification list nahi dikhti thi. Ab:
   - Bell tap karne par ek naya **Notifications screen** (`ui/notifications/NotificationsActivity.kt`) khulta hai jisme asal me aayi hui notifications (naya exam alert + daily reminder) ki list dikhti hai — naya sabse upar, saath me "kitne time pehle" bhi. Yeh list device par hi save hoti hai (`util/NotificationStore.kt`), koi Firestore collection nahi chahiye.
   - Notification aane par bell par ek chhota **red dot** dikhta hai (unread indicator); Notifications screen khulte hi dot hat jaata hai.
   - Daily reminder ka ON/OFF ab **Profile screen** me shift ho gaya hai (naya switch row "Daily Practice Reminder", About button ke upar).

## Privacy Policy + Terms (Phase 14)
Google Sign-in use karne wale kisi bhi app ko Play Store par ek Privacy Policy URL dena mandatory hai. Do cheezein add ki hain:

1. **Hosted HTML pages** — `docs/privacy-policy.html` aur `docs/terms.html`. Yeh GitHub Pages se free me host honge (repo me `docs/` folder hai isiliye).
2. **In-app About screen** — `ui/about/AboutActivity.kt`, Profile screen se "About" button dabane par khulta hai. Isme app version, "Privacy Policy", "Terms of Service" aur "Contact Us" rows hain — Privacy Policy/Terms tap karne par browser me hosted page khulta hai, Contact Us email app kholta hai.

### GitHub Pages enable karna (ek baar, zaroori)
Bina is step ke Privacy Policy link kaam nahi karega:
1. GitHub par apne repo (`eve-mock-test-app`) ki **Settings** kholo.
2. Left sidebar me **Pages** par jao.
3. **Source** me "Deploy from a branch" select karo, **Branch**: `main`, folder: `/docs`, phir **Save**.
4. 1-2 minute me page live ho jayega: `https://<tumhara-github-username>.github.io/eve-mock-test-app/privacy-policy.html`

`Constants.kt` me `PRIVACY_POLICY_URL` aur `TERMS_URL` pehle se `vishu762701.github.io/eve-mock-test-app/...` set hain. Agar tumhara GitHub username ya repo ka naam isse alag hai, to yeh do lines update kar dena.

**Play Store Console** me app submit karte waqt "App content" → "Privacy Policy" section me yahi hosted URL daalna hoga.

## Leaderboard / Rank (Phase 16)
Har exam submit karne ke baad ab ek **"View Leaderboard"** button dikhta hai (Result screen ke Answer Key ke neeche) jo us exam ke top scorers aur apna rank/percentile dikhata hai — Test History se purana attempt review karne par bhi yeh button dikhta hai.

**Kaam kaise karta hai:**
- Naya Firestore collection: `leaderboard`, doc ID = `"{examId}_{userId}"` — har user ka ek exam me sirf ek (BEST score wala) entry.
- Client seedha is collection me kabhi nahi likhta (koi bhi apna score khud badha na sake, isliye) — `functions/index.js` ka naya `updateLeaderboard` Cloud Function jab bhi koi `attempts` document create hota hai (test submit) tab automatically best-score entry upsert kar deta hai. Yeh wahi optional Cloud Function setup hai jo Phase 12 (push notification) me pehle se use ho raha hai — **Blaze plan** chahiye deploy karne ke liye (`firebase deploy --only functions`); bina deploy kiye baaki app crash nahi hogi, bas Leaderboard screen par "Abhi koi scorer nahi hai" dikhega.
- `firestore.rules` me `leaderboard` collection: koi bhi logged-in user **read** kar sakta hai, **write** sirf Cloud Function (Admin SDK) se hoti hai — client write hamesha reject hogi.
- Apna rank nikalne ke liye poori list download nahi karte — Firestore ki `count()` aggregation query se seedha server par "mujhse zyada score kitno ka hai" count ho jaata hai (`LeaderboardRepository.getUserRank`), fast hai chahe hazaaron attempts ho chuke hon.
- `firestore.indexes.json` me ek naya composite index (`examId` + `score`) add hua hai — yeh bhi `firebase deploy --only firestore:indexes` se deploy karna padega (Phase 13 ke rules deploy jaisa hi step), warna Leaderboard/rank queries "index required" error dengi.

**Setup ek baar (rules + function dono deploy karne ke liye):**
```bash
cd eve-mock-test-app
firebase deploy --only firestore:rules,firestore:indexes,functions
```

## Performance Analytics (Phase 17)
Home screen par **Test History** ke bagal ek naya **Performance** button hai — Test History ke
history se aage ka "insights" version: score trend graph, topic-wise accuracy, aur weak areas.
Koi naya Firestore collection nahi chahiye, sab kuch `attempts` collection (Phase 10) se hi
client-side compute hota hai, isliye koi extra `firebase deploy` bhi nahi karna padta.

**Kaam kaise karta hai:**
- `data/model/Question.kt` me ek naya optional `topic` field hai (jaise "Percentage", "Polity",
  "Modern History"). Admin Dashboard ke question form me "Question text" ke turant neeche ek
  naya "Topic (optional)" field hai — bharna zaroori nahi, khali chhoda to wo question
  Performance screen par **"General"** bucket me count hota hai.
- Test submit hote hi har answer ke saath uske question ka topic bhi save ho jaata hai
  (`AnswerItem.topic`) — isliye purane attempts kabhi galat nahi honge chahe baad me admin
  us question ka topic edit/delete kar de.
- `ui/performance/PerformanceViewModel.kt` current user ke saare Test History attempts load
  karke 3 cheezein nikalta hai:
  1. **Overall stats** — total attempts + overall accuracy %
  2. **Score Trend** — last 15 attempts ki accuracy % ka line chart, purane se naye order me
     (custom `ScoreTrendChartView.kt` — Canvas se khud draw kiya hai, koi naya charting library
     add nahi ki gayi taaki build simple rahe)
  3. **Topic-wise Accuracy** — saare attempts ke answers ko topic ke hisaab se group karke
     har topic ki accuracy %, progress bar ke saath (green ≥70%, amber 40-69%, red <40%)
- **Weak Areas** card sabse kam accuracy wale (aur kam se kam 2 questions attempt kiye hue)
  top 3 topics highlight karta hai — red card, taaki student ko turant pata chale kahan focus
  karna hai.
- Purane attempts jinme topic save hi nahi hua (Phase 17 se pehle ke, ya jahan admin ne topic
  field khali chhoda tha) "General" bucket me chale jaate hain — koi data lost nahi hota,
  bas unattributed rehta hai.
- Har cheez client-side, ek hi Firestore read (`attempts` collection, jo already cache/offline
  ho chuki hoti hai Test History dekhne se) se compute hoti hai — koi naya composite index ya
  security rule change nahi chahiye.

## Topic-wise Practice (Phase 18)
Home screen par **Practice** button `PracticeActivity` kholta hai. Exam select karo, phir
us exam ke topic-tagged questions chips me dikhte hain (jaise "Percentage • 24 questions").
Chip tap karte hi Test screen **practice mode** me khulti hai: us topic ke max 10 random
questions, 1 minute/question. Submit ke baad wahi Result + History flow chalta hai, attempt
name me "• Topic Practice" suffix save hota hai.

Koi naya collection nahi — Phase 17 wala `questions.topic` field hi reuse hota hai. Jis exam
me admin ne topic khali chhoda ho, wahan empty state dikhega.

## Previous Year Questions (Phase 19)
Indian exam prep apps ka sabse searched feature. Home par **PYQ** button `PyqActivity` kholta
hai:

- Exam select karo → tagged PYQ years chips me (naye year upar)
- Year select karo → paper/shift chips (Prelims, Tier 1, Shift 2, …)
- Ek se zyada papers hon to **"Saare papers"** chip bhi aati hai
- Tap karte hi Test screen PYQ mode me start hoti hai — **questions shuffle nahi hote**
  (admin ne jis order me upload kiya wahi paper order), timer chhote set par 1 min/Q,
  20+ questions par exam ka official time

**Admin:** question form me **Previous Year Question** checkbox + Year (required) + Paper/Shift
(optional). Edit par pehle se tagged PYQ fields prefill ho jaate hain. Manage list me
`PYQ 2024 Prelims` badge dikhta hai.

**Schema (questions collection, extra fields):**
- `isPyq` (Boolean)
- `pyqYear` (Int, jaise 2024)
- `pyqPaper` (String, optional)

Naya Firestore collection / index / security rule change nahi — read already signed-in users
ko allowed hai, write sirf admin. History/Result me attempt name `"SSC CGL • PYQ 2024 • Prelims"`
jaisa save hota hai taaki review me mock vs PYQ alag dikhe.

## Notes
- Negative marking off hai by default — `util/Constants.kt` me `NEGATIVE_MARK` change kar sakte ho.
- Koi Android Studio / wrapper zip nahi diya — seedha GitHub push karo, Actions khud build karega. Agar Android Studio me kholna hai to ek baar khulte hi wo khud gradle wrapper regenerate kar dega.
