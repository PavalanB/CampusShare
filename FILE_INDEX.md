# CampusShare — Complete File Index

This single project folder contains ALL phases built so far.
Copy the entire `CampusShare/` folder into Android Studio.

---

## Phase 1 — Authentication ✅

| File | Location | Purpose |
|------|----------|---------|
| `LoginActivity.java` | activities/ | Login with email + password |
| `RegisterActivity.java` | activities/ | Register with college ID, dept, year |
| `ForgotPasswordActivity.java` | activities/ | Send Firebase password reset email |
| `AuthRepository.java` | repositories/ | All Firebase Auth calls |
| `User.java` | models/ | User data model |
| `SessionManager.java` | utils/ | Cache logged-in user in SharedPreferences |
| `activity_login.xml` | res/layout/ | Login screen UI |
| `activity_register.xml` | res/layout/ | Register screen UI |
| `activity_forgot_password.xml` | res/layout/ | Forgot password UI |

---

## Phase 2 — Resource Listing + Photo Upload ✅

| File | Location | Purpose |
|------|----------|---------|
| `MainActivity.java` | activities/ | Home screen with bottom nav (Browse / My Listings / Profile) |
| `AddResourceActivity.java` | activities/ | Add or edit a resource, with gallery photo picker |
| `ResourceDetailActivity.java` | activities/ | Full detail view + "Request to Borrow" button |
| `ProfileActivity.java` | activities/ | Shows name, dept, credit score, avg rating |
| `ResourceRepository.java` | repositories/ | Firestore CRUD + Firebase Storage photo upload |
| `Resource.java` | models/ | Resource data model |
| `ResourceAdapter.java` | adapters/ | RecyclerView adapter (Browse + My Listings modes) |
| `activity_main.xml` | res/layout/ | RecyclerView + BottomNav + FAB |
| `activity_add_resource.xml` | res/layout/ | Add/Edit resource form |
| `activity_resource_detail.xml` | res/layout/ | Resource detail screen |
| `activity_profile.xml` | res/layout/ | Profile screen |
| `item_resource.xml` | res/layout/ | Single resource card in the list |
| `menu_bottom_nav.xml` | res/menu/ | Browse / My Listings / Profile tabs |
| `badge_available.xml` | res/drawable/ | Green pill badge |
| `badge_unavailable.xml` | res/drawable/ | Red pill badge |
| `circle_avatar.xml` | res/drawable/ | Round avatar background |
| `ic_resource_placeholder.xml` | res/drawable/ | Placeholder image for resources |
| `spinner_background.xml` | res/drawable/ | Styled spinner border |

---

## Coming Next

| Phase | What gets built |
|-------|----------------|
| Phase 3 | Borrow request flow — send, accept, reject, mark returned |
| Phase 4 | Search + filter by category / department |
| Phase 5 | Credit score engine + priority borrowing logic |
| Phase 6 | FCM push notifications |
| Phase 7 | Ratings after return |
| Phase 8 | Need posts — broadcast what you need |

---

## How to open in Android Studio

1. Open Android Studio
2. File → Open → select the `CampusShare/` folder
3. Wait for Gradle sync to finish
4. Add your `google-services.json` to `app/` (from Firebase Console)
5. Run on emulator or device

## Package name
`com.campusshare`

## Min SDK
24 (Android 7.0) — covers 94%+ of active devices
