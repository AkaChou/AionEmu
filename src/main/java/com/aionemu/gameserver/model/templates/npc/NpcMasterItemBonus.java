package com.aionemu.gameserver.model.templates.npc;

import java.util.EnumMap;
import java.util.Map;

import com.aionemu.gameserver.model.stats.container.StatEnum;
import lombok.Getter;

/**
 * 真端 NPC 主人物品加成配置：召唤物按比例继承主人装备属性，并按上限封顶。
 * Retail NPC master-item bonus configuration: a summon inherits a percentage of the
 * master's equipment stats, capped by a per-stat limit.
 * <p>真端 {@code Npc_SetAttrBonus_ByMasterItemBonus} 在召唤物出生时读取主人的物品加成
 * （{@code BonusAttrSum_MasterItem}），逐属性计算 {@code min(主人加成 × ratio / 100, limit)}。
 * 本模型只保存模板侧的 ratio/limit，主人侧数值在运行时由召唤物属性容器读取。
 * The retail {@code Npc_SetAttrBonus_ByMasterItemBonus} reads the master's item bonus
 * ({@code BonusAttrSum_MasterItem}) when the summon spawns and computes
 * {@code min(master bonus × ratio / 100, limit)} per stat. This model stores only the
 * template-side ratio/limit; the master-side values are read at runtime.
 */
@Getter
public final class NpcMasterItemBonus {

	/** 逐属性配置。 / Per-stat configuration. */
	private final Map<StatEnum, Entry> entries = new EnumMap<>(StatEnum.class);

	/**
	 * 单属性继承配置。
	 * Inheritance configuration for one stat.
	 *
	 * @param ratio 继承百分比（100 = 全额） / inheritance percentage (100 = full)
	 * @param limit 继承加成上限 / capped inherited bonus
	 */
	public record Entry(int ratio, int limit) {
	}

	/**
	 * 写入一条配置。
	 * Puts one entry.
	 *
	 * @param stat  召唤物侧属性 / summon-side stat
	 * @param ratio 继承百分比 / inheritance percentage
	 * @param limit 上限 / limit
	 */
	public void put(StatEnum stat, int ratio, int limit) {
		entries.put(stat, new Entry(ratio, limit));
	}

	/**
	 * 返回指定属性的配置。
	 * Returns the configuration for the given stat.
	 *
	 * @param stat 属性 / stat
	 * @return 配置；未配置时为 {@code null} / entry, or {@code null}
	 */
	public Entry get(StatEnum stat) {
		return entries.get(stat);
	}
}
