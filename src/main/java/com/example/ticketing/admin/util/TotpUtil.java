package com.example.ticketing.admin.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;

public class TotpUtil {

    public static boolean verifyOtp(String secret, String codeStr) {
        if (codeStr == null || codeStr.length() != 6) {
            return false;
        }
        try {
            int code = Integer.parseInt(codeStr);
            long timeIndex = System.currentTimeMillis() / 1000 / 30;
            // Allow window of -1, 0, +1 to account for clock skew
            for (int i = -2; i <= 2; i++) {
                if (generateTOTP(secret, timeIndex + i) == code) {
                    return true;
                }
            }
        } catch (Exception e) {
            // ignore parsing errors
        }
        return false;
    }

    private static int generateTOTP(String secret, long timeIndex) throws Exception {
        byte[] key = decodeBase32(secret);
        byte[] data = ByteBuffer.allocate(8).putLong(timeIndex).array();
        
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "RAW"));
        byte[] hash = mac.doFinal(data);
        
        int offset = hash[hash.length - 1] & 0xf;
        int binary = ((hash[offset] & 0x7f) << 24) |
                     ((hash[offset + 1] & 0xff) << 16) |
                     ((hash[offset + 2] & 0xff) << 8) |
                     (hash[offset + 3] & 0xff);
        
        return binary % 1000000;
    }
    
    private static byte[] decodeBase32(String base32) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        base32 = base32.toUpperCase().replaceAll("[^A-Z2-7]", "");
        int len = base32.length();
        byte[] bytes = new byte[len * 5 / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int count = 0;
        for (int i = 0; i < len; i++) {
            int val = chars.indexOf(base32.charAt(i));
            if (val < 0) continue;
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                if (count < bytes.length) {
                    bytes[count++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                }
                bitsLeft -= 8;
            }
        }
        return bytes;
    }
}
