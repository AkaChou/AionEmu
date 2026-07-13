package com.aionemu.gameserver.model.skill;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.templates.npcskill.NpcSkillTemplate;
import com.aionemu.gameserver.model.templates.npcskill.NpcSkillTemplates;

/**
 * NPC 技能列表，用于技能相关逻辑。
 * Npc Skill List for skill logic.
 *
 * @author ATracer
 */
public class NpcSkillList implements SkillList<Npc> {

	private List<NpcSkillEntry> skills;

	public NpcSkillList(Npc owner) {
		this(owner.getNpcId());
	}

	NpcSkillList(int npcId) {
		initSkillList(npcId);
	}

	private void initSkillList(int npcId) {
		NpcSkillTemplates npcSkillList = DataManager.NPC_SKILL_DATA.getNpcSkillList(npcId);
		if (npcSkillList != null) {
			initSkills();
			for (NpcSkillTemplate template : npcSkillList.getNpcSkills()) {
				NpcSkillEntry entry = template.getSkillid() > 0 ? new NpcSkillTemplateEntry(template) : null;
				if (template.getSourceIndex() < 0) {
					skills.add(entry);
				} else {
					while (skills.size() <= template.getSourceIndex()) {
						skills.add(null);
					}
					skills.set(template.getSourceIndex(), entry);
				}
			}
		}
	}

	/** 添加技能。 / Adds skill. */
	@Override
	public boolean addSkill(Npc creature, int skillId, int skillLevel) {
		initSkills();
		skills.add(new NpcSkillParameterEntry(skillId, skillLevel));
		return true;
	}

	/** 移除技能。 / Removes skill. */
	@Override
	public boolean removeSkill(int skillId) {
		Iterator<NpcSkillEntry> iter = skills.iterator();
		while (iter.hasNext()) {
			NpcSkillEntry next = iter.next();
			if (next != null && next.getSkillId() == skillId) {
				iter.remove();
				return true;
			}
		}
		return false;
	}

	/** 是否技能存在 / Whether skill present*/
	@Override
	public boolean isSkillPresent(int skillId) {
		if (skills == null) {
			return false;
		}
		return getSkill(skillId) != null;
	}

	/** 获取技能等级。 / Returns the skill level. */
	@Override
	public int getSkillLevel(int skillId) {
		return getSkill(skillId).getSkillLevel();
	}

	/** 大小 / size. */
	@Override
	public int size() {
		return skills != null ? skills.size() : 0;
	}

	private void initSkills() {
		if (skills == null) {
			skills = new ArrayList<NpcSkillEntry>();
		}
	}

	/** 返回随机技能 / Returns the random skill */
	public NpcSkillEntry getRandomSkill() {
		if (skills == null || skills.size() == 0) {
			return null;
		}
		int count = 0;
		for (NpcSkillEntry skill : skills) {
			if (skill != null && !skill.isUltraSkill() && skill.hasUsesLeft()) {
				count++;
			}
		}
		if (count == 0) {
			return null;
		}
		int selected = Rnd.get(count);
		for (NpcSkillEntry skill : skills) {
			if (skill != null && !skill.isUltraSkill() && skill.hasUsesLeft() && selected-- == 0) {
				return skill;
			}
		}
		return null;
	}

	public NpcSkillEntry getSkillByIndex(int index) {
		return skills == null || index < 0 || index >= skills.size() ? null : skills.get(index);
	}

	public void resetUseCounts() {
		if (skills != null) {
			skills.stream().filter(java.util.Objects::nonNull).forEach(NpcSkillEntry::resetUseCount);
		}
	}

	private SkillEntry getSkill(int skillId) {
		for (SkillEntry entry : skills) {
			if (entry != null && entry.getSkillId() == skillId) {
				return entry;
			}
		}
		return null;
	}

	/** 返回 use in spawned skill / Returns the use in spawned skill */
	public NpcSkillEntry getUseInSpawnedSkill() {
		if (this.skills == null) {
			return null;
		}
		Iterator<NpcSkillEntry> iter = skills.iterator();
		while (iter.hasNext()) {
			NpcSkillEntry next = iter.next();
			if (next == null) {
				continue;
			}
			NpcSkillTemplateEntry tmpEntry = (NpcSkillTemplateEntry) next;
			if (tmpEntry.UseInSpawned()) {
				return next;
			}
		}
		return null;
	}

	/** 添加关联技能 / Adds linked skill */
	@Override
	public boolean addLinkedSkill(Npc creature, int skillId) {
		return false;
	}
}
