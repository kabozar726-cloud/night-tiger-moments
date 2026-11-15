# Night Tiger — PUBG Moments v1.0.0

**App:** Night Tiger — PUBG Moments
**Player:** ꧁ Night Tiger ꧂ (Abozar's PUBG Mobile in-game name)
**Package:** `com.nighttiger.moments` — versionCode 1, versionName `1.0.0`
**Repo:** https://github.com/kabozar726-cloud/night-tiger-moments

## v1 kya karta hai
1. Gallery se lambi PUBG screen-recording video chunein.
2. **Scan** dabayein — app video ko on-device scan karti hai:
   - **Primary signal:** 5-second windows mein loud audio energy (gunfire /
     excitement), ffmpeg `volumedetect` se (mean_volume dB).
   - **Secondary signal:** scene-change count, aik ffmpeg `select/showinfo`
     pass se (best effort — agar fail ho to loudness hi faisla karti hai).
3. Candidate moments ki list aati hai — har aik mein time range, loudness
   score (0–100), peak dB aur scene changes. Aap preview karke tick karein.
4. **Shorts Banao** — har chuna hua moment 30–60 second ki vertical 9:16
   Short banta hai (720x1280, scale + pad), **original game audio ke saath**.
5. Shorts Gallery mein save hoti hain: **Movies / Night Tiger**. Share dabakar
   TikTok (ya kisi bhi app) par bhej sakte hain.

## v1 mein kya NAHI hai (user ki order)
- **No voiceover** — koi AI voice nahi lagti.
- **No captions** — koi likha hua text nahi lagta.
- Sirf best-moment extraction aur cutting.

## Imaandaar hadood (ye app ke rules hain, bug nahi)
- Detection **signal-based** hai, insani faisla nahi. Loud window sirf
  candidate hai — aik acha moment khamosh bhi ho sakta hai (silent clutch),
  aur aik loud window bekaar bhi ho sakti hai (lobby noise, plane). Is liye
  preview + user selection zaroori hai; app aap ki tap ke baghair cut nahi karti.
- Lambi video scan / cut karne mein phone par waqt lagta hai (battery +
  storage). Sara bhari kaam UI thread se bahar hota hai (Dispatchers.IO +
  FFmpegKit async) — app hang nahi hoti.
- Scene-change pass 30 minute se lambi video par skip ho jata hai (sirf
  loudness use hoti hai), taake scan khatam ho sake.

## Build
- Stack: AGP 8.5.2, Kotlin 2.0.21, Compose BOM 2024.06.00, compiler 1.5.14,
  minSdk 26, targetSdk 34, compileSdk 34, JDK 17, Gradle 8.9.
- On-device engine: `com.arthenica:ffmpeg-kit-full:6.0.LTS` (no server, no upload).
- GitHub Actions (`.github/workflows/android.yml`): `gradle assembleDebug`,
  release tag `build-<number>`, APK `app/build/outputs/apk/debug/app-debug.apk`.
