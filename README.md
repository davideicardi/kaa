# kaa-schema-registry

(KAfka Avro4s Schema Registry)

Avro serializer for case classes with schemas persisted in Kafka.
It allows to easily share avro schemas across multiple applications and instances
allowing schema evolution.

[Single object AVRO encoding](https://avro.apache.org/docs/current/spec.html#single_object_encoding) is used to reduce records size, only a schema id (hash)
is persisted within the record.  

## Usage

TODO

## Internals

Kaa is implemented as an extension of the Avro4s `GeneridSerde`.
During serialization a schema hash is generated and stored inside Kafka (key=hash, value=schema).
When deserializing the schema is retrieved from Kafka and used for the deserialization.
A Kafka consumer reads all schemas that will be cached in memory.

TODO

## Credits

- Avro: https://avro.apache.org/
  - Single object encoding: https://avro.apache.org/docs/current/spec.html#single_object_encoding
- Avro4s: https://github.com/sksamuel/avro4s
- Kafka: https://kafka.apache.org/
- Avro formats: https://gist.github.com/davideicardi/e8c5a69b98e2a0f18867b637069d03a9
- Agile Lab Darwin for inspiration: https://github.com/agile-lab-dev/darwin

## Contributing

Run unit tests:

```
sbt test
```

Run integration tests:

```
docker-compose up
sbt it:test
```