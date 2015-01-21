package com.jpush.protocal.im.requestproto;

import java.util.List;

import jpushim.s2b.JpushimSdk2B;

import com.google.protobuf.ByteString;
import com.jpush.protocal.im.bean.LogoutRequestBean;

public class ImLogoutRequestProto extends BaseProtobufRequest {

	public ImLogoutRequestProto(int cmd, int version, long uid, List cookie,
			Object bean) {
		super(cmd, version, uid, cookie, bean);
	}

	@Override
	protected void buildBody(Object obj) {
		JpushimSdk2B.ProtocolBody.Builder bodyBuilder = JpushimSdk2B.ProtocolBody.newBuilder();
		LogoutRequestBean bean = (LogoutRequestBean) obj;
		JpushimSdk2B.Logout.Builder logoutBuilder = JpushimSdk2B.Logout.newBuilder();
		logoutBuilder.setUsername(ByteString.copyFromUtf8(bean.getUsername()));
		logoutBuilder.setAppkey(ByteString.copyFromUtf8(bean.getAppkey()));
		bodyBuilder.setLogout(logoutBuilder);
		protocalBuilder.setBody(bodyBuilder);
	}

}
