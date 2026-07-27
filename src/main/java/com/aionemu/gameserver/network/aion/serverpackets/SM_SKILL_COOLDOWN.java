package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.configs.main.SkillConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.skill.PlayerSkillEntry;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;

/**
 * 向客户端同步技能冷却时间表。
 * Server packet synchronizing the skill cooldown table to the client.
 */
public class SM_SKILL_COOLDOWN extends AionServerPacket {
	private final List<Cooldown> cooldowns = new ArrayList<>();
	private final boolean isSkillRemove;

	/**
	 * 使用给定参数构造 SM_SKILL_COOLDOWN 包。
	 * Creates a SM_SKILL_COOLDOWN packet with the given parameters.
	 *
	 * cooldown map
	 */
	public SM_SKILL_COOLDOWN(Map<Integer, Long> cooldowns) {
		this(cooldowns, true);
	}

	/**
	 * 使用给定参数构造 SM_SKILL_COOLDOWN 包。
	 * Creates a SM_SKILL_COOLDOWN packet with the given parameters.
	 *
	 * cooldown map
	 * @param isSkillRemove 是否因移除技能 / skill-remove flag
	 */
	public SM_SKILL_COOLDOWN(Map<Integer, Long> cooldowns, boolean isSkillRemove) {
		this.isSkillRemove = isSkillRemove;
		for (Map.Entry<Integer, Long> entry : cooldowns.entrySet()) {
			for (int skillId : DataManager.SKILL_DATA.getSkillsForDelayId(entry.getKey())) {
				this.cooldowns.add(new Cooldown(skillId, entry.getValue()));
			}
		}
		sortByAnimationDuration();
	}

	/**
	 * 使用给定参数构造 SM_SKILL_COOLDOWN 包。
	 * Creates a SM_SKILL_COOLDOWN packet with the given parameters.
	 *
	 * 玩家 / player
	 * cooldown map
	 * @param isSkillRemove 是否因移除技能 / skill-remove flag
	 */
	public SM_SKILL_COOLDOWN(Player player, Map<Integer, Long> cooldowns, boolean isSkillRemove) {
		this.isSkillRemove = isSkillRemove;
		for (PlayerSkillEntry skill : player.getSkillList().getAllSkills()) {
			SkillTemplate skillTemplate = DataManager.SKILL_DATA.getSkillTemplate(skill.getSkillId());
			Long reuseTime = cooldowns.get(skillTemplate.getDelayId());
			if (reuseTime != null) {
				this.cooldowns.add(new Cooldown(skill.getSkillId(), reuseTime));
			}
		}
		sortByAnimationDuration();
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeH(cooldowns.size());
		writeC(isSkillRemove ? 1 : 0);
		for (Cooldown cooldown : cooldowns) {
			writeH(cooldown.skillId);
			writeD(cooldown.getRemainingMillis());
			writeD(isSkillRemove ? 0 : cooldown.getAnimationDurationMillis());
		}
	}

	private void sortByAnimationDuration() {
		cooldowns.sort(Comparator.comparingInt(Cooldown::getAnimationDurationMillis));
	}

	private static class Cooldown {
		private final int skillId;
		private final long reuseTime;

		private Cooldown(int skillId, long reuseTime) {
			this.skillId = skillId;
			this.reuseTime = reuseTime;
		}

		private int getRemainingMillis() {
			return (int) Math.max(0, reuseTime - System.currentTimeMillis());
		}

		private int getAnimationDurationMillis() {
			return SkillConfig.scaleCooldown(DataManager.SKILL_DATA.getSkillTemplate(skillId).getCooldown()) * 100;
		}
	}
}
