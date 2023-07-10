package logic;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

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
         System.out.println("qiwondqwiodnqwidnqoiwdniwqdnoqiwdnoqwdnoqwindqiwndqwd");
         int intData = -1;
         String receivedDp = new String(dp.getData(),0,dp.getLength());
         String[] splitReceiveDp = receivedDp.split(" ");
         System.out.println("vor getfree rooms");
         if(this.type.equals("Hotel")){
             intData = databaseHotel.getFreeRooms(splitReceiveDp[1],splitReceiveDp[2]);
             
         }else if(this.type.equals("Autoverleih")){
             intData = databaseAutoverleih.getFreeCars(splitReceiveDp[1],splitReceiveDp[2]);
         }
         System.out.println("receivedb wird gebaut");
         receivedDp = receivedDp + " " + type + " " + intData;

         try {
             System.out.println("vor socket send");
             socket.send(new DatagramPacket(receivedDp.getBytes(),receivedDp.length(),dp.getAddress(),dp.getPort()));
             System.out.println("port" + dp.getPort());
         } catch (IOException e) {
             throw new RuntimeException(e);
         }

     }
}
