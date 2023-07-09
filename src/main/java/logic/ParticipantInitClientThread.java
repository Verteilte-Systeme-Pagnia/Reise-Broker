package logic;

import logic.transaction.DatabaseAutoverleih;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;

public class ParticipantInitClientThread extends Thread{
    
    private String type;
    private DatabaseHotel databaseHotel;
    private DatabaseAutoverleih databaseAutoverleih;  
    private DatagramPacket dp;
    private DatagramSocket socket;
      
     public ParticipantInitClientThread(String type, DatabaseAutoverleih databaseAutoverleih, DatagramPacket dp, DatagramSocket socket){
            this.type = type;
            this.databaseAutoverleih = databaseAutoverleih;
            this.dp = dp;
            this.socket = socket;
        }
        
     public ParticipantInitClientThread(String type, DatabaseHotel databaseHotel, DatagramPacket dp, DatagramSocket socket){
             this.type = type;
             this.databaseHotel = databaseHotel;
             this.dp = dp;
             this.socket = socket;
         }
             
     public void run(){
         String intData = null;
         String receivedDp = new String(dp.getData(),0,dp.getLength());
         if(this.type.equals("Hotel")){
             //Abfrage Hotel
             
         }else if(this.type.equals("Autoverleih")){
             //Abfrage Autoverleih
         }
        
         receivedDp = receivedDp + " " + type + " " + intData;

         try {
             socket.send(new DatagramPacket(receivedDp.getBytes(),receivedDp.length()));
         } catch (IOException e) {
             throw new RuntimeException(e);
         }

     }
}
