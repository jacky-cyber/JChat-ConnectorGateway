package com.jpush.protocal.im.requestproto;

import java.util.List;

import com.jpush.protobuf.Im;
import com.jpush.protobuf.User;
import com.jpush.protocal.common.Command;
import com.jpush.protocal.im.bean.LoginRequestBean;

public class ImLoginRequestProto extends BaseProtobufRequest {

	public ImLoginRequestProto(int cmd, int version, long uid, List cookie,
			Object bean) {
		super(cmd, version, uid, cookie, bean);
	}

	@Override
	protected void buildBody(Object obj) {
		Im.ProtocolBody.Builder bodyBuilder = Im.ProtocolBody.newBuilder();
		LoginRequestBean bean = (LoginRequestBean) obj;
		User.Login.Builder loginBuilder = User.Login.newBuilder();
		loginBuilder.setUsername(bean.getUsername());
		loginBuilder.setPassword(bean.getPassword());
		loginBuilder.setAppkey(bean.getAppkey());
		loginBuilder.setPlatform(Command.DEVICE_TYPE.ANDROID);  // need modify
		bodyBuilder.setLogin(loginBuilder);
		protocalBuilder.setBody(bodyBuilder);
	}

}
