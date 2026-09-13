package com.seepd.tiktokpp;

import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;

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
     * Filter out VPN/tun network interfaces to hide VPN usage from TikTok.
     * From SimpleTikTokMod: NetworkInterface.getNetworkInterfaces
     * Only filter interfaces that are actually VPN (have no InetAddress or are point-to-point).
     */
    private int hookVpnDetection(ClassLoader classLoader) {
        int installed = 0;
        try {
            Method method = NetworkInterface.class.getMethod("getNetworkInterfaces");
            hook(method)
                    .setId("toki-anti-vpn")
                    .intercept(chain -> {
                        Enumeration<NetworkInterface> original =
                                (Enumeration<NetworkInterface>) chain.proceed();
                        if (original == null) return null;
                        return Collections.enumeration(
                                Collections.list(original).stream()
                                        .filter(ni -> !isRealVpnInterface(ni))
                                        .collect(java.util.stream.Collectors.toList()));
                    });
            installed++;
        } catch (Throwable error) {
            logError("Unable to hook NetworkInterface#getNetworkInterfaces", error);
        }
        logInfo("Anti-detection hooks installed: " + installed + " target(s)");
        return installed;
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

    private static boolean isRealVpnInterface(NetworkInterface ni) {
        String name = ni.getName();
        if (name == null) return false;
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        // Only filter actual VPN interfaces: must start with tun/tap AND be point-to-point
        boolean nameMatch = lower.startsWith("tun") || lower.startsWith("utun") || lower.startsWith("tap");
        if (!nameMatch) return false;
        // A real VPN interface is point-to-point and typically has no hardware address
        try {
            return ni.isPointToPoint() && ni.getHardwareAddress() == null;
        } catch (Exception e) {
            return false;
        }
    }
}
