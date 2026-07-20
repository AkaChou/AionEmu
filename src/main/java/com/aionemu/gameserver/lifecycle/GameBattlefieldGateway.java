package com.aionemu.gameserver.lifecycle;

import org.springframework.stereotype.Component;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.configs.main.AutoGroupConfig;
import com.aionemu.gameserver.services.RetailMatchmakingService;
import com.aionemu.gameserver.utils.Util;

@Component
public class GameBattlefieldGateway {
	public void start() {
		Util.printSection(I18n.get("console.section.battlefield"));
		if (AutoGroupConfig.AUTO_GROUP_ENABLED
				&& GameCoreGameplayServices.autoGroupService() instanceof RetailMatchmakingService matchmaking) {
			matchmaking.startScheduleNotifications();
		}
	}
}
