# Add project specific ProGuard rules here.

# Mantenir classes de dades amb serialització
-keep class io.github.serg987.sudokueinkhtr.SavedGameState { *; }
-keep class io.github.serg987.sudokueinkhtr.SavedCell { *; }
-keep class io.github.serg987.sudokueinkhtr.GameStatistics { *; }

# TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Si tens problemes amb Compose (normalment no cal)
# -keep class androidx.compose.** { *; }

# Ignore warnings for optional Joda-Time dependencies
-dontwarn org.joda.convert.FromString
-dontwarn org.joda.convert.ToString

# Ignore warnings for missing SLF4J bindings
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.slf4j.impl.StaticMDCBinder
-dontwarn org.slf4j.impl.StaticMarkerBinder

# Bypass R8 NPE in LocationManagerCompat
-keep class androidx.core.location.** { *; }
-dontwarn androidx.core.location.**
