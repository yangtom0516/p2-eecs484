package project2;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.ArrayList;

/*
    The StudentFakebookOracle class is derived from the FakebookOracle class and implements
    the abstract query functions that investigate the database provided via the <connection>
    parameter of the constructor to discover specific information.
*/
public final class StudentFakebookOracle extends FakebookOracle {
    // [Constructor]
    // REQUIRES: <connection> is a valid JDBC connection
    public StudentFakebookOracle(Connection connection) {
        oracle = connection;
    }

    @Override
    // Query 0
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the total number of users for which a birth month is listed
    //        (B) Find the birth month in which the most users were born
    //        (C) Find the birth month in which the fewest users (at least one) were born
    //        (D) Find the IDs, first names, and last names of users born in the month
    //            identified in (B)
    //        (E) Find the IDs, first names, and last name of users born in the month
    //            identified in (C)
    //
    // This query is provided to you completed for reference. Below you will find the appropriate
    // mechanisms for opening up a statement, executing a query, walking through results, extracting
    // data, and more things that you will need to do for the remaining nine queries
    public BirthMonthInfo findMonthOfBirthInfo() throws SQLException {
        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            // Step 1
            // ------------
            // * Find the total number of users with birth month info
            // * Find the month in which the most users were born
            // * Find the month in which the fewest (but at least 1) users were born
            ResultSet rst = stmt.executeQuery(
                    "SELECT COUNT(*) AS Birthed, Month_of_Birth " + // select birth months and number of uses with that birth month
                            "FROM " + UsersTable + " " + // from all users
                            "WHERE Month_of_Birth IS NOT NULL " + // for which a birth month is available
                            "GROUP BY Month_of_Birth " + // group into buckets by birth month
                            "ORDER BY Birthed DESC, Month_of_Birth ASC"); // sort by users born in that month, descending; break ties by birth month

            int mostMonth = 0;
            int leastMonth = 0;
            int total = 0;
            while (rst.next()) { // step through result rows/records one by one
                if (rst.isFirst()) { // if first record
                    mostMonth = rst.getInt(2); //   it is the month with the most
                }
                if (rst.isLast()) { // if last record
                    leastMonth = rst.getInt(2); //   it is the month with the least
                }
                total += rst.getInt(1); // get the first field's value as an integer
            }
            BirthMonthInfo info = new BirthMonthInfo(total, mostMonth, leastMonth);

            // Step 2
            // ------------
            // * Get the names of users born in the most popular birth month
            rst = stmt.executeQuery(
                    "SELECT User_ID, First_Name, Last_Name " + // select ID, first name, and last name
                            "FROM " + UsersTable + " " + // from all users
                            "WHERE Month_of_Birth = " + mostMonth + " " + // born in the most popular birth month
                            "ORDER BY User_ID"); // sort smaller IDs first

            while (rst.next()) {
                info.addMostPopularBirthMonthUser(new UserInfo(rst.getLong(1), rst.getString(2), rst.getString(3)));
            }

            // Step 3
            // ------------
            // * Get the names of users born in the least popular birth month
            rst = stmt.executeQuery(
                    "SELECT User_ID, First_Name, Last_Name " + // select ID, first name, and last name
                            "FROM " + UsersTable + " " + // from all users
                            "WHERE Month_of_Birth = " + leastMonth + " " + // born in the least popular birth month
                            "ORDER BY User_ID"); // sort smaller IDs first

            while (rst.next()) {
                info.addLeastPopularBirthMonthUser(new UserInfo(rst.getLong(1), rst.getString(2), rst.getString(3)));
            }

            // Step 4
            // ------------
            // * Close resources being used
            rst.close();
            stmt.close(); // if you close the statement first, the result set gets closed automatically

            return info;

        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return new BirthMonthInfo(-1, -1, -1);
        }
    }

    @Override
    // Query 1
    // -----------------------------------------------------------------------------------
    // GOALS: (A) The first name(s) with the most letters
    //        (B) The first name(s) with the fewest letters
    //        (C) The first name held by the most users
    //        (D) The number of users whose first name is that identified in (C)
    public FirstNameInfo findNameInfo() throws SQLException {
        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                FirstNameInfo info = new FirstNameInfo();
                info.addLongName("Aristophanes");
                info.addLongName("Michelangelo");
                info.addLongName("Peisistratos");
                info.addShortName("Bob");
                info.addShortName("Sue");
                info.addCommonName("Harold");
                info.addCommonName("Jessica");
                info.setCommonNameCount(42);
                return info;
            */
            FirstNameInfo info = new FirstNameInfo();

            ResultSet longest = stmt.executeQuery(
                "SELECT DISTINCT first_name " +
                "FROM " + UsersTable + " " +
                "WHERE LENGTH(first_Name) = (SELECT MAX(LENGTH(first_Name)) FROM " + UsersTable + ") " +
                "ORDER BY first_name ASC"
            );
            
            while (longest.next()) {
                info.addLongName(longest.getString(1));
            }
            ResultSet shortest = stmt.executeQuery(
                "SELECT DISTINCT first_name " +
                "FROM " + UsersTable + " " +
                "WHERE LENGTH(first_Name) = (SELECT MIN(LENGTH(first_Name)) FROM " + UsersTable + ") " +
                "ORDER BY first_name ASC"
            );
            while (shortest.next()) {
                info.addShortName(shortest.getString(1));
            }
            ResultSet common = stmt.executeQuery(
                "WITH max_count AS ( " +
                "SELECT COUNT(*) AS count " +
                "FROM " + UsersTable + " " +
                "GROUP BY first_name " +
                ") " +
                "SELECT DISTINCT first_name, COUNT(*) AS count " +
                "FROM " + UsersTable + " " +
                "GROUP BY first_name " +
                "HAVING COUNT(*) = (SELECT MAX(count) FROM max_count) " +
                "ORDER BY first_name ASC"
            );
            int commonCount = 0;
            while (common.next()) {
                info.addCommonName(common.getString(1));
                commonCount = common.getInt(2);
            }
            info.setCommonNameCount(commonCount);
            longest.close();
            shortest.close();
            common.close();
            stmt.close();
            return info;
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return new FirstNameInfo();
        }
    }

    @Override
    // Query 2
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the IDs, first names, and last names of users without any friends
    //
    // Be careful! Remember that if two users are friends, the Friends table only contains
    // the one entry (U1, U2) where U1 < U2.
    public FakebookArrayList<UserInfo> lonelyUsers() throws SQLException {
        FakebookArrayList<UserInfo> results = new FakebookArrayList<UserInfo>(", ");

        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                UserInfo u1 = new UserInfo(15, "Abraham", "Lincoln");
                UserInfo u2 = new UserInfo(39, "Margaret", "Thatcher");
                results.add(u1);
                results.add(u2);
            */
           String query = 
                "SELECT user_id, first_name, last_name " +
                "FROM " + UsersTable + " " +
                "WHERE user_id NOT IN (SELECT DISTINCT user1_id FROM " + FriendsTable + ") " +
                "AND user_id NOT IN (SELECT DISTINCT user2_id FROM " + FriendsTable + ") " +
                "ORDER BY user_id ASC";
            ResultSet rst = stmt.executeQuery(query);
            while (rst.next()) {
                long uid = rst.getLong(1);
                String fname = rst.getString(2);
                String lname = rst.getString(3);
                results.add(new UserInfo(uid, fname, lname));
            }
            rst.close();
            stmt.close();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        return results;
    }

    @Override
    // Query 3
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the IDs, first names, and last names of users who no longer live
    //            in their hometown (i.e. their current city and their hometown are different)
    public FakebookArrayList<UserInfo> liveAwayFromHome() throws SQLException {
        FakebookArrayList<UserInfo> results = new FakebookArrayList<UserInfo>(", ");

        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                UserInfo u1 = new UserInfo(9, "Meryl", "Streep");
                UserInfo u2 = new UserInfo(104, "Tom", "Hanks");
                results.add(u1);
                results.add(u2);
            */
           String query1 = 
                "SELECT U.user_id, U.first_name, U.last_name " +
                "FROM " + UsersTable + " U, " + CurrentCitiesTable + " C, " + HometownCitiesTable + " H " +
                "WHERE U.user_id = C.user_id " +
                "AND U.user_id = H.user_id " +
                "AND C.current_city_id <> H.hometown_city_id " +
                "ORDER BY user_id ASC";
            ResultSet rst = stmt.executeQuery(query1);
            while (rst.next()) {
                long uid = rst.getLong(1);
                String fname = rst.getString(2);
                String lname = rst.getString(3);
                results.add(new UserInfo(uid, fname, lname));
            }
            rst.close();
            stmt.close();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        return results;
    }

    @Override
    // Query 4
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the IDs, links, and IDs and names of the containing album of the top
    //            <num> photos with the most tagged users
    //        (B) For each photo identified in (A), find the IDs, first names, and last names
    //            of the users therein tagged
    public FakebookArrayList<TaggedPhotoInfo> findPhotosWithMostTags(int num) throws SQLException {
        FakebookArrayList<TaggedPhotoInfo> results = new FakebookArrayList<TaggedPhotoInfo>("\n");

        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly);
             Statement stmt2 = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                PhotoInfo p = new PhotoInfo(80, 5, "www.photolink.net", "Winterfell S1");
                UserInfo u1 = new UserInfo(3901, "Jon", "Snow");
                UserInfo u2 = new UserInfo(3902, "Arya", "Stark");
                UserInfo u3 = new UserInfo(3903, "Sansa", "Stark");
                TaggedPhotoInfo tp = new TaggedPhotoInfo(p);
                tp.addTaggedUser(u1);
                tp.addTaggedUser(u2);
                tp.addTaggedUser(u3);
                results.add(tp);
            */
            String query1 =
                "WITH tag_counts AS (" +
                "SELECT tag_photo_id, COUNT(*) AS num_tags " +
                "FROM " + TagsTable + " " +
                "GROUP BY tag_photo_id " + ") " +
                "SELECT p.photo_id, p.photo_link, p.album_id, a.album_name " +
                "FROM " + PhotosTable + " p, " + AlbumsTable + " a, tag_counts tc " +
                "WHERE p.album_id = a.album_id " +
                "AND p.photo_id = tc.tag_photo_id " +
                "ORDER BY tc.num_tags DESC, tc.tag_photo_id ASC " +
                "FETCH FIRST " + num + " ROWS ONLY";
            ResultSet rst = stmt.executeQuery(query1);
            while (rst.next()) {
                long pid = rst.getLong(1);
                String plink = rst.getString(2);
                Long aid = rst.getLong(3);
                String aname = rst.getString(4);
                PhotoInfo p = new PhotoInfo(pid, aid, plink, aname);
                TaggedPhotoInfo tp = new TaggedPhotoInfo(p);
                String query2 =
                    "SELECT u.user_id, u.first_name, u.last_name " +
                    "FROM " + UsersTable + " U, " + TagsTable + " T " +
                    "WHERE U.user_id = T.tag_subject_id " +
                    "AND T.tag_photo_id = " + pid + " " +
                    "ORDER BY u.user_id ASC";
                ResultSet rst2 = stmt2.executeQuery(query2);
                while (rst2.next()) {
                    long uid = rst2.getLong(1);
                    String fname = rst2.getString(2);
                    String lname = rst2.getString(3);
                    UserInfo u = new UserInfo(uid, fname, lname);
                    tp.addTaggedUser(u);
                }
                results.add(tp);
                rst2.close();
            }
            rst.close();
            stmt.close();
            stmt2.close();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return results;
    }

    @Override
    // Query 5
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the IDs, first names, last names, and birth years of each of the two
    //            users in the top <num> pairs of users that meet each of the following
    //            criteria:
    //              (i) same gender
    //              (ii) tagged in at least one common photo
    //              (iii) difference in birth years is no more than <yearDiff>
    //              (iv) not friends
    //        (B) For each pair identified in (A), find the IDs, links, and IDs and names of
    //            the containing album of each photo in which they are tagged together
    public FakebookArrayList<MatchPair> matchMaker(int num, int yearDiff) throws SQLException {
        FakebookArrayList<MatchPair> results = new FakebookArrayList<MatchPair>("\n");

        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly);
            Statement stmt2 = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                UserInfo u1 = new UserInfo(93103, "Romeo", "Montague");
                UserInfo u2 = new UserInfo(93113, "Juliet", "Capulet");
                MatchPair mp = new MatchPair(u1, 1597, u2, 1597);
                PhotoInfo p = new PhotoInfo(167, 309, "www.photolink.net", "Tragedy");
                mp.addSharedPhoto(p);
                results.add(mp);
            */
            String query1 = 
                "WITH common_photo AS (" +
                "SELECT t1.tag_subject_id AS user1_id, t2.tag_subject_id AS user2_id, COUNT(*) AS common_photo_count " +
                "FROM " + TagsTable + " t1 " + 
                "JOIN " + TagsTable + " t2 " +
                "ON t1.tag_photo_id = t2.tag_photo_id " +
                "AND t1.tag_subject_id < t2.tag_subject_id " +
                "JOIN " + UsersTable + " u1 ON t1.tag_subject_id = u1.user_id " +
                "JOIN " + UsersTable + " u2 ON t2.tag_subject_id = u2.user_id " +
                "WHERE u1.gender = u2.gender " +
                "AND ABS(u1.year_of_birth - u2.year_of_birth) <= " + yearDiff + " " +
                "AND NOT EXISTS (SELECT * FROM " + FriendsTable + " f WHERE (f.user1_id = u1.user_id AND f.user2_id = u2.user_id)) " +
                "GROUP BY t1.tag_subject_id, t2.tag_subject_id " +
                ") " +
                "SELECT u1.user_id, u1.first_name, u1.last_name, u1.year_of_birth, u2.user_id, u2.first_name, u2.last_name, u2.year_of_birth " +
                "FROM common_photo c " +
                "JOIN " + UsersTable + " u1 ON c.user1_id = u1.user_id " +
                "JOIN " + UsersTable + " u2 ON c.user2_id = u2.user_id " +
                "ORDER BY c.common_photo_count DESC, u1.user_id ASC, u2.user_id ASC " +
                "FETCH FIRST " + num + " ROWS ONLY";
            ResultSet rst = stmt.executeQuery(query1);
            while (rst.next()) {
                long uid1 = rst.getLong(1);
                String fname1 = rst.getString(2);
                String lname1 = rst.getString(3);
                int yob1 = rst.getInt(4);
                long uid2 = rst.getLong(5);
                String fname2 = rst.getString(6);
                String lname2 = rst.getString(7);
                int yob2 = rst.getInt(8);
                UserInfo u1 = new UserInfo(uid1, fname1, lname1);
                UserInfo u2 = new UserInfo(uid2, fname2, lname2);
                MatchPair mp = new MatchPair(u1, yob1, u2, yob2);

                String query2 = 
                    "SELECT p.photo_id, p.photo_link, p.album_id, a.album_name " +
                    "FROM " + TagsTable + " t1 " +
                    "JOIN " + TagsTable + " t2 " +
                    "ON t1.tag_photo_id = t2.tag_photo_id " +
                    "JOIN " + PhotosTable + " p " +
                    "ON t1.tag_photo_id = p.photo_id " +
                    "JOIN " + AlbumsTable + " a " +
                    "ON p.album_id = a.album_id " +
                    "WHERE t1.tag_subject_id = " + uid1 + " " +
                    "AND t2.tag_subject_id = " + uid2 +
                    "ORDER BY p.photo_id ASC";
                ResultSet rst2 = stmt2.executeQuery(query2);
                while (rst2.next()) {
                    long pid = rst2.getLong(1);
                    String plink = rst2.getString(2);
                    Long aid = rst2.getLong(3);
                    String aname = rst2.getString(4);
                    PhotoInfo p = new PhotoInfo(pid, aid, plink, aname);
                    mp.addSharedPhoto(p);
                }
                results.add(mp);
                rst2.close();
            }
            rst.close();
            stmt.close();
            stmt2.close();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        return results;
    }

    @Override
    // Query 6
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the IDs, first names, and last names of each of the two users in
    //            the top <num> pairs of users who are not friends but have a lot of
    //            common friends
    //        (B) For each pair identified in (A), find the IDs, first names, and last names
    //            of all the two users' common friends
    public FakebookArrayList<UsersPair> suggestFriends(int num) throws SQLException {
        FakebookArrayList<UsersPair> results = new FakebookArrayList<UsersPair>("\n");

        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly);
             Statement stmt2 = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                UserInfo u1 = new UserInfo(16, "The", "Hacker");
                UserInfo u2 = new UserInfo(80, "Dr.", "Marbles");
                UserInfo u3 = new UserInfo(192, "Digit", "Le Boid");
                UsersPair up = new UsersPair(u1, u2);
                up.addSharedFriend(u3);
                results.add(up);
            */
            String query1 =
                "SELECT u1.user_id, u1.first_name, u1.last_name, " +
                "u2.user_id, u2.first_name, u2.last_name, " +
                "COUNT(*) AS common_friends " +
                "FROM " + UsersTable + " u1 " +
                "JOIN " + UsersTable + " u2 " +
                "ON u1.user_id < u2.user_id " +
                "JOIN " + UsersTable + " u3 " +
                "ON u3.user_id <> u1.user_id AND u3.user_id <> u2.user_id " +
                "JOIN " + FriendsTable + " f1 " +
                "ON (f1.user1_id = LEAST(u1.user_id, u3.user_id) AND f1.user2_id = GREATEST(u1.user_id, u3.user_id)) " +
                "JOIN " + FriendsTable + " f2 " +
                "ON (f2.user1_id = LEAST(u2.user_id, u3.user_id) AND f2.user2_id = GREATEST(u2.user_id, u3.user_id)) " +
                "WHERE NOT EXISTS ( " +
                "SELECT 1 FROM " + FriendsTable + " f3 " +
                "WHERE f3.user1_id = LEAST(u1.user_id, u2.user_id) AND f3.user2_id = GREATEST(u1.user_id, u2.user_id)) " +
                "GROUP BY u1.user_id, u1.first_name, u1.last_name, u2.user_id, u2.first_name, u2.last_name " +
                "ORDER BY common_friends DESC, u1.user_id ASC, u2.user_id ASC " +
                "FETCH FIRST " + num + " ROWS ONLY";
            ResultSet rst = stmt.executeQuery(query1);
            while (rst.next()) {
                long uid1 = rst.getLong(1);
                String fname1 = rst.getString(2);
                String lname1 = rst.getString(3);
                long uid2 = rst.getLong(4);
                String fname2 = rst.getString(5);
                String lname2 = rst.getString(6);
                UsersPair up = new UsersPair(new UserInfo(uid1, fname1, lname1), new UserInfo(uid2, fname2, lname2));

                String query2 =
                    "SELECT u.user_id, u.first_name, u.last_name " +
                    "FROM " + UsersTable + " u " +
                    "JOIN " + FriendsTable + " f1 " +
                    "ON (f1.user1_id = LEAST(u.user_id, " + uid1 + ") AND f1.user2_id = GREATEST(u.user_id, " + uid1 + ")) " +
                    "JOIN " + FriendsTable + " f2 " +
                    "ON (f2.user1_id = LEAST(u.user_id, " + uid2 + ") AND f2.user2_id = GREATEST(u.user_id, " + uid2 + ")) " +
                    "ORDER BY u.user_id ASC";
                ResultSet rst2 = stmt2.executeQuery(query2);
                while (rst2.next()) {
                    long uid = rst2.getLong(1);
                    String fname = rst2.getString(2);
                    String lname = rst2.getString(3);
                    up.addSharedFriend(new UserInfo(uid, fname, lname));
                }
                results.add(up);
                rst2.close();
            }
            rst.close();
            stmt.close();
            stmt2.close();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        return results;
    }

    @Override
    // Query 7
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the name of the state or states in which the most events are held
    //        (B) Find the number of events held in the states identified in (A)
    public EventStateInfo findEventStates() throws SQLException {
        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly);
            Statement stmt2 = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                EventStateInfo info = new EventStateInfo(50);
                info.addState("Kentucky");
                info.addState("Hawaii");
                info.addState("New Hampshire");
                return info;
            */
            String query1 = 
                "SELECT MAX(event_count) " +
                "FROM ( " +
                "SELECT COUNT(*) AS event_count " +
                "FROM " + EventsTable + " e " +
                "JOIN " + CitiesTable + " c ON e.event_city_id = c.city_id " +
                "GROUP BY c.state_name )";
            ResultSet rst = stmt.executeQuery(query1);
            EventStateInfo info = new EventStateInfo(-1);
            while (rst.next()) {
                int maxCount = rst.getInt(1);
                info = new EventStateInfo(maxCount);

                String query2 =
                    "SELECT c.state_name " +
                    "FROM " + EventsTable + " e " +
                    "JOIN " + CitiesTable + " c ON e.event_city_id = c.city_id " +
                    "GROUP BY c.state_name " +
                    "HAVING COUNT(*) = " + maxCount + " " +
                    "ORDER BY c.state_name ASC";
                ResultSet rst2 = stmt2.executeQuery(query2);
                while (rst2.next()) {
                    info.addState(rst2.getString(1));
                }
                rst2.close();
            }
            rst.close();
            stmt.close();
            stmt2.close();
            return info;
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return new EventStateInfo(-1);
        }
    }

    @Override
    // Query 8
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the ID, first name, and last name of the oldest friend of the user
    //            with User ID <userID>
    //        (B) Find the ID, first name, and last name of the youngest friend of the user
    //            with User ID <userID>
    public AgeInfo findAgeInfo(long userID) throws SQLException {
        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly);
            Statement stmt2 = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                UserInfo old = new UserInfo(12000000, "Galileo", "Galilei");
                UserInfo young = new UserInfo(80000000, "Neil", "deGrasse Tyson");
                return new AgeInfo(old, young);
            */
           String query1 = 
                "SELECT u2.user_id, u2.first_name, u2.last_name " +
                "FROM " + UsersTable + " u1 " +
                "JOIN " + UsersTable + " u2 " + 
                "ON (u1.user_id <> u2.user_id) AND u1.user_id = " + userID + " " +
                "JOIN " + FriendsTable + " f " +
                "ON (f.user1_id = LEAST(u1.user_id, u2.user_id) AND f.user2_id = GREATEST(u1.user_id, u2.user_id)) " +
                "ORDER BY u2.year_of_birth ASC, u2.user_id DESC " +
                "FETCH FIRST 1 ROWS ONLY";
            AgeInfo info = new AgeInfo(new UserInfo(-1, "ERROR", "ERROR"), new UserInfo(-1, "ERROR", "ERROR"));
            ResultSet rst = stmt.executeQuery(query1);
            while (rst.next()) {
                long uid = rst.getLong(1);
                String fname = rst.getString(2);
                String lname = rst.getString(3);
                UserInfo old = new UserInfo(uid, fname, lname);

                String query2 = 
                    "SELECT u2.user_id, u2.first_name, u2.last_name " +
                    "FROM " + UsersTable + " u1 " +
                    "JOIN " + UsersTable + " u2 " + 
                    "ON (u1.user_id <> u2.user_id) AND u1.user_id = " + userID + " " +
                    "JOIN " + FriendsTable + " f " +
                    "ON (f.user1_id = LEAST(u1.user_id, u2.user_id) AND f.user2_id = GREATEST(u1.user_id, u2.user_id)) " +
                    "ORDER BY u2.year_of_birth DESC, u2.user_id DESC " +
                    "FETCH FIRST 1 ROWS ONLY";
                ResultSet rst2 = stmt2.executeQuery(query2);
                while (rst2.next()) {
                    long uidY = rst2.getLong(1);
                    String fnameY = rst2.getString(2);
                    String lnameY = rst2.getString(3);
                    UserInfo young = new UserInfo(uidY, fnameY, lnameY);
                    info = new AgeInfo(old, young);
                }
                rst2.close();
            }
            rst.close();
            stmt.close();
            stmt2.close();
            return info;
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return new AgeInfo(new UserInfo(-1, "ERROR", "ERROR"), new UserInfo(-1, "ERROR", "ERROR"));
        }
    }

    @Override
    // Query 9
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find all pairs of users that meet each of the following criteria
    //              (i) same last name
    //              (ii) same hometown
    //              (iii) are friends
    //              (iv) less than 10 birth years apart
    public FakebookArrayList<SiblingInfo> findPotentialSiblings() throws SQLException {
        FakebookArrayList<SiblingInfo> results = new FakebookArrayList<SiblingInfo>("\n");

        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                UserInfo u1 = new UserInfo(81023, "Kim", "Kardashian");
                UserInfo u2 = new UserInfo(17231, "Kourtney", "Kardashian");
                SiblingInfo si = new SiblingInfo(u1, u2);
                results.add(si);
            */
            String query = 
                "SELECT u1.user_id, u1.first_name, u1.last_name, u2.user_id, u2.first_name, u2.last_name " +
                "FROM " + UsersTable + " u1 " +
                "JOIN " + UsersTable + " u2 " +
                "ON u1.user_id < u2.user_id " +
                "AND u1.last_name = u2.last_name " +
                "JOIN " + HometownCitiesTable + " h1 " +
                "ON u1.user_id = h1.user_id " +
                "JOIN " + HometownCitiesTable + " h2 " +
                "ON u2.user_id = h2.user_id " +
                "WHERE h1.hometown_city_id = h2.hometown_city_id " +
                "AND ABS(u1.year_of_birth - u2.year_of_birth) < 10 " +
                "AND EXISTS ( " +
                "SELECT 1 FROM " + FriendsTable + " f " +
                "WHERE f.user1_id = u1.user_id AND f.user2_id = u2.user_id ) " +
                "ORDER BY u1.user_id ASC, u2.user_id ASC";
            ResultSet rst = stmt.executeQuery(query);
            while (rst.next()) {
                long uid1 = rst.getLong(1);
                String fname1 = rst.getString(2);
                String lname1 = rst.getString(3);
                long uid2 = rst.getLong(4);
                String fname2 = rst.getString(5);
                String lname2 = rst.getString(6);
                SiblingInfo si = new SiblingInfo(new UserInfo(uid1, fname1, lname1), new UserInfo(uid2, fname2, lname2));
                results.add(si);
            }
            rst.close();
            stmt.close();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        return results;
    }

    // Member Variables
    private Connection oracle;
    private final String UsersTable = FakebookOracleConstants.UsersTable;
    private final String CitiesTable = FakebookOracleConstants.CitiesTable;
    private final String FriendsTable = FakebookOracleConstants.FriendsTable;
    private final String CurrentCitiesTable = FakebookOracleConstants.CurrentCitiesTable;
    private final String HometownCitiesTable = FakebookOracleConstants.HometownCitiesTable;
    private final String ProgramsTable = FakebookOracleConstants.ProgramsTable;
    private final String EducationTable = FakebookOracleConstants.EducationTable;
    private final String EventsTable = FakebookOracleConstants.EventsTable;
    private final String AlbumsTable = FakebookOracleConstants.AlbumsTable;
    private final String PhotosTable = FakebookOracleConstants.PhotosTable;
    private final String TagsTable = FakebookOracleConstants.TagsTable;
}
