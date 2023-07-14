package fehler.fehlerfall3.java.logic;// Package not detected, please report project structure on CodeTogether's GitHub Issues

import java.net.InetAddress;

public abstract class Config {
    public final static int Coordinator1Port = 5000;//fehler.fehlerfall3.java.Broker1
    public  static InetAddress Coordinator1Address;

    public final static int Coordinator2Port = 5001;//fehler.fehlerfall3.java.Broker2
    public  static InetAddress Coordinator2Address;

    public final static int Participant1Port = 4998;//Autoverleih
    public  static InetAddress Participant1Address;

    public final static int Participant2Port = 4999;//Hotel
    public  static InetAddress Participant2Address;

    public final static int ClientPort = 5002;//fehler.fehlerfall3.java.Client
    public  static InetAddress ClientAddress;

     public static void main(String[] args){
         
            InetAddress localhost = null;
            try {
                localhost = InetAddress.getLocalHost();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Coordinator1Address = localhost;
            Coordinator2Address = localhost;
            Participant1Address = localhost;
            Participant2Address = localhost;
            ClientAddress = localhost;

     }

}
