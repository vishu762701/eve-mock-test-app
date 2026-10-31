package com.eve.app.data.model

data class EmojiPlacement(
    val id: String = "",
    val url: String = "",
    val kind: String = "lottie",
    val alt: String = ""
)

data class EmojiPack(
    val id: String = "",
    val name: String = "",
    val title: String = "",
    val source: String = "",
    val licenseAttribution: String? = null,
    val enabled: Boolean = true,
    val itemCount: Int = 0,
    val createdAt: Long = 0L,
    val createdBy: String = ""
)

data class EmojiItem(
    val id: String = "",
    val packId: String = "",
    val packTitle: String? = null,
    val packSlug: String? = null,
    val altEmoji: String = "",
    val kind: String = "lottie",
    val url: String = "",
    val storageKey: String? = null,
    val byteSize: Int = 0,
    val sha256: String? = null,
    val enabled: Boolean = true,
    val premiumOrigin: Boolean = false,
    val telegramCustomEmojiId: String? = null,
    val source: String = "",
    val createdAt: Long = 0L
)

data class EmojiPlacementAdmin(
    val id: String = "",
    val slotType: String = "",
    val targetId: String = "",
    val itemId: String = "",
    val altEmoji: String = "",
    val kind: String = "lottie",
    val url: String = "",
    val packTitle: String? = null,
    val updatedBy: String = "",
    val updatedAt: Long = 0L
)
