# kaa-schema-registry

![Scala CI](https://github.com/davideicardi/kaa/workflows/Scala%20CI/badge.svg)

(Kafka Avro4s Schema Registry)

Scala client library that provide an Avro schema registry with Kafka persistency and [Avro4s](https://github.com/sksamuel/avro4s) serializer support.
It allows resolving AVRO schemas across multiple applications. It can be used instead of [Confluent Schema Registry](https://github.com/confluentinc/schema-registry).

For serialization, [Single object AVRO encoding](https://avro.apache.org/docs/current/spec.html#single_object_encoding) is used, where only a schema id (hash) is persisted within the record.
Thanks to Avro4s, all Scala case classes and primitive types can be serialized and deserialized. 

## Features

- `kaa.schemaregistry.KaaSchemaRegistry`: a simple embeddable schema registry that read and write schemas to Kafka
- `kaa.schemaregistry.avro.SingleObjectSerializer[T]`: an avro serializer/deserializer for case classes, based on Avro4s, that internally uses `KaaSchemaRegistry` for schema resolution
- `kaa.schemaregistry.avro.GenericRecordSingleObjectSerializer`: an avro serializer/deserializer for `GenericRecord` classes that internally uses `KaaSchemaRegistry` for schema resolution
- `kaa.schemaregistry.kafka.KaaSerde[T]`: an implementation of Kafka's `Serde[T]` based on `SingleObjectSerializer`, that can be used with Kafka Stream
- `kaa.schemaregistry.kafka.KaaGenericRecordSerde[T]`: an implementation of Kafka's `Serde[T]` based on `GenericRecordSingleObjectSerializer`, that can be used with Kafka Stream

During serialization, a schema hash is generated and stored inside Kafka with the schema (key=hash, value=schema).
When deserializing the schema is retrieved from Kafka and used for the deserialization.
`KaaSchemaRegistry` internally runs a Kafka consumer to read all schemas that will be cached in memory.

You can use `kaa.schemaregistry.KaaSchemaRegistryAdmin` to programmatically create Kafka's schema topic.
NOTE: if you want to create the topic manually, remember to put cleanup policy to `compact` to maintain all the schemas.

## Why

The main advantage of Kaa is that it doesn't require an external services to retrieve schemas. 
This library automatically reads and writes to Kafka. This can simplify installation and configuration of client applications
and it is especially useful for applications that already interact with Kafka.

Confluent Schema Registry on the other end requires you to install a dedicated service.

## Prerequisites

Compiled with:

- Scala 2.12, 2.13
- Kafka 2.4
- Avro4s 4.0

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

### Create the schema topic

You can use `KaaSchemaRegistryAdmin` to create the Kafka topic to store schema the first time.

```scala
// create the topic
val admin = new KaaSchemaRegistryAdmin(brokers)
try {
  if (!admin.topicExists()) admin.createTopic()
} finally {
  admin.close()
}
```

### Setup the schema registry

Just create an instance of `KaaSchemaRegistry` and call the `start` function.
Remember to close it calling `close` at the end.

```scala
// create the schema registry
val schemaRegistry = new KaaSchemaRegistry(brokers, e => println(e))
try {
  schemaRegistry.start()

  // put your code here
  
} finally {
  schemaRegistry.close()
}
```

### Using `SingleObjectSerializer` directly

```scala
case class SuperheroV1(name: String)

// given a schema registry
val schemaRegistry: KaaSchemaRegistry = ???

// create the serializer
val serializerV1 = new SingleObjectSerializer[SuperheroV1](schemaRegistry)
// serialize
val bytesV1 = serializerV1.serialize(SuperheroV1("Spiderman"))
// deserialize
val result = serializerV1.deserialize(bytesV1)
```

### Kafka usage

All you have to do is to import `KaaSerde` and add an implicit `SchemaRegistry`.

```scala
// Import the serializer
import kaa.schemaregistry.kafka.KaaSerde._
// Import scala implicit conversions
import org.apache.kafka.streams.scala.ImplicitConversions._

// Define an implicit schema registry
implicit val schemaRegistry: SchemaRegistry = ???
```

After that you can directly define your case classes and use Kafka Streams functions, like:

```scala
streamsBuilder.stream[TKey, TValue]("topicName")
```

For a complete example see [es4kafka](https://github.com/davideicardi/es4kafka).

## Http schema server

For a simple server implementation see `./kaa-registry-server`.

Use `GET /schemas/ids/{schemaId}` method to retrieve schemas. 

## See also

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

Run example application:

```
docker-compose up -d
sbt sample/run
docker-compose down
```

Run http server:

```
sbt kaaRegistryServer/run
```