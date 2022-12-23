# Chat-Cliente-Server

Desenvolvimento em Java de um servidor de chat e de um cliente simples para comunicar com ele

## Linha de Comando

  - java ChatServer 8000
  - java ChatClient localhost 8000

## Comandos

  - /nick *nome*
    - escolher nome ou mudar de nome. **Atenção**, o nome escolhido não pode estar em uso!   
  - /join *sala*
    - entrar numa sala ou mudar de sala. Se não exister, é criada 
  - /leave
    - sair da sala 
  - /bye
    - sair do chat 

## Mensagens do Servidor para o Cliente

  - **OK**
    - sucesso do comando enviado pelo cliente 
  - **ERROR**
    - insucesso do comando enviado pelo cliente 
  - **MESSAGE** *nome* *mensagem*
    - difundir aos utilizadores numa sala a *mensagem* (simples) enviada pelo utilizador *nome*, também nessa sala.
  - **NEWNICK** *nome_antigo* *nome_novo*
    - indicar a todos os utilizadores duma sala que o utilizador *nome_antigo*, que está nessa sala, mudou de nome para *nome_novo*
  - **JOINED** *nome*
    - indicar aos utilizadores numa sala que entrou um novo utilizador, com o nome *nome*, nessa sala
  - **LEFT** *nome*
    - indicar aos utilizadores numa sala que o utilizador com o nome *nome*, que também se encontrava nessa sala, saiu    
  - **BYE**
    - confirmar a um utilizador que invocou o comando */bye* a sua saída
  
## Estados do servidor 
  
  - init
    - Estado inicial de um utilizador que acabou de estabelecer a conexão ao servidor
    - Ainda sem nome associado 
  - outside
    - Utilizador já tem um nome associado, mas não está em nenhuma sala de chat
  - inside
    - Utilizador está numa sala de chat, podendo enviar mensagens simples e devendo receber todas as mensagens que os outros utilizadores nessa sala enviem
