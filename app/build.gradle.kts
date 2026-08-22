import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id ("kotlin-kapt")
    id("com.google.gms.google-services")
}

val localProperties = Properties().apply {
    load(rootProject.file("local.properties").inputStream())
}

android {
    namespace = "kr.baeksuk.urlBox"
    compileSdk = 36

    defaultConfig {
        applicationId = "kr.baeksuk.urlBox"
        minSdk = 28
        targetSdk = 36
        versionCode = 43
        versionName = "1.2.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "default_web_client_id",
            "\"${localProperties["default_web_client_id"]}\""
        )

        buildConfigField(
            "String",
            "kakao_native_app_key",
            "\"${localProperties["kakao_native_app_key"]}\""
        )

        buildConfigField(
            "String",
            "ADMIN_EMAILS",
            "\"${localProperties["admin_emails"]}\""
        )

        manifestPlaceholders["kakao_app_key"] = localProperties["kakao_app_key"] as Any

        manifestPlaceholders["ad_mob_app_id"] = localProperties["ad_mob_app_id"] as Any

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures{
        buildConfig = true
        dataBinding = true
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        }
    }

    /** BaseActivity 에서 댓글 알림을 위한 GoogleCredential 의존성 추가를 위한 코드 -> com.google.api-client:google-api-client:1.34.0 **/
    packaging {
        resources {
            excludes += mutableSetOf("META-INF/DEPENDENCIES")
        }
        jniLibs {
            // 16KB 페이지 크기 정렬 활성화
            useLegacyPackaging = false
        }
    }

}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")

    implementation("androidx.datastore:datastore-preferences:1.2.1")

    /** 새로고침 라이브러리 **/
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    /** 튜토리얼 뷰페이져 관련 라이브러리 **/
    implementation("com.tbuonomo:dotsindicator:5.1.0")

    /** 로티 애니메이션 **/
    implementation ("com.airbnb.android:lottie:6.0.0")
    
    implementation ("com.google.android.play:review-ktx:2.0.2")

    implementation("com.google.android.gms:play-services-ads:23.4.0")

    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation ("androidx.appcompat:appcompat:1.6.0-rc01") // 언어 변경

    implementation ("com.kakao.sdk:v2-all:2.20.0")// 전체 모듈 설치, 2.11.0 버전부터 지원
    implementation ("com.kakao.sdk:v2-user:2.20.0")// 카카오 로그인 API 모듈
    implementation ("com.kakao.sdk:v2-share:2.20.0") // 카카오톡 공유 API 모듈
    implementation ("com.kakao.sdk:v2-talk:2.20.0") // 카카오톡 채널, 카카오톡 소셜, 카카오톡 메시지 API 모듈
    implementation ("com.kakao.sdk:v2-friend:2.20.0") // 피커 API 모듈
    implementation ("com.kakao.sdk:v2-cert:2.20.0") // 카카오톡 인증 서비스 API 모듈


    implementation ("com.github.bumptech.glide:glide:4.15.1")
    kapt ("com.github.bumptech.glide:compiler:4.15.1")

    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("androidx.credentials:credentials:1.5.0-alpha05")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0-alpha05")
    implementation ("com.google.api-client:google-api-client:1.34.0")
    implementation("com.google.firebase:firebase-functions-ktx:20.1.0")
    implementation(platform("com.google.firebase:firebase-bom:32.3.1"))
    implementation("com.google.android.recaptcha:recaptcha:18.4.0")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx:21.0.0")
    implementation("com.google.firebase:firebase-storage-ktx:21.0.1")
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    implementation ("androidx.viewpager2:viewpager2:1.1.0")
    implementation ("com.google.android.flexbox:flexbox:3.0.0")
    implementation("androidx.room:room-runtime:2.8.4")
    annotationProcessor("androidx.room:room-compiler:2.8.4")
    kapt("androidx.room:room-compiler:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")

    implementation("com.vanniktech:android-image-cropper:4.6.0")
    implementation ("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")

    implementation ("io.insert-koin:koin-android:3.5.0")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}