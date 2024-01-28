buildscript {
    extra.apply {
        set("kotlin_version", "1.9.22")
    }
    val kotlinVersion = rootProject.extra.get("kotlin_version") as String
    repositories {
        google()
        mavenCentral()
        
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.google.com") }
        maven { url = uri("https://jitpack.io") }
    }
}
tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}