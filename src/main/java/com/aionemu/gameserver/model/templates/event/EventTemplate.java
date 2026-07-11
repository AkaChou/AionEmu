package com.aionemu.gameserver.model.templates.event;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlList;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.SpawnsData2;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.Guides.GuideTemplate;
import com.aionemu.gameserver.model.templates.spawns.Spawn;
import com.aionemu.gameserver.model.templates.spawns.SpawnMap;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.gametime.DateTimeUtil;
import com.aionemu.gameserver.world.World;

/**
 * 活动模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EventTemplate")
@Slf4j
public class EventTemplate {


	@XmlElement(name = "event_drops", required = false)
	protected EventDrops eventDrops;

	@XmlElement(name = "quests", required = false)
	protected EventQuestList quests;

	@XmlElement(name = "spawns", required = false)
	protected SpawnsData2 spawns;

	@XmlElement(name = "inventory_drop", required = false)
	protected List<InventoryDrop> inventoryDrops;

	@XmlList
	@XmlElement(name = "surveys", required = false)
	protected List<String> surveys;

	@XmlAttribute(name = "name", required = true)
	protected String name;

	@XmlAttribute(name = "start", required = true)
	@XmlSchemaType(name = "dateTime")
	protected XMLGregorianCalendar startDate;

	@XmlAttribute(name = "end", required = true)
	@XmlSchemaType(name = "dateTime")
	protected XMLGregorianCalendar endDate;

	@XmlAttribute(name = "theme", required = false)
	private String theme;

	@XmlTransient
	protected List<VisibleObject> spawnedObjects;

	@XmlTransient
	private List<Future<?>> invDropTasks = null;

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return name;
	}

	/** 活动掉落。 / Event Drop. */
	public EventDrops EventDrop() {
		return eventDrops;
	}

	/** 返回开始日期 / Returns the start date*/
	public ZonedDateTime getStartDate() {
		return DateTimeUtil.fromCalendar(startDate.toGregorianCalendar());
	}

	/** 返回结束日期 / Returns the end date*/
	public ZonedDateTime getEndDate() {
		return DateTimeUtil.fromCalendar(endDate.toGregorianCalendar());
	}

	/** 返回 startable quests / Returns the startable quests */
	public List<Integer> getStartableQuests() {
		if (quests == null) {
			return new ArrayList<Integer>();
		}
		return quests.getStartableQuests();
	}

	/** 返回 maintainable quests / Returns the maintainable quests */
	public List<Integer> getMaintainableQuests() {
		if (quests == null) {
			return new ArrayList<Integer>();
		}
		return quests.getMaintainQuests();
	}

	/** 是否激活。 / Whether Active. */
	public boolean isActive() {
		ZonedDateTime now = DateTimeUtil.now();
		return getStartDate().isBefore(now) && getEndDate().isAfter(now);
	}

	/**
	 * @return Whether expired / Whether expired
	 */
	public boolean isExpired() {
		return !isActive();
	}

	@XmlTransient
	volatile boolean isStarted = false;

	/** 设置 started / Sets the started */
	public void setStarted() {
		isStarted = true;
	}

	/**
	 * @return Whether started / Whether started
	 */
	public boolean isStarted() {
		return isStarted;
	}

	/** 开始 / Start. */
	public void Start() {
		if (isStarted) {
			return;
		}
		if (spawns != null && spawns.size() > 0) {
			if (spawnedObjects == null) {
				spawnedObjects = new ArrayList<VisibleObject>();
			}
			int spawnCount = 0;
			for (SpawnMap map : spawns.getTemplates()) {
				DataManager.SPAWNS_DATA2.addNewSpawnMap(map);
				Collection<Integer> instanceIds = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(map.getMapId()).getAvailableInstanceIds();
				for (Integer instanceId : instanceIds) {
					for (Spawn spawn : map.getSpawns()) {
						spawn.setEventTemplate(this);
						for (SpawnSpotTemplate spot : spawn.getSpawnSpotTemplates()) {
							SpawnTemplate t = SpawnEngine.addNewSpawn(map.getMapId(), spawn.getNpcId(), spot.getX(), spot.getY(), spot.getZ(), spot.getHeading(), spawn.getRespawnTime());
							t.setEventTemplate(this);
							SpawnEngine.spawnObject(t, instanceId);
							spawnCount++;
						}
					}
				}
			}
			log.info(I18n.get("log.ac12f1e2f672", spawnCount, this.getName()));
			DataManager.SPAWNS_DATA2.afterUnmarshal(null, null);
			DataManager.SPAWNS_DATA2.clearTemplates();
		}
		if (inventoryDrops != null) {
			invDropTasks = new ArrayList<>();
			for (InventoryDrop inventoryDrop : inventoryDrops) {
				invDropTasks.add(GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(
					() -> com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(player -> dropInventoryItem(player, inventoryDrop)),
					inventoryDrop.getInterval() * 60000, inventoryDrop.getInterval() * 60000));
			}
		}
		if (surveys != null) {
			for (String survey : surveys) {
				GuideTemplate template = DataManager.GUIDE_HTML_DATA.getTemplateByTitle(survey);
				if (template != null) {
					template.setActivated(true);
				}
			}
		}
		isStarted = true;
	}

	private void dropInventoryItem(Player player, InventoryDrop inventoryDrop) {
		int level = player.getCommonData().getLevel();
		if (level < inventoryDrop.getStartLevel() || inventoryDrop.getEndLevel() > 0 && level > inventoryDrop.getEndLevel()) {
			return;
		}
		int itemId = inventoryDrop.getDropItem();
		if (inventoryDrop.getMaxCountOfDay() > 0) {
			if (player.getItemMaxThisCount(itemId) >= inventoryDrop.getMaxCountOfDay()) {
				return;
			}
			ItemService.dropItemToInventory(player, itemId, inventoryDrop.getCount());
			player.addItemMaxCountOfDay(itemId, player.getItemMaxThisCount(itemId) + 1);
		} else {
			ItemService.dropItemToInventory(player, itemId, inventoryDrop.getCount());
		}
	}

	/** 停止 / Stop. */
	public void Stop() {
		if (!isStarted) {
			return;
		}
		if (spawnedObjects != null) {
			for (VisibleObject o : spawnedObjects) {
				if (o.isSpawned()) {
					o.getController().delete();
				}
			}
			DataManager.SPAWNS_DATA2.removeEventSpawnObjects(spawnedObjects);
			log.info(I18n.get("log.81239f5579ea", spawnedObjects.size(), this.getName()));
			spawnedObjects.clear();
			spawnedObjects = null;
		}
		if (invDropTasks != null) {
			for (Future<?> invDropTask : invDropTasks) {
				invDropTask.cancel(false);
			}
			invDropTasks = null;
		}
		if (surveys != null) {
			for (String survey : surveys) {
				GuideTemplate template = DataManager.GUIDE_HTML_DATA.getTemplateByTitle(survey);
				if (template != null) {
					template.setActivated(false);
				}
			}
		}
		isStarted = false;
	}

	/** Adds 已刷新对象 / Adds spawned object */
	public void addSpawnedObject(VisibleObject object) {
		if (spawnedObjects == null) {
			spawnedObjects = new ArrayList<VisibleObject>();
		}
		spawnedObjects.add(object);
	}

	/** 返回主题 / Returns the theme */
	public String getTheme() {
		if (theme != null) {
			return theme.toLowerCase();
		}
		return theme;
	}

	/** 获取背包掉落。 / Returns the inventory drop. */
	public InventoryDrop getInventoryDrop() {
		return inventoryDrops == null || inventoryDrops.isEmpty() ? null : inventoryDrops.get(0);
	}
}
