package project.business;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import project.business.Comunicacao.*;
import project.business.Operacoes.*;
import project.business.RH.*;
import project.data.*; 

public class CadeiaRestaurantesFacade implements ICadeiaRestaurantesLN {

    private GestorRH gestorRH;
    private GestorOperacao gestorOps;
    private GestorComunicacao gestorCom;

    public CadeiaRestaurantesFacade() {
        // Os gestores são as classes de lógica. 
        // Eles internamente é que usam os DAOs via .getInstance()
        this.gestorRH = new GestorRH();
        this.gestorOps = new GestorOperacao();
        this.gestorCom = new GestorComunicacao();  
    }

    // ==========================================
    // 1. SUBSISTEMA RH (Delegação Total)
    // ==========================================
    @Override
    public Funcionario autenticarFuncionario(int id, String password) {
        return gestorRH.autenticar(id, password);
    }
    @Override
    public List<Funcionario> listarFuncionariosGlobal() {
        return gestorRH.listarGlobal();
    }
    @Override
    public List<Funcionario> listarFuncionariosDoRestaurante(int idGerente) {
        return gestorRH.listarLocal(idGerente);
    }
    @Override
    public boolean contratarTrabalhador(int id, String nome, String pass, String posto, int idRestaurante) {
        return gestorRH.contratarDireto(id, nome, pass, posto, idRestaurante);
    }
    @Override
    public boolean despedirFuncionario(int idAlvo) {
        return gestorRH.despedir(idAlvo);
    }
    @Override
    public boolean realocarTrabalhador(int id, String novoPosto) {
        return gestorRH.realocar(id, novoPosto);
    }
    @Override
    public boolean solicitarContratacao(int idGerente, String nome, String pass, String posto) {
        return gestorRH.pedirContratacao(idGerente, nome, pass, posto);
    }
    @Override
    public boolean solicitarDespedimento(int idGerente, int idAlvo) {
        return gestorRH.pedirDespedimento(idGerente, idAlvo);
    }
    @Override
    public List<Solicitacao> listarSolicitacoesPendentes() {
        return gestorRH.verPendentes();
    }
    @Override
    public boolean tratarSolicitacao(int idSolicitacao, boolean aprovar) {
        return gestorRH.tratarSolicitacao(idSolicitacao, aprovar);
    }

    // ==========================================
    // 2. SUBSISTEMA OPERAÇÕES (Delegação Total)
    // ==========================================
    @Override
    public Cliente autenticarOuRegistarCliente(String nif) {
        return gestorOps.autenticarCliente(nif);
    }

    @Override
    public List<Artigo> getArtigosPorCategoria(String categoria) {
        return gestorOps.getArtigos(categoria);
    }

    @Override
    public int registarPedido(Pedido p) {
        return gestorOps.registarPedido(p);
    }

    @Override
    public List<Pedido> listarPedidosAtivos(int idRestaurante) {
        return gestorOps.listarAtivos(idRestaurante);
    }

    @Override
    public List<LinhaPedido> getDetalhesDoPedido(int idPedido) {
        return gestorOps.getDetalhes(idPedido);
    }

    @Override
    public boolean atualizarEstadoPedido(int idPedido, EstadoPedido novoEstado) {
        return gestorOps.atualizarEstado(idPedido, novoEstado);
    }

    @Override
    public Map<String, Integer> consultarStock(int idRestaurante) {
        // Lógica movida para o Gestor para manter a Facade limpa
        return gestorOps.consultarStock(idRestaurante);
    }

    @Override
    public int consultarPontos(String nif) {
        return gestorOps.consultarPontos(nif);
    }

    @Override
    public List<Ingrediente> getTodosIngredientes() {
        return gestorOps.getTodosIngredientes();
    }

    @Override
    public int getStockArmazem(int idRestaurante, int idIngrediente) {
        return gestorOps.getStock(idRestaurante, idIngrediente);
    }

    @Override
    public boolean encomendarStock(int idRestaurante, int idIngrediente, int quantidade) {
        return gestorOps.encomendarStock(idRestaurante, idIngrediente, quantidade);
    }

    @Override
    public boolean adiarPedido(int idPedido, int minutos) {
        return gestorOps.adiarPedido(idPedido, minutos);
    }

    @Override
    public Relatorio gerarRelatorio(int idRest, String nomeRest) {
        return gestorOps.getRelatorio(idRest, nomeRest);
    }

    // ==========================================
    // 3. SUBSISTEMA COMUNICAÇÃO
    // ==========================================
    @Override
    public List<Restaurante> listarRestaurantes() {
        return gestorCom.listarTodosRestaurantes();
    }

    @Override
    public boolean enviarMensagem(int idRestaurante, String conteudo, boolean isPublic) {
        return gestorCom.enviar(idRestaurante, conteudo, isPublic);
    }

    @Override
    public boolean enviarMensagem(int idRestaurante, String mensagem) {
        return this.enviarMensagem(idRestaurante, mensagem, false);
    }

    @Override
    public List<String> lerMensagens(int idRestaurante, int idTrabalhador) {
        return gestorCom.ler(idRestaurante, idTrabalhador);
    }

    @Override
    public int contarNotificacoes(int idRestaurante, int idTrabalhador) {
        return gestorCom.contar(idRestaurante, idTrabalhador);
    }

    @Override
    public int getRestauranteDoFuncionario(int idFuncionario) {
        return gestorCom.getRestauranteFuncionario(idFuncionario);
    }
    
    @Override
    public boolean verificarConexao() {
        return project.data.ConexaoDB.testarConexao();
    }

    @Override
    public boolean enviarMensagemGlobal(String conteudo) {
        return gestorCom.enviarGlobal(conteudo, false);
    }

    @Override
    public List<String> listarAlertasPublicos(int idRestaurante) {
        return gestorCom.listarAlertasPublicos(idRestaurante);
    }

    @Override
    public boolean limparAlertasDoPedido(int idRestaurante, int idPedido) {
        return gestorCom.limparAlertasDoPedido(idRestaurante, idPedido);
    }
}