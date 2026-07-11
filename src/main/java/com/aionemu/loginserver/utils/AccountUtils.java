package com.aionemu.loginserver.utils;


import com.aionemu.boot.i18n.I18n;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.aionemu.commons.utils.Base64;
import lombok.extern.slf4j.Slf4j;

/**
 * 账号密码编码等工具方法。
 * Utility methods for account password encoding, etc.
 *
 * @author SoulKeeper
 */
@Slf4j
public class AccountUtils {

    /**
     * 对密码进行编码：先用 SHA-1 哈希，再以 Base64 包装为字符串。
     * Encodes password. SHA-1 is used to encode password bytes, Base64 wraps SHA1-hash to string.
     *
     * @param password 待编码密码 / password to encode
     * @return 编码后的密码 / encoded password
     */
    public static String encodePassword(String password) {
        try {
            MessageDigest messageDiegest = MessageDigest.getInstance("SHA-1");
            messageDiegest.update(password.getBytes("UTF-8"));
            return Base64.encodeToString(messageDiegest.digest(), false);
        } catch (NoSuchAlgorithmException e) {
            log.error(I18n.get("log.4f2731090659"));
            throw new Error(e);
        } catch (UnsupportedEncodingException e) {
            log.error(I18n.get("log.4f2731090659"));
            throw new Error(e);
        }
    }
}
