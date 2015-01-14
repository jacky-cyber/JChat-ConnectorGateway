package com.jpush.protocal.decoder;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

import com.jpush.protocal.im.response.ImAddGroupMemberResponse;
import com.jpush.protocal.im.response.ImCreateGroupResponse;
import com.jpush.protocal.im.response.ImDeleteGroupMemberResponse;
import com.jpush.protocal.im.response.ImExitGroupResponse;
import com.jpush.protocal.im.response.ImGroupMsgResponse;
import com.jpush.protocal.im.response.ImLoginResponse;
import com.jpush.protocal.im.response.ImLogoutResponse;
import com.jpush.protocal.im.response.ImResponse;
import com.jpush.protocal.im.response.ImSingleMsgResponse;
import com.jpush.protocal.im.response.ImUpdateGroupInfoResponse;
import com.jpush.protocal.push.HeartBeatRequest;
import com.jpush.protocal.push.PushLoginResponse;
import com.jpush.protocal.push.PushLoginResponseBean;
import com.jpush.protocal.push.PushLogoutResponse;
import com.jpush.protocal.push.PushLogoutResponseBean;
import com.jpush.protocal.push.PushRegResponse;
import com.jpush.protocal.push.PushRegResponseBean;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class ImProtocalServerEncoder extends MessageToByteEncoder<Object> {
	private static Logger log = (Logger) LoggerFactory.getLogger(ImProtocalServerEncoder.class);
	@Override
	protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out)
			throws Exception {
		log.info("服务端开始encode....");
		if(msg instanceof PushRegResponseBean){
			PushRegResponseBean bean = (PushRegResponseBean) msg;
			PushRegResponse response = new PushRegResponse(1, 2234, 243, bean);
			byte[] data = response.getResponsePackage();
			out.writeBytes(data);
		}
		if(msg instanceof PushLoginResponseBean){
			PushLoginResponseBean bean = (PushLoginResponseBean) msg;
			PushLoginResponse response = new PushLoginResponse(1, 1223, 23424, bean);
			byte[] data = response.getResponsePackage();
			out.writeBytes(data);
		}
		if(msg instanceof PushLogoutResponseBean){
			PushLogoutResponseBean bean = (PushLogoutResponseBean) msg;
			PushLogoutResponse response = new PushLogoutResponse(1, 1223, 23424, bean);
			byte[] data = response.getResponsePackage();
			out.writeBytes(data);
		}
		if(msg instanceof HeartBeatRequest){
			HeartBeatRequest request = (HeartBeatRequest) msg;
			byte[] data = request.getRequestPackage();
			out.writeBytes(data);
		}
		if(msg instanceof ImResponse){  //  Im 业务响应
			ImResponse response = (ImResponse) msg;
			out.writeBytes(response.getResponsePackage());
		}
		
	}

}
