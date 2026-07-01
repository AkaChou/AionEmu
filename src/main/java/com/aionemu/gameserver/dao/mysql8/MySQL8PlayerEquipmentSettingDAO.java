package com.aionemu.gameserver.dao.mysql8;

import lombok.extern.slf4j.Slf4j;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.PlayerEquipmentSettingDAO;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.equipmentsetting.EquipmentSetting;
import com.aionemu.gameserver.model.gameobjects.player.equipmentsetting.EquipmentSettingList;
@Slf4j

public class MySQL8PlayerEquipmentSettingDAO extends PlayerEquipmentSettingDAO {


	static final String INSERT_QUERY = "INSERT INTO `player_equipment_setting` (`player_id`, `slot`, `name`, `display`, `m_hand`, `s_hand`, `helmet`, `torso`, `glove`, `boots`, `earrings_left`, `earrings_right`, `ring_left`, `ring_right`, `necklace`, `shoulder`, `pants`, `powershard_left`, `powershard_right`, `wings`, `waist`, `m_off_hand`, `s_off_hand`, `plume`, `bracelet`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE `name` = IF(`name` = '', VALUES(`name`), `name`), `display` = VALUES(`display`), `m_hand` = VALUES(`m_hand`), `s_hand` = VALUES(`s_hand`), `helmet` = VALUES(`helmet`), `torso` = VALUES(`torso`), `glove` = VALUES(`glove`), `boots` = VALUES(`boots`), `earrings_left` = VALUES(`earrings_left`), `earrings_right` = VALUES(`earrings_right`), `ring_left` = VALUES(`ring_left`), `ring_right` = VALUES(`ring_right`), `necklace` = VALUES(`necklace`), `shoulder` = VALUES(`shoulder`), `pants` = VALUES(`pants`), `powershard_left` = VALUES(`powershard_left`), `powershard_right` = VALUES(`powershard_right`), `wings` = VALUES(`wings`), `waist` = VALUES(`waist`), `m_off_hand` = VALUES(`m_off_hand`), `s_off_hand` = VALUES(`s_off_hand`), `plume` = VALUES(`plume`), `bracelet` = VALUES(`bracelet`)";
	private static final String SELECT_QUERY = "SELECT * FROM `player_equipment_setting` WHERE `player_id` = ?";

	@Override
	public void loadEquipmentSetting(Player player) {
		EquipmentSettingList equipmentSettingList = new EquipmentSettingList(player);
		try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {
			stmt.setInt(1, player.getObjectId());
			try (ResultSet rset = stmt.executeQuery()) {
				while (rset.next()) {
					equipmentSettingList.add(rset.getInt("slot"), rset.getString("name"), rset.getInt("display"),
							rset.getInt("m_hand"), rset.getInt("s_hand"), rset.getInt("helmet"),
							rset.getInt("torso"), rset.getInt("glove"), rset.getInt("boots"),
							rset.getInt("earrings_left"), rset.getInt("earrings_right"), rset.getInt("ring_left"),
							rset.getInt("ring_right"), rset.getInt("necklace"), rset.getInt("shoulder"),
							rset.getInt("pants"), rset.getInt("powershard_left"), rset.getInt("powershard_right"),
							rset.getInt("wings"), rset.getInt("waist"), rset.getInt("m_off_hand"),
							rset.getInt("s_off_hand"), rset.getInt("plume"), rset.getInt("bracelet"), false);
				}
			}
		} catch (Exception e) {
			log.error("Could not restore equipment settings for player {}", player.getObjectId(), e);
		}
		player.setEquipmentSettingList(equipmentSettingList);
	}

	@Override
	public void insertEquipmentSetting(Player player, EquipmentSetting equipmentSetting) {
		try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
			stmt.setInt(1, player.getObjectId());
			stmt.setInt(2, equipmentSetting.getSlot());
			stmt.setString(3, equipmentSetting.getName());
			stmt.setInt(4, equipmentSetting.getDisplay());
			stmt.setInt(5, equipmentSetting.getmHand());
			stmt.setInt(6, equipmentSetting.getsHand());
			stmt.setInt(7, equipmentSetting.getHelmet());
			stmt.setInt(8, equipmentSetting.getTorso());
			stmt.setInt(9, equipmentSetting.getGlove());
			stmt.setInt(10, equipmentSetting.getBoots());
			stmt.setInt(11, equipmentSetting.getEarringsLeft());
			stmt.setInt(12, equipmentSetting.getEarringsRight());
			stmt.setInt(13, equipmentSetting.getRingLeft());
			stmt.setInt(14, equipmentSetting.getRingRight());
			stmt.setInt(15, equipmentSetting.getNecklace());
			stmt.setInt(16, equipmentSetting.getShoulder());
			stmt.setInt(17, equipmentSetting.getPants());
			stmt.setInt(18, equipmentSetting.getPowershardLeft());
			stmt.setInt(19, equipmentSetting.getPowershardRight());
			stmt.setInt(20, equipmentSetting.getWings());
			stmt.setInt(21, equipmentSetting.getWaist());
			stmt.setInt(22, equipmentSetting.getmOffHand());
			stmt.setInt(23, equipmentSetting.getsOffHand());
			stmt.setInt(24, equipmentSetting.getPlume());
			stmt.setInt(25, equipmentSetting.getBracelet());
			stmt.executeUpdate();
			equipmentSetting.setPersistentState(PersistentState.UPDATED);
		} catch (Exception e) {
			log.error("Could not store equipment setting for player {}", player.getObjectId(), e);
		}
	}

	@Override
	public boolean supports(String databaseName, int majorVersion, int minorVersion) {
		return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
	}
}
