# PlayAccount Assistant Pro

Android app (Kotlin + Jetpack Compose) with a Gemini-powered assistant through a separate Node.js backend.

## Security
- Gemini API key belongs only in backend environment variable `GEMINI_API_KEY`.
- Never put the key in Android source, Gradle config, APK, or GitHub repository.
- Google account registration opens https://accounts.google.com/signup.
- The app does not automate account creation, bypass verification, or store Google passwords/OTP codes.
- Account manager stores email addresses only in app-private preferences.
- Independent project; not affiliated with Google.

## Build Android
Requirements: JDK 17, Android SDK Platform 35, Gradle 8.9.
```bash
gradle assembleDebug
```
APK: `app/build/outputs/apk/debug/app-debug.apk`
Or push to GitHub and run Actions -> Build PlayAccount Assistant Pro APK.

## Deploy Gemini backend
1. Create an API key in Google AI Studio.
2. Deploy the `backend` folder as a Node.js service or use its Dockerfile.
3. Add `GEMINI_API_KEY` as a secret/environment variable in the hosting provider dashboard.
4. Optional: set `GEMINI_MODEL` (default `gemini-flash-latest`). The backend uses Google's current `@google/genai` JavaScript SDK and includes a basic per-IP request limit.
5. Check `https://YOUR-SERVICE/health`.
6. In the Android app Settings, paste the backend base URL.

Before public release, add backend authentication, rate limiting, privacy policy, abuse monitoring, and production testing.
