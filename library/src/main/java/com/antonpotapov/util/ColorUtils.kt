package com.antonpotapov.util

/**
 * ======================================
 * Created by awesome potapov on 23.10.17.
 * ======================================
 */
internal object ColorUtils {

    /**
     * Converts a color from the RGB color space the L*a*b color space
     *
     * Taken from https://github.com/StanfordHCI/c3/blob/master/java/src/edu/stanford
     * /vis/color/LAB.java
     *
     * Maps an RGB triple to binned LAB space (D65). Binning is done by *flooring* LAB values.
     *
     * @param ri Red component of the RGB color.
     * @param gi Green component of the RGB color.
     * @param bi Blue component of the RGB color.
     * @return The color in the L*a*b color space
     */
    internal fun rgb2lab(ri: Int, gi: Int, bi: Int): DoubleArray {
        // first, normalize RGB values
        var r = ri / 255.0
        var g = gi / 255.0
        var b = bi / 255.0
        // D65 standard referent
        val X = 0.950470
        val Y = 1.0
        val Z = 1.088830
        // second, map sRGB to CIE XYZ
        r = if (r <= 0.04045) r / 12.92 else Math.pow((r + 0.055) / 1.055, 2.4)
        g = if (g <= 0.04045) g / 12.92 else Math.pow((g + 0.055) / 1.055, 2.4)
        b = if (b <= 0.04045) b / 12.92 else Math.pow((b + 0.055) / 1.055, 2.4)
        var x = (0.4124564 * r + 0.3575761 * g + 0.1804375 * b) / X
        var y = (0.2126729 * r + 0.7151522 * g + 0.0721750 * b) / Y
        var z = (0.0193339 * r + 0.1191920 * g + 0.9503041 * b) / Z
        // third, map CIE XYZ to CIE L*a*b* and return
        x = if (x > 0.008856) Math.pow(x, 1.0 / 3) else 7.787037 * x + 4.0 / 29
        y = if (y > 0.008856) Math.pow(y, 1.0 / 3) else 7.787037 * y + 4.0 / 29
        z = if (z > 0.008856) Math.pow(z, 1.0 / 3) else 7.787037 * z + 4.0 / 29
        val L = 116 * y - 16
        val A = 500 * (x - y)
        val B = 200 * (y - z)
        return doubleArrayOf(L, A, B)
    }

    /**
     * Compares to L*a*b colors and returns the degree of their similarity. The lower the result the
     * more similar are the colors.
     *
     * Taken from https://github.com/StanfordHCI/c3/blob/master/java/src/edu/stanford
     * /vis/color/LAB.java
     *
     * @param lab1 First color represented in L*a*b color space.
     * @param lab2 Second color represented in L*a*b color space.
     * @return The degree of similarity between the two input colors according to the CIEDE2000
     *         color-difference formula.
     */
    internal fun ciede2000(lab1: DoubleArray, lab2: DoubleArray): Double {
        // adapted from Sharma et al's MATLAB implementation at
        // http://www.ece.rochester.edu/~gsharma/ciede2000/
        // parametric factors, use defaults
        val kl = 1.0
        val kc = 1.0
        val kh = 1.0
        // compute terms
        val pi: Double = Math.PI
        val L1 = lab1[0]
        val a1 = lab1[1]
        val b1 = lab1[2]
        val Cab1 = Math.sqrt((a1 * a1) + (b1 * b1))
        val L2 = lab2[0]
        val a2 = lab2[1]
        val b2 = lab2[2]
        val Cab2 = Math.sqrt((a2 * a2) + (b2 * b2))
        val Cab = 0.5 * (Cab1 + Cab2)
        val G = 0.5 * (1 - Math.sqrt(Math.pow(Cab, 7.0) / (Math.pow(Cab, 7.0) + Math.pow(25.0, 7.0))))
        val ap1 = (1 + G) * a1
        val ap2 = (1 + G) * a2
        val Cp1 = Math.sqrt((ap1 * ap1) + (b1 * b1))
        val Cp2 = Math.sqrt((ap2 * ap2) + (b2 * b2))
        val Cpp = Cp1 * Cp2
        // ensure hue is between 0 and 2pi
        var hp1 = Math.atan2(b1, ap1)
        if (hp1 < 0) {
            hp1 += 2 * pi
        }
        var hp2: Double = Math.atan2(b2, ap2)
        if (hp2 < 0) {
            hp2 += 2 * pi
        }
        var dL = L2 - L1
        var dC = Cp2 - Cp1
        var dhp = hp2 - hp1
        if (dhp > +pi) {
            dhp -= 2 * pi
        }
        if (dhp < -pi) {
            dhp += 2 * pi
        }
        if (Cpp == 0.0) {
            dhp = 0.0
        }
        // Note that the defining equations actually need signed Hue and chroma
        // differences which is different from prior color difference formulae
        var dH: Double = 2 * Math.sqrt(Cpp) * Math.sin(dhp / 2)
        // Weighting functions
        val Lp = 0.5 * (L1 + L2)
        val Cp = 0.5 * (Cp1 + Cp2)
        // Average Hue Computation. This is equivalent to that in the paper but
        // simpler programmatically. Average hue is computed in radians and
        // converted to degrees where needed
        var hp = 0.5 * (hp1 + hp2)
        // Identify positions for which abs hue diff exceeds 180 degrees
        if (Math.abs(hp1 - hp2) > pi) {
            hp -= pi
        }
        if (hp < 0) {
            hp += 2 * pi
        }
        // Check if one of the chroma values is zero, in which case set mean hue
        // to the sum which is equivalent to other value
        if (Cpp == 0.0) {
            hp = hp1 + hp2
        }
        val Lpm502: Double = (Lp - 50) * (Lp - 50)
        val Sl = 1 + ((0.015 * Lpm502) / Math.sqrt(20 + Lpm502))
        val Sc = 1 + (0.045 * Cp)
        val T = ((1 - (0.17 * Math.cos(hp - (pi / 6)))) + (0.24 * Math.cos(2 * hp))
                + (0.32 * Math.cos((3 * hp) + (pi / 30)))) - (0.20 * Math.cos((4 * hp) - ((63 * pi) / 180)))
        val Sh = 1 + (0.015 * Cp * T)
        val ex = (((180 / pi) * hp) - 275) / 25
        val delthetarad = ((30 * pi) / 180) * Math.exp(-1 * (ex * ex))
        val Rc = 2 * Math.sqrt(Math.pow(Cp, 7.0) / (Math.pow(Cp, 7.0) + Math.pow(25.0, 7.0)))
        val RT = -1 * Math.sin(2 * delthetarad) * Rc
        dL /= kl * Sl
        dC /= kc * Sc
        dH /= kh * Sh
        // The CIED200 color difference
        return Math.sqrt((dL * dL) + (dC * dC) + (dH * dH) + (RT * dC * dH))
    }
}