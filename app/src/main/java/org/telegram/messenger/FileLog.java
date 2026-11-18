/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.Log;



import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashSet;

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


    private static volatile boolean gsonDisabled;

    private static HashSet<String> privateFields;

    public static class ByteArrayHexAdapter extends TypeAdapter<byte[]> {

        @Override
        public void write(JsonWriter out, byte[] value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }

            StringBuilder hex = new StringBuilder(2 + value.length * 2);
            hex.append("0x");
            for (byte b : value) {
                hex.append(String.format("%02x", b & 0xFF));
            }
            out.value(hex.toString());
        }

        @Override
        public byte[] read(JsonReader in) throws IOException {
            String hex = in.nextString();
            int len = hex.length();
            byte[] result = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                result[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i+1), 16));
            }
            return result;
        }
    }

    private static class TLObjectDeserializer implements JsonSerializer<TLObject> {
        @Override
        public JsonElement serialize(TLObject src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonObj = new JsonObject();
            String className = src.getClass().getName();
            final String usualPrefix = "org.telegram.tgnet.";
            if (className.startsWith(usualPrefix)) {
                className = className.substring(usualPrefix.length());
            }
            jsonObj.addProperty("_", className);
            try {
                Field[] fields = src.getClass().getFields();
                for (Field field : fields) {
                    if (privateFields != null && privateFields.contains(field.getName())) continue;
                    field.setAccessible(true);
                    try {
                        Object value = field.get(src);
                        // Native buffers can already have been returned to their pool.
                        if (value instanceof NativeByteBuffer) {
                            continue;
                        }
                        if (value != null) {
                            Class clazz = value.getClass();
                            if (clazz.isInstance(DispatchQueue.class) || clazz.isInstance(AnimatedFileDrawable.class) || clazz.isInstance(ColorStateList.class) || clazz.isInstance(Context.class)) {
                                continue;
                            }
                        }
                        JsonElement jsonElement = context.serialize(value);
                        jsonObj.add(field.getName(), jsonElement);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return jsonObj;
        }
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
        if (getInstance().streamWriter != null) {
            getInstance().logQueue.postRunnable(() -> {
                try {
                    getInstance().streamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " E/tmessages: " + message + "\n");
                    getInstance().streamWriter.write(exception.toString());
                    StackTraceElement[] stack = exception.getStackTrace();
                    for (int a = 0; a < stack.length; a++) {
                        getInstance().streamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " E/tmessages: \tat " + stack[a] + "\n");
                    }
                    getInstance().streamWriter.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public static void e(final String message) {
        if (!BuildVars.LOGS_ENABLED) {
            return;
        }
        ensureInitied();
        Log.e(tag, message);
        if (getInstance().streamWriter != null) {
            getInstance().logQueue.postRunnable(() -> {
                try {
                    getInstance().streamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " E/tmessages: " + message + "\n");
                    getInstance().streamWriter.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public static void e(final Throwable e) {
        e(e, true);
    }

    public static void fatal(final Throwable e) {
        fatal(e, true);
    }

    private static long dumpedHeap;

    public static void d(final String message) {
        if (!BuildVars.LOGS_ENABLED) {
            return;
        }
        ensureInitied();
        Log.d(tag, message);
        if (getInstance().streamWriter != null) {
            getInstance().logQueue.postRunnable(() -> {
                try {
                    getInstance().streamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " D/tmessages: " + message + "\n");
                    getInstance().streamWriter.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                    if (AndroidUtilities.isENOSPC(e)) {
                        LaunchActivity.checkFreeDiscSpaceStatic(1);
                    }
                }
            });
        }
    }

    public static void w(final String message) {
        if (!BuildVars.LOGS_ENABLED) {
            return;
        }
        ensureInitied();
        Log.w(tag, message);
        if (getInstance().streamWriter != null) {
            getInstance().logQueue.postRunnable(() -> {
                try {
                    getInstance().streamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " W/tmessages: " + message + "\n");
                    getInstance().streamWriter.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public static class IgnoreSentException extends Exception{

        public IgnoreSentException(String e) {
            super(e);
        }

    }
}
