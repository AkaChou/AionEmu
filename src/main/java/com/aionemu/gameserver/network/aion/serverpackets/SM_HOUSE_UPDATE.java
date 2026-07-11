package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.model.gameobjects.HouseDecoration;
import com.aionemu.gameserver.model.gameobjects.SummonedHouseNpc;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.team.legion.LegionEmblem;
import com.aionemu.gameserver.model.team.legion.LegionMember;
import com.aionemu.gameserver.model.templates.housing.BuildingType;
import com.aionemu.gameserver.model.templates.housing.PartType;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import org.apache.commons.lang3.StringUtils;

/**
 * 向客户端同步房屋状态变更（外观部件、门牌、军团徽记等）的服务端包。
 * Server packet synchronizing house state updates (appearance parts, sign notice, legion emblem, etc.) to the client.
 */
public class SM_HOUSE_UPDATE extends AionServerPacket {
	private House house;

	/**
	 * 使用指定房屋构造状态更新包。
	 * Creates an update packet for the given house.
	 *
	 * @param house 待同步的房屋 / house to update
	 */
	public SM_HOUSE_UPDATE(House house) {
		this.house = house;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeH(1);
		writeH(0);
		writeH(1);
		writeD(0);
		writeD(house.getAddress().getId());
		int playerObjectId = house.getOwnerId();
		writeD(playerObjectId);
		writeD(house.getBuilding().getType().getId());
		writeC(1);
		writeD(house.getBuilding().getId());
		writeC(house.getHouseOwnerInfoFlags());
		writeC(house.getDoorState().getPacketValue());
		int dataSize = 52;
		if (house.getButler() != null) {
			SummonedHouseNpc butler = (SummonedHouseNpc) house.getButler();
			if (!StringUtils.isEmpty(butler.getMasterName())) {
				dataSize -= (butler.getMasterName().length() + 1) * 2;
				writeS(butler.getMasterName());
			}
		}
		for (int i = 0; i < dataSize; i++) {
			writeC(0);
		}
		LegionMember member = GameCoreGameplayServices.legionService().getLegionMember(playerObjectId);
		writeD(member == null ? 0 : member.getLegion().getLegionId());
		// 显示/隐藏所有者名称。 / Show/Hide Owner Name
		writeC(house.getNoticeState().getPacketValue());
		byte[] signNotice = house.getSignNotice();
		for (int i = 0; i < signNotice.length; i++) {
			writeC(signNotice[i]);
		}
		for (int i = signNotice.length; i < House.NOTICE_LENGTH; i++) {
			writeC(0);
		}
		writePartData(house, PartType.ROOF, 0, true);
		writePartData(house, PartType.OUTWALL, 0, true);
		writePartData(house, PartType.FRAME, 0, true);
		writePartData(house, PartType.DOOR, 0, true);
		writePartData(house, PartType.GARDEN, 0, true);
		writePartData(house, PartType.FENCE, 0, true);
		for (int floor = 0; floor < 6; floor++) {
			writePartData(house, PartType.INWALL_ANY, floor, floor > 0);
		}
		for (int floor = 0; floor < 6; floor++) {
			writePartData(house, PartType.INFLOOR_ANY, floor, floor > 0);
		}
		writePartData(house, PartType.ADDON, 0, true);
		writeD(0);
		writeD(0);
		writeC(0);

		// 徽章与颜色 / Emblem & Color
		if (member == null || member.getLegion().getLegionEmblem() == null) {
			writeC(0);
			writeC(0);
			writeD(0);
		} else {
			LegionEmblem emblem = member.getLegion().getLegionEmblem();
			writeC(emblem.getEmblemId());
			writeC(emblem.getEmblemType().getValue());
			writeC(emblem.isDefaultEmblem() ? 0x0 : 0xFF);
			writeC(emblem.getColor_r());
			writeC(emblem.getColor_g());
			writeC(emblem.getColor_b());
		}
	}

	/**
	 * 写入指定部位类型的装饰模板 ID；个人室内房在 skipPersonal 时写 0。
	 * Writes the decoration template ID for the given part type; writes 0 for personal indoor houses when skipPersonal is set.
	 *
	 * house
	 * part type
	 * @param floor 楼层索引 / floor index
	 * @param skipPersonal 是否跳过个人室内房部位 / whether to skip personal indoor parts
	 */
	private void writePartData(House house, PartType partType, int floor, boolean skipPersonal) {
		boolean isPersonal = house.getBuilding().getType() == BuildingType.PERSONAL_INS;
		HouseDecoration deco = house.getRenderPart(partType, floor);
		if (skipPersonal && isPersonal) {
			writeD(0);
		} else {
			writeD(deco != null ? deco.getTemplate().getId() : 0);
		}
	}
}
