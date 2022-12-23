# Chat-Cliente-Server

Desenvolvimento em Java de um servidor de chat e de um cliente simples para comunicar com ele

## Linha de Comando

  - java ChatServer 8000
  - java ChatClient localhost 8000

## Comandos

  - /nick *nome*
  - /join *sala*
  - /leave
  - /bye

## Mensagens do Servidor para o Cliente

  - **OK**
  - **ERROR**
  - **MESSAGE** *nome* *mensagem*
  - **NEWNICK** *nome_antigo* *nome_novo*
  - **JOINED** *nome*
  - **LEFT** *nome*
  - **BYE**
  
## Estados do servidor 
  
  - init
  - outside
  - inside
  
