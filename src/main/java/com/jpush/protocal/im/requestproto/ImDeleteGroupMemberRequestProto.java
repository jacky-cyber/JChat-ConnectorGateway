package com.jpush.protocal.im.requestproto;

import java.util.List;

import com.jpush.protobuf.Group;
import com.jpush.protobuf.Im;
import com.jpush.protocal.im.bean.DeleteGroupMemberRequestBean;

public class ImDeleteGroupMemberRequestProto extends BaseProtobufRequest {
	public ImDeleteGroupMemberRequestProto(int cmd, int version, long uid,
			List cookie, Object bean) {
		super(cmd, version, uid, cookie, bean);
	}

	@Override
	protected void buildBody(Object obj) {
		Im.ProtocolBody.Builder bodyBuilder = Im.ProtocolBody.newBuilder();
		DeleteGroupMemberRequestBean bean = (DeleteGroupMemberRequestBean) obj;
		Group.DelGroupMember.Builder delGroupMemberBuilder = Group.DelGroupMember.newBuilder();
		delGroupMemberBuilder.setGid(bean.getGid());
		delGroupMemberBuilder.setMemberCount(bean.getMember_count());
		List memList = bean.getMember_uid_list();
		for(int i=0; i<memList.size(); i++){
			delGroupMemberBuilder.addMemberUidlist((Long)memList.get(i));
		}
		bodyBuilder.setDelGroupMember(delGroupMemberBuilder);
		protocalBuilder.setBody(bodyBuilder);
	}

}
