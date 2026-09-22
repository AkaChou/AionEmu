package com.aionemu.gameserver.services;

import java.nio.ByteBuffer;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.configs.main.LegionConfig;
import com.aionemu.gameserver.dao.LegionDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.model.team.legion.LegionEmblem;
import com.aionemu.gameserver.model.team.legion.LegionEmblemType;
import com.aionemu.gameserver.model.team.legion.LegionHistoryType;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_SEND_EMBLEM;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_SEND_EMBLEM_DATA;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_UPDATE_EMBLEM;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

import lombok.extern.slf4j.Slf4j;

/**
 * 军团徽章域：负责徽章的接收上传、扣费生效、历史记录与在线成员下发。
 * Legion emblem domain: owns emblem upload intake, charged activation, history and fan-out to online members.
 * <p>该类型只服务 {@link LegionService}（与 {@link LegionMembers}/{@link LegionRestrictions} 同模式，
 * 持有宿主引用以回调历史记录与权限校验）。对外仍通过 {@link LegionService} 的原公开方法访问（门面签名不变）。
 * This type only serves {@link LegionService} (same pattern as {@link LegionMembers}/{@link LegionRestrictions},
 * keeping a host reference for history and permission callbacks). External callers keep using the original
 * {@link LegionService} facade methods with unchanged signatures.</p>
 */
@Slf4j
final class LegionEmblems {

	/** 自定义徽章上传的基纳费用 / Kinah cost for uploading a custom emblem. */
	private static final int CUSTOM_EMBLEM_COST = 1130000;
	/** 单个徽章数据包的最大字节数 / Max bytes carried by one emblem data packet. */
	private static final int MAX_EMBLEM_CHUNK = 7993;

	private final LegionService legion;

	/**
	 * 绑定宿主军团服务。
	 * Binds the hosting legion service.
	 * @param legionService 宿主军团服务 / hosting legion service
	 */
	LegionEmblems(LegionService legionService) {
		this.legion = legionService;
	}

	/**
	 * 保存自定义军团徽章并同步给所有在线成员。
	 * Stores a custom legion emblem and syncs it to all online members.
	 * @param activePlayer 操作玩家 / acting player
	 * @param customEmblem 自定义徽章 / custom emblem
	 */
	void storeCustomEmblem(Player activePlayer, LegionEmblem customEmblem) {
		legion.addHistory(activePlayer.getLegion(), "", LegionHistoryType.EMBLEM_MODIFIED);
		activePlayer.getLegion().setLegionEmblem(customEmblem);
		updateMembersEmblem(activePlayer.getLegion(), customEmblem.getEmblemType());
		PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_GUILD_CHANGE_EMBLEM);
	}

	/**
	 * 保存预设/标准军团徽章（扣费、写历史、广播更新）。
	 * Stores a standard/predefined legion emblem (charges kinah, writes history, broadcasts update).
	 * @param activePlayer 操作玩家 / acting player
	 * @param legionId 军团 ID / legion id
	 * @param emblemId 徽章模板 ID / emblem template id
	 * @param color_r 红色分量 / red component
	 * @param color_g 绿色分量 / green component
	 * @param color_b 蓝色分量 / blue component
	 * @param emblemType 徽章类型 / emblem type
	 */
	void storeStandardEmblem(Player activePlayer, int legionId, int emblemId, int color_r, int color_g,
			int color_b, LegionEmblemType emblemType) {
		if (legion.restrictions().canStoreLegionEmblem(activePlayer, legionId, emblemId)) {
			Legion legion2 = activePlayer.getLegion();
			if (legion2.getLegionEmblem().isDefaultEmblem()) {
				legion.addHistory(legion2, "", LegionHistoryType.EMBLEM_REGISTER);
			} else {
				legion.addHistory(legion2, "", LegionHistoryType.EMBLEM_MODIFIED);
			}
			activePlayer.getInventory().decreaseKinah(LegionConfig.LEGION_EMBLEM_REQUIRED_KINAH);
			legion2.getLegionEmblem().setEmblem(emblemId, color_r, color_g, color_b, emblemType, null);
			updateMembersEmblem(legion2, emblemType);
			PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_GUILD_CHANGE_EMBLEM);
		}
	}

	/**
	 * 开始上传自定义徽章：记录颜色/类型与总字节数并进入上传中状态。
	 * Starts custom emblem upload: records colors/type and total size, marks uploading.
	 * @param activePlayer 操作玩家 / acting player
	 * @param totalSize 徽章数据总大小 / total emblem data size
	 * @param color_r 红色分量 / red component
	 * @param color_g 绿色分量 / green component
	 * @param color_b 蓝色分量 / blue component
	 * @param emblemType 徽章类型 / emblem type
	 */
	void uploadEmblemInfo(Player activePlayer, int totalSize, int color_r, int color_g, int color_b,
			LegionEmblemType emblemType) {
		if (legion.restrictions().canUploadEmblemInfo(activePlayer)) {
			LegionEmblem legionEmblem = activePlayer.getLegion().getLegionEmblem();
			legionEmblem.resetUploadSettings();

			int emblemId = legionEmblem.getEmblemId() + 1;
			legionEmblem.setEmblem(emblemId, color_r, color_g, color_b, emblemType, null);
			legionEmblem.setUploadSize(totalSize);
			legionEmblem.setUploading(true);
		}
	}

	/**
	 * 接收自定义徽章分片数据；收齐后扣费并落库生效。
	 * Receives a chunk of custom emblem data; when complete, charges kinah and persists the emblem.
	 * @param activePlayer 操作玩家 / acting player
	 * @param size 本片字节数 / chunk size
	 * @param data 本片数据 / chunk bytes
	 */
	void uploadEmblemData(Player activePlayer, int size, byte[] data) {
		if (legion.restrictions().canUploadEmblem(activePlayer)) {
			LegionEmblem legionEmblem = activePlayer.getLegion().getLegionEmblem();
			legionEmblem.addUploadedSize(size);
			legionEmblem.addUploadData(data);

			if (legionEmblem.getUploadSize() == legionEmblem.getUploadedSize()) {
				if (legionEmblem.getUploadedSize() == 0 || legionEmblem.getUploadSize() == 0) {
					PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_GUILD_WARN_CORRUPT_EMBLEM_FILE);
					return;
				}
				if (!activePlayer.getInventory().tryDecreaseKinah(CUSTOM_EMBLEM_COST)) {
					PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_MONEY);
					return;
				}
				// 已完成 / Finished
				legionEmblem.setCustomEmblemData(legionEmblem.getUploadData());
				DAOManager.getDAO(LegionDAO.class).storeLegionEmblem(activePlayer.getLegion().getLegionId(),
						legionEmblem);
				LegionEmblem emblem = DAOManager.getDAO(LegionDAO.class)
						.loadLegionEmblem(activePlayer.getLegion().getLegionId());
				storeCustomEmblem(activePlayer, emblem);
			}
		}
	}

	/**
	 * 向玩家分包发送自定义徽章二进制数据。
	 * Sends custom emblem binary data to a player in packets.
	 * @param player 接收玩家 / receiving player
	 * @param legionEmblem 徽章对象 / emblem object
	 * @param legionId 军团 ID / legion id
	 * @param legionName 军团名称 / legion name
	 */
	void sendEmblemData(Player player, LegionEmblem legionEmblem, int legionId, String legionName) {
		PacketSendUtility.sendPacket(player,
				new SM_LEGION_SEND_EMBLEM(legionId, legionEmblem.getEmblemId(), legionEmblem.getColor_r(),
						legionEmblem.getColor_g(), legionEmblem.getColor_b(), legionName, legionEmblem.getEmblemType(),
						legionEmblem.getCustomEmblemData().length));
		ByteBuffer buf = ByteBuffer.allocate(legionEmblem.getCustomEmblemData().length);
		buf.put(legionEmblem.getCustomEmblemData()).position(0);
		log.debug(I18n.get("log.6d0b78f11bb1", buf.capacity()));
		int currentSize;
		byte[] bytes;
		do {
			log.debug(I18n.get("log.25810a3ba4ac", buf.position()));
			currentSize = buf.capacity() - buf.position();

			if (currentSize >= MAX_EMBLEM_CHUNK) {
				bytes = new byte[MAX_EMBLEM_CHUNK];
				for (int i = 0; i < MAX_EMBLEM_CHUNK; i++) {
					bytes[i] = buf.get();
				}
				log.debug(I18n.get("log.394b13fe9cb8", bytes.length));
				PacketSendUtility.sendPacket(player, new SM_LEGION_SEND_EMBLEM_DATA(MAX_EMBLEM_CHUNK, bytes));
			} else {
				bytes = new byte[currentSize];
				for (int i = 0; i < currentSize; i++) {
					bytes[i] = buf.get();
				}
				log.debug(I18n.get("log.394b13fe9cb8", bytes.length));
				PacketSendUtility.sendPacket(player, new SM_LEGION_SEND_EMBLEM_DATA(currentSize, bytes));
			}
		} while (buf.capacity() != buf.position());
	}

	/**
	 * 向每位在线军团成员广播徽章更新，自定义徽章附带分片数据。
	 * Broadcasts the emblem update to every online legion member, including chunked data for custom emblems.
	 * @param legion 目标军团 / target legion
	 * @param emblemType 徽章类型 / emblem type
	 */
	private void updateMembersEmblem(Legion legion, LegionEmblemType emblemType) {
		LegionEmblem legionEmblem = legion.getLegionEmblem();
		for (Player onlineLegionMember : legion.getOnlineLegionMembers()) {
			PacketSendUtility.broadcastPacket(onlineLegionMember,
					new SM_LEGION_UPDATE_EMBLEM(legion.getLegionId(), legionEmblem.getEmblemId(),
							legionEmblem.getColor_r(), legionEmblem.getColor_g(), legionEmblem.getColor_b(),
							emblemType),
					true);
			if (legionEmblem.getEmblemType() == LegionEmblemType.CUSTOM) {
				sendEmblemData(onlineLegionMember, legionEmblem, legion.getLegionId(), legion.getLegionName());
			}
		}
	}
}
