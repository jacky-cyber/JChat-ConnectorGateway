package com.jpush.protocal.im.requestproto;

import java.util.List;

import com.jpush.protobuf.Group;
import com.jpush.protobuf.Im;
import com.jpush.protocal.im.bean.ExitGroupRequestBean;

public class ImExitGroupRequestProto extends BaseProtobufRequest {

	public ImExitGroupRequestProto(int cmd, int version, long uid, List cookie,
			Object bean) {
		super(cmd, version, uid, cookie, bean);
	}

	@Override
	protected void buildBody(Object obj) {
		Im.ProtocolBody.Builder bodyBuilder = Im.ProtocolBody.newBuilder();
		ExitGroupRequestBean bean = (ExitGroupRequestBean) obj;
		Group.ExitGroup.Builder exitGroupBuilder = Group.ExitGroup.newBuilder();
		exitGroupBuilder.setGid(bean.getGid()); 
		bodyBuilder.setExitGroup(exitGroupBuilder);
		protocalBuilder.setBody(bodyBuilder);
	}

}
