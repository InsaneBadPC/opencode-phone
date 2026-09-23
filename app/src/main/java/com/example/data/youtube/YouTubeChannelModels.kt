package com.example.data.youtube

enum class YouTubeAuthType(val label: String) {
    GOOGLE_OAUTH("Google OAuth (YouTube Studio)"),
    YOUTUBE_API_KEY("YouTube Data API v3 Klíč"),
    HANDLE_SYNC("Veřejný kanál (@handle)")
}

data class YouTubeChannelAccount(
    val channelId: String,
    val handle: String,
    val title: String,
    val description: String,
    val customUrl: String,
    val avatarUrl: String,
    val bannerUrl: String? = null,
    val subscriberCount: Long,
    val videoCount: Int,
    val viewCount: Long,
    val authType: YouTubeAuthType,
    val connectedAt: Long = System.currentTimeMillis(),
    val channelEmail: String? = null,
    val isVerified: Boolean = true
)

data class YouTubeVideoItem(
    val id: String,
    val title: String,
    val description: String,
    val publishedAt: String,
    val thumbnailUrl: String,
    val duration: String,
    val isShort: Boolean = false,
    val viewCount: Long,
    val likeCount: Long,
    val commentCount: Long,
    val ctrPercent: Float,
    val avgRetentionPercent: Float,
    val tags: List<String> = emptyList(),
    val privacyStatus: String = "public",
    val aiHealthScore: Int = 85, // 1 - 100
    val optimizationTips: List<String> = emptyList()
)

data class VideoAiAuditResult(
    val videoId: String,
    val overallScore: Int,
    val titleCritique: String,
    val hookCritique: String,
    val recommendedTitles: List<String>,
    val recommendedTags: List<String>,
    val pinnedCommentSuggestion: String,
    val suggestedThumbConcept: String,
    val estimatedCtrBoost: String
)
