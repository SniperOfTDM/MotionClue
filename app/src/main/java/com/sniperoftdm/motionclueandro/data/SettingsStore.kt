// com/sniperoftdm/motionclueandro/data/SettingsStore.kt
package com.sniperoftdm.motionclueandro.data

import android.content.Context
import android.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class TransportPreset(
    val title: String,
    val sensitivity: Float,
    val hybridBlend: Float,
) {
    CAR("Car", 1.0f, 0.5f),
    BUS("Bus / Metro", 1.4f, 0.35f),
    TRAIN("Train", 0.6f, 0.75f),
    BOAT("Boat / Ferry", 1.8f, 0.20f),
    CUSTOM("Custom", 1.0f, 0.5f);
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "motion_settings")

/**
 * Persisted user settings via DataStore Preferences.
 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val PRESET = stringPreferencesKey("transport_preset")
        val SENSITIVITY = floatPreferencesKey("sensitivity")
        val HYBRID_BLEND = floatPreferencesKey("hybrid_blend")
        val DOT_SIZE_DP = floatPreferencesKey("dot_size_dp")
        val OPACITY = floatPreferencesKey("opacity")
        val DOT_COLOR = stringPreferencesKey("dot_color")
        val DOT_COUNT = intPreferencesKey("dot_count")
        val EDGE_BAND_DP = floatPreferencesKey("edge_band_dp")
        val INVERT_DIRECTION = booleanPreferencesKey("invert_direction")
        val USE_GPS = booleanPreferencesKey("use_gps")
        val ADAPTIVE_BRIGHTNESS = booleanPreferencesKey("adaptive_brightness")
    }

    data class Settings(
        val presetName: String = TransportPreset.CAR.name,
        val sensitivity: Float = 1.0f,
        val hybridBlend: Float = 0.5f,
        val dotSizeDp: Float = 6f,
        val opacity: Float = 0.7f,
        val dotColorName: String = "White",
        val dotCount: Int = 10,
        val edgeBandDp: Float = 28f,
        val invertDirection: Boolean = true,
        val useGps: Boolean = true,
        val adaptiveBrightness: Boolean = true,
    ) {
        val dotColorInt: Int
            get() = when (dotColorName) {
                "Black" -> Color.BLACK
                "Yellow" -> Color.YELLOW
                "Gray" -> Color.GRAY
                else -> Color.WHITE
            }
    }

    val settingsFlow: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            presetName = prefs[Keys.PRESET] ?: TransportPreset.CAR.name,
            sensitivity = prefs[Keys.SENSITIVITY] ?: 1.0f,
            hybridBlend = prefs[Keys.HYBRID_BLEND] ?: 0.5f,
            dotSizeDp = prefs[Keys.DOT_SIZE_DP] ?: 6f,
            opacity = prefs[Keys.OPACITY] ?: 0.7f,
            dotColorName = prefs[Keys.DOT_COLOR] ?: "White",
            dotCount = prefs[Keys.DOT_COUNT] ?: 10,
            edgeBandDp = prefs[Keys.EDGE_BAND_DP] ?: 28f,
            invertDirection = prefs[Keys.INVERT_DIRECTION] ?: true,
            useGps = prefs[Keys.USE_GPS] ?: true,
            adaptiveBrightness = prefs[Keys.ADAPTIVE_BRIGHTNESS] ?: true,
        )
    }

    suspend fun applyPreset(preset: TransportPreset) {
        context.dataStore.edit {
            it[Keys.PRESET] = preset.name
            if (preset != TransportPreset.CUSTOM) {
                it[Keys.SENSITIVITY] = preset.sensitivity
                it[Keys.HYBRID_BLEND] = preset.hybridBlend
            }
        }
    }

    suspend fun updateSensitivity(value: Float) {
        context.dataStore.edit {
            it[Keys.SENSITIVITY] = value.coerceIn(0.2f, 3.0f)
            it[Keys.PRESET] = TransportPreset.CUSTOM.name
        }
    }

    suspend fun updateHybridBlend(value: Float) {
        context.dataStore.edit {
            it[Keys.HYBRID_BLEND] = value.coerceIn(0f, 1f)
            it[Keys.PRESET] = TransportPreset.CUSTOM.name
        }
    }

    suspend fun updateDotSizeDp(value: Float) {
        context.dataStore.edit { it[Keys.DOT_SIZE_DP] = value.coerceIn(3f, 12f) }
    }

    suspend fun updateOpacity(value: Float) {
        context.dataStore.edit { it[Keys.OPACITY] = value.coerceIn(0.2f, 1f) }
    }

    suspend fun updateDotColor(name: String) {
        context.dataStore.edit { it[Keys.DOT_COLOR] = name }
    }

    suspend fun updateDotCount(value: Int) {
        context.dataStore.edit { it[Keys.DOT_COUNT] = value.coerceIn(6, 20) }
    }

    suspend fun updateEdgeBandDp(value: Float) {
        context.dataStore.edit { it[Keys.EDGE_BAND_DP] = value.coerceIn(16f, 64f) }
    }

    suspend fun updateInvertDirection(value: Boolean) {
        context.dataStore.edit { it[Keys.INVERT_DIRECTION] = value }
    }

    suspend fun updateUseGps(value: Boolean) {
        context.dataStore.edit { it[Keys.USE_GPS] = value }
    }

    suspend fun updateAdaptiveBrightness(value: Boolean) {
        context.dataStore.edit { it[Keys.ADAPTIVE_BRIGHTNESS] = value }
    }
}
