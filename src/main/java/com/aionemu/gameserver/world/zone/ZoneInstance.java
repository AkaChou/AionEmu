package com.aionemu.gameserver.world.zone;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.geometry.Area;
import com.aionemu.gameserver.model.templates.zone.ZoneClassName;
import com.aionemu.gameserver.model.templates.zone.ZoneInfo;
import com.aionemu.gameserver.model.templates.zone.ZoneTemplate;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.zone.handler.AdvencedZoneHandler;
import com.aionemu.gameserver.world.zone.handler.ZoneHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 区域运行时实例：跟踪区内生物、派发进入/离开/死亡，并解析区域属性标志。
 * Runtime zone instance: tracks creatures inside, dispatches enter/leave/die, and resolves zone attribute flags.
 *
 * @author ATracer
 */
public class ZoneInstance implements Comparable<ZoneInstance> {

	/** 区域模板信息 / zone template info */
	private ZoneInfo template;
	/** 所属地图 ID / owning map id */
	private int mapId;
	/** 区内生物集合 / creatures inside the zone */
	private Map<Integer, Creature> creatures = new LinkedHashMap<Integer, Creature>();
	/** 区域事件处理器列表 / zone event handlers */
	protected List<ZoneHandler> handlers = new ArrayList<ZoneHandler>();

	/**
	 * 创建区域实例。
	 * Create a zone instance.
	 *
	 * map id
	 * @param template 区域模板信息 / zone template info
	 */
	public ZoneInstance(int mapId, ZoneInfo template) {
		this.template = template;
		this.mapId = mapId;
	}

	/**
	 * 返回区域几何面积模板。
	 * Return the zone geometry area template.
	 *
	 * area template
	 */
	public Area getAreaTemplate() {
		return template.getArea();
	}

	/**
	 * 返回区域数据模板。
	 * Return the zone data template.
	 *
	 * zone template
	 */
	public ZoneTemplate getZoneTemplate() {
		return template.getZoneTemplate();
	}

	/**
	 * 重新校验生物是否仍在本区域内。
	 * Revalidate whether the creature is still inside this zone.
	 *
	 * creature
	 * whether inside
	 */
	public boolean revalidate(Creature creature) {
		return (mapId == creature.getWorldId()
				&& template.getArea().isInside3D(creature.getX(), creature.getY(), creature.getZ()));
	}

	/**
	 * 生物进入区域：登记并通知控制器与处理器。
	 * Creature enters the zone: register and notify controller and handlers.
	 *
	 * creature
	 *
	 * @param creature @return 是否成功进入（已在区内则为 false） / whether enter succeeded (false if already inside)
	 */
	public synchronized boolean onEnter(Creature creature) {
		if (creatures.containsKey(creature.getObjectId())) {
			return false;
		}
		creatures.put(creature.getObjectId(), creature);
		if (creature instanceof Player) {
			creature.getController().onEnterZone(this);
		}
		for (int i = 0; i < handlers.size(); i++) {
			handlers.get(i).onEnterZone(creature, this);
		}
		return true;
	}

	/**
	 * 生物离开区域：移除并通知控制器与处理器。
	 * Creature leaves the zone: remove and notify controller and handlers.
	 *
	 * creature
	 *
	 * @param creature @return 是否成功离开（不在区内则为 false） / whether leave succeeded (false if not inside)
	 */
	public synchronized boolean onLeave(Creature creature) {
		if (!creatures.containsKey(creature.getObjectId())) {
			return false;
		}
		creatures.remove(creature.getObjectId());
		creature.getController().onLeaveZone(this);
		for (int i = 0; i < handlers.size(); i++) {
			handlers.get(i).onLeaveZone(creature, this);
		}
		return true;
	}

	/**
	 * 区内死亡事件：依次询问扩展处理器是否处理。
	 * In-zone death event: ask advanced handlers in order whether they handle it.
	 *
	 * attacker
	 * dead target
	 *
	 * @return 是否已被处理 / whether handled
	 */
	public boolean onDie(Creature attacker, Creature target) {
		if (!creatures.containsKey(target.getObjectId())) {
			return false;
		}
		for (int i = 0; i < handlers.size(); i++) {
			ZoneHandler handler = handlers.get(i);
			if (handler instanceof AdvencedZoneHandler) {
				if (((AdvencedZoneHandler) handler).onDie(attacker, target, this)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 生物是否登记在本区域内。
	 * Whether the creature is registered inside this zone.
	 *
	 * creature
	 * whether inside
	 */
	public boolean isInsideCreature(Creature creature) {
		return creatures.containsKey(creature.getObjectId());
	}

	/**
	 * 三维坐标是否落在本区域几何内。
	 * Whether the 3D coordinates fall inside this zone's geometry.
	 *
	 * @param x X 坐标 / X coordinate
	 * @param y Y 坐标 / Y coordinate
	 * @param z Z 坐标 / Z coordinate
	 * whether inside
	 */
	public boolean isInsideCordinate(float x, float y, float z) {
		return template.getArea().isInside3D(x, y, z);
	}

	/**
	 * 按优先级与名称 ID 比较区域实例。
	 * Compare zone instances by priority then name id.
	 *
	 * @param o 另一区域实例 / other zone instance
	 * comparison result
	 */
	@Override
	public int compareTo(ZoneInstance o) {
		int result = getZoneTemplate().getPriority() - o.getZoneTemplate().getPriority();
		if (result == 0) {
			return template.getZoneTemplate().getName().id() - o.template.getZoneTemplate().getName().id();
		}
		return result;
	}

	/**
	 * 添加区域事件处理器。
	 * Add a zone event handler.
	 *
	 * handler
	 */
	public void addHandler(ZoneHandler handler) {
		this.handlers.add(handler);
	}

	/**
	 * 是否允许飞行（区域标志或地图覆盖）。
	 * Whether flying is allowed (zone flags or map override).
	 *
	 * whether fly is allowed
	 */
	public boolean canFly() {
		if (template.getZoneTemplate().getFlags() == -1 || template.getZoneTemplate().getFlags() == 0
				|| com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(mapId).hasOverridenOption(ZoneAttributes.FLY)) {
			return com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(mapId).isPossibleFly();
		}
		return (template.getZoneTemplate().getFlags() & ZoneAttributes.FLY.getId()) != 0;
	}

	/**
	 * 是否允许滑翔。
	 * Whether gliding is allowed.
	 *
	 * @return 是否可滑翔 / whether glide is allowed
	 */
	public boolean canGlide() {
		if (template.getZoneTemplate().getFlags() == -1 || template.getZoneTemplate().getFlags() == 0
				|| com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(mapId).hasOverridenOption(ZoneAttributes.GLIDE)) {
			return com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(mapId).canGlide();
		}
		return (template.getZoneTemplate().getFlags() & ZoneAttributes.GLIDE.getId()) != 0;
	}

	/**
	 * 是否允许放置绑定点（Kisk）。
	 * Whether placing a bind point (kisk) is allowed.
	 *
	 * @return 是否可放绑定点 / whether kisk placement is allowed
	 */
	public boolean canPutKisk() {
		if (template.getZoneTemplate().getFlags() == -1 || template.getZoneTemplate().getFlags() == 0
				|| com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(mapId).hasOverridenOption(ZoneAttributes.BIND)) {
			return com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(mapId).canPutKisk();
		}
		return (template.getZoneTemplate().getFlags() & ZoneAttributes.BIND.getId()) != 0;
	}

	/**
	 * 是否允许召回。
	 * Whether recall is allowed.
	 *
	 * @return 是否可召回 / whether recall is allowed
	 */
	public boolean canRecall() {
		if (template.getZoneTemplate().getFlags() == -1 || template.getZoneTemplate().getFlags() == 0
				|| com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(mapId).hasOverridenOption(ZoneAttributes.RECALL)) {
			return com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(mapId).canRecall();
		}
		return (template.getZoneTemplate().getFlags() & ZoneAttributes.RECALL.getId()) != 0;
	}

	/**
	 * 是否允许坐骑。
	 * Whether riding is allowed.
	 *
	 * @return 是否可坐骑 / whether ride is allowed
	 */
	public boolean canRide() {
		if (template.getZoneTemplate().getFlags() == -1 || template.getZoneTemplate().getFlags() == 0
				|| com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(mapId).hasOverridenOption(ZoneAttributes.RIDE)) {
			return com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(mapId).canRide();
		}
		return (template.getZoneTemplate().getFlags() & ZoneAttributes.RIDE.getId()) != 0;
	}

	/**
	 * 是否允许飞行坐骑。
	 * Whether fly-riding is allowed.
	 *
	 * @return 是否可飞行坐骑 / whether fly-ride is allowed
	 */
	public boolean canFlyRide() {
		if (template.getZoneTemplate().getFlags() == -1 || template.getZoneTemplate().getFlags() == 0
				|| com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(mapId).hasOverridenOption(ZoneAttributes.FLY_RIDE)) {
			return com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(mapId).canFlyRide();
		}
		return (template.getZoneTemplate().getFlags() & ZoneAttributes.FLY_RIDE.getId()) != 0;
	}

	/**
	 * 是否允许 PvP。
	 * Whether PvP is allowed.
	 *
	 * whether PvP is allowed
	 */
	public boolean isPvpAllowed() {
		if (template.getZoneTemplate().getZoneType() != ZoneClassName.PVP) {
			return com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(mapId).isPvpAllowed();
		}
		return (template.getZoneTemplate().getFlags() & ZoneAttributes.PVP_ENABLED.getId()) != 0;
	}

	/**
	 * 是否允许同族决斗。
	 * Whether same-race duels are allowed.
	 *
	 * @return 是否允许同族决斗 / whether same-race duels are allowed
	 */
	public boolean isSameRaceDuelsAllowed() {
		if (template.getZoneTemplate().getZoneType() != ZoneClassName.DUEL || template.getZoneTemplate().getFlags() == 0
				|| com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(mapId).hasOverridenOption(ZoneAttributes.DUEL_SAME_RACE_ENABLED)) {
			return com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(mapId).isSameRaceDuelsAllowed();
		}
		return (template.getZoneTemplate().getFlags() & ZoneAttributes.DUEL_SAME_RACE_ENABLED.getId()) != 0;
	}

	/**
	 * 是否允许异族决斗。
	 * Whether other-race duels are allowed.
	 *
	 * @return 是否允许异族决斗 / whether other-race duels are allowed
	 */
	public boolean isOtherRaceDuelsAllowed() {
		if (template.getZoneTemplate().getZoneType() != ZoneClassName.DUEL || template.getZoneTemplate().getFlags() == 0
				|| com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(mapId).hasOverridenOption(ZoneAttributes.DUEL_OTHER_RACE_ENABLED)) {
			return com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(mapId).isOtherRaceDuelsAllowed();
		}
		return (template.getZoneTemplate().getFlags() & ZoneAttributes.DUEL_OTHER_RACE_ENABLED.getId()) != 0;
	}

	/**
	 * 返回区域所属城镇 ID。
	 * Return the town id associated with this zone.
	 *
	 * town id
	 */
	public int getTownId() {
		return template.getZoneTemplate().getTownId();
	}

	/**
	 * 返回区内生物集合。
	 * Return the map of creatures inside the zone.
	 *
	 * creature map
	 */
	public Map<Integer, Creature> getCreatures() {
		return creatures;
	}
}
