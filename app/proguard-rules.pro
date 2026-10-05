# ---- 4D FIT R8 rules ----
# Gson-serialised DTOs (API payloads, cached content and mock-server state)
-keep class com.fourdfit.app.data.api.dto.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*

# Retrofit + coroutines (Retrofit ships most rules; these guard R8 full mode)
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation interface com.fourdfit.app.data.api.FitApi

# Gson TypeToken generic signatures
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# Tink (used by androidx.security-crypto) references annotations not on the classpath
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
