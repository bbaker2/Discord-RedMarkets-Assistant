package com.bbaker.discord.redmarket.db;

public interface DatabaseService {

	public boolean hasTable(String tableName);

	public String qualifiedName(String name);

	public String query(String template, String tableName, String... extraArgs);

}