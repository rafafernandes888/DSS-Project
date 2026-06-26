package project.business.Operacoes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Pedido {
    private int id;
    private LocalDateTime dataHora;
    private EstadoPedido estado;
    private Cliente cliente;
    private int idRestaurante;
    
    private List<LinhaPedido> itens; 
    private String metodoPagamento;
    private String localConsumo;
    private int tempoEstimado;
    private LocalDateTime dataConclusao;
    
    // NOVO: Campo para armazenar o valor snapshot vindo da BD
    private double valorTotal;

    public Pedido(Cliente cliente) {
        this.cliente = cliente;
        this.itens = new ArrayList<>();
        this.estado = EstadoPedido.REGISTADO;
        this.dataHora = LocalDateTime.now();
        this.tempoEstimado = 0; 
        this.valorTotal = 0.0;
    }

    /**
     * Adiciona um item ao pedido.
     */
    public void adicionarItem(Artigo a, String personalizacao, boolean pagoComPontos) {
        this.itens.add(new LinhaPedido(a, personalizacao, pagoComPontos));
        this.tempoEstimado += 2; 
    }

    /**
     * Calcula o total financeiro (em Euros) do pedido.
     * Ajustado para suportar o valor carregado pelo DAO.
     */
    public double getValorTotal() {
        // Se houver itens, calculamos dinamicamente (para novos pedidos)
        if (itens != null && !itens.isEmpty()) {
            return itens.stream().mapToDouble(LinhaPedido::getPrecoTotal).sum();
        }
        // Caso contrário, devolvemos o valor estático guardado (para histórico/DAO)
        return this.valorTotal;
    }

    // NOVO: Método necessário para o PedidoDAO compilar
    public void setValorTotal(double valorTotal) {
        this.valorTotal = valorTotal;
    }

    /**
     * Calcula o total de pontos que este pedido vai descontar do cliente.
     */
    public int getTotalPontosGastos() {
        return itens.stream()
                    .filter(LinhaPedido::isPagoComPontos)
                    .mapToInt(LinhaPedido::getCustoEmPontos)
                    .sum();
    }

    // --- GETTERS E SETTERS ---

    public String getMetodoPagamento() { return metodoPagamento; }
    public void setMetodoPagamento(String mp) { this.metodoPagamento = mp; }
    
    public String getLocalConsumo() { return localConsumo; }
    public void setLocalConsumo(String lc) { this.localConsumo = lc; }
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public EstadoPedido getEstado() { return estado; }
    public void setEstado(EstadoPedido estado) { this.estado = estado; }

    public List<LinhaPedido> getItens() { return itens; }
    public void setItens(List<LinhaPedido> itens) { this.itens = itens; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public int getTempoEstimado() { return tempoEstimado; }
    public void setTempoEstimado(int tempo) { this.tempoEstimado = tempo; }

    

    public LocalDateTime getDataHora() { return dataHora; }   
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }

    public int getIdRestaurante() { return idRestaurante; }
    public void setIdRestaurante(int idRestaurante) { this.idRestaurante = idRestaurante; }

    @Override
    public String toString() {
        String infoCliente = (this.cliente != null) ? "NIF: " + this.cliente.getNif() : "Anonimo";
        String consumo = (this.localConsumo != null) ? this.localConsumo : "N/D";
        
        int pts = getTotalPontosGastos();
        String infoPts = (pts > 0) ? " | Pontos Gastos: " + pts : "";
        
        return String.format("Pedido #%d [%s] - %s | %s | Valor: %.2f€%s | Tempo: %d min", 
                            this.id, this.estado, consumo, infoCliente, this.getValorTotal(), infoPts, this.tempoEstimado);
    }
}