package com.jpush.protocal.im.requestproto;

import java.util.List;

import jpushim.s2b.JpushimSdk2B;

import com.jpush.protocal.im.bean.ExitGroupRequestBean;

public class ImExitGroupRequestProto extends BaseProtobufRequest {

	public ImExitGroupRequestProto(int cmd, int version, long uid, List cookie,
			Object bean) {
		super(cmd, version, uid, cookie, bean);
	}

	@Override
	protected void buildBody(Object obj) {
		JpushimSdk2B.ProtocolBody.Builder bodyBuilder = JpushimSdk2B.ProtocolBody.newBuilder();
		ExitGroupRequestBean bean = (ExitGroupRequestBean) obj;
		JpushimSdk2B.ExitGroup.Builder exitGroupBuilder = JpushimSdk2B.ExitGroup.newBuilder();
		exitGroupBuilder.setGid(bean.getGid()); 
		bodyBuilder.setExitGroup(exitGroupBuilder);
		protocalBuilder.setBody(bodyBuilder);
	}

}
