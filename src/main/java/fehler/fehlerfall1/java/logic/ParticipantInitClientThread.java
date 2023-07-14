package fehler.fehlerfall1.java.logic;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class ParticipantInitClientThread extends Thread{//participant intit client thread wird vom vom particiapnt receive erzeugt, bei verfügbarkeitsanfragen von einem client zu einem koordinator zu einem partizipanten
    
    private String type;//der type des participant wird mitgegeben, sodass dieser weiß auf welches objekt er zugreifen muss
    private DatabaseHotel databaseHotel; //databashotel für synchronen Zugriff auf Datenbank
    private DatabaseAutoverleih databaseAutoverleih;  //databasehotel für synchronen zugriff auf datenbank
    private DatagramPacket dp;
    private DatagramSocket socket;

    //2 Konstruktoren, je nachdem was erzeugt wird
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
         System.out.println("PartizipantInitClientThread wurde gestartet");
         int intData = -1;
         String receivedDp = new String(dp.getData(),0,dp.getLength());
         String[] splitReceiveDp = receivedDp.split(" ");
         if(this.type.equals("Hotel")){//zugriff auf datenbank des entsprechenden types in diesem fall hotel
             intData = databaseHotel.getFreeRooms(splitReceiveDp[1],splitReceiveDp[2]);
             
         }else if(this.type.equals("Autoverleih")){//zugriff auf datenbank des entsprechenden types in diesem fall autoverleih
             intData = databaseAutoverleih.getFreeCars(splitReceiveDp[1],splitReceiveDp[2]);
         }
         receivedDp = receivedDp + " " + type + " " + intData;
         try {
             socket.send(new DatagramPacket(receivedDp.getBytes(),receivedDp.length(),dp.getAddress(),dp.getPort()));
         } catch (IOException e) {
             throw new RuntimeException(e);
         }

     }
}
