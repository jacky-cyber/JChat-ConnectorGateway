package cn.jpush.protocal.im.req.proto;

import java.util.List;

import jpushim.s2b.JpushimSdk2B;
import jpushim.s2b.JpushimSdk2B.Packet;
import cn.jpush.protocal.utils.Command;

import com.google.protobuf.ByteString;

/*
 * IM 协议 Protobuf 结构请求封装
 */
public class BaseProtobufRequest {
	private int cmd;
	private int version;
	private long uid;
	private String appkey;
	private List<Integer> cookie;
	private Object bean;
	protected Packet.Builder protocalBuilder = Packet.newBuilder();
	public BaseProtobufRequest(int cmd, int version, long uid, String appkey, List cookie, Object bean) {
		super();
		this.cmd = cmd;
		this.version = version;
		this.uid = uid;
		this.appkey = appkey;
		this.cookie = cookie;
		this.bean = bean;
	}
	
	public Packet buildProtoBufProtocal(){
		this.buildHead();
		this.buildBody(this.bean);
		return protocalBuilder.build();
	}
	
	protected void buildBody(Object obj){}
	
	private void buildHead(){
		JpushimSdk2B.ProtocolHead.Builder headBuilder = JpushimSdk2B.ProtocolHead.newBuilder();
		headBuilder.setCmd(this.cmd);
		headBuilder.setVer(this.version);
		headBuilder.setUid(this.uid);
		headBuilder.setAppkey(ByteString.copyFromUtf8(this.appkey));
		JpushimSdk2B.Cookie.Builder cookieBuilder = JpushimSdk2B.Cookie.newBuilder();
		for(int i=0; i<cookie.size(); i++){
			cookieBuilder.addRes(cookie.get(i));
		}
		headBuilder.setCookie(cookieBuilder);
		protocalBuilder.setHead(headBuilder);
	}
	
	public int getCmd() {
		return cmd;
	}

	public void setCmd(int cmd) {
		this.cmd = cmd;
	}

	public int getVersion() {
		return version;
	}
	public void setVersion(int version) {
		this.version = version;
	}
	public long getUid() {
		return uid;
	}
	public void setUid(long uid) {
		this.uid = uid;
	}

	public List<Integer> getCookie() {
		return cookie;
	}

	public void setCookie(List<Integer> cookie) {
		this.cookie = cookie;
	}

	public String getAppkey() {
		return appkey;
	}

	public void setAppkey(String appkey) {
		this.appkey = appkey;
	}

	public Object getBean() {
		return bean;
	}

	public void setBean(Object bean) {
		this.bean = bean;
	}
	
}
