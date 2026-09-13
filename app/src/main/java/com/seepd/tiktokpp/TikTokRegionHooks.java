package com.seepd.tiktokpp;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.TimeZone;

import io.github.libxposed.api.XposedModule;

/**
 * TikTok-specific region/environment hooks merged from TikTokRegionHook v1.5.0.
 * <p>
 * Targets TikTok's own internal region classes (com.bytedance.i18n.region.*,
 * com.ss.ugc.clientai.core.api.FeatureProducer, and version-specific obfuscated
 * classes). These complement the framework-level telephony/locale/timezone hooks
 * in {@link EnvironmentHooks}.
 * <p>
 * Each hook is wrapped in try/catch so it degrades gracefully if the target class
 * is absent from a given TikTok build.
 */
final class TikTokRegionHooks extends HookFeature {

    private final String regionCode;
    private final String countryCode;
    private final String mccMnc;
    private final String carrier;

    TikTokRegionHooks(XposedModule module, RegionPreset preset) {
        super(module);
        this.regionCode = preset.code.toUpperCase(Locale.ROOT);
        this.countryCode = preset.code.toLowerCase(Locale.ROOT);
        this.mccMnc = preset.operator;
        this.carrier = preset.operatorName;
    }

    int install(ClassLoader classLoader) {
        int installed = 0;
        installed += hookStoreRegionSource(classLoader);
        installed += hookFeatureProducer(classLoader);
        installed += hookRegionManagerStoreRegion(classLoader);
        installed += hookNetworkProvider(classLoader);
        installed += hookRegionProvider(classLoader);
        installed += hookSystemLocaleRegion(classLoader);
        installed += hookCurrentSimInfo(classLoader);
        logInfo("TikTok region hooks installed: " + installed + " target(s)");
        return installed;
    }

    // ── StoreRegionSource ──────────────────────────────────────────────────

    /**
     * Hook com.bytedance.i18n.region.StoreRegionSource.LIZ() to return a
     * spoofed 13Dk(region, "local") region holder. This forces the
     * store_region seen by all downstream consumers.
     */
    private int hookStoreRegionSource(ClassLoader classLoader) {
        int installed = 0;
        try {
            Class<?> storeRegionSourceClass = Class.forName(
                    "com.bytedance.i18n.region.StoreRegionSource", false, classLoader);
            Class<?> regionHolderClass = findRegionHolderClass(classLoader);
            if (regionHolderClass == null) {
                logInfo("StoreRegionSource: region holder class not found, skipping");
                return 0;
            }

            for (Method method : storeRegionSourceClass.getDeclaredMethods()) {
                if (method.getParameterCount() != 0) {
                    continue;
                }
                if (!regionHolderClass.isAssignableFrom(method.getReturnType())
                        && !method.getReturnType().equals(regionHolderClass)) {
                    continue;
                }
                Object spoofedHolder = createRegionHolder(regionHolderClass);
                if (spoofedHolder == null) {
                    continue;
                }
                hook(method)
                        .setId("toki-store-region-source")
                        .intercept(chain -> spoofedHolder);
                installed++;
                logInfo("Hooked StoreRegionSource." + method.getName()
                        + " -> region=" + regionCode);
                break;
            }
        } catch (ClassNotFoundException ignored) {
            logInfo("StoreRegionSource not found in this TikTok build");
        } catch (Throwable error) {
            logError("Unable to hook StoreRegionSource", error);
        }
        return installed;
    }

    // ── FeatureProducer ────────────────────────────────────────────────────

    /**
     * Hook FeatureProducer.getStringFeature / getStringFeature$default to
     * spoof f_global_* region feature strings.
     */
    private int hookFeatureProducer(ClassLoader classLoader) {
        int installed = 0;
        try {
            Class<?> featureProducerClass = Class.forName(
                    "com.ss.ugc.clientai.core.api.FeatureProducer", false, classLoader);
            for (Method method : featureProducerClass.getDeclaredMethods()) {
                String name = method.getName();
                if (!"getStringFeature".equals(name)
                        && !"getStringFeature$default".equals(name)) {
                    continue;
                }
                if (method.getReturnType() != String.class) {
                    continue;
                }
                final boolean isDefaultVariant = name.endsWith("$default");
                hook(method)
                        .setId("toki-feature-producer-" + name + "-"
                                + method.getParameterCount())
                        .intercept(chain -> {
                            int paramCount = method.getParameterCount();
                            String featureKey = null;
                            // getStringFeature(featureKey, ...) — key at index 0
                            // getStringFeature$default(Companion, featureKey, ...) — key at index 1
                            int startIndex = isDefaultVariant ? 1 : 0;
                            for (int i = startIndex; i < paramCount; i++) {
                                Object arg = chain.getArg(i);
                                if (arg instanceof String) {
                                    featureKey = (String) arg;
                                    break;
                                }
                            }
                            if (featureKey != null) {
                                String spoofed = getRegionFeatureValue(featureKey);
                                if (spoofed != null) {
                                    return spoofed;
                                }
                            }
                            return chain.proceed();
                        });
                installed++;
            }
            if (installed > 0) {
                logInfo("Hooked FeatureProducer: " + installed + " overload(s)");
            }
        } catch (ClassNotFoundException ignored) {
            logInfo("FeatureProducer not found in this TikTok build");
        } catch (Throwable error) {
            logError("Unable to hook FeatureProducer", error);
        }
        return installed;
    }

    private String getRegionFeatureValue(String key) {
        if (key == null) {
            return null;
        }
        switch (key) {
            case "f_global_carrier_region":
            case "f_global_op_region":
            case "f_global_sys_region":
            case "f_global_account_region":
            case "f_global_residence":
            case "f_global_carrier_region_v2":
            case "f_global_region":
            case "f_global_current_region":
                return regionCode;
            case "f_global_timezone_name":
                return TimeZone.getDefault().getID();
            case "f_global_timezone_offset":
                return String.valueOf(
                        TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000);
            case "f_global_mcc_mnc":
                return mccMnc;
            default:
                return null;
        }
    }

    // ── RegionManager store_region ─────────────────────────────────────────

    /**
     * Hook RegionManager.LIZ() (the store_region accessor) to return a
     * spoofed region holder, as a secondary override for the region source.
     */
    private int hookRegionManagerStoreRegion(ClassLoader classLoader) {
        int installed = 0;
        try {
            Class<?> regionManagerClass = Class.forName(
                    "com.bytedance.i18n.region.RegionManager", false, classLoader);
            Class<?> regionHolderClass = findRegionHolderClass(classLoader);
            if (regionHolderClass == null) {
                return 0;
            }

            for (Method method : regionManagerClass.getDeclaredMethods()) {
                if (!"LIZ".equals(method.getName()) || method.getParameterCount() != 0) {
                    continue;
                }
                Object spoofedHolder = createRegionHolder(regionHolderClass);
                if (spoofedHolder == null) {
                    continue;
                }
                hook(method)
                        .setId("toki-region-manager-store-region")
                        .intercept(chain -> spoofedHolder);
                installed++;
                logInfo("Hooked RegionManager.LIZ -> region=" + regionCode);
            }
        } catch (ClassNotFoundException ignored) {
            logInfo("RegionManager not found in this TikTok build");
        } catch (Throwable error) {
            logError("Unable to hook RegionManager store region", error);
        }
        return installed;
    }

    // ── X.0VV8 network provider (46.6.3 obfuscated) ───────────────────────

    /**
     * Attempt to hook X.0VV8 (46.6.3's network info provider). This class
     * exposes methods returning countryIso, mccMnc, and carrier. The
     * obfuscated name changes per TikTok build, so this is best-effort.
     */
    private int hookNetworkProvider(ClassLoader classLoader) {
        int installed = 0;
        String[] classNames = {"X.0VV8"};
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className, false, classLoader);
                installed += hookReturning(clazz, "LIZJ", countryCode);
                installed += hookReturning(clazz, "LJ", mccMnc);
                installed += hookReturning(clazz, "LJI", carrier);
                installed += hookReturning(clazz, "LJIIIIZZ", countryCode);
                installed += hookReturning(clazz, "LJIIJ", mccMnc);
                installed += hookReturning(clazz, "LJIIL", carrier);
                if (installed > 0) {
                    logInfo("Hooked " + className + ": " + installed + " method(s)");
                }
            } catch (ClassNotFoundException ignored) {
                // Not found — class name may have changed in this TikTok build
            } catch (Throwable error) {
                logError("Unable to hook network provider " + className, error);
            }
        }
        return installed;
    }

    // ── X.11ga region provider (46.6.3 obfuscated) ────────────────────────

    /**
     * Attempt to hook X.11ga (46.6.3's region provider). All methods return
     * the 2-letter region code. Best-effort — obfuscated name varies per build.
     */
    private int hookRegionProvider(ClassLoader classLoader) {
        int installed = 0;
        String[] classNames = {"X.11ga"};
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className, false, classLoader);
                installed += hookReturning(clazz, "LIZ", regionCode);
                installed += hookReturning(clazz, "LIZIZ", regionCode);
                installed += hookReturning(clazz, "LIZJ", regionCode);
                installed += hookReturning(clazz, "LIZLLL", regionCode);
                installed += hookReturning(clazz, "LJ", regionCode);
                installed += hookReturning(clazz, "LJFF", regionCode);
                if (installed > 0) {
                    logInfo("Hooked " + className + ": " + installed + " method(s)");
                }
            } catch (ClassNotFoundException ignored) {
                // Not found — class name may have changed in this TikTok build
            } catch (Throwable error) {
                logError("Unable to hook region provider " + className, error);
            }
        }
        return installed;
    }

    // ── X.0VV4 system locale region (46.6.3 obfuscated) ───────────────────

    /**
     * Attempt to hook X.0VV4.LIZIZ(Locale)->String which derives the region
     * code from a Locale. Best-effort — obfuscated name varies per build.
     */
    private int hookSystemLocaleRegion(ClassLoader classLoader) {
        int installed = 0;
        String[] classNames = {"X.0VV4"};
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className, false, classLoader);
                installed += hookReturning(clazz, "LIZIZ", regionCode);
                if (installed > 0) {
                    logInfo("Hooked " + className + ": " + installed + " method(s)");
                }
            } catch (ClassNotFoundException ignored) {
                // Not found
            } catch (Throwable error) {
                logError("Unable to hook system locale region " + className, error);
            }
        }
        return installed;
    }

    // ── X.0V1x/X.0V1y current SIM info (46.6.3 obfuscated) ───────────────

    /**
     * Attempt to hook X.0V1x.LIZJ(int, ...)->X.0V1y which constructs a
     * current SIM info object with (subId, countryIso, carrier). Best-effort.
     */
    private int hookCurrentSimInfo(ClassLoader classLoader) {
        int installed = 0;
        try {
            Class<?> simInfoClass = Class.forName("X.0V1y", false, classLoader);
            Class<?> factoryClass = Class.forName("X.0V1x", false, classLoader);
            for (Method method : factoryClass.getDeclaredMethods()) {
                if (!"LIZJ".equals(method.getName())
                        || method.getParameterCount() != 2) {
                    continue;
                }
                hook(method)
                        .setId("toki-current-sim-info")
                        .intercept(chain -> {
                            try {
                                Constructor<?> ctor = simInfoClass.getDeclaredConstructor(
                                        Integer.class, String.class, String.class);
                                ctor.setAccessible(true);
                                return ctor.newInstance(
                                        chain.getArg(0), countryCode, carrier);
                            } catch (Throwable ignored) {
                                return chain.proceed();
                            }
                        });
                installed++;
                logInfo("Hooked X.0V1x.LIZJ -> sim info with region=" + countryCode);
            }
        } catch (ClassNotFoundException ignored) {
            // Not found — class names may have changed in this TikTok build
        } catch (Throwable error) {
            logError("Unable to hook current SIM info", error);
        }
        return installed;
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /**
     * Find the region holder class (X.13Dk in 46.7.16, X.0W7B in 46.6.3).
     * These are simple data classes holding (region, source) strings.
     */
    private Class<?> findRegionHolderClass(ClassLoader classLoader) {
        String[] candidates = {"X.13Dk", "X.0W7B"};
        for (String name : candidates) {
            try {
                return Class.forName(name, false, classLoader);
            } catch (ClassNotFoundException ignored) {
                // try next
            }
        }
        return null;
    }

    /**
     * Instantiate a region holder via its (String, String) constructor.
     * Field layout: LIZ = region code, LIZIZ = source identifier ("local", "cdn").
     */
    private Object createRegionHolder(Class<?> clazz) {
        try {
            Constructor<?> ctor = clazz.getDeclaredConstructor(String.class, String.class);
            ctor.setAccessible(true);
            return ctor.newInstance(regionCode, "local");
        } catch (Throwable ignored) {
            // Fallback: try any 2-parameter String constructor
            try {
                for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
                    Class<?>[] params = ctor.getParameterTypes();
                    if (params.length == 2
                            && params[0].equals(String.class)
                            && params[1].equals(String.class)) {
                        ctor.setAccessible(true);
                        return ctor.newInstance(regionCode, "local");
                    }
                }
            } catch (Throwable error) {
                logError("Unable to create region holder", error);
            }
        }
        return null;
    }

    /**
     * Hook every method named {@code methodName} on {@code clazz} whose return
     * type is assignable from {@link String} to return {@code value} constantly.
     */
    private int hookReturning(Class<?> clazz, String methodName, String value) {
        int installed = 0;
        for (Method method : clazz.getDeclaredMethods()) {
            if (!methodName.equals(method.getName())
                    || !String.class.isAssignableFrom(method.getReturnType())) {
                continue;
            }
            try {
                hook(method)
                        .setId("toki-tiktok-region-" + clazz.getSimpleName()
                                + "-" + methodName + "-" + method.getParameterCount())
                        .intercept(chain -> value);
                installed++;
            } catch (Throwable error) {
                logError("Unable to hook " + clazz.getName() + "." + methodName, error);
            }
        }
        return installed;
    }
}
