package com.eve.app.data.model

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

/** Keep malformed document fields from becoming a confirmed editable legal document. */
class AppContentJsonAdapter : TypeAdapterFactory {
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (type.rawType != AppContent::class.java) return null
        val delegate = gson.getDelegateAdapter(this, type)
        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T?) = delegate.write(out, value)
            override fun read(input: JsonReader): T? {
                if (input.peek() == JsonToken.NULL) { input.nextNull(); return null }
                val fields = JsonObject()
                input.beginObject()
                while (input.hasNext()) {
                    val name = input.nextName()
                    if (fields.has(name)) throw JsonParseException("Duplicate document field: $name")
                    fields.add(name, JsonParser.parseReader(input))
                }
                input.endObject()
                for (name in listOf("title", "body")) {
                    val value = fields[name]
                    if (value?.isJsonPrimitive != true || !value.asJsonPrimitive.isString || value.asString.isBlank()) {
                        throw JsonParseException("Missing or invalid document field: $name")
                    }
                }
                for (name in listOf("updatedBy", "supportEmail", "phone", "website", "address")) {
                    fields[name]?.let {
                        if (it.isJsonNull) fields.addProperty(name, "")
                        else if (!it.isJsonPrimitive || !it.asJsonPrimitive.isString) throw JsonParseException("Invalid document field: $name")
                    }
                }
                fields["updatedAt"]?.let {
                    if (it.isJsonNull || !it.isJsonPrimitive || !it.asJsonPrimitive.isNumber) throw JsonParseException("Invalid document timestamp")
                    val timestamp = try { it.asBigDecimal.longValueExact() }
                    catch (e: ArithmeticException) { throw JsonParseException("Invalid document timestamp", e) }
                    if (timestamp < 0) throw JsonParseException("Invalid document timestamp")
                    fields.addProperty("updatedAt", timestamp)
                }
                return delegate.fromJsonTree(fields)
            }
        }
    }
}
