package com.bbaker.discord.redmarket.commands.negotiation;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class TrackerMapper implements RowMapper<Tracker> {

    @Override
    public Tracker map(ResultSet rs, StatementContext ctx) throws SQLException {
        return new Tracker(
            rs.getInt("provider"),
            rs.getInt("client"),
            rs.getInt("total"),
            rs.getInt("round"),
            rs.getInt("sway_client"),
            rs.getInt("sway_provider"),
            rs.getBoolean("is_secret")
        );
    }

}
