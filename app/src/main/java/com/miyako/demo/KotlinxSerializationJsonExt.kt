package com.miyako.demo

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializerOrNull
import kotlin.jvm.Throws

object AnySerializer : KSerializer<Any> {
  override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Any")

  override fun serialize(encoder: Encoder, value: Any) {
    val jsonEncoder = encoder as JsonEncoder
    val jsonElement = serializeAny(value)
    jsonEncoder.encodeJsonElement(jsonElement)
  }

  @Throws(IllegalStateException::class)
  private fun serializeAny(value: Any?): JsonElement {
    return value.toJsonElement()
  }

  override fun deserialize(decoder: Decoder): Any {
    val jsonDecoder = decoder as JsonDecoder
    val element = jsonDecoder.decodeJsonElement()

    return deserializeJsonElement(element)
  }

  private fun deserializeJsonElement(element: JsonElement): Any = when (element) {
    is JsonObject -> element.mapValues { deserializeJsonElement(it.value) }

    is JsonArray -> element.map { deserializeJsonElement(it) }

    is JsonPrimitive -> element.toString()
  }
}

fun Json.anyToJsonElement(any: Any): JsonElement {
  return Json.encodeToJsonElement(AnySerializer, any)
}

fun Json.anyToString(any: Any): String {
  return Json.encodeToString(AnySerializer, any)
}

fun Json.anyFromJsonElement(string: JsonElement): Any {
  return Json.decodeFromJsonElement(AnySerializer, string)
}

fun Json.anyFromString(string: String): Any {
  return Json.decodeFromString(AnySerializer, string)
}

fun Any?.toJsonElement(): JsonElement {
  val serializer = this?.let { Json.serializersModule.serializerOrNull(this::class.java) }
  return if (serializer != null && this != null) {
    Json.encodeToJsonElement(serializer, this)
  } else when (this) {
    null -> JsonNull
    is Map<*, *> -> toJsonElement()
    is Iterable<*> -> toJsonElement()
    is ByteArray -> Json.encodeToJsonElement(ByteArraySerializer(), this)
    is CharArray -> Json.encodeToJsonElement(CharArraySerializer(), this)
    is ShortArray -> Json.encodeToJsonElement(ShortArraySerializer(), this)
    is IntArray -> Json.encodeToJsonElement(IntArraySerializer(), this)
    is LongArray -> Json.encodeToJsonElement(LongArraySerializer(), this)
    is FloatArray -> Json.encodeToJsonElement(FloatArraySerializer(), this)
    is DoubleArray -> Json.encodeToJsonElement(DoubleArraySerializer(), this)
    is BooleanArray -> Json.encodeToJsonElement(BooleanArraySerializer(), this)
    is Array<*> -> toJsonElement()
    is Boolean -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is String -> JsonPrimitive(this)
    is Enum<*> -> JsonPrimitive(this.toString())
    else -> {
      throw IllegalStateException("Can't serialize class: ${this::class.java}")
    }
  }
}

fun Map<*, *>.toJsonElement(): JsonElement {
  val map = mutableMapOf<String, JsonElement>()
  this.forEach { (key, value) ->
    // JSON object key must be String
    map[key.toString()] = value.toJsonElement()
  }
  return JsonObject(map)
}

fun Iterable<*>.toJsonElement(): JsonElement {
  return JsonArray(this.map { it.toJsonElement() })
}

fun Array<*>.toJsonElement(): JsonElement {
  return JsonArray(this.map { it.toJsonElement() })
}
