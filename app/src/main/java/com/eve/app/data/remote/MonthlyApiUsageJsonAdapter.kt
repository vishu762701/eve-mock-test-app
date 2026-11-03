package com.eve.app.data.remote

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

/** Scope strict counter presence/types to the authoritative usage response only. */
class MonthlyApiUsageJsonAdapter : TypeAdapterFactory {
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (type.rawType != MonthlyApiUsageDto::class.java) return null
        val delegate = gson.getDelegateAdapter(this, type)
        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T?) = delegate.write(out, value)
            override fun read(input: JsonReader): T? {
                if (input.peek() == JsonToken.NULL) { input.nextNull(); return null }
                val fields = JsonObject()
                input.beginObject()
                while (input.hasNext()) {
                    val name = input.nextName()
                    if (fields.has(name)) throw JsonParseException("Duplicate usage field: $name")
                    fields.add(name, JsonParser.parseReader(input))
                }
                input.endObject()
                for (name in listOf("yearMonth", "monthDisplayName")) {
                    val value = fields[name]
                    if (value?.isJsonPrimitive != true || !value.asJsonPrimitive.isString || value.asString.isBlank()) {
                        throw JsonParseException("Missing or invalid usage field: $name")
                    }
                }
                for (name in listOf("geminiCalls", "geminiSuccessfulCalls", "geminiFailedCalls",
                    "documentsCreated", "testSubmissions", "lastUpdated")) {
                    val value = fields[name]
                    if (value?.isJsonPrimitive != true || !value.asJsonPrimitive.isNumber) {
                        throw JsonParseException("Missing or invalid usage counter: $name")
                    }
                    val count = try { value.asBigDecimal.longValueExact() }
                    catch (e: ArithmeticException) { throw JsonParseException("Invalid usage counter: $name", e) }
                    if (count < 0) throw JsonParseException("Negative usage counter: $name")
                    fields.addProperty(name, count)
                }
                fields["countedSince"]?.takeUnless { it.isJsonNull }?.let {
                    if (!it.isJsonPrimitive || !it.asJsonPrimitive.isString) throw JsonParseException("Invalid usage tracking note")
                }
                return delegate.fromJsonTree(fields)
            }
        }
    }
}
