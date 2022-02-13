/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.opencv

import android.graphics.Bitmap

class NativeLib : AutoCloseable {

    companion object {
        // Used to load the 'smartautoclicker' library on application startup.
        init {
            System.loadLibrary("smartautoclicker")
        }
    }

    private val nativePtr: Long = newDetector()

    override fun close() = deleteDetector()

    private external fun newDetector(): Long

    private external fun deleteDetector()

    external fun setScreenImage(screenBitmap: Bitmap)

    external fun detectCondition(conditionBitmap: Bitmap, threshold: Double): Boolean
}