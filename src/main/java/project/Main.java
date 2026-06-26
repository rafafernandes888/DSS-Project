package project;

import project.ui.TextoUI;

public class Main {
    public static void main(String[] args) {
        // O Main agora apenas inicia a Interface de Utilizador
        TextoUI ui = new TextoUI();
        ui.run();
    }
}