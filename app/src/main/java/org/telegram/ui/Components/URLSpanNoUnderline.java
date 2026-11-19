/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Components;

import android.net.Uri;
import android.text.TextPaint;
import android.text.style.URLSpan;
import android.view.View;


public class URLSpanNoUnderline extends URLSpan {

    private boolean forceNoUnderline = false;

    // Used to label video timestamps
    public String label;

    public URLSpanNoUnderline(String url) {
        this(url, null);
    }

    public URLSpanNoUnderline(String url, boolean forceNoUnderline) {
        this(url, null);
        this.forceNoUnderline = forceNoUnderline;
    }

    public URLSpanNoUnderline(String url, Object run) { // EVE-PATCH: use Object instead of TextStyleSpan
        super(url != null ? url.replace('\u202E', ' ') : url);
    }

    @Override
    public void onClick(View widget) {
        String url = getURL();
        if (url.startsWith("@")) {
            Uri uri = Uri.parse("https://t.me/" + url.substring(1));
            widget.getContext().startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, uri)); // EVE-PATCH: open URL with standard Android Intent without Browser
        } else {
            widget.getContext().startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url))); // EVE-PATCH: open URL with standard Android Intent without Browser
        }
    }

    @Override
    public void updateDrawState(TextPaint p) {
        int l = p.linkColor;
        int c = p.getColor();
        super.updateDrawState(p);
        p.setUnderlineText(l == c && !forceNoUnderline);
    }

}
