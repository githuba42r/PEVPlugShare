# AR Socket Finder Feature Documentation

## Overview

The AR Socket Finder is an augmented reality feature that helps users locate charging sockets with pixel-accurate precision. It uses ARCore's Geospatial API to place a glowing target circle on the exact real-world location of a charging socket, even when it's obscured, hidden, or mounted in non-obvious places.

## Features

### Phase 1 - MVP (Implemented)

- **ARCore Geospatial API Integration**: Uses Google's Geospatial API for outdoor positioning with 30cm accuracy
- **Glowing Target Circle**: Pulsing green circle appears on the socket's real-world location
- **Distance & Height Indicators**: Shows distance to socket and vertical offset (above/below)
- **Off-Screen Arrow**: Directional arrow when socket is outside camera field of view
- **Compass Fallback Mode**: Graceful degradation to compass-based navigation on non-AR devices
- **Automatic Mode Switching**: Toggle between AR and compass modes

### Key Components

#### 1. Data Models

**ArSocketData** (`data/model/ArSocketData.kt`)
- Stores 3D offset data in East-North-Up (ENU) coordinate system
- Tracks camera position and orientation when photo was taken
- Supports multiple localization methods (GPS+Bearing, Geospatial API, VPS, Photo Matching)

**PhotoMetadata** (Extended)
- Added `pitch`: Camera pitch angle for AR calculations
- Added `distanceEstimate`: User-estimated distance to socket

**Location Model** (Extended)
- Added `arSocketData`: Optional AR positioning data field

#### 2. AR Utilities

**ArCoordinateUtils** (`ar/ArCoordinateUtils.kt`)
- Geographic ↔ ENU coordinate conversion
- Bearing and distance calculations
- 3D distance with altitude support
- Display formatting for distances and heights

**ArCoreHelper** (`ar/ArCoreHelper.kt`)
- ARCore session lifecycle management
- Geospatial API integration
- Anchor creation and tracking
- Device compatibility checking
- Installation prompts

**ArOverlayView** (`ar/ArOverlayView.kt`)
- Custom view for AR overlay rendering
- Pulsing glowing circle effect
- Off-screen directional arrows
- Distance and height text display

#### 3. Activity

**ArSocketFinderActivity** (`ar/ArSocketFinderActivity.kt`)
- Main AR view activity
- Dual mode support (AR / Compass)
- Real-time frame updates (~30 fps)
- Sensor fusion for compass mode
- Location tracking integration

## User Flow

1. User browses location list in MainActivity
2. Taps AR button (green eye icon) on a location card
3. ArSocketFinderActivity launches
4. System checks for ARCore support and camera permission
5. If supported: Initializes AR with geospatial tracking
6. If not supported: Falls back to compass mode
7. User sees:
   - **AR Mode**: Live camera view with glowing target circle on socket
   - **Compass Mode**: Directional arrow and distance to socket
8. User can toggle between modes using button at bottom
9. Distance and height information updates in real-time

## Requirements

### Minimum Requirements
- Android SDK 26+ (Android 8.0 Oreo)
- Camera permission
- Location permission (fine)
- Accelerometer sensor
- Magnetometer sensor

### AR Mode Requirements
- ARCore-compatible device (95%+ of flagships 2022+)
- Google Play Services for AR
- Internet connection (for Geospatial API)
- Outdoor or VPS-covered indoor location

### Graceful Degradation
- Non-AR devices automatically use compass mode
- Failed ARCore initialization falls back to compass
- User can manually switch between modes

## Permissions

Already present in AndroidManifest.xml:
- `CAMERA` - For AR camera view
- `ACCESS_FINE_LOCATION` - For GPS positioning
- `INTERNET` - For Geospatial API

## Implementation Details

### Coordinate System

The system uses **ENU (East-North-Up)** local coordinate system:
- **East**: Positive X axis pointing east
- **North**: Positive Y axis pointing north  
- **Up**: Positive Z axis pointing up (altitude)

When a photo is taken with AR data:
1. Camera GPS position is recorded (lat, lon, alt)
2. Camera bearing (azimuth) and pitch are recorded
3. User estimates distance to socket
4. System calculates 3D offset in ENU coordinates
5. Offset is stored with location for future AR sessions

### AR Rendering Pipeline

```
Frame Update Loop (30 fps):
  ├─ Update ARCore session
  ├─ Get current camera pose
  ├─ Check geospatial tracking state
  ├─ Get socket anchor pose
  ├─ Project anchor to screen coordinates
  ├─ Update overlay view
  │   ├─ Draw glowing circle (if on-screen)
  │   ├─ Draw directional arrow (if off-screen)
  │   └─ Draw distance/height text
  └─ Render frame
```

### Compass Mode

Fallback mode using device sensors:
- Accelerometer + Magnetometer → Device orientation
- GPS → Current user position
- Calculate bearing to target location
- Rotate arrow to point toward target
- Display distance and cardinal direction

## Configuration

Currently no user-configurable settings. Future additions:
- Enable/disable AR mode preference
- AR circle color/style preference
- Voice guidance toggle
- Auto-calibration settings

## Performance

- **Target Frame Rate**: 30 fps (capped for battery life)
- **Battery Impact**: Medium (AR camera + sensors)
- **Network Usage**: Low (Geospatial API queries only during initialization)
- **Accuracy**: 
  - Outdoors: ±30cm horizontal, ±50cm vertical
  - VPS areas: ±15cm horizontal, ±20cm vertical
  - Compass mode: ±2-5m (depends on GPS accuracy)

## Known Limitations

1. **Geospatial API Coverage**: Not all areas have coverage (mostly major cities)
2. **Indoor Performance**: Limited without VPS coverage
3. **First Lock Time**: Can take 5-20 seconds for initial geospatial lock
4. **Occlusion**: Target circle renders even when behind objects (no depth occlusion yet)
5. **Multi-Socket**: Currently shows one socket at a time (no clustering)

## Future Enhancements (Phase 2 & 3)

### Phase 2 - Precision Upgrade
- ARCore VPS integration for centimeter accuracy
- On-device photo feature matching (indoor/no-VPS)
- "Ghost photo" overlay when facing original photo direction
- Improved vertical guidance

### Phase 3 - Polish & Multi-Socket
- Socket clustering for nearby locations
- Ground-plane navigation arrows (Google Maps style)
- Depth occlusion using ARCore depth API
- Voice guidance
- AR calibration UI
- Settings panel

## Testing Recommendations

### On Emulator
- Compass mode can be tested
- AR mode will not work (requires physical device)

### On Physical Device
1. Test outdoors for best Geospatial API performance
2. Ensure good GPS signal (open sky)
3. Test mode switching
4. Verify fallback on AR-incompatible devices
5. Test with different socket distances
6. Check battery drain during extended use

### Test Scenarios
- [ ] Launch AR from location list
- [ ] ARCore initialization success
- [ ] ARCore initialization failure → compass fallback
- [ ] Toggle between AR and compass modes
- [ ] Target circle rendering on-screen
- [ ] Directional arrow when target off-screen
- [ ] Distance and height text accuracy
- [ ] Battery usage during 5-minute session
- [ ] Geospatial lock time measurement
- [ ] Indoor vs outdoor performance

## Troubleshooting

**AR Mode doesn't start**
- Check device ARCore compatibility
- Ensure Google Play Services for AR is installed
- Verify camera permission granted
- Check internet connection

**Geospatial lock taking too long**
- Move to open area with clear sky view
- Wait 20-30 seconds
- Try switching to compass mode and back
- Restart app if stuck

**Target circle not appearing**
- Check if socket has AR data stored
- Verify geospatial tracking state in status text
- Ensure socket distance is reasonable (< 100m)
- Try moving camera around to help tracking

**Compass mode arrow not moving**
- Check magnetometer calibration (wave phone in figure-8)
- Verify location permission granted
- Ensure GPS is enabled
- Try moving to different location

## Architecture

```
ArSocketFinderActivity
    │
    ├─── ArCoreHelper (ARCore session management)
    │       └─── Geospatial API
    │
    ├─── ArOverlayView (Custom rendering)
    │       ├─── Target circle
    │       ├─── Directional arrow
    │       └─── Text labels
    │
    ├─── SensorManager (Compass mode)
    │       ├─── Accelerometer
    │       └─── Magnetometer
    │
    └─── FusedLocationProviderClient (GPS)
```

## Code Examples

### Launch AR Finder from Code
```kotlin
val intent = ArSocketFinderActivity.createIntent(context, location)
startActivity(intent)
```

### Calculate ENU Offsets
```kotlin
val (offsetE, offsetN, offsetU) = ArCoordinateUtils.calculateENU(
    cameraLat = 37.7749,
    cameraLon = -122.4194,
    cameraAlt = 10.0,
    bearing = 45f,
    pitch = -10f,
    distance = 5.0f
)
```

### Create Geospatial Anchor
```kotlin
val anchor = arCoreHelper.createGeospatialAnchor(
    latitude = 37.7749,
    longitude = -122.4194,
    altitude = 10.0
)
```

## Resources

- [ARCore Geospatial API Guide](https://developers.google.com/ar/develop/geospatial)
- [ARCore Supported Devices](https://developers.google.com/ar/devices)
- [ENU Coordinate System](https://en.wikipedia.org/wiki/Local_tangent_plane_coordinates)

## Support

For issues or feature requests, please open a GitHub issue with:
- Device model and Android version
- ARCore version (from Google Play Store)
- Steps to reproduce
- Screenshots/videos if applicable
- Logcat output (filter for "ArSocketFinder" tag)
