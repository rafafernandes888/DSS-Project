package project.ui;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import project.business.*;
import project.business.Comunicacao.*;
import project.business.Operacoes.*;
import project.business.RH.*;

import java.util.*;
import java.util.stream.Collectors;

public class TextoUI {

    private ICadeiaRestaurantesLN sistema; 
    private Scanner scin;
    private Set<String> alergiasAtivas = new HashSet<>();

    public TextoUI() {
        this.sistema = new CadeiaRestaurantesFacade();
        this.scin = new Scanner(System.in);
    }

    public void run() {

        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        imprimirLogotipo();
        
        if (!sistema.verificarConexao()) {
            System.out.println("❌ ERRO DE SISTEMA: Por favor, contacte um funcionário.");
            return;
        }

        menuPrincipal();
    }

    private void imprimirLogotipo() {
        System.out.println("******************************************");
        System.out.println("* *");
        System.out.println("* WELCOME TO DSS FAST-FOOD         *");
        System.out.println("* *");
        System.out.println("******************************************");
    }

    private void menuPrincipal() {
        Menu menu = new Menu("Menu Principal", new String[]{
                "Entrar como Cliente",
                "Entrar como Staff (acesso restrito)"
        });

        menu.setHandler(1, this::menuCliente);
        menu.setHandler(2, this::verificarAcessoStaff);

        menu.run();
    }

    private void menuCliente() {
        System.out.println("\n--- MODO QUIOSQUE ---");
        Menu menu = new Menu("Quiosque Cliente", new String[]{
                "Iniciar Novo Pedido",
                "Consultar Ementa",
                "Consultar Monitor de Pedidos (Estado)",
                "Consultar Saldo de Pontos" // Nova opção
        });
        menu.setHandler(1, this::fluxoFazerPedido);
        menu.setHandler(2, this::menuConsultarEmenta);
        menu.setHandler(3, this::fluxoMonitorCliente);
        menu.setHandler(4, this::fluxoConsultarPontos); // Novo handler
        menu.run();
    }

    private void fluxoFazerPedido() {
        this.alergiasAtivas.clear();

        System.out.println("\n🏢 --- SELEÇÃO DE LOJA ---");
        List<Restaurante> restaurantes = sistema.listarRestaurantes();
        if (restaurantes.isEmpty()) {
            System.out.println("❌ Não há restaurantes registados.");
            return;
        }

        for (Restaurante r : restaurantes) {
            System.out.println("ID " + r.getId() + " - " + r.getLocalizacao());
        }

        int idLoja = -1;
        while (idLoja == -1) {
            System.out.print("Em que restaurante está a fazer o pedido? (ID): ");
            try {
                int input = Integer.parseInt(scin.nextLine());
                for (Restaurante r : restaurantes) {
                    if (r.getId() == input) idLoja = input;
                }
                if (idLoja == -1) System.out.println("⚠️ ID inválido.");
            } catch (NumberFormatException e) {
                System.out.println("⚠️ Introduza um número.");
            }
        }

        // --- IDENTIFICAÇÃO LOGO NO INÍCIO ---
        System.out.println("\n💳 IDENTIFICAÇÃO DE CLIENTE");
        String nifInput = "";
        Cliente clienteAtivo = null;
        
        while (true) {
            System.out.print("Introduza o seu NIF para pontos (ou Enter para saltar): ");
            nifInput = scin.nextLine().trim();
            
            if (nifInput.isEmpty()) {
                break; // Cliente não quer usar pontos
            }
            
            // Validar que o NIF tem exatamente 9 dígitos numéricos
            if (nifInput.matches("\\d{9}")) {
                clienteAtivo = sistema.autenticarOuRegistarCliente(nifInput);
                break;
            } else {
                System.out.println("⚠️ NIF inválido! O NIF deve ter exatamente 9 dígitos numéricos.");
            }
        }

        if (clienteAtivo != null) {
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("✅ Bem-vindo! Saldo atual: " + clienteAtivo.getPontos() + " pontos.");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        }

        configurarAlergias();

        System.out.println("\n🛒 --- O MEU CARRINHO (Loja " + idLoja + ") --- 🛒");
        Carrinho carrinho = new Carrinho();
        boolean finalizar = false;

        while (!finalizar) {
            System.out.println("\n-----------------------------");
            if (carrinho.estaVazio()) {
                System.out.println("(O carrinho está vazio)");
            } else {
                List<LinhaPedido> itens = carrinho.getItens();
                for (int i = 0; i < itens.size(); i++) {
                    System.out.println((i + 1) + ". " + itens.get(i).toString());
                }
            }
            
            // --- ATUALIZAÇÃO VISUAL: MOSTRAR DINHEIRO E PONTOS ---
            System.out.printf("TOTAL EM DINHEIRO: %.2f€\n", carrinho.getTotal());
            if (carrinho.getTotalPontosADescontar() > 0) {
                System.out.println("PONTOS A DESCONTAR: " + carrinho.getTotalPontosADescontar());
            }
            System.out.println("-----------------------------");

            System.out.println("1. ➕ Adicionar itens");
            System.out.println("2. 🗑️ Remover item do carrinho");
            System.out.println("3. ✅ Finalizar e Pagar");
            System.out.println("0. ❌ Cancelar e Sair");
            System.out.print("Escolha: ");
            String op = scin.nextLine();

            switch (op) {
                // CORREÇÃO: Passar o clienteAtivo para o menu de adição
                case "1": menuAdicionarItens(carrinho, clienteAtivo); break; 
                case "2": menuRemoverItens(carrinho); break;
                case "3":
                    if (carrinho.estaVazio()) {
                        System.out.println("⚠️ O carrinho está vazio!");
                    } else {
                        processarCheckOutComLoja(carrinho, idLoja, clienteAtivo);
                        finalizar = true;
                    }
                    break;
                case "0":
                    System.out.println("Pedido cancelado.");
                    finalizar = true;
                    break;
                default:
                    System.out.println("❌ Opção inválida.");
            }
        }
    }

private void fluxoMonitorCliente() {
        System.out.println("\n📺 --- MONITOR DE PEDIDOS ---");
        
        System.out.print("Indique o ID do Restaurante para consulta: ");
        int idRest;
        try {
            idRest = Integer.parseInt(scin.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("❌ ID inválido.");
            return;
        }

        // Obtém a lista de pedidos ativos (Registados, Em Preparação, Em Espera e Pronto)
        List<Pedido> fila = sistema.listarPedidosAtivos(idRest);
        // BUSCA AS MENSAGENS REAIS DA BASE DE DADOS (Cenário 3 - Ricardo)
        List<String> avisosBD = sistema.listarAlertasPublicos(idRest);

        // Listas para separar os pedidos por estado
        List<String> preparando = new ArrayList<>();
        List<String> prontos = new ArrayList<>();
        List<String> alertas = new ArrayList<>();

        for (String msg : avisosBD) {
            alertas.add("⚠️ " + msg);
        }

        for (Pedido p : fila) {
            if (p.getEstado() == EstadoPedido.PRONTO) {
                // Pedidos que o cliente já pode ir buscar
                prontos.add("#" + String.format("%03d", p.getId()));
            } else {
                // Pedidos que ainda estão na cozinha
                String idFormatado = "#" + String.format("%03d", p.getId());
                
                // Se o pedido estiver adiado (Cenário do Ricardo), adicionamos o ícone
                if (p.getEstado() == EstadoPedido.EM_ESPERA) {
                    idFormatado += " ⏳";
                    
                    // Verifica se já existe uma mensagem específica na BD para este ID (#001, #002...)
                    // Se não houver, adicionamos a mensagem genérica como alternativa
                    boolean jaTemMensagem = avisosBD.stream().anyMatch(m -> m.contains("#" + p.getId()));
                    if (!jaTemMensagem) {
                        alertas.add("⚠️ Pedido #" + p.getId() + " aguarda reposição de stock.");
                    }
                }
                preparando.add(idFormatado);
            }
        }

        // --- DESENHO DO MONITOR (Estilo McDonald's) ---
        System.out.println("\n=======================================================");
        
        // Secção de Alertas Reais + Mensagens de Estado
        if (!alertas.isEmpty()) {
            System.out.println("📢 ALERTAS DO SISTEMA:");
            for (String a : alertas) {
                System.out.println("   " + a);
            }
            System.out.println("=======================================================");
        }

        // Cabeçalhos das Colunas
        System.out.printf("  %-24s |  %-24s\n", "🔥 A PREPARAR", "✅ PRONTO A LEVANTAR");
        System.out.println("--------------------------- | -------------------------");

        // Impressão Lado-a-Lado
        int maxLinhas = Math.max(preparando.size(), prontos.size());
        for (int i = 0; i < maxLinhas; i++) {
            String colPrep = (i < preparando.size()) ? preparando.get(i) : "";
            String colPrnt = (i < prontos.size()) ? prontos.get(i) : "";
            
            System.out.printf("  %-24s |  %-24s\n", colPrep, colPrnt);
        }

        System.out.println("=======================================================");
        System.out.println("\n(Pressione Enter para sair/atualizar)");
        scin.nextLine();
    }

    private void fluxoConsultarPontos() {
    System.out.println("\n--- CONSULTA DE PONTOS ---");
    
    String nif = "";
    while (true) {
        System.out.print("Introduza o seu NIF: ");
        nif = scin.nextLine().trim();
        
        if (nif.isEmpty()) {
            System.out.println("⚠️ Operação cancelada.");
            return;
        }
        
        // Validar que o NIF tem exatamente 9 dígitos numéricos
        if (nif.matches("\\d{9}")) {
            break;
        } else {
            System.out.println("⚠️ NIF inválido! O NIF deve ter exatamente 9 dígitos numéricos.");
        }
    }

    int pontos = sistema.consultarPontos(nif);
    
    if (pontos >= 0) {
        System.out.println("\n******************************************");
        System.out.println("👤 Cliente NIF: " + nif);
        System.out.println("💳 Saldo Atual: " + pontos + " pontos");
        System.out.println("******************************************");
    } else {
        System.out.println("❌ Erro: Não foi possível encontrar o cliente.");
    }
    
    System.out.println("\nPressione Enter para voltar...");
    scin.nextLine();
    }

    private void configurarAlergias() {
        boolean sair = false;
        while (!sair) {
            System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("⚠️  PAINEL DE SEGURANÇA ALIMENTAR");
            System.out.print("Restrições Ativas: ");
            if (alergiasAtivas.isEmpty()) System.out.println("[Nenhuma]");
            else System.out.println(String.join(", ", alergiasAtivas));
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            
            // Define se aparece a bola [●] ou espaço vazio [ ] baseando-se no Set de alergias
            String bGluten = alergiasAtivas.contains("GLÚTEN") ? "●" : " ";
            String bLactose = alergiasAtivas.contains("LACTOSE") ? "●" : " ";
            String bOvos = alergiasAtivas.contains("OVOS") ? "●" : " ";

            System.out.println("1. [" + bGluten + "] GLÚTEN");
            System.out.println("2. [" + bLactose + "] LACTOSE");
            System.out.println("3. [" + bOvos + "] OVOS");
            System.out.println("0. ✅ CONCLUIR E IR PARA EMENTA");
            System.out.print("Opção: ");
            
            String op = scin.nextLine();
            switch (op) {
                case "1" -> toggleAlergia("GLÚTEN");
                case "2" -> toggleAlergia("LACTOSE");
                case "3" -> toggleAlergia("OVOS");
                case "0" -> sair = true;
                default -> System.out.println("⚠️ Opção inválida.");
            }
        }
    }

    private void toggleAlergia(String tipo) {
        if (alergiasAtivas.contains(tipo)) alergiasAtivas.remove(tipo);
        else alergiasAtivas.add(tipo);
    }

    private void menuAdicionarItens(Carrinho c, Cliente clienteAtivo) {
        System.out.println("\n--- SELECIONE A CATEGORIA ---");
        System.out.println("1. Menus Completos");
        System.out.println("2. Hambúrgueres");
        System.out.println("3. Acompanhamentos");
        System.out.println("4. Bebidas");
        System.out.print("Opção: ");
        String catOp = scin.nextLine();
        
        List<Artigo> disponiveis;
        switch(catOp) {
            case "1": disponiveis = sistema.getArtigosPorCategoria("MENU"); break;
            case "2": disponiveis = sistema.getArtigosPorCategoria("HAMBURGUER"); break;
            case "3": disponiveis = sistema.getArtigosPorCategoria("ACOMPANHAMENTO"); break;
            case "4": disponiveis = sistema.getArtigosPorCategoria("BEBIDA"); break;
            default: return;
        }

        Artigo escolhido = escolherArtigoComAlergenos(disponiveis);
        
        if (escolhido != null) {
            String personalizacao = "";

            //SE FOR UM MENU, ESCOLHER BEBIDA PRIMEIRO 
            if (escolhido.getNome().toUpperCase().contains("MENU")) {
                String bebida = escolherBebidaParaMenu();
                personalizacao = "BEBIDA: " + bebida;
                System.out.println("✅ " + bebida + " adicionada ao seu menu.");
            }

            boolean temPerigo = escolhido.getIngredientes().stream()
                    .anyMatch(i -> i.isAlergenio() && checkTipo(i.getNome()));
            
            // Lógica de Alergias e Customização de Ingredientes
            if (temPerigo) {
                System.out.println("\n⚠️ ATENÇÃO: Este item contém ingredientes não recomendados para si!");
                System.out.println("Deve alterar o pedido para remover o alergénio.");
                String extraPerso = customizarIngredientes(escolhido);
                if (!extraPerso.isEmpty()) {
                    personalizacao = personalizacao.isEmpty() ? extraPerso : personalizacao + " | " + extraPerso;
                }
            } else {
                System.out.println("\nProduto: " + escolhido.getNome());
                System.out.println("1. ✅ Adicionar como está");
                System.out.println("2. 🛠️  Alterar ingredientes");
                System.out.print("Opção: ");
                if (scin.nextLine().equals("2")) {
                    String extraPerso = customizarIngredientes(escolhido);
                    if (!extraPerso.isEmpty()) {
                        personalizacao = personalizacao.isEmpty() ? extraPerso : personalizacao + " | " + extraPerso;
                    }
                }
            }
            
            // Lógica de Gastar Pontos (Mantida como tinha)
            boolean usarPontos = false;
            if (clienteAtivo != null) {
                int custoPontos = (int) (escolhido.getPreco() * 10);
                int saldoDisponivel = clienteAtivo.getPontos() - c.getTotalPontosADescontar();

                if (saldoDisponivel >= custoPontos) {
                    System.out.println("\n💰 SALDO DISPONÍVEL: " + saldoDisponivel + " pontos");
                    System.out.print("👉 Deseja trocar " + custoPontos + " pontos por este item (ficará a 0.00€)? (S/N): ");
                    if (scin.nextLine().equalsIgnoreCase("S")) {
                        usarPontos = true;
                        System.out.println("✅ Item marcado como Recompensa!");
                    }
                }
            }
            
            c.adicionarItem(escolhido, personalizacao, usarPontos);
            System.out.println("✅ " + escolhido.getNome() + " adicionado!");
        }
    }

    private boolean checkTipo(String nome) {
        String n = nome.toLowerCase();
        if (n.contains("pão") && !n.contains("(gf)") && alergiasAtivas.contains("GLÚTEN")) return true;
        if (n.contains("queijo") && !n.contains("vegan") && alergiasAtivas.contains("LACTOSE")) return true;
        if (n.contains("mix") && alergiasAtivas.contains("LACTOSE")) return true;
        if (n.contains("ovo") && alergiasAtivas.contains("OVOS")) return true;
        return false;
    }

private String customizarIngredientes(Artigo a) {
        // Filtrar ingredientes que fazem parte da sanduíche
        List<Ingrediente> alteraveis = a.getIngredientes().stream()
                .filter(i -> {
                    String n = i.getNome().toLowerCase();
                    return !n.contains("dose") && !n.contains("xarope") && !n.contains("mix") && 
                           !n.contains("batata") && !n.contains("cola") && !n.contains("água") && 
                           !n.contains("bebida") && !n.contains("refrigerante");
                })
                .collect(Collectors.toList());

        Map<Integer, String> alteracoes = new HashMap<>();
        boolean concluido = false;

        while (!concluido) {
            System.out.println("\n🛠️ --- PERSONALIZAR SANDUÍCHE: " + a.getNome() + " ---");
            
            for (int i = 0; i < alteraveis.size(); i++) {
                Ingrediente ing = alteraveis.get(i);
                String estado = alteracoes.getOrDefault(ing.getId(), "Normal");
                String tagAlergia = (ing.isAlergenio() && checkTipo(ing.getNome())) ? " ⚠️ [ALÉRGICO]" : "";
                System.out.println((i + 1) + ". " + ing.getNome() + tagAlergia + " [" + estado + "]");
            }

            System.out.println("8. [+] ADICIONAR EXTRA (Queijo, Bacon, etc.)");
            System.out.println("0. ✅ CONCLUIR");
            System.out.print("Escolha (ou 0 para finalizar): ");
            
            try {
                String input = scin.nextLine();
                int escolha = Integer.parseInt(input);

                if (escolha == 0) {
                    concluido = true;
                } 
                else if (escolha == 8) {
                    List<Ingrediente> todos = sistema.getTodosIngredientes();
                    System.out.println("\n--- SELECIONE O INGREDIENTE EXTRA ---");
                    for (int i = 0; i < todos.size(); i++) {
                        Ingrediente ing = todos.get(i);
                        System.out.printf("%d. %-20s (+%.2f€)%n", (i + 1), ing.getNome(), ing.getPrecoVenda());
                    }
                    System.out.print("Escolha o número: ");
                    int idxExtra = Integer.parseInt(scin.nextLine()) - 1;

                    if (idxExtra >= 0 && idxExtra < todos.size()) {
                        Ingrediente extra = todos.get(idxExtra);
                        a.adicionarIngrediente(extra); 
                        alteracoes.put(extra.getId(), "EXTRA " + extra.getNome());
                        if (!alteraveis.contains(extra)) alteraveis.add(extra);
                        System.out.println("✅ " + extra.getNome() + " adicionado como extra!");
                    }
                } 
                else if (escolha > 0 && escolha <= alteraveis.size()) {
                    Ingrediente selecionado = alteraveis.get(escolha - 1);
                    System.out.println("\n" + selecionado.getNome() + ": [1] Normal | [2] Retirar | [3] Extra");
                    String subOp = scin.nextLine();
                    switch (subOp) {
                        case "1" -> alteracoes.remove(selecionado.getId());
                        case "2" -> alteracoes.put(selecionado.getId(), "SEM " + selecionado.getNome());
                        case "3" -> alteracoes.put(selecionado.getId(), "EXTRA " + selecionado.getNome());
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠️ Escolha inválida.");
            }
        }

        System.out.print("Alguma nota adicional? ");
        String nota = scin.nextLine().trim();
        
        List<String> result = new ArrayList<>(alteracoes.values());
        String strIngredientes = "";
        
        // CORREÇÃO: Identificamos explicitamente o que são alterações de ingredientes
        if (!result.isEmpty()) {
            strIngredientes = "ALTERAÇÕES: " + String.join(", ", result);
        }
        
        // CORREÇÃO: Usamos o prefixo "OBS:" para a nota manual real do utilizador
        if (!nota.isEmpty()) {
            return strIngredientes.isEmpty() ? "OBS: " + nota : strIngredientes + " | OBS: " + nota;
        }
        return strIngredientes;
    }
    private void menuRemoverItens(Carrinho c) {
        if (c.estaVazio()) return;
        System.out.print("Número do item a remover: ");
        try {
            int idx = Integer.parseInt(scin.nextLine()) - 1;
            c.removerItem(idx);
            System.out.println("🗑️ Item removido.");
        } catch (Exception e) {
            System.out.println("❌ Inválido.");
        }
    }

    private void processarCheckOutComLoja(Carrinho c, int idLoja, Cliente cliente) {
            System.out.println("\n--- FINALIZAR PEDIDO ---");
            System.out.println("Onde deseja fazer a refeição?");
            System.out.println("1. Local (Restaurante) | 2. Takeaway");
            String localOp = scin.nextLine();
            String localConsumo = localOp.equals("2") ? "TAKEAWAY" : "LOCAL";

            String metodo;
            double totalPagar = c.getTotal();

            // --- CORREÇÃO: VERIFICAR SE O PEDIDO É GRÁTIS ---
            if (totalPagar > 0) {
                System.out.println("\nSelecione o método de pagamento:");
                System.out.println("1. MBWay | 2. Multibanco | 3. Numerário");
                String pagOp = scin.nextLine();
                metodo = pagOp.equals("1") ? "MBWAY" : (pagOp.equals("2") ? "MULTIBANCO" : "NUMERARIO");
                
                // Simula o pagamento apenas se houver valor a pagar
                simularPagamentoUI(metodo, totalPagar);
            } else {
                // Se for 0.00€, o método é automaticamente "PONTOS" e saltamos a simulação
                metodo = "FIDELIZACAO (PONTOS)";
                System.out.println("\n✨ Pedido pago com pontos. A processar registo direto...");
            }

            // Criamos o objeto Pedido
            Pedido p = new Pedido(cliente);
            p.setIdRestaurante(idLoja);
            p.setMetodoPagamento(metodo);
            p.setLocalConsumo(localConsumo);
            
            for (LinhaPedido item : c.getItens()) {
                p.adicionarItem(item.getArtigo(), item.getPersonalizacao(), item.isPagoComPontos());
            }

            // O sistema regista o pedido (GestorOperacoes tratará de abater os pontos)
            int resultado = sistema.registarPedido(p);
            
            if (resultado > 0) {
                System.out.println("\n******************************************");
                System.out.println("PEDIDO REGISTADO COM SUCESSO!");
                System.out.println("Senha de Levantamento: " + resultado);
                System.out.println("Local: Balcão da Loja #" + idLoja);
                System.out.println("Método: " + metodo + " | Consumo: " + localConsumo);
                System.out.println("⏱️ Tempo estimado: " + p.getTempoEstimado() + " minutos");
                
                if (cliente != null && !cliente.getNif().equals("N/A") && !cliente.getNif().isEmpty()) {
                    System.out.println("------------------------------------------");
                    int ptsGastos = p.getTotalPontosGastos();
                    int ptsGanhos = (int) p.getValorTotal();

                    if (ptsGastos > 0) {
                        System.out.println("🔥 Pontos utilizados: " + ptsGastos);
                    }
                    System.out.println("💰 Pontos ganhos neste pedido: " + ptsGanhos);
                    System.out.println("💳 Novo saldo total de pontos: " + cliente.getPontos());
                }

                if (totalPagar > 0 && metodo.equals("NUMERARIO")) {
                    System.out.println("⚠️ POR FAVOR, PAGUE NO BALCÃO ANTES DE RECOLHER O SEU PEDIDO.");
                }
                System.out.println("******************************************");
            } else if (resultado == -2) {
                System.out.println("\n❌ DESCULPE: Artigo indisponível por falta de ingredientes nesta loja.");
            } else {
                System.out.println("\n❌ ERRO CRÍTICO: Não foi possível processar o pedido.");
            }
            
            System.out.println("\nPressione Enter para continuar...");
            scin.nextLine();
        }

    private void simularPagamentoUI(String metodo, double total) {
        System.out.println("\n--- PROCESSANDO PAGAMENTO ---");
        System.out.printf("Valor Total: %.2f EUR\n", total);
        
        switch (metodo) {
            case "MBWAY" -> {
                System.out.println("QR CODE GERADO:");
                System.out.println("  #######  ");
                System.out.println("  #  O  #  ");
                System.out.println("  #     #  ");
                System.out.println("  #######  ");
                System.out.println("A aguardar a confirmação no telemóvel...");
            }
            case "MULTIBANCO" -> {
                System.out.println("Insira ou aproxime o seu cartão...");
                System.out.println("Introduza o PIN no terminal...");
            }
            case "NUMERARIO" -> {
                System.out.println("A gerar talão para pagamento no balcão...");
            }
        }
        
        try { Thread.sleep(1500); } catch (InterruptedException e) {}
        System.out.println("✅ Processamento concluído.");
    }

    private void verificarAcessoStaff() {
        System.out.print("PIN de segurança: ");
        if (scin.nextLine().equals("123")) {
            loginStaff();
        } else {
            System.out.println("❌ Acesso negado.");
        }
    }

    private void loginStaff() {
        System.out.print("ID: ");
        try {
            int id = Integer.parseInt(scin.nextLine());
            System.out.print("Password: ");
            String pass = scin.nextLine();
            Funcionario logado = sistema.autenticarFuncionario(id, pass);
            if (logado != null) {
                System.out.println("✅ Bem-vindo, " + logado.getNome());
                direcionarMenuPorClasse(logado);
            } else {
                System.out.println("❌ Credenciais inválidas!");
            }
        } catch (Exception e) {
            System.out.println("Erro ao entrar.");
        }
    }

    private void direcionarMenuPorClasse(Funcionario f) {
        if (f instanceof COO) menuCOO((COO) f);
        else if (f instanceof Gerente) menuGerente((Gerente) f);
        else if (f instanceof Trabalhador) menuTrabalhador((Trabalhador) f);
    }

    // No ficheiro src/main/java/project/ui/TextoUI.java

    private void menuCOO(COO coo) {
        Menu menu = new Menu("Painel COO", new String[]{
            "Gestão Restaurantes", 
            "Gestão RH", 
            "Ver Indicadores de Performance (Relatório)",
            "Enviar Mensagem Global (Incentivo Staff)" 
        });
        
        menu.setHandler(1, this::menuGestaoRestaurantes);
        menu.setHandler(2, () -> menuGestaoRH(coo));
        menu.setHandler(3, this::fluxoRelatorios);
        menu.setHandler(4, () -> {
            System.out.print("\nEscreva a mensagem de incentivo para toda a rede: ");
            String msg = scin.nextLine();
            if (sistema.enviarMensagemGlobal("[COO] " + msg)) {
                System.out.println("✅ Mensagem enviada para todos os colaboradores!");
            } else {
                System.out.println("❌ Erro ao enviar mensagem global.");
            }
        });
        menu.run();
    }

    private void fluxoRelatorios() {
    System.out.println("\n📊 --- INDICADORES GLOBAIS DE PERFORMANCE ---");
    List<Restaurante> rests = sistema.listarRestaurantes();
    
    if (rests.isEmpty()) {
        System.out.println("Não há dados disponíveis.");
        return;
    }

    for (Restaurante r : rests) {
        // Precisas de criar este método na Facade que chame o PedidoDAO.getRelatorioRestaurante
        Relatorio rel = sistema.gerarRelatorio(r.getId(), r.getLocalizacao());
        if (rel != null) {
            System.out.println(rel.toString());
        }
    }
    System.out.println("\n(Pressione Enter para voltar)");
    scin.nextLine();
    }

    private void menuGerente(Gerente g) {
        Menu menu = new Menu("Painel de Gerência (Loja " + g.getIdRestaurante() + ")", new String[]{
            "Minha Equipa", 
            "Solicitar Contratação", 
            "Solicitar Despedimento",
            "Ver Mensagens/Notificações",
            "Enviar Mensagem à Equipa",
            "Encomendar Stock"
        });

        // 1. Ver Equipa Local
        menu.setHandler(1, () -> listarFuncionariosUI(g));

        menu.setHandler(2, () -> {
            System.out.println("--- Nova Contratação ---");
            
            // Validação do Nome (obrigatório)
            String nome;
            while (true) {
                System.out.print("Nome Candidato: ");
                nome = scin.nextLine().trim();
                if (!nome.isEmpty()) break;
                System.out.println("⚠️ O nome é obrigatório!");
            }
            
            // Validação da Password (obrigatória)
            String pass;
            while (true) {
                System.out.print("Password Temporária: ");
                pass = scin.nextLine().trim();
                if (!pass.isEmpty()) break;
                System.out.println("⚠️ A password é obrigatória!");
            }
            
            // Validação do Posto (obrigatório)
            String posto;
            while (true) {
                System.out.print("Posto (Cozinha/Balcao/etc): ");
                posto = scin.nextLine().trim();
                if (!posto.isEmpty()) break;
                System.out.println("⚠️ O posto é obrigatório!");
            }

            // Facade chama GestorRH -> SolicitacaoDAO
            if (sistema.solicitarContratacao(g.getId(), nome, pass, posto)) {
                System.out.println("✅ Solicitação enviada ao COO.");
            } else {
                System.out.println("❌ Erro ao enviar solicitação.");
            }
        });

        // 3. Pedir Despedimento (RH)
        menu.setHandler(3, () -> {
            System.out.println("--- Despedimento ---");
            // Mostramos a lista primeiro para ele ver os IDs
            listarFuncionariosUI(g); 
            System.out.print("ID do Funcionário a despedir: ");
            try {
                int idAlvo = Integer.parseInt(scin.nextLine());
                if (sistema.solicitarDespedimento(g.getId(), idAlvo)) {
                    System.out.println("✅ Pedido de despedimento enviado.");
                } else {
                    System.out.println("❌ Erro (Não pode despedir o COO nem a si mesmo).");
                }
            } catch (Exception e) {
                System.out.println("ID inválido.");
            }
        });

        // 4. Ver Mensagens/Notificações
        menu.setHandler(4, () -> {
            int idRest = g.getIdRestaurante();
            List<String> msgs = sistema.lerMensagens(idRest, g.getId());
            if (msgs.isEmpty()) {
                System.out.println("📭 Sem mensagens novas.");
            } else {
                System.out.println("\n📬 --- MENSAGENS ---");
                for (String s : msgs) {
                    System.out.println("  • " + s);
                }
            }
        });

        // 5. Enviar Mensagem à Equipa
        menu.setHandler(5, () -> {
            System.out.println("--- Enviar Mensagem à Equipa ---");
            System.out.print("Escreva a mensagem: ");
            String msg = scin.nextLine().trim();
            
            if (msg.isEmpty()) {
                System.out.println("⚠️ Mensagem vazia, operação cancelada.");
                return;
            }
            
            // Envia apenas para o restaurante do gerente
            String msgFormatada = "[Gerente] " + msg;
            if (sistema.enviarMensagem(g.getIdRestaurante(), msgFormatada)) {
                System.out.println("✅ Mensagem enviada à equipa.");
            } else {
                System.out.println("❌ Erro ao enviar mensagem.");
            }
        });

        // 6. Encomendar Stock
        menu.setHandler(6, () -> {
            System.out.println("\n📦 --- ENCOMENDAR STOCK ---");
            List<Ingrediente> ingredientes = sistema.getTodosIngredientes();
            for (Ingrediente i : ingredientes) {
                int stockAtual = sistema.getStockArmazem(g.getIdRestaurante(), i.getId());
                System.out.println("ID " + i.getId() + " - " + i.getNome() + " (Stock atual: " + stockAtual + ")");
            }
            
            try {
                System.out.print("ID do ingrediente a encomendar: ");
                int idIng = Integer.parseInt(scin.nextLine().trim());
                
                System.out.print("Quantidade a adicionar: ");
                int qtd = Integer.parseInt(scin.nextLine().trim());
                
                if (qtd <= 0) {
                    System.out.println("⚠️ Quantidade inválida.");
                    return;
                }
                
                if (sistema.encomendarStock(g.getIdRestaurante(), idIng, qtd)) {
                    System.out.println("✅ Encomenda registada! +" + qtd + " unidades adicionadas ao stock.");
                } else {
                    System.out.println("❌ Erro ao processar encomenda.");
                }
            } catch (NumberFormatException e) {
                System.out.println("⚠️ Valor inválido.");
            }
        });

        menu.run();
    }
    private void menuTrabalhador(Trabalhador t) {
        int idRest = sistema.getRestauranteDoFuncionario(t.getId());
        boolean sair = false;
        while(!sair) {
            System.out.println("\n--- MENU TRABALHADOR ---");
            System.out.println("1. Monitor Cozinha");
            System.out.println("2. Consultar Stock");
            System.out.println("3. Ver Mensagens");
            System.out.println("0. Sair");
            System.out.print("Opção: ");
            String op = scin.nextLine();
            if(op.equals("1")) fluxoMonitorCozinha(idRest);
            else if(op.equals("2")) mostrarStock(idRest);
            else if(op.equals("3")) {
                List<String> msgs = sistema.lerMensagens(idRest, t.getId());
                if (msgs.isEmpty()) {
                    System.out.println("📭 Nenhuma mensagem.");
                } else {
                    System.out.println("\n📬 --- MENSAGENS ---");
                    for (String s : msgs) System.out.println("  • " + s);
                }
            }
            else if(op.equals("0")) sair = true;
        }
    }

    private void mostrarStock(int idRest) {
        System.out.println("\n--- STOCK ATUAL ---");
        List<Ingrediente> ingredientes = sistema.getTodosIngredientes();
        for (Ingrediente i : ingredientes) {
            int qtd = sistema.getStockArmazem(idRest, i.getId());
            System.out.println("🔹 " + i.getNome() + ": " + qtd);
        }
        scin.nextLine();
    }

private void fluxoMonitorCozinha(int idRestaurante) {
        System.out.println("\n--- MONITOR DE COZINHA (LOJA " + idRestaurante + ") ---");
        
        List<Pedido> ativos = sistema.listarPedidosAtivos(idRestaurante);
        
        if(ativos.isEmpty()) {
            System.out.println("Nenhum pedido pendente.");
            return;
        }

        for(Pedido p : ativos) {
            String tipoConsumo = (p.getLocalConsumo() != null && !p.getLocalConsumo().isEmpty()) 
                                 ? p.getLocalConsumo() 
                                 : "LOCAL";
            
            String alerta = "";
            if (p.getEstado() == EstadoPedido.EM_ESPERA) {
                alerta = " ⏳ [PARADO - ESPERA INGREDIENTE] (+Tempo)";
            }
            
            System.out.println("\nPedido #" + p.getId() + " [" + p.getEstado() + "]" + alerta + " - " + tipoConsumo);
            
            List<LinhaPedido> detalhes = sistema.getDetalhesDoPedido(p.getId());
            for(LinhaPedido lp : detalhes) {
                System.out.println("  > " + lp.getArtigo().getNome());
                
                System.out.print("    Ingredientes: ");
                if (lp.getArtigo().getIngredientes() != null) {
                    // CORREÇÃO: Filtramos a bebida genérica da lista de ingredientes 
                    // para não aparecer duplicado com a escolha do cliente.
                    String ingList = lp.getArtigo().getIngredientes().stream()
                            .filter(i -> {
                                String n = i.getNome().toLowerCase();
                                // Se o artigo for um Menu, ignoramos ingredientes de bebida na listagem
                                return !n.contains("água") && !n.contains("cola") && 
                                       !n.contains("refrigerante") && !n.contains("bebida");
                            })
                            .map(Ingrediente::getNome)
                            .collect(Collectors.joining(", "));
                    System.out.println(ingList.isEmpty() ? "Padrao" : ingList);
                } else {
                    System.out.println("N/A");
                }
                
                String totalPerso = lp.getPersonalizacao();
                if (totalPerso != null && !totalPerso.isEmpty()) {
                    String[] partes = totalPerso.split(" \\| ");
                    for (String parte : partes) {
                        String info = parte.trim(); 
                        if (info.startsWith("BEBIDA:")) {
                            // Imprime a bebida real escolhida (ex: Água Mineral)
                            System.out.println("    🥤 " + info);
                        } 
                        else if (info.startsWith("ALTERAÇÕES:")) {
                            System.out.println("    ⚠️ " + info);
                        } 
                        else if (info.startsWith("OBS:")) {
                            System.out.println("    📝 NOTA DO CLIENTE: " + info.replace("OBS: ", ""));
                        }
                        else if (!info.isEmpty()) {
                            System.out.println("    ℹ️ " + info);
                        }
                    }
                }
            }
        }
        
        System.out.println("\n--------------------------------");
        System.out.println("1. Avançar Estado do Pedido (Normal)");
        System.out.println("2. ⚠️ Reportar Falta de Ingrediente (Adiar Pedido)");
        System.out.println("0. Sair");
        System.out.print("Opção: ");
        String op = scin.nextLine();

        if (op.equals("1")) {
            avancarEstadoPedido(ativos); 
        } 
        else if (op.equals("2")) {
            tratarFaltaIngrediente(idRestaurante); 
        }
    }

    private void avancarEstadoPedido(List<Pedido> ativos) {
        System.out.print("Digite o ID do pedido para avançar: ");
        try {
            int id = Integer.parseInt(scin.nextLine());
            Pedido pedidoAlvo = ativos.stream().filter(ped -> ped.getId() == id).findFirst().orElse(null);
            
            if (pedidoAlvo != null) {
                // Se tentar avançar um pedido que está em espera, ele volta ao normal (EM_PREPARACAO)
                if (pedidoAlvo.getEstado() == EstadoPedido.EM_ESPERA) {
                    System.out.println("🔄 A retomar pedido...");
                    sistema.atualizarEstadoPedido(id, EstadoPedido.EM_PREPARACAO);
                    // Limpar o alerta público associado a este pedido
                    sistema.limparAlertasDoPedido(pedidoAlvo.getIdRestaurante(), id);
                    return;
                }

                EstadoPedido atual = pedidoAlvo.getEstado();
                EstadoPedido proximo = switch (atual) {
                    case REGISTADO -> EstadoPedido.EM_PREPARACAO;
                    case EM_PREPARACAO -> EstadoPedido.PRONTO;
                    case PRONTO -> EstadoPedido.ENTREGUE;
                    case ENTREGUE -> EstadoPedido.CONCLUIDO;
                    default -> null;
                };
                
                if (proximo != null) {
                    sistema.atualizarEstadoPedido(id, proximo);
                    System.out.println("✅ Estado atualizado para: " + proximo);
                }
            } else {
                System.out.println("❌ Pedido não encontrado na lista.");
            }
        } catch (Exception e) { System.out.println("❌ ID inválido."); }
    }

    private void tratarFaltaIngrediente(int idRestaurante) {
        System.out.println("\n🥕 --- REPORTAR FALTA DE INGREDIENTE ---");
        
        // 1. Listar Ingredientes (Precisa do getTodosIngredientes na Facade)
        List<Ingrediente> ings = sistema.getTodosIngredientes();
        if(ings.isEmpty()) {
            System.out.println("Erro ao carregar ingredientes.");
            return;
        }
        
        for(Ingrediente i : ings) {
            System.out.println("ID " + i.getId() + " - " + i.getNome());
        }

        System.out.print("Qual o ID do ingrediente em falta? ");
        int idIng = -1;
        try {
            idIng = Integer.parseInt(scin.nextLine());
        } catch (NumberFormatException e) { return; }

        // 2. Verificar Stock no Armazém (Precisa do getStockArmazem na Facade)
        int qtd = sistema.getStockArmazem(idRestaurante, idIng);
        
        System.out.println("📦 A consultar armazém...");
        if (qtd > 0) {
            System.out.println("✅ Existem " + qtd + " unidades no armazém.");
            System.out.println("O pedido será adiado 15 minutos enquanto se repõe o stock.");
        } else {
            System.out.println("⚠️ ALERTA: Stock a 0 no armazém! O atraso pode ser maior.");
        }

        // 3. Selecionar o Pedido a Adiar
        System.out.print("Qual o ID do Pedido afetado? ");
        int idPedido = -1;
        try {
            idPedido = Integer.parseInt(scin.nextLine());
        } catch (NumberFormatException e) { return; }

    // 4. Executar Ação (BD + Mensagens)
    if (sistema.adiarPedido(idPedido, 15)) {
        System.out.println("✅ Pedido #" + idPedido + " colocado EM_ESPERA (+15 min).");
        
        // NOVO: Pedir o motivo para o monitor de clientes (Cenário 3)
        System.out.print("Escreva o aviso para o Monitor de Clientes (ex: Atraso na Grelha): ");
        String avisoPublico = scin.nextLine();
        
        // A) Enviar mensagem PÚBLICA (isPublic = true) -> Vai para o Monitor
        sistema.enviarMensagem(idRestaurante, "Pedido #" + idPedido + ": " + avisoPublico, true);
        
        // B) Enviar mensagem PRIVADA (isPublic = false) -> Só o Gerente vê
        String msgPrivada = "LOG: Trabalhador reportou falta de Ingrediente ID " + idIng + " para o Pedido #" + idPedido;
        sistema.enviarMensagem(idRestaurante, msgPrivada, false);
        
        System.out.println("📨 Notificações enviadas (Monitor e Gerência).");
    } else {
        System.out.println("❌ Erro ao atualizar pedido.");
    }
    }

    private void menuConsultarEmenta() {
        System.out.println("\n=======================================================");
        System.out.println("                🍴 A NOSSA EMENTA 🍴                ");
        System.out.println("=======================================================");

        // Definimos todas as categorias que queremos listar sequencialmente
        String[] categorias = {"MENU", "HAMBURGUER", "ACOMPANHAMENTO", "BEBIDA"};

        for (String cat : categorias) {
            // Obtém a lista da categoria através da Facade
            List<Artigo> itens = sistema.getArtigosPorCategoria(cat);
            
            if (itens != null && !itens.isEmpty()) {
                System.out.println("\n>>> " + cat + "S");
                System.out.printf("%-4s | %-30s | %-8s\n", "ID", "NOME", "PREÇO");
                System.out.println("-------------------------------------------------------");
                
                for (Artigo a : itens) {
                    // Verifica alergias para o Cenário da Joana
                    String alerta = a.isPodeCausarAlergia() ? " ⚠️" : "";
                    
                    // Imprime a linha diretamente com espaço de 30 caracteres para o nome
                    System.out.printf("%-4d | %-30s | %7.2f€ %s\n", 
                                    a.getId(), 
                                    a.getNome(), 
                                    a.getPreco(), 
                                    alerta);
                }
            }
        }

        System.out.println("\n=======================================================");
        System.out.println("Pressione Enter para voltar ao menu anterior...");
        scin.nextLine(); 
    }

    private Artigo escolherArtigoComAlergenos(List<Artigo> lista) {
        System.out.println("\n--- SELEÇÃO DE ARTIGOS ---");
        for (Artigo a : lista) {
        boolean perigoso = a.getIngredientes().stream()
                .anyMatch(i -> i.isAlergenio() && checkTipo(i.getNome()));
        
        // 1. Definimos a tag sem espaços fixos extras
        String tag = alergiasAtivas.isEmpty() ? "🔹" : (perigoso ? "❌ [PERIGO]" : "✅ [SEGURO]");
        
        // 2. Ajustamos o printf:
        // %d -> ID
        // %s -> Tag (sem o -10, para não forçar largura)
        // %-30s -> Nome do artigo (ajustado para 30 para manter alinhamento dos preços)
        System.out.printf("%d - %s %-30s (%.2f€)%n", a.getId(), tag, a.getNome(), a.getPreco());
        }

        System.out.print("\nID do Artigo: ");

        try {
            int id = Integer.parseInt(scin.nextLine());
            return lista.stream().filter(a -> a.getId() == id).findFirst().orElse(null);
        } catch (Exception e) { return null; }
    }

    private String escolherBebidaParaMenu() {
    System.out.println("\n🥤 --- ESCOLHA A BEBIDA DO SEU MENU ---");
    // Vamos buscar todas as bebidas à base de dados
    List<Artigo> bebidas = sistema.getArtigosPorCategoria("BEBIDA");
    
    if (bebidas.isEmpty()) return "Bebida Padrão";

    for (int i = 0; i < bebidas.size(); i++) {
        System.out.println((i + 1) + ". " + bebidas.get(i).getNome());
    }

    System.out.print("Escolha o número da bebida: ");
    try {
        int escolha = Integer.parseInt(scin.nextLine());
        if (escolha > 0 && escolha <= bebidas.size()) {
            return bebidas.get(escolha - 1).getNome();
        }
    } catch (NumberFormatException e) {
        System.out.println("⚠️ Opção inválida, será usada a bebida padrão.");
    }
    
    return bebidas.get(0).getNome(); // Devolve a primeira se houver erro
    }

    // --- MÉTODOS DE GESTÃO COO (CORRECÇÃO DO ERRO) ---
    
    private void menuGestaoRestaurantes() {
        Menu menu = new Menu("Restaurantes", new String[]{"Listar", "Enviar Msg"});
        menu.setHandler(1, () -> {
            // Facade
            for (Restaurante r : sistema.listarRestaurantes()) 
                System.out.println(r);
        });
        // Dentro de menuGestaoRestaurantes na TextoUI.java

        menu.setHandler(2, () -> {
            boolean sucesso = false;

            // Ciclo DO-WHILE: Repete enquanto não tiver sucesso e o utilizador quiser continuar
            do {
                System.out.println("\n📧 --- NOVA MENSAGEM ---");

                int id = -1;

                // Bloco para garantir que o utilizador escreve um número
                while (true) {
                    System.out.print("Destinatário (ID do Restaurante): ");
                    try {
                        String input = scin.nextLine();
                        id = Integer.parseInt(input);
                        break; // Se converteu bem, sai deste mini-loop
                    } catch (NumberFormatException e) {
                        System.out.println("⚠️ O ID tem de ser um número inteiro.");
                    }
                }

                System.out.print("Escreva a mensagem: ");
                String msg = scin.nextLine();

                // Tenta enviar chamando a Facade (que chama o DAO)
                if (sistema.enviarMensagem(id, msg, false)) {
                    System.out.println("✅ Mensagem enviada com sucesso!");
                    sucesso = true; // Quebra o ciclo principal
                } else {
                    // Se falhou (porque o ID não existe no SQL), mostramos aviso amigável
                    System.out.println();
                    System.out.print("ID não corresponde a um restaurante.Deseja tentar novamente? (S/N): ");
                    String resp = scin.nextLine().trim().toUpperCase();

                    if (!resp.equals("S")) {
                        return; // O utilizador desistiu, sai do menu
                    }
                    // Se for "S", o loop 'do-while' repete e pede o ID outra vez
                }

            } while (!sucesso);
        });

        menu.run();
    }

    private void menuGestaoRH(COO coo) {
        Menu menu = new Menu("RH", new String[]{
            "Ver Pendentes", "Listar Todos", "Contratar", "Despedir"
        });

        // Handler Pendentes
// SUBSTITUIR o handler 1 (Ver Pendentes) no menuGestaoRH - linhas 1055 a 1068

        // Handler Pendentes - VERSÃO CORRIGIDA
        menu.setHandler(1, () -> {
            List<Solicitacao> pends = sistema.listarSolicitacoesPendentes();
            
            if(pends.isEmpty()) {
                System.out.println("Nenhuma solicitação pendente.");
                return;
            }
            
            // Mostra lista numerada para facilitar a escolha
            System.out.println("\n=== Solicitações Pendentes ===");
            for(int i = 0; i < pends.size(); i++) {
                System.out.println((i + 1) + ". " + pends.get(i));
            }
            
            System.out.print("\nNúmero da solicitação para Aprovar (0 para sair): ");
            try {
                int escolha = Integer.parseInt(scin.nextLine());
                
                if(escolha > 0 && escolha <= pends.size()) {
                    // Obtém o ID real da solicitação escolhida
                    int idSolReal = pends.get(escolha - 1).getId();
                    
                    if(sistema.tratarSolicitacao(idSolReal, true)) {
                        System.out.println("✅ Solicitação aprovada com sucesso!");
                    } else {
                        System.out.println("❌ Erro ao aprovar solicitação.");
                    }
                } else if(escolha != 0) {
                    System.out.println("⚠️ Número inválido.");
                }
            } catch (NumberFormatException e) {
                System.out.println("⚠️ Introduza um número válido.");
            }
        });

        // Handler Listar
        menu.setHandler(2, () -> listarFuncionariosUI(coo));

        // Handler Contratar (direto pelo COO)
        menu.setHandler(3, () -> {
            System.out.println("\n--- Contratação Direta (COO) ---");
            
            // Validação do ID
            int id;
            while (true) {
                System.out.print("ID do novo funcionário: ");
                try {
                    id = Integer.parseInt(scin.nextLine().trim());
                    if (id > 0) break;
                    System.out.println("⚠️ O ID deve ser um número positivo!");
                } catch (NumberFormatException e) {
                    System.out.println("⚠️ Introduza um número válido!");
                }
            }
            
            // Validação do Nome (obrigatório)
            String nome;
            while (true) {
                System.out.print("Nome do funcionário: ");
                nome = scin.nextLine().trim();
                if (!nome.isEmpty()) break;
                System.out.println("⚠️ O nome é obrigatório!");
            }
            
            // Validação da Password (obrigatória)
            String pass;
            while (true) {
                System.out.print("Password: ");
                pass = scin.nextLine().trim();
                if (!pass.isEmpty()) break;
                System.out.println("⚠️ A password é obrigatória!");
            }
            
            // Validação do Posto (obrigatório)
            String posto;
            while (true) {
                System.out.print("Posto (Cozinha/Balcao/etc): ");
                posto = scin.nextLine().trim();
                if (!posto.isEmpty()) break;
                System.out.println("⚠️ O posto é obrigatório!");
            }

            // Mostrar restaurantes disponíveis e pedir ID
            System.out.println("\n📍 Restaurantes disponíveis:");
            List<Restaurante> restaurantes = sistema.listarRestaurantes();
            for (Restaurante r : restaurantes) {
                System.out.println("   ID " + r.getId() + " - " + r.getLocalizacao());
            }
            
            int idRestaurante;
            while (true) {
                System.out.print("ID do Restaurante onde vai trabalhar: ");
                try {
                    idRestaurante = Integer.parseInt(scin.nextLine().trim());
                    // Verificar se o restaurante existe
                    boolean existe = false;
                    for (Restaurante r : restaurantes) {
                        if (r.getId() == idRestaurante) {
                            existe = true;
                            break;
                        }
                    }
                    if (existe) break;
                    System.out.println("⚠️ Restaurante não encontrado!");
                } catch (NumberFormatException e) {
                    System.out.println("⚠️ Introduza um número válido!");
                }
            }

            if (sistema.contratarTrabalhador(id, nome, pass, posto, idRestaurante)) {
                System.out.println("✅ Funcionário contratado com sucesso!");
            } else {
                System.out.println("❌ Erro ao contratar (ID já existe ou dados inválidos).");
            }
        });

        // Handler Despedir (direto pelo COO)
        menu.setHandler(4, () -> {
            System.out.println("\n--- Despedimento Direto (COO) ---");
            
            // Mostra lista de funcionários primeiro
            listarFuncionariosUI(coo);
            
            System.out.print("\nID do funcionário a despedir (0 para cancelar): ");
            try {
                int idAlvo = Integer.parseInt(scin.nextLine().trim());
                
                if (idAlvo == 0) {
                    System.out.println("Operação cancelada.");
                    return;
                }
                
                // Confirmação
                System.out.print("Tem a certeza que deseja despedir o funcionário " + idAlvo + "? (S/N): ");
                String confirmacao = scin.nextLine().trim().toUpperCase();
                
                if (confirmacao.equals("S")) {
                    if (sistema.despedirFuncionario(idAlvo)) {
                        System.out.println("✅ Funcionário despedido com sucesso!");
                    } else {
                        System.out.println("❌ Erro ao despedir (funcionário não existe ou não pode ser despedido).");
                    }
                } else {
                    System.out.println("Operação cancelada.");
                }
            } catch (NumberFormatException e) {
                System.out.println("⚠️ Introduza um ID válido.");
            }
        });

        menu.run();
    }

    private void listarFuncionariosUI(Funcionario quem) {
        List<Funcionario> lista = (quem instanceof COO) ? sistema.listarFuncionariosGlobal() : sistema.listarFuncionariosDoRestaurante(quem.getId());
        for(Funcionario f : lista) System.out.println(f);
    }
}

