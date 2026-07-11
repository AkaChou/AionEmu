package com.aionemu.gameserver.network.aion.gmhandler;

import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * GM 指令：提升或降低目标玩家等级。
 * GM command handler that levels the target player up or down.
 *
 * @author Alcapwnd
 */
public class CmdLevelUpDown extends AbstractGMHandler {

	/**
	 * 升级或降级方向。
	 * Level-up or level-down direction.
	 */
	public enum LevelUpDownState {

		UP, DOWN;
	};

	private LevelUpDownState state;

	/**
	 * 创建处理器并立即调整等级。
	 * Creates the handler and immediately adjusts the level.
	 *
	 * @param admin 执行指令的管理员 / the admin executing the command
	 * @param params 等级变化量 / level delta
	 * @param state 升级或降级 / level-up or level-down
	 */
	public CmdLevelUpDown(Player admin, String params, LevelUpDownState state) {
		super(admin, params);
		this.state = state;
		run();
	}

	/**
	 * 按 UP/DOWN 状态在合法范围内调整目标玩家等级。
	 * Adjusts the target player's level within valid bounds for UP/DOWN.
	 */
	public void run() {
		Player t = target != null ? target : admin;
		Integer level = Integer.parseInt(params);

		if (state == LevelUpDownState.DOWN) {
			if (t.getCommonData().getLevel() - level >= 1) {
				int newLevel = t.getCommonData().getLevel() - level;
				t.getCommonData().setLevel(newLevel);
			} else {
				PacketSendUtility.sendMessage(admin,
						"The value of <level> will plus calculated to the current player level!");
			}
		} else if (state == LevelUpDownState.UP) {
			if (t.getCommonData().getLevel() + level <= GSConfig.PLAYER_MAX_LEVEL) {
				int newLevel = t.getCommonData().getLevel() + level;
				t.getCommonData().setLevel(newLevel);
			} else {
				PacketSendUtility.sendMessage(admin,
						"The value of <level> will plus calculated to the current player level!");
			}
		}
	}
}
