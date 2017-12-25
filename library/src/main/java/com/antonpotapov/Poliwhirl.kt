package com.antonpotapov

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.support.annotation.ColorInt
import android.support.annotation.NonNull
import android.util.Log
import com.antonpotapov.util.ColorUtils
import com.antonpotapov.util.SizeAwareArrayList
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor

/**
 * ======================================
 * Created by awesome potapov on 20.10.17.
 * ======================================
 */
class Poliwhirl {

    interface Callback {

        /**
         * One color to rule them all...
         *
         * This color is the chosen one to be placed as a background for your picture
         */
        fun foundColor(@ColorInt color: Int)
    }

    companion object {
        private val logTag = "Poliwhirl"
        private val maxPermittedColorsSize = 32
        private val currentThreadExecutor = Executor { it.run() }
        private val asyncExecutor = Executors.newSingleThreadExecutor()
    }

    var verticalBorderSizeMul: Int = 16
        private set
    var horizontalBorderSizeMul: Int = 16
        private set
    var minAvailableDistance: Int = 20
        private set
    var accuracy: Int = 3
        private set
    private var request: Request

    init {
        request = createRequest()
    }

    /**
     * Generates color on this thread using current thread executor
     */
    @ColorInt
    fun generate(bitmap: Bitmap): Int {
        var result = 0
        generateOnExecutor(bitmap, object : Callback {
            override fun foundColor(color: Int) {
                result = color
            }
        }, currentThreadExecutor)
        return result
    }

    /**
     * Generates color in async way on the general single thread executor
     */
    fun generateAsync(@NonNull bitmap: Bitmap, @NonNull callback: Callback) {
        generateOnExecutor(bitmap, callback, asyncExecutor)
    }

    /**
     * Generates color on async executor. If [executor] is [ThreadPoolExecutor] calculations will be
     * processed in parallel on the several threads
     */
    fun generateOnExecutor(@NonNull bitmap: Bitmap, @NonNull callback: Callback,
                           @NonNull executor: Executor): Request =
            generateOnExecutor(bitmap, callback, executor, 0)

    /**
     * Generates color on provided executor. This method is the most flexible one.
     * Here you can provide [forceNumThreads] to split calculations to the separated parts.
     * It's nice to set the number of parallel threads in provided [executor]
     */
    fun generateOnExecutor(@NonNull bitmap: Bitmap, @NonNull callback: Callback,
                           @NonNull executor: Executor, forceNumThreads: Int): Request {
        if (forceNumThreads < 0) throw IllegalArgumentException("provide legal threads number")
        try {
            return request.execute(bitmap, callback, executor, forceNumThreads)
        } finally {
            request = createRequest()
        }
    }

    /**
     * Step size in pixel when processing an image.
     * For example: if [accuracy] is 3, algorithm will check every 3 pixel
     */
    fun setAccuracy(accuracy: Int): Poliwhirl {
        this.accuracy = accuracy
        request.accuracy = accuracy
        return this
    }

    /**
     * Color distance calculated by CIEDE2000 formula. Colors with delta < minAvailableDistance
     * will be processed as the same
     */
    fun setMinAvailableColorDistance(minAvailableDistance: Int): Poliwhirl {
        if (minAvailableDistance < 0) throw IllegalArgumentException("minAvailableDistance should be >= 0")
        this.minAvailableDistance = minAvailableDistance
        request.minAvailableDistance = minAvailableDistance
        return this
    }

    /**
     * Colors on the border of the image have more weight. So, this multiplier determines the size
     * of this border.
     * For example: if [borderSizeMul] is 16 than vertical borders will have width=image.width/16
     *
     * @see setHorizontalBorderSizeDivideMultiplier
     */
    fun setVerticalBorderSizeDivideMultiplier(borderSizeMul: Int): Poliwhirl {
        if (borderSizeMul <= 1) throw IllegalArgumentException("verticalBorderSizeMul should be > 1")
        this.verticalBorderSizeMul = borderSizeMul
        request.verticalBorderSizeMul = borderSizeMul
        return this
    }

    /**
     * Colors on the border of the image have more weight. So, this multiplier determines the size
     * of this border.
     * For example: if [borderSizeMul] is 16 than horizontal borders will have height=image.height/16
     *
     *  @see setVerticalBorderSizeDivideMultiplier
     */
    fun setHorizontalBorderSizeDivideMultiplier(borderSizeMul: Int): Poliwhirl {
        if (borderSizeMul <= 1) throw IllegalArgumentException("horizontalBorderSizeMul should be > 1")
        this.horizontalBorderSizeMul = borderSizeMul
        request.horizontalBorderSizeMul = borderSizeMul
        return this
    }

    /**
     * Sets both vertical and horizontal [borderSizeMul]
     *
     * @see setVerticalBorderSizeDivideMultiplier
     * @see setHorizontalBorderSizeDivideMultiplier
     */
    fun setBorderSizeDivideMultiplier(borderSizeMul: Int): Poliwhirl {
        setVerticalBorderSizeDivideMultiplier(borderSizeMul)
        setHorizontalBorderSizeDivideMultiplier(borderSizeMul)
        return this
    }

    private fun createRequest(): Request = Request(verticalBorderSizeMul, horizontalBorderSizeMul,
            minAvailableDistance, accuracy)

    class Request internal constructor(var verticalBorderSizeMul: Int,
                                       var horizontalBorderSizeMul: Int,
                                       var minAvailableDistance: Int,
                                       var accuracy: Int) {
        private var cornersMul: Double = 1.0
        private var borderMul: Double = 1.0
        private var baseMul: Double = 1.0

        private fun updateMultipliers(borderWidth: Int, borderHeight: Int, width: Int, height: Int) {
            baseMul = 1.0
            val dBorderHeight = height - 2 * borderHeight
            val dBorderWidth = width - 2 * borderWidth
            borderMul = (baseMul * dBorderHeight * dBorderWidth + 1) / (2 * (dBorderHeight * borderHeight + dBorderWidth * borderWidth))
            cornersMul = (borderMul * (dBorderWidth * borderHeight + dBorderHeight * borderWidth) + 1) / (2 * borderHeight * borderWidth)
        }

        internal fun execute(bitmap: Bitmap,
                             callback: Callback,
                             executor: Executor,
                             forceNumThreads: Int): Request {
            assertAccuracy(bitmap.width, bitmap.height)

            val verticalBorder = bitmap.height / horizontalBorderSizeMul
            val horizontalBorder = bitmap.width / verticalBorderSizeMul
            updateMultipliers(horizontalBorder, verticalBorder, bitmap.width, bitmap.height)

            val start = System.currentTimeMillis()

            var currentlyFinishedThreadCount = 0

            var numThreads = if (forceNumThreads <= 0) Math.max(0, Math.min(
                    (executor as? ThreadPoolExecutor)?.maximumPoolSize ?: bitmap.height / accuracy, 1)) else 0
            if (bitmap.height * 2 < numThreads) {
                numThreads = 1
            }

            val resultColors: MutableList<ColorGroup> = ArrayList()
            val bitmapHeightByThread = bitmap.height / numThreads
            for (threadNumber in 0 until numThreads) {
                executor.execute {
                    val colors: SizeAwareArrayList<ColorGroup> = SizeAwareArrayList(maxPermittedColorsSize)
                    var endX = 0
                    var endY = 0
                    for (y in threadNumber * bitmapHeightByThread
                            until (threadNumber * bitmapHeightByThread + bitmapHeightByThread)
                            step accuracy) {

                        for (x in 0 until bitmap.width step accuracy) {
                            val color = bitmap.getPixel(x, y)
                            val colormultiplier = getColorMultiplier(x, y,
                                    bitmap.width, bitmap.height, horizontalBorder, verticalBorder)
                            putColor(colors, color, colormultiplier)
                            endX = x
                        }
                        endY = y
                    }

                    Log.d(logTag, "Finished a part at ($endX,$endY) in ${System.currentTimeMillis() - start}")
                    synchronized(resultColors) {
                        resultColors.add(colors.maxBy { it.multiplier }!!)
                    }
                    currentlyFinishedThreadCount += 1
                    if (currentlyFinishedThreadCount == numThreads) {
                        Handler(Looper.getMainLooper()).post { onFindingColorFinished(resultColors, callback, start) }
                    }
                }
            }
            return this
        }

        private fun assertAccuracy(bitmapWidth: Int, bitmapHeight: Int) {
            if (accuracy > bitmapHeight) throw IllegalArgumentException("accuracy should be <= image height")
            if (accuracy > bitmapWidth) throw IllegalArgumentException("accuracy should be <= image width")
            if (accuracy <= 0) throw IllegalArgumentException("accuracy should be > 0")
        }

        private fun putColor(colors: SizeAwareArrayList<ColorGroup>, newColor: Int, newColormultiplier: Double) {
            val newColorLab = ColorUtils.rgb2lab(Color.red(newColor), Color.green(newColor), Color.blue(newColor))
            for (colorKey in colors) {
                if (ColorUtils.ciede2000(newColorLab, colorKey.colorLab) <= minAvailableDistance) {
                    colorKey.addMultiplier(newColor, newColormultiplier)
                    return
                }
            }
            colors.add(ColorGroup(newColor, newColorLab, newColormultiplier))
        }


        /**
         * Returning color multiplier according to pixel position on the bitmap
         */
        private fun getColorMultiplier(x: Int, y: Int,
                                       width: Int, height: Int,
                                       horizontalBorder: Int, verticalBorder: Int): Double {
            return if (x < horizontalBorder && y < verticalBorder ||
                    x > width - horizontalBorder && y < verticalBorder ||
                    x > horizontalBorder && y > height - verticalBorder ||
                    x > width - horizontalBorder && y > height - verticalBorder) {
                cornersMul
            } else if (x > horizontalBorder && y > verticalBorder
                    && x < width - horizontalBorder && y < height - verticalBorder) {
                baseMul
            } else {
                borderMul
            }
        }

        /**
         * Invoked on UI thread
         */
        private fun onFindingColorFinished(colors: List<ColorGroup>, callback: Callback, startTimestamp: Long) {
            Log.d(logTag, "Finished in " + (System.currentTimeMillis() - startTimestamp))
            callback.foundColor(colors.maxBy { it.multiplier }!!.getTopColor())
        }

    }

    /**
     * This class represents color group where colors have close enough [colorLab].
     * Group stores multiple sub-colors and processes their multiplier updates
     */
    private class ColorGroup(baseColor: Int, val colorLab: DoubleArray, var multiplier: Double) {

        private val maxColorsSize = 4
        private val colors: SizeAwareArrayList<ColorInGroup> = SizeAwareArrayList(maxColorsSize)

        init {
            colors.add(ColorInGroup(baseColor, multiplier))
        }

        /**
         * Adds multiplier for the provided color or adds it to the group if there is no such color yet
         */
        fun addMultiplier(color: Int, additionalMultiplier: Double): ColorGroup {
            synchronized(colors) {
                this.multiplier += additionalMultiplier
                for (colorInGroup in colors) {
                    if (colorInGroup.color == color) {
                        colorInGroup.colorMultiplier += additionalMultiplier
                        return this
                    }
                }
                colors.add(ColorInGroup(color, multiplier))
                return this
            }
        }

        /**
         * @return the color with the top [ColorInGroup.colorMultiplier]
         */
        fun getTopColor(): Int {
            synchronized(colors) {
                return colors.maxBy { it.colorMultiplier }!!.color
            }
        }
    }

    /**
     * This class represents a single color in the [ColorGroup].
     *
     * [colorMultiplier] is a multiplier of a particular color in the group
     */
    private data class ColorInGroup(val color: Int, var colorMultiplier: Double) {
        override fun equals(other: Any?): Boolean = (other as ColorInGroup).color == color
        override fun hashCode(): Int {
            var result = color
            result = 31 * result + colorMultiplier.hashCode()
            return result
        }
    }
}