package com.aionemu.gameserver.controllers.attack;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.aionemu.commons.callbacks.Callback;
import com.aionemu.commons.callbacks.CallbackResult;
import com.aionemu.commons.callbacks.metadata.ObjectCallback;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 生物仇恨列表：维护攻击者伤害/仇恨，并提供最高仇恨、最高伤害与掉落归属等查询。
 * most-damage / loot ownership. / most-damage / loot ownership.
 *
 * @author ATracer, KKnD
 */
@SuppressWarnings("rawtypes")
public class AggroList {

	/** 列表所属单位 / List owner */
	protected final Creature owner;
	/** 对象 ID → 仇恨条目 / object id → aggro entry */
	private Map<Integer, AggroInfo> aggroList = new ConcurrentHashMap<Integer, AggroInfo>();

	/**
	 * 为指定单位创建仇恨列表。
	 * Creates an aggro list for the given owner.
	 *
	 * @param owner 列表所属单位 / list owner
	 */
	public AggroList(Creature owner) {
		this.owner = owner;
	}

	/**
	 * 仅对敌人累加伤害与等量仇恨（召唤物/陷阱/宠物等计入，坠落伤害等不计入）。
	 * Adds damage and equal hate only from enemies (includes summons/traps/pets; excludes fall damage, etc.).
	 *
	 * attacker
	 * damage amount
	 */
	@ObjectCallback(AddDamageValueCallback.class)
	public void addDamage(Creature attacker, int damage) {
		if (!isAware(attacker)) {
			return;
		}
		AggroInfo ai = getAggroInfo(attacker);
		/**
		 * 当前按每次受到伤害等量增加仇恨，并额外广播仇恨。
	 * For now add hate equal to each damage received; additionally broadcast extra hate
		 */
		synchronized (ai) {
			ai.addDamage(damage);
			ai.addHate(damage);
		}
		owner.getAi2().onCreatureEvent(AIEventType.ATTACK, attacker);
	}

	/**
	 * 累加非伤害技能等产生的额外仇恨。
	 * Adds extra hate received from non-damage skill effects.
	 *
	 * hate source
	 * @param hate 仇恨增量 / hate amount
	 */
	public void addHate(final Creature creature, int hate) {
		if (!isAware(creature)) {
			return;
		}
		addHateValue(creature, hate);
	}

	/**
	 * 以 1 点仇恨开始仇恨该生物。
	 * Starts hating the creature by adding 1 hate.
	 *
	 * target creature
	 */
	public void startHate(final Creature creature) {
		addHateValue(creature, 1);
	}

	/**
	 * 内部：写入仇恨值并触发 AI/任务相关事件。
	 * Internal: writes hate and fires AI/quest-related events.
	 *
	 * hate source
	 * @param hate 仇恨增量 / hate amount
	 */
	protected void addHateValue(final Creature creature, int hate) {
		AggroInfo ai = getAggroInfo(creature);
		synchronized (ai) {
			ai.addHate(hate);
		}
		if (creature instanceof Player && owner instanceof Npc) {
			owner.getKnownList().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					if (MathUtil.isIn3dRange(owner, player, 50)) {
						GameEngineServices.questEngine().onAddAggroList(new QuestEnv(owner, player, 0, 0));
					}
				}
			});
		}
		owner.getAi2().onCreatureEvent(AIEventType.ATTACK, creature);
	}

	/**
	 * 返回造成伤害最高的玩家/队伍/联盟对象。
	 * Returns the player/group/alliance that dealt the most damage.
	 *
	 * @return 最高伤害来源 / most-damage source
	 */
	public AionObject getMostDamage() {
		AionObject mostDamage = null;
		int maxDamage = 0;
		for (AggroInfo ai : getFinalDamageList(true)) {
			if (ai.getAttacker() == null || owner.equals(ai.getAttacker())) {
				continue;
			}
			if (ai.getDamage() > maxDamage) {
				mostDamage = ai.getAttacker();
				maxDamage = ai.getDamage();
			}
		}
		return mostDamage;
	}

	/**
	 * 返回最高伤害玩家或队伍所属种族。
	 * Returns the race of the most-damage player or group winner.
	 *
	 * @return 胜出种族，可能为 null / winner race, or null
	 */
	public Race getPlayerWinnerRace() {
		AionObject winner = getMostDamage();
		if (winner instanceof PlayerGroup) {
			return ((PlayerGroup) winner).getRace();
		} else if (winner instanceof Player) {
			return ((Player) winner).getRace();
		}
		return null;
	}

	/**
	 * 返回造成伤害最高的玩家（含宠物伤害归并）。
	 * Returns the player who dealt the most damage (pet damage merged).
	 *
	 * @return 最高伤害玩家 / most-damage player
	 */
	public Player getMostPlayerDamage() {
		if (aggroList.isEmpty()) {
			return null;
		}
		Player mostDamage = null;
		int maxDamage = 0;

		// 使用最终伤害列表，一并获取宠物伤害。 / Use final damage list to get pet damage as well.
		for (AggroInfo ai : this.getFinalDamageList(false)) {
			if (ai.getDamage() > maxDamage && ai.getAttacker() instanceof Player) {
				mostDamage = (Player) ai.getAttacker();
				maxDamage = ai.getDamage();
			}
		}
		return mostDamage;
	}

	/**
	 * 在指定队伍成员中返回造成伤害最高的玩家；导师则回退到最高等级成员。
	 * Returns the most-damaging player among the team; mentors fall back to the highest-level member.
	 *
	 * @param team 队伍成员集合 / team members
	 * @param highestLevel 队伍最高等级 / highest level in the team
	 * @return 最高伤害玩家 / most-damage player
	 */
	public Player getMostPlayerDamageOfMembers(Collection<Player> team, int highestLevel) {
		if (aggroList.isEmpty()) {
			return null;
		}
		Player mostDamage = null;
		int maxDamage = 0;

		// 使用最终伤害列表，一并获取宠物伤害。 / Use final damage list to get pet damage as well.
		for (AggroInfo ai : this.getFinalDamageList(false)) {
			if (!(ai.getAttacker() instanceof Player)) {
				continue;
			}

			if (!team.contains((Player) ai.getAttacker())) {
				continue;
			}

			if (ai.getDamage() > maxDamage) {
				mostDamage = (Player) ai.getAttacker();
				maxDamage = ai.getDamage();
			}
		}

		if (mostDamage != null && mostDamage.isMentor()) {
			for (Player member : team) {
				if (member.getLevel() == highestLevel) {
					mostDamage = member;
				}
			}
		}
		return mostDamage;
	}

	/**
	 * 返回当前仇恨最高的生物；已死亡者仇恨清零。
	 * Returns the most-hated creature; hate of dead attackers is zeroed.
	 *
	 * @return 最高仇恨生物 / most-hated creature
	 */
	public Creature getMostHated() {
		if (aggroList.isEmpty()) {
			return null;
		}
		Creature mostHated = null;
		int maxHate = 0;

		for (AggroInfo ai : aggroList.values()) {
			if (ai == null) {
				continue;
			}
			// 仇恨列表仅包含生物 / aggroList will never contain anything but creatures
			Creature attacker = (Creature) ai.getAttacker();

			if (attacker.getLifeStats().isAlreadyDead()) {
				ai.setHate(0);
			}
			if (ai.getHate() > maxHate) {
				mostHated = attacker;
				maxHate = ai.getHate();
			}
		}
		return mostHated;
	}

	/**
	 * 判断指定生物是否为当前最高仇恨目标。
	 * Returns whether the given creature is currently the most hated.
	 *
	 * @param creature 待判断生物 / creature to check
	 * @return 是否最高仇恨 / whether most hated
	 */
	public boolean isMostHated(Creature creature) {
		if (creature == null || creature.getLifeStats().isAlreadyDead()) {
			return false;
		}
		Creature mostHated = getMostHated();
		return mostHated != null && mostHated.equals(creature);
	}

	/**
	 * 若已在仇恨列表中，则追加仇恨值。
	 * Adds hate only if the creature is already on the hate list.
	 *
	 * target creature
	 * @param value 仇恨增量 / hate amount
	 */
	public void notifyHate(Creature creature, int value) {
		if (isHating(creature)) {
			addHate(creature, value);
		}
	}

	/**
	 * 停止对该可见对象的仇恨（仇恨置 0，条目保留）。
	 * Stops hating the visible object (sets hate to 0, keeps the entry).
	 *
	 * target object
	 */
	public void stopHating(VisibleObject creature) {
		AggroInfo aggroInfo = aggroList.get(creature.getObjectId());
		if (aggroInfo != null) {
			aggroInfo.setHate(0);
		}
	}

	/**
	 * 从仇恨列表中完全移除该生物。
	 * Completely removes the creature from the aggro list.
	 *
	 * target creature
	 */
	public void remove(Creature creature) {
		aggroList.remove(creature.getObjectId());
	}

	/**
	 * 清空整张仇恨列表。
	 * Clears the entire aggro list.
	 */
	public void clear() {
		aggroList.clear();
	}

	/**
	 * 获取或创建指定生物的仇恨条目。
	 * Gets or creates the aggro entry for the given creature.
	 *
	 * target creature
	 * aggro info
	 */
	public AggroInfo getAggroInfo(Creature creature) {
		AggroInfo ai = aggroList.get(creature.getObjectId());
		if (ai == null) {
			ai = new AggroInfo(creature);
			AggroInfo existing = aggroList.putIfAbsent(creature.getObjectId(), ai);
			if (existing != null) {
				ai = existing;
			}
		}
		return ai;
	}

	/**
	 * 判断是否已仇恨该生物。
	 * Returns whether this list is already hating the creature.
	 *
	 * target creature
	 *
	 * @param creature @return 是否在列表中 / whether present
	 */
	public boolean isHating(Creature creature) {
		return aggroList.containsKey(creature.getObjectId());
	}

	/**
	 * 返回仇恨条目快照集合。
	 * Returns a snapshot collection of aggro entries.
	 *
	 * @return 仇恨列表快照 / aggro list snapshot
	 */
	public Collection<AggroInfo> getList() {
		return new ArrayList<AggroInfo>(aggroList.values());
	}

	/**
	 * 返回列表中所有攻击者的伤害总和。
	 * Returns the sum of all damage recorded in the list.
	 *
	 * total damage
	 */
	public int getTotalDamage() {
		int totalDamage = 0;
		for (AggroInfo ai : aggroList.values()) {
			totalDamage += ai.getDamage();
		}
		return totalDamage;
	}

	/**
	 * 返回最终伤害列表：将 NPC/召唤物伤害归并到主人，可选合并队伍伤害。
	 * Returns the final damage list with pet/summon damage merged to masters; optionally merges group damage.
	 *
	 * @param mergeGroupDamage 是否合并队伍伤害 / whether to merge group damage
	 * @return 最终伤害条目集合 / final damage entries
	 */
	public Collection<AggroInfo> getFinalDamageList(boolean mergeGroupDamage) {
		Map<Integer, AggroInfo> list = new HashMap<Integer, AggroInfo>();
		for (AggroInfo ai : aggroList.values()) {
			// 仅获取主人以控制伤害。 / Get master only to control damage.
			Creature creature = ((Creature) ai.getAttacker()).getMaster();

			// 不计入已知列表外生物的伤害。 / Don't include damage from creatures outside the known list.
			if (creature == null || !owner.getKnownList().knowns(creature)) {
				continue;
			}

			if (mergeGroupDamage) {
				AionObject source;

				if (creature instanceof Player && ((Player) creature).isInTeam()) {
					source = ((Player) creature).getCurrentTeam();
				} else {
					source = creature;
				}

				if (list.containsKey(source.getObjectId())) {
					list.get(source.getObjectId()).addDamage(ai.getDamage());
				} else {
					AggroInfo aggro = new AggroInfo(source);
					aggro.setDamage(ai.getDamage());
					list.put(source.getObjectId(), aggro);
				}
			} else if (list.containsKey(creature.getObjectId())) {
				// 召唤或其他协助 / Summon or other assistance
				list.get(creature.getObjectId()).addDamage(ai.getDamage());
			} else {
				// 创建独立对象，避免污染当前列表。 / Create a separate object so we don't taint current list.
				AggroInfo aggro = new AggroInfo(creature);
				aggro.addDamage(ai.getDamage());
				list.put(creature.getObjectId(), aggro);
			}
		}
		return list.values();
	}

	/**
	 * 判断列表是否感知该生物（非自身，且敌对或部落敌对）。
	 * Returns whether this list is aware of the creature (not self, and enemy or tribe-hostile).
	 *
	 * @param creature 待判断生物 / creature to check
	 * whether aware
	 */
	protected boolean isAware(Creature creature) {
		return creature != null && !creature.getObjectId().equals(owner.getObjectId()) && (creature.isEnemy(owner)
				|| DataManager.TRIBE_RELATIONS_DATA.isHostileRelation(owner.getTribe(), creature.getTribe()));
	}

	/**
	 * 伤害累加后的回调钩子基类。
	 * Base callback hook invoked after damage is added.
	 */
	public static abstract class AddDamageValueCallback implements Callback<AggroList> {

		/**
		 * 调用前：始终继续。
		 * Before call: always continue.
		 *
		 * @param obj 仇恨列表 / aggro list
		 * arguments
		 * continue result
		 */
		@Override
		public final CallbackResult beforeCall(AggroList obj, Object[] args) {
			return CallbackResult.newContinue();
		}

		/**
		 * 调用后：若感知攻击者则触发 {@link #onDamageAdded}。
		 * After call: invokes {@link #onDamageAdded} when the attacker is aware.
		 *
		 * @param obj 仇恨列表 / aggro list
		 * @param args 参数（攻击者、伤害） / arguments (attacker, damage)
		 * @param methodResult 方法返回值 / method result
		 * continue result
		 */
		@Override
		public final CallbackResult afterCall(AggroList obj, Object[] args, Object methodResult) {

			Creature creature = (Creature) args[0];
			Integer damage = (Integer) args[1];

			if (obj.isAware(creature)) {
				onDamageAdded(creature, damage);
			}
			return CallbackResult.newContinue();
		}

		/**
		 * 返回回调基类类型。
		 * Returns the callback base class type.
		 *
		 * base class
		 */
		@Override
		public final Class<? extends Callback> getBaseClass() {
			return AddDamageValueCallback.class;
		}

		/**
		 * 伤害成功写入后的业务钩子。
		 * Business hook after damage has been recorded.
		 *
		 * attacker
		 * damage amount
		 */
		public abstract void onDamageAdded(Creature creature, int damage);
	}
}
