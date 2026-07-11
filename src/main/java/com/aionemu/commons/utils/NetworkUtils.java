package com.aionemu.commons.utils;

import lombok.experimental.UtilityClass;

/**
 * 网络相关工具，提供 IP 模式匹配等能力。
 * Network helpers such as IP pattern matching.
 */
@UtilityClass
public class NetworkUtils {

    /**
     * 检查 IP 是否匹配模式（支持 {@code *} 与区间）。
     * Check whether an IP matches a pattern ({@code *} and ranges supported).
     *
     * @param pattern 匹配模式，如 {@code 192.168.*.*} 或 {@code 192.168.1-100.*} / Pattern, e.g. {@code 192.168.*.*} or {@code 192.168.1-100.*}
     * IP address to check
     *
     * @return 若 matched 则为 true / True if matched
     */
    public boolean checkIPMatching(String pattern, String address) {
        if (!pattern.equals("*.*.*.*") && !pattern.equals("*")) {
            String[] mask = pattern.split("\\.");
            String[] ip_address = address.split("\\.");

            for (int i = 0; i < mask.length; ++i) {
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
