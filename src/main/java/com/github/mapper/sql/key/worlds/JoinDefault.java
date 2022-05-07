package com.github.mapper.sql.key.worlds;

import com.github.mapper.sql.*;

import java.util.Objects;

public class JoinDefault extends KeyWorld implements Join {

    private static final String JOIN = "inner join %s on %s = %s";

    private static final String JOIN_WITH_ALIAS = "inner join %s as %s on %s = %s";

    private final String operator;

    private final QueryContext queryContext;

    public JoinDefault(String tableName, String leftColName, String rightColName, QueryContext queryContext) {
        this.operator = String.format(JOIN, tableName, leftColName, rightColName);
        this.queryContext = queryContext;
    }

    public JoinDefault(Class<?> toJoin, Class<?> toTable, String columnToJoin, String columnToTable, QueryContext queryContext) {
        this.queryContext = queryContext;
        this.queryContext.addTable(toJoin, toTable);
        QueryContext.Table toJoinTbl = this.queryContext.getTable(toJoin);
        var toJoinTblName = toJoinTbl.getName();
        var toJoinTblAlias = toJoinTbl.getAlias();
        QueryContext.Table toTbl = this.queryContext.getTable(toTable);
        var toTblAlias = toTbl.getAlias();
        this.operator = String.format(
                JOIN_WITH_ALIAS,
                toJoinTblName,
                toJoinTblAlias,
                String.format("%s.%s", toJoinTblAlias, columnToJoin),
                String.format("%s.%s", toTblAlias, columnToTable)
        );
    }

    @Override
    public Join join(String tableName, ColumnName leftCol, ColumnName rightCol) {
        this.next = new JoinDefault(tableName, leftCol.get(), rightCol.get(), this.queryContext);
        this.next.prev = this;
        return (JoinDefault) this.next;
    }

    @Override
    public Join join(String tableName, String leftCol, String rightCol) {
        this.next = new JoinDefault(tableName, leftCol, rightCol, this.queryContext);
        this.next.prev = this;
        return (JoinDefault) this.next;
    }

    @Override
    public Join join(Class<?> toTable, Class<?> fromTable, String to, String from) {
        this.next = new JoinDefault(toTable, fromTable,to, from, this.queryContext);
        this.next.prev = this;
        return (JoinDefault) this.next;
    }

    @Override
    public LeftJoin leftJoin(String tableName, ColumnName leftCol, ColumnName rightCol) {
        this.next = new LeftJoinDefault(tableName, leftCol.get(), rightCol.get(), this.queryContext);
        this.next.prev = this;
        return (LeftJoinDefault) this.next;
    }

    @Override
    public LeftJoin leftJoin(String tableName, String leftCol, String rightCol) {
        this.next = new LeftJoinDefault(tableName, leftCol, rightCol, this.queryContext);
        this.next.prev = this;
        return (LeftJoinDefault) this.next;
    }

    @Override
    public LeftJoin leftJoin(Class<?> toTable, Class<?> fromTable, String to, String from) {
        this.next = new LeftJoinDefault(toTable, fromTable, to, from, this.queryContext);
        this.next.prev = this;
        return (LeftJoinDefault) this.next;
    }

    @Override
    public Where where(SQLCondition condition) {
        this.next = new WhereDefault(condition, this.queryContext);
        this.next.prev = this;
        return (WhereDefault) this.next;
    }

    @Override
    public SQLSelect toSelect() {
        return this::asString;
    }

    @Override
    public ReactiveSelect toReactiveSelect() {
        return new ReactiveSelectDefault() {
            @Override
            protected KeyWorld collect() {
                return JoinDefault.this.toFirst();
            }

            @Override
            protected QueryContext context() {
                return JoinDefault.this.queryContext;
            }
        };
    }

    @Override
    public String asString() {
        if (Objects.nonNull(this.prev)) {
            KeyWorld tmp = this.prev;
            this.prev = null;
            return tmp.asString();
        }
        return this.operator;
    }

    @Override
    public String getText() {
        return this.asString();
    }

    @Override
    public KeyWorld toFirst() {
        if (Objects.nonNull(this.prev)) {
            KeyWorld tmp = this.prev;
            this.prev = null;
            return tmp.toFirst();
        }
        return this;
    }

}
