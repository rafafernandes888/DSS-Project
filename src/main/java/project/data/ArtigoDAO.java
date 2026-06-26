package project.data;

import project.business.Operacoes.Artigo;
import project.business.Operacoes.Categoria;
import project.business.Operacoes.Ingrediente;

import java.sql.*;
import java.util.*;

/**
 * Refatoração seguindo o padrão do projeto de referência (Map e Singleton)
 */
public class ArtigoDAO implements Map<Integer, Artigo> {
    
    private static ArtigoDAO singleton = null;

    // Construtor privado para garantir o padrão Singleton
    private ArtigoDAO() {
        // Opcional: Aqui podes colocar a lógica de criação da tabela se ela não existir
    }

    // Método para obter a instância única (Singleton)
    public static ArtigoDAO getInstance() {
        if (ArtigoDAO.singleton == null) {
            ArtigoDAO.singleton = new ArtigoDAO();
        }
        return ArtigoDAO.singleton;
    }

    @Override
    public int size() {
        int i = 0;
        try (Connection conn = ConexaoDB.getConexao();
             Statement stm = conn.createStatement();
             ResultSet rs = stm.executeQuery("SELECT count(*) FROM Artigo")) {
            if(rs.next()) i = rs.getInt(1);
        } catch (SQLException e) { throw new RuntimeException(e); }
        return i;
    }

    @Override
    public boolean containsKey(Object key) {
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement pstm = conn.prepareStatement("SELECT id FROM Artigo WHERE id=?")) {
            pstm.setInt(1, (Integer) key);
            try (ResultSet rs = pstm.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public Artigo get(Object key) {
        Artigo a = null;
        String sqlArtigo = "SELECT * FROM Artigo WHERE id=?";
        String sqlIng = "SELECT i.id, i.nome, i.alergenio, i.preco_venda " +
                        "FROM Ingrediente i JOIN Artigo_Ingrediente ai ON i.id = ai.id_ingrediente " +
                        "WHERE ai.id_artigo = ?";

        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement pstm = conn.prepareStatement(sqlArtigo)) {
            pstm.setInt(1, (Integer) key);
            try (ResultSet rs = pstm.executeQuery()) {
                if (rs.next()) {
                    // Mapeamento da Categoria (mantendo a tua lógica segura)
                    Categoria cat = Categoria.valueOf(rs.getString("categoria").toUpperCase().replace("MENU", "MENU_COMPLETO"));
                    
                    a = new Artigo(rs.getInt("id"), rs.getString("nome"), cat, rs.getDouble("preco"), rs.getString("descricao"));

                    // Carregar Ingredientes
                    try (PreparedStatement pstmIng = conn.prepareStatement(sqlIng)) {
                        pstmIng.setInt(1, a.getId());
                        try (ResultSet rsIng = pstmIng.executeQuery()) {
                            while (rsIng.next()) {
                                a.adicionarIngrediente(new Ingrediente(
                                    rsIng.getInt("id"), rsIng.getString("nome"), 
                                    rsIng.getBoolean("alergenio"), rsIng.getDouble("preco_venda")
                                ));
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return a;
    }

    @Override
    public Collection<Artigo> values() {
        Collection<Artigo> res = new ArrayList<>();
        try (Connection conn = ConexaoDB.getConexao();
             Statement stm = conn.createStatement();
             ResultSet rs = stm.executeQuery("SELECT id FROM Artigo")) {
            while (rs.next()) {
                res.add(this.get(rs.getInt("id")));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return res;
    }

    // Métodos obrigatórios da interface Map (podem ser implementados conforme necessário ou lançar exceção)
    @Override public boolean isEmpty() { return this.size() == 0; }
    @Override public boolean containsValue(Object value) { return false; }
    @Override public Artigo put(Integer key, Artigo value) { return null; } // Implementar se precisares de salvar via Map
    @Override public Artigo remove(Object key) { return null; }
    @Override public void putAll(Map<? extends Integer, ? extends Artigo> m) {}
    @Override public void clear() {}
    @Override public Set<Integer> keySet() { return null; }
    @Override public Set<Entry<Integer, Artigo>> entrySet() { return null; }
}