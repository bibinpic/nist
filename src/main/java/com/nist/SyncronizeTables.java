package com.nist;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

public class SyncronizeTables {
	public static void main(String args[]) {
		Connection c = null;
		Statement stmt = null;
		try {
			Class.forName("org.postgresql.Driver");
			c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/nist", "postgres", "password");
			c.setAutoCommit(false);
			System.out.println("Opened database successfully");

			// Get New User and Updates from Source Table
			ArrayList<Data> users = getUsersfromSource(stmt, c);

			for (Data user : users) {
				// Add/Update user in to the target system
				updateSystem2USR(stmt, c, user);
			}
			c.commit();
			// Get New Group membership from Source Table
			ArrayList<Data> groups = getGroupfromSource(stmt, c);
			for (Data group : groups) {
				// Add new group in to the target system
				updateSystem2GRP(stmt, c, group);
			}
			c.commit();

			// Remove System2 Invalid groups

			removeGroup(stmt, c);
			c.commit();

			// Remove System2 Invalid users

			removeInvalidUser(stmt, c);
			c.commit();

			c.close();
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
		System.out.println("Operation done successfully");
	}

	public static ArrayList<Data> getUsersfromSource(Statement stmt, Connection c) {
		ArrayList<Data> user = new ArrayList<Data>();
		try {

			System.out.println("Add Out of Sync User data to target");

			stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery(
					"SELECT SYSTEM1_USR_id,system1_displayname FROM SYSTEM1_USR LEFT JOIN SYSTEM2_USR ON\r\n"
							+ "(SYSTEM1_USR.SYSTEM1_USR_ID = SYSTEM2_USR.SYSTEM2_USR_ID \r\n"
							+ "and SYSTEM1_USR.system1_displayname = SYSTEM2_USR.system2_displayname 													   \r\n"
							+ ") WHERE SYSTEM2_USR.SYSTEM2_USR_ID IS NULL;");
			while (rs.next()) {
				int id = rs.getInt("SYSTEM1_USR_id");
				String name = rs.getString("system1_displayname");
				user.add(new Data(id, name));
				System.out.println("ID = " + id);
				System.out.println("NAME = " + name);
				System.out.println();
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
		return user;
	}

	public static ArrayList<Data> getGroupfromSource(Statement stmt, Connection c) {

		System.out.println("Add Out of Sync Group data to target");

		ArrayList<Data> group = new ArrayList<Data>();
		try {
			stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery(
					"SELECT SYSTEM1_USR_grp_membership.* FROM SYSTEM1_USR_grp_membership LEFT JOIN SYSTEM2_USR_grp_membership ON\r\n"
							+ "(SYSTEM1_USR_grp_membership.SYSTEM1_USR_ID = SYSTEM2_USR_grp_membership.SYSTEM2_USR_ID \r\n"
							+ "and SYSTEM1_USR_grp_membership.system1_grp_nm = SYSTEM2_USR_grp_membership.system2_grp_nm 													   \r\n"
							+ ") WHERE SYSTEM2_USR_grp_membership.SYSTEM2_USR_ID IS NULL");
			while (rs.next()) {
				int id = rs.getInt("SYSTEM1_USR_id");
				String name = rs.getString("system1_grp_nm");
				group.add(new Data(id, name));
				System.out.println("ID = " + id);
				System.out.println("NAME = " + name);
				System.out.println();
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
		return group;
	}

	public static void updateSystem2USR(Statement stmt, Connection c, Data user) {
		try {
			stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM SYSTEM2_USR where SYSTEM2_USR_ID=" + user.getId());
			String sql = "";
			if (rs.next()) {
				sql = "UPDATE SYSTEM2_USR set system2_displayname ='" + user.getName() + "' where SYSTEM2_USR_ID="
						+ user.getId();
			} else {
				sql = "INSERT INTO SYSTEM2_USR (SYSTEM2_USR_ID,system2_displayname) " + "VALUES (" + user.getId() + ",'"
						+ user.getName() + "');";
			}
			rs.close();
			stmt.executeUpdate(sql);
			stmt.close();
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
	}

	public static void updateSystem2GRP(Statement stmt, Connection c, Data group) {
		try {
			stmt = c.createStatement();
			stmt.executeUpdate("INSERT INTO SYSTEM2_USR_grp_membership (SYSTEM2_USR_ID,system2_grp_nm) " + "VALUES ("
					+ group.getId() + ",'" + group.getName() + "');");
			stmt.close();
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
	}

	public static void removeGroup(Statement stmt, Connection c) {
		try {
			System.out.println("Remove Out of Sync Group data from target");
			
			stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery(
					"SELECT system2_usr_id,system2_grp_nm FROM SYSTEM2_USR_grp_membership LEFT JOIN SYSTEM1_USR_grp_membership ON\r\n"
							+ "(SYSTEM2_USR_grp_membership.SYSTEM2_USR_ID = SYSTEM1_USR_grp_membership.SYSTEM1_USR_ID \r\n"
							+ "and SYSTEM2_USR_grp_membership.system2_grp_nm = SYSTEM1_USR_grp_membership.system1_grp_nm 													   \r\n"
							+ ") WHERE SYSTEM1_USR_grp_membership.SYSTEM1_USR_ID IS NULL\r\n" + ";");
			String sql = "";
			while (rs.next()) {
				sql = "DELETE from  SYSTEM2_USR_grp_membership where  system2_grp_nm ='"
						+ rs.getString("system2_grp_nm") + "' and SYSTEM2_USR_ID=" + rs.getInt("SYSTEM2_USR_id");
			}
			rs.close();
			stmt.executeUpdate(sql);
			stmt.close();
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
	}

	public static void removeInvalidUser(Statement stmt, Connection c) {
		try {
			System.out.println("Remove Out of Sync User data from target");
			stmt = c.createStatement();
			stmt.executeUpdate(
					"DELETE from SYSTEM2_USR where SYSTEM2_USR_ID IN (SELECT SYSTEM2_USR_id FROM SYSTEM2_USR LEFT JOIN SYSTEM1_USR ON "
					+ "(SYSTEM2_USR.SYSTEM2_USR_ID = SYSTEM1_USR.SYSTEM1_USR_ID) WHERE SYSTEM1_USR.SYSTEM1_USR_ID IS NULL);");
			stmt.close();
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
	}
}
