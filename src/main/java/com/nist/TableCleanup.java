package com.nist;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

public class TableCleanup {
	public static void main(String args[]) {
		Connection c = null;
		Statement stmt = null;
		try {
			Class.forName("org.postgresql.Driver");
			c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/nist", "postgres", "password");
			c.setAutoCommit(false);
			System.out.println("Opened database successfully");

			// Compare users from table
			ArrayList<Data> users = getUsersfromSource(stmt, c);
			ArrayList<Data> targetUsrList = new ArrayList<Data>();

			for (Data user : users) {
				if (user.getId() != 0) {
					if (getTargetUser(user.getId(), users) != null) {
						System.out.println("Target User Update :" + user.getId() + ", Display Name:" + user.getName());
						user.setAction(1);
						targetUsrList.add(user);
					} else {
						System.out.println("Target User Add :" + user.getId() + ", Display Name:" + user.getName());
						user.setAction(2);
						targetUsrList.add(user);
					}
				} else {
					if (getSourceUser(user.getId2(), users) == null) {
						System.out.println("Target User Remove :" + user.getId2() + ", Display Name:" + user.getName2());
						user.setAction(3);
						targetUsrList.add(user);
					}
				}

			}

			// Compare group tables
			ArrayList<Data> groups = getGroupfromSource(stmt, c);
			ArrayList<Data> targetGrpList = new ArrayList<Data>();

			for (Data group : groups) {
				if (group.getId() != 0) {
					System.out.println("Target Group Add :" + group.getId() + ", Group Name :" + group.getName());
					group.setAction(2);
					targetGrpList.add(group);
				} else {
					System.out.println("Target Group Remove :" + group.getId2() + ", Group Name :" + group.getName2());
					group.setAction(3);
					targetGrpList.add(group);
				}
			}

			// Fix Target Table Results

			// Update and Insert User System2
			for (Data _user : targetUsrList) {
				String sql = "";
				if (_user.getAction() == 1) {
					sql = "UPDATE SYSTEM2_USR set system2_displayname ='" + _user.getName() + "' where SYSTEM2_USR_ID="
							+ _user.getId();
					execute(stmt, c, sql);
				} else if (_user.getAction() == 2) {
					sql = "INSERT INTO SYSTEM2_USR (SYSTEM2_USR_ID,system2_displayname) VALUES (" + _user.getId() + ",'"
							+ _user.getName() + "') ";
					execute(stmt, c, sql);
				}

			}

			// Add and Remove Group System2
			for (Data _group : targetGrpList) {
				String sql = "";
				if (_group.getAction() == 2) {
					sql = "INSERT INTO SYSTEM2_USR_grp_membership (SYSTEM2_USR_ID,system2_grp_nm) VALUES ("
							+ _group.getId() + ",'" + _group.getName() + "')";
					execute(stmt, c, sql);
				} else if (_group.getAction() == 3) {
					sql = "DELETE from  SYSTEM2_USR_grp_membership where  system2_grp_nm ='" + _group.getName2()
							+ "' and SYSTEM2_USR_ID=" + _group.getId2();
					execute(stmt, c, sql);
				}

			}

			// Remove User System2
			for (Data _user : targetUsrList) {
				String sql = "";
				if (_user.getAction() == 3) {
					sql = "DELETE from  SYSTEM2_USR where  system2_displayname  ='" + _user.getName2()
							+ "' and SYSTEM2_USR_ID=" + _user.getId2();
					execute(stmt, c, sql);
				}

			}
			c.close();
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
		System.out.println("Operation done successfully");
	}

	public static final Data getTargetUser(int userId, ArrayList<Data> user) {
		return user.stream().filter(m -> m.getId2() == userId).findAny().orElse(null);
	}

	public static final Data getSourceUser(int userId, ArrayList<Data> user) {
		return user.stream().filter(m -> m.getId() == userId).findAny().orElse(null);
	}

	public static ArrayList<Data> getUsersfromSource(Statement stmt, Connection c) {
		ArrayList<Data> user = new ArrayList<Data>();
		try {

			System.out.println("Difference in User data with source and target");
			stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery(
					"select SYSTEM1_USR.*,SYSTEM2_USR.*  from SYSTEM1_USR  full outer join SYSTEM2_USR \r\n"
							+ "   on (SYSTEM1_USR.SYSTEM1_USR_ID = SYSTEM2_USR.SYSTEM2_USR_ID \r\n"
							+ "and SYSTEM1_USR.system1_displayname = SYSTEM2_USR.system2_displayname)\r\n"
							+ "   where SYSTEM2_USR.SYSTEM2_USR_ID IS NULL or SYSTEM1_USR.SYSTEM1_USR_ID IS NULL;");
			while (rs.next()) {
				int id = rs.getInt("SYSTEM1_USR_id");
				String name = rs.getString("system1_displayname");
				int id_2 = rs.getInt("SYSTEM2_USR_id");
				String name_2 = rs.getString("system2_displayname");
				Data d = new Data(id, name);
				d.setId2(id_2);
				d.setName2(name_2);
				user.add(d);
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

		System.out.println("Difference in Group data with source and  target");

		ArrayList<Data> group = new ArrayList<Data>();
		try {
			stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery(
					"SELECT system2_usr_id,system2_grp_nm,system1_usr_id,system1_grp_nm FROM SYSTEM2_USR_grp_membership\r\n"
							+ "full outer join SYSTEM1_USR_grp_membership ON\r\n"
							+ "(SYSTEM2_USR_grp_membership.SYSTEM2_USR_ID = SYSTEM1_USR_grp_membership.SYSTEM1_USR_ID \r\n"
							+ "and SYSTEM2_USR_grp_membership.system2_grp_nm = SYSTEM1_USR_grp_membership.system1_grp_nm) \r\n"
							+ "WHERE SYSTEM1_USR_grp_membership.SYSTEM1_USR_ID IS NULL or SYSTEM2_USR_grp_membership.SYSTEM2_USR_ID IS NULL;");
			while (rs.next()) {
				int id = rs.getInt("system1_usr_id");
				String name = rs.getString("system1_grp_nm");
				int id_2 = rs.getInt("system2_usr_id");
				String name_2 = rs.getString("system2_grp_nm");
				Data d = new Data(id, name);
				d.setId2(id_2);
				d.setName2(name_2);
				group.add(d);
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
		return group;
	}

	public static void execute(Statement stmt, Connection c, String sql) {
		try {
			stmt = c.createStatement();
			stmt.executeUpdate(sql);
			stmt.close();
			c.commit();
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
	}

}
