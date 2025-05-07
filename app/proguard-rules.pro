# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/USER_NAME/Library/Android/sdk/tools/proguard/proguard-android-optimize.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep rules here:

# If you use reflection or JNI, you might need to keep certain classes.
# Example:
# -keep class com.example.MyClass
# -keepclassmembers class com.example.MyClass {
#   public <init>(android.content.Context);
# }

# Keep Hilt generated classes
-keep class * extends androidx.hilt.lifecycle.ViewModelInjectFactory
-keep class * extends dagger.hilt.android.internal.managers.ActivityComponentManager.FragmentComponentBuilder
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager.ViewWithFragmentComponentBuilder
-keep class * extends dagger.hilt.android.internal.managers.BroadcastReceiverComponentManager.BroadcastReceiverComponentBuilder
-keep class * extends dagger.hilt.android.internal.managers.ServiceComponentManager.ServiceComponentBuilder
-keep class * extends dagger.hilt.android.internal.managers.ApplicationComponentManager.ApplicationComponentBuilder

# Keep Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.flow.** {
    *;
}
-keepclassmembers class kotlinx.coroutines.internal.** {
    *;
}
-keepclassmembers class kotlinx.coroutines.channels.** {
    *;
}
-keepclassmembers class kotlinx.coroutines.selects.** {
    *;
}

# Keep Kotlin Serialization
-keepclassmembers class **$$serializer {
    public static final **$$serializer INSTANCE;
    public final kotlinx.serialization.KSerializer[] childSerializers();
    public kotlinx.serialization.KSerializer[] typeParametersSerializers();
    private <init>();
}
-keepclasseswithmembers class ** {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers enum * {
    @kotlinx.serialization.Serializable *;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Coil classes (if reflection is used internally, though usually not needed for basic usage)
# -keep class coil.** { *; }

# Keep Google Play Billing library classes
-keep class com.android.billingclient.** { *; }

# Keep Google AdMob classes
-keep class com.google.android.gms.ads.** { *; }

# If you are using CameraX, ensure its classes are not removed if reflection is used.
# Usually, CameraX is fine with default Proguard rules.
# -keep class androidx.camera.core.** { *; }

# For Jetpack Compose
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keepclassmembers class * {
    @androidx.compose.ui.tooling.preview.Preview <methods>;
}
-keepclassmembers class * implements androidx.compose.ui.tooling.preview.PreviewParameterProvider {
    public <init>(...);
}
-keepclassmembers class * extends androidx.compose.ui.tooling.preview.PreviewParameterProvider {
    public <init>(...);
}
-keepclassmembers class * {
    @androidx.compose.runtime.InternalComposeApi <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.ReadOnlyComposable <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.NonRestartableComposable <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.DisallowComposableCalls <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.Explicitlgan <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.ComposableTarget <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.ComposableOpenTarget <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.ComposableInferredTarget <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.ComposeCompilerApi <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.StableMarker <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.Immutable <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.Stable <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.ProvidableCompositionLocal <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.staticCompositionLocalOf <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.compositionLocalOf <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.CompositionLocal <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.CompositionLocalProvider <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.ProvidedValue <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.RecomposeScope <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.Recomposer <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.Composer <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.RememberObserver <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.SideEffect <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.DisposableEffect <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.LaunchedEffect <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.ProduceStateScope <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.derivedStateOf <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.mutableStateOf <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.mutableStateListOf <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.mutableStateMapOf <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.snapshotFlow <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.remember <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.rememberSaveable <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.Saver <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.SaverScope <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.SaveableStateRegistry <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.SaveableStateHolder <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.autoSaver <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.listSaver <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.mapSaver <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.SaverRestorer <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.SaverScopeImpl <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.SaveableStateRegistryImpl <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.SaveableStateHolderImpl <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.SaveableStateRegistryAmbient <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.SaveableStateHolderAmbient <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.SaveableStateRegistryLocal <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.SaveableStateHolderLocal <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.LocalSaveableStateRegistry <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.LocalSaveableStateHolder <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.SaverKt <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.RememberSaveableKt <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.SaveableStateRegistryKt <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.SaveableStateHolderKt <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.AutoSaverKt <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.ListSaverKt <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.MapSaverKt <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.SaverRestorerKt <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.SaverScopeImplKt <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.SaveableStateRegistryImplKt <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.SaveableStateHolderImplKt <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.SaveableStateRegistryAmbientKt <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.SaveableStateHolderAmbientKt <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.SaveableStateRegistryLocalKt <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.SaveableStateHolderLocalKt <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.LocalSaveableStateRegistryKt <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.saveable.LocalSaveableStateHolderKt <methods>;
}

# Keep Parcelize generated classes
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}
-keepclassmembers class **$$ParcelableCreator {
    *;
}

# Keep Google Play Services Auth classes (Sign-In)
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.common.** { *; }

# Keep Google API Client & HTTP Client & GSON (used by Drive API)
# Keep names used by reflection in GSON
-keepclassmembers,allowobfuscation class * {
    @com.google.api.client.util.Key <fields>;
}
# Keep setters used by reflection in GSON
-keepclassmembers,allowobfuscation class * {
    @com.google.api.client.util.Key <methods>;
}
# Keep specific classes used by Google API Client if needed
-keep class com.google.api.client.googleapis.** { *; }
-keep class com.google.api.client.http.** { *; }
-keep class com.google.api.client.json.** { *; }
-keep class com.google.api.client.util.** { *; }
-keep class com.google.api.services.drive.** { *; }

# Keep GSON specific classes if needed (usually covered by above, but good to be explicit)
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
# Rules for R8 missing classes (javax.naming and org.ietf.jgss)
-dontwarn javax.naming.InvalidNameException
-dontwarn javax.naming.NamingException
-dontwarn javax.naming.directory.Attribute
-dontwarn javax.naming.directory.Attributes
-dontwarn javax.naming.ldap.LdapName
-dontwarn javax.naming.ldap.Rdn
-dontwarn org.ietf.jgss.GSSContext
-dontwarn org.ietf.jgss.GSSCredential
-dontwarn org.ietf.jgss.GSSException
-dontwarn org.ietf.jgss.GSSManager
-dontwarn org.ietf.jgss.GSSName
-dontwarn org.ietf.jgss.Oid