# MCTrack SDK

Java SDK for sending session, payment, profile, and custom events to [MCTrack](https://mctrack.net).

Used by the official MCTrack Spigot/Paper, BungeeCord, and Velocity plugins, and available for any Java application that wants to forward analytics events to the MCTrack ingestion service.

## Requirements

- Java 17+

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("net.mctrack:mctrack-sdk:1.0.0")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'net.mctrack:mctrack-sdk:1.0.0'
}
```

### Maven

```xml
<dependency>
    <groupId>net.mctrack</groupId>
    <artifactId>mctrack-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick start

```java
import com.mctrack.common.api.MCTrackAPI;
import com.mctrack.common.config.MCTrackConfig;

MCTrackConfig config = new MCTrackConfig();
config.setApiUrl("https://ingest.mctrack.net");
config.setApiKey("your-api-key");
config.setNetworkId("your-network-uuid");
config.setServerName("lobby");

MCTrackAPI api = new MCTrackAPI(config, System.out::println);
api.start();

// Track a custom event
Map<String, Object> properties = new HashMap<>();
properties.put("quest_name", "daily_crate");
properties.put("reward_tier", "vip");
api.trackEvent("reward_claimed", playerUuid, properties);

// Set player profile properties
api.setPlayerProperty(playerUuid, "rank", "vip");

// Shut down cleanly to flush queued events
api.stop();
```

## What's in the SDK

- `MCTrackAPI` — HTTP client + event batching with retry/backoff
- `MCTrackConfig` — config loader (YAML or programmatic)
- Event models — session, heartbeat, server switch, payment, profile update, custom event
- `SessionManager` — in-memory session state for plugin authors
- `BedrockDetection`, `PlayerActivityLogger`, `UpdateChecker` — utilities used by the bundled MCTrack plugins

See the [MCTrack plugin docs](https://github.com/EssentrixLtd/mctrack/blob/master/docs/PLUGIN_DEVELOPMENT.md) for the full API reference and event payload formats.

## Building from source

```bash
./gradlew build
```

## License

Apache License 2.0. See [LICENSE](LICENSE).

---

An [Essentrix Ltd](https://essentrix.ltd) product.
