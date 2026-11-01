#pragma once
#include <android/sensor.h>
#include <cstdint>
#include <string>

namespace motionclue {

class SensorInventory {
public:
    void discover(ASensorManager* manager);

    const ASensor* accelerometer      = nullptr;
    const ASensor* gyroscope          = nullptr;
    const ASensor* magnetometer       = nullptr;
    const ASensor* gravity            = nullptr;
    const ASensor* linearAccel        = nullptr;
    const ASensor* rotationVector     = nullptr;
    const ASensor* gameRotationVector = nullptr;
    const ASensor* pressure           = nullptr;
    const ASensor* light              = nullptr;
    const ASensor* proximity          = nullptr;
    const ASensor* stepDetector       = nullptr;

    uint32_t    sensorMask = 0;
    std::string describeText;

    bool hasAccelerometer() const { return accelerometer != nullptr; }
    bool hasGyroscope()     const { return gyroscope != nullptr; }
};

} // namespace motionclue
