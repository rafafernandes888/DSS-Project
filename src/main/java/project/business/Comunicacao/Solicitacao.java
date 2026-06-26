package project.business.Comunicacao;

/**
 * Representa um pedido de RH feito por um Gerente à COO.
 * Atualizada para incluir o idGerente, necessário para o processamento da aprovação.
 */
public class Solicitacao {
    private int id;
    private int idGerente;    // NOVO: Necessário para saber a qual restaurante alocar
    private String nomeGerente;
    private String tipo;      // CONTRATAR, DESPEDIR
    
    // Dados para execução (guardados para usar na aprovação)
    private int idAlvo;         
    private String nomeCandidato;
    private String passCandidato;
    private String posto;

    // Construtor atualizado com idGerente
    public Solicitacao(int id, int idGerente, String nomeGerente, String tipo, int idAlvo, String nomeCand, String passCand, String posto) {
        this.id = id;
        this.idGerente = idGerente; // Inicialização do novo campo
        this.nomeGerente = nomeGerente;
        this.tipo = tipo;
        this.idAlvo = idAlvo;
        this.nomeCandidato = nomeCand;
        this.passCandidato = passCand;
        this.posto = posto;
    }

    public String getDescricaoFormatada() {
        switch (this.tipo) {
            case "CONTRATAR":
                return "Admissão: " + nomeCandidato + " (" + posto + ")";
            case "DESPEDIR":
                return "Despedimento: ID Func. " + idAlvo;
            case "REALOCAR":
                return "Mudar ID " + idAlvo + " para " + posto;
            default:
                return "Solicitação genérica";
        }
    }

    // --- GETTERS ---
    public int getId() { return id; }
    
    // NOVO: Método que o GestorRH estava a pedir (getIdGerente)
    public int getIdGerente() { return idGerente; } 
    
    public String getNomeGerente() { return nomeGerente; }
    public String getTipo() { return tipo; }
    public int getIdAlvo() { return idAlvo; }
    public String getNomeCandidato() { return nomeCandidato; }
    public String getPassCandidato() { return passCandidato; }
    public String getPosto() { return posto; }

    @Override
    public String toString() {
        return String.format("Pedido #%d [%s] de Gerente %s (#%d) -> %s", 
                             id, tipo, nomeGerente, idGerente, getDescricaoFormatada());
    }
}