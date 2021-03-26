package kaa.schemaregistry


case class InvalidSchemaException(msg: String) extends Exception(msg)

case class SchemaNotFoundException(msg: String) extends Exception(msg)

case class InvalidStateException(msg: String) extends Exception(msg)
