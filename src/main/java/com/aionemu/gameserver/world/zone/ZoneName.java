package com.aionemu.gameserver.world.zone;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 区域名称注册表：按字符串缓存唯一 {@link ZoneName} 实例。
 * Zone-name registry: caches unique {@link ZoneName} instances by string key.
 *
 * @author Rolandas
 */
@Slf4j
public final class ZoneName {


	/** 名称 → 实例缓存 / name → instance cache */
	private static final Map<String, ZoneName> zoneNames = new ConcurrentHashMap<String, ZoneName>();
	/** 空名称常量 / none name constant */
	public static final String NONE = "NONE";
	/** 欧比斯城堡区域名称常量 / abyss castle zone name constant */
	public static final String ABYSS_CASTLE = "_ABYSS_CASTLE_AREA_";

	static {
		zoneNames.put(NONE, new ZoneName(NONE));
		zoneNames.put(ABYSS_CASTLE, new ZoneName(ABYSS_CASTLE));
	}

	/** 区域名称字符串 / zone name string */
	private String _name;

	/**
	 * @param name 区域名称 / zone name
	 */
	private ZoneName(String name) {
		this._name = name;
	}

	/**
	 * 返回区域名称字符串。
	 * Return the zone name string.
	 *
	 * @return 区域名称字符串 / the name
	 */
	public String name() {
		return _name;
	}

	/**
	 * 返回名称哈希作为 ID。
	 * Return the name hash as the id.
	 *
	 * @return 名称哈希 / the name hash
	 */
	public int id() {
		return _name.hashCode();
	}

	/**
	 * 按名称创建或获取已有 {@link ZoneName}（大写键）。
	 * Create or get an existing {@link ZoneName} by name (upper-case key).
	 *
	 * @param name 区域名称 / zone name
	 * @return 区域名称实例 / zone name instance
	 */
	public static final ZoneName createOrGet(String name) {
		return zoneNames.computeIfAbsent(name.toUpperCase(), ZoneName::new);
	}

	/**
	 * 按名称返回 ID；未知名称回退为 NONE。
	 * Return the id for the name; unknown names fall back to NONE.
	 *
	 * @param name 区域名称 / zone name
	 * @return 区域 ID / the zone id
	 */
	public static final int getId(String name) {
		ZoneName zoneName = zoneNames.get(name.toUpperCase());
		return (zoneName != null ? zoneName : zoneNames.get(NONE)).id();
	}

	/**
	 * 按名称获取已注册实例；未知名称告警并返回 NONE。
	 * Get a registered instance by name; unknown names warn and return NONE.
	 *
	 * @param name 区域名称 / zone name
	 * @return 区域名称实例 / zone name instance
	 */
	public static final ZoneName get(String name) {
		name = name.toUpperCase();
		ZoneName zoneName = zoneNames.get(name);
		if (zoneName != null) {
			return zoneName;
		}
		log.warn(I18n.get("log.6e966df7a0b7", name));
		return zoneNames.get(NONE);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return _name;
	}
}
