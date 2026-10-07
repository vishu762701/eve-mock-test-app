package com.eve.app.data.model.uistudio

import com.google.gson.annotations.SerializedName

/**
 * Root configuration schema for EVE UI Studio.
 */
data class UiStudioConfig(
    @SerializedName("schemaVersion")
    val schemaVersion: Int = 1,
    @SerializedName("configVersion")
    val configVersion: Int = 1,
    @SerializedName("version")
    val version: Int = 0,
    @SerializedName("revision")
    val revision: String = "",
    @SerializedName("publishedAt")
    val publishedAt: Long = 0L,
    @SerializedName("publishedBy")
    val publishedBy: String = "",
    @SerializedName("updatedAt")
    val updatedAt: Long = 0L,
    @SerializedName("updatedBy")
    val updatedBy: String = "",
    @SerializedName("notes")
    val notes: String = "",
    @SerializedName("status")
    val status: String = "draft",
    @SerializedName("branding")
    val branding: BrandingConfig = BrandingConfig(),
    @SerializedName("designSystem")
    val designSystem: DesignSystemConfig = DesignSystemConfig(),
    @SerializedName("screens")
    val screens: Map<String, ScreenConfig> = emptyMap()
)

data class BrandingConfig(
    @SerializedName("appDisplayName")
    val appDisplayName: String = "EVE Exam Prep",
    @SerializedName("shortName")
    val shortName: String = "EVE",
    @SerializedName("logoUrl")
    val logoUrl: String? = null,
    @SerializedName("showLogo")
    val showLogo: Boolean = true,
    @SerializedName("logoSize")
    val logoSize: Int = 32,
    @SerializedName("brandColor")
    val brandColor: String = "#007AFF",
    @SerializedName("globalBackgroundColor")
    val globalBackgroundColor: String = "#000000"
)

data class DesignSystemConfig(
    @SerializedName("appBackground")
    val appBackground: String = "#000000",
    @SerializedName("surfaceBackground")
    val surfaceBackground: String = "#1E293B",
    @SerializedName("textPrimary")
    val textPrimary: String = "#FFFFFF",
    @SerializedName("textSecondary")
    val textSecondary: String = "#94A3B8",
    @SerializedName("accentColor")
    val accentColor: String = "#007AFF",
    @SerializedName("successColor")
    val successColor: String = "#34C759",
    @SerializedName("warningColor")
    val warningColor: String = "#FF9500",
    @SerializedName("errorColor")
    val errorColor: String = "#FF3B30",
    @SerializedName("borderColor")
    val borderColor: String = "#334155",
    @SerializedName("dividerColor")
    val dividerColor: String = "#1E293B",
    @SerializedName("radiusScale")
    val radiusScale: Int = 14,
    @SerializedName("spacingScale")
    val spacingScale: Int = 16,
    @SerializedName("defaultOpacity")
    val defaultOpacity: Float = 1.0f,
    @SerializedName("defaultBlurRadius")
    val defaultBlurRadius: Int = 0
)

data class ScreenConfig(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("name")
    val name: String = "",
    @SerializedName("description")
    val description: String = "",
    @SerializedName("backgroundColor")
    val backgroundColor: String? = null,
    @SerializedName("backgroundOpacity")
    val backgroundOpacity: Float? = null,
    @SerializedName("backgroundImageUrl")
    val backgroundImageUrl: String? = null,
    @SerializedName("padding")
    val padding: Int? = null,
    @SerializedName("spacing")
    val spacing: Int? = null,
    @SerializedName("transition")
    val transition: String = "contextual",
    @SerializedName("animation")
    val animation: AnimationProperties = AnimationProperties(),
    @SerializedName("components")
    val components: Map<String, ComponentConfig> = emptyMap()
)

data class ComponentConfig(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("type")
    val type: String = "card",
    @SerializedName("name")
    val name: String = "",
    @SerializedName("parentId")
    val parentId: String? = null,
    @SerializedName("order")
    val order: Int = 0,
    @SerializedName("visible")
    val visible: Boolean = true,
    @SerializedName("enabled")
    val enabled: Boolean = true,
    @SerializedName("isProtected")
    val isProtected: Boolean = false,
    @SerializedName("layout")
    val layout: LayoutProperties = LayoutProperties(),
    @SerializedName("appearance")
    val appearance: AppearanceProperties = AppearanceProperties(),
    @SerializedName("material")
    val material: MaterialProperties = MaterialProperties(),
    @SerializedName("typography")
    val typography: TypographyProperties = TypographyProperties(),
    @SerializedName("content")
    val content: ContentProperties = ContentProperties(),
    @SerializedName("actions")
    val actions: ActionProperties = ActionProperties(),
    @SerializedName("animation")
    val animation: AnimationProperties = AnimationProperties(),
    @SerializedName("states")
    val states: StateProperties = StateProperties(),
    @SerializedName("children")
    val children: List<String> = emptyList()
)

data class LayoutProperties(
    @SerializedName("width")
    val width: String? = null,
    @SerializedName("height")
    val height: String? = null,
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
    val paddingEnd: Int? = null,
    @SerializedName("gravity")
    val gravity: String? = null
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

data class MaterialProperties(
    @SerializedName("blurRadius")
    val blurRadius: Int? = null,
    @SerializedName("materialOpacity")
    val materialOpacity: Float? = null,
    @SerializedName("tintColor")
    val tintColor: String? = null,
    @SerializedName("tintOpacity")
    val tintOpacity: Float? = null
)

data class TypographyProperties(
    @SerializedName("textColor")
    val textColor: String? = null,
    @SerializedName("textSize")
    val textSize: Int? = null,
    @SerializedName("textStyle")
    val textStyle: String? = null,
    @SerializedName("textAlign")
    val textAlign: String? = null,
    @SerializedName("maxLines")
    val maxLines: Int? = null
)

data class ContentProperties(
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("subtitle")
    val subtitle: String? = null,
    @SerializedName("hint")
    val hint: String? = null,
    @SerializedName("imageSource")
    val imageSource: String? = null,
    @SerializedName("icon")
    val icon: String? = null
)

data class ActionProperties(
    @SerializedName("actionType")
    val actionType: String = "none",
    @SerializedName("actionTarget")
    val actionTarget: String? = null
)

data class AnimationProperties(
    @SerializedName("enabled")
    val enabled: Boolean = false,
    @SerializedName("type")
    val type: String = "fade_scale",
    @SerializedName("durationMs")
    val durationMs: Long = 300L,
    @SerializedName("delayMs")
    val delayMs: Long = 0L,
    @SerializedName("interpolator")
    val interpolator: String = "standard"
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
    @SerializedName("revision")
    val revision: String? = null,
    @SerializedName("serverDraft")
    val serverDraft: UiStudioConfig? = null,
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
    val config: UiStudioConfig,
    @SerializedName("baseRevision")
    val baseRevision: String? = null,
    @SerializedName("force")
    val force: Boolean = false
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

data class StateProperties(
    @SerializedName("pressedBackgroundColor")
    val pressedBackgroundColor: String? = null,
    @SerializedName("selectedBackgroundColor")
    val selectedBackgroundColor: String? = null,
    @SerializedName("disabledBackgroundColor")
    val disabledBackgroundColor: String? = null,
    @SerializedName("selectedTextColor")
    val selectedTextColor: String? = null
)

data class UiStudioSessionState(
    @SerializedName("selectedScreenKey")
    val selectedScreenKey: String = "home",
    @SerializedName("selectedComponentKey")
    val selectedComponentKey: String = "hero_banner",
    @SerializedName("selectedTab")
    val selectedTab: String = "design",
    @SerializedName("viewMode")
    val viewMode: String = "split",
    @SerializedName("deviceWidthMode")
    val deviceWidthMode: String = "normal",
    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

sealed class SaveDraftResult {
    data class ServerSuccess(val config: UiStudioConfig) : SaveDraftResult()
    data class LocalOfflineSuccess(val config: UiStudioConfig, val error: String) : SaveDraftResult()
    data class Conflict(val serverDraft: UiStudioConfig?, val message: String) : SaveDraftResult()
    data class Failure(val error: String) : SaveDraftResult()
}

sealed class PublishResult {
    data class VerifiedSuccess(val version: Int, val publishedAt: Long, val config: UiStudioConfig) : PublishResult()
    data class VerificationFailed(val version: Int, val reason: String) : PublishResult()
    data class NetworkFailure(val error: String) : PublishResult()
}
