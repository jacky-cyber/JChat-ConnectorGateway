package com.jpush.protocal.decoder;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

import com.jpush.protobuf.Im;
import com.jpush.protobuf.Im.Protocol;
import com.jpush.protobuf.Im.ProtocolBody;
import com.jpush.protobuf.User.Login;
import com.jpush.protocal.common.JPushTcpServerHandler;
import com.jpush.protocal.push.PushLoginRequestBean;
import com.jpush.protocal.push.PushRegRequestBean;
import com.jpush.protocal.utils.Command;
import com.jpush.protocal.utils.ProtocolUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 * IM 协议解码器 从字节流到对象
 */
public class ImProtocalServerDecoder extends ByteToMessageDecoder {
	private static Logger log = (Logger) LoggerFactory.getLogger(ImProtocalServerDecoder.class);
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in,
			List<Object> out) throws Exception {
		log.info("服务端开始decode......");
		while(in.isReadable()){
			int length = in.readableBytes();
			log.info("可读数据量为："+length);
			if(length<2){
				return;
			} else {
				byte[] len = in.readBytes(2).array();
				in.resetReaderIndex();  // reset readIndex 
				int pkg_len = ProtocolUtil.byteArrayToInt(len);
				log.info("数据包大小为： "+pkg_len);
				if(length<pkg_len){
					return;
				} else {
					in.readBytes(3).array();
					int command = ProtocolUtil.byteArrayToInt(in.readBytes(1).array());
					in.readBytes(20);
					if(command==Command.KKPUSH_REG.COMMAND){  // push reg
						log.info("jpush reg request decoder...");
						int cmd = ProtocolUtil.byteArrayToInt(in.readBytes(1).array());
						int strKeyLen = ProtocolUtil.byteArrayToInt(in.readBytes(2).array());
						String strKey = new String(in.readBytes(strKeyLen).array(),"utf-8");
						int strApkVersionLen = ProtocolUtil.byteArrayToInt(in.readBytes(2).array());
						String strApkVersion = new String(in.readBytes(strApkVersionLen).array(),"utf-8");
						int strClientInfoLen = ProtocolUtil.byteArrayToInt(in.readBytes(2).array());
						String strClientInfo = new String(in.readBytes(strClientInfoLen).array(),"utf-8");
						int strDeviceTokenLen = ProtocolUtil.byteArrayToInt(in.readBytes(2).array());
						String strDeviceToken = new String(in.readBytes(strDeviceTokenLen).array(),"utf-8");
						int build_type = ProtocolUtil.byteArrayToInt(in.readBytes(1).array());
						int aps_type = ProtocolUtil.byteArrayToInt(in.readBytes(1).array());
						int platform = ProtocolUtil.byteArrayToInt(in.readBytes(1).array());
						int strKeyExtLen = ProtocolUtil.byteArrayToInt(in.readBytes(2).array());
						String strKeyExt = new String(in.readBytes(strKeyExtLen).array(),"utf-8");
						in.discardReadBytes();
						PushRegRequestBean bean = new PushRegRequestBean(strKey, strApkVersion, strClientInfo, strDeviceToken, build_type, aps_type, platform, strKeyExt);
						out.add(bean);
					} else if(command==Command.KKPUSH_LOGIN.COMMAND){  // push login
						log.info("jpush login request decoder...");
						int cmd = ProtocolUtil.byteArrayToInt(in.readBytes(1).array());
						String from_resources = new String(in.readBytes(4).array(),"utf-8");
						int passwordLen = ProtocolUtil.byteArrayToInt(in.readBytes(2).array());
						String password = new String(in.readBytes(passwordLen).array(),"utf-8");
						int client_version = ProtocolUtil.byteArrayToInt(in.readBytes(4).array());
						int appkeyLen = ProtocolUtil.byteArrayToInt(in.readBytes(2).array());
						String appkey = new String(in.readBytes(appkeyLen).array(),"utf-8");
						int platform = ProtocolUtil.byteArrayToInt(in.readBytes(1).array());
						PushLoginRequestBean bean = new PushLoginRequestBean(from_resources, password, client_version, appkey, platform);
						out.add(bean);
					} else if(command==Command.KKPUSH_LOGOUT.COMMAND){  // push logout
						log.info("jpush logout request decoder...");
						int cmd = ProtocolUtil.byteArrayToInt(in.readBytes(1).array());
						out.add(cmd);
					} else if(command==Command.JPUSH_IM.COMMAND){  // im 业务
						log.info("im 业务请求....");
						Protocol protocol = Im.Protocol.parseFrom(in.readBytes(pkg_len-24).array());
						out.add(protocol);
					}
				
				}
			}
		}
	}

}
