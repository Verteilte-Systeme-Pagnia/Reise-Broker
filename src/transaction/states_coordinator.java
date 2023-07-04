package transaction;

public enum states_coordinator {
    INIT,
    WAIT,
    COMMIT,
    ABORT,
    SENDCLIENT,
    Finish
}
