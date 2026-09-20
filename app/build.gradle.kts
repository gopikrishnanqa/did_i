import java.io.File
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val keystorePropertiesFile = rootProject.file("credentials/keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

fun installedNdkVersion(): String? {
    val sdkDir = localProperties.getProperty("sdk.dir")
        ?: System.getenv("ANDROID_SDK_ROOT")
        ?: System.getenv("ANDROID_HOME")
        ?: return null
    val ndkRoot = File(sdkDir, "ndk")
    if (!ndkRoot.isDirectory) return null
    return ndkRoot.listFiles()
        ?.filter { it.isDirectory && File(it, "source.properties").isFile }
        ?.map { it.name }
        ?.maxOrNull()
}

android {
    namespace = "com.druanlabs.didicheck"
    compileSdk = 36
    installedNdkVersion()?.let { ndkVersion = it }

    defaultConfig {
        applicationId = "com.druanlabs.didicheck"
        minSdk = 26
        targetSdk = 36
        versionCode = 15
        versionName = "1.0.14"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

val releaseNativeLibsDir = layout.buildDirectory.dir(
    "intermediates/merged_native_libs/release/mergeReleaseNativeLibs/out/lib"
)
val releaseNativeSymbolTablesDir = layout.buildDirectory.dir(
    "intermediates/native_symbol_tables/release/extractReleaseNativeSymbolTables/out"
)

tasks.configureEach {
    if (name == "extractReleaseNativeSymbolTables") {
        doLast {
            val src = releaseNativeLibsDir.get().asFile
            val dest = releaseNativeSymbolTablesDir.get().asFile
            if (src.isDirectory) {
                dest.mkdirs()
                src.copyRecursively(dest, overwrite = true)
            }
        }
    }
}

val packageReleaseNativeDebugSymbols by tasks.registering(Zip::class) {
    group = "build"
    description = "Zip native libraries for Play Console debug-symbol upload"
    dependsOn("mergeReleaseNativeLibs")
    from(releaseNativeLibsDir)
    archiveFileName.set("native-debug-symbols.zip")
    destinationDirectory.set(layout.buildDirectory.dir("outputs/native-debug-symbols/release"))
}

fun injectNativeDebugSymbolsIntoBundle(aab: File, libsDir: File) {
    val prefix = "BUNDLE-METADATA/com.android.tools.build.debugsymbols/"
    val tmp = File(aab.parentFile, "${aab.name}.symbols-tmp")
    if (tmp.exists() && !tmp.delete()) {
        throw GradleException("Could not delete $tmp")
    }
    ZipFile(aab).use { zin ->
        ZipOutputStream(tmp.outputStream().buffered()).use { zout ->
            val entries = zin.entries()
            while (entries.hasMoreElements()) {
                val e = entries.nextElement()
                if (e.name.startsWith(prefix)) continue
                val copy = ZipEntry(e.name).apply {
                    method = e.method
                    time = e.time
                    if (e.method == ZipEntry.STORED) {
                        size = e.size
                        crc = e.crc
                    }
                }
                zout.putNextEntry(copy)
                zin.getInputStream(e).use { it.copyTo(zout) }
                zout.closeEntry()
            }
            libsDir.walkTopDown().filter { it.isFile }.forEach { file ->
                val rel = file.relativeTo(libsDir).invariantSeparatorsPath
                zout.putNextEntry(ZipEntry("$prefix$rel"))
                file.inputStream().use { it.copyTo(zout) }
                zout.closeEntry()
            }
        }
    }
    if (!aab.delete()) {
        throw GradleException("Could not replace $aab")
    }
    if (!tmp.renameTo(aab)) {
        tmp.copyTo(aab, overwrite = true)
        tmp.delete()
    }
}

val injectReleaseNativeDebugSymbols by tasks.registering {
    group = "build"
    description = "Embed native debug symbols in the unsigned release App Bundle"
    dependsOn(packageReleaseNativeDebugSymbols)
    dependsOn("packageReleaseBundle")
    mustRunAfter("shrinkBundleReleaseResources")
    doLast {
        val libs = releaseNativeLibsDir.get().asFile
        val candidates = listOf(
            layout.buildDirectory.file(
                "intermediates/intermediary_bundle/release/shrinkBundleReleaseResources/intermediary-bundle.aab"
            ).get().asFile,
            layout.buildDirectory.file(
                "intermediates/intermediary_bundle/release/packageReleaseBundle/intermediary-bundle.aab"
            ).get().asFile
        )
        val aab = candidates.firstOrNull { it.isFile }
            ?: throw GradleException("Unsigned release AAB not found for native symbol injection")
        if (!libs.isDirectory) {
            throw GradleException("Merged native libs not found at $libs")
        }
        injectNativeDebugSymbolsIntoBundle(aab, libs)
    }
}

tasks.configureEach {
    if (name == "bundleRelease") {
        dependsOn(packageReleaseNativeDebugSymbols)
    }
    if (name == "shrinkBundleReleaseResources") {
        finalizedBy(injectReleaseNativeDebugSymbols)
    }
    if (name == "signReleaseBundle") {
        dependsOn(injectReleaseNativeDebugSymbols)
        mustRunAfter(injectReleaseNativeDebugSymbols)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
}
