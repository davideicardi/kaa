# kaa-schema-registry

(KAfka Avro4s Schema Registry)

Avro case class serializer with schemas persisted in Kafka.
It allows to easily share avro schemas across multiple applications and instances
allowing schema evolution. Row-based AVRO format is used to reduce records size, only a schema id (hash)
is persisted with the record.  

## Usage

TODO

## Internals

Kaa is implemented as an extension of the Avro4s `GeneridSerde`.
Every time a case class is serialized the schema hash is generated and stored inside Kafka.
When record is deserilized the schema is extracted from Kafka and used for the deserialization.

A Kafka consumer is used to reads all schemas that will be cached in memory.

TODO

## Credits

- Avro
  - Avro row based format 
- Avro4s
- Kafka
 
