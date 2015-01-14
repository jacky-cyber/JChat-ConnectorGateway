package com.jpush.protocal.push;

public class PushRegRequestBean {
	private String strKey;
	private String strApkVersion;
	private String strClientInfo;
	private String strDeviceToken;
	private int build_type;
	private int aps_type;
	private int platform;
	private String strKeyExt;
	public PushRegRequestBean(){}
	public PushRegRequestBean(String strKey, String strApkVersion,
							String strClientInfo, String strDeviceToken,
							int build_type, int aps_type, int platform, String strKeyExt){
		this.strKey = strKey;
		this.strApkVersion = strApkVersion;
		this.strClientInfo = strClientInfo;
		this.strDeviceToken = strDeviceToken;
		this.build_type = build_type;
		this.aps_type = aps_type;
		this.platform = platform;
		this.strKeyExt = strKeyExt;
	}
	public String getStrKey() {
		return strKey;
	}
	public void setStrKey(String strKey) {
		this.strKey = strKey;
	}
	public String getStrApkVersion() {
		return strApkVersion;
	}
	public void setStrApkVersion(String strApkVersion) {
		this.strApkVersion = strApkVersion;
	}
	public String getStrClientInfo() {
		return strClientInfo;
	}
	public void setStrClientInfo(String strClientInfo) {
		this.strClientInfo = strClientInfo;
	}
	public String getStrDeviceToken() {
		return strDeviceToken;
	}
	public void setStrDeviceToken(String strDeviceToken) {
		this.strDeviceToken = strDeviceToken;
	}
	public int getBuild_type() {
		return build_type;
	}
	public void setBuild_type(int build_type) {
		this.build_type = build_type;
	}
	public int getAps_type() {
		return aps_type;
	}
	public void setAps_type(int aps_type) {
		this.aps_type = aps_type;
	}
	public int getPlatform() {
		return platform;
	}
	public void setPlatform(int platform) {
		this.platform = platform;
	}
	public String getStrKeyExt() {
		return strKeyExt;
	}
	public void setStrKeyExt(String strKeyExt) {
		this.strKeyExt = strKeyExt;
	}
	
}
