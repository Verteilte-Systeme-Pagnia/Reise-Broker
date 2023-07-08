import java.net.*;
import java.util.*;
public class Hotel {

    public static void main(String[] args) {
        try{
            ArrayList<CoordinatorRef> coordinatorRefs = new ArrayList<>();

            coordinatorRefs.add(new CoordinatorRef(InetAddress.getByName("localhost"),Config.Coordinator1Port));
            coordinatorRefs.add(new CoordinatorRef(InetAddress.getByName("localhost"),Config.Coordinator2Port));

            ArrayList<ParticipantRef> participantRefs = new ArrayList<>();

            participantRefs.add(new ParticipantRef(InetAddress.getByName("localhost"),Config.Participant2Port));

            ParticipantReceive participantHotel = new ParticipantReceive(coordinatorRefs,participantRefs,"LogFileParticipant2.txt");
            participantHotel.initialize(Config.Participant2Port, "Hotel");

        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
