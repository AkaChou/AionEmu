package com.aionemu.gameserver.skillengine.action;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.configs.main.SkillConfig;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;

class DpUseActionTest {

	@AfterEach
	void resetConfig() {
		SkillConfig.CONSUME_DP = true;
	}

	@Test
	void skipsDpDeductionWhenDisabled() throws Exception {
		PlayerCommonData commonData = new PlayerCommonData(1);
		commonData.setPlayerClass(PlayerClass.GLADIATOR);
		commonData.setDp(4000);
		Player player = newPlayer(commonData);
		Skill skill = new Skill(new SkillTemplate(), player, 1, player, null);
		DpUseAction action = new DpUseAction();
		action.value = 2000;

		SkillConfig.CONSUME_DP = false;
		action.act(skill);
		assertEquals(4000, commonData.getDp());

		SkillConfig.CONSUME_DP = true;
		action.act(skill);
		assertEquals(2000, commonData.getDp());
	}

	private Player newPlayer(PlayerCommonData commonData) throws Exception {
		Field unsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
		unsafeField.setAccessible(true);
		Object unsafe = unsafeField.get(null);
		Player player = (Player) unsafe.getClass().getMethod("allocateInstance", Class.class).invoke(unsafe, Player.class);
		Field commonDataField = Player.class.getDeclaredField("playerCommonData");
		commonDataField.setAccessible(true);
		commonDataField.set(player, commonData);
		return player;
	}
}
