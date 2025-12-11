import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import java.io.FileInputStream

// No se necesitan cambios aquí al principio
val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("key.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

// En el bloque de plugins, ya no se especifican las versiones,
// porque se gestionan en el build.gradle.kts a nivel de proyecto.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android") // El nombre del plugin de Kotlin cambió
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.compose")
    id("kotlin-parcelize")
}

android {
    namespace = "com.alainmtz.work_group_tasks"
    compileSdk = 36
    ndkVersion = "28.2.13676358"

    defaultConfig {
        applicationId = "com.alainmtz.work_group_tasks"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties["keyAlias"] as String?
            keyPassword = keystoreProperties["keyPassword"] as String?
            storeFile = if (keystoreProperties["storeFile"] != null) {
                rootProject.file(keystoreProperties["storeFile"] as String)
            } else {
                null
            }
            storePassword = keystoreProperties["storePassword"] as String?
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false // Es recomendable añadir esto explícitamente
            signingConfig = signingConfigs.getByName("release")
            // proguardFiles(...) // Puedes añadir reglas de ProGuard aquí si lo necesitas
        }
        getByName("debug") {
            // applicationIdSuffix = ".debug" // Removed to match google-services.json
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlin{
        compilerOptions {
            jvmTarget = JvmTarget.fromTarget("17")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        // Asegúrate de que esta versión es compatible con tu versión de Kotlin
        // kotlinCompilerExtensionVersion = "1.5.13" // Actualizada para Kotlin 1.9.23
    }

    // buildToolsVersion ya no es necesario, se elimina.
    // El AGP usa una versión adecuada automáticamente.
}

dependencies {
    // Las dependencias se mantienen igual, pero asegúrate de que sean las últimas versiones estables.

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation(platform("com.google.firebase:firebase-bom:34.6.0")) // Versión actualizada
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-functions")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation("com.google.firebase:firebase-appcheck-debug")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("com.google.android.gms:play-services-auth:21.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2") // Versión actualizada
    implementation("androidx.datastore:datastore-preferences:1.2.0")

    // Jetpack Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.11.00") // Versión actualizada
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.12.0") // Versión actualizada
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0") // Versión actualizada
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0") // Versión actualizada
    implementation("androidx.navigation:navigation-compose:2.9.6")
    implementation("io.github.rroohit:ImageCropView:3.1.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    
    // Unit testing dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    
    // Android testing dependencies
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
