package com.aionemu.gameserver.utils.xml;

import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * UTF-16LE 文本的 Deflate 压缩/解压工具。
 * Deflate compress/decompress helpers for UTF-16LE text.
 */
public final class CompressUtil {

	/**
	 * 解压字节数组为 UTF-16LE 字符串。
	 * Decompress a byte array into a UTF-16LE string.
	 *
	 * @param bytes 压缩数据 / Compressed bytes
	 * @return 解压后的文本 / Decompressed text
	 * @throws Exception 解压失败时 / On decompress failure
	 */
	public static String Decompress(byte[] bytes) throws Exception {
		Inflater decompressor = new Inflater();
		decompressor.setInput(bytes);

		ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);

		byte[] buffer = new byte[1024];
		try {
			while (true) {
				int count = decompressor.inflate(buffer);
				if (count > 0) {
					bos.write(buffer, 0, count);
				} else {
					if ((count == 0) && (decompressor.finished())) {
						break;
					}
					throw new RuntimeException("Bad zip data, size: " + bytes.length);
				}
			}
		} finally {
			decompressor.end();
		}

		bos.close();
		return bos.toString("UTF-16LE");
	}

	/**
	 * 将文本以 UTF-16LE 编码后 Deflate 压缩。
	 * Encode text as UTF-16LE and Deflate-compress it.
	 *
	 * @param text 源文本 / Source text
	 * @return 压缩字节 / Compressed bytes
	 *
	 * @throws Exception 压缩失败时 / On compress failure
	 */
	public static byte[] Compress(String text) throws Exception {
		Deflater compressor = new Deflater();
		byte[] bytes = text.getBytes("UTF-16LE");
		compressor.setInput(bytes);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		compressor.finish();

		byte[] buffer = new byte[1024];
		try {
			while (!compressor.finished()) {
				int count = compressor.deflate(buffer);
				bos.write(buffer, 0, count);
			}
		} finally {
			compressor.finish();
		}

		bos.close();
		return bos.toByteArray();
	}
}
