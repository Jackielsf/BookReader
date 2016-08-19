package com.lsf.bookreader_lsf.app.db;

import java.text.DecimalFormat;
import java.util.Date;

public class Book{

	private static final DecimalFormat decimalFormat = new DecimalFormat("0.00");

	private long id;
	private String name;// 书名
	private String filePath;// 文件路径
	private String imagePath;// 封面图片路径
	private Date lastReadTime;// 最后阅读时间
	private int begin = 0;// 从那里开始看书
	private String progress;// 进度比例

	public Book() {
	}

	public Book(long id, String name, String filePath, String imagePath,
			Date lastReadTime, int begin, String progress) {
		super();
		this.id = id;
		this.name = name;
		this.filePath = filePath;
		this.imagePath = imagePath;
		this.lastReadTime = lastReadTime;
		this.begin = begin;
		this.progress = progress;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getImagePath() {
		return imagePath;
	}

	public void setImagePath(String imagePath) {
		this.imagePath = imagePath;
	}

	public Date getLastReadTime() {
		return lastReadTime;
	}

	public void setLastReadTime(Date lastReadTime) {
		this.lastReadTime = lastReadTime;
	}

	public int getBegin() {
		return begin;
	}

	public void setBegin(int begin) {
		this.begin = begin;
	}

	public String getProgress() {
		return progress;
	}

	public void setProgress(String progress) {
		this.progress = progress;
	}

	@Override
	public String toString() {
		return "Book [id=" + id + ", name=" + name + ", filePath=" + filePath
				+ ", imagePath=" + imagePath + ", lastReadTime=" + lastReadTime
				+ ", begin=" + begin + ", progress=" + progress + "]";
	}
}
