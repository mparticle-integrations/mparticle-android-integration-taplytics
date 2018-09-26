## Taplytics Kit Integration

This repository contains the [Taplytics](https://www.taplytics.com/) integration for the [mParticle Android SDK](https://github.com/mParticle/mparticle-android-sdk).

### Adding the integration

1. The Taplytics Kit requires that you add Taplytics's Maven server to your build.gradle:

    ```
    repositories {
        maven { url "https://github.com/taplytics/Taplytics-Android-SDK/raw/master/AndroidStudio/" }
        ...
    }
    ```

2. Add the kit dependency to your app's build.gradle:

    ```groovy
    dependencies {
        implementation 'com.mparticle:android-taplytics-kit:5+'
    }
    ```
2. Follow the mParticle Android SDK [quick-start](https://github.com/mParticle/mparticle-android-sdk), then rebuild and launch your app, and verify that you see `"Taplytics detected"` in the output of `adb logcat`.
3. Reference mParticle's integration docs below to enable the integration.

### License

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)