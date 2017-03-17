package pk.com.takephotoandsignature.signaturepad.views;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import pk.com.takephotoandsignature.R;
import pk.com.takephotoandsignature.signaturepad.utils.Bezier;
import pk.com.takephotoandsignature.signaturepad.utils.ControlTimedPoints;
import pk.com.takephotoandsignature.signaturepad.utils.SvgBuilder;
import pk.com.takephotoandsignature.signaturepad.utils.TimedPoint;

/**
 * <p> 自定义的view 签字版</p>
 * Created by pukai on 2016-4-27.
 */
public class SignaturePad extends View {
    //View state
    private List<TimedPoint> mPoints;
    private boolean mIsEmpty = true;
    private float mLastTouchX;
    private float mLastTouchY;
    private float mLastVelocity;
    private float mLastWidth;
    private RectF mDirtyRect;
    private final SvgBuilder mSvgBuilder = new SvgBuilder();

    // Cache
    private List<TimedPoint> mPointsCache = new ArrayList<>();
    private ControlTimedPoints mControlTimedPointsCached = new ControlTimedPoints();
    private Bezier mBezierCached = new Bezier();

    //Configurable parameters
    private int mMinWidth;
    private int mMaxWidth;
    private float mVelocityFilterWeight;
    private OnSignedListener mOnSignedListener;
    private boolean mClearOnDoubleClick;
    //Click values
    private long mFirstClick;
    private int mCountClick;
    private static final int DOUBLE_CLICK_DELAY_MS = 200;

    private Paint mPaint = new Paint();
    private Bitmap mSignatureBitmap = null;
    private Canvas mSignatureBitmapCanvas = null;
    private Bitmap mCurrentBitMap = null;

    public SignaturePad(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.SignaturePad,
                0, 0);

        //Configurable parameters
        try {
            mMinWidth = a.getDimensionPixelSize(R.styleable.SignaturePad_minWidth, convertDpToPx(3));
            mMaxWidth = a.getDimensionPixelSize(R.styleable.SignaturePad_maxWidth, convertDpToPx(7));
            mVelocityFilterWeight = a.getFloat(R.styleable.SignaturePad_velocityFilterWeight, 0.9f);
            mPaint.setColor(a.getColor(R.styleable.SignaturePad_penColor, Color.BLACK));
            mClearOnDoubleClick = a.getBoolean(R.styleable.SignaturePad_clearOnDoubleClick, false);
        } finally {
            a.recycle();
        }

        //Fixed parameters
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeJoin(Paint.Join.ROUND);

        //Dirty rectangle to update only the changed portion of the view
        mDirtyRect = new RectF();

        clear();
    }

    public SignaturePad(Context context) {
        super(context);
        //Fixed parameters
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mClearOnDoubleClick = false;
        //Dirty rectangle to update only the changed portion of the view
        mDirtyRect = new RectF();
        clear();
    }

    /**
     * 根据资源ID设置画笔的颜色
     *
     * @param colorRes the color resource.
     */
    public void setPenColorRes(int colorRes) {
        try {
            setPenColor(getResources().getColor(colorRes));
        } catch (Resources.NotFoundException ex) {
            setPenColor(Color.parseColor("#000000"));
        }
    }

    /**
     * 直接设置画笔的颜色
     *
     * @param color the color.
     */
    public void setPenColor(int color) {
        mPaint.setColor(color);
    }

    /**
     * 设置笔画的最小宽度
     *
     * @param minWidth the width in dp.
     */
    public void setMinWidth(float minWidth) {
        mMinWidth = convertDpToPx(minWidth);
    }

    /**
     * 设置笔画的最大宽度
     *
     * @param maxWidth the width in dp.
     */
    public void setMaxWidth(float maxWidth) {
        mMaxWidth = convertDpToPx(maxWidth);
    }

    /**
     * 设置过滤器
     *
     * @param velocityFilterWeight the weight.
     */
    public void setVelocityFilterWeight(float velocityFilterWeight) {
        mVelocityFilterWeight = velocityFilterWeight;
    }

    /**
     * 清除
     */
    public void clear() {
        mSvgBuilder.clear();
        mPoints = new ArrayList<>();
        mLastVelocity = 0;
        mLastWidth = (mMinWidth + mMaxWidth) / 2;

        if (mSignatureBitmap != null) {
            mSignatureBitmap.recycle();
            mSignatureBitmap = null;
            ensureSignatureBitmap();
        }

        setIsEmpty(true);

        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled())
            return false;

        float eventX = event.getX();
        float eventY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                mPoints.clear();
                if (isDoubleClick()) break;
                mLastTouchX = eventX;
                mLastTouchY = eventY;
                addPoint(getNewPoint(eventX, eventY));
                if (mOnSignedListener != null) mOnSignedListener.onStartSigning();

            case MotionEvent.ACTION_MOVE:
                resetDirtyRect(eventX, eventY);
                addPoint(getNewPoint(eventX, eventY));
                break;

            case MotionEvent.ACTION_UP:
                resetDirtyRect(eventX, eventY);
                addPoint(getNewPoint(eventX, eventY));
                getParent().requestDisallowInterceptTouchEvent(true);
                setIsEmpty(false);
                break;

            default:
                return false;
        }

        //invalidate();
        invalidate(
                (int) (mDirtyRect.left - mMaxWidth),
                (int) (mDirtyRect.top - mMaxWidth),
                (int) (mDirtyRect.right + mMaxWidth),
                (int) (mDirtyRect.bottom + mMaxWidth));

        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mSignatureBitmap != null) {
            canvas.drawBitmap(mSignatureBitmap, 0, 0, mPaint);
        }
    }

    public void setOnSignedListener(OnSignedListener listener) {
        mOnSignedListener = listener;
    }

    public boolean isEmpty() {
        return mIsEmpty;
    }

    public String getSignatureSvg() {
        int width = getTransparentSignatureBitmap().getWidth();
        int height = getTransparentSignatureBitmap().getHeight();
        return mSvgBuilder.build(width, height);
    }

    /**
     * 获取当前截图
     *
     * @return
     */
    public Bitmap getSignatureBitmap() {
        Bitmap originalBitmap = getTransparentSignatureBitmap();
        if(originalBitmap==null){
            return null;
        }
        Bitmap whiteBgBitmap = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(whiteBgBitmap);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(originalBitmap, 0, 0, null);
        return whiteBgBitmap;
    }

    private void setSignatureBitmap(Bitmap signature) {
        clear();
        if(getWidth()<=0||getHeight()<=0){
            return;
        }
        ensureSignatureBitmap();
        RectF tempSrc = new RectF();
        RectF tempDst = new RectF();

        int dWidth = signature.getWidth();
        int dHeight = signature.getHeight();
        int vWidth = getWidth();
        int vHeight = getHeight();

        // Generate the required transform.
        tempSrc.set(0, 0, dWidth, dHeight);
        tempDst.set(0, 0, vWidth, vHeight);

        Matrix drawMatrix = new Matrix();
        drawMatrix.setRectToRect(tempSrc, tempDst, Matrix.ScaleToFit.CENTER);
        Canvas canvas = new Canvas(mSignatureBitmap);
        canvas.drawBitmap(signature, drawMatrix, null);
        setIsEmpty(false);
        invalidate();
    }

    /**
     * 设置新的图片并且设置新的尺寸
     *
     * @param signature 新的bitmap
     * @param width     宽的尺寸
     * @param height    高的尺寸
     */
    public void setSignatureBitmap(Bitmap signature, int width, int height) {
        if (mCurrentBitMap != null) {
            mCurrentBitMap = null;
        }
        if (width == getWidth() && height == getHeight()) {
            setSignatureBitmap(signature);
            return;
        }
        mCurrentBitMap = signature;
    }

    public Bitmap getTransparentSignatureBitmap() {
        ensureSignatureBitmap();
        return mSignatureBitmap;
    }

    public Bitmap getTransparentSignatureBitmap(boolean trimBlankSpace) {

        if (!trimBlankSpace) {
            return getTransparentSignatureBitmap();
        }

        ensureSignatureBitmap();

        int imgHeight = mSignatureBitmap.getHeight();
        int imgWidth = mSignatureBitmap.getWidth();

        int backgroundColor = Color.TRANSPARENT;

        int xMin = Integer.MAX_VALUE,
                xMax = Integer.MIN_VALUE,
                yMin = Integer.MAX_VALUE,
                yMax = Integer.MIN_VALUE;

        boolean foundPixel = false;

        // Find xMin
        for (int x = 0; x < imgWidth; x++) {
            boolean stop = false;
            for (int y = 0; y < imgHeight; y++) {
                if (mSignatureBitmap.getPixel(x, y) != backgroundColor) {
                    xMin = x;
                    stop = true;
                    foundPixel = true;
                    break;
                }
            }
            if (stop)
                break;
        }
        // Image is empty...
        if (!foundPixel)
            return null;

        // Find yMin
        for (int y = 0; y < imgHeight; y++) {
            boolean stop = false;
            for (int x = xMin; x < imgWidth; x++) {
                if (mSignatureBitmap.getPixel(x, y) != backgroundColor) {
                    yMin = y;
                    stop = true;
                    break;
                }
            }
            if (stop)
                break;
        }

        // Find xMax
        for (int x = imgWidth - 1; x >= xMin; x--) {
            boolean stop = false;
            for (int y = yMin; y < imgHeight; y++) {
                if (mSignatureBitmap.getPixel(x, y) != backgroundColor) {
                    xMax = x;
                    stop = true;
                    break;
                }
            }
            if (stop)
                break;
        }

        // Find yMax
        for (int y = imgHeight - 1; y >= yMin; y--) {
            boolean stop = false;
            for (int x = xMin; x <= xMax; x++) {
                if (mSignatureBitmap.getPixel(x, y) != backgroundColor) {
                    yMax = y;
                    stop = true;
                    break;
                }
            }
            if (stop)
                break;
        }

        return Bitmap.createBitmap(mSignatureBitmap, xMin, yMin, xMax - xMin, yMax - yMin);
    }

    private boolean isDoubleClick() {
        if (mClearOnDoubleClick) {
            if (mFirstClick != 0 && System.currentTimeMillis() - mFirstClick > DOUBLE_CLICK_DELAY_MS) {
                mCountClick = 0;
            }
            mCountClick++;
            if (mCountClick == 1) {
                mFirstClick = System.currentTimeMillis();
            } else if (mCountClick == 2) {
                long lastClick = System.currentTimeMillis();
                if (lastClick - mFirstClick < DOUBLE_CLICK_DELAY_MS) {
                    this.clear();
                    return true;
                }
            }
        }
        return false;
    }

    private TimedPoint getNewPoint(float x, float y) {
        int mCacheSize = mPointsCache.size();
        TimedPoint timedPoint;
        if (mCacheSize == 0) {
            // Cache is empty, create a new point
            timedPoint = new TimedPoint();
        } else {
            // Get point from cache
            timedPoint = mPointsCache.remove(mCacheSize - 1);
        }

        return timedPoint.set(x, y);
    }

    private void recyclePoint(TimedPoint point) {
        mPointsCache.add(point);
    }

    private void addPoint(TimedPoint newPoint) {
        mPoints.add(newPoint);

        int pointsCount = mPoints.size();
        if (pointsCount > 3) {
            ControlTimedPoints tmp = calculateCurveControlPoints(mPoints.get(0), mPoints.get(1), mPoints.get(2));
            TimedPoint c2 = tmp.getC2();
            recyclePoint(tmp.getC1());

            tmp = calculateCurveControlPoints(mPoints.get(1), mPoints.get(2), mPoints.get(3));
            TimedPoint c3 = tmp.getC1();
            recyclePoint(tmp.getC2());

            Bezier curve = mBezierCached.set(mPoints.get(1), c2, c3, mPoints.get(2));

            TimedPoint startPoint = curve.getStartPoint();
            TimedPoint endPoint = curve.getEndPoint();

            float velocity = endPoint.velocityFrom(startPoint);
            velocity = Float.isNaN(velocity) ? 0.0f : velocity;

            velocity = mVelocityFilterWeight * velocity
                    + (1 - mVelocityFilterWeight) * mLastVelocity;

            // The new width is a function of the velocity. Higher velocities
            // correspond to thinner strokes.
            float newWidth = strokeWidth(velocity);
            // The Bezier's width starts out as last curve's final width, and
            // gradually changes to the stroke width just calculated. The new
            // width calculation is based on the velocity between the Bezier's
            // start and end mPoints.
            if (velocity < 10) {
                addBezier(curve, mLastWidth, newWidth);
            }

            mLastVelocity = velocity;
            mLastWidth = newWidth;

            // Remove the first element from the list,
            // so that we always have no more than 4 mPoints in mPoints array.
            recyclePoint(mPoints.remove(0));

            recyclePoint(c2);
            recyclePoint(c3);

        } else if (pointsCount == 1) {
            // To reduce the initial lag make it work with 3 mPoints
            // by duplicating the first point
            TimedPoint firstPoint = mPoints.get(0);
            mPoints.add(getNewPoint(firstPoint.getX(), firstPoint.getY()));
        }
    }

    private void addBezier(Bezier curve, float startWidth, float endWidth) {
        mSvgBuilder.append(curve, (startWidth + endWidth) / 2);
        ensureSignatureBitmap();
        float originalWidth = mPaint.getStrokeWidth();
        float widthDelta = endWidth - startWidth;
        float drawSteps = (float) Math.floor(curve.length());

        for (int i = 0; i < drawSteps; i++) {
            // Calculate the Bezier (x, y) coordinate for this step.
            float t = ((float) i) / drawSteps;
            float tt = t * t;
            float ttt = tt * t;
            float u = 1 - t;
            float uu = u * u;
            float uuu = uu * u;

            float x = uuu * curve.getStartPoint().getX();
            x += 3 * uu * t * curve.getControl1().getX();
            x += 3 * u * tt * curve.getControl2().getX();
            x += ttt * curve.getEndPoint().getX();

            float y = uuu * curve.getStartPoint().getY();
            y += 3 * uu * t * curve.getControl1().getY();
            y += 3 * u * tt * curve.getControl2().getY();
            y += ttt * curve.getEndPoint().getY();

            // Set the incremental stroke width and draw.
            mPaint.setStrokeWidth(startWidth + ttt * widthDelta);
            mSignatureBitmapCanvas.drawPoint(x, y, mPaint);
            expandDirtyRect(x, y);
        }

        mPaint.setStrokeWidth(originalWidth);
    }

    private ControlTimedPoints calculateCurveControlPoints(TimedPoint s1, TimedPoint s2, TimedPoint s3) {
        float dx1 = s1.getX() - s2.getX();
        float dy1 = s1.getY() - s2.getY();
        float dx2 = s2.getX() - s3.getX();
        float dy2 = s2.getY() - s3.getY();

        float m1X = (s1.getX() + s2.getX()) / 2.0f;
        float m1Y = (s1.getY() + s2.getY()) / 2.0f;
        float m2X = (s2.getX() + s3.getX()) / 2.0f;
        float m2Y = (s2.getY() + s3.getY()) / 2.0f;

        float l1 = (float) Math.sqrt(dx1 * dx1 + dy1 * dy1);
        float l2 = (float) Math.sqrt(dx2 * dx2 + dy2 * dy2);

        float dxm = (m1X - m2X);
        float dym = (m1Y - m2Y);
        float k = l2 / (l1 + l2);
        if (Float.isNaN(k)) k = 0.0f;
        float cmX = m2X + dxm * k;
        float cmY = m2Y + dym * k;

        float tx = s2.getX() - cmX;
        float ty = s2.getY() - cmY;

        return mControlTimedPointsCached.set(getNewPoint(m1X + tx, m1Y + ty), getNewPoint(m2X + tx, m2Y + ty));
    }

    private float strokeWidth(float velocity) {
        return Math.max(mMaxWidth / (velocity + 1), mMinWidth);
    }

    /**
     * Called when replaying history to ensure the dirty region includes all
     * mPoints.
     *
     * @param historicalX the previous x coordinate.
     * @param historicalY the previous y coordinate.
     */
    private void expandDirtyRect(float historicalX, float historicalY) {
        if (historicalX < mDirtyRect.left) {
            mDirtyRect.left = historicalX;
        } else if (historicalX > mDirtyRect.right) {
            mDirtyRect.right = historicalX;
        }
        if (historicalY < mDirtyRect.top) {
            mDirtyRect.top = historicalY;
        } else if (historicalY > mDirtyRect.bottom) {
            mDirtyRect.bottom = historicalY;
        }
    }

    /**
     * Resets the dirty region when the motion event occurs.
     *
     * @param eventX the event x coordinate.
     * @param eventY the event y coordinate.
     */
    private void resetDirtyRect(float eventX, float eventY) {

        // The mLastTouchX and mLastTouchY were set when the ACTION_DOWN motion event occurred.
        mDirtyRect.left = Math.min(mLastTouchX, eventX);
        mDirtyRect.right = Math.max(mLastTouchX, eventX);
        mDirtyRect.top = Math.min(mLastTouchY, eventY);
        mDirtyRect.bottom = Math.max(mLastTouchY, eventY);
    }

    private void setIsEmpty(boolean newValue) {
        mIsEmpty = newValue;
        if (mOnSignedListener != null) {
            if (mIsEmpty) {
                mOnSignedListener.onClear();
            } else {
                mOnSignedListener.onSigned();
            }
        }
    }

    private void ensureSignatureBitmap() {
        if (mSignatureBitmap == null&&getWidth()>0&&getHeight()>0) {
            mSignatureBitmap = Bitmap.createBitmap(getWidth(), getHeight(),
                    Bitmap.Config.ARGB_8888);
            mSignatureBitmapCanvas = new Canvas(mSignatureBitmap);
        }
    }

    private int convertDpToPx(float dp) {
        return Math.round(getContext().getResources().getDisplayMetrics().density * dp);
    }
    public void recycleBitmap() {
        if (mSignatureBitmap != null) {
            mSignatureBitmap.recycle();
            mSignatureBitmap = null;
        }
        if (mCurrentBitMap != null) {
            mCurrentBitMap.recycle();
            mCurrentBitMap = null;
        }
    }

    /**
     * 用户操作的事件监听接口
     */
    public interface OnSignedListener {
        /**
         * 开始签名
         */
        void onStartSigning();

        /**
         * 正在签名
         */
        void onSigned();

        /**
         * 清除
         */
        void onClear();
    }

    /**
     * view的尺寸发生变化时会触发此事件
     *
     * @param w    当前的宽
     * @param h    当前高
     * @param oldw 旧的宽
     * @param oldh 旧的高
     */
    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        // TODO Auto-generated method stub
        if (mCurrentBitMap != null) {
            setSignatureBitmap(mCurrentBitMap);
        }
        super.onSizeChanged(w, h, oldw, oldh);
    }
}
