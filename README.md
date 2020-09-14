# kaa-schema-registry

![Scala CI](https://github.com/davideicardi/kaa/workflows/Scala%20CI/badge.svg)

(Kafka Avro4s Schema Registry)

Scala client library that provide an Avro schema registry with Kafka persistency and [Avro4s](https://github.com/sksamuel/avro4s) serializer support.
It allows to share avro schemas across multiple applications without third party software (it can replace [Confluent Schema Registry](https://github.com/confluentinc/schema-registry)). You can use this library with your Kafka client app without calling an external service for schema resolution.

For serialization, [Single object AVRO encoding](https://avro.apache.org/docs/current/spec.html#single_object_encoding) is used to reduce records size, only a schema id (hash) is persisted within the record.  

## Usage

Official releases (published in Maven Central):

```sbt
libraryDependencies += "com.davideicardi" %% "kaa" % "<version>"
```

Packages are also available in Sonatype, also with snapshots versions:

```sbt
externalResolvers += Resolver.sonatypeRepo("snapshots")
// externalResolvers += Resolver.sonatypeRepo("public") // for official releases
```

Using `AvroSingleObjectSerializer`:

```scala
// create the topic
val admin = new KaaSchemaRegistryAdmin(brokers)
if (!admin.topicExists()) admin.createTopic()

// create the schema registry
val schemaRegistry = new KaaSchemaRegistry(brokers)
try {
    // create the serializer
    val serializerV1 = new AvroSingleObjectSerializer[SuperheroV1](schemaRegistry)

    // serialize
    val bytesV1 = serializerV1.serialize(SuperheroV1("Spiderman"))

    // deserialize
    val result = serializerV1.deserialize(bytesV1)
    println(result)
} finally {
    schemaRegistry.shutdown()
}

case class SuperheroV1(name: String)
```

## Features

Kaa provides essentially 3 features:

- `com.davideicardi.kaa.KaaSchemaRegistry`: a simple embeddable schema registry that read and write schemas to Kafka
- `com.davideicardi.kaa.avro.AvroSingleObjectSerializer`: an avro serializer/deserializer based on Avro4s that internally uses `KaaSchemaRegistry`
- `com.davideicardi.kaa.kafka.GenericSerde[T]` an implementation of Kafka's `Serde[T]` based on `AvroSingleObjectSerializer`, that can be used with Kafka Stream

During serialization a schema hash is generated and stored inside Kafka with the schema (key=hash, value=schema).
When deserializing the schema is retrieved from Kafka and used for the deserialization.
`KaaSchemaRegistry` internally runs a Kafka consumer to read all schemas that will be cached in memory.

You can use `com.davideicardi.kaa.KaaSchemaRegistryAdmin` to programmatically create Kafka's schema topic.
NOTE: if you want to create the topic manually, remember to put cleanup policy to `compact` to maintain all the schemas.

## Credits

- Avro: https://avro.apache.org/
  - Single object encoding: https://avro.apache.org/docs/current/spec.html#single_object_encoding
- Avro4s: https://github.com/sksamuel/avro4s
- Kafka: https://kafka.apache.org/
- Avro formats: https://gist.github.com/davideicardi/e8c5a69b98e2a0f18867b637069d03a9
- Agile Lab's Darwin Schema Registry: https://github.com/agile-lab-dev/darwin
- Confluent's Schema Registry: https://github.com/confluentinc/schema-registry

## Contributing

Run unit tests:

```
sbt test
```

Run integration tests:

```
docker-compose up -d
sbt it:test
docker-compose down
```
