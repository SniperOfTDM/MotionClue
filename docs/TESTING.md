# Testing

MotionClue has no automated test suite. All testing is manual, on a physical device.

## Test device

| Field | Value |
|-------|-------|
| Model | Poco X7 Pro |
| SoC | MediaTek Dimensity 8400-Ultra |
| ROM | HyperOS (MIUI-based) |
| Android | 15 (API 35) |

## Manual checklist

Run through this list before each release.

### Startup

- [ ] App opens without crash
- [ ] Overlay permission prompt appears if not granted
- [ ] Start button launches the foreground service
- [ ] Notification appears with "Stop" action
- [ ] Status line shows "sensors: running", "engine: native"

### Overlay behavior

- [ ] Dots appear on left and right screen edges
- [ ] Dots shift sideways when phone tilts or vehicle accelerates
- [ ] Dots flow vertically when vehicle is moving (GPS enabled)
- [ ] Dots stop flowing when vehicle is stationary
- [ ] Overlay is click-through (can interact with apps underneath)
- [ ] Overlay covers display cutout area

### Settings

- [ ] Sensitivity slider changes dot responsiveness in real time
- [ ] Dot size, color, opacity, and count update live
- [ ] Edge band width changes dot column position
- [ ] Invert direction toggle reverses dot movement
- [ ] GPS toggle enables/disables flow
- [ ] Adaptive brightness dims dots in the dark

### Lifecycle

- [ ] Screen off stops sensor thread and rendering
- [ ] Screen on resumes sensor thread and rendering
- [ ] Stop button kills the service and removes overlay
- [ ] Quick Settings tile toggles the service
- [ ] Rotation change updates display rotation config
- [ ] Killing the app via recents stops the service

### Environmental sensors

- [ ] Proximity near hides overlay dots
- [ ] Proximity far shows overlay dots again
- [ ] Ambient lux adapts dot opacity (if adaptive brightness on)

### Edge cases

- [ ] Engine falls back to stub if native library is unavailable
- [ ] Service handles location permission revoked at runtime
- [ ] "Copy diagnostics" includes sensor inventory and status

## Known limitations

- Tested primarily on one device (Poco X7 Pro)
- Flow direction is currently fixed, not learned from vehicle heading
- Battery impact has not been formally measured
- No automated UI or integration tests exist
