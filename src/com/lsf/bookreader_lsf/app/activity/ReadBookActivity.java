package com.lsf.bookreader_lsf.app.activity;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Date;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.lsf.bookreader_lsf.app.R;
import com.lsf.bookreader_lsf.app.R.id;
import com.lsf.bookreader_lsf.app.R.layout;
import com.lsf.bookreader_lsf.app.db.Book;
import com.lsf.bookreader_lsf.app.db.BookDB;
import com.lsf.bookreader_lsf.app.service.PlayMusicService;
import com.lsf.bookreader_lsf.app.utils.PatternUtils;
import com.lsf.bookreader_lsf.app.utils.ToastUtils;
import com.lsf.bookreader_lsf.app.view.BookPageFactory;
import com.lsf.bookreader_lsf.app.view.BookPageView;

public class ReadBookActivity extends Activity {

	public static String TAG = "ReadBookActivity";

	private BookDB bookDB;
	private Book book;

	private int screenWidth;
	private int screenHeight;

	private BookPageView mBookPageView;
	private Bitmap mCurPageBitmap, mNextPageBitmap;
	private Canvas mCurPageCanvas, mNextPageCanvas;
	private BookPageFactory pagefactory;
	private long fileLenth = 1L;

	private boolean isShowSetting = false;
	private LinearLayout bookSetting;
	private LinearLayout readProgress;
	private TextView bookProgress;
	private SeekBar progressSetting;
	
	private Button playMusic;
	private Button controlMusic;
	private Button readSetting;
	private boolean isPlayMusic = false;
	private boolean isPauseMusic = false;
	
	private SettingDialog settingdialog;
	private byte pattern;
	private int textColor;
	private int backColor;
	private int fontSize;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.read_book);
		initUI();
		initBookData();
	}

	private void initUI() {
		bookSetting = (LinearLayout) findViewById(R.id.book_setting);
		readProgress = (LinearLayout) findViewById(R.id.readprogress);
		mBookPageView = (BookPageView) findViewById(R.id.bookview);
		initView();
		bookProgress = (TextView) findViewById(R.id.bookprogress);
		progressSetting = (SeekBar) findViewById(R.id.progress_setting);
		progressSetting.setOnSeekBarChangeListener(new OnSeekBarChangeListener());
		playMusic = (Button) findViewById(R.id.shuffle);
		playMusic.setOnClickListener(new OnClickListener());
		controlMusic = (Button) findViewById(R.id.control_music);
		controlMusic.setOnClickListener(new OnClickListener());
		readSetting = (Button) findViewById(R.id.setting);
		readSetting.setOnClickListener(new OnClickListener());
	}
	
	private void initView(){
		Display display = getWindowManager().getDefaultDisplay();
		screenWidth = display.getWidth();
		screenHeight = display.getHeight();
		mBookPageView.setViewWidth(screenWidth);
		mBookPageView.setViewHeight(screenHeight);
		mCurPageBitmap = Bitmap.createBitmap(screenWidth, screenHeight,
				Bitmap.Config.ARGB_8888); // 创建当前的图片
		mNextPageBitmap = Bitmap.createBitmap(screenWidth, screenHeight,
				Bitmap.Config.ARGB_8888); // 创建下一页的图片
		mCurPageCanvas = new Canvas(mCurPageBitmap);
		mNextPageCanvas = new Canvas(mNextPageBitmap);
		SharedPreferences sharedPreferences = getSharedPreferences("readset", MODE_PRIVATE);
		textColor = sharedPreferences.getInt("textColor", Color.BLACK);
		backColor = sharedPreferences.getInt("backColor", Color.rgb(199, 237, 204));
		fontSize = sharedPreferences.getInt("fontSize", 36);
		// 实例化工具类
		pagefactory = new BookPageFactory(this, screenWidth, screenHeight, fontSize, textColor, backColor);
		// 设置自定义View的进度条上一页，下一页的图片
		mBookPageView.setBitmaps(mCurPageBitmap, mCurPageBitmap);
		// 设置自定义View的触屏事件
		mBookPageView.setOnTouchListener(new OnTouchListener());
		// this.registerForContextMenu(mBookPageView);
	}

	private void initBookData() {
		try {
			bookDB = BookDB.getInstance(this);
			Long bookId = getIntent().getLongExtra("bookId", -1);
			if (bookId == -1) {
				ToastUtils.toast(this, "查询的id不存在");
				startActivity(new Intent(this, BookShelfActivity.class));
				return;
			}
			book = bookDB.get(bookId);
			fileLenth = pagefactory.openBook(book);
			Log.w("TestDraw", "初始化: initBookData ");
			pagefactory.onDraw(mCurPageCanvas); // 绘制进度百分比
		} catch (IOException e1) {
			e1.printStackTrace();
			ToastUtils.toast(this, "查询的电子书不存在");
		}
	}

	private class OnTouchListener implements View.OnTouchListener {
		public boolean onTouch(View v, MotionEvent e) {
			boolean ret1 = false;
			if (v == mBookPageView) {
				if (e.getAction() == MotionEvent.ACTION_DOWN) {// 屏幕按下
					// 设置动画效果
					mBookPageView.abortAnimation();
					// 修改点击的坐标，从而判断是上一页还是下一页
					mBookPageView.calculateCornerXY(e.getX(), e.getY());
					if (mBookPageView.canReadNextPage(e.getX(), e.getY())) {
						if (mBookPageView.DragToRight()) {
							pagefactory.prePage();// 上一页
							// 如果是首页了，就不用更改进度框的信息
							if (pagefactory.isFirstPage()) {
								ToastUtils.toast(ReadBookActivity.this, "首页");
								return false;
							}
							pagefactory.onDraw(mNextPageCanvas);
						} else {
							pagefactory.nextPage();// 下一页
							// 如果是末页了，就不用更改进度框的信息
							if (pagefactory.isLastPage()) {
								ToastUtils.toast(ReadBookActivity.this, "末页");
								return false;
							}
							pagefactory.onDraw(mNextPageCanvas);
						}
						// 把上一页和下一页的图片给自定义View
						// TODO 页面动画有问题，强制设置了动画的两个页面为下一页。
						mBookPageView.setBitmaps(mNextPageBitmap,
								mNextPageBitmap);
					} else {
						if (isShowSetting) {
							isShowSetting = false;
						} else {
							isShowSetting = true;
						}
						showSetting();
					}
				}
				ret1 = mBookPageView.doTouchEvent(e);
				return ret1;
			}
			return false;
		}
	}
	
	private class OnClickListener implements View.OnClickListener{

		@Override
		public void onClick(View v) {
			switch(v.getId()){
			case R.id.shuffle:
				if(!isPlayMusic){
					startService(new Intent(ReadBookActivity.this, PlayMusicService.class));
					playMusic.setText("停止");
					isPlayMusic = true;
					controlMusic.setVisibility(View.VISIBLE);
					controlMusic.setText("暂停");
					break;
				} else {
					stopService(new Intent(ReadBookActivity.this, PlayMusicService.class));
					playMusic.setText("音乐");
					isPlayMusic = false;
					controlMusic.setVisibility(View.GONE);
					break;
				}
			case R.id.control_music:
				if(!isPauseMusic){
					Intent pauseIntent = new Intent();  
					pauseIntent.setAction("com.android.music.musicservicecommand.pause");  
					pauseIntent.putExtra("command", "pause");  
					sendBroadcast(pauseIntent); 
					isPauseMusic = true;
					controlMusic.setText("播放");
					break;
				} else {
					Intent playIntent = new Intent();  
					playIntent.setAction("com.android.music.musicservicecommand.play");  
					playIntent.putExtra("command", "play");  
					sendBroadcast(playIntent);  
					isPauseMusic = false;
					controlMusic.setText("暂停");
					break;
				}
			case R.id.setting:
				settingdialog = new SettingDialog(ReadBookActivity.this, fontSize, textColor);
				initDialog(settingdialog);
				settingdialog.show();
				break;
			}
		}
	}
	
	private void showSetting() {
		if (isShowSetting) {
			bookSetting.setVisibility(View.VISIBLE);
			readProgress.setVisibility(View.VISIBLE);
			bookProgress.setText(book.getProgress());
			double progreee = Double.valueOf(book.getProgress().replaceAll("%", ""));
			progressSetting.setProgress((int) (progreee*100));
		} else {
			bookSetting.setVisibility(View.GONE);
			readProgress.setVisibility(View.GONE);
		}
	}

	private class OnSeekBarChangeListener implements
			SeekBar.OnSeekBarChangeListener {
		private double progressValueFinal;
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			double progressValue = progress * 0.01;
			DecimalFormat df = new DecimalFormat("#0.00");
			progressValueFinal = new Double(df.format(progressValue)); 
			bookProgress.setText(progressValueFinal+"%");
		}

		// 开始拖动
		public void onStartTrackingTouch(SeekBar seekBar) {
		}

		// 结束拖动
		public void onStopTrackingTouch(SeekBar seekBar) {
			int begin = (int) (progressValueFinal * fileLenth /100); 
			book.setProgress(progressValueFinal+"");
			book.setBegin(begin);
			book.setLastReadTime(new Date());
			bookDB.update(book);
			Log.d(TAG, "begin:" +begin); 
			Log.d(TAG, "fileLenth:" + fileLenth); // 重新设置进度位置，并清空原来的文字
			pagefactory.setM_mbBufEnd(begin); 
			pagefactory.onDraw(mCurPageCanvas); 
			pagefactory.onDraw(mNextPageCanvas); 
			mBookPageView.setBitmaps(mCurPageBitmap, mNextPageBitmap);
			mBookPageView.postInvalidate();
		}
	}

	private void initDialog(final SettingDialog settingdialog) {
		settingdialog.setOnStartNewReadViewListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				pattern = settingdialog.getReadPattern();			
				fontSize = settingdialog.getFontSize();
				SharedPreferences sharedPreferences = getSharedPreferences("readset", MODE_PRIVATE); //私有数据
				Editor editor = sharedPreferences.edit();
				if(pattern == PatternUtils.DAYTIMEPATTERN){
					textColor = Color.BLACK;
					backColor = Color.rgb(199, 237, 204); 
				} else {
					textColor = Color.WHITE;
					backColor = Color.BLACK; 
				}
				editor.putInt("textColor", textColor);
				editor.putInt("backColor", backColor);
				editor.putInt("fontSize", fontSize);
				editor.commit();//提交修改
				settingdialog.dismiss();
				initView();
				initBookData();
				isShowSetting = false;
				showSetting();
				mBookPageView.postInvalidate();
			}
		});
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	// 改变阅读页面的返回事件
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			startActivity(new Intent(this, BookShelfActivity.class));
			ReadBookActivity.this.finish();
			return false;
		}
		return false;
	}
}
