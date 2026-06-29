/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : TProxyService.java
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-03 16:18:39
 * Description : Java JNI bridge for the prebuilt libhev-socks5-tunnel native library.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package hev.sockstun;

/**
 * Thin Java bridge for libhev-socks5-tunnel.
 *
 * The native library registers methods against hev.sockstun.TProxyService.
 * Keeping this bridge in Java avoids Kotlin object/class bytecode collisions.
 */
public final class TProxyService {

    private static final boolean LIBRARY_LOADED;

    static {
        boolean loaded;
        try {
            System.loadLibrary("hev-socks5-tunnel");
            loaded = true;
        } catch (Throwable ignored) {
            loaded = false;
        }
        LIBRARY_LOADED = loaded;
    }

    private TProxyService() {
    }

    /**
     * Returns whether the native tunnel library was loaded successfully.
     */
    public static boolean libraryLoaded() {
        return LIBRARY_LOADED;
    }

    public static native void TProxyStartService(String configPath, int fd);

    public static native void TProxyStopService();

    public static native long[] TProxyGetStats();
}
