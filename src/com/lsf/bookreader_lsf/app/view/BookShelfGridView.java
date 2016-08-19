package com.lsf.bookreader_lsf.app.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.GridView;

import com.lsf.bookreader_lsf.app.R;
import com.lsf.bookreader_lsf.app.activity.BookShelfActivity;
import com.lsf.bookreader_lsf.app.utils.ImageUtil;

public class BookShelfGridView extends GridView {
	
	private Bitmap background;

	public BookShelfGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {

		int count = getChildCount();

		background = ImageUtil.getBitmap(getContext(), R.drawable.bookshelf_layer_center, BookShelfActivity.width / 3, BookShelfActivity.height / 3 - 5);
		
		int top = count > 0 ? getChildAt(0).getTop() : 0;
		int backgroundWidth = background.getWidth();
		int backgroundHeight = background.getHeight();

		for (int y = top; y < BookShelfActivity.height; y += backgroundHeight) {
			for (int x = 0; x < BookShelfActivity.width; x += backgroundWidth) {
				canvas.drawBitmap(background, x, y, null);
			}
		}
		super.dispatchDraw(canvas);
	}
}
