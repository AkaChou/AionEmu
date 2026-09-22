package com.aionemu.gameserver.model.gameobjects;

import java.util.LinkedHashMap;
import java.util.Map;

import com.aionemu.gameserver.configs.main.SkillConfig;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;

/**
 * 生物技能冷却注册表。
 * Skill-cooldown registry for a creature.
 * <p>该类型只服务 {@link Creature}，负责冷却表的惰性创建、查询与移除；对外仍通过 {@link Creature}
 * 的原方法访问，并保留冷却表未创建时返回 {@code null} 的语义。
 * This type only serves {@link Creature}: it owns lazy map creation, lookup and removal. External callers
 * still use the original {@link Creature} methods, including the nullable map exposure semantics.</p>
 */
final class CreatureCooldowns {

	private final Creature creature;
	private Map<Integer, Long> skillCoolDowns;
	private Map<Integer, Long> skillCoolDownsBase;

	/**
	 * 绑定宿主生物。
	 * Binds the hosting creature.
	 * @param creature 宿主生物 / hosting creature
	 */
	CreatureCooldowns(Creature creature) {
		this.creature = creature;
	}

	/**
	 * 是否可施放指定技能。
	 * Whether the given skill can be used.
	 * @param template 技能模板 / skill template
	 * @return 是否可施放 / whether usable
	 */
	boolean isSkillDisabled(SkillTemplate template) {
		if (skillCoolDowns == null) {
			return false;
		}
		int delayId = template.getDelayId();
		Long coolDown = skillCoolDowns.get(delayId);
		if (coolDown == null) {
			return false;
		}

		if (coolDown < System.currentTimeMillis()) {
			removeSkillCoolDown(delayId);
			return false;
		}

		/*
		 * Some shared cooldown skills have indipendent and different cooldown they must
		 * not be blocked
		 */
		if (skillCoolDownsBase != null && skillCoolDownsBase.get(delayId) != null) {
			int cooldown = template.scaleCooldownByAttackDelay(template.getCooldown(),
					creature.getGameStats().getAttackSpeed().getCurrent());
			return (template.getDuration() + SkillConfig.scaleCooldown(cooldown) * 100L
					+ skillCoolDownsBase.get(delayId)) >= System.currentTimeMillis();
		}
		return true;
	}

	/**
	 * 获取指定冷却 ID 的结束时间。
	 * Returns the cooldown end time for the given id.
	 * @param delayId 冷却 ID / cooldown id
	 * @return 结束时间，未设置时为 0 / end time, or 0 when absent
	 */
	long getSkillCoolDown(int delayId) {
		if (skillCoolDowns == null || !skillCoolDowns.containsKey(delayId)) {
			return 0;
		}
		return skillCoolDowns.get(delayId);
	}

	/**
	 * 获取整组冷却基准时间。
	 * Returns the base time for a cooldown group.
	 * @param delayId 冷却 ID / cooldown id
	 * @return 基准时间，未设置时为 0 / base time, or 0 when absent
	 */
	long getSkillCoolDownBase(int delayId) {
		return skillCoolDownsBase == null ? 0 : skillCoolDownsBase.getOrDefault(delayId, 0L);
	}

	/**
	 * 设置技能冷却。
	 * Sets a skill cooldown.
	 * @param delayId 冷却 ID / cooldown id
	 * @param time 冷却结束时间 / cooldown end time
	 */
	void setSkillCoolDown(int delayId, long time) {
		if (delayId == 0) {
			return;
		}
		if (skillCoolDowns == null) {
			skillCoolDowns = new LinkedHashMap<>();
		}
		skillCoolDowns.put(delayId, time);
	}

	/**
	 * 移除指定冷却。
	 * Removes the cooldown for the given id.
	 * @param delayId 冷却 ID / cooldown id
	 */
	void removeSkillCoolDown(int delayId) {
		if (skillCoolDowns == null) {
			return;
		}
		skillCoolDowns.remove(delayId);
		if (skillCoolDownsBase != null) {
			skillCoolDownsBase.remove(delayId);
		}
	}

	/**
	 * 设置整组冷却基准时间。
	 * Sets the base time for a cooldown group.
	 * @param delayId 冷却 ID / cooldown id
	 * @param baseTime 基准时间 / base time
	 */
	void setSkillCoolDownBase(int delayId, long baseTime) {
		if (delayId == 0) {
			return;
		}
		if (skillCoolDownsBase == null) {
			skillCoolDownsBase = new LinkedHashMap<>();
		}
		skillCoolDownsBase.put(delayId, baseTime);
	}

	/**
	 * 返回原始冷却表。
	 * Returns the backing cooldown map.
	 * @return 冷却表，未创建时为 null / cooldown map, or null when not created
	 */
	Map<Integer, Long> getSkillCoolDowns() {
		return skillCoolDowns;
	}

	/**
	 * 替换原始冷却表。
	 * Replaces the backing cooldown map.
	 * @param skillCoolDowns 新冷却表 / new cooldown map
	 */
	void setSkillCoolDowns(Map<Integer, Long> skillCoolDowns) {
		this.skillCoolDowns = skillCoolDowns;
	}

	/**
	 * 返回原始整组冷却基准表。
	 * Returns the backing base-time map.
	 * @return 基准表，未创建时为 null / base-time map, or null when not created
	 */
	Map<Integer, Long> getSkillCoolDownsBase() {
		return skillCoolDownsBase;
	}

	/**
	 * 替换原始整组冷却基准表。
	 * Replaces the backing base-time map.
	 * @param skillCoolDownsBase 新基准表 / new base-time map
	 */
	void setSkillCoolDownsBase(Map<Integer, Long> skillCoolDownsBase) {
		this.skillCoolDownsBase = skillCoolDownsBase;
	}
}
