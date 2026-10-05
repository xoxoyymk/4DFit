# Using Firebase instead of the REST backend (optional)

The UI depends only on the interfaces in `domain/repository/Repositories.kt`, so Firebase can
replace the REST/mock layer without touching any screen. Steps:

## 1. Project setup
1. Create a Firebase project and add an Android app with package `com.fourdfit.app`.
2. Download `google-services.json` into `app/` (it is git-ignored on purpose).
3. In `gradle/libs.versions.toml` add:
   ```toml
   [versions]
   googleServices = "4.4.2"
   firebaseBom = "33.7.0"
   [libraries]
   firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
   firebase-auth = { group = "com.google.firebase", name = "firebase-auth" }
   firebase-firestore = { group = "com.google.firebase", name = "firebase-firestore" }
   firebase-messaging = { group = "com.google.firebase", name = "firebase-messaging" }
   [plugins]
   google-services = { id = "com.google.gms.google-services", version.ref = "googleServices" }
   ```
4. Apply `alias(libs.plugins.google.services)` in `app/build.gradle.kts` (and `apply false` at the root),
   then add `implementation(platform(libs.firebase.bom))`, `implementation(libs.firebase.auth)`,
   `implementation(libs.firebase.firestore)` and optionally `implementation(libs.firebase.messaging)`.
   Add `kotlinx-coroutines-play-services` for `.await()`.

## 2. Data model (Firestore)
```
users/{uid}                      profile fields (no password, no photo)
users/{uid}/history/{clientId}   workout sessions (clientId = idempotent key from the app)
content/workouts                 catalog JSON (same shape as GET /workouts)
content/nutrition                30-day plan JSON
horoscopes/{SIGN}_{yyyy-MM-dd}   written daily by a scheduled Cloud Function
```

## 3. Security rules
```
rules_version = '2';
service cloud.firestore {
  match /databases/{db}/documents {
    match /users/{uid} {
      allow read, write, delete: if request.auth != null && request.auth.uid == uid;
      match /history/{id} {
        allow read, write, delete: if request.auth != null && request.auth.uid == uid;
      }
    }
    match /content/{doc}    { allow read: if request.auth != null; }
    match /horoscopes/{doc} { allow read: if request.auth != null; }
  }
}
```

## 4. Repository implementation (sketch)
```kotlin
class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val userDao: UserDao,
) : AuthRepository {
    override val isLoggedIn: Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser != null) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun register(data: RegistrationData): AppResult<Unit> = runCatching {
        val uid = auth.createUserWithEmailAndPassword(data.email, data.password).await().user!!.uid
        val dto = data.toRequest().let { r ->
            mapOf("fullName" to r.fullName, "email" to r.email, "dateOfBirth" to r.dateOfBirth,
                  "gender" to r.gender, "heightCm" to r.heightCm, "activityLevel" to r.activityLevel,
                  "fitnessGoal" to r.fitnessGoal, "country" to r.country)
        }
        db.collection("users").document(uid).set(dto).await()
        // also upsert into Room (userDao) exactly like AuthRepositoryImpl does
    }.fold({ AppResult.Success(Unit) }, { AppResult.Error(it.localizedMessage ?: "Sign-up failed") })

    override suspend fun login(email: String, password: String) = runCatching {
        auth.signInWithEmailAndPassword(email, password).await()
    }.fold({ AppResult.Success(Unit) }, { AppResult.Error("Incorrect email or password.") })

    override suspend fun logout() { auth.signOut() /* + clear Room, settings, photo as in AuthRepositoryImpl */ }

    override suspend fun deleteAccount(): AppResult<Unit> = runCatching {
        val user = auth.currentUser ?: error("Not signed in")
        db.collection("users").document(user.uid).delete().await()   // delete history via a Cloud Function
        user.delete().await()
    }.fold({ AppResult.Success(Unit) }, { AppResult.Error(it.localizedMessage ?: "Couldn't delete account") })
}
```
Firebase Auth stores the session token securely itself; passwords are never kept by the app.
Implement `ProfileRepository`, `WorkoutRepository` (history sub-collection), `NutritionRepository`
and `HoroscopeRepository` the same way, keep Room as the offline cache, then construct the Firebase
implementations in `di/AppContainer.kt`.

## 5. Daily horoscope + notifications
- A scheduled Cloud Function (`onSchedule("every day 00:05")`) can port `backend/src/horoscope.js`
  and write `horoscopes/{SIGN}_{date}` for all 12 signs.
- For push reminders, subscribe devices to an FCM topic per sign (`horoscope_leo`) and send a short
  message after generation. Local WorkManager reminders keep working without FCM.
