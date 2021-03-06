package com.nist;

public class Employee {

    String name;
    int id;

    public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Employee(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof Employee))
            return false;
        Employee employee = (Employee) obj;
        return employee.getId() == this.getId()
                && employee.getName() == this.getName();
    }

     @Override
        public int hashCode() {
            int result=1;
            result=31*result+id;
            result=31*result+(name!=null ? name.hashCode():0);
            return result;
        }

}