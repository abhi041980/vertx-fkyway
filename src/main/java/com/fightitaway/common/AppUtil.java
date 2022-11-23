package com.fightitaway.common;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * Request utilities.
 * 
 * @author Smartdietplanner
 * @since 08-Aug-2020
 *
 */
public class AppUtil {
	public static boolean isEmpty(Collection<?> collection)
	  {
	    return (collection == null) || (collection.isEmpty());
	  }

	  public static boolean isEmpty(Map<?, ?> map)
	  {
	    return (map == null) || (map.isEmpty());
	  }

	  public static boolean isEmpty(Object object)
	  {
	    return object == null;
	  }

	  public static boolean isEmpty(Object[] array)
	  {
	    return (array == null) || (array.length == 0);
	  }

	  public static boolean isEmpty(String string)
	  {
	    return (string == null) || (string.trim().length() == 0);
	  }

	  public static boolean isNotNullOrEmpty(String str)
	  {
	    return (str != null) && (!str.isEmpty());
	  }

	  public static String getValueOrDefault(String value, String defaultValue) {
	    return (isNotNullOrEmpty(value)) ? value : defaultValue;
	  }
	  
	  public static Calendar getCalendarInstance() {
		  Calendar calendar = Calendar.getInstance(); // creates calendar
			calendar.setTime(new Date()); // sets calendar time/date
			calendar.add(Calendar.HOUR_OF_DAY, 10); // adds ten hour
			calendar.add(Calendar.MINUTE, 30);
			
			return calendar;
	  }
	  
	  public static Calendar getExpiryDate(Date date, Integer noOfDays) {
		  Calendar calendar = Calendar.getInstance(); // creates calendar
			calendar.setTime(date); // sets calendar time/date
			calendar.add(Calendar.DAY_OF_MONTH, noOfDays);
			calendar.add(Calendar.HOUR_OF_DAY, 10); // adds ten hour
			calendar.add(Calendar.MINUTE, 30);
			
			return calendar;
	  }
}
