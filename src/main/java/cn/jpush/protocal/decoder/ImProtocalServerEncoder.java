package cn.jpush.protocal.decoder;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import cn.jpush.protocal.im.response.ImAddGroupMemberResponse;
import cn.jpush.protocal.im.response.ImCreateGroupResponse;
import cn.jpush.protocal.im.response.ImDeleteGroupMemberResponse;
import cn.jpush.protocal.im.response.ImExitGroupResponse;
import cn.jpush.protocal.im.response.ImGroupMsgResponse;
import cn.jpush.protocal.im.response.ImLoginResponse;
import cn.jpush.protocal.im.response.ImLogoutResponse;
import cn.jpush.protocal.im.response.ImResponse;
import cn.jpush.protocal.im.response.ImSingleMsgResponse;
import cn.jpush.protocal.im.response.ImUpdateGroupInfoResponse;
import cn.jpush.protocal.push.HeartBeatRequest;
import cn.jpush.protocal.push.HeartBeatResponse;
import cn.jpush.protocal.push.PushLoginResponse;
import cn.jpush.protocal.push.PushLoginResponseBean;
import cn.jpush.protocal.push.PushLogoutResponse;
import cn.jpush.protocal.push.PushLogoutResponseBean;
import cn.jpush.protocal.push.PushMessageRequest;
import cn.jpush.protocal.push.PushRegResponse;
import cn.jpush.protocal.push.PushRegResponseBean;
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
			log.info("返回 jpush reg 数据.");
			PushRegResponseBean bean = (PushRegResponseBean) msg;
			PushRegResponse response = new PushRegResponse(1, 2234, 243, bean);
			byte[] data = response.getResponsePackage();
			out = out.writeBytes(data);
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
		if(msg instanceof HeartBeatResponse){
			HeartBeatResponse response = (HeartBeatResponse) msg;
			byte[] data = response.getResponsePackage();
			out.writeBytes(data);
		}
		if(msg instanceof PushMessageRequest){  //  im 消息走 push
			PushMessageRequest response = (PushMessageRequest) msg;
			byte[] data = response.getRequestPackage();
			out.writeBytes(data);
		}
		if(msg instanceof ImResponse){  //  Im 业务响应
			ImResponse response = (ImResponse) msg;
			out.writeBytes(response.getResponsePackage());
		}
		
	}
	
	@Override
	public void flush(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		super.flush(ctx);
	}
	
}
