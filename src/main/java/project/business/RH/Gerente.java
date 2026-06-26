package project.business.RH;

/**
 * Classe que representa um Gerente.
 * Estende Funcionario e inclui a ligação ao restaurante que gere.
 */
public class Gerente extends Funcionario {
    
    private int idRestaurante; // Campo necessário para a lógica de equipa no GestorRH

    // Construtor atualizado para incluir o idRestaurante
    public Gerente(int id, String nome, String pass, int idRestaurante) {
        super(id, nome, pass);
        this.idRestaurante = idRestaurante;
    }

    /**
     * @return O ID do restaurante associado a este gerente.
     * Este método resolve o erro "cannot find symbol getIdRestaurante()" no GestorRH.
     */
    public int getIdRestaurante() {
        return this.idRestaurante;
    }

    public void setIdRestaurante(int idRestaurante) {
        this.idRestaurante = idRestaurante;
    }

    @Override
    public String getTipoCargo() {
        return "GERENTE";
    }
}