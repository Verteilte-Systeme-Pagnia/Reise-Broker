// Package not detected, please report project structure on CodeTogether's GitHub Issues

import java.net.InetAddress;

public class Config {
    public final static int Coordinator1Port = 5000;
    public final static InetAddress Coordinator1Address;

    public final static int Coordinator2Port = 5001;
    public final static InetAddress Coordinator2Address;

    public final static int Participant1Port = 4998;
    public final static InetAddress Participant1Address;

    public final static int Participant2Port = 4999;
    public final static InetAddress Participant2Address;

    public final static int ClientPort = 5002;
    public final static InetAddress ClientAddress;

     static {
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
