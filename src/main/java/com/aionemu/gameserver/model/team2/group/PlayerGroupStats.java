package com.aionemu.gameserver.model.team2.group;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.google.common.base.Predicate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.AccessLevel;

/**
 * 玩家队伍属性，用于团队2相关逻辑。
 * Player Group Stats for team 2 logic.
 * @author ATracer
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class PlayerGroupStats implements Predicate<Player> {

	private final PlayerGroup group;
	/** 返回最小经验玩家等级 / Returns the min exp player level*/
	private int minExpPlayerLevel;
	/** 返回最大经验玩家等级 / Returns the max exp player level*/
	private int maxExpPlayerLevel;
	Player minLevelPlayer;
	Player maxLevelPlayer;

	/** 添加玩家 / On Add Player*/
	public void onAddPlayer(PlayerGroupMember member) {
		group.applyOnMembers(this);
		calculateExpLevels();
	}

	/** 移除玩家 / On Remove Player*/
	public void onRemovePlayer(PlayerGroupMember member) {
		group.applyOnMembers(this);
	}

	private void calculateExpLevels() {
		minExpPlayerLevel = minLevelPlayer.getLevel();
		maxExpPlayerLevel = maxLevelPlayer.getLevel();
		minLevelPlayer = null;
		maxLevelPlayer = null;
	}

	/** 应用。 / Apply. */
	@Override
	public boolean apply(Player player) {
		if (minLevelPlayer == null || maxLevelPlayer == null) {
			minLevelPlayer = player;
			maxLevelPlayer = player;
		} else {
			if (player.getCommonData().getExp() < minLevelPlayer.getCommonData().getExp()) {
				minLevelPlayer = player;
			}
			if (!player.isMentor() && player.getCommonData().getExp() > maxLevelPlayer.getCommonData().getExp()) {
				maxLevelPlayer = player;
			}
		}
		return true;
	}
}
