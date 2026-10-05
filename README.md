# CampusHub

O CampusHub é um aplicativo Android voltado à comunidade acadêmica. A proposta é reunir, em um único lugar, os eventos oferecidos pela universidade e permitir que os alunos consultem as informações e gerenciem suas inscrições.

O projeto está sendo desenvolvido como atividade acadêmica da disciplina de Desenvolvimento Mobile. O fluxo principal solicitado inicialmente já está funcional e o aplicativo continua evoluindo em etapas.

## Objetivo

O aplicativo deverá permitir que o aluno:

- crie uma conta;
- faça login e logout;
- recupere sua senha;
- visualize e edite seu perfil;
- consulte os eventos disponíveis;
- veja os detalhes de um evento;
- inscreva-se ou cancele sua inscrição;
- consulte os eventos em que está inscrito na tela **Meus Eventos**.

A estrutura foi mantida simples para que novas funcionalidades possam ser acrescentadas sem misturar interface, estado da tela e acesso ao Firebase.

## Estado atual

Neste momento, estão implementados:

- cadastro com criação da conta no Firebase Authentication e do perfil no Firestore;
- login, logout, restauração de sessão e recuperação de senha;
- consulta e edição do perfil;
- listagem e detalhes de eventos;
- inscrição e cancelamento com controle de vagas;
- tela **Meus Eventos**;
- estados de carregamento, lista vazia e erro.

A próxima etapa proposta para a disciplina ainda está pendente: favoritos, comentários, avaliações e busca/filtros de eventos.

## Tecnologias

- Kotlin;
- Android SDK nativo;
- layouts em XML;
- View Binding;
- ViewModel e LiveData;
- Material Components;
- Gradle com Kotlin DSL;
- Firebase Authentication;
- Cloud Firestore.

## Como executar

### Pré-requisitos

- Android Studio instalado;
- SDK do Android configurado;
- emulador Android ou dispositivo físico com Android 9 (API 28) ou superior;
- Git instalado para clonar o repositório.

### Passos

1. Clone o repositório:

   ```bash
   git clone https://github.com/HemannB/Campus-Hub.git
   ```

2. Abra a pasta clonada no Android Studio.

3. Aguarde a sincronização do Gradle. Se o Android Studio solicitar algum componente do SDK, instale-o.

4. Selecione um emulador ou conecte um dispositivo Android com a depuração USB habilitada.

5. Execute o módulo `app` pelo botão **Run** do Android Studio.

Também é possível validar o projeto pelo terminal, na raiz do repositório:

```bash
./gradlew assembleDebug
```

## Organização do código

O projeto usa uma arquitetura simples inspirada em MVVM:

```text
Activity -> ViewModel -> Repository -> Firebase
```

## Observação

O CampusHub é um projeto acadêmico e não está pronto para uso em produção. As funcionalidades são implementadas gradualmente conforme o desenvolvimento da disciplina.
