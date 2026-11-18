/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import android.util.Log;



import java.io.OutputStreamWriter;

public class FileLog {
    private OutputStreamWriter streamWriter = null;
    private DispatchQueue2 logQueue = null;

    public static boolean databaseIsMalformed = false;

    private final static String tag = "tmessages";

    private static volatile FileLog Instance = null;
    public static FileLog getInstance() {
        FileLog localInstance = Instance;
        if (localInstance == null) {
            synchronized (FileLog.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new FileLog();
                    if (BuildVars.LOGS_ENABLED) {
                        // no-op init // EVE-PATCH: wire to no-op
                    }
                }
            }
        }
        return localInstance;
    }

    private FileLog() {

    }



    public static void ensureInitied() {
        // no-op init // EVE-PATCH: wire to no-op
    }

    public static void e(final String message, final Throwable exception) {
        if (!BuildVars.LOGS_ENABLED) {
            return;
        }
        ensureInitied();
        Log.e(tag, message, exception);
    }

    public static void e(final String message) {
        if (!BuildVars.LOGS_ENABLED) {
            return;
        }
        ensureInitied();
        Log.e(tag, message);
    }

    public static void e(final Throwable e) {
        e(e, true);
    }

    public static void e(final Throwable e, boolean logToAppCenter) {
        if (!BuildVars.LOGS_ENABLED) {
            return;
        }
        ensureInitied();
        e.printStackTrace();
    }

    public static void fatal(final Throwable e) {
        fatal(e, true);
    }

    private static long dumpedHeap;

    public static void fatal(final Throwable e, boolean logToAppCenter) {
        if (!BuildVars.LOGS_ENABLED) {
            return;
        }
        ensureInitied();
        e.printStackTrace();
    }

    public static void d(final String message) {
        if (!BuildVars.LOGS_ENABLED) {
            return;
        }
        ensureInitied();
        Log.d(tag, message);
    }

    public static void w(final String message) {
        if (!BuildVars.LOGS_ENABLED) {
            return;
        }
        ensureInitied();
        Log.w(tag, message);
    }

    public static class IgnoreSentException extends Exception{

        public IgnoreSentException(String e) {
            super(e);
        }

    }
}
