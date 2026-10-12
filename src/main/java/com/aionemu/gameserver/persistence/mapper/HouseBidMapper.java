package com.aionemu.gameserver.persistence.mapper;

import java.sql.Timestamp;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.aionemu.gameserver.persistence.entity.HouseBidPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 house_bids 的映射层。
 * Mapper for table house_bids.
 */
public interface HouseBidMapper extends BaseMapper<HouseBidPo> {

	/**
	 * 变更玩家对某房屋的出价并返回受影响行数。
	 * Changes a player's bid for a house and returns the affected row count.
	 * <p>
	 * 调用方需按“0 行则补插”处理，而 Flex UpdateChain 只能返回是否执行成功、无法取回受影响行数，
	 * 故此处保留注解 SQL。
	 * The caller needs the affected row count to decide whether to insert; Flex UpdateChain only reports execution
	 * success, so the annotated SQL is kept here.
	 * </p>
	 *
	 * @param playerId    玩家 ID / player ID
	 * @param houseId     房屋 ID / house ID
	 * @param newBidOffer 新出价金额 / new bid amount
	 * @param time        出价时间 / bid time
	 * @return 受影响行数 / affected row count
	 */
	@Update("UPDATE `house_bids` SET `bid` = #{newBidOffer}, `bid_time` = #{time} "
		+ "WHERE `player_id` = #{playerId} AND `house_id` = #{houseId}")
	int changeBid(@Param("playerId") int playerId, @Param("houseId") int houseId,
		@Param("newBidOffer") long newBidOffer, @Param("time") Timestamp time);
}
