package project.business.Comunicacao;

import project.business.Operacoes.Restaurante;
import project.data.RestauranteDAO;
import java.util.*;

public class GestorComunicacao implements IGestorComunicacao {

    private Map<Integer, Restaurante> restaurantes;

    public GestorComunicacao() {
        this.restaurantes = RestauranteDAO.getInstance();
    }

    @Override
    public List<Restaurante> listarTodosRestaurantes() {
        return new ArrayList<>(this.restaurantes.values());
    }
    
    @Override
    public int getRestauranteFuncionario(int idFuncionario) {
        // Aqui mantemos o cast para performance (evita carregar todos os restaurantes para procurar um ID).
        // Num cenário puramente memória, teríamos de iterar a lista .values()
        return ((RestauranteDAO) this.restaurantes).getRestauranteDoFuncionario(idFuncionario);
    }

    @Override
    public boolean enviar(int idRestaurante, String msg, boolean isPublic) {
        if (msg == null || msg.trim().isEmpty()) return false;

        Restaurante r = this.restaurantes.get(idRestaurante);
        
        if (r != null) {
            // 2. LÓGICA DE DOMÍNIO: O objeto altera-se na memória
            r.adicionarMensagem(msg, isPublic);

            // 3. PUT: Guardamos o estado alterado
            // O DAO deteta as mensagens novas na lista e faz o INSERT
            this.restaurantes.put(idRestaurante, r);
            return true;
        }
        return false;
    }

    @Override
    public List<String> listarAlertasPublicos(int idRestaurante) {
        Restaurante r = this.restaurantes.get(idRestaurante);
        if (r != null) {
            // Delega ao objeto a responsabilidade de filtrar as suas mensagens
            return r.getMensagens(true); 
        }
        return new ArrayList<>();
    }

    @Override
    public int contar(int idRestaurante, int idTrabalhador) { 
        Restaurante r = this.restaurantes.get(idRestaurante);
        if (r != null) {
            return r.contarNotificacoesNaoLidas(idTrabalhador);
        }
        return 0;
    }

    @Override
    public List<String> ler(int idRestaurante, int idTrabalhador) { 
        Restaurante r = this.restaurantes.get(idRestaurante);
        if (r != null) {
            return r.getMensagens(false);
        }
        return new ArrayList<>();
    }

    @Override
    public boolean enviarGlobal(String msg, boolean isPublic) {
        if (msg == null || msg.trim().isEmpty()) return false;
        
        // Padrão Puro: Percorremos o mapa e atualizamos cada objeto
        boolean sucesso = false;
        for (Restaurante r : this.restaurantes.values()) {
            r.adicionarMensagem(msg, isPublic);
            this.restaurantes.put(r.getId(), r); // Grava na BD individualmente
            sucesso = true;
        }
        return sucesso;
    }

    @Override
    public boolean limparAlertasDoPedido(int idRestaurante, int idPedido) {
        return ((RestauranteDAO) this.restaurantes).limparAlertasDoPedido(idRestaurante, idPedido);
    }
}