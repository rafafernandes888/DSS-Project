package project.business.Operacoes;

import java.util.ArrayList;
import java.util.List;

public class Carrinho {
    private List<LinhaPedido> itens;

    public Carrinho() {
        this.itens = new ArrayList<>();
    }

    /**
     * Adiciona um item ao carrinho, especificando se será pago com pontos ou dinheiro.
     */
    public void adicionarItem(Artigo a, String personalizacao, boolean pagoComPontos) {
        this.itens.add(new LinhaPedido(a, personalizacao, pagoComPontos));
    }

    public void removerItem(int index) {
        if (index >= 0 && index < itens.size()) {
            this.itens.remove(index);
        }
    }

    public List<LinhaPedido> getItens() {
        return new ArrayList<>(this.itens);
    }

    /**
     * Calcula o total que o cliente tem de pagar em DINHEIRO (EUR).
     * Itens pagos com pontos contam como 0.0€ aqui.
     */
    public double getTotal() {
        return itens.stream()
                    .mapToDouble(LinhaPedido::getPrecoTotal)
                    .sum();
    }

    /**
     * Calcula o total de PONTOS que serão retirados do saldo do cliente.
     */
    public int getTotalPontosADescontar() {
        return itens.stream()
                    .filter(LinhaPedido::isPagoComPontos)
                    .mapToInt(LinhaPedido::getCustoEmPontos)
                    .sum();
    }

    public boolean estaVazio() {
        return itens.isEmpty();
    }
}