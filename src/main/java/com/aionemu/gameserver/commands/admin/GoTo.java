package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.WorldMap;
import com.aionemu.gameserver.world.WorldMapType;
import com.aionemu.gameserver.model.TeleportAnimation;

/**
 * 管理员传送命令：按地点名传送至预设坐标（含副本实例处理）。
 * Admin teleport command: go to a named location with preset coordinates (handles instance maps).
 */
public class GoTo extends AdminCommand
{
	public GoTo() {
		super("goto");
	}

	/**
	 * 按地点名称将管理员传送到预设坐标。
	 * Teleport the admin to a preset destination by location name.
	 * @param player 执行命令的管理员 / Admin executing the command
	 * @param params 地点名称（可多词） / Location name (may be multi-word)
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params == null || params.length < 1) {
			PacketSendUtility.sendMessage(player, "syntax //goto <location>");
			return;
		}
		StringBuilder sbDestination = new StringBuilder();
		for(String p: params) {
			sbDestination.append(p).append(" ");
		}
		String destination = sbDestination.toString().trim();

		for (Destination dest : DESTINATIONS) {
			if (dest.matches(destination)) {
				goTo(player, dest.worldId(), dest.x(), dest.y(), dest.z());
				return;
			}
		}
		PacketSendUtility.sendMessage(player, "Could not find the specified destination !");
	}

	/**
	 * 目的地条目：主名称、可选别名、地图 ID 与坐标。
	 * A destination entry: primary name, optional alias, map id and coordinates.
	 */
	private record Destination(String name, String alias, int worldId, float x, float y, float z) {

		/**
		 * 判断输入是否命中该目的地的任一名称（忽略大小写）。
		 * Checks whether the input matches either name of this destination (case-insensitive).
		 */
		boolean matches(String input) {
			return input.equalsIgnoreCase(name) || input.equalsIgnoreCase(alias);
		}

		/**
		 * 以主名称充当别名的便捷条目（无独立别名时使用）。
		 * Convenience entry that reuses the primary name as the alias (for destinations without a distinct alias).
		 */
		Destination(String name, int worldId, float x, float y, float z) {
			this(name, name, worldId, x, y, z);
		}
	}

	/**
	 * 目的地表：数组顺序即匹配优先级，重名键先命中先得（与原 else-if 链语义一致）。
	 * The destination table: array order defines match priority and duplicate keys resolve to the first hit (same semantics as the original else-if chain).
	 */
	private static final Destination[] DESTINATIONS = {
		new Destination("Sanctum", WorldMapType.SANCTUM.getId(), 1322, 1511, 568),
		new Destination("Kaisinel1", WorldMapType.KAISINEL.getId(), 2155, 1567, 1205),
		new Destination("Poeta", WorldMapType.POETA.getId(), 829, 1231, 118),
		new Destination("Melponeh", WorldMapType.POETA.getId(), 426, 1740, 119),
		new Destination("Verteron", WorldMapType.VERTERON.getId(), 1643, 1500, 119),
		new Destination("Cantas", WorldMapType.VERTERON.getId(), 2384, 788, 102),
		new Destination("Ardus", WorldMapType.VERTERON.getId(), 2333, 1817, 193),
		new Destination("Pilgrims", WorldMapType.VERTERON.getId(), 2063, 2412, 274),
		new Destination("Tolbas", WorldMapType.VERTERON.getId(), 1291, 2206, 142),
		new Destination("Eltnen", WorldMapType.ELTNEN.getId(), 343, 2724, 264),
		new Destination("Golden", WorldMapType.ELTNEN.getId(), 688, 431, 332),
		new Destination("Eltnen Observatory", WorldMapType.ELTNEN.getId(), 1779, 883, 422),
		new Destination("Novan", WorldMapType.ELTNEN.getId(), 947, 2215, 252),
		new Destination("Agairon", WorldMapType.ELTNEN.getId(), 1921, 2045, 361),
		new Destination("Kuriullu", WorldMapType.ELTNEN.getId(), 2411, 2724, 361),
		new Destination("Theobomos", WorldMapType.THEOBOMOS.getId(), 1398, 1557, 31),
		new Destination("Jamanok", WorldMapType.THEOBOMOS.getId(), 458, 1257, 127),
		new Destination("Meniherk", WorldMapType.THEOBOMOS.getId(), 1396, 1560, 31),
		new Destination("obsvillage", WorldMapType.THEOBOMOS.getId(), 2234, 2284, 50),
		new Destination("Josnack", WorldMapType.THEOBOMOS.getId(), 901, 2774, 62),
		new Destination("Anangke", WorldMapType.THEOBOMOS.getId(), 2681, 847, 138),
		new Destination("Heiron", WorldMapType.HEIRON.getId(), 2540, 343, 411),
		new Destination("Heiron Observatory", WorldMapType.HEIRON.getId(), 1423, 1334, 175),
		new Destination("Senemonea", WorldMapType.HEIRON.getId(), 971, 686, 135),
		new Destination("Jeiaparan", WorldMapType.HEIRON.getId(), 1635, 2693, 115),
		new Destination("Changarnerk", WorldMapType.HEIRON.getId(), 916, 2256, 157),
		new Destination("Kishar", WorldMapType.HEIRON.getId(), 1999, 1391, 118),
		new Destination("Arbolu", WorldMapType.HEIRON.getId(), 170, 1662, 120),
		new Destination("Panium", "Pandaemonium", WorldMapType.PANDAEMONIUM.getId(), 1679, 1400, 195),
		new Destination("Marchutan1", WorldMapType.MARCHUTAN.getId(), 1557, 1429, 266),
		new Destination("Ishalgen", WorldMapType.ISHALGEN.getId(), 579, 2445, 279),
		new Destination("Anturoon", WorldMapType.ISHALGEN.getId(), 940, 1707, 259),
		new Destination("Altgard", WorldMapType.ALTGARD.getId(), 1748, 1807, 254),
		new Destination("Basfelt", WorldMapType.ALTGARD.getId(), 1903, 696, 260),
		new Destination("Trader", WorldMapType.ALTGARD.getId(), 2680, 1024, 311),
		new Destination("Impetusium", WorldMapType.ALTGARD.getId(), 2643, 1658, 324),
		new Destination("Altgard Observatory", WorldMapType.ALTGARD.getId(), 1468, 2560, 299),
		new Destination("Morheim", WorldMapType.MORHEIM.getId(), 308, 2274, 449),
		new Destination("Desert", WorldMapType.MORHEIM.getId(), 634, 900, 360),
		new Destination("Slag", WorldMapType.MORHEIM.getId(), 1772, 1662, 197),
		new Destination("Kellan", WorldMapType.MORHEIM.getId(), 1070, 2486, 239),
		new Destination("Alsig", WorldMapType.MORHEIM.getId(), 2387, 1742, 102),
		new Destination("Morheim Observatory", WorldMapType.MORHEIM.getId(), 2794, 1122, 171),
		new Destination("Halabana", WorldMapType.MORHEIM.getId(), 2346, 2219, 127),
		new Destination("Brusthonin", WorldMapType.BRUSTHONIN.getId(), 2917, 2421, 15),
		new Destination("Baltasar", WorldMapType.BRUSTHONIN.getId(), 1413, 2013, 51),
		new Destination("Bollu", WorldMapType.BRUSTHONIN.getId(), 840, 2016, 307),
		new Destination("Edge", WorldMapType.BRUSTHONIN.getId(), 1523, 374, 231),
		new Destination("Bubu", WorldMapType.BRUSTHONIN.getId(), 526, 848, 76),
		new Destination("Settlers", WorldMapType.BRUSTHONIN.getId(), 2917, 2417, 15),
		new Destination("Beluslan", WorldMapType.BELUSLAN.getId(), 398, 400, 222),
		new Destination("Besfer", WorldMapType.BELUSLAN.getId(), 533, 1866, 262),
		new Destination("Kidorun", WorldMapType.BELUSLAN.getId(), 1243, 819, 260),
		new Destination("Red Mane", WorldMapType.BELUSLAN.getId(), 2358, 1241, 470),
		new Destination("Kistenian", WorldMapType.BELUSLAN.getId(), 1942, 513, 412),
		new Destination("Hoarfrost", WorldMapType.BELUSLAN.getId(), 2431, 2063, 579),
		// 欧比斯与雷珊塔要塞 / Abyss & Reshanta Fortresses
		new Destination("Siels Eastern Fortress", "sieleast", 400010000, 1792, 2412, 1479),
		new Destination("Siels Western Fortress", "sielwest", 400010000, 2207, 2014, 1479),
		new Destination("Sulfur Fortress", "Sulfur", 400010000, 1043, 951, 1538),
		new Destination("Krotan Fortress", "krotan", 400010000, 1389, 994, 2960),
		new Destination("Kysis Fortress", "kysis", 400010000, 2052, 1731, 3006),
		new Destination("Miren Fortress", "miren", 400010000, 1222, 1784, 2943),
		new Destination("Asteria Fortress", "asteria", 400010000, 468, 2488, 2921),
		new Destination("Roah Fortress", "roah", 400010000, 2227, 456, 2894),
		new Destination("Divine Fortress", "divine", 400010000, 1580, 1759, 2316),
		new Destination("haramel", "Haramel", 300200000, 176, 21, 144),
		new Destination("nochsana", "Nochsana Training Camp", 300030000, 513, 668, 331),
		new Destination("arcanis", "Sky Temple Of Arcanis", 320050000, 177, 229, 536),
		new Destination("firetemple", "Fire Temple", 320100000, 148, 461, 141),
		new Destination("kromede", "Kromede Trial", 300230000, 248, 244, 189),
		new Destination("steelrake", "Steel Rake", 300100000, 237, 506, 948),
		new Destination("elementis", "Elementis Forest", 300260000, 176, 612, 231),
		new Destination("indratu", "Indratu Fortress", 310090000, 562, 335, 1015),
		new Destination("azoturan", "Azoturan Fortress", 310100000, 300, 338, 1018),
		new Destination("aethero", "Aetherogenetics Lab", 310050000, 360, 230, 147),
		new Destination("adma", "Adma Stronghold", 320130000, 450, 200, 168),
		new Destination("alquimia", "Alquimia Research Center", 320110000, 603, 527, 200),
		new Destination("draupnir", "Draupnir Cave", 320080000, 491, 373, 622),
		new Destination("theobomoslab", "Theobomos Research Lab", 310110000, 477, 201, 170),
		new Destination("dp", "Dark Poeta", 300040000, 1214, 412, 140),
		new Destination("sulfur", "Sulfur Tree Nest", 300060000, 462, 345, 163),
		new Destination("left", "Left Wing Chamber", 300080000, 672, 606, 321),
		new Destination("right", "Right Wing Chamber", 300090000, 263, 386, 103),
		new Destination("carpus", "Carpus Isle Storeroom", 300050000, 469, 568, 202), //4.9
		new Destination("twilight", "Twilight Battlefield Storeroom", 300130000, 527, 120, 176), //4.9
		new Destination("grave", "Grave Of Steel Storeroom", 300120000, 528, 121, 176), //4.9
		new Destination("isle", "Isle Of Roots Storeroom", 300140000, 528, 109, 176), //4.9
		new Destination("hamate", "Hamate Isle Storeroom", 300070000, 504, 396, 94), //4.9
		new Destination("baranath", "Baranath Dredgion", 300110000, 399, 169, 432),
		new Destination("chantra", "Chantra Dredgion", 300210000, 399, 169, 432),
		new Destination("taloc", "Taloc's Hollow", 300190000, 200, 214, 1099),
		new Destination("udas", "Udas Temple", 300150000, 637, 657, 134),
		new Destination("udas2", "Lower Udas Temple", 300160000, 1146, 277, 116),
		new Destination("besh", "Beshmundir Temple", 300170000, 1477, 237, 243),
		new Destination("padma", "Padmarashka Cave", 320150000, 385, 506, 66),
		new Destination("karamatisa", "Karamatis A", 310010000, 221, 250, 206),
		new Destination("karamatisb", "Karamatis B", 310020000, 221, 250, 206),
		new Destination("karamatisc", "Karamatis C", 310120000, 221, 250, 206),
		new Destination("aerdina", "Aerdina", 310030000, 275, 168, 205),
		new Destination("geranaia", "Geranaia", 310040000, 275, 168, 205),
		new Destination("sliver", "Sliver Of Darkness", 310070000, 247, 249, 1392),
		new Destination("space", "Space Of Destiny", 320070000, 246, 246, 125),
		new Destination("ataxiar a", "Ataxiar A", 320010000, 221, 250, 206),
		new Destination("ataxiar b", "Ataxiar B", 320020000, 221, 250, 206),
		new Destination("bregirun", "Bregirun", 320030000, 275, 168, 205),
		new Destination("nidalber", "Nidalber", 320040000, 275, 168, 205),
		new Destination("sanctum arena", "Sanctum Undeground Arena", 310080000, 275, 242, 159),
		new Destination("triniel arena", "Triniel Undeground Arena", 320090000, 275, 239, 159),
		new Destination("prisone", "Prison Elyos", 510010000, 256, 256, 49),
		new Destination("prisona", "Prison Asmos", 520010000, 256, 256, 49),
		new Destination("IDAbPro", 300010000, 270, 200, 206),
		new Destination("gm", 120020000, 1442, 1133, 302),
		new Destination("kaisinel2", 110070000, 459, 251, 128),
		new Destination("marchutan2", 120080000, 577, 250, 94),
		new Destination("splinter", "Abyssal Splinter", 300220000, 704, 153, 453),
		new Destination("splinter2", "Unstable Abyssal Splinter", 300600000, 704, 153, 453),
		new Destination("esoterrace", "Esoterrace", 300250000, 333, 437, 326),
		new Destination("aturam", "Aturam Sky Fortress", 300240000, 684, 460, 655),
		new Destination("aturam2", "[Event] Aturam Sky Fortress", 300241000, 684, 460, 655),
		new Destination("rentus", "Rentus Base", 300280000, 564, 595, 153),
		new Destination("crucible", "Empyrean Crucible", 300300000, 354, 350, 96), //2.5
		new Destination("crucible2", "Crucible Challenge", 300320000, 357, 1662, 95), //2.6
		new Destination("chaos", "Arena Of Chaos", 300350000, 1332, 1078, 340), //2.7
		new Destination("discipline", "Arena Of Discipline", 300360000, 707, 1779, 165), //2.7
		new Destination("chaos2", "Chaos Training Grounds", 300420000, 1332, 1078, 340), //2.7
		new Destination("discipline2", "Discipline Training Grounds", 300430000, 707, 1779, 165), //2.7
		new Destination("terath", "Terath Dredgion", 300440000, 399, 169, 432),
		new Destination("harmony", "Arena Of Harmony", 300450000, 500, 371, 211), //3.9
		new Destination("cabin", "Steel Rake Cabin", 300460000, 248, 244, 189),
		new Destination("sealed", "Sealed Danuar Mysticarium", 300480000, 179, 122, 231), //4.8
		new Destination("stronghold", "Tiamat Stronghold", 300510000, 1581, 1068, 492), //3.9
		new Destination("refuge", "Dragon Lord Refuge", 300520000, 503, 524, 240), //3.9
		new Destination("bastion", "Eternal Bastion", 300540000, 743, 293, 233), //4.3
		new Destination("glory", "Arena Of Glory", 300550000, 500, 371, 211), //3.9
		new Destination("shugo", "Shugo Imperial Tomb", 300560000, 152, 199, 538), //3.9
		new Destination("harmonyTraining", "Harmony Training Grounds", 300570000, 500, 371, 211), //3.9
		// （需要客户端补丁） / (Need Client Patch)
		new Destination("katalamely", "Katalam Elyos", 600050000, 398, 2718, 142), //4.3
		new Destination("katalamasmo", "Katalam Asmo", 600050000, 361, 383, 281), //4.3
		new Destination("lakrumely", "LakrumEly", 600200000, 2689, 488, 324), //6.2
		new Destination("lakrumasmo", "LakrumAsmo", 600200000, 2926, 2506, 314), //6.2
		// ///////////////////地城/////////////////// / ///////////////////DUNGEONS///////////////////
		new Destination("ophidan", "Ophidan Bridge", 300590000, 769, 558, 572), //4.3
		new Destination("raksang", "Raksang Ruins", 300610000, 851, 947, 1206), //4.8
		new Destination("rentus2", "[Occupied] Rentus Base", 300620000, 573, 596, 153), //4.8
		new Destination("refuge2", "[Anguished] Dragon Lord's Refuge", 300630000, 503, 524, 240), //4.8
		new Destination("hexway", "The Hexway", 300700000, 672, 606, 321), //3.9
		new Destination("infinity", "Infinity Shard", 300800000, 118, 115, 131), //4.3
		new Destination("unity", "Unity Training Grounds", 301100000, 500, 371, 211), //3.7
		new Destination("reliquary", "Danuar Reliquary", 301110000, 256, 257, 241), //4.3
		new Destination("kamar", "Kamar Battlefield", 301120000, 1374, 1455, 600), //4.3
		new Destination("sauro", "Sauro Supply Base", 301130000, 641, 176, 195), //4.3
		new Destination("seized", "Seized Danuar Sanctuary", 301140000, 388, 1184, 55), //4.3
		new Destination("asteriasolo", "Asteria IU Solo", 301150000, 500, 500, 500), //4.3
		new Destination("circus", "Nightmare Circus", 301160000, 472, 564, 202), //4.3
		new Destination("circus2", "The Nightmare Circus", 301200000, 472, 564, 202), //4.3
		new Destination("engulfed", "Engulfed Ophidan Bridge", 301210000, 690, 469, 599), //4.5
		new Destination("iron", "Iron Wall Warfront", 301220000, 743, 293, 233), //4.5
		new Destination("obelisk", "Illuminary Obelisk", 301230000, 321, 323, 405), //4.5
		new Destination("foundry", "Linkgate Foundry", 301270000, 357, 259, 311), //4.7
		new Destination("dome", "Idgel Dome", 301310000, 261, 262, 85), //4.7
		new Destination("ophidan2", "Lucky Ophidan Bridge", 301320000, 769, 558, 572), //4.5
		new Destination("reliquary2", "Lucky Danuar Reliquary", 301330000, 256, 257, 241), //4.5
		new Destination("foundryquest", "Linkgate Foundry Quest", 301340000, 241, 332, 91), //4.7
		new Destination("reliquary3", "[Infernal] Danuar Reliquary", 301360000, 256, 257, 241), //4.7
		new Destination("obelisk2", "[Infernal] Illuminary Obelisk", 301370000, 321, 323, 405), //4.7
		new Destination("sanctuary2", "Danuar Sanctuary", 301380000, 388, 1184, 55), //4.8
		new Destination("drakenspire", "Drakenspire Depths", 301390000, 327, 183, 1687), //4.8
		new Destination("vault", "The Shugo Emperor's Vault", 301400000, 543, 294, 400), //4.7.5
		new Destination("reach", "Stonespear Reach", 301500000, 219, 268, 96), //4.8
		new Destination("manor", "Sealed Argent Manor", 301510000, 995, 1207, 65), //4.9.1
		new Destination("archives", "Archives Of Eternity", 301540000, 737, 511, 469), //5.0
		new Destination("cradle", "Cradle Of Eternity", 301550000, 1477, 774, 1035), //5.1
		new Destination("trials", "Trials Of Eternity", 301560000, 1235, 1026, 761), //5.5
		new Destination("archives2", "Archives Of Eternity", 301570000, 690, 432, 468), //5.0
		new Destination("vault2", "Emperor Trillirunerk's Safe", 301590000, 543, 294, 400), //4.9.1
		new Destination("adma2", "Adma's Fall", 301600000, 480, 462, 173), //5.0
		new Destination("lap", "Theobomos Test Chamber", 301610000, 219, 357, 202), //5.0
		new Destination("dragon", "Drakenseer Lair", 301620000, 275, 349, 336), //5.0
		new Destination("underpath", "Contaminated Underpath", 301630000, 230, 169, 164), //5.0.5
		new Destination("underpath2", "[Event] Contaminated Underpath", 301631000, 230, 169, 164), //5.6
		new Destination("underpath3", "IDEvent_Def_H", 301632000, 230, 169, 164), //5.8
		new Destination("factory", "Secret Munitions Factory", 301640000, 407, 292, 198), //5.0.5
		new Destination("dred4", "Ashunatal Dredgion", 301650000, 399, 169, 432), //5.1
		new Destination("fallen", "Fallen Poeta", 301660000, 286, 972, 107), //5.1
		new Destination("warpath", "Ophidan Warpath", 301670000, 690, 469, 599), //5.1
		new Destination("landmark", "Idgel Dome Landmark", 301680000, 261, 262, 85), //5.1
		new Destination("run", "IDRun", 301700000, 1371, 1380, 375), //???
		new Destination("mirash", "Mirash Sanctuary", 301720000, 770, 819, 518), //5.8
		new Destination("smoldering", "Smoldering Fire Temple", 302000000, 148, 461, 141), //5.1
		new Destination("oblivion", "Fissure Of Oblivion", 302100000, 916, 461, 352), //5.1
		new Destination("oblivion2", "[Opportunity] Fissure Of Oblivion", 302110000, 916, 461, 352), //5.6
		new Destination("sanctum2", "Dredgion Defense: Sanctum", 302200000, 1532, 1511, 565),
		new Destination("pandae2", "Dredgion Defense: Panium", 302300000, 1274, 1357, 204),
		new Destination("arena", "Arena Of Tenacity", 302310000, 255, 255, 72), //5.3
		new Destination("tenacity", "Hall Of Tenacity", 302320000, 278, 256, 236), //5.3
		new Destination("kumuki", "Kumuki Cave", 302330000, 176, 21, 144), //5.3
		new Destination("souls", "Bastion Of Souls", 302340000, 1318, 1360, 494), //5.5
		new Destination("canyonE", "Evergale Canyon", 302350000, 402, 751, 336), //5.5
		new Destination("canyonA", "Evergale Canyon", 302350000, 1094, 752, 336f), //5.5
		new Destination("arena1", "IDTM_ArenaE_01", 302360000, 0, 0, 0), //5.6
		new Destination("tenacity1", "IDTM_LobbyE_01", 302370000, 278, 256, 236), //5.6
		new Destination("arena2", "IDTM_ArenaP_01", 302380000, 268, 256, 68), //5.6
		new Destination("tenacity2", "IDTM_LobbyP_01", 302390000, 278, 256, 236), //5.6
		new Destination("spire", "Crucible Spire", 302400000, 223, 249, 241), //5.6
		new Destination("arena3", "IDTM_ArenaP_02", 302410000, 268, 256, 68), //5.6
		new Destination("tenacity3", "IDTM_LobbyP_02", 302420000, 278, 256, 236), //5.6
		new Destination("solo1", 210120000, 1852, 1716, 301), //5.3
		new Destination("solo2", 220130000, 1852, 1716, 301), //5.3
		new Destination("divine1", "Divine Tower E", 310160000, 240, 222, 378), //5.8
		new Destination("divine2", "Divine Tower A", 320160000, 240, 222, 378), //5.8
		//** 区域 / Zones *//
		new Destination("silentera", 600010000, 583, 767, 300), //5.8
		//** 4.3 / 4.3* *//
		new Destination("iu", 600080000, 1510, 1511, 565),
		//** 4.7 / 4.7* *//
		new Destination("kaldorely", 600090000, 1305, 1321, 199),
		new Destination("kaldorasmo", 600090000, 400, 1351, 163),
		new Destination("levinshorely", 600100000, 99, 101, 348),
		new Destination("levinshorasmo", 600100000, 1842, 1772, 305),
		//** 5.8 / 5.8* *//
		new Destination("inggison", 210050000, 1335, 276, 590), //5.8
		new Destination("gelkmaros", 220070000, 1763, 2911, 554), //5.8
		new Destination("eyeely", 600040000, 754, 30, 1196), //5.8
		new Destination("eyeasmo", 600040000, 754, 1506, 1196), //5.8
		//** 帕内斯特拉 / Panesterra* *//
		new Destination("belus", 400020000, 1238, 1232, 1518),
		new Destination("annex", "Transidium Annex", 400030000, 287, 290, 680), //4.7
		new Destination("aspida", 400040000, 1238, 1232, 1518),
		new Destination("atanatos", 400050000, 1238, 1232, 1518),
		new Destination("disillon", 400060000, 1238, 1232, 1518),
		new Destination("oriel", 700010000, 1261, 1845, 98),
		new Destination("pernon", 710010000, 1069, 1539, 98),
		new Destination("wisplight", 130090000, 247, 236, 129), //4.7.2
		new Destination("fatebound", 140010000, 272, 266, 96), //4.7.2
		//** 4.8 / 4.8* *//
		new Destination("cygnea", 210070000, 2917, 838, 569), //4.8
		new Destination("griffoen", 210080000, 263, 199, 498), //4.8
		new Destination("enshar", 220080000, 470, 2341, 216), //4.8
		new Destination("habrok", 220090000, 252, 170, 504), //4.8
		new Destination("idianely", 210090000, 701, 693, 514), //4.8
		new Destination("idianasmo", 220100000, 701, 693, 514), //4.8
		//** 5.0 / 5.0* *//
		new Destination("iluma", 210100000, 1414, 1281, 336), //5.0
		new Destination("tower1", 210110000, 474, 499, 303), //5.0
		new Destination("norsvold", 220110000, 1807, 1986, 197), //5.0
		new Destination("tower2", 220120000, 474, 499, 303), //5.0
		new Destination("Ariels Rest", "arielsrest", 210100000, 1360, 371, 350),
		new Destination("Zephyr Vale", "zephyrvale", 210100000, 829, 1842, 310),
		new Destination("Polten Marsh", "poltenmarsh", 210100000, 2438, 1298, 227),
		new Destination("Kojol Valley", "kojolvalley", 210100000, 1886, 2069, 343),
		new Destination("Nightbloom Forest", "nightbloomforest", 220110000, 1462, 966, 226),
		new Destination("Aetherspring Lake", "aetherspringlake", 220110000, 986, 1381, 285),
		new Destination("Aetheric Gales", "aethericgales", 220110000, 1109, 2608, 231),
		new Destination("Canyon of Lost Souls", "canyonoflostsouls", 220110000, 2210, 1870, 239),
		//** 5.3 / 5.3* *//
		new Destination("teminon", 400010000, 2309, 606, 1538), //5.3
		new Destination("redemption", 400010000, 1778, 178, 2938), //5.3
		new Destination("primum", 400010000, 577, 2541, 1636), //5.3
		new Destination("harbinger", 400010000, 982, 2847, 3033), //5.3
		//** 地图测试 / Map Test* *//
		new Destination("art", "Test Server Art", 900180000, 508, 540, 100),
		new Destination("tag", "Test Tag Match", 900190000, 96, 114, 20),
		new Destination("attack", "Test Time Attack", 900200000, 96, 114, 20),
		new Destination("stage", "Tournament Stage Test", 900210000, 276, 242, 37),
		new Destination("lobby", "Tournament Lobby Test", 900230000, 276, 242, 37),
	};

	private static void goTo(final Player player, int worldId, float x, float y, float z) {
		WorldMap destinationMap = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(worldId);
		if (destinationMap.isInstanceType()) {
			TeleportService2.teleportTo(player, worldId, InstanceService.resolveInstanceId(worldId, player), x, y, z, player.getHeading(), TeleportAnimation.NO_ANIMATION);
		}
		else {
			TeleportService2.teleportTo(player, worldId, x, y, z, player.getHeading(), TeleportAnimation.NO_ANIMATION);
		}
	}

	/**
	 * 参数错误时显示命令语法。
	 * Show command syntax on invalid arguments.
	 * @param player 接收提示的玩家 / Player receiving the hint
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "Syntax : //goto <location>");
	}
}
