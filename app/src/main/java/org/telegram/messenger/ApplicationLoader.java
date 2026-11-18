// EVE-SHIM: minimal ApplicationLoader foundation for Telegram screen ports
package org.telegram.messenger;

import android.annotation.SuppressLint;
import android.content.Context;

public class ApplicationLoader {
    @SuppressLint("StaticFieldLeak")
    public static volatile Context applicationContext;
}
