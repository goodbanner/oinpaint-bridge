
    private static void runBatch(final Object vm) {
        final List<File> todo = collectTodo();
        log("batch todo=" + todo.size());
        if (todo.isEmpty()) { log("nothing to do"); return; }

        new Thread(new Runnable() {
            @Override
            public void run() {
                int ok = 0, fail = 0, consecutiveFail = 0;
                for (File f : todo) {
                    if (consecutiveFail >= 3) {
                        log("STOP: 3 consecutive failures, 可能触发风控或接口变更");
                        break;
                    }
                    boolean done = processOne(vm, f);
                    if (done) {
                        ok++;
                        consecutiveFail = 0;
                        markProcessed(f.getName());
                    } else {
                        fail++;
                        consecutiveFail++;
                    }
                    log("progress ok=" + ok + " fail=" + fail + " consecutiveFail=" + consecutiveFail);
                    if (!done && consecutiveFail >= 3) break;
                    sleepRandom(12000, 18000);
                }
                log("BATCH DONE ok=" + ok + " fail=" + fail);
            }
        }, "oinpaint-batch").start();
    }

    private static List<File> collectTodo() {
        List<File> list = new ArrayList<File>();
        File dir = new File(IN);
        File[] fs = dir.listFiles();
        if (fs == null) return list;
        List<String> done = loadProcessed();
        for (File f : fs) {
            String n = f.getName().toLowerCase();
            if (!(n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png"))) continue;
            if (done.contains(f.getName())) continue;
            list.add(f);
        }
        Collections.sort(list, new Comparator<File>() {
            @Override
            public int compare(File a, File b) { return a.getName().compareTo(b.getName()); }
        });
        return list;
    }

    private static boolean processOne(Object vm, File f) {
        try {
            log(">> process " + f.getName());
            Bitmap src = BitmapFactory.decodeFile(f.getAbsolutePath());
            if (src == null) { log("decode failed " + f.getName()); return false; }

            setLatch();
            lastResult = null;

            Class<?> promptCls = vm.getClass().getClassLoader().loadClass("com.oplus.aiunit.vision.oy2$c");
            Object prompt = promptCls.getDeclaredConstructor().newInstance();
            java.lang.reflect.Field textField = promptCls.getDeclaredField("a");
            textField.setAccessible(true);
            textField.set(prompt, PROMPT);

            setCurrentImage(vm, src);
            sleep(800);

            invokeU(vm, prompt);

            CountDownLatch l = latch;
            boolean got = l != null && l.await(120, TimeUnit.SECONDS);
            if (!got) { log("timeout " + f.getName()); return false; }

            Bitmap out = lastResult;
            if (out == null) { log("no result " + f.getName()); return false; }
            saveBitmap(out, new File(OUT, f.getName()));
            return true;
        } catch (Throwable t) {
            log("processOne failed " + f.getName() + ": " + stack(t));
            return false;
        }
    }
