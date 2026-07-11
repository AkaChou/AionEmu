package com.aionemu.commons.logging;

/**
 * 共享日志消息前缀，供专用文件 Appender 与 ConsoleFilter 使用
 * Shared log message prefixes used by dedicated file appenders and ConsoleFilter
 */
public final class LogTags {

    /**
     * 聊天/消息日志前缀
     * Chat/message log prefix
     */
    public static final String MESSAGE = "[MESSAGE]";

    /**
     * 物品日志前缀
     * Item log prefix
     */
    public static final String ITEM = "[ITEM]";

    /**
     * 管理员命令日志前缀
     * Admin command log prefix
     */
    public static final String ADMIN = "[ADMIN COMMAND]";

    /**
     * 审计日志前缀
     * Audit log prefix
     */
    public static final String AUDIT = "[AUDIT]";

    private LogTags() {
    }
}
