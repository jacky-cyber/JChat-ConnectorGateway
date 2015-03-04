package cn.jpush.protocal.im.req.proto;

import java.util.List;

import jpushim.s2b.JpushimSdk2B;
import cn.jpush.protocal.im.bean.LoginRequestBean;
import cn.jpush.protocal.utils.Command;

import com.google.protobuf.ByteString;

public class ImLoginRequestProto extends BaseProtobufRequest {
	private JpushimSdk2B.Login.Builder loginBuilder;
	private int sid;
	private long juid;
	public ImLoginRequestProto(int cmd, int version, long uid, String appkey,
			List<Integer> cookie, Object bean) {
		super(cmd, version, uid, appkey, cookie, bean);
	}
	
	public ImLoginRequestProto(int cmd, int version, long uid, int sid, long juid, String appkey,
			List<Integer> cookie, Object bean) {
		super(cmd, version, uid, appkey, cookie, bean);
		this.sid = sid;
		this.juid = juid;
	}

	@Override
	protected void buildBody(Object obj) {
		JpushimSdk2B.ProtocolBody.Builder bodyBuilder = JpushimSdk2B.ProtocolBody.newBuilder();
		LoginRequestBean bean = (LoginRequestBean) obj;
		loginBuilder = JpushimSdk2B.Login.newBuilder();
		loginBuilder.setUsername(ByteString.copyFromUtf8(bean.getUsername()));
		loginBuilder.setPassword(ByteString.copyFromUtf8(bean.getPassword()));
		loginBuilder.setPlatform(Command.DEVICE_TYPE.ANDROID);  // need modify
		bodyBuilder.setLogin(loginBuilder);
		protocalBuilder.setBody(bodyBuilder);
	}

	public JpushimSdk2B.Login.Builder getLoginBuilder() {
		return loginBuilder;
	}

	public void setLoginBuilder(JpushimSdk2B.Login.Builder loginBuilder) {
		this.loginBuilder = loginBuilder;
	}

	public int getSid() {
		return sid;
	}

	public void setSid(int sid) {
		this.sid = sid;
	}

	public long getJuid() {
		return juid;
	}

	public void setJuid(long juid) {
		this.juid = juid;
	}

}
