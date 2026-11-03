package com.eve.app.data.model

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

/** Only the admin config has the Worker's mixed D1/camelCase boolean contract. */
class AdminPremiumConfigJsonAdapter : TypeAdapterFactory {
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (type.rawType != AdminPremiumConfigDto::class.java) return null
        val delegate = gson.getDelegateAdapter(this, type)
        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T?) = delegate.write(out, value)

            override fun read(input: JsonReader): T? {
                if (input.peek() == JsonToken.NULL) {
                    input.nextNull()
                    return null
                }
                // Read members separately so duplicate keys cannot hide conflicting values.
                val fields = JsonObject()
                input.beginObject()
                while (input.hasNext()) {
                    val name = input.nextName()
                    if (fields.has(name)) throw JsonParseException("Duplicate premium config field: $name")
                    fields.add(name, JsonParser.parseReader(input))
                }
                input.endObject()
                for ((canonical, legacy) in BOOLEAN_FIELDS) {
                    if (!fields.has(canonical) && !fields.has(legacy)) {
                        throw JsonParseException("Missing premium config field: $canonical")
                    }
                    val current = fields.get(canonical)?.let { strictBoolean(it, canonical) }
                    val old = fields.get(legacy)?.let { strictBoolean(it, legacy) }
                    if (current != null && old != null && current != old) {
                        throw JsonParseException("Conflicting premium config field: $canonical")
                    }
                    fields.remove(legacy)
                    fields.addProperty(canonical, current ?: old!!)
                }
                return delegate.fromJsonTree(fields)
            }
        }
    }

    private fun strictBoolean(value: JsonElement, name: String): Boolean {
        if (value.isJsonPrimitive) {
            val primitive = value.asJsonPrimitive
            if (primitive.isBoolean) return primitive.asBoolean
            if (primitive.isNumber) {
                // D1 stores integer flags. No coercion of fractions, strings or other numbers.
                when (primitive.asString) {
                    "0" -> return false
                    "1" -> return true
                }
            }
        }
        throw JsonParseException("Invalid premium config boolean: $name")
    }

    private companion object {
        val BOOLEAN_FIELDS = listOf(
            "isEnabled" to "is_enabled", "isLifetime" to "is_lifetime",
            "qrEnabled" to "qr_enabled", "upiEnabled" to "upi_enabled"
        )
    }
}
