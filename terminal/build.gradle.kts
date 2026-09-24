plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.terminalhouse.terminal"
    // Alineado con la app (motor original del encargo: compileSdk 35; se sube a 36.1
    // para compilar con el mismo AGP/plataforma que el módulo :app de Gemini).
    compileSdk { version = release(36) { minorApiLevel = 1 } }
    ndkVersion = "26.1.10909125"

    defaultConfig {
        minSdk = 24
        externalNativeBuild {
            cmake {
                cFlags += listOf("-DHAVE_FORKPTY=1")
                abiFilters += listOf("arm64-v8a", "armeabi-v7a")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/jni/CMakeLists.txt")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.7.1")
}
