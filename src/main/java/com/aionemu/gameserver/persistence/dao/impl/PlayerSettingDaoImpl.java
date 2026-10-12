package com.aionemu.gameserver.persistence.dao.impl;

import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerSettings;
import com.aionemu.gameserver.persistence.dao.PlayerSettingDao;
import com.aionemu.gameserver.persistence.entity.PlayerSettingPo;
import com.aionemu.gameserver.persistence.mapper.PlayerSettingMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家 UI/客户端设置数据访问实现。
 * Data access implementation for player UI/client settings persistence.
 * @author ATracer
 */
@Slf4j
@Service
@Lazy
public class PlayerSettingDaoImpl extends ServiceImpl<PlayerSettingMapper, PlayerSettingPo>
	implements PlayerSettingDao {

	/**
	 * 加载玩家设置。
	 * Loads player settings.
	 * @param player 玩家 / player
	 */
	@Override
	public void loadSettings(final Player player) {
		final int playerId = player.getObjectId();
		final PlayerSettings playerSettings = new PlayerSettings();

		try {
			mapper.selectListByQuery(QueryWrapper.create().eq(PlayerSettingPo::getPlayerId, playerId))
				.forEach(po -> {
					switch (intValue(po.getSettingsType())) {
						case 0 -> playerSettings.setUiSettings(po.getSettings());
						case 1 -> playerSettings.setShortcuts(po.getSettings());
						case 2 -> playerSettings.setHouseBuddies(po.getSettings());
						case -1 -> playerSettings.setDisplay(blobToInt(po.getSettings()));
						case -2 -> playerSettings.setDeny(blobToInt(po.getSettings()));
						default -> {
						}
					}
				});
		} catch (DataAccessException e) {
			log.error(I18n.get("log.6e9496b8330d", playerId, " from DB"), e);
		}

		playerSettings.setPersistentState(PersistentState.UPDATED);
		player.setPlayerSettings(playerSettings);
	}

	/**
	 * 保存玩家设置。
	 * Saves player settings.
	 * @param player 玩家 / player
	 */
	@Override
	public void saveSettings(final Player player) {
		final int playerId = player.getObjectId();
		PlayerSettings playerSettings = player.getPlayerSettings();

		if (playerSettings.getPersistentState() == PersistentState.UPDATED) {
			return;
		}

		final byte[] uiSettings = playerSettings.getUiSettings();
		final byte[] shortcuts = playerSettings.getShortcuts();
		final byte[] houseBuddies = playerSettings.getHouseBuddies();
		final int display = playerSettings.getDisplay();
		final int deny = playerSettings.getDeny();

		try {
			if (uiSettings != null) {
				saveSetting(playerId, 0, uiSettings);
			}
			if (shortcuts != null) {
				saveSetting(playerId, 1, shortcuts);
			}
			if (houseBuddies != null) {
				saveSetting(playerId, 2, houseBuddies);
			}
			saveSetting(playerId, -1, Integer.toString(display));
			saveSetting(playerId, -2, Integer.toString(deny));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.19b35d703bb1", playerId), e);
			return;
		}

		playerSettings.setPersistentState(PersistentState.UPDATED);
	}

	/**
	 * 写入一行二进制设置（界面/快捷键/房屋好友）。
	 * Writes one binary settings row (UI/shortcuts/house buddies).
	 *
	 * @param playerId     玩家 ID / player id
	 * @param settingsType 设置类型 / settings type
	 * @param settings     二进制设置内容 / binary settings payload
	 */
	private void saveSetting(int playerId, int settingsType, byte[] settings) {
		PlayerSettingPo po = new PlayerSettingPo();
		po.setPlayerId(playerId);
		po.setSettingsType(settingsType);
		po.setSettings(settings);
		mapper.replaceSetting(po);
	}

	/**
	 * 写入一行数值设置（显示/拒绝）；BLOB 列保存十进制文本，与原实现 setInt 写 BLOB 的存储形态一致。
	 * Writes one numeric settings row (display/deny); the BLOB column holds decimal text, matching the
	 * storage produced by the legacy setInt into a BLOB column.
	 *
	 * @param playerId     玩家 ID / player id
	 * @param settingsType 设置类型 / settings type
	 * @param settings     十进制数值文本 / decimal value text
	 */
	private void saveSetting(int playerId, int settingsType, String settings) {
		saveSetting(playerId, settingsType, settings.getBytes(StandardCharsets.US_ASCII));
	}

	/**
	 * BLOB 数值读取：还原旧实现 rs.getInt 对 BLOB 的数字前缀解析语义（非数字视为 0）。
	 * Reads a numeric BLOB: reproduces the legacy rs.getInt coercion of a BLOB column (non-numeric becomes 0).
	 *
	 * @param settings 二进制设置内容 / binary settings payload
	 * @return 解析出的整数 / parsed int
	 */
	private static int blobToInt(byte[] settings) {
		if (settings == null) {
			return 0;
		}

		try {
			return Integer.parseInt(new String(settings, StandardCharsets.US_ASCII).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/**
	 * 可空整数列的空值归一化，保持原 rs.getInt 读取语义（NULL 视为 0）。
	 * Normalizes nullable integer columns on read, keeping the legacy rs.getInt semantics (NULL becomes 0).
	 *
	 * @param value 可空值 / nullable value
	 * @return 归一化后的整数 / normalized int
	 */
	private static int intValue(Integer value) {
		return value == null ? 0 : value;
	}
}
