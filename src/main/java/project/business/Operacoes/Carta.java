package project.business.Operacoes;

import java.util.ArrayList;
import java.util.List;

public class Carta {
    private List<Artigo> todosOsArtigos;

    // O Construtor RECEBE a lista (Injeção de Dependência simples)
    public Carta(List<Artigo> artigos) {
        this.todosOsArtigos = artigos;
    }

    // Se precisares de atualizar a lista mais tarde
    public void setArtigos(List<Artigo> artigos) {
        this.todosOsArtigos = artigos;
    }

    // --- MÉTODOS DE FILTRAGEM (Lógica de Negócio Pura) --

    public List<Artigo> getMenusCompletos() {
        return filtrarPor(Categoria.MENU_COMPLETO); // Certifica-te que Categoria é visível
    }

    public List<Artigo> getHamburgueres() {
        return filtrarPor(Categoria.HAMBURGUER); // Se Categoria for Enum, usa Categoria.HAMBURGUER
    }

    public List<Artigo> getBebidas() {
         return filtrarPor(Categoria.BEBIDA);
    }
    
    public List<Artigo> getAcompanhamentos() {
         return filtrarPor(Categoria.ACOMPANHAMENTO);
    }

    private List<Artigo> filtrarPor(Categoria catAlvo) {
        List<Artigo> resultado = new ArrayList<>();
        for (Artigo a : this.todosOsArtigos) {
            if (a.getCategoria() == catAlvo) { 
                resultado.add(a);
            }
        }
        return resultado;
    }
    
    public Artigo getArtigoPorId(int id) {
        for (Artigo a : this.todosOsArtigos) {
            if (a.getId() == id) {
                return a;
            }
        }
        return null;
    }
}