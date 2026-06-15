# Розгорнуті відповіді для захисту дипломної роботи

Цей документ написаний у форматі відповідей, які можна вивчати й озвучувати на захисті. Формулювання навмисно не зведені до коротких тез: у кожному розділі спочатку пояснюється загальне поняття, потім показується, як воно реалізоване саме в цьому проєкті, і лише після цього дається обґрунтування вибору та порівняння з альтернативами.

## 1. Чому був обраний патерн MVVM

MVVM розшифровується як Model-View-ViewModel. Це архітектурний патерн для побудови інтерфейсних застосунків, у якому екран не повинен самостійно працювати з базою даних, мережею, файлами або складною бізнес-логікою. Його основна ідея полягає в тому, що UI показує готовий стан, ViewModel цей стан готує, а Model містить дані та правила роботи з ними.

У моєму Android-застосунку цей патерн особливо доречний, бо застосунок написаний на Jetpack Compose. Compose працює декларативно: я не кажу інтерфейсу “знайди цей TextView і зміни текст”, а описую, яким має бути екран для певного стану. Якщо змінюється `UiState`, Compose сам перемальовує потрібні частини екрана. MVVM добре підходить до такої моделі, бо ViewModel природно стає джерелом стану для Compose-екрана.

У проєкті View - це composable-екрани та UI-компоненти: `HomeScreen`, `TabListScreen`, `TabViewerScreen`, `SettingsScreen`, `GoalsScreen`, `AiAssistantScreen`, `NotesScreen`. Їхня роль - показати дані користувачу і передати подію назад у ViewModel. Наприклад, коли користувач натискає кнопку синхронізації, `SettingsScreen` не створює `FirebaseFirestore`, не читає базу і не вирішує конфлікти. Він просто викликає `settingsViewModel.syncCloud()`. Коли користувач відкриває табулатуру, екран не читає файл напряму, а викликає `viewModel.loadLesson(lessonId)`.

ViewModel у проєкті - це класи з пакета `presentation`: `MainViewModel`, `TabListViewModel`, `TabViewerViewModel`, `GoalsViewModel`, `SettingsViewModel`, `AiAssistantViewModel`, `TabNotesViewModel`, `ThemeViewModel`, `AuthViewModel`. Вони не малюють UI і не містять верстки. Їхня задача - тримати стан, запускати корутини, викликати use case або репозиторії, обробляти результат і оновлювати `StateFlow`.

Наприклад, `TabListViewModel` формує `TabListUiState`. У цьому стані є список вбудованих уроків, список користувацьких табулатур, вибрана складність, вибрана папка, пошуковий запит, режим сортування, прогрес по кожній табулатурі, ознаки завантаження і повідомлення про помилки. Екран не рахує ці речі сам. Він читає:

```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```

і вже на основі `uiState` показує вкладки, картки уроків, фільтри, прогрес і діалоги.

Model у цьому проєкті - це не один клас і не одна таблиця. Model включає доменні моделі, інтерфейси репозиторіїв, use case і реалізації data-шару. Наприклад, `TabItem`, `Lesson`, `Session`, `Goal`, `AudioNote`, `TextNote`, `TabPlaybackProgress` - це доменні моделі. `TabRepository`, `SessionRepository`, `GoalRepository`, `SyncRepository`, `AiAssistantRepository` - це доменні контракти. `TabRepositoryImpl`, `FirestoreSyncRepositoryImpl`, `DataStoreAppSettingsRepository` - це data-реалізації, які вже працюють із Room, DataStore, Firebase або файлами.

Типовий pipeline виглядає так: Room DAO повертає `Flow`, репозиторій перетворює Room entity у доменну модель, ViewModel збирає ці дані в `UiState`, а Compose-екран відображає готовий стан. Це важливо, бо `Flow` дозволяє автоматично оновлювати UI при зміні бази даних.

```kotlin
// DAO працює з локальною БД.
@Transaction
@Query("SELECT * FROM tabs WHERE isUserTab = 1")
fun getUserTabs(): Flow<List<TabWithTags>>

// Репозиторій перетворює entity у доменну модель.
override fun observeUserTabs(): Flow<List<TabItem>> =
    tabDao.getUserTabs().map { tabs -> tabs.map { it.toDomain() } }

// ViewModel формує стан екрана.
tabRepository.observeUserTabs()
    .onEach { userTabs ->
        _uiState.update { it.copy(userTabs = userTabs, isUserTabsLoading = false) }
    }
    .launchIn(viewModelScope)
```

Якщо комісія запитає, чому не MVC, відповідь така: MVC для Android часто призводить до великих Activity або Fragment, які одночасно відповідають за UI, запити до даних, навігацію, життєвий цикл і бізнес-логіку. Для мого застосунку це було б особливо небезпечно, бо один екран табулатури має багато незалежних станів: WebView, alphaTab, відтворення, loop, AI-помічник, нотатки, режим практики, SoundFont, Base64-кеш, відновлення позиції, аналіз аплікатури. Якщо помістити це в Activity або Controller, код стане важким для тестування й підтримки.

Якщо запитають про MVP, його теж можна було використати, але він гірше підходить до Compose. У MVP Presenter часто імперативно командує View: “покажи loading”, “онови список”, “відкрий діалог”. Compose краще працює через опис стану: є `UiState`, і UI є функцією від цього стану. Тому MVVM природніше поєднується з Compose, `StateFlow` і lifecycle-aware collection.

Якщо запитають про MVI, правильна відповідь така: MVI міг би дати ще суворіший контроль стану через intents і reducer, але для цього проєкту це був би надлишковий рівень складності. У застосунку багато інтеграцій з Android API, WebView, Firebase, DataStore, Room і медіа. Повний MVI додав би багато службового коду, тоді як MVVM уже достатньо чітко відділяє екран від логіки й добре тестується.

Коротке сильне формулювання для захисту: MVVM був обраний тому, що застосунок має реактивний Compose-інтерфейс і багато джерел стану. View тільки показує стан, ViewModel керує станом і діями, Model відповідає за дані та правила. Це зменшує зв’язність, спрощує тестування і не дозволяє перетворити Activity або екран на моноліт.

## 2. Як реалізована чиста архітектура

Чиста архітектура - це підхід, у якому система ділиться на шари, а залежності спрямовані від зовнішніх деталей до внутрішньої бізнес-логіки. Головне правило звучить так: доменна логіка не повинна залежати від конкретного UI-фреймворку, бази даних або мережевої бібліотеки. Іншими словами, бізнес-правила повинні знати, що потрібно зробити, але не повинні знати, якою саме технологією це виконується.

У моєму проєкті це реалізовано через три основні шари. Шар `presentation` містить Compose-екрани, ViewModel і UI state. Шар `domain` містить доменні моделі, інтерфейси репозиторіїв і use case. Шар `data` містить конкретні реалізації: Room entity і DAO, DataStore, Firebase, роботу з файлами, AI-запити, мапери та синхронізацію.

Наприклад, у доменному шарі є інтерфейс:

```kotlin
interface TabRepository {
    fun getTabs(): Flow<List<TabItem>>
    suspend fun getLesson(id: String): Lesson?
    suspend fun updateTab(tab: TabItem)
    suspend fun addUserTab(uriString: String)
}
```

Цей інтерфейс не містить жодного слова про Room, Firestore, assets або Android `ContentResolver`. Він описує можливості, які потрібні застосунку: отримати табулатури, отримати урок, оновити табулатуру, додати користувацький файл. Реалізація `TabRepositoryImpl` уже належить до data-шару і знає, що вбудовані уроки читаються з `assets/lessons/lessons.json`, користувацькі файли копіюються у внутрішнє сховище, а метадані зберігаються в Room.

Use case у проєкті потрібні для сценаріїв, які не є просто одним запитом до однієї таблиці. Наприклад, `LoadTabViewerLessonUseCase` при відкритті табулатури одночасно позначає її як відкриту, читає урок, читає `TabItem` і читає збережений прогрес відтворення. Це краще, ніж писати всі ці кроки прямо у ViewModel, бо use case описує завершений бізнес-сценарій.

```kotlin
class LoadTabViewerLessonUseCase @Inject constructor(
    private val tabRepository: TabRepository,
    private val tabPlaybackProgressRepository: TabPlaybackProgressRepository
) {
    suspend operator fun invoke(id: String): TabViewerLessonLoadResult {
        tabRepository.markTabOpened(id)

        val lesson = tabRepository.getLesson(id)
        val tabItem = tabRepository.getTabById(id)
        val savedProgress = tabPlaybackProgressRepository.getByTabId(id)

        return TabViewerLessonLoadResult(
            lesson = lesson,
            tabItem = tabItem,
            savedProgress = savedProgress,
            shouldRestore = savedProgress != null,
            tabPath = lesson?.tabsGpPath ?: tabItem?.filePath
        )
    }
}
```

У реальному коді ці операції виконуються паралельно через `coroutineScope` і `async`, але суть саме така: use case збирає один сценарій із кількох джерел.

Впровадження залежностей реалізоване через Hilt. Dependency Injection означає, що клас не створює свої залежності вручну, а отримує їх ззовні. Наприклад, `TabViewerViewModel` не викликає `TabRepositoryImpl(...)` і не створює Room database. Він просто оголошує в конструкторі, що йому потрібно:

```kotlin
@HiltViewModel
class TabViewerViewModel @Inject constructor(
    private val loadTabViewerLessonUseCase: LoadTabViewerLessonUseCase,
    private val tabFileRepository: TabFileRepository,
    private val soundFontRepository: SoundFontRepository,
    private val tabPlaybackProgressRepository: TabPlaybackProgressRepository,
    private val dispatchers: AppDispatchers
) : ViewModel()
```

Hilt сам вирішує, яку реалізацію підставити. У `RepositoryModule` доменні інтерфейси зв’язуються з конкретними класами:

```kotlin
@Binds
@Singleton
abstract fun bindTabRepository(
    implementation: TabRepositoryImpl
): TabRepository
```

Це означає: коли комусь потрібен `TabRepository`, дай `TabRepositoryImpl`. Для об’єктів, які не можна просто створити через конструктор, використовується `@Provides`. Наприклад, Room database створюється в `DataModule`:

```kotlin
@Provides
@Singleton
fun provideAppDatabase(context: Context): AppDatabase {
    return Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "app_database"
    )
    .addMigrations(...)
    .fallbackToDestructiveMigration()
    .build()
}
```

Тут `@Provides` потрібен тому, що Room database створюється через builder, а не простим конструктором. Те саме стосується `FirebaseAuth.getInstance()`, `FirebaseFirestore.getInstance()`, `FirebaseStorage.getInstance()` і `PreferenceDataStoreFactory`.

Окремо в проєкті є `AppDispatchers`, який інкапсулює `Dispatchers.IO`, `Dispatchers.Default` і `Dispatchers.Main`. Це важливо для тестування. У production Hilt дає справжні dispatcher-и, а в unit-тестах вони підміняються тестовим dispatcher-ом через `MainDispatcherRule`. Завдяки цьому тести можуть керувати часом і корутинами.

Альтернатива чистій архітектурі - напряму звертатися з ViewModel до Room DAO, Firebase і DataStore. Це простіше на перших етапах, але створює сильну зв’язність. Наприклад, якби `SettingsViewModel` напряму працював з Firestore, його було б важче тестувати, а зміна синхронізації вимагала б переписування ViewModel. У моєму проєкті ViewModel залежить від `SyncRepository`, а конкретна реалізація `FirestoreSyncRepositoryImpl` підставляється через Hilt.

Сильне формулювання для захисту: чиста архітектура реалізована через розділення на presentation, domain і data. Доменний шар описує моделі, use case та контракти репозиторіїв. Data-шар реалізує ці контракти через Room, DataStore, Firebase і файлову систему. Presentation-шар не знає технічних деталей зберігання. Залежності з’єднані через Hilt, тому код можна тестувати через fake-репозиторії.

## 3. Чому використовуються Room, DataStore Preferences і Firebase Firestore

У застосунку одночасно використовуються Room, DataStore Preferences, Firebase Firestore і Firebase Storage, але вони не дублюють одне одного. Кожна технологія відповідає за свій тип даних і свій режим роботи.

Room - це локальна реляційна база даних поверх SQLite. Вона підходить для структурованих даних, де є таблиці, зв’язки, запити, індекси, сортування і реактивне спостереження через `Flow`. У моєму застосунку Room зберігає сутності, які є частиною навчального процесу: табулатури, теги, сесії практики, зв’язки сесій із табулатурами, цілі, текстові нотатки та метадані аудіонотаток.

DataStore Preferences - це сховище ключ-значення. Воно не замінює базу даних, бо не призначене для складних реляційних запитів. Його роль - зберігати налаштування і невеликі snapshot-и стану. У проєкті DataStore зберігає тему, мову, вибраного AI-провайдера, URL локального llama.cpp сервера, швидкість і масштаб для режимів, режим відображення нот/табулатури, факт проходження onboarding, UID власника синхронізації, час останньої синхронізації, pending deletion для користувацьких табулатур і JSON зі збереженим прогресом відтворення.

Firestore - це хмарна документна база. Вона потрібна не для локальної роботи, а для перенесення даних між пристроями користувача. Наприклад, користувач може створити цілі або додати табулатури на одному пристрої, а потім синхронізувати їх на іншому. Firestore зберігає документи й колекції, але не зберігає великі файли оптимально, тому для самих `.gp` файлів і аудіонотаток використовується Firebase Storage. Firestore зберігає метадані та `storagePath`, а Storage зберігає байти файлу.

Локальна Room-схема включає такі основні таблиці:

```text
tabs(id, name, description, difficulty, lessonNumber, isCompleted,
     isUserTab, filePath, asciiTabs, folder, openCount,
     lastOpenedAt, createdAt, updatedAt, offlineReady)

tags(name)

tab_tag_cross_ref(tabId, tagName)

sessions(id, startTime, endTime)

practiced_tabs(id, sessionId, tabId, duration)

goals(id, syncId, type, description, target, progress, deadline, updatedAt)

text_notes(id, lessonId, content, createdAt, isFavorite)

audio_notes(id, lessonId, filePath, createdAt, isFavorite)
```

Firestore-структура організована навколо користувача:

```text
users/{uid}/settings/app
users/{uid}/tabs/{tabId}
users/{uid}/sessions/{sessionDocumentId}
users/{uid}/goals/{syncId}
users/{uid}/progress/{tabId}
users/{uid}/text_notes/{textNoteDocumentId}
users/{uid}/audio_notes/{audioNoteDocumentId}
```

Це означає, що локальна база є реляційною, а хмара - документною. При синхронізації дані мапляться між цими моделями. Наприклад, у Room теги нормалізовані в окремі таблиці, а у Firestore вони записуються в поле `tagsCsv`, бо в документній моделі невеликий список тегів зручніше тримати прямо в документі табулатури. У Room сесія і практиковані табулатури - це `sessions` і `practiced_tabs`, а у Firestore одна сесія містить масив `practicedTabs`.

Синхронізація реалізована явно в `FirestoreSyncRepositoryImpl`. Це не автоматична магія Firestore, а конкретний алгоритм. Спочатку застосунок перевіряє, чи є авторизований Firebase-користувач. Потім будується `SyncContext`: визначається, чи змінився акаунт, чи треба надавати перевагу remote-стану, і які користувацькі табулатури позначені як видалені локально. Якщо акаунт змінився, локальні cloud-scoped дані очищуються, щоб дані одного користувача не змішалися з даними іншого.

Далі застосунок читає віддалений стан з Firestore: settings, tabs, sessions, goals, progress, text_notes, audio_notes. Паралельно читаються документи з різних колекцій. Для файлів user tabs і audio notes mapper через `FirestoreSyncFileStore` намагається відновити локальний файл: спочатку з Firebase Storage за `storagePath`, а якщо це неможливо, з `fileBase64`, якщо він є.

Після читання remote-стану застосовується merge policy. Для налаштувань правило таке: якщо це перша синхронізація або перемикання акаунта, перевага надається remote-стану. В інших випадках порівнюється `updatedAt`. При цьому `hasSeenOnboarding` об’єднується через OR, бо якщо onboarding уже був пройдений хоча б на одному пристрої, його не треба показувати знову.

Для табулатур правило складніше. Якщо local і remote мають однаковий `id`, вони об’єднуються. Якщо `id` різний, але це однакова користувацька табулатура за канонічним ключем, вони теж можуть бути зіставлені. Новіше значення за `updatedAt` дає основні поля, але не всі поля просто перезаписуються. Теги об’єднуються, `openCount` бере максимум, `lastOpenedAt` бере максимум, `createdAt` бере найраніший ненульовий час, `offlineReady` об’єднується через OR. Це зроблено тому, що деякі поля не є “останній запис переміг”, а природно накопичуються.

Для цілей використовується `syncId`, а конфлікт вирішується через найбільший `updatedAt`. Для прогресу відтворення групування йде за `tabId`, і перемагає запис із найбільшим `updatedAt`. Для текстових нотаток document id будується з `lessonId` і `createdAt`, а при об’єднанні favorite-стан зберігається через OR. Для аудіонотаток також використовується document id на основі `lessonId` і `createdAt`, а файл обирається той, який реально існує локально або був відновлений з хмари.

Якщо одна й та сама сутність була змінена локально і у Firestore, поведінка залежить від типу сутності. Для `Goal` і `Progress` перемагає новіший `updatedAt`. Для `TabItem` новіша версія дає основні поля, але частина інформації об’єднується, щоб не втрачати накопичувані дані. Для тегів застосовується union. Для видалень використовується окрема логіка pending deletion, бо видалений локально файл не повинен випадково повернутися з remote-копії.

Під час роботи без Інтернету застосунок продовжує працювати з Room, DataStore і локальними файлами. Користувач може тренуватися, відкривати локально доступні табулатури, записувати сесії, змінювати цілі, додавати нотатки і прогрес. Ці зміни залишаються локально. Firestore-синхронізація виконується тоді, коли користувач авторизований і запускає синхронізацію. Після відновлення Інтернету merge policy зводить локальний і віддалений стани.

Важливо сказати чесно: у цьому проєкті немає повністю автоматичної двосторонньої realtime-синхронізації в фоні. Реалізований керований sync pipeline. Це свідомо простіше і передбачуваніше для дипломного застосунку, бо конфлікти вирішуються в одному місці - `FirestoreSyncMergePolicy`.

## 4. До якої нормальної форми приведено локальну БД

Нормалізація бази даних - це процес організації таблиць так, щоб уникати непотрібного дублювання, повторюваних груп і логічних аномалій при оновленні. Якщо база погано нормалізована, одна й та сама інформація може зберігатися в кількох місцях, і тоді при зміні одного запису легко отримати суперечливий стан.

Перша нормальна форма означає, що значення в комірках мають бути атомарними, тобто в одному полі не має бути списку значень. Наприклад, поле `tagsCsv = "rock,metal,jazz"` у таблиці `tabs` було б слабким місцем з погляду 1НФ, бо в одному атрибуті фактично лежить список. У поточній Room-моделі теги винесені в `tags` і `tab_tag_cross_ref`, тому зв’язок “табулатура має багато тегів” зберігається реляційно.

Друга нормальна форма означає, що кожен неключовий атрибут має залежати від усього первинного ключа, а не від частини ключа. Це актуально для таблиць зі складеним ключем. У моїй схемі найважливіша така таблиця - `tab_tag_cross_ref`, де первинний ключ складається з `(tabId, tagName)`. У ній немає додаткових неключових полів, тому немає ситуації, де якесь поле залежить тільки від `tabId` або тільки від `tagName`.

Третя нормальна форма означає, що неключові поля не повинні залежати від інших неключових полів. Інакше виникає транзитивна залежність. Наприклад, якби в таблиці `tabs` зберігалися `difficultyId`, `difficultyName` і `difficultyDescription`, то `difficultyDescription` залежав би не від `tab.id`, а від `difficultyId`. Це порушувало б 3НФ. У моєму проєкті такої таблиці немає: `difficulty` зберігається як enum-значення, а інші характеристики табулатури залежать від її `id`.

Таблиця `tabs` має первинний ключ `id`. Поля `name`, `description`, `difficulty`, `lessonNumber`, `isCompleted`, `isUserTab`, `filePath`, `asciiTabs`, `folder`, `openCount`, `lastOpenedAt`, `createdAt`, `updatedAt`, `offlineReady` описують конкретну табулатуру. Вони не визначають одне одного. Наприклад, `folder` не визначає `name`, `difficulty` не визначає `filePath`, `openCount` не визначає `description`. Тому неключові атрибути функціонально залежать від ключа табулатури, а не від інших неключових атрибутів.

Таблиця `tags` має ключ `name` і не має додаткових атрибутів. Вона також не порушує 3НФ. Таблиця `tab_tag_cross_ref` зберігає тільки факт зв’язку між табулатурою і тегом. Це класична нормалізована реалізація зв’язку багато-до-багатьох.

Таблиця `sessions` має ключ `id`, а `startTime` і `endTime` залежать від конкретної сесії. Практиковані табулатури не зберігаються як список у `sessions`, а винесені в `practiced_tabs`. Це важливо. Одна сесія може включати кілька табулатур, а одна табулатура може зустрічатися в багатьох сесіях. Тому окрема таблиця `practiced_tabs(sessionId, tabId, duration)` прибирає повторювані групи і дозволяє робити JOIN.

Таблиця `goals` має ключ `id`, а `syncId`, `type`, `description`, `target`, `progress`, `deadline`, `updatedAt` описують одну навчальну ціль. `syncId` має унікальний індекс для синхронізації, але це не порушує 3НФ: він не створює неключову залежність виду “одне неключове поле визначає інше”. `isCompleted` і `isOverdue` не зберігаються як окремі колонки в актуальній entity, а обчислюються в доменній моделі `Goal`. Це також зменшує ризик аномалій: якщо `progress` змінився, не треба окремо синхронізувати похідне поле `isCompleted`.

Таблиці `text_notes` і `audio_notes` мають ключ `id`. Їхні поля описують конкретну нотатку: до якого уроку вона належить, який має текст або файл, коли створена, чи позначена як favorite. Тут також немає транзитивної залежності між неключовими атрибутами.

Отже, локальну базу можна обґрунтовано вважати приведеною до 3НФ: повторювані групи винесені в окремі таблиці, багато-до-багатьох зв’язок табулатур і тегів реалізований через junction table, практиковані табулатури винесені з сесій, неключові атрибути залежать від ключів своїх таблиць, а не від інших неключових атрибутів.

## 5. Як організована взаємодія Kotlin і JavaScript у WebView

У застосунку Kotlin і JavaScript працюють у двох різних середовищах. Kotlin виконується в Android Runtime і відповідає за Compose-інтерфейс, ViewModel, життєвий цикл, Room, DataStore, Firebase, файли й Android API. JavaScript виконується всередині `WebView` і відповідає за роботу з alphaTab: завантаження Guitar Pro файлу, рендеринг табулатури, відтворення, аналіз нот і формування підказок для гри.

Ці середовища ізольовані. Kotlin не може напряму викликати локальну JS-функцію як звичайний Kotlin-метод, а JS не має прямого доступу до Kotlin-об’єктів. Тому взаємодія зроблена у двох напрямках. Kotlin викликає JavaScript через `webView.evaluateJavascript(...)`. JavaScript викликає Kotlin через об’єкт, зареєстрований методом `webView.addJavascriptInterface(bridge, "Android")`.

Основний WebView створюється у `createTabWebViewEntry`. Там налаштовується `WebViewAssetLoader`, який дає змогу завантажувати локальні assets через віртуальну HTTPS-адресу:

```text
https://appassets.androidplatform.net/assets/web/tab_viewer/index.html
```

Це краще, ніж `file://`, бо WebView отримує нормальний origin, а доступ до локальної файлової системи можна залишити вимкненим. HTML-сторінка `index.html` підключає `alphatab_local.js`, файли аналізу і `tab_viewer_bridge.js`. Саме `tab_viewer_bridge.js` є JavaScript-шаром між Kotlin і alphaTab.

Стан WebView інкапсульований у `TabWebViewEntry`. Він містить сам `WebView`, `TabJsBridge`, ознаки готовності JS, кількість завантажених тактів, ознаку завантаження SoundFont, назву вже завантаженого файлу, останній tick і останній bar index. Це потрібно тому, що WebView має власний JavaScript-контекст і власний глобальний об’єкт `window`. Якщо створити інший WebView, у нього буде інший `window` і інший стан.

Коли Kotlin хоче передати команду в JS, він формує JavaScript-рядок і виконує його:

```kotlin
webView.evaluateJavascript("window.playPause();", null)
webView.evaluateJavascript("window.setTabScale($currentScale);", null)
webView.evaluateJavascript("window.setLoopRange($start, $end, $enabled);", null)
```

У JS відповідні функції явно записані в `window`:

```javascript
window.playPause = () => {
    if (!api || _playbackBlocked) return false;
    tryResumeAudio();
    api.playPause();
    return true;
};

window.setTabScale = (s) => {
    _pendingScale = parseFloat(s);
    if (!api) return;
    api.settings.display.scale = _pendingScale;
    api.updateSettings();
    if (api.score) api.render();
};
```

Запис через `window` важливий, бо `evaluateJavascript("window.playPause();", null)` шукає функцію саме в глобальному браузерному об’єкті. Якщо функція була б локальною змінною всередині closure, Kotlin не зміг би її викликати за глобальним ім’ям.

У зворотному напрямку Kotlin створює `TabJsBridge` і реєструє його:

```kotlin
webView.addJavascriptInterface(bridge, "Android")
```

Після цього в JavaScript з’являється `window.Android`. Назва `Android` не є стандартною властивістю браузера; це просто ім’я, яке задане другим аргументом `addJavascriptInterface`. Методи, які можна викликати з JS, позначені `@JavascriptInterface`:

```kotlin
internal class TabJsBridge {
    var onTabAnalysisCallback: (String) -> Unit = {}
    var onScoreLoadedCallback: (Int) -> Unit = {}
    var onPlaybackProgressCallback: (Long, Boolean, Int) -> Unit = { _, _, _ -> }

    @JavascriptInterface
    fun postTabAnalysis(json: String) = onTabAnalysisCallback(json)

    @JavascriptInterface
    fun onScoreLoaded(totalMeasures: Int) = onScoreLoadedCallback(totalMeasures)

    @JavascriptInterface
    fun onPlaybackProgress(tick: Long, isPlaying: Boolean, barIndex: Int) =
        onPlaybackProgressCallback(tick, isPlaying, barIndex)
}
```

У JS це виглядає так:

```javascript
if (window.Android?.onScoreLoaded) {
    window.Android.onScoreLoaded(totalMeasures);
}

if (window.Android?.postTabAnalysis) {
    window.Android.postTabAnalysis(JSON.stringify(analysis));
}
```

Важлива деталь: callback-и з JavaScript не можна бездумно вважати UI-потоком. У проєкті в `BindTabViewerBridge` оновлення, які впливають на Compose-стан, обгортаються через `Handler(Looper.getMainLooper()).post { ... }`. Це гарантує, що оновлення стану екрана відбуваються в коректному потоці.

JSON використовується тому, що через bridge зручно передавати рядки, а дані аналізу мають структуру. Наприклад, `TabAnalysis` включає номер такту, список нот лівої руки, список нот правої руки, список інструкцій, можливе баре, підказку контексту і наступну позицію руки. Передавати це як багато окремих аргументів було б незручно і крихко. JavaScript робить `JSON.stringify(...)`, Kotlin приймає рядок і через Gson перетворює його в `TabAnalysis`.

```kotlin
fun setTabAnalysis(analysisJson: String) {
    runCatching {
        gson.fromJson(analysisJson, TabAnalysis::class.java)
    }.onSuccess { analysis ->
        _uiState.update { it.copy(tabAnalysis = analysis, isAnalysisLoading = false) }
    }
}
```

Ризики безпеки WebView пов’язані з тим, що JavaScript-код, який виконується на сторінці, може викликати exposed-методи `Android`. Тому небезпечно відкривати в такому WebView довільні сайти. У моєму проєкті це обмежено кількома способами. WebView завантажує локальний `index.html` через `WebViewAssetLoader`, навігація дозволена тільки на host `appassets.androidplatform.net`, file access і content access вимкнені, `allowFileAccessFromFileURLs` і `allowUniversalAccessFromFileURLs` вимкнені, mixed content заборонений, багато вікон не підтримується. Крім того, bridge не відкриває методів для читання довільних файлів, виконання команд або доступу до секретів. Він передає тільки вузький набір подій: прогрес, аналіз, статуси, кількість тактів.

Чому alphaTab у WebView, а не повністю нативна Android-бібліотека: тому що задача полягає не лише в малюванні шести ліній табулатури. Потрібно читати формати Guitar Pro, інтерпретувати треки, такти, голоси, ноти, тривалості, техніки, темп, програвати звук із SoundFont, показувати курсор і давати доступ до внутрішньої структури партитури. alphaTab уже має готовий парсер, renderer і player для цього. Повністю нативний підхід означав би писати або інтегрувати складний нотний рушій, що суттєво збільшило б ризики і обсяг проєкту.

## 6. Навіщо файл табулатури кодується в Base64 і як впливає CORS

Base64 - це спосіб представити бінарні дані текстовим рядком. Він не шифрує файл і не захищає його. Його задача - перетворити довільні байти у символи, які можна безпечно передати через текстовий канал. У проєкті це потрібно тому, що Guitar Pro файл і SoundFont - це бінарні файли, а `evaluateJavascript` приймає рядок JavaScript-коду.

Якщо табулатура є вбудованим asset-файлом, застосунок може передати alphaTab URL:

```kotlin
val assetUrl = "https://appassets.androidplatform.net/assets/$fileName"
webView.evaluateJavascript("window.loadTab('$assetUrl');", null)
```

Але якщо це користувацький файл, він лежить у внутрішньому сховищі Android або приходить через `content://`. Давати WebView прямий доступ до `file://` або `content://` небажано. Тому Kotlin читає байти через `TabFileRepository`, кодує їх у Base64, кешує і передає в JS:

```kotlin
val base64 = Base64.encodeToString(
    tabFileRepository.readTabBytes(path),
    Base64.NO_WRAP
)
webView.evaluateJavascript("window.loadTabFromBase64('$base64');", null)
```

У JavaScript Base64 повертається назад у байти:

```javascript
function base64ToUint8Array(base64) {
    const binary = atob(base64);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) {
        bytes[i] = binary.charCodeAt(i);
    }
    return bytes;
}

window.loadTabFromBase64 = (base64) => {
    const bytes = base64ToUint8Array(base64);
    api.load(bytes);
};
```

CORS - це browser security policy, яка не дозволяє сторінці з одного origin довільно читати ресурси з іншого origin. У WebView сторінка працює з origin `https://appassets.androidplatform.net`. Якби JS намагався читати локальний файл напряму, виникали б проблеми з політиками доступу і безпекою. Base64 обходить це не через “злам CORS”, а через те, що файл читає не JavaScript, а Android-код, який має контрольований доступ до файлу. Потім Android передає вже самі байти у вигляді рядка.

Альтернативи були такі. Можна було завжди використовувати URL через `WebViewAssetLoader`, але це підходить передусім для assets, а не для довільних користувацьких файлів у внутрішньому сховищі. Можна було дозволити `file://`, але це погіршує безпеку WebView. Можна було піднімати локальний HTTP-сервер усередині застосунку і віддавати файли через `localhost`, але це зайва складність і новий attack surface. Можна було використовувати `WebMessagePort` або blob URL, але байти все одно треба було б доставити в JavaScript. Для цього проєкту Base64 є простим і контрольованим рішенням.

Недолік Base64 - збільшення розміру приблизно на 33% і витрати на кодування/декодування. Тому в `TabViewerViewModel` є кеш у пам’яті й кеш на диску. Якщо та сама табулатура відкривається повторно, Base64 не обов’язково генерується заново.

## 7. Як працює евристичний алгоритм підбору аплікатури

Аплікатура - це вибір пальців лівої і правої руки для виконання нот. Для гітари це складна задача, бо одна й та сама нота може бути зіграна в різних позиціях, одна й та сама музична фраза може мати кілька зручних варіантів, а “найкращий” варіант залежить від контексту, темпу, наступних нот і фізіології виконавця.

У проєкті алгоритм працює в JavaScript після того, як alphaTab розібрала Guitar Pro файл. Спочатку `extractBeatData(track)` отримує послідовність музичних подій. Для кожної події зберігаються номер такту, тривалість, список нот, струни, лади, ознаки dead note, tapping, tie, а також техніки: bend, vibrato, slide, hammer-on, pull-off, palm mute, harmonic, trill, ghost, let ring, staccato, accent та інші.

Початкова гіпотеза для лівої руки така: рука має позицію, а палець можна приблизно визначити формулою:

```text
finger = fret - position + 1
```

Якщо позиція 5, то 5 лад відповідає першому пальцю, 6 лад - другому, 7 лад - третьому, 8 лад - четвертому. Це не жорстке правило, а стартова модель. Алгоритм генерує кілька позицій-кандидатів і для кожної оцінює, наскільки вона зручна.

Для баре є окрема логіка. Функція `detectBarreCandidates(notes)` шукає кілька нот на одному ладу на сусідніх або близьких струнах. Якщо таких нот достатньо, створюється кандидат баре: fret, fromString, toString, finger = 1, strength і coverage. Потім алгоритм може призначити перший палець на кілька струн одного ладу.

Для кожної події будуються кандидати лівої руки і кандидати правої руки. Ліва рука оцінюється за такими критеріями: чи вкладаються ноти у пальці 1-4, наскільки далеко позиція від попередньої, чи є надто широке розтягнення, чи зручно використовувати баре, чи не призначено один палець на різні лади, чи логічно пальці розташовані від нижчих ладів до вищих. На низьких ладах розтягнення штрафується сильніше, бо фізично відстань між ладами більша.

Права рука будується за класичною fingerstyle-логікою: басові струни частіше граються великим пальцем `p`, верхні струни - `i`, `m`, `a`. Для одного treble-звуку вибирається природний палець за струною. Для акорду з басом і верхніми струнами створюється pinch або fingerstyleChord. Для швидких послідовних treble-нот алгоритм заохочує чергування пальців, бо повтор одним пальцем у швидкому темпі гірший.

Після цього алгоритм не просто вибирає найкращий кандидат для кожної ноти окремо. У `optimizeBeatSequence` використовується динамічне програмування. Для кожної події є список найкращих локальних кандидатів. Потім для кожного кандидата поточної події рахується вартість переходу від кожного кандидата попередньої події. Алгоритм зберігає найкращий попередній шлях через `scores` і `backPointers`, а в кінці відновлює найкращу послідовність.

Це важливий нюанс для захисту: повної точної оптимізації всіх можливих аплікатур для всієї композиції немає, але всередині згенерованого обмеженого набору кандидатів шлях вибирається системно через dynamic programming. Тобто алгоритм евристичний на етапі генерації й оцінки кандидатів, але не просто жадібний.

Штрафи переходів враховують зміну позиції, зміну баре, втрату спільної форми, незручний рух одним пальцем між струнами, великі стрибки правої руки, повтор пальця на швидких treble-нотах. Бонуси даються за збереження форми, за утримання tie тим самим пальцем, за природне legato, за slide одним пальцем, за чергування пальців правої руки у швидкому пасажі.

Чому не було застосовано точний алгоритм оптимізації: для реальної композиції кількість можливих аплікатур росте дуже швидко. Якщо для кожної події є десятки варіантів, то для сотень подій повний перебір стає непридатним. Крім того, “правильна аплікатура” не має єдиного математичного критерію. Два гітаристи можуть вибрати різні варіанти, і обидва будуть правильними. Тому практичніше застосувати евристику, яка швидко дає добру рекомендацію і враховує музичний контекст.

Коректність перевірялась вручну на типових випадках: відкриті струни, прості рифи, акорди, баре, широкі розтягнення, legato, slide, palm mute, tapping, гармоніки, швидкі послідовності. Також перевірялось, що техніки з alphaTab правильно переносяться в підказки. Важливо формулювати чесно: алгоритм є навчальною рекомендацією, а не абсолютним експертним стандартом.

## 8. Як із alphaTab отримуються такти, долі, ноти, тривалості та техніки

Після завантаження файлу alphaTab створює об’єкт `score`. У ньому музика має внутрішню ієрархічну структуру. На верхньому рівні є `tracks`. У треку є `staves`. У stave є `bars`. У bar є `voices`. У voice є `beats`. У beat є `notes`. Саме цю структуру обходить мій JavaScript-аналіз.

Основна функція - `extractBeatData(track)`. Вона проходить по першому stave активного треку:

```javascript
for (const bar of track.staves[0].bars) {
    for (const voice of bar.voices) {
        if (voice.isEmpty) continue;
        for (const beat of voice.beats) {
            const notes = beat.notes.filter(note => note.fret != null);
            // побудова події
        }
    }
}
```

Номер такту береться з `bar.index`. Для користувача він показується як `barIdx + 1`, бо внутрішня індексація починається з нуля. Позиція долі в часі береться через `beat.absoluteDisplayStart` або `beat.start`. Для відтворення й синхронізації з курсором використовується tick. Tick - це внутрішня одиниця часу в alphaTab. Вона точніша за номер такту, бо всередині такту може бути багато долей і нот.

Тривалість beat читається з `beat.duration`. У `tab_viewer_i18n.js` є таблиця:

```javascript
const DUR_BEATS = {1:4, 2:2, 4:1, 8:.5, 16:.25, 32:.125, 64:.0625};
```

Це означає: ціла нота - 4 долі, половинна - 2, четвертна - 1, восьма - 0.5 і так далі. Так алгоритм отримує відносну тривалість музичної події.

Для кожної ноти читаються `string`, `fret`, `isDead`, `isTapped`, `isLeftHandTapped`, `isTieDestination`. Окрема функція `normalizeStringIndex` приводить індекс струни до зручного гітарного представлення. Це потрібно тому, що внутрішній порядок струн у бібліотеці може відрізнятися від того, як гітарист звик бачити струни.

Техніки беруться з двох рівнів: з ноти і з beat. На рівні ноти читаються bend, vibrato, slide in/out, hammer/pull origin/destination, palm mute, harmonic, trill, ghost, dead, let ring, staccato, accent. На рівні beat читаються fade in, rasgueado, tremolo picking, grace note, whammy bar, beat vibrato, brush up/down, arpeggio up/down, pick stroke up/down, crescendo/decrescendo, slap і pop.

Потім ці сирі прапорці перетворюються на зрозумілі інструкції. Наприклад, якщо нота має `isPalmMute`, у підказках з’являється palm mute instruction. Якщо є `slideInType` або `slideOutType`, це перетворюється на slide. Якщо є `isHammerPullOrigin` або `isHammerPullDestination`, алгоритм визначає legato і на основі напрямку руху може інтерпретувати hammer-on або pull-off.

Результат аналізу для поточного моменту передається в Kotlin як JSON:

```json
{
  "barIndex": 3,
  "leftHand": [
    {"finger":"1","string":"B","fret":"5","isSlide":true}
  ],
  "rightHand": [
    {"finger":"m","string":"B"}
  ],
  "instructions": ["..."],
  "barreFret": null,
  "contextHint": "slide"
}
```

Kotlin отримує цей JSON через `postTabAnalysis`, парсить у `TabAnalysis` і передає в `GuitarFretboard`, який уже малює підказки на грифі.

## 9. Як працює AI-помічник

AI-помічник працює не з бінарним `.gp` файлом напряму, а з текстовим описом табулатури. Це важливо, бо мовна модель не є Guitar Pro парсером. Якщо просто передати їй байти файлу, вона не зможе надійно зрозуміти структуру тактів, нот і технік. Тому спочатку alphaTab розбирає файл у WebView, а потім JavaScript формує компактний текстовий опис.

У `tab_analysis_runtime.js` функція `buildCompactTabs(track)` проходить по подіях і формує текст на кшталт:

```text
Measure 1:
  Event 1 inside this measure: string 6 fret 0
  Event 2 inside this measure: string 5 fret 2 [palm-mute]
Measure 2:
  Event 1 inside this measure: string 3 fret 4 [slide]
```

Це стислий формат, але він містить головне: номер такту, порядок подій, струни, лади і технічні прапорці. Він значно корисніший для AI, ніж raw binary.

У `AiAssistantScreen` формується контекст. Якщо JS-аналіз уже готовий, використовується `compactTabs`. Якщо користувач вибрав конкретний діапазон тактів, функція `sliceCompactTabs` вирізає тільки потрібні `Measure N`. Якщо користувач увімкнув full context, у prompt додається вся compact-табулатура і raw ASCII tab. Якщо compact-аналіз недоступний, використовується fallback ASCII з уроку.

```kotlin
private fun buildAiTabsContext(
    asciiTab: String?,
    fallbackAscii: String?,
    compactTabs: String?,
    selectedRange: IntRange?,
    isFullContext: Boolean
): String
```

Обмеження контексту реалізоване через вибір тактів. За замовчуванням AI не отримує всю композицію, а отримує тільки поточний або вибраний фрагмент. Це важливо для великих композицій, бо мовні моделі мають обмежений context window, а великі табулатури можуть займати багато тексту. Крім того, локальний llama.cpp server може працювати з меншим контекстом, ніж Gemini.

`AiAssistantPromptBuilder` збирає фінальний prompt із ролі, інструкцій, теорії уроку, табулатурного контексту і питання користувача. Потім `AiAssistantRepositoryImpl` дивиться в DataStore, який провайдер вибраний: Gemini або локальний llama.cpp.

Gemini-запит проходить через Cloudflare Worker. Це зроблено, щоб не зберігати Gemini API key у мобільному застосунку. Якщо API-ключ покласти в APK, його можна витягнути після декомпіляції або перехопити. Worker зберігає ключ як server-side secret і приймає від застосунку запит без ключа.

Worker не просто проксі. Він перевіряє метод HTTP, перевіряє наявність `GEMINI_API_KEY`, читає Bearer token, перевіряє Firebase ID token через Google JWKS, перевіряє project id, перевіряє JSON body, нормалізує prompt, дозволяє тільки моделі з allowlist і тільки потім викликає Gemini API.

Однак важливо не казати, що Worker робить систему повністю безпечною. Він захищає API key від прямого витягування з мобільного застосунку, але авторизований користувач усе одно може надсилати запити до Worker. Для production-захисту потрібні rate limits, квоти, логування, App Check, abuse detection і, можливо, обмеження розміру prompt. Тому правильне формулювання: Cloudflare Worker є значно безпечнішим способом зберігання API-ключа, ніж ключ у APK, але це не абсолютний захист від зловживань.

## 10. Як реалізоване llama.cpp і перемикання між ним та Gemini

У застосунку є enum:

```kotlin
enum class AiProvider {
    GEMINI,
    LOCAL_LLAMA_CPP
}
```

Вибір провайдера зберігається в DataStore як частина `AppSettingsSnapshot`. У налаштуваннях користувач може вибрати Gemini або локальну модель через llama.cpp server. Якщо вибрано `GEMINI`, `AiAssistantRepositoryImpl` будує prompt і відправляє його на Cloudflare Worker. Якщо вибрано `LOCAL_LLAMA_CPP`, репозиторій бере `localAiServerUrl` і відправляє POST-запит на:

```text
{baseUrl}/v1/chat/completions
```

Це OpenAI-compatible endpoint, який підтримує `llama-server`. Тіло запиту містить `model`, `stream: false`, `max_tokens` і `messages`. Відповідь парситься з `choices[0].message.content`, або з fallback-полів `text`, `content`, `response`.

Локальна модель не запускається всередині Android-застосунку. У проєкті немає GGUF-моделі в APK, немає JNI-інтеграції llama.cpp і немає локального inference на телефоні. Android-застосунок є клієнтом до вже запущеного llama.cpp server. Цей сервер може працювати на комп’ютері в тій самій локальній мережі або теоретично на самому Android-пристрої через Termux, але застосунок його не запускає сам.

Саме тому в налаштуваннях користувачу пояснюється команда:

```text
./llama-server -m /path/to/model.gguf --host 0.0.0.0 --port 8080
```

Параметр `--host 0.0.0.0` потрібен, щоб сервер приймав підключення з інших пристроїв у мережі. У застосунку треба ввести адресу типу:

```text
http://192.168.1.15:8080
```

Код спеціально відхиляє `localhost`, `127.0.0.1` і `0.0.0.0` як адресу для мобільного клієнта. Причина проста: `localhost` на телефоні означає сам телефон, а не комп’ютер, на якому запущений llama.cpp. Якщо сервер запущений на ПК, телефон повинен звертатися до локальної IP-адреси ПК.

## 11. Як реалізовані інші інструменти для навчання

Застосунок має не лише перегляд табулатури, а набір навчальних інструментів навколо неї. Перший інструмент - режими нормальної гри і практики. У нормальному режимі користувач може слухати й переглядати табулатуру. У режимі практики акцент переноситься на навчання: швидкість нижча, масштаб може бути іншим, а під табулатурою показується гриф із підказками пальців.

Швидкість і масштаб зберігаються окремо для normal і practice режимів у DataStore. Це зроблено тому, що користувач може хотіти слухати композицію на 100%, але тренувати фрагмент на 30% зі збільшеним масштабом.

Другий інструмент - loop. Користувач вибирає початковий і кінцевий такт, а Kotlin викликає:

```javascript
window.setLoopRange(startMeasure, endMeasure, isLooping)
```

У JavaScript ця функція знаходить `masterBars[startMeasure - 1]` і `masterBars[endMeasure - 1]`, визначає `startTick` і `endTick`, а потім встановлює `api.playbackRange`. Тобто loop працює не приблизно по часу, а по tick-діапазону alphaTab.

Третій інструмент - поступове прискорення loop. Після кожного повтору JS визначає, що відтворення повернулося з кінця діапазону на початок, і викликає `Android.onLoopIterationCompleted()`. Kotlin збільшує швидкість на заданий крок після заданої кількості повторів, доки не досягне кінцевої швидкості.

Четвертий інструмент - метроном. Він реалізований у JavaScript через Web Audio API. Темп за замовчуванням визначається зі score через alphaTab, але користувач може змінити BPM. Це зручно, бо метроном живе в тому ж середовищі, де працює відтворення табулатури.

П’ятий інструмент - вибір треку і транспонування. JavaScript читає `api.score.tracks`, повертає список треків у Kotlin, а при виборі викликається `window.applyLearningTools(trackIndex, transposeSemitones)`. Усередині змінюються `api.settings.notation.transpositionPitches`, виконується `api.renderTracks([track])`, після чого аналіз перебудовується для нового треку.

Шостий інструмент - нотатки. Текстові нотатки зберігаються в Room у `text_notes`. Аудіонотатки записуються або імпортуються як файли у внутрішнє сховище, а їхні метадані зберігаються в `audio_notes`. Для запису використовується `MediaRecorder`, для відтворення - `MediaPlayer`, а `AudioNoteMediaController` об’єднує імпорт, запис, play/pause і seek.

Сьомий інструмент - сесії практики. Користувач запускає практику, `MainViewModel` починає таймер, а при переході між табулатурами фіксується час по кожній. Після завершення створюється `Session` із `PracticedTab`. Це дозволяє показувати загальний час практики, історію сесій і heatmap активності.

Восьмий інструмент - цілі. `Goal` може бути пов’язана з кількістю завершених уроків, часом практики або бути custom-ціллю. `ObserveGoalsProgressUseCase` об’єднує цілі, кількість завершених уроків і сесії практики, після чого рахує актуальний прогрес.

Дев’ятий інструмент - “продовжити навчання”. `ObserveContinueLearningUseCase` дивиться на останню відкриту табулатуру і прогрес відтворення. На головному екрані користувач бачить останній матеріал і може повернутися до нього.

## 12. Які частини покриті JUnit-тестами і що тестувалось вручну

JUnit і Robolectric тести покривають насамперед логіку, яку можна перевірити без реального WebView, Firebase і Android-пристрою. Це правильний розподіл, бо unit-тести мають бути швидкими, стабільними й не залежати від мережі.

`MainViewModelTest` перевіряє сценарій практичної сесії. Тест запускає сесію, встановлює активну табулатуру `tab-a`, просуває тестовий час, перемикається на `tab-b`, знову просуває час і завершує сесію. Далі перевіряється, що в репозиторій збережено одну сесію з двома practiced tabs і правильними тривалостями.

`PracticeHeatmapViewModelTest` перевіряє групування сесій за днем. У тесті створюються дві сесії за сьогодні і одна за вчора. Потім перевіряється, що сьогоднішні сесії згруповані в один день, а короткі сесії округлюються до хвилин так, щоб навіть коротка практика була видимою в heatmap.

`TabListViewModelTest` перевіряє дві речі. Перша - перейменування папки користувацьких табулатур: якщо дві табулатури були в папці `Rock`, після `renameFolder("Rock", "Favorites")` обидві мають перейти в `Favorites`, а активний фільтр теж оновлюється. Друга - захист від тимчасового порожнього emission прогресу: якщо DataStore на мить віддає порожній список, UI не повинен миготіти нульовим прогресом.

`TabViewerViewModelTest` перевіряє збереження прогресу відтворення. Важливий сценарій - користувач дійшов до 10-го такту, потім перемотав назад на 3-й. Прогрес має перезаписатися нижчим поточним значенням, інакше “продовжити навчання” завжди повертало б у кінець.

`TabLoadMetricsTrackerTest` перевіряє вимірювання продуктивності. Через `ShadowSystemClock` тест штучно просуває час між етапами: tap, load requested, score loaded, fully visible. Потім перевіряються `tapToRequestMs`, `requestToScoreLoadedMs`, `tapToScoreLoadedMs`, `tapToFullyVisibleMs`. Другий тест перевіряє, що сигнали від іншої табулатури не псують активне вимірювання.

`TabPlaybackProgressRepositoryImplTest` перевіряє DataStore-репозиторій прогресу: `upsert`, `getByTabId`, `replaceAll`, видалення старих записів при replace. Для тесту створюється тимчасовий Preferences-файл.

`TabCatalogMapperTest` перевіряє мапінг уроків у `TabItem`, визначення складності, lesson number, локалізацію описів, нормалізацію тегів і fallback-ім’я файлу.

У тестах підмінялися залежності через fake-реалізації: `FakeSessionRepository`, `FakeTabRepository`, `FakeTabPlaybackProgressRepository`, `FakeTabFileRepository`, `FakeSoundFontRepository`. Dispatcher-и підмінялися через `MainDispatcherRule`, щоб корутини і час були керованими.

Ручного тестування потребували частини, які залежать від реального середовища: WebView і alphaTab, рендеринг табулатури, звук і SoundFont, курсор відтворення, JS bridge, Firebase Auth, реальна Firestore/Storage синхронізація, Cloudflare Worker, Gemini, локальний llama.cpp server, запис з мікрофона і візуальна коректність підказок на грифі.

## 13. Як вимірювався час до повного відображення табулатури

Час до повного відображення вимірюється через `TabLoadMetricsTracker`. Це окремий об’єкт, який фіксує ключові моменти завантаження табулатури. Важливо, що вимірюється не тільки час парсингу файлу, а повний користувацький шлях від натискання до видимого результату.

Перший етап - користувач натискає на табулатуру в списку. У цей момент викликається:

```kotlin
TabLoadMetricsTracker.start(tab.id, tab.name)
```

Це фіксує `tapStartedAtMs` через `SystemClock.elapsedRealtime()`. `elapsedRealtime` краще для таких вимірювань, ніж `System.currentTimeMillis()`, бо не залежить від зміни системного часу.

Другий етап - `TabViewer` реально відправляє команду завантаження в WebView. Якщо це вбудований asset, джерело буде `asset-url`. Якщо це користувацький локальний файл, джерело буде `base64`.

```kotlin
TabLoadMetricsTracker.markLoadRequested(tabId, "asset-url")
TabLoadMetricsTracker.markLoadRequested(tabId, "base64")
```

Третій етап - alphaTab завершила рендеринг. JavaScript після `renderFinished` планує `onScoreLoaded(totalMeasures)`, Kotlin отримує цю подію через bridge і викликає:

```kotlin
TabLoadMetricsTracker.markScoreLoaded(tabId, source)
```

Четвертий етап - Compose фактично розблокував показ контенту. У `TabViewerScreen` є невелика затримка, після якої `isDisplayUnlocked` стає true. Саме тоді викликається:

```kotlin
TabLoadMetricsTracker.markFullyVisible(lessonId)
```

У результаті рахуються такі метрики:

```text
tapToRequestMs        - від натискання до запиту завантаження
requestToScoreLoadedMs - від запиту в WebView до scoreLoaded
tapToScoreLoadedMs    - від натискання до готовності score
tapToFullyVisibleMs   - від натискання до повної видимості для користувача
```

Найважливіша метрика - `tapToFullyVisibleMs`, бо вона відповідає реальному відчуттю користувача: скільки часу минуло від натискання до моменту, коли табулатура стала видимою і готовою для роботи.

Для детального аналізу продуктивності в коді є debug-прапори `ENABLE_TAB_PERF_TRACE`. Коли вони ввімкнені, можна логувати готовність JS, джерело завантаження, cache hit/miss для Base64, час `scoreLoaded`, frame drops і layout shifts. У звичайному режимі ці логи вимкнені, щоб не засмічувати Logcat і не впливати на поведінку застосунку.

