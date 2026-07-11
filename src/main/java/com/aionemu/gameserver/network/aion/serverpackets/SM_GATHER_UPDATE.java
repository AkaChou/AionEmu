package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.templates.gather.GatherableTemplate;
import com.aionemu.gameserver.model.templates.gather.Material;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端发送采集进度与结果更新的服务端包。
 * Server packet that sends gathering progress and result updates to the client.
 *
 * @author ATracer
 * @author orz
 * @author Antraxx
 */
public class SM_GATHER_UPDATE extends AionServerPacket {

	private GatherableTemplate template;
	private int action;
	private int itemId;
	private int success;
	private int failure;
	private int nameId;

	/**
	 * @param template 可采集物模板 / Gatherable template
	 * Material being gathered
	 * Success progress
	 * Failure progress
	 * @param action 采集动作类型 / Gathering action type
	 */
	public SM_GATHER_UPDATE(GatherableTemplate template, Material material, int success, int failure, int action) {
		this.action = action;
		this.template = template;
		this.itemId = material.getItemid();
		this.success = success;
		this.failure = failure;
		this.nameId = material.getNameid();
	}

	/**
	 * 按 action 写入不同阶段的采集进度、计时与结果数据。
	 * Writes gathering progress, timers, and result data for the given action stage.
	 */
	@Override
	protected void writeImpl(AionConnection con) {
		writeH(template.getHarvestSkill());
		writeC(action);
		writeD(itemId);

		switch (action) {
		case 0: {
			writeQ(con.getActivePlayer().getRates().getGatheringCountRate());
			writeD(success);
			writeD(failure);
			writeD(0);
			writeD(1200); // timer??
			writeD(1330011); // ??text??skill??
			writeH(0x24); // 0x24
			writeD(this.nameId);
			writeH(0); // 0x24
			break;
		}
		case 1: {
			writeQ(con.getActivePlayer().getRates().getGatheringCountRate());
			writeD(success);
			writeD(failure);
			writeD(700); // unk timer??
			writeD(1200); // unk timer??
			writeD(0); // unk timer??writeD(700);
			writeH(0);
			break;
		}
		case 2: {
			writeQ(con.getActivePlayer().getRates().getGatheringCountRate());
			writeD(success);
			writeD(failure);
			writeD(700);// unk timer??
			writeD(1200); // unk timer??
			writeD(0); // unk timer??writeD(700);
			writeH(0);
			break;
		}
		case 3: {
			writeQ(con.getActivePlayer().getRates().getGatheringCountRate());
			writeD(success);
			writeD(failure);
			writeD(700);// unk timer??
			writeD(1200); // unk timer??
			writeD(0); // unk timer??writeD(700);
			writeH(0);
			break;
		}
		case 5: // you have stopped gathering
		{
			writeQ(con.getActivePlayer().getRates().getGatheringCountRate());
			writeD(0);
			writeD(0);
			writeD(700);// unk timer??
			writeD(1200); // unk timer??
			writeD(1330080); // unk timer??writeD(700);
			writeH(0);
			break;
		}
		case 6: {
			writeQ(con.getActivePlayer().getRates().getGatheringCountRate());
			writeD(success);
			writeD(failure);
			writeD(700); // unk timer??
			writeD(1200); // unk timer??
			writeD(0); // unk timer??writeD(700);
			writeH(0);
			break;
		}
		case 7: {
			writeQ(con.getActivePlayer().getRates().getGatheringCountRate());
			writeD(success);
			writeD(failure);
			writeD(0);
			writeD(1200); // timer??
			writeD(1330079); // ??text??skill??
			writeH(0x24); // 0x24
			writeD(nameId);
			writeH(0); // 0x24
			break;
		}
		}
	}
}
