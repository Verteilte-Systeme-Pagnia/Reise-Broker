import java.net.*;
import java.util.*;

public class Broker2 {
    public static void main(String[] args) {
        try {
            // Beispielverwendung der Klasse TwoPhaseCommitProtocol
            ArrayList<ParticipantRef> participants = new ArrayList<ParticipantRef>();
            ParticipantRef autoParticipant = new ParticipantRef(InetAddress.getByName("localhost"), Config.Participant1Port);
            ParticipantRef hotelParticipant = new ParticipantRef(InetAddress.getByName("localhost"),Config.Participant2Port);
            participants.add(autoParticipant);
            participants.add(hotelParticipant);

            CoordinatorReceive broker2 = new CoordinatorReceive(participants,"LogFileCoordinator2.txt");
            broker2.initialize(Config.Coordinator2Port);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

    }
}