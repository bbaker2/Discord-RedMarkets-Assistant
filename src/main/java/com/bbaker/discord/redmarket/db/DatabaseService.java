package com.bbaker.discord.redmarket.db;

import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.HandleConsumer;

public interface DatabaseService {

	public boolean hasTable(String tableName);

	public String qualifiedName(String name);

	public String query(String template, String tableName, String... extraArgs);

	public <X extends Exception> void useHandle(final HandleConsumer<X> callback) throws X;

	public <R, X extends Exception> R withHandle(HandleCallback<R, X> callback) throws X;

}