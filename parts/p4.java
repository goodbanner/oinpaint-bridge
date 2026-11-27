
    private static void hookVm(ClassLoader cl) {
        try {
            Class<?> vmClass = cl.loadClass("com.oplus.gallery.photoeditor.editingvvm.airetouch.AiRetouchVM");
            Class<?> promptClass = cl.loadClass("com.oplus.aiunit.vision.oy2");

            hookMethod(cl, vmClass.getName(), "u",
                    new Class<?>[]{vmClass, promptClass},
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            Object vm = arg(param, 0);
                            Object p = arg(param, 1);
                            vmInstance = vm;
                            String text = safeText(p);
                            log("u() vm@" + System.identityHashCode(vm) + " prompt=[" + text + "]");
                        }
                    });
            log("hooked u");

            hookMethod(cl, vmClass.getName(), "h0",
                    new Class<?>[]{int.class, Bitmap.class, boolean.class},
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Object r = param.result;
                            if (r instanceof Bitmap) {
                                Bitmap b = (Bitmap) r;
                                lastResult = b;
                                CountDownLatch l = latch;
                                if (l != null && l.getCount() > 0) l.countDown();
                                log("h0 result " + b.getWidth() + "x" + b.getHeight());
                            }
                        }
                    });
            log("hooked h0");
        } catch (Throwable t) {
            log("hookVm failed: " + stack(t));
        }
    }
