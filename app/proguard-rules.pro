# Keep Room, Serialization, and Glance entry points when minify is on.
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature
-keepclassmembers class ** {
    @kotlinx.serialization.SerialName <fields>;
}

# Glance widget receivers/providers
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }

# Google Drive / Sign-In
-keep class com.google.android.gms.** { *; }
-keep class com.google.api.services.drive.** { *; }
-dontwarn com.google.api.client.**
-dontwarn com.google.common.**

# DataStore / preferences file names are reflective
-dontwarn androidx.datastore.**
