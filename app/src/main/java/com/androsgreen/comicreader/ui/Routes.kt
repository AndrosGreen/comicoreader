package com.androsgreen.comicreader.ui

import kotlinx.serialization.Serializable

// Each bottom-bar tab is a nested graph, so it keeps its own back stack.

@Serializable
object LibraryGraph

@Serializable
object LibraryRoute

@Serializable
object CollectionsGraph

@Serializable
object CollectionsRoute

@Serializable
data class CollectionDetailRoute(val collectionId: Long)
