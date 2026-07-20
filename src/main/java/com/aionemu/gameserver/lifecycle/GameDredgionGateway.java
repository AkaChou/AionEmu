package com.aionemu.gameserver.lifecycle;

import org.springframework.stereotype.Component;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.utils.Util;

@Component
public class GameDredgionGateway {
	public void start() {
		Util.printSection(I18n.get("console.section.dredgion"));
	}
}
