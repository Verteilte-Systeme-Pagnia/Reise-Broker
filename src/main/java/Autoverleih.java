import logic.Config;
import logic.CoordinatorRef;
import logic.ParticipantReceive;
import logic.ParticipantRef;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.ArrayList;

public class Autoverleih {


    public static void main(String[] args) {
        try{
            ArrayList<CoordinatorRef> coordinatorRefs = new ArrayList<>();

            coordinatorRefs.add(new CoordinatorRef(InetAddress.getByName("localhost"), Config.Coordinator1Port));
            coordinatorRefs.add(new CoordinatorRef(InetAddress.getByName("localhost"),Config.Coordinator2Port));

            ArrayList<ParticipantRef> participantRefs = new ArrayList<>();

            participantRefs.add(new ParticipantRef(InetAddress.getByName("localhost"),Config.Participant1Port));

            ParticipantReceive participantHotel = new ParticipantReceive(coordinatorRefs,participantRefs,"LogFileParticipant1.txt");
            participantHotel.initialize(Config.Participant1Port, "Autoverleih");

        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

    }


}
