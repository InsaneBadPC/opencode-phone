package com.example.data.models

data class AiModel(
    val id: String,
    val displayName: String,
    val providerId: String,
    val providerName: String,
    val apiModelId: String,
    val description: String,
    val contextWindow: String = "128k",
    val isFree: Boolean = true,
    val isReasoning: Boolean = false,
    val tag: String = "Code", // "Code", "Fast", "Reasoning", "General"
    val freeTierNote: String = "Free tier"
)

data class AiProvider(
    val id: String,
    val name: String,
    val iconEmoji: String,
    val badge: String,
    val description: String,
    val freeTierDetails: String,
    val apiKeyUrl: String,
    val keyPlaceholder: String,
    val requiresKey: Boolean,
    val defaultBaseUrl: String,
    val isEnabled: Boolean = true,
    val apiKey: String = "",
    val customBaseUrl: String = "",
    val models: List<AiModel> = emptyList()
) {
    val effectiveBaseUrl: String
        get() = if (customBaseUrl.isNotBlank()) customBaseUrl.trim() else defaultBaseUrl

    // Determines if this provider is currently available to supply models to the user
    val isAvailable: Boolean
        get() {
            if (!isEnabled) return false
            if (id == "google_gemini") return true // Default built-in AI Studio key exists
            if (!requiresKey) return true // Free offline like Ollama
            return apiKey.isNotBlank()
        }
}

object AiProviderCatalog {
    fun getDefaultProviders(): List<AiProvider> {
        return listOf(
            // 1. Google Gemini
            AiProvider(
                id = "google_gemini",
                name = "Google AI Studio (Gemini)",
                iconEmoji = "✨",
                badge = "Free Tier (15 RPM)",
                description = "Oficiální bezplatný přístup od Google AI Studio s 15 RPM a 1M tokeny denně.",
                freeTierDetails = "Bezplatný limit 15 požadavků/min, 1M tokenů kontextové okno, multimodální porozumění. Není nutná platební karta.",
                apiKeyUrl = "https://aistudio.google.com/app/apikey",
                keyPlaceholder = "AIzaSy... (nebo nechte prázdné pro vestavěný klíč)",
                requiresKey = false,
                defaultBaseUrl = "https://generativelanguage.googleapis.com/v1beta",
                models = listOf(
                    AiModel(
                        id = "gemini-2.5-flash",
                        displayName = "Gemini 2.5 Flash",
                        providerId = "google_gemini",
                        providerName = "Google AI Studio",
                        apiModelId = "gemini-2.5-flash",
                        description = "Ultrarychlý model nové generace s 1M kontextem a bleskovou syntézou kódu.",
                        contextWindow = "1M",
                        isReasoning = false,
                        tag = "Fast & Code",
                        freeTierNote = "15 RPM zdarma"
                    ),
                    AiModel(
                        id = "gemini-2.5-pro",
                        displayName = "Gemini 2.5 Pro",
                        providerId = "google_gemini",
                        providerName = "Google AI Studio",
                        apiModelId = "gemini-2.5-pro",
                        description = "Špičkový model pro komplexní logiku, systémovou architekturu a refaktoring.",
                        contextWindow = "2M",
                        isReasoning = true,
                        tag = "Reasoning & Arch",
                        freeTierNote = "2 RPM zdarma"
                    ),
                    AiModel(
                        id = "gemini-2.0-flash",
                        displayName = "Gemini 2.0 Flash",
                        providerId = "google_gemini",
                        providerName = "Google AI Studio",
                        apiModelId = "gemini-2.0-flash",
                        description = "Stabilní a rychlý model pro každodenní vývoj a rychlé dotazy.",
                        contextWindow = "1M",
                        isReasoning = false,
                        tag = "Fast",
                        freeTierNote = "15 RPM zdarma"
                    ),
                    AiModel(
                        id = "gemini-2.0-flash-thinking",
                        displayName = "Gemini 2.0 Flash Thinking",
                        providerId = "google_gemini",
                        providerName = "Google AI Studio",
                        apiModelId = "gemini-2.0-flash-thinking-exp",
                        description = "Experimentální model zobrazující myšlenkový řetězec před vygenerováním odpovědi.",
                        contextWindow = "1M",
                        isReasoning = true,
                        tag = "Thinking",
                        freeTierNote = "10 RPM zdarma"
                    ),
                    AiModel(
                        id = "gemini-1.5-pro",
                        displayName = "Gemini 1.5 Pro",
                        providerId = "google_gemini",
                        providerName = "Google AI Studio",
                        apiModelId = "gemini-1.5-pro",
                        description = "Osvědčený velký model s hlubokou analýzou celých repozitářů.",
                        contextWindow = "2M",
                        isReasoning = false,
                        tag = "Large Context",
                        freeTierNote = "2 RPM zdarma"
                    )
                )
            ),

            // 2. Groq Cloud
            AiProvider(
                id = "groq",
                name = "Groq Cloud",
                iconEmoji = "⚡",
                badge = "Free Tier (Ultra-Fast LPU)",
                description = "Extrémně rychlá inference stovek tokenů/s na LPU procesorech.",
                freeTierDetails = "Bezplatný přístup s limitem 30 RPM pro open-source modely. Blesková odezva pod 200 ms.",
                apiKeyUrl = "https://console.groq.com/keys",
                keyPlaceholder = "gsk_...",
                requiresKey = true,
                defaultBaseUrl = "https://api.groq.com/openai/v1",
                models = listOf(
                    AiModel(
                        id = "groq-llama-3.3-70b",
                        displayName = "Llama 3.3 70B Versatile",
                        providerId = "groq",
                        providerName = "Groq",
                        apiModelId = "llama-3.3-70b-versatile",
                        description = "Vlajkový open-source model pro programování, algoritmy a analýzu kódu.",
                        contextWindow = "128k",
                        isReasoning = false,
                        tag = "Code & General",
                        freeTierNote = "30 RPM zdarma"
                    ),
                    AiModel(
                        id = "groq-deepseek-r1-70b",
                        displayName = "DeepSeek R1 Distill 70B",
                        providerId = "groq",
                        providerName = "Groq",
                        apiModelId = "deepseek-r1-distill-llama-70b",
                        description = "Uvažovací model s detailním myšlenkovým postupem na hardwaru Groq.",
                        contextWindow = "128k",
                        isReasoning = true,
                        tag = "Reasoning",
                        freeTierNote = "30 RPM zdarma"
                    ),
                    AiModel(
                        id = "groq-llama-3.1-8b",
                        displayName = "Llama 3.1 8B Instant",
                        providerId = "groq",
                        providerName = "Groq",
                        apiModelId = "llama-3.1-8b-instant",
                        description = "Okamžitá odezva (500+ tokenů/s) pro autokompletaci a rychlé odpovědi.",
                        contextWindow = "128k",
                        isReasoning = false,
                        tag = "Ultra-Fast",
                        freeTierNote = "30 RPM zdarma"
                    ),
                    AiModel(
                        id = "groq-mixtral-8x7b",
                        displayName = "Mixtral 8x7B",
                        providerId = "groq",
                        providerName = "Groq",
                        apiModelId = "mixtral-8x7b-32768",
                        description = "Výkonná směs expertů (MoE) s 32k kontextovým oknem.",
                        contextWindow = "32k",
                        isReasoning = false,
                        tag = "MoE",
                        freeTierNote = "30 RPM zdarma"
                    )
                )
            ),

            // 3. OpenRouter
            AiProvider(
                id = "openrouter",
                name = "OpenRouter",
                iconEmoji = "🌐",
                badge = "Free Modely (:free)",
                description = "Agregátor desítek AI modelů s bezplatnou variantou bez nutnosti kreditní karty.",
                freeTierDetails = "Všechny modely s příponou :free jsou k dispozici zcela zdarma. Zadejte bezplatný klíč z openrouter.ai.",
                apiKeyUrl = "https://openrouter.ai/keys",
                keyPlaceholder = "sk-or-v1-...",
                requiresKey = true,
                defaultBaseUrl = "https://openrouter.ai/api/v1",
                models = listOf(
                    AiModel(
                        id = "openrouter-deepseek-r1-free",
                        displayName = "DeepSeek R1 (Free)",
                        providerId = "openrouter",
                        providerName = "OpenRouter",
                        apiModelId = "deepseek/deepseek-r1:free",
                        description = "Plnohodnotný uvažovací model s 671B parametry zcela zdarma.",
                        contextWindow = "64k",
                        isReasoning = true,
                        tag = "Reasoning",
                        freeTierNote = "100% Free"
                    ),
                    AiModel(
                        id = "openrouter-llama-3.3-70b-free",
                        displayName = "Llama 3.3 70B (Free)",
                        providerId = "openrouter",
                        providerName = "OpenRouter",
                        apiModelId = "meta-llama/llama-3.3-70b-instruct:free",
                        description = "Meta Llama 3.3 s instrukčním vyladěním pro software engineering.",
                        contextWindow = "128k",
                        isReasoning = false,
                        tag = "Code",
                        freeTierNote = "100% Free"
                    ),
                    AiModel(
                        id = "openrouter-qwen-2.5-coder-free",
                        displayName = "Qwen 2.5 Coder 32B (Free)",
                        providerId = "openrouter",
                        providerName = "OpenRouter",
                        apiModelId = "qwen/qwen-2.5-coder-32b-instruct:free",
                        description = "Jeden z nejlepších open modelů na kódování a generování Jetpack Compose.",
                        contextWindow = "32k",
                        isReasoning = false,
                        tag = "Coder Specialist",
                        freeTierNote = "100% Free"
                    ),
                    AiModel(
                        id = "openrouter-gemini-2.0-flash-free",
                        displayName = "Gemini 2.0 Flash Exp (Free)",
                        providerId = "openrouter",
                        providerName = "OpenRouter",
                        apiModelId = "google/gemini-2.0-flash-exp:free",
                        description = "Experimentální model Google Gemini zprostředkovaný přes OpenRouter.",
                        contextWindow = "32k",
                        isReasoning = false,
                        tag = "Fast",
                        freeTierNote = "100% Free"
                    )
                )
            ),

            // 4. GitHub Models
            AiProvider(
                id = "github_models",
                name = "GitHub Models",
                iconEmoji = "🐙",
                badge = "Free Tier (Personal Token)",
                description = "Bezplatné modely pro vývojáře přímo v infrastruktuře GitHub a Azure AI.",
                freeTierDetails = "Zdarma pro všechny uživatele GitHubu s běžným Personal Access Tokenem (PAT). Žádné platby.",
                apiKeyUrl = "https://github.com/settings/tokens",
                keyPlaceholder = "ghp_...",
                requiresKey = true,
                defaultBaseUrl = "https://models.inference.ai.azure.com",
                models = listOf(
                    AiModel(
                        id = "gh-gpt-4o-mini",
                        displayName = "GPT-4o mini (GitHub)",
                        providerId = "github_models",
                        providerName = "GitHub Models",
                        apiModelId = "gpt-4o-mini",
                        description = "Rychlý a přesný model OpenAI dostupný přes bezplatný GitHub token.",
                        contextWindow = "128k",
                        isReasoning = false,
                        tag = "Fast & General",
                        freeTierNote = "Free pro GitHub uživatele"
                    ),
                    AiModel(
                        id = "gh-o3-mini",
                        displayName = "OpenAI o3-mini (GitHub)",
                        providerId = "github_models",
                        providerName = "GitHub Models",
                        apiModelId = "o3-mini",
                        description = "Nový uvažovací STEM model od OpenAI pro náročné programátorské úlohy.",
                        contextWindow = "128k",
                        isReasoning = true,
                        tag = "Reasoning",
                        freeTierNote = "Free pro GitHub uživatele"
                    ),
                    AiModel(
                        id = "gh-llama-3.3-70b",
                        displayName = "Meta Llama 3.3 70B (GitHub)",
                        providerId = "github_models",
                        providerName = "GitHub Models",
                        apiModelId = "Meta-Llama-3.3-70B-Instruct",
                        description = "Hostovaný model od Meta AI na Azure cloudu.",
                        contextWindow = "128k",
                        isReasoning = false,
                        tag = "Code",
                        freeTierNote = "Free pro GitHub uživatele"
                    ),
                    AiModel(
                        id = "gh-phi-4",
                        displayName = "Microsoft Phi-4 (GitHub)",
                        providerId = "github_models",
                        providerName = "GitHub Models",
                        apiModelId = "Phi-4",
                        description = "14B model Microsoftu s výjimečnou schopností syntaktické analýzy.",
                        contextWindow = "16k",
                        isReasoning = false,
                        tag = "Efficient",
                        freeTierNote = "Free pro GitHub uživatele"
                    )
                )
            ),

            // 5. Hugging Face (Inference API)
            AiProvider(
                id = "huggingface",
                name = "Hugging Face",
                iconEmoji = "🤗",
                badge = "Free Serverless API",
                description = "Bezplatné Serverless Inference API pro populární komunitní modely.",
                freeTierDetails = "Zdarma pro registrované uživatele Hugging Face s User Access Tokenem (Read).",
                apiKeyUrl = "https://huggingface.co/settings/tokens",
                keyPlaceholder = "hf_...",
                requiresKey = true,
                defaultBaseUrl = "https://api-inference.huggingface.co/models",
                models = listOf(
                    AiModel(
                        id = "hf-qwen-2.5-coder",
                        displayName = "Qwen 2.5 Coder 32B (HF)",
                        providerId = "huggingface",
                        providerName = "Hugging Face",
                        apiModelId = "Qwen/Qwen2.5-Coder-32B-Instruct",
                        description = "Populární model pro Kotlin, Compose, Python a webový vývoj.",
                        contextWindow = "32k",
                        isReasoning = false,
                        tag = "Coder Specialist",
                        freeTierNote = "Free Inference API"
                    ),
                    AiModel(
                        id = "hf-deepseek-coder",
                        displayName = "DeepSeek Coder V2 (HF)",
                        providerId = "huggingface",
                        providerName = "Hugging Face",
                        apiModelId = "deepseek-ai/DeepSeek-Coder-V2-Lite-Instruct",
                        description = "Ověřený kódovací model podporující 300+ programovacích jazyků.",
                        contextWindow = "32k",
                        isReasoning = false,
                        tag = "Code",
                        freeTierNote = "Free Inference API"
                    ),
                    AiModel(
                        id = "hf-mistral-7b",
                        displayName = "Mistral 7B Instruct v0.3",
                        providerId = "huggingface",
                        providerName = "Hugging Face",
                        apiModelId = "mistralai/Mistral-7B-Instruct-v0.3",
                        description = "Kompaktní model pro rychlou asistenci a dokumentaci.",
                        contextWindow = "32k",
                        isReasoning = false,
                        tag = "Fast",
                        freeTierNote = "Free Inference API"
                    )
                )
            ),

            // 6. Mistral AI (La Plateforme)
            AiProvider(
                id = "mistral",
                name = "Mistral AI",
                iconEmoji = "🌊",
                badge = "Free Experimentation Tier",
                description = "Evropský AI šampión s bezplatným experimentálním přístupem k API.",
                freeTierDetails = "Bezplatný vývojářský tier na La Plateforme s limitem 1 req/s a úvodním kreditem.",
                apiKeyUrl = "https://console.mistral.ai/api-keys/",
                keyPlaceholder = "...",
                requiresKey = true,
                defaultBaseUrl = "https://api.mistral.ai/v1",
                models = listOf(
                    AiModel(
                        id = "mistral-codestral",
                        displayName = "Codestral Latest",
                        providerId = "mistral",
                        providerName = "Mistral AI",
                        apiModelId = "codestral-latest",
                        description = "Vlajkový kódovací model s podporou Fill-in-the-Middle a 256k kontextem.",
                        contextWindow = "256k",
                        isReasoning = false,
                        tag = "Coder Specialist",
                        freeTierNote = "Free Dev Tier"
                    ),
                    AiModel(
                        id = "mistral-small",
                        displayName = "Mistral Small Latest",
                        providerId = "mistral",
                        providerName = "Mistral AI",
                        apiModelId = "mistral-small-latest",
                        description = "Rychlý model s vynikajícím poměrem výkonu a latence.",
                        contextWindow = "32k",
                        isReasoning = false,
                        tag = "Fast",
                        freeTierNote = "Free Dev Tier"
                    ),
                    AiModel(
                        id = "mistral-nemo",
                        displayName = "Mistral NeMo 12B",
                        providerId = "mistral",
                        providerName = "Mistral AI",
                        apiModelId = "open-mistral-nemo",
                        description = "12B model vyvinutý s NVIDIA pro efektivní kódování a 128k kontext.",
                        contextWindow = "128k",
                        isReasoning = false,
                        tag = "Efficient",
                        freeTierNote = "Free Dev Tier"
                    )
                )
            ),

            // 7. Cohere
            AiProvider(
                id = "cohere",
                name = "Cohere",
                iconEmoji = "🌿",
                badge = "Free Trial Key",
                description = "Podnikové modely s bezplatným Trial API klíčem pro vývojáře.",
                freeTierDetails = "Bezplatný Trial API klíč s měsíčním limitem 1 000 volání zdarma na dashboard.cohere.com.",
                apiKeyUrl = "https://dashboard.cohere.com/api-keys",
                keyPlaceholder = "...",
                requiresKey = true,
                defaultBaseUrl = "https://api.cohere.ai/v1",
                models = listOf(
                    AiModel(
                        id = "cohere-command-r-plus",
                        displayName = "Command R+",
                        providerId = "cohere",
                        providerName = "Cohere",
                        apiModelId = "command-r-plus",
                        description = "Optimalizovaný model pro vícekrokové nástroje a prohledávání repozitářů.",
                        contextWindow = "128k",
                        isReasoning = false,
                        tag = "Tools & Code",
                        freeTierNote = "1 000 volání/měsíc free"
                    ),
                    AiModel(
                        id = "cohere-command-r",
                        displayName = "Command R",
                        providerId = "cohere",
                        providerName = "Cohere",
                        apiModelId = "command-r",
                        description = "Svižný model s excelentním porozuměním dlouhým textům.",
                        contextWindow = "128k",
                        isReasoning = false,
                        tag = "Fast",
                        freeTierNote = "1 000 volání/měsíc free"
                    )
                )
            ),

            // 8. DeepSeek API
            AiProvider(
                id = "deepseek",
                name = "DeepSeek API",
                iconEmoji = "🐳",
                badge = "Free Trial Credit",
                description = "Přímý přístup k originálnímu API modelů DeepSeek-V3 a DeepSeek-R1.",
                freeTierDetails = "Nové účty získávají bezplatný startovní kredit pro vyzkoušení API na platform.deepseek.com.",
                apiKeyUrl = "https://platform.deepseek.com/api_keys",
                keyPlaceholder = "sk-...",
                requiresKey = true,
                defaultBaseUrl = "https://api.deepseek.com",
                models = listOf(
                    AiModel(
                        id = "deepseek-v3",
                        displayName = "DeepSeek-V3",
                        providerId = "deepseek",
                        providerName = "DeepSeek API",
                        apiModelId = "deepseek-chat",
                        description = "Nejnovější 671B MoE model nabízející výjimečnou programovací úroveň.",
                        contextWindow = "64k",
                        isReasoning = false,
                        tag = "Coder & General",
                        freeTierNote = "Free trial kredit"
                    ),
                    AiModel(
                        id = "deepseek-r1",
                        displayName = "DeepSeek-R1",
                        providerId = "deepseek",
                        providerName = "DeepSeek API",
                        apiModelId = "deepseek-reasoner",
                        description = "Hluboké uvažování s kompletním řetězcem myšlenek (Chain-of-Thought).",
                        contextWindow = "64k",
                        isReasoning = true,
                        tag = "Deep Reasoning",
                        freeTierNote = "Free trial kredit"
                    )
                )
            ),

            // 9. Together AI
            AiProvider(
                id = "together",
                name = "Together AI",
                iconEmoji = "🤝",
                badge = "Free $5 Credit",
                description = "Cloudová platforma nabízející $5 bezplatný uvítací kredit pro open-source modely.",
                freeTierDetails = "Bezplatný uvítací kredit $5 pro všechny nové registrace. Široký výběr modelů Llama a Qwen.",
                apiKeyUrl = "https://api.together.ai/settings/api-keys",
                keyPlaceholder = "...",
                requiresKey = true,
                defaultBaseUrl = "https://api.together.xyz/v1",
                models = listOf(
                    AiModel(
                        id = "together-llama-3.3-70b-turbo",
                        displayName = "Llama 3.3 70B Turbo",
                        providerId = "together",
                        providerName = "Together AI",
                        apiModelId = "meta-llama/Llama-3.3-70B-Instruct-Turbo",
                        description = "Nízko-latenční inference optimalizovaná pro vývoj aplikací.",
                        contextWindow = "128k",
                        isReasoning = false,
                        tag = "Code & Turbo",
                        freeTierNote = "$5 free credit"
                    ),
                    AiModel(
                        id = "together-qwen-72b-turbo",
                        displayName = "Qwen 2.5 72B Turbo",
                        providerId = "together",
                        providerName = "Together AI",
                        apiModelId = "Qwen/Qwen2.5-72B-Instruct-Turbo",
                        description = "Masivní 72B model pro pokročilou logiku a syntézu kódu.",
                        contextWindow = "32k",
                        isReasoning = false,
                        tag = "Large & Capable",
                        freeTierNote = "$5 free credit"
                    )
                )
            ),

            // 10. Ollama / Lokální AI
            AiProvider(
                id = "ollama_local",
                name = "Ollama / Lokální AI",
                iconEmoji = "🦙",
                badge = "100% Free & Offline",
                description = "Zcela bezplatné a privátní modely běžící lokálně (localhost nebo LAN IP).",
                freeTierDetails = "100% zdarma bez limitů a bez nutnosti posílat data do cloudu. Výchozí URL: http://localhost:11434.",
                apiKeyUrl = "https://ollama.com",
                keyPlaceholder = "Nepovinné (pro Ollamu není klíč třeba)",
                requiresKey = false,
                defaultBaseUrl = "http://localhost:11434/v1",
                models = listOf(
                    AiModel(
                        id = "ollama-qwen-coder",
                        displayName = "Qwen 2.5 Coder (Local)",
                        providerId = "ollama_local",
                        providerName = "Ollama / Lokální",
                        apiModelId = "qwen2.5-coder:latest",
                        description = "Lokální specialista na programování a Android Compose.",
                        contextWindow = "32k",
                        isReasoning = false,
                        tag = "Offline Coder",
                        freeTierNote = "100% Zdarma offline"
                    ),
                    AiModel(
                        id = "ollama-deepseek-r1-8b",
                        displayName = "DeepSeek-R1 8B (Local)",
                        providerId = "ollama_local",
                        providerName = "Ollama / Lokální",
                        apiModelId = "deepseek-r1:8b",
                        description = "Lokální reasoning model schopný logického uvažování na zařízení.",
                        contextWindow = "32k",
                        isReasoning = true,
                        tag = "Offline Reasoning",
                        freeTierNote = "100% Zdarma offline"
                    ),
                    AiModel(
                        id = "ollama-llama-3.2",
                        displayName = "Llama 3.2 (Local)",
                        providerId = "ollama_local",
                        providerName = "Ollama / Lokální",
                        apiModelId = "llama3.2:latest",
                        description = "Úsporný model optimalizovaný pro mobilní zařízení a notebooky.",
                        contextWindow = "128k",
                        isReasoning = false,
                        tag = "Offline Fast",
                        freeTierNote = "100% Zdarma offline"
                    ),
                    AiModel(
                        id = "ollama-mistral",
                        displayName = "Mistral 7B (Local)",
                        providerId = "ollama_local",
                        providerName = "Ollama / Lokální",
                        apiModelId = "mistral:latest",
                        description = "Standardní open-weights model pro běžné vývojářské dotazy.",
                        contextWindow = "32k",
                        isReasoning = false,
                        tag = "Offline",
                        freeTierNote = "100% Zdarma offline"
                    )
                )
            )
        )
    }
}
