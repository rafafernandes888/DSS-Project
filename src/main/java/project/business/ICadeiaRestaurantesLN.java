package project.business;

import java.util.List;
import java.util.Map; // Necessário para o inventário

import project.business.Comunicacao.*;
import project.business.Operacoes.*;
import project.business.RH.*;

public interface ICadeiaRestaurantesLN {
    
    // --- AUTENTICAÇÃO E UTILITÁRIOS ---
    Funcionario autenticarFuncionario(int id, String password);
    int getRestauranteDoFuncionario(int idFuncionario);
    boolean verificarConexao();

    // --- CLIENTE E PEDIDOS ---
    Cliente autenticarOuRegistarCliente(String nif);
    List<Artigo> getArtigosPorCategoria(String categoria);
    int registarPedido(Pedido p); // Retorna a senha (>0), -2 se faltar stock, ou -1 erro

    // --- COZINHA E STAFF ---
    List<Pedido> listarPedidosAtivos(int idRestaurante);
    List<LinhaPedido> getDetalhesDoPedido(int idPedido);
    boolean atualizarEstadoPedido(int idPedido, EstadoPedido novoEstado);
    
    // MÉTODO PARA CONSULTA DE STOCK
    Map<String, Integer> consultarStock(int idRestaurante);

    // --- GESTÃO DE RECURSOS HUMANOS (RH) ---
    List<Funcionario> listarFuncionariosGlobal(); 
    List<Funcionario> listarFuncionariosDoRestaurante(int idGerente);
    
    // Ações Diretas (COO)
    boolean contratarTrabalhador(int id, String nome, String pass, String posto, int idRestaurante);
    boolean despedirFuncionario(int idAlvo);
    boolean realocarTrabalhador(int id, String novoPosto);

    // Solicitações (Gerente -> COO)
    boolean solicitarContratacao(int idGerente, String nome, String pass, String posto);
    boolean solicitarDespedimento(int idGerente, int idAlvo);
    List<Solicitacao> listarSolicitacoesPendentes();
    boolean tratarSolicitacao(int idSolicitacao, boolean aprovar);

    // --- COMUNICAÇÃO E GESTÃO DE REDE ---
    List<Restaurante> listarRestaurantes();
    boolean enviarMensagem(int idRestaurante, String mensagem);
    List<String> lerMensagens(int idRestaurante, int idTrabalhador);
    int contarNotificacoes(int idRestaurante, int idTrabalhador);

    List<Ingrediente> getTodosIngredientes();
    int getStockArmazem (int idRestaurante, int idIngrediente);
    boolean encomendarStock(int idRestaurante, int idIngrediente, int quantidade);
    boolean adiarPedido(int idPedido, int minutos);

    // Adicione este método à interface
    int consultarPontos(String nif);
    Relatorio gerarRelatorio(int idRest, String nomeRest);

    boolean enviarMensagemGlobal(String conteudo);

    boolean enviarMensagem(int idRestaurante, String conteudo, boolean isPublic);

    List<String> listarAlertasPublicos(int idRestaurante);

    boolean limparAlertasDoPedido(int idRestaurante, int idPedido);
}