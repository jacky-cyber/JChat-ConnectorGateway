package cn.jpush.protocal.utils;

import java.util.Random;

public class QNRandomUtils {
	
	private QNRandomUtils(){}
	
	private static final char[] symbols = new char[36];
	

	static {
		for (int idx = 0; idx < 10; ++idx)
			symbols[idx] = (char) ('0' + idx);
		for (int idx = 10; idx < 36; ++idx)
			symbols[idx] = (char) ('a' + idx - 10);
	}

	private static final Random random = new Random();

	private static final char[] buf =  new char[20];

	public static String nextString() {
		for (int idx = 0; idx < buf.length; ++idx) 
			buf[idx] = symbols[random.nextInt(symbols.length)];
		return new String(buf);
	}


	public static String getSuffixFile(String key,String ext){
		return key+ "." +ext;
	}
	
	public static String getSuffixFile(String ext){
		String hashCode = StringUtils.toMD5(nextString()+System.currentTimeMillis());
		hashCode  = hashCode.substring(0, 26);
		return hashCode + "." +ext;
	}

	public static void main(String[] args) {
		String code = QNRandomUtils.nextString()+System.currentTimeMillis();
		String md5 = StringUtils.toMD5(code);
		System.out.println(md5.substring(0,26));
		
		//QNRandomUtils randomString = new QNRandomUtils(9);
	/*	StringBuffer buffer = new StringBuffer(10000000);
		//System.out.println(UUID.randomUUID());
			for(int i = 0 ; i<4000000; i++){
		//	System.err.println(QNRandomUtils.nextString());
			buffer.append(StringUtils.toMD5(QNRandomUtils.nextString()+System.currentTimeMillis())).append(",");			
		}*/
		
/*//		String[] results = buffer.toString().split(",");
//		Set<String> hasSet = new HashSet<String>();
//		/*		for(int i = 0; i<results.length; i++){
//			hasSet.add(results[i]);			
//		}*/
//
//		for(String sv:results){
//			if(!hasSet.add(sv)){
//				System.out.println("重复结果>............"+sv);            	
//			}
//		}
//
//		System.out.println(results.length +" set length :"+hasSet.size());
//		
	}
}
