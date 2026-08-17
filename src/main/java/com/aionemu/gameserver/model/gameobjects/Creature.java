package com.aionemu.gameserver.model.gameobjects;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameEventServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai2.AI2;
import com.aionemu.gameserver.ai2.AI2Engine;
import com.aionemu.gameserver.configs.main.SkillConfig;
import com.aionemu.gameserver.controllers.CreatureController;
import com.aionemu.gameserver.controllers.ObserveController;
import com.aionemu.gameserver.controllers.attack.AggroList;
import com.aionemu.gameserver.controllers.effect.EffectController;
import com.aionemu.gameserver.controllers.movement.MoveController;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.TribeClass;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureSeeState;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.gameobjects.state.CreatureVisualState;
import com.aionemu.gameserver.model.stats.container.CreatureGameStats;
import com.aionemu.gameserver.model.stats.container.CreatureLifeStats;
import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;
import com.aionemu.gameserver.model.templates.item.ItemAttackType;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.zone.ZoneType;
import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster.BroadcastMode;
import com.aionemu.gameserver.world.MapRegion;
import com.aionemu.gameserver.world.WorldPosition;
import com.aionemu.gameserver.world.zone.ZoneName;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 生物游戏对象。
 * Creature game object.
 */
@Slf4j

public abstract class Creature extends VisibleObject {
	protected AI2 ai2;
	private boolean isDespawnDelayed = false;
	private CreatureLifeStats<? extends Creature> lifeStats;
	private CreatureGameStats<? extends Creature> gameStats;
	private EffectController effectController;
	protected MoveController moveController;
	private int state = CreatureState.ACTIVE.getId();
	private int visualState = CreatureVisualState.VISIBLE.getId();
	private int seeState = CreatureSeeState.NORMAL.getId();
	private volatile Skill castingSkill;
	private Map<Integer, Long> skillCoolDowns;
	private Map<Integer, Long> skillCoolDownsBase;
	private ObserveController observeController;
	private TransformModel transformModel;
	private final AggroList aggroList;
	private byte adminFlags = 0;
	private Item usingItem;
	private final transient byte[] zoneTypes = new byte[ZoneType.values().length];
	private int skillNumber;
	private int attackedCount;
	private long spawnTime = System.currentTimeMillis();
	private int PulledMulti = 1;
	/** 真实 stat ratio，1000 表示 1.0。 / Retail stat ratio, 1000 = 1.0. */
	private int statRatio = 1000;

	/**
	 * 构造生物。
	 * Constructs a creature.
	 *
	 * @param objId 对象 ID / object id
	 * @param controller 生物控制器 / creature controller
	 * @param spawnTemplate 生成模板 / spawn template
	 * @param objectTemplate 对象模板 / object template
	 * @param position 世界位置 / world position
	 */
	public Creature(int objId, CreatureController<? extends Creature> controller, SpawnTemplate spawnTemplate,
			VisibleObjectTemplate objectTemplate, WorldPosition position) {
		super(objId, controller, spawnTemplate, objectTemplate, position);
		this.observeController = new ObserveController();
		this.setTransformModel(new TransformModel(this));
		if (spawnTemplate != null && spawnTemplate.getModel() != null) {
			if (spawnTemplate.getModel().getTribe() != null) {
				getTransformModel().setTribe(spawnTemplate.getModel().getTribe(), true);
			}
		}
		this.aggroList = createAggroList();
	}

	/** 返回移动控制器 / Returns the move controller */
	public MoveController getMoveController() {
		return this.moveController;
	}

	protected AggroList createAggroList() {
		return new AggroList(this);
	}

	/**
	 * 返回此生物的控制器。
	 * Returns the controller of this Creature object.
	 *
	 * @return 生物控制器 / creature controller
	 */
	@Override
	public CreatureController<? extends Creature> getController() {
		return (CreatureController<?>) super.getController();
	}

	/**
	 * 返回生命属性。
	 * Returns the life stats.
	 *
	 * @return 生命属性 / the lifeStats
	 */
	public CreatureLifeStats<? extends Creature> getLifeStats() {
		return lifeStats;
	}

	/**
	 * 设置生命属性。
	 * Sets the life stats.
	 *
	 * @param lifeStats 要设置的生命属性 / the lifeStats to set
	 */
	public void setLifeStats(CreatureLifeStats<? extends Creature> lifeStats) {
		this.lifeStats = lifeStats;
	}

	/**
	 * 返回游戏属性。
	 * Returns the game stats.
	 *
	 * @return 游戏属性 / the gameStats
	 */
	public CreatureGameStats<? extends Creature> getGameStats() {
		return gameStats;
	}

	/**
	 * 设置游戏属性。
	 * Sets the game stats.
	 *
	 * @param gameStats 要设置的游戏属性 / the gameStats to set
	 */
	public void setGameStats(CreatureGameStats<? extends Creature> gameStats) {
		this.gameStats = gameStats;
	}

	/** 获取等级。 / Returns the level. */
	public abstract byte getLevel();

	/**
	 * 返回效果控制器。
	 * Returns the effect controller.
	 *
	 * @return 效果控制器 / the effectController
	 */
	public EffectController getEffectController() {
		return effectController;
	}

	/**
	 * 设置效果控制器。
	 * Sets the effect controller.
	 *
	 * @param effectController 要设置的效果控制器 / the effectController to set
	 */
	public void setEffectController(EffectController effectController) {
		this.effectController = effectController;
	}

	/** 返回 AI 2 / Returns the ai 2 */
	public AI2 getAi2() {
		return ai2 != null ? ai2 : GameEngineServices.ai2Engine().setupAI("dummy", this);
	}

	/** 设置 AI 2 / Sets the ai 2 */
	public void setAi2(AI2 ai2) {
		this.ai2 = ai2;
	}

	/**
	 * 是否延迟删除。
	 * Whether the despawn is delayed.
	 *
	 * @return 是否延迟删除 / whether delete delayed
	  */
	public boolean isDeleteDelayed() {
		return isDespawnDelayed;
	}

	/** 设置延迟消失 / Sets whether the despawn is delayed */
	public void setDespawnDelayed(boolean delayed) {
		isDespawnDelayed = delayed;
	}

	/** 是否旗帜 / Whether flag. */
	public boolean isFlag() {
		return false;
	}

	/**
	 * 生物是否正在施放技能。
	 * Whether the creature is casting some skill.
	 *
	 * @return 是否正在施放 / whether casting
	 */
	public boolean isCasting() {
		return castingSkill != null;
	}

	/**
	 * 设置当前施放技能，技能结束时为 null。
	 * Sets the current casting skill, or null when the skill ends.
	 *
	 * @param castingSkill 当前施放技能 / current casting skill
	 */
	public synchronized void setCasting(Skill castingSkill) {
		if (castingSkill != null) {
			skillNumber++;
		}
		this.castingSkill = castingSkill;
	}

	/**
	 * 仅当当前施法仍是预期实例时清除它，避免并发完成覆盖后续施法。
	 * Clears the cast only when it is still the expected instance, preventing concurrent completion from overwriting a later
	 * cast.
	 *
	 * @param expected 预期的施法实例 / expected cast instance
	 * @return 是否已清除 / whether the cast was cleared
	 */
	public synchronized boolean clearCasting(Skill expected) {
		if (castingSkill != expected) {
			return false;
		}
		castingSkill = null;
		return true;
	}

	/**
	 * 返回当前施放技能 ID。
	 * Returns the current casting skill id.
	 *
	 * @return 技能 ID / current casting skill id
	 */
	public int getCastingSkillId() {
		return castingSkill != null ? castingSkill.getSkillTemplate().getSkillId() : 0;
	}

	/**
	 * 返回当前施放技能。
	 * Returns the current casting skill.
	 *
	 * @return 施放技能 / current casting skill
	 */
	public Skill getCastingSkill() {
		return castingSkill;
	}

	/** 返回技能编号 / Returns the skill number */
	public int getSkillNumber() {
		return skillNumber;
	}

	/** 设置技能编号 / Sets the skill number */
	public void setSkillNumber(int skillNumber) {
		this.skillNumber = skillNumber;
	}

	/** 返回被攻击次数 / Returns the attacked count */
	public int getAttackedCount() {
		return this.attackedCount;
	}

	/** 递增被攻击次数 / Increments the attacked count. */
	public void incrementAttackedCount() {
		this.attackedCount++;
	}

	/** 清除被攻击次数 / Clears the attacked count. */
	public void clearAttackedCount() {
		attackedCount = 0;
	}

	/**
	 * 是否正在使用物品。
	 * Whether the creature is using an item.
	 *
	 * @return 是否正在使用物品 / whether using an item
	 */
	public boolean isUsingItem() {
		return usingItem != null;
	}

	/**
	 * 设置正在使用的物品。
	 * Sets the item being used.
	 *
	 * @param usingItem 正在使用的物品 / item being used
	 */
	public void setUsingItem(Item usingItem) {
		this.usingItem = usingItem;
	}

	/**
	 * 获取正在使用的物品 ID。
	 * Gets the id of the item being used.
	 *
	 * @return 物品 ID / item id
	 */
	public int getUsingItemId() {
		return usingItem != null ? usingItem.getItemTemplate().getTemplateId() : 0;
	}

	/**
	 * 返回正在使用的物品。
	 * Returns the item being used.
	 *
	 * @return 正在使用的物品 / item being used
	 */
	public Item getUsingItem() {
		return usingItem;
	}

	/**
	 * 是否有禁用移动的异常效果。
	 * Whether any abnormal effect disables movement.
	 *
	 * @return 是否禁用移动 / whether movement is disabled
	 */
	public boolean canPerformMove() {
		return !(getEffectController().isAbnormalState(AbnormalState.CANT_MOVE_STATE)
				|| getTransformModel().isMoveDisabled() || !isSpawned());
	}

	/**
	 * 是否有禁用攻击的异常效果。
	 * Whether any abnormal effect disables attack.
	 *
	 * @return 是否禁用攻击 / whether attack is disabled
	 */
	public boolean canAttack() {
		return !(getEffectController().isAbnormalState(AbnormalState.CANT_ATTACK_STATE) || isCasting()
				|| getTransformModel().isAttackDisabled()
				|| isInState(CreatureState.RESTING) || isInState(CreatureState.PRIVATE_SHOP));
	}

	/** 返回真实战斗属性缩放系数。 / Returns the retail combat-stat scaling ratio. */
	public float getStatRatio() {
		int ratio = statRatio;
		if (this instanceof Player) {
			ratio = Math.max(ratio, getPlayerStatRatio(getLevel()));
		}
		return Math.max(1000, Math.min(65000, ratio)) / 1000f;
	}

	/** 设置 stat ratio（真实范围为 1000~65000）。 / Sets the retail stat ratio. */
	public void setStatRatio(int statRatio) {
		this.statRatio = Math.max(1000, Math.min(65000, statRatio));
	}

	/** 5.8 pc_stat_ratio.xml 的等级曲线。 / Retail pc_stat_ratio.xml level curve. */
	static int getPlayerStatRatio(int level) {
		return 1000 + Math.max(0, level - 65) * 15;
	}

	/**
	 * 返回状态。
	 * Returns the state.
	 *
	 * @return 状态 / state
	 */
	public int getState() {
		return state;
	}

	/**
	 * 设置状态。
	 * Sets the state.
	 *
	 * @param state 要设置的状态 / the state to set
	 */
	public void setState(CreatureState state) {
		this.state |= state.getId();
	}

	/**
	 * 设置状态（通常取自模板）。
	 * Sets the state, usually taken from templates.
	 *
	 * @param state 状态 / state
	 */
	public void setState(int state) {
		this.state = state;
	}

	/** 取消状态 / Unset state. */
	public void unsetState(CreatureState state) {
		this.state &= ~state.getId();
	}

	/** 是否处于状态 / Whether in state. */
	public boolean isInState(CreatureState state) {
		int isState = this.state & state.getId();

		if (isState == state.getId()) {
			return true;
		}
		return false;
	}

	/**
	 * 返回可视状态。
	 * Returns the visual state.
	 *
	 * @return 可视状态 / visualState
	 */
	public int getVisualState() {
		return visualState;
	}

	/**
	 * 设置可视状态。
	 * Sets the visual state.
	 *
	 * @param visualState 要设置的可视状态 / the visualState to set
	 */
	public void setVisualState(CreatureVisualState visualState) {
		this.visualState |= visualState.getId();
	}

	/** 取消可视状态 / Unsets the visual state. */
	public void unsetVisualState(CreatureVisualState visualState) {
		this.visualState &= ~visualState.getId();
	}

	/**
	 * 是否处于指定可视状态。
	 * Whether in the given visual state.
	 *
	 * @param visualState 可视状态 / visual state
	 * @return 是否处于该状态 / whether in that state
	  */
	public boolean isInVisualState(CreatureVisualState visualState) {
		int isVisualState = this.visualState & visualState.getId();

		if (isVisualState == visualState.getId()) {
			return true;
		}
		return false;
	}

	/**
	 * 返回感知状态。
	 * Returns the see state.
	 *
	 * @return 感知状态 / seeState
	 */
	public int getSeeState() {
		return seeState;
	}

	/**
	 * 设置感知状态。
	 * Sets the see state.
	 *
	 * @param seeState 要设置的感知状态 / the seeState to set
	 */
	public void setSeeState(CreatureSeeState seeState) {
		this.seeState |= seeState.getId();
	}

	/** 取消感知状态 / Unsets the see state. */
	public void unsetSeeState(CreatureSeeState seeState) {
		this.seeState &= ~seeState.getId();
	}

	/**
	 * 是否处于指定感知状态。
	 * Whether in the given see state.
	 *
	 * @param seeState 感知状态 / see state
	 * @return 是否处于该状态 / whether in that state
	  */
	public boolean isInSeeState(CreatureSeeState seeState) {
		int isSeeState = this.seeState & seeState.getId();

		if (isSeeState == seeState.getId()) {
			return true;
		}
		return false;
	}

	/**
	 * 返回变身模型。
	 * Returns the transform model.
	 *
	 * @return 变身模型 / the transformModel
	 */
	public TransformModel getTransformModel() {
		return transformModel;
	}

	/**
	 * 设置变身模型。
	 * Sets the transform model.
	 *
	 * @param model 要设置的变身模型 / the transformedModel to set
	 */
	public final void setTransformModel(TransformModel model) {
		this.transformModel = model;
	}

	/**
	 * 返回仇恨列表。
	 * Returns the aggro list.
	 *
	 * @return 仇恨列表 / the aggroList
	 */
	public final AggroList getAggroList() {
		return aggroList;
	}

	/**
	 * 数据包广播掩码。 / PacketBroadcasterMask
	 */
	private volatile byte packetBroadcastMask;

	/**
	 * 为玩家添加广播。
	 * This is adding a broadcast to the player.
	 */
	public final synchronized void addPacketBroadcastMask(BroadcastMode mode) {
		packetBroadcastMask |= mode.mask();

		GameEventServices.packetBroadcaster().add(this);

		// 调试 / Debug
		if (log.isDebugEnabled()) {
			log.debug("PacketBroadcaster: Packet " + mode.name() + " added to player " + this.getName());
		}
	}

	/**
	 * 移除玩家的广播。
	 * This is removing the broadcast from the player.
	 */
	public final synchronized void removePacketBroadcastMask(BroadcastMode mode) {
		packetBroadcastMask &= ~mode.mask();

		// 调试 / Debug
		if (log.isDebugEnabled()) {
			log.debug("PacketBroadcaster: Packet " + mode.name() + " removed from player " + this.getName()); // fix
		}
	}

	/**
	 * 广播获取器。
	 * Broadcast getter.
	 */
	public final byte getPacketBroadcastMask() {
		return packetBroadcastMask;
	}

	/**
	 * 返回观察控制器。
	 * Returns the observe controller.
	 *
	 * @return 观察控制器 / the observeController
	 */
	public ObserveController getObserveController() {
		return observeController;
	}

	/**
	 * 通过双重分派判断双方是否敌对。
	 * Uses double dispatch to determine whether the creatures are enemies.
	 *
	 * @param creature 待检查生物 / creature to check
	 * @return 双方敌对时为 true / true if the creatures are enemies
	 */
	public boolean isEnemy(Creature creature) {
		return creature.isEnemyFrom(this);
	}

	/**
	 * 判断生物是否为敌对目标。
	 * Whether the creature is an enemy.
	 *
	 * @param creature 生物 / creature
	 * @return 是否敌对 / whether enemy
	 */
	public boolean isEnemyFrom(Creature creature) {
		return false;
	}

	/**
	 * 判断玩家是否为敌对目标。
	 * Whether the player is an enemy.
	 *
	 * @param player 玩家 / player
	 * @return 是否敌对 / whether enemy
	 */
	public boolean isEnemyFrom(Player player) {
		return false;
	}

	/**
	 * 判断 NPC 是否为敌对目标。
	 * Whether the NPC is an enemy.
	 *
	 * @param npc NPC / NPC
	 * @return 是否敌对 / whether enemy
	 */
	public boolean isEnemyFrom(Npc npc) {
		return false;
	}

	/** 获取部落。 / Returns the tribe. */
	public TribeClass getTribe() {
		return TribeClass.GENERAL;
	}

	/**
	 * 通过双重分派判断当前生物是否会主动攻击目标。
	 * Uses double dispatch to determine whether this creature is aggressive to the target.
	 *
	 * @param creature 待检查生物 / creature to check
	 * @return 会主动攻击时为 true / true if this creature is aggressive to the target
	 */
	public boolean isAggressiveTo(Creature creature) {
		return creature.isAggroFrom(this);
	}

	/**
	 * 判断生物是否为敌对目标。
	 * Whether the creature is an enemy.
	 *
	 * @param creature 生物 / creature
	 * @return 是否敌对 / whether enemy
	 */
	public boolean isAggroFrom(Creature creature) {
		return false;
	}

	/**
	 * 判断 NPC 是否为敌对目标。
	 * Whether the NPC is an enemy.
	 *
	 * @param npc NPC / NPC
	 * @return 是否敌对 / whether enemy
	 */
	public boolean isAggroFrom(Npc npc) {
		return false;
	}

	/**
	 * 判断 NPC 是否为敌对目标。
	 * Whether the NPC is an enemy.
	 *
	 * @param npc NPC / NPC
	 * @return 是否敌对 / whether enemy
	 */
	public boolean isHostileFrom(Npc npc) {
		return false;
	}

	/**
	 * 判断 NPC 是否会支援本生物。
	 * Whether the NPC supports this creature.
	 *
	 * @param npc NPC / NPC
	 * @return 是否会支援 / whether support
	 */
	public boolean isSupportFrom(Npc npc) {
		return false;
	}

	/**
	 * @param npc
	 */
	public boolean isFriendFrom(Npc npc) {
		return false;
	}

	/**
	 * 判断生物是否为敌对目标。
	 * Whether the creature is an enemy.
	 *
	 * @param creature 生物 / creature
	 * @return 是否敌对 / whether enemy
	 */

	@Override
	public boolean canSee(Creature creature) {
		if (creature == null) {
			return false;
		}
		return creature.getVisualState() <= getSeeState();
	}

	/** 是否可看见对象 / Whether see object */
	public boolean isSeeObject(VisibleObject object) {
		return getKnownList().getVisibleObjects().containsKey(object.getObjectId());
	}

	/** 是否可看见玩家 / Whether see player */
	public boolean isSeePlayer(Player player) {
		return getKnownList().getVisiblePlayers().containsKey(player.getObjectId());
	}

	/**
	 * 返回 NPC 对象类型 NORMAL。
	 * Returns NpcObjectType.NORMAL.
	 *
	 * @return NPC 对象类型 / NpcObjectType.NORMAL
	 */
	public NpcObjectType getNpcObjectType() {
		return NpcObjectType.NORMAL;
	}

	/**
	 * 召唤物与各类仆从：返回当前操控的玩家。用于决斗与敌对关系判定。 / For summons and different kind of servants<br> it will return currently acting player.<br> This method is used for duel and enemy relations,<br> rewards<br>.
	 */
	public Creature getMaster() {
		return this;
	}

	/**
	 * 召唤物返回召唤对象，仆从返回玩家对象。用于 NPC 寻找可攻击目标。 / For summons it will return summon object and for <br> servants - player object.<br> Used to find attackable target for npcs.<br>.
	 */
	public Creature getActingCreature() {
		return this;
	}

	/**
	 * 是否可施放指定技能。
	 * Whether the given skill can be used.
	 *
	 * @param template 技能模板 / skill template
	 * @return 是否可施放 / whether usable
	 */
	public boolean isSkillDisabled(SkillTemplate template) {

		if (skillCoolDowns == null) {
			return false;
		}
		int delayId = template.getDelayId();
		Long coolDown = skillCoolDowns.get(delayId);
		if (coolDown == null) {
			return false;
		}

		if (coolDown < System.currentTimeMillis()) {
			removeSkillCoolDown(delayId);
			return false;
		}

		/*
		 * Some shared cooldown skills have indipendent and different cooldown they must
		 * not be blocked
		 */
		if (skillCoolDownsBase != null && skillCoolDownsBase.get(delayId) != null) {
			int cooldown = template.scaleCooldownByAttackDelay(template.getCooldown(), getGameStats().getAttackSpeed().getCurrent());
			if ((template.getDuration() + SkillConfig.scaleCooldown(cooldown) * 100 + skillCoolDownsBase.get(delayId)) < System
					.currentTimeMillis()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 检查指定冷却 ID 是否处于冷却中。
	 * Checks whether the given cooldown id is active.
	 *
	 * @param delayId 冷却 ID / cooldown id
	 * @return 是否在冷却中 / whether on cooldown
	 */
	public long getSkillCoolDown(int delayId) {
		if (skillCoolDowns == null || !skillCoolDowns.containsKey(delayId)) {
			return 0;
		}
		return skillCoolDowns.get(delayId);
	}

	public long getSkillCoolDownBase(int delayId) {
		return skillCoolDownsBase == null ? 0 : skillCoolDownsBase.getOrDefault(delayId, 0L);
	}

	/**
	 * 设置技能冷却。
	 * Sets a skill cooldown.
	 *
	 * @param delayId 冷却 ID / cooldown id
	 * @param time 冷却时间 / cooldown time
	 */
	public void setSkillCoolDown(int delayId, long time) {

		if (delayId == 0) {
			return;
		}

		if (skillCoolDowns == null) {
			skillCoolDowns = new LinkedHashMap<Integer, Long>();
		}
		skillCoolDowns.put(delayId, time);
	}

	/**
	 * 返回技能冷却表。
	 * Returns the skill cooldowns.
	 *
	 * @return 技能冷却表 / the skillCoolDowns
	 */
	public Map<Integer, Long> getSkillCoolDowns() {
		return skillCoolDowns;
	}

	/**
	 * 移除指定冷却。
	 * Removes the cooldown for the given id.
	 *
	 * @param delayId 冷却 ID / cooldown id
	 */
	public void removeSkillCoolDown(int delayId) {
		if (skillCoolDowns == null) {
			return;
		}
		skillCoolDowns.remove(delayId);
		if (skillCoolDownsBase != null) {
			skillCoolDownsBase.remove(delayId);
		}
	}

	/**
	 * 保存产生整组冷却的技能的当前毫秒时间。 / This function saves the currentMillis of skill that generated the cooldown of an entire cooldownGroup.
	 */
	public void setSkillCoolDownBase(int delayId, long baseTime) {

		if (delayId == 0) {
			return;
		}

		if (skillCoolDownsBase == null) {
			skillCoolDownsBase = new LinkedHashMap<Integer, Long>();
		}
		skillCoolDownsBase.put(delayId, baseTime);
	}

	/**
	 * 返回管理员中立值。
	 * Returns the admin neutral value.
	 *
	 * @return 管理员中立值 / isAdminNeutral value
	 */
	public int getAdminNeutral() {
		return adminFlags >> 4;
	}

	/**
	 * 设置管理员中立值。
	 * Sets the admin neutral value.
	 *
	 * @param newValue 新值 / new value
	 */
	public void setAdminNeutral(int newValue) {
		adminFlags = (byte) ((adminFlags & 0xF) | (newValue & 0xF) << 4);
	}

	/**
	 * 返回管理员仇恨值。
	 * Returns the admin enmity value.
	 *
	 * @return 管理员仇恨值 / isAdminEnmity value
	 */
	public int getAdminEnmity() {
		return adminFlags & 0xF;
	}

	/**
	 * 设置管理员中立值。
	 * Sets the admin neutral value.
	 *
	 * @param newValue 新值 / new value
	 */
	public void setAdminEnmity(int newValue) {
		adminFlags = (byte) ((adminFlags & 0xF0) | (newValue & 0xF));
	}

	/** 返回碰撞半径 / Returns the collision */
	public float getCollision() {
		return getObjectTemplate().getBoundRadius().getCollision();
	}

	/**
	 * @return
	 */
	public boolean isAttackableNpc() {
		return false;
	}

	/** 返回攻击类型 / Returns the attack type. */
	public ItemAttackType getAttackType() {
		return ItemAttackType.PHYSICAL;
	}

	/**
	 * 生物是否正在飞行（FLY 或 GLIDE 状态）。
	 * Whether the creature is flying (FLY or GLIDE states).
	 *
	 * @return 是否正在飞行 / whether flying
	 */
	public boolean isFlying() {
		return (isInState(CreatureState.FLYING) && !isInState(CreatureState.RESTING))
				|| isInState(CreatureState.GLIDING);
	}

	/**
	 * 是否处于飞行状态。
	 * Whether the creature is in flying state.
	 *
	 * @return 是否处于飞行状态 / whether in flying state
	  */
	public boolean isInFlyingState() {
		return isInState(CreatureState.FLYING) && !isInState(CreatureState.RESTING);
	}

	/** 是否玩家。 / Whether player. */
	public byte isPlayer() {
		return 0;
	}

	/** 是否为物理职业 / Whether physical class. */
	public boolean isPhysClass(Creature creature) {
		if (creature instanceof Player) {
			switch (((Player) creature).getPlayerClass()) {
			case GLADIATOR:
			case TEMPLAR:
			case ASSASSIN:
			case RANGER:
			case CLERIC:
			case CHANTER:
				return true;
			default:
				return false;
			}
		}
		return false;
	}

	/** 是否为魔法职业 / Whether magic class. */
	public boolean isMagicClass(Creature creature) {
		if (creature instanceof Player) {
			switch (((Player) creature).getPlayerClass()) {
			case SORCERER:
			case SPIRIT_MASTER:
			case AETHERTECH:
			case GUNSLINGER:
			case SONGWEAVER:
				return true;
			default:
				return false;
			}
		}
		return false;
	}

	/**
	 * 是否为 PvP 目标。
	 * Whether the creature is a PvP target.
	 *
	 * @param creature 生物 / creature
	 * @return 是否为 PvP 目标 / whether pvp target
	  */
	public boolean isPvpTarget(Creature creature) {
		return getActingCreature() instanceof Player && creature.getActingCreature() instanceof Player;
	}

	/** 重新校验区域 / Revalidates zones. */
	public void revalidateZones() {
		MapRegion mapRegion = this.getPosition().getMapRegion();
		if (mapRegion != null) {
			mapRegion.revalidateZones(this);
		}
	}

	/**
	 * 是否处于指定区域。
	 * Whether the creature is inside the given zone.
	 *
	 * @param zoneName 区域名 / zone name
	 * @return 是否在区域内 / whether inside zone
	 */
	public boolean isInsideZone(ZoneName zoneName) {
		if (!isSpawned()) {
			return false;
		}
		return getPosition().getMapRegion().isInsideZone(zoneName, this);
	}

	/** 设置所在区域类型 / Sets the inside zone type */
	public void setInsideZoneType(ZoneType zoneType) {
		byte current = zoneTypes[zoneType.getValue()];
		zoneTypes[zoneType.getValue()] = (byte) (current + 1);
	}

	/** 取消设置所在区域类型 / Unset Inside Zone Type */
	public void unsetInsideZoneType(ZoneType zoneType) {
		byte current = zoneTypes[zoneType.getValue()];
		zoneTypes[zoneType.getValue()] = (byte) (current - 1);
	}

	/** 是否处于指定区域类型 / Whether inside zone type */
	public boolean isInsideZoneType(ZoneType zoneType) {
		return zoneTypes[zoneType.getValue()] > 0;
	}

	/** 获取种族。 / Returns the race. */
	public Race getRace() {
		return Race.NONE;
	}

	/** 获取技能冷却。 / Returns the skill cooldown. */
	public int getSkillCooldown(SkillTemplate template) {
		return template.getCooldown();
	}

	/** 获取物品冷却。 / Returns the item cooldown. */
	public int getItemCooldown(ItemTemplate template) {
		return template.getUseLimits().getDelayTime();
	}

	/** 是否新生成 / Whether new spawn. */
	public boolean isNewSpawn() {
		return System.currentTimeMillis() - spawnTime < 1500;
	}

	/** 返回拉取倍率 / Returns the pulled multi */
	public int getPulledMulti() {
		return PulledMulti;
	}

	/** 设置拉取倍率 / Sets the pulled multi */
	public void setPulledMulti(int pulledMulti) {
		PulledMulti = pulledMulti;
	}
}
