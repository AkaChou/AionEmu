package com.aionemu.commons.configs;

import com.aionemu.commons.configuration.Property;
import java.io.File;

public class DatabaseConfig {
    
    @Property(key = "database.driver", defaultValue = "com.mysql.cj.jdbc.Driver")
    public static String DATABASE_DRIVER;
    
    @Property(key = "database.url", defaultValue = "jdbc:mysql://localhost:3306/aion?useUnicode=true&characterEncoding=UTF-8&useSSL=false")
    public static String DATABASE_URL;
    
    @Property(key = "database.user", defaultValue = "root")
    public static String DATABASE_USER;
    
    @Property(key = "database.password", defaultValue = "root")
    public static String DATABASE_PASSWORD;
    
    @Property(key = "database.maxconnections", defaultValue = "20")
    public static int DATABASE_MAXCONNECTIONS;
    
    @Property(key = "database.scriptcontext.descriptor", defaultValue = "./data/scripts/system/database.xml")
    public static File DATABASE_SCRIPTCONTEXT_DESCRIPTOR;
}