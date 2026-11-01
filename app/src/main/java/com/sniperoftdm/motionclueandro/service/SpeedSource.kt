package com.sniperoftdm.motionclueandro.service

/**
 * Interface for GPS fix data providers.
 * Renamed from speed-only to fix (speed + bearing) for contract v2.
 */
interface SpeedSource {
    fun start()
    fun stop()
}
