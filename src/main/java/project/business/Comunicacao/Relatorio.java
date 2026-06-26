package project.business.Comunicacao;

public class Relatorio {
    private String nomeRestaurante;
    private String nomeGerente;
    private double faturacaoTotal;
    private double tempoMedioEspera;

    public Relatorio(String nomeRestaurante, String nomeGerente, double faturacaoTotal, double tempoMedioEspera) {
        this.nomeRestaurante = nomeRestaurante;
        this.nomeGerente = (nomeGerente != null) ? nomeGerente : "Sem Gerente";
        this.faturacaoTotal = faturacaoTotal;
        this.tempoMedioEspera = tempoMedioEspera;
    }

    @Override
    public String toString() {
        return String.format("📍 %-20s | Gerente: %-15s | Vendas: %8.2f€ | ⏱️ Espera: %4.1f min", 
            nomeRestaurante, nomeGerente, faturacaoTotal, tempoMedioEspera);
    }
}