# NMTV - Deployment Guide

This guide covers building, deploying, and distributing the NMTV Android TV application.

## Prerequisites

- Android Studio Arctic Fox or newer
- JDK 8 or higher
- Android SDK with API 34
- ADB (Android Debug Bridge)
- Android TV device for testing

## Building for Production

### Step 1: Update Version

Edit [`app/build.gradle.kts`](app/build.gradle.kts:12):

```kotlin
defaultConfig {
    applicationId = "com.nmtv.app"
    minSdk = 21
    targetSdk = 34
    versionCode = 2      // Increment for each release
    versionName = "1.1"  // Update version string
}
```

### Step 2: Create Signing Key

Generate a keystore for signing the release APK:

```bash
keytool -genkey -v -keystore nmtv-release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias nmtv-key
```

**Save the keystore file securely** - you'll need it for all future releases.

### Step 3: Configure Signing

Create `keystore.properties` in project root:

```properties
storeFile=/path/to/nmtv-release-key.jks
storePassword=YOUR_KEYSTORE_PASSWORD
keyAlias=nmtv-key
keyPassword=YOUR_KEY_PASSWORD
```

Add to `.gitignore`:
```
keystore.properties
*.jks
```

Update [`app/build.gradle.kts`](app/build.gradle.kts) to load signing config:

```kotlin
android {
    // ... existing config ...
    
    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = Properties()
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))
                
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

### Step 4: Replace Placeholder Assets

Before releasing, replace placeholder graphics:

1. **App Banner** (`app/src/main/res/drawable/`):
   - Create `app_banner.png` (320x180 pixels)
   - Remove `app_banner.xml`

2. **Launcher Icon** (`app/src/main/res/mipmap-*/`):
   - Create PNG icons for each density:
     - mdpi: 48x48 px
     - hdpi: 72x72 px
     - xhdpi: 96x96 px
     - xxhdpi: 144x144 px
     - xxxhdpi: 192x192 px
   - Remove XML placeholders

### Step 5: Build Release APK

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

### Step 6: Test Release Build

Install and test on Android TV:

```bash
adb install app/build/outputs/apk/release/app-release.apk
```

## Deployment Methods

### Method 1: Direct Installation (Sideloading)

**For personal/internal use:**

1. Transfer APK to TV via USB drive or network
2. Use file manager to install
3. Enable "Unknown Sources" in TV settings if prompted

### Method 2: Private Distribution

**For limited distribution:**

1. Host APK on secure server
2. Provide download link to users
3. Users install via browser or downloader app
4. Requires "Unknown Sources" enabled

### Method 3: Google Play Store

**For public distribution:**

#### Requirements:
- Google Play Developer account ($25 one-time fee)
- Privacy Policy URL
- Content rating questionnaire
- Store listing assets:
  - App icon (512x512)
  - Feature graphic (1024x500)
  - Screenshots (1920x1080, at least 2)
  - TV banner (320x180)

#### Steps:

1. **Create App Bundle** (preferred over APK):
   ```bash
   ./gradlew bundleRelease
   ```
   Output: `app/build/outputs/bundle/release/app-release.aab`

2. **Google Play Console**:
   - Go to [play.google.com/console](https://play.google.com/console)
   - Create new application
   - Upload AAB file

3. **Complete Store Listing**:
   - Title: NMTV
   - Short description (80 chars max)
   - Full description (4000 chars max)
   - Upload graphics assets
   - Set content rating
   - Add privacy policy URL

4. **Submit for Review**:
   - Create production release
   - Review and publish
   - Typically 1-3 days for approval

### Method 4: Enterprise Distribution (MDM)

**For corporate environments:**

1. Build signed APK
2. Upload to Mobile Device Management (MDM) system
3. Push to devices remotely
4. Examples: Google Workspace, VMware Workspace ONE

## Coolify Deployment Notes

Since you deploy to Coolify and cannot run scripts/exec on the platform:

### Build Strategy

**Option A: Build Locally, Deploy APK**
```bash
# Build release APK locally
./gradlew assembleRelease

# Deploy APK to CDN/storage
# Users download and install manually
```

**Option B: CI/CD Pipeline**

Create GitHub Actions workflow (`.github/workflows/build.yml`):

```yaml
name: Build APK

on:
  push:
    tags:
      - 'v*'

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Build Release APK
        run: ./gradlew assembleRelease
      
      - name: Upload APK
        uses: actions/upload-artifact@v3
        with:
          name: app-release
          path: app/build/outputs/apk/release/app-release.apk
```

Then download APK from Actions and host on Coolify static file server.

### Hosting APK on Coolify

1. **Create Static File Service**:
   - Set up Nginx service in Coolify
   - Mount volume for APK files

2. **Upload APK**:
   ```bash
   # From local machine to Coolify server
   scp app-release.apk user@coolify-server:/path/to/static/files/
   ```

3. **Provide Download Link**:
   - https://your-coolify-domain.com/nmtv/app-release.apk

4. **Version Management**:
   - Include version in filename: `nmtv-v1.0.0.apk`
   - Keep previous versions for rollback

## Over-the-Air (OTA) Updates

To implement auto-updates in the app:

1. **Create Update Check API**:
   ```kotlin
   // Check for updates from your server
   data class AppUpdate(
       val versionCode: Int,
       val versionName: String,
       val downloadUrl: String,
       val releaseNotes: String
   )
   ```

2. **Version Comparison**:
   ```kotlin
   fun checkForUpdate() {
       // Compare current versionCode with server versionCode
       // Prompt user to download if newer version available
   }
   ```

3. **Download and Install**:
   ```kotlin
   // Download new APK
   // Use Android DownloadManager
   // Prompt user to install
   ```

## Testing Checklist

Before deployment, verify:

- [ ] App launches and plays stream immediately
- [ ] Loading indicator shows during buffering
- [ ] Pause overlay appears on pause
- [ ] Error handling and retry logic works
- [ ] Remote control buttons respond correctly
- [ ] App exits cleanly on back button
- [ ] Stream restarts on app resume
- [ ] No crashes in logs (`adb logcat`)
- [ ] TV banner displays in launcher
- [ ] App icon is visible
- [ ] Proper video aspect ratio
- [ ] Audio plays correctly
- [ ] No memory leaks (test long playback)

## Monitoring

### Crash Reporting

Integrate Firebase Crashlytics for production monitoring:

1. Add Firebase to project
2. Add Crashlytics dependency
3. Initialize in `TvStreamApp.kt`
4. Monitor crashes in Firebase console

### Analytics

Track usage with Firebase Analytics:

```kotlin
// Log playback events
analytics.logEvent("video_start", bundle)
analytics.logEvent("video_error", bundle)
```

## Rollback Plan

If issues occur after deployment:

1. **Immediate**: Remove download link or Play Store listing
2. **Short-term**: Deploy previous version APK
3. **Long-term**: Fix issue and deploy new version

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0   | TBD  | Initial release |

## Support

For deployment issues:
- Check build logs
- Verify signing configuration
- Test on multiple Android TV devices
- Review Google Play policy compliance

---

**Note**: Keep signing keys and passwords secure. Loss of signing key means you cannot update the app on Google Play.