
package project.business.Operacoes;

import java.util.ArrayList;
import java.util.List;

public class Artigo {
    private int id;
    private String nome;
    private Categoria categoria;
    private double preco;
    private String descricao;
    private List<Ingrediente> ingredientes; // Lista de composição

    public Artigo(int id, String nome, Categoria categoria, double preco, String descricao) {
        this.id = id;
        this.nome = nome;
        this.categoria = categoria;
        this.preco = preco;
        this.descricao = descricao;
        this.ingredientes = new ArrayList<>();
    }

    public Artigo(String nome, Categoria categoria, double preco, String descricao) {
        this(0, nome, categoria, preco, descricao);
    }

    public void adicionarIngrediente(Ingrediente i) {
        this.ingredientes.add(i);
    }

    public List<Ingrediente> getIngredientes() {
        return new ArrayList<>(ingredientes);
    }

    public int getId() { return id; }
    public String getNome() { return nome; }
    public Categoria getCategoria() { return categoria; }
    public double getPreco() { return preco; }
    public String getDescricao() { return descricao; }

    private boolean podeCausarAlergia; 


    public boolean isPodeCausarAlergia() {
        return podeCausarAlergia;
    }

    public void setPodeCausarAlergia(boolean valor) {
        this.podeCausarAlergia = valor;
    }

    @Override
    public String toString() {
        return String.format("[%d] %-20s | %6.2f € | (%s)", id, nome, preco, descricao);
    }
}