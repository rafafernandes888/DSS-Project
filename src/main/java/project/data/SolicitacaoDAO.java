package project.data;

import project.business.Comunicacao.Solicitacao;
import java.sql.*;
import java.util.*;

public class SolicitacaoDAO implements Map<Integer, Solicitacao> {

    private static SolicitacaoDAO singleton = null;

    private SolicitacaoDAO() {
        // Construtor vazio. Não cria tabelas (respeita o seed.py).
    }

    public static SolicitacaoDAO getInstance() {
        if (singleton == null) {
            singleton = new SolicitacaoDAO();
        }
        return singleton;
    }

    /* --- MÉTODOS DE LEITURA (GET) --- */

    @Override
    public Solicitacao get(Object key) {
        Solicitacao s = null;
        // Faz JOIN com Gerente para obter o nome do gerente (necessário para a UI do COO)
        String sql = "SELECT s.*, g.nome AS nome_gerente " +
                     "FROM Solicitacao_RH s " +
                     "LEFT JOIN Gerente g ON s.id_gerente = g.id " +
                     "WHERE s.id = ?";
                     
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, (Integer) key);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    s = new Solicitacao(
                        rs.getInt("id"),
                        rs.getInt("id_gerente"),
                        rs.getString("nome_gerente"), // Vem do JOIN
                        rs.getString("tipo"),
                        rs.getInt("id_trabalhador_alvo"), 
                        rs.getString("nome_candidato"),
                        rs.getString("pass_candidato"),
                        rs.getString("posto_proposto") 
                    );
                }
            }
        } catch (SQLException e) { 
            e.printStackTrace();
            throw new RuntimeException("Erro SQL ao ler Solicitação: " + e.getMessage());
        }
        return s;
    }

    @Override
    public boolean containsKey(Object key) {
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement pstm = conn.prepareStatement("SELECT 1 FROM Solicitacao_RH WHERE id=?")) {
            pstm.setInt(1, (Integer) key);
            try (ResultSet rs = pstm.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public int size() {
        try (Connection conn = ConexaoDB.getConexao();
             Statement stm = conn.createStatement();
             ResultSet rs = stm.executeQuery("SELECT count(*) FROM Solicitacao_RH")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    /* --- MÉTODOS DE ESCRITA (PUT / REMOVE) --- */

    @Override
    public Solicitacao put(Integer key, Solicitacao value) {
        // INSERE ou ATUALIZA na Base de Dados
        String sql = "INSERT INTO Solicitacao_RH (id, id_gerente, tipo, nome_candidato, pass_candidato, posto_proposto, id_trabalhador_alvo, estado) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDENTE') " +
                     "ON DUPLICATE KEY UPDATE estado=VALUES(estado)";
        
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, value.getId()); // Usa o ID gerado no Java
            stmt.setInt(2, value.getIdGerente());
            stmt.setString(3, value.getTipo());
            
            // Tratamento seguro de Nulos
            if (value.getNomeCandidato() != null) stmt.setString(4, value.getNomeCandidato());
            else stmt.setNull(4, Types.VARCHAR);

            if (value.getPassCandidato() != null) stmt.setString(5, value.getPassCandidato());
            else stmt.setNull(5, Types.VARCHAR);

            if (value.getPosto() != null) stmt.setString(6, value.getPosto());
            else stmt.setNull(6, Types.VARCHAR);
            
            if (value.getIdAlvo() > 0) stmt.setInt(7, value.getIdAlvo());
            else stmt.setNull(7, Types.INTEGER);

            stmt.executeUpdate();
            
            return value; // Sucesso
        } catch (SQLException e) { 
            e.printStackTrace();
            throw new RuntimeException("Erro ao gravar no schema do seed.py: " + e.getMessage()); 
        }
    }

    @Override
    public Solicitacao remove(Object key) {
        // APAGA da Base de Dados
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM Solicitacao_RH WHERE id = ?")) {
            stmt.setInt(1, (Integer) key);
            stmt.executeUpdate();
        } catch (SQLException e) { 
            e.printStackTrace(); 
        }
        return null;
    }

    /* --- MÉTODOS DE LISTAGEM --- */

    @Override
    public Collection<Solicitacao> values() {
        // Lista TODAS as solicitações
        List<Solicitacao> lista = new ArrayList<>();
        try (Connection conn = ConexaoDB.getConexao();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id FROM Solicitacao_RH")) {
            while (rs.next()) {
                Solicitacao s = this.get(rs.getInt("id"));
                if (s != null) lista.add(s);
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return lista;
    }

    public List<Solicitacao> listarPendentes() {
        // Lista apenas as PENDENTES (usado pelo menu do COO)
        List<Solicitacao> lista = new ArrayList<>();
        String sql = "SELECT id FROM Solicitacao_RH WHERE estado = 'PENDENTE'";
        
        try (Connection conn = ConexaoDB.getConexao();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Solicitacao s = this.get(rs.getInt("id"));
                if (s != null) lista.add(s);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }

    // Método extra para atualizar estado (se necessário)
    public boolean atualizarEstado(int id, String novoEstado) {
        String sql = "UPDATE Solicitacao_RH SET estado = ? WHERE id = ?";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, novoEstado);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    /* Stubs obrigatórios da interface Map (não usados, mas necessários para compilar) */
    @Override public boolean isEmpty() { return size() == 0; }
    @Override public boolean containsValue(Object value) { return false; }
    @Override public void putAll(Map<? extends Integer, ? extends Solicitacao> m) {}
    @Override public void clear() {}
    @Override public Set<Integer> keySet() { return null; }
    @Override public Set<Entry<Integer, Solicitacao>> entrySet() { return null; }
}