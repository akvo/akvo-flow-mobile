# Project specific keep options

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
#-keep class android.support.** { *; }
#-keep interface android.support.** { *; }

# Preserve the special static methods that are required in all enumeration classes.
# For enumeration classes, see http://proguard.sourceforge.net/manual/examples.html#enumerations
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Application classes that will be serialized/deserialized over Gson
-keep class org.akvo.flow.** { *; }
-keep interface org.akvo.flow.** { *; }

# For native methods, see http://proguard.sourceforge.net/manual/examples.html#native
-keepclasseswithmembernames class * {
    native <methods>;
}
# Be safe with context
-keepclasseswithmembernames class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembernames class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keep class androidx.appcompat.app.ActionBarDrawerToggle { *; }
-keep class androidx.appcompat.app.ActionBarDrawerToggle$Delegate { *; }

# Samsung Android 4.2 bug workaround
-keep class !android.support.v7.view.menu.**,!android.support.design.internal.NavigationMenu,!android.support.design.internal.NavigationMenuPresenter,!android.support.design.internal.NavigationSubMenu,** {*;}

# for tests
-dontwarn org.xmlpull.v1.**
-dontwarn org.mockito.**
-dontwarn sun.reflect.**
-dontwarn android.test.**

-dontwarn org.mockito.**
-dontwarn sun.reflect.**
-dontwarn android.test.**

# In case of proguard issues just delete all the proguard config files
# and uncomment the following lines
#-keepattributes **
#-keep class !android.support.v7.view.menu.**,!android.support.design.internal.NavigationMenu,!android.support.design.internal.NavigationMenuPresenter,!android.support.design.internal.NavigationSubMenu,** {*;}
#-dontpreverify
#-dontoptimize
#-dontshrink
#-dontwarn **
#-dontnote **