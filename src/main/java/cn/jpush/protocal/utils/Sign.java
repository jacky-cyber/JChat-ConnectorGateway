package cn.jpush.protocal.utils;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.Formatter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.UnsupportedEncodingException;  

public class Sign {
    public static void main(String[] args) {
        String sign = Sign.getSignature("4f7aef34fb361292c566a1cd", "111", "222", "054d6103823a726fc12d0466");
        System.out.println(sign);
    };
    
    public static String getSignature(String appKey, String timestamp, String randomStr, String masterSecrect){
    	 String str = "appKey=" + appKey +
                 "&timestamp=" + timestamp +
                 "&randomStr=" + randomStr +
                 "&masterSecrect=" + masterSecrect;
    	 return StringUtils.toMD5(str);
    }

    private static String byteToHex(final byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash)
        {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }

    private static String create_nonce_str() {
        return UUID.randomUUID().toString();
    }

    private static String create_timestamp() {
        return Long.toString(System.currentTimeMillis() / 1000);
    }
}
