package cn.jpush.webim.socketio.bean;

import java.util.Map;

import com.google.gson.Gson;

public class ImageMsgBody{
	private String media_id; 
	private long media_crc32;
	private int width;
	private int height;
	private String format;
	private String img_link;
	private Map extras;
	
	public String getMedia_id() {
		return media_id;
	}

	public void setMedia_id(String media_id) {
		this.media_id = media_id;
	}

	public long getMedia_crc32() {
		return media_crc32;
	}

	public void setMedia_crc32(long media_crc32) {
		this.media_crc32 = media_crc32;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getImg_link() {
		return img_link;
	}

	public void setImg_link(String img_link) {
		this.img_link = img_link;
	}

	public Map getExtras() {
		return extras;
	}

	public void setExtras(Map extras) {
		this.extras = extras;
	}

	public String toString(){
		Gson gson = new Gson();
		return gson.toJson(this);
	}
}
