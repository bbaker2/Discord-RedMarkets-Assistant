package com.bbaker.discord.redmarket.db;

import java.util.Properties;
import java.util.regex.Pattern;

import org.h2.util.StringUtils;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.h2.H2DatabasePlugin;

import com.bbaker.discord.redmarket.exceptions.SetupException;

public class DatabaseServiceImpl implements DatabaseService  {

    protected Jdbi jdbi;
    private Properties dbProps;
    private String prefix;

    public DatabaseServiceImpl(Properties p) throws SetupException {
        // provide the defaults
    	dbProps = new Properties();
        dbProps.put("url", 		"jdbc:h2:./discord_redmarket");
        dbProps.put("port", 	"9138");
        dbProps.put("password", "gna7");
        dbProps.put("user", 	"su");
        dbProps.put("prefix", 	"rm_");

        // Merge in any values who have the "db." prefix
        for(Object key : p.keySet()) {
        	if(key instanceof String) {
        		String strKey = (String)key;
        		if(strKey.startsWith("db.")) {
        			dbProps.put(strKey.substring(3), p.get(key));
        		}
        	}
        }

        prefix = dbProps.getProperty("prefix");

        if(StringUtils.isNullOrEmpty(prefix)) {
            throw new SetupException("Missing the prefix for the database tables. Make sure 'prefix' is correctly populated in the properties.");
        }

        if(!Pattern.matches("[a-zA-Z]+_", prefix)) {
            throw new SetupException("'%s' is not an acceptable prefix. Must only contain characters and ends with one underscore", prefix);
        }

        this.jdbi = Jdbi.create(dbProps.getProperty("url"), dbProps);
        this.jdbi.installPlugin(new H2DatabasePlugin());
    }

    /* (non-Javadoc)
	 * @see com.bbaker.discord.redmarket.db.DatabaseService#hasTable(java.lang.String)
	 */
    @Override
	public boolean hasTable(String tableName) {
        String query = 	"select count(ID) "+
                        "from INFORMATION_SCHEMA.TABLES "+
                        "where TABLE_TYPE='TABLE' "+
                            "and TABLE_NAME = :tableName";

        return jdbi.withHandle(handle ->
            handle.createQuery(query)
                .bind("tableName", qualifiedName(tableName))
                .mapTo(int.class)
                .findOnly().intValue() > 0
        );
    }

    /* (non-Javadoc)
	 * @see com.bbaker.discord.redmarket.db.DatabaseService#qualifiedName(java.lang.String)
	 */
    @Override
	public String qualifiedName(String name) {
        return (prefix + name).toUpperCase();
    }

    /* (non-Javadoc)
	 * @see com.bbaker.discord.redmarket.db.DatabaseService#query(java.lang.String, java.lang.String, java.lang.String)
	 */
    @Override
	public String query(String template, String tableName, String... extraArgs) {
        if(extraArgs.length > 0) {
            Object[] args = new Object[1+extraArgs.length];
            args[0] = qualifiedName(tableName);
            System.arraycopy(extraArgs, 0, args, 1, extraArgs.length);
            return String.format(template, args);
        } else {
            return String.format(template, qualifiedName(tableName));
        }
    }

}
