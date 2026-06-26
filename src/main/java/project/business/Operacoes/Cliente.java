package project.business.Operacoes;

public class Cliente {
    private int id;
    private String nif;
    private int pontos;

    public Cliente(int id, String nif, int pontos) {
        this.id = id;
        this.nif = (nif == null || nif.isEmpty()) ? "N/A" : nif;
        this.pontos = pontos;
    }

    public int getId() { 
        return id; 
    }
    
    public String getNif() { 
        return nif; 
    }

    public int getPontos() {
        return pontos;
    }

    public void setPontos(int pontos) {
        this.pontos = pontos;
    }
    
    @Override
    public String toString() {
        return "Cliente " + id + " (NIF: " + nif + " | Pontos: " + pontos + ")";
    }
}