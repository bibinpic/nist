package com.nist;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

public class TableSyncronize {
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

			ArrayList<Data> updateUserList = new ArrayList<Data>();
			ArrayList<Data> createUserList = new ArrayList<Data>();
			ArrayList<Data> removeUserList = new ArrayList<Data>();

			for (Data user : users) {
				if (user.getId() != 0) {
					if (getTargetUser(user.getId(), users) != null) {
						System.out.println("Target User Update :" + user.getId() + ", Display Name:" + user.getName());
						updateUserList.add(user);
					} else {
						System.out.println("Target User Add :" + user.getId() + ", Display Name:" + user.getName());
						createUserList.add(user);
					}
				} else {
					if (getSourceUser(user.getId2(), users) == null) {
						System.out
								.println("Target User Remove :" + user.getId2() + ", Display Name:" + user.getName2());
						removeUserList.add(user);
					}
				}

			}

			// Compare group tables
			ArrayList<Data> groups = getGroupfromSource(stmt, c);
			ArrayList<Data> removeGrpList = new ArrayList<Data>();
			ArrayList<Data> createGrpList = new ArrayList<Data>();

			for (Data group : groups) {
				if (group.getId() != 0) {
					System.out.println("Target Group Add :" + group.getId() + ", Group Name :" + group.getName());
					createGrpList.add(group);
				} else {
					System.out.println("Target Group Remove :" + group.getId2() + ", Group Name :" + group.getName2());
					removeGrpList.add(group);
				}
			}

			// Fix Target Table Results

			// Update User in System2
			for (Data _user : updateUserList) {
				String sql = "";
				sql = "UPDATE SYSTEM2_USER set system2_display_name ='" + _user.getName() + "' where SYSTEM2_USER_ID="
						+ _user.getId();
				execute(stmt, c, sql);
			}
			// Create User in System2
			for (Data _user : createUserList) {
				String sql = "";
				sql = "INSERT INTO SYSTEM2_USER (SYSTEM2_USER_ID,system2_display_name) VALUES (" + _user.getId() + ",'"
						+ _user.getName() + "') ";
				execute(stmt, c, sql);
			}
			// Create Group in System2
			for (Data _group : createGrpList) {
				String sql = "";
				sql = "INSERT INTO SYSTEM2_USER_group_membership (SYSTEM2_USER_ID,system2_group_name) VALUES ("
						+ _group.getId() + ",'" + _group.getName() + "')";
				execute(stmt, c, sql);
			}

			// Remove Group from System2
			for (Data _group : removeGrpList) {
				String sql = "";
				sql = "DELETE from  SYSTEM2_USER_group_membership where  system2_group_name ='" + _group.getName2()
						+ "' and SYSTEM2_USER_ID=" + _group.getId2();
				execute(stmt, c, sql);
			}

			// Remove User from System2
			for (Data _user : removeUserList) {
				String sql = "";
				sql = "DELETE from  SYSTEM2_USER where  system2_display_name  ='" + _user.getName2()
						+ "' and SYSTEM2_USER_ID=" + _user.getId2();
				execute(stmt, c, sql);
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
					"select SYSTEM1_USR_ID,REGEXP_REPLACE(system1_displayname,'(.*) (.*)', '\\2, \\1') \r\n"
							+ "as display_name_1,SYSTEM2_USER_ID,system2_display_name  \r\n"
							+ "from SYSTEM1_USR  full outer join SYSTEM2_USER\r\n"
							+ "on (SYSTEM1_USR.SYSTEM1_USR_ID = SYSTEM2_USER.SYSTEM2_USER_ID\r\n"
							+ "and REGEXP_REPLACE(system1_displayname,'(.*) (.*)', '\\2, \\1') = SYSTEM2_USER.system2_display_name)\r\n"
							+ "where SYSTEM2_USER.SYSTEM2_USER_ID IS NULL or SYSTEM1_USR.SYSTEM1_USR_ID IS NULL;");
			while (rs.next()) {
				int id = rs.getInt("SYSTEM1_USR_id");
				String name = rs.getString("display_name_1");
				int id_2 = rs.getInt("SYSTEM2_USER_ID");
				String name_2 = rs.getString("system2_display_name");
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
					"SELECT system2_user_id,system2_group_name,system1_usr_id,system1_grp_nm FROM system2_user_group_membership\r\n"
							+ "full outer join SYSTEM1_USR_grp_membership ON\r\n"
							+ "(system2_user_group_membership.system2_user_id= SYSTEM1_USR_grp_membership.SYSTEM1_USR_ID \r\n"
							+ "and SYSTEM2_USER_group_membership.system2_group_name = SYSTEM1_USR_grp_membership.system1_grp_nm)\r\n"
							+ "WHERE SYSTEM1_USR_grp_membership.SYSTEM1_USR_ID IS NULL or system2_user_group_membership.system2_user_id IS NULL;");
			while (rs.next()) {
				int id = rs.getInt("system1_usr_id");
				String name = rs.getString("system1_grp_nm");
				int id_2 = rs.getInt("system2_user_id");
				String name_2 = rs.getString("system2_group_name");
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
