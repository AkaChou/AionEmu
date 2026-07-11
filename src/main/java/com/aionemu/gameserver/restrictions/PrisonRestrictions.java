package com.aionemu.gameserver.restrictions;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapType;

/**
 * 监狱限制：在监狱地图或服刑状态禁止攻击、技能、聊天、组队与换装等。
 * Prison restrictions: blocks attack, skill, chat, invite and equip while imprisoned.
 */
public class PrisonRestrictions extends AbstractRestrictions {

	/**
	 * 监狱中视为受限。
	 * Restricted while in prison.
	 */
	@Override
	public boolean isRestricted(Player player, Class<? extends Restrictions> callingRestriction) {
		if (isInPrison(player)) {
			PacketSendUtility.sendMessage(player, "You are in prison!");
			return true;
		}
		return false;
	}

	/**
	 * 监狱中禁止攻击。
	 * Attack forbidden in prison.
	 */
	@Override
	public boolean canAttack(Player player, VisibleObject target) {
		if (isInPrison(player)) {
			PacketSendUtility.sendMessage(player, "You cannot attack in prison!");
			return false;
		}
		return true;
	}

	/**
	 * 监狱中禁止使用技能。
	 * Skill use forbidden in prison.
	 */
	@Override
	public boolean canUseSkill(Player player, Skill skill) {
		if (isInPrison(player)) {
			PacketSendUtility.sendMessage(player, "You cannot use skills in prison!");
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
	 * 监狱中禁止聊天。
	 * Chat forbidden in prison.
	 */
	@Override
	public boolean canChat(Player player) {
		if (isInPrison(player)) {
			PacketSendUtility.sendMessage(player, "You cannot chat in prison!");
			return false;
		}
		return true;
	}

	/**
	 * 监狱中禁止邀请进组。
	 * Group invite forbidden in prison.
	 */
	@Override
	public boolean canInviteToGroup(Player player, Player target) {
		if (isInPrison(player)) {
			PacketSendUtility.sendMessage(player, "You cannot invite members to group in prison!");
			return false;
		}
		return true;
	}

	/**
	 * 监狱中禁止邀请进联盟。
	 * Alliance invite forbidden in prison.
	 */
	@Override
	public boolean canInviteToAlliance(Player player, Player target) {
		if (isInPrison(player)) {
			PacketSendUtility.sendMessage(player, "You cannot invite members to alliance in prison!");
			return false;
		}
		return true;
	}

	/**
	 * 监狱中禁止邀请进军团联盟。
	 * League invite forbidden in prison.
	 */
	@Override
	public boolean canInviteToLeague(Player player, Player target) {
		if (isInPrison(player)) {
			PacketSendUtility.sendMessage(player, "You cannot invite members to league in prison!");
			return false;
		}
		return true;
	}

	/**
	 * 监狱中禁止换装。
	 * Equip change forbidden in prison.
	 */
	@Override
	public boolean canChangeEquip(Player player) {
		if (isInPrison(player)) {
			PacketSendUtility.sendMessage(player, "You cannot equip / unequip item in prison!");
			return false;
		}
		return true;
	}

	/**
	 * 监狱中禁止使用物品。
	 * Item use forbidden in prison.
	 */
	@Override
	public boolean canUseItem(Player player, Item item) {
		if (isInPrison(player)) {
			PacketSendUtility.sendMessage(player, "You cannot use item in prison!");
			return false;
		}
		return true;
	}

	/**
	 * 是否在监狱状态或监狱地图。
	 * Whether the player is imprisoned or on a prison map.
	 *
	 * @param player 玩家 / player
	 * @return 是否在监狱 / whether in prison
	 */
	private boolean isInPrison(Player player) {
		return player.isInPrison() || player.getWorldId() == WorldMapType.DE_PRISON.getId()
				|| player.getWorldId() == WorldMapType.DF_PRISON.getId();
	}
}
