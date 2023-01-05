import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.DataFormatException;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


public class ChatClient {

    // Variáveis relacionadas com a interface gráfica --- * NÃO MODIFICAR *
    JFrame frame = new JFrame("Chat Client");
    private JTextField chatBox = new JTextField();
    private JTextArea chatArea = new JTextArea();
    // --- Fim das variáveis relacionadas coma interface gráfica

    // Se for necessário adicionar variáveis ao objecto ChatClient, devem
    // ser colocadas aqui
    private int port, state;

    private Socket clientSocket;
    private String server;

    private BufferedReader inFromServer;
    private DataOutputStream outToServer;


    // Método a usar para acrescentar uma string à caixa de texto
    // * NÃO MODIFICAR *
    public void printMessage(final String message) {
        chatArea.append(message);
    }

    
    // Construtor
    public ChatClient(String server, int port) throws IOException {

        // Inicialização da interface gráfica --- * NÃO MODIFICAR *
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(chatBox);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.SOUTH);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.setSize(500, 300);
        frame.setVisible(true);
        chatArea.setEditable(false);
        chatBox.setEditable(true);
        chatBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    newMessage(chatBox.getText());
                } catch (IOException ex) {
                } finally {
                    chatBox.setText("");
                }
            }
        });
        frame.addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                chatBox.requestFocusInWindow();
            }
        });
        // --- Fim da inicialização da interface gráfica

        // Se for necessário adicionar código de inicialização ao
        // construtor, deve ser colocado aqui
        this.port = port;
        this.server = server;
        state = 0;
        clientSocket = new Socket(server, port);
        outToServer = new DataOutputStream(clientSocket.getOutputStream());
        inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }


    // Método invocado sempre que o utilizador insere uma mensagem
    // na caixa de entrada
    public void newMessage(String message) throws IOException {
        // PREENCHER AQUI com código que envia a mensagem ao servidor
        message = message.trim();
        System.out.println("SENDING:" + message);
        outToServer.write((message + "\n").getBytes("UTF-8"));
    }

    
    // Método principal do objecto
    public void run() throws IOException {
        // PREENCHER AQUI
        try {
            BufferedReader data;
            boolean flag = true;
            while(flag){
              data = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
              String msg = data.readLine();
              System.out.println("RECEIVED: " + msg);
  
              if (msg == null) {
                flag = false;
                break;
              }
  
              String[] tks = msg.split(" ");
              switch(tks[0]) {
                case "MESSAGE": {
                  msg = msg.replaceFirst("MESSAGE","").replaceFirst(tks[1],"");
                  msg = tks[1] + ":" + msg;
                  break;
                }
                case "PRIVATE": {
                    break;
                }
                case "NEWNICK": {
                  msg = tks[1] + " changed his nickname to: " + tks[2];
                  break;
                }
                case "JOINED": {
                  msg = tks[1] + " joined the room";
                  break;
                }
                case "LEFT": {
                  msg = tks[1] + " left the room";
                  break;
                }
              }
  
              System.out.println("PRINTING: " + msg);
  
              if(msg.compareTo("BYE\n") == 0) {
                flag = false;
              }
              msg = msg + "\n";
              printMessage(msg);
            }
          } catch(Exception e) {
            e.printStackTrace();
          }
    }
    

    // Instancia o ChatClient e arranca-o invocando o seu método run()
    // * NÃO MODIFICAR *
    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
        client.run();
    }

}
