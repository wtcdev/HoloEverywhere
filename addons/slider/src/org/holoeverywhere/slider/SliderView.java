
package org.holoeverywhere.slider;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.slider.DrawerView.Drawer;
import org.holoeverywhere.widget.Scroller;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

public class SliderView extends ViewGroup implements ISlider, Drawer {
    public static abstract class BaseSlidingDrawer implements SliderDrawer {
        protected SliderView mSlider;

        public void loadResources(Context context, SliderView slider) {
            mSlider = slider;
        }

        @Override
        public void onPostContentDraw(Canvas canvas, int progress, DrawerView view) {

        }

        @Override
        public void onPostLeftDraw(Canvas canvas, int progress, DrawerView view) {

        }

        @Override
        public void onPostRightDraw(Canvas canvas, int progress, DrawerView view) {

        }

        @Override
        public void onPreContentDraw(Canvas canvas, int progress, DrawerView view) {

        }

        @Override
        public void onPreLeftDraw(Canvas canvas, int progress, DrawerView view) {

        }

        @Override
        public void onPreRightDraw(Canvas canvas, int progress, DrawerView view) {

        }
    }

    public static class DefaultSlidingDrawer extends BaseSlidingDrawer {
        protected Interpolator mShadowInterpolator, mTranslateInterpolator;

        @Override
        public void loadResources(Context context, SliderView slider) {
            super.loadResources(context, slider);
            mShadowInterpolator = AnimationUtils.loadInterpolator(context,
                    slider.mShadowInterpolatorRes);
            mTranslateInterpolator = AnimationUtils.loadInterpolator(context,
                    slider.mTranslateInterpolatorRes);
        }

        @Override
        public void onPostLeftDraw(Canvas canvas, int progress, DrawerView view) {
            if (progress < -100 || progress > 0) {
                return;
            }
            final int alpha = (int) (mShadowInterpolator.getInterpolation((100 + progress) / 100f
                    * mSlider.mLeftViewShadow) * 0xFF);
            canvas.drawColor(alpha << 24 | mSlider.mLeftViewShadowColor);
        }

        @Override
        public void onPostRightDraw(Canvas canvas, int progress, DrawerView view) {
            if (progress < 0 || progress > 100) {
                return;
            }
            final int alpha = (int) (mShadowInterpolator.getInterpolation((100 - progress) / 100f
                    * mSlider.mRightViewShadow) * 0xFF);
            canvas.drawColor(alpha << 24 | mSlider.mRightViewShadowColor);
        }

        @Override
        public void onPreLeftDraw(Canvas canvas, int progress, DrawerView view) {
            if (progress < -100 || progress > 0) {
                return;
            }
            canvas.translate(view.getWidth() * mSlider.mLeftTranslateFactor
                    * mTranslateInterpolator.getInterpolation((100 + progress) / 100f), 0);
        }

        @Override
        public void onPreRightDraw(Canvas canvas, int progress, DrawerView view) {
            if (progress < 0 || progress > 100) {
                return;
            }
            canvas.translate(view.getWidth() * -mSlider.mRightTranslateFactor
                    * mTranslateInterpolator.getInterpolation((100 - progress) / 100f), 0);
        }
    }

    public interface OnSlideListener {
        public void onContentShowed();

        public void onLeftShowed();

        public void onRightShowed();
    }

    public static class SavedState extends BaseSavedState {
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        int currentState;

        public SavedState(Parcel source) {
            super(source);
            currentState = source.readInt();
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(currentState);
        }
    }

    public interface SliderDrawer {
        public void onPostContentDraw(Canvas canvas, int progress, DrawerView view);

        public void onPostLeftDraw(Canvas canvas, int progress, DrawerView view);

        public void onPostRightDraw(Canvas canvas, int progress, DrawerView view);

        public void onPreContentDraw(Canvas canvas, int progress, DrawerView view);

        public void onPreLeftDraw(Canvas canvas, int progress, DrawerView view);

        public void onPreRightDraw(Canvas canvas, int progress, DrawerView view);
    }

    public static enum TouchMode {
        /**
         * Intercept touch events by full screen. Make unavailable some touch
         * widgets, like switch or progress bar
         */
        Fullscreen,
        /**
         * Only left side, even if right view was setted
         */
        Left,
        /**
         * Double side
         */
        LeftRight,
        /**
         * Doesn't process any touches
         */
        None,
        /**
         * Only right side, even if left view was setted
         */
        Right;
    }

    private static final int DRAG_CLOSE = 3;
    private static final int DRAG_IDLE = 0;
    private static final int DRAG_NOP = 2;
    private static final int DRAG_PERFORM = 1;
    private static final int SHADOW_COLOR_MASK = 0x00FFFFFF;
    private static final int STATE_CONTENT_OPENED = 0;
    private static final int STATE_LEFT_OPENED = 1;
    private static final int STATE_RIGHT_OPENED = 2;
    private boolean mBlockLongMove = true;
    private int mCurrentState = STATE_CONTENT_OPENED;
    private final float[] mDownPoint = new float[2];
    private float mDraggingOffset;
    private int mDragState = DRAG_IDLE;
    private SliderDrawer mDrawer;
    private boolean mDrawerSetted = false;
    private int mLeftDragBound;
    private float mLeftTranslateFactor;
    private View mLeftView, mRightView, mContentView;
    private float mLeftViewShadow;
    private int mLeftViewShadowColor;
    private int mLeftViewWidth;
    private boolean mLeftViewWidthSetted = false, mRightViewWidthSetted = false;
    private OnSlideListener mOnSlideListener;
    private boolean mOverlayActionBar = false;
    private int mRightDragBound;
    private float mRightTranslateFactor;
    private float mRightViewShadow;
    private int mRightViewShadowColor;
    private int mRightViewWidth;
    private final Scroller mScroller;
    private int mScrollOnLayoutTarget = -1;
    private final int mShadowInterpolatorRes, mTranslateInterpolatorRes;
    private Runnable mShowContentRunnable;
    private final Rect mTempRect = new Rect();
    private TouchMode mTouchMode = TouchMode.LeftRight;
    private int mTouchModeLeftMargin;

    private int mTouchModeRightMargin;
    private final int mTouchSlop;

    private final ViewConfiguration mViewConfiguration;

    public SliderView(Context context) {
        this(context, null);
    }

    public SliderView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.sliderStyle);
    }

    public SliderView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Slider,
                defStyle, R.style.Holo_Slider);

        mScroller = new Scroller(context,
                AnimationUtils.loadInterpolator(context,
                        a.getResourceId(R.styleable.Slider_scrollInterpolator,
                                R.interpolator.decelerate_quad)), false);

        final int dragBound = getPercentValue(a, R.styleable.Slider_dragBound, 20);
        setLeftDragBound(getPercentValue(a, R.styleable.Slider_leftDragBound, dragBound));
        setRightDragBound(getPercentValue(a, R.styleable.Slider_rightDragBound, dragBound));

        final float shadow = getPercentValue(a, R.styleable.Slider_shadow, .8f);
        setLeftViewShadow(getPercentValue(a, R.styleable.Slider_leftShadow, shadow));
        setRightViewShadow(getPercentValue(a, R.styleable.Slider_rightShadow, shadow));

        final int shadowColor = a.getColor(R.styleable.Slider_shadowColor, 0x1E1E1E);
        setLeftViewShadowColor(a.getColor(R.styleable.Slider_leftShadowColor, shadowColor));
        setRightViewShadowColor(a.getColor(R.styleable.Slider_rightShadowColor, shadowColor));

        final float translateFactor = getPercentValue(a, R.styleable.Slider_translateFactor, .3f);
        setLeftTranslateFactor(getPercentValue(a, R.styleable.Slider_leftTranslateFactor,
                translateFactor));
        setRightTranslateFactor(getPercentValue(a, R.styleable.Slider_rightTranslateFactor,
                translateFactor));

        final int margin = a.getDimensionPixelSize(R.styleable.Slider_touchModeMargin, 0);
        setTouchModeLeftMargin(a.getDimensionPixelSize(R.styleable.Slider_touchModeLeftMargin,
                margin));
        setTouchModeRightMargin(a.getDimensionPixelSize(R.styleable.Slider_touchModeRightMargin,
                margin));

        mShadowInterpolatorRes = a.getResourceId(R.styleable.Slider_shadowInterpolator,
                R.interpolator.decelerate_quint);
        mTranslateInterpolatorRes = a.getResourceId(R.styleable.Slider_translateInterpolator,
                R.interpolator.linear);

        if (a.hasValue(R.styleable.Slider_touchMode)) {
            switch (a.getInt(R.styleable.Slider_touchMode, -1)) {
                case 0:
                    setTouchMode(TouchMode.None);
                    break;
                case 1:
                    setTouchMode(TouchMode.Left);
                    break;
                case 2:
                    setTouchMode(TouchMode.Right);
                    break;
                default:
                case 3:
                    setTouchMode(TouchMode.LeftRight);
                    break;
                case 4:
                    setTouchMode(TouchMode.Fullscreen);
                    break;
            }
        }

        a.recycle();

        mViewConfiguration = ViewConfiguration.get(context);
        mTouchSlop = mViewConfiguration.getScaledTouchSlop();
    }

    private void attachView(View view, boolean matchParentWidth) {
        if (view == null) {
            return;
        }
        if (view.getParent() != this) {
            if (view.getParent() != null) {
                ((ViewGroup) view.getParent()).removeView(view);
            }
            addViewInLayout(view, -1, obtainParams(matchParentWidth));
        }
    }

    private void attachView(View view, int width, boolean matchParentWidth) {
        if (view == null) {
            return;
        }
        attachView(view, matchParentWidth);
        final int pWidth = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        final int pHeight = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
        view.measure(MeasureSpec.makeMeasureSpec(width > 0 && width < pWidth ? width : pWidth,
                matchParentWidth || width > 0 ? MeasureSpec.EXACTLY : MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(pHeight, MeasureSpec.EXACTLY));
    }

    protected int computeDelay(int dX) {
        return Math.max(100, Math.min(800, Math.abs(dX) * 2));
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        }
    }

    private boolean contains(View view, float x, float y) {
        if (view == null) {
            return false;
        }
        view.getHitRect(mTempRect);
        return mTempRect.contains((int) x + getScrollX(), (int) y + getScrollY());
    }

    private boolean contains(View view, MotionEvent ev) {
        return contains(view, ev.getX(), ev.getY());
    }

    @Override
    public void disableShadow() {
        setLeftViewShadow(0);
        setRightViewShadow(0);
    }

    public void dispatchRestoreInstanceState(SavedState state) {
        onRestoreInstanceState(state);
    }

    public SavedState dispatchSaveInstanceState() {
        return (SavedState) onSaveInstanceState();
    }

    @Override
    protected boolean fitSystemWindows(Rect insets) {
        if (!mOverlayActionBar) {
            setPadding(insets.left, insets.top, insets.right, insets.bottom);
        }
        return true;
    }

    @Override
    public View getContentView() {
        return mContentView;
    }

    @Override
    public SliderDrawer getDrawer() {
        return mDrawer;
    }

    @Override
    public int getLeftDragBound() {
        return mLeftDragBound;
    }

    @Override
    public float getLeftTranslateFactor() {
        return mLeftTranslateFactor;
    }

    @Override
    public View getLeftView() {
        return mLeftView;
    }

    public float getLeftViewShadow() {
        return mLeftViewShadow;
    }

    @Override
    public int getLeftViewShadowColor() {
        return mLeftViewShadowColor;
    }

    @Override
    public int getLeftViewWidth() {
        return mLeftViewWidth;
    }

    @Override
    public OnSlideListener getOnSlideListener() {
        return mOnSlideListener;
    }

    private float getPercentValue(TypedArray a, int id, float defValue) {
        if (!a.hasValue(id)) {
            return defValue;
        }
        TypedValue value = a.peekValue(id);
        if (value.type == TypedValue.TYPE_FRACTION) {
            return a.getFraction(id, 1, 1, defValue);
        } else {
            return a.getFloat(id, defValue);
        }
    }

    private int getPercentValue(TypedArray a, int id, int defValue) {
        if (!a.hasValue(id)) {
            return defValue;
        }
        TypedValue value = a.peekValue(id);
        if (value.type == TypedValue.TYPE_FRACTION) {
            return (int) (a.getFraction(id, 1, 1, defValue / 100f) * 100f);
        } else {
            return a.getInteger(id, defValue);
        }
    }

    @Override
    public int getProgress() {
        final int x = getScrollX() * 100;
        if (x < 0) {
            return x / mLeftViewWidth;
        } else if (x > 0) {
            return x / mRightViewWidth;
        } else {
            return 0;
        }
    }

    @Override
    public int getRightDragBound() {
        return mRightDragBound;
    }

    @Override
    public float getRightTranslateFactor() {
        return mRightTranslateFactor;
    }

    @Override
    public View getRightView() {
        return mRightView;
    }

    public float getRightViewShadow() {
        return mRightViewShadow;
    }

    @Override
    public int getRightViewShadowColor() {
        return mRightViewShadowColor;
    }

    @Override
    public int getRightViewWidth() {
        return mRightViewWidth;
    }

    @Override
    public TouchMode getTouchMode() {
        return mTouchMode;
    }

    @Override
    public int getTouchModeLeftMargin() {
        return mTouchModeLeftMargin;
    }

    @Override
    public int getTouchModeRightMargin() {
        return mTouchModeRightMargin;
    }

    @Override
    public boolean isBlockLongMove() {
        return mBlockLongMove;
    }

    @Override
    public boolean isContentShowed() {
        return mCurrentState == STATE_CONTENT_OPENED;
    }

    @Override
    public boolean isLeftShowed() {
        return mCurrentState == STATE_LEFT_OPENED;
    }

    @Override
    public boolean isOverlayActionBar() {
        return mOverlayActionBar;
    }

    @Override
    public boolean isRightShowed() {
        return mCurrentState == STATE_RIGHT_OPENED;
    }

    private int obtainDragBound(boolean right, boolean invert) {
        int i = right ? mRightDragBound : mLeftDragBound;
        return invert ? 100 - i : i;
    }

    private LayoutParams obtainParams(boolean b) {
        LayoutParams params = new LayoutParams(b ? MATCH_PARENT : WRAP_CONTENT, MATCH_PARENT);
        return params;
    }

    private View obtainView(int id) {
        return obtainView(findViewById(id));
    }

    private View obtainView(View view) {
        if (view == null) {
            return null;
        }
        if (view.getParent() != null) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
        if (!(view instanceof DrawerView)) {
            DrawerView drawer = new DrawerView(getContext());
            drawer.addView(view);
            drawer.setDrawer(this);
            drawer.setClickable(true);
            return drawer;
        }
        return view;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mLeftView = obtainView(R.id.leftView);
        mRightView = obtainView(R.id.rightView);
        mContentView = obtainView(R.id.contentView);
        removeAllViews();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (MotionEventCompat.getActionMasked(ev)) {
            case MotionEvent.ACTION_DOWN:
                mDownPoint[0] = ev.getX();
                mDownPoint[1] = ev.getY();
                if (mCurrentState != STATE_CONTENT_OPENED) {
                    if (contains(mContentView, ev)) {
                        mDragState = DRAG_CLOSE;
                        return true;
                    } else {
                        mDragState = DRAG_NOP;
                        return false;
                    }
                }
                switch (mTouchMode) {
                    case None:
                        mDragState = DRAG_NOP;
                        break;
                    case Left:
                    case Right:
                    case LeftRight:
                        mDragState = DRAG_NOP;
                        if ((mTouchMode == TouchMode.Left || mTouchMode == TouchMode.LeftRight)
                                && mDownPoint[0] <= mTouchModeLeftMargin
                                && mLeftView != null) {
                            mDragState = DRAG_IDLE;
                        }
                        if ((mTouchMode == TouchMode.Right || mTouchMode == TouchMode.LeftRight)
                                && mDownPoint[0] >= getWidth() - mTouchModeLeftMargin
                                && mRightView != null) {
                            mDragState = DRAG_IDLE;
                        }
                        break;
                    case Fullscreen:
                        mDragState = DRAG_IDLE;
                        break;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float dX = ev.getX() - mDownPoint[0];
                float dY = ev.getY() - mDownPoint[1];
                switch (mDragState) {
                    case DRAG_IDLE:
                        if (Math.sqrt(dX * dX + dY * dY) >= mTouchSlop) {
                            if (Math.abs(dX) < Math.abs(dY)) {
                                mDragState = DRAG_NOP;
                            } else {
                                if (dX > 0 && mLeftView != null || dX < 0 && mRightView != null) {
                                    mDragState = DRAG_PERFORM;
                                    mDraggingOffset = mDownPoint[0];
                                    return true;
                                } else {
                                    mDragState = DRAG_NOP;
                                }
                            }
                        }
                        break;
                }
                break;
        }
        return false;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        l += getPaddingLeft();
        t += getPaddingTop();
        r -= getPaddingRight();
        b -= getPaddingBottom();
        if (mContentView != null) {
            mContentView.layout(l, t, r, b);
        }
        if (mLeftView != null) {
            mLeftView.layout(l - mLeftViewWidth, t, l, b);
        }
        if (mRightView != null) {
            mRightView.layout(r, t, r + mRightViewWidth, b);
        }
        if (mScrollOnLayoutTarget > 0) {
            switch (mScrollOnLayoutTarget) {
                case STATE_CONTENT_OPENED:
                    showContentView(false);
                    break;
                case STATE_LEFT_OPENED:
                    showLeftView(false);
                    break;
                case STATE_RIGHT_OPENED:
                    showRightView(false);
                    break;
            }
            mScrollOnLayoutTarget = -1;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        attachView(mContentView, -1, true);
        attachView(mLeftView, mLeftViewWidthSetted ? mLeftViewWidth : -1, false);
        attachView(mRightView, mRightViewWidthSetted ? mRightViewWidth : -1, false);
        if (mLeftView != null && !mLeftViewWidthSetted) {
            mLeftViewWidth = mLeftView.getMeasuredWidth();
        }
        if (mRightView != null && !mRightViewWidthSetted) {
            mRightViewWidth = mRightView.getMeasuredWidth();
        }
    }

    @Override
    public void onPostDraw(DrawerView view, Canvas canvas) {
        if (mDrawer == null) {
            if (mDrawerSetted) {
                return;
            }
            setDrawer(new DefaultSlidingDrawer());
        }
        if (view == mContentView) {
            mDrawer.onPostContentDraw(canvas, getProgress(), view);
        } else if (view == mLeftView) {
            mDrawer.onPostLeftDraw(canvas, getProgress(), view);
        } else if (view == mRightView) {
            mDrawer.onPostRightDraw(canvas, getProgress(), view);
        }
    }

    @Override
    public void onPreDraw(DrawerView view, Canvas canvas) {
        if (mDrawer == null) {
            if (mDrawerSetted) {
                return;
            }
            setDrawer(new DefaultSlidingDrawer());
        }
        if (view == mContentView) {
            mDrawer.onPreContentDraw(canvas, getProgress(), view);
        } else if (view == mLeftView) {
            mDrawer.onPreLeftDraw(canvas, getProgress(), view);
        } else if (view == mRightView) {
            mDrawer.onPreRightDraw(canvas, getProgress(), view);
        }
    }

    @Override
    protected void onRestoreInstanceState(Parcelable sState) {
        SavedState state = (SavedState) sState;
        super.onRestoreInstanceState(state.getSuperState());
        mScrollOnLayoutTarget = state.currentState;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState state = new SavedState(super.onSaveInstanceState());
        state.currentState = mCurrentState;
        return state;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (MotionEventCompat.getActionMasked(event)) {
            case MotionEvent.ACTION_DOWN:
                switch (mDragState) {
                    case DRAG_CLOSE:
                        return contains(mContentView, event);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                switch (mDragState) {
                    case DRAG_CLOSE:
                        float dX = event.getX() - mDownPoint[0];
                        float dY = event.getY() - mDownPoint[1];
                        if (Math.sqrt(dX * dX + dY * dY) >= mTouchSlop) {
                            if (Math.abs(dX / 2) > Math.abs(dY)
                                    && (mCurrentState == STATE_LEFT_OPENED && dX < 0
                                    || mCurrentState == STATE_RIGHT_OPENED && dX > 0)) {
                                mDragState = DRAG_PERFORM;
                                mDraggingOffset = mDownPoint[0];
                            } else {
                                mDragState = DRAG_NOP;
                            }
                        }
                        return true;
                    case DRAG_PERFORM:
                        int x = (int) (mDraggingOffset - event.getX() + getScrollX());
                        mDraggingOffset = event.getX();
                        x = Math.max(x, mCurrentState == STATE_RIGHT_OPENED && mBlockLongMove ? 0
                                : -mLeftViewWidth);
                        x = Math.min(x, mCurrentState == STATE_LEFT_OPENED && mBlockLongMove ? 0
                                : mRightViewWidth);
                        scrollTo(x, 0);
                        return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mDragState == DRAG_CLOSE) {
                    showContentView(true);
                    mDragState = DRAG_IDLE;
                    return true;
                }
            case MotionEvent.ACTION_OUTSIDE:
            case MotionEvent.ACTION_CANCEL:
                int x = getScrollX();
                if (x < 0) { // left pane
                    if (mCurrentState != STATE_RIGHT_OPENED
                            && x < mLeftViewWidth / -100f
                                    * obtainDragBound(false, mCurrentState == STATE_LEFT_OPENED)) {
                        showLeftView(true);
                    } else {
                        showContentView(true);
                    }
                } else if (x > 0) { // right pane
                    if (mCurrentState != STATE_LEFT_OPENED
                            && x > mRightViewWidth / 100f
                                    * obtainDragBound(true, mCurrentState == STATE_RIGHT_OPENED)) {
                        showRightView(true);
                    } else {
                        showContentView(true);
                    }
                } else {
                    showContentView(true);
                }
                return true;
        }
        return false;
    }

    private void scrollTo(int x, boolean smooth) {
        if (smooth) {
            final int startX = getScrollX();
            final int dX = x - startX;
            if (dX != 0) {
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                mScroller.startScroll(startX, 0, dX, 0, computeDelay(dX));
                postInvalidate();
            }
        } else {
            scrollTo(x, 0);
        }
    }

    @Override
    public void setBlockLongMove(boolean blockLongMove) {
        mBlockLongMove = blockLongMove;
    }

    @Override
    public void setContentView(int layoutId) {
        setContentView(LayoutInflater.inflate(getContext(), layoutId, this, false));
    }

    @Override
    public void setContentView(View view) {
        if (view == mContentView) {
            return;
        }
        if (mContentView != null) {
            removeViewInLayout(mContentView);
        }
        attachView(mContentView = obtainView(view), true);
        requestLayout();
    }

    @Override
    public void setDragBound(int dragBound) {
        setLeftDragBound(dragBound);
        setRightDragBound(dragBound);
    }

    @Override
    public void setDrawer(SliderDrawer drawer) {
        mDrawerSetted = true;
        if (mDrawer == drawer) {
            return;
        }
        mDrawer = drawer;
        if (mDrawer instanceof BaseSlidingDrawer) {
            ((BaseSlidingDrawer) mDrawer).loadResources(getContext(), this);
        }
    }

    @Override
    public void setLeftDragBound(int leftDragBound) {
        mLeftDragBound = leftDragBound;
    }

    @Override
    public void setLeftTranslateFactor(float leftTranslateFactor) {
        mLeftTranslateFactor = leftTranslateFactor;
        postInvalidate();
    }

    @Override
    public void setLeftView(int layoutId) {
        setLeftView(LayoutInflater.inflate(getContext(), layoutId, this, false));
    }

    @Override
    public void setLeftView(View view) {
        if (view == mLeftView) {
            return;
        }
        if (mLeftView != null) {
            removeViewInLayout(mLeftView);
        }
        attachView(mLeftView = obtainView(view), false);
        requestLayout();
    }

    public void setLeftViewShadow(float leftViewShadow) {
        mLeftViewShadow = leftViewShadow;
        postInvalidate();
    }

    @Override
    public void setLeftViewShadowColor(int leftViewShadowColor) {
        mLeftViewShadowColor = leftViewShadowColor & SHADOW_COLOR_MASK;
    }

    @Override
    public void setLeftViewWidth(int leftViewWidth) {
        mLeftViewWidth = leftViewWidth;
        mLeftViewWidthSetted = leftViewWidth > 0;
        requestLayout();
        postInvalidate();
    }

    @Override
    public void setOnSlideListener(OnSlideListener onSlideListener) {
        mOnSlideListener = onSlideListener;
    }

    @Override
    public void setOverlayActionBar(boolean overlayActionBar) {
        mOverlayActionBar = overlayActionBar;
    }

    @Override
    public void setProgress(int progress) {
        progress = Math.max(-100, Math.min(100, progress));
        if (progress < 0) {
            show(STATE_LEFT_OPENED, false, false, progress * mLeftViewWidth / 100);
        } else if (progress > 0) {
            show(STATE_RIGHT_OPENED, false, false, progress * mRightViewWidth / 100);
        } else {
            show(STATE_CONTENT_OPENED, false, false);
        }
    }

    @Override
    public void setRightDragBound(int rightDragBound) {
        mRightDragBound = rightDragBound;
    }

    @Override
    public void setRightTranslateFactor(float rightTranslateFactor) {
        mRightTranslateFactor = rightTranslateFactor;
        postInvalidate();
    }

    @Override
    public void setRightView(int layoutId) {
        setRightView(LayoutInflater.inflate(getContext(), layoutId, this, false));
    }

    @Override
    public void setRightView(View view) {
        if (view == mRightView) {
            return;
        }
        if (mRightView != null) {
            removeViewInLayout(mRightView);
        }
        attachView(mRightView = obtainView(view), false);
        requestLayout();
    }

    public void setRightViewShadow(float rightViewShadow) {
        mRightViewShadow = rightViewShadow;
        postInvalidate();
    }

    @Override
    public void setRightViewShadowColor(int rightViewShadowColor) {
        mRightViewShadowColor = rightViewShadowColor & SHADOW_COLOR_MASK;
    }

    @Override
    public void setRightViewWidth(int rightViewWidth) {
        mRightViewWidth = rightViewWidth;
        mRightViewWidthSetted = rightViewWidth > 0;
        requestLayout();
        postInvalidate();
    }

    @Override
    public void setShadowColor(int shadowColor) {
        setLeftViewShadowColor(shadowColor);
        setRightViewShadowColor(shadowColor);
    }

    @Override
    public void setTouchMode(TouchMode touchMode) {
        if (touchMode == null) {
            throw new IllegalArgumentException();
        }
        mTouchMode = touchMode;
    }

    @Override
    public void setTouchModeLeftMargin(int touchModeLeftMargin) {
        mTouchModeLeftMargin = touchModeLeftMargin;
    }

    @Override
    public void setTouchModeMargin(int touchModeMargin) {
        setTouchModeLeftMargin(touchModeMargin);
        setTouchModeRightMargin(touchModeMargin);
    }

    @Override
    public void setTouchModeRightMargin(int touchModeRightMargin) {
        mTouchModeRightMargin = touchModeRightMargin;
    }

    @Override
    public void setTranslateFactor(float translateFactor) {
        setLeftTranslateFactor(translateFactor);
        setRightTranslateFactor(translateFactor);
    }

    public void setViewShadow(float viewShadow) {
        setLeftViewShadow(viewShadow);
        setRightViewShadow(viewShadow);
    }

    private void show(int newState, boolean smooth, boolean post) {
        switch (newState) {
            case STATE_CONTENT_OPENED:
                show(newState, smooth, post, 0);
                break;
            case STATE_LEFT_OPENED:
                show(newState, smooth, post, -mLeftViewWidth);
                break;
            case STATE_RIGHT_OPENED:
                show(newState, smooth, post, mRightViewWidth);
                break;
        }
    }

    private void show(int newState, boolean smooth, boolean post, int offset) {
        if (getScrollX() != offset || mCurrentState != newState) {
            if (newState == STATE_LEFT_OPENED && mLeftView == null
                    || newState == STATE_RIGHT_OPENED && mRightView == null) {
                show(STATE_CONTENT_OPENED, smooth, post, 0);
                return;
            }
            if (post) {
                mScrollOnLayoutTarget = newState;
                return;
            }
            scrollTo(offset, smooth);
            mCurrentState = newState;
            if (mOnSlideListener != null) {
                switch (newState) {
                    case STATE_CONTENT_OPENED:
                        mOnSlideListener.onContentShowed();
                        break;
                    case STATE_LEFT_OPENED:
                        mOnSlideListener.onLeftShowed();
                        break;
                    case STATE_RIGHT_OPENED:
                        mOnSlideListener.onRightShowed();
                        break;
                }
            }
        }
    }

    @Override
    public void showContentDelayed() {
        if (mShowContentRunnable == null) {
            mShowContentRunnable = new Runnable() {
                @Override
                public void run() {
                    showContentView(true);
                }
            };
        }
        postDelayed(mShowContentRunnable, 100);
    }

    @Override
    public void showContentView(boolean smooth) {
        show(STATE_CONTENT_OPENED, smooth, false);
    }

    @Override
    public void showLeftView(boolean smooth) {
        show(STATE_LEFT_OPENED, smooth, false);
    }

    @Override
    public void showRightView(boolean smooth) {
        show(STATE_RIGHT_OPENED, smooth, false);
    }

    public void postShowContentView() {
        show(STATE_CONTENT_OPENED, false, true);
    }

    public void postShowLeftView() {
        show(STATE_LEFT_OPENED, false, true);
    }

    public void postShowRightView() {
        show(STATE_RIGHT_OPENED, false, true);
    }

    @Override
    public void toggle() {
        if (mCurrentState == STATE_CONTENT_OPENED) {
            showLeftView(true);
        } else {
            showContentView(true);
        }
    }
}
