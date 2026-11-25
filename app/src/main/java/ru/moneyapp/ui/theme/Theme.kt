package ru.moneyapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Dining
import androidx.compose.material.icons.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.FreeBreakfast
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Icecream
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalAtm
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.LocalLaundryService
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Money
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.Motorcycle
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.LocalPizza
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Pool
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.SmokeFree
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.EmojiObjects
import androidx.compose.material.icons.rounded.Theaters
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.VideogameAsset
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material.icons.rounded.WorkHistory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography as M3Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.Shapes
import kotlin.math.abs

// ---------------------------------------------------------------------------
// MoneyAPP design system — single source of truth for the visual layer.
// Dark fintech theme by default, light theme from the same tokens.
// ---------------------------------------------------------------------------

// --- Semantic colors (dark) -------------------------------------------------
private val DarkBackground = Color(0xFF0F1115)
private val DarkSurface = Color(0xFF171A21)
private val DarkSurfaceElevated = Color(0xFF1F232C)
private val DarkOutline = Color(0xFF2A2F3A)

private val DarkPrimary = Color(0xFF34D399)      // emerald accent
private val DarkOnPrimary = Color(0xFF052E21)
private val DarkPrimaryContainer = Color(0xFF0B3B2E)
private val DarkOnPrimaryContainer = Color(0xFFA7F3D0)

private val DarkOnSurface = Color(0xFFE8EAED)
private val DarkOnSurfaceVariant = Color(0xFF9AA3B2)
private val DarkError = Color(0xFFFF7A6B)

private val DarkIncome = Color(0xFF4ADE80)
private val DarkExpense = Color(0xFFFF6F61)   // coral
private val DarkSavings = Color(0xFFC084FC)
private val DarkWarning = Color(0xFFFBBF24)
private val DarkIncomeText = Color(0xFF4ADE80)
private val DarkExpenseText = Color(0xFFFF6F61)
private val DarkSavingsText = Color(0xFFC084FC)
private val DarkWarningText = Color(0xFFFBBF24)

// --- Semantic colors (light) ------------------------------------------------
private val LightBackground = Color(0xFFF5F7FA)
private val LightSurface = Color(0xFFFFFFFF)
private val LightSurfaceElevated = Color(0xFFF0F3F7)
private val LightOutline = Color(0xFFD7DEE8)

private val LightPrimary = Color(0xFF059669)
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightPrimaryContainer = Color(0xFFD1FAE5)
private val LightOnPrimaryContainer = Color(0xFF064E3B)

private val LightOnSurface = Color(0xFF14181F)
private val LightOnSurfaceVariant = Color(0xFF5C6675)
private val LightError = Color(0xFFDC4C3F)

private val LightIncome = Color(0xFF16A34A)
private val LightExpense = Color(0xFFF97066)
private val LightSavings = Color(0xFF9333EA)
private val LightWarning = Color(0xFFB45309)
private val LightIncomeText = Color(0xFF15803D)
private val LightExpenseText = Color(0xFFC63B2E)
private val LightSavingsText = Color(0xFF6D28D9)
private val LightWarningText = Color(0xFFB45309)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSavings,
    onSecondary = Color(0xFF2E1065),
    secondaryContainer = Color(0xFF2E1A47),
    onSecondaryContainer = Color(0xFFE9D5FF),
    tertiary = DarkIncome,
    onTertiary = Color(0xFF052E16),
    tertiaryContainer = Color(0xFF052E21),
    onTertiaryContainer = Color(0xFFBBF7D0),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLow = Color(0xFF14171D),
    surfaceContainer = DarkSurfaceElevated,
    surfaceContainerHigh = Color(0xFF232833),
    outline = DarkOutline,
    outlineVariant = Color(0xFF22262F),
    error = DarkError,
    onError = Color(0xFF3D0A06),
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSavings,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E8FF),
    onSecondaryContainer = Color(0xFF3B0764),
    tertiary = LightIncome,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDCFCE7),
    onTertiaryContainer = Color(0xFF14532D),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = LightSurfaceElevated,
    surfaceContainerHigh = Color(0xFFE9EEF4),
    outline = LightOutline,
    outlineVariant = Color(0xFFE3E9F0),
    error = LightError,
    onError = Color.White,
)

// --- Typography ------------------------------------------------------------
// Money figures always use tabular figures so columns of sums line up.
private const val TNUM = "tnum"

private val BaseBody = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.1.sp
)

object MoneyTypography {
    val displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    )

    // Hero balance on the dashboard: large, tabular
    val balanceHero = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 42.sp,
        fontFeatureSettings = TNUM
    )

    val headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    )

    // Large amount input in the transaction form
    val amountInput = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        fontFeatureSettings = TNUM
    )

    // Section / card titles
    val titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 26.sp
    )

    val titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp
    )

    // KPI and row amounts: tabular
    val amountMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontFeatureSettings = TNUM
    )

    val amountSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontFeatureSettings = TNUM
    )

    val bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 15.sp,
        lineHeight = 22.sp
    )

    val bodyMedium = BaseBody

    val bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )

    val labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )

    val labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )

    val labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.2.sp
    )
}

// M3 scheme consumed by MaterialTheme: built from the same tokens.
private val m3Typography = M3Typography(
    displaySmall = MoneyTypography.displaySmall,
    headlineSmall = MoneyTypography.headlineSmall,
    titleLarge = MoneyTypography.titleLarge,
    titleMedium = MoneyTypography.titleMedium,
    bodyLarge = MoneyTypography.bodyLarge,
    bodyMedium = MoneyTypography.bodyMedium,
    bodySmall = MoneyTypography.bodySmall,
    labelLarge = MoneyTypography.labelLarge,
    labelMedium = MoneyTypography.labelMedium,
    labelSmall = MoneyTypography.labelSmall
)

// --- Shapes (8dp grid, cards 16–20dp) --------------------------------------
val MoneyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

internal val CardShape: Shape = RoundedCornerShape(18.dp)
internal val FieldShape: Shape = RoundedCornerShape(12.dp)
internal val ChipShape: Shape = CircleShape

// --- Motion (nothing longer than 300ms) -------------------------------------
object MotionTokens {
    val fast: AnimationSpec<Float> = tween(durationMillis = 150, easing = FastOutSlowInEasing)
    val medium: AnimationSpec<Float> = tween(durationMillis = 220, easing = FastOutSlowInEasing)
    val slow: AnimationSpec<Float> = tween(durationMillis = 300, easing = FastOutSlowInEasing)
}

// --- Theme state -------------------------------------------------------------
// Set by MoneyAppTheme, reflects the active theme.
internal val LocalDarkTheme = compositionLocalOf { false }

@Composable
fun incomeColor(): Color = if (LocalDarkTheme.current) DarkIncome else LightIncome

@Composable
fun expenseColor(): Color = if (LocalDarkTheme.current) DarkExpense else LightExpense

@Composable
fun savingsColor(): Color = if (LocalDarkTheme.current) DarkSavings else LightSavings

@Composable
fun warningColor(): Color = if (LocalDarkTheme.current) DarkWarning else LightWarning

@Composable
fun incomeTextColor(): Color = if (LocalDarkTheme.current) DarkIncomeText else LightIncomeText

@Composable
fun expenseTextColor(): Color = if (LocalDarkTheme.current) DarkExpenseText else LightExpenseText

@Composable
fun savingsTextColor(): Color = if (LocalDarkTheme.current) DarkSavingsText else LightSavingsText

@Composable
fun warningTextColor(): Color = if (LocalDarkTheme.current) DarkWarningText else LightWarningText

// Soft gradient for the hero balance card (instead of a flat fill).
@Composable
fun balanceCardGradient(): Brush {
    val (a, b, c) = if (LocalDarkTheme.current) {
        Triple(Color(0xFF12403A), Color(0xFF0F2B2E), Color(0xFF151A22))
    } else {
        Triple(Color(0xFF6EE7B7), Color(0xFFB5F3DC), Color(0xFFE9FBF2))
    }
    return Brush.linearGradient(listOf(a, b, c), start = Offset.Zero, end = Offset(600f, 800f))
}

// Gradient text color helper for the hero card (dark: light mint; light: deep green)
@Composable
fun balanceCardTextColor(): Color =
    if (LocalDarkTheme.current) Color(0xFFD9FBEC) else Color(0xFF064E3B)

@Composable
fun balanceCardSubTextColor(): Color =
    if (LocalDarkTheme.current) Color(0x99A7F3D0) else Color(0x99064E3B)

// --- Legacy aliases (old screens imported these directly) --------------------
@Deprecated("Use incomeColor()", ReplaceWith("incomeColor()"))
val PrimaryColor: Color = Color(0xFF0D47A1)
@Deprecated("Use incomeColor()", ReplaceWith("incomeColor()"))
val IncomeColor: Color = Color(0xFF4CAF50)
@Deprecated("Use expenseColor()", ReplaceWith("expenseColor()"))
val ExpenseColor: Color = Color(0xFFEF5350)
@Deprecated("Use savingsColor()", ReplaceWith("savingsColor()"))
val SavingsColor: Color = Color(0xFF6A1B9A)
@Deprecated("Use warningColor()", ReplaceWith("warningColor()"))
val NormColor: Color = Color(0xFF8D6E63)
@Deprecated("Use MaterialTheme.colorScheme.background", ReplaceWith("MaterialTheme.colorScheme.background"))
val NeutralBg: Color = Color(0xFFF5F5F5)

// --- Category icons ----------------------------------------------------------
// One vector style everywhere (Material Symbols Rounded, same stroke).
// The emoji field stays in the data layer; this map drives the UI.

private val CategoryIcons = mapOf(
    "Зарплата" to Icons.Rounded.Wallet,
    "Переработки" to Icons.Rounded.WorkHistory,
    "Премия" to Icons.Rounded.CardGiftcard,
    "Прочий доход" to Icons.Rounded.AttachMoney,
    "Инвестиции" to Icons.Rounded.TrendingUp,
    "Продукты" to Icons.Rounded.ShoppingCart,
    "Ипотека/Аренда" to Icons.Rounded.Home,
    "ЖКХ" to Icons.Rounded.ElectricBolt,
    "Транспорт" to Icons.Rounded.DirectionsCar,
    "Рестораны" to Icons.Rounded.Restaurant,
    "Здоровье" to Icons.Rounded.LocalHospital,
    "Одежда" to Icons.Rounded.Checkroom,
    "Подписки" to Icons.Rounded.Subscriptions,
    "Спорт" to Icons.Rounded.FitnessCenter,
    "Образование" to Icons.Rounded.School,
    "Подарки" to Icons.Rounded.CardGiftcard,
    "Развлечения" to Icons.Rounded.Theaters,
    "Связь" to Icons.Rounded.Smartphone,
    "Услуги" to Icons.Rounded.Build,
    "Маркетплейсы" to Icons.Rounded.ShoppingBag,
    "Вредные привычки" to Icons.Rounded.SmokeFree
)

// v1.9.0: user-selectable icon palette for categories. The picker shows
// these tiles; Category.icon stores the key (null = automatic by name).
data class CategoryIconOption(val key: String, val icon: ImageVector)

val CategoryIconPalette: List<CategoryIconOption> = listOf(
    CategoryIconOption("wallet", Icons.Rounded.Wallet),
    CategoryIconOption("money", Icons.Rounded.AttachMoney),
    CategoryIconOption("coins", Icons.Rounded.Paid),
    CategoryIconOption("card", Icons.Rounded.CreditCard),
    CategoryIconOption("cash", Icons.Rounded.Payments),
    CategoryIconOption("bill", Icons.Rounded.Money),
    CategoryIconOption("savings", Icons.Rounded.Savings),
    CategoryIconOption("percent", Icons.Rounded.Percent),
    CategoryIconOption("trend-up", Icons.Rounded.TrendingUp),
    CategoryIconOption("atms", Icons.Rounded.LocalAtm),
    CategoryIconOption("account", Icons.Rounded.AccountBalance),
    CategoryIconOption("briefcase", Icons.Rounded.Work),
    CategoryIconOption("gift", Icons.Rounded.CardGiftcard),
    CategoryIconOption("cart", Icons.Rounded.ShoppingCart),
    CategoryIconOption("bag", Icons.Rounded.ShoppingBag),
    CategoryIconOption("store", Icons.Rounded.Storefront),
    CategoryIconOption("home", Icons.Rounded.Home),
    CategoryIconOption("apartment", Icons.Rounded.Apartment),
    CategoryIconOption("bolt", Icons.Rounded.ElectricBolt),
    CategoryIconOption("bulb", Icons.Rounded.Lightbulb),
    CategoryIconOption("car", Icons.Rounded.DirectionsCar),
    CategoryIconOption("bus", Icons.Rounded.DirectionsBus),
    CategoryIconOption("bike", Icons.Rounded.DirectionsBike),
    CategoryIconOption("moto", Icons.Rounded.Motorcycle),
    CategoryIconOption("plane", Icons.Rounded.Flight),
    CategoryIconOption("gas", Icons.Rounded.LocalGasStation),
    CategoryIconOption("restaurant", Icons.Rounded.Restaurant),
    CategoryIconOption("food", Icons.Rounded.Dining),
    CategoryIconOption("coffee", Icons.Rounded.LocalCafe),
    CategoryIconOption("breakfast", Icons.Rounded.FreeBreakfast),
    CategoryIconOption("pizza", Icons.Rounded.LocalPizza),
    CategoryIconOption("icecream", Icons.Rounded.Icecream),
    CategoryIconOption("bar", Icons.Rounded.LocalBar),
    CategoryIconOption("hospital", Icons.Rounded.LocalHospital),
    CategoryIconOption("pharmacy", Icons.Rounded.LocalPharmacy),
    CategoryIconOption("heart", Icons.Rounded.Favorite),
    CategoryIconOption("medicine", Icons.Rounded.Medication),
    CategoryIconOption("shirt", Icons.Rounded.Checkroom),
    CategoryIconOption("style", Icons.Rounded.Style),
    CategoryIconOption("subscriptions", Icons.Rounded.Subscriptions),
    CategoryIconOption("phone", Icons.Rounded.Smartphone),
    CategoryIconOption("devices", Icons.Rounded.Devices),
    CategoryIconOption("wifi", Icons.Rounded.Wifi),
    CategoryIconOption("fitness", Icons.Rounded.FitnessCenter),
    CategoryIconOption("esports", Icons.Rounded.SportsEsports),
    CategoryIconOption("pool", Icons.Rounded.Pool),
    CategoryIconOption("school", Icons.Rounded.School),
    CategoryIconOption("book", Icons.Rounded.MenuBook),
    CategoryIconOption("toy", Icons.Rounded.EmojiObjects),
    CategoryIconOption("theater", Icons.Rounded.Theaters),
    CategoryIconOption("games", Icons.Rounded.VideogameAsset),
    CategoryIconOption("tools", Icons.Rounded.Build),
    CategoryIconOption("cleaning", Icons.Rounded.CleaningServices),
    CategoryIconOption("laundry", Icons.Rounded.LocalLaundryService),
    CategoryIconOption("smokefree", Icons.Rounded.SmokeFree),
    CategoryIconOption("pets", Icons.Rounded.Pets),
    CategoryIconOption("category", Icons.Rounded.Category),
    CategoryIconOption("label", Icons.Rounded.Label),
    CategoryIconOption("star", Icons.Rounded.Star),
    CategoryIconOption("mood", Icons.Rounded.Mood)
)

private val categoryIconByKey: Map<String, ImageVector> =
    CategoryIconPalette.associate { it.key to it.icon }

/**
 * Rounded vector icon for a category.
 * v1.9.0 resolution order: explicit icon key (the user's picker choice) →
 * name map (seed categories) → deterministic hash fallback for the rest.
 */
fun categoryIcon(name: String, iconKey: String? = null): ImageVector {
    iconKey?.trim()?.takeIf { it.isNotEmpty() }?.let { key ->
        categoryIconByKey[key]?.let { return it }
    }
    CategoryIcons[name]?.let { return it }
    val fallbacks = listOf(
        Icons.Rounded.Category, Icons.Rounded.Label, Icons.Rounded.AttachMoney,
        Icons.Rounded.ShoppingCart, Icons.Rounded.Payments, Icons.Rounded.Dining
    )
    return fallbacks[(name.hashCode() and 0x7fffffff) % fallbacks.size]
}

// Deterministic per-category accent for icon tinting (soft fintech palette).
private val CategoryPalette = listOf(
    Color(0xFF34D399), Color(0xFF38BDF8), Color(0xFFC084FC), Color(0xFFFBBF24),
    Color(0xFFFF8A65), Color(0xFF4ADE80), Color(0xFF7DD3FC), Color(0xFFF472B6),
    Color(0xFFA3E635), Color(0xFFFB923C)
)

fun categoryTint(name: String): Color =
    CategoryPalette[(abs(name.hashCode()) % 1000) % CategoryPalette.size]

// Type icons (Доход / Расход / Отложить)
fun typeIcon(type: ru.moneyapp.domain.model.TransactionType): ImageVector = when (type) {
    ru.moneyapp.domain.model.TransactionType.INCOME -> Icons.Rounded.TrendingUp
    ru.moneyapp.domain.model.TransactionType.EXPENSE -> Icons.Rounded.TrendingDown
    ru.moneyapp.domain.model.TransactionType.SAVINGS -> Icons.Rounded.Savings
    ru.moneyapp.domain.model.TransactionType.SAVINGS_SPEND -> Icons.Rounded.Paid
}

// --- Theme entry point -------------------------------------------------------
@Composable
fun MoneyAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // No window.statusBarColor / isAppearanceLight* here: the activity runs
    // edge-to-edge (enableEdgeToEdge) and the TopAppBar extends its surface
    // behind the status bar. Painting the status bar a fixed color on top
    // produced a two-tone header (blue bar over the surface app bar).

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = m3Typography,
            shapes = MoneyShapes,
            content = content
        )
    }
}
