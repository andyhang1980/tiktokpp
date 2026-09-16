package com.seepd.tiktokpp;

import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/** Long press to 2x speed, double tap to like, and other gesture hooks. */
final class GestureHooks {

    GestureHooks() {
    }

    void installLongPress2x(ClassLoader classLoader) {
        try {
            // Hook the video player's touch event to detect long press
            XposedHelpers.findAndHookMethod(
                    "com.ss.android.ugc.aweme.feed.model.Video",
                    classLoader,
                    "getSpeed",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            // Store original speed for restoration
                        }
                    });
        } catch (Throwable ignored) {
        }
    }
}
