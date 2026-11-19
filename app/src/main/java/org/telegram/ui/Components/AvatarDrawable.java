/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.StaticLayout;
import android.text.TextPaint;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;


public class AvatarDrawable extends Drawable {

    private TextPaint namePaint;
    private boolean hasGradient;
    private boolean hasAdvancedGradient;
    private int color, color2;
    private GradientTools advancedGradient;
    private boolean needApplyColorAccent;
    private StaticLayout textLayout;
    private float textWidth;
    private float textHeight;
    private float textLeft;
    private boolean isProfile;
    private boolean drawDeleted;
    private int avatarType;
    private float archivedAvatarProgress;
    private float scaleSize = 1f;
    private StringBuilder stringBuilder = new StringBuilder(5);
    private int roundRadius = -1;

    private int gradientTop, gradientBottom;
    private int gradientColor1, gradientColor2;
    private LinearGradient gradient;

    private int gradientTop2, gradientBottom2;
    private int gradientColor21, gradientColor22;
    private LinearGradient gradient2;
    private boolean drawAvatarBackground = true;
    private boolean rotate45Background = false;

    public static final int AVATAR_TYPE_NORMAL = 0;
    public static final int AVATAR_TYPE_SAVED = 1;
    public static final int AVATAR_TYPE_ARCHIVED = 2;
    public static final int AVATAR_TYPE_SHARES = 3;
    public static final int AVATAR_TYPE_REPLIES = 12;

    public static final int AVATAR_TYPE_FILTER_CONTACTS = 4;
    public static final int AVATAR_TYPE_FILTER_NON_CONTACTS = 5;
    public static final int AVATAR_TYPE_FILTER_GROUPS = 6;
    public static final int AVATAR_TYPE_FILTER_CHANNELS = 7;
    public static final int AVATAR_TYPE_FILTER_BOTS = 8;
    public static final int AVATAR_TYPE_FILTER_MUTED = 9;
    public static final int AVATAR_TYPE_FILTER_READ = 10;
    public static final int AVATAR_TYPE_FILTER_ARCHIVED = 11;
    public static final int AVATAR_TYPE_REGISTER = 13;
    public static final int AVATAR_TYPE_OTHER_CHATS = 14;
    public static final int AVATAR_TYPE_CLOSE_FRIENDS = 15;
    public static final int AVATAR_TYPE_GIFT = 16;
    public static final int AVATAR_TYPE_COUNTRY = 17;
    public static final int AVATAR_TYPE_UNCLAIMED = 18;
    public static final int AVATAR_TYPE_TO_BE_DISTRIBUTED = 19;
    public static final int AVATAR_TYPE_STORY = 20;
    public static final int AVATAR_TYPE_ANONYMOUS = 21;
    public static final int AVATAR_TYPE_MY_NOTES = 22;
    public static final int AVATAR_TYPE_EXISTING_CHATS = 23;
    public static final int AVATAR_TYPE_NEW_CHATS = 24;
    public static final int AVATAR_TYPE_PREMIUM = 25;
    public static final int AVATAR_TYPE_STARS = 26;
    public static final int AVATAR_TYPE_SUGGESTION = 27;

    /**
     * Matches {@link org.telegram.ui.Components.AvatarConstructorFragment#defaultColors}
     * but reordered to preserve color tints.
     */
    public static final int[][] advancedGradients = new int[][]{
            new int[]{0xFFF64884, 0xFFEF5B41, 0xFFF6A730, 0xFFFF7742},
            new int[]{0xFFF5694E, 0xFFF5772C, 0xFFFFD412, 0xFFFFA743},
            new int[]{0xFF837CFF, 0xFFB063FF, 0xFFFF72A9, 0xFFE269FF},
            new int[]{0xFF09D260, 0xFF5EDC40, 0xFFC1E526, 0xFF80DF2B},
            new int[]{0xFF5EB6FB, 0xFF1FCEEB, 0xFF45F7B7, 0xFF1FF1D9},
            new int[]{0xFF4D8DFF, 0xFF2BBFFF, 0xFF20E2CD, 0xFF0EE1F1},
            new int[]{0xFFF94BA0, 0xFFFB5C80, 0xFFFFB23A, 0xFFFE7E62},
    };

    private int alpha = 255;
    private Theme.ResourcesProvider resourcesProvider;
    private boolean invalidateTextLayout;

    public AvatarDrawable() {
        this((Theme.ResourcesProvider) null);
    }

    public AvatarDrawable(Theme.ResourcesProvider resourcesProvider) {
        super();
        this.resourcesProvider = resourcesProvider;
        namePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        namePaint.setTypeface(AndroidUtilities.bold());
        namePaint.setTextSize(dp(18));
    }

    public void setDrawAvatarBackground(boolean drawAvatarBackground) {
        this.drawAvatarBackground = drawAvatarBackground;
    }

    public void setProfile(boolean value) {
        isProfile = value;
    }

    public static int getPeerColorIndex(int color) {
        float[] tempHSV = Theme.getTempHsv(5);
        Color.colorToHSV(color, tempHSV);
        final int hue = (int) tempHSV[0];
        if (hue >= 345 || hue < 29) return 0; // red
        if (hue < 67) return 1; // orange
        if (hue < 140) return 3; // green
        if (hue < 199) return 4; // cyan
        if (hue < 234) return 5; // blue
        if (hue < 301) return 2; // violet
        return 6; // pink
    }

    public static int getColorIndex(long id) {
        return (int) Math.abs(id % Theme.keys_avatar_background.length);
    }

    public static int getColorForId(long id) {
        return Theme.getColor(Theme.keys_avatar_background[getColorIndex(id)]);
    }

    public static int getButtonColorForId(long id, Theme.ResourcesProvider resourcesProvider) {
        return Theme.getColor(Theme.key_avatar_actionBarSelectorBlue, resourcesProvider);
    }

    public static int getIconColorForId(long id, Theme.ResourcesProvider resourcesProvider) {
        return Theme.getColor(Theme.key_avatar_actionBarIconBlue, resourcesProvider);
    }

    public static int getProfileColorForId(long id, Theme.ResourcesProvider resourcesProvider) {
        return Theme.getColor(Theme.keys_avatar_background[getColorIndex(id)], resourcesProvider);
    }

    public static int getProfileTextColorForId(long id, Theme.ResourcesProvider resourcesProvider) {
        return Theme.getColor(Theme.key_avatar_subtitleInProfileBlue, resourcesProvider);
    }

    public static int getProfileBackColorForId(long id, Theme.ResourcesProvider resourcesProvider) {
        return Theme.getColor(Theme.key_windowBackgroundGray, resourcesProvider);
    }

    public static String colorName(int color) {
        final int[] resIds = new int[] { R.string.ColorRed, R.string.ColorOrange, R.string.ColorViolet, R.string.ColorGreen, R.string.ColorCyan, R.string.ColorBlue, R.string.ColorPink };
        return LocaleController.getString(resIds[color % resIds.length]);
    }

    public static int getNameColorNameForId(long id) {
        return Theme.keys_avatar_nameInMessage[getColorIndex(id)];
    }

    public void setScaleSize(float value) {
        scaleSize = value;
    }

    public void setAvatarType(int value) {
        avatarType = value;
        rotate45Background = false;
        hasAdvancedGradient = false;
        hasGradient = false;
        if (avatarType == AVATAR_TYPE_REGISTER) {
            color = color2 = Theme.getColor(Theme.key_chats_actionBackground);
        } else if (avatarType == AVATAR_TYPE_ARCHIVED) {
            color = color2 = getThemedColor(Theme.key_avatar_backgroundArchivedHidden);
        } else if (avatarType == AVATAR_TYPE_SUGGESTION || avatarType == AVATAR_TYPE_REPLIES || avatarType == AVATAR_TYPE_SAVED || avatarType == AVATAR_TYPE_OTHER_CHATS) {
            hasGradient = true;
            color = getThemedColor(Theme.key_avatar_backgroundSaved);
            color2 = getThemedColor(Theme.key_avatar_background2Saved);
        } else if (avatarType == AVATAR_TYPE_STORY) {
            rotate45Background = true;
            hasGradient = true;
            color = getThemedColor(Theme.key_stories_circle1);
            color2 = getThemedColor(Theme.key_stories_circle2);
        } else if (avatarType == AVATAR_TYPE_SHARES) {
            hasGradient = true;
            color = getThemedColor(Theme.keys_avatar_background[getColorIndex(5)]);
            color2 = getThemedColor(Theme.keys_avatar_background2[getColorIndex(5)]);
        } else if (avatarType == AVATAR_TYPE_PREMIUM) {
            hasGradient = true;
            color = getThemedColor(Theme.keys_avatar_background[getColorIndex(2)]);
            color2 = getThemedColor(Theme.keys_avatar_background2[getColorIndex(2)]);
        } else if (avatarType == AVATAR_TYPE_STARS) {
            hasGradient = true;
            color = getThemedColor(Theme.keys_avatar_background[getColorIndex(1)]);
            color2 = getThemedColor(Theme.keys_avatar_background2[getColorIndex(1)]);
        } else if (avatarType == AVATAR_TYPE_FILTER_CONTACTS) {
            hasGradient = true;
            color = getThemedColor(Theme.keys_avatar_background[getColorIndex(5)]);
            color2 = getThemedColor(Theme.keys_avatar_background2[getColorIndex(5)]);
        } else if (avatarType == AVATAR_TYPE_FILTER_NON_CONTACTS) {
            hasGradient = true;
            color = getThemedColor(Theme.keys_avatar_background[getColorIndex(4)]);
            color2 = getThemedColor(Theme.keys_avatar_background2[getColorIndex(4)]);
        } else if (avatarType == AVATAR_TYPE_FILTER_GROUPS || avatarType == AVATAR_TYPE_EXISTING_CHATS) {
            hasGradient = true;
            color = getThemedColor(Theme.keys_avatar_background[getColorIndex(3)]);
            color2 = getThemedColor(Theme.keys_avatar_background2[getColorIndex(3)]);
        } else if (avatarType == AVATAR_TYPE_FILTER_CHANNELS || avatarType == AVATAR_TYPE_NEW_CHATS) {
            hasGradient = true;
            color = getThemedColor(Theme.keys_avatar_background[getColorIndex(1)]);
            color2 = getThemedColor(Theme.keys_avatar_background2[getColorIndex(1)]);
        } else if (avatarType == AVATAR_TYPE_FILTER_BOTS) {
            hasGradient = true;
            color = getThemedColor(Theme.keys_avatar_background[getColorIndex(0)]);
            color2 = getThemedColor(Theme.keys_avatar_background2[getColorIndex(0)]);
        } else if (avatarType == AVATAR_TYPE_FILTER_MUTED) {
            hasGradient = true;
            color = getThemedColor(Theme.keys_avatar_background[getColorIndex(6)]);
            color2 = getThemedColor(Theme.keys_avatar_background2[getColorIndex(6)]);
        } else if (avatarType == AVATAR_TYPE_FILTER_READ) {
            hasGradient = true;
            color = getThemedColor(Theme.keys_avatar_background[getColorIndex(5)]);
            color2 = getThemedColor(Theme.keys_avatar_background2[getColorIndex(5)]);
        } else if (avatarType == AVATAR_TYPE_COUNTRY) {
            hasGradient = true;
            color = getThemedColor(Theme.keys_avatar_background[getColorIndex(5)]);
            color2 = getThemedColor(Theme.keys_avatar_background2[getColorIndex(5)]);
        } else if (avatarType == AVATAR_TYPE_ANONYMOUS) {
            hasAdvancedGradient = true;
            if (advancedGradient == null) {
                advancedGradient = new GradientTools();
            }
            advancedGradient.setColors(0xFF837CFF, 0xFFB063FF, 0xFFFF72A9, 0xFFE269FF);
        } else if (avatarType == AVATAR_TYPE_MY_NOTES) {
            hasAdvancedGradient = true;
            if (advancedGradient == null) {
                advancedGradient = new GradientTools();
            }
            advancedGradient.setColors(0xFF4D8DFF, 0xFF2BBFFF, 0xFF20E2CD, 0xFF0EE1F1);
        } else {
            hasGradient = true;
            color = getThemedColor(Theme.keys_avatar_background[getColorIndex(4)]);
            color2 = getThemedColor(Theme.keys_avatar_background2[getColorIndex(4)]);
        }
        needApplyColorAccent = avatarType != AVATAR_TYPE_ARCHIVED && avatarType != AVATAR_TYPE_SAVED && avatarType != AVATAR_TYPE_STORY && avatarType != AVATAR_TYPE_ANONYMOUS && avatarType != AVATAR_TYPE_SUGGESTION && avatarType != AVATAR_TYPE_REPLIES && avatarType != AVATAR_TYPE_OTHER_CHATS;
        avatarTypeColorsThemed = avatarType == AVATAR_TYPE_REGISTER || avatarType == AVATAR_TYPE_ARCHIVED || avatarType == AVATAR_TYPE_SUGGESTION || avatarType == AVATAR_TYPE_REPLIES || avatarType == AVATAR_TYPE_SAVED || avatarType == AVATAR_TYPE_OTHER_CHATS || avatarType == AVATAR_TYPE_STORY;
        avatarTypeColor = color;
        avatarTypeColor2 = color2;
    }

    // colors picked by id in setInfo: re-read from the theme after a theme change (on draw), while still shown
    private boolean idColorsThemed;
    private int idColorIndex, idColor, idColor2, idColorsGeneration;

    private boolean avatarTypeColorsThemed;
    private int avatarTypeColor, avatarTypeColor2, avatarTypeColorsGeneration;

    // re-reads the theme colors set by setAvatarType after a theme change, unless they were replaced since
    private void refreshAvatarTypeColors() {
        try {
            if (color != avatarTypeColor || color2 != avatarTypeColor2) {
                avatarTypeColorsThemed = false;
                return;
            }
            final int c, c2;
            if (avatarType == AVATAR_TYPE_REGISTER) {
                c = c2 = Theme.getColor(Theme.key_chats_actionBackground);
            } else if (avatarType == AVATAR_TYPE_ARCHIVED) {
                c = c2 = getThemedColor(Theme.key_avatar_backgroundArchivedHidden);
            } else if (avatarType == AVATAR_TYPE_SUGGESTION || avatarType == AVATAR_TYPE_REPLIES || avatarType == AVATAR_TYPE_SAVED || avatarType == AVATAR_TYPE_OTHER_CHATS) {
                c = getThemedColor(Theme.key_avatar_backgroundSaved);
                c2 = getThemedColor(Theme.key_avatar_background2Saved);
            } else if (avatarType == AVATAR_TYPE_STORY) {
                c = getThemedColor(Theme.key_stories_circle1);
                c2 = getThemedColor(Theme.key_stories_circle2);
            } else {
                avatarTypeColorsThemed = false;
                return;
            }
            color = avatarTypeColor = c;
            color2 = avatarTypeColor2 = c2;
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public void setArchivedAvatarHiddenProgress(float progress) {
        archivedAvatarProgress = progress;
    }

    public int getAvatarType() {
        return avatarType;
    }

    public void setColor(int value) {
        hasGradient = false;
        hasAdvancedGradient = false;
        color = color2 = value;
        needApplyColorAccent = false;
    }

    public void setColor(int value, int value2) {
        hasGradient = true;
        hasAdvancedGradient = false;
        color = value;
        color2 = value2;
        needApplyColorAccent = false;
    }

    public void setTextSize(int size) {
        namePaint.setTextSize(size);
    }

    public void setInfo(long id, String firstName, String lastName) {
        setInfo(id, firstName, lastName, null, null, null, false); // EVE-PATCH: call 7-arg setInfo directly
    }

    public int getColor() {
        return needApplyColorAccent ? Theme.changeColorAccent(color) : color;
    }

    public int getColor2() {
        return needApplyColorAccent ? Theme.changeColorAccent(color2) : color2;
    }

    private static String takeFirstCharacter(String text) {
        return text.substring(0, text.offsetByCodePoints(0, Math.min(text.codePointCount(0, text.length()), 1)));
    }

    public void setInfo(long id) {
        invalidateTextLayout = true;
        hasGradient = true;
        hasAdvancedGradient = false;
        color = getThemedColor(Theme.keys_avatar_background[getColorIndex(id)]);
        color2 = getThemedColor(Theme.keys_avatar_background2[getColorIndex(id)]);
        avatarType = AVATAR_TYPE_NORMAL;
        drawDeleted = false;
        getAvatarSymbols("", "", "", stringBuilder);
    }

    public void setInfo(long id, String firstName, String lastName, String custom) {
        setInfo(id, firstName, lastName, custom, null, null, false); // EVE-PATCH: call 7-arg setInfo directly
    }

    public void setInfo(long id, String firstName, String lastName, String custom, Integer customColor, Object profileColor, boolean advancedGradient) { // EVE-PATCH: remove MessagesController.PeerColor parameter
        invalidateTextLayout = true;
        if (advancedGradient) {
            hasGradient = false;
            hasAdvancedGradient = true;
            if (this.advancedGradient == null) {
                this.advancedGradient = new GradientTools();
            }
        } else {
            hasGradient = true;
            hasAdvancedGradient = false;
        }

        if (false) { // EVE-PATCH: disable PeerColor branch without MessagesController
            if (advancedGradient) {
                int[] gradient = advancedGradients[0]; // EVE-PATCH: use dummy gradient in dead branch
                this.advancedGradient.setColors(gradient[0], gradient[1], gradient[2], gradient[3]);
            } else {
            }
        } else if (false) { // EVE-PATCH: disable customColor PeerColor branch
        } else {
            if (advancedGradient) {
                int[] gradient = advancedGradients[getColorIndex(id)];
                this.advancedGradient.setColors(gradient[0], gradient[1], gradient[2], gradient[3]);
            } else {
                color = getThemedColor(Theme.keys_avatar_background[getColorIndex(id)]);
                color2 = getThemedColor(Theme.keys_avatar_background2[getColorIndex(id)]);
                idColorIndex = getColorIndex(id);
                idColor = color;
                idColor2 = color2;
                idColorsThemed = true;
            }
        }
        needApplyColorAccent = id == 5; // Tinting manually set blue color


        avatarType = AVATAR_TYPE_NORMAL;
        drawDeleted = false;

        if (firstName == null || firstName.length() == 0) {
            firstName = lastName;
            lastName = null;
        }

        getAvatarSymbols(firstName, lastName, custom, stringBuilder);
    }

    public void setText(String text) {
        invalidateTextLayout = true;
        avatarType = AVATAR_TYPE_NORMAL;
        drawDeleted = false;
        getAvatarSymbols(text, null, null, stringBuilder);
    }

    public static void getAvatarSymbols(String firstName, String lastName, String custom, StringBuilder result) {
        result.setLength(0);
        if (custom != null) {
            result.append(custom);
        } else {
            if (firstName != null && firstName.length() > 0) {
                result.append(takeFirstCharacter(firstName));
            }
            if (lastName != null && lastName.length() > 0) {
                String lastNameLastWord = lastName;
                int index;
                if ((index = lastNameLastWord.lastIndexOf(' ')) >= 0) {
                    lastNameLastWord = lastNameLastWord.substring(index + 1);
                }
                if (Build.VERSION.SDK_INT > 17) {
                    result.append("\u200C");
                }
                result.append(takeFirstCharacter(lastNameLastWord));
            } else if (firstName != null && firstName.length() > 0) {
                for (int a = firstName.length() - 1; a >= 0; a--) {
                    if (firstName.charAt(a) == ' ') {
                        if (a != firstName.length() - 1 && firstName.charAt(a + 1) != ' ') {
                            int index = result.length();
                            if (Build.VERSION.SDK_INT > 17) {
                                result.append("\u200C");
                            }
                            result.append(takeFirstCharacter(firstName.substring(index)));
                            break;
                        }
                    }
                }
            }
        }
    }

    private Drawable customIconDrawable;
    private int iconTx, iconTy;
    public void setCustomIcon(Drawable drawable) {
        customIconDrawable = drawable;
    }

    public void setIconTranslation(int tx, int ty) {
        this.iconTx = tx;
        this.iconTy = ty;
    }

    public Drawable getCustomIcon() {
        return customIconDrawable;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds == null) {
            return;
        }
        int size = bounds.width();
        if (false) { // EVE-PATCH: remove ThemeBind generation recheck
            refreshAvatarTypeColors();
        }
        if (false) { // EVE-PATCH: remove ThemeBind generation recheck
            if (avatarType == AVATAR_TYPE_NORMAL && color == idColor && color2 == idColor2 && idColorIndex >= 0 && idColorIndex < Theme.keys_avatar_background.length) {
                color = idColor = getThemedColor(Theme.keys_avatar_background[idColorIndex]);
                color2 = idColor2 = getThemedColor(Theme.keys_avatar_background2[idColorIndex]);
            } else {
                idColorsThemed = false; // colors replaced since (peer colors, setColor, avatar type): leave them
            }
        }
        namePaint.setColor(ColorUtils.setAlphaComponent(getThemedColor(Theme.key_avatar_text), alpha));
        Paint backgroundPaint = Theme.avatar_backgroundPaint;
        if (hasAdvancedGradient && advancedGradient != null) {
            advancedGradient.setBounds(bounds.left, bounds.top, bounds.left + size, bounds.top + size);
            backgroundPaint = advancedGradient.paint;
        } else if (hasGradient) {
            int color = ColorUtils.setAlphaComponent(getColor(), alpha);
            int color2 = ColorUtils.setAlphaComponent(getColor2(), alpha);
            if (gradient == null || gradientBottom != bounds.height() || gradientColor1 != color || gradientColor2 != color2) {
                gradient = new LinearGradient(0, 0, 0, gradientBottom = bounds.height(), gradientColor1 = color, gradientColor2 = color2, Shader.TileMode.CLAMP);
            }
            backgroundPaint.setShader(gradient);
            backgroundPaint.setAlpha(alpha);
        } else {
            backgroundPaint.setShader(null);
            backgroundPaint.setColor(ColorUtils.setAlphaComponent(getColor(), alpha));
        }
        canvas.save();
        canvas.translate(bounds.left, bounds.top);

        if (drawAvatarBackground) {
            if (rotate45Background) {
                canvas.save();
                canvas.rotate(-45, size / 2.0f, size / 2.0f);
            }
            if (roundRadius > 0) {
                AndroidUtilities.rectTmp.set(0, 0, size, size);
                canvas.drawRoundRect(AndroidUtilities.rectTmp, roundRadius, roundRadius, backgroundPaint);
            } else {
                canvas.drawCircle(size / 2.0f, size / 2.0f, size / 2.0f, backgroundPaint);
            }
            if (rotate45Background) {
                canvas.restore();
            }
        }

        canvas.restore();
    }

    @Override
    public void setAlpha(int alpha) {
        this.alpha = alpha;
    }

    @Override
    public void setColorFilter(ColorFilter cf) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return 0;
    }

    @Override
    public int getIntrinsicHeight() {
        return 0;
    }

    private int getThemedColor(int key) {
        return Theme.getColor(key, resourcesProvider);
    }

    public void setRoundRadius(int roundRadius) {
        this.roundRadius = roundRadius;
    }
}
