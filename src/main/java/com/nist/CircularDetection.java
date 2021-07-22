package com.nist;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class CircularDetection {
	public static void main(String[] args) {

		Connection c = null;
		Statement stmt = null;

		try {
			Class.forName("org.postgresql.Driver");
			c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/nist", "postgres", "password");
			c.setAutoCommit(false);
			System.out.println("Opened database successfully");
			// Get All Users
			ArrayList<UserData> users = getUsers(stmt, c);

			ArrayList<UserData> manager = new ArrayList<UserData>();

			System.out.println("--------Print all CR Issues--------");
			Map<Integer, Integer> _fixlist = new HashMap<Integer, Integer>();
			// Detect CR and report
			for (UserData user : users) {
				getReport(user.getUserId(), user.getUserName(), users, true);
			}
			
			System.out.println("--------Fix all CR Issues--------");
			// Fix CR Issue User Objects
			for (UserData user : users) {
				manager.add(user);
				_fixlist = detectCRAndBuildFixList(user.getUserId(), user.getUserName(), manager, _fixlist);
			}

			for (int user_id : _fixlist.values()) {
				fixUserManger(stmt, c, user_id);
				c.commit();
			}
			// Reload users
			users = getUsers(stmt, c);

			// Report all user after CR fix
			System.out.println("--------- Print results after CR  fix ----------");
			for (UserData user : users) {
				getReport(user.getUserId(), user.getUserName(), users, false);
			}
			c.close();
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}

	}

	private static class UserData {
		public UserData(int userId, int managerId, String ManagerName, String UserName) {
			setUserId(userId);
			setManagerId(managerId);
			setManagerName(ManagerName);
			setUserName(UserName);
		}

		private int UserId;

		public final int getUserId() {
			return UserId;
		}

		public final void setUserId(int value) {
			UserId = value;
		}

		private int ManagerId;

		public final int getManagerId() {
			return ManagerId;
		}

		public final void setManagerId(int value) {
			ManagerId = value;
		}

		public String getManagerName() {
			return ManagerName;
		}

		public void setManagerName(String managerName) {
			ManagerName = managerName;
		}

		public String getUserName() {
			return UserName;
		}

		public void setUserName(String userName) {
			UserName = userName;
		}

		private String ManagerName;

		private String UserName;
	}

	private static Map<Integer, Integer> detectCRAndBuildFixList(int user_id, String userName,
			ArrayList<UserData> users, Map<Integer, Integer> _crlist) {
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
								System.out.println(userName + " >>Fix User Id :" + _crid);
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
					data = false;
				}
			}
		}
		return _crid;
	}

	private static boolean getReport(int user_id, String userName, ArrayList<UserData> users, boolean _crOnly) {
		StringBuilder _log = new StringBuilder();
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();
		UserData user = null;
		boolean data = true;
		while (data) {
			user = getUserId(user_id, users);
			if (user == null) {
				data = false;
			} else {
				if (user_id != -1) {
					if (isUnique(map, user.getUserId(), user.getManagerId())) {
						user_id = user.getManagerId();
						_log.append(user.getUserName()).append("->");
					} else {
						_log.append(user.getUserName());
						System.out.println(userName + " >> CR Detected : " + _log.toString());
						data = false;
					}
				} else {
					_log.append(user.getManagerName());
					data = false;
				}
			}
		}
		// Log after CR
		if (!_crOnly) {
			System.out.println(userName + " >> Path : " + _log.toString());
		}
		return data;
	}

	private static <K, V> boolean isUnique(Map<K, V> map, K key, V v1) {
		V v2 = map.putIfAbsent(key, v1);
		if (v2 != null) {
			return false;
		} else {
			return true;
		}
	}

	public static final UserData getUserId(int userId, ArrayList<UserData> user) {
		return user.stream().filter(m -> m.UserId == userId).findAny().orElse(null);
	}

	public static ArrayList<UserData> getUsers(Statement stmt, Connection c) {
		ArrayList<UserData> users = new ArrayList<UserData>();
		try {
			stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery(
					"select a.id as id , a.username user_name,b.username manager_name,a.manager_id as manager_id from people a left join people b on a.manager_id = b.id order by a.id;");
			while (rs.next()) {
				int id = rs.getInt("id");
				String name = rs.getString("user_name");
				int manager_id = rs.getInt("manager_id");
				String manager_name = rs.getString("manager_name");
				UserData user = new UserData(id, manager_id, manager_name, name);
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

	public static void fixUserManger(Statement stmt, Connection c, int id) {
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
}