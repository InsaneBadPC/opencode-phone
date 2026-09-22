package com.example.data.youtube

import java.util.UUID

enum class VideoDraftFormat(val displayName: String, val aspectRatio: String) {
    LONG_FORM("Dlouhé video", "16:9"),
    SHORTS("YouTube Shorts", "9:16")
}

enum class VideoDraftStatus(val label: String) {
    IDEA("💡 Námět"),
    SCRIPTING("✍️ Scénář"),
    RECORDING("🎥 Natáčení"),
    EDITING("✂️ Střih"),
    READY("✅ Připraveno"),
    SCHEDULED("⏰ Naplánováno"),
    PUBLISHED("🚀 Vydáno")
}

data class PublishingSlotRecommendation(
    val dayOfWeek: String,
    val timeString: String,
    val historicalScore: Int, // 0..100
    val engagementLevel: String,
    val rationale: String,
    val unlistedUploadTime: String
)

data class VideoDraft(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val format: VideoDraftFormat = VideoDraftFormat.LONG_FORM,
    val status: VideoDraftStatus = VideoDraftStatus.IDEA,
    val topicNiche: String = "AI & Programování",
    val scheduledSlot: PublishingSlotRecommendation? = null,
    val expectedDurationMinutes: Int = 10,
    val targetCtrGoal: Double = 8.5,
    val hookNotes: String = "",
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class DayEngagementHeatmap(
    val dayName: String,
    val dayShort: String,
    val peakHours: String,
    val heatScore: Int, // 0..100
    val audienceBehavior: String,
    val isRecommendedPrimary: Boolean = false
)

object YouTubeHistoricalData {
    val defaultEngagementHeatmaps = listOf(
        DayEngagementHeatmap(
            dayName = "Pondělí",
            dayShort = "Po",
            peakHours = "18:00 – 20:00",
            heatScore = 68,
            audienceBehavior = "Začátek týdne, kratší sledování, diváci dohánějí pracovní resty."
        ),
        DayEngagementHeatmap(
            dayName = "Úterý",
            dayShort = "Út",
            peakHours = "17:30 – 19:30",
            heatScore = 84,
            audienceBehavior = "Vysoká retence u technických a vzdělávacích tutoriálů."
        ),
        DayEngagementHeatmap(
            dayName = "Středa",
            dayShort = "St",
            peakHours = "17:00 – 19:00",
            heatScore = 78,
            audienceBehavior = "Stabilní vyhledávací provoz (Search Intent), ideální pro recenze a návody."
        ),
        DayEngagementHeatmap(
            dayName = "Čtvrtek",
            dayShort = "Čt",
            peakHours = "17:30 – 20:00",
            heatScore = 96,
            audienceBehavior = "🔥 VIRÁLNÍ ŠPIČKA: Nejvyšší rychlost prokliků (Velocity) před víkendem!",
            isRecommendedPrimary = true
        ),
        DayEngagementHeatmap(
            dayName = "Pátek",
            dayShort = "Pá",
            peakHours = "14:30 – 17:00",
            heatScore = 72,
            audienceBehavior = "Vhodné pro odpolední Shorts; po 19:00 sledovanost klesá kvůli volnočasovým aktivitám."
        ),
        DayEngagementHeatmap(
            dayName = "Sobota",
            dayShort = "So",
            peakHours = "10:30 – 13:00",
            heatScore = 82,
            audienceBehavior = "Dopolední pohodové sledování u kávy, rodinný a hobby obsah."
        ),
        DayEngagementHeatmap(
            dayName = "Neděle",
            dayShort = "Ne",
            peakHours = "15:00 – 18:30",
            heatScore = 93,
            audienceBehavior = "⚡ MASIVNÍ NÁVŠTĚVNOST: Nejdelší průměrná doba sledování (AVD) z celého týdne.",
            isRecommendedPrimary = true
        )
    )

    val topPublishingSlots = listOf(
        PublishingSlotRecommendation(
            dayOfWeek = "Čtvrtek",
            timeString = "17:30",
            historicalScore = 96,
            engagementLevel = "🔥 Virální špička (Top Slot #1)",
            rationale = "Nejvyšší míra okamžitých prokliků v prvních 120 minutách. Algoritmus dostane silný signál pro expanzi do 'Browse Features'.",
            unlistedUploadTime = "15:30 (2 hodiny předem pro 4K transkód)"
        ),
        PublishingSlotRecommendation(
            dayOfWeek = "Neděle",
            timeString = "16:00",
            historicalScore = 93,
            engagementLevel = "⚡ Rekordní retence (Top Slot #2)",
            rationale = "Diváci tráví u videí nejvíce minut bez přeskakování. Perfektní pro hloubková a dlouhá videa nad 12 minut.",
            unlistedUploadTime = "14:00 (2 hodiny předem)"
        ),
        PublishingSlotRecommendation(
            dayOfWeek = "Úterý",
            timeString = "18:00",
            historicalScore = 84,
            engagementLevel = "📈 Vysoká stabilita",
            rationale = "Vhodné pro technické návody a programování. Menší konkurence velkých kanálů než o víkendu.",
            unlistedUploadTime = "16:00 (2 hodiny předem)"
        ),
        PublishingSlotRecommendation(
            dayOfWeek = "Pátek (Shorts)",
            timeString = "15:00",
            historicalScore = 88,
            engagementLevel = "⚡ Shorts Rush",
            rationale = "Optimální slot pro vertikální YouTube Shorts, které fungují jako pozvánka na dlouhé video.",
            unlistedUploadTime = "14:00 (1 hodina předem)"
        )
    )

    fun getInitialDrafts(): List<VideoDraft> = listOf(
        VideoDraft(
            id = "draft_1",
            title = "Jak postavit vlastního AI agenta v Androidu krok za krokem",
            format = VideoDraftFormat.LONG_FORM,
            status = VideoDraftStatus.READY,
            topicNiche = "AI & Programování",
            scheduledSlot = topPublishingSlots[0], // Thursday 17:30
            expectedDurationMinutes = 14,
            targetCtrGoal = 9.2,
            hookNotes = "Začít šokující ukázkou: mobilní telefon píše funkční aplikaci bez lidského zásahu."
        ),
        VideoDraft(
            id = "draft_2",
            title = "Proč 90 % programátorů selže při práci s Dockerem (a jak to vyřešit)",
            format = VideoDraftFormat.LONG_FORM,
            status = VideoDraftStatus.SCRIPTING,
            topicNiche = "Tech & Programování",
            scheduledSlot = topPublishingSlots[1], // Sunday 16:00
            expectedDurationMinutes = 11,
            targetCtrGoal = 8.8,
            hookNotes = "Pattern Interrupt: Smazal jsem produkci kvůli 1 špatnému Dockerfile příkazu."
        ),
        VideoDraft(
            id = "draft_3",
            title = "3 tajné zkratky v Termuxu, které ti ušetří hodiny denně",
            format = VideoDraftFormat.SHORTS,
            status = VideoDraftStatus.EDITING,
            topicNiche = "Termux & Linux",
            scheduledSlot = topPublishingSlots[3], // Friday 15:00 Shorts
            expectedDurationMinutes = 1,
            targetCtrGoal = 12.0,
            hookNotes = "Nekonečná smyčka (Seamless Loop) na konci videa."
        ),
        VideoDraft(
            id = "draft_4",
            title = "Budoucnost programování: Nahradí nás AI v roce 2027?",
            format = VideoDraftFormat.LONG_FORM,
            status = VideoDraftStatus.IDEA,
            topicNiche = "AI & Budoucnost",
            scheduledSlot = null,
            expectedDurationMinutes = 15,
            targetCtrGoal = 10.0,
            hookNotes = "Diskuze s komunitou a anketa předem."
        )
    )

    fun recommendBestSlotForDraft(draft: VideoDraft): PublishingSlotRecommendation {
        return if (draft.format == VideoDraftFormat.SHORTS) {
            topPublishingSlots.find { it.dayOfWeek.contains("Shorts") } ?: topPublishingSlots[0]
        } else if (draft.expectedDurationMinutes >= 12) {
            topPublishingSlots[1] // Sunday 16:00 for long deep-dives
        } else {
            topPublishingSlots[0] // Thursday 17:30 for high velocity
        }
    }
}
