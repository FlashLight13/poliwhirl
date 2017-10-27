package com.antonpotapov

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.support.annotation.ColorInt
import android.util.Log
import com.antonpotapov.util.ColorUtils
import com.antonpotapov.util.SizeAwareArrayList
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

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

        private val CPU_COUNT = Runtime.getRuntime().availableProcessors()
        // We want at least 2 threads and at most 4 threads in the core pool,
        // preferring to have 1 less than the CPU count to avoid saturating
        // the CPU with background work
        private val CORE_POOL_SIZE = Math.max(2, Math.min(CPU_COUNT - 1, 4))
        private val MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1
        private val asyncExecutor = ThreadPoolExecutor(
                CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, 30.toLong(), TimeUnit.SECONDS,
                LinkedBlockingQueue<Runnable>(), object : ThreadFactory {
            private val mCount = AtomicInteger(1)

            override fun newThread(r: Runnable): Thread =
                    Thread(r, "Poliwhirl processing thread #" + mCount.getAndIncrement())
        })

        init {
            asyncExecutor.allowCoreThreadTimeOut(true)
            Executors.newCachedThreadPool()
        }
    }

    var borderSizeMul: Int = 16
        private set
    var minAvailableDistance: Int = 20
        private set
    var accuracy: Int = 3
        private set
    private var request: Request

    init {
        request = createRequest()
    }

    fun generate(bitmap: Bitmap): Int {
        var result = 0
        generateOnExecutor(bitmap, object : Callback {
            override fun foundColor(color: Int) {
                result = color
            }
        }, currentThreadExecutor)
        return result
    }

    fun generateAsync(bitmap: Bitmap, callback: Callback) {
        generateOnExecutor(bitmap, callback, asyncExecutor)
    }

    fun generateOnExecutor(bitmap: Bitmap, callback: Callback, executor: Executor): Request {
        try {
            return request.execute(bitmap, callback, executor)
        } finally {
            request = createRequest()
        }
    }

    fun setAccuracy(accuracy: Int): Poliwhirl {
        this.accuracy = accuracy
        request.accuracy = accuracy
        return this
    }

    fun setMinAvailableColorDistance(minAvailableDistance: Int): Poliwhirl {
        this.minAvailableDistance = minAvailableDistance
        request.minAvailableDistance = minAvailableDistance
        return this
    }

    fun setBorderSizeDivideMultiplier(borderSizeMul: Int): Poliwhirl {
        this.borderSizeMul = borderSizeMul
        request.borderSizeMul = borderSizeMul
        return this
    }

    private fun createRequest(): Request = Request(borderSizeMul, minAvailableDistance, accuracy)

    class Request internal constructor(var borderSizeMul: Int,
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
                             executor: Executor): Request {
            val verticalBorder = bitmap.height / borderSizeMul
            val horizontalBorder = bitmap.width / borderSizeMul
            updateMultipliers(horizontalBorder, verticalBorder, bitmap.width, bitmap.height)

            val start = System.currentTimeMillis()

            var currentlyFinishedThreadCount = 0
            var numThreads = Math.max(0, Math.min(
                    (executor as? ThreadPoolExecutor)?.maximumPoolSize ?: bitmap.height / accuracy,
                    MAXIMUM_POOL_SIZE))
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
        fun addMultiplier(color: Int, additionalmultiplier: Double): ColorGroup {
            synchronized(colors) {
                this.multiplier += additionalmultiplier
                for (colorInGroup in colors) {
                    if (colorInGroup.color == color) {
                        colorInGroup.colorMultiplier += additionalmultiplier
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