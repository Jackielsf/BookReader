package com.lsf.bookreader_lsf.app.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lsf.bookreader_lsf.app.R;
import com.lsf.bookreader_lsf.app.db.Book;
import com.lsf.bookreader_lsf.app.db.BookDB;
import com.lsf.bookreader_lsf.app.utils.ImageUtil;
import com.lsf.bookreader_lsf.app.utils.ToastUtils;

public class BookShelfActivity extends Activity {

	private static String TAG = "BookShelfActivity";
	private BookDB bookDB;
	private ArrayList<Book> books;

	private GridView bookShelf;

	private ProgressDialog dialog;
	private int selectedIndex = -1; // 显示图书首页的索引

	public static int width, height;
	public static Bitmap itemBackground;

	private int mDownPosX;
	private int mUpPosX;
	private LinearLayout bookUpdateLayout;
	private Button updateFromLocal;
	private Button updateFromService;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.book_shelf);
		init();
	}

	private void init() {
		bookDB = BookDB.getInstance(this);
		initBookShelf();
		loadBooks();
	}

	private void initBookShelf() {
		bookShelf = (GridView) findViewById(R.id.bookShelf);
		bookShelf.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				selectedIndex = position;
				readBook();
			}
		});
		bookShelf.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				selectedIndex = position;
				return false;
			}
		});
		bookShelf.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (MotionEvent.ACTION_DOWN == event.getAction()) {
					mDownPosX = (int) event.getX();
				}
				if (MotionEvent.ACTION_UP == event.getAction()) {
					mUpPosX = (int) event.getX();
					if (mUpPosX - mDownPosX > 300 || mDownPosX - mUpPosX > 300) {
						showMenu();
						return true;
					}
				}
				return false;
			}
		});
		// 注册上下文菜单
		registerForContextMenu(bookShelf);
	}

	private void showMenu() {
		bookUpdateLayout = (LinearLayout) findViewById(R.id.book_update);
		bookUpdateLayout.setVisibility(View.VISIBLE);
		updateFromLocal = (Button) findViewById(R.id.update_local);
		updateFromLocal.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				bookUpdateFromLocal();
				bookUpdateLayout.setVisibility(View.GONE);
			}
		});
		updateFromService = (Button) findViewById(R.id.update_service);
		updateFromService.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				bookUpdateFromService();
				bookUpdateLayout.setVisibility(View.GONE);
			}
		});
	}

	private void bookUpdateFromLocal() {
		File path = Environment.getExternalStorageDirectory();// 获得SD卡路径
		File bookPath = new File(path.toString() + "/BookReader/books/");
		File[] files = bookPath.listFiles();
		for (File file : files) {
			String fileName = file.getName();
			if (fileName.endsWith(".txt")) {
				String name = fileName.substring(0, fileName.lastIndexOf(".")).toString();
				if(bookDB.queryName(new String[] {name} ) == null){
					Book book = new Book();
					book.setName(name);
					book.setFilePath(bookPath + "/" + name + ".txt");
					File imagePath = new File(path.toString() + "/BookReader/images/" + name + ".png");
					if(imagePath.exists()){
						book.setImagePath(imagePath.toString());
					}
					bookDB.save(book);
				}
			}
		}
		loadBooks();
	}

	private void bookUpdateFromService() {

	}

	private void showMore() {

	}

	private void readBook() {
		dialog = ProgressDialog.show(BookShelfActivity.this, "温馨提示", "正在加载文件",
				true);
		Book book = books.get(selectedIndex);
		File file = new File(book.getFilePath());
		if (!file.exists()) {
			ToastUtils.toast(this, "文件不存在");
			dialog.dismiss();
			return;
		}
		Long id = book.getId();
		Intent intent = new Intent(this, ReadBookActivity.class);
		intent.putExtra("bookId", id);
		Log.w("BookTest", "bookId:" + id);
		dialog.dismiss();
		startActivity(intent);
	}

	// // 创建选项菜单：点击手机上的MENU
	// @Override
	// public boolean onCreateOptionsMenu(Menu menu) {
	// MenuInflater inflater = new MenuInflater(this);
	// inflater.inflate(R.menu.my_book, menu);// 解析定义在menu目录下的菜单布局文件
	// return super.onCreateOptionsMenu(menu);
	// }

	// 处理选项菜单的点击事件
	// @Override
	// public boolean onOptionsItemSelected(MenuItem item) {
	// switch (item.getItemId()) {
	// case R.id.menu_importbook:// menu_importbook 图书导入
	// Intent intent = new Intent(this, ImportBook.class);
	// startActivityForResult(intent, REQUEST_CODE_IMPORT_BOOK);
	// break;
	// case R.id.menu_help:
	// // 跳转到viewpager页面
	// Intent intent1 = new Intent(MyBook.this, ViewPagerActivity.class);
	// startActivity(intent1);
	// break;
	// case R.id.menu_setting:
	// // 跳转到设置页面
	// Intent intent2 = new Intent(MyBook.this, SettingActivity.class);
	// startActivity(intent2);
	// break;
	// }
	// return super.onOptionsItemSelected(item);
	// }

	// @Override
	// protected void onActivityResult(int requestCode, int resultCode, Intent
	// data) {
	// // 直接点击文件浏览器列表返回
	// if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_IMPORT_BOOK) {
	// loadData();
	// ToastUtils.toast(this, "添加图书成功");
	// }
	// }

	private void loadBooks() {
		books = bookDB.getAllBooks();
		Display defaultDisplay = getWindowManager().getDefaultDisplay();
		width = defaultDisplay.getWidth();
		height = defaultDisplay.getHeight();
		itemBackground = ImageUtil.getBitmap(this, R.drawable.cover_txt,
				width / 3, height / 3);
		ShlefAdapter adapter = new ShlefAdapter();
		bookShelf.setAdapter(adapter);
	}

	class ShlefAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return books.size();
		}

		@Override
		public Book getItem(int position) {
			return books.get(position);
		}

		@Override
		public long getItemId(int position) {
			return getItem(position).getId();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Book book = getItem(position);
			convertView = LayoutInflater.from(getApplicationContext()).inflate(
					R.layout.book_item, null);
			TextView textViewName = (TextView) convertView
					.findViewById(R.id.bookName);
			TextView textViewImage = (TextView) convertView
					.findViewById(R.id.bookImage);
			if (book.getImagePath() == null) {
				textViewImage.setBackgroundColor(Color.rgb(199, 237, 204));
				textViewImage.setText(book.getName());
			} else {
				Bitmap bm = BitmapFactory.decodeFile(book.getImagePath());
				Drawable drawable = new BitmapDrawable(bm);
				textViewImage.setBackground(drawable);
			}
			textViewName.setText(book.getName());
			BitmapDrawable bitmapDrawable = new BitmapDrawable(itemBackground);
			textViewName.setBackgroundDrawable(bitmapDrawable);
			return convertView;
		}
	}

	// 创建上下文菜单
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		// menu.setHeaderTitle("上下文菜单");
		MenuInflater inflater = new MenuInflater(this);
		inflater.inflate(R.menu.menu_book, menu);
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	// 处理上下文菜单的点击事件
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.read:
			readBook();
			break;
		case R.id.delete:
			Long id1 = books.get(selectedIndex).getId();
			bookDB.delete(id1);
			ToastUtils.toast(this, R.string.msg_delete_success);
			loadBooks();
			break;
		default:
			break;
		}
		return super.onContextItemSelected(item);
	}

	// 改变主页面的返回事件
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			dialog();
			return false;
		}
		return false;
	}

	private void dialog() {
		AlertDialog.Builder builder = new Builder(this);
		builder.setMessage("确定要退出吗?");
		builder.setTitle("温馨提示");
		builder.setPositiveButton("确认",
				new android.content.DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						BookShelfActivity.this.finish();
					}
				});
		builder.setNegativeButton("取消",
				new android.content.DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		builder.create().show();
	}
}
