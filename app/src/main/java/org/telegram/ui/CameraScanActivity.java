package org.telegram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ShapeDrawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;
import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;


import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Components.AnimationProperties;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.LinkPath;
import org.telegram.ui.Components.LinkSpanDrawable;
import org.telegram.ui.Components.TypefaceSpan;
import org.telegram.ui.Components.URLSpanNoUnderline;

import java.util.ArrayList;

public class CameraScanActivity extends BaseFragment {

    private TextView titleTextView;
    private TextView descriptionText;
    private View cameraView; public interface EveCameraHost { View createPreviewView(Context context); void setTorchEnabled(boolean enabled); void openGallery(); void destroyCamera(); } private EveCameraHost eveCameraHost; public void setEveCameraHost(EveCameraHost host) { this.eveCameraHost = host; } public void onEveQrDetected(String text, RectF bounds) { if (recognized || text == null) return; recognized = true; recognizedStart = SystemClock.elapsedRealtime(); if (bounds != null) { updateRecognizedBounds(bounds, null); } if (delegate != null) { delegate.didFindQr(text); } } // EVE-PATCH: replace CameraView with EveCameraHost CUT-INTERFACE for CameraX/ZXing
    private HandlerThread backgroundHandlerThread = new HandlerThread("ScanCamera");
    private Handler handler;
    private TextView recognizedMrzView;
    private Paint paint = new Paint();
    private Paint cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Path path = new Path();
    private ImageView galleryButton;
    private ImageView flashButton;
    private AnimatorSet flashAnimator;
    private float backShadowAlpha = .5f;
    protected boolean shownAsBottomSheet = false;

    private SpringAnimation qrAppearing = null;
    private float qrAppearingValue = 0;

    private final PointF[] fromPoints = new PointF[4];
    private final PointF[] points = new PointF[4];
    private final PointF[] tmpPoints = new PointF[4];
    private final PointF[] tmp2Points = new PointF[4];
    {
        for (int i = 0; i < 4; ++i) {
            fromPoints[i] = new PointF(-1, -1);
            points[i] = new PointF(-1, -1);
            tmpPoints[i] = new PointF(-1, -1);
            tmp2Points[i] = new PointF(-1, -1);
        }
    }

    private final RectF fromBounds = new RectF();
    private final RectF bounds = new RectF();
    private long lastBoundsUpdate = 0;
    private final long boundsUpdateDuration = 75;

    private CameraScanActivityDelegate delegate;
    private boolean recognized;
    private long recognizedStart;
    private int recognizeFailed = 0;
    private int recognizeIndex = 0;
    private String recognizedText;

    private int sps; // samples per second (when already recognized)

    private boolean qrLoading = false;
    private boolean qrLoaded = false;

    private boolean needGalleryButton;

    private int currentType;

    public static final int TYPE_MRZ = 0;
    public static final int TYPE_QR = 1;
    public static final int TYPE_QR_LOGIN = 2;
    public static final int TYPE_QR_WEB_BOT = 3;

    public interface CameraScanActivityDelegate {

        default void didFindQr(String text) {

        }

        default boolean processQr(String text, Runnable onLoadEnd) {
            return false;
        }

        default String getSubtitleText() {
            return null;
        }

        default void onDismiss() {}
    }

    public CameraScanActivity(int type) {
        super();
        currentType = type;
        if (isQr()) {
        }

    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        destroy(false, null);
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        if (shownAsBottomSheet) {
            actionBar.setItemsColor(0xffffffff, false);
            actionBar.setItemsBackgroundColor(0xffffffff, false);
            actionBar.setTitleColor(0xffffffff);
        } else if (currentType == TYPE_MRZ) {
            actionBar.setItemsColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2), false); // EVE-PATCH: direct actionBar items color without ThemeBind
            actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarWhiteSelector), false); // EVE-PATCH: direct actionBar selector without ThemeBind
            actionBar.setTitleColor(Theme.getColor(Theme.key_actionBarDefaultTitle)); // EVE-PATCH: direct actionBar title color without ThemeBind
        } else {
            // overwritten with fixed white below (camera backdrop): no theme binding
            actionBar.setItemsColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2), false);
            actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarWhiteSelector), false);
            actionBar.setTitleColor(Theme.getColor(Theme.key_actionBarDefaultTitle));
        }
        actionBar.setCastShadows(false);
        if (!AndroidUtilities.isTablet() && !isQr()) {
            actionBar.showActionModeTop();
        }
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        paint.setColor(0x7f000000);
        cornerPaint.setColor(0xffffffff);
        cornerPaint.setStyle(Paint.Style.FILL);

        ViewGroup viewGroup = new ViewGroup(context) {

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int width = MeasureSpec.getSize(widthMeasureSpec);
                int height = MeasureSpec.getSize(heightMeasureSpec);
                actionBar.measure(widthMeasureSpec, heightMeasureSpec);
                if (currentType == TYPE_MRZ) {
                    if (cameraView != null) {
                        cameraView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec((int) (width * 0.704f), MeasureSpec.EXACTLY));
                    }
                } else {
                    if (cameraView != null) {
                        cameraView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
                    }
                    recognizedMrzView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.UNSPECIFIED));
                    if (galleryButton != null) {
                        galleryButton.measure(MeasureSpec.makeMeasureSpec(dp(60), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(dp(60), MeasureSpec.EXACTLY));
                    }
                    flashButton.measure(MeasureSpec.makeMeasureSpec(dp(60), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(dp(60), MeasureSpec.EXACTLY));
                }
                titleTextView.measure(MeasureSpec.makeMeasureSpec(width - dp(72), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.UNSPECIFIED));
                if (currentType == TYPE_QR_WEB_BOT) {
                    descriptionText.measure(MeasureSpec.makeMeasureSpec(width - dp(72), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.UNSPECIFIED));
                } else {
                    descriptionText.measure(MeasureSpec.makeMeasureSpec((int) (width * 0.9f), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.UNSPECIFIED));
                }

                setMeasuredDimension(width, height);
            }

            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
                int width = r - l;
                int height = b - t;

                int y = 0;
                if (currentType == TYPE_MRZ) {
                    if (cameraView != null) {
                        cameraView.layout(0, y, cameraView.getMeasuredWidth(), y + cameraView.getMeasuredHeight());
                    }
                    recognizedMrzView.setTextSize(TypedValue.COMPLEX_UNIT_PX, height / 22);
                    recognizedMrzView.setPadding(0, 0, 0, height / 15);
                    y = (int) (height * 0.65f);
                    titleTextView.layout(dp(36), y, dp(36) + titleTextView.getMeasuredWidth(), y + titleTextView.getMeasuredHeight());
                } else {
                    actionBar.layout(0, 0, actionBar.getMeasuredWidth(), actionBar.getMeasuredHeight());
                    if (cameraView != null) {
                        cameraView.layout(0, 0, cameraView.getMeasuredWidth(), cameraView.getMeasuredHeight());
                    }
                    int size = (int) (Math.min(width, height) / 1.5f);
                    if (currentType == TYPE_QR) {
                        y = (height - size) / 2 - titleTextView.getMeasuredHeight() - dp(30);
                    } else {
                        y = (height - size) / 2 - titleTextView.getMeasuredHeight() - dp(64);
                    }
                    titleTextView.layout(dp(36), y, dp(36) + titleTextView.getMeasuredWidth(), y + titleTextView.getMeasuredHeight());
                    if (currentType == TYPE_QR_WEB_BOT) {
                        y += titleTextView.getMeasuredHeight() + dp(8);
                        descriptionText.layout(dp(36), y, dp(36) + descriptionText.getMeasuredWidth(), y + descriptionText.getMeasuredHeight());
                    }
                    recognizedMrzView.layout(0, getMeasuredHeight() - recognizedMrzView.getMeasuredHeight(), getMeasuredWidth(), getMeasuredHeight());

                    int x;
                    if (needGalleryButton) {
                        x = width / 2 + dp(35);
                    } else {
                        x = width / 2 - flashButton.getMeasuredWidth() / 2;
                    }
                    y = (height - size) / 2 + size + dp(80);
                    flashButton.layout(x, y, x + flashButton.getMeasuredWidth(), y + flashButton.getMeasuredHeight());

                    if (galleryButton != null) {
                        x = width / 2 - dp(35) - galleryButton.getMeasuredWidth();
                        galleryButton.layout(x, y, x + galleryButton.getMeasuredWidth(), y + galleryButton.getMeasuredHeight());
                    }
                }

                if (currentType != TYPE_QR_WEB_BOT) {
                    y = (int) (height * 0.74f);
                    int x = (int) (width * 0.05f);
                    descriptionText.layout(x, y, x + descriptionText.getMeasuredWidth(), y + descriptionText.getMeasuredHeight());
                }

                updateNormalBounds();
            }

            Path path = new Path();

            @Override
            protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
                boolean result = super.drawChild(canvas, child, drawingTime);
                if (isQr() && child == cameraView) {
                    RectF bounds = getBounds();
                    int sizex = (int) (child.getWidth() * bounds.width()),
                        sizey = (int) (child.getHeight() * bounds.height()),
                        cx = (int) (child.getWidth() * bounds.centerX()),
                        cy = (int) (child.getHeight() * bounds.centerY());

//                    PointF[] points = getPoints();
//                    path.rewind();
//                    for (int i = 0; i < points.length; ++i) {
//                        float x = child.getWidth() * points[i].x;
//                        float y = child.getHeight() * points[i].y;
//                        if (i == 0) path.moveTo(x, y);
//                        else path.lineTo(x, y);
//                    }
//                    Theme.DEBUG_RED.setAlpha(40);
//                    canvas.drawPath(path, Theme.DEBUG_RED);

                    sizex *= (.5f + qrAppearingValue * .5f);
                    sizey *= (.5f + qrAppearingValue * .5f);
                    int x = cx - sizex / 2,
                        y = cy - sizey / 2;

                    paint.setAlpha((int) (255 * (1f - (1f - backShadowAlpha) * Math.min(1, qrAppearingValue))));
                    canvas.drawRect(0, 0, child.getMeasuredWidth(), y, paint);
                    canvas.drawRect(0, y + sizey, child.getMeasuredWidth(), child.getMeasuredHeight(), paint);
                    canvas.drawRect(0, y, x, y + sizey, paint);
                    canvas.drawRect(x + sizex, y, child.getMeasuredWidth(), y + sizey, paint);
                    paint.setAlpha((int) (255 * Math.max(0, 1f - qrAppearingValue)));
                    canvas.drawRect(x, y, x + sizex, y + sizey, paint);

                    final int lineWidth = AndroidUtilities.lerp(0, dp(4), Math.min(1, qrAppearingValue * 20f)),
                              halfLineWidth = lineWidth / 2;
                    final int lineLength = AndroidUtilities.lerp(Math.min(sizex, sizey), dp(20), Math.min(1.2f, (float) Math.pow(qrAppearingValue, 1.8f)));

                    cornerPaint.setAlpha((int) (255 * Math.min(1, qrAppearingValue)));

                    path.reset();
                    path.arcTo(aroundPoint(x, y + lineLength, halfLineWidth), 0, 180);
                    path.arcTo(aroundPoint((int) (x + lineWidth * 1.5f), (int) (y + lineWidth * 1.5f), lineWidth * 2), 180, 90);
                    path.arcTo(aroundPoint(x + lineLength, y, halfLineWidth), 270, 180);
                    path.lineTo(x + halfLineWidth, y + halfLineWidth);
                    path.arcTo(aroundPoint((int) (x + lineWidth * 1.5f), (int) (y + lineWidth * 1.5f), lineWidth), 270, -90);
                    path.close();
                    canvas.drawPath(path, cornerPaint);

                    path.reset();
                    path.arcTo(aroundPoint(x + sizex, y + lineLength, halfLineWidth), 180, -180);
                    path.arcTo(aroundPoint((int) (x + sizex - lineWidth * 1.5f), (int) (y + lineWidth * 1.5f), lineWidth * 2), 0, -90);
                    path.arcTo(aroundPoint(x + sizex- lineLength, y, halfLineWidth), 270, -180);
                    path.arcTo(aroundPoint((int) (x + sizex - lineWidth * 1.5f), (int) (y + lineWidth * 1.5f), lineWidth), 270, 90);
                    path.close();
                    canvas.drawPath(path, cornerPaint);

                    path.reset();
                    path.arcTo(aroundPoint(x, y + sizey - lineLength, halfLineWidth), 0, -180);
                    path.arcTo(aroundPoint((int) (x + lineWidth * 1.5f), (int) (y + sizey - lineWidth * 1.5f), lineWidth * 2), 180, -90);
                    path.arcTo(aroundPoint(x + lineLength, y + sizey, halfLineWidth), 90, -180);
                    path.arcTo(aroundPoint((int) (x + lineWidth * 1.5f), (int) (y + sizey - lineWidth * 1.5f), lineWidth), 90, 90);
                    path.close();
                    canvas.drawPath(path, cornerPaint);

                    path.reset();
                    path.arcTo(aroundPoint(x + sizex, y + sizey - lineLength, halfLineWidth), 180, 180);
                    path.arcTo(aroundPoint((int) (x + sizex - lineWidth * 1.5f), (int) (y + sizey - lineWidth * 1.5f), lineWidth * 2), 0, 90);
                    path.arcTo(aroundPoint(x + sizex - lineLength, y + sizey, halfLineWidth), 90, 180);
                    path.arcTo(aroundPoint((int) (x + sizex - lineWidth * 1.5f), (int) (y + sizey - lineWidth * 1.5f), lineWidth), 90, -90);
                    path.close();

                    canvas.drawPath(path, cornerPaint);
                }
                return result;
            }

            private RectF aroundPoint(int x, int y, int r) {
                AndroidUtilities.rectTmp.set(x - r, y - r, x + r, y + r);
                return AndroidUtilities.rectTmp;
            }
        };
        viewGroup.setOnTouchListener((v, event) -> true);
        fragmentView = viewGroup;

        if (isQr()) {
            if (eveCameraHost != null && fragmentView instanceof ViewGroup) { cameraView = eveCameraHost.createPreviewView(fragmentView.getContext()); if (cameraView != null) { ((ViewGroup) fragmentView).addView(cameraView, 0, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT)); } } // EVE-PATCH: attach Eve CameraX preview view
        } else {
        }

        if (currentType == TYPE_MRZ) {
            actionBar.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite)); // EVE-PATCH: direct actionBar background without ThemeBind
            fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite)); // EVE-PATCH: direct fragment background without ThemeBind
        } else {
            actionBar.setBackgroundDrawable(null);
            actionBar.setAddToContainer(false);
            actionBar.setTitleColor(0xffffffff);
            actionBar.setItemsColor(0xffffffff, false);
            actionBar.setItemsBackgroundColor(0x22ffffff, false);
            viewGroup.setBackgroundColor(0xFF000000);
            viewGroup.addView(actionBar);
        }

        if (currentType == TYPE_QR_LOGIN || currentType == TYPE_QR_WEB_BOT) {
            actionBar.setTitle(LocaleController.getString(R.string.AuthAnotherClientScan));
        }

        Paint selectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectionPaint.setPathEffect(LinkPath.getRoundedEffect());
        selectionPaint.setColor(ColorUtils.setAlphaComponent(Color.WHITE, 40));
        titleTextView = new TextView(context) {
            LinkPath textPath;
            private LinkSpanDrawable<URLSpanNoUnderline> pressedLink;
            LinkSpanDrawable.LinkCollector links = new LinkSpanDrawable.LinkCollector(this);

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                if (getText() instanceof Spanned) {
                    Spanned spanned = (Spanned) getText();
                    URLSpanNoUnderline[] innerSpans = spanned.getSpans(0, spanned.length(), URLSpanNoUnderline.class);
                    if (innerSpans != null && innerSpans.length > 0) {
                        textPath = new LinkPath(true);
                        textPath.setAllowReset(false);
                        for (int a = 0; a < innerSpans.length; a++) {
                            int start = spanned.getSpanStart(innerSpans[a]);
                            int end = spanned.getSpanEnd(innerSpans[a]);
                            textPath.setCurrentLayout(getLayout(), start, 0);
                            int shift = getText() != null ? getPaint().baselineShift : 0;
                            textPath.setBaselineShift(shift != 0 ? shift + dp(shift > 0 ? 5 : -2) : 0);
                            getLayout().getSelectionPath(start, end, textPath);
                        }
                        textPath.setAllowReset(true);
                    }
                }
            }

            @Override
            public boolean onTouchEvent(MotionEvent e) {
                final Layout textLayout = getLayout();
                int textX = 0, textY = 0;
                int x = (int) (e.getX() - textX);
                int y = (int) (e.getY() - textY);
                if (e.getAction() == MotionEvent.ACTION_DOWN || e.getAction() == MotionEvent.ACTION_UP) {
                    final int line = textLayout.getLineForVertical(y);
                    final int off = textLayout.getOffsetForHorizontal(line, x);

                    final float left = textLayout.getLineLeft(line);
                    if (left <= x && left + textLayout.getLineWidth(line) >= x && y >= 0 && y <= textLayout.getHeight()) {
                        Spannable buffer = (Spannable) textLayout.getText();
                        ClickableSpan[] link = buffer.getSpans(off, off, ClickableSpan.class);
                        if (link.length != 0) {
                            links.clear();
                            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                                pressedLink = new LinkSpanDrawable(link[0], null, e.getX(), e.getY());
                                pressedLink.setColor(0x2dffffff);
                                links.addLink(pressedLink);
                                int start = buffer.getSpanStart(pressedLink.getSpan());
                                int end = buffer.getSpanEnd(pressedLink.getSpan());
                                LinkPath path = pressedLink.obtainNewPath();
                                path.setCurrentLayout(textLayout, start, textY);
                                textLayout.getSelectionPath(start, end, path);
                            } else if (e.getAction() == MotionEvent.ACTION_UP) {
                                if (pressedLink != null && pressedLink.getSpan() == link[0]) {
                                    link[0].onClick(this);
                                }
                                pressedLink = null;
                            }
                            return true;
                        }
                    }
                }
                if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
                    links.clear();
                    pressedLink = null;
                }
                return super.onTouchEvent(e);
            }

            @Override
            protected void onDraw(Canvas canvas) {
                if (textPath != null) {
                    canvas.drawPath(textPath, selectionPaint);
                }
                if (links.draw(canvas)) {
                    invalidate();
                }
                super.onDraw(canvas);
            }
        };
        titleTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);

        viewGroup.addView(titleTextView);

        descriptionText = new TextView(context);
        if (currentType == TYPE_QR_WEB_BOT) {
            descriptionText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText6)); // overwritten with fixed white below
        } else {
            descriptionText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText6)); // EVE-PATCH: direct description color without ThemeBind
        }
        descriptionText.setGravity(Gravity.CENTER_HORIZONTAL);
        descriptionText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        viewGroup.addView(descriptionText);

        recognizedMrzView = new TextView(context);
        recognizedMrzView.setTextColor(0xffffffff);
        recognizedMrzView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        recognizedMrzView.setAlpha(0);

        if (currentType == TYPE_MRZ) {
            titleTextView.setText(LocaleController.getString(R.string.PassportScanPassport));
            descriptionText.setText(LocaleController.getString(R.string.PassportScanPassportInfo));
            titleTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText)); // EVE-PATCH: direct title color without ThemeBind
            recognizedMrzView.setTypeface(Typeface.MONOSPACE);
        } else {
            if (needGalleryButton) {
                //titleTextView.setText(LocaleController.getString(R.string.WalletScanCode));
            } else {
                if (currentType == TYPE_QR || currentType == TYPE_QR_WEB_BOT) {
                    titleTextView.setText(LocaleController.getString(R.string.AuthAnotherClientScan));
                } else {
                    String text = LocaleController.getString(R.string.AuthAnotherClientInfo5);
                    SpannableStringBuilder spanned = new SpannableStringBuilder(text);

                    String[] links = new String[] {
                        LocaleController.getString(R.string.AuthAnotherClientDownloadClientUrl),
                        LocaleController.getString(R.string.AuthAnotherWebClientUrl)
                    };
                    for (int i = 0; i < links.length; ++i) {
                        text = spanned.toString();
                        int index1 = text.indexOf('*');
                        int index2 = text.indexOf('*', index1 + 1);

                        if (index1 != -1 && index2 != -1 && index1 != index2) {
                            titleTextView.setMovementMethod(new AndroidUtilities.LinkMovementMethodMy());
                            spanned.replace(index2, index2 + 1, " ");
                            spanned.replace(index1, index1 + 1, " ");
                            index1 += 1;
                            index2 += 1;
                            spanned.setSpan(new URLSpanNoUnderline(links[i], true), index1, index2 - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            spanned.setSpan(new TypefaceSpan(AndroidUtilities.bold()), index1, index2 - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        } else {
                            break;
                        }
                    }

                    titleTextView.setLinkTextColor(0xffffffff);

                    titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                    titleTextView.setLineSpacing(dp(2), 1.0f);
                    titleTextView.setPadding(0, 0, 0, 0);
                    titleTextView.setText(spanned);
                }
            }
            titleTextView.setTextColor(0xffffffff);
            if (currentType == TYPE_QR_WEB_BOT) {
                descriptionText.setTextColor(0x99ffffff);
            }
            recognizedMrzView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            recognizedMrzView.setPadding(dp(10), 0, dp(10), dp(10));
            if (needGalleryButton) {
                //recognizedMrzView.setText(LocaleController.getString(R.string.WalletScanCodeNotFound));
            } else {
                recognizedMrzView.setText(LocaleController.getString(R.string.AuthAnotherClientNotFound));
            }
            viewGroup.addView(recognizedMrzView);

            if (needGalleryButton) {
                galleryButton = new ImageView(context);
                galleryButton.setScaleType(ImageView.ScaleType.CENTER);
                galleryButton.setImageResource(R.drawable.qr_gallery);
                galleryButton.setBackgroundDrawable(Theme.createSelectorDrawableFromDrawables(Theme.createCircleDrawable(dp(60), 0x22ffffff), Theme.createCircleDrawable(dp(60), 0x44ffffff)));
                viewGroup.addView(galleryButton);
                galleryButton.setOnClickListener(currentImage -> {
                    if (getParentActivity() == null) {
                        return;
                    }
                    final Activity activity = getParentActivity();
                    if (eveCameraHost != null) { eveCameraHost.openGallery(); } // EVE-PATCH: route gallery pick to EveCameraHost
                });
            }

            flashButton = new ImageView(context);
            flashButton.setScaleType(ImageView.ScaleType.CENTER);
            flashButton.setImageResource(R.drawable.qr_flashlight);
            flashButton.setBackgroundDrawable(Theme.createCircleDrawable(dp(60), 0x22ffffff));
            viewGroup.addView(flashButton);
            flashButton.setOnClickListener(currentImage -> {
                if (cameraView == null) {
                    return;
                }
                if (eveCameraHost != null) { // EVE-PATCH: toggle torch through EveCameraHost
                    ShapeDrawable shapeDrawable = (ShapeDrawable) flashButton.getBackground();
                    if (flashAnimator != null) {
                        flashAnimator.cancel();
                        flashAnimator = null;
                    }
                    flashAnimator = new AnimatorSet();
                    ObjectAnimator animator = ObjectAnimator.ofInt(shapeDrawable, AnimationProperties.SHAPE_DRAWABLE_ALPHA, flashButton.getTag() == null ? 0x44 : 0x22);
                    animator.addUpdateListener(animation -> flashButton.invalidate());
                    flashAnimator.playTogether(animator);
                    flashAnimator.setDuration(200);
                    flashAnimator.setInterpolator(CubicBezierInterpolator.DEFAULT);
                    flashAnimator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            flashAnimator = null;
                        }
                    });
                    flashAnimator.start();
                    if (flashButton.getTag() == null) {
                        flashButton.setTag(1);
                        eveCameraHost.setTorchEnabled(true); // EVE-PATCH: enable torch via EveCameraHost
                    } else {
                        flashButton.setTag(null);
                        eveCameraHost.setTorchEnabled(false); // EVE-PATCH: disable torch via EveCameraHost
                    }
                }
            });
        }

        fragmentView.setKeepScreenOn(true);

        return fragmentView;
    }

    private ValueAnimator recognizedAnimator;
    private float recognizedT = 0;
    private float newRecognizedT = 0;
    private SpringAnimation useRecognizedBoundsAnimator;
    private float useRecognizedBounds = 0;
    private void updateRecognized() {
        float wasNewRecognizedT = recognizedT;
        newRecognizedT = recognized ? 1f : 0f;
        if (wasNewRecognizedT != newRecognizedT) {
            if (recognizedAnimator != null) {
                recognizedAnimator.cancel();
            }
        } else {
            return;
        }

        recognizedAnimator = ValueAnimator.ofFloat(recognizedT, newRecognizedT);
        recognizedAnimator.addUpdateListener(a -> {
            recognizedT = (float) a.getAnimatedValue();
            titleTextView.setAlpha(1f - recognizedT);
            if (currentType == TYPE_QR_WEB_BOT) {
                descriptionText.setAlpha(1f - recognizedT);
            }
            flashButton.setAlpha(1f - recognizedT);
            backShadowAlpha = .5f + recognizedT * .25f;
            fragmentView.invalidate();
        });
        recognizedAnimator.setDuration((long) (300 * Math.abs(recognizedT - newRecognizedT)));
        recognizedAnimator.setInterpolator(CubicBezierInterpolator.DEFAULT);
        recognizedAnimator.start();

        if (useRecognizedBoundsAnimator != null) {
            useRecognizedBoundsAnimator.cancel();
        }
        final float force = 500f;
        useRecognizedBoundsAnimator = new SpringAnimation(new FloatValueHolder((recognized ? useRecognizedBounds : 1f - useRecognizedBounds) * force));
        useRecognizedBoundsAnimator.addUpdateListener((animation, value, velocity) -> {
            useRecognizedBounds = recognized ? value / force : (1f - value / force);
            fragmentView.invalidate();
        });
        useRecognizedBoundsAnimator.setSpring(new SpringForce(force));
        useRecognizedBoundsAnimator.getSpring().setDampingRatio(1f);
        useRecognizedBoundsAnimator.getSpring().setStiffness(500.0f);
        useRecognizedBoundsAnimator.start();
    }

    private void setPointsFromBounds(RectF bounds, PointF[] points) {
        points[0].set(bounds.left, bounds.top);
        points[1].set(bounds.right, bounds.top);
        points[2].set(bounds.right, bounds.bottom);
        points[3].set(bounds.left, bounds.bottom);
    }

    private void updateRecognizedBounds(RectF newBounds, PointF[] newPoints) {
        final long now = SystemClock.elapsedRealtime();
        if (lastBoundsUpdate == 0) {
            // first update = set
            lastBoundsUpdate = now - boundsUpdateDuration;
            bounds.set(newBounds);
            fromBounds.set(newBounds);
            if (newPoints == null) {
                setPointsFromBounds(newBounds, fromPoints);
                setPointsFromBounds(newBounds, points);
            } else {
                for (int i = 0; i < 4; i++) {
                    fromPoints[i].set(newPoints[i].x, newPoints[i].y);
                    points[i].set(newPoints[i].x, newPoints[i].y);
                }
            }
        } else {
            // next updates = interpolate
            if (fromBounds != null && now - lastBoundsUpdate < boundsUpdateDuration) {
                float t = (now - lastBoundsUpdate) / (float) boundsUpdateDuration;
                t = Math.min(1, Math.max(0, t));
                AndroidUtilities.lerp(fromBounds, bounds, t, fromBounds);

                for (int i = 0; i < 4; ++i) {
                    fromPoints[i].set(
                        AndroidUtilities.lerp(fromPoints[i].x, points[i].x, t),
                        AndroidUtilities.lerp(fromPoints[i].y, points[i].y, t)
                    );
                }
            } else {
                fromBounds.set(bounds);
                for (int i = 0; i < 4; ++i) {
                    fromPoints[i].set(points[i].x, points[i].y);
                }
            }
            bounds.set(newBounds);
            if (newPoints == null) {
                setPointsFromBounds(bounds, points);
            } else {
                for (int i = 0; i < 4; ++i) {
                    points[i].set(newPoints[i].x, newPoints[i].y);
                }
            }
            lastBoundsUpdate = now;
        }
        fragmentView.invalidate();
    }

    private RectF getRecognizedBounds() {
        float t = (SystemClock.elapsedRealtime() - lastBoundsUpdate) / (float) boundsUpdateDuration;
        t = Math.min(1, Math.max(0, t));
        if (t < 1f) {
            fragmentView.invalidate();
        }
        AndroidUtilities.lerp(fromBounds, bounds, t, AndroidUtilities.rectTmp);
        return AndroidUtilities.rectTmp;
    }

    private PointF[] getRecognizedPoints() {
        float t = (SystemClock.elapsedRealtime() - lastBoundsUpdate) / (float) boundsUpdateDuration;
        t = Math.min(1, Math.max(0, t));
        if (t < 1f) {
            fragmentView.invalidate();
        }
        for (int i = 0; i < 4; ++i) {
            tmpPoints[i].set(
                AndroidUtilities.lerp(fromPoints[i].x, points[i].x, t),
                AndroidUtilities.lerp(fromPoints[i].y, points[i].y, t)
            );
        }
        return tmpPoints;
    }

    private RectF normalBounds;
    private void updateNormalBounds() {
        if (normalBounds == null) {
            normalBounds = new RectF();
        }
        int width = fragmentView.getWidth(),
            height = fragmentView.getHeight(),
            side = (int) (Math.min(width, height) / 1.5f);
        normalBounds.set(
            (width - side) / 2f / (float) width,
            (height - side) / 2f / (float) height,
            (width + side) / 2f / (float) width,
            (height + side) / 2f / (float) height
        );
    }

    private RectF getBounds() {
        RectF recognizedBounds = getRecognizedBounds();
        if (useRecognizedBounds < 1f) {
            if (normalBounds == null) {
                updateNormalBounds();
            }
            AndroidUtilities.lerp(normalBounds, recognizedBounds, useRecognizedBounds, recognizedBounds);
        }
        return recognizedBounds;
    }

    private PointF[] getPoints() {
        PointF[] recognizedPoints = getRecognizedPoints();
        if (useRecognizedBounds < 1f) {
            if (normalBounds == null) {
                updateNormalBounds();
            }
            setPointsFromBounds(normalBounds, tmp2Points);
            for (int i = 0; i < recognizedPoints.length; ++i) {
                recognizedPoints[i].set(
                    AndroidUtilities.lerp(tmp2Points[i].x, recognizedPoints[i].x, useRecognizedBounds),
                    AndroidUtilities.lerp(tmp2Points[i].y, recognizedPoints[i].y, useRecognizedBounds)
                );
            }
        }
        return recognizedPoints;
    }

    public void setDelegate(CameraScanActivityDelegate cameraScanActivityDelegate) {
        delegate = cameraScanActivityDelegate;
    }

    public void destroy(boolean async, final Runnable beforeDestroyRunnable) {
        if (cameraView != null) {
            if (eveCameraHost != null) { eveCameraHost.destroyCamera(); } // EVE-PATCH: destroy camera via EveCameraHost
            cameraView = null;
        }
        backgroundHandlerThread.quitSafely();
    }

    private void onNoQrFound() {
        AndroidUtilities.runOnUIThread(() -> {
            if (recognizedMrzView.getTag() != null) {
                recognizedMrzView.setTag(null);
                recognizedMrzView.animate().setDuration(200).alpha(0.0f).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
            }
        });
    }

    private float averageProcessTime = 0;
    private long processTimesCount = 0;

    private class QrResult {
        String text;
        RectF bounds;
        PointF[] cornerPoints;
    }


    private boolean isQr() {
        return currentType == TYPE_QR || currentType == TYPE_QR_LOGIN || currentType == TYPE_QR_WEB_BOT;
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();
        if (isQr()) {
            return themeDescriptions;
        }



        themeDescriptions.add(new ThemeDescription(titleTextView, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(descriptionText, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteGrayText6));

        return themeDescriptions;
    }
}
