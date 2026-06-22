package com.aionemu.commons.utils;

import java.nio.ByteBuffer;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

/**
 * 打印工具类，提供各种格式化输出和数据转换功能
 * Print utility class providing various formatting output and data conversion functions
 */
public class PrintUtils {
   /**
    * 打印带有分隔符的章节标题
    * Print a section title with separators
    *
    * @param sectionName 章节名称
    *                    Section name to be printed
    */
   public static void printSection(String sectionName) {
      StringBuilder sb = new StringBuilder();
      sb.append("-[ " + sectionName + " ]");

      while(sb.length() < 79) {
         sb.insert(0, "=");
      }

      System.out.println(sb.toString());
   }

   /**
    * 将十六进制字符串转换为字节数组
    * Convert hexadecimal string to byte array
    *
    * @param string 十六进制字符串
    *               Hexadecimal string to convert
    * @return 转换后的字节数组
    *         Converted byte array
    */
   public static byte[] hex2bytes(String string) {
      String finalString = string.replaceAll("\\s+", "");
      byte[] bytes = new byte[finalString.length() / 2];

      for(int i = 0; i < bytes.length; ++i) {
         bytes[i] = (byte)Integer.parseInt(finalString.substring(2 * i, 2 * i + 2), 16);
      }

      return bytes;
   }

   /**
    * 将字节数组转换为十六进制字符串
    * Convert byte array to hexadecimal string
    *
    * @param bytes 要转换的字节数组
    *              Byte array to convert
    * @return 转换后的十六进制字符串
    *         Converted hexadecimal string
    */
   public static String bytes2hex(byte[] bytes) {
      StringBuilder result = new StringBuilder();
      byte[] arr$ = bytes;
      int len$ = bytes.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         byte b = arr$[i$];
         int value = b & 255;
         result.append(String.format("%02X", value));
      }

      return result.toString();
   }

   /**
    * 反转十六进制字符串
    * Reverse a hexadecimal string
    *
    * @param input 输入的十六进制字符串
    *              Input hexadecimal string
    * @return 反转后的十六进制字符串
    *         Reversed hexadecimal string
    */
   public static String reverseHex(String input) {
      String[] chunked = new String[input.length() / 2];
      int position = 0;

      for(int i = 0; i < input.length(); i += 2) {
         chunked[position] = input.substring(position * 2, position * 2 + 2);
         ++position;
      }

      ArrayUtils.reverse(chunked);
      return StringUtils.join(chunked);
   }

   /**
    * 将ByteBuffer数据转换为带格式的十六进制字符串
    * Convert ByteBuffer data to formatted hexadecimal string
    *
    * @param data 要转换的ByteBuffer数据
    *             ByteBuffer data to convert
    * @return 格式化的十六进制字符串，包含地址、十六进制值和ASCII表示
    *         Formatted hexadecimal string with address, hex values and ASCII representation
    */
   public static String toHex(ByteBuffer data) {
      int position = data.position();
      StringBuilder result = new StringBuilder();
      int counter = 0;

      while(data.hasRemaining()) {
         if (counter % 16 == 0) {
            result.append(String.format("%04X: ", counter));
         }

         int b = data.get() & 255;
         result.append(String.format("%02X ", b));
         ++counter;
         if (counter % 16 == 0) {
            result.append("  ");
            toText(data, result, 16);
            result.append("\n");
         }
      }

      int rest = counter % 16;
      if (rest > 0) {
         for(int i = 0; i < 17 - rest; ++i) {
            result.append("   ");
         }

         toText(data, result, rest);
      }

      data.position(position);
      return result.toString();
   }

   /**
    * 将ByteBuffer数据转换为文本表示
    * Convert ByteBuffer data to text representation
    *
    * @param data ByteBuffer数据
    *             ByteBuffer data
    * @param result 结果StringBuilder
    *               Result StringBuilder
    * @param cnt 要转换的字符数
    *            Number of characters to convert
    */
   private static void toText(ByteBuffer data, StringBuilder result, int cnt) {
      int charPos = data.position() - cnt;

      for(int a = 0; a < cnt; ++a) {
         int c = data.get(charPos++);
         if (c > 31 && c < 128) {
            result.append((char)c);
         } else {
            result.append('.');
         }
      }

   }
}