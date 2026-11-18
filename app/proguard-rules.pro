# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Hilt rules
-keep public class * extends android.app.Service
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.view.View
-keep class  **.*_HiltModules* { *; }
-keep class com.google.dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Retrofit / OkHttp / Moshi
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keep @interface retrofit2.http.** { *; }

# Keep network models to avoid obfuscation of field names used by Moshi reflection
-keep class io.github.alxiw.wearpagingmediator.data.api.** { *; }
-keepclassmembers class io.github.alxiw.wearpagingmediator.data.model.** {
    <fields>;
}

# Coil
-keep class coil.** { *; }
-dontwarn coil.**
