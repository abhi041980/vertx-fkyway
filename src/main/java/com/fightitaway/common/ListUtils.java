package com.fightitaway.common;

import java.util.List;

public class ListUtils {

	public static boolean isNotEmpty(List list) {
		return list!=null && !list.isEmpty();
	}
	
	public static boolean isEmpty(List list) {
		return list==null ||list.isEmpty();
	}
}
