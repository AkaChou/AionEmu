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

/**
 * NPC 模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "npc_template")
public class NpcTemplate extends VisibleObjectTemplate {
	private static final int STUN_LIKE_ABNORMALS = 0x9D040;
	private static final int ALL_OTHER_ABNORMALS = 0x1FFFFFF & ~STUN_LIKE_ABNORMALS;

	private int npcId;
	private int abnormalImmunity;
	@XmlAttribute(name = "level", required = true)
	private byte level;
	@XmlAttribute(name = "name_id", required = true)
	private int nameId;
	@XmlAttribute(name = "title_id")
	private int titleId;
	@XmlAttribute(name = "name")
	private String name;
	@XmlAttribute(name = "height")
	private float height = 1;
	@XmlAttribute(name = "npc_type", required = true)
	private NpcType npcType;
	@XmlElement(name = "stats")
	private NpcStatsTemplate statsTemplate;
	@XmlElement(name = "equipment")
	private NpcEquippedGear equipment;
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
	@XmlAttribute(name = "attack_range")
	private int attackRange;
	@XmlAttribute(name = "attack_rate")
	private int attackRate;
	@XmlAttribute(name = "attack_delay")
	private int attackDelay;
	@XmlAttribute(name = "hpgauge_level")
	private int hpGaugeLevel;
	@XmlAttribute(name = "tribe")
	private TribeClass tribe;
	@XmlAttribute(name = "ai")
	private String ai = AiNames.DUMMY_NPC.getName();
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
	@XmlAttribute(name = "name_desc")
	private String namedesc;
	@XmlTransient
	private NpcDrop npcDrop;
	// 大量拾取 4.7 / Massive Looting 4.7
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

	/** 返回标题 ID / Returns the title id */
	public int getTitleId() {
		return titleId;
	}

	/** 获取名称。 / Returns the name. */
	@Override
	public String getName() {
		return name;
	}

	/** 返回 height / Returns the height */
	public float getHeight() {
		return height;
	}

	/** 返回 npc type / Returns the npc type */
	public NpcType getNpcType() {
		return npcType;
	}

	/** SetsNPC 类型 / Sets the npc type */
	public void setNpcType(NpcType newType) {
		npcType = newType;
	}

	/** 获取装备。 / Returns the equipment. */
	public NpcEquippedGear getEquipment() {
		return equipment;
	}

	/** 获取等级。 / Returns the level. */
	public byte getLevel() {
		return level;
	}

	/** 获取属性模板。 / Returns the stats template. */
	public NpcStatsTemplate getStatsTemplate() {
		return statsTemplate;
	}

	/** 设置属性模板。 / Sets the stats template. */
	public void setStatsTemplate(NpcStatsTemplate statsTemplate) {
		this.statsTemplate = statsTemplate;
	}

	/** 获取归还之石属性模板。 / Returns the kisk stats template. */
	public KiskStatsTemplate getKiskStatsTemplate() {
		return kiskStatsTemplate;
	}

	/** 获取部落。 / Returns the tribe. */
	public TribeClass getTribe() {
		return tribe;
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
			abnormalImmunity |= switch (name.trim().toLowerCase(Locale.ROOT)) {
				case "", "0" -> 0;
				case "stat_arall" -> ALL_OTHER_ABNORMALS;
				case "stat_arstunlike" -> STUN_LIKE_ABNORMALS;
				case "stat_arphysicalab" -> 0x1620B7;
				case "stat_armentalab" -> 0x800F48;
				case "poison" -> 1 << 0;
				case "bleed" -> 1 << 1;
				case "paralyze" -> 1 << 2;
				case "sleep" -> 1 << 3;
				case "root" -> 1 << 4;
				case "blind" -> 1 << 5;
				case "charm" -> 1 << 6;
				case "disease" -> 1 << 7;
				case "silence" -> 1 << 8;
				case "fear" -> 1 << 9;
				case "curse" -> 1 << 10;
				case "confuse" -> 1 << 11;
				case "stun" -> 1 << 12;
				case "petrification", "perification" -> 1 << 13;
				case "stumble" -> 1 << 14;
				case "stagger" -> 1 << 15;
				case "openaerial" -> 1 << 16;
				case "snare" -> 1 << 17;
				case "slow" -> 1 << 18;
				case "spin" -> 1 << 19;
				case "bind" -> 1 << 20;
				case "deform" -> 1 << 21;
				case "pulled" -> 1 << 22;
				case "nofly" -> 1 << 23;
				case "simpleroot" -> 1 << 24;
				default -> throw new IllegalArgumentException("Unknown NPC abnormal immunity: " + name);
			};
		}
	}

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

	/** 返回攻击范围 / Returns the attack range*/
	public int getAttackRange() {
		return attackRange;
	}

	/** 返回 attack rate / Returns the attack rate */
	public int getAttackRate() {
		return attackRate;
	}

	/** 返回攻击延迟 / Returns the attack delay*/
	public int getAttackDelay() {
		return attackDelay;
	}

	/** 返回 hp gauge level / Returns the hp gauge level */
	public int getHpGaugeLevel() {
		return hpGaugeLevel;
	}

	/** 获取种族。 / Returns the race. */
	public Race getRace() {
		return race;
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

	/** 设置 npc drop / Sets the npc drop */
	public void setNpcDrop(NpcDrop npcDrop) {
		this.npcDrop = npcDrop;
	}

	/**
	 * @return Whether interact
	 */
	public boolean canInteract() {
		return talkInfo != null;
	}

	/**
	 * @return Whether dialog npc
	 */
	public boolean isDialogNpc() {
		if (talkInfo == null) {
			return false;
		}
		return talkInfo.isDialogNpc();
	}

	/**
	 * @return Whether float corpse
	 */
	public boolean isFloatCorpse() {
		return floatcorpse;
	}

	/** 返回 mist spawn condition / Returns the mist spawn condition */
	public Boolean getMistSpawnCondition() {
		return onMist;
	}

	/** 返回 namedesc / Returns the namedesc */
	public String getNamedesc() {
		return namedesc;
	}

	/** 返回 massive looting / Returns the massive looting */
	public MassiveLooting getMassiveLooting() {
		return massiveLooting;
	}
}
