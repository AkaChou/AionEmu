package com.aionemu.gameserver.model.gameobjects;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameEventServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai2.AI2;
import com.aionemu.gameserver.ai2.AI2Engine;
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
	private Skill castingSkill;
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

	/**
	 * @param objId
	 * @param controller
	 * @param spawnTemplate
	 * @param objectTemplate
	 * @param position
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

	/** 返回 move controller / Returns the move controller */
	public MoveController getMoveController() {
		return this.moveController;
	}

	protected AggroList createAggroList() {
		return new AggroList(this);
	}

	/**
	 * Return CreatureController of this Creature object
	 *
	 * @return CreatureController.
	 */
	@Override
	public CreatureController<? extends Creature> getController() {
		return (CreatureController<?>) super.getController();
	}

	/**
	 * @return the lifeStats
	 */
	public CreatureLifeStats<? extends Creature> getLifeStats() {
		return lifeStats;
	}

	/**
	 * @param lifeStats the lifeStats to set
	 */
	public void setLifeStats(CreatureLifeStats<? extends Creature> lifeStats) {
		this.lifeStats = lifeStats;
	}

	/**
	 * @return the gameStats
	 */
	public CreatureGameStats<? extends Creature> getGameStats() {
		return gameStats;
	}

	/**
	 * @param gameStats the gameStats to set
	 */
	public void setGameStats(CreatureGameStats<? extends Creature> gameStats) {
		this.gameStats = gameStats;
	}

	/** 获取等级。 / Returns the level. */
	public abstract byte getLevel();

	/**
	 * @return the effectController
	 */
	public EffectController getEffectController() {
		return effectController;
	}

	/**
	 * @param effectController the effectController to set
	 */
	public void setEffectController(EffectController effectController) {
		this.effectController = effectController;
	}

	/** 返回 ai 2 / Returns the ai 2 */
	public AI2 getAi2() {
		return ai2 != null ? ai2 : GameEngineServices.ai2Engine().setupAI("dummy", this);
	}

	/** 设置 ai 2 / Sets the ai 2 */
	public void setAi2(AI2 ai2) {
		this.ai2 = ai2;
	}

	/**
	 * @return 是否删除 delayed。 / Whether delete delayed
	  */
	public boolean isDeleteDelayed() {
		return isDespawnDelayed;
	}

	/** 设置 despawn delayed / Sets the despawn delayed */
	public void setDespawnDelayed(boolean delayed) {
		isDespawnDelayed = delayed;
	}

	/** 是否标志 / Whether flag*/
	public boolean isFlag() {
		return false;
	}

	/**
	 * @return 生物是否正在施放技能。 / Is creature casting some skill
	 */
	public boolean isCasting() {
		return castingSkill != null;
	}

	/**
	 * 设置当前 casting 技能或 nullwhen 技能 ends。
	 * Set current casting skill or null when skill ends
	 *
	 * @param castingSkill
	 */
	public void setCasting(Skill castingSkill) {
		if (castingSkill != null) {
			skillNumber++;
		}
		this.castingSkill = castingSkill;
	}

	/**
	 * @return 当前施放技能 ID。 / Current casting skill id
	 */
	public int getCastingSkillId() {
		return castingSkill != null ? castingSkill.getSkillTemplate().getSkillId() : 0;
	}

	/**
	 * @return 当前施放技能。 / Current casting skill
	 */
	public Skill getCastingSkill() {
		return castingSkill;
	}

	/** 返回 skill number / Returns the skill number */
	public int getSkillNumber() {
		return skillNumber;
	}

	/** 设置技能编号 / Sets the skill number */
	public void setSkillNumber(int skillNumber) {
		this.skillNumber = skillNumber;
	}

	/** 返回 attacked count / Returns the attacked count */
	public int getAttackedCount() {
		return this.attackedCount;
	}

	/** 递增 attacked count / Increment Attacked Count */
	public void incrementAttackedCount() {
		this.attackedCount++;
	}

	/** 清除 attacked 次数 / Clear attacked count */
	public void clearAttackedCount() {
		attackedCount = 0;
	}

	/**
	 * @return 是否正在使用物品。 / Is using item
	 */
	public boolean isUsingItem() {
		return usingItem != null;
	}

	/**
	 * 设置 using 物品。
	 * Set using item
	 *
	 * @param usingItem
	 */
	public void setUsingItem(Item usingItem) {
		this.usingItem = usingItem;
	}

	/**
	 * 获取 usingitemid。
	 * get Using ItemId
	 *
	 * @return
	 */
	public int getUsingItemId() {
		return usingItem != null ? usingItem.getItemTemplate().getTemplateId() : 0;
	}

	/**
	 * @return 正在使用的物品。 / Using Item
	 */
	public Item getUsingItem() {
		return usingItem;
	}

	/**
	 * @return 检查是否有禁用移动的异常效果。 / All abnormal effects are checked that disable movements
	 */
	public boolean canPerformMove() {
		return !(getEffectController().isAbnormalState(AbnormalState.CANT_MOVE_STATE) || !isSpawned());
	}

	/**
	 * @return 检查是否有禁用攻击的异常效果。 / All abnormal effects are checked that disable attack
	 */
	public boolean canAttack() {
		return !(getEffectController().isAbnormalState(AbnormalState.CANT_ATTACK_STATE) || isCasting()
				|| isInState(CreatureState.RESTING) || isInState(CreatureState.PRIVATE_SHOP));
	}

	/**
	 * @return state
	 */
	public int getState() {
		return state;
	}

	/**
	 * @param state the state to set
	 */
	public void setState(CreatureState state) {
		this.state |= state.getId();
	}

	/**
	 * @param state taken usually from templates
	 */
	public void setState(int state) {
		this.state = state;
	}

	/** 取消设置 state / Unset State */
	public void unsetState(CreatureState state) {
		this.state &= ~state.getId();
	}

	/** 是否状态 / Whether in state*/
	public boolean isInState(CreatureState state) {
		int isState = this.state & state.getId();

		if (isState == state.getId()) {
			return true;
		}
		return false;
	}

	/**
	 * @return visualState
	 */
	public int getVisualState() {
		return visualState;
	}

	/**
	 * @param visualState the visualState to set
	 */
	public void setVisualState(CreatureVisualState visualState) {
		this.visualState |= visualState.getId();
	}

	/** 取消设置 visual state / Unset Visual State */
	public void unsetVisualState(CreatureVisualState visualState) {
		this.visualState &= ~visualState.getId();
	}

	/**
	 * @param visualState 是否处于指定可视状态。 / Whether in visual state
	  */
	public boolean isInVisualState(CreatureVisualState visualState) {
		int isVisualState = this.visualState & visualState.getId();

		if (isVisualState == visualState.getId()) {
			return true;
		}
		return false;
	}

	/**
	 * @return seeState
	 */
	public int getSeeState() {
		return seeState;
	}

	/**
	 * @param seeState the seeState to set
	 */
	public void setSeeState(CreatureSeeState seeState) {
		this.seeState |= seeState.getId();
	}

	/** 取消设置 see state / Unset See State */
	public void unsetSeeState(CreatureSeeState seeState) {
		this.seeState &= ~seeState.getId();
	}

	/**
	 * @param seeState 是否处于指定可见状态。 / Whether in see state
	  */
	public boolean isInSeeState(CreatureSeeState seeState) {
		int isSeeState = this.seeState & seeState.getId();

		if (isSeeState == seeState.getId()) {
			return true;
		}
		return false;
	}

	/**
	 * @return the transformModel
	 */
	public TransformModel getTransformModel() {
		return transformModel;
	}

	/**
	 * @param model the transformedModel to set
	 */
	public final void setTransformModel(TransformModel model) {
		this.transformModel = model;
	}

	/**
	 * @return the aggroList
	 */
	public final AggroList getAggroList() {
		return aggroList;
	}

	/**
	 * 数据包广播掩码。 / PacketBroadcasterMask
	 */
	private volatile byte packetBroadcastMask;

	/**
	 * This is adding broadcast to player
	 */
	public final void addPacketBroadcastMask(BroadcastMode mode) {
		packetBroadcastMask |= mode.mask();

		GameEventServices.packetBroadcaster().add(this);

		// 调试 / Debug
		if (log.isDebugEnabled()) {
			log.debug("PacketBroadcaster: Packet " + mode.name() + " added to player " + this.getName());
		}
	}

	/**
	 * This is removing broadcast from player
	 */
	public final void removePacketBroadcastMask(BroadcastMode mode) {
		packetBroadcastMask &= ~mode.mask();

		// 调试 / Debug
		if (log.isDebugEnabled()) {
			log.debug("PacketBroadcaster: Packet " + mode.name() + " removed from player " + this.getName()); // fix
		}
	}

	/**
	 * Broadcast getter
	 */
	public final byte getPacketBroadcastMask() {
		return packetBroadcastMask;
	}

	/**
	 * @return the observeController
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
	 * @param creature
	 */
	public boolean isEnemyFrom(Creature creature) {
		return false;
	}

	/**
	 * @param player
	 * @return
	 */
	public boolean isEnemyFrom(Player player) {
		return false;
	}

	/**
	 * @param npc
	 * @return
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
	 * @param creature
	 * @return
	 */
	public boolean isAggroFrom(Creature creature) {
		return false;
	}

	/**
	 * @param npc
	 * @return
	 */
	public boolean isAggroFrom(Npc npc) {
		return false;
	}

	/**
	 * @param npc
	 * @return
	 */
	public boolean isHostileFrom(Npc npc) {
		return false;
	}

	/**
	 * @param npc
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
	 * @param creature
	 * @return
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
	 * @return NpcObjectType.NORMAL
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
	 * @param template
	 * @return
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
			if ((template.getDuration() + template.getCooldown() * 100 + skillCoolDownsBase.get(delayId)) < System
					.currentTimeMillis()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @param delayId
	 * @return
	 */
	public long getSkillCoolDown(int delayId) {
		if (skillCoolDowns == null || !skillCoolDowns.containsKey(delayId)) {
			return 0;
		}
		return skillCoolDowns.get(delayId);
	}

	/**
	 * @param delayId
	 * @param time
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
	 * @return the skillCoolDowns
	 */
	public Map<Integer, Long> getSkillCoolDowns() {
		return skillCoolDowns;
	}

	/**
	 * @param delayId
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
	 * @return isAdminNeutral value
	 */
	public int getAdminNeutral() {
		return adminFlags >> 4;
	}

	/**
	 * @param newValue
	 */
	public void setAdminNeutral(int newValue) {
		adminFlags = (byte) ((adminFlags & 0xF) | (newValue & 0xF) << 4);
	}

	/**
	 * @return isAdminEnmity value
	 */
	public int getAdminEnmity() {
		return adminFlags & 0xF;
	}

	/**
	 * @param newValue
	 */
	public void setAdminEnmity(int newValue) {
		adminFlags = (byte) ((adminFlags & 0xF0) | (newValue & 0xF));
	}

	/** 返回碰撞 / Returns the collision */
	public float getCollision() {
		return getObjectTemplate().getBoundRadius().getCollision();
	}

	/**
	 * @return
	 */
	public boolean isAttackableNpc() {
		return false;
	}

	/** 返回攻击类型 / Returns the attack type*/
	public ItemAttackType getAttackType() {
		return ItemAttackType.PHYSICAL;
	}

	/**
	 * @return 生物正在飞行（FLY 或 GLIDE 状态）。 / Creature is flying (FLY or GLIDE states)
	 */
	public boolean isFlying() {
		return (isInState(CreatureState.FLYING) && !isInState(CreatureState.RESTING))
				|| isInState(CreatureState.GLIDING);
	}

	/**
	 * @return 是否处于飞行状态。 / Whether in flying state
	  */
	public boolean isInFlyingState() {
		return isInState(CreatureState.FLYING) && !isInState(CreatureState.RESTING);
	}

	/** 是否玩家。 / Whether Player. */
	public byte isPlayer() {
		return 0;
	}

	/** 是否为物理职业 / Whether phys class */
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

	/** 是否为魔法职业 / Whether magic class */
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
	 * @param creature 是否为 PvP 目标。 / Whether pvp target
	  */
	public boolean isPvpTarget(Creature creature) {
		return getActingCreature() instanceof Player && creature.getActingCreature() instanceof Player;
	}

	/** 重校验区域 / revalidate Zones. */
	public void revalidateZones() {
		MapRegion mapRegion = this.getPosition().getMapRegion();
		if (mapRegion != null) {
			mapRegion.revalidateZones(this);
		}
	}

	/**
	 * @param zoneName Whether inside zone
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

	/** 是否新刷新 / Whether new spawn*/
	public boolean isNewSpawn() {
		return System.currentTimeMillis() - spawnTime < 1500;
	}

	/** 返回 pulled multi / Returns the pulled multi */
	public int getPulledMulti() {
		return PulledMulti;
	}

	/** 设置 pulled multi / Sets the pulled multi */
	public void setPulledMulti(int pulledMulti) {
		PulledMulti = pulledMulti;
	}
}
