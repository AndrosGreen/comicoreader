package com.androsgreen.comicreader.data.archive

import org.junit.Assert.assertEquals
import org.junit.Test

class NaturalOrderComparatorTest {

    private fun sorted(vararg names: String) = names.sortedWith(NaturalOrderComparator)

    @Test
    fun numbersCompareByValue() {
        assertEquals(
            listOf("page1.jpg", "page2.jpg", "page10.jpg", "page100.jpg"),
            sorted("page10.jpg", "page100.jpg", "page2.jpg", "page1.jpg")
        )
    }

    @Test
    fun leadingZerosCompareByValue_withDeterministicTieBreak() {
        assertEquals(listOf("p007.jpg", "p7.jpg", "p8.jpg"), sorted("p8.jpg", "p7.jpg", "p007.jpg"))
    }

    @Test
    fun veryLongNumbersDoNotOverflow() {
        assertEquals(
            listOf("p99999999999999999999.jpg", "p100000000000000000000.jpg"),
            sorted("p100000000000000000000.jpg", "p99999999999999999999.jpg")
        )
    }

    @Test
    fun lettersIgnoreCase() {
        assertEquals(listOf("a.jpg", "B.jpg", "c.jpg"), sorted("c.jpg", "B.jpg", "a.jpg"))
    }

    @Test
    fun pathsSortFolderByFolder() {
        assertEquals(
            listOf("ch1/p2.jpg", "ch1/p10.jpg", "ch2/p1.jpg", "ch10/p1.jpg"),
            sorted("ch10/p1.jpg", "ch2/p1.jpg", "ch1/p10.jpg", "ch1/p2.jpg")
        )
    }

    @Test
    fun folderContentsSortBeforeSiblingsSharingAPrefix() {
        // Plain string order would put "ch1-extra" first, because '-' < '/'.
        assertEquals(
            listOf("ch1/p1.jpg", "ch1-extra/p1.jpg"),
            sorted("ch1-extra/p1.jpg", "ch1/p1.jpg")
        )
    }

    @Test
    fun shorterNameSortsFirstWhenItIsAPrefix() {
        assertEquals(listOf("page", "page1"), sorted("page1", "page"))
    }

    @Test
    fun equalStringsCompareAsZero() {
        assertEquals(0, NaturalOrderComparator.compare("ch1/p10.jpg", "ch1/p10.jpg"))
    }
}
