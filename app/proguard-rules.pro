# Add project specific ProGuard rules here.

# Mantenir classes de dades amb serialització
-keep class com.ktacrack.sudokueink.SavedGameState { *; }
-keep class com.ktacrack.sudokueink.SavedCell { *; }
-keep class com.ktacrack.sudokueink.GameStatistics { *; }

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
