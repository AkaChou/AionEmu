package com.aionemu.loginserver.network.ncrypt;

import com.aionemu.boot.i18n.I18n;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.spec.RSAKeyGenParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.commons.utils.Rnd;

/**
 * 密钥生成器：生成 Blowfish 密钥与 RSA 密钥对。
 * Key generator that produces Blowfish keys and RSA key pairs.
 *
 * @author -Nemesiss-
 */
@Slf4j
public class KeyGen {

    /** Blowfish 密钥生成器 / Blowfish key generator */
    private static KeyGenerator blowfishKeyGen;
    /** RSA key pairs with encrypted modulus N / RSA key pairs with encrypted modulus N */
    private static EncryptedRSAKeyPair[] encryptedRSAKeyPairs;

    /**
     * RSA 密钥生成器并预热 RSA 解密。 / RSA 密钥生成器并预热 RSA 解密。
     * Initialize Blowfish and RSA key generators and warm up RSA decrypt.
     *
     * key algo init failed。 / key algo init failed.
     */
    public static void init() throws GeneralSecurityException {
        log.info(I18n.get("log.e944cdc0c5ce"));

        blowfishKeyGen = KeyGenerator.getInstance("Blowfish");

        KeyPairGenerator rsaKeyPairGenerator = KeyPairGenerator.getInstance("RSA");

        RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(1024, RSAKeyGenParameterSpec.F4);
        rsaKeyPairGenerator.initialize(spec);
        encryptedRSAKeyPairs = new EncryptedRSAKeyPair[10];

        for (int i = 0; i < 10; i++) {
            encryptedRSAKeyPairs[i] = new EncryptedRSAKeyPair(
                    rsaKeyPairGenerator.generateKeyPair());
        }

        // 预初始化 RSA 密码器……约节省 300ms / Pre-init RSA cipher.. saving about 300ms
        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
        rsaCipher.init(Cipher.DECRYPT_MODE, encryptedRSAKeyPairs[0].getRSAKeyPair().getPrivate());
    }

    /**
     * 生成随机 Blowfish 密钥。
     * Generate a random Blowfish key.
     *
     * random Blowfish key
     */
    public static SecretKey generateBlowfishKey() {
        return blowfishKeyGen.generateKey();
    }

    /**
     * 从池中随机取一对加密模数的 RSA 密钥对。
     * Pick a random encrypted-modulus RSA key pair from the pool.
     *
     * @return 加密 RSA 密钥对 / encrypted RSA key pair
     */
    public static EncryptedRSAKeyPair getEncryptedRSAKeyPair() {
        return encryptedRSAKeyPairs[Rnd.nextInt(10)];
    }
}
