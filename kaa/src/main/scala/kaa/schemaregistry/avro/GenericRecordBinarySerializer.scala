package kaa.schemaregistry.avro

import org.apache.avro._
import org.apache.avro.generic.GenericRecord

class GenericRecordBinarySerializer() {
  def serialize(record: GenericRecord): Array[Byte] = {
    AvroBinary.write(record, record.getSchema)
  }

  def deserialize(bytes: Array[Byte], writerSchema: Schema, readerSchema: Option[Schema] = None): GenericRecord = {
    AvroBinary.read(bytes, writerSchema, readerSchema).asInstanceOf[GenericRecord]
  }
}
