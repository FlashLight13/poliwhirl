package com.antonpotapov

import android.graphics.Bitmap
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * ======================================
 * Created by awesome potapov on 30.10.17.
 * ======================================
 */
@RunWith(RobolectricTestRunner::class)
class PoliwhirlApiTests {

    @Test(expected = IllegalArgumentException::class)
    fun testApiAccuracy1() {
        Poliwhirl().setAccuracy(0).generate(Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testApiAccuracy2() {
        Poliwhirl().setAccuracy(11).generate(Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testApiDistance() {
        Poliwhirl().setMinAvailableColorDistance(-1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testApiHorizontalBorder1() {
        Poliwhirl().setHorizontalBorderSizeDivideMultiplier(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testApiHorizontalBorder2() {
        Poliwhirl().setHorizontalBorderSizeDivideMultiplier(1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testApiVerticalBorder1() {
        Poliwhirl().setVerticalBorderSizeDivideMultiplier(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testApiVerticalBorder2() {
        Poliwhirl().setVerticalBorderSizeDivideMultiplier(1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testApiBorder1() {
        Poliwhirl().setBorderSizeDivideMultiplier(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testApiBorder2() {
        Poliwhirl().setBorderSizeDivideMultiplier(1)
    }
}