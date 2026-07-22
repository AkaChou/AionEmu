package com.aionemu.gameserver.world.zone;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.scripting.classlistener.AggregatedClassListener;
import com.aionemu.commons.scripting.classlistener.OnClassLoadUnloadListener;
import com.aionemu.commons.scripting.classlistener.ScheduledTaskClassListener;
import com.aionemu.commons.scripting.CompiledScriptLoader;
import com.aionemu.gameserver.GameServerError;
import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.ZoneData;
import com.aionemu.gameserver.geoEngine.scene.Spatial;
import com.aionemu.gameserver.model.GameEngine;
import com.aionemu.gameserver.model.geometry.Area;
import com.aionemu.gameserver.model.geometry.CylinderArea;
import com.aionemu.gameserver.model.geometry.PolyArea;
import com.aionemu.gameserver.model.geometry.SemisphereArea;
import com.aionemu.gameserver.model.geometry.SphereArea;
import com.aionemu.gameserver.model.siege.SiegeLocation;
import com.aionemu.gameserver.model.siege.SiegeShield;
import com.aionemu.gameserver.model.templates.materials.MaterialTemplate;
import com.aionemu.gameserver.model.templates.world.WorldMapTemplate;
import com.aionemu.gameserver.model.templates.zone.MaterialZoneTemplate;
import com.aionemu.gameserver.model.templates.zone.WorldZoneTemplate;
import com.aionemu.gameserver.model.templates.zone.ZoneInfo;
import com.aionemu.gameserver.model.templates.zone.ZoneTemplate;
import com.aionemu.gameserver.model.vortex.VortexLocation;
import com.aionemu.gameserver.services.ShieldService;
import com.aionemu.gameserver.world.zone.handler.GeneralZoneHandler;
import com.aionemu.gameserver.world.zone.handler.MaterialZoneHandler;
import com.aionemu.gameserver.world.zone.handler.ZoneHandler;
import com.aionemu.gameserver.world.zone.handler.ZoneHandlerClassListener;
import com.aionemu.gameserver.world.zone.handler.ZoneNameAnnotation;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 区域服务：加载区域脚本、按地图构建区域实例、创建材质碰撞区。
 * Zone service: loads zone scripts, builds zone instances per map, creates material collision zones.
 *
 * @author ATracer
 * @author antness
 */
@Slf4j
public final class ZoneService implements GameEngine {

	/** 可选 Spring 单例提供者 / optional Spring singleton provider */
	private static volatile ObjectProvider<ZoneService> instanceProvider;
	/** 按地图 ID 索引的区域信息 / zone info indexed by map id */
	private IntObjectHashMap<List<ZoneInfo>> zoneByMapIdMap;
	/** 区域名称 → 脚本处理器类 / zone name → script handler class */
	private final Map<ZoneName, Class<? extends ZoneHandler>> handlers = new HashMap<ZoneName, Class<? extends ZoneHandler>>();
	/** 区域名称 → 可碰撞处理器实例 / zone name → collidable handler instance */
	private final Map<ZoneName, ZoneHandler> collidableHandlers = new ConcurrentHashMap<ZoneName, ZoneHandler>();
	/** 默认空处理器 / default no-op handler */
	public static final ZoneHandler DUMMY_ZONE_HANDLER = new GeneralZoneHandler();

	/**
	 * 从静态数据初始化区域索引。
	 * Initialize the zone index from static data.
	 */
	public ZoneService() {
		this.zoneByMapIdMap = DataManager.ZONE_DATA.getZones();
	}

	/**
	 * 获取单例：优先 Spring 提供者，否则回退内部持有者。
	 * Get the singleton: prefer Spring provider, otherwise fall back to the internal holder.
	 *
	 * zone service
	 */
	public static ZoneService getInstance() {
		ObjectProvider<ZoneService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/**
	 * 设置 Spring 单例提供者。
	 * Set the Spring singleton provider.
	 *
	 * provider
	 */
	public static void setInstanceProvider(ObjectProvider<ZoneService> provider) {
		instanceProvider = provider;
	}

	/**
	 * 内部单例持有者。
	 * Internal singleton holder.
	 */
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {

		protected static final ZoneService instance = new ZoneService();
	}

	/**
	 * 为指定区域名称创建新的处理器实例；无脚本则返回默认空处理器。
	 * Create a new handler instance for the given zone name; returns the dummy handler if none is registered.
	 *
	 * zone name
	 *
	 * @param zoneName
	 * @return 区域处理器 / zone handler
	 */
	public ZoneHandler getNewZoneHandler(ZoneName zoneName) {
		ZoneHandler zoneHandler = collidableHandlers.get(zoneName);
		if (zoneHandler != null) {
			return zoneHandler;
		}
		Class<? extends ZoneHandler> zoneClass = handlers.get(zoneName);
		if (zoneClass != null) {
			try {
				zoneHandler = zoneClass.getDeclaredConstructor().newInstance();
			} catch (ReflectiveOperationException ex) {
				log.warn(I18n.get("log.8cafdba4d507", zoneName, ex), ex);
			}
		}
		if (zoneHandler == null) {
			zoneHandler = DUMMY_ZONE_HANDLER;
		}
		return zoneHandler;
	}

	/**
	 * 根据 {@link ZoneNameAnnotation} 注册区域处理器类。
	 * Register a zone handler class based on {@link ZoneNameAnnotation}.
	 *
	 * handler class
	 */
	public final void addZoneHandlerClass(Class<? extends ZoneHandler> handler) {
		ZoneNameAnnotation idAnnotation = handler.getAnnotation(ZoneNameAnnotation.class);
		if (idAnnotation != null) {
			String[] zoneNames = idAnnotation.value().split(" ");
			for (String zoneNameString : zoneNames) {
				try {
					ZoneName zoneName = ZoneName.get(zoneNameString.trim());
					if (zoneName == ZoneName.get("NONE")) {
						throw new RuntimeException();
					}
					handlers.put(zoneName, handler);
				} catch (Exception e) {
					log.warn(I18n.get("log.20f41b75299b", idAnnotation.value()));
				}
			}
		}
	}

	/**
	 * 将处理器类直接绑定到指定区域名称。
	 * Bind a handler class directly to the given zone name.
	 *
	 * zone name
	 * handler class
	 */
	public final void addZoneHandlerClass(ZoneName zoneName, Class<? extends ZoneHandler> handler) {
		handlers.put(zoneName, handler);
	}

	/**
	 * 加载区域脚本处理器。
	 * Load zone script handlers.
	 *
	 * progress latch
	 */
	@Override
	public void load(CountDownLatch progressLatch) {
		log.info(I18n.get("log.b89b79895cf7"));

		AggregatedClassListener acl = new AggregatedClassListener();
		acl.addClassListener(new OnClassLoadUnloadListener());
		acl.addClassListener(new ScheduledTaskClassListener());
		acl.addClassListener(new ZoneHandlerClassListener());

		try {
			acl.postLoad(CompiledScriptLoader.load("com.aionemu.gameserver.world.zone.scripts"));
			log.info(I18n.get("log.fed3fa674a5b", handlers.size()));
		} catch (IllegalStateException e) {
			log.warn(I18n.get("log.673c69d49b31", e.getMessage()), e);
		} catch (Exception e) {
			throw new GameServerError("Can't initialize instance handlers.", e);
		} finally {
			if (progressLatch != null) {
				progressLatch.countDown();
			}
		}
	}

	/**
	 * 关闭服务并清空已注册处理器。
	 * Shut down the service and clear registered handlers.
	 */
	@Override
	public void shutdown() {
		log.info(I18n.get("log.28d38de81d63"));
		handlers.clear();
		log.info(I18n.get("log.0d5ebaf38acd"));
	}

	/**
	 * 按世界地图 ID 构建全部区域实例（含全图默认区与类型特化区）。
	 * Build all zone instances for a world map id (full-map default plus type-specific zones).
	 *
	 * map id
	 *
	 * @param mapId
	 * @return 区域名称 → 区域实例 / zone name → zone instance
	 */
	public Map<ZoneName, ZoneInstance> getZoneInstancesByWorldId(int mapId) {
		Map<ZoneName, ZoneInstance> zones = new HashMap<ZoneName, ZoneInstance>();
		int worldSize = DataManager.WORLD_MAPS_DATA.getTemplate(mapId).getWorldSize();
		WorldZoneTemplate zone = new WorldZoneTemplate(worldSize, mapId);
		PolyArea fullArea = new PolyArea(zone.getName(), mapId, zone.getPoints().getPoint(),
				zone.getPoints().getBottom(), zone.getPoints().getTop());
		ZoneInstance fullMap = new ZoneInstance(mapId, new ZoneInfo(fullArea, zone));
		fullMap.addHandler(getNewZoneHandler(zone.getName()));
		zones.put(zone.getName(), fullMap);

		Collection<ZoneInfo> areas = this.zoneByMapIdMap.get(mapId);
		if (areas == null) {
			return zones;
		}
		GameFeatureServices.shieldService().load(mapId);

		for (ZoneInfo area : areas) {
			ZoneInstance instance = null;
			switch (area.getZoneTemplate().getZoneType()) {
			case FLY:
				instance = new FlyZoneInstance(mapId, area);
				break;
			case FORT:
				instance = new SiegeZoneInstance(mapId, area);
				SiegeLocation siege = DataManager.SIEGE_LOCATION_DATA.getSiegeLocations()
						.get(area.getZoneTemplate().getSiegeId().get(0));
				if (siege != null) {
					siege.addZone((SiegeZoneInstance) instance);
					if (GeoDataConfig.GEO_SHIELDS_ENABLE) {
						GameFeatureServices.shieldService().attachShield(siege);
					}
				}
				break;
			case ARTIFACT:
				instance = new SiegeZoneInstance(mapId, area);
				for (int artifactId : area.getZoneTemplate().getSiegeId()) {
					SiegeLocation artifact = DataManager.SIEGE_LOCATION_DATA.getArtifacts().get(artifactId);
					if (artifact == null) {
						log.warn(I18n.get("log.58679a0b704d", area.getZoneTemplate().getName().name()));
					} else {
						artifact.addZone((SiegeZoneInstance) instance);
					}
				}
				break;
			case PVP:
				instance = new PvPZoneInstance(mapId, area);
				break;
			default:
				InvasionZoneInstance invasionZone = getIZI(area);
				if (invasionZone != null) {
					instance = invasionZone;
				} else {
					instance = new ZoneInstance(mapId, area);
				}
			}
			instance.addHandler(getNewZoneHandler(area.getZoneTemplate().getName()));
			zones.put(area.getZoneTemplate().getName(), instance);
		}
		return zones;
	}

	/**
	 * 按区域名称判断是否为入侵区并校验。
	 * Determine by zone name whether this is an invasion zone and validate it.
	 *
	 * @param area 区域信息 / zone info
	 * @return 入侵区域实例，或 null / invasion zone instance, or null
	 */
	private InvasionZoneInstance getIZI(ZoneInfo area) {
		if (area.getZoneTemplate().getName().name().equals("WAILING_CLIFFS_220050000")
				|| area.getZoneTemplate().getName().name().equals("BALTASAR_CEMETERY_220050000")
				|| area.getZoneTemplate().getName().name().equals("THE_LEGEND_SHRINE_220050000")
				|| area.getZoneTemplate().getName().name().equals("SUDORVILLE_220050000")
				|| area.getZoneTemplate().getName().name().equals("BALTASAR_HILL_VILLAGE_220050000")
				|| area.getZoneTemplate().getName().name().equals("BRUSTHONIN_MITHRIL_MINE_220050000")) {
			return validateZone(area);
		} else if (area.getZoneTemplate().getName().name().equals("JAMANOK_INN_210060000")
				|| area.getZoneTemplate().getName().name().equals("THE_STALKING_GROUNDS_210060000")
				|| area.getZoneTemplate().getName().name().equals("BLACK_ROCK_HOT_SPRING_210060000")
				|| area.getZoneTemplate().getName().name().equals("FREGIONS_FLAME_210060000")) {
			return validateZone(area);
		}
		return null;
	}

	/**
	 * 校验并创建与旋涡位置关联的入侵区域。
	 * Validate and create an invasion zone linked to a vortex location.
	 *
	 * @param area 区域信息 / zone info
	 * @return 入侵区域实例，或 null / invasion zone instance, or null
	 */
	private InvasionZoneInstance validateZone(ZoneInfo area) {
		int mapId = area.getZoneTemplate().getMapid();
		VortexLocation vortex = DataManager.VORTEX_DATA.getVortexLocation(mapId);
		if (vortex != null) {
			InvasionZoneInstance instance = new InvasionZoneInstance(mapId, area);
			vortex.addZone(instance);
			return instance;
		}
		return null;
	}

	/**
	 * 为 mesh_materials 中指定的网格创建材质区域模板。
	 * Create a material zone template for a mesh specified in mesh_materials.
	 *
	 * geometry
	 * world map id
	 * material id
	 * @param failOnMissing 缺失时是否严格失败 / whether to fail strictly when missing
	 */
	public void createMaterialZoneTemplate(Spatial geometry, int worldId, int materialId, boolean failOnMissing) {
		ZoneName zoneName = null;
		if (failOnMissing) {
			zoneName = ZoneName.get(geometry.getName() + "_" + worldId);
		} else {
			zoneName = ZoneName.createOrGet(geometry.getName() + "_" + worldId);
		}

		if (zoneName.name().equals(ZoneName.NONE)) {
			return;
		}

		ZoneHandler handler = collidableHandlers.get(zoneName);
		if (handler == null) {
			if (materialId == 11) {
				if (GeoDataConfig.GEO_SHIELDS_ENABLE) {
					handler = new SiegeShield(geometry);
					GameFeatureServices.shieldService().registerShield(worldId, (SiegeShield) handler);
				} else {
					return;
				}
			} else {
				MaterialTemplate template = DataManager.MATERIAL_DATA.getTemplate(materialId);
				if (template == null) {
					return;
				}
				handler = new MaterialZoneHandler(geometry, template);
			}
			collidableHandlers.put(zoneName, handler);
		}

		Collection<ZoneInfo> areas = this.zoneByMapIdMap.get(worldId);
		if (areas == null) {
			this.zoneByMapIdMap.put(worldId, new ArrayList<ZoneInfo>());
			areas = this.zoneByMapIdMap.get(worldId);
		}
		ZoneInfo zoneInfo = null;
		for (ZoneInfo area : areas) {
			if (area.getZoneTemplate().getName().equals(zoneName)) {
				zoneInfo = area;
				break;
			}
		}
		if (zoneInfo == null) {
			MaterialZoneTemplate zoneTemplate = new MaterialZoneTemplate(geometry, worldId);
			// 若需要搜索或许加入区域数据？ / maybe add to zone data if needed search ?
			Area zoneInfoArea = null;
			if (zoneTemplate.getSphere() != null) {
				zoneInfoArea = new SphereArea(zoneName, worldId, zoneTemplate.getSphere().getX(),
						zoneTemplate.getSphere().getY(), zoneTemplate.getSphere().getZ(),
						zoneTemplate.getSphere().getR());
			} else if (zoneTemplate.getCylinder() != null) {
				zoneInfoArea = new CylinderArea(zoneName, worldId, zoneTemplate.getCylinder().getX(),
						zoneTemplate.getCylinder().getY(), zoneTemplate.getCylinder().getR(),
						zoneTemplate.getCylinder().getBottom(), zoneTemplate.getCylinder().getTop());
			} else if (zoneTemplate.getSemisphere() != null) {
				zoneInfoArea = new SemisphereArea(zoneName, worldId, zoneTemplate.getSemisphere().getX(),
						zoneTemplate.getSemisphere().getY(), zoneTemplate.getSemisphere().getZ(),
						zoneTemplate.getSemisphere().getR());
			}
			if (zoneInfoArea != null) {
				zoneInfo = new ZoneInfo(zoneInfoArea, zoneTemplate);
				areas.add(zoneInfo);
			}
		}
	}

	/**
	 * 为动态几何体创建材质区域模板（可后续存 XML）；regionId 由 RegionUtil 根据包围体中心生成。
	 * Create a material zone template for dynamic geometry (may be saved to XML later);
	 * regionId is generated by RegionUtil from the bounding-volume center.
	 *
	 * geometry
	 * region id
	 * world map id
	 * material id
	 */
	public void createMaterialZoneTemplate(Spatial geometry, int regionId, int worldId, int materialId) {
		geometry.setName(geometry.getName() + "_" + regionId);
		createMaterialZoneTemplate(geometry, worldId, materialId, false);
	}

	/**
	 * 将可碰撞材质区域模板排序后写回 ZoneData。
	 * Sort collidable material zone templates and persist them via ZoneData.
	 */
	public void saveMaterialZones() {
		List<ZoneTemplate> templates = new ArrayList<ZoneTemplate>();
		for (WorldMapTemplate map : DataManager.WORLD_MAPS_DATA) {
			Collection<ZoneInfo> areas = this.zoneByMapIdMap.get(map.getMapId());
			if (areas == null) {
				continue;
			}
			for (ZoneInfo zone : areas) {
				if (collidableHandlers.containsKey(zone.getArea().getZoneName())) {
					templates.add(zone.getZoneTemplate());
				}
			}
		}
		Collections.sort(templates, new Comparator<ZoneTemplate>() {

			@Override
			public int compare(ZoneTemplate o1, ZoneTemplate o2) {
				return o1.getMapid() - o2.getMapid();
			}
		});

		ZoneData zoneData = new ZoneData();
		zoneData.zoneList = templates;
		zoneData.saveData();
	}
}
