#include "SensorInventory.h"
#include "Config.h"
#include <sstream>

namespace motionclue {

void SensorInventory::discover(ASensorManager* manager) {
    if (!manager) return;

    ASensorList list;
    int count = ASensorManager_getSensorList(manager, &list);

    std::ostringstream desc;
    desc << "Device sensors (" << count << " total):\n";

    for (int i = 0; i < count; i++) {
        const ASensor* s = list[i];
        int type        = ASensor_getType(s);
        const char* nm  = ASensor_getName(s);
        const char* vn  = ASensor_getVendor(s);
        int minDelayUs  = ASensor_getMinDelay(s);
        float maxHz     = (minDelayUs > 0) ? 1000000.0f / static_cast<float>(minDelayUs) : 0.0f;
        bool wakeup     = ASensor_isWakeUpSensor(s);

        desc << "  type=" << type
             << " name=\"" << (nm ? nm : "?")
             << "\" vendor=\"" << (vn ? vn : "?")
             << "\" maxRate=" << maxHz << "Hz"
             << " wakeup=" << (wakeup ? "yes" : "no") << "\n";
    }

    accelerometer      = ASensorManager_getDefaultSensor(manager, SENSOR_TYPE_ACCELEROMETER);
    gyroscope          = ASensorManager_getDefaultSensor(manager, SENSOR_TYPE_GYROSCOPE);
    magnetometer       = ASensorManager_getDefaultSensor(manager, SENSOR_TYPE_MAGNETIC_FIELD);
    gravity            = ASensorManager_getDefaultSensor(manager, SENSOR_TYPE_GRAVITY);
    linearAccel        = ASensorManager_getDefaultSensor(manager, SENSOR_TYPE_LINEAR_ACCELERATION);
    rotationVector     = ASensorManager_getDefaultSensor(manager, SENSOR_TYPE_ROTATION_VECTOR);
    gameRotationVector = ASensorManager_getDefaultSensor(manager, SENSOR_TYPE_GAME_ROTATION_VECTOR);
    pressure           = ASensorManager_getDefaultSensor(manager, SENSOR_TYPE_PRESSURE);
    light              = ASensorManager_getDefaultSensor(manager, SENSOR_TYPE_LIGHT);
    proximity          = ASensorManager_getDefaultSensor(manager, SENSOR_TYPE_PROXIMITY);
    stepDetector       = ASensorManager_getDefaultSensor(manager, SENSOR_TYPE_STEP_DETECTOR);

    sensorMask = 0;
    if (accelerometer)      sensorMask |= (1u << MASK_BIT_ACCEL);
    if (gyroscope)          sensorMask |= (1u << MASK_BIT_GYRO);
    if (magnetometer)       sensorMask |= (1u << MASK_BIT_MAG);
    if (gravity)            sensorMask |= (1u << MASK_BIT_GRAVITY_HW);
    if (linearAccel)        sensorMask |= (1u << MASK_BIT_LINEAR_HW);
    if (rotationVector)     sensorMask |= (1u << MASK_BIT_ROT_VEC);
    if (gameRotationVector) sensorMask |= (1u << MASK_BIT_GAME_ROT_VEC);
    if (pressure)           sensorMask |= (1u << MASK_BIT_PRESSURE);
    if (light)              sensorMask |= (1u << MASK_BIT_LIGHT);
    if (proximity)          sensorMask |= (1u << MASK_BIT_PROXIMITY);
    if (stepDetector)       sensorMask |= (1u << MASK_BIT_STEP_DETECTOR);

    desc << "Engine uses:";
    if (accelerometer)      desc << " accel";
    if (gyroscope)          desc << " gyro";
    if (gameRotationVector) desc << " gameRotVec";
    if (rotationVector)     desc << " rotVec(diag)";
    if (magnetometer)       desc << " mag(diag)";
    if (gravity)            desc << " gravity(hw,cmp)";
    if (linearAccel)        desc << " linAccel(hw,cmp)";
    if (stepDetector)       desc << " stepDet";
    if (light)              desc << " light";
    if (proximity)          desc << " proximity";
    if (pressure)           desc << " pressure";
    desc << "\n";

    describeText = desc.str();
}

} // namespace motionclue
