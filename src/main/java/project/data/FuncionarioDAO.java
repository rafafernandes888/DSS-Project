package project.data;

import project.business.RH.*;
import java.sql.*;
import java.util.*;

/**
 * FuncionarioDAO refatorado para implementar Map<Integer, Funcionario> e Singleton.
 * Abstrai a persistência em múltiplas tabelas (COO, Gerente, Trabalhador).
 */
public class FuncionarioDAO implements Map<Integer, Funcionario> {

    private static FuncionarioDAO singleton = null;

    private FuncionarioDAO() {}

    public static FuncionarioDAO getInstance() {
        if (singleton == null) {
            singleton = new FuncionarioDAO();
        }
        return singleton;
    }

    /* --- MÉTODOS DA INTERFACE MAP (Indexação por ID) --- */

    @Override
    public int size() {
        int total = 0;
        String[] tabelas = {"COO", "Gerente", "Trabalhador"};
        try (Connection conn = ConexaoDB.getConexao()) {
            for (String tab : tabelas) {
                try (PreparedStatement stm = conn.prepareStatement("SELECT count(*) FROM " + tab);
                     ResultSet rs = stm.executeQuery()) {
                    if (rs.next()) total += rs.getInt(1);
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return total;
    }

    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    @Override
    public Funcionario get(Object key) {
        if (!(key instanceof Integer)) return null;
        int id = (Integer) key;
        // Tenta encontrar em cada tabela sucessivamente
        Funcionario f = buscarPorId(id, "COO");
        if (f != null) return f;
        f = buscarPorId(id, "Gerente");
        if (f != null) return f;
        return buscarPorId(id, "Trabalhador");
    }

    @Override
    public Funcionario put(Integer key, Funcionario f) {
        String tabela;
        if (f instanceof COO) tabela = "COO";
        else if (f instanceof Gerente) tabela = "Gerente";
        else tabela = "Trabalhador";

        String sql;
        if (f instanceof Trabalhador) {
            sql = "INSERT INTO Trabalhador (id, nome, pass, posto_atual, id_restaurante) VALUES (?, ?, ?, ?, ?) " +
                  "ON DUPLICATE KEY UPDATE nome=VALUES(nome), pass=VALUES(pass), posto_atual=VALUES(posto_atual), id_restaurante=VALUES(id_restaurante)";
        } else if (f instanceof Gerente) {
            sql = "INSERT INTO Gerente (id, nome, pass, id_restaurante) VALUES (?, ?, ?, ?) " +
                  "ON DUPLICATE KEY UPDATE nome=VALUES(nome), pass=VALUES(pass), id_restaurante=VALUES(id_restaurante)";
        } else {
            sql = "INSERT INTO " + tabela + " (id, nome, pass) VALUES (?, ?, ?) " +
                  "ON DUPLICATE KEY UPDATE nome=VALUES(nome), pass=VALUES(pass)";
        }

        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, f.getId());
            stmt.setString(2, f.getNome());
            stmt.setString(3, f.getPassword());
            
            if (f instanceof Trabalhador) {
                Trabalhador t = (Trabalhador) f;
                stmt.setString(4, t.getPostoAtual());
                stmt.setInt(5, t.getIdRestaurante());
            } else if (f instanceof Gerente) {
                stmt.setInt(4, ((Gerente) f).getIdRestaurante());
            }
            
            stmt.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
        return f;
    }

    @Override
    public Collection<Funcionario> values() {
        List<Funcionario> lista = new ArrayList<>();
        buscarNaTabela(lista, "SELECT * FROM COO", "COO");
        buscarNaTabela(lista, "SELECT * FROM Gerente", "GERENTE");
        buscarNaTabela(lista, "SELECT * FROM Trabalhador", "TRABALHADOR");
        return lista;
    }

    /* --- MÉTODOS DE LÓGICA DE NEGÓCIO --- */

    public Funcionario autenticar(int id, String password) {
        Funcionario f = this.get(id);
        if (f != null && f.getPassword().equals(password)) {
            return f;
        }
        return null;
    }

    public boolean realocarTrabalhador(int id, String novoPosto) {
        String sql = "UPDATE Trabalhador SET posto_atual = ? WHERE id = ?";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, novoPosto);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public List<Funcionario> listarDoRestauranteDoGerente(int idGerente) {
        List<Funcionario> lista = new ArrayList<>();
        // Query otimizada para buscar toda a equipa (Trabalhadores e o próprio Gerente)
        String sql = "SELECT t.id, t.nome, t.pass, t.posto_atual, t.id_restaurante FROM Trabalhador t " +
                     "JOIN Gerente g ON t.id_restaurante = g.id_restaurante WHERE g.id = ?";

        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idGerente);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Trabalhador t = new Trabalhador(rs.getInt("id"), rs.getString("nome"), rs.getString("pass"));
                    t.setPostoAtual(rs.getString("posto_atual"));
                    t.setIdRestaurante(rs.getInt("id_restaurante"));
                    lista.add(t);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }

    public boolean removerFuncionario(int id) {
        String[] tabelas = {"COO", "Gerente", "Trabalhador"};
        try (Connection conn = ConexaoDB.getConexao()) {
            for (String tab : tabelas) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM " + tab + " WHERE id = ?")) {
                    stmt.setInt(1, id);
                    if (stmt.executeUpdate() > 0) return true;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    /* --- MÉTODOS AUXILIARES DE PERSISTÊNCIA --- */

    private Funcionario buscarPorId(int id, String tabela) {
        String sql = "SELECT * FROM " + tabela + " WHERE id = ?";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String nome = rs.getString("nome");
                    String pass = rs.getString("pass");
                    
                    if (tabela.equals("COO")) return new COO(id, nome, pass);
                    
                    // CORREÇÃO: Adicionado o 4º argumento (id_restaurante)
                    if (tabela.equals("Gerente")) {
                        return new Gerente(id, nome, pass, rs.getInt("id_restaurante"));
                    }
                    
                    Trabalhador t = new Trabalhador(id, nome, pass);
                    t.setPostoAtual(rs.getString("posto_atual"));
                    t.setIdRestaurante(rs.getInt("id_restaurante"));
                    return t;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    private void buscarNaTabela(List<Funcionario> lista, String sql, String tipo) {
        try (Connection conn = ConexaoDB.getConexao();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String nome = rs.getString("nome");
                String pass = rs.getString("pass");

                if (tipo.equals("COO")) {
                    lista.add(new COO(id, nome, pass));
                } else if (tipo.equals("GERENTE")) {
                    // CORREÇÃO: Adicionado o 4º argumento (id_restaurante)
                    lista.add(new Gerente(id, nome, pass, rs.getInt("id_restaurante")));
                } else {
                    Trabalhador t = new Trabalhador(id, nome, pass);
                    t.setPostoAtual(rs.getString("posto_atual"));
                    t.setIdRestaurante(rs.getInt("id_restaurante"));
                    lista.add(t);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    /* Stubs obrigatórios da interface Map */
    @Override public boolean isEmpty() { return size() == 0; }
    @Override public boolean containsValue(Object value) { return false; }
    @Override public Funcionario remove(Object key) { 
        Funcionario f = get(key);
        if (f != null) removerFuncionario((Integer) key); 
        return f; 
    }
    @Override public void putAll(Map<? extends Integer, ? extends Funcionario> m) {
        for (Entry<? extends Integer, ? extends Funcionario> e : m.entrySet()) put(e.getKey(), e.getValue());
    }
    @Override public void clear() {}
    @Override public Set<Integer> keySet() { return null; }
    @Override public Set<Entry<Integer, Funcionario>> entrySet() { return null; }
}