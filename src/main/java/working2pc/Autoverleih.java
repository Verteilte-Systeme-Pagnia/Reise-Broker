package working2pc;

import working2pc.logic.Config;
import working2pc.logic.CoordinatorRef;
import working2pc.logic.ParticipantReceive;
import working2pc.logic.ParticipantRef;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class Autoverleih {
    
    public static void main(String[] args) {
        try{
            ArrayList<CoordinatorRef> coordinatorRefs = new ArrayList<>();

            coordinatorRefs.add(new CoordinatorRef(InetAddress.getByName("localhost"), Config.Coordinator1Port));//referenz für broker1
            coordinatorRefs.add(new CoordinatorRef(InetAddress.getByName("localhost"), Config.Coordinator2Port));//referenz für broker2

            ArrayList<ParticipantRef> participantRefs = new ArrayList<>();

            participantRefs.add(new ParticipantRef(InetAddress.getByName("localhost"), Config.Participant2Port));//referenz für Hotel

            ParticipantReceive participantHotel = new ParticipantReceive(coordinatorRefs,participantRefs,"LogFileParticipant1.txt");
            participantHotel.initialize(Config.Participant1Port, "Autoverleih");

        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

    }


}
