import pymysql
import random
from pymysql import Error

# ==============================================================================
# --- CONFIGURAÇÃO DA CONEXÃO ---
# ==============================================================================
DB_CONFIG = {
    'host': 'localhost',
    'port': 3307,
    'database': 'CadeiaRestaurantesDB', 
    'user': 'root',
    'password': 'root'
}

# ==============================================================================
# --- GERADORES E CONSTANTES ---
# ==============================================================================
NOMES = ["João", "Maria", "Ana", "Pedro", "Tiago", "Sofia", "Beatriz", "Ricardo", "Carlos", "Marta", "Luís", "Inês"]
APELIDOS = ["Silva", "Santos", "Ferreira", "Pereira", "Oliveira", "Costa", "Rodrigues", "Martins", "Gomes", "Lopes"]
POSTOS = ["Grelha", "Caixa", "Balcão", "Limpeza", "Cozinha", "Entregas"]
LOCAIS = ["Braga", "Porto", "Lisboa", "Coimbra", "Aveiro", "Faro"] 

def gerar_nome_aleatorio():
    """Gera um nome completo aleatório para os trabalhadores e gerentes."""
    return f"{random.choice(NOMES)} {random.choice(APELIDOS)}"

# ==============================================================================
# --- FUNÇÃO PRINCIPAL DE POVOAMENTO ---
# ==============================================================================
def povoar_db():
    print("⏳ Iniciando o processo de conexão...")
    try:
        conn = pymysql.connect(**DB_CONFIG)
        if conn:
            cursor = conn.cursor() 
            
            # ------------------------------------------------------------------
            # 1. LIMPEZA E PREPARAÇÃO (DDL)
            # ------------------------------------------------------------------
            print("🏗️ A recriar estrutura da Base de Dados...")
            cursor.execute("SET FOREIGN_KEY_CHECKS=0;")
            
            tabelas_para_limpar = [
                "Stock", "Artigo_Ingrediente", "Ingrediente", "Mensagem_Lida", 
                "Mensagem_Restaurante", "Solicitacao_RH", "Pedido_Artigo", "Pedido", 
                "Artigo", "Trabalhador", "Restaurante", "Gerente", "COO", "Cliente"
            ]
            for t in tabelas_para_limpar:
                cursor.execute(f"DROP TABLE IF EXISTS {t}")
            
            # --- CRIAÇÃO DAS TABELAS ---
            cursor.execute("CREATE TABLE COO (id INT PRIMARY KEY, nome VARCHAR(100), pass VARCHAR(50))")
            # Adicionado id_restaurante à tabela Gerente
            cursor.execute("CREATE TABLE Gerente (id INT PRIMARY KEY, nome VARCHAR(100), pass VARCHAR(50), id_restaurante INT)")
            cursor.execute("CREATE TABLE Cliente (id INT AUTO_INCREMENT PRIMARY KEY, nif VARCHAR(9), pontos INT DEFAULT 0)")
            cursor.execute("CREATE TABLE Restaurante (id INT PRIMARY KEY, localizacao VARCHAR(100), id_gerente INT, FOREIGN KEY (id_gerente) REFERENCES Gerente(id))")
            cursor.execute("CREATE TABLE Trabalhador (id INT PRIMARY KEY, nome VARCHAR(100), pass VARCHAR(50), posto_atual VARCHAR(50), id_restaurante INT, FOREIGN KEY (id_restaurante) REFERENCES Restaurante(id))")
            cursor.execute("CREATE TABLE Artigo (id INT PRIMARY KEY, nome VARCHAR(100), categoria VARCHAR(50), preco DECIMAL(5, 2), descricao TEXT)")
            cursor.execute("CREATE TABLE Ingrediente (id INT PRIMARY KEY, nome VARCHAR(100), alergenio BOOLEAN DEFAULT 0, preco_venda DECIMAL(5, 2) DEFAULT 0.00)")
            cursor.execute("CREATE TABLE Artigo_Ingrediente (id_artigo INT, id_ingrediente INT, PRIMARY KEY (id_artigo, id_ingrediente), FOREIGN KEY (id_artigo) REFERENCES Artigo(id), FOREIGN KEY (id_ingrediente) REFERENCES Ingrediente(id))")
            cursor.execute("CREATE TABLE Stock (id_restaurante INT, id_ingrediente INT, quantidade INT DEFAULT 0, PRIMARY KEY (id_restaurante, id_ingrediente), FOREIGN KEY (id_restaurante) REFERENCES Restaurante(id), FOREIGN KEY (id_ingrediente) REFERENCES Ingrediente(id))")
            cursor.execute("CREATE TABLE Pedido (id INT AUTO_INCREMENT PRIMARY KEY, data_hora DATETIME DEFAULT CURRENT_TIMESTAMP, data_conclusao DATETIME, valor_total DECIMAL(6, 2) DEFAULT 0, id_cliente INT, id_restaurante INT, estado VARCHAR(50), metodo_pagamento VARCHAR(50), local_consumo VARCHAR(50), tempo_estimado INT, FOREIGN KEY (id_cliente) REFERENCES Cliente(id), FOREIGN KEY (id_restaurante) REFERENCES Restaurante(id))")
            cursor.execute("CREATE TABLE Pedido_Artigo (id INT AUTO_INCREMENT PRIMARY KEY, id_pedido INT, id_artigo INT, personalizacao VARCHAR(255), pago_com_pontos BOOLEAN DEFAULT 0, FOREIGN KEY (id_pedido) REFERENCES Pedido(id), FOREIGN KEY (id_artigo) REFERENCES Artigo(id))")
            cursor.execute("CREATE TABLE Mensagem_Restaurante (id INT AUTO_INCREMENT PRIMARY KEY, id_restaurante INT, conteudo TEXT, data_envio DATETIME DEFAULT CURRENT_TIMESTAMP, lida BOOLEAN DEFAULT 0, is_public BOOLEAN DEFAULT 0, FOREIGN KEY (id_restaurante) REFERENCES Restaurante(id))")
            cursor.execute("CREATE TABLE Mensagem_Lida (id_mensagem INT, id_trabalhador INT, data_leitura DATETIME DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (id_mensagem, id_trabalhador), FOREIGN KEY (id_mensagem) REFERENCES Mensagem_Restaurante(id), FOREIGN KEY (id_trabalhador) REFERENCES Trabalhador(id))")
            cursor.execute("CREATE TABLE Solicitacao_RH (id INT AUTO_INCREMENT PRIMARY KEY, id_gerente INT NOT NULL, tipo ENUM('CONTRATAR', 'DESPEDIR', 'REALOCAR') NOT NULL, estado ENUM('PENDENTE', 'APROVADO', 'REJEITADO') DEFAULT 'PENDENTE', nome_candidato VARCHAR(100), pass_candidato VARCHAR(50), posto_proposto VARCHAR(50), id_trabalhador_alvo INT, data_pedido DATETIME DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (id_gerente) REFERENCES Gerente(id))")

            # ------------------------------------------------------------------
            # 2. INSERÇÃO DE STAFF E ESTRUTURA
            # ------------------------------------------------------------------
            print("👥 A inserir equipas e restaurantes...")
            cursor.execute("INSERT INTO COO (id, nome, pass) VALUES (1, %s, 'admin123')", (gerar_nome_aleatorio(),))
            
            for i, local in enumerate(LOCAIS):
                id_ger, id_res = 10 + i, i + 1
                # Enviamos 4 valores para as 4 colunas: id, nome, pass, id_restaurante
                cursor.execute("INSERT INTO Gerente VALUES (%s, %s, %s, %s)", 
                            (id_ger, gerar_nome_aleatorio(), f"GERENTE{id_ger}", id_res))
                cursor.execute("INSERT INTO Restaurante VALUES (%s, %s, %s)", 
                            (id_res, local, id_ger))

            for i in range(40):
                id_t = 200 + i
                cursor.execute("INSERT INTO Trabalhador VALUES (%s, %s, %s, %s, %s)", 
                               (id_t, gerar_nome_aleatorio(), f"TRABALHADOR{id_t}", random.choice(POSTOS), (i % len(LOCAIS)) + 1))
            
            # ------------------------------------------------------------------
            # 3. EMENTA (ARTIGOS)
            # ------------------------------------------------------------------
            print("🍔 A configurar Ementa Diversificada...")
            artigos = [
                # MENUS
                (11, "Menu Whopper Clássico", "Menu", 8.95, "Hambúrguer grelhado, batatas médias e bebida"),
                (12, "Menu Big Mac", "Menu", 8.50, "Dois andares, molho especial, batatas e bebida"),
                (13, "Menu Sem Glúten", "Menu", 10.20, "Hambúrguer em pão de milho, batatas e água"),
                (14, "Menu Veggie Alface-Wrap", "Menu", 9.50, "Hambúrguer vegetal embrulhado em alface"),
                (15, "Menu Fit Sem Lactose", "Menu", 9.00, "Carne grelhada, salada e água"),
                (16, "Menu Criança", "Menu", 6.50, "Hambúrguer simples, batatas pequenas e sumo"),
                (17, "Menu Duplo Queijo", "Menu", 9.80, "Hambúrguer duplo, batatas grandes e bebida"),
                (18, "Menu Prego no Prato", "Menu", 10.50, "Prego de vaca, ovo, batatas e bebida"),
                # HAMBÚRGUERES E PREGOS
                (21, "Whopper", "Hamburguer", 5.50, "Carne de vaca grelhada com vegetais frescos"),
                (22, "Veggie Burger de Grão", "Hamburguer", 6.20, "Hambúrguer 100% vegetal"),
                (23, "Prego em Pão de Milho", "Hamburguer", 7.00, "Bife grelhado com ovo em pão de milho"),
                (24, "Big Mac", "Hamburguer", 5.20, "Hambúrguer de dois andares com molho especial"),
                (25, "Cheeseburger Simples", "Hamburguer", 3.80, "Carne, queijo e pão"),
                (26, "Hambúrguer Duplo Bacon", "Hamburguer", 7.40, "Duas carnes, bacon crocante e queijo"),
                (27, "Hambúrguer Vegan", "Hamburguer", 6.80, "Opção vegetal sem produtos animais"),
                # ACOMPANHAMENTOS
                (31, "Batatas Fritas Médias", "Acompanhamento", 2.20, "Batatas douradas e estaladiças"),
                (32, "Salada Mediterrânea", "Acompanhamento", 3.50, "Alface, tomate, cebola e azeite"),
                (33, "Batatas Grandes", "Acompanhamento", 2.80, "Porção grande de batatas fritas"),
                (34, "Aros de Cebola", "Acompanhamento", 3.20, "Cebola panada e frita"),
                (35, "Salada Simples", "Acompanhamento", 2.50, "Alface e tomate"),
                # BEBIDAS
                (41, "Coca-Cola", "Bebida", 2.00, "Refrigerante clássico"),
                (42, "Água Mineral", "Bebida", 1.50, "Água sem gás"),
                (43, "Coca-Cola Zero", "Bebida", 2.00, "Sem açúcar"),
                (44, "Ice Tea Limão", "Bebida", 2.10, "Bebida refrescante de limão"),
                (45, "Sumo de Laranja", "Bebida", 2.30, "Sumo natural"),
                # SOBREMESAS
                (51, "Gelado de Fruta", "Sobremesa", 2.50, "Sobremesa sem lactose"),
                (52, "Gelado de Baunilha", "Sobremesa", 2.80, "Gelado cremoso"),
                (53, "Tarte de Maçã", "Sobremesa", 3.00, "Servida quente")
            ]
            cursor.executemany("INSERT INTO Artigo VALUES (%s, %s, %s, %s, %s)", artigos)

            # ------------------------------------------------------------------
            # 4. INGREDIENTES
            # ------------------------------------------------------------------
            ingredientes = [
                (1, "Pão de Sésamo", 1, 0.80),
                (2, "Carne de Vaca", 0, 2.00),
                (3, "Alface Iceberg", 0, 0.30),
                (4, "Tomate Fresco", 0, 0.40),
                (5, "Queijo Cheddar", 1, 0.60),
                (6, "Ovo", 1, 0.60),
                (7, "Pão de Milho", 0, 1.00),
                (8, "Cebola Roxa", 0, 0.30),
                (9, "Hambúrguer de Grão", 0, 2.00),
                (10, "Batatas", 0, 1.50),
                (11, "Refrigerante Cola", 0, 1.80),
                (12, "Gelado de Baunilha", 1, 2.00),
                (13, "Polpa de Fruta", 0, 1.50),
                (14, "Água", 0, 1.00),
                (15, "Queijo Vegan", 0, 0.80),
                (16, "Bacon", 0, 0.90),
                (17, "Molho Especial", 0, 0.30)
            ]
            cursor.executemany("INSERT INTO Ingrediente VALUES (%s, %s, %s, %s)", ingredientes)

            # ------------------------------------------------------------------
            # 5. RELAÇÕES ARTIGO_INGREDIENTE
            # ------------------------------------------------------------------
            relacoes = [
                (11, 1), (11, 2), (11, 3), (11, 4), (11, 5), (11, 10), (11, 11),
                (12, 1), (12, 2), (12, 5), (12, 17), (12, 10), (12, 11),
                (13, 7), (13, 2), (13, 3), (13, 4), (13, 10), (13, 14),
                (14, 9), (14, 3), (14, 4), (14, 10), (14, 14),
                (15, 7), (15, 2), (15, 3), (15, 4), (15, 10), (15, 14),
                (16, 1), (16, 2), (16, 10), (16, 13),
                (17, 1), (17, 2), (17, 5), (17, 10), (17, 11),
                (18, 2), (18, 6), (18, 10), (18, 11),
                (21, 1), (21, 2), (21, 3), (21, 4), (21, 5),
                (22, 1), (22, 9), (22, 3), (22, 4),
                (23, 7), (23, 2), (23, 6), 
                (24, 1), (24, 2), (24, 5), (24, 17),
                (25, 1), (25, 2), (25, 5),
                (26, 1), (26, 2), (26, 5), (26, 16),
                (27, 1), (27, 9), (27, 3), (27, 15),
                (31, 10), (32, 3), (32, 4), (32, 8),
                (33, 10), (34, 8), (35, 3), (35, 4),
                (41, 11), (42, 14), (43, 11), (44, 11), (45, 13),
                (51, 13), (52, 12), (53, 4)
            ]
            cursor.executemany("INSERT INTO Artigo_Ingrediente VALUES (%s, %s)", relacoes)

            # ------------------------------------------------------------------
            # 6. STOCK INICIAL
            # ------------------------------------------------------------------
            for r_id in range(1, len(LOCAIS) + 1):
                for i_id in range(1, 18):
                    cursor.execute("INSERT INTO Stock VALUES (%s, %s, %s)", (r_id, i_id, 250))
            
            # --- FINALIZAÇÃO ---
            conn.commit()
            cursor.execute("SET FOREIGN_KEY_CHECKS=1;")
            print("✅ Base de Dados reconstruída com sucesso!")

    except Error as e:
        print(f"❌ Erro: {e}")
    finally:
        if 'conn' in locals() and conn:
            cursor.close()
            conn.close()

if __name__ == '__main__':
    povoar_db()