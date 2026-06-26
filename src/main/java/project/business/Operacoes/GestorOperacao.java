package project.business.Operacoes;

import project.business.Comunicacao.*;
import project.data.PedidoDAO;
import project.data.ClienteDAO;
import project.data.ArtigoDAO; 
import project.data.StockDAO;
import project.data.IngredienteDAO; // Adicionado import necessário
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.Map; // Adicionado para suportar Maps
import java.util.HashMap; // Adicionado para suportar Maps

/**
 * Gestor de Operações refatorado para seguir as boas práticas de DSS:
 * - Acesso a DAOs via Singleton (.getInstance())
 * - Utilização de DAOs como Map para abstração total
 */
public class GestorOperacao implements IGestorOperacao {

    // --- DAOs DECLARADOS COMO MAPS (Padrão Nota 20) ---
    private Map<Integer, Pedido> pedidos;
    private Map<Integer, Cliente> clientes;
    private Map<Integer, Artigo> artigos;
    private Map<Integer, Ingrediente> ingredientes;
    private StockDAO stockDAO; // Mantém-se como DAO (chave composta)
    private Carta carta;

    public GestorOperacao() {
        // 1. Inicialização via Singleton atribuída aos Maps
        this.pedidos = PedidoDAO.getInstance(); 
        this.clientes = ClienteDAO.getInstance();
        this.artigos = ArtigoDAO.getInstance();
        this.ingredientes = IngredienteDAO.getInstance();
        this.stockDAO = StockDAO.getInstance();
        
        // 2. Carregar dados da BD usando a interface Map do ArtigoDAO
        // O método .values() substitui o antigo .listarTodos()
        List<Artigo> dadosDoMenu = new ArrayList<>(this.artigos.values());
        
        // 3. Inicializar a Carta em memória
        this.carta = new Carta(dadosDoMenu);
    }

    // --- CLIENTES ---
    public Cliente autenticarCliente(String nif) {
        if (nif == null || nif.trim().isEmpty()) {
            return null; 
        }
        // Uso de Cast para aceder ao método específico do DAO
        return ((ClienteDAO) this.clientes).autenticarOuRegistar(nif);
    }

    // --- CARTA / MENU ---
    public List<Artigo> getArtigos(String categoria) {
        try {
            return switch (categoria.toUpperCase()) {
                case "MENU" -> carta.getMenusCompletos();
                case "HAMBURGUER" -> carta.getHamburgueres();
                case "ACOMPANHAMENTO" -> carta.getAcompanhamentos();
                case "BEBIDA" -> carta.getBebidas();
                default -> new ArrayList<>();
            };
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    public void forcarAtualizacaoMenu() {
        // Atualiza a carta obtendo os valores mais recentes do Map ArtigoDAO
        List<Artigo> novosDados = new ArrayList<>(this.artigos.values());
        this.carta.setArtigos(novosDados);
    }

    // --- PEDIDOS COM VALIDAÇÃO REAL DE STOCK (SEM/EXTRA) ---
    public int registarPedido(Pedido p) {
        // 1. Validação Crítica de Integridade
        if (p.getItens().isEmpty() || p.getIdRestaurante() <= 0) {
            return -1; 
        }

        // 2. REGRA DE NEGÓCIO: VERIFICAÇÃO PRÉVIA DE STOCK COM PERSONALIZAÇÃO
        for (LinhaPedido linha : p.getItens()) {
            String perso = linha.getPersonalizacao().toUpperCase();
            
            for (Ingrediente ing : linha.getArtigo().getIngredientes()) {
                String nomeIng = ing.getNome().toUpperCase();

                if (perso.contains("SEM " + nomeIng)) {
                    continue;
                }

                int qtdNecessaria = perso.contains("EXTRA " + nomeIng) ? 2 : 1;

                if (stockDAO.getQuantidade(p.getIdRestaurante(), ing.getId()) < qtdNecessaria) {
                    System.err.println("❌ FALTA DE STOCK no Restaurante #" + p.getIdRestaurante() + ": " + ing.getNome());
                    return -2; 
                }
            }
        }

        // 3. Configurações de Estado e Defaults
        if (p.getEstado() == null) {
            p.setEstado(EstadoPedido.REGISTADO);
        }
        if (p.getMetodoPagamento() == null) {
            p.setMetodoPagamento("DINHEIRO");
        }
        if (p.getLocalConsumo() == null) {
            p.setLocalConsumo("LOCAL"); 
        }

        int tempoFila = ((PedidoDAO) this.pedidos).getSomaTemposEspera(p.getIdRestaurante());
        p.setTempoEstimado(p.getTempoEstimado() + tempoFila);

        int idPedido = ((PedidoDAO) this.pedidos).criarPedido(p);

        if (idPedido > 0) {
            // A) Abater Stock Inteligente
            for (LinhaPedido linha : p.getItens()) {
                String perso = linha.getPersonalizacao().toUpperCase();
                
                for (Ingrediente ing : linha.getArtigo().getIngredientes()) {
                    String nomeIng = ing.getNome().toUpperCase();

                    if (perso.contains("SEM " + nomeIng)) {
                        continue; 
                    }

                    int unidadesAAbater = perso.contains("EXTRA " + nomeIng) ? 2 : 1;
                    stockDAO.baixarStock(p.getIdRestaurante(), ing.getId(), unidadesAAbater);
                }
            }

            // B) GESTÃO DE PONTOS (Fidelização)
            if (p.getCliente() != null && !p.getCliente().getNif().equals("N/A") && !p.getCliente().getNif().isEmpty()) {
                
                int pontosGastos = p.getTotalPontosGastos(); 
                int pontosGanhos = (int) p.getValorTotal(); 
                
                int novoSaldo = p.getCliente().getPontos() - pontosGastos + pontosGanhos;
                
                // Atualiza o objeto em memória
                p.getCliente().setPontos(novoSaldo);
                
                // Atualiza na BD via Cast
                ((ClienteDAO) this.clientes).atualizarPontos(p.getCliente().getId(), novoSaldo);
            }
        }

        return idPedido;
    }

    public int consultarPontos(String nif) {
        if (nif == null || nif.trim().isEmpty()) return -1;
        
        Cliente c = ((ClienteDAO) this.clientes).autenticarOuRegistar(nif);
        return (c != null) ? c.getPontos() : -1;
    }

    public boolean adiarPedido(int idPedido, int minutos) {
        return ((PedidoDAO) this.pedidos).adiarPedido(idPedido, minutos);
    }

    public List<Pedido> listarAtivos(int idRestaurante) {
        return ((PedidoDAO) this.pedidos).listarPedidosAtivos(idRestaurante);
    }

    public List<LinhaPedido> getDetalhes(int idPedido) {
        return ((PedidoDAO) this.pedidos).getItensDoPedido(idPedido);
    }

    public boolean atualizarEstado(int idPedido, EstadoPedido novoEstado) {
        return ((PedidoDAO) this.pedidos).atualizarEstado(idPedido, novoEstado);
    }

    public Relatorio getRelatorio(int idRest, String nomeRest) {
        return ((PedidoDAO) this.pedidos).getRelatorioRestaurante(idRest, nomeRest);
    }

    public Map<String, Integer> consultarStock(int idRestaurante) {
        Map<String, Integer> stockMap = new HashMap<>();
        // Usa o método já existente no gestor para obter ingredientes dos hambúrgueres
        List<Artigo> hamburgueres = this.getArtigos("HAMBURGUER");
        for (Artigo a : hamburgueres) {
            for (Ingrediente ing : a.getIngredientes()) {
                if (!stockMap.containsKey(ing.getNome())) {
                    int qtd = stockDAO.getQuantidade(idRestaurante, ing.getId());
                    stockMap.put(ing.getNome(), qtd);
                }
            }
        }
        return stockMap;
    }

    public List<Ingrediente> getTodosIngredientes() {
        // Utiliza o Map ingredientes diretamente
        return new ArrayList<>(this.ingredientes.values());
    }

    public int getStock(int idRest, int idIng) {
        return stockDAO.getQuantidade(idRest, idIng);
    }

    public boolean encomendarStock(int idRestaurante, int idIngrediente, int quantidade) {
        return stockDAO.aumentarStock(idRestaurante, idIngrediente, quantidade);
    }
}