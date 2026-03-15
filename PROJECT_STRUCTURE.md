# CampusShare — Project Structure

## Folder Layout

```
CampusShare/
├── FIREBASE_SETUP.md                  ← Start here before running the app
├── app/
│   ├── build.gradle                   ← All dependencies (Firebase, Material, Glide)
│   ├── google-services.json           ← YOU must add this from Firebase Console
│   └── src/main/
│       ├── AndroidManifest.xml        ← App entry point, permissions, activity list
│       ├── java/com/campusshare/
│       │   ├── activities/
│       │   │   ├── LoginActivity.java          ← Login screen logic
│       │   │   ├── RegisterActivity.java       ← Registration screen logic
│       │   │   ├── ForgotPasswordActivity.java ← Password reset logic
│       │   │   └── MainActivity.java           ← Home screen (placeholder for Phase 2)
│       │   ├── models/
│       │   │   └── User.java                  ← User data model (matches Firestore doc)
│       │   ├── repositories/
│       │   │   └── AuthRepository.java        ← ALL Firebase Auth + Firestore calls
│       │   └── utils/
│       │       └── SessionManager.java        ← Saves logged-in user to SharedPreferences
│       └── res/
│           ├── layout/
│           │   ├── activity_login.xml
│           │   ├── activity_register.xml
│           │   ├── activity_forgot_password.xml
│           │   └── activity_main.xml
│           ├── drawable/
│           │   └── spinner_background.xml     ← Styled border for department/year spinners
│           └── menu/
│               └── menu_main.xml              ← Logout option in toolbar
```

---

## Architecture Pattern

```
Activity (UI only)
     ↓ calls
Repository (all Firebase logic)
     ↓ reads/writes
Firebase Auth + Firestore
```

Activities never call Firebase directly. This keeps code clean and
easy to debug — if something breaks, you know exactly which file to look at.

---

## Phase Roadmap

| Phase | What gets built | Files to add |
|-------|----------------|--------------|
| ✅ 1 | Auth (Login, Register, Forgot Password) | Done |
| 2 | Resource listing + photo upload | ResourceActivity, ResourceRepository, Resource.java |
| 3 | Borrow request flow | RequestActivity, RequestRepository, BorrowRequest.java |
| 4 | Search + filter | SearchFragment, SearchAdapter |
| 5 | Credit score engine | CreditManager.java, LedgerEntry.java |
| 6 | Push notifications | FCMService.java, NotificationHelper.java |
| 7 | Ratings system | RatingDialog, update RequestRepository |
| 8 | Need posts | NeedPostActivity, NeedPostRepository |

---

## Key Design Decisions

- `AuthRepository` translates Firebase error codes into friendly English messages
- `SessionManager` caches the user locally so screens load instantly without hitting Firestore
- `User.java` has an empty constructor — this is required by Firestore's `.toObject(User.class)`
- Credit ledger lives at `/users/{uid}/ledger/{partnerID}` not as a single number —
  this tracks directional debt (who owes whom specifically)
