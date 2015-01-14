package com.jpush.protocal.encoder;

import java.util.List;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

import com.jpush.protobuf.Im;
import com.jpush.protobuf.Im.Protocol;
import com.jpush.protocal.decoder.ImProtocalServerEncoder;
import com.jpush.protocal.push.PushLoginResponseBean;
import com.jpush.protocal.push.PushLogoutResponseBean;
import com.jpush.protocal.push.PushRegResponseBean;
import com.jpush.protocal.utils.Command;
import com.jpush.protocal.utils.ProtocolUtil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class ImProtocalClientDecoder extends ByteToMessageDecoder {
	private static Logger log = (Logger) LoggerFactory.getLogger(ImProtocalClientDecoder.class);
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in,
			List<Object> out) throws Exception {
		log.info("客户端接收数据开始decode......");
		int length = in.readableBytes();
		if(length<2){
			return;
		} else {
			byte[] len = in.readBytes(2).array();
			in.resetReaderIndex();  // reset readIndex 
			int pkg_len = ProtocolUtil.byteArrayToInt(len);
			log.info("客户端接收数据包大小为： "+pkg_len);
			if(length<pkg_len){
				return;
			} else {
				in.readBytes(3).array();
				int command = ProtocolUtil.byteArrayToInt(in.readBytes(1).array());
				in.readBytes(16).array();
				if(command==Command.KKPUSH_REG.COMMAND){
					log.info("receive push reg response");
					int code = ProtocolUtil.byteArrayToInt(in.readBytes(2).array());
					long uid = ProtocolUtil.byteArrayToLong(in.readBytes(8).array());
					int passwd_len = ProtocolUtil.byteArrayToInt(in.readBytes(2).array());
					String passwd = new String(in.readBytes(passwd_len).array(),"utf-8");
					int regid_len = ProtocolUtil.byteArrayToInt(in.readBytes(2).array());
					String regid = new String(in.readBytes(regid_len).array(),"utf-8");
					int deviceid_len = ProtocolUtil.byteArrayToInt(in.readBytes(2).array());
					String deviceid = new String(in.readBytes(deviceid_len).array(),"utf-8");
				
					PushRegResponseBean bean = new PushRegResponseBean(code, uid, passwd, regid, deviceid);
					out.add(bean);
				}
				if(command==Command.KKPUSH_LOGIN.COMMAND){
					log.info("receive push login response");
					int code = ProtocolUtil.byteArrayToInt(in.readBytes(2).array());
					int sid = ProtocolUtil.byteArrayToInt(in.readBytes(4).array());
					int server_version = ProtocolUtil.byteArrayToInt(in.readBytes(2).array());
					int session_key_len = ProtocolUtil.byteArrayToInt(in.readBytes(2).array());
					String session_key = new String(in.readBytes(session_key_len).array(),"utf-8");
					int server_time = ProtocolUtil.byteArrayToInt(in.readBytes(4).array());
					PushLoginResponseBean bean = new PushLoginResponseBean(code, sid, server_version, session_key, server_time);
					out.add(bean);
				}
				if(command==Command.KKPUSH_LOGOUT.COMMAND){   // logout response
					log.info("receive push logout response");
					int code = ProtocolUtil.byteArrayToInt(in.readBytes(2).array());
					PushLogoutResponseBean bean = new PushLogoutResponseBean(code);
					out.add(bean);
				}
				if(command==Command.KKPUSH_HEARTBEAT.COMMAND){  // heart beat request
					log.info("receive push heartbeat request");
					in.readBytes(4).array();
					int resp_command = ProtocolUtil.byteArrayToInt(in.readBytes(1).array());
					int version = ProtocolUtil.byteArrayToInt(in.readBytes(1).array());
					log.info("server heart beat request data info, cmd:"+resp_command+", version："+version);
				}
				if(command==Command.JPUSH_IM.COMMAND){
					log.info("im 业务响应....");
					Protocol protocol = Im.Protocol.parseFrom(in.readBytes(pkg_len-20).array());
					out.add(protocol);
				}
			}
		}
	}

}
