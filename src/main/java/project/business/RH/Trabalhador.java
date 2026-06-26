package project.business.RH;

public class Trabalhador extends Funcionario {
    private String posto_atual;
    private int idRestaurante; 

    public Trabalhador(int id, String nome, String pass) {
        super(id, nome, pass);
        this.posto_atual = "Indefinido";
    }

    public String getPostoAtual() { 
        return posto_atual; 
    }
    public void setPostoAtual(String posto_atual) { 
        this.posto_atual = posto_atual; 
    }

    public int getIdRestaurante() {
        return idRestaurante;
    }

    public void setIdRestaurante(int idRestaurante) {
        this.idRestaurante = idRestaurante;
    }

    @Override
    public String getTipoCargo() { return "TRABALHADOR"; }

    @Override
    public String toString() {
        return super.toString() + " [" + posto_atual + "]";
    }
}