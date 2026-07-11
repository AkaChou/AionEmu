package com.aionemu.loginserver.network.ncrypt;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;

/**
 * 保存标准 RSA 密钥对，并对模数 N 做网络传输用的简单混淆。
 * Holds a standard RSA key pair and scrambles modulus N for network transfer.
 *
 * @author EvilSpirit
 */
public class EncryptedRSAKeyPair {

    /** Original RSA key pair / Original RSA key pair */
    private KeyPair RSAKeyPair;
    /** 混淆后的模数 / Scrambled modulus bytes */
    private byte[] encryptedModulus;

    /**
     * 保存 RSA 密钥对并加密其模数 N。
     * Store the RSA key pair and encrypt its modulus N.
     *
     * @param RSAKeyPair 标准 KeyPairGenerator 生成的密钥对 / key pair from KeyPairGenerator
     */
    public EncryptedRSAKeyPair(KeyPair RSAKeyPair) {
        this.RSAKeyPair = RSAKeyPair;
        encryptedModulus = encryptModulus(((RSAPublicKey) this.RSAKeyPair.getPublic()).getModulus());
    }

    /**
     * 对 RSA 模数 N 做协议约定的字节混淆。
     * Scramble RSA modulus N with the protocol-defined byte mixing.
     *
     * RSA modulus
     *
     * @param modulus @return 混淆后的模数 / encrypted modulus
     */
    private byte[] encryptModulus(BigInteger modulus) {
        byte[] encryptedModulus = modulus.toByteArray();

        if ((encryptedModulus.length == 0x81) && (encryptedModulus[0] == 0x00)) {
            byte[] temp = new byte[0x80];

            System.arraycopy(encryptedModulus, 1, temp, 0, 0x80);

            encryptedModulus = temp;
        }

        for (int i = 0; i < 4; i++) {
            byte temp = encryptedModulus[i];

            encryptedModulus[i] = encryptedModulus[0x4d + i];
            encryptedModulus[0x4d + i] = temp;
        }

        for (int i = 0; i < 0x40; i++) {
            encryptedModulus[i] = (byte) (encryptedModulus[i] ^ encryptedModulus[0x40 + i]);
        }

        for (int i = 0; i < 4; i++) {
            encryptedModulus[0x0d + i] = (byte) (encryptedModulus[0x0d + i] ^ encryptedModulus[0x34 + i]);
        }

        for (int i = 0; i < 0x40; i++) {
            encryptedModulus[0x40 + i] = (byte) (encryptedModulus[0x40 + i] ^ encryptedModulus[i]);
        }

        return encryptedModulus;
    }

    /**
     * 获取原始 RSA 密钥对。
     * Get the original RSA key pair.
     *
     * RSA key pair
     */
    public KeyPair getRSAKeyPair() {
        return RSAKeyPair;
    }

    /**
     * 获取用于网络传输的混淆模数。
     * Get the scrambled modulus for network transfer.
     *
     * encrypted modulus
     */
    public byte[] getEncryptedModulus() {
        return encryptedModulus;
    }
}
