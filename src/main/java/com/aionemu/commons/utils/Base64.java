/**
 * This file is part of Aion-Lightning <aion-lightning.org>.
 *
 * Aion-Lightning is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Aion-Lightning is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. *
 *
 * You should have received a copy of the GNU General Public License along with Aion-Lightning. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * Credits goes to all Open Source Core Developer Groups listed below Please do not change here something, ragarding the developer credits, except the
 * "developed by XXXX". Even if you edit a lot of files in this source, you still have no rights to call it as "your Core". Everybody knows that this
 * Emulator Core was developed by Aion Lightning
 * 
 * @-Aion-Unique-
 * @-Aion-Lightning
 * @Aion-Engine
 * @Aion-Extreme
 * @Aion-NextGen
 * @Aion-Core Dev.
 */
package com.aionemu.commons.utils;

import java.util.Arrays;

/**
 * Base64编解码工具类，完全符合RFC 2045规范。
 * A very fast and memory efficient class to encode and decode to and from BASE64 in full accordance with RFC 2045.
 *
 * 性能特点 Performance characteristics:
 * - 在Windows XP SP1及更高版本上，对于小数组(10-1000字节)，此编解码器比sun.misc.Encoder()/Decoder()快10倍
 * - 对于大数组(10000-1000000字节)快2-3倍
 * - 对于字节数组，编码速度比Jakarta Commons Base64编解码快20%，解码大数组时快50%
 * - 对于很小的数组(<30字节)，速度是其他实现的两倍
 * 
 * 内存效率 Memory efficiency:
 * - 不创建临时数组，仅分配结果数组
 * - 产生更少的垃圾，可以处理两倍于其他算法的数组大小
 * 
 * 输出特点 Output characteristics:
 * - 输出与Sun的编码器相同，除了Sun的编码器在最后一个字符不是填充字符时会添加行分隔符
 * - 完全符合RFC 2045规范
 *
 * @author Mikael Grev
 * @version 2.2
 */
public class Base64 {
	
	// Base64字符表 Base64 character table
	private static final char[] CA = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
	
	// 反向查找表 Reverse lookup table
	private static final int[] IA = new int[256];
	
	static {
		// 初始化反向查找表 Initialize reverse lookup table
		Arrays.fill(IA, -1);
		for (int i = 0, iS = CA.length; i < iS; i++) {
			IA[CA[i]] = i;
		}
		IA['='] = 0;
	}
	
	/**
	 * 将原始字节数组编码为BASE64字符数组
	 * Encodes a raw byte array into a BASE64 char[] representation in accordance with RFC 2045.
	 *
	 * @param sArr 要转换的字节数组。如果为null或长度为0，将返回空数组
	 *            The bytes to convert. If null or length 0 an empty array will be returned.
	 * @param lineSep 是否在76个字符后添加"\r\n"换行符
	 *               Optional "\r\n" after 76 characters, unless end of file.
	 * @return BASE64编码后的字符数组，永不为null
	 *         A BASE64 encoded array. Never null.
	 */
	public static char[] encodeToChar(byte[] sArr, boolean lineSep) {
		// 检查特殊情况 Check special case
		int sLen = sArr != null ? sArr.length : 0;
		if (sLen == 0) {
			return new char[0];
		}
		
		// 计算结果数组长度 Calculate result array length
		int eLen = (sLen / 3) * 3;
		int cCnt = ((sLen - 1) / 3 + 1) << 2;
		int dLen = cCnt + (lineSep ? (cCnt - 1) / 76 << 1 : 0);
		char[] dArr = new char[dLen];
		
		// 编码24位数据块 Encode even 24-bits
		for (int s = 0, d = 0, cc = 0; s < eLen;) {
			// 将三个字节复制到int的低24位 Copy next three bytes into lower 24 bits of int
			int i = (sArr[s++] & 0xff) << 16 | (sArr[s++] & 0xff) << 8 | (sArr[s++] & 0xff);
			
			// 将int编码为四个字符 Encode the int into four chars
			dArr[d++] = CA[(i >>> 18) & 0x3f];
			dArr[d++] = CA[(i >>> 12) & 0x3f];
			dArr[d++] = CA[(i >>> 6) & 0x3f];
			dArr[d++] = CA[i & 0x3f];
			
			// 添加可选的换行符 Add optional line separator
			if (lineSep && ++cc == 19 && d < dLen - 2) {
				dArr[d++] = '\r';
				dArr[d++] = '\n';
				cc = 0;
			}
		}
		
		// 处理剩余字节 Pad and encode last bits if source isn't even 24 bits
		int left = sLen - eLen;
		if (left > 0) {
			// 准备最后的int Prepare the last int
			int i = ((sArr[eLen] & 0xff) << 10) | (left == 2 ? ((sArr[sLen - 1] & 0xff) << 2) : 0);
			
			// 设置最后四个字符 Set last four chars
			dArr[dLen - 4] = CA[i >> 12];
			dArr[dLen - 3] = CA[(i >>> 6) & 0x3f];
			dArr[dLen - 2] = left == 2 ? CA[i & 0x3f] : '=';
			dArr[dLen - 1] = '=';
		}
		return dArr;
	}
	
	/**
	 * 解码BASE64编码的字符数组，忽略所有非法字符，可以处理包含或不包含换行符的数组
	 * Decodes a BASE64 encoded char array. All illegal characters will be ignored and can handle both arrays with and without line separators.
	 *
	 * @param sArr 源字符数组，如果为null或长度为0将返回空数组
	 *             The source array. <code>null</code> or length 0 will return an empty array.
	 * @return 解码后的字节数组，可能长度为0。如果合法字符（包括'='）不能被4整除，则返回null（即肯定已损坏）
	 *         The decoded array of bytes. May be of length 0. Will be <code>null</code> if the legal characters (including '=') isn't divideable by
	 *         4. (I.e. definitely corrupted).
	 */
	public static byte[] decode(char[] sArr) {
		// 检查特殊情况 Check special case
		int sLen = sArr != null ? sArr.length : 0;
		if (sLen == 0) {
			return new byte[0];
		}
		
		// 统计非法字符数量（包括'\r', '\n'）以确定返回数组的大小
		// Count illegal characters (including '\r', '\n') to know what size the returned array will be
		int sepCnt = 0; // 分隔符数量 Number of separator characters
		for (int i = 0; i < sLen; i++) {
			if (IA[sArr[i]] < 0) {
				sepCnt++;
			}
		}
		
		// 检查合法字符（包括'='）是否能被4整除，符合RFC 2045规范
		// Check if legal chars (including '=') are evenly divisible by 4 as specified in RFC 2045
		if ((sLen - sepCnt) % 4 != 0) {
			return null;
		}
		
		// 统计结尾'='的数量 Count '=' at end
		int pad = 0;
		for (int i = sLen; i > 1 && IA[sArr[--i]] <= 0;) {
			if (sArr[i] == '=') {
				pad++;
			}
		}
		
		// 计算解码后的字节长度 Calculate decoded byte length
		int len = ((sLen - sepCnt) * 6 >> 3) - pad;
		
		// 预分配字节数组 Preallocate byte[] of exact length
		byte[] dArr = new byte[len];
		
		// 解码过程 Decoding process
		for (int s = 0, d = 0; d < len;) {
			// 将四个有效字符组装成一个int Assemble three bytes into an int from four "valid" characters
			int i = 0;
			for (int j = 0; j < 4; j++) {
				int c = IA[sArr[s++]];
				if (c >= 0) {
					i |= c << (18 - j * 6);
				} else {
					j--; // 跳过非法字符 Skip illegal character
				}
			}
			
			// 添加字节 Add the bytes
			dArr[d++] = (byte) (i >> 16);
			if (d < len) {
				dArr[d++] = (byte) (i >> 8);
				if (d < len) {
					dArr[d++] = (byte) i;
				}
			}
		}
		
		return dArr;
	}
	
	/**
	 * 快速解码BASE64编码的字符数组，该方法比{@link #decode(char[])}快约两倍
	 * Decodes a BASE64 encoded char array that is known to be reasonably well formatted. 
	 * The method is about twice as fast as {@link #decode(char[])}.
	 *
	 * 前提条件 Preconditions:
	 * + 数组每行长度必须为76个字符或没有行分隔符（单行）
	 *   The array must have a line length of 76 chars OR no line separators at all (one line).
	 * + 行分隔符必须是"\r\n"，符合RFC 2045规范
	 *   Line separator must be "\r\n", as specified in RFC 2045
	 * + 数组在编码字符串内不能包含非法字符
	 *   The array must not contain illegal characters within the encoded string
	 * + 数组开头和结尾可以包含非法字符，这些字符会被适当处理
	 *   The array CAN have illegal characters at the beginning and end, those will be dealt with appropriately.
	 *
	 * @param sArr 源字符数组，如果为null会抛出异常
	 *             The source array. Length 0 will return an empty array. <code>null</code> will throw an exception.
	 * @return 解码后的字节数组，可能长度为0
	 *         The decoded array of bytes. May be of length 0.
	 */
	public static byte[] decodeFast(char[] sArr) {
		// 检查特殊情况 Check special case
		int sLen = sArr.length;
		if (sLen == 0) {
			return new byte[0];
		}
		
		// 起始和结束索引 Start and end index after trimming
		int sIx = 0, eIx = sLen - 1;

		// 去除开头的非法字符 Trim illegal chars from start
		while (sIx < eIx && IA[sArr[sIx]] < 0) {
			sIx++;
		}

		// 去除结尾的非法字符 Trim illegal chars from end
		while (eIx > 0 && IA[sArr[eIx]] < 0) {
			eIx--;
		}

		// 获取填充字符'='的数量 Get the padding count (=) (0, 1 or 2)
		int pad = sArr[eIx] == '=' ? (sArr[eIx - 1] == '=' ? 2 : 1) : 0;
		
		// 有效内容长度 Content count including possible separators
		int cCnt = eIx - sIx + 1;
		
		// 分隔符数量 Separator count
		int sepCnt = sLen > 76 ? (sArr[76] == '\r' ? cCnt / 78 : 0) << 1 : 0;

		// 解码后的字节长度 The number of decoded bytes
		int len = ((cCnt - sepCnt) * 6 >> 3) - pad;
		
		// 预分配字节数组 Preallocate byte[] of exact length
		byte[] dArr = new byte[len];

		// 解码除最后0-2字节外的所有数据 Decode all but the last 0 - 2 bytes
		int d = 0;
		for (int cc = 0, eLen = (len / 3) * 3; d < eLen;) {
			// 将四个有效字符组装成一个int Assemble three bytes into an int from four "valid" characters
			int i = IA[sArr[sIx++]] << 18 
				| IA[sArr[sIx++]] << 12 
				| IA[sArr[sIx++]] << 6 
				| IA[sArr[sIx++]];

			// 添加字节 Add the bytes
			dArr[d++] = (byte) (i >> 16);
			dArr[d++] = (byte) (i >> 8);
			dArr[d++] = (byte) i;

			// 如果遇到行分隔符，跳过它 If line separator, jump over it
			if (sepCnt > 0 && ++cc == 19) {
				sIx += 2;
				cc = 0;
			}
		}

		// 解码最后1-3字节（包括'='） Decode last 1-3 bytes (incl '=') into 1-3 bytes
		if (d < len) {
			int i = 0;
			for (int j = 0; sIx <= eIx - pad; j++) {
				i |= IA[sArr[sIx++]] << (18 - j * 6);
			}

			for (int r = 16; d < len; r -= 8) {
				dArr[d++] = (byte) (i >> r);
			}
		}

		return dArr;
	}
	
	/**
	 * 将原始字节数组编码为BASE64字节数组
	 * Encodes a raw byte array into a BASE64 byte[] representation in accordance with RFC 2045.
	 *
	 * @param sArr 要转换的字节数组。如果为null或长度为0，将返回空数组
	 *            The bytes to convert. If null or length 0 an empty array will be returned.
	 * @param lineSep 是否在76个字符后添加"\r\n"换行符
	 *               Optional "\r\n" after 76 characters, unless end of file.
	 * @return BASE64编码后的字节数组，永不为null
	 *         A BASE64 encoded array. Never null.
	 */
	public static byte[] encodeToByte(byte[] sArr, boolean lineSep) {
		// 检查特殊情况 Check special case
		int sLen = sArr != null ? sArr.length : 0;
		if (sLen == 0) {
			return new byte[0];
		}
		
		// 计算结果数组长度 Calculate result array length
		int eLen = (sLen / 3) * 3; // 完整的24位数据块数量 Full 24-bit chunks
		int cCnt = ((sLen - 1) / 3 + 1) << 2; // Base64字符数量 Base64 character count
		int dLen = cCnt + (lineSep ? (cCnt - 1) / 76 << 1 : 0); // 包含换行符的总长度 Total length including line separators
		byte[] dArr = new byte[dLen]; // 预分配结果数组 Preallocate result array
		
		// 编码24位数据块 Encode even 24-bits
		for (int s = 0, d = 0, cc = 0; s < eLen;) {
			// 将三个字节复制到int的低24位 Copy next three bytes into lower 24 bits of int
			int i = (sArr[s++] & 0xff) << 16 | (sArr[s++] & 0xff) << 8 | (sArr[s++] & 0xff);
			
			// 将int编码为四个Base64字符 Encode the int into four Base64 chars
			dArr[d++] = (byte) CA[(i >>> 18) & 0x3f];
			dArr[d++] = (byte) CA[(i >>> 12) & 0x3f];
			dArr[d++] = (byte) CA[(i >>> 6) & 0x3f];
			dArr[d++] = (byte) CA[i & 0x3f];
			
			// 添加可选的换行符 Add optional line separator
			if (lineSep && ++cc == 19 && d < dLen - 2) {
				dArr[d++] = '\r';
				dArr[d++] = '\n';
				cc = 0;
			}
		}
		
		// 处理剩余字节 Pad and encode last bits if source isn't an even 24 bits
		int left = sLen - eLen; // 剩余字节数 0 - 2
		if (left > 0) {
			// 准备最后的int Prepare the last int
			int i = ((sArr[eLen] & 0xff) << 10) | (left == 2 ? ((sArr[sLen - 1] & 0xff) << 2) : 0);
			
			// 设置最后四个字符 Set last four chars
			dArr[dLen - 4] = (byte) CA[i >> 12];
			dArr[dLen - 3] = (byte) CA[(i >>> 6) & 0x3f];
			dArr[dLen - 2] = left == 2 ? (byte) CA[i & 0x3f] : (byte) '=';
			dArr[dLen - 1] = '=';
		}
		return dArr;
	}
	
	/**
	 * 解码BASE64编码的字节数组，忽略所有非法字符，可以处理包含或不包含换行符的数组
	 * Decodes a BASE64 encoded byte array. All illegal characters will be ignored and can handle both arrays with and without line separators.
	 *
	 * @param sArr 源字节数组，如果为null会抛出异常
	 *             The source array. Length 0 will return an empty array. <code>null</code> will throw an exception.
	 * @return 解码后的字节数组，可能长度为0。如果合法字符（包括'='）不能被4整除，则返回null
	 *         The decoded array of bytes. May be of length 0. Will be null if the legal characters (including '=') isn't divideable by 4.
	 */
	public static byte[] decode(byte[] sArr) {
		// 检查特殊情况 Check special case
		int sLen = sArr.length;
		
		// 统计非法字符数量（包括'\r', '\n'）以确定返回数组的大小
		// Count illegal characters (including '\r', '\n') to know what size the returned array will be
		int sepCnt = 0; // 分隔符数量 Number of separator characters
		for (int i = 0; i < sLen; i++) {
			if (IA[sArr[i] & 0xff] < 0) {
				sepCnt++;
			}
		}
		
		// 检查合法字符（包括'='）是否能被4整除，符合RFC 2045规范
		// Check if legal chars (including '=') are evenly divisible by 4 as specified in RFC 2045
		if ((sLen - sepCnt) % 4 != 0) {
			return null;
		}
		
		// 统计结尾'='的数量 Count '=' at end
		int pad = 0;
		for (int i = sLen; i > 1 && IA[sArr[--i] & 0xff] <= 0;) {
			if (sArr[i] == '=') {
				pad++;
			}
		}
		
		// 计算解码后的字节长度 Calculate decoded byte length
		int len = ((sLen - sepCnt) * 6 >> 3) - pad;
		
		// 预分配字节数组 Preallocate byte[] of exact length
		byte[] dArr = new byte[len];
		
		// 解码过程 Decoding process
		for (int s = 0, d = 0; d < len;) {
			// 将四个有效字符组装成一个int Assemble three bytes into an int from four "valid" characters
			int i = 0;
			for (int j = 0; j < 4; j++) {
				int c = IA[sArr[s++] & 0xff];
				if (c >= 0) {
					i |= c << (18 - j * 6);
				} else {
					j--; // 跳过非法字符 Skip illegal character
				}
			}
			
			// 添加字节 Add the bytes
			dArr[d++] = (byte) (i >> 16);
			if (d < len) {
				dArr[d++] = (byte) (i >> 8);
				if (d < len) {
					dArr[d++] = (byte) i;
				}
			}
		}
		
		return dArr;
	}
	
	/**
	 * 快速解码BASE64编码的字节数组，该方法比{@link #decode(byte[])}快约两倍
	 * Decodes a BASE64 encoded byte array that is known to be reasonably well formatted. 
	 * The method is about twice as fast as {@link #decode(byte[])}.
	 *
	 * 前提条件 Preconditions:
	 * + 数组每行长度必须为76个字符或没有行分隔符（单行）
	 *   The array must have a line length of 76 chars OR no line separators at all (one line).
	 * + 行分隔符必须是"\r\n"，符合RFC 2045规范
	 *   Line separator must be "\r\n", as specified in RFC 2045
	 * + 数组在编码字符串内不能包含非法字符
	 *   The array must not contain illegal characters within the encoded string
	 * + 数组开头和结尾可以包含非法字符，这些字符会被适当处理
	 *   The array CAN have illegal characters at the beginning and end, those will be dealt with appropriately.
	 *
	 * @param sArr 源字节数组，如果为null会抛出异常
	 *             The source array. Length 0 will return an empty array. <code>null</code> will throw an exception.
	 * @return 解码后的字节数组，可能长度为0
	 *         The decoded array of bytes. May be of length 0.
	 */
	public static byte[] decodeFast(byte[] sArr) {
		// 检查特殊情况 Check special case
		int sLen = sArr.length;
		if (sLen == 0) {
			return new byte[0];
		}
		
		// 起始和结束索引 Start and end index after trimming
		int sIx = 0, eIx = sLen - 1;

		// 去除开头的非法字符 Trim illegal chars from start
		while (sIx < eIx && IA[sArr[sIx] & 0xff] < 0) {
			sIx++;
		}

		// 去除结尾的非法字符 Trim illegal chars from end
		while (eIx > 0 && IA[sArr[eIx] & 0xff] < 0) {
			eIx--;
		}

		// 获取填充字符'='的数量 Get the padding count (=) (0, 1 or 2)
		int pad = sArr[eIx] == '=' ? (sArr[eIx - 1] == '=' ? 2 : 1) : 0;
		
		// 有效内容长度 Content count including possible separators
		int cCnt = eIx - sIx + 1;
		
		// 分隔符数量 Separator count
		int sepCnt = sLen > 76 ? (sArr[76] == '\r' ? cCnt / 78 : 0) << 1 : 0;

		// 解码后的字节长度 The number of decoded bytes
		int len = ((cCnt - sepCnt) * 6 >> 3) - pad;
		
		// 预分配字节数组 Preallocate byte[] of exact length
		byte[] dArr = new byte[len];

		// 解码除最后0-2字节外的所有数据 Decode all but the last 0 - 2 bytes
		int d = 0;
		for (int cc = 0, eLen = (len / 3) * 3; d < eLen;) {
			// 将四个有效字符组装成一个int Assemble three bytes into an int from four "valid" characters
			int i = IA[sArr[sIx++]] << 18 
				| IA[sArr[sIx++]] << 12 
				| IA[sArr[sIx++]] << 6 
				| IA[sArr[sIx++]];

			// 添加字节 Add the bytes
			dArr[d++] = (byte) (i >> 16);
			dArr[d++] = (byte) (i >> 8);
			dArr[d++] = (byte) i;

			// 如果遇到行分隔符，跳过它 If line separator, jump over it
			if (sepCnt > 0 && ++cc == 19) {
				sIx += 2;
				cc = 0;
			}
		}

		// 解码最后1-3字节（包括'='） Decode last 1-3 bytes (incl '=') into 1-3 bytes
		if (d < len) {
			int i = 0;
			for (int j = 0; sIx <= eIx - pad; j++) {
				i |= IA[sArr[sIx++]] << (18 - j * 6);
			}

			for (int r = 16; d < len; r -= 8) {
				dArr[d++] = (byte) (i >> r);
			}
		}

		return dArr;
	}
	
	/**
	 * 将原始字节数组编码为BASE64字符串
	 * Encodes a raw byte array into a BASE64 String representation in accordance with RFC 2045.
	 *
	 * @param sArr 要转换的字节数组。如果为null或长度为0，将返回空数组
	 *            The bytes to convert. If null or length 0 an empty array will be returned.
	 * @param lineSep 是否在76个字符后添加"\r\n"换行符
	 *               Optional "\r\n" after 76 characters, unless end of file.
	 * @return BASE64编码后的字符串，永不为null
	 *         A BASE64 encoded array. Never null.
	 */
	public static String encodeToString(byte[] sArr, boolean lineSep) {
		// 重用char[]数组，因为无法增量创建String，且StringBuffer/Builder会更慢
		// Reuse char[] since we can't create a String incrementally anyway and
		// StringBuffer/Builder would be slower.
		return new String(encodeToChar(sArr, lineSep));
	}
	
	/**
	 * 解码BASE64编码的字符串，忽略所有非法字符，可以处理包含或不包含换行符的字符串
	 * Decodes a BASE64 encoded String. All illegal characters will be ignored and can handle both strings with and without line separators.
	 *
	 * 注意：调用decode(str.toCharArray())可能快约2倍，但会创建临时数组。此版本使用str.charAt(i)遍历字符串
	 * Note! It can be up to about 2x the speed to call decode(str.toCharArray()) instead. That will create a temporary array though. 
	 * This version will use str.charAt(i) to iterate the string.
	 *
	 * @param str 源字符串，如果为null或长度为0将返回空数组
	 *            The source string. null or length 0 will return an empty array.
	 * @return 解码后的字节数组，可能长度为0。如果合法字符（包括'='）不能被4整除，则返回null（即肯定已损坏）
	 *         The decoded array of bytes. May be of length 0. Will be null if the legal characters (including '=') isn't divideable by 4. 
	 *         (I.e. definitely corrupted).
	 */
	public static byte[] decode(String str) {
		// 检查特殊情况 Check special case
		int sLen = str != null ? str.length() : 0;
		if (sLen == 0) {
			return new byte[0];
		}
		
		// 统计非法字符数量（包括'\r', '\n'）以确定返回数组的大小
		// Count illegal characters (including '\r', '\n') to know what size the returned array will be
		int sepCnt = 0; // 分隔符数量 Number of separator characters
		for (int i = 0; i < sLen; i++) {
			if (IA[str.charAt(i)] < 0) {
				sepCnt++;
			}
		}
		
		// 检查合法字符（包括'='）是否能被4整除，符合RFC 2045规范
		// Check if legal chars (including '=') are evenly divisible by 4 as specified in RFC 2045
		if ((sLen - sepCnt) % 4 != 0) {
			return null;
		}
		
		// 统计结尾'='的数量 Count '=' at end
		int pad = 0;
		for (int i = sLen; i > 1 && IA[str.charAt(--i)] <= 0;) {
			if (str.charAt(i) == '=') {
				pad++;
			}
		}
		
		// 计算解码后的字节长度 Calculate decoded byte length
		int len = ((sLen - sepCnt) * 6 >> 3) - pad;
		
		// 预分配字节数组 Preallocate byte[] of exact length
		byte[] dArr = new byte[len];
		
		// 解码过程 Decoding process
		for (int s = 0, d = 0; d < len;) {
			// 将四个有效字符组装成一个int Assemble three bytes into an int from four "valid" characters
			int i = 0;
			for (int j = 0; j < 4; j++) {
				int c = IA[str.charAt(s++)];
				if (c >= 0) {
					i |= c << (18 - j * 6);
				} else {
					j--; // 跳过非法字符 Skip illegal character
				}
			}
			
			// 添加字节 Add the bytes
			dArr[d++] = (byte) (i >> 16);
			if (d < len) {
				dArr[d++] = (byte) (i >> 8);
				if (d < len) {
					dArr[d++] = (byte) i;
				}
			}
		}
		
		return dArr;
	}
	
	/**
	 * 快速解码BASE64编码的字符串，该方法比{@link #decode(String)}快约两倍
	 * Decodes a BASE64 encoded string that is known to be reasonably well formatted. 
	 * The method is about twice as fast as {@link #decode(String)}.
	 *
	 * 前提条件 Preconditions:
	 * + 数组每行长度必须为76个字符或没有行分隔符（单行）
	 *   The array must have a line length of 76 chars OR no line separators at all (one line).
	 * + 行分隔符必须是"\r\n"，符合RFC 2045规范
	 *   Line separator must be "\r\n", as specified in RFC 2045
	 * + 数组在编码字符串内不能包含非法字符
	 *   The array must not contain illegal characters within the encoded string
	 * + 数组开头和结尾可以包含非法字符，这些字符会被适当处理
	 *   The array CAN have illegal characters at the beginning and end, those will be dealt with appropriately.
	 *
	 * @param s 源字符串，如果为null会抛出异常
	 *          The source string. Length 0 will return an empty array. <code>null</code> will throw an exception.
	 * @return 解码后的字节数组，可能长度为0
	 *         The decoded array of bytes. May be of length 0.
	 */
	public static byte[] decodeFast(String s) {
		// 检查特殊情况 Check special case
		int sLen = s.length();
		if (sLen == 0) {
			return new byte[0];
		}

		// 起始和结束索引 Start and end index after trimming
		int sIx = 0, eIx = sLen - 1;

		// 去除开头的非法字符 Trim illegal chars from start
		while (sIx < eIx && IA[s.charAt(sIx) & 0xff] < 0) {
			sIx++;
		}

		// 去除结尾的非法字符 Trim illegal chars from end
		while (eIx > 0 && IA[s.charAt(eIx) & 0xff] < 0) {
			eIx--;
		}

		// 获取填充字符'='的数量 Get the padding count (=) (0, 1 or 2)
		int pad = s.charAt(eIx) == '=' ? (s.charAt(eIx - 1) == '=' ? 2 : 1) : 0;
		
		// 有效内容长度 Content count including possible separators
		int cCnt = eIx - sIx + 1;
		
		// 分隔符数量 Separator count
		int sepCnt = sLen > 76 ? (s.charAt(76) == '\r' ? cCnt / 78 : 0) << 1 : 0;

		// 解码后的字节长度 The number of decoded bytes
		int len = ((cCnt - sepCnt) * 6 >> 3) - pad;
		
		// 预分配字节数组 Preallocate byte[] of exact length
		byte[] dArr = new byte[len];

		// 解码除最后0-2字节外的所有数据 Decode all but the last 0 - 2 bytes
		int d = 0;
		for (int cc = 0, eLen = (len / 3) * 3; d < eLen;) {
			// 将四个有效字符组装成一个int Assemble three bytes into an int from four "valid" characters
			int i = IA[s.charAt(sIx++)] << 18 
				| IA[s.charAt(sIx++)] << 12 
				| IA[s.charAt(sIx++)] << 6 
				| IA[s.charAt(sIx++)];

			// 添加字节 Add the bytes
			dArr[d++] = (byte) (i >> 16);
			dArr[d++] = (byte) (i >> 8);
			dArr[d++] = (byte) i;

			// 如果遇到行分隔符，跳过它 If line separator, jump over it
			if (sepCnt > 0 && ++cc == 19) {
				sIx += 2;
				cc = 0;
			}
		}

		// 解码最后1-3字节（包括'='） Decode last 1-3 bytes (incl '=') into 1-3 bytes
		if (d < len) {
			int i = 0;
			for (int j = 0; sIx <= eIx - pad; j++) {
				i |= IA[s.charAt(sIx++)] << (18 - j * 6);
			}

			for (int r = 16; d < len; r -= 8) {
				dArr[d++] = (byte) (i >> r);
			}
		}

		return dArr;
	}
}
