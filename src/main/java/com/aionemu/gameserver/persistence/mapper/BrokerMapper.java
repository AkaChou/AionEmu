package com.aionemu.gameserver.persistence.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Select;

import com.aionemu.gameserver.persistence.entity.BrokerPo;
import com.aionemu.gameserver.persistence.entity.InventoryPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 broker 的映射层。
 * Mapper for table broker.
 *
 * <p>交易所托管物品读 inventory、已用 ID 读 players 均跨表（归属其它闭包），保留注解 SQL；
 * 条目插入/删除/更新经 storeInTransaction 的原生 PreparedStatement 执行，故保留 ? 原文。
 * Broker inventory reads inventory and used ids read players, both foreign tables owned by other
 * closures, so they stay as annotated SQL; entry insert/delete/update run through the raw
 * PreparedStatement inside storeInTransaction, so `?` is kept.
 */
public interface BrokerMapper extends BaseMapper<BrokerPo> {

	/**
	 * 读取交易所位置（item_location=126）的托管物品（原 SELECT_INVENTORY_QUERY）。
	 * Loads broker-held inventory items (location=126) (formerly SELECT_INVENTORY_QUERY).
	 */
	@Select("SELECT * FROM inventory WHERE `item_location` = 126")
	List<InventoryPo> selectBrokerItems();

	/**
	 * 返回已使用的玩家 ID（原 getUsedIDs 内联 SQL）。
	 * Returns used player ids (formerly the inline SQL in getUsedIDs).
	 */
	@Select("SELECT id FROM players")
	List<Integer> selectPlayerIds();

	/**
	 * 插入交易所条目；经 storeInTransaction 的原生 PreparedStatement 执行，故保留 ? 原文。
	 * Insert statement; executed by raw PreparedStatement inside storeInTransaction, so `?` is kept.
	 */
	String INSERT_BROKER_QUERY_JDBC = "INSERT INTO `broker` (`item_pointer`, `item_id`, `item_count`, "
		+ "`item_creator`, `seller`, `price`, `broker_race`, `expire_time`, `settle_time`, `seller_id`, "
		+ "`is_sold`, `is_settled`, `is_splitsell`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	/**
	 * 删除交易所条目；经 storeInTransaction 的原生 PreparedStatement 执行，故保留 ? 原文。
	 * Delete statement; executed by raw PreparedStatement inside storeInTransaction, so `?` is kept.
	 */
	String DELETE_BROKER_QUERY_JDBC = "DELETE FROM `broker` "
		+ "WHERE `item_pointer` = ? AND `seller_id` = ? AND `expire_time` = ?";

	/**
	 * 更新出售/结算状态；经 storeInTransaction 的原生 PreparedStatement 执行，故保留 ? 原文。
	 * Sold/settled update; executed by raw PreparedStatement inside storeInTransaction, so `?` is kept.
	 */
	String UPDATE_BROKER_QUERY_JDBC = "UPDATE broker SET `is_sold` = ?, `is_settled` = 1, `settle_time` = ? "
		+ "WHERE `item_pointer` = ? AND `expire_time` = ? AND `seller_id` = ? AND `is_settled` = 0";

	/**
	 * 更新数量/价格等字段；经 storeInTransaction 的原生 PreparedStatement 执行，故保留 ? 原文。
	 * Item detail update; executed by raw PreparedStatement inside storeInTransaction, so `?` is kept.
	 */
	String UPDATE_ITEM_QUERY_JDBC = "UPDATE broker SET `item_count` = ?, `price` = ?, `is_sold` = ?, "
		+ "`is_settled` = ?, `settle_time` = ?, `is_splitsell` = ? "
		+ "WHERE `item_pointer` = ? AND `expire_time` = ? AND `seller_id` = ? AND `is_settled` = 0";
}
