# kotlinx-serialization
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.ledgerlite.app.**$$serializer { *; }
-keepclassmembers class com.ledgerlite.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.ledgerlite.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep @kotlinx.serialization.Serializable class com.ledgerlite.app.** { *; }
-keepclassmembers @kotlinx.serialization.Serializable class com.ledgerlite.app.** {
    *** Companion;
    *** serializer(...);
}
-keepattributes *Annotation*, InnerClasses

# Enum
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Room
-keep class com.ledgerlite.app.data.local.** { *; }
-keep class com.ledgerlite.app.data.local.LedgerDatabase { *; }
-keep class com.ledgerlite.app.data.local.Converters { *; }

# App entry
-keep class com.ledgerlite.app.LedgerLiteApp { *; }
-keep class com.ledgerlite.app.MainActivity { *; }

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**
