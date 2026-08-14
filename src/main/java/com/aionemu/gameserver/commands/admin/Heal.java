package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_STATUPDATE_EXP;
import com.aionemu.gameserver.skillengine.model.SkillTargetSlot;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 管理员治疗命令：恢复目标 HP/MP，或玩家的 DP/FP/安息能量。
 * Admin heal command: restore target HP/MP, or a player's DP/FP/repose energy.
 */
public class Heal extends AdminCommand
{
	public Heal() {
		super("heal");
	}

	/**
	 * 治疗当前选中的生物：默认满 HP/MP 并清除 SPEC2 异常；子命令 dp/fp/repose/test。
	 * Heal the selected creature: default full HP/MP and clear SPEC2 abnormals; subcommands dp/fp/repose/test.
	 *
	 * @param player 执行命令的管理员 / Admin executing the command
	 * @param params 可选子命令 / Optional subcommand
	 */
	@Override
	public void execute(Player player, String... params) {
		VisibleObject target = player.getTarget();
		if (target == null) {
			PacketSendUtility.sendMessage(player, "No target selected");
			return;
		} if (!(target instanceof Creature)) {
			PacketSendUtility.sendMessage(player, "Target has to be Creature!");
			return;
		}
		Creature creature = (Creature) target;
		if (params == null || params.length < 1) {
			creature.getLifeStats().increaseHp(TYPE.HP, creature.getLifeStats().getMaxHp() + 1);
			creature.getLifeStats().increaseMp(TYPE.MP, creature.getLifeStats().getMaxMp() + 1);
			creature.getEffectController().removeAbnormalEffectsByTargetSlot(SkillTargetSlot.SPEC2);
			PacketSendUtility.sendMessage(player, creature.getName() + " has been refreshed !");
		} else if (params[0].equals("dp") && creature instanceof Player) {
			Player targetPlayer = (Player) creature;
			targetPlayer.getCommonData().setDp(targetPlayer.getGameStats().getMaxDp().getCurrent());
			PacketSendUtility.sendMessage(player, targetPlayer.getName() + " is now full of DP !");
		} else if (params[0].equals("fp") && creature instanceof Player) {
			Player targetPlayer = (Player) creature;
			targetPlayer.getLifeStats().setCurrentFp(targetPlayer.getLifeStats().getMaxFp());
			PacketSendUtility.sendMessage(player, targetPlayer.getName() + " FP has been fully refreshed !");
		} else if (params[0].equals("repose") && creature instanceof Player) {
			Player targetPlayer = (Player) creature;
			PlayerCommonData pcd = targetPlayer.getCommonData();
			pcd.setCurrentReposteEnergy(pcd.getMaxReposteEnergy());
			PacketSendUtility.sendMessage(player, targetPlayer.getName() + " Reposte Energy has been fully refreshed !");
			PacketSendUtility.sendPacket(targetPlayer, new SM_STATUPDATE_EXP(pcd.getExpShown(), pcd.getExpRecoverable(), pcd.getExpNeed(), pcd.getCurrentReposteEnergy(), pcd.getMaxReposteEnergy()));
		} else if (params[0].equals("test") && creature instanceof Player) {
			Player targetPlayer = (Player) creature;
			PlayerCommonData pcd = targetPlayer.getCommonData();
			pcd.setCurrentReposteEnergy(pcd.getMaxReposteEnergy());
			PacketSendUtility.sendPacket(targetPlayer, new SM_STATUPDATE_EXP(pcd.getExpShown(), pcd.getExpRecoverable(), pcd.getExpNeed(), pcd.getCurrentReposteEnergy(), pcd.getMaxReposteEnergy(), 0, 38730744));
		} else {
			onFail(player, null);
		}
	}

	/**
	 * 参数错误时显示命令语法。
	 * Show command syntax on invalid arguments.
	 *
	 * @param player 接收提示的玩家 / Player receiving the hint
	 */
	@Override
	public void onFail(Player player, String message) {
		String syntax = "//heal : Full HP and MP\n" + "//heal dp : Full DP, must be used on a player !\n" + "//heal fpr : Full FP, must be used on a player\n" + "//heal repose : Full repose energy, must be used on a player";
		PacketSendUtility.sendMessage(player, syntax);
	}
}
