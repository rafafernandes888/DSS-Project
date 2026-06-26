package project.data;

import java.sql.*;
import java.util.*;

/**
 * StockDAO refatorado para implementar Map<String, Integer> e Singleton.
 * A chave do Map é uma String no formato "idRestaurante:idIngrediente".
 */
public class StockDAO implements Map<String, Integer> {

    private static StockDAO singleton = null;

    private StockDAO() {}

    public static StockDAO getInstance() {
        if (singleton == null) {
            singleton = new StockDAO();
        }
        return singleton;
    }

    /* --- MÉTODOS DA INTERFACE MAP (Chave: "idRest:idIng") --- */

    @Override
    public int size() {
        int i = 0;
        try (Connection conn = ConexaoDB.getConexao();
             Statement stm = conn.createStatement();
             ResultSet rs = stm.executeQuery("SELECT count(*) FROM Stock")) {
            if(rs.next()) i = rs.getInt(1);
        } catch (SQLException e) { throw new RuntimeException(e); }
        return i;
    }

    @Override
    public boolean containsKey(Object key) {
        String[] ids = key.toString().split(":");
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement pstm = conn.prepareStatement("SELECT 1 FROM Stock WHERE id_restaurante=? AND id_ingrediente=?")) {
            pstm.setInt(1, Integer.parseInt(ids[0]));
            pstm.setInt(2, Integer.parseInt(ids[1]));
            try (ResultSet rs = pstm.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override
    public Integer get(Object key) {
        String[] ids = key.toString().split(":");
        return this.getQuantidade(Integer.parseInt(ids[0]), Integer.parseInt(ids[1]));
    }

    @Override
    public Integer put(String key, Integer value) {
        String[] ids = key.split(":");
        int idRest = Integer.parseInt(ids[0]);
        int idIng = Integer.parseInt(ids[1]);
        
        String sql = "INSERT INTO Stock (id_restaurante, id_ingrediente, quantidade) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE quantidade = VALUES(quantidade)";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idRest);
            stmt.setInt(2, idIng);
            stmt.setInt(3, value);
            stmt.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
        return value;
    }

    /* --- MÉTODOS DE LÓGICA DE NEGÓCIO (Utilizados pelo GestorOperacoes) --- */

    public int getQuantidade(int idRestaurante, int idIngrediente) {
        String sql = "SELECT quantidade FROM Stock WHERE id_restaurante = ? AND id_ingrediente = ?";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idRestaurante);
            stmt.setInt(2, idIngrediente);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("quantidade") : 0;
            }
        } catch (SQLException e) { return 0; }
    }

    public void baixarStock(int idRestaurante, int idIngrediente, int qtd) {
        String sql = "UPDATE Stock SET quantidade = quantidade - ? WHERE id_restaurante = ? AND id_ingrediente = ?";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, qtd);
            stmt.setInt(2, idRestaurante);
            stmt.setInt(3, idIngrediente);
            stmt.executeUpdate();
        } catch (SQLException e) { 
            throw new RuntimeException("Erro ao baixar stock: " + e.getMessage()); 
        }
    }

    public boolean aumentarStock(int idRestaurante, int idIngrediente, int qtd) {
        String sql = "UPDATE Stock SET quantidade = quantidade + ? WHERE id_restaurante = ? AND id_ingrediente = ?";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, qtd);
            stmt.setInt(2, idRestaurante);
            stmt.setInt(3, idIngrediente);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { 
            return false; 
        }
    }

    @Override
    public Collection<Integer> values() {
        List<Integer> res = new ArrayList<>();
        try (Connection conn = ConexaoDB.getConexao();
             Statement stm = conn.createStatement();
             ResultSet rs = stm.executeQuery("SELECT quantidade FROM Stock")) {
            while (rs.next()) {
                res.add(rs.getInt("quantidade"));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return res;
    }

    /* Stub métodos obrigatórios da interface Map */
    @Override public boolean isEmpty() { return this.size() == 0; }
    @Override public boolean containsValue(Object value) { return false; }
    @Override public Integer remove(Object key) { return null; }
    @Override public void putAll(Map<? extends String, ? extends Integer> m) {}
    @Override public void clear() {}
    @Override public Set<String> keySet() { return null; }
    @Override public Set<Entry<String, Integer>> entrySet() { return null; }
}