# Wear Paging Mediator

A Wear OS app for searching GitHub repositories, powered by Paging 3 and built with Clean Architecture and Jetpack Compose.

## Features

- 🔍 Search GitHub repositories by keyword via voice or text input
- 📄 Paginated results with dynamic star-based separators
- 💾 Offline support — cached data is available without network connection
- ⌚ Wear OS native UI with rotary scroll and round display support

## Architecture

- Network + Cache — Paging 3 Remote Mediator fetches data from GitHub API and persists it to Room database
- Offline-first — users retain access to previously loaded results when offline
- UI — TransformingLazyColumn with paging items, separators grouped by star count, and load state footer

The Remote Mediator refreshes the query in the background while the Room PagingSource emits the cached results right away, so the screen opens on the cache instead of the network.

## Screenshots

<img src="img/sample.png" alt="drawing" width="180"/>

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
