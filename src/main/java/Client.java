import logic.Config;

import java.io.IOException;
import java.net.*;

public class Client {
    public static void main(String[] args) {
        DatagramSocket socket = null;
        try {
            //Verfügbarkeit überprüfen
            socket = new DatagramSocket(Config.ClientPort);
            //case initialize
            byte[] msg = "checkAvailability 2002-03-03 2025-01-01".getBytes(); //Room autos fromdate todate
            DatagramPacket sendDP = new DatagramPacket(msg,0,msg.length,InetAddress.getByName("localhost"),Config.Coordinator1Port);
            socket.send(sendDP);

            byte[] puffer1 = new byte[65507];
            DatagramPacket receiveDP1 = new DatagramPacket(puffer1, puffer1.length);
            socket.receive(receiveDP1);

            String message = new String(receiveDP1.getData(),0,receiveDP1.getLength());

            System.out.println(message);

            byte[] puffer2 = new byte[65507];
            DatagramPacket receiveDP2 = new DatagramPacket(puffer2, puffer2.length);
            socket.receive(receiveDP2);

            message = new String(receiveDP2.getData(),0,receiveDP2.getLength());
            System.out.println(message);

            //das fürs buchen

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
