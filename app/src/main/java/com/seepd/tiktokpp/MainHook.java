package com.seepd.tiktokpp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;

import android.telephony.TelephonyManager;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class MainHook implements IXposedHookLoadPackage {
    private static final String TAG = "TikTokPP";
    private static boolean initialized = false;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!"com.zhiliaoapp.musically".equals(lpparam.packageName)
                && !"com.ss.android.ugc.aweme".equals(lpparam.packageName)) {
            return;
        }

        log("Loaded in " + lpparam.packageName);

        // ── Fix SuperNotCalledException (no context needed) ──
        try {
            final java.lang.reflect.Field mCalledField =
                    android.app.Activity.class.getDeclaredField("mCalled");
            mCalledField.setAccessible(true);

            Class<?> activityClass = Class.forName("android.app.Activity", false, lpparam.classLoader);

            XC_MethodHook hook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Throwable t = param.getThrowable();
                    if (t != null && t.getClass().getName().contains("SuperNotCalled")) {
                        param.setThrowable(null);
                        try {
                            mCalledField.setBoolean(param.thisObject, true);
                        } catch (Throwable ignored) {}
                        log("SuperNotCalledException caught and suppressed");
                    }
                }
            };
            XposedBridge.hookAllMethods(activityClass, "performResume", hook);
            log("performResume hook installed");
        } catch (Throwable t) {
            log("Failed: " + t.getMessage());
        }

        // ── Region hooks via attachBaseContext (needs context for SharedPrefs) ──
        try {
            XposedHelpers.findAndHookMethod(
                    "android.content.ContextWrapper",
                    lpparam.classLoader,
                    "attachBaseContext",
                    Context.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (initialized) return;
                            initialized = true;

                            Context context = (Context) param.args[0];
                            if (context == null) return;

                            RegionPreset preset = RegionPreset.US;
                            ModuleConfig config = ModuleConfig.defaults();

                            // TelephonyManager region spoof
                            installTelephonyHooks(preset);

                            // Load config from preferences, force language + timezone on
                            try {
                                SharedPreferences prefs = context.getSharedPreferences(
                                        ModuleConfig.PREFS, Context.MODE_PRIVATE);
                                prefs.edit()
                                        .putBoolean(ModuleConfig.KEY_LANGUAGE_SPOOF, true)
                                        .putBoolean(ModuleConfig.KEY_TIMEZONE_SPOOF, true)
                                        .apply();
                                config = ModuleConfig.fromPreferences(prefs);

                                EnvironmentHooks environmentHooks = new EnvironmentHooks();
                                environmentHooks.installRegion(lpparam.classLoader, preset);
                                environmentHooks.installSystemEnvironment(config);
                                environmentHooks.installGps(config);
                            } catch (Throwable t) { log("EnvironmentHooks failed: " + t.getMessage()); }

                            // TikTok internal region hooks
                            try {
                                TikTokRegionHooks tikTokRegionHooks = new TikTokRegionHooks(preset);
                                tikTokRegionHooks.install(lpparam.classLoader);
                            } catch (Throwable t) { log("TikTokRegionHooks failed: " + t.getMessage()); }

                            // ── Author location ──
                            try {
                                AuthorLocationHooks authorLocationHooks = new AuthorLocationHooks();
                                authorLocationHooks.install(lpparam.classLoader);
                            } catch (Throwable t) { log("AuthorLocationHooks failed: " + t.getMessage()); }

                            // ── Download hooks (ACL only, like TiktokPatchXposed) ──
                            try {
                                hookAclReturnConstant(lpparam.classLoader,
                                        "com.ss.android.ugc.aweme.feed.model.ACLCommonShare",
                                        "getCode", 0);
                                hookAclReturnConstant(lpparam.classLoader,
                                        "com.ss.android.ugc.aweme.feed.model.ACLCommonShare",
                                        "getShowType", 2);
                                hookAclReturnConstant(lpparam.classLoader,
                                        "com.ss.android.ugc.aweme.feed.model.ACLCommonShare",
                                        "getTranscode", 1);
                                log("ACL share hooks installed");
                            } catch (Throwable t) { log("ACL hooks failed: " + t.getMessage()); }

                            // ── Playback hooks ──
                            try {
                                PlaybackHooks playbackHooks = new PlaybackHooks();
                                playbackHooks.installLoopPrevention(lpparam.classLoader);
                                playbackHooks.installAlwaysShowProgressBar(lpparam.classLoader);
                                playbackHooks.installDefaultSpeed(lpparam.classLoader);
                            } catch (Throwable t) { log("PlaybackHooks failed: " + t.getMessage()); }

                            // ── Feed hooks ──
                            try {
                                FeedHooks feedHooks = new FeedHooks();
                                feedHooks.install(lpparam.classLoader, config);
                            } catch (Throwable t) { log("FeedHooks failed: " + t.getMessage()); }

                            // ── Feed overlay hooks ──
                            try {
                                FeedOverlayHooks feedOverlayHooks = new FeedOverlayHooks();
                                feedOverlayHooks.install(lpparam.classLoader, config);
                            } catch (Throwable t) { log("FeedOverlayHooks failed: " + t.getMessage()); }

                            // ── Purification hooks ──
                            // installComponents disabled (breaks share panel)
                            // installGlobalNavigation disabled (also breaks share panel)

                            // ── Startup login skip ──
                            try {
                                StartupHooks startupHooks = new StartupHooks(SystemClock.elapsedRealtime());
                                startupHooks.installLoginSkip(lpparam.classLoader);
                            } catch (Throwable t) { log("StartupHooks failed: " + t.getMessage()); }

                            // ── Reuse permissions ──
                            try {
                                ReusePermissionHooks reusePermissionHooks = new ReusePermissionHooks();
                                reusePermissionHooks.install(lpparam.classLoader, config);
                            } catch (Throwable t) { log("ReusePermissionHooks failed: " + t.getMessage()); }

                            // ── Comment translation ──
                            try {
                                CommentTranslationHooks commentTranslationHooks = new CommentTranslationHooks(
                                        context, config.autoTranslateComments);
                                commentTranslationHooks.install(lpparam.classLoader);
                            } catch (Throwable t) { log("CommentTranslationHooks failed: " + t.getMessage()); }

                            // ── Offline cache ──
                            try {
                                OfflineCacheHooks offlineCacheHooks = new OfflineCacheHooks(context);
                                offlineCacheHooks.install(lpparam.classLoader);
                            } catch (Throwable t) { log("OfflineCacheHooks failed: " + t.getMessage()); }

                            log("All region + feature hooks installed");
                        }
                    });
            log("attachBaseContext hook installed");
        } catch (Throwable t) {
            log("Failed to hook attachBaseContext: " + t.getMessage());
        }
    }

    private void installTelephonyHooks(RegionPreset preset) {
        try {
            XposedHelpers.findAndHookMethod(
                    TelephonyManager.class, "hasIccCard",
                    XC_MethodReplacement.returnConstant(true));
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(
                    TelephonyManager.class, "getSimState",
                    XC_MethodReplacement.returnConstant(5));
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(
                    TelephonyManager.class, "getNetworkType",
                    XC_MethodReplacement.returnConstant(13));
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(
                    TelephonyManager.class, "getDataNetworkType",
                    XC_MethodReplacement.returnConstant(13));
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(
                    TelephonyManager.class, "isNetworkRoaming",
                    XC_MethodReplacement.returnConstant(false));
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(
                    TelephonyManager.class, "getSimCountryIso",
                    XC_MethodReplacement.returnConstant(preset.code.toLowerCase(Locale.ROOT)));
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(
                    TelephonyManager.class, "getNetworkCountryIso",
                    XC_MethodReplacement.returnConstant(preset.code.toLowerCase(Locale.ROOT)));
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(
                    TelephonyManager.class, "getSimOperator",
                    XC_MethodReplacement.returnConstant(preset.operator));
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(
                    TelephonyManager.class, "getNetworkOperator",
                    XC_MethodReplacement.returnConstant(preset.operator));
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(
                    TelephonyManager.class, "getSimOperatorName",
                    XC_MethodReplacement.returnConstant(preset.operatorName));
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(
                    TelephonyManager.class, "getNetworkOperatorName",
                    XC_MethodReplacement.returnConstant(preset.operatorName));
        } catch (Throwable ignored) {}
        log("Telephony hooks installed: region=" + preset.code
                + ", operator=" + preset.operator + ", name=" + preset.operatorName);
    }

    private static void log(String msg) {
        XposedBridge.log(TAG + ": " + msg);
    }

    private static void hookAclReturnConstant(ClassLoader classLoader,
            String className, String methodName, Object value) {
        try {
            Class<?> type = Class.forName(className, false, classLoader);
            for (Method method : type.getDeclaredMethods()) {
                if (methodName.equals(method.getName()) && method.getParameterCount() == 0) {
                    method.setAccessible(true);
                    XposedBridge.hookMethod(method,
                            XC_MethodReplacement.returnConstant(value));
                }
            }
        } catch (Throwable t) {
            log("hookAclReturnConstant failed: " + className + "#" + methodName);
        }
    }
}
