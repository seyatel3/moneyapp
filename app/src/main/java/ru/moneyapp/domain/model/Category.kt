package ru.moneyapp.domain.model

data class Category(
    val name: String,
    val type: TransactionType,
    val emoji: String,
    val isVisible: Boolean = true,
    val sortOrder: Int = 0,
    // v1.9.0: icon key from CategoryIconPalette (user's choice in the picker);
    // null/blank = automatic icon by category name.
    // Last on purpose: the seed below uses positional arguments.
    val icon: String? = null
)

/**
 * All categories seeded from the xlsx file.
 * Single source of truth.
 */
object DefaultCategories {

    fun getAll(): List<Category> = income + savings + expenses

    val income: List<Category> = listOf(
        Category("Зарплата", TransactionType.INCOME, "💼", true, 1),
        Category("Переработки", TransactionType.INCOME, "💼", true, 2),
        Category("Премия", TransactionType.INCOME, "💼", true, 3),
        Category("Прочий доход", TransactionType.INCOME, "💼", true, 4),
        Category("Прочее", TransactionType.INCOME, "💼", true, 5),
    )

    val savings: List<Category> = listOf(
        Category("Инвестиции", TransactionType.SAVINGS, "📈", true, 1),
    )

    val expenses: List<Category> = listOf(
        Category("Продукты", TransactionType.EXPENSE, "🛒", true, 1),
        Category("Ипотека/Аренда", TransactionType.EXPENSE, "🏠", true, 2),
        Category("ЖКХ", TransactionType.EXPENSE, "🏠", true, 3),
        Category("Транспорт", TransactionType.EXPENSE, "🚌", true, 4),
        Category("Рестораны", TransactionType.EXPENSE, "🍕", true, 5),
        Category("Здоровье", TransactionType.EXPENSE, "❤️", true, 6),
        Category("Одежда", TransactionType.EXPENSE, "👗", true, 7),
        Category("Подписки", TransactionType.EXPENSE, "📱", true, 8),
        Category("Спорт", TransactionType.EXPENSE, "💪", true, 9),
        Category("Образование", TransactionType.EXPENSE, "📚", true, 10),
        Category("Подарки", TransactionType.EXPENSE, "🎁", true, 11),
        Category("Развлечения", TransactionType.EXPENSE, "🎉", true, 12),
        Category("Связь", TransactionType.EXPENSE, "📱", true, 13),
        Category("Услуги", TransactionType.EXPENSE, "🔧", true, 14),
        Category("Маркетплейсы", TransactionType.EXPENSE, "🛍️", true, 15),
        Category("Вредные привычки", TransactionType.EXPENSE, "🚬", true, 16),
        Category("Прочее", TransactionType.EXPENSE, "📦", true, 17),
    )

    fun getByType(type: TransactionType): List<Category> = when (type) {
        TransactionType.INCOME -> income
        TransactionType.EXPENSE -> expenses
        TransactionType.SAVINGS -> savings
        // v1.11.6: spending from savings uses expense categories
        TransactionType.SAVINGS_SPEND -> expenses
    }

    fun getByTypeAndVisibility(type: TransactionType): List<Category> =
        getByType(type).filter { it.isVisible }

    fun findByName(name: String): Category? = getAll().find { it.name == name }

    // v1.10.0: type-aware seed lookup (a name may exist in several types).
    fun findByTypeAndName(type: TransactionType, name: String): Category? =
        getByType(type).find { it.name == name }
}
