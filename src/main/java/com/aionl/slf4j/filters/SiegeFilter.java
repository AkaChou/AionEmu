package com.aionl.slf4j.filters;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * 攻城战日志过滤器
 * Siege log filter that filters siege-related messages
 */
public class SiegeFilter extends Filter<ILoggingEvent> {

    /**
     * 决定是否接受或拒绝日志事件
     * Decides whether to accept or deny a logging event
     *
     * @param loggingEvent 待处理的日志事件 (The logging event to be processed)
     * @return FilterReply.ACCEPT 如果消息以[SIEGE]开头；FilterReply.DENY 如果不是
     *         (FilterReply.ACCEPT if message starts with [SIEGE]; FilterReply.DENY if not)
     */
    public FilterReply decide(ILoggingEvent loggingEvent) {
        Object message = loggingEvent.getMessage();
        return ((String)message).startsWith("[SIEGE]") ? FilterReply.ACCEPT : FilterReply.DENY;
    }
}
