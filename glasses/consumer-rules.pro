# Keep the HeyCyan/X01 vendor SDK and its bundled libraries intact.
# The SDK relies on reflection / fixed class names, so do not rename or strip.
-keep class com.oudmon.** { *; }
-keep interface com.oudmon.** { *; }
-keep class com.jieli.** { *; }
-keep class com.androidnetworking.** { *; }
-dontwarn com.oudmon.**
-dontwarn com.jieli.**
-dontwarn com.androidnetworking.**
