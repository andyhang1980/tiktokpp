package com.seepd.tiktokpp;

import java.lang.reflect.Method;
import android.location.Location;
import android.os.Build;
import android.provider.Settings;

import io.github.libxposed.api.XposedModule;

/**
 * Anti-detection hooks: VPN hiding, mock location hiding, build property spoofing.
 * Adapted from SimpleTikTokMod and GlobalKit modules.
 */
final class AntiDetectionHooks extends HookFeature {

    AntiDetectionHooks(XposedModule module) {
        super(module);
    }

    int install(ClassLoader classLoader) {
        int installed = 0;
        installed += hookVpnDetection(classLoader);
        installed += hookMockLocation(classLoader);
        installed += hookBuildProperties(classLoader);
        return installed;
    }

    /**
     * VPN detection hook - disabled by default to prevent breaking network connectivity.
     * Enable only when VPN hiding is actually needed.
     */
    private int hookVpnDetection(ClassLoader classLoader) {
        // Disabled: filtering network interfaces can break connectivity on many devices
        logInfo("Anti-VPN hook skipped (disabled to preserve network connectivity)");
        return 0;
    }

    /**
     * Hide mock location flag and mock provider status from TikTok.
     * From GlobalKit: Settings.Secure.getInt("mock_location"), Location.isFromMockProvider
     */
    private int hookMockLocation(ClassLoader classLoader) {
        int installed = 0;

        // Hide mock_location setting
        try {
            Class<?> settingsSecure = Class.forName("android.provider.Settings$Secure");
            Method getInt = settingsSecure.getMethod("getInt",
                    android.content.ContentResolver.class, String.class, int.class);
            hook(getInt)
                    .setId("toki-anti-mock-location")
                    .intercept(chain -> {
                        String key = (String) chain.getArg(1);
                        if ("mock_location".equals(key)) {
                            return 0;
                        }
                        return chain.proceed();
                    });
            installed++;
        } catch (Throwable error) {
            logError("Unable to hook Settings.Secure#getInt for mock_location", error);
        }

        // Hide isFromMockProvider
        try {
            Method method = Location.class.getMethod("isFromMockProvider");
            hook(method)
                    .setId("toki-anti-mock-provider")
                    .intercept(chain -> false);
            installed++;
        } catch (Throwable error) {
            logError("Unable to hook Location#isFromMockProvider", error);
        }

        return installed;
    }

    /**
     * Spoof Build properties to prevent device fingerprinting.
     * From SimpleTikTokMod: Build fields + SystemProperties
     */
    private int hookBuildProperties(ClassLoader classLoader) {
        int installed = 0;

        // Hook SystemProperties.get to intercept property lookups
        try {
            Class<?> sysPropClass = Class.forName("android.os.SystemProperties");
            Method getMethod = sysPropClass.getMethod("get", String.class, String.class);
            hook(getMethod)
                    .setId("toki-anti-sysprop")
                    .intercept(chain -> {
                        String key = (String) chain.getArg(0);
                        String defaultVal = (String) chain.getArg(1);
                        // Block fingerprinting properties
                        if ("ro.product.model".equals(key)) {
                            return Build.MODEL;
                        }
                        if ("ro.product.manufacturer".equals(key)) {
                            return Build.MANUFACTURER;
                        }
                        if ("ro.product.brand".equals(key)) {
                            return Build.BRAND;
                        }
                        if ("ro.product.device".equals(key)) {
                            return Build.DEVICE;
                        }
                        if ("ro.product.name".equals(key)) {
                            return Build.PRODUCT;
                        }
                        if ("ro.build.display.id".equals(key)) {
                            return Build.DISPLAY;
                        }
                        if ("ro.build.version.release".equals(key)) {
                            return Build.VERSION.RELEASE;
                        }
                        if ("ro.build.version.sdk".equals(key)) {
                            return String.valueOf(Build.VERSION.SDK_INT);
                        }
                        return chain.proceed();
                    });
            installed++;
        } catch (Throwable error) {
            logError("Unable to hook SystemProperties#get", error);
        }

        return installed;
    }

}
