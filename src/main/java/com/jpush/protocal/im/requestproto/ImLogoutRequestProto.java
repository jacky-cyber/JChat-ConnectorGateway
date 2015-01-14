package com.jpush.protocal.im.requestproto;

import java.util.List;

import com.jpush.protobuf.Im;
import com.jpush.protobuf.User;
import com.jpush.protocal.im.bean.LogoutRequestBean;

public class ImLogoutRequestProto extends BaseProtobufRequest {

	public ImLogoutRequestProto(int cmd, int version, long uid, List cookie,
			Object bean) {
		super(cmd, version, uid, cookie, bean);
	}

	@Override
	protected void buildBody(Object obj) {
		Im.ProtocolBody.Builder bodyBuilder = Im.ProtocolBody.newBuilder();
		LogoutRequestBean bean = (LogoutRequestBean) obj;
		User.Logout.Builder logoutBuilder = User.Logout.newBuilder();
		logoutBuilder.setUsername(bean.getUsername());
		logoutBuilder.setAppkey(bean.getAppkey());
		bodyBuilder.setLogout(logoutBuilder);
		protocalBuilder.setBody(bodyBuilder);
	}

}
