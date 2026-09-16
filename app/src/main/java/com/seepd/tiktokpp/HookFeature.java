package com.seepd.tiktokpp;

import android.content.SharedPreferences;
import android.util.Log;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Compatibility layer: wraps classic XposedBridge API with a builder pattern
 * similar to libxposed, so existing hook classes need minimal changes.
 */
abstract class HookFeature {
    private static final String TAG = "TikTokPP";

    protected final void logInfo(String message) {
        Log.i(TAG, message);
        XposedBridge.log(TAG + ": " + message);
    }

    protected final void logError(String message, Throwable error) {
        Log.e(TAG, message, error);
        XposedBridge.log(TAG + ": " + message);
    }

    protected final SharedPreferences getRemotePreferences(String name) {
        // Classic Xposed doesn't have remote preferences; return null
        return null;
    }

    /**
     * Returns a HookBuilder that wraps classic XposedBridge hooking.
     * Usage: hook(method).setId("name").intercept(chain -> { ... })
     */
    protected final HookBuilder hook(Executable executable) {
        return new HookBuilder(executable);
    }

    static final class HookBuilder {
        private final Executable executable;
        private String id;

        HookBuilder(Executable executable) {
            this.executable = executable;
        }

        public HookBuilder setId(String id) {
            this.id = id;
            return this;
        }

        public void intercept(Interceptor interceptor) {
            if (executable instanceof Method) {
                hookMethod((Method) executable, interceptor);
            } else {
                hookConstructor(interceptor);
            }
        }

        private void hookMethod(Method method, Interceptor interceptor) {
            Class<?> declaringClass = method.getDeclaringClass();
            Class<?>[] paramTypes = method.getParameterTypes();

            // Expand Class<?>[] into individual Object[] elements for varargs
            Object[] params = new Object[paramTypes.length + 1];
            for (int i = 0; i < paramTypes.length; i++) {
                params[i] = paramTypes[i];
            }
            params[paramTypes.length] = new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            ClassicChain chain = new ClassicChain(param, true);
                            Object result = interceptor.intercept(chain);
                            if (chain.isProceeded()) {
                                param.setResult(result);
                            }
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            // For interceptors that call proceed() in the after phase,
                            // the result is already set in beforeHookedMethod
                        }
                    };
            XposedHelpers.findAndHookMethod(declaringClass, method.getName(), params);
        }

        private void hookConstructor(Interceptor interceptor) {
            Class<?> declaringClass = executable.getDeclaringClass();
            Class<?>[] paramTypes = executable.getParameterTypes();

            // Expand Class<?>[] into individual Object[] elements for varargs
            Object[] params = new Object[paramTypes.length + 1];
            for (int i = 0; i < paramTypes.length; i++) {
                params[i] = paramTypes[i];
            }
            params[paramTypes.length] = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    ClassicChain chain = new ClassicChain(param, true);
                    Object result = interceptor.intercept(chain);
                    if (chain.isProceeded()) {
                        param.setResult(result);
                    }
                }
            };
            XposedHelpers.findAndHookConstructor(declaringClass, params);
        }
    }

    /**
     * Wraps MethodHookParam to provide a chain interface similar to libxposed.
     */
    static final class ClassicChain {
        private final XC_MethodHook.MethodHookParam param;
        private boolean proceeded;

        ClassicChain(XC_MethodHook.MethodHookParam param, boolean autoProceed) {
            this.param = param;
            this.proceeded = autoProceed;
        }

        public Object getThisObject() {
            return param.thisObject;
        }

        public Object getArg(int index) {
            Object[] args = param.args;
            if (args == null || index < 0 || index >= args.length) return null;
            return args[index];
        }

        public Object[] getArgs() {
            return param.args;
        }

        public Object proceed() throws Throwable {
            proceeded = true;
            return param.getResultOrThrowable();
        }

        public Object proceed(Object[] args) throws Throwable {
            param.args = args;
            proceeded = true;
            return param.getResultOrThrowable();
        }

        public boolean isProceeded() {
            return proceeded;
        }
    }

    @FunctionalInterface
    interface Interceptor {
        Object intercept(ClassicChain chain) throws Throwable;
    }
}
