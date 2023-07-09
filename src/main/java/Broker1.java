import logic.Config;
import logic.CoordinatorReceive;
import logic.ParticipantRef;

import java.net.*;
import java.util.*;

public class Broker1 {
    public static void main(String[] args) {
        try {
            // Beispielverwendung der Klasse TwoPhaseCommitProtocol
            ArrayList<ParticipantRef> participants = new ArrayList<ParticipantRef>();
            ParticipantRef autoParticipant = new ParticipantRef(InetAddress.getByName("localhost"), Config.Participant1Port);
            ParticipantRef hotelParticipant = new ParticipantRef(InetAddress.getByName("localhost"),Config.Participant2Port);
            participants.add(autoParticipant);
            participants.add(hotelParticipant);

            CoordinatorReceive broker1 = new CoordinatorReceive(participants,"LogFileCoordinator1.txt");
            broker1.initialize(Config.Coordinator1Port);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

    }
}
