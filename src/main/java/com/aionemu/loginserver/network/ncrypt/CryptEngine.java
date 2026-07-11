package com.aionemu.loginserver.network.ncrypt;

import com.aionemu.commons.utils.Rnd;

/**
 * 登录服包加解密引擎：Blowfish 加解密、校验和与首包 XOR。
 * Login-server crypto engine: Blowfish encrypt/decrypt, checksum and first-packet XOR.
 *
 * @author EvilSpirit
 */
public class CryptEngine {

    /** 当前 Blowfish 密钥 / Current Blowfish key */
    private byte[] key = {(byte) 0x6b, (byte) 0x60, (byte) 0xcb, (byte) 0x5b,
        (byte) 0x82, (byte) 0xce, (byte) 0x90, (byte) 0xb1, (byte) 0xcc,
        (byte) 0x2b, (byte) 0x6c, (byte) 0x55, (byte) 0x6c, (byte) 0x6c,
        (byte) 0x6c, (byte) 0x6c};
    /** 密钥是否已切换到会话密钥 / Whether the session key has been applied */
    private boolean updatedKey = false;
    /** Blowfish 密码器 / Blowfish cipher */
    private BlowfishCipher cipher;

    /**
     * 使用静态初始密钥构造，以便加密发给客户端的首包。
     * Construct with the static initial key for the first client-bound packet.
     */
    public CryptEngine() {
        cipher = new BlowfishCipher(key);
    }

    /**
     * 更新后续包加解密使用的 Blowfish 密钥。
     * Update the Blowfish key used for subsequent packet crypto.
     *
     * new Blowfish key
     */
    public void updateKey(byte[] newKey) {
        this.key = newKey;
    }

    /**
     * 解密数据并校验校验和。
     * Decrypt data and verify checksum.
     *
     * @param data 待解密字节数组 / bytes to decrypt
     * offset
     * length
     *
     * @return 校验和合法返回 true / true if checksum is valid
     */
    public boolean decrypt(byte[] data, int offset, int length) {
        cipher.decipher(data, offset, length);

        return verifyChecksum(data, offset, length);
    }

    /**
     * 加密数据：首包走 XOR+初始密钥，之后走校验和+会话密钥。
     * Encrypt data: first packet uses XOR+initial key, later packets use checksum+session key.
     *
     * @param data 待加密字节数组 / bytes to encrypt
     * offset
     * length
     * @return 加密后长度 / encrypted length
     */
    public int encrypt(byte[] data, int offset, int length) {
        length += 4;

        // 密钥未更新，因此第一个包应用……加密 / the key is not updated, so the first packet should be encrypted with
        // 初始密钥 / initial key
        if (!updatedKey) {
            length += 4;
            length += 8 - length % 8;
            encXORPass(data, offset, length, Rnd.nextInt());
            cipher.cipher(data, offset, length);
            cipher.updateKey(key);
            updatedKey = true;
        } else {
            length += 8 - length % 8;
            appendChecksum(data, offset, length);
            cipher.cipher(data, offset, length);
        }

        return length;
    }

    /**
     * 校验包尾校验和。
     * Verify the trailing packet checksum.
     *
     * encrypted packet
     * offset
     * length
     *
     * @return 校验和通过返回 true / true if checksum is ok
     */
    private boolean verifyChecksum(byte[] data, int offset, int length) {
        if ((length & 3) != 0 || (length <= 4)) {
            return false;
        }

        long chksum = 0;
        int count = length - 4;
        long check;
        int i;

        for (i = offset; i < count; i += 4) {
            check = data[i] & 0xff;
            check |= data[i + 1] << 8 & 0xff00;
            check |= data[i + 2] << 0x10 & 0xff0000;
            check |= data[i + 3] << 0x18 & 0xff000000;
            chksum ^= check;
        }

        check = data[i] & 0xff;
        check |= data[i + 1] << 8 & 0xff00;
        check |= data[i + 2] << 0x10 & 0xff0000;
        check |= data[i + 3] << 0x18 & 0xff000000;
        check = data[i] & 0xff;
        check |= data[i + 1] << 8 & 0xff00;
        check |= data[i + 2] << 0x10 & 0xff0000;
        check |= data[i + 3] << 0x18 & 0xff000000;

        return 0 == chksum;
    }

    /**
     * 在包尾追加校验和。
     * Append checksum to the end of the packet.
     *
     * @param raw 待写校验和的包 / packet buffer
     * offset
     * length
     */
    private void appendChecksum(byte[] raw, int offset, int length) {
        long chksum = 0;
        int count = length - 4;
        long ecx;
        int i;

        for (i = offset; i < count; i += 4) {
            ecx = raw[i] & 0xff;
            ecx |= raw[i + 1] << 8 & 0xff00;
            ecx |= raw[i + 2] << 0x10 & 0xff0000;
            ecx |= raw[i + 3] << 0x18 & 0xff000000;
            chksum ^= ecx;
        }

        ecx = raw[i] & 0xff;
        ecx |= raw[i + 1] << 8 & 0xff00;
        ecx |= raw[i + 2] << 0x10 & 0xff0000;
        ecx |= raw[i + 3] << 0x18 & 0xff000000;
        raw[i] = (byte) (chksum & 0xff);
        raw[i + 1] = (byte) (chksum >> 0x08 & 0xff);
        raw[i + 2] = (byte) (chksum >> 0x10 & 0xff);
        raw[i + 3] = (byte) (chksum >> 0x18 & 0xff);
    }

    /**
     * 首包 XOR 加密（4 字节整型密钥）。
     * First-packet XOR pass with a 4-byte integer key.
     *
     * @param data 待加密数据 / data to encrypt
     * offset
     * length
     * @param key 整型密钥 / integer key
     */
    private void encXORPass(byte[] data, int offset, int length, int key) {
        int stop = length - 8;
        int pos = 4 + offset;
        int edx;
        int ecx = key;

        while (pos < stop) {
            edx = (data[pos] & 0xFF);
            edx |= (data[pos + 1] & 0xFF) << 8;
            edx |= (data[pos + 2] & 0xFF) << 16;
            edx |= (data[pos + 3] & 0xFF) << 24;
            ecx += edx;
            edx ^= ecx;
            data[pos++] = (byte) (edx & 0xFF);
            data[pos++] = (byte) (edx >> 8 & 0xFF);
            data[pos++] = (byte) (edx >> 16 & 0xFF);
            data[pos++] = (byte) (edx >> 24 & 0xFF);
        }

        data[pos++] = (byte) (ecx & 0xFF);
        data[pos++] = (byte) (ecx >> 8 & 0xFF);
        data[pos++] = (byte) (ecx >> 16 & 0xFF);
        data[pos] = (byte) (ecx >> 24 & 0xFF);
    }
}
