package project.business.Operacoes;

import java.util.List;
import java.util.Map;
import project.business.Comunicacao.*;
import project.business.RH.*;

public interface IGestorOperacao{
    Cliente autenticarCliente(String nif);
    List<Artigo> getArtigos(String categoria);
    void forcarAtualizacaoMenu();
    int registarPedido(Pedido p);
    int consultarPontos(String nif);
    boolean adiarPedido(int idPedido, int minutos);
    List<Pedido> listarAtivos(int idRestaurante);
    List<LinhaPedido> getDetalhes(int idPedido);
    boolean atualizarEstado(int idPedido, EstadoPedido novoEstado);
    Relatorio getRelatorio(int idRest, String nomeRest);
    Map<String, Integer> consultarStock(int idRestaurante);
    List<Ingrediente> getTodosIngredientes();
    int getStock(int idRest, int idIng);
}