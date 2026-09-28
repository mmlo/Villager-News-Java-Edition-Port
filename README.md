# Villager News (Java Edition Port)

[![Minecraft 1.20.1](https://img.shields.io/badge/Minecraft-1.20.1-blue.svg)](https://minecraft.net/)
[![Forge 47.4.10+](https://img.shields.io/badge/Forge-47.4.10%2B-orange.svg)](https://files.minecraftforge.net/)
[![Java 17](https://img.shields.io/badge/Java-17-red.svg)](https://adoptium.net/)
[![Status](https://img.shields.io/badge/Status-Unofficial%20Port-yellow.svg)](#-isenção-de-responsabilidade-e-atribuição-disclaimer)
[![License](https://img.shields.io/badge/License-All%20Rights%20Reserved%20(Assets)-lightgrey.svg)](#-licença)

> [!WARNING]
> **AVISO DE PORT NÃO-OFICIAL (UNOFFICIAL FAN PORT):**
> Este projeto é uma adaptação comunitária e independente feita por fãs. Ele **NÃO** é um produto oficial nem possui vínculo direto com a **Element Animation** ou a **Oreville Studios**.
> 
> **Por favor, NÃO entre em contato com a Element Animation ou com a Oreville Studios para pedir suporte ou relatar problemas desta versão Java.** Utilize exclusivamente a aba de [Issues](../../issues) deste repositório para reportar quaisquer bugs ou dúvidas.

---

## 📖 Sobre o Mod

O **Villager News (Java Edition Port)** é um port fiel do clássico addon oficial do **Minecraft Bedrock "Villager News 1.0"** para o **Minecraft Vanilla Java Edition 1.20.1 com Minecraft Forge**.

Ele recria na edição Java toda a atmosfera dos icônicos episódios de notícias dos aldeões:

- 🎙️ **Apresentadores e Repórteres Villager**:
  - Modelos 3D autênticos com animações baseadas no motor Molang do Bedrock.
  - Comportamentos de jornalistas, segurando microfones e placas de notícias.
- 🗣️ **Mais de 2.200 Linhas de Voz e Diálogos**:
  - Falas originais completas em **Inglês**.
  - Localização completa e inédita com vozes em **Português Brasileiro (PT-BR)**.
- 📰 **Manual do Repórter In-Game**:
  - Item de guia interativo com interface gráfica customizada e suporte a troca de idiomas.
- 🎩 **Acessórios e Itens Cosméticos**:
  - Microfone de repórter (com pose e animação em primeira e terceira pessoa).
  - Chapéu do Prefeito, Bigodes, Elmo Testificate e Nariz Removível de Villager.
- ⚡ **Compatibilidade Vanilla & Forge**:
  - Funciona em servidores dedicados e clientes individuais com Minecraft Forge 1.20.1.

---

## 📦 Como Instalar

1. Baixe e instale o **[Minecraft Forge 1.20.1](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html)** (versão `47.4.10` ou mais recente recomendada).
2. Baixe o arquivo `.jar` compilado mais recente na página de **[Releases](../../releases)**.
3. Copie o arquivo `.jar` para a pasta `.minecraft/mods` da sua instalação do jogo.
4. Abra o Minecraft Launcher, selecione o perfil do **Forge 1.20.1** e inicie o jogo.

---

## 🎮 Comandos In-Game

O mod disponibiliza o comando raiz `/vn` para operadores (permissão de nível 2):

- `/vn info`: Exibe informações sobre o mod, total de diálogos e sons registrados.
- `/vn speak <alvos> <groupId>`: Força entidades do Villager News a reproduzirem um grupo específico de diálogo e áudio.
- `/vn random <alvos>`: Dispara um diálogo aleatório nas entidades selecionadas.

---

## 🛠️ Como Compilar a Partir do Código-Fonte

### Pré-requisitos
- **Java Development Kit (JDK) 17** instalado.
- Git.

### Passos
1. Clone o repositório:
   ```bash
   git clone https://github.com/SEU_USUARIO/villager-news.git
   cd villager-news
   ```
2. Compile o mod usando o Gradle Wrapper:
   - **Linux / macOS:**
     ```bash
     ./gradlew build
     ```
   - **Windows:**
     ```cmd
     gradlew.bat build
     ```
3. O mod compilado será gerado em:
   ```
   build/libs/oreville_vn-1.0.4-port.1.jar
   ```

---

## 🤝 Contribuições e Relato de Bugs

Encontrou algum bug visual, erro de tradução ou problema de compatibilidade?
1. Verifique se o bug já não foi reportado na aba de **[Issues](../../issues)**.
2. Abra um novo chamado usando o template de [Bug Report](../../issues/new?template=bug_report.md), anexando o log do jogo (`latest.log` ou `crash-reports`).

---

## 📜 Isenção de Responsabilidade e Atribuição (Disclaimer)

- **Criação e Conteúdo Original:**
  Todo o universo do **Villager News**, roteiros, vozes originais, personagens, piadas e modelos foram concebidos e criados por **[Element Animation](https://www.elementanimation.com/)** e produzidos para o Minecraft Bedrock pela **[Oreville Studios](https://www.orevillestudios.com/)**. Todos os direitos sobre o universo do Villager News pertencem a eles.
- **Natureza do Projeto:**
  Este é um port comunitário e amador criado exclusivamente com fins de compatibilidade e entretenimento gratuito para a comunidade Java.
- **Aviso Legal da Mojang:**
  *Este projeto não é um produto oficial do Minecraft. Não é aprovado nem associado à Mojang Studios ou à Microsoft Corporation.*

---

## ⚖️ Licença

- O código-fonte de adaptação Java Forge deste repositório está disponível para estudo e contribuições da comunidade.
- Todos os recursos originais (arquivos de áudio, geometrias Bedrock, texturas e marcas) são de propriedade intelectual de **Element Animation / Oreville Studios**.
