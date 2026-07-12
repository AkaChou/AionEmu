package com.aionemu.commons.network;

import java.util.Arrays;

/**
 * IPv4 地址范围，支持范围判定与字节/字符串互转。
 * IPv4 address range with membership checks and byte/string conversions.
 */
public class IPRange {

    /**
     * 范围最小值（数值形式）。
     * Minimum value of the range (numeric form).
     */
    private final long min;

    /**
     * 范围最大值（数值形式）。
     * Maximum value of the range (numeric form).
     */
    private final long max;

    /**
     * 映射目标地址字节。
     * Mapped target address bytes.
     */
    private final byte[] address;

    /**
     * 使用字符串构造 IP 范围。
     * Construct IP range from strings.
     *
     * Minimum IP
     * Maximum IP
     * Target IP
     */
    public IPRange(String min, String max, String address) {
        this.min = toLong("min", toByteArray(min));
        this.max = toLong("max", toByteArray(max));
        this.address = toByteArray(address);
    }

    /**
     * 使用字节数组构造 IP 范围。
     * Construct IP range from byte arrays.
     *
     * @param min 最小 IP 字节 / Minimum IP bytes
     * @param max 最大 IP 字节 / Maximum IP bytes
     * Target IP bytes
     */
    public IPRange(byte[] min, byte[] max, byte[] address) {
        requireIpv4Bytes("address", address);
        this.min = toLong("min", min);
        this.max = toLong("max", max);
        this.address = address;
    }

    /**
     * 检查指定 IP 是否在范围内。
     * Check whether the given IP is within range.
     *
     * IP to check
     *
     * @param address
     * @return 是否在范围内 / Whether in range
     */
    public boolean isInRange(String address) {
        long addr = toLong("address", toByteArray(address));
        return addr >= this.min && addr <= this.max;
    }

    /**
     * 获取目标 IP 字节。
     * Get target IP bytes.
     *
     * IP byte array
     */
    public byte[] getAddress() {
        return this.address;
    }

    /**
     * 获取最小 IP 字节。
     * Get minimum IP bytes.
     *
     * Minimum IP bytes
     */
    public byte[] getMinAsByteArray() {
        return toBytes(this.min);
    }

    /**
     * 获取最大 IP 字节。
     * Get maximum IP bytes.
     *
     * Maximum IP bytes
     */
    public byte[] getMaxAsByteArray() {
        return toBytes(this.max);
    }

    /**
     * 将 IPv4 字节转为无符号长整型。
     * Convert IPv4 bytes to unsigned long.
     *
     * @param field 字段名（校验报错用） / Field name for validation errors
     * 4-byte IP
     * @return 数值形式地址 / Numeric address
     */
    private static long toLong(String field, byte[] bytes) {
        requireIpv4Bytes(field, bytes);
        long result = 0L;
        result |= (bytes[3] & 0xFF);
        result |= ((bytes[2] & 0xFF) << 8);
        result |= ((bytes[1] & 0xFF) << 16);
        result |= ((long) (bytes[0] & 0xFF) << 24);
        return result & 0xFFFFFFFFL;
    }

    /**
     * 校验 IPv4 字节长度。
     * Validate IPv4 byte length.
     *
     * Field name
     * @param bytes 字节数组 / Byte array
     */
    private static void requireIpv4Bytes(String field, byte[] bytes) {
        if (bytes == null || bytes.length != 4) {
            throw new IllegalArgumentException("IPRange " + field + " must be 4 bytes, got "
                + (bytes == null ? "null" : bytes.length));
        }
    }

    /**
     * 将长整型地址转为 4 字节。
     * Convert long address to 4 bytes.
     *
     * @param val 数值地址 / Numeric address
     * Byte array
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
     * 将点分 IPv4 字符串转为字节数组。
     * Convert dotted IPv4 string to byte array.
     *
     * IP string
     * Byte array
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
        return max == ipRange.max
            && min == ipRange.min
            && Arrays.equals(address, ipRange.address);
    }

    @Override
    public int hashCode() {
        int result = (int) (min ^ (min >>> 32));
        result = 31 * result + (int) (max ^ (max >>> 32));
        result = 31 * result + Arrays.hashCode(address);
        return result;
    }
}
