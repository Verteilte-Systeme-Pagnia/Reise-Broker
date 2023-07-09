import logic.Config;
import logic.CoordinatorRef;
import logic.ParticipantReceive;
import logic.ParticipantRef;

import java.net.*;
import java.sql.*;
import java.util.*;
public class Hotel {

    public static void main(String[] args) {
       /* try{
            ArrayList<CoordinatorRef> coordinatorRefs = new ArrayList<>();

            coordinatorRefs.add(new CoordinatorRef(InetAddress.getByName("localhost"), Config.Coordinator1Port));
            coordinatorRefs.add(new CoordinatorRef(InetAddress.getByName("localhost"),Config.Coordinator2Port));

            ArrayList<ParticipantRef> participantRefs = new ArrayList<>();

            participantRefs.add(new ParticipantRef(InetAddress.getByName("localhost"),Config.Participant2Port));

            ParticipantReceive participantHotel = new ParticipantReceive(coordinatorRefs,participantRefs,"LogFileParticipant2.txt");
            participantHotel.initialize(Config.Participant2Port, "Hotel");

        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }*/


        String url = "jdbc:mysql://localhost:3306/hotel";
        String user = "root";
        String password = "pass";
        try {

            Connection connection = DriverManager.getConnection(url, user, password);
            System.out.println("Connection is Successful to the database" + url);

            int freeRooms = getFreeRooms(connection);
            boolean reserve = reserveRoom(connection,1);
            System.out.println(freeRooms);
            System.out.println(reserve);
            boolean cancel = cancelReservation(connection);
            System.out.println(cancel);

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }


    }
    public static int getFreeRooms(Connection connection) throws SQLException {
        String sql = "SELECT COUNT(*) FROM hotel WHERE status = 'frei'";
        PreparedStatement ps = connection.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getInt("COUNT(*)");

    }

    public static int getReservedRooms(Connection connection) throws SQLException {
        String sql = "SELECT COUNT(*) FROM hotel WHERE status = 'reserviert'";
        PreparedStatement ps = connection.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getInt("COUNT(*)");
    }

    public static boolean reserveRoom(Connection connection, int number) throws SQLException {
        String sql = "UPDATE hotel SET status = 'reserviert' WHERE status = 'frei' LIMIT " + number ;
        PreparedStatement ps = connection.prepareStatement(sql);
        int affectedRows = ps.executeUpdate();
        return affectedRows == number;
    }

    public static boolean cancelReservation(Connection connection) throws SQLException {
        String sql = "UPDATE hotel SET status = 'frei' WHERE status = 'reserviert'";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.executeUpdate();

        return getReservedRooms(connection) == 0;
    }


}
