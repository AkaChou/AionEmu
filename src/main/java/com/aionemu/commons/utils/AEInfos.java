package com.aionemu.commons.utils;

import com.aionemu.boot.i18n.I18n;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * 运行时环境信息采集与打印（OS / CPU / JRE / JVM / 内存）。
 * Runtime environment info collection and printing (OS / CPU / JRE / JVM / memory).
 */
@Slf4j
@UtilityClass
public class AEInfos {

    /**
     * 构建内存信息行。
     * Build memory info lines.
     *
     * @return 本地化内存信息 / Localized memory lines
     */
    public String[] getMemoryInfo() {
        double max = (double) (Runtime.getRuntime().maxMemory() / 1024L);
        double allocated = (double) (Runtime.getRuntime().totalMemory() / 1024L);
        double nonAllocated = max - allocated;
        double cached = (double) (Runtime.getRuntime().freeMemory() / 1024L);
        double used = allocated - cached;
        double useable = max - used;
        DecimalFormat valueFormat = new DecimalFormat("0");
        DecimalFormat percentFormat = new DecimalFormat("0.0000");
        return new String[] {
            I18n.get("system.info.memory.at", getRealTime()),
            I18n.get("system.info.memory.allowed", valueFormat.format(max)),
            I18n.get("system.info.memory.allocated", valueFormat.format(allocated), percentFormat.format(allocated / max * 100.0D)),
            I18n.get("system.info.memory.non_allocated", valueFormat.format(nonAllocated), percentFormat.format(nonAllocated / max * 100.0D)),
            I18n.get("system.info.memory.used", valueFormat.format(used), percentFormat.format(used / max * 100.0D)),
            I18n.get("system.info.memory.cached", valueFormat.format(cached), percentFormat.format(cached / max * 100.0D)),
            I18n.get("system.info.memory.usable", valueFormat.format(useable), percentFormat.format(useable / max * 100.0D))
        };
    }

    /**
     * 构建 CPU 信息行。
     * Build CPU info lines.
     *
     * @return 本地化 CPU 信息 / Localized CPU lines
     */
    public String[] getCPUInfo() {
        return new String[] {
            I18n.get("system.info.cpu.available", Runtime.getRuntime().availableProcessors()),
            I18n.get("system.info.cpu.identifier", System.getenv("PROCESSOR_IDENTIFIER"))
        };
    }

    /**
     * 构建操作系统信息行。
     * Build OS info lines.
     *
     * @return 本地化 OS 信息 / Localized OS lines
     */
    public String[] getOSInfo() {
        return new String[] {
            I18n.get("system.info.os.name", System.getProperty("os.name"), System.getProperty("os.version")),
            I18n.get("system.info.os.arch", System.getProperty("os.arch"))
        };
    }

    /**
     * 构建 JRE 信息行。
     * Build JRE info lines.
     *
     * @return 本地化 JRE 信息 / Localized JRE lines
     */
    public String[] getJREInfo() {
        return new String[] {
            I18n.get("system.info.jre.runtime", System.getProperty("java.runtime.name")),
            I18n.get("system.info.jre.version", System.getProperty("java.version")),
            I18n.get("system.info.jre.class_version", System.getProperty("java.class.version"))
        };
    }

    /**
     * 构建 JVM 信息行。
     * Build JVM info lines.
     *
     * @return 本地化 JVM 信息 / Localized JVM lines
     */
    public String[] getJVMInfo() {
        return new String[] {
            I18n.get("system.info.jvm.name", System.getProperty("java.vm.name")),
            I18n.get("system.info.jvm.home", System.getProperty("java.home")),
            I18n.get("system.info.jvm.version", System.getProperty("java.vm.version")),
            I18n.get("system.info.jvm.vendor", System.getProperty("java.vm.vendor")),
            I18n.get("system.info.jvm.mode", System.getProperty("java.vm.info"))
        };
    }

    /**
     * 返回当前时间字符串 {@code H:mm:ss}。
     * Return current time as {@code H:mm:ss}.
     *
     * @return 时间字符串 / Time string
     */
    public String getRealTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("H:mm:ss");
        return formatter.format(new Date());
    }

    /**
     * 打印内存信息。
     * Print memory info.
     */
    public void printMemoryInfo() {
        for (String line : getMemoryInfo()) {
            log.info(line);
        }
    }

    /**
     * 打印 CPU 信息。
     * Print CPU info.
     */
    public void printCPUInfo() {
        for (String line : getCPUInfo()) {
            log.info(line);
        }
    }

    /**
     * 打印操作系统信息。
     * Print OS info.
     */
    public void printOSInfo() {
        for (String line : getOSInfo()) {
            log.info(line);
        }
    }

    /**
     * 打印 JRE 信息。
     * Print JRE info.
     */
    public void printJREInfo() {
        for (String line : getJREInfo()) {
            log.info(line);
        }
    }

    /**
     * 打印 JVM 信息。
     * Print JVM info.
     */
    public void printJVMInfo() {
        for (String line : getJVMInfo()) {
            log.info(line);
        }
    }

    /**
     * 打印当前时间。
     * Print current time.
     */
    public void printRealTime() {
        log.info(getRealTime());
    }

    /**
     * 分段打印全部运行时信息。
     * Print all runtime info by section.
     */
    public void printAllInfos() {
        printSectioned(I18n.get("system.info.section.os"), getOSInfo());
        printSectioned(I18n.get("system.info.section.cpu"), getCPUInfo());
        printSectioned(I18n.get("system.info.section.jre"), getJREInfo());
        printSectioned(I18n.get("system.info.section.jvm"), getJVMInfo());
        printSectioned(I18n.get("system.info.section.memory"), getMemoryInfo());
    }

    /**
     * 按章节标题打印信息行。
     * Print lines under a section title.
     *
     * @param title 章节标题 / Section title
     * @param lines 信息行数组 / Info lines
     */
    private void printSectioned(String title, String[] lines) {
        PrintUtils.printSection(title);
        for (String line : lines) {
            log.info(line);
        }
    }
}
