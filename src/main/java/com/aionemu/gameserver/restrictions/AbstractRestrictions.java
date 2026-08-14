package com.aionemu.gameserver.restrictions;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Skill;

/**
 * 限制实现基类：提供激活/停用，未覆盖方法默认禁用。
 * Base restriction implementation with activate/deactivate; uncovered methods are disabled by default.
 *
 * @author lord_rex
 */
public abstract class AbstractRestrictions implements Restrictions {

	/**
	 * 激活本限制。
	 * Activates this restriction.
	 */
	public void activate() {
		RestrictionsManager.activate(this);
	}

	/**
	 * 停用本限制。
	 * Deactivates this restriction.
	 */
	public void deactivate() {
		RestrictionsManager.deactivate(this);
	}

	/**
	 * 按实现类哈希，避免同类重复激活。
	 * Hashes by implementation class to avoid duplicate activation of the same type.
	 *
	 * @return 类哈希 / class hash
	 */
	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	/**
	 * 同类实例视为相等，避免多次激活同一限制类型。
	 * Same implementation class is equal to avoid multi-activation.
	 *
	 * @param obj 对象 / object
	 * @return 是否同类 / whether same class
	 */
	@Override
	public boolean equals(Object obj) {
		return getClass().equals(obj.getClass());
	}

	@Override
	@DisabledRestriction
	public boolean isRestricted(Player player, Class<? extends Restrictions> callingRestriction) {
		throw new AbstractMethodError();
	}

	@Override
	@DisabledRestriction
	public boolean canAttack(Player player, VisibleObject target) {
		throw new AbstractMethodError();
	}

	@Override
	@DisabledRestriction
	public boolean canAffectBySkill(Player player, VisibleObject target, Skill skill) {
		throw new AbstractMethodError();
	}

	@Override
	@DisabledRestriction
	public boolean canUseSkill(Player player, Skill skill) {
		throw new AbstractMethodError();
	}

	@Override
	@DisabledRestriction
	public boolean canChat(Player player) {
		throw new AbstractMethodError();
	}

	@Override
	@DisabledRestriction
	public boolean canInviteToGroup(Player player, Player target) {
		throw new AbstractMethodError();
	}

	@Override
	@DisabledRestriction
	public boolean canChangeEquip(Player player) {
		throw new AbstractMethodError();
	}

	@Override
	@DisabledRestriction
	public boolean canUseWarehouse(Player player) {
		throw new AbstractMethodError();
	}

	@Override
	@DisabledRestriction
	public boolean canTrade(Player player) {
		throw new AbstractMethodError();
	}

	@Override
	@DisabledRestriction
	public boolean canUseItem(Player player, Item item) {
		throw new AbstractMethodError();
	}
}
