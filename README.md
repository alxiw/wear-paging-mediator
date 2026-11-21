# Wear Paging Mediator

A Wear OS app for searching GitHub repositories, powered by [Paging 3](https://developer.android.com/topic/libraries/architecture/paging/v3-overview) and built with Clean Architecture and Jetpack Compose.

## Overview

- 🔍 **Search & history** — query GitHub repositories via voice or text input, or long-press the search button to reopen one of the last 20 queries
- 📄 **Paginated results** — infinite scrolling with star-band separators between cards
- 🏷️ **Repository details** — tap a card for the full description, exact stats, and clickable topics that start a new search, or long-press to open it in a browser
- 💾 **Offline support** — cached queries and results remain available without a network connection
- ⌚ **Wear OS native UI** — built for round displays with rotary scroll support

<img src="img/sample.png" alt="Wear Paging Mediator overview" width="540"/>

1. **Splash screen** — the app icon on a black background before the main screen opens.
2. **Main screen** — the default `android` query with the search button and repository cards (name, summary, language, and abbreviated star/fork counts).
3. **Recent searches** — up to the last 20 queries with the moment each one was used; tapping an entry runs that search.
4. **Search input** — the Wear OS input panel for a typed or dictated query.
5. **Scrolled repository list** — paginated results divided by star-band separators.
6. **Repository details & topics** — the full description, exact star and fork counts, and topics that start a new search.

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
- **Bounded query cache** — Queries are normalized via `SearchQuery` and cached per query in Room. `RecentQueriesCache` keeps the results of the `RecentQuery.MAX_CACHED_QUERIES` most recently *opened* queries and deletes the rows of the rest, together with the repositories and topics no other query references. A query is opened when its `Pager` is created, not when GitHub answers, so an offline opening counts. `MainViewModel` keeps an in-memory LRU of three `Pager` flows — fewer than the Room limit on purpose, so a window that is still shown is never evicted from the database; returning to one of them shows the kept window without a new `Pager`, and the dialog keeps the query where it was.
- **Schema migrations** — Room exports the schema of every version to `data/schemas`; `GitHubDatabaseMigrationTest` runs the real migrations on those exported schemas and validates the result against the current one.

### Presentation (MVI + Wear Compose)

- **Unidirectional flow** — UI events are sent as `MainIntent`s to `MainViewModel`, processed by the pure `MainReducer` into a new `MainState` (the active query is persisted via `SavedStateHandle`) and one-off `MainEffect`s (e.g., refreshing results or opening a link).
- **Wear OS UI** — `TransformingLazyColumn` with star-band separators (`StarBand`), load states shown next to the items, and dialogs that read Room on demand: the repository topics on a tap and the recent queries on a long press of the search button.

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
