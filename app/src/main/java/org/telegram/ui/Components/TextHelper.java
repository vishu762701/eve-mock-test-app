package org.telegram.ui.Components;

import android.content.Context;
import android.util.TypedValue;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

public class TextHelper {

    public static TextView makeTextView(Context context, float textSizeDp, int colorKey, boolean bold) {
        return makeTextView(context, textSizeDp, colorKey, bold, null);
    }
    public static TextView makeTextView(Context context, float textSizeDp, int colorKey, boolean bold, Theme.ResourcesProvider resourcesProvider) {
        final TextView textView = new TextView(context); textView.setTextColor(Theme.getColor(colorKey, resourcesProvider)); // EVE-PATCH: direct text color without ThemeBind
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSizeDp);
        if (bold) {
            textView.setTypeface(AndroidUtilities.bold());
        }
        return textView;
    }

    public static LinkSpanDrawable.LinksTextView makeLinkTextView(Context context, float textSizeDp, int colorKey, boolean bold) {
        return makeLinkTextView(context, textSizeDp, colorKey, Theme.key_chat_messageLinkIn, bold, null);
    }
    public static LinkSpanDrawable.LinksTextView makeLinkTextView(Context context, float textSizeDp, int colorKey, boolean bold, Theme.ResourcesProvider resourcesProvider) {
        return makeLinkTextView(context, textSizeDp, colorKey, Theme.key_chat_messageLinkIn, bold, resourcesProvider);
    }
    public static LinkSpanDrawable.LinksTextView makeLinkTextView(Context context, float textSizeDp, int colorKey, int linkColorKey, boolean bold) {
        return makeLinkTextView(context, textSizeDp, colorKey, linkColorKey, bold, null);
    }
    public static LinkSpanDrawable.LinksTextView makeLinkTextView(Context context, float textSizeDp, int colorKey, int linkColorKey, boolean bold, Theme.ResourcesProvider resourcesProvider) {
        final LinkSpanDrawable.LinksTextView textView = new LinkSpanDrawable.LinksTextView(context); textView.setTextColor(Theme.getColor(colorKey, resourcesProvider)); // EVE-PATCH: direct link textView color without ThemeBind
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSizeDp);
        textView.setLinkTextColor(Theme.getColor(linkColorKey, resourcesProvider)); // EVE-PATCH: direct link color without ThemeBind
        if (bold) {
            textView.setTypeface(AndroidUtilities.bold());
        }
        return textView;
    }

}
