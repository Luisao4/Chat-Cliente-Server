import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;

enum State {
  init, outside, inside
}

class ChatUser {
  private String msg = "";
  private String nick;
  private ChatRoom chatRoom;
  private State chatState;
  
  ChatUser() {
    this.chatState = State.init;
    this.nick = "";
  }
  void setNick(String nick){
    this.nick = nick;
  }
  String getNick(){
    return this.nick;
  }
  void setState(State state){
    this.chatState = state;
  }
  State getState(){
    return this.chatState;
  }
  void setChatRoom(ChatRoom chatRoom){
    this.chatRoom = chatRoom;
  }
  ChatRoom getChatRoom(){
    return this.chatRoom;
  }
  String getMsg(){
    return this.msg;
  }
  void addMsg(String msg){
    this.msg = this.msg + msg;
  }
  void cleanMsg(){
    this.msg = "";
  }
  boolean isInRoom(){
    if (this.getChatRoom() == null)
      return false;
    else
      return true;
  }
}

class ChatRoom{
  private String title;
  private HashSet<SelectionKey> chatUsers;
  
  ChatRoom(String title){
    this.title = title;
    chatUsers = new HashSet<>();
  }
  
  String getTitle(){
    return this.title;
  }
  
  HashSet<SelectionKey> getChatUsers(){
    return this.chatUsers;
  }
  
  void addChatUser(SelectionKey key){
    this.chatUsers.add(key);
  }
  
  void removeChatUser(SelectionKey key){
    this.chatUsers.remove(key);
  }
  boolean isEmpty(){
    return chatUsers.isEmpty();
  }
}

public class ChatServer
{
  // A pre-allocated buffer for the received data
  static private final ByteBuffer buffer = ByteBuffer.allocate( 16384 );
  
  // Decoder for incoming text -- assume UTF-8
  static private final Charset charset = Charset.forName("UTF8");
  static private final CharsetDecoder decoder = charset.newDecoder();
  static private final CharsetEncoder encoder = charset.newEncoder();
  static private HashSet<SelectionKey> chatUsers = new HashSet<>();
  static private HashSet<ChatRoom> chatRooms = new HashSet<>();

  static public void main( String args[] ) throws Exception {
    // Parse port from command line
    int port = Integer.parseInt( args[0] );
    
    try {
      // Instead of creating a ServerSocket, create a ServerSocketChannel
      ServerSocketChannel ssc = ServerSocketChannel.open();
      
      // Set it to non-blocking, so we can use select
      ssc.configureBlocking( false );
      
      // Get the Socket connected to this channel, and bind it to the
      // listening port
      ServerSocket ss = ssc.socket();
      InetSocketAddress isa = new InetSocketAddress( port );
      ss.bind( isa );
      
      // Create a new Selector for selecting
      Selector selector = Selector.open();
      
      // Register the ServerSocketChannel, so we can listen for incoming
      // connections
      ssc.register( selector, SelectionKey.OP_ACCEPT );
      System.out.println( "Listening on port "+port );
      
      while (true) {
        // See if we've had any activity -- either an incoming connection,
        // or incoming data on an existing connection
        int num = selector.select();
        
        // If we don't have any activity, loop around and wait again
        if (num == 0) {
          continue;
        }
        
        // Get the keys corresponding to the activity that has been
        // detected, and process them one by one
        Set<SelectionKey> keys = selector.selectedKeys();
        Iterator<SelectionKey> it = keys.iterator();
        while (it.hasNext()) {
          // Get a key representing one of bits of I/O activity
          SelectionKey key = it.next();
          
          // What kind of activity is it?
          if (key.isAcceptable()) {
            
            // It's an incoming connection.  Register this socket with
            // the Selector so we can listen for input on it
            Socket s = ss.accept();
            System.out.println( "Got connection from "+s );
            
            // Make sure to make it non-blocking, so we can use a selector
            // on it.
            SocketChannel sc = s.getChannel();
            sc.configureBlocking( false );
            
            // Register it with the selector, for reading
            sc.register( selector, SelectionKey.OP_READ );
            
          } else if (key.isReadable()) {
            
            SocketChannel sc = null;
            
            try {
              
              // connect from new user
              if(key.attachment() == null){
                key.attach(new ChatUser());
                chatUsers.add(key);
              }
              
              boolean ok = processInput( key );
              sc = (SocketChannel) key.channel();
              
              // If the connection is dead, remove it from the selector
              // and close it
              if (!ok) {
                removeChatUser(key);
                key.cancel();
                
                Socket s = null;
                try {
                  s = sc.socket();
                  System.out.println( "Closing connection to "+s );
                  s.close();
                } catch( IOException ie ) {
                  System.err.println( "Error closing socket "+s+": "+ie );
                }
              }
              
            } catch( IOException ie ) {
              
              // On exception, remove this channel from the selector
              key.cancel();
              
              try {
                sc.close();
              } catch( IOException ie2 ) { System.out.println( ie2 ); }
              
              System.out.println( "Closed "+sc );
            }
          }
        }
        
        // We remove the selected keys, because we've dealt with them.
        keys.clear();
      }
    } catch( IOException ie ) {
      System.err.println( ie );
    }
  }
  
  
  // Just read the message from the socket and send it to stdout
  static private boolean processInput( SelectionKey key ) throws IOException {
    
    SocketChannel sc = (SocketChannel) key.channel();
    
    // Read the message to the buffer
    buffer.clear();
    sc.read( buffer );
    buffer.flip();
    
    // If no data, close the connection
    if (buffer.limit()==0) {
      return false;
    }
    
    // Decode and print the message to stdout
    String msg = decoder.decode(buffer).toString();
    
    //Ignore ctrl D
    ChatUser chatUser = (ChatUser) key.attachment();
    msg = chatUser.getMsg() + msg;
    if(msg.endsWith("\n")){ // \n
    }
    else {                  // ctrl D
        chatUser.addMsg(msg);
        return true;
    }

    
    if (msg.startsWith("/nick ")) {
      String nick = msg.substring(6);
      nick = nick.replace("\n", "");
      nickCommand(nick, key);
    } else if (msg.startsWith("/priv ")) {
      String aux = msg.substring(6);
      String destiny = "";
      int i = 0;
      for (i = 0; aux.charAt(i) != ' '; i++) {
        destiny = destiny + aux.charAt(i);
      }
      String privMsg = aux.substring(i + 1);
      privMsg.replace("\n", "");
      privCommand(destiny, key, privMsg);
    } else if (msg.startsWith("/join ")) {
      String chatRoom = msg.substring(6);
      chatRoom = chatRoom.replace("\n", "");
      commandJoin(chatRoom, key);
    } else if (msg.startsWith("/bye")) {
      byeCommand(key); // bye command
    }

    else if (msg.startsWith("/leave")) {
      leaveCommand(key);
    } else {
      msg = msg.replace("\n", "");
      message(key, msg);
    }
    
    chatUser.cleanMsg();  
    return true;
  }

  static private void nickCommand(String nick, SelectionKey key) throws IOException{
    ChatUser chatUser = (ChatUser) key.attachment();

    //check if nick is unique
    for(SelectionKey aux : chatUsers){
      ChatUser auxChatUser = (ChatUser) aux.attachment();
      if(auxChatUser.getNick().compareTo(nick) == 0){
        send(key, "ERROR");
        return;
      }
    }
    //check if is in any room
    if(chatUser.getState() == State.inside){
      String oldnick = chatUser.getNick();
      String message = "NEWNICK" + " " + oldnick + " " + nick;
      ChatRoom chatRoom = chatUser.getChatRoom();
      status(chatRoom, key, message);
    }
    else {
      chatUser.setState(State.outside);
    }
    chatUser.setNick(nick);
    send(key, "OK");
  }
  static private void privCommand(String chatUser, SelectionKey key, String msg) throws IOException{
    for(SelectionKey auxKey : chatUsers){
      ChatUser destiny = (ChatUser) auxKey.attachment();

      if(destiny.getNick().compareTo(chatUser) == 0){

        ChatUser origin = (ChatUser) key.attachment();
        send(auxKey , "PRIVATE " + origin.getNick() + " " + msg);
        send(key    , "PRIVATE " + origin.getNick() + " " + msg);
        return;

      }
    }
    send(key, "ERROR");
  }
  static private void commandJoin(String chatRoom, SelectionKey key) throws IOException{
    ChatUser chatUser = (ChatUser) key.attachment();
    if(chatUser.getState() == State.init){
      send(key, "ERROR");
      return;
    }
    ChatRoom auxChatRoom = chatUser.getChatRoom();
    //check if user already in chatRoom
    if(auxChatRoom != null){
      if(auxChatRoom.getTitle().compareTo(chatRoom) == 0){
        send(key, "ERROR");
        return;
      }
    }
    ChatRoom newChatRoom = null;

    for(ChatRoom aux : chatRooms){
      if(aux.getTitle().compareTo(chatRoom) == 0){
        newChatRoom = aux;
        break;
      }
    }
    if(chatUser.isInRoom()){
      leaveCommand(key);
    }
    if(newChatRoom != null){ //room already exists
      chatUser.setChatRoom(newChatRoom);
      newChatRoom.addChatUser(key);
    }
    else { //room doesn't exist
      newChatRoom = new ChatRoom(chatRoom);
      chatRooms.add(newChatRoom);
      chatUser.setChatRoom(newChatRoom);
      newChatRoom.addChatUser(key);
    }
    send(key, "OK");
    status(newChatRoom, key, "JOINED " + chatUser.getNick());
    chatUser.setState(State.inside);
  }
  
  static private void leaveCommand(SelectionKey key) throws IOException {
    ChatUser chatUser = (ChatUser) key.attachment();
    if(chatUser.getState() != State.inside){
      send(key, "ERROR");
      return;
    }
    ChatRoom chatRoom = chatUser.getChatRoom();
    chatRoom.removeChatUser(key);
    chatUser.setChatRoom(null);
    chatUser.setState(State.outside);
    //deleting the room if it becomes empty
    if(chatRoom.isEmpty()){
      chatRooms.remove(chatRoom);
    }
    else{
      status(chatRoom, key, "LEFT " + chatUser.getNick());
    }
    send(key, "OK");
  }

  static private void removeChatUser(SelectionKey key) throws IOException{
    //this function is to be used when the connection is lost
    //to remove the user from the "database" and inform other users
    ChatUser chatUser = (ChatUser) key.attachment();
    if(chatUser.getState() == State.inside){
      ChatRoom chatRoom = chatUser.getChatRoom();
      chatRoom.removeChatUser(key);
      //deleting the room if it becomes empty
      if(chatRoom.isEmpty()){
        chatRooms.remove(chatRoom);
      }
      else{
        status(chatRoom, key, "LEFT " + chatUser.getNick());
      }
    }
    chatUsers.remove(key);
  }
  static private void byeCommand(SelectionKey key) throws IOException {
    ChatUser charUser = (ChatUser) key.attachment();
    if(charUser.getState() == State.inside){
      leaveCommand(key);
    }
    send(key, "BYE");
    chatUsers.remove(key);
    SocketChannel sc = (SocketChannel) key.channel();
    Socket s = null;
    try {
      s = sc.socket();
      //System.out.println( "Closing connection to "+s );
      s.close();
    } catch(IOException e){
      //System.err.println( "Error closing socket "+s+": "+e );
    }
  }
  static private void send(SelectionKey key, String msg) throws IOException {
    msg = msg + "\n";
    SocketChannel socketCh = (SocketChannel) key.channel();
    socketCh.write(encoder.encode(CharBuffer.wrap(msg)));
  }
  
  static private void message(SelectionKey key, String msg) throws IOException {
    ChatUser chatUser = (ChatUser) key.attachment();
    if(chatUser.getState() != State.inside){
      send(key,"ERROR");
      return;
    }
    // if(msg.charAt(0) == '/'){
    //   msg = "/" + msg;
    // }
    msg = "MESSAGE " + chatUser.getNick() + " " + msg;
    msgSenttoRoom(chatUser.getChatRoom(), msg);
  }
  
  static private void msgSenttoRoom(ChatRoom chatRoom, String msg) throws IOException {
    for(SelectionKey chatUser : chatRoom.getChatUsers()){
      send(chatUser,msg);
    }
  }
  static private void status(ChatRoom chatRoom, SelectionKey key, String msg) throws IOException {
    for(SelectionKey chatUser : chatRoom.getChatUsers()){
      if (chatUser == key) {
        continue;
      }
      send(chatUser, msg);
    }
  }
}
