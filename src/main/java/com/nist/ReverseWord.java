package com.nist;

import java.util.Arrays;
import java.util.Collections;

public class ReverseWord {

	public static void main(String[] args) {
		String words[] = "AB,CDEF,G,HIJKL,MNOPQ".split(",");
	
		Collections.reverse(Arrays.asList(words));
		
        System.out.println(Arrays.asList(words));

	}
}
