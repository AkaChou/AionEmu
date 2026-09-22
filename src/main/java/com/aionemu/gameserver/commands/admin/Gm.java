package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 中文 GM 模式开关：等价于 {@code //invul} + {@code //speed 300}。
 * Chinese GM-mode switch: equivalent to {@code //invul} plus {@code //speed 300}.
 * <p>
 * 开启时无敌并把移动/飞行速度覆盖为 300%，再执行一次则解除无敌并恢复基础速度。
 * Enabling grants invulnerability plus a 300% walk/fly speed override; running it again removes both.
 * @author AionEmu
 */
public class Gm extends AdminCommand {

	/** GM 模式使用的速度百分比。 / Speed percent used by GM mode. */
	private static final int GM_SPEED_PERCENT = 300;

	/**
	 * 以别名 {@code gm} 构造命令。
	 * Constructs the command with the alias {@code gm}.
	 */
	public Gm() {
		super("gm");
	}

	/**
	 * 切换 GM 模式：无敌 + 300% 移动与飞行速度。
	 * Toggles GM mode: invulnerability plus a 300% walk and fly speed override.
	 * @param player 执行 GM / Admin player
	 */
	@Override
	public void execute(Player player, String... params) {
		if (player.isInvul()) {
			player.setInvul(false);
			Speed.applyPercent(player, 0);
			PacketSendUtility.sendMessage(player, "GM 模式已关闭：解除无敌，移动与飞行速度恢复正常。");
		}
		else {
			player.setInvul(true);
			Speed.applyPercent(player, GM_SPEED_PERCENT);
			PacketSendUtility.sendMessage(player, "GM 模式已开启：无敌 + 移动/飞行速度 300%（再次输入 //gm 关闭）。");
		}
	}
}
