package kaa.schemaregistry


class InvalidSchemaException(s:String) extends Exception(s){}

class SchemaNotFoundException(s:String) extends Exception(s){}