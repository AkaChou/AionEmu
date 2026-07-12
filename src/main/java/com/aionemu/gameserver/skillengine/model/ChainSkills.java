package com.aionemu.gameserver.skillengine.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 玩家连锁技能集合：管理单次与多次连击的类别状态。
 * Player chain-skill collection: manages single and multi-cast chain states.
 */
public class ChainSkills {

	private Map<String, ChainSkill> multiSkills = new LinkedHashMap<String, ChainSkill>();
	private ChainSkill chainSkill = new ChainSkill("", 0, 0);

	/**
	 * 获取指定类别的当前连击计数（冷却过期时清零）。
	 * Gets current chain count for a category (resets when cooldown expired).
	 *
	 * 玩家 / player
	 * skill template
	 * chain category
	 * chain count
	 */
	public int getChainCount(Player player, SkillTemplate template, String category) {
		if (category == null) {
			return 0;
		}
		long nullTime = player.getSkillCoolDown(template.getDelayId());
		if (this.multiSkills.get(category) != null) {
			if (System.currentTimeMillis() >= nullTime && this.multiSkills.get(category).getUseTime() <= nullTime) {
				this.multiSkills.get(category).setChainCount(0);
			}
			return this.multiSkills.get(category).getChainCount();
		}
		return 0;
	}

	/**
	 * 获取指定类别最近连击使用时间。
	 * Gets last chain use time for a category.
	 *
	 * chain category
	 *
	 * @param category
	 * @return 毫秒时间戳，无则 0 / epoch millis, or 0 if none
	 */
	public long getLastChainUseTime(String category) {
		if (this.multiSkills.get(category) != null) {
			return this.multiSkills.get(category).getUseTime();
		} else if (chainSkill.getCategory().equals(category)) {
			return this.chainSkill.getUseTime();
		} else {
			return 0;
		}
	}

	/**
	 * 判断类别在给定时间窗内是否仍处于连击有效期。
	 * Whether the category is still within the chain time window.
	 *
	 * chain category
	 * @param time 有效窗口（毫秒） / window millis
	 * whether enabled
	 */
	public boolean chainSkillEnabled(String category, int time) {
		long useTime = 0;
		if (this.multiSkills.get(category) != null) {
			useTime = this.multiSkills.get(category).getUseTime();
		} else if (chainSkill.getCategory().equals(category)) {
			useTime = chainSkill.getUseTime();
		}
		if ((useTime + time) >= System.currentTimeMillis()) {
			return true;
		} else

			return false;
	}

	/**
	 * 记录/推进一次连锁使用。
	 * Records or advances one chain use.
	 *
	 * chain category
	 * @param multiCast 是否多次连击 / whether multi-cast chain
	 */
	public void addChainSkill(String category, boolean multiCast) {
		if (multiCast) {
			if (this.multiSkills.get(category) != null) {
				if (multiCast) {
					this.multiSkills.get(category).increaseChainCount();
				}
				this.multiSkills.get(category).setUseTime(System.currentTimeMillis());
			} else {
				this.multiSkills.put(category,
						new ChainSkill(category, (multiCast ? 1 : 0), System.currentTimeMillis()));
			}
		} else {
			chainSkill.updateChainSkill(category);
		}
	}

	/**
	 * 获取全部连锁状态（含单次与多次）。
	 * Gets all chain states (single and multi).
	 *
	 * @return 连锁技能集合 / chain skill collection
	 */
	public Collection<ChainSkill> getChainSkills() {
		Collection<ChainSkill> collection = new ArrayList<ChainSkill>();
		collection.add(this.chainSkill);
		collection.addAll(this.multiSkills.values());
		return collection;
	}
}
