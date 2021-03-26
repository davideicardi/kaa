package kaa.schemaregistry.utils

import java.util
import scala.jdk.CollectionConverters._

object CollectionConverters {
    def mapAsJava[K, V](map: Map[K, V]): util.Map[K, V] = {
        map.asJava
    }
}
