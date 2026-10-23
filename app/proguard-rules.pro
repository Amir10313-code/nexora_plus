# Nexora Plus R8 rules
-keepattributes *Annotation*, InnerClasses, Signature

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Widget + workers are referenced from the manifest / WorkManager by name
-keep class com.mahirex.nexoraplus.widget.** { *; }
-keep class com.mahirex.nexoraplus.data.sync.OccasionSyncWorker { *; }
-keep class com.mahirex.nexoraplus.data.focus.FocusEndWorker { *; }
