# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.openshelf.booktracker.**$$serializer { *; }
-keepclassmembers class com.openshelf.booktracker.** {
    *** Companion;
}
-keepclasseswithmembers class com.openshelf.booktracker.** {
    kotlinx.serialization.KSerializer serializer(...);
}