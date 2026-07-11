package com.aionemu.gameserver.model.templates.world;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.configs.main.WorldConfig;
import com.aionemu.gameserver.world.WorldDropType;
import com.aionemu.gameserver.world.WorldType;
import com.aionemu.gameserver.world.zone.ZoneAttributes;

/**
 * 世界地图模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlRootElement(name = "map")
@XmlAccessorType(XmlAccessType.NONE)
public class WorldMapTemplate {
	@XmlAttribute(name = "name")
	protected String name = "";

	@XmlAttribute(name = "id", required = true)
	protected Integer mapId;

	@XmlAttribute(name = "twin_count")
	protected int twinCount;

	@XmlAttribute(name = "beginner_twin_count")
	protected int beginnerTwinCount;

	@XmlAttribute(name = "max_user")
	protected int maxUser;

	@XmlAttribute(name = "prison")
	protected boolean prison = false;

	@XmlAttribute(name = "instance")
	protected boolean instance = false;

	@XmlAttribute(name = "death_level", required = true)
	protected int deathlevel = 0;

	@XmlAttribute(name = "water_level", required = true)
	protected int waterlevel = 16;

	@XmlAttribute(name = "world_type")
	protected WorldType worldType = WorldType.NONE;

	@XmlAttribute(name = "world_size")
	protected int worldSize;

	@XmlElement(name = "ai_info")
	protected AiInfo aiInfo = AiInfo.DEFAULT;

	@XmlAttribute(name = "except_buff")
	protected boolean exceptBuff = false;

	@XmlAttribute(name = "flags")
	protected List<ZoneAttributes> flagValues;

	@XmlAttribute(name = "drop_type")
	protected WorldDropType dropWorldType = WorldDropType.NONE;

	@XmlTransient
	protected Integer flags;

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return name;
	}

	/** 返回映射 ID / Returns the map id */
	public Integer getMapId() {
		return mapId;
	}

	/** 返回 twin count / Returns the twin count */
	public int getTwinCount() {
		if (WorldConfig.WORLD_MAX_TWINS_USUAL == 0) {
			return twinCount;
		}
		return Math.min(WorldConfig.WORLD_MAX_TWINS_USUAL, twinCount);
	}

	/** 返回 beginner twin count / Returns the beginner twin count */
	public int getBeginnerTwinCount() {
		if (WorldConfig.WORLD_MAX_TWINS_BEGINNER == 0) {
			return beginnerTwinCount;
		} else if (WorldConfig.WORLD_MAX_TWINS_BEGINNER == -1) {
			return 0;
		}
		return Math.min(WorldConfig.WORLD_MAX_TWINS_BEGINNER, beginnerTwinCount);
	}

	/** 返回 max user / Returns the max user */
	public int getMaxUser() {
		return maxUser;
	}

	/**
	 * @return Whether prison / Whether prison
	 */
	public boolean isPrison() {
		return prison;
	}

	/** 是否副本。 / Whether Instance. */
	public boolean isInstance() {
		return instance;
	}

	/** 返回水等级 / Returns the water level*/
	public int getWaterLevel() {
		return waterlevel;
	}

	/** 返回 death level / Returns the death level */
	public int getDeathLevel() {
		return deathlevel;
	}

	/** 获取世界类型。 / Returns the world type. */
	public WorldType getWorldType() {
		return worldType;
	}

	/** 返回世界大小 / Returns the world size*/
	public int getWorldSize() {
		return worldSize;
	}

	/** 获取世界掉落类型。 / Returns the world drop type. */
	public WorldDropType getWorldDropType() {
		return dropWorldType;
	}

	/** 是否飞行。 / Whether Fly. */
	public boolean isFly() {
		return (flags & ZoneAttributes.FLY.getId()) != 0;
	}

	/**
	 * @return 是否 glide / 是否 glide。 / Whether glide / Whether glide
	 */
	public boolean canGlide() {
		return (flags & ZoneAttributes.GLIDE.getId()) != 0;
	}

	/**
	 * @return 是否放入 kisk / 是否放入 kisk。 / Whether put kisk / Whether put kisk
	 */
	public boolean canPutKisk() {
		return (flags & ZoneAttributes.BIND.getId()) != 0;
	}

	/**
	 * @return Whether recall / Whether recall
	 */
	public boolean canRecall() {
		return (flags & ZoneAttributes.RECALL.getId()) != 0;
	}

	/**
	 * @return 是否 ride / 是否 ride。 / Whether ride / Whether ride
	 */
	public boolean canRide() {
		return (flags & ZoneAttributes.RIDE.getId()) != 0;
	}

	/**
	 * @return Whether fly ride / Whether fly ride
	 */
	public boolean canFlyRide() {
		return (flags & ZoneAttributes.FLY_RIDE.getId()) != 0;
	}

	/**
	 * @return Whether pvp allowed / Whether pvp allowed
	 */
	public boolean isPvpAllowed() {
		return (flags & ZoneAttributes.PVP_ENABLED.getId()) != 0;
	}

	/** 是否 same race duels allowed / Whether same race duels allowed */
	public boolean isSameRaceDuelsAllowed() {
		return (flags & ZoneAttributes.DUEL_SAME_RACE_ENABLED.getId()) != 0;
	}

	/** 是否 other race duels allowed / Whether other race duels allowed */
	public boolean isOtherRaceDuelsAllowed() {
		return (flags & ZoneAttributes.DUEL_OTHER_RACE_ENABLED.getId()) != 0;
	}

	/** 返回 flags / Returns the flags */
	public int getFlags() {
		return flags;
	}

	protected void afterUnmarshal(Unmarshaller u, Object parent) {
		flags = ZoneAttributes.fromList(flagValues);
	}

	/** 是否 except buff / Whether except buff */
	public boolean isExceptBuff() {
		return exceptBuff;
	}

	/** 返回 ai info / Returns the ai info */
	public AiInfo getAiInfo() {
		return aiInfo;
	}
}
