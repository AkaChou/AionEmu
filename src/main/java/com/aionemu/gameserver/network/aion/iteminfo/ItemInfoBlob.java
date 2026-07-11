package com.aionemu.gameserver.network.aion.iteminfo;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.functions.IStatFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatFunction;
import com.aionemu.gameserver.model.templates.item.ArmorType;
import com.aionemu.gameserver.model.templates.item.EquipType;
import com.aionemu.gameserver.model.templates.item.ItemCategory;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.PacketWriteHelper;

/**
 * 物品信息 Blob 容器，聚合若干 {@link ItemBlobEntry} 并序列化为客户端可读格式。
 * 负责按物品模板与运行时状态组装完整或局部的物品详情包。
 * Container for item-info blobs that aggregates {@link ItemBlobEntry} instances
 * and serializes them into the client-readable format.
 * Builds full or partial item detail payloads from the item template and runtime state.
 *
 * @author -Nemesiss-
 * @modified Rolandas
 */
public class ItemInfoBlob extends PacketWriteHelper {
	/** 所属玩家。 / Owning player. */
	protected final Player player;
	/** 目标物品。 / Target item. */
	protected final Item item;

	/** 已添加 blob 条目列表 / List of added blob entries */
	private List<ItemBlobEntry> itemBlobEntries = new ArrayList<ItemBlobEntry>();

	/**
	 * 为指定玩家与物品创建 Blob 容器。
	 * Creates a blob container for the given player and item.
	 *
	 * 所属玩家 / owning player
	 * @param item 目标物品 / target item
	 */
	public ItemInfoBlob(Player player, Item item) {
		this.player = player;
		this.item = item;
	}

	/**
	 * 写入总长度及全部 Blob 条目。
	 * Writes the total size followed by all blob entries.
	 */
	@Override
	public void writeMe(ByteBuffer buf) {
		writeH(buf, size());
		for (ItemBlobEntry ent : itemBlobEntries) {
			ent.writeMe(buf);
		}
	}

	/**
	 * 按类型创建并添加一个 Blob 条目。
	 * Creates and adds a blob entry of the given type.
	 *
	 * blob type
	 */
	public void addBlobEntry(ItemBlobType type) {
		ItemBlobEntry ent = type.newBlobEntry();
		ent.setOwner(player, item, null);
		itemBlobEntries.add(ent);
	}

	/**
	 * 添加一条属性加成 Blob 条目。
	 * Adds a stat-bonus blob entry bound to the given modifier.
	 *
	 * stat modifier
	 */
	public void addBonusBlobEntry(IStatFunction modifier) {
		ItemBlobEntry ent = ItemBlobType.STAT_BONUSES.newBlobEntry();
		ent.setOwner(player, item, modifier);
		itemBlobEntries.add(ent);
	}

	/**
	 * 工厂方法：创建并绑定所属关系的单个 Blob 条目。
	 * 不支持 {@link ItemBlobType#STAT_BONUSES}（需通过 {@link #addBonusBlobEntry}）。
	 * Factory method that creates a single blob entry and binds ownership.
	 * Does not support {@link ItemBlobType#STAT_BONUSES} (use {@link #addBonusBlobEntry}).
	 *
	 * blob type
	 * 所属玩家 / owning player
	 * @param item 所属物品 / owning item
	 * @return 已绑定的条目 / bound entry
	 */
	public static ItemBlobEntry newBlobEntry(ItemBlobType type, Player player, Item item) {
		if (type == ItemBlobType.STAT_BONUSES) {
			throw new UnsupportedOperationException();
		}
		ItemBlobEntry ent = type.newBlobEntry();
		ent.setOwner(player, item, null);
		return ent;
	}

	/**
	 * 按物品类型与状态组装完整的物品信息 Blob。
	 * 可装备物品会附加槽位、魔石、调校、羽饰/手环等信息；非装备则可能仅含普通信息或尘晶碎片。
	 * Assembles a full item-info blob based on item type and runtime state.
	 * Equipable items get slot, mana-stone, conditioning, plume/bracelet data, etc.;
	 * non-equipable items may only include general info or a stigma-shard entry.
	 *
	 * 所属玩家 / owning player
	 * @param item 目标物品 / target item
	 * full blob
	 */
	public static ItemInfoBlob getFullBlob(Player player, Item item) {
		ItemInfoBlob blob = new ItemInfoBlob(player, item);
		ItemTemplate itemTemplate = item.getItemTemplate();
		if (itemTemplate.getWeaponType() != null && itemTemplate.isTwoHandWeapon()) {
			blob.addBlobEntry(ItemBlobType.COMPOSITE_ITEM);
		}
		if (item.getEquipmentType() != EquipType.NONE) {
			blob.addBlobEntry(ItemBlobType.EQUIPPED_SLOT);
			if (itemTemplate.getArmorType() != null && itemTemplate.getArmorType() != ArmorType.NO_ARMOR) {
				switch (itemTemplate.getArmorType()) {
				case WING:
					blob.addBlobEntry(ItemBlobType.SLOTS_WING);
					break;
				case SHIELD:
					blob.addBlobEntry(ItemBlobType.SLOTS_SHIELD);
					break;
				default:
					blob.addBlobEntry(ItemBlobType.SLOTS_ARMOR);
					break;
				}
			} else if (itemTemplate.isWeapon()) {
				blob.addBlobEntry(ItemBlobType.SLOTS_WEAPON);
			} else if (item.getEquipmentType() == EquipType.ARMOR) {
				blob.addBlobEntry(ItemBlobType.SLOTS_ACCESSORY);
			}
			blob.addBlobEntry(ItemBlobType.MANA_SOCKETS);
			if (item.getConditioningInfo() != null) {
				blob.addBlobEntry(ItemBlobType.CONDITIONING_INFO);
			}
			if (blob.getBlobEntries().size() > 0) {
				blob.addBlobEntry(ItemBlobType.PREMIUM_OPTION);
				if (itemTemplate.isCanIdian()) {
					blob.addBlobEntry(ItemBlobType.IDIAN_INFO);
				}
			}
			blob.addBlobEntry(ItemBlobType.WRAPP_INFO);
			if (itemTemplate.getCategory() == ItemCategory.PLUME) {
				blob.addBlobEntry(ItemBlobType.PLUME_INFO);
			}
			if (itemTemplate.getCategory() == ItemCategory.BRACELET) {
				blob.addBlobEntry(ItemBlobType.BRACELET_INFO);
			}
			List<StatFunction> allModifiers = itemTemplate.getModifiers();
			if (allModifiers != null) {
				for (IStatFunction modifier : allModifiers) {
					if (modifier.isBonus() && !modifier.hasConditions()) {
						blob.addBonusBlobEntry(modifier);
					}
				}
			}
		} else if (itemTemplate.getTemplateId() == 141000001) {
			blob.addBlobEntry(ItemBlobType.STIGMA_SHARD);
		}
		blob.addBlobEntry(ItemBlobType.GENERAL_INFO);
		return blob;
	}

	/**
	 * 返回当前已添加的 Blob 条目列表。
	 * Returns the list of currently added blob entries.
	 *
	 * entry list
	 */
	public List<ItemBlobEntry> getBlobEntries() {
		return itemBlobEntries;
	}

	/**
	 * 计算全部条目的总负载长度（每条目含 1 字节类型 ID）。
	 * Computes the total payload size of all entries (each entry includes a 1-byte type id).
	 *
	 * total size in bytes
	 */
	public int size() {
		int totalSize = 0;
		for (ItemBlobEntry ent : itemBlobEntries) {
			totalSize += ent.getSize() + 1;
		}
		return totalSize;
	}

	/**
	 * 物品信息 Blob 类型枚举，映射客户端入口 ID 与具体条目实现。
	 * Item-info blob type enum mapping client entry ids to concrete entry implementations.
	 */
	public enum ItemBlobType {
		/** 通用物品信息。 / General item info. */
		GENERAL_INFO(0x00) {
			@Override
			ItemBlobEntry newBlobEntry() {
				return new GeneralInfoBlobEntry();
			}
		},
		/** 武器槽位信息。 / Weapon slot info. */
		SLOTS_WEAPON(0x01) {
			@Override
			ItemBlobEntry newBlobEntry() {
				return new WeaponInfoBlobEntry();
			}
		},
		/** 防具槽位信息。 / Armor slot info. */
		SLOTS_ARMOR(0x02) {
			@Override
			ItemBlobEntry newBlobEntry() {
				return new ArmorInfoBlobEntry();
			}
		},
		/** 盾牌槽位信息。 / Shield slot info. */
		SLOTS_SHIELD(0x03) {
			@Override
			ItemBlobEntry newBlobEntry() {
				return new ShieldInfoBlobEntry();
			}
		},
		/** 饰品槽位信息。 / Accessory slot info. */
		SLOTS_ACCESSORY(0x04) {
			@Override
			ItemBlobEntry newBlobEntry() {
				return new AccessoryInfoBlobEntry();
			}
		},
		/** 当前装备槽位。 / Currently equipped slot. */
		EQUIPPED_SLOT(0x06) {
			@Override
			ItemBlobEntry newBlobEntry() {
				return new EquippedSlotBlobEntry();
			}
		},
		/** 尘晶技能信息。 / Stigma skill info. */
		STIGMA_INFO(0x07) {
			@Override
			ItemBlobEntry newBlobEntry() {
				return new StigmaInfoBlobEntry();
			}
		},
		/** 尘晶碎片信息。 / Stigma shard info. */
		STIGMA_SHARD(0x08) {
			@Override
			ItemBlobEntry newBlobEntry() {
				return new StigmaShardInfoBlobEntry();
			}
		},
		/** random options / random options */
		PREMIUM_OPTION(0x10) {
			@Override
			ItemBlobEntry newBlobEntry() {
				return new PremiumOptionInfoBlobEntry();
			}
		},
		/** 艾帝安（抛光）信息。 / Idian (polish) info. */
		IDIAN_INFO(0x11) {
			@Override
			ItemBlobEntry newBlobEntry() {
				return new IdianInfoBlobEntry();
			}
		},
		/** 包装/拆封次数信息。Wrap/unwrap count info. */
		WRAPP_INFO(0x12) {
			@Override
			ItemBlobEntry newBlobEntry() {
				return new WrappInfoBlobEntry();
			}
		},
		/** 羽饰信息。 / Plume info. */
		PLUME_INFO(0x13) {
			@Override
			ItemBlobEntry newBlobEntry() {
				return new PlumeInfoBlobEntry();
			}
		},
		/** 手环信息。 / Bracelet info. */
		BRACELET_INFO(0x14) {
			@Override
			ItemBlobEntry newBlobEntry() {
				return new BraceletInfoBlobEntry();
			}
		},
		/** 属性加成。 / Stat bonuses. */
		STAT_BONUSES(0x0A) {
			@Override
			ItemBlobEntry newBlobEntry() {
				return new BonusInfoBlobEntry();
			}
		},
		/** 魔石孔位与附魔等。 / Mana sockets, enchant, etc. */
		MANA_SOCKETS(0x0B) {
			@Override
			ItemBlobEntry newBlobEntry() {
				return new ManaStoneInfoBlobEntry();
			}
		},
		/** 翅膀槽位信息。 / Wing slot info. */
		SLOTS_WING(0x0D) {
			@Override
			ItemBlobEntry newBlobEntry() {
				return new WingInfoBlobEntry();
			}
		},
		/** 合成/融合物品信息。Composite/fusioned item info. */
		COMPOSITE_ITEM(0x0E) {
			@Override
			ItemBlobEntry newBlobEntry() {
				return new CompositeItemBlobEntry();
			}
		},
		/** 调校（充能）信息。 / Conditioning (charge) info. */
		CONDITIONING_INFO(0x0F) {
			@Override
			ItemBlobEntry newBlobEntry() {
				return new ConditioningInfoBlobEntry();
			}
		};

		/** 客户端入口 ID。 / Client entry id. */
		private int entryId;

		/**
		 * 以客户端入口 ID 构造类型。
		 * Constructs a type with the given client entry id.
		 *
		 * entry id
		 */
		private ItemBlobType(int entryId) {
			this.entryId = entryId;
		}

		/**
		 * 返回客户端入口 ID。
		 * Returns the client entry id.
		 *
		 * entry id
		 */
		public int getEntryId() {
			return entryId;
		}

		/**
		 * 创建对应的 Blob 条目实例。
		 * Creates a new blob entry instance for this type.
		 *
		 * new entry
		 */
		abstract ItemBlobEntry newBlobEntry();
	}
}
