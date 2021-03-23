package kaa.schemaregistry.utils

import java.util
import collection.JavaConverters._

object CollectionConverters {
    def mapAsJava[K, V](map: Map[K, V]): util.Map[K, V] = {
        map.asJava
    }
}
