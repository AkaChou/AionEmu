package com.aionemu.gameserver.model.siege;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.CollisionDieActor;
import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.geoEngine.scene.DespawnableNode;
import com.aionemu.gameserver.geoEngine.scene.Spatial;
import com.aionemu.gameserver.lifecycle.GameFeatureServices;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.handler.ZoneHandler;

/**
 * 要塞护盾模型。
 * Siege Shield model.
 *
 * @author Rolandas
 */
public class SiegeShield implements ZoneHandler {

	Map<Integer, ActionObserver> observed = new ConcurrentHashMap<Integer, ActionObserver>();
	private Spatial geometry;
	private int siegeLocationId;
	private boolean isEnabled = false;

	public SiegeShield(Spatial geometry) {
		this.geometry = geometry;
		if (geometry.getParent() instanceof DespawnableNode) {
			((DespawnableNode) geometry.getParent()).setType(DespawnableNode.DespawnableType.SHIELD);
		}
	}

	/** 获取几何。 / Returns the geometry. */
	public Spatial getGeometry() {
		return geometry;
	}

	/** 进入区域时 / On Enter Zone */
	@Override
	public void onEnterZone(Creature creature, ZoneInstance zone) {
		if (!(creature instanceof Player)) {
			return;
		}
		Player player = (Player) creature;
		if (GeoDataConfig.GEO_SHIELDS_ENABLE && (isEnabled || siegeLocationId == 0)) {
			FortressLocation loc = GameFeatureServices.siegeService().getFortress(siegeLocationId);
			if (loc == null || loc.getRace() != SiegeRace.getByRace(player.getRace())) {
				CollisionDieActor actor = new CollisionDieActor(creature, geometry);
				creature.getObserveController().addObserver(actor);
				observed.put(creature.getObjectId(), actor);
			}
		}
	}

	/** 离开区域时 / On Leave Zone */
	@Override
	public void onLeaveZone(Creature creature, ZoneInstance zone) {
		ActionObserver actor = observed.get(creature.getObjectId());
		if (actor != null) {
			creature.getObserveController().removeObserver(actor);
			observed.remove(creature.getObjectId());
		}
	}

	/** 设置启用状态 / Sets the enabled */
	public void setEnabled(boolean enable) {
		isEnabled = enable;
	}

	/** 是否启用。 / Whether Enabled. */
	public boolean isEnabled() {
		return isEnabled;
	}

	/** 返回攻城地点 ID / Returns the siege location id */
	public int getSiegeLocationId() {
		return siegeLocationId;
	}

	/** 设置攻城地点 ID / Sets the siege location id */
	public void setSiegeLocationId(int siegeLocationId) {
		this.siegeLocationId = siegeLocationId;
		if (geometry.getParent() instanceof DespawnableNode) {
			((DespawnableNode) geometry.getParent()).setId(siegeLocationId);
		}
	}

	/** 返回字符串表示。 / Returns string representation. */
	@Override
	public String toString() {
		return "LocId=" + siegeLocationId + "; Name=" + geometry.getName() + "; Bounds=" + geometry.getWorldBound();
	}
}
