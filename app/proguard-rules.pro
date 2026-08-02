# Keep Room, Serialization, and Glance entry points when minify is on.
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature
-keepclassmembers class ** {
    @kotlinx.serialization.SerialName <fields>;
}

# Glance widget receivers/providers
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }

# DataStore / preferences file names are reflective
-dontwarn androidx.datastore.**
