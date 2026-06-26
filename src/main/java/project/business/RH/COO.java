package project.business.RH;

public class COO extends Funcionario {

    public COO(int id, String nome, String pass) {
        super(id, nome, pass);
    }

    @Override
    public String getTipoCargo() {
        return "COO"; 
    }

}