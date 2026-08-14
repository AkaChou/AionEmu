package com.aionemu.gameserver.restrictions;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.lang3.ArrayUtils;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Skill;

/**
 * 限制管理器：按方法维度注册/注销限制实现，并对外提供统一校验入口。
 * Restriction manager that registers/unregisters implementations per method and exposes unified checks.
 */
public final class RestrictionsManager {
	/**
	 * 工具类，禁止实例化。
	 * Utility class; not instantiable.
	 */
	private RestrictionsManager() {
	}

	/**
	 * 按 {@link RestrictionMode} 分桶的限制实现数组。
	 * Restriction implementations bucketed by {@link RestrictionMode}.
	 */
	private static final Restrictions[][] RESTRICTIONS = new Restrictions[RestrictionMode.VALUES.length][0];

	/**
	 * 激活限制：扫描未禁用方法并按优先级插入对应桶。
	 * Activates a restriction by scanning non-disabled methods and inserting into priority-ordered buckets.
	 *
	 * @param restriction 限制实现 / restriction implementation
	 */
	public synchronized static void activate(Restrictions restriction) {
		for (Method method : restriction.getClass().getMethods()) {
			RestrictionMode mode = RestrictionMode.parse(method);
			if (mode == null) {
				continue;
			}
			if (method.getAnnotation(DisabledRestriction.class) != null) {
				continue;
			}
			Restrictions[] restrictions = RESTRICTIONS[mode.ordinal()];
			if (!ArrayUtils.contains(restrictions, restriction)) {
				restrictions = Arrays.copyOf(restrictions, restrictions.length + 1);
				restrictions[restrictions.length - 1] = restriction;
			}
			Arrays.sort(restrictions, mode);
			RESTRICTIONS[mode.ordinal()] = restrictions;
		}
	}

	/**
	 * 从所有模式桶中移除限制实现。
	 * Removes the restriction from all mode buckets.
	 *
	 * @param restriction 限制实现 / restriction implementation
	 */
	public synchronized static void deactivate(Restrictions restriction) {
		for (RestrictionMode mode : RestrictionMode.VALUES) {
			Restrictions[] restrictions = RESTRICTIONS[mode.ordinal()];
			for (int index; (index = ArrayUtils.indexOf(restrictions, restriction)) != -1;) {
				restrictions = (Restrictions[]) ArrayUtils.remove(restrictions, index);
			}
			RESTRICTIONS[mode.ordinal()] = restrictions;
		}
	}

	static {
		// 常规玩法限制 / normal-play restrictions
		activate(new PlayerRestrictions());
		// 关机倒计时限制 / shutdown restrictions
		activate(new ShutdownRestrictions());
		// 监狱限制 / prison restrictions
		activate(new PrisonRestrictions());
	}

	/**
	 * 判断玩家是否处于指定限制状态。
	 * Checks whether the player is under the given restriction kind.
	 *
	 * @param player 玩家 / player
	 * @param callingRestriction 调用限制类型 / calling restriction type
	 * @return true 表示被限制 / true when restricted
	 */
	public static boolean isRestricted(Player player, Class<? extends Restrictions> callingRestriction) {
		if (player == null) {
			return true;
		}
		for (Restrictions restrictions : RESTRICTIONS[RestrictionMode.isRestricted.ordinal()]) {
			if (!restrictions.isRestricted(player, callingRestriction)) {
				return false;
			}
		}
		return false;
	}

	/**
	 * 是否允许攻击目标。
	 * Whether the player may attack the target.
	 *
	 * @param player 玩家 / player
	 * @param target 目标 / target
	 * @return 允许时为 true / true when allowed
	 */
	public static boolean canAttack(Player player, VisibleObject target) {
		for (Restrictions restrictions : RESTRICTIONS[RestrictionMode.canAttack.ordinal()]) {
			if (!restrictions.canAttack(player, target)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 是否允许技能影响目标。
	 * Whether a skill may affect the target.
	 *
	 * @param player 玩家 / player
	 * @param target 目标 / target
	 * @param skill 技能 / skill
	 * @return 允许时为 true / true when allowed
	 */
	public static boolean canAffectBySkill(Player player, VisibleObject target, Skill skill) {
		for (Restrictions restrictions : RESTRICTIONS[RestrictionMode.canAffectBySkill.ordinal()]) {
			if (!restrictions.canAffectBySkill(player, target, skill)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 是否允许使用技能。
	 * Whether the player may use the skill.
	 *
	 * @param player 玩家 / player
	 * @param skill 技能 / skill
	 * @return 允许时为 true / true when allowed
	 */
	public static boolean canUseSkill(Player player, Skill skill) {
		for (Restrictions restrictions : RESTRICTIONS[RestrictionMode.canUseSkill.ordinal()]) {
			if (!restrictions.canUseSkill(player, skill)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 是否允许聊天。
	 * Whether the player may chat.
	 *
	 * @param player 玩家 / player
	 * @return 允许时为 true / true when allowed
	 */
	public static boolean canChat(Player player) {
		for (Restrictions restrictions : RESTRICTIONS[RestrictionMode.canChat.ordinal()]) {
			if (!restrictions.canChat(player)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 是否允许邀请进组。
	 * Whether the player may invite to a group.
	 *
	 * @param player 玩家 / player
	 * @param target 目标 / target player
	 * @return 允许时为 true / true when allowed
	 */
	public static boolean canInviteToGroup(Player player, Player target) {
		for (Restrictions restrictions : RESTRICTIONS[RestrictionMode.canInviteToGroup.ordinal()]) {
			if (!restrictions.canInviteToGroup(player, target)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 是否允许邀请进联盟。
	 * Whether the player may invite to an alliance.
	 *
	 * @param player 玩家 / player
	 * @param target 目标 / target player
	 * @return 允许时为 true / true when allowed
	 */
	public static boolean canInviteToAlliance(Player player, Player target) {
		for (Restrictions restrictions : RESTRICTIONS[RestrictionMode.canInviteToAlliance.ordinal()]) {
			if (!restrictions.canInviteToAlliance(player, target)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 是否允许邀请进军团联盟。
	 * Whether the player may invite to a league.
	 *
	 * @param player 玩家 / player
	 * @param target 目标 / target player
	 * @return 允许时为 true / true when allowed
	 */
	public static boolean canInviteToLeague(Player player, Player target) {
		for (Restrictions restrictions : RESTRICTIONS[RestrictionMode.canInviteToLeague.ordinal()]) {
			if (!restrictions.canInviteToLeague(player, target)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 是否允许更换装备。
	 * Whether the player may change equipment.
	 *
	 * @param player 玩家 / player
	 * @return 允许时为 true / true when allowed
	 */
	public static boolean canChangeEquip(Player player) {
		for (Restrictions restrictions : RESTRICTIONS[RestrictionMode.canChangeEquip.ordinal()]) {
			if (!restrictions.canChangeEquip(player)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 是否允许交易；已死亡时额外禁止。
	 * Whether the player may trade; also forbids when already dead.
	 *
	 * @param player 玩家 / player
	 * @return 允许时为 true / true when allowed
	 */
	public static boolean canTrade(Player player) {
		for (Restrictions restrictions : RESTRICTIONS[RestrictionMode.canTrade.ordinal()]) {
			if (!restrictions.canTrade(player)) {
				return false;
			}
		}
		if (player.getLifeStats().isAlreadyDead()) {
			return false;
		}
		return true;
	}

	/**
	 * 是否允许使用仓库。
	 * Whether the player may use warehouse.
	 *
	 * @param player 玩家 / player
	 * @return 允许时为 true / true when allowed
	 */
	public static boolean canUseWarehouse(Player player) {
		for (Restrictions restrictions : RESTRICTIONS[RestrictionMode.canUseWarehouse.ordinal()]) {
			if (!restrictions.canUseWarehouse(player)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 是否允许使用物品。
	 * Whether the player may use the item.
	 *
	 * @param player 玩家 / player
	 * @param item 物品 / item
	 * @return 允许时为 true / true when allowed
	 */
	public static boolean canUseItem(Player player, Item item) {
		for (Restrictions restrictions : RESTRICTIONS[RestrictionMode.canUseItem.ordinal()]) {
			if (!restrictions.canUseItem(player, item)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 限制方法模式：与 {@link Restrictions} 方法一一对应，并按优先级比较实现。
	 * Restriction method mode: one-to-one with {@link Restrictions} methods; compares implementations by priority.
	 */
	private static enum RestrictionMode implements Comparator<Restrictions> {
		isRestricted, canAttack, canAffectBySkill, canUseSkill, canChat, canInviteToGroup, canInviteToAlliance,
		canInviteToLeague, canChangeEquip, canTrade, canUseWarehouse, canUseItem;

		private final Method METHOD;

		private RestrictionMode() {
			for (Method method : Restrictions.class.getMethods()) {
				if (name().equals(method.getName())) {
					METHOD = method;
					return;
				}
			}
			throw new InternalError();
		}

		private boolean equalsMethod(Method method) {
			if (!METHOD.getName().equals(method.getName())) {
				return false;
			}
			if (!METHOD.getReturnType().equals(method.getReturnType())) {
				return false;
			}
			return Arrays.equals(METHOD.getParameterTypes(), method.getParameterTypes());
		}

		private static final RestrictionMode[] VALUES = RestrictionMode.values();

		private static RestrictionMode parse(Method method) {
			for (RestrictionMode mode : VALUES) {
				if (mode.equalsMethod(method)) {
					return mode;
				}
			}
			return null;
		}

		@Override
		public int compare(Restrictions o1, Restrictions o2) {
			return Double.compare(getPriority(o2), getPriority(o1));
		}

		private double getPriority(Restrictions restriction) {
			RestrictionPriority a1 = getMatchingMethod(restriction.getClass()).getAnnotation(RestrictionPriority.class);
			if (a1 != null) {
				return a1.value();
			}
			RestrictionPriority a2 = restriction.getClass().getAnnotation(RestrictionPriority.class);
			if (a2 != null) {
				return a2.value();
			}
			return RestrictionPriority.DEFAULT_PRIORITY;
		}

		private Method getMatchingMethod(Class<? extends Restrictions> clazz) {
			for (Method method : clazz.getMethods()) {
				if (equalsMethod(method)) {
					return method;
				}
			}
			throw new InternalError();
		}
	}
}
