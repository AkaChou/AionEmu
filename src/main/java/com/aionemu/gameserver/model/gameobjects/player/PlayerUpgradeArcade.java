package com.aionemu.gameserver.model.gameobjects.player;

import lombok.Getter;
import lombok.Setter;

/**
 * 玩家升级街机游戏对象。
 * Player Upgrade Arcade game object.
 *
 * @author Ranastic
 */
public class PlayerUpgradeArcade {
	/** 返回 frenzy points / Returns the frenzy points */
	@Getter
	@Setter
	private int frenzyPoints = 0;
	/** 返回 frenzy count / Returns the frenzy count */
	@Getter
	@Setter
	private int frenzyCount = 0;
	/** 返回 frenzy level / Returns the frenzy level */
	@Getter
	@Setter
	private int frenzyLevel = 1;
	/** 返回失败等级 / Returns the failed level*/
	@Getter
	@Setter
	private int failedLevel = 1;
	/**
	 * @return 是否处于狂热状态 / Whether frenzy
	 */
	@Getter
	@Setter
	private boolean isFrenzy = false;
	/**
	 * @return 是否重试。 / Whether re try
	 */
	@Getter
	@Setter
	private boolean reTry = false;
	/** 是否失败 / Whether failed*/
	@Getter
	@Setter
	private boolean failed = false;

	/** 重置。 / Reset. */
	public void reset() {
		this.isFrenzy = false;
		this.failed = false;
		this.frenzyLevel = 1;
		this.failedLevel = 1;
		this.reTry = false;
	}
}
