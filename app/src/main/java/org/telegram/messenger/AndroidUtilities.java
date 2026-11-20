/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import static org.telegram.messenger.LocaleController.getString;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.app.PictureInPictureParams;
import android.content.ContentUris;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.CalendarContract;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.telephony.TelephonyManager;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.StateSet;
import android.view.Display;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.EdgeEffect;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.math.MathUtils;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;


import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.HideViewAfterAnimation;
import org.telegram.ui.Components.TypefaceSpan;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;


public class AndroidUtilities {
    public final static int REPLACING_TAG_TYPE_LINK = 0;
    public final static int REPLACING_TAG_TYPE_BOLD = 1;
    public final static int REPLACING_TAG_TYPE_LINKBOLD = 2;
    public final static int REPLACING_TAG_TYPE_LINK_NBSP = 3;
    public final static int REPLACING_TAG_TYPE_UNDERLINE = 4;

    public final static String TYPEFACE_ROBOTO_MEDIUM = "fonts/rmedium.ttf";
    public final static String TYPEFACE_ROBOTO_EXTRA_BOLD = "fonts/rextrabold.ttf";
    public final static String TYPEFACE_ROBOTO_MEDIUM_ITALIC = "fonts/rmediumitalic.ttf";
    public final static String TYPEFACE_ROBOTO_MONO = "fonts/rmono.ttf";
    public final static String TYPEFACE_MERRIWEATHER_BOLD = "fonts/mw_bold.ttf";

    public static Typeface mediumTypeface;
    public static ThreadLocal<byte[]> readBufferLocal = new ThreadLocal<>();
    public static ThreadLocal<byte[]> bufferLocal = new ThreadLocal<>();

    public static Typeface bold() {
        if (mediumTypeface == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // EVE-PATCH: decouple SharedConfig
                mediumTypeface = Typeface.create(null, 500, false);
            } else {
                mediumTypeface = getTypeface(TYPEFACE_ROBOTO_MEDIUM);
            }
        }
        return mediumTypeface;
    }

    private static final Hashtable<String, Typeface> typefaceCache = new Hashtable<>();
    public static float touchSlop;
    private static int prevOrientation = -10;
    private static int pendingUnlockOrientation = -10;
    private static int pendingLockedOrientation = -10;
    private static WeakReference<Activity> pendingUnlockActivity;
    private static boolean waitingForSms = false;
    private static boolean waitingForCall = false;
    private static final Object smsLock = new Object();
    private static final Object callLock = new Object();

    @Deprecated
    public static int statusBarHeight = 0;

    @Deprecated
    public static int navigationBarHeight = 0;

    public static boolean firstConfigurationWas;
    public static float density = 1;
    public static Point displaySize = new Point();
    public static float screenRefreshRate = 60;
    public static float screenMaxRefreshRate = 60;
    public static float screenRefreshTime = 1000 / screenRefreshRate;
    public static int roundMessageSize;
    public static int roundPlayingMessageSize;
    public static int roundSidePlayingMessageSize;
    public static int roundMessageInset;
    public static boolean incorrectDisplaySizeFix;
    public static Integer photoSize = null, highQualityPhotoSize = null;
    public static DisplayMetrics displayMetrics = new DisplayMetrics();
    public static int leftBaseline;
    public static boolean usingHardwareInput;
    public static boolean isInMultiwindow;

    public static int roundPlayingMessageSize(boolean withSidemenu, boolean compact) {
        if (withSidemenu) {
            return roundSidePlayingMessageSize;
        }
        // compact mode: a playing round video keeps the 16dp edge and a column for its side buttons
        return compact ? roundPlayingMessageSize - dp(16 + 40 - 28 / 2) : roundPlayingMessageSize;
    }

    public static DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator();
    public static AccelerateInterpolator accelerateInterpolator = new AccelerateInterpolator();
    public static OvershootInterpolator overshootInterpolator = new OvershootInterpolator();

    private static AccessibilityManager accessibilityManager;
    private static Vibrator vibrator;

    private static Boolean isTablet = null, wasTablet = null, isSmallScreen = null;
    private static int adjustOwnerClassGuid = 0;
    private static int altFocusableClassGuid = 0;

    public static final RectF rectTmp = new RectF();
    public static final Rect rectTmp2 = new Rect();
    public static final int[] pointTmp2 = new int[2];

    public static Pattern WEB_URL = null;
    public static Pattern BAD_CHARS_PATTERN = null;
    public static Pattern LONG_BAD_CHARS_PATTERN = null;
    public static Pattern BAD_CHARS_MESSAGE_PATTERN = null;
    public static Pattern BAD_CHARS_MESSAGE_LONG_PATTERN = null;
    public static Pattern REMOVE_MULTIPLE_DIACRITICS = null;
    public static Pattern REMOVE_RTL = null;
    private static Pattern singleTagPatter = null;

    public static final Paint strokeTop = new Paint(Paint.ANTI_ALIAS_FLAG);
    public static final Paint strokeBottom = new Paint(Paint.ANTI_ALIAS_FLAG);

    public static String removeDiacritics(String str) {
        if (str == null) return null;
        if (REMOVE_MULTIPLE_DIACRITICS == null) return str;
        Matcher matcher = REMOVE_MULTIPLE_DIACRITICS.matcher(str);
        if (matcher == null) return str;
        return matcher.replaceAll("$1");
    }

    public static String removeRTL(String str) {
        if (str == null) return null;
        if (REMOVE_RTL == null) {
            REMOVE_RTL = Pattern.compile("[\\u200E\\u200F\\u202A-\\u202E]");
        }
        Matcher matcher = REMOVE_RTL.matcher(str);
        if (matcher == null) return str;
        return matcher.replaceAll("");
    }

    public static String escape(String str) {
        return removeRTL(removeDiacritics(str));
    }

    static {
        try {
            final String GOOD_IRI_CHAR = "a-zA-Z0-9\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF";
            BAD_CHARS_PATTERN = Pattern.compile("[\u2500-\u25ff]");
            LONG_BAD_CHARS_PATTERN = Pattern.compile("[\u4e00-\u9fff]");
            BAD_CHARS_MESSAGE_LONG_PATTERN = Pattern.compile("[\u0300-\u036f\u2066-\u2067]");
            BAD_CHARS_MESSAGE_PATTERN = Pattern.compile("[\u2066-\u2067]+");
            REMOVE_MULTIPLE_DIACRITICS = Pattern.compile("([\\u0300-\\u036f]{1,2})[\\u0300-\\u036f]+");
            final Pattern IP_ADDRESS = Pattern.compile(
                    "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
                            + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
                            + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                            + "|[1-9][0-9]|[0-9]))");
            final String IRI = "[" + GOOD_IRI_CHAR + "]([" + GOOD_IRI_CHAR + "\\-]{0,61}[" + GOOD_IRI_CHAR + "]){0,1}";
            final String GOOD_GTLD_CHAR = "a-zA-Z\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF";
            final String GTLD = "[" + GOOD_GTLD_CHAR + "]{2,63}";
            final String HOST_NAME = "(" + IRI + "\\.)+" + GTLD;
            final Pattern DOMAIN_NAME = Pattern.compile("(" + HOST_NAME + "|" + IP_ADDRESS + ")");
            WEB_URL = Pattern.compile(
                    "((?:(http|https|Http|Https|ton|tg|tonsite):\\/\\/(?:(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)"
                            + "\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,64}(?:\\:(?:[a-zA-Z0-9\\$\\-\\_"
                            + "\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,25})?\\@)?)?"
                            + "(?:" + DOMAIN_NAME + ")"
                            + "(?:\\:\\d{1,5})?)" // plus option port number
                            + "(\\/(?:(?:[" + GOOD_IRI_CHAR + "\\;\\/\\?\\:\\@\\&\\=\\#\\~"  // plus option query params
                            + "\\-\\.\\+\\!\\*\\'\\(\\)\\,\\_])|(?:\\%[a-fA-F0-9]{2}))*)?"
                            + "(?:\\b|$)");
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    static {
        leftBaseline = isTablet() ? 80 : 72;
        checkDisplaySize(ApplicationLoader.applicationContext, null);
    }

    public static final String STICKERS_PLACEHOLDER_PACK_NAME = "tg_placeholders_android";
    public static final String STICKERS_PLACEHOLDER_PACK_NAME_2 = "tg_superplaceholders_android_2";

    private static boolean containsUnsupportedCharacters(String text) {
        if (text.contains("\u202C")) {
            return true;
        }
        if (text.contains("\u202D")) {
            return true;
        }
        if (text.contains("\u202E")) {
            return true;
        }
        try {
            if (BAD_CHARS_PATTERN.matcher(text).find()) {
                return true;
            }
        } catch (Throwable e) {
            return true;
        }
        return false;
    }

    public static String getSafeString(String str) {
        try {
            return BAD_CHARS_MESSAGE_PATTERN.matcher(str).replaceAll("\u200C");
        } catch (Throwable e) {
            return str;
        }
    }

    public static CharSequence ellipsizeCenterEnd(CharSequence str, String query, int availableWidth, TextPaint textPaint, int maxSymbols) {
        try {
            int lastIndex = str.length();
            int startHighlightedIndex = str.toString().toLowerCase().indexOf(query);

            if (lastIndex > maxSymbols) {
                str = str.subSequence(Math.max(0, startHighlightedIndex - maxSymbols / 2), Math.min(lastIndex, startHighlightedIndex + maxSymbols / 2));
                startHighlightedIndex -= Math.max(0, startHighlightedIndex - maxSymbols / 2);
                lastIndex = str.length();
            }
            StaticLayout staticLayout = new StaticLayout(str, textPaint, Integer.MAX_VALUE, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            float endOfTextX = staticLayout.getLineWidth(0);
            if (endOfTextX + textPaint.measureText("...") < availableWidth) {
                return str;
            }

            int i = startHighlightedIndex + 1;
            while (i < str.length() - 1 && !Character.isWhitespace(str.charAt(i))) {
                i++;
            }
            int endHighlightedIndex = i;

            float endOfHighlight = staticLayout.getPrimaryHorizontal(endHighlightedIndex);
            if (staticLayout.isRtlCharAt(endHighlightedIndex)) {
                endOfHighlight = endOfTextX - endOfHighlight;
            }
            if (endOfHighlight < availableWidth) {
                return str;
            }
            float x = endOfHighlight - availableWidth + textPaint.measureText("...") * 2 + availableWidth * 0.1f;
            if (str.length() - endHighlightedIndex > 20) {
                x += availableWidth * 0.1f;
            }

            if (x > 0) {
                int charOf = staticLayout.getOffsetForHorizontal(0, x);
                int k = 0;
                if (charOf > str.length() - 1) {
                    charOf = str.length() - 1;
                }
                while (!Character.isWhitespace(str.charAt(charOf)) && k < 10) {
                    k++;
                    charOf++;
                    if (charOf > str.length() - 1) {
                        charOf = staticLayout.getOffsetForHorizontal(0, x);
                        break;
                    }
                }
                CharSequence sub;
                if (k >= 10) {
                    x = staticLayout.getPrimaryHorizontal(startHighlightedIndex + 1) - availableWidth * 0.3f;
                    sub = str.subSequence(staticLayout.getOffsetForHorizontal(0, x), str.length());
                } else {
                    if (charOf > 0 && charOf < str.length() - 2 && Character.isWhitespace(str.charAt(charOf))) {
                        charOf++;
                    }
                    sub = str.subSequence(charOf, str.length());
                }
                return SpannableStringBuilder.valueOf("...").append(sub);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return str;
    }

    public static Activity getActivity() {
        return getActivity(null);
    }

    public static Activity getActivity(Context context) {
        Activity activity = findActivity(context);
        if (activity == null || activity.isFinishing()) activity = findActivity(ApplicationLoader.applicationContext); // EVE-PATCH: fallback to applicationContext activity lookup
        if (activity == null || activity.isFinishing()) activity = findActivity(ApplicationLoader.applicationContext);
        return activity;
    }

    public static Activity findActivity(Context context) {
        if (context instanceof Activity) {
            return (Activity) context;
        }
        if (context instanceof ContextWrapper) {
            return findActivity(((ContextWrapper) context).getBaseContext());
        }
        return null;
    }

    public static SpannableStringBuilder makeClickable(CharSequence str, int type, Runnable runnable, Theme.ResourcesProvider resourcesProvider) {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(str);
        if (type == REPLACING_TAG_TYPE_LINK || type == REPLACING_TAG_TYPE_LINK_NBSP || type == REPLACING_TAG_TYPE_LINKBOLD || type == REPLACING_TAG_TYPE_UNDERLINE) {
            spannableStringBuilder.setSpan(new ClickableSpan() {
                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(type == REPLACING_TAG_TYPE_UNDERLINE);
                    if (type == REPLACING_TAG_TYPE_LINKBOLD) {
                        ds.setTypeface(AndroidUtilities.bold());
                    }
                }
                @Override
                public void onClick(@NonNull View view) {
                    if (runnable != null) {
                        runnable.run();
                    }
                }
            }, 0, spannableStringBuilder.length(), 0);
        } else {
            spannableStringBuilder.setSpan(new CharacterStyle() {
                @Override
                public void updateDrawState(TextPaint textPaint) {
                    textPaint.setTypeface(AndroidUtilities.bold());
                    int wasAlpha = textPaint.getAlpha();
                    textPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText, resourcesProvider));
                    textPaint.setAlpha(wasAlpha);
                }
            }, 0, spannableStringBuilder.length(), 0);
        }
        return spannableStringBuilder;
    }

    public static SpannableStringBuilder makeClickable(CharSequence str, Runnable runnable) {
        return makeClickable(str, 0, runnable, null);
    }


    public static SpannableStringBuilder replaceMultipleTags(String str, Runnable ...runnables) {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(str);
        for (int i = 0; i < runnables.length; ++i) {
            Runnable runnable = runnables[i];

            int start = charSequenceIndexOf(spannableStringBuilder, "**");
            int end = charSequenceIndexOf(spannableStringBuilder, "**", start + 2);
            if (start < 0 || end < 0) break;

            spannableStringBuilder.delete(start, start + 2);
            end = end - 2;
            spannableStringBuilder.delete(end, end + 2);
            spannableStringBuilder.setSpan(new ClickableSpan() {
                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(false);
                }
                @Override
                public void onClick(@NonNull View widget) {
                    if (runnable != null) runnable.run();
                }
            }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannableStringBuilder;
    }

    public static SpannableStringBuilder replaceSingleLink(String str, int color) {
        return replaceSingleLink(str, color, null);
    }

    public static SpannableStringBuilder replaceSingleLink(String str, int color, Runnable onClick) {
        int startIndex = str.indexOf("**");
        int endIndex = str.indexOf("**", startIndex + 1);
        str = str.replace("**", "");
        int index = -1;
        int len = 0;
        if (startIndex >= 0 && endIndex >= 0 && endIndex - startIndex > 2) {
            len = endIndex - startIndex - 2;
            index = startIndex;
        }
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(str);
        if (index >= 0) {
            if (onClick != null) {
                spannableStringBuilder.setSpan(new ClickableSpan() {
                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                        super.updateDrawState(ds);
                        ds.setUnderlineText(false);
                        ds.setColor(color);
                    }

                    @Override
                    public void onClick(@NonNull View view) {
                        if (onClick != null) {
                            onClick.run();
                        }
                    }
                }, index, index + len, 0);
            } else {
                spannableStringBuilder.setSpan(new CharacterStyle() {
                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                        ds.setUnderlineText(false);
                        ds.setColor(color);
                    }
                }, index, index + len, 0);
            }
        }
        return spannableStringBuilder;
    }

    /** like replaceSingleLink(str, color, onClick), but the link color is read from the theme on every draw */
    public static SpannableStringBuilder replaceSingleLinkKey(String str, int colorKey, Theme.ResourcesProvider resourcesProvider, Runnable onClick) {
        return replaceSingleLinkKey(str, colorKey, resourcesProvider, onClick, false);
    }

    /** like replaceSingleLinkBold(str, color, onClick), but the link color is read from the theme on every draw */
    public static SpannableStringBuilder replaceSingleLinkBoldKey(String str, int colorKey, Theme.ResourcesProvider resourcesProvider, Runnable onClick) {
        return replaceSingleLinkKey(str, colorKey, resourcesProvider, onClick, true);
    }

    private static SpannableStringBuilder replaceSingleLinkKey(String str, int colorKey, Theme.ResourcesProvider resourcesProvider, Runnable onClick, boolean bold) {
        int startIndex = str.indexOf("**");
        int endIndex = str.indexOf("**", startIndex + 1);
        str = str.replace("**", "");
        int index = -1;
        int len = 0;
        if (startIndex >= 0 && endIndex >= 0 && endIndex - startIndex > 2) {
            len = endIndex - startIndex - 2;
            index = startIndex;
        }
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(str);
        if (index >= 0) {
            if (onClick != null) {
                spannableStringBuilder.setSpan(new ClickableSpan() {
                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                        super.updateDrawState(ds);
                        ds.setUnderlineText(false);
                        if (bold) {
                            ds.setTypeface(AndroidUtilities.bold());
                        }
                        ds.setColor(Theme.getColor(colorKey, resourcesProvider));
                    }

                    @Override
                    public void onClick(@NonNull View view) {
                        onClick.run();
                    }
                }, index, index + len, 0);
            } else {
                spannableStringBuilder.setSpan(new CharacterStyle() {
                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                        ds.setUnderlineText(false);
                        if (bold) {
                            ds.setTypeface(AndroidUtilities.bold());
                        }
                        ds.setColor(Theme.getColor(colorKey, resourcesProvider));
                    }
                }, index, index + len, 0);
            }
        }
        return spannableStringBuilder;
    }

    public static SpannableStringBuilder replaceSingleLinkBold(String str, int color) {
        return replaceSingleLinkBold(str, color, null);
    }

    public static SpannableStringBuilder replaceSingleLinkBold(String str, int color, Runnable onClick) {
        int startIndex = str.indexOf("**");
        int endIndex = str.indexOf("**", startIndex + 1);
        str = str.replace("**", "");
        int index = -1;
        int len = 0;
        if (startIndex >= 0 && endIndex >= 0 && endIndex - startIndex > 2) {
            len = endIndex - startIndex - 2;
            index = startIndex;
        }
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(str);
        if (index >= 0) {
            if (onClick != null) {
                spannableStringBuilder.setSpan(new ClickableSpan() {
                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                        super.updateDrawState(ds);
                        ds.setUnderlineText(false);
                        ds.setTypeface(AndroidUtilities.bold());
                        ds.setColor(color);
                    }

                    @Override
                    public void onClick(@NonNull View view) {
                        if (onClick != null) {
                            onClick.run();
                        }
                    }
                }, index, index + len, 0);
            } else {
                spannableStringBuilder.setSpan(new CharacterStyle() {
                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                        ds.setUnderlineText(false);
                        ds.setTypeface(AndroidUtilities.bold());
                        ds.setColor(color);
                    }
                }, index, index + len, 0);
            }
        }
        return spannableStringBuilder;
    }

    public static void recycleBitmaps(List<Bitmap> bitmapToRecycle) {
        if (Build.VERSION.SDK_INT <= 23) {
            // cause to crash:
            // /system/lib/libskia.so (SkPixelRef::unlockPixels()+3)
            // /system/lib/libskia.so (SkBitmap::freePixels()+14)
            // /system/lib/libskia.so (SkBitmap::setPixelRef(SkPixelRef*, int, int)+50)
            // /system/lib/libhwui.so (android::uirenderer::ResourceCache::recycleLocked(SkBitmap*)+30)
            // /system/lib/libhwui.so (android::uirenderer::ResourceCache::recycle(SkBitmap*)+20)
            // gc recycle it automatically
            return;
        }
        if (bitmapToRecycle != null && !bitmapToRecycle.isEmpty()) {
            ArrayList<WeakReference<Bitmap>> bitmapsToRecycleRef = new ArrayList<>();
            for (int i = 0; i < bitmapToRecycle.size(); i++) {
                Bitmap bitmap = bitmapToRecycle.get(i);
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmapsToRecycleRef.add(new WeakReference<>(bitmap));
                }
            }
            AndroidUtilities.runOnUIThread(() -> Utilities.globalQueue.postRunnable(() -> {
                for (int i = 0; i < bitmapsToRecycleRef.size(); i++) {
                    Bitmap bitmap = bitmapsToRecycleRef.get(i).get();
                    bitmapsToRecycleRef.get(i).clear();
                    if (bitmap != null && !bitmap.isRecycled()) {
                        try {
                            bitmap.recycle();
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                    }
                }
            }), 36);
        }
    }

    public static void recycleBitmap(Bitmap image) {
        recycleBitmaps(Collections.singletonList(image));
    }

    public static void removeFromParent(View child) {
        if (child != null && child.getParent() != null) {
            ((ViewGroup) child.getParent()).removeView(child);
        }
    }

    public static boolean isFilNotFoundException(Throwable e) {
        return e instanceof FileNotFoundException || e instanceof EOFException;
    }

    public static String formatVideoDurationFast(int minutes, int seconds) {
        StringBuilder stringBuilder = new StringBuilder();
        if (minutes >= 60) {
            normalizeTimePart(stringBuilder, minutes / 60);
            stringBuilder.append(":");
            normalizeTimePart(stringBuilder, minutes % 60);
            stringBuilder.append(":");
            normalizeTimePart(stringBuilder,seconds);
        } else {
            normalizeTimePart(stringBuilder, minutes);
            stringBuilder.append(":");
            normalizeTimePart(stringBuilder, seconds);
        }
        return stringBuilder.toString();
    }

    public static String formatTimerDurationFast(long seconds, int ms) {
        StringBuilder stringBuilder = new StringBuilder();
        long minutes = seconds / 60;
        if (minutes >= 60) {
            stringBuilder.append(minutes / 60).append(":");
            normalizeTimePart(stringBuilder, minutes % 60);
            stringBuilder.append(":");
            normalizeTimePart(stringBuilder, seconds % 60);
            stringBuilder.append("," ).append(ms / 10);
        } else {
            stringBuilder.append(minutes).append(":");
            normalizeTimePart(stringBuilder, seconds % 60);
            stringBuilder.append(",").append(ms / 10);
        }
        return stringBuilder.toString();
    }

    public static void normalizeTimePart(StringBuilder stringBuilder, int time) {
        if (time < 10) {
            stringBuilder
                    .append("0")
                    .append(time);
        } else {
            stringBuilder.append(time);
        }
    }

    public static void normalizeTimePart(StringBuilder stringBuilder, long time) {
        if (time < 10) {
            stringBuilder
                    .append("0")
                    .append(time);
        } else {
            stringBuilder.append(time);
        }
    }

    public static void getViewPositionInParent(View view, ViewGroup parent, float[] pointPosition) {
        pointPosition[0] = 0;
        pointPosition[1] = 0;
        if (view == null || parent == null) {
            return;
        }
        View currentView = view;
        while (currentView != parent) {
            //fix strange offset inside view pager
            if (!(currentView.getParent() instanceof ViewPager)) {
                pointPosition[0] += currentView.getX();
                pointPosition[1] += currentView.getY();
            }
            currentView = (View) currentView.getParent();
        }
    }

    public static MotionEvent emptyMotionEvent() {
        return MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0, 0, 0);
    }

    public static boolean isDarkColor(int color) {
        return AndroidUtilities.computePerceivedBrightness(color) < 0.721f;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void getBitmapFromSurface(SurfaceView surfaceView, Bitmap surfaceBitmap) {
        if (surfaceView == null || !surfaceView.getHolder().getSurface().isValid()) {
            return;
        }
        CountDownLatch countDownLatch = new CountDownLatch(1);
        PixelCopy.request(surfaceView, surfaceBitmap, copyResult -> {
            countDownLatch.countDown();
        }, Utilities.searchQueue.getHandlerSync());
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void getBitmapFromSurface(SurfaceView surfaceView, Bitmap surfaceBitmap, Runnable done) {
        if (surfaceView == null || ApplicationLoader.applicationHandler == null || !surfaceView.getHolder().getSurface().isValid()) {
            return;
        }
        PixelCopy.request(surfaceView, surfaceBitmap, copyResult -> {
            done.run();
        }, ApplicationLoader.applicationHandler);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void getBitmapFromSurface(Surface surface, Bitmap surfaceBitmap) {
        if (surface == null || !surface.isValid()) {
            return;
        }
        CountDownLatch countDownLatch = new CountDownLatch(1);
        PixelCopy.request(surface, surfaceBitmap, copyResult -> {
            countDownLatch.countDown();
        }, Utilities.searchQueue.getHandlerSync());
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static Bitmap getBitmapFromWindow(Window window) {
        if (window == null || window.getDecorView() == null) {
            return null;
        }

        final Bitmap bitmap = Bitmap.createBitmap(window.getDecorView().getWidth(), window.getDecorView().getHeight(), Bitmap.Config.ARGB_8888);
        final boolean[] success = new boolean[] { false };

        CountDownLatch countDownLatch = new CountDownLatch(1);
        PixelCopy.request(window, bitmap, copyResult -> {
            success[0] = copyResult == PixelCopy.SUCCESS;
            countDownLatch.countDown();
        }, Utilities.searchQueue.getHandlerSync());
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (success[0]) {
            return bitmap;
        } else {
            bitmap.recycle();
            return null;
        }
    }

    public static float[] getCoordinateInParent(ViewGroup parentView, View view) {
        float x = 0, y = 0;
        View child = view;
        float yOffset = 0;
        float xOffset = 0;
        if (child != null && parentView != null) {
            while (child != parentView) {
                if (child == null) {
                    xOffset = 0;
                    yOffset = 0;
                    break;
                }
                yOffset += child.getY();
                xOffset += child.getX();
                if (child instanceof NestedScrollView) {
                    yOffset -= child.getScrollY();
                    xOffset -= child.getScrollX();
                }
                if (child.getParent() instanceof View) {
                    child = (View) child.getParent();
                } else {
                    xOffset = 0;
                    yOffset = 0;
                    break;
                }
            }
        }
        return new float[] {xOffset, yOffset};
    }

    public static void doOnLayout(View view, Runnable runnable) {
        if (runnable == null) {
            return;
        }
        if (view == null) {
            runnable.run();
            return;
        }
        view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                view.removeOnLayoutChangeListener(this);
                runnable.run();
            }
        });
    }

    public static String readRes(int rawRes) {
        return readRes(null, rawRes);
    }

    public static String readRes(File path) {
        return readRes(path, 0);
    }

    public static String readRes(File path, int rawRes) {
        int totalRead = 0;
        byte[] readBuffer = readBufferLocal.get();
        if (readBuffer == null) {
            readBuffer = new byte[64 * 1024];
            readBufferLocal.set(readBuffer);
        }
        InputStream inputStream = null;
        try {
            if (path != null) {
                inputStream = new FileInputStream(path);
            } else {
                inputStream = ApplicationLoader.applicationContext.getResources().openRawResource(rawRes);
            }
            int readLen;
            byte[] buffer = bufferLocal.get();
            if (buffer == null) {
                buffer = new byte[4096];
                bufferLocal.set(buffer);
            }
            while ((readLen = inputStream.read(buffer, 0, buffer.length)) >= 0) {
                if (readBuffer.length < totalRead + readLen) {
                    byte[] newBuffer = new byte[readBuffer.length * 2];
                    System.arraycopy(readBuffer, 0, newBuffer, 0, totalRead);
                    readBuffer = newBuffer;
                    readBufferLocal.set(readBuffer);
                }
                if (readLen > 0) {
                    System.arraycopy(buffer, 0, readBuffer, totalRead, readLen);
                    totalRead += readLen;
                }
            }
        } catch (Throwable e) {
            return null;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Throwable ignore) {

            }
        }

        return new String(readBuffer, 0, totalRead);
    }

    @Nullable
    public static Bitmap getBitmapFromRaw(@RawRes int rawRes) {
        InputStream is = null;
        Bitmap bitmap = null;
        try {
            is = ApplicationLoader.applicationContext.getResources().openRawResource(rawRes);
            bitmap = BitmapFactory.decodeStream(is);
        } catch (Throwable e) {
            FileLog.e(e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {

            }
        }
        return bitmap;
    }

    private static class LinkSpec {
        String url;
        int start;
        int end;
    }

    private static String makeUrl(String url, String[] prefixes, Matcher matcher) {
        boolean hasPrefix = false;
        for (int i = 0; i < prefixes.length; i++) {
            if (url.regionMatches(true, 0, prefixes[i], 0, prefixes[i].length())) {
                hasPrefix = true;
                if (!url.regionMatches(false, 0, prefixes[i], 0, prefixes[i].length())) {
                    url = prefixes[i] + url.substring(prefixes[i].length());
                }
                break;
            }
        }
        if (!hasPrefix && prefixes.length > 0) {
            url = prefixes[0] + url;
        }
        return url;
    }

    public static final Linkify.MatchFilter sUrlMatchFilter = (s, start, end) -> {
        if (start == 0) {
            return true;
        }
        if (s.charAt(start - 1) == '@') {
            return false;
        }
        return true;
    };

    public static boolean doSafe(Utilities.Callback0Return<Boolean> runnable) {
        return doSafe(runnable, 200);
    }

    public static boolean doSafe(Utilities.Callback0Return<Boolean> runnable, int timeout) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<Boolean> task = () -> {
            try {
                return runnable.run();
            } catch (Exception e) {
                FileLog.e(e);
                return false;
            }
        };
        boolean success = false;
        Future<Boolean> future = null;
        try {
            future = executor.submit(task);
            success = future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            if (future != null) {
                future.cancel(true);
            }
        } catch (Exception ex) {
            FileLog.e(ex);
        } finally {
            executor.shutdownNow();
        }
        return success;
    }

    public static void fillStatusBarHeight(Context context, boolean force) {
        if (context == null || (AndroidUtilities.statusBarHeight > 0 && !force)) {
            return;
        }
    }

    public static int getStatusBarHeight(Context context) {
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        return resourceId > 0 ? context.getResources().getDimensionPixelSize(resourceId) : 0;
    }

    public static int calcBitmapColor(Bitmap bitmap) {
        if (bitmap == null) {
            return 0;
        }
        try {
            Bitmap b = Bitmap.createScaledBitmap(bitmap, 1, 1, true);
            if (b != null) {
                int bitmapColor = b.getPixel(0, 0);
                if (bitmap != b) {
                    b.recycle();
                }
                return bitmapColor;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return 0;
    }

    public static double[] rgbToHsv(int color) {
        return rgbToHsv(Color.red(color), Color.green(color), Color.blue(color));
    }

    public static double[] rgbToHsv(int r, int g, int b) {
        double rf = r / 255.0;
        double gf = g / 255.0;
        double bf = b / 255.0;
        double max = (rf > gf && rf > bf) ? rf : Math.max(gf, bf);
        double min = (rf < gf && rf < bf) ? rf : Math.min(gf, bf);
        double h, s;
        double d = max - min;
        s = max == 0 ? 0 : d / max;
        if (max == min) {
            h = 0;
        } else {
            if (rf > gf && rf > bf) {
                h = (gf - bf) / d + (gf < bf ? 6 : 0);
            } else if (gf > bf) {
                h = (bf - rf) / d + 2;
            } else {
                h = (rf - gf) / d + 4;
            }
            h /= 6;
        }
        return new double[]{h, s, max};
    }

    public static int hsvToColor(double h, double s, double v) {
        int[] rgb = hsvToRgb(h, s, v);
        return Color.argb(0xff, rgb[0], rgb[1], rgb[2]);
    }

    public static int[] hsvToRgb(double h, double s, double v) {
        double r = 0, g = 0, b = 0;
        double i = (int) Math.floor(h * 6);
        double f = h * 6 - i;
        double p = v * (1 - s);
        double q = v * (1 - f * s);
        double t = v * (1 - (1 - f) * s);
        switch ((int) i % 6) {
            case 0:
                r = v;
                g = t;
                b = p;
                break;
            case 1:
                r = q;
                g = v;
                b = p;
                break;
            case 2:
                r = p;
                g = v;
                b = t;
                break;
            case 3:
                r = p;
                g = q;
                b = v;
                break;
            case 4:
                r = t;
                g = p;
                b = v;
                break;
            case 5:
                r = v;
                g = p;
                b = q;
                break;
        }
        return new int[]{(int) (r * 255), (int) (g * 255), (int) (b * 255)};
    }

    public static void adjustSaturationColorMatrix(ColorMatrix colorMatrix, float saturation) {
        if (colorMatrix == null) {
            return;
        }
        float x = 1 + saturation;
        final float lumR = 0.3086f;
        final float lumG = 0.6094f;
        final float lumB = 0.0820f;

        colorMatrix.postConcat(new ColorMatrix(new float[]{
                lumR * (1 - x) + x, lumG * (1 - x), lumB * (1 - x), 0, 0,
                lumR * (1 - x), lumG * (1 - x) + x, lumB * (1 - x), 0, 0,
                lumR * (1 - x), lumG * (1 - x), lumB * (1 - x) + x, 0, 0,
                0, 0, 0, 1, 0
        }));
    }

    public static void adjustBrightnessColorMatrix(ColorMatrix colorMatrix, float brightness) {
        if (colorMatrix == null) {
            return;
        }
        brightness *= 255;
        colorMatrix.postConcat(new ColorMatrix(new float[]{
                1, 0, 0, 0, brightness,
                0, 1, 0, 0, brightness,
                0, 0, 1, 0, brightness,
                0, 0, 0, 1, 0
        }));
    }

    public static void adjustHueColorMatrix(ColorMatrix cm, float value) {
        value = cleanValue(value, 180f) / 180f * (float) Math.PI;
        if (value == 0) {
            return;
        }
        float cosVal = (float) Math.cos(value);
        float sinVal = (float) Math.sin(value);
        float lumR = 0.213f;
        float lumG = 0.715f;
        float lumB = 0.072f;
        float[] mat = new float[]
                {
                        lumR + cosVal * (1 - lumR) + sinVal * (-lumR), lumG + cosVal * (-lumG) + sinVal * (-lumG), lumB + cosVal * (-lumB) + sinVal * (1 - lumB), 0, 0,
                        lumR + cosVal * (-lumR) + sinVal * (0.143f), lumG + cosVal * (1 - lumG) + sinVal * (0.140f), lumB + cosVal * (-lumB) + sinVal * (-0.283f), 0, 0,
                        lumR + cosVal * (-lumR) + sinVal * (-(1 - lumR)), lumG + cosVal * (-lumG) + sinVal * (lumG), lumB + cosVal * (1 - lumB) + sinVal * (lumB), 0, 0,
                        0f, 0f, 0f, 1f, 0f,
                        0f, 0f, 0f, 0f, 1f};
        cm.postConcat(new ColorMatrix(mat));
    }

    protected static float cleanValue(float p_val, float p_limit) {
        return Math.min(p_limit, Math.max(-p_limit, p_val));
    }

    public static void multiplyBrightnessColorMatrix(ColorMatrix colorMatrix, float v) {
        if (colorMatrix == null) {
            return;
        }
        colorMatrix.postConcat(new ColorMatrix(new float[]{
                v, 0, 0, 0, 0,
                0, v, 0, 0, 0,
                0, 0, v, 0, 0,
                0, 0, 0, 1, 0
        }));
    }

    public static Bitmap snapshotView(View v) {
        Bitmap bm = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        v.draw(canvas);

        int[] loc = new int[2];
        v.getLocationInWindow(loc);
        snapshotTextureViews(loc[0], loc[1], loc, canvas, v);

        return bm;
    }

    private static void snapshotTextureViews(int rootX, int rootY, int[] loc, Canvas canvas, View v) {
        if (v instanceof TextureView) {
            TextureView tv = (TextureView) v;
            tv.getLocationInWindow(loc);

            Bitmap textureSnapshot = tv.getBitmap();
            if (textureSnapshot != null) {
                canvas.save();
                canvas.drawBitmap(textureSnapshot, loc[0] - rootX, loc[1] - rootY, null);
                canvas.restore();
                textureSnapshot.recycle();
            }
        }
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                snapshotTextureViews(rootX, rootY, loc, canvas, vg.getChildAt(i));
            }
        }
    }

    public static void requestAltFocusable(Activity activity, int classGuid) {
        if (activity == null) {
            return;
        }
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        altFocusableClassGuid = classGuid;
    }

    public static void removeAltFocusable(Activity activity, int classGuid) {
        if (activity == null) {
            return;
        }
        if (altFocusableClassGuid == classGuid) {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        }
    }

    public static void requestAdjustResize(Activity activity, int classGuid) {
        if (activity == null) {
            return;
        }
        requestAdjustResize(activity.getWindow(), classGuid);
    }

    public static void requestAdjustResize(Window window, int classGuid) {
        if (window == null || isTablet()) {
            return;
        }
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        adjustOwnerClassGuid = classGuid;
    }

    public static void requestAdjustNothing(Activity activity, int classGuid) {
        if (activity == null || isTablet()) {
            return;
        }
        activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        adjustOwnerClassGuid = classGuid;
    }

    public static void setAdjustResizeToNothing(Activity activity, int classGuid) {
        if (activity == null || isTablet()) {
            return;
        }
        if (adjustOwnerClassGuid == 0 || adjustOwnerClassGuid == classGuid) {
            activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        }
    }

    public static void removeAdjustResize(Activity activity, int classGuid) {
        if (activity == null || isTablet()) {
            return;
        }
        if (adjustOwnerClassGuid == classGuid) {
            activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        }
    }

    public static int[] toIntArray(List<Integer> integers) {
        int[] ret = new int[integers.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = integers.get(i);
        }
        return ret;
    }

    public static boolean isInternalUri(Uri uri) {
        return isInternalUri(uri, 0);
    }

    public static boolean isInternalUri(int fd) {
        return isInternalUri(null, fd);
    }

    private static boolean isInternalUri(Uri uri, int fd) {
        String pathString;
        if (uri != null) {
            pathString = uri.getPath();
            if (pathString == null) {
                return false;
            }
            // Allow sending VoIP logs from cache/voip_logs
            if (pathString.matches(Pattern.quote(new File(ApplicationLoader.applicationContext.getCacheDir(), "voip_logs").getAbsolutePath()) + "/\\d+\\.log")) {
                return false;
            }
            int tries = 0;
            while (true) {
                if (pathString != null && pathString.length() > 4096) {
                    return true;
                }
                String newPath;
                try {
                    newPath = Utilities.readlink(pathString);
                } catch (Throwable e) {
                    return true;
                }
                if (newPath == null || newPath.equals(pathString)) {
                    break;
                }
                pathString = newPath;
                tries++;
                if (tries >= 10) {
                    return true;
                }
            }
        } else {
            pathString = "";
            int tries = 0;
            while (true) {
                if (pathString != null && pathString.length() > 4096) {
                    return true;
                }
                String newPath;
                try {
                    newPath = Utilities.readlinkFd(fd);
                } catch (Throwable e) {
                    return true;
                }
                if (newPath == null || newPath.equals(pathString)) {
                    break;
                }
                pathString = newPath;
                tries++;
                if (tries >= 10) {
                    return true;
                }
            }
        }
        if (pathString != null) {
            try {
                String path = new File(pathString).getCanonicalPath();
                if (path != null) {
                    pathString = path;
                }
            } catch (Exception e) {
                pathString.replace("/./", "/");
                //igonre
            }
        }
        if (pathString.endsWith(".attheme")) {
            return false;
        }
        return pathString != null && pathString.toLowerCase().contains("/data/data/" + ApplicationLoader.applicationContext.getPackageName());
    }

    @SuppressLint("WrongConstant")
    public static void lockOrientation(Activity activity) {
        if (activity == null || prevOrientation != -10 || isTabletInternal()) {
            return;
        }
        try {
            prevOrientation = getPendingUnlockOrientation(activity);
            clearPendingOrientationUnlock();
            WindowManager manager = (WindowManager) activity.getSystemService(Activity.WINDOW_SERVICE);
            if (manager != null && manager.getDefaultDisplay() != null) {
                int rotation = manager.getDefaultDisplay().getRotation();
                int orientation = activity.getResources().getConfiguration().orientation;

                if (rotation == Surface.ROTATION_270) {
                    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    } else {
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                    }
                } else if (rotation == Surface.ROTATION_90) {
                    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                    } else {
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    }
                } else if (rotation == Surface.ROTATION_0) {
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    } else {
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    }
                } else {
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                    } else {
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                    }
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }


    @SuppressLint("WrongConstant")
    public static void lockOrientation(Activity activity, int orientation) {
        if (activity == null || isTabletInternal()) {
            return;
        }
        try {
            prevOrientation = getPendingUnlockOrientation(activity);
            clearPendingOrientationUnlock();
            activity.setRequestedOrientation(orientation);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    @SuppressLint("WrongConstant")
    public static void unlockOrientation(Activity activity) {
        if (activity == null) {
            return;
        }
        try {
            if (prevOrientation != -10) {
                final int lockedOrientation = activity.getRequestedOrientation();
                activity.setRequestedOrientation(prevOrientation);
                if (isLargeScreen(activity)) {
                    // a lock taken on a foldable's small screen: the large screen ignores the request, so apply it again on the next small one
                    pendingUnlockOrientation = prevOrientation;
                    pendingLockedOrientation = lockedOrientation;
                    pendingUnlockActivity = new WeakReference<>(activity);
                }
                prevOrientation = -10;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private static boolean isLargeScreen(Activity activity) {
        return activity.getResources().getConfiguration().smallestScreenWidthDp >= 600;
    }

    private static int getPendingUnlockOrientation(Activity activity) {
        if (pendingUnlockOrientation != -10 && pendingUnlockActivity != null && pendingUnlockActivity.get() == activity) {
            return pendingUnlockOrientation;
        }
        return activity.getRequestedOrientation();
    }

    // any other orientation request replaces the one the pending unlock would restore
    public static void clearPendingOrientationUnlock() {
        pendingUnlockOrientation = -10;
        pendingLockedOrientation = -10;
        pendingUnlockActivity = null;
    }

    @SuppressLint("WrongConstant") public static void applyPendingOrientationUnlock(Activity activity) { // EVE-PATCH: suppress wrong constant lint
        if (activity == null || pendingUnlockOrientation == -10 || prevOrientation != -10 || isLargeScreen(activity)) {
            return;
        }
        if (pendingUnlockActivity == null || pendingUnlockActivity.get() != activity) {
            return;
        }
        final int locked = pendingLockedOrientation;
        final int unlocked = pendingUnlockOrientation;
        clearPendingOrientationUnlock();
        try {
            // Activity skips a request equal to its last one, so restate the lock the system still holds first
            activity.setRequestedOrientation(locked);
            activity.setRequestedOrientation(unlocked);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static byte[] getStringBytes(String src) {
        try {
            return src.getBytes("UTF-8");
        } catch (Exception ignore) {

        }
        return new byte[0];
    }

    public static Typeface getTypeface(String assetPath) {
        if (ApplicationLoader.applicationContext == null) { return null; } synchronized (typefaceCache) { // EVE-PATCH: guard null ApplicationLoader.applicationContext in getTypeface
            if (!typefaceCache.containsKey(assetPath)) {
                try {
                    Typeface t;
                    if (Build.VERSION.SDK_INT >= 26) {
                        Typeface.Builder builder = new Typeface.Builder(ApplicationLoader.applicationContext.getAssets(), assetPath);
                        if (assetPath.contains("rextrabold")) {
                            builder.setWeight(800);
                        }
                        if (assetPath.contains("medium") || assetPath.contains("rbold")) {
                            builder.setWeight(700);
                        }
                        if (assetPath.contains("italic")) {
                            builder.setItalic(true);
                        }
                        t = builder.build();
                    } else {
                        t = Typeface.createFromAsset(ApplicationLoader.applicationContext.getAssets(), assetPath);
                    }
                    typefaceCache.put(assetPath, t);
                } catch (Exception e) {
                    if (BuildVars.LOGS_ENABLED) {
                        FileLog.e("Could not get typeface '" + assetPath + "' because " + e.getMessage());
                    }
                    return null;
                }
            }
            return typefaceCache.get(assetPath);
        }
    }

    public static boolean isWaitingForSms() {
        boolean value;
        synchronized (smsLock) {
            value = waitingForSms;
        }
        return value;
    }

    public static int getShadowHeight() {
        if (density >= 4.0f) {
            return 3;
        } else if (density >= 2.0f) {
            return 2;
        } else {
            return 1;
        }
    }

    public static boolean showKeyboard(View view) {
        if (view == null) {
            return false;
        }
        try {
            InputMethodManager inputManager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            return inputManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        } catch (Exception e) {
            FileLog.e(e);
        }
        return false;
    }

    public static String[] getCurrentKeyboardLanguage() {
        try {
            InputMethodManager inputManager = (InputMethodManager) ApplicationLoader.applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE);
            InputMethodSubtype inputMethodSubtype = inputManager.getCurrentInputMethodSubtype();
            String locale = null;
            if (inputMethodSubtype != null) {
                if (Build.VERSION.SDK_INT >= 24) {
                    locale = inputMethodSubtype.getLanguageTag();
                }
                if (TextUtils.isEmpty(locale)) {
                    locale = inputMethodSubtype.getLocale();
                }
            } else {
                inputMethodSubtype = inputManager.getLastInputMethodSubtype();
                if (inputMethodSubtype != null) {
                    if (Build.VERSION.SDK_INT >= 24) {
                        locale = inputMethodSubtype.getLanguageTag();
                    }
                    if (TextUtils.isEmpty(locale)) {
                        locale = inputMethodSubtype.getLocale();
                    }
                }
            }
            if (TextUtils.isEmpty(locale)) {
                locale = LocaleController.getSystemLocaleStringIso639();
                String locale2;
                LocaleController.LocaleInfo localeInfo = LocaleController.getInstance().getCurrentLocaleInfo();
                locale2 = localeInfo.getBaseLangCode();
                if (TextUtils.isEmpty(locale2)) {
                    locale2 = localeInfo.getLangCode();
                }
                if (locale.contains(locale2) || locale2.contains(locale)) {
                    if (!locale.contains("en")) {
                        locale2 = "en";
                    } else {
                        locale2 = null;
                    }
                }
                if (!TextUtils.isEmpty(locale2)) {
                    return new String[]{locale.replace('_', '-'), locale2};
                } else {
                    return new String[]{locale.replace('_', '-')};
                }
            } else {
                return new String[]{locale.replace('_', '-')};
            }
        } catch (Exception ignore) {}
        return new String[]{"en"};
    }

    public static void hideKeyboard(View view) {
        if (view == null) return;
        try {
            final InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (!imm.isActive()) return;
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static ArrayList<File> getDataDirs() {
        ArrayList<File> result = null;
        File[] dirs = ApplicationLoader.applicationContext.getExternalFilesDirs(null);
        if (dirs != null) {
            for (int a = 0; a < dirs.length; a++) {
                if (dirs[a] == null) {
                    continue;
                }
                String path = dirs[a].getAbsolutePath();

                if (result == null) {
                    result = new ArrayList<>();
                }
                result.add(dirs[a]);
            }
        }
        if (result == null) {
            result = new ArrayList<>();
        }
        if (result.isEmpty()) {
            result.add(Environment.getExternalStorageDirectory());
        }
        return result;
    }

    public static ArrayList<File> getRootDirs() {
        HashSet<String> pathes = new HashSet<>();
        ArrayList<File> result = null;
        File[] dirs = ApplicationLoader.applicationContext.getExternalFilesDirs(null);
        if (dirs != null) {
            for (int a = 0; a < dirs.length; a++) {
                if (dirs[a] == null) {
                    continue;
                }
                String path = dirs[a].getAbsolutePath();
                int idx = path.indexOf("/Android");
                if (idx >= 0) {
                    if (result == null) {
                        result = new ArrayList<>();
                    }
                    File file = new File(path.substring(0, idx));
                    for (int i = 0; i < result.size(); i++) {
                        if (result.get(i).getPath().equals(file.getPath())) {
                            continue;
                        }
                    }
                    if (file != null && !pathes.contains(file.getAbsolutePath())) {
                        pathes.add(file.getAbsolutePath());
                        result.add(file);
                    }
                }
            }
        }
        if (result == null) {
            result = new ArrayList<>();
        }
        if (result.isEmpty()) {
            File dir = Environment.getExternalStorageDirectory();
            if (dir != null && !pathes.contains(dir.getAbsolutePath())) {
                result.add(dir);
            }
        }
        return result;
    }

    public static File getCacheDir() {
        String state = null;
        try {
            state = Environment.getExternalStorageState();
        } catch (Exception e) {
            FileLog.e(e);
        }

        if (state == null || state.startsWith(Environment.MEDIA_MOUNTED)) {
            FileLog.d("external dir mounted");
            try {
                File file;

                File[] dirs = ApplicationLoader.applicationContext.getExternalCacheDirs();
                file = dirs[0];

                FileLog.d("check dir " + (file == null ? null : file.getPath()) + " ");
                if (file != null && (file.exists() || file.mkdirs()) && file.canWrite()) {
//                    boolean canWrite = true;
//                    try {
//                        AndroidUtilities.createEmptyFile(new File(file, ".nomedia"));
//                    } catch (Exception e) {
//                        canWrite = false;
//                    }
//                    if (canWrite) {
//                        return file;
//                    }
                    return file;
                } else if (file != null) {
                    FileLog.d("check dir file exist " + file.exists() + " can write " + file.canWrite());
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        try {
            File file = ApplicationLoader.applicationContext.getCacheDir();
            if (file != null) {
                return file;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        try {
            File file = ApplicationLoader.applicationContext.getFilesDir();
            if (file != null) {
                File cacheFile = new File(file, "cache/");
                cacheFile.mkdirs();
                if ((file.exists() || file.mkdirs()) && file.canWrite()) {
                    return cacheFile;
                }
            }
        } catch (Exception e) {

        }
        return new File("");
    }

    public static int dp(float value) {
        if (value == 0) {
            return 0;
        }
        return (int) Math.ceil(density * value);
    }

    public static int dpr(float value) {
        if (value == 0) {
            return 0;
        }
        return Math.round(density * value);
    }

    public static int dp2(float value) {
        if (value == 0) {
            return 0;
        }
        return (int) Math.floor(density * value);
    }

    public static int compare(int lhs, int rhs) {
        if (lhs == rhs) {
            return 0;
        } else if (lhs > rhs) {
            return 1;
        }
        return -1;
    }

    public static int compare(long lhs, long rhs) {
        if (lhs == rhs) {
            return 0;
        } else if (lhs > rhs) {
            return 1;
        }
        return -1;
    }

    public static float dpf2(float value) {
        if (value == 0) {
            return 0;
        }
        return density * value;
    }

    public static void checkDisplaySize(Context context, Configuration newConfiguration) {
        try {
            float oldDensity = density;
            density = context.getResources().getDisplayMetrics().density;
            float newDensity = density;
            if (firstConfigurationWas && Math.abs(oldDensity - newDensity) > 0.001) {
            }
            firstConfigurationWas = true;
            Configuration configuration = newConfiguration;
            if (configuration == null) {
                configuration = context.getResources().getConfiguration();
            }
            // usingHardwareInput = false; just for test
            usingHardwareInput = configuration.keyboard != Configuration.KEYBOARD_NOKEYS && configuration.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO;
            WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (manager != null) {
                Display display = manager.getDefaultDisplay();
                if (display != null) {
                    display.getMetrics(displayMetrics);
                    display.getSize(displaySize);
                    screenRefreshRate = display.getRefreshRate();
                    screenMaxRefreshRate = screenRefreshRate;
                    float[] rates = display.getSupportedRefreshRates();
                    if (rates != null) {
                        for (int i = 0; i < rates.length; ++i) {
                            if (rates[i] > screenMaxRefreshRate) {
                                screenMaxRefreshRate = rates[i];
                            }
                        }
                    }
                    screenRefreshTime = 1000 / screenRefreshRate;
                }
            }
            if (configuration.screenWidthDp != Configuration.SCREEN_WIDTH_DP_UNDEFINED) {
                int newSize = (int) Math.ceil(configuration.screenWidthDp * density);
                if (Math.abs(displaySize.x - newSize) > 3) {
                    displaySize.x = newSize;
                }
            }
            if (configuration.screenHeightDp != Configuration.SCREEN_HEIGHT_DP_UNDEFINED) {
                int newSize = (int) Math.ceil(configuration.screenHeightDp * density);
                if (Math.abs(displaySize.y - newSize) > 3) {
                    displaySize.y = newSize;
                }
            }
            if (roundMessageSize == 0) {
                if (AndroidUtilities.isTablet()) {
                    roundMessageSize = (int) (getMinTabletSide() * 0.6f);
                    roundPlayingMessageSize = (int) (getMinTabletSide() - dp(28));
                    roundSidePlayingMessageSize = (int) (getMinTabletSide() - dp(28 + 64));
                } else {
                    roundMessageSize = (int) (Math.min(displaySize.x, displaySize.y) * 0.6f);
                    roundPlayingMessageSize = (int) (Math.min(displaySize.x, displaySize.y) - dp(28));
                    roundSidePlayingMessageSize = (int) (Math.min(displaySize.x - dp(64), displaySize.y) - dp(28));
                }
                roundMessageInset = dp(2);
            }
            fillStatusBarHeight(context, true);
            if (BuildVars.LOGS_ENABLED) {
                FileLog.e("density = " + density + " display size = " + displaySize.x + " " + displaySize.y + " " + displayMetrics.xdpi + "x" + displayMetrics.ydpi + ", screen layout: " + configuration.screenLayout + ", statusbar height: " + statusBarHeight + ", navbar height: " + navigationBarHeight);
            }
            ViewConfiguration vc = ViewConfiguration.get(context);
            touchSlop = vc.getScaledTouchSlop();
            isSmallScreen = null;
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static void setPreferredMaxRefreshRate(Window window) {
        setPreferredMaxRefreshRate(window, screenMaxRefreshRate);
    }

    public static void setPreferredMaxRefreshRate(Window window, float rate) {
        if (window == null) return;
        final WindowManager wm = window.getWindowManager();
        if (wm == null) return;
        WindowManager.LayoutParams params = window.getAttributes();
        params.preferredRefreshRate = rate;
        try {
            wm.updateViewLayout(window.getDecorView(), params);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static void setPreferredMaxRefreshRate(WindowManager wm, View windowView, WindowManager.LayoutParams params) {
        if (wm == null) return;
        if (Math.abs(params.preferredRefreshRate - screenMaxRefreshRate) > 0.2) {
            params.preferredRefreshRate = screenMaxRefreshRate;
            if (windowView.isAttachedToWindow()) {
                try {
                    wm.updateViewLayout(windowView, params);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
        }
    }

    public static double fixLocationCoord(double value) {
        return ((long) (value * 1000000)) / 1000000.0;
    }

    public static float getPixelsInCM(float cm, boolean isX) {
        return (cm / 2.54f) * (isX ? displayMetrics.xdpi : displayMetrics.ydpi);
    }

    public static int getMyLayerVersion(int layer) {
        return layer & 0xffff;
    }

    public static int getPeerLayerVersion(int layer) {
        return Math.max(73, (layer >> 16) & 0xffff);
    }

    public static int setMyLayerVersion(int layer, int version) {
        return layer & 0xffff0000 | version;
    }

    public static int setPeerLayerVersion(int layer, int version) {
        return layer & 0x0000ffff | (version << 16);
    }

    public static void executeOnUIThread(Runnable runnable) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            AndroidUtilities.runOnUIThread(runnable);
            return;
        }

        runnable.run();
    }

    public static void runOnUIThread(Runnable runnable) {
        runOnUIThread(runnable, 0);
    }

    public static void runOnUIThread(Runnable runnable, long delay) {
        if (ApplicationLoader.applicationHandler == null) {
            return;
        }
        if (delay == 0) {
            ApplicationLoader.applicationHandler.post(runnable);
        } else {
            ApplicationLoader.applicationHandler.postDelayed(runnable, delay);
        }
    }

    public static void cancelRunOnUIThread(Runnable runnable) {
        if (ApplicationLoader.applicationHandler == null) {
            return;
        }
        ApplicationLoader.applicationHandler.removeCallbacks(runnable);
    }

    public static boolean isValidWallChar(char ch) {
        return ch == '-' || ch == '~';
    }

    public static boolean isTabletForce() {
        return false; // EVE-PATCH: phone default for tablet
    }

    public static boolean isTabletInternal() {
        if (isTablet == null) {
            isTablet = isTabletForce();
        }
        return isTablet;
    }

    public static void resetTabletFlag() {
        if (wasTablet == null) {
            wasTablet = isTabletInternal();
        }
        isTablet = null;
    }

    public static void resetWasTabletFlag() {
        wasTablet = null;
    }

    public static Boolean getWasTablet() {
        return wasTablet;
    }

    public static boolean isTablet() {
        return isTabletInternal(); // EVE-PATCH: decouple SharedConfig
    }

    public static boolean isFold() {
        return (
            ApplicationLoader.applicationContext != null &&
            ApplicationLoader.applicationContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_HINGE_ANGLE)
        );
    }

    public static boolean isSmallScreen() {
        if (isSmallScreen == null) {
            isSmallScreen = (Math.max(displaySize.x, displaySize.y) - statusBarHeight - navigationBarHeight) / density <= 650;
        }
        return isSmallScreen;
    }

    public static boolean isSmallTablet() {
        float minSide = Math.min(displaySize.x, displaySize.y) / density;
        return minSide <= 690;
    }

    public static int getMinTabletSide() {
        if (!isSmallTablet()) {
            final int smallSide = Math.min(displaySize.x, displaySize.y);
            final int leftSide = getTabletLeftFragmentSize(smallSide, 0, 0);
            return smallSide - leftSide;
        } else {
            final int smallSide = Math.min(displaySize.x, displaySize.y);
            final int maxSide = Math.max(displaySize.x, displaySize.y);
            final int leftSide = getTabletLeftFragmentSize(maxSide, 0, 0);
            return Math.min(smallSide, maxSide - leftSide);
        }
    }

    public static int getTabletLeftFragmentSize(final int fullWidth, final int insetLeft, final int insetRight) {
        return insetLeft + Math.max(dp(320), (fullWidth - insetLeft - insetRight) * 35 / 100);
    }

    public static int getPhotoSize() {
        return getPhotoSize(false);
    }

    public static int getPhotoSize(boolean highQuality) {
        if (highQuality) {
            if (highQualityPhotoSize == null) {
                highQualityPhotoSize = 2560;
            }
            return highQualityPhotoSize;
        } else {
            if (photoSize == null) {
                photoSize = 1280;
            }
            return photoSize;
        }
    }

    /*public static void clearCursorDrawable(EditText editText) {
        if (editText == null) {
            return;
        }
        try {
            Field mCursorDrawableRes = TextView.class.getDeclaredField("mCursorDrawableRes");
            mCursorDrawableRes.setAccessible(true);
            mCursorDrawableRes.setInt(editText, 0);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }*/

    private static Runnable unregisterRunnable;

    public static int getViewInset(View view) {
        if (view == null || view.getHeight() == AndroidUtilities.displaySize.y || view.getHeight() == AndroidUtilities.displaySize.y - statusBarHeight) {
            return 0;
        }
        try {
            WindowInsets insets = view.getRootWindowInsets();
            return insets != null ? insets.getStableInsetBottom() : 0;
        } catch (Exception e) {
            FileLog.e(e);
        }
        return 0;
    }

    public static Point getRealScreenSize() {
        Point size = new Point();
        try {
            WindowManager windowManager = (WindowManager) ApplicationLoader.applicationContext.getSystemService(Context.WINDOW_SERVICE);
            windowManager.getDefaultDisplay().getRealSize(size);
        } catch (Exception e) {
            FileLog.e(e);
        }
        return size;
    }

    public static void setEnabled(View view, boolean enabled) {
        if (view == null) {
            return;
        }
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                setEnabled(viewGroup.getChildAt(i), enabled);
            }
        }
    }

    public static int charSequenceIndexOf(CharSequence cs, CharSequence needle, int fromIndex) {
        if (needle == null || needle.length() <= 0) return -1;
        for (int i = fromIndex; i <= cs.length() - needle.length(); i++) {
            boolean eq = true;
            for (int j = 0; j < needle.length(); j++) {
                if (needle.charAt(j) != cs.charAt(i + j)) {
                    eq = false;
                    break;
                }
            }
            if (eq)
                return i;
        }
        return -1;
    }

    public static int charSequenceIndexOf(CharSequence cs, CharSequence needle) {
        return charSequenceIndexOf(cs, needle, 0);
    }

    public static boolean charSequenceContains(CharSequence cs, CharSequence needle) {
        return charSequenceIndexOf(cs, needle) != -1;
    }

    public static CharSequence getTrimmedString(CharSequence src) {
        if (src == null || src.length() == 0) {
            return src;
        }
        while (src.length() > 0 && (src.charAt(0) == '\n' || src.charAt(0) == ' ')) {
            src = src.subSequence(1, src.length());
        }
        while (src.length() > 0 && (src.charAt(src.length() - 1) == '\n' || src.charAt(src.length() - 1) == ' ')) {
            src = src.subSequence(0, src.length() - 1);
        }
        return src;
    }

    public static void setViewPagerEdgeEffectColor(ViewPager viewPager, int color) {
        try {
            Field field = ViewPager.class.getDeclaredField("mLeftEdge");
            field.setAccessible(true);
            EdgeEffect mLeftEdge = (EdgeEffect) field.get(viewPager);
            if (mLeftEdge != null) {
                mLeftEdge.setColor(color);
            }

            field = ViewPager.class.getDeclaredField("mRightEdge");
            field.setAccessible(true);
            EdgeEffect mRightEdge = (EdgeEffect) field.get(viewPager);
            if (mRightEdge != null) {
                mRightEdge.setColor(color);
            }
        } catch (Exception ignore) {

        }
    }

    public static void setScrollViewEdgeEffectColor(HorizontalScrollView scrollView, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            scrollView.setEdgeEffectColor(color);
        } else {
            try {
                Field field = HorizontalScrollView.class.getDeclaredField("mEdgeGlowLeft");
                field.setAccessible(true);
                EdgeEffect mEdgeGlowTop = (EdgeEffect) field.get(scrollView);
                if (mEdgeGlowTop != null) {
                    mEdgeGlowTop.setColor(color);
                }

                field = HorizontalScrollView.class.getDeclaredField("mEdgeGlowRight");
                field.setAccessible(true);
                EdgeEffect mEdgeGlowBottom = (EdgeEffect) field.get(scrollView);
                if (mEdgeGlowBottom != null) {
                    mEdgeGlowBottom.setColor(color);
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
    }

    public static void setScrollViewEdgeEffectColor(ScrollView scrollView, int color) {
        if (Build.VERSION.SDK_INT >= 29) {
            scrollView.setTopEdgeEffectColor(color);
            scrollView.setBottomEdgeEffectColor(color);
        } else {
            try {
                Field field = ScrollView.class.getDeclaredField("mEdgeGlowTop");
                field.setAccessible(true);
                EdgeEffect mEdgeGlowTop = (EdgeEffect) field.get(scrollView);
                if (mEdgeGlowTop != null) {
                    mEdgeGlowTop.setColor(color);
                }

                field = ScrollView.class.getDeclaredField("mEdgeGlowBottom");
                field.setAccessible(true);
                EdgeEffect mEdgeGlowBottom = (EdgeEffect) field.get(scrollView);
                if (mEdgeGlowBottom != null) {
                    mEdgeGlowBottom.setColor(color);
                }
            } catch (Exception ignore) {

            }
        }
    }

    public static void clearDrawableAnimation(View view) {
        if (view == null) {
            return;
        }
        Drawable drawable;
        if (view instanceof ListView) {
            drawable = ((ListView) view).getSelector();
            if (drawable != null) {
                drawable.setState(StateSet.NOTHING);
            }
        } else {
            drawable = view.getBackground();
            if (drawable != null) {
                drawable.setState(StateSet.NOTHING);
                drawable.jumpToCurrentState();
            }
        }
    }

    public static final int FLAG_TAG_BR = 1;
    public static final int FLAG_TAG_BOLD = 2;
    public static final int FLAG_TAG_COLOR = 4;
    public static final int FLAG_TAG_URL = 8;
    public static final int FLAG_TAG_ALL = FLAG_TAG_BR | FLAG_TAG_BOLD | FLAG_TAG_URL;

    public static SpannableStringBuilder replaceTags(String str) {
        return replaceTags(str, FLAG_TAG_ALL);
    }

    public static SpannableStringBuilder replaceTags(String str, int flag, Object... args) {
        try {
            int start;
            int end;
            StringBuilder stringBuilder = new StringBuilder(str);
            if ((flag & FLAG_TAG_BR) != 0) {
                while ((start = stringBuilder.indexOf("<br>")) != -1) {
                    stringBuilder.replace(start, start + 4, "\n");
                }
                while ((start = stringBuilder.indexOf("<br/>")) != -1) {
                    stringBuilder.replace(start, start + 5, "\n");
                }
            }
            ArrayList<Integer> bolds = new ArrayList<>();
            if ((flag & FLAG_TAG_BOLD) != 0) {
                while ((start = stringBuilder.indexOf("<b>")) != -1) {
                    stringBuilder.replace(start, start + 3, "");
                    end = stringBuilder.indexOf("</b>");
                    if (end == -1) {
                        end = stringBuilder.indexOf("<b>");
                    }
                    stringBuilder.replace(end, end + 4, "");
                    bolds.add(start);
                    bolds.add(end);
                }
                while ((start = stringBuilder.indexOf("**")) != -1) {
                    stringBuilder.replace(start, start + 2, "");
                    end = stringBuilder.indexOf("**");
                    if (end >= 0) {
                        stringBuilder.replace(end, end + 2, "");
                        bolds.add(start);
                        bolds.add(end);
                    }
                }
            }
            if ((flag & FLAG_TAG_URL) != 0) {
                while ((start = stringBuilder.indexOf("**")) != -1) {
                    stringBuilder.replace(start, start + 2, "");
                    end = stringBuilder.indexOf("**");
                    if (end >= 0) {
                        stringBuilder.replace(end, end + 2, "");
                        bolds.add(start);
                        bolds.add(end);
                    }
                }
            }
            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(stringBuilder);
            for (int a = 0; a < bolds.size() / 2; a++) {
                spannableStringBuilder.setSpan(new TypefaceSpan(AndroidUtilities.bold()), bolds.get(a * 2), bolds.get(a * 2 + 1), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            return spannableStringBuilder;
        } catch (Exception e) {
            FileLog.e(e);
        }
        return new SpannableStringBuilder(str);
    }

    public static CharSequence replaceTags(CharSequence cs) {
        if (cs instanceof SpannableStringBuilder) {
            return replaceTags((SpannableStringBuilder) cs);
        } else {
            return replaceTags(new SpannableStringBuilder(cs));
        }
    }

    public static SpannableStringBuilder replaceTags(SpannableStringBuilder stringBuilder) {
        try {
            int start;
            int end;
            ArrayList<Integer> bolds = new ArrayList<>();
            while ((start = AndroidUtilities.charSequenceIndexOf(stringBuilder, "**")) != -1) {
                stringBuilder.replace(start, start + 2, "");
                end = AndroidUtilities.charSequenceIndexOf(stringBuilder, "**");
                if (end >= 0) {
                    stringBuilder.replace(end, end + 2, "");
                    bolds.add(start);
                    bolds.add(end);
                }
            }
            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(stringBuilder);
            for (int a = 0; a < bolds.size() / 2; a++) {
                spannableStringBuilder.setSpan(new TypefaceSpan(AndroidUtilities.bold()), bolds.get(a * 2), bolds.get(a * 2 + 1), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            return spannableStringBuilder;
        } catch (Exception e) {
            FileLog.e(e);
        }
        return stringBuilder;
    }

    private static Pattern linksPattern;

    public static class LinkMovementMethodMy extends LinkMovementMethod {
        @Override
        public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
            try {
                boolean result = super.onTouchEvent(widget, buffer, event);
                if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    Selection.removeSelection(buffer);
                }
                return result;
            } catch (Exception e) {
                FileLog.e(e);
            }
            return false;
        }
    }

    public static void shakeView(final View view) {
        if (view == null) {
            return;
        }
        final float N = 4;
        Object animator = view.getTag(R.id.shake_animation);
        if (animator instanceof ValueAnimator) {
            ((ValueAnimator) animator).cancel();
        }
        ValueAnimator va = ValueAnimator.ofFloat(0, 1);
        va.addUpdateListener(anm -> {
            float x = (float) anm.getAnimatedValue();
            view.setTranslationX((float) ((4 * x * (1 - x)) * Math.sin(N * (x * Math.PI)) * dp(N)));
        });
        va.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setTranslationX(0);
            }
        });
        va.setDuration(300);
        va.start();
        view.setTag(R.id.shake_animation, va);
    }

    public static void shakeViewSpring(View view) {
        shakeViewSpring(view, 10, null);
    }

    public static void shakeViewSpring(View view, float shiftDp) {
        shakeViewSpring(view, shiftDp, null);
    }

    public static void shakeViewSpring(View view, Runnable endCallback) {
        shakeViewSpring(view, 10, endCallback);
    }

    public static void shakeViewSpring(View view, float shiftDp, Runnable endCallback) {
        if (view == null) {
            return;
        }
        int shift = dp(shiftDp);
        if (view.getTag(R.id.spring_tag) != null) {
            ((SpringAnimation) view.getTag(R.id.spring_tag)).cancel();
        }
        Float wasX = (Float) view.getTag(R.id.spring_was_translation_x_tag);
        if (wasX != null) {
            view.setTranslationX(wasX);
        }
        view.setTag(R.id.spring_was_translation_x_tag, view.getTranslationX());

        float translationX = view.getTranslationX();
        SpringAnimation springAnimation = new SpringAnimation(view, DynamicAnimation.TRANSLATION_X, translationX)
                .setSpring(new SpringForce(translationX).setStiffness(600f))
                .setStartVelocity(-shift * 100)
                .addEndListener((animation, canceled, value, velocity) -> {
                    if (endCallback != null) endCallback.run();

                    view.setTranslationX(translationX);
                    view.setTag(R.id.spring_tag, null);
                    view.setTag(R.id.spring_was_translation_x_tag, null);
                });
        view.setTag(R.id.spring_tag, springAnimation);
        springAnimation.start();
    }

    /*public static String ellipsize(String text, int maxLines, int maxWidth, TextPaint paint) {
        if (text == null || paint == null) {
            return null;
        }
        int count;
        int offset = 0;
        StringBuilder result = null;
        TextView
        for (int a = 0; a < maxLines; a++) {
            count = paint.breakText(text, true, maxWidth, null);
            if (a != maxLines - 1) {
                if (result == null) {
                    result = new StringBuilder(count * maxLines + 1);
                }
                boolean foundSpace = false;
                for (int c = count - 1; c >= offset; c--) {
                    if (text.charAt(c) == ' ') {
                        foundSpace = true;
                        result.append(text.substring(offset, c - 1));
                        offset = c - 1;
                    }
                }
                if (!foundSpace) {
                    offset = count;
                }
                text = text.substring(0, offset);
            } else if (maxLines == 1) {
                return text.substring(0, count);
            } else {
                result.append(text.substring(0, count));
            }
        }
        return result.toString();
    }*/

    /*public static void turnOffHardwareAcceleration(Window window) {
        if (window == null || Build.MODEL == null) {
            return;
        }
        if (Build.MODEL.contains("GT-S5301") ||
                Build.MODEL.contains("GT-S5303") ||
                Build.MODEL.contains("GT-B5330") ||
                Build.MODEL.contains("GT-S5302") ||
                Build.MODEL.contains("GT-S6012B") ||
                Build.MODEL.contains("MegaFon_SP-AI")) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        }
    }*/

    public static String getPath(final Uri uri) {
        try {
            if (DocumentsContract.isDocumentUri(ApplicationLoader.applicationContext, uri)) {
                if (isExternalStorageDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];
                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    }
                } else if (isDownloadsDocument(uri)) {
                    final String id = DocumentsContract.getDocumentId(uri);
                    final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                    return getDataColumn(ApplicationLoader.applicationContext, contentUri, null, null);
                } else if (isMediaDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    Uri contentUri = null;
                    switch (type) {
                        case "image":
                            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                            break;
                        case "video":
                            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                            break;
                        case "audio":
                            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                            break;
                    }

                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[]{
                            split[1]
                    };

                    return getDataColumn(ApplicationLoader.applicationContext, contentUri, selection, selectionArgs);
                }
            } else if ("content".equalsIgnoreCase(uri.getScheme())) {
                return getDataColumn(ApplicationLoader.applicationContext, uri, null, null);
            } else if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {

        final String column = "_data";
        final String[] projection = {
                column
        };
        try (Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                String value = cursor.getString(column_index);
                if (value.startsWith("content://") || !value.startsWith("/") && !value.startsWith("file://")) {
                    return null;
                }
                return value;
            }
        } catch (Exception ignore) {

        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static String generateFileName(int type, String ext) {
        Date date = new Date();
        date.setTime(System.currentTimeMillis() + Utilities.random.nextInt(1000) + 1);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(date);
        if (type == 0) {
            return "IMG_" + timeStamp + "." + (TextUtils.isEmpty(ext) ? "jpg" : ext);
        } else {
            return "VID_" + timeStamp + ".mp4";
        }
    }

    public static boolean isKeyguardSecure() {
        KeyguardManager km = (KeyguardManager) ApplicationLoader.applicationContext.getSystemService(Context.KEYGUARD_SERVICE);
        return km.isKeyguardSecure();
    }

    public static boolean isSimAvailable() {
        TelephonyManager tm = (TelephonyManager) ApplicationLoader.applicationContext.getSystemService(Context.TELEPHONY_SERVICE);
        int state = tm.getSimState();
        return state != TelephonyManager.SIM_STATE_ABSENT && state != TelephonyManager.SIM_STATE_UNKNOWN && tm.getPhoneType() != TelephonyManager.PHONE_TYPE_NONE && !isAirplaneModeOn();
    }

    public static boolean isAirplaneModeOn() {
        return Settings.Global.getInt(ApplicationLoader.applicationContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    public static String formatFileSize(long size) {
        return formatFileSize(size, false, false);
    }

    public static String formatFileSize(long size, boolean removeZero, boolean makeShort) {
        if (size == 0) {
            return String.format("%d KB", 0);
        } else if (size < 1024) {
            return String.format("%d B", size);
        } else if (size < 1024 * 1024) {
            float value = size / 1024.0f;
            if (removeZero && (value - (int) value) * 10 == 0) {
                return String.format("%d KB", (int) value);
            } else {
                return String.format("%.1f KB", value);
            }
        } else if (size < 1000 * 1024 * 1024) {
            float value = size / 1024.0f / 1024.0f;
            if (removeZero && (value - (int) value) * 10 == 0) {
                return String.format("%d MB", (int) value);
            } else {
                return String.format("%.1f MB", value);
            }
        } else {
            float value = (int) (size / 1024L / 1024L) / 1000.0f;
            if (removeZero && (value - (int) value) * 10 == 0) {
                return String.format("%d GB", (int) value);
            } else if (makeShort) {
                return String.format("%.1f GB", value);
            } else {
                return String.format("%.2f GB", value);
            }
        }
    }

    public static String formatShortDuration(int duration) {
        return formatDuration(duration, false);
    }

    public static String formatTimestamp(int duration) {
        int h = duration / 3600;
        int m = duration / 60 % 60;
        int s = duration % 60;
        String str = "";
        if (h > 0) {
            str += String.format(Locale.US, "%dh", h);
        }
        if (m > 0) {
            str += String.format(Locale.US, h > 0 ? "%02dm" : "%dm", m);
        }
        str += String.format(Locale.US, h > 0 || m > 0 ? "%02ds" : "%ds", s);
        return str;
    }

    public static String formatLongDuration(int duration) {
        return formatDuration(duration, true);
    }

    public static String formatDuration(int duration, boolean isLong) {
        return formatDuration(duration, isLong, false);
    }
    public static String formatDuration(int duration, boolean isLong, boolean noSecondsIfHours) {
        int h = duration / 3600;
        int m = duration / 60 % 60;
        int s = duration % 60;
        if (h == 0) {
            if (isLong) {
                return String.format(Locale.US, "%02d:%02d", m, s);
            } else {
                return String.format(Locale.US, "%d:%02d", m, s);
            }
        } else {
            if (noSecondsIfHours) {
                return String.format(Locale.US, "%d:%02d", h, m);
            } else {
                return String.format(Locale.US, "%d:%02d:%02d", h, m, s);
            }
        }
    }

    public static String formatFullDuration(int duration) {
        int h = duration / 3600;
        int m = duration / 60 % 60;
        int s = duration % 60;
        if (duration < 0) {
            return String.format(Locale.US, "-%02d:%02d:%02d", Math.abs(h), Math.abs(m), Math.abs(s));
        } else {
            return String.format(Locale.US, "%02d:%02d:%02d", h, m, s);
        }
    }

    public static String formatDurationNoHours(int duration, boolean isLong) {
        int m = duration / 60;
        int s = duration % 60;
        if (isLong) {
            return String.format(Locale.US, "%02d:%02d", m, s);
        } else {
            return String.format(Locale.US, "%d:%02d", m, s);
        }
    }

    public static String formatShortDuration(int progress, int duration) {
        return formatDuration(progress, duration, false);
    }

    public static String formatLongDuration(int progress, int duration) {
        return formatDuration(progress, duration, true);
    }

    public static String formatDuration(int progress, int duration, boolean isLong) {
        int h = duration / 3600;
        int m = duration / 60 % 60;
        int s = duration % 60;

        int ph = progress / 3600;
        int pm = progress / 60 % 60;
        int ps = progress % 60;

        if (duration == 0) {
            if (ph == 0) {
                if (isLong) {
                    return String.format(Locale.US, "%02d:%02d / -:--", pm, ps);
                } else {
                    return String.format(Locale.US, "%d:%02d / -:--", pm, ps);
                }
            } else {
                return String.format(Locale.US, "%d:%02d:%02d / -:--", ph, pm, ps);
            }
        } else {
            if (ph == 0 && h == 0) {
                if (isLong) {
                    return String.format(Locale.US, "%02d:%02d / %02d:%02d", pm, ps, m, s);
                } else {
                    return String.format(Locale.US, "%d:%02d / %d:%02d", pm, ps, m, s);
                }
            } else {
                return String.format(Locale.US, "%d:%02d:%02d / %d:%02d:%02d", ph, pm, ps, h, m, s);
            }
        }
    }

    public static String formatVideoDuration(int progress, int duration) {
        int h = duration / 3600;
        int m = duration / 60 % 60;
        int s = duration % 60;

        int ph = progress / 3600;
        int pm = progress / 60 % 60;
        int ps = progress % 60;

        if (ph == 0 && h == 0) {
            return String.format(Locale.US, "%02d:%02d / %02d:%02d", pm, ps, m, s);
        } else {
            if (h == 0) {
                return String.format(Locale.US, "%d:%02d:%02d / %02d:%02d", ph, pm, ps, m, s);
            } else if (ph == 0) {
                return String.format(Locale.US, "%02d:%02d / %d:%02d:%02d", pm, ps, h, m, s);
            } else {
                return String.format(Locale.US, "%d:%02d:%02d / %d:%02d:%02d", ph, pm, ps, h, m, s);
            }
        }
    }

    public static String formatCount(int count) {
        if (count < 1000) return Integer.toString(count);

        ArrayList<String> strings = new ArrayList<>();
        while (count != 0) {
            int mod = count % 1000;
            count /= 1000;
            if (count > 0) {
                strings.add(String.format(Locale.ENGLISH, "%03d", mod));
            } else {
                strings.add(Integer.toString(mod));
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = strings.size() - 1; i >= 0; i--) {
            stringBuilder.append(strings.get(i));
            if (i != 0) {
                stringBuilder.append(",");
            }
        }

        return stringBuilder.toString();
    }

    public static final String[] numbersSignatureArray = {"", "K", "M", "B", "T", "P"};

    public static String formatWholeNumber(int v, int dif) {
        if (v == 0) {
            return "0";
        }
        float num_ = v;
        int count = 0;
        if (dif == 0) dif = v;
        if (dif < 1000) {
            return AndroidUtilities.formatCount(v);
        }
        while (dif >= 1000 && count < numbersSignatureArray.length - 1) {
            dif /= 1000;
            num_ /= 1000;
            count++;
        }
        if (num_ < 0.1) {
            return "0";
        } else {
            if ((num_ * 10) == (int) (num_ * 10)) {
                return String.format(Locale.ENGLISH, "%s%s", AndroidUtilities.formatCount((int) num_), numbersSignatureArray[count]);
            } else {
                return String.format(Locale.ENGLISH, "%.1f%s", (int) (num_ * 10) / 10f, numbersSignatureArray[count]);
            }
        }
    }

    public static byte[] decodeQuotedPrintable(final byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        for (int i = 0; i < bytes.length; i++) {
            final int b = bytes[i];
            if (b == '=') {
                try {
                    final int u = Character.digit((char) bytes[++i], 16);
                    final int l = Character.digit((char) bytes[++i], 16);
                    buffer.write((char) ((u << 4) + l));
                } catch (Exception e) {
                    FileLog.e(e);
                    return null;
                }
            } else {
                buffer.write(b);
            }
        }
        byte[] array = buffer.toByteArray();
        try {
            buffer.close();
        } catch (Exception e) {
            FileLog.e(e);
        }
        return array;
    }

    public static boolean copyFile(InputStream sourceFile, File destFile) throws IOException {
        return copyFile(sourceFile, new FileOutputStream(destFile));
    }

    public static boolean copyFile(InputStream sourceFile, OutputStream out) throws IOException {
        byte[] buf = new byte[4096];
        int len;
        while ((len = sourceFile.read(buf)) > 0) {
            Thread.yield();
            out.write(buf, 0, len);
        }
        out.close();
        return true;
    }

    public static boolean copyFileSafe(File sourceFile, File destFile) {
        try {
            return copyFile(sourceFile, destFile);
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }

    public static boolean copyFile(File sourceFile, File destFile) throws IOException {
        if (sourceFile.equals(destFile)) {
            return true;
        }
        if (!destFile.exists()) {
            destFile.createNewFile();
        }
        try (FileInputStream source = new FileInputStream(sourceFile); FileOutputStream destination = new FileOutputStream(destFile)) {
            destination.getChannel().transferFrom(source.getChannel(), 0, source.getChannel().size());
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
        return true;
    }

    public static byte[] calcAuthKeyHash(byte[] auth_key) {
        byte[] sha1 = Utilities.computeSHA1(auth_key);
        byte[] key_hash = new byte[16];
        System.arraycopy(sha1, 0, key_hash, 0, 16);
        return key_hash;
    }

    public static CharSequence replaceTwoNewLinesToOne(CharSequence original) {
        char[] buf = new char[2];
        if (original instanceof StringBuilder) {
            StringBuilder stringBuilder = (StringBuilder) original;
            for (int a = 0, N = original.length(); a < N - 2; a++) {
                stringBuilder.getChars(a, a + 2, buf, 0);
                if (buf[0] == '\n' && buf[1] == '\n') {
                    stringBuilder = stringBuilder.replace(a, a + 2, "\n");
                    a--;
                    N--;
                }
            }
            return original;
        } else if (original instanceof SpannableStringBuilder) {
            SpannableStringBuilder stringBuilder = (SpannableStringBuilder) original;
            for (int a = 0, N = original.length(); a < N - 2; a++) {
                stringBuilder.getChars(a, a + 2, buf, 0);
                if (buf[0] == '\n' && buf[1] == '\n') {
                    stringBuilder = stringBuilder.replace(a, a + 2, "\n");
                    a--;
                    N--;
                }
            }
            return original;
        } else if (original instanceof SpannableString) {
            if (TextUtils.indexOf(original, "\n\n") < 0) return original;
            SpannableStringBuilder stringBuilder = new SpannableStringBuilder(original);
            for (int a = 0, N = original.length(); a < N - 2; a++) {
                stringBuilder.getChars(a, a + 2, buf, 0);
                if (buf[0] == '\n' && buf[1] == '\n') {
                    stringBuilder = stringBuilder.replace(a, a + 2, "\n");
                    a--;
                    N--;
                }
            }
            return stringBuilder;
        }
        return original.toString().replace("\n\n", "\n");
    }

    public static CharSequence replaceNewLines(CharSequence original) {
        if (original instanceof StringBuilder) {
            StringBuilder stringBuilder = (StringBuilder) original;
            for (int a = 0, N = original.length(); a < N; a++) {
                if (original.charAt(a) == '\n') {
                    stringBuilder.setCharAt(a, ' ');
                }
            }
            return original;
        } else if (original instanceof SpannableStringBuilder) {
            SpannableStringBuilder stringBuilder = (SpannableStringBuilder) original;
            for (int a = 0, N = original.length(); a < N; a++) {
                if (original.charAt(a) == '\n') {
                    stringBuilder.replace(a, a + 1, " ");
                }
            }
            return stringBuilder;
        } else if (original instanceof SpannableString) {
            if (TextUtils.indexOf(original, '\n') < 0) return original;
            SpannableStringBuilder stringBuilder = new SpannableStringBuilder(original);
            for (int a = 0, N = original.length(); a < N; a++) {
                if (original.charAt(a) == '\n') {
                    stringBuilder.replace(a, a + 1, " ");
                }
            }
            return stringBuilder;
        } else if (original == null) {
            return null;
        }
        return original.toString().replace('\n', ' ');
    }

    public static void setRectR(RectF rect, float cx, float cy, float r) {
        rect.set(cx - r, cy - r, cx + r, cy + r);
    }
    public static void setRectD(RectF rect, float cx, float cy, float d) {
        rect.set(cx - d / 2f, cy - d / 2f, cx + d / 2f, cy + d / 2f);
    }

    public static void setRectToRect(Matrix matrix, RectF src, RectF dst, int rotation, int invert, boolean translate) {
        float tx, sx;
        float ty, sy;
        boolean xLarger = false;
        if (rotation == 90 || rotation == 270) {
            sx = dst.height() / src.width();
            sy = dst.width() / src.height();
        } else {
            sx = dst.width() / src.width();
            sy = dst.height() / src.height();
        }
        if (sx < sy) {
            sx = sy;
            xLarger = true;
        } else {
            sy = sx;
        }
        if (translate) {
            matrix.setTranslate(dst.left, dst.top);
        }
        if (rotation == 90) {
            matrix.preRotate(90);
            if (invert == 1) {
                matrix.preScale(-1, 1);
            } else if (invert == 2) {
                matrix.preScale(1, -1);
            }
            matrix.preTranslate(0, -dst.width());
        } else if (rotation == 180) {
            matrix.preRotate(180);
            if (invert == 1) {
                matrix.preScale(-1, 1);
            } else if (invert == 2) {
                matrix.preScale(1, -1);
            }
            matrix.preTranslate(-dst.width(), -dst.height());
        } else if (rotation == 270) {
            matrix.preRotate(270);
            if (invert == 1) {
                matrix.preScale(-1, 1);
            } else if (invert == 2) {
                matrix.preScale(1, -1);
            }
            matrix.preTranslate(-dst.height(), 0);
        }

        if (translate) {
            tx = -src.left * sx;
            ty = -src.top * sy;
        } else {
            tx = dst.left - src.left * sx;
            ty = dst.top - src.top * sy;
        }

        float diff;
        if (xLarger) {
            diff = dst.width() - src.width() * sy;
        } else {
            diff = dst.height() - src.height() * sy;
        }
        diff = diff / 2.0f;
        if (xLarger) {
            tx += diff;
        } else {
            ty += diff;
        }

        matrix.preScale(sx, sy);
        if (translate) {
            matrix.preTranslate(tx, ty);
        }
    }

    public static Vibrator getVibrator() {
        if (vibrator == null) {
            vibrator = (Vibrator) ApplicationLoader.applicationContext.getSystemService(Context.VIBRATOR_SERVICE);
        }
        return vibrator;
    }

    public static boolean isAccessibilityTouchExplorationEnabled() {
        if (accessibilityManager == null) {
            accessibilityManager = (AccessibilityManager) ApplicationLoader.applicationContext.getSystemService(Context.ACCESSIBILITY_SERVICE);
        }
        return accessibilityManager.isEnabled() && accessibilityManager.isTouchExplorationEnabled();
    }

    public static float getAnimatorDurationScale() {
        try {
            return Settings.Global.getFloat(
                    ApplicationLoader.applicationContext.getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1.0f
            );
        } catch (Exception e) {
            return 1.0f;
        }
    }

    public static boolean shouldEnableAnimation() {
        if (Build.VERSION.SDK_INT < 26 || Build.VERSION.SDK_INT >= 28) {
            return true;
        }
        PowerManager powerManager = (PowerManager) ApplicationLoader.applicationContext.getSystemService(Context.POWER_SERVICE);
        if (powerManager.isPowerSaveMode()) {
            return false;
        }
        float scale = getAnimatorDurationScale();
        if (scale <= 0.0f) {
            return false;
        }
        return true;
    }

    @SuppressLint("PrivateApi")
    public static String getSystemProperty(String key) {
        try {
            Class props = Class.forName("android.os.SystemProperties");
            return (String) props.getMethod("get", String.class).invoke(null, key);
        } catch (Exception ignore) {
        }
        return null;
    }

    public static void fixGoogleMapsBug() { //https://issuetracker.google.com/issues/154855417#comment301
        SharedPreferences googleBug = ApplicationLoader.applicationContext.getSharedPreferences("google_bug_154855417", Context.MODE_PRIVATE);
        if (!googleBug.contains("fixed")) {
            File corruptedZoomTables = new File(ApplicationLoader.getFilesDirFixed(), "ZoomTables.data");
            corruptedZoomTables.delete();
            googleBug.edit().putBoolean("fixed", true).apply();
        }
    }

    public static CharSequence concat(CharSequence... text) {
        if (text.length == 0) {
            return "";
        }

        if (text.length == 1) {
            return text[0];
        }

        boolean spanned = false;
        for (CharSequence piece : text) {
            if (piece instanceof Spanned) {
                spanned = true;
                break;
            }
        }

        if (spanned) {
            final SpannableStringBuilder ssb = new SpannableStringBuilder();
            for (CharSequence piece : text) {
                // If a piece is null, we append the string "null" for compatibility with the
                // behavior of StringBuilder and the behavior of the concat() method in earlier
                // versions of Android.
                ssb.append(piece == null ? "null" : piece);
            }
            return new SpannedString(ssb);
        } else {
            final StringBuilder sb = new StringBuilder();
            for (CharSequence piece : text) {
                sb.append(piece);
            }
            return sb.toString();
        }
    }

    public static float[] RGBtoHSB(int r, int g, int b) {
        float hue, saturation, brightness;
        float[] hsbvals = new float[3];
        int cmax = Math.max(r, g);
        if (b > cmax) {
            cmax = b;
        }
        int cmin = Math.min(r, g);
        if (b < cmin) {
            cmin = b;
        }

        brightness = ((float) cmax) / 255.0f;
        if (cmax != 0) {
            saturation = ((float) (cmax - cmin)) / ((float) cmax);
        } else {
            saturation = 0;
        }
        if (saturation == 0) {
            hue = 0;
        } else {
            float redc = ((float) (cmax - r)) / ((float) (cmax - cmin));
            float greenc = ((float) (cmax - g)) / ((float) (cmax - cmin));
            float bluec = ((float) (cmax - b)) / ((float) (cmax - cmin));
            if (r == cmax) {
                hue = bluec - greenc;
            } else if (g == cmax) {
                hue = 2.0f + redc - bluec;
            } else {
                hue = 4.0f + greenc - redc;
            }
            hue = hue / 6.0f;
            if (hue < 0) {
                hue = hue + 1.0f;
            }
        }
        hsbvals[0] = hue;
        hsbvals[1] = saturation;
        hsbvals[2] = brightness;
        return hsbvals;
    }

    public static int HSBtoRGB(float hue, float saturation, float brightness) {
        int r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = (int) (brightness * 255.0f + 0.5f);
        } else {
            float h = (hue - (float) Math.floor(hue)) * 6.0f;
            float f = h - (float) java.lang.Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - (saturation * (1.0f - f)));
            switch ((int) h) {
                case 0:
                    r = (int) (brightness * 255.0f + 0.5f);
                    g = (int) (t * 255.0f + 0.5f);
                    b = (int) (p * 255.0f + 0.5f);
                    break;
                case 1:
                    r = (int) (q * 255.0f + 0.5f);
                    g = (int) (brightness * 255.0f + 0.5f);
                    b = (int) (p * 255.0f + 0.5f);
                    break;
                case 2:
                    r = (int) (p * 255.0f + 0.5f);
                    g = (int) (brightness * 255.0f + 0.5f);
                    b = (int) (t * 255.0f + 0.5f);
                    break;
                case 3:
                    r = (int) (p * 255.0f + 0.5f);
                    g = (int) (q * 255.0f + 0.5f);
                    b = (int) (brightness * 255.0f + 0.5f);
                    break;
                case 4:
                    r = (int) (t * 255.0f + 0.5f);
                    g = (int) (p * 255.0f + 0.5f);
                    b = (int) (brightness * 255.0f + 0.5f);
                    break;
                case 5:
                    r = (int) (brightness * 255.0f + 0.5f);
                    g = (int) (p * 255.0f + 0.5f);
                    b = (int) (q * 255.0f + 0.5f);
                    break;
            }
        }
        return 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
    }

    public static float computePerceivedBrightness(int color) {
        return (Color.red(color) * 0.2126f + Color.green(color) * 0.7152f + Color.blue(color) * 0.0722f) / 255f;
    }

    public static int getPatternColor(int color) {
        return getPatternColor(color, false);
    }

    public static int getPatternColor(int color, boolean alwaysDark) {
        float[] hsb = RGBtoHSB(Color.red(color), Color.green(color), Color.blue(color));
        if (hsb[1] > 0.0f || (hsb[2] < 1.0f && hsb[2] > 0.0f)) {
            hsb[1] = Math.min(1.0f, hsb[1] + (alwaysDark ? 0.15f : 0.05f) + 0.1f * (1.0f - hsb[1]));
        }
        if (alwaysDark || hsb[2] > 0.5f) {
            hsb[2] = Math.max(0.0f, hsb[2] * 0.65f);
        } else {
            hsb[2] = Math.max(0.0f, Math.min(1.0f, 1.0f - hsb[2] * 0.65f));
        }
        return HSBtoRGB(hsb[0], hsb[1], hsb[2]) & (alwaysDark ? 0x99ffffff : 0x66ffffff);
    }

    public static int getPatternSideColor(int color) {
        float[] hsb = RGBtoHSB(Color.red(color), Color.green(color), Color.blue(color));
        hsb[1] = Math.min(1.0f, hsb[1] + 0.05f);
        if (hsb[2] > 0.5f) {
            hsb[2] = Math.max(0.0f, hsb[2] * 0.90f);
        } else {
            hsb[2] = Math.max(0.0f, hsb[2] * 0.90f);
        }
        return HSBtoRGB(hsb[0], hsb[1], hsb[2]) | 0xff000000;
    }

    public static int getWallpaperRotation(int angle, boolean iOS) {
        if (iOS) {
            angle += 180;
        } else {
            angle -= 180;
        }
        while (angle >= 360) {
            angle -= 360;
        }
        while (angle < 0) {
            angle += 360;
        }
        return angle;
    }

    public static float distanceInfluenceForSnapDuration(float f) {
        f -= 0.5F;
        f *= 0.47123894F;
        return (float) Math.sin(f);
    }

    public static void makeAccessibilityAnnouncement(CharSequence what) {
        if (TextUtils.isEmpty(what)) return;
        AccessibilityManager am = (AccessibilityManager) ApplicationLoader.applicationContext.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am.isEnabled()) {
            AccessibilityEvent ev = AccessibilityEvent.obtain();
            ev.setEventType(AccessibilityEvent.TYPE_ANNOUNCEMENT);
            ev.getText().add(what);
            am.sendAccessibilityEvent(ev);
        }
    }

    public static int getOffsetColor(int color1, int color2, float offset, float alpha) {
        int rF = Color.red(color2);
        int gF = Color.green(color2);
        int bF = Color.blue(color2);
        int aF = Color.alpha(color2);
        int rS = Color.red(color1);
        int gS = Color.green(color1);
        int bS = Color.blue(color1);
        int aS = Color.alpha(color1);
        return Color.argb((int) ((aS + (aF - aS) * offset) * alpha), (int) (rS + (rF - rS) * offset), (int) (gS + (gF - gS) * offset), (int) (bS + (bF - bS) * offset));
    }

    public static int indexOfIgnoreCase(final String origin, final String searchStr) {
        if (searchStr.isEmpty() || origin.isEmpty()) {
            return origin.indexOf(searchStr);
        }

        for (int i = 0; i < origin.length(); i++) {
            if (i + searchStr.length() > origin.length()) {
                return -1;
            }
            int j = 0;
            int ii = i;
            while (ii < origin.length() && j < searchStr.length()) {
                char c = Character.toLowerCase(origin.charAt(ii));
                char c2 = Character.toLowerCase(searchStr.charAt(j));
                if (c != c2) {
                    break;
                }
                j++;
                ii++;
            }
            if (j == searchStr.length()) {
                return i;
            }
        }

        return -1;
    }

    public static float distance(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
    }

    public static int lerp(int a, int b, float f) {
        return (int) (a + f * (b - a));
    }

    public static float lerpAngle(float a, float b, float f) {
        float delta = ((b - a + 360 + 180) % 360) - 180;
        return (a + delta * f + 360) % 360;
    }

    public static float lerp(float a, float b, float f) {
        return a + f * (b - a);
    }

    public static float lerp(float a, float b, float c, float f) {
        return lerp(a, b, c, 0.5f, f);
    }

    public static float lerp(float a, float b, float c, float middle, float f) {
        if (f < middle) return lerp(a, b, f / middle);
        return lerp(b, c, (f - middle) / (1.0f - middle));
    }

    public static float lerp(boolean a, boolean b, float f) {
        return (a ? 1.0f : 0.0f) + f * ((b ? 1.0f : 0.0f) - (a ? 1.0f : 0.0f));
    }

    public static float lerp3(float atMinusOne, float atZero, float atPlusOne, float t) {
        if (t < 0) return lerp(atZero, atMinusOne, -t);
        return lerp(atZero, atPlusOne, t);
    }
    public static int lerpColor(int a, int b, float t) {
        return Color.argb(
            lerp(Color.alpha(a), Color.alpha(b), t),
            lerp(Color.red(a),   Color.red(b),   t),
            lerp(Color.green(a), Color.green(b), t),
            lerp(Color.blue(a),  Color.blue(b),  t)
        );
    }
    public static int lerpColor3(int atMinusOne, int atZero, int atPlusOne, float t) {
        if (t < 0) return lerpColor(atZero, atMinusOne, -t);
        return lerpColor(atZero, atPlusOne, t);
    }

    public static double lerp(double a, double b, float f) {
        return a + f * (b - a);
    }

    public static float lerp(float[] ab, float f) {
        return lerp(ab[0], ab[1], f);
    }

    public static void lerp(Rect a, RectF b, float f, RectF to) {
        if (to != null) {
            to.set(
                lerp(a.left, b.left, f),
                lerp(a.top, b.top, f),
                lerp(a.right, b.right, f),
                lerp(a.bottom, b.bottom, f)
            );
        }
    }

    public static void lerp(RectF a, Rect b, float f, RectF to) {
        if (to != null) {
            to.set(
                lerp(a.left, b.left, f),
                lerp(a.top, b.top, f),
                lerp(a.right, b.right, f),
                lerp(a.bottom, b.bottom, f)
            );
        }
    }

    public static void lerp(RectF a, RectF b, float f, RectF to) {
        if (to != null) {
            to.set(
                lerp(a.left, b.left, f),
                lerp(a.top, b.top, f),
                lerp(a.right, b.right, f),
                lerp(a.bottom, b.bottom, f)
            );
        }
    }

    public static void lerp(Rect a, Rect b, float f, Rect to) {
        if (to != null) {
            to.set(
                lerp(a.left, b.left, f),
                lerp(a.top, b.top, f),
                lerp(a.right, b.right, f),
                lerp(a.bottom, b.bottom, f)
            );
        }
    }

    public static void lerpCentered(RectF a, RectF b, float f, RectF to) {
        if (to == null) return;
        final float cx = lerp(a.centerX(), b.centerX(), f);
        final float cy = lerp(a.centerY(), b.centerY(), f);
        final float hw = lerp(a.width(), b.width(), Math.min(1, f)) / 2f;
        final float hh = lerp(a.height(), b.height(), Math.min(1, f)) / 2f;
        to.set(cx - hw, cy - hh, cx + hw, cy + hh);
    }

    public static void lerpCentered(Rect a, Rect b, float f, Rect to) {
        if (to == null) return;
        final float cx = lerp(a.centerX(), b.centerX(), f);
        final float cy = lerp(a.centerY(), b.centerY(), f);
        final float hw = lerp(a.width(), b.width(), Math.min(1, f)) / 2f;
        final float hh = lerp(a.height(), b.height(), Math.min(1, f)) / 2f;
        to.set((int) (cx - hw), (int) (cy - hh), (int) (cx + hw), (int) (cy + hh));
    }

    public static void lerp(int[] a, int[] b, float f, int[] to) {
        if (to == null) return;
        for (int i = 0; i < to.length; ++i) {
            int av = a == null || i >= a.length ? 0 : a[i];
            int bv = b == null || i >= b.length ? 0 : b[i];
            to[i] = lerp(av, bv, f);
        }
    }

    public static void lerp(float[] a, float[] b, float f, float[] to) {
        if (to == null) return;
        for (int i = 0; i < to.length; ++i) {
            float av = a == null || i >= a.length ? 0 : a[i];
            float bv = b == null || i >= b.length ? 0 : b[i];
            to[i] = lerp(av, bv, f);
        }
    }

    private static final float[] tempFloats = new float[9], tempFloats2 = new float[9];
    public static void lerp(Matrix a, Matrix b, float t, Matrix to) {
        if (a == null || b == null) return;
        a.getValues(tempFloats);
        b.getValues(tempFloats2);
        lerp(tempFloats, tempFloats2, t, tempFloats2);
        to.setValues(tempFloats2);
    }

    public static float ilerp(int x, int a, int b) {
        return (float) (x - a) / (b - a);
    }

    public static float ilerp(float x, float a, float b) {
        return (x - a) / (b - a);
    }

    public static void scaleRect(RectF rect, float scale) {
        scaleRect(rect, scale, rect.centerX(), rect.centerY());
    }

    public static void scaleRect(RectF rect, float scale, float px, float py) {
        final float wl = px - rect.left, wr = rect.right - px;
        final float ht = py - rect.top, hb = rect.bottom - py;
        rect.set(
            px - wl * scale,
            py - ht * scale,
            px + wr * scale,
            py + hb * scale
        );
    }

    public static float cascade(float fullAnimationT, float position, float count, float waveLength) {
        if (count <= 0) return fullAnimationT;
        final float waveDuration = 1f / count * Math.min(waveLength, count);
        final float waveOffset = position / count * (1f - waveDuration);
        return MathUtils.clamp((fullAnimationT - waveOffset) / waveDuration, 0, 1);
    }

    public static int multiplyAlphaComponent(int color, float k) {
        return ColorUtils.setAlphaComponent(color, (int) (Color.alpha(color) * k));
    }

    public static float computeDampingRatio(float tension /* stiffness */, float friction /* damping */, float mass) {
        return friction / (2f * (float) Math.sqrt(mass * tension));
    }

    public static String getCertificateSHA256Fingerprint() {
        PackageManager pm = ApplicationLoader.applicationContext.getPackageManager();
        String packageName = ApplicationLoader.applicationContext.getPackageName();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            Signature[] signatures = packageInfo.signatures;
            byte[] cert = signatures[0].toByteArray();
            InputStream input = new ByteArrayInputStream(cert);
            CertificateFactory cf = CertificateFactory.getInstance("X509");
            X509Certificate c = (X509Certificate) cf.generateCertificate(input);
            return Utilities.bytesToHex(Utilities.computeSHA256(c.getEncoded()));
        } catch (Throwable ignore) {

        }
        return "";
    }

    public static String getCertificateSHA1Fingerprint() {
        PackageManager pm = ApplicationLoader.applicationContext.getPackageManager();
        String packageName = ApplicationLoader.applicationContext.getPackageName();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            Signature[] signatures = packageInfo.signatures;
            byte[] cert = signatures[0].toByteArray();
            InputStream input = new ByteArrayInputStream(cert);
            CertificateFactory cf = CertificateFactory.getInstance("X509");
            X509Certificate c = (X509Certificate) cf.generateCertificate(input);
            return Utilities.bytesToHex(Utilities.computeSHA1(c.getEncoded()));
        } catch (Throwable ignore) {

        }
        return "";
    }

    private static char[] characters = new char[]{' ', ' ', '!', '"', '#', '%', '&', '\'', '(', ')', '*', ',', '-', '.', '/', ':', ';', '?', '@', '[', '\\', ']', '_', '{', '}', '¡', '§', '«', '¶', '·', '»', '¿', ';', '·', '՚', '՛', '՜', '՝', '՞', '՟', '։', '֊', '־', '׀', '׃', '׆', '׳', '״', '؉', '؊', '،', '؍', '؛', '؞', '؟', '٪', '٫', '٬', '٭', '۔', '܀', '܁', '܂', '܃', '܄', '܅', '܆', '܇', '܈', '܉', '܊', '܋', '܌', '܍', '߷', '߸', '߹', '࠰', '࠱', '࠲', '࠳', '࠴', '࠵', '࠶', '࠷', '࠸', '࠹', '࠺', '࠻', '࠼', '࠽', '࠾', '࡞', '।', '॥', '॰', '৽', '੶', '૰', '౷', '಄', '෴', '๏', '๚', '๛', '༄', '༅', '༆', '༇', '༈', '༉', '༊', '་', '༌', '།', '༎', '༏', '༐', '༑', '༒', '༔', '༺', '༻', '༼', '༽', '྅', '࿐', '࿑', '࿒', '࿓', '࿔', '࿙', '࿚', '၊', '။', '၌', '၍', '၎', '၏', '჻', '፠', '፡', '።', '፣', '፤', '፥', '፦', '፧', '፨', '᐀', '᙮', '᚛', '᚜', '᛫', '᛬', '᛭', '᜵', '᜶', '។', '៕', '៖', '៘', '៙', '៚', '᠀', '᠁', '᠂', '᠃', '᠄', '᠅', '᠆', '᠇', '᠈', '᠉', '᠊', '᥄', '᥅', '᨞', '᨟', '᪠', '᪡', '᪢', '᪣', '᪤', '᪥', '᪦', '᪨', '᪩', '᪪', '᪫', '᪬', '᪭', '᭚', '᭛', '᭜', '᭝', '᭞', '᭟', '᭠', '᯼', '᯽', '᯾', '᯿', '᰻', '᰼', '᰽', '᰾', '᰿', '᱾', '᱿', '᳀', '᳁', '᳂', '᳃', '᳄', '᳅', '᳆', '᳇', '᳓', '‐', '‑', '‒', '–', '—', '―', '‖', '‗', '‘', '’', '‚', '‛', '“', '”', '„', '‟', '†', '‡', '•', '‣', '․', '‥', '…', '‧', '‰', '‱', '′', '″', '‴', '‵', '‶', '‷', '‸', '‹', '›', '※', '‼', '‽', '‾', '‿', '⁀', '⁁', '⁂', '⁃', '⁅', '⁆', '⁇', '⁈', '⁉', '⁊', '⁋', '⁌', '⁍', '⁎', '⁏', '⁐', '⁑', '⁓', '⁔', '⁕', '⁖', '⁗', '⁘', '⁙', '⁚', '⁛', '⁜', '⁝', '⁞', '⁽', '⁾', '₍', '₎', '⌈', '⌉', '⌊', '⌋', '〈', '〉', '❨', '❩', '❪', '❫', '❬', '❭', '❮', '❯', '❰', '❱', '❲', '❳', '❴', '❵', '⟅', '⟆', '⟦', '⟧', '⟨', '⟩', '⟪', '⟫', '⟬', '⟭', '⟮', '⟯', '⦃', '⦄', '⦅', '⦆', '⦇', '⦈', '⦉', '⦊', '⦋', '⦌', '⦍', '⦎', '⦏', '⦐', '⦑', '⦒', '⦓', '⦔', '⦕', '⦖', '⦗', '⦘', '⧘', '⧙', '⧚', '⧛', '⧼', '⧽', '⳹', '⳺', '⳻', '⳼', '⳾', '⳿', '⵰', '⸀', '⸁', '⸂', '⸃', '⸄', '⸅', '⸆', '⸇', '⸈', '⸉', '⸊', '⸋', '⸌', '⸍', '⸎', '⸏', '⸐', '⸑', '⸒', '⸓', '⸔', '⸕', '⸖', '⸗', '⸘', '⸙', '⸚', '⸛', '⸜', '⸝', '⸞', '⸟', '⸠', '⸡', '⸢', '⸣', '⸤', '⸥', '⸦', '⸧', '⸨', '⸩', '⸪', '⸫', '⸬', '⸭', '⸮', '⸰', '⸱', '⸲', '⸳', '⸴', '⸵', '⸶', '⸷', '⸸', '⸹', '⸺', '⸻', '⸼', '⸽', '⸾', '⸿', '⹀', '⹁', '⹂', '⹃', '⹄', '⹅', '⹆', '⹇', '⹈', '⹉', '⹊', '⹋', '⹌', '⹍', '⹎', '⹏', '、', '。', '〃', '〈', '〉', '《', '》', '「', '」', '『', '』', '【', '】', '〔', '〕', '〖', '〗', '〘', '〙', '〚', '〛', '〜', '〝', '〞', '〟', '〰', '〽', '゠', '・', '꓾', '꓿', '꘍', '꘎', '꘏', '꙳', '꙾', '꛲', '꛳', '꛴', '꛵', '꛶', '꛷', '꡴', '꡵', '꡶', '꡷', '꣎', '꣏', '꣸', '꣹', '꣺', '꣼', '꤮', '꤯', '꥟', '꧁', '꧂', '꧃', '꧄', '꧅', '꧆', '꧇', '꧈', '꧉', '꧊', '꧋', '꧌', '꧍', '꧞', '꧟', '꩜', '꩝', '꩞', '꩟', '꫞', '꫟', '꫰', '꫱', '꯫', '﴾', '﴿', '︐', '︑', '︒', '︓', '︔', '︕', '︖', '︗', '︘', '︙', '︰', '︱', '︲', '︳', '︴', '︵', '︶', '︷', '︸', '︹', '︺', '︻', '︼', '︽', '︾', '︿', '﹀', '﹁', '﹂', '﹃', '﹄', '﹅', '﹆', '﹇', '﹈', '﹉', '﹊', '﹋', '﹌', '﹍', '﹎', '﹏', '﹐', '﹑', '﹒', '﹔', '﹕', '﹖', '﹗', '﹘', '﹙', '﹚', '﹛', '﹜', '﹝', '﹞', '﹟', '﹠', '﹡', '﹣', '﹨', '﹪', '﹫', '！', '＂', '＃', '％', '＆', '＇', '（', '）', '＊', '，', '－', '．', '／', '：', '；', '？', '＠', '［', '＼', '］', '＿', '｛', '｝', '｟', '｠', '｡', '｢', '｣', '､', '･'};
    //private static String[] longCharacters = new String[] {"𐄀", "𐄁", "𐄂", "𐎟", "𐏐", "𐕯", "𐡗", "𐤟", "𐤿", "𐩐", "𐩑", "𐩒", "𐩓", "𐩔", "𐩕", "𐩖", "𐩗", "𐩘", "𐩿", "𐫰", "𐫱", "𐫲", "𐫳", "𐫴", "𐫵", "𐫶", "𐬹", "𐬺", "𐬻", "𐬼", "𐬽", "𐬾", "𐬿", "𐮙", "𐮚", "𐮛", "𐮜", "𐽕", "𐽖", "𐽗", "𐽘", "𐽙", "𑁇", "𑁈", "𑁉", "𑁊", "𑁋", "𑁌", "𑁍", "𑂻", "𑂼", "𑂾", "𑂿", "𑃀", "𑃁", "𑅀", "𑅁", "𑅂", "𑅃", "𑅴", "𑅵", "𑇅", "𑇆", "𑇇", "𑇈", "𑇍", "𑇛", "𑇝", "𑇞", "𑇟", "𑈸", "𑈹", "𑈺", "𑈻", "𑈼", "𑈽", "𑊩", "𑑋", "𑑌", "𑑍", "𑑎", "𑑏", "𑑛", "𑑝", "𑓆", "𑗁", "𑗂", "𑗃", "𑗄", "𑗅", "𑗆", "𑗇", "𑗈", "𑗉", "𑗊", "𑗋", "𑗌", "𑗍", "𑗎", "𑗏", "𑗐", "𑗑", "𑗒", "𑗓", "𑗔", "𑗕", "𑗖", "𑗗", "𑙁", "𑙂", "𑙃", "𑙠", "𑙡", "𑙢", "𑙣", "𑙤", "𑙥", "𑙦", "𑙧", "𑙨", "𑙩", "𑙪", "𑙫", "𑙬", "𑜼", "𑜽", "𑜾", "𑠻", "𑧢", "𑨿", "𑩀", "𑩁", "𑩂", "𑩃", "𑩄", "𑩅", "𑩆", "𑪚", "𑪛", "𑪜", "𑪞", "𑪟", "𑪠", "𑪡", "𑪢", "𑱁", "𑱂", "𑱃", "𑱄", "𑱅", "𑱰", "𑱱", "𑻷", "𑻸", "𑿿", "𒑰", "𒑱", "𒑲", "𒑳", "𒑴", "𖩮", "𖩯", "𖫵", "𖬷", "𖬸", "𖬹", "𖬺", "𖬻", "𖭄", "𖺗", "𖺘", "𖺙", "𖺚", "𖿢", "𛲟", "𝪇", "𝪈", "𝪉", "𝪊", "𝪋", "𞥞", "𞥟"};
    private static HashSet<Character> charactersMap;

    public static boolean isPunctuationCharacter(char ch) {
        if (charactersMap == null) {
            charactersMap = new HashSet<>();
            for (int a = 0; a < characters.length; a++) {
                charactersMap.add(characters[a]);
            }
        }
        //int len = longCharacters[0].length();
        return charactersMap.contains(ch);
    }

    public static int getColorDistance(int color1, int color2) {
        int r1 = Color.red(color1);
        int g1 = Color.green(color1);
        int b1 = Color.blue(color1);

        int r2 = Color.red(color2);
        int g2 = Color.green(color2);
        int b2 = Color.blue(color2);

        int rMean = (r1 + r2) / 2;
        int r = r1 - r2;
        int g = g1 - g2;
        int b = b1 - b2;
        return (((512 + rMean) * r * r) >> 8) + (4 * g * g) + (((767 - rMean) * b * b) >> 8);
    }

    public static int getAverageColor(int color1, int color2) {
        int r1 = Color.red(color1);
        int r2 = Color.red(color2);
        int g1 = Color.green(color1);
        int g2 = Color.green(color2);
        int b1 = Color.blue(color1);
        int b2 = Color.blue(color2);
        return Color.argb(255, (r1 / 2 + r2 / 2), (g1 / 2 + g2 / 2), (b1 / 2 + b2 / 2));
    }

    public static void setLightStatusBar(Activity activity, boolean enable) {
        if (activity != null) {
            setLightStatusBar(activity.getWindow(), enable);
        }
    }

    public static void setLightStatusBar(Dialog dialog, boolean enable) {
        if (dialog != null) {
            setLightStatusBar(dialog.getWindow(), enable);
        }
    }


    public static void setLightStatusBar(Window window, boolean enable) {
        final View decorView = window.getDecorView();
        changeSetSystemUiVisibility(decorView, View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR, enable);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            final int statusBarColor = Color.TRANSPARENT;
            if (window.getStatusBarColor() != statusBarColor) {
                window.setStatusBarColor(statusBarColor);
            }
        }
    }

    public static boolean getLightNavigationBar(Window window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final View decorView = window.getDecorView();
            final int flags = decorView.getSystemUiVisibility();
            return (flags & View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR) != 0; // EVE-PATCH: use standard bitwise check
        }
        return false;
    }

    public static void setLightNavigationBar(Dialog dialog, boolean enable) {
        if (dialog != null) {
            setLightNavigationBar(dialog.getWindow(), enable);
        }
    }

    // Activity needs a smarter way to control it.
    public static void setLightNavigationBar(Activity activity, boolean enable) {
        if (activity != null) {
            setLightNavigationBar(activity.getWindow(), enable);
        }
    }


    // do not make public: Use setLightNavigationBar for activity or dialog.
    private static void setLightNavigationBar(Window window, boolean enable) {
        if (window != null) {
            setLightNavigationBar(window.getDecorView(), enable);
        }
    }

    // do not use it: Use setLightNavigationBar for activity or dialog.
    public static void setLightNavigationBar(View view, boolean enable) {
        if (view != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            changeSetSystemUiVisibility(view, View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR, enable);
        }
    }

    public static void setLightStatusBar(View view, boolean enable) {
        if (view != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            changeSetSystemUiVisibility(view, View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR, enable);
        }
    }

    private static void changeSetSystemUiVisibility(@NonNull View view, int flag, boolean enable) {
        final int oldFlags = view.getSystemUiVisibility();
        final int newFlags = enable ? (oldFlags | flag) : (oldFlags & ~flag); // EVE-PATCH: use standard bitwise check
        if (oldFlags != newFlags) {
            view.setSystemUiVisibility(newFlags);
        }
    }

    private static HashMap<Window, ValueAnimator> navigationBarColorAnimators;

    public interface IntColorCallback {
        public void run(int color);
    }

    public static void setNavigationBarColor(Dialog dialog, int color) {
         setNavigationBarColor(dialog, color, true);
    }

    public static void setNavigationBarColor(Dialog dialog, int color, boolean animated) {
        setNavigationBarColor(dialog, color, animated, null);
    }

    public static void setNavigationBarColor(Dialog dialog, int color, boolean animated, IntColorCallback onUpdate) {
        if (dialog != null) {
            setNavigationBarColor(dialog.getWindow(), color, animated, onUpdate);
        }
    }

    @Deprecated
    public static void setNavigationBarColor(Activity activity, int color) {
        setNavigationBarColor(activity, color, true);
    }

    @Deprecated
    public static void setNavigationBarColor(Activity activity, int color, boolean animated) {
        if (activity != null) {
            setNavigationBarColor(activity.getWindow(), color, animated, null);
        }
    }

    private static void setNavigationBarColor(Window window, int color, boolean animated, IntColorCallback onUpdate) {
        if (window == null) {
            return;
        }
        if (navigationBarColorAnimators != null) {
            ValueAnimator animator = navigationBarColorAnimators.get(window);
            if (animator != null) {
                animator.cancel();
                navigationBarColorAnimators.remove(window);
            }
        }

        if (!animated) {
            if (onUpdate != null) {
                onUpdate.run(color);
            }
            try {
                window.setNavigationBarColor(color);
            } catch (Exception ignore) {
            }
        } else {
            ValueAnimator animator = ValueAnimator.ofArgb(window.getNavigationBarColor(), color);
            animator.addUpdateListener(a -> {
                int tcolor = (int) a.getAnimatedValue();
                if (onUpdate != null) {
                    onUpdate.run(tcolor);
                }
                try {
                    window.setNavigationBarColor(tcolor);
                } catch (Exception ignore) {
                }
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (navigationBarColorAnimators != null) {
                        navigationBarColorAnimators.remove(window);
                    }
                }
            });
            animator.setDuration(200);
            animator.setInterpolator(CubicBezierInterpolator.DEFAULT);
            animator.start();
            if (navigationBarColorAnimators == null) {
                navigationBarColorAnimators = new HashMap<>();
            }
            navigationBarColorAnimators.put(window, animator);
        }
    }

    public static boolean checkHostForPunycode(String url) {
        if (url == null) {
            return false;
        }
        boolean hasLatin = false;
        boolean hasNonLatin = false;
        try {
            for (int a = 0, N = url.length(); a < N; a++) {
                char ch = url.charAt(a);
                if (ch == '.' || ch == '-' || ch == '/' || ch == '+' || ch >= '0' && ch <= '9') {
                    continue;
                }
                if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z') {
                    hasLatin = true;
                } else {
                    hasNonLatin = true;
                }
                if (hasLatin && hasNonLatin) {
                    break;
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return hasLatin && hasNonLatin;
    }

    public static boolean shouldShowUrlInAlert(String url) {
        try {
            Uri uri = Uri.parse(url);
            url = uri.getHost();
            return checkHostForPunycode(url);
        } catch (Exception e) {
            FileLog.e(e);
        }
        return false;
    }

    public static boolean checkInlinePermissions(Context context) {
        return Settings.canDrawOverlays(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static boolean isInPictureInPictureMode(Activity activity) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity.isInPictureInPictureMode();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void setPictureInPictureParams(Activity activity, PictureInPictureParams params) {
        if (activity == null || activity.isDestroyed()) {
            return;
        }

        if (params == null) {
            resetPictureInPictureParams(activity);
            return;
        }

        try {
            activity.setPictureInPictureParams(params);
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void resetPictureInPictureParams(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder();
            builder.setSourceRectHint(null);
            builder.setAspectRatio(null);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                //builder.setSeamlessResizeEnabled(true);
                builder.setAutoEnterEnabled(false);
            }

            setPictureInPictureParams(activity, builder.build());
        }
    }

    public static void updateViewLayout(WindowManager windowManager, View view, ViewGroup.LayoutParams params) {
        if (windowManager != null && view != null && view.getParent() != null) {
            windowManager.updateViewLayout(view, params);
        }
    }

    public static void updateImageViewImageAnimated(ImageView imageView, int newIcon) {
        updateImageViewImageAnimated(imageView, ContextCompat.getDrawable(imageView.getContext(), newIcon));
    }

    public static void updateImageViewImageAnimated(ImageView imageView, Drawable newIcon) {
        if (imageView.getDrawable() == newIcon) {
            return;
        }
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1).setDuration(150);
        AtomicBoolean changed = new AtomicBoolean();
        animator.addUpdateListener(animation -> {
            float val = (float) animation.getAnimatedValue();
            float scale = 0.5f + Math.abs(val - 0.5f);
            imageView.setScaleX(scale);
            imageView.setScaleY(scale);
            if (val >= 0.5f && !changed.get()) {
                changed.set(true);
                imageView.setImageDrawable(newIcon);
            }
        });
        animator.start();
    }

    public static void updateViewVisibilityAnimated(View view, boolean show) {
        updateViewVisibilityAnimated(view, show, 1f, true, true);
    }

    public static void updateViewVisibilityAnimated(View view, boolean show, float scaleFactor, boolean animated) {
        updateViewVisibilityAnimated(view, show, scaleFactor, true, animated);
    }

    public static void updateViewVisibilityAnimated(View view, boolean show, float scaleFactor, boolean goneOnHide, boolean animated) {
        updateViewVisibilityAnimated(view, show, scaleFactor, goneOnHide, 1f, animated, null);
    }

    public static void updateViewVisibilityAnimated(View view, boolean show, float scaleFactor, boolean goneOnHide, float maxAlpha, boolean animated, ValueAnimator.AnimatorUpdateListener onUpdate) {
        if (view == null) {
            return;
        }
        if (view.getParent() == null) {
            animated = false;
        }

        if (!animated) {
            view.animate().setListener(null).cancel();
            view.setVisibility(show ? View.VISIBLE : (goneOnHide ? View.GONE : View.INVISIBLE));
            view.setTag(show ? 1 : null);
            view.setAlpha(maxAlpha);
            view.setScaleX(1f);
            view.setScaleY(1f);
        } else if (show && view.getTag() == null) {
            view.animate().setListener(null).cancel();
            if (view.getVisibility() != View.VISIBLE) {
                view.setVisibility(View.VISIBLE);
                view.setAlpha(0f);
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);
            }
            view.animate().alpha(maxAlpha).scaleY(1f).scaleX(1f).setDuration(150).setUpdateListener(onUpdate).start();
            view.setTag(1);
        } else if (!show && view.getTag() != null) {
            view.animate().setListener(null).cancel();
            view.animate().alpha(0).scaleY(scaleFactor).scaleX(scaleFactor).setListener(new HideViewAfterAnimation(view, goneOnHide)).setDuration(150).setUpdateListener(onUpdate).start();
            view.setTag(null);
        }
    }

    public static long getPrefIntOrLong(SharedPreferences preferences, String key, long defaultValue) {
        try {
            return preferences.getLong(key, defaultValue);
        } catch (Exception e) {
            return preferences.getInt(key, (int) defaultValue);
        }
    }

    public static Bitmap getScaledBitmap(float w, float h, String path, String streamPath, int streamOffset) {
        FileInputStream stream = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;

            if (path != null) {
                BitmapFactory.decodeFile(path, options);
            } else {
                stream = new FileInputStream(streamPath);
                stream.getChannel().position(streamOffset);
                BitmapFactory.decodeStream(stream, null, options);
            }
            if (options.outWidth > 0 && options.outHeight > 0) {
                if (w > h && options.outWidth < options.outHeight) {
                    float temp = w;
                    w = h;
                    h = temp;
                }
                float scale = Math.min(options.outWidth / w, options.outHeight / h);
                options.inSampleSize = 1;
                if (scale > 1.0f) {
                    do {
                        options.inSampleSize *= 2;
                    } while (options.inSampleSize < scale);
                }
                options.inJustDecodeBounds = false;
                Bitmap wallpaper;
                if (path != null) {
                    wallpaper = BitmapFactory.decodeFile(path, options);
                } else {
                    stream.getChannel().position(streamOffset);
                    wallpaper = BitmapFactory.decodeStream(stream, null, options);
                }
                return wallpaper;
            }
        } catch (Throwable e) {
            FileLog.e(e);
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (Exception e2) {
                FileLog.e(e2);
            }
        }
        return null;
    }

    public static Uri getBitmapShareUri(Bitmap bitmap, String fileName, Bitmap.CompressFormat format) {
        File cachePath = AndroidUtilities.getCacheDir();
        if (!cachePath.isDirectory()) {
            try {
                cachePath.mkdirs();
            } catch (Exception e) {
                FileLog.e(e);
                return null;
            }
        }
        File file = new File(cachePath, fileName);
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(format, 87, out);
            out.close();
            return FileProvider.getUriForFile(ApplicationLoader.applicationContext, ApplicationLoader.getApplicationId() + ".provider", file);
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }

    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isAccessibilityScreenReaderEnabled() {
        return isAccessibilityTouchExplorationEnabled();
    }

    public static boolean isWhitespace(char c) {
        return Character.isWhitespace(c) || c == '\u2800' || c == '\u3164' || c == '\uffa0';
    }

    public static CharSequence superTrim(CharSequence text) {
        if (text == null) return null;
        int len = text.length();
        int st = 0;
        while (st < len && isWhitespace(text.charAt(st))) st++;
        while (st < len && isWhitespace(text.charAt(len - 1))) len--;
        return (st > 0 || len < text.length()) ? text.subSequence(st, len) : text;
    }

    public static CharSequence trim(CharSequence text, int[] newStart) {
        if (text == null) {
            return null;
        }
        int len = text.length();
        int st = 0;

        while (st < len && text.charAt(st) <= ' ') {
            st++;
        }
        while (st < len && text.charAt(len - 1) <= ' ') {
            len--;
        }
        if (newStart != null) {
            newStart[0] = st;
        }
        return (st > 0 || len < text.length()) ? text.subSequence(st, len) : text;
    }

    // detect Error NO SPaCe left on device :(
    public static boolean isENOSPC(Exception e) {
        return e instanceof IOException && e.getCause() instanceof ErrnoException && ((ErrnoException) e.getCause()).errno == OsConstants.ENOSPC || e.getMessage() != null && e.getMessage().equalsIgnoreCase("no space left on device");
    }

    public static boolean isEROFS(Exception e) {
        return e instanceof IOException && e.getCause() instanceof ErrnoException && ((ErrnoException) e.getCause()).errno == OsConstants.EROFS || e.getMessage() != null && e.getMessage().toLowerCase().contains("read-only file system");
    }

    public static SpannableStringBuilder replaceCharSequence(String what, CharSequence from, CharSequence obj) {
        SpannableStringBuilder spannableStringBuilder;
        if (from instanceof SpannableStringBuilder) {
            spannableStringBuilder = (SpannableStringBuilder) from;
        } else {
            spannableStringBuilder = new SpannableStringBuilder(from);
        }
        int index = TextUtils.indexOf(from, what);
        if (index >= 0) {
            spannableStringBuilder.replace(index, index + what.length(), obj);
        }
        return spannableStringBuilder;
    }

    public static CharSequence replaceMultipleCharSequence(String what, CharSequence from, CharSequence obj) {
        SpannableStringBuilder spannableStringBuilder;
        if (from instanceof SpannableStringBuilder) {
            spannableStringBuilder = (SpannableStringBuilder) from;
        } else {
            spannableStringBuilder = new SpannableStringBuilder(from);
        }
        int index = TextUtils.indexOf(from, what, 0);
        while (index >= 0) {
            spannableStringBuilder.replace(index, index + what.length(), obj);
            index = TextUtils.indexOf(spannableStringBuilder, what, index + 1);
        }
        return spannableStringBuilder;
    }

    public static boolean makingGlobalBlurBitmap;

    // rounds percents to be exact 100% in sum
    public static int[] roundPercents(float[] percents, int[] output) {
        if (percents == null) {
            throw new NullPointerException("percents or output is null");
        }
        if (output == null) {
            output = new int[percents.length];
        }
        if (percents.length != output.length) {
            throw new IndexOutOfBoundsException("percents.length != output.length");
        }

        float sum = 0;
        for (int i = 0; i < percents.length; ++i) {
            sum += percents[i];
        }

        int roundedSum = 0;
        for (int i = 0; i < percents.length; ++i) {
            roundedSum += (output[i] = (int) Math.floor(percents[i] / sum * 100));
        }

        while (roundedSum < 100) {
            float maxError = 0;
            int maxErrorIndex = -1;
            for (int i = 0; i < percents.length; ++i) {
                float error = (percents[i] / sum) - (output[i] / 100f);
                if (percents[i] > 0 && error >= maxError) {
                    maxErrorIndex = i;
                    maxError = error;
                }
            }
            if (maxErrorIndex < 0) {
                break;
            }
            output[maxErrorIndex]++;
            roundedSum++;
        }

        return output;
    }

    public static boolean isRTL(CharSequence text) {
        if (text == null || text.length() <= 0) {
            return false;
        }
        char c;
        for (int i = 0; i < text.length(); ++i) {
            c = text.charAt(i);
            if (c >= 0x590 && c <= 0x6ff) {
                return true;
            }
        }
        return false;
    }

    private static Pattern uriParse;

    public static Pattern getURIParsePattern() {
        if (uriParse == null) {
            uriParse = Pattern.compile("^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?"); // RFC 3986 B
        }
        return uriParse;
    }

    public static String getHostAuthority(String uri) {
        return getHostAuthority(uri, false);
    }

    public static String getHostAuthority(String uri, boolean removeWWW) {
        if (uri == null) {
            return null;
        }
        // CVE-2017-13274
        Matcher matcher = getURIParsePattern().matcher(uri);
        if (matcher.matches()) {
            String authority = matcher.group(4);
            if (authority != null) {
                authority = authority.toLowerCase();
            }
            if (removeWWW && authority != null && authority.startsWith("www.")) {
                authority = authority.substring(4);
            }
            return authority;
        }
        return null;
    }

    public static String getHostAuthority(Uri uri) {
        if (uri == null) {
            return null;
        }
        return getHostAuthority(uri.toString());
    }

    public static String getHostAuthority(Uri uri, boolean removeWWW) {
        if (uri == null) {
            return null;
        }
        return getHostAuthority(uri.toString(), removeWWW);
    }

    public static boolean intersect1d(int x1, int x2, int y1, int y2) {
        return Math.max(x1, x2) > Math.min(y1, y2) && Math.max(y1, y2) > Math.min(x1, x2);
    }

    public static boolean intersect1d(float x1, float x2, float y1, float y2) {
        return Math.max(x1, x2) > Math.min(y1, y2) && Math.max(y1, y2) > Math.min(x1, x2);
    }

    public static boolean intersect1dInclusive(int x1, int x2, int y1, int y2) {
        return Math.max(x1, x2) >= Math.min(y1, y2) && Math.max(y1, y2) >= Math.min(x1, x2);
    }

    public static String getSysInfoString(String path) {
        RandomAccessFile reader = null;
        try {
            reader = new RandomAccessFile(path, "r");
            String line = reader.readLine();
            if (line != null) {
                return line;
            }
        } catch (Exception ignore) {

        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignore) {
                }
            }
        }
        return null;
    }

    public static Long getSysInfoLong(String path) {
        String line = getSysInfoString(path);
        if (line != null) {
            try {
                return Utilities.parseLong(line);
            } catch (Exception e) {
            }
        }
        return null;
    }

    public static boolean isActivityRunning(Activity activity) {
        if (activity == null) {
            return false;
        }

        return !activity.isDestroyed() && !activity.isFinishing();
    }

    public static boolean isSafeToShow(Context context) {
        Activity activity = findActivity(context);
        if (activity == null) return true;
        return isActivityRunning(activity);
    }

    public static int getDominantColor(Bitmap bitmap) {
        if (bitmap == null) {
            return Color.WHITE;
        }
        float stepH = (bitmap.getHeight() - 1) / 10f;
        float stepW = (bitmap.getWidth() - 1) / 10f;
        int r = 0, g = 0, b = 0;
        int amount = 0;
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                int x = (int) (stepW * i);
                int y = (int) (stepH * j);
                int pixel = bitmap.getPixel(x, y);
                if (Color.alpha(pixel) > 200) {
                    r += Color.red(pixel);
                    g += Color.green(pixel);
                    b += Color.blue(pixel);
                    amount++;
                }
            }
        }
        if (amount == 0) {
            return 0;
        }
        return Color.argb(255, r / amount, g / amount, b / amount);
    }

	public static void quietSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {

        }
    }

    public static ByteBuffer cloneByteBuffer(ByteBuffer original) {
        ByteBuffer clone;
        try {
            clone = ByteBuffer.allocate(original.capacity());
        } catch (OutOfMemoryError error) {
            System.gc();
            clone = ByteBuffer.allocate(original.capacity());
        }
        int position = original.position();
        original.rewind();
        clone.put(original);
        original.rewind();
        clone.flip();
        clone.position(position);
        return clone;
    }

    private static Boolean isHonor;
    public static boolean isHonor() {
        if (isHonor == null) {
            try {
                final String brand = Build.BRAND.toLowerCase();
                isHonor = brand.contains("huawei") || brand.contains("honor");
            } catch (Exception e) {
                FileLog.e(e);
                isHonor = false;
            }
        }
        return isHonor;
    }

    public static View findChildViewUnder(ViewGroup parent, float x, float y) {
        if (parent == null) return null;
        if (parent.getVisibility() != View.VISIBLE) return null;
        for (int i = 0; i < parent.getChildCount(); ++i) {
            View child = parent.getChildAt(i);
            if (child == null) continue;
            if (child.getVisibility() != View.VISIBLE) continue;
            if (child instanceof ViewGroup) {
                View foundChild = findChildViewUnder((ViewGroup) child, x - child.getLeft(), y - child.getTop());
                if (foundChild != null) {
                    return foundChild;
                }
            } else if (
                x >= child.getX() && x <= child.getX() + child.getWidth() &&
                y >= child.getY() && x <= child.getY() + child.getHeight()
            ) {
                return child;
            }
        }
        return null;
    }

    public static void vibrateCursor(View view) {
        try {
            if (view == null || view.getContext() == null) return;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
            if (!((Vibrator) view.getContext().getSystemService(Context.VIBRATOR_SERVICE)).hasAmplitudeControl()) return;
            view.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
        } catch (Exception ignore) {}
    }

    public static void vibrate(View view) {
        try {
            if (view == null || view.getContext() == null) return;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
            if (!((Vibrator) view.getContext().getSystemService(Context.VIBRATOR_SERVICE)).hasAmplitudeControl()) return;
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
        } catch (Exception ignore) {}
    }

    public static void applySpring(Animator anim, double stiffness, double damping) {
        applySpring(anim, stiffness, damping, 1, 0);
    }

    public static void applySpring(Animator anim, double stiffness, double damping, double mass) {
        applySpring(anim, stiffness, damping, mass, 0);
    }

    public static void applySpring(Animator anim, double stiffness, double damping, double mass, double initialVelocity) {
        final double w0 = Math.sqrt(stiffness / mass);
        final double zeta = damping / (2.0 * Math.sqrt(stiffness * mass));
        final double wd, A, B;
        if (zeta < 1) {
            wd = w0 * Math.sqrt(1.0 - zeta * zeta);
            A = 1.0;
            B = (zeta * w0 + -initialVelocity) / wd;
        } else {
            wd = 0.0;
            A = 1.0;
            B = -initialVelocity + w0;
        }
        final double threshold = 0.0025;
        final double duration = Math.log(threshold) / (-zeta * w0);
        anim.setDuration((long) (duration * 1000L));
        anim.setInterpolator(new Interpolator() {
            @Override
            public float getInterpolation(float t) {
                if (zeta < 1) {
                    return (float) (1.0 - Math.exp(-t * zeta * w0) * (A * Math.cos(wd * t) + B * Math.sin(wd * t)));
                } else {
                    return (float) (1.0 - (A + B * t) * Math.exp(-t * w0));
                }
            }
        });
    }

    public static void applySpring(Animator anim, float stiffness, float damping, float mass, long overrideDuration) {
        final double zeta = damping / (2 * Math.sqrt(stiffness * mass));
        final double omega = Math.sqrt(stiffness / mass);
        final double threshold = 0.0025;
        anim.setDuration(overrideDuration);
        anim.setInterpolator(new Interpolator() {
            @Override
            public float getInterpolation(float t) {
                if (zeta < 1) {
                    final double dampedFrequency = omega * Math.sqrt(1 - zeta * zeta);
                    return (float) (1 - Math.exp(-zeta * omega * t) *
                            (Math.cos(dampedFrequency * t) + (zeta * omega / dampedFrequency) * Math.sin(dampedFrequency * t)));
                } else {
                    final double a = -zeta * omega * t;
                    return (float) (1 - (1 + a) * Math.exp(a));
                }
            }
        });
    }

    public static CharSequence removeSpans(CharSequence text, Class spanClass) {
        if (!(text instanceof Spannable)) return text;
        final Spannable spannable = (Spannable) text;
        final Object[] spans = spannable.getSpans(0, spannable.length(), spanClass);
        for (int i = 0; i < spans.length; ++i) {
            spannable.removeSpan(spans[i]);
        }
        return spannable;
    }

    public static void notifyDataSetChanged(RecyclerView listView) {
        if (listView == null) return;
        if (listView.getAdapter() == null) return;
        if (listView.isComputingLayout()) {
            listView.post(() -> {
                if (listView.getAdapter() != null) {
                    listView.getAdapter().notifyDataSetChanged();
                }
            });
        } else {
            listView.getAdapter().notifyDataSetChanged();
        }
    }

    public static void doOnPreDraw(@NonNull View view, @NonNull Runnable action) {
        final ViewTreeObserver observer = view.getViewTreeObserver();

        ViewTreeObserver.OnPreDrawListener[] listenerHolder = new ViewTreeObserver.OnPreDrawListener[1];
        boolean[] completed = new boolean[1];
        listenerHolder[0] = () -> {
            if (observer.isAlive()) {
                observer.removeOnPreDrawListener(listenerHolder[0]);
            }
            if (!completed[0]) {
                completed[0] = true;
                action.run();
            }
            return true;
        };
        observer.addOnPreDrawListener(listenerHolder[0]);
    }

    public static boolean isInAirplaneMode(Context context) {
        try {
            return Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        } catch (Exception ignore) {
            return false;
        }
    }

    public static boolean isWifiEnabled(Context context) {
        try {
            WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            return wm != null && wm.isWifiEnabled();
        } catch (Exception ignore) {
            return false;
        }
    }

    public static boolean gzip(File input, File output) {
        try (
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(input));
            GZIPOutputStream out = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(output)))
        ) {
            byte[] buffer = new byte[8 * 1024];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            return true;
        } catch (FileNotFoundException e) {
            FileLog.e(e);
        } catch (IOException e) {
            FileLog.e(e);
        }
        return false;
    }

    @Nullable
    public static <T> T randomOf(ArrayList<T> array) {
        if (array.isEmpty()) return null;
        return array.get(Math.abs(Utilities.fastRandom.nextInt() % array.size()));
    }

    public static float getNavigationBarThirdButtonsFactor(int height) {
        return Utilities.clamp01(((float) height - dp(32)) / dp(16));
    }

    public static float getNavigationBarThirdButtonsFactor(float min, float max, int height) {
        return lerp(min, max, getNavigationBarThirdButtonsFactor(height));
    }

    private static final Paint navbarProtactionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public static void drawNavigationBarProtection(Canvas canvas, View view, int color, int navigationBarHeight) {
        drawNavigationBarProtection(canvas, view, color, navigationBarHeight, 1);
    }

    public static void drawNavigationBarProtection(Canvas canvas, View view, int color, int navigationBarHeight, float alpha) {
        navbarProtactionPaint.setColor(Theme.multAlpha(color, alpha * AndroidUtilities.getNavigationBarThirdButtonsFactor(0, 0.75f, navigationBarHeight)));
        // bug? : view.getY() is unnecessary
        canvas.drawRect(
                0, view.getY() + view.getMeasuredHeight() - navigationBarHeight,
                view.getMeasuredWidth(), view.getY() + view.getMeasuredHeight(), navbarProtactionPaint);
    }




    /**
     * Fix for Build.VERSION.SDK_INT < Build.VERSION_CODES.R
     */
    @NonNull
    public static WindowInsets fixedDispatchApplyWindowInsets(@NonNull WindowInsets insets, ViewGroup view) {
        for (int a = 0, N = view.getChildCount(); a < N; a++) {
            final View child = view.getChildAt(a);
            child.dispatchApplyWindowInsets(insets);
        }
        return insets;
    }




    public static void enableEdgeToEdge(Activity activity) {
        final Window window = activity.getWindow();
        try {
            AndroidUtilities.enableEdgeToEdge(window);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.setNavigationBarDividerColor(0);
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    /**
     * From androidx.core:core:1.17.0
     * todo: replace to WindowCompat.enableEdgeToEdge() when update
     */
    public static void enableEdgeToEdge(@NonNull Window window) {
        Objects.requireNonNull(window);

        // This triggers the initialization of the decor view here to prevent the attributes set by
        // this method from getting overwritten by the initialization later.
        window.getDecorView();

        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 28) {
            final int newMode = Build.VERSION.SDK_INT >= 30
                    ? WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    : WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            final WindowManager.LayoutParams attrs = window.getAttributes();
            if (attrs.layoutInDisplayCutoutMode != newMode) {
                attrs.layoutInDisplayCutoutMode = newMode;
                window.setAttributes(attrs);
            }
        }
        if (Build.VERSION.SDK_INT >= 29) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }
    }

    public static void applyEdgeToEdgeLayoutParams(WindowManager.LayoutParams windowLayoutParams) {
        if (Build.VERSION.SDK_INT >= 28) {
            windowLayoutParams.layoutInDisplayCutoutMode = Build.VERSION.SDK_INT >= 30
                    ? WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    : WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
    }



    public static Insets getDefaultWindowInsets(WindowInsetsCompat insets, boolean withIme) {
        final int insetsType = WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout();
        final Insets systemInsets = Insets.max(
            insets.getInsetsIgnoringVisibility(insetsType),
            insets.getInsets(insetsType));

        if (withIme) {
            final Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            return Insets.max(systemInsets, imeInsets);
        }

        return systemInsets;
    }

    public static void setViewLayoutMargins(View v, int l, int t, int r, int b) {
        if (v == null) {
            return;
        }

        final ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            final ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;

            if (mlp.leftMargin != l || mlp.topMargin != t || mlp.rightMargin != r || mlp.bottomMargin != b) {
                mlp.leftMargin = l;
                mlp.topMargin = t;
                mlp.rightMargin = r;
                mlp.bottomMargin = b;
                v.requestLayout();
            }
        }
    }

    public static void drawStroke(Canvas canvas, RectF rect, float radius) {
        drawStroke(canvas, rect, radius, 1.0F);
    }
    public static void drawStroke(Canvas canvas, RectF rect, float radius, float alpha) {
        if (alpha <= 0) return;

        radius = Math.min(radius, Math.min(rect.width(), rect.height()) / 2f);

        {
            final float stroke = dpf2(1);
            final float strokeHalf = stroke / 2;
            final float r = Math.max(0, radius - stroke);
            strokeTop.setStyle(Paint.Style.STROKE);
            strokeTop.setStrokeWidth(stroke);
            strokeTop.setColor(Theme.multAlpha(0x21FFFFFF, alpha));
            canvas.save();
            if (canvas.clipRect(rect.left - strokeHalf, rect.top, rect.right + strokeHalf, MathUtils.clamp(rect.top + r, rect.top, rect.bottom))) {
                canvas.drawRoundRect(
                    rect.left,
                    rect.top + strokeHalf,
                    rect.right,
                    rect.bottom + strokeHalf,
                    r, r,
                    strokeTop
                );
            }
            canvas.restore();
        }

        {
            final float stroke = dpf2(2 / 3f);
            final float strokeHalf = stroke / 2;
            final float r = Math.max(0, radius - stroke);
            strokeBottom.setStyle(Paint.Style.STROKE);
            strokeBottom.setStrokeWidth(stroke);
            strokeBottom.setColor(Theme.multAlpha(0x16FFFFFF, alpha));
            canvas.save();
            if (canvas.clipRect(rect.left - strokeHalf, MathUtils.clamp(rect.bottom - r, rect.top, rect.bottom), rect.right + strokeHalf, rect.bottom)) {
                canvas.drawRoundRect(
                    rect.left,
                    rect.top - strokeHalf,
                    rect.right,
                    rect.bottom - strokeHalf,
                    r, r,
                    strokeBottom
                );
            }
            canvas.restore();
        }
    }

    public static long pack(int a, int b) { return ((long) a << 32) | (b & 0xFFFFFFFFL); }
    public static int unpackA(long packed) { return (int) (packed >> 32); }
    public static int unpackB(long packed) { return (int) packed; }

    public static boolean isContextSafe(Context context) {
        if (context == null) return false;
        if (context instanceof Activity) {
            final Activity activity = (Activity) context;
            if (activity.isFinishing()) return false;
            if (activity.isDestroyed()) return false;
            return true;
        }
        if (context instanceof ContextWrapper) {
            final Context baseContext = ((ContextWrapper) context).getBaseContext();
            return isContextSafe(baseContext);
        }
        return true;
    }

    public static Bitmap applyColorMatrix(Bitmap bitmap, ColorMatrix matrix) {
        final Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(matrix));
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));

        final Bitmap result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(result);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return result;
    }

    public static int applyColorMatrix(int argb, ColorMatrix matrix) {
        float[] m = matrix.getArray();

        int A = Color.alpha(argb);
        int R = Color.red(argb);
        int G = Color.green(argb);
        int B = Color.blue(argb);

        float r = m[0]  * R + m[1]  * G + m[2]  * B + m[3]  * A + m[4];
        float g = m[5]  * R + m[6]  * G + m[7]  * B + m[8]  * A + m[9];
        float b = m[10] * R + m[11] * G + m[12] * B + m[13] * A + m[14];
        float a = m[15] * R + m[16] * G + m[17] * B + m[18] * A + m[19];

        int R2 = MathUtils.clamp(Math.round(r), 0, 255);
        int G2 = MathUtils.clamp(Math.round(g), 0, 255);
        int B2 = MathUtils.clamp(Math.round(b), 0, 255);
        int A2 = MathUtils.clamp(Math.round(a), 0, 255);

        return Color.argb(A2, R2, G2, B2);
    }

    public static void createCalendarEvent(Activity activity, long timestampMillis,
                                           String title, String description, boolean allDay) {
        final long beginMillis, endMillis;
        if (allDay) {
            // normalize timestamp and set 1 day duration

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timestampMillis);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            beginMillis = calendar.getTimeInMillis();
            endMillis = beginMillis + TimeUnit.DAYS.toMillis(1);
        } else {
            // set 10 minutes duration by default

            beginMillis = timestampMillis;
            endMillis = beginMillis + TimeUnit.MINUTES.toMillis(10);
        }

        final Intent intent = new Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginMillis)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            .putExtra(CalendarContract.Events.ALL_DAY, allDay);

        if (!TextUtils.isEmpty(title)) {
            intent.putExtra(CalendarContract.Events.TITLE, title);
        }

        if (!TextUtils.isEmpty(description)) {
            intent.putExtra(CalendarContract.Events.DESCRIPTION, description);
        }

        try {
            activity.startActivity(intent);
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    public static <A, B> B find(List<A> array, Class<B> clazz) {
        if (array == null) {
            return null;
        }
        for (int i = 0; i < array.size(); ++i) {
            final A obj = array.get(i);
            if (clazz.isInstance(obj)) {
                return clazz.cast(obj);
            }
        }
        return null;
    }

    public static <A, B> B findLast(List<A> array, Class<B> clazz) {
        if (array == null) {
            return null;
        }
        for (int i = array.size() - 1; i >= 0; --i) {
            final A obj = array.get(i);
            if (clazz.isInstance(obj)) {
                return clazz.cast(obj);
            }
        }
        return null;
    }

}
