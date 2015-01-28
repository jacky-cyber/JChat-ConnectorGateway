package cn.jpush.protocal.utils;

public enum QNErrorEnum {


	REQUEST_PARAMS_EROR(400),//	请求参数错误
	
	AUTH_ERROR(401),	//认证授权失败，可能是密钥信息不对或者数字签名错误
	
	REQUEST_EROR(405),	//请求方式错误，非预期的请求方式
	
	CALLBACK_EROR(579),//文件上传成功，但是回调（callback app-server）失败
	
	SYSTEM_ERROR(599),//	服务端操作失败
	
	FILE_UPDATED(608),//	文件内容被修改
	
	FILE_NOT_EXIT(612),//	指定的文件不存在或已经被删除
	
	FILE_EXIT(614),//	文件已存在
	
	BUCKET_FULL(630),//	Bucket 数量已达顶限，创建失败
	
	BUCKET_NO_EXIT(631),//	指定的 Bucket 不存在
	
	DATA_ERROR(701) ;//	上传数据块校验出错
	
	private final int value;
	private QNErrorEnum(final int value) {
		this.value = value;
	}
	
	public int value() {
		return this.value;
	}
}
