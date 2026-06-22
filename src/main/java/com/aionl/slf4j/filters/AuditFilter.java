package com.aionl.slf4j.filters;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * 审计日志过滤器
 * Audit log filter for filtering log messages with [AUDIT] prefix
 *
 * 该过滤器用于筛选带有[AUDIT]前缀的日志消息，通常用于记录系统审计相关的日志
 * This filter is used to filter log messages with [AUDIT] prefix, typically used for system audit logging
 */
public class AuditFilter extends Filter<ILoggingEvent> {

    /**
     * 决定是否接受或拒绝日志事件
     * Decide whether to accept or deny the logging event
     *
     * @param loggingEvent 日志事件对象 / The logging event object
     * @return FilterReply.ACCEPT 如果消息以[AUDIT]开头，否则返回FilterReply.DENY
     *         FilterReply.ACCEPT if message starts with [AUDIT], otherwise FilterReply.DENY
     */
    public FilterReply decide(ILoggingEvent loggingEvent) {
        Object message = loggingEvent.getMessage();
        return ((String)message).startsWith("[AUDIT]") ? FilterReply.ACCEPT : FilterReply.DENY;
    }
}
