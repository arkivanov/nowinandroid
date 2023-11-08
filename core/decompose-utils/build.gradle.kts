plugins {
    alias(libs.plugins.nowinandroid.android.library)
}

android {
    namespace = "com.google.samples.apps.nowinandroid.core.decompose.utils"
}

dependencies {
    implementation(libs.decompose.decompose)
    implementation(libs.kotlinx.coroutines.android)
}
