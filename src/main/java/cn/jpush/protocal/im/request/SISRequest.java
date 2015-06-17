package cn.jpush.protocal.im.request;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;

import cn.jpush.protocal.utils.Command;
import cn.jpush.protocal.utils.ProtocolUtil;
/**
 * SIS 请求协议包
 * 详细内容参考jpush wiki文档
 */
public class SISRequest {
	private String sdk_ver;
	private String net_type;
	private int tel_opera;
	private String senderid; 
	private int test_mode;
	
	protected byte[] mHeader;
	protected byte[] mBody;
	
	public SISRequest(String sdk_ver, String net_type, int tel_opera, String senderid, int test_mode) {
		this.sdk_ver = sdk_ver;
		this.net_type = net_type;
		this.tel_opera = tel_opera;
		this.senderid = senderid;
		this.test_mode = test_mode;
	}
	
	/**
	 * 返回整个请求包
	 * @return
	 * @throws KKException 
	 */
	public byte[] getRequestPackage(){
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try{
			bos.write(ProtocolUtil.intToByteArray(128, 2));  // pkg length 2B
			bos.write(ProtocolUtil.stringToByteArray("UF", 2));  // "UF" 2B  
			bos.write(ProtocolUtil.stringToByteArray(this.net_type, 30));  // net_type 30B
			bos.write(ProtocolUtil.intToByteArray(this.tel_opera, 4));  // tel_opera  4B
			bos.write(ProtocolUtil.intToByteArray(0, 4));  // uid  4B
			bos.write(ProtocolUtil.stringToByteArray(this.senderid, 50));  // senderid 50B
			bos.write(ProtocolUtil.stringToByteArray(this.sdk_ver, 10));   // sdk_ver 10B
			bos.write(ProtocolUtil.intToByteArray(this.test_mode, 4));  // test_mode  4B
			bos.write(ProtocolUtil.stringToByteArray("", 22));  //  res 22B
		} catch (Exception e) {
			try {
				bos.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		return bos.toByteArray();
	}
	
	public byte[] getDataBytes(String data, int length){
		byte[] result = new byte[length];
		try {
			byte[] dataBytes = data.getBytes("utf-8");
			int dataLength = dataBytes.length;
			for(int i=0; i<dataLength; i++){
				result[length-dataLength+i] = dataBytes[i];
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * 写 TLV2 类型的数据
	 * @param baos
	 * @param string
	 * @throws IOException
	 */
	protected void writeTLV2(ByteArrayOutputStream baos, String string) throws IOException {
		if (string==null || "".equals(string)) {
			baos.write(ProtocolUtil.intToByteArray(0, 2));
		} else {
			byte[] data = string.getBytes(Command.ENCODING_UTF_8);
			baos.write(ProtocolUtil.intToByteArray(data.length, 2));
			baos.write(data);
		}
	}
	
	/**
	 * 写 TLV3 类型的数据
	 * @param baos
	 * @param string
	 * @throws IOException
	 */
	public void writeTLV3(ByteArrayOutputStream baos, String string) throws IOException {
		if (string==null || "".equals(string)) {
			baos.write(ProtocolUtil.intToByteArray(0, 2));
		} else {
			byte[] data = compress(string);
			baos.write(ProtocolUtil.intToByteArray(data.length, 2));
			baos.write(data);
		}
	}
	
	/**
	 * TLV3 数据压缩
	 * @param str
	 * @return
	 */
	private byte[] compress(String str) {
		if (str == null){
			return null;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
	   GZIPOutputStream gzip;
		try {
			gzip = new GZIPOutputStream(out);
		   gzip.write(str.getBytes());
		   gzip.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return out.toByteArray();
	}
	
	/**
	 * 解析 TLV3 数据
	 * @param data
	 * @param startIndex
	 * @param length
	 * @return
	 */
	protected String getTLV3Data(byte[] data, int startIndex, int length){
		byte[] tmp = ProtocolUtil.getDefaultByte(length);
		System.arraycopy(data, startIndex, tmp, 0, tmp.length);
		return decompress(tmp);
	}
	
	/**
	 * TLV3 数据解压
	 * @param data
	 * @return
	 */
    public String decompress(byte[] data) {   
        byte[] output = new byte[0];   
  
        Inflater decompresser = new Inflater();   
        decompresser.reset();   
        decompresser.setInput(data);   
  
        ByteArrayOutputStream o = new ByteArrayOutputStream(data.length);   
        try {   
            byte[] buf = new byte[1024];   
            while (!decompresser.finished()) {   
                int i = decompresser.inflate(buf);   
                o.write(buf, 0, i);   
            	}   
            output = o.toByteArray();   
        } catch (Exception e) {   
            output = data;   
            e.printStackTrace();   
        } finally {   
            try {   
                o.close();   
            } catch (IOException e) {   
                e.printStackTrace();   
            }   
        }
      String result="";
		try {
			result = new String(output,"UTF-8");
		} catch (UnsupportedEncodingException e) {
		}
    	decompresser.end();   
    	return result;   
	}

}
