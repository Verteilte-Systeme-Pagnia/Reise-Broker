package working2pc;

import javax.swing.*;
import java.awt.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;

public class UIController {
    private Client client;
    private final Object[] shoppingCart = new Object[4];
    private JFrame frame;
    //Teilbereich des UIs für die Eingabe der Daten
    DateSection dateSection;
    //Teilbereich des UIs für die Eingabe der Anzahl
    AmountSection amountSection;
    //Teilbereich des UIs für die Knöpfe
    ButtonSection buttonSection;

    private static class DateSection extends JPanel {
        JTextField dateFromTextField;
        JTextField dateToTextField;
        public DateSection(){
            setLayout(new GridLayout(0,4,10,10));
            setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createEtchedBorder(),
                    "Bitte geben sie einen Zeitraum an. (Format: YYYY-MM-DD)"));

            JLabel dateFromLabel = new JLabel("Von:");
            add(dateFromLabel);

            dateFromTextField = new JTextField();
            dateToTextField = new JTextField();

            add(dateFromTextField);
            dateFromTextField.setColumns(10);

            JLabel dateToLabel = new JLabel("Bis:");
            add(dateToLabel);

            add(dateToTextField);
            dateToTextField.setColumns(10);
        }

        public String getDateFrom(){
            return dateFromTextField.getText();
        }

        public String getDateTo(){
            return dateToTextField.getText();
        }
    }

    private static class AmountSection extends JPanel {
        private JTextField hotelTextField;
        private JTextField carTextField;
        public AmountSection(){
            setLayout(new GridLayout(0,4,10,10));
            setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createEtchedBorder(),
                    "Bitte geben sie die gewünschte Anzahl an Hotelzimmern und Mietwagen an."));

            JLabel hotelLabel = new JLabel("Hotelzimmer:");
            add(hotelLabel);
            hotelTextField = new JTextField();
            add(hotelTextField);
            hotelTextField.setColumns(10);

            JLabel carLabel = new JLabel("Mietwagen:");
            add(carLabel);
            carTextField = new JTextField();
            add(carTextField);
            carTextField.setColumns(10);
        }

        public String getHotelAmount(){
            return hotelTextField.getText();
        }
        public String getCarAmount(){
            return carTextField.getText();
        }
    }

    private class ButtonSection extends JPanel {
        JButton reserveButton;
        JButton showShoppingCartButton;
        JButton bookButton;
        public ButtonSection(){
            setLayout(new GridLayout(0, 3));

            reserveButton = new JButton("Verfügbarkeit prüfen ");
            reserveButton.addActionListener(e -> {
                String dateFrom = dateSection.getDateFrom();
                String dateTo = dateSection.getDateTo();
                if(!validateDates(dateFrom, dateTo)){
                    JOptionPane.showMessageDialog(frame, "Bitte geben Sie ein gültiges Datum ein.");
                    return;
                }
                int nHotels;
                int nCars;
                try {
                    nHotels = Integer.parseInt(amountSection.getHotelAmount());
                    nCars = Integer.parseInt(amountSection.getCarAmount());
                    if(nHotels < 1 || nCars < 1){
                        JOptionPane.showMessageDialog(frame, "Bitte geben sie eine Anzahl über 0 an.");
                    }
                } catch (NumberFormatException nfe){
                    JOptionPane.showMessageDialog(frame, "Bitte geben sie eine Zahl an.");
                    return;
                }
                reserve(dateFrom, dateTo, nHotels, nCars);
            });
            add(reserveButton);

            showShoppingCartButton = new JButton("Warenkorb anzeigen");
            showShoppingCartButton.addActionListener(e -> {
                String shoppingCartString = "Datum: " + shoppingCart[0] + " - " + shoppingCart[1] + "\n" +
                        "Hotelzimmer: " + shoppingCart[2] + "\n" +
                        "Mietwagen: " + shoppingCart[3];
                JOptionPane.showMessageDialog(frame, shoppingCartString);
            });
            add(showShoppingCartButton);

            bookButton = new JButton("Buchen");
            bookButton.setEnabled(false); // Deaktiviert, bis Verfügbarkeit überprüft wurde
            bookButton.addActionListener(e -> book());
            add(bookButton);
        }

        public JButton getBookButton(){
            return bookButton;
        }
    }

    public UIController() {
        initialize();
    }

    //Initialisieren der einzelnen Komponenten des UIs
    private void initialize() {
        client = new Client();
        frame = new JFrame();
        frame.setTitle("Travel Broker");
        frame.setBounds(100, 100, 500, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container contentPane = frame.getContentPane();
        contentPane.setLayout(new GridLayout(0, 1, 30, 30));

        dateSection = new DateSection();
        contentPane.add(dateSection);

        amountSection = new AmountSection();
        contentPane.add(amountSection);

        buttonSection = new ButtonSection();
        contentPane.add(buttonSection);
    }


    //Validieren der eingegebenen Daten
    private boolean validateDates(String dateFrom, String dateTo){
        if(!validateDate(dateFrom) || !validateDate(dateTo)){
            return false;
        }
        LocalDate from = LocalDate.parse(dateFrom);
        LocalDate to = LocalDate.parse(dateTo);
        if(!from.isBefore(to.plusDays(1))){
            return false;
        }
        return true;
    }

    //Validieren eines Datums, wird von der validateDates genutzt
    private boolean validateDate(String date){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setLenient(false);
        try {
            dateFormat.parse(date);
        } catch (ParseException e) {
            return false;
        }
        LocalDate currentDate = LocalDate.now();
        LocalDate inputDate = LocalDate.parse(date);
        if(inputDate.isBefore(currentDate)){
            return false;
        }
        return true;
    }

    //Reservieren der gewünschten Zimmer und Autos. Reservierung wird nicht auf der Datenbank persistiert,
    //sondern nur im Client gespeichert
    private void reserve(String dateFrom, String dateTo, int nRooms, int nCars) {
        String[] databaseResponse = client.checkAvailability(dateFrom, dateTo);
        int availableHotelRooms = parseAvailabilityResponse(databaseResponse[0]);
        int availableCars = parseAvailabilityResponse(databaseResponse[1]);

        if(availableHotelRooms < nRooms || availableCars < nCars){
            JOptionPane.showMessageDialog(frame, "Verfügbarkeit nicht gegeben. Bitte überprüfen Sie Ihre Auswahl.");
            buttonSection.getBookButton().setEnabled(false);
        } else {
            shoppingCart[0] = dateFrom;
            shoppingCart[1] = dateTo;
            shoppingCart[2] = nRooms;
            shoppingCart[3] = nCars;
            JOptionPane.showMessageDialog(frame, "Verfügbarkeit gegeben. Die gewünschten Zimmer und Autos wurden dem Warenkorb hinzugefügt und können gebucht werden");
            buttonSection.getBookButton().setEnabled(true);
        }
    }

    //Verarbeiten der Response aus dem Client
    private int parseAvailabilityResponse(String response){
        String[] responseSplit = response.split(" ");
        return Integer.parseInt(responseSplit[responseSplit.length - 1]);
    }

    //Buchen der reservierten Zimmer und Autos. Buchungsdaten werden aus dem lokalen shoppingCart geladen und
    //Buchung wird auf der Datenbank persistiert
    private void book() {
        String response = client.book(
                (String)shoppingCart[0],
                (String)shoppingCart[1],
                (int)shoppingCart[2],
                (int)shoppingCart[3]
        );

        if(response.equals("Booking-Error")){
            JOptionPane.showMessageDialog(frame, "Buchung fehlgeschlagen. Die Pagnia Piranhas bitten um Entschuldigung.");
            buttonSection.getBookButton().setEnabled(false);
        } else if (response.equals("Successfully-Booked")){
            JOptionPane.showMessageDialog(frame, "Buchung erfolgreich. Die Pagnia Piranhas wünschen viel Spaß!");
            buttonSection.getBookButton().setEnabled(false);
        }
    }

    public void show() {
        frame.setVisible(true);
    }

    //Starten des UIControllers
    public static void main(String[] args) {
        UIController ui = new UIController();
        ui.show();
    }
}
