package cn.jpush.protocal.im.request;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;

import cn.jpush.protocal.utils.Command;
import cn.jpush.protocal.utils.ProtocolUtil;

public class BaseRequest {
	//  request head  
	protected int pkg_length = 0;   // 2B
	protected int version;      // 1B
	protected int command = Command.JPUSH_IM.COMMAND;      // 1B   
	protected long rid;         // 8B
	protected int sid;          // 4B
	protected long juid;        // 8B
	protected int platform = 5;  // 表示平台
	
	protected byte[] mHeader;
	protected byte[] mBody;

	public BaseRequest(int version, long rid, int sid, long juid){
		this.version = version;
		this.rid = rid;
		this.sid = sid;
		this.juid = juid;
	}
	
	/**
	 * 构建消息头
	 */
	public void buidRequestHeader() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try{
			bos.write(ProtocolUtil.intToByteArray(this.pkg_length, 2));  // pkg length 2B
			bos.write(ProtocolUtil.intToByteArray(this.version, 1));  // version  1B  
			bos.write(ProtocolUtil.intToByteArray(this.command, 1));  // command  1B
			bos.write(ProtocolUtil.longToByteArray(this.rid, 8));   // rid  8B
			bos.write(ProtocolUtil.intToByteArray(this.sid, 4));   // sid   4B
			bos.write(ProtocolUtil.longToByteArray(this.juid, 8));  // juid    8B
			this.mHeader = bos.toByteArray();
		} catch (Exception e) {
			try {
				bos.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	/**
	 * 构建消息体
	 */
	public void buidRequestBody() {}
	
	/**
	 * 返回整个请求包
	 * @return
	 * @throws KKException 
	 */
	public byte[] getRequestPackage(){
		this.buidRequestHeader();
		this.buidRequestBody();
		return this.combineData(mHeader, mBody);
	}
	
	/**
	 *  合并消息头和消息体
	 * @param header
	 * @param body
	 * @return
	 * @throws KKException
	 */
	protected byte[] combineData(byte[] header, byte[] body) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			if (header != null) {
				baos.write(header);
			}
			if (body != null) {
				baos.write(body);
			}
			return updateTotalLength(baos);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			throw new RuntimeException();
		}
	}
	
	/**0
	 * 修改包中的包长度数据
	 * @param bos
	 * @return
	 * @throws IOException
	 */
	private byte[] updateTotalLength(ByteArrayOutputStream bos) throws IOException {
		pkg_length = bos.size();
		byte[] totalLenth = ProtocolUtil.intToByteArray(pkg_length, 2);
		try {
			bos.flush();
		} catch (Exception e) {
		}
		byte[] data = bos.toByteArray();
		System.arraycopy(totalLenth, 0, data, 0, 2);
		return data;
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
