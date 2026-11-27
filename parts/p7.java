
    private static void setCurrentImage(Object vm, Bitmap bmp) {
        String[] candidates = new String[]{"B", "o0"};
        for (String name : candidates) {
            try {
                for (Method m : vm.getClass().getDeclaredMethods()) {
                    if (!m.getName().equals(name)) continue;
                    m.setAccessible(true);
                    Class<?>[] ps = m.getParameterTypes();
                    if (ps.length == 1 && ps[0] == Bitmap.class) {
                        m.invoke(vm, bmp);
                        log("set image via " + name + "(Bitmap)");
                        return;
                    }
                    if (ps.length == 2 && ps[0] == Bitmap.class && ps[1] == boolean.class) {
                        m.invoke(vm, bmp, Boolean.TRUE);
                        log("set image via " + name + "(Bitmap,boolean)");
                        return;
                    }
                }
            } catch (Throwable t) {
                log("set image via " + name + " failed: " + t);
            }
        }
        log("WARN: no image setter found");
    }

    private static void invokeU(Object vm, Object prompt) throws Throwable {
        for (Method m : vm.getClass().getDeclaredMethods()) {
            if (!m.getName().equals("u")) continue;
            Class<?>[] ps = m.getParameterTypes();
            if (ps.length == 2) {
                m.setAccessible(true);
                m.invoke(null, vm, prompt);
                log("invoked u(VM,oy2)");
                return;
            }
        }
        throw new NoSuchMethodException("u(AiRetouchVM,oy2)");
    }
