package com.aionemu.gameserver.dao.mysql8;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.PlayerNpcFactionsDAO;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.npcFaction.ENpcFactionQuestState;
import com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFaction;
import com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFactions;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 玩家 NPC 阵营 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerNpcFactionsDAO.
 *
 * Updated for MySQL 8 - Fixed connection leaks.
 *
 * @author MrPoke
 */
@Slf4j
public class MySQL8PlayerNpcFactionsDAO extends PlayerNpcFactionsDAO {

	/** 查询 NPC 势力 SQL / Select NPC factions SQL*/
	private static final String SELECT_QUERY = "SELECT `faction_id`, `active`, `time`, `state`, `quest_id` FROM player_npc_factions WHERE `player_id`=?";
	/** 插入 NPC 势力 SQL / Insert NPC faction SQL*/
	private static final String INSERT_QUERY = "INSERT INTO player_npc_factions (`player_id`, `faction_id`, `active`, `time`, `state`, `quest_id`) VALUES (?,?,?,?,?,?)";
	/** 更新 NPC 势力 SQL / Update NPC faction SQL*/
	private static final String UPDATE_QUERY = "UPDATE player_npc_factions SET `active`=?, `time`=?, `state`=?, `quest_id`=? WHERE `player_id`=? AND `faction_id`=?";

	/**
	 * 加载玩家 NPC 阵营数据。
	 * Loads NPC faction data for a player.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void loadNpcFactions(Player player) {
		NpcFactions factions = new NpcFactions(player);

		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {

			stmt.setInt(1, player.getObjectId());

			try (ResultSet rset = stmt.executeQuery()) {
				while (rset.next()) {
					int faction_id = rset.getInt("faction_id");
					boolean active = rset.getBoolean("active");
					int time = rset.getInt("time");
					int questId = rset.getInt("quest_id");
					ENpcFactionQuestState state = ENpcFactionQuestState.valueOf(rset.getString("state"));
					NpcFaction faction = new NpcFaction(faction_id, time, active, state, questId);
					faction.setPersistentState(PersistentState.UPDATED);
					factions.addNpcFaction(faction);
				}
			}

			player.setNpcFactions(factions);
		} catch (Exception e) {
			log.error(I18n.get("log.165b89344e7f", player.getObjectId(), " from DB", e));
		}
	}

	/**
	 * 按持久化状态保存玩家 NPC 阵营。
	 * Stores player NPC factions according to their persistent state.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void storeNpcFactions(Player player) {
		for (NpcFaction npcFaction : player.getNpcFactions().getNpcFactions()) {
			switch (npcFaction.getPersistentState()) {
				case NEW:
					insertNpcFaction(player.getObjectId(), npcFaction);
					break;
				case UPDATE_REQUIRED:
					updateNpcFaction(player.getObjectId(), npcFaction);
					break;
				default:
					continue;
			}
			npcFaction.setPersistentState(PersistentState.UPDATED);
		}
	}

	/**
	 * 插入 NPC 阵营记录。
	 * Inserts an NPC faction record.
	 *
	 * player object id
	 * faction
	 */
	private void insertNpcFaction(int playerObjectId, NpcFaction faction) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {

			stmt.setInt(1, playerObjectId);
			stmt.setInt(2, faction.getId());
			stmt.setBoolean(3, faction.isActive());
			stmt.setInt(4, faction.getTime());
			stmt.setString(5, faction.getState().name());
			stmt.setInt(6, faction.getQuestId());
			stmt.executeUpdate();
		} catch (Exception e) {
			log.error(I18n.get("log.e4fe3ce16d8e", playerObjectId, " from DB", e));
		}
	}

	/**
	 * 更新 NPC 阵营记录。
	 * Updates an NPC faction record.
	 *
	 * player object id
	 * faction
	 */
	private void updateNpcFaction(int playerObjectId, NpcFaction faction) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {

			stmt.setBoolean(1, faction.isActive());
			stmt.setInt(2, faction.getTime());
			stmt.setString(3, faction.getState().name());
			stmt.setInt(4, faction.getQuestId());
			stmt.setInt(5, playerObjectId);
			stmt.setInt(6, faction.getId());
			stmt.executeUpdate();
		} catch (Exception e) {
			log.error(I18n.get("log.494a98a72b0b", playerObjectId, " from DB", e));
		}
	}

	/**
	 * 是否支持当前数据库。
	 * Whether the current database is supported.
	 *
	 * @param arg0 数据库名 / database name
	 * major version
	 * minor version
	 * whether supported
	 */
	@Override
	public boolean supports(String arg0, int arg1, int arg2) {
		return MySQL8DAOUtils.supports(arg0, arg1, arg2);
	}
}
