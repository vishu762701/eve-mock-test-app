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
5. **Firestore Database → Create database** (production mode) → 2 collections banega apne aap jab data add hoga: `exams`, `questions`.
6. Firestore **Rules** tab me yeh laga do (MVP ke liye — logged-in user read kar sake, sirf admins write karein):
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
    match /{document=**} {
      allow read: if request.auth != null;
      allow write: if isAdmin();
    }
  }
}
```
   Yeh 4 emails already `Constants.kt` me bhi hardcoded hain, dono jagah match hone chahiye. Agar app ke andar se koi naya admin add karo, wo `admins` collection me save hota hai aur is rule ka `exists(...)` check use hoke automatically usko bhi write access mil jata hai — Firestore rules dobara publish karne ki zaroorat nahi padti jab tak naye hardcoded emails na jodne ho.

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

## Notes
- Negative marking off hai by default — `util/Constants.kt` me `NEGATIVE_MARK` change kar sakte ho.
- Koi Android Studio / wrapper zip nahi diya — seedha GitHub push karo, Actions khud build karega. Agar Android Studio me kholna hai to ek baar khulte hi wo khud gradle wrapper regenerate kar dega.
