
    private static List<String> loadProcessed() {
        List<String> out = new ArrayList<String>();
        try {
            File f = new File(PROGRESS);
            if (!f.exists()) return out;
            byte[] buf = new byte[(int) f.length()];
            java.io.FileInputStream in = new java.io.FileInputStream(f);
            try { in.read(buf); } finally { in.close(); }
            String s = new String(buf, "UTF-8");
            for (String line : s.split("\n")) {
                String t = line.trim();
                if (t.length() > 0) out.add(t);
            }
        } catch (Throwable t) {
            log("loadProcessed failed: " + t);
        }
        return out;
    }
