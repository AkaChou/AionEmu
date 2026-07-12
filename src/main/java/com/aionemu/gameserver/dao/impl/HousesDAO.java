package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.house.HouseStatus;
import com.aionemu.gameserver.model.templates.housing.Building;
import com.aionemu.gameserver.model.templates.housing.BuildingType;
import com.aionemu.gameserver.model.templates.housing.HouseAddress;
import com.aionemu.gameserver.model.templates.housing.HousingLand;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 房屋数据 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of HousesDAO.
 * Fixed connection leaks.
 */
@Slf4j
public class HousesDAO extends com.aionemu.gameserver.dao.HousesDAO {


    /** 查询普通房屋 / Select regular houses */
    private static final String SELECT_HOUSES_QUERY = "SELECT * FROM houses WHERE address <> 2001 AND address <> 3001";
    /** 查询工作室房屋 / Select studio houses */
    private static final String SELECT_STUDIOS_QUERY = "SELECT * FROM houses WHERE address = 2001 OR address = 3001";
    /** 插入新房屋 / Insert new house */
    private static final String ADD_HOUSE_QUERY = "INSERT INTO houses (id, address, building_id, player_id, acquire_time, " + "settings, status, fee_paid, next_pay, sell_started, sign_notice) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    /** 更新房屋 / Update house */
    private static final String UPDATE_HOUSE_QUERY = "UPDATE houses SET building_id = ?, player_id = ?, acquire_time = ?, " + "settings = ?, status = ?, fee_paid = ?, next_pay = ?, sell_started = ?, " + "sign_notice = ? WHERE id = ?";
    /** 按玩家删除房屋 / Delete house by player */
    private static final String DELETE_HOUSE_QUERY = "DELETE FROM houses WHERE player_id = ?";
    /** 查询已使用房屋 ID / Select used house ids */
    private static final String SELECT_USED_IDS_QUERY = "SELECT DISTINCT id FROM houses";
    /** 检查房屋 ID 是否已使用 / Check whether house id is used */
    private static final String CHECK_ID_USED_QUERY = "SELECT COUNT(id) as cnt FROM houses WHERE id = ?";

    /**
     * 获取所有已使用的房屋 ID。
     * Returns all used house ids.
     *
     * 已使用 ID 数组。
     * used id array.
     */
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
            log.error(I18n.get("log.977a91e82734", e));
            return new int[0];
        }

        int[] result = new int[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            result[i] = ids.get(i);
        }
        return result;
    }

    /**
     * 判断当前数据库是否受本 DAO 支持。
     * Checks whether the given database is supported by this DAO.
     *
     * @param databaseName 数据库名称 / database name
     * major version
     * minor version
     * whether supported
     */
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }

    /**
     * 判断指定房屋对象 ID 是否已使用。
     * Checks whether the given house object id is already used.
     *
     * house object id
     *
     * @param houseObjectId @return 是否已使用 / whether used
     */
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
            log.error(I18n.get("log.a9a5e7d0614f", houseObjectId, e));
            return true;
        }
    }

    /**
     * 持久化房屋（按状态执行插入或更新）。
     * Persists a house (insert or update depending on persistent state).
     *
     * house
     */
    @Override
    public void storeHouse(House house) {
        if (house.getPersistentState() == PersistentState.NEW) {
            insertNewHouse(house);
        } else {
            updateHouse(house);
        }
        house.setPersistentState(PersistentState.UPDATED);
    }

    /**
     * 插入新房屋记录。
     * Inserts a new house record.
     *
     * house
     */
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
            log.error(I18n.get("log.4abe7981971b", house.getObjectId(), e));
        }
    }

    /**
     * 更新已有房屋记录。
     * Updates an existing house record.
     *
     * house
     */
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
            log.error(I18n.get("log.340314d885f4", house.getObjectId(), e));
        }
    }

    /**
     * 从数据库加载房屋（普通房或工作室）。
     * Loads houses from the database (regular houses or studios).
     *
     * @param lands 地块模板集合 / housing land templates
     * @param studios 是否加载工作室 / whether to load studios
     * house map
     */
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
                    log.warn(I18n.get("log.32a2bfc0a656", addressId));
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
                    log.warn(I18n.get("log.89b55a9b2477", buildingId, addressId));
                    continue;
                }

                if (addressHouseIds.containsKey(address.getId())) {
                    log.warn(I18n.get("log.28a74968b3a8", address.getId()));
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
                    log.warn(I18n.get("log.9d052d863885", statusStr, houseId));
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
            log.error(I18n.get("log.423d09af3ba2", e));
        }

        return houses;
    }

    /**
     * 删除指定玩家的房屋记录。
     * Deletes house records for the given player.
     *
     * player id
     */
    @Override
    public void deleteHouse(int playerId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_HOUSE_QUERY)) {

            stmt.setInt(1, playerId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            log.error(I18n.get("log.f2facfdc760c", playerId, e));
        }
    }
}
