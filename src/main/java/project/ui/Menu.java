package project.ui;

import java.util.*;

public class Menu {

    // Interfaces funcionais
    public interface Handler {
        void execute();
    }

    public interface PreCondition {
        boolean validate();
    }

    private static Scanner is = new Scanner(System.in);

    private String titulo;
    private List<String> opcoes;
    private List<PreCondition> disponivel;
    private List<Handler> handlers;

    // --- Construtores ---

    public Menu(String titulo, List<String> opcoes) {
        this.titulo = titulo;
        this.opcoes = new ArrayList<>(opcoes);
        this.disponivel = new ArrayList<>();
        this.handlers = new ArrayList<>();
        // Por defeito, tudo está disponível e sem ação definida
        this.opcoes.forEach(s -> {
            this.disponivel.add(() -> true);
            this.handlers.add(() -> System.out.println("\n🚧 Opção ainda não implementada!"));
        });
    }

    public Menu(String titulo, String[] opcoes) {
        this(titulo, Arrays.asList(opcoes));
    }

    // --- Configuração ---

    public void setHandler(int i, Handler h) {
        this.handlers.set(i - 1, h);
    }

    public void setPreCondition(int i, PreCondition b) {
        this.disponivel.set(i - 1, b);
    }

    // --- Execução ---

    public void run() {
        int op;
        do {
            show();
            op = readOption();
            if (op > 0 && !this.disponivel.get(op - 1).validate()) {
                System.out.println("🔒 Opção indisponível neste momento.");
            } else if (op > 0) {
                this.handlers.get(op - 1).execute();
            }
        } while (op != 0);
    }

    private void show() {
        System.out.println("\n=== " + this.titulo + " ===");
        for (int i = 0; i < this.opcoes.size(); i++) {
            System.out.print((i + 1) + " - ");
            System.out.println(this.disponivel.get(i).validate() ? this.opcoes.get(i) : "--- (Indisponível)");
        }
        System.out.println("0 - Sair / Voltar");
    }

    private int readOption() {
        int op;
        System.out.print("Opção: ");
        try {
            String line = is.nextLine();
            op = Integer.parseInt(line);
        } catch (NumberFormatException e) {
            op = -1;
        }
        if (op < 0 || op > this.opcoes.size()) {
            System.out.println("❌ Opção Inválida!");
            op = -1;
        }
        return op;
    }
}