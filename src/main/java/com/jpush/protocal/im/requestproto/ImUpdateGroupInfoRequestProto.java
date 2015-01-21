package com.jpush.protocal.im.requestproto;

import java.util.List;

import jpushim.s2b.JpushimSdk2B;

import com.google.protobuf.ByteString;
import com.jpush.protocal.im.bean.UpdateGroupInfoRequestBean;

public class ImUpdateGroupInfoRequestProto extends BaseProtobufRequest {

	public ImUpdateGroupInfoRequestProto(int cmd, int version, long uid,
			List cookie, Object bean) {
		super(cmd, version, uid, cookie, bean);
	}

	@Override
	protected void buildBody(Object obj) {
		JpushimSdk2B.ProtocolBody.Builder bodyBuilder = JpushimSdk2B.ProtocolBody.newBuilder();
		UpdateGroupInfoRequestBean bean = (UpdateGroupInfoRequestBean) obj;
		JpushimSdk2B.UpdateGroupInfo.Builder updateGroupInfoMemberBuilder = JpushimSdk2B.UpdateGroupInfo.newBuilder();
		updateGroupInfoMemberBuilder.setGid(bean.getGid());
		updateGroupInfoMemberBuilder.setName(ByteString.copyFromUtf8(bean.getName()));
		updateGroupInfoMemberBuilder.setInfo(ByteString.copyFromUtf8(bean.getContent()));
		bodyBuilder.setUpdateGroupInfo(updateGroupInfoMemberBuilder);
		protocalBuilder.setBody(bodyBuilder);
	}

}
