# Wear Paging Mediator

A Wear OS app for searching GitHub repositories, powered by Paging 3 and built with Clean Architecture and Jetpack Compose.

## Features

- 🔍 Search GitHub repositories by keyword via voice or text input
- 📄 Paginated results with dynamic star-based separators
- 🏷️ Repository details dialog with full description, stats, and clickable topics for instant topic-based search
- 🕘 Recent searches — long-press the search button to reopen one of the last 20 queries, with the moment each one was used
- 💾 Offline support — cached data is available without network connection
- ⌚ Wear OS native UI with rotary scroll and round display support

## Architecture

The project follows **Clean Architecture** across four Gradle modules with unidirectional data flow (**MVI**) in the presentation layer and an **offline-first Paging 3** pipeline in the data layer.

### Modules

- **`:app`** — Application setup, Hilt DI graph, and `MainActivity` entry point.
- **`:domain`** — Core models (`Repo`, `SearchQuery`, `RecentQuery`), `Repository` interface, and use cases (`GetReposFlowUseCase`, `GetRepoTopicsUseCase`, `GetRecentQueriesUseCase`).
- **`:data`** — `ReposRepository`, Retrofit `GitHubApi`, Room `GitHubDatabase`, mappers, and `ReposRemoteMediator`.
- **`:presentation`** — MVI state holder (`MainViewModel`), pure reducer (`MainReducer`), UI models/intents/effects, and Wear OS Compose screens.

### Offline-First Data & Paging Pipeline

- **Single source of truth** — Room's `PagingSource` emits cached results immediately when a query is opened, while `ReposRemoteMediator` fetches fresh pages from the GitHub Search API in the background.
- **Transactional caching** — Each page load atomically updates repositories, per-query result ordering (`SearchQueryResultEntity`), pagination keys (`RepoRemoteKeyEntity`), and repository-topic relations (`RepoTopicCrossRef`) in Room.
- **Bounded query cache** — Queries are normalized via `SearchQuery` and cached per query in Room. `RecentQueriesCache` keeps the results of the `RecentQuery.MAX_CACHED_QUERIES` most recently *opened* queries (opening counts, not a successful refresh, so offline use is accounted for) and deletes the rows of the rest together with the repositories and topics no other query references. `MainViewModel` additionally keeps an in-memory LRU of three `Pager` flows — fewer than the Room limit on purpose, so a query that is still shown is never evicted from the database.
- **Schema migrations** — Room exports the schema of every version to `data/schemas`; `GitHubDatabaseMigrationTest` runs the real migrations on those exported schemas and validates the result against the current one.
- **Offline resilience** — Previously loaded pages and repository topics remain readable from Room without a network connection.

### Presentation (MVI + Wear Compose)

- **Unidirectional flow** — UI events are sent as `MainIntent`s to `MainViewModel`, processed by the pure `MainReducer` into a new `MainState` (with active query persistence via `SavedStateHandle`) and one-off `MainEffect`s (e.g., refreshing results or opening a link).
- **Wear OS UI** — Built with `TransformingLazyColumn`, dynamic star-count separators (`StarBand`), contextual load-state notices/footers, and `TopicsDialog` that loads repository topics on demand from Room when a card is tapped. A long press on the search button opens `RecentQueriesDialog`, which reads the most recently opened queries with the moment each one was used and searches for the tapped one.

## Screenshots

<img src="img/sample.png" alt="drawing" width="540"/>

1. **Splash screen** — Launch screen displaying the app icon on a black background before the main screen opens.
2. **Main screen** — Repository list loaded for the default `android` query, with the search button at the top followed by repository cards showing the repository name, summary, language, and abbreviated star/fork counts.
3. **Recent searches** — Opened by long-pressing the search button; lists up to the last 20 used queries along with the timestamp of each search. Tapping any entry runs that search.
4. **Search input** — Opened by tapping the search button; launches the Wear OS input panel for entering a new search query via voice or keyboard.
5. **Scrolled repository list** — The main screen scrolled down through paginated results in `TransformingLazyColumn`, with dynamic star-count separators between repository cards.
6. **Repository details & topics** — Opened by tapping a repository card; shows the full repository name, complete description, exact star and fork counts, and clickable topics for launching a topic search. Long-pressing a repository card opens its GitHub page in a browser if one is available on the device.

## Dependencies

* [Kotlin](https://github.com/JetBrains/kotlin) 2.4.21
* [Jetpack Compose for Wear OS](https://developer.android.com/jetpack/androidx/releases/wear-compose) 1.7.1
* [Hilt](https://github.com/google/dagger) 2.60.1
* [Coroutines](https://github.com/Kotlin/kotlinx.coroutines) 1.11.0
* [Paging](https://developer.android.com/topic/libraries/architecture/paging/v3-overview) 3.5.3
* [Retrofit](https://github.com/square/retrofit) 3.0.0
* [Moshi](https://github.com/square/moshi) 1.15.2
* [Room](https://developer.android.com/topic/libraries/architecture/room) 2.8.5

## License

[MIT](LICENSE) © [alxiw](https://github.com/alxiw)
