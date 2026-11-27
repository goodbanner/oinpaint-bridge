    private static final String TARGET = "com.coloros.gallery3d";
    private static final String BASE = "/storage/emulated/0/Download/formatFile/video";
    private static final String LOG = BASE + "/oinpaint_module.log";
    private static final String IN = BASE + "/need_fix_frames";
    private static final String OUT = BASE + "/fixed_results";
    private static final String PROGRESS = BASE + "/processed.txt";
    private static final String PROMPT = "去除底部中央的彩色水印和残留字幕";
    private static final String TRIGGER = "OINPAINT_RUN_ALL";

    private static final Random RND = new Random();
    private static volatile Object vmInstance = null;
    private static volatile CountDownLatch latch = null;
    private static volatile Bitmap lastResult = nulloca;

    private static void setLatch() {
        latch = new CountDownLatch(1);
    }
