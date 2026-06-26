package project.data;

import project.business.Comunicacao.*;
import project.business.Operacoes.*;
import project.business.RH.*;

import java.sql.*;
import java.util.*;

/**
 * RestauranteDAO atualizado para suportar o padrão de Domínio Rico (Pre/Pos DAO).
 * O get() carrega as mensagens para o objeto.
 * O put() persiste o estado completo do objeto (incluindo mensagens).
 */
public class RestauranteDAO implements Map<Integer, Restaurante> {

    private static RestauranteDAO singleton = null;

    private RestauranteDAO() {}

    public static RestauranteDAO getInstance() {
        if (singleton == null) {
            singleton = new RestauranteDAO();
        }
        return singleton;
    }

    /* --- MÉTODOS DA INTERFACE MAP (Acesso por ID) --- */

    @Override
    public int size() {
        try (Connection conn = ConexaoDB.getConexao();
             Statement stm = conn.createStatement();
             ResultSet rs = stm.executeQuery("SELECT count(*) FROM Restaurante")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) { throw new RuntimeException("Erro ao contar restaurantes", e); }
    }

    @Override
    public boolean containsKey(Object key) {
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement pstm = conn.prepareStatement("SELECT 1 FROM Restaurante WHERE id=?")) {
            pstm.setInt(1, (Integer) key);
            try (ResultSet rs = pstm.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public Restaurante get(Object key) {
        Restaurante r = null;
        String sql = "SELECT r.id, r.localizacao, " +
                    "g.id AS id_gerente, g.nome AS nome_gerente, g.pass AS pass_gerente " +
                    "FROM Restaurante r " +
                    "LEFT JOIN Gerente g ON r.id_gerente = g.id " +
                    "WHERE r.id = ?";

        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, (Integer) key);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Gerente gerente = null;
                    int idGerente = rs.getInt("id_gerente"); 
                    if (idGerente > 0) {
                        gerente = new Gerente(idGerente, rs.getString("nome_gerente"), rs.getString("pass_gerente"), rs.getInt("id"));
                    }
                    r = new Restaurante(rs.getInt("id"), rs.getString("localizacao"), gerente);
                    
                    // --- ATUALIZAÇÃO: Carregar mensagens para a memória do objeto ---
                    carregarMensagensParaObjeto(r);
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return r;
    }

    /**
     * Helper: Vai à BD buscar as mensagens e coloca-as dentro do objeto Restaurante.
     */
    private void carregarMensagensParaObjeto(Restaurante r) {
        // Ordenamos ASC para manter a ordem cronológica correta ao inserir na lista
        String sql = "SELECT conteudo, is_public FROM Mensagem_Restaurante WHERE id_restaurante = ? ORDER BY data_envio ASC";
        
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, r.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while(rs.next()) {
                    // O objeto Restaurante guarda as mensagens nas suas listas internas
                    r.adicionarMensagem(rs.getString("conteudo"), rs.getBoolean("is_public"));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public Collection<Restaurante> values() {
        List<Restaurante> lista = new ArrayList<>();
        try (Connection conn = ConexaoDB.getConexao();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id FROM Restaurante")) {
            while (rs.next()) {
                lista.add(this.get(rs.getInt("id")));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return lista;
    }

    @Override
    public Restaurante put(Integer key, Restaurante value) {
        String sqlRest = "INSERT INTO Restaurante (id, localizacao, id_gerente) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE localizacao=VALUES(localizacao), id_gerente=VALUES(id_gerente)";
        
        try (Connection conn = ConexaoDB.getConexao()) {
            conn.setAutoCommit(false); // Início da Transação
            
            try (PreparedStatement stmt = conn.prepareStatement(sqlRest)) {
                // 1. Gravar Restaurante
                stmt.setInt(1, value.getId());
                stmt.setString(2, value.getLocalizacao());
                if (value.getGerente() != null) stmt.setInt(3, value.getGerente().getId());
                else stmt.setNull(3, Types.INTEGER);
                stmt.executeUpdate();
                
                // 2. Gravar Mensagens (Sincronização de Estado)
                // Estratégia: Limpar antigas e regravar a lista atual do objeto para garantir consistência
                try (PreparedStatement delStmt = conn.prepareStatement("DELETE FROM Mensagem_Restaurante WHERE id_restaurante = ?")) {
                    delStmt.setInt(1, value.getId());
                    delStmt.executeUpdate();
                }

                String sqlMsg = "INSERT INTO Mensagem_Restaurante (id_restaurante, conteudo, is_public) VALUES (?, ?, ?)";
                try (PreparedStatement stmtMsg = conn.prepareStatement(sqlMsg)) {
                    
                    // Alertas Públicos
                    for (String msg : value.getMensagens(true)) {
                        stmtMsg.setInt(1, value.getId());
                        stmtMsg.setString(2, msg);
                        stmtMsg.setBoolean(3, true); 
                        stmtMsg.executeUpdate();
                    }

                    // Mensagens Privadas
                    for (String msg : value.getMensagens(false)) {
                        stmtMsg.setInt(1, value.getId());
                        stmtMsg.setString(2, msg);
                        stmtMsg.setBoolean(3, false);
                        stmtMsg.executeUpdate();
                    }
                }

                conn.commit(); // Sucesso
                
            } catch (SQLException e) {
                conn.rollback(); 
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) { 
            throw new RuntimeException("Erro ao guardar Restaurante e Mensagens", e); 
        }
        
        return value;
    }

    /* --- MÉTODOS DE LÓGICA DE NEGÓCIO --- */

    public int getRestauranteDoFuncionario(int idFuncionario) {
        // CORREÇÃO: Primeiro procura na tabela Gerente
        String sqlGerente = "SELECT id_restaurante FROM Gerente WHERE id = ?";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sqlGerente)) {
            stmt.setInt(1, idFuncionario);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int idRest = rs.getInt("id_restaurante");
                    if (idRest > 0) return idRest;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        
        // Se não encontrou em Gerente, procura em Trabalhador
        String sqlTrabalhador = "SELECT id_restaurante FROM Trabalhador WHERE id = ?";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sqlTrabalhador)) {
            stmt.setInt(1, idFuncionario);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_restaurante");
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        
        return -1; // Não encontrado em nenhuma tabela
    }

    // Mantemos estes métodos para utilidade ou compatibilidade,
    // embora o Gestor agora use get/put para a lógica principal.

    public boolean enviarMensagemGlobal(String msg, boolean isPublic) {
        String sql = "INSERT INTO Mensagem_Restaurante (id_restaurante, conteudo, is_public) SELECT id, ?, ? FROM Restaurante";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, msg);
            stmt.setBoolean(2, isPublic);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }
    
    // Wrapper simples caso ainda seja necessário chamar diretamente
    public boolean enviarMensagem(int idRest, String msg, boolean isPublic) {
         // Agora preferimos usar o put() via Gestor, mas isto serve de backup
        String sql = "INSERT INTO Mensagem_Restaurante (id_restaurante, conteudo, is_public) VALUES (?, ?, ?)";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idRest);
            stmt.setString(2, msg);
            stmt.setBoolean(3, isPublic);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public List<String> listarAlertasPublicos(int idRest) {
        // Método de consulta direta (pode ser usado como otimização)
        List<String> alertas = new ArrayList<>();
        String sql = "SELECT conteudo FROM Mensagem_Restaurante WHERE id_restaurante = ? AND is_public = 1 ORDER BY data_envio DESC";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idRest);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) alertas.add(rs.getString("conteudo"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return alertas;
    }

    public boolean limparAlertasDoPedido(int idRestaurante, int idPedido) {
        // Remove alertas públicos que contenham referência ao pedido (ex: "Pedido #1:")
        String sql = "DELETE FROM Mensagem_Restaurante WHERE id_restaurante = ? AND is_public = 1 AND conteudo LIKE ?";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idRestaurante);
            stmt.setString(2, "Pedido #" + idPedido + ":%");
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }
    
    public int contarNotificacoes(int idRestaurante, int idTrabalhador) {
        String sql = "SELECT COUNT(*) FROM Mensagem_Restaurante m " +
                     "WHERE m.id_restaurante = ? " +
                     "AND NOT EXISTS (SELECT 1 FROM Mensagem_Lida l WHERE l.id_mensagem = m.id AND l.id_trabalhador = ?)";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idRestaurante);
            stmt.setInt(2, idTrabalhador);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }
    
    public List<String> lerMensagens(int idRestaurante, int idTrabalhador) {
        // Mantido para compatibilidade se necessário
        List<String> msgs = new ArrayList<>();
        String sqlSelect = "SELECT id, conteudo, data_envio FROM Mensagem_Restaurante WHERE id_restaurante = ? ORDER BY data_envio DESC";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sqlSelect)) {
            stmt.setInt(1, idRestaurante);
            try (ResultSet rs = stmt.executeQuery()) {
                while(rs.next()) {
                    msgs.add(String.format("[%s] 📢 ADMIN: %s", rs.getTimestamp("data_envio"), rs.getString("conteudo")));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return msgs;
    }

    public List<Relatorio> obterRelatoriosDeDesempenho() {
        Collection<Restaurante> restaurantes = this.values();
        List<Relatorio> relatorios = new ArrayList<>();

        for (Restaurante r : restaurantes) {
            double faturacaoSimulada = Math.random() * 5000;
            double esperaSimulada = 10 + Math.random() * 20;
            String nomeGerente = (r.getGerente() != null) ? r.getGerente().getNome() : "N/A";

            relatorios.add(new Relatorio(r.getLocalizacao(), nomeGerente, faturacaoSimulada, esperaSimulada));
        }
        return relatorios;
    }

    /* Stubs obrigatórios da interface Map */
    @Override public boolean isEmpty() { return size() == 0; }
    @Override public boolean containsValue(Object value) { return false; }
    @Override public Restaurante remove(Object key) { return null; }
    @Override public void putAll(Map<? extends Integer, ? extends Restaurante> m) {}
    @Override public void clear() {}
    @Override public Set<Integer> keySet() { return null; }
    @Override public Set<Entry<Integer, Restaurante>> entrySet() { return null; }
}