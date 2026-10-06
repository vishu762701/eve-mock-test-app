package com.eve.app.data.model.uistudio

import com.google.gson.annotations.SerializedName

/**
 * Root configuration schema for EVE UI Studio.
 */
data class UiStudioConfig(
    @SerializedName("version")
    val version: Int = 0,
    @SerializedName("publishedAt")
    val publishedAt: Long = 0L,
    @SerializedName("publishedBy")
    val publishedBy: String = "",
    @SerializedName("notes")
    val notes: String = "",
    @SerializedName("screens")
    val screens: Map<String, ScreenConfig> = emptyMap()
)

data class ScreenConfig(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("name")
    val name: String = "",
    @SerializedName("components")
    val components: Map<String, ComponentConfig> = emptyMap()
)

data class ComponentConfig(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("name")
    val name: String = "",
    @SerializedName("visible")
    val visible: Boolean = true,
    @SerializedName("order")
    val order: Int = 0,
    @SerializedName("layout")
    val layout: LayoutProperties = LayoutProperties(),
    @SerializedName("appearance")
    val appearance: AppearanceProperties = AppearanceProperties(),
    @SerializedName("typography")
    val typography: TypographyProperties = TypographyProperties(),
    @SerializedName("content")
    val content: ContentProperties = ContentProperties()
)

data class LayoutProperties(
    @SerializedName("marginTop")
    val marginTop: Int? = null,
    @SerializedName("marginBottom")
    val marginBottom: Int? = null,
    @SerializedName("marginStart")
    val marginStart: Int? = null,
    @SerializedName("marginEnd")
    val marginEnd: Int? = null,
    @SerializedName("paddingTop")
    val paddingTop: Int? = null,
    @SerializedName("paddingBottom")
    val paddingBottom: Int? = null,
    @SerializedName("paddingStart")
    val paddingStart: Int? = null,
    @SerializedName("paddingEnd")
    val paddingEnd: Int? = null
)

data class AppearanceProperties(
    @SerializedName("backgroundColor")
    val backgroundColor: String? = null,
    @SerializedName("cornerRadius")
    val cornerRadius: Int? = null,
    @SerializedName("strokeWidth")
    val strokeWidth: Int? = null,
    @SerializedName("strokeColor")
    val strokeColor: String? = null,
    @SerializedName("opacity")
    val opacity: Float? = null,
    @SerializedName("elevation")
    val elevation: Int? = null
)

data class TypographyProperties(
    @SerializedName("textColor")
    val textColor: String? = null,
    @SerializedName("textSize")
    val textSize: Int? = null,
    @SerializedName("textStyle")
    val textStyle: String? = null,
    @SerializedName("textAlign")
    val textAlign: String? = null
)

data class ContentProperties(
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("subtitle")
    val subtitle: String? = null
)

// Response & Request DTOs
data class UiStudioPublishedResponse(
    @SerializedName("version")
    val version: Int = 0,
    @SerializedName("publishedAt")
    val publishedAt: Long = 0L,
    @SerializedName("publishedBy")
    val publishedBy: String = "",
    @SerializedName("notes")
    val notes: String = "",
    @SerializedName("config")
    val config: UiStudioConfig? = null
)

data class UiStudioDraftResponse(
    @SerializedName("config")
    val config: UiStudioConfig? = null,
    @SerializedName("updatedAt")
    val updatedAt: Long = 0L,
    @SerializedName("updatedBy")
    val updatedBy: String = "",
    @SerializedName("source")
    val source: String = ""
)

data class UiStudioVersionItem(
    @SerializedName("version")
    val version: Int = 0,
    @SerializedName("created_at")
    val createdAt: Long = 0L,
    @SerializedName("created_by")
    val createdBy: String = "",
    @SerializedName("notes")
    val notes: String? = null
)

data class UiStudioAuditItem(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("action")
    val action: String = "",
    @SerializedName("performed_by")
    val performedBy: String = "",
    @SerializedName("version")
    val version: Int? = null,
    @SerializedName("details")
    val details: String? = null,
    @SerializedName("timestamp")
    val timestamp: Long = 0L
)

data class SaveDraftRequest(
    @SerializedName("config")
    val config: UiStudioConfig
)

data class PublishStudioRequest(
    @SerializedName("notes")
    val notes: String = "",
    @SerializedName("config")
    val config: UiStudioConfig? = null
)

data class RestoreStudioRequest(
    @SerializedName("target")
    val target: String = "draft",
    @SerializedName("notes")
    val notes: String = ""
)

data class ResetStudioRequest(
    @SerializedName("target")
    val target: String = "draft"
)
