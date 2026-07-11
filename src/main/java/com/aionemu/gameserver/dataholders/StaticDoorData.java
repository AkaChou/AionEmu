package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.staticdoor.StaticDoorWorld;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 静态门数据容器，按世界地图 ID 索引门模板。
 * Static-door data holder, indexing door templates by world map id.
 *
 * @author Wakizashi
 */
@XmlRootElement(name = "staticdoor_templates")
@XmlAccessorType(XmlAccessType.FIELD)
public class StaticDoorData {

	@XmlElement(name = "world")
	private List<StaticDoorWorld> staticDorWorlds;

	/** 门模板映射。 / Map of all door templates. */
	private IntObjectHashMap<StaticDoorWorld> staticDoorData = new IntObjectHashMap<StaticDoorWorld>();

	/**
	 * JAXB 反序列化完成后，按世界 ID 重建门模板索引。
	 * After JAXB unmarshalling, rebuilds the door-template index by world id.
	 *
	 * @param u JAXB 反序列化器 / JAXB unmarshaller
	 * parent object
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		staticDoorData.clear();

		for (StaticDoorWorld world : staticDorWorlds) {
			staticDoorData.put(world.getWorld(), world);
		}
	}

	/**
	 * 返回已加载的静态门世界数量。
	 * Returns the number of loaded static-door worlds.
	 *
	 * world count
	 */
	public int size() {
		return staticDoorData.size();
	}

	/**
	 * 按世界 ID 获取该地图的静态门数据。
	 * Returns the static-door data for the given world id.
	 *
	 * @param world 世界地图 ID / world map id
	 * @return 静态门世界模板，不存在则为 null / static-door world template or null
	 */
	public StaticDoorWorld getStaticDoorWorlds(int world) {
		return staticDoorData.get(world);
	}

	/**
	 * 返回 JAXB 加载的静态门世界列表。
	 * Returns the JAXB-loaded list of static-door worlds.
	 *
	 * @return 静态门世界列表 / static-door world list
	 */
	public List<StaticDoorWorld> getStaticDorWorlds() {
		return staticDorWorlds;
	}

	/**
	 * 设置静态门世界列表并重建索引。
	 * Sets the static-door world list and rebuilds the index.
	 *
	 * @param staticDorWorlds 静态门世界列表 / static-door world list
	 */
	public void setStaticDorWorlds(List<StaticDoorWorld> staticDorWorlds) {
		this.staticDorWorlds = staticDorWorlds;
		afterUnmarshal(null, null);
	}
}
