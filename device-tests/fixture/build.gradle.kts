plugins { id("com.android.application") version "8.13.2" }
val renamedFixture = providers.gradleProperty("renameFixture").orNull == "true"
android {
 namespace = "com.example.keeperfixture"
 compileSdk = 36
 defaultConfig {
  applicationId = "com.example.keeperfixture"; minSdk = 33; targetSdk = 36; versionCode = 1; versionName = "1"
  manifestPlaceholders["fixtureService"] = if (renamedFixture) ".RenamedService" else ".FixtureService"
 }
}
