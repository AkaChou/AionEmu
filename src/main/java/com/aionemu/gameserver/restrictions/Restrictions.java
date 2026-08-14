package com.aionemu.gameserver.restrictions;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Skill;

/**
 * 玩家行为限制契约：攻击、技能、聊天、组队、装备、仓库、交易与物品使用。
 * Player action restriction contract: attack, skill, chat, party, equip, warehouse, trade and item use.
 */
public interface Restrictions {

	/**
	 * 是否处于指定限制状态。
	 * Whether the player is under the given restriction kind.
	 *
	 * @param player 玩家 / player
	 * @param callingRestriction 调用限制类型 / calling restriction type
	 * @return true 表示被限制 / true when restricted
	 */
	public boolean isRestricted(Player player, Class<? extends Restrictions> callingRestriction);

	/**
	 * 是否允许攻击目标。
	 * Whether the player may attack the target.
	 *
	 * @param player 玩家 / player
	 * @param target 目标 / target
	 * @return 允许时为 true / true when allowed
	 */
	public boolean canAttack(Player player, VisibleObject target);

	/**
	 * 是否允许技能影响目标。
	 * Whether a skill may affect the target.
	 *
	 * @param player 玩家 / player
	 * @param target 目标 / target
	 * @param skill 技能 / skill
	 * @return 允许时为 true / true when allowed
	 */
	public boolean canAffectBySkill(Player player, VisibleObject target, Skill skill);

	/**
	 * 是否允许使用技能。
	 * Whether the player may use the skill.
	 *
	 * @param player 玩家 / player
	 * @param skill 技能 / skill
	 * @return 允许时为 true / true when allowed
	 */
	public boolean canUseSkill(Player player, Skill skill);

	/**
	 * 是否允许聊天。
	 * Whether the player may chat.
	 *
	 * @param player 玩家 / player
	 * @return 允许时为 true / true when allowed
	 */
	public boolean canChat(Player player);

	/**
	 * 是否允许邀请进组。
	 * Whether the player may invite to a group.
	 *
	 * @param player 玩家 / player
	 * @param target 目标玩家 / target player
	 * @return 允许时为 true / true when allowed
	 */
	public boolean canInviteToGroup(Player player, Player target);

	/**
	 * 是否允许邀请进联盟。
	 * Whether the player may invite to an alliance.
	 *
	 * @param player 玩家 / player
	 * @param target 目标玩家 / target player
	 * @return 允许时为 true / true when allowed
	 */
	public boolean canInviteToAlliance(Player player, Player target);

	/**
	 * 是否允许邀请进军团联盟。
	 * Whether the player may invite to a league.
	 *
	 * @param player 玩家 / player
	 * @param target 目标玩家 / target player
	 * @return 允许时为 true / true when allowed
	 */
	public boolean canInviteToLeague(Player player, Player target);

	/**
	 * 是否允许更换装备。
	 * Whether the player may change equipment.
	 *
	 * @param player 玩家 / player
	 * @return 允许时为 true / true when allowed
	 */
	public boolean canChangeEquip(Player player);

	/**
	 * 是否允许使用仓库。
	 * Whether the player may use warehouse.
	 *
	 * @param player 玩家 / player
	 * @return 允许时为 true / true when allowed
	 */
	public boolean canUseWarehouse(Player player);

	/**
	 * 是否允许交易。
	 * Whether the player may trade.
	 *
	 * @param player 玩家 / player
	 * @return 允许时为 true / true when allowed
	 */
	public boolean canTrade(Player player);

	/**
	 * 是否允许使用物品。
	 * Whether the player may use the item.
	 *
	 * @param player 玩家 / player
	 * @param item 物品 / item
	 * @return 允许时为 true / true when allowed
	 */
	public boolean canUseItem(Player player, Item item);
}
