package com.androsgreen.comicreader.ui.collections

import com.androsgreen.comicreader.domain.Comic

data class CollectionDetailUiState(
    val name: String = "",
    /** In the order they were added to the collection. */
    val comics: List<Comic> = emptyList(),
    /** True until the collection and its comics have loaded. */
    val isLoading: Boolean = true,
    /** True once the collection no longer exists; the screen should close. */
    val isDeleted: Boolean = false,
    /** The comic awaiting remove confirmation, if any. */
    val pendingRemove: Comic? = null
)
