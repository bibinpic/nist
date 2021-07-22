package com.nist;

import java.util.HashSet;

/**
 * Hello world!
 *
 */
public class TestEqualHash 
{
    public static void main( String[] args )
    {

        Employee employee1 = new Employee("bibin", 25);
        Employee employee2 = new Employee("bibin", 25);  
        
        System.out.println("Equals Test :" +employee1.equals(employee2));   
        HashSet<Employee> employees = new HashSet<Employee>();     
        employees.add(employee1);    
        System.out.println("Equals Test Hash Set :" +employees.contains(employee2));      
       System.out.println("Hash Test hashCode():"+ "employee1:  " + employee1.hashCode()+ "  employee2:" + employee2.hashCode());  	
    }  
}
