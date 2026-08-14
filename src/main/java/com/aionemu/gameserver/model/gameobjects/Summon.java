package com.aionemu.gameserver.model.gameobjects;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import java.util.concurrent.Future;

import com.aionemu.gameserver.ai2.AI2Engine;
import com.aionemu.gameserver.controllers.CreatureController;
import com.aionemu.gameserver.controllers.SummonController;
import com.aionemu.gameserver.controllers.attack.AggroList;
import com.aionemu.gameserver.controllers.attack.PlayerAggroList;
import com.aionemu.gameserver.controllers.movement.SiegeWeaponMoveController;
import com.aionemu.gameserver.controllers.movement.SummonMoveController;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.SkillElement;
import com.aionemu.gameserver.model.TribeClass;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.container.SummonGameStats;
import com.aionemu.gameserver.model.stats.container.SummonLifeStats;
import com.aionemu.gameserver.model.summons.SummonMode;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.stats.SummonStatsTemplate;
import com.aionemu.gameserver.world.WorldPosition;

/**
 * 召唤物游戏对象。
 * Summon game object.
 *
 * @author ATracer
 */
public class Summon extends Creature {

	private static final long NANOS_PER_SECOND = 1_000_000_000L;

	private Player master;
	private SummonMode mode = SummonMode.GUARD;
	private byte level;
	private int liveTime = 0;
	private long expirationTimeNanos;
	private Future<?> releaseTask;
	private final SkillElement alwaysResistElement;

	/**
	 * 构造召唤物。
	 * Constructs a summon.
	 *
	 * @param objId 对象 ID / object id
	 * @param controller 生物控制器 / creature controller
	 * @param spawnTemplate 生成模板 / spawn template
	 * @param objectTemplate NPC 模板 / NPC template
	 * @param level 等级 / level
	 * @param time 存活时长（秒），0 表示永久 / lifetime in seconds, 0 for permanent
	 */
	public Summon(int objId, CreatureController<? extends Creature> controller, SpawnTemplate spawnTemplate,
			NpcTemplate objectTemplate, byte level, int time) {
		super(objId, controller, spawnTemplate, objectTemplate, new WorldPosition(spawnTemplate.getWorldId()));
		controller.setOwner(this);
		String ai = objectTemplate.getAi();
		GameEngineServices.ai2Engine().setupAI(ai, this);
		moveController = (ai.equals("siege_weapon") ? new SiegeWeaponMoveController(this)
				: new SummonMoveController(this));
		this.level = level;
		setLiveTime(time);
		SummonStatsTemplate statsTemplate = DataManager.SUMMON_STATS_DATA
				.getSummonTemplate(objectTemplate.getTemplateId(), level);
		setGameStats(new SummonGameStats(this, statsTemplate));
		setLifeStats(new SummonLifeStats(this));
		alwaysResistElement = getAlwaysResistElement(objectTemplate.getName());
	}

	static SkillElement getAlwaysResistElement(String name) {
		return switch (name) {
			case "earth spirit" -> SkillElement.EARTH;
			case "fire spirit" -> SkillElement.FIRE;
			case "water spirit" -> SkillElement.WATER;
			case "wind spirit" -> SkillElement.WIND;
			default -> SkillElement.NONE;
		};
	}

	/** 返回常驻免疫属性 / Returns the always resist element */
	public SkillElement getAlwaysResistElement() {
		return alwaysResistElement;
	}

	@Override
	protected AggroList createAggroList() {
		return new PlayerAggroList(this);
	}

	/** 获取游戏属性。 / Returns the game stats. */
	@Override
	public SummonGameStats getGameStats() {
		return (SummonGameStats) super.getGameStats();
	}

	/** 返回主人 / Returns the master. */
	@Override
	public Player getMaster() {
		return master;
	}

	/**
	 * 设置主人。
	 * Sets the master.
	 *
	 * @param master 主人玩家 / the master to set
	 */
	public void setMaster(Player master) {
		this.master = master;
	}

	/** 获取名称。 / Returns the name. */
	@Override
	public String getName() {
		return objectTemplate.getName();
	}

	/**
	 * 返回等级。
	 * Returns the level.
	 *
	 * @return 等级 / the level
	 */
	@Override
	public byte getLevel() {
		return level;
	}

	/** 获取对象模板。 / Returns the object template. */
	@Override
	public NpcTemplate getObjectTemplate() {
		return (NpcTemplate) super.getObjectTemplate();
	}

	/** 返回 NPC ID / Returns the npc id */
	public int getNpcId() {
		return getObjectTemplate().getTemplateId();
	}

	/** 返回名称 ID / Returns the name id */
	public int getNameId() {
		return getObjectTemplate().getNameId();
	}

	/**
	 * 返回 NPC 对象类型 SUMMON。
	 * Returns NpcObjectType.SUMMON.
	 *
	 * @return NPC 对象类型 / NpcObjectType.SUMMON
	 */
	@Override
	public NpcObjectType getNpcObjectType() {
		return NpcObjectType.SUMMON;
	}

	/** 返回控制器 / Returns the controller */
	@Override
	public SummonController getController() {
		return (SummonController) super.getController();
	}

	/**
	 * 返回召唤模式。
	 * Returns the summon mode.
	 *
	 * @return 模式 / the mode
	 */
	public SummonMode getMode() {
		return mode;
	}

	/**
	 * 设置召唤模式。
	 * Sets the summon mode.
	 *
	 * @param mode 要设置的模式 / the mode to set
	 */
	public void setMode(SummonMode mode) {
		this.mode = mode;
	}

	/** 是否敌对。 / Whether enemy. */
	@Override
	public boolean isEnemy(Creature creature) {
		return master != null ? master.isEnemy(creature) : false;
	}

	/**
	 * 判断 NPC 是否为敌对目标。
	 * Whether the NPC is an enemy.
	 *
	 * @param npc NPC / NPC
	 * @return 是否敌对 / whether enemy
	  */
	@Override
	public boolean isEnemyFrom(Npc npc) {
		return master != null ? master.isEnemyFrom(npc) : false;
	}

	/**
	 * 判断玩家是否为敌对目标。
	 * Whether the player is an enemy.
	 *
	 * @param player 玩家 / player
	 * @return 是否敌对 / whether enemy
	  */
	@Override
	public boolean isEnemyFrom(Player player) {
		return master != null ? master.isEnemyFrom(player) : false;
	}

	/** 获取部落。 / Returns the tribe. */
	@Override
	public TribeClass getTribe() {
		if (master == null) {
			return ((NpcTemplate) objectTemplate).getTribe();
		}
		return master.getTribe();
	}

	/**
	 * NPC 是否会对召唤物产生仇恨。
	 * Whether the NPC aggroes on the summon.
	 *
	 * @param npc NPC / NPC
	 * @return 是否产生仇恨 / whether aggro
	  */
	@Override
	public final boolean isAggroFrom(Npc npc) {
		if (getMaster() == null) {
			return false;
		}
		return getMaster().isAggroFrom(npc);
	}

	/** 返回移动控制器 / Returns the move controller */
	@Override
	public SummonMoveController getMoveController() {
		return (SummonMoveController) super.getMoveController();
	}

	/** 返回实际行动的生物 / Returns the acting creature */
	@Override
	public Creature getActingCreature() {
		return getMaster() == null ? this : getMaster();
	}

	/** 获取种族。 / Returns the race. */
	@Override
	public Race getRace() {
		return getMaster() != null ? getMaster().getRace() : Race.NONE;
	}

	/**
	 * 返回剩余存活秒数，永久召唤物返回 {@code 0}。
	 * Returns remaining live time in seconds, or {@code 0} for a permanent summon.
	 *
	 * @return 剩余存活秒数 / remaining live time in seconds
	 */
	public int getLiveTime() {
		return getLiveTime(System.nanoTime());
	}

	int getLiveTime(long currentTimeNanos) {
		if (liveTime <= 0) {
			return 0;
		}
		long remainingTime = expirationTimeNanos - currentTimeNanos;
		if (remainingTime <= 0) {
			return 0;
		}
		return (int) Math.min(Integer.MAX_VALUE, (remainingTime + NANOS_PER_SECOND - 1) / NANOS_PER_SECOND);
	}

	/** 是否已超过限时召唤的有效期。 / Whether this timed summon has expired. */
	public boolean isExpired() {
		return isExpired(System.nanoTime());
	}

	boolean isExpired(long currentTimeNanos) {
		return liveTime > 0 && getLiveTime(currentTimeNanos) == 0;
	}

	/**
	 * 设置存活时长（秒），0 表示永久。
	 * Sets the live time in seconds, 0 for permanent.
	 *
	 * @param liveTime 存活秒数 / live time in sec.
	 */
	public void setLiveTime(int liveTime) {
		setLiveTime(liveTime, System.nanoTime());
	}

	void setLiveTime(int liveTime, long currentTimeNanos) {
		this.liveTime = liveTime;
		expirationTimeNanos = liveTime > 0 ? currentTimeNanos + liveTime * NANOS_PER_SECOND : 0;
	}

	/** 设置释放任务 / Sets the release task. */
	public void setReleaseTask(Future<?> task) {
		releaseTask = task;
	}

	/** 取消释放任务 / Cancels the release task. */
	public void cancelReleaseTask() {
		if (releaseTask != null && !releaseTask.isDone()) {
			releaseTask.cancel(true);
		}
	}
}
