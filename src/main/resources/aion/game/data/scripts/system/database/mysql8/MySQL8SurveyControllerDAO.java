package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.SurveyControllerDAO;
import com.aionemu.gameserver.model.templates.survey.SurveyItem;
import javolution.util.FastList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author KID
 * Updated for MySQL 8 - Fixed connection leaks
 */
public class MySQL8SurveyControllerDAO extends SurveyControllerDAO {

	private static final Logger log = LoggerFactory.getLogger(MySQL8SurveyControllerDAO.class);
	
	private static final String UPDATE_QUERY = "UPDATE `surveys` SET `used`=?, used_time=NOW() WHERE `unique_id`=?";
	private static final String SELECT_QUERY = "SELECT * FROM `surveys` WHERE `used`=?";

	@Override
	public boolean supports(String arg0, int arg1, int arg2) {
		return MySQL8DAOUtils.supports(arg0, arg1, arg2);
	}

	@Override
	public FastList<SurveyItem> getAllNew() {
		FastList<SurveyItem> list = FastList.newInstance();
		
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {
			
			stmt.setInt(1, 0);
			
			try (ResultSet rset = stmt.executeQuery()) {
				while (rset.next()) {
					SurveyItem item = new SurveyItem();
					item.uniqueId = rset.getInt("unique_id");
					item.ownerId = rset.getInt("owner_id");
					item.itemId = rset.getInt("item_id");
					item.count = rset.getLong("item_count");
					item.html = rset.getString("html_text");
					item.radio = rset.getString("html_radio");
					list.add(item);
				}
			}
		} catch (Exception e) {
			log.warn("getAllNew() from DB", e);
		}
		return list;
	}

	@Override
	public boolean useItem(int id) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
			
			stmt.setInt(1, 1);
			stmt.setInt(2, id);
			return stmt.executeUpdate() > 0;
		} catch (Exception e) {
			log.error("useItem", e);
			return false;
		}
	}
}