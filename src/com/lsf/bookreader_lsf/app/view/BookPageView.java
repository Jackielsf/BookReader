package com.lsf.bookreader_lsf.app.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Region;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

public class BookPageView extends View {
	private static final String TAG = "BookPageView";

	private int viewWidth;
	private int viewHeight;

	private int cornerX = 0; // 拖拽点对应的页脚
	private int cornerY = 0;

	private Path mPath0;
	private Path mPath1;

	Bitmap currentPageBitmap = null;
	Bitmap nextPageBitmap = null;

	PointF touchPoint = new PointF(); // 拖拽点
	PointF mBezierStart1 = new PointF(); // 贝塞尔曲线起始点
	PointF mBezierControl1 = new PointF(); // 贝塞尔曲线控制点
	PointF mBeziervertex1 = new PointF(); // 贝塞尔曲线顶点
	PointF mBezierEnd1 = new PointF(); // 贝塞尔曲线结束点

	PointF mBezierStart2 = new PointF(); // 另一条贝塞尔曲线
	PointF mBezierControl2 = new PointF();
	PointF mBeziervertex2 = new PointF();
	PointF mBezierEnd2 = new PointF();

	float mMiddleX;
	float mMiddleY;
	float mDegrees;
	float touchToCornerDistance;
	ColorMatrixColorFilter colorMatrixFilter;
	Matrix mMatrix;
	float[] mMatrixArray = { 0, 0, 0, 0, 0, 0, 0, 0, 1.0f };

	boolean isRightUp_LeftDown; // 是否属于右上左下
	private boolean canDragOver; // 是否可以翻頁
	public boolean isCanDragOver() {
		return canDragOver;
	}
	
	// 返回 sqrt(x2 +y2)，没有中间溢出或下溢。
	float mMaxLength = (float) Math.hypot(viewWidth, viewHeight);
	int[] mBackShadowColors;
	int[] mFrontShadowColors;

	GradientDrawable mBackShadowDrawableLR;
	GradientDrawable mBackShadowDrawableRL;
	GradientDrawable mFolderShadowDrawableLR;
	GradientDrawable mFolderShadowDrawableRL;

	GradientDrawable mFrontShadowDrawableHBT;
	GradientDrawable mFrontShadowDrawableHTB;
	GradientDrawable mFrontShadowDrawableVLR;
	GradientDrawable mFrontShadowDrawableVRL;

	Paint mPaint;

	Scroller mScroller;

//	public BookPageView(Context context) {
//		super(context);
//		this.viewWidth = 1080;
//		this.viewHeight = 1716;
//		initDate();
//	}
	
	public BookPageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initDate();
	}

	private void initDate() {
		mPath0 = new Path();
		mPath1 = new Path();
		createDrawable();

		mPaint = new Paint();
		mPaint.setStyle(Paint.Style.FILL);

		ColorMatrix colorMatrix = new ColorMatrix();
		float array[] = { 0.55f, 0, 0, 0, 80.0f, 0, 0.55f, 0, 0, 80.0f, 0, 0,
				0.55f, 0, 80.0f, 0, 0, 0, 0.2f, 0 };
		colorMatrix.set(array);
		colorMatrixFilter = new ColorMatrixColorFilter(colorMatrix);

		mMatrix = new Matrix();
		mScroller = new Scroller(getContext());

		touchPoint.x = 0.01f; // 不让x,y为0,否则在点计算时会有问题
		touchPoint.y = 0.01f;
	}

	/**
	 * 计算拖拽点对应的拖拽脚
	 */
	public void calculateCornerXY(float x, float y) {
		if (x <= viewWidth / 2) {
			cornerX = 0;
		} else {
			cornerX = viewWidth;
		}
		if (y <= viewHeight / 2) {
			cornerY = 0;
		} else {
			cornerY = viewHeight;
		}
		if ((cornerX == 0 && cornerY == viewHeight)
				|| (cornerX == viewWidth && cornerY == 0)) {
			isRightUp_LeftDown = true;
		} else {
			isRightUp_LeftDown = false;
		}
	}

	public boolean doTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			touchPoint.x = event.getX();
			touchPoint.y = event.getY();
		}
		if (event.getAction() == MotionEvent.ACTION_UP) {
			if (canReadNextPage(touchPoint.x, touchPoint.y)) {
				startAnimation(1200);
			} else {
				touchPoint.x = cornerX - 0.09f;
				touchPoint.y = cornerY - 0.09f;
			}
			this.postInvalidate();
		}
		return true;
	}

	/**
	 * 求解直线P1P2和直线P3P4的交点坐标
	 */
	public PointF getCrossPoint(PointF P1, PointF P2, PointF P3, PointF P4) {
		PointF CrossP = new PointF();
		// 二元函数通式： y=ax+b
		float a1 = (P2.y - P1.y) / (P2.x - P1.x);
		float b1 = ((P1.x * P2.y) - (P2.x * P1.y)) / (P1.x - P2.x);

		float a2 = (P4.y - P3.y) / (P4.x - P3.x);
		float b2 = ((P3.x * P4.y) - (P4.x * P3.y)) / (P3.x - P4.x);
		CrossP.x = (b2 - b1) / (a1 - a2);
		CrossP.y = a1 * CrossP.x + b1;
		return CrossP;
	}

	/**
	 * 计算贝塞尔曲线的相应点
	 *        mBeziervertex1.x 推导:
	 * ((mBezierStart1.x+mBezierEnd1.x)/2+mBezierControl1.x)/2 化简等价于
	 * (mBezierStart1.x+ 2*mBezierControl1.x+mBezierEnd1.x) / 4
	 */
	private void calculatePoints() {
		mMiddleX = (touchPoint.x + cornerX) / 2;
		mMiddleY = (touchPoint.y + cornerY) / 2;
		mBezierControl1.x = mMiddleX - (cornerY - mMiddleY)
				* (cornerY - mMiddleY) / (cornerX - mMiddleX);
		mBezierControl1.y = cornerY;
		mBezierControl2.x = cornerX;
		mBezierControl2.y = mMiddleY - (cornerX - mMiddleX)
				* (cornerX - mMiddleX) / (cornerY - mMiddleY);

		mBezierStart1.x = mBezierControl1.x - (cornerX - mBezierControl1.x) / 2;
		mBezierStart1.y = cornerY;
		mBezierStart2.x = cornerX;
		mBezierStart2.y = mBezierControl2.y - (cornerY - mBezierControl2.y) / 2;

		mBezierEnd1 = getCrossPoint(touchPoint, mBezierControl1, mBezierStart1, mBezierStart2);
		mBezierEnd2 = getCrossPoint(touchPoint, mBezierControl2, mBezierStart1, mBezierStart2);

		mBeziervertex1.x = (mBezierStart1.x + 2 * mBezierControl1.x + mBezierEnd1.x) / 4;
		mBeziervertex1.y = (2 * mBezierControl1.y + mBezierStart1.y + mBezierEnd1.y) / 4;
		mBeziervertex2.x = (mBezierStart2.x + 2 * mBezierControl2.x + mBezierEnd2.x) / 4;
		mBeziervertex2.y = (2 * mBezierControl2.y + mBezierStart2.y + mBezierEnd2.y) / 4;
		
		touchToCornerDistance = (float) Math.hypot((touchPoint.x - cornerX),
				(touchPoint.y - cornerY));
	}

	private void drawCurrentPageArea(Canvas canvas, Bitmap bitmap, Path path) {
		mPath0.reset();
		mPath0.moveTo(mBezierStart1.x, mBezierStart1.y);
		mPath0.quadTo(mBezierControl1.x, mBezierControl1.y, mBezierEnd1.x,
				mBezierEnd1.y);
		mPath0.lineTo(touchPoint.x, touchPoint.y);
		mPath0.lineTo(mBezierEnd2.x, mBezierEnd2.y);
		mPath0.quadTo(mBezierControl2.x, mBezierControl2.y, mBezierStart2.x,
				mBezierStart2.y);
		mPath0.lineTo(cornerX, cornerY);
		mPath0.close();

		canvas.save();
		canvas.clipPath(path, Region.Op.XOR);
		canvas.drawBitmap(bitmap, 0, 0, null);
		canvas.restore();
	}

	private void drawNextPageAreaAndShadow(Canvas canvas, Bitmap bitmap) {
		mPath1.reset();
		mPath1.moveTo(mBezierStart1.x, mBezierStart1.y);
		mPath1.lineTo(mBeziervertex1.x, mBeziervertex1.y);
		mPath1.lineTo(mBeziervertex2.x, mBeziervertex2.y);
		mPath1.lineTo(mBezierStart2.x, mBezierStart2.y);
		mPath1.lineTo(cornerX, cornerY);
		mPath1.close();

		mDegrees = (float) Math.toDegrees(Math.atan2(mBezierControl1.x
				- cornerX, mBezierControl2.y - cornerY));
		int leftx;
		int rightx;
		GradientDrawable mBackShadowDrawable;
		if (isRightUp_LeftDown) {
			leftx = (int) (mBezierStart1.x);
			rightx = (int) (mBezierStart1.x + touchToCornerDistance / 4);
			mBackShadowDrawable = mBackShadowDrawableLR;
		} else {
			leftx = (int) (mBezierStart1.x - touchToCornerDistance / 4);
			rightx = (int) mBezierStart1.x;
			mBackShadowDrawable = mBackShadowDrawableRL;
		}
		canvas.save();
		canvas.clipPath(mPath0);
		canvas.clipPath(mPath1, Region.Op.INTERSECT);
		canvas.drawBitmap(bitmap, 0, 0, null);
		canvas.rotate(mDegrees, mBezierStart1.x, mBezierStart1.y);
		mBackShadowDrawable.setBounds(leftx, (int) mBezierStart1.y, rightx,
				(int) (mMaxLength + mBezierStart1.y));
		mBackShadowDrawable.draw(canvas);
		canvas.restore();
	}

	public void setBitmaps(Bitmap bm1, Bitmap bm2) {
		currentPageBitmap = bm1;
		nextPageBitmap = bm2;
	}

	public void setScreen(int w, int h) {
		viewWidth = w;
		viewHeight = h;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawColor(0xFFAAAAAA);
		calculatePoints();
		drawCurrentPageArea(canvas, currentPageBitmap, mPath0);   
		//TODO drawCurrentPageArea　函数动画结束后会重画一遍currentPageBitmap，导致显示问题，待解决。
		drawNextPageAreaAndShadow(canvas, nextPageBitmap);
		drawCurrentPageShadow(canvas);
		drawCurrentBackArea(canvas, currentPageBitmap);
	}

	/**
	 * 创建阴影的GradientDrawable
	 */
	private void createDrawable() {
		int[] color = { 0x333333, 0xb0333333 };
		mFolderShadowDrawableRL = new GradientDrawable(
				GradientDrawable.Orientation.RIGHT_LEFT, color);
		mFolderShadowDrawableRL
				.setGradientType(GradientDrawable.LINEAR_GRADIENT);

		mFolderShadowDrawableLR = new GradientDrawable(
				GradientDrawable.Orientation.LEFT_RIGHT, color);
		mFolderShadowDrawableLR
				.setGradientType(GradientDrawable.LINEAR_GRADIENT);

		mBackShadowColors = new int[] { 0xff111111, 0x111111 };
		mBackShadowDrawableRL = new GradientDrawable(
				GradientDrawable.Orientation.RIGHT_LEFT, mBackShadowColors);
		mBackShadowDrawableRL.setGradientType(GradientDrawable.LINEAR_GRADIENT);

		mBackShadowDrawableLR = new GradientDrawable(
				GradientDrawable.Orientation.LEFT_RIGHT, mBackShadowColors);
		mBackShadowDrawableLR.setGradientType(GradientDrawable.LINEAR_GRADIENT);

		mFrontShadowColors = new int[] { 0x80111111, 0x111111 };
		mFrontShadowDrawableVLR = new GradientDrawable(
				GradientDrawable.Orientation.LEFT_RIGHT, mFrontShadowColors);
		mFrontShadowDrawableVLR
				.setGradientType(GradientDrawable.LINEAR_GRADIENT);
		mFrontShadowDrawableVRL = new GradientDrawable(
				GradientDrawable.Orientation.RIGHT_LEFT, mFrontShadowColors);
		mFrontShadowDrawableVRL
				.setGradientType(GradientDrawable.LINEAR_GRADIENT);

		mFrontShadowDrawableHTB = new GradientDrawable(
				GradientDrawable.Orientation.TOP_BOTTOM, mFrontShadowColors);
		mFrontShadowDrawableHTB
				.setGradientType(GradientDrawable.LINEAR_GRADIENT);

		mFrontShadowDrawableHBT = new GradientDrawable(
				GradientDrawable.Orientation.BOTTOM_TOP, mFrontShadowColors);
		mFrontShadowDrawableHBT
				.setGradientType(GradientDrawable.LINEAR_GRADIENT);
	}

	/**
	 * 绘制翻起页的阴影
	 */
	public void drawCurrentPageShadow(Canvas canvas) {
		double degree;
		if (isRightUp_LeftDown) {
			degree = Math.PI
					/ 4
					- Math.atan2(mBezierControl1.y - touchPoint.y, touchPoint.x
							- mBezierControl1.x);
		} else {
			degree = Math.PI
					/ 4
					- Math.atan2(touchPoint.y - mBezierControl1.y, touchPoint.x
							- mBezierControl1.x);
		}
		// 翻起页阴影顶点与touch点的距离
		double d1 = (float) 25 * 1.414 * Math.cos(degree);
		double d2 = (float) 25 * 1.414 * Math.sin(degree);
		float x = (float) (touchPoint.x + d1);
		float y;
		if (isRightUp_LeftDown) {
			y = (float) (touchPoint.y + d2);
		} else {
			y = (float) (touchPoint.y - d2);
		}
		mPath1.reset();
		mPath1.moveTo(x, y);
		mPath1.lineTo(touchPoint.x, touchPoint.y);
		mPath1.lineTo(mBezierControl1.x, mBezierControl1.y);
		mPath1.lineTo(mBezierStart1.x, mBezierStart1.y);
		mPath1.close();
		float rotateDegrees;
		canvas.save();

		canvas.clipPath(mPath0, Region.Op.XOR);
		canvas.clipPath(mPath1, Region.Op.INTERSECT);
		int leftx;
		int rightx;
		GradientDrawable mCurrentPageShadow;
		if (isRightUp_LeftDown) {
			leftx = (int) (mBezierControl1.x);
			rightx = (int) mBezierControl1.x + 25;
			mCurrentPageShadow = mFrontShadowDrawableVLR;
		} else {
			leftx = (int) (mBezierControl1.x - 25);
			rightx = (int) mBezierControl1.x + 1;
			mCurrentPageShadow = mFrontShadowDrawableVRL;
		}

		rotateDegrees = (float) Math.toDegrees(Math.atan2(touchPoint.x
				- mBezierControl1.x, mBezierControl1.y - touchPoint.y));
		canvas.rotate(rotateDegrees, mBezierControl1.x, mBezierControl1.y);
		mCurrentPageShadow.setBounds(leftx,
				(int) (mBezierControl1.y - mMaxLength), rightx,
				(int) (mBezierControl1.y));
		mCurrentPageShadow.draw(canvas);
		canvas.restore();

		mPath1.reset();
		mPath1.moveTo(x, y);
		mPath1.lineTo(touchPoint.x, touchPoint.y);
		mPath1.lineTo(mBezierControl2.x, mBezierControl2.y);
		mPath1.lineTo(mBezierStart2.x, mBezierStart2.y);
		mPath1.close();
		canvas.save();
		canvas.clipPath(mPath0, Region.Op.XOR);
		canvas.clipPath(mPath1, Region.Op.INTERSECT);
		if (isRightUp_LeftDown) {
			leftx = (int) (mBezierControl2.y);
			rightx = (int) (mBezierControl2.y + 25);
			mCurrentPageShadow = mFrontShadowDrawableHTB;
		} else {
			leftx = (int) (mBezierControl2.y - 25);
			rightx = (int) (mBezierControl2.y + 1);
			mCurrentPageShadow = mFrontShadowDrawableHBT;
		}
		rotateDegrees = (float) Math.toDegrees(Math.atan2(mBezierControl2.y
				- touchPoint.y, mBezierControl2.x - touchPoint.x));
		canvas.rotate(rotateDegrees, mBezierControl2.x, mBezierControl2.y);
		float temp;
		if (mBezierControl2.y < 0)
			temp = mBezierControl2.y - viewHeight;
		else
			temp = mBezierControl2.y;

		int MyBook = (int) Math.hypot(mBezierControl2.x, temp);
		if (MyBook > mMaxLength)
			mCurrentPageShadow.setBounds((int) (mBezierControl2.x - 25)
					- MyBook, leftx, (int) (mBezierControl2.x + mMaxLength)
					- MyBook, rightx);
		else
			mCurrentPageShadow.setBounds(
					(int) (mBezierControl2.x - mMaxLength), leftx,
					(int) (mBezierControl2.x), rightx);

		mCurrentPageShadow.draw(canvas);
		canvas.restore();
	}

	/**
	 * 绘制翻起页背面
	 */
	private void drawCurrentBackArea(Canvas canvas, Bitmap bitmap) {
		int i = (int) (mBezierStart1.x + mBezierControl1.x) / 2;
		float f1 = Math.abs(i - mBezierControl1.x);
		int i1 = (int) (mBezierStart2.y + mBezierControl2.y) / 2;
		float f2 = Math.abs(i1 - mBezierControl2.y);
		float f3 = Math.min(f1, f2);
		mPath1.reset();
		mPath1.moveTo(mBeziervertex2.x, mBeziervertex2.y);
		mPath1.lineTo(mBeziervertex1.x, mBeziervertex1.y);
		mPath1.lineTo(mBezierEnd1.x, mBezierEnd1.y);
		mPath1.lineTo(touchPoint.x, touchPoint.y);
		mPath1.lineTo(mBezierEnd2.x, mBezierEnd2.y);
		mPath1.close();
		GradientDrawable mFolderShadowDrawable;
		int left;
		int right;
		if (isRightUp_LeftDown) {
			left = (int) (mBezierStart1.x - 1);
			right = (int) (mBezierStart1.x + f3 + 1);
			mFolderShadowDrawable = mFolderShadowDrawableLR;
		} else {
			left = (int) (mBezierStart1.x - f3 - 1);
			right = (int) (mBezierStart1.x + 1);
			mFolderShadowDrawable = mFolderShadowDrawableRL;
		}
		canvas.save();
		canvas.clipPath(mPath0);
		canvas.clipPath(mPath1, Region.Op.INTERSECT);

		mPaint.setColorFilter(colorMatrixFilter);

		float dis = (float) Math.hypot(cornerX - mBezierControl1.x,
				mBezierControl2.y - cornerY);
		float f8 = (cornerX - mBezierControl1.x) / dis;
		float f9 = (mBezierControl2.y - cornerY) / dis;
		mMatrixArray[0] = 1 - 2 * f9 * f9;
		mMatrixArray[1] = 2 * f8 * f9;
		mMatrixArray[3] = mMatrixArray[1];
		mMatrixArray[4] = 1 - 2 * f8 * f8;
		mMatrix.reset();
		mMatrix.setValues(mMatrixArray);
		mMatrix.preTranslate(-mBezierControl1.x, -mBezierControl1.y);
		mMatrix.postTranslate(mBezierControl1.x, mBezierControl1.y);
		canvas.drawBitmap(bitmap, mMatrix, mPaint);
		// canvas.drawBitmap(bitmap, mMatrix, null);
		mPaint.setColorFilter(null);
		canvas.rotate(mDegrees, mBezierStart1.x, mBezierStart1.y);
		mFolderShadowDrawable.setBounds(left, (int) mBezierStart1.y, right,
				(int) (mBezierStart1.y + mMaxLength));
		mFolderShadowDrawable.draw(canvas);
		canvas.restore();
	}

	public void computeScroll() {
		super.computeScroll();
		if (mScroller.computeScrollOffset()) {
			float x = mScroller.getCurrX();
			float y = mScroller.getCurrY();
			touchPoint.x = x;
			touchPoint.y = y;
			postInvalidate();
		}
	}

	private void startAnimation(int delayMillis) {
		int dx, dy;
		// dx 水平方向滑动的距离，负值会使滚动向左滚动
		// dy 垂直方向滑动的距离，负值会使滚动向上滚动
		if (cornerX > 0) {
			dx = -(int) (viewWidth + touchPoint.x);
		} else {
			dx = (int) (viewWidth - touchPoint.x + viewWidth);
		}
		if (cornerY > 0) {
			dy = (int) (viewHeight - touchPoint.y);
		} else {
			dy = (int) (1 - touchPoint.y); // 防止mTouch.y最终变为0
		}
		mScroller.startScroll((int) touchPoint.x, (int) touchPoint.y, dx, dy,
				delayMillis);
	}

	public void abortAnimation() {
		if (!mScroller.isFinished()) {
			mScroller.abortAnimation();
		}
	}

	public boolean canReadNextPage(float x, float y){
		if (x > (viewWidth / 2 - 250)
				&& x < (viewWidth / 2 + 250)
				&& y > (viewHeight / 2 - 400)
				&& y < (viewHeight / 2 + 400)) {
			return false;
		} else {
			return true;
		}
	}
	
	public boolean canDragOver() {
		if (touchToCornerDistance > viewWidth / 10)
			return true;
		return false;
	}

	/**
	 * 是否从左边翻向右边
	 */
	public boolean DragToRight() {
		if (cornerX > 0)
			return false;
		return true;
	}
	
	public void setViewWidth(int viewWidth) {
		this.viewWidth = viewWidth;
	}

	public void setViewHeight(int viewHeight) {
		this.viewHeight = viewHeight;
	}
}
