package com.aionemu.commons.network;

import java.util.Arrays;

/**
 * IP地址范围类
 * IP Address Range Class
 *
 * 该类用于表示和管理IP地址范围，支持IPv4地址范围检查
 * This class represents and manages IP address ranges, supporting IPv4 address range checking
 */
public class IPRange {
    
    /**
     * 范围最小值
     * Minimum value of the range
     */
    private final long min;
    
    /**
     * 范围最大值
     * Maximum value of the range
     */
    private final long max;
    
    /**
     * 地址字节数组
     * Address byte array
     */
    private final byte[] address;

    /**
     * 使用字符串构造IP范围
     * Construct IP range using strings
     *
     * @param min 最小IP地址 / Minimum IP address
     * @param max 最大IP地址 / Maximum IP address
     * @param address 目标IP地址 / Target IP address
     */
    public IPRange(String min, String max, String address) {
        this.min = toLong(toByteArray(min));
        this.max = toLong(toByteArray(max));
        this.address = toByteArray(address);
    }

    /**
     * 使用字节数组构造IP范围
     * Construct IP range using byte arrays
     *
     * @param min 最小IP地址字节数组 / Minimum IP address byte array
     * @param max 最大IP地址字节数组 / Maximum IP address byte array
     * @param address 目标IP地址字节数组 / Target IP address byte array
     */
    public IPRange(byte[] min, byte[] max, byte[] address) {
        this.min = toLong(min);
        this.max = toLong(max);
        this.address = address;
    }

    /**
     * 检查指定IP地址是否在范围内
     * Check if the specified IP address is in range
     *
     * @param address 要检查的IP地址 / IP address to check
     * @return 如果在范围内返回true / Returns true if in range
     */
    public boolean isInRange(String address) {
        long addr = toLong(toByteArray(address));
        return addr >= this.min && addr <= this.max;
    }

    /**
     * 获取目标IP地址
     * Get the target IP address
     *
     * @return IP地址字节数组 / IP address byte array
     */
    public byte[] getAddress() {
        return this.address;
    }

    /**
     * 获取最小IP地址
     * Get the minimum IP address
     *
     * @return 最小IP地址字节数组 / Minimum IP address byte array
     */
    public byte[] getMinAsByteArray() {
        return toBytes(this.min);
    }

    /**
     * 获取最大IP地址
     * Get the maximum IP address
     *
     * @return 最大IP地址字节数组 / Maximum IP address byte array
     */
    public byte[] getMaxAsByteArray() {
        return toBytes(this.max);
    }

    /**
     * 将字节数组转换为长整型
     * Convert byte array to long
     *
     * @param bytes 字节数组 / Byte array
     * @return 转换后的长整型值 / Converted long value
     */
    private static long toLong(byte[] bytes) {
        long result = 0L;
        result |= (bytes[3] & 0xFF);
        result |= ((bytes[2] & 0xFF) << 8);
        result |= ((bytes[1] & 0xFF) << 16);
        result |= ((long)(bytes[0] & 0xFF) << 24);
        return result & 0xFFFFFFFFL;
    }

    /**
     * 将长整型转换为字节数组
     * Convert long to byte array
     *
     * @param val 长整型值 / Long value
     * @return 转换后的字节数组 / Converted byte array
     */
    private static byte[] toBytes(long val) {
        return new byte[] {
            (byte) ((val >> 24) & 0xFF),
            (byte) ((val >> 16) & 0xFF),
            (byte) ((val >> 8) & 0xFF),
            (byte) (val & 0xFF)
        };
    }

    /**
     * 将IP地址字符串转换为字节数组
     * Convert IP address string to byte array
     *
     * @param address IP地址字符串 / IP address string
     * @return 转换后的字节数组 / Converted byte array
     */
    public static byte[] toByteArray(String address) {
        byte[] result = new byte[4];
        String[] strings = address.split("\\.");
        for (int i = 0; i < strings.length; i++) {
            result[i] = (byte) Integer.parseInt(strings[i]);
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IPRange)) return false;
        IPRange ipRange = (IPRange) o;
        return max == ipRange.max && 
               min == ipRange.min && 
               Arrays.equals(address, ipRange.address);
    }

    @Override
    public int hashCode() {
        int result = (int) (min ^ (min >>> 32));
        result = 31 * result + (int) (max ^ (max >>> 32));
        result = 31 * result + Arrays.hashCode(address);
        return result;
    }
}
