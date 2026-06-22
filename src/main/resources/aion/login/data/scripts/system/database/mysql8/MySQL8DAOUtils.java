package mysql8;

/**
 * DAO utils for MySQL 8
 * 
 * @author Updated for MySQL 8
 */
public class MySQL8DAOUtils {

    public static final String MYSQL_DB_NAME = "MySQL";

    public static boolean supports(String db, int majorVersion, int minorVersion) {
        return MYSQL_DB_NAME.equals(db) && majorVersion >= 8;
    }
}