plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android { namespace = "tr.olgun.wifiharita"; compileSdk = 35
 defaultConfig { applicationId = "tr.olgun.wifiharita"; minSdk = 26; targetSdk = 32; versionCode = 1; versionName = "1.0" }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
dependencies { implementation("androidx.core:core-ktx:1.15.0"); implementation("androidx.activity:activity-ktx:1.9.3") }