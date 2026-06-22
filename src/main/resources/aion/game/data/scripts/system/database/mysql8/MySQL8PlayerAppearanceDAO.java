package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.PlayerAppearanceDAO;
import com.aionemu.gameserver.model.gameobjects.player.PlayerAppearance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MySQL8PlayerAppearanceDAO extends PlayerAppearanceDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8PlayerAppearanceDAO.class);
    
    private static final String SELECT_QUERY = "SELECT * FROM player_appearance WHERE player_id = ?";
    private static final String REPLACE_QUERY = "REPLACE INTO player_appearance (" + "player_id, voice, skin_rgb, hair_rgb, eye_rgb, lip_rgb, face, hair, deco, tattoo, " + "face_contour, expression, pupil_shape, remove_mane, right_eye_rgb, eye_lash_shape, " + "jaw_line, forehead, eye_height, eye_space, eye_width, eye_size, eye_shape, eye_angle, " + "brow_height, brow_angle, brow_shape, nose, nose_bridge, nose_width, nose_tip, " + "cheek, lip_height, mouth_size, lip_size, smile, lip_shape, jaw_height, chin_jut, " + "ear_shape, head_size, neck, neck_length, shoulder_size, torso, chest, waist, hips, " + "arm_thickness, hand_size, leg_thickness, facial_rate, foot_size, arm_length, leg_length, " + "shoulders, face_shape, pupil_size, upper_torso, fore_arm_thickness, hand_span, " + "calf_thickness, height) VALUES (" + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " + "?, ?)";
    
    @Override
    public PlayerAppearance load(final int playerId) {
        PlayerAppearance pa = new PlayerAppearance();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement statement = con.prepareStatement(SELECT_QUERY)) {
            
            statement.setInt(1, playerId);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
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
                }
            }
        } catch (SQLException e) {
            log.error("Could not restore PlayerAppearance data for player {} from DB", playerId, e);
            return null;
        }
        return pa;
    }
    
    @Override
    public boolean store(final int id, final PlayerAppearance pa) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(REPLACE_QUERY)) {
            
            log.debug("[DAO: MySQL8PlayerAppearanceDAO] storing appearance {}", id);
            
            int paramIndex = 1;
            ps.setInt(paramIndex++, id);                    // 1  - player_id
            ps.setInt(paramIndex++, pa.getVoice());         // 2  - voice
            ps.setInt(paramIndex++, pa.getSkinRGB());       // 3  - skin_rgb
            ps.setInt(paramIndex++, pa.getHairRGB());       // 4  - hair_rgb
            ps.setInt(paramIndex++, pa.getEyeRGB());        // 5  - eye_rgb
            ps.setInt(paramIndex++, pa.getLipRGB());        // 6  - lip_rgb
            ps.setInt(paramIndex++, pa.getFace());          // 7  - face
            ps.setInt(paramIndex++, pa.getHair());          // 8  - hair
            ps.setInt(paramIndex++, pa.getDeco());          // 9  - deco
            ps.setInt(paramIndex++, pa.getTattoo());        // 10 - tattoo
            ps.setInt(paramIndex++, pa.getFaceContour());   // 11 - face_contour
            ps.setInt(paramIndex++, pa.getExpression());    // 12 - expression
            ps.setInt(paramIndex++, pa.getPupilShape());    // 13 - pupil_shape
            ps.setInt(paramIndex++, pa.getRemoveMane());    // 14 - remove_mane
            ps.setInt(paramIndex++, pa.getRightEyeRGB());   // 15 - right_eye_rgb
            ps.setInt(paramIndex++, pa.getEyeLashShape());  // 16 - eye_lash_shape
            ps.setInt(paramIndex++, pa.getJawLine());       // 17 - jaw_line
            ps.setInt(paramIndex++, pa.getForehead());      // 18 - forehead
            ps.setInt(paramIndex++, pa.getEyeHeight());     // 19 - eye_height
            ps.setInt(paramIndex++, pa.getEyeSpace());      // 20 - eye_space
            ps.setInt(paramIndex++, pa.getEyeWidth());      // 21 - eye_width
            ps.setInt(paramIndex++, pa.getEyeSize());       // 22 - eye_size
            ps.setInt(paramIndex++, pa.getEyeShape());      // 23 - eye_shape
            ps.setInt(paramIndex++, pa.getEyeAngle());      // 24 - eye_angle
            ps.setInt(paramIndex++, pa.getBrowHeight());    // 25 - brow_height
            ps.setInt(paramIndex++, pa.getBrowAngle());     // 26 - brow_angle
            ps.setInt(paramIndex++, pa.getBrowShape());     // 27 - brow_shape
            ps.setInt(paramIndex++, pa.getNose());          // 28 - nose
            ps.setInt(paramIndex++, pa.getNoseBridge());    // 29 - nose_bridge
            ps.setInt(paramIndex++, pa.getNoseWidth());     // 30 - nose_width
            ps.setInt(paramIndex++, pa.getNoseTip());       // 31 - nose_tip
            ps.setInt(paramIndex++, pa.getCheek());         // 32 - cheek
            ps.setInt(paramIndex++, pa.getLipHeight());     // 33 - lip_height
            ps.setInt(paramIndex++, pa.getMouthSize());     // 34 - mouth_size
            ps.setInt(paramIndex++, pa.getLipSize());       // 35 - lip_size
            ps.setInt(paramIndex++, pa.getSmile());         // 36 - smile
            ps.setInt(paramIndex++, pa.getLipShape());      // 37 - lip_shape
            ps.setInt(paramIndex++, pa.getJawHeigh());      // 38 - jaw_height
            ps.setInt(paramIndex++, pa.getChinJut());       // 39 - chin_jut
            ps.setInt(paramIndex++, pa.getEarShape());      // 40 - ear_shape
            ps.setInt(paramIndex++, pa.getHeadSize());      // 41 - head_size
            ps.setInt(paramIndex++, pa.getNeck());          // 42 - neck
            ps.setInt(paramIndex++, pa.getNeckLength());    // 43 - neck_length
            ps.setInt(paramIndex++, pa.getShoulderSize());  // 44 - shoulder_size
            ps.setInt(paramIndex++, pa.getTorso());         // 45 - torso
            ps.setInt(paramIndex++, pa.getChest());         // 46 - chest
            ps.setInt(paramIndex++, pa.getWaist());         // 47 - waist
            ps.setInt(paramIndex++, pa.getHips());          // 48 - hips
            ps.setInt(paramIndex++, pa.getArmThickness());  // 49 - arm_thickness
            ps.setInt(paramIndex++, pa.getHandSize());      // 50 - hand_size
            ps.setInt(paramIndex++, pa.getLegThickness());  // 51 - leg_thickness
            ps.setInt(paramIndex++, pa.getFacialRate());    // 52 - facial_rate
            ps.setInt(paramIndex++, pa.getFootSize());      // 53 - foot_size
            ps.setInt(paramIndex++, pa.getArmLength());     // 54 - arm_length
            ps.setInt(paramIndex++, pa.getLegLength());     // 55 - leg_length
            ps.setInt(paramIndex++, pa.getShoulders());     // 56 - shoulders
            ps.setInt(paramIndex++, pa.getFaceShape());     // 57 - face_shape
            ps.setInt(paramIndex++, pa.getPupilSize());     // 58 - pupil_size
            ps.setInt(paramIndex++, pa.getUpperTorso());    // 59 - upper_torso
            ps.setInt(paramIndex++, pa.getForeArmThickness()); // 60 - fore_arm_thickness
            ps.setInt(paramIndex++, pa.getHandSpan());      // 61 - hand_span
            ps.setInt(paramIndex++, pa.getCalfThickness()); // 62 - calf_thickness
            ps.setFloat(paramIndex, pa.getHeight());        // 63 - height
            
            ps.executeUpdate();
            return true;
            
        } catch (SQLException e) {
            log.error("Could not store PlayerAppearance data for player {}", id, e);
            return false;
        }
    }
    
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}