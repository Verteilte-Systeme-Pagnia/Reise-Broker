import logic.Config;

import java.io.IOException;
import java.net.*;

public class Client {
    public static void main(String[] args) {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(Config.ClientPort);
            byte[] msg = "Hotelzimmer:1a VW-5678".getBytes();
            DatagramPacket sendDP = new DatagramPacket(msg,0,msg.length,InetAddress.getByName("localhost"),Config.Coordinator1Port);
            socket.send(sendDP);

            byte[] puffer = new byte[65507];
            DatagramPacket receiveDP = new DatagramPacket(puffer, puffer.length);
            socket.receive(receiveDP);
            
            String message = new String(receiveDP.getData(),0,receiveDP.getLength());

            System.out.println(message);
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
