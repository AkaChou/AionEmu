package com.aionemu.gameserver.model.items;

/**
 * 物品 ID 枚举。
 * Item Id enumeration.
 */

public enum ItemId {
	/** 基纳。 / Kinah. */
	KINAH(182400001), LUNA(182495000), // Aion 5.0.5
	/** 烙印之石碎片 / Stigma Shard*/
	STIGMA_SHARD(141000001), RUSTED_MEDAL(182005205), RUSTED_MEDAL_ASMODIANS(182005206), BROKEN_COIN(182005367),
	/** Veille Flavor / Veille Flavor */
	VEILLE_FLAVOR(182006281), // Aion 2.0
	/** Mastarius Flavor / Mastarius Flavor */
	MASTARIUS_FLAVOR(182006282), // Aion 2.0
	/** Iron Coin Elyos / Iron Coin Elyos */
	IRON_COIN_ELYOS(186000001), BRONZE_COIN_ELYOS(186000002), SILVER_COIN_ELYOS(186000003), GOLD_COIN_ELYOS(186000004),
	/** Platinum Coin Elyos / Platinum Coin Elyos */
	PLATINUM_COIN_ELYOS(186000005), IRON_COIN_ASMODIANS(186000006), BRONZE_COIN_ASMODIANS(186000007),
	/** Silver Coin Asmodians / Silver Coin Asmodians */
	SILVER_COIN_ASMODIANS(186000008), GOLD_COIN_ASMODIANS(186000009), PLATINUM_COIN_ASMODIANS(186000010),
	/** Mithril Coin Elyos / Mithril Coin Elyos */
	MITHRIL_COIN_ELYOS(186000018), // Aion 2.0
	/** Mithril Coin Asmodians / Mithril Coin Asmodians */
	MITHRIL_COIN_ASMODIANS(186000019), // Aion 2.0
	/** 金勋章 / Golden Medal*/
	GOLDEN_MEDAL(186000030), SILVER_MEDAL(186000031), BALAUR_HEART(186000032), HOT_BALAUR_HEART(186000033),
	/** Angels Eye / Angels Eye */
	ANGELS_EYE(186000037), DAEMONS_EYE(186000038),
	// 王冠。 / Crown.
	/** Major Ancien Crown / Major Ancien Crown */
	MAJOR_ANCIEN_CROWN(186000051), GREATER_ANCIEN_CROWN(186000052), ANCIEN_CROWN(186000053),
	/** Lesser Ancien Crown / Lesser Ancien Crown */
	LESSER_ANCIEN_CROWN(186000054),
	// 高脚杯。 / Goblet.
	/** Major Ancien Goblet / Major Ancien Goblet */
	MAJOR_ANCIEN_GOBLET(186000055), GREATER_ANCIEN_GOBLET(186000056), ANCIEN_GOBLET(186000057),
	/** Lesser Ancien Goblet / Lesser Ancien Goblet */
	LESSER_ANCIEN_GOBLET(186000058),
	// 封印。 / Seal.
	/** Major Ancien Seal / Major Ancien Seal */
	MAJOR_ANCIEN_SEAL(186000059), GREATER_ANCIEN_SEAL(186000060), ANCIEN_SEAL(186000061), LESSER_ANCIEN_SEAL(186000062),
	// 图标。 / Icon.
	/** Major Ancien Icon / Major Ancien Icon */
	MAJOR_ANCIEN_ICON(186000063), GREATER_ANCIEN_ICON(186000064), ANCIEN_ICON(186000065), LESSER_ANCIEN_ICON(186000066),
	/** Sanctum Decoration / Sanctum Decoration */
	SANCTUM_DECORATION(186000078), PANDAEMONIUM_DECORATION(186000079), ETERNITY_JEWELL(186000094),
	/** Platinum Medal / Platinum Medal */
	PLATINUM_MEDAL(186000096), BURNING_BALAUR_HEART(186000097), DECREE_OF_VALOR(186000098), // Aion 2.0
	/** Progress Token / Progress Token */
	PROGRESS_TOKEN(186000100), // Aion 2.0
	/** Radiant Token / Radiant Token */
	RADIANT_TOKEN(186000101), // Aion 2.0
	/** Fortuneers Token / Fortuneers Token */
	FORTUNEERS_TOKEN(186000102), // Aion 2.0
	/** Ward Token / Ward Token */
	WARD_TOKEN(186000103), // Aion 2.0
	/** Crusader Token / Crusader Token */
	CRUSADER_TOKEN(186000104), // Aion 2.0
	/** Daemon Token / Daemon Token */
	DAEMON_TOKEN(186000105), // Aion 2.0
	/** Orichalcum Token / Orichalcum Token */
	ORICHALCUM_TOKEN(186000114), // Aion 2.5
	/** Circle Token / Circle Token */
	CIRCLE_TOKEN(186000115), // Aion 2.5
	/** Groggie Elyos / Groggie Elyos */
	GROGGIE_ELYOS(186000122), // Aion 2.1
	/** Groggie Asmodians / Groggie Asmodians */
	GROGGIE_ASMODIANS(186000123), // Aion 2.1
	/** Worthiness Ticket Elyos / Worthiness Ticket Elyos */
	WORTHINESS_TICKET_ELYOS(186000124), // Aion 2.7
	/** Worthiness Ticket Asmodians / Worthiness Ticket Asmodians */
	WORTHINESS_TICKET_ASMODIANS(186000125), // Aion 2.7
	/** Ascension Energy / Ascension Energy */
	ASCENSION_ENERGY(186000127), // Aion 1.5
	/** Crucible Insignia Elyos / Crucible Insignia Elyos */
	CRUCIBLE_INSIGNIA_ELYOS(186000130), // Aion 2.5 ~ Aion 2.6
	/** Crucible Insignia Asmodians / Crucible Insignia Asmodians */
	CRUCIBLE_INSIGNIA_ASMODIANS(186000131), // Aion 2.5 ~ Aion 2.6
	/** Wright Token / Wright Token */
	WRIGHT_TOKEN(186000132), // Aion 3.0
	/** Shaper Token / Shaper Token */
	SHAPER_TOKEN(186000133), // Aion 3.0
	/** Worthiness Ticket / Worthiness Ticket */
	WORTHINESS_TICKET(186000134), // Aion 2.5 ~ Aion 2.6
	/** 混沌竞技场入场券 / Arena Of Chaos Ticket */
	ARENA_OF_CHAOS_TICKET(186000135), // Aion 2.7
	/** 孤独竞技场入场券 / Arena Of Discipline Ticket */
	ARENA_OF_DISCIPLINE_TICKET(186000136), // Aion 2.7
	/** Courage Insignia / Courage Insignia */
	COURAGE_INSIGNIA(186000137), // Aion 2.7
	/** Plant Fossil Fragment / Plant Fossil Fragment */
	PLANT_FOSSIL_FRAGMENT(186000138), // Aion 2.6
	/** Insect Fossil Fragment / Insect Fossil Fragment */
	INSECT_FOSSIL_FRAGMENT(186000139), // Aion 2.6
	/** Animal Fossil Fragment / Animal Fossil Fragment */
	ANIMAL_FOSSIL_FRAGMENT(186000140), // Aion 2.6
	/** 混沌竞技场入场券 2 级 / Arena Of Chaos Ticket Level 2 */
	ARENA_OF_CHAOS_TICKET_LEVEL_2(186000141), // Aion 3.0
	/** 孤独竞技场入场券 2 级 / Arena Of Discipline Ticket Level 2 */
	ARENA_OF_DISCIPLINE_TICKET_LEVEL_2(186000142), // Aion 3.0
	/** Karon Coin / Karon Coin */
	KARON_COIN(186000143), // Aion 3.0
	/** Leaf Coin / Leaf Coin */
	LEAF_COIN(186000146), // Aion 3.0
	/** Mithril Medal / Mithril Medal */
	MITHRIL_MEDAL(186000147), // Aion 3.0
	/** Opportunity Token / Opportunity Token */
	OPPORTUNITY_TOKEN(186000165), // Aion 3.0
	/** 合作竞技场入场券 / Arena Of Harmony Ticket */
	ARENA_OF_HARMONY_TICKET(186000184), // Aion 3.5
	/** 荣耀竞技场入场券 / Arena Of Glory Ticket */
	ARENA_OF_GLORY_TICKET(186000185), // Aion 3.5
	/** 军团硬币 / Legion Coin*/
	LEGION_COIN(186000199), // Aion 3.5
	/** 龙族勋章 / Balaur Medal*/
	BALAUR_MEDAL(186000200), // Aion 3.5
	/** Protectorate Coin / Protectorate Coin */
	PROTECTORATE_COIN(186000201), // Aion 3.5
	/** Tiamat Blood / Tiamat Blood */
	TIAMAT_BLOOD(186000202), // Aion 3.5
	/** Ascension Crystal / Ascension Crystal */
	ASCENSION_CRYSTAL(186000221), // Aion 3.7
	/** Veteran Crystal / Veteran Crystal */
	VETERAN_CRYSTAL(186000222), // Aion 3.7
	/** Glorious Insignia / Glorious Insignia */
	GLORIOUS_INSIGNIA(182213259); // Aion 3.5

	private int itemId;

	private ItemId(int itemId) {
		this.itemId = itemId;
	}

	/** 值。 / Value. */
	public int value() {
		return itemId;
	}
}
