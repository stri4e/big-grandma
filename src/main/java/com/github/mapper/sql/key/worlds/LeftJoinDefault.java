package com.github.mapper.sql.key.worlds;

import com.github.mapper.sql.ColumnName;
import com.github.mapper.sql.SQLCondition;
import com.github.mapper.sql.SQLSelect;
import com.github.mapper.utils.MapperUtils;

import java.util.Objects;

public class LeftJoinDefault extends KeyWorld implements LeftJoin {


    private static final String LEFT_JOIN = "left join %s on %s = %s";

    private final String operator;

    public LeftJoinDefault(String tableName, String leftCol, String rightCol) {
        this.operator = String.format(LEFT_JOIN, tableName, leftCol, rightCol);
    }

    public LeftJoinDefault(Class<?> toTable, Class<?> fromTable, String to, String from) {
        var toTableName = MapperUtils.findTableName(toTable);
        var fromTableName = MapperUtils.findTableName(fromTable);
        this.operator = String.format(
                LEFT_JOIN,
                fromTableName,
                String.format("%s.%s", toTableName, to),
                String.format("%s.%s", fromTableName, from)
        );
    }

    public LeftJoinDefault(String toTable, Class<?> fromTable, String to, String from) {
        var fromTableName = MapperUtils.findTableName(fromTable);
        this.operator = String.format(
                LEFT_JOIN,
                fromTableName,
                String.format("%s.%s", toTable, to),
                String.format("%s.%s", fromTableName, from)
        );
    }

    @Override
    public LeftJoin leftJoin(String tableName, ColumnName leftCol, ColumnName rightCol) {
        this.next = new LeftJoinDefault(tableName, leftCol.get(), rightCol.get());
        this.next.prev = this;
        return (LeftJoinDefault) this.next;
    }

    @Override
    public LeftJoin leftJoin(String tableName, String leftCol, String rightCol) {
        this.next = new LeftJoinDefault(tableName, leftCol, rightCol);
        this.next.prev = this;
        return (LeftJoinDefault) this.next;
    }

    @Override
    public LeftJoin leftJoin(Class<?> toTable, Class<?> fromTable, String to, String from) {
        this.next = new LeftJoinDefault(toTable, fromTable, to, from);
        this.next.prev = this;
        return (LeftJoinDefault) this.next;
    }

    @Override
    public Join join(String tableName, ColumnName leftCol, ColumnName rightCol) {
        this.next = new JoinDefault(tableName, leftCol.get(), rightCol.get());
        this.next.prev = this;
        return (JoinDefault) this.next;
    }

    @Override
    public Join join(String tableName, String leftCol, String rightCol) {
        this.next = new JoinDefault(tableName, leftCol, rightCol);
        this.next.prev = this;
        return (JoinDefault) this.next;
    }

    @Override
    public Join join(Class<?> toTable, Class<?> fromTable, String to, String from) {
        this.next = new JoinDefault(toTable, fromTable, to, from);
        this.next.prev = this;
        return (JoinDefault) this.next;
    }

    @Override
    public Where where(SQLCondition condition) {
        this.next = new WhereDefault(condition);
        this.next.prev = this;
        return (WhereDefault) this.next;
    }

    @Override
    public SQLSelect toSelect() {
        return this::asString;
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

}