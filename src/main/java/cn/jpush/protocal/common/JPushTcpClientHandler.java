package cn.jpush.protocal.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jpushim.s2b.JpushimSdk2B.AddGroupMember;
import jpushim.s2b.JpushimSdk2B.ChatMsg;
import jpushim.s2b.JpushimSdk2B.ChatMsgSync;
import jpushim.s2b.JpushimSdk2B.CreateGroup;
import jpushim.s2b.JpushimSdk2B.DelGroupMember;
import jpushim.s2b.JpushimSdk2B.EventNotification;
import jpushim.s2b.JpushimSdk2B.ExitGroup;
import jpushim.s2b.JpushimSdk2B.GroupMsg;
import jpushim.s2b.JpushimSdk2B.Login;
import jpushim.s2b.JpushimSdk2B.Logout;
import jpushim.s2b.JpushimSdk2B.Packet;
import jpushim.s2b.JpushimSdk2B.Response;
import jpushim.s2b.JpushimSdk2B.SingleMsg;
import jpushim.s2b.JpushimSdk2B.UpdateGroupInfo;

import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import ch.qos.logback.classic.Logger;
import cn.jpush.protocal.im.req.proto.ImChatMsgSyncRequestProto;
import cn.jpush.protocal.im.resp.proto.ImLoginResponseProto;
import cn.jpush.protocal.im.response.ImLoginResponse;
import cn.jpush.protocal.push.HeartBeatRequest;
import cn.jpush.protocal.push.PushLoginResponseBean;
import cn.jpush.protocal.push.PushLogoutResponseBean;
import cn.jpush.protocal.push.PushMessageRequestBean;
import cn.jpush.protocal.push.PushRegResponseBean;
import cn.jpush.protocal.utils.APIProxy;
import cn.jpush.protocal.utils.BASE64Utils;
import cn.jpush.protocal.utils.Command;
import cn.jpush.protocal.utils.HttpResponseWrapper;
import cn.jpush.protocal.utils.JMessage;
import cn.jpush.protocal.utils.ProtocolUtil;
import cn.jpush.protocal.utils.StringUtils;
import cn.jpush.protocal.utils.TcpCode;
import cn.jpush.socketio.SocketIOClient;
import cn.jpush.webim.common.RedisClient;
import cn.jpush.webim.common.UidResourcesPool;
import cn.jpush.webim.server.V1;
import cn.jpush.webim.server.WebImServer;
import cn.jpush.webim.socketio.bean.ChatMessageObject;
import cn.jpush.webim.socketio.bean.Group;
import cn.jpush.webim.socketio.bean.IMPacket;
import cn.jpush.webim.socketio.bean.MsgContentBean;
import cn.jpush.webim.socketio.bean.SdkCommonErrorRespObject;
import cn.jpush.webim.socketio.bean.SdkCommonSuccessRespObject;
import cn.jpush.webim.socketio.bean.SdkGroupObject;
import cn.jpush.webim.socketio.bean.SdkSyncEventObject;
import cn.jpush.webim.socketio.bean.SdkSyncMsgObject;
import cn.jpush.webim.socketio.bean.SdkUserInfoObject;

import com.google.gson.Gson;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class JPushTcpClientHandler extends ChannelInboundHandlerAdapter {
	private static Logger log = (Logger) LoggerFactory.getLogger(JPushTcpClientHandler.class);
	private static final String VERSION = "1.0";
	private static final String DATA_AISLE = "data";
	private RedisClient redisClient = new RedisClient();
	private Gson gson = new Gson();
	private PushLoginResponseBean pushLoginResponseBean;
	private int sid;
	private long juid;

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		log.info(String.format("handler: %s added", ctx.channel().toString()));
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) {
		Channel channel = ctx.channel();
		log.warn(String.format("handler: %s removed from push server",
				channel.toString()));
		// 与JPush Server断开后，通知相应用户掉线
		if (channel != null) {
			String kan = "";
			try {
				kan = WebImServer.pushChannelToUsernameMap.get(channel);
			} catch (Exception e) {
				log.warn(String.format("handler removed exception: %s",
						e.getMessage()));
			}
			if (StringUtils.isNotEmpty(kan)) {
				SocketIOClient sessionClient = WebImServer.userNameToSessionCilentMap
						.get(kan);
				if (sessionClient != null) {
					SdkCommonSuccessRespObject resp = new SdkCommonSuccessRespObject(
							JPushTcpClientHandler.VERSION, "1000001", JMessage.Method.DISCONNECT, "");
					sessionClient
							.sendEvent("onDisconnected", gson.toJson(resp));
				}
			}
		}
	}

	@Override
	public void channelActive(final ChannelHandlerContext ctx) throws Exception {
		log.info(String.format("handler: %s active", ctx.channel().toString()));
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		log.info(String.format("handler: %s recv data from push server", ctx
				.channel().toString()));
		if (msg instanceof PushRegResponseBean) {
			PushRegResponseBean bean = (PushRegResponseBean) msg;
			log.info(String.format(
					"client handler resolve jpush reg response data: %s",
					gson.toJson(bean)));
			long juid = bean.getUid();
			String password = bean.getPasswd();
			log.info(String
					.format("client handler add jpush reg resp juid: %d and password: %s to pool",
							juid, password));
			UidResourcesPool.addUidToPool(juid, password);
			UidResourcesPool.capacityCountDown.countDown();
			if (UidResourcesPool.capacityCountDown.getCount() == 0) {
				UidResourcesPool.produceResourceSemaphore.release();
				ctx.channel().close();
			}
			log.info("client handler add jpush reg resp data to pool success.");
		}
		if (msg instanceof PushLoginResponseBean) {
			pushLoginResponseBean = (PushLoginResponseBean) msg;
			V1.pushLoginInCountDown.countDown();
			log.info(String.format(
					"client handler resolve jpush login response data: %s",
					gson.toJson(pushLoginResponseBean)));
		}
		if (msg instanceof PushLogoutResponseBean) {
			PushLogoutResponseBean bean = (PushLogoutResponseBean) msg;
			log.info(String.format(
					"client handler resolve jpush logout response data: %s",
					gson.toJson(bean)));
		}
		if (msg instanceof IMPacket) {
			SocketIOClient sessionClient = null;
			IMPacket imPacket = (IMPacket) msg;
			long rid = imPacket.getRid();
			Packet protocol = imPacket.getPacket();
			int command = protocol.getHead().getCmd();
			String appKey = protocol.getHead().getAppkey().toStringUtf8();
			log.info(String.format(
					"client handler resolve IM module, the IM Command is: %d",
					command));
			switch (command) {
			case Command.JPUSH_IM.LOGIN:
				log.info(String.format(
						"client handler resolve IM login resp data: %s",
						protocol.toString()));
				Login loginBean = ProtocolUtil.getLogin(protocol);
				String userName = loginBean.getUsername().toStringUtf8();
				Response loginResp = ProtocolUtil.getCommonResp(protocol);
				int imLoginRespCode = loginResp.getCode();
				String imLoginRespMsg = loginResp.getMessage().toStringUtf8();
				log.info(String
						.format("client handler resolve IM login resp code: %d, message: %s",
								imLoginRespCode, imLoginRespMsg));

				if (imLoginRespCode == TcpCode.IM.SUCCESS) {
					long uid = protocol.getHead().getUid();
					String password = loginBean.getPassword().toStringUtf8();
					String stoken = (APIProxy.getToken(String.valueOf(uid),
							password)).content;
					Map tokenMap = gson.fromJson(stoken, HashMap.class);
					String _token = (String) tokenMap.get("token");
					String token = BASE64Utils.encodeString(uid + ":" + _token);

					// 存储用户信息
					Jedis jedis = null;
					try {
						jedis = redisClient.getJeids();
						Map<String, String> map = new HashMap<String, String>();
						map.put("uid", String.valueOf(uid));
						map.put("token", token);
						jedis.hmset(appKey + ":" + userName, map);
					} catch (JedisConnectionException e) {
						log.error(e.getMessage());
						redisClient.returnBrokenResource(jedis);
						throw new JedisConnectionException(e);
					} finally {
						redisClient.returnResource(jedis);
					}
					SdkCommonSuccessRespObject loginComResp = new SdkCommonSuccessRespObject(
							JPushTcpClientHandler.VERSION, String.valueOf(rid), JMessage.Method.LOGIN, "");
					WebImServer.userNameToSessionCilentMap.get(
							appKey + ":" + userName).sendEvent(JPushTcpClientHandler.DATA_AISLE,
							gson.toJson(loginComResp));
					log.info(String.format(
							"client handler send event: %s to webclient",
							"login"));
				} else {
					SdkCommonErrorRespObject loginComResp = new SdkCommonErrorRespObject(
							JPushTcpClientHandler.VERSION, String.valueOf(rid), JMessage.Method.LOGIN);
					loginComResp.setErrorInfo(imLoginRespCode, imLoginRespMsg);
					WebImServer.userNameToSessionCilentMap.get(
							appKey + ":" + userName).sendEvent(JPushTcpClientHandler.DATA_AISLE,
							gson.toJson(loginComResp));
					log.warn(String
							.format("client handler send event: %s, exception code: %d",
									"loginfail", loginComResp.getError()
											.getCode()));
				}

				break;
			case Command.JPUSH_IM.LOGOUT:
				log.info(String.format(
						"client handler resolve IM logout resp data: %s",
						protocol.toString()));
				// Logout logoutBean = ProtocolUtil.getLogout(protocol);
				Response logoutResp = ProtocolUtil.getCommonResp(protocol);
				int imLogoutCode = logoutResp.getCode();
				String imLogoutMessage = logoutResp.getMessage().toStringUtf8();
				log.info("logout response data: code: " + imLogoutCode + ", message: " + imLogoutMessage);
				String lokan = WebImServer.pushChannelToUsernameMap.get(ctx.channel());
				sessionClient = WebImServer.userNameToSessionCilentMap.get(lokan);
				String lkan = WebImServer.sessionClientToUserNameMap.get(sessionClient);
				WebImServer.sessionClientToUserNameMap.remove(sessionClient);
				WebImServer.userNameToSessionCilentMap.remove(lkan);
				WebImServer.userNameToPushChannelMap.remove(lkan);
				WebImServer.pushChannelToUsernameMap.remove(ctx.channel());
				if(logoutResp.getCode()==TcpCode.IM.SUCCESS){
					log.info("user logout success");
					SdkCommonSuccessRespObject logoutComResp = new SdkCommonSuccessRespObject(
							JPushTcpClientHandler.VERSION, String.valueOf(rid), JMessage.Method.LOGOUT, "");
					sessionClient.sendEvent(JPushTcpClientHandler.DATA_AISLE, gson.toJson(logoutComResp));
				} else {
					SdkCommonErrorRespObject logoutComResp = new SdkCommonErrorRespObject(
							JPushTcpClientHandler.VERSION, String.valueOf(rid), JMessage.Method.LOGOUT);
					logoutComResp.setErrorInfo(imLogoutCode, imLogoutMessage);
					sessionClient.sendEvent(JPushTcpClientHandler.DATA_AISLE, gson.toJson(logoutComResp));
				}
				break;
			case Command.JPUSH_IM.SENDMSG_SINGAL:
				log.info(String.format(
						"client handler resolve IM single msg resp data: %s",
						protocol.toString()));
				SingleMsg singleMsgBean = ProtocolUtil.getSingleMsg(protocol);
				Response singleMsgResp = ProtocolUtil.getCommonResp(protocol);
				int imSingleMsgRespCode = singleMsgResp.getCode();
				String imSingleMsgRespMsg = singleMsgResp.getMessage()
						.toStringUtf8();
				log.info(String
						.format("client handler resolve IM single msg resp code: %d, message: %s",
								imSingleMsgRespCode, imSingleMsgRespMsg));
				// 消息发送状态下发
				@SuppressWarnings("unchecked")
				HashMap<String, Object> _dataMap = gson.fromJson(singleMsgBean
						.getContent().getContent().toStringUtf8(),
						HashMap.class);
				String kan = WebImServer.pushChannelToUsernameMap.get(ctx.channel());
				sessionClient = WebImServer.userNameToSessionCilentMap.get(kan);

				if (sessionClient != null) {
					if (imSingleMsgRespCode == TcpCode.IM.SUCCESS) {
						log.info("send single msg success");
						SdkCommonSuccessRespObject resp = new SdkCommonSuccessRespObject(
								JPushTcpClientHandler.VERSION, String.valueOf(rid),
								JMessage.Method.MESSAGE_FEEDBACK, "");
						sessionClient.sendEvent(JPushTcpClientHandler.DATA_AISLE, gson.toJson(resp));
					} else {
						log.info("send single msg failture");
						SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(
								JPushTcpClientHandler.VERSION, String.valueOf(rid), JMessage.Method.LOGIN);
						resp.setErrorInfo(imSingleMsgRespCode,
								imSingleMsgRespMsg);
						sessionClient.sendEvent(JPushTcpClientHandler.DATA_AISLE, gson.toJson(resp));
					}
				} else {
					log.warn("the user is not online");
				}

				break;
			case Command.JPUSH_IM.SENDMSG_GROUP:
				log.info(String.format(
						"client handler resolve IM group msg resp data: %s",
						protocol.toString()));
				GroupMsg groupMsgBean = ProtocolUtil.getGroupMsg(protocol);
				Response groupMsgResp = ProtocolUtil.getCommonResp(protocol);
				int imGroupMsgRespCode = groupMsgResp.getCode();
				String imGroupMsgRespMsg = groupMsgResp.getMessage()
						.toStringUtf8();
				log.info(String
						.format("client handler resolve IM group msg resp code: %d, message: %s",
								imGroupMsgRespCode, imGroupMsgRespMsg));
				// 消息发送状态下发
				@SuppressWarnings("unchecked")
				HashMap<String, Object> gdataMap = gson.fromJson(groupMsgBean
						.getContent().getContent().toStringUtf8(),
						HashMap.class);
				// long gid = Long.parseLong((String)
				// gdataMap.get("target_id"));
				ChatMessageObject gcontent = gson.fromJson(
						gson.toJson(gdataMap), ChatMessageObject.class);
				// 找群成员
				Channel mchannel = ctx.channel();
				if (mchannel != null) {
					String skan = WebImServer.pushChannelToUsernameMap
							.get(mchannel);
					sessionClient = WebImServer.userNameToSessionCilentMap
							.get(skan);
					if (imGroupMsgRespCode == TcpCode.IM.SUCCESS) {
						SdkCommonSuccessRespObject resp = new SdkCommonSuccessRespObject(
								JPushTcpClientHandler.VERSION, String.valueOf(rid),
								JMessage.Method.MESSAGE_FEEDBACK, "");
						sessionClient.sendEvent(JPushTcpClientHandler.DATA_AISLE, gson.toJson(resp));
					} else {
						SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(
								JPushTcpClientHandler.VERSION, String.valueOf(rid),
								JMessage.Method.MESSAGE_FEEDBACK);
						resp.setErrorInfo(imGroupMsgRespCode, imGroupMsgRespMsg);
						sessionClient.sendEvent(JPushTcpClientHandler.DATA_AISLE, gson.toJson(resp));
					}
				} else {
					log.warn("the user is not online");
				}
				break;
			case Command.JPUSH_IM.CREATE_GROUP:
				log.info(String.format(
						"client handler resolve IM create group resp data: %s",
						protocol.toString()));
				CreateGroup createGroupBean = ProtocolUtil
						.getCreateGroup(protocol);
				Response createGroupResp = ProtocolUtil.getCommonResp(protocol);
				int createGroupRespcode = createGroupResp.getCode();
				String createGroupRespMsg = createGroupResp.getMessage()
						.toStringUtf8();
				log.info(String.format(
						"create group response data: code: %d, message: %s",
						createGroupRespcode, createGroupRespMsg));
				String ckan = WebImServer.pushChannelToUsernameMap.get(ctx
						.channel());
				sessionClient = WebImServer.userNameToSessionCilentMap
						.get(ckan);
				SdkGroupObject groupObject = new SdkGroupObject();
				groupObject.setGid(createGroupBean.getGid());
				groupObject.setFlag(createGroupBean.getFlag());
				groupObject.setGroupDescription(createGroupBean.getGroupDesc()
						.toStringUtf8());
				groupObject.setGroupLevel(createGroupBean.getGroupLevel());
				groupObject.setGroupName(createGroupBean.getGroupName()
						.toStringUtf8());
				if (sessionClient != null) {
					if (createGroupRespcode == TcpCode.IM.SUCCESS) {
						log.info("create group success");
						SdkCommonSuccessRespObject resp = new SdkCommonSuccessRespObject(
								JPushTcpClientHandler.VERSION, String.valueOf(rid), JMessage.Method.GROUP_CREATE,
								gson.toJson(groupObject));
						sessionClient.sendEvent(JPushTcpClientHandler.DATA_AISLE, gson.toJson(resp));
					} else {
						log.warn("create group failture");
						SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(
								JPushTcpClientHandler.VERSION, String.valueOf(rid), JMessage.Method.GROUP_CREATE);
						resp.setErrorInfo(createGroupRespcode,
								createGroupRespMsg);
						sessionClient.sendEvent(JPushTcpClientHandler.DATA_AISLE, gson.toJson(resp));
					}
				} else {
					log.warn(String
							.format("Exception -- Can not get connection to: %s client",
									ckan));
				}
				break;
			case Command.JPUSH_IM.EXIT_GROUP:
				log.info(String.format(
						"client handler resolve IM exit group resp data: %s",
						protocol.toString()));
				// ExitGroup exitGroupBean =
				// ProtocolUtil.getExitGroup(protocol);
				Response exitGroupResp = protocol.getBody().getCommonRep();
				int exitGroupRespcode = exitGroupResp.getCode();
				String exitGroupRespMsg = exitGroupResp.getMessage()
						.toStringUtf8();
				log.info(String
						.format("add group member response data: code: %d, message: %s",
								exitGroupRespcode, exitGroupRespMsg));
				String ekan = WebImServer.pushChannelToUsernameMap.get(ctx
						.channel());
				sessionClient = WebImServer.userNameToSessionCilentMap
						.get(ekan);
				log.info("exit group response data: code: "
						+ exitGroupResp.getCode() + ", message: "
						+ exitGroupResp.getMessage().toStringUtf8());
				if (sessionClient != null) {
					if (TcpCode.IM.SUCCESS == exitGroupRespcode) {
						log.info("exit group success");
						SdkCommonSuccessRespObject resp = new SdkCommonSuccessRespObject(
								JPushTcpClientHandler.VERSION, String.valueOf(rid), JMessage.Method.GROUP_EXIT,
								"");
						sessionClient.sendEvent(JPushTcpClientHandler.DATA_AISLE, gson.toJson(resp));
					} else {
						log.warn("exit group failture");
						SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(
								JPushTcpClientHandler.VERSION, String.valueOf(rid), JMessage.Method.GROUP_EXIT);
						resp.setErrorInfo(exitGroupRespcode, exitGroupRespMsg);
						sessionClient.sendEvent(JPushTcpClientHandler.DATA_AISLE, gson.toJson(resp));
					}
				} else {
					log.warn(String
							.format("Exception -- Can not get connection to: %s client",
									ekan));
				}
				break;
			case Command.JPUSH_IM.ADD_GROUP_MEMBER:
				log.info(String
						.format("client handler resolve IM add group member resp data: %s",
								protocol.toString()));
				// AddGroupMember addGroupMemberBean =
				// ProtocolUtil.getAddGroupMember(protocol);
				Response addGroupMemberResp = ProtocolUtil
						.getCommonResp(protocol);
				int addGroupMemRespcode = addGroupMemberResp.getCode();
				String addGroupMemRespMsg = addGroupMemberResp.getMessage()
						.toStringUtf8();
				log.info(String
						.format("add group member response data: code: %d, message: %s",
								addGroupMemRespcode, addGroupMemRespMsg));
				String akan = WebImServer.pushChannelToUsernameMap.get(ctx
						.channel());
				sessionClient = WebImServer.userNameToSessionCilentMap
						.get(akan);
				if (null != sessionClient) {
					if (addGroupMemRespcode == TcpCode.IM.SUCCESS) {
						SdkCommonSuccessRespObject resp = new SdkCommonSuccessRespObject(
								JPushTcpClientHandler.VERSION, String.valueOf(rid),
								JMessage.Method.GROUPMEMBERS_ADD, "");
						sessionClient.sendEvent(JPushTcpClientHandler.DATA_AISLE, gson.toJson(resp));
					} else {
						SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(
								JPushTcpClientHandler.VERSION, String.valueOf(rid),
								JMessage.Method.GROUPMEMBERS_ADD);
						resp.setErrorInfo(addGroupMemRespcode,
								addGroupMemRespMsg);
						sessionClient.sendEvent(JPushTcpClientHandler.DATA_AISLE, gson.toJson(resp));
					}
				} else {
					log.warn(String
							.format("Exception -- Can not get connection to: %s client",
									akan));
				}
				break;
			case Command.JPUSH_IM.DEL_GROUP_MEMBER:
				log.info(String.format(
						"client handler resolve IM del group member data: %s",
						protocol.toString()));
				// DelGroupMember delGroupMemberBean =
				// ProtocolUtil.getDelGroupMember(protocol);
				// log.info("del group member data, gid: "+delGroupMemberBean.getGid());
				Response delGroupMemberResp = ProtocolUtil
						.getCommonResp(protocol);
				int delGroupMemRespcode = delGroupMemberResp.getCode();
				String delGroupMemRespMsg = delGroupMemberResp.getMessage()
						.toStringUtf8();
				log.info(String
						.format("del group member response data: code: %d, message: %s",
								delGroupMemRespcode, delGroupMemRespMsg));
				String rkan = WebImServer.pushChannelToUsernameMap.get(ctx
						.channel());
				sessionClient = WebImServer.userNameToSessionCilentMap
						.get(rkan);
				if (null != sessionClient) {
					if (delGroupMemRespcode == TcpCode.IM.SUCCESS) {
						SdkCommonSuccessRespObject resp = new SdkCommonSuccessRespObject(
								JPushTcpClientHandler.VERSION, String.valueOf(rid),
								JMessage.Method.GROUPMEMBERS_REMOVE, "");
						sessionClient.sendEvent(JPushTcpClientHandler.DATA_AISLE, gson.toJson(resp));
					} else {
						SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(
								JPushTcpClientHandler.VERSION, String.valueOf(rid),
								JMessage.Method.GROUPMEMBERS_REMOVE);
						resp.setErrorInfo(delGroupMemRespcode,
								delGroupMemRespMsg);
						sessionClient.sendEvent(JPushTcpClientHandler.DATA_AISLE, gson.toJson(resp));
					}
				} else {
					log.warn(String
							.format("Exception -- Can not get connection to: %s client",
									rkan));
				}
				break;
			case Command.JPUSH_IM.UPDATE_GROUP_INFO:
				log.info(String.format(
						"client handler resolve IM update group info data: %s",
						protocol.toString()));
				UpdateGroupInfo bean = ProtocolUtil
						.getUpdateGroupInfo(protocol);
				// log.info("update group info data, gid: "+bean.getGid()+", info: "+bean.getInfo());
				Response updateGroupInfoResp = ProtocolUtil
						.getCommonResp(protocol);
				int updateGroupInfoRespcode = updateGroupInfoResp.getCode();
				String updateGroupInfoRespMsg = updateGroupInfoResp
						.getMessage().toStringUtf8();
				log.info(String
						.format("update group info response data: code: %d, message: %s",
								updateGroupInfoRespcode, updateGroupInfoRespMsg));
				String ukan = WebImServer.pushChannelToUsernameMap.get(ctx
						.channel());
				sessionClient = WebImServer.userNameToSessionCilentMap
						.get(ukan);
				if (updateGroupInfoRespcode == TcpCode.IM.SUCCESS) {
					log.info("send update group info success event to webclient");
					SdkGroupObject updateGroupObject = new SdkGroupObject();
					updateGroupObject.setGid(bean.getGid());
					updateGroupObject.setGroupDescription(bean.getInfo()
							.toStringUtf8());
					updateGroupObject.setGroupName(bean.getName()
							.toStringUtf8());
					if (null != sessionClient) {
						SdkCommonSuccessRespObject resp = new SdkCommonSuccessRespObject(
								JPushTcpClientHandler.VERSION, String.valueOf(rid),
								JMessage.Method.GROUPINFO_UPDATE,
								gson.toJson(updateGroupObject));
						sessionClient.sendEvent(JPushTcpClientHandler.DATA_AISLE, gson.toJson(resp));
					} else {
						log.warn(String.format("user: %s is not online", ukan));
					}
				} else {
					SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(
							JPushTcpClientHandler.VERSION, String.valueOf(rid), JMessage.Method.GROUPINFO_UPDATE);
					resp.setErrorInfo(updateGroupInfoRespcode,
							updateGroupInfoRespMsg);
					sessionClient.sendEvent(JPushTcpClientHandler.DATA_AISLE, gson.toJson(resp));
					log.warn(String
							.format("modify group info failture, code: %s, message: %s",
									updateGroupInfoRespcode,
									updateGroupInfoRespMsg));
				}
				break;

			case Command.JPUSH_IM.SYNC_EVENT:
				log.info(String.format(
						"client handler resolve IM event sync data: %s",
						protocol.toString()));
				EventNotification eventNotification = ProtocolUtil
						.getEventNotification(protocol);
				int sync_eventType = eventNotification.getEventType();
				long sync_uid = eventNotification.getFromUid();
				long sync_gid = eventNotification.getGid();
				String skan = WebImServer.pushChannelToUsernameMap.get(ctx
						.channel());
				if (StringUtils.isNotEmpty(skan)) {
					sessionClient = WebImServer.userNameToSessionCilentMap
							.get(skan);
				} else {
					log.warn("user has logout");
					return;
				}
				SdkSyncEventObject syncEventObject = new SdkSyncEventObject();
				syncEventObject.setFrom_uid(eventNotification.getFromUid());
				syncEventObject.setEvent_id(eventNotification.getEventId());
				syncEventObject.setI_event_type(sync_eventType);
				if (sync_eventType == 8) {
					syncEventObject.setEvent_type("create_group");
				} else if (sync_eventType == 9) {
					syncEventObject.setEvent_type("exit_group");
				} else if (sync_eventType == 10) {
					syncEventObject.setEvent_type("add_members");
				} else if (sync_eventType == 11) {
					syncEventObject.setEvent_type("remove_members");
				}
				String keyAndname = WebImServer.pushChannelToUsernameMap
						.get(ctx.channel());
				if (StringUtils.isEmpty(keyAndname)) {
					log.warn("user have logout");
					return;
				} else {
					appKey = StringUtils.getAppKey(keyAndname);
					userName = StringUtils.getUserName(keyAndname);
					if (StringUtils.isEmpty(appKey)
							|| StringUtils.isEmpty(userName)) {
						log.warn("resovle username exception");
						return;
					}
				}
				Jedis jedis = null;
				String token = "";
				try {
					jedis = redisClient.getJeids();
					List<String> dataList = jedis.hmget(
							appKey + ":" + userName, "appKey", "token");
					appKey = dataList.get(0);
					token = dataList.get(1);
				} catch (JedisConnectionException e) {
					log.error(e.getMessage());
					redisClient.returnBrokenResource(jedis);
					throw new JedisConnectionException(e);
				} finally {
					redisClient.returnResource(jedis);
				}
				String uid = String.valueOf(sync_uid);
				HttpResponseWrapper responseWrapper = APIProxy
						.getUserInfoByUid(appKey, uid, token);
				if (responseWrapper.isOK()) {
					SdkUserInfoObject userInfo = gson.fromJson(
							responseWrapper.content, SdkUserInfoObject.class);
					syncEventObject.setFrom_username(userInfo.getUsername());
				}
				syncEventObject.setGid(sync_gid);
				List<Long> uidList = eventNotification.getToUidlistList();
				ArrayList<String> usernameList = new ArrayList<String>();
				for (long userId : uidList) {
					HttpResponseWrapper wrapper = APIProxy.getUserInfoByUid(
							appKey, String.valueOf(userId), token);
					if (wrapper.isOK()) {
						SdkUserInfoObject userInfo = gson.fromJson(
								responseWrapper.content,
								SdkUserInfoObject.class);
						usernameList.add(userInfo.getUsername());
					}
				}
				syncEventObject.setTo_username_list(usernameList);
				syncEventObject.setDescription(eventNotification
						.getDescription().toStringUtf8());
				if (sessionClient != null) {
					SdkCommonSuccessRespObject resp = new SdkCommonSuccessRespObject(
							JPushTcpClientHandler.VERSION, String.valueOf(rid), JMessage.Method.EVENT_RECEIVE,
							gson.toJson(syncEventObject));
					sessionClient.sendEvent(JPushTcpClientHandler.DATA_AISLE, gson.toJson(resp));
				} else {
					log.warn("the user is not online");
				}
				break;

			case Command.JPUSH_IM.SYNC_MSG:
				log.info(String.format(
						"client handler resolve IM msg sync data: %s",
						protocol.toString()));
				ChatMsgSync chatMsgSync = ProtocolUtil.getChatMsgSync(protocol);
				int msgCount = chatMsgSync.getChatMsgCount();
				List<ChatMsg> chatMsgList = chatMsgSync.getChatMsgList();
				for (int i = 0; i < msgCount; i++) {
					ChatMsg chatMsg = chatMsgList.get(i);
					@SuppressWarnings("unchecked")
					HashMap<String, Object> dataMap = gson.fromJson(chatMsg
							.getContent().getContent().toStringUtf8(),
							HashMap.class);
					String target_type = (String) dataMap.get("target_type");
					if ("single".equals(target_type)) {
						Channel _channel = ctx.channel();
						String _mkan = "";
						if (_channel != null) {
							_mkan = WebImServer.pushChannelToUsernameMap
									.get(_channel);
							sessionClient = WebImServer.userNameToSessionCilentMap
									.get(_mkan);
						}
						MsgContentBean content = gson.fromJson(
								gson.toJson(dataMap), MsgContentBean.class);

						if (sessionClient != null) {
							SdkSyncMsgObject syncMsgObject = new SdkSyncMsgObject();
							syncMsgObject.setiMsgType(chatMsg.getMsgType());
							syncMsgObject.setMessageId(chatMsg.getMsgid());
							syncMsgObject.setFromUid(chatMsg.getFromUid());
							syncMsgObject.setFromGid(chatMsg.getFromGid());
							syncMsgObject.setVersion(content.getVersion());
							syncMsgObject.setFromType(content.getFrom_type());
							syncMsgObject.setTargetType(content
									.getTarget_type());
							syncMsgObject.setTargetId(content.getTarget_id());
							syncMsgObject.setTargetName(content
									.getTarget_name());
							syncMsgObject.setFromId(content.getFrom_id());
							syncMsgObject.setFromName(content.getFrom_name());
							syncMsgObject.setCreateTime(content
									.getCreate_time());
							syncMsgObject.setMsgBody(content.getMsg_body()
									.toString());
							if ("text".equals(content.getMsg_type())) {
								syncMsgObject.setMsgType("text");
							} else if ("image".equals(content.getMsg_type())) {
								syncMsgObject.setMsgType("image");
							} else if ("voice".equals(content.getMsg_type())) {
								syncMsgObject.setMsgType("voice");
							}
							SdkCommonSuccessRespObject resp = new SdkCommonSuccessRespObject(
									JPushTcpClientHandler.VERSION, String.valueOf(rid),
									JMessage.Method.MESSAGE_RECEIVE,
									gson.toJson(syncMsgObject));
							sessionClient.sendEvent(JPushTcpClientHandler.DATA_AISLE, gson.toJson(resp));
							log.info(String.format("single msg data: %s",
									chatMsg.getContent().getContent()
											.toStringUtf8()));
							log.info(String.format(
									"send ChatEvent Single Msg to Client: %s",
									gson.toJson(resp)));
						} else {
							log.warn("the user is not online");
						}
					} else if ("group".equals(target_type)) {
						MsgContentBean content = gson.fromJson(
								gson.toJson(dataMap), MsgContentBean.class);
						// 找群成员
						Channel _channel = ctx.channel();
						if (_channel != null) {
							String _mkan = WebImServer.pushChannelToUsernameMap
									.get(_channel);
							sessionClient = WebImServer.userNameToSessionCilentMap
									.get(_mkan);
							SdkSyncMsgObject syncMsgObject = new SdkSyncMsgObject();
							syncMsgObject.setiMsgType(chatMsg.getMsgType());
							syncMsgObject.setMessageId(chatMsg.getMsgid());
							syncMsgObject.setFromUid(chatMsg.getFromUid());
							syncMsgObject.setFromGid(chatMsg.getFromGid());
							syncMsgObject.setVersion(content.getVersion());
							syncMsgObject.setFromType(content.getFrom_type());
							syncMsgObject.setTargetType(content
									.getTarget_type());
							syncMsgObject.setTargetId(content.getTarget_id());
							syncMsgObject.setTargetName(content
									.getTarget_name());
							syncMsgObject.setFromId(content.getFrom_id());
							syncMsgObject.setFromName(content.getFrom_name());
							syncMsgObject.setCreateTime(content
									.getCreate_time());
							syncMsgObject.setMsgBody(content.getMsg_body()
									.toString());
							if ("text".endsWith(content.getMsg_type())) {
								syncMsgObject.setMsgType("text");
							} else if ("image".endsWith(content.getMsg_type())) {
								syncMsgObject.setMsgType("image");
							} else if ("voice".equals(content.getMsg_type())) {
								syncMsgObject.setMsgType("voice");
							}
							log.info("return group msg string: "
									+ gson.toJson(syncMsgObject));
							if (sessionClient != null) {
								SdkCommonSuccessRespObject resp = new SdkCommonSuccessRespObject(
										JPushTcpClientHandler.VERSION, String.valueOf(rid),
										JMessage.Method.MESSAGE_RECEIVE,
										gson.toJson(syncMsgObject));
								sessionClient.sendEvent(JPushTcpClientHandler.DATA_AISLE,
										gson.toJson(resp));
								log.info(String
										.format("send ChatEvent Group Msg to Client: %s",
												gson.toJson(resp)));
							} else {
								log.warn(String
										.format("user: %s get connection to webclient is empty",
												_mkan));
							}
						}
					}
				}
				break;

			default:
				log.warn("未定义的 IM 业务消息类型.");
				break;
			}

		}
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
			throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent e = (IdleStateEvent) evt;
			/*
			 * if(e.state()==IdleState.READER_IDLE){
			 * log.info("client heartbeat...client read idle...channel:"
			 * +ctx.channel().toString()+", count:"+count++); }
			 */
			if (e.state() == IdleState.WRITER_IDLE) {
				log.info("client heartbeat...write idle:"
						+ ctx.channel().toString());
				// 心跳请求
				HeartBeatRequest request = new HeartBeatRequest(2, 1,
						this.getSid(), this.getJuid());
				ctx.channel().writeAndFlush(request);
			}
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		cause.printStackTrace();
		ctx.close();
	}

	public PushLoginResponseBean getPushLoginResponseBean() {
		return pushLoginResponseBean;
	}

	public void setPushLoginResponseBean(
			PushLoginResponseBean pushLoginResponseBean) {
		this.pushLoginResponseBean = pushLoginResponseBean;
	}

	public int getSid() {
		return sid;
	}

	public void setSid(int sid) {
		this.sid = sid;
	}

	public long getJuid() {
		return juid;
	}

	public void setJuid(long juid) {
		this.juid = juid;
	}

}
