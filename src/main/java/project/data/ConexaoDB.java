package project.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexaoDB {

    private static final String URL = "jdbc:mysql://localhost:3307/CadeiaRestaurantesDB?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    
    private static final String USER = "root"; 
    private static final String PASS = "root";   

    // CONEXAO PARA USO DOS DAOS
    public static Connection getConexao() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    // APENAS PARA TESTAR (DEBUG)
    public static boolean testarConexao() {
        try (Connection conn = getConexao()) {
            return true; 
        } catch (SQLException e) {
            System.err.println("❌ Erro na conexão: " + e.getMessage());
            return false;
        }
    }
}