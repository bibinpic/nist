package com.nist;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class CircularDetectionSQL {
	public static void main(String[] args) {

		Connection c = null;
		Statement stmt = null;

		try {
			Class.forName("org.postgresql.Driver");
			c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/nist", "postgres", "password");
			c.setAutoCommit(false);
			System.out.println("Opened database successfully");

			// Get All CR Users Chain
			System.out.println("--------Print all CR Issues--------");

			ArrayList<UserData> users = getCRUsersChain(stmt, c);

			// Fix CR Issue User Objects
			Map<Integer, Integer> _fixids = new HashMap<Integer, Integer>();
			ArrayList<UserData> manager = new ArrayList<UserData>();
			System.out.println("--------Fix all CR Issues--------");
			for (UserData user : users) {
				manager.add(user);
				_fixids = getCRfixuser(user.getUserId(), user.getUserName(), manager, _fixids);
			}

			for (int user_id : _fixids.values()) {
				fixCRChain(stmt, c, user_id);
				c.commit();
			}
			System.out.println("--------- Print results after CR  fix ----------");

			for (UserData user : users) {
				reportFixedUserChain(stmt, c, user);
			}
			c.close();

		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}

	}
	private static Map<Integer, Integer> getCRfixuser(int user_id, String userName, ArrayList<UserData> users,
			Map<Integer, Integer> _crlist) {
		ArrayList<UserData> _list = new ArrayList<UserData>();
		UserData user = null;
		boolean data = true;
		while (data) {
			user = getUserId(user_id, users);
			if (user == null) {
				data = false;
			} else {
				if (user_id != -1) {
					if (!_list.contains(user)) {
						_list.add(user);
						 user_id = user.getManagerId();
					} else {
						int _crid = getManager(user.getManagerId(), user.getUserId(), user.getUserName(), _list);
						if (_crid != -1) {
							if (isUnique(_crlist, _crid, _crid)) {
								System.out.println(userName + " >>User ID to Correct :" + _crid);
							}
						}
						data = false;
					}
				} else {
					data = false;
				}
			}
		}
		return _crlist;
	}
	private static <K, V> boolean isUnique(Map<K, V> map, K key, V v1) {
		V v2 = map.putIfAbsent(key, v1);
		if (v2 != null) {
			return false;
		} else {
			return true;
		}
	}
	public static int getManager(int managerId, int userid, String userName, ArrayList<UserData> _list) {
		UserData users = null;
		boolean data = true;
		int _crid = -1;
		while (data) {
			users = getUserId(managerId, _list);
			if (users == null) {
				data = false;
			} else {
				UserData userdata = users;
				userName = userdata.getUserName();
				if (userid != userdata.getManagerId()) {
					managerId = userdata.getManagerId();
					_crid = userdata.getUserId();
				} else {
					if (_list.size() == 1 || _list.size() == 2) {
						_crid = userdata.getManagerId();
					}
					data = false;
				}
			}
		}
		return _crid;
	}

	public static final UserData getUserId(int userId, ArrayList<UserData> user) {
		return user.stream().filter(m -> m.UserId == userId).findAny().orElse(null);
	}

	public static ArrayList<UserData> getCRUsersChain(Statement stmt, Connection c) {
		ArrayList<UserData> users = new ArrayList<UserData>();
		try {
			stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery(
					"select array_to_string(path, ' > ') cr_managers,cr_managers.idpath,cr_managers.username,cr_managers.id,cr_managers.manager_id\r\n"
					+ "from cr_managers where cycle and manager_id<>-1 order by id");
			while (rs.next()) {
				int id = rs.getInt("id");
				String name = rs.getString("username");
				int manager_id = rs.getInt("manager_id");
				String path = rs.getString("cr_managers");
				System.out.println(name + " : CR Happened: -> " + path);
				UserData user = new UserData(id, manager_id, name);
				users.add(user);
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
		return users;
	}
	public static void reportFixedUserChain(Statement stmt, Connection c, UserData user) {
		try {
			stmt = c.createStatement();
			ResultSet rs = stmt
					.executeQuery("WITH RECURSIVE managers(id, manager_id,username, depth, path, cycle) AS (\r\n"
							+ "	SELECT u.id, u.manager_id,u.username, 1,\r\n" + "		ARRAY[u.username],\r\n"
							+ "		false\r\n" + "	FROM people u where u.id =" + user.getUserId() + "	UNION ALL\r\n"
							+ "	SELECT u.id, u.manager_id,u.username, cm.depth + 1,\r\n"
							+ "		path || u.username,\r\n" + "		u.username = ANY(path)\r\n"
							+ "	FROM people u, managers cm\r\n" + "	WHERE u.id = cm.manager_id AND NOT cycle\r\n"
							+ "	)\r\n" + "\r\n"
							+ "select array_to_string (path[array_lower(path,1) : array_upper(path,1)-1],'>')  managers from managers where cycle ");
			while (rs.next()) {
				String manager = rs.getString("managers");
				System.out.println(user.getUserName() + " : Fixed CR Happened-> " + manager);
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
	}
	public static void fixCRChain(Statement stmt, Connection c, int id) {
		try {
			stmt = c.createStatement();
			String sql = "UPDATE people set manager_id =-1 where id=" + id;
			stmt.executeUpdate(sql);
			stmt.close();
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
	}
	private static class UserData {
		public UserData(int userId, int managerId, String UserName) {
			setUserId(userId);
			setManagerId(managerId);
			setUserName(UserName);
		}
		private int UserId;
		private String UserName;
		private int ManagerId;

		public int getUserId() {
			return UserId;
		}

		public final void setUserId(int value) {
			UserId = value;
		}

		public final int getManagerId() {
			return ManagerId;
		}

		public final void setManagerId(int value) {
			ManagerId = value;
		}

		public String getUserName() {
			return UserName;
		}

		public void setUserName(String userName) {
			UserName = userName;
		}
	}
}