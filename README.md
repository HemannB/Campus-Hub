# CampusHub

O CampusHub é um aplicativo Android voltado à comunidade acadêmica. A proposta é reunir, em um único lugar, os eventos oferecidos pela universidade e permitir que os alunos consultem as informações e gerenciem suas inscrições.

O projeto está sendo desenvolvido como atividade acadêmica da disciplina de Desenvolvimento Mobile e ainda se encontra em fase inicial.

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

A estrutura poderá ser ampliada futuramente, mas o foco atual é atender a esse fluxo principal do aluno de maneira simples e fácil de manter.

## Estado atual

Neste momento, o projeto possui:

- tela inicial do CampusHub;
- navegação da tela inicial para a tela de login;
- interface da tela de login;
- validação local inicial dos campos de e-mail e senha;
- configuração base do projeto no Firebase.

O login com Firebase, o cadastro, a recuperação de senha e as funcionalidades de eventos ainda estão em desenvolvimento.

## Tecnologias

- Kotlin;
- Android SDK nativo;
- layouts em XML;
- Material Components;
- Gradle com Kotlin DSL;
- Firebase, inicialmente para autenticação e banco de dados.

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

## Organização planejada

O projeto seguirá uma arquitetura simples inspirada em MVVM:

```text
Activity -> ViewModel -> Repository -> Firebase
```

Essa separação mantém as telas responsáveis pela interface, os ViewModels pelo estado da UI e os repositórios pelo acesso aos serviços do Firebase, sem adicionar complexidade desnecessária ao projeto acadêmico.

## Observação

O CampusHub ainda não está pronto para uso em produção. As funcionalidades serão implementadas gradualmente conforme o desenvolvimento da disciplina.
