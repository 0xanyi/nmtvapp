# NMTV - Android TV Streaming App

A minimalist Android TV application that launches directly into fullscreen HLS video playback with no navigation menus or channel selection interface.

## Features

- **Instant Playback**: Opens directly to fullscreen video on app launch
- **HLS Streaming**: Adaptive bitrate streaming with ExoPlayer
- **TV Remote Support**: Play/pause with center button, exit with back button
- **Auto-Reconnection**: Exponential backoff retry logic (1s → 2s → 4s → 8s → 16s, max 30s)
- **Loading Indicators**: Visual feedback during buffering and errors
- **Minimal UI**: Clean pause overlay with play button when paused
- **Future-Ready**: Channel switching architecture ready for expansion

## Requirements

- **Android TV**: API 21+ (Android 5.0 Lollipop or higher)
- **Internet Connection**: Required for HLS streaming
- **TV Device**: Google TV, Android TV, or Fire TV

## Project Structure

```
app/
├── src/main/
│   ├── java/com/nmtv/app/
│   │   ├── MainActivity.kt              # Main video player activity
│   │   ├── TvStreamApp.kt               # Application class
│   │   ├── data/
│   │   │   ├── model/Channel.kt         # Channel data model
│   │   │   └── repository/              # Channel data sources
│   │   └── player/
│   │       ├── StreamPlayer.kt          # ExoPlayer wrapper
│   │       ├── StreamPlayerListener.kt  # Player callbacks
│   │       └── RetryManager.kt          # Reconnection logic
│   ├── res/
│   │   ├── layout/activity_main.xml     # Fullscreen player layout
│   │   ├── values/                      # Strings, colors, styles
│   │   └── drawable/                    # Icons and graphics
│   └── AndroidManifest.xml              # TV configuration
└── build.gradle.kts                     # Dependencies
```

## Building the App

### Prerequisites

1. **Android Studio**: Arctic Fox or newer
2. **Android SDK**: API 34
3. **Gradle**: 8.2+

### Build Steps

1. **Clone the repository** (or use the existing project):
   ```bash
   cd /Users/0xanyi/Developer/tvapp
   ```

2. **Open in Android Studio**:
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the project directory

3. **Sync Gradle**:
   - Android Studio will automatically sync Gradle
   - Wait for dependencies to download

4. **Build the APK**:
   ```bash
   ./gradlew assembleDebug
   ```
   - Output: `app/build/outputs/apk/debug/app-debug.apk`

5. **Build Release APK** (for production):
   ```bash
   ./gradlew assembleRelease
   ```
   - Output: `app/build/outputs/apk/release/app-release.apk`

## Installing on Android TV

### Method 1: ADB (Recommended)

1. **Enable Developer Options** on your TV:
   - Go to Settings → About → Build Number
   - Click 7 times to enable developer mode

2. **Enable ADB Debugging**:
   - Settings → Developer Options → USB Debugging → On

3. **Connect via ADB**:
   ```bash
   adb connect <TV_IP_ADDRESS>:5555
   ```

4. **Install the APK**:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

### Method 2: USB Drive

1. Copy the APK to a USB drive
2. Use a file manager app on TV to locate and install the APK
3. May require enabling "Install from Unknown Sources"

### Method 3: Direct from Android Studio

1. Connect TV via ADB
2. Select your TV device in Android Studio
3. Click Run (▶️)

## Configuration

### Changing the Stream URL

Edit [`app/src/main/java/com/nmtv/app/data/repository/LocalChannelRepository.kt`](app/src/main/java/com/nmtv/app/data/repository/LocalChannelRepository.kt:11):

```kotlin
private val channels = listOf(
    Channel(
        id = "nmtv_uk",
        name = "NMTV UK",
        streamUrl = "YOUR_HLS_STREAM_URL_HERE.m3u8",
        isDefault = true
    )
)
```

### Adding More Channels

To add a second channel for future channel switching:

```kotlin
private val channels = listOf(
    Channel(
        id = "nmtv_uk",
        name = "NMTV UK",
        streamUrl = "https://cdn3.wowza.com/5/L1Uzd2FrbVlLRG1W/live/smil:nmtvuk.smil/playlist.m3u8",
        isDefault = true
    ),
    Channel(
        id = "channel_2",
        name = "Channel 2",
        streamUrl = "https://your-stream-url.m3u8",
        isDefault = false
    )
)
```

Use **D-pad Up/Down** on remote to switch channels.

### Customizing App Name

Edit [`app/src/main/res/values/strings.xml`](app/src/main/res/values/strings.xml:3):

```xml
<string name="app_name">Your App Name</string>
```

### Customizing App Banner

Replace [`app/src/main/res/drawable/app_banner.xml`](app/src/main/res/drawable/app_banner.xml) with a PNG image:
- **Dimensions**: 320x180 pixels
- **Format**: PNG or JPG
- **Location**: `app/src/main/res/drawable/app_banner.png`

### Customizing App Icon

Replace placeholder launcher icons in `app/src/main/res/mipmap-*/` with PNG icons:
- **hdpi**: 72x72 px
- **mdpi**: 48x48 px
- **xhdpi**: 96x96 px
- **xxhdpi**: 144x144 px
- **xxxhdpi**: 192x192 px

## Remote Control Keys

| Remote Button | Action |
|---------------|--------|
| **D-pad Center** | Toggle play/pause |
| **Play/Pause** | Toggle play/pause |
| **Back** | Exit app |
| **D-pad Up** | Switch to next channel (if multiple configured) |
| **D-pad Down** | Switch to previous channel (if multiple configured) |

## Troubleshooting

### App Won't Install

- Ensure "Install from Unknown Sources" is enabled in TV settings
- Check ADB connection: `adb devices`
- Try uninstalling previous version: `adb uninstall com.nmtv.app`

### Stream Won't Play

- Verify HLS URL works in a browser or VLC
- Check internet connection on TV
- Review logs: `adb logcat -s StreamPlayer MainActivity`
- Ensure URL is HTTPS (not HTTP) for Android 9+

### Black Screen on Launch

- Check if stream URL is valid
- Look for errors in logcat: `adb logcat -s StreamPlayer`
- Verify TV has internet access

### Buffering Issues

- Check internet speed (recommend 5+ Mbps for HD)
- Try lower quality stream if available
- Check router/network stability

## Architecture

This app follows a clean architecture pattern:

- **Data Layer**: Channel model and repository
- **Player Layer**: ExoPlayer wrapper with retry logic
- **UI Layer**: Single fullscreen activity

See [`ARCHITECTURE.md`](ARCHITECTURE.md) for detailed technical documentation.

## Key Technologies

- **Kotlin**: Modern Android development language
- **ExoPlayer (Media3)**: Google's media player library
- **Leanback**: Android TV UI library
- **HLS**: HTTP Live Streaming protocol

## Performance Considerations

- **Memory**: App uses ~50-100MB during playback
- **CPU**: Efficient video decoding with hardware acceleration
- **Network**: Adaptive bitrate adjusts to connection speed
- **Battery**: Wake lock keeps screen on during playback

## Google Play TV Requirements

This app meets all Google Play for TV requirements:

- ✅ Leanback library integration
- ✅ TV launcher intent filter
- ✅ TV banner (320x180)
- ✅ No touchscreen requirement
- ✅ D-pad navigation support
- ✅ Landscape orientation
- ✅ No mobile-only features

## Future Enhancements

Planned features for future versions:

1. **Multiple Channels**: Channel guide with EPG
2. **Favorites**: Mark favorite channels
3. **Settings**: Stream quality preference
4. **DVR**: Record and playback functionality
5. **Subtitles**: Closed caption support

## License

This project is provided as-is for your use. Modify as needed for your requirements.

## Support

For issues or questions:
1. Check logs: `adb logcat -s StreamPlayer MainActivity`
2. Review architecture documentation
3. Verify stream URL and network connectivity

## Development

### Running Tests

```bash
./gradlew test
```

### Code Style

This project follows [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html).

### Contributing

1. Follow existing code structure
2. Add tests for new features
3. Update documentation
4. Test on real Android TV hardware

---

**Built for Android TV** 📺