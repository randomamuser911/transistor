/*
 * ImageHelper.kt
 * Implements the ImageHelper object
 * An ImageHelper provides helper methods for bitmap processing
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-26 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log


/*
 * ImageHelper object
 */
object ImageHelper {

    /* Define log tag */
    private val TAG: String = ImageHelper::class.java.simpleName


    /* Decodes a downscaled version of an image file */
    fun decodeSampledBitmap(imagePath: String, targetWidth: Int): Bitmap? {
        return try {
            // read the dimensions first
            val bounds: BitmapFactory.Options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(imagePath, bounds)
            var sampleSize: Int = 1
            while (bounds.outWidth / (sampleSize * 2) >= targetWidth) {
                sampleSize *= 2
            }
            // decode the small version
            val options: BitmapFactory.Options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            BitmapFactory.decodeFile(imagePath, options)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to decode image file $imagePath")
            null
        }
    }


    /* Blurs a (small) bitmap and returns the blurred copy using a fast box blur */
    fun boxBlur(source: Bitmap, radius: Int, passes: Int = 2): Bitmap {
        val width: Int = source.width
        val height: Int = source.height
        val pixels: IntArray = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        repeat(passes) {
            boxBlurHorizontal(pixels, width, height, radius)
            boxBlurVertical(pixels, width, height, radius)
        }
        // decoded bitmaps are immutable - write the result into a mutable bitmap
        val blurred: Bitmap = if (source.isMutable) source else Bitmap.createBitmap(width, height, source.config ?: Bitmap.Config.ARGB_8888)
        blurred.setPixels(pixels, 0, width, 0, 0, width, height)
        return blurred
    }


    /* Horizontal pass of the box blur */
    private fun boxBlurHorizontal(pixels: IntArray, width: Int, height: Int, radius: Int) {
        val windowSize: Int = radius * 2 + 1
        val row: IntArray = IntArray(width)
        for (y: Int in 0 until height) {
            // copy the row once
            for (x: Int in 0 until width) {
                row[x] = pixels[y * width + x]
            }
            var sumRed: Int = 0
            var sumGreen: Int = 0
            var sumBlue: Int = 0
            var sumAlpha: Int = 0
            // initial window (clamped at the left edge)
            for (k: Int in -radius..radius) {
                val clamped: Int = row[k.coerceIn(0, width - 1)]
                sumRed += (clamped shr 16) and 0xFF
                sumGreen += (clamped shr 8) and 0xFF
                sumBlue += clamped and 0xFF
                sumAlpha += (clamped shr 24) and 0xFF
            }
            for (x: Int in 0 until width) {
                pixels[y * width + x] = (sumAlpha / windowSize shl 24) or (sumRed / windowSize shl 16) or (sumGreen / windowSize shl 8) or (sumBlue / windowSize)
                // slide the window: add the pixel entering on the right, remove the one leaving on the left
                val entering: Int = row[(x + radius + 1).coerceAtMost(width - 1)]
                val leaving: Int = row[(x - radius).coerceAtLeast(0)]
                sumRed += ((entering shr 16) and 0xFF) - ((leaving shr 16) and 0xFF)
                sumGreen += ((entering shr 8) and 0xFF) - ((leaving shr 8) and 0xFF)
                sumBlue += (entering and 0xFF) - (leaving and 0xFF)
                sumAlpha += ((entering shr 24) and 0xFF) - ((leaving shr 24) and 0xFF)
            }
        }
    }


    /* Vertical pass of the box blur */
    private fun boxBlurVertical(pixels: IntArray, width: Int, height: Int, radius: Int) {
        val windowSize: Int = radius * 2 + 1
        val column: IntArray = IntArray(height)
        for (x: Int in 0 until width) {
            for (y: Int in 0 until height) {
                column[y] = pixels[y * width + x]
            }
            var sumRed: Int = 0
            var sumGreen: Int = 0
            var sumBlue: Int = 0
            var sumAlpha: Int = 0
            for (k: Int in -radius..radius) {
                val clamped: Int = column[k.coerceIn(0, height - 1)]
                sumRed += (clamped shr 16) and 0xFF
                sumGreen += (clamped shr 8) and 0xFF
                sumBlue += clamped and 0xFF
                sumAlpha += (clamped shr 24) and 0xFF
            }
            for (y: Int in 0 until height) {
                pixels[y * width + x] = (sumAlpha / windowSize shl 24) or (sumRed / windowSize shl 16) or (sumGreen / windowSize shl 8) or (sumBlue / windowSize)
                val entering: Int = column[(y + radius + 1).coerceAtMost(height - 1)]
                val leaving: Int = column[(y - radius).coerceAtLeast(0)]
                sumRed += ((entering shr 16) and 0xFF) - ((leaving shr 16) and 0xFF)
                sumGreen += ((entering shr 8) and 0xFF) - ((leaving shr 8) and 0xFF)
                sumBlue += (entering and 0xFF) - (leaving and 0xFF)
                sumAlpha += ((entering shr 24) and 0xFF) - ((leaving shr 24) and 0xFF)
            }
        }
    }

}
