-keep class com.squareup.okhttp.** { *; }
-keep interface com.squareup.okhttp.** { *; }
-dontwarn com.squareup.okhttp.**

-dontwarn io.reactivex.**
-dontwarn okio.**
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepattributes *Annotation*,Signature,Exceptions
-keep class com.squareup.okhttp.** { *; }
-keep interface com.squareup.okhttp.** { *; }
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }