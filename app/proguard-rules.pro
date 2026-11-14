-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keep class ir.timemate.app.data.model.** { *; }

# PDFBox references an optional JPEG2000 image decoder. TimeMate only extracts text
# from the University's timetable PDFs and never invokes JPX image decoding.
-dontwarn com.gemalto.jp2.JP2Decoder
