package fehler.fehlerfall2.java;

import javax.swing.*;
import java.awt.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;

public class UIController {
    private Client client;
    private final Object[] shoppingCart = new Object[4];
    private JFrame frame;
    DateSection dateSection;
    AmountSection amountSection;
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

    private boolean validateDate(String date){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setLenient(false);
        try {
            dateFormat.parse(date);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

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

    private int parseAvailabilityResponse(String response){
        String[] responseSplit = response.split(" ");
        return Integer.parseInt(responseSplit[responseSplit.length - 1]);
    }

    private void book() {
        client.book((String)shoppingCart[0], (String)shoppingCart[1], (int)shoppingCart[2], (int)shoppingCart[3]);

        JOptionPane.showMessageDialog(frame, "Herzlichen Dank für ihre Buchung bei den Pagnia Piranhas.");
        buttonSection.getBookButton().setEnabled(false);
    }

    public void show() {
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        UIController ui = new UIController();
        ui.show();
    }
}
