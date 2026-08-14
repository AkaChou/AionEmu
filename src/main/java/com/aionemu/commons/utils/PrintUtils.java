package com.aionemu.commons.utils;

import java.nio.ByteBuffer;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 控制台分区打印与十六进制辅助工具。
 * Console section printer and hex helpers.
 */
@UtilityClass
public class PrintUtils {

    private static final Logger CONSOLE = LoggerFactory.getLogger("aion.console");
    private static final int RULE_WIDTH = 46;
    private static final String RULE = "─".repeat(RULE_WIDTH);

    /**
     * 打印分区标题与分隔线。
     * Print a section title with a rule line.
     *
     * @param sectionName 分区名称 / Section name
     */
    public void printSection(String sectionName) {
        String title = normalizeTitle(sectionName);
        CONSOLE.info("");
        if (!title.isEmpty()) {
            CONSOLE.info("  " + title);
        }
        CONSOLE.info("  " + RULE);
    }

    /**
     * 打印子分区标题。
     * Print a sub-section title.
     *
     * @param title 子分区标题 / Sub-section title
     */
    public void printSubSection(String title) {
        String normalized = normalizeTitle(title);
        if (normalized.isEmpty()) {
            return;
        }
        CONSOLE.info("  · " + normalized);
    }

    /**
     * 通过 aion.console 输出原始横幅行（无时间戳样式）。
     * Print a raw banner line via aion.console (no timestamp pattern).
     *
     * @param line 横幅内容 / Banner line
     */
    public void printBannerLine(String line) {
        CONSOLE.info(line == null ? "" : line);
    }

    /**
     * 规范化标题：去空白与装饰符号。
     * Normalize a title by trimming whitespace and decoration marks.
     *
     * @param raw 原始标题 / Raw title
     * @return 规范化结果 / Normalized title
     */
    public String normalizeTitle(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim().replaceAll("\\s+", " ");
        boolean changed;
        do {
            changed = false;
            String next = s;
            next = next.replaceAll("^[-\\*=\\[\\]\\s]+", "");
            next = next.replaceAll("[-\\*=\\[\\]\\s]+$", "");
            next = next.trim();
            if (!next.equals(s)) {
                changed = true;
                s = next;
            }
        } while (changed);
        return s;
    }

    /**
     * 将十六进制字符串转为字节数组（忽略空白）。
     * Convert a hex string to bytes (whitespace ignored).
     *
     * @param string 十六进制字符串 / Hex string
     * @return 字节数组 / Byte array
     */
    public byte[] hex2bytes(String string) {
        String finalString = string.replaceAll("\\s+", "");
        byte[] bytes = new byte[finalString.length() / 2];

        for (int i = 0; i < bytes.length; ++i) {
            bytes[i] = (byte) Integer.parseInt(finalString.substring(2 * i, 2 * i + 2), 16);
        }

        return bytes;
    }

    /**
     * 将字节数组转为连续十六进制字符串。
     * Convert bytes to a continuous hex string.
     *
     * @param bytes 字节数组 / Byte array
     * @return 十六进制字符串 / Hex string
     */
    public String bytes2hex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        byte[] arr$ = bytes;
        int len$ = bytes.length;

        for (int i$ = 0; i$ < len$; ++i$) {
            byte b = arr$[i$];
            int value = b & 255;
            result.append(String.format("%02X", value));
        }

        return result.toString();
    }

    /**
     * 按字节反转十六进制字符串。
     * Reverse a hex string by byte pairs.
     *
     * @param input 十六进制字符串 / Hex string
     * @return 反转后的十六进制字符串 / Reversed hex string
     */
    public String reverseHex(String input) {
        String[] chunked = new String[input.length() / 2];
        int position = 0;

        for (int i = 0; i < input.length(); i += 2) {
            chunked[position] = input.substring(position * 2, position * 2 + 2);
            ++position;
        }

        ArrayUtils.reverse(chunked);
        return StringUtils.join(chunked);
    }

    /**
     * 将 {@link ByteBuffer} 格式化为带偏移与 ASCII 侧栏的十六进制转储。
     * Format a {@link ByteBuffer} as a hex dump with offsets and ASCII side panel.
     *
     * @param data 缓冲区（读取后恢复 position） / Buffer (position restored after read)
     * @return 十六进制转储文本 / Hex dump text
     */
    public String toHex(ByteBuffer data) {
        int position = data.position();
        StringBuilder result = new StringBuilder();
        int counter = 0;

        while (data.hasRemaining()) {
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
            for (int i = 0; i < 17 - rest; ++i) {
                result.append("   ");
            }

            toText(data, result, rest);
        }

        data.position(position);
        return result.toString();
    }

    /**
     * 追加可读 ASCII 文本段。
     * Append a readable ASCII text segment.
     *
     * @param data 缓冲区 / Buffer
     * @param result 输出构建器 / Output builder
     * @param cnt 字节数 / Byte count
     */
    private void toText(ByteBuffer data, StringBuilder result, int cnt) {
        int charPos = data.position() - cnt;

        for (int a = 0; a < cnt; ++a) {
            int c = data.get(charPos++);
            if (c > 31 && c < 128) {
                result.append((char) c);
            } else {
                result.append('.');
            }
        }
    }
}
