package com.wheelpicker;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;



public class LoopView extends View {
    ScheduledExecutorService mExecutor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> mFuture;
    int totalScrollY;
    Handler handler;
    LoopListener loopListener;
    private GestureDetector gestureDetector;
    private int selectedItem;
    private GestureDetector.SimpleOnGestureListener simpleOnGestureListener;
    Context context;
    Paint paintA;  //paint that draw top and bottom text
    Paint paintB;  // paint that draw center text
    Paint paintC;  // paint that draw line besides center text
    ArrayList arrayList;
    int textSize;
    int maxTextWidth;
    int maxTextHeight;
    int colorGray;
    int colorBlack;
    int colorGrayLight;
    float lineSpacingMultiplier;
    boolean isLoop;
    int firstLineY;
    int secondLineY;
    int preCurrentIndex;
    int initPosition;
    int itemCount;
    int measuredHeight;
    int halfCircumference;
    int radius;
    int measuredWidth;
    int change;
    float y1;
    float y2;
    float dy;

    private AccessibilityNodeProvider mAccessibilityNodeProvider;

    public LoopView(Context context) {
        super(context);
        initLoopView(context);
    }

    public LoopView(Context context, AttributeSet attributeset) {
        super(context, attributeset);
        initLoopView(context);
    }

    public LoopView(Context context, AttributeSet attributeset, int defStyleAttr) {
        super(context, attributeset, defStyleAttr);
        initLoopView(context);
    }

    private void initLoopView(Context context) {
        textSize = 0;
        colorGray = 0xffafafaf;
        colorBlack = 0xff313131;
        colorGrayLight = 0xffc5c5c5;
        lineSpacingMultiplier = 2.0F;
        isLoop = false;
        initPosition = 0;
        itemCount = 7;
        y1 = 0.0F;
        y2 = 0.0F;
        dy = 0.0F;
        totalScrollY = 0;
        simpleOnGestureListener = new LoopViewGestureListener(this);
        handler = new MessageHandler(this);
        this.context = context;
        setTextSize(16F);

        paintA = new Paint();
        paintA.setColor(colorGrayLight);
        paintB = new Paint();
        paintB.setTextSize(textSize);
        paintC = new Paint();
        paintA.setTextSize(textSize);
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }
        gestureDetector = new GestureDetector(context, simpleOnGestureListener);
        gestureDetector.setIsLongpressEnabled(false);
    }

    static int getSelectedItem(LoopView loopview) {
        return loopview.selectedItem;
    }

    static void smoothScroll(LoopView loopview) {
        loopview.smoothScroll();
    }

    private void initData() {
        if (arrayList == null) {
            return;
        }
        paintA.setAntiAlias(true);
        paintB.setAntiAlias(true);
        paintC.setAntiAlias(true);
        paintC.setTypeface(Typeface.MONOSPACE);
        paintC.setTextSize(textSize);
        measureTextWidthHeight();
        halfCircumference = (int) (maxTextHeight * lineSpacingMultiplier * (itemCount - 1));
        measuredHeight = (int) ((halfCircumference * 2) / Math.PI);
        radius = (int) (halfCircumference / Math.PI);
        firstLineY = (int) ((measuredHeight - lineSpacingMultiplier * maxTextHeight) / 2.0F);
        secondLineY = (int) ((measuredHeight + lineSpacingMultiplier * maxTextHeight) / 2.0F);
        if (initPosition == -1) {
            if (isLoop) {
                initPosition = (arrayList.size() + 1) / 2;
            } else {
                initPosition = 0;
            }
        }
        preCurrentIndex = initPosition;
    }

    private void measureTextWidthHeight() {
        Rect rect = new Rect();
        for (int i = 0; i < arrayList.size(); i++) {
            String s1 = (String) arrayList.get(i);
            paintB.getTextBounds(s1, 0, s1.length(), rect);
            int textWidth = rect.width();
            if (textWidth > maxTextWidth) {
                maxTextWidth = textWidth;
            }
            paintB.getTextBounds("\u661F\u671F", 0, 2, rect); // 星期
            int textHeight = rect.height();
            if (textHeight > maxTextHeight) {
                maxTextHeight = textHeight;
            }
        }

    }


    private void smoothScroll() {
        int offset = (int) (totalScrollY % (lineSpacingMultiplier * maxTextHeight));
        cancelFuture();
        mFuture = mExecutor.scheduleWithFixedDelay(new MTimer(this, offset), 0, 10, TimeUnit.MILLISECONDS);
    }

    public void cancelFuture() {
        if (mFuture!=null&&!mFuture.isCancelled()) {
            mFuture.cancel(true);
            mFuture = null;
        }
    }

    public final int getSelectedItem() {
        return selectedItem;
    }

    protected final void smoothScroll(float velocityY) {
        cancelFuture();
        int velocityFling = 20;
        mFuture = mExecutor.scheduleWithFixedDelay(new LoopTimerTask(this, velocityY), 0, velocityFling, TimeUnit.MILLISECONDS);
    }


    protected final void itemSelected() {
        if (loopListener != null) {
            postDelayed(new LoopRunnable(this), 200L);
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
        setContentDescription((String)arrayList.get(selectedItem));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        String as[];
        if (arrayList == null) {
            super.onDraw(canvas);
            return;
        }
        as = new String[itemCount];
        change = (int) (totalScrollY / (lineSpacingMultiplier * maxTextHeight));
        preCurrentIndex = initPosition + change % arrayList.size();
        if (!isLoop) {
            if (preCurrentIndex < 0) {
                preCurrentIndex = 0;
            }
            if (preCurrentIndex > arrayList.size() - 1) {
                preCurrentIndex = arrayList.size() - 1;
            }
            // break;
        } else {
            if (preCurrentIndex < 0) {
                preCurrentIndex = arrayList.size() + preCurrentIndex;
            }
            if (preCurrentIndex > arrayList.size() - 1) {
                preCurrentIndex = preCurrentIndex - arrayList.size();
            }
            // continue;
        }

        int j2 = (int) (totalScrollY % (lineSpacingMultiplier * maxTextHeight));
        int k1 = 0;
        while (k1 < itemCount) {
            int l1 = preCurrentIndex - (itemCount / 2 - k1);
            if (isLoop) {
                if (l1 < 0) {
                    l1 = l1 + arrayList.size();
                }
                if (l1 > arrayList.size() - 1) {
                    l1 = l1 - arrayList.size();
                }
                as[k1] = (String) arrayList.get(l1);
            } else if (l1 < 0) {
                as[k1] = "";
            } else if (l1 > arrayList.size() - 1) {
                as[k1] = "";
            } else {
                as[k1] = (String) arrayList.get(l1);
            }
            k1++;
        }
        canvas.drawLine(0.0F, firstLineY, measuredWidth, firstLineY, paintC);
        canvas.drawLine(0.0F, secondLineY, measuredWidth, secondLineY, paintC);
        int j1 = 0;
        while (j1 < itemCount) {
            canvas.save();
            // L=α* r
            // (L * π ) / (π * r)
            float itemHeight = maxTextHeight * lineSpacingMultiplier;
            double radian = ((itemHeight * j1 - j2) * Math.PI) / halfCircumference;
            float angle = (float) (90D - (radian / Math.PI) * 180D);
            if (angle >= 90F || angle <= -90F) {
                canvas.restore();
            } else {
                int translateY = (int) (radius - Math.cos(radian) * radius - (Math.sin(radian) * maxTextHeight) / 2D);
                canvas.translate(0.0F, translateY);
                canvas.scale(1.0F, (float) Math.sin(radian));
                if (translateY <= firstLineY && maxTextHeight + translateY >= firstLineY) {
                    canvas.save();
                    //top = 0,left = (measuredWidth - maxTextWidth)/2
                    canvas.clipRect(0, 0, measuredWidth, firstLineY - translateY);
                    drawCenter(canvas, paintA, as[j1],maxTextHeight);
                    canvas.restore();
                    canvas.save();
                    canvas.clipRect(0, firstLineY - translateY, measuredWidth, (int) (itemHeight));
                    drawCenter(canvas, paintB, as[j1], maxTextHeight);
                    canvas.restore();
                } else if (translateY <= secondLineY && maxTextHeight + translateY >= secondLineY) {
                    canvas.save();
                    canvas.clipRect(0, 0, measuredWidth, secondLineY - translateY);
                    drawCenter(canvas, paintB, as[j1], maxTextHeight);
                    canvas.restore();
                    canvas.save();
                    canvas.clipRect(0, secondLineY - translateY, measuredWidth, (int) (itemHeight));
                    drawCenter(canvas, paintA, as[j1],maxTextHeight);
                    canvas.restore();
                } else if (translateY >= firstLineY && maxTextHeight + translateY <= secondLineY) {
                    canvas.clipRect(0, 0, measuredWidth, (int) (itemHeight));
                    drawCenter(canvas, paintB, as[j1],maxTextHeight);
                    selectedItem = arrayList.indexOf(as[j1]);
                } else {
                    canvas.clipRect(0, 0, measuredWidth, (int) (itemHeight));
                    drawCenter(canvas, paintA, as[j1],maxTextHeight);
                }
                canvas.restore();
            }
            j1++;
        }
        super.onDraw(canvas);
    }

    private Rect r = new Rect();

    private void drawCenter(Canvas canvas, Paint paint, String text, int y) {
        canvas.getClipBounds(r);
        int cWidth = r.width();
        paint.setTextAlign(Paint.Align.LEFT);
        paint.getTextBounds(text, 0, text.length(), r);
        float x = cWidth / 2f - r.width() / 2f - r.left;
        canvas.drawText(text, x, y, paint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        initData();
        measuredWidth = getMeasuredWidth();
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionevent) {
        switch (motionevent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                y1 = motionevent.getRawY();
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                y2 = motionevent.getRawY();
                dy = y1 - y2;
                y1 = y2;
                totalScrollY = (int) ((float) totalScrollY + dy);
                if (!isLoop) {
                    int initPositionCircleLength = (int) (initPosition * (lineSpacingMultiplier * maxTextHeight));
                    int initPositionStartY = -1 * initPositionCircleLength;
                    if (totalScrollY < initPositionStartY) {
                        totalScrollY = initPositionStartY;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
            default:
                if (!gestureDetector.onTouchEvent(motionevent) && motionevent.getAction() == MotionEvent.ACTION_UP) {
                    smoothScroll();
                }
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                return true;
        }

        if (!isLoop) {
            int circleLength = (int) ((float) (arrayList.size() - 1 - initPosition) * (lineSpacingMultiplier * maxTextHeight));
            if (totalScrollY >= circleLength) {
                totalScrollY = circleLength;
            }
        }
        invalidate();

        if (!gestureDetector.onTouchEvent(motionevent) && motionevent.getAction() == MotionEvent.ACTION_UP) {
            smoothScroll();
        }
        return true;
    }

    // Picker methods
    public final void setLoop(boolean isCyclic) {
        isLoop = isCyclic;
    }

    public final void setTextSize(float size) {
        if (size > 0.0F) {
            textSize = (int) (context.getResources().getDisplayMetrics().density * size);
        }
    }

    public final void setInitPosition(int initPosition) {
        this.initPosition = initPosition;
    }

    public final void setListener(LoopListener LoopListener) {
        loopListener = LoopListener;
    }

    public final void setArrayList(ArrayList arraylist) {
        this.arrayList = arraylist;
        initData();
        invalidate();
    }

    public final void setSelectedItemTextColor(int color) {
        paintB.setColor(color);
    }

    public final void setSelectedItemTextSize(int textSize) {
        float scaledSizeInPixels = textSize * getResources().getDisplayMetrics().scaledDensity;
        paintB.setTextSize(scaledSizeInPixels);
    }

    public final void setSelectedItemFont(Typeface font) {
        paintB.setTypeface(font);
    }

    public final void setItemTextColor(int color) {
        paintA.setColor(color);
    }

    public final void setItemTextSize(int textSize) {
        float scaledSizeInPixels = textSize * getResources().getDisplayMetrics().scaledDensity;
        paintA.setTextSize(scaledSizeInPixels);
    }

    public final void setItemFont(Typeface font) {
        paintA.setTypeface(font);
    }

    public final void setIndicatorColor(int color) {
        paintC.setColor(color);
    }

    public final void setIndicatorWidth(int width) {
        paintC.setStrokeWidth(width);
    }

    public final void hideIndicator() {
        paintC.setColor(Color.TRANSPARENT);
    }

    public final void setSelectedItem(int position) {
        totalScrollY = (int) ((float) (position - initPosition) * (lineSpacingMultiplier * maxTextHeight));
        invalidate();
        smoothScroll();
    }

    @Override
    public void onPopulateAccessibilityEvent (AccessibilityEvent event) {
        super.onPopulateAccessibilityEvent(event);
        event.getText().add((String)arrayList.get(selectedItem));
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        boolean completed = super.dispatchPopulateAccessibilityEvent(event);
        event.getText().add((String)arrayList.get(selectedItem));
        return completed;
    }

    @Override
    public AccessibilityNodeProvider getAccessibilityNodeProvider() {
        if (mAccessibilityNodeProvider == null) {
            mAccessibilityNodeProvider = new VirtualDescendantsProvider();
        }
        return mAccessibilityNodeProvider;
    }

    private class VirtualDescendantsProvider extends AccessibilityNodeProvider {
        @Override
        public AccessibilityNodeInfo createAccessibilityNodeInfo(int virtualViewId) {
            AccessibilityNodeInfo info = null;
            LoopView root = LoopView.this;

            if (virtualViewId == View.NO_ID) {
                info = AccessibilityNodeInfo.obtain(root);
                onInitializeAccessibilityNodeInfo(info);

                int itemIndex = root.getSelectedItem();
                int childCount = root.arrayList != null ? root.arrayList.size() : 0;
                int sideSize = (root.itemCount % 2 == 0  ? root.itemCount : (root.itemCount - 1))/2;
                int minIndex = Math.max(itemIndex - sideSize,0);
                int maxIndex = Math.min(itemIndex + sideSize, childCount-1);

                for (int i = minIndex; i <= maxIndex; ++i) {
                    info.addChild(root, i);
                }
            } else {
                info = AccessibilityNodeInfo.obtain();
                info.setClassName(root.getClass().getName() + "Item");
                info.setPackageName(getContext().getPackageName());
                info.setSource(root, virtualViewId);

                // A Naive computation of bounds per item, by dividing global space
                // to slots per itemsCount, and figuring out the right position
                // as offset from the selected item, which is the center
                int itemIndex = root.getSelectedItem();
                int childCount = root.arrayList != null ? root.arrayList.size() : 0;
                int sideSize = (root.itemCount % 2 == 0  ? root.itemCount : (root.itemCount - 1))/2;
                int minIndex = Math.max(itemIndex - sideSize,0);
                int maxIndex = Math.min(itemIndex + sideSize, childCount-1);
                boolean isInView = (virtualViewId>= minIndex && virtualViewId<=maxIndex);

                Rect r = new Rect();
                boolean isVisible = isInView && root.getGlobalVisibleRect(r);

                // for some reason, while showing 5 items (like ios) itemCount returns 7
                // this sets visible only the middle 5 (maximum)
                isVisible = isVisible && (Math.abs(virtualViewId - itemIndex) <= 2 );

                if(isInView) {
                    int itemHeight = (int) Math.round(r.height()/ Math.max(root.itemCount, 5) * 1.2);
                    r.top += ((virtualViewId - itemIndex + sideSize)*itemHeight) - itemHeight;
                    r.bottom = r.top + itemHeight;
                }
                else {
                    r.top = r.bottom = r.left = r.right = 0;
                }

                info.setBoundsInScreen(r);
                info.setVisibleToUser(isVisible);
                info.setParent(root);
                info.setText((String) root.arrayList.get(virtualViewId));
                info.setSelected(itemIndex == virtualViewId);
                info.setEnabled(true);
            }
            return info;
        }

        @Override
        public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByText(String searched,
                                                                            int virtualViewId) {
            if (TextUtils.isEmpty(searched)) {
                return Collections.emptyList();
            }
            String searchedLowerCase = searched.toLowerCase();
            List<AccessibilityNodeInfo> result = null;
            LoopView root = LoopView.this;

            if (virtualViewId == View.NO_ID) {
                for (int i = 0; i < root.arrayList.size(); i++) {
                    String textToLowerCase = ((String) root.arrayList.get(i)).toLowerCase();
                    if (textToLowerCase.contains(searchedLowerCase)) {
                        if (result == null) {
                            result = new ArrayList<AccessibilityNodeInfo>();
                        }
                        result.add(createAccessibilityNodeInfo(i));
                    }
                }
            } else {
                String textToLowerCase = ((String) root.arrayList.get(virtualViewId)).toLowerCase();
                if (textToLowerCase.contains(searchedLowerCase)) {
                    result = new ArrayList<AccessibilityNodeInfo>();
                    result.add(createAccessibilityNodeInfo(virtualViewId));
                }
            }
            if (result == null) {
                return Collections.emptyList();
            }
            return result;
        }

        @Override
        public boolean performAction(int virtualViewId, int action, Bundle arguments) {

            LoopView root = LoopView.this;

            if (virtualViewId == View.NO_ID) {
                switch (action) {
                    // Allowing lowercase search as in the implementation of findAccessibilityNodeInfosByText
                    case AccessibilityNodeInfo.ACTION_SET_TEXT:
                        CharSequence chars = arguments.getCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE);
                        String searched = chars.toString();
                        if(TextUtils.isEmpty(searched))
                            return true; // ignore empty text
                        String searchedLowerCase = searched.toLowerCase();
                        for (int i = 0; i < root.arrayList.size(); i++) {
                            String textToLowerCase = ((String)root.arrayList.get(i)).toLowerCase();
                            if (textToLowerCase.contains(searchedLowerCase)) {
                                root.setSelectedItem(i);
                                break;
                            }
                        }
                        return true;
                    default:
                        return root.performAccessibilityAction(action, arguments);
                }
            }

            return false;
        }
    }

}

