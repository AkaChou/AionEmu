package com.aionemu.loginserver.utils;

import com.aionemu.commons.utils.PrintUtils;

/**
 * 登录服通用打印工具（章节标题输出）。
 * General login-server print utilities (section heading output).
 */
public class Util {

    /**
     * 打印带分隔线的章节标题。
     * Prints a section heading with separators.
     *
     * @param s 标题文本 / section title
     */
    public static void printSection(String s) {
        PrintUtils.printSection(s);
    }
}
