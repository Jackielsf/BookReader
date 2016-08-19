package com.lsf.bookreader_lsf.app.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class BookOpenHelper extends SQLiteOpenHelper {

	public static final String TABLE_NAME = "books";

	public static final String CREATE_BOOKS = "create table books ("
			+ "book_id integer primary key autoincrement, "
			+ "book_name text, " + "file_path text, " + "image_path text, "
			+ "last_read_time integer, " + "begin integer, " + "progress text)";

	public BookOpenHelper(Context context, String name, CursorFactory factory,
			int version) {
		super(context, name, factory, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.w("lsfreader", "create database!");
		db.execSQL(CREATE_BOOKS);
		db.execSQL("insert into " + TABLE_NAME + " (book_name, file_path, image_path) "
				+ "values ('万古至尊', '/storage/emulated/0/BookReader/books/万古至尊.txt', '/storage/emulated/0/BookReader/images/万古至尊.png')");
		db.execSQL("insert into " + TABLE_NAME + " (book_name, file_path, image_path) "
				+ "values ('不死武尊', '/storage/emulated/0/BookReader/books/不死武尊.txt', '/storage/emulated/0/BookReader/images/不死武尊.png')");
		db.execSQL("insert into " + TABLE_NAME + " (book_name, file_path, image_path) "
				+ "values ('十国千娇', '/storage/emulated/0/BookReader/books/十国千娇.txt', '/storage/emulated/0/BookReader/images/十国千娇.png')");
		db.execSQL("insert into " + TABLE_NAME + " (book_name, file_path, image_path) "
				+ "values ('战神年代', '/storage/emulated/0/BookReader/books/战神年代.txt', '/storage/emulated/0/BookReader/images/战神年代.png')");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
	}

}
