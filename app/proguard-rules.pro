# Keep kotlinx.serialization generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.tertiaryinfotech.runtrackgps.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.tertiaryinfotech.runtrackgps.model.** { *; }
