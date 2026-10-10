package com.aionemu.gameserver.dao.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.house.HouseStatus;
import com.aionemu.gameserver.model.templates.housing.Building;
import com.aionemu.gameserver.model.templates.housing.BuildingType;
import com.aionemu.gameserver.model.templates.housing.HouseAddress;
import com.aionemu.gameserver.model.templates.housing.HousingLand;

import lombok.extern.slf4j.Slf4j;

/**
 * 房屋数据 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of HousesDAO using JdbcClient.
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

    private final JdbcClient jdbcClient;

    public HousesDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    public HousesDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    /**
     * 获取所有已使用的房屋 ID。
     * Returns all used house ids.
     */
    @Override
    public int[] getUsedIDs() {
        try {
            List<Integer> ids = jdbcClient.sql(SELECT_USED_IDS_QUERY)
                .query((rs, rowNum) -> rs.getInt(1))
                .list();

            int[] result = new int[ids.size()];
            for (int i = 0; i < ids.size(); i++) {
                result[i] = ids.get(i);
            }
            return result;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.977a91e82734"), e);
            return new int[0];
        }
    }

    /**
     * 判断当前数据库是否受本 DAO 支持。
     * Checks whether the given database is supported by this DAO.
     * @param databaseName 数据库名 / database name
     * @param majorVersion 主版本 / major version
     * @param minorVersion 次版本 / minor version
     * @return 是否支持 / whether supported
     */
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }

    /**
     * 判断指定房屋对象 ID 是否已使用。
     * Checks whether the given house object id is already used.
     * @param houseObjectId 房屋对象 ID / house object id
     * @return 是否已使用 / whether used
     */
    @Override
    public boolean isIdUsed(int houseObjectId) {
        try {
            Integer count = jdbcClient.sql(CHECK_ID_USED_QUERY)
                .param(houseObjectId)
                .query(Integer.class)
                .optional()
                .orElse(0);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.a9a5e7d0614f", houseObjectId), e);
            return true;
        }
    }

    /**
     * 持久化房屋（按状态执行插入或更新）。
     * Persists a house (insert or update depending on persistent state).
     * @param house 房屋 / house
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
     * @param house 房屋 / house
     */
    private void insertNewHouse(House house) {
        try {
            Timestamp acquireTime = house.getAcquiredTime() == null
                ? new Timestamp(System.currentTimeMillis())
                : house.getAcquiredTime();
            int feePaid = house.isFeePaid() ? 1 : 0;
            byte[] signNotice = house.getSignNotice();

            jdbcClient.sql(ADD_HOUSE_QUERY)
                .param(house.getObjectId())
                .param(house.getAddress().getId())
                .param(house.getBuilding().getId())
                .param(house.getOwnerId())
                .param(acquireTime)
                .param(house.getPermissions())
                .param(house.getStatus().toString())
                .param(feePaid)
                .param(house.getNextPay())
                .param(house.getSellStarted())
                .param(signNotice != null && signNotice.length > 0 ? signNotice : null)
                .update();
        } catch (Exception e) {
            log.error(I18n.get("log.4abe7981971b", house.getObjectId()), e);
        }
    }

    /**
     * 更新已有房屋记录。
     * Updates an existing house record.
     * @param house 房屋 / house
     */
    private void updateHouse(House house) {
        try {
            int feePaid = house.isFeePaid() ? 1 : 0;
            byte[] signNotice = house.getSignNotice();

            jdbcClient.sql(UPDATE_HOUSE_QUERY)
                .param(house.getBuilding().getId())
                .param(house.getOwnerId())
                .param(house.getAcquiredTime())
                .param(house.getPermissions())
                .param(house.getStatus().toString())
                .param(feePaid)
                .param(house.getNextPay())
                .param(house.getSellStarted())
                .param(signNotice != null && signNotice.length > 0 ? signNotice : null)
                .param(house.getObjectId())
                .update();
        } catch (Exception e) {
            log.error(I18n.get("log.340314d885f4", house.getObjectId()), e);
        }
    }

    /**
     * 从数据库加载房屋（普通房或工作室）。
     * Loads houses from the database (regular houses or studios).
     * @param lands 地块模板集合 / housing land templates
     * @param studios 是否加载工作室 / whether to load studios
     * @return 房屋地图 / house map
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

        Map<Integer, Integer> addressHouseIds = new HashMap<>();
        String query = studios ? SELECT_STUDIOS_QUERY : SELECT_HOUSES_QUERY;

        try {
            jdbcClient.sql(query)
                .query((rset, rowNum) -> {
                    int houseId = rset.getInt("id");
                    int buildingId = rset.getInt("building_id");
                    int addressId = rset.getInt("address");

                    HouseAddress address = addressesById.get(addressId);
                    if (address == null) {
                        log.warn(I18n.get("log.32a2bfc0a656", addressId));
                        return null;
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
                        return null;
                    }

                    if (addressHouseIds.containsKey(address.getId())) {
                        log.warn(I18n.get("log.28a74968b3a8", address.getId()));
                        return null;
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
                    } catch (Exception ignored) {
                    }

                    int id = studios ? house.getOwnerId() : address.getId();
                    houses.put(id, house);
                    return null;
                })
                .list();
        } catch (Exception e) {
            log.error(I18n.get("log.423d09af3ba2"), e);
        }

        return houses;
    }

    /**
     * 删除指定玩家的房屋记录。
     * Deletes house records for the given player.
     * @param playerId 玩家 ID / player id
     */
    @Override
    public void deleteHouse(int playerId) {
        try {
            jdbcClient.sql(DELETE_HOUSE_QUERY)
                .param(playerId)
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.f2facfdc760c", playerId), e);
        }
    }
}
