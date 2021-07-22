package com.nist;
public class Data {
	private int id;
	private String name;
	private String manager;
	private int manager_id;
	public int getId() {
		return id;
	}
	public String getName() {
		return name;
	}
	public Data(int id,String name) {
       this.id=id;
       this.name =name;
       
	}
	public int getManager_id() {
		return manager_id;
	}
	public void setManager_id(int manager_id) {
		this.manager_id = manager_id;
	}
	public String getManager() {
		return manager;
	}
	public void setManager(String manager) {
		this.manager = manager;
	}
	
}