package fehler.fehlerfall3.java.logic.transaction;

public enum states_participant {
    INIT,
    READY,
    COMMIT,
    ABORT,
    ACK, //von jonas hinzugefügt um zu bestätigen, dass der Participant das commit oder das abort erhalten hat
    
    Finish
}
