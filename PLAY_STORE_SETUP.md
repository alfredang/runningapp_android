# Google Play Store — Setup & Submission Guide (RunTrack GPS)

This is the end-to-end guide to ship **RunTrack GPS** to Google Play. Steps that **only you**
can do (they need your Google login) are marked **[YOU]**; everything else is already done in
this repo or scripted.

- **Package name:** `com.tertiaryinfotech.runtrackgps`
- **App name:** RunTrack GPS
- **Developer:** Tertiary Infotech Academy Pte. Ltd.

---

## 1. Google Maps API key

The map needs a key. This is separate from Play Console (it lives in Google Cloud).

**[YOU]** Create the key:
1. Go to <https://console.cloud.google.com/> and select (or create) a project, e.g. *RunTrack GPS*.
2. **APIs & Services → Library →** enable **“Maps SDK for Android”**.
3. **APIs & Services → Credentials → Create credentials → API key.** Copy the key.
4. (Recommended) **Restrict** the key → *Android apps* → add:
   - Package name: `com.tertiaryinfotech.runtrackgps`
   - SHA-1 fingerprints: add **both** your debug and release/upload SHA-1 (see §3 to get them).
   - Under *API restrictions*, restrict to **Maps SDK for Android**.
5. Billing must be enabled on the Cloud project (Maps has a generous always-free tier; you won't be
   charged for normal usage, but a billing account must exist).

Then put the key in `local.properties` (already gitignored):

```properties
MAPS_API_KEY=AIza...your_key...
```

> Until a valid key is set, the app builds and runs but the map tiles stay blank — everything else
> (GPS, distance, timer, voice) works.

---

## 2. Create your upload keystore  *(scripted — run once)*

Google Play apps must be signed. Run the helper (it generates a keystore and `keystore.properties`,
both gitignored):

```bash
cd mobile/Android/runningapp
./scripts/make_keystore.sh
```

It will prompt for a password (or accept one as `$1`). Output:
- `keystore/runtrackgps-release.jks` — **back this up somewhere safe. If you lose it you can't update the app**
  (unless enrolled in Play App Signing, which we use — see below).
- `keystore/keystore.properties` — passwords read by Gradle at build time.

---

## 3. Build the release App Bundle (.aab)  *(scripted)*

```bash
./gradlew :app:bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab
```

Get your **upload SHA-1** (needed for the Maps key restriction in §1) any time with:

```bash
keytool -list -v -keystore keystore/runtrackgps-release.jks -alias runtrackgps | grep SHA1
```

---

## 4. Play Console — first-time app setup  **[YOU]**

> The Play Console has no public API for *creating* an app or filling these forms, so these are
> manual web steps. They take ~30–45 min the first time. Answers tailored to this app are below.

1. Go to <https://play.google.com/console> (you need a **Play Developer account**, US$25 one-time).
2. **Create app:**
   - App name: **RunTrack GPS**
   - Default language: English (US)
   - App or game: **App**
   - Free or paid: **Free**
   - Accept the declarations.
3. **Set up your app → App access:** *All functionality is available without special access.*
4. **Ads:** *No, this app does not contain ads.*
5. **Content rating** (questionnaire). Answers for this app:
   - Category: **Health & Fitness / Utility** (no violence, no sexual content, no profanity,
     no controlled substances, no gambling, no user interaction/sharing). Result: **Everyone / PEGI 3**.
6. **Target audience and content:** target age **18+** (or 13+; the app is a general fitness tool).
   Not directed to children.
7. **Data safety** (this is the important one). For this app:
   - **Does your app collect or share any of the required user data types?**
     - The app collects **Location (approximate & precise)** and **Audio (voice/microphone)** — but
       **all processing is on-device and nothing is transmitted off the device or shared.**
     - Recommended honest answers:
       - *Location:* **Collected = Yes** (used on device), **Shared = No.** Purpose: **App functionality.**
         Processed ephemerally / not sent to a server. Optional: *No* (required for core feature).
       - *Audio (voice commands):* recognition is handled by the Android speech recognizer; mark
         **not collected** by *your* app if you do not store/transmit it (you don't). If you prefer to
         be conservative, declare *Audio → Collected = Yes, Shared = No, App functionality.*
     - **Data is encrypted in transit:** N/A (no transmission) — answer per the form (say Yes; there is
       no network traffic to expose).
     - **Users can request data deletion:** data lives only on-device and is removed on uninstall /
       via in-app *Clear* in History.
   - A ready-to-paste summary is in **§7** below.
8. **Privacy policy:** a hosted URL is **required** because the app requests location. A ready-made
   policy is in [`store/privacy-policy.md`](store/privacy-policy.md) — host it (GitHub Pages, your
   site, etc.) and paste the URL.
9. **Government apps / Financial / Health:** No to all (it's a personal fitness tracker).
10. **Foreground service / Location permission declaration:** Because the app uses
    `ACCESS_BACKGROUND_LOCATION` + a `location` foreground service, Play will ask you to **justify
    background location**. Use the text in **§8**, and record a short screen video showing the run
    tracking continuing with the screen off (Play requires a demo video for background location).

---

## 5. Store listing  **[YOU] paste the prepared copy**

**Main store listing** (copy from [`store/listing.md`](store/listing.md)):
- App name: RunTrack GPS
- Short description (≤80 chars)
- Full description (≤4000 chars)
- App icon: 512×512 PNG → `store/assets/icon-512.png` (generated by `scripts/make_play_assets.sh`)
- Feature graphic: 1024×500 PNG → `store/assets/feature-1024x500.png`
- Phone screenshots: `store/assets/screenshots/` (min 2, 1080×1920 or similar)
- Category: **Health & Fitness**
- Contact email: angch@tertiaryinfotech.com

---

## 6. Create a release  **[YOU]**

1. **Release → Testing → Internal testing** (fastest; recommended first).
   - Upload `app-release.aab`.
   - Add your own Google account as a tester, save, roll out.
   - Install via the opt-in link on your phone and verify.
2. When happy: **Release → Production → Create new release**, upload the same AAB (or a new
   versionCode), complete the release notes, and **Send for review**. First review is typically a few
   days.

> **Play App Signing:** accept it when prompted (default). Google holds the *app signing key*; your
> `.jks` is only the *upload key*. If you ever lose the upload key, Google can reset it — but still
> back it up.

---

## 7. Data safety — copy/paste summary

```
Data collected: Location (approximate + precise). Used only on the device to draw your route and
measure distance/pace. Not shared with anyone. Not sent off the device.
Microphone/voice: used only in-app for voice commands ("start/pause/stop") via the Android speech
recognizer; not stored or shared by the app.
No accounts, no ads, no analytics, no third-party data sharing.
Data is stored locally and removed on uninstall or via the in-app "Clear" action.
```

## 8. Background-location justification — copy/paste

```
RunTrack GPS is an outdoor run tracker. It uses background location and a foreground service so it
can keep recording the user's running route, distance, and pace accurately when the screen is off or
the phone is in a pocket during a run. Background location is essential to the core feature
(continuous GPS tracking of a workout) and is only active while a run is in progress, indicated by a
persistent notification. Location data never leaves the device.
```

---

## Quick command reference

```bash
# Debug build + install on connected phone/emulator
./gradlew :app:installDebug

# Release bundle for Play
./gradlew :app:bundleRelease

# Show upload-key SHA-1 / SHA-256 (for Maps key + Play App Signing)
keytool -list -v -keystore keystore/runtrackgps-release.jks -alias runtrackgps

# Generate 512 icon, feature graphic, framed screenshots
./scripts/make_play_assets.sh
```
