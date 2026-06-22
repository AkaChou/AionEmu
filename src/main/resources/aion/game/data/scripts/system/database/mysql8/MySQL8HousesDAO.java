package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.HousesDAO;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.house.HouseStatus;
import com.aionemu.gameserver.model.templates.housing.Building;
import com.aionemu.gameserver.model.templates.housing.BuildingType;
import com.aionemu.gameserver.model.templates.housing.HouseAddress;
import com.aionemu.gameserver.model.templates.housing.HousingLand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MySQL 8 implementation of HousesDAO
 * Fixed connection leaks
 */
public class MySQL8HousesDAO extends HousesDAO {
    
    private static final Logger log = LoggerFactory.getLogger(MySQL8HousesDAO.class);
    
    private static final String SELECT_HOUSES_QUERY = "SELECT * FROM houses WHERE address <> 2001 AND address <> 3001";
    private static final String SELECT_STUDIOS_QUERY = "SELECT * FROM houses WHERE address = 2001 OR address = 3001";
    private static final String ADD_HOUSE_QUERY = "INSERT INTO houses (id, address, building_id, player_id, acquire_time, " + "settings, status, fee_paid, next_pay, sell_started, sign_notice) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_HOUSE_QUERY = "UPDATE houses SET building_id = ?, player_id = ?, acquire_time = ?, " + "settings = ?, status = ?, fee_paid = ?, next_pay = ?, sell_started = ?, " + "sign_notice = ? WHERE id = ?";
    private static final String DELETE_HOUSE_QUERY = "DELETE FROM houses WHERE player_id = ?";
    private static final String SELECT_USED_IDS_QUERY = "SELECT DISTINCT id FROM houses";
    private static final String CHECK_ID_USED_QUERY = "SELECT COUNT(id) as cnt FROM houses WHERE id = ?";
    
    @Override
    public int[] getUsedIDs() {
        List<Integer> ids = new ArrayList<>();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement statement = con.prepareStatement(SELECT_USED_IDS_QUERY, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
             ResultSet rs = statement.executeQuery()) {
            
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            log.error("Can't get list of id's from houses table", e);
            return new int[0];
        }
        
        int[] result = new int[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            result[i] = ids.get(i);
        }
        return result;
    }
    
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
    
    @Override
    public boolean isIdUsed(int houseObjectId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement s = con.prepareStatement(CHECK_ID_USED_QUERY)) {
            
            s.setInt(1, houseObjectId);
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt") > 0;
                }
            }
            return false;
        } catch (SQLException e) {
            log.error("Can't check if house {} is used", houseObjectId, e);
            return true;
        }
    }
    
    @Override
    public void storeHouse(House house) {
        if (house.getPersistentState() == PersistentState.NEW) {
            insertNewHouse(house);
        } else {
            updateHouse(house);
        }
        house.setPersistentState(PersistentState.UPDATED);
    }
    
    private void insertNewHouse(House house) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(ADD_HOUSE_QUERY)) {
            
            stmt.setInt(1, house.getObjectId());
            stmt.setInt(2, house.getAddress().getId());
            stmt.setInt(3, house.getBuilding().getId());
            stmt.setInt(4, house.getOwnerId());
            
            if (house.getAcquiredTime() == null) {
                stmt.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            } else {
                stmt.setTimestamp(5, house.getAcquiredTime());
            }
            
            stmt.setInt(6, house.getPermissions());
            stmt.setString(7, house.getStatus().toString());
            stmt.setInt(8, house.isFeePaid() ? 1 : 0);
            
            if (house.getNextPay() == null) {
                stmt.setNull(9, Types.TIMESTAMP);
            } else {
                stmt.setTimestamp(9, house.getNextPay());
            }
            
            if (house.getSellStarted() == null) {
                stmt.setNull(10, Types.TIMESTAMP);
            } else {
                stmt.setTimestamp(10, house.getSellStarted());
            }
            
            byte[] signNotice = house.getSignNotice();
            if (signNotice == null || signNotice.length == 0) {
                stmt.setNull(11, Types.BINARY);
            } else {
                stmt.setBinaryStream(11, new ByteArrayInputStream(signNotice), signNotice.length);
            }
            
            stmt.executeUpdate();
            
        } catch (Exception e) {
            log.error("Could not store house data for house ID: {}", house.getObjectId(), e);
        }
    }
    
    private void updateHouse(House house) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_HOUSE_QUERY)) {
            
            stmt.setInt(1, house.getBuilding().getId());
            stmt.setInt(2, house.getOwnerId());
            
            if (house.getAcquiredTime() == null) {
                stmt.setNull(3, Types.TIMESTAMP);
            } else {
                stmt.setTimestamp(3, house.getAcquiredTime());
            }
            
            stmt.setInt(4, house.getPermissions());
            stmt.setString(5, house.getStatus().toString());
            stmt.setInt(6, house.isFeePaid() ? 1 : 0);
            
            if (house.getNextPay() == null) {
                stmt.setNull(7, Types.TIMESTAMP);
            } else {
                stmt.setTimestamp(7, house.getNextPay());
            }
            
            if (house.getSellStarted() == null) {
                stmt.setNull(8, Types.TIMESTAMP);
            } else {
                stmt.setTimestamp(8, house.getSellStarted());
            }
            
            byte[] signNotice = house.getSignNotice();
            if (signNotice == null || signNotice.length == 0) {
                stmt.setNull(9, Types.BINARY);
            } else {
                stmt.setBinaryStream(9, new ByteArrayInputStream(signNotice), signNotice.length);
            }
            
            stmt.setInt(10, house.getObjectId());
            stmt.executeUpdate();
            
        } catch (Exception e) {
            log.error("Could not update house data for house ID: {}", house.getObjectId(), e);
        }
    }
    
    @Override
    public Map<Integer, House> loadHouses(Collection<HousingLand> lands, boolean studios) {
        Map<Integer, House> houses = new HashMap<>();
        Map<Integer, HouseAddress> addressesById = new HashMap<>();
        Map<Integer, List<Building>> buildingsForAddress = new HashMap<>();
        
        for (HousingLand land : lands) {
            for (HouseAddress address : land.getAddresses()) {
                addressesById.put(address.getId(), address);
                buildingsForAddress.put(address.getId(), land.getBuildings());
            }
        }
        
        java.util.HashMap<Integer, Integer> addressHouseIds = new java.util.HashMap<>();
        String query = studios ? SELECT_STUDIOS_QUERY : SELECT_HOUSES_QUERY;
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query);
             ResultSet rset = stmt.executeQuery()) {
            
            while (rset.next()) {
                int houseId = rset.getInt("id");
                int buildingId = rset.getInt("building_id");
                int addressId = rset.getInt("address");
                
                HouseAddress address = addressesById.get(addressId);
                if (address == null) {
                    log.warn("Address not found for ID: {}", addressId);
                    continue;
                }
                
                List<Building> buildings = buildingsForAddress.get(address.getId());
                Building building = null;
                
                if (buildings != null) {
                    for (Building b : buildings) {
                        if (b.getId() == buildingId) {
                            building = b;
                            break;
                        }
                    }
                }
                
                if (building == null) {
                    log.warn("Building not found for ID: {} at address: {}", buildingId, addressId);
                    continue;
                }
                
                if (addressHouseIds.containsKey(address.getId())) {
                    log.warn("Duplicate house address {}!", address.getId());
                    continue;
                }
                
                House house = new House(houseId, building, address, 0);
                if (building.getType() == BuildingType.PERSONAL_FIELD) {
                    addressHouseIds.put(address.getId(), houseId);
                }
                
                house.setOwnerId(rset.getInt("player_id"));
                house.setAcquiredTime(rset.getTimestamp("acquire_time"));
                house.setPermissions(rset.getInt("settings"));
                
                String statusStr = rset.getString("status");
                try {
                    house.setStatus(HouseStatus.valueOf(statusStr));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid house status: {} for house ID: {}", statusStr, houseId);
                    house.setStatus(HouseStatus.INACTIVE);
                }
                
                house.setFeePaid(rset.getInt("fee_paid") != 0);
                house.setNextPay(rset.getTimestamp("next_pay"));
                house.setSellStarted(rset.getTimestamp("sell_started"));
                
                try (InputStream binaryStream = rset.getBinaryStream("sign_notice")) {
                    if (binaryStream != null) {
                        byte[] bytes = new byte[House.NOTICE_LENGTH];
                        int bytesRead = binaryStream.read(bytes);
                        if (bytesRead > 0) {
                            house.setSignNotice(bytes);
                        }
                    }
                }
                
                int id = studios ? house.getOwnerId() : address.getId();
                houses.put(id, house);
            }
            
        } catch (Exception e) {
            log.error("Could not restore House data from DB", e);
        }
        
        return houses;
    }
    
    @Override
    public void deleteHouse(int playerId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_HOUSE_QUERY)) {
            
            stmt.setInt(1, playerId);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            log.error("Delete House failed for player ID: {}", playerId, e);
        }
    }
}