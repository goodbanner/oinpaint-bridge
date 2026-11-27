package com.example.oinpaintbridge;

import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class MainHook implements IXposedHookLoadPackage {
    private static final String LOG = "/storage/emulated/0/Download/formatFile/video/oinpaint_module.log";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!"com.coloros.gallery3d".equals(lpparam.packageName)) return;
        ClassLoader cl = lpparam.classLoader;
        log("=== probe ===");
        dumpClass(cl, "com.oplus.aiunit.vision.oy2$c");
        dumpClass(cl, "com.oplus.aiunit.vision.oy2");
        dumpClass(cl, "com.oplus.gallery.photoeditor.editingvvm.airetouch.vm.result.AiRetouchResultGalleryExporter");
    }

    private static void dumpClass(ClassLoader cl, String name) {
        try {
            Class<?> c = Class.forName(name, false, cl);
            log("CLASS " + name);
            for (Constructor<?> ct : c.getDeclaredConstructors()) {
                log("  CTOR " + java.util.Arrays.toString(ct.getParameterTypes()));
            }
            for (Field f : c.getDeclaredFields()) {
                log("  FIELD " + f.getType().getName() + " " + f.getName());
            }
            for (Method m : c.getDeclaredMethods()) {
                log("  METHOD " + m.getName() + java.util.Arrays.toString(m.getParameterTypes()) + " -> " + m.getReturnType().getName());
            }
        } catch (Throwable t) {
            log("dump " + name + " failed: " + t);
        }
    }

    private static void log(String s) {
        String line = "[OInpaintBridge] " + s;
        Log.i("OInpaintBridge", line);
        try {
            File f = new File(LOG);
            File p = f.getParentFile();
            if (p != null) p.mkdirs();
            FileOutputStream fos = new FileOutputStream(f, true);
            try { fos.write((line + "\n").getBytes("UTF-8")); } finally { fos.close(); }
        } catch (Throwable ignored) {}
    }
}
