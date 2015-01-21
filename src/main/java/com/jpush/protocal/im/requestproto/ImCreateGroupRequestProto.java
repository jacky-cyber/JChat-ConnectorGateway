package com.jpush.protocal.im.requestproto;

import java.util.List;

import jpushim.s2b.JpushimSdk2B;

import com.google.protobuf.ByteString;
import com.jpush.protocal.im.bean.CreateGroupRequestBean;

public class ImCreateGroupRequestProto extends BaseProtobufRequest {

	public ImCreateGroupRequestProto(int cmd, int version, long uid,
			List cookie, Object bean) {
		super(cmd, version, uid, cookie, bean);
	}

	@Override
	protected void buildBody(Object obj) {
		JpushimSdk2B.ProtocolBody.Builder bodyBuilder = JpushimSdk2B.ProtocolBody.newBuilder();
		CreateGroupRequestBean bean = (CreateGroupRequestBean) obj;
		JpushimSdk2B.CreateGroup.Builder createGroupBuilder = JpushimSdk2B.CreateGroup.newBuilder();
		createGroupBuilder.setGroupName(ByteString.copyFromUtf8(bean.getGroup_name()));
		createGroupBuilder.setGroupDesc(ByteString.copyFromUtf8(bean.getGroup_desc()));
		createGroupBuilder.setGroupLevel(bean.getGroup_level());
		createGroupBuilder.setFlag(bean.getFlag());
		bodyBuilder.setCreateGroup(createGroupBuilder);
		protocalBuilder.setBody(bodyBuilder);
	}

}
