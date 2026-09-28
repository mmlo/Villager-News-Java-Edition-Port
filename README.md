# Villager News (Java Edition Port)

[![Minecraft 1.20.1](https://img.shields.io/badge/Minecraft-1.20.1-blue.svg)](https://minecraft.net/)
[![Forge 47.4.10+](https://img.shields.io/badge/Forge-47.4.10%2B-orange.svg)](https://files.minecraftforge.net/)
[![Java 17](https://img.shields.io/badge/Java-17-red.svg)](https://adoptium.net/)
[![Status](https://img.shields.io/badge/Status-Unofficial%20Port-yellow.svg)](#-disclaimer--attribution)
[![CurseForge](https://img.shields.io/badge/CurseForge-Project%201716868-firebrick.svg)](https://www.curseforge.com/minecraft/mc-mods)

> 🌐 **Language / Idioma:** [English](#-english) | [Português do Brasil](#-português-do-brasil)

---

# 🇬🇧 English

> [!WARNING]
> **UNOFFICIAL FAN PORT DISCLAIMER:**
> This project is an independent, community-driven fan adaptation. It is **NOT** an official product and is **NOT** affiliated with, sponsored by, or endorsed by **Element Animation** or **Oreville Studios**.
> 
> **Please DO NOT contact Element Animation or Oreville Studios for support or to report bugs found in this Java edition port.** All feedback, bug reports, and inquiries must be submitted exclusively via this repository's [GitHub Issues](../../issues) tracker.

---

## 📖 About the Mod

**Villager News (Java Edition Port)** is a faithful port of the official **Minecraft Bedrock "Villager News 1.0"** addon (originally crafted by **Element Animation** & **Oreville Studios**) to **Minecraft Vanilla Java Edition 1.20.1 using Minecraft Forge**.

It brings the hilarious, chaotic world of the famous Villager News broadcast directly into Minecraft Java:

- 🎙️ **Villager Anchors & Reporters**:
  - Authentic 3D entity models featuring Bedrock geometry and dynamic animations driven by a custom Molang interpreter.
  - Authentic journalistic behaviors (holding handheld microphones, displaying news placards, custom reactions).
- 🗣️ **Over 2,200 Spoken Lines & Dialogues**:
  - Complete original voice lines in **English**.
  - Brand-new, full voice-acted localization in **Brazilian Portuguese (PT-BR)**.
- 📰 **In-Game Reporter's Manual**:
  - Custom interactive guide book GUI with bilingual language switching support.
- 🎩 **Wearables & Cosmetic Items**:
  - Reporter Microphone (custom first-person and third-person holding animations).
  - Mayor's Hat, Mustache, Testificate Helmet, and Detachable Villager Nose.
- ⚡ **Multiplayer & Server Ready**:
  - Full client-server synchronization built on Forge 1.20.1 networking.

---

## 📦 Installation

1. Ensure you have **[Minecraft Forge 1.20.1](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html)** installed (build `47.4.10` or newer is recommended).
2. Download the latest compiled `.jar` from the **[Releases](../../releases)** tab or via **[CurseForge](https://www.curseforge.com/minecraft/mc-mods)**.
3. Place the downloaded `.jar` file into your `.minecraft/mods` directory.
4. Launch Minecraft using your Forge 1.20.1 profile and enjoy!

---

## 🎮 In-Game Commands

The mod includes the `/vn` command for operators (permission level 2):

- `/vn info`: Shows loaded dialogues, registered sounds, and port status.
- `/vn speak <targets> <groupId>`: Forces target Villager News entities to play a specific dialogue line and audio.
- `/vn random <targets>`: Triggers a random conversational dialogue on selected entities.

---

## 🛠️ Building from Source

### Prerequisites
- **Java Development Kit (JDK) 17** installed (e.g. Eclipse Temurin, OpenJDK).
- Git.

### Build Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/mmlo/Villager-News-Java-Edition-Port.git
   cd Villager-News-Java-Edition-Port
   ```
2. Build with the Gradle Wrapper:
   - **Linux / macOS:**
     ```bash
     ./gradlew build
     ```
   - **Windows:**
     ```cmd
     gradlew.bat build
     ```
3. The resulting `.jar` file will be generated in `build/libs/`.

---

## 🤝 Bug Reports & Contributing

Encountered an issue, model glitch, or translation inconsistency?
1. Check existing reports on the **[Issues](../../issues)** page.
2. Open a new report using the provided [Bug Report Template](../../issues/new?template=bug_report.md) with relevant logs (`latest.log` or `crash-reports`).

---

## 📜 Disclaimer & Attribution

- **Original Creation & Intellectual Property:**
  All concepts, script lines, original voices, characters, and models from **Villager News** were created by **[Element Animation](https://www.elementanimation.com/)** and adapted for Minecraft Bedrock by **[Oreville Studios](https://www.orevillestudios.com/)**. All rights to the original content remain with their respective copyright holders.
- **Nature of the Port:**
  This project is a non-commercial, non-profit community endeavor dedicated to bringing the addon to Java Edition players.
- **Mojang Disclaimer:**
  *Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.*

---

## ⚖️ License

- The Java port source code is licensed under the repository terms.
- All original media assets (sound clips, textures, and Bedrock geometries) remain the intellectual property of **Element Animation / Oreville Studios**.

---
---

# 🇧🇷 Português do Brasil

> [!WARNING]
> **AVISO DE PORT NÃO-OFICIAL (ADAPTAÇÃO DE FÃS):**
> Este projeto é uma adaptação comunitária e independente feita por fãs. Ele **NÃO** é um produto oficial nem possui vínculo, patrocínio ou aprovação da **Element Animation** ou da **Oreville Studios**.
> 
> **Por favor, NÃO entre em contato com a Element Animation ou com a Oreville Studios para pedir suporte ou relatar problemas desta versão Java.** Utilize exclusivamente a aba de [Issues](../../issues) deste repositório para reportar quaisquer dúvidas ou falhas.

---

## 📖 Sobre o Mod

O **Villager News (Java Edition Port)** é um port fiel do clássico addon oficial do **Minecraft Bedrock "Villager News 1.0"** (criado originalmente por **Element Animation** & **Oreville Studios**) para o **Minecraft Vanilla Java Edition 1.20.1 com Minecraft Forge**.

Ele traz para o Minecraft Java toda a diversão e caos da redação do telejornal dos aldeões:

- 🎙️ **Apresentadores e Repórteres Villager**:
  - Modelos 3D autênticos com renderização das geometrias do Bedrock e expressões dinâmicas animadas via Molang.
  - Comportamentos jornalísticos fiéis (segurando microfone, exibindo placas de notícias, reações contextuais).
- 🗣️ **Mais de 2.200 Linhas de Áudio Falado**:
  - Dublagem original completa em **Inglês**.
  - Localização inédita completa com vozes em **Português Brasileiro (PT-BR)**.
- 📰 **Manual do Repórter In-Game**:
  - Livro interativo com interface gráfica própria e suporte a troca de idiomas.
- 🎩 **Itens Cosméticos e Acessórios**:
  - Microfone de Repórter (animação e pose exclusiva em 1ª e 3ª pessoa).
  - Chapéu do Prefeito, Bigodes, Elmo Testificate e Nariz Removível de Villager.
- ⚡ **Pronto para Multiplayer e Servidores**:
  - Totalmente compatível com clientes e servidores dedicados Forge 1.20.1.

---

## 📦 Como Instalar

1. Baixe e instale o **[Minecraft Forge 1.20.1](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html)** (recomendada versão `47.4.10` ou superior).
2. Baixe o arquivo `.jar` mais recente na aba de **[Releases](../../releases)** deste repositório ou no **[CurseForge](https://www.curseforge.com/minecraft/mc-mods)**.
3. Coloque o arquivo `.jar` baixado dentro da pasta `.minecraft/mods`.
4. Inicie o Minecraft no perfil do Forge 1.20.1 e aproveite!

---

## 🎮 Comandos In-Game

O mod disponibiliza o comando `/vn` para operadores (permissão de nível 2):

- `/vn info`: Exibe informações sobre o mod, diálogos carregados e sons registrados.
- `/vn speak <alvos> <groupId>`: Força entidades do Villager News a reproduzirem um grupo específico de diálogo e áudio.
- `/vn random <alvos>`: Dispara um diálogo aleatório nas entidades selecionadas.

---

## 🛠️ Como Compilar a Partir do Código-Fonte

### Pré-requisitos
- **Java Development Kit (JDK) 17** instalado.
- Git.

### Passos de Compilação
1. Clone o repositório:
   ```bash
   git clone https://github.com/mmlo/Villager-News-Java-Edition-Port.git
   cd Villager-News-Java-Edition-Port
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
3. O arquivo `.jar` compilado pronto para uso estará na pasta `build/libs/`.

---

## 🤝 Contribuições e Relato de Bugs

Encontrou algum problema visual, de áudio ou compatibilidade?
1. Veja se o problema já não foi relatado na aba de **[Issues](../../issues)**.
2. Abra um chamado através do [Modelo de Relato de Bug](../../issues/new?template=bug_report.md) incluindo os logs (`latest.log` ou `crash-reports`).

---

## 📜 Isenção de Responsabilidade e Atribuição

- **Criação e Conteúdo Original:**
  Todo o universo do **Villager News**, roteiros, vozes originais, personagens e modelos foram criados por **[Element Animation](https://www.elementanimation.com/)** e adaptados para o Bedrock pela **[Oreville Studios](https://www.orevillestudios.com/)**. Todos os direitos sobre os conteúdos originais pertencem a eles.
- **Natureza do Projeto:**
  Este é um port comunitário feito por fãs, gratuito e sem fins comerciais, com o intuito de trazer o conteúdo aos jogadores da Java Edition.
- **Aviso Legal da Mojang:**
  *Não é um produto oficial do Minecraft. Não aprovado nem associado à Mojang Studios ou à Microsoft Corporation.*

---

## ⚖️ Licença

- O código-fonte de adaptação Java Forge deste repositório está sob os termos de licença do projeto.
- Todos os arquivos de mídia originais (áudios, texturas e geometrias) permanecem como propriedade intelectual de **Element Animation / Oreville Studios**.
