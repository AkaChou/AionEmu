package com.aionemu.gameserver.network.aion;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.sql.Timestamp;
import java.util.List;

import com.aionemu.gameserver.model.account.PlayerAccountData;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.PlayerAppearance;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.items.GodStone;
import com.aionemu.gameserver.model.items.ItemSlot;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;

/**
 * 角色信息写入辅助基类，用于选角列表等服务端包。
 * Player info write helper base used by character-list and related server packets.
 */
@Slf4j
public abstract class PlayerInfo extends AionServerPacket {


	/**
	 * 子类构造入口。
	 * Subclass constructor entry.
	 */
	protected PlayerInfo() {
	}

	/**
	 * 写入单个角色的外观、位置、装备摘要等选角信息。
	 * Writes one character's appearance, position, equipment summary for character select.
	 *
	 * @param accPlData 角色账号数据 / player account data
	 */
	protected void writePlayerInfo(PlayerAccountData accPlData) {
		PlayerCommonData pbd = accPlData.getPlayerCommonData();
		final int raceId = pbd.getRace().getRaceId();
		final int genderId = pbd.getGender().getGenderId();
		final PlayerAppearance playerAppearance = accPlData.getAppereance();
		writeD(pbd.getPlayerObjId());
		writeS(pbd.getName(), 52);
		writeD(genderId);
		writeD(raceId);
		writeD(pbd.getPlayerClass().getClassId());
		writeD(playerAppearance.getVoice());
		writeD(playerAppearance.getSkinRGB());
		writeD(playerAppearance.getHairRGB());
		writeD(playerAppearance.getEyeRGB());
		writeD(playerAppearance.getLipRGB());
		writeC(playerAppearance.getFace());
		writeC(playerAppearance.getHair());
		writeC(playerAppearance.getDeco());
		writeC(playerAppearance.getTattoo());
		writeC(playerAppearance.getFaceContour());
		writeC(playerAppearance.getExpression());
		writeC(playerAppearance.getPupilShape());
		writeC(playerAppearance.getRemoveMane());
		writeD(playerAppearance.getRightEyeRGB());
		writeC(playerAppearance.getEyeLashShape());
		writeC(0x06);// UNK 6
		writeC(playerAppearance.getJawLine());
		writeC(playerAppearance.getForehead());
		writeC(playerAppearance.getEyeHeight());
		writeC(playerAppearance.getEyeSpace());
		writeC(playerAppearance.getEyeWidth());
		writeC(playerAppearance.getEyeSize());
		writeC(playerAppearance.getEyeShape());
		writeC(playerAppearance.getEyeAngle());
		writeC(playerAppearance.getBrowHeight());
		writeC(playerAppearance.getBrowAngle());
		writeC(playerAppearance.getBrowShape());
		writeC(playerAppearance.getNose());
		writeC(playerAppearance.getNoseBridge());
		writeC(playerAppearance.getNoseWidth());
		writeC(playerAppearance.getNoseTip());
		writeC(playerAppearance.getCheek());
		writeC(playerAppearance.getLipHeight());
		writeC(playerAppearance.getMouthSize());
		writeC(playerAppearance.getLipSize());
		writeC(playerAppearance.getSmile());
		writeC(playerAppearance.getLipShape());
		writeC(playerAppearance.getJawHeigh());
		writeC(playerAppearance.getChinJut());
		writeC(playerAppearance.getEarShape());
		writeC(playerAppearance.getHeadSize());
		// 1.5.x：HeadSize 后为 0x00、肩宽、臂长、腿长（BYTE）。 / 1.5.x 0x00, shoulderSize, armLength, legLength (BYTE) after HeadSize

		writeC(playerAppearance.getNeck());
		writeC(playerAppearance.getNeckLength());
		writeC(playerAppearance.getShoulderSize()); // shoulderSize

		writeC(playerAppearance.getTorso());
		writeC(playerAppearance.getChest());
		writeC(playerAppearance.getWaist());
		writeC(playerAppearance.getHips());
		writeC(playerAppearance.getArmThickness());
		writeC(playerAppearance.getHandSize());
		writeC(playerAppearance.getLegThickness());
		writeC(playerAppearance.getFootSize());
		writeC(playerAppearance.getFacialRate());
		writeC(0);// unk;
		writeC(playerAppearance.getArmLength());
		writeC(playerAppearance.getLegLength());
		writeC(playerAppearance.getShoulders());
		writeC(playerAppearance.getFaceShape());
		writeC(playerAppearance.getPupilSize());
		writeC(playerAppearance.getUpperTorso());
		writeC(playerAppearance.getForeArmThickness());
		writeC(playerAppearance.getHandSpan());
		writeC(playerAppearance.getCalfThickness());
		writeC(0x00);// always 0 may be acessLevel
		writeC(0x00);// always 0
		writeC(0x00);// always 0

		writeF(playerAppearance.getHeight());
		int raceSex = 100000 + raceId * 2 + genderId;
		writeD(raceSex);
		writeD(pbd.getPosition().getMapId());
		writeF(pbd.getPosition().getX());
		writeF(pbd.getPosition().getY());
		writeF(pbd.getPosition().getZ());
		writeD(pbd.getPosition().getHeading());
		writeH(pbd.getLevel());
		writeH(0);
		writeD(pbd.getTitleId());
		if (accPlData.isLegionMember()) {
			writeD(accPlData.getLegion().getLegionId());
			writeS(accPlData.getLegion().getLegionName(), 82);
		} else {
			writeB(new byte[86]);
		}
		writeH(accPlData.isLegionMember() ? 0x01 : 0x00);
		Timestamp lastOnline = pbd.getLastOnline();
		writeD(getLastOnlineTimeInSeconds(lastOnline));
		int itemsDataSize = 0;
		List<Item> items = accPlData.getEquipment();
		for (Item item : items) {
			if (itemsDataSize >= 208) {
				break;
			}
			ItemTemplate itemTemplate = item.getItemTemplate();
			if (itemTemplate == null) {
				log.warn(I18n.get("log.afc9d6d2f89b", pbd.getPlayerObjId(), item.getObjectId()));
				continue;
			}
			long slot = item.getEquipmentSlot();
			if (slot == 393216 || slot == 131072 || slot == 262144) {
				continue;
			}
			if (itemTemplate.isArmor() || itemTemplate.isWeapon()) {
				if (itemTemplate.getItemSlot() <= ItemSlot.PANTS.getSlotIdMask()) {
					writeC(slot == 2 || slot == 64 || slot == 256 ? 2 : 1);
					writeD(item.getItemSkinTemplate().getTemplateId());
					GodStone godStone = item.getGodStone();
					writeD(godStone != null ? godStone.getItemId() : 0);
					writeD(item.getItemColor());
					itemsDataSize += 13;
				}
			}
		}
		byte[] stupidNc = new byte[208 - itemsDataSize];
		writeB(stupidNc);
		writeB(new byte[92]);// NA 5.0.2.7 protocol
		writeD(accPlData.getDeletionTimeInSeconds());
	}

	static int getLastOnlineTimeInSeconds(Timestamp lastOnline) {
		return lastOnline == null ? 0 : (int) (lastOnline.getTime() / 1000L);
	}
}
