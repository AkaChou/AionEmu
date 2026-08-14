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
 * XML template.
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

	/** 返回双生副本数 / Returns the twin count */
	public int getTwinCount() {
		if (WorldConfig.WORLD_MAX_TWINS_USUAL == 0) {
			return twinCount;
		}
		return Math.min(WorldConfig.WORLD_MAX_TWINS_USUAL, twinCount);
	}

	/** 返回新手双生副本数 / Returns the beginner twin count */
	public int getBeginnerTwinCount() {
		if (WorldConfig.WORLD_MAX_TWINS_BEGINNER == 0) {
			return beginnerTwinCount;
		} else if (WorldConfig.WORLD_MAX_TWINS_BEGINNER == -1) {
			return 0;
		}
		return Math.min(WorldConfig.WORLD_MAX_TWINS_BEGINNER, beginnerTwinCount);
	}

	/** 返回最大玩家数 / Returns the max user */
	public int getMaxUser() {
		return maxUser;
	}

	/**
	 * 是否监狱地图。
	 * Whether this is a prison map.
	 *
	 * @return 是否监狱 / whether prison
	 */
	public boolean isPrison() {
		return prison;
	}

	/** 是否副本。 / Whether instance. */
	public boolean isInstance() {
		return instance;
	}

	/** 返回水位 / Returns the water level. */
	public int getWaterLevel() {
		return waterlevel;
	}

	/** 返回死亡高度 / Returns the death level */
	public int getDeathLevel() {
		return deathlevel;
	}

	/** 获取世界类型。 / Returns the world type. */
	public WorldType getWorldType() {
		return worldType;
	}

	/** 返回世界大小 / Returns the world size. */
	public int getWorldSize() {
		return worldSize;
	}

	/** 获取世界掉落类型。 / Returns the world drop type. */
	public WorldDropType getWorldDropType() {
		return dropWorldType;
	}

	/** 是否飞行。 / Whether fly. */
	public boolean isFly() {
		return (flags & ZoneAttributes.FLY.getId()) != 0;
	}

	/**
	 * 是否允许滑翔。
	 * Whether gliding is allowed.
	 *
	 * @return 是否允许滑翔 / whether glide
	  */
	public boolean canGlide() {
		return (flags & ZoneAttributes.GLIDE.getId()) != 0;
	}

	/**
	 * 是否允许放置归还之石。
	 * Whether a kisk can be placed.
	 *
	 * @return 是否允许放置归还之石 / whether put kisk
	  */
	public boolean canPutKisk() {
		return (flags & ZoneAttributes.BIND.getId()) != 0;
	}

	/**
	 * 是否允许召回。
	 * Whether recall is allowed.
	 *
	 * @return 是否允许召回 / whether recall
	 */
	public boolean canRecall() {
		return (flags & ZoneAttributes.RECALL.getId()) != 0;
	}

	/**
	 * 是否允许骑乘。
	 * Whether riding is allowed.
	 *
	 * @return 是否允许骑乘 / whether ride
	  */
	public boolean canRide() {
		return (flags & ZoneAttributes.RIDE.getId()) != 0;
	}

	/**
	 * 是否允许飞行骑乘。
	 * Whether fly riding is allowed.
	 *
	 * @return 是否允许飞行骑乘 / whether fly ride
	 */
	public boolean canFlyRide() {
		return (flags & ZoneAttributes.FLY_RIDE.getId()) != 0;
	}

	/**
	 * 是否允许 PvP。
	 * Whether PvP is allowed.
	 *
	 * @return 是否允许 PvP / whether pvp allowed
	 */
	public boolean isPvpAllowed() {
		return (flags & ZoneAttributes.PVP_ENABLED.getId()) != 0;
	}

	/** 是否允许同种族决斗 / Whether same race duels allowed */
	public boolean isSameRaceDuelsAllowed() {
		return (flags & ZoneAttributes.DUEL_SAME_RACE_ENABLED.getId()) != 0;
	}

	/** 是否允许跨种族决斗 / Whether other race duels allowed */
	public boolean isOtherRaceDuelsAllowed() {
		return (flags & ZoneAttributes.DUEL_OTHER_RACE_ENABLED.getId()) != 0;
	}

	/** 返回区域标志 / Returns the flags */
	public int getFlags() {
		return flags;
	}

	protected void afterUnmarshal(Unmarshaller u, Object parent) {
		flags = ZoneAttributes.fromList(flagValues);
	}

	/** 是否排除增益 / Whether except buff */
	public boolean isExceptBuff() {
		return exceptBuff;
	}

	/** 返回 AI 信息 / Returns the ai info */
	public AiInfo getAiInfo() {
		return aiInfo;
	}
}
