package com.lsf.bookreader_lsf.app.db;

import java.util.ArrayList;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class BookDB {
	
	private static final String TAG = "BookDB";

	public static final String DB_NAME = "book_reader_lsf";
	public static final String TABLE_NAME = "books";

	public static final String BOOK_ID = "book_id";
	public static final String BOOK_NAME = "book_name";
	public static final String FILE_PATH = "file_path";
	public static final String IMAGE_PATH = "image_path";
	public static final String LAST_READ_TIME = "last_read_time";
	public static final String BEGIN = "begin";
	public static final String PROGRESS = "progress";

	public static final int version = 1;

	private static BookDB bookDB;

	private SQLiteDatabase db;

	private BookDB(Context context) {
		BookOpenHelper dbHelper = new BookOpenHelper(context, DB_NAME, null,
				version);
		db = dbHelper.getWritableDatabase();
	}

	public synchronized static BookDB getInstance(Context context) {
		if (bookDB == null) {
			bookDB = new BookDB(context);
		}
		return bookDB;
	}

	// /**
	// * 保存book
	// *
	// * @param book
	// * ：图书领域类对象
	// * @return：返回保存的主键
	// */
	// public Long save(Book book) {
	// SQLiteDatabase sqLiteDatabase = dataBaseHelper.getWritableDatabase();
	// // 重构方法的演示
	// ContentValues values = bookToContentValues(book);
	// // 只有使用对象的方法操作SQLite才能返回保存的主键,具体可以看Android源码，O(∩_∩)O哈哈~
	// Long count = sqLiteDatabase.insert(TABLE_NAME, null, values);
	// sqLiteDatabase.close();
	// return count;
	// }

	public long update(Book book) {
		// 重构方法的演示
		ContentValues values = bookToContentValues(book);
		long count = db.update(TABLE_NAME, values, BOOK_ID + "=?",
				new String[] { book.getId().toString() });
		return count;
	}

	//
	// /**
	// * 删除book
	// *
	// * @param 图书领域类对象主键
	// * @return：返回保存的主键
	// */
	// public long delete(Long id) {
	// SQLiteDatabase sqLiteDatabase = dataBaseHelper.getWritableDatabase();
	// long count = sqLiteDatabase.delete(TABLE_NAME, BookColumn._ID + "=?", new
	// String[] { id.toString() });
	// sqLiteDatabase.close();
	// return count;
	// }

	public Book get(Long id) {
		Book book = null;
		Cursor cursor = db.query(TABLE_NAME, new String[] { BOOK_ID, BOOK_NAME,
				FILE_PATH, IMAGE_PATH, LAST_READ_TIME, BEGIN, PROGRESS },
				BOOK_ID + "=?", new String[] { id.toString() }, null, null, null);
		if (cursor.moveToFirst()) {
			book = cursorToBook(cursor);
			Log.w("BookTest", "book : " + book.toString());
			cursor.close();// 关闭游标
		}
		return book;
	}

	//
	// // 实际应用应该考虑使用分页。。。。
	// /**
	// * 分页
	// *
	// * @param firstResult
	// * 从什么位置开始取数据
	// * @param maxResult
	// * 总共取多少条记录
	// * @return获得分页后所有实体
	// */
	// // public ArrayList<Emp> findPage(int firstResult, int maxResult) {
	// // ArrayList<Emp> emps = new ArrayList<Emp>();
	// // SQLiteDatabase db = databaseHelper.getReadableDatabase();
	// // Cursor cursor = db.query("emp", new String[] { "emp_id", "name" },
	// // "limit ?,?", new String[] { String.valueOf(firstResult),
	// // String.valueOf(maxResult) }, null, null, null);
	// // // Cursor cursor = db.rawQuery("select * from emp limit ?,?",
	// // // new String[] { String.valueOf(firstResult),
	// String.valueOf(maxResult)
	// // });
	// // while (cursor.moveToNext()) {
	// // int emp_id = cursor.getInt(cursor.getColumnIndex("emp_id"));
	// // String name = cursor.getString(cursor.getColumnIndex("name"));
	// // emps.add(new Emp(emp_id, name));
	// // }
	// // return emps;
	// // }

	public ArrayList<Book> getAllBooks() {
		ArrayList<Book> books = new ArrayList<Book>();
		Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null,
				BOOK_ID + " desc");
		while (cursor.moveToNext()) {
			Book book = cursorToBook(cursor);
			books.add(book);
		}
		cursor.close();
		return books;
	}

	private Book cursorToBook(Cursor cursor) {
		long id = cursor.getLong(0);
		String name = cursor.getString(cursor.getColumnIndex(BOOK_NAME));
		String filePath = cursor.getString(cursor.getColumnIndex(FILE_PATH));
		String imagePath = cursor.getString(cursor.getColumnIndex(IMAGE_PATH));
		Date lastReadTime = new Date(cursor.getLong(cursor
				.getColumnIndex(LAST_READ_TIME)));
		int begin = cursor.getInt(cursor.getColumnIndex(BEGIN));
		String progress = cursor.getString(cursor.getColumnIndex(PROGRESS));
		Book book = new Book(id, name, filePath, imagePath, lastReadTime,
				begin, progress);
		return book;
	}

	private ContentValues bookToContentValues(Book book) {
		ContentValues values = new ContentValues();
		values.put(BOOK_NAME, book.getName());
		values.put(FILE_PATH, book.getFilePath());
		values.put(IMAGE_PATH, book.getImagePath());
		if (book.getLastReadTime() != null) {// 不为空才获得getTime，不是会出现空指针异常
			values.put(LAST_READ_TIME, book.getLastReadTime().getTime());
		}
		values.put(BEGIN, book.getBegin());
		Log.w(TAG + "seekbar", book.getProgress());
		values.put(PROGRESS, book.getProgress());
		return values;
	}
}
