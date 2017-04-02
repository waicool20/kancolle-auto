package com.waicool20.kcsubswitcher

import org.sikuli.basics.Settings
import org.sikuli.script.*
import java.util.*
import java.util.concurrent.TimeUnit

typealias xCoord = Int
typealias yCoord = Int
typealias Width = Int
typealias Height = Int

fun Region.subRegion(x: xCoord, y: yCoord, width: Width, height: Height) =
        Region(this.x + x, this.y + y, width, height)

/**
 * Find something in a region or return null instead of raising an Exception
 */

fun Region.findOrNull(image: String) = try {
    find(image)
} catch (e: FindFailed) {
    null
}

fun Region.findOrNull(pattern: Pattern) = try {
    find(pattern)
} catch (e: FindFailed) {
    null
}

fun Region.findOrNull(image: Image) = try {
    find(image)
} catch (e: FindFailed) {
    null
}

fun Region.findAllOrEmpty(image: String): List<Match> {
    val matches = mutableListOf<Match>()
    try {
        findAll(image)?.forEachRemaining { matches.add(it) }
    } catch (e: FindFailed) {
        Unit // Ignore
    }
    return matches
}

fun Region.findAllOrEmpty(pattern: Pattern): List<Match> {
    val matches = mutableListOf<Match>()
    try {
        findAll(pattern)?.forEachRemaining { matches.add(it) }
    } catch (e: FindFailed) {
        Unit // Ignore
    }
    return matches
}

fun Region.findAllOrEmpty(image: Image): List<Match> {
    val matches = mutableListOf<Match>()
    try {
        findAll(image)?.forEachRemaining { matches.add(it) }
    } catch (e: FindFailed) {
        Unit // Ignore
    }
    return matches
}

/**
 * Region exist utils
 */

fun Region.has(image: String, similarity: Double = Settings.MinSimilarity) =
        exists(Pattern(image).similar(similarity.toFloat())) != null

fun Region.has(pattern: Pattern, similarity: Double = Settings.MinSimilarity) =
        exists(Pattern(pattern).similar(similarity.toFloat())) != null

fun Region.has(image: Image, similarity: Double = Settings.MinSimilarity) =
        exists(Pattern(image).similar(similarity.toFloat())) != null

inline fun <reified T> Region.has(images: Set<T>, similarity: Double = Settings.MinSimilarity): Boolean {
    when {
        images.first() is String -> if (images.parallelMap({ has(it as String, similarity) }).filter { it }.count() > 0) return true
        images.first() is Pattern -> if (images.parallelMap({ has(it as Pattern, similarity) }).filter { it }.count() > 0) return true
        images.first() is Image -> if (images.parallelMap({ has(it as Image, similarity) }).filter { it }.count() > 0) return true
    }
    return false
}

/**
 * Inverse of region exists utils
 */

fun Region.doesntHave(image: String, similarity: Double = Settings.MinSimilarity) =
        !has(image, similarity)

fun Region.doesntHave(pattern: Pattern, similarity: Double = Settings.MinSimilarity) =
        !has(pattern, similarity)

fun Region.doesntHave(image: Image, similarity: Double = Settings.MinSimilarity) =
        !has(image, similarity)

inline fun <reified T> Region.doesntHave(images: Set<T>, similarity: Double = Settings.MinSimilarity): Boolean =
        !has(images, similarity)


/**
 * Random click generator
 */

fun Match.clickRandomly() {
    val RNG = Random()
    val dx = RNG.nextInt(w / 2) * RNG.nextSign()
    val dy = RNG.nextInt(h / 2) * RNG.nextSign()
    Pattern(image).targetOffset(dx, dy)
    click()
}

fun Region.clickRandomly(image: String, usePreviousMatch: Boolean = false) {
    try {
        (if (usePreviousMatch) lastMatch else findOrNull(image))?.let(Match::clickRandomly)
    } catch (e: FindFailed) {
        e.printStackTrace()
    }
}

fun Region.clickRandomly(pattern: Pattern, usePreviousMatch: Boolean = false) {
    try {
        (if (usePreviousMatch) lastMatch else findOrNull(pattern))?.let(Match::clickRandomly)
    } catch (e: FindFailed) {
        e.printStackTrace()
    }
}

fun Region.clickRandomly(image: Image, usePreviousMatch: Boolean = false) {
    try {
        (if (usePreviousMatch) lastMatch else findOrNull(image))?.let(Match::clickRandomly)
    } catch (e: FindFailed) {
        e.printStackTrace()
    }
}


/**
 * Random rest after clicking generator
 */
fun Region.clickAndRest(image: String,
                        minMillis: Long = 200, maxMillis: Long = 500,
                        usePreviousMatch: Boolean = false) {
    clickRandomly(image, usePreviousMatch)
    TimeUnit.MILLISECONDS.sleep(Random().nextLong(minMillis, maxMillis))
}

fun Region.clickAndRest(pattern: Pattern,
                        minMillis: Long = 200, maxMillis: Long = 500,
                        usePreviousMatch: Boolean = false) {
    clickRandomly(pattern, usePreviousMatch)
    TimeUnit.MILLISECONDS.sleep(Random().nextLong(minMillis, maxMillis))
}

fun Region.clickAndRest(image: Image,
                        minMillis: Long = 200, maxMillis: Long = 500,
                        usePreviousMatch: Boolean = false) {
    clickRandomly(image, usePreviousMatch)
    TimeUnit.MILLISECONDS.sleep(Random().nextLong(minMillis, maxMillis))
}

/**
 * Check before clicking
 */
fun Region.checkAndClick(image: String, minMillis: Long = 200, maxMillis: Long = 500) {
    if (has(image)) clickAndRest(image, minMillis, maxMillis, true)
}

fun Region.checkAndClick(pattern: Pattern, minMillis: Long = 200, maxMillis: Long = 500) {
    if (has(pattern)) clickAndRest(pattern, minMillis, maxMillis, true)
}

fun Region.checkAndClick(image: Image, minMillis: Long = 200, maxMillis: Long = 500) {
    if (has(image)) clickAndRest(image, minMillis, maxMillis, true)
}

/**
 * Find minimum similarity for something to have match, it will sweep from max to min
 */
fun Region.findMinimumSimilarity(image: String, min: Double = 0.0, max: Double = 1.0, steps: Double = 0.05): Double {
    var similarity = max
    while (similarity >= min) {
        println("Checking similarity $similarity for $image")
        if (this.findOrNull(image) != null) return similarity
        similarity -= steps
    }
    return -1.0
}

fun Region.findMinimumSimilarity(pattern: Pattern, min: Double = 0.0, max: Double = 1.0, steps: Double = 0.05): Double {
    var similarity = max
    while (similarity >= min) {
        println("Checking similarity $similarity for $pattern")
        if (findOrNull(pattern) != null) return similarity
        similarity -= steps
    }
    return -1.0
}

fun Region.findMinimumSimilarity(image: Image, min: Double = 0.0, max: Double = 1.0, steps: Double = 0.05): Double {
    var similarity = max
    while (similarity >= min) {
        println("Checking similarity $similarity for $image")
        if (this.findOrNull(image) != null) return similarity
        similarity -= steps
    }
    return -1.0
}

fun Region.hoverRandomly(times: Int = 1) {
    val RNG = Random()
    repeat(times) {
        hover(Location(x + RNG.nextInt(w), y + RNG.nextInt(h)))
        TimeUnit.MILLISECONDS.sleep(100)
    }
}
