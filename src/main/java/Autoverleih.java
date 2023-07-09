import logic.Config;
import logic.CoordinatorRef;
import logic.ParticipantReceive;
import logic.ParticipantRef;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.ArrayList;

public class Autoverleih {

    private Connection connection;
    public Autoverleih() {
        try {
            this.connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/autoverleih", "root", "pass");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static void main(String[] args) {
        /*try{
            ArrayList<CoordinatorRef> coordinatorRefs = new ArrayList<>();

            coordinatorRefs.add(new CoordinatorRef(InetAddress.getByName("localhost"), Config.Coordinator1Port));
            coordinatorRefs.add(new CoordinatorRef(InetAddress.getByName("localhost"),Config.Coordinator2Port));

            ArrayList<ParticipantRef> participantRefs = new ArrayList<>();

            participantRefs.add(new ParticipantRef(InetAddress.getByName("localhost"),Config.Participant1Port));

            ParticipantReceive participantHotel = new ParticipantReceive(coordinatorRefs,participantRefs,"LogFileParticipant1.txt");
            participantHotel.initialize(Config.Participant1Port, "Autoverleih");

        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }*/

        Autoverleih autoverleih = new Autoverleih();

        String startDate = "2023-07-10";
        String endDate = "2023-07-20";
        try{
            System.out.println(autoverleih.getFreeCars(startDate,endDate));
            //System.out.println(hotel.reserveRoom(3,startDate,endDate));
            //System.out.println(hotel.bookReservedRooms(startDate,endDate));
            //System.out.println(hotel.checkOutdatedBookings());
        }catch(SQLException throwables){
            throwables.printStackTrace();
        }
    }

    public int getFreeCars(String startDate, String endDate) throws SQLException {
        this.checkOutdatedBookings();
        String sql = "SELECT COUNT(*) FROM autos WHERE " + "reserved = 0 AND" +
                " id NOT IN (SELECT carId FROM buchungen WHERE (startDatum >= " + "'" +  startDate + "'" + " AND startDatum <=  " + "'" + endDate + "'" + ") OR (" +
                " endDatum >= " + "'" + startDate + "'" + " AND endDatum <= " + "'" + endDate + "'" + ") )";

        PreparedStatement ps = this.connection.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getInt("COUNT(*)");
    }

    public int getReservedCars() throws SQLException {

        String sql = "SELECT COUNT(*) FROM autos WHERE reserved = 1";
        PreparedStatement ps = this.connection.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getInt("COUNT(*)");
    }

    public  boolean reserveCar( int number, String startDate, String endDate) throws SQLException {
        String sql = "UPDATE autos SET reserved = 1 WHERE reserved = 0 AND id NOT IN " +
                "(SELECT carId FROM buchungen WHERE (startDatum >= " + "'" +  startDate + "'" + " AND startDatum <=  " + "'" + endDate + "'" + ") OR (" +
                " endDatum >= " + "'" + startDate + "'" + " AND endDatum <= " + "'" + endDate + "'" + ") ) LIMIT " + number;
        PreparedStatement ps = this.connection.prepareStatement(sql);
        int affectedRows = ps.executeUpdate();
        /*wenn die Anzahl der zu reservierenden Autos der Anzahl an geupdateten Zeilen entspricht, war die Datenbankoperation erfolgreich*/
        return affectedRows == number;
    }

    public  boolean cancelReservation() throws SQLException {
        String sql = "UPDATE autos SET reserved = 0 WHERE reserved = 1";
        PreparedStatement ps = this.connection.prepareStatement(sql);
        ps.executeUpdate();
        /*wenn es keine reservierten Autos mehr gibt war die Datenbankoperation erfolgreich*/
        return this.getReservedCars() == 0;
    }

    public boolean bookReservedCars(String startDate, String endDate) throws  SQLException{
        int reservedRooms = getReservedCars();
        int insertCount = 0;
        for(int i = 0; i<reservedRooms;i++) {
            String sql = "INSERT INTO buchungen (carId,startDatum,endDatum) " +
                    "VALUES((SELECT id FROM autos WHERE reserved = 1 LIMIT 1)," + "'" + startDate + "', '" + endDate + "' )";
            System.out.printf(sql);
            PreparedStatement ps = this.connection.prepareStatement(sql);
            insertCount += ps.executeUpdate();
            sql = "UPDATE autos SET reserved = 0 WHERE reserved = 1 LIMIT 1";
            ps = this.connection.prepareStatement(sql);
            ps.executeUpdate();
        }
        /*wenn es keine reservierten Autos mehr gibt und genauso viele Buchungen,wie ursprünglich reservierte Autos erstellt wurden,
        dann war die Datenbankoperation erfolgreich*/
        return getReservedCars() == 0 && reservedRooms == insertCount;
    }

    public boolean checkOutdatedBookings() throws  SQLException{
        String sql = "DELETE FROM buchungen WHERE endDatum <  CURDATE()";
        PreparedStatement ps = this.connection.prepareStatement(sql);
        ps.executeUpdate();
        sql = "SELECT COUNT(*) FROM buchungen WHERE endDatum <  CURDATE()";
        ps = this.connection.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        rs.next();
        /* wenn es keine Buchungen in der Vergangenheit gibt, war die Datenbankoperation erfolgreich*/
        return rs.getInt("COUNT(*)") == 0;
    }

}
