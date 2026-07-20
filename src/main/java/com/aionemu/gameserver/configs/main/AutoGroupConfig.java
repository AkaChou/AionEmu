package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

public class AutoGroupConfig {
	@Property(key = "gameserver.autogroup.enable", defaultValue = "true")
	public static boolean AUTO_GROUP_ENABLED;
}
