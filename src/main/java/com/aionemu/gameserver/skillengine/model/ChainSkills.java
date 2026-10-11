package com.aionemu.gameserver.skillengine.model;

import java.util.LinkedHashMap;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 玩家连锁技能集合：管理单次与多次连击的类别状态。
 * Player chain-skill collection: manages single and multi-cast chain states.
 */
public class ChainSkills {

	private final Map<String, ChainSkill> multiSkills = new LinkedHashMap<>();
	private final ChainSkill chainSkill = new ChainSkill("", 0, 0);

	/**
	 * 获取指定类别的当前连击计数（冷却过期时清零）。
	 * Gets current chain count for a category (resets when cooldown expired).
	 * @param player 玩家 / player
	 * @param template 技能模板 / skill template
	 * @param category 连锁类别 / chain category
	 * @return 连击计数 / chain count
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
	 * 判断类别在给定时间窗内是否仍处于连击有效期。
	 * Whether the category is still within the chain time window.
	 * @param category 连锁类别 / chain category
	 * @param time 有效窗口（毫秒） / window millis
	 * @return 是否有效 / whether enabled
	 */
	public boolean chainSkillEnabled(String category, int time) {
		long useTime = 0;
		if (this.multiSkills.get(category) != null) {
			useTime = this.multiSkills.get(category).getUseTime();
		} else if (chainSkill.getCategory().equals(category)) {
			useTime = chainSkill.getUseTime();
		}
		return (useTime + time) >= System.currentTimeMillis();
	}

	/**
	 * 记录/推进一次连锁使用。
	 * Records or advances one chain use.
	 * @param category 连锁类别 / chain category
	 * @param multiCast 是否多次连击 / whether multi-cast chain
	 */
	public void addChainSkill(String category, boolean multiCast) {
		if (multiCast) {
			if (this.multiSkills.get(category) != null) {
				this.multiSkills.get(category).increaseChainCount();
				this.multiSkills.get(category).setUseTime(System.currentTimeMillis());
			} else {
				this.multiSkills.put(category, new ChainSkill(category, 1, System.currentTimeMillis()));
			}
		} else {
			chainSkill.updateChainSkill(category);
		}
	}
}
