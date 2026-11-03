package com.eve.app.ui.admin

import com.eve.app.data.model.AdminPremiumConfigDto
import com.eve.app.data.remote.ApiResponse
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import org.junit.Assert.*
import org.junit.Test

/** Drives the real UI acknowledgment functions, not regex-only source assertions. */
class AdminPremiumResponseContractsTest {
    private val config = AdminPremiumConfigDto(
        id = "default", planName = "EVE Premium", priceInr = 199,
        currency = "INR", durationDays = 30, sessionExpiryMinutes = 10,
        merchantVpa = "payments@bank", merchantName = "EVE",
        benefits = listOf("Mock tests")
    )

    @Test fun legacyWorkerSnakeCaseConfigIsReadWithoutClientDefaults() {
        val json = """{
          "id":"default","is_enabled":0,"plan_name":"EVE Premium","price_inr":199,
          "currency":"INR","duration_days":30,"is_lifetime":0,
          "description":"","benefits":["Mock tests"],"qr_enabled":1,"upi_enabled":1,
          "session_expiry_minutes":10,"merchant_vpa":"payments@bank","merchant_name":"EVE",
          "isEnabled":false,"isLifetime":false,"qrEnabled":true,"upiEnabled":true
        }"""
        val read = Gson().fromJson(json, AdminPremiumConfigDto::class.java)
        assertTrue(AdminPremiumResponseContracts.configValid(read))
        assertEquals(199, read.priceInr)
        assertEquals("payments@bank", read.merchantVpa)
        assertEquals(30, read.durationDays)
        assertEquals("EVE", read.merchantName)
        assertTrue(AdminPremiumResponseContracts.configMatches(config.copy(isEnabled = false), read))
    }

    @Test fun absentOrMismatchedPremiumConfigCannotBeConfirmed() {
        assertThrows(JsonParseException::class.java) {
            Gson().fromJson("""{"isEnabled":true}""", AdminPremiumConfigDto::class.java)
        }
        val incomplete = Gson().fromJson("""{"isEnabled":true,"isLifetime":false,
            "qrEnabled":true,"upiEnabled":true}""", AdminPremiumConfigDto::class.java)
        assertFalse(AdminPremiumResponseContracts.configValid(incomplete))
        assertFalse(AdminPremiumResponseContracts.configMatches(config, incomplete))
        assertFalse(AdminPremiumResponseContracts.configMatches(config, config.copy(priceInr = 99)))
        assertFalse(AdminPremiumResponseContracts.configMatches(config, config.copy(merchantVpa = "other@bank")))
        assertFalse(AdminPremiumResponseContracts.configMatches(config, null))
    }

    @Test fun workerBooleansAcceptOnlyExplicitBooleanOrLegacyIntegerFlags() {
        val gson = GsonBuilder().setLenient().create() // Production Retrofit configuration.
        for ((name, legacy) in flags) {
            for (key in listOf(name, legacy)) {
                for ((wire, expected) in listOf("true" to true, "false" to false, "1" to true, "0" to false)) {
                    val payload = JsonParser.parseString(gson.toJson(config)).asJsonObject
                    payload.remove(name)
                    payload.add(key, JsonParser.parseString(wire))
                    val response = gson.fromJson<ApiResponse<AdminPremiumConfigDto>>(
                        """{"success":true,"data":$payload}""",
                        object : TypeToken<ApiResponse<AdminPremiumConfigDto>>() {}.type)
                    assertTrue(AdminPremiumResponseContracts.configValid(response.data))
                    val field = JsonParser.parseString(gson.toJson(response.data)).asJsonObject.get(name)
                    assertTrue(field.asJsonPrimitive.isBoolean)
                    assertEquals("$key=$wire", expected, field.asBoolean)
                }
            }
        }
    }

    @Test fun missingNullMalformedAndContradictoryFlagsNeverBecomeAuthoritativeConfig() {
        val gson = GsonBuilder().setLenient().create()
        for ((name, legacy) in flags) {
            for (key in listOf(name, legacy)) {
                for (wire in listOf("null", "2", "-1", "0.5", "1.0", "1e0", "\"1\"", "\"true\"", "\"false\"", "\"garbage\"", "{}", "[]")) {
                    val payload = JsonParser.parseString(gson.toJson(config)).asJsonObject
                    payload.remove(name)
                    payload.add(key, JsonParser.parseString(wire))
                    assertThrows("$key=$wire", JsonParseException::class.java) {
                        gson.fromJson(payload, AdminPremiumConfigDto::class.java)
                    }
                }
            }
            val payload = JsonParser.parseString(gson.toJson(config)).asJsonObject
            payload.remove(name)
            assertThrows("missing $name", JsonParseException::class.java) {
                gson.fromJson(payload, AdminPremiumConfigDto::class.java)
            }
            for (aliases in listOf("\"$name\":true,\"$legacy\":0", "\"$legacy\":1,\"$name\":false",
                "\"$name\":true,\"$name\":false")) {
                val wire = payload.toString().dropLast(1) + "," + aliases + "}"
                assertThrows(aliases, JsonParseException::class.java) {
                    gson.fromJson(wire, AdminPremiumConfigDto::class.java)
                }
            }
        }
        assertFalse(AdminPremiumResponseContracts.configValid(gson.fromJson("null", AdminPremiumConfigDto::class.java)))
    }

    private val flags = listOf("isEnabled" to "is_enabled", "isLifetime" to "is_lifetime",
        "qrEnabled" to "qr_enabled", "upiEnabled" to "upi_enabled")

    @Test fun premiumGrantRequiresServerIssuedActiveEntitlementAndLifetimeConfirmation() {
        val confirmed = ApiResponse<Any>(success = true, data = mapOf(
            "user_id" to "u-1", "is_lifetime" to 1.0,
            "source" to "ADMIN_GRANTED", "status" to "ACTIVE"
        ))
        assertTrue(AdminPremiumResponseContracts.granted(confirmed, true))
        assertFalse(AdminPremiumResponseContracts.granted(ApiResponse(success = true, data = null), true))
        assertFalse(AdminPremiumResponseContracts.granted(ApiResponse(success = false, data = confirmed.data, error = "Denied"), true))
        assertFalse(AdminPremiumResponseContracts.granted(ApiResponse(success = true, data = mapOf<String, Any>(
            "user_id" to "u-1", "is_lifetime" to 0.0,
            "source" to "ADMIN_GRANTED", "status" to "ACTIVE")), true))
        assertFalse(AdminPremiumResponseContracts.granted(ApiResponse(success = true, data = mapOf<String, Any>(
            "user_id" to "", "is_lifetime" to 1.0,
            "source" to "ADMIN_GRANTED", "status" to "ACTIVE")), true))
    }

    @Test fun extensionAndRevocationMustMatchExactlyTheTargetUid() {
        val extended = ApiResponse<Any>(success = true, data = mapOf("userId" to "u-1", "expiresAt" to 2027000000000.0))
        assertTrue(AdminPremiumResponseContracts.extended(extended, "u-1"))
        assertFalse(AdminPremiumResponseContracts.extended(extended, "u-2"))
        assertFalse(AdminPremiumResponseContracts.extended(
            ApiResponse(success = true, data = mapOf<String, Any>("userId" to "u-1", "expiresAt" to -1)), "u-1"))
        val revoked = ApiResponse<Any>(success = true, data = mapOf("userId" to "u-1", "status" to "REVOKED"))
        assertTrue(AdminPremiumResponseContracts.revoked(revoked, "u-1"))
        assertFalse(AdminPremiumResponseContracts.revoked(revoked, "u-2"))
        assertFalse(AdminPremiumResponseContracts.revoked(ApiResponse(success = true,
            data = mapOf<String, Any>("userId" to "u-1", "status" to "ACTIVE")), "u-1"))
    }
}
