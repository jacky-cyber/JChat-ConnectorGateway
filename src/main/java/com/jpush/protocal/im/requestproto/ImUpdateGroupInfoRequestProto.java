package com.jpush.protocal.im.requestproto;

import java.util.List;

import com.jpush.protobuf.Group;
import com.jpush.protobuf.Im;
import com.jpush.protocal.im.bean.UpdateGroupInfoRequestBean;

public class ImUpdateGroupInfoRequestProto extends BaseProtobufRequest {

	public ImUpdateGroupInfoRequestProto(int cmd, int version, long uid,
			List cookie, Object bean) {
		super(cmd, version, uid, cookie, bean);
	}

	@Override
	protected void buildBody(Object obj) {
		Im.ProtocolBody.Builder bodyBuilder = Im.ProtocolBody.newBuilder();
		UpdateGroupInfoRequestBean bean = (UpdateGroupInfoRequestBean) obj;
		Group.UpdateGroupInfo.Builder updateGroupInfoMemberBuilder = Group.UpdateGroupInfo.newBuilder();
		updateGroupInfoMemberBuilder.setGid(bean.getGid());
		updateGroupInfoMemberBuilder.setName(bean.getName());
		updateGroupInfoMemberBuilder.setInfo(bean.getContent());
		bodyBuilder.setUpdateGroupInfo(updateGroupInfoMemberBuilder);
		protocalBuilder.setBody(bodyBuilder);
	}

}
