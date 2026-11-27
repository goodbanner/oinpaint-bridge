
    private static void markProcessed(String name) {
        try {
            FileOutputStream fos = new FileOutputStream(new File(PROGRESS), true);
            try { fos.write((name + "\n").getBytes("UTF-8")); } finally { fos.close(); }
        } catch (Throwable t) {
            log("markProcessed failed: " + t);
        }
    }

    private static void saveBitmap(Bitmap bmp, File dst) {
        try {
            File dir = dst.getParentFile();
            if (dir != null) dir.mkdirs();
            FileOutputStream fos = new FileOutputStream(dst);
            try { bmp.compress(Bitmap.CompressFormat.JPEG, 95, fos); }
            finally { fos.close(); }
            log("SAVED " + dst.getAbsolutePath() + " (" + dst.length() + " bytes)");
        } catch (Throwable t) {
            log("SAVE_FAILED " + dst.getName() + ": " + t);
        }
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (Throwable ignored) {}
    }

    private static void sleepRandom(long minMs, long maxMs) {
        long d = minMs + (long) (RND.nextDouble() * (maxMs - minMs));
        log("cooldown " + (d / 1000) + "s");
        sleep(d);
    }

    private static void log(String s) {
        String line = "[OInpaintBridge] " + s;
        Log.i("OInpaintBridge", line);
        try {
            File f = new File(LOG);
            File parent = f.getParentFile();
            if (parent != null) parent.mkdirs();
            FileOutputStream fos = new FileOutputStream(f, true);
            try { fos.write((line + "\n").getBytes("UTF-8")); }
            finally { fos.close(); }
        } catch (Throwable ignored) {}
    }

    private static String stack(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
