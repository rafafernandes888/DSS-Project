package project.business.RH;

import project.business.Comunicacao.Solicitacao;
import project.data.FuncionarioDAO;
import project.data.SolicitacaoDAO; 
import project.data.RestauranteDAO;

import java.util.*;
import java.util.stream.Collectors;

/**
 * GestorRH - Sub-fachada de Recursos Humanos.
 * Implementa o padrão de Maps para os DAOs e delegação de lógica.
 */
public class GestorRH implements IGestorRH {

    private Map<Integer, Funcionario> funcionarios;
    private Map<Integer, Solicitacao> solicitacoes;

    public GestorRH() {
        // Inicialização via Singleton seguindo o padrão de excelência (Nota 20)
        this.funcionarios = FuncionarioDAO.getInstance();
        this.solicitacoes = SolicitacaoDAO.getInstance();
    }

    @Override
    public Funcionario autenticar(int id, String password) {
        Funcionario f = this.funcionarios.get(id);
        if (f != null && f.getPassword().equals(password)) return f;
        return null;
    }

    @Override
    public List<Funcionario> listarGlobal() {
        return new ArrayList<>(this.funcionarios.values());
    }

    /**
     * CORREÇÃO: Lista toda a equipa do restaurante, incluindo Gerentes e Trabalhadores.
     */
    @Override
    public List<Funcionario> listarLocal(int idGerente) {
        // 1. Primeiro, encontra o restaurante do gerente
        Funcionario gerente = this.funcionarios.get(idGerente);
        if (gerente == null || !(gerente instanceof Gerente)) {
            return new ArrayList<>();
        }
        
        int idRestaurante = ((Gerente) gerente).getIdRestaurante();
        
        // 2. Filtra todos os funcionários desse restaurante
        List<Funcionario> equipa = new ArrayList<>();
        for (Funcionario f : this.funcionarios.values()) {
            if (f instanceof Trabalhador) {
                Trabalhador t = (Trabalhador) f;
                if (t.getIdRestaurante() == idRestaurante) {
                    equipa.add(t);
                }
            } else if (f instanceof Gerente) {
                Gerente g = (Gerente) f;
                if (g.getIdRestaurante() == idRestaurante) {
                    equipa.add(g);
                }
            }
        }
        
        return equipa;
    }
    @Override
    public boolean contratarDireto(int id, String nome, String pass, String posto, int idRestaurante) {
        if (id <= 0 || this.funcionarios.containsKey(id)) return false;
        
        Trabalhador t = new Trabalhador(id, nome, pass);
        t.setPostoAtual(posto);
        t.setIdRestaurante(idRestaurante);
        // O put no Map atualiza automaticamente a Base de Dados via DAO
        this.funcionarios.put(id, t);
        return true;
    }

    @Override
    public boolean despedir(int idAlvo) {
        // O remove no Map executa o DELETE na BD
        return this.funcionarios.remove(idAlvo) != null;
    }

    @Override
    public boolean realocar(int id, String novoPosto) {
        Funcionario f = this.funcionarios.get(id);
        if (f instanceof Trabalhador) {
            ((Trabalhador) f).setPostoAtual(novoPosto);
            this.funcionarios.put(id, f); // Update no Map e BD
            return true;
        }
        return false;
    }

    @Override
    public boolean pedirContratacao(int idGerente, String nome, String pass, String posto) {
        int idSol = getProximoIdSolicitacao();
        
        // CORREÇÃO: Necessário obter o nome do gerente para o construtor de 8 parâmetros
        Funcionario g = this.funcionarios.get(idGerente);
        String nomeG = (g != null) ? g.getNome() : "Gerente Desconhecido";
        
        // Construtor: id, idGerente, nomeGerente, tipo, idAlvo, nomeCandidato, passCandidato, posto
        Solicitacao sol = new Solicitacao(idSol, idGerente, nomeG, "CONTRATAR", 0, nome, pass, posto);
        this.solicitacoes.put(idSol, sol);
        return true;    
    }

    @Override
    public boolean pedirDespedimento(int idGerente, int idAlvo) {
        if (idAlvo == 1 || idAlvo == idGerente) return false;
        int idSol = getProximoIdSolicitacao();

        Funcionario g = this.funcionarios.get(idGerente);
        String nomeG = (g != null) ? g.getNome() : "Gerente Desconhecido";

        // CORREÇÃO: Passar os 8 parâmetros, usando null para os campos de contratação
        Solicitacao sol = new Solicitacao(idSol, idGerente, nomeG, "DESPEDIR", idAlvo, null, null, null);
        this.solicitacoes.put(idSol, sol);
        return true;
    }

    @Override
    public List<Solicitacao> verPendentes() {
        return new ArrayList<>(this.solicitacoes.values());
    }

    @Override
    public boolean tratarSolicitacao(int idSolicitacao, boolean aprovar) {
        Solicitacao sol = this.solicitacoes.get(idSolicitacao);
        if (sol == null) return false;

        if (!aprovar) {
            this.solicitacoes.remove(idSolicitacao);
            return true;
        }

        boolean sucesso = false;
        switch (sol.getTipo()) {
            case "CONTRATAR":
                // CORREÇÃO: Buscar o próximo ID sequencial da base de dados
                int novoId = getProximoIdTrabalhador();
                
                Trabalhador novo = new Trabalhador(novoId, sol.getNomeCandidato(), sol.getPassCandidato());
                novo.setPostoAtual(sol.getPosto());
                
                // Define o restaurante destino baseado em quem pediu
                int idRest = RestauranteDAO.getInstance().getRestauranteDoFuncionario(sol.getIdGerente());
                novo.setIdRestaurante(idRest);

                this.funcionarios.put(novoId, novo);
                sucesso = true;
                break;

            case "DESPEDIR":
                sucesso = this.funcionarios.remove(sol.getIdAlvo()) != null;
                break;

            case "REALOCAR":
                sucesso = this.realocar(sol.getIdAlvo(), sol.getPosto());
                break;
        }

        if (sucesso) this.solicitacoes.remove(idSolicitacao);
        return sucesso;
    }

        private int getProximoIdTrabalhador() {
        String sql = "SELECT MAX(id) AS max_id FROM Trabalhador";
        try (java.sql.Connection conn = project.data.ConexaoDB.getConexao();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
             java.sql.ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                int maxId = rs.getInt("max_id");
                return maxId + 1; // Próximo ID sequencial
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return 200; // Valor padrão caso a tabela esteja vazia
    }

        private int getProximoIdSolicitacao() {
        String sql = "SELECT MAX(id) AS max_id FROM Solicitacao_RH";
        try (java.sql.Connection conn = project.data.ConexaoDB.getConexao();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
             java.sql.ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                int maxId = rs.getInt("max_id");
                if (maxId > 0) return maxId + 1;
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return 1; // Primeira solicitação se a tabela estiver vazia
    }
}