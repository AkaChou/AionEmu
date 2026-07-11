package com.aionemu.gameserver.dataholders.loadingutils;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.ValidationEventHandler;
import jakarta.xml.bind.ValidationEventLocator;

/**
 * JAXB XML 校验事件处理器：致命/错误时记录行号并抛出 Error，其余事件放行。
 * JAXB XML validation-event handler: logs line/column and throws Error on fatal/error events, otherwise continues.
 *
 * @author Rolandas
 */
@Slf4j
public class XmlValidationHandler implements ValidationEventHandler {


	/**
	 * 处理 JAXB 校验事件；致命错误与错误会中断加载。
	 * Handles a JAXB validation event; fatal and error severities abort loading.
	 *
	 * @param event 校验事件 / validation event
	 * @return 可继续则为 true / true if processing may continue
	 */
	@Override
	public boolean handleEvent(ValidationEvent event) {
		if (event.getSeverity() == ValidationEvent.FATAL_ERROR || event.getSeverity() == ValidationEvent.ERROR) {
			ValidationEventLocator locator = event.getLocator();
			String message = event.getMessage();
			int line = locator.getLineNumber();
			int column = locator.getColumnNumber();
			log.error(I18n.get("log.7c37b0227a81", line, column, message));
			throw new Error(event.getLinkedException());
		}
		return true;
	}
}
