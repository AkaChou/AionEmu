package com.aionl.slf4j.filters;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * GM管理员命令日志过滤器
 * GM Admin command log filter that filters admin command messages
 */
public class GmAuditFilter extends Filter<ILoggingEvent> {

    /**
     * 决定是否接受或拒绝日志事件
     * Decides whether to accept or deny a logging event
     *
     * @param loggingEvent 待处理的日志事件 (The logging event to be processed)
     * @return FilterReply.ACCEPT 如果消息以[ADMIN COMMAND]开头；FilterReply.DENY 如果不是
     *         (FilterReply.ACCEPT if message starts with [ADMIN COMMAND]; FilterReply.DENY if not)
     */
    public FilterReply decide(ILoggingEvent loggingEvent) {
        Object message = loggingEvent.getMessage();
        return ((String)message).startsWith("[ADMIN COMMAND]") ? FilterReply.ACCEPT : FilterReply.DENY;
    }
}
