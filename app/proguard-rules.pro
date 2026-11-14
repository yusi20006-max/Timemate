-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keep class ir.timemate.app.data.model.** { *; }



# The bundled prayer JSON is decoded through Gson reflection in release builds.
-keep class ir.timemate.app.prayer.BundledPrayerTimesAsset { *; }
-keep class ir.timemate.app.prayer.BundledPrayerTimesSource { *; }
