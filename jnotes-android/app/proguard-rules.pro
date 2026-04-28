# Add project specific ProGuard rules here.

# Keep model classes used for kotlinx-serialization
-keep,includedescriptorclasses class com.notp9194bot.jnotes.**$$serializer { *; }
-keepclassmembers class com.notp9194bot.jnotes.** {
    *** Companion;
}
-keepclasseswithmembers class com.notp9194bot.jnotes.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# WorkManager workers
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker

# BroadcastReceivers (declared in manifest)
-keep class com.notp9194bot.jnotes.domain.reminder.ReminderReceiver
-keep class com.notp9194bot.jnotes.domain.reminder.BootReceiver

# Kotlin metadata for reflection
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
