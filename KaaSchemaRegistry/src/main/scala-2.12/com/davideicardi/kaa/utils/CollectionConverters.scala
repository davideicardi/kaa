package com.davideicardi.kaa.utils

import collection.JavaConverters._

object CollectionConverters {
    def mapAsJava[K, V](map: Map[K, V]) = {
        map.asJava
    }
}
