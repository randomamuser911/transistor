/*
 * EqualizerManager.kt
 * Implements the EqualizerManager object
 * EqualizerManager owns the Equalizer audio effect attached to the player's audio session
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-26 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers

import android.media.audiofx.Equalizer
import android.util.Log


/*
 * EqualizerManager object
 */
object EqualizerManager {

    /* Define log tag */
    private val TAG: String = EqualizerManager::class.java.simpleName


    /* Main class variables */
    var equalizer: Equalizer? = null
        private set


    /* Attaches a new equalizer to the given audio session and applies saved settings */
    fun attach(audioSessionId: Int) {
        release()
        try {
            val newEqualizer: Equalizer = Equalizer(0, audioSessionId)
            newEqualizer.enabled = PreferencesHelper.loadEqualizerEnabled()
            val savedBandLevels: List<Int> = PreferencesHelper.loadEqualizerBandLevels()
            if (savedBandLevels.size == newEqualizer.numberOfBands.toInt()) {
                savedBandLevels.forEachIndexed { band, level ->
                    newEqualizer.setBandLevel(band.toShort(), level.toShort())
                }
            }
            equalizer = newEqualizer
        } catch (e: Exception) {
            Log.e(TAG, "Unable to attach equalizer to audio session $audioSessionId")
        }
    }


    /* Releases the equalizer */
    fun release() {
        try {
            equalizer?.release()
        } catch (e: Exception) {
            Log.v(TAG, "Unable to release equalizer")
        }
        equalizer = null
    }

}
