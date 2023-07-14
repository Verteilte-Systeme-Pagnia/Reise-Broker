package fehler.fehlerfall2.java;

import fehler.fehlerfall2.java.logic.Config;
import fehler.fehlerfall2.java.logic.CoordinatorRef;
import fehler.fehlerfall2.java.logic.ParticipantReceive;
import fehler.fehlerfall2.java.logic.ParticipantRef;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
public class Hotel {
    
    public static void main(String[] args) {
        try{
            ArrayList<CoordinatorRef> coordinatorRefs = new ArrayList<>();

            coordinatorRefs.add(new CoordinatorRef(InetAddress.getByName("localhost"), Config.Coordinator1Port));//referenz für broker1
            coordinatorRefs.add(new CoordinatorRef(InetAddress.getByName("localhost"), Config.Coordinator2Port));//referenz für broker2

            ArrayList<ParticipantRef> participantRefs = new ArrayList<>();

            participantRefs.add(new ParticipantRef(InetAddress.getByName("localhost"), Config.Participant1Port));//referenz für bekannten partizipanten

            ParticipantReceive participantHotel = new ParticipantReceive(coordinatorRefs,participantRefs,"LogFileParticipant2.txt");
            participantHotel.initialize(Config.Participant2Port, "Hotel");

        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }



    }





}
