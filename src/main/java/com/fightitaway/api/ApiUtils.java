package com.fightitaway.api;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TimeZone;
import java.util.stream.Collectors;

import com.fightitaway.common.MongoRepositoryWrapper;
import com.fightitaway.service.CalculationData;
import com.fightitaway.service.CalculationResult;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;

/**
 * API utilities - reusable components.
 * 
 * @author Smartdietplanner
 * @since 08-Aug-2020
 *
 */
public class ApiUtils {
	//private static final io.vertx.core.logging.Logger logger = LoggerFactory.getLogger(ApiUtils.class);
	protected  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MongoRepositoryWrapper.class);

	private static DecimalFormat df2 = new DecimalFormat("#.#");

	private static LinkedHashMap<String, String> slots = new LinkedHashMap<String, String>();
	
	public static LinkedHashMap<String, String> slotsRemark = new LinkedHashMap<String, String>();
	private static final HashMap<Float, Integer> activityMap = new HashMap<>();

    static {
    	activityMap.put(1.2f, 200);
    	activityMap.put(1.375f, 300);
    	activityMap.put(1.55f, 400);
    	activityMap.put(1.7f, 600);
    }

	static {
		slots.put("0", "06:30");
		slots.put("1", "06:45");
		slots.put("2", "08:45");
		slots.put("3", "11:00");
		slots.put("4", "13:30");
		slots.put("5", "15:30");
		slots.put("6", "17:30");
		slots.put("7", "19:30");
		slots.put("8", "23:00");
		
		slotsRemark.put("0", "2 glasses of warm water at this time will help to clean your stomach and  improve digestion.");
		slotsRemark.put("1", "Fruits, especially apple or banana is the best choice at this time.");
		slotsRemark.put("2", "Change it in each day of week. Avoid milk or coffee or tea. Black coffe or tea or green tea is good.");
		slotsRemark.put("3", "Fruit ( if not taken early morning) or butterlmilk or coconut water or nuts are best choices.");
		slotsRemark.put("4", "Eating salad 15 minutes before lunch is must. Use whole wheat aatta. Use mustard, til or coconut oil for cooking.");
		//slotsRemark.put("5", "Take tea or coffee as per your liking. One tsp of sugar is fine.");
		slotsRemark.put("5", "Drinking green tea at this time will boost your metabolism.");
		//slotsRemark.put("6", "Nuts or makhane or dhokla or soup are good options. This meal is must to reduce dinner intake.");
		slotsRemark.put("6", "Makhane, Dhokla, Soup or sprouts are good options at this time.");
		slotsRemark.put("7", "Should be 50% of lunch. Avoid roti or rice. Fruits, chila, omlette, sprouts, are good options.");
		//slotsRemark.put("8", "Haldi milk is good for sound sleep which is foundation of weight loss.Also good for immunity.");
		slotsRemark.put("8", "Haldi milk is good for sound sleep n immunity.");
	}

	public static String getTimeForSlot(int slot) {

		return slots.get(slot + "");
	}

	public static String getDecimal(double input) {
		df2.setRoundingMode(RoundingMode.UP);
		return df2.format(input);
	}

	public static Stack<Integer> isCurrentForSlot() {
		Stack<Integer> possibleSlots = new Stack<Integer>();

		boolean flag = false;
		Iterator<Map.Entry<String, String>> it = slots.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, String> pair = it.next();
			flag = isCurrentForSlot(pair.getValue());
			if (flag) {
				String slotNum = pair.getKey();
				logger.info(slotNum);
				possibleSlots.add(Integer.parseInt(slotNum));
			}
		}
		return possibleSlots;
	}

	public static boolean isCurrentForSlot(String time) {
		Boolean flag = false;
		try {
			Date currentTime = getDateFromTime(new SimpleDateFormat("HH:mm").format(new Date()));
			Date slotTime = getDateFromTime(time);
			switch (time) {

			case "06:30":
				Date nextDate = getDateFromTime("06:45");
				flag = currentTime.before(slotTime) && nextDate.after(currentTime);
				if (flag) {
					return true;
				}
				break;

			case "06:45":
				nextDate = getDateFromTime("8:45");
				flag = currentTime.before(slotTime) && nextDate.after(currentTime);
				break;

			case "08:45":
				nextDate = getDateFromTime("11:00");
				flag = currentTime.before(slotTime) && nextDate.after(currentTime);
				break;

			case "11:00":
				nextDate = getDateFromTime("13:30");
				flag = currentTime.before(slotTime) && nextDate.after(currentTime);
				break;

			case "13:30":
				logger.info("time" + time);
				nextDate = getDateFromTime("15:30");
				flag = currentTime.before(slotTime) && nextDate.after(currentTime);
				break;

			case "15:30":
				logger.info("time" + time);
				nextDate = getDateFromTime("17:30");
				flag = currentTime.before(slotTime) && nextDate.after(currentTime);
				break;

			case "17:30":
				logger.info("time" + time);
				nextDate = getDateFromTime("19:30");
				flag = currentTime.before(slotTime) && nextDate.after(currentTime);

			case "19:30":
				logger.info("time" + currentTime);
				nextDate = getDateFromTime("23:00");
				flag = currentTime.before(slotTime) && nextDate.after(currentTime);
				return true;

			default:
				break;
			}

		} catch (Exception e) {

			e.printStackTrace();
		}
		return flag;
	}

	public static void main(String[] args) {

		logger.info(getCurrentDate("d.raghvendra@gmail.com", "[Q1W2E3R4T5Y6U7I]"));

	}

	private static Date getDateFromTime(String time) {
		String[] parts = time.split(":");
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
		calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
		calendar.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
		calendar.set(Calendar.SECOND, 0);
		return calendar.getTime();
	}

	public static int getSlotsForList(List<Integer> slots) {

		Stack<Integer> possibleSlots = isCurrentForSlot();
		int size = possibleSlots.size();
		logger.info("size" + size);
		for (int i = 0; i < size; i++) {
			int slot = possibleSlots.peek();
			for (int j = 0; j < slots.size(); j++) {
				if (slot == slots.get(j)) {
					return slot;
				}
			}
		}

		return 4;

	}

	public static CalculationResult calculatePercentage(CalculationData data, String traceId) {
		String method = "ApiUtils calculatePercentage() " + traceId;
		Double minCutOffCalories = 1200d;
		Double maxCutOffCalories = 1800d;
		CalculationResult result = new CalculationResult();
		int fiber = 35;
		int baseMulti = 5;

		Double suggestedWeight = data.getSuggestedWeight();
		logger.info("##### " + method + " WEIGHT           -->> " + data.getWeight());
		logger.info("##### " + method + " SUGGESTED WEIGHT -->> " + suggestedWeight);
		logger.info("##### " + method + " HEIGHT           -->> " + data.getHeight());
		logger.info("##### " + method + " AGE	           -->> " + data.getAge());
		logger.info("##### " + method + " ACTIVITIES	   -->> " + data.getActivities());
		if (!"G1".equalsIgnoreCase(data.getGender())) {
			baseMulti = -161;
			fiber = 25;
		}

		///////////////// OLD IMPLEMENTATION /////////////////
//		Double calories = (((((10 * data.getWeight()) + (6.25 * data.getHeight())) - (5 * data.getAge()) + baseMulti))
//				* data.getActivityUnit());
//
//		logger.info("##### " + method + " BEFORE CALORY FACTOR -->> " + calories);
//		double caloryFactor = 0.3d;
//		if (suggestedWeight > data.getWeight())
//			caloryFactor = 0.1;
//
//		logger.info("##### " + method + " CALORY FACTOR -->> " + caloryFactor);
//		Double targetCalories = calories - (calories * caloryFactor);

//		logger.info("##### " + method + " BEFORE CALORY FACTOR -->> " + calories);
//		double caloryFactor = 0.3d;
//		if (suggestedWeight > data.getWeight())
//			caloryFactor = 0.1;
//
//		logger.info("##### " + method + " CALORY FACTOR -->> " + caloryFactor);
//		Double targetCalories = calories - (calories * caloryFactor);

		///////////////// NEW IMPLEMENTATION /////////////////
		Double bmr = ((((10 * data.getWeight()) + (6.25 * data.getHeight())) - (5 * data.getAge()) + baseMulti));
		logger.info("##### " + method + " BMR -->> " + bmr);
//		Double calories = bmr + activityMap.get(data.getActivityUnit());
		Double calories = bmr + data.getActivities().get(data.getActivityUnit());
		logger.info("##### " + method + " CALORIES -->> " + calories);
		
//		int cal = 0;
//		if ("AC4".equalsIgnoreCase(data.getActivityCode()))
//			cal = 200;
		
//		logger.info("##### " + method + " CALORIESSS -->> " + calories);
		double calory = 500;
		if (suggestedWeight > data.getWeight())
			calory = 200;

		logger.info("##### " + method + " CALORY -->> " + calory);
		Double targetCalories = calories - calory;
		logger.info("##### " + method + " TARGET CALORIES (BEFORE) -->> " + targetCalories);

		if (targetCalories.intValue() < minCutOffCalories.intValue())
			targetCalories = minCutOffCalories;
		else if (targetCalories.intValue() > maxCutOffCalories.intValue())
			targetCalories = maxCutOffCalories;

		logger.info("##### " + method + " TARGET CALORIES (AFTER) -->> " + targetCalories);

		Double carb = (targetCalories * 45) / (4 * 100);
		Double protien = (targetCalories * 35) / (4 * 100);
		Double fat = (targetCalories * 20) / (9 * 100);
		result.setCarb(carb.intValue());
		result.setFat(fat.intValue());
		result.setFiber(fiber);
		result.setProtien(protien.intValue());
		result.setCalories(targetCalories.intValue());
		result.setBmr(bmr.intValue());
		result.setActivityCalories(data.getActivities().get(data.getActivityUnit()));

		return result;
	}

	public static String getCurrentDate(String email, String traceId) {
		String method = "ApiUtils getCurrentDate() " + traceId;
		logger.info("##### " + method + " EMAIL -->> " + email);
		Calendar calll = Calendar.getInstance(); // creates calendar
		calll.setTime(new Date()); // sets calendar time/date
		calll.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		calll.add(Calendar.MINUTE, 30); // add 30 minutes
		String d = new SimpleDateFormat("ddMMyyyy").format(calll.getTime());
		logger.info("##### " + method + " CURRENT DATE -->> " + d);
		return d;
	}
	
	public static String getPreviousDate() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.DATE, 1);
		calendar.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		calendar.add(Calendar.MINUTE, 30); // add 30 minutes

		String previousDate = new SimpleDateFormat("ddMMyyyy").format(calendar.getTime());
		logger.info("##### getPreviousDate() CURRENT DATE -->> " + previousDate);
		return previousDate;
	}
	
	public static String getTodaysDate() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.DATE, 0);
		calendar.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		calendar.add(Calendar.MINUTE, 30); // add 30 minutes

		String previousDate = new SimpleDateFormat("ddMMyyyy").format(calendar.getTime());
		logger.info("##### getPreviousDate() CURRENT DATE -->> " + previousDate);
		return previousDate;
	}
	
	public static String getCurrentDateTime(Integer validityInDays) {
		Calendar calll = Calendar.getInstance(); // creates calendar
		calll.setTime(new Date()); // sets calendar time/date
		calll.add(Calendar.DATE, validityInDays);
		calll.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		calll.add(Calendar.MINUTE, 30); // add 30 minutes
		String currentDateTime = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").format(calll.getTime());
		logger.info("##### getCurrentDateTime() CURRENT DATETIME -->> " + currentDateTime);
		return currentDateTime;
	}
	
	public static String getCurrentTime() {
		Calendar calll = Calendar.getInstance(); // creates calendar
		calll.setTime(new Date());
		calll.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		calll.add(Calendar.MINUTE, 30); // add 30 minutes
		String currentDateTime = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").format(calll.getTime());
		logger.info("##### getCurrentDateTime() CURRENT DATETIME -->> " + currentDateTime);
		return currentDateTime;
	}
	
	
	public static String getCouponExpiryDate(Integer validityInDays, int existingDays) {
		Calendar calendar = Calendar.getInstance();
		//calendar.add(Calendar.MONTH, validity);
		calendar.add(Calendar.DAY_OF_MONTH, (validityInDays + existingDays));
		SimpleDateFormat format1 = new SimpleDateFormat("ddMMyyyy");
		String couponExpiryDate = format1.format(calendar.getTime());
		logger.info("##### getCouponExpiryDate() COUPON EXPIRY DATE -->> " + couponExpiryDate);
		return couponExpiryDate;
	}
	
	public static String getFutureDate(Integer validityInDays) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.DAY_OF_MONTH, validityInDays);
		calendar.add(Calendar.HOUR_OF_DAY, 10); // adds ten hour
		calendar.add(Calendar.MINUTE, 30); // add 30 minutes
		SimpleDateFormat format1 = new SimpleDateFormat("ddMMyyyy");
		String futureDate = format1.format(calendar.getTime());
		return futureDate;
	}
	
	public static String getExpiryDate(Integer validityInDays) {
		Calendar calendar = Calendar.getInstance();
		//calendar.add(Calendar.MONTH, validity);
		calendar.add(Calendar.DAY_OF_MONTH, validityInDays);
		calendar.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		calendar.add(Calendar.MINUTE, 30); // add 30 minutes
		String couponExpiryDate = "";
		try {
			couponExpiryDate = new SimpleDateFormat("dd-MMM-yyyy")
					.format(new SimpleDateFormat("ddMMyyyy")
							.parse(new SimpleDateFormat("ddMMyyyy").format(calendar.getTime())));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.info("##### getCouponExpiryDate() COUPON EXPIRY DATE -->> " + couponExpiryDate);
		return couponExpiryDate;
	}
	
	public static String getCurrentDateInddMMMyyyyFormat(Integer validityInDays) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.DAY_OF_MONTH, -validityInDays);
		calendar.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		calendar.add(Calendar.MINUTE, 30); // add 30 minutes
		String date = new SimpleDateFormat("dd-MMM-yyyy").format(calendar.getTime());
		logger.info("##### getCurrentDateInddMMMyyyyFormat() DATE -->> " + date);
		return date;
	}
	
	public static String getFilteredDate(Integer noOfDays) {
		Calendar calendar = Calendar.getInstance();
		//calendar.add(Calendar.MONTH, validity);
		calendar.setTime(new Date());
		calendar.add(Calendar.DAY_OF_MONTH, -noOfDays);
		calendar.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		calendar.add(Calendar.MINUTE, 30); // add 30 minutes
		String filteredDate = "";
		try {
			filteredDate = new SimpleDateFormat("dd-MMM-yyyy")
					.format(new SimpleDateFormat("ddMMyyyy")
							.parse(new SimpleDateFormat("ddMMyyyy").format(calendar.getTime())));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		logger.info("##### getFilteredDate() FILTERED DATE -->> " + filteredDate);
		return filteredDate;
	}
	
	public static String getFilteredDateddMMyyyy(Integer noOfDays) {
		Calendar calendar = Calendar.getInstance();
		//calendar.add(Calendar.MONTH, validity);
		calendar.setTime(new Date());
		calendar.add(Calendar.DAY_OF_MONTH, -noOfDays);
		calendar.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		calendar.add(Calendar.MINUTE, 30); // add 30 minutes
		String filteredDate = "";
		filteredDate = new SimpleDateFormat("ddMMyyyy").format(calendar.getTime());
		logger.info("##### getFilteredDateddMMyyyy() FILTEREDDDMMYYYY DATE -->> " + filteredDate);
		return filteredDate;
	}
	
	public static String getFilteredFutureDateddMMyyyy(Integer noOfDays) {
		Calendar calendar = Calendar.getInstance();
		//calendar.add(Calendar.MONTH, validity);
		calendar.setTime(new Date());
		calendar.add(Calendar.DAY_OF_MONTH, noOfDays);
		calendar.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		calendar.add(Calendar.MINUTE, 30); // add 30 minutes
		String filteredDate = "";
		filteredDate = new SimpleDateFormat("ddMMyyyy").format(calendar.getTime());
		logger.info("##### getFilteredDateddMMyyyy() FILTEREDDDMMYYYY DATE -->> " + filteredDate);
		return filteredDate;
	}
	
	public static String getFilteredFutureDateddMMMyyyy(Integer noOfDays) {
		Calendar calendar = Calendar.getInstance();
		//calendar.add(Calendar.MONTH, validity);
		calendar.setTime(new Date());
		calendar.add(Calendar.DAY_OF_MONTH, noOfDays);
		calendar.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		calendar.add(Calendar.MINUTE, 30); // add 30 minutes
		String filteredDate = "";		
		try {
			filteredDate = new SimpleDateFormat("dd-MMM-yyyy")
					.format(new SimpleDateFormat("ddMMyyyy")
							.parse(new SimpleDateFormat("ddMMyyyy").format(calendar.getTime())));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
//		logger.info("##### getFilteredDateddMMyyyy() getFilteredFutureDateddMMyyyy DATE -->> " + filteredDate);
		return filteredDate;
	}
	
	public static String getDiscountedCouponExpiryDate(Integer validityInDays) {
		Calendar calendar = Calendar.getInstance();
		// calendar.add(Calendar.MONTH, validity);
		calendar.add(Calendar.DAY_OF_MONTH, validityInDays);
		calendar.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		calendar.add(Calendar.MINUTE, 30); // add 30 minutes
		
		String couponExpiryDate = "";
		try {
			couponExpiryDate = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss")
					.format(new SimpleDateFormat("ddMMyyyy HH:mm:ss")
							.parse(new SimpleDateFormat("ddMMyyyy HH:mm:ss").format(calendar.getTime())));
		} catch (ParseException e) {
			e.printStackTrace();
		}

		logger.info("##### ApiUtils() getCouponExpiryDate() COUPON EXPIRY DATE -->> " + couponExpiryDate);
		return couponExpiryDate;
	}
	
	public static String getTrialDate(Integer duration) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		calendar.add(Calendar.MINUTE, duration + 30); // add 30 minutes
		String filteredDate = "";
		try {
			filteredDate = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss")
					.format(new SimpleDateFormat("ddMMyyyy HH:mm:ss")
							.parse(new SimpleDateFormat("ddMMyyyy HH:mm:ss").format(calendar.getTime())));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		logger.info("##### getFilteredDate() FILTERED DATE -->> " + filteredDate);
		return filteredDate;
	}
	
	public static String getTodaysDateTime() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		calendar.add(Calendar.MINUTE, 30); // add 30 minutes
		String filteredDate = "";
		try {
			filteredDate = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss")
					.format(new SimpleDateFormat("ddMMyyyy HH:mm:ss")
							.parse(new SimpleDateFormat("ddMMyyyy HH:mm:ss").format(calendar.getTime())));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		logger.info("##### getFilteredDate() FILTERED DATE -->> " + filteredDate);
		return filteredDate;
	}
	
	public static String getSpecificDate(Integer validityInDays, String date) {
		String couponExpiryDate = "";
		try {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(new SimpleDateFormat("ddMMyyyy").parse(date));
			calendar.add(Calendar.DAY_OF_MONTH, validityInDays);
			calendar.add(Calendar.HOUR_OF_DAY, 10); // adds ten hours
			calendar.add(Calendar.MINUTE, 30); // add 30 minutes
			couponExpiryDate = new SimpleDateFormat("dd-MMM-yyyy")
					.format(new SimpleDateFormat("ddMMyyyy")
							.parse(new SimpleDateFormat("ddMMyyyy").format(calendar.getTime())));
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.info("##### getCouponExpiryDate() COUPON EXPIRY DATE -->> " + couponExpiryDate);
		return couponExpiryDate;
	}

	public static <T> LinkedList<T> convertALtoLL(List<T> aL) {
		return aL.stream().collect(Collectors.toCollection(LinkedList::new));
	}

	
   public static JsonObject getMealByCode(String code,List<JsonObject> datas) {
		for (JsonObject jsonObject : datas) {
			if(jsonObject.getString("code").equalsIgnoreCase(code)) {
				return jsonObject;
			}
		}
		return null;
	}
	
	public static List<JsonObject> setCustomerSelectionOnTop(Set<String> codes,List<JsonObject> categoryData,final List<JsonObject> data){
		String method = "ApiUtils setCustomerSelectionOnTop()";
		logger.info("##### " + method + " codes -->> " + Json.encodePrettily(codes));
		LinkedList<JsonObject> finalList=	convertALtoLL(categoryData);
		for (JsonObject object : categoryData) {
			if(codes.contains(object.getString("code"))) {
				JsonObject jsonObject=getMealByCode(object.getString("code"), data);
				logger.debug("##### " + method + " JSON OBJECT  FIRST -->> " + jsonObject.getString("itemCode"));
				if(jsonObject!=null) {
					finalList.remove();
					logger.debug("##### " + method + " JSON OBJECT LAST -->> " + jsonObject.getString("itemCode"));
					finalList.addFirst(jsonObject);
				}
			}
		}

		return categoryData;
	}
	
	
	
	public static String getCategoryForBMI(Double bmi) {
		
		if(bmi<16) {
			
			return "Severe Thinness";
		}else if(bmi>=16 && bmi<17) {
			return "Moderate Thinness";
		}else if(bmi>=17 && bmi<18.5) {
			return "Mild Thinness";
		}else if(bmi>=18.5 && bmi<25) {
			return "Normal";
		}else if(bmi>=25 && bmi<30) {
			return "Overweight";
		}else if(bmi>=30 && bmi<35) {
			return "Obese Class I";
		}else if(bmi>=35 && bmi<40) {
			return "Obese Class II";
		}else {
			return "Obese Class III";
		}
	}
	
	public static Integer getNumberOfDaysFromWeight(Double currentWeight,Double suggestedWeight) {
		
		Double substData = currentWeight * 1000 - suggestedWeight * 1000;
		
		if (currentWeight.doubleValue() > suggestedWeight.doubleValue()) {
			substData = suggestedWeight * 1000 - currentWeight * 1000;
		}
		
		Double percetOfWeight =  (currentWeight * 1000 / 100) * .1;
		
		Double numberDays = (substData / percetOfWeight);
		
		return numberDays.intValue();
		
	}
	
	public static Double getPlus5Pper(Double recomCal){
		Double fivePers=(recomCal * 5)/100;
		return recomCal+fivePers;
	}
	
	public static Double getMinus15Pper(Double recomCal){
		Double fivePers=(recomCal * 15)/100;
		return recomCal-fivePers;
	}
	
	public static Double getMinus5Pper(Double recomCal){
		Double fivePers=(recomCal * 5)/100;
		return recomCal-fivePers;
	}
	
	public static Double getMinus7point5Pper(Double recomCal){
		Double sevenpoint5Pers=(recomCal * 7.5)/100;
		return recomCal-sevenpoint5Pers;
	}
	
	 public  static String  getStatus(Double totalCal,Double recomCal) {
		 
		if(totalCal>getPlus5Pper(recomCal) && totalCal<getMinus15Pper(recomCal)) {
			return "N";
		} else if(totalCal>getPlus5Pper(recomCal)) {
			return "H";
		}else {
			return "L";
		}
	}
	 
	public static String getCaloriesStatus(String leverNo, Double totalCal, Double recomCal) {
		String method = "ApiUtils getCaloriesStatus() ";
		logger.info("##### " + method + leverNo + "       TOTAL CALCULATED CALORIES -->> " + totalCal);
		logger.info("##### " + method + leverNo + " 		   RECOMMENDED CALORIES -->> " + recomCal);
		logger.info("##### " + method + leverNo + " totalCal>getPlus5Pper(recomCal) -->> "
				+ (totalCal > getPlus5Pper(recomCal)));
		logger.info("##### " + method+ leverNo + "         	  getPlus5Pper(recomCal) ->> " + getPlus5Pper(recomCal));
		logger.info("##### " + method+ leverNo + "           getMinus5Pper(recomCal) ->> " + getMinus5Pper(recomCal));
		if ((totalCal > getPlus5Pper(recomCal) && totalCal < getMinus5Pper(recomCal))
				|| (totalCal <= getPlus5Pper(recomCal) && totalCal >= getMinus5Pper(recomCal))) {
			return "N";
		} else if (totalCal > getPlus5Pper(recomCal)) {
			return "H";
		} else if (totalCal < getMinus5Pper(recomCal)) {
			return "L";
		} else {
			return "L";
		}
	}
	
	public static String getChangedCaloriesStatus(String leverNo, Double totalCal, Double recomCal) {
		String method = "ApiUtils getCaloriesStatus() ";
		logger.info("##### " + method + leverNo + "       TOTAL CALCULATED CALORIES -->> " + totalCal);
		logger.info("##### " + method + leverNo + " 		   RECOMMENDED CALORIES -->> " + recomCal);
		logger.info("##### " + method + leverNo + " totalCal>getPlus5Pper(recomCal) -->> "
				+ (totalCal > getPlus5Pper(recomCal)));
		logger.info("##### " + method+ leverNo + "         	  getPlus5Pper(recomCal) ->> " + getPlus5Pper(recomCal));
		logger.info("##### " + method+ leverNo + "           getMinus5Pper(recomCal) ->> " + getMinus7point5Pper(recomCal));
		if ((totalCal > getPlus5Pper(recomCal) && totalCal < getMinus7point5Pper(recomCal))
				|| (totalCal <= getPlus5Pper(recomCal) && totalCal >= getMinus7point5Pper(recomCal))) {
			return "N";
		} else if (totalCal > getPlus5Pper(recomCal)) {
			return "H";
		} else if (totalCal < getMinus7point5Pper(recomCal)) {
			return "L";
		} else {
			return "L";
		}
	}
	 
	public static Double getTotalCalories(List<JsonObject> plans) {
		String method = "ApiUtils getTotalCalories() ";
		Double totalCalries = 0.0;
		for (JsonObject json : plans) {
			totalCalries += json.getDouble("Calories");
			logger.info("##### " + method + " ITEM CODE -->> " + json.getString("itemCode") + " --- CALORIES -->> "
					+ json.getDouble("Calories"));
		}

		// Double totalCalries=plans.stream().mapToDouble(mapper ->
		// mapper.getDouble("Calories")).sum();
		logger.info("##### " + method + " CALORIES SUM -->> " + totalCalries);
		return totalCalries;
	}
	
	public static Boolean checkFoodInList(List<JsonObject> prefList,int slot) {
		 
		return	!prefList.stream().filter(x -> getDietBySlot(x, slot)).collect(Collectors.toList()).isEmpty();
	
	}
	
	public static boolean getDietBySlot(JsonObject x,int slot) {
		JsonArray slots = x.getJsonArray("Slots");
		if (slots == null || slots.isEmpty()) {
			return false;
		}
		return slots.contains(slot);

	}
	
	public static void addFoodItem(JsonObject x,List<JsonObject> planlist) {
		for (JsonObject jsonObject : planlist) {
			if(jsonObject.getString("code").equalsIgnoreCase(x.getString("code"))) {
				return ;
			}
		}
		planlist.add(x);

	}
	
	public static void updateFoodItem(JsonObject x, int multiplier, List<JsonObject> planlist) {
		for (JsonObject jsonObject : planlist) {
			if(jsonObject.getString("code").equalsIgnoreCase(x.getString("code"))) {
				FoodFilterUtils.updateCalories(jsonObject, multiplier);
			}
		}
		planlist.add(x);

	}
	
	public static boolean isDateValidBetweenTodayAndGivenDate(String date) {
		boolean isDateValid = false;
		try {
			Date requestedDate = new SimpleDateFormat("ddMMyyyy").parse(date);
			long difference = requestedDate.getTime() - new Date().getTime();
			Long daysBetween = (difference / (1000 * 60 * 60 * 24)) + 1;
			if (daysBetween >= 0 && daysBetween <= 7)
				isDateValid = true;
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return isDateValid;
		
	}
}
