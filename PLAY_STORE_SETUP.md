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
8. **Privacy policy:** a hosted URL is **required** because the app requests location. Use the
   company policy already live at **<https://www.tertiaryinfotech.com/privacy-policy-html>** — paste
   that URL into Play Console. (A longer app-specific version is kept in
   [`store/privacy-policy.md`](store/privacy-policy.md) for reference, but the company URL is what we
   submit.)
   > Note: the company page is a general PDPA policy and does not explicitly mention location/mobile
   > data. Because the app transmits nothing off-device this is acceptable, but if Play's reviewer
   > pushes back on the background-location declaration, host the fuller policy in
   > `store/privacy-policy.md` at a tertiaryinfotech.com URL and swap it in.
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

## 6. Release path: Internal → **Closed test** → Production  **[YOU]**

> Current Console state: **Draft · Internal testing** (versionCode 1 already uploaded). The goal is
> Production. The bridge is a **Closed test**.
>
> **Why a closed test is required:** Google requires personal developer accounts (created after
> 13 Nov 2023) to run a **closed test with at least 12 testers, opted-in for 14 days**, before they
> can apply for production access. The Tertiary Infotech account is an **organisation** account,
> which is normally exempt — but running a closed test first is still the safe, recommended path and
> is what this guide assumes. If the Console offers "Apply for production access" without the
> 12-tester gate, you may skip straight to §6.3.

### 6.1 Create the Closed testing track
1. **Release → Testing → Closed testing → Create track** (or use the default "Closed testing –
   Alpha"). Name it e.g. **Alpha**.
2. **Testers tab → create an email list** and add tester Google accounts (aim for **12+** if you want
   to satisfy the production-access requirement). Save.
3. **Releases tab → Create new release.**
   - **Promote the existing build:** the versionCode 1 bundle already in Internal testing can be
     promoted — no rebuild. Or upload `app/build/outputs/bundle/release/app-release.aab` fresh.
   - Add **release notes** (a starter is in §6.4 below).
   - **Save → Review release → Start rollout to Closed testing.**
4. Share the **opt-in URL** (Testers tab) with your testers. Have them join and install. Keep the
   test open **≥ 14 days** with **≥ 12 testers** if you're using it to unlock production access.

### 6.2 Rebuild only if you change versionCode
Promoting reuses versionCode 1. If you instead want a *new* upload, bump it first:
```kotlin
// app/build.gradle.kts
versionCode = 2
versionName = "1.0.1"
```
then `./gradlew :app:bundleRelease` and upload the new AAB.

### 6.3 Promote to Production
1. **Release → Production → Create new release.**
2. **Promote** the closed-testing build (recommended) or upload the AAB.
3. Confirm the **Data safety**, **content rating**, **target audience**, **app access**, and the
   **background-location declaration + demo video** (§4.10) are all complete — Production won't submit
   until every "Set up your app" task is green.
4. **Privacy policy URL** must be set to <https://www.tertiaryinfotech.com/privacy-policy-html>.
5. Add release notes → **Review release → Start rollout to Production → Send for review.** First
   review is typically a few days.

### 6.5 Automated upload via fastlane (Google Play Developer API)
Instead of clicking, uploads/promotions can be scripted. Lanes are in [`fastlane/Fastfile`](fastlane/Fastfile).

**One-time setup (you):**
1. **Service account:** Play Console → **Users & permissions → API access** (or Google Cloud → IAM →
   Service Accounts) → create a service account → create a **JSON key**. Save it to
   `keystore/play-service-account.json` (gitignored).
2. In Play Console → **Users & permissions**, invite the service-account email and grant **Release
   to testing tracks** and **Release to production**.
3. The Console "Set up your app" questionnaires (Data safety, content rating, target audience, app
   access, privacy URL, background-location declaration + demo video) have **no API** — complete them
   in the web UI once.

**Then, from the repo root:**
```bash
fastlane closed       # upload the AAB to the Closed testing (alpha) track
fastlane production   # promote to Production and roll out / send for review
```
> The very first release on a brand-new track sometimes must be created once in the Console before the
> API will accept updates; if `fastlane closed` errors on "no existing release", do that first upload
> manually, then the API works for everything after.

### 6.4 Release notes — copy/paste
```
RunTrack GPS — first release.
- GPS run tracking with live route map, distance, pace, and timer
- Voice commands: start, pause, resume, stop
- Background tracking with a persistent notification (screen can be off)
- Local run history — no account, no ads, no data leaves your device
```

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
