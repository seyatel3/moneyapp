package ru.moneyapp.ui.l10n

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import ru.moneyapp.domain.model.BudgetStatus
import ru.moneyapp.domain.model.BudgetType
import ru.moneyapp.domain.model.GoalStatus
import ru.moneyapp.domain.model.PaymentMethod
import ru.moneyapp.domain.model.TransactionType
import java.util.Locale

/**
 * v1.9.0: in-app language (RU / EN).
 *
 * A Kotlin bundle instead of Android resources on purpose:
 *  - the switch is app state (DataStore), not the system locale — it must
 *    work on phones whose system language is Russian;
 *  - snapshot state makes every composable re-compose on the switch without
 *    an activity recreation;
 *  - non-Compose code (ViewModels, notifications, workers) reads the same
 *    bundle without a Context.
 *
 * `L10n.s` is the bundle for the active language; RU is the default.
 */
enum class AppLang { RU, EN }

class Strings {
    // --- common ---
    var ok: String = "OK"
    var cancel: String = "Отмена"
    var save: String = "Сохранить"
    var create: String = "Создать"
    var delete: String = "Удалить"
    var edit: String = "Изменить"
    var add: String = "Добавить"
    var close: String = "Закрыть"
    var chooseFile: String = "Выбрать файл"
    var importBtn: String = "Импортировать"
    var noData: String = "Нет данных"
    // --- transaction types / payment ---
    var txTypeIncome: String = "Доход"
    var txTypeExpense: String = "Расход"
    // v1.10.0: «Отложить» не помещался в 3-х кнопочные переключатели —
    // короче синоним, глобально (UI только; xlsx-форматы и импорт не тронуты).
    var txTypeSavings: String = "Копилка"
    var txTypeSavingsSpend: String = "Из накоплений"
    var txTypeIncomePlural: String = "Доходы"
    var txTypeExpensePlural: String = "Расходы"
    var txTypeSavingsPlural: String = "Сбережения"
    var pmCash: String = "Наличные"
    var pmCard: String = "Безнал"
    var budgetTypeIncome: String = "Доход"
    var budgetTypeExpense: String = "Расход"
    var budgetTypeSavings: String = "Копилка"
    var budgetTypeSummary: String = "Итог"
    var budgetStatusOk: String = "OK"
    var budgetStatusClose: String = "Почти"
    var budgetStatusOver: String = "Перебор"
    var budgetStatusUnder: String = "Недобор"
    var goalStatusInProgress: String = "В процессе"
    var goalStatusCompleted: String = "Выполнено"
    var goalStatusOverdue: String = "Просрочено"
    // --- months (index 1..12; 0 unused) ---
    var monthsFull: List<String> = listOf(
        "", "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    )
    var monthsShort: List<String> = listOf(
        "", "Янв", "Фев", "Мар", "Апр", "Май", "Июн",
        "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек"
    )
    // v1.11.0: weekday labels for the analytics cards (Mon=1 .. Sun=7)
    var weekdayShort: List<String> = listOf(
        "", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"
    )
    // --- nav ---
    var navDashboard: String = "Дашборд"
    var navTransactions: String = "Операции"
    var navBudget: String = "Бюджет"
    var navGoals: String = "Цели"
    var navAnalytics: String = "Анализ"
    var navSettings: String = "Настройки"
    var navCategories: String = "Категории"
    // --- day headers ---
    var today: String = "Сегодня"
    var yesterday: String = "Вчера"
    var dayBeforeYesterday: String = "Позавчера"
    // --- dashboard ---
    var dashBalance: String = "Остаток"
    var dashIncome: String = "Доходы"
    var dashExpenses: String = "Расходы"
    var dashSavings: String = "Отложено"
    var dashSavingsRate: String = "Норма"
    var dashSummaryFor: (String) -> String = { "Сводка за $it" }
    var dashExpensesByCategory: String = "Расходы по категориям"
    var dashNoExpensesTitle: String = "Нет расходов за этот месяц"
    var dashNoExpensesHint: String = "Добавьте операции — здесь появится структура затрат"
    var dashMonthlyLimit: String = "Лимит на месяц"
    var dashLeft: (String, String) -> String = { left, pct -> "Осталось $left ₽ • $pct%" }
    var dashOver: (String, String) -> String = { over, pct -> "Превышение $over ₽ • $pct%" }
    var prevMonth: String = "Предыдущий месяц"
    var nextMonth: String = "Следующий месяц"
    var prevYear: String = "Предыдущий год"
    var nextYear: String = "Следующий год"
    // v1.10.0: месяц/год toggle on the dashboard
    var dashPeriodMonth: String = "Месяц"
    var dashPeriodYear: String = "Год"
    // --- transactions ---
    var txSearchPlaceholder: String = "Поиск по описанию"
    // v1.10.0: search is a compact button that expands into a field
    var txSearch: String = "Поиск"
    var txDateFilter: String = "Даты"
    var txDateFrom: String = "От"
    var txDateTo: String = "До"
    var txDateClear: String = "Сбросить"
    var txFilterAll: String = "Все"
    var txEmptyTitle: String = "Нет транзакций"
    var txEmptyHint: String = "Нажмите «Добавить», чтобы записать первую операцию"
    var txDeleteTitle: String = "Удалить транзакцию?"
    var txNewTitle: String = "Новая транзакция"
    var txEditTitle: String = "Изменить транзакцию"
    var txAmount: String = "Сумма (₽)"
    var txCategory: String = "Категория"
    var txNoCategoriesHint: String = "Категорий этого типа пока нет — добавьте первую кнопкой «+»"
    var txDescription: String = "Описание"
    var txNote: String = "Примечание"
    var txSaveChanges: String = "Сохранить изменения"
    var txClear: String = "Очистить"
    // --- categories ---
    var catManageTitle: String = "Управление категориями"
    // v1.10.0: tap is now edit, delete moved to the trash icon
    var catDragHint: String = "Перетаскивайте за ручку слева внутри секции, чтобы изменить порядок. Тап по строке — редактирование, корзина — удаление."
    var catEmptyHint: String = "Нет категорий — добавьте через «Добавить»"
    var catDeleteTitle: String = "Удалить категорию?"
    var catDeleteText: (String) -> String = { "«$it» будет удалена. Существующие транзакции сохранятся." }
    var catSectionIncome: String = "Доходы"
    var catSectionExpense: String = "Расходы"
    var catSectionSavings: String = "Копилка"
    var catNewTitle: String = "Новая категория"
    var catEditTitle: String = "Изменить категорию"
    var catRenameHint: String = "Новое имя и тип будут обновлены в связанных операциях и планах бюджета"
    var catName: String = "Название"
    var catIcon: String = "Иконка"
    var catIconAuto: String = "Автомат"
    var catMove: String = "Переместить"
    var catWillBeAddedTo: (String) -> String = { "Будет добавлена к типу «$it»" }
    var catDuplicate: String = "Категория с таким названием уже есть"
    var catSortHint: String = "Сортировка, добавление и скрытие категорий"
    // --- settings: account / sync ---
    var setAccount: String = "Аккаунт"
    var setUsername: String = "Имя пользователя"
    var setCurrency: String = "Валюта"
    var setSync: String = "Синхронизация"
    var setGoogleSheets: String = "Google Таблицы"
    var setGoogleSheetsSub: String = "разовая настройка, ~5 минут"
    var setExpand: String = "Развернуть"
    var setClientId: String = "Client ID (шаг 5)"
    var setSpreadsheetLink: String = "Ссылка на свою таблицу (необязательно)"
    var setSpreadsheetLinkHint: String = "Пусто = таблица создастся сама. Своя таблица: вставьте ссылку docs.google.com/spreadsheets/d/… — данные будут в ней (на вкладке «Транзакции», остальные вкладки не трогаются). После подключения этот блок скроется — останется только «Синхронизировать»."
    var setConnectGoogle: String = "Подключить Google аккаунт"
    var setConnected: String = "Подключено"
    var setSpreadsheet: (String) -> String = { "Таблица: $it" }
    var setSyncNow: String = "Синхронизировать"
    var setDisconnect: String = "Отключить"
    var guideStep1: String = "Создайте проект (любое имя, например «MoneyAPP»)."
    var guideStep2: String = "«APIs & Services» → «Library»: включите Google Sheets API и Google Drive API."
    var guideStep3: String = "«OAuth consent screen»: выберите External и добавьте свой email в «Test users»."
    var guideStep4: String = "«Credentials» → «Create credentials» → «OAuth client ID» → тип «Desktop app». В поле «Authorized redirect URIs» добавьте moneyapp://oauth2-callback."
    var guideStep5: String = "Скопируйте Client ID и вставьте его в поле ниже."
    // --- settings: notifications / appearance ---
    var setNotifications: String = "Уведомления"
    var setDailyReminder: String = "Ежедневное напоминание"
    var setReminderTime: String = "Время напоминания"
    var setAppearance: String = "Оформление"
    var themeSystem: String = "Системная"
    var themeLight: String = "Светлая"
    var themeDark: String = "Тёмная"
    var setLanguage: String = "Язык"
    var langRu: String = "Русский"
    var langEn: String = "English"
    // --- settings: budget ---
    var setMonthlyBudget: String = "Бюджет на месяц"
    var setCapLimit: String = "Лимит (₽/мес)"
    var setCapLimitHint: String = "Пусто — общий лимит выключен"
    var setCapLinkIncome: String = "Связать с доходом"
    var setCapIncomePct: String = "Доля дохода, %"
    var setTemplateGroup: String = "Шаблон по категориям"
    var setCopyPrevMonth: String = "Копировать план прошлого месяца"
    // --- settings: data / categories / about ---
    var setData: String = "Данные"
    var setExportExcel: String = "Экспорт Excel"
    var setImportExcel: String = "Импорт Excel"
    var setBackup: String = "Резервная копия (JSON)"
    var setRestore: String = "Восстановить из резервной копии"
    var setCategories: String = "Категории"
    var setAbout: String = "О приложении"
    var setVersion: (String) -> String = { "MoneyAPP v$it" }
    var setTagline: String = "Персональный финансовый трекер"
    var setGmsFree: String = "GMS-free • Работает на Huawei Mate X3"
    var setLastCrash: String = "Журнал последнего сбоя"
    var setShareCrash: String = "Поделиться журналом"
    var setHide: String = "Скрыть"
    // --- settings: dialogs ---
    var setImportExcelTitle: String = "Импорт Excel"
    var setImportFound: (Int) -> String = { "Найдено транзакций: $it" }
    var setImportPeriod: (String) -> String = { "Период: $it" }
    var setImportCounts: (Int, Int, Int, Int) -> String = { a, b, c, d -> "Доходы: $a • Расходы: $b • Отложено: $c • Из накоплений: $d" }
    var setImportTotal: (String) -> String = { "Итого: $it ₽" }
    var setImportDupes: String = "Дубликаты (дата + тип + категория + описание + сумма) будут пропущены"
    var setRestoreTitle: String = "Восстановление из резервной копии"
    var setRestoreText: String = "Копия объединится с текущими данными: новые записи будут добавлены, существующие — обновлены, если в копии они новее. Ничего не удаляется."
    // --- settings: messages ---
    var setErrNoClientId: String = "Введите OAuth Client ID: Google Cloud Console → Credentials → Create OAuth Client ID (тип: настольное приложение)"
    var setErrAuthError: String = "Ошибка авторизации"
    var setErrAuthCancelled: String = "Авторизация отменена"
    var setErrNoPkce: String = "Нет PKCE-верификатора"
    var setErrBadLink: String = "Не удалось распознать ссылку на таблицу — вставьте полный адрес из адресной строки браузера"
    var setErrNoSheetAccess: String = "Нет доступа к таблице по ссылке. Проверьте, что адрес скопирован полностью и таблица принадлежит подключённому Google-аккаунту"
    var setMsgConnected: String = "Google подключён. Авто-синхронизация каждые 15 минут."
    var setMsgConnectError: (String) -> String = { "Не удалось подключиться: $it" }
    var setMsgSyncStarted: String = "Синхронизация запущена"
    var setMsgDisconnected: String = "Google отключён"
    var setMsgReminderTime: (String) -> String = { "Время напоминания: $it" }
    var setErrOpenFile: String = "Не удалось открыть файл"
    var setMsgExcelExported: (Int) -> String = { "Excel выгружен: $it транзакций" }
    var setMsgExportError: (String) -> String = { "Ошибка экспорта: $it" }
    var setMsgNoTxInFile: String = "В файле не найдено транзакций"
    var setMsgReadFileError: (String) -> String = { "Не удалось прочитать файл: $it" }
    var setMsgImportNothing: (Int) -> String = { "Новых транзакций нет (дубликатов: $it)" }
    var setMsgImportDone: (Int, Int) -> String = { added, skipped -> "Импортировано: $added, пропущено дубликатов: $skipped" }
    var setMsgImportError: (String) -> String = { "Ошибка импорта: $it" }
    var setMsgBackupCreated: (Int, Int) -> String = { tx, goals -> "Резервная копия создана: $tx транзакций, $goals целей" }
    var setMsgBackupError: (String) -> String = { "Ошибка создания копии: $it" }
    var setMsgRestored: (Int, Int, Int, Int) -> String = { added, updated, goals, cats -> "Восстановлено: транзакций +$added/=$updated, целей +$goals, категорий +$cats" }
    var setMsgRestoreError: (String) -> String = { "Ошибка восстановления: $it" }
    var crashShareSubject: String = "MoneyApp — журнал сбоя"
    var crashChooserTitle: String = "Отправить журнал"
    // --- budget screen ---
    var budNotConfigured: String = "Бюджет не настроен"
    var budNotConfiguredHint: String = "Настройте план бюджета для этого месяца"
    var budOnTrack: String = "В норме"
    var budCloseToLimit: String = "Близко к лимиту"
    var budLimitExceeded: String = "Лимит превышен"
    var budLeft: (String) -> String = { "Осталось $it ₽" }
    var budOver: (String) -> String = { "Превышение $it ₽" }
    var budAwaitingIncome: String = "Ждём доходы"
    var budAwaitingIncomeText: (Int) -> String = { "Лимит = $it% дохода за месяц. Он появится автоматически, как только за этот месяц будут записаны доходы." }
    // v1.10.0: no income for the month → the cap uses the median of the year's
    // monthly incomes instead of waiting forever
    var budMedianIncome: String = "Нет дохода за месяц"
    var budMedianIncomeText: (Int, String) -> String = { pct, median -> "Доходы за этот месяц ещё не записаны. Лимит = $pct% от медианного дохода за год ($median ₽)." }
    var budSpentThisMonth: (String) -> String = { "Расходовано за месяц: $it ₽" }
    var budDifference: (String) -> String = { "Разница: $it ₽" }
    // v1.11.5: budget plan dialog (parity with the mini-app)
    var budAddPlan: String = "Добавить план"
    var budNewPlan: String = "Новый план"
    var budEditPlan: String = "Изменить план"
    var budPlanAmount: String = "План (₽)"
    var budCategory: String = "Категория"
    var budPlanSaved: String = "План сохранён"
    var budPlanDeleted: String = "План удалён"
    var budPlanFromTemplate: String = "План из шаблона по категориям. Сохранение задаст план только на этот месяц; сам шаблон (в Настройках) не изменится."
    var budPlanFromPrev: String = "План скопирован из прошлого месяца. Сохранение задаст план только на этот месяц."
    // --- analytics ---
    var anaIncomeExpenses: String = "Доходы / Расходы"
    var anaNoDataYear: String = "Нет данных за этот год"
    var anaCumulative: String = "Накопления (нарастающий итог)"
    var anaOther: String = "Прочее"
    var anaTotal: String = "Итого"
    var anaTop5: String = "Топ-5 расходов"
    var anaMonthly: String = "Помесячная аналитика"
    var anaNoDataCompare: String = "Нет данных для сравнения"
    var anaMonth: String = "Месяц"
    var anaIncome: String = "Доход"
    var anaExpense: String = "Расход"
    var anaSavings: String = "Сбережения"
    var anaSavingsTrend: String = "Тренд нормы сбережений"
    var anaMonthlyAverage: String = "Среднее за месяц (за год)"
    // v1.11.0: extended analytics (mirror of the Telegram mini-app)
    // v1.11.2: avg-check month/12-mo toggle, pace redesign, info buttons,
    //          heatmap tap; the auto-detected subscriptions card removed
    var anaAvgCheck: String = "Средний чек по категориям"
    var anaAvgCheckPeriodMonth: String = "Месяц"
    var anaAvgCheckPeriodYear: String = "12 мес."
    var anaAvgCheckEmptyMonth: String = "Нет расходов в текущем месяце"
    var anaAvgCheckEmptyYear: String = "Нет расходов за 12 месяцев"
    var anaOpsCount: (Int) -> String = { "${it} оп." }
    var anaTop5Month: String = "Топ-5 месяца"
    var anaNoPrev: String = "без истории"
    var anaCushion: String = "Финансовая подушка"
    var anaCushionBase: String = "Баланс + копилка"
    var anaCushionAvgExp: String = "Средний расход/мес"
    var anaCushionMonths: (String) -> String = { "$it мес. при текущих расходах" }
    var anaCushionLowData: String = "Мало данных для расчёта"
    var anaPace: String = "Темп бюджета"
    var anaPaceDisabled: String = "Задайте лимит бюджета в настройках"
    var anaPaceAwaiting: String = "Ждём доход за месяц"
    var anaPaceOn: String = "По плану"
    var anaPaceFast: String = "Быстрее плана"
    var anaPaceSlow: String = "Медленнее плана"
    var anaPaceSpent: String = "Потрачено"
    var anaPacePlanToday: String = "План на сегодня"
    var anaPaceLimit: String = "Лимит"
    var anaPaceDayOf: (Int, Int) -> String = { d, dim -> "день $d из $dim" }
    var anaPaceSpentPlan: (String, String) -> String = { s, e -> "потрачено $s · план на сегодня $e" }
    var anaBigExpenses: String = "Крупные траты"
    var anaBalanceCurve: String = "Кривая баланса"
    var anaWeekday: String = "Расходы по дням недели"
    var anaWeekdayTotal: (String) -> String = { "Итого за год: $it" }
    var anaCalendar: String = "Расходы по дням"
    var anaCalendarMax: (String) -> String = { "День с максимумом: $it · строки: Пн–Вс" }
    var anaHeatTapHint: String = "Нажмите на день — покажет дату и сумму"
    var anaHeatNoSpend: String = "без расходов"
    var anaHeatLess: String = "меньше"
    var anaHeatMore: String = "больше"
    var anaInfo: String = "Пояснение"
    // v1.11.2: card explanations (what it shows + how it is calculated)
    var anaExBars: String = "Столбцы — доход и расход по каждому месяцу выбранного года. Зелёный — поступления, красный — траты. Копилка (сбережения) в доход не входит, это отдельный тип операции."
    var anaExCum: String = "Накопительный итог: сколько сейчас в копилке с начала года. Линия растёт на сумму вложений в каждый месяц и падает на траты из накоплений (снятия)."
    var anaExDonut: String = "Распределение расходов по категориям за текущий год. Размер сектора пропорционален сумме трат в категории. Показаны 5 крупнейших, остальное — сектор «Прочее». Траты из накоплений учитываются как расходы."
    var anaExTop5: String = "5 категорий с самыми большими тратами за текущий год. Сумма — все расходы этой категории с января по декабрь, отсортировано по убыванию. Траты из накоплений учитываются как расходы."
    var anaExMTable: String = "Помесячные итоги: доход, расход и сбережения за каждый месяц года. Показаны только месяцы, где была хотя бы одна операция."
    var anaExRates: String = "Норма сбережений помесячно: сколько процентов дохода ушло в копилку. Формула: сбережения месяца ÷ доход месяца × 100. Месяцы без дохода не учитываются."
    var anaExAverages: String = "Средние месячные суммы за 12 месяцев года: (сумма доходов ÷ 12), (сумма расходов ÷ 12), (сумма сбережений ÷ 12). Сбережения — за вычетом трат из накоплений."
    var anaExAvgCheck: String = "Средний чек в категории = сумма расходов в ней ÷ количество операций. Показаны 6 категорий с самым большим числом операций. Переключатель: текущий месяц или последние 12 месяцев. Траты из накоплений учитываются как расходы."
    var anaExTop5Trend: String = "Топ-5 категорий текущего месяца в сравнении с прошлым месяцем. Проценты — изменение: (траты сейчас − траты в прошлом месяце) ÷ прошлые траты × 100. Если прошлых трат не было — «без истории». Траты из накоплений учитываются как расходы."
    var anaExCushion: String = "Финансовая подушка: на сколько месяцев хватит накоплений. База = текущий баланс (доходы − расходы − копилка) + деньги в копилке (вложения − траты из накоплений). Средний расход — по месяцам, где были траты. Месяцы = база ÷ средний расход."
    var anaExPace: String = "Скорость трат месячного лимита. План на сегодня = лимит × (день месяца ÷ дней в месяце). Если потрачено больше плана — «Быстрее плана», меньше — «Медленнее плана». Линия-маркер — где должен быть план, полоса — что уже потрачено."
    var anaExBigExp: String = "10 самых крупных трат за последние 3 месяца (сначала текущего). Каждая строка — одна операция, отсортировано по сумме. Траты из накоплений учитываются как расходы."
    var anaExCurve: String = "Кривая баланса в текущем месяце: остаток средств на конец каждого дня (баланс на начало месяца + накопленный результат дня). Показывает, как менялись деньги в течение месяца."
    var anaExWeekday: String = "Расходы по дням недели за текущий год. Сумма всех расходов, чья дата выпадает на этот день недели (Пн–Вс). Помогает видеть, в какие дни недели тратится больше. Траты из накоплений учитываются как расходы."
    var anaExHeatmap: String = "Карта расходов за последние 12 месяцев: каждый квадратик — день, чем темнее цвет, тем больше потрачено (5 оттенков + пустой день). Нажмите на день, чтобы увидеть дату и точную сумму. Рядом с сеткой — шкала «меньше…больше». Траты из накоплений учитываются как расходы."
    // --- goals ---
    var goalEmptyTitle: String = "Нет целей"
    var goalEmptyHint: String = "Создайте первую цель для накоплений"
    var goalCreateBtn: String = "Создать цель"
    var goalDone: (String) -> String = { "$it% выполнено" }
    var goalRemaining: (String) -> String = { "Осталось: $it ₽" }
    var goalDeadline: (String) -> String = { "Срок: $it" }
    var goalRecommended: (String) -> String = { "Рекомендуемый мес. взнос: $it ₽" }
    var goalContribute: String = "Внести"
    var goalDeleteCd: String = "Удалить цель"
    var goalNewTitle: String = "Новая цель"
    var goalEditTitle: String = "Изменить цель"
    var goalName: String = "Название"
    var goalDescription: String = "Описание"
    var goalTarget: String = "Целевая сумма (₽)"
    var goalDeadlineField: String = "Срок (ГГГГ-ММ-ДД)"
    var goalContributionTitle: (String) -> String = { "Взнос в $it" }
    var goalSaved: (String) -> String = { "Накоплено: $it ₽" }
    var goalContributionAmount: String = "Сумма взноса (₽)"
    var goalContributionDesc: String = "Взнос в цель"
    var goalContributionNote: (String) -> String = { "Цель: $it" }
    // --- notifications ---
    var notifChannelName: String = "Напоминания MoneyAPP"
    var notifChannelDesc: String = "Ежедневные напоминания о записи расходов"
    var notifReminderTitle: String = "Не забудьте записать расходы"
    var notifReminderText: String = "Откройте MoneyAPP и добавьте сегодняшние транзакции"
    // --- importer / backup errors (surfaced in snackbars) ---
    var errOpenFile: String = "Не удалось открыть файл"
    var errNoSheet: String = "Лист «Транзакции» не найден"
    var errEmptyBackup: String = "Пустая или некорректная резервная копия"
    var errOpenFileWrite: String = "Не удалось открыть файл для записи"
}

val RuStrings = Strings()

val EnStrings: Strings = Strings().apply {
    // --- common ---
    ok = "OK"
    cancel = "Cancel"
    save = "Save"
    create = "Create"
    delete = "Delete"
    edit = "Edit"
    add = "Add"
    close = "Close"
    chooseFile = "Choose file"
    importBtn = "Import"
    noData = "No data"
    // --- transaction types / payment ---
    txTypeIncome = "Income"
    txTypeExpense = "Expense"
    txTypeSavings = "Save"
    txTypeSavingsSpend = "From savings"
    txTypeIncomePlural = "Income"
    txTypeExpensePlural = "Expenses"
    txTypeSavingsPlural = "Savings"
    pmCash = "Cash"
    pmCard = "Card"
    budgetTypeIncome = "Income"
    budgetTypeExpense = "Expense"
    budgetTypeSavings = "Savings"
    budgetTypeSummary = "Total"
    budgetStatusOk = "OK"
    budgetStatusClose = "Close"
    budgetStatusOver = "Over"
    budgetStatusUnder = "Under"
    goalStatusInProgress = "In progress"
    goalStatusCompleted = "Done"
    goalStatusOverdue = "Overdue"
    // --- months ---
    monthsFull = listOf(
        "", "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    monthsShort = listOf(
        "", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    weekdayShort = listOf(
        "", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
    )
    // --- nav ---
    navDashboard = "Dashboard"
    navTransactions = "Transactions"
    navBudget = "Budget"
    navGoals = "Goals"
    navAnalytics = "Analytics"
    navSettings = "Settings"
    navCategories = "Categories"
    // --- day headers ---
    today = "Today"
    yesterday = "Yesterday"
    dayBeforeYesterday = "Day before yesterday"
    // --- dashboard ---
    dashBalance = "Balance"
    dashIncome = "Income"
    dashExpenses = "Expenses"
    dashSavings = "Savings"
    dashSavingsRate = "Savings rate"
    dashSummaryFor = { "Summary for $it" }
    dashExpensesByCategory = "Expenses by category"
    dashNoExpensesTitle = "No expenses this month"
    dashNoExpensesHint = "Add transactions — the spending structure will appear here"
    dashMonthlyLimit = "Monthly limit"
    dashLeft = { left, pct -> "$left left • $pct%" }
    dashOver = { over, pct -> "Over by $over • $pct%" }
    prevMonth = "Previous month"
    nextMonth = "Next month"
    prevYear = "Previous year"
    nextYear = "Next year"
    dashPeriodMonth = "Month"
    dashPeriodYear = "Year"
    // --- transactions ---
    txSearchPlaceholder = "Search by description"
    txSearch = "Search"
    txDateFilter = "Dates"
    txDateFrom = "From"
    txDateTo = "To"
    txDateClear = "Reset"
    txFilterAll = "All"
    txEmptyTitle = "No transactions"
    txEmptyHint = "Tap “Add” to record your first transaction"
    txDeleteTitle = "Delete transaction?"
    txNewTitle = "New transaction"
    txEditTitle = "Edit transaction"
    txAmount = "Amount (₽)"
    txCategory = "Category"
    txNoCategoriesHint = "No categories of this type yet — add the first one with the “+” button"
    txDescription = "Description"
    txNote = "Note"
    txSaveChanges = "Save changes"
    txClear = "Clear"
    // --- categories ---
    catManageTitle = "Manage categories"
    catDragHint = "Drag by the handle on the left within a section to reorder. Tap a row to edit, the trash icon deletes."
    catEmptyHint = "No categories — add one via “Add”"
    catDeleteTitle = "Delete category?"
    catDeleteText = { "“$it” will be deleted. Existing transactions are kept." }
    catSectionIncome = "Income"
    catSectionExpense = "Expenses"
    catSectionSavings = "Savings"
    catNewTitle = "New category"
    catEditTitle = "Edit category"
    catRenameHint = "The new name and type are applied to linked transactions and budget plans"
    catName = "Name"
    catIcon = "Icon"
    catIconAuto = "Auto"
    catMove = "Move"
    catWillBeAddedTo = { "Will be added to the “$it” type" }
    catDuplicate = "A category with this name already exists"
    catSortHint = "Sort, add, and hide categories"
    // --- settings: account / sync ---
    setAccount = "Account"
    setUsername = "Username"
    setCurrency = "Currency"
    setSync = "Sync"
    setGoogleSheets = "Google Sheets"
    setGoogleSheetsSub = "one-time setup, ~5 minutes"
    setExpand = "Expand"
    setClientId = "Client ID (step 5)"
    setSpreadsheetLink = "Link to your own spreadsheet (optional)"
    setSpreadsheetLinkHint = "Blank = the app creates a spreadsheet. Own spreadsheet: paste a docs.google.com/spreadsheets/d/… link — the data will live there (on the “Transactions” tab; other tabs are left untouched). After connecting this block hides — only “Sync” remains."
    setConnectGoogle = "Connect Google account"
    setConnected = "Connected"
    setSpreadsheet = { "Spreadsheet: $it" }
    setSyncNow = "Sync"
    setDisconnect = "Disconnect"
    guideStep1 = "Create a project (any name, e.g. “MoneyAPP”)."
    guideStep2 = "“APIs & Services” → “Library”: enable Google Sheets API and Google Drive API."
    guideStep3 = "“OAuth consent screen”: choose External and add your email to “Test users”."
    guideStep4 = "“Credentials” → “Create credentials” → “OAuth client ID” → type “Desktop app”. Add moneyapp://oauth2-callback to the “Authorized redirect URIs” field."
    guideStep5 = "Copy the Client ID and paste it into the field below."
    // --- settings: notifications / appearance ---
    setNotifications = "Notifications"
    setDailyReminder = "Daily reminder"
    setReminderTime = "Reminder time"
    setAppearance = "Appearance"
    themeSystem = "System"
    themeLight = "Light"
    themeDark = "Dark"
    setLanguage = "Language"
    langRu = "Русский"
    langEn = "English"
    // --- settings: budget ---
    setMonthlyBudget = "Monthly budget"
    setCapLimit = "Limit (₽/month)"
    setCapLimitHint = "Blank — the total limit is off"
    setCapLinkIncome = "Link to income"
    setCapIncomePct = "Income share, %"
    setTemplateGroup = "Per-category template"
    setCopyPrevMonth = "Copy previous month’s plan"
    // --- settings: data / categories / about ---
    setData = "Data"
    setExportExcel = "Excel export"
    setImportExcel = "Excel import"
    setBackup = "Backup (JSON)"
    setRestore = "Restore from backup"
    setCategories = "Categories"
    setAbout = "About"
    setVersion = { "MoneyAPP v$it" }
    setTagline = "Personal finance tracker"
    setGmsFree = "GMS-free • Works on Huawei Mate X3"
    setLastCrash = "Last crash log"
    setShareCrash = "Share log"
    setHide = "Hide"
    // --- settings: dialogs ---
    setImportExcelTitle = "Excel import"
    setImportFound = { "Transactions found: $it" }
    setImportPeriod = { "Period: $it" }
    setImportCounts = { a, b, c, d -> "Income: $a • Expenses: $b • Saved: $c • From savings: $d" }
    setImportTotal = { "Total: $it ₽" }
    setImportDupes = "Duplicates (date + type + category + description + amount) will be skipped"
    setRestoreTitle = "Restore from backup"
    setRestoreText = "The backup will be merged with current data: new records are added, existing ones updated if newer in the backup. Nothing is deleted."
    // --- settings: messages ---
    setErrNoClientId = "Enter the OAuth Client ID: Google Cloud Console → Credentials → Create OAuth Client ID (type: desktop app)"
    setErrAuthError = "Authorization error"
    setErrAuthCancelled = "Authorization cancelled"
    setErrNoPkce = "No PKCE verifier"
    setErrBadLink = "Couldn’t recognize the spreadsheet link — paste the full address from the browser’s address bar"
    setErrNoSheetAccess = "No access to the spreadsheet at this link. Check that the address is copied in full and the spreadsheet belongs to the connected Google account"
    setMsgConnected = "Google connected. Auto-sync every 15 minutes."
    setMsgConnectError = { "Couldn’t connect: $it" }
    setMsgSyncStarted = "Sync started"
    setMsgDisconnected = "Google disconnected"
    setMsgReminderTime = { "Reminder time: $it" }
    setErrOpenFile = "Couldn’t open the file"
    setMsgExcelExported = { "Excel exported: $it transactions" }
    setMsgExportError = { "Export error: $it" }
    setMsgNoTxInFile = "No transactions found in the file"
    setMsgReadFileError = { "Couldn’t read the file: $it" }
    setMsgImportNothing = { "No new transactions (duplicates: $it)" }
    setMsgImportDone = { added, skipped -> "Imported: $added, duplicates skipped: $skipped" }
    setMsgImportError = { "Import error: $it" }
    setMsgBackupCreated = { tx, goals -> "Backup created: $tx transactions, $goals goals" }
    setMsgBackupError = { "Backup error: $it" }
    setMsgRestored = { added, updated, goals, cats -> "Restored: transactions +$added/=$updated, goals +$goals, categories +$cats" }
    setMsgRestoreError = { "Restore error: $it" }
    crashShareSubject = "MoneyApp — crash log"
    crashChooserTitle = "Send log"
    // --- budget screen ---
    budNotConfigured = "Budget not configured"
    budNotConfiguredHint = "Set up a budget plan for this month"
    budOnTrack = "On track"
    budCloseToLimit = "Close to the limit"
    budLimitExceeded = "Limit exceeded"
    budLeft = { "$it left" }
    budOver = { "Over by $it" }
    budAwaitingIncome = "Awaiting income"
    budAwaitingIncomeText = { "Limit = $it% of monthly income. It will appear automatically as soon as income is recorded for this month." }
    budMedianIncome = "No income this month"
    budMedianIncomeText = { pct, median -> "No income recorded for this month yet. Limit = $pct% of the yearly median income ($median ₽)." }
    budSpentThisMonth = { "Spent this month: $it" }
    budDifference = { "Difference: $it" }
    // v1.11.5: budget plan dialog (parity with the mini-app)
    budAddPlan = "Add plan"
    budNewPlan = "New plan"
    budEditPlan = "Edit plan"
    budPlanAmount = "Plan (₽)"
    budCategory = "Category"
    budPlanSaved = "Plan saved"
    budPlanDeleted = "Plan deleted"
    budPlanFromTemplate = "Plan from the category template. Saving applies to this month only; the template (in Settings) is unchanged."
    budPlanFromPrev = "Plan copied from the previous month. Saving applies to this month only."
    // --- analytics ---
    anaIncomeExpenses = "Income / Expenses"
    anaNoDataYear = "No data for this year"
    anaCumulative = "Savings (cumulative)"
    anaOther = "Other"
    anaTotal = "Total"
    anaTop5 = "Top-5 expenses"
    anaMonthly = "Monthly analytics"
    anaNoDataCompare = "No data to compare"
    anaMonth = "Month"
    anaIncome = "Income"
    anaExpense = "Expense"
    anaSavings = "Savings"
    anaSavingsTrend = "Savings rate trend"
    anaMonthlyAverage = "Monthly average (year)"
    // v1.11.0: extended analytics (mirror of the Telegram mini-app)
    // v1.11.2: avg-check month/12-mo toggle, pace redesign, info buttons,
    //          heatmap tap; the auto-detected subscriptions card removed
    anaAvgCheck = "Average check by category"
    anaAvgCheckPeriodMonth = "Month"
    anaAvgCheckPeriodYear = "12 mo."
    anaAvgCheckEmptyMonth = "No expenses in the current month"
    anaAvgCheckEmptyYear = "No expenses over 12 months"
    anaOpsCount = { "${it} ops" }
    anaTop5Month = "Month top-5"
    anaNoPrev = "no history"
    anaCushion = "Financial cushion"
    anaCushionBase = "Balance + piggy bank"
    anaCushionAvgExp = "Avg expense/mo"
    anaCushionMonths = { "$it mo. at current spending" }
    anaCushionLowData = "Not enough data to calculate"
    anaPace = "Budget pace"
    anaPaceDisabled = "Set a budget limit in settings"
    anaPaceAwaiting = "Awaiting income for the month"
    anaPaceOn = "On track"
    anaPaceFast = "Faster than plan"
    anaPaceSlow = "Slower than plan"
    anaPaceSpent = "Spent"
    anaPacePlanToday = "Plan for today"
    anaPaceLimit = "Limit"
    anaPaceDayOf = { d, dim -> "day $d of $dim" }
    anaPaceSpentPlan = { s, e -> "spent $s · plan for today $e" }
    anaBigExpenses = "Big expenses"
    anaBalanceCurve = "Balance curve"
    anaWeekday = "Expenses by weekday"
    anaWeekdayTotal = { "Year total: $it" }
    anaCalendar = "Expenses by day"
    anaCalendarMax = { "Max day: $it · rows: Mon–Sun" }
    anaHeatTapHint = "Tap a day to see date and amount"
    anaHeatNoSpend = "no spending"
    anaHeatLess = "less"
    anaHeatMore = "more"
    anaInfo = "Explanation"
    // v1.11.2: card explanations (what it shows + how it is calculated)
    anaExBars = "Bars are income and expenses for each month of the selected year. Green — money in, red — spending. The piggy bank (savings) is not part of income — it is a separate transaction type."
    anaExCum = "Cumulative total: how much is in the piggy bank since the start of the year. The line grows by each month's contribution and drops by spending from savings (withdrawals)."
    anaExDonut = "Expense distribution by category for the current year. Sector size is proportional to the amount spent in the category. The 5 largest are shown, the rest is the \"Other\" sector. Spending from savings counts as expenses."
    anaExTop5 = "The 5 categories with the largest spending for the current year. The amount is all spending in that category from January to December, sorted descending. Spending from savings counts as expenses."
    anaExMTable = "Monthly totals: income, expenses and savings for each month of the year. Only months with at least one operation are shown."
    anaExRates = "Monthly savings rate: the percentage of income that went into the piggy bank. Formula: month's savings ÷ month's income × 100. Months without income are not counted."
    anaExAverages = "Average monthly amounts over the 12 months of the year: (total income ÷ 12), (total expenses ÷ 12), (total savings ÷ 12). Savings are net of spending from savings."
    anaExAvgCheck = "Average check in a category = total spending in it ÷ number of operations. The 6 categories with the most operations are shown. Toggle: current month or the last 12 months. Spending from savings counts as expenses."
    anaExTop5Trend = "Top-5 categories of the current month vs the previous one. The percentage is the change: (current spending − previous month) ÷ previous month × 100. If there was no previous spending — \"no history\". Spending from savings counts as expenses."
    anaExCushion = "Financial cushion: how many months the savings will last. Base = current balance (income − expenses − piggy bank) + money in the piggy bank (contributions − spending from savings). The average expense is computed over months with spending. Months = base ÷ average expense."
    anaExPace = "How fast the monthly limit is being spent. Plan for today = limit × (day of month ÷ days in month). If spending exceeds the plan — \"Faster than plan\", if less — \"Slower than plan\". The marker line is where the plan should be, the bar is what has been spent."
    anaExBigExp = "The 10 largest expenses over the last 3 months (including the current one). Each row is a single operation, sorted by amount. Spending from savings counts as expenses."
    anaExCurve = "Balance curve for the current month: the balance at the end of each day (start-of-month balance + the accumulated daily result). Shows how the money changed during the month."
    anaExWeekday = "Expenses by weekday for the current year. The sum of all expenses whose date falls on that weekday (Mon–Sun). Helps see which weekdays spend more. Spending from savings counts as expenses."
    anaExHeatmap = "Expense map for the last 12 months: each square is a day, the darker the color the more was spent (5 shades + an empty day). Tap a day to see the date and the exact amount. Next to the grid is the \"less…more\" scale. Spending from savings counts as expenses."
    // --- goals ---
    goalEmptyTitle = "No goals"
    goalEmptyHint = "Create your first savings goal"
    goalCreateBtn = "Create goal"
    goalDone = { "$it% done" }
    goalRemaining = { "Remaining: $it" }
    goalDeadline = { "Deadline: $it" }
    goalRecommended = { "Recommended monthly: $it" }
    goalContribute = "Contribute"
    goalDeleteCd = "Delete goal"
    goalNewTitle = "New goal"
    goalEditTitle = "Edit goal"
    goalName = "Name"
    goalDescription = "Description"
    goalTarget = "Target amount (₽)"
    goalDeadlineField = "Deadline (YYYY-MM-DD)"
    goalContributionTitle = { "Contribution to $it" }
    goalSaved = { "Saved: $it" }
    goalContributionAmount = "Contribution amount (₽)"
    goalContributionDesc = "Contribution to goal"
    goalContributionNote = { "Goal: $it" }
    // --- notifications ---
    notifChannelName = "MoneyAPP reminders"
    notifChannelDesc = "Daily reminders to record expenses"
    notifReminderTitle = "Don’t forget to record your expenses"
    notifReminderText = "Open MoneyAPP and add today’s transactions"
    // --- importer / backup errors ---
    errOpenFile = "Couldn’t open the file"
    errNoSheet = "“Transactions” sheet not found"
    errEmptyBackup = "Empty or invalid backup"
    errOpenFileWrite = "Couldn’t open the file for writing"
}

object L10n {
    // Snapshot state: composable readers re-compose on the switch, plain
    // (non-Compose) code just reads the value.
    private val _lang = mutableStateOf(AppLang.RU)

    var lang: AppLang
        get() = _lang.value
        set(value) {
            if (_lang.value != value) _lang.value = value
        }

    val s: Strings
        get() = if (lang == AppLang.EN) EnStrings else RuStrings

    val locale: Locale get() = if (lang == AppLang.EN) Locale.ENGLISH else Locale("ru")

    /** Full month name for charts/titles, month 1..12. */
    fun monthFull(month: Int): String = s.monthsFull[month.coerceIn(1, 12)]

    /** Short month name for chart axes, month 1..12. */
    fun monthShort(month: Int): String = s.monthsShort[month.coerceIn(1, 12)]
}

// --- Localized labels for domain enums (UI only; data formats keep labelRu) ---

fun TransactionType.label(): String = when (this) {
    TransactionType.INCOME -> L10n.s.txTypeIncome
    TransactionType.EXPENSE -> L10n.s.txTypeExpense
    TransactionType.SAVINGS -> L10n.s.txTypeSavings
    TransactionType.SAVINGS_SPEND -> L10n.s.txTypeSavingsSpend
}

fun PaymentMethod.label(): String = when (this) {
    PaymentMethod.CASH -> L10n.s.pmCash
    PaymentMethod.CARD -> L10n.s.pmCard
}

fun BudgetType.label(): String = when (this) {
    BudgetType.INCOME -> L10n.s.budgetTypeIncome
    BudgetType.EXPENSE -> L10n.s.budgetTypeExpense
    BudgetType.SAVINGS -> L10n.s.budgetTypeSavings
    BudgetType.SUMMARY -> L10n.s.budgetTypeSummary
}

fun BudgetStatus.label(): String = when (this) {
    BudgetStatus.OK -> L10n.s.budgetStatusOk
    BudgetStatus.CLOSE -> L10n.s.budgetStatusClose
    BudgetStatus.OVER -> L10n.s.budgetStatusOver
    BudgetStatus.UNDER -> L10n.s.budgetStatusUnder
}

fun GoalStatus.label(): String = when (this) {
    GoalStatus.IN_PROGRESS -> L10n.s.goalStatusInProgress
    GoalStatus.COMPLETED -> L10n.s.goalStatusCompleted
    GoalStatus.OVERDUE -> L10n.s.goalStatusOverdue
}

// --- Cold-process language resolution ---------------------------------------
// Receivers/workers that run without a warm Application scope read the
// language from DataStore themselves (L10n.lang is in-memory and defaults
// to RU until MoneyAppApplication.onCreate finishes).

private val Context.l10nDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Mirrors SettingsRepository's private "language" key — keep them in sync. */
val L10nLanguageKey = stringPreferencesKey("language")

/**
 * v1.9.0: the active language from DataStore, with a bounded wait.
 * Falls back to the current in-memory value on any failure (DataStore not
 * ready, process under memory pressure) — never blocks the caller long.
 */
suspend fun resolveLanguage(context: Context, timeoutMs: Long = 1500): AppLang =
    try {
        val value = withTimeout(timeoutMs) {
            context.l10nDataStore.data.first()[L10nLanguageKey]
        }
        if (value == "en") AppLang.EN else AppLang.RU
    } catch (_: Exception) {
        L10n.lang
    }
