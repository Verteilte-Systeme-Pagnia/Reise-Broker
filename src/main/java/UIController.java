import javax.swing.*;
import java.awt.*;

public class UIController {
    private JFrame frame;
    private JTextField hotelTextField;
    private JTextField carTextField;
    private JButton reserveButton;
    private JButton bookButton;

    private JLabel availableHotelLabel;
    private JLabel availableCarLabel;

    private int availableHotelRooms;
    private int availableCars;

    public UIController() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame();
        frame.setTitle("Travel Broker");
        frame.setBounds(100, 100, 400, 250);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new GridLayout(4, 2, 10, 10));

        JLabel hotelLabel = new JLabel("Hotelzimmer:");
        frame.getContentPane().add(hotelLabel);

        hotelTextField = new JTextField();
        frame.getContentPane().add(hotelTextField);
        hotelTextField.setColumns(10);

        JLabel carLabel = new JLabel("Mietwagen:");
        frame.getContentPane().add(carLabel);

        carTextField = new JTextField();
        frame.getContentPane().add(carTextField);
        carTextField.setColumns(10);

        availableHotelLabel = new JLabel();
        frame.getContentPane().add(availableHotelLabel);

        availableCarLabel = new JLabel();
        frame.getContentPane().add(availableCarLabel);

        updateAvailability();

        reserveButton = new JButton("Reservieren");
        reserveButton.addActionListener(e -> checkAvailability(Integer.valueOf(hotelTextField.getText()), Integer.valueOf(carTextField.getText())));
        frame.getContentPane().add(reserveButton);

        bookButton = new JButton("Buchen");
        bookButton.setEnabled(false); // Deaktiviert, bis Verfügbarkeit überprüft wurde
        bookButton.addActionListener(e -> book(Integer.valueOf(hotelTextField.getText()), Integer.valueOf(carTextField.getText())));
        frame.getContentPane().add(bookButton);
    }

    private void updateAvailability(){
        // Prüfe Verfügbarkeit der Hotelzimmer und Mietwagen
        availableHotelRooms = checkHotelAvailability();
        availableCars = checkCarAvailability();

        availableHotelLabel.setText("Verfügbare Hotelzimmer: " + availableHotelRooms);
        availableCarLabel.setText("Verfügbare Mietwagen: " + availableCars);
    }

    private void checkAvailability(int nRooms, int nCars) {
        if (availableHotelRooms - nRooms >= 0 && availableCars - nCars >= 0) {
            JOptionPane.showMessageDialog(frame, "Verfügbarkeit gegeben. Sie können buchen.");
            bookButton.setEnabled(true);
        } else {
            JOptionPane.showMessageDialog(frame, "Verfügbarkeit nicht gegeben. Bitte überprüfen Sie Ihre Auswahl.");
            bookButton.setEnabled(false);
        }
    }

    private int checkHotelAvailability() {
        // Gib die Anzahl der verfügbaren Hotelzimmer zurück
        // Implementiere hier den entsprechenden Code, um die Verfügbarkeit des Hotels zu überprüfen
        // Hier könnte eine Anfrage an den Hotelzimmer-Anbieter gesendet werden
        return 5; // Beispiel-Rückgabewert
    }

    private int checkCarAvailability() {
        // Gib die Anzahl der verfügbaren Mietwagen zurück
        // Implementiere hier den entsprechenden Code, um die Verfügbarkeit des Mietwagens zu überprüfen
        // Hier könnte eine Anfrage an den Mietwagen-Anbieter gesendet werden
        return 3; // Beispiel-Rückgabewert
    }

    private void book(int nRooms, int nCars) {
        // Führe die verbindliche Buchung für Hotelzimmer und Mietwagen durch
        // Implementiere hier den entsprechenden Code, um die Buchung durchzuführen
        // Hier könnten Anfragen an die entsprechenden Anbieter gesendet werden

        JOptionPane.showMessageDialog(frame, "Buchung abgeschlossen. Vielen Dank!");
    }

    public void show() {
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        UIController ui = new UIController();
        ui.show();
    }
}
