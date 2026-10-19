package com.aionemu.gameserver.model.stats.container;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.stats.calc.AdditionStat;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.templates.npc.NpcMasterItemBonus;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;

/**
 * 真端主人物品加成继承：召唤物按模板比例继承主人装备属性，并按模板上限封顶。
 * Retail master-item inheritance: a summon inherits a template-defined percentage of the
 * master's equipment stats, capped by the template limit.
 * <p>真端 {@code Npc_SetAttrBonus_ByMasterItemBonus} 的算式为
 * {@code base + base × ratio% × masterPercent% + masterFlat × ratio%}，再以 limit 封顶；
 * 本实现读取主人装备加成总额 {@code V}，按 {@code min(V × ratio / 100, limit)} 注入召唤物属性。
 * The retail formula is {@code base + base × ratio% × masterPercent% + masterFlat × ratio%},
 * clamped by the limit; this implementation reads the master's total equipment bonus {@code V}
 * and injects {@code min(V × ratio / 100, limit)} into the summon's stat.
 */
final class MasterItemBonusApplier {

	private MasterItemBonusApplier() {
	}

	/**
	 * 对单个属性应用主人物品加成继承。
	 * Applies master-item inheritance to one stat.
	 *
	 * @param owner    召唤物 / summon or summoned object
	 * @param template 召唤物模板 / summon template
	 * @param statEnum 目标属性 / target stat
	 * @param stat     已计算的属性 / computed stat
	 * @return 调整后的属性 / adjusted stat
	 */
	static Stat2 apply(Creature owner, NpcTemplate template, StatEnum statEnum, Stat2 stat) {
		Creature master = owner.getMaster();
		if (master == null || template == null) {
			return stat;
		}
		NpcMasterItemBonus config = template.getMasterItemBonus();
		if (config == null) {
			return stat;
		}
		NpcMasterItemBonus.Entry entry = config.get(statEnum);
		if (entry == null || entry.ratio() <= 0) {
			return stat;
		}
		StatEnum masterStat = masterStat(statEnum);
		Stat2 probe = new AdditionStat(masterStat, 0, owner);
		master.getGameStats().getItemStatBoost(masterStat, probe);
		float itemBonus = Math.max(0f, probe.getExactCurrent());
		float inherited = Math.min(itemBonus * entry.ratio() / 100f, entry.limit());
		if (inherited > 0f) {
			stat.setBonusRate(1f);
			stat.addToBonus(inherited);
		}
		return stat;
	}

	/**
	 * 主人物品属性名映射：命中/暴击/物攻读取主手属性。
	 * Master-side stat mapping: accuracy, critical and physical attack read main-hand stats.
	 *
	 * @param stat 召唤物侧属性 / summon-side stat
	 * @return 主人侧属性 / master-side stat
	 */
	static StatEnum masterStat(StatEnum stat) {
		return switch (stat) {
			case PHYSICAL_ACCURACY -> StatEnum.MAIN_HAND_ACCURACY;
			case PHYSICAL_CRITICAL -> StatEnum.MAIN_HAND_CRITICAL;
			case PHYSICAL_ATTACK -> StatEnum.MAIN_HAND_POWER;
			default -> stat;
		};
	}
}
