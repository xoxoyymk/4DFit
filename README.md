# 4D FIT — Android fitness & wellness app

Kotlin + Jetpack Compose (Material 3), clean architecture, offline-first, with an optional REST backend.

**Features:** animated first-launch registration with zodiac reveal · Home dashboard (greeting, today's
workout, activity rings, hydration, wellness goal, nutrition tip, streak, weekly chart) · workout library
(29 exercises, 8 programs, 10 filters, search) with animated "kinetic figure" demonstrations · exercise
detail with rest timer, safety notes and common mistakes · interactive player (get-ready countdown,
work/rest intervals, sets, reps, pause/resume/skip/finish, haptics, keep-screen-on, confetti +
badge unlock) · 30-day balanced nutrition plan with meal check-offs · daily horoscope with disclaimer ·
profile + edit · progress (weekly chart, 4-week heatmap, history, 7 achievements) · settings (theme,
units, high contrast, reduce motion, language, notifications, privacy, terms, logout, delete account).

---

## 1. Run it (about 5 minutes)

**You need:** Android Studio Ladybug (2024.2) or newer, JDK 17 (bundled with Android Studio), Android SDK 35.

1. Unzip, then **File › Open** the `4DFit` folder (the one containing `settings.gradle.kts`).
2. Let Gradle sync. If Android Studio offers to upgrade AGP or a library, you can accept — versions are in
   `gradle/libs.versions.toml`.
3. Pick an emulator or phone (Android 8.0 / API 26+) and press **Run**.

The app starts in **mock mode**: a built-in on-device server, so everything works offline with no setup.
Create a profile and use the app normally.

Command line: `./gradlew assembleDebug` (APK in `app/build/outputs/apk/debug/`) and `./gradlew test`.

## 2. Use the real backend (optional)

```bash
cd backend
npm install
npm run smoke                      # 15 end-to-end checks
JWT_SECRET=some-long-secret npm start   # http://localhost:8080/v1
```
Then in `gradle.properties`:
```
fourdfit.useMockApi=false
fourdfit.apiBaseUrl=http://10.0.2.2:8080/v1/     # Android emulator → your computer
```
Cleartext to `10.0.2.2` is allowed **only in debug builds**. Release builds require an `https://` URL
(enforced in `ApiFactory` and the network security config). For Firebase instead, see `docs/FIREBASE.md`.

## 3. Architecture

```
 UI (Compose screens) ──observe StateFlow── ViewModels
                                              │ depend on interfaces
                                    domain/repository (contracts) + domain/model + usecase
                                              │ implemented by
                 data/repository ──► FitApi (Retrofit | MockFitApi)   ── HTTPS/JSON
                        │        └─► Room (cache + local logs)  DataStore (settings)
                        └──────────► EncryptedSharedPreferences (session token)
```
- **Offline-first:** catalog and nutrition plan come from Room cache → bundled seed if never synced;
  workouts are saved locally first and uploaded (idempotent `clientId`), retried on next sync.
- **DI:** a small manual container (`di/AppContainer.kt`) + `appViewModel { }` helper. No annotation processing beyond Room.
- **Content:** `tools/generate_seed.py` produces the exercise library, 30-day plan and horoscope pools
  for both the app (`app/src/main/assets/seed`) and the backend (`backend/data/seed`).

### API
| Method | Path | Purpose |
|---|---|---|
| POST | /register, /login | Create account / sign in → `{token, user}` |
| GET, PUT | /profile | Read / update profile |
| DELETE | /account | Delete account and data |
| GET | /workouts, /workouts/{id} | Catalog / single program |
| GET | /nutrition/30-days | Meal plan |
| GET | /horoscope/{zodiac}?date= | Daily reading |
| POST | /workout/history | Save a session |
| GET | /progress | Totals + history |

## 4. Security & privacy
- Password field added to registration (required for secure sign-in). It is sent once over HTTPS and
  **never stored on the device**; only the server token is kept, in EncryptedSharedPreferences (Keystore-backed).
- HTTPS-only networking in release, no secrets in the APK (base URL comes from Gradle properties),
  `allowBackup=false`, R8 enabled for release, logging limited to request lines in debug.
- Profile photo, water and step logs stay on the device; photos are downscaled and EXIF-stripped.
- Logout clears local data; account deletion requires server confirmation first.
- Backend: bcrypt (cost 12), HS256 JWT, rate-limited auth, validated + size-limited bodies.

## 5. Accessibility
System font scaling (all text in sp, scrollable layouts), TalkBack labels on every icon button, merged
semantics on rings/charts with spoken summaries, 48dp touch targets, state shown with icons and text
as well as colour (checks, locks, "Unlocked/Locked", difficulty bars), a high-contrast theme and a
reduce-motion setting that stops looping animations.

## 6. Design
Dark "neon glass" look per the brief: deep gradient backdrop with drifting light orbs and a faint
perspective grid, glass cards with slowly orbiting gradient borders, depth (scale/tilt) page and card
transitions. The signature element is the **kinetic figure** — a neon stick athlete drawn on Canvas that
demonstrates each of 19 movement patterns. Typefaces: Space Grotesk (display) + Manrope (body), OFL.
Light theme available in Settings.

## 7. Notes, limits and next steps
- **Not compiled in my environment.** The Android SDK and Google Maven aren't reachable there, so the first
  full build happens in your Android Studio. What *was* verified: every Kotlin file parses (ktlint),
  the domain/util layer compiles with Kotlin 2.0.21 and all 13 unit tests pass, and the backend passes
  its 15-step smoke test. If Gradle reports an unresolved symbol (for example a Material icon renamed in
  a newer library), the fix is usually a one-line swap.
- UI strings are in Kotlin (English). For translations, move them to `strings.xml` and add locales to
  `res/xml/locales_config.xml`; the Language setting already opens Android's per-app language screen on Android 13+.
- Mock mode keeps one account per device and doesn't check passwords (it never stores them). Use the backend for real auth.
- Step counts use the hardware step counter and are approximate. Reminders use WorkManager and may arrive a few minutes late.
- Legal texts in `ui/settings/LegalContent.kt` are templates — review before publishing. Add Room migrations before changing the schema.

## 8. Files
```
.gitignore
app/build.gradle.kts
app/proguard-rules.pro
app/src/debug/res/xml/network_security_config.xml
app/src/main/AndroidManifest.xml
app/src/main/java/com/fourdfit/app/FourDFitApp.kt
app/src/main/java/com/fourdfit/app/MainActivity.kt
app/src/main/java/com/fourdfit/app/data/api/ApiFactory.kt
app/src/main/java/com/fourdfit/app/data/api/FitApi.kt
app/src/main/java/com/fourdfit/app/data/api/HoroscopeEngine.kt
app/src/main/java/com/fourdfit/app/data/api/MockFitApi.kt
app/src/main/java/com/fourdfit/app/data/api/SafeApiCall.kt
app/src/main/java/com/fourdfit/app/data/api/dto/Dtos.kt
app/src/main/java/com/fourdfit/app/data/database/FitDatabase.kt
app/src/main/java/com/fourdfit/app/data/local/AssetSeedLoader.kt
app/src/main/java/com/fourdfit/app/data/local/ImageStorage.kt
app/src/main/java/com/fourdfit/app/data/local/SecureTokenStore.kt
app/src/main/java/com/fourdfit/app/data/local/SettingsRepositoryImpl.kt
app/src/main/java/com/fourdfit/app/data/mapper/Mappers.kt
app/src/main/java/com/fourdfit/app/data/repository/AuthRepositoryImpl.kt
app/src/main/java/com/fourdfit/app/data/repository/ContentRepositories.kt
app/src/main/java/com/fourdfit/app/data/sensor/StepCounterManager.kt
app/src/main/java/com/fourdfit/app/di/AppContainer.kt
app/src/main/java/com/fourdfit/app/di/ViewModelFactory.kt
app/src/main/java/com/fourdfit/app/domain/model/Models.kt
app/src/main/java/com/fourdfit/app/domain/model/WellnessContent.kt
app/src/main/java/com/fourdfit/app/domain/repository/Repositories.kt
app/src/main/java/com/fourdfit/app/domain/usecase/ProgressCalculator.kt
app/src/main/java/com/fourdfit/app/navigation/AppNavHost.kt
app/src/main/java/com/fourdfit/app/navigation/BottomBar.kt
app/src/main/java/com/fourdfit/app/navigation/Routes.kt
app/src/main/java/com/fourdfit/app/notifications/Reminders.kt
app/src/main/java/com/fourdfit/app/ui/components/Background.kt
app/src/main/java/com/fourdfit/app/ui/components/Buttons.kt
app/src/main/java/com/fourdfit/app/ui/components/Celebration.kt
app/src/main/java/com/fourdfit/app/ui/components/Charts.kt
app/src/main/java/com/fourdfit/app/ui/components/Common.kt
app/src/main/java/com/fourdfit/app/ui/components/ExerciseAnimation.kt
app/src/main/java/com/fourdfit/app/ui/components/Glass.kt
app/src/main/java/com/fourdfit/app/ui/components/Inputs.kt
app/src/main/java/com/fourdfit/app/ui/home/HomeScreen.kt
app/src/main/java/com/fourdfit/app/ui/home/HomeViewModel.kt
app/src/main/java/com/fourdfit/app/ui/horoscope/HoroscopeScreen.kt
app/src/main/java/com/fourdfit/app/ui/horoscope/HoroscopeViewModel.kt
app/src/main/java/com/fourdfit/app/ui/nutrition/NutritionScreen.kt
app/src/main/java/com/fourdfit/app/ui/nutrition/NutritionViewModel.kt
app/src/main/java/com/fourdfit/app/ui/onboarding/OnboardingScreen.kt
app/src/main/java/com/fourdfit/app/ui/onboarding/OnboardingViewModel.kt
app/src/main/java/com/fourdfit/app/ui/onboarding/ProfileForm.kt
app/src/main/java/com/fourdfit/app/ui/profile/ProfileScreens.kt
app/src/main/java/com/fourdfit/app/ui/profile/ProfileViewModels.kt
app/src/main/java/com/fourdfit/app/ui/progress/ProgressScreen.kt
app/src/main/java/com/fourdfit/app/ui/root/FourDFitRoot.kt
app/src/main/java/com/fourdfit/app/ui/root/RootViewModel.kt
app/src/main/java/com/fourdfit/app/ui/settings/LegalContent.kt
app/src/main/java/com/fourdfit/app/ui/settings/SettingsScreens.kt
app/src/main/java/com/fourdfit/app/ui/settings/SettingsViewModels.kt
app/src/main/java/com/fourdfit/app/ui/theme/Color.kt
app/src/main/java/com/fourdfit/app/ui/theme/Theme.kt
app/src/main/java/com/fourdfit/app/ui/theme/Type.kt
app/src/main/java/com/fourdfit/app/ui/workout/DetailScreens.kt
app/src/main/java/com/fourdfit/app/ui/workout/PlayerViewModel.kt
app/src/main/java/com/fourdfit/app/ui/workout/WorkoutLibraryScreen.kt
app/src/main/java/com/fourdfit/app/ui/workout/WorkoutPlayerScreen.kt
app/src/main/java/com/fourdfit/app/ui/workout/WorkoutViewModels.kt
app/src/main/java/com/fourdfit/app/utils/AppResult.kt
app/src/main/java/com/fourdfit/app/utils/DateUtils.kt
app/src/main/java/com/fourdfit/app/utils/Units.kt
app/src/main/java/com/fourdfit/app/utils/Validators.kt
app/src/main/java/com/fourdfit/app/utils/ZodiacCalculator.kt
app/src/main/res/drawable/ic_launcher_background.xml
app/src/main/res/drawable/ic_launcher_foreground.xml
app/src/main/res/drawable/ic_launcher_monochrome.xml
app/src/main/res/drawable/ic_notification.xml
app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml
app/src/main/res/values/colors.xml
app/src/main/res/values/strings.xml
app/src/main/res/values/themes.xml
app/src/main/res/xml/locales_config.xml
app/src/main/res/xml/network_security_config.xml
app/src/test/java/com/fourdfit/app/ProgressCalculatorTest.kt
app/src/test/java/com/fourdfit/app/ValidatorsTest.kt
app/src/test/java/com/fourdfit/app/ZodiacCalculatorTest.kt
backend/.env.example
backend/README.md
backend/package-lock.json
backend/package.json
backend/src/horoscope.js
backend/src/server.js
backend/src/smoke-test.js
backend/src/store.js
build.gradle.kts
docs/FIREBASE.md
gradle.properties
gradle/libs.versions.toml
gradle/wrapper/gradle-wrapper.properties
gradlew
gradlew.bat
settings.gradle.kts
tools/generate_seed.py
```
(plus fonts in `app/src/main/res/font`, seed JSON in `app/src/main/assets/seed` and `backend/data/seed`, and the Gradle wrapper jar)
