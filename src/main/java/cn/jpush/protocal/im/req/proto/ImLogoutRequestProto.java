package cn.jpush.protocal.im.req.proto;

import java.util.List;

import jpushim.s2b.JpushimSdk2B;
import cn.jpush.protocal.im.bean.LogoutRequestBean;

import com.google.protobuf.ByteString;

public class ImLogoutRequestProto extends BaseProtobufRequest {

	public ImLogoutRequestProto(int cmd, int version, long uid, String appkey,
			List cookie, Object bean) {
		super(cmd, version, uid, appkey, cookie, bean);
	}

	@Override
	protected void buildBody(Object obj) {
		JpushimSdk2B.ProtocolBody.Builder bodyBuilder = JpushimSdk2B.ProtocolBody.newBuilder();
		LogoutRequestBean bean = (LogoutRequestBean) obj;
		JpushimSdk2B.Logout.Builder logoutBuilder = JpushimSdk2B.Logout.newBuilder();
		logoutBuilder.setUsername(ByteString.copyFromUtf8(bean.getUsername()));
		bodyBuilder.setLogout(logoutBuilder);
		protocalBuilder.setBody(bodyBuilder);
	}

}
