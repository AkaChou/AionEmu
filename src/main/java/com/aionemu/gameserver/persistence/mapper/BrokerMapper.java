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
 * <p>交易所托管物品读 inventory（归属其它闭包），跨本表实体，保留注解 SQL。
 * The broker-held item read spans inventory (owned by another closure), so it stays as annotated SQL.
 */
public interface BrokerMapper extends BaseMapper<BrokerPo> {

	/**
	 * 读取交易所位置（item_location=126）的托管物品（原 SELECT_INVENTORY_QUERY）。
	 * Loads broker-held inventory items (location=126) (formerly SELECT_INVENTORY_QUERY).
	 */
	@Select("SELECT * FROM inventory WHERE `item_location` = 126")
	List<InventoryPo> selectBrokerItems();
}
