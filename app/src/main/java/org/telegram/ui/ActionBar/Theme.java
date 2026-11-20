/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.ActionBar;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.dpf2;
import static org.telegram.messenger.LocaleController.getString;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.RoundRectShape;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.LongSparseArray;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.StateSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.core.math.MathUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.Cells.BaseCell;
import org.telegram.ui.Components.CombinedDrawable;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

public class Theme {

    public static final String DEFAULT_BACKGROUND_SLUG = "d";
    public static final String THEME_BACKGROUND_SLUG = "t";
    public static final String COLOR_BACKGROUND_SLUG = "c";

    public static final int MSG_OUT_COLOR_BLACK = 0xff212121;
    public static final int MSG_OUT_COLOR_WHITE = 0xffffffff;
    public static final int default_shadow_color = ColorUtils.setAlphaComponent(Color.BLACK, 27);
    public static boolean disallowChangeServiceMessageColor;

    public static void applyDefaultShadow(Paint paint) {
        paint.setShadowLayer(dpf2(1), 0, dpf2(0.33f), default_shadow_color);
    }

    public static Paint getThemePaint(String key, ResourcesProvider resourcesProvider) {
        if (resourcesProvider != null) {
            final Paint paint = resourcesProvider.getPaint(key);
            if (paint != null) {
                return paint;
            }
        }
        return getThemePaint(key);
    }

    public static ColorFilter getAnimatedEmojiColorFilter(ResourcesProvider resourcesProvider) {
        if (resourcesProvider != null) {
            return resourcesProvider.getAnimatedEmojiColorFilter();
        }
        return Theme.chat_animatedEmojiTextColorFilter;
    }

    public static class ThemeAccent {
        public int id;

        public ThemeInfo parentTheme;

        public int accentColor;
        public int accentColor2;
        public int myMessagesAccentColor;
        public int myMessagesGradientAccentColor1;
        public int myMessagesGradientAccentColor2;
        public int myMessagesGradientAccentColor3;
        public boolean myMessagesAnimated;
        public long backgroundOverrideColor;
        public long backgroundGradientOverrideColor1;
        public long backgroundGradientOverrideColor2;
        public long backgroundGradientOverrideColor3;
        public int backgroundRotation = 45;
        public String patternSlug = "";
        public float patternIntensity;
        public boolean patternMotion;

        public int account;

        public String pathToFile;
        public String uploadingThumb;
        public String uploadingFile;

        public boolean isDefault;

        ThemeAccent() {

        }

        // a color of the table being built, not of the active theme
        private static int colorInTable(SparseIntArray colors, int key) {
            int index = colors.indexOfKey(key);
            if (index < 0) {
                final int fallbackKey = fallbackKeys.get(key, -1);
                if (fallbackKey >= 0) {
                    index = colors.indexOfKey(fallbackKey);
                }
            }
            return index >= 0 ? colors.valueAt(index) : defaultColors[key];
        }

        public boolean fillAccentColors(SparseIntArray currentColorsNoAccent, SparseIntArray currentColors) {
            boolean isMyMessagesGradientColorsNear = false;

            float[] hsvTemp1 = getTempHsv(1);
            float[] hsvTemp2 = getTempHsv(2);

            Color.colorToHSV(parentTheme.accentBaseColor, hsvTemp1);
            Color.colorToHSV(accentColor, hsvTemp2);
            boolean isDarkTheme = parentTheme.isDark();

            if (accentColor != parentTheme.accentBaseColor || accentColor2 != 0) {
                for (int i = 0; i < defaultColors.length; i++) {
                    int key = i;
                    if (themeAccentExclusionKeys.contains(key)) {
                        continue;
                    }
                    int index = currentColorsNoAccent.indexOfKey(key);
                    int color;
                    if (index < 0) {
                        int fallbackKey = fallbackKeys.get(key, -1);
                        if (fallbackKey >= 0 && currentColorsNoAccent.indexOfKey(fallbackKey) >= 0) {
                            continue;
                        }
                        color = defaultColors[key];
                    } else {
                        color = currentColorsNoAccent.valueAt(index);
                    }
                    int newColor = changeColorAccent(hsvTemp1, hsvTemp2, color, isDarkTheme, color);
                    if (newColor != color) {
                        currentColors.put(key, newColor);
                    }
                }
            }
            int myMessagesAccent = myMessagesAccentColor;
            if ((myMessagesAccentColor != 0 || accentColor != 0) && myMessagesGradientAccentColor1 != 0) {
                int firstColor = myMessagesAccentColor != 0 ? myMessagesAccentColor : accentColor;
                int color = currentColorsNoAccent.get(key_chat_outBubble);
                if (color == 0) {
                    color = defaultColors[key_chat_outBubble];
                }
                int newColor = changeColorAccent(hsvTemp1, hsvTemp2, color, isDarkTheme, color);
                int distance1 = AndroidUtilities.getColorDistance(firstColor, newColor);
                int distance2 = AndroidUtilities.getColorDistance(firstColor, myMessagesGradientAccentColor1);

                boolean useBlackText;
                if (myMessagesGradientAccentColor2 != 0) {
                    int averageColor = AndroidUtilities.getAverageColor(myMessagesAccentColor, myMessagesGradientAccentColor1);
                    averageColor = AndroidUtilities.getAverageColor(averageColor, myMessagesGradientAccentColor2);
                    if (myMessagesGradientAccentColor3 != 0) {
                        averageColor = AndroidUtilities.getAverageColor(averageColor, myMessagesGradientAccentColor3);
                    }
                    useBlackText = AndroidUtilities.computePerceivedBrightness(averageColor) > 0.705f;
                } else {
                    useBlackText = useBlackText(myMessagesAccentColor, myMessagesGradientAccentColor1);
                }
                if (useBlackText) {
                    isMyMessagesGradientColorsNear = distance1 <= 35000 && distance2 <= 35000;
                } else {
                    isMyMessagesGradientColorsNear = false;
                }
                myMessagesAccent = getAccentColor(hsvTemp1, color, firstColor);
            }
            boolean changeMyMessagesColors = (myMessagesAccent != 0 && (parentTheme.accentBaseColor != 0 && myMessagesAccent != parentTheme.accentBaseColor || accentColor != 0 && accentColor != myMessagesAccent));
            if (changeMyMessagesColors || accentColor2 != 0) {
                if (accentColor2 != 0) {
                    Color.colorToHSV(accentColor2, hsvTemp2);
                } else {
                    Color.colorToHSV(myMessagesAccent, hsvTemp2);
                }

                for (int key = myMessagesStartIndex; key < myMessagesEndIndex; key++) {
                    int index = currentColorsNoAccent.indexOfKey(key);
                    int color;
                    if (index < 0) {
                        int fallbackKey = fallbackKeys.get(key, -1);
                        if (fallbackKey >= 0 && currentColorsNoAccent.get(fallbackKey, -1) >= 0) {
                            continue;
                        }
                        color = defaultColors[key];
                    } else {
                        color = currentColorsNoAccent.valueAt(index);
                    }
                    int newColor = changeColorAccent(hsvTemp1, hsvTemp2, color, isDarkTheme, color);
                    if (newColor != color) {
                        currentColors.put(key, newColor);
                    }
                }
                for (int key : myMessagesAccentExtraKeys) {
                    final int index = currentColorsNoAccent.indexOfKey(key);
                    final int color = index < 0 ? defaultColors[key] : currentColorsNoAccent.valueAt(index);
                    final int newColor = changeColorAccent(hsvTemp1, hsvTemp2, color, isDarkTheme, color);
                    if (newColor != color) {
                        currentColors.put(key, newColor);
                    }
                }

                if (changeMyMessagesColors) {
                    Color.colorToHSV(myMessagesAccent, hsvTemp2);
                    for (int key = myMessagesBubblesStartIndex; key < myMessagesBubblesEndIndex; key++) {
                        int index = currentColorsNoAccent.indexOfKey(key);
                        int color;
                        if (index < 0) {
                            int fallbackKey = fallbackKeys.get(key, -1);
                            if (fallbackKey >= 0 && currentColorsNoAccent.get(fallbackKey, -1) >= 0) {
                                continue;
                            }
                            color = defaultColors[key];
                        } else {
                            color = currentColorsNoAccent.valueAt(index);
                        }
                        int newColor = changeColorAccent(hsvTemp1, hsvTemp2, color, isDarkTheme, color);
                        if (newColor != color) {
                            currentColors.put(key, newColor);
                        }
                    }
                }
            }
            if (!isMyMessagesGradientColorsNear) {
                if (myMessagesGradientAccentColor1 != 0) {
                    int textColor;
                    int subTextColor;
                    int seekbarColor;
                    boolean useBlackText;
                    if (myMessagesGradientAccentColor2 != 0) {
                        int color = AndroidUtilities.getAverageColor(myMessagesAccentColor, myMessagesGradientAccentColor1);
                        color = AndroidUtilities.getAverageColor(color, myMessagesGradientAccentColor2);
                        if (myMessagesGradientAccentColor3 != 0) {
                            color = AndroidUtilities.getAverageColor(color, myMessagesGradientAccentColor3);
                        }
                        useBlackText = AndroidUtilities.computePerceivedBrightness(color) > 0.705f;
                    } else {
                        useBlackText = useBlackText(myMessagesAccentColor, myMessagesGradientAccentColor1);
                    }
                    if (useBlackText) {
                        textColor = MSG_OUT_COLOR_BLACK;
                        subTextColor = 0xff555555;
                        seekbarColor = 0x4d000000;
                    } else {
                        textColor = MSG_OUT_COLOR_WHITE;
                        subTextColor = 0xffeeeeee;
                        seekbarColor = 0x4dffffff;
                    }

                    if (accentColor2 == 0) {
                        currentColors.put(key_chat_outAudioProgress, seekbarColor);
                        currentColors.put(key_chat_outAudioSelectedProgress, seekbarColor);
                        currentColors.put(key_chat_outAudioSeekbar, seekbarColor);
                        currentColors.put(key_chat_outAudioCacheSeekbar, seekbarColor);
                        currentColors.put(key_chat_outAudioSeekbarSelected, seekbarColor);
                        currentColors.put(key_chat_outAudioSeekbarFill, textColor);

                        currentColors.put(key_chat_outVoiceSeekbar, seekbarColor);
                        currentColors.put(key_chat_outVoiceSeekbarSelected, seekbarColor);
                        currentColors.put(key_chat_outVoiceSeekbarFill, textColor);

                        currentColors.put(key_chat_messageLinkOut, textColor);
                        currentColors.put(key_chat_outForwardedNameText, textColor);
                        currentColors.put(key_chat_outViaBotNameText, textColor);
                        currentColors.put(key_chat_outReplyLine, textColor);
                        currentColors.put(key_chat_outReplyLine2, textColor);
                        currentColors.put(key_chat_outReplyNameText, textColor);

                        currentColors.put(key_chat_outPreviewLine, textColor);
                        currentColors.put(key_chat_outSiteNameText, textColor);
                        currentColors.put(key_chat_outInstant, textColor);
                        currentColors.put(key_chat_outInstantSelected, textColor);
                        currentColors.put(key_chat_outPreviewInstantText, textColor);

                        currentColors.put(key_chat_outViews, textColor);
                        currentColors.put(key_chat_outViewsSelected, textColor);

                        currentColors.put(key_chat_outAudioTitleText, textColor);
                        currentColors.put(key_chat_outFileNameText, textColor);
                        currentColors.put(key_chat_outContactNameText, textColor);

                        currentColors.put(key_chat_outAudioPerformerText, textColor);
                        currentColors.put(key_chat_outAudioPerformerSelectedText, textColor);

                        currentColors.put(key_chat_outSentCheck, textColor);
                        currentColors.put(key_chat_outSentCheckSelected, textColor);

                        currentColors.put(key_chat_outSentCheckRead, textColor);
                        currentColors.put(key_chat_outSentCheckReadSelected, textColor);

                        currentColors.put(key_chat_outSentClock, textColor);
                        currentColors.put(key_chat_outSentClockSelected, textColor);

                        currentColors.put(key_chat_outMenu, textColor);
                        currentColors.put(key_chat_outMenuSelected, textColor);

                        currentColors.put(key_chat_outTimeText, textColor);
                        currentColors.put(key_chat_outTimeSelectedText, textColor);

                        currentColors.put(key_chat_outAudioDurationText, subTextColor);
                        currentColors.put(key_chat_outAudioDurationSelectedText, subTextColor);

                        currentColors.put(key_chat_outContactPhoneText, subTextColor);
                        currentColors.put(key_chat_outContactPhoneSelectedText, subTextColor);

                        currentColors.put(key_chat_outFileInfoText, subTextColor);
                        currentColors.put(key_chat_outFileInfoSelectedText, subTextColor);

                        currentColors.put(key_chat_outVenueInfoText, subTextColor);
                        currentColors.put(key_chat_outVenueInfoSelectedText, subTextColor);


                        currentColors.put(key_chat_outLoader, textColor);
                        currentColors.put(key_chat_outLoaderSelected, textColor);
                        currentColors.put(key_chat_outFileProgress, myMessagesAccentColor);
                        currentColors.put(key_chat_outFileProgressSelected, myMessagesAccentColor);
                        currentColors.put(key_chat_outMediaIcon, myMessagesAccentColor);
                        currentColors.put(key_chat_outMediaIconSelected, myMessagesAccentColor);
                    }

                    currentColors.put(key_chat_outReplyMessageText, textColor);
                    currentColors.put(key_chat_outReplyMediaMessageText, textColor);
                    currentColors.put(key_chat_outReplyMediaMessageSelectedText, textColor);
                    currentColors.put(key_chat_messageTextOut, textColor);
                }
            }
            if (isMyMessagesGradientColorsNear) {
                int outColor = currentColors.indexOfKey(key_chat_outLoader) >= 0
                        ? currentColors.get(key_chat_outLoader)
                        : Color.TRANSPARENT;
                if (AndroidUtilities.getColorDistance(0xffffffff, outColor) < 5000) {
                    isMyMessagesGradientColorsNear = false;
                }
            }
            if (myMessagesAccentColor != 0 && myMessagesGradientAccentColor1 != 0) {
                currentColors.put(key_chat_outBubble, myMessagesAccentColor);
                currentColors.put(key_chat_outBubbleGradient1, myMessagesGradientAccentColor1);
                if (myMessagesGradientAccentColor2 != 0) {
                    currentColors.put(key_chat_outBubbleGradient2, myMessagesGradientAccentColor2);
                    if (myMessagesGradientAccentColor3 != 0) {
                        currentColors.put(key_chat_outBubbleGradient3, myMessagesGradientAccentColor3);
                    }
                }
                currentColors.put(key_chat_outBubbleGradientAnimated, myMessagesAnimated ? 1 : 0);
            }
            int backgroundOverride = (int) backgroundOverrideColor;
            if (backgroundOverride != 0) {
                currentColors.put(key_chat_wallpaper, backgroundOverride);
            } else if (backgroundOverrideColor != 0) {
                currentColors.delete(key_chat_wallpaper);
            }
            int backgroundGradientOverride1 = (int) backgroundGradientOverrideColor1;
            if (backgroundGradientOverride1 != 0) {
                currentColors.put(key_chat_wallpaper_gradient_to1, backgroundGradientOverride1);
            } else if (backgroundGradientOverrideColor1 != 0) {
                currentColors.delete(key_chat_wallpaper_gradient_to1);
            }
            int backgroundGradientOverride2 = (int) backgroundGradientOverrideColor2;
            if (backgroundGradientOverride2 != 0) {
                currentColors.put(key_chat_wallpaper_gradient_to2, backgroundGradientOverride2);
            } else if (backgroundGradientOverrideColor2 != 0) {
                currentColors.delete(key_chat_wallpaper_gradient_to2);
            }
            int backgroundGradientOverride3 = (int) backgroundGradientOverrideColor3;
            if (backgroundGradientOverride3 != 0) {
                currentColors.put(key_chat_wallpaper_gradient_to3, backgroundGradientOverride3);
            } else if (backgroundGradientOverrideColor3 != 0) {
                currentColors.delete(key_chat_wallpaper_gradient_to3);
            }
            if (backgroundRotation != 45) {
                currentColors.put(key_chat_wallpaper_gradient_rotation, backgroundRotation);
            }

            int outBubble = currentColors.get(key_chat_outBubble);
            if (outBubble == 0) {
                outBubble = getColorFromTable(currentColors, key_chat_outBubble, null);
            }
            int inBubble = currentColors.get(key_chat_inBubble);
            if (inBubble == 0) {
                inBubble = getColorFromTable(currentColors, key_chat_inBubble, null);
            }

            if (accentColor != parentTheme.accentBaseColor || myMessagesAccentColor != 0) { // EVE-PATCH: decouple info from accent bubble check
                // compact mode's outgoing keys are neutral, so the hue shift above leaves them as is:
                // follow the accent the way the regular bubble does
                final int replyLine = colorInTable(currentColors, key_chat_outReplyLine);
                currentColors.put(key_chat_compact_outBackground, outBubble | 0xFF000000);
                currentColors.put(key_chat_compact_outTimeText, colorInTable(currentColors, key_chat_outTimeText));
                currentColors.put(key_chat_compact_outCheck, colorInTable(currentColors, key_chat_outSentCheckRead));
                currentColors.put(key_chat_compact_outReplyLine, replyLine);
                currentColors.put(key_chat_compact_outReplyText, colorInTable(currentColors, key_chat_outReplyNameText));
                currentColors.put(key_chat_compact_outReplyBackground, Theme.multAlpha(replyLine, .10f));
                currentColors.put(key_chat_compact_outAccent, colorInTable(currentColors, key_chat_outFileNameText));
                currentColors.put(key_chat_compact_outAccentFill, colorInTable(currentColors, key_chat_outLoader));
                currentColors.put(key_chat_compact_outAccentIcon, colorInTable(currentColors, key_chat_outMediaIcon));
                currentColors.put(key_chat_compact_outSecondaryText, colorInTable(currentColors, key_chat_outFileInfoText));
                currentColors.put(key_chat_compact_outAccentLight, colorInTable(currentColors, key_chat_outFileBackground));
                currentColors.put(key_chat_compact_outAccentBorder, colorInTable(currentColors, key_chat_outTableBorder));
            }

            int gradientAverageColor = outBubble;

            if (!isDarkTheme) {
                currentColors.put(key_chat_inTextSelectionHighlight, textSelectionBackground(false, inBubble, accentColor));
                currentColors.put(key_chat_outTextSelectionHighlight, textSelectionBackground(true, outBubble, accentColor));
                currentColors.put(key_chat_outTextSelectionCursor, textSelectionHandle(outBubble, accentColor));
            }

            float accentHue = getHue(getColorFromTable(currentColors, key_windowBackgroundWhiteBlueText, null));
            currentColors.put(key_chat_outBubbleLocationPlaceholder, locationPlaceholderColor(accentHue, outBubble, isDarkTheme));
            currentColors.put(key_chat_inBubbleLocationPlaceholder, locationPlaceholderColor(accentHue, inBubble, isDarkTheme));

            int inMsgLink = currentColors.get(key_chat_messageLinkIn);
            if (inMsgLink == 0) {
                inMsgLink = getColorFromTable(currentColors, key_chat_messageLinkIn, null);
            }
            int outMsgLink = currentColors.get(key_chat_messageLinkOut);
            if (outMsgLink == 0) {
                outMsgLink = getColorFromTable(currentColors, key_chat_messageLinkOut, null);
            }
            currentColors.put(key_chat_linkSelectBackground, linkSelectionBackground(inMsgLink, inBubble, isDarkTheme));
            currentColors.put(key_chat_outLinkSelectBackground, linkSelectionBackground(outMsgLink, outBubble, isDarkTheme));

            int submenuBackground = currentColors.get(key_actionBarDefaultSubmenuBackground);
            if (submenuBackground == 0) {
                submenuBackground = getColorFromTable(currentColors, key_actionBarDefaultSubmenuBackground, null);
            }
            currentColors.put(key_actionBarDefaultSubmenuSeparator, Color.argb(
                Color.alpha(submenuBackground),
                Math.max(0, Color.red(submenuBackground) - 10),
                Math.max(0, Color.green(submenuBackground) - 10),
                Math.max(0, Color.blue(submenuBackground) - 10)
            ));

            if (isDarkTheme && currentColors.get(key_chat_outBubbleGradient1) != 0) {
                int outBubbleAverage = averageColor(currentColors, key_chat_outBubbleGradient1, key_chat_outBubbleGradient2, key_chat_outBubbleGradient3);
                Color.colorToHSV(outBubbleAverage, tempHSV);
                tempHSV[1] = Utilities.clamp(tempHSV[1] + .1f, 1, 0);
                tempHSV[2] = Utilities.clamp(tempHSV[2] - .8f, 1, 0);
                currentColors.put(key_chat_outCodeBackground, Color.HSVToColor(0x40, tempHSV));
            } else {
                currentColors.put(key_chat_outCodeBackground, codeBackground(outBubble, isDarkTheme));
            }
            applyCalculatedTableColors(currentColorsNoAccent, currentColors, isDarkTheme);
            applyCalculatedArticleCodeColors(currentColorsNoAccent, currentColors, isDarkTheme);

            return !isMyMessagesGradientColorsNear;
        }

        public void resetAccentColorsForMyMessagesGiftThemeLight(SparseIntArray currentColors) {
            for (int a = Theme.myMessagesBubblesStartIndex; a < Theme.myMessagesBubblesEndIndex; a++) {
                currentColors.delete(a);
                currentColors.put(a, defaultColors[a]);
            }
            for (int a = Theme.myMessagesStartIndex; a < Theme.myMessagesEndIndex; a++) {
                currentColors.delete(a);
                currentColors.put(a, defaultColors[a]);
            }
            for (int a = Theme.myMessages2StartIndex; a < Theme.myMessages2EndIndex; a++) {
                currentColors.delete(a);
                currentColors.put(a, defaultColors[a]);
            }
        }


        private float[] tempHSV = new float[3];
        private int setHue(int color, int hueFromColor) {
            Color.colorToHSV(hueFromColor, tempHSV);
            float hue = tempHSV[0];
            Color.colorToHSV(color, tempHSV);
            if (tempHSV[1] > 0.02f) {
                tempHSV[0] = hue;
            }
            return Color.HSVToColor(Color.alpha(color), tempHSV);
        }
        private float getHue(int color) {
            Color.colorToHSV(color, tempHSV);
            return tempHSV[0];
        }
        private int bubbleSelectedOverlay(int bubble, int accentColor) {
            Color.colorToHSV(accentColor, tempHSV);
            float h = tempHSV[0];
            Color.colorToHSV(bubble, tempHSV);
            if (tempHSV[1] <= 0) {
                tempHSV[0] = h;
            }
            tempHSV[1] = Math.max(0, Math.min(1, tempHSV[1] + .6f));
            tempHSV[2] = Math.max(0, Math.min(1, tempHSV[2] - .05f));
            return Color.HSVToColor(30, tempHSV);
        }
        private int textSelectionBackground(boolean isOut, int bubble, int accentColor) {
            Color.colorToHSV(accentColor, tempHSV);
            float h = tempHSV[0];
            Color.colorToHSV(bubble, tempHSV);
            if (tempHSV[1] <= 0 || tempHSV[0] > 45 && tempHSV[0] < 85) {
                tempHSV[0] = h;
            }
            tempHSV[1] = Math.max(0, Math.min(1, tempHSV[1] + (tempHSV[2] > .85f ? .25f : .45f)));
            tempHSV[2] = Math.max(0, Math.min(1, tempHSV[2] - .15f));
            return Color.HSVToColor(80, tempHSV);
        }
        private int textSelectionHandle(int bubble, int accentColor) {
            Color.colorToHSV(accentColor, tempHSV);
            float h = tempHSV[0];
            Color.colorToHSV(bubble, tempHSV);
            if (tempHSV[1] <= 0 || tempHSV[0] > 45 && tempHSV[0] < 85) {
                tempHSV[0] = h;
            }
            tempHSV[1] = Math.max(0, Math.min(1, tempHSV[1] + .6f));
            tempHSV[2] = Math.max(0, Math.min(1, tempHSV[2] - (tempHSV[2] > .7f ? .25f : .125f)));
            return Theme.blendOver(bubble, Color.HSVToColor(255, tempHSV));
        }
        private int linkSelectionBackground(int linkColor, int bgColor, boolean isDarkTheme) {
            Color.colorToHSV(ColorUtils.blendARGB(linkColor, bgColor, .25f), tempHSV);
            tempHSV[1] = Math.max(0, Math.min(1, tempHSV[1] - .1f));
            tempHSV[2] = Math.max(0, Math.min(1, tempHSV[2] + (isDarkTheme ? .1f : 0)));
            return Color.HSVToColor(0x33, tempHSV);
        }
        private int codeBackground(int bubbleColor, boolean isDarkTheme) {
            Color.colorToHSV(bubbleColor, tempHSV);
            int alpha = 0x20;
            if (isDarkTheme) {
                alpha = 0x40;
                tempHSV[1] = Utilities.clamp(tempHSV[1] - .08f, 1f, 0f);
                tempHSV[2] = .03f;
            } else {
                if (tempHSV[1] <= 0 || tempHSV[2] >= 1 || tempHSV[2] <= 0) {
                    tempHSV[2] = Math.max(0, Math.min(1, tempHSV[2] + -.2f));
                } else {
                    tempHSV[1] = Math.max(0, Math.min(1, tempHSV[1] + .28f));
                    tempHSV[2] = Math.max(0, Math.min(1, tempHSV[2] + -.1f));
                }
            }
            return Color.HSVToColor(alpha, tempHSV);
        }
        private int locationPlaceholderColor(float accentHue, int bubbleColor, boolean isDarkTheme) {
            if (isDarkTheme) {
                return 0x1effffff;
            } else {
                Color.colorToHSV(bubbleColor, tempHSV);
                if (tempHSV[1] <= 0 || tempHSV[2] >= 1 || tempHSV[2] <= 0) {
                    tempHSV[0] = accentHue;
                    tempHSV[1] = .2f;
                } else {
                    tempHSV[0] = MathUtils.clamp(tempHSV[0] + .22f, 0, 1);
                    tempHSV[1] = MathUtils.clamp(tempHSV[1] - .35f, 0, 1);
                }
                tempHSV[2] = MathUtils.clamp(tempHSV[2] - .65f, 0, 1);
                return Color.HSVToColor(0x5a, tempHSV);
            }
        }
        private int averageColor(SparseIntArray colors, int ...keys) {
            int r = 0, g = 0, b = 0, c = 0;
            for (int i = 0; i < keys.length; ++i) {
                if (colors.indexOfKey(keys[i]) < 0) {
                    continue;
                }
                try {
                    int color = colors.get(keys[i]);
                    r += Color.red(color);
                    g += Color.green(color);
                    b += Color.blue(color);
                    c++;
                } catch (Exception ignore) {}
            }
            if (c == 0) {
                return 0;
            }
            return Color.argb(255, r / c, g / c, b / c);
        }

    }

    public static int blendOver(int A, int B) { // B over A
        // over operator: https://en.wikipedia.org/wiki/Alpha_compositing#Description
        float aB = Color.alpha(B) / 255f,
              aA = Color.alpha(A) / 255f,
              aC = (aB + aA * (1 - aB));
        if (aC == 0f)
            return 0;
        return Color.argb(
            (int) (aC * 255),
            (int) ((Color.red(B) * aB + Color.red(A) * aA * (1 - aB)) / aC),
            (int) ((Color.green(B) * aB + Color.green(A) * aA * (1 - aB)) / aC),
            (int) ((Color.blue(B) * aB + Color.blue(A) * aA * (1 - aB)) / aC)
        );
    }

    public static int adaptHue(int color, int hueFromColor) {
        float hue, sat;
        float[] tempHSV = getTempHsv(5);
        Color.colorToHSV(hueFromColor, tempHSV);
        hue = tempHSV[0];
        sat = tempHSV[1];
        Color.colorToHSV(color, tempHSV);
        tempHSV[0] = hue;
        tempHSV[1] = AndroidUtilities.lerp(tempHSV[1], sat, .25f);
        return Color.HSVToColor(Color.alpha(color), tempHSV);
    }

    public static int adaptHSV(int color, float sat, float val) {
        float[] tempHSV = getTempHsv(5);
        Color.colorToHSV(color, tempHSV);
        if (tempHSV[1] > .1f && tempHSV[1] < .9f) { // otherwise, saturation would reveal some random hue there
            tempHSV[1] = MathUtils.clamp(tempHSV[1] + sat, 0, 1);
        }
        tempHSV[2] = MathUtils.clamp(tempHSV[2] + val, 0, 1);
        return Color.HSVToColor(Color.alpha(color), tempHSV);
    }

    public static int percentSV(int color1, int color2, float satT, float valT) {
        float[] tempHSV = getTempHsv(5);
        Color.colorToHSV(color2, tempHSV);
        float sat2 = tempHSV[1];
        float val2 = tempHSV[2];
        Color.colorToHSV(color1, tempHSV);
        tempHSV[1] = MathUtils.clamp(AndroidUtilities.lerp(tempHSV[1], sat2, satT), 0, 1);
        tempHSV[2] = MathUtils.clamp(AndroidUtilities.lerp(tempHSV[2], val2, valT), 0, 1);
        return Color.HSVToColor(
            AndroidUtilities.lerp(Color.alpha(color1), Color.alpha(color2), .85f),
            tempHSV
        );
    }

    public static int capAlpha(int color, float maxAlpha) {
        return ColorUtils.setAlphaComponent(color, MathUtils.clamp(Color.alpha(color), 0, (int) (0xFF * maxAlpha)));
    }

    public static int multAlpha(int color, float multiply) {
        if (multiply == 1f)
            return color;
        return ColorUtils.setAlphaComponent(color, MathUtils.clamp((int) (Color.alpha(color) * multiply), 0, 0xFF));
    }

    public static int reverseBlendOver(float ax, int y, int z) {
        float ay = Color.alpha(y) / 255f,
              az = Color.alpha(z) / 255f;
        return Color.argb(
            (int) (ax * 255),
            (int) ((Color.red(y) * ay * (1 - ax) - Color.red(z) * az) / ax),
            (int) ((Color.green(y) * ay * (1 - ax) - Color.green(z) * az) / ax),
            (int) ((Color.blue(y) * ay * (1 - ax) - Color.blue(z) * az) / ax)
        );
    }

    public static class ThemeInfo { // EVE-PATCH: decouple NotificationCenter from ThemeInfo
        public String name;
        public String pathToFile;
        public String pathToWallpaper;
        public String assetName;
        public String slug;
        public boolean badWallpaper;
        public boolean isBlured;
        public boolean isMotion;
        public int patternBgColor;
        public int patternBgGradientColor1;
        public int patternBgGradientColor2;
        public int patternBgGradientColor3;
        public int patternBgGradientRotation = 45;
        public int patternIntensity;

        public int account;

        public boolean loaded = true;

        public String uploadingThumb;
        public String uploadingFile;

        private int previewBackgroundColor;
        public int previewBackgroundGradientColor1;
        public int previewBackgroundGradientColor2;
        public int previewBackgroundGradientColor3;
        public int previewWallpaperOffset;
        private int previewInColor;
        private int previewOutColor;
        public boolean firstAccentIsDefault;
        public boolean previewParsed;
        public boolean themeLoaded = true;

        public int sortIndex;

        public int defaultAccentCount;

        public int accentBaseColor;

        public int currentAccentId;
        public int prevAccentId = -1;
        public SparseArray<ThemeAccent> themeAccentsMap;
        public ArrayList<ThemeAccent> themeAccents;
        public LongSparseArray<ThemeAccent> accentsByThemeId;
        public LongSparseArray<ThemeAccent> chatAccentsByThemeId = new LongSparseArray<>();
        public int lastChatThemeId = 0;
        public int lastAccentId = 100;

        private String loadingThemeWallpaperName;
        private String newPathToWallpaper;

        private int isDark = UNKNOWN;

        private final static int DARK= 1;
        private final static int LIGHT = 0;
        private final static int UNKNOWN = -1;

        ThemeInfo() {

        }

        public String getName() {
            return name != null ? name : ""; // EVE-PATCH: decouple info from getName
        }

        public void setCurrentAccentId(int id) {
            currentAccentId = id;
            ThemeAccent accent = getAccent(false);
            if (accent != null) {
            }
        }

        public void setPreviewInColor(int color) {
            previewInColor = color;
        }

        public void setPreviewOutColor(int color) {
            previewOutColor = color;
        }

        public void setPreviewBackgroundColor(int color) {
            previewBackgroundColor = color;
        }

        public int getPreviewInColor() {
            if (firstAccentIsDefault && currentAccentId == DEFALT_THEME_ACCENT_ID) {
                return 0xffffffff;
            }
            return previewInColor;
        }

        public int getPreviewOutColor() {
            if (firstAccentIsDefault && currentAccentId == DEFALT_THEME_ACCENT_ID) {
                return 0xfff0fee0;
            }
            return previewOutColor;
        }

        public int getPreviewBackgroundColor() {
            if (firstAccentIsDefault && currentAccentId == DEFALT_THEME_ACCENT_ID) {
                return 0xffcfd9e3;
            }
            return previewBackgroundColor;
        }


        private boolean isDefaultMyMessagesBubbles() {
            if (!firstAccentIsDefault) {
                return false;
            }
            if (currentAccentId == DEFALT_THEME_ACCENT_ID) {
                return true;
            }
            ThemeAccent defaultAccent = themeAccentsMap.get(DEFALT_THEME_ACCENT_ID);
            ThemeAccent accent = themeAccentsMap.get(currentAccentId);
            if (defaultAccent == null || accent == null) {
                return false;
            }
            return defaultAccent.myMessagesAccentColor == accent.myMessagesAccentColor &&
                    defaultAccent.myMessagesGradientAccentColor1 == accent.myMessagesGradientAccentColor1 &&
                    defaultAccent.myMessagesGradientAccentColor2 == accent.myMessagesGradientAccentColor2 &&
                    defaultAccent.myMessagesGradientAccentColor3 == accent.myMessagesGradientAccentColor3 &&
                    defaultAccent.myMessagesAnimated == accent.myMessagesAnimated;
        }

        private boolean isDefaultMyMessages() {
            if (!firstAccentIsDefault) {
                return false;
            }
            if (currentAccentId == DEFALT_THEME_ACCENT_ID) {
                return true;
            }
            ThemeAccent defaultAccent = themeAccentsMap.get(DEFALT_THEME_ACCENT_ID);
            ThemeAccent accent = themeAccentsMap.get(currentAccentId);
            if (defaultAccent == null || accent == null) {
                return false;
            }
            return defaultAccent.accentColor2 == accent.accentColor2 &&
                    defaultAccent.myMessagesAccentColor == accent.myMessagesAccentColor &&
                    defaultAccent.myMessagesGradientAccentColor1 == accent.myMessagesGradientAccentColor1 &&
                    defaultAccent.myMessagesGradientAccentColor2 == accent.myMessagesGradientAccentColor2 &&
                    defaultAccent.myMessagesGradientAccentColor3 == accent.myMessagesGradientAccentColor3 &&
                    defaultAccent.myMessagesAnimated == accent.myMessagesAnimated;
        }

        private boolean isDefaultMainAccent() {
            if (!firstAccentIsDefault) {
                return false;
            }
            if (currentAccentId == DEFALT_THEME_ACCENT_ID) {
                return true;
            }
            ThemeAccent defaultAccent = themeAccentsMap.get(DEFALT_THEME_ACCENT_ID);
            ThemeAccent accent = themeAccentsMap.get(currentAccentId);
            return accent != null && defaultAccent != null && defaultAccent.accentColor == accent.accentColor;
        }

        public boolean hasAccentColors() {
            return defaultAccentCount != 0;
        }

        public boolean isDark() {
            if (isDark != UNKNOWN) {
                return isDark == DARK;
            }
            if ("Dark Blue".equals(name) || "Night".equals(name)) {
                isDark = DARK;
            } else if ("Blue".equals(name) || "Arctic Blue".equals(name) || "Day".equals(name)) {
                isDark = LIGHT;
            }
            if (isDark == UNKNOWN) {
                String[] wallpaperLink = new String[1];
                SparseIntArray colors = getThemeFileValues(new File(pathToFile), null, wallpaperLink);
                checkIsDark(colors, this);
            }
            return isDark == DARK;
        }

        public boolean isLight() {
            return pathToFile == null && !isDark();
        }

        public String getKey() {
            return name;
        }

        static ThemeInfo createWithString(String string) {
            if (TextUtils.isEmpty(string)) {
                return null;
            }
            String[] args = string.split("\\|");
            if (args.length != 2) {
                return null;
            }
            ThemeInfo themeInfo = new ThemeInfo();
            themeInfo.name = args[0];
            themeInfo.pathToFile = args[1];
            return themeInfo;
        }

        private void setAccentColorOptions(int[] options) {
            setAccentColorOptions(options, null, null, null, null, null, null, null, null, null, null);
        }

        private void setAccentColorOptions(int[] accent, int[] myMessages, int[] myMessagesGradient, int[] background, int[] backgroundGradient1, int[] backgroundGradient2, int[] backgroundGradient3, int[] ids, String[] patternSlugs, int[] patternRotations, int[] patternIntensities) {
            defaultAccentCount = accent.length;
            themeAccents = new ArrayList<>();
            themeAccentsMap = new SparseArray<>();
            accentsByThemeId = new LongSparseArray<>();

            for (int a = 0; a < accent.length; a++) {
                ThemeAccent themeAccent = new ThemeAccent();
                themeAccent.id = ids != null ? ids[a] : a;
                if (isHome(themeAccent)) {
                    themeAccent.isDefault = true;
                }
                themeAccent.accentColor = accent[a];
                themeAccent.parentTheme = this;
                if (myMessages != null) {
                    themeAccent.myMessagesAccentColor = myMessages[a];
                }
                if (myMessagesGradient != null) {
                    themeAccent.myMessagesGradientAccentColor1 = myMessagesGradient[a];
                }
                if (background != null) {
                    themeAccent.backgroundOverrideColor = background[a];
                    if (firstAccentIsDefault && themeAccent.id == DEFALT_THEME_ACCENT_ID) {
                        themeAccent.backgroundOverrideColor = 0x100000000L;
                    } else {
                        themeAccent.backgroundOverrideColor = background[a];
                    }
                }
                if (backgroundGradient1 != null) {
                    if (firstAccentIsDefault && themeAccent.id == DEFALT_THEME_ACCENT_ID) {
                        themeAccent.backgroundGradientOverrideColor1 = 0x100000000L;
                    } else {
                        themeAccent.backgroundGradientOverrideColor1 = backgroundGradient1[a];
                    }
                }
                if (backgroundGradient2 != null) {
                    if (firstAccentIsDefault && themeAccent.id == DEFALT_THEME_ACCENT_ID) {
                        themeAccent.backgroundGradientOverrideColor2 = 0x100000000L;
                    } else {
                        themeAccent.backgroundGradientOverrideColor2 = backgroundGradient2[a];
                    }
                }
                if (backgroundGradient3 != null) {
                    if (firstAccentIsDefault && themeAccent.id == DEFALT_THEME_ACCENT_ID) {
                        themeAccent.backgroundGradientOverrideColor3 = 0x100000000L;
                    } else {
                        themeAccent.backgroundGradientOverrideColor3 = backgroundGradient3[a];
                    }
                }
                if (patternSlugs != null) {
                    themeAccent.patternIntensity = patternIntensities[a] / 100.0f;
                    themeAccent.backgroundRotation = patternRotations[a];
                    themeAccent.patternSlug = patternSlugs[a];
                }

                //override default themes
                if (isHome(themeAccent) && name.equals("Dark Blue") || name.equals("Night")) {
                    themeAccent.myMessagesAccentColor = 0xff258DE5;
                    themeAccent.myMessagesGradientAccentColor1 = 0xff4272DF;
                    themeAccent.myMessagesGradientAccentColor2 = 0xff8146D7;
                    themeAccent.myMessagesGradientAccentColor3 = 0xff9F3EAA;
                    if (name.equals("Night")) {
                        themeAccent.patternIntensity = -0.57f;
                        themeAccent.backgroundOverrideColor = 0xff6c7fa6;
                        themeAccent.backgroundGradientOverrideColor1 = 0xff2e344b;
                        themeAccent.backgroundGradientOverrideColor2 = 0xff7874a7;
                        themeAccent.backgroundGradientOverrideColor3 = 0xff333258;
                    }
                }
                themeAccentsMap.put(themeAccent.id, themeAccent);
                themeAccents.add(themeAccent);
            }
            accentBaseColor = themeAccentsMap.get(0).accentColor;
        }


        public ThemeAccent getAccent(boolean createNew) {
            if (themeAccents == null) {
                return null;
            }
            ThemeAccent accent = themeAccentsMap.get(currentAccentId);
            if (accent == null) {
                return null;
            }
            if (createNew) {
                int id = ++lastAccentId;
                ThemeAccent themeAccent = new ThemeAccent();
                themeAccent.accentColor = accent.accentColor;
                themeAccent.accentColor2 = accent.accentColor2;
                themeAccent.myMessagesAccentColor = accent.myMessagesAccentColor;
                themeAccent.myMessagesGradientAccentColor1 = accent.myMessagesGradientAccentColor1;
                themeAccent.myMessagesGradientAccentColor2 = accent.myMessagesGradientAccentColor2;
                themeAccent.myMessagesGradientAccentColor3 = accent.myMessagesGradientAccentColor3;
                themeAccent.myMessagesAnimated = accent.myMessagesAnimated;
                themeAccent.backgroundOverrideColor = accent.backgroundOverrideColor;
                themeAccent.backgroundGradientOverrideColor1 = accent.backgroundGradientOverrideColor1;
                themeAccent.backgroundGradientOverrideColor2 = accent.backgroundGradientOverrideColor2;
                themeAccent.backgroundGradientOverrideColor3 = accent.backgroundGradientOverrideColor3;
                themeAccent.backgroundRotation = accent.backgroundRotation;
                themeAccent.patternSlug = accent.patternSlug;
                themeAccent.patternIntensity = accent.patternIntensity;
                themeAccent.patternMotion = accent.patternMotion;
                themeAccent.parentTheme = this;
                prevAccentId = currentAccentId;
                currentAccentId = themeAccent.id = id;
                themeAccentsMap.put(id, themeAccent);
                themeAccents.add(0, themeAccent);
                sortAccents(this);
                return themeAccent;
            } else {
                return accent;
            }
        }

        public int getAccentColor(int id) {
            ThemeAccent accent = themeAccentsMap.get(id);
            return accent != null ? accent.accentColor : 0;
        }

    }

    public interface ResourcesProvider {

        int getColor(int key);

        default int getColorOrDefault(int key) {
            return getColor(key);
        }

        default int getCurrentColor(int key) {
            return getColor(key);
        }

        default void setAnimatedColor(int key, int color) {}

        default Drawable getDrawable(String drawableKey) {
            return null;
        }

        default Paint getPaint(String paintKey) {
            return Theme.getThemePaint(paintKey);
        }

        default boolean hasGradientService() {
            return false;
        }

        default boolean isDark() {
            // used only in PeerColorActivity
            // support in other implementations to use
            return Theme.isCurrentThemeDark();
        }

        default void applyServiceShaderMatrix(int w, int h, float translationX, float translationY) {
            Theme.applyServiceShaderMatrix(w, h, translationX, translationY);
        }

        default ColorFilter getAnimatedEmojiColorFilter() {
            return Theme.chat_animatedEmojiTextColorFilter;
        }

        /** A chat's provider answers for its dialog, see SharedConfig#isCompactMode(int, long). */
        default boolean isCompactMode() {
            return false; // EVE-PATCH: decouple SharedConfig from isCompactMode
        }
    }

    public static boolean isCompactMode(ResourcesProvider resourcesProvider) {
        return resourcesProvider != null && resourcesProvider.isCompactMode(); // EVE-PATCH: decouple SharedConfig
    }

    private static final Object sync = new Object();
    public static Runnable wallpaperLoadTask;

    public static final int ACTION_BAR_PHOTO_VIEWER_COLOR = 0x7f000000;
    public static final int ACTION_BAR_MEDIA_PICKER_COLOR = 0xff333333;
    public static final int ACTION_BAR_VIDEO_EDIT_COLOR = 0xff000000;
    public static final int ACTION_BAR_PLAYER_COLOR = 0xffffffff;
    public static final int ACTION_BAR_PICKER_SELECTOR_COLOR = 0xff3d3d3d;
    public static final int ACTION_BAR_WHITE_SELECTOR_COLOR = 0x40ffffff;
    public static final int ACTION_BAR_AUDIO_SELECTOR_COLOR = 0x2f000000;
    public static final int ARTICLE_VIEWER_MEDIA_PROGRESS_COLOR = 0xffffffff;

    public static final int AUTO_NIGHT_TYPE_NONE = 0;
    public static final int AUTO_NIGHT_TYPE_SCHEDULED = 1;
    public static final int AUTO_NIGHT_TYPE_AUTOMATIC = 2;
    public static final int AUTO_NIGHT_TYPE_SYSTEM = 3;

    private static final int LIGHT_SENSOR_THEME_SWITCH_DELAY = 1800;
    private static final int LIGHT_SENSOR_THEME_SWITCH_NEAR_DELAY = 12000;
    private static final int LIGHT_SENSOR_THEME_SWITCH_NEAR_THRESHOLD = 12000;
    private static SensorManager sensorManager;
    private static Sensor lightSensor;
    private static boolean lightSensorRegistered;
    private static float lastBrightnessValue = 1.0f;
    private static long lastThemeSwitchTime;
    private static boolean switchDayRunnableScheduled;
    private static boolean switchNightRunnableScheduled;

    public static int DEFALT_THEME_ACCENT_ID = 99;
    public static int selectedAutoNightType = AUTO_NIGHT_TYPE_NONE;
    public static boolean autoNightScheduleByLocation;
    public static float autoNightBrighnessThreshold = 0.25f;
    public static int autoNightDayStartTime = 22 * 60;
    public static int autoNightDayEndTime = 8 * 60;
    public static int autoNightSunsetTime = 22 * 60;
    public static int autoNightLastSunCheckDay = -1;
    public static int autoNightSunriseTime = 8 * 60;
    public static String autoNightCityName = "";
    public static double autoNightLocationLatitude = 10000;
    public static double autoNightLocationLongitude = 10000;

    private static Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static int loadingCurrentTheme;
    private static int lastLoadingCurrentThemeTime;

    public static ArrayList<ThemeInfo> themes;
    private static ArrayList<ThemeInfo> otherThemes;
    private static HashMap<String, ThemeInfo> themesDict;
    private static ThemeInfo currentTheme;
    private static ThemeInfo currentNightTheme;
    private static ThemeInfo currentDayTheme;
    private static ThemeInfo defaultTheme;
    private static ThemeInfo previousTheme;
    private static boolean changingWallpaper;
    private static boolean hasPreviousTheme;
    private static boolean isApplyingAccent;
    private static boolean switchingNightTheme;
    private static boolean isInNigthMode;
    private static int previousPhase;

    private static int switchNightThemeDelay;
    private static long lastDelayUpdateTime;

    private static boolean isCustomTheme;
    private static int serviceMessageColor;
    private static Bitmap serviceBitmap;
    public static BitmapShader serviceBitmapShader;
    private static Matrix serviceBitmapMatrix;
    private static int serviceSelectedMessageColor;
    public static int serviceMessageColorBackup;
    public static int serviceSelectedMessageColorBackup;
    public static int currentColor;
    private static Drawable wallpaper;
    private static Drawable themedWallpaper;
    private static int themedWallpaperFileOffset;
    private static String themedWallpaperLink;
    private static boolean isWallpaperMotion;
    private static int patternIntensity;
    private static boolean isPatternWallpaper;

    public static Paint dividerPaint = new Paint(); static { dividerPaint.setStrokeWidth(1); } // EVE-PATCH: initialize dividerPaint
    public static Paint dividerExtraPaint;
    public static Paint linkSelectionPaint;
    public static Paint checkboxSquare_eraserPaint;
    public static Paint checkboxSquare_checkPaint;
    public static Paint checkboxSquare_backgroundPaint;
    public static Paint avatar_backgroundPaint;

    public static Drawable listSelector;
    public static Drawable[] avatarDrawables = new Drawable[25];

    public static Drawable moveUpDrawable;

    public static Paint dialogs_onlineCirclePaint;
    public static Paint dialogs_tabletSeletedPaint;
    public static Paint dialogs_pinnedPaint;
    public static Paint dialogs_countPaint;
    public static Paint dialogs_errorPaint;
    public static Paint dialogs_countGrayPaint;
    public static Paint dialogs_actionMessagePaint;
    public static Paint dialogs_tagPaint;
    public static TextPaint[] dialogs_namePaint;
    public static TextPaint[] dialogs_nameEncryptedPaint;
    public static TextPaint dialogs_searchNamePaint;
    public static TextPaint dialogs_searchNameEncryptedPaint;
    public static TextPaint[] dialogs_messagePaint;
    public static TextPaint dialogs_messageNamePaint;
    public static TextPaint[] dialogs_messagePrintingPaint;
    public static TextPaint dialogs_timePaint;
    public static TextPaint dialogs_timePaintBold;
    public static TextPaint dialogs_timePaintBoldAccent;
    public static TextPaint dialogs_countTextPaint;
    public static TextPaint dialogs_countTextPaint2;
    public static TextPaint dialogs_archiveTextPaint;
    public static TextPaint dialogs_archiveTextPaintSmall;
    public static TextPaint dialogs_onlinePaint;
    public static TextPaint dialogs_offlinePaint;
    public static TextPaint dialogs_tagTextPaint;

    public static Drawable dialogs_communityCardsDrawable;
    public static Drawable dialogs_checkDrawable;
    public static Drawable dialogs_playDrawable;
    public static Drawable dialogs_checkReadDrawable;
    public static Drawable dialogs_halfCheckDrawable;
    public static Drawable dialogs_clockDrawable;
    public static Drawable dialogs_errorDrawable;
    public static Drawable dialogs_reorderDrawable;
    public static Drawable dialogs_lockDrawable;
    public static Drawable dialogs_lock2Drawable;
    public static Drawable dialogs_muteDrawable;
    public static Drawable dialogs_unmuteDrawable;
    public static Drawable dialogs_hiddenDrawable;
    public static Drawable dialogs_verifiedDrawable;
    public static Drawable dialogs_verifiedCheckDrawable;
    public static Drawable dialogs_pinnedDrawable;
    public static Drawable dialogs_pinnedDrawable2;
    public static Drawable dialogs_pinnedDrawable2Accent;
    public static Drawable dialogs_mentionDrawable;
    public static Drawable dialogs_reactionsMentionDrawable;
    public static Drawable dialogs_pollMentionDrawable;
    public static Drawable dialogs_mentionDrawableMuted;
    public static Drawable dialogs_reactionsMentionDrawableMuted;
    public static Drawable dialogs_pollMentionDrawableMuted;
    public static Drawable dialogs_holidayDrawable;
    public static Drawable dialogs_forum_arrowDrawable;
    public static boolean dialogs_archiveDrawableRecolored;
    public static boolean dialogs_hidePsaDrawableRecolored;
    public static boolean dialogs_archiveAvatarDrawableRecolored;
    private static int dialogs_holidayDrawableOffsetX;
    private static int dialogs_holidayDrawableOffsetY;
    private static long lastHolidayCheckTime;
    private static boolean canStartHolidayAnimation;

    public static TextPaint profile_aboutTextPaint;
    public static Drawable profile_verifiedDrawable;
    public static Drawable profile_verifiedCheckDrawable;

    public static Paint chat_docBackPaint;
    public static Paint chat_deleteProgressPaint;
    public static Paint chat_urlPaint;
    public static Paint chat_outUrlPaint;
    public static Paint chat_textSearchSelectionPaint;
    public static Paint chat_instantViewRectPaint;
    public static Paint chat_instantViewButtonPaint;
    public static Paint chat_pollTimerPaint;
    public static Paint chat_replyLinePaint;
    public static Paint chat_msgErrorPaint;
    public static Paint chat_statusPaint;
    public static Paint chat_statusRecordPaint;
    public static Paint chat_messageBackgroundSelectedPaint;
    public static Paint chat_actionBackgroundPaint;
    public static Paint chat_actionBackgroundSelectedPaint;
    public static Paint chat_actionBackgroundGradientDarkenPaint;
    public static Paint chat_timeBackgroundPaint;
    public static Paint chat_composeBackgroundPaint;
    public static Paint chat_radialProgressPaint;
    public static Paint chat_radialProgress2Paint;
    public static Paint chat_radialProgressPausedPaint;
    public static Paint chat_radialProgressPausedSeekbarPaint;
    public static Paint chat_videoProgressPaint;

    public static TextPaint chat_msgTextPaint;
    public static TextPaint chat_msgTextCodePaint;
    public static TextPaint chat_msgTextCode2Paint;
    public static TextPaint chat_msgTextCode3Paint;
    public static TextPaint chat_actionTextPaint;
    public static TextPaint chat_actionTextPaint2;
    public static TextPaint chat_actionTextPaint3;
    public static TextPaint chat_unlockExtendedMediaTextPaint;
    public static TextPaint chat_msgBotButtonPaint;
    public static TextPaint chat_msgGameTextPaint;
    public static TextPaint[] chat_msgTextPaintEmoji;
    public static TextPaint chat_msgTextPaintOneEmoji;
    public static TextPaint chat_msgTextPaintTwoEmoji;
    public static TextPaint chat_msgTextPaintThreeEmoji;
    public static TextPaint chat_infoPaint;
    public static TextPaint chat_infoBoldPaint;
    public static TextPaint chat_stickerCommentCountPaint;
    public static TextPaint chat_livePaint;
    public static TextPaint chat_docNamePaint;
    public static TextPaint chat_locationTitlePaint;
    public static TextPaint chat_locationAddressPaint;
    public static TextPaint chat_durationPaint;
    public static TextPaint chat_gamePaint;
    public static TextPaint chat_shipmentPaint;
    public static TextPaint chat_instantViewPaint;
    public static TextPaint chat_audioTimePaint;
    public static TextPaint chat_audioTitlePaint;
    public static TextPaint chat_audioPerformerPaint;
    public static TextPaint chat_botButtonPaint;
    public static TextPaint chat_contactNamePaint;
    public static TextPaint chat_contactPhonePaint;
    public static TextPaint chat_timePaint;
    public static TextPaint chat_adminPaint;
    public static TextPaint chat_ephemeralPaint;
    public static TextPaint chat_namePaint;
    public static TextPaint chat_forwardNamePaint;
    public static TextPaint chat_replyNamePaint;
    public static TextPaint chat_replyTextPaint;
    public static TextPaint chat_quoteTextPaint;
    public static TextPaint chat_explanationTextPaint;
    public static TextPaint chat_titleLabelTextPaint;
    public static TextPaint chat_topicTextPaint;
    public static TextPaint chat_commentTextPaint;
    public static TextPaint chat_contextResult_titleTextPaint;
    public static TextPaint chat_contextResult_descriptionTextPaint;
    public static TextPaint chat_msgCodeBgPaint;

    public static Drawable chat_msgNoSoundDrawable;
    public static Drawable chat_composeShadowDrawable;
    public static Drawable chat_composeShadowRoundDrawable;
    public static Drawable chat_roundVideoShadow;
    public static Drawable chat_livePhoto;
    public static PorterDuffColorFilter chat_animatedEmojiTextColorFilter;
    public static PorterDuffColorFilter chat_outAnimatedEmojiTextColorFilter;

    public static Drawable chat_msgOutCheckDrawable;
    public static Drawable chat_msgOutCheckSelectedDrawable;
    public static Drawable chat_msgOutCheckReadDrawable;
    public static Drawable chat_msgOutCheckReadSelectedDrawable;
    public static Drawable chat_msgOutHalfCheckDrawable;
    public static Drawable chat_msgOutHalfCheckSelectedDrawable;
    public static Drawable chat_msgMediaCheckDrawable;
    public static Drawable chat_msgMediaHalfCheckDrawable;
    public static Drawable chat_msgStickerCheckDrawable;
    public static Drawable chat_msgStickerHalfCheckDrawable;
    public static Drawable chat_msgStickerViewsDrawable;
    public static Drawable chat_msgStickerRepliesDrawable;
    public static Drawable chat_msgUnlockDrawable;
    public static Drawable chat_msgInViewsDrawable;
    public static Drawable chat_msgInViewsSelectedDrawable;
    public static Drawable chat_msgOutViewsDrawable;
    public static Drawable chat_msgOutViewsSelectedDrawable;
    public static Drawable chat_msgInRepliesDrawable;
    public static Drawable chat_msgInRepliesSelectedDrawable;
    public static Drawable chat_msgOutRepliesDrawable;
    public static Drawable chat_msgOutRepliesSelectedDrawable;
    public static Drawable chat_msgInPinnedDrawable;
    public static Drawable chat_msgInPinnedSelectedDrawable;
    public static Drawable chat_msgOutPinnedDrawable;
    public static Drawable chat_msgOutPinnedSelectedDrawable;
    public static Drawable chat_msgStickerPinnedDrawable;
    public static Drawable chat_msgMediaPinnedDrawable;
    public static Drawable chat_msgMediaViewsDrawable;
    public static Drawable chat_msgMediaRepliesDrawable;
    public static Drawable chat_msgInMenuDrawable;
    public static Drawable chat_msgInMenuSelectedDrawable;
    public static Drawable chat_msgOutMenuDrawable;
    public static Drawable chat_msgOutMenuSelectedDrawable;
    public static Drawable chat_msgMediaMenuDrawable;
    public static Drawable chat_msgInInstantDrawable;
    public static Drawable chat_msgOutInstantDrawable;
    public static Drawable chat_msgErrorDrawable;
    public static Drawable chat_muteIconDrawable;
    public static Drawable chat_lockIconDrawable;
    public static Drawable chat_inlineResultFile;
    public static Drawable chat_inlineResultAudio;
    public static Drawable chat_inlineResultLocation;
    public static Drawable chat_msgOutLocationDrawable;
    public static Drawable chat_contextResult_shadowUnderSwitchDrawable;
    public static Drawable chat_shareIconDrawable;
    public static Drawable chat_replyIconDrawable;
    public static Drawable chat_closeIconDrawable;
    public static Drawable chat_moreIconDrawable;
    public static Drawable chat_goIconDrawable;
    public static Drawable chat_botLinkDrawable;
    public static Drawable chat_botCardDrawable;
    public static Drawable chat_botInlineDrawable;
    public static Drawable chat_botWebViewDrawable;
    public static Drawable chat_botInviteDrawable;
    public static Drawable chat_botLockDrawable;
    public static Drawable chat_commentDrawable;
    public static Drawable chat_commentStickerDrawable;
    public static Drawable chat_commentArrowDrawable;
    public static Drawable chat_gradientLeftDrawable;
    public static Drawable chat_gradientRightDrawable;
    public static Drawable[] chat_msgInCallDrawable = new Drawable[2];
    public static Drawable[] chat_msgInCallSelectedDrawable = new Drawable[2];
    public static Drawable[] chat_msgOutCallDrawable = new Drawable[2];
    public static Drawable[] chat_msgOutCallSelectedDrawable = new Drawable[2];
    public static Drawable[] chat_pollCheckDrawable = new Drawable[2];
    public static Drawable[] chat_pollCrossDrawable = new Drawable[2];
    public static Drawable[] chat_pollHintDrawable = new Drawable[2];
    public static Drawable[] chat_psaHelpDrawable = new Drawable[2];

    public static Drawable chat_msgCallUpGreenDrawable;
    public static Drawable chat_msgCallDownRedDrawable;
    public static Drawable chat_msgCallDownGreenDrawable;

    public static Drawable chat_attachEmptyDrawable;
    public static Drawable[] chat_locationDrawable = new Drawable[2];
    public static Drawable[] chat_contactDrawable = new Drawable[2];
    public static Drawable[][] chat_fileStatesDrawable = new Drawable[5][2];

    public static Drawable calllog_msgCallUpRedDrawable;
    public static Drawable calllog_msgCallUpGreenDrawable;
    public static Drawable calllog_msgCallDownRedDrawable;
    public static Drawable calllog_msgCallDownGreenDrawable;

    public static Path[] chat_filePath = new Path[2];
    public static Path[] chat_updatePath = new Path[3];
    public static Drawable chat_flameIcon;
    public static Drawable chat_gifIcon;

    public static int colorsCount;
    public static final int key_wallpaperFileOffset = colorsCount++;
    public static final int key_dialogBackground = colorsCount++;
    public static final int key_dialogBackgroundGray = colorsCount++;
    public static final int key_dialogTextBlack = colorsCount++;
    public static final int key_dialogTextLink = colorsCount++;
    public static final int key_dialogLinkSelection = colorsCount++;
    public static final int key_dialogTextBlue = colorsCount++;
    public static final int key_dialogTextBlue2 = colorsCount++;
    public static final int key_dialogTextBlue4 = colorsCount++;
    public static final int key_dialogTextGray = colorsCount++;
    public static final int key_dialogTextGray2 = colorsCount++;
    public static final int key_dialogTextGray3 = colorsCount++;
    public static final int key_dialogTextGray4 = colorsCount++;
    public static final int key_dialogTextHint = colorsCount++;
    public static final int key_dialogInputField = colorsCount++;
    public static final int key_dialogInputFieldActivated = colorsCount++;
    public static final int key_dialogCheckboxSquareBackground = colorsCount++;
    public static final int key_dialogCheckboxSquareCheck = colorsCount++;
    public static final int key_dialogCheckboxSquareUnchecked = colorsCount++;
    public static final int key_dialogCheckboxSquareDisabled = colorsCount++;
    public static final int key_dialogScrollGlow = colorsCount++;
    public static final int key_dialogRoundCheckBox = colorsCount++;
    public static final int key_dialogRoundCheckBoxCheck = colorsCount++;
    public static final int key_dialogRadioBackground = colorsCount++;
    public static final int key_dialogRadioBackgroundChecked = colorsCount++;
    public static final int key_dialogLineProgress = colorsCount++;
    public static final int key_dialogLineProgressBackground = colorsCount++;
    public static final int key_dialogButton = colorsCount++;
    public static final int key_dialogButtonSelector = colorsCount++;
    public static final int key_dialogIcon = colorsCount++;
    public static final int key_dialogGrayLine = colorsCount++;
    public static final int key_dialogTopBackground = colorsCount++;
    public static final int key_dialog_inlineProgressBackground = colorsCount++;
    public static final int key_dialog_inlineProgress = colorsCount++;
    public static final int key_dialogSearchBackground = colorsCount++;
    public static final int key_dialogSearchHint = colorsCount++;
    public static final int key_dialogSearchIcon = colorsCount++;
    public static final int key_dialogSearchText = colorsCount++;
    public static final int key_dialogFloatingButton = colorsCount++;
    public static final int key_dialogFloatingButtonPressed = colorsCount++;
    public static final int key_dialogFloatingIcon = colorsCount++;
    public static final int key_dialogShadowLine = colorsCount++;
    public static final int key_dialogEmptyImage = colorsCount++;
    public static final int key_dialogEmptyText = colorsCount++;
    public static final int key_dialogSwipeRemove = colorsCount++;
    public static final int key_dialogReactionMentionBackground = colorsCount++;
    public static final int key_dialogCardShadow = colorsCount++;
    public static final int key_dialogGiftsBackground = colorsCount++;
    public static final int key_dialogGiftsTabText = colorsCount++;

    public static final int key_windowBackgroundWhite = colorsCount++;
    public static final int key_windowBackgroundUnchecked = colorsCount++;
    public static final int key_windowBackgroundChecked = colorsCount++;
    public static final int key_windowBackgroundCheckText = colorsCount++;
    public static final int key_progressCircle = colorsCount++;
    public static final int key_listSelector = colorsCount++;
    public static final int key_settings_listSelector = colorsCount++;
    public static final int key_windowBackgroundWhiteInputField = colorsCount++;
    public static final int key_windowBackgroundWhiteInputFieldActivated = colorsCount++;
    public static final int key_windowBackgroundWhiteGrayIcon = colorsCount++;
    public static final int key_windowBackgroundWhiteBlueText = colorsCount++;
    public static final int key_windowBackgroundWhiteBlueText2 = colorsCount++;
    public static final int key_windowBackgroundWhiteBlueText3 = colorsCount++;
    public static final int key_windowBackgroundWhiteBlueText4 = colorsCount++;
    public static final int key_windowBackgroundWhiteBlueText5 = colorsCount++;
    public static final int key_windowBackgroundWhiteBlueText6 = colorsCount++;
    public static final int key_windowBackgroundWhiteBlueText7 = colorsCount++;
    public static final int key_windowBackgroundWhiteBlueButton = colorsCount++;
    public static final int key_windowBackgroundWhiteBlueIcon = colorsCount++;
    public static final int key_windowBackgroundWhiteGreenText = colorsCount++;
    public static final int key_windowBackgroundWhiteGreenText2 = colorsCount++;
    public static final int key_windowBackgroundWhiteGrayText = colorsCount++;
    public static final int key_windowBackgroundWhiteGrayText2 = colorsCount++;
    public static final int key_windowBackgroundWhiteGrayText3 = colorsCount++;
    public static final int key_windowBackgroundWhiteGrayText4 = colorsCount++;
    public static final int key_windowBackgroundWhiteGrayText5 = colorsCount++;
    public static final int key_windowBackgroundWhiteGrayText6 = colorsCount++;
    public static final int key_windowBackgroundWhiteGrayText7 = colorsCount++;
    public static final int key_windowBackgroundWhiteGrayText8 = colorsCount++;
    public static final int key_windowBackgroundWhiteBlackText = colorsCount++;
    public static final int key_windowBackgroundWhiteHintText = colorsCount++;
    public static final int key_windowBackgroundWhiteValueText = colorsCount++;
    public static final int key_windowBackgroundWhiteLinkText = colorsCount++;
    public static final int key_windowBackgroundWhiteLinkSelection = colorsCount++;
    public static final int key_windowBackgroundWhiteBlueHeader = colorsCount++;
    public static final int key_switchTrack = colorsCount++;
    public static final int key_switchTrackChecked = colorsCount++;
    public static final int key_switchTrackBlue = colorsCount++;
    public static final int key_switchTrackBlueChecked = colorsCount++;
    public static final int key_switchTrackBlueThumb = colorsCount++;
    public static final int key_switchTrackBlueThumbChecked = colorsCount++;
    public static final int key_switchTrackBlueSelector = colorsCount++;
    public static final int key_switchTrackBlueSelectorChecked = colorsCount++;
    public static final int key_switch2Track = colorsCount++;
    public static final int key_switch2TrackChecked = colorsCount++;
    public static final int key_checkboxSquareBackground = colorsCount++;
    public static final int key_checkboxSquareCheck = colorsCount++;
    public static final int key_checkboxSquareUnchecked = colorsCount++;
    public static final int key_checkboxSquareDisabled = colorsCount++;
    public static final int key_windowBackgroundGray = colorsCount++;
    public static final int key_windowBackgroundGrayShadow = colorsCount++;
    public static final int key_emptyListPlaceholder = colorsCount++;
    public static final int key_divider = colorsCount++;
    public static final int key_graySection = colorsCount++;
    public static final int key_graySectionText = colorsCount++;
    public static final int key_radioBackground = colorsCount++;
    public static final int key_radioBackgroundChecked = colorsCount++;
    public static final int key_checkbox = colorsCount++;
    public static final int key_checkboxDisabled = colorsCount++;
    public static final int key_checkboxCheck = colorsCount++;
    public static final int key_fastScrollActive = colorsCount++;
    public static final int key_fastScrollInactive = colorsCount++;
    public static final int key_fastScrollText = colorsCount++;
    public static final int key_pollCreateIcons = colorsCount++;

    public static final int key_text_RedRegular = colorsCount++;
    public static final int key_text_RedBold = colorsCount++;
    public static final int key_fill_RedNormal = colorsCount++;
    public static final int key_fill_RedDark = colorsCount++;

    public static final int key_inappPlayerPerformer = colorsCount++;
    public static final int key_inappPlayerTitle = colorsCount++;
    public static final int key_inappPlayerBackground = colorsCount++;
    public static final int key_inappPlayerPlayPause = colorsCount++;
    public static final int key_inappPlayerClose = colorsCount++;

    public static final int key_returnToCallBackground = colorsCount++;
    public static final int key_returnToCallMutedBackground = colorsCount++;
    public static final int key_returnToCallText = colorsCount++;

    public static final int key_contextProgressInner1 = colorsCount++;
    public static final int key_contextProgressOuter1 = colorsCount++;
    public static final int key_contextProgressInner2 = colorsCount++;
    public static final int key_contextProgressOuter2 = colorsCount++;
    public static final int key_contextProgressInner3 = colorsCount++;
    public static final int key_contextProgressOuter3 = colorsCount++;
    public static final int key_contextProgressInner4 = colorsCount++;
    public static final int key_contextProgressOuter4 = colorsCount++;

    public static final int key_avatar_text = colorsCount++;
    public static final int key_avatar_backgroundSaved = colorsCount++;
    public static final int key_avatar_background2Saved = colorsCount++;
    public static final int key_avatar_backgroundArchived = colorsCount++;
    public static final int key_avatar_backgroundArchivedHidden = colorsCount++;
    public static final int key_avatar_backgroundRed = colorsCount++;
    public static final int key_avatar_backgroundOrange = colorsCount++;
    public static final int key_avatar_backgroundViolet = colorsCount++;
    public static final int key_avatar_backgroundGreen = colorsCount++;
    public static final int key_avatar_backgroundCyan = colorsCount++;
    public static final int key_avatar_backgroundBlue = colorsCount++;
    public static final int key_avatar_backgroundPink = colorsCount++;
    public static final int key_avatar_background2Red = colorsCount++;
    public static final int key_avatar_background2Orange = colorsCount++;
    public static final int key_avatar_background2Violet = colorsCount++;
    public static final int key_avatar_background2Green = colorsCount++;
    public static final int key_avatar_background2Cyan = colorsCount++;
    public static final int key_avatar_background2Blue = colorsCount++;
    public static final int key_avatar_background2Pink = colorsCount++;
    public static final int key_avatar_backgroundGray = colorsCount++;

    public static final int key_avatar_backgroundInProfileBlue = colorsCount++;
    public static final int key_avatar_backgroundActionBarBlue = colorsCount++;
    public static final int key_avatar_actionBarSelectorBlue = colorsCount++;
    public static final int key_avatar_actionBarIconBlue = colorsCount++;
    public static final int key_avatar_subtitleInProfileBlue = colorsCount++;

    public static final int key_avatar_nameInMessageRed = colorsCount++;
    public static final int key_avatar_nameInMessageOrange = colorsCount++;
    public static final int key_avatar_nameInMessageViolet = colorsCount++;
    public static final int key_avatar_nameInMessageGreen = colorsCount++;
    public static final int key_avatar_nameInMessageCyan = colorsCount++;
    public static final int key_avatar_nameInMessageBlue = colorsCount++;
    public static final int key_avatar_nameInMessagePink = colorsCount++;

    public static int[] keys_avatar_background = {key_avatar_backgroundRed, key_avatar_backgroundOrange, key_avatar_backgroundViolet, key_avatar_backgroundGreen, key_avatar_backgroundCyan, key_avatar_backgroundBlue, key_avatar_backgroundPink};
    public static int[] keys_avatar_background2 = {key_avatar_background2Red, key_avatar_background2Orange, key_avatar_background2Violet, key_avatar_background2Green, key_avatar_background2Cyan, key_avatar_background2Blue, key_avatar_background2Pink};
    public static int[] keys_avatar_nameInMessage = {key_avatar_nameInMessageRed, key_avatar_nameInMessageOrange, key_avatar_nameInMessageViolet, key_avatar_nameInMessageGreen, key_avatar_nameInMessageCyan, key_avatar_nameInMessageBlue, key_avatar_nameInMessagePink};

    public static final int key_actionBarDefault = colorsCount++;
    public static final int key_actionBarDefaultSelector = colorsCount++;
    public static final int key_actionBarWhiteSelector = colorsCount++;
    public static final int key_actionBarDefaultIcon = colorsCount++;
    public static final int key_actionBarActionModeDefault = colorsCount++;
    public static final int key_actionBarActionModeDefaultTop = colorsCount++;
    public static final int key_actionBarActionModeDefaultIcon = colorsCount++;
    public static final int key_actionBarActionModeDefaultSelector = colorsCount++;
    public static final int key_actionBarDefaultTitle = colorsCount++;
    public static final int key_actionBarDefaultSubtitle = colorsCount++;
    public static final int key_actionBarDefaultSearch = colorsCount++;
    public static final int key_actionBarDefaultSearchPlaceholder = colorsCount++;
    public static final int key_actionBarDefaultSubmenuItem = colorsCount++;
    public static final int key_actionBarDefaultSubmenuItemIcon = colorsCount++;
    public static final int key_actionBarDefaultSubmenuBackground = colorsCount++;
    public static final int key_actionBarDefaultSubmenuSeparator = colorsCount++;
    public static final int key_actionBarTabActiveText = colorsCount++;
    public static final int key_actionBarTabUnactiveText = colorsCount++;
    public static final int key_actionBarTabLine = colorsCount++;
    public static final int key_actionBarTabSelector = colorsCount++;
    public static final int key_actionBarDefaultArchived = colorsCount++;
    public static final int key_actionBarDefaultArchivedSelector = colorsCount++;
    public static final int key_actionBarDefaultArchivedIcon = colorsCount++;
    public static final int key_actionBarDefaultArchivedTitle = colorsCount++;
    public static final int key_actionBarDefaultArchivedSearch = colorsCount++;
    public static final int key_actionBarDefaultArchivedSearchPlaceholder = colorsCount++;

    public static final int key_actionBarBrowser = colorsCount++;

    public static final int key_chats_onlineCircle = colorsCount++;
    public static final int key_chats_unreadCounter = colorsCount++;
    public static final int key_chats_unreadCounterMuted = colorsCount++;
    public static final int key_chats_unreadCounterText = colorsCount++;
    public static final int key_chats_name = colorsCount++;
    public static final int key_chats_nameArchived = colorsCount++;
    public static final int key_chats_secretName = colorsCount++;
    public static final int key_chats_secretIcon = colorsCount++;
    public static final int key_chats_pinnedIcon = colorsCount++;
    public static final int key_chats_archiveBackground = colorsCount++;
    public static final int key_chats_archivePinBackground = colorsCount++;
    public static final int key_chats_archiveIcon = colorsCount++;
    public static final int key_chats_archiveText = colorsCount++;
    public static final int key_chats_message = colorsCount++;
    public static final int key_chats_messageArchived = colorsCount++;
    public static final int key_chats_message_threeLines = colorsCount++;
    public static final int key_chats_draft = colorsCount++;
    public static final int key_chats_nameMessage = colorsCount++;
    public static final int key_chats_nameMessageArchived = colorsCount++;
    public static final int key_chats_nameMessage_threeLines = colorsCount++;
    public static final int key_chats_nameMessageArchived_threeLines = colorsCount++;
    public static final int key_chats_attachMessage = colorsCount++;
    public static final int key_chats_actionMessage = colorsCount++;
    public static final int key_chats_date = colorsCount++;
    public static final int key_chats_date_bold = colorsCount++;
    public static final int key_chats_pinnedOverlay = colorsCount++;
    public static final int key_chats_tabletSelectedOverlay = colorsCount++;
    public static final int key_chats_sentCheck = colorsCount++;
    public static final int key_chats_sentReadCheck = colorsCount++;
    public static final int key_chats_sentClock = colorsCount++;
    public static final int key_chats_sentError = colorsCount++;
    public static final int key_chats_sentErrorIcon = colorsCount++;
    public static final int key_chats_verifiedBackground = colorsCount++;
    public static final int key_chats_verifiedCheck = colorsCount++;
    public static final int key_chats_muteIcon = colorsCount++;
    public static final int key_chats_mentionIcon = colorsCount++;
    public static final int key_chats_menuTopShadow = colorsCount++;
    public static final int key_chats_menuTopShadowCats = colorsCount++;
    public static final int key_chats_menuBackground = colorsCount++;
    public static final int key_chats_menuItemText = colorsCount++;
    public static final int key_chats_menuItemCheck = colorsCount++;
    public static final int key_chats_menuItemIcon = colorsCount++;
    public static final int key_chats_menuName = colorsCount++;
    public static final int key_chats_menuPhone = colorsCount++;
    public static final int key_chats_menuPhoneCats = colorsCount++;
    public static final int key_chats_menuTopBackgroundCats = colorsCount++;
    public static final int key_chats_menuTopBackground = colorsCount++;
    public static final int key_chats_actionIcon = colorsCount++;
    public static final int key_chats_actionBackground = colorsCount++;
    public static final int key_chats_actionPressedBackground = colorsCount++;
    public static final int key_chats_archivePullDownBackground = colorsCount++;
    public static final int key_chats_archivePullDownBackgroundActive = colorsCount++;
    public static final int key_chats_tabUnreadActiveBackground = colorsCount++;
    public static final int key_chats_tabUnreadUnactiveBackground = colorsCount++;

    public static final int key_chat_attachCheckBoxCheck = colorsCount++;
    public static final int key_chat_attachCheckBoxBackground = colorsCount++;
    public static final int key_chat_attachPhotoBackground = colorsCount++;
    public static final int key_chat_attachActiveTab = colorsCount++;
    public static final int key_chat_attachUnactiveTab = colorsCount++;
    public static final int key_chat_attachPermissionImage = colorsCount++;
    public static final int key_chat_attachPermissionMark = colorsCount++;
    public static final int key_chat_attachPermissionText = colorsCount++;
    public static final int key_chat_attachEmptyImage = colorsCount++;

    public static final int key_chat_inPollCorrectAnswer = colorsCount++;
    public static final int key_chat_outPollCorrectAnswer = colorsCount++;
    public static final int key_chat_inPollWrongAnswer = colorsCount++;
    public static final int key_chat_outPollWrongAnswer = colorsCount++;

    public static final int key_chat_attachIcon = colorsCount++;
    public static final int key_chat_attachGalleryBackground = colorsCount++;   //
    public static final int key_chat_attachAudioBackground = colorsCount++; //
    public static final int key_chat_attachContactBackground = colorsCount++;   //
    public static final int key_chat_attachContactText = colorsCount++; //
    public static final int key_chat_attachLocationBackground = colorsCount++;  //
    public static final int key_chat_attachPollBackground = colorsCount++;  //

    public static final int key_chat_status = colorsCount++;
    public static final int key_chat_inGreenCall = colorsCount++;
    public static final int key_chat_inBubble = colorsCount++;
    public static final int key_chat_inBubbleSelectedOverlay = colorsCount++;
    public static final int key_chat_inBubbleShadow = colorsCount++;
    public static final int key_chat_compact_background = colorsCount++;
    public static final int key_chat_compact_backgroundGradient = colorsCount++;
    // compact mode's glass: its outline (0: the white highlight instead), its shadow and the tight shadow around it
    public static final int key_chat_compact_glassOutline = colorsCount++;
    public static final int key_chat_compact_glassShadow = colorsCount++;
    public static final int key_chat_compact_glassHairline = colorsCount++;
    public static final int key_chat_compact_topicSeparatorText = colorsCount++;
    // the outlines of the input field's and of the bottom buttons' glass (0 for none) and the fades at the chat's edges
    public static final int key_chat_compact_inputGlassOutline = colorsCount++;
    public static final int key_chat_compact_buttonGlassOutline = colorsCount++;
    public static final int key_chat_compact_fadeEdge = colorsCount++;
    public static final int key_chat_compact_fadeMiddle = colorsCount++;
    public static final int key_chat_compact_actionBackground = colorsCount++;
    public static final int key_chat_compact_actionText = colorsCount++;
    public static final int key_chat_compact_actionIcon = colorsCount++;
    public static final int key_chat_compact_botButtonText = colorsCount++;
    public static final int key_chat_compact_pullingDownText = colorsCount++;
    public static final int key_chat_compact_topicEmptyText = colorsCount++;
    public static final int key_chat_compact_progress = colorsCount++;
    public static final int key_chat_compact_outBackground = colorsCount++;
    public static final int key_chat_compact_outTimeText = colorsCount++;
    public static final int key_chat_compact_outCheck = colorsCount++;
    public static final int key_chat_compact_outReplyBackground = colorsCount++;
    public static final int key_chat_compact_outReplyLine = colorsCount++;
    public static final int key_chat_compact_outReplyText = colorsCount++;
    public static final int key_chat_compact_inTimeText = colorsCount++;
    public static final int key_chat_compact_replyText = colorsCount++;
    public static final int key_chat_compact_sideButtonIcon = colorsCount++;
    public static final int key_chat_compact_inAudioDuration = colorsCount++;
    public static final int key_chat_compact_inWaveform = colorsCount++;
    public static final int key_chat_compact_inWaveformBackground = colorsCount++;
    public static final int key_chat_compact_inTranscribeButton = colorsCount++;
    public static final int key_chat_compact_outAccent = colorsCount++;
    public static final int key_chat_compact_outAccentFill = colorsCount++;
    public static final int key_chat_compact_outAccentIcon = colorsCount++;
    public static final int key_chat_compact_outSecondaryText = colorsCount++;
    public static final int key_chat_compact_outAccentLight = colorsCount++;
    public static final int key_chat_compact_outAccentBorder = colorsCount++;
    public static final int key_chat_compact_reactionBackground = colorsCount++;
    public static final int key_chat_compact_reactionText = colorsCount++;
    public static final int key_chat_compact_reactionBackgroundSelected = colorsCount++;
    public static final int key_chat_compact_reactionTextSelected = colorsCount++;

    public static final int key_actionBarActionModeReaction = colorsCount++;
    public static final int key_actionBarActionModeReactionText = colorsCount++;
    public static final int key_actionBarActionModeReactionDot = colorsCount++;

    public static final int key_buttonNeutral = colorsCount++;
    public static final int key_buttonNeutralText = colorsCount++;

    //my messages bubbles
    public static final int myMessagesBubblesStartIndex = colorsCount;
    public static final int key_chat_outBubble = colorsCount++;
    public static final int key_chat_outBubbleSelected = colorsCount++;
    public static final int key_chat_outBubbleShadow = colorsCount++;
    public static final int key_chat_outBubbleGradient1 = colorsCount++;
    public static final int key_chat_outBubbleGradient2 = colorsCount++;
    public static final int key_chat_outBubbleGradient3 = colorsCount++;
    public static final int myMessagesBubblesEndIndex = colorsCount;

    //my messages
    public static final int myMessagesStartIndex = colorsCount;
    public static final int key_chat_outGreenCall = colorsCount++;
    public static final int key_chat_outSentCheck = colorsCount++;
    public static final int key_chat_outSentCheckSelected = colorsCount++;
    public static final int key_chat_outSentCheckRead = colorsCount++;
    public static final int key_chat_outSentCheckReadSelected = colorsCount++;
    public static final int key_chat_outSentClock = colorsCount++;
    public static final int key_chat_outSentClockSelected = colorsCount++;
    public static final int key_chat_outMediaIcon = colorsCount++;
    public static final int key_chat_outMediaIconSelected = colorsCount++;
    public static final int key_chat_outViews = colorsCount++;
    public static final int key_chat_outViewsSelected = colorsCount++;
    public static final int key_chat_outMenu = colorsCount++;
    public static final int key_chat_outMenuSelected = colorsCount++;
    public static final int key_chat_outInstant = colorsCount++;
    public static final int key_chat_outInstantSelected = colorsCount++;
    public static final int key_chat_outPreviewInstantText = colorsCount++;
    public static final int key_chat_outForwardedNameText = colorsCount++;
    public static final int key_chat_outViaBotNameText = colorsCount++;
    public static final int key_chat_outReplyLine = colorsCount++;
    public static final int key_chat_outReplyLine2 = colorsCount++;
    public static final int key_chat_outReplyNameText = colorsCount++;
    public static final int key_chat_outReplyMessageText = colorsCount++;
    public static final int key_chat_outReplyMediaMessageText = colorsCount++;
    public static final int key_chat_outReplyMediaMessageSelectedText = colorsCount++;
    public static final int key_chat_outPreviewLine = colorsCount++;
    public static final int key_chat_outSiteNameText = colorsCount++;
    public static final int key_chat_outContactNameText = colorsCount++;
    public static final int key_chat_outContactPhoneText = colorsCount++;
    public static final int key_chat_outContactPhoneSelectedText = colorsCount++;
    public static final int key_chat_outAudioPerformerText = colorsCount++;
    public static final int key_chat_outAudioPerformerSelectedText = colorsCount++;
    public static final int key_chat_outTimeSelectedText = colorsCount++;
    public static final int key_chat_outAdminText = colorsCount++;
    public static final int key_chat_outAdminSelectedText = colorsCount++;
    public static final int key_chat_outAudioProgress = colorsCount++;
    public static final int key_chat_outAudioSelectedProgress = colorsCount++;
    public static final int key_chat_outTimeText = colorsCount++;
    public static final int key_chat_outAudioTitleText = colorsCount++;
    public static final int key_chat_outAudioDurationText = colorsCount++;
    public static final int key_chat_outAudioDurationSelectedText = colorsCount++;
    public static final int key_chat_outAudioSeekbar = colorsCount++;
    public static final int key_chat_outAudioCacheSeekbar = colorsCount++;
    public static final int key_chat_outAudioSeekbarSelected = colorsCount++;
    public static final int key_chat_outAudioSeekbarFill = colorsCount++;
    public static final int key_chat_outVoiceSeekbar = colorsCount++;
    public static final int key_chat_outVoiceSeekbarSelected = colorsCount++;
    public static final int key_chat_outVoiceSeekbarFill = colorsCount++;
    public static final int key_chat_outFileProgress = colorsCount++;
    public static final int key_chat_outFileProgressSelected = colorsCount++;
    public static final int key_chat_outFileNameText = colorsCount++;
    public static final int key_chat_outFileInfoText = colorsCount++;
    public static final int key_chat_outFileInfoSelectedText = colorsCount++;
    public static final int key_chat_outFileBackground = colorsCount++;
    public static final int key_chat_outFileBackgroundSelected = colorsCount++;
    public static final int key_chat_outVenueInfoText = colorsCount++;
    public static final int key_chat_outVenueInfoSelectedText = colorsCount++;
    public static final int key_chat_outLinkSelectBackground = colorsCount++;
    public static final int key_chat_outLoader = colorsCount++;
    public static final int key_chat_outLoaderSelected = colorsCount++;
    public static final int key_chat_outLocationIcon = colorsCount++;
    public static final int key_chat_outContactBackground = colorsCount++;
    public static final int key_chat_outContactIcon = colorsCount++;
    public static final int key_chat_outReactionButtonBackground = colorsCount++;
    public static final int myMessagesEndIndex = colorsCount;

    public static final int myMessages2StartIndex = colorsCount;
    public static final int key_chat_outTextSelectionHighlight = colorsCount++;
    public static final int key_chat_outTextSelectionCursor = colorsCount++;
    public static final int key_chat_outBubbleLocationPlaceholder = colorsCount++;
    public static final int key_chat_outBubbleSelectedOverlay = colorsCount++;
    public static final int key_chat_outPsaNameText = colorsCount++;
    public static final int key_chat_outBubbleGradientAnimated = colorsCount++;
    public static final int key_chat_outBubbleGradientSelectedOverlay = colorsCount++;
    public static final int myMessages2EndIndex = colorsCount;

    public static final int key_chat_inBubbleSelected = colorsCount++;
    public static final int key_chat_messageTextIn = colorsCount++;
    public static final int key_chat_messageTextOut = colorsCount++;
    public static final int key_chat_messageLinkIn = colorsCount++;
    public static final int key_chat_messageLinkOut = colorsCount++;
    public static final int key_chat_serviceText = colorsCount++;
    public static final int key_chat_serviceLink = colorsCount++;
    public static final int key_chat_serviceIcon = colorsCount++;
    public static final int key_chat_serviceBackground = colorsCount++;
    public static final int key_chat_serviceBackgroundSelected = colorsCount++;
    public static final int key_chat_serviceBackgroundSelector = colorsCount++;
    public static final int key_chat_muteIcon = colorsCount++;
    public static final int key_chat_lockIcon = colorsCount++;
    public static final int key_chat_inSentClock = colorsCount++;
    public static final int key_chat_inSentClockSelected = colorsCount++;
    public static final int key_chat_mediaSentCheck = colorsCount++;
    public static final int key_chat_mediaSentClock = colorsCount++;
    public static final int key_chat_inMediaIcon = colorsCount++;
    public static final int key_chat_inMediaIconSelected = colorsCount++;

    public static final int key_chat_mediaTimeBackground = colorsCount++;
    public static final int key_chat_inViews = colorsCount++;
    public static final int key_chat_inViewsSelected = colorsCount++;
    public static final int key_chat_mediaViews = colorsCount++;
    public static final int key_chat_inMenu = colorsCount++;
    public static final int key_chat_inMenuSelected = colorsCount++;
    public static final int key_chat_mediaMenu = colorsCount++;
    public static final int key_chat_inInstant = colorsCount++;
    public static final int key_chat_inInstantSelected = colorsCount++;
    public static final int key_chat_sentError = colorsCount++;
    public static final int key_chat_sentErrorIcon = colorsCount++;
    public static final int key_chat_selectedBackground = colorsCount++;
    public static final int key_chat_previewDurationText = colorsCount++;
    public static final int key_chat_previewGameText = colorsCount++;
    public static final int key_chat_inPreviewInstantText = colorsCount++;
    public static final int key_chat_secretTimeText = colorsCount++;
    public static final int key_chat_stickerNameText = colorsCount++;
    public static final int key_chat_botButtonText = colorsCount++;
    public static final int key_chat_inForwardedNameText = colorsCount++;
    public static final int key_chat_inPsaNameText = colorsCount++;
    public static final int key_chat_inViaBotNameText = colorsCount++;
    public static final int key_chat_stickerViaBotNameText = colorsCount++;
    public static final int key_chat_inQuote = colorsCount++;
    public static final int key_chat_outQuote = colorsCount++;
    public static final int key_chat_inReplyLine = colorsCount++;
    public static final int key_chat_stickerReplyLine = colorsCount++;
    public static final int key_chat_inReplyNameText = colorsCount++;
    public static final int key_chat_stickerReplyNameText = colorsCount++;
    public static final int key_chat_inReplyMessageText = colorsCount++;
    public static final int key_chat_inReplyMediaMessageText = colorsCount++;
    public static final int key_chat_inReplyMediaMessageSelectedText = colorsCount++;
    public static final int key_chat_stickerReplyMessageText = colorsCount++;
    public static final int key_chat_inPreviewLine = colorsCount++;
    public static final int key_chat_inSiteNameText = colorsCount++;
    public static final int key_chat_inContactNameText = colorsCount++;
    public static final int key_chat_inContactPhoneText = colorsCount++;
    public static final int key_chat_inContactPhoneSelectedText = colorsCount++;
    public static final int key_chat_mediaProgress = colorsCount++;
    public static final int key_chat_inAudioProgress = colorsCount++;
    public static final int key_chat_inAudioSelectedProgress = colorsCount++;
    public static final int key_chat_mediaTimeText = colorsCount++;
    public static final int key_chat_inAdminText = colorsCount++;
    public static final int key_chat_inAdminSelectedText = colorsCount++;
    public static final int key_chat_inTimeText = colorsCount++;
    public static final int key_chat_inTimeSelectedText = colorsCount++;
    public static final int key_chat_inAudioPerformerText = colorsCount++;
    public static final int key_chat_inAudioPerformerSelectedText = colorsCount++;
    public static final int key_chat_inAudioTitleText = colorsCount++;
    public static final int key_chat_inAudioDurationText = colorsCount++;
    public static final int key_chat_inAudioDurationSelectedText = colorsCount++;
    public static final int key_chat_inAudioSeekbar = colorsCount++;
    public static final int key_chat_inAudioCacheSeekbar = colorsCount++;
    public static final int key_chat_inAudioSeekbarSelected = colorsCount++;
    public static final int key_chat_inAudioSeekbarFill = colorsCount++;
    public static final int key_chat_inVoiceSeekbar = colorsCount++;
    public static final int key_chat_inVoiceSeekbarSelected = colorsCount++;
    public static final int key_chat_inVoiceSeekbarFill = colorsCount++;
    public static final int key_chat_inFileProgress = colorsCount++;
    public static final int key_chat_inFileProgressSelected = colorsCount++;
    public static final int key_chat_inFileNameText = colorsCount++;
    public static final int key_chat_inFileInfoText = colorsCount++;
    public static final int key_chat_inFileInfoSelectedText = colorsCount++;
    public static final int key_chat_inFileBackground = colorsCount++;
    public static final int key_chat_inFileBackgroundSelected = colorsCount++;
    public static final int key_chat_inVenueInfoText = colorsCount++;
    public static final int key_chat_inVenueInfoSelectedText = colorsCount++;
    public static final int key_chat_mediaInfoText = colorsCount++;
    public static final int key_chat_linkSelectBackground = colorsCount++;
    public static final int key_chat_textSelectBackground = colorsCount++;
    public static final int key_chat_wallpaper = colorsCount++;
    public static final int key_chat_wallpaper_gradient_to1 = colorsCount++;
    public static final int key_chat_wallpaper_gradient_to2 = colorsCount++;
    public static final int key_chat_wallpaper_gradient_to3 = colorsCount++;
    public static final int key_chat_wallpaper_gradient_rotation = colorsCount++;
    public static final int key_chat_messagePanelBackground = colorsCount++;
    public static final int key_chat_messagePanelShadow = colorsCount++;
    public static final int key_chat_messagePanelText = colorsCount++;
    public static final int key_chat_messagePanelHint = colorsCount++;
    public static final int key_chat_messagePanelCursor = colorsCount++;
    public static final int key_chat_messagePanelIcons = colorsCount++;
    public static final int key_chat_messagePanelSend = colorsCount++;
    public static final int key_chat_messagePanelVoiceLock = colorsCount++;
    public static final int key_chat_messagePanelVoiceLockBackground = colorsCount++;
    public static final int key_chat_messagePanelVoiceLockShadow = colorsCount++;
    public static final int key_chat_topPanelBackground = colorsCount++;
    public static final int key_chat_topPanelClose = colorsCount++;
    public static final int key_chat_topPanelLine = colorsCount++;
    public static final int key_chat_topPanelTitle = colorsCount++;
    public static final int key_chat_topPanelMessage = colorsCount++;
    public static final int key_chat_addContact = colorsCount++;
    public static final int key_chat_inLoader = colorsCount++;
    public static final int key_chat_inLoaderSelected = colorsCount++;
    public static final int key_chat_inLoaderPhoto = colorsCount++;
    public static final int key_chat_mediaLoaderPhoto = colorsCount++;
    public static final int key_chat_mediaLoaderPhotoSelected = colorsCount++;
    public static final int key_chat_mediaLoaderPhotoIcon = colorsCount++;
    public static final int key_chat_mediaLoaderPhotoIconSelected = colorsCount++;
    public static final int key_chat_inLocationBackground = colorsCount++;
    public static final int key_chat_inLocationIcon = colorsCount++;
    public static final int key_chat_inContactBackground = colorsCount++;
    public static final int key_chat_inContactIcon = colorsCount++;
    public static final int key_chat_replyPanelIcons = colorsCount++;
    public static final int key_chat_replyPanelClose = colorsCount++;
    public static final int key_chat_replyPanelName = colorsCount++;
    public static final int key_chat_replyPanelLine = colorsCount++;
    public static final int key_chat_searchPanelIcons = colorsCount++;
    public static final int key_chat_searchPanelText = colorsCount++;
    public static final int key_chat_secretChatStatusText = colorsCount++;
    public static final int key_chat_fieldOverlayText = colorsCount++;
    public static final int key_chat_stickersHintPanel = colorsCount++;
    public static final int key_chat_botSwitchToInlineText = colorsCount++;
    public static final int key_chat_unreadMessagesStartArrowIcon = colorsCount++;
    public static final int key_chat_unreadMessagesStartText = colorsCount++;
    public static final int key_chat_unreadMessagesStartBackground = colorsCount++;
    public static final int key_chat_inlineResultIcon = colorsCount++;
    public static final int key_chat_emojiPanelBackground = colorsCount++;
    public static final int key_chat_emojiSearchBackground = colorsCount++;
    public static final int key_chat_emojiSearchIcon = colorsCount++;
    public static final int key_chat_emojiPanelShadowLine = colorsCount++;
    public static final int key_chat_emojiPanelEmptyText = colorsCount++;
    public static final int key_chat_emojiPanelIcon = colorsCount++;
    public static final int key_chat_emojiBottomPanelIcon = colorsCount++;
    public static final int key_chat_emojiPanelIconSelected = colorsCount++;
    public static final int key_chat_emojiPanelStickerPackSelector = colorsCount++;
    public static final int key_chat_emojiPanelStickerPackSelectorLine = colorsCount++;
    public static final int key_chat_emojiPanelBackspace = colorsCount++;
    public static final int key_chat_emojiPanelTrendingTitle = colorsCount++;
    public static final int key_chat_emojiPanelStickerSetName = colorsCount++;
    public static final int key_chat_emojiPanelStickerSetNameHighlight = colorsCount++;
    public static final int key_chat_emojiPanelStickerSetNameIcon = colorsCount++;
    public static final int key_chat_emojiPanelTrendingDescription = colorsCount++;
    public static final int key_chat_botKeyboardButtonText = colorsCount++;
    public static final int key_chat_botKeyboardButtonBackground = colorsCount++;
    public static final int key_chat_botKeyboardButtonBackgroundPressed = colorsCount++;
    public static final int key_chat_emojiPanelNewTrending = colorsCount++;
    public static final int key_chat_messagePanelVoicePressed = colorsCount++;
    public static final int key_chat_messagePanelVoiceBackground = colorsCount++;
    public static final int key_chat_messagePanelVoiceDelete = colorsCount++;
    public static final int key_chat_messagePanelVoiceDuration = colorsCount++;
    public static final int key_chat_recordedVoicePlayPause = colorsCount++;
    public static final int key_chat_recordedVoiceProgress = colorsCount++;
    public static final int key_chat_recordedVoiceProgressInner = colorsCount++;
    public static final int key_chat_recordedVoiceDot = colorsCount++;
    public static final int key_chat_recordedVoiceBackground = colorsCount++;
    public static final int key_chat_recordedVoiceDarkerBackground = colorsCount++;
    public static final int key_chat_recordVoiceCancel = colorsCount++;
    public static final int key_chat_recordTime = colorsCount++;
    public static final int key_chat_messagePanelCancelInlineBot = colorsCount++;
    public static final int key_chat_gifSaveHintText = colorsCount++;
    public static final int key_chat_gifSaveHintBackground = colorsCount++;
    public static final int key_chat_goDownButton = colorsCount++;
    public static final int key_chat_goDownButtonCounter = colorsCount++;
    public static final int key_chat_goDownButtonCounterBackground = colorsCount++;
    public static final int key_chat_inTextSelectionHighlight = colorsCount++;
    public static final int key_chat_TextSelectionCursor = colorsCount++;
    public static final int key_chat_inBubbleLocationPlaceholder = colorsCount++;
    public static final int key_chat_BlurAlpha = colorsCount++;
    public static final int key_chat_BlurAlphaSlow = colorsCount++;
    public static final int key_chat_editMediaButton = colorsCount++;

    public static final int key_chat_msgIvButtonDefaultIn = colorsCount++;
    public static final int key_chat_msgIvButtonDefaultInPressed = colorsCount++;
    public static final int key_chat_msgIvButtonDefaultInText = colorsCount++;
    public static final int key_chat_msgIvButtonPrimaryIn = colorsCount++;
    public static final int key_chat_msgIvButtonPrimaryInPressed = colorsCount++;
    public static final int key_chat_msgIvButtonPrimaryInText = colorsCount++;
    public static final int key_chat_msgIvButtonDangerIn = colorsCount++;
    public static final int key_chat_msgIvButtonDangerInPressed = colorsCount++;
    public static final int key_chat_msgIvButtonDangerInText = colorsCount++;
    public static final int key_chat_msgIvButtonSuccessIn = colorsCount++;
    public static final int key_chat_msgIvButtonSuccessInPressed = colorsCount++;
    public static final int key_chat_msgIvButtonSuccessInText = colorsCount++;
    public static final int key_chat_msgIvButtonDefaultInlineIn = colorsCount++;
    public static final int key_chat_msgIvButtonDefaultInlineInPressed = colorsCount++;
    public static final int key_chat_msgIvButtonDefaultInlineInText = colorsCount++;
    public static final int key_chat_msgIvButtonDefaultOut = colorsCount++;
    public static final int key_chat_msgIvButtonDefaultOutPressed = colorsCount++;
    public static final int key_chat_msgIvButtonDefaultOutText = colorsCount++;
    public static final int key_chat_msgIvButtonPrimaryOut = colorsCount++;
    public static final int key_chat_msgIvButtonPrimaryOutPressed = colorsCount++;
    public static final int key_chat_msgIvButtonPrimaryOutText = colorsCount++;
    public static final int key_chat_msgIvButtonDangerOut = colorsCount++;
    public static final int key_chat_msgIvButtonDangerOutPressed = colorsCount++;
    public static final int key_chat_msgIvButtonDangerOutText = colorsCount++;
    public static final int key_chat_msgIvButtonSuccessOut = colorsCount++;
    public static final int key_chat_msgIvButtonSuccessOutPressed = colorsCount++;
    public static final int key_chat_msgIvButtonSuccessOutText = colorsCount++;
    public static final int key_chat_msgIvButtonDefaultInlineOut = colorsCount++;
    public static final int key_chat_msgIvButtonDefaultInlineOutPressed = colorsCount++;
    public static final int key_chat_msgIvButtonDefaultInlineOutText = colorsCount++;

    public static final int key_voipgroup_listSelector = colorsCount++;
    public static final int key_voipgroup_inviteMembersBackground = colorsCount++;
    public static final int key_voipgroup_actionBar = colorsCount++;
    public static final int key_voipgroup_actionBarItems = colorsCount++;
    public static final int key_voipgroup_actionBarItemsSelector = colorsCount++;
    public static final int key_voipgroup_actionBarUnscrolled = colorsCount++;
    public static final int key_voipgroup_listViewBackgroundUnscrolled = colorsCount++;
    public static final int key_voipgroup_lastSeenTextUnscrolled = colorsCount++;
    public static final int key_voipgroup_mutedIconUnscrolled = colorsCount++;
    public static final int key_voipgroup_nameText = colorsCount++;
    public static final int key_voipgroup_lastSeenText = colorsCount++;
    public static final int key_voipgroup_listeningText = colorsCount++;
    public static final int key_voipgroup_speakingText = colorsCount++;
    public static final int key_voipgroup_mutedIcon = colorsCount++;
    public static final int key_voipgroup_mutedByAdminIcon = colorsCount++;
    public static final int key_voipgroup_listViewBackground = colorsCount++;
    public static final int key_voipgroup_dialogBackground = colorsCount++;
    public static final int key_voipgroup_leaveCallMenu = colorsCount++;
    public static final int key_voipgroup_checkMenu = colorsCount++;
    public static final int key_voipgroup_soundButton = colorsCount++;
    public static final int key_voipgroup_soundButtonActive = colorsCount++;
    public static final int key_voipgroup_soundButtonActiveScrolled = colorsCount++;
    public static final int key_voipgroup_soundButton2 = colorsCount++;
    public static final int key_voipgroup_soundButtonActive2 = colorsCount++;
    public static final int key_voipgroup_soundButtonActive2Scrolled = colorsCount++;
    public static final int key_voipgroup_leaveButton = colorsCount++;
    public static final int key_voipgroup_leaveButtonScrolled = colorsCount++;
    public static final int key_voipgroup_muteButton = colorsCount++;
    public static final int key_voipgroup_muteButton2 = colorsCount++;
    public static final int key_voipgroup_muteButton3 = colorsCount++;
    public static final int key_voipgroup_unmuteButton = colorsCount++;
    public static final int key_voipgroup_unmuteButton2 = colorsCount++;
    public static final int key_voipgroup_disabledButton = colorsCount++;
    public static final int key_voipgroup_rtmpButton = colorsCount++;
    public static final int key_voipgroup_disabledButtonActive = colorsCount++;
    public static final int key_voipgroup_disabledButtonActiveScrolled = colorsCount++;
    public static final int key_voipgroup_connectingProgress = colorsCount++;
    public static final int key_voipgroup_scrollUp = colorsCount++;
    public static final int key_voipgroup_searchPlaceholder = colorsCount++;
    public static final int key_voipgroup_searchBackground = colorsCount++;
    public static final int key_voipgroup_searchText = colorsCount++;
    public static final int key_voipgroup_overlayGreen1 = colorsCount++;
    public static final int key_voipgroup_overlayGreen2 = colorsCount++;
    public static final int key_voipgroup_overlayBlue1 = colorsCount++;
    public static final int key_voipgroup_overlayBlue2 = colorsCount++;
    public static final int key_voipgroup_topPanelGreen1 = colorsCount++;
    public static final int key_voipgroup_topPanelGreen2 = colorsCount++;
    public static final int key_voipgroup_topPanelBlue1 = colorsCount++;
    public static final int key_voipgroup_topPanelBlue2 = colorsCount++;
    public static final int key_voipgroup_topPanelGray = colorsCount++;
    public static final int key_voipgroup_overlayAlertGradientMuted = colorsCount++;
    public static final int key_voipgroup_overlayAlertGradientMuted2 = colorsCount++;
    public static final int key_voipgroup_overlayAlertGradientUnmuted = colorsCount++;
    public static final int key_voipgroup_overlayAlertGradientUnmuted2 = colorsCount++;
    public static final int key_voipgroup_overlayAlertMutedByAdmin = colorsCount++;
    public static final int key_voipgroup_overlayAlertMutedByAdmin2 = colorsCount++;
    public static final int key_voipgroup_mutedByAdminGradient = colorsCount++;
    public static final int key_voipgroup_mutedByAdminGradient2 = colorsCount++;
    public static final int key_voipgroup_mutedByAdminGradient3 = colorsCount++;
    public static final int key_voipgroup_mutedByAdminMuteButton = colorsCount++;
    public static final int key_voipgroup_mutedByAdminMuteButtonDisabled = colorsCount++;
    public static final int key_voipgroup_windowBackgroundWhiteInputField = colorsCount++;
    public static final int key_voipgroup_windowBackgroundWhiteInputFieldActivated = colorsCount++;

    public static final int key_table_background = colorsCount++;
    public static final int key_table_border = colorsCount++;

    public static final int key_passport_authorizeBackground = colorsCount++;
    public static final int key_passport_authorizeBackgroundSelected = colorsCount++;
    public static final int key_passport_authorizeText = colorsCount++;

    public static final int key_profile_creatorIcon = colorsCount++;
    public static final int key_profile_title = colorsCount++;
    public static final int key_profile_actionIcon = colorsCount++;
    public static final int key_profile_actionBackground = colorsCount++;
    public static final int key_profile_actionPressedBackground = colorsCount++;
    public static final int key_profile_verifiedBackground = colorsCount++;
    public static final int key_profile_verifiedCheck = colorsCount++;
    public static final int key_profile_status = colorsCount++;
    public static final int key_chat_tagAdmin = colorsCount++;
    public static final int key_chat_tagCreator = colorsCount++;

    public static final int key_profile_tabText = colorsCount++;
    public static final int key_profile_tabSelectedText = colorsCount++;
    public static final int key_profile_tabSelectedLine = colorsCount++;
    public static final int key_profile_tabSelector = colorsCount++;

    public static final int key_sharedMedia_startStopLoadIcon = colorsCount++;
    public static final int key_sharedMedia_linkPlaceholder = colorsCount++;
    public static final int key_sharedMedia_linkPlaceholderText = colorsCount++;
    public static final int key_sharedMedia_photoPlaceholder = colorsCount++;

    public static final int key_featuredStickers_addedIcon = colorsCount++;
    public static final int key_featuredStickers_buttonProgress = colorsCount++;
    public static final int key_featuredStickers_addButton = colorsCount++;
    public static final int key_featuredStickers_addButton2 = colorsCount++;
    public static final int key_featuredStickers_addButtonPressed = colorsCount++;
    public static final int key_featuredStickers_removeButtonText = colorsCount++;
    public static final int key_featuredStickers_buttonText = colorsCount++;
    public static final int key_featuredStickers_unread = colorsCount++;

    public static final int key_stickers_menu = colorsCount++;
    public static final int key_stickers_menuSelector = colorsCount++;

    public static final int key_changephoneinfo_image2 = colorsCount++;

    public static final int key_groupcreate_hintText = colorsCount++;
    public static final int key_groupcreate_cursor = colorsCount++;
    public static final int key_groupcreate_sectionShadow = colorsCount++;
    public static final int key_groupcreate_sectionText = colorsCount++;
    public static final int key_groupcreate_spanText = colorsCount++;
    public static final int key_groupcreate_spanBackground = colorsCount++;
    public static final int key_groupcreate_spanDelete = colorsCount++;

    public static final int key_contacts_inviteBackground = colorsCount++;
    public static final int key_contacts_inviteText = colorsCount++;

    public static final int key_login_progressInner = colorsCount++;
    public static final int key_login_progressOuter = colorsCount++;

    public static final int key_picker_enabledButton = colorsCount++;
    public static final int key_picker_disabledButton = colorsCount++;
    public static final int key_picker_badge = colorsCount++;
    public static final int key_picker_badgeText = colorsCount++;

    public static final int key_location_sendLocationBackground = colorsCount++;
    public static final int key_location_sendLocationIcon = colorsCount++;
    public static final int key_location_sendLocationText = colorsCount++;
    public static final int key_location_sendLiveLocationBackground = colorsCount++;
    public static final int key_location_sendLiveLocationIcon = colorsCount++;
    public static final int key_location_sendLiveLocationText = colorsCount++;
    public static final int key_location_liveLocationProgress = colorsCount++;
    public static final int key_location_placeLocationBackground = colorsCount++;
    public static final int key_location_actionIcon = colorsCount++;
    public static final int key_location_actionActiveIcon = colorsCount++;
    public static final int key_location_actionBackground = colorsCount++;
    public static final int key_location_actionPressedBackground = colorsCount++;

    public static final int key_dialog_liveLocationProgress = colorsCount++;

    public static final int key_files_folderIcon = colorsCount++;
    public static final int key_files_folderIconBackground = colorsCount++;
    public static final int key_files_iconText = colorsCount++;

    public static final int key_sessions_devicesImage = colorsCount++;

    public static final int key_calls_callReceivedGreenIcon = colorsCount++;
    public static final int key_calls_callReceivedRedIcon = colorsCount++;

    public static final int key_undo_background = colorsCount++;
    public static final int key_undo_cancelColor = colorsCount++;
    public static final int key_undo_infoColor = colorsCount++;

    public static final int key_sheet_scrollUp = colorsCount++;
    public static final int key_sheet_other = colorsCount++;
    public static final int key_bot_loadingIcon = colorsCount++;
    public static final int key_gift_ribbon = colorsCount++;
    public static final int key_gift_ribbon_soldout = colorsCount++;

    public static final int key_player_actionBarSelector = colorsCount++;
    public static final int key_player_actionBarTitle = colorsCount++;
    public static final int key_player_actionBarSubtitle = colorsCount++;
    public static final int key_player_actionBarItems = colorsCount++;
    public static final int key_player_background = colorsCount++;
    public static final int key_player_time = colorsCount++;
    public static final int key_player_progressBackground = colorsCount++;
    public static final int key_player_progressCachedBackground = colorsCount++;
    public static final int key_player_progress = colorsCount++;
    public static final int key_player_button = colorsCount++;
    public static final int key_player_buttonActive = colorsCount++;

    public static final int key_statisticChartSignature = colorsCount++;
    public static final int key_statisticChartSignatureAlpha = colorsCount++;
    public static final int key_statisticChartHintLine = colorsCount++;
    public static final int key_statisticChartActiveLine = colorsCount++;
    public static final int key_statisticChartInactivePickerChart = colorsCount++;
    public static final int key_statisticChartActivePickerChart = colorsCount++;
    public static final int key_statisticChartRipple = colorsCount++;
    public static final int key_statisticChartBackZoomColor = colorsCount++;
    public static final int key_statisticChartChevronColor = colorsCount++;
    public static final int key_statisticChartLine_blue = colorsCount++;
    public static final int key_statisticChartLine_green = colorsCount++;
    public static final int key_statisticChartLine_red = colorsCount++;
    public static final int key_statisticChartLine_golden = colorsCount++;
    public static final int key_statisticChartLine_lightblue = colorsCount++;
    public static final int key_statisticChartLine_lightgreen = colorsCount++;
    public static final int key_statisticChartLine_orange = colorsCount++;
    public static final int key_statisticChartLine_indigo = colorsCount++;
    public static final int key_statisticChartLine_purple = colorsCount++;
    public static final int key_statisticChartLine_cyan = colorsCount++;
    public static final int key_statisticChartLineEmpty = colorsCount++;

    public static final int key_color_lightblue = colorsCount++;
    public static final int key_color_blue = colorsCount++;
    public static final int key_color_green = colorsCount++;
    public static final int key_color_lightgreen = colorsCount++;
    public static final int key_color_red = colorsCount++;
    public static final int key_color_orange = colorsCount++;
    public static final int key_color_yellow = colorsCount++;
    public static final int key_color_purple = colorsCount++;
    public static final int key_color_cyan = colorsCount++;
    public static final int[] keys_colors = {key_color_lightblue, key_color_blue, key_color_green, key_color_lightgreen, key_color_red, key_color_orange, key_color_yellow, key_color_purple, key_color_cyan};

    public static final int key_chat_inReactionButtonBackground = colorsCount++;
    public static final int key_chat_outReactionButtonText = colorsCount++;
    public static final int key_chat_inReactionButtonText = colorsCount++;
    public static final int key_chat_inReactionButtonTextSelected = colorsCount++;
    public static final int key_chat_outReactionButtonTextSelected = colorsCount++;
    public static final int key_chat_reactionServiceButtonBackgroundSelected = colorsCount++;
    public static final int key_chat_reactionServiceButtonTextSelected = colorsCount++;
    public static final int key_reactionStarSelector = colorsCount++;

    public static final int key_premiumGradient0 = colorsCount++;
    public static final int key_premiumGradient1 = colorsCount++;
    public static final int key_premiumGradient2 = colorsCount++;
    public static final int key_premiumGradient3 = colorsCount++;
    public static final int key_premiumGradient4 = colorsCount++;
    public static final int key_premiumGradientBackground1 = colorsCount++;
    public static final int key_premiumGradientBackground2 = colorsCount++;
    public static final int key_premiumGradientBackground3 = colorsCount++;
    public static final int key_premiumGradientBackground4 = colorsCount++;
    public static final int key_premiumGradientBackgroundOverlay = colorsCount++;
    public static final int key_premiumStartSmallStarsColor = colorsCount++;
    public static final int key_premiumStarGradient1 = colorsCount++;
    public static final int key_premiumStarGradient2 = colorsCount++;
    public static final int key_premiumCoinGradient1 = colorsCount++;
    public static final int key_premiumCoinGradient2 = colorsCount++;
    public static final int key_premiumStartSmallStarsColor2 = colorsCount++;
    public static final int key_premiumGradientBottomSheet1 = colorsCount++;
    public static final int key_premiumGradientBottomSheet2 = colorsCount++;
    public static final int key_premiumGradientBottomSheet3 = colorsCount++;
    public static final int key_topics_unreadCounter = colorsCount++;
    public static final int key_topics_unreadCounterMuted = colorsCount++;
    public static final int key_starsGradient1 = colorsCount++;
    public static final int key_starsGradient2 = colorsCount++;

    public static final int key_stories_circle1 = colorsCount++;
    public static final int key_stories_circle2 = colorsCount++;
    public static final int key_stories_circle_dialog1 = colorsCount++;
    public static final int key_stories_circle_dialog2 = colorsCount++;
    public static final int key_stories_circle_closeFriends1 = colorsCount++;
    public static final int key_stories_circle_closeFriends2 = colorsCount++;
    public static final int key_stories_circle_live1 = colorsCount++;
    public static final int key_stories_circle_live2 = colorsCount++;

    public static final int key_chat_inCodeBackground = colorsCount++;
    public static final int key_chat_outCodeBackground = colorsCount++;
    public static final int key_chat_inTableBackground = colorsCount++;
    public static final int key_chat_outTableBackground = colorsCount++;
    public static final int key_chat_inTableBorder = colorsCount++;
    public static final int key_chat_outTableBorder = colorsCount++;
    public static final int key_chat_inDivider = colorsCount++;
    public static final int key_chat_outDivider = colorsCount++;
    public static final int key_chat_inArticleCodeBackground = colorsCount++;
    public static final int key_chat_outArticleCodeBackground = colorsCount++;
    public static final int key_chat_inArticleCodeScrollbarBackground = colorsCount++;
    public static final int key_chat_inArticleCodeScrollbar = colorsCount++;
    public static final int key_chat_outArticleCodeScrollbarBackground = colorsCount++;
    public static final int key_chat_outArticleCodeScrollbar = colorsCount++;
    public static final int key_chat_inArticleDetailsArrow = colorsCount++;
    public static final int key_chat_outArticleDetailsArrow = colorsCount++;
    public static final int key_chat_inArticleDetailsLine = colorsCount++;
    public static final int key_chat_outArticleDetailsLine = colorsCount++;
    private static final int[] myMessagesAccentExtraKeys = { key_chat_outDivider };
    public static final int key_code_keyword = colorsCount++;
    public static final int key_code_operator = colorsCount++;
    public static final int key_code_constant = colorsCount++;
    public static final int key_code_string = colorsCount++;
    public static final int key_code_number = colorsCount++;
    public static final int key_code_comment = colorsCount++;
    public static final int key_code_function = colorsCount++;

    public static final int key_iv_background = colorsCount++;
    public static final int key_iv_backgroundGray = colorsCount++;
    public static final int key_iv_ab_progress = colorsCount++;
    public static final int key_iv_navigationBackground = colorsCount++;

    public static final int key_share_linkText = colorsCount++;
    public static final int key_share_linkBackground = colorsCount++;
    public static final int key_share_icon = colorsCount++;

    public static final int key_glass_defaultIcon = colorsCount++;
    public static final int key_glass_defaultText = colorsCount++;
    public static final int key_glass_targetMainTabs = colorsCount++;
    public static final int key_glass_targetMainTopPanel = colorsCount++;
    public static final int key_glass_tabSelected = colorsCount++;
    public static final int key_glass_tabSelectedText = colorsCount++;
    public static final int key_glass_tabUnselected = colorsCount++;

    public static final int key_botKeyboard_button_primary = colorsCount++;
    public static final int key_botKeyboard_button_danger = colorsCount++;
    public static final int key_botKeyboard_button_success = colorsCount++;

    public static final int key_telegram_color_dialogsLogo = colorsCount++;
    public static final int key_telegram_color = colorsCount++;
    public static final int key_telegram_color_text = colorsCount++;

    public static final int key_wallet_comment = colorsCount++;
    public static final int key_wallet_out_commentBorder = colorsCount++;
    public static final int key_wallet_inputCurrency = colorsCount++;

    public static final String key_drawable_botInline = "drawableBotInline";
    public static final String key_drawable_botLink = "drawableBotLink";
    public static final String key_drawable_botWebView = "drawableBotWebView";
    public static final String key_drawable_botLock = "drawableBotLock";
    public static final String key_drawable_botInvite = "drawable_botInvite";
    public static final String key_drawable_commentSticker = "drawableCommentSticker";
    public static final String key_drawable_goIcon = "drawableGoIcon";
    public static final String key_drawable_msgError = "drawableMsgError";
    public static final String key_drawable_msgIn = "drawableMsgIn";
    public static final String key_drawable_msgInInstant = "drawableMsgInInstant";
    public static final String key_drawable_msgInClock = "drawableMsgInClock";
    public static final String key_drawable_msgInClockSelected = "drawableMsgInClockSelected";
    public static final String key_drawable_msgInSelected = "drawableMsgInSelected";
    public static final String key_drawable_msgInMedia = "drawableMsgInMedia";
    public static final String key_drawable_msgInMediaSelected = "drawableMsgInMediaSelected";
    public static final String key_drawable_msgOut = "drawableMsgOut";
    public static final String key_drawable_msgOutSelected = "drawableMsgOutSelected";
    public static final String key_drawable_msgOutCallAudio = "drawableMsgOutCallAudio";
    public static final String key_drawable_msgOutCallAudioSelected = "drawableMsgOutCallAudioSelected";
    public static final String key_drawable_msgOutCallVideo = "drawableMsgOutCallVideo";
    public static final String key_drawable_msgOutCallVideoSelected = "drawableMsgOutCallVideo";
    public static final String key_drawable_msgOutCheck = "drawableMsgOutCheck";
    public static final String key_drawable_msgOutCheckSelected = "drawableMsgOutCheckSelected";
    public static final String key_drawable_msgOutCheckRead = "drawableMsgOutCheckRead";
    public static final String key_drawable_msgOutCheckReadSelected = "drawableMsgOutCheckReadSelected";
    public static final String key_drawable_msgOutHalfCheck = "drawableMsgOutHalfCheck";
    public static final String key_drawable_msgOutHalfCheckSelected = "drawableMsgOutHalfCheckSelected";
    public static final String key_drawable_msgOutInstant = "drawableMsgOutInstant";
    public static final String key_drawable_msgOutMedia = "drawableMsgOutMedia";
    public static final String key_drawable_msgOutMediaSelected = "drawableMsgOutMediaSelected";
    public static final String key_drawable_msgOutMenu = "drawableMsgOutMenu";
    public static final String key_drawable_msgOutMenuSelected = "drawableMsgOutMenuSelected";
    public static final String key_drawable_msgOutPinned = "drawableMsgOutPinned";
    public static final String key_drawable_msgOutPinnedSelected = "drawableMsgOutPinnedSelected";
    public static final String key_drawable_msgOutReplies = "drawableMsgOutReplies";
    public static final String key_drawable_msgOutRepliesSelected = "drawableMsgOutReplies";
    public static final String key_drawable_msgOutViews = "drawableMsgOutViews";
    public static final String key_drawable_msgOutViewsSelected = "drawableMsgOutViewsSelected";
    public static final String key_drawable_msgStickerCheck = "drawableMsgStickerCheck";
    public static final String key_drawable_msgStickerClock = "drawableMsgStickerClock";
    public static final String key_drawable_msgStickerHalfCheck = "drawableMsgStickerHalfCheck";
    public static final String key_drawable_msgStickerPinned = "drawableMsgStickerPinned";
    public static final String key_drawable_msgStickerReplies = "drawableMsgStickerReplies";
    public static final String key_drawable_msgStickerViews = "drawableMsgStickerViews";
    public static final String key_drawable_replyIcon = "drawableReplyIcon";
    public static final String key_drawable_shareIcon = "drawableShareIcon";
    public static final String key_drawable_closeIcon = "drawableCloseIcon";
    public static final String key_drawable_moreIcon = "drawableMoreIcon";
    public static final String key_drawable_muteIconDrawable = "drawableMuteIcon";
    public static final String key_drawable_lockIconDrawable = "drawableLockIcon";
    public static final String key_drawable_chat_pollHintDrawableOut = "drawable_chat_pollHintDrawableOut";
    public static final String key_drawable_chat_pollHintDrawableIn = "drawable_chat_pollHintDrawableIn";

    private static final HashMap<String, Drawable> defaultChatDrawables = new HashMap<>();
    private static final HashMap<String, Integer> defaultChatDrawableColorKeys = new HashMap<>();

    public static final String key_paint_chatActionBackground = "paintChatActionBackground";
    public static final String key_paint_chatActionBackgroundDarken = "paintChatActionBackgroundDarken";
    public static final String key_paint_chatActionBackgroundSelected = "paintChatActionBackgroundSelected";
    public static final String key_paint_chatMessageBackgroundSelected = "paintChatMessageBackgroundSelected";
    public static final String key_paint_chatActionText = "paintChatActionText";
    public static final String key_paint_chatActionText2 = "paintChatActionText2";
    public static final String key_paint_chatActionText3 = "paintChatActionText3";
    public static final String key_paint_chatBotButton = "paintChatBotButton";
    public static final String key_paint_chatComposeBackground = "paintChatComposeBackground";
    public static final String key_paint_divider = "paintDivider";
    public static final String key_paint_chatTimeBackground = "paintChatTimeBackground";
    private static final HashMap<String, Paint> defaultChatPaints = new HashMap<>();
    private static final HashMap<String, Integer> defaultChatPaintColors = new HashMap<>();

    private static int[] defaultColors;
    private static SparseIntArray fallbackKeys = new SparseIntArray();
    private static HashSet<Integer> themeAccentExclusionKeys = new HashSet<>();
    private static SparseIntArray currentColorsNoAccent;
    private static SparseIntArray currentColors;
    private static SparseIntArray animatingColors;
    public static boolean shouldDrawGradientIcons;

    private static final ThreadLocal<float[]> hsvTemp1Local = new ThreadLocal<>();
    private static final ThreadLocal<float[]> hsvTemp2Local = new ThreadLocal<>();
    private static final ThreadLocal<float[]> hsvTemp3Local = new ThreadLocal<>();
    private static final ThreadLocal<float[]> hsvTemp4Local = new ThreadLocal<>();
    private static final ThreadLocal<float[]> hsvTemp5Local = new ThreadLocal<>();

    static {
        defaultColors = ThemeColors.createDefaultColors();

        fallbackKeys.put(key_iv_background, key_windowBackgroundWhite);
        fallbackKeys.put(key_iv_backgroundGray, key_windowBackgroundGray);
        fallbackKeys.put(key_iv_navigationBackground, key_windowBackgroundGray);
        fallbackKeys.put(key_bot_loadingIcon, key_groupcreate_spanBackground);
        fallbackKeys.put(key_gift_ribbon_soldout, key_text_RedBold);
        fallbackKeys.put(key_iv_ab_progress, key_featuredStickers_addButton);
        fallbackKeys.put(key_featuredStickers_addButton2, key_featuredStickers_addButton);
        fallbackKeys.put(key_dialogGiftsBackground, key_windowBackgroundGray);
        fallbackKeys.put(key_dialogGiftsTabText, key_windowBackgroundWhiteGrayText2);
        fallbackKeys.put(key_chat_inQuote, key_featuredStickers_addButtonPressed);
        fallbackKeys.put(key_chat_outQuote, key_chat_outReplyLine);
        fallbackKeys.put(key_chat_outReplyLine2, key_chat_outReplyLine);
        fallbackKeys.put(key_chat_inAdminText, key_chat_inTimeText);
        fallbackKeys.put(key_chat_inAdminSelectedText, key_chat_inTimeSelectedText);
        fallbackKeys.put(key_player_progressCachedBackground, key_player_progressBackground);
        fallbackKeys.put(key_chat_inAudioCacheSeekbar, key_chat_inAudioSeekbar);
        fallbackKeys.put(key_chat_outAudioCacheSeekbar, key_chat_outAudioSeekbar);
        fallbackKeys.put(key_chat_emojiSearchBackground, key_chat_emojiPanelStickerPackSelector);
        fallbackKeys.put(key_location_sendLiveLocationIcon, key_location_sendLocationIcon);
        fallbackKeys.put(key_changephoneinfo_image2, key_featuredStickers_addButton);
        fallbackKeys.put(key_graySectionText, key_windowBackgroundWhiteGrayText2);
        fallbackKeys.put(key_chat_inMediaIcon, key_chat_inBubble);
        fallbackKeys.put(key_chat_compact_background, key_windowBackgroundWhite);
        fallbackKeys.put(key_chat_outMediaIcon, key_chat_outBubble);
        fallbackKeys.put(key_chat_inMediaIconSelected, key_chat_inBubbleSelected);
        fallbackKeys.put(key_chat_outMediaIconSelected, key_chat_outBubbleSelected);
        fallbackKeys.put(key_dialog_inlineProgressBackground, key_windowBackgroundGray);
        fallbackKeys.put(key_dialog_inlineProgress, key_chats_menuItemIcon);
        fallbackKeys.put(key_groupcreate_spanDelete, key_chats_actionIcon);
        fallbackKeys.put(key_sharedMedia_photoPlaceholder, key_windowBackgroundGray);
        fallbackKeys.put(key_chat_attachPollBackground, key_chat_attachAudioBackground);
        fallbackKeys.put(key_chats_onlineCircle, key_windowBackgroundWhiteBlueText);
        fallbackKeys.put(key_windowBackgroundWhiteBlueButton, key_windowBackgroundWhiteValueText);
        fallbackKeys.put(key_windowBackgroundWhiteBlueIcon, key_windowBackgroundWhiteValueText);
        fallbackKeys.put(key_undo_background, key_chat_gifSaveHintBackground);
        fallbackKeys.put(key_undo_cancelColor, key_chat_gifSaveHintText);
        fallbackKeys.put(key_undo_infoColor, key_chat_gifSaveHintText);
        fallbackKeys.put(key_windowBackgroundUnchecked, key_windowBackgroundWhite);
        fallbackKeys.put(key_windowBackgroundChecked, key_windowBackgroundWhite);
        fallbackKeys.put(key_switchTrackBlue, key_switchTrack);
        fallbackKeys.put(key_switchTrackBlueChecked, key_switchTrackChecked);
        fallbackKeys.put(key_switchTrackBlueThumb, key_windowBackgroundWhite);
        fallbackKeys.put(key_switchTrackBlueThumbChecked, key_windowBackgroundWhite);
        fallbackKeys.put(key_windowBackgroundCheckText, key_windowBackgroundWhite);
        fallbackKeys.put(key_contextProgressInner4, key_contextProgressInner1);
        fallbackKeys.put(key_contextProgressOuter4, key_contextProgressOuter1);
        fallbackKeys.put(key_switchTrackBlueSelector, key_listSelector);
        fallbackKeys.put(key_switchTrackBlueSelectorChecked, key_listSelector);
        fallbackKeys.put(key_settings_listSelector, key_listSelector);
        fallbackKeys.put(key_chat_emojiBottomPanelIcon, key_chat_emojiPanelIcon);
        fallbackKeys.put(key_chat_emojiSearchIcon, key_chat_emojiPanelIcon);
        fallbackKeys.put(key_chat_emojiPanelStickerSetNameHighlight, key_windowBackgroundWhiteBlueText4);
        fallbackKeys.put(key_chat_emojiPanelStickerPackSelectorLine, key_chat_emojiPanelIconSelected);
        fallbackKeys.put(key_sheet_scrollUp, key_chat_emojiPanelStickerPackSelector);
        fallbackKeys.put(key_sheet_other, key_player_actionBarItems);
        fallbackKeys.put(key_dialogSearchBackground, key_chat_emojiPanelStickerPackSelector);
        fallbackKeys.put(key_dialogSearchHint, key_chat_emojiPanelIcon);
        fallbackKeys.put(key_dialogSearchIcon, key_chat_emojiPanelIcon);
        fallbackKeys.put(key_dialogSearchText, key_windowBackgroundWhiteBlackText);
        fallbackKeys.put(key_buttonNeutral, key_chat_replyPanelLine);
        fallbackKeys.put(key_buttonNeutralText, key_windowBackgroundWhiteBlackText);
        fallbackKeys.put(key_dialogFloatingButton, key_dialogRoundCheckBox);
        fallbackKeys.put(key_dialogFloatingButtonPressed, key_dialogRoundCheckBox);
        fallbackKeys.put(key_dialogFloatingIcon, key_dialogRoundCheckBoxCheck);
        fallbackKeys.put(key_dialogShadowLine, key_chat_emojiPanelShadowLine);
        fallbackKeys.put(key_actionBarDefaultArchived, key_actionBarDefault);
        fallbackKeys.put(key_actionBarDefaultArchivedSelector, key_actionBarDefaultSelector);
        fallbackKeys.put(key_actionBarDefaultArchivedIcon, key_actionBarDefaultIcon);
        fallbackKeys.put(key_actionBarDefaultArchivedTitle, key_windowBackgroundWhiteBlackText);
        fallbackKeys.put(key_actionBarDefaultArchivedSearch, key_actionBarDefaultSearch);
        fallbackKeys.put(key_actionBarDefaultArchivedSearchPlaceholder, key_actionBarDefaultSearchPlaceholder);
        fallbackKeys.put(key_chats_message_threeLines, key_chats_message);
        fallbackKeys.put(key_chats_nameMessage_threeLines, key_chats_nameMessage);
        fallbackKeys.put(key_chats_nameArchived, key_chats_name);
        fallbackKeys.put(key_chats_nameMessageArchived, key_chats_nameMessage);
        fallbackKeys.put(key_chats_nameMessageArchived_threeLines, key_chats_nameMessage);
        fallbackKeys.put(key_chats_messageArchived, key_chats_message);
        fallbackKeys.put(key_avatar_backgroundArchived, key_chats_unreadCounterMuted);
        fallbackKeys.put(key_chats_archiveBackground, key_chats_actionBackground);
        fallbackKeys.put(key_chats_archivePinBackground, key_chats_unreadCounterMuted);
        fallbackKeys.put(key_chats_archiveIcon, key_chats_actionIcon);
        fallbackKeys.put(key_chats_archiveText, key_chats_actionIcon);
        fallbackKeys.put(key_actionBarDefaultSubmenuItemIcon, key_dialogIcon);
        fallbackKeys.put(key_checkboxDisabled, key_chats_unreadCounterMuted);
        fallbackKeys.put(key_chat_status, key_actionBarDefaultSubtitle);
        fallbackKeys.put(key_chat_inGreenCall, key_calls_callReceivedGreenIcon);
        fallbackKeys.put(key_chat_outGreenCall, key_calls_callReceivedGreenIcon);
        fallbackKeys.put(key_actionBarTabActiveText, key_actionBarDefaultTitle);
        fallbackKeys.put(key_actionBarTabUnactiveText, key_actionBarDefaultSubtitle);
        fallbackKeys.put(key_actionBarTabLine, key_actionBarDefaultTitle);
        fallbackKeys.put(key_actionBarTabSelector, key_actionBarDefaultSelector);
        fallbackKeys.put(key_profile_status, key_avatar_subtitleInProfileBlue);
        fallbackKeys.put(key_chats_menuTopBackgroundCats, key_avatar_backgroundActionBarBlue);
        fallbackKeys.put(key_chat_outLinkSelectBackground, key_chat_linkSelectBackground);
        fallbackKeys.put(key_actionBarDefaultSubmenuSeparator, key_windowBackgroundGray);
        fallbackKeys.put(key_chat_attachPermissionImage, key_dialogTextBlack);
        fallbackKeys.put(key_chat_attachPermissionMark, key_chat_sentError);
        fallbackKeys.put(key_chat_attachPermissionText, key_dialogTextBlack);
        fallbackKeys.put(key_chat_attachEmptyImage, key_emptyListPlaceholder);
        fallbackKeys.put(key_actionBarBrowser, key_actionBarDefault);
        fallbackKeys.put(key_chats_sentReadCheck, key_chats_sentCheck);
        fallbackKeys.put(key_chat_outSentCheckRead, key_chat_outSentCheck);
        fallbackKeys.put(key_chat_outSentCheckReadSelected, key_chat_outSentCheckSelected);
        fallbackKeys.put(key_chats_archivePullDownBackground, key_chats_unreadCounterMuted);
        fallbackKeys.put(key_chats_archivePullDownBackgroundActive, key_chats_actionBackground);
        fallbackKeys.put(key_avatar_backgroundArchivedHidden, key_avatar_backgroundSaved);
        fallbackKeys.put(key_featuredStickers_removeButtonText, key_featuredStickers_addButtonPressed);
        fallbackKeys.put(key_dialogEmptyImage, key_player_time);
        fallbackKeys.put(key_dialogEmptyText, key_player_time);
        fallbackKeys.put(key_location_actionIcon, key_dialogTextBlack);
        fallbackKeys.put(key_location_actionActiveIcon, key_windowBackgroundWhiteBlueText7);
        fallbackKeys.put(key_location_actionBackground, key_dialogBackground);
        fallbackKeys.put(key_location_actionPressedBackground, key_dialogBackgroundGray);
        fallbackKeys.put(key_location_sendLocationText, key_windowBackgroundWhiteBlueText7);
        fallbackKeys.put(key_location_sendLiveLocationText, key_windowBackgroundWhiteGreenText);
        fallbackKeys.put(key_chat_outTextSelectionHighlight, key_chat_textSelectBackground);
        fallbackKeys.put(key_chat_inTextSelectionHighlight, key_chat_textSelectBackground);
        fallbackKeys.put(key_chat_TextSelectionCursor, key_chat_messagePanelCursor);
        fallbackKeys.put(key_chat_outTextSelectionCursor, key_chat_TextSelectionCursor);
        fallbackKeys.put(key_chat_inPollCorrectAnswer, key_chat_attachLocationBackground);
        fallbackKeys.put(key_chat_outPollCorrectAnswer, key_chat_attachLocationBackground);
        fallbackKeys.put(key_chat_inPollWrongAnswer, key_chat_attachAudioBackground);
        fallbackKeys.put(key_chat_outPollWrongAnswer, key_chat_attachAudioBackground);
        fallbackKeys.put(key_chat_editMediaButton, key_dialogFloatingButton);
        fallbackKeys.put(key_chat_attachCheckBoxBackground, key_dialogRoundCheckBox);
        fallbackKeys.put(key_pollCreateIcons, key_windowBackgroundWhiteGrayIcon);

        fallbackKeys.put(key_profile_tabText, key_windowBackgroundWhiteGrayText);
        fallbackKeys.put(key_profile_tabSelectedText, key_windowBackgroundWhiteBlueHeader);
        fallbackKeys.put(key_profile_tabSelectedLine, key_windowBackgroundWhiteBlueHeader);
        fallbackKeys.put(key_profile_tabSelector, key_listSelector);

        fallbackKeys.put(key_chat_attachContactText, key_chat_attachContactBackground);

        fallbackKeys.put(key_chat_inPsaNameText, key_avatar_nameInMessageGreen);
        fallbackKeys.put(key_chat_outPsaNameText, key_avatar_nameInMessageGreen);

        fallbackKeys.put(key_chat_outAdminText, key_chat_outTimeText);
        fallbackKeys.put(key_chat_outAdminSelectedText, key_chat_outTimeSelectedText);

        fallbackKeys.put(key_returnToCallMutedBackground, key_windowBackgroundWhite);
        fallbackKeys.put(key_dialogSwipeRemove, key_avatar_backgroundRed);

        fallbackKeys.put(key_chat_inReactionButtonBackground, key_chat_inLoader);
        fallbackKeys.put(key_chat_outReactionButtonBackground, key_chat_outLoader);
        fallbackKeys.put(key_chat_reactionServiceButtonBackgroundSelected, key_chat_outBubble);
        fallbackKeys.put(key_chat_reactionServiceButtonTextSelected, key_chat_messageTextOut);
        fallbackKeys.put(key_chat_inReactionButtonText, key_chat_inPreviewInstantText);
        fallbackKeys.put(key_chat_outReactionButtonText, key_chat_outPreviewInstantText);
        fallbackKeys.put(key_chat_inReactionButtonTextSelected, key_windowBackgroundWhite);
        fallbackKeys.put(key_chat_outReactionButtonTextSelected, key_windowBackgroundWhite);
        fallbackKeys.put(key_dialogReactionMentionBackground, key_voipgroup_mutedByAdminGradient2);
        fallbackKeys.put(key_topics_unreadCounter, key_chats_unreadCounter);
        fallbackKeys.put(key_topics_unreadCounterMuted, key_chats_message);

        fallbackKeys.put(key_avatar_background2Saved, key_avatar_backgroundSaved);
        fallbackKeys.put(key_avatar_background2Red, key_avatar_backgroundRed);
        fallbackKeys.put(key_avatar_background2Orange, key_avatar_backgroundOrange);
        fallbackKeys.put(key_avatar_background2Violet, key_avatar_backgroundViolet);
        fallbackKeys.put(key_avatar_background2Green, key_avatar_backgroundGreen);
        fallbackKeys.put(key_avatar_background2Cyan, key_avatar_backgroundCyan);
        fallbackKeys.put(key_avatar_background2Blue, key_avatar_backgroundBlue);
        fallbackKeys.put(key_avatar_background2Pink, key_avatar_backgroundPink);
        fallbackKeys.put(key_chats_date_bold, key_chats_date);

        fallbackKeys.put(key_statisticChartLine_orange, key_color_orange);
        fallbackKeys.put(key_statisticChartLine_blue, key_color_blue);
        fallbackKeys.put(key_statisticChartLine_red, key_color_red);
        fallbackKeys.put(key_statisticChartLine_lightblue, key_color_lightblue);
        fallbackKeys.put(key_statisticChartLine_golden, key_color_yellow);
        fallbackKeys.put(key_statisticChartLine_purple, key_color_purple);
        fallbackKeys.put(key_statisticChartLine_indigo, key_color_purple);
        fallbackKeys.put(key_statisticChartLine_cyan, key_color_cyan);

        fallbackKeys.put(key_actionBarActionModeReaction, key_windowBackgroundGray);
        fallbackKeys.put(key_actionBarActionModeReactionText, key_chat_inReactionButtonText);

        fallbackKeys.put(key_table_background, key_graySection);
        fallbackKeys.put(key_table_border, key_divider);
        fallbackKeys.put(key_chat_inTableBackground, key_table_background);
        fallbackKeys.put(key_chat_outTableBackground, key_chat_outCodeBackground);
        fallbackKeys.put(key_chat_inTableBorder, key_table_border);
        fallbackKeys.put(key_chat_outTableBorder, key_table_border);
        fallbackKeys.put(key_chat_outArticleCodeBackground, key_chat_outCodeBackground);

        fallbackKeys.put(key_share_icon, key_windowBackgroundWhiteBlackText);
        fallbackKeys.put(key_share_linkBackground, key_windowBackgroundGray);
        fallbackKeys.put(key_share_linkText, key_windowBackgroundWhiteBlackText);
        fallbackKeys.put(key_glass_defaultIcon, Theme.key_chat_messagePanelIcons);
        fallbackKeys.put(key_glass_defaultText, Theme.key_chat_messagePanelText);
        fallbackKeys.put(key_glass_targetMainTabs, Theme.key_dialogBackground);
        fallbackKeys.put(key_glass_targetMainTopPanel, Theme.key_dialogBackground);
        fallbackKeys.put(key_glass_tabSelected, Theme.key_chat_messagePanelSend);
        fallbackKeys.put(key_glass_tabSelectedText, Theme.key_chat_messagePanelSend);
        fallbackKeys.put(key_glass_tabUnselected, Theme.key_windowBackgroundWhiteBlackText);
        fallbackKeys.put(key_actionBarDefaultTitle, key_windowBackgroundWhiteBlackText);
        fallbackKeys.put(key_telegram_color_dialogsLogo, key_windowBackgroundWhiteBlackText);
        fallbackKeys.put(key_profile_title, key_windowBackgroundWhiteBlackText);
        fallbackKeys.put(key_telegram_color, Theme.key_chat_messagePanelSend);
        fallbackKeys.put(key_telegram_color_text, Theme.key_windowBackgroundWhiteBlueText4);

        fallbackKeys.put(key_wallet_comment, Theme.key_dialogBackgroundGray);
        fallbackKeys.put(key_wallet_out_commentBorder, Theme.key_divider);
        fallbackKeys.put(key_wallet_inputCurrency, Theme.key_color_green);


        // fallbackKeys.put(key_chat_msgIvButtonDefaultIn, );
        // fallbackKeys.put(key_chat_msgIvButtonDefaultInPressed, );
        fallbackKeys.put(key_chat_msgIvButtonDefaultInText, key_chat_messageTextIn);

        fallbackKeys.put(key_chat_msgIvButtonPrimaryIn, key_featuredStickers_addButton);
        fallbackKeys.put(key_chat_msgIvButtonPrimaryInPressed, key_featuredStickers_addButtonPressed);
        fallbackKeys.put(key_chat_msgIvButtonPrimaryInText,  key_featuredStickers_buttonText);

        // fallbackKeys.put(key_chat_msgIvButtonDangerIn, );
        // fallbackKeys.put(key_chat_msgIvButtonDangerInPressed, );
        fallbackKeys.put(key_chat_msgIvButtonDangerInText, key_avatar_nameInMessageRed);

        // fallbackKeys.put(key_chat_msgIvButtonSuccessIn, );
        // fallbackKeys.put(key_chat_msgIvButtonSuccessInPressed,  );
        fallbackKeys.put(key_chat_msgIvButtonSuccessInText, key_avatar_nameInMessageGreen );

        //fallbackKeys.put(key_chat_msgIvButtonDefaultInlineIn, );
        //fallbackKeys.put(key_chat_msgIvButtonDefaultInlineInPressed,  );
        fallbackKeys.put(key_chat_msgIvButtonDefaultInlineInText, key_telegram_color_text);



        // fallbackKeys.put(key_chat_msgIvButtonDefaultOut,  );
        // fallbackKeys.put(key_chat_msgIvButtonDefaultOutPressed,  );
        fallbackKeys.put(key_chat_msgIvButtonDefaultOutText, key_chat_messageTextOut);

        fallbackKeys.put(key_chat_msgIvButtonPrimaryOut, key_featuredStickers_addButton);
        fallbackKeys.put(key_chat_msgIvButtonPrimaryOutPressed, key_featuredStickers_addButtonPressed);
        fallbackKeys.put(key_chat_msgIvButtonPrimaryOutText, key_featuredStickers_buttonText);

        // fallbackKeys.put(key_chat_msgIvButtonDangerOut, );
        // fallbackKeys.put(key_chat_msgIvButtonDangerOutPressed, );
        fallbackKeys.put(key_chat_msgIvButtonDangerOutText, key_avatar_nameInMessageRed );

        // fallbackKeys.put(key_chat_msgIvButtonSuccessOut,  );
        // fallbackKeys.put(key_chat_msgIvButtonSuccessOutPressed, );
        fallbackKeys.put(key_chat_msgIvButtonSuccessOutText, key_avatar_nameInMessageGreen );

        // fallbackKeys.put(key_chat_msgIvButtonDefaultInlineOut, );
        // fallbackKeys.put(key_chat_msgIvButtonDefaultInlineOutPressed, );
        fallbackKeys.put(key_chat_msgIvButtonDefaultInlineOutText, key_telegram_color_text);



        for (int i = 0; i < keys_avatar_background.length; i++) {
            themeAccentExclusionKeys.add(keys_avatar_background[i]);
        }
        for (int i = 0; i < keys_avatar_background2.length; i++) {
            themeAccentExclusionKeys.add(keys_avatar_background2[i]);
        }
        for (int i = 0; i < keys_avatar_nameInMessage.length; i++) {
            themeAccentExclusionKeys.add(keys_avatar_nameInMessage[i]);
        }
        for (int i = 0; i < keys_colors.length; i++) {
            themeAccentExclusionKeys.add(keys_colors[i]);
        }

        themeAccentExclusionKeys.add(key_chat_attachGalleryBackground);
        themeAccentExclusionKeys.add(key_statisticChartLine_blue);
        themeAccentExclusionKeys.add(key_statisticChartLine_green);
        themeAccentExclusionKeys.add(key_statisticChartLine_red);
        themeAccentExclusionKeys.add(key_statisticChartLine_golden);
        themeAccentExclusionKeys.add(key_statisticChartLine_lightblue);
        themeAccentExclusionKeys.add(key_statisticChartLine_lightgreen);
        themeAccentExclusionKeys.add(key_statisticChartLine_orange);
        themeAccentExclusionKeys.add(key_statisticChartLine_indigo);
        themeAccentExclusionKeys.add(key_chat_inCodeBackground);
        themeAccentExclusionKeys.add(key_chat_inTableBackground);
        themeAccentExclusionKeys.add(key_chat_outTableBackground);
        themeAccentExclusionKeys.add(key_chat_inTableBorder);
        themeAccentExclusionKeys.add(key_chat_outTableBorder);
        themeAccentExclusionKeys.add(key_chat_inDivider);
        themeAccentExclusionKeys.add(key_chat_inArticleCodeBackground);
        themeAccentExclusionKeys.add(key_chat_outArticleCodeBackground);
        themeAccentExclusionKeys.add(key_chat_inArticleCodeScrollbarBackground);
        themeAccentExclusionKeys.add(key_chat_inArticleCodeScrollbar);
        themeAccentExclusionKeys.add(key_chat_outArticleCodeScrollbarBackground);
        themeAccentExclusionKeys.add(key_chat_outArticleCodeScrollbar);
        themeAccentExclusionKeys.add(key_chat_inArticleDetailsArrow);
        themeAccentExclusionKeys.add(key_chat_outArticleDetailsArrow);
        themeAccentExclusionKeys.add(key_chat_inArticleDetailsLine);
        themeAccentExclusionKeys.add(key_chat_outArticleDetailsLine);

        themeAccentExclusionKeys.add(key_voipgroup_checkMenu);
        themeAccentExclusionKeys.add(key_voipgroup_muteButton);
        themeAccentExclusionKeys.add(key_voipgroup_muteButton2);
        themeAccentExclusionKeys.add(key_voipgroup_muteButton3);
        themeAccentExclusionKeys.add(key_voipgroup_searchText);
        themeAccentExclusionKeys.add(key_voipgroup_searchPlaceholder);
        themeAccentExclusionKeys.add(key_voipgroup_searchBackground);
        themeAccentExclusionKeys.add(key_voipgroup_leaveCallMenu);
        themeAccentExclusionKeys.add(key_voipgroup_scrollUp);
        themeAccentExclusionKeys.add(key_voipgroup_soundButton);
        themeAccentExclusionKeys.add(key_voipgroup_soundButtonActive);
        themeAccentExclusionKeys.add(key_voipgroup_soundButtonActiveScrolled);
        themeAccentExclusionKeys.add(key_voipgroup_soundButton2);
        themeAccentExclusionKeys.add(key_voipgroup_soundButtonActive2);
        themeAccentExclusionKeys.add(key_voipgroup_soundButtonActive2Scrolled);
        themeAccentExclusionKeys.add(key_voipgroup_leaveButton);
        themeAccentExclusionKeys.add(key_voipgroup_leaveButtonScrolled);
        themeAccentExclusionKeys.add(key_voipgroup_connectingProgress);
        themeAccentExclusionKeys.add(key_voipgroup_disabledButton);
        themeAccentExclusionKeys.add(key_voipgroup_rtmpButton);
        themeAccentExclusionKeys.add(key_voipgroup_disabledButtonActive);
        themeAccentExclusionKeys.add(key_voipgroup_disabledButtonActiveScrolled);
        themeAccentExclusionKeys.add(key_voipgroup_unmuteButton);
        themeAccentExclusionKeys.add(key_voipgroup_unmuteButton2);
        themeAccentExclusionKeys.add(key_voipgroup_actionBarUnscrolled);
        themeAccentExclusionKeys.add(key_voipgroup_listViewBackgroundUnscrolled);
        themeAccentExclusionKeys.add(key_voipgroup_lastSeenTextUnscrolled);
        themeAccentExclusionKeys.add(key_voipgroup_mutedIconUnscrolled);
        themeAccentExclusionKeys.add(key_voipgroup_actionBar);
        themeAccentExclusionKeys.add(key_voipgroup_actionBarItems);
        themeAccentExclusionKeys.add(key_voipgroup_actionBarItemsSelector);
        themeAccentExclusionKeys.add(key_voipgroup_mutedByAdminIcon);
        themeAccentExclusionKeys.add(key_voipgroup_mutedIcon);
        themeAccentExclusionKeys.add(key_voipgroup_lastSeenText);
        themeAccentExclusionKeys.add(key_voipgroup_nameText);
        themeAccentExclusionKeys.add(key_voipgroup_listViewBackground);
        themeAccentExclusionKeys.add(key_voipgroup_listeningText);
        themeAccentExclusionKeys.add(key_voipgroup_speakingText);
        themeAccentExclusionKeys.add(key_voipgroup_listSelector);
        themeAccentExclusionKeys.add(key_voipgroup_inviteMembersBackground);
        themeAccentExclusionKeys.add(key_voipgroup_dialogBackground);
        themeAccentExclusionKeys.add(key_voipgroup_overlayGreen1);
        themeAccentExclusionKeys.add(key_voipgroup_overlayGreen2);
        themeAccentExclusionKeys.add(key_voipgroup_overlayBlue1);
        themeAccentExclusionKeys.add(key_voipgroup_overlayBlue2);
        themeAccentExclusionKeys.add(key_voipgroup_topPanelGreen1);
        themeAccentExclusionKeys.add(key_voipgroup_topPanelGreen2);
        themeAccentExclusionKeys.add(key_voipgroup_topPanelBlue1);
        themeAccentExclusionKeys.add(key_voipgroup_topPanelBlue2);
        themeAccentExclusionKeys.add(key_voipgroup_topPanelGray);
        themeAccentExclusionKeys.add(key_voipgroup_overlayAlertGradientMuted);
        themeAccentExclusionKeys.add(key_voipgroup_overlayAlertGradientMuted2);
        themeAccentExclusionKeys.add(key_voipgroup_overlayAlertGradientUnmuted);
        themeAccentExclusionKeys.add(key_voipgroup_overlayAlertGradientUnmuted2);
        themeAccentExclusionKeys.add(key_voipgroup_overlayAlertMutedByAdmin);
        themeAccentExclusionKeys.add(key_voipgroup_overlayAlertMutedByAdmin2);
        themeAccentExclusionKeys.add(key_voipgroup_mutedByAdminGradient);
        themeAccentExclusionKeys.add(key_voipgroup_mutedByAdminGradient2);
        themeAccentExclusionKeys.add(key_voipgroup_mutedByAdminGradient3);
        themeAccentExclusionKeys.add(key_voipgroup_mutedByAdminMuteButton);
        themeAccentExclusionKeys.add(key_voipgroup_mutedByAdminMuteButtonDisabled);
        themeAccentExclusionKeys.add(key_voipgroup_windowBackgroundWhiteInputField);
        themeAccentExclusionKeys.add(key_voipgroup_windowBackgroundWhiteInputFieldActivated);
        themeAccentExclusionKeys.add(key_premiumGradient0);
        themeAccentExclusionKeys.add(key_premiumGradient1);
        themeAccentExclusionKeys.add(key_premiumGradient2);
        themeAccentExclusionKeys.add(key_premiumGradient3);
        themeAccentExclusionKeys.add(key_premiumGradient4);
        themeAccentExclusionKeys.add(key_premiumGradientBackground1);
        themeAccentExclusionKeys.add(key_premiumGradientBackground2);
        themeAccentExclusionKeys.add(key_premiumGradientBackground3);
        themeAccentExclusionKeys.add(key_premiumGradientBackground4);
        themeAccentExclusionKeys.add(key_premiumStartSmallStarsColor);
        themeAccentExclusionKeys.add(key_premiumStarGradient1);
        themeAccentExclusionKeys.add(key_premiumStarGradient2);
        themeAccentExclusionKeys.add(key_stories_circle1);
        themeAccentExclusionKeys.add(key_stories_circle2);
        themeAccentExclusionKeys.add(key_stories_circle_dialog1);
        themeAccentExclusionKeys.add(key_stories_circle_dialog2);
        themeAccentExclusionKeys.add(key_stories_circle_closeFriends1);
        themeAccentExclusionKeys.add(key_stories_circle_closeFriends2);


        themes = new ArrayList<>();
        otherThemes = new ArrayList<>();
        themesDict = new HashMap<>();
        currentColorsNoAccent = new SparseIntArray();
        currentColors = new SparseIntArray();

        SharedPreferences themeConfig = ApplicationLoader.applicationContext.getSharedPreferences("themeconfig", Activity.MODE_PRIVATE);

        ThemeInfo themeInfo = new ThemeInfo();
        themeInfo.name = "Blue";
        themeInfo.assetName = "bluebubbles.attheme";
        themeInfo.previewBackgroundColor = 0xff95beec;
        themeInfo.previewInColor = 0xffffffff;
        themeInfo.previewOutColor = 0xffd0e6ff;
        themeInfo.firstAccentIsDefault = true;
        themeInfo.currentAccentId = DEFALT_THEME_ACCENT_ID;
        themeInfo.sortIndex = 1;
        themeInfo.setAccentColorOptions(
                new int[]    { 0xFF5890C5,                     0xFF239853,                    0xFFCE5E82,                    0xFF7F63C3,                    0xFF2491AD,                    0xFF299C2F,                    0xFF8854B4,                    0xFF328ACF,                    0xFF43ACC7,                    0xFF52AC44,                    0xFFCD5F93,                    0xFFD28036,                    0xFF8366CC,                    0xFFCE4E57,                    0xFFD3AE40,                    0xFF7B88AB },
                new int[]    { 0xFFB8E18D,                     0xFFFAFBCC,                    0xFFFFF9DC,                    0xFFC14F6E,                    0xFFD1BD1B,                    0xFFFFFAC9,                    0xFFFCF6D8,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000 },
                new int[]    { 0x00000000,                     0xFFF2FBC9,                    0xFFFBF4DF, 	                         0,	                             0,                    0xFFFDEDB4,                    0xFFFCF7B6,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000 },
                new int[]    { 0x00000000,                     0xFFdfe2a0,                    0xFFf1b290,                    0xFFd7c1e9,                    0xFFd7b89e,                    0xFFec9e73,                    0xFFcbb0e4,                    0xff9bbce7,                    0xff91c5ec,                    0xff9bc982,                    0xffe4a1c1,                    0xffe3ae7d,                    0xffb8aaea,                    0xffeb9c79,                    0xffd3bc74,                    0xffa0aace },
                new int[]    { 0x00000000,                     0xFFbad89d,                    0xFFeccf94,                    0xFFe8bdd6,                    0xFFe6dec2,                    0xFFe8d085,                    0xFFebc8e9,                    0xffc0d9f3,                    0xffbfdfec,                    0xffe0dd93,                    0xffe9bed6,                    0xffecd5a2,                    0xffc5c9ee,                    0xfff0bd99,                    0xffe9df9e,                    0xffcacedd },
                new int[]    { 0x00000000,                     0xFFe2dea7,                    0xFFe7b384,                    0xFFd2aee9,                    0xFFdac5ae,                    0xFFeea677,                    0xFFdfa8d1,                    0xff95c3eb,                    0xffb5e1d9,                    0xffbed595,                    0xffcca8e1,                    0xffdfb076,                    0xffb3b1e2,                    0xffe79db4,                    0xffe0c88b,                    0xffa6add2 },
                new int[]    { 0x00000000,                     0xFF9ec790,                    0xFFebdea8,                    0xFFeccb88,                    0xFFe5dcbf,                    0xFFede4a9,                    0xFFedc8a8,                    0xffbbd5e8,                    0xffbfdbe8,                    0xffd1db97,                    0xffefcbd7,                    0xffecd694,                    0xffdfbeed,                    0xfff3b182,                    0xffe5d397,                    0xffcacee8 },
                new int[]    {         99,                              9,                            10,                            11,                            12,                            13,                            14,                             0,                             1,                             2,                             3,                             4,                             5,                             6,                             7,                             8 },
                new String[] {         "",  "p-pXcflrmFIBAAAAvXYQk-mCwZU", "JqSUrO0-mFIBAAAAWwTvLzoWGQI", "O-wmAfBPSFADAAAA4zINVfD_bro", "RepJ5uE_SVABAAAAr4d0YhgB850", "-Xc-np9y2VMCAAAARKr0yNNPYW0", "fqv01SQemVIBAAAApND8LDRUhRU", "fqv01SQemVIBAAAApND8LDRUhRU", "RepJ5uE_SVABAAAAr4d0YhgB850", "lp0prF8ISFAEAAAA_p385_CvG0w", "heptcj-hSVACAAAAC9RrMzOa-cs", "PllZ-bf_SFAEAAAA8crRfwZiDNg", "dhf9pceaQVACAAAAbzdVo4SCiZA", "Ujx2TFcJSVACAAAARJ4vLa50MkM", "p-pXcflrmFIBAAAAvXYQk-mCwZU", "dk_wwlghOFACAAAAfz9xrxi6euw" },
                new int[]    {          0,                            180,                            45,                             0,                            45,                           180,                             0,                             0,                             0,                             0,                             0,                             0,                             0,                             0,                             0,                             0 },
                new int[]    {          0,                             52,                            46,                            57,                            45,                            64,                            52,                            35,                            36,                            41,                            50,                            50,                            35,                            38,                            37,                            30 }
                );
        sortAccents(themeInfo);
        themes.add(currentTheme = currentDayTheme = defaultTheme = themeInfo); // EVE-PATCH: initialize currentTheme in static block to prevent NPE
        themesDict.put("Blue", themeInfo);

        themeInfo = new ThemeInfo();
        themeInfo.name = "Dark Blue";
        themeInfo.assetName = "darkblue.attheme";
        themeInfo.previewBackgroundColor = 0xff5f6e82;
        themeInfo.previewInColor = 0xff76869c;
        themeInfo.previewOutColor = 0xff82a8e3;
        themeInfo.sortIndex = 3;
        themeInfo.setAccentColorOptions(
                new int[]    {                    0xFF927BD4,                    0xFF698AFB,                    0xFF23A7F0,                    0xFF7B71D1,                    0xFF69B955,                    0xFF2990EA,                    0xFF7082E9,                    0xFF66BAED,                    0xff3685fa,                    0xff46c8ed,                    0xff64AC5F,                    0xffeb7cb1,                    0xffee902a,                    0xffa281f0,                    0xffd34324,                    0xffeebd34,                    0xff7f8fab,                    0xff3581e3 },
                new int[]    {                    0xFF9D5C99,                    0xFF635545,                    0xFF31818B,                    0xFFAD6426,                    0xFF4A7034,                    0xFF335D82,                    0xFF36576F,                    0xFF597563,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000 },
                new int[]    {                    0xFF604DA8,                    0xFF685D4C,                    0xFF1B6080,                    0xFF99354E,                    0xFF275D3B,                    0xFF317A98,                    0xFF376E87,                    0xFF5E7370,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000 },
                new int[]    {                    0xFF271e2e,                    0xFF171a22,                    0xFF071e1f,                    0xFF100f13,                    0xFF0e1811,                    0xFF0b131c,                    0xFF1d2129,                    0xFF202c2a,                    0xff0e141a,                    0xff162325,                    0xff161d15,                    0xff24191e,                    0xff251b13,                    0xff1f1d29,                    0xff22160e,                    0xff272115,                    0xff171a1b,                    0xff0e141a },
                new int[]    {                    0xFF110e13,                    0xFF26262e,                    0xFF141d26,                    0xFF221a27,                    0xFF1f2818,                    0xFF192330,                    0xFF12161a,                    0xFF141a1e,                    0xff172431,                    0xff0e1718,                    0xff172719,                    0xff23171c,                    0xff201408,                    0xff14131c,                    0xff2d1d16,                    0xff1a160d,                    0xff212328,                    0xff172431 },
                new int[]    {                    0xFF2b1e2b,                    0xFF15151b,                    0xFF0c151a,                    0xFF0e0f13,                    0xFF0b170f,                    0xFF131822,                    0xFF17242d,                    0xFF16202b,                    0xff0f171e,                    0xff1e2e2e,                    0xff141e14,                    0xff2b1929,                    0xff2e1f15,                    0xff292331,                    0xff23140c,                    0xff292414,                    0xff181a1d,                    0xff0f171e },
                new int[]    {                    0xFF161227,                    0xFF1a1916,                    0xFF0d272c,                    0xFF271d29,                    0xFF171d19,                    0xFF172331,                    0xFF111521,                    0xFF051717,                    0xff141c2b,                    0xff121f1f,                    0xff1c261a,                    0xff1f141d,                    0xff1b130a,                    0xff17131b,                    0xff2d1924,                    0xff1e170e,                    0xff212228,                    0xff141c2b },
                new int[]    {                            11,                            12,                            13,                            14,                            15,                            16,                            17,                            18,                             0,                             1,                             2,                             3,                             4,                             5,                             6,                             7,                             8,                             9 },
                new String[] { "O-wmAfBPSFADAAAA4zINVfD_bro", "RepJ5uE_SVABAAAAr4d0YhgB850", "dk_wwlghOFACAAAAfz9xrxi6euw", "9LW_RcoOSVACAAAAFTk3DTyXN-M", "PllZ-bf_SFAEAAAA8crRfwZiDNg", "-Xc-np9y2VMCAAAARKr0yNNPYW0", "kO4jyq55SFABAAAA0WEpcLfahXk", "CJNyxPMgSVAEAAAAvW9sMwc51cw", "fqv01SQemVIBAAAApND8LDRUhRU", "RepJ5uE_SVABAAAAr4d0YhgB850", "CJNyxPMgSVAEAAAAvW9sMwc51cw", "9LW_RcoOSVACAAAAFTk3DTyXN-M", "9GcNVISdSVADAAAAUcw5BYjELW4", "F5oWoCs7QFACAAAAgf2bD_mg8Bw", "9ShF73d1MFIIAAAAjWnm8_ZMe8Q", "3rX-PaKbSFACAAAAEiHNvcEm6X4", "dk_wwlghOFACAAAAfz9xrxi6euw", "fqv01SQemVIBAAAApND8LDRUhRU" },
                new int[]    {                           225,                            45,                           225,                           135,                            45,                           225,                            45,                             0,                             0,                             0,                             0,                             0,                             0,                             0,                             0,                             0,                             0,                             0 },
                new int[]    {                            40,                            40,                            31,                            50,                            25,                            34,                            35,                            35,                            38,                            29,                            24,                            34,                            34,                            31,                            29,                            37,                            21,                            38 }
                );
        sortAccents(themeInfo);
        themes.add(themeInfo);
        themesDict.put("Dark Blue", currentNightTheme = themeInfo);

        themeInfo = new ThemeInfo();
        themeInfo.name = "Arctic Blue";
        themeInfo.assetName = "arctic.attheme";
        themeInfo.previewBackgroundColor = 0xffe1e9f0;
        themeInfo.previewInColor = 0xffffffff;
        themeInfo.previewOutColor = 0xff6ca1eb;
        themeInfo.sortIndex = 5;
        themeInfo.setAccentColorOptions(
                new int[]    {                    0xFF40B1E2,                    0xFF41B05D,                    0xFFCE8C20,                    0xFF57A3EB,                    0xFFDE8534,                    0xFFCC6189,                    0xFF3490EB,                    0xFF43ACC7,                    0xFF52AC44,                    0xFFCD5F93,                    0xFFD28036,                    0xFF8366CC,                    0xFFCE4E57,                    0xFFD3AE40,                    0xFF7B88AB },
                new int[]    {                    0xFF319FCA,                    0xFF28A359,                    0xFF8C5A3F,                    0xFF3085D3,                    0xFFC95870,                    0xFF7871CD,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000 },
                new int[]    {                    0xFF4EBEE2,                    0xFF6BBC59,                    0xFF9E563C,                    0xFF48C2D8,                    0xFFD87047,                    0xFFBE6EAF,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000 },
                new int[]    {                    0xFFc5e2f0,                    0xFFdadea9,                    0xFFd6c9a5,                    0xFFe3f3f3,                    0xFFeee5b0,                    0xFFe5dfec,                    0xffe0e7ed,                    0xffbfe0eb,                    0xffc2e0af,                    0xffefd9e4,                    0xfff1dfbd,                    0xffe1dbec,                    0xffedd8d8,                    0xffebe1cd,                    0xffdcdee5 },
                new int[]    {                    0xFFe8f4f3,                    0xFFbce3ac,                    0xFFe6dbaf,                    0xFFc8e6ee,                    0xFFeebeaa,                    0xFFe1c6ec,                    0xffbed7f3,                    0xffbfe0eb,                    0xffcbe19a,                    0xffecc6d9,                    0xffe8c79b,                    0xffbdc1ec,                    0xffeecac0,                    0xffebe2b5,                    0xffc3cadf },
                new int[]    {                    0xFFb4daf0,                    0xFFcde7a9,                    0xFFe8c091,                    0xFFd9eff3,                    0xFFeecf92,                    0xFFf6eaf6,                    0xffe0e8f3,                    0xffcaebec,                    0xffb8de89,                    0xfff1d8e6,                    0xfff3d7a6,                    0xffd6d8f5,                    0xffedddcd,                    0xffebdcc9,                    0xffe7edf1 },
                new int[]    {                    0xFFcff0ef,                    0xFFa8cf9b,                    0xFFe1d09f,                    0xFFb4d6e8,                    0xFFeeaf87,                    0xFFe5c5cf,                    0xffc8dbf3,                    0xffaedceb,                    0xffcee5a2,                    0xfff0c0d9,                    0xffdfb48e,                    0xffbdbaf2,                    0xfff1c9bb,                    0xffe7d7ae,                    0xffc5c6da },
                new int[]    {                             9,                            10,                            11,                            12,                            13,                            14,                             0,                             1,                             2,                             3,                             4,                             5,                             6,                             7,                             8 },
                new String[] { "MIo6r0qGSFAFAAAAtL8TsDzNX60", "dhf9pceaQVACAAAAbzdVo4SCiZA", "fqv01SQemVIBAAAApND8LDRUhRU", "p-pXcflrmFIBAAAAvXYQk-mCwZU", "JqSUrO0-mFIBAAAAWwTvLzoWGQI", "F5oWoCs7QFACAAAAgf2bD_mg8Bw", "fqv01SQemVIBAAAApND8LDRUhRU", "RepJ5uE_SVABAAAAr4d0YhgB850", "PllZ-bf_SFAEAAAA8crRfwZiDNg", "pgJfpFNRSFABAAAACDT8s5sEjfc", "ptuUd96JSFACAAAATobI23sPpz0", "dhf9pceaQVACAAAAbzdVo4SCiZA", "JqSUrO0-mFIBAAAAWwTvLzoWGQI", "9iklpvIPQVABAAAAORQXKur_Eyc", "F5oWoCs7QFACAAAAgf2bD_mg8Bw" },
                new int[]    {                           315,                           315,                           225,                           315,                             0,                           180,                             0,                             0,                             0,                             0,                             0,                             0,                             0,                             0,                             0 },
                new int[]    {                            50,                            50,                            58,                            47,                            46,                            50,                            49,                            46,                            51,                            50,                            49,                            34,                            54,                            50,                            40 }
                );
        sortAccents(themeInfo);
        themes.add(themeInfo);
        themesDict.put("Arctic Blue", themeInfo);

        themeInfo = new ThemeInfo();
        themeInfo.name = "Day";
        themeInfo.assetName = "day.attheme";
        themeInfo.previewBackgroundColor = 0xffffffff;
        themeInfo.previewInColor = 0xffebeef4;
        themeInfo.previewOutColor = 0xff7cb2fe;
        themeInfo.sortIndex = 2;
        themeInfo.setAccentColorOptions(
                new int[]    { 0xFF56A2C9, 0xFFCC6E83, 0xFFD08E47, 0xFFCC6462, 0xFF867CD2, 0xFF4C91DF, 0xFF57B4D9, 0xFF54B169, 0xFFD9BF3F, 0xFFCC6462, 0xFFCC6E83, 0xFF9B7BD2, 0xFFD79144, 0xFF7B88AB },
                new int[]    { 0xFF6580DC, 0xFF6C6DD2, 0xFFCB5481, 0xFFC34A4A, 0xFF5C8EDF, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000 },
                new int[]    { 0xFF3EC1D6, 0xFFC86994, 0xFFDBA12F, 0xFFD08E3B, 0xFF51B5CB, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000 },
                new int[]    { 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000 },
                new int[]    { 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000 },
                null,
                null,
                new int[]    {          9,         10,         11,         12,         13,          0,          1,          2,          3,          4,          5,          6,          7,          8 },
                new String[] {         "",         "",         "",         "",         "",         "",         "",         "",         "",         "",         "",         "",         "",         "" },
                new int[]    {          0,          0,          0,          0,          0,          0,          0,          0,          0,          0,          0,          0,          0,          0 },
                new int[]    {          0,          0,          0,          0,          0,          0,          0,          0,          0,          0,          0,          0,          0,          0 }
                );
        sortAccents(themeInfo);
        themes.add(themeInfo);
        themesDict.put("Day", themeInfo);

        themeInfo = new ThemeInfo();
        themeInfo.name = "Night";
        themeInfo.assetName = "night.attheme";
        themeInfo.previewBackgroundColor = 0xff535659;
        themeInfo.previewInColor = 0xff747A84;
        themeInfo.previewOutColor = 0xff75A2E6;
        themeInfo.sortIndex = 4;
        themeInfo.setAccentColorOptions(
                new int[]    {                    0xFF6ABE3F,                    0xFF8D78E3,                    0xFFDE5E7E,                    0xFF5977E8,                    0xFFDBC11A,                    0xff3e88f7,                    0xff4ab5d3,                    0xff4ab841,                    0xffd95576,                    0xffe27d2b,                    0xff936cda,                    0xffd04336,                    0xffe8ae1c,                    0xff7988a3 },
                new int[]    {                    0xFF8A5294,                    0xFFB46C1B,                    0xFFAF4F6F,                    0xFF266E8D,                    0xFF744EB7,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000 },
                new int[]    {                    0xFF6855BB,                    0xFFA53B4A,                    0xFF62499C,                    0xFF2F919D,                    0xFF298B95,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000,                    0x00000000 },
                new int[]    {                    0xFF16131c,                    0xFF1e1118,                    0xFF0f0b10,                    0xFF090c0c,                    0xFF071519,                    0xff0d0e17,                    0xff111b1c,                    0xff0c110c,                    0xff0e0b0d,                    0xff1d160f,                    0xff09090a,                    0xff1c1210,                    0xff1d1b18,                    0xff0e1012 },
                new int[]    {                    0xFF201827,                    0xFF100f13,                    0xFF1b151a,                    0xFF141f22,                    0xFF0c0c0f,                    0xff090a0c,                    0xff0a0e0e,                    0xff080908,                    0xff1a1618,                    0xff13100d,                    0xff1e1a21,                    0xff0f0d0c,                    0xff0c0b08,                    0xff070707 },
                new int[]    {                    0xFF0e0b13,                    0xFF211623,                    0xFF130e12,                    0xFF0d0f11,                    0xFF10191f,                    0xff181c28,                    0xff142121,                    0xff121812,                    0xff130e11,                    0xff1a130f,                    0xff0b0a0b,                    0xff120d0b,                    0xff15140f,                    0xff101214 },
                new int[]    {                    0xFF1e192a,                    0xFF111016,                    0xFF21141a,                    0xFF111a1b,                    0xFF0a0d13,                    0xff0e0f12,                    0xff070c0b,                    0xff0b0d0b,                    0xff22121e,                    0xff0f0c0c,                    0xff110f17,                    0xff070606,                    0xff0c0a0a,                    0xff09090b },
                new int[]    {                             9,                            10,                            11,                            12,                            13,                             0,                             1,                             2,                             3,                             4,                             5,                             6,                             7,                             8 },
                new String[] { "YIxYGEALQVADAAAAA3QbEH0AowY", "9LW_RcoOSVACAAAAFTk3DTyXN-M", "O-wmAfBPSFADAAAA4zINVfD_bro", "F5oWoCs7QFACAAAAgf2bD_mg8Bw", "-Xc-np9y2VMCAAAARKr0yNNPYW0", "fqv01SQemVIBAAAApND8LDRUhRU", "F5oWoCs7QFACAAAAgf2bD_mg8Bw", "ptuUd96JSFACAAAATobI23sPpz0", "p-pXcflrmFIBAAAAvXYQk-mCwZU", "Nl8Pg2rBQVACAAAA25Lxtb8SDp0", "dhf9pceaQVACAAAAbzdVo4SCiZA", "9GcNVISdSVADAAAAUcw5BYjELW4", "9LW_RcoOSVACAAAAFTk3DTyXN-M", "dk_wwlghOFACAAAAfz9xrxi6euw" },
                new int[]    {                            45,                           135,                             0,                           180,                             0,                             0,                             0,                             0,                             0,                             0,                             0,                             0,                             0,                             0 },
                new int[]    {                            34,                            47,                            52,                            48,                            54,                            50,                            37,                            56,                            48,                            49,                            40,                            64,                            38,                            48 }
                );
        sortAccents(themeInfo);
        themes.add(themeInfo);
        themesDict.put("Night", themeInfo);

        sortThemes();

        ThemeInfo applyingTheme = null;
        SharedPreferences preferences = com.eve.app.bridge.TelegramBridge.getMainSettings(ApplicationLoader.applicationContext); // EVE-PATCH: use app shared preferences via Eve bridge
        try {
            final ThemeInfo themeDarkBlue = themesDict.get("Dark Blue");

            String theme = preferences.getString("theme", null);
            if ("Default".equals(theme)) {
                applyingTheme = themesDict.get("Blue");
                applyingTheme.currentAccentId = DEFALT_THEME_ACCENT_ID;
            } else if ("Dark".equals(theme)) {
                applyingTheme = themeDarkBlue;
                applyingTheme.currentAccentId = 9;
            } else if (theme != null) {
                applyingTheme = themesDict.get(theme);
                if (applyingTheme != null && !themeConfig.contains("lastDayTheme")) {
                    SharedPreferences.Editor editor = themeConfig.edit();
                    editor.putString("lastDayTheme", applyingTheme.getKey());
                    editor.commit();
                }
            }

            theme = preferences.getString("nighttheme", null);
            if ("Default".equals(theme)) {
                applyingTheme = themesDict.get("Blue");
                applyingTheme.currentAccentId = DEFALT_THEME_ACCENT_ID;
            } else if ("Dark".equals(theme)) {
                currentNightTheme = themeDarkBlue;
                themeDarkBlue.currentAccentId = 9;
            } else if (theme != null) {
                ThemeInfo t = themesDict.get(theme);
                if (t != null) {
                    currentNightTheme = t;
                }
            }

            if (currentNightTheme != null && !themeConfig.contains("lastDarkTheme")) {
                SharedPreferences.Editor editor = themeConfig.edit();
                editor.putString("lastDarkTheme", currentNightTheme.getKey());
                editor.commit();
            }

            SharedPreferences.Editor oldEditor = null;
            SharedPreferences.Editor oldEditorNew = null;
            for (ThemeInfo info : themesDict.values()) {
                if (info.assetName != null && info.accentBaseColor != 0) {
                    String accents = themeConfig.getString("accents_" + info.assetName, null);
                    info.currentAccentId = themeConfig.getInt("accent_current_" + info.assetName, info.firstAccentIsDefault ? DEFALT_THEME_ACCENT_ID : 0);
                    ArrayList<ThemeAccent> newAccents = new ArrayList<>();
                    if (!newAccents.isEmpty()) {
                        info.themeAccents.addAll(0, newAccents);
                        sortAccents(info);
                    }
                    if (info.themeAccentsMap != null && info.themeAccentsMap.get(info.currentAccentId) == null) {
                        info.currentAccentId = info.firstAccentIsDefault ? DEFALT_THEME_ACCENT_ID : 0;
                    }
                    ThemeAccent accent = info.getAccent(false);
                    if (accent != null) {
                    }
                }
            }
            if (oldEditor != null) {
                oldEditor.commit();
                oldEditorNew.commit();
            }

            selectedAutoNightType = preferences.getInt("selectedAutoNightType", Build.VERSION.SDK_INT >= 29 ? AUTO_NIGHT_TYPE_SYSTEM : AUTO_NIGHT_TYPE_NONE);
            autoNightScheduleByLocation = preferences.getBoolean("autoNightScheduleByLocation", false);
            autoNightBrighnessThreshold = preferences.getFloat("autoNightBrighnessThreshold", 0.25f);
            autoNightDayStartTime = preferences.getInt("autoNightDayStartTime", 22 * 60);
            autoNightDayEndTime = preferences.getInt("autoNightDayEndTime", 8 * 60);
            autoNightSunsetTime = preferences.getInt("autoNightSunsetTime", 22 * 60);
            autoNightSunriseTime = preferences.getInt("autoNightSunriseTime", 8 * 60);
            autoNightCityName = preferences.getString("autoNightCityName", "");
            long val = preferences.getLong("autoNightLocationLatitude3", 10000);
            if (val != 10000) {
                autoNightLocationLatitude = Double.longBitsToDouble(val);
            } else {
                autoNightLocationLatitude = 10000;
            }
            val = preferences.getLong("autoNightLocationLongitude3", 10000);
            if (val != 10000) {
                autoNightLocationLongitude = Double.longBitsToDouble(val);
            } else {
                autoNightLocationLongitude = 10000;
            }
            autoNightLastSunCheckDay = preferences.getInt("autoNightLastSunCheckDay", -1);
        } catch (Exception e) {
            FileLog.e(e);
            throw new RuntimeException(e);
        }
        if (applyingTheme == null) {
            applyingTheme = defaultTheme;
        } else {
            currentDayTheme = applyingTheme;
        }

    }

    private static void sortAccents(ThemeInfo info) {
        Collections.sort(info.themeAccents, (o1, o2) -> {
            if (isHome(o1)) {
                return -1;
            }
            if (isHome(o2)) {
                return 1;
            }
            int i1 = o1.isDefault ? 1 : 0;
            int i2 = o2.isDefault ? 1 : 0;

            if (i1 == i2) {
                if (o1.isDefault) {
                    if (o1.id > o2.id) {
                        return 1;
                    } else if (o1.id < o2.id) {
                        return -1;
                    }
                } else {
                    if (o1.id > o2.id) {
                        return -1;
                    } else if (o1.id < o2.id) {
                        return 1;
                    }
                }
            } else {
                if (i1 > i2) {
                    return -1;
                } else {
                    return 1;
                }
            }
            return 0;
        });
    }

    private static Method StateListDrawable_getStateDrawableMethod;
    private static Field BitmapDrawable_mColorFilter;

    @SuppressLint("PrivateApi")
    private static Drawable getStateDrawable(Drawable drawable, int index) {
        if (Build.VERSION.SDK_INT >= 29 && drawable instanceof StateListDrawable) {
            return ((StateListDrawable) drawable).getStateDrawable(index);
        } else {
            if (StateListDrawable_getStateDrawableMethod == null) {
                try {
                    StateListDrawable_getStateDrawableMethod = StateListDrawable.class.getDeclaredMethod("getStateDrawable", int.class);
                } catch (Throwable ignore) {

                }
            }
            if (StateListDrawable_getStateDrawableMethod == null) {
                return null;
            }
            try {
                return (Drawable) StateListDrawable_getStateDrawableMethod.invoke(drawable, index);
            } catch (Exception ignore) {

            }
            return null;
        }
    }

    public static Drawable createEmojiIconSelectorDrawable(Context context, int resource, int defaultColor, int pressedColor) {
        Resources resources = context.getResources();
        Drawable defaultDrawable = resources.getDrawable(resource).mutate();
        if (defaultColor != 0) {
            defaultDrawable.setColorFilter(new PorterDuffColorFilter(defaultColor, PorterDuff.Mode.MULTIPLY));
        }
        Drawable pressedDrawable = resources.getDrawable(resource).mutate();
        if (pressedColor != 0) {
            pressedDrawable.setColorFilter(new PorterDuffColorFilter(pressedColor, PorterDuff.Mode.MULTIPLY));
        }
        StateListDrawable stateListDrawable = new StateListDrawable() {
            @Override
            public boolean selectDrawable(int index) {
                return super.selectDrawable(index);
            }
        };
        stateListDrawable.setEnterFadeDuration(1);
        stateListDrawable.setExitFadeDuration(200);
        stateListDrawable.addState(new int[]{android.R.attr.state_selected}, pressedDrawable);
        stateListDrawable.addState(new int[]{}, defaultDrawable);
        return stateListDrawable;
    }

    public static Drawable createSimpleSelectorDrawable(Context context, int resource, int defaultColor, int pressedColor) {
        Resources resources = context.getResources();
        Drawable defaultDrawable = resources.getDrawable(resource).mutate();
        if (defaultColor != 0) {
            defaultDrawable.setColorFilter(new PorterDuffColorFilter(defaultColor, PorterDuff.Mode.MULTIPLY));
        }
        Drawable pressedDrawable = resources.getDrawable(resource).mutate();
        if (pressedColor != 0) {
            pressedDrawable.setColorFilter(new PorterDuffColorFilter(pressedColor, PorterDuff.Mode.MULTIPLY));
        }
        StateListDrawable stateListDrawable = new StateListDrawable() {
            @Override
            public boolean selectDrawable(int index) {
                return super.selectDrawable(index);
            }
        };
        stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, pressedDrawable);
        stateListDrawable.addState(new int[]{android.R.attr.state_selected}, pressedDrawable);
        stateListDrawable.addState(StateSet.WILD_CARD, defaultDrawable);
        return stateListDrawable;
    }

    public static ShapeDrawable createCircleDrawable(int size, int color) {
        OvalShape ovalShape = new OvalShape();
        ovalShape.resize(size, size);
        ShapeDrawable defaultDrawable = new ShapeDrawable(ovalShape);
        defaultDrawable.setIntrinsicWidth(size);
        defaultDrawable.setIntrinsicHeight(size);
        defaultDrawable.getPaint().setColor(color);
        return defaultDrawable;
    }

    public static Drawable createOutlineCircleDrawable(int size, int color, int strokeWidth) {
        return new Drawable() {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG); {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(strokeWidth);
                paint.setColor(color);
            }
            @Override
            public void draw(@NonNull Canvas canvas) {
                final Rect b = getBounds();
                canvas.drawCircle(b.centerX(), b.centerY(), size / 2.0f, paint);
            }
            @Override
            public void setAlpha(int alpha) {
                paint.setAlpha(alpha);
            }
            @Override
            public void setColorFilter(@Nullable ColorFilter colorFilter) {
                paint.setColorFilter(colorFilter);
            }
            @Override
            public int getOpacity() {
                return PixelFormat.TRANSPARENT;
            }
            @Override
            public int getIntrinsicWidth() {
                return size + strokeWidth;
            }
            @Override
            public int getIntrinsicHeight() {
                return size + strokeWidth;
            }
        };
    }

    public static ShapeDrawable createCircleDrawable(int size, int colorTop, int colorBottom) {
        OvalShape ovalShape = new OvalShape();
        ovalShape.resize(size, size);
        ShapeDrawable defaultDrawable = new ShapeDrawable(ovalShape);
        defaultDrawable.setIntrinsicWidth(size);
        defaultDrawable.setIntrinsicHeight(size);
        LinearGradient gradient = new LinearGradient(
            0, 0, 0, size,
            colorTop, colorBottom,
            Shader.TileMode.CLAMP
        );
        defaultDrawable.getPaint().setShader(gradient);
        return defaultDrawable;
    }

    public static CombinedDrawable createCircleDrawableWithIcon(int size, int iconRes) {
        return createCircleDrawableWithIcon(size, iconRes, 0);
    }

    public static CombinedDrawable createCircleDrawableWithIcon(int size, int iconRes, int stroke) {
        Drawable drawable;
        if (iconRes != 0) {
            drawable = ApplicationLoader.applicationContext.getResources().getDrawable(iconRes).mutate();
        } else {
            drawable = null;
        }
        return createCircleDrawableWithIcon(size, drawable, stroke);
    }

    public static CombinedDrawable createCircleDrawableWithIcon(int size, Drawable drawable, int stroke) {
        OvalShape ovalShape = new OvalShape();
        ovalShape.resize(size, size);
        ShapeDrawable defaultDrawable = new ShapeDrawable(ovalShape);
        Paint paint = defaultDrawable.getPaint();
        paint.setColor(0xffffffff);
        if (stroke == 1) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
        } else if (stroke == 2) {
            paint.setAlpha(0);
        }
        CombinedDrawable combinedDrawable = new CombinedDrawable(defaultDrawable, drawable);
        combinedDrawable.setCustomSize(size, size);
        return combinedDrawable;
    }

    public static Drawable createRoundRectDrawableWithIcon(int rad, int iconRes) {
        ShapeDrawable defaultDrawable = new ShapeDrawable(new RoundRectShape(new float[]{rad, rad, rad, rad, rad, rad, rad, rad}, null, null));
        defaultDrawable.getPaint().setColor(0xffffffff);
        Drawable drawable = ApplicationLoader.applicationContext.getResources().getDrawable(iconRes).mutate();
        return new CombinedDrawable(defaultDrawable, drawable);
    }

    public static int getWallpaperColor(int color) {
        if (color == 0) {
            return 0;
        }
        return color | 0xff000000;
    }

    public static float getThemeIntensity(float value) {
        if (value < 0 && !getActiveTheme().isDark()) {
            return -value;
        }
        return value;
    }

    public static void setCombinedDrawableColor(Drawable combinedDrawable, int color, boolean isIcon) {
        if (!(combinedDrawable instanceof CombinedDrawable)) {
            return;
        }
        Drawable drawable;
        if (isIcon) {
            drawable = ((CombinedDrawable) combinedDrawable).getIcon();
        } else {
            drawable = ((CombinedDrawable) combinedDrawable).getBackground();
        }
        if (drawable instanceof ColorDrawable) {
            ((ColorDrawable) drawable).setColor(color);
        } else {
            drawable.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
        }
    }

    public static Drawable createSimpleSelectorCircleDrawable(int size, int defaultColor, int pressedColor) {
        OvalShape ovalShape = new OvalShape();
        ovalShape.resize(size, size);
        ShapeDrawable defaultDrawable = new ShapeDrawable(ovalShape);
        defaultDrawable.getPaint().setColor(defaultColor);
        ShapeDrawable pressedDrawable = new ShapeDrawable(ovalShape);
        pressedDrawable.getPaint().setColor(0xffffffff);
        ColorStateList colorStateList = new ColorStateList(
                new int[][]{StateSet.WILD_CARD},
                new int[]{pressedColor}
        );
        return new BaseCell.RippleDrawableSafe(colorStateList, defaultDrawable, pressedDrawable);
    }

    public static ShapeDrawable createRoundRectDrawable(int rad, int defaultColor) {
        ShapeDrawable defaultDrawable = new ShapeDrawable(new RoundRectShape(new float[]{rad, rad, rad, rad, rad, rad, rad, rad}, null, null));
        defaultDrawable.getPaint().setColor(defaultColor);
        return defaultDrawable;
    }
    public static InsetDrawable createRoundRectDrawableShadowed(int rad, int defaultColor) {
        ShapeDrawable defaultDrawable = new ShapeDrawable(new RoundRectShape(new float[]{rad, rad, rad, rad, rad, rad, rad, rad}, null, null));
        defaultDrawable.getPaint().setColor(defaultColor);
        defaultDrawable.getPaint().setShadowLayer(dpf2(2), 0, dpf2(0.33f), multAlpha(0x11000000, Color.alpha(defaultColor) / 255.0f));
        return new InsetDrawable(defaultDrawable, dp(3), dp(3), dp(3), dp(3));
    }

    public static GradientDrawable createRoundRectGradientDrawable(int rad, int topColor, int bottomColor) {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.RIGHT_LEFT,
                new int[]{ topColor, bottomColor }
        );
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setCornerRadius(rad);
        return gd;
    }

    public static ShapeDrawable createRoundRectDrawable(int topLeftRed, int topRightRad, int bottomRightRad, int bottomLeftRad, int defaultColor) {
        ShapeDrawable defaultDrawable = new ShapeDrawable(new RoundRectShape(new float[]{topLeftRed, topLeftRed, topRightRad, topRightRad, bottomRightRad, bottomRightRad, bottomLeftRad, bottomLeftRad}, null, null));
        defaultDrawable.getPaint().setColor(defaultColor);
        return defaultDrawable;
    }

    public static ShapeDrawable createRoundRectDrawable(int topRad, int bottomRad, int defaultColor) {
        ShapeDrawable defaultDrawable = new ShapeDrawable(new RoundRectShape(new float[]{topRad, topRad, topRad, topRad, bottomRad, bottomRad, bottomRad, bottomRad}, null, null));
        defaultDrawable.getPaint().setColor(defaultColor);
        return defaultDrawable;
    }

    public static Drawable createServiceDrawable(int rad, View view, View containerView) {
        return createServiceDrawable(rad, view, containerView, chat_actionBackgroundPaint);
    }

    public static Drawable createServiceDrawable(int rad, View view, View containerView, Paint backgroundPaint) {
        return createServiceDrawable(rad, view, containerView, backgroundPaint, null);
    }

    public static Drawable createServiceDrawable(int rad, View view, View containerView, Theme.ResourcesProvider resourcesProvider) {
        return createServiceDrawable(rad, view, containerView, null, resourcesProvider);
    }

    public static Drawable createServiceDrawable(int rad, View view, View containerView, Paint backgroundPaint, Theme.ResourcesProvider resourcesProvider) {
        return new Drawable() {

            private RectF rect = new RectF();

            @Override
            public void draw(@NonNull Canvas canvas) {
                Rect bounds = getBounds();
                rect.set(bounds.left, bounds.top, bounds.right, bounds.bottom);
                canvas.drawRoundRect(rect, rad, rad, backgroundPaint != null ? backgroundPaint : Theme.getThemePaint(Theme.key_paint_chatActionBackground, resourcesProvider));
                if (resourcesProvider != null ? resourcesProvider.hasGradientService() : hasGradientService()) {
                    canvas.drawRoundRect(rect, rad, rad, Theme.getThemePaint(Theme.key_paint_chatActionBackgroundDarken, resourcesProvider));
                }
            }

            @Override
            public void setAlpha(int alpha) {

            }

            @Override
            public void setColorFilter(@Nullable ColorFilter colorFilter) {

            }

            @Override
            public int getOpacity() {
                return PixelFormat.TRANSPARENT;
            }
        };
    }

    public static Drawable createSimpleSelectorRoundRectDrawable(int rad, int defaultColor, int pressedColor) {
        return createSimpleSelectorRoundRectDrawable(rad, defaultColor, pressedColor, pressedColor);
    }

    public static Drawable createSimpleSelectorRoundRectDrawable(int rad, int defaultColor, int pressedColor, int maskColor) {
        return createSimpleSelectorRoundRectDrawable(rad, rad, rad, rad, defaultColor, pressedColor, maskColor);
    }

    public static Drawable createSimpleSelectorRoundRectDrawable(int leftTop, int rightTop, int rightBottom, int leftBottom, int defaultColor, int pressedColor, int maskColor) {
        return createSimpleSelectorRoundRectDrawable(new float[]{
                leftTop, leftTop,
                rightTop, rightTop,
                rightBottom, rightBottom,
                leftBottom, leftBottom
        }, defaultColor, pressedColor, maskColor);
    }

    private static Drawable createSimpleSelectorRoundRectDrawable(float[] rad, int defaultColor, int pressedColor, int maskColor) {
        return createSimpleSelectorRoundRectDrawable(rad, defaultColor, pressedColor, maskColor, 0);
    }
    private static Drawable createSimpleSelectorRoundRectDrawable(float[] rad, int defaultColor, int pressedColor, int maskColor, int inset) {
        ShapeDrawable defaultDrawable = new ShapeDrawable(new RoundRectShape(rad, null, null));
        defaultDrawable.setPadding(inset, inset, inset, inset);
        defaultDrawable.getPaint().setColor(defaultColor);
        ShapeDrawable pressedDrawable = new ShapeDrawable(new RoundRectShape(rad, null, null));
        pressedDrawable.getPaint().setColor(maskColor);
        pressedDrawable.setPadding(inset, inset, inset, inset);
        ColorStateList colorStateList = new ColorStateList(
                new int[][]{StateSet.WILD_CARD},
                new int[]{pressedColor}
        );
        return new BaseCell.RippleDrawableSafe(colorStateList, defaultDrawable, pressedDrawable);
    }

    public static Drawable createSelectorDrawableFromDrawables(Drawable normal, Drawable pressed) {
        StateListDrawable stateListDrawable = new StateListDrawable();
        stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, pressed);
        stateListDrawable.addState(new int[]{android.R.attr.state_selected}, pressed);
        stateListDrawable.addState(StateSet.WILD_CARD, normal);
        return stateListDrawable;
    }

    public static Drawable getRoundRectSelectorDrawable(int color) {
        return getRoundRectSelectorDrawable(dp(3), color);
    }

    public static Drawable getRoundRectSelectorDrawable(int corners, int color) {
        Drawable maskDrawable = createRoundRectDrawable(corners, 0xffffffff);
        ColorStateList colorStateList = new ColorStateList(
                new int[][]{StateSet.WILD_CARD},
                new int[]{(color & 0x00ffffff) | 0x19000000}
        );
        return new BaseCell.RippleDrawableSafe(colorStateList, null, maskDrawable);
    }

    public static Drawable createSelectorWithBackgroundDrawable(int backgroundColor, int color) {
        Drawable maskDrawable = new ColorDrawable(backgroundColor);
        ColorStateList colorStateList = new ColorStateList(
                new int[][]{StateSet.WILD_CARD},
                new int[]{color}
        );
        return new BaseCell.RippleDrawableSafe(colorStateList, new ColorDrawable(backgroundColor), maskDrawable);
    }

    public static Drawable getSelectorDrawable(boolean whiteBackground) {
        return getSelectorDrawable(getColor(key_listSelector), whiteBackground);
    }
    public static Drawable getSelectorDrawable(boolean whiteBackground, ResourcesProvider resourcesProvider) {
        final int color = getColor(key_listSelector, resourcesProvider);
        if (whiteBackground) {
            return getSelectorDrawable(color, key_windowBackgroundWhite, resourcesProvider);
        } else {
            return createSelectorDrawable(color, 2);
        }
    }

    public static Drawable getSelectorDrawable(int color, boolean whiteBackground) {
        if (whiteBackground) {
            return getSelectorDrawable(color, key_windowBackgroundWhite);
        } else {
            return createSelectorDrawable(color, 2);
        }
    }

    public static Drawable getSelectorDrawable(int color, int backgroundColor) {
        return getSelectorDrawable(color, backgroundColor, null);
    }
    public static Drawable getSelectorDrawable(int color, int backgroundColor, Theme.ResourcesProvider resourcesProvider) {
        if (backgroundColor >= 0) {
            Drawable maskDrawable = new ColorDrawable(0xffffffff);
            ColorStateList colorStateList = new ColorStateList(
                    new int[][]{StateSet.WILD_CARD},
                    new int[]{color}
            );
            return new BaseCell.RippleDrawableSafe(colorStateList, new ColorDrawable(getColor(backgroundColor, resourcesProvider)), maskDrawable);
        } else {
            return createSelectorDrawable(color, 2);
        }
    }

    public static Drawable createSelectorDrawable(int color) {
        return createSelectorDrawable(color, RIPPLE_MASK_CIRCLE_20DP, -1);
    }

    public static Drawable createSelectorDrawable(int color, int maskType) {
        return createSelectorDrawable(color, maskType, -1);
    }

    public static final int RIPPLE_MASK_CIRCLE_20DP = 1;
    public static final int RIPPLE_MASK_ALL = 2;
    public static final int RIPPLE_MASK_CIRCLE_TO_BOUND_EDGE = 3;
    public static final int RIPPLE_MASK_CIRCLE_TO_BOUND_CORNER = 4;
    public static final int RIPPLE_MASK_CIRCLE_AUTO = 5;
    public static final int RIPPLE_MASK_ROUNDRECT_6DP = 7;

    public static Drawable createSelectorDrawable(int color, int maskType, int radius) {
        Drawable maskDrawable = null;
        if ((maskType == RIPPLE_MASK_CIRCLE_20DP || maskType == 5) && Build.VERSION.SDK_INT >= 23) {
            maskDrawable = null;
        } else if (
            maskType == RIPPLE_MASK_CIRCLE_20DP ||
            maskType == RIPPLE_MASK_CIRCLE_TO_BOUND_EDGE ||
            maskType == RIPPLE_MASK_CIRCLE_TO_BOUND_CORNER ||
            maskType == RIPPLE_MASK_CIRCLE_AUTO ||
            maskType == 6 ||
            maskType == RIPPLE_MASK_ROUNDRECT_6DP
        ) {
            maskPaint.setColor(0xffffffff);
            maskDrawable = new Drawable() {

                RectF rect;

                @Override
                public void draw(Canvas canvas) {
                    Rect bounds = getBounds();
                    if (maskType == RIPPLE_MASK_ROUNDRECT_6DP) {
                        if (rect == null) {
                            rect = new RectF();
                        }
                        rect.set(bounds);
                        float rad = radius <= 0 ? dp(6) : radius;
                        canvas.drawRoundRect(rect, rad, rad, maskPaint);
                    } else {
                        int rad;
                        if (maskType == RIPPLE_MASK_CIRCLE_20DP || maskType == 6) {
                            rad = radius <= 0 ? dp(20) : radius;
                        } else if (maskType == RIPPLE_MASK_CIRCLE_TO_BOUND_EDGE) {
                            rad = (Math.max(bounds.width(), bounds.height()) / 2);
                        } else {
                            // RIPPLE_MASK_CIRCLE_AUTO = 5
                            // RIPPLE_MASK_CIRCLE_TO_BOUND_CORNER = 4
                            rad = (int) Math.ceil(Math.sqrt((bounds.left - bounds.centerX()) * (bounds.left - bounds.centerX()) + (bounds.top - bounds.centerY()) * (bounds.top - bounds.centerY())));
                        }
                        canvas.drawCircle(bounds.centerX(), bounds.centerY(), rad, maskPaint);
                    }
                }

                @Override
                public void setAlpha(int alpha) {

                }

                @Override
                public void setColorFilter(ColorFilter colorFilter) {

                }

                @Override
                public int getOpacity() {
                    return PixelFormat.UNKNOWN;
                }
            };
        } else if (maskType == RIPPLE_MASK_ALL) {
            maskDrawable = new ColorDrawable(0xffffffff);
        }
        ColorStateList colorStateList = new ColorStateList(
            new int[][]{ StateSet.WILD_CARD },
            new int[]{ color }
        );
        RippleDrawable rippleDrawable = new BaseCell.RippleDrawableSafe(colorStateList, null, maskDrawable);
        if (Build.VERSION.SDK_INT >= 23) {
            if (maskType == RIPPLE_MASK_CIRCLE_20DP) {
                rippleDrawable.setRadius(radius <= 0 ? dp(20) : radius);
            } else if (maskType == RIPPLE_MASK_CIRCLE_AUTO) {
                rippleDrawable.setRadius(RippleDrawable.RADIUS_AUTO);
            }
        }
        return rippleDrawable;
    }

    public static Drawable createInsetRoundRectDrawable(int color, float radius, int inset) {
        return createInsetRoundRectDrawable(color, radius, inset, inset);
    }

    public static Drawable createInsetRoundRectDrawable(int color, float radius, int insetW, int insetH) {
        return createInsetRoundRectDrawable(color, radius, insetW, insetH, insetW, insetH);
    }

    public static Drawable createInsetRoundRectDrawable(int color, float radius, int insetL, int insetT, int insetR, int insetB) {
        maskPaint.setColor(0xffffffff);
        Drawable maskDrawable = new Drawable() {
            private final RectF rectF = new RectF();

            @Override
            public void draw(Canvas canvas) {
                rectF.set(getBounds());
                rectF.left += insetL;
                rectF.top += insetT;
                rectF.right -= insetR;
                rectF.bottom -= insetB;
                canvas.drawRoundRect(rectF, radius, radius, maskPaint);
            }

            @Override
            public void setAlpha(int alpha) {
            }

            @Override
            public void setColorFilter(ColorFilter colorFilter) {
            }

            @Override
            public int getOpacity() {
                return PixelFormat.UNKNOWN;
            }
        };
        ColorStateList colorStateList = new ColorStateList(
                new int[][]{StateSet.WILD_CARD},
                new int[]{color}
        );
        return new BaseCell.RippleDrawableSafe(colorStateList, null, maskDrawable);
    }

    public static Drawable createCircleSelectorDrawable(int color, int leftInset, int rightInset) {
        maskPaint.setColor(0xffffffff);
        Drawable maskDrawable = new Drawable() {

            @Override
            public void draw(Canvas canvas) {
                Rect bounds = getBounds();
                final int rad = (Math.max(bounds.width(), bounds.height()) / 2) + leftInset + rightInset;
                canvas.drawCircle(bounds.centerX() - leftInset + rightInset, bounds.centerY(), rad, maskPaint);
            }

            @Override
            public void setAlpha(int alpha) {
            }

            @Override
            public void setColorFilter(ColorFilter colorFilter) {
            }

            @Override
            public int getOpacity() {
                return PixelFormat.UNKNOWN;
            }
        };
        ColorStateList colorStateList = new ColorStateList(
                new int[][]{StateSet.WILD_CARD},
                new int[]{color}
        );
        return new BaseCell.RippleDrawableSafe(colorStateList, null, maskDrawable);
    }

    /**
     * AdaptiveRipple implements methods for creating ripple drawables
     * Ripple color is based on background color, shifting saturation and value
     *
     * There are four public static methods:
     *  circle(String|int backgroundColor?, float radius?)
     *      - creates ripple drawable for circle buttons, draws only ripples
     *        radius (by default) can be RADIUS_TO_BOUNDS (-1): will be calculated to touch bounds edges
     *        radius can be RADIUS_OUT_BOUNDS (-2): will be calculated to touch bounds corners (fill entire bounds)
     *        otherwise radius is described in dp
     *  filledCircle(String|Drawable|int background?, String|int backgroundColor?, float radius?)
     *      - creates ripple drawable for circle buttons, draws ripples and background
     *        (!) it's not recommended to use only drawable background, as it would be impossible to get background color
     *          (there would be an attempt to cast drawable to ColorDrawable to retrieve a color)
     *  rect(String|int backgroundColor?, ...float radii?)
     *      - creates ripple drawable for rectangular buttons, draws only ripples
     *        radii array is allowed to have 0, 1, 2, 3, 4, 8 elements:
     *        0 - no rounding
     *        1 - all roundings
     *        2 - top, bottom roundings
     *        3 - top-left, top-right, bottom roundings
     *        4 - top-left, top-right, bottom-right, bottom-left roundings
     *        8 - top-left-x, top-left-y, top-right-x, top-right-y,
     *            bottom-right-x, bottom-right-y, bottom-left-x, bottom-left-y roundings
     *        in case it has more, would be downgraded to nearest (ex.: 5 -> 4, 10 -> 8)
     *   filledRect(String|Drawable|int background?, String|int backgroundColor?, ...float radii?)
     *      - creates ripple drawable for rectangular buttons, draws ripples and background
     *
     *  by default, backgroundColor is getColor(Theme.key_windowBackgroundWhite)
     */
    public static class AdaptiveRipple {

        public static final float RADIUS_TO_BOUNDS = -1;
        public static final float RADIUS_OUT_BOUNDS = -2;

        private static final int defaultBackgroundColorKey = Theme.key_windowBackgroundWhite;

        public static Drawable circle() {
            return circle(Theme.getColor(defaultBackgroundColorKey), RADIUS_TO_BOUNDS);
        }
        public static Drawable circleByKey(int backgroundColorKey) {
            return circle(Theme.getColor(backgroundColorKey), RADIUS_TO_BOUNDS);
        }
        public static Drawable circleByKey(int backgroundColorKey, float radius) {
            return circle(Theme.getColor(backgroundColorKey), radius);
        }
        public static Drawable circle(int backgroundColor) {
            return circle(backgroundColor, RADIUS_TO_BOUNDS);
        }
        public static Drawable circle(int backgroundColor, float radius) {
            return createCircle(calcRippleColor(backgroundColor), radius);
        }

        public static Drawable filledCircle() {
            return filledCircle(null, Theme.getColor(defaultBackgroundColorKey), RADIUS_TO_BOUNDS);
        }
        public static Drawable filledCircle(Drawable background) {
            return filledCircle(background, Theme.getColor(defaultBackgroundColorKey), RADIUS_TO_BOUNDS);
        }
        public static Drawable filledCircleByKey(int backgroundColorKey) {
            return filledCircle(null, Theme.getColor(backgroundColorKey), RADIUS_TO_BOUNDS);
        }
        public static Drawable filledCircle(Drawable background, int backgroundColorKey) {
            return filledCircle(background, Theme.getColor(backgroundColorKey), RADIUS_TO_BOUNDS);
        }
        public static Drawable filledCircleByKey(int backgroundColorKey, float radius) {
            return filledCircle(null, Theme.getColor(backgroundColorKey), radius);
        }
        public static Drawable filledCircleByKey(Drawable background, int backgroundColorKey, float radius) {
            return filledCircle(background, Theme.getColor(backgroundColorKey), radius);
        }
        public static Drawable filledCircle(int backgroundColor) {
            return filledCircle(null, backgroundColor, RADIUS_TO_BOUNDS);
        }
        public static Drawable filledCircle(int backgroundColor, float radius) {
            return filledCircle(null, backgroundColor, radius);
        }
        public static Drawable filledCircle(Drawable background, int backgroundColor, float radius) {
            return createCircle(background, calcRippleColor(backgroundColor), radius);
        }

        public static Drawable rect() {
            return rect(Theme.getColor(defaultBackgroundColorKey));
        }
        public static Drawable rectByKey(int backgroundColorKey) {
            return rect(Theme.getColor(backgroundColorKey));
        }

        public static Drawable rectByKey(int backgroundColorKey, float ...radii) {
            return rect(Theme.getColor(backgroundColorKey), radii);
        }
        public static Drawable rect(int backgroundColor) {
            return rect(backgroundColor, 0);
        }
        public static Drawable rect(int backgroundColor, float ...radii) {
            return createRect(0, calcRippleColor(backgroundColor), radii);
        }

        public static Drawable filledRect() {
            return filledRect(Theme.getColor(defaultBackgroundColorKey), 0);
        }
        public static Drawable filledRect(Drawable background) {
            int backgroundColor = background instanceof ColorDrawable ? ((ColorDrawable) background).getColor() : Theme.getColor(defaultBackgroundColorKey);
            return filledRect(background, backgroundColor, 0);
        }
        public static Drawable filledRectByKey(int backgroundColorKey) {
            return filledRect(Theme.getColor(backgroundColorKey));
        }
        public static Drawable filledRectByKey(Drawable background, int backgroundColorKey) {
            return filledRect(background, Theme.getColor(backgroundColorKey));
        }
        public static Drawable filledRectByKey(int backgroundColorKey, float ...radii) {
            return filledRect(Theme.getColor(backgroundColorKey), radii);
        }
        public static Drawable filledRectByKey(Drawable background, int backgroundColorKey, float ...radii) {
            return filledRect(background, Theme.getColor(backgroundColorKey), radii);
        }
        public static Drawable filledRect(int backgroundColor) {
            return createRect(backgroundColor, calcRippleColor(backgroundColor));
        }
        public static Drawable filledRect(int backgroundColor, float ...radii) {
            return createRect(backgroundColor, calcRippleColor(backgroundColor), radii);
        }
        public static Drawable filledRect(Drawable background, int backgroundColor, float ...radii) {
            return createRect(background, calcRippleColor(backgroundColor), radii);
        }

        public static Drawable createRect(int rippleColor, float ...radii) {
            return createRect(0, rippleColor, radii);
        }
        public static Drawable createRect(int backgroundColor, int rippleColor, float ...radii) {
            Drawable background = null;
            if (backgroundColor != 0) {
                if (hasNonzeroRadii(radii)) {
                    background = new ShapeDrawable(new RoundRectShape(calcRadii(radii), null, null));
                    ((ShapeDrawable) background).getPaint().setColor(backgroundColor);
                } else {
                    background = new ColorDrawable(backgroundColor);
                }
            }
            return createRect(
                background,
                rippleColor,
                radii
            );
        }
        private static Drawable createRect(Drawable background, int rippleColor, float ...radii) {
            Drawable maskDrawable = null;
            if (hasNonzeroRadii(radii)) {
                maskDrawable = new ShapeDrawable(new RoundRectShape(calcRadii(radii), null, null));
                ((ShapeDrawable) maskDrawable).getPaint().setColor(0xffffffff);
            } else {
                maskDrawable = new ShapeDrawable(new RectShape());
                ((ShapeDrawable) maskDrawable).getPaint().setColor(0xffffffff);
            }
            return new BaseCell.RippleDrawableSafe(
                new ColorStateList(
                    new int[][]{ StateSet.WILD_CARD },
                    new int[]{ rippleColor }
                ),
                background,
                maskDrawable
            );
        }

        private static Drawable createCircle(int rippleColor) {
            return createCircle(0, rippleColor, RADIUS_TO_BOUNDS);
        }
        private static Drawable createCircle(int rippleColor, float radius) {
            return createCircle(0, rippleColor, radius);
        }
        private static Drawable createCircle(int backgroundColor, int rippleColor, float radius) {
            return createCircle(
                backgroundColor == 0 ? null : new CircleDrawable(radius, backgroundColor),
                rippleColor,
                radius
            );
        }
        private static Drawable createCircle(Drawable background, int rippleColor, float radius) {
            return new BaseCell.RippleDrawableSafe(
                new ColorStateList(
                        new int[][]{StateSet.WILD_CARD},
                        new int[]{rippleColor}
                ),
                background,
                new CircleDrawable(radius)
            );
        }

        private static class CircleDrawable extends Drawable {

            private static Paint maskPaint;
            private Paint paint;
            private float radius;

            public CircleDrawable(float radius) {
                this.radius = radius;
                if (this.maskPaint == null) {
                    this.maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    this.maskPaint.setColor(0xffffffff);
                }
                this.paint = this.maskPaint;
            }

            public CircleDrawable(float radius, int paintColor) {
                this.radius = radius;
                this.paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                this.paint.setColor(paintColor);
            }

            @Override
            public void draw(@NonNull Canvas canvas) {
                android.graphics.Rect bounds = getBounds();
                float rad;
                if (Math.abs(radius - RADIUS_TO_BOUNDS) < 0.01f) {
                    rad = Math.max(bounds.width(), bounds.height()) / 2;
                } else if (Math.abs(radius - RADIUS_OUT_BOUNDS) < 0.01f) {
                    rad = (int) Math.ceil(Math.sqrt((bounds.left - bounds.centerX()) * (bounds.left - bounds.centerX()) + (bounds.top - bounds.centerY()) * (bounds.top - bounds.centerY())));
                } else {
                    rad = dp(radius);
                }
                canvas.drawCircle(bounds.centerX(), bounds.centerY(), rad, paint);
            }

            @Override
            public void setAlpha(int i) {}
            @Override
            public void setColorFilter(@Nullable ColorFilter colorFilter) {}
            @Deprecated @Override
            public int getOpacity() {
                return PixelFormat.TRANSPARENT;
            }
        }

        private static float[] calcRadii(float ...radii) {
            if (radii.length == 0) {
                return new float[] { 0, 0, 0, 0, 0, 0, 0, 0 };
            } else if (radii.length == 1) {
                return new float[] { dp(radii[0]), dp(radii[0]), dp(radii[0]), dp(radii[0]), dp(radii[0]), dp(radii[0]), dp(radii[0]), dp(radii[0]) };
            } else if (radii.length == 2) {
                return new float[] { dp(radii[0]), dp(radii[0]), dp(radii[0]), dp(radii[0]), dp(radii[1]), dp(radii[1]), dp(radii[1]), dp(radii[1]) };
            } else if (radii.length == 3) {
                return new float[] { dp(radii[0]), dp(radii[0]), dp(radii[1]), dp(radii[1]), dp(radii[2]), dp(radii[2]), dp(radii[2]), dp(radii[2]) };
            } else if (radii.length < 8) {
                return new float[] { dp(radii[0]), dp(radii[0]), dp(radii[1]), dp(radii[1]), dp(radii[2]), dp(radii[2]), dp(radii[3]), dp(radii[3]) };
            } else {
                return new float[] { dp(radii[0]), dp(radii[1]), dp(radii[2]), dp(radii[3]), dp(radii[4]), dp(radii[5]), dp(radii[6]), dp(radii[7]) };
            }
        }
        private static boolean hasNonzeroRadii(float ...radii) {
            for (int i = 0; i < Math.min(8, radii.length); ++i) {
                if (radii[i] > 0) {
                    return true;
                }
            }
            return false;
        }

        private static float[] tempHSV;
        public static int calcRippleColor(int backgroundColor) {
            if (tempHSV == null) {
                tempHSV = new float[3];
            }
            Color.colorToHSV(backgroundColor, tempHSV);
            if (tempHSV[1] > 0.01f) {
                // when saturation is too low, hue is ignored
                // so changing saturation at that point would reveal ignored hue (usually red, hue=0)
                tempHSV[1] = Math.min(1, Math.max(0, tempHSV[1] + (isCurrentThemeDark() ? .25f : -.25f)));
                tempHSV[2] = Math.min(1, Math.max(0, tempHSV[2] + (isCurrentThemeDark() ? .05f : -.05f)));
            } else {
                tempHSV[2] = Math.min(1, Math.max(0, tempHSV[2] + (isCurrentThemeDark() ? .1f : -.1f)));
            }
            return Color.HSVToColor(127, tempHSV);
        }
    }

    public static class RippleRadMaskDrawable extends Drawable {
        private Path path = new Path();
        private float[] radii = new float[8];
        boolean invalidatePath = true;

        public RippleRadMaskDrawable(float top, float bottom) {
            radii[0] = radii[1] = radii[2] = radii[3] = dp(top);
            radii[4] = radii[5] = radii[6] = radii[7] = dp(bottom);
        }
        public RippleRadMaskDrawable(float topLeft, float topRight, float bottomRight, float bottomLeft) {
            radii[0] = radii[1] = dp(topLeft);
            radii[2] = radii[3] = dp(topRight);
            radii[4] = radii[5] = dp(bottomRight);
            radii[6] = radii[7] = dp(bottomLeft);
        }

        public void setRadius(float top, float bottom) {
            radii[0] = radii[1] = radii[2] = radii[3] = dp(top);
            radii[4] = radii[5] = radii[6] = radii[7] = dp(bottom);
            invalidatePath = true;
            invalidateSelf();
        }
        public void setRadius(float topLeft, float topRight, float bottomRight, float bottomLeft) {
            radii[0] = radii[1] = dp(topLeft);
            radii[2] = radii[3] = dp(topRight);
            radii[4] = radii[5] = dp(bottomRight);
            radii[6] = radii[7] = dp(bottomLeft);
            invalidatePath = true;
            invalidateSelf();
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            invalidatePath = true;
        }

        @Override
        public void draw(Canvas canvas) {
            if (invalidatePath) {
                invalidatePath = false;
                path.reset();
                AndroidUtilities.rectTmp.set(getBounds());
                path.addRoundRect(AndroidUtilities.rectTmp, radii, Path.Direction.CW);
            }
            canvas.drawPath(path, maskPaint);
        }

        @Override
        public void setAlpha(int alpha) {

        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {

        }

        @Override
        public int getOpacity() {
            return PixelFormat.UNKNOWN;
        }
    }

    public static void setMaskDrawableRad(Drawable rippleDrawable, int top, int bottom) {
        if (rippleDrawable instanceof RippleDrawable) {
            RippleDrawable drawable = (RippleDrawable) rippleDrawable;
            int count = drawable.getNumberOfLayers();
            for (int a = 0; a < count; a++) {
                Drawable layer = drawable.getDrawable(a);
                if (layer instanceof RippleRadMaskDrawable) {
                    ((RippleRadMaskDrawable) layer).setRadius(top, bottom);
                    break;
                }
            }
        }
    }

    public static void setMaskDrawableRad(Drawable rippleDrawable, float topLeftRad, float topRightRad, float bottomRightRad, float bottomLeftRad) {
        if (rippleDrawable instanceof RippleDrawable) {
            RippleDrawable drawable = (RippleDrawable) rippleDrawable;
            int count = drawable.getNumberOfLayers();
            for (int a = 0; a < count; a++) {
                Drawable layer = drawable.getDrawable(a);
                if (layer instanceof RippleRadMaskDrawable) {
                    ((RippleRadMaskDrawable) layer).setRadius(topLeftRad, topRightRad, bottomRightRad, bottomLeftRad);
                    break;
                }
            }
        }
    }

    public static Drawable createRadSelectorDrawable(int color, int topRad, int bottomRad) {
        maskPaint.setColor(0xffffffff);
        Drawable maskDrawable = new RippleRadMaskDrawable(topRad, bottomRad);
        ColorStateList colorStateList = new ColorStateList(
            new int[][] { StateSet.WILD_CARD },
            new int[] { color }
        );
        return new BaseCell.RippleDrawableSafe(colorStateList, null, maskDrawable);
    }

    public static Drawable createRadSelectorDrawable(int color, int rippleColor, int topRad, int bottomRad) {
        maskPaint.setColor(0xffffffff);
        Drawable maskDrawable = new RippleRadMaskDrawable(topRad, bottomRad);
        ColorStateList colorStateList = new ColorStateList(
            new int[][] { StateSet.WILD_CARD },
            new int[] { rippleColor }
        );
        return new BaseCell.RippleDrawableSafe(colorStateList, createRoundRectDrawable(dp(topRad), dp(bottomRad), color), maskDrawable);
    }

    public static Drawable createRadSelectorDrawable(int color, int topLeftRad, int topRightRad, int bottomRightRad, int bottomLeftRad) {
        maskPaint.setColor(0xffffffff);
        Drawable maskDrawable = new RippleRadMaskDrawable(topLeftRad, topRightRad, bottomRightRad, bottomLeftRad);
        ColorStateList colorStateList = new ColorStateList(
                new int[][]{StateSet.WILD_CARD},
                new int[]{color}
        );
        return new BaseCell.RippleDrawableSafe(colorStateList, null, maskDrawable);
    }

    private static void sortThemes() {
        Collections.sort(themes, (o1, o2) -> {
            if (o1.pathToFile == null && o1.assetName == null) {
                return -1;
            } else if (o2.pathToFile == null && o2.assetName == null) {
                return 1;
            }
            return o1.name.compareTo(o2.name);
        });
    }

    public static ThemeInfo getTheme(String key) {
        return themesDict.get(key);
    }

    /** bumped by every theme apply: a background apply that finishes after a newer apply must not overwrite it */
    private static int applyThemeGeneration;

    private static boolean useBlackText(int color1, int color2) {
        float r1 = Color.red(color1) / 255.0f;
        float r2 = Color.red(color2) / 255.0f;
        float g1 = Color.green(color1) / 255.0f;
        float g2 = Color.green(color2) / 255.0f;
        float b1 = Color.blue(color1) / 255.0f;
        float b2 = Color.blue(color2) / 255.0f;
        float r = (r1 * 0.5f + r2 * 0.5f);
        float g = (g1 * 0.5f + g2 * 0.5f);
        float b = (b1 * 0.5f + b2 * 0.5f);

        float lightness = 0.2126f * r + 0.7152f * g + 0.0722f * b;
        float lightness2 = 0.2126f * r1 + 0.7152f * g1 + 0.0722f * b1;
        return lightness > 0.705f || lightness2 > 0.705f;
    }

    private static int calculatedTableBackground(int bubbleColor, boolean isDarkTheme, boolean isOut) {
        if (isDarkTheme && isOut) {
            return multAlpha(Color.WHITE, .07f);
        }
        float[] hsv = getTempHsv(3);
        Color.colorToHSV(bubbleColor, hsv);
        if (isDarkTheme) {
            hsv[2] = Math.min(1f, hsv[2] + .07f);
            if (isOut) {
                hsv[1] = Math.min(1f, hsv[1] + .02f);
            }
        } else {
            hsv[2] = Math.max(0f, hsv[2] - (isOut ? .06f : .03f));
            if (isOut && hsv[1] > .02f) {
                hsv[1] = Math.min(1f, hsv[1] + .02f);
            }
        }
        return Color.HSVToColor(Color.alpha(bubbleColor), hsv);
    }

    private static int calculatedTableBorder(int bubbleColor, boolean isDarkTheme, boolean isOut) {
        if (isDarkTheme && isOut) {
            return multAlpha(Color.WHITE, .14f);
        }
        float[] hsv = getTempHsv(3);
        Color.colorToHSV(bubbleColor, hsv);
        if (isDarkTheme) {
            hsv[2] = Math.min(1f, hsv[2] + .14f);
            if (isOut) {
                hsv[1] = Math.min(1f, hsv[1] + .03f);
            }
        } else {
            hsv[2] = Math.max(0f, hsv[2] - (isOut ? .14f : .12f));
            if (isOut && hsv[1] > .02f) {
                hsv[1] = Math.min(1f, hsv[1] + .04f);
            }
        }
        return Color.HSVToColor(Color.alpha(bubbleColor), hsv);
    }

    private static int tableOutBubble(SparseIntArray colors) {
        int r = 0, g = 0, b = 0, count = 0;
        final int[] keys = {
            key_chat_outBubble, key_chat_outBubbleGradient1,
            key_chat_outBubbleGradient2, key_chat_outBubbleGradient3
        };
        for (int key : keys) {
            if (key != key_chat_outBubble && colors.indexOfKey(key) < 0) {
                continue;
            }
            final int color = colors.get(key, defaultColors[key]);
            r += Color.red(color);
            g += Color.green(color);
            b += Color.blue(color);
            count++;
        }
        return Color.rgb(r / count, g / count, b / count);
    }

    private static void applyCalculatedTableColors(SparseIntArray sourceColors, SparseIntArray colors, boolean isDarkTheme) {
        final int inBubble = colors.get(key_chat_inBubble, defaultColors[key_chat_inBubble]);
        final int outBubble = tableOutBubble(colors);
        if (sourceColors.indexOfKey(key_chat_inTableBackground) < 0) {
            colors.put(key_chat_inTableBackground, calculatedTableBackground(inBubble, isDarkTheme, false));
        }
        if (sourceColors.indexOfKey(key_chat_outTableBackground) < 0) {
            colors.put(key_chat_outTableBackground, calculatedTableBackground(outBubble, isDarkTheme, true));
        }
        if (sourceColors.indexOfKey(key_chat_inTableBorder) < 0) {
            colors.put(key_chat_inTableBorder, calculatedTableBorder(inBubble, isDarkTheme, false));
        }
        if (sourceColors.indexOfKey(key_chat_outTableBorder) < 0) {
            colors.put(key_chat_outTableBorder, calculatedTableBorder(outBubble, isDarkTheme, true));
        }
        if (sourceColors.indexOfKey(key_chat_outDivider) < 0) {
            final int replyLine = colors.get(key_chat_outReplyLine, defaultColors[key_chat_outReplyLine]);
            colors.put(key_chat_outDivider, multAlpha(replyLine, .2f));
        }
        if (isDarkTheme && sourceColors.indexOfKey(key_chat_inDivider) < 0) {
            final int replyText = colors.get(key_chat_inReplyMessageText, defaultColors[key_chat_inReplyMessageText]);
            colors.put(key_chat_inDivider, multAlpha(replyText, .2f));
        }
    }

    private static void applyCalculatedArticleCodeColors(SparseIntArray sourceColors, SparseIntArray colors, boolean isDarkTheme) {
        if (isDarkTheme && sourceColors.indexOfKey(key_chat_inArticleCodeBackground) < 0) {
            colors.put(key_chat_inArticleCodeBackground, multAlpha(Color.WHITE, .10f));
        }
        final int inBubble = colors.get(key_chat_inBubble, defaultColors[key_chat_inBubble]);
        final int outBubble = tableOutBubble(colors);
        if (sourceColors.indexOfKey(key_chat_inArticleCodeScrollbarBackground) < 0) {
            colors.put(key_chat_inArticleCodeScrollbarBackground, calculatedArticleCodeScrollbar(inBubble, isDarkTheme, false));
        }
        if (sourceColors.indexOfKey(key_chat_inArticleCodeScrollbar) < 0) {
            colors.put(key_chat_inArticleCodeScrollbar, calculatedArticleCodeScrollbar(inBubble, isDarkTheme, true));
        }
        if (sourceColors.indexOfKey(key_chat_outArticleCodeScrollbarBackground) < 0) {
            colors.put(key_chat_outArticleCodeScrollbarBackground, calculatedArticleCodeScrollbar(outBubble, isDarkTheme, false));
        }
        if (sourceColors.indexOfKey(key_chat_outArticleCodeScrollbar) < 0) {
            colors.put(key_chat_outArticleCodeScrollbar, calculatedArticleCodeScrollbar(outBubble, isDarkTheme, true));
        }
        if (sourceColors.indexOfKey(key_chat_inArticleDetailsArrow) < 0) {
            colors.put(key_chat_inArticleDetailsArrow, calculatedArticleDetailsColor(inBubble, isDarkTheme, false, true));
        }
        if (sourceColors.indexOfKey(key_chat_outArticleDetailsArrow) < 0) {
            colors.put(key_chat_outArticleDetailsArrow, calculatedArticleDetailsColor(outBubble, isDarkTheme, true, true));
        }
        if (sourceColors.indexOfKey(key_chat_inArticleDetailsLine) < 0) {
            colors.put(key_chat_inArticleDetailsLine, calculatedArticleDetailsColor(inBubble, isDarkTheme, false, false));
        }
        if (sourceColors.indexOfKey(key_chat_outArticleDetailsLine) < 0) {
            colors.put(key_chat_outArticleDetailsLine, calculatedArticleDetailsColor(outBubble, isDarkTheme, true, false));
        }
    }

    private static int calculatedArticleDetailsColor(int bubbleColor, boolean isDarkTheme, boolean isOut, boolean arrow) {
        if (isDarkTheme) {
            return multAlpha(Color.WHITE, arrow ? .62f : .18f);
        }
        final int baseColor = arrow ? 0xff9ea4a8 : 0xffd8d8d8;
        final float[] hsv = getTempHsv(3);
        Color.colorToHSV(bubbleColor, hsv);
        return isOut && hsv[1] > .02f ? adaptHue(baseColor, bubbleColor) : baseColor;
    }

    private static int calculatedArticleCodeScrollbar(int bubbleColor, boolean isDarkTheme, boolean thumb) {
        if (isDarkTheme) {
            return multAlpha(Color.WHITE, thumb ? .22f : .12f);
        }
        final int baseColor = thumb ? 0xffc5cdd5 : 0xffe1e6eb;
        final float[] hsv = getTempHsv(3);
        Color.colorToHSV(bubbleColor, hsv);
        return hsv[1] > .02f ? adaptHue(baseColor, bubbleColor) : baseColor;
    }

    public static boolean hasHue(int color) {
        float[] hsvTemp3 = getTempHsv(3);
        Color.colorToHSV(color, hsvTemp3);
        return hsvTemp3[1] > .1f && hsvTemp3[1] < .9f;
    }

    public static int changeColorAccent(int themeBaseAccent, int accent, int color, boolean isDark) {
        return changeColorAccent(themeBaseAccent, accent, color, isDark, color);
    }

    public static int changeColorAccent(int themeBaseAccent, int accent, int color, boolean isDark, int fallback) {
        float[] hsvTemp3 = getTempHsv(3);
        float[] hsvTemp4 = getTempHsv(4);
        Color.colorToHSV(themeBaseAccent, hsvTemp3);
        Color.colorToHSV(accent, hsvTemp4);
        return changeColorAccent(hsvTemp3, hsvTemp4, color, isDark, fallback);
    }

    public static int changeColorAccent(ThemeInfo themeInfo, int accent, int color) {
        if (accent == 0 || themeInfo.accentBaseColor == 0 || accent == themeInfo.accentBaseColor || themeInfo.firstAccentIsDefault && themeInfo.currentAccentId == DEFALT_THEME_ACCENT_ID) {
            return color;
        }
        float[] hsvTemp3 = getTempHsv(3);
        float[] hsvTemp4 = getTempHsv(4);

        Color.colorToHSV(themeInfo.accentBaseColor, hsvTemp3);
        Color.colorToHSV(accent, hsvTemp4);
        return changeColorAccent(hsvTemp3, hsvTemp4, color, themeInfo.isDark(), color);
    }

    public static float[] getTempHsv(int num) {
        ThreadLocal<float[]> local;
        switch (num) {
            case 1:
                local = hsvTemp1Local;
                break;
            case 2:
                local = hsvTemp2Local;
                break;
            case 3:
                local = hsvTemp3Local;
                break;
            case 4:
                local = hsvTemp4Local;
                break;
            case 5:
            default:
                local = hsvTemp5Local;
                break;
        }
        float[] hsvTemp = local.get();
        if (hsvTemp == null) {
            hsvTemp = new float[3];
            local.set(hsvTemp);
        }
        return hsvTemp;
    }

    private static int getAccentColor(float[] baseHsv, int baseColor, int elementColor) {
        float[] hsvTemp3 = getTempHsv(3);
        float[] hsvTemp4 = getTempHsv(4);
        Color.colorToHSV(baseColor, hsvTemp3);
        Color.colorToHSV(elementColor, hsvTemp4);

        float dist = Math.min(1.5f * hsvTemp3[1] / baseHsv[1], 1f);

        hsvTemp3[0] = hsvTemp4[0] - hsvTemp3[0] + baseHsv[0];
        hsvTemp3[1] = hsvTemp4[1] * baseHsv[1] / hsvTemp3[1];
        hsvTemp3[2] = (hsvTemp4[2] / hsvTemp3[2] + dist - 1f) * baseHsv[2] / dist;
        if (hsvTemp3[2] < 0.3f) {
            return elementColor;
        }
        return Color.HSVToColor(255, hsvTemp3);
    }

    public static int changeColorAccent(int color) {
        ThemeAccent accent = currentTheme.getAccent(false);
        return changeColorAccent(currentTheme, accent != null ? accent.accentColor : 0, color);
    }

    private static float abs(float a) {
        return a > 0 ? a : -a;
    }

    private static float[] tmpHSV5;
    public static int changeColorAccent(float[] baseHsv, float[] accentHsv, int color, boolean isDarkTheme, int fallback) {
        if (tmpHSV5 == null) {
            tmpHSV5 = new float[3];
        }
        float[] colorHsv = tmpHSV5;
        Color.colorToHSV(color, colorHsv);

        final float diffH = Math.min(abs(colorHsv[0] - baseHsv[0]), abs(colorHsv[0] - baseHsv[0] - 360f));
        if (diffH > 30f) {
            return fallback;
        }

        float dist = Math.min(1.5f * colorHsv[1] / baseHsv[1], 1f);

        colorHsv[0] = colorHsv[0] + accentHsv[0] - baseHsv[0];
        colorHsv[1] = colorHsv[1] * accentHsv[1] / baseHsv[1];
        colorHsv[2] = colorHsv[2] * (1f - dist + dist * accentHsv[2] / baseHsv[2]);

        int newColor = Color.HSVToColor(Color.alpha(color), colorHsv);

        float origBrightness = AndroidUtilities.computePerceivedBrightness(color);
        float newBrightness = AndroidUtilities.computePerceivedBrightness(newColor);

        // We need to keep colors lighter in dark themes and darker in light themes
        boolean needRevertBrightness = isDarkTheme ? origBrightness > newBrightness : origBrightness < newBrightness;

        if (needRevertBrightness) {
            float amountOfNew = 0.6f;
            float fallbackAmount = (1f - amountOfNew) * origBrightness / newBrightness + amountOfNew;
            newColor = changeBrightness(newColor, fallbackAmount);
        }

        return newColor;
    }

    private static int changeBrightness(int color, float amount) {
        int r = (int) (Color.red(color) * amount);
        int g = (int) (Color.green(color) * amount);
        int b = (int) (Color.blue(color) * amount);

        r = r < 0 ? 0 : Math.min(r, 255);
        g = g < 0 ? 0 : Math.min(g, 255);
        b = b < 0 ? 0 : Math.min(b, 255);
        return Color.argb(Color.alpha(color), r, g, b);
    }

    public static int[] getDefaultColors() {
        return defaultColors;
    }

    public static ThemeInfo getPreviousTheme() {
        return previousTheme;
    }

    public static String getCurrentThemeName() {
        String text = currentDayTheme.getName();
        if (text.toLowerCase().endsWith(".attheme")) {
            text = text.substring(0, text.lastIndexOf('.'));
        }
        return text;
    }

    public static String getCurrentNightThemeName() {
        if (currentNightTheme == null) {
            return "";
        }
        String text = currentNightTheme.getName();
        if (text.toLowerCase().endsWith(".attheme")) {
            text = text.substring(0, text.lastIndexOf('.'));
        }
        return text;
    }

    public static ThemeInfo getCurrentTheme() {
        return currentDayTheme != null ? currentDayTheme : defaultTheme;
    }

    public static ThemeInfo getCurrentNightTheme() {
        return currentNightTheme;
    }

    public static boolean isCurrentThemeNight() {
        return currentTheme == currentNightTheme;
    }

    public static boolean isCurrentThemeDark() {
        return currentTheme != null ? currentTheme.isDark() : (currentDayTheme != null && currentDayTheme.isDark()); // EVE-PATCH: guard null currentTheme in isCurrentThemeDark
    }

    public static ThemeInfo getActiveTheme() {
        return currentTheme;
    }

    private static long getAutoNightSwitchThemeDelay() {
        long newTime = SystemClock.elapsedRealtime();
        if (Math.abs(lastThemeSwitchTime - newTime) >= LIGHT_SENSOR_THEME_SWITCH_NEAR_THRESHOLD) {
            return LIGHT_SENSOR_THEME_SWITCH_DELAY;
        }
        return LIGHT_SENSOR_THEME_SWITCH_NEAR_DELAY;
    }

    private static final float MAXIMUM_LUX_BREAKPOINT = 500.0f;

    public static File getAssetFile(String assetName) {
        File file = new File(ApplicationLoader.getFilesDirFixed(), assetName);
        long size;
        try {
            InputStream stream = ApplicationLoader.applicationContext.getAssets().open(assetName);
            size = stream.available();
            stream.close();
        } catch (Exception e) {
            size = 0;
            FileLog.e(e);
        }
        if (!file.exists() || size != 0 && file.length() != size) {
            try (InputStream in = ApplicationLoader.applicationContext.getAssets().open(assetName)) {
                AndroidUtilities.copyFile(in, file);
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        return file;
    }

    public static int getPreviewColor(SparseIntArray colors, int key) {
        int index = colors.indexOfKey(key);
        if (index >= 0) {
            return colors.valueAt(index);
        } else {
            return defaultColors[key];
        }
    }

    private static void checkIsDark(SparseIntArray colors, Theme.ThemeInfo info) {
        if (info == null || colors == null) {
            return;
        }
        if (info.isDark == ThemeInfo.UNKNOWN) {
            int averageBackgroundColor = getPreviewColor(colors, key_windowBackgroundWhite);
            averageBackgroundColor = ColorUtils.blendARGB(averageBackgroundColor, getPreviewColor(colors, key_windowBackgroundWhite), 0.5f);
            if (ColorUtils.calculateLuminance(averageBackgroundColor) < 0.5f) {
                info.isDark = ThemeInfo.DARK;
            } else {
                info.isDark = ThemeInfo.LIGHT;
            }
        }
    }

    public static void getThemeFileValuesInBackground(File file, String assetName, String[] wallpaperLink, Utilities.Callback<SparseIntArray> onDone) {
        Utilities.themeQueue.postRunnable(() -> {
            onDone.run(getThemeFileValues(file, assetName, wallpaperLink));
        });
    }

    public static SparseIntArray getThemeFileValues(File file, String assetName, String[] wallpaperLink) {
        FileInputStream stream = null;
        SparseIntArray stringMap = new SparseIntArray();
        try {
            byte[] bytes = new byte[1024];
            int currentPosition = 0;
            if (assetName != null) {
                file = getAssetFile(assetName);
            }
            stream = new FileInputStream(file);
            int idx;
            int read;
            boolean finished = false;
            int wallpaperFileOffset = -1;
            while ((read = stream.read(bytes)) != -1) {
                int previousPosition = currentPosition;
                int start = 0;
                for (int a = 0; a < read; a++) {
                    if (bytes[a] == '\n') {
                        int len = a - start + 1;
                        String line = new String(bytes, start, len - 1);
                        if (line.startsWith("WLS=")) {
                            if (wallpaperLink != null && wallpaperLink.length > 0) {
                                wallpaperLink[0] = line.substring(4);
                            }
                        } else if (line.startsWith("WPS")) {
                            wallpaperFileOffset = currentPosition + len;
                            finished = true;
                            break;
                        } else {
                            if ((idx = line.indexOf('=')) != -1) {
                                String key = line.substring(0, idx);
                                String param = line.substring(idx + 1);
                                int value;
                                if (param.length() > 0 && param.charAt(0) == '#') {
                                    try {
                                        value = Color.parseColor(param);
                                    } catch (Exception ignore) {
                                        value = Utilities.parseInt(param);
                                    }
                                } else {
                                    value = Utilities.parseInt(param);
                                }
                                int keyFromString = ThemeColors.stringKeyToInt(key);
                                if (keyFromString >= 0) {
                                    stringMap.put(keyFromString, value);
                                }
                            }
                        }
                        start += len;
                        currentPosition += len;
                    }
                }
                if (previousPosition == currentPosition) {
                    break;
                }
                stream.getChannel().position(currentPosition);
                if (finished) {
                    break;
                }
            }
            stringMap.put(key_wallpaperFileOffset, wallpaperFileOffset);
        } catch (Throwable e) {
            FileLog.e(e);
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        return stringMap;
    }

    public static void createCommonDialogResources(Context context) {
        if (dialogs_countTextPaint == null) {
            dialogs_countTextPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            dialogs_countTextPaint.setTypeface(AndroidUtilities.bold());
            dialogs_countTextPaint2 = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            dialogs_countTextPaint2.setTypeface(AndroidUtilities.bold());
            dialogs_countPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dialogs_onlineCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dialogs_tagPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        }

        dialogs_countTextPaint.setTextSize(dp(12));
        dialogs_countTextPaint2.setTextSize(dp(13));
    }

    public static void destroyResources() {

    }

    public static boolean hasGradientService() {
        return serviceBitmapShader != null;
    }

    private static int[] viewPos = new int[2];

    public static void applyServiceShaderMatrix(int w, int h, float translationX, float translationY) {
        applyServiceShaderMatrix(serviceBitmap, serviceBitmapShader, serviceBitmapMatrix, w, h, translationX, translationY);
    }

    public static void applyServiceShaderMatrix(Bitmap bitmap, BitmapShader shader, Matrix matrix, int w, int h, float translationX, float translationY) {
        if (shader == null || matrix == null) {
            return;
        }

        float bitmapWidth = bitmap.getWidth();
        float bitmapHeight = bitmap.getHeight();
        float maxScale = Math.max(w / bitmapWidth, h / bitmapHeight);
        float width = bitmapWidth * maxScale;
        float height = bitmapHeight * maxScale;
        float x = (w - width) / 2;
        float y = (h - height) / 2;

        matrix.reset();
        matrix.setTranslate(x - translationX, y - translationY);
        matrix.preScale(maxScale, maxScale);
        shader.setLocalMatrix(matrix);
    }

    private static WeakReference<Drawable> lastDrawableToBlur;
    private static Bitmap blurredBitmap;
    private static Bitmap checkBlur(Drawable d) {
        if (lastDrawableToBlur != null && lastDrawableToBlur.get() == d) {
            return blurredBitmap;
        }
        if (lastDrawableToBlur != null) {
            lastDrawableToBlur.clear();
        }
        lastDrawableToBlur = null;
        if (d == null || d.getIntrinsicWidth() == 0 || d.getIntrinsicHeight() == 0) {
            return blurredBitmap = null;
        }
        lastDrawableToBlur = new WeakReference<>(d);

        final int h = 24;
        final int w = (int) ((float) d.getIntrinsicWidth() / d.getIntrinsicHeight() * h);
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        d.setBounds(0, 0, w, h);
        final ColorFilter wasColorFilter = d.getColorFilter();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(1.3f);
        AndroidUtilities.multiplyBrightnessColorMatrix(colorMatrix, .94f);
        d.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        d.draw(new Canvas(bitmap));
        d.setColorFilter(wasColorFilter);
        Utilities.blurBitmap(bitmap, 3);
        return blurredBitmap = bitmap;
    }

    private static ColorFilter currentShareColorFilter;
    private static int currentShareColorFilterColor;
    private static ColorFilter currentShareSelectedColorFilter;
    private static  int currentShareSelectedColorFilterColor;
    public static ColorFilter getShareColorFilter(int color, boolean selected) {
        if (selected) {
            if (currentShareSelectedColorFilter == null || currentShareSelectedColorFilterColor != color) {
                currentShareSelectedColorFilterColor = color;
                currentShareSelectedColorFilter = new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY);
            }
            return currentShareSelectedColorFilter;
        } else {
            if (currentShareColorFilter == null || currentShareColorFilterColor != color) {
                currentShareColorFilterColor = color;
                currentShareColorFilter = new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY);
            }
            return currentShareColorFilter;
        }
    }

    public static Drawable getThemedDrawableByKey(Context context, int resId, int key, Theme.ResourcesProvider resourcesProvider) {
        return getThemedDrawable(context, resId, getColor(key, resourcesProvider));
    }

    public static Drawable getThemedDrawableByKey(Context context, int resId, int key) {
        return getThemedDrawable(context, resId, getColor(key));
    }

    public static Drawable getThemedDrawable(Context context, int resId, int color) {
        if (context == null) {
            return null;
        }
        Drawable drawable = context.getResources().getDrawable(resId).mutate();
        drawable.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
        return drawable;
    }

    public static int getDefaultColor(int key) {
        int value = defaultColors[key];
        if (value == 0) {
            int fallbackKey = fallbackKeys.get(key, -1);
            if (fallbackKey != -1) {
                return getDefaultColor(fallbackKey);
            }
            if (isMyMessagesBubbles(key) || key == key_chats_menuTopShadow || key == key_chats_menuTopBackground || key == key_chats_menuTopShadowCats || key == key_chat_wallpaper_gradient_to2 || key == key_chat_wallpaper_gradient_to3) {
                return 0;
            }
            return 0xffff0000;
        }
        return value;
    }

    public static boolean hasThemeKey(int key) {
        return currentColors.indexOfKey(key) >= 0;
    }

    public static void setAnimatingColor(boolean animating) {
        animatingColors = animating ? new SparseIntArray() : null;
    }

    public static boolean isAnimatingColor() {
        return animatingColors != null;
    }

    public static void setAnimatedColor(int key, int value) {
        if (animatingColors == null) {
            return;
        }
        animatingColors.put(key, value);
    }

    public static int getDefaultAccentColor(int key) {
        int index = currentColorsNoAccent.indexOfKey(key);
        if (index >= 0) {
            int color = currentColorsNoAccent.valueAt(index);
            ThemeAccent accent = currentTheme.getAccent(false);
            if (accent == null) {
                return 0;
            }
            float[] hsvTemp1 = getTempHsv(1);
            float[] hsvTemp2 = getTempHsv(2);
            Color.colorToHSV(currentTheme.accentBaseColor, hsvTemp1);
            Color.colorToHSV(accent.accentColor, hsvTemp2);
            return changeColorAccent(hsvTemp1, hsvTemp2, color, currentTheme.isDark(), color);
        }
        return 0;
    }

    public static int getNonAnimatedColor(int key) {
        return getColor(key, null, true);
    }

    public static int getColor(int key, ResourcesProvider provider) {
        if (provider != null) {
            if (provider.isCompactMode()) {
                final Integer compactColor = getCompactColor(key, provider);
                if (compactColor != null) {
                    return compactColor;
                }
            }
            return provider.getColor(key);
        }
        return getColor(key);
    }

    public static int getCurrentColor(int key) {
        return currentColors.get(key);
    }

    public static void syncFromEvePalette(android.content.Context context) { if (currentColors == null) { currentColors = new SparseIntArray(); } if (currentColorsNoAccent == null) { currentColorsNoAccent = new SparseIntArray(); } if (currentTheme == null) { currentTheme = currentDayTheme != null ? currentDayTheme : defaultTheme; } if (context == null) return; try { animatingColors = null; int[] defaults = ThemeColors.createDefaultColors(); if (defaults != null) { for (int i = 0; i < defaults.length; i++) { currentColors.put(i, defaults[i]); currentColorsNoAccent.put(i, defaults[i]); } } java.util.Map<String, Integer> baseRaw = com.eve.app.theme.TelegramThemeEngine.INSTANCE.resolveCurrentBaseRawColors(context); if (baseRaw != null) { for (java.util.Map.Entry<String, Integer> e : baseRaw.entrySet()) { int k = ThemeColors.stringKeyToInt(e.getKey()); if (k >= 0 && e.getValue() != null) { currentColorsNoAccent.put(k, e.getValue()); } } } java.util.Map<String, Integer> raw = com.eve.app.theme.TelegramThemeEngine.INSTANCE.resolveCurrentRawColors(context); if (raw != null) { for (java.util.Map.Entry<String, Integer> e : raw.entrySet()) { int k = ThemeColors.stringKeyToInt(e.getKey()); if (k >= 0 && e.getValue() != null) { currentColors.put(k, e.getValue()); } } } com.eve.app.theme.TelegramThemeEngine.ResolvedPalette palette = com.eve.app.theme.TelegramThemeEngine.INSTANCE.resolveCurrentPalette(context); if (palette != null) { if (palette.isDark() && currentNightTheme != null) { currentTheme = currentNightTheme; } else if (currentDayTheme != null) { currentTheme = currentDayTheme; } else { currentTheme = defaultTheme; } } } catch (Throwable ignored) {} if (dividerPaint == null) { dividerPaint = new Paint(); dividerPaint.setStrokeWidth(1); } dividerPaint.setColor(getColor(key_divider)); if (dividerExtraPaint != null) { dividerExtraPaint.setColor(getColor(key_divider)); } if (linkSelectionPaint != null) { linkSelectionPaint.setColor(getColor(key_chat_linkSelectBackground)); } if (checkboxSquare_checkPaint != null) { checkboxSquare_checkPaint.setColor(getColor(key_checkboxCheck)); } if (checkboxSquare_backgroundPaint != null) { checkboxSquare_backgroundPaint.setColor(getColor(key_checkboxSquareBackground)); } if (dialogs_onlineCirclePaint != null) { dialogs_onlineCirclePaint.setColor(getColor(key_chats_onlineCircle)); } if (profile_aboutTextPaint != null) { profile_aboutTextPaint.setColor(getColor(key_windowBackgroundWhiteBlackText)); } if (listSelector != null) { setDrawableColor(listSelector, getColor(key_listSelector)); } } public static int getColor(int key) { // EVE-PATCH: sync all 929 Telegram theme keys (currentColors + currentColorsNoAccent) and refresh cached paints/drawables
        return getColor(key, null, false);
    }

    public static int getColor(int key, boolean[] isDefault) {
        return getColor(key, isDefault, false);
    }

    public static int getColor(int key, boolean[] isDefault, boolean ignoreAnimation) {
        if (false) { // EVE-PATCH: decouple SharedConfig from getColor
            // compact mode swaps some message colors for its own keys, see getCompactColor
            final Integer compactColor = getCompactColor(key, null);
            if (compactColor != null) {
                return compactColor;
            }
        }
        if (!ignoreAnimation && animatingColors != null) {
            int index = animatingColors.indexOfKey(key);
            if (index >= 0) {
                return animatingColors.valueAt(index);
            }
        }
        return getColorFromTable(currentColors, key, isDefault);
    }

    /**
     * {@link #getColor(int)} without the animated colors, on the given color table: also used while a new table is computed
     * and not published yet ({@link ThemeAccent#fillAccentColors}).
     */
    private static int getColorFromTable(SparseIntArray currentColors, int key, boolean[] isDefault) {
        if (serviceBitmapShader != null && (key_chat_serviceText == key || key_chat_serviceLink == key || key_chat_serviceIcon == key
                || key_chat_stickerReplyLine == key || key_chat_stickerReplyNameText == key || key_chat_stickerReplyMessageText == key)) {
            return 0xffffffff;
        }
        if (currentTheme == defaultTheme) {
            boolean useDefault;
            if (isMyMessagesBubbles(key)) {
                useDefault = currentTheme.isDefaultMyMessagesBubbles();
            } else if (isMyMessages(key)) {
                useDefault = currentTheme.isDefaultMyMessages();
            } else if (key_chat_wallpaper == key || key_chat_wallpaper_gradient_to1 == key || key_chat_wallpaper_gradient_to2 == key || key_chat_wallpaper_gradient_to3 == key) {
                useDefault = false;
            } else {
                useDefault = currentTheme.isDefaultMainAccent();
            }
            if (useDefault) {
                if (key == key_chat_serviceBackground) {
                    return serviceMessageColor;
                } else if (key == key_chat_serviceBackgroundSelected) {
                    return serviceSelectedMessageColor;
                }
                return getDefaultColor(key);
            }
        }
        int index = currentColors.indexOfKey(key);
        int color;
        if (index < 0) {
            int fallbackKey = fallbackKeys.get(key, -1);
            if (fallbackKey != -1) {
                int fallbackIndex = currentColors.indexOfKey(fallbackKey);
                if (fallbackIndex >= 0) {
                    return currentColors.valueAt(fallbackIndex);
                }
            }

            if (isDefault != null) {
                isDefault[0] = true;
            }
            if (key == key_chat_serviceBackground) {
                return serviceMessageColor;
            } else if (key == key_chat_serviceBackgroundSelected) {
                return serviceSelectedMessageColor;
            }
            return getDefaultColor(key);
        } else {
            color = currentColors.valueAt(index);
        }
        if (key_windowBackgroundWhite == key || key_windowBackgroundGray == key || key_actionBarDefault == key || key_actionBarDefaultArchived == key) {
            color |= 0xff000000;
        }
        return color;
    }

    private static boolean isMyMessagesBubbles(int key) {
        return key >= myMessagesBubblesStartIndex && key < myMessagesBubblesEndIndex;
    }


    private static boolean isMyMessages(int key) {
        return key >= myMessagesStartIndex && key < myMessagesEndIndex;
    }

    public static void setDefaultColor(int key, int color) {
        defaultColors[key] = color;
    }

    public static void setDrawableColor(Drawable drawable, int color) {
        if (drawable == null) {
            return;
        }
        if (drawable instanceof ShapeDrawable) { // EVE-PATCH: keep ShapeDrawable branch, remove unported drawables
            ((ShapeDrawable) drawable).getPaint().setColor(color); // EVE-PATCH: use ShapeDrawable paint
        } else {
            drawable.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
        }
    }

    public static void setDrawableColorByKey(Drawable drawable, int key) {
        setDrawableColor(drawable, getColor(key));
    }

    public static int getCompactOutBubbleColor(ResourcesProvider resourcesProvider) {
        return ColorUtils.compositeColors(getColor(key_chat_compact_outBackground, resourcesProvider), getColor(key_chat_compact_background, resourcesProvider));
    }

    /**
     * Compact mode replaces some outgoing message colors with its own keys, blended to be opaque.
     * Returns null when compact mode is off or the key is not replaced.
     */
    private static final int COMPACT_DIRECT = 1, COMPACT_OVER_OUT_BUBBLE = 2, COMPACT_OVER_BACKGROUND = 3;
    // regular key -> (mode << 16) | compact key, 0 when compact mode doesn't replace it
    private static int[] compactColorMap;

    private static void mapCompact(int mode, int compactKey, int... keys) {
        for (int key : keys) {
            compactColorMap[key] = (mode << 16) | compactKey;
        }
    }

    private static int[] getCompactColorMap() {
        if (compactColorMap == null) {
            compactColorMap = new int[colorsCount];
            mapCompact(COMPACT_OVER_OUT_BUBBLE, key_chat_compact_outTimeText, key_chat_outTimeText, key_chat_outTimeSelectedText, key_chat_outViews, key_chat_outViewsSelected);
            mapCompact(COMPACT_OVER_BACKGROUND, key_chat_compact_inTimeText, key_chat_inTimeText, key_chat_inTimeSelectedText, key_chat_inViews, key_chat_inViewsSelected);
            mapCompact(COMPACT_DIRECT, key_chat_compact_outCheck, key_chat_outSentCheck, key_chat_outSentCheckSelected, key_chat_outSentCheckRead, key_chat_outSentCheckReadSelected, key_chat_outSentClock, key_chat_outSentClockSelected);
            mapCompact(COMPACT_DIRECT, key_chat_compact_outReplyLine, key_chat_outReplyLine, key_chat_outReplyLine2);
            mapCompact(COMPACT_DIRECT, key_chat_compact_outReplyText, key_chat_outReplyNameText, key_chat_outReplyMessageText, key_chat_outReplyMediaMessageText, key_chat_outReplyMediaMessageSelectedText);
            // outgoing message accents: neutral instead of the theme's green
            mapCompact(COMPACT_DIRECT, key_chat_compact_outAccent, key_chat_outInstant, key_chat_outInstantSelected, key_chat_outPreviewInstantText, key_chat_outForwardedNameText, key_chat_outViaBotNameText, key_chat_outPsaNameText, key_chat_outSiteNameText, key_chat_outContactNameText, key_chat_outAudioTitleText, key_chat_outFileNameText, key_chat_outLocationIcon, key_chat_outMenu, key_chat_outMenuSelected, key_chat_outQuote, key_chat_outPreviewLine, key_chat_outAdminText, key_chat_outAdminSelectedText);
            mapCompact(COMPACT_DIRECT, key_chat_compact_outAccentFill, key_chat_outLoader, key_chat_outLoaderSelected, key_chat_outContactBackground, key_chat_outAudioSeekbarFill, key_chat_outVoiceSeekbarFill);
            mapCompact(COMPACT_DIRECT, key_chat_compact_outAccentIcon, key_chat_outMediaIcon, key_chat_outMediaIconSelected, key_chat_outContactIcon, key_chat_outAudioProgress, key_chat_outAudioSelectedProgress);
            mapCompact(COMPACT_OVER_OUT_BUBBLE, key_chat_compact_outSecondaryText, key_chat_outFileInfoText, key_chat_outFileInfoSelectedText, key_chat_outVenueInfoText, key_chat_outVenueInfoSelectedText, key_chat_outAudioDurationText, key_chat_outAudioDurationSelectedText);
            mapCompact(COMPACT_OVER_OUT_BUBBLE, key_chat_compact_outAccentLight, key_chat_outAudioSeekbar, key_chat_outAudioSeekbarSelected, key_chat_outVoiceSeekbar, key_chat_outVoiceSeekbarSelected, key_chat_outFileProgress, key_chat_outFileProgressSelected, key_chat_outFileBackground, key_chat_outFileBackgroundSelected, key_chat_outTableBackground, key_chat_outArticleCodeScrollbarBackground);
            mapCompact(COMPACT_OVER_OUT_BUBBLE, key_chat_compact_outAccentBorder, key_chat_outTableBorder, key_chat_outDivider);
            mapCompact(COMPACT_DIRECT, key_chat_compact_inAudioDuration, key_chat_inAudioDurationText, key_chat_inAudioDurationSelectedText);
            mapCompact(COMPACT_DIRECT, key_chat_compact_inWaveform, key_chat_inVoiceSeekbarFill);
            mapCompact(COMPACT_DIRECT, key_chat_compact_inWaveformBackground, key_chat_inVoiceSeekbar, key_chat_inVoiceSeekbarSelected);
            // the voice/round transcribe button is tinted with it; reaction chips read compact keys directly
            mapCompact(COMPACT_DIRECT, key_chat_compact_inTranscribeButton, key_chat_inReactionButtonBackground);
            // names, via bots and replies next to stickers sit on the light action pill, not on a wallpaper
            mapCompact(COMPACT_DIRECT, key_chat_compact_actionText, key_chat_stickerNameText, key_chat_stickerViaBotNameText, key_chat_stickerReplyNameText, key_chat_stickerReplyMessageText);
        }
        return compactColorMap;
    }

    public static Integer getCompactColor(int key, ResourcesProvider resourcesProvider) {
        if (!isCompactMode(resourcesProvider) || key < 0 || key >= colorsCount) {
            return null;
        }
        final int entry = getCompactColorMap()[key];
        if (entry == 0) {
            return null;
        }
        final int compactKey = entry & 0xFFFF;
        switch (entry >> 16) {
            case COMPACT_OVER_OUT_BUBBLE:
                return ColorUtils.compositeColors(getColor(compactKey, resourcesProvider), getCompactOutBubbleColor(resourcesProvider));
            case COMPACT_OVER_BACKGROUND:
                return ColorUtils.compositeColors(getColor(compactKey, resourcesProvider), getColor(key_chat_compact_background, resourcesProvider));
            default:
                return getColor(compactKey, resourcesProvider);
        }
    }

    public static void setEmojiDrawableColor(Drawable drawable, int color, boolean selected) {
        if (drawable instanceof StateListDrawable) {
            try {
                Drawable state;
                if (selected) {
                    state = getStateDrawable(drawable, 0);
                } else {
                    state = getStateDrawable(drawable, 1);
                }
                if (state instanceof ShapeDrawable) {
                    ((ShapeDrawable) state).getPaint().setColor(color);
                } else {
                    state.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
                }
            } catch (Throwable ignore) {

            }
        }
    }

    public static boolean setSelectorDrawableColor(Drawable drawable, int color, boolean selected) {
        boolean changed = false;
        if (drawable instanceof StateListDrawable) {
            try {
                Drawable state;
                if (selected) {
                    state = getStateDrawable(drawable, 0);
                    if (state instanceof ShapeDrawable) {
                        changed = ((ShapeDrawable) state).getPaint().getColor() != color || changed;
                        ((ShapeDrawable) state).getPaint().setColor(color);
                    } else {
                        state.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
                    }
                    state = getStateDrawable(drawable, 1);
                } else {
                    state = getStateDrawable(drawable, 2);
                }
                if (state instanceof ShapeDrawable) {
                    changed = ((ShapeDrawable) state).getPaint().getColor() != color || changed;
                    ((ShapeDrawable) state).getPaint().setColor(color);
                } else {
                    state.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
                }
            } catch (Throwable ignore) {

            }
        } else if (drawable instanceof RippleDrawable) {
            RippleDrawable rippleDrawable = (RippleDrawable) drawable;
            if (selected) {
                rippleDrawable.setColor(new ColorStateList(
                    new int[][]{StateSet.WILD_CARD},
                    new int[]{color}
                ));
            } else {
                if (rippleDrawable.getNumberOfLayers() > 0) {
                    Drawable drawable1 = rippleDrawable.getDrawable(0);
                    if (drawable1 instanceof ShapeDrawable) {
                        changed = ((ShapeDrawable) drawable1).getPaint().getColor() != color || changed;
                        ((ShapeDrawable) drawable1).getPaint().setColor(color);
                    } else {
                        drawable1.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
                    }
                }
            }
        }
        return changed;
    }

    public static int getServiceMessageColor() {
        int index = currentColors.indexOfKey(key_chat_serviceBackground);
        if (index >= 0) {
            return currentColors.valueAt(index);
        } else {
            return serviceMessageColor;
        }
    }


    public static boolean isPatternWallpaper() {
        String selectedBgSlug = ""; // EVE-PATCH: decouple getSelectedBackgroundSlug
        return isPatternWallpaper || "CJz3BZ6YGEYBAAAABboWp6SAv04".equals(selectedBgSlug) || "qeZWES8rGVIEAAAARfWlK1lnfiI".equals(selectedBgSlug);
    }

    public static SparseIntArray getFallbackKeys() {
        return fallbackKeys;
    }

    public static int getFallbackKey(int key) {
        return fallbackKeys.get(key);
    }

    public static Map<String, Drawable> getThemeDrawablesMap() {
        return defaultChatDrawables;
    }

    public static Drawable getThemeDrawable(String drawableKey) {
        return defaultChatDrawables.get(drawableKey);
    }

    public static Drawable getThemeDrawable(String key, Theme.ResourcesProvider resourcesProvider) {
        Drawable drawable = resourcesProvider != null ? resourcesProvider.getDrawable(key) : null;
        return drawable != null ? drawable : defaultChatDrawables.get(key);
    }

    public static int getThemeDrawableColorKey(String drawableKey) {
        return defaultChatDrawableColorKeys.get(drawableKey);
    }

    public static Map<String, Paint> getThemePaintsMap() {
        return defaultChatPaints;
    }

    public static Paint getThemePaint(String paintKey) {
        if (Objects.equals(paintKey, Theme.key_paint_divider)) {
            return dividerPaint;
        }
        return defaultChatPaints.get(paintKey);
    }

    public static int getThemePaintColorKey(String paintKey) {
        return defaultChatPaintColors.get(paintKey);
    }

    private static void addChatDrawable(String key, Drawable drawable, int colorKey) {
        defaultChatDrawables.put(key, drawable);
        defaultChatDrawableColorKeys.put(key, colorKey);
    }

    private static void addChatPaint(String key, Paint paint, int colorKey) {
        defaultChatPaints.put(key, paint);
        defaultChatPaintColors.put(key, colorKey);
    }

    public static boolean isCurrentThemeDay() {
        return !getActiveTheme().isDark();
    }

    public static boolean isHome(ThemeAccent accent) {
        if (accent.parentTheme != null) {
            if (accent.parentTheme.getKey().equals("Blue") && accent.id == 99) {
                return true;
            }
            if (accent.parentTheme.getKey().equals("Day") && accent.id == 9) {
                return true;
            }
            if ((accent.parentTheme.getKey().equals("Night") || accent.parentTheme.getKey().equals("Dark Blue")) && accent.id == 0) {
                return true;
            }
        }
        return false;
    }

    public enum IvButtonColors {
        DEFAULT(
                Theme.key_chat_msgIvButtonDefaultIn, Theme.key_chat_msgIvButtonDefaultInPressed, Theme.key_chat_msgIvButtonDefaultInText,
                Theme.key_chat_msgIvButtonDefaultOut, Theme.key_chat_msgIvButtonDefaultOutPressed, Theme.key_chat_msgIvButtonDefaultOutText
        ),
        PRIMARY(
                Theme.key_chat_msgIvButtonPrimaryIn, Theme.key_chat_msgIvButtonPrimaryInPressed, Theme.key_chat_msgIvButtonPrimaryInText,
                Theme.key_chat_msgIvButtonPrimaryOut, Theme.key_chat_msgIvButtonPrimaryOutPressed, Theme.key_chat_msgIvButtonPrimaryOutText
        ),
        DANGER(
                Theme.key_chat_msgIvButtonDangerIn, Theme.key_chat_msgIvButtonDangerInPressed, Theme.key_chat_msgIvButtonDangerInText,
                Theme.key_chat_msgIvButtonDangerOut, Theme.key_chat_msgIvButtonDangerOutPressed, Theme.key_chat_msgIvButtonDangerOutText
        ),
        SUCCESS(
                Theme.key_chat_msgIvButtonSuccessIn, Theme.key_chat_msgIvButtonSuccessInPressed, Theme.key_chat_msgIvButtonSuccessInText,
                Theme.key_chat_msgIvButtonSuccessOut, Theme.key_chat_msgIvButtonSuccessOutPressed, Theme.key_chat_msgIvButtonSuccessOutText
        ),
        DEFAULT_IN_TEXT(
                Theme.key_chat_msgIvButtonDefaultInlineIn, Theme.key_chat_msgIvButtonDefaultInlineInPressed, Theme.key_chat_msgIvButtonDefaultInlineInText,
                Theme.key_chat_msgIvButtonDefaultInlineOut, Theme.key_chat_msgIvButtonDefaultInlineOutPressed, Theme.key_chat_msgIvButtonDefaultInlineOutText
        );

        private final int backgroundIn, backgroundInPressed, textIn;
        private final int backgroundOut, backgroundOutPressed, textOut;

        IvButtonColors(int backgroundIn, int backgroundInPressed, int textIn, int backgroundOut, int backgroundOutPressed, int textOut) {
            this.backgroundIn = backgroundIn;
            this.backgroundInPressed = backgroundInPressed;
            this.textIn = textIn;
            this.backgroundOut = backgroundOut;
            this.backgroundOutPressed = backgroundOutPressed;
            this.textOut = textOut;
        }

        public int getBackgroundKey(boolean out) {
            return out ? backgroundOut : backgroundIn;
        }

        public int getBackgroundPressedKey(boolean out) {
            return out ? backgroundOutPressed : backgroundInPressed;
        }

        public int getTextKey(boolean out) {
            return out ? textOut : textIn;
        }

    }

    public interface Colorable {
        public void updateColors();
        public default void addColorKeys(HashSet<Integer> colors) {}
    }


    private static int themeGenerationId;

    public static void invalidateTheme() {
        themeGenerationId++;
    }

    /** Changes on every theme color update: an explicit {@link #invalidateTheme()} or a {@link ThemeBind} update pass. */
    public static int getThemeGenerationId() {
        return themeGenerationId; // EVE-PATCH: decouple ThemeBind
    }


    private static final Paint PAINT_FILLING = new Paint(Paint.ANTI_ALIAS_FLAG);
    public static final Paint PAINT_CLEAR = new Paint(Paint.ANTI_ALIAS_FLAG); static {
        PAINT_CLEAR.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    public static void createCommonResources(Context context) { if (dividerPaint == null) { dividerPaint = new Paint(); dividerPaint.setStrokeWidth(1); } } public static Paint fillingPaint(int color) { // EVE-PATCH: provide lightweight createCommonResources for cells
        if (PAINT_FILLING.getColor() != color) {
            PAINT_FILLING.setColor(color);
        }
        return PAINT_FILLING;
    }


    /* DEBUG */

    public static Paint DEBUG_RED = new Paint(); static { DEBUG_RED.setColor(0xffff0000); }
    public static Paint DEBUG_BLUE = new Paint(); static { DEBUG_BLUE.setColor(0xff0000ff); }
    public static Paint DEBUG_GREEN_40 = new Paint(); static { DEBUG_GREEN_40.setColor(0x4000FF00); }
    public static Paint DEBUG_GREEN_B0 = new Paint(); static { DEBUG_GREEN_B0.setColor(0xB000FF00); }
    public static Paint DEBUG_RED_STROKE = new Paint(); static {
        DEBUG_RED_STROKE.setColor(0xffff0000);
        DEBUG_RED_STROKE.setStrokeWidth(2);
        DEBUG_RED_STROKE.setStyle(Paint.Style.STROKE);
    }
    public static Paint DEBUG_GREEN_STROKE = new Paint(); static {
        DEBUG_GREEN_STROKE.setColor(0xff00ff00);
        DEBUG_GREEN_STROKE.setStrokeWidth(2);
        DEBUG_GREEN_STROKE.setStyle(Paint.Style.STROKE);
    }
}
