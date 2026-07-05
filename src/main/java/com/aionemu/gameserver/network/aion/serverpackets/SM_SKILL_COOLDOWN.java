/*

 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.skill.PlayerSkillEntry;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;

public class SM_SKILL_COOLDOWN extends AionServerPacket {
	private final List<Cooldown> cooldowns = new ArrayList<>();
	private final boolean isSkillRemove;

	public SM_SKILL_COOLDOWN(Map<Integer, Long> cooldowns) {
		this(cooldowns, true);
	}

	public SM_SKILL_COOLDOWN(Map<Integer, Long> cooldowns, boolean isSkillRemove) {
		this.isSkillRemove = isSkillRemove;
		for (Map.Entry<Integer, Long> entry : cooldowns.entrySet()) {
			for (int skillId : DataManager.SKILL_DATA.getSkillsForDelayId(entry.getKey())) {
				this.cooldowns.add(new Cooldown(skillId, entry.getValue()));
			}
		}
		sortByAnimationDuration();
	}

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
			return DataManager.SKILL_DATA.getSkillTemplate(skillId).getCooldown() * 100;
		}
	}
}
