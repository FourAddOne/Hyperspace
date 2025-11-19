import mysql.connector

# 数据库连接配置
config = {
    'user': 'root',
    'password': '84519597',
    'host': 'localhost',
    'database': 'hyperspace',
    'raise_on_warnings': True
}

try:
    # 建立数据库连接
    cnx = mysql.connector.connect(**config)
    cursor = cnx.cursor()
    
    # 检查messages表结构
    cursor.execute("DESCRIBE messages")
    columns = cursor.fetchall()
    
    print("Messages表结构:")
    for column in columns:
        print(f"  {column[0]} {column[1]} {column[2]} {column[3]} {column[4]} {column[5]}")
    
    # 尝试插入一条测试数据
    print("\n尝试插入测试数据:")
    try:
        cursor.execute("""
            INSERT INTO messages 
            (message_id, conversation_id, sender_id, receiver_id, content_type, content, file_url, status, timestamp) 
            VALUES 
            (%s, %s, %s, %s, %s, %s, %s, %s, %s)
        """, ("test_id", None, "sender_test", "receiver_test", "text", "test content", None, "sent", "2025-01-01 00:00:00"))
        
        cnx.commit()
        print("插入成功")
        
        # 删除测试数据
        cursor.execute("DELETE FROM messages WHERE message_id = %s", ("test_id",))
        cnx.commit()
        print("测试数据已删除")
        
    except Exception as e:
        print(f"插入失败: {e}")
    
except Exception as e:
    print(f"数据库连接失败: {e}")
    
finally:
    try:
        cursor.close()
        cnx.close()
    except:
        pass