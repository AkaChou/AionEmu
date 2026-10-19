package com.aionemu.gameserver.model.templates.npc;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.aionemu.gameserver.model.SkillElement;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import lombok.Getter;

/**
 * 真端 NPC 战斗元素覆盖层：承载本地模板原先缺失的命中、减伤、元素防御、
 * 异常抗性/穿透与 PvE 比率。
 * Retail NPC combat-element overlay: carries hit, mitigation, elemental defence,
 * abnormal resistance/penetration and PvE ratios that the local templates did not model.
 * <p>覆盖层由 {@code npc-combat-elements.xml} 生成，仅对实际有非默认值的 NPC 建行；
 * 基础值通过 {@link com.aionemu.gameserver.model.stats.container.NpcGameStats#getStat} 注入，
 * 现有伤害公式与效果抗性公式因此可以直接消费这些属性。
 * The overlay is generated from {@code npc-combat-elements.xml} and only contains NPCs with
 * at least one non-default value. The bases are injected through
 * {@link com.aionemu.gameserver.model.stats.container.NpcGameStats#getStat}, so the existing
 * damage and effect-resistance formulas consume them directly.
 */
@Getter
public final class NpcCombatElements {

	/** 属性基础值；同一属性可由多个真端元素合并。 / Stat bases, merged across retail elements. */
	private final Map<StatEnum, Integer> statBases = new EnumMap<>(StatEnum.class);
	/** 元素免疫集合。 / Elemental immunities. */
	private final Set<SkillElement> elementalImmunities = EnumSet.noneOf(SkillElement.class);

	/** 每次普攻的命中段数（真端 hit_count）。 / Hit count per auto-attack (retail hit_count). */
	private int hitCount = 1;
	/** 真端 dp_reduce；本地暂无消费者，保留数据。 / Retail dp_reduce; data-only for now. */
	private int dpReduce;
	/** 真端 cancel_level；本地暂无消费者，保留数据。 / Retail cancel_level; data-only for now. */
	private int cancelLevel;
	/** 物理伤害修剪标记（真端 physical_damage_trim）。 / Physical damage-trim flag. */
	private boolean physicalDamageTrim;
	/** 魔法伤害修剪标记（真端 magical_damage_trim）。 / Magical damage-trim flag. */
	private boolean magicalDamageTrim;
	/** 是否免疫无视减伤法术（真端 noreducespell_immune）。 / No-reduce-spell immunity flag. */
	private boolean noReduceSpellImmune;
	/** 真端 limit_attribute（Type_A..Type_E）。 / Retail limit_attribute (Type_A..Type_E). */
	private String limitAttribute;

	/**
	 * 合并写入属性基础值。
	 * Merges a stat base value.
	 *
	 * @param stat  属性 / stat
	 * @param value 基础值 / base value
	 */
	public void putStatBase(StatEnum stat, int value) {
		statBases.merge(stat, value, Integer::sum);
	}

	/**
	 * 返回属性基础值；未建模时返回 {@code null}。
	 * Returns the stat base, or {@code null} when the stat is not modelled.
	 *
	 * @param stat 属性 / stat
	 * @return 基础值或 {@code null} / base value or {@code null}
	 */
	public Integer getStatBase(StatEnum stat) {
		return statBases.get(stat);
	}

	/**
	 * 将真端 limit_attribute 映射为本地 PvE 攻击比率属性。
	 * Maps the retail limit_attribute to the local PvE attack-ratio stat.
	 *
	 * @return 对应的 PvE 属性；无法映射时为 {@code null} / mapped stat, or {@code null}
	 */
	public StatEnum getLimitAttributeStat() {
		if (limitAttribute == null) {
			return null;
		}
		return switch (limitAttribute) {
			case "Type_A" -> StatEnum.PVE_ATTACK_RATIO_TYPE_A;
			case "Type_B" -> StatEnum.PVE_ATTACK_RATIO_TYPE_B;
			case "Type_C" -> StatEnum.PVE_ATTACK_RATIO_TYPE_C;
			case "Type_D" -> StatEnum.PVE_ATTACK_RATIO_TYPE_D;
			case "Type_E" -> StatEnum.PVE_ATTACK_RATIO_TYPE_E;
			default -> null;
		};
	}

	/**
	 * 设置命中段数。
	 * Sets the hit count.
	 *
	 * @param hitCount 命中段数 / hit count
	 */
	public void setHitCount(int hitCount) {
		this.hitCount = Math.max(1, hitCount);
	}

	/**
	 * 设置 dp_reduce。
	 * Sets dp_reduce.
	 *
	 * @param dpReduce 真端值 / retail value
	 */
	public void setDpReduce(int dpReduce) {
		this.dpReduce = dpReduce;
	}

	/**
	 * 设置 cancel_level。
	 * Sets cancel_level.
	 *
	 * @param cancelLevel 真端值 / retail value
	 */
	public void setCancelLevel(int cancelLevel) {
		this.cancelLevel = cancelLevel;
	}

	/**
	 * 设置物理伤害修剪标记。
	 * Sets the physical damage-trim flag.
	 *
	 * @param physicalDamageTrim 是否修剪 / whether trimmed
	 */
	public void setPhysicalDamageTrim(boolean physicalDamageTrim) {
		this.physicalDamageTrim = physicalDamageTrim;
	}

	/**
	 * 设置魔法伤害修剪标记。
	 * Sets the magical damage-trim flag.
	 *
	 * @param magicalDamageTrim 是否修剪 / whether trimmed
	 */
	public void setMagicalDamageTrim(boolean magicalDamageTrim) {
		this.magicalDamageTrim = magicalDamageTrim;
	}

	/**
	 * 设置无视减伤法术免疫。
	 * Sets no-reduce-spell immunity.
	 *
	 * @param noReduceSpellImmune 是否免疫 / whether immune
	 */
	public void setNoReduceSpellImmune(boolean noReduceSpellImmune) {
		this.noReduceSpellImmune = noReduceSpellImmune;
	}

	/**
	 * 设置 limit_attribute。
	 * Sets limit_attribute.
	 *
	 * @param limitAttribute 真端字符串 / retail string
	 */
	public void setLimitAttribute(String limitAttribute) {
		this.limitAttribute = limitAttribute;
	}

	/**
	 * 判断是否免疫指定元素。
	 * Returns whether the NPC is immune to the given element.
	 *
	 * @param element 元素 / element
	 * @return 是否免疫 / whether immune
	 */
	public boolean isElementallyImmune(SkillElement element) {
		return elementalImmunities.contains(element);
	}

	/**
	 * 添加元素免疫。
	 * Adds an elemental immunity.
	 *
	 * @param element 元素 / element
	 */
	public void addElementalImmunity(SkillElement element) {
		elementalImmunities.add(element);
	}
}
