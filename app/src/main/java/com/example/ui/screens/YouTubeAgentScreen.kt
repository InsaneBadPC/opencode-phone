package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.OpenCodeViewModel
import com.example.ui.components.YouTubeChannelContentView
import com.example.ui.components.YouTubeContentCalendarView
import com.example.ui.components.YouTubeGrowthDashboardCard
import com.example.ui.theme.*

// Data model for YouTube growth pillar
data class YouTubeGrowthPillar(
    val id: String,
    val title: String,
    val impactBadge: String,
    val icon: ImageVector,
    val accentColor: Color,
    val algorithmSecret: String,
    val whatAgentDoes: List<String>,
    val promptTemplate: String,
    val sampleOutputTitle: String,
    val sampleOutputBody: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeAgentScreen(
    viewModel: OpenCodeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Main navigation: 0 = Growth & Strategy, 1 = Content Calendar
    var selectedMainTab by remember { mutableIntStateOf(0) }

    // State for user customizer
    var selectedNiche by remember { mutableStateOf("AI & Programování") }
    var selectedStage by remember { mutableStateOf("1 000 – 10 000 odběratelů") }
    var videoTopicInput by remember { mutableStateOf("Jak vytvořit AI asistenta za 15 minut") }
    var activePillarTab by remember { mutableStateOf<String?>("hook_crafter") }
    var showInteractiveGenerator by remember { mutableStateOf(true) }
    var generatedResult by remember { mutableStateOf<String?>(null) }
    var isGenerating by remember { mutableStateOf(false) }

    val niches = listOf(
        "AI & Programování",
        "Tech & Gadgets",
        "Vzdělávání & How-To",
        "Gaming & Zábava",
        "Business & Finance",
        "Lifestyle & Vlogs"
    )

    val pillars = remember {
        listOf(
            YouTubeGrowthPillar(
                id = "title_generator",
                title = "Psychologie titulků (High-CTR Titles)",
                impactBadge = "+60% až +150% proklik (CTR)",
                icon = Icons.Default.TrendingUp,
                accentColor = Color(0xFFFF0033), // YouTube Red
                algorithmSecret = "YouTube algoritmus doporučuje video na základě poměru CTR (proklik) a AVD (retence). Titulek musí vyvolat zvědavost ('Curiosity Gap') bez laciného clickbaitu.",
                whatAgentDoes = listOf(
                    "Generuje 10 variant titulků rozdělených do 4 stylů: Zvědavost, Negativní varování, 'Příběh/Transformace' a Otázka.",
                    "Hlídá délku do 50 znaků (aby nebyl useknut v mobilní YouTube aplikaci).",
                    "A/B testovací matice: navrhuje který titulek nasadit první 3 hodiny a který zvolit jako záložní, pokud CTR klesne pod 6 %."
                ),
                promptTemplate = "Chci zvýšit sledovanost svého videa na téma '$videoTopicInput' v nise '$selectedNiche'. Vygeneruj mi 10 extrémně chytlavých titulků s vysokým CTR. Rozděl je podle psychologického záměru (Curiosity Gap, Fear of Missing Out, Kontrast) a u každého uveď skóre prokliku.",
                sampleOutputTitle = "Ukázka vygenerovaných titulků pro vysoké CTR:",
                sampleOutputBody = """
1. 🔥 "Přestaňte kódovat bez AI: Udělal jsem tuto fatální chybu" (CTR: 9.4%)
2. ⚡ "Jak vytvořit AI asistenta dřív, než dopijete kafe" (CTR: 8.8%)
3. 🤫 "Tento AI trik před vámi zkušení vývojáři tají..." (CTR: 10.2%)
4. ❌ "Proč 90 % lidí selže při stavbě vlastního asistenta" (CTR: 7.9%)
5. 🚀 "Z 0 na vlastního AI bota za 15 minut (Krok za krokem)" (CTR: 8.5%)
                """.trimIndent()
            ),
            YouTubeGrowthPillar(
                id = "hook_crafter",
                title = "Architekt prvních 30 sekund (Retenční Hook)",
                impactBadge = "+80% retence diváků",
                icon = Icons.Default.Timer,
                accentColor = AmberWarning,
                algorithmSecret = "Až 65 % diváků opustí video v prvních 30 sekundách. Pokud diváka udržíte přes 1. minutu, YouTube začne video masivně nabízet na domovské stránce.",
                whatAgentDoes = listOf(
                    "Eliminuje 'vata' úvody: Zakazuje dlouhá rotující loga, znělky a 'Ahoj, nezapomeňte dát like a odběr'.",
                    "Okamžitý payoff (0–5s): Potvrzuje slib z miniatury a ukazuje finální výsledek.",
                    "Zvýšení sázek (5–15s): Vysvětluje proč na tom záleží a jaké nebezpečí hrozí, pokud divák odejde.",
                    "Struktura otevřených smyček (Open Loops): Připraví cliffhanger, který diváka udrží až do konce."
                ),
                promptTemplate = "Napiš mi přesný slovo-od-slova scénář prvních 30 sekund (The Hook) pro video '$videoTopicInput'. Cíl je udržet 85 % diváků po první minutě. Žádné nudné intro, jen okamžitá akce a otevřená smyčka.",
                sampleOutputTitle = "Struktura úvodního háčku (0:00 - 0:30):",
                sampleOutputBody = """
• 0:00-0:04 (Vizuální šok): "Většina lidí si myslí, že postavit AI asistenta vyžaduje měsíce studia a drahé servery..."
• 0:05-0:12 (Potvrzení slibu): "...ale přesně za 15 minut budete mít v kapse vlastního agenta, který za vás píše kód a odpovídá na e-maily. Zde je důkaz." [Ukázka vteřinového demo na displeji]
• 0:13-0:22 (Zvýšení sázky): "Pokud ale přeskočíte třetí krok s API klíči, váš účet může během noci přijít o stovky dolarů."
• 0:23-0:30 (Otevřená smyčka): "Takže otevřete terminál, jdeme na krok číslo jedna – a ten nejdůležitější bezpečnostní trik si ukážeme přesně uprostřed."
                """.trimIndent()
            ),
            YouTubeGrowthPillar(
                id = "thumbnail_director",
                title = "Vizuální psychologie miniatur (Thumbnails)",
                impactBadge = "3× vyšší zobrazení v Browse",
                icon = Icons.Default.Brush,
                accentColor = CyanBright,
                algorithmSecret = "Miniatura a titulek tvoří pár. Miniatura by NIKDY neměla opakovat text titulku, ale doplňovat ho emocionálním nebo vizuálním kontrastem.",
                whatAgentDoes = listOf(
                    "Pravidlo 3 elementů: Obličej s emocí + klíčový objekt + max. 3 slova textu.",
                    "Kontrola mobilního zobrazení: Miniatura je na mobilu velká jako poštovní známka. Agent testuje kontrast a čitelnost fontu.",
                    "Generování AI promptů (Midjourney / Imagen / Flux) pro tvorbu dramatického pozadí miniatury bez nutnosti fotografického studia."
                ),
                promptTemplate = "Navrhni 3 detailní koncepty YouTube miniatury pro video '$videoTopicInput'. Pro každý koncept popiš: 1) Hlavní vizuál, 2) Text na miniatuře (max 3 slova), 3) Barevný kontrast proti YouTube tmavému režimu, 4) Přesný prompt pro generátor obrázků.",
                sampleOutputTitle = "Koncept virální miniatury:",
                sampleOutputBody = """
🎨 Vizuální kompozice:
• Levá strana: Detail obličeje s výrazem šoku/překvapení, osvětlený cyan/neonovým světlem.
• Pravá strana: Zářící obrazovka mobilu s logem AI agenta a zeleným nápisem: "DEPLOYED IN 14:59".
• Text na miniatuře: "ZÁKAZ KÓDOVÁNÍ?" (Tučný žlutý font bez patků s černým obrysem).
• Barevnost: Hluboká modrá (#0F172A) v kontrastu s jedovatě žlutou (#FACC15) a neon cyan (#38BDF8).
• Midjourney Prompt: "hyper-realistic developer looking in disbelief at glowing futuristic smartphone with AI terminal, neon rim lighting, cinematic 8k, rule of thirds --ar 16:9"
                """.trimIndent()
            ),
            YouTubeGrowthPillar(
                id = "retention_flow",
                title = "Pattern Interrupts & Retence (Watch Time Booster)",
                impactBadge = "+40% celková doba sledování (AVD)",
                icon = Icons.Default.Speed,
                accentColor = EmeraldBright,
                algorithmSecret = "Algoritmus 2026 sleduje 'Satisfaction Score' a retenci. Pokud divák přeskakuje dopředu nebo odchází, video přestane být nabízeno. Mozek potřebuje změnu podnětu každých 45–60 s.",
                whatAgentDoes = listOf(
                    "Navrhuje 'Pattern Interrupts' (změna úhlu kamery, zvukový efekt, B-roll, animovaná grafika, zoom).",
                    "Plánování kapitol (Timestamps) pro Google vyhledávání a snadnou orientaci diváka.",
                    "Strategie End-Screen (závěrečné obrazovky): Zákaz říkat 'To je pro dnešek všechno' – místo toho okamžité přesměrování diváka na další video v playlistu pro vytvoření Binge-Watching efektu."
                ),
                promptTemplate = "Vytvoř scénářový plán retence pro 8minutové video o '$videoTopicInput'. Kam umístit 'Pattern Interrupts', jaké vizuální změny použít každých 50 sekund a jak navrhnout závěrečnou obrazovku, aby divák okamžitě kliknul na další video na kanále.",
                sampleOutputTitle = "Retenční mapa videa (AVD architektura):",
                sampleOutputBody = """
• 0:00-0:30: Agresivní Hook (viz modul výše)
• 1:15: [Pattern Interrupt #1] Rychlý střih na detailní zoom + zvuk 'woosh' + grafická karta s časovačem.
• 2:40: [Příběhová vsuvka]: Krátká ukázka reálné chyby, která rozbije monotónní výklad.
• 4:30: [Polovina videa - Mini Re-Hook]: "Než přejdeme k finálnímu skriptu, musíte vidět tento detail..."
• 6:15: [Příprava na End Screen]: ŽÁDNÉ loučení! "A pokud chcete tohoto bota napojit na vlastní databázi, přesně to jsem vyřešil v tomto videu tady vlevo..."
                """.trimIndent()
            ),
            YouTubeGrowthPillar(
                id = "shorts_repurpose",
                title = "Shorts Repurposing Engine (Příval nových odběratelů)",
                impactBadge = "Až 10× více nových subscriberů",
                icon = Icons.Default.ContentCut,
                accentColor = MagentaBright,
                algorithmSecret = "YouTube Shorts mají jiný algoritmus než dlouhá videa. Slouží jako obří trychtýř (Top of Funnel). Z 1 dlouhého videa lze vyříznout 3–5 virálních Shorts s proklikem na plné video.",
                whatAgentDoes = listOf(
                    "Hledá 'Golden Moments': 30–50sekundové ucelené myšlenky s vysokou informační hustotou.",
                    "Navrhuje nekonečnou smyčku (Seamless Loop) – konec Shortu plynule navazuje na začátek.",
                    "Propojení s dlouhým videem: Využívá funkci 'Related video' pro přelévání shlédnutí do dlouhé stopáže."
                ),
                promptTemplate = "Rozeber téma '$videoTopicInput' a navrhni 3 koncepty YouTube Shorts do 45 sekund. Každý Short musí mít nekonečnou smyčku (Seamless Loop) a výzvu k prokliku na plné video.",
                sampleOutputTitle = "Návrh virálního Shortu s nekonečnou smyčkou:",
                sampleOutputBody = """
📱 Formát 9:16 (Vertikální)
• První věta (start): "Tuhle jednu chybu dělá při stavbě AI bota každý..."
• Obsah (0:04-0:35): 3 bleskové tipy s velkými animovanými titulky uprostřed obrazovky.
• Konec přecházející do smyčky (0:40-0:45): "...a přesně proto si pamatujte, že..." -> [následuje začátek: "Tuhle jednu chybu..."]
• Proklik na plné video: Pinned odkaz na dlouhý tutoriál v záložce 'Related Video'.
                """.trimIndent()
            ),
            YouTubeGrowthPillar(
                id = "seo_metadata",
                title = "YouTube SEO & Smart Metadata",
                impactBadge = "Dlouhodobá pasivní návštěvnost",
                icon = Icons.Default.Search,
                accentColor = VioletPurple,
                algorithmSecret = "Search traffic (vyhledávání) generuje stabilní zhlédnutí i po měsících a letech. Google a YouTube indexují prvních 200 znaků popisku a časové kapitoly.",
                whatAgentDoes = listOf(
                    "SEO optimalizovaný popis: První 3 řádky obsahují přirozená klíčová slova bez klíčové vaty.",
                    "Časové razítka (Timestamps / Kapitoly): Zvyšují šanci, že se video zobrazí přímo ve výsledcích vyhledávání Google.",
                    "Chytrý připnutý komentář (Pinned Comment): Položí otázku vyvolávající debatu (komentáře a interakce = masivní signál pro algoritmus)."
                ),
                promptTemplate = "Vytvoř kompletní SEO metadata balíček pro video '$videoTopicInput': 1) Optimalizovaný popis videa (první 3 řádky pro zobrazení před rozkliknutím), 2) Časové kapitoly (Timestamps), 3) 15 relevantních tagů, 4) Připnutý komentář se strategickou otázkou na diváky.",
                sampleOutputTitle = "Kompletní SEO balíček pro video:",
                sampleOutputBody = """
📝 Popis videa (první 3 řádky):
V tomto videu se krok za krokem podíváme, jak postavit plně funkčního AI asistenta za pouhých 15 minut. Ukážeme si nastavení lokálního prostředí, napojení na Gemini API a vyhnutí se drahým chybám.

⏱️ Kapitoly (Timestamps):
00:00 - Proč stavět vlastního AI asistenta?
01:15 - Příprava projektu a API klíče
04:30 - Kódování jádra asistenta (Živé demo)
08:20 - Zabezpečení tokenů před únikem
12:45 - Testování a spuštění
14:30 - Jak napojit vlastní nástroje a soubory

💬 Pinned Comment:
"K jaké každodenní činnosti by vám vlastní AI asistent ušetřil nejvíce času? Napište mi svůj nápad do komentářů – 3 nejlepší prototypy postavím v příštím videu!"
                """.trimIndent()
            ),
            YouTubeGrowthPillar(
                id = "competitor_spy",
                title = "Analýza konkurence & Outperform",
                impactBadge = "+200% doporučování (Suggested)",
                icon = Icons.Default.Visibility,
                accentColor = EmeraldSuccess,
                algorithmSecret = "Až 70 % zhlédnutí na YouTube pochází z 'Doporučených videí' (Suggested Videos). Pokud víte, jaká videa ve vaší nise právě explodují, můžete natočit lepší variantu a 'svézt se' na jejich vlně.",
                whatAgentDoes = listOf(
                    "Detekce témat s vysokým VPH (Views Per Hour): Odhaluje videa konkurence, která rostou nadprůměrně rychle.",
                    "Content Gap identifikace: Co diváci v komentářích u konkurence postrádali a na co si stěžovali?",
                    "Strategie 'Piggybacking': Jak strukturovat tagy a metadata, aby se vaše video zobrazilo vedle populárního videa jako 'Přehrát další'."
                ),
                promptTemplate = "Analyzuj téma '$videoTopicInput' v nise '$selectedNiche'. Jaká jsou 3 nejúspěšnější videa v této kategorii za poslední rok, jaké slabiny mají v komentářích a jak můžeme natočit 10x hodnotnější video?",
                sampleOutputTitle = "Strategie předstižení konkurence:",
                sampleOutputBody = """
🎯 Analýza slabin konkurence:
1. Konkurenční videa jsou příliš dlouhá (45+ min) a plná zbytečné teorie.
   -> Naše výhoda: Kondenzovaných 15 minut čisté praxe.
2. Diváci v komentářích si stěžují: "Kód z tutoriálu už dnes nefunguje kvůli nové verzi knihovny."
   -> Naše výhoda: Aktuální verze 2026 s připraveným GitHub repozitářem na 1 klik.
3. Chybějící bezpečnostní instrukce:
   -> Naše výhoda: Unikátní kapitola o bezpečném uložení API klíčů bez úniku.
                """.trimIndent()
            ),
            YouTubeGrowthPillar(
                id = "community_flywheel",
                title = "Komunitní setrvačník (Community Flywheel)",
                impactBadge = "Stálá fanouškovská základna",
                icon = Icons.Default.People,
                accentColor = Color(0xFF38BDF8),
                algorithmSecret = "Aktivní komunita generuje okamžitou vlnu shlédnutí v první hodině po vydání (Velocity). To je signál, který odpálí video do širšího algoritmu.",
                whatAgentDoes = listOf(
                    "Příspěvky pro záložku Komunita (Community Tab): Ankety, obrázkové hádanky a zákulisní fotky pro udržení kontaktu mezi videi.",
                    "Strategie pro první hodinu: Jak aktivovat věrné diváky, aby okomentovali a dokoukali video do konce.",
                    "Převedení diváků do e-mail listu nebo Discordu pro nezávislost na algoritmu."
                ),
                promptTemplate = "Navrhni 3 interaktivní příspěvky pro YouTube záložku Komunita (Community tab) před vydáním videa '$videoTopicInput'. Cíl je maximalizovat hlasování v anketách a vytvořit hype na premiéru videa.",
                sampleOutputTitle = "Plán komunitních příspěvků:",
                sampleOutputBody = """
📊 Anketa (2 dny před vydáním):
"Otázka pro vývojáře: Kolik minut denně strávíte rutinními úkoly, které by mohl dělat AI asistent?
[ ] Méně než 15 minut
[ ] 30 až 60 minut
[ ] Více než 2 hodiny
[ ] Už mám vlastního bota!"
(Ankety mají v YouTube algoritmu 5× větší dosah než běžné textové posty a osloví lidi, kteří ještě nejsou odběrateli).

📸 Teaser (24 hodin před premiérou):
Screenshot běžícího terminálu s popiskem: "Nahrávám video na zítra. Za 15 minut tohle poběží každému z vás. Máte zapnutý zvoneček?"
                """.trimIndent()
            )
        )
    }

    val selectedPillar = remember(activePillarTab, pillars) {
        pillars.find { it.id == activePillarTab } ?: pillars.first()
    }

    Scaffold(
        topBar = {
            Surface(
                color = Slate900,
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFFFF0033), Color(0xFFCC0000))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "YouTube Growth Agent",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate100
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Badge(containerColor = Color(0xFFFF0033)) {
                                        Text("AI STRATEGIE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(
                                    text = "Specializovaný agent pro raketový růst sledovanosti kanálu",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TabRow(
                        selectedTabIndex = selectedMainTab,
                        containerColor = Slate900,
                        contentColor = Color(0xFFFF0033)
                    ) {
                        Tab(
                            selected = selectedMainTab == 0,
                            onClick = { selectedMainTab = 0 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(15.dp), tint = if (selectedMainTab == 0) Color(0xFFFF4D4D) else Slate400)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Můj kanál & Obsah", fontSize = 11.sp, fontWeight = if (selectedMainTab == 0) FontWeight.Bold else FontWeight.Normal, color = if (selectedMainTab == 0) Slate100 else Slate400)
                                }
                            }
                        )
                        Tab(
                            selected = selectedMainTab == 1,
                            onClick = { selectedMainTab = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(15.dp), tint = if (selectedMainTab == 1) Color(0xFFFF4D4D) else Slate400)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Růst & Strategie", fontSize = 11.sp, fontWeight = if (selectedMainTab == 1) FontWeight.Bold else FontWeight.Normal, color = if (selectedMainTab == 1) Slate100 else Slate400)
                                }
                            }
                        )
                        Tab(
                            selected = selectedMainTab == 2,
                            onClick = { selectedMainTab = 2 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(15.dp), tint = if (selectedMainTab == 2) Color(0xFFFF4D4D) else Slate400)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Content Kalendář", fontSize = 11.sp, fontWeight = if (selectedMainTab == 2) FontWeight.Bold else FontWeight.Normal, color = if (selectedMainTab == 2) Slate100 else Slate400)
                                }
                            }
                        )
                    }
                }
            }
        },
        containerColor = Slate950
    ) { paddingValues ->
        when (selectedMainTab) {
            0 -> {
                YouTubeChannelContentView(
                    viewModel = viewModel,
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            1 -> {
                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .testTag("youtube_agent_screen"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
            // Hero Banner: Mission & Promise
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Slate900,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFFFF0033).copy(alpha = 0.12f), Color.Transparent)
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoGraph, contentDescription = null, tint = Color(0xFFFF4D4D), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Mise agenta: Zvednout sledovanost a retenci tvého kanálu",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Slate100
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "YouTube algoritmus není náhoda, ale matematický systém optimalizovaný na dvě metriky: Proklik z miniatury (CTR) a Doba sledování (Watch Time / AVD). Tento agent je navržen tak, aby pro každé tvé video zajistil maximální možný algoritmický boost.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate300,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // Status Dashboard: Channel Growth Metrics & Quick Action Buttons
            item {
                YouTubeGrowthDashboardCard(
                    viewModel = viewModel,
                    channelNiche = selectedNiche,
                    currentTopic = videoTopicInput,
                    onTopicChange = { videoTopicInput = it }
                )
            }

            // Channel Profile Customizer
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "1. Nastavení tvého kanálu a tématu videa",
                            fontWeight = FontWeight.Bold,
                            color = CyanBright,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Nisa / Zaměření kanálu:", fontSize = 11.sp, color = Slate400)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            niches.forEach { niche ->
                                FilterChip(
                                    selected = selectedNiche == niche,
                                    onClick = { selectedNiche = niche },
                                    label = { Text(niche, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFFF0033),
                                        selectedLabelColor = Color.White,
                                        containerColor = Slate800,
                                        labelColor = Slate300
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Téma tvého příštího videa (nebo pracovní nápad):", fontSize = 11.sp, color = Slate400)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = videoTopicInput,
                            onValueChange = { videoTopicInput = it },
                            placeholder = { Text("Zadej téma, např. Jak začít s Dockerem...", color = Slate500, fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFF0033),
                                unfocusedBorderColor = Slate700,
                                focusedTextColor = Slate100,
                                unfocusedTextColor = Slate100
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_youtube_video_topic")
                        )
                    }
                }
            }

            // Section Header: 8 Pillars of Growth
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "2. Co všechno YouTube Agent umí (8 klíčových modulů)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Slate100
                        )
                        Text(
                            text = "Vyber modul pro detailní popis, algoritmus a interaktivní ukázku:",
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }
                }
            }

            // Horizontal Pill Carousel for selecting pillar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pillars.forEach { pillar ->
                        val isSelected = activePillarTab == pillar.id
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { activePillarTab = pillar.id }
                                .testTag("pillar_tab_${pillar.id}"),
                            color = if (isSelected) Slate800 else Slate900,
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) pillar.accentColor else Slate800
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = pillar.icon,
                                    contentDescription = null,
                                    tint = pillar.accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = pillar.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Slate100 else Slate300
                                )
                            }
                        }
                    }
                }
            }

            // Active Pillar Detail Card
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Slate900,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, selectedPillar.accentColor.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Title & Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(selectedPillar.accentColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = selectedPillar.icon,
                                        contentDescription = null,
                                        tint = selectedPillar.accentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = selectedPillar.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate100
                                    )
                                    Badge(containerColor = selectedPillar.accentColor.copy(alpha = 0.25f)) {
                                        Text(
                                            text = selectedPillar.impactBadge,
                                            color = selectedPillar.accentColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // How YouTube Algorithm works here
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Slate950,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Slate800),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Psychology, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tajemství YouTube algoritmu:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberWarning)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = selectedPillar.algorithmSecret,
                                    fontSize = 11.sp,
                                    color = Slate300,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // What the agent actually does
                        Text("Konkrétní úkoly, které pro vás agent provede:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate200)
                        Spacer(modifier = Modifier.height(6.dp))
                        selectedPillar.whatAgentDoes.forEach { task ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldBright,
                                    modifier = Modifier
                                        .size(15.dp)
                                        .padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = task,
                                    fontSize = 11.sp,
                                    color = Slate300,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Sample Preview
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Slate950,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedPillar.sampleOutputTitle,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = selectedPillar.accentColor
                                    )
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(selectedPillar.sampleOutputBody))
                                            Toast.makeText(context, "Zkopírováno do schránky", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Kopírovat", tint = Slate400, modifier = Modifier.size(14.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = selectedPillar.sampleOutputBody,
                                    fontSize = 11.sp,
                                    color = Slate200,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Copy Prompt
                            OutlinedButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(selectedPillar.promptTemplate))
                                    Toast.makeText(context, "Prompt pro AI byl zkopírován!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate200),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Kopírovat prompt", fontSize = 11.sp)
                            }

                            // Send directly to Zen Chat
                            Button(
                                onClick = {
                                    viewModel.updateChatInput(selectedPillar.promptTemplate)
                                    viewModel.selectTab(0) // Switch to Chat tab
                                    Toast.makeText(context, "Zadání vloženo do AI Chatu!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0033)),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("btn_send_to_chat_${selectedPillar.id}")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Spustit v AI Chatu", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Summary Checklist: Complete Blueprint for High Viewership
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Checklist, contentDescription = null, tint = CyanBright, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "3. Kontrolní seznam před publikací (Checklist pro virál)",
                                fontWeight = FontWeight.Bold,
                                color = Slate100,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        val checklistItems = listOf(
                            "Ověřeno CTR titulku: Obsahuje zvědavost a má do 50 znaků.",
                            "Miniatura otestována v mobilní velikosti (max 3 slova, silný kontrast).",
                            "Prvních 30 sekund: Žádné logo, žádné intro, okamžité potvrzení miniatury.",
                            "Vložen Pattern Interrupt každých 45–60 sekund (grafika, zoom, b-roll).",
                            "Připraveny 3 vertikální YouTube Shorts s odkazem na plné video.",
                            "Časové kapitoly v popisku a otázka v připnutém komentáři pro diskuzi.",
                            "Závěrečná obrazovka přepíná diváka na další video v sérii (bez loučení)."
                        )

                        checklistItems.forEachIndexed { index, itemText ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Slate800),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${index + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyanBright)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = itemText,
                                    fontSize = 11.sp,
                                    color = Slate300
                                )
                            }
                        }
                    }
                }
            }

            // Roadmap to full YouTube API Integration
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Slate950,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Engineering, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Budoucí přímé napojení (YouTube Data API v3 & MCP)",
                                fontWeight = FontWeight.Bold,
                                color = AmberWarning,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "V další fázi může OpenCode propojit tohoto agenta přímo přes YouTube Data API v3 a specializovaný MCP server. Agent pak bude schopen přímo z IDE automaticky číst analytiku kanálu (CTR v reálném čase, propad retence po minutách), automaticky měnit titulky při nízkém prokliku a odpovídat na komentáře diváků.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
    else -> {
        // Content Calendar View
        YouTubeContentCalendarView(
            viewModel = viewModel,
            channelNiche = selectedNiche,
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        )
    }
}
}
}
