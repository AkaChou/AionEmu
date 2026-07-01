package com.aionemu.commons.logging.slf4j.filters;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.AbstractMatcherFilter;
import ch.qos.logback.core.spi.FilterReply;

public class ExactLevelFilter extends AbstractMatcherFilter<ILoggingEvent> {

	private Level targetLevel;

	@Override
	public void start() {
		if (targetLevel != null) {
			super.start();
		}
	}

	@Override
	public FilterReply decide(ILoggingEvent event) {
		if (!isStarted() || event == null) {
			return FilterReply.NEUTRAL;
		}
		return targetLevel.equals(event.getLevel()) ? onMatch : onMismatch;
	}

	public void setTargetLevel(Level targetLevel) {
		this.targetLevel = targetLevel;
	}
}
