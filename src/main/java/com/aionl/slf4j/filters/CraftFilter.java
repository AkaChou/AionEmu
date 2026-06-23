package com.aionl.slf4j.filters;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * 工艺制作日志过滤器
 * Craft log filter that filters craft-related messages
 */
public class CraftFilter extends Filter<ILoggingEvent> {

    /**
     * 决定是否接受或拒绝日志事件
     * Decides whether to accept or deny a logging event
     *
     * @param loggingEvent 待处理的日志事件 (The logging event to be processed)
     * @return FilterReply.ACCEPT 如果消息以[CRAFT]开头；FilterReply.DENY 如果不是
     *         (FilterReply.ACCEPT if message starts with [CRAFT]; FilterReply.DENY if not)
     */
    public FilterReply decide(ILoggingEvent loggingEvent) {
        Object message = loggingEvent.getMessage();
        return ((String)message).startsWith("[CRAFT]") ? FilterReply.ACCEPT : FilterReply.DENY;
    }
}
