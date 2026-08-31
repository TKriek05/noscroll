# Sideload-build draait zonder minify; deze regels staan klaar voor als dat verandert.
-keep class com.tkriek.scrollless.service.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
