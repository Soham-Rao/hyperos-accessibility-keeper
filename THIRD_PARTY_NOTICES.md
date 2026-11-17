# Third-party notices

Reviewed 2026-11-18 against the resolved release runtime dependency graph.

The project source is [MIT-licensed](LICENSE). Runtime libraries below retain **Apache License 2.0**; the MIT license does not replace their licenses. The full [Apache text](legal/APACHE-2.0.txt), [offline-readable notices](legal/THIRD_PARTY_NOTICES.txt), and [machine-readable inventory with artifact hashes](legal/runtime-dependencies.json) are included in this repository and packaged into the app.

## Runtime components

| Component | Version | License |
| --- | --- | --- |
| `androidx.activity:activity` | 1.7.0 | Apache-2.0 |
| `androidx.annotation:annotation-experimental` | 1.4.1 | Apache-2.0 |
| `androidx.annotation:annotation-jvm` | 1.8.1 | Apache-2.0 |
| `androidx.appcompat:appcompat-resources` | 1.7.0 | Apache-2.0 |
| `androidx.appcompat:appcompat` | 1.7.0 | Apache-2.0 |
| `androidx.arch.core:core-common` | 2.2.0 | Apache-2.0 |
| `androidx.arch.core:core-runtime` | 2.2.0 | Apache-2.0 |
| `androidx.collection:collection-jvm` | 1.4.2 | Apache-2.0 |
| `androidx.concurrent:concurrent-futures-ktx` | 1.1.0 | Apache-2.0 |
| `androidx.concurrent:concurrent-futures` | 1.1.0 | Apache-2.0 |
| `androidx.core:core-ktx` | 1.15.0 | Apache-2.0 |
| `androidx.core:core` | 1.15.0 | Apache-2.0 |
| `androidx.cursoradapter:cursoradapter` | 1.0.0 | Apache-2.0 |
| `androidx.customview:customview` | 1.0.0 | Apache-2.0 |
| `androidx.drawerlayout:drawerlayout` | 1.0.0 | Apache-2.0 |
| `androidx.emoji2:emoji2-views-helper` | 1.3.0 | Apache-2.0 |
| `androidx.emoji2:emoji2` | 1.3.0 | Apache-2.0 |
| `androidx.fragment:fragment` | 1.5.4 | Apache-2.0 |
| `androidx.interpolator:interpolator` | 1.0.0 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-common-jvm` | 2.8.7 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-livedata-core-ktx` | 2.8.7 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-livedata-core` | 2.8.7 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-livedata` | 2.8.7 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-process` | 2.8.7 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-runtime-android` | 2.8.7 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-service` | 2.8.7 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-viewmodel-android` | 2.8.7 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-viewmodel-savedstate` | 2.8.7 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-viewmodel` | 2.8.7 | Apache-2.0 |
| `androidx.loader:loader` | 1.0.0 | Apache-2.0 |
| `androidx.profileinstaller:profileinstaller` | 1.3.1 | Apache-2.0 |
| `androidx.resourceinspection:resourceinspection-annotation` | 1.0.1 | Apache-2.0 |
| `androidx.room:room-common` | 2.6.1 | Apache-2.0 |
| `androidx.room:room-ktx` | 2.6.1 | Apache-2.0 |
| `androidx.room:room-runtime` | 2.6.1 | Apache-2.0 |
| `androidx.savedstate:savedstate` | 1.2.1 | Apache-2.0 |
| `androidx.sqlite:sqlite-framework` | 2.4.0 | Apache-2.0 |
| `androidx.sqlite:sqlite` | 2.4.0 | Apache-2.0 |
| `androidx.startup:startup-runtime` | 1.1.1 | Apache-2.0 |
| `androidx.tracing:tracing-ktx` | 1.2.0 | Apache-2.0 |
| `androidx.tracing:tracing` | 1.2.0 | Apache-2.0 |
| `androidx.vectordrawable:vectordrawable-animated` | 1.1.0 | Apache-2.0 |
| `androidx.vectordrawable:vectordrawable` | 1.1.0 | Apache-2.0 |
| `androidx.versionedparcelable:versionedparcelable` | 1.1.1 | Apache-2.0 |
| `androidx.viewpager:viewpager` | 1.0.0 | Apache-2.0 |
| `androidx.work:work-runtime-ktx` | 2.10.0 | Apache-2.0 |
| `androidx.work:work-runtime` | 2.10.0 | Apache-2.0 |
| `com.google.guava:listenablefuture` | 1.0 | Apache-2.0 |
| `org.jetbrains.kotlin:kotlin-stdlib-jdk7` | 1.8.22 | Apache-2.0 |
| `org.jetbrains.kotlin:kotlin-stdlib-jdk8` | 1.8.22 | Apache-2.0 |
| `org.jetbrains.kotlin:kotlin-stdlib` | 2.2.0 | Apache-2.0 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.7.3 | Apache-2.0 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm` | 1.7.3 | Apache-2.0 |
| `org.jetbrains:annotations` | 23.0.0 | Apache-2.0 |

## Evidence and limits

54 unique runtime components were resolved. Each had Apache 2.0 license metadata, directly or via its Maven parent. `com.google.guava:listenablefuture:1.0` inherits its license from `com.google.guava:guava-parent:26.0-android`; it was not classified from a missing field alone.

Runtime AAR/JAR files and their nested `classes.jar` contents were inspected for LICENSE, NOTICE, COPYING and copyright files. Embedded Apache texts were recorded; no additional notice files were found by that inspection. This is an artifact/metadata review, not an independent legal-ownership or security audit.

AndroidX is credited to the Android Open Source Project and contributors; Kotlin/coroutines/annotations to JetBrains and contributors; Guava to Google and contributors. Follow each component’s published source and terms for its specific ownership notices.

## Assets and development tools

System fonts and framework drawable references are provided by Android. Installed app icons/names are read from installed apps, not redistributed as Keeper artwork. The project vector icons and README demo-data SVGs were authored for this fork. No third-party font, stock image, icon pack or provenance screenshot is bundled in the APK by this work.

The Gradle wrapper retained in the source repository is Apache-2.0-licensed. The Android build plugin, Kotlin compiler, JUnit and Hamcrest are development/test inputs and are outside this runtime APK inventory. Dependency versions and bundled assets must be reviewed again when changed.

References: [AndroidX license](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/LICENSE.txt), [Kotlin license](https://github.com/JetBrains/kotlin/blob/v2.2.0/license/LICENSE.txt), [coroutines license](https://github.com/Kotlin/kotlinx.coroutines/blob/1.7.3/LICENSE.txt), [Guava license](https://github.com/google/guava/blob/v26.0/COPYING), [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0).
