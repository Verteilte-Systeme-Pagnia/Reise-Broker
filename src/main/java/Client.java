import logic.Config;

import java.io.IOException;
import java.net.*;

public class Client {
    public static void main(String[] args) {
        DatagramSocket socket = null;
        try {
            //Initialisierung sendet message erhält 2 receives mit hotel oder auto
            socket = new DatagramSocket(Config.ClientPort);
            //case initialize
            byte[] msg = "InitializeClient".getBytes(); //Room autos fromdate todate
            DatagramPacket sendDP = new DatagramPacket(msg,0,msg.length,InetAddress.getByName("localhost"),Config.Coordinator1Port);
            socket.send(sendDP);

            byte[] puffer = new byte[65507];
            DatagramPacket receiveDP = new DatagramPacket(puffer, puffer.length);
            socket.receive(receiveDP); // hotel oder auto

            String message = new String(receiveDP.getData(),0,receiveDP.getLength());

            System.out.println(message);

            socket.receive(receiveDP); //Hotel oder auto

             message = new String(receiveDP.getData(),0,receiveDP.getLength());

            System.out.println(message);

            /**case button buchen is pressed
            byte[] msg = "5 4 2002-03-03 2025-01-01 Booked".getBytes(); //Room autos fromdate todate
            DatagramPacket sendDP = new DatagramPacket(msg,0,msg.length,InetAddress.getByName("localhost"),Config.Coordinator1Port);
            socket.send(sendDP);

            byte[] puffer = new byte[65507];
            DatagramPacket receiveDP = new DatagramPacket(puffer, puffer.length);
            socket.receive(receiveDP);
            
            String message = new String(receiveDP.getData(),0,receiveDP.getLength());

            System.out.println(message);
            */
        }catch (SocketException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if(socket != null){
                socket.close();
            }
        }
    }
}
