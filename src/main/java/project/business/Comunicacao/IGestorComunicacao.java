package project.business.Comunicacao;

import java.util.List;
import project.business.Operacoes.Restaurante;

public interface IGestorComunicacao {
    List<Restaurante> listarTodosRestaurantes();
    int getRestauranteFuncionario(int idFuncionario);
    boolean enviar(int idRestaurante, String msg, boolean isPublic);
    List<String> listarAlertasPublicos(int idRestaurante);
    int contar(int idRestaurante, int idTrabalhador);
    List<String> ler(int idRestaurante, int idTrabalhador);
    boolean enviarGlobal(String msg, boolean isPublic);
    boolean limparAlertasDoPedido(int idRestaurante, int idPedido);
}