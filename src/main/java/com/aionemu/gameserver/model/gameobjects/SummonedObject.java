package com.aionemu.gameserver.model.gameobjects;

import org.apache.commons.lang3.StringUtils;

import com.aionemu.gameserver.controllers.NpcController;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.container.NpcLifeStats;
import com.aionemu.gameserver.model.stats.container.SummonedObjectGameStats;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;

/**
 * 被召唤对象。
 * Summoned Object game object.
 *
 * @author ATracer
 */
public class SummonedObject<T extends VisibleObject> extends Npc {

	private byte level;

	/**
	 * 本召唤对象的创建者。
	 * Creator of this SummonedObject.
	 */
	private T creator;

	/**
	 * 构造被召唤对象。
	 * Constructs a summoned object.
	 *
	 * @param objId 对象 ID / object id
	 * @param controller NPC 控制器 / NPC controller
	 * @param spawnTemplate 生成模板 / spawn template
	 * @param objectTemplate NPC 模板 / NPC template
	 * @param level 等级 / level
	 */
	public SummonedObject(int objId, NpcController controller, SpawnTemplate spawnTemplate, NpcTemplate objectTemplate,
			byte level) {
		super(objId, controller, spawnTemplate, objectTemplate, level);
		this.level = level;
	}

	@Override
	protected void setupStatContainers(byte level) {
		setGameStats(new SummonedObjectGameStats(this));
		setLifeStats(new NpcLifeStats(this));
	}

	/** 获取等级。 / Returns the level. */
	@Override
	public byte getLevel() {
		return this.level;
	}

	/** 返回创建者 / Returns the creator */
	@Override
	public T getCreator() {
		return creator;
	}

	/** 设置创建者 / Sets the creator */
	public void setCreator(T creator) {
		if (creator instanceof Player) {
			((Player) creator).setSummonedObj(this);
		}
		this.creator = creator;
	}

	/** 返回主人名称 / Returns the master name */
	@Override
	public String getMasterName() {
		return creator != null ? creator.getName() : StringUtils.EMPTY;
	}

	/** 返回创建者 ID / Returns the creator id */
	@Override
	public int getCreatorId() {
		return creator != null ? creator.getObjectId() : 0;
	}

	/** 返回实际行动的生物 / Returns the acting creature */
	@Override
	public Creature getActingCreature() {
		if (creator instanceof Creature) {
			return (Creature) getCreator();
		}
		return this;
	}

	/** 返回主人 / Returns the master. */
	@Override
	public Creature getMaster() {
		if (creator instanceof Creature) {
			return (Creature) getCreator();
		}
		return this;
	}

	/** 获取种族。 / Returns the race. */
	@Override
	public Race getRace() {
		if (creator instanceof Creature) {
			return creator != null ? ((Creature) creator).getRace() : Race.NONE;
		}
		return super.getRace();
	}
}
