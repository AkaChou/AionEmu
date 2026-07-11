package com.aionemu.gameserver.utils;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.utils.AEInfos;
import com.aionemu.commons.versionning.Version;
import com.aionemu.gameserver.GameServer;
import lombok.extern.slf4j.Slf4j;

/**
 * 游戏服与公共库版本信息工具。
 * Utility for gameserver and commons version information.
 */
@Slf4j
public class AEVersions {

    private static final Version commons = new Version(AEInfos.class);
    private static final Version gameserver = new Version(GameServer.class);

    /**
     * 格式化修订号信息。
     * Formats revision information.
     *
     * Version object
     *
     * @param version @return 格式化后的修订号 / Formatted revision string
     */
    private static String getRevisionInfo(Version version) {
        return String.format("%-6s", version.getRevision());
    }

    /**
     * 格式化分支信息。
     * Formats branch information.
     *
     * Version object
     *
     * @param version @return 格式化后的分支名 / Formatted branch string
     */
    private static String getBranchInfo(Version version) {
        return String.format("%-6s", version.getBranch());
    }

    /**
     * 格式化分支提交时间信息。
     * Formats branch commit time information.
     *
     * Version object
     *
     * @param version @return 格式化后的提交时间 / Formatted commit time string
     */
    private static String getBranchCommitTimeInfo(Version version) {
        return String.format("%-6s", version.getCommitTime());
    }

    /**
     * 格式化构建日期信息。
     * Formats build date information.
     *
     * Version object
     *
     * @param version @return 格式化后的构建日期 / Formatted build date string
     */
    private static String getDateInfo(Version version) {
        return String.format("[ %4s ]", version.getDate());
    }

    /**
     * 获取完整版本信息行数组。
     * Returns the full version info as an array of log lines.
     *
     * @return 版本信息行 / Version info lines
     */
    public static String[] getFullVersionInfo() {
        return new String[] {
            I18n.get("log.e151bc369cae", getRevisionInfo(commons)),
            I18n.get("log.3e9f33fc2086", getDateInfo(commons)),
            I18n.get("log.5edd8112fbb8", getRevisionInfo(gameserver)),
            I18n.get("log.80479b9805fc", getBranchInfo(gameserver)),
            I18n.get("log.2025eec46a1a", getBranchCommitTimeInfo(gameserver)),
            I18n.get("log.0a4eaad062ea", getDateInfo(gameserver))
        };
    }

    /**
     * 将完整版本信息输出到日志。
     * Prints the full version info to the log.
     */
    public static void printFullVersionInfo() {
        for (String line : getFullVersionInfo()) {
            log.info(line);
        }
    }
}
