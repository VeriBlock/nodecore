package org.veriblock.core.utilities.extensions

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun Int.isValidPort(): Boolean {
    return this in 1..65535
}

fun String.isPositiveInteger(): Boolean = toIntOrNull()?.let {
    it > 0
} ?: false

fun Char.isAlphabetic(): Boolean {
    return (this in 'a'..'z') or (this in 'A'..'Z')
}

fun String.isAlphabetic(): Boolean {
    for (element in this) {
        if (!element.isAlphabetic()) {
            return false
        }
    }
    return true
}

fun Char.isNumeric(): Boolean {
    return this in '0'..'9'
}

fun String.isNumeric(): Boolean {
    for (element in this) {
        if (!element.isNumeric()) {
            return false
        }
    }
    return true
}

fun Char.isAlphanumeric(): Boolean {
    return this.isAlphabetic() or this.isNumeric()
}

fun String.isAlphanumeric(): Boolean {
    return this.isAlphabetic() or this.isNumeric()
}

fun Date.formatForHttp(): String {
    val PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz"
    val GMT = TimeZone.getTimeZone("GMT")
    val formatter = SimpleDateFormat(PATTERN_RFC1123, Locale.US)
    formatter.timeZone = GMT
    return formatter.format(this)
}

fun Long.formatAtomicLongWithDecimal(): String {
    val isNegative = this < 0
    var result = "" + if (isNegative) -1 * this else this
    while (result.length < 8) {
        result = "0$result"
    }
    val spotForDecimal = result.length - 8
    result = (if (isNegative) "-" else "") + result.substring(0, spotForDecimal) + "." + result.substring(spotForDecimal)
    if (result[0] == '.') {
        result = "0$result"
    }
    return result
}

fun String.countChar(toFind: Char): Int {
    var count = 0
    for (i in 0 until length) {
        if (this[i] == toFind) {
            count++
        }
    }
    return count
}

private const val DECIMAL_NUMBER_CHARACTERS = "-0123456789."
fun String.isDecimalNumber(): Boolean {
    for (c in this) {
        require(DECIMAL_NUMBER_CHARACTERS.contains(c)) {
            "isDecimalNumber cannot be called with a non-decimal number (${this})!"
        }
    }
    require(this.countChar('.') <= 1) {
        "isDecimalNumber cannot be called with a String with more than one decimal point (${this})"
    }
    return true
}

fun String.asDecimalCoinToAtomicLong(): Long {
    require(isDecimalNumber()) {
        "convertDecimalCoinToAtomicLong cannot be called with a non-decimal String (${this})!"
    }
    var result = this
    if (!result.contains(".")) {
        result = "$result."
    }
    if (result[0] == '.') {
        result = "0$result"
    }
    val numCharactersAfterDecimal = result.length - result.indexOf(".") - 1
    require(numCharactersAfterDecimal <= 8) {
        "convertDecimalCoinToAtomicLong cannot be called with a String with more than 8 numbers after the decimal point!"
    }
    result = result.replace(".", "")
    for (i in 8 downTo numCharactersAfterDecimal + 1) {
        result += "0"
    }
    return result.toLong()
}
