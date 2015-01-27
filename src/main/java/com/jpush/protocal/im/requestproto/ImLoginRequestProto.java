package com.jpush.protocal.im.requestproto;

import java.util.List;

import jpushim.s2b.JpushimSdk2B;

import com.google.protobuf.ByteString;
import com.jpush.protocal.im.bean.LoginRequestBean;
import com.jpush.protocal.utils.Command;

public class ImLoginRequestProto extends BaseProtobufRequest {

	public ImLoginRequestProto(int cmd, int version, long uid, String appkey,
			List cookie, Object bean) {
		super(cmd, version, uid, appkey, cookie, bean);
	}

	@Override
	protected void buildBody(Object obj) {
		JpushimSdk2B.ProtocolBody.Builder bodyBuilder = JpushimSdk2B.ProtocolBody.newBuilder();
		LoginRequestBean bean = (LoginRequestBean) obj;
		JpushimSdk2B.Login.Builder loginBuilder = JpushimSdk2B.Login.newBuilder();
		loginBuilder.setUsername(ByteString.copyFromUtf8(bean.getUsername()));
		loginBuilder.setPassword(ByteString.copyFromUtf8(bean.getPassword()));
		loginBuilder.setPlatform(Command.DEVICE_TYPE.ANDROID);  // need modify
		bodyBuilder.setLogin(loginBuilder);
		protocalBuilder.setBody(bodyBuilder);
	}

}
