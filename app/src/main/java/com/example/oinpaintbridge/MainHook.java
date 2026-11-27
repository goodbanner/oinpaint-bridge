package com.example.oinpaintbridge;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class MainHook implements IXposedHookLoadPackage {
    private static final String LOG = "/storage/emulated/0/Download/formatFile/video/oinpaint_module.log";
    private static final String OUT = "/storage/emulated/0/Download/formatFile/video/auto_results";
    private static final String PROMPT = "去除底部中央的彩色水印和残留字幕";
    private static final int AI_ENTRY_ID = 2131431135;
    private static volatile Object savedVM = null;
    private static volatile ClassLoader appCl = null;
    private static volatile boolean submitted = false;
    private static volatile long submitTime = 0;
    private static volatile Activity currentAct = null;
    private static volatile boolean downloadScheduled = false;
    private static volatile boolean downloadClicked = false;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        if (!"com.coloros.gallery3d".equals(lp.packageName)) return;
        appCl = lp.classLoader;
        log("=== FULL AUTO + SUBMIT start ===");
        try {
            // capture VM
            hookMethod(appCl, "com.oplus.gallery.photoeditor.editingvvm.airetouch.AiRetouchVM", "onResume", new Class<?>[]{}, new XC_MethodHook(){
                @Override protected void afterHookedMethod(MethodHookParam p){ savedVM = p.thisObject; submitted = false; downloadScheduled = false; downloadClicked = false; submitTime = 0; log("captured VM"); }
            });
            log("hooked VM.onResume");
            Class<?> vmClass = appCl.loadClass("com.oplus.gallery.photoeditor.editingvvm.airetouch.AiRetouchVM");
            Class<?> promptClass = appCl.loadClass("com.oplus.aiunit.vision.oy2");
            hookMethod(appCl, vmClass.getName(), "u", new Class<?>[]{vmClass, promptClass}, new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam p){
                    Object pr = p.args != null && p.args.length>1 ? p.args[1] : null;
                    String t = "";
                    try { if (pr!=null) { Method mt=pr.getClass().getMethod("getText"); Object v=mt.invoke(pr); t=String.valueOf(v); } } catch(Throwable e){}
                    log("u() called prompt=[" + t + "]");
                    if (!downloadScheduled) {
                        downloadScheduled = true;
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable(){ public void run(){ autoDownload(); } }, 16000);
                        log("download scheduled 16s after u()");
                    }
                }
            });
            log("hooked u");

            // auto click AI entry
            hookMethod(appCl, "android.app.Activity", "onResume", new Class<?>[]{}, new XC_MethodHook(){
                @Override protected void afterHookedMethod(MethodHookParam p){
                    final Activity act = (Activity) p.thisObject;
                    currentAct = act;
                    String n = act.getClass().getName();
                    if (!n.contains("PhotoEditor") && !n.contains("pictureeditor")) return;
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable(){ public void run(){ autoClick(act); } }, 1800);
                }
            });
            log("hooked Activity.onResume");

            // auto fill + auto submit
            Class<?> editCls = appCl.loadClass("com.oplus.gallery.photoeditor.editingvvm.airetouch.ui.input.AiRetouchEditText");
            hookMethod(appCl, editCls.getName(), "onAttachedToWindow", new Class<?>[]{}, new XC_MethodHook(){
                @Override protected void afterHookedMethod(MethodHookParam p){
                    try {
                        EditText et = (EditText) p.thisObject;
                        if (!PROMPT.equals(et.getText().toString())) { et.setText(PROMPT); et.setSelection(PROMPT.length()); log("auto-filled"); }
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable(){ public void run(){ autoSubmit(); } }, 5000);
                    } catch (Throwable t) { log("fill err " + t); }
                }
            });
            log("hooked AiRetouchEditText");

            // save result
            Class<?> tceCls = appCl.loadClass("com.oplus.aiunit.vision.tce");
            for (Method m : tceCls.getDeclaredMethods()) {
                if (m.getName().equals("a") && m.getParameterTypes().length == 1 && m.getReturnType() == Bitmap.class) {
                    final Class<?>[] ps = m.getParameterTypes();
                    hookMethod(appCl, tceCls.getName(), "a", ps, new XC_MethodHook(){
                        @Override protected void afterHookedMethod(MethodHookParam p){
                            Object r = p.result;
                            if (r instanceof Bitmap && submitTime > 0) {
                                Bitmap b=(Bitmap)r;
                                if(!b.isRecycled()&&b.getWidth()>=500){ try{ Bitmap cp=b.copy(b.getConfig()!=null?b.getConfig():Bitmap.Config.ARGB_8888,true); save(cp); }catch(Throwable t){} }
                            }
                        }
                    });
                    log("hooked tce.a");
                    break;
                }
            }
            // hook h0: capture result bitmap on preview update after submit
            hookMethod(appCl, vmClass.getName(), "h0", new Class<?>[]{int.class, Bitmap.class, boolean.class}, new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam p){
                    if (submitTime <= 0) return;
                    Object bm = p.args != null && p.args.length>1 ? p.args[1] : null;
                    if (bm instanceof Bitmap) {
                        Bitmap b = (Bitmap) bm;
                        if (!b.isRecycled() && b.getWidth() >= 500) {
                            try { Bitmap cp = b.copy(b.getConfig()!=null?b.getConfig():Bitmap.Config.ARGB_8888,true); save(cp); log("saved via h0"); }
                            catch (Throwable t) { log("h0 save err " + t); }
                        }
                    }
                }
            });
            log("hooked h0");
        } catch (Throwable t) { log("install failed " + stack(t)); }
    }

    private static void autoClick(Activity act) {
        try {
            View root = act.getWindow().getDecorView();
            View target = root.findViewById(AI_ENTRY_ID);
            if (target == null || target.getVisibility() != View.VISIBLE) { log("AI entry not ready"); new Handler(Looper.getMainLooper()).postDelayed(new Runnable(){ public void run(){ autoClick(act); } }, 1000); return; }
            currentAct = act;
            target.performClick();
            log("auto-clicked AI retouch entry");
        } catch (Throwable t) { log("autoClick err " + t); }
    }

    private static void autoSubmit() {
        try {
            if (savedVM == null || submitted) return;
            submitted = true;
            submitTime = System.currentTimeMillis();
            Class<?> promptCls = appCl.loadClass("com.oplus.aiunit.vision.oy2$c");
            Object prompt = allocInstance(promptCls);
            Field f = promptCls.getDeclaredField("a"); f.setAccessible(true); f.set(prompt, PROMPT);
            Object bus = getField(savedVM, "b");
            Class<?> busCls = appCl.loadClass("com.oplus.gallery.photoeditor.editingvvm.VVMBusExtKt");
            for (Method m : busCls.getDeclaredMethods()) {
                if ("g".equals(m.getName()) && m.getParameterTypes().length == 3 && m.getParameterTypes()[1] == String.class) {
                    m.invoke(null, bus, "AiRetouchVM/onRetouchPromptSend", new Object[]{prompt});
                    log("auto-submitted via VVMBus");
                    return;
                }
            }
            log("VVMBus.g not found");
        } catch (Throwable t) { log("autoSubmit err " + t); }
    }

    private static Object allocInstance(Class<?> cls) throws Throwable {
        Class<?> unsafeCls = Class.forName("sun.misc.Unsafe");
        Field f = unsafeCls.getDeclaredField("theUnsafe"); f.setAccessible(true);
        Object unsafe = f.get(null);
        Method m = unsafeCls.getMethod("allocateInstance", Class.class);
        return m.invoke(unsafe, cls);
    }

    private static Object getField(Object obj, String name) {
        Class<?> c = obj.getClass();
        while (c != null && !c.getName().startsWith("java.")) {
            try { Field f=c.getDeclaredField(name); f.setAccessible(true); return f.get(obj); }
            catch (NoSuchFieldException e){ c=c.getSuperclass(); } catch (Throwable t){ return null; }
        }
        return null;
    }

    private static void autoDownload() {
        try {
            if (downloadClicked) return;
            downloadClicked = true;
            if (currentAct == null) { log("no activity for download"); return; }
            View root = currentAct.getWindow().getDecorView();
            boolean clicked = findDownload(root);
            log("autoDownload clicked=" + clicked);
        } catch (Throwable t) { log("autoDownload err " + t); }
    }

    private static boolean findDownload(View v) {
        if (v == null) return false;
        try {
            int[] loc = new int[2];
            v.getLocationOnScreen(loc);
            if (v.isClickable() && loc[0] > 1050 && loc[0] < 1430 && loc[1] > 1450 && loc[1] < 1850) {
                v.performClick();
                log("clicked download-like view at " + loc[0] + "," + loc[1] + " " + v.getClass().getSimpleName());
                return true;
            }
        } catch (Throwable t) {}
        if (v instanceof android.view.ViewGroup) {
            android.view.ViewGroup g = (android.view.ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                if (findDownload(g.getChildAt(i))) return true;
            }
        }
        return false;
    }

    private static void save(Bitmap b) {
        try {
            File dir=new File(OUT); dir.mkdirs();
            File f=new File(dir,"result_"+System.currentTimeMillis()+".jpg");
            FileOutputStream fos=new FileOutputStream(f);
            try { b.compress(Bitmap.CompressFormat.JPEG,95,fos); } finally { fos.close(); }
            log("SAVED "+f.getName()+" "+f.length());
        } catch(Throwable t){ log("save err "+t); }
    }

    private static void hookMethod(ClassLoader cl, String cls, String name, Class<?>[] params, XC_MethodHook cb) throws Throwable {
        Class<?> h=Class.forName("de.robv.android.xposed.XposedHelpers"); Method target=null;
        for(Method m:h.getDeclaredMethods()){if(!"findAndHookMethod".equals(m.getName()))continue;Class<?>[] ps=m.getParameterTypes();if(ps.length==4&&ps[0]==String.class&&ps[1]==ClassLoader.class&&ps[2]==String.class&&ps[3].isArray()){target=m;break;}}
        Object[] tail=new Object[params.length+1];System.arraycopy(params,0,tail,0,params.length);tail[params.length]=cb;target.invoke(null,cls,cl,name,tail);
    }

    private static void log(String s){String line="[OInpaintBridge] "+s;Log.i("OInpaintBridge",line);try{File f=new File(LOG);File p=f.getParentFile();if(p!=null)p.mkdirs();FileOutputStream fos=new FileOutputStream(f,true);try{fos.write((line+"\n").getBytes("UTF-8"));}finally{fos.close();}}catch(Throwable ignored){}}
    private static String stack(Throwable t){StringWriter sw=new StringWriter();t.printStackTrace(new PrintWriter(sw));return sw.toString();}
}
