package com.aionl.slf4j.filters;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * 控制台日志过滤器
 * Console log filter that filters out specific message types from console output
 */
public class ConsoleFilter extends Filter<ILoggingEvent> {
    
    /**
     * 决定是否接受或拒绝日志事件
     * Decides whether to accept or deny a logging event
     *
     * @param event 待处理的日志事件 (The logging event to be processed)
     * @return FilterReply.ACCEPT 如果消息不以指定前缀开头；FilterReply.DENY 如果消息以指定前缀开头
     *         (FilterReply.ACCEPT if message doesn't start with specified prefixes; FilterReply.DENY if it does)
     */
    public FilterReply decide(ILoggingEvent event) {
        return !event.getMessage().startsWith("[MESSAGE]") 
            && !event.getMessage().startsWith("[ITEM]") 
            && !event.getMessage().startsWith("[ADMIN COMMAND]") 
            && !event.getMessage().startsWith("[AUDIT]") 
                ? FilterReply.ACCEPT 
                : FilterReply.DENY;
    }
}
