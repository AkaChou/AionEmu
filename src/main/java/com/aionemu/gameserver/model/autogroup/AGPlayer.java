package com.aionemu.gameserver.model.autogroup;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import lombok.Getter;
import lombok.Setter;

/**
 * AG 玩家，用于 autogroup 相关逻辑。
 * AG Player for autogroup logic.
 */

public class AGPlayer {
	/** 返回对象 ID / Returns the object id */
	@Getter
	private final Integer objectId;
	/** 获取种族。 / Returns the race. */
	@Getter
	private final Race race;
	/** 获取玩家职业。 / Returns the player class. */
	@Getter
	private final PlayerClass playerClass;
	/** 获取名称。 / Returns the name. */
	@Getter
	private final String name;
	/** 设置实例 / Sets the in instance*/
	@Getter
	@Setter
	private boolean isInInstance;
	/** 是否在线。 / Whether Online. */
	@Getter
	@Setter
	private boolean isOnline;
	/** 设置 press enter / Sets the press enter */
	@Setter
	private boolean isPressEnter;

	public AGPlayer(Player player) {
		objectId = player.getObjectId();
		race = player.getRace();
		playerClass = player.getPlayerClass();
		name = player.getName();
		isOnline = true;
	}

	/**
	 * @return 是否已按下回车 / Whether pressed enter
	 */
	public boolean isPressedEnter() {
		return isPressEnter;
	}
}
