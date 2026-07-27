package com.aionemu.gameserver.network;

import java.nio.ByteBuffer;

/**
 * 客户端/服务端加解密密钥对，基于 baseKey 生成并随包长度滚动更新。
 * Client/server encryption key pair derived from a base key and rolled with packet size.
 *
 * @author cura
 */
public class EncryptionKeyPair {

	/**
	 * 服务端密钥在 keys 中的下标。
	 * Index into {@link #keys} for the server encryption key.
	 */
	private static final int SERVER = 0;

	/**
	 * 客户端密钥在 keys 中的下标。
	 * Index into {@link #keys} for the client encryption key.
	 */
	private static final int CLIENT = 1;

	/**
	 * 静态 XOR 密钥表。
	 * Static XOR key table.
	 */
	private final static byte[] staticKey = "nKO/WctQ0AVLbpzfBkS6NevDYT8ourG5CRlmdjyJ72aswx4EPq1UgZhFMXH?3iI9"
			.getBytes();

	/**
	 * 客户端包第二字节必须等于该静态码。
	 * Second byte of client packet must equal this static code.
	 */
	private final static byte staticClientPacketCode = 0x7D;

	/**
	 * 用于生成客户端/服务端密钥的基础密钥。
	 * Base key used to generate client/server keys.
	 */
	private int baseKey = 0;

	/**
	 * 加密密钥数组：0=服务端，1=客户端。
	 * Encryption keys: 0=server, 1=client.
	 */
	private byte[][] keys = null;

	/**
	 * 上次使用密钥的时间戳。
	 * Timestamp of last key use.
	 */
	private long lastUpdate;

	/**
	 * 基于 baseKey 初始化客户端/服务端加密密钥。
	 * Initializes client/server encryption keys from the base key.
	 *
	 * @param baseKey 随机整数基础密钥 / random integer base key
	 */
	public EncryptionKeyPair(int baseKey) {
		this.baseKey = baseKey;
		this.keys = new byte[2][];

		this.keys[SERVER] = new byte[] { (byte) (baseKey & 0xff), (byte) ((baseKey >> 8) & 0xff),
				(byte) ((baseKey >> 16) & 0xff), (byte) ((baseKey >> 24) & 0xff), (byte) 0xa1, (byte) 0x6c, (byte) 0x54,
				(byte) 0x87 };
		this.keys[CLIENT] = new byte[this.keys[SERVER].length];
		System.arraycopy(this.keys[SERVER], 0, this.keys[CLIENT], 0, this.keys[SERVER].length);
		this.lastUpdate = System.currentTimeMillis();
	}

	/**
	 * 获取生成密钥对所用的 baseKey。
	 * Returns the base key used to generate the key pair.
	 *
	 * base key
	 */
	public int getBaseKey() {
		return baseKey;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{client:0x");
		for (int i = 0; i < keys[CLIENT].length; i++) {
			sb.append(Integer.toHexString(keys[CLIENT][i] & 0xff));
		}
		sb.append(",server:0x");
		for (int i = 0; i < keys[SERVER].length; i++) {
			sb.append(Integer.toHexString(keys[SERVER][i] & 0xff));
		}
		sb.append(",base:0x");
		sb.append(Integer.toHexString(baseKey));
		sb.append(",update:" + lastUpdate + "}");
		return sb.toString();
	}

	/**
	 * 校验客户端包是否正确解码且由 Aion 客户端正确编码。
	 * Validates that the client packet was decoded correctly and coded by the Aion client.
	 *
	 * @param buf 包缓冲区 / packet buffer
	 * @return 密钥对有效时为 {@code true} / {@code true} if valid
	 */
	private final boolean validateClientPacket(ByteBuffer buf) {
		return buf.getShort(0) == ~buf.getShort(3) && buf.get(2) == staticClientPacketCode;
	}

	/**
	 * 解密缓冲区中的客户端包；成功时滚动更新客户端密钥。
	 * Decrypts the client packet from this buffer; on success rolls the client key.
	 *
	 * @param buf 待解密缓冲区 / buffer to decrypt
	 * @return 解密是否成功 / true if decryption succeeded
	 */
	public boolean decrypt(ByteBuffer buf) {
		if (buf.remaining() < 5) {
			return false;
		}
		final byte[] data = buf.array();
		final int size = buf.remaining();
		byte[] clientPacketKey = keys[CLIENT];

		/** 当前待解密字节下标 / index of the byte to decrypt */
		int arrayIndex = buf.arrayOffset() + buf.position();

		/** 前一加密字节 / previous encrypted byte */
		int prev = data[arrayIndex];

		/** 解密首字节 / decrypt first byte */
		data[arrayIndex++] ^= (clientPacketKey[0] & 0xff);

		/** 解密循环 / decrypt loop */
		for (int i = 1; i < size; i++, arrayIndex++) {
			int curr = data[arrayIndex] & 0xff;
			data[arrayIndex] ^= (staticKey[i & 63] & 0xff) ^ (clientPacketKey[i & 7] & 0xff) ^ prev;
			prev = curr;
		}

		/** 旧密钥为长整型 / old key as long */
		long oldKey = (((long) clientPacketKey[0] & 0xff) << 0) | (((long) clientPacketKey[1] & 0xff) << 8)
				| (((long) clientPacketKey[2] & 0xff) << 16) | (((long) clientPacketKey[3] & 0xff) << 24)
				| (((long) clientPacketKey[4] & 0xff) << 32) | (((long) clientPacketKey[5] & 0xff) << 40)
				| (((long) clientPacketKey[6] & 0xff) << 48) | (((long) clientPacketKey[7] & 0xff) << 56);

		/** 按包长度滚动密钥 / roll key by packet size */
		oldKey += size;

		if (validateClientPacket(buf)) {
			/** 写回新密钥 / write new key value */
			clientPacketKey[0] = (byte) (oldKey >> 0 & 0xff);
			clientPacketKey[1] = (byte) (oldKey >> 8 & 0xff);
			clientPacketKey[2] = (byte) (oldKey >> 16 & 0xff);
			clientPacketKey[3] = (byte) (oldKey >> 24 & 0xff);
			clientPacketKey[4] = (byte) (oldKey >> 32 & 0xff);
			clientPacketKey[5] = (byte) (oldKey >> 40 & 0xff);
			clientPacketKey[6] = (byte) (oldKey >> 48 & 0xff);
			clientPacketKey[7] = (byte) (oldKey >> 56 & 0xff);
			return true;
		}
		return false;
	}

	/**
	 * 加密缓冲区中的服务端包，并滚动更新服务端密钥。
	 * Encrypts the server packet from this buffer and rolls the server key.
	 *
	 * @param buf 待加密缓冲区 / buffer to encrypt
	 */
	public void encrypt(ByteBuffer buf) {
		final byte[] data = buf.array();
		final int size = buf.remaining();
		byte[] serverPacketKey = keys[SERVER];

		/** 当前待加密字节下标 / index of the byte to encrypt */
		int arrayIndex = buf.arrayOffset() + buf.position();

		/** 加密首字节 / encrypt first byte */
		data[arrayIndex] ^= (serverPacketKey[0] & 0xff);

		/** 前一加密字节 / previous encrypted byte */
		int prev = data[arrayIndex++];

		/** 加密循环 / encrypt loop */
		for (int i = 1; i < size; i++, arrayIndex++) {
			data[arrayIndex] ^= (staticKey[i & 0x3F] & 0xff) ^ (serverPacketKey[i & 0x07] & 0xff) ^ prev;
			prev = data[arrayIndex];
		}

		/** 旧密钥为长整型 / old key as long */
		long oldKey = (((long) serverPacketKey[0] & 0xff) << 0) | (((long) serverPacketKey[1] & 0xff) << 8)
				| (((long) serverPacketKey[2] & 0xff) << 16) | (((long) serverPacketKey[3] & 0xff) << 24)
				| (((long) serverPacketKey[4] & 0xff) << 32) | (((long) serverPacketKey[5] & 0xff) << 40)
				| (((long) serverPacketKey[6] & 0xff) << 48) | (((long) serverPacketKey[7] & 0xff) << 56);

		/** 按包长度滚动密钥 / roll key by packet size */
		oldKey += size;

		/** 写回新密钥 / write new key value */
		serverPacketKey[0] = (byte) (oldKey >> 0 & 0xff);
		serverPacketKey[1] = (byte) (oldKey >> 8 & 0xff);
		serverPacketKey[2] = (byte) (oldKey >> 16 & 0xff);
		serverPacketKey[3] = (byte) (oldKey >> 24 & 0xff);
		serverPacketKey[4] = (byte) (oldKey >> 32 & 0xff);
		serverPacketKey[5] = (byte) (oldKey >> 40 & 0xff);
		serverPacketKey[6] = (byte) (oldKey >> 48 & 0xff);
		serverPacketKey[7] = (byte) (oldKey >> 56 & 0xff);
	}
}
