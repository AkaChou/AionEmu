package com.aionemu.gameserver.restrictions;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 关机倒计时限制：关机过程中禁止攻击、技能、聊天、组队与换装等。
 * Shutdown restrictions: blocks attack, skill, chat, invite and equip during shutdown progress.
 */
public class ShutdownRestrictions extends AbstractRestrictions {

	/**
	 * 关机过程中视为受限。
	 * Restricted during shutdown progress.
	 */
	@Override
	public boolean isRestricted(Player player, Class<? extends Restrictions> callingRestriction) {
		if (isInShutdownProgress(player)) {
			PacketSendUtility.sendMessage(player, "You are in shutdown progress!");
			return true;
		}
		return false;
	}

	/**
	 * 关机过程中禁止攻击。
	 * Attack forbidden during shutdown.
	 */
	@Override
	public boolean canAttack(Player player, VisibleObject target) {
		if (isInShutdownProgress(player)) {
			PacketSendUtility.sendMessage(player, "You cannot attack in Shutdown progress!");
			return false;
		}
		return true;
	}

	/**
	 * 技能影响目标不做额外限制。
	 * No extra restriction for skill affect.
	 */
	@Override
	public boolean canAffectBySkill(Player player, VisibleObject target, Skill skill) {
		return true;
	}

	/**
	 * 关机过程中禁止使用技能。
	 * Skill use forbidden during shutdown.
	 */
	@Override
	public boolean canUseSkill(Player player, Skill skill) {
		if (isInShutdownProgress(player)) {
			PacketSendUtility.sendMessage(player, "You cannot use skills in Shutdown progress!");
			return false;
		}
		return true;
	}

	/**
	 * 关机过程中禁止聊天。
	 * Chat forbidden during shutdown.
	 */
	@Override
	public boolean canChat(Player player) {
		if (isInShutdownProgress(player)) {
			PacketSendUtility.sendMessage(player, "You cannot chat in Shutdown progress!");
			return false;
		}
		return true;
	}

	/**
	 * 关机过程中禁止邀请进组。
	 * Group invite forbidden during shutdown.
	 */
	@Override
	public boolean canInviteToGroup(Player player, Player target) {
		if (isInShutdownProgress(player)) {
			PacketSendUtility.sendMessage(player, "You cannot invite members to group in Shutdown progress!");
			return false;
		}
		return true;
	}

	/**
	 * 关机过程中禁止邀请进联盟。
	 * Alliance invite forbidden during shutdown.
	 */
	@Override
	public boolean canInviteToAlliance(Player player, Player target) {
		if (isInShutdownProgress(player)) {
			PacketSendUtility.sendMessage(player, "You cannot invite members to alliance in Shutdown progress!");
			return false;
		}
		return true;
	}

	/**
	 * 关机过程中禁止邀请进军团联盟。
	 * League invite forbidden during shutdown.
	 */
	@Override
	public boolean canInviteToLeague(Player player, Player target) {
		if (isInShutdownProgress(player)) {
			PacketSendUtility.sendMessage(player, "You cannot invite members to league in Shutdown progress!");
			return false;
		}
		return true;
	}

	/**
	 * 关机过程中禁止换装。
	 * Equip change forbidden during shutdown.
	 */
	@Override
	public boolean canChangeEquip(Player player) {
		if (isInShutdownProgress(player)) {
			PacketSendUtility.sendMessage(player, "You cannot equip / unequip item in Shutdown progress!");
			return false;
		}
		return true;
	}

	/**
	 * 玩家控制器是否处于关机倒计时。
	 * Whether the player controller is in shutdown progress.
	 *
	 * @param player 玩家 / player
	 * @return 是否关机中 / whether shutting down
	 */
	private boolean isInShutdownProgress(Player player) {
		return player.getController().isInShutdownProgress();
	}
}
