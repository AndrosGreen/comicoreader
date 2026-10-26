package com.androsgreen.comicreader.data.archive

/**
 * Orders strings the way people expect file names to be ordered: runs of digits compare by
 * numeric value (`page2` < `page10`), letters compare ignoring case, and `/` sorts before any
 * other character so paths are ordered folder by folder. Ties fall back to plain string order,
 * so the result is always deterministic.
 */
object NaturalOrderComparator : Comparator<String> {

    override fun compare(a: String, b: String): Int {
        var i = 0
        var j = 0
        while (i < a.length && j < b.length) {
            if (a[i].isAsciiDigit() && b[j].isAsciiDigit()) {
                val startA = i
                val startB = j
                while (i < a.length && a[i].isAsciiDigit()) i++
                while (j < b.length && b[j].isAsciiDigit()) j++
                // Compare as digit strings so arbitrarily long numbers can't overflow.
                val numberA = a.substring(startA, i).trimStart('0')
                val numberB = b.substring(startB, j).trimStart('0')
                if (numberA.length != numberB.length) return numberA.length - numberB.length
                val byValue = numberA.compareTo(numberB)
                if (byValue != 0) return byValue
            } else {
                val byChar = sortKey(a[i]).compareTo(sortKey(b[j]))
                if (byChar != 0) return byChar
                i++
                j++
            }
        }
        val byRemainingLength = (a.length - i) - (b.length - j)
        if (byRemainingLength != 0) return byRemainingLength
        return a.compareTo(b)
    }

    private fun Char.isAsciiDigit() = this in '0'..'9'

    private fun sortKey(c: Char): Int = if (c == '/') -1 else c.lowercaseChar().code
}
