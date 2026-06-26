package project.business.RH;

import java.util.List;
import project.business.Comunicacao.Solicitacao;

public interface IGestorRH {
    Funcionario autenticar(int id, String pass);
    List<Funcionario> listarGlobal();
    List<Funcionario> listarLocal(int idGerente);
    boolean contratarDireto(int id, String nome, String pass, String posto, int idRestaurante);
    boolean despedir(int idAlvo);
    boolean realocar(int id, String novoPosto);
    boolean pedirContratacao(int idGerente, String nome, String pass, String posto);
    boolean pedirDespedimento(int idGerente, int idAlvo);
    List<Solicitacao> verPendentes();
    boolean tratarSolicitacao(int idSolicitacao, boolean aprovar);
}