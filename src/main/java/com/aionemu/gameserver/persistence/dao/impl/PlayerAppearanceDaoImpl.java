package com.aionemu.gameserver.persistence.dao.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.player.PlayerAppearance;
import com.aionemu.gameserver.persistence.dao.PlayerAppearanceDao;
import com.aionemu.gameserver.persistence.entity.PlayerAppearancePo;
import com.aionemu.gameserver.persistence.mapper.PlayerAppearanceMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家外观（捏脸/体型）数据访问实现。
 * Player appearance (face/body) data access implementation.
 */
@Slf4j
@Service
@Lazy
public class PlayerAppearanceDaoImpl extends ServiceImpl<PlayerAppearanceMapper, PlayerAppearancePo>
	implements PlayerAppearanceDao {

	/**
	 * 加载指定玩家的外观数据。
	 * Loads the appearance data for the given player.
	 * @param playerId 玩家 ID / player id
	 * @return 玩家外观；加载失败返回 null / player appearance, or null on failure
	 */
	@Override
	public PlayerAppearance load(final int playerId) {
		try {
			PlayerAppearancePo po = mapper.selectOneById(playerId);
			return po == null ? null : toAppearance(po);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.5f83ae225558", playerId), e);
			return null;
		}
	}

	/**
	 * 持久化玩家外观数据。
	 * Persists the player's appearance data.
	 * @param id 玩家 ID / player id
	 * @param pa 外观数据 / appearance data
	 * @return 是否成功 / whether successful
	 */
	@Override
	public boolean store(final int id, final PlayerAppearance pa) {
		try {
			log.debug("[DAO: PlayerAppearanceDao] storing appearance {}", id);

			mapper.replaceAppearance(toPo(id, pa));
			return true;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.915757e82ea4", id), e);
			return false;
		}
	}

	/**
	 * 将外观模型转换为持久化实体。
	 * Converts the appearance model into its persistence entity.
	 *
	 * @param id 玩家 ID / player id
	 * @param pa 外观数据 / appearance data
	 * @return 外观实体 / appearance entity
	 */
	private static PlayerAppearancePo toPo(int id, PlayerAppearance pa) {
		PlayerAppearancePo po = new PlayerAppearancePo();
		po.setPlayerId(id);
		po.setVoice(pa.getVoice());
		po.setSkinRgb(pa.getSkinRGB());
		po.setHairRgb(pa.getHairRGB());
		po.setEyeRgb(pa.getEyeRGB());
		po.setLipRgb(pa.getLipRGB());
		po.setFace(pa.getFace());
		po.setHair(pa.getHair());
		po.setDeco(pa.getDeco());
		po.setTattoo(pa.getTattoo());
		po.setFaceContour(pa.getFaceContour());
		po.setExpression(pa.getExpression());
		po.setPupilShape(pa.getPupilShape());
		po.setRemoveMane(pa.getRemoveMane());
		po.setRightEyeRgb(pa.getRightEyeRGB());
		po.setEyeLashShape(pa.getEyeLashShape());
		po.setJawLine(pa.getJawLine());
		po.setForehead(pa.getForehead());
		po.setEyeHeight(pa.getEyeHeight());
		po.setEyeSpace(pa.getEyeSpace());
		po.setEyeWidth(pa.getEyeWidth());
		po.setEyeSize(pa.getEyeSize());
		po.setEyeShape(pa.getEyeShape());
		po.setEyeAngle(pa.getEyeAngle());
		po.setBrowHeight(pa.getBrowHeight());
		po.setBrowAngle(pa.getBrowAngle());
		po.setBrowShape(pa.getBrowShape());
		po.setNose(pa.getNose());
		po.setNoseBridge(pa.getNoseBridge());
		po.setNoseWidth(pa.getNoseWidth());
		po.setNoseTip(pa.getNoseTip());
		po.setCheek(pa.getCheek());
		po.setLipHeight(pa.getLipHeight());
		po.setMouthSize(pa.getMouthSize());
		po.setLipSize(pa.getLipSize());
		po.setSmile(pa.getSmile());
		po.setLipShape(pa.getLipShape());
		po.setJawHeight(pa.getJawHeigh());
		po.setChinJut(pa.getChinJut());
		po.setEarShape(pa.getEarShape());
		po.setHeadSize(pa.getHeadSize());
		po.setNeck(pa.getNeck());
		po.setNeckLength(pa.getNeckLength());
		po.setShoulderSize(pa.getShoulderSize());
		po.setTorso(pa.getTorso());
		po.setChest(pa.getChest());
		po.setWaist(pa.getWaist());
		po.setHips(pa.getHips());
		po.setArmThickness(pa.getArmThickness());
		po.setHandSize(pa.getHandSize());
		po.setLegThickness(pa.getLegThickness());
		po.setFacialRate(pa.getFacialRate());
		po.setFootSize(pa.getFootSize());
		po.setArmLength(pa.getArmLength());
		po.setLegLength(pa.getLegLength());
		po.setShoulders(pa.getShoulders());
		po.setFaceShape(pa.getFaceShape());
		po.setPupilSize(pa.getPupilSize());
		po.setUpperTorso(pa.getUpperTorso());
		po.setForeArmThickness(pa.getForeArmThickness());
		po.setHandSpan(pa.getHandSpan());
		po.setCalfThickness(pa.getCalfThickness());
		po.setHeight(pa.getHeight());
		return po;
	}

	/**
	 * 将持久化实体还原为外观模型。
	 * Restores the appearance model from its persistence entity.
	 *
	 * @param po 外观实体 / appearance entity
	 * @return 外观模型 / appearance model
	 */
	private static PlayerAppearance toAppearance(PlayerAppearancePo po) {
		PlayerAppearance pa = new PlayerAppearance();
		pa.setVoice(po.getVoice());
		pa.setSkinRGB(po.getSkinRgb());
		pa.setHairRGB(po.getHairRgb());
		pa.setEyeRGB(po.getEyeRgb());
		pa.setLipRGB(po.getLipRgb());
		pa.setFace(po.getFace());
		pa.setHair(po.getHair());
		pa.setDeco(po.getDeco());
		pa.setTattoo(po.getTattoo());
		pa.setFaceContour(po.getFaceContour());
		pa.setExpression(po.getExpression());
		pa.setPupilShape(po.getPupilShape());
		pa.setRemoveMane(po.getRemoveMane());
		pa.setRightEyeRGB(po.getRightEyeRgb());
		pa.setEyeLashShape(po.getEyeLashShape());
		pa.setJawLine(po.getJawLine());
		pa.setForehead(po.getForehead());
		pa.setEyeHeight(po.getEyeHeight());
		pa.setEyeSpace(po.getEyeSpace());
		pa.setEyeWidth(po.getEyeWidth());
		pa.setEyeSize(po.getEyeSize());
		pa.setEyeShape(po.getEyeShape());
		pa.setEyeAngle(po.getEyeAngle());
		pa.setBrowHeight(po.getBrowHeight());
		pa.setBrowAngle(po.getBrowAngle());
		pa.setBrowShape(po.getBrowShape());
		pa.setNose(po.getNose());
		pa.setNoseBridge(po.getNoseBridge());
		pa.setNoseWidth(po.getNoseWidth());
		pa.setNoseTip(po.getNoseTip());
		pa.setCheek(po.getCheek());
		pa.setLipHeight(po.getLipHeight());
		pa.setMouthSize(po.getMouthSize());
		pa.setLipSize(po.getLipSize());
		pa.setSmile(po.getSmile());
		pa.setLipShape(po.getLipShape());
		pa.setJawHeigh(po.getJawHeight());
		pa.setChinJut(po.getChinJut());
		pa.setEarShape(po.getEarShape());
		pa.setHeadSize(po.getHeadSize());
		pa.setNeck(po.getNeck());
		pa.setNeckLength(po.getNeckLength());
		pa.setShoulderSize(po.getShoulderSize());
		pa.setTorso(po.getTorso());
		pa.setChest(po.getChest());
		pa.setWaist(po.getWaist());
		pa.setHips(po.getHips());
		pa.setArmThickness(po.getArmThickness());
		pa.setHandSize(po.getHandSize());
		pa.setLegThickness(po.getLegThickness());
		pa.setFacialRate(po.getFacialRate());
		pa.setFootSize(po.getFootSize());
		pa.setArmLength(po.getArmLength());
		pa.setLegLength(po.getLegLength());
		pa.setShoulders(po.getShoulders());
		pa.setFaceShape(po.getFaceShape());
		pa.setPupilSize(po.getPupilSize());
		pa.setUpperTorso(po.getUpperTorso());
		pa.setForeArmThickness(po.getForeArmThickness());
		pa.setHandSpan(po.getHandSpan());
		pa.setCalfThickness(po.getCalfThickness());
		pa.setHeight(po.getHeight());
		return pa;
	}
}
