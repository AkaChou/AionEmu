package com.aionemu.gameserver.dataholders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;

/**
 * 新玩家初始数据表，包含创建职业物品与阵营出生点。
 * Initial data table for new players, including class starter items and race spawn locations.
 * <br/>
 * Created on: 09.08.2009 18:20:41
 *
 * @author Aquanox
 */
@XmlRootElement(name = "player_initial_data")
@XmlAccessorType(XmlAccessType.FIELD)
public class PlayerInitialData {

	@XmlElement(name = "player_data")
	private List<PlayerCreationData> dataList = new ArrayList<PlayerCreationData>();

	@XmlElement(name = "elyos_spawn_location", required = true)
	private LocationData elyosSpawnLocation;
	@XmlElement(name = "asmodian_spawn_location", required = true)
	private LocationData asmodianSpawnLocation;

	private Map<PlayerClass, PlayerCreationData> data = new LinkedHashMap<PlayerClass, PlayerCreationData>();

	/**
	 * JAXB 反序列化完成后，按职业索引创建数据并释放列表。
	 * After JAXB unmarshalling, indexes creation data by class and releases the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (PlayerCreationData pt : dataList) {
			data.put(pt.getRequiredPlayerClass(), pt);
		}

		dataList.clear();
		dataList = null;
	}

	/**
	 * 按职业获取玩家创建数据。
	 * Returns player creation data for the given class.
	 *
	 * @param cls 玩家职业 / player class
	 * @return 创建数据，不存在则为 null / creation data or null
	 */
	public PlayerCreationData getPlayerCreationData(PlayerClass cls) {
		return data.get(cls);
	}

	/**
	 * 返回已加载的职业创建数据数量。
	 * Returns the number of loaded class creation entries.
	 *
	 * entry count
	 */
	public int size() {
		return data.size();
	}

	/**
	 * 按阵营获取出生坐标。
	 * Returns the spawn location for the given race.
	 *
	 * 阵营 / race
	 * @return 出生坐标数据 / spawn location data
	 * if race is unsupported。
	 */
	public LocationData getSpawnLocation(Race race) {
		switch (race) {
		case ASMODIANS:
			return asmodianSpawnLocation;
		case ELYOS:
			return elyosSpawnLocation;
		default:
			throw new IllegalArgumentException();
		}
	}

	/**
	 * 玩家创建数据持有者，描述职业初始物品。
	 * Player creation data holder describing class starter items.
	 */
	public static class PlayerCreationData {

		@XmlAttribute(name = "class")
		private PlayerClass requiredPlayerClass;

		@XmlElement(name = "items")
		private ItemsType itemsType;

		// @XmlElement(name="shortcuts")
		// private ShortcutType shortcutData;

		PlayerClass getRequiredPlayerClass() {
			return requiredPlayerClass;
		}

		/**
		 * 返回该职业不可变的初始物品列表。
		 * Returns the unmodifiable starter item list for this class.
		 *
		 * @return 初始物品列表 / starter item list
		 */
		public List<ItemType> getItems() {
			return Collections.unmodifiableList(itemsType.items);
		}

		static class ItemsType {

			@XmlElement(name = "item")
			public List<ItemType> items = new ArrayList<ItemType>();
		}

		/**
		 * 初始物品条目：模板 ID 与数量。
		 * Starter item entry: template id and count.
		 */
		public static class ItemType {

			@XmlAttribute(name = "id")
			public int templateId;

			@XmlAttribute(name = "count")
			public int count;

			/**
			 * 返回该物品对应的物品模板。
			 * Returns the item template for this entry.
			 *
			 * item template
			 */
			public ItemTemplate getTemplate() {
				return DataManager.ITEM_DATA.getItemTemplate(templateId);
			}

			/**
			 * 返回物品数量。
			 * Returns the item count.
			 *
			 * count
			 */
			public int getCount() {
				return count;
			}

			@Override
			public String toString() {
				final StringBuilder sb = new StringBuilder();
				sb.append("ItemType");
				sb.append("{templateId=").append(templateId);
				sb.append(", count=").append(count);
				sb.append('}');
				return sb.toString();
			}
		}
		// public static class ShortcutType
		// {
		// public List<Shortcut> shortcuts;
		// }
	}

	/**
	 * 出生坐标数据持有者。
	 * Spawn location data holder.
	 */
	public static class LocationData {

		@XmlAttribute(name = "map_id")
		private int mapId;
		@XmlAttribute(name = "x")
		private float x;
		@XmlAttribute(name = "y")
		private float y;
		@XmlAttribute(name = "z")
		private float z;
		@XmlAttribute(name = "heading")
		private byte heading;

		LocationData() {

		}

		/**
		 * 返回地图 ID。
		 * Returns the map id.
		 *
		 * map id
		 */
		public int getMapId() {
			return mapId;
		}

		/**
		 * 返回 X 坐标。
		 * Returns the X coordinate.
		 *
		 * X 坐标 / X coordinate
		 */
		public float getX() {
			return x;
		}

		/**
		 * 返回 Y 坐标。
		 * Returns the Y coordinate.
		 *
		 * Y 坐标 / Y coordinate
		 */
		public float getY() {
			return y;
		}

		/**
		 * 返回 Z 坐标。
		 * Returns the Z coordinate.
		 *
		 * Z 坐标 / Z coordinate
		 */
		public float getZ() {
			return z;
		}

		/**
		 * 返回朝向。
		 * Returns the heading.
		 *
		 * 朝向 / heading
		 */
		public byte getHeading() {
			return heading;
		}
	}
}
