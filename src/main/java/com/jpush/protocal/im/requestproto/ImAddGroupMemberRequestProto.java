package com.jpush.protocal.im.requestproto;

import java.util.List;

import com.jpush.protobuf.Group;
import com.jpush.protobuf.Im;
import com.jpush.protocal.im.bean.AddGroupMemberRequestBean;

public class ImAddGroupMemberRequestProto extends BaseProtobufRequest {
	
	public ImAddGroupMemberRequestProto(int cmd, int version, long uid,
			List cookie, Object bean) {
		super(cmd, version, uid, cookie, bean);
	}

	@Override
	protected void buildBody(Object obj) {
		Im.ProtocolBody.Builder bodyBuilder = Im.ProtocolBody.newBuilder();
		AddGroupMemberRequestBean bean = (AddGroupMemberRequestBean) obj;
		Group.AddGroupMember.Builder addGroupMemberBuilder = Group.AddGroupMember.newBuilder();
		addGroupMemberBuilder.setGid(bean.getGid());
		addGroupMemberBuilder.setMemberCount(bean.getMember_count());
		List memList = bean.getMember_uid_list();
		for(int i=0; i<memList.size(); i++){
			addGroupMemberBuilder.addMemberUidlist((Long)memList.get(i));
		}
		bodyBuilder.setAddGroupMember(addGroupMemberBuilder);
		protocalBuilder.setBody(bodyBuilder);
	}

}
