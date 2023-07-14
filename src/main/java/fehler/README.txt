Dokumentation der Fehlerfälle

fehlerfall1: Eine andauernde Netzpartitionierung während der ersten Phase, die dazu führt, dass ein oder mehrere
             Teilnehmer des Protokollablaufs nicht mehr mit dem Koordinator kommunizieren können, wird dazu führen,
             dass der Koordinator ABORT entscheidet.
             Fällt ein Teilnehmer in der ersten Phase aus, so antwortet er nicht. Der Koordinator wertet dies als ABORT
             und entscheidet ABORT.

             Änderung: fehlerfall1.logic.participantThread line: 82-86
             Der Hotel Partizipant bleibt an der while(true) Schleife hängen und sendet nie eine Nachricht an den
             Koordinator. Dieser wartet bis zum timeout und wechselt dann ins ABORT und sendet ein GLOBAL_ABORT.
             In unserer Implementierung wartet er auf ein ACK von allen Partizipanten und sendet nach timeouts
             immer wieder das GLOBAL_ABORT. Das ist so implementiert, da der Hotel Partizipant bei einem Wiederaufwachen
             das Protokoll korrekt zu Ende führen könnte.

fehlerfall2: Fällt ein Teilnehmer in der zweiten Phase aus, so bekommt er die Entscheidung des Koordinators nicht mit.
             Der Koordinator hat die Entscheidung im persistenten Log-File (stable storage) festgehalten.
             Der Teilnehmer hat in seinem persistenten Log-File notiert, dass die Transaktion begonnen, aber noch
             nicht abgeschlossen wurde. Nach dem Booten bekommt der Teilnehmer vom Koordinator den Ausgang der
             Transaktion nachdem die verlorengegangene Nachricht mittels Timeouts erkannt wurde und nochmals gesendet wurde.

fehlerfall3: Fällt der Koordinator aus, nachdem er die Entscheidung getroffen und diese im Log-File notiert hat, oder
             kommt es zu diesem Zeitpunkt zu einer Netzpartitionierung, so kann das Protokoll erst nach dem Reboot
             des Koordinators fortgesetzt werden. Das Protokoll ist solange blockiert.
             Kennt einer der Teilnehmer die Entscheidung des Koordinators bereits, kann er diese auf Nachfrage an die
             anderen Teilnehmer weiterleiten.