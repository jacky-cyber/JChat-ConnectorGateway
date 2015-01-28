package cn.jpush.protocal.utils;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.EncoderException;
import org.json.JSONException;
import org.slf4j.LoggerFactory;

import com.qiniu.api.auth.AuthException;
import com.qiniu.api.auth.digest.Mac;
import com.qiniu.api.io.IoApi;
import com.qiniu.api.io.PutExtra;
import com.qiniu.api.io.PutRet;
import com.qiniu.api.rs.GetPolicy;
import com.qiniu.api.rs.PutPolicy;
import com.qiniu.api.rs.URLUtils;

import ch.qos.logback.classic.Logger;

public class QNCloudUtils {

	private QNCloudUtils() {
	}
	private static final Logger logger = (Logger) LoggerFactory.getLogger(QNCloudUtils.class);
	private static final int DEFAULT_ATTEMPTS = 3;
	private static String token = null;
	private static Mac mac = null;

	static {
		mac = new Mac(Configure.QNCloudInterface.QN_ACCESS_KEY, Configure.QNCloudInterface.QN_SECRET_KEY);
		PutPolicy putPolicy = new PutPolicy(Configure.QNCloudInterface.QN_BUCKETNAME);
		putPolicy.expires = getDeadline();
		try {
			token = putPolicy.token(mac);
		} catch (AuthException e) {
			logger.error("qiniu get token auth error:", e);
			e.printStackTrace();
		} catch (JSONException e) {
			logger.error("qiniu get token json exception:", e);
			e.printStackTrace();
		}

	}

	/**
	 * 
	 * @author zengzhiwu
	 * @date 2013-12-31
	 * @desc
	 * @param data
	 * @param key
	 * @param attempts
	 *            重试次数
	 * @param delay
	 *            多少秒重试一次
	 * @return
	 */
	public static String RetryOnFailureUploadPubFile(byte[] data, String key, int attempts, int delay) {

		Map<String, Object> uploadResult = uploadFilToMap(data, key, true);

		if (uploadResult.containsKey("error")) {
			if (attempts <= 0)
				attempts = DEFAULT_ATTEMPTS;

			PutRet putRet = (PutRet) uploadResult.get("error");
			key = generateNewKey(putRet, key);
			logger.info(String.format("qn-->upload faile,retry upload.key:%s , attempts :%s,delay:%s", key, attempts, delay));

			for (int i = attempts; i > 0; i--) {
				uploadResult = uploadFilToMap(data, key, true);
				logger.info(String.format("qn-->upload retry.key:%s,count:%s,result:%s", key, i, uploadResult));

				if (uploadResult.containsKey("url")) {
					break;
				} else {
					key = generateNewKey((PutRet) uploadResult.get("error"), key);
				}

				if (delay > 0) {
					try {
						Thread.sleep(delay * 1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}

		return uploadResult.get("url") == null ? null : uploadResult.get("url").toString();

	}

	public static String uploadPubFile(byte[] data) {
		return uploadFile(data, null, true);
	}

	/*
	 * upload public source file
	 */
	public static String uploadPubFile(byte[] data, String key) {
		return RetryOnFailureUploadPubFile(data, key, 3, 0);
		// return uploadFile(data, key, true);
	}

	/*
	 * upload private source file
	 */
	public static String uploadPriFile(byte[] data) {
		return uploadFile(data, null, false);
	}

	/*
	 * upload private source file
	 */
	public static String uploadPriFile(byte[] data, String key) {
		return uploadFile(data, key, false);
	}

	private static String uploadFile(byte[] data, String key, boolean Pub) {
		ByteArrayInputStream bai = new ByteArrayInputStream(data);
		PutRet putRet = IoApi.Put(token, key, bai, new PutExtra());
		if (putRet.ok()) {
			logger.info(String.format("qiniu-->upload file success.reponse:%s", putRet.getResponse()));

			if (Pub) {
				return downloadPubFileUrl(putRet.getKey());
			} else {
				return downloadPriFileUrl(putRet.getKey());
			}
		} else {

			logger.info(String.format("qiniu-->upload file error:res-status:%s, reponse:%s, key:%s", putRet.getStatusCode(), putRet.getResponse(), putRet.getKey()));

			return null;

		}
	}

	private static Map<String, Object> uploadFilToMap(byte[] data, String key, boolean Pub) {
		Map<String, Object> results = new HashMap<String, Object>();
		ByteArrayInputStream bai = new ByteArrayInputStream(data);
		PutRet putRet = IoApi.Put(token, key, bai, new PutExtra());

		if (putRet.ok()) {
			logger.info(String.format("qiniu-->upload file success.reponse:%s", putRet.getResponse()));

			if (Pub) {
				results.put("url", downloadPubFileUrl(putRet.getKey()));

			} else {
				results.put("url", downloadPriFileUrl(putRet.getKey()));
			}
			return results;
		} else {
			logger.info(String.format("qiniu-->upload file error:%s,res-status:%s, reponse:%s, key:%s", putRet.getException(), putRet.getStatusCode(), putRet.getResponse(), key));

			results.put("error", putRet);
			return results;
		}
	}

	/*
	 * download the public source
	 */
	public static String downloadPubFileUrl(String key) {

		return "http://" + Configure.QNCloudInterface.QN_DOMAIN + "/" + key;
	}

	/**
	 * 
	 * @author zengzhiwu
	 * @date 2013-12-30
	 * @desc download the public source,custom file name.
	 * @param key
	 *            下载的文件名称
	 * @param custName
	 *            下载自定义名称
	 * @return
	 */
	public static String downloadPubFileUrl(String key, String custName) {

		String downloadUrl = "http://" + Configure.QNCloudInterface.QN_DOMAIN + "/" + key;
		if (custName != null) {
			downloadUrl += "?download/" + custName;
		}
		return downloadUrl;
	}

	/**
	 * 
	 * @author zengzhiwu
	 * @date 2013-12-30
	 * @desc 支持 https下载
	 * @param key
	 * @return
	 */
	public static String downloadDNPubFileUrl(String key) {
		return "https://" + Configure.QNCloudInterface.QN_DN_DOMAIN + "/" + key;
	}

	/*
	 * download the private source.
	 */
	public static String downloadPriFileUrl(String key) {
		return download(key, null);
	}

	/*
	 * download the private source. Custom download file name.
	 */
	public static String downloadPriFileUrl(String key, String custName) {
		return download(key, custName);
	}

	private static String download(String key, String custName) {
		String downloadUrl = "";
		try {
			String baseUrl = URLUtils.makeBaseUrl(Configure.QNCloudInterface.QN_DOMAIN, key);
			if (custName != null)
				baseUrl += "?download/" + custName;

			GetPolicy getPolicy = new GetPolicy();
			getPolicy.expires = getDeadline();
			downloadUrl = getPolicy.makeRequest(baseUrl, mac);

		} catch (EncoderException e) {
			e.printStackTrace();
		} catch (AuthException e) {
			logger.error("download url auth error:" + e);
			e.printStackTrace();
		}

		return downloadUrl;

	}

	// 获取连接超时时间
	private static int getDeadline() {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.YEAR, 20);
		return (int) (calendar.getTimeInMillis() / 1000);
	}

	private static String generateNewKey(PutRet putRet, String key) {
		String newKey = "";
		String ext = key.substring(key.lastIndexOf(".") + 1);
		// 如果文件名称存在的话，重新生成。
		if (putRet.getStatusCode() == QNErrorEnum.FILE_EXIT.value()) {

			if (key.indexOf("-") >= 0) {
				newKey = key.substring(0, key.lastIndexOf("-")) + QNRandomUtils.getSuffixFile(ext);
			} else {
				newKey = QNRandomUtils.getSuffixFile(ext);
			}
			logger.info(String.format("qn-->key:%s is exit.generate new key:%s", key, newKey));
		} else {
			newKey = key;
		}
		return newKey;

	}

	public static void main(String[] args) throws FileNotFoundException {

		new QNCloudUtils();
		/*
		 * String key = "3013/22/434/kktalk.jpg"; File file = new
		 * File("D:/bb.jpg");
		 * 
		 * InputStream inputStream = new FileInputStream(file); byte[] buff =
		 * new byte[1024]; int byteRead = 0; ByteArrayOutputStream outputStream
		 * = new ByteArrayOutputStream(); try { while((byteRead =
		 * inputStream.read(buff)) != -1){ outputStream.write(buff, 0,
		 * byteRead); } } catch (IOException e) { // TODO Auto-generated catch
		 * block e.printStackTrace(); }
		 * 
		 * String downloadUrl =
		 * QNCloudUtils.RetryOnFailureUploadPubFile(outputStream
		 * .toByteArray(),key,3,2); System.out.println(downloadUrl);
		 */

	}
}
