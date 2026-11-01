#pragma once
#include <cstdint>
#include <cstddef>
#include <cstring>

namespace motionclue {

// Contract v2 buffer layout, slots 2-31 (120 bytes).
// Written only on the sensor thread; Kotlin reads via seqlock.
struct FrameData {
    int32_t  flags          = 0;       // slot 2
    float    shiftX         = 0.0f;    // slot 3
    float    shiftY         = 0.0f;    // slot 4
    float    flowDirX       = 0.0f;    // slot 5
    float    flowDirY       = 1.0f;    // slot 6
    float    flowRate       = 0.0f;    // slot 7
    float    speedMps       = -1.0f;   // slot 8
    float    longAccel      = 0.0f;    // slot 9
    float    latAccel       = 0.0f;    // slot 10
    float    yawRate        = 0.0f;    // slot 11
    float    confidence     = 1.0f;    // slot 12
    int32_t  sensorMask     = 0;       // slot 13
    int64_t  timestampNanos = 0;       // slots 14-15 (byte offset 56, 8-aligned)
    float    ambientLux     = -1.0f;   // slot 16
    float    proximityCm    = -1.0f;   // slot 17
    float    pressureHpa    = -1.0f;   // slot 18
    float    climbRate      = 0.0f;    // slot 19
    float    headingRad     = -1.0f;   // slot 20
    float    tiltDeg        = 0.0f;    // slot 21
    float    viewBlend      = 0.0f;    // slot 22
    float    stationarySec  = 0.0f;    // slot 23
    float    measuredRateHz = 0.0f;    // slot 24
    int32_t  droppedSamples = 0;       // slot 25
    int32_t  reserved[6]    = {};      // slots 26-31
};

static_assert(sizeof(FrameData) == 120, "FrameData must be 120 bytes (slots 2-31)");
static_assert(offsetof(FrameData, timestampNanos) == 48,
              "timestampNanos at struct offset 48 -> buffer byte offset 56");

constexpr size_t BUFFER_SIZE    = 128;
constexpr int32_t BUFFER_VERSION = 2;
constexpr size_t OFF_VERSION    = 0;
constexpr size_t OFF_SEQUENCE   = 4;
constexpr size_t OFF_DATA       = 8;

class SharedFrame {
public:
    bool init(uint8_t* buffer, size_t capacity);
    void write(const FrameData& data);
    void disable();
    bool valid() const { return buf_ != nullptr; }
private:
    uint8_t* buf_ = nullptr;
    uint32_t sequence_ = 0;
    bool enabled_ = false;
};

} // namespace motionclue
