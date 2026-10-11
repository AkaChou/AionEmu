package com.aionemu.gameserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.player.PlayerAppearance;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家外观（捏脸/体型）DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerAppearanceDAO using JdbcClient.
 */
@Slf4j
@Repository
public class PlayerAppearanceDAO extends com.aionemu.gameserver.dao.PlayerAppearanceDAO {

    /** 按玩家 ID 查询外观 / Select appearance by player id */
    private static final String SELECT_QUERY = "SELECT * FROM player_appearance WHERE player_id = ?";
    /** 替换写入玩家外观 / Replace-insert player appearance */
    private static final String REPLACE_QUERY = "REPLACE INTO player_appearance (" + "player_id, voice, skin_rgb, hair_rgb, eye_rgb, lip_rgb, face, hair, deco, tattoo, " + "face_contour, expression, pupil_shape, remove_mane, right_eye_rgb, eye_lash_shape, " + "jaw_line, forehead, eye_height, eye_space, eye_width, eye_size, eye_shape, eye_angle, " + "brow_height, brow_angle, brow_shape, nose, nose_bridge, nose_width, nose_tip, " + "cheek, lip_height, mouth_size, lip_size, smile, lip_shape, jaw_height, chin_jut, " + "ear_shape, head_size, neck, neck_length, shoulder_size, torso, chest, waist, hips, " + "arm_thickness, hand_size, leg_thickness, facial_rate, foot_size, arm_length, leg_length, " + "shoulders, face_shape, pupil_size, upper_torso, fore_arm_thickness, hand_span, " + "calf_thickness, height) VALUES (" + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " + "?, ?)";

    private final JdbcClient jdbcClient;

    public PlayerAppearanceDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    @Autowired

    public PlayerAppearanceDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    /**
     * 加载指定玩家的外观数据。
     * Loads the appearance data for the given player.
     * @param playerId 玩家 ID / player id
     * @return 玩家外观；加载失败返回 null / player appearance, or null on failure
     */
    @Override
    public PlayerAppearance load(final int playerId) {
        try {
            return jdbcClient.sql(SELECT_QUERY)
                .param(playerId)
                .query((resultSet, _) -> {
                    PlayerAppearance pa = new PlayerAppearance();
                    pa.setVoice(resultSet.getInt("voice"));
                    pa.setSkinRGB(resultSet.getInt("skin_rgb"));
                    pa.setHairRGB(resultSet.getInt("hair_rgb"));
                    pa.setEyeRGB(resultSet.getInt("eye_rgb"));
                    pa.setLipRGB(resultSet.getInt("lip_rgb"));
                    pa.setFace(resultSet.getInt("face"));
                    pa.setHair(resultSet.getInt("hair"));
                    pa.setDeco(resultSet.getInt("deco"));
                    pa.setTattoo(resultSet.getInt("tattoo"));
                    pa.setFaceContour(resultSet.getInt("face_contour"));
                    pa.setExpression(resultSet.getInt("expression"));
                    pa.setPupilShape(resultSet.getInt("pupil_shape"));
                    pa.setRemoveMane(resultSet.getInt("remove_mane"));
                    pa.setRightEyeRGB(resultSet.getInt("right_eye_rgb"));
                    pa.setEyeLashShape(resultSet.getInt("eye_lash_shape"));
                    pa.setJawLine(resultSet.getInt("jaw_line"));
                    pa.setForehead(resultSet.getInt("forehead"));
                    pa.setEyeHeight(resultSet.getInt("eye_height"));
                    pa.setEyeSpace(resultSet.getInt("eye_space"));
                    pa.setEyeWidth(resultSet.getInt("eye_width"));
                    pa.setEyeSize(resultSet.getInt("eye_size"));
                    pa.setEyeShape(resultSet.getInt("eye_shape"));
                    pa.setEyeAngle(resultSet.getInt("eye_angle"));
                    pa.setBrowHeight(resultSet.getInt("brow_height"));
                    pa.setBrowAngle(resultSet.getInt("brow_angle"));
                    pa.setBrowShape(resultSet.getInt("brow_shape"));
                    pa.setNose(resultSet.getInt("nose"));
                    pa.setNoseBridge(resultSet.getInt("nose_bridge"));
                    pa.setNoseWidth(resultSet.getInt("nose_width"));
                    pa.setNoseTip(resultSet.getInt("nose_tip"));
                    pa.setCheek(resultSet.getInt("cheek"));
                    pa.setLipHeight(resultSet.getInt("lip_height"));
                    pa.setMouthSize(resultSet.getInt("mouth_size"));
                    pa.setLipSize(resultSet.getInt("lip_size"));
                    pa.setSmile(resultSet.getInt("smile"));
                    pa.setLipShape(resultSet.getInt("lip_shape"));
                    pa.setJawHeigh(resultSet.getInt("jaw_height"));
                    pa.setChinJut(resultSet.getInt("chin_jut"));
                    pa.setEarShape(resultSet.getInt("ear_shape"));
                    pa.setHeadSize(resultSet.getInt("head_size"));
                    pa.setNeck(resultSet.getInt("neck"));
                    pa.setNeckLength(resultSet.getInt("neck_length"));
                    pa.setShoulderSize(resultSet.getInt("shoulder_size"));
                    pa.setTorso(resultSet.getInt("torso"));
                    pa.setChest(resultSet.getInt("chest"));
                    pa.setWaist(resultSet.getInt("waist"));
                    pa.setHips(resultSet.getInt("hips"));
                    pa.setArmThickness(resultSet.getInt("arm_thickness"));
                    pa.setHandSize(resultSet.getInt("hand_size"));
                    pa.setLegThickness(resultSet.getInt("leg_thickness"));
                    pa.setFacialRate(resultSet.getInt("facial_rate"));
                    pa.setFootSize(resultSet.getInt("foot_size"));
                    pa.setArmLength(resultSet.getInt("arm_length"));
                    pa.setLegLength(resultSet.getInt("leg_length"));
                    pa.setShoulders(resultSet.getInt("shoulders"));
                    pa.setFaceShape(resultSet.getInt("face_shape"));
                    pa.setPupilSize(resultSet.getInt("pupil_size"));
                    pa.setUpperTorso(resultSet.getInt("upper_torso"));
                    pa.setForeArmThickness(resultSet.getInt("fore_arm_thickness"));
                    pa.setHandSpan(resultSet.getInt("hand_span"));
                    pa.setCalfThickness(resultSet.getInt("calf_thickness"));
                    pa.setHeight(resultSet.getFloat("height"));
                    return pa;
                })
                .optional()
                .orElse(null);
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
            log.debug("[DAO: PlayerAppearanceDAO] storing appearance {}", id);

            jdbcClient.sql(REPLACE_QUERY)
                .param(id)
                .param(pa.getVoice())
                .param(pa.getSkinRGB())
                .param(pa.getHairRGB())
                .param(pa.getEyeRGB())
                .param(pa.getLipRGB())
                .param(pa.getFace())
                .param(pa.getHair())
                .param(pa.getDeco())
                .param(pa.getTattoo())
                .param(pa.getFaceContour())
                .param(pa.getExpression())
                .param(pa.getPupilShape())
                .param(pa.getRemoveMane())
                .param(pa.getRightEyeRGB())
                .param(pa.getEyeLashShape())
                .param(pa.getJawLine())
                .param(pa.getForehead())
                .param(pa.getEyeHeight())
                .param(pa.getEyeSpace())
                .param(pa.getEyeWidth())
                .param(pa.getEyeSize())
                .param(pa.getEyeShape())
                .param(pa.getEyeAngle())
                .param(pa.getBrowHeight())
                .param(pa.getBrowAngle())
                .param(pa.getBrowShape())
                .param(pa.getNose())
                .param(pa.getNoseBridge())
                .param(pa.getNoseWidth())
                .param(pa.getNoseTip())
                .param(pa.getCheek())
                .param(pa.getLipHeight())
                .param(pa.getMouthSize())
                .param(pa.getLipSize())
                .param(pa.getSmile())
                .param(pa.getLipShape())
                .param(pa.getJawHeigh())
                .param(pa.getChinJut())
                .param(pa.getEarShape())
                .param(pa.getHeadSize())
                .param(pa.getNeck())
                .param(pa.getNeckLength())
                .param(pa.getShoulderSize())
                .param(pa.getTorso())
                .param(pa.getChest())
                .param(pa.getWaist())
                .param(pa.getHips())
                .param(pa.getArmThickness())
                .param(pa.getHandSize())
                .param(pa.getLegThickness())
                .param(pa.getFacialRate())
                .param(pa.getFootSize())
                .param(pa.getArmLength())
                .param(pa.getLegLength())
                .param(pa.getShoulders())
                .param(pa.getFaceShape())
                .param(pa.getPupilSize())
                .param(pa.getUpperTorso())
                .param(pa.getForeArmThickness())
                .param(pa.getHandSpan())
                .param(pa.getCalfThickness())
                .param(pa.getHeight())
                .update();
            return true;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.915757e82ea4", id), e);
            return false;
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
}
