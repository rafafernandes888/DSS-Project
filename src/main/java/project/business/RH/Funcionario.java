package project.business.RH;

// Classe base (Pai)
public abstract class Funcionario {
    private int id;
    private String nome;
    private String pass;

    public Funcionario(int id, String nome, String pass) {
        this.id = id;
        this.nome = nome;
        this.pass = pass;
    }

    // Getters e Setters comuns
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id; 
    }

    public String getNome() { 
        return nome; 
    }
    public void setNome(String nome) {
        this.nome = nome; 
    }

    public String getPassword() { 
        return pass; 
    }
    public void setPassword(String pass) { 
        this.pass = pass; 
    }

    public abstract String getTipoCargo();

    @Override
    public String toString() {
        return getTipoCargo() + " #" + id + " | " + nome;
    }
}