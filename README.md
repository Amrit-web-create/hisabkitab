# Hisab & List (Kotlin · Jetpack Compose)

100% offline kirana + ghar list app. 3 tabs (List/Bill, Calculator, History), EN | हिंदी toggle,
voice parser ("allo" → Potato ↔ आलू), Set Rate (voice + keypad), offline UPI QR, WhatsApp/share.

## GitHub par APK build
1. Is poore folder ko ek naye GitHub repo me push karo (root me `app/`, `.github/` dikhna chahiye).
2. Repo → **Actions** → "Android Build" → run (push par apne aap chalta hai).
3. Run khatam hone par **Artifacts → HisabList-debug-apk** se `app-debug.apk` download karo.

## Android Studio
File → Open → is folder ko kholo → Run ▶ (Android Studio 2024.1+, JDK 17).

## Code map (`app/src/main/java/com/hisablist/app/`)
- `data/Dictionary.kt` – bilingual item dictionary + alias map (allo/aalu/alu/potato → आलू), number & unit words
- `data/VoiceParser.kt` – offline parser ("2 kilo allo", "doodh 2 packet", "chini hatao"), voice-math, calculator
- `data/UpiQr.kt` – `upi://pay?pa=..&pn=..&am=..&cu=INR` + offline QR (ZXing core) + shareable PNG
- `data/Store.kt` – SharedPreferences/JSON persistence (lists, rates, settings)
- `data/Strings.kt` – EN/हिंदी UI strings
- `ui/Voice.kt` – SpeechRecognizer with EXTRA_PREFER_OFFLINE, typing fallback; TTS for calculator
- `ui/ListScreen.kt`, `CalcScreen.kt`, `HistoryScreen.kt`, `SettingsScreen.kt` (Set Rate), `QrDialog.kt`
- `ui/Theme.kt` – cartoon UI kit (thick outlines, chunky 3D buttons, dotted background)

## Voice offline note
Phone me Settings → Language → Voice input / Google → Offline speech recognition me Hindi (India) aur
English (India) pack download kar lo. Pack na ho to app typing fallback dikhata hai.
