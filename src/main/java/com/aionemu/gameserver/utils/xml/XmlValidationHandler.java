package com.aionemu.gameserver.utils.xml;

import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.ValidationEventHandler;
import jakarta.xml.bind.ValidationEventLocator;

/**
 * JAXB 校验事件处理器：错误与致命错误时抛 RuntimeException。
 * JAXB ValidationEventHandler that throws RuntimeException on error/fatal error.
 *
 * @author Rolandas
 */
public class XmlValidationHandler implements ValidationEventHandler {

	/**
	 * 处理校验事件；ERROR/FATAL_ERROR 时抛出异常。
	 * Handle a validation event; throws on ERROR/FATAL_ERROR.
	 *
	 * @param event 校验事件 / Validation event
	 * @return 非致命时返回 true 以继续 / True to continue on non-fatal events
	 */
	@Override
	public boolean handleEvent(ValidationEvent event) {
		if (event.getSeverity() == ValidationEvent.FATAL_ERROR || event.getSeverity() == ValidationEvent.ERROR) {
			ValidationEventLocator locator = event.getLocator();
			String message = event.getMessage();
			String file = locator.getURL() == null ? "" : "file=" + locator.getURL().toString() + ", ";
			int line = locator.getLineNumber();
			int column = locator.getColumnNumber();
			throw new RuntimeException("Error at [" + file + "line=" + line + ", column=" + column + "]: " + message, event.getLinkedException());
		}
		return true;
	}
}
