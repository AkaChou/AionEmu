package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.configs.main.SkillConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.SkillData;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.skill.PlayerSkillEntry;
import com.aionemu.gameserver.model.skill.PlayerSkillList;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SMSkillCooldownTest {

	@Test
	void writesLoginCooldownAsActiveMillisAndAnimationMillis() throws Exception {
		SkillData previousSkillData = DataManager.SKILL_DATA;
		DataManager.SKILL_DATA = skillData(skillTemplate(1001, 10, 3000));
		try {
			long reuseTime = System.currentTimeMillis() + 123_456;
			ByteBuffer buffer = write(new SM_SKILL_COOLDOWN(playerWithSkills(1001), Map.of(10, reuseTime), false));

			assertEquals(1, Short.toUnsignedInt(buffer.getShort()));
			assertEquals(0, Byte.toUnsignedInt(buffer.get()));
			assertEquals(1001, Short.toUnsignedInt(buffer.getShort()));
			int remaining = buffer.getInt();
			assertTrue(remaining > 120_000 && remaining <= 123_456, "skill cooldown remaining time must be written in milliseconds");
			assertEquals(300_000, buffer.getInt());
		} finally {
			DataManager.SKILL_DATA = previousSkillData;
		}
	}

	@Test
	void scalesLoginCooldownAnimation() throws Exception {
		SkillData previousSkillData = DataManager.SKILL_DATA;
		double previousMultiplier = SkillConfig.COOLDOWN_MULTIPLIER;
		DataManager.SKILL_DATA = skillData(skillTemplate(1001, 10, 3000));
		SkillConfig.COOLDOWN_MULTIPLIER = 0.01;
		try {
			ByteBuffer buffer = write(new SM_SKILL_COOLDOWN(playerWithSkills(1001), Map.of(10, System.currentTimeMillis() + 3_000), false));
			buffer.position(9);

			assertEquals(3_000, buffer.getInt());
		} finally {
			SkillConfig.COOLDOWN_MULTIPLIER = previousMultiplier;
			DataManager.SKILL_DATA = previousSkillData;
		}
	}

	@Test
	void loginCooldownOnlyContainsLearnedSkillsWithLongestAnimationLast() throws Exception {
		SkillData previousSkillData = DataManager.SKILL_DATA;
		DataManager.SKILL_DATA = skillData(skillTemplate(1001, 10, 3000), skillTemplate(1002, 10, 30), skillTemplate(1003, 10, 1));
		try {
			ByteBuffer buffer = write(new SM_SKILL_COOLDOWN(playerWithSkills(1001, 1002), Map.of(10, System.currentTimeMillis() + 300_000), false));

			assertEquals(2, Short.toUnsignedInt(buffer.getShort()));
			assertEquals(0, Byte.toUnsignedInt(buffer.get()));
			assertEquals(1002, Short.toUnsignedInt(buffer.getShort()));
			buffer.getInt();
			assertEquals(3_000, buffer.getInt());
			assertEquals(1001, Short.toUnsignedInt(buffer.getShort()));
			buffer.getInt();
			assertEquals(300_000, buffer.getInt());
		} finally {
			DataManager.SKILL_DATA = previousSkillData;
		}
	}

	@Test
	void newSkillResendsOnlyItsActiveCooldown() throws Exception {
		String skillListSource = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/model/skill/PlayerSkillList.java"));
		int addSkill = skillListSource.indexOf("private synchronized boolean addSkill(");
		int sendSkillList = skillListSource.indexOf("new SM_SKILL_LIST(player, player.getSkillList().getLinkedSkills())", addSkill);
		int resendCooldown = skillListSource.indexOf("new SM_SKILL_COOLDOWN(player, Map.of(skillTemplate.getDelayId(), reuseTime), false)", addSkill);

		assertTrue(skillListSource.indexOf("if (isNew && player.isSpawned())", addSkill) > sendSkillList);
		assertTrue(skillListSource.indexOf("if (reuseTime > System.currentTimeMillis())", addSkill) > sendSkillList);
		assertTrue(resendCooldown > sendSkillList, "cooldown must be resent after the new skill is visible to the client");

		String minionSource = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/services/toypet/MinionService.java"));
		assertFalse(minionSource.contains("SM_SKILL_COOLDOWN"), "minion spawning must use the shared skill-addition cooldown sync");
	}

	private ByteBuffer write(SM_SKILL_COOLDOWN packet) throws Exception {
		packet.setBuf(ByteBuffer.allocate(32));
		Method writeImpl = SM_SKILL_COOLDOWN.class.getDeclaredMethod("writeImpl", AionConnection.class);
		writeImpl.setAccessible(true);
		writeImpl.invoke(packet, new Object[] { null });
		ByteBuffer buffer = packet.getBuf();
		buffer.flip();
		return buffer;
	}

	private Player playerWithSkills(int... skillIds) throws Exception {
		Player player = (Player) allocateInstance(Player.class);
		List<PlayerSkillEntry> entries = new ArrayList<>();
		for (int skillId : skillIds) {
			entries.add(new PlayerSkillEntry(skillId, false, false, 1, 0, null, 0, false, PersistentState.NOACTION));
		}
		player.setSkillList(new PlayerSkillList(entries));
		return player;
	}

	private Object allocateInstance(Class<?> type) throws Exception {
		Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
		Field field = unsafeType.getDeclaredField("theUnsafe");
		field.setAccessible(true);
		Object unsafe = field.get(null);
		return unsafeType.getMethod("allocateInstance", Class.class).invoke(unsafe, type);
	}

	private SkillData skillData(SkillTemplate... templates) {
		SkillData skillData = new SkillData();
		skillData.setSkillTemplates(List.of(templates));
		skillData.initializeCooldownGroups();
		return skillData;
	}

	private SkillTemplate skillTemplate(int skillId, int delayId, int cooldown) throws Exception {
		SkillTemplate template = new SkillTemplate();
		setInt(template, "skillId", skillId);
		setInt(template, "delayId", delayId);
		setInt(template, "cooldown", cooldown);
		return template;
	}

	private void setInt(SkillTemplate template, String fieldName, int value) throws Exception {
		Field field = SkillTemplate.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.setInt(template, value);
	}
}
