package cn.jpush.protocal.utils;

import java.util.HashMap;
import java.util.Map;

public class Configure {

	/** false：开发状态，true：生产状态 */
	public static final boolean CURRENT_STATUS = true;

	/**
	 * 1: richpush 编辑器中运行上传文件格式 2: 目前只针对Img做处理
	 * 
	 **/
	public static Map<String, String> UPLOAD_FILE_TYPE = new HashMap<String, String>();
	public static int UPLOAD_IMAGE_MAX_SIZE = 1024 * 200;

	static {
		UPLOAD_FILE_TYPE.put("image", "gif,jpg,jpeg,png,bmp");
		UPLOAD_FILE_TYPE.put("flash", "swf,flv");
		UPLOAD_FILE_TYPE.put("media", "swf,flv,mp3,wav,wma,wmv,mid,avi,mpg,asf,rm,rmvb");
		UPLOAD_FILE_TYPE.put("file", "doc,docx,xls,xlsx,ppt,htm,html,txt,zip,rar,gz,bz2");

	}

	public interface SEND_ERROR {
		final int SEND_SUCCESS = 0;
		final int SEND_DEFAULT = -1;
		final int SCHEDULE_CANCELLED = 9;
		final int API_RESPONSE_EMPTY = -10;

		final int SCHEDULE_TIME_FORMAT_ERROR = 8001;
		final int SEND_PLATFORM_ERROR = 8002;

		final int SEND_READ_TIME_OUT = -12; // Read Time Out

		final int SEND_ERROR_RICH_RESOURCE_OVERLENGTH = 8003; // 富媒体资源超长错误
		final int SEND_ERROR_RICH_RESOURCE_TYPE = 8004; // 富媒体资源类型错误

	}

	public interface Push {
		int NOTIFICATION = 1;
		int MESSAGE = 0;

		String DELIVERY_NOW = "now";
		String DELIVERY_SCHEDULED = "scheduled";
		String DELIVERY_DURATION = "duration";

		String BROADCAST = "broadcast";
		String TAGS = "tags";
		String ALIAS = "alias";
		String IMEI = "imei";
		String REGISTRATIONID = "registrationid";
		String SEGMENTID = "segmentid";

		String SAME_MSG_FLAG = "yes";
	}

	public interface Redis {
		static final String PUSH_LIST = "pushlist";
		static final String TAG_ALIAS = "tagalias";
		static final String SEGMENT = "segment";
		static final String PUSH_CONFIG = "push_config";

	}

	public interface TagsRedis {
		String KEY_PREFIX = "taglist-";
	}

	public interface Sdk {
		// release_type
		String RELEASE_AUTOMATIC = "automatic";
		String RELEASE_MANUAL = "manual";
		// sdk_type
		String SDK_IOS_OPEN_UDID = "ios-OPENUDID";
		String SDK_IOS_UDID = "ios-UDID";
		String SDK_ANDROID = "android";
		String SDK_ANDROID_x86 = "android_x86"; // android_x86_sdk

		String SDK_ANDROID_MIPS = "android_mips";

		String SDK_WINPHONE = "winphone";

	}

	public interface Stage {
		int DEVELOPMENT = 0;// 开发阶段
		String DEVELOPMENT_TAG = "Development";

		int PRODUCTS = 1;// 生产阶段
		String PRODUCTS_TAG = "Production";

		// iOS证书相关的字典项
		int VERITY_CODE_INIT = 0;
		int VERITY_CODE_ING = 1;
		int VERITY_CODE_ERROR = -1;
		int VERITY_CODE_OK = 2;
		String KEY_TYPE = "PKCS12";
		String SUBJECT_KEY = "CN";
		String SUBJECT_UID_KEY = "UID";
		String TEST_TOKEN = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
	}

	// 页面显示数据大小
	public interface PageSize {
		int PUSH_SENT = 12;// 推送历史记录
	}

	// 推送发送来源
	public interface SendSource {
		int API = 1;// 第三方发送

		int PORTAL = 0;// PORTAL发送
	}

	// 消息回调地址 Key
	public interface API {
		String SEND_MESSAGE = "send_msg";
	}

	// 文件夹路径
	public interface Dirs {
		String APPLE_CERTIFICATE = "/apple";

		String APP_ICON = "/appfile";

		String SDK_ANDROID = "/android";
		String SDK_IOS = "/ios";
	}

	// md5扰码
	public interface Scramblers {
		String API_MASTER_SECRET = "rdv58";

		String APP_KEY = "34FG10";
	}

	// 文件名后缀
	public interface FileExtentions {
		String P12 = ".p12";

		String IMAGES = ".gif|.png|.jpg|.jpeg";

		String zip = ".zip";

	}

	public interface ExecShell {
		String INTEGRATION_SDK_IP = "INTEGRATION_SDK_IP"; // CURRENT_STATUS ?
															// "192.168.252.173":"192.168.252.158";

		String INTEGRATION_USER_NAME = "jpush";// CURRENT_STATUS ? "jpush":
												// "push";

		String INTEGRATION_PASSWORD = "jpush#@!"; // CURRENT_STATUS ?
													// "jpush#@!": "Push#@!";

		String INTEGRATION_DOWNLOAD_URL = "INTEGRATION_DOWNLOAD_URL"; // SDK下载地址

		String INTEGRATION_SDK_NAME = "INTEGRATION_SDK_NAME";
	}

	public interface RichPush {
		int MAX_COUNT = 3;
		// 富媒体资源类型 RICH_MEDIA_TYPE_NETWORK:网络资源,RICH_MEDIA_TYPE_LOCAL:本地资源
		String RICH_MEDIA_TYPE_NETWORK = "rich_media_net";

		String RICH_MEDIA_TYPE_LOCAL = "rich_media_local";

		int NOTIFICATION = 1;

		int MESSAGE = 0;

		int RICH_PUSH = 2;

		int RICH_MEDIA_PUSH = 3;

		int RICH_MEDIA_MAX_SIZE = 1024 * 200;

		String UD_DOMAIN = "fastdfs.jpush.cn";

		interface Template {
			int HTML_SIZE = 1024 * 20;; // html大小

			int IMAGE_SIZE = 1024 * 200; // 所有图片大小

			String fILTER_TAG = "LINK,SCRIPT,VIDEO,OBJECT,AUDIO,EMBED,IFRAME"; // html页面不允许内容标签

			String SAVE_TYPE_INSERT = "2"; // 新增

			String SAVE_TYPE_UPDATE = "1"; // 修改

		}

		public interface STATE_CODE {
			int SYNC_TIMEOUT_DEFAULT = 15;
			int SYNC_TIMEOUT_ERROR = -1;
			int SYNC_SUCCESS = 0;
		}
	}

	public interface ScheduledJob {
		String PUSH_GROUP = "PUSH_GROUP";
		String APNS_VERIFY_GROUP = "APNS_VERIFY_GROUP";
	}

	public interface MsgActiviteUser {
		// %s -> msg_id
		String ANDROID = "m-a-u-%s-a";
		String IOS = "m-a-u-%s-i";
		String WINPHONE = "m-a-u-%s-w";
	}

	public interface MsgTotleUser {
		// %s -> msg_id
		String ANDROID = "m-t-u-%s-a";
		String IOS = "m-t-u-%s-i";
		String IOS_TOTAL = "msg-total-i-%s";// apns目标
		String IOS_FAILD = "msg-failed-i-%s";
		String WINPHONE = "msg-total-w-%s";
	}

	public interface MsgReceived {
		// %s -> msg_id
		// String ANDROID = "m-a-r-%s"; // 安卓送达
		String ANDROID = "msg-recv-a-%d"; // 安卓送达
		String IOS = "msg-success-i-%s"; // apns推送成功
		String MESSAGE_IOS = "msg-recived-i-%d";
		String WINPHONE = "msg-succ-w-%s";// winphone推送成功

	}

	public interface MsgClickedCnt {
		// %s -> msg_id
		String ANDROID = "msg-clicked-a-%s";
		String MESSAGE_ANDROID = "custom-clicked-a-%s";
		String IOS = "msg-clicked-i-%s";
		String WINPHONE = "msg-clicked-w-%s";
	}

	public interface MsgOnlineCnt {
		String ANDROID = "m-s-c-%s-a";
	}

	/**
	 * 应用管理接口
	 * 
	 * @author zengzhiwu
	 * 
	 */
	public interface AppManager {
		String APP_MANAGER_URL_CU = "APP_MANAGER_URL_CU";

		String APP_MANAGER_URL_CHECK = "APP_MANAGER_URL_CHECK";

		String APP_MANAGER_URL_DELETE = "APP_MANAGER_URL_DELETE";
	}

	/**
	 * 应用分组接口
	 * 
	 * @author DuYang
	 * 
	 */
	public interface GroupManager {
		String URL_CREATE_AND_UPDATE = "GROUP_MANAGER_URL_CREATE_AND_UPDATE";
		String URL_DELETE = "GROUP_MANAGER_URL_DELETE";
		String URL_ADD_APP = "GROUP_MANAGER_URL_ADD_APP";
		String URL_RM_APP = "GROUP_MANAGER_URL_RM_APP";
		String URL_LIST_GROUP = "GROUP_MANAGER_URL_LIST_GROUP";
		String URL_GET_GROUP_INFO = "GROUP_MANAGER_URL_GET_GROUP_INFO";
	}

	/*
	 * qiuiu cloud server
	 */

	public interface QNCloudInterface {

		String QN_ACCESS_KEY = SystemConfig.getProperty("qiniu.access_key");

		String QN_SECRET_KEY = SystemConfig.getProperty("qiniu.secret_key");

		String QN_BUCKETNAME = SystemConfig.getProperty("qiniu.bucketname");

		String QN_DOMAIN = SystemConfig.getProperty("qiniu.domain");

		String QN_DN_DOMAIN = SystemConfig.getProperty("qiniu.dn.domain");

	}

	/*
	 * 告警中心，code定义
	 */
	public interface AlertCodeInterface {
		int APP_MANAGER_CODE = 14;

		int SEND_PUSH_API = 15;

		int PUSH_LIST_REDIS = 1;

		int QINIU_SERVER = 16;

		int SEND_PUSH_WEBSTATS = 47;

		int SEND_PUSH_DAO = 53;
	}

	public interface HttpResponseCoce {
		int RESP_EMPTY = -1;
		int RESP_ERROR = 0;
		int RESP_SUCCESS = 1;
	}

	public interface Status {
		int STATUS_OK = 1;
		int STATUS_NO = 0;
	}

	public static final String API_URL_SENDMSG = "API_URL_SENDMSG";

	public static final String DIR_APP_UPLOADS = "DIR_APP_UPLOADS";

	public static final String BUILD_EXAMPLE_ANDROID_PACKAGE = "BUILD_EXAMPLE_ANDROID_PACKAGE";

	public static final String VERIFY_P12_URL = "VERIFY_P12_URL";

	public static final String API_CALLBACK_URL = "API_CALLBACK_URL";

	public static final String ANDROID_SDK_PATH = "ANDROID_SDK_PATH";

	public static final String ANDROID_SDK_FTL_LIST = "ANDROID_SDK_FTL_LIST";

	public static final String SDK_DIR = "SDK_DIR";

	public static final String IOS_SDK_UDID_PATH = "IOS_SDK_UDID_PATH";

	public static final String IOS_SDK_OPENUDID_PATH = "IOS_SDK_OPENUDID_PATH";

	public static final long MAX_TIME_TO_LIVE = 864000L;

	public static final long DEFAULT_TIME_TO_LIVE = 86400L;

	/** 开发者SDK集成 版本名称 **/
	public static final String DEVELOPER_SDK_VERSION = "DEVELOPER_SDK_VERSION";

	public static final String HTML_TO_IMAGE_PATH = "HTML_TO_IMAGE_PATH";

	/** richpush send url **/
	public static final String API_RICHPUSH_URL_SEDNMESSAGE = "API_RICHPUSH_URL_SEDNMESSAGE";

	public static final String PROTOCAL = CURRENT_STATUS ? "https://" : "http://";

	public static final String APP_PLATFORM_RECIVE_COUNT_URL_NEW = "APP_PLATFORM_RECIVE_COUNT_URL_NEW";
	public static final String APP_PLATFORM_RECIVE_COUNT_URL = "APP_PLATFORM_RECIVE_COUNT_URL";

	public static final String FASTDFS_URL = "http://fastdfs.jpush.cn:8080";

	/** USER MANAGER INTERFACE **/

	public interface UserManager {
		public static final String USER_MANAGER_URL_REGISTER = "USER_MANAGER_URL_REGISTER";

		public static final String USER_MANAGER_URL_UPDATE = "USER_MANAGER_URL_UPDATE";

		public static final String USER_MANAGER_URL_CHECK_USER = "USER_MANAGER_URL_CHECK_USER";

		public static final String USER_MANAGER_URL_CHECK_EMAIL = "USER_MANAGER_URL_CHECK_EMAIL";

		public static final String USER_MANAGER_URL_UPDATE_EMAIL = "USER_MANAGER_URL_UPDATE_EMAIL";

		public static final String USER_MANAGER_URL_UPDATE_PASS = "USER_MANAGER_URL_UPDATE_PASS";
	}

	public interface KPI_CODE {
		public static final String DEV_APP_RETENTION = "dev_app_retention";
	}

	public static final String CITYS_CACHE_KEY = "citys_cahce";

	public static final String API_PUSH_SENT = "API_PUSH_SENT";

	public static final String API_PUSH_V3 = "API_PUSH_V3";
	public static final String API_PUSH_V3_URL = "API_PUSH_V3_URL";
	public static final String API_PUSH_V2 = "API_PUSH_V2";
	public static final String API_RICH_PUSH_V2 = "API_RICH_PUSH_V2";

	public static interface WEB_SITE_CONF {
		public static final String FRIEND_LINKS = "FRIEND_LINKS";
		public static final String PROMPT_MESSAGES = "PROMPT_MESSAGES";
		public static final String SUPPORT_QQ_GROUP = "SUPPORT_QQ_GROUP";

	}

	public static interface WEB_CONTEXT_PATH {
		public static final String COMMON = SystemConfig.getProperty("common.context.path");
		public static final String PUSH = SystemConfig.getProperty("push.context.path");

	}
}
