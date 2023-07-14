package fehler.fehlerfall3.java;

import fehler.fehlerfall3.java.logic.Config;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Client {
    DatagramSocket socket;
    public Client(){
        try {
            socket = new DatagramSocket(Config.ClientPort);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }
    public String[] checkAvailability(String dateFrom, String dateTo){
        String hotelResponse;
        String carResponse;
        try{
            byte[] msg = ("checkAvailability " + dateFrom + " " + dateTo).getBytes();
            DatagramPacket sendDP = new DatagramPacket(msg,0,msg.length,InetAddress.getByName("localhost"), Config.Coordinator1Port);
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
        String response;
        try {
            byte[] msg = (nRooms + " " + nCars + " " + dateFrom + " " + dateTo + " Booked").getBytes();
            DatagramPacket sendDP = new DatagramPacket(msg,0,msg.length,InetAddress.getByName("localhost"), Config.Coordinator1Port);
            socket.send(sendDP);

            byte[] puffer = new byte[65507];
            DatagramPacket receiveDP = new DatagramPacket(puffer, puffer.length);
            socket.receive(receiveDP);
            String result = new String(receiveDP.getData(),0,receiveDP.getLength());
            //Booking-Error => Buchung fehlgeschlagen
            //Successfully-Booked => Buchung erfolgreich

            response = new String(receiveDP.getData(),0,receiveDP.getLength());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return response;
    }
}
