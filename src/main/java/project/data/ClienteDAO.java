package project.data;

import java.sql.*;
import java.util.*;
import project.business.Operacoes.Cliente;

/**
 * ClienteDAO refatorado para implementar Map<Integer, Cliente> e Singleton.
 * Segue as boas práticas de DSS para gestão eficiente de entidades.
 */
public class ClienteDAO implements Map<Integer, Cliente> {

    private static ClienteDAO singleton = null;

    private ClienteDAO() {}

    public static ClienteDAO getInstance() {
        if (singleton == null) {
            singleton = new ClienteDAO();
        }
        return singleton;
    }

    /* --- MÉTODOS DA INTERFACE MAP (Acesso por ID) --- */

    @Override
    public int size() {
        int i = 0;
        try (Connection conn = ConexaoDB.getConexao();
             Statement stm = conn.createStatement();
             ResultSet rs = stm.executeQuery("SELECT count(*) FROM Cliente")) {
            if(rs.next()) i = rs.getInt(1);
        } catch (SQLException e) { throw new RuntimeException(e); }
        return i;
    }

    @Override
    public boolean containsKey(Object key) {
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement pstm = conn.prepareStatement("SELECT id FROM Cliente WHERE id=?")) {
            pstm.setInt(1, (Integer) key);
            try (ResultSet rs = pstm.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public Cliente get(Object key) {
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Cliente WHERE id = ?")) {
            stmt.setInt(1, (Integer) key);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Cliente(rs.getInt("id"), rs.getString("nif"), rs.getInt("pontos"));
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return null;
    }

    @Override
    public Cliente put(Integer key, Cliente value) {
        // No contexto de DAOs, o put insere ou atualiza o cliente
        String sql = "INSERT INTO Cliente (id, nif, pontos) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE nif=VALUES(nif), pontos=VALUES(pontos)";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, value.getId());
            stmt.setString(2, value.getNif());
            stmt.setInt(3, value.getPontos());
            stmt.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
        return value;
    }

    @Override
    public Collection<Cliente> values() {
        List<Cliente> clientes = new ArrayList<>();
        try (Connection conn = ConexaoDB.getConexao();
             Statement stm = conn.createStatement();
             ResultSet rs = stm.executeQuery("SELECT id FROM Cliente")) {
            while (rs.next()) {
                clientes.add(this.get(rs.getInt("id")));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return clientes;
    }

    /* --- MÉTODOS DE LÓGICA DE NEGÓCIO (Acesso por NIF) --- */

    public Cliente autenticarOuRegistar(String nif) {
        Cliente c = buscarPorNIF(nif);
        if (c != null) return c;
        
        // Se não existe, cria um novo
        return criarNovoCliente(nif);
    }

    public void atualizarPontos(int idCliente, int novosPontosTotais) {
        String sql = "UPDATE Cliente SET pontos = ? WHERE id = ?";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, novosPontosTotais);
            stmt.setInt(2, idCliente);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar pontos: " + e.getMessage());
        }
    }

    private Cliente buscarPorNIF(String nif) {
        String sql = "SELECT * FROM Cliente WHERE nif = ?";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nif);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Cliente(rs.getInt("id"), rs.getString("nif"), rs.getInt("pontos"));
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return null; 
    }

    private Cliente criarNovoCliente(String nif) {
        String sql = "INSERT INTO Cliente (nif, pontos) VALUES (?, 0)";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, nif);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return new Cliente(rs.getInt(1), nif, 0);
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return null;
    }

    /* Métodos obrigatórios da interface Map */
    @Override public boolean isEmpty() { return this.size() == 0; }
    @Override public boolean containsValue(Object value) { return false; }
    @Override public Cliente remove(Object key) { return null; }
    @Override public void putAll(Map<? extends Integer, ? extends Cliente> m) {}
    @Override public void clear() {}
    @Override public Set<Integer> keySet() { return null; }
    @Override public Set<Entry<Integer, Cliente>> entrySet() { return null; }
}