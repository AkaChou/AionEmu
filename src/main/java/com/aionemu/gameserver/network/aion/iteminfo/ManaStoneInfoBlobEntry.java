package com.aionemu.gameserver.network.aion.iteminfo;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Set;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.items.IdianStone;
import com.aionemu.gameserver.model.items.ItemStone;
import com.aionemu.gameserver.model.items.ManaStone;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob.ItemBlobType;

/**
 * 魔石孔位与附魔信息 Blob。
 * 写入灵魂绑定、强化等级、皮肤、可选孔位、魔石、神石、染色、艾帝安、
 * 羽饰加成、增幅、技能强化与月石重塑等数据。
 * Blob for mana sockets and enchant-related info.
 * Writes soul-bind, enchant level, skin, optional sockets, mana stones, god stone,
 * dye, Idian, plume bonuses, amplification, skill boost, and Luna reskin data.
 *
 * @author -Nemesiss-
 * @modified Rolandas
 */
public class ManaStoneInfoBlobEntry extends ItemBlobEntry {

	/**
	 * 构造魔石孔位 Blob 条目。
	 * Constructs a mana-socket blob entry.
	 */
	ManaStoneInfoBlobEntry() {
		super(ItemBlobType.MANA_SOCKETS);
	}

	/**
	 * 将本 Blob 的具体内容写入缓冲区。
	 * Writes this blob's concrete payload into the buffer.
	 */
	@Override
	public void writeThisBlob(ByteBuffer buf) {
		Item item = ownerItem;
		writeC(buf, item.isSoulBound() ? 1 : 0);
		writeC(buf, item.getEnchantLevel());
		writeD(buf, item.getItemSkinTemplate().getTemplateId());
		writeC(buf, item.getOptionalSocket());
		writeC(buf, item.hasEnchantBonus() ? item.getEnchantBonus() : 0);
		writeItemStones(buf);
		ItemStone god = item.getGodStone();
		writeD(buf, god == null ? 0 : god.getItemId());
		int itemColor = item.getItemColor();
		int dyeExpiration = item.getColorTimeLeft();
		if ((dyeExpiration > 0 && item.getColorExpireTime() > 0 || dyeExpiration == 0 && item.getColorExpireTime() == 0)
				&& item.getItemTemplate().isItemDyePermitted()) {
			writeC(buf, itemColor == 0 ? 0 : 1);
			writeD(buf, itemColor);
			writeD(buf, 0);
			writeD(buf, dyeExpiration);
		} else {
			writeC(buf, 0);
			writeD(buf, 0);
			writeD(buf, 0);
			writeD(buf, 0);
		}
		IdianStone idianStone = item.getIdianStone();
		if (idianStone != null && idianStone.getPolishNumber() > 0) {
			writeD(buf, idianStone.getItemId());
			writeC(buf, idianStone.getPolishNumber());
		} else {
			writeD(buf, 0);
			writeC(buf, 0);
		}
		writeC(buf, item.getAuthorize());
		writeH(buf, 0);
		writePlumeBonusStat(buf);
		writeB(buf, new byte[36]);
		writeAmplification(buf);
		writeB(buf, new byte[12]);
		writeSkillBoost(buf);
		writeD(buf, item.isLunaReskin() ? 1 : 0);
		writeC(buf, item.getReductionLevel());
	}

	/**
	 * 写入增幅相关字段。
	 * Writes amplification-related fields.
	 */
	private void writeAmplification(ByteBuffer buf) {
		Item item = ownerItem;
		int skillId = item.getAmplificationSkill();
		boolean hasSkill = item.isAmplified() && DataManager.SKILL_DATA.getSkillTemplate(skillId) != null;
		writeC(buf, hasSkill ? 1 : 0);
		writeD(buf, hasSkill ? skillId : 0);
	}

	/**
	 * 写入技能强化（Enhance）数据。
	 * Writes skill-boost (enhance) data.
	 */
	private void writeSkillBoost(ByteBuffer buf) {
		Item item = this.ownerItem;
		boolean hasSkillBoost = item.isEnhance() && item.getEnhanceSkillId() > 0 && item.getEnhanceEnchantLevel() > 0;
		writeD(buf, hasSkillBoost ? item.getEnhanceSkillId() : 0);
		writeD(buf, hasSkillBoost ? item.getEnhanceEnchantLevel() : 0);
	}

	/**
	 * 写入羽饰加成属性；非羽饰则填零。
	 * Writes plume bonus stats; fills zeros for non-plume items.
	 */
	private void writePlumeBonusStat(ByteBuffer buf) {
		Item item = ownerItem;
		if (item.getItemTemplate().isPlume()) {
			writeD(buf, 0);
			writeD(buf, 0);
			writeD(buf, 0);
			writeD(buf, 0);
			writeD(buf, 42);
			writeD(buf, item.getAuthorize() * 150);
			if (item.getItemTemplate().getTemperingTableId() == 10051
					|| item.getItemTemplate().getTemperingTableId() == 10063
					|| item.getItemTemplate().getTemperingTableId() == 10107) {
				writeD(buf, 30);
				writeD(buf, item.getAuthorize() * 4);
				writeD(buf, 0);
				writeD(buf, 0);
			} else if (item.getItemTemplate().getTemperingTableId() == 10052
					|| item.getItemTemplate().getTemperingTableId() == 10064
					|| item.getItemTemplate().getTemperingTableId() == 10108) {
				writeD(buf, 35);
				writeD(buf, item.getAuthorize() * 20);
				writeD(buf, 0);
				writeD(buf, 0);
			} else if (item.getItemTemplate().getTemperingTableId() == 10056
					|| item.getItemTemplate().getTemperingTableId() == 10065
					|| item.getItemTemplate().getTemperingTableId() == 10109) {
				writeD(buf, 33);
				writeD(buf, item.getAuthorize() * 12);
				writeD(buf, 0);
				writeD(buf, 0);
			} else if (item.getItemTemplate().getTemperingTableId() == 10057
					|| item.getItemTemplate().getTemperingTableId() == 10066
					|| item.getItemTemplate().getTemperingTableId() == 10110) {
				writeD(buf, 36);
				writeD(buf, item.getAuthorize() * 8);
				writeD(buf, 0);
				writeD(buf, 0);
			} else if (item.getItemTemplate().getTemperingTableId() == 10103
					|| item.getItemTemplate().getTemperingTableId() == 10105) {
				writeD(buf, 32);
				writeD(buf, item.getAuthorize() * 16);
				writeD(buf, 0);
				writeD(buf, 0);
			} else if (item.getItemTemplate().getTemperingTableId() == 10104
					|| item.getItemTemplate().getTemperingTableId() == 10106) {
				writeD(buf, 34);
				writeD(buf, item.getAuthorize() * 8);
				writeD(buf, 0);
				writeD(buf, 0);
			}
			writeD(buf, 0);
			writeD(buf, 0);
			writeD(buf, 0);
			writeD(buf, 0);
			writeD(buf, 0);
			writeD(buf, 0);
		} else {
			writeB(buf, new byte[64]);
		}
	}

	/**
	 * 写入物品镶嵌的魔石列表（古代石优先）。
	 * Writes socketed mana stones (ancient stones first).
	 */
	private void writeItemStones(ByteBuffer buf) {
		Item item = ownerItem;
		int count = 0;
		if (item.hasManaStones()) {
			Set<ManaStone> itemStones = item.getItemStones();
			ArrayList<ManaStone> basicStones = new ArrayList<ManaStone>();
			ArrayList<ManaStone> ancientStones = new ArrayList<ManaStone>();
			for (ManaStone itemStone : itemStones) {
				if (itemStone.isBasic()) {
					basicStones.add(itemStone);
				} else {
					ancientStones.add(itemStone);
				}
			}
			if (item.getItemTemplate().getSpecialSlots() > 0) {
				if (ancientStones.size() > 0) {
					for (ManaStone ancientStone : ancientStones) {
						if (count == 6) {
							break;
						}
						writeD(buf, ancientStone.getItemId());
						count++;
					}
				}
				for (int i = count; i < item.getItemTemplate().getSpecialSlots(); i++) {
					writeD(buf, 0);
					count++;
				}
			}
			for (ManaStone basicStone : basicStones) {
				if (count == 6) {
					break;
				}
				writeD(buf, basicStone.getItemId());
				count++;
			}
			skip(buf, (6 - count) * 4);
		} else {
			skip(buf, 24);
		}
	}

	/**
	 * 返回本 Blob 负载的字节长度。
	 * Returns the payload size of this blob in bytes.
	 */
	@Override
	public int getSize() {
		return 187;
	}
}
