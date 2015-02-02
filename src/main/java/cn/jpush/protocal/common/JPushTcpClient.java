package cn.jpush.protocal.common;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import cn.jpush.protocal.encoder.ImProtocalClientDecoder;
import cn.jpush.protocal.encoder.ImProtocalClientEncoder;
import cn.jpush.protocal.im.bean.AddGroupMemberRequestBean;
import cn.jpush.protocal.im.bean.CreateGroupRequestBean;
import cn.jpush.protocal.im.bean.DeleteGroupMemberRequestBean;
import cn.jpush.protocal.im.bean.ExitGroupRequestBean;
import cn.jpush.protocal.im.bean.LoginRequestBean;
import cn.jpush.protocal.im.bean.LogoutRequestBean;
import cn.jpush.protocal.im.bean.SendGroupMsgRequestBean;
import cn.jpush.protocal.im.bean.SendSingleMsgRequestBean;
import cn.jpush.protocal.im.bean.UpdateGroupInfoRequestBean;
import cn.jpush.protocal.im.req.proto.ImAddGroupMemberRequestProto;
import cn.jpush.protocal.im.req.proto.ImCreateGroupRequestProto;
import cn.jpush.protocal.im.req.proto.ImDeleteGroupMemberRequestProto;
import cn.jpush.protocal.im.req.proto.ImExitGroupRequestProto;
import cn.jpush.protocal.im.req.proto.ImLoginRequestProto;
import cn.jpush.protocal.im.req.proto.ImLogoutRequestProto;
import cn.jpush.protocal.im.req.proto.ImSendGroupMsgRequestProto;
import cn.jpush.protocal.im.req.proto.ImSendSingleMsgRequestProto;
import cn.jpush.protocal.im.req.proto.ImUpdateGroupInfoRequestProto;
import cn.jpush.protocal.push.PushLoginRequestBean;
import cn.jpush.protocal.push.PushLogoutRequest;
import cn.jpush.protocal.push.PushRegRequestBean;
import cn.jpush.protocal.utils.Command;
import cn.jpush.protocal.utils.SystemConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;

public class JPushTcpClient {
	private static Logger log = (Logger) LoggerFactory.getLogger(JPushTcpClient.class);
	//private static final String HOST = "183.232.38.229";
	private static final int PORT = SystemConfig.getIntProperty("jpush.server.port"); 
	private static final String HOST = SystemConfig.getProperty("jpush.server.host");
	
	private Bootstrap b; 
	private EventLoopGroup workGroup;

	public JPushTcpClient(){
		b = new Bootstrap();
		try {
			this.init();
		} catch (InterruptedException e) {
			log.error("init client failture, please try again.");
		}
	}
	
	public void init() throws InterruptedException{
		log.info("netty tcp client is init......");
		workGroup = new NioEventLoopGroup();
		b.group(workGroup);
		b.channel(NioSocketChannel.class);
		b.option(ChannelOption.TCP_NODELAY, true);
		b.option(ChannelOption.SO_KEEPALIVE, true);
		b.handler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline()/*.addLast("idleStateHandler", new IdleStateHandler(50, 50, 0))*/
								.addLast(new ImProtocalClientEncoder())
								.addLast(new ImProtocalClientDecoder())
								.addLast(new JPushTcpClientHandler());
					
			}	
		});		
	}
	
	public Channel getChannel(){
		Channel channel = null;
		try {
			channel = b.connect(HOST, PORT).sync().channel();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return channel;
	}
	
	public void sendRequest(final Channel channel, Object request) throws InterruptedException{
		if(channel==null||request==null)
			throw new IllegalArgumentException("send request arguments exception");
		ChannelFuture future = channel.writeAndFlush(request);
		future.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				log.info("请求发送完成.");
				//workGroup.shutdownGracefully();
			}
		});
		channel.closeFuture().sync();
	}
	
	public static void main(String[] args) {
		JPushTcpClient client = new JPushTcpClient();
		try {
			//client.init();
			log.info("success to connect the server.");
			Channel channel = client.getChannel();
			//PushLoginRequestBean req = new PushLoginRequestBean("android", "sdfqw23", 1, "appkey:123d32", 5);
			//PushRegRequestBean req = new PushRegRequestBean("ssfe", "1.0.1", "web im", "service token232", 1, 2, 5, "null ext");
			//PushLogoutRequest req = new PushLogoutRequest(1, 23, 123, 43);
			/******  im 业务     *********/
			//  login
			LoginRequestBean bean = new LoginRequestBean("walter","password123");
			List<Integer> cookie = new ArrayList<Integer>();
			cookie.add(123);
			ImLoginRequestProto req = new ImLoginRequestProto(Command.JPUSH_IM.LOGIN, 1, 2324, SystemConfig.getProperty("jpush.appkey"), cookie, bean);
			// logout
		   /*LogoutRequestBean bean = new LogoutRequestBean("walter");
			List<Integer> cookie = new ArrayList<Integer>();
			cookie.add(123);
			ImLogoutRequestProto req = new ImLogoutRequestProto(Command.JPUSH_IM.LOGOUT, 1, 2324, SystemConfig.getProperty("jpush.appkey"), cookie, bean);*/
			//* send single message
			/*SendSingleMsgRequestBean bean = new SendSingleMsgRequestBean(32451225, "this is a single msg.");
			List<Integer> cookie = new ArrayList<Integer>();
			cookie.add(123);
			ImSendSingleMsgRequestProto req = new ImSendSingleMsgRequestProto(Command.JPUSH_IM.SENDMSG_SINGAL, 1, 2324, SystemConfig.getProperty("jpush.appkey"), cookie, bean);*/
			//* send group message
		   /*SendGroupMsgRequestBean bean = new SendGroupMsgRequestBean(345342, "this is a group msg");
			List<Integer> cookie = new ArrayList<Integer>();
			cookie.add(123);
			ImSendGroupMsgRequestProto req = new ImSendGroupMsgRequestProto(Command.JPUSH_IM.SENDMSG_GROUP, 1, 2312, SystemConfig.getProperty("jpush.appkey"), cookie, bean);*/
			//* create group request
			/*CreateGroupRequestBean bean = new CreateGroupRequestBean("group_001", "Jpush Group", 1, 2);
			List<Integer> cookie = new ArrayList<Integer>();
			cookie.add(123);
			ImCreateGroupRequestProto req = new ImCreateGroupRequestProto(Command.JPUSH_IM.CREATE_GROUP, 1, 23332, SystemConfig.getProperty("jpush.appkey"), cookie, bean);*/
			//* exit group message
			/*ExitGroupRequestBean bean = new ExitGroupRequestBean(123456);
			List<Integer> cookie = new ArrayList<Integer>();
			cookie.add(123);
			ImExitGroupRequestProto req = new ImExitGroupRequestProto(Command.JPUSH_IM.EXIT_GROUP, 1, 12212, SystemConfig.getProperty("jpush.appkey"), cookie, bean);*/
			// add group members
			/*List<Long> list = new ArrayList();
			list.add(12334L);list.add(546232L);list.add(456456L);
			AddGroupMemberRequestBean bean = new AddGroupMemberRequestBean(12234, 3, list);
			List<Integer> cookie = new ArrayList<Integer>();
			cookie.add(123);
			ImAddGroupMemberRequestProto req = new ImAddGroupMemberRequestProto(Command.JPUSH_IM.ADD_GROUP_MEMBER, 1, 1232, SystemConfig.getProperty("jpush.appkey"), cookie, bean);*/
			//* delete group members
			/*List<Long> list = new ArrayList();
			list.add(1111L);list.add(22222L);list.add(555556L);
			DeleteGroupMemberRequestBean bean = new DeleteGroupMemberRequestBean(12234, 3, list);
			List<Integer> cookie = new ArrayList<Integer>();
			cookie.add(123);
			ImDeleteGroupMemberRequestProto req = new ImDeleteGroupMemberRequestProto(Command.JPUSH_IM.DEL_GROUP_MEMBER, 1, 2323, SystemConfig.getProperty("jpush.appkey"), cookie, bean);*/
			//* update group info message
			/*UpdateGroupInfoRequestBean bean = new UpdateGroupInfoRequestBean(1234, "jpush dev group", "modify group info");
			List<Integer> cookie = new ArrayList<Integer>();
			cookie.add(123);
			ImUpdateGroupInfoRequestProto req = new ImUpdateGroupInfoRequestProto(Command.JPUSH_IM.UPDATE_GROUP_INFO, 1, 23321, SystemConfig.getProperty("jpush.appkey"), cookie, bean);*/
			
			client.sendRequest(channel, req);
		} catch (InterruptedException e) {
			log.error("connect server exception."+e.getMessage());
		}
	}

}