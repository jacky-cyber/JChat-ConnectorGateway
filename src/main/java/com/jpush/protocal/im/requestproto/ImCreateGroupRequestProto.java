package com.jpush.protocal.im.requestproto;

import java.util.List;

import com.jpush.protobuf.Group;
import com.jpush.protobuf.Im;
import com.jpush.protocal.im.bean.CreateGroupRequestBean;

public class ImCreateGroupRequestProto extends BaseProtobufRequest {

	public ImCreateGroupRequestProto(int cmd, int version, long uid,
			List cookie, Object bean) {
		super(cmd, version, uid, cookie, bean);
	}

	@Override
	protected void buildBody(Object obj) {
		Im.ProtocolBody.Builder bodyBuilder = Im.ProtocolBody.newBuilder();
		CreateGroupRequestBean bean = (CreateGroupRequestBean) obj;
		Group.CreateGroup.Builder createGroupBuilder = Group.CreateGroup.newBuilder();
		createGroupBuilder.setGroupName(bean.getGroup_name());
		createGroupBuilder.setGroupDesc(bean.getGroup_desc());
		createGroupBuilder.setGroupLevel(bean.getGroup_level());
		createGroupBuilder.setFlag(bean.getFlag());
		bodyBuilder.setCreateGroup(createGroupBuilder);
		protocalBuilder.setBody(bodyBuilder);
	}

}
