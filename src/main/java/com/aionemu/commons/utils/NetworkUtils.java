package com.aionemu.commons.utils;

/**
 * 网络工具类，提供IP地址匹配等网络相关功能
 * Network utility class providing IP address matching and other network-related functions
 */
public class NetworkUtils {
   /**
    * 检查IP地址是否匹配指定的模式
    * Check if an IP address matches the specified pattern
    *
    * @param pattern 匹配模式，支持*通配符和范围匹配(例如: "192.168.*.*"或"192.168.1-100.*")
    *                Pattern to match against, supports * wildcard and range matching (e.g. "192.168.*.*" or "192.168.1-100.*")
    * @param address 要检查的IP地址
    *                IP address to check
    * @return 如果IP地址匹配模式则返回true，否则返回false
    *         Returns true if the IP address matches the pattern, false otherwise
    */
   public static boolean checkIPMatching(String pattern, String address) {
      if (!pattern.equals("*.*.*.*") && !pattern.equals("*")) {
         String[] mask = pattern.split("\\.");
         String[] ip_address = address.split("\\.");

         for(int i = 0; i < mask.length; ++i) {
            if (!mask[i].equals("*") && !mask[i].equals(ip_address[i])) {
               if (!mask[i].contains("-")) {
                  return false;
               }

               byte min = Byte.parseByte(mask[i].split("-")[0]);
               byte max = Byte.parseByte(mask[i].split("-")[1]);
               byte ip = Byte.parseByte(ip_address[i]);
               if (ip < min || ip > max) {
                  return false;
               }
            }
         }

         return true;
      } else {
         return true;
      }
   }
}
