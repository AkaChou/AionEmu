package com.aionemu.gameserver.model.vortex;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.controllers.RVController;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Kisk;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.vortex.HomePoint;
import com.aionemu.gameserver.model.templates.vortex.ResurrectionPoint;
import com.aionemu.gameserver.model.templates.vortex.StartPoint;
import com.aionemu.gameserver.model.templates.vortex.VortexTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.vortexservice.DimensionalVortex;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldPosition;
import com.aionemu.gameserver.world.zone.InvasionZoneInstance;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.handler.ZoneHandler;

import java.util.LinkedHashMap;

/**
 * 漩涡位置模型。
 * Vortex Location model.
 */

public class VortexLocation implements ZoneHandler {
	protected boolean isActive;
	protected DimensionalVortex<VortexLocation> activeVortex;
	protected RVController vortexController;
	protected VortexTemplate template;
	protected int id;
	protected Race offenceRace;
	protected Race defendsRace;
	protected List<InvasionZoneInstance> zones;
	protected Map<Integer, Player> players = new HashMap<>();
	protected Map<Integer, Kisk> kisks = new LinkedHashMap<Integer, Kisk>();
	private final List<VisibleObject> spawned = new ArrayList<VisibleObject>();
	protected HomePoint home;
	protected ResurrectionPoint resurrection;
	protected StartPoint start;

	public VortexLocation() {
	}

	public VortexLocation(VortexTemplate template) {
		this.template = template;
		this.id = template.getId();
		this.offenceRace = template.getInvadersRace();
		this.defendsRace = template.getDefendersRace();
		this.zones = new ArrayList<InvasionZoneInstance>();
		this.home = template.getHomePoint();
		this.resurrection = template.getResurrectionPoint();
		this.start = template.getStartPoint();
	}

	/** 是否激活。 / Whether Active. */
	public boolean isActive() {
		return isActive;
	}

	/** 设置 active vortex / Sets the active vortex */
	public void setActiveVortex(DimensionalVortex<VortexLocation> vortex) {
		isActive = vortex != null;
		this.activeVortex = vortex;
	}

	/** 返回 active vortex / Returns the active vortex */
	public DimensionalVortex<VortexLocation> getActiveVortex() {
		return activeVortex;
	}

	/** 设置 vortex controller / Sets the vortex controller */
	public void setVortexController(RVController controller) {
		this.vortexController = controller;
	}

	/** 返回 vortex controller / Returns the vortex controller */
	public RVController getVortexController() {
		return vortexController;
	}

	/** 获取模板。 / Returns the template. */
	public final VortexTemplate getTemplate() {
		return template;
	}

	/** 返回 home point / Returns the home point */
	public WorldPosition getHomePoint() {
		return home.getHomePoint();
	}

	/** 返回 resurrection point / Returns the resurrection point */
	public WorldPosition getResurrectionPoint() {
		return resurrection.getResurrectionPoint();
	}

	/** 返回开始点 / Returns the start point*/
	public WorldPosition getStartPoint() {
		return start.getStartPoint();
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回 defenders race / Returns the defenders race */
	public Race getDefendersRace() {
		return defendsRace;
	}

	/** 返回 invaders race / Returns the invaders race */
	public Race getInvadersRace() {
		return offenceRace;
	}

	/** 返回 home world id / Returns the home world id */
	public int getHomeWorldId() {
		return home.getWorldId();
	}

	/** 返回 invasion world id / Returns the invasion world id */
	public int getInvasionWorldId() {
		return start.getWorldId();
	}

	/** 返回是否已刷新 / Returns the spawned */
	public List<VisibleObject> getSpawned() {
		return spawned;
	}

	/** 返回玩家集合 / Returns the players */
	public Map<Integer, Player> getPlayers() {
		return players;
	}

	/** 返回 invaders kisks / Returns the invaders kisks */
	public Map<Integer, Kisk> getInvadersKisks() {
		return kisks;
	}

	/**
	 * @param objId Whether invader inside / Whether invader inside
	 */
	public boolean isInvaderInside(int objId) {
		return isActive() && getVortexController().getPassedPlayers().containsKey(objId);
	}

	/**
	 * @param player Whether inside active vortex / Whether inside active vortex
	 */
	public boolean isInsideActiveVortex(Player player) {
		return isActive() && isInsideLocation(player);
	}

	/** 添加区域。 / Adds zone. */
	public void addZone(InvasionZoneInstance zone) {
		this.zones.add(zone);
		zone.addHandler(this);
	}

	/**
	 * @param creature Whether inside location / Whether inside location
	 */
	public boolean isInsideLocation(Creature creature) {
		if (zones.isEmpty()) {
			return false;
		}
		for (int i = 0; i < zones.size(); i++) {
			if (zones.get(i).isInsideCreature(creature)) {
				return true;
			}
		}
		return false;
	}

	/** 返回 zones / Returns the zones */
	public List<InvasionZoneInstance> getZones() {
		return zones;
	}

	/** 在 EnterZone / On Enter Zone */
	@Override
	public void onEnterZone(Creature creature, ZoneInstance zone) {
		if (creature instanceof Kisk) {
			if (creature.getRace().equals(getInvadersRace())) {
				kisks.put(creature.getObjectId(), (Kisk) creature);
			}
		} else if (creature instanceof Player) {
			Player player = (Player) creature;
			if (!players.containsKey(player.getObjectId())) {
				players.put(player.getObjectId(), player);
				if (isActive()) {
					if (player.getRace().equals(getInvadersRace())) {
						if (getVortexController().getPassedPlayers().containsKey(player.getObjectId())
								&& !getActiveVortex().getInvaders().containsKey(player.getObjectId())) {
							getActiveVortex().addPlayer(player, true);
						}
					} else {
						getActiveVortex().updateDefenders(player);
					}
				}
			}
		}
	}

	/** 在 LeaveZone / On Leave Zone */
	@Override
	public void onLeaveZone(Creature creature, ZoneInstance zone) {
		if (!isInsideLocation(creature)) {
			if (creature instanceof Kisk) {
				kisks.remove(creature.getObjectId());
			}
			if (creature instanceof Player) {
				final Player player = (Player) creature;
				players.remove(player.getObjectId());
				if (isActive()) {
					if (player.getRace().equals(getInvadersRace())) {
						if (getVortexController().getPassedPlayers().containsKey(player.getObjectId())) {
							PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(904305));
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
								/** 运行 / run. */
								@Override
								public void run() {
									if (player.isOnline() && !isInsideActiveVortex(player)) {
										getActiveVortex().kickPlayer(player, true);
									}
								}
							}, 10 * 1000);
						}
					} else {
						GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							/** 运行 / run. */
							@Override
							public void run() {
								if (player.isOnline() && !isInsideActiveVortex(player)) {
									getActiveVortex().kickPlayer(player, false);
								}
							}
						}, 10 * 1000);
					}
				}
			}
		}
	}
}
