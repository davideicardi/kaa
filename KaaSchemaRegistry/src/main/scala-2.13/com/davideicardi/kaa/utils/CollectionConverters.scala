package com.davideicardi.kaa.utils

import scala.jdk.CollectionConverters._

object CollectionConverters {
    def mapAsJava[K, V](map: Map[K, V]) = {
        map.asJava
    }
}
