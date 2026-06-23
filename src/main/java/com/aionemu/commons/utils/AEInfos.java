package com.aionemu.commons.utils;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 系统信息工具类，用于获取和打印系统相关信息
 * System information utility class for retrieving and printing system-related information
 */
public class AEInfos {
    private static final Logger log = LoggerFactory.getLogger(AEInfos.class);

    /**
     * 获取内存使用信息
     * Get memory usage information
     * 
     * @return 包含内存使用详情的字符串数组 / String array containing memory usage details
     */
    public static String[] getMemoryInfo() {
        double max = (double)(Runtime.getRuntime().maxMemory() / 1024L);
        double allocated = (double)(Runtime.getRuntime().totalMemory() / 1024L);
        double nonAllocated = max - allocated;
        double cached = (double)(Runtime.getRuntime().freeMemory() / 1024L);
        double used = allocated - cached;
        double useable = max - used;
        DecimalFormat df = new DecimalFormat(" (0.0000'%')");
        DecimalFormat df2 = new DecimalFormat(" # 'KB'");
        return new String[]{
            "+----",
            "| Global Memory Informations at " + getRealTime().toString() + ":",
            "|    |",
            "| Allowed Memory:" + df2.format(max),
            "|    |= Allocated Memory:" + df2.format(allocated) + df.format(allocated / max * 100.0D),
            "|    |= Non-Allocated Memory:" + df2.format(nonAllocated) + df.format(nonAllocated / max * 100.0D),
            "| Allocated Memory:" + df2.format(allocated),
            "|    |= Used Memory:" + df2.format(used) + df.format(used / max * 100.0D),
            "|    |= Unused (cached) Memory:" + df2.format(cached) + df.format(cached / max * 100.0D),
            "| Useable Memory:" + df2.format(useable) + df.format(useable / max * 100.0D),
            "+----"
        };
    }

    /**
     * 获取CPU信息
     * Get CPU information
     * 
     * @return 包含CPU信息的字符串数组 / String array containing CPU information
     */
    public static String[] getCPUInfo() {
        return new String[]{
            "Available CPU(s): " + Runtime.getRuntime().availableProcessors(),
            "Processor(s) Identifier: " + System.getenv("PROCESSOR_IDENTIFIER"),
            "..................................................",
            ".................................................."
        };
    }

    /**
     * 获取操作系统信息
     * Get operating system information
     * 
     * @return 包含操作系统信息的字符串数组 / String array containing OS information
     */
    public static String[] getOSInfo() {
        return new String[]{
            "OS: " + System.getProperty("os.name") + " Build: " + System.getProperty("os.version"),
            "OS Arch: " + System.getProperty("os.arch"),
            "..................................................",
            ".................................................."
        };
    }

    /**
     * 获取JRE信息
     * Get JRE information
     * 
     * @return 包含JRE信息的字符串数组 / String array containing JRE information
     */
    public static String[] getJREInfo() {
        return new String[]{
            "Java Platform Information",
            "Java Runtime Name: " + System.getProperty("java.runtime.name"),
            "Java Version: " + System.getProperty("java.version"),
            "Java Class Version: " + System.getProperty("java.class.version"),
            "..................................................",
            ".................................................."
        };
    }

    /**
     * 获取JVM信息
     * Get JVM information
     * 
     * @return 包含JVM信息的字符串数组 / String array containing JVM information
     */
    public static String[] getJVMInfo() {
        return new String[]{
            "Virtual Machine Information (JVM)",
            "JVM Name: " + System.getProperty("java.vm.name"),
            "JVM Installation Directory: " + System.getProperty("java.home"),
            "JVM Version: " + System.getProperty("java.vm.version"),
            "JVM Vendor: " + System.getProperty("java.vm.vendor"),
            "JVM Info: " + System.getProperty("java.vm.info"),
            "..................................................",
            ".................................................."
        };
    }

    /**
     * 获取当前时间
     * Get current time
     * 
     * @return 格式化的当前时间字符串 / Formatted current time string
     */
    public static String getRealTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("H:mm:ss");
        return formatter.format(new Date());
    }

    /**
     * 打印内存信息
     * Print memory information
     */
    public static void printMemoryInfo() {
        for (String line : getMemoryInfo()) {
            log.info(line);
        }
    }

    /**
     * 打印CPU信息
     * Print CPU information
     */
    public static void printCPUInfo() {
        for (String line : getCPUInfo()) {
            log.info(line);
        }
    }

    /**
     * 打印操作系统信息
     * Print operating system information
     */
    public static void printOSInfo() {
        for (String line : getOSInfo()) {
            log.info(line);
        }
    }

    /**
     * 打印JRE信息
     * Print JRE information
     */
    public static void printJREInfo() {
        for (String line : getJREInfo()) {
            log.info(line);
        }
    }

    /**
     * 打印JVM信息
     * Print JVM information
     */
    public static void printJVMInfo() {
        for (String line : getJVMInfo()) {
            log.info(line);
        }
    }

    /**
     * 打印当前时间
     * Print current time
     */
    public static void printRealTime() {
        log.info(getRealTime());
    }

    /**
     * 打印所有系统信息
     * Print all system information
     */
    public static void printAllInfos() {
        printOSInfo();
        printCPUInfo();
        printJREInfo();
        printJVMInfo();
        printMemoryInfo();
    }
}
