package working2pc;

import working2pc.logic.Config;
import working2pc.logic.CoordinatorReceive;
import working2pc.logic.ParticipantRef;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class Broker1 {
    public static void main(String[] args) {
        try {
            // Beispielverwendung der Klasse TwoPhaseCommitProtocol
            ArrayList<ParticipantRef> participants = new ArrayList<ParticipantRef>();
            ParticipantRef autoParticipant = new ParticipantRef(InetAddress.getByName("localhost"), Config.Participant1Port);//Referenz für Autoverleih
            ParticipantRef hotelParticipant = new ParticipantRef(InetAddress.getByName("localhost"), Config.Participant2Port);//referenz für Hotel
            participants.add(autoParticipant);
            participants.add(hotelParticipant);

            CoordinatorReceive broker1 = new CoordinatorReceive(participants,"LogFileCoordinator1.txt");
            broker1.initialize(Config.Coordinator1Port);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

    }
}
