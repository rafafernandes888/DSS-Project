package project.business.Operacoes;

public class LinhaPedido {
    private Artigo artigo;
    private String personalizacao;
    private boolean pagoComPontos; // NOVO: Define se o item é uma recompensa de pontos

    // Construtor atualizado para aceitar o modo de pagamento
    public LinhaPedido(Artigo artigo, String personalizacao, boolean pagoComPontos) {
        this.artigo = artigo;
        this.personalizacao = (personalizacao == null) ? "" : personalizacao;
        this.pagoComPontos = pagoComPontos;
    }

    public Artigo getArtigo() { return artigo; }
    public String getPersonalizacao() { return personalizacao; }
    public boolean isPagoComPontos() { return pagoComPontos; }
    
    /**
     * Retorna apenas o preço base do artigo (sem extras).
     */
    public double getPreco() { return artigo.getPreco(); }

    /**
     * NOVO: Calcula quanto custa este artigo em pontos.
     * Regra: 1€ = 10 pontos.
     */
    public int getCustoEmPontos() {
        return (int) (artigo.getPreco() * 10);
    }

    /**
     * Calcula o preço total desta linha em DINHEIRO.
     * Se estiver marcado como 'pagoComPontos', o custo financeiro é ZERO.
     */
    public double getPrecoTotal() {
        if (pagoComPontos) return 0.0;
        
        double total = artigo.getPreco();
        double creditos = 0.0;
        double extras = 0.0;
        String p = personalizacao.toUpperCase();

        // Percorre os ingredientes que fazem parte da receita deste artigo
        for (Ingrediente ing : artigo.getIngredientes()) {
            String nomeIng = ing.getNome().toUpperCase();
            
            // Se o cliente retirou um ingrediente da receita original, gera CRÉDITO
            if (p.contains("SEM " + nomeIng)) {
                creditos += ing.getPrecoVenda();
            }
            
            // Se o cliente pediu dose EXTRA de um ingrediente da receita, gera CUSTO
            if (p.contains("EXTRA " + nomeIng)) {
                extras += ing.getPrecoVenda();
            }
        }

        // O ajuste é a diferença entre o que foi adicionado e o que foi retirado
        double ajuste = extras - creditos;

        // Regra: O preço nunca baixa do valor base do artigo (ajuste mínimo de 0)
        return total + Math.max(0, ajuste);
    }

    @Override
    public String toString() {
        // Mostra o preço em dinheiro ou a indicação de que foi pago com pontos
        String infoPreco = pagoComPontos ? "[PAGO COM PONTOS]" : String.format("(%.2f€)", getPrecoTotal());
        String nota = personalizacao.isEmpty() ? "" : " [" + personalizacao + "]";
        return artigo.getNome() + " " + infoPreco + nota;
    }
}