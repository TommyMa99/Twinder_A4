import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.*;

public class DatabaseConnectionPool {
    private static final String JDBC_URL = "jdbc:mysql://twinder-mysql-db.cswcvfjifpvk.us-east-2.rds.amazonaws.com/swipeInfoDB";
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "password";
    private static final int MAX_TOTAL_CONNECTIONS = 100;

    private static BasicDataSource dataSource;

    public static synchronized Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.jdbc.Driver");
        if (dataSource == null) {
            // Create the connection pool
            dataSource = new BasicDataSource();
            dataSource.setUrl(JDBC_URL);
            dataSource.setUsername(USERNAME);
            dataSource.setPassword(PASSWORD);
            dataSource.setMaxTotal(MAX_TOTAL_CONNECTIONS);
        }

        // Get a connection from the pool
        return dataSource.getConnection();
    }

    public static void closeConnection(Connection conn) throws SQLException {
        if (conn != null) {
            conn.commit();
            conn.close();
        }
    }
}
