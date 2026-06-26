package project.data;

import project.business.Operacoes.Ingrediente;
import java.sql.*;
import java.util.*;

/**
 * IngredienteDAO refatorado para implementar Map<Integer, Ingrediente> e Singleton.
 * Proporciona acesso eficiente (O(1)) aos ingredientes por ID.
 */
public class IngredienteDAO implements Map<Integer, Ingrediente> {

    private static IngredienteDAO singleton = null;

    // Construtor privado para o padrão Singleton
    private IngredienteDAO() {}

    /**
     * Obtém a instância única do DAO.
     */
    public static IngredienteDAO getInstance() {
        if (singleton == null) {
            singleton = new IngredienteDAO();
        }
        return singleton;
    }

    /* --- MÉTODOS DA INTERFACE MAP --- */

    @Override
    public int size() {
        int i = 0;
        try (Connection conn = ConexaoDB.getConexao();
             Statement stm = conn.createStatement();
             ResultSet rs = stm.executeQuery("SELECT count(*) FROM Ingrediente")) {
            if(rs.next()) i = rs.getInt(1);
        } catch (SQLException e) { 
            throw new RuntimeException("Erro ao contar ingredientes", e); 
        }
        return i;
    }

    @Override
    public boolean containsKey(Object key) {
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement pstm = conn.prepareStatement("SELECT id FROM Ingrediente WHERE id=?")) {
            pstm.setInt(1, (Integer) key);
            try (ResultSet rs = pstm.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) { 
            throw new RuntimeException(e); 
        }
    }

    @Override
    public Ingrediente get(Object key) {
        Ingrediente ing = null;
        String sql = "SELECT * FROM Ingrediente WHERE id=?";

        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setInt(1, (Integer) key);
            try (ResultSet rs = pstm.executeQuery()) {
                if (rs.next()) {
                    ing = new Ingrediente(
                        rs.getInt("id"),
                        rs.getString("nome"),
                        rs.getBoolean("alergenio"),
                        rs.getDouble("preco_venda")
                    );
                }
            }
        } catch (SQLException e) { 
            throw new RuntimeException(e); 
        }
        return ing;
    }

    @Override
    public Collection<Ingrediente> values() {
        Collection<Ingrediente> res = new ArrayList<>();
        try (Connection conn = ConexaoDB.getConexao();
             Statement stm = conn.createStatement();
             ResultSet rs = stm.executeQuery("SELECT id FROM Ingrediente")) {
            while (rs.next()) {
                res.add(this.get(rs.getInt("id")));
            }
        } catch (SQLException e) { 
            throw new RuntimeException(e); 
        }
        return res;
    }

    @Override
    public Ingrediente put(Integer key, Ingrediente value) {
        String sql = "INSERT INTO Ingrediente (id, nome, alergenio, preco_venda) VALUES (?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE nome=VALUES(nome), alergenio=VALUES(alergenio), preco_venda=VALUES(preco_venda)";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, value.getId());
            stmt.setString(2, value.getNome());
            stmt.setBoolean(3, value.isAlergenio());
            stmt.setDouble(4, value.getPrecoVenda());
            stmt.executeUpdate();
        } catch (SQLException e) { 
            throw new RuntimeException(e); 
        }
        return value;
    }

    /* Métodos secundários da interface Map (pode deixar como stubs ou implementar se necessário) */
    @Override public boolean isEmpty() { return size() == 0; }
    @Override public boolean containsValue(Object value) { return false; }
    @Override public Ingrediente remove(Object key) { return null; }
    @Override public void putAll(Map<? extends Integer, ? extends Ingrediente> m) {}
    @Override public void clear() {}
    @Override public Set<Integer> keySet() { return null; }
    @Override public Set<Entry<Integer, Ingrediente>> entrySet() { return null; }
}