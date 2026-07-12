package com.aionemu.gameserver.model.gameobjects;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;

import com.aionemu.gameserver.ai2.AI2Engine;
import com.aionemu.gameserver.ai2.AITemplate;
import com.aionemu.gameserver.ai2.poll.AIQuestion;
import com.aionemu.gameserver.configs.main.AIConfig;
import com.aionemu.gameserver.controllers.NpcController;
import com.aionemu.gameserver.controllers.movement.NpcMoveController;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.NpcType;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.TribeClass;
import com.aionemu.gameserver.model.drop.NpcDrop;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.siege.SiegeNpc;
import com.aionemu.gameserver.model.skill.NpcSkillList;
import com.aionemu.gameserver.model.stats.container.NpcGameStats;
import com.aionemu.gameserver.model.stats.container.NpcLifeStats;
import com.aionemu.gameserver.model.templates.item.ItemAttackType;
import com.aionemu.gameserver.model.templates.npc.AbyssNpcType;
import com.aionemu.gameserver.model.templates.npc.NpcRank;
import com.aionemu.gameserver.model.templates.npc.NpcRating;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.npc.NpcTemplateType;
import com.aionemu.gameserver.model.templates.npcshout.NpcShout;
import com.aionemu.gameserver.model.templates.npcshout.ShoutEventType;
import com.aionemu.gameserver.model.templates.npcshout.ShoutType;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LOOKATOBJECT;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.spawnengine.WalkerGroup;
import com.aionemu.gameserver.spawnengine.WalkerGroupShift;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldPosition;
import com.aionemu.gameserver.world.WorldType;
import com.google.common.base.Preconditions;

/**
 * NPC 游戏对象。
 * Npc game object.
 *
 * @author Luno
 */
public class Npc extends Creature {

	private WalkerGroup walkerGroup;
	private boolean isQuestBusy = false;
	private NpcSkillList skillList;
	private WalkerGroupShift walkerGroupShift;
	private long lastShoutedSeconds;
	private String masterName = StringUtils.EMPTY;
	private int creatorId = 0;
	private int townId;
	private int abyssId;
	private NpcType npcType;
	private ItemAttackType attacktype = ItemAttackType.PHYSICAL;
	private int sensoryRange = getObjectTemplate().getAggroRange();

	public Npc(int objId, NpcController controller, SpawnTemplate spawnTemplate, NpcTemplate objectTemplate) {
		this(objId, controller, spawnTemplate, objectTemplate, objectTemplate.getLevel());
	}

	public Npc(int objId, NpcController controller, SpawnTemplate spawnTemplate, NpcTemplate objectTemplate,
			byte level) {
		super(objId, controller, spawnTemplate, objectTemplate, new WorldPosition(spawnTemplate.getWorldId()));
		Preconditions.checkNotNull(objectTemplate, "Npcs should be based on template");
		controller.setOwner(this);
		moveController = new NpcMoveController(this);
		skillList = new NpcSkillList(this);
		npcType = objectTemplate.getNpcType();
		setupStatContainers(level);

		boolean aiOverride = false;
		if (spawnTemplate.getModel() != null) {
			if (spawnTemplate.getModel().getAi() != null) {
				aiOverride = true;
				GameEngineServices.ai2Engine().setupAI(spawnTemplate.getModel().getAi(), this);
			}
		}

		if (!aiOverride) {
			GameEngineServices.ai2Engine().setupAI(objectTemplate.getAi(), this);
		}
		lastShoutedSeconds = System.currentTimeMillis() / 1000;
	}

	/** 返回 move controller / Returns the move controller */
	@Override
	public NpcMoveController getMoveController() {
		return (NpcMoveController) super.getMoveController();
	}

	/**
	 * @param level
	 */
	protected void setupStatContainers(byte level) {
		setGameStats(new NpcGameStats(this));
		setLifeStats(new NpcLifeStats(this));
	}

	/** 获取对象模板。 / Returns the object template. */
	@Override
	public NpcTemplate getObjectTemplate() {
		return (NpcTemplate) objectTemplate;
	}

	/** 获取名称。 / Returns the name. */
	@Override
	public String getName() {
		return getObjectTemplate().getName();
	}

	/** 返回 NPC ID / Returns the npc id */
	public int getNpcId() {
		return getObjectTemplate().getTemplateId();
	}

	/** 获取等级。 / Returns the level. */
	@Override
	public byte getLevel() {
		return getObjectTemplate().getLevel();
	}

	/** 返回 life stats / Returns the life stats */
	@Override
	public NpcLifeStats getLifeStats() {
		return (NpcLifeStats) super.getLifeStats();
	}

	/** 获取游戏属性。 / Returns the game stats. */
	@Override
	public NpcGameStats getGameStats() {
		return (NpcGameStats) super.getGameStats();
	}

	/** 返回 controller / Returns the controller */
	@Override
	public NpcController getController() {
		return (NpcController) super.getController();
	}

	/** 获取技能列表。 / Returns the skill list. */
	public NpcSkillList getSkillList() {
		return this.skillList;
	}

	/** 返回攻击类型 / Returns the attack type*/
	@Override
	public ItemAttackType getAttackType() {
		return this.ai2.modifyAttackType(attacktype);
	}

	/**
	 * @return Whether walk routes
	 */
	public boolean hasWalkRoutes() {
		return getSpawn().getWalkerId() != null;
	}

	/** 返回 rating / Returns the rating */
	public NpcRating getRating() {
		return getObjectTemplate().getRating();
	}

	/** 获取军阶。 / Returns the rank. */
	public NpcRank getRank() {
		return getObjectTemplate().getRank();
	}

	/** 返回欧比斯 NPC 类型 / Returns the abyss npc type */
	public AbyssNpcType getAbyssNpcType() {
		return getObjectTemplate().getAbyssNpcType();
	}

	/** 返回 hp gauge / Returns the hp gauge */
	public int getHpGauge() {
		return getObjectTemplate().getHpGaugeLevel();
	}

	/**
	 * @return 是否处于和平状态。 / Whether peace
	  */
	public boolean isPeace() {
		return getNpcType().equals(NpcType.PEACE);
	}

	/** 是否好友到 / Whether friend to*/
	public boolean isFriendTo(Player player) {
		if (this.getTribe() == TribeClass.NOFIGHT) {
			return false;
		}
		return DataManager.TRIBE_RELATIONS_DATA.isFriendlyRelation(getTribe(), player.getTribe());
	}

	/** 是否会主动攻击 / Whether aggressive to */
	@Override
	public boolean isAggressiveTo(Creature creature) {
		if (creature instanceof Player) {
			return ((Player) creature).isAggroFrom(this);
		} else if (creature instanceof Summon) {
			return ((Summon) creature).isAggroFrom(this);
		}

		if (this.getTribe() == TribeClass.XDRAKAN_DGUARD && creature.getTribe() == TribeClass.XDRAKAN_LGUARD
				|| this.getTribe() == TribeClass.XDRAKAN_LGUARD && creature.getTribe() == TribeClass.XDRAKAN_DGUARD) {
			return true;
		}

		if (DataManager.TRIBE_RELATIONS_DATA.isAggressiveRelation(getTribe(), creature.getTribe())) {
			return true;
		} else {
			return (creature instanceof Npc && guardAgainst((Npc) creature));
		}
	}

	/**
	 * 守卫防御其位置的行为。 / Represents the action of a guard defending its position.
	 */
	public boolean guardAgainst(Npc npc) {
		// 即便 NPC 仇恨玩家，也不应互相仇恨（如希波吕托斯）。 / Even if NPCs aggro players they shouldn't aggro between (as Hippolytus)
		if (this.getRace() == npc.getRace()) {
			return false;
		}
		if ((getTribe().isLightGuard() || this.getRace() == Race.ELYOS
				&& this.getObjectTemplate().getNpcTemplateType() == NpcTemplateType.GUARD)
				&& DataManager.TRIBE_RELATIONS_DATA.isAggressiveRelation(npc.getTribe(), TribeClass.PC)) {
			return true;
		}
		if ((getTribe().isDarkGuard() || this.getRace() == Race.ASMODIANS
				&& this.getObjectTemplate().getNpcTemplateType() == NpcTemplateType.GUARD)
				&& DataManager.TRIBE_RELATIONS_DATA.isAggressiveRelation(npc.getTribe(), TribeClass.PC_DARK)) {
			return true;
		}
		return false;
	}

	/**
	 * @param npc 是否会对其产生仇恨。 / Whether aggro from
	  */
	@Override
	public boolean isAggroFrom(Npc npc) {
		return DataManager.TRIBE_RELATIONS_DATA.isAggressiveRelation(npc.getTribe(), getTribe());
	}

	/**
	 * 判断是否与其敌对。 / Whether hostile to the target.
	 */
	@Override
	public boolean isHostileFrom(Npc npc) {
		return DataManager.TRIBE_RELATIONS_DATA.isHostileRelation(npc.getTribe(), getTribe());
	}

	/**
	 * 判断是否会支援其。 / Whether this NPC supports the target.
	 */
	@Override
	public boolean isSupportFrom(Npc npc) {
		return DataManager.TRIBE_RELATIONS_DATA.isSupportRelation(npc.getTribe(), getTribe());
	}

	/** 是否好友 / Whether friend from*/
	@Override
	public boolean isFriendFrom(Npc npc) {
		return DataManager.TRIBE_RELATIONS_DATA.isFriendlyRelation(npc.getTribe(), getTribe());
	}

	/**
	 * @param player Whether none relation
	 */
	public boolean isNoneRelation(Player player) {
		return DataManager.TRIBE_RELATIONS_DATA.isNoneRelation(getTribe(), player.getTribe());
	}

	/** 获取部落。 / Returns the tribe. */
	@Override
	public TribeClass getTribe() {
		TribeClass transformTribe = getTransformModel().getTribe();
		if (transformTribe != null) {
			return transformTribe;
		}
		return this.getObjectTemplate().getTribe();
	}

	/** 返回 aggro range / Returns the aggro range */
	public int getAggroRange() {
		return ai2.modifySensoryRange(sensoryRange);
	}

	/**
	 * Check whether npc located near initial spawn location
	 *
	 * @return true or false
	 */
	public boolean isAtSpawnLocation() {
		return getDistanceToSpawnLocation() < 3;
	}

	/**
	 * @return distance to spawn location
	 */
	public double getDistanceToSpawnLocation() {
		return MathUtil.getDistance(getSpawn().getX(), getSpawn().getY(), getSpawn().getZ(), getX(), getY(), getZ());
	}

	/** 是否敌对。 / Whether Enemy. */
	@Override
	public boolean isEnemy(Creature creature) {
		if (creature instanceof Player) {
			if (getAi2().ask(AIQuestion.CAN_ATTACK_PLAYER).isPositive()) {
				return true;
			}
		}
		return creature.isEnemyFrom(this);
	}

	/**
	 * @param npc 是否为敌对目标。 / Whether enemy from
	  */
	@Override
	public boolean isEnemyFrom(Npc npc) {
		if (npc.isFriendFrom(this)) {
			return false;
		}
		return isAggressiveTo(npc) || npc.getAggroList().isHating(this) || getAggroList().isHating(npc);
	}

	/**
	 * @param player 是否为敌对目标。 / Whether enemy from
	  */
	@Override
	public boolean isEnemyFrom(Player player) {
		return isAttackableNpc() || player.isAggroIconTo(this);
	}

	/** 返回 see state / Returns the see state */
	@Override
	public int getSeeState() {
		int skillSeeState = super.getSeeState();
		int congenitalSeeState = getObjectTemplate().getRating().getCongenitalSeeState().getId();
		return Math.max(skillSeeState, congenitalSeeState);
	}

	/** 返回 is quest busy / Returns the is quest busy */
	public boolean getIsQuestBusy() {
		return isQuestBusy;
	}

	/** 设置 is quest busy / Sets the is quest busy */
	public void setIsQuestBusy(boolean busy) {
		isQuestBusy = busy;
	}

	/**
	 * @return Whether attackable npc
	 */
	@Override
	public boolean isAttackableNpc() {
		return getNpcType() == NpcType.ATTACKABLE;
	}

	/**
	 * @return Name of the Master
	 */
	public String getMasterName() {
		return masterName;
	}

	/** 设置 master name / Sets the master name */
	public void setMasterName(String masterName) {
		this.masterName = masterName;
	}

	/**
	 * @return UniqueId of the VisibleObject which created this Npc (could be player
	 *         or house)
	 */
	public int getCreatorId() {
		return creatorId;
	}

	/** 设置 creator id / Sets the creator id */
	public void setCreatorId(int creatorId) {
		this.creatorId = creatorId;
	}

	/** 返回城镇 ID / Returns the town id */
	public int getTownId() {
		return townId;
	}

	/** 设置 town id / Sets the town id */
	public void setTownId(int townId) {
		this.townId = townId;
	}

	/** 返回欧比斯 ID / Returns the abyss id */
	public int getAbyssId() {
		return abyssId;
	}

	/** 设置 abyss id / Sets the abyss id */
	public void setAbyssId(int abyssId) {
		this.abyssId = abyssId;
	}

	/** 返回 creator / Returns the creator */
	public VisibleObject getCreator() {
		return null;
	}

	/** 设置 target / Sets the target */
	@Override
	public void setTarget(VisibleObject creature) {
		if (getTarget() != creature) {
			super.setTarget(creature);
			super.clearAttackedCount();
			getGameStats().renewLastChangeTargetTime();
			if (!getLifeStats().isAlreadyDead()) {
				PacketSendUtility.broadcastPacket(this, new SM_LOOKATOBJECT(this));
			}
		}
	}

	/** 设置巡逻队伍。 / Sets the walker group. */
	public void setWalkerGroup(WalkerGroup wg) {
		this.walkerGroup = wg;
	}

	/** 获取巡逻队伍。 / Returns the walker group. */
	public WalkerGroup getWalkerGroup() {
		return walkerGroup;
	}

	/** 设置 walker group shift / Sets the walker group shift */
	public void setWalkerGroupShift(WalkerGroupShift shift) {
		this.walkerGroupShift = shift;
	}

	/** 返回 walker group shift / Returns the walker group shift */
	public WalkerGroupShift getWalkerGroupShift() {
		return walkerGroupShift;
	}

	/** 是否首领 / Whether boss*/
	public boolean isBoss() {
		return getObjectTemplate().getRank() == NpcRank.EXPERT;
	}

	/** 是否标志 / Whether flag*/
	public boolean isFlag() {
		return getObjectTemplate().getNpcTemplateType() == NpcTemplateType.FLAG;
	}

	/**
	 * @return Whether entity
	 */
	public boolean hasEntity() {
		return getSpawn().getEntityId() != 0;
	}

	/** 获取种族。 / Returns the race. */
	@Override
	public Race getRace() {
		return this.getObjectTemplate().getRace();
	}

	/** 返回 npc drop / Returns the npc drop */
	public NpcDrop getNpcDrop() {
		return getObjectTemplate().getNpcDrop();
	}

	/** 返回 npc type / Returns the npc type */
	public NpcType getNpcType() {
		return npcType;
	}

	/** SetsNPC 类型 / Sets the npc type */
	public void setNpcType(NpcType newType) {
		npcType = newType;
	}

	/** 是否为奖励欧比斯点数。 / Whether reward ap. */
	public boolean isRewardAP() {
		if (this instanceof SiegeNpc) {
			return true;
		} else if (this.getWorldType() == WorldType.ABYSS) {
			return true;
		} else if (this.getAi2().ask(AIQuestion.SHOULD_REWARD_AP).isPositive()) {
			return true;
		} else if (this.getWorldType() == WorldType.BALAUREA || this.getWorldType() == WorldType.PANESTERRA
				|| this.isInInstance()) {
			return getRace() == Race.DRAKAN || getRace() == Race.DRAGON || getRace() == Race.NAGA
					|| getRace() == Race.LIZARDMAN || getRace() == Race.GCHIEF_DRAGON;
		}
		return false;
	}

	/** 可喊话 / may Shout. */
	public boolean mayShout(int delaySeconds) {
		if (!DataManager.NPC_SHOUT_DATA.hasAnyShout(getPosition().getMapId(), getNpcId())) {
			return false;
		}
		return (System.currentTimeMillis() - lastShoutedSeconds) / 1000 >= delaySeconds;
	}

	/** 喊话 / shout. */
	public void shout(final NpcShout shout, final Creature target, final Object param, int delaySeconds) {
		if (shout.getWhen() != ShoutEventType.DIED && shout.getWhen() != ShoutEventType.BEFORE_DESPAWN
				&& getLifeStats().isAlreadyDead() || !mayShout(delaySeconds)) {
			return;
		}

		if (shout.getPattern() != null
				&& !((AITemplate) getAi2()).onPatternShout(shout.getWhen(), shout.getPattern(), shout.getSkillNo())) {
			return;
		}

		final int shoutRange = getObjectTemplate().getMinimumShoutRange();
		if (shout.getShoutType() == ShoutType.SAY && !(target instanceof Player)
				|| target != null && !MathUtil.isIn3dRange(target, this, shoutRange)) {
			return;
		}
		final Npc thisNpc = this;
		final SM_SYSTEM_MESSAGE message = new SM_SYSTEM_MESSAGE(true, shout.getStringId(), getObjectId(), 1, param);
		lastShoutedSeconds = System.currentTimeMillis() / 1000;

		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

			/** 运行 / run. */
			@Override
			public void run() {
				if (thisNpc.getLifeStats().isAlreadyDead() && shout.getWhen() != ShoutEventType.DIED
						&& shout.getWhen() != ShoutEventType.BEFORE_DESPAWN) {
					return;
				}
				// 针对特定玩家的消息（IDLE 时已在广播）。 / message for the specific player (when IDLE we are already broadcasting!!!)
				if (shout.getShoutType() == ShoutType.SAY || shout.getWhen() == ShoutEventType.IDLE) {
					// 【RR】是否应将 lastShoutedSeconds 与广播分开？ / [RR] Should we have lastShoutedSeconds separated from broadcasts (??)
					PacketSendUtility.sendPacket((Player) target, message);
				} else {
					Iterator<Player> iter = thisNpc.getKnownList().getKnownPlayers().values().iterator();
					while (iter.hasNext()) {
						Player kObj = iter.next();
						if (kObj.getLifeStats().isAlreadyDead() || !kObj.isOnline()) {
							continue;
						}
						if (MathUtil.isIn3dRange(kObj, thisNpc, shoutRange)) {
							PacketSendUtility.sendPacket(kObj, message);
						}
					}
				}
			}
		}, delaySeconds * 1000);
	}
}
