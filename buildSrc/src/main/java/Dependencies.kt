object Versions {
    const val minSDK = 23
    const val targetSDK = 31
    const val compileSDK = 31
    const val kotlin = "1.6.10"
    const val kotlinCoroutines = "1.5.0"
    const val compose = "1.2.0-alpha02"
    const val activity = "1.4.0"
    const val serialization = "1.2.2"
    const val ktor = "1.6.1"
    const val readium = "develop-SNAPSHOT"
}

object Dependencies {
    const val serialization = "org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.serialization}"

    object Activity {
        const val core = "androidx.activity:activity:${Versions.activity}"
        const val compose = "androidx.activity:activity-compose:${Versions.activity}"
        const val ktx = "androidx.activity:activity-ktx:${Versions.activity}"
    }

    object Compose {
        const val layout = "androidx.compose.foundation:foundation-layout:${Versions.compose}"
        const val material = "androidx.compose.material:material:${Versions.compose}"
        const val icons = "androidx.compose.material:material-icons-extended:${Versions.compose}"
        const val tooling = "androidx.compose.ui:ui-tooling:${Versions.compose}"
        const val compiler = "androidx.compose.compiler:compiler:${Versions.compose}"
    }

    object Coroutines {
        const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinCoroutines}"
        const val js = "org.jetbrains.kotlinx:kotlinx-coroutines-core-js:${Versions.kotlinCoroutines}"
    }

    object Ktor {
        const val server = "io.ktor:ktor-server-core:${Versions.ktor}"
        const val netty = "io.ktor:ktor-server-netty:${Versions.ktor}"
        const val serverSerialization = "io.ktor:ktor-serialization:${Versions.ktor}"
        const val serverAuth = "io.ktor:ktor-auth:${Versions.ktor}"
        const val serverAuthJWT = "io.ktor:ktor-auth-jwt:${Versions.ktor}"
        const val client = "io.ktor:ktor-client-core:${Versions.ktor}"
        const val clientSerialization = "io.ktor:ktor-client-serialization:${Versions.ktor}"
        const val clientAuth = "io.ktor:ktor-client-auth:${Versions.ktor}"
        const val websockets = "io.ktor:ktor-websockets:${Versions.ktor}"
        const val clientWebsockets = "io.ktor:ktor-client-websockets:${Versions.ktor}"
    }

    object Readium {
        const val shared = "com.github.readium.kotlin-toolkit:readium-shared:${Versions.readium}"
        const val streamer = "com.github.readium.kotlin-toolkit:readium-streamer:${Versions.readium}"
        const val navigator = "com.github.readium.kotlin-toolkit:readium-navigator:${Versions.readium}"
        const val opds = "com.github.readium.kotlin-toolkit:readium-opds:${Versions.readium}"
        const val lcp = "com.github.readium.kotlin-toolkit:readium-lcp:${Versions.readium}"
    }
}