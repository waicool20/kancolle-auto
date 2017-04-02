package com.waicool20.kcsubswitcher

import org.sikuli.basics.Settings
import org.sikuli.script.*
import java.util.*
import java.util.concurrent.TimeUnit

typealias xCoord = Int
typealias yCoord = Int
typealias Width = Int
typealias Height = Int

fun Region.subRegion(x: xCoord, y: yCoord, width: Width, height: Height): Region {
    val xCoord = if (x in 0..w) (this.x + x) else w
    val yCoord = if (y in 0..h) (this.y + y) else h
    val newWidth = if (width in 0..w) width else w
    val newHeight = if (height in 0..w) height else h
    return Region(xCoord, yCoord, newWidth, newHeight)
}

enum class Quadrant {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}

fun Region.getQuadrant(quadrant: Quadrant) = when (quadrant) {
    Quadrant.TOP_LEFT -> subRegion(0, 0, w / 2, h / 2)
    Quadrant.TOP_RIGHT -> subRegion(w / 2, 0, w / 2, h / 2)
    Quadrant.BOTTOM_LEFT -> subRegion(0, h / 2, w / 2, h / 2)
    Quadrant.BOTTOM_RIGHT -> subRegion(w / 2, h / 2, w / 2, h / 2)
}

/**
 * Find something in a region or return null instead of raising an Exception
 */

fun <PSI> Region.findOrNull(psi: PSI) = try {
    find(psi)
} catch (e: FindFailed) {
    null
}

fun <PSI> Region.findAllOrEmpty(psi: PSI): List<Match> {
    val matches = mutableListOf<Match>()
    try {
        findAll(psi)?.forEachRemaining { matches.add(it) }
    } catch (e: FindFailed) {
        Unit // Ignore
    }
    return matches
}

/**
 * Region exist utils
 */

fun <PSI> Region.has(psi: PSI, similarity: Double = Settings.MinSimilarity) = when (psi) {
    is String -> exists(Pattern(psi).similar(similarity.toFloat())) != null
    is Pattern -> exists(Pattern(psi).similar(similarity.toFloat())) != null
    is Image -> exists(Pattern(psi).similar(similarity.toFloat())) != null
    else -> throw IllegalArgumentException()
}

fun <PSI> Region.has(images: Set<PSI>, similarity: Double = Settings.MinSimilarity) = when (images.firstOrNull()) {
    is String -> images.parallelMap({ has(it, similarity) }).contains(true)
    is Pattern -> images.parallelMap({ has(it, similarity) }).contains(true)
    is Image -> images.parallelMap({ has(it, similarity) }).contains(true)
    else -> false
}

/**
 * Inverse of region exists utils
 */

fun <PSI> Region.doesntHave(psi: PSI, similarity: Double = Settings.MinSimilarity) =
        !has(psi, similarity)

fun <PSI> Region.doesntHave(images: Set<PSI>, similarity: Double = Settings.MinSimilarity): Boolean =
        !has(images, similarity)

/**
 * Utility class to make common clicking actions more readable
 */
class Clicker<out PSI>(val region: Region, val target: PSI) {

    fun <PSI2> untilThisDisappears(psi2: PSI2) {
        while (region.has(psi2)) normally()
    }

    fun <PSI2> untilThisAppears(psi2: PSI2) {
        while (region.doesntHave(psi2)) normally()
    }

    fun normally(times: Int = 1,
                 minMillis: Long = 200, maxMillis: Long = 500,
                 usePreviousMatch: Boolean = false) {
        randomly(times, usePreviousMatch)
        TimeUnit.MILLISECONDS.sleep(Random().nextLong(minMillis, maxMillis))
    }

    fun ifItExists() {
        if (target !is Region && region.has(target)) normally()
    }

    private fun randomly(times: Int = 1, usePreviousMatch: Boolean = false) {
        val RNG = Random()
        if (target is Region) {
            repeat(times) {
                val xCoord = target.x + RNG.nextInt(target.w)
                val yCoord = target.y + RNG.nextInt(target.h)
                target.click(Location(xCoord, yCoord))
                TimeUnit.MILLISECONDS.sleep(100)
            }
        } else {
            val match = if (usePreviousMatch) region.lastMatch else region.findOrNull(target)
            repeat(times) {
                if (match != null) {
                    with(match) {
                        val dx = RNG.nextInt(w / 2) * RNG.nextSign()
                        val dy = RNG.nextInt(h / 2) * RNG.nextSign()
                        Pattern(image).targetOffset(dx, dy)
                        click()
                    }
                }
            }
        }
    }
}

/**
 * Function that keeps on clicking target untilThisAppears target is seen
 */

fun <PSI> Region.clickOn(source: PSI) = Clicker(this, source)

fun Region.clickItself() = Clicker(this, this)

/**
 * Find minimum similarity for something to have match, it will sweep from max to min
 */
fun <PSI> Region.findMinimumSimilarity(psi: PSI, min: Double = 0.0, max: Double = 1.0, steps: Double = 0.05): Double {
    var similarity = max
    while (similarity >= min) {
        println("Checking similarity $similarity for $psi")
        if (this.findOrNull(psi) != null) return similarity
        similarity -= steps
    }
    return -1.0
}

/**
 * Random hover function
 */

fun Region.hoverRandomly(times: Int = 1) {
    val RNG = Random()
    repeat(times) {
        hover(Location(x + RNG.nextInt(w), y + RNG.nextInt(h)))
        TimeUnit.MILLISECONDS.sleep(100)
    }
}
