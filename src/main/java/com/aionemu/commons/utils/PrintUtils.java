package com.aionemu.commons.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * 控制台分区打印与十六进制辅助工具。
 * Console section printer and hex helpers.
 */
@UtilityClass
@Slf4j
public class PrintUtils {

    private static final int RULE_WIDTH = 46;
    private static final String RULE = "─".repeat(RULE_WIDTH);

    /**
     * 打印分区标题与分隔线。
     * Print a section title with a rule line.
     * @param sectionName 分区名称 / Section name
     */
    public void printSection(String sectionName) {
        String title = normalizeTitle(sectionName);
        log.info("");
        if (!title.isEmpty()) {
			log.info("  {}", title);
        }
		log.info("  {}", RULE);
    }

    /**
     * 打印子分区标题。
     * Print a sub-section title.
     * @param title 子分区标题 / Sub-section title
     */
    public void printSubSection(String title) {
        String normalized = normalizeTitle(title);
        if (normalized.isEmpty()) {
            return;
        }
        log.info("  · {}", normalized);
    }

    /**
     * 通过 aion.console 输出原始横幅行（无时间戳样式）。
     * Print a raw banner line via aion.console (no timestamp pattern).
     * @param line 横幅内容 / Banner line
     */
    public void printBannerLine(String line) {
        log.info(line == null ? "" : line);
    }

    /**
     * 规范化标题：去空白与装饰符号。
     * Normalize a title by trimming whitespace and decoration marks.
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
            next = next.replaceAll("^[-*=\\[\\]\\s]+", "");
            next = next.replaceAll("[-*=\\[\\]\\s]+$", "");
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
}
