package project.business.Operacoes;

import project.business.RH.Gerente;
import java.util.*; // Necessário para as Listas

public class Restaurante {
    private int id;
    private String localizacao;
    private Gerente gerente; 

    // --- NOVOS ATRIBUTOS (A "Memória" do Restaurante) ---
    // Estas listas permitem que o objeto funcione sem Base de Dados (Pré-DAO)
    private List<String> inboxPrivada;
    private List<String> alertasPublicos;

    public Restaurante(int id, String localizacao, Gerente gerente) {
        this.id = id;
        this.localizacao = localizacao;
        this.gerente = gerente;
        
        // Inicializamos as listas vazias para evitar NullPointerException
        this.inboxPrivada = new ArrayList<>();
        this.alertasPublicos = new ArrayList<>();
    }

    // --- GETTERS E SETTERS BÁSICOS ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getLocalizacao() { return localizacao; }
    public Gerente getGerente() { return gerente; }

    // --- LÓGICA DE COMUNICAÇÃO (O "Cérebro" novo) ---

    /**
     * Guarda uma mensagem na memória interna do objeto.
     * Usado pelo GestorComunicacao antes do .put()
     */
    public void adicionarMensagem(String conteudo, boolean isPublic) {
        if (isPublic) {
            // Alertas públicos (ex: para o Monitor)
            // Adicionamos no início (0) para os mais recentes aparecerem primeiro
            this.alertasPublicos.add(0, conteudo);
        } else {
            // Mensagens privadas (ex: para o Gerente)
            this.inboxPrivada.add(0, conteudo);
        }
    }

    /**
     * Recupera as mensagens da memória.
     */
    public List<String> getMensagens(boolean isPublic) {
        if (isPublic) {
            return new ArrayList<>(this.alertasPublicos);
        } else {
            return new ArrayList<>(this.inboxPrivada);
        }
    }

    /**
     * Conta mensagens. 
     * Nota: Numa versão puramente em memória (Pre-DAO), contamos o tamanho da lista.
     * Na versão com BD, o DAO pode ter lógica mais complexa de "Lidas/Não Lidas".
     */
    public int contarNotificacoesNaoLidas(int idTrabalhador) {
        // Simplificação: Retorna o total de mensagens privadas
        return this.inboxPrivada.size();
    }

    @Override
    public String toString() {
        String nomeGerente = (gerente != null) ? gerente.getNome() : "Sem Gerente";
        return "Restaurante #" + id + " [" + localizacao + "] - Gerente: " + nomeGerente;
    }
}