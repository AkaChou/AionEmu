package com.aionemu.gameserver.network;

import lombok.extern.slf4j.Slf4j;
import java.nio.ByteBuffer;

import com.aionemu.commons.utils.Rnd;

/**
 * 连接级加解密器：加密服务端包、解密客户端包，并混淆操作码。
 * Per-connection crypt that encrypts server packets, decrypts client packets and obfuscates opcodes.
 *
 * @author hack99
 * @author kao
 * @author -Nemesiss-
 */
@Slf4j
public class Crypt {

	/**
	 * 服务端包第二字节必须等于该静态码。
	 * Second byte of server packet must equal this static code.
	 */
	public final static byte staticServerPacketCode = 0x56; // 5.8
	/**
	 * 在首个服务端包发出后启用加密。
	 * Crypt is enabled after the first server packet was sent.
	 */
	private boolean isEnabled;
	private EncryptionKeyPair packetKey = null;

	/**
	 * 启用加密密钥：生成随机密钥用于加密后续服务端包并解密客户端包。
	 * 由 SM_KEY 调用，将密钥发送给 Aion 客户端。
	 * Enables the crypt key — generates a random key used to encrypt subsequent server packets
	 * and decrypt client packets. Called from SM_KEY which sends the key to the Aion client.
	 *
	 * @return 发给客户端的“伪密钥” / "false key" for the Aion client
	 */
	public final int enableKey() {
		if (packetKey != null) {
			throw new KeyAlreadySetException();
		}

		/**
		 * 随机密钥，用于包加解密
		 * rnd key — used to encrypt/decrypt packets
		 */
		int key = Rnd.nextInt();

		packetKey = new EncryptionKeyPair(key);

		log.debug("new encrypt key: " + packetKey);

		/**
		 * 经变换后发给客户端的伪密钥
		 * false key sent to aion client in SM_KEY
		 */
		return (key ^ 0xCD92E4D5) + 0x3FF2CCD7; // 5.8 EU
	}

	/**
	 * 解密缓冲区中的客户端包。
	 * Decrypts the client packet from this buffer.
	 *
	 * @param buf 待解密缓冲区 / buffer to decrypt
	 * @return 解密是否成功 / true if decryption succeeded
	 */
	public final boolean decrypt(ByteBuffer buf) {
		if (!isEnabled) {
			log.debug("if encryption wasn't enabled, then maybe it's client reconnection, so skip packet");
			return true;
		}
		return packetKey.decrypt(buf);
	}

	/**
	 * 加密缓冲区中的服务端包；首包不加密并开启后续加密。
	 * Encrypts the server packet from this buffer; first packet is unencrypted and enables crypt.
	 *
	 * @param buf 待加密缓冲区 / buffer to encrypt
	 */
	public final void encrypt(ByteBuffer buf) {
		if (!isEnabled) {
			/**
			 * 首包不加密
			 * first packet is not encrypted
			 */
			isEnabled = true;
			log.debug("packet is not encrypted... send in SM_KEY");
			return;
		}
		packetKey.encrypt(buf);
	}

	/**
	 * 服务端操作码混淆。
	 * Server packet opcode obfuscation.
	 *
	 * @param op 原始操作码 / raw opcode
	 * @return 混淆后的操作码 / obfuscated opcode
	 */
	public static final int encodeOpcodec(int op) {
		return (op + 0xD5) ^ 0xD5; // 5.8 EU
	}
}
