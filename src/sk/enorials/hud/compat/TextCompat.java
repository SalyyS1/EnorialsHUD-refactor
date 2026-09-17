/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.format.Style
 *  net.kyori.adventure.text.format.Style$Builder
 */
package sk.enorials.hud.compat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;

public final class TextCompat {
    private static final MethodHandle[] METHODS = TextCompat.methods();
    public static final boolean SHADOWS = METHODS != null;

    private TextCompat() {
    }

    private static MethodHandle[] methods() {
        try {
            Class<?> clazz = Class.forName("net.kyori.adventure.text.format.ShadowColor");
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            MethodHandle[] methodHandleArray = new MethodHandle[4];
            methodHandleArray[0] = lookup.unreflect(clazz.getMethod("shadowColor", Integer.TYPE)).asType(MethodType.methodType(Object.class, Integer.TYPE));
            Class[] classArray = new Class[]{Component.class, Style.class, Style.Builder.class};
            for (int i = 0; i < classArray.length; ++i) {
                for (Method method : classArray[i].getMethods()) {
                    if (!method.getName().equals("shadowColor") || method.getParameterCount() != 1 || !method.getParameterTypes()[0].isAssignableFrom(clazz)) continue;
                    methodHandleArray[i + 1] = lookup.unreflect(method).asType(MethodType.methodType(Object.class, Object.class, Object.class));
                    break;
                }
                if (methodHandleArray[i + 1] != null) continue;
                throw new IllegalStateException("No compatible Adventure shadow setter");
            }
            return methodHandleArray;
        }
        catch (ClassNotFoundException classNotFoundException) {
            return null;
        }
        catch (ReflectiveOperationException reflectiveOperationException) {
            throw new ExceptionInInitializerError(reflectiveOperationException);
        }
    }

    public static <T> T shadow(T t, int n) {
        int n2;
        if (!SHADOWS) {
            return t;
        }
        int n3 = t instanceof Component ? 1 : (t instanceof Style ? 2 : (n2 = t instanceof Style.Builder ? 3 : 0));
        if (n2 == 0) {
            throw new IllegalArgumentException("Unsupported text style: " + t.getClass().getName());
        }
        try {
            Object object = METHODS[0].invokeExact(n);
            return (T)METHODS[n2].invokeExact(t, object);
        }
        catch (Throwable throwable) {
            throw new IllegalStateException("Cannot apply text shadow", throwable);
        }
    }
}

