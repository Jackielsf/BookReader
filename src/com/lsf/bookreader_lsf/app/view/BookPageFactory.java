package com.lsf.bookreader_lsf.app.view;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Vector;

import com.lsf.bookreader_lsf.app.activity.ReadBookActivity;
import com.lsf.bookreader_lsf.app.db.Book;
import com.lsf.bookreader_lsf.app.db.BookDB;
import com.lsf.bookreader_lsf.app.utils.DateUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.util.Log;

public class BookPageFactory {
	private static final String TAG = "BookPageFactory";
	
	private File bookFile = null;
	private MappedByteBuffer m_mbBuf = null;
	
	// 文件长度
	private int bookBufLen = 0;
	// 文字的启动位置
	private int bookBufBegin = 0;
	private int bookBufEnd = 0;
	private String strCharsetName = "UTF-8";
	private Bitmap bookBackground = null;
	private int screenWidth;// 屏幕宽度
	private int screenHeight;// 屏幕高度
	private int marginWidth = 15; // 左右与边缘的距离
	private int marginHeight = 20; // 上下与边缘的距离
	private float visibleHeight; // 绘制内容的高
	private float visibleWidth; // 绘制内容的宽
	private BookDB bookDB;
	private Book book;
	
	// 因为MappedByteBuffer是同步处理的，故使用Vector
	private Vector<String> showLines = new Vector<String>();

	private int fontSize;
	private int lineCount; // 每页可以显示的行数
	private int textColor;
	private int backColor; // 背景颜色


	private boolean isFirstPage, isLastPage;

	private Paint mPaint;

	public BookPageFactory(Context context, int w, int h, int fontSize, int textColor, int backColor) {
		bookDB = BookDB.getInstance(context);
		screenWidth = w;
		screenHeight = h;
		this.fontSize = fontSize;
		this.textColor = textColor;
		this.backColor = backColor;
		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaint.setTextAlign(Align.LEFT);
		mPaint.setTextSize(fontSize);
		mPaint.setColor(textColor);
		visibleWidth = screenWidth - marginWidth * 2;
		visibleHeight = screenHeight - marginHeight * 2 - fontSize;
		lineCount = (int) (visibleHeight / fontSize); 
	}

	public long openBook(Book book) throws IOException {
		this.book = book;
		bookFile = new File(book.getFilePath());
		long bookFileLen = bookFile.length();
		bookBufLen = (int) bookFileLen;
		m_mbBuf = new RandomAccessFile(bookFile, "r").getChannel().map(
				FileChannel.MapMode.READ_ONLY, 0, bookFileLen);
		this.bookBufEnd = book.getBegin();
		Log.w("TestBook", "bookBufEnd: " + bookBufEnd);
		this.bookBufBegin = this.bookBufEnd;
		return bookFileLen;
	}

	protected byte[] readParagraphBack(int nFromPos) {
		int nEnd = nFromPos;
		int i;
		byte b0, b1;
		if (strCharsetName.equals("UTF-16LE")) {
			i = nEnd - 2;
			while (i > 0) {
				b0 = m_mbBuf.get(i);
				b1 = m_mbBuf.get(i + 1);
				if (b0 == 0x0a && b1 == 0x00 && i != nEnd - 2) {
					i += 2;
					break;
				}
				i--;
			}

		} else if (strCharsetName.equals("UTF-16BE")) {
			i = nEnd - 2;
			while (i > 0) {
				b0 = m_mbBuf.get(i);
				b1 = m_mbBuf.get(i + 1);
				if (b0 == 0x00 && b1 == 0x0a && i != nEnd - 2) {
					i += 2;
					break;
				}
				i--;
			}
		} else {
			i = nEnd - 1;
			while (i > 0) {
				b0 = m_mbBuf.get(i);
				if (b0 == 0x0a && i != nEnd - 1) {
					i++;
					break;
				}
				i--;
			}
		}
		if (i < 0)
			i = 0;
		int nParaSize = nEnd - i;
		int j;
		byte[] buf = new byte[nParaSize];
		for (j = 0; j < nParaSize; j++) {
			buf[j] = m_mbBuf.get(i + j);
		}
		return buf;
	}

	// 读取上一段落
	protected byte[] readParagraphForward(int nFromPos) {
		int nStart = nFromPos;
		int i = nStart;
		byte b0, b1;
		// 根据编码格式判断换行
		if (strCharsetName.equals("UTF-16LE")) {
			while (i < bookBufLen - 1) {
				b0 = m_mbBuf.get(i++);
				b1 = m_mbBuf.get(i++);
				if (b0 == 0x0a && b1 == 0x00) {
					break;
				}
			}
		} else if (strCharsetName.equals("UTF-16BE")) {
			while (i < bookBufLen - 1) {
				b0 = m_mbBuf.get(i++);
				b1 = m_mbBuf.get(i++);
				if (b0 == 0x00 && b1 == 0x0a) {
					break;
				}
			}
		} else {
			while (i < bookBufLen) {
				b0 = m_mbBuf.get(i++);
				if (b0 == 0x0a) {
					break;
				}
			}
		}
		int nParaSize = i - nStart;
		byte[] buf = new byte[nParaSize];
		for (i = 0; i < nParaSize; i++) {
			buf[i] = m_mbBuf.get(nFromPos + i);
		}
		return buf;
	}

	protected Vector<String> pageDown() {
		String strParagraph = "";
		Vector<String> lines = new Vector<String>();
		while (lines.size() < lineCount && bookBufEnd < bookBufLen) {
			byte[] paraBuf = readParagraphForward(bookBufEnd); // 读取一个段落
			bookBufEnd += paraBuf.length;
			try {
				strParagraph = new String(paraBuf, strCharsetName);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			String strReturn = "";
			if (strParagraph.indexOf("\r\n") != -1) {
				strReturn = "\r\n";
				strParagraph = strParagraph.replaceAll("\r\n", "");
			} else if (strParagraph.indexOf("\n") != -1) {
				strReturn = "\n";
				strParagraph = strParagraph.replaceAll("\n", "");
			}

			if (strParagraph.length() == 0) {
				lines.add(strParagraph);
			}
			while (strParagraph.length() > 0) {
				int nSize = mPaint.breakText(strParagraph, true, visibleWidth,
						null);
				lines.add(strParagraph.substring(0, nSize));
				strParagraph = strParagraph.substring(nSize);
				if (lines.size() >= lineCount) {
					break;
				}
			}
			if (strParagraph.length() != 0) {
				try {
					bookBufEnd -= (strParagraph + strReturn)
							.getBytes(strCharsetName).length;
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		}
		return lines;
	}

	protected void pageUp() {
		if (bookBufBegin < 0)
			bookBufBegin = 0;
		Vector<String> lines = new Vector<String>();
		String strParagraph = "";
		while (lines.size() < lineCount && bookBufBegin > 0) {
			Vector<String> paraLines = new Vector<String>();
			byte[] paraBuf = readParagraphBack(bookBufBegin);
			bookBufBegin -= paraBuf.length;
			try {
				strParagraph = new String(paraBuf, strCharsetName);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			strParagraph = strParagraph.replaceAll("\r\n", "");
			strParagraph = strParagraph.replaceAll("\n", "");

			if (strParagraph.length() == 0) {
				paraLines.add(strParagraph);
			}
			while (strParagraph.length() > 0) {
				int nSize = mPaint.breakText(strParagraph, true, visibleWidth,
						null);
				paraLines.add(strParagraph.substring(0, nSize));
				strParagraph = strParagraph.substring(nSize);
			}
			lines.addAll(0, paraLines);
		}
		while (lines.size() > lineCount) {
			try {
				bookBufBegin += lines.get(0).getBytes(strCharsetName).length;
				lines.remove(0);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		bookBufEnd = bookBufBegin;
		return;
	}

	public void prePage() {
		if (bookBufBegin <= 0) {
			bookBufBegin = 0;
			isFirstPage = true;
			return;
		} else {
			isFirstPage = false;
		}
		showLines.clear();
		pageUp();
		showLines = pageDown();
	}

	public void nextPage() {
		if (bookBufEnd >= bookBufLen) {
			isLastPage = true;
			return;
		} else
			isLastPage = false;
		showLines.clear();
		bookBufBegin = bookBufEnd;
		showLines = pageDown();
	}

	// 绘制当前的进度百分比
	public void onDraw(Canvas c) {
		Log.w("TestDraw", "use pageFactory ondraw");
		if (showLines.size() == 0) {// 还没有取好数据
			showLines = pageDown();// 存储显示是文字
			Log.d(TAG, "m_lines.size() == 0");
		}
		if (showLines.size() > 0) {
			if (bookBackground == null)// 判断背景颜色是否为空
				c.drawColor(backColor);
			else
				c.drawBitmap(bookBackground, 0, 0, null);
			int y = marginHeight;
			// 循环输出文字，进行绘制
			for (String strLine : showLines) {
				y += fontSize;// 一次绘制一行
				c.drawText(strLine, marginWidth, y, mPaint);
				Log.d("MyBook", strLine);
			}
		}
		// 计算分页的内容，也是绘制在屏幕
		float fPercent = (float) (book.getBegin() * 1.0 / bookBufLen);

		DecimalFormat df = new DecimalFormat("#0.00");
		String strPercent = df.format(fPercent * 100) + "%";
		// int nPercentWidth = (int) mPaint.measureText("999.9%") + 1;
		// c.drawText(strPercent, mWidth - nPercentWidth, mHeight - 5, mPaint);
		c.drawText(DateUtils.formatTime() + "  " + strPercent + "  test", 0,
				screenHeight - 5, mPaint);
		// 修改SQList book的数据
		book.setBegin(bookBufBegin);
		book.setProgress(strPercent);
		book.setLastReadTime(new Date());
		bookDB.update(book);
	}

	public void setBgBitmap(Bitmap BG) {
		bookBackground = BG;
	}

	public boolean isFirstPage() {
		return isFirstPage;
	}

	public boolean isLastPage() {
		return isLastPage;
	}

	public void setM_mbBufEnd(int m_mbBufEnd) {
		this.bookBufEnd = m_mbBufEnd;
		this.bookBufBegin = m_mbBufEnd;
		this.showLines.clear();
	}

	public int getM_backColor() {
		return backColor;
	}

	public void setM_backColor(int m_backColor) {
		this.backColor = m_backColor;
	}

}
