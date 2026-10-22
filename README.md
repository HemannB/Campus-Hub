# CampusHub

O CampusHub é um aplicativo Android para divulgação e acompanhamento de
eventos universitários. Alunos podem criar uma conta, consultar eventos,
gerenciar inscrições e favoritos e compartilhar experiências por meio de
comentários, avaliações e publicações multimídia.

O projeto foi desenvolvido como atividade acadêmica da disciplina de
Desenvolvimento Mobile e contempla as três etapas propostas.

## Funcionalidades

### Conta, perfil e eventos

- cadastro com Firebase Authentication e perfil armazenado no Firestore;
- login, logout, restauração de sessão e recuperação de senha;
- visualização e edição do perfil acadêmico;
- listagem e consulta dos detalhes dos eventos;
- inscrição e cancelamento com controle de vagas;
- tela **Meus Eventos** com os eventos em que o aluno está inscrito.

### Favoritos, comentários e avaliações

- favoritar e desfavoritar eventos sem realizar uma inscrição;
- tela **Meus Favoritos**;
- busca de eventos pelo título;
- filtros por categoria e situação, entre próximos e encerrados;
- comentários com nome do autor e data de publicação;
- edição e exclusão permitidas somente ao autor do comentário;
- avaliações de 1 a 5 para alunos inscritos em eventos encerrados;
- uma avaliação por aluno e evento, com possibilidade de alterar a nota;
- apresentação da média e da quantidade de avaliações nos detalhes do evento.

### Recursos nativos e publicações

- exibição do endereço do evento e de sua localização no Google Maps;
- captura de fotos com a câmera;
- seleção de fotos existentes no dispositivo;
- gravação, reprodução e exclusão de áudios de até 30 segundos;
- seleção, reprodução e exclusão de vídeos de até 30 segundos;
- identificação do autor e da data de cada publicação;
- exclusão de publicações permitida somente ao próprio autor.

As publicações de fotos, áudios e vídeos são independentes da inscrição, dos
comentários e das avaliações. Os arquivos e seus metadados são mantidos
localmente no dispositivo, conforme permitido pelos requisitos da atividade,
e não são sincronizados entre dispositivos.

## Regras funcionais importantes

- favoritar um evento não realiza uma inscrição;
- somente usuários autenticados podem publicar comentários e mídias;
- somente o autor pode editar ou excluir o próprio comentário;
- somente alunos inscritos podem avaliar um evento;
- a avaliação é liberada apenas depois do horário de término do evento;
- eventos sem datas válidas não podem ser avaliados;
- cada aluno possui no máximo uma avaliação por evento, mas pode atualizar a
  nota;
- fotos, áudios e vídeos podem ser publicados independentemente da inscrição
  no evento.

## Tecnologias

- Kotlin e Android SDK nativo;
- layouts XML e View Binding;
- ViewModel e LiveData;
- Material Components e RecyclerView;
- Gradle com Kotlin DSL;
- Firebase Authentication;
- Cloud Firestore;
- Google Maps SDK e Geocoder;
- Photo Picker, FileProvider, MediaRecorder, MediaPlayer e VideoView.

## Organização do código

O projeto usa uma arquitetura simples inspirada em MVVM:

```text
Activity -> ViewModel -> Repository -> Firebase ou armazenamento local
```

Os principais diretórios são:

```text
app/src/main/java/br/com/uri/campushub/
├── auth/        # telas de autenticação e recuperação de senha
├── model/       # modelos e regras de domínio
├── repository/  # acesso ao Firebase e ao armazenamento local
├── student/     # telas e adaptadores da área do aluno
└── viewmodel/   # estado e regras de apresentação
```

O Firestore armazena usuários, eventos, inscrições, favoritos, comentários e
avaliações. As publicações multimídia são armazenadas nos arquivos internos e
nas preferências locais do aplicativo.

## Como executar

### Pré-requisitos

- Android Studio;
- SDK do Android configurado;
- emulador ou dispositivo com Android 9 (API 28) ou superior;
- projeto Firebase configurado;
- chave da API do Google Maps habilitada para Android.

### Configuração

1. Clone o repositório:

   ```bash
   git clone https://github.com/HemannB/Campus-Hub.git
   cd Campus-Hub
   ```

2. Crie o arquivo `secrets.properties` na raiz do projeto e informe a chave do
   Google Maps:

   ```properties
   MAPS_API_KEY=SUA_CHAVE_DO_GOOGLE_MAPS
   ```

3. Confirme que `app/google-services.json` corresponde ao projeto Firebase que
   será utilizado.

4. Abra o projeto no Android Studio e aguarde a sincronização do Gradle.

5. Selecione um dispositivo e execute o módulo `app`.

Sem uma chave válida do Google Maps, as demais funcionalidades continuam
disponíveis, mas o mapa do evento não será carregado corretamente.

## Validação

Para compilar o APK de desenvolvimento e executar os testes locais:

```bash
./gradlew testDebugUnitTest assembleDebug
```

Para executar o Android Lint:

```bash
./gradlew lintDebug
```

Os testes de avaliação cobrem eventos ainda em andamento, eventos encerrados,
o instante exato do término e eventos com datas ausentes ou inconsistentes.

## Observação

O CampusHub é um projeto acadêmico. O armazenamento local das publicações foi
adotado deliberadamente para atender ao escopo da atividade sem exigir um
serviço de hospedagem de mídias.
