-keep class io.github.libxposed.** { *; }
-keep class com.seepd.tiktokpp.MainHook { *; }
-keep class com.seepd.tiktokpp.SettingsProvider { *; }

-keepclassmembers class * extends io.github.libxposed.api.XposedModule {
    *;
}

-keep class com.seepd.tiktokpp.HookFeature { *; }
-keep class com.seepd.tiktokpp.ModuleConfig { *; }
