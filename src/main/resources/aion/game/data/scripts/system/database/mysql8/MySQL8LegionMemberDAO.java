package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.LegionMemberDAO;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.team.legion.LegionMember;
import com.aionemu.gameserver.model.team.legion.LegionMemberEx;
import com.aionemu.gameserver.model.team.legion.LegionRank;
import com.aionemu.gameserver.services.LegionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;

/**
 * @author Simple
 * Updated for MySQL 8 - Fixed connection leaks and SQL syntax
 */
public class MySQL8LegionMemberDAO extends LegionMemberDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8LegionMemberDAO.class);
    
    private static final String INSERT_LEGIONMEMBER_QUERY = "INSERT INTO legion_members (`legion_id`, `player_id`, `rank`) VALUES (?, ?, ?)";
    
    private static final String UPDATE_LEGIONMEMBER_QUERY = "UPDATE legion_members SET nickname = ?, `rank` = ?, selfintro = ?, challenge_score = ? WHERE player_id = ?";
    
    private static final String SELECT_LEGIONMEMBER_QUERY = "SELECT * FROM legion_members WHERE player_id = ?";
    private static final String DELETE_LEGIONMEMBER_QUERY = "DELETE FROM legion_members WHERE player_id = ?";
    private static final String SELECT_LEGIONMEMBERS_QUERY = "SELECT player_id FROM legion_members WHERE legion_id = ?";
    private static final String SELECT_LEGIONMEMBEREX_QUERY = "SELECT players.name, players.exp, players.player_class, players.last_online, " + "players.world_id, legion_members.* FROM players, legion_members " + "WHERE players.id = ? AND players.id = legion_members.player_id";
    private static final String SELECT_LEGIONMEMBEREX2_QUERY = "SELECT players.id, players.exp, players.player_class, players.last_online, " + "players.world_id, legion_members.* FROM players, legion_members " + "WHERE players.name = ? AND players.id = legion_members.player_id";
    private static final String CHECK_ID_USED_QUERY = "SELECT COUNT(player_id) as cnt FROM legion_members WHERE player_id = ?";
    private static final String SELECT_USED_IDS_QUERY = "SELECT player_id FROM legion_members";

    @Override
    public boolean isIdUsed(final int playerObjId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement s = con.prepareStatement(CHECK_ID_USED_QUERY)) {
            
            s.setInt(1, playerObjId);
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt") > 0;
                }
            }
            return false;
        } catch (SQLException e) {
            log.error("Can't check if player ID {} is used in legion", playerObjId, e);
            return true;
        }
    }

    @Override
    public boolean saveNewLegionMember(final LegionMember legionMember) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_LEGIONMEMBER_QUERY)) {
            
            stmt.setInt(1, legionMember.getLegion().getLegionId());
            stmt.setInt(2, legionMember.getObjectId());
            stmt.setString(3, legionMember.getRank().toString());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Error saving new legion member: {}", legionMember.getObjectId(), e);
            return false;
        }
    }

    @Override
    public void storeLegionMember(final int playerId, final LegionMember legionMember) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_LEGIONMEMBER_QUERY)) {
            
            stmt.setString(1, legionMember.getNickname());
            stmt.setString(2, legionMember.getRank().toString()); // rank экранирован в запросе
            stmt.setString(3, legionMember.getSelfIntro());
            stmt.setInt(4, legionMember.getChallengeScore());
            stmt.setInt(5, playerId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Error storing legion member: {}", playerId, e);
        }
    }

    @Override
    public LegionMember loadLegionMember(final int playerObjId) {
        if (playerObjId == 0) {
            return null;
        }
        
        LegionMember result = null;
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_LEGIONMEMBER_QUERY)) {
            
            stmt.setInt(1, playerObjId);
            
            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    LegionMember legionMember = new LegionMember(playerObjId);
                    int legionId = resultSet.getInt("legion_id");
                    
                    try {
                        legionMember.setRank(LegionRank.valueOf(resultSet.getString("rank")));
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid legion rank for player {}: {}", playerObjId, resultSet.getString("rank"));
                        legionMember.setRank(LegionRank.VOLUNTEER);
                    }
                    
                    legionMember.setNickname(resultSet.getString("nickname"));
                    legionMember.setSelfIntro(resultSet.getString("selfintro"));
                    legionMember.setChallengeScore(resultSet.getInt("challenge_score"));
                    legionMember.setLegion(LegionService.getInstance().getLegion(legionId));
                    
                    result = legionMember;
                }
            }
        } catch (SQLException e) {
            log.error("Error loading legion member: {}", playerObjId, e);
        }

        return result;
    }

    @Override
    public LegionMemberEx loadLegionMemberEx(final int playerObjId) {
        LegionMemberEx result = null;
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_LEGIONMEMBEREX_QUERY)) {
            
            stmt.setInt(1, playerObjId);
            
            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    LegionMemberEx legionMemberEx = new LegionMemberEx(playerObjId);
                    
                    legionMemberEx.setName(resultSet.getString("players.name"));
                    legionMemberEx.setExp(resultSet.getLong("players.exp"));
                    
                    try {
                        legionMemberEx.setPlayerClass(PlayerClass.valueOf(resultSet.getString("players.player_class")));
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid player class for player {}: {}", playerObjId, resultSet.getString("players.player_class"));
                    }
                    
                    legionMemberEx.setLastOnline(resultSet.getTimestamp("players.last_online"));
                    legionMemberEx.setWorldId(resultSet.getInt("players.world_id"));

                    int legionId = resultSet.getInt("legion_members.legion_id");
                    
                    try {
                        legionMemberEx.setRank(LegionRank.valueOf(resultSet.getString("legion_members.rank")));
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid legion rank for player {}: {}", playerObjId, resultSet.getString("legion_members.rank"));
                        legionMemberEx.setRank(LegionRank.VOLUNTEER);
                    }
                    
                    legionMemberEx.setNickname(resultSet.getString("legion_members.nickname"));
                    legionMemberEx.setSelfIntro(resultSet.getString("legion_members.selfintro"));
                    legionMemberEx.setLegion(LegionService.getInstance().getLegion(legionId));
                    
                    result = legionMemberEx;
                }
            }
        } catch (SQLException e) {
            log.error("Error loading legion member ex: {}", playerObjId, e);
        }

        return result;
    }

    @Override
    public LegionMemberEx loadLegionMemberEx(final String playerName) {
        LegionMemberEx result = null;
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_LEGIONMEMBEREX2_QUERY)) {
            
            stmt.setString(1, playerName);
            
            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    LegionMemberEx legionMember = new LegionMemberEx(playerName);
                    
                    legionMember.setObjectId(resultSet.getInt("id"));
                    legionMember.setExp(resultSet.getLong("exp"));
                    
                    try {
                        legionMember.setPlayerClass(PlayerClass.valueOf(resultSet.getString("player_class")));
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid player class for player {}: {}", playerName, resultSet.getString("player_class"));
                    }
                    
                    legionMember.setLastOnline(resultSet.getTimestamp("last_online"));
                    legionMember.setWorldId(resultSet.getInt("world_id"));

                    int legionId = resultSet.getInt("legion_id");
                    
                    try {
                        legionMember.setRank(LegionRank.valueOf(resultSet.getString("rank")));
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid legion rank for player {}: {}", playerName, resultSet.getString("rank"));
                        legionMember.setRank(LegionRank.VOLUNTEER);
                    }
                    
                    legionMember.setNickname(resultSet.getString("nickname"));
                    legionMember.setSelfIntro(resultSet.getString("selfintro"));
                    legionMember.setLegion(LegionService.getInstance().getLegion(legionId));
                    
                    result = legionMember;
                }
            }
        } catch (SQLException e) {
            log.error("Error loading legion member ex by name: {}", playerName, e);
        }

        return result;
    }

    @Override
    public ArrayList<Integer> loadLegionMembers(final int legionId) {
        final ArrayList<Integer> legionMembers = new ArrayList<>();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_LEGIONMEMBERS_QUERY)) {
            
            stmt.setInt(1, legionId);
            
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    int playerObjId = resultSet.getInt("player_id");
                    legionMembers.add(playerObjId);
                }
            }
        } catch (SQLException e) {
            log.error("Error loading legion members for legion: {}", legionId, e);
        }

        return legionMembers.isEmpty() ? null : legionMembers;
    }

    @Override
    public void deleteLegionMember(int playerObjId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement statement = con.prepareStatement(DELETE_LEGIONMEMBER_QUERY)) {
            
            statement.setInt(1, playerObjId);
            statement.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to delete legion member: {}", playerObjId, e);
        }
    }

    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }

    @Override
    public int[] getUsedIDs() {
        ArrayList<Integer> ids = new ArrayList<>();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement statement = con.prepareStatement(SELECT_USED_IDS_QUERY,
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
             ResultSet rs = statement.executeQuery()) {
            
            while (rs.next()) {
                ids.add(rs.getInt("player_id"));
            }
        } catch (SQLException e) {
            log.error("Can't get list of id's from legion_members table", e);
            return new int[0];
        }
        
        int[] result = new int[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            result[i] = ids.get(i);
        }
        return result;
    }
}