package project.business.Operacoes;

public class Ingrediente {
    private int id;
    private String nome;
    private boolean isAlergenio;
    private String tipoAlergenio; // Ex: "GLÚTEN", "LACTOSE", "OVOS"
    private double precoVenda;    // NOVO: Preço individual do ingrediente

    // Construtor atualizado para receber o preço vindo da Base de Dados
    public Ingrediente(int id, String nome, boolean isAlergenio, double precoVenda) {
        this.id = id;
        this.nome = nome;
        this.isAlergenio = isAlergenio;
        this.precoVenda = precoVenda;

        // Lógica de classificação de alergénios mantida
        String n = nome.toLowerCase();
        if (n.contains("pão")) this.tipoAlergenio = "GLÚTEN";
        else if (n.contains("queijo") || n.contains("mix")) this.tipoAlergenio = "LACTOSE";
        else if (n.contains("ovo")) this.tipoAlergenio = "OVOS";
        else this.tipoAlergenio = "NENHUM";
    }

    public int getId() { return id; }
    public String getNome() { return nome; }
    public boolean isAlergenio() { return isAlergenio; }
    public String getTipoAlergenio() { return tipoAlergenio; }
    
    // NOVO: Getter para o preço
    public double getPrecoVenda() { 
        return precoVenda; 
    }

    @Override
    public String toString() {
        return nome + (isAlergenio ? " (CONTÉM " + tipoAlergenio + ")" : "");
    }
}