# Schützt die Standard-Attribute von AndroidX, Java und GSON
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Verhindert, dass GSON Ihre Datenmodelle beim Schrumpfen unbrauchbar macht
-keep class de.baumann.browser.** { *; }

# Hält den Core von AndroidX-Bibliotheken stabil
-dontwarn androidx.**

# ── Servo Engine JNI ───────────────────────────────────────────────────
# Prevents ProGuard from stripping Servo's JNI bridge classes
-keep class org.servo.** { *; }
-keep class org.mozilla.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}