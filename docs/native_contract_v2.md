# Native Contract v2

(Identical in all prompts. If anything conflicts with it, stop and report the conflict. Contract version: 2.)

Library: libmotionclue_native.so, loaded with System.loadLibrary("motionclue_native").

JNI class: com.sniperoftdm.motionclueandro.bridge.NativeMotionEngine (normal class; external methods are instance methods; handle passed explicitly).

| Kotlin signature | JNI descriptor |
|---|---|
| nativeCreate(buffer: java.nio.ByteBuffer): Long | (Ljava/nio/ByteBuffer;)J |
| nativeStart(handle: Long): Boolean | (J)Z |
| nativeStop(handle: Long) | (J)V |
| nativeSetFix(handle: Long, speedMps: Float, speedAccMps: Float, bearingDeg: Float, bearingAccDeg: Float, elapsedRealtimeNanos: Long) | (JFFFFJ)V |
| nativeSetConfig(handle: Long, params: FloatArray) | (J[F)V |
| nativeResetCalibration(handle: Long) | (J)V |
| nativeDescribeSensors(handle: Long): String | (J)Ljava/lang/String; |
| nativeDestroy(handle: Long) | (J)V |

Rules: nativeCreate returns 0 on failure; every other method is a no-op for handle 0 (nativeDescribeSensors returns ""). Start/Stop idempotent; Stop returns only after the native sensor thread has stopped. Destroy implies Stop. speedMps < 0 = speed unavailable; bearingDeg < 0 = bearing unavailable (valid range 0..360). nativeDescribeSensors returns plain text, one line per sensor the device exposes (type, name, vendor, min/max rate, wake-up) plus a line saying which ones the engine uses; it may be called before nativeStart. Native code never calls back into Kotlin.

Shared buffer: direct ByteBuffer, capacity exactly 128 bytes, ByteOrder.nativeOrder(), allocated by Kotlin and kept referenced until nativeDestroy returns. Byte offset = 4 x slot. Native writes with memcpy; Kotlin reads with absolute-offset getters.

| Slot | Type | Name | Meaning |
|---|---|---|---|
| 0 | int32 | version | always 2 |
| 1 | uint32 | sequence | seqlock: odd while native writes, even when stable |
| 2 | int32 | flags | bit0 sensors running, bit1 flow direction learned (0 while direction is fixed), bit2 speed valid, bit3 sensor error, bit4 gravity settled, bit5 stationary, bit6 walking, bit7 proximity near |
| 3 | float | shiftX | acceleration shift, view frame X (right), -1..1 |
| 4 | float | shiftY | acceleration shift, view frame Y (forward = screen-up), -1..1 |
| 5 | float | flowDirX | unit vector of flow direction in view frame |
| 6 | float | flowDirY | same, Y |
| 7 | float | flowRate | pattern units/s (1.0 = one dot spacing), >= 0, already weighted |
| 8 | float | speedMps | fused speed, -1 if unknown |
| 9 | float | longAccel | m/s2, debug |
| 10 | float | latAccel | m/s2, debug |
| 11 | float | yawRate | rad/s, debug |
| 12 | float | confidence | 0..1 flow-direction confidence (1.0 while direction is fixed) |
| 13 | int32 | sensorMask | bit0 accel, 1 gyro, 2 magnetometer, 3 gravity(HW), 4 linear accel(HW), 5 rotation vector, 6 game rotation vector, 7 pressure, 8 light, 9 proximity, 10 step detector; set = delivering events |
| 14-15 | int64 | timestampNanos | elapsedRealtimeNanos of last native update (byte offset 56, 8-aligned) |
| 16 | float | ambientLux | -1 if no sensor |
| 17 | float | proximityCm | -1 if no sensor; 0 = near |
| 18 | float | pressureHpa | -1 if no sensor |
| 19 | float | climbRate | m/s from pressure, 0 if none |
| 20 | float | headingRad | compass heading of view-forward, 0 = north, clockwise; -1 unknown |
| 21 | float | tiltDeg | angle between screen normal and vertical |
| 22 | float | viewBlend | 0 = upright (gaze along -Z), 1 = flat (gaze along screen-up) |
| 23 | float | stationarySec | seconds stationary |
| 24 | float | measuredRateHz | measured accelerometer rate |
| 25 | int32 | droppedSamples | count since start |
| 26-31 | | reserved | 0 |

View frame: X = to the user's right, Y = away from the user along the line of sight (drawn as screen-up in the current display orientation). Kotlin passes display rotation in config slot 7 and does no other rotation math.

Seqlock read (Kotlin): read sequence s1; if odd retry; copy all fields; read sequence s2; accept only if s1 == s2; after 3 failed attempts reuse the previous frame.

Config array for nativeSetConfig (FloatArray, length 12):

| 0 sensitivity 1.0 (0.2-3.0) | 1 hybridBlend 0.5 (0-1) | 2 springStiffness 18.0 | 3 springDamping 6.0 | 4 referenceAccel m/s2 3.0 | 5 flowGain 0.5 | 6 deadZone m/s2 0.15 | 7 displayRotation 0 (0-3 = Surface.ROTATION_0/90/180/270) | 8 flowFloorKmh 5.0 | 9 flowCapUnits 4.0 | 10 useMagnetometer 1 (0/1) | 11 sampleRateHz 100 (50-200; native clamps to 200 so no HIGH_SAMPLING_RATE_SENSORS permission is needed) |

Hybrid weights (native): accelWeight = min(1, 2*(1-hybridBlend)); flowWeight = min(1, 2*hybridBlend). If speed is invalid or confidence < 0.3, flowWeight = 0 and accelWeight = 1.
