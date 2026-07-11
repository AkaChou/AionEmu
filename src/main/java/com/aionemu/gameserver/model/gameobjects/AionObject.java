package com.aionemu.gameserver.model.gameobjects;


import com.google.common.base.Function;

/**
 * 所有可交互游戏对象的基类（玩家、NPC、物品等）。
 * Base class for all interactive in-game objects (players, NPCs, items, etc.).
 *
 * @author -Nemesiss-, SoulKeeper
 */
public abstract class AionObject {

	public static Function<AionObject, Integer> OBJECT_TO_ID_TRANSFORMER = new Function<AionObject, Integer>() {
		/** 应用。 / Apply. */
		@Override
		public Integer apply(AionObject input) {
			return input != null ? input.getObjectId() : null;
		}
	};

	/**
	 * 所有游戏对象的唯一 ID：物品、玩家、怪物等。
	 * Unique id, for all game objects such as: items, players, monsters.
	 */
	private Integer objectId;

	public AionObject(Integer objId) {
		this.objectId = objId;
	}

	/**
	 * Returns unique ObjectId of AionObject
	 *
	 * @return Int ObjectId
	 */
	public Integer getObjectId() {
		return objectId;
	}

	/**
	 * 返回名称的 object.<br>Unique 用于 players , common 用于 NPCs ,物品, etc。 / Returns name of the object.<br> Unique for players, common for NPCs, items, etc
	 *
	 * @return name of the object
	 */
	public abstract String getName();
}
