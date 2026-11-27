
    private static void hookMethod(ClassLoader appCl, String className, String methodName,
                                   Class<?>[] params, XC_MethodHook callback) throws Throwable {
        Class<?> helpers = Class.forName("de.robv.android.xposed.XposedHelpers");
        Method target = null;
        for (Method m : helpers.getDeclaredMethods()) {
            if (!"findAndHookMethod".equals(m.getName())) continue;
            Class<?>[] ps = m.getParameterTypes();
            if (ps.length == 4 && ps[0] == String.class && ps[1] == ClassLoader.class
                    && ps[2] == String.class && ps[3].isArray()) {
                target = m;
                break;
            }
        }
        if (target == null) throw new NoSuchMethodException("findAndHookMethod");
        Object[] tail = new Object[params.length + 1];
        System.arraycopy(params, 0, tail, 0, params.length);
        tail[params.length] = callback;
        target.invoke(null, className, appCl, methodName, tail);
    }

    private static Object arg(MethodHookParam param, int i) {
        if (param.args == null) return null;
        return i < param.args.length ? param.args[i] : null;
    }

    private static String safeText(Object p) {
        if (p == null) return "";
        try {
            Method m = p.getClass().getMethod("getText");
            Object v = m.invoke(p);
            return v == null ? "" : String.valueOf(v);
        } catch (Throwable t) {
            return "";
        }
    }
