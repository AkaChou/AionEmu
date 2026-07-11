package com.aionemu.gameserver.model.autogroup;

import java.util.List;

import com.aionemu.gameserver.dataholders.DataManager;

/**
 * 自动队伍类型枚举。
 * Auto Group Type enumeration.
 *
 * @author Rinzler (Encom)
 */

public enum AutoGroupType {
	// 战舰。 / DREDGION.
	/** Baranath Dredgion / Baranath Dredgion */
	BARANATH_DREDGION(1, 600000, 4) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoDredgionInstance();
		}
	},
	CHANTRA_DREDGION(2, 600000, 4) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoDredgionInstance();
		}
	},
	TERATH_DREDGION(3, 600000, 4) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoDredgionInstance();
		}
	},
	ASHUNATAL_DREDGION(121, 600000, 4) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoDredgionInstance();
		}
	},

	// 竞技场 PVP 46–60 / ARENA PVP 46-60
	ARENA_OF_CHAOS_46_60_1(21, 110000, 2, 1) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoPvPFFAInstance();
		}
	},
	ARENA_OF_CHAOS_46_60_2(22, 110000, 2, 2) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoPvPFFAInstance();
		}
	},
	ARENA_OF_CHAOS_46_60_3(23, 110000, 2, 3) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoPvPFFAInstance();
		}
	},
	ARENA_OF_DISCIPLINE_46_60_1(24, 110000, 2, 1) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoPvPFFAInstance();
		}
	},
	ARENA_OF_DISCIPLINE_46_60_2(25, 110000, 2, 2) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoPvPFFAInstance();
		}
	},
	ARENA_OF_DISCIPLINE_46_60_3(26, 110000, 2, 3) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoPvPFFAInstance();
		}
	},
	CHAOS_TRAINING_GROUNDS_46_60_1(27, 110000, 2, 1) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoPvPFFAInstance();
		}
	},
	CHAOS_TRAINING_GROUNDS_46_60_2(28, 110000, 2, 2) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoPvPFFAInstance();
		}
	},
	CHAOS_TRAINING_GROUNDS_46_60_3(29, 110000, 2, 3) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoPvPFFAInstance();
		}
	},
	DISCIPLINE_TRAINING_GROUNDS_46_60_1(30, 110000, 2, 1) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoPvPFFAInstance();
		}
	},
	DISCIPLINE_TRAINING_GROUNDS_46_60_2(31, 110000, 2, 2) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoPvPFFAInstance();
		}
	},
	DISCIPLINE_TRAINING_GROUNDS_46_60_3(32, 110000, 2, 3) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoPvPFFAInstance();
		}
	},
	ARENA_OF_HARMONY_46_60_1(33, 110000, 4, 1) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoHarmonyInstance();
		}
	},
	ARENA_OF_HARMONY_46_60_2(34, 110000, 4, 2) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoHarmonyInstance();
		}
	},
	ARENA_OF_HARMONY_46_60_3(35, 110000, 4, 3) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoHarmonyInstance();
		}
	},
	ARENA_OF_GLORY_46_60_1(38, 110000, 4) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoPvPFFAInstance();
		}
	},
	HARMONY_TRAINING_GROUNDS_46_60_1(101, 110000, 4, 1) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoHarmonyInstance();
		}
	},
	HARMONY_TRAINING_GROUNDS_46_60_2(102, 110000, 4, 2) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoHarmonyInstance();
		}
	},
	HARMONY_TRAINING_GROUNDS_46_60_3(103, 110000, 4, 3) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoHarmonyInstance();
		}
	},
	UNITY_TRAINING_GROUNDS_46_60_1(104, 110000, 4, 1) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoHarmonyInstance();
		}
	},
	UNITY_TRAINING_GROUNDS_46_60_2(105, 110000, 4, 2) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoHarmonyInstance();
		}
	},
	UNITY_TRAINING_GROUNDS_46_60_3(106, 110000, 4, 3) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoHarmonyInstance();
		}
	},

	// 竞技场 PVP 61–65 / ARENA PVP 61-65
	ARENA_OF_CHAOS_61_65_1(39, 110000, 2, 1) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoPvPFFAInstance();
		}
	},
	ARENA_OF_DISCIPLINE_61_65_1(40, 110000, 2, 1) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoPvPFFAInstance();
		}
	},
	ARENA_OF_HARMONY_61_65_1(41, 110000, 4, 1) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoHarmonyInstance();
		}
	},
	ARENA_OF_GLORY_61_65_1(42, 110000, 4) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoPvPFFAInstance();
		}
	},
	CHAOS_TRAINING_GROUNDS_61_65_1(43, 110000, 2, 1) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoPvPFFAInstance();
		}
	},
	DISCIPLINE_TRAINING_GROUNDS_61_65_1(44, 110000, 2, 1) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoPvPFFAInstance();
		}
	},
	HARMONY_TRAINING_GROUNDS_61_65_1(45, 110000, 4, 1) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoHarmonyInstance();
		}
	},
	UNITY_TRAINING_GROUNDS_61_65_1(46, 110000, 4, 1) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoHarmonyInstance();
		}
	},

	// 竞技场 PVP 66–83 / ARENA PVP 66-83
	ARENA_OF_CHAOS_66_83_1(113, 110000, 2, 1) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoPvPFFAInstance();
		}
	},
	ARENA_OF_DISCIPLINE_66_83_1(114, 110000, 2, 1) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoPvPFFAInstance();
		}
	},
	ARENA_OF_HARMONY_66_83_1(115, 110000, 4, 1) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoHarmonyInstance();
		}
	},
	ARENA_OF_GLORY_66_83_1(116, 110000, 4) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoPvPFFAInstance();
		}
	},
	CHAOS_TRAINING_GROUNDS_66_83_1(117, 110000, 2, 1) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoPvPFFAInstance();
		}
	},
	DISCIPLINE_TRAINING_GROUNDS_66_83_1(118, 110000, 2, 1) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoPvPFFAInstance();
		}
	},
	HARMONY_TRAINING_GROUNDS_66_83_1(119, 110000, 4, 1) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoHarmonyInstance();
		}
	},
	UNITY_TRAINING_GROUNDS_66_83_1(120, 110000, 4, 1) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoHarmonyInstance();
		}
	},

	// 战场。 / BATTLEFIELD.
	KAMAR_BATTLEFIELD(107, 600000, 2) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoKamarBattlefieldInstance();
		}
	},
	ENGULFED_OPHIDAN_BRIDGE(108, 600000, 2) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoEngulfedOphidanBridgeInstance();
		}
	},
	IRON_WALL_WARFRONT(109, 600000, 2) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoIronWallWarfrontInstance();
		}
	},
	IDGEL_DOME(111, 600000, 2) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoIdgelDomeInstance();
		}
	},
	OPHIDAN_WARPATH(122, 600000, 2) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoEngulfedOphidanBridgeInstance();
		}
	},
	IDGEL_DOME_LANDMARK(123, 600000, 2) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoIdgelDomeInstance();
		}
	},
	HALL_OF_TENACITY(125, 600000, 2) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoHallOfTenacityInstance();
		}
	},
	// 5.6
	IDTM_LOBBY_P_01(127, 600000, 2) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	IDTM_LOBBY_P_02(128, 600000, 2) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	IDTM_LOBBY_E_01(129, 600000, 2) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	// IDTM_LOBBY_P_01(130, 600000, 2) { @Override AutoInstance newAutoInstance() {
	// return new AutoGeneralInstance(); } },
	// 5.8
	IDRUN(131, 600000, 2) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},

	// 副本。 / INSTANCE.
	FIRE_TEMPLE(302, 300000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	NOCHSANA_TRAINING_CAMP(303, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	DARK_POETA(304, 1200000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	THEOBOMOS_LAB(305, 1200000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	ADMA_STRONGHOLD(306, 1200000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	DRAUPNIR_CAVE(307, 1200000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	STEEL_RAKE(308, 1200000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	UDAS_TEMPLE(309, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	LOWER_UDAS_TEMPLE(310, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	EMPYREAN_CRUCIBLE(311, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	RENTUS_BASE(313, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	OPHIDAN_BRIDGE(314, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	INDRATU_FORTRESS(315, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	DANUAR_RELIQUARY(316, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	SAURO_SUPPLY_BASE(317, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	AETHEROGENETICS_LAB(318, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	DRAGON_LORD_REFUGE(322, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	ALQUIMIA_RESEARCH_CENTER(323, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	INFINITY_SHARD(324, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	NIGHTMARE_CIRCUS(330, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	BESHMUNDIR_TEMPLE_NORMAL(331, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	BESHMUNDIR_TEMPLE_HARD(332, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	TIAMAT_STRONGHOLD(333, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	AZOTURAN_FORTRESS(334, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	ETERNAL_BASTION(335, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	SEIZED_DANUAR_SANCTUARY(336, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	RUKIBUKI_CIRCUS_TROUPE_CAMP(337, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	ILLUMINARY_OBELISK(338, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	SHUGO_IMPERIAL_TOMB(339, 600000, 3) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	LUCKY_OPHIDAN_BRIDGE(340, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	LUCKY_DANUAR_RELIQUARY(341, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	INFERNAL_ILLUMINARY_OBELISK(342, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	INFERNAL_DANUAR_RELIQUARY(345, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	DANUAR_SANCTUARY(346, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	DRAKENSPIRE_DEPTHS(347, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	THE_SHUGO_EMPEROR_VAULT(348, 600000, 3) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	OCCUPIED_RENTUS_BASE(349, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	ARCHIVES_OF_ETERNITY(350, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	CRADLE_OF_ETERNITY(351, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	TRIALS_OF_ETERNITY(352, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	EMPEROR_TRILLIRUNERK_SAFE(353, 600000, 3) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	ADMA_FALL(354, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	THEOBOMOS_TEST_CHAMBER(355, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	DRAKENSEER_LAIR(356, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	FALLEN_POETA(357, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	ESOTERRACE(358, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	BASTION_OF_SOULS(359, 600000, 12) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	PADMARASHKA_CAVE(360, 600000, 12) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	IDAB1_HEROES_L(419, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	IDAB1_HEROES_D(421, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},

	// 副本跨小队匹配 / INSTANCE INTER PARTY MATCH
	STEEL_RAKE_INTER_PARTY_MATCH(401, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	UDAS_TEMPLE_INTER_PARTY_MATCH(402, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	LOWER_UDAS_TEMPLE_INTER_PARTY_MATCH(403, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	INDRATU_FORTRESS_INTER_PARTY_MATCH(404, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	AZOTURAN_FORTRESS_INTER_PARTY_MATCH(405, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	AETHEROGENETICS_LAB_INTER_PARTY_MATCH(406, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	ALQUIMIA_RESEARCH_CENTER_INTER_PARTY_MATCH(407, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	ANGUISHED_DRAGON_LORD_REFUGE_INTER_PARTY_MATCH(408, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	FIRE_TEMPLE_OF_MEMORIES(409, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	RENTUS_BASE_INTER_PARTY_MATCH(410, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	DRAGON_LORD_REFUGE_INTER_PARTY_MATCH(411, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	DANUAR_SANCTUARY_INTER_PARTY_MATCH(412, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	TIAMAT_STRONGHOLD_INTER_PARTY_MATCH(413, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	OPHIDAN_BRIDGE_INTER_PARTY_MATCH(414, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	DRAKENSPIRE_DEPTHS_INTER_PARTY_MATCH(415, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},

	// 帕内斯特拉 4.7 / PANESTERRA 4.7
	BELUS(10001, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	ASPIDA(10005, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	ATANATOS(10007, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	},
	DISILLON(10010, 600000, 6) {
		@Override
		AutoInstance newAutoInstance() {
			return new AutoGeneralInstance();
		}
	};

	private int instanceMaskId;
	private int time;
	private byte playerSize;
	private byte difficultId;
	private AutoGroup template;

	private AutoGroupType(int instanceMaskId, int time, int playerSize, int difficultId) {
		this(instanceMaskId, time, playerSize);
		this.difficultId = (byte) difficultId;
	}

	private AutoGroupType(int instanceMaskId, int time, int playerSize) {
		this.instanceMaskId = instanceMaskId;
		this.time = time;
		this.playerSize = (byte) playerSize;
		template = DataManager.AUTO_GROUP.getTemplateByInstaceMaskId(this.instanceMaskId);
	}

	/** 返回副本映射 ID / Returns the instance map id */
	public int getInstanceMapId() {
		return template.getInstanceId();
	}

	/** 返回玩家大小 / Returns the player size*/
	public byte getPlayerSize() {
		return playerSize;
	}

	/** 返回副本掩码 ID / Returns the instance mask id */
	public int getInstanceMaskId() {
		return instanceMaskId;
	}

	/** 返回名称 ID / Returns the name id */
	public int getNameId() {
		return template.getNameId();
	}

	/** 返回标题 ID / Returns the title id */
	public int getTitleId() {
		return template.getTitleId();
	}

	/** 返回时间 / Returns the time*/
	public int getTime() {
		return time;
	}

	/** 获取最小等级。 / Returns the min level. */
	public int getMinLevel() {
		return template.getMinLvl();
	}

	/** 获取最大等级。 / Returns the max level. */
	public int getMaxLevel() {
		return template.getMaxLvl();
	}

	/** Whether 登记小队 / Whether register group */
	public boolean hasRegisterGroup() {
		return template.hasRegisterGroup();
	}

	/** Whether 登记 fast / Whether register fast */
	public boolean hasRegisterFast() {
		return template.hasRegisterFast();
	}

	/**
	 * @return Whether special purpose / Whether special purpose
	 */
	public boolean hasSpecialPurpose() {
		return template.hasSpecialPurpose();
	}

	/** Whether 登记 new / Whether register new */
	public boolean hasRegisterNew() {
		return template.hasRegisterNew();
	}

	/** 包含 NPC ID / contain Npc Id. */
	public boolean containNpcId(int npcId) {
		return template.getNpcIds().contains(npcId);
	}

	/** 返回 npc ids / Returns the npc ids */
	public List<Integer> getNpcIds() {
		return template.getNpcIds();
	}

	/** 是否为无畏舰。 / Whether dredgion. */
	public boolean isDredgion() {
		switch (this) {
		case BARANATH_DREDGION:
		case CHANTRA_DREDGION:
		case TERATH_DREDGION:
			return true;
		default:
			break;
		}
		return false;
	}

	/**
	 * @return Whether asyunatar / Whether asyunatar
	 */
	public boolean isAsyunatar() {
		switch (this) {
		case ASHUNATAL_DREDGION:
			return true;
		default:
			break;
		}
		return false;
	}

	/**
	 * @return 是否 kamar / 是否 kamar。 / Whether kamar / Whether kamar
	 */
	public boolean isKamar() {
		switch (this) {
		case KAMAR_BATTLEFIELD:
			return true;
		default:
			break;
		}
		return false;
	}

	/**
	 * @return Whether ophidan / Whether ophidan
	 */
	public boolean isOphidan() {
		switch (this) {
		case ENGULFED_OPHIDAN_BRIDGE:
			return true;
		default:
			break;
		}
		return false;
	}

	/**
	 * @return Whether suspicious ophidan / Whether suspicious ophidan
	 */
	public boolean isSuspiciousOphidan() {
		switch (this) {
		case OPHIDAN_WARPATH:
			return true;
		default:
			break;
		}
		return false;
	}

	/**
	 * @return Whether bastion / Whether bastion
	 */
	public boolean isBastion() {
		switch (this) {
		case IRON_WALL_WARFRONT:
			return true;
		default:
			break;
		}
		return false;
	}

	/**
	 * @return Whether idgel dome / Whether idgel dome
	 */
	public boolean isIdgelDome() {
		switch (this) {
		case IDGEL_DOME:
			return true;
		default:
			break;
		}
		return false;
	}

	/**
	 * @return Whether idgel dome landmark / Whether idgel dome landmark
	 */
	public boolean isIdgelDomeLandmark() {
		switch (this) {
		case IDGEL_DOME_LANDMARK:
			return true;
		default:
			break;
		}
		return false;
	}

	/**
	 * @return 是否 hall 的 tenacity / 是否 hall 的 tenacity。 / Whether hall of tenacity / Whether hall of tenacity
	 */
	public boolean isHallOfTenacity() {
		switch (this) {
		case HALL_OF_TENACITY:
			return true;
		default:
			break;
		}
		return false;
	}

	/**
	 * @return Whether grand arena training camp / Whether grand arena training camp
	 */
	public boolean isGrandArenaTrainingCamp() {
		switch (this) {
		case IDTM_LOBBY_P_01:
			return true;
		default:
			break;
		}
		return false;
	}

	/**
	 * @return 是否 idrun / 是否 idrun。 / Whether id run / Whether id run
	 */
	public boolean isIDRun() {
		switch (this) {
		case IDRUN:
			return true;
		default:
			break;
		}
		return false;
	}

	/** 按 mask id 返回 agt / Returns the agt by mask id */
	public static AutoGroupType getAGTByMaskId(int instanceMaskId) {
		for (AutoGroupType autoGroupsType : values()) {
			if (autoGroupsType.getInstanceMaskId() == instanceMaskId) {
				return autoGroupsType;
			}
		}
		return null;
	}

	/** 获取自动队伍。 / Returns the auto group. */
	public static AutoGroupType getAutoGroup(int level, int npcId) {
		for (AutoGroupType agt : values()) {
			if (agt.hasLevelPermit(level) && agt.containNpcId(npcId)) {
				return agt;
			}
		}
		return null;
	}

	/** 按 world 返回 auto group / Returns the auto group by world */
	public static AutoGroupType getAutoGroupByWorld(int level, int worldId) {
		for (AutoGroupType agt : values()) {
			if (agt.getInstanceMapId() == worldId && agt.hasLevelPermit(level)) {
				return agt;
			}
		}
		return null;
	}

	/** 获取自动队伍。 / Returns the auto group. */
	public static AutoGroupType getAutoGroup(int npcId) {
		for (AutoGroupType agt : values()) {
			if (agt.containNpcId(npcId)) {
				return agt;
			}
		}
		return null;
	}

	/** 是否 pv p solo arena / Whether pv p solo arena */
	public boolean isPvPSoloArena() {
		switch (this) {
		case ARENA_OF_DISCIPLINE_46_60_1:
		case ARENA_OF_DISCIPLINE_46_60_2:
		case ARENA_OF_DISCIPLINE_46_60_3:
		case ARENA_OF_DISCIPLINE_61_65_1:
		case ARENA_OF_DISCIPLINE_66_83_1:
			return true;
		default:
			break;
		}
		return false;
	}

	/** 是否 training pv p solo arena / Whether training pv p solo arena */
	public boolean isTrainingPvPSoloArena() {
		switch (this) {
		case DISCIPLINE_TRAINING_GROUNDS_46_60_1:
		case DISCIPLINE_TRAINING_GROUNDS_46_60_2:
		case DISCIPLINE_TRAINING_GROUNDS_46_60_3:
		case DISCIPLINE_TRAINING_GROUNDS_61_65_1:
		case DISCIPLINE_TRAINING_GROUNDS_66_83_1:
			return true;
		default:
			break;
		}
		return false;
	}

	/**
	 * @return Whether pv pffa arena / Whether pv pffa arena
	 */
	public boolean isPvPFFAArena() {
		switch (this) {
		case ARENA_OF_CHAOS_46_60_1:
		case ARENA_OF_CHAOS_46_60_2:
		case ARENA_OF_CHAOS_46_60_3:
		case ARENA_OF_CHAOS_61_65_1:
		case ARENA_OF_CHAOS_66_83_1:
			return true;
		default:
			break;
		}
		return false;
	}

	/**
	 * @return Whether training pv pffa arena / Whether training pv pffa arena
	 */
	public boolean isTrainingPvPFFAArena() {
		switch (this) {
		case CHAOS_TRAINING_GROUNDS_46_60_1:
		case CHAOS_TRAINING_GROUNDS_46_60_2:
		case CHAOS_TRAINING_GROUNDS_46_60_3:
		case CHAOS_TRAINING_GROUNDS_61_65_1:
		case CHAOS_TRAINING_GROUNDS_66_83_1:
			return true;
		default:
			break;
		}
		return false;
	}

	/**
	 * @return Whether training harmony arena / Whether training harmony arena
	 */
	public boolean isTrainingHarmonyArena() {
		switch (this) {
		case HARMONY_TRAINING_GROUNDS_46_60_1:
		case HARMONY_TRAINING_GROUNDS_46_60_2:
		case HARMONY_TRAINING_GROUNDS_46_60_3:
		case HARMONY_TRAINING_GROUNDS_61_65_1:
		case HARMONY_TRAINING_GROUNDS_66_83_1:
		case UNITY_TRAINING_GROUNDS_46_60_1:
		case UNITY_TRAINING_GROUNDS_46_60_2:
		case UNITY_TRAINING_GROUNDS_46_60_3:
		case UNITY_TRAINING_GROUNDS_61_65_1:
		case UNITY_TRAINING_GROUNDS_66_83_1:
			return true;
		default:
			break;
		}
		return false;
	}

	/**
	 * @return Whether harmony arena / Whether harmony arena
	 */
	public boolean isHarmonyArena() {
		switch (this) {
		case ARENA_OF_HARMONY_46_60_1:
		case ARENA_OF_HARMONY_46_60_2:
		case ARENA_OF_HARMONY_46_60_3:
		case ARENA_OF_HARMONY_61_65_1:
		case ARENA_OF_HARMONY_66_83_1:
			return true;
		default:
			break;
		}
		return false;
	}

	/**
	 * @return Whether glory arena / Whether glory arena
	 */
	public boolean isGloryArena() {
		switch (this) {
		case ARENA_OF_GLORY_46_60_1:
		case ARENA_OF_GLORY_61_65_1:
		case ARENA_OF_GLORY_66_83_1:
			return true;
		default:
			break;
		}
		return false;
	}

	/**
	 * @return Whether pvp arena / Whether pvp arena
	 */
	public boolean isPvpArena() {
		return isHarmonyArena() || isTrainingHarmonyArena() || isTrainingPvPFFAArena() || isPvPFFAArena()
				|| isTrainingPvPSoloArena() || isPvPSoloArena();
	}

	/**
	 * @param level Whether level permit / Whether level permit
	 */
	public boolean hasLevelPermit(int level) {
		return level >= getMinLevel() && level <= getMaxLevel();
	}

	/** 返回 difficult id / Returns the difficult id */
	public byte getDifficultId() {
		return difficultId;
	}

	/** 获取自动副本。 / Returns the auto instance. */
	public AutoInstance getAutoInstance() {
		return newAutoInstance();
	}

	abstract AutoInstance newAutoInstance();
}
