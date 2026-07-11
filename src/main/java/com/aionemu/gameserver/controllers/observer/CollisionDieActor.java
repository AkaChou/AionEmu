package com.aionemu.gameserver.controllers.observer;

import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.collision.CollisionResults;
import com.aionemu.gameserver.geoEngine.scene.Spatial;
import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * 碰撞致死行为者：与材质几何碰撞时杀死生物。
 * Collision die actor: kills the creature when colliding with material geometry.
 *
 * @author Rolandas
 */
public class CollisionDieActor extends AbstractCollisionObserver implements IActor {
	/** 是否启用 / Whether enabled */
	private boolean isEnabled = true;

	/**
	 * @param creature 被观察生物 / observed creature
	 * @param geometry 碰撞几何体 / collision geometry
	 */
	public CollisionDieActor(Creature creature, Spatial geometry) {
		super(creature, geometry, CollisionIntention.MATERIAL.getId());
	}

	@Override
	public void setEnabled(boolean enable) {
		isEnabled = enable;
	}

	@Override
	public void onMoved(CollisionResults collisionResults) {
		if (isEnabled && collisionResults.size() != 0) {
			act();
		}
	}

	@Override
	public void act() {
		if (isEnabled) {
			creature.getController().die();
		}
	}

	@Override
	public void abort() {
	}
}
