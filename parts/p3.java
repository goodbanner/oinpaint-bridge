
    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET.equals(lpparam.packageName)) return;
        log("=== start ===");
        try {
            hookVm(lpparam.classLoader);
        } catch (Throwable t) {
            log("install failed: " + stack(t));
        }
    }
