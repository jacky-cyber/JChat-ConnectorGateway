package cn.jpush.protocal.utils;

import io.netty.buffer.ByteBuf;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import org.slf4j.LoggerFactory;

import jpushim.s2b.JpushimSdk2B.AddGroupMember;
import jpushim.s2b.JpushimSdk2B.ChatMsgSync;
import jpushim.s2b.JpushimSdk2B.CreateGroup;
import jpushim.s2b.JpushimSdk2B.DelGroupMember;
import jpushim.s2b.JpushimSdk2B.EventNotification;
import jpushim.s2b.JpushimSdk2B.ExitGroup;
import jpushim.s2b.JpushimSdk2B.GroupMsg;
import jpushim.s2b.JpushimSdk2B.Login;
import jpushim.s2b.JpushimSdk2B.Logout;
import jpushim.s2b.JpushimSdk2B.Packet;
import jpushim.s2b.JpushimSdk2B.Response;
import jpushim.s2b.JpushimSdk2B.SingleMsg;
import jpushim.s2b.JpushimSdk2B.UpdateGroupInfo;
import ch.qos.logback.classic.Logger;
import cn.jpush.protocal.common.JPushTcpClientHandler;
import cn.jpush.protocal.push.PushLoginRequestBean;
import cn.jpush.protocal.push.PushLoginResponseBean;
import cn.jpush.protocal.push.PushLogoutResponseBean;
import cn.jpush.protocal.push.PushMessageRequestBean;
import cn.jpush.protocal.push.PushRegRequestBean;
import cn.jpush.protocal.push.PushRegResponseBean;

public class ProtocolUtil {
	private static Logger log = (Logger) LoggerFactory.getLogger(ProtocolUtil.class);
	private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private static final String CM_WAP_PROXY_HOST = "10.0.0.172";
	public static final String CM_WAP_PROXY_HEADER = "X-Online-Host";
	
	/**
	 * 
	 * @param oUrl Original access complete url
	 * @return two params in String array:  0 - new url,  1: host name
	 * @throws Exception
	 */
	public static String[] useWapConnection(String oUrl) throws Exception {
		URL url = new URL(oUrl);
		int port = url.getPort();
		String sPort = (port > 0 ? ":" + port  : "");
		
		String newUrl = url.getProtocol() + "://" + CM_WAP_PROXY_HOST + sPort + url.getPath();
		
		return new String[] { newUrl, url.getHost() };
	}
	

	public static String md5Encrypt(String value) {
		byte[] obj = value.getBytes();
		MessageDigest md5;
		try {
//			md5 = MessageDigest.getInstance("sha-1");
			md5 = MessageDigest.getInstance("md5");
			md5.update(obj);
			return byteToHexString(md5.digest()).toUpperCase();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return value;

	}

	/**
	 * convert byte to hex string
	 * 
	 * @param b
	 *            the byte need to be converted
	 * @return
	 */
	public static String byteToHexString(byte[] b) {
		StringBuffer sb = new StringBuffer();
		String temp = "";
		for (int i = 0; i < b.length; i++) {
			temp = Integer.toHexString(b[i] & 0Xff);
			if (temp.length() == 1)
				temp = "0" + temp;
			sb.append(temp);
		}
		return sb.toString();
	}

	/**
	 * append the string using the special
	 * 
	 * @param str
	 *            the string need to be appended
	 * @param isLeft
	 *            if true append the characher to the left, else append to right
	 * @param appendChar
	 *            appened character
	 * @param length
	 *            the total length of the return string
	 * @return
	 */
	public static String appendStr(String str, boolean isLeft, char appendChar, int length) {
		if (str != null) {
			str = "";
		}
		for (int i = 0; i < (length - str.length()); i++) {
			if (isLeft) {
				str = appendChar + str;
			} else {
				str = str + appendChar;
			}
		}
		return str;
	}

	/**
	 * convert the int value to byte array
	 * 
	 * @param value the int value need to be convert;
	 * @return four byte array
	 */
	public static byte[] intToByteArray(int value) {
		byte[] b = new byte[4];
		for (int i = 0; i < 4; i++) {
			int offset = (b.length - 1 - i) * 8;
			b[i] = (byte) ((value >>> offset) & 0xFF);
		}
		return b;
	}

	/**
	 * convert the int value to byte array
	 * 
	 * @param value the int value need to be convert;
	 * @return four byte array
	 */
	public static byte[] longToByteArray(long value) {
		byte[] b = new byte[8];
		for (int i = 0; i < 8; i++) {
			int offset = (b.length - 1 - i) * 8;
			b[i] = (byte) ((value >>> offset) & 0xFF);
		}
		return b;
	}

	/**
	 * copy the byte array,if the source array length is less than the byteSize, the applen the 0 byte to the left
	 * @param b the by need to be copy
	 * @param byteSize the return byte size
	 * @return the new byte with the byte size
	 */
	public static byte[] copyArray(byte[] b, int byteSize) {
		byte[] value = new byte[byteSize];
		int index = 0;
		int srcIndex = b.length - byteSize;
		if (byteSize > b.length) {
			index = byteSize - b.length;
			byteSize = b.length;
			srcIndex = 0;
		}

		System.arraycopy(b, srcIndex, value, index, byteSize);
		return value;

	}

	/**
	 * convert the byte array to the int
	 * @param b the source byte array
	 * @return the int value
	 */
	public static final int byteArrayToInt(byte[] b) {
		return (int) byteArrayToLong(b);
	}

	/**
	 * convert the byte array to the long
	 * @param b the source byte array
	 * @return the long value
	 */
	public static final long byteArrayToLong(byte[] b) {
		long value = 0;
		for (int i = 0; i < b.length - 1; i++) {
			value += (((long) b[i] & 0xFF) << (b.length - 1 - i) * 8);
		}
		value += (long) b[b.length - 1] & 0xFF;
		return value;
	}

	/**
	 * convert int data to byte array
	 * @param value the int value need to be connverted
	 * @param outSize, the return array size
	 * @return byte array
	 */
	public static byte[] intToByteArray(int value, int outSize) {
		byte[] data = intToByteArray(value);
		return copyArray(data, outSize);
	}
	
	public static byte[] stringToByteArray(String value, int outSize) {
		byte[] data = value.getBytes();
		return copyArray(data, outSize);
	}

	/**
	 * convert long data to byte array
	 * @param value the long value need to be connverted
	 * @param outSize, the return array size
	 * @return byte array
	 */
	public static byte[] longToByteArray(long value, int outSize) {
		byte[] data = longToByteArray(value);
		return copyArray(data, outSize);
	}

	public static void main(String[] args) {
		System.out.println(md5Encrypt("1.6"));
	}

	/**
	 * get seconds from the standard date
	 * @param currentDate
	 * @return
	 */
	public static int getStandardSecond(Date currentDate) {
		try {
			Date date = df.parse(df.format(currentDate));
			Date standandDate = df.parse("1970-01-01 00:00:00");
			int second = (int) ((date.getTime() - standandDate.getTime()) / 1000);
			return second;

		} catch (Exception e) {
		}
		return 0;
	}

	/**
	 * get the current start data
	 * @param currentDate
	 * @return
	 */
	public static Date getStandardDate(int second) {
		try {
			Date standandDate = df.parse("1970-01-01 00:00:00");
			long secondTmp = ProtocolUtil.byteArrayToLong(ProtocolUtil.intToByteArray(second));
			long time = standandDate.getTime() + secondTmp * 1000;
			return new Date(time);

		} catch (Exception e) {
		}
		return new Date();
	}

	/**
	 * get randomint 
	 * @return
	 */
	public static int getRandomInt() {
		Random r = new Random();
		int feed = r.nextInt(Integer.MAX_VALUE);
		return feed;
	}

	/**
	 * check whether the object is null or empyt
	 * @param obj
	 * @return
	 */
	public static boolean isNullAndEmpty(Object obj) {
		if (obj == null) {
			return true;
		} else if (obj.toString().trim().equals("")) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * print stack tracet information
	 * @param e exception object
	 * @return
	 */
	public static String stackTraceToString(Exception e) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream p = new PrintStream(baos);
		e.printStackTrace(p);
		p.flush();
		return baos.toString();
	}

	public static byte[] getDefaultByte(int byteSize) {
		byte[] data = new byte[byteSize];
		for (int i = 0; i < data.length; i++) {
			data[0] = 0;
		}
		return data;
	}

	/**
	 * if the object is null, set the value to empty
	 * @param obj
	 * @return
	 */
	public static Object setNullValueToEmpty(Object obj) {
		if (isNullAndEmpty(obj)) {
			return "";
		} else {
			return obj;
		}
	}

	public static boolean isDigital(String str) {
		try {
			Integer.valueOf(str);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public static synchronized String getStringData(byte[] data, int startIndex, int length) {
		byte[] tmp = getDefaultByte(length);
		System.arraycopy(data, startIndex, tmp, 0, tmp.length);
		try {
			return new String(tmp, Command.ENCODING_UTF_8);
		} catch (UnsupportedEncodingException e) {
		}
		return null;
	}

	public static synchronized long getLongData(byte[] data, int startIndex, int length) {
		byte[] tmp = getDefaultByte(length);
		System.arraycopy(data, startIndex, tmp, 0, tmp.length);
		return byteArrayToLong(tmp);
	}

	public static synchronized int getIntData(byte[] data, int startIndex, int length) {
		byte[] tmp = getDefaultByte(length);
		System.arraycopy(data, startIndex, tmp, 0, tmp.length);
		return byteArrayToInt(tmp);
	}
	
	//  push protocol 解析
	
	public static PushRegRequestBean getPushRegRequestBean(ByteBuf in) throws UnsupportedEncodingException{
		int strKeyLen = ProtocolUtil.byteArrayToInt(in.readBytes(2).array());
		String strKey = new String(in.readBytes(strKeyLen).array(),"utf-8");
		int strApkVersionLen = ProtocolUtil.byteArrayToInt(in.readBytes(2).array());
		String strApkVersion = new String(in.readBytes(strApkVersionLen).array(),"utf-8");
		int strClientInfoLen = ProtocolUtil.byteArrayToInt(in.readBytes(2).array());
		String strClientInfo = new String(in.readBytes(strClientInfoLen).array(),"utf-8");
		int strDeviceTokenLen = ProtocolUtil.byteArrayToInt(in.readBytes(2).array());
		String strDeviceToken = new String(in.readBytes(strDeviceTokenLen).array(),"utf-8");
		int build_type = ProtocolUtil.byteArrayToInt(in.readBytes(1).array());
		int aps_type = ProtocolUtil.byteArrayToInt(in.readBytes(1).array());
		int platform = ProtocolUtil.byteArrayToInt(in.readBytes(1).array());
		int strKeyExtLen = ProtocolUtil.byteArrayToInt(in.readBytes(2).array());
		String strKeyExt = new String(in.readBytes(strKeyExtLen).array(),"utf-8");
		int bussiness = ProtocolUtil.byteArrayToInt(in.readBytes(1).array());
		in.discardReadBytes();
		PushRegRequestBean bean = new PushRegRequestBean(strKey, strApkVersion, strClientInfo, strDeviceToken, build_type, aps_type, platform, strKeyExt);
		return bean;
	}
	
	public static PushLoginRequestBean getPushLoginRequestBean(ByteBuf in) throws UnsupportedEncodingException{
		String from_resources = new String(in.readBytes(4).array(),"utf-8");
		int passwordLen = ProtocolUtil.byteArrayToInt(in.readBytes(2).array());
		String password = new String(in.readBytes(passwordLen).array(),"utf-8");
		int client_version = ProtocolUtil.byteArrayToInt(in.readBytes(4).array());
		int appkeyLen = ProtocolUtil.byteArrayToInt(in.readBytes(2).array());
		String appkey = new String(in.readBytes(appkeyLen).array(),"utf-8");
		int platform = ProtocolUtil.byteArrayToInt(in.readBytes(1).array());
		in.discardReadBytes();
		PushLoginRequestBean bean = new PushLoginRequestBean(11, from_resources, password, client_version, appkey, platform);
		return bean;
	}
	
	public static PushRegResponseBean getPushRegResponseBean(ByteBuf in) throws UnsupportedEncodingException{
		int code = ProtocolUtil.byteArrayToInt(in.readBytes(2).array());
		long uid = ProtocolUtil.byteArrayToLong(in.readBytes(8).array());
		int passwd_len = ProtocolUtil.byteArrayToInt(in.readBytes(2).array());
		String passwd = new String(in.readBytes(passwd_len).array(),"utf-8");
		int regid_len = ProtocolUtil.byteArrayToInt(in.readBytes(2).array());
		String regid = new String(in.readBytes(regid_len).array(),"utf-8");
		//int deviceid_len = ProtocolUtil.byteArrayToInt(in.readBytes(2).array());
		//String deviceid = new String(in.readBytes(deviceid_len).array(),"utf-8");
		in.discardReadBytes();
		PushRegResponseBean bean = new PushRegResponseBean(code, uid, passwd, regid, "");
		return bean;
	}
	
	public static PushLoginResponseBean getPushLoginResponseBean(ByteBuf in) throws UnsupportedEncodingException{
		int code = ProtocolUtil.byteArrayToInt(in.readBytes(2).array());
		PushLoginResponseBean bean = null;
		if(code==0){
			int sid = ProtocolUtil.byteArrayToInt(in.readBytes(4).array());
			int server_version = ProtocolUtil.byteArrayToInt(in.readBytes(2).array());
			int session_key_len = ProtocolUtil.byteArrayToInt(in.readBytes(2).array());
			String session_key = new String(in.readBytes(session_key_len).array(),"utf-8");
			int server_time = ProtocolUtil.byteArrayToInt(in.readBytes(4).array());
			bean = new PushLoginResponseBean(code, sid, server_version, session_key, server_time);
		} else {
			log.error("push login exception -- code: "+code);
			int message_len = ProtocolUtil.byteArrayToInt(in.readBytes(2).array());
			String message = new String(in.readBytes(message_len).array(),"utf-8");
			log.info("push login error info: "+message);
		}
		in.discardReadBytes();
		return bean;
	}
	
	public static PushLogoutResponseBean getPushLogoutResponseBean(ByteBuf in) throws UnsupportedEncodingException{
		int code = ProtocolUtil.byteArrayToInt(in.readBytes(2).array());
		in.discardReadBytes();
		PushLogoutResponseBean bean = new PushLogoutResponseBean(code);
		return bean;
	}
	
	public static PushMessageRequestBean getPushMessageRequestBean(ByteBuf in) throws UnsupportedEncodingException{
		//in.readBytes(4); //  这里是个 push message 请求，不是响应，注意头的长度.
		int msgtype = ProtocolUtil.byteArrayToInt(in.readBytes(1).array());
		int msgid = ProtocolUtil.byteArrayToInt(in.readBytes(8).array());
		int message_len = ProtocolUtil.byteArrayToInt(in.readBytes(2).array());
		String message = new String(in.readBytes(message_len).array(),"utf-8");
		in.discardReadBytes();
		PushMessageRequestBean bean = new PushMessageRequestBean(msgtype, msgid, message);
		return bean;
	}
	
	//  IM ProtoBuf 数据解析
	public static Response getCommonResp(Packet protocol){
		Response resp = protocol.getBody().getCommonRep();
		return resp;
	}
	
	public static Login getLogin(Packet protocol){
		Login loginBean = protocol.getBody().getLogin();
		return loginBean;
	}

	public static Logout getLogout(Packet protocol){
		Logout logoutBean = protocol.getBody().getLogout();
		return logoutBean;
	}
	
	public static SingleMsg getSingleMsg(Packet protocol){
		SingleMsg singleMsgBean = protocol.getBody().getSingleMsg();
		return singleMsgBean;
	}
	
	public static GroupMsg getGroupMsg(Packet protocol){
		GroupMsg groupMsgBean = protocol.getBody().getGroupMsg();
		return groupMsgBean;
	}
	
	public static CreateGroup getCreateGroup(Packet protocol){
		CreateGroup createGroupBean = protocol.getBody().getCreateGroup();
		return createGroupBean;
	}
	
	public static ExitGroup getExitGroup(Packet protocol){
		ExitGroup exitGroupBean = protocol.getBody().getExitGroup();
		return exitGroupBean;
	}
	
	public static AddGroupMember getAddGroupMember(Packet protocol){
		AddGroupMember addGroupMemberBean = protocol.getBody().getAddGroupMember();
		return addGroupMemberBean;
	}
	
	public static DelGroupMember getDelGroupMember(Packet protocol){
		DelGroupMember delGroupMemberBean = protocol.getBody().getDelGroupMember();
		return delGroupMemberBean;
	}
	
	public static UpdateGroupInfo getUpdateGroupInfo(Packet protocol){
		UpdateGroupInfo updateGroupInfoBean = protocol.getBody().getUpdateGroupInfo();
		return updateGroupInfoBean;
	}
	
	public static EventNotification getEventNotification(Packet protocol){
		EventNotification eventNotification = protocol.getBody().getEventNotification();
		return eventNotification;
	}
	
	public static ChatMsgSync getChatMsgSync(Packet protocol){
		ChatMsgSync chatMsgSync = protocol.getBody().getChatMsg();
		return chatMsgSync;
	}
	
}


