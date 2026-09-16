package com.seepd.tiktokpp;

import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;

import android.telephony.TelephonyManager;

/**
 * Anti-detection hooks: VPN hiding, SIM/network spoofing.
 * Adapted from SimpleTikTokMod — does NOT hook SystemProperties,
 * Settings.Secure, or Location to avoid TikTok crash.
 */
final class AntiDetectionHooks extends HookFeature {

    AntiDetectionHooks() {
    }

    int install(ClassLoader classLoader) {
        int installed = 0;
        installed += hookVpnDetection(classLoader);
        installed += hookSimNetworkSpoofing(classLoader);
        logInfo("Anti-detection hooks installed: " + installed + " target(s)");
        return installed;
    }

    /**
     * VPN detection hook — filters VPN network interfaces from enumeration.
     * Only filters tun and ppp prefixes, matching SimpleTikTokMod exactly.
     */
    private int hookVpnDetection(ClassLoader classLoader) {
        int installed = 0;

        try {
            Method getNetworkInterfaces = NetworkInterface.class.getMethod("getNetworkInterfaces");
            hook(getNetworkInterfaces)
                    .setId("anti-vpn-getNetworkInterfaces")
                    .intercept(chain -> {
                        Enumeration<NetworkInterface> original =
                                (Enumeration<NetworkInterface>) chain.proceed();
                        if (original == null) return null;
                        ArrayList<NetworkInterface> filtered = new ArrayList<>();
                        while (original.hasMoreElements()) {
                            NetworkInterface ni = original.nextElement();
                            String name = ni.getName().toLowerCase(Locale.ROOT);
                            if (!name.startsWith("tun") && !name.startsWith("ppp")) {
                                filtered.add(ni);
                            }
                        }
                        return Collections.enumeration(filtered);
                    });
            installed++;
        } catch (Throwable error) {
            logError("Unable to hook NetworkInterface#getNetworkInterfaces", error);
        }

        return installed;
    }

    /**
     * Spoof TelephonyManager to make TikTok think SIM is present and
     * network is connected. From SimpleTikTokMod.
     */
    private int hookSimNetworkSpoofing(ClassLoader classLoader) {
        int installed = 0;

        // hasIccCard → true (SIM present)
        try {
            Method method = TelephonyManager.class.getMethod("hasIccCard");
            hook(method)
                    .setId("anti-detect-hasIccCard")
                    .intercept(chain -> true);
            installed++;
        } catch (Throwable error) {
            logError("Unable to hook TelephonyManager#hasIccCard", error);
        }

        // getSimState → 5 (SIM_STATE_READY)
        try {
            Method method = TelephonyManager.class.getMethod("getSimState");
            hook(method)
                    .setId("anti-detect-getSimState")
                    .intercept(chain -> 5);
            installed++;
        } catch (Throwable error) {
            logError("Unable to hook TelephonyManager#getSimState", error);
        }

        // getNetworkType → 13 (NETWORK_TYPE_LTE)
        try {
            Method method = TelephonyManager.class.getMethod("getNetworkType");
            hook(method)
                    .setId("anti-detect-getNetworkType")
                    .intercept(chain -> 13);
            installed++;
        } catch (Throwable error) {
            logError("Unable to hook TelephonyManager#getNetworkType", error);
        }

        // getDataNetworkType → 13 (NETWORK_TYPE_LTE)
        try {
            Method method = TelephonyManager.class.getMethod("getDataNetworkType");
            hook(method)
                    .setId("anti-detect-getDataNetworkType")
                    .intercept(chain -> 13);
            installed++;
        } catch (Throwable error) {
            logError("Unable to hook TelephonyManager#getDataNetworkType", error);
        }

        // isNetworkRoaming → false
        try {
            Method method = TelephonyManager.class.getMethod("isNetworkRoaming");
            hook(method)
                    .setId("anti-detect-isNetworkRoaming")
                    .intercept(chain -> false);
            installed++;
        } catch (Throwable error) {
            logError("Unable to hook TelephonyManager#isNetworkRoaming", error);
        }

        return installed;
    }

}
