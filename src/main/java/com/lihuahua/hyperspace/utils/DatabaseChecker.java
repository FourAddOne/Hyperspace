package com.lihuahua.hyperspace.utils;

import java.sql.*;

public class DatabaseChecker {
    private static final String URL = "jdbc:mysql://localhost:3306/hyperspace";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "84519597";

    public static void main(String[] args) {
        try {
            // 加载MySQL驱动
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // 建立连接
            Connection connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            
            // 检查messages表结构
            checkMessagesTable(connection);
            
            // 关闭连接
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void checkMessagesTable(Connection connection) throws SQLException {
        System.out.println("检查messages表结构:");
        
        DatabaseMetaData metaData = connection.getMetaData();
        ResultSet columns = metaData.getColumns(null, null, "messages", null);
        
        System.out.println("表字段信息:");
        while (columns.next()) {
            String columnName = columns.getString("COLUMN_NAME");
            String typeName = columns.getString("TYPE_NAME");
            String isNullable = columns.getString("IS_NULLABLE");
            String columnDef = columns.getString("COLUMN_DEF");
            
            System.out.println("  " + columnName + " (" + typeName + ", nullable: " + isNullable + 
                             (columnDef != null ? ", default: " + columnDef : "") + ")");
        }
        
        columns.close();
        
        // 尝试执行插入操作
        System.out.println("\n尝试执行插入操作:");
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO messages (message_id, sender_id, receiver_id, content, status, timestamp) VALUES (?, ?, ?, ?, ?, ?)");
            stmt.setString(1, "test_id");
            stmt.setString(2, "sender_test");
            stmt.setString(3, "receiver_test");
            stmt.setString(4, "test content");
            stmt.setString(5, "sent");
            stmt.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
            
            int result = stmt.executeUpdate();
            System.out.println("插入操作结果: " + result);
            
            // 回滚测试数据
            stmt = connection.prepareStatement("DELETE FROM messages WHERE message_id = ?");
            stmt.setString(1, "test_id");
            stmt.executeUpdate();
            
            stmt.close();
        } catch (SQLException e) {
            System.err.println("插入操作失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}