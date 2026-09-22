package com.aionemu.gameserver.model.templates.npc;

import java.util.Locale;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.ai2.AiNames;
import com.aionemu.gameserver.configs.main.AIConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.NpcType;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.TribeClass;
import com.aionemu.gameserver.model.drop.NpcDrop;
import com.aionemu.gameserver.model.items.NpcEquippedGear;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.templates.BoundRadius;
import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;
import com.aionemu.gameserver.model.templates.stats.KiskStatsTemplate;
import com.aionemu.gameserver.model.templates.stats.NpcStatsTemplate;
import lombok.Getter;
import lombok.Setter;

/**
 * NPC 模板（静态数据/XML）。
 * XML template.
 */

@Getter
@Setter
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "npc_template")
public class NpcTemplate extends VisibleObjectTemplate {
	private static final int STUN_LIKE_ABNORMALS = 0x9D040;
	private static final int ALL_OTHER_ABNORMALS = 0x1FFFFFF & ~STUN_LIKE_ABNORMALS;

	private int npcId;
	// JAXB 的 String 写入属性不能配对 int getter，否则免疫字符串不会被解析。
	// Keep the JAXB String write-only property from pairing with an incompatible int getter.
	@Getter(lombok.AccessLevel.NONE)
	private int abnormalImmunity;
	/** 获取等级。 / Returns the level. */
	@XmlAttribute(name = "level", required = true)
	private byte level;
	@XmlAttribute(name = "name_id", required = true)
	private int nameId;
	/** 返回标题 ID / Returns the title id */
	@XmlAttribute(name = "title_id")
	private int titleId;
	@XmlAttribute(name = "name")
	private String name;
	/** 返回 height / Returns the height */
	@XmlAttribute(name = "height")
	private float height = 1;
	/** 返回 npc type / Returns the npc type */
	@XmlAttribute(name = "npc_type", required = true)
	private NpcType npcType;
	/** 获取属性模板。 / Returns the stats template. */
	@XmlElement(name = "stats")
	private NpcStatsTemplate statsTemplate;
	/** 获取装备。 / Returns the equipment. */
	@XmlElement(name = "equipment")
	private NpcEquippedGear equipment;
	/** 获取归还之石属性模板。 / Returns the kisk stats template. */
	@XmlElement(name = "kisk_stats")
	private KiskStatsTemplate kiskStatsTemplate;
	@SuppressWarnings("unused")
	@XmlElement(name = "ammo_speed")
	private int ammoSpeed = 0;
	@XmlAttribute(name = "rank")
	private NpcRank rank;
	@XmlAttribute(name = "rating")
	private NpcRating rating;
	@XmlAttribute(name = "sensory_range")
	private int aggrorange;
	/** 返回攻击范围 / Returns the attack range*/
	@XmlAttribute(name = "attack_range")
	private int attackRange;
	/** 返回 attack rate / Returns the attack rate */
	@XmlAttribute(name = "attack_rate")
	private int attackRate;
	/** 返回攻击延迟 / Returns the attack delay*/
	@XmlAttribute(name = "attack_delay")
	private int attackDelay;
	/** 返回 hp gauge level / Returns the hp gauge level */
	@XmlAttribute(name = "hpgauge_level")
	private int hpGaugeLevel;
	/** 获取部落。 / Returns the tribe. */
	@XmlAttribute(name = "tribe")
	private TribeClass tribe;
	@XmlAttribute(name = "ai")
	private String ai = AiNames.DUMMY_NPC.getName();
	/** 获取种族。 / Returns the race. */
	@XmlAttribute
	private Race race = Race.NONE;
	@XmlAttribute
	private int state;
	@XmlAttribute
	private boolean floatcorpse;
	@XmlAttribute(name = "on_mist")
	private Boolean onMist;
	@XmlElement(name = "bound_radius")
	private BoundRadius boundRadius;
	@XmlAttribute(name = "type")
	private NpcTemplateType npcTemplateType;
	@XmlAttribute(name = "abyss_type")
	private AbyssNpcType abyssNpcType;
	@XmlElement(name = "talk_info")
	private TalkInfo talkInfo;
	/** 返回 namedesc / Returns the namedesc */
	@XmlAttribute(name = "name_desc")
	private String namedesc;
	/** 设置 npc drop / Sets the npc drop */
	@XmlTransient
	private NpcDrop npcDrop;
	// 大量拾取 4.7 / Massive Looting 4.7
	/** 返回 massive looting / Returns the massive looting */
	@XmlElement(name = "massive_looting")
	private MassiveLooting massiveLooting;

	/** 返回模板 ID / Returns the template id */
	@Override
	public int getTemplateId() {
		return npcId;
	}

	/** 返回名称 ID / Returns the name id */
	@Override
	public int getNameId() {
		return nameId;
	}

	/** 获取名称。 / Returns the name. */
	@Override
	public String getName() {
		return name;
	}

	/** 返回 ai / Returns the ai */
	public String getAi() {
		if (AIConfig.ENABLE_FEARFUL_BEAST_AI && npcType == NpcType.ATTACKABLE && level <= 2 && race == Race.BEAST
				&& statsTemplate != null && statsTemplate.getMaxHp() < 10 && !"aggressive".equals(ai)) {
			return "fearful_beast";
		}
		return (!"noaction".equals(ai) && level > 1 && getAbyssNpcType().equals(AbyssNpcType.TELEPORTER))
				? "siege_teleporter"
				: ai;
	}

	/** 返回字符串表示。 / Returns string representation. */
	@Override
	public String toString() {
		return "Npc Template id: " + npcId + " name: " + name;
	}

	@SuppressWarnings("unused")
	@XmlID
	@XmlAttribute(name = "npc_id", required = true)
	private void setXmlUid(String uid) {
		npcId = Integer.parseInt(uid);
	}

	@XmlAttribute(name = "abnormal_immunity")
	private void setAbnormalImmunity(String value) {
		for (String name : value.split(",")) {
			switch (name.trim().toLowerCase(Locale.ROOT)) {
				case "":
				case "0":
					abnormalImmunity |= 0;
					break;
				case "stat_arall":
					abnormalImmunity |= ALL_OTHER_ABNORMALS;
					break;
				case "stat_arstunlike":
					abnormalImmunity |= STUN_LIKE_ABNORMALS;
					break;
				case "stat_arphysicalab":
					abnormalImmunity |= 0x1620B7;
					break;
				case "stat_armentalab":
					abnormalImmunity |= 0x800F48;
					break;
				case "poison":
					abnormalImmunity |= 1 << 0;
					break;
				case "bleed":
					abnormalImmunity |= 1 << 1;
					break;
				case "paralyze":
					abnormalImmunity |= 1 << 2;
					break;
				case "sleep":
					abnormalImmunity |= 1 << 3;
					break;
				case "root":
					abnormalImmunity |= 1 << 4;
					break;
				case "blind":
					abnormalImmunity |= 1 << 5;
					break;
				case "charm":
					abnormalImmunity |= 1 << 6;
					break;
				case "disease":
					abnormalImmunity |= 1 << 7;
					break;
				case "silence":
					abnormalImmunity |= 1 << 8;
					break;
				case "fear":
					abnormalImmunity |= 1 << 9;
					break;
				case "curse":
					abnormalImmunity |= 1 << 10;
					break;
				case "confuse":
					abnormalImmunity |= 1 << 11;
					break;
				case "stun":
					abnormalImmunity |= 1 << 12;
					break;
				case "petrification":
				case "perification":
					abnormalImmunity |= 1 << 13;
					break;
				case "stumble":
					abnormalImmunity |= 1 << 14;
					break;
				case "stagger":
					abnormalImmunity |= 1 << 15;
					break;
				case "openaerial":
					abnormalImmunity |= 1 << 16;
					break;
				case "snare":
					abnormalImmunity |= 1 << 17;
					break;
				case "slow":
					abnormalImmunity |= 1 << 18;
					break;
				case "spin":
					abnormalImmunity |= 1 << 19;
					break;
				case "bind":
					abnormalImmunity |= 1 << 20;
					break;
				case "deform":
					abnormalImmunity |= 1 << 21;
					break;
				case "pulled":
					abnormalImmunity |= 1 << 22;
					break;
				case "nofly":
					abnormalImmunity |= 1 << 23;
					break;
				case "simpleroot":
					abnormalImmunity |= 1 << 24;
					break;
				default:
					throw new IllegalArgumentException("Unknown NPC abnormal immunity: " + name);
			}
		}
	}

	/**
	 * 判断是否免疫指定异常状态。
	 * Returns whether the NPC is immune to the given abnormal status.
	 *
	 * @param stat 异常状态属性 / abnormal status stat
	 * @return 是否免疫 / whether immune
	 */
	public boolean isImmuneTo(StatEnum stat) {
		int mask = switch (stat) {
            case POISON_RESISTANCE -> 1 << 0;
            case BLEED_RESISTANCE -> 1 << 1;
            case PARALYZE_RESISTANCE -> 1 << 2;
            case SLEEP_RESISTANCE -> 1 << 3;
            case ROOT_RESISTANCE -> 1 << 4 | 1 << 24;
            case BLIND_RESISTANCE -> 1 << 5;
            case CHARM_RESISTANCE -> 1 << 6;
            case DISEASE_RESISTANCE -> 1 << 7;
            case SILENCE_RESISTANCE -> 1 << 8;
            case FEAR_RESISTANCE -> 1 << 9;
            case CURSE_RESISTANCE -> 1 << 10;
            case CONFUSE_RESISTANCE -> 1 << 11;
            case STUN_RESISTANCE -> 1 << 12;
            case PERIFICATION_RESISTANCE -> 1 << 13;
            case STUMBLE_RESISTANCE -> 1 << 14;
            case STAGGER_RESISTANCE -> 1 << 15;
            case OPENAREIAL_RESISTANCE -> 1 << 16;
            case SNARE_RESISTANCE -> 1 << 17;
            case SLOW_RESISTANCE -> 1 << 18;
            case SPIN_RESISTANCE -> 1 << 19;
            case BIND_RESISTANCE -> 1 << 20;
            case DEFORM_RESISTANCE -> 1 << 21;
            case PULLED_RESISTANCE -> 1 << 22;
            default -> 0;
        };
        return (abnormalImmunity & mask) != 0;
	}

	/** 获取军阶。 / Returns the rank. */
	public final NpcRank getRank() {
		return rank;
	}

	/** 返回 rating / Returns the rating */
	public final NpcRating getRating() {
		return rating;
	}

	/** 返回 aggro range / Returns the aggro range */
	public int getAggroRange() {
		return aggrorange;
	}

	/** 返回 minimum shout range / Returns the minimum shout range */
	public int getMinimumShoutRange() {
		if (aggrorange < 10) {
			return 10;
		}
		return aggrorange;
	}

	/** 获取状态。 / Returns the state. */
	@Override
	public int getState() {
		return state;
	}

	/** 获取边界半径。 / Returns the bound radius. */
	@Override
	public BoundRadius getBoundRadius() {
		return boundRadius != null ? boundRadius : super.getBoundRadius();
	}

	/** 返回 npc template type / Returns the npc template type */
	public NpcTemplateType getNpcTemplateType() {
		return npcTemplateType != null ? npcTemplateType : NpcTemplateType.NONE;
	}

	/** 返回欧比斯 NPC 类型 / Returns the abyss npc type */
	public AbyssNpcType getAbyssNpcType() {
		return abyssNpcType != null ? abyssNpcType : AbyssNpcType.NONE;
	}

	/** 返回 talk distance / Returns the talk distance */
	public final int getTalkDistance() {
		if (talkInfo == null) {
			return 2;
		}
		return talkInfo.getDistance();
	}

	/** 返回 talk delay / Returns the talk delay */
	public int getTalkDelay() {
		if (talkInfo == null) {
			return 0;
		}
		return talkInfo.getDelay();
	}

	/** 返回 npc drop / Returns the npc drop */
	public NpcDrop getNpcDrop() {
		if (npcDrop != null) {
			return npcDrop;
		}
		return DataManager.NPC_DROP_DATA == null ? null : DataManager.NPC_DROP_DATA.getDrop(npcId);
	}

	/**
	 * 是否可交互（存在对话信息）。
	 * Whether the NPC can be interacted with.
	 *
	 * @return 是否可交互 / whether interact
	 */
	public boolean canInteract() {
		return talkInfo != null;
	}

	/**
	 * 是否为对话型 NPC。
	 * Whether this is a dialog NPC.
	 *
	 * @return 是否对话型 / whether dialog npc
	 */
	public boolean isDialogNpc() {
		if (talkInfo == null) {
			return false;
		}
		return talkInfo.isDialogNpc();
	}

	/**
	 * 尸体是否漂浮。
	 * Whether the corpse floats.
	 *
	 * @return 是否漂浮尸体 / whether float corpse
	 */
	public boolean isFloatCorpse() {
		return floatcorpse;
	}

	/** 返回 mist spawn condition / Returns the mist spawn condition */
	public Boolean getMistSpawnCondition() {
		return onMist;
	}
}
