package project.data;

import project.business.Comunicacao.*;
import project.business.Operacoes.*; 
import java.sql.*;
import java.util.*;

/**
 * PedidoDAO refatorado para implementar Map<Integer, Pedido> e Singleton.
 * Segue as boas práticas de DSS observadas no projeto de referência.
 */
public class PedidoDAO implements Map<Integer, Pedido> {

    private static PedidoDAO singleton = null;

    private PedidoDAO() {}

    public static PedidoDAO getInstance() {
        if (singleton == null) {
            singleton = new PedidoDAO();
        }
        return singleton;
    }

    /* --- MÉTODOS DA INTERFACE MAP --- */

    @Override
    public int size() {
        int i = 0;
        try (Connection conn = ConexaoDB.getConexao();
             Statement stm = conn.createStatement();
             ResultSet rs = stm.executeQuery("SELECT count(*) FROM Pedido")) {
            if(rs.next()) i = rs.getInt(1);
        } catch (SQLException e) { throw new RuntimeException(e); }
        return i;
    }

    @Override
    public boolean containsKey(Object key) {
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement pstm = conn.prepareStatement("SELECT id FROM Pedido WHERE id=?")) {
            pstm.setInt(1, (Integer) key);
            try (ResultSet rs = pstm.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public Pedido get(Object key) {
        Pedido p = null;
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Pedido WHERE id = ?")) {
            stmt.setInt(1, (Integer) key);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    p = mapResultSetToPedido(rs);
                    // Carrega os itens (linhas) do pedido automaticamente
                    List<LinhaPedido> itens = getItensDoPedido(p.getId());
                    for(LinhaPedido l : itens) {
                        p.adicionarItem(l.getArtigo(), l.getPersonalizacao(), l.isPagoComPontos());
                    }
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return p;
    }

    @Override
    public Pedido put(Integer key, Pedido value) {
        // No contexto de DAOs, o put equivale a criar ou atualizar o registo
        this.criarPedido(value);
        return value;
    }

    @Override
    public Collection<Pedido> values() {
        List<Pedido> lista = new ArrayList<>();
        try (Connection conn = ConexaoDB.getConexao();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id FROM Pedido")) {
            while (rs.next()) {
                lista.add(this.get(rs.getInt("id")));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return lista;
    }

    /* --- MÉTODOS ESPECÍFICOS DE NEGÓCIO --- */

    public int criarPedido(Pedido p) {
        String sqlPedido = "INSERT INTO Pedido (id_restaurante, estado, id_cliente, metodo_pagamento, local_consumo, tempo_estimado, valor_total) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String sqlItens = "INSERT INTO Pedido_Artigo (id_pedido, id_artigo, personalizacao, pago_com_pontos) VALUES (?, ?, ?, ?)";

        try (Connection conn = ConexaoDB.getConexao()) {
            conn.setAutoCommit(false); 

            try (PreparedStatement stmt = conn.prepareStatement(sqlPedido, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, p.getIdRestaurante());
                stmt.setString(2, p.getEstado().name());
                if (p.getCliente() == null) stmt.setNull(3, java.sql.Types.INTEGER);
                else stmt.setInt(3, p.getCliente().getId());
                stmt.setString(4, p.getMetodoPagamento());
                stmt.setString(5, p.getLocalConsumo());
                stmt.setInt(6, p.getTempoEstimado());
                stmt.setDouble(7, p.getValorTotal()); 
                stmt.executeUpdate();

                ResultSet rs = stmt.getGeneratedKeys();
                int idPedido = rs.next() ? rs.getInt(1) : -1;

                if (idPedido != -1) {
                    try (PreparedStatement stmtItens = conn.prepareStatement(sqlItens)) {
                        for (LinhaPedido linha : p.getItens()) {
                            stmtItens.setInt(1, idPedido);
                            stmtItens.setInt(2, linha.getArtigo().getId());
                            stmtItens.setString(3, linha.getPersonalizacao());
                            stmtItens.setBoolean(4, linha.isPagoComPontos());
                            stmtItens.addBatch();
                        }
                        stmtItens.executeBatch();
                    }
                }
                
                conn.commit();
                p.setId(idPedido);
                return idPedido;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) { 
            e.printStackTrace();
            return -1; 
        }
    }

    public List<LinhaPedido> getItensDoPedido(int idPedido) {
        List<LinhaPedido> itens = new ArrayList<>();
        String sql = "SELECT pa.personalizacao, pa.pago_com_pontos, a.id, a.nome, a.preco, a.categoria, a.descricao " +
                     "FROM Pedido_Artigo pa JOIN Artigo a ON pa.id_artigo = a.id WHERE pa.id_pedido = ?";
        
        String sqlIng = "SELECT i.id, i.nome, i.alergenio, i.preco_venda FROM Ingrediente i " +
                        "JOIN Artigo_Ingrediente ai ON i.id = ai.id_ingrediente " +
                        "WHERE ai.id_artigo = ?";

        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idPedido);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Categoria catEnum = Categoria.valueOf(rs.getString("categoria").toUpperCase().replace("MENU", "MENU_COMPLETO"));
                    Artigo artigo = new Artigo(rs.getInt("id"), rs.getString("nome"), catEnum, rs.getDouble("preco"), rs.getString("descricao"));
                    
                    try (PreparedStatement psIng = conn.prepareStatement(sqlIng)) {
                        psIng.setInt(1, artigo.getId());
                        try (ResultSet rsIng = psIng.executeQuery()) {
                            while (rsIng.next()) {
                                artigo.adicionarIngrediente(new Ingrediente(
                                    rsIng.getInt("id"), rsIng.getString("nome"), 
                                    rsIng.getBoolean("alergenio"), rsIng.getDouble("preco_venda")
                                ));
                            }
                        }
                    }
                    itens.add(new LinhaPedido(artigo, rs.getString("personalizacao"), rs.getBoolean("pago_com_pontos")));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return itens;
    }

    public List<Pedido> listarPedidosAtivos(int idRestaurante) {
        List<Pedido> lista = new ArrayList<>();
        String sql = "SELECT * FROM Pedido WHERE id_restaurante = ? AND estado NOT IN ('CONCLUIDO', 'ENTREGUE') " +
                    "ORDER BY CASE WHEN estado = 'EM_ESPERA' THEN 1 ELSE 0 END, id ASC";

        try (Connection conn = ConexaoDB.getConexao();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idRestaurante);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapResultSetToPedido(rs));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }

    public boolean atualizarEstado(int idPedido, EstadoPedido novoEstado) {
        String sql = (novoEstado == EstadoPedido.CONCLUIDO) 
            ? "UPDATE Pedido SET estado = ?, data_conclusao = CURRENT_TIMESTAMP WHERE id = ?" 
            : "UPDATE Pedido SET estado = ? WHERE id = ?";
        
        try (Connection conn = ConexaoDB.getConexao();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, novoEstado.name());
            stmt.setInt(2, idPedido);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public Relatorio getRelatorioRestaurante(int idRest, String nomeRest) {
        String sql = "SELECT SUM(valor_total) as faturacao, " +
                     "AVG(TIMESTAMPDIFF(MINUTE, data_hora, data_conclusao)) as media " +
                     "FROM Pedido WHERE id_restaurante = ? AND estado = 'ENTREGUE'";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idRest);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                double faturacao = rs.getDouble("faturacao");
                double media = rs.getDouble("media");
                return new Relatorio(nomeRest, "Gerente Loja " + idRest, faturacao, media);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return new Relatorio(nomeRest, "Gerente Loja " + idRest, 0.0, 0.0);
    }

    public boolean adiarPedido(int idPedido, int minutosExtra) {
        String sql = "UPDATE Pedido SET estado = 'EM_ESPERA', tempo_estimado = tempo_estimado + ? WHERE id = ?";
        try (Connection conn = ConexaoDB.getConexao();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, minutosExtra);
            stmt.setInt(2, idPedido);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public int getSomaTemposEspera(int idRestaurante) {
        String sql = "SELECT SUM(tempo_estimado) as total FROM Pedido " +
                    "WHERE id_restaurante = ? AND estado IN ('REGISTADO', 'EM_PREPARACAO')";
        try (Connection conn = ConexaoDB.getConexao();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idRestaurante);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getInt("total") : 0;
        } catch (SQLException e) { return 0; }
    }

    private Pedido mapResultSetToPedido(ResultSet rs) throws SQLException {
        int idCli = rs.getInt("id_cliente");
        Cliente c = (idCli > 0) ? new Cliente(idCli, "N/A", 0) : null;
        Pedido p = new Pedido(c);
        p.setId(rs.getInt("id"));
        p.setIdRestaurante(rs.getInt("id_restaurante"));
        try { p.setEstado(EstadoPedido.valueOf(rs.getString("estado"))); } catch (Exception e) {}
        p.setMetodoPagamento(rs.getString("metodo_pagamento"));
        p.setLocalConsumo(rs.getString("local_consumo"));
        p.setTempoEstimado(rs.getInt("tempo_estimado"));
        p.setValorTotal(rs.getDouble("valor_total"));
        return p;
    }

    /* Stub métodos obrigatórios da interface Map */
    @Override public boolean isEmpty() { return this.size() == 0; }
    @Override public boolean containsValue(Object value) { return false; }
    @Override public Pedido remove(Object key) { return null; }
    @Override public void putAll(Map<? extends Integer, ? extends Pedido> m) {}
    @Override public void clear() {}
    @Override public Set<Integer> keySet() { return null; }
    @Override public Set<Entry<Integer, Pedido>> entrySet() { return null; }
}