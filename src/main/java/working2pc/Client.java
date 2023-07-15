package working2pc;

import working2pc.logic.Config;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Random;

public class Client {
    DatagramSocket socket;
    Random random = new Random();
    int randomInt;
    public Client(){
        try {
            socket = new DatagramSocket(Config.ClientPort);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }
    public String[] checkAvailability(String dateFrom, String dateTo){
        randomInt = random.nextInt(2);
        String hotelResponse;
        String carResponse;
        try{
            byte[] msg = ("checkAvailability " + dateFrom + " " + dateTo).getBytes();
            DatagramPacket sendDP;
            if(randomInt == 0){
                sendDP = new DatagramPacket(msg, 0, msg.length, InetAddress.getByName("localhost"), Config.Coordinator1Port);
            } else {
                sendDP = new DatagramPacket(msg, 0, msg.length, InetAddress.getByName("localhost"), Config.Coordinator2Port);
            }
            socket.send(sendDP);

            byte[] puffer1 = new byte[65507];
            DatagramPacket receiveDP1 = new DatagramPacket(puffer1, puffer1.length);
            socket.receive(receiveDP1);
            hotelResponse = new String(receiveDP1.getData(),0,receiveDP1.getLength());

            byte[] puffer2 = new byte[65507];
            DatagramPacket receiveDP2 = new DatagramPacket(puffer2, puffer2.length);
            socket.receive(receiveDP2);
            carResponse = new String(receiveDP2.getData(),0,receiveDP2.getLength());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new String[]{hotelResponse, carResponse};
    }

    public String book(String dateFrom, String dateTo, int nRooms, int nCars){
        randomInt = random.nextInt(2);
        String response;
        try {
            byte[] msg = (nRooms + " " + nCars + " " + dateFrom + " " + dateTo + " Booked").getBytes();
            DatagramPacket sendDP;
            if(randomInt == 0){
                sendDP = new DatagramPacket(msg, 0, msg.length, InetAddress.getByName("localhost"), Config.Coordinator1Port);
            } else {
                sendDP = new DatagramPacket(msg, 0, msg.length, InetAddress.getByName("localhost"), Config.Coordinator2Port);
            }
            socket.send(sendDP);

            byte[] puffer = new byte[65507];
            DatagramPacket receiveDP = new DatagramPacket(puffer, puffer.length);
            socket.receive(receiveDP);
            response = new String(receiveDP.getData(),0,receiveDP.getLength());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return response;
    }
}
