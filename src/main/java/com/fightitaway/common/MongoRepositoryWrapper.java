package com.fightitaway.common;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.lang3.RandomStringUtils;
import org.bson.Document;

import com.fightitaway.api.ApiUtils;
import com.fightitaway.api.FoodFilterUtils;
import com.fightitaway.service.CalculationData;
import com.fightitaway.service.CalculationResult;
import com.fightitaway.service.CancelRequest;
import com.fightitaway.service.ConfirmPaymentRequest;
import com.fightitaway.service.CreatCustomerDietPlanRequest;
import com.fightitaway.service.CreatCustomerHabitPlanRequest;
import com.fightitaway.service.CreateOrder;
import com.fightitaway.service.CustDietPreference;
import com.fightitaway.service.CustProfile;
import com.fightitaway.service.Customer;
import com.fightitaway.service.DBResult;
import com.fightitaway.service.DietItems;
import com.fightitaway.service.DiscountedCouponDetails;
import com.fightitaway.service.FilterData;
import com.fightitaway.service.FoodDetail;
import com.fightitaway.service.HabitData;
import com.fightitaway.service.Profile;
import com.fightitaway.service.RefundRequest;
import com.fightitaway.service.SelectiveItems;
import com.fightitaway.service.SlotFilter;
import com.fightitaway.service.SubscribeRequest;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import com.opencsv.CSVWriter;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.BulkOperation;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.mongo.MongoClient;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


/**
 * Applications connectivity via mongodb.
 * 
 * @author Smartdietplanner
 * @since 08-Aug-2020
 *
 */
public class MongoRepositoryWrapper {

	int fiber = 35;
	List<String> prefCodeList = new ArrayList<String>();

	protected org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MongoRepositoryWrapper.class);

	protected final MongoClient client;

	protected final Vertx vertx;

	protected final JsonObject config;

	private static Map<Integer, String> map = new HashMap<>();

	static {
		map.put(0, "D/DM");
		map.put(1, "F");
		map.put(2, "W/WC/WP/WPP/WE/WCP");
		map.put(3, "W,D/DM");
		map.put(4, "A/C,B");
		map.put(5, "D");
		map.put(6, "W/D");
		map.put(7, "W/WC/WCP/WE/F/C");
		map.put(8, "D,DM");
	}

	/**
	 * Constructor.
	 * 
	 * @param vertx
	 * @param config
	 */
	public MongoRepositoryWrapper(Vertx vertx, JsonObject config) {
		JsonObject mongoConf = config.getJsonObject("mongo");
		this.client = MongoClient.createShared(vertx, mongoConf);
		this.vertx = vertx;
		this.config = config;
	}

	/**
	 * Create customer profile.
	 * 
	 * @param document
	 * @param request
	 * @param traceId
	 * @return Future<String>
	 */
	public Future<String> createDocument(String document, JsonObject request, String traceId) {
		String method = "MongoRepositoryWrapper createDocument() " + traceId + "-[" + request.getString("_id") + "]";
		Promise<String> promise = Promise.promise();
		logger.info("##### " + method + " CUSTOMER PROFILE REQUEST -->> " + request);
		client.rxSave(document, request).subscribe(res -> {
			logger.info("##### " + method + " CUSTOMER PROFILE CREATED");
			promise.complete(res);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get customer profiles.
	 * 
	 * @param collection
	 * @param query
	 * @param traceId
	 * @return Future<List<JsonObject>>
	 */
	public Future<List<JsonObject>> getDocuments(String collection, JsonObject query, String traceId) {
		String method = "MongoRepositoryWrapper getDocuments() " + traceId;
		Promise<List<JsonObject>> promise = Promise.promise();
		client.rxFind(collection, query).subscribe(res -> {
			promise.complete(res);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Get one.
	 * 
	 * @param document
	 * @param fieldName
	 * @param value
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getOne(String document, String fieldName, String value, String traceId) {
		String method = "MongoRepositoryWrapper getOne() " + traceId + "-[" + value + "]";
		logger.info("##### " + method + " VALUE -->> " + value);
		Promise<JsonObject> promise = Promise.promise();
		if (null == value || "".equalsIgnoreCase(value) || "null".equalsIgnoreCase(value)) {
			promise.complete(new JsonObject());
		} else {
			JsonObject query = new JsonObject().put("_id", value);
			logger.info("##### " + method + " QUERY -->> " + query);
			client.rxFindOne(document, query, null).subscribe(res -> {

				if (res == null || res.isEmpty())
					promise.fail("No Record Found");
				else
					promise.complete(res);
			}, (ex) -> {
				logger.error("##### " + method + " DATA -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		}
		return promise.future();
	}

	/**
	 * Get One plan.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getOnePlan(String email, String traceId) {
		String method = "MongoRepositoryWrapper getOnePlan() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);
		logger.info("##### " + method + " QUERY " + query);
		JsonObject query1 = new JsonObject().put("emailId", email);
		client.rxFindOne("PLAN_SUBCRIPTION_DETAIL", query1, null).subscribe(res1 -> {
			JsonObject profile = new JsonObject();
			profile.put("code", "0001");
			profile.put("message", "No Subscription Plan Found for [" + email + "]");
			if (res1 != null && !res1.isEmpty()) {
				try {
					profile.put("code", "0000");
					profile.put("message", "Subscription Plan Found for [" + email + "]");
					boolean isUpgraded = false;
					if (res1.getBoolean("isUpgraded", false)) {
						isUpgraded = true;
					}
					Calendar calendarr1 = Calendar.getInstance();
					calendarr1.setTime(new Date());
					calendarr1.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
					calendarr1.add(Calendar.MINUTE, 30); // add 30 minutes
					Calendar calendarr2 = Calendar.getInstance();
					DateFormat dateFormatISO = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
					Date createdDate = dateFormatISO.parse(res1.getString("createdDate"));
					calendarr2.setTime(createdDate);
					calendarr2.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
					calendarr2.add(Calendar.MINUTE, 30); // add 30 minutes
					Date currentDate = calendarr1.getTime();
					Date dateTillPlanCanBeCancelled = calendarr2.getTime();
					@SuppressWarnings("unused")
					boolean isDateValidForCancelPlan = false;
					boolean isRefundApplicable = true;
					if (!isUpgraded) {
						isRefundApplicable = false;
						calendarr2.add(Calendar.DATE, 0);
						if (dateTillPlanCanBeCancelled.after(currentDate))
							isDateValidForCancelPlan = true;
					} else {
						isRefundApplicable = false;
					}

					// CHECK IF PLAN ACTIVE
					profile.put("isPlanActive",
							new Date().before(new SimpleDateFormat("dd-MMM-yyyy").parse(res1.getString("expiryDate"))));
					profile.put("planExpiryDate", new SimpleDateFormat("dd-MMM-yyyy")
							.format((new SimpleDateFormat("dd-MMM-yyyy").parse(res1.getString("expiryDate")))));
					profile.put("planCouponCode", res1.getString("couponCode", "CU5000"));
//					profile.put("isDateValidForCancelPlan", isDateValidForCancelPlan);
//					if (isDateValidForCancelPlan)
//						profile.put("planCancelledDateTill",
//								new SimpleDateFormat("dd-MMM-yyyy").format(dateTillPlanCanBeCancelled));
					profile.put("isPlanUpgraded", res1.getBoolean("isUpgraded", false));
					profile.put("planUpgradedDate", res1.getString("upgradedDate", ""));
					profile.put("isRefundApplicable", isRefundApplicable);
					profile.put("amount", res1.getInteger("amount", 0));
					if (isRefundApplicable)
						profile.put("refundedAmount", 0);
				} catch (ParseException e) {
					logger.error("##### " + method + " EXCEPTION -->> " + e.getMessage());
					e.printStackTrace();
				}
			}

			promise.complete(profile);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Update customer profile.
	 * 
	 * @param emailId
	 * @param profile
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> updateProfile(String emailId, JsonObject profile, String traceId) {
		String method = "MongoRepositoryWrapper updateProfile() " + traceId + "-[" + emailId + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", emailId);
		JsonObject response = new JsonObject();
		JsonObject fields = new JsonObject().put("profile.name", "profile.name").put("profile.createdDate",
				"profile.createdDate");
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFindOne("CUST_PROFILE", query, fields).subscribe(res -> {
			JsonObject payload = new JsonObject();
			payload.put("emailId", emailId);
			payload.put("loginDateTime", ApiUtils.getCurrentDateTime(0));
			client.rxSave("CUST_LOGIN_DATETIME", payload).subscribe(res2 -> {
				String createdDate = res.getJsonObject("profile").getString("createdDate");
				String name = res.getJsonObject("profile").getString("name");
				logger.info("##### " + method + " CREATED DATE -->> " + createdDate);
				logger.info("##### " + method + "         NAME -->> " + name);
				if (null == createdDate || "".equals(createdDate)) {
					Calendar cal = Calendar.getInstance(); // creates calendar
					cal.setTime(new Date()); // sets calendar time/date
					cal.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
					cal.add(Calendar.MINUTE, 30); // add 30 minutes
					createdDate = new SimpleDateFormat("dd-MMM-yyyy").format(cal.getTime());
					logger.info("##### " + method + " CREATED DATE (AFTER) -->> " + createdDate);
				}

				response.put("code", "0000");
				response.put("message", "success");

				promise.complete(response);
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		}, (ex) -> {
			logger.error("##### " + method + " ERROR 1 -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get customer default profile.
	 * 
	 * @param email
	 * @param jsonObject
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getDefaultProfile(String email, JsonObject jsonObject, String traceId) {
		String method = "MongoRepositoryWrapper getDefaultProfile() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		try {
			JsonObject query = new JsonObject();
			query.put("_id", "v1");
			logger.info("##### " + method + " QUERY -->> " + query);
			client.rxFindOne("DEFAUL_PROFILE", query, null).subscribe(res -> {
				JsonObject otherMaster = res.getJsonObject("otherMaster");
				otherMaster.put("community", jsonObject.getJsonArray("community"));
				otherMaster.put("diseases", jsonObject.getJsonArray("diseases"));
				res.put("Master", jsonObject.getJsonObject("dietPref"));
				res.put("portions", jsonObject.getJsonArray("portions"));
				promise.complete(res);
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		} catch (Exception ex) {
			logger.error("##### " + method + " EXCEPTION -->> " + ex.getMessage());
		}

		return promise.future();
	}

	/**
	 * Update customer demographic.
	 * 
	 * @param key
	 * @param email
	 * @param request
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> updateByKey(String key, String email, JsonObject request, String traceId) {
		String method = "MongoRepositoryWrapper  updateByKey() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);
		JsonObject response = new JsonObject();
		JsonObject update = new JsonObject().put("$set", new JsonObject().put(key, request));
		logger.info("##### " + method + " UPDATE DEMOGRAPHIC QUERY  -->> " + query);
		logger.info("##### " + method + " UPDATE DEMOGRAPHIC UPDATE -->> " + update);
		client.rxUpdateCollection("CUST_PROFILE", query, update).subscribe(res -> {
			response.put("code", "0000").put("message", "success");
			promise.complete(response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Update customer diet.
	 * 
	 * @param email
	 * @param request
	 * @param data
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> updateDiet(String email, JsonObject request, CalculationData data, String traceId) {
		String method = "MongoRepositoryWrapper  updateDiet() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject response = new JsonObject();
		Double suggestedWeight = data.getSuggestedWeight();
		Double currentWeight = data.getWeight();

		Integer numberDays = ApiUtils.getNumberOfDaysFromWeight(currentWeight, suggestedWeight);
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
		logger.info("##### " + method + " UPDATE DIET NO OF DAYS -->> " + numberDays);
		if (data.getPlanTakenDate() == null) {
			calendar.add(Calendar.DATE, Math.abs(numberDays));
		} else {
			calendar.setTime(new Date());
			calendar.add(Calendar.DATE, Math.abs(numberDays));
		}

		request.put("dateBy", dateFormat.format(calendar.getTime()));
		request.put("suggestedWeight", suggestedWeight);
		request.put("updatedDate", ApiUtils.getCurrentDateInddMMMyyyyFormat(0));
		request.put("updatedDateTime", ApiUtils.getCurrentTime());
		response.put("dateBy", dateFormat.format(calendar.getTime()));
		response.put("suggestedWeight", suggestedWeight);
		response.put("updatedDate", request.getString("updatedDate"));
		response.put("updatedDateTime", request.getString("updatedDateTime"));

		logger.info("##### " + method + " DATEBY (AFTER FORMAT)    -->> " + dateFormat.format(calendar.getTime()));
		logger.info("##### " + method + " UPDATE DIET REQUEST -->> " + request);

		JsonObject query = new JsonObject().put("_id", email);
		JsonObject update = new JsonObject().put("$set", new JsonObject().put("diet", request));
		logger.info("##### " + method + "  UPDATE DIET QUERY -->> " + query);
		logger.info("##### " + method + " UPDATE DIET UPDATE -->> " + update);
		client.rxUpdateCollection("CUST_PROFILE", query, update).subscribe(res -> {
			logger.info("##### " + method + " UPDATE DIET SUCCESSFULLY UPDATED -->> " + res);
			response.put("code", "0000").put("message", "success");
			promise.complete(response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			response.put("code", "0001").put("message", ex.getMessage());
			promise.complete(response);
		});

		return promise.future();
	}

	/**
	 * Update lifestyle.
	 * 
	 * @param email
	 * @param request
	 * @param data
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> updateLifeStyle(String email, JsonObject request, CalculationData data, String traceId) {
		String method = "MongoRepositoryWrapper updateLifeStyle() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();

		JsonObject query = new JsonObject().put("_id", email);
		logger.info("##### " + method + " UPDATE LIFESTYLE QUERY -->> " + query);
		JsonObject response = new JsonObject();

		String activityCode = request.getJsonObject("activities").getString("code");
		Float activity = request.getJsonObject("activities").getFloat("data");
		logger.info("##### " + method + " ACTIVITY -->> " + activity);

		data.setActivityUnit(activity);
		data.setActivityCode(activityCode);
		CalculationResult result = ApiUtils.calculatePercentage(data, traceId);

		request.put("carb", result.getCarb());
		request.put("protien", result.getProtien());
		request.put("fat", result.getFat());
		request.put("fiber", result.getFiber());
		request.put("calories", result.getCalories().intValue());
		request.put("bmr", result.getBmr().intValue());
		request.put("activityCalories", result.getActivityCalories());
		request.put("updatedDate", ApiUtils.getCurrentDateInddMMMyyyyFormat(0));
		request.put("updatedDateTime", ApiUtils.getCurrentTime());

		JsonObject wakeupObj = request.getJsonObject("wakeup");
		wakeupObj.put("code", wakeupObj.getString("code"));
		request.put("wakeup", wakeupObj);
		logger.info("##### " + method + " WAKEUP OBJECT -->> " + request.getJsonObject("wakeup"));

		JsonObject leaveForOfficeObj = request.getJsonObject("leaveForOffice");
		leaveForOfficeObj.put("code", leaveForOfficeObj.getString("code"));
		leaveForOfficeObj.put("value", leaveForOfficeObj.getString("value"));
		request.put("leaveForOffice", leaveForOfficeObj);
		logger.info("##### " + method + " LEAVEFOROFFICE OBJECT -->> " + request.getJsonObject("leaveForOffice"));

		if (request.getJsonArray("communities").contains("J")) {
			request.getJsonArray("communities").remove("J");
			request.getJsonArray("communities").add("P");
			logger.info("##### " + method + " COMMUNITY 'J' REMOVED AND ADDED 'P' -->> "
					+ request.getJsonArray("communities"));
		}

		request.put("communities", request.getJsonArray("communities"));
		request.put("diseases", request.getJsonArray("diseases"));
		logger.info("##### " + method + " COMMUNITIES OBJECT -->> " + request.getJsonArray("communities"));
		logger.info("##### " + method + " DISEASES OBJECT    -->> " + request.getJsonArray("diseases"));

		JsonObject update = new JsonObject().put("$set", new JsonObject().put("lifeStyle", request));
		logger.info("##### " + method + " LIFESTYLE QUERY   -->> " + query);
		logger.info("##### " + method + " LIFESTYLE PAYLOAD -->> " + update);
		client.rxUpdateCollection("CUST_PROFILE", query, update).subscribe(res -> {
			response.put("code", "0000");
			response.put("message", "Success");
			response.put("carb", result.getCarb());
			response.put("protien", result.getProtien());
			response.put("fat", result.getFat());
			response.put("fiber", result.getFiber());
			response.put("calories", result.getCalories().intValue());
			response.put("wakeup", request.getJsonObject("wakeup"));
			response.put("leaveForOffice", request.getJsonObject("leaveForOffice"));
			response.put("communities", request.getJsonArray("communities"));
			response.put("diseases", request.getJsonArray("diseases"));
			promise.complete(response);

		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get data for calculation.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getDataForCalculation(String email, String traceId) {
		String method = "MongoRepositoryWrapper getDataForCalculation() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);
		logger.info("##### " + method + " QUERY -->> " + query);
		JsonObject fileds = new JsonObject().put("email", "email").put("demographic.gender", "demographic.gender")
				.put("demographic.height", "demographic.height").put("demographic.weight", "demographic.weight")
				.put("demographic.age", "demographic.age").put("lifeStyle.activities", "lifeStyle.activities");
		client.rxFindOne("CUST_PROFILE", query, fileds).subscribe(res -> {
			JsonObject response = new JsonObject();
			response.put("gender", res.getJsonObject("demographic").getJsonObject("gender").getString("code"));
			response.put("height", res.getJsonObject("demographic").getJsonObject("height").getInteger("value"));
			response.put("weight", res.getJsonObject("demographic").getJsonObject("weight").getInteger("value"));
			response.put("age", res.getJsonObject("demographic").getJsonObject("age").getInteger("avg_age"));
			response.put("activityUnit", res.getJsonObject("lifeStyle").getJsonObject("activities").getFloat("data"));
			promise.complete(response);
			logger.info("##### " + method + " RESPONSE -->> " + response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get calculation data for lifestyle.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<CalculationData> getCalcDataForLifeStyle(String email, String traceId) {
		String method = "MongoRepositoryWrapper getCalcDataForLifeStyle() " + traceId + "-[" + email + "]";
		CalculationData data = new CalculationData();
		Promise<CalculationData> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);
		JsonObject fields = new JsonObject().put("email", "email").put("demographic.gender", "demographic.gender")
				.put("demographic.height", "demographic.height").put("demographic.weight", "demographic.weight")
				.put("demographic.suggestedWeight", "demographic.suggestedWeight")
				.put("demographic.age", "demographic.age");
		logger.info("##### " + method + " QUERY  -->> " + query);
		logger.info("##### " + method + " FIELDS -->> " + fields);
		client.rxFindOne("CUST_PROFILE", query, fields).subscribe(res -> {
			try {
				data.setAge(res.getJsonObject("demographic").getJsonObject("age").getInteger("avg_age"));
				data.setGender(res.getJsonObject("demographic").getJsonObject("gender").getString("code"));
				data.setHeight(res.getJsonObject("demographic").getJsonObject("height").getDouble("value"));
				data.setWeight(res.getJsonObject("demographic").getJsonObject("weight").getDouble("value"));
				data.setSuggestedWeight(res.getJsonObject("demographic").getDouble("suggestedWeight"));
				logger.info("##### " + method + " RESPONSE -->> " + data.toString());
			} catch (Exception e) {
				logger.error("##### " + method + " EXCEPTION -->> " + e.getMessage());
				e.printStackTrace();
			}
			promise.complete(data);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->>" + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get calculation data for diet.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<CalculationData>
	 */
	public Future<CalculationData> getCalcDataForDiet(String email, String traceId) {
		String method = "MongoRepositoryWrapper getCalcDataForDiet() " + traceId + "-[" + email + "]";
		CalculationData data = new CalculationData();
		Promise<CalculationData> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);
		JsonObject fileds = new JsonObject().put("email", "email").put("demographic.gender", "demographic.gender")
				.put("demographic.height", "demographic.height").put("demographic.weight", "demographic.weight")
				.put("demographic.age", "demographic.age")
				.put("demographic.suggestedWeight", "demographic.suggestedWeight");
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFindOne("CUST_PROFILE", query, fileds).subscribe(res -> {
			logger.debug("##### " + method + " RES -->> " + res.toString());
			try {
				data.setAge(res.getJsonObject("demographic").getJsonObject("age").getInteger("avg_age"));
				data.setGender(res.getJsonObject("demographic").getJsonObject("gender").getString("code"));
				data.setHeight(res.getJsonObject("demographic").getJsonObject("height").getDouble("value"));
				data.setWeight(res.getJsonObject("demographic").getJsonObject("weight").getDouble("value"));
				data.setSuggestedWeight(res.getJsonObject("demographic").getDouble("suggestedWeight"));
				logger.info("##### " + method + " RESPONSE -->> " + data.toString());
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("##### " + method + " EXCEPTION -->> " + e.getMessage());
			}
			promise.complete(data);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get plan activation date.
	 * 
	 * @param email
	 * @param data
	 * @param traceId
	 * @return Future<CalculationData>
	 */
	public Future<CalculationData> getPlanActivatedDate(String email, CalculationData data, String traceId) {
		String method = "MongoRepositoryWrapper getPlanActivatedDate() " + traceId + "-[" + email + "]";
		Promise<CalculationData> promise = Promise.promise();
		DateFormat dateFormat = new SimpleDateFormat("ddMMyyyy");
		JsonObject query = new JsonObject();
		query.put("emailId", email);
		query.put("isActive", true);
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFindOne("PLAN_SUBCRIPTION_DETAIL", query, null).subscribe(res -> {
			if (res != null && !res.isEmpty()) {
				try {
					String date = res.getString("expiryDate");
					data.setPlanTakenDate(dateFormat.parse(date));
					logger.info("##### " + method + " EXPIRY DATE -->> " + date);
				} catch (ParseException e) {
					e.printStackTrace();
				}
			} else {
				data.setPlanTakenDate(null);
			}
			promise.complete(data);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get diet list for food.
	 * 
	 * @param email
	 * @param filterData
	 * @param traceId
	 * @return Future<DBResult>
	 */
	public Future<DBResult> getDietListForFood(String email, FilterData filterData, String traceId) {
		String method = "MongoRepositoryWrapper getDietListForFood() " + traceId + "-[" + email + "]";
		DBResult dbResult = new DBResult();
		dbResult.setFilterData(filterData);
		Promise<DBResult> promise = Promise.promise();
		JsonObject query = new JsonObject();
		JsonArray orFiels = new JsonArray();
		if ("NV".equalsIgnoreCase(filterData.getFoodType())) {
			orFiels.add(new JsonObject().put("foodType", "NV"));
			orFiels.add(new JsonObject().put("foodType", "E"));
			orFiels.add(new JsonObject().put("foodType", "V"));
		} else if ("E".equalsIgnoreCase(filterData.getFoodType())) {
			orFiels.add(new JsonObject().put("foodType", "E"));
			orFiels.add(new JsonObject().put("foodType", "V"));
		} else {
			orFiels.add(new JsonObject().put("foodType", "V"));
		}

		query.put("$or", orFiels);

		logger.info("##### " + method + " COMMUNITIES -->> " + filterData.getCommunity());
		logger.info("##### " + method + " QUERY       -->> " + query);
		client.rxFind("DIET_PLAN", query).map(map -> {
			map.forEach(action -> {
				action.put("imageUrl", config.getString("imageBaseUrl") + "/" + action.getString("code") + ".png");
				action.put("category", action.getString("Type"));
			});
			return map;
		}).subscribe(res -> {
			try {
				List<JsonObject> dietplanList = new ArrayList<>();
				if (null != res && !res.isEmpty()) {
					dietplanList = filterListByCommunity(res, filterData.getCommunity(), traceId);
					dietplanList = getSlotDiets(dietplanList, dbResult, traceId);
				}

				// dbResult.setData(res);
				// dbResult.setMasterPlan(res);
				dbResult.setData(dietplanList);
				dbResult.setMasterPlan(dietplanList);
			} catch (Exception ex) {
				logger.error("##### " + method + "  EXCEPTION -->> " + ex.getMessage());
			}

			promise.complete(dbResult);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		Future<DBResult> test = promise.future();
		return test;
	}

	/**
	 * Get diet list refresh food.
	 * 
	 * @param email
	 * @param filterData
	 * @param traceId
	 * @return Future<DBResult>
	 */
	public Future<DBResult> getDietListRefreshFood(String email, FilterData filterData, String traceId) {
		String method = "MongoRepositoryWrapper getDietListRefreshFood() " + traceId + "-[" + email + "]";
		DBResult dbResult = new DBResult();
		dbResult.setFilterData(filterData);
		Promise<DBResult> promise = Promise.promise();
		JsonObject query = new JsonObject();
		JsonArray orFiels = new JsonArray();
		if ("NV".equalsIgnoreCase(filterData.getFoodType())) {
			orFiels.add(new JsonObject().put("foodType", "NV"));
			orFiels.add(new JsonObject().put("foodType", "E"));
			orFiels.add(new JsonObject().put("foodType", "V"));
		} else if ("E".equalsIgnoreCase(filterData.getFoodType())) {
			orFiels.add(new JsonObject().put("foodType", "E"));
			orFiels.add(new JsonObject().put("foodType", "V"));
		} else {
			orFiels.add(new JsonObject().put("foodType", "V"));
		}

		query.put("$or", orFiels);
		client.rxFind("DIET_PLAN", query).map(map -> {
			map.forEach(action -> {
				action.put("imageUrl", config.getString("imageBaseUrl") + "/" + action.getString("code") + ".png");
				action.put("category", action.getString("Type"));
				if (null != filterData.getDisease() && filterData.getDisease().size() > 0) {
					JsonArray jsonArr = action.getJsonArray("RecommendedIn");
					List<String> listAll = new ArrayList<>();
					if (null != action && null != jsonArr && !jsonArr.isEmpty() && jsonArr.size() > 0) {
						JsonArray jArr = new JsonArray();
						jsonArr.forEach(obj -> {
							if (null != obj) {
								String food = (String) obj;
								if (filterData.getDisease().contains(food) && !jArr.contains(food)) {
									listAll.add(food);
									jArr.add(food);
									action.put("recommendedFor", jArr);
								}
							}
						});
					}
				}
			});
			return map;
		}).subscribe(res -> {
			try {
				dbResult.setData(res);
				dbResult.setMasterPlan(res);
			} catch (Exception ex) {
				logger.error("##### " + method + " EXCEPTION -->> " + ex.getMessage());
			}

			promise.complete(dbResult);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		Future<DBResult> test = promise.future();
		return test;
	}

	/**
	 * Get diets for option.
	 * 
	 * @param dbResult
	 * @param slot
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getDietsForOption(DBResult dbResult, Integer slot, String traceId) {
		String method = "MongoRepositoryWrapper getDietsForOption() " + traceId;
		logger.info("##### " + method + " SLOT -->> " + slot);
		logger.info("##### " + method + " FILTER DATA -->> "
				+ ((null != dbResult && null != dbResult.getFilterData()) ? dbResult.getFilterData().toString()
						: null));
		JsonObject suggestedPlan = new JsonObject();
		Promise<JsonObject> promise = Promise.promise();

		// filter slots related data only
		List<JsonObject> data = dbResult.getData().stream().filter(x -> getDietBySlot(x, slot))
				.collect(Collectors.toList());
		logger.info("##### " + method + " OPTION DATA SIZE (BEFORE) -->> " + data.size());
		// filter data having not related diseases and respective community
		data = data.stream().filter(x -> filterAvoidIn(x, dbResult.getFilterData().getDisease()))
				.filter(x -> filterByCustCommunity(x, dbResult.getFilterData().getCommunity()))
				.filter(x -> filterByDietSeason(x)).collect(Collectors.toList());
		logger.info("##### " + method + " OPTION DATA SIZE  (AFTER) -->> " + data.size());

		List<JsonObject> mealOptions = new ArrayList<JsonObject>();
		List<JsonObject> bedtimeOptions = new ArrayList<JsonObject>();
		Map<String, List<JsonObject>> breakfastMap = new LinkedHashMap<String, List<JsonObject>>();
		Map<String, List<JsonObject>> breakfastMap1 = new LinkedHashMap<String, List<JsonObject>>();
		Map<String, List<JsonObject>> breakfastMap4 = new LinkedHashMap<String, List<JsonObject>>();
		List<JsonObject> categories = new ArrayList<JsonObject>();
		List<JsonObject> categories1 = new ArrayList<JsonObject>();
		List<JsonObject> categories2 = new ArrayList<JsonObject>();
		switch (slot) {

		case 0:

			List<JsonObject> wakeupListD = data.stream().filter(x -> {
				if (x.getString("Type").equalsIgnoreCase("D") || x.getString("Type").equalsIgnoreCase("DM")
						|| getSnacks(x)) {
					return true;
				} else {
					return false;
				}
			}).collect(Collectors.toList());

			List<JsonObject> wakeupListA = data.stream().filter(x -> {
				if (x.getString("Type").equalsIgnoreCase("A")) {
					return true;
				} else {
					return false;
				}
			}).collect(Collectors.toList());

			wakeupListA = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), wakeupListA,
					data);
			wakeupListA = wakeupListA.stream().filter(x -> filterByDietSeason(x)).collect(Collectors.toList());
//			wakeupListA = getFilteredListByOptions1(wakeupListA, dbResult, dbResult.getFilterData().getDishes(),
//					"WAKEUPLISTA", 0);
			wakeupListA = getSlotOptions(wakeupListA, dbResult, dbResult.getFilterData().getDishes(), "WAKEUPLISTA", 0,
					traceId);

			wakeupListD = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), wakeupListD,
					data);
			wakeupListD = wakeupListD.stream().filter(x -> filterByDietSeason(x)).collect(Collectors.toList());
//			wakeupListD = getFilteredListByOptions1(wakeupListD, dbResult, dbResult.getFilterData().getSnacks(),
//					"WAKEUPLISTD", 0);
			wakeupListD = getSlotOptions(wakeupListD, dbResult, dbResult.getFilterData().getSnacks(), "wakeupListD", 0,
					traceId);
//			wakeupListD = getFinalFilteredListByOptions1(data, wakeupListD, dbResult, dbResult.getFilterData().getSnacks(),
//					"WAKEUPLISTD", 0);

			breakfastMap.put("Drink", wakeupListD);
			if (dbResult.getFilterData().getDisease() != null && !dbResult.getFilterData().getDisease().isEmpty()) {
				breakfastMap.put("One", wakeupListA);

			}

			JsonObject wakeupOption = new JsonObject();
			wakeupOption.put("optionId", 1);
			wakeupOption.put("isMandatory", true);
			wakeupOption.put("optionName", "Wake up Option");
			wakeupOption.put("isCategory", true);

			for (Map.Entry<String, List<JsonObject>> entry : breakfastMap.entrySet()) {
				JsonObject category = new JsonObject();
				category.put("name", entry.getKey());
				category.put("food", entry.getValue());
				categories.add(category);
			}
			wakeupOption.put("categories", categories);
			mealOptions.add(wakeupOption);
			suggestedPlan.put("mealOptions", mealOptions);

			break;

		case 1:

			List<JsonObject> fruits = data.stream().filter(this::getALlFruits).collect(Collectors.toList());
			fruits = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), fruits, data);
			// fruits = getFilteredListByOptions1(fruits, dbResult,
			// dbResult.getFilterData().getFruits(), "FRUITS)", 1);
//			fruits = getFinalFilteredListByOptions1(data, fruits, dbResult, dbResult.getFilterData().getFruits(),
//					"FRUITS", 1);
			fruits = getSlotOptions(fruits, dbResult, dbResult.getFilterData().getFruits(), "FRUITS", 1, traceId);
			JsonObject fruitOption = new JsonObject();
			fruitOption.put("optionId", 1);
			fruitOption.put("isMandatory", true);
			fruitOption.put("optionName", "Fruits");
			fruitOption.put("isCategory", true);
			// fruitOption.put("food", fruits);

			Map<String, List<JsonObject>> fruitsMap = new LinkedHashMap<String, List<JsonObject>>();
			fruitsMap.put("Fruits", fruits);

			for (Map.Entry<String, List<JsonObject>> entry : fruitsMap.entrySet()) {
				JsonObject category = new JsonObject();
				category.put("name", entry.getKey());
				category.put("food", entry.getValue());
				categories.add(category);
			}
			fruitOption.put("categories", categories);
			mealOptions.add(fruitOption);

			// TEA/COFFEE
			List<JsonObject> teaCoffeList = data.stream().filter(x -> this.getDrinks(x)).collect(Collectors.toList());
			teaCoffeList = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), teaCoffeList,
					data);

			// OTHERS
			List<JsonObject> othersList = data.stream().filter(x -> this.getSnacks(x)).collect(Collectors.toList());
			othersList = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), othersList, data);

			// teaCoffeList = getFilteredListByOptions(teaCoffeList, dbResult,
			// "TEACOFFELIST", 1);

			JsonObject teaCoffe = new JsonObject();
			teaCoffe.put("optionId", 2);
			teaCoffe.put("isMandatory", true);
			// teaCoffe.put("optionName", "Tea/Coffe");
			teaCoffe.put("optionName", "Others Options");
			teaCoffe.put("isCategory", true);
			// teaCoffe.put("food", teaCoffeList);
			Map<String, List<JsonObject>> teaCoffeeMap = new LinkedHashMap<String, List<JsonObject>>();
			teaCoffeeMap.put("Tea/Coffe", teaCoffeList);
			teaCoffeeMap.put("Others", othersList);

			for (Map.Entry<String, List<JsonObject>> entry : teaCoffeeMap.entrySet()) {
				JsonObject category = new JsonObject();
				category.put("name", entry.getKey());
				category.put("food", entry.getValue());
				categories1.add(category);
			}
			teaCoffe.put("categories", categories1);
			mealOptions.add(teaCoffe);
			suggestedPlan.put("mealOptions", mealOptions);

			break;

		case 2:

			List<JsonObject> snacks = data.stream().filter(this::getSnacks).collect(Collectors.toList());
			snacks = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), snacks, data);
//			snacks = getFilteredListByOptions1(snacks, dbResult, dbResult.getFilterData().getSnacks(), "SNACKS", 2);
			snacks = getSlotOptions(snacks, dbResult, dbResult.getFilterData().getSnacks(), "SNACKS", 2, traceId);

			List<JsonObject> drinks = data.stream().filter(this::getDrinks).collect(Collectors.toList());
			drinks = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), drinks, data);
//			drinks = getFilteredListByOptions1(drinks, dbResult, dbResult.getFilterData().getDrinks(), "DRINKS", 2);
			drinks = getSlotOptions(drinks, dbResult, dbResult.getFilterData().getDrinks(), "DRINKS", 2, traceId);

			List<JsonObject> breadRice = data.stream().filter(x -> x.getString("Type").equalsIgnoreCase("B"))
					.collect(Collectors.toList());
			breadRice = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), breadRice, data);
//			breadRice = getFilteredListByOptions1(breadRice, dbResult, dbResult.getFilterData().getRice(), "BREADRICE",
//					2);
			breadRice = getSlotOptions(breadRice, dbResult, dbResult.getFilterData().getRice(), "BREADRICE", 2,
					traceId);

			List<JsonObject> others = data.stream().filter(
					x -> x.getString("Type").equalsIgnoreCase("S") || x.getString("Type").equalsIgnoreCase("SM"))
					.collect(Collectors.toList());
			others = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), others, data);
			// others = getFilteredListByOptions(others, dbResult, "OTHERS", 2);

			breakfastMap.put("Snacks", snacks);
			breakfastMap.put("Drinks", drinks);
			breakfastMap.put("Bread/Rice", breadRice);
			breakfastMap.put("Others", others);

			JsonObject bfast = new JsonObject();
			bfast.put("optionId", 1);
			bfast.put("isMandatory", true);
			bfast.put("optionName", "Breakfast");
			bfast.put("isCategory", true);

			for (Map.Entry<String, List<JsonObject>> entry : breakfastMap.entrySet()) {
				JsonObject category = new JsonObject();
				category.put("name", entry.getKey());
				category.put("food", entry.getValue());
				categories.add(category);
			}
			bfast.put("categories", categories);
			mealOptions.add(bfast);
			suggestedPlan.put("mealOptions", mealOptions);

			break;

		case 3:
			// SNACKS
			List<JsonObject> snackList = data.stream().filter(x -> this.getSnacks(x)).collect(Collectors.toList());
			snackList = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), snackList, data);
//			snackList = getFilteredListByOptions1(snackList, dbResult, dbResult.getFilterData().getSnacks(),
//					"SNACKLIST", 3);
			snackList = getSlotOptions(snackList, dbResult, dbResult.getFilterData().getSnacks(), "SNACKLIST", 3,
					traceId);

			List<JsonObject> drincList3 = data.stream().filter(x -> this.getDrinks(x)).collect(Collectors.toList());
			drincList3 = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), drincList3, data);
//			drincList3 = getFilteredListByOptions1(drincList3, dbResult, dbResult.getFilterData().getDrinks(),
//					"DRINKLIST3", 3);
			drincList3 = getSlotOptions(drincList3, dbResult, dbResult.getFilterData().getDrinks(), "DRINKLIST3", 3,
					traceId);

			breakfastMap.put("Snacks", snackList);
			breakfastMap.put("Drinks", drincList3);

			JsonObject snacksOption = new JsonObject();
			snacksOption.put("optionId", 1);
			snacksOption.put("isMandatory", true);
			snacksOption.put("optionName", "Snacks");
			snacksOption.put("isCategory", true);
			// snacksOption.put("food", snackList);

			breakfastMap.put("Snacks", snackList);

			breakfastMap.put("Drinks", drincList3);

			for (Map.Entry<String, List<JsonObject>> entry : breakfastMap.entrySet()) {
				JsonObject category = new JsonObject();
				category.put("name", entry.getKey());
				category.put("food", entry.getValue());
				categories.add(category);
			}
			snacksOption.put("categories", categories);
			mealOptions.add(snacksOption);

			// FRUITS
			fruits = data.stream().filter(this::getALlFruits).collect(Collectors.toList());

			fruits = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), fruits, data);
			// fruits = getFilteredListByOptions1(fruits, dbResult,
			// dbResult.getFilterData().getFruits(), "FRUITS", 3);
			fruits = getSlotOptions(fruits, dbResult, dbResult.getFilterData().getFruits(), "FRUITS)", 3, traceId);

			fruitOption = new JsonObject();
			fruitOption.put("optionId", 2);
			fruitOption.put("isMandatory", true);
			fruitOption.put("optionName", "Fruits");
			// fruitOption.put("isCategory", false);
			fruitOption.put("isCategory", true);
			// fruitOption.put("food", fruits);

			Map<String, List<JsonObject>> breakfastFruitsMap = new LinkedHashMap<String, List<JsonObject>>();
			breakfastFruitsMap.put("fruits", fruits);
			for (Map.Entry<String, List<JsonObject>> entry : breakfastFruitsMap.entrySet()) {
				JsonObject category = new JsonObject();
				category.put("name", entry.getKey());
				category.put("food", entry.getValue());
				categories1.add(category);
			}
			fruitOption.put("categories", categories1);
			mealOptions.add(fruitOption);
			suggestedPlan.put("mealOptions", mealOptions);

			break;

		case 4:

			// List<JsonObject> subziRotiList =
			// data.stream().filter(x->x.getString("Type").equalsIgnoreCase("S")||
			// x.getString("Type").equalsIgnoreCase("SM")||x.getString("Type").equalsIgnoreCase("A")||x.getString("Type").equalsIgnoreCase("B")||x.getString("Type").equalsIgnoreCase("C")).collect(Collectors.toList());
			// subziRotiList=ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(),
			// subziRotiList, data);

			JsonObject subziRoti = new JsonObject();
			subziRoti.put("optionId", 1);
			subziRoti.put("isMandatory", true);
			subziRoti.put("optionName", "Subzi/Roti");
			subziRoti.put("isCategory", true);

			// List<JsonObject> subziCurries =
			// data.stream().filter(x->x.getString("Type").equalsIgnoreCase("A")||
			// x.getString("Type").equalsIgnoreCase("C")).collect(Collectors.toList());

			List<JsonObject> subzi = data.stream().filter(x -> x.getString("Type").equalsIgnoreCase("A"))
					.collect(Collectors.toList());
			subzi = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), subzi, data);
//			subzi = getFilteredListByOptions1(subzi, dbResult, dbResult.getFilterData().getDishes(), "DISHES (SUBZI)",
//					4);
			subzi = getSlotOptions(subzi, dbResult, dbResult.getFilterData().getDishes(), "DISHES (SUBZI)", 4, traceId);

			List<JsonObject> curries = data.stream().filter(x -> x.getString("Type").equalsIgnoreCase("C"))
					.collect(Collectors.toList());
			curries = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), curries, data);
//			curries = getFilteredListByOptions1(curries, dbResult, dbResult.getFilterData().getPules(),
//					"PULSES (CURRIES)", 4);
			curries = getSlotOptions(curries, dbResult, dbResult.getFilterData().getPules(), "PULSES (CURRIES)", 4,
					traceId);

			breadRice = data.stream().filter(x -> x.getString("Type").equalsIgnoreCase("B"))
					.collect(Collectors.toList());
			breadRice = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), breadRice, data);
//			breadRice = getFilteredListByOptions1(breadRice, dbResult, dbResult.getFilterData().getRice(), "BREAD/RICE",
//					4);
			breadRice = getSlotOptions(breadRice, dbResult, dbResult.getFilterData().getRice(), "BREAD/RICE", 4,
					traceId);

			others = data.stream().filter(
					x -> x.getString("Type").equalsIgnoreCase("S") || x.getString("Type").equalsIgnoreCase("SM"))
					.collect(Collectors.toList());

			breakfastMap.put("Subzi", subzi);
			breakfastMap.put("Curries", curries);
			breakfastMap.put("Bread/Rice", breadRice);
			breakfastMap.put("Others", others);

			for (Map.Entry<String, List<JsonObject>> entry : breakfastMap.entrySet()) {
				JsonObject category = new JsonObject();
				category.put("name", entry.getKey());
				category.put("food", entry.getValue());
				categories.add(category);
			}

			subziRoti.put("categories", categories);
			mealOptions.add(subziRoti);

			snacks = data.stream().filter(x -> this.getSnacks(x) || this.getDrinks(x)).collect(Collectors.toList());
			snacks = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), snacks, data);
//			snacks = getFilteredListByOptions1(snacks, dbResult, dbResult.getFilterData().getSnacks(), "SNACKS", 4);
			snacks = getSlotOptions(snacks, dbResult, dbResult.getFilterData().getSnacks(), "SNACKS", 4, traceId);

			snacksOption = new JsonObject();
			snacksOption.put("optionId", 2);
			snacksOption.put("isMandatory", true);
			snacksOption.put("optionName", "Snacks");
			snacksOption.put("isCategory", true);
			// snacksOption.put("food", snacks);
			breakfastMap1.put("Snacks", snacks);
			// breakfastMap1.put("Bread/Rice", breadRice);
			for (Map.Entry<String, List<JsonObject>> entry : breakfastMap1.entrySet()) {
				JsonObject category = new JsonObject();
				category.put("name", entry.getKey());
				category.put("food", entry.getValue());
				categories1.add(category);
			}
			snacksOption.put("categories", categories1);
			mealOptions.add(snacksOption);
			suggestedPlan.put("mealOptions", mealOptions);

			break;

		case 5:
			List<JsonObject> drikList = data.stream().filter(this::getDrinks).collect(Collectors.toList());
			drikList = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), drikList, data);
//			drikList = getFilteredListByOptions1(drikList, dbResult, dbResult.getFilterData().getDrinks(), "DRINKLIST",
//					5);
			drikList = getSlotOptions(drikList, dbResult, dbResult.getFilterData().getDrinks(), "DRINKLIST", 5,
					traceId);

			JsonObject drinkOption = new JsonObject();
			drinkOption.put("optionId", 1);
			drinkOption.put("isMandatory", true);
			drinkOption.put("isCategory", true);
			drinkOption.put("optionName", "Drinks");
			// drinkOption.put("food", drikList);
			// mealOptions.add(drinkOption);
			breakfastMap1.put("Drinks", drikList);
			for (Map.Entry<String, List<JsonObject>> entry : breakfastMap1.entrySet()) {
				JsonObject category = new JsonObject();
				category.put("name", entry.getKey());
				category.put("food", entry.getValue());
				categories.add(category);
			}
			drinkOption.put("categories", categories);
			mealOptions.add(drinkOption);
			suggestedPlan.put("mealOptions", mealOptions);

			break;

		case 6:

			snackList = data.stream().filter(x -> this.getSnacks(x)).collect(Collectors.toList());
			snackList = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), snackList, data);
//			snackList = getFilteredListByOptions1(snackList, dbResult, dbResult.getFilterData().getSnacks(),
//					"SNACKLIST", 6);
			snackList = getSlotOptions(snackList, dbResult, dbResult.getFilterData().getSnacks(), "SNACKLIST", 6,
					traceId);

			List<JsonObject> drincList6 = data.stream().filter(x -> this.getDrinks(x)).collect(Collectors.toList());
			drincList6 = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), drincList6, data);
//			drincList6 = getFilteredListByOptions1(drincList6, dbResult, dbResult.getFilterData().getDrinks(),
//					"DRINCLIST6", 6);
			drincList6 = getSlotOptions(drincList6, dbResult, dbResult.getFilterData().getDrinks(), "DRINKLIST6", 6,
					traceId);

			breakfastMap.put("Snacks", snackList);
			breakfastMap.put("Drinks", drincList6);

			snacksOption = new JsonObject();
			snacksOption.put("optionId", 1);
			snacksOption.put("isMandatory", true);
			snacksOption.put("optionName", "Evening Snacks");
			snacksOption.put("isCategory", true);
			// snacksOption.put("food", snackList);

			breakfastMap.put("Snacks", snackList);

			breakfastMap.put("Drinks", drincList6);

			for (Map.Entry<String, List<JsonObject>> entry : breakfastMap.entrySet()) {
				JsonObject category = new JsonObject();
				category.put("name", entry.getKey());
				category.put("food", entry.getValue());
				categories.add(category);
			}
			snacksOption.put("categories", categories);
			mealOptions.add(snacksOption);

			fruits = data.stream().filter(this::getALlFruits).collect(Collectors.toList());
			fruits = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), fruits, data);
			// fruits = getFilteredListByOptions1(fruits, dbResult,
			// dbResult.getFilterData().getFruits(), "FRUITS", 6);
			fruits = getSlotOptions(fruits, dbResult, dbResult.getFilterData().getFruits(), "FRUITS", 6, traceId);

			fruitOption = new JsonObject();
			fruitOption.put("optionId", 2);
			fruitOption.put("isMandatory", true);
			fruitOption.put("optionName", "Fruits");
			fruitOption.put("isCategory", true);
			// fruitOption.put("food", fruits);
			breakfastMap1.put("Fruits", fruits);

			for (Map.Entry<String, List<JsonObject>> entry : breakfastMap1.entrySet()) {
				JsonObject category = new JsonObject();
				category.put("name", entry.getKey());
				category.put("food", entry.getValue());
				categories1.add(category);
			}
			fruitOption.put("categories", categories1);

			mealOptions.add(fruitOption);
			suggestedPlan.put("mealOptions", mealOptions);

			break;

		case 7:

			snacks = data.stream().filter(x -> this.getDrinks(x) || this.getSnacks(x)).collect(Collectors.toList());
			snacks = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), snacks, data);
//			snacks = getFilteredListByOptions1(snacks, dbResult, dbResult.getFilterData().getSnacks(), "SNACKS", 7);
			snacks = getSlotOptions(snacks, dbResult, dbResult.getFilterData().getSnacks(), "SNACKS", 7, traceId);

			breadRice = data.stream().filter(x -> x.getString("Type").equalsIgnoreCase("B"))
					.collect(Collectors.toList());
			breadRice = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), breadRice, data);
//			breadRice = getFilteredListByOptions1(breadRice, dbResult, dbResult.getFilterData().getRice(),
//					"BREADRICE (RICE)", 7);
			breadRice = getSlotOptions(breadRice, dbResult, dbResult.getFilterData().getRice(), "BREADRICE (RICE)", 7,
					traceId);

			snacksOption = new JsonObject();
			snacksOption.put("optionId", 1);
			snacksOption.put("isMandatory", true);
			snacksOption.put("optionName", "Snacks");
			snacksOption.put("isCategory", true);
			// snacksOption.put("food", snacks);
			breakfastMap1.put("Snacks", snacks);
			// breakfastMap1.put("Bread/Rice", breadRice);
			for (Map.Entry<String, List<JsonObject>> entry : breakfastMap1.entrySet()) {
				JsonObject category = new JsonObject();
				category.put("name", entry.getKey());
				category.put("food", entry.getValue());
				categories1.add(category);
			}
			snacksOption.put("categories", categories1);
			mealOptions.add(snacksOption);

			subziRoti = new JsonObject();
			subziRoti.put("optionId", 2);
			subziRoti.put("isMandatory", true);
			subziRoti.put("optionName", "Subzi/Roti");
			subziRoti.put("isCategory", true);

			// subziCurries =
			// data.stream().filter(x->x.getString("Type").equalsIgnoreCase("A")||
			// x.getString("Type").equalsIgnoreCase("C")).collect(Collectors.toList());
			// subziCurries=ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(),
			// subziCurries, data);

			subzi = data.stream().filter(x -> x.getString("Type").equalsIgnoreCase("A")).collect(Collectors.toList());
			subzi = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), subzi, data);
//			subzi = getFilteredListByOptions1(subzi, dbResult, dbResult.getFilterData().getDishes(), "SUBZI (DISHES)",
//					7);
			subzi = getSlotOptions(subzi, dbResult, dbResult.getFilterData().getDishes(), "SUBZI (DISHES)", 7, traceId);

			curries = data.stream().filter(x -> x.getString("Type").equalsIgnoreCase("C")).collect(Collectors.toList());
			curries = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), curries, data);
//			curries = getFilteredListByOptions1(curries, dbResult, dbResult.getFilterData().getPules(),
//					"CURRIES (PULSES)", 7);
			curries = getSlotOptions(curries, dbResult, dbResult.getFilterData().getPules(), "CURRIES (PULSES)", 7,
					traceId);

			others = data.stream().filter(
					x -> x.getString("Type").equalsIgnoreCase("S") || x.getString("Type").equalsIgnoreCase("SM"))
					.collect(Collectors.toList());

			breakfastMap.put("Subzi", subzi);
			breakfastMap.put("Curries", curries);
			breakfastMap.put("Bread/Rice", breadRice);
			breakfastMap.put("Others", others);

			for (Map.Entry<String, List<JsonObject>> entry : breakfastMap.entrySet()) {
				JsonObject category = new JsonObject();
				category.put("name", entry.getKey());
				category.put("food", entry.getValue());
				categories.add(category);
			}
			subziRoti.put("categories", categories);
			mealOptions.add(subziRoti);

			fruits = data.stream().filter(x -> x.getString("Type").equalsIgnoreCase("F")).collect(Collectors.toList());
			fruits = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), fruits, data);
//			fruits = getFilteredListByOptions1(fruits, dbResult, dbResult.getFilterData().getFruits(), "FRUITS", 7);
			fruits = getSlotOptions(fruits, dbResult, dbResult.getFilterData().getFruits(), "FRUITS", 7, traceId);

			fruitOption = new JsonObject();
			fruitOption.put("optionId", 3);
			fruitOption.put("isMandatory", true);
			fruitOption.put("optionName", "Fruits");
			fruitOption.put("isCategory", true);
			// fruitOption.put("food", fruits);

			breakfastMap4.put("Fruits", fruits);
			for (Map.Entry<String, List<JsonObject>> entry : breakfastMap4.entrySet()) {
				JsonObject category = new JsonObject();
				category.put("name", entry.getKey());
				category.put("food", entry.getValue());
				categories2.add(category);
			}
			fruitOption.put("categories", categories2);
			mealOptions.add(fruitOption);
			suggestedPlan.put("mealOptions", mealOptions);

			break;

		case 8:

			// List<JsonObject> bedTimeList =
			// data.stream().filter(x->x.getString("Type").equalsIgnoreCase("F")||
			// this.getSnacks(x)|| this.getDrinks(x)).collect(Collectors.toList());
			// List<JsonObject> bedTimeList =
			// data.stream().filter(x->x.getString("Type").equalsIgnoreCase("F")||
			// this.getDrinks(x)).collect(Collectors.toList());
			// bedTimeList=ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(),
			// bedTimeList, data);

			drinks = data.stream().filter(x -> this.getDrinks(x)).collect(Collectors.toList());
			drinks = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), drinks, data);
//			drinks = getFilteredListByOptions1(drinks, dbResult, dbResult.getFilterData().getDrinks(), "DRINKS", 8);
			drinks = getSlotOptions(drinks, dbResult, dbResult.getFilterData().getDrinks(), "DRINKS", 8, traceId);
			List<String> list = new ArrayList<>();
			dbResult.getFilterData().getTimings().getJsonArray("timings").forEach(action -> {
				JsonObject json = (JsonObject) action;
				if (json.getInteger("slot") == 8 && null == json.getValue("time"))
					list.add(null);
			});

			if (null != list && list.size() > 0) {
				drinks = data.stream().filter(x -> ("D".equalsIgnoreCase(x.getString("Type"))))
						.collect(Collectors.toList());
				drinks = getSlotOptions(drinks, dbResult, dbResult.getFilterData().getDrinks(), "DRINKS", 8, traceId);
			}

			Map<String, List<JsonObject>> bedtimeMap = new LinkedHashMap<String, List<JsonObject>>();
			bedtimeMap.put("drinks", drinks);

			if (null == list || (null != list && list.size() <= 0)) {
				fruits = data.stream().filter(x -> x.getString("Type").equalsIgnoreCase("F"))
						.collect(Collectors.toList());
				fruits = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), fruits, data);
//				fruits = getFilteredListByOptions1(fruits, dbResult, dbResult.getFilterData().getFruits(), "FRUITS", 8);
				fruits = getSlotOptions(fruits, dbResult, dbResult.getFilterData().getFruits(), "FRUITS", 8, traceId);

				bedtimeMap.put("fruits", fruits);
			}

			JsonObject bedtimeOption = new JsonObject();
			bedtimeOption.put("optionId", 1);
			bedtimeOption.put("isMandatory", true);
			bedtimeOption.put("optionName", "Night time Option");
			bedtimeOption.put("isCategory", true);

			for (Map.Entry<String, List<JsonObject>> entry : bedtimeMap.entrySet()) {
				JsonObject category = new JsonObject();
				category.put("name", entry.getKey());
				category.put("food", entry.getValue());
				categories.add(category);
			}

			bedtimeOption.put("categories", categories);
			bedtimeOptions.add(bedtimeOption);
			suggestedPlan.put("mealOptions", bedtimeOptions);
			break;

		default:
			break;
		}

		logger.debug("##### " + method + "  SUGGESTEDPLAN (MEALOPTIONS) -->> " + suggestedPlan);
		promise.complete(suggestedPlan);

		return promise.future();
	}

	/**
	 * Get food for refresh options.
	 * 
	 * @param dbResult
	 * @param slot
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getFoodForRefreshOption(DBResult dbResult, Integer slot, String traceId) {
		String method = "MongoRepositoryWrapper getFoodForRefreshOption() " + traceId;
		logger.info("##### " + method + " SLOT -->> " + slot);

		JsonObject suggestedPlan = new JsonObject();

		Promise<JsonObject> promise = Promise.promise();

		// filter slots related data only
		List<JsonObject> data = dbResult.getData().stream().filter(x -> getDietBySlot(x, slot))
				.collect(Collectors.toList());
		logger.info("##### " + method + " REFRESH OPTION DATA SIZE 1 -->> " + data.size());
		// filter data having not related diseases and respective community
		data = data.stream().filter(x -> filterAvoidIn(x, dbResult.getFilterData().getDisease()))
				.filter(x -> filterByCustCommunity(x, dbResult.getFilterData().getCommunity()))
				.filter(x -> filterByDietSeason(x)).collect(Collectors.toList());
		logger.info("##### " + method + " REFRESH OPTION DATA SIZE 2 -->> " + data.size());
		List<JsonObject> mealOptions = new ArrayList<JsonObject>();
		List<JsonObject> bedtimeOptions = new ArrayList<JsonObject>();

		Map<String, List<JsonObject>> breakfastMap = new LinkedHashMap<String, List<JsonObject>>();
		Map<String, List<JsonObject>> breakfastMap1 = new LinkedHashMap<String, List<JsonObject>>();
		Map<String, List<JsonObject>> breakfastMap4 = new LinkedHashMap<String, List<JsonObject>>();

		List<JsonObject> categories = new ArrayList<JsonObject>();
		List<JsonObject> categories1 = new ArrayList<JsonObject>();
		List<JsonObject> categories2 = new ArrayList<JsonObject>();

		switch (slot) {

		case 0:

			List<JsonObject> wakeupListD = data.stream().filter(x -> {
				if (x.getString("Type").equalsIgnoreCase("D") || x.getString("Type").equalsIgnoreCase("DM")
						|| getSnacks(x)) {
					return true;
				} else {
					return false;
				}
			}).collect(Collectors.toList());

			List<JsonObject> wakeupListA = data.stream().filter(x -> {
				if (x.getString("Type").equalsIgnoreCase("A")) {
					return true;
				} else {
					return false;
				}
			}).collect(Collectors.toList());

			wakeupListA = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), wakeupListA,
					data);
			wakeupListA = wakeupListA.stream().filter(x -> filterByDietSeason(x)).collect(Collectors.toList());
//			wakeupListA = getFilteredListByOptions1(wakeupListA, dbResult, dbResult.getFilterData().getDishes(),
//					"WAKEUPLISTA", 0);
			wakeupListA = getSlotOptions(wakeupListA, dbResult, dbResult.getFilterData().getDishes(), "WAKEUPLISTA", 0,
					traceId);

			wakeupListD = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), wakeupListD,
					data);
			wakeupListD = wakeupListD.stream().filter(x -> filterByDietSeason(x)).collect(Collectors.toList());
//			wakeupListD = getFilteredListByOptions1(wakeupListD, dbResult, dbResult.getFilterData().getSnacks(),
//					"WAKEUPLISTD", 0);
			wakeupListD = getSlotOptions(wakeupListD, dbResult, dbResult.getFilterData().getSnacks(), "wakeupListD", 0,
					traceId);
//			wakeupListD = getFinalFilteredListByOptions1(data, wakeupListD, dbResult, dbResult.getFilterData().getSnacks(),
//					"WAKEUPLISTD", 0);

			breakfastMap.put("Drink", wakeupListD);
			if (dbResult.getFilterData().getDisease() != null && !dbResult.getFilterData().getDisease().isEmpty()) {
				breakfastMap.put("One", wakeupListA);

			}

			JsonObject wakeupOption = new JsonObject();
			wakeupOption.put("optionId", 1);
			wakeupOption.put("isMandatory", true);
			wakeupOption.put("optionName", "Wake up Option");
			wakeupOption.put("isCategory", true);

			for (Map.Entry<String, List<JsonObject>> entry : breakfastMap.entrySet()) {
				JsonObject category = new JsonObject();
				category.put("name", entry.getKey());
				category.put("food", entry.getValue());
				categories.add(category);
			}
			wakeupOption.put("categories", categories);
			mealOptions.add(wakeupOption);
			suggestedPlan.put("mealOptions", mealOptions);

			break;

		case 1:

			List<JsonObject> fruits = data.stream().filter(this::getALlFruits).collect(Collectors.toList());
			fruits = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), fruits, data);
			// fruits = getFilteredListByOptions1(fruits, dbResult,
			// dbResult.getFilterData().getFruits(), "FRUITS)", 1);
//			fruits = getFinalFilteredListByOptions1(data, fruits, dbResult, dbResult.getFilterData().getFruits(),
//					"FRUITS", 1);
			fruits = getSlotOptions(fruits, dbResult, dbResult.getFilterData().getFruits(), "FRUITS", 1, traceId);
			JsonObject fruitOption = new JsonObject();
			fruitOption.put("optionId", 1);
			fruitOption.put("isMandatory", true);
			fruitOption.put("optionName", "Fruits");
			fruitOption.put("isCategory", true);
			// fruitOption.put("food", fruits);

			Map<String, List<JsonObject>> fruitsMap = new LinkedHashMap<String, List<JsonObject>>();
			fruitsMap.put("Fruits", fruits);

			for (Map.Entry<String, List<JsonObject>> entry : fruitsMap.entrySet()) {
				JsonObject category = new JsonObject();
				category.put("name", entry.getKey());
				category.put("food", entry.getValue());
				categories.add(category);
			}
			fruitOption.put("categories", categories);
			mealOptions.add(fruitOption);

			List<JsonObject> teaCoffeList = data.stream().filter(x -> this.getDrinks(x) || this.getSnacks(x))
					.collect(Collectors.toList());

			teaCoffeList = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), teaCoffeList,
					data);
			// teaCoffeList = getFilteredListByOptions(teaCoffeList, dbResult,
			// "TEACOFFELIST", 1);

			JsonObject teaCoffe = new JsonObject();
			teaCoffe.put("optionId", 2);
			teaCoffe.put("isMandatory", true);
			teaCoffe.put("optionName", "Tea/Coffe");
			teaCoffe.put("isCategory", true);
			// teaCoffe.put("food", teaCoffeList);
			Map<String, List<JsonObject>> teaCoffeeMap = new LinkedHashMap<String, List<JsonObject>>();
			teaCoffeeMap.put("Tea/Coffe", teaCoffeList);

			for (Map.Entry<String, List<JsonObject>> entry : teaCoffeeMap.entrySet()) {
				JsonObject category = new JsonObject();
				category.put("name", entry.getKey());
				category.put("food", entry.getValue());
				categories1.add(category);
			}
			teaCoffe.put("categories", categories1);
			mealOptions.add(teaCoffe);
			suggestedPlan.put("mealOptions", mealOptions);

			break;

		case 2:

			List<JsonObject> snacks = data.stream().filter(this::getSnacks).collect(Collectors.toList());
			snacks = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), snacks, data);
//			snacks = getFilteredListByOptions1(snacks, dbResult, dbResult.getFilterData().getSnacks(), "SNACKS", 2);
			snacks = getSlotOptions(snacks, dbResult, dbResult.getFilterData().getSnacks(), "SNACKS", 2, traceId);

			List<JsonObject> drinks = data.stream().filter(this::getDrinks).collect(Collectors.toList());
			drinks = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), drinks, data);
//			drinks = getFilteredListByOptions1(drinks, dbResult, dbResult.getFilterData().getDrinks(), "DRINKS", 2);
			drinks = getSlotOptions(drinks, dbResult, dbResult.getFilterData().getDrinks(), "DRINKS", 2, traceId);

			List<JsonObject> breadRice = data.stream().filter(x -> x.getString("Type").equalsIgnoreCase("B"))
					.collect(Collectors.toList());
			breadRice = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), breadRice, data);
//			breadRice = getFilteredListByOptions1(breadRice, dbResult, dbResult.getFilterData().getRice(), "BREADRICE",
//					2);
			breadRice = getSlotOptions(breadRice, dbResult, dbResult.getFilterData().getRice(), "BREADRICE", 2,
					traceId);

			List<JsonObject> others = data.stream().filter(
					x -> x.getString("Type").equalsIgnoreCase("S") || x.getString("Type").equalsIgnoreCase("SM"))
					.collect(Collectors.toList());
			others = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), others, data);
			// others = getFilteredListByOptions(others, dbResult, "OTHERS", 2);

			breakfastMap.put("Snacks", snacks);
			breakfastMap.put("Drinks", drinks);
			breakfastMap.put("Bread/Rice", breadRice);
			breakfastMap.put("Others", others);

			JsonObject bfast = new JsonObject();
			bfast.put("optionId", 1);
			bfast.put("isMandatory", true);
			bfast.put("optionName", "Breakfast");
			bfast.put("isCategory", true);

			for (Map.Entry<String, List<JsonObject>> entry : breakfastMap.entrySet()) {
				JsonObject category = new JsonObject();
				category.put("name", entry.getKey());
				category.put("food", entry.getValue());
				categories.add(category);
			}
			bfast.put("categories", categories);
			mealOptions.add(bfast);
			suggestedPlan.put("mealOptions", mealOptions);

			break;

		case 3:
			// SNACKS
			List<JsonObject> snackList = data.stream().filter(x -> this.getSnacks(x)).collect(Collectors.toList());
			snackList = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), snackList, data);
//			snackList = getFilteredListByOptions1(snackList, dbResult, dbResult.getFilterData().getSnacks(),
//					"SNACKLIST", 3);
			snackList = getSlotOptions(snackList, dbResult, dbResult.getFilterData().getSnacks(), "SNACKLIST", 3,
					traceId);

			List<JsonObject> drincList3 = data.stream().filter(x -> this.getDrinks(x)).collect(Collectors.toList());
			drincList3 = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), drincList3, data);
//			drincList3 = getFilteredListByOptions1(drincList3, dbResult, dbResult.getFilterData().getDrinks(),
//					"DRINKLIST3", 3);
			drincList3 = getSlotOptions(drincList3, dbResult, dbResult.getFilterData().getDrinks(), "DRINKLIST3", 3,
					traceId);

			breakfastMap.put("Snacks", snackList);
			breakfastMap.put("Drinks", drincList3);

			JsonObject snacksOption = new JsonObject();
			snacksOption.put("optionId", 1);
			snacksOption.put("isMandatory", true);
			snacksOption.put("optionName", "Snacks");
			snacksOption.put("isCategory", true);
			// snacksOption.put("food", snackList);

			breakfastMap.put("Snacks", snackList);

			breakfastMap.put("Drinks", drincList3);

			for (Map.Entry<String, List<JsonObject>> entry : breakfastMap.entrySet()) {
				JsonObject category = new JsonObject();
				category.put("name", entry.getKey());
				category.put("food", entry.getValue());
				categories.add(category);
			}
			snacksOption.put("categories", categories);
			mealOptions.add(snacksOption);

			// FRUITS
			fruits = data.stream().filter(this::getALlFruits).collect(Collectors.toList());

			fruits = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), fruits, data);
			// fruits = getFilteredListByOptions1(fruits, dbResult,
			// dbResult.getFilterData().getFruits(), "FRUITS", 3);
			fruits = getSlotOptions(fruits, dbResult, dbResult.getFilterData().getFruits(), "FRUITS)", 3, traceId);

			fruitOption = new JsonObject();
			fruitOption.put("optionId", 2);
			fruitOption.put("isMandatory", true);
			fruitOption.put("optionName", "Fruits");
			// fruitOption.put("isCategory", false);
			fruitOption.put("isCategory", true);
			// fruitOption.put("food", fruits);

			Map<String, List<JsonObject>> breakfastFruitsMap = new LinkedHashMap<String, List<JsonObject>>();
			breakfastFruitsMap.put("fruits", fruits);
			for (Map.Entry<String, List<JsonObject>> entry : breakfastFruitsMap.entrySet()) {
				JsonObject category = new JsonObject();
				category.put("name", entry.getKey());
				category.put("food", entry.getValue());
				categories1.add(category);
			}
			fruitOption.put("categories", categories1);
			mealOptions.add(fruitOption);
			suggestedPlan.put("mealOptions", mealOptions);

			break;

		case 4:

			// List<JsonObject> subziRotiList =
			// data.stream().filter(x->x.getString("Type").equalsIgnoreCase("S")||
			// x.getString("Type").equalsIgnoreCase("SM")||x.getString("Type").equalsIgnoreCase("A")||x.getString("Type").equalsIgnoreCase("B")||x.getString("Type").equalsIgnoreCase("C")).collect(Collectors.toList());
			// subziRotiList=ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(),
			// subziRotiList, data);

			JsonObject subziRoti = new JsonObject();
			subziRoti.put("optionId", 1);
			subziRoti.put("isMandatory", true);
			subziRoti.put("optionName", "Subzi/Roti");
			subziRoti.put("isCategory", true);

			// List<JsonObject> subziCurries =
			// data.stream().filter(x->x.getString("Type").equalsIgnoreCase("A")||
			// x.getString("Type").equalsIgnoreCase("C")).collect(Collectors.toList());

			List<JsonObject> subzi = data.stream().filter(x -> x.getString("Type").equalsIgnoreCase("A"))
					.collect(Collectors.toList());
			subzi = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), subzi, data);
//			subzi = getFilteredListByOptions1(subzi, dbResult, dbResult.getFilterData().getDishes(), "DISHES (SUBZI)",
//					4);
			subzi = getSlotOptions(subzi, dbResult, dbResult.getFilterData().getDishes(), "DISHES (SUBZI)", 4, traceId);

			List<JsonObject> curries = data.stream().filter(x -> x.getString("Type").equalsIgnoreCase("C"))
					.collect(Collectors.toList());
			curries = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), curries, data);
//			curries = getFilteredListByOptions1(curries, dbResult, dbResult.getFilterData().getPules(),
//					"PULSES (CURRIES)", 4);
			curries = getSlotOptions(curries, dbResult, dbResult.getFilterData().getPules(), "PULSES (CURRIES)", 4,
					traceId);

			breadRice = data.stream().filter(x -> x.getString("Type").equalsIgnoreCase("B"))
					.collect(Collectors.toList());
			breadRice = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), breadRice, data);
//			breadRice = getFilteredListByOptions1(breadRice, dbResult, dbResult.getFilterData().getRice(), "BREAD/RICE",
//					4);
			breadRice = getSlotOptions(breadRice, dbResult, dbResult.getFilterData().getRice(), "BREAD/RICE", 4,
					traceId);

			others = data.stream().filter(
					x -> x.getString("Type").equalsIgnoreCase("S") || x.getString("Type").equalsIgnoreCase("SM"))
					.collect(Collectors.toList());

			breakfastMap.put("Subzi", subzi);
			breakfastMap.put("Curries", curries);
			breakfastMap.put("Bread/Rice", breadRice);
			breakfastMap.put("Others", others);

			for (Map.Entry<String, List<JsonObject>> entry : breakfastMap.entrySet()) {
				JsonObject category = new JsonObject();
				category.put("name", entry.getKey());
				category.put("food", entry.getValue());
				categories.add(category);
			}

			subziRoti.put("categories", categories);
			mealOptions.add(subziRoti);

			snacks = data.stream().filter(x -> this.getSnacks(x) || this.getDrinks(x)).collect(Collectors.toList());
			snacks = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), snacks, data);
//			snacks = getFilteredListByOptions1(snacks, dbResult, dbResult.getFilterData().getSnacks(), "SNACKS", 4);
			snacks = getSlotOptions(snacks, dbResult, dbResult.getFilterData().getSnacks(), "SNACKS", 4, traceId);

			snacksOption = new JsonObject();
			snacksOption.put("optionId", 2);
			snacksOption.put("isMandatory", true);
			snacksOption.put("optionName", "Snacks");
			snacksOption.put("isCategory", true);
			// snacksOption.put("food", snacks);
			breakfastMap1.put("Snacks", snacks);
			for (Map.Entry<String, List<JsonObject>> entry : breakfastMap1.entrySet()) {
				JsonObject category = new JsonObject();
				category.put("name", entry.getKey());
				category.put("food", entry.getValue());
				categories1.add(category);
			}
			snacksOption.put("categories", categories1);
			mealOptions.add(snacksOption);
			suggestedPlan.put("mealOptions", mealOptions);

			break;

		case 5:
			List<JsonObject> drikList = data.stream().filter(this::getDrinks).collect(Collectors.toList());
			drikList = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), drikList, data);
//			drikList = getFilteredListByOptions1(drikList, dbResult, dbResult.getFilterData().getDrinks(), "DRINKLIST",
//					5);
			drikList = getSlotOptions(drikList, dbResult, dbResult.getFilterData().getDrinks(), "DRINKLIST", 5,
					traceId);

			JsonObject drinkOption = new JsonObject();
			drinkOption.put("optionId", 1);
			drinkOption.put("isMandatory", true);
			drinkOption.put("isCategory", true);
			drinkOption.put("optionName", "Drinks");
			// drinkOption.put("food", drikList);
			// mealOptions.add(drinkOption);
			breakfastMap1.put("Drinks", drikList);
			for (Map.Entry<String, List<JsonObject>> entry : breakfastMap1.entrySet()) {
				JsonObject category = new JsonObject();
				category.put("name", entry.getKey());
				category.put("food", entry.getValue());
				categories.add(category);
			}
			drinkOption.put("categories", categories);
			mealOptions.add(drinkOption);
			suggestedPlan.put("mealOptions", mealOptions);

			break;

		case 6:

			snackList = data.stream().filter(x -> this.getSnacks(x)).collect(Collectors.toList());
			snackList = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), snackList, data);
//			snackList = getFilteredListByOptions1(snackList, dbResult, dbResult.getFilterData().getSnacks(),
//					"SNACKLIST", 6);
			snackList = getSlotOptions(snackList, dbResult, dbResult.getFilterData().getSnacks(), "SNACKLIST", 6,
					traceId);

			List<JsonObject> drincList6 = data.stream().filter(x -> this.getDrinks(x)).collect(Collectors.toList());
			drincList6 = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), drincList6, data);
//			drincList6 = getFilteredListByOptions1(drincList6, dbResult, dbResult.getFilterData().getDrinks(),
//					"DRINCLIST6", 6);
			drincList6 = getSlotOptions(drincList6, dbResult, dbResult.getFilterData().getDrinks(), "DRINKLIST6", 6,
					traceId);

			breakfastMap.put("Snacks", snackList);
			breakfastMap.put("Drinks", drincList6);

			snacksOption = new JsonObject();
			snacksOption.put("optionId", 1);
			snacksOption.put("isMandatory", true);
			snacksOption.put("optionName", "Evening Snacks");
			snacksOption.put("isCategory", true);
			// snacksOption.put("food", snackList);

			breakfastMap.put("Snacks", snackList);

			breakfastMap.put("Drinks", drincList6);

			for (Map.Entry<String, List<JsonObject>> entry : breakfastMap.entrySet()) {
				JsonObject category = new JsonObject();
				category.put("name", entry.getKey());
				category.put("food", entry.getValue());
				categories.add(category);
			}
			snacksOption.put("categories", categories);
			mealOptions.add(snacksOption);

			fruits = data.stream().filter(this::getALlFruits).collect(Collectors.toList());
			fruits = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), fruits, data);
			// fruits = getFilteredListByOptions1(fruits, dbResult,
			// dbResult.getFilterData().getFruits(), "FRUITS", 6);
			fruits = getSlotOptions(fruits, dbResult, dbResult.getFilterData().getFruits(), "FRUITS", 6, traceId);

			fruitOption = new JsonObject();
			fruitOption.put("optionId", 2);
			fruitOption.put("isMandatory", true);
			fruitOption.put("optionName", "Fruits");
			fruitOption.put("isCategory", true);
			// fruitOption.put("food", fruits);
			breakfastMap1.put("Fruits", fruits);

			for (Map.Entry<String, List<JsonObject>> entry : breakfastMap1.entrySet()) {
				JsonObject category = new JsonObject();
				category.put("name", entry.getKey());
				category.put("food", entry.getValue());
				categories1.add(category);
			}
			fruitOption.put("categories", categories1);

			mealOptions.add(fruitOption);
			suggestedPlan.put("mealOptions", mealOptions);

			break;

		case 7:

			snacks = data.stream().filter(x -> this.getDrinks(x) || this.getSnacks(x)).collect(Collectors.toList());
			snacks = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), snacks, data);
//			snacks = getFilteredListByOptions1(snacks, dbResult, dbResult.getFilterData().getSnacks(), "SNACKS", 7);
			snacks = getSlotOptions(snacks, dbResult, dbResult.getFilterData().getSnacks(), "SNACKS", 7, traceId);

			snacksOption = new JsonObject();
			snacksOption.put("optionId", 1);
			snacksOption.put("isMandatory", true);
			snacksOption.put("optionName", "Snacks");
			snacksOption.put("isCategory", true);
			// snacksOption.put("food", snacks);
			breakfastMap1.put("Snacks", snacks);
			for (Map.Entry<String, List<JsonObject>> entry : breakfastMap1.entrySet()) {
				JsonObject category = new JsonObject();
				category.put("name", entry.getKey());
				category.put("food", entry.getValue());
				categories1.add(category);
			}
			snacksOption.put("categories", categories1);
			mealOptions.add(snacksOption);

			subziRoti = new JsonObject();
			subziRoti.put("optionId", 2);
			subziRoti.put("isMandatory", true);
			subziRoti.put("optionName", "Subzi/Roti");
			subziRoti.put("isCategory", true);

			// subziCurries =
			// data.stream().filter(x->x.getString("Type").equalsIgnoreCase("A")||
			// x.getString("Type").equalsIgnoreCase("C")).collect(Collectors.toList());
			// subziCurries=ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(),
			// subziCurries, data);

			subzi = data.stream().filter(x -> x.getString("Type").equalsIgnoreCase("A")).collect(Collectors.toList());
			subzi = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), subzi, data);
//			subzi = getFilteredListByOptions1(subzi, dbResult, dbResult.getFilterData().getDishes(), "SUBZI (DISHES)",
//					7);
			subzi = getSlotOptions(subzi, dbResult, dbResult.getFilterData().getDishes(), "SUBZI (DISHES)", 7, traceId);

			curries = data.stream().filter(x -> x.getString("Type").equalsIgnoreCase("C")).collect(Collectors.toList());
			curries = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), curries, data);
//			curries = getFilteredListByOptions1(curries, dbResult, dbResult.getFilterData().getPules(),
//					"CURRIES (PULSES)", 7);
			curries = getSlotOptions(curries, dbResult, dbResult.getFilterData().getPules(), "CURRIES (PULSES)", 7,
					traceId);

			breadRice = data.stream().filter(x -> x.getString("Type").equalsIgnoreCase("B"))
					.collect(Collectors.toList());
			breadRice = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), breadRice, data);
//			breadRice = getFilteredListByOptions1(breadRice, dbResult, dbResult.getFilterData().getRice(),
//					"BREADRICE (RICE)", 7);
			breadRice = getSlotOptions(breadRice, dbResult, dbResult.getFilterData().getRice(), "BREADRICE (RICE)", 7,
					traceId);

			others = data.stream().filter(
					x -> x.getString("Type").equalsIgnoreCase("S") || x.getString("Type").equalsIgnoreCase("SM"))
					.collect(Collectors.toList());

			breakfastMap.put("Subzi", subzi);
			breakfastMap.put("Curries", curries);
			breakfastMap.put("Bread/Rice", breadRice);
			breakfastMap.put("Others", others);

			for (Map.Entry<String, List<JsonObject>> entry : breakfastMap.entrySet()) {
				JsonObject category = new JsonObject();
				category.put("name", entry.getKey());
				category.put("food", entry.getValue());
				categories.add(category);
			}
			subziRoti.put("categories", categories);
			mealOptions.add(subziRoti);

			fruits = data.stream().filter(x -> x.getString("Type").equalsIgnoreCase("F")).collect(Collectors.toList());
			fruits = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), fruits, data);
//			fruits = getFilteredListByOptions1(fruits, dbResult, dbResult.getFilterData().getFruits(), "FRUITS", 7);
			fruits = getSlotOptions(fruits, dbResult, dbResult.getFilterData().getFruits(), "FRUITS", 7, traceId);

			fruitOption = new JsonObject();
			fruitOption.put("optionId", 3);
			fruitOption.put("isMandatory", true);
			fruitOption.put("optionName", "Fruits");
			fruitOption.put("isCategory", true);
			// fruitOption.put("food", fruits);

			breakfastMap4.put("Fruits", fruits);
			for (Map.Entry<String, List<JsonObject>> entry : breakfastMap4.entrySet()) {
				JsonObject category = new JsonObject();
				category.put("name", entry.getKey());
				category.put("food", entry.getValue());
				categories2.add(category);
			}
			fruitOption.put("categories", categories2);
			mealOptions.add(fruitOption);
			suggestedPlan.put("mealOptions", mealOptions);

			break;

		case 8:

			// List<JsonObject> bedTimeList =
			// data.stream().filter(x->x.getString("Type").equalsIgnoreCase("F")||
			// this.getSnacks(x)|| this.getDrinks(x)).collect(Collectors.toList());
			// List<JsonObject> bedTimeList =
			// data.stream().filter(x->x.getString("Type").equalsIgnoreCase("F")||
			// this.getDrinks(x)).collect(Collectors.toList());
			// bedTimeList=ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(),
			// bedTimeList, data);

			drinks = data.stream().filter(x -> this.getDrinks(x)).collect(Collectors.toList());
			drinks = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), drinks, data);
//			drinks = getFilteredListByOptions1(drinks, dbResult, dbResult.getFilterData().getDrinks(), "DRINKS", 8);
			drinks = getSlotOptions(drinks, dbResult, dbResult.getFilterData().getDrinks(), "DRINKS", 8, traceId);
//			if (null == dbResult.getFilterData().getSlot8())
//				drinks = data.stream().filter(x -> ("D".equalsIgnoreCase(x.getString("Type"))))
//						.collect(Collectors.toList());

			List<String> list = new ArrayList<>();
			dbResult.getFilterData().getTimings().getJsonArray("timings").forEach(action -> {
				JsonObject json = (JsonObject) action;
				if (json.getInteger("slot") == 8 && null == json.getValue("time"))
					list.add(null);
			});

			if (null != list && list.size() > 0) {
				drinks = data.stream().filter(x -> ("D".equalsIgnoreCase(x.getString("Type"))))
						.collect(Collectors.toList());
				drinks = getSlotOptions(drinks, dbResult, dbResult.getFilterData().getDrinks(), "DRINKS", 8, traceId);
			}

			fruits = data.stream().filter(x -> x.getString("Type").equalsIgnoreCase("F")).collect(Collectors.toList());
			fruits = ApiUtils.setCustomerSelectionOnTop(dbResult.getFilterData().getAllPrefood(), fruits, data);
//			fruits = getFilteredListByOptions1(fruits, dbResult, dbResult.getFilterData().getFruits(), "FRUITS", 8);
			fruits = getSlotOptions(fruits, dbResult, dbResult.getFilterData().getFruits(), "FRUITS", 8, traceId);

			Map<String, List<JsonObject>> bedtimeMap = new LinkedHashMap<String, List<JsonObject>>();
			bedtimeMap.put("drinks", drinks);
			bedtimeMap.put("fruits", fruits);

			JsonObject bedtimeOption = new JsonObject();
			bedtimeOption.put("optionId", 1);
			bedtimeOption.put("isMandatory", true);
			bedtimeOption.put("optionName", "Night time Option");
			bedtimeOption.put("isCategory", true);

			for (Map.Entry<String, List<JsonObject>> entry : bedtimeMap.entrySet()) {
				JsonObject category = new JsonObject();
				category.put("name", entry.getKey());
				category.put("food", entry.getValue());
				categories.add(category);
			}

			bedtimeOption.put("categories", categories);
			bedtimeOptions.add(bedtimeOption);
			suggestedPlan.put("mealOptions", bedtimeOptions);
			break;

		default:
			break;
		}

		promise.complete(suggestedPlan);
		return promise.future();
	}

	/**
	 * Get diet drink from stream v2.
	 * 
	 * @param dbResult
	 * @param prefList
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getDietDrinFromSream2(DBResult dbResult, List<JsonObject> prefList, String date,
			String traceId) {
		JsonObject response = new JsonObject();
		Promise<JsonObject> promise = Promise.promise();
		List<JsonObject> data = dbResult.getData().stream()
				.filter(x -> filterAvoidIn(x, dbResult.getFilterData().getDisease()))
				.filter(x -> filterByCommunity(x, dbResult.getFilterData().getCommunity()))
				.collect(Collectors.toList());

		final List<JsonObject> jsonArray = new ArrayList<>();
		List<Integer> slots = new ArrayList<>();

		try {
			Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8).forEach(slot -> {
				JsonObject list = filterForSlot(slot, data, dbResult, prefList);
				JsonObject slotObject = new JsonObject();
				if (!list.isEmpty()) {
					slotObject.put("time", ApiUtils.getTimeForSlot(slot));
					slotObject.put("slot", slot);
					slotObject.put("data", list);
					slots.add(slot);
					jsonArray.add(slotObject);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}

		response.put("currentSlot", ApiUtils.getSlotsForList(slots));
		Double totalCalories = jsonArray.stream().mapToDouble(x -> {
			return Double.valueOf(x.getJsonObject("data").getString("totalCalories"));
		}).sum();
		response.put("totalCalories", totalCalories.intValue());
		response.put("totalCaloriesPer", "40");

		Double totalCarbs = jsonArray.stream().mapToDouble(x -> {
			return Double.valueOf(x.getJsonObject("data").getString("totalCarbs"));
		}).sum();
		response.put("totalCarbs", totalCarbs.intValue());
		Double totalCarbsPer = totalCalories / totalCarbs;
		response.put("totalCarbsPer", totalCarbsPer.intValue());

		Double totalFat = jsonArray.stream().mapToDouble(x -> {
			return Double.valueOf(x.getJsonObject("data").getString("totalFat"));
		}).sum();

		Double totalFatPer = totalCalories / totalFat;
		response.put("totalFat", totalFat.intValue());
		response.put("totalFatPer", totalFatPer.intValue());

		Double totalProtien = jsonArray.stream().mapToDouble(x -> {
			return Double.valueOf(x.getJsonObject("data").getString("totalProtien"));
		}).sum();
		Double totalProtienPer = totalCalories / totalProtien;

		response.put("totalProtien", totalProtien.intValue());
		response.put("totalProtienPer", totalProtienPer.intValue());

		Double totalFiber = jsonArray.stream().mapToDouble(x -> {
			return Double.valueOf(x.getJsonObject("data").getString("totalFiber"));
		}).sum();

		Double totalFiberPer = totalCalories / totalFiber;

		response.put("totalFiber", totalFiber.intValue());
		response.put("totalFiberPer", totalFiberPer.intValue());

		response.put("diets", jsonArray);

		saveDietPlan(response, dbResult.getFilterData().getEmail(), date, traceId);

		promise.complete(response);

		return promise.future();
	}

	/**
	 * Check reference food.
	 * 
	 * @param code
	 * @param prefList
	 * @return JsonObject
	 */
	private JsonObject checkPrefFood(String code, List<JsonObject> prefList) {

		for (JsonObject jsonObject : prefList)
			if (jsonObject.getString("code").equals(code))
				return jsonObject;

		return null;
	}

	/**
	 * Create customer diet plan.
	 * 
	 * @param dbResult
	 * @param prefList
	 * @param date
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> createDietplan(DBResult dbResult, List<JsonObject> prefList, String date,
			String traceId) {
		String method = "MongoRepositoryWrapper createDietplan() " + traceId + "-["
				+ dbResult.getFilterData().getEmail() + "]";
		JsonObject response = new JsonObject();
		response.put("isLastLeverExecuted", false);
		response.put("lastLeverCalories", 0.0);
		Promise<JsonObject> promise = Promise.promise();
		try {
			JsonArray previousDietsArr = dbResult.getFilterData().getPreviousDiets();
			Map<String, String> dietMap = new HashMap<>();
			previousDietsArr.forEach(diet -> {
				JsonObject dietObj = (JsonObject)diet;
				dietMap.put(dietObj.getString("slot") + ":" + dietObj.getString("code"), dietObj.getString("type"));
			});
			
			
			
			
			
			Set<String> customerProfileDiets = dbResult.getFilterData().getAllPrefood();
			if (prefList != null && prefList.size() > 0) {
				prefList.forEach(pref -> {
					customerProfileDiets.add(pref.getString("code"));
				});
			}

			try {
				List<JsonObject> finalCustomerProfileDietsPreferencesList = customerProfileDiets.stream().map(code -> {
					final JsonObject obj = getMealByCode(code, dbResult.getData());
					if (obj != null) {
						if (null != prefList && prefList.size() > 0) {
							JsonObject prefObj = checkPrefFood(code, prefList);
							if (prefObj != null) {
								obj.put("portion", prefObj.getDouble("portion"));
								obj.put("originalPortion", obj.getDouble("portion"));
							}
						}
					}
					return obj;
				}).collect(Collectors.toList());

				List<JsonObject> allPlanList = dbResult.getData().stream().map(mapper -> {
					if (mapper != null) {
						if (null != prefList && prefList.size() > 0) {
							JsonObject prefObj = checkPrefFood(mapper.getString("code"), prefList);
							if (prefObj != null) {
								// mapper.put("portion", prefObj.getInteger("portion"));
								mapper.put("originalPortion", mapper.getDouble("portion"));
							}
						}
					}
					return mapper;
				}).collect(Collectors.toList());
				logger.info("##### " + method + " ALL PLAN LIST LIST SIZE -->> " + allPlanList.size());
				finalCustomerProfileDietsPreferencesList = finalCustomerProfileDietsPreferencesList.stream()
						.filter(x -> x != null).filter(x -> filterAvoidIn(x, dbResult.getFilterData().getDisease()))
						.filter(x -> filterByDietSeason(x)).filter(x -> filterByDietSeason(x))
						.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
						.collect(Collectors.toList());
				logger.info("##### " + method + " FINAL PREF LIST SIZE -->> "
						+ finalCustomerProfileDietsPreferencesList.size());

				List<JsonObject> allDiets = new ArrayList<JsonObject>();
				List<JsonObject> slots0 = FoodFilterUtils.getSlot0(allPlanList, dbResult,
						dbResult.getFilterData().getDisease(), dbResult.getFilterData().getCommunity(), traceId);
				List<JsonObject> slots1 = FoodFilterUtils.getSlot1(allPlanList, dbResult,
						dbResult.getFilterData().getDisease(), dbResult.getFilterData().getCommunity(), prefList,
						traceId);
				allDiets.addAll(slots1);
				List<JsonObject> slots2 = FoodFilterUtils.getSlot2(dietMap, allPlanList, dbResult,
						dbResult.getFilterData().getDisease(), finalCustomerProfileDietsPreferencesList,
						dbResult.getFilterData(), traceId);
				allDiets.addAll(slots2);
				List<JsonObject> slots3 = FoodFilterUtils.getSlot3(allPlanList, dbResult,
						dbResult.getFilterData().getDisease(), finalCustomerProfileDietsPreferencesList, traceId);
				allDiets.addAll(slots3);
				List<JsonObject> slots4 = FoodFilterUtils.getSlot4(dietMap, allPlanList, dbResult,
						dbResult.getFilterData().getDisease(), finalCustomerProfileDietsPreferencesList,
						dbResult.getFilterData(), traceId);
				allDiets.addAll(slots4);
				SlotFilter slotFilter5 = FoodFilterUtils.getSlot5(allPlanList, dbResult,
						dbResult.getFilterData().getDisease(), finalCustomerProfileDietsPreferencesList, traceId);
				List<JsonObject> slots5 = slotFilter5.getDataList();
				allDiets.addAll(slots5);
				List<JsonObject> slots6 = FoodFilterUtils.getSlot6(allPlanList, dbResult,
						dbResult.getFilterData().getDisease(), finalCustomerProfileDietsPreferencesList, traceId);
				allDiets.addAll(slots6);
				List<JsonObject> slots7 = FoodFilterUtils.getSlot7(dietMap, allPlanList, dbResult,
						dbResult.getFilterData().getDisease(), finalCustomerProfileDietsPreferencesList, traceId);
				allDiets.addAll(slots7);
				SlotFilter slotFilter8 = FoodFilterUtils.getSlot8(allPlanList, dbResult,
						dbResult.getFilterData().getDisease(), finalCustomerProfileDietsPreferencesList, traceId);
				List<JsonObject> slots8 = slotFilter8.getDataList();

				// Rule-1 (Modified)
//				FoodFilterUtils.addPortion("173", .5d, slots8);

				List<JsonObject> allPlan = new ArrayList<JsonObject>();
				allPlan.addAll(slots0);
				logger.info("##### " + method + " SLOTS 0 SIZE (BEFORE) -->> " + slots0.size() + " -- CALORIES -->> "
						+ ApiUtils.getTotalCalories(slots0));
				allPlan.addAll(slots1);
				logger.info("##### " + method + " SLOTS 1 SIZE (BEFORE) -->> " + slots1.size() + " -- CALORIES -->> "
						+ ApiUtils.getTotalCalories(slots1));
				allPlan.addAll(slots2);
				logger.info("##### " + method + " SLOTS 2 SIZE (BEFORE) -->> " + slots2.size() + " -- CALORIES -->> "
						+ ApiUtils.getTotalCalories(slots2));
				allPlan.addAll(slots3);
				logger.info("##### " + method + " SLOTS 3 SIZE (BEFORE) -->> " + slots3.size() + " -- CALORIES -->> "
						+ ApiUtils.getTotalCalories(slots3));
				allPlan.addAll(slots4);
				logger.info("##### " + method + " SLOTS 4 SIZE (BEFORE) -->> " + slots4.size() + " -- CALORIES -->> "
						+ ApiUtils.getTotalCalories(slots4));
				allPlan.addAll(slots5);
				logger.info("##### " + method + " SLOTS 5 SIZE (BEFORE) -->> " + slots5.size() + " -- CALORIES -->> "
						+ ApiUtils.getTotalCalories(slots5));
				allPlan.addAll(slots6);
				logger.info("##### " + method + " SLOTS 6 SIZE (BEFORE) -->> " + slots6.size() + " -- CALORIES -->> "
						+ ApiUtils.getTotalCalories(slots6));
				allPlan.addAll(slots7);
				logger.info("##### " + method + " SLOTS 7 SIZE (BEFORE) -->> " + slots7.size() + " -- CALORIES -->> "
						+ ApiUtils.getTotalCalories(slots7));
				// allPlan.addAll(slots8);
				logger.info("##### " + method + " SLOTS 8 SIZE (BEFORE) -->> " + slots8.size() + " -- CALORIES -->> "
						+ ApiUtils.getTotalCalories(slots8));

				// IF NV ITEM SERVED IN DINNER (SLOT 7), THEN ADD GREEN TEA INTO SLOT 8
				if ("NV".equalsIgnoreCase(dbResult.getFilterData().getFoodType())) {
					boolean isItemNV = false;
					for (JsonObject json : slots7)
						// IF NV ITEM SERVED IN DINNER (SLOT 7)
						if ("NV".equalsIgnoreCase(json.getString("foodType")))
							isItemNV = true;

					logger.info("##### " + method + " IS ITEM NV ?? " + isItemNV);
					if (isItemNV) {
						List<JsonObject> drinks = allPlanList.stream().filter(x -> getDietBySlot(x, 8))
								.filter(x -> filterAvoidIn(x, dbResult.getFilterData().getDisease()))
								.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
								.filter(x -> filterByDietSeason(x))
								.filter(x -> x.getString("Type").equalsIgnoreCase("D")).collect(Collectors.toList());

						if (null != drinks && drinks.size() > 0 && null != slots8 && slots8.size() > 0) {
							slots8.clear();
							Collections.shuffle(drinks);
							logger.info("##### " + method + " DRINKS ITEM REPLACED -->> " + drinks.get(0));
							JsonObject json = drinks.get(0);
							FoodFilterUtils.updateCalories(json, json.getDouble("portion"));
							ApiUtils.addFoodItem(json, slots8);
						}
					}
				}

				// IF NV ITEM SERVED IN DINNER (SLOT 7), THEN ADD GREEN TEA INTO SLOT 8
//				if ("NV".equalsIgnoreCase(dbResult.getFilterData().getFoodType())) {
//					logger.info("##### " + method + " CUSTOMER FOOD TYPE -->> ['"
//							+ dbResult.getFilterData().getFoodType() + "']");
//					// CHECK IF NV ITEM SERVED IN DINNER ie. SLOT 7
//					boolean isGreenTeaItemAlreadtAddedInSlot8 = false;
//					boolean isItemNV = false;
//					for (JsonObject json : slots7)
//						if ("NV".equalsIgnoreCase(json.getString("foodType"))) {
//							if (json.getString("itemCode").equalsIgnoreCase("170")) {
//								logger.info("##### " + method + " SLOT 7 ITEM [" + json.getString("itemCode")
//										+ "] FOUND AS GREEN TEA");
//								isGreenTeaItemAlreadtAddedInSlot8 = true;
//								break;
//							}
//							isItemNV = true;
//						}
//
//					logger.info("##### " + method + " IS ITEM NON-VEG?? [" + isItemNV + "]");
//					logger.info("##### " + method + " isGreenTeaItemAlreadtAddedInSlot8 ["
//							+ isGreenTeaItemAlreadtAddedInSlot8 + "]");
//					logger.info("##### " + method + " SLOT 8 SIZE (BEFORE ADDITION/REMOVAL) [" + slots8.size() + "]");
//					if (isItemNV && !isGreenTeaItemAlreadtAddedInSlot8) {
//						JsonObject code375 = getMealByCode("375", dbResult.getData());
//						for (JsonObject jsonObj : slots8) {
//							logger.info("##### " + method + " SLOT 8 ITEM [" + jsonObj.getString("itemCode") + "]");
//							if (!code375.getString("itemCode").equalsIgnoreCase(jsonObj.getString("itemCode"))
//									&& "DM".equalsIgnoreCase(jsonObj.getString("Type"))) {
//								// TO BE REMOVED MILK ITEMS
//								logger.info("##### " + method + " SLOT 8 ITEM IS REMOVED -->> ["
//										+ jsonObj.getString("itemCode") + "]");
//								slots8.remove(jsonObj);
//								break;
//							}
//						}
//
//						ApiUtils.addFoodItem(code375, slots8);
//						logger.info("##### " + method + " SLOT 8 SIZE (AFTER ADDITION) [" + slots8.size() + "]");
//					} else {
//						logger.info("##### " + method + " SLOT 8 - IF ITEM IS NON-VEG?? [" + isItemNV
//								+ "] -- IS GREEN TEA ALREAD ADDED?? -->> [" + isGreenTeaItemAlreadtAddedInSlot8 + "]");
//					}
//				}

				allPlan.addAll(slots8);
				logger.info("##### " + method + " SLOTS 8 SIZE (AFTER) -->> " + slots8.size() + " -- CALORIES -->> "
						+ ApiUtils.getTotalCalories(slots8));
				logger.info("##### " + method + " ALL PLAN SIZE (AFTER) -->> " + allPlan.size());
				Double tolalCalories = 0d;
				Double totalSlotsCalories = 0d;
				Double tolalCaloriesLocal = 0d;
				tolalCalories = ApiUtils.getTotalCalories(allPlan);
				logger.info("##### " + method + " ALL SLOTS CALORIES [BEFORE ] -->> " + tolalCalories);
				logger.info("##### " + method + " RECOMMENDED CALORIES         -->> "
						+ dbResult.getFilterData().getCalories());

				String status = ApiUtils.getChangedCaloriesStatus("FILTER FOR LEVERS", tolalCalories,
						dbResult.getFilterData().getCalories());
				logger.info("##### " + method + " [LEVER STARTING] CALORIES STATUS [" + status + "]");
				if ("L".equals(status)) {

					boolean is1stLeverDone = false;
					boolean is2ndLeverDone = false;
					boolean is3rdLeverDone = false;
					boolean is4thLeverDone = false;
					boolean is5thLeverDone = false;
					boolean is6thLeverDone = false;
//					boolean is7thLeverDone = false;
					List<JsonObject> plansListForSlot1And2And3 = new ArrayList<>();
					plansListForSlot1And2And3.addAll(slots1);
					plansListForSlot1And2And3.addAll(slots2);
					plansListForSlot1And2And3.addAll(slots3);
					Double tolalCaloriesOfSlot1And2And3 = ApiUtils.getTotalCalories(plansListForSlot1And2And3);
					double slots123TotalCaloriesPercent = Math
							.round((100 / dbResult.getFilterData().getCalories()) * tolalCaloriesOfSlot1And2And3);
					Double caloriesPlus5Percent = 0d;
					Double caloriesMinus7point5Percent = 0d;
					Double caloriesMidRange = 1600d;

					status = ApiUtils.getChangedCaloriesStatus("0TH LEVER SLOTS 2", tolalCalories,
							dbResult.getFilterData().getCalories());

					// For 1st Lever
					if ("L".equals(status)) {
						List<JsonObject> plansListForSlot4And5And6 = new ArrayList<>();
						plansListForSlot4And5And6.addAll(slots4);
						plansListForSlot4And5And6.addAll(slots5);
						plansListForSlot4And5And6.addAll(slots6);
						Double tolalCaloriesOfSlot4And5And6 = ApiUtils.getTotalCalories(plansListForSlot4And5And6);
						double slots456TotalCaloriesPercent = Math
								.round((100 / dbResult.getFilterData().getCalories()) * tolalCaloriesOfSlot4And5And6);
						if (tolalCaloriesOfSlot4And5And6 < 350 || slots456TotalCaloriesPercent < 30) {
							// IF REQUIRED THEN: - IN SLOT 4 ONLY
							// 1. Increase portion of roti – make it 2 portions : 1 st lever
							// STILL ITEMS NEED TO BE ADDED AT 3RD LEVER
							// 2. Add raita in lunch: 3rd lever
							// STILL ITEMS NEED TO BE ADDED AT 6TH LEVER
							// 3. Increase portion of subzi to 1.5: 6th lever
							// double updatedCalories = 0.0;
							for (JsonObject action : slots4) {
								JsonObject meanDetail = getMealByCode(action.getString("code"),
										dbResult.getMasterPlan());
								if ("B".equalsIgnoreCase(action.getString("Type"))) {
									action = meanDetail;
									action.put("portion", (action.getInteger("portion") * 2));
									FoodFilterUtils.updateCalories(meanDetail, 2);
									tolalCaloriesLocal = (action.getDouble("Calories") / 2);
									slots4 = FoodFilterUtils.filterByCustCommunity(slots4,
											dbResult.getFilterData().getCommunity(), 4, "");
									slots4 = getSaladOnTopAndThenRemaingItems(slots4, "034");
									slots4 = sortingSlot4SpecificItems(slots4);
									tolalCaloriesOfSlot4And5And6 += action.getDouble("Calories");
									slots456TotalCaloriesPercent = Math
											.round((100 / dbResult.getFilterData().getCalories())
													* tolalCaloriesOfSlot4And5And6);
									logger.info("##### " + method
											+ " 1ST LEVER - SLOTS 4 -  'B' FOUND  MEAN DEATIL -->> " + meanDetail);
									is1stLeverDone = true;
									break;
								}
							}

							totalSlotsCalories = checkCalories(slots0, slots1, slots2, slots3, slots4, slots5, slots6,
									slots7, slots8);
							logger.info("##### " + method + " 1ST LEVER - ALL SLOTS TOTAL CALORIES -->> "
									+ totalSlotsCalories);
						}

						if (is1stLeverDone)
							status = ApiUtils.getChangedCaloriesStatus("1ST LEVER", totalSlotsCalories,
									dbResult.getFilterData().getCalories());
						response.put("calStatus", status);
						logger.info("##### " + method + " 1ST LEVER - STATUS -->> " + status);
						logger.info(
								"#########################################################################################");
						logger.info("");

						// for 2nd Lever
						if ("L".equals(status)) {
							// 300 calories are fixed in morning: slot 1 + slot 3
							tolalCaloriesOfSlot1And2And3 = ApiUtils.getTotalCalories(plansListForSlot1And2And3);
							slots123TotalCaloriesPercent = Math.round(
									(100 / dbResult.getFilterData().getCalories()) * tolalCaloriesOfSlot1And2And3);
							if (tolalCaloriesOfSlot1And2And3 < 600 || slots123TotalCaloriesPercent < 45) {
								// IF REQUIRED THEN: - IN SLOT 2 ONLY
								// 1. Increase portion of snack , if portion is 1 then make it 2 - 2 nd lever
								// itemCode-008 Chapati (make it 2 portion)
								// STILL ITEMS NEED TO BE ADDED AT 4TH LEVER
								// 2. Add tea or coffee as per liking: 4th lever
								for (JsonObject action : slots2) {
									JsonObject meanDetail = getMealByCode(action.getString("code"),
											dbResult.getMasterPlan());
									if ("W".equalsIgnoreCase(action.getString("Type"))
											|| "WC".equalsIgnoreCase(action.getString("Type"))
											|| "wcp".equalsIgnoreCase(action.getString("Type"))
											|| "WP".equalsIgnoreCase(action.getString("Type"))
											|| "wpp".equalsIgnoreCase(action.getString("Type"))) {

										if (action.getDouble("portion") > 1) {
											logger.info("##### " + method + " 2ND LEVER - SLOTS 2 JSON -->> " + action);
											continue;
										}

										boolean isLeverToBeExecuted = dbResult.getFilterData()
												.getCalories() <= caloriesMidRange ? true : false;
										if ((isLeverToBeExecuted && !action.getString("Type").contains("WP"))
												|| !isLeverToBeExecuted) {
											FoodFilterUtils.updateCalories(meanDetail, 2);
											action = meanDetail;
											action.put("portion", (meanDetail.getInteger("portion")) * 2);
											tolalCaloriesLocal += (action.getDouble("Calories") / 2);
											slots2 = FoodFilterUtils.filterByCustCommunity(slots2,
													dbResult.getFilterData().getCommunity(), 2, "");
											tolalCaloriesOfSlot1And2And3 += action.getDouble("Calories");
											slots123TotalCaloriesPercent = Math
													.round((100 / dbResult.getFilterData().getCalories())
															* tolalCaloriesOfSlot1And2And3);
											is2ndLeverDone = true;
											logger.info("##### " + method
													+ " 2ND LEVER - SLOTS 2 -  'WP' FOUND  MEAN DEATIL -->> "
													+ meanDetail);
										}
									}
								}
							}

							totalSlotsCalories = checkCalories(slots0, slots1, slots2, slots3, slots4, slots5, slots6,
									slots7, slots8);
							logger.info("##### " + method + " 2ND LEVER - ALL SLOTS TOTAL CALORIES -->> "
									+ totalSlotsCalories);

							if (is2ndLeverDone)
								status = ApiUtils.getChangedCaloriesStatus("2ND LEVER", totalSlotsCalories,
										dbResult.getFilterData().getCalories());
							response.put("calStatus", status);
							logger.info("##### " + method + " 2ND LEVER - STATUS -->> " + status);
							logger.info(
									"#########################################################################################");
							logger.info("");

							// for 3rd lever
							if ("L".equals(status)) {
								// JsonObject code091 = getMealByCode("091", dbResult.getData());
								// ApiUtils.addFoodItem(code091, slots4); // PUNEET SIR ASKED TO REMOVE
								slots4 = sortingSlot4SpecificItems(slots4);
								slots4 = FoodFilterUtils.filterByCustCommunity(slots4,
										dbResult.getFilterData().getCommunity(), 4, "3RD LEVER");
								slots4 = getSaladOnTopAndThenRemaingItems(slots4, "034");
								is3rdLeverDone = true;
								totalSlotsCalories = checkCalories(slots0, slots1, slots2, slots3, slots4, slots5,
										slots6, slots7, slots8);
								logger.info("##### " + method + " 3RD LEVER - ALL SLOTS TOTAL CALORIES -->> "
										+ totalSlotsCalories);

								if (is3rdLeverDone)
									status = ApiUtils.getChangedCaloriesStatus("3RD LEVER", totalSlotsCalories,
											dbResult.getFilterData().getCalories());
								response.put("calStatus", status);
								logger.info("##### " + method + " 3RD LEVER - STATUS -->> " + status);
								logger.info(
										"#########################################################################################");
								logger.info("");

								// for 4th lever
								if ("L".equals(status)) {
									List<JsonObject> slot2DMDrinks = finalCustomerProfileDietsPreferencesList.stream()
											.filter(x -> getDietBySlot(x, 2))
											.filter(x -> x.getString("Type").equalsIgnoreCase("DM"))
											.collect(Collectors.toList());
									List<String> community = dbResult.getFilterData().getCommunity();
									boolean isSouthIndian = false;
									if (community.contains("S") || community.contains("SI") || community.contains("s")
											|| community.contains("si"))
										isSouthIndian = true;

									JsonObject slot2DrinksObj = FoodFilterUtils.getTeaOrCoffeeItems(slot2DMDrinks,
											isSouthIndian, dbResult.getData());
									if (slot2DrinksObj != null) {
										// ApiUtils.addFoodItem(slot2DrinksObj, slots2);
										slots2 = FoodFilterUtils.filterByCustCommunity(slots2,
												dbResult.getFilterData().getCommunity(), 2, "");
										is4thLeverDone = true;
									}

									totalSlotsCalories = checkCalories(slots0, slots1, slots2, slots3, slots4, slots5,
											slots6, slots7, slots8);
									logger.info("##### " + method + " 4TH LEVER - ALL SLOTS TOTAL CALORIES -->> "
											+ totalSlotsCalories);

									if (is4thLeverDone)
										status = ApiUtils.getChangedCaloriesStatus("4TH LEVER", totalSlotsCalories,
												dbResult.getFilterData().getCalories());
									response.put("calStatus", status);
									logger.info("##### " + method + " 4TH LEVER - STATUS -->> " + status);
									logger.info(
											"#########################################################################################");
									logger.info("");

									// for 5th lever
									if ("L".equals(status)) {
										List<JsonObject> plansListForSlot6And7And8 = new ArrayList<>();
										plansListForSlot6And7And8.addAll(slots6);
										plansListForSlot6And7And8.addAll(slots7);
										plansListForSlot6And7And8.addAll(slots8);
										// IF REQUIRED THEN: - IN SLOT 5 ONLY
										// 1. In SLOT 5. Add tea or coffee in slot 6: 5th lever
										List<JsonObject> slot6NVItems = slots6.stream().filter(x -> getDietBySlot(x, 6))
												.filter(x -> ("NV".equalsIgnoreCase(x.getString("foodType"))))
												.collect(Collectors.toList());
										if (null != slot6NVItems && slot6NVItems.size() <= 0) {
											List<JsonObject> slot6DMDrinks = slots6.stream()
													.filter(x -> getDietBySlot(x, 6))
													.filter(x -> (x.getString("Type").equalsIgnoreCase("D")
															|| x.getString("Type").equalsIgnoreCase("DM")))
													.collect(Collectors.toList());
											JsonObject slot6DrinksObj = FoodFilterUtils.geFilteredItems(slot6DMDrinks,
													allDiets);

											List<JsonObject> list = new ArrayList<JsonObject>();
											List<String> items = new ArrayList<>();
											boolean isWMItemReceived = false;
											for (JsonObject json : slots6) {
												items.add(json.getString("itemCode"));
												if ("WM".equalsIgnoreCase(json.getString("Type")))
													isWMItemReceived = true;
											}

											if (!isWMItemReceived && null == slot6DrinksObj) {
												if (!items.contains("060"))
													list.add(getMealByCode("060", dbResult.getData()));
												if (!items.contains("061"))
													list.add(getMealByCode("061", dbResult.getData()));
												if (!items.contains("063"))
													list.add(getMealByCode("063", dbResult.getData()));
//											for (JsonObject json : slots6) {
//												if ("060".equalsIgnoreCase(json.getString("itemCode"))) {
//													list.add(getMealByCode("060", dbResult.getData()));
//													break;
//												} else if ("061".equalsIgnoreCase(json.getString("itemCode"))) {
//													list.add(getMealByCode("061", dbResult.getData()));
//													break;
//												} else if ("063".equalsIgnoreCase(json.getString("itemCode"))) {
//													list.add(getMealByCode("063", dbResult.getData()));
//													break;
//												}
//											}

												slot6DrinksObj = FoodFilterUtils.getItemAfterShuffle(list);
												ApiUtils.addFoodItem(slot6DrinksObj, slots6);
											}
										}

										totalSlotsCalories = checkCalories(slots0, slots1, slots2, slots3, slots4,
												slots5, slots6, slots7, slots8);
										logger.info("##### " + method + " 5TH LEVER - ALL SLOTS TOTAL CALORIES -->> "
												+ totalSlotsCalories);

										if (is5thLeverDone)
											status = ApiUtils.getChangedCaloriesStatus("5TH LEVER SLOTS 6",
													totalSlotsCalories, dbResult.getFilterData().getCalories());
										response.put("calStatus", status);
										logger.info("##### " + method + " 5TH LEVER - STATUS -->> " + status);
										logger.info(
												"#########################################################################################");
										logger.info("");

										// for 6th lever
										if ("L".equals(status)) {
											for (JsonObject action : slots4) {
												JsonObject meanDetail = getMealByCode(action.getString("code"),
														dbResult.getMasterPlan());
												if ("A".equalsIgnoreCase(action.getString("Type"))
														|| "C".equalsIgnoreCase(action.getString("Type"))) {
													FoodFilterUtils.updateCalories(meanDetail, 1.5);
													action = meanDetail;
													action.put("portion", 1.5);
													tolalCaloriesLocal += (action.getDouble("Calories") / 1.5);
													tolalCaloriesOfSlot4And5And6 += action.getDouble("Calories");
													slots456TotalCaloriesPercent = Math
															.round((100 / dbResult.getFilterData().getCalories())
																	* slots456TotalCaloriesPercent);
													slots4 = sortingSlot4SpecificItems(slots4);
													slots4 = FoodFilterUtils.filterByCustCommunity(slots4,
															dbResult.getFilterData().getCommunity(), 4, "");
													slots4 = getSaladOnTopAndThenRemaingItems(slots4, "034");
													is6thLeverDone = true;
													logger.info("##### " + method
															+ " 6TH LEVER - SLOTS 4 -  'A' FOUND MEAN DEATIL -->> "
															+ action);
													break;
												}
											}

											totalSlotsCalories = checkCalories(slots0, slots1, slots2, slots3, slots4,
													slots5, slots6, slots7, slots8);
											logger.info(
													"##### " + method + " 6TH LEVER - ALL SLOTS TOTAL CALORIES -->> "
															+ totalSlotsCalories);

											if (is6thLeverDone)
												status = ApiUtils.getChangedCaloriesStatus("6TH LEVER SLOTS 4",
														totalSlotsCalories, dbResult.getFilterData().getCalories());
											response.put("calStatus", status);
											logger.info("##### " + method + " 6TH LEVER - STATUS -->> " + status);
											logger.info(
													"#########################################################################################");
											logger.info("");

											// for 7th lever
//											if ("L".equals(status)) {
//												List<JsonObject> slot4ListCA = slots4.stream()
//														.filter(x -> x.getString("Type").equalsIgnoreCase("C")
//																|| x.getString("Type").equalsIgnoreCase("A"))
//														.collect(Collectors.toList());
//												String dietToBeAddedAorC = ("A"
//														.equalsIgnoreCase(slot4ListCA.get(0).getString("Type"))) ? "C"
//																: "A";
//												List<JsonObject> slot4ListForAorC = dbResult.getData().stream()
//														.filter(x -> x.getString("Type")
//																.equalsIgnoreCase(dietToBeAddedAorC))
//														.filter(x -> getDietBySlot(x, 4))
//														.filter(x -> filterAvoidIn(x,
//																dbResult.getFilterData().getDisease()))
//														.filter(x -> filterByCustFoodType(x,
//																dbResult.getFilterData().getFoodType()))
//														.collect(Collectors.toList());
//												slot4ListForAorC = FoodFilterUtils.filterByCustCommunity(
//														slot4ListForAorC, dbResult.getFilterData().getCommunity(), 4,
//														"");
//												slot4ListForAorC = FoodFilterUtils.getPrefListFromCommunity(
//														"7TH LEVER - SLOT 4", slot4ListForAorC,
//														dbResult.getFilterData().getCommunity());
//												JsonObject slot4Obj = new JsonObject();
//
//												if (null != prefList && prefList.size() > 0) {
//													List<JsonObject> prefListSlot4 = FoodFilterUtils
//															.getPrefListFromCommunity("SLOT 4", prefList,
//																	dbResult.getFilterData().getCommunity());
//													slot4Obj = FoodFilterUtils.geFilteredData(slot4ListForAorC,
//															prefListSlot4);
//												} else {
//													List<JsonObject> prefListSlot7 = FoodFilterUtils
//															.getPrefListFromCommunity("SLOT 4", dbResult.getData(),
//																	dbResult.getFilterData().getCommunity());
//													slot4Obj = FoodFilterUtils.geFilteredData(slot4ListForAorC,
//															prefListSlot7);
//													if (null == slot4Obj) {
//														slot4Obj = FoodFilterUtils.geFilteredData(slot4ListForAorC);
//													}
//												}
//
//												logger.debug("##### " + method
//														+ " 7TH LEVER - SLOT 4 OBJECT TO BE ADDED -->> " + slot4Obj);
//												if (null != slot4Obj && slot4Obj.size() != 0) {
//													FoodFilterUtils.updateCalories(slot4Obj,
//															slot4Obj.getDouble("portion"));
//													ApiUtils.addFoodItem(slot4Obj, slots4);
//													slots4 = sortingSlot4SpecificItems(slots4);
//													tolalCaloriesLocal += slot4Obj.getDouble("Calories");
//													logger.info("##### " + method
//															+ " 7TH LEVER - SLOT 4 - TOTAL CALCULATED CALORIES -->> "
//															+ tolalCaloriesLocal);
//													slots4 = sortingSlot4SpecificItems(slots4);
//													is7thLeverDone = true;
//													logger.info("##### " + method
//															+ " 7TH LEVER - SLOTS 4 -  'A' FOUND MEAN DEATIL -->> "
//															+ slot4Obj);
//												}
//												slots4 = getSaladOnTopAndThenRemaingItems(slots4, "034");
//												totalSlotsCalories = checkCalories(slots0, slots1, slots2, slots3,
//														slots4, slots5, slots6, slots7, slots8);
//												logger.info("##### " + method
//														+ " 7TH LEVER - ALL SLOTS TOTAL CALORIES -->> "
//														+ totalSlotsCalories);
//
//												if (is7thLeverDone)
//													status = ApiUtils.getChangedCaloriesStatus("7TH LEVER SLOTS 4",
//															totalSlotsCalories, dbResult.getFilterData().getCalories());
//												logger.info("##### " + method + " 7TH LEVER - STATUS -->> " + status);
//												response.put("calStatus", status);
//												logger.info(
//														"#########################################################################################");
											// for 8th lever
											if ("L".equals(status)) {
												logger.info("##### " + method + " 8TH LEVER");
												slots4 = slots4.stream().map(mapper -> {
													if (mapper != null
															&& "B".equalsIgnoreCase(mapper.getString("Type"))) {
														Double portion = mapper.getDouble("portion");
														Double calories = Double.parseDouble(ApiUtils
																.getDecimal(mapper.getDouble("Calories") / portion));
														Double carbs = Double.parseDouble(ApiUtils
																.getDecimal(mapper.getDouble("Carbs") / portion));
														Double fat = Double.parseDouble(
																ApiUtils.getDecimal(mapper.getDouble("Fat") / portion));
														Double protien = Double.parseDouble(ApiUtils
																.getDecimal(mapper.getDouble("Protien") / portion));
														Double fiber = Double.parseDouble(ApiUtils
																.getDecimal(mapper.getDouble("Fiber") / portion));

														mapper.put("portion", portion + 1);
														mapper.put("Calories", Double.parseDouble(ApiUtils
																.getDecimal(mapper.getDouble("Calories") + calories)));
														mapper.put("Carbs", Double.parseDouble(ApiUtils
																.getDecimal(mapper.getDouble("Carbs") + carbs)));
														mapper.put("Fat", Double.parseDouble(
																ApiUtils.getDecimal(mapper.getDouble("Fat") + fat)));
														mapper.put("Protien", Double.parseDouble(ApiUtils
																.getDecimal(mapper.getDouble("Protien") + protien)));
														mapper.put("Fiber", Double.parseDouble(ApiUtils
																.getDecimal(mapper.getDouble("Fiber") + fiber)));

														// FoodFilterUtils.updateCalories(mapper, portion);
														response.put("isLastLeverExecuted", true);
														response.put("lastLeverCalories", mapper.getDouble("Calories"));
														logger.info("##### " + method
																+ " 8TH LEVER - SLOTS 4 -  'B' FOUND MEAN DEATIL -->> "
																+ mapper);
													}
													return mapper;
												}).collect(Collectors.toList());
												totalSlotsCalories = checkCalories(slots0, slots1, slots2, slots3,
														slots4, slots5, slots6, slots7, slots8);
												logger.info("##### " + method
														+ " 8TH LEVER - ALL SLOTS TOTAL CALORIES -->> "
														+ totalSlotsCalories);

												status = ApiUtils.getChangedCaloriesStatus("8TH LEVER SLOTS 4",
														totalSlotsCalories, dbResult.getFilterData().getCalories());
												response.put("calStatus", status);
												logger.info("##### " + method + " 8TH LEVER - STATUS -->> " + status);

												slots4 = getSaladOnTopAndThenRemaingItems(slots4, "034");
											}

											logger.info(
													"#########################################################################################");
											logger.info("");
											// } //
										}
									}
								}
							}
						}
					}

					allPlan.clear();
					allPlan.addAll(slots0);
					allPlan.addAll(slots1);
					allPlan.addAll(slots2);
					allPlan.addAll(slots3);
					allPlan.addAll(slots4);
					allPlan.addAll(slots5);
					allPlan.addAll(slots6);
					allPlan.addAll(slots7);
					allPlan.addAll(slots8);

					logger.info("##### " + method + " ALL SLOTS TOTAL CALORIES          -->> " + totalSlotsCalories);
					caloriesPlus5Percent = ApiUtils.getPlus5Pper(dbResult.getFilterData().getCalories());
					logger.info("##### " + method + " RECOMMENDED CALORIES (PLUS 5%)    -->> [" + caloriesPlus5Percent
							+ "]");
					caloriesMinus7point5Percent = ApiUtils.getMinus7point5Pper(dbResult.getFilterData().getCalories());
					logger.info("##### " + method + " RECOMMENDED CALORIES (MINUS 7.5%) -->> ["
							+ caloriesMinus7point5Percent + "]");
					if (totalSlotsCalories > caloriesPlus5Percent)
						logger.info("##### " + method + " TOTAl CALORIES [" + totalSlotsCalories
								+ "] - GREATER THAN RECOMMENDED CALORIES PLUS 5% -->> [" + caloriesMinus7point5Percent
								+ "]");
					else if (totalSlotsCalories < caloriesMinus7point5Percent)
						logger.info("##### " + method + " TOTAl CALORIES [" + totalSlotsCalories
								+ "] - LESS THAN RECOMMENDED CALORIES MINUS 5% -->> [" + caloriesMinus7point5Percent
								+ "]");
					else
						logger.info("##### " + method + " FINAL CALORIES FOUND -->> [" + totalSlotsCalories + "]");
					logger.info(
							"#########################################################################################");
					logger.info("");

					//////////////////////////////////////// OLD CODE - START
					//////////////////////////////////////// /////////////////////////////////////////////

					/*
					 * slots2.forEach(action -> { JsonObject
					 * meanDetail=getMealByCode(action.getString("code"), dbResult.getMasterPlan());
					 * if("WP".equalsIgnoreCase(action.getString("Type"))) {
					 * FoodFilterUtils.updateCalories(meanDetail, 2); action=meanDetail;
					 * action.put("portion", 2);
					 * 
					 * }else if("WC".equalsIgnoreCase(action.getString("Type"))) {
					 * FoodFilterUtils.updateCalories(meanDetail, 2); action=meanDetail;
					 * action.put("portion", 2); } else
					 * if("W".equalsIgnoreCase(action.getString("Type"))) {
					 * FoodFilterUtils.updateCalories(meanDetail, 2); action=meanDetail;
					 * action.put("portion", 2); }else
					 * if("B".equalsIgnoreCase(action.getString("Type"))) {
					 * FoodFilterUtils.updateCalories(meanDetail, 2); action=meanDetail;
					 * action.put("portion", 2); }
					 * 
					 * });
					 * 
					 * List<JsonObject> slot2Drin=finalPrefList.stream().filter(x ->
					 * getDietBySlot(x,
					 * 2)).filter(x->x.getString("Type").equalsIgnoreCase("DM")).collect(Collectors.
					 * toList()); JsonObject slot2DrinObj=
					 * FoodFilterUtils.geFilteredData(slot2Drin,allDiets); if(slot2DrinObj!=null) {
					 * ApiUtils.addFoodItem(slot2DrinObj,slots2);
					 * ApiUtils.addFoodItem(slot2DrinObj,allPlan); }
					 * 
					 * 
					 * status=ApiUtils.getStatus(ApiUtils.getTotalCalories(allPlan),
					 * dbResult.getFilterData().getCalories());
					 * 
					 * if("L".equals(status)) {
					 * 
					 * slots4.forEach(action -> { JsonObject
					 * meanDetail=getMealByCode(action.getString("code"), dbResult.getMasterPlan());
					 * 
					 * if("B".equalsIgnoreCase(action.getString("Type"))) {
					 * FoodFilterUtils.updateCalories(meanDetail, 2); action=meanDetail;
					 * action.put("portion", 2); }
					 * 
					 * });
					 * 
					 * JsonObject code091=getMealByCode("091", dbResult.getData());
					 * ApiUtils.addFoodItem(code091,slots4); ApiUtils.addFoodItem(code091,allPlan);
					 * 
					 * List<JsonObject> slot6Drin=finalPrefList.stream().filter(x ->
					 * getDietBySlot(x,
					 * 6)).filter(x->x.getString("Type").equalsIgnoreCase("DM")).collect(Collectors.
					 * toList()); if(slot6Drin==null || slot6Drin.isEmpty()) {
					 * slot6Drin=finalPrefList.stream().filter(x -> getDietBySlot(x,
					 * 6)).filter(x->getDrinks(x)).collect(Collectors.toList()); }
					 * logger.info("**slot6Drin**"+slot6Drin.size()); JsonObject slot6DrinObj=
					 * FoodFilterUtils.geFilteredData(slot6Drin,allDiets);
					 * 
					 * if(slot6DrinObj!=null) { ApiUtils.addFoodItem(slot6DrinObj,slots6);
					 * ApiUtils.addFoodItem(slot6DrinObj,allPlan); }
					 * 
					 * }
					 */
					///////////////////////////////////// OLD CODE - END
					///////////////////////////////////// /////////////////////////////////////////////

				} else if ("H".equals(status)) {
					// 032 TO BE DROPPED FROM SLOT 3
//					for (JsonObject json : slots3) {
//						JsonObject meanDetail = getMealByCode(json.getString("code"), dbResult.getMasterPlan());
//						if ("032".equalsIgnoreCase(json.getString("itemCode"))) {
//							int portion = json.getInteger("portion");
//							logger.info("##### " + method + " 0TH LEVER SLOTS 3 '032' FOUND");
//							logger.info("##### 0TH LEVER - SLOTS 3 - FETCHED CALORIES (BEFORE) -->> " + json.getDouble("Calories"));
//							double calories = json.getDouble("Calories") / 2;
//							logger.info("##### 0TH LEVER - SLOTS 3 - FETCHED CALORIES (AFTER) -->> " + calories);
//							FoodFilterUtils.updateCalories(meanDetail, 0.5);
//							json = meanDetail;
//							json.put("portion", portion / 2);
//							tolalCalories = tolalCalories - calories;
//							logger.info("##### 0TH LEVER - SLOTS 3 - TOTAL CALCULATED CALORIES -->> " + tolalCalories);
//							break;
//						}
//					}
//					
//					logger.info("##### " + method + " 0TH LEVER - 032 ITEM BECOME HALF (5) FOR SLOT 3");

				} // END

				List<JsonObject> result = new ArrayList<JsonObject>();
				Double slotWiseTotalCalories = 0d;

				// SLOT 0
				JsonObject slotObject0 = new JsonObject();
				slotObject0.put("time", dbResult.getFilterData().getSlot0().replace(" AM", ""));
				slotObject0.put("message", dbResult.getFilterData().getSlot0Message());
				slotObject0.put("habitCode", "H018");
				slotObject0.put("slot", 0);
				slotObject0.put("Locked", false);
				slotObject0.put("isLocked", false);
				slotObject0.put("Remark", "Start your day with atleast 2 glasses of warm water.");

				slotObject0.put("data", slots0);
				Double totalCalories = slots0.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Calories"))
						.sum();
				Double totalCarbs = slots0.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Carbs")).sum();
				Double totalFat = slots0.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fat")).sum();
				Double totalProtien = slots0.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Protien"))
						.sum();
				Double totalFiber = slots0.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fiber")).sum();
				slotObject0.put("totalCalories", ApiUtils.getDecimal(totalCalories));
				slotObject0.put("totalCarbs", ApiUtils.getDecimal(totalCarbs));
				slotObject0.put("totalFat", ApiUtils.getDecimal(totalFat));
				slotObject0.put("totalProtien", ApiUtils.getDecimal(totalProtien));
				slotObject0.put("totalFiber", ApiUtils.getDecimal(totalFiber));
				result.add(slotObject0);
				slotWiseTotalCalories += totalCalories;
				logger.info("##### " + method + " [SLOT 0] TOTAL CALORIES -->> [" + totalCalories + "]");

				// SLOT 1
				JsonObject slotObject1 = new JsonObject();
				slotObject1.put("time", dbResult.getFilterData().getSlot1().replace(" AM", ""));
				slotObject1.put("message", dbResult.getFilterData().getSlot1Message());
				slotObject1.put("habitCode", "H013");
				slotObject1.put("slot", 1);
				slotObject1.put("Locked", false);
				slotObject1.put("isLocked", false);
				slotObject1.put("data", slots1);
				slotObject1.put("Remark", "Break your fasting with fruits and not tea.");

				totalCalories = slots1.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Calories")).sum();
				totalCarbs = slots1.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Carbs")).sum();
				totalFat = slots1.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fat")).sum();
				totalProtien = slots1.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Protien")).sum();
				totalFiber = slots1.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fiber")).sum();
				slotObject1.put("totalCalories", ApiUtils.getDecimal(totalCalories));
				slotObject1.put("totalCarbs", ApiUtils.getDecimal(totalCarbs));
				slotObject1.put("totalFat", ApiUtils.getDecimal(totalFat));
				slotObject1.put("totalProtien", ApiUtils.getDecimal(totalProtien));
				slotObject1.put("totalFiber", ApiUtils.getDecimal(totalFiber));
				result.add(slotObject1);
				slotWiseTotalCalories += totalCalories;
				logger.info("##### " + method + " [SLOT 1] TOTAL CALORIES -->> [" + totalCalories + "]");

				// SLOT 2
				List<String> tips = new ArrayList<String>();
				tips.add("H017");
				tips.add("H020");
				Map<String, String> map = new HashMap<>();
				map.put("H017", "By having heavy breakfast you will be aligning with circadian cycle.");
				map.put("H020", "Limit sugar intake to 2 tsp each day.");
				Collections.shuffle(tips);
				String tip = tips.get(0);
				JsonObject slotObject2 = new JsonObject();
				slotObject2.put("time", dbResult.getFilterData().getSlot2().replace(" AM", ""));
				slotObject2.put("message",
						(null != dbResult.getFilterData().getSlot2Message()
								&& !"".equalsIgnoreCase(dbResult.getFilterData().getSlot2Message()))
										? dbResult.getFilterData().getSlot2Message()
										: "BreakFast");
				slotObject2.put("habitCode", tip);
				slotObject2.put("slot", 2);
				slotObject2.put("data", slots2);
				slotObject2.put("Locked", false);
				slotObject2.put("isLocked", false);
				slotObject2.put("Remark", map.get(tip));

				totalCalories = slots2.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Calories")).sum();
				totalCarbs = slots2.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Carbs")).sum();
				totalFat = slots2.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fat")).sum();
				totalProtien = slots2.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Protien")).sum();
				totalFiber = slots2.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fiber")).sum();
				slotObject2.put("totalCalories", ApiUtils.getDecimal(totalCalories));
				slotObject2.put("totalCarbs", ApiUtils.getDecimal(totalCarbs));
				slotObject2.put("totalFat", ApiUtils.getDecimal(totalFat));
				slotObject2.put("totalProtien", ApiUtils.getDecimal(totalProtien));
				slotObject1.put("totalFiber", ApiUtils.getDecimal(totalFiber));
				result.add(slotObject2);
				slotWiseTotalCalories += totalCalories;
				logger.info("##### " + method + " [SLOT 2] TOTAL CALORIES -->> [" + totalCalories + "]");

				// SLOT 3
				JsonObject slotObject3 = new JsonObject();
				slotObject3.put("time", dbResult.getFilterData().getSlot3().replace(" AM", ""));
				slotObject3.put("message", dbResult.getFilterData().getSlot3Message());
				slotObject3.put("habitCode", "H003");
				slotObject3.put("slot", 3);
				slotObject3.put("Locked", false);
				slotObject3.put("isLocked", true);
				slotObject3.put("data", slots3);
				slotObject3.put("Remark", "Drink coconut water or butter milk each day.");

				totalCalories = slots3.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Calories")).sum();
				totalCarbs = slots3.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Carbs")).sum();
				totalFat = slots3.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fat")).sum();
				totalProtien = slots3.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Protien")).sum();
				totalFiber = slots3.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fiber")).sum();
				slotObject3.put("totalCalories", ApiUtils.getDecimal(totalCalories));
				slotObject3.put("totalCarbs", ApiUtils.getDecimal(totalCarbs));
				slotObject3.put("totalFat", ApiUtils.getDecimal(totalFat));
				slotObject3.put("totalProtien", ApiUtils.getDecimal(totalProtien));
				slotObject3.put("totalFiber", ApiUtils.getDecimal(totalFiber));
				result.add(slotObject3);
				slotWiseTotalCalories += totalCalories;
				logger.info("##### " + method + " [SLOT 3] TOTAL CALORIES -->> [" + totalCalories + "]");

				// SLOT 4
				tips.clear();
				tips.add("H012");
				tips.add("H011");
				tips.add("H025");
				map.clear();
				map.put("H012", "Eat bowl of salad before lunch.");
				map.put("H011", "Use Kachi ghani oil for cooking.");
				map.put("H025", "Eat 1 tsp tadka of Desi ghee for garnishing.");
				Collections.shuffle(tips);
				tip = tips.get(0);
				JsonObject slotObject4 = new JsonObject();
				slotObject4.put("time", dbResult.getFilterData().getSlot4().replace(" AM", "").replace(" PM", ""));
				slotObject4.put("message", dbResult.getFilterData().getSlot4Message());
				slotObject4.put("habitCode", tip);
				slotObject4.put("slot", 4);
				slotObject4.put("Locked", false);
				slotObject4.put("isLocked", true);
				slotObject4.put("data", slots4);
				slotObject4.put("Remark", map.get(tip));

				totalCalories = slots4.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Calories")).sum();
				totalCarbs = slots4.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Carbs")).sum();
				totalFat = slots4.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fat")).sum();
				totalProtien = slots4.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Protien")).sum();
				totalFiber = slots4.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fiber")).sum();
				slotObject4.put("totalCalories", ApiUtils.getDecimal(totalCalories));
				slotObject4.put("totalCarbs", ApiUtils.getDecimal(totalCarbs));
				slotObject4.put("totalFat", ApiUtils.getDecimal(totalFat));
				slotObject4.put("totalProtien", ApiUtils.getDecimal(totalProtien));
				slotObject4.put("totalFiber", ApiUtils.getDecimal(totalFiber));
				result.add(slotObject4);
				slotWiseTotalCalories += totalCalories;
				logger.info("##### " + method + " [SLOT 4] TOTAL CALORIES -->> [" + totalCalories + "]");

				// SLOT 5
				JsonObject slotObject5 = new JsonObject();
				slotObject5.put("time", dbResult.getFilterData().getSlot5().replace(" AM", "").replace(" PM", ""));
				slotObject5.put("message", dbResult.getFilterData().getSlot5Message());
				slotObject5.put("habitCode", "H001");
				slotObject5.put("slot", 5);
				slotObject5.put("data", slots5);
				slotObject5.put("Remark", "Green tea is good deoxidation.");
				// slotObject5.put("Locked", slotFilter5.isLocked());
				slotObject5.put("Locked", true);
				slotObject5.put("isLocked", true);

				totalCalories = slots5.stream().filter(x -> x != null).filter(x -> x != null)
						.mapToDouble(m -> m.getDouble("Calories")).sum();
				totalCarbs = slots5.stream().filter(x -> x != null).filter(x -> x != null)
						.mapToDouble(m -> m.getDouble("Carbs")).sum();
				totalFat = slots5.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fat")).sum();
				totalProtien = slots5.stream().filter(x -> x != null).filter(x -> x != null)
						.mapToDouble(m -> m.getDouble("Protien")).sum();
				totalFiber = slots5.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fiber")).sum();
				slotObject5.put("totalCalories", ApiUtils.getDecimal(totalCalories));
				slotObject5.put("totalCarbs", ApiUtils.getDecimal(totalCarbs));
				slotObject5.put("totalFat", ApiUtils.getDecimal(totalFat));
				slotObject5.put("totalProtien", ApiUtils.getDecimal(totalProtien));
				slotObject5.put("totalFiber", ApiUtils.getDecimal(totalFiber));
				result.add(slotObject5);
				slotWiseTotalCalories += totalCalories;
				logger.info("##### " + method + " [SLOT 5] TOTAL CALORIES -->> [" + totalCalories + "]");

				// SLOT 6
				tips.clear();
				tips.add("H019");
				tips.add("H004");
				map.clear();
				map.put("H019", "Eat Frequently for mindful eating. So evening day snacks are good");
				map.put("H004", "Soup is a good healthy choice at this time.");
				Collections.shuffle(tips);
				tip = tips.get(0);
				JsonObject slotObject6 = new JsonObject();
				slotObject6.put("time", dbResult.getFilterData().getSlot6().replace(" AM", "").replace(" PM", ""));
				slotObject6.put("message", dbResult.getFilterData().getSlot6Message());
				slotObject6.put("habitCode", tip);
				slotObject6.put("slot", 6);
				slotObject6.put("data", slots6);
				slotObject6.put("Locked", false);
				slotObject6.put("isLocked", true);

				totalCalories = slots6.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Calories")).sum();
				totalCarbs = slots6.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Carbs")).sum();
				totalFat = slots6.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fat")).sum();
				totalProtien = slots6.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Protien")).sum();
				totalFiber = slots6.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fiber")).sum();
				slotObject6.put("totalCalories", ApiUtils.getDecimal(totalCalories));
				slotObject6.put("totalCarbs", ApiUtils.getDecimal(totalCarbs));
				slotObject6.put("totalFat", ApiUtils.getDecimal(totalFat));
				slotObject6.put("totalProtien", ApiUtils.getDecimal(totalProtien));
				slotObject6.put("totalFiber", ApiUtils.getDecimal(totalFiber));
				slotObject6.put("Remark", map.get(tip));
				result.add(slotObject6);
				slotWiseTotalCalories += totalCalories;
				logger.info("##### " + method + " [SLOT 6] TOTAL CALORIES -->> [" + totalCalories + "]");

				// SLOT 7
				JsonObject slotObject7 = new JsonObject();
				slotObject7.put("time", dbResult.getFilterData().getSlot7().replace(" AM", "").replace(" PM", ""));
				slotObject7.put("message", dbResult.getFilterData().getSlot7Message());
				slotObject7.put("habitCode", "H016");
				slotObject7.put("slot", 7);
				slotObject7.put("Locked", false);
				slotObject7.put("isLocked", true);
				slotObject7.put("data", slots7);
				slotObject7.put("Remark", "Take light dinner by 7 PM to align with Circadian cycle.");

				totalCalories = slots7.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Calories")).sum();
				totalCarbs = slots7.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Carbs")).sum();
				totalFat = slots7.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fat")).sum();
				totalProtien = slots7.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Protien")).sum();
				totalFiber = slots7.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fiber")).sum();
				slotObject7.put("totalCalories", ApiUtils.getDecimal(totalCalories));
				slotObject7.put("totalCarbs", ApiUtils.getDecimal(totalCarbs));
				slotObject7.put("totalFat", ApiUtils.getDecimal(totalFat));
				slotObject7.put("totalProtien", ApiUtils.getDecimal(totalProtien));
				slotObject7.put("totalFiber", ApiUtils.getDecimal(totalFiber));
				result.add(slotObject7);
				slotWiseTotalCalories += totalCalories;
				logger.info("##### " + method + " [SLOT 7] TOTAL CALORIES -->> [" + totalCalories + "]");

				// SLOT 8
				JsonObject slotObject8 = new JsonObject();
				List<String> list = new ArrayList<>();
//				slotObject8.put("time",
//						dbResult.getFilterData().getSlot8().replace(" AM", "").replace(" PM", "").replace(" ", ""));
				if (null != dbResult.getFilterData().getTimings())
					dbResult.getFilterData().getTimings().getJsonArray("timings").forEach(action -> {
						JsonObject json = (JsonObject) action;
						if (json.getInteger("slot") == 8 && null == json.getValue("time"))
							list.add(null);
					});

				if (null != list && list.size() > 0) {
					List<JsonObject> slots8List = allPlanList.stream().filter(x -> getDietBySlot(x, 8))
							.filter(x -> filterAvoidIn(x, dbResult.getFilterData().getDisease()))
							.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
							.filter(x -> filterByDietSeason(x)).filter(x -> x.getString("Type").equalsIgnoreCase("D"))
							.collect(Collectors.toList());
					Collections.shuffle(slots8List);
					logger.info("##### " + method + " SLOTS 8 IS EMPTY -->> " + slots8.isEmpty());
					if (null == slots8List || (null != slots8List && slots8List.size() <= 0) || slots8List.isEmpty()) {
						slots8List = allPlanList.stream().filter(x -> ("DM".equalsIgnoreCase(x.getString("Type"))))
								.collect(Collectors.toList());
						Collections.shuffle(slots8);
						FoodFilterUtils.updateCalories(slots8List.get(0), slots8List.get(0).getDouble("portion"));
						slots8.add(slots8List.get(0));
					} else {
						List<JsonObject> drinks = allPlanList.stream().filter(x -> getDietBySlot(x, 8))
								.filter(x -> filterAvoidIn(x, dbResult.getFilterData().getDisease()))
								.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
								.filter(x -> filterByDietSeason(x))
								.filter(x -> x.getString("Type").equalsIgnoreCase("D")).collect(Collectors.toList());

						if (null != drinks && drinks.size() > 0 && null != slots8 && slots8.size() > 0) {
							slots8.clear();
							Collections.shuffle(drinks);
							logger.info("##### " + method + " ITEMS REPLACED [DRINKS] -->> " + drinks.get(0));
							JsonObject json = drinks.get(0);
							FoodFilterUtils.updateCalories(json, json.getDouble("portion"));
							ApiUtils.addFoodItem(json, slots8);
						}
					}

					slotObject8.put("time", JsonObject.mapFrom(null));
				} else {
					slotObject8.put("time",
							dbResult.getFilterData().getSlot8().replace(" AM", "").replace(" PM", "").replace(" ", ""));
				}

				slotObject8.put("message", dbResult.getFilterData().getSlot8Message());
				slotObject8.put("habitCode", "H016");
				slotObject8.put("slot", 8);
				slotObject8.put("Locked", slotFilter8.isLocked());
				slotObject8.put("isLocked", true);
				slotObject8.put("data", slots8);
				slotObject8.put("Remark", "Haldi milk is good for sound sleep n immunity.");

				totalCalories = slots8.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Calories")).sum();
				totalCarbs = slots8.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Carbs")).sum();
				totalFat = slots8.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fat")).sum();
				totalProtien = slots8.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Protien")).sum();
				totalFiber = slots8.stream().filter(x -> x != null).filter(x -> x != null)
						.mapToDouble(m -> m.getDouble("Fiber")).sum();
				slotObject8.put("totalCalories", ApiUtils.getDecimal(totalCalories));
				slotObject8.put("totalCarbs", ApiUtils.getDecimal(totalCarbs));
				slotObject8.put("totalFat", ApiUtils.getDecimal(totalFat));
				slotObject8.put("totalProtien", ApiUtils.getDecimal(totalProtien));
				slotObject8.put("totalFiber", ApiUtils.getDecimal(totalFiber));
				result.add(slotObject8);
				slotWiseTotalCalories += totalCalories;
				slotWiseTotalCalories = Double.parseDouble(ApiUtils.getDecimal(slotWiseTotalCalories));
				logger.info("##### " + method + " [SLOT 8] TOTAL CALORIES -->> [" + totalCalories + "]");

				allPlan = allPlan.stream().filter(x -> x != null).collect(Collectors.toList());
				totalCalories = allPlan.stream().mapToDouble(x -> {
					return Double.valueOf(x.getDouble("Calories"));
				}).sum();

				logger.info("##### " + method + "    ALL SLOTS CALORIES -->> [" + slotWiseTotalCalories + "]");
				logger.info("##### " + method + "  RECOMMENDED CALORIES -->> [" + dbResult.getFilterData().getCalories()
						+ "]");

				response.put("calStatus", status);
				response.put("totalCal", Double.parseDouble(ApiUtils.getDecimal(slotWiseTotalCalories)));
				response.put("recomended", dbResult.getFilterData().getCalories());
				response.put("tolalCalories", Double.parseDouble(ApiUtils.getDecimal(slotWiseTotalCalories)));
				Double totalCaloriesPer = ((slotWiseTotalCalories * 100)
						/ dbResult.getFilterData().getCalories()) > 100.0 ? 100
								: ((slotWiseTotalCalories * 100) / dbResult.getFilterData().getCalories());
				response.put("totalCalories", Double.parseDouble(ApiUtils.getDecimal(slotWiseTotalCalories)));
				response.put("totalCaloriesPer", totalCaloriesPer.intValue());

				totalCarbs = allPlan.stream().mapToDouble(x -> {
					return Double.valueOf(x.getDouble("Carbs"));
				}).sum();
				response.put("totalCarbs", totalCarbs.intValue());
				// Double totalCarbsPer=tolalCalories/totalCarbs;
				Double totalCarbsPer = ((totalCarbs * 4) * 100) / totalCalories;
				response.put("totalCarbsPer", totalCarbsPer.intValue());

				totalFat = allPlan.stream().mapToDouble(x -> {
					return Double.valueOf(x.getDouble("Fat"));
				}).sum();

				// Double totalFatPer=tolalCalories/totalFat;
				Double totalFatPer = ((totalFat * 9) * 100) / totalCalories;
				response.put("totalFat", totalFat.intValue());
				response.put("totalFatPer", totalFatPer.intValue());

				totalProtien = allPlan.stream().mapToDouble(x -> {
					return Double.valueOf(x.getDouble("Protien"));
				}).sum();
				// Double totalProtienPer=tolalCalories/totalProtien;
				Double totalProtienPer = ((totalProtien * 4) * 100) / totalCalories;
				response.put("totalProtien", totalProtien.intValue());
				response.put("totalProtienPer", totalProtienPer.intValue());

				totalFiber = allPlan.stream().mapToDouble(x -> {
					return Double.valueOf(x.getDouble("Fiber"));
				}).sum();
				Double totalFiberPer = totalCalories / totalFiber;

				response.put("totalFiber", totalFiber.intValue());
				response.put("totalFiberPer", totalFiberPer.intValue());

				Double calDistribution = (slots7.stream().filter(x -> x != null)
						.mapToDouble(m -> m.getDouble("Calories")).sum())
						+ (slots8.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Calories")).sum());
				Double calDistributionPer = (100 / tolalCalories) * calDistribution;
				response.put("calDistribution", calDistribution.intValue());
				response.put("calDistributionPer", calDistributionPer.intValue());

				response.put("diets", result);

				// WRITE TO CACHE
				if (slotWiseTotalCalories > ApiUtils.getMinus7point5Pper(dbResult.getFilterData().getCalories())
						|| slotWiseTotalCalories < ApiUtils.getPlus5Pper(dbResult.getFilterData().getCalories()))

					// UPDATE DIET PLAN IN CACHE
					saveDietPlan(response, dbResult.getFilterData().getEmail(), date, traceId);
				logger.info("##### " + method + " DIET PLAN CACHED & TOTAL CALORIES [" + slotWiseTotalCalories + "]");
				////////////////////////////////////////////////////////////
				JsonArray dietListArr = response.getJsonArray("diets");
				dietListArr.forEach(action -> {
					if (null != action) {
						JsonObject obj = (JsonObject) action;
						JsonArray dataArr = obj.getJsonArray("data");
						dataArr.forEach(mapper -> {
							if (null != mapper) {
								JsonObject food = (JsonObject) mapper;
								food.remove("Slots");
								food.remove("Season");
								food.remove("AvoidIn");
								food.remove("Remark");
								food.remove("Detox");
								food.remove("Special_diet");
								food.remove("courtesy");
								food.remove("recipe");
								food.remove("steps");
								food.remove("updated_by");
								food.remove("video");
								food.remove("Special_slot");
								food.remove("Ultra_special");
							}
						});
					}
				});
				////////////////////////////////////////////////////////////
			} catch (Exception e) {
				e.printStackTrace();
			}

			promise.complete(response);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return promise.future();
	}

	/**
	 * Get diet drink from stream v3 configuration.
	 * 
	 * @param dbResult
	 * @param prefList
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getDietDrinFromSreamv3Config(DBResult dbResult, List<JsonObject> prefList, String date,
			String traceId) {
		String method = "MongoRepositoryWrapper getDietDrinFromSreamv3Config() " + traceId;

		logger.info("##### " + method + " PREF LIST SIZE -->> " + prefList.size());
		prefList.forEach(pref -> logger
				.info("##### " + method + " PREF LIST ITEM CODES -->> " + pref.getString("itemCode") + ", "));

		JsonObject response = new JsonObject();
		Promise<JsonObject> promise = Promise.promise();

		try {
			Set<String> allFoods = dbResult.getFilterData().getAllPrefood();
			logger.info("##### " + method + " ALL FOODS SIZE (BEFORE) -->> " + allFoods.size());
			if (prefList != null) {
				prefList.forEach(pref -> {
					allFoods.add(pref.getString("code"));
					logger.info("##### " + method + " ALL FOODS ITEM CODES -->> " + pref.getString("itemCode"));
				});
			}
			logger.info("##### " + method + " ALL FOODS SIZE (AFTER) -->> " + allFoods.size());

			try {

				List<JsonObject> finalPrefList = allFoods.stream().map(code -> {
					final JsonObject obj = getMealByCode(code, dbResult.getData());
					if (obj != null) {
						JsonObject prefObj = checkPrefFood(code, prefList);
						if (prefObj != null) {
							// obj.put("portion", prefObj.getInteger("portion"));
							// obj.put("originalPortion", obj.getInteger("portion"));
						}
					}
					return obj;
				}).collect(Collectors.toList());

				List<JsonObject> allPlanList = dbResult.getData().stream().map(mapper -> {
					if (mapper != null) {
						JsonObject prefObj = checkPrefFood(mapper.getString("code"), prefList);
						if (prefObj != null) {
							mapper.put("portion", prefObj.getInteger("portion"));
							mapper.put("originalPortion", mapper.getInteger("portion"));
						}
					}
					return mapper;
				}).collect(Collectors.toList());

				finalPrefList = finalPrefList.stream().filter(x -> x != null)
						.filter(x -> filterAvoidIn(x, dbResult.getFilterData().getDisease()))
						.filter(x -> filterByDietSeason(x))
						.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
						.collect(Collectors.toList());
				logger.info("##### " + method + " FINAL PREF LIST SIZE -->> " + finalPrefList.size());
				for (JsonObject json : finalPrefList) {
					logger.info("##### " + method + " FINAL PREF ITEM CODES -->> " + json.getString("itemCode") + "---"
							+ json.getInteger("portion"));
				}

				logger.info("##### " + method + " ALL PLAN LIST SIZE -->> " + allPlanList.size());
				String allPlanListItemCodes = "";
				for (JsonObject json : allPlanList)
					allPlanListItemCodes += json.getString("itemCode") + ", ";

				logger.info("##### " + method + " allPlanListItemCodes SIZE -->> " + allPlanListItemCodes);

				List<JsonObject> allDiets = new ArrayList<JsonObject>();
//				JsonObject slots = this.config.getJsonObject("slots");
				List<JsonObject> slots0 = FoodFilterUtils.getSlot0Config(allPlanList, dbResult,
						dbResult.getFilterData().getDisease(), dbResult.getFilterData().getCommunity(), allDiets);
//				List<JsonObject> slots1 = FoodFilterUtils.getSlot1Config(allPlanList, dbResult,
//						dbResult.getFilterData().getDisease(), dbResult.getFilterData().getCommunity(), prefList,
//						allDiets, slots.getString("slot1"));
//				allDiets.addAll(slots1);
//				List<JsonObject> slots2 = FoodFilterUtils.getSlot2Config(allPlanList, dbResult,
//						dbResult.getFilterData().getDisease(), finalPrefList, dbResult.getFilterData(), allDiets,
//						slots.getString("slot2"));
//				allDiets.addAll(slots2);
//				List<JsonObject> slots3 = FoodFilterUtils.getSlot3Config(allPlanList, dbResult,
//						dbResult.getFilterData().getDisease(), finalPrefList, allDiets, slots.getString("slot3"));
//				allDiets.addAll(slots3);
//				List<JsonObject> slots4 = FoodFilterUtils.getSlot4Config(allPlanList, dbResult,
//						dbResult.getFilterData().getDisease(), finalPrefList, dbResult.getFilterData(), allDiets,
//						slots.getString("slot4"));
//				allDiets.addAll(slots4);
//				SlotFilter slotFilter5 = FoodFilterUtils.getSlot5Config(allPlanList, dbResult,
//						dbResult.getFilterData().getDisease(), finalPrefList, allDiets, slots.getString("slot5"));
//				List<JsonObject> slots5 = slotFilter5.getDataList();
//				allDiets.addAll(slots5);
//				List<JsonObject> slots6 = FoodFilterUtils.getSlot6Config(allPlanList, dbResult,
//						dbResult.getFilterData().getDisease(), finalPrefList, allDiets, slots.getString("slot6"));
//				allDiets.addAll(slots6);
//				List<JsonObject> slots7 = FoodFilterUtils.getSlot7Config(allPlanList, dbResult,
//						dbResult.getFilterData().getDisease(), finalPrefList, allDiets, slots.getString("slot7"));
//				allDiets.addAll(slots7);
//				SlotFilter slotFilter8 = FoodFilterUtils.getSlot8Config(allPlanList, dbResult,
//						dbResult.getFilterData().getDisease(), finalPrefList, allDiets, slots.getString("slot8"));
//				List<JsonObject> slots8 = slotFilter8.getDataList();

				// JsonObject mongoConf = this.config.getJsonObject("mongo");
				JsonArray slotsJsonArray = this.config.getJsonArray("slots");
				Map<String, List<JsonObject>> map = FoodFilterUtils.getSlotsDiets(allPlanList, dbResult,
						dbResult.getFilterData().getDisease(), dbResult.getFilterData().getCommunity(), prefList,
						allDiets, slotsJsonArray);

				List<JsonObject> slots1 = map.get("slot1");
				List<JsonObject> slots2 = map.get("slot2");
				List<JsonObject> slots3 = map.get("slot3");
				List<JsonObject> slots4 = map.get("slot4");
				List<JsonObject> slots5 = map.get("slot5");
				List<JsonObject> slots6 = map.get("slot6");
				List<JsonObject> slots7 = map.get("slot7");
				List<JsonObject> slots8 = map.get("slot8");

				allDiets.addAll(slots1);
				allDiets.addAll(slots2);
				allDiets.addAll(slots3);
				allDiets.addAll(slots4);
				allDiets.addAll(slots5);
				allDiets.addAll(slots6);
				allDiets.addAll(slots7);
				// Rule-1 (Modified)
				FoodFilterUtils.addPortion("173", .5d, slots8);

				List<JsonObject> allPlan = new ArrayList<JsonObject>();
				// List<JsonObject> allPlanList = new ArrayList<JsonObject>();
				logger.info("##### " + method + " ALL PLAN SIZE (BEFORE) -->> " + allPlan.size());
				allPlan.addAll(slots0);
				logger.info("##### " + method + " SLOTS 0 SIZE (BEFORE) -->> " + slots0.size() + " -- CALORIES -->> "
						+ ApiUtils.getTotalCalories(slots0));
				allPlan.addAll(slots1);
				logger.info("##### " + method + " SLOTS 1 SIZE (BEFORE) -->> " + slots1.size() + " -- CALORIES -->> "
						+ ApiUtils.getTotalCalories(slots1));
				allPlan.addAll(slots2);
				logger.info("##### " + method + " SLOTS 2 SIZE (BEFORE) -->> " + slots2.size() + " -- CALORIES -->> "
						+ ApiUtils.getTotalCalories(slots2));
				allPlan.addAll(slots3);
				logger.info("##### " + method + " SLOTS 3 SIZE (BEFORE) -->> " + slots3.size() + " -- CALORIES -->> "
						+ ApiUtils.getTotalCalories(slots3));
				allPlan.addAll(slots4);
				logger.info("##### " + method + " SLOTS 4 SIZE (BEFORE) -->> " + slots4.size() + " -- CALORIES -->> "
						+ ApiUtils.getTotalCalories(slots4));
				allPlan.addAll(slots5);
				logger.info("##### " + method + " SLOTS 5 SIZE (BEFORE) -->> " + slots5.size() + " -- CALORIES -->> "
						+ ApiUtils.getTotalCalories(slots5));
				allPlan.addAll(slots6);
				logger.info("##### " + method + " SLOTS 6 SIZE (BEFORE) -->> " + slots6.size() + " -- CALORIES -->> "
						+ ApiUtils.getTotalCalories(slots6));
				allPlan.addAll(slots7);
				logger.info("##### " + method + " SLOTS 7 SIZE (BEFORE) -->> " + slots7.size() + " -- CALORIES -->> "
						+ ApiUtils.getTotalCalories(slots7));
				allDiets.addAll(slots8);
				logger.info("##### " + method + " SLOTS 8 SIZE (BEFORE) -->> " + slots8.size() + " -- CALORIES -->> "
						+ ApiUtils.getTotalCalories(slots8));

				// IF NV ITEM SERVED IN DINNER (SLOT 7), THEN ADD GREEN TEA INTO SLOT 8
				if ("NV".equalsIgnoreCase(dbResult.getFilterData().getFoodType())) {
					logger.info("##### " + method + " CUSTOMER IS ['" + dbResult.getFilterData().getFoodType() + "']");
					logger.info("##### " + method + " SLOT 8 SIZE (BEFORE ADDITION) [" + slots8.size() + "]");
					// CHECK IF NV ITEM SERVED IN DINNER ie. SLOT 7
					boolean isGreenTeaItemAlreadtAddedInSlot8 = false;
					boolean isItemNV = false;
					for (JsonObject json : slots7)
						if ("NV".equalsIgnoreCase(json.getString("foodType"))) {
							if (json.getString("itemCode").equalsIgnoreCase("170")) {
								logger.info("##### " + method + " SLOT 7 ITEM [" + json.getString("itemCode")
										+ "] FOUND AS GREEN TEA");
								isGreenTeaItemAlreadtAddedInSlot8 = true;
								break;
							}
							isItemNV = true;
						}

					logger.info("##### " + method + " IS ITEM NON-VEG?? [" + isItemNV + "]");
					logger.info("##### " + method + " isGreenTeaItemAlreadtAddedInSlot8 ["
							+ isGreenTeaItemAlreadtAddedInSlot8 + "]");
					logger.info("##### " + method + " SLOT 8 SIZE (BEFORE ADDITION/REMOVAL) [" + slots8.size() + "]");
					if (isItemNV && !isGreenTeaItemAlreadtAddedInSlot8) {
						JsonObject code170 = getMealByCode("170", dbResult.getData());
						for (JsonObject jsonObj : slots8) {
							logger.info("##### " + method + " SLOT 8 ITEM [" + jsonObj.getString("itemCode") + "]");
							if (!code170.getString("itemCode").equalsIgnoreCase(jsonObj.getString("itemCode"))
									&& "DM".equalsIgnoreCase(jsonObj.getString("Type"))) {
								// TO BE REMOVED MILK ITEMS
								logger.info("##### " + method + " SLOT 8 ITEM IS REMOVED -->> ["
										+ jsonObj.getString("itemCode") + "]");
								slots8.remove(jsonObj);
								break;
							}
						}

						ApiUtils.addFoodItem(code170, slots8);
						logger.info("##### " + method + " SLOT 8 SIZE (AFTER ADDITION) [" + slots8.size() + "]");
					} else {
						logger.info("##### " + method + " SLOT 8 - IF ITEM IS NON-VEG?? [" + isItemNV
								+ "] -- IS GREEN TEA ALREAD ADDED?? -->> [" + isGreenTeaItemAlreadtAddedInSlot8 + "]");
					}
				}

				allPlan.addAll(slots8);
				logger.info("##### " + method + " ALL PLAN SIZE (AFTER) -->> " + allPlan.size());
				Double tolalCalories = 0d;
				Double totalSlotsCalories = 0d;
				Double tolalCaloriesLocal = 0d;
				logger.info("##### TOTAL CALORIES -->> " + tolalCalories);
				tolalCalories = ApiUtils.getTotalCalories(allPlan);
				logger.info("##### " + method + " TOTAL CALORIES ADDED (	) -->> " + tolalCalories);
				logger.info("##### " + method + " RECOMMENDED CALORIES (FROM CUST_PROFILE) -->> "
						+ dbResult.getFilterData().getCalories());

				String status = ApiUtils.getChangedCaloriesStatus("FILTER FOR LEVERS", tolalCalories,
						dbResult.getFilterData().getCalories());
				logger.info("##### " + method + " CALORIES STATUS (START) [" + status + "]");
				if ("L".equals(status)) {

					boolean is1stLeverDone = false;
					boolean is2ndLeverDone = false;
					boolean is3rdLeverDone = false;
					boolean is4thLeverDone = false;
					boolean is5thLeverDone = false;
					boolean is6thLeverDone = false;
					boolean is7thLeverDone = false;
					List<JsonObject> plansListForSlot1And2And3 = new ArrayList<>();
					plansListForSlot1And2And3.addAll(slots1);
					plansListForSlot1And2And3.addAll(slots2);
					plansListForSlot1And2And3.addAll(slots3);
					Double tolalCaloriesOfSlot1And2And3 = ApiUtils.getTotalCalories(plansListForSlot1And2And3);
					double slots123TotalCaloriesPercent = Math
							.round((100 / dbResult.getFilterData().getCalories()) * tolalCaloriesOfSlot1And2And3);
					Double caloriesPlus5Percent = 0d;
					Double caloriesMinus7point5Percent = 0d;
					Double caloriesMidRange = 1600d;

					status = ApiUtils.getChangedCaloriesStatus("0TH LEVER SLOTS 2", tolalCalories,
							dbResult.getFilterData().getCalories());

					// For 1st Lever
					if ("L".equals(status)) {
						List<JsonObject> plansListForSlot4And5And6 = new ArrayList<>();
						plansListForSlot4And5And6.addAll(slots4);
						plansListForSlot4And5And6.addAll(slots5);
						plansListForSlot4And5And6.addAll(slots6);
						Double tolalCaloriesOfSlot4And5And6 = ApiUtils.getTotalCalories(plansListForSlot4And5And6);
						double slots456TotalCaloriesPercent = Math
								.round((100 / dbResult.getFilterData().getCalories()) * tolalCaloriesOfSlot4And5And6);
						logger.debug("##### " + method
								+ " 1ST LEVER  - SLOTS 4 -  tolalCaloriesOfSlot4And5And6 (BEFORE) -->> "
								+ tolalCaloriesOfSlot4And5And6);
						logger.debug("##### " + method
								+ " 1ST LEVER  - SLOTS 4 - slots456TotalCaloriesPercent (BEFORE) -->> "
								+ slots456TotalCaloriesPercent + " %");
						if (tolalCaloriesOfSlot4And5And6 < 350 || slots456TotalCaloriesPercent < 30) {
							// IF REQUIRED THEN: - IN SLOT 4 ONLY
							// 1. Increase portion of roti – make it 2 portions : 1 st lever
							// STILL ITEMS NEED TO BE ADDED AT 3RD LEVER
							// 2. Add raita in lunch: 3rd lever
							// STILL ITEMS NEED TO BE ADDED AT 6TH LEVER
							// 3. Increase portion of subzi to 1.5: 6th lever
							// double updatedCalories = 0.0;
							for (JsonObject action : slots4) {
								JsonObject meanDetail = getMealByCode(action.getString("code"),
										dbResult.getMasterPlan());
								if ("B".equalsIgnoreCase(action.getString("Type"))) {
									action = meanDetail;
									action.put("portion", (action.getInteger("portion") * 2));
									FoodFilterUtils.updateCalories(meanDetail, 2);
									tolalCaloriesLocal = (action.getDouble("Calories") / 2);
									slots4 = FoodFilterUtils.filterByCustCommunity(slots4,
											dbResult.getFilterData().getCommunity(), 4, "");
									slots4 = getSaladOnTopAndThenRemaingItems(slots4, "034");
									slots4 = sortingSlot4SpecificItems(slots4);
									tolalCaloriesOfSlot4And5And6 += action.getDouble("Calories");
									slots456TotalCaloriesPercent = Math
											.round((100 / dbResult.getFilterData().getCalories())
													* tolalCaloriesOfSlot4And5And6);
									logger.info("##### " + method + " 1ST - SLOTS 4 -  'B' FOUND");
									logger.info("##### " + method + " 1ST LEVER - SLOTS 4 - CALORIES FOUND -->> "
											+ action.getDouble("Calories"));
									logger.info(
											"##### " + method + " 1ST LEVER - SLOTS 4 - TOTAL CALCULATED CALORIES -->> "
													+ tolalCaloriesLocal);
									logger.debug("##### " + method
											+ " 1ST LEVER - SLOTS 4 - tolalCaloriesOfSlot4And5And6 (AFTER) -->> "
											+ tolalCaloriesOfSlot4And5And6);
									logger.debug("##### " + method
											+ " 1ST LEVER - SLOTS 4 - slots456TotalCaloriesPercent (AFTER) -->> "
											+ slots456TotalCaloriesPercent + " %");
									is1stLeverDone = true;
									break;
								}
							}

							logger.info(
									"##### " + method + " 1ST LEVER - SLOTS 4 - SIZE (AFTER) -->> " + slots4.size());
							totalSlotsCalories = checkCalories(slots0, slots1, slots2, slots3, slots4, slots5, slots6,
									slots7, slots8);
							logger.info("##### " + method + " ALL SLOTS TOTAL CALORIES (AFTER 1ST LEVER) -->> "
									+ totalSlotsCalories);
						}

						if (is1stLeverDone)
							status = ApiUtils.getChangedCaloriesStatus("1ST LEVER", totalSlotsCalories,
									dbResult.getFilterData().getCalories());
						logger.info("##### " + method + " 1ST LEVER STATUS (AFTER) -->> " + status);
						logger.info(
								"#########################################################################################");
						logger.info("");

						// for 2nd Lever
						if ("L".equals(status)) {
							// 300 calories are fixed in morning: slot 1 + slot 3
							tolalCaloriesOfSlot1And2And3 = ApiUtils.getTotalCalories(plansListForSlot1And2And3);
							slots123TotalCaloriesPercent = Math.round(
									(100 / dbResult.getFilterData().getCalories()) * tolalCaloriesOfSlot1And2And3);
							logger.debug("##### " + method
									+ " 2ND LEVER - SLOTS 2 - tolalCaloriesOfSlot1And2And3 (BEFORE) -->> "
									+ tolalCaloriesOfSlot1And2And3);
							logger.debug("##### " + method
									+ " 2ND LEVER - SLOTS 2 - slots123TotalCaloriesPercent (BEFORE) -->> "
									+ slots123TotalCaloriesPercent + " %");
							if (tolalCaloriesOfSlot1And2And3 < 600 || slots123TotalCaloriesPercent < 45) {
								// IF REQUIRED THEN: - IN SLOT 2 ONLY
								// 1. Increase portion of snack , if portion is 1 then make it 2 - 2 nd lever
								// itemCode-008 Chapati (make it 2 portion)
								// STILL ITEMS NEED TO BE ADDED AT 4TH LEVER
								// 2. Add tea or coffee as per liking: 4th lever
								logger.debug("##### " + method + " 2ND LEVER - SLOTS 2 ITEMS BEFORE -->> " + slots2);
								for (JsonObject action : slots2) {
									JsonObject meanDetail = getMealByCode(action.getString("code"),
											dbResult.getMasterPlan());
									if ("W".equalsIgnoreCase(action.getString("Type"))
											|| "WC".equalsIgnoreCase(action.getString("Type"))
											|| "wcp".equalsIgnoreCase(action.getString("Type"))
											|| "WP".equalsIgnoreCase(action.getString("Type"))
											|| "wpp".equalsIgnoreCase(action.getString("Type"))
											|| "wm".equalsIgnoreCase(action.getString("Type"))) {
										boolean isLeverToBeExecuted = dbResult.getFilterData()
												.getCalories() <= caloriesMidRange ? true : false;
										logger.debug(
												"##### " + method + " 2ND LEVER - SLOTS 2 isLeverToBeExecuted?? -->> "
														+ isLeverToBeExecuted);
										logger.debug("##### " + method + " 2ND LEVER - SLOTS 2 IS WP EXISTS?? -->> "
												+ !action.getString("Type").contains("WP"));
										if ((isLeverToBeExecuted && !action.getString("Type").contains("WP"))
												|| !isLeverToBeExecuted) {
											logger.debug("##### " + method + " 2ND LEVER - SLOTS 2 - '"
													+ action.getString("Type") + "' FOUND");
											FoodFilterUtils.updateCalories(meanDetail, 2);
											action = meanDetail;
											action.put("portion", (meanDetail.getInteger("portion")) * 2);
											tolalCaloriesLocal += (action.getDouble("Calories") / 2);
											slots2 = FoodFilterUtils.filterByCustCommunity(slots2,
													dbResult.getFilterData().getCommunity(), 4, "");
											logger.debug(
													"##### " + method + " 2ND LEVER - SLOTS 2 - CALORIES FOUND -->> "
															+ action.getDouble("Calories"));
											logger.debug("##### " + method
													+ " 2ND LEVER - SLOTS 2 - TOTAL CALCULATED CALORIES -->> "
													+ tolalCaloriesLocal);
											tolalCaloriesOfSlot1And2And3 += action.getDouble("Calories");
											slots123TotalCaloriesPercent = Math
													.round((100 / dbResult.getFilterData().getCalories())
															* tolalCaloriesOfSlot1And2And3);
											logger.debug("##### " + method
													+ " 2ND LEVER - SLOTS 2 - tolalCaloriesOfSlot1And2And3 -->> "
													+ tolalCaloriesOfSlot1And2And3);
											logger.debug("##### " + method
													+ " 2ND LEVER - SLOTS 2 - slots123TotalCaloriesPercent -->> "
													+ slots123TotalCaloriesPercent + " %");
											is2ndLeverDone = true;
										} else {
											logger.debug(
													"##### " + method + " 2ND LEVER - SLOTS 2 is2ndLeverDone -->> ["
															+ is2ndLeverDone + "]");
											logger.debug("##### " + method
													+ " 2ND LEVER - SLOTS 2 'WP'/'WPP' ITEM(S) -->> " + meanDetail);
										}
									}
								}

								logger.debug("##### " + method + " 2ND LEVER SLOTS 2 ITEMS AFTER -->> " + slots2);
								logger.info("##### " + method + " 2ND LEVER - SLOTS 2 - SIZE (AFTER) -->> "
										+ slots2.size());
							}

							totalSlotsCalories = checkCalories(slots0, slots1, slots2, slots3, slots4, slots5, slots6,
									slots7, slots8);
							logger.info("##### " + method + " ALL SLOTS TOTAL CALORIES (AFTER 2ND LEVER) -->> "
									+ totalSlotsCalories);

							if (is2ndLeverDone)
								status = ApiUtils.getChangedCaloriesStatus("2ND LEVER", totalSlotsCalories,
										dbResult.getFilterData().getCalories());
							logger.info("##### " + method + " SLOTS 2 STATUS AFTER 2ND LEVER -->> " + status);
							logger.info(
									"#########################################################################################");
							logger.info("");

							// for 3rd lever
							if ("L".equals(status)) {
								JsonObject code091 = getMealByCode("091", dbResult.getData());
								ApiUtils.addFoodItem(code091, slots4);
								slots4 = sortingSlot4SpecificItems(slots4);
								slots4 = FoodFilterUtils.filterByCustCommunity(slots4,
										dbResult.getFilterData().getCommunity(), 4, "3RD LEVER");
								slots4 = getSaladOnTopAndThenRemaingItems(slots4, "034");
								logger.debug("##### " + method + " 3RD LEVER - SLOTS 4 - ITEMCODE 091 -->> "
										+ code091.toString());
								tolalCaloriesLocal += code091.getDouble("Calories");
								logger.debug(
										"##### " + method + " 3RD LEVER - SLOTS 4 - TOTAL CALCULATED CALORIES -->> "
												+ tolalCaloriesLocal);
								is3rdLeverDone = true;
								logger.debug("##### " + method + " 3RD LEVER - SLOTS 4 - SIZE (AFTER) -->> "
										+ slots4.size());

								totalSlotsCalories = checkCalories(slots0, slots1, slots2, slots3, slots4, slots5,
										slots6, slots7, slots8);
								logger.info(
										"##### " + method + " SLOTS 4 ALL SLOTS TOTAL CALORIES (AFTER 3RD LEVER) -->> "
												+ totalSlotsCalories);

								if (is3rdLeverDone)
									status = ApiUtils.getChangedCaloriesStatus("3RD LEVER", totalSlotsCalories,
											dbResult.getFilterData().getCalories());
								logger.info("##### " + method + " STATUS AFTER 3RD LEVER -->> " + status);
								logger.info(
										"#########################################################################################");
								logger.info("");

								// for 4th lever
								if ("L".equals(status)) {
									List<JsonObject> slot2DMDrinks = finalPrefList.stream()
											.filter(x -> getDietBySlot(x, 2))
											.filter(x -> x.getString("Type").equalsIgnoreCase("DM"))
											.collect(Collectors.toList());
									List<String> community = dbResult.getFilterData().getCommunity();
									boolean isSouthIndian = false;
									if (community.contains("S") || community.contains("SI") || community.contains("s")
											|| community.contains("si"))
										isSouthIndian = true;

									JsonObject slot2DrinksObj = FoodFilterUtils.getTeaOrCoffeeItems(slot2DMDrinks,
											isSouthIndian, dbResult.getData());
									if (slot2DrinksObj != null) {
										logger.debug("##### " + method + " 4TH LEVER - SLOTS 2 - FOUND FOOD -->> "
												+ slot2DrinksObj.getString("itemCode") + " -- "
												+ slot2DrinksObj.getString("Food"));
										ApiUtils.addFoodItem(slot2DrinksObj, slots2);
										logger.debug("##### " + method + " 4TH LEVER - SLOTS 2 - FOOD ADDED -->> "
												+ slot2DrinksObj);
										slots2 = FoodFilterUtils.filterByCustCommunity(slots2,
												dbResult.getFilterData().getCommunity(), 2, "");
										tolalCaloriesLocal += slot2DrinksObj.getDouble("Calories");
										logger.debug("##### " + method
												+ " 4TH LEVER - SLOTS 4 - TOTAL CALCULATED CALORIES -->> "
												+ tolalCaloriesLocal);
										is4thLeverDone = true;
									} else {
										logger.debug("##### " + method
												+ " 4TH LEVER DEFAULT - SLOTS 2 - slot2DrinksObj is NULL");
									}
									logger.debug("##### " + method + " 4TH LEVER - SLOTS 2 - SIZE (AFTER) -->> "
											+ slots2.size());

									totalSlotsCalories = checkCalories(slots0, slots1, slots2, slots3, slots4, slots5,
											slots6, slots7, slots8);
									logger.info("##### " + method
											+ " SLOTS 2 ALL SLOTS TOTAL CALORIES (AFTER 4TH LEVER) -->> "
											+ totalSlotsCalories);

									if (is4thLeverDone)
										status = ApiUtils.getChangedCaloriesStatus("4TH LEVER", totalSlotsCalories,
												dbResult.getFilterData().getCalories());
									logger.info("##### " + method + "SLOTS 2 4TH LEVER STATUS (AFTER) -->> " + status);
									logger.info(
											"#########################################################################################");
									logger.info("");

									// for 5th lever
									if ("L".equals(status)) {
										List<JsonObject> plansListForSlot6And7And8 = new ArrayList<>();
										plansListForSlot6And7And8.addAll(slots6);
										plansListForSlot6And7And8.addAll(slots7);
										plansListForSlot6And7And8.addAll(slots8);
										Double tolalCaloriesOfSlot6And7And8 = ApiUtils
												.getTotalCalories(plansListForSlot6And7And8);
										// IF REQUIRED THEN: - IN SLOT 5 ONLY
										// 1. In SLOT 5. Add tea or coffee in slot 6: 5th lever
										List<JsonObject> slot6DMDrinks = finalPrefList.stream()
												.filter(x -> getDietBySlot(x, 6))
												.filter(x -> x.getString("Type").equalsIgnoreCase("DM"))
												.collect(Collectors.toList());
										JsonObject slot6DrinksObj = FoodFilterUtils.geFilteredItems(slot6DMDrinks,
												allDiets);

										List<JsonObject> list = new ArrayList<JsonObject>();
										if (null == slot6DrinksObj) {
											logger.info("##### " + method
													+ " 5TH LEVER - SLOTS 6 - HAS NO ITEM AVAILABLE FOR TYPE 'A'");
											for (JsonObject json : slots6) {
												if ("060".equalsIgnoreCase(json.getString("itemCode"))) {
													list.add(getMealByCode("060", dbResult.getData()));
													break;
												} else if ("061".equalsIgnoreCase(json.getString("itemCode"))) {
													list.add(getMealByCode("061", dbResult.getData()));
													break;
												} else if ("063".equalsIgnoreCase(json.getString("itemCode"))) {
													list.add(getMealByCode("063", dbResult.getData()));
													break;
												}
											}

											slot6DrinksObj = FoodFilterUtils.getItemAfterShuffle(list);
										}

										if (null != slot6DrinksObj) {

											for (JsonObject str : list)
												if (str.getString("itemCode")
														.equalsIgnoreCase(slot6DrinksObj.getString("itemCode"))) {
													ApiUtils.addFoodItem(slot6DrinksObj, slots6);
													break;
												}

											slots6 = FoodFilterUtils.filterByCustCommunity(slots6,
													dbResult.getFilterData().getCommunity(), 6, "");

											tolalCaloriesOfSlot6And7And8 += slot6DrinksObj.getDouble("Calories");
											tolalCaloriesLocal += slot6DrinksObj.getDouble("Calories");
											logger.debug("##### " + method
													+ " 5TH LEVER - SLOTS 6 - TOTAL CALCULATED CALORIES -->> "
													+ tolalCaloriesLocal);
											is5thLeverDone = true;
										}
										logger.debug("##### " + method + " 5TH LEVER - SLOTS 6 - SIZE (AFTER) -->> "
												+ slots6.size());

										totalSlotsCalories = checkCalories(slots0, slots1, slots2, slots3, slots4,
												slots5, slots6, slots7, slots8);
										logger.info(
												"##### " + method + " ALL SLOTS TOTAL CALORIES (AFTER 5TH LEVER) -->> "
														+ totalSlotsCalories);

										if (is5thLeverDone)
											status = ApiUtils.getChangedCaloriesStatus("5TH LEVER SLOTS 6",
													totalSlotsCalories, dbResult.getFilterData().getCalories());
										logger.info("##### " + method + " 5TH LEVER - SLOTS 6 - STATUS (AFTER) -->> "
												+ status);
										logger.info(
												"#########################################################################################");
										logger.info("");

										// 6th lever
										if ("L".equals(status)) {
											for (JsonObject action : slots4) {
												JsonObject meanDetail = getMealByCode(action.getString("code"),
														dbResult.getMasterPlan());
												if ("A".equalsIgnoreCase(action.getString("Type"))) {
													logger.debug(
															"##### " + method + " 6TH LEVER - SLOTS 4 - 'A' FOUND");
													FoodFilterUtils.updateCalories(meanDetail, 1.5);
													action = meanDetail;
													action.put("portion", 1.5);
													tolalCaloriesLocal += (action.getDouble("Calories") / 1.5);
													tolalCaloriesOfSlot4And5And6 += action.getDouble("Calories");
													slots456TotalCaloriesPercent = Math
															.round((100 / dbResult.getFilterData().getCalories())
																	* slots456TotalCaloriesPercent);
													slots4 = sortingSlot4SpecificItems(slots4);
													slots4 = FoodFilterUtils.filterByCustCommunity(slots4,
															dbResult.getFilterData().getCommunity(), 4, "");
													slots4 = getSaladOnTopAndThenRemaingItems(slots4, "034");
													is6thLeverDone = true;
													logger.debug("##### " + method
															+ " 6TH LEVER - SLOTS 4 - TOTAL CALCULATED CALORIES -->> "
															+ tolalCaloriesLocal);
													break;
												}
											}
											logger.debug("##### " + method + " 6TH LEVER - SLOTS 4 - SIZE (AFTER) -->> "
													+ slots4.size());

											totalSlotsCalories = checkCalories(slots0, slots1, slots2, slots3, slots4,
													slots5, slots6, slots7, slots8);
											logger.info("##### " + method
													+ " ALL SLOTS TOTAL CALORIES  (AFTER 6TH LEVER)-->> "
													+ totalSlotsCalories);

											if (is6thLeverDone)
												status = ApiUtils.getChangedCaloriesStatus("6TH LEVER SLOTS 4",
														totalSlotsCalories, dbResult.getFilterData().getCalories());
											logger.info("##### " + method + " SLOTS 4 6TH LEVER STATUS (AFTER) -->> "
													+ status);
											logger.info(
													"#########################################################################################");
											logger.info("");

											// for 7th lever
											if ("L".equals(status)) {
												logger.debug("##### " + method
														+ " 7TH LEVER - SLOTS 4 - SIZE (BEFORE) -->> " + slots4.size());
												List<JsonObject> slot4ListCA = slots4.stream()
														.filter(x -> x.getString("Type").equalsIgnoreCase("C")
																|| x.getString("Type").equalsIgnoreCase("A"))
														.collect(Collectors.toList());
												String dietToBeAddedAorC = ("A"
														.equalsIgnoreCase(slot4ListCA.get(0).getString("Type"))) ? "C"
																: "A";
												logger.debug("##### " + method + " dietToBeAddedAorC AFTER -->> "
														+ dietToBeAddedAorC);
												logger.debug("##### " + method
														+ " 7TH LEVER - SLOT 4 DIET TYPE TO BE ADDED ['"
														+ dietToBeAddedAorC + "']");
												List<JsonObject> slot4ListForAorC = dbResult.getData().stream()
														.filter(x -> x.getString("Type")
																.equalsIgnoreCase(dietToBeAddedAorC))
														.filter(x -> getDietBySlot(x, 4))
														.filter(x -> filterAvoidIn(x,
																dbResult.getFilterData().getDisease()))
														.filter(x -> filterByCustFoodType(x,
																dbResult.getFilterData().getFoodType()))
														.collect(Collectors.toList());
												slot4ListForAorC = FoodFilterUtils.filterByCustCommunity(
														slot4ListForAorC, dbResult.getFilterData().getCommunity(), 4,
														"");
												slot4ListForAorC = FoodFilterUtils.getPrefListFromCommunity("SLOT 7",
														slot4ListForAorC, dbResult.getFilterData().getCommunity());
												List<JsonObject> prefListSlot7 = FoodFilterUtils
														.getPrefListFromCommunity("SLOT 7", prefList,
																dbResult.getFilterData().getCommunity());
												JsonObject slot4Obj = FoodFilterUtils.geFilteredData(slot4ListForAorC,
														prefListSlot7);

												logger.debug("##### " + method
														+ " 7TH LEVER - SLOT 4 OBJECT TO BE ADDED -->> " + slot4Obj);
												if (!slot4Obj.isEmpty() && null != slot4Obj.getString("itemCode")) {
													logger.debug("##### " + method + " 7TH LEVER - SLOT 4 ITEM ["
															+ slot4Obj.getString("itemCode") + "] - TYPE -->> ["
															+ slot4Obj.getString("Type") + "]");
													ApiUtils.addFoodItem(slot4Obj, slots4);
													slots4 = sortingSlot4SpecificItems(slots4);
													tolalCaloriesLocal += slot4Obj.getDouble("Calories");
													logger.debug("##### " + method
															+ " 7TH LEVER - SLOT 4 - TOTAL CALCULATED CALORIES -->> "
															+ tolalCaloriesLocal);
													slots4 = sortingSlot4SpecificItems(slots4);
													is7thLeverDone = true;
												}
												slots4 = getSaladOnTopAndThenRemaingItems(slots4, "034");
												logger.debug("##### " + method
														+ " 7TH LEVER - SLOT 4 - SIZE (AFTER) -->> " + slots4.size());

												totalSlotsCalories = checkCalories(slots0, slots1, slots2, slots3,
														slots4, slots5, slots6, slots7, slots8);

												logger.info("##### " + method
														+ " ALL SLOTS TOTAL CALORIES (AFTER 7TH LEVER) -->> "
														+ totalSlotsCalories);
												logger.info(
														"#########################################################################################");
												logger.info("");

												allPlan.clear();
												allPlan.addAll(slots0);
												allPlan.addAll(slots1);
												allPlan.addAll(slots2);
												allPlan.addAll(slots3);
												allPlan.addAll(slots4);
												allPlan.addAll(slots5);
												allPlan.addAll(slots6);
												allPlan.addAll(slots7);
												allPlan.addAll(slots8);

												totalSlotsCalories = ApiUtils.getTotalCalories(allPlan);
												logger.info("##### " + method
														+ " FINAL SLOTS TOTAL CALORIES (INSIDE LEVER) -->> "
														+ totalSlotsCalories);

												logger.info("##### " + method
														+ " 7TH LEVER - SLOTS 4 - is7thLeverDone?? -->> "
														+ is7thLeverDone);
												logger.info("##### " + method
														+ " 7TH LEVER - SLOTS 4 - TOTAL CALCULTAED CALORIES -->> "
														+ totalSlotsCalories);
												caloriesPlus5Percent = ApiUtils
														.getPlus5Pper(dbResult.getFilterData().getCalories());
												logger.info("##### " + method
														+ " 7TH LEVER - SLOTS 4 - CALORIES PLUS 5% -->> "
														+ caloriesPlus5Percent);
												caloriesMinus7point5Percent = ApiUtils
														.getMinus7point5Pper(dbResult.getFilterData().getCalories());
												logger.info("##### " + method
														+ " 7TH LEVER - SLOTS 4 - CALORIES MINUS 5% -->> "
														+ caloriesMinus7point5Percent);
												if (totalSlotsCalories > caloriesPlus5Percent) {
													logger.info("##### " + method
															+ " 7TH LEVER - SLOTS 4 - TOTAl CALORIES " + tolalCalories
															+ " IS GREATER THAN RECOMMENDED CALORIES PLUS 5% -->> "
															+ caloriesPlus5Percent);
												} else if (totalSlotsCalories < caloriesMinus7point5Percent) {
													logger.info("##### " + method
															+ " 7TH LEVER - SLOTS 4 - TOTAl CALORIES " + tolalCalories
															+ " IS LESS THAN RECOMMENDED CALORIES MINUS 5% -->> "
															+ caloriesMinus7point5Percent);
												}
											}
										}
									}
								}
							}
						}
					}

					caloriesPlus5Percent = ApiUtils.getPlus5Pper(dbResult.getFilterData().getCalories());
					caloriesMinus7point5Percent = ApiUtils.getMinus7point5Pper(dbResult.getFilterData().getCalories());
					if (totalSlotsCalories > caloriesPlus5Percent)
						logger.info("##### " + method + " TOTAl CALORIES [" + totalSlotsCalories
								+ "] - GREATER THAN RECOMMENDED CALORIES PLUS 5% -->> [" + caloriesMinus7point5Percent
								+ "]");
					else if (totalSlotsCalories < caloriesMinus7point5Percent)
						logger.info("##### " + method + " TOTAl CALORIES [" + totalSlotsCalories
								+ "] - LESS THAN RECOMMENDED CALORIES MINUS 5% -->> [" + caloriesMinus7point5Percent
								+ "]");

					//////////////////////////////////////// OLD CODE - START
					//////////////////////////////////////// /////////////////////////////////////////////

					/*
					 * slots2.forEach(action -> { JsonObject
					 * meanDetail=getMealByCode(action.getString("code"), dbResult.getMasterPlan());
					 * if("WP".equalsIgnoreCase(action.getString("Type"))) {
					 * FoodFilterUtils.updateCalories(meanDetail, 2); action=meanDetail;
					 * action.put("portion", 2);
					 * 
					 * }else if("WC".equalsIgnoreCase(action.getString("Type"))) {
					 * FoodFilterUtils.updateCalories(meanDetail, 2); action=meanDetail;
					 * action.put("portion", 2); } else
					 * if("W".equalsIgnoreCase(action.getString("Type"))) {
					 * FoodFilterUtils.updateCalories(meanDetail, 2); action=meanDetail;
					 * action.put("portion", 2); }else
					 * if("B".equalsIgnoreCase(action.getString("Type"))) {
					 * FoodFilterUtils.updateCalories(meanDetail, 2); action=meanDetail;
					 * action.put("portion", 2); }
					 * 
					 * });
					 * 
					 * List<JsonObject> slot2Drin=finalPrefList.stream().filter(x ->
					 * getDietBySlot(x,
					 * 2)).filter(x->x.getString("Type").equalsIgnoreCase("DM")).collect(Collectors.
					 * toList()); JsonObject slot2DrinObj=
					 * FoodFilterUtils.geFilteredData(slot2Drin,allDiets); if(slot2DrinObj!=null) {
					 * ApiUtils.addFoodItem(slot2DrinObj,slots2);
					 * ApiUtils.addFoodItem(slot2DrinObj,allPlan); }
					 * 
					 * 
					 * status=ApiUtils.getStatus(ApiUtils.getTotalCalories(allPlan),
					 * dbResult.getFilterData().getCalories());
					 * 
					 * if("L".equals(status)) {
					 * 
					 * slots4.forEach(action -> { JsonObject
					 * meanDetail=getMealByCode(action.getString("code"), dbResult.getMasterPlan());
					 * 
					 * if("B".equalsIgnoreCase(action.getString("Type"))) {
					 * FoodFilterUtils.updateCalories(meanDetail, 2); action=meanDetail;
					 * action.put("portion", 2); }
					 * 
					 * });
					 * 
					 * JsonObject code091=getMealByCode("091", dbResult.getData());
					 * ApiUtils.addFoodItem(code091,slots4); ApiUtils.addFoodItem(code091,allPlan);
					 * 
					 * List<JsonObject> slot6Drin=finalPrefList.stream().filter(x ->
					 * getDietBySlot(x,
					 * 6)).filter(x->x.getString("Type").equalsIgnoreCase("DM")).collect(Collectors.
					 * toList()); if(slot6Drin==null || slot6Drin.isEmpty()) {
					 * slot6Drin=finalPrefList.stream().filter(x -> getDietBySlot(x,
					 * 6)).filter(x->getDrinks(x)).collect(Collectors.toList()); }
					 * logger.info("**slot6Drin**"+slot6Drin.size()); JsonObject slot6DrinObj=
					 * FoodFilterUtils.geFilteredData(slot6Drin,allDiets);
					 * 
					 * if(slot6DrinObj!=null) { ApiUtils.addFoodItem(slot6DrinObj,slots6);
					 * ApiUtils.addFoodItem(slot6DrinObj,allPlan); }
					 * 
					 * }
					 */
					///////////////////////////////////// OLD CODE - END
					///////////////////////////////////// /////////////////////////////////////////////

				} else if ("H".equals(status)) {
					// 032 TO BE DROPPED FROM SLOT 3
//					for (JsonObject json : slots3) {
//						JsonObject meanDetail = getMealByCode(json.getString("code"), dbResult.getMasterPlan());
//						if ("032".equalsIgnoreCase(json.getString("itemCode"))) {
//							int portion = json.getInteger("portion");
//							logger.info("##### " + method + " 0TH LEVER SLOTS 3 '032' FOUND");
//							logger.info("##### 0TH LEVER - SLOTS 3 - FETCHED CALORIES (BEFORE) -->> " + json.getDouble("Calories"));
//							double calories = json.getDouble("Calories") / 2;
//							logger.info("##### 0TH LEVER - SLOTS 3 - FETCHED CALORIES (AFTER) -->> " + calories);
//							FoodFilterUtils.updateCalories(meanDetail, 0.5);
//							json = meanDetail;
//							json.put("portion", portion / 2);
//							tolalCalories = tolalCalories - calories;
//							logger.info("##### 0TH LEVER - SLOTS 3 - TOTAL CALCULATED CALORIES -->> " + tolalCalories);
//							break;
//						}
//					}
//					
//					logger.info("##### " + method + " 0TH LEVER - 032 ITEM BECOME HALF (5) FOR SLOT 3");

				} // END

				List<JsonObject> result = new ArrayList<JsonObject>();
				Double slotWiseTotalCalories = 0d;

				// SLOT 0
				JsonObject slotObject0 = new JsonObject();
				// slotObject0.put("time", ApiUtils.getTimeForSlot(0));
				slotObject0.put("time", dbResult.getFilterData().getSlot0());
				slotObject0.put("message", dbResult.getFilterData().getSlot0Message());
				slotObject0.put("slot", 0);
				slotObject0.put("Locked", false);
				slotObject0.put("Remark",
						"2 glasses of warm water at this time will help to clean your stomach and  improve digestion.");

				slotObject0.put("data", slots0);
				Double totalCalories = slots0.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Calories"))
						.sum();
				Double totalCarbs = slots0.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Carbs")).sum();
				Double totalFat = slots0.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fat")).sum();
				Double totalProtien = slots0.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Protien"))
						.sum();
				Double totalFiber = slots0.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fiber")).sum();
				slotObject0.put("totalCalories", ApiUtils.getDecimal(totalCalories));
				slotObject0.put("totalCarbs", ApiUtils.getDecimal(totalCarbs));
				slotObject0.put("totalFat", ApiUtils.getDecimal(totalFat));
				slotObject0.put("totalProtien", ApiUtils.getDecimal(totalProtien));
				slotObject0.put("totalFiber", ApiUtils.getDecimal(totalFiber));
				result.add(slotObject0);
				slotWiseTotalCalories += totalCalories;
				logger.info("##### " + method + " SLOT 0 TOTAL CALORIES -->> [" + totalCalories + "]");

				// SLOT 1
				JsonObject slotObject1 = new JsonObject();
				// slotObject1.put("time", ApiUtils.getTimeForSlot(1));
				slotObject1.put("time", dbResult.getFilterData().getSlot1());
				slotObject1.put("message", dbResult.getFilterData().getSlot1Message());
				slotObject1.put("slot", 1);
				slotObject1.put("Locked", false);
				slotObject1.put("data", slots1);
				slotObject1.put("Remark", "Fruits, especially apple or banana is the best choice at this time.");

				totalCalories = slots1.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Calories")).sum();
				totalCarbs = slots1.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Carbs")).sum();
				totalFat = slots1.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fat")).sum();
				totalProtien = slots1.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Protien")).sum();
				totalFiber = slots1.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fiber")).sum();
				slotObject1.put("totalCalories", ApiUtils.getDecimal(totalCalories));
				slotObject1.put("totalCarbs", ApiUtils.getDecimal(totalCarbs));
				slotObject1.put("totalFat", ApiUtils.getDecimal(totalFat));
				slotObject1.put("totalProtien", ApiUtils.getDecimal(totalProtien));
				slotObject1.put("totalFiber", ApiUtils.getDecimal(totalFiber));
				result.add(slotObject1);
				slotWiseTotalCalories += totalCalories;
				logger.info("##### " + method + " SLOT 1 TOTAL CALORIES -->> [" + totalCalories + "]");

				// SLOT 2
				JsonObject slotObject2 = new JsonObject();
				// slotObject2.put("time", ApiUtils.getTimeForSlot(2));
				slotObject2.put("time", dbResult.getFilterData().getSlot2());
				slotObject2.put("message",
						(null != dbResult.getFilterData().getSlot2Message()
								&& !"".equalsIgnoreCase(dbResult.getFilterData().getSlot2Message()))
										? dbResult.getFilterData().getSlot2Message()
										: "BreakFast");
				slotObject2.put("slot", 2);
				slotObject2.put("data", slots2);
				slotObject2.put("Locked", false);
				slotObject2.put("Remark",
						"Change it in each day of week. Avoid milk or coffee or tea. Black coffe or tea or green tea is good.");

				totalCalories = slots2.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Calories")).sum();
				totalCarbs = slots2.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Carbs")).sum();
				totalFat = slots2.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fat")).sum();
				totalProtien = slots2.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Protien")).sum();
				totalFiber = slots2.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fiber")).sum();
				slotObject2.put("totalCalories", ApiUtils.getDecimal(totalCalories));
				slotObject2.put("totalCarbs", ApiUtils.getDecimal(totalCarbs));
				slotObject2.put("totalFat", ApiUtils.getDecimal(totalFat));
				slotObject2.put("totalProtien", ApiUtils.getDecimal(totalProtien));
				slotObject1.put("totalFiber", ApiUtils.getDecimal(totalFiber));
				result.add(slotObject2);
				slotWiseTotalCalories += totalCalories;
				logger.info("##### " + method + " SLOT 2 TOTAL CALORIES -->> [" + totalCalories + "]");

				// SLOT 3
				JsonObject slotObject3 = new JsonObject();
				// slotObject3.put("time", ApiUtils.getTimeForSlot(3));
				slotObject3.put("time", dbResult.getFilterData().getSlot3());
				slotObject3.put("message", dbResult.getFilterData().getSlot3Message());
				slotObject3.put("slot", 3);
				slotObject3.put("Locked", false);
				slotObject3.put("data", slots3);
				slotObject3.put("Remark",
						"Fruit ( if not taken early morning) or butterlmilk or coconut water or nuts are best choices");

				totalCalories = slots3.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Calories")).sum();
				totalCarbs = slots3.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Carbs")).sum();
				totalFat = slots3.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fat")).sum();
				totalProtien = slots3.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Protien")).sum();
				totalFiber = slots3.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fiber")).sum();
				slotObject3.put("totalCalories", ApiUtils.getDecimal(totalCalories));
				slotObject3.put("totalCarbs", ApiUtils.getDecimal(totalCarbs));
				slotObject3.put("totalFat", ApiUtils.getDecimal(totalFat));
				slotObject3.put("totalProtien", ApiUtils.getDecimal(totalProtien));
				slotObject3.put("totalFiber", ApiUtils.getDecimal(totalFiber));
				result.add(slotObject3);
				slotWiseTotalCalories += totalCalories;
				logger.info("##### " + method + " SLOT 3 TOTAL CALORIES -->> [" + totalCalories + "]");

				// SLOT 4
				JsonObject slotObject4 = new JsonObject();
				// slotObject4.put("time", ApiUtils.getTimeForSlot(4));
				slotObject4.put("time", dbResult.getFilterData().getSlot4());
				slotObject4.put("message", dbResult.getFilterData().getSlot4Message());
				slotObject4.put("slot", 4);
				slotObject4.put("Locked", false);
				slotObject4.put("data", slots4);
				slotObject4.put("Remark",
						"Eating salad 15 minutes before lunch is must. Use whole wheat aatta. Use mustard, til or coconut oil for cooking");

				totalCalories = slots4.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Calories")).sum();
				totalCarbs = slots4.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Carbs")).sum();
				totalFat = slots4.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fat")).sum();
				totalProtien = slots4.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Protien")).sum();
				totalFiber = slots4.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fiber")).sum();
				slotObject4.put("totalCalories", ApiUtils.getDecimal(totalCalories));
				slotObject4.put("totalCarbs", ApiUtils.getDecimal(totalCarbs));
				slotObject4.put("totalFat", ApiUtils.getDecimal(totalFat));
				slotObject4.put("totalProtien", ApiUtils.getDecimal(totalProtien));
				slotObject4.put("totalFiber", ApiUtils.getDecimal(totalFiber));
				result.add(slotObject4);
				slotWiseTotalCalories += totalCalories;
				logger.info("##### " + method + " SLOT 4 TOTAL CALORIES -->> [" + totalCalories + "]");

				// SLOT 5
				JsonObject slotObject5 = new JsonObject();
				// slotObject5.put("time", ApiUtils.getTimeForSlot(5));
				slotObject5.put("time", dbResult.getFilterData().getSlot5());
				slotObject5.put("message", dbResult.getFilterData().getSlot5Message());
				slotObject5.put("slot", 5);
				slotObject5.put("data", slots5);
				slotObject5.put("Remark", "Drinking green tea at this time will boost your metabolism.");
				// slotObject5.put("Locked", slotFilter5.isLocked());
				slotObject5.put("Locked", true);

				totalCalories = slots5.stream().filter(x -> x != null).filter(x -> x != null)
						.mapToDouble(m -> m.getDouble("Calories")).sum();
				totalCarbs = slots5.stream().filter(x -> x != null).filter(x -> x != null)
						.mapToDouble(m -> m.getDouble("Carbs")).sum();
				totalFat = slots5.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fat")).sum();
				totalProtien = slots5.stream().filter(x -> x != null).filter(x -> x != null)
						.mapToDouble(m -> m.getDouble("Protien")).sum();
				totalFiber = slots5.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fiber")).sum();
				slotObject5.put("totalCalories", ApiUtils.getDecimal(totalCalories));
				slotObject5.put("totalCarbs", ApiUtils.getDecimal(totalCarbs));
				slotObject5.put("totalFat", ApiUtils.getDecimal(totalFat));
				slotObject5.put("totalProtien", ApiUtils.getDecimal(totalProtien));
				slotObject5.put("totalFiber", ApiUtils.getDecimal(totalFiber));
				result.add(slotObject5);
				slotWiseTotalCalories += totalCalories;
				logger.info("##### " + method + " SLOT 5 TOTAL CALORIES -->> [" + totalCalories + "]");

				// SLOT 6
				JsonObject slotObject6 = new JsonObject();
				// slotObject6.put("time", ApiUtils.getTimeForSlot(6));
				slotObject6.put("time", dbResult.getFilterData().getSlot6());
				slotObject6.put("message", dbResult.getFilterData().getSlot6Message());
				slotObject6.put("slot", 6);
				slotObject6.put("data", slots6);
				slotObject6.put("Locked", false);

				totalCalories = slots6.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Calories")).sum();
				totalCarbs = slots6.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Carbs")).sum();
				totalFat = slots6.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fat")).sum();
				totalProtien = slots6.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Protien")).sum();
				totalFiber = slots6.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fiber")).sum();
				slotObject6.put("totalCalories", ApiUtils.getDecimal(totalCalories));
				slotObject6.put("totalCarbs", ApiUtils.getDecimal(totalCarbs));
				slotObject6.put("totalFat", ApiUtils.getDecimal(totalFat));
				slotObject6.put("totalProtien", ApiUtils.getDecimal(totalProtien));
				slotObject6.put("totalFiber", ApiUtils.getDecimal(totalFiber));
				slotObject6.put("Remark",
						"Makhane or dhokla or soup are good options. This meal is must to reduce dinner intake");
				result.add(slotObject6);
				slotWiseTotalCalories += totalCalories;
				logger.info("##### " + method + " SLOT 6 TOTAL CALORIES -->> [" + totalCalories + "]");

				// SLOT 7
				JsonObject slotObject7 = new JsonObject();
				// slotObject7.put("time", ApiUtils.getTimeForSlot(7));
				slotObject7.put("time", dbResult.getFilterData().getSlot7());
				slotObject7.put("message", dbResult.getFilterData().getSlot7Message());
				slotObject7.put("slot", 7);
				slotObject7.put("Locked", false);
				slotObject7.put("data", slots7);
				slotObject7.put("Remark",
						"should be 50% of lunch. Avoid roti or rice. Fruits, chila, omlette, sprouts, are good options.");

				totalCalories = slots7.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Calories")).sum();
				totalCarbs = slots7.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Carbs")).sum();
				totalFat = slots7.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fat")).sum();
				totalProtien = slots7.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Protien")).sum();
				totalFiber = slots7.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fiber")).sum();
				slotObject7.put("totalCalories", ApiUtils.getDecimal(totalCalories));
				slotObject7.put("totalCarbs", ApiUtils.getDecimal(totalCarbs));
				slotObject7.put("totalFat", ApiUtils.getDecimal(totalFat));
				slotObject7.put("totalProtien", ApiUtils.getDecimal(totalProtien));
				slotObject7.put("totalFiber", ApiUtils.getDecimal(totalFiber));
				result.add(slotObject7);
				slotWiseTotalCalories += totalCalories;
				logger.info("##### " + method + " SLOT 7 TOTAL CALORIES -->> [" + totalCalories + "]");

				// SLOT 8
				JsonObject slotObject8 = new JsonObject();
				// slotObject8.put("time", ApiUtils.getTimeForSlot(8));
				slotObject8.put("time", dbResult.getFilterData().getSlot8());
				slotObject8.put("message", dbResult.getFilterData().getSlot8Message());
				slotObject8.put("slot", 8);
				// slotObject8.put("Locked", slotFilter8.isLocked());
				slotObject8.put("Locked", true);
				slotObject8.put("data", slots8);
				slotObject8.put("Remark", "Haldi milk is good for sound sleep n immunity.");

				totalCalories = slots8.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Calories")).sum();
				totalCarbs = slots8.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Carbs")).sum();
				totalFat = slots8.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fat")).sum();
				totalProtien = slots8.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Protien")).sum();
				totalFiber = slots8.stream().filter(x -> x != null).filter(x -> x != null)
						.mapToDouble(m -> m.getDouble("Fiber")).sum();
				slotObject8.put("totalCalories", ApiUtils.getDecimal(totalCalories));
				slotObject8.put("totalCarbs", ApiUtils.getDecimal(totalCarbs));
				slotObject8.put("totalFat", ApiUtils.getDecimal(totalFat));
				slotObject8.put("totalProtien", ApiUtils.getDecimal(totalProtien));
				slotObject8.put("totalFiber", ApiUtils.getDecimal(totalFiber));
				result.add(slotObject8);
				slotWiseTotalCalories += totalCalories;
				logger.info("##### " + method + " SLOT 8 TOTAL CALORIES -->> [" + totalCalories + "]");

				allPlan = allPlan.stream().filter(x -> x != null).collect(Collectors.toList());
				totalCalories = allPlan.stream().mapToDouble(x -> {
					return Double.valueOf(x.getDouble("Calories"));
				}).sum();

				logger.info("##### " + method + "    ALL SLOTS CALORIES -->> [" + slotWiseTotalCalories + "]");
				logger.info("##### " + method + "  RECOMMENDED CALORIES -->> [" + dbResult.getFilterData().getCalories()
						+ "]");

				response.put("calStatus", status);
				response.put("totalCal", slotWiseTotalCalories);
				response.put("recomended", dbResult.getFilterData().getCalories());
				response.put("tolalCalories", slotWiseTotalCalories);
				Double totalCaloriesPer = ((slotWiseTotalCalories * 100)
						/ dbResult.getFilterData().getCalories()) > 100.0 ? 100
								: ((slotWiseTotalCalories * 100) / dbResult.getFilterData().getCalories());
				response.put("totalCalories", slotWiseTotalCalories);
				response.put("totalCaloriesPer", totalCaloriesPer.intValue());

				totalCarbs = allPlan.stream().mapToDouble(x -> {
					return Double.valueOf(x.getDouble("Carbs"));
				}).sum();
				response.put("totalCarbs", totalCarbs.intValue());
				// Double totalCarbsPer=tolalCalories/totalCarbs;
				Double totalCarbsPer = ((totalCarbs * 4) * 100) / totalCalories;
				response.put("totalCarbsPer", totalCarbsPer.intValue());

				totalFat = allPlan.stream().mapToDouble(x -> {
					return Double.valueOf(x.getDouble("Fat"));
				}).sum();

				// Double totalFatPer=tolalCalories/totalFat;
				Double totalFatPer = ((totalFat * 9) * 100) / totalCalories;
				response.put("totalFat", totalFat.intValue());
				response.put("totalFatPer", totalFatPer.intValue());

				totalProtien = allPlan.stream().mapToDouble(x -> {
					return Double.valueOf(x.getDouble("Protien"));
				}).sum();
				// Double totalProtienPer=tolalCalories/totalProtien;
				Double totalProtienPer = ((totalProtien * 4) * 100) / totalCalories;
				response.put("totalProtien", totalProtien.intValue());
				response.put("totalProtienPer", totalProtienPer.intValue());

				totalFiber = allPlan.stream().mapToDouble(x -> {
					return Double.valueOf(x.getDouble("Fiber"));
				}).sum();
				Double totalFiberPer = totalCalories / totalFiber;

				response.put("totalFiber", totalFiber.intValue());
				response.put("totalFiberPer", totalFiberPer.intValue());

				Double calDistribution = (slots7.stream().filter(x -> x != null)
						.mapToDouble(m -> m.getDouble("Calories")).sum())
						+ (slots8.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Calories")).sum());
				// Double calDistributionPer = tolalCalories/calDistribution;
				Double calDistributionPer = (100 / tolalCalories) * calDistribution;
				response.put("calDistribution", calDistribution.intValue());
				response.put("calDistributionPer", calDistributionPer.intValue());

				response.put("diets", result);

				// WRITE TO CACHE
				if (slotWiseTotalCalories > ApiUtils.getMinus7point5Pper(dbResult.getFilterData().getCalories())
						|| slotWiseTotalCalories < ApiUtils.getPlus5Pper(dbResult.getFilterData().getCalories()))
					saveDietPlan(response, dbResult.getFilterData().getEmail(), date, traceId);

			} catch (Exception e) {
				e.printStackTrace();
			}

			promise.complete(response);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return promise.future();

	}

	/**
	 * Get diet drink from stream.
	 * 
	 * @param dbResult
	 * @param prefList
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getDietDrinFromSream(DBResult dbResult, List<JsonObject> prefList, String traceId) {
		String method = "MongoRepositoryWrapper getDietDrinFromSream() " + traceId;
		JsonObject response = new JsonObject();
		Promise<JsonObject> promise = Promise.promise();
		List<JsonObject> data = dbResult.getData().stream()
				.filter(x -> filterAvoidIn(x, dbResult.getFilterData().getDisease()))
				.filter(x -> filterByCommunity(x, dbResult.getFilterData().getCommunity()))
				.collect(Collectors.toList());

		logger.info("##### " + method + " DATA SIZE -->> " + data.size());
		final List<JsonObject> jsonArray = new ArrayList<>();
		List<Integer> slots = new ArrayList<>();

		try {
			Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8).forEach(slot -> {
				JsonObject list = filterForSlot(slot, data, dbResult, prefList);
				JsonObject slotObject = new JsonObject();
				if (!list.isEmpty()) {
					slotObject.put("time", ApiUtils.getTimeForSlot(slot));
					slotObject.put("slot", slot);
					slotObject.put("data", list);
					slots.add(slot);
					jsonArray.add(slotObject);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}

		response.put("diets", jsonArray);
		promise.complete(response);

		return promise.future();
	}

	/**
	 * Get diet drink from stream by slot.
	 * 
	 * @param dbResult
	 * @param prefList
	 * @param slot
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getDietDrinFromSreamBySlot(DBResult dbResult, List<JsonObject> prefList, int slot,
			String traceId) {
		String method = "MongoRepositoryWrapper getDietDrinFromSreamBySlot() " + traceId;
		logger.info("##### " + method + " SLOT -->> " + slot);
		JsonObject response = new JsonObject();
		Promise<JsonObject> promise = Promise.promise();
		List<JsonObject> data = dbResult.getData().stream()
				.filter(x -> filterAvoidIn(x, dbResult.getFilterData().getDisease()))
				.filter(x -> filterByCommunity(x, dbResult.getFilterData().getCommunity()))
				.collect(Collectors.toList());

		JsonObject list = filterForSlot(slot, data, dbResult, prefList);
		JsonObject slotObject = new JsonObject();
		if (!list.isEmpty()) {
			slotObject.put("time", ApiUtils.getTimeForSlot(slot));
			slotObject.put("slot", slot);
			slotObject.put("data", list);
		}

		response.put("diets", slotObject);
		promise.complete(response);

		return promise.future();
	}

	/**
	 * get diet plans for drink.
	 * 
	 * @param foodList
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getDietPlansForDrink(List<JsonObject> foodList, String traceId) {
		String method = "MongoRepositoryWrapper getDietPlansForDrink() " + traceId;
		JsonObject query = new JsonObject();
		query.put("foodType", "V");
		query.put("$or",
				new JsonArray().add(new JsonObject().put("Type", "D")).add(new JsonObject().put("Type", "DM")));

		Promise<JsonObject> promise = Promise.promise();
		client.rxFind("DIET_PLAN", query).subscribe(res -> {
			res = res.stream().map(mapper -> {
				mapper.put("code", mapper.getString("_id"));
				mapper.remove("_id");
				return mapper;
			}).collect(Collectors.toList());
			Collections.shuffle(res);
			if (res.size() > 5) {
				res = res.subList(0, 5);
			}
			JsonObject jsonObject = new JsonObject();
			jsonObject.put("foodList", foodList);
			jsonObject.put("drinkList", res);
			promise.complete(jsonObject);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get diet preference.
	 * 
	 * @param query
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getDietPref(JsonObject query, String traceId) {
		String method = "MongoRepositoryWrapper getDietPref() " + traceId;
		Promise<JsonObject> promise = Promise.promise();
		client.rxFind("DIET_PLAN", query).subscribe(res -> {
			res = res.stream().map(mapper -> {
				mapper.remove("_id");
				mapper.put("isSelected", false);
				return mapper;
			}).collect(Collectors.toList());

			JsonObject response = new JsonObject();

			try {
				List<JsonObject> drinks = res.stream().filter(x -> this.notSlot(x, 0)).filter(
						x -> x.getString("Type").equalsIgnoreCase("D") || x.getString("Type").equalsIgnoreCase("DM"))
						.collect(Collectors.toList());

				List<JsonObject> fruits = res.stream().filter(x -> x.getString("Type").equalsIgnoreCase("F"))
						.collect(Collectors.toList());

				List<JsonObject> plscurries = res.stream().filter(x -> x.getString("Type").equalsIgnoreCase("C"))
						.collect(Collectors.toList());

				List<JsonObject> snacks = res.stream().filter(this::getSnacksBySlots).filter(this::getSnacks)
						.collect(Collectors.toList());
				List<JsonObject> dishes = res.stream().filter(x -> x.getString("Type").equalsIgnoreCase("A"))
						.collect(Collectors.toList());

				List<JsonObject> rice = res.stream().filter(x -> x.getString("Type").equalsIgnoreCase("B"))
						.collect(Collectors.toList());

				response.put("drinks", drinks);
				response.put("drinksCount", drinks.size());
				response.put("fruits", fruits);
				response.put("fruitsCount", fruits.size());
				response.put("plscurries", plscurries);
				response.put("plscurriesCount", plscurries.size());
				response.put("snacks", snacks);
				response.put("snacksCount", snacks.size());
				response.put("dishes", dishes);
				response.put("dishesCount", dishes.size());
				response.put("rice", rice);
				response.put("riceCount", rice.size());
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("##### " + method + " EXCEPTION -->> " + e.getMessage());
			}
			promise.complete(response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * get snacks.
	 * 
	 * @param x
	 * @return boolean
	 */
	private boolean getSnacks(JsonObject x) {

		if (x.getString("Type").equalsIgnoreCase("W") || x.getString("Type").equalsIgnoreCase("wp")
				|| x.getString("Type").equalsIgnoreCase("wc") || x.getString("Type").equalsIgnoreCase("wpp")
				|| x.getString("Type").equalsIgnoreCase("wcp") || x.getString("Type").equalsIgnoreCase("we")
				|| x.getString("Type").equalsIgnoreCase("wm")) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Get drinks.
	 * 
	 * @param x
	 * @return boolean
	 */
	private boolean getDrinks(JsonObject x) {

		if (x.getString("Type").equalsIgnoreCase("D") || x.getString("Type").equalsIgnoreCase("DM")) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * get SM.
	 * 
	 * @param x
	 * @return boolean
	 */
	@SuppressWarnings("unused")
	private boolean getSM(JsonObject x) {

		if (x.getString("Type").equalsIgnoreCase("D") || x.getString("Type").equalsIgnoreCase("DM")) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Get all fruits.
	 * 
	 * @param x
	 * @return boolean
	 */
	private boolean getALlFruits(JsonObject x) {

		if (x.getString("Type").equalsIgnoreCase("F") || x.getString("Type").equalsIgnoreCase("FS")) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Get snacks.
	 * 
	 * @param x
	 * @param type
	 * @return boolean
	 */
	@SuppressWarnings("unused")
	private boolean getSnacks(JsonObject x, String type) {

		if (x.getString("Type").equalsIgnoreCase(type)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Get snacks by slots.
	 * 
	 * @param x
	 * @return boolean.
	 */
	private boolean getSnacksBySlots(JsonObject x) {
		JsonArray slots = x.getJsonArray("Slots");
		if (slots == null || slots.isEmpty()) {
			return false;
		}

		if (slots.contains(0))
			return false;

		return true;
	}

	boolean b = false;

	/**
	 * get diet by slot.
	 * 
	 * @param x
	 * @param slot
	 * @return boolean
	 */
	private boolean getDietBySlot(JsonObject x, int slot) {
		JsonArray slots = x.getJsonArray("Slots");
		if (slots == null || slots.isEmpty()) {
			return false;
		}
		return slots.contains(slot);

	}

	/**
	 * remove record from slots.
	 * 
	 * @param x
	 * @param itemCode
	 * @return boolean
	 */
	@SuppressWarnings("unused")
	private boolean removeRecordFromSlots(JsonObject x, String itemCode) {
		String slotItemCode = x.getString("itemCode");
		if (slotItemCode == null || "".equalsIgnoreCase(slotItemCode) || itemCode.equalsIgnoreCase(slotItemCode))
			return false;
		return true;
	}

	/**
	 * Not slot.
	 * 
	 * @param x
	 * @param slot
	 * @return boolean
	 */
	private boolean notSlot(JsonObject x, int slot) {
		JsonArray slots = x.getJsonArray("Slots");
		if (slots == null || slots.isEmpty()) {
			return false;
		}
		return !slots.contains(slot);

	}

	/**
	 * Filter by community.
	 * 
	 * @param x
	 * @param communities
	 * @return boolean
	 */
	private boolean filterByCommunity(JsonObject x, List<String> communities) {
		JsonArray communityArray = x.getJsonArray("Community");
		if (communityArray == null || communityArray.isEmpty()) {
			return false;
		}

		for (String comunity : communities) {
			if (communityArray.contains(comunity)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Filter by diet season.
	 * 
	 * @param x
	 * @return boolean
	 */
	private boolean filterByDietSeason(JsonObject x) {
		JsonArray seasons = x.getJsonArray("Season");
		if (null == seasons || seasons.isEmpty()) {
			return true;
		} else if (null != seasons && seasons.size() <= 1 && seasons.contains("NA")) {
			return true;
		} else {
			Iterator<Object> iter = seasons.iterator();
			Calendar cal = Calendar.getInstance();
			while (iter.hasNext()) {
				String season = (String) iter.next();
				if (new SimpleDateFormat("MMM").format(cal.getTime()).toLowerCase().equalsIgnoreCase(season))
					return true;
			}
		}

		return false;
	}

	@SuppressWarnings("unused")
	private List<JsonObject> getFilteredListByOptions(List<JsonObject> x, DBResult dbresult, String item, int slot) {

		List<String> itemFilteredList = new ArrayList<String>();
		List<JsonObject> selectedUltraSpecialItemsList = new ArrayList<>();
		for (JsonObject json : x)
			if (json.containsKey("Ultra_special")) {
				int ultraSpecialItem = json.getInteger("Ultra_special");
				if (ultraSpecialItem > 0 && ultraSpecialItem == slot
						&& !itemFilteredList.contains(json.getString("itemCode"))) {
					selectedUltraSpecialItemsList.add(json);
					itemFilteredList.add(json.getString("itemCode"));
				}
			}

		List<JsonObject> selectedSpecialSlotItemsList = new ArrayList<>();
		for (JsonObject json : x)
			if (json.containsKey("Special_slot")) {
				int specialslotItem = json.getInteger("Special_slot");
				if (specialslotItem > 0 && specialslotItem == slot
						&& !itemFilteredList.contains(json.getString("itemCode"))) {
					selectedSpecialSlotItemsList.add(json);
					itemFilteredList.add(json.getString("itemCode"));
				}
			}

		List<JsonObject> selectedFoodTypeItemsList = new ArrayList<>();
		for (JsonObject json : x) {
			String foodType = json.getString("foodType");
			if (!itemFilteredList.contains(json.getString("itemCode")))
				if (filterByCustFoodType(json, foodType)) {
					selectedFoodTypeItemsList.add(json);
					itemFilteredList.add(json.getString("itemCode"));
				}
		}

		List<JsonObject> selectedCommunityItemsList = new ArrayList<>();
		for (JsonObject json : x) {
			if (!itemFilteredList.contains(json.getString("itemCode"))
					&& filterByCustCommunity(json, dbresult.getFilterData().getCommunity())) {
				selectedCommunityItemsList.add(json);
			}
		}

		selectedCommunityItemsList = sortedFilteredListForOptions(selectedCommunityItemsList,
				dbresult.getFilterData().getCommunity(), itemFilteredList, item, slot);

		List<JsonObject> selectedRemainingItemsList = new ArrayList<>();
		for (JsonObject json : x)
			if (!itemFilteredList.contains(json.getString("itemCode")))
				selectedRemainingItemsList.add(json);

		List<JsonObject> finalCustDietList = new ArrayList<>();
		selectedUltraSpecialItemsList = FoodFilterUtils.sortByDietScore(selectedUltraSpecialItemsList);
		finalCustDietList.addAll(selectedUltraSpecialItemsList);

		// Suggested items - to be displayed to customer
		selectedSpecialSlotItemsList = FoodFilterUtils.sortByDietScore(selectedSpecialSlotItemsList);
		finalCustDietList.addAll(selectedSpecialSlotItemsList);

		selectedFoodTypeItemsList = FoodFilterUtils.sortByDietScore(selectedFoodTypeItemsList);
		finalCustDietList.addAll(selectedFoodTypeItemsList);

		finalCustDietList.addAll(selectedCommunityItemsList);

		selectedRemainingItemsList = FoodFilterUtils.sortByDietScore(selectedRemainingItemsList);
		finalCustDietList.addAll(selectedRemainingItemsList);

		return finalCustDietList;
	}

	/**
	 * Get filtered list by options.
	 * 
	 * @param x
	 * @param dbresult
	 * @param dietList
	 * @param item
	 * @param slot
	 * @return List<JsonObject>
	 */
	private List<JsonObject> getFilteredListByOptions1(List<JsonObject> x, DBResult dbresult, List<String> dietList,
			String item, int slot) {
		String method = "MongoRepositoryWrapper getFilteredListByOptions1()";
		logger.info("##### " + method + " SLOT [" + slot + "] <-> [" + item + "]  (STARTED) ####################");
		logger.info("##### " + method + " ITEM -->> " + item);
		List<String> itemFilteredList = new ArrayList<String>();
		logger.info("##### " + method + " INITIAL LIST SIZE [" + item + "] -->> " + x.size());
		List<JsonObject> selectedUltraSpecialItemsList = new ArrayList<>();
		for (JsonObject json : x)
			if (json.containsKey("Ultra_special")) {
				int ultraSpecialItem = json.getInteger("Ultra_special");
				if (ultraSpecialItem > 0 && ultraSpecialItem == slot
						&& !itemFilteredList.contains(json.getString("itemCode"))) {
					selectedUltraSpecialItemsList.add(json);
					logger.debug("##### " + method + " ULTRA SPECIAL ITEMS -->> " + json.getString("itemCode") + "-"
							+ json.getString("Food"));
					itemFilteredList.add(json.getString("itemCode"));
				}
			}

		logger.debug("##### " + method + " FINAL selectedUltraSpecialItemsList SIZE -->> "
				+ selectedUltraSpecialItemsList.size());
		List<JsonObject> selectedSpecialSlotItemsList = new ArrayList<>();
		for (JsonObject json : x)
			if (json.containsKey("Special_slot")) {
				int specialslotItem = json.getInteger("Special_slot");
				if (specialslotItem > 0 && specialslotItem == slot
						&& !itemFilteredList.contains(json.getString("itemCode"))) {
					selectedSpecialSlotItemsList.add(json);
					logger.debug("##### " + method + " SPECIAL SLOT ITEMS -->> " + json.getString("itemCode") + "-"
							+ json.getString("Food"));
					itemFilteredList.add(json.getString("itemCode"));
				}
			}

		logger.debug("##### " + method + " FINAL selectedSpecialSlotItemsList SIZE -->> "
				+ selectedSpecialSlotItemsList.size());
		List<JsonObject> selectedFoodTypeItemsList = new ArrayList<>();
		for (JsonObject json : x) {
			String foodType = json.getString("foodType");
			if (!itemFilteredList.contains(json.getString("itemCode")))
				if (filterByCustFoodType(json, foodType) && null != dietList
						&& dietList.contains(json.getString("itemCode"))) {
					selectedFoodTypeItemsList.add(json);
					logger.debug(
							"##### " + method + " SELECTED ITEMS [" + item + "] -->> " + json.getString("itemCode"));
					itemFilteredList.add(json.getString("itemCode"));
				}
		}

		logger.debug(
				"##### " + method + " FINAL selectedFoodTypeItemsList SIZE -->> " + selectedFoodTypeItemsList.size());

		List<JsonObject> selectedCommunityItemsList = new ArrayList<>();

		selectedCommunityItemsList = sortedFilteredListForOptions(x, dbresult.getFilterData().getCommunity(),
				itemFilteredList, item, slot);
		logger.debug("##### " + method + " selectedCommunityItemsList SIZE -->> " + selectedCommunityItemsList.size());
		logger.debug("##### " + method + "     mselectedCommunityItemsList -->> " + selectedCommunityItemsList);

		List<JsonObject> selectedRemainingItemsList = new ArrayList<>();
		for (JsonObject json : x)
			if (!itemFilteredList.contains(json.getString("itemCode")))
				selectedRemainingItemsList.add(json);

		logger.debug("##### " + method + " itemFilteredList SIZE -->> " + itemFilteredList.size());

		List<JsonObject> finalCustDietList = new ArrayList<>();
		selectedUltraSpecialItemsList = FoodFilterUtils.sortByDietScore(selectedUltraSpecialItemsList);
		finalCustDietList.addAll(selectedUltraSpecialItemsList);

		// Suggested items - to be displayed to customer
		selectedSpecialSlotItemsList = FoodFilterUtils.sortByDietScore(selectedSpecialSlotItemsList);
		finalCustDietList.addAll(selectedSpecialSlotItemsList);

		selectedFoodTypeItemsList = FoodFilterUtils.sortByDietScore(selectedFoodTypeItemsList);
		finalCustDietList.addAll(selectedFoodTypeItemsList);
		logger.debug("##### " + method + "  selectedFoodTypeItemsList SIZE -->> " + selectedFoodTypeItemsList.size());
		logger.debug("##### " + method + "       selectedFoodTypeItemsList -->> " + selectedFoodTypeItemsList);

		finalCustDietList.addAll(selectedCommunityItemsList);
		logger.debug("##### " + method + " selectedCommunityItemsList SIZE -->> " + selectedCommunityItemsList.size());
		logger.debug("##### " + method + "      selectedCommunityItemsList -->> " + selectedCommunityItemsList);

		selectedRemainingItemsList = FoodFilterUtils.sortByDietScore(selectedRemainingItemsList);
		finalCustDietList.addAll(selectedRemainingItemsList);
		logger.debug("##### " + method + " selectedRemainingItemsList SIZE -->> " + selectedRemainingItemsList.size());
		logger.debug("##### " + method + "      selectedRemainingItemsList -->> " + selectedRemainingItemsList);

		logger.debug("##### " + method + " finalCustDietList SIZE -->> " + finalCustDietList.size());
		finalCustDietList.addAll(selectedRemainingItemsList);
		logger.info("##### " + method + "          finalCustDietList SIZE -->> " + finalCustDietList.size());
		logger.debug("##### " + method + "               finalCustDietList -->> " + finalCustDietList);
		logger.info("##### " + method + " SLOT [" + slot + "] <-> [" + item + "] (END) #######################");
		logger.info("");

		return finalCustDietList;
	}

	/**
	 * Get filtered diet list by options.
	 * 
	 * @param x
	 * @param dbresult
	 * @param dietList
	 * @param item
	 * @param slot
	 * @return List<JsonObject>
	 */
	@SuppressWarnings("unused")
	private List<JsonObject> getFilteredDietListByOptions(List<JsonObject> x, DBResult dbresult, List<String> dietList,
			String item, int slot) {

		logger.info("#################### SLOT [" + slot + "] <-> [" + item + "]  (STARTED) ####################");
		logger.info("##### " + item);
		List<String> itemFilteredList = new ArrayList<String>();
		logger.info("##### INITIAL LIST SIZE [" + item + "] -->> " + x.size());
		List<JsonObject> selectedUltraSpecialItemsList = new ArrayList<>();
		for (JsonObject json : x)
			if (json.containsKey("Ultra_special")) {
				int ultraSpecialItem = json.getInteger("Ultra_special");
				if (ultraSpecialItem > 0 && ultraSpecialItem == slot
						&& !itemFilteredList.contains(json.getString("itemCode"))) {
					selectedUltraSpecialItemsList.add(json);
					logger.info("##### ULTRA SPECIAL ITEMS -->> " + json.getString("itemCode") + "-"
							+ json.getString("Food"));
					itemFilteredList.add(json.getString("itemCode"));
				}
			}

		logger.info("##### FINAL selectedUltraSpecialItemsList SIZE -->> " + selectedUltraSpecialItemsList.size());

		List<JsonObject> selectedSpecialSlotItemsList = new ArrayList<>();
		for (JsonObject json : x)
			if (json.containsKey("Special_slot")) {
				int specialslotItem = json.getInteger("Special_slot");
				if (specialslotItem > 0 && specialslotItem == slot
						&& !itemFilteredList.contains(json.getString("itemCode"))) {
					selectedSpecialSlotItemsList.add(json);
					logger.info("##### SPECIAL SLOT ITEMS -->> " + json.getString("itemCode") + "-"
							+ json.getString("Food"));
					itemFilteredList.add(json.getString("itemCode"));
				}
			}

		logger.info("##### FINAL selectedSpecialSlotItemsList SIZE -->> " + selectedSpecialSlotItemsList.size());

		List<JsonObject> selectedFoodTypeItemsList = new ArrayList<>();
		for (JsonObject json : x) {
			String foodType = json.getString("foodType");
			if (!itemFilteredList.contains(json.getString("itemCode")))
				if (filterByCustFoodType(json, foodType) && dietList.contains(json.getString("itemCode"))) {
					selectedFoodTypeItemsList.add(json);
					logger.info("##### SELECTED ITEMS [" + item + "] -->> " + json.getString("itemCode"));
					itemFilteredList.add(json.getString("itemCode"));
				}
		}

		logger.info("##### FINAL selectedFoodTypeItemsList SIZE -->> " + selectedFoodTypeItemsList.size());

		List<JsonObject> selectedCommunityItemsList = new ArrayList<>();

		selectedCommunityItemsList = sortedFilteredListForOptions(x, dbresult.getFilterData().getCommunity(),
				itemFilteredList, item, slot);
		logger.info("##### selectedCommunityItemsList SIZE -->> " + selectedCommunityItemsList.size());

		List<JsonObject> selectedRemainingItemsList = new ArrayList<>();
		for (JsonObject json : x)
			if (!itemFilteredList.contains(json.getString("itemCode")))
				selectedRemainingItemsList.add(json);

		logger.info("##### itemFilteredList SIZE -->> " + itemFilteredList.size());

		List<JsonObject> finalCustDietList = new ArrayList<>();
		selectedUltraSpecialItemsList = FoodFilterUtils.sortByDietScore(selectedUltraSpecialItemsList);
		finalCustDietList.addAll(selectedUltraSpecialItemsList);

		// Suggested items - to be displayed to customer
		selectedSpecialSlotItemsList = FoodFilterUtils.sortByDietScore(selectedSpecialSlotItemsList);
		finalCustDietList.addAll(selectedSpecialSlotItemsList);

		selectedFoodTypeItemsList = FoodFilterUtils.sortByDietScore(selectedFoodTypeItemsList);
		finalCustDietList.addAll(selectedFoodTypeItemsList);

		finalCustDietList.addAll(selectedCommunityItemsList);

		selectedRemainingItemsList = FoodFilterUtils.sortByDietScore(selectedRemainingItemsList);
		finalCustDietList.addAll(selectedRemainingItemsList);
		logger.info("##### finalCustDietList SIZE -->> " + finalCustDietList.size());
		logger.info("####################### SLOT [" + slot + "] <-> [" + item + "] (END) #######################");
		logger.info("");

		return finalCustDietList;
	}

	/**
	 * Filter by customer community.
	 * 
	 * @param x
	 * @param communities
	 * @return boolean.
	 */
	private boolean filterByCustCommunity(JsonObject x, List<String> communities) {
		JsonArray communityArray = x.getJsonArray("Community");
		if (communityArray == null || communityArray.isEmpty() || communityArray.contains("U")) {
			return true;
		} else {
			if (communityArray.size() <= 1) {
				Iterator<Object> iter = communityArray.iterator();
				while (iter.hasNext()) {
					String community = (String) iter.next();
					if (communities.contains(community))
						return true;
				}
			}
		}

		return false;
	}

	/**
	 * Sorted filtered list.
	 * 
	 * @param x
	 * @param communities
	 * @param custType
	 * @param traceId
	 * @return List<JsonObject>
	 */
	private List<JsonObject> sortedFilteredList(List<JsonObject> x, List<String> communities, String custType,
			String traceId) {
		String method = "MongoRepositoryWrapper sortedFilteredList() " + traceId + " ";

		List<JsonObject> topMostCommunityCustDietList = new ArrayList<>();
		List<JsonObject> secondMostCommunityCustDietList = new ArrayList<>();
		List<JsonObject> thirdMostCommunityCustDietList = new ArrayList<>();
		List<JsonObject> communityCustDietAsUList = new ArrayList<>();
		List<JsonObject> emptyCommunityCustDietList = new ArrayList<>();
		List<JsonObject> sortedFilteredList = new ArrayList<>();
		for (JsonObject json : x) {
			JsonArray communityArray = json.getJsonArray("Community");

			if (communityArray == null || communityArray.isEmpty()) {
				emptyCommunityCustDietList.add(json);
				continue;
			}

			if (communityArray.size() <= 1) {
				if (communityArray.contains("U")) {
					communityCustDietAsUList.add(json);
					continue;
				} else {
					Iterator<Object> iter = communityArray.iterator();
					while (iter.hasNext()) {
						String community = (String) iter.next();
						if (communities.contains(community)) {
							topMostCommunityCustDietList.add(json);
							break;
						}
					}
				}
			} else {
				Iterator<Object> iter = communityArray.iterator();
				while (iter.hasNext()) {
					String community = (String) iter.next();
					if (communityArray.size() > 1) {
						if (community.equalsIgnoreCase("U")) {
							continue;
						} else if (communities.contains(community)) {
							secondMostCommunityCustDietList.add(json);
							break;
						} else if (!communities.contains(community)) {
							thirdMostCommunityCustDietList.add(json);
							break;
						} else {
							logger.debug("##### " + method + custType + "NEED TO CHECK WHAT IS ITEM -->> "
									+ json.getString("itemCode"));
							break;
						}
					}
				}
			}
		}

		// SORTING TOP MOST ITEMS
		logger.debug("##### " + method + custType + " TOP MOST COMMUNITY LIST -->> "
				+ topMostCommunityCustDietList.size() + " -- " + communities);
		topMostCommunityCustDietList = FoodFilterUtils.sortByDietScore(topMostCommunityCustDietList);
		sortedFilteredList.addAll(topMostCommunityCustDietList);
		// SORTING SECOND MOST ITEMS
		logger.debug("##### " + method + custType + " SECOND MOST COMMUNITY LIST -->> "
				+ secondMostCommunityCustDietList.size() + " -- " + communities);
		secondMostCommunityCustDietList = FoodFilterUtils.sortByDietScore(secondMostCommunityCustDietList);
		sortedFilteredList.addAll(secondMostCommunityCustDietList);
		// SORTING THIRD MOST ITEMS
		logger.debug("##### " + method + custType + " THIRD MOST COMMUNITY LIST  -->> "
				+ thirdMostCommunityCustDietList.size() + " -- " + communities);
		thirdMostCommunityCustDietList = FoodFilterUtils.sortByDietScore(thirdMostCommunityCustDietList);
		sortedFilteredList.addAll(thirdMostCommunityCustDietList);
		// SORTING 'U' ITEMS
		logger.debug("##### " + method + custType + " COMMUNITY AS U LIST        -->> "
				+ communityCustDietAsUList.size() + " -- " + communities);
		communityCustDietAsUList = FoodFilterUtils.sortByDietScore(communityCustDietAsUList);
		sortedFilteredList.addAll(communityCustDietAsUList);
		// SORTING EMPTY ITEMS
		logger.debug("##### " + method + custType + " EMPTY COMMUNITY LIST       -->> "
				+ emptyCommunityCustDietList.size() + " -- " + communities);
		emptyCommunityCustDietList = FoodFilterUtils.sortByDietScore(emptyCommunityCustDietList);
		sortedFilteredList.addAll(emptyCommunityCustDietList);

		return sortedFilteredList;
	}

	/**
	 * Sorted filtered list.
	 * 
	 * @param x
	 * @param communities
	 * @param custType
	 * @param traceId
	 * @return List<JsonObject>
	 */
	private List<JsonObject> filterListByCommunity(List<JsonObject> x, List<String> communities, String traceId) {
		String method = "MongoRepositoryWrapper filterListByCommunity() " + traceId + " ";

		List<JsonObject> topMostCommunityCustDietList = new ArrayList<>();
		List<JsonObject> secondMostCommunityCustDietList = new ArrayList<>();
		List<JsonObject> thirdMostCommunityCustDietList = new ArrayList<>();
		List<JsonObject> communityCustDietAsUList = new ArrayList<>();
		List<JsonObject> emptyCommunityCustDietList = new ArrayList<>();
		List<JsonObject> sortedFilteredList = new ArrayList<>();
		for (JsonObject json : x) {
			JsonArray communityArray = json.getJsonArray("Community");
			if ("011".equalsIgnoreCase(json.getString("_id"))) {
				logger.info("##### " + method + " COMMUNITY -->> " + communityArray);
			}

			if (communityArray == null || communityArray.isEmpty()) {
				emptyCommunityCustDietList.add(json);
				continue;
			}

			if (communityArray.size() <= 1) {
				if (communityArray.contains("U")) {
					communityCustDietAsUList.add(json);
					continue;
				} else {
					Iterator<Object> iter = communityArray.iterator();
					while (iter.hasNext()) {
						String community = (String) iter.next();
						if (communities.contains(community)) {
							topMostCommunityCustDietList.add(json);
							break;
						}
					}
				}
			} else {
				Iterator<Object> iter = communityArray.iterator();
				while (iter.hasNext()) {
					String community = (String) iter.next();
					if (communityArray.size() > 1) {
						if (community.equalsIgnoreCase("U")) {
							continue;
						} else if (communities.contains(community)) {
							secondMostCommunityCustDietList.add(json);
							break;
						} else {
							if (communities.contains("U") && !communities.contains(community))
								thirdMostCommunityCustDietList.add(json);
							logger.debug("##### " + method + "NEED TO CHECK WHAT IS THE ITEM -->> "
									+ json.getString("itemCode"));
							break;
						}
					}
				}
			}
		}

		// SORTING TOPMOST ITEMS
		logger.debug("##### " + method + " TOP MOST COMMUNITY LIST    -->> " + topMostCommunityCustDietList.size()
				+ " -- " + communities);
		topMostCommunityCustDietList = FoodFilterUtils.sortByDietScore(topMostCommunityCustDietList);
		sortedFilteredList.addAll(topMostCommunityCustDietList);
		// SORTING SECONDMOST ITEMS
		logger.debug("##### " + method + " SECOND MOST COMMUNITY LIST -->> " + secondMostCommunityCustDietList.size()
				+ " -- " + communities);
		secondMostCommunityCustDietList = FoodFilterUtils.sortByDietScore(secondMostCommunityCustDietList);
		sortedFilteredList.addAll(secondMostCommunityCustDietList);
		// SORTING 'U' ITEMS
		logger.debug("##### " + method + " AS A 'U' LIST              -->> " + communityCustDietAsUList.size() + " -- "
				+ communities);

		thirdMostCommunityCustDietList = FoodFilterUtils.sortByDietScore(thirdMostCommunityCustDietList);
		sortedFilteredList.addAll(thirdMostCommunityCustDietList);
		// SORTING 'U' AND ONE MORE COMMUNITY ITEM
		logger.debug("##### " + method + " AS 'U' AND ONE MORE ITEM LIST              -->> "
				+ thirdMostCommunityCustDietList.size() + " -- " + communities);

		communityCustDietAsUList = FoodFilterUtils.sortByDietScore(communityCustDietAsUList);
		sortedFilteredList.addAll(communityCustDietAsUList);
		// SORTING EMPTY ITEMS
		logger.debug("##### " + method + " EMPTY LIST                 -->> " + emptyCommunityCustDietList.size()
				+ " -- " + communities);
		emptyCommunityCustDietList = FoodFilterUtils.sortByDietScore(emptyCommunityCustDietList);
		sortedFilteredList.addAll(emptyCommunityCustDietList);

		return sortedFilteredList;
	}

	/**
	 * Sorted filtered list for options.
	 * 
	 * @param x
	 * @param communities
	 * @param itemFilteredList
	 * @param item
	 * @param slot
	 * @return List<JsonObject>
	 */
	private List<JsonObject> sortedFilteredListForOptions(List<JsonObject> x, List<String> communities,
			List<String> itemFilteredList, String item, int slot) {
		String method = "MongoRepositoryWrapper sortedFilteredListForOptions()";
		List<JsonObject> topMostCommunityCustDietList = new ArrayList<>();
		List<JsonObject> secondMostCommunityCustDietList = new ArrayList<>();
		List<JsonObject> thirdMostCommunityCustDietList = new ArrayList<>();
		List<JsonObject> communityCustDietAsUList = new ArrayList<>();
		List<JsonObject> emptyCommunityCustDietList = new ArrayList<>();
		List<JsonObject> sortedFilteredList = new ArrayList<>();
		List<String> items = new ArrayList<>();
		for (JsonObject json : x) {
			if (itemFilteredList.contains(json.getString("itemCode")))
				continue;
			else
				itemFilteredList.add(json.getString("itemCode"));

			items.add(json.getString("itemCode"));

			JsonArray communityArray = json.getJsonArray("Community");

			if (communityArray == null || communityArray.isEmpty()) {
				emptyCommunityCustDietList.add(json);
				continue;
			}

			if (communityArray.size() <= 1) {
				if (communityArray.contains("U")) {
					communityCustDietAsUList.add(json);
					continue;
				} else {
					Iterator<Object> iter = communityArray.iterator();
					while (iter.hasNext()) {
						String community = (String) iter.next();
						if (communities.contains(community)) {
							topMostCommunityCustDietList.add(json);
							break;
						}
					}
				}
			} else {
				Iterator<Object> iter = communityArray.iterator();
				while (iter.hasNext()) {
					String community = (String) iter.next();
					if (communityArray.size() > 1) {
						if (community.equalsIgnoreCase("U")) {
							continue;
						} else if (communities.contains(community)) {
							secondMostCommunityCustDietList.add(json);
							break;
						} else if (!communities.contains(community)) {
							thirdMostCommunityCustDietList.add(json);
							break;
						} else {
							logger.debug("##### " + method + " NEED TO CHECK WHAT IS ITEM -->> "
									+ json.getString("itemCode"));
							break;
						}
					}
				}
			}
		}

		// SORTING TOPMOST ITEMS
		logger.debug("##### " + method + " COMMUNITY ITEM (topMostCommunityCustDietList) -->> "
				+ topMostCommunityCustDietList.size() + " -- " + communities);
		topMostCommunityCustDietList = FoodFilterUtils.sortByDietScore(topMostCommunityCustDietList);
		sortedFilteredList.addAll(topMostCommunityCustDietList);
		// SORTING SECONDMOST ITEMS
		logger.debug("##### " + method + " COMMUNITY ITEM (secondMostCommunityCustDietList) -->> "
				+ secondMostCommunityCustDietList.size() + " -- " + communities);
		secondMostCommunityCustDietList = FoodFilterUtils.sortByDietScore(secondMostCommunityCustDietList);
		sortedFilteredList.addAll(secondMostCommunityCustDietList);
		// SORTING TOP ITEMS
		logger.debug("##### " + method + " COMMUNITY ITEM (thirdMostCommunityCustDietList) -->> "
				+ thirdMostCommunityCustDietList.size() + " -- " + communities);
		thirdMostCommunityCustDietList = FoodFilterUtils.sortByDietScore(thirdMostCommunityCustDietList);
		sortedFilteredList.addAll(thirdMostCommunityCustDietList);
		// SORTING 'U' ITEMS
		logger.debug("##### " + method + " COMMUNITY ITEM (communityCustDietAsUList) -->> "
				+ communityCustDietAsUList.size() + " -- " + communities);
		communityCustDietAsUList = FoodFilterUtils.sortByDietScore(communityCustDietAsUList);
		sortedFilteredList.addAll(communityCustDietAsUList);
		// SORTING EMPTY ITEMS
		logger.debug("##### " + method + " COMMUNITY ITEM (emptyCommunityCustDietList) -->> "
				+ emptyCommunityCustDietList.size() + " -- " + communities);
		emptyCommunityCustDietList = FoodFilterUtils.sortByDietScore(emptyCommunityCustDietList);
		sortedFilteredList.addAll(emptyCommunityCustDietList);
		logger.debug("##### " + method + " COMMUNITY ITEM (sortedFilteredList SIZE) FOR SLOT [" + slot + "] -->> "
				+ sortedFilteredList.size());

		return sortedFilteredList;
	}

	/**
	 * Filter by foodtype ie. Veg/Non-Veg
	 * 
	 * @param x
	 * @param foodType
	 * 
	 * @return booolean
	 */
	private boolean filterByCustFoodType(JsonObject x, String foodType) {

		if (null != foodType && foodType.length() > Constants.ZERO)
			if ("NV".equalsIgnoreCase(foodType)
					&& ("NV".equalsIgnoreCase(x.getString("foodType")) || "E".equalsIgnoreCase(x.getString("foodType"))
							|| "V".equalsIgnoreCase(x.getString("foodType")))) {
				return true;
			} else if ("V".equalsIgnoreCase(foodType) && "V".equalsIgnoreCase(x.getString("foodType"))) {
				return true;
			} else if ("E".equalsIgnoreCase(foodType) && ("E".equalsIgnoreCase(x.getString("foodType"))
					|| "V".equalsIgnoreCase(x.getString("foodType")))) {
				return true;
			}

		return false;
	}

	/**
	 * FIlter lists for avoidIn.
	 * 
	 * @param x
	 * @param diseases
	 * @return boolean
	 */
	private boolean filterAvoidIn(JsonObject x, List<String> diseases) {
		if (diseases == null) {
			return true;
		}
		JsonArray avoidInArray = x.getJsonArray("AvoidIn");
		if (avoidInArray == null || avoidInArray.isEmpty())
			return true;

		for (String diseas : diseases) {
			if (avoidInArray.contains(diseas)) {
				logger.debug("##### filterAvoidIn() DISCARDED ITEMS FROM DISEASES -->> [" + x.getString("itemCode")
						+ "] -- USER DISEASES -- " + diseases);
				return false;
			}
		}
		return true;

	}

	/**
	 * Get community.
	 * 
	 * @param email
	 * @param dieltMaster
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getCommunity(String email, JsonObject dieltMaster, String traceId) {
		String method = "MongoRepositoryWrapper getCommunity() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		try {
			client.rxFind("COMMUNITY_MASTER", new JsonObject()).subscribe(res -> {
				res = res.stream().map(mapper -> {
					mapper.put("code", mapper.getString("_id"));
					JsonObject response = new JsonObject();
					response.put("code", mapper.getString("_id")).put("value", mapper.getString("name"));
					response.put("isSelected", false);
					return response;
				}).collect(Collectors.toList());

				JsonObject response = new JsonObject();
				response.put("dietPref", dieltMaster);
				response.put("community", res);
				promise.complete(response);
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		} catch (Exception ex) {
			logger.error("##### " + method + " EXCEPTIONNNN -->> " + ex.getMessage());
		}

		return promise.future();
	}

	/**
	 * Get filtered community.
	 * 
	 * @param dieltMaster
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getFilteredCommunity(JsonObject dieltMaster, String traceId) {
		String method = "MongoRepositoryWrapper getFilteredCommunity() " + traceId;
		Promise<JsonObject> promise = Promise.promise();
		client.rxFind("COMMUNITY_MASTER", new JsonObject()).subscribe(res -> {
			res = res.stream().map(mapper -> {
				mapper.put("code", mapper.getString("_id"));
				JsonObject response = new JsonObject();
				response.put("code", mapper.getString("_id")).put("value", mapper.getString("name"));
				response.put("isSelected", false);
				return response;
			}).collect(Collectors.toList());

			JsonObject response = new JsonObject();
			dieltMaster.remove("community");
			response.put("dietPref", dieltMaster);
			response.put("community", res);
			promise.complete(response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get filtered community.
	 * 
	 * @param communities
	 * @return String
	 */
	public String getFilteredCommunity(List<String> communities) {

		String community = "";
		for (String comunity : communities)
			if (!"U".equalsIgnoreCase(comunity)) {
				community = comunity;
				break;
			}

		return community;
	}

	/**
	 * Get diseases.
	 * 
	 * @param email
	 * @param jsonObject
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getDiseases(String email, JsonObject jsonObject, String traceId) {
		String method = "MongoRepositoryWrapper getDiseases() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		try {
			client.rxFind("DISEASE_MASTER", new JsonObject()).subscribe(res -> {
				res = res.stream().map(mapper -> {
					mapper.put("code", mapper.getString("_id"));
					JsonObject response = new JsonObject();
					response.put("code", mapper.getString("_id")).put("value", mapper.getString("name"));
					response.put("isSelected", false);
					return response;
				}).collect(Collectors.toList());
				jsonObject.put("diseases", res);
				promise.complete(jsonObject);
			}, (ex) -> {
				logger.error("##### ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		} catch (Exception ex) {
			logger.error("##### " + method + " EXCEPTION -->> " + ex.getMessage());
		}

		return promise.future();
	}

	/**
	 * Get portions.
	 * 
	 * @param email
	 * @param jsonObject
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getPortions(String email, JsonObject jsonObject, String traceId) {
		String method = "MongoRepositoryWrapper getPortions() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		try {
			client.rxFind("PORTION_MASTER", new JsonObject()).subscribe(res -> {
				res = res.stream().map(mapper -> {
					mapper.put("code", mapper.getString("_id"));
					JsonObject response = new JsonObject();
					response.put("code", mapper.getString("_id")).put("value", mapper.getString("name"));
					response.put("isSelected", false);
					return response;
				}).collect(Collectors.toList());

				jsonObject.put("portions", res);
				logger.info("##### " + method + " JSONOBJECT -->> " + jsonObject.toString());
				promise.complete(jsonObject);
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		} catch (Exception ex) {
			logger.error("##### " + method + " EXCEPTION -->> " + ex.getMessage());
		}

		return promise.future();
	}

	/**
	 * Get data for filter diets.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<FilterData> getDataForFilterDiets(String email, String traceId) {
		String method = "MongoRepositoryWrapper getDataForFilterDiets() " + traceId + "-[" + email + "]";
		Set<String> allPrefood = new HashSet<String>();
		Promise<FilterData> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);
		JsonObject fields = new JsonObject().put("lifeStyle.foodType", "lifeStyle.foodType")
				.put("diet.food", "diet.food").put("diet.drinks", "diet.drinks").put("diet.snacks", "diet.snacks")
				.put("diet.fruits", "diet.fruits").put("diet.dishes", "diet.dishes").put("diet.pules", "diet.pules")
				.put("diet.rice", "diet.rice").put("lifeStyle.communities", "lifeStyle.communities")
				.put("lifeStyle.diseases", "lifeStyle.diseases").put("lifeStyle.calories", "lifeStyle.calories");
		logger.info("##### " + method + " FIELDS -->> " + fields);
		client.rxFindOne("CUST_PROFILE", query, fields).subscribe(res -> {
			FilterData data = new FilterData();
			data.setEmail(email);
			try {
				if (res != null) {
					if (res.getJsonObject("lifeStyle").getJsonArray("communities") != null
							&& res.getJsonObject("lifeStyle").getJsonArray("communities").size() > 0) {

						// COMMUNITY
						JsonArray communities = res.getJsonObject("lifeStyle").getJsonArray("communities");
						List<String> ar = new ArrayList<String>();
						communities.forEach(action -> {
							ar.add(action.toString());
						});
						data.setCommunity(ar);
					}

					// FOOD TYPE
					if (res.getJsonObject("lifeStyle").getString("foodType") != null) {
						data.setFoodType(res.getJsonObject("lifeStyle").getString("foodType"));
					}

					// CALORIES
					if (res.getJsonObject("lifeStyle").getDouble("calories") != null) {
						data.setCalories(res.getJsonObject("lifeStyle").getDouble("calories"));
					}

					// DISEASES
					if (res.getJsonObject("lifeStyle").getJsonArray("diseases") != null
							&& res.getJsonObject("lifeStyle").getJsonArray("diseases").size() > 0) {
						JsonArray diseases = res.getJsonObject("lifeStyle").getJsonArray("diseases");
						List<String> ar = new ArrayList<String>();
						diseases.forEach(action -> {
							ar.add(action.toString());
						});
						data.setDisease(ar);
					}

					// FOOD
					if (res.getJsonObject("diet").getJsonArray("food") != null
							&& res.getJsonObject("diet").getJsonArray("food").size() > 0) {
						JsonArray diseases = res.getJsonObject("diet").getJsonArray("food");
						List<String> ar = new ArrayList<String>();
						diseases.forEach(action -> {
							ar.add(action.toString());
						});
						data.setFoods(ar);
					}

					// DRINKS
					if (res.getJsonObject("diet").getJsonArray("drinks") != null
							&& res.getJsonObject("diet").getJsonArray("drinks").size() > 0) {
						JsonArray diseases = res.getJsonObject("diet").getJsonArray("drinks");
						List<String> ar = new ArrayList<String>();
						diseases.forEach(action -> {
							ar.add(action.toString());
							allPrefood.add(action.toString());
						});
						data.setDrinks(ar);
					}

					// SNACKS
					if (res.getJsonObject("diet").getJsonArray("snacks") != null
							&& res.getJsonObject("diet").getJsonArray("snacks").size() > 0) {
						JsonArray diseases = res.getJsonObject("diet").getJsonArray("snacks");
						List<String> ar = new ArrayList<String>();
						diseases.forEach(action -> {
							ar.add(action.toString());
							allPrefood.add(action.toString());
						});

						data.setSnacks(ar);
					}

					// FRUITS
					if (res.getJsonObject("diet").getJsonArray("fruits") != null
							&& res.getJsonObject("diet").getJsonArray("fruits").size() > 0) {
						JsonArray diseases = res.getJsonObject("diet").getJsonArray("fruits");
						List<String> ar = new ArrayList<String>();
						diseases.forEach(action -> {
							ar.add(action.toString());
							allPrefood.add(action.toString());
						});
						data.setFruits(ar);
					}

					// DISHES
					if (res.getJsonObject("diet").getJsonArray("dishes") != null
							&& res.getJsonObject("diet").getJsonArray("dishes").size() > 0) {
						JsonArray diseases = res.getJsonObject("diet").getJsonArray("dishes");
						List<String> ar = new ArrayList<String>();
						diseases.forEach(action -> {
							ar.add(action.toString());
							allPrefood.add(action.toString());
						});
						data.setDishes(ar);
					}

					// PULSES
					if (res.getJsonObject("diet").getJsonArray("pules") != null
							&& res.getJsonObject("diet").getJsonArray("pules").size() > 0) {
						JsonArray diseases = res.getJsonObject("diet").getJsonArray("pules");
						List<String> ar = new ArrayList<String>();
						diseases.forEach(action -> {
							ar.add(action.toString());
							allPrefood.add(action.toString());
						});
						data.setPules(ar);
					}

					// RICE
					if (res.getJsonObject("diet").getJsonArray("rice") != null
							&& res.getJsonObject("diet").getJsonArray("rice").size() > 0) {
						JsonArray diseases = res.getJsonObject("diet").getJsonArray("rice");
						List<String> ar = new ArrayList<String>();
						diseases.forEach(action -> {
							ar.add(action.toString());
							allPrefood.add(action.toString());
						});
						data.setRice(ar);
					}
				}
			} catch (Exception e) {
				logger.error("##### " + method + " EXCEPTION -->> " + e.getMessage());
			}

			data.setAllPrefood(allPrefood);
			promise.complete(data);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Create customer diet plan.
	 * 
	 * @param email
	 * @param request
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> createCustDietPlan(String email, CreatCustomerDietPlanRequest request, String traceId) {
		String method = "MongoRepositoryWrapper createCustDietPlan() " + traceId + "-[" + email + "]";
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
		String currentDate = dateFormat.format(new Date());
		JsonArray meals = new JsonArray();
		request.getMeals().forEach(action -> {
			JsonObject jsonObject = new JsonObject();
			jsonObject.put("type", action.getType());
			jsonObject.put("code", action.getCode());
			jsonObject.put("description", action.getDescription());
			meals.add(jsonObject);
		});

		JsonArray drinks = new JsonArray();
		request.getDrinks().forEach(action -> {
			JsonObject jsonObject = new JsonObject();
			jsonObject.put("type", action.getType());
			jsonObject.put("code", action.getCode());
			jsonObject.put("description", action.getDescription());
			drinks.add(jsonObject);
		});

		Promise<JsonObject> promise = Promise.promise();
		JsonObject payload = new JsonObject();
		payload.put("email", email);
		payload.put("drinks", drinks);
		payload.put("meals", meals);

		payload.put("createdDate", new JsonObject().put("$date", currentDate));
		logger.info("##### " + method + " PAYLOAD -->> " + payload);
		client.rxSave("CUST_DIET_PLAN", payload).subscribe(res -> {
			promise.complete(new JsonObject());
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Get customer diet plan.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getCustomerDietPlan(String email, String traceId) {
		String method = "MongoRepositoryWrapper getCustomerDietPlan() " + traceId + "-[" + email + "]";
		JsonObject query = new JsonObject().put("email", email);
		logger.info("##### " + method + " QUERY -->> " + query.toString());
		Promise<JsonObject> promise = Promise.promise();
		client.rxFind("CUST_DIET_PLAN", query).subscribe(res -> {
			res = res.stream().map(mapper -> {
				JsonObject response = new JsonObject();
				response.put("id", mapper.getString("_id"));
				response.put("email", mapper.getString("email"));
				response.put("meals", mapper.getJsonArray("meals", null));
				response.put("drinks", mapper.getJsonArray("drinks", null));
				response.put("createDate", mapper.getJsonObject("createdDate").getString("$date"));
				return response;
			}).collect(Collectors.toList());
			JsonObject jsonObject = new JsonObject();
			jsonObject.put("dietList", res);
			promise.complete(jsonObject);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Get default habits.
	 * 
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getDefaultHabits(String traceId) {
		String method = "MongoRepositoryWrapper getDefaultHabits() " + traceId;
		JsonObject query = new JsonObject();
		Promise<JsonObject> promise = Promise.promise();
		client.rxFind("HABIT_MASTER", query).subscribe(res -> {
			try {
				res = res.stream().map(mapper -> {
					JsonObject response = new JsonObject();
					response.put("code", mapper.getString("_id"));
					response.put("iconUrl", mapper.getString("iconUrl"));
					response.put("videoUrl", mapper.getString("videoUrl"));
					response.put("selected", mapper.getBoolean("selected", false));
					response.put("description", mapper.getString("description"));
					response.put("title", mapper.getString("title", ""));
					response.put("order", mapper.getInteger("order"));
					response.put("canDelete", mapper.getBoolean("canDelete", false));
					return response;
				}).collect(Collectors.toList());
			} catch (Exception e) {
				e.printStackTrace();
			}
			JsonObject jsonObject = new JsonObject();
			jsonObject.put("habitList", res);
			promise.complete(jsonObject);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Get habit master.
	 * 
	 * @param habitData
	 * @param traceId
	 * @return Future<HabitData>
	 */
	public Future<HabitData> getHabitMaster(HabitData habitData, String traceId) {
		String method = "MongoRepositoryWrapper getHabitMaster() " + traceId;
		JsonObject query = new JsonObject();
		Promise<HabitData> promise = Promise.promise();
		client.rxFind("HABIT_MASTER", query).subscribe(res -> {
			res = res.stream().map(mapper -> {
				JsonObject response = new JsonObject();
				response.put("code", mapper.getString("_id"));
				response.put("iconUrl", mapper.getString("iconUrl"));
				response.put("videoUrl", mapper.getString("videoUrl"));
				response.put("selected", mapper.getBoolean("selected", false));
				response.put("description", mapper.getString("description"));
				response.put("title", mapper.getString("title", ""));
				response.put("order", mapper.getInteger("order", 1));
				response.put("canDelete", mapper.getBoolean("canDelete", true));
				return response;
			}).collect(Collectors.toList());
			habitData.setMasterList(res);
			promise.complete(habitData);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Get customer followed habit.
	 * 
	 * @param email
	 * @param habitData
	 * @param traceId
	 * @return Future<HabitData>
	 */
	public Future<HabitData> getCustFollowedHabit(String email, HabitData habitData, String traceId) {
		String method = "MongoRepositoryWrapper getCustFollowedHabit() " + traceId + "-[" + email + "]";
		JsonObject query = new JsonObject();
		query.put("email", email);
		Promise<HabitData> promise = Promise.promise();
		client.rxFind("CUST_HABIT_FOLLOW", query).subscribe(res -> {
			if (res == null || res.isEmpty()) {
			}
			List<JsonObject> customerFollowedList = new ArrayList<JsonObject>();
			res.forEach(action -> {

				Calendar cal1 = new GregorianCalendar();
				Calendar cal2 = new GregorianCalendar();
				SimpleDateFormat parseFormat = new SimpleDateFormat("ddMMyyyy");
				Date startDate;
				try {
					startDate = parseFormat.parse(action.getString("date"));
					cal1.setTime(startDate);
					Date endDate = new Date();
					cal2.setTime(endDate);
					int noOfDaysPassed = daysPassed(cal1.getTime(), cal2.getTime());
					if (noOfDaysPassed <= 21)
						customerFollowedList.add(action);
				} catch (ParseException e) {
					e.printStackTrace();
					logger.error("##### " + method + " EXCEPTION -->> " + e.getMessage());
				}
			});

			habitData.setCustomerFollowedList(customerFollowedList);

			promise.complete(habitData);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Get customer habit.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<HabitData>
	 */
	public Future<HabitData> getCustomerHabit(String email, String traceId) {
		String method = "MongoRepositoryWrapper getCustomerHabit() " + traceId + "-[" + email + "]";
		HabitData habitData = new HabitData();
		JsonObject query = new JsonObject().put("email", email);
		Promise<HabitData> promise = Promise.promise();
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFind("CUST_HABIT_DETAIL_V2", query).subscribe(res -> {

			if (res == null || res.isEmpty()) {
				promise.fail("No habit added");
			} else {
				habitData.setCustomerHabitList(res);
				promise.complete(habitData);
			}

		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Get customer followed habits.
	 * 
	 * @param email
	 * @param habitData
	 * @param traceId
	 * @return Future<HabitData>
	 */
	public Future<HabitData> getCustomerFollowedHabit(String email, HabitData habitData, String traceId) {
		String method = "MongoRepositoryWrapper getCustomerFollowedHabit() " + traceId + "-[" + email + "]";
		JsonObject query = new JsonObject().put("email", email);
		Promise<HabitData> promise = Promise.promise();
		client.rxFind("CUST_HABIT_FOLLOW", query).subscribe(res -> {
			if (res == null || res.isEmpty()) {
				res = new ArrayList<JsonObject>();
			}
			habitData.setCustomerHabitList(res);
			promise.complete(habitData);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Create habit master.
	 * 
	 * @param payload
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> createHabitMaster(JsonObject payload, String traceId) {
		String method = "MongoRepositoryWrapper createHabitMaster() " + traceId;
		payload.put("_id", payload.getString("code"));
		Promise<JsonObject> promise = Promise.promise();
		client.rxSave("HABIT_MASTER", payload).subscribe(res -> {
			JsonObject jsonObject = new JsonObject();
			jsonObject.put("code", "0000");
			jsonObject.put("message", "success");
			promise.complete(jsonObject);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Create customer habit.
	 * 
	 * @param email
	 * @param request
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> createCustHabit(String email, CreatCustomerHabitPlanRequest request, String traceId) {
		String method = "MongoRepositoryWrapper createCustHabit() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		Calendar calll = Calendar.getInstance(); // creates calendar
		calll.setTime(new Date()); // sets calendar time/date
		calll.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		calll.add(Calendar.MINUTE, 30); // add 30 minutes
		String currentDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(calll.getTime());
		request.getHabits().forEach(action -> {
			JsonObject jsonObject = new JsonObject();
			jsonObject.put("_id", new JsonObject().put("email", email).put("code", action.getCode()));
			jsonObject.put("code", action.getCode());
			jsonObject.put("date", ApiUtils.getCurrentDate(email, traceId));
			jsonObject.put("email", email);
			jsonObject.put("iconUrl", action.getIconUrl());
			jsonObject.put("createdDate", new JsonObject().put("$date", currentDate));
			jsonObject.put("description", action.getDescription());

			client.rxSave("CUST_HABIT_DETAIL_V2", jsonObject).subscribe(res -> {
				logger.debug("##### " + method + " CUST_HABIT_DETAIL_V2 SAVED SUCCESSFULLY.");
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			});
		});
		promise.complete(new JsonObject().put("code", "0000").put("message", "Sucess"));
		return promise.future();
	}

	/**
	 * Update customer habit status.
	 * 
	 * @param email
	 * @param request
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> updateCustHabitStaus(String email, CreatCustomerHabitPlanRequest request,
			String traceId) {
		String method = "MongoRepositoryWrapper updateCustHabitStaus() " + traceId + "-[" + email + "]";

		Promise<JsonObject> promise = Promise.promise();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.DATE, 1);
		calendar.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		calendar.add(Calendar.MINUTE, 30); // add 30 minutes
		String currentDate = dateFormat.format(calendar.getTime());

		request.getHabits().forEach(action -> {
			JsonObject payload = new JsonObject();
			payload.put("_id", new JsonObject().put("email", email).put("code", action.getCode()).put("date",
					ApiUtils.getCurrentDate(email, traceId)));
			payload.put("createdDate", currentDate);
			payload.put("code", action.getCode());
			payload.put("status", action.getStatus() == null ? "PENDING" : action.getStatus());
			payload.put("date", ApiUtils.getCurrentDate(email, traceId));
			payload.put("email", email);

			client.rxSave("CUST_HABIT_FOLLOW", payload).subscribe(res -> {
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			});
		});

		promise.complete(new JsonObject().put("code", "0000").put("message", "Sucess"));
		return promise.future();
	}

	/**
	 * Remove customer habit status.
	 * 
	 * @param email
	 * @param code
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> deleteCustHabitStaus(String email, String code, String traceId) {
		String method = "MongoRepositoryWrapper deleteCustHabitStaus() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();

		JsonObject query = new JsonObject();
		query.put("email", email).put("code", code);
		client.rxRemoveDocument("CUST_HABIT_DETAIL_V2", query).subscribe(res -> {
			logger.info("##### " + method + " CUST_HABIT_DETAIL_V2 SAVED SUCCESSFULLY.");
			promise.complete(new JsonObject().put("code", "0000").put("message", "Sucess"));
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.complete(new JsonObject().put("code", "0001").put("message", ex.getMessage()));

		});

		return promise.future();
	}

	/**
	 * Get diet plan from cache.
	 * 
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getDietPlanCachedata(String traceId) {
		String method = "MongoRepositoryWrapper getDietPlanCachedata() " + traceId;
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject();
		client.rxFind("CUST_DAILY_DIET", query).map(map -> {
			return map;
		}).subscribe(res1 -> {
			JsonObject dietData = new JsonObject();
			List<JsonObject> dietCacheList = new ArrayList<JsonObject>();
			res1.forEach(action -> {
				dietCacheList.add(action);
			});

			dietData.put("diet", dietCacheList);
			promise.complete(dietData);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Remove diet choices from cache.
	 * 
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> deleteDietChoicesCache(String traceId) {
		String method = "MongoRepositoryWrapper deleteDietChoicesCache() " + traceId;
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject();
		client.rxFind("CUST_DAILY_DIET", query).map(map -> {
			return map;
		}).subscribe(res1 -> {
			res1.forEach(action -> {
				JsonObject json = action;
				query.put("_id", new JsonObject().put("email", json.getJsonObject("_id").getString("email")).put("date",
						ApiUtils.getPreviousDate()));
				logger.info("##### " + method + " QUERY -->> " + query);
				client.rxRemoveDocument("CUST_DAILY_DIET", query).subscribe(res -> {
					logger.debug("##### " + method + " CUST_DAILY_DIET REMOVED SUCCESSFULLY.");
				}, (ex) -> {
					logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
					promise.complete(new JsonObject().put("code", "0001").put("message", ex.getMessage()));
				});
			});

			promise.complete(new JsonObject().put("code", "0000").put("message", "Sucess"));
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Remove customer habit followed.
	 * 
	 * @param email
	 * @param code
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> deleteCustHabitFollowed(String email, String code, String traceId) {
		String method = "MongoRepositoryWrapper deleteCustHabitFollowed() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();

		JsonObject query = new JsonObject();
		query.put("email", email).put("code", code);
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxRemoveDocument("CUST_HABIT_FOLLOW", query).subscribe(res -> {
			logger.debug("##### " + method + " CUST_HABIT_FOLLOW REMOVED SUCCESSFULLY.");
			promise.complete(new JsonObject().put("code", "0000").put("message", "Sucess"));
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.complete(new JsonObject().put("code", "0001").put("message", ex.getMessage()));

		});

		return promise.future();
	}

	/**
	 * Get customer habit followed.
	 * 
	 * @param resp
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getCustomerHabitForUpdate(JsonObject resp, String email, String traceId) {
		String method = "MongoRepositoryWrapper getCustomerHabitForUpdate() " + traceId + "-[" + email + "]";
		JsonObject query = new JsonObject().put("email", email);
		logger.info("##### " + method + " QUERY -->> " + query);
		FindOptions findOptions = new FindOptions();
		findOptions.setSort(new JsonObject().put("createdDate", -1));
		Promise<JsonObject> promise = Promise.promise();
		client.rxFind("CUST_HABIT_FOLLOW", query).subscribe(res -> {
			if (res == null || res.isEmpty()) {
				res = new ArrayList<JsonObject>();
			} else {
				res = res.stream().map(mapper -> {
					JsonObject response = new JsonObject();
					try {
						if (null != mapper) {
							JsonObject id = mapper.getJsonObject("_id");
							response.put("email", id.getString("email"));
							response.put("date", mapper.getString("date"));
							response.put("code", id.getString("code"));
							response.put("canDelete", mapper.getBoolean("canDelete", true));
							if (null != mapper.getString("createdDate")) {
								response.put("createdDate", mapper.getString("createdDate"));
							}
						}

					} catch (Exception ex) {
						logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
					}
					return response;
				}).collect(Collectors.toList());
			}

			resp.put("habits", res);
			promise.complete(resp);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get customer habit for today via email.
	 * 
	 * @param email
	 * @param json
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getCustomerHabitForToday(String email, JsonObject json, String traceId) {
		String method = "MongoRepositoryWrapper getCustomerHabitForToday() " + traceId + "-[" + email + "]";
		JsonObject query = new JsonObject();
		query.put("email", email);
		query.put("date", ApiUtils.getTodaysDate());
		logger.info("##### " + method + " QUERY -->> " + query);
		Promise<JsonObject> promise = Promise.promise();
		boolean isAnyHabitSubscribed = json.getBoolean("isAnyHabitSubscribed");
		JsonObject finalPayload = new JsonObject();
		client.rxFind("CUST_HABIT_FOLLOW", query).subscribe(res -> {
			finalPayload.put("isHabitApplicableForToday", true);
			if (res == null || res.isEmpty()) {
				logger.info("##### " + method + " NO HABIT(S) FOLLOWED FOR TODAY.");
				if (isAnyHabitSubscribed) {
					logger.info("##### " + method + " IS HABIT APPLICABLE -->> "
							+ json.getBoolean("isHabitApplicableForToday"));
					String profileCreationDateString = json.getString("profileCreatedDate");
					try {
						Date profileCreationDate = new SimpleDateFormat("dd-MMM-yyyy").parse(profileCreationDateString);
						Calendar calendar = Calendar.getInstance();
						calendar.setTime(new Date());
						calendar.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
						calendar.add(Calendar.MINUTE, 30); // add 30 minutes
						String currentDateInString = new SimpleDateFormat("dd-MMM-yyyy").format(calendar.getTime());
						Date currentDate = new SimpleDateFormat("dd-MMM-yyyy").parse(currentDateInString);
						if (profileCreationDate.before(currentDate))
							finalPayload.put("isHabitApplicableForToday", false);
					} catch (ParseException e) {
						e.printStackTrace();
					}
				}
			} else {
				finalPayload.put("isHabitApplicableForToday", true);
			}

			promise.complete(finalPayload);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get customer payment details for today via email.
	 * 
	 * @param email
	 * @param json
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getCustomerPaymentDetailForToday(String email, JsonObject json, String traceId) {
		String method = "MongoRepositoryWrapper getCustomerPaymentDetailForToday() " + traceId + "-[" + email + "]";
		JsonObject query = new JsonObject();
		query.put("emailId", email);
		query.put("isActive", true);
		logger.info("##### " + method + " QUERY -->> " + query);
		Promise<JsonObject> promise = Promise.promise();
		client.rxFind("PLAN_SUBCRIPTION_DETAIL", query).map(map -> {
			return map;
		}).subscribe(res -> {
			json.put("code", "0000");
			json.put("message", "success");
			if (!res.isEmpty()) {
				res.forEach(action -> {
					try {
						Date expiryDateTime = new SimpleDateFormat("dd-MMM-yyyy").parse(action.getString("expiryDate"));
						String currentDateTimeInString = new SimpleDateFormat("dd-MMM-yyyy")
								.format(Calendar.getInstance().getTime());
						Date currentDateTime = new SimpleDateFormat("dd-MMM-yyyy").parse(currentDateTimeInString);
						json.put("isPlanExpired", false);
						if (expiryDateTime.before(currentDateTime)) {
							json.put("isPlanSubscribed", true);
							json.put("isPlanExpired", true);
						}
					} catch (ParseException e) {
						e.printStackTrace();
					}
				});
			} else {
				json.put("isPlanSubscribed", false);
			}

			promise.complete(json);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get habit master details by code.
	 * 
	 * @param code
	 * @param habit
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getHabiMasterDetailByCode(String code, JsonObject habit, String traceId) {
		String method = "MongoRepositoryWrapper getHabiMasterDetailByCode() " + traceId;
		JsonObject query = new JsonObject().put("_id", code);
		logger.info("##### " + method + " QUERY -->> " + query);
		FindOptions findOptions = new FindOptions();
		findOptions.setSort(new JsonObject().put("createdDate", -1));
		Promise<JsonObject> promise = Promise.promise();
		client.rxFindOne("HABIT_MASTER", query, null).subscribe(res -> {
			JsonObject response = new JsonObject();
			JsonObject id = res.getJsonObject("_id");
			response.put("email", id.getString("email"));
			response.put("date", habit.getString("date"));
			response.put("code", id.getString("code"));
			response.put("canDelete", res.getBoolean("canDelete", true));
			response.put("createdDate", res.getJsonObject("createdDate").getString("$date"));
			promise.complete(response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Get customer subscribed habits.
	 * 
	 * @param data
	 * @param traceId
	 * @return Future<List<JsonObject>>
	 */
	public Future<List<JsonObject>> getCustomerHabit(HabitData data, String traceId) {
		String method = "MongoRepositoryWrapper getCustomerHabit() " + traceId;
		List<JsonObject> responseList = new ArrayList<JsonObject>();
		Promise<List<JsonObject>> promise = Promise.promise();
		try {
			List<JsonObject> masterList = data.getMasterList(); // HABITS MASTER LIST (HABIT_MASTER)
			logger.debug("##### " + method + " HABIT MASTER LIST            -->> " + masterList.size());
			List<JsonObject> customerHabits = data.getCustomerHabitList(); // CUSTOMER HABITS COMPLETE LIST
																			// (CUST_HABIT_DETAIL_V2)
			logger.debug("##### " + method + " CUSTOMER HABITS LIST         -->> " + customerHabits.size());
			List<JsonObject> customerFollowedList = data.getCustomerFollowedList(); // ONLY DONE HABITS FOLLOW
																					// (CUST_HABIT_FOLLOW)
			logger.debug("##### " + method + " CUSTOMER HABIT FOLLOWED LIST -->> " + customerFollowedList.size());

			List<String> habitCodesList = new ArrayList<String>();
			for (JsonObject custObj : customerHabits) {
				String code = custObj.getString("code");
				if (!habitCodesList.contains(code))
					habitCodesList.add(code);
				else
					continue;

				int totalFollowedHabits = customerFollowedList.stream()
						.filter(f -> f.getString("code").equalsIgnoreCase(code)).collect(Collectors.toList()).size();

				int totalDoneFollowedHabits = customerFollowedList.stream()
						.filter(f -> f.getString("code").equalsIgnoreCase(code))
						.filter(f -> f.getString("status").equalsIgnoreCase("Done")).collect(Collectors.toList())
						.size();

				double achivedPercentageDouble = 0.0d;
				String achivedPercentage = "";
				if (totalFollowedHabits > 0) {
					achivedPercentageDouble = ((Math.round(Double.valueOf(totalDoneFollowedHabits)) * 100)
							/ totalFollowedHabits);
					DecimalFormat f = new DecimalFormat("##");
					achivedPercentage = f.format(achivedPercentageDouble);
				} else {
					achivedPercentageDouble = 0;
					achivedPercentage = "0";
				}

				String color = "";
				String message = "You achieved " + achivedPercentage + "% in last "
						+ (totalFollowedHabits > 1 ? totalFollowedHabits + " days. " : totalFollowedHabits + " day. ");

				if (achivedPercentageDouble > 90) {
					color = "#14CEB7"; // green
					message += "Awesome.";
				} else if (achivedPercentageDouble >= 75 && achivedPercentageDouble <= 90) {
					color = "Orange";
					message += "Good.";
				} else {
					color = "Red";
					message += "Please improve.";
				}

				JsonObject jsonObject = getMealByCode(code, masterList);

				logger.debug("##### " + method + " COMPLETE % [" + code + "] -->> " + achivedPercentage);
				if (null != jsonObject) {
					jsonObject.put("completePer", achivedPercentage);
					jsonObject.put("canDelete", jsonObject.getBoolean("canDelete", true));
					jsonObject.put("order", jsonObject.getInteger("order", 1));
					jsonObject.remove("selected");
					jsonObject.put("color", color);
					jsonObject.put("message", message);
					responseList.add(jsonObject);
				}
			}

			logger.info("##### " + method + " TOTAL CUSTOMER HABIT CODES -->> "
					+ (null != habitCodesList || habitCodesList.size() > 0 ? habitCodesList.size() : 0));
		} catch (Exception e) {
			e.printStackTrace();
		}
		promise.complete(responseList);
		return promise.future();
	}

	public int daysPassed(Date d1, Date d2) {
		return (int) ((d2.getTime() - d1.getTime()) / (1000 * 60 * 60 * 24));
	}

	@SuppressWarnings("unused")
	private Double getHabitDoneCount(JsonArray habits) {
		double count = 0;
		for (int i = 0; i < habits.size(); i++) {

			JsonObject jsonObject = habits.getJsonObject(i);
			if ("DONE".equalsIgnoreCase(jsonObject.getString("status"))) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Check customer habit.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> checkCustomerHabit(String email, String traceId) {
		String method = "MongoRepositoryWrapper checkCustomerHabit() " + traceId + "-[" + email + "]";
		JsonObject query = new JsonObject().put("email", email);
		logger.info("##### " + method + " QUERY -->> " + query);
		Promise<JsonObject> promise = Promise.promise();
		client.rxFindOne("CUST_HABIT_DETAIL", query, null).subscribe(res -> {
			if (res == null || res.isEmpty()) {
				promise.fail("no habit created ");
			} else {
				JsonObject response = new JsonObject();
				response.put("email", res.getString("email"));
				response.put("habits", res.getJsonArray("habits", null));
				response.put("createDate", res.getJsonObject("createdDate").getString("$date"));
				promise.complete(response);
			}
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Filter for slot.
	 * 
	 * @param slot
	 * @param datas
	 * @param dbResult
	 * @param prefList
	 * @return JsonObject
	 */
	private JsonObject filterForSlot(int slot, List<JsonObject> datas, DBResult dbResult, List<JsonObject> prefList) {
		List<String> disease = dbResult.getFilterData().getDisease();
		if (prefList == null) {
			prefList = new ArrayList<JsonObject>();
		}
		prefList = prefList.stream().filter(x -> getDietBySlot(x, slot)).map(this::removeUnusedKey)
				.collect(Collectors.toList());
		JsonObject response = new JsonObject();
		List<JsonObject> allDatas = dbResult.getData();
		datas = datas.stream().map(this::removeUnusedKey).filter(x -> getDietBySlot(x, slot))
				.collect(Collectors.toList());
		List<JsonObject> drinks = datas.stream()
				.filter(x -> x.getString("Type").equalsIgnoreCase("D") || x.getString("Type").equalsIgnoreCase("DM"))
				.collect(Collectors.toList());
		List<JsonObject> fruits = datas.stream().filter(x -> x.getString("Type").equalsIgnoreCase("F"))
				.collect(Collectors.toList());
		List<JsonObject> snacks = datas.stream().filter(this::getSnacks).collect(Collectors.toList());
		List<JsonObject> dishes = datas.stream().filter(x -> x.getString("Type").equalsIgnoreCase("A"))
				.collect(Collectors.toList());
		List<JsonObject> rice = datas.stream().filter(x -> x.getString("Type").equalsIgnoreCase("B"))
				.collect(Collectors.toList());
		List<JsonObject> secondaryWithMilk = datas.stream().filter(x -> x.getString("Type").equalsIgnoreCase("SM"))
				.collect(Collectors.toList());
		List<JsonObject> plscurries = datas.stream().filter(x -> x.getString("Type").equalsIgnoreCase("C"))
				.collect(Collectors.toList());

		response.put("Remark", "Food detail");
		response.put("Locked", false);

		List<JsonObject> filteredDietList = new ArrayList<JsonObject>();

		JsonObject foodItems = new JsonObject();

		if (!prefList.isEmpty()) {

			List<JsonObject> drinkPref = getDietByTypeForSlection(prefList, Arrays.asList("D", "DM"));

			if (!drinkPref.isEmpty()) {
				foodItems.put("drink", FoodFilterUtils.geFilteredData(drinkPref));
			}

			List<JsonObject> dishesPref = getDietByTypeForSlection(prefList, Arrays.asList("A"));
			if (!dishesPref.isEmpty()) {
				foodItems.put("dishes", FoodFilterUtils.geFilteredData(dishesPref));
			}
			List<JsonObject> fruitsPref = getDietByTypeForSlection(prefList, Arrays.asList("F"));
			if (!fruitsPref.isEmpty()) {
				foodItems.put("fruits", FoodFilterUtils.geFilteredData(fruitsPref));
			}

			List<JsonObject> snacksPref = prefList.stream().filter(this::getSnacks).collect(Collectors.toList());
			if (!snacksPref.isEmpty()) {
				foodItems.put("snacks", FoodFilterUtils.geFilteredData(snacksPref));
			}

			List<JsonObject> plscurriesPref = getDietByTypeForSlection(prefList, Arrays.asList("C"));
			if (!plscurriesPref.isEmpty()) {
				foodItems.put("plscurries", FoodFilterUtils.geFilteredData(plscurriesPref));
			}

			List<JsonObject> ricePref = getDietByTypeForSlection(prefList, Arrays.asList("B"));
			if (!ricePref.isEmpty()) {
				foodItems.put("rice", FoodFilterUtils.geFilteredData(ricePref));
			}

			List<JsonObject> secondaryPref = getDietByTypeForSlection(prefList, Arrays.asList("S"));
			if (!secondaryPref.isEmpty()) {
				foodItems.put("secondary", FoodFilterUtils.geFilteredData(secondaryPref));
			}

			List<JsonObject> secondaryWithMilkPref = getDietByTypeForSlection(prefList, Arrays.asList("SM"));
			if (!secondaryWithMilkPref.isEmpty()) {
				foodItems.put("secondaryWithMilk", FoodFilterUtils.geFilteredData(secondaryWithMilkPref));
			}
		}

		switch (slot) {

		case 0:

			JsonObject slot_0_items = new JsonObject();

			if (foodItems.getJsonObject("drinks") == null) {
				if (disease == null || disease.isEmpty()) {
					List<JsonObject> drink = datas.stream().filter(x -> x.getString("Type").equalsIgnoreCase("D")
							|| x.getString("Type").equalsIgnoreCase("DM")).collect(Collectors.toList());
					if (FoodFilterUtils.geFilteredData(drink) != null) {
						slot_0_items.put("drinks", FoodFilterUtils.geFilteredData(drink));
					}
				} else {
					List<JsonObject> drink = FoodFilterUtils.filterByDiseaseRecommendedIn(datas, disease).stream()
							.filter(x -> x.getString("Type").equalsIgnoreCase("D")
									|| x.getString("Type").equalsIgnoreCase("DM"))
							.collect(Collectors.toList());
					;
					if (FoodFilterUtils.geFilteredData(drink) != null) {
						slot_0_items.put("drinks", FoodFilterUtils.geFilteredData(drink));
					}
				}

			} else {
				slot_0_items.put("drinks", foodItems.getJsonObject("drinks"));
			}

			if (foodItems.getJsonObject("dishes") == null) {

				if (disease != null && !disease.isEmpty()) {
					List<JsonObject> dishes_0 = FoodFilterUtils.filterByDiseaseRecommendedIn(datas, disease).stream()
							.filter(x -> x.getString("Type").equalsIgnoreCase("A")).collect(Collectors.toList());
					;
					if (dishes_0 != null) {
						slot_0_items.put("dishes", FoodFilterUtils.geFilteredData(dishes_0));
					}
				}

			} else {
				slot_0_items.put("dishes", foodItems.getJsonObject("dishes"));
			}

			response.put("meals", slot_0_items);
			response.put("Remark",
					"2 glasses of warm water at this time will help to clean your stomach and  improve digestion.");
			filteredDietList.add(slot_0_items.getJsonObject("drinks"));
			filteredDietList.add(slot_0_items.getJsonObject("dishes"));

			break;

		case 1:
			String[] recF = { "097", "098" };
			JsonObject slot_1_items = new JsonObject();
			JsonObject fObj = new JsonObject();
			int index = ThreadLocalRandom.current().nextInt(recF.length);
			if (disease == null || disease.isEmpty()) {
				fObj = getMealByCode(recF[index], allDatas);
			} else {
				fObj = FoodFilterUtils.geFilteredData(FoodFilterUtils.filterByDiseaseRecommendedIn(datas, disease));
			}

			slot_1_items.put("fruits", fObj);
			response.put("meals", slot_1_items);
			response.put("Remark", "Fruits, especially apple or banana is the best choice at this time.");
			filteredDietList.add(slot_1_items.getJsonObject("fruits"));

			break;
		case 2:

			JsonObject slot_2_items = new JsonObject();

			if (foodItems.getJsonObject("drinks") == null) {
				slot_2_items.put("drinks", FoodFilterUtils.geFilteredData(drinks));
			} else {
				slot_2_items.put("drinks", foodItems.getJsonObject("drinks"));
			}

			if (foodItems.getJsonObject("snacks") == null) {

				JsonObject snacksObj = FoodFilterUtils.geFilteredData(snacks);
				if (snacksObj != null) {
					String type = snacksObj.getString("Type");
					if ("WE".equalsIgnoreCase(type)) {
						slot_2_items.put("rice", getMealByCode("008", allDatas));
						filteredDietList.add(slot_2_items.getJsonObject("rice"));
					} else if ("WC".equalsIgnoreCase(type) || "WCP".equalsIgnoreCase(type)) {
						slot_2_items.put("rice", getMealByCode("055", allDatas));
						filteredDietList.add(slot_2_items.getJsonObject("rice"));

					}
				}
				slot_2_items.put("snacks", snacksObj);

			} else {
				slot_2_items.put("snacks", foodItems.getJsonObject("snacks"));
			}

			response.put("meals", slot_2_items);
			response.put("Remark",
					"Change it in each day of week. Avoid milk or coffee or tea. Black coffe or tea or green tea is good.");
			filteredDietList.add(slot_2_items.getJsonObject("drinks"));
			filteredDietList.add(slot_2_items.getJsonObject("snacks"));
			break;

		case 3:

			JsonObject slot_3_items = new JsonObject();
			snacks = datas.stream().filter(x -> this.getSnacks(x)).collect(Collectors.toList());

			if (foodItems.getJsonObject("snacks") == null) {
				slot_3_items.put("snacks", FoodFilterUtils.geFilteredData(snacks));
			} else {
				slot_3_items.put("snacks", foodItems.getJsonObject("snacks"));
			}

			response.put("meals", slot_3_items);
			response.put("Remark",
					"Fruit ( if not taken early morning) or butterlmilk or coconut water or nuts are best choices");

			filteredDietList.add(slot_3_items.getJsonObject("snacks"));
			break;

		case 4:

			JsonObject slot_4_items = new JsonObject();

			slot_4_items.put("Salads", getMealByCode("034", allDatas));

			if (foodItems.getJsonObject("plscurries") == null) {
				slot_4_items.put("plscurries", FoodFilterUtils.geFilteredData(plscurries));

			} else {
				slot_4_items.put("plscurries", foodItems.getJsonObject("plscurries"));
			}

			if (foodItems.getJsonObject("rice") == null) {
				slot_4_items.put("rice", FoodFilterUtils.geFilteredData(rice));
			} else {
				slot_4_items.put("rice", foodItems.getJsonObject("rice"));
			}

			if (foodItems.getJsonObject("secondaryWithMilk") == null) {
				slot_4_items.put("secondaryWithMilk", FoodFilterUtils.geFilteredData(secondaryWithMilk));
			} else {
				slot_4_items.put("secondaryWithMilk", foodItems.getJsonObject("secondaryWithMilk"));
			}
			response.put("meals", slot_4_items);
			response.put("Remark",
					"Eating salad 15 minutes before lunch is must. Use whole wheat aatta. Use mustard, til or coconut oil for cooking");

			filteredDietList.add(slot_4_items.getJsonObject("Salads"));
			filteredDietList.add(slot_4_items.getJsonObject("plscurries"));
			filteredDietList.add(slot_4_items.getJsonObject("rice"));
			filteredDietList.add(slot_4_items.getJsonObject("secondaryWithMilk"));
			break;

		case 5:

			JsonObject slot_5_items = new JsonObject();

			if (foodItems.getJsonObject("drinks") == null) {
				slot_5_items.put("drinks", FoodFilterUtils.geFilteredData(drinks));
			} else {
				slot_5_items.put("drinks", foodItems.getJsonObject("drinks"));
			}
			response.put("meals", slot_5_items);
			response.put("Remark", "Drinking green tea at this time will boost your metabolism.");
			filteredDietList.add(slot_5_items.getJsonObject("drinks"));

			break;
		case 6:

			String[] wOptions = { "032", "035", "036", "065", "028" };
			index = ThreadLocalRandom.current().nextInt(wOptions.length);

			JsonObject slot_6_items = new JsonObject();
			if (foodItems.getJsonObject("snacks") == null) {
				slot_6_items.put("snacks", getMealByCode(wOptions[index], allDatas));
			} else {
				slot_6_items.put("snacks", foodItems.getJsonObject("snacks"));
			}

			response.put("meals", slot_6_items);
			response.put("Remark",
					"Makhane or dhokla or soup are good options. This meal is must to reduce dinner intake");
			filteredDietList.add(slot_6_items.getJsonObject("snacks"));
			break;

		case 7:
			JsonObject slot_7_items = new JsonObject();
			slot_7_items.put("Salads", getMealByCode("034", allDatas));

			if (foodItems.getJsonObject("dishes") == null) {
				slot_7_items.put("dishes", FoodFilterUtils.geFilteredData(dishes));

			} else {
				slot_7_items.put("dishes", foodItems.getJsonObject("dishes"));
			}
			if (foodItems.getJsonObject("rice") == null) {
				rice = rice.stream().filter(x -> x.getInteger("portion") == 1).collect(Collectors.toList());
				slot_7_items.put("rice", FoodFilterUtils.geFilteredData(rice));

			} else {
				slot_7_items.put("rice", foodItems.getJsonObject("rice"));
			}
			response.put("meals", slot_7_items);
			response.put("Remark",
					"should be 50% of lunch. Avoid roti or rice. Fruits, chila, omlette, sprouts, are good options.");
			filteredDietList.add(slot_7_items.getJsonObject("dishes"));
			filteredDietList.add(slot_7_items.getJsonObject("rice"));
			filteredDietList.add(slot_7_items.getJsonObject("Salads"));

			break;

		case 8:

			JsonObject slot_8_items = new JsonObject();
			if (foodItems.getJsonObject("drinks") == null) {

				slot_8_items.put("drinks", FoodFilterUtils.geFilteredData(drinks));

			} else {
				slot_8_items.put("drinks", foodItems.getJsonObject("drinks"));
			}

			response.put("meals", slot_8_items);
			response.put("Remark", "Haldi milk is good for sound sleep n immunity.");
			response.put("Locked", false);
			filteredDietList.add(slot_8_items.getJsonObject("drinks"));
			break;

		default:
			response.put("drinks", drinks.get(0));
			response.put("fruits", fruits.get(0));
			response.put("snacks", snacks.get(0));
			response.put("dishes", dishes.get(0));
			break;

		}

		filteredDietList = filteredDietList.stream().filter(predicate -> predicate != null)
				.collect(Collectors.toList());
		Double totalCalories = filteredDietList.stream().mapToDouble(m -> m.getDouble("Calories")).sum();
		Double totalCarbs = filteredDietList.stream().mapToDouble(m -> m.getDouble("Carbs")).sum();
		Double totalFat = filteredDietList.stream().mapToDouble(m -> m.getDouble("Fat")).sum();
		Double totalProtien = filteredDietList.stream().mapToDouble(m -> m.getDouble("Protien")).sum();
		Double totalFiber = filteredDietList.stream().mapToDouble(m -> m.getDouble("Fiber")).sum();

		response.put("totalCalories", ApiUtils.getDecimal(totalCalories));
		response.put("totalCarbs", ApiUtils.getDecimal(totalCarbs));
		response.put("totalFat", ApiUtils.getDecimal(totalFat));
		response.put("totalProtien", ApiUtils.getDecimal(totalProtien));
		response.put("totalFiber", ApiUtils.getDecimal(totalFiber));
		response.put("slotItems", filteredDietList);
		return response;
	}

	/**
	 * Filter for slot2.
	 * 
	 * @param slot
	 * @param datas
	 * @param dbResult
	 * @param prefList
	 * @return List<JsonObject>
	 */
	@SuppressWarnings("unused")
	private List<JsonObject> filterForSlot2(int slot, List<JsonObject> datas, DBResult dbResult,
			List<JsonObject> prefList) {
		String method = "MongoRepositoryWrapper filterForSlot2()";
		List<String> desease = dbResult.getFilterData().getDisease();
		if (prefList == null) {
			prefList = new ArrayList<JsonObject>();
		}
		prefList = prefList.stream().filter(x -> getDietBySlot(x, slot)).collect(Collectors.toList());

		JsonObject response = new JsonObject();
		List<JsonObject> allDatas = dbResult.getData();
		datas = datas.stream().filter(x -> getDietBySlot(x, slot)).collect(Collectors.toList());
		List<JsonObject> drinks = datas.stream()
				.filter(x -> x.getString("Type").equalsIgnoreCase("D") || x.getString("Type").equalsIgnoreCase("DM"))
				.collect(Collectors.toList());
		Collections.shuffle(drinks);
		List<JsonObject> fruits = datas.stream().filter(x -> x.getString("Type").equalsIgnoreCase("F"))
				.collect(Collectors.toList());
		Collections.shuffle(fruits);
		List<JsonObject> snacks = datas.stream().filter(this::getSnacks).collect(Collectors.toList());
		Collections.shuffle(snacks);

		List<JsonObject> rice = datas.stream().filter(x -> x.getString("Type").equalsIgnoreCase("B"))
				.collect(Collectors.toList());
		Collections.shuffle(rice);
		List<JsonObject> secondary = datas.stream().filter(x -> x.getString("Type").equalsIgnoreCase("S"))
				.collect(Collectors.toList());
		Collections.shuffle(secondary);
		List<JsonObject> secondaryWithMilk = datas.stream().filter(x -> x.getString("Type").equalsIgnoreCase("SM"))
				.collect(Collectors.toList());
		Collections.shuffle(secondaryWithMilk);

		List<JsonObject> plscurries = datas.stream().filter(x -> x.getString("Type").equalsIgnoreCase("C"))
				.collect(Collectors.toList());

		response.put("Remark", "Food detail");
		response.put("Locked", false);

		List<JsonObject> finalDietList = new ArrayList<JsonObject>();

		switch (slot) {
		case 0:

			if (desease == null || desease.isEmpty()) {
				List<JsonObject> drink = datas.stream().filter(x -> x.getString("Type").equalsIgnoreCase("D"))
						.collect(Collectors.toList());
				JsonObject filteredDrink = FoodFilterUtils.geFilteredData(drink);
				if (filteredDrink != null) {
					finalDietList.add(filteredDrink);
				}
			} else {
				List<JsonObject> drink = FoodFilterUtils.filterByDiseaseRecommendedIn(datas, desease).stream()
						.filter(x -> x.getString("Type").equalsIgnoreCase("D")).collect(Collectors.toList());
				;
				JsonObject filteredDrink = FoodFilterUtils.geFilteredData(drink);
				if (filteredDrink != null) {
					finalDietList.add(filteredDrink);
				}
				List<JsonObject> dishes = FoodFilterUtils.filterByDiseaseRecommendedIn(datas, desease).stream()
						.filter(x -> x.getString("Type").equalsIgnoreCase("A")).collect(Collectors.toList());
				;
				JsonObject filteredDish = FoodFilterUtils.geFilteredData(dishes);
				if (filteredDish != null) {
					finalDietList.add(filteredDish);
				}
			}
			break;

		case 1:

			break;
		case 2:

			break;

		case 3:

			break;

		case 4:

		}
		return finalDietList;
	}

	/**
	 * Get coupon list.
	 * 
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getCouponList(String traceId) {
		String method = "MongoRepositoryWrapper getCouponList() " + traceId;
		Promise<JsonObject> promise = Promise.promise();
		client.rxFind("COUPON_MASTER", new JsonObject()).subscribe(res -> {
			JsonObject response = new JsonObject();

			List<JsonObject> list = new ArrayList<>();
			Map<Integer, JsonObject> map = new HashMap<>();
			res = res.stream().map(mapper -> {
				JsonObject mapObject = new JsonObject();
				mapObject.put("amount", mapper.getDouble("amount"));
				mapObject.put("discountedAmount", mapper.getDouble("discountedAmount"));
				Integer period = mapper.getInteger("period");
				mapObject.put("period", period);
				mapObject.put("perid", mapper.getInteger("durationInMonth"));
				mapObject.put("couponCode", mapper.getString("_id"));
				if (null != mapper.getString("food"))
					mapObject.put("food", mapper.getString("food"));
				else
					mapObject.put("food", JsonObject.mapFrom(null));

				if (null != mapper.getString("message0"))
					mapObject.put("message0", mapper.getString("message0"));
				else
					mapObject.put("message0", JsonObject.mapFrom(null));

				if (null != mapper.getString("message1"))
					mapObject.put("message1", mapper.getString("message1"));
				else
					mapObject.put("message1", JsonObject.mapFrom(null));

				if (null != mapper.getString("message2"))
					mapObject.put("message2", mapper.getString("message2"));
				else
					mapObject.put("message2", JsonObject.mapFrom(null));

				if (null != mapper.getString("message3"))
					mapObject.put("message3", mapper.getString("message3"));
				else
					mapObject.put("message3", JsonObject.mapFrom(null));

				if (null != mapper.getString("message4"))
					mapObject.put("message4", mapper.getString("message4"));
				else
					mapObject.put("message4", JsonObject.mapFrom(null));

				if (null != mapper.getString("discount"))
					mapObject.put("discount", mapper.getString("discount"));
				else
					mapObject.put("discount", JsonObject.mapFrom(null));

				map.put(mapper.getInteger("period"), mapObject);
				return mapObject;
			}).collect(Collectors.toList());

			// SORT COUPONS
			SortedMap<Integer, JsonObject> sortedMap = new TreeMap<>(map);
			sortedMap.keySet().forEach(key -> {
				System.out.println(key + " -> " + sortedMap.get(key));
			});

			for (Integer period : sortedMap.keySet())
				list.add(map.get(period));

			response.put("couponList", list);
			response.put("code", "0000");
			response.put("message", "Success");
			promise.complete(response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get coupon for subscribed plan customers.
	 * 
	 * @param request
	 * @param traceId
	 * @return Future<SubscribeRequest>
	 */
	public Future<SubscribeRequest> getCouponForPaymentDoneUser(SubscribeRequest request, String traceId) {
		String method = "MongoRepositoryWrapper getCouponForPaymentDoneUser() " + traceId;
		logger.info("##### " + method + " COUPON CODE -->> " + request.getCouponCode());
		Promise<SubscribeRequest> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("_id", request.getCouponCode());
		client.rxFindOne("COUPON_MASTER", query, null).subscribe(res -> {

			if (res == null || res.isEmpty()) {
				promise.fail("Invalid coupon code");
			} else {
				request.setValidityInDays(res.getInteger("period"));
				request.setCouponCode(request.getCouponCode());
				promise.complete(request);
			}
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Remove unused key.
	 * 
	 * @param object
	 * @return JsonObject
	 */
	private JsonObject removeUnusedKey(JsonObject object) {
		if (object != null) {
			object.remove("Season");
			object.remove("_id");
			object.remove("itemCode");
			object.remove("Remark");
		}

		logger.info("##### removeUnusedKey() OBJECT AFTER -->> " + object);
		return object;
	}

	/**
	 * Update customer weight.
	 * 
	 * @param email
	 * @param weight
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> updateCustomerWeight(String email, Double weight, String traceId) {
		String method = "MongoRepositoryWrapper updateCustomerWeight() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		Calendar calll = Calendar.getInstance(); // creates calendar
		calll.setTime(new Date()); // sets calendar time/date
		calll.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		calll.add(Calendar.MINUTE, 30); // add 30 minutes
		String currentDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(calll.getTime());
		JsonObject jsonObject = new JsonObject();
		jsonObject.put("_id",
				new JsonObject().put("email", email).put("date", ApiUtils.getCurrentDate(email, traceId)));
		jsonObject.put("weight", weight);
		jsonObject.put("date", ApiUtils.getCurrentDate(email, traceId));
		jsonObject.put("email", email);
		jsonObject.put("createdDate", new JsonObject().put("$date", currentDate));
		logger.info("##### " + method + " PAYLOAD -->> " + jsonObject);
		client.rxSave("CUSTOMER_DAILY_WEIGHT", jsonObject).subscribe(res -> {
			promise.complete(new JsonObject().put("code", "0000").put("message", "Sucess"));
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get customer latest weight.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getCustomerLatestWeight(String email, String traceId) {
		String method = "MongoRepositoryWrapper getCustomerLatestWeight() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject jsonObject = new JsonObject();
		JsonObject response = new JsonObject();
		jsonObject.put("email", email);
		logger.info("##### " + method + " QUERY -->> " + jsonObject);
		client.rxFind("CUSTOMER_DAILY_WEIGHT", jsonObject).map(mapper -> {
			mapper.forEach(action -> {
				action.put("createdTime", action.getJsonObject("createdDate").getString("$date"));
				action.remove("_id");
				action.remove("email");
				action.remove("createdDate");
			});
			return mapper;
		}).subscribe(res -> {
			if (res != null && !res.isEmpty()) {
				JsonObject data = new JsonObject();
				data.put("weight", res.get(res.size() - 1).getDouble("weight"));
				data.put("date", res.get(res.size() - 1).getString("date"));
				response.put("data", data);
			}
			response.put("code", "0000");
			response.put("messsage", "success");
			promise.complete(response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			response.put("code", "0001");
			response.put("messsage", ex.getMessage());
			promise.complete(response);
		});
		return promise.future();
	}

	/**
	 * Get customer weight graph data.
	 * 
	 * @param profile
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getCustomerWeightGraphData(Profile profile, String traceId) {
		String method = "MongoRepositoryWrapper getCustomerWeightGraphData() " + traceId + "-[" + profile.getEmail()
				+ "]";
		JsonObject response = new JsonObject();
		DateFormat dateFormatISO = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
		SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yyyy");
		Calendar calendar = Calendar.getInstance();
		logger.info("##### " + method + " PROFILE OBJECT -->> " + profile);
		try {
			String subDate = format.format(dateFormatISO.parse(profile.getSubscribeDate()));
			logger.info("##### " + method + " PLAN SUBSCRIPTION DATE   -->> " + profile.getSubscribeDate());
			Integer numberDays = ApiUtils.getNumberOfDaysFromWeight(new Double(profile.getWeight()),
					new Double(profile.getSuggestedWeight()));
			logger.info("##### " + method + " CALCULATED NO OF DAYS    -->> " + numberDays);
			calendar.setTime(dateFormatISO.parse(profile.getSubscribeDate()));
			calendar.add(Calendar.DATE, Math.abs(numberDays));
			response.put("suggstedWeight", profile.getSuggestedWeight());
			logger.info("##### " + method + " SUGGESTED DATE           -->> " + format.format(calendar.getTime()));
			response.put("targetDate", format.format(calendar.getTime()));
			logger.info("##### " + method + " SUGGESTED/TARGETED DATE  -->> " + format.format(calendar.getTime()));
			response.put("startWeight", profile.getWeight());
			if (null != profile.getCurrentWeight())
				response.put("currentWeight", profile.getCurrentWeight());
			else
				response.put("currentWeight", profile.getWeight());
			response.put("startDate", subDate);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		Promise<JsonObject> promise = Promise.promise();
		JsonObject jsonObject = new JsonObject();
		jsonObject.put("email", profile.getEmail());
		client.rxFind("CUSTOMER_DAILY_WEIGHT", jsonObject).map(mapper -> {

			mapper.forEach(action -> {
				action.put("createdTime", action.getJsonObject("createdDate").getString("$date"));
				action.remove("_id");
				action.remove("email");
				action.remove("createdDate");
			});
			return mapper;
		}).subscribe(res -> {
			if (res != null && !res.isEmpty()) {
				List<JsonObject> resList = new ArrayList<>();
				for (JsonObject json : res) {
					resList.add(json);
//					Date createdDate;
//					Date startDate;
//					try {
//						createdDate = dateFormatISO.parse(json.getString("createdTime"));
//						startDate = format.parse(response.getString("startDate"));
//						if (createdDate.after(startDate))
//							resList.add(json);
//						else
//							logger.info("##### " + method + " CREATED DATE [" + createdDate
//									+ "] IS LESS THAN PLAN SUBSCRIPTION DATE [" + startDate + "].");
//					} catch (ParseException e) {
//						logger.info("##### " + method + " ESXCEPTION   -->> " + e.getMessage());
//						e.printStackTrace();
//					}
				}

				response.put("currentWeight", res.get(res.size() - 1).getDouble("weight"));
				response.put("dailyWeightArray", resList);
			}

			promise.complete(response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Get customer targeted weight.
	 * 
	 * @param json
	 * @param profile
	 * @param weight
	 * @param suggestedWeight
	 * @return CustProfile
	 */
	public CustProfile getCustomerTargetedWeight(JsonObject json, CustProfile profile, Integer weight,
			Double suggestedWeight) {
		String method = "MongoRepositoryWrapper getCustomerTargetedWeight()";
		DateFormat dateFormatISO = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
		SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yyyy");
		Calendar calendar = Calendar.getInstance();
		List<Customer> profiles = profile.getProfile();
		for (Customer customer : profiles) {
			try {
				if (json.getJsonObject("profile").getString("email").equalsIgnoreCase(customer.getEmail())) {
					Integer numberDays = ApiUtils.getNumberOfDaysFromWeight(new Double(weight),
							new Double(suggestedWeight));
					calendar.setTime(dateFormatISO.parse(customer.getSubscribeDate()));
					calendar.add(Calendar.DATE, Math.abs(numberDays));

					profile.setTargetedDate(format.format(calendar.getTime()));
					break;
				}
			} catch (Exception e) {
				logger.error("##### " + method + " ERROR -->> " + e.getMessage());
				e.printStackTrace();
			}
		}

		return profile;
	}

	/**
	 * Get profile detail.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<Profile>
	 */
	public Future<Profile> getProfileDetail(String email, String traceId) {
		String method = "MongoRepositoryWrapper getProfileDetail() " + traceId + "-[" + email + "]";
		Promise<Profile> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);
		JsonObject fileds = new JsonObject().put("email", "email").put("demographic.gender", "gender")
				.put("demographic.suggestedWeight", "suggestedWeight").put("demographic.height", "height")
				.put("demographic.currentWeight", "currentWeight").put("diet.dateBy", "dateBy")
				.put("diet.suggestedWeight", "suggestedWeight").put("demographic.weight", "weight");

		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFindOne("CUST_PROFILE", query, fileds).subscribe(res -> {
			if (res == null || res.isEmpty()) {
				promise.fail("invalid customer");
			} else {
				logger.info("##### " + method + " RES -->> " + res);
				JsonObject response = new JsonObject();
				response.put("email", res.getString("_id"));
				response.put("gender", res.getJsonObject("demographic").getJsonObject("gender").getString("code"));
				response.put("height", res.getJsonObject("demographic").getJsonObject("height").getInteger("value"));
				response.put("dateBy", res.getJsonObject("diet").getString("dateBy"));
//				response.put("suggestedWeight", res.getJsonObject("diet").getDouble("suggestedWeight"));
				logger.info("##### " + method + " STARTED...");
				response.put("suggestedWeight", res.getJsonObject("demographic").getDouble("suggestedWeight"));
				logger.info("##### " + method + "ENDED");
				response.put("weight", res.getJsonObject("demographic").getJsonObject("weight").getInteger("value"));
				if (null != res.getJsonObject("demographic").getDouble("currentWeight"))
					response.put("currentWeight", res.getJsonObject("demographic").getDouble("currentWeight"));
				else
					response.put("currentWeight",
							res.getJsonObject("demographic").getJsonObject("weight").getInteger("value"));
				Profile profile = Json.decodeValue(response.toString(), Profile.class);
				logger.info("##### " + method + " PROFILE -->> " + profile);
				promise.complete(profile);
			}

		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get complete profile details.
	 * 
	 * @param traceId
	 * @return Future<CustProfile>
	 */
	public Future<CustProfile> getCompleteProfileDetails(String traceId) {
		String method = "MongoRepositoryWrapper getCompleteProfileDetails() " + traceId;
		List<Customer> profiles = new ArrayList<>();
		CustProfile customerProfile = new CustProfile();
		Promise<CustProfile> promise = Promise.promise();
		JsonObject query = new JsonObject();

		JsonObject fields = new JsonObject().put("email", "email").put("demographic.gender", "gender")
				.put("demographic.height", "height").put("diet.dateBy", "dateBy")
				.put("diet.suggestedWeight", "suggestedWeight").put("demographic.weight", "weight");

		client.rxFindOne("CUST_PROFILE", query, fields).subscribe(res -> {
			if (res == null || res.isEmpty()) {
				promise.fail("invalid customer");
			} else {
				JsonObject response = new JsonObject();
				response.put("email", res.getString("_id"));
				response.put("gender", res.getJsonObject("demographic").getJsonObject("gender").getString("code"));
				response.put("height", res.getJsonObject("demographic").getJsonObject("height").getInteger("value"));
				response.put("dateBy", res.getJsonObject("diet").getString("dateBy"));
				response.put("suggestedWeight", res.getJsonObject("diet").getDouble("suggestedWeight"));
				response.put("weight", res.getJsonObject("demographic").getJsonObject("weight").getInteger("value"));
				Customer profile = Json.decodeValue(response.toString(), Customer.class);
				profiles.add(profile);
				customerProfile.setProfile(profiles);
				promise.complete(customerProfile);
			}
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Update customer diet preferences.
	 * 
	 * @param dietCodeList
	 * @param request
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> updateDietPreferrence(JsonArray dietCodeList, CustDietPreference request,
			String traceId) {
		String method = "MongoRepositoryWrapper updateDietPreferrence() " + traceId;
		Calendar calll = Calendar.getInstance(); // creates calendar
		calll.setTime(new Date()); // sets calendar time/date
		calll.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		calll.add(Calendar.MINUTE, 30); // add 30 minutes
		String currentDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(calll.getTime());
		Promise<JsonObject> promise = Promise.promise();
		JsonObject payload = new JsonObject();
		payload.put("_id", request.getEmail());
		payload.put("createdDate", currentDate);

		dietCodeList.forEach(action -> {
			if (request.getFoodCodeList().contains(action.toString()))
				request.getFoodCodeList().remove(action.toString());
		});
		dietCodeList.addAll(request.getFoodCodeList());
		payload.put("foodList", dietCodeList);
		logger.info("##### " + method + " PAYLOAD -->> " + payload);
		client.rxSave("CUST_DIET_PREF", payload).subscribe(res -> {
			JsonObject response = new JsonObject();
			response.put("code", "0000");
			response.put("message", "success");
			promise.complete(response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Update customer diet preferences V2.
	 * 
	 * @param dietCodeList
	 * @param request
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> updateOrSaveDietPreferrenceV2(Set<FoodDetail> dietCodeList, CustDietPreference request,
			String traceId) {
		String method = "MongoRepositoryWrapper updateOrSaveDietPreferrenceV2() " + traceId + "-[" + request.getEmail()
				+ "]";
		Calendar calll = Calendar.getInstance(); // creates calendar
		calll.setTime(new Date()); // sets calendar time/date
		calll.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		calll.add(Calendar.MINUTE, 30); // add 30 minutes
		String currentDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(calll.getTime());
		Promise<JsonObject> promise = Promise.promise();
		JsonObject payload = new JsonObject();
		payload.put("createdDate", currentDate);

		JsonArray jsonArray = new JsonArray();
		if (null != dietCodeList && dietCodeList.size() > 0) {
			logger.info("##### " + method + "   EXISTING RECORD TO BE UPDATED.");
			List<FoodDetail> foodDetailsList = new ArrayList<>();
			foodDetailsList.addAll(dietCodeList);
			try {
				for (FoodDetail foodDetail : foodDetailsList)
					jsonArray.add(new JsonObject().put("code", foodDetail.getCode())
							.put("portion", foodDetail.getPortion()).put("counter", foodDetail.getCounter()));
			} catch (Exception e) {
				e.printStackTrace();
			}
			payload.put("foodList", jsonArray);
			payload.put("createdDate", currentDate);

			JsonObject query = new JsonObject().put("_id", request.getEmail());
			JsonObject update = new JsonObject().put("$set",
					new JsonObject().put("createdDate", currentDate).put("foodList", jsonArray));
			logger.info("##### " + method + "   QUERY -->> " + query);
			logger.info("##### " + method + " PAYLOAD -->> " + update);
			client.rxUpdateCollection("CUST_DIET_PREF_V2", query, update).subscribe(res -> {
				JsonObject response = new JsonObject();
				response.put("code", "0000");
				response.put("message", "success");
				promise.complete(response);
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		} else {
			try {
				List<FoodDetail> foodDetailsList = new ArrayList<>();
				foodDetailsList.addAll(request.getFoods());
				try {
					for (FoodDetail foodDetail : foodDetailsList)
						jsonArray.add(new JsonObject().put("code", foodDetail.getCode())
								.put("portion", foodDetail.getPortion()).put("counter", 1));
				} catch (Exception e) {
					e.printStackTrace();
				}

				payload.put("_id", request.getEmail());
				payload.put("foodList", jsonArray);
				logger.info("##### " + method + " PAYLOAD -->> " + payload);
				client.rxSave("CUST_DIET_PREF_V2", payload).subscribe(res -> {
					JsonObject response = new JsonObject();
					response.put("code", "0000");
					response.put("message", "success");
					promise.complete(response);
				}, (ex) -> {
					logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
					promise.fail(ex.getMessage());
				});
			} catch (Exception ex) {
				logger.error("##### " + method + " EXCEPTION -->> " + ex.getMessage());
			}
		}

		return promise.future();
	}

	/**
	 * Get customer diet preference.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonArray>
	 */
	public Future<JsonArray> getCustDietPref(String email, String traceId) {
		String method = "MongoRepositoryWrapper getCustDietPref() " + traceId + "-[" + email + "]";
		Promise<JsonArray> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);
		client.rxFindOne("CUST_DIET_PREF", query, null).subscribe(res -> {
			if (res == null || res.isEmpty())
				res = new JsonObject();

			promise.complete(res.getJsonArray("foodList", new JsonArray()));
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Get customer diet preference V2.
	 * 
	 * @param preference
	 * @param email
	 * @param traceId
	 * @return Future<Set<FoodDetail>>
	 */
	public Future<Set<FoodDetail>> getCustDietPrefV2(CustDietPreference preference, String email, String traceId) {
		String method = "MongoRepositoryWrapper getCustDietPrefV2() " + traceId + "-[" + email + "]";
		Promise<Set<FoodDetail>> promise = Promise.promise();
		Set<FoodDetail> foodDetails = new HashSet<FoodDetail>();
		Set<FoodDetail> prefDoodDetails = preference.getFoods();
		List<FoodDetail> prefFoodDetailsList = new ArrayList<FoodDetail>();
		prefFoodDetailsList.addAll(prefDoodDetails);

		Set<FoodDetail> finalFoodDetails = new HashSet<FoodDetail>();
		JsonObject query = new JsonObject().put("_id", email);
		client.rxFindOne("CUST_DIET_PREF_V2", query, null).subscribe(res -> {
			List<String> items = new ArrayList<>();
			if (res != null) {
				JsonArray foodList = res.getJsonArray("foodList");
				if (foodList != null && !foodList.isEmpty()) {
					for (int i = 0; i < foodList.size(); i++) {
						JsonObject detail = foodList.getJsonObject(i);
						FoodDetail foodDetail = new FoodDetail();
						foodDetail.setCode(detail.getString("code"));
						foodDetail.setPortion(detail.getDouble("portion"));
						if (null == detail.getInteger("counter"))
							foodDetail.setCounter(1);
						else
							foodDetail.setCounter(detail.getInteger("counter"));
						foodDetails.add(foodDetail);
						items.add(detail.getString("code"));
					}
				}

				List<FoodDetail> foodDetailsList = new ArrayList<FoodDetail>();
				foodDetailsList.addAll(foodDetails);

				List<String> list = new ArrayList<>();
				Map<String, FoodDetail> map = new HashMap<String, FoodDetail>();
				for (FoodDetail fd : foodDetailsList) {
					list.add(fd.getCode());
					map.put(fd.getCode(), fd);
				}

				List<String> list1 = new ArrayList<>();
				Map<String, FoodDetail> map1 = new HashMap<String, FoodDetail>();
				for (FoodDetail fd : prefFoodDetailsList) {
					list1.add(fd.getCode());
					map1.put(fd.getCode(), fd);
				}

				// finalFoodDetails
				Iterator<Map.Entry<String, FoodDetail>> itr = map1.entrySet().iterator();
				while (itr.hasNext()) {
					Map.Entry<String, FoodDetail> entry = itr.next();
					if (list.contains(entry.getKey())) {
						FoodDetail fd = map.get(entry.getKey());
						if (null == fd.getCounter() || fd.getCounter() == 0)
							fd.setCounter(1);
						else
							fd.setCounter(fd.getCounter() + 2);
						map.put(entry.getKey(), fd);
					} else {
						FoodDetail fd = map1.get(entry.getKey());
						if (null == fd.getCounter() || fd.getCounter() == 0)
							fd.setCounter(1);
						map.put(entry.getKey(), fd);
					}
				}

				itr = map.entrySet().iterator();
				while (itr.hasNext()) {
					Map.Entry<String, FoodDetail> entry = itr.next();
					finalFoodDetails.add(entry.getValue());
				}
			}

			promise.complete(finalFoodDetails);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Get customer diet preference for refresh.
	 * 
	 * @param preference
	 * @param email
	 * @return Future<Set<FoodDetail>>
	 */
	public Future<Set<FoodDetail>> getCustDietPrefV2ForRefresh(CustDietPreference preference, String email) {
		String method = "MongoRepositoryWrapper getCustDietPrefV2ForRefresh() [" + email + "]";
		Promise<Set<FoodDetail>> promise = Promise.promise();
		Set<FoodDetail> foodDetails = new HashSet<FoodDetail>();
		Set<FoodDetail> prefDoodDetails = preference.getFoods();
		List<FoodDetail> prefFoodDetailsList = new ArrayList<FoodDetail>();
		prefFoodDetailsList.addAll(prefDoodDetails);

		Set<FoodDetail> finalFoodDetails = new HashSet<FoodDetail>();
		JsonObject query = new JsonObject().put("_id", email);
		client.rxFindOne("CUST_DIET_PREF_V2", query, null).subscribe(res -> {
			List<String> items = new ArrayList<>();
			if (res != null) {
				JsonArray foodList = res.getJsonArray("foodList");
				if (foodList != null && !foodList.isEmpty()) {
					for (int i = 0; i < foodList.size(); i++) {
						JsonObject detail = foodList.getJsonObject(i);
						FoodDetail foodDetail = new FoodDetail();
						foodDetail.setCode(detail.getString("code"));
						foodDetail.setPortion(detail.getDouble("portion"));
						if (null == detail.getInteger("counter"))
							foodDetail.setCounter(1);
						else
							foodDetail.setCounter(detail.getInteger("counter"));
						foodDetails.add(foodDetail);
						items.add(detail.getString("code"));
					}
				}

				List<FoodDetail> foodDetailsList = new ArrayList<FoodDetail>();
				foodDetailsList.addAll(foodDetails);

				List<String> list = new ArrayList<>();
				Map<String, FoodDetail> map = new HashMap<String, FoodDetail>();
				for (FoodDetail fd : foodDetailsList) {
					list.add(fd.getCode());
					map.put(fd.getCode(), fd);
				}

				List<String> list1 = new ArrayList<>();
				Map<String, FoodDetail> map1 = new HashMap<String, FoodDetail>();
				for (FoodDetail fd : prefFoodDetailsList) {
					list1.add(fd.getCode());
					map1.put(fd.getCode(), fd);
				}

				// finalFoodDetails
				Iterator<Map.Entry<String, FoodDetail>> itr = map1.entrySet().iterator();
				while (itr.hasNext()) {
					Map.Entry<String, FoodDetail> entry = itr.next();
					if (list.contains(entry.getKey())) {
						FoodDetail fd = map.get(entry.getKey());
						if (null == fd.getCounter() || fd.getCounter() == 0)
							fd.setCounter(1);
						else
							fd.setCounter(fd.getCounter() + 2);
						map.put(entry.getKey(), fd);
					} else {
						FoodDetail fd = map1.get(entry.getKey());
						if (null == fd.getCounter() || fd.getCounter() == 0)
							fd.setCounter(1);
						map.put(entry.getKey(), fd);
					}
				}

				itr = map.entrySet().iterator();
				while (itr.hasNext()) {
					Map.Entry<String, FoodDetail> entry = itr.next();
					finalFoodDetails.add(entry.getValue());
				}
			}

			promise.complete(finalFoodDetails);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Get customer diet preference via email.
	 * 
	 * @param email
	 * @param data
	 * @param traceId
	 * @return Future<DBResult>
	 */
	public Future<DBResult> getCustDietPrefCode(String email, DBResult data, String traceId) {
		String method = "MongoRepositoryWrapper getCustDietPrefCode() " + traceId + "-[" + email + "]";
		Promise<DBResult> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFindOne("CUST_DIET_PREF_V2", query, null).subscribe(res -> {
			List<FoodDetail> list = new ArrayList<FoodDetail>();
			if (res == null || res.isEmpty()) {
				res = new JsonObject();
			} else {
				try {
					JsonArray jsonArray = res.getJsonArray("foodList");
					for (int i = 0; i < jsonArray.size(); i++) {
						JsonObject jsonObject = jsonArray.getJsonObject(i);
						FoodDetail foodDeta = new FoodDetail();
						foodDeta.setCode(jsonObject.getString("code"));
						list.add(foodDeta);
					}

					data.setPrefCode(list);
				} catch (Exception ex) {
					logger.error("##### " + method + " EXCEPTION -->> " + ex.getMessage());
				}
			}
			promise.complete(data);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get customer preference list.
	 * 
	 * @param filterData
	 * @param traceId
	 * @return Future<List<JsonObject>>
	 */
	public Future<List<JsonObject>> getCustDietPrefList(DBResult filterData, String traceId) {
		List<Future<JsonObject>> futures = filterData.getPrefCode().stream().map(mapper -> {
			Future<JsonObject> future = getDietPlanForCode(mapper.getCode(), mapper.getPortion());
			return future;
		}).collect(Collectors.toList());
		return Functional.allOfFutures(futures);
	}

	/**
	 * Get customer preference list.
	 * 
	 * @param filterData
	 * @param traceId
	 * @return Future<List<JsonObject>>
	 */
	public Future<List<JsonObject>> getCustDietPrefListDB(DBResult filterData, String traceId) {
		List<Future<JsonObject>> futures = filterData.getPrefCode().stream().map(mapper -> {
			Future<JsonObject> future = getDietPlanForCodeDB(mapper.getCode(), mapper.getPortion());
			return future;
		}).collect(Collectors.toList());
		return Functional.allOfFutures(futures);
	}

	/**
	 * Get customer preference list.
	 * 
	 * @param filterData
	 * @param traceId
	 * @return Future<List<JsonObject>>
	 */
	public Future<List<JsonObject>> getCustDietPreferencesFromDB(DBResult filterData, String traceId) {
		List<Future<JsonObject>> futures = filterData.getPrefCode().stream().map(mapper -> {
			Future<JsonObject> future = getFoodsFromFoodCode(mapper.getCode());
			return future;
		}).collect(Collectors.toList());
		return Functional.allOfFutures(futures);
	}

	/**
	 * Get diet plan via food code.
	 * 
	 * @param code
	 * @param portion
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getDietPlanForCode(String code, Double portion) {
		String method = "MongoRepositoryWrapper getDietPlanForCode()";
		logger.info("##### " + method + "    CODE -->> " + code);
		logger.info("##### " + method + " PORTION -->> " + portion);
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", code);
		client.rxFindOne("DIET_PLAN", query, null).subscribe(res -> {
			res.put("imageUrl", config.getString("imageBaseUrl") + "/" + res.getString("code") + ".png");
			FoodFilterUtils.updateDietCalories(res, portion);
			res.put("portion", portion);
			res.put("originalPortion", res.getInteger("portion"));
			res.put("category", res.getString("Type"));
			promise.complete(res);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Fetch food item via food code.
	 * 
	 * @param email
	 * @param foodId
	 * @param jsonObject
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> fetchFood(String email, String foodId, JsonObject jsonObject, String traceId) {
		String method = "MongoRepositoryWrapper fetchFood() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", foodId);
		client.rxFindOne("DIET_PLAN", query, null).subscribe(res -> {
			JsonObject json = new JsonObject();
			JsonObject response = new JsonObject();
			json.put("code", "0001");
			json.put("message", "fail");
			if (null == res || res.isEmpty()) {
				json.put("message", "No Diet Found [" + foodId + "]");
			} else {
				json.put("foodItem", foodId);
				json.put("Name", res.getString("Food"));
				json.put("recipe", res.getString("recipe"));
				json.put("steps", res.getString("steps"));
				json.put("video", res.getString("video"));
				json.put("courtesy", res.getString("courtesy"));
				json.put("Protien", res.getDouble("Protien"));
				json.put("Carbs", res.getDouble("Carbs"));
				json.put("Fiber", res.getDouble("Fiber"));
				json.put("Fat", res.getDouble("Fat"));
				json.put("Calories", res.getDouble("Calories"));
				json.put("portion", res.getDouble("portion"));
				json.put("remark", res.getString("Remark"));
				JsonArray diseaseMasterArr = jsonObject.getJsonArray("diseases");
				JsonArray avoidInArray = res.getJsonArray("AvoidIn");
				JsonArray recommendedInArray = res.getJsonArray("RecommendedIn");
				JsonArray avoidedItems = new JsonArray();
				JsonArray recommendedItems = new JsonArray();

				diseaseMasterArr.forEach(action -> {
					JsonObject jsonItem = (JsonObject) action;
					String code = jsonItem.getString("code");
					String value = jsonItem.getString("value");
					if (null != avoidInArray && avoidInArray.size() > 0 && avoidInArray.contains(code))
						avoidedItems.add(value);

					if (null != recommendedInArray && recommendedInArray.size() > 0
							&& recommendedInArray.contains(code))
						recommendedItems.add(value);
				});

				json.put("avoidIn", avoidedItems);
				json.put("recommendedIn", recommendedItems);
				json.put("imageUrl", config.getString("imageBaseUrl") + "/" + res.getString("code") + ".png");

				response.put("code", "0000");
				response.put("message", "success");
				response.put("dietItem", updateFoodForRecommendedFor(email, json,
						jsonObject.getJsonArray("customerDiseases"), recommendedInArray, traceId));
			}
			promise.complete(response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get food item via food code.
	 * 
	 * @param code
	 * @param portion
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getDietPlanForCodeDB(String code, Double portion) {
		String method = "MongoRepositoryWrapper getDietPlanForCode()";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", code);
		client.rxFindOne("DIET_PLAN", query, null).subscribe(res -> {
			res.put("imageUrl", config.getString("imageBaseUrl") + "/" + res.getString("code") + ".png");
			FoodFilterUtils.updateDietCalories(res, portion);
			promise.complete(res);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Get food items via food code.
	 * 
	 * @param code
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getFoodsFromFoodCode(String code) {
		String method = "MongoRepositoryWrapper getFoodsFromFoodCode()";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", code);
		client.rxFindOne("DIET_PLAN", query, null).subscribe(res -> {
			res.put("imageUrl", config.getString("imageBaseUrl") + "/" + res.getString("code") + ".png");
			promise.complete(res);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Get diet plan for code.
	 * 
	 * @param code
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getDietPlanForCode(String code, String traceId) {
		String method = "MongoRepositoryWrapper getDietPlanForCode() " + traceId;
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", code);
		client.rxFindOne("DIET_PLAN", query, null).subscribe(res -> {
			res.put("imageUrl", config.getString("imageBaseUrl") + "/" + res.getString("code") + ".png");
			res.put("originalPortion", res.getInteger("portion"));

			promise.complete(res);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Create order for customer.
	 * 
	 * @param request
	 * @param email
	 * @param traceId
	 * @return Future<CreateOrder>
	 */
	public Future<CreateOrder> createOrder(CreateOrder request, String email, String traceId) {
		String method = "MongoRepositoryWrapper createOrder() " + traceId + "-[" + email + "]";
		Calendar cal = Calendar.getInstance(); // creates calendar
		cal.setTime(new Date()); // sets calendar time/date
		cal.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		cal.add(Calendar.MINUTE, 30); // add 30 minutes

		logger.info("##### " + method + " CREATE ORDER REQUEST -->> " + request.toString());
		Promise<CreateOrder> promise = Promise.promise();
		JsonObject payload = new JsonObject();
		payload.put("emailId", email);
		payload.put("orderId", request.getOrderId());

		logger.info("##### " + method + " TESTING FOR EMAIL");
		if (email.contains("d.raghvendra@") || email.contains("raghvendradwivedi3101@")
				|| email.contains("draghvendra3101@") || email.contains("puneet0205@")
				|| email.contains("puneetm0205@"))
			request.setAmount(1.0);
		logger.info("##### " + method + " TESTING FOR EMAIL 2 -->> " + request.getAmount());
		payload.put("amount", request.getAmount());
		payload.put("couponCode", request.getCouponCode());

		String createdDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(cal.getTime());
		payload.put("createdDate", createdDate);
		payload.put("durationInMonth", request.getValidityInMonth());
		payload.put("durationInDays", request.getValidityInDays());
		String txnId = UUID.randomUUID().toString();
		payload.put("txnId", txnId);
		logger.info("##### " + method + " PAYLOAD -->> " + payload);
		client.rxSave("PAYMENT_DETAIL", payload).subscribe(res -> {
			request.setTxnId(res);
			promise.complete(request);

		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	@SuppressWarnings("deprecation")
	public Future<ConfirmPaymentRequest> createOrderForFreeAccount(ConfirmPaymentRequest request, String traceId) {
		String method = "MongoRepositoryWrapper createOrderForFreeAccount() " + traceId + "-[" + request.getEmailId()
				+ "]";
		Integer freeAccountValidityInDays = this.config.getInteger("freeAccountValidity");
		Calendar cal = Calendar.getInstance(); // creates calendar
		cal.setTime(new Date()); // sets calendar time/date
		cal.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		cal.add(Calendar.MINUTE, 30); // add 30 minutes

		Promise<ConfirmPaymentRequest> promise = Promise.promise();
		JsonObject payload = new JsonObject();
		payload.put("emailId", request.getEmailId());
		String uniqueId = RandomStringUtils.randomAlphanumeric(14);
		String orderId = "order_" + uniqueId;
		payload.put("orderId", orderId);
		payload.put("amount", 0);
		payload.put("couponCode", request.getCouponCode());
		String createdDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(cal.getTime());
		payload.put("createdDate", createdDate);
		String paymentId = "pay_" + uniqueId;
		payload.put("paymentId", paymentId);
		boolean isAccountFree = true;
		payload.put("isAccountFree", isAccountFree);
		payload.put("durationInDays", freeAccountValidityInDays);
		String txnId = UUID.randomUUID().toString();
		payload.put("txnId", txnId);
		payload.put("validityInDays", request.getValidityInDays());
		logger.info("##### " + method + " CREATE ORDER FOR FREE ACCOUNT - PAYLOAD -->> " + payload);
		client.rxSave("PAYMENT_DETAIL", payload).subscribe(res -> {
			request.setOrderId(orderId);
			request.setAmount(0.0);
			request.setPaymentId(paymentId);
			request.setIsAccountFree(isAccountFree);
			request.setTxnId(txnId);
			request.setSignature(RandomStringUtils.randomAlphanumeric(65));
			request.setValidityInDays(request.getValidityInDays());
			request.setCouponCode("NA");
			logger.info("##### " + method + " CREATE ORDER (FREE ACCOUNT - REQUEST) -->> " + request.toString());
			promise.complete(request);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR 1 -->>" + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Update customer subscription plan (ie. plan upgrade and cancel/refund plan).
	 * 
	 * @param refundRequest
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> updateSubscriptionPlan(RefundRequest refundRequest, String email, String traceId) {
		String method = "MongoRepositoryWrapper updateSubscriptionPlan() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject payload = new JsonObject();
		payload.put("isRefunded", refundRequest.getIsRefunded());
		payload.put("refundedAmount", refundRequest.getRefundedAmount());
		payload.put("refundedDate", refundRequest.getRefundedDate());
		JsonObject query = new JsonObject();
		query.put("emailId", email);
		query.put("isActive", true);
		logger.info("##### " + method + "                             HTTP STATUS CODE -->> "
				+ refundRequest.getStatusCode());
		if ("200".equalsIgnoreCase(refundRequest.getStatusCode())) {
			JsonObject update = new JsonObject();
			try {
				update = new JsonObject().put("$set", new JsonObject().put("isRefunded", refundRequest.getIsRefunded())
						.put("refundedAmount", refundRequest.getRefundedAmount())
						.put("refundedDate", refundRequest.getRefundedDate()).put("isActive", false)
						.put("expiryDate", new SimpleDateFormat("dd-MMM-yyyy").format(
								new SimpleDateFormat("ddMMyyyy").parse(ApiUtils.getCurrentDate(email, traceId)))));
			} catch (ParseException e) {
				e.printStackTrace();
			}

			client.rxUpdateCollection("PLAN_SUBCRIPTION_DETAIL", query, update).subscribe(res -> {
				JsonObject response = new JsonObject();
				response.put("isRefunded", refundRequest.getIsRefunded());
				response.put("refundedAmount", refundRequest.getRefundedAmount());
				response.put("refundedDate", refundRequest.getRefundedDate());
				response.put("isActive", false);
				logger.info("##### " + method + " RESPONSE                                     -->> " + response);
				promise.complete(response);
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		} else {
			JsonObject response = new JsonObject();
			response.put("code", refundRequest.getStatusCode());
			response.put("message", refundRequest.getDescription());
			logger.info("##### " + method + " 							  	   RESPONSE -->> " + response);
			logger.info("##### " + method + " PLAN SUBSCRIPTION DETAIL UPDATE FAIL FOR -->> " + email);
			promise.complete(response);
		}

		return promise.future();
	}

	/**
	 * Get Coupon detail via coupon code.
	 * 
	 * @param email
	 * @param couponCode
	 * @param traceId
	 * @return Future<CreateOrder>
	 */
	public Future<CreateOrder> getCouponDetail(String email, String couponCode, String traceId) {
		String method = "MongoRepositoryWrapper getCouponDetail() " + traceId + "-[" + email + "]";
		Promise<CreateOrder> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("_id", couponCode);
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFindOne("COUPON_MASTER", query, null).subscribe(res -> {

			if (res == null || res.isEmpty()) {
				promise.fail("Invalid coupon code");
			} else {
				CreateOrder createOrder = new CreateOrder();
				createOrder.setAmount(res.getDouble("discountedAmount"));
				createOrder.setValidityInMonth(res.getInteger("durationInMonth"));
				createOrder.setValidityInDays(res.getInteger("period"));
				createOrder.setCouponCode(couponCode);
				logger.info("##### " + method + " COUPON -->> " + createOrder.toString());
				promise.complete(createOrder);
			}
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Get coupon detail via coupon code.
	 * 
	 * @param request
	 * @param traceId
	 * @return Future<ConfirmPaymentRequest>
	 */
	public Future<ConfirmPaymentRequest> getCouponDetail(ConfirmPaymentRequest request, String traceId) {
		String method = "MongoRepositoryWrapper getCouponDetail() " + traceId + "-[" + request.getEmailId() + "]";
		Promise<ConfirmPaymentRequest> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("_id", request.getCouponCode());
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFindOne("COUPON_MASTER", query, null).subscribe(res -> {

			if (res == null || res.isEmpty()) {
				promise.fail("Invalid coupon code");
			} else {
				request.setValidityInDays(res.getInteger("period"));
				request.setCouponCode(request.getCouponCode());
				promise.complete(request);
				logger.info("##### " + method + " COUPON DETAIL -->> " + request.toString());
			}

		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Get payment Detail via transactionId.
	 * 
	 * @param txnId
	 * @param request
	 * @param traceId
	 * @return Future<SubscribeRequest>
	 */
	public Future<SubscribeRequest> getPaymentDetail(String txnId, SubscribeRequest request, String traceId) {
		String method = "MongoRepositoryWrapper getPaymentDetail() " + traceId + "-[" + request.getEmailId() + "]";
		Promise<SubscribeRequest> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("txnId", txnId);
		logger.info("##### " + method + " TRANSACTION ID -->> " + request.getTxnId());
		client.rxFindOne("PAYMENT_DETAIL", query, null).subscribe(res -> {
			if (res == null || res.isEmpty()) {
				promise.fail("Invalid txnId");
			} else {
				request.setCouponCode(res.getString("couponCode"));
				request.setValidityInDays(res.getInteger("durationInDays"));
				request.setAmount(res.getDouble("amount"));
				promise.complete(request);
			}

		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Get payment detail for free account (amount=0).
	 * 
	 * @param request
	 * @param traceId
	 * @return Future<SubscribeRequest>
	 */
	public Future<SubscribeRequest> getPaymentDetailForFreeAccount(SubscribeRequest request, String traceId) {
		String method = "MongoRepositoryWrapper getPaymentDetailForFreeAccount() " + traceId + "-["
				+ request.getEmailId() + "]";
		Promise<SubscribeRequest> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("txnId", request.getTxnId());
		logger.info("##### " + method + " TRANSACTION ID   -->> " + request.getTxnId());
		client.rxFindOne("PAYMENT_DETAIL", query, null).subscribe(res -> {

			if (res == null || res.isEmpty()) {
				promise.fail("Invalid txnId");
			} else {
				request.setCouponCode(res.getString("couponCode"));
				request.setValidityInDays(res.getInteger("durationInDays"));
				request.setAmount(res.getDouble("amount"));
				promise.complete(request);
			}

		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Get payment detail via orderId.
	 * 
	 * @param request
	 * @param traceId
	 * @return Future<ConfirmPaymentRequest>
	 */
	public Future<ConfirmPaymentRequest> getPaymentDetailForCoupon(ConfirmPaymentRequest request, String traceId) {
		String method = "MongoRepositoryWrapper getPaymentDetailForCoupon() " + traceId + "-[" + request.getEmailId()
				+ "]";
		Promise<ConfirmPaymentRequest> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("orderId", request.getOrderId());
		client.rxFindOne("PAYMENT_DETAIL", query, null).subscribe(res -> {

			if (res == null || res.isEmpty()) {
				promise.fail("Invalid OrderId");
			} else {
				request.setCouponCode(res.getString("couponCode"));
				request.setValidityInDays(res.getInteger("durationInDays"));
				request.setAmount(res.getDouble("amount"));
				logger.info("##### " + method + " PAYMENT DETAIL REQUEST -->> " + request.toString());
				promise.complete(request);
			}

		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Confirm payment for amount > 0.
	 * 
	 * @param request
	 * @param traceId
	 * @return Future<ConfirmPaymentRequest>
	 */
	public Future<ConfirmPaymentRequest> confirmPayment(ConfirmPaymentRequest request, String traceId) {
		String method = "MongoRepositoryWrapper confirmPayment() " + traceId + "-[" + request.getEmailId() + "]";
		SubscribeRequest subRequest = new SubscribeRequest();
		Calendar calll = Calendar.getInstance(); // creates calendar
		calll.setTime(new Date()); // sets calendar time/date
		calll.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		calll.add(Calendar.MINUTE, 30); // add 30 minutes
		String currentDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(calll.getTime());
		Promise<ConfirmPaymentRequest> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("emailId", request.getEmailId());
		query.put("orderId", request.getOrderId());
		JsonObject update = new JsonObject();
		update.put("$set", new JsonObject().put("status", request.getStatus()).put("updatedDate", currentDate)
				.put("paymentId", request.getPaymentId()).put("signature", request.getSignature()));
		logger.info("##### " + method + " QUERY   -->> " + query);
		logger.info("##### " + method + " PAYLOAD -->> " + update);
		client.rxUpdateCollection("PAYMENT_DETAIL", query, update).subscribe(res -> {
			subRequest.setTxnId(request.getTxnId());
			subRequest.setOrderId(request.getOrderId());
			subRequest.setPaymentId(request.getPaymentId());
			subRequest.setEmailId(request.getEmailId());
			subRequest.setSignature(request.getSignature());
			subRequest.setValidityInDays(request.getValidityInDays());
			logger.info("##### " + method + " SUBSCRIBED REQUEST -->> " + subRequest);
			promise.complete(request);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Confirm payment for free account (amount=0).
	 * 
	 * @param request
	 * @param traceId
	 * @return Future<ConfirmPaymentRequest>
	 */
	public Future<ConfirmPaymentRequest> confirmPaymentForFreeAccount(ConfirmPaymentRequest request, String traceId) {
		String method = "MongoRepositoryWrapper confirmPaymentForFreeAccount() " + traceId + "-[" + request.getEmailId()
				+ "]";
		SubscribeRequest subRequest = new SubscribeRequest();
		Calendar calll = Calendar.getInstance(); // creates calendar
		calll.setTime(new Date()); // sets calendar time/date
		calll.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		calll.add(Calendar.MINUTE, 30); // add 30 minutes
		String currentDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(calll.getTime());
		Promise<ConfirmPaymentRequest> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("emailId", request.getEmailId());
		query.put("orderId", request.getOrderId());
		JsonObject update = new JsonObject();
		update.put("$set", new JsonObject().put("status", request.getStatus()).put("updatedDate", currentDate)
				.put("paymentId", request.getPaymentId()).put("signature", request.getSignature()));
		client.rxUpdateCollection("PAYMENT_DETAIL", query, update).subscribe(res -> {
			logger.info("##### " + method + " CONFIRM PAYMENT PAYMENT DETAIL FOR FREE ACCOUNT INSIDE.");
			subRequest.setTxnId(request.getTxnId());
			subRequest.setOrderId(request.getOrderId());
			subRequest.setPaymentId(request.getPaymentId());
			subRequest.setEmailId(request.getEmailId());
			subRequest.setSignature(request.getSignature());
			subRequest.setCouponCode(request.getCouponCode());
			promise.complete(request);
			logger.info(
					"##### " + method + " SUBSCRIPTION REQUEST (CONFIRM PAYMENT FOR FREE ACCOUNT) -->> " + subRequest);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Get Subscribed plan via email.
	 * 
	 * @param request
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> subcribePlan(ConfirmPaymentRequest request, String traceId) {
		String method = "MongoRepositoryWrapper subcribePlan() " + traceId + "-[" + request.getEmailId() + "]";
		Promise<JsonObject> promise = Promise.promise();
		logger.info("##### " + method + " SUBSCRIPTION REQUEST -->> " + request);
		if (null != request && null != request.getPaymentId() && !"".equalsIgnoreCase(request.getPaymentId())) {
			Calendar cal1 = Calendar.getInstance(); // creates calendar
			cal1.setTime(new Date()); // sets calendar time/date
			cal1.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
			cal1.add(Calendar.MINUTE, 30); // add 30 minutes
			String currentDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(cal1.getTime());
			JsonObject response = new JsonObject();
			JsonObject query = new JsonObject();
			query.put("emailId", request.getEmailId());
			logger.info("##### " + method + " QUERY -->> " + query);
			client.rxFindOne("PLAN_SUBCRIPTION_DETAIL", query, null).subscribe(res -> {
				try {
					JsonObject payload = new JsonObject();
					Calendar cal = Calendar.getInstance();
					cal.setTime(new Date());
					cal.add(Calendar.HOUR_OF_DAY, 10); // adds ten hour
					cal.add(Calendar.MINUTE, 30); // add 30 minutes

					Double amount = request.getAmount();
					Double recentPaidAmount = request.getAmount();
					String expiryDate = ApiUtils.getCouponExpiryDate(request.getValidityInDays(), 0);
					logger.info("##### " + method + " EXISTING EXPIRY DATE -->> " + expiryDate);
					boolean isPlanUpgraded = false;
					boolean isNewPlan = false;
					boolean isFreeAccountTaken = false;
					boolean isCustomeAllowedForFreeAccount = false;
					if (null != res && !res.isEmpty() && res.containsKey("isFreeAccountTaken")
							&& res.getBoolean("isFreeAccountTaken")
							&& (request.getAmount().equals(0) || request.getAmount().equals(0.0))) {
						isCustomeAllowedForFreeAccount = true;
						logger.info("##### " + method + " FREE ACCOUNT ALREADY TAKEN ON ["
								+ res.getString("createdDate") + "]");
						promise.fail("not allowed for free account.");
					} else if (request.getAmount().equals(0) || request.getAmount().equals(0.0)) {
						isFreeAccountTaken = true;
						logger.info("##### " + method + "           FREE ACCOUNT EXPIRY DATE -->> " + expiryDate);
					} else if (null != res && !res.isEmpty()) {
						String existingExpirydate = res.getString("expiryDate");
						SimpleDateFormat myFormat = new SimpleDateFormat("dd-MMM-yyyy");

						logger.info("##### " + method + "      IS REQUEST FOR PLAN UPGRADE?? -->> " + isPlanUpgraded);
						logger.info("##### " + method + "                       TOTAL AMOUNT -->> " + amount);
						Date curentDate = new Date();
						logger.info("##### " + method + "                        EXPIRY DATE -->> " + expiryDate);
						Date expirydate = new SimpleDateFormat("ddMMyyyy").parse(expiryDate);
						if (!expirydate.before(curentDate)) {
							isPlanUpgraded = true;
							logger.info("##### " + method + "    IS REQUEST FOR PLAN UPGRADE -->> " + isPlanUpgraded);
							logger.info("##### " + method + "        IS EXISTING PLAN ACTIVE -->> "
									+ (!expirydate.before(curentDate)));
							if (null != res.getDouble("amount"))
								amount += res.getDouble("amount");
							try {
								Date dateBefore = myFormat.parse(existingExpirydate);
								logger.info("##### " + method + "                 DATE BEFORE -->> " + dateBefore);
								Date dateAfter = new Date();
								long difference = dateBefore.getTime() - dateAfter.getTime();
								logger.info("##### " + method + "                 DIFFERENCE -->> " + difference);
								Long daysBetween = (difference / (1000 * 60 * 60 * 24));
								logger.info("##### " + method + "     TOTAL DAYS (AFTER UPGRADE) -->> "
										+ daysBetween.intValue());
								expiryDate = ApiUtils.getCouponExpiryDate(request.getValidityInDays(),
										daysBetween.intValue());
								logger.info("##### " + method + " TOTAL DAYS (AFTER UPGRADE) -->> " + daysBetween);
							} catch (Exception e) {
								e.printStackTrace();
								logger.error("##### " + method + "                      ERROR -->> " + e.getMessage());
							}
						} else {
							isNewPlan = true;
						}
					}

					if (!isCustomeAllowedForFreeAccount) {
						List<Double> list = new ArrayList<>();
						list.add(amount);
						if (!isPlanUpgraded) { // NEW PLAN SUBSCRIPTION
//							payload.put("_id", request.getEmailId() + "::"
//									+ new SimpleDateFormat("dd-MMM-yyyy").format(cal.getTime()));
							payload.put("emailId", request.getEmailId());
							payload.put("orderId", request.getOrderId());
							payload.put("paymentId", request.getPaymentId());
							payload.put("couponCode", request.getCouponCode());
							payload.put("createdDate", currentDate);
							payload.put("txnId", request.getTxnId());
							payload.put("isActive", true);
							payload.put("signature", request.getSignature());
							payload.put("expiryDate", new SimpleDateFormat("dd-MMM-yyyy")
									.format(new SimpleDateFormat("ddMMyyyy").parse(expiryDate)));
							payload.put("upgradedDate", new SimpleDateFormat("dd-MMM-yyyy").format(cal.getTime()));
							payload.put("amount", amount);
							payload.put("recentPaidAmount", recentPaidAmount);
							payload.put("isUpgraded", isPlanUpgraded);
							payload.put("isFreeAccountTaken", isFreeAccountTaken);
							request.setExpiryDate(expiryDate);
							String expiryDateNew = new SimpleDateFormat("dd-MMM-yyyy")
									.format(new SimpleDateFormat("ddMMyyyy").parse(expiryDate));
							logger.info("##### " + method + " PAYLOAD -->> " + payload);
							client.rxSave("PLAN_SUBCRIPTION_DETAIL", payload).subscribe(res1 -> {
								logger.info("##### " + method + " SUBSCRIBE PLAN FOR FREE ACCOUNT SAVE SUCCESSFULLY");
								response.put("code", "0000");
								response.put("message", "success");
								response.put("amount", list.get(0));
								response.put("recentPaidAmount", request.getAmount());
								response.put("planExpiryDate", expiryDateNew);
								promise.complete(response);
							}, (ex) -> {
								logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
								promise.fail(ex.getMessage());
							});
						} else { // PLAN UPGRADE
							String expiryDateNew = new SimpleDateFormat("dd-MMM-yyyy")
									.format(new SimpleDateFormat("ddMMyyyy").parse(expiryDate));
							payload = new JsonObject().put("$set", new JsonObject().put("emailId", request.getEmailId())
									.put("orderId", request.getOrderId()).put("paymentId", request.getPaymentId())
									.put("couponCode", request.getCouponCode()).put("createdDate", currentDate)
									.put("txnId", request.getTxnId()).put("isActive", true)
									.put("signature", request.getSignature()).put("expiryDate", expiryDateNew)
									.put("isUpgraded", isPlanUpgraded)
									.put("upgradedDate", new SimpleDateFormat("dd-MMM-yyyy").format(cal.getTime()))
									.put("isNewPlan", isNewPlan).put("amount", amount)
									.put("recentPaidAmount", recentPaidAmount).put("isUpgraded", isPlanUpgraded));
							request.setExpiryDate(expiryDate);
							logger.info("##### " + method + " QUERY    -->> " + query);
							logger.info("##### " + method + " PAYLOAD  -->> " + payload);
							client.rxUpdateCollection("PLAN_SUBCRIPTION_DETAIL", query, payload).subscribe(res1 -> {
								logger.info("##### " + method + " SUCCESSFULLY UPDATED SUBSCRIPTION DETAIL FOR ["
										+ request.getEmailId() + "]");
								promise.complete(new JsonObject().put("code", "000").put("message", "Success")
										.put("amount", list.get(0)).put("recentPaidAmount", recentPaidAmount)
										.put("planExpiryDate", expiryDateNew));
							}, ex -> {
								promise.fail("unable to update");
							});
						}
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}, (ex) -> {
				logger.error("##### " + method + " ERROR 1 -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		} else {
			promise.fail("No Payment Id found");
		}

		return promise.future();
	}

	/**
	 * Get subscribed plan for free account (amount=0).
	 * 
	 * @param request
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> subcribePlanForFreeAccount(ConfirmPaymentRequest request, String traceId) {
		String method = "MongoRepositoryWrapper subcribePlan() " + traceId + "-[" + request.getEmailId() + "]";
		Promise<JsonObject> promise = Promise.promise();
		if (null != request && null != request.getPaymentId() && !"".equalsIgnoreCase(request.getPaymentId())) {
			Calendar cal1 = Calendar.getInstance(); // creates calendar
			cal1.setTime(new Date()); // sets calendar time/date
			cal1.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
			cal1.add(Calendar.MINUTE, 30); // add 30 minutes
			String currentDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(cal1.getTime());
			JsonObject response = new JsonObject();
			JsonObject query = new JsonObject();
			query.put("emailId", request.getEmailId());
			logger.info("##### " + method + " QUERY -->> " + query);
			client.rxFindOne("PLAN_SUBCRIPTION_DETAIL", query, null).subscribe(res -> {
				logger.info("##### " + method + " RES -->> " + res);
				try {
					JsonObject payload = new JsonObject();
					Calendar cal = Calendar.getInstance();
					cal.setTime(new Date());
					cal.add(Calendar.HOUR_OF_DAY, 10); // adds ten hour
					cal.add(Calendar.MINUTE, 30); // add 30 minutes

					Double amount = request.getAmount();
					String expiryDate = ApiUtils.getCouponExpiryDate(request.getValidityInDays(), 0);

					logger.info("##### " + method + "                   EXISTING EXPIRY DATE -->> " + expiryDate);
					boolean isPlanUpgraded = false;
					boolean isNewPlan = false;
					boolean isFreeAccountTaken = false;
					boolean isCustomeAllowedForFreeAccount = false;
					if (null != res && !res.isEmpty() && res.containsKey("isFreeAccountTaken")
							&& res.getBoolean("isFreeAccountTaken")
							&& (request.getAmount().equals(0) || request.getAmount().equals(0.0))) {
						isCustomeAllowedForFreeAccount = true;
						logger.info("##### " + method + " FREE ACCOUNT ALREADY TAKEN ON ["
								+ res.getString("createdDate") + "]");
						promise.fail("not allowed for free account.");
					} else if (request.getAmount().equals(0) || request.getAmount().equals(0.0)) {
						isFreeAccountTaken = true;
						logger.info("##### " + method + "           FREE ACCOUNT EXPIRY DATE -->> " + expiryDate);
					} else if (null != res && !res.isEmpty()) {
						String existingExpirydate = res.getString("expiryDate");
						SimpleDateFormat myFormat = new SimpleDateFormat("dd-MMM-yyyy");

						logger.info("##### " + method + "      IS REQUEST FOR PLAN UPGRADE?? -->> " + isPlanUpgraded);
						amount = request.getAmount();
						if (null != res.getDouble("amount")) {
							amount = res.getDouble("amount") + request.getAmount();
						}

						logger.info("##### " + method + "                       TOTAL AMOUNT -->> " + amount);
						Date curentDate = new Date();
						logger.info("##### " + method + "                        EXPIRY DATE -->> " + expiryDate);
						Date expirydate = new SimpleDateFormat("ddMMyyyy").parse(expiryDate);
						if (!expirydate.before(curentDate)) {
							isPlanUpgraded = true;
							logger.debug("##### " + method + "        IS EXISTING PLAN ACTIVE -->> "
									+ (!expirydate.before(curentDate)));
							try {
								Date dateBefore = myFormat.parse(existingExpirydate);
								logger.info("##### " + method + "                 DATE BEFORE -->> " + dateBefore);
								Date dateAfter = new Date();
								logger.info("##### " + method + "                  DATE AFTER -->> " + dateBefore);
								long difference = dateBefore.getTime() - dateAfter.getTime();
								logger.info("##### " + method + "                 DIFFERENCE -->> " + difference);
								Long daysBetween = (difference / (1000 * 60 * 60 * 24));
								logger.info("##### " + method + "     TOTAL DAYS (AFTER UPGRADE) -->> "
										+ daysBetween.intValue());
								expiryDate = ApiUtils.getCouponExpiryDate(request.getValidityInDays(),
										daysBetween.intValue());
								logger.info("##### " + method + " TOTAL DAYS (AFTER UPGRADE) -->> " + daysBetween);
							} catch (Exception e) {
								e.printStackTrace();
								logger.error("##### " + method + "                      ERROR -->> " + e.getMessage());
							}
						} else {
							isNewPlan = true;
						}
					}

					if (!isCustomeAllowedForFreeAccount) {
						logger.info("##### " + method + "            UPDATED EXPIRY DATE 1  -->> " + expiryDate);
						if (!isPlanUpgraded) { // NEW PLAN SUBSCRIPTION
							payload.put("emailId", request.getEmailId());
							payload.put("orderId", request.getOrderId());
							payload.put("paymentId", request.getPaymentId());
							payload.put("couponCode", request.getCouponCode());
							payload.put("createdDate", currentDate);
							payload.put("txnId", request.getTxnId());
							payload.put("isActive", true);
							payload.put("signature", request.getSignature());
							logger.info("##### " + method + "         UPDATED EXPIRY DATE 2  -->> " + expiryDate);
							payload.put("expiryDate", new SimpleDateFormat("dd-MMM-yyyy")
									.format(new SimpleDateFormat("ddMMyyyy").parse(expiryDate)));
							payload.put("upgradedDate", new SimpleDateFormat("dd-MMM-yyyy").format(cal.getTime()));
							payload.put("amount", amount);
							payload.put("isUpgraded", isPlanUpgraded);
							payload.put("isFreeAccountTaken", isFreeAccountTaken);
							request.setExpiryDate(expiryDate);
							String expiryDateNew = new SimpleDateFormat("dd-MMM-yyyy")
									.format(new SimpleDateFormat("ddMMyyyy").parse(expiryDate));
							logger.info(
									"##### " + method + " SUBSCRIBE PLAN FOR FREE ACCOUNT - PAYLOAD -->> " + payload);
							client.rxSave("PLAN_SUBCRIPTION_DETAIL", payload).subscribe(res1 -> {
								logger.info("##### " + method + " SUBSCRIBE PLAN FOR FREE ACCOUNT SAVE SUCCESSFULLY");
								response.put("code", "0000");
								response.put("message", "success");
								response.put("planExpiryDate", expiryDateNew);
								promise.complete(response);
							}, (ex) -> {
								logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
								promise.fail(ex.getMessage());
							});
						} else { // PLAN UPGRADE
							logger.info(
									"##### " + method + "     EXISTING REQUEST TO UPDATE -->> " + request.getEmailId());
							logger.info("##### " + method + "                    EXPIRY DATE -->> " + expiryDate);
							logger.info(
									"##### " + method + "     EXISTING REQUEST TO UPDATE -->> " + request.getEmailId());
							String expiryDateNew = new SimpleDateFormat("dd-MMM-yyyy")
									.format(new SimpleDateFormat("ddMMyyyy").parse(expiryDate));
							payload = new JsonObject().put("$set", new JsonObject().put("emailId", request.getEmailId())
									.put("orderId", request.getOrderId()).put("paymentId", request.getPaymentId())
									.put("couponCode", request.getCouponCode()).put("createdDate", currentDate)
									.put("txnId", request.getTxnId()).put("isActive", true)
									.put("signature", request.getSignature()).put("expiryDate", expiryDateNew)
									.put("isUpgraded", isPlanUpgraded)
									.put("upgradedDate", new SimpleDateFormat("dd-MMM-yyyy").format(cal.getTime()))
									.put("isNewPlan", isNewPlan).put("amount", amount)
									.put("isUpgraded", isPlanUpgraded));
							request.setExpiryDate(expiryDate);
							client.rxUpdateCollection("PLAN_SUBCRIPTION_DETAIL", query, payload).subscribe(res1 -> {
								logger.debug("##### " + method + " SUCCESSFULLY UPDATED SUBSCRIPTION DETAI");
								promise.complete(new JsonObject().put("code", "000").put("message", "Success")
										.put("planExpiryDate", expiryDateNew));
							}, ex -> {
								promise.fail("unable to update");
							});
						}
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}, (ex) -> {
				logger.error("##### " + method + " ERROR 1 -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		} else {
			promise.fail("No Payment Id found");
		}

		return promise.future();
	}

	/**
	 * Get subscribed plan for discounted coupon.
	 * 
	 * @param emailId
	 * @param validitInDays
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> subcribePlanForDiscountCoupon(String emailId, Integer validitInDays) {
		String method = "MongoRepositoryWrapper subcribePlan() [" + emailId + "]";

		Promise<JsonObject> promise = Promise.promise();
		JsonObject payload = new JsonObject();
		try {
			Calendar cal = Calendar.getInstance();
			cal.setTime(new Date());
			String expiryDate = ApiUtils.getCouponExpiryDate(validitInDays, 0);
			String currentDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(cal.getTime());
			payload.put("emailId", emailId);
			payload.put("orderId", "NA");
			payload.put("couponCode", "NA");
			payload.put("createdDate", currentDate);
			payload.put("amount", 0);
			payload.put("txnId", "NA");
			payload.put("isActive", false);
			payload.put("expiryDate",
					new SimpleDateFormat("dd-MMM-yyyy").format(new SimpleDateFormat("ddMMyyyy").parse(expiryDate)));
			payload.put("paymentId", "NA");
			payload.put("signature", "NA");
			logger.info("##### " + method + " UPDATED EXPIRY DATE 2  -->> " + expiryDate);
			String expiryDateNew = new SimpleDateFormat("dd-MMM-yyyy")
					.format(new SimpleDateFormat("ddMMyyyy").parse(expiryDate));
			client.rxSave("PLAN_SUBCRIPTION_DETAIL", payload).subscribe(res1 -> {
				logger.info("##### " + method + " SUCCESSFULLY SAVED DISCOUNTED PLAN SUBCRIPTION_DETAILS");
				JsonObject response = new JsonObject();
				response.put("code", "0000");
				response.put("message", "success");
				response.put("planExpiryDate", expiryDateNew);
				promise.complete(response);
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		} catch (Exception ex) {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		}

		return promise.future();
	}

	/**
	 * Check lan subscription.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<Boolean>
	 */
	public Future<Boolean> checkPlanSubscription(String email, String traceId) {
		String method = "MongoRepositoryWrapper checkPlanSubscription() " + traceId + "-[" + email + "]";
		Promise<Boolean> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("emailId", email);
		query.put("isActive", true);
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFindOne("PLAN_SUBCRIPTION_DETAIL", query, null).subscribe(res -> {
			if (res != null && !res.isEmpty()) {
				promise.complete(true);
			} else {
				promise.complete(false);
			}

		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get subscribed plan via emailId and isActive status.
	 * 
	 * @param email
	 * @param profile
	 * @param traceId
	 * @return Future<Profile>
	 */
	public Future<Profile> getSubscribePlanDate(String email, Profile profile, String traceId) {
		String method = "MongoRepositoryWrapper getSubscribePlanDate() " + traceId + "-[" + email + "]";
		Promise<Profile> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("emailId", email);
		query.put("isActive", true);
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFindOne("PLAN_SUBCRIPTION_DETAIL", query, null).subscribe(res -> {
			if (res != null && !res.isEmpty()) {
				profile.setSubscribeDate(res.getString("createdDate"));
				promise.complete(profile);
			} else {
				promise.complete(profile);
			}
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		logger.debug("##### " + method + " PROFILE -->> " + profile);
		return promise.future();
	}

	/**
	 * Get complete subscribed plan.
	 * 
	 * @param traceId
	 * @return Future<CustProfile>
	 */
	public Future<CustProfile> getCompleteSubscribedPlanDate(String traceId) {
		String method = "MongoRepositoryWrapper getCompleteSubscribedPlanDate() " + traceId;
		Promise<CustProfile> promise = Promise.promise();
		JsonObject query = new JsonObject();
		client.rxFind("PLAN_SUBCRIPTION_DETAIL", query).map(map -> {
			return map;
		}).subscribe(res1 -> {
			CustProfile profile = new CustProfile();
			List<Customer> profiles = new ArrayList<Customer>();
			res1.forEach(action -> {
				try {
					Customer customer = new Customer();
					customer.setSubscribeDate(action.getString("createdDate"));
					customer.setEmail(action.getString("emailId"));
					profiles.add(customer);
				} catch (Exception e) {
					logger.error("##### " + method + " ERROR 1 -->> " + e.getMessage());
					e.printStackTrace();
				}
			});

			profile.setProfile(profiles);
			promise.complete(profile);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR 2 -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Check if plan is subscribed.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<Boolean> isPlanTaken(String email, String traceId) {
		String method = "MongoRepositoryWrapper isPlanTaken() " + traceId + "-[" + email + "]";
		Promise<Boolean> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("emailId", email);
		query.put("isActive", true);
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFindOne("PLAN_SUBCRIPTION_DETAIL", query, null).subscribe(res -> {
			if (res != null && !res.isEmpty()) {
				promise.fail("One plan already taken");
			} else {
				promise.complete(true);
			}
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Check if plan is active.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<Boolean> isPlanActive(String email, String traceId) {
		String method = "MongoRepositoryWrapper isPlanActive() " + traceId + "-[" + email + "]";
		Promise<Boolean> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("emailId", email);
		query.put("isActive", true);
		SimpleDateFormat format = new SimpleDateFormat("ddMMyyyy");
		logger.debug("##### " + method + " QUERY -->> " + query);
		client.rxFindOne("PLAN_SUBCRIPTION_DETAIL", query, null).subscribe(res -> {
			boolean isPlanActive = true;
			if (res != null && !res.isEmpty()) {
				try {
					Date expiryDate = format.parse(res.getString("expiryDate"));
					Date currentDate = new Date();
					if (expiryDate.before(currentDate))
						isPlanActive = true;
				} catch (ParseException e) {
					e.printStackTrace();
				}
				promise.complete(isPlanActive);
			} else {
				promise.fail("One plan already taken");
			}
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Check if plan is available.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<Boolean> isPlanAlreadyAvailable(String email, String traceId) {
		String method = "MongoRepositoryWrapper isPlanAlreadyAvailable() " + traceId + "-[" + email + "]";
		Promise<Boolean> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("emailId", email);
		query.put("isActive", true);
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFindOne("PLAN_SUBCRIPTION_DETAIL", query, null).subscribe(res -> {
			if (res != null && !res.isEmpty()) {
				promise.fail("One plan already taken");
			} else {
				promise.complete(true);
			}
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get payment detail.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<RefundRequest> getPaymentDetail(String email, String traceId) {
		String method = "MongoRepositoryWrapper getPaymentDetail() " + traceId + "-[" + email + "]";
		RefundRequest response = new RefundRequest();
		Promise<RefundRequest> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("emailId", email);
		query.put("isActive", true);
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFindOne("PLAN_SUBCRIPTION_DETAIL", query, null).subscribe(res -> {
			if (res != null && !res.isEmpty()) {
				response.setEmail(email);
				response.setPaymentId(res.getString("paymentId"));
				response.setAmount(res.getDouble("amount"));
				promise.complete(response);
			} else {
				promise.fail("No subscription");
			}
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get subscribed plan payment detail(s).
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<CancelRequest> getSubscribedPlanPaymentDetail(String email, String traceId) {
		String method = "MongoRepositoryWrapper getSubscribedPlanPaymentDetail() " + traceId + "-[" + email + "]";
		CancelRequest response = new CancelRequest();
		Promise<CancelRequest> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("emailId", email);
		query.put("isActive", true);
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFindOne("PLAN_SUBCRIPTION_DETAIL", query, null).subscribe(res -> {
			if (res != null && !res.isEmpty()) {
				response.setPaymentId(res.getString("paymentId")); // NEED TO ASK FAKAHARE TO PASS PAYMENT ID
				response.setPaymentId(res.getString("orderId"));
				response.setAmount(res.getDouble("amount") - 100);
				promise.complete(response);
			} else {
				promise.fail("No subscription");
			}
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Remove subscription.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> removeRemovePlans(String email, String traceId) {
		String method = "MongoRepositoryWrapper removeRemovePlans() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("emailId", email);
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxRemoveDocument("PLAN_SUBCRIPTION_DETAIL", query).subscribe(res -> {
			promise.complete(new JsonObject().put("code", "0000"));
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get fod by code.
	 * 
	 * @param code
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getFoodByCode(String code, String traceId) {
		String method = "MongoRepositoryWrapper getFoodByCode() " + traceId;
		logger.debug("##### " + method + " CODE -->> " + code);
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("_id", code);
		client.rxFindOne("DIET_PLAN", query, null).subscribe(res -> {
			res.put("imageUrl", config.getString("imageBaseUrl") + "/" + res.getString("code") + ".png");
			res.put("originalPortion", res.getInteger("portion"));
			promise.complete(res);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Get diet plan via dlot.
	 * 
	 * @param type
	 * @param traceId
	 * @return Future<List<JsonObject>>
	 */
	public Future<List<JsonObject>> getDieltPlanBySlots(String type, String traceId) {
		String method = "MongoRepositoryWrapper getDieltPlanBySlots() " + traceId;
		logger.debug("##### " + method + " TYPE -->> " + type);
		JsonObject query = new JsonObject();
		String[] r = type.split(",");
		if (r.length > 1) {
			// query.put("Slots", new JsonArray().add(Integer.parseInt(r[1]))) ;
		}
		Promise<List<JsonObject>> promise = Promise.promise();
		client.rxFind("DIET_PLAN", query).subscribe(res -> {
			res = res.stream().filter(x -> this.getDietBySlot(x, 7))
					.sorted((p1, p2) -> p1.getDouble("Calories").compareTo(p2.getDouble("Calories")))
					.collect(Collectors.toList());
			promise.complete(res);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Update dailty diet list.
	 * 
	 * @param data
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> saveDietPlan(JsonObject data, String email, String date, String traceId) {
		String method = "MongoRepositoryWrapper saveDietPlan() " + traceId + "-[" + email + "]";
		Calendar cal1 = Calendar.getInstance(); // creates calendar
		cal1.setTime(new Date()); // sets calendar time/date
		cal1.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		cal1.add(Calendar.MINUTE, 30); // add 30 minutes
		String currentDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(cal1.getTime());
		Promise<JsonObject> promise = Promise.promise();
		try {
			JsonObject query = new JsonObject();
			query.put("_id", new JsonObject().put("email", email).put("date", date));
			query.put("data", data);
			query.put("updatedTime", new JsonObject().put("$date", currentDate));

			logger.info("##### " + method + " QUERY -->> " + query);
			client.rxSave("CUST_DAILY_DIET", query).subscribe(res -> {
				logger.info("##### " + method + " DIETPLAN CACHED SUCCESSFULLY");
				JsonObject response = new JsonObject();
				response.put("code", "0000");
				response.put("message", "success");
				promise.complete(response);
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		} catch (Exception e) {
			logger.error("##### " + method + " EXCEPTION -->> " + e.getMessage());
			e.printStackTrace();
		}

		return promise.future();
	}

	/**
	 * Subscribed discounted coupon.
	 * 
	 * @param emailId
	 * @param data
	 * @return Future<JsonObject>
	 */
	@SuppressWarnings("deprecation")
	public Future<JsonObject> subscribeDiscountedCoupon(String emailId, JsonObject data) {
		String method = "MongoRepositoryWrapper subscribeDiscountedCoupon()";
		DateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
		Integer discountedCouponValidityInDays = this.config.getInteger("discountedCouponValidity");
		Calendar calll = Calendar.getInstance(); // creates calendar
		calll.setTime(new Date()); // sets calendar time/date
		Calendar call2 = Calendar.getInstance(); // creates calendar
		call2.setTime(new Date()); // sets calendar time/date
		calll.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		calll.add(Calendar.MINUTE, 30); // add 30 minutes
		String currentDate = dateFormat.format(calll.getTime());
		// String currentDate = convertServerDateTimeToRequestedTimeZone(new Date(),
		// "yyyy-MM-dd'T'HH:mm:ss'Z'", "IST");
		logger.info("##### " + method + "                       CURRENT DATE -->> " + currentDate);

		// String expiryDate = "";
		call2.add(Calendar.DATE, discountedCouponValidityInDays); // adds no of days
		call2.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		call2.add(Calendar.MINUTE, 30); // add 30 minutes
		// expiryDate = dateFormat.format(call2.getTime());
		String expiryDate = convertServerDateTimeToRequestedTimeZone(call2.getTime(), "yyyy-MM-dd'T'HH:mm:ss'Z'",
				"IST");
		logger.info("##### " + method + "                        EXPIRY DATE -->> " + expiryDate);
		// CREATING UNIQUE COUPON CODE FOR COUPON OFFER
		String uniqueCouponCode = RandomStringUtils.randomAlphanumeric(7);
		logger.info("##### " + method + "                 UNIQUE COUPON CODE -->> " + uniqueCouponCode);
		logger.info("##### " + method + " DISCOUNTED COUPON VALIDITY IN DAYS -->> " + discountedCouponValidityInDays);

		Promise<JsonObject> promise = Promise.promise();
		try {
			JsonObject query = new JsonObject();
			query.put("_id", uniqueCouponCode);
			query.put("uniqueCouponCode", uniqueCouponCode);
			query.put("validityInDays", discountedCouponValidityInDays);
			query.put("isCouponActive", true);
			query.put("generatedBy", emailId);
			query.put("consumedBy", "");
			query.put("couponExpiryDateTime", expiryDate);
			query.put("createdDateTime", currentDate);
			query.put("consumedDateTime", "");
			logger.info("##### " + method + " QUERY -->> " + query);
			client.rxSave("DISCOUNTED_PLAN_SUBCRIPTION_DETAIL", query).subscribe(res -> {
				promise.complete(data);

			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return promise.future();
	}

	/**
	 * Get today dietlist.
	 * 
	 * @param email
	 * @param date
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getTodayDietList(String email, String date, String traceId) {
		String method = "MongoRepositoryWrapper getTodayDietList() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("_id", new JsonObject().put("email", email).put("date", date));
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFindOne("CUST_DAILY_DIET", query, null).subscribe(res -> {
			if (res != null && !res.isEmpty()) {
				JsonObject json = res.getJsonObject("data");
				JsonArray dietListArr = json.getJsonArray("diets");
				dietListArr.forEach(action -> {
					if (null != action) {
						JsonObject obj = (JsonObject) action;
						JsonArray dataArr = obj.getJsonArray("data");
						dataArr.forEach(mapper -> {
							if (null != mapper) {
								JsonObject food = (JsonObject) mapper;
								food.remove("Slots");
								food.remove("Season");
								food.remove("AvoidIn");
								food.remove("Remark");
								food.remove("Detox");
								food.remove("Special_diet");
								food.remove("courtesy");
								food.remove("recipe");
								food.remove("steps");
								food.remove("updated_by");
								food.remove("video");
								food.remove("Special_slot");
								food.remove("Ultra_special");
							}
						});
					}
				});

				promise.complete(res.getJsonObject("data"));
//				promise.complete(json);
			} else {
				logger.info("##### " + method + " FAILED TO FETCH CACHE DIET");
				promise.fail("No Diet saved for today");
			}
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Get dietplan list from Cache - for specified date.
	 * 
	 * @param email
	 * @param slotId
	 * @param foodCodeList
	 * @param date
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getDietPlanFromCache(String email, Integer slotId, Set<FoodDetail> foodCodeList,
			String date, String traceId) {
		String method = "MongoRepositoryWrapper getDietPlanFromCache() " + traceId + "-[" + email + "]";
		logger.debug("##### " + method + "     SLOT -->> " + slotId);
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject();
		String filteredDate = ApiUtils.getCurrentDate(email, traceId);
		if (null != date && !"".equalsIgnoreCase(date))
			filteredDate = date;
		
//		query.put("_id", new JsonObject().put("email", email).put("date", ApiUtils.getCurrentDate(email, traceId)));
		query.put("_id", new JsonObject().put("email", email).put("date", filteredDate));
		client.rxFindOne("CUST_DAILY_DIET", query, null).subscribe(res -> {
			if (res != null && !res.isEmpty())
				promise.complete(res.getJsonObject("data"));
			else
				promise.fail("No Diet saved for today");
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Update todays dietplan.
	 * 
	 * @param email
	 * @param slot
	 * @param data
	 * @param slotObject
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> updateTodayDietPlan(String email, Integer slot, JsonObject data, JsonObject slotObject,
			String traceId) {
		String method = "MongoRepositoryWrapper updateTodayDietPlan() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonArray dietList = data.getJsonArray("diets");
		dietList.forEach(action -> {
			JsonObject sloData = (JsonObject) action;
			if (slot == sloData.getInteger("slot"))
				dietList.add(slotObject);
		});

//		for (int i = 0; i < dietList.size(); i++) {
//			JsonObject sloData = dietList.getJsonObject(i);
//			if (slot == sloData.getInteger("slot")) {
//				dietList.set(i, slotObject);
//				break;
//			}
//		}

		JsonObject query = new JsonObject().put("_id",
				new JsonObject().put("email", email).put("date", ApiUtils.getCurrentDate(email, traceId)));
		JsonObject update = new JsonObject();
		update.put("$set", new JsonObject().put("data", data));
		logger.info("##### " + method + "                   QUERY -->> " + query);
		logger.info("##### " + method + " FINAL UPDATE/SAVED DATA -->> " + update);
		client.rxUpdateCollection("CUST_DAILY_DIET", query, update).subscribe(res -> {
			promise.complete(new JsonObject().put("code", "000").put("message", "Success"));
		}, ex -> {
			promise.fail("unable to update");
		});

		return promise.future();
	}

	/**
	 * Update today dietplan in Cache.
	 * 
	 * @param email
	 * @param slot
	 * @param data
	 * @param slotObject
	 * @param foodCodeList
	 * @param date
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> updateTodayDietPlanCache(String email, Integer slot, JsonObject data,
			JsonObject slotObject, Set<FoodDetail> foodCodeList, String date, String traceId) {
		String method = "MongoRepositoryWrapper updateTodayDietPlanCache() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		List<FoodDetail> foodMainList = new ArrayList<FoodDetail>();

		if (null != data && !data.isEmpty()) {
			foodMainList.addAll(foodCodeList);
			JsonArray dietList = data.getJsonArray("diets");
			logger.info("###### " + method + "      SLOT OBJECT -->> " + slotObject.encodePrettily());
			dietList.forEach(action -> {
				JsonObject jsonObj = (JsonObject) action;
				if (slot == jsonObj.getInteger("slot")) {
					JsonArray slotObjArray = slotObject.getJsonArray("data");
					jsonObj.put("data", slotObjArray);
				}
			});

			logger.info("###### " + method + " UPDATED DIET LIST -->> " + data);
			String filteredDate = ApiUtils.getCurrentDate(email, traceId);
			if (null != date && !"".equalsIgnoreCase(date))
				filteredDate = date;

			// CALCULATE CALORIES
			FoodFilterUtils.calculateCalories(data);
			JsonObject query = new JsonObject().put("_id.email",email).put("_id.date", filteredDate);
			JsonObject update = new JsonObject();
			update.put("$set", new JsonObject().put("data", data));
			logger.info("###### " + method + " QUERY -->> " + query);
			client.rxUpdateCollection("CUST_DAILY_DIET", query, update).subscribe(res -> {
				logger.info("##### " + method + " DIETLIST UPDATED SUCCESSFULLY");

				JsonArray dietListArr = data.getJsonArray("diets");
				dietListArr.forEach(action -> {
					if (null != action) {
						JsonObject obj = (JsonObject) action;
						JsonArray dataArr = obj.getJsonArray("data");
						dataArr.forEach(mapper -> {
							if (null != mapper) {
								JsonObject food = (JsonObject) mapper;
								food.remove("Slots");
								food.remove("Season");
								food.remove("AvoidIn");
								food.remove("Remark");
								food.remove("Detox");
								food.remove("Special_diet");
								food.remove("courtesy");
								food.remove("recipe");
								food.remove("steps");
								food.remove("updated_by");
								food.remove("video");
								food.remove("Special_slot");
								food.remove("Ultra_special");
							}
						});
					}
				});

				promise.complete(new JsonObject().put("code", "000").put("message", "Success").put("dietplan", data));
			}, ex -> {
				promise.fail("unable to update");
			});
		} else {
			promise.complete(
					new JsonObject().put("code", "0001")
							.put("message",
									"[" + ApiUtils.getCurrentDate(email, traceId)
											+ "] - Dietplan cache is unavailable for [" + email + "]")
							.put("dietplan", data));
		}

		return promise.future();
	}

	/**
	 * Update todays dietplan in Cache - refresh module.
	 * 
	 * @param email
	 * @param slot
	 * @param data
	 * @param slotObject
	 * @param foodCodeList
	 * @param date
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> updateTodayDietPlanCacheRefresh(String email, Integer slot, JsonObject data,
			JsonObject slotObject, Set<FoodDetail> foodCodeList, String date, String traceId) {
		String method = "MongoRepositoryWrapper updateTodayDietPlanCacheRefresh() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		List<FoodDetail> foodMainList = new ArrayList<FoodDetail>();

		if (null != data && !data.isEmpty()) {
			JsonObject json1 = slotObject.getJsonObject("oldFoodItem");
			String oldFoodType = json1.getString("Type");
			logger.info("##### " + method + "     OLD FOOD ITEM -->> " + json1);
			logger.info("##### " + method + "     FOO CODE LIST -->> " + foodCodeList);
			foodMainList.addAll(foodCodeList);
			logger.debug("##### " + method + "   RESPONSE BEFORE -->> " + data);
			JsonArray dietList = data.getJsonArray("diets");
			logger.debug("###### " + method + "        DIET LIST -->> " + dietList);
			logger.info("###### " + method + "      SLOT OBJECT -->> " + slotObject.encodePrettily());
			String code = foodCodeList.stream().findFirst().get().getCode();

			dietList.forEach(action -> {
				JsonObject jsonObj = (JsonObject) action;
				JsonArray updatedJsonArr = new JsonArray();
				if (slot == jsonObj.getInteger("slot")) {
					JsonArray jsonObjArray = jsonObj.getJsonArray("data");
					jsonObjArray.forEach(action1 -> {
						JsonObject jsonObject = (JsonObject) action1;
						if (code.equalsIgnoreCase(jsonObject.getString("itemCode"))) {
							JsonObject json = slotObject.getJsonArray("data").getJsonObject(0);
							logger.debug("##### " + method + "                    OLD FOODTYPE -->> " + oldFoodType);
							if (("B".equalsIgnoreCase(oldFoodType) && "B".equalsIgnoreCase(json.getString("Type")))
									|| ("WC".equalsIgnoreCase(oldFoodType)
											&& "WC".equalsIgnoreCase(json.getString("Type")))
									|| ("WP".equalsIgnoreCase(oldFoodType)
											&& "WP".equalsIgnoreCase(json.getString("Type")))) {
								Double oldPortion = json.getDouble("portion");
								logger.info("##### " + method + "            OLD PORTION -->> " + oldPortion);
								Double newPortion = jsonObject.getDouble("portion");
								logger.info("##### " + method + "            NEW PORTION -->> " + newPortion);

								Double updatedPortionDev = oldPortion / newPortion;
								logger.info("##### " + method + "     UPDATE PORTION DEV -->> " + updatedPortionDev);
								Double updatedPortionModulo = oldPortion % newPortion;
								logger.info("##### " + method + "   UPDATE PORTON MODULO -->> " + updatedPortionModulo);

								Double updatedPortion = updatedPortionDev;
								if (updatedPortionModulo < 1 && updatedPortionModulo >= 0.5)
									updatedPortion = updatedPortionDev + 0.5;
								logger.info("##### " + method + "        UPDATED PORTION -->> " + updatedPortion);

								if (updatedPortion < 1)
									updatedPortion = 1.0;

								Double updatedCalories = slotObject.getJsonObject("oldFoodItem").getDouble("Calories")
										* updatedPortion;
								logger.info("##### " + method + "       UPDATED CALORIES -->> " + updatedCalories);

								// IF FOOD IS OF TYPE 'B'
								if ("B".equalsIgnoreCase(oldFoodType) && "B".equalsIgnoreCase(json.getString("Type"))) {
									updatedPortion = jsonObject.getDouble("portion");
									updatedCalories = json.getDouble("Calories") * updatedPortion;
								}

								json.put("portion", updatedPortion);
								json.put("Calories", Double.parseDouble(ApiUtils.getDecimal(updatedCalories)));
							} else {
								json.put("Calories",
										Double.parseDouble(ApiUtils.getDecimal(json.getDouble("Calories"))));
							}

							logger.info("###### " + method + "         UPDATED FOOD ITEM -->> " + json);
							updatedJsonArr.add(json);
							if (slotObject.containsKey("oldFoodItem"))
								slotObject.remove("oldFoodItem");
						} else {
							updatedJsonArr.add(jsonObject);
						}
					});

					jsonObj.put("data", updatedJsonArr);
				}
			});

			// CALCULATE CALORIES
			FoodFilterUtils.calculateCalories(data);
			String filteredDate = ApiUtils.getCurrentDate(email, traceId);
			if (null != date && !"".equalsIgnoreCase(date))
				filteredDate = date;

//			JsonObject query = new JsonObject().put("_id", new JsonObject().put("email", email).put("date", date));
			JsonObject query = new JsonObject().put("_id.email",email).put("_id.date", filteredDate);
			JsonObject update = new JsonObject();
			update.put("$set", new JsonObject().put("data", data));
			client.rxUpdateCollection("CUST_DAILY_DIET", query, update).subscribe(res -> {
				logger.info("##### " + method + " REFRESHED CACHE DIETPLAN UPDATED SUCCESSFULLY.");
				JsonArray dietListArr = data.getJsonArray("diets");
				dietListArr.forEach(action -> {
					if (null != action) {
						JsonObject obj = (JsonObject) action;
						JsonArray dataArr = obj.getJsonArray("data");
						dataArr.forEach(mapper -> {
							if (null != mapper) {
								JsonObject food = (JsonObject) mapper;
								food.remove("Slots");
								food.remove("Season");
								food.remove("AvoidIn");
								food.remove("Remark");
								food.remove("Detox");
								food.remove("Special_diet");
								food.remove("courtesy");
								food.remove("recipe");
								food.remove("steps");
								food.remove("updated_by");
								food.remove("video");
								food.remove("Special_slot");
								food.remove("Ultra_special");
							}
						});
					}
				});

				promise.complete(new JsonObject().put("code", "0000").put("message", "Success").put("dietplan", data));
			}, ex -> {
				promise.fail("unable to update");
			});
		} else {
			promise.complete(
					new JsonObject().put("code", "0001")
							.put("message",
									"[" + ApiUtils.getCurrentDate(email, traceId)
											+ "] - Dietplan cache is unavailable for [" + email + "]")
							.put("dietplan", data));
		}

		return promise.future();
	}

	/**
	 * Update dietplan in Cache.
	 * 
	 * @param email
	 * @param slot
	 * @param data
	 * @param slotDetail
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> updateDietPlanInCache(String email, Integer slot, JsonObject data, JsonObject slotDetail,
			String traceId) {
		String method = "MongoRepositoryWrapper updateDietPlanInCache() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();

		JsonArray dietList = data.getJsonArray("diets");
		logger.info("##### " + method + "         SLOTOBJECT -->> " + slotDetail.encodePrettily());

		JsonArray listArr = new JsonArray();
		dietList.forEach(action -> {
			JsonObject sloData = (JsonObject) action;
			if (slot == sloData.getInteger("slot")) {
				logger.info("##### " + method + " JSON FOUND         -->> " + slotDetail.getJsonObject("diets"));
				listArr.add(sloData);
				logger.info("##### " + method + " LIST ARRAY         -->> " + listArr);
			}
		});

//		for (int i = 0; i < dietLis.size(); i++) {
//			JsonObject sloData = dietLis.getJsonObject(i);
//			if (slot == sloData.getInteger("slot")) {
//				logger.info("##### " + method + "     FOUND -->> " + slotDetail.getJsonObject("diets"));
//				dietLis.set(i, slotDetail);
//				logger.info("##### " + method + " AFTER SET -->> " + dietLis.encodePrettily());
//				break;
//			}
//		}

		JsonObject query = new JsonObject().put("_id",
				new JsonObject().put("email", email).put("date", ApiUtils.getCurrentDate(email, traceId)));
		JsonObject update = new JsonObject();
		update.put("$set", new JsonObject().put("data", data));
		logger.info("##### " + method + " QUERY   -->> " + query);
		logger.info("##### " + method + " PAYLOAD -->> " + update);
		client.rxUpdateCollection("CUST_DAILY_DIET", query, update).subscribe(res -> {
			promise.complete(new JsonObject().put("code", "000").put("message", "Success"));
		}, ex -> {
			promise.fail("unable to update");
		});
		return promise.future();
	}

	/**
	 * Get diet by food type.
	 * 
	 * @param prefList
	 * @param types
	 * @return List<JsonObject>
	 */
	public List<JsonObject> getDietByTypeForSlection(final List<JsonObject> prefList, final List<String> types) {
		List<JsonObject> list = prefList.stream().filter(x -> types.contains(x.getString("Type")))
				.collect(Collectors.toList());
		return list;
	}

	/**
	 * Get diet list for food code.
	 * 
	 * @param slot
	 * @param foodList
	 * @param traceId
	 * @return Future<List<JsonObject>>
	 */
	public Future<List<JsonObject>> getDietListForFoodCode(Integer slot, Set<FoodDetail> foodList, String traceId) {
		List<Future<JsonObject>> futures = foodList.stream().map(mapper -> {
			Future<JsonObject> future = null;
			future = getDietPlanForCode(mapper.getCode(), mapper.getPortion());
			return future;
		}).collect(Collectors.toList());

		return Functional.allOfFutures(futures);
	}

	/**
	 * Get food item via food code.
	 * 
	 * @param slot
	 * @param foodItem
	 * @param json
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getDietListForRefreshFoodCode(Integer slot, FoodDetail foodItem, JsonObject json,
			String traceId) {
		String method = "MongoRepositoryWrapper getDietListForRefreshFoodCode() " + traceId;
		Promise<JsonObject> promise = Promise.promise();
		logger.info("##### " + method + " SLOT      -->> " + slot);
		logger.info("##### " + method + " FOOD ITEM -->> " + foodItem.getCode());

		JsonObject obj = json.getJsonObject("newFoodItem");
		JsonObject query = new JsonObject().put("_id", obj.getString("itemCode"));
		logger.info("##### " + method + " QUERY     -->> " + query);
		client.rxFindOne("DIET_PLAN", query, null).subscribe(res -> {
			res.put("imageUrl", config.getString("imageBaseUrl") + "/" + res.getString("code") + ".png");
			res.put("category", res.getString("Type"));

			JsonObject response = new JsonObject();
			res.put("Calories", obj.getDouble("Calories") * obj.getDouble("portion"));
			response.put("newFoodItem", res);
			response.put("oldFoodItem", json.getJsonObject("oldFoodItem"));

			promise.complete(response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Create slot detail.
	 * 
	 * @param slot
	 * @param foodList
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> createSlotDetail(Integer slot, List<JsonObject> foodList, String traceId) {
		String method = "MongoRepositoryWrapper createSlotDetail() " + traceId;
		Promise<JsonObject> promise = Promise.promise();
		logger.info("##### " + method + " SLOT -->> " + slot);
		JsonObject response = new JsonObject();
		response.put("slot", slot);
		response.put("Remark", ApiUtils.slotsRemark.get(slot + ""));
		response.put("data", foodList);

		Double totalCalories = foodList.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Calories")).sum();
		Double totalCarbs = foodList.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Carbs")).sum();
		Double totalFat = foodList.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fat")).sum();
		Double totalProtien = foodList.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Protien")).sum();
		Double totalFiber = foodList.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fiber")).sum();
		response.put("totalCalories", Double.parseDouble(ApiUtils.getDecimal(totalCalories)));
		response.put("totalCarbs", Double.parseDouble(ApiUtils.getDecimal(totalCarbs)));
		response.put("totalFat", Double.parseDouble(ApiUtils.getDecimal(totalFat)));
		response.put("totalProtien", Double.parseDouble(ApiUtils.getDecimal(totalProtien)));
		response.put("totalFiber", Double.parseDouble(ApiUtils.getDecimal(totalFiber)));
		promise.complete(response);

		logger.info("##### " + method + " TOTAL CALORIES -->> " + ApiUtils.getDecimal(totalCalories));
		logger.info("##### " + method + " SLOT OBJECT    -->> " + response);
		return promise.future();
	}

	/**
	 * Create slot detail for refresh.
	 * 
	 * @param slot
	 * @param foodItem
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> createSlotRefreshDetail(Integer slot, JsonObject foodItem, String traceId) {
		String method = "MongoRepositoryWrapper createSlotRefreshDetail() " + traceId;
		Promise<JsonObject> promise = Promise.promise();
		logger.info("##### " + method + " SLOT -->> " + slot);
		JsonObject response = new JsonObject();
		response.put("slot", slot);
		response.put("Remark", ApiUtils.slotsRemark.get(slot + ""));
		List<JsonObject> list = new ArrayList<>();
		list.add(foodItem.getJsonObject("newFoodItem"));
		response.put("data", list);

		Double totalCalories = list.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Calories")).sum();
		Double totalCarbs = list.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Carbs")).sum();
		Double totalFat = list.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fat")).sum();
		Double totalProtien = list.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Protien")).sum();
		Double totalFiber = list.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fiber")).sum();
		response.put("totalCalories", Double.parseDouble(ApiUtils.getDecimal(totalCalories)));
		response.put("totalCarbs", Double.parseDouble(ApiUtils.getDecimal(totalCarbs)));
		response.put("totalFat", Double.parseDouble(ApiUtils.getDecimal(totalFat)));
		response.put("totalProtien", Double.parseDouble(ApiUtils.getDecimal(totalProtien)));
		response.put("totalFiber", Double.parseDouble(ApiUtils.getDecimal(totalFiber)));
		response.put("oldFoodItem", foodItem.getJsonObject("oldFoodItem"));
		promise.complete(response);

		logger.info("##### " + method + " REFRESHED TOTAL CALORIES -->> " + ApiUtils.getDecimal(totalCalories));
		logger.info("##### " + method + " REFRESHED SLOT OBJECT    -->> " + response);
		return promise.future();
	}

	/**
	 * Get customer diets details.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<FilterData> getCustDietProfile(String email, String traceId) {
		String method = "MongoRepositoryWrapper getCustDietProfile() " + traceId + "-[" + email + "]";
		Set<String> allPrefood = new HashSet<String>();
		Promise<FilterData> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);

		JsonObject fields = new JsonObject().put("lifeStyle.foodType", "lifeStyle.foodType")
				.put("diet.food", "diet.food").put("diet.drinks", "diet.drinks").put("diet.snacks", "diet.snacks")
				.put("diet.fruits", "diet.fruits").put("diet.dishes", "diet.dishes").put("diet.pules", "diet.pules")
				.put("diet.rice", "diet.rice").put("lifeStyle.communities", "lifeStyle.communities")
				.put("lifeStyle.diseases", "lifeStyle.diseases").put("lifeStyle.calories", "lifeStyle.calories");

		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFindOne("CUST_PROFILE", query, fields).subscribe(res -> {
			FilterData data = new FilterData();
			data.setEmail(email);
			try {
				if (res != null) {
					if (null != res.getJsonObject("lifeStyle")
							&& res.getJsonObject("lifeStyle").getJsonArray("communities") != null
							&& res.getJsonObject("lifeStyle").getJsonArray("communities").size() > 0) {
						JsonArray communities = res.getJsonObject("lifeStyle").getJsonArray("communities");
						logger.info("##### " + method + " COMMUNITIES -->> " + communities);
						List<String> ar = new ArrayList<String>();
						communities.forEach(action -> {
							ar.add(action.toString());
						});
						data.setCommunity(ar);
					}
					if (null != res.getJsonObject("lifeStyle")
							&& res.getJsonObject("lifeStyle").getString("foodType") != null) {
						data.setFoodType(res.getJsonObject("lifeStyle").getString("foodType"));
						logger.info("##### " + method + " FOODTYPE -->> " + data.getFoodType());
					}

					if (null != res.getJsonObject("lifeStyle")
							&& res.getJsonObject("lifeStyle").getDouble("calories") != null) {
						data.setCalories(res.getJsonObject("lifeStyle").getDouble("calories"));
						logger.info("##### " + method + " CALORIES -->> " + data.getCalories());
					}

					if (null != res.getJsonObject("lifeStyle")
							&& res.getJsonObject("lifeStyle").getJsonArray("diseases") != null
							&& res.getJsonObject("lifeStyle").getJsonArray("diseases").size() > 0) {
						JsonArray diseases = res.getJsonObject("lifeStyle").getJsonArray("diseases");
						logger.info("##### " + method + " DISEASES -->> " + diseases);
						List<String> ar = new ArrayList<String>();
						diseases.forEach(action -> {
							ar.add(action.toString());
						});
						data.setDisease(ar);
					}

					try {
						if (null != res.getJsonObject("diet") && res.getJsonObject("diet").getJsonArray("food") != null
								&& res.getJsonObject("diet").getJsonArray("food").size() > 0) {
							JsonArray foods = res.getJsonObject("diet").getJsonArray("food");
							logger.debug("##### " + method + " FOODS -->> " + foods);
							List<String> ar = new ArrayList<String>();
							foods.forEach(action -> {
								ar.add(action.toString());
							});
							data.setFoods(ar);
						}
					} catch (Exception ex) {

					}

					try {
						if (null != res.getJsonObject("diet")
								&& res.getJsonObject("diet").getJsonArray("drinks") != null
								&& res.getJsonObject("diet").getJsonArray("drinks").size() > 0) {
							JsonArray drinks = res.getJsonObject("diet").getJsonArray("drinks");
							logger.debug("##### " + method + " DRINKS -->> " + drinks);
							List<String> ar = new ArrayList<String>();
							drinks.forEach(action -> {
								ar.add(action.toString());
								allPrefood.add(action.toString());
							});
							data.setDrinks(ar);
						}
					} catch (Exception ex) {

					}

					try {
						if (null != res.getJsonObject("diet")
								&& res.getJsonObject("diet").getJsonArray("snacks") != null
								&& res.getJsonObject("diet").getJsonArray("snacks").size() > 0) {
							JsonArray snacks = res.getJsonObject("diet").getJsonArray("snacks");
							logger.debug("##### " + method + " SNACKS -->> " + snacks);
							List<String> ar = new ArrayList<String>();
							snacks.forEach(action -> {
								ar.add(action.toString());
								allPrefood.add(action.toString());
							});

							data.setSnacks(ar);
						}
					} catch (Exception ex) {

					}

					try {
						if (null != res.getJsonObject("diet")
								&& res.getJsonObject("diet").getJsonArray("fruits") != null
								&& res.getJsonObject("diet").getJsonArray("fruits").size() > 0) {
							JsonArray fruits = res.getJsonObject("diet").getJsonArray("fruits");
							logger.debug("##### " + method + " FRUITS -->> " + fruits);
							List<String> ar = new ArrayList<String>();
							fruits.forEach(action -> {
								ar.add(action.toString());
								allPrefood.add(action.toString());
							});
							data.setFruits(ar);
						}
					} catch (Exception ex) {

					}

					try {
						if (null != res.getJsonObject("diet")
								&& res.getJsonObject("diet").getJsonArray("dishes") != null
								&& res.getJsonObject("diet").getJsonArray("dishes").size() > 0) {
							JsonArray dishes = res.getJsonObject("diet").getJsonArray("dishes");
							logger.debug("##### " + method + " DISHES -->> " + dishes);
							List<String> ar = new ArrayList<String>();
							dishes.forEach(action -> {
								ar.add(action.toString());
								allPrefood.add(action.toString());
							});
							data.setDishes(ar);
						}
					} catch (Exception ex) {

					}

					try {
						if (null != res.getJsonObject("diet") && res.getJsonObject("diet").getJsonArray("pules") != null
								&& res.getJsonObject("diet").getJsonArray("pules").size() > 0) {
							JsonArray pulses = res.getJsonObject("diet").getJsonArray("pules");
							logger.debug("##### " + method + " PULSES -->> " + pulses);
							List<String> ar = new ArrayList<String>();
							pulses.forEach(action -> {
								ar.add(action.toString());
								allPrefood.add(action.toString());
							});
							data.setPules(ar);
						}
					} catch (Exception ex) {

					}

					try {
						if (null != res.getJsonObject("diet") && res.getJsonObject("diet").getJsonArray("rice") != null
								&& res.getJsonObject("diet").getJsonArray("rice").size() > 0) {
							JsonArray rice = res.getJsonObject("diet").getJsonArray("rice");
							logger.debug("##### " + method + " RICE -->> " + rice);
							List<String> ar = new ArrayList<String>();
							rice.forEach(action -> {
								ar.add(action.toString());
								allPrefood.add(action.toString());
							});
							data.setRice(ar);
						}
					} catch (Exception ex) {

					}

					logger.debug("##### " + method + " DATA -->> " + data);
					data.setAllPrefood(allPrefood);
					promise.complete(data);
				} else {
					promise.fail("No profile found for [" + email + "]");
				}
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("##### " + method + " ERROR -->> " + e.getMessage());
				promise.fail("No profile found for [" + email + "]");
			}
		}, (ex) -> {
			logger.error("##### " + method + " ERROR2 -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Customer Diet Preferences.
	 * 
	 * @param query
	 * @param profile
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getCustDietPref(String email, JsonObject query, FilterData profile, String traceId) {
		String method = "MongoRepositoryWrapper getCustDietPref() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + " EMAIL -->> " + email);
		Promise<JsonObject> promise = Promise.promise();
		client.rxFind("DIET_PLAN", query).subscribe(res -> {
			res = res.stream().map(mapper -> {
				mapper.remove("_id");
				mapper.put("isSelected", false);
				return mapper;
			}).collect(Collectors.toList());

			JsonObject response = new JsonObject();
			try {
				// drinks
				List<JsonObject> drinksComplete = res.stream().filter(x -> this.notSlot(x, Constants.ZERO)).filter(
						x -> x.getString("Type").equalsIgnoreCase("D") || x.getString("Type").equalsIgnoreCase("DM"))
						.collect(Collectors.toList());
				List<JsonObject> drinks = getFilteredList(profile.getEmail(), drinksComplete, "DRINKS",
						profile.getDrinks(), profile.getFoodType(), profile.getCommunity(), profile.getDisease(),
						traceId);

				// fruits
				List<JsonObject> fruitsComplete = res.stream().filter(x -> x.getString("Type").equalsIgnoreCase("F"))
						.collect(Collectors.toList());
				List<JsonObject> fruits = getFilteredList(profile.getEmail(), fruitsComplete, "FRUITS",
						profile.getFruits(), profile.getFoodType(), profile.getCommunity(), profile.getDisease(),
						traceId);

				// pulses
				List<JsonObject> plscurriesComplete = res.stream()
						.filter(x -> x.getString("Type").equalsIgnoreCase("C")).collect(Collectors.toList());
				List<JsonObject> plscurries = getFilteredList(profile.getEmail(), plscurriesComplete, "PULSES",
						profile.getPules(), profile.getFoodType(), profile.getCommunity(), profile.getDisease(),
						traceId);

				// snacks
				List<JsonObject> snacksComplete = res.stream().filter(this::getSnacksBySlots).filter(this::getSnacks)
						.collect(Collectors.toList());
				List<JsonObject> snacks = getFilteredList(profile.getEmail(), snacksComplete, "SNACKS",
						profile.getSnacks(), profile.getFoodType(), profile.getCommunity(), profile.getDisease(),
						traceId);

				// dishes
				List<JsonObject> dishesComplete = res.stream().filter(x -> x.getString("Type").equalsIgnoreCase("A"))
						.collect(Collectors.toList());
				List<JsonObject> dishes = getFilteredList(profile.getEmail(), dishesComplete, "DISHES",
						profile.getDishes(), profile.getFoodType(), profile.getCommunity(), profile.getDisease(),
						traceId);

				// rice
				List<JsonObject> riceComplete = res.stream().filter(x -> x.getString("Type").equalsIgnoreCase("B"))
						.collect(Collectors.toList());
				List<JsonObject> rice = getFilteredList(profile.getEmail(), riceComplete, "RICE", profile.getRice(),
						profile.getFoodType(), profile.getCommunity(), profile.getDisease(), traceId);

				response.put("drinks", drinks);
				response.put("drinksCount", drinks.size());
				response.put("fruits", fruits);
				response.put("fruitsCount", fruits.size());
				response.put("plscurries", plscurries);
				response.put("plscurriesCount", plscurries.size());
				response.put("snacks", snacks);
				response.put("snacksCount", snacks.size());
				response.put("dishes", dishes);
				response.put("dishesCount", dishes.size());
				response.put("rice", rice);
				response.put("riceCount", rice.size());
			} catch (Exception e) {
				e.printStackTrace();
			}
			promise.complete(response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get customer diet preference update.
	 * 
	 * @param email
	 * @param query
	 * @param profile
	 * @param traceId
	 * @return JsonObject
	 */
	public JsonObject getCustDietPrefUpdate(String email, JsonObject query, FilterData profile, String traceId) {
		String method = "MongoRepositoryWrapper getCustDietPrefUpdate() " + traceId + "-[" + email + "] ";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject response = new JsonObject();
		client.rxFind("DIET_PLAN", query).subscribe(res -> {
			res = res.stream().map(mapper -> {
				mapper.remove("_id");
				mapper.put("isSelected", false);
				return mapper;
			}).collect(Collectors.toList());

			try {
				// drinks
				List<JsonObject> drinksComplete = res.stream().filter(x -> this.notSlot(x, Constants.ZERO))
						.filter(x -> x.getString("Type").equalsIgnoreCase("D")
								|| x.getString("Type").equalsIgnoreCase("DM"))
						.filter(this::getSnacksBySlots).filter(x -> this.notSlot(x, 0)).collect(Collectors.toList());
				List<JsonObject> drinks = getFilteredList(email, drinksComplete, "DRINKS", profile.getDrinks(),
						profile.getFoodType(), profile.getCommunity(), profile.getDisease(), traceId);

				// fruits
				List<JsonObject> fruitsComplete = res.stream().filter(x -> x.getString("Type").equalsIgnoreCase("F"))
						.filter(this::getSnacksBySlots).filter(x -> this.notSlot(x, 0)).collect(Collectors.toList());
				logger.debug("##### " + method + " FRUITS COMPLETE FETCH -->> " + fruitsComplete);
				List<JsonObject> fruits = getFilteredList(email, fruitsComplete, "FRUITS", profile.getFruits(),
						profile.getFoodType(), profile.getCommunity(), profile.getDisease(), traceId);

				// pulses
				List<JsonObject> plscurriesComplete = res.stream()
						.filter(x -> x.getString("Type").equalsIgnoreCase("C")).filter(this::getSnacksBySlots)
						.filter(x -> this.notSlot(x, 0)).collect(Collectors.toList());
				logger.debug("##### " + method + " PULSES COMPLETE FETCH -->> " + plscurriesComplete);
				List<JsonObject> plscurries = getFilteredList(email, plscurriesComplete, "PULSES", profile.getPules(),
						profile.getFoodType(), profile.getCommunity(), profile.getDisease(), traceId);

				// snacks
				List<JsonObject> snacksComplete = res.stream().filter(this::getSnacksBySlots)
						.filter(x -> this.notSlot(x, 0)).filter(this::getSnacks).collect(Collectors.toList());
				logger.debug("##### " + method + " SNACKS COMPLETE FETCH -->> " + snacksComplete);
				List<JsonObject> snacks = getFilteredList(email, snacksComplete, "SNACKS", profile.getSnacks(),
						profile.getFoodType(), profile.getCommunity(), profile.getDisease(), traceId);

				// dishes
				List<JsonObject> dishesComplete = res.stream().filter(x -> x.getString("Type").equalsIgnoreCase("A"))
						.filter(this::getSnacksBySlots).filter(x -> this.notSlot(x, 0)).collect(Collectors.toList());
				logger.debug("##### " + method + " DISHES COMPLETE FETCH -->> " + dishesComplete);
				List<JsonObject> dishes = getFilteredList(email, dishesComplete, "DISHES", profile.getDishes(),
						profile.getFoodType(), profile.getCommunity(), profile.getDisease(), traceId);

				// rice
				List<JsonObject> riceComplete = res.stream().filter(x -> x.getString("Type").equalsIgnoreCase("B"))
						.filter(this::getSnacksBySlots).filter(x -> this.notSlot(x, 0)).collect(Collectors.toList());
				logger.debug("##### " + method + " RICE COMPLETE FETCH -->> " + riceComplete);
				List<JsonObject> rice = getFilteredList(email, riceComplete, "RICE", profile.getRice(),
						profile.getFoodType(), profile.getCommunity(), profile.getDisease(), traceId);

				response.put("drinks", drinks);
				response.put("drinksCount", drinks.size());
				response.put("fruits", fruits);
				response.put("fruitsCount", fruits.size());
				response.put("plscurries", plscurries);
				response.put("plscurriesCount", plscurries.size());
				response.put("snacks", snacks);
				response.put("snacksCount", snacks.size());
				response.put("dishes", dishes);
				response.put("dishesCount", dishes.size());
				response.put("rice", rice);
				response.put("riceCount", rice.size());
				response.put("community", getFilteredCommunity(profile.getCommunity()));
				promise.complete(response);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, (ex) -> {
			logger.error("##### " + method + "  ERROR -->> " + ex.getMessage());
			// promise.fail(ex.getMessage());
		});

		return response;
	}

	/**
	 * Get customer dietpref update recursive.
	 * 
	 * @param email
	 * @param query
	 * @param profile
	 * @param index
	 * @param traceId
	 * @return JsonObject
	 */
	public JsonObject getCustDietPrefUpdateRecursive(String email, JsonObject query, FilterData profile, Integer index,
			String traceId) {
		String method = "MongoRepositoryWrapper getCustDietPrefUpdateRecursive() " + traceId + "-[" + email
				+ "] [EXISTING CUSTOMER] ";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject response = new JsonObject();
		client.rxFind("DIET_PLAN", query).subscribe(res -> {
			res = res.stream().map(mapper -> {
				mapper.remove("_id");
				mapper.put("isSelected", false);
				return mapper;
			}).collect(Collectors.toList());

			try {
				// drinks
				List<JsonObject> drinksComplete = res.stream().filter(x -> this.notSlot(x, Constants.ZERO))
						.filter(x -> x.getString("Type").equalsIgnoreCase("D")
								|| x.getString("Type").equalsIgnoreCase("DM"))
						.filter(this::getSnacksBySlots).filter(x -> this.notSlot(x, 0)).collect(Collectors.toList());
				List<JsonObject> drinks = getFilteredList(email, drinksComplete, "DRINKS", profile.getDrinks(),
						profile.getFoodType(), profile.getCommunity(), profile.getDisease(), traceId);

				// fruits
				List<JsonObject> fruitsComplete = res.stream().filter(x -> x.getString("Type").equalsIgnoreCase("F"))
						.filter(this::getSnacksBySlots).filter(x -> this.notSlot(x, 0)).collect(Collectors.toList());
				List<JsonObject> fruits = getFilteredList(email, fruitsComplete, "FRUITS", profile.getFruits(),
						profile.getFoodType(), profile.getCommunity(), profile.getDisease(), traceId);

				// pulses
				List<JsonObject> plscurriesComplete = res.stream()
						.filter(x -> x.getString("Type").equalsIgnoreCase("C")).filter(this::getSnacksBySlots)
						.filter(x -> this.notSlot(x, 0)).collect(Collectors.toList());
				List<JsonObject> plscurries = getFilteredList(email, plscurriesComplete, "PULSES", profile.getPules(),
						profile.getFoodType(), profile.getCommunity(), profile.getDisease(), traceId);

				// snacks
				List<JsonObject> snacksComplete = res.stream().filter(this::getSnacksBySlots)
						.filter(x -> this.notSlot(x, 0)).filter(this::getSnacks).collect(Collectors.toList());
				List<JsonObject> snacks = getFilteredList(email, snacksComplete, "SNACKS", profile.getSnacks(),
						profile.getFoodType(), profile.getCommunity(), profile.getDisease(), traceId);

				// dishes
				List<JsonObject> dishesComplete = res.stream().filter(x -> x.getString("Type").equalsIgnoreCase("A"))
						.filter(this::getSnacksBySlots).filter(x -> this.notSlot(x, 0)).collect(Collectors.toList());
				List<JsonObject> dishes = getFilteredList(email, dishesComplete, "DISHES", profile.getDishes(),
						profile.getFoodType(), profile.getCommunity(), profile.getDisease(), traceId);

				// rice
				List<JsonObject> riceComplete = res.stream().filter(x -> x.getString("Type").equalsIgnoreCase("B"))
						.filter(this::getSnacksBySlots).filter(x -> this.notSlot(x, 0)).collect(Collectors.toList());
				List<JsonObject> rice = getFilteredList(email, riceComplete, "RICE", profile.getRice(),
						profile.getFoodType(), profile.getCommunity(), profile.getDisease(), traceId);

				response.put("drinks", drinks);
				response.put("drinksCount", drinks.size());
				response.put("fruits", fruits);
				response.put("fruitsCount", fruits.size());
				response.put("plscurries", plscurries);
				response.put("plscurriesCount", plscurries.size());
				response.put("snacks", snacks);
				response.put("snacksCount", snacks.size());
				response.put("dishes", dishes);
				response.put("dishesCount", dishes.size());
				response.put("rice", rice);
				response.put("riceCount", rice.size());
				response.put("community", getFilteredCommunity(profile.getCommunity()));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, (ex) -> {
			logger.error("##### " + method + "  ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return response;
	}

	/**
	 * Get customer community.
	 * 
	 * @param dieltMaster
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getCustCommunity(JsonObject dieltMaster, String traceId) {
		String method = "MongoRepositoryWrapper getCustCommunity() " + traceId;
		Promise<JsonObject> promise = Promise.promise();

		client.rxFind("COMMUNITY_MASTER", new JsonObject()).subscribe(res -> {

			res = res.stream().map(mapper -> {
				mapper.put("code", mapper.getString("_id"));
				JsonObject response = new JsonObject();
				response.put("code", mapper.getString("_id")).put("value", mapper.getString("name"));
				response.put("isSelected", false);
				return response;
			}).collect(Collectors.toList());

			JsonObject response = new JsonObject();
			response.put("dietPref", dieltMaster);
			response.put("community", res);
			promise.complete(response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * get customer diseases.
	 * 
	 * @param jsonObject
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getCustDiseases(JsonObject jsonObject, String traceId) {
		String method = "MongoRepositoryWrapper getCalcDiseases() " + traceId;
		Promise<JsonObject> promise = Promise.promise();

		client.rxFind("DISEASE_MASTER", new JsonObject()).subscribe(res -> {
			res = res.stream().map(mapper -> {
				mapper.put("code", mapper.getString("_id"));
				JsonObject response = new JsonObject();
				response.put("code", mapper.getString("_id")).put("value", mapper.getString("name"));
				response.put("isSelected", false);
				return response;
			}).collect(Collectors.toList());

			jsonObject.put("diseases", res);
			promise.complete(jsonObject);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get customer portions.
	 * 
	 * @param jsonObject
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getCustPortions(JsonObject jsonObject, String traceId) {
		String method = "MongoRepositoryWrapper getCustPortions() " + traceId;
		Promise<JsonObject> promise = Promise.promise();

		client.rxFind("PORTION_MASTER", new JsonObject()).subscribe(res -> {

			res = res.stream().map(mapper -> {
				mapper.put("code", mapper.getString("_id"));
				JsonObject response = new JsonObject();
				response.put("code", mapper.getString("_id")).put("value", mapper.getString("name"));
				response.put("isSelected", false);
				return response;

			}).collect(Collectors.toList());

			jsonObject.put("portions", res);
			promise.complete(jsonObject);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	public Future<JsonObject> getCustDefaultProfile(JsonObject jsonObject, String traceId) {
		String method = "MongoRepositoryWrapper getCustDefaultProfile() " + traceId;
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("_id", "v1");
		client.rxFindOne("DEFAUL_PROFILE", query, null).subscribe(res -> {
			JsonObject otherMaster = res.getJsonObject("otherMaster");
			otherMaster.put("community", jsonObject.getJsonArray("community"));
			otherMaster.put("diseases", jsonObject.getJsonArray("diseases"));
			res.put("Master", jsonObject.getJsonObject("dietPref"));
			res.put("portions", jsonObject.getJsonArray("portions"));

			promise.complete(res);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Find the filtered items based on foodtype, slot, community etc
	 * 
	 * @param x
	 * @param custDietItems
	 * @param foodType
	 * @param custCommunity
	 * @param diseases
	 * 
	 * @return finalDietList
	 */
	private List<JsonObject> getFilteredList(String email, List<JsonObject> x, String typeOfDiet,
			List<String> custDietItems, String foodType, List<String> custCommunity, List<String> diseases,
			String traceId) {
		String method = "MongoRepositoryWrapper getFilteredList() " + traceId + "-[" + email + "] ";
		logger.info("##### " + method + " DIET TYPE -->> " + typeOfDiet.toUpperCase());
		List<JsonObject> selectedItemsList = new ArrayList<>();
		List<JsonObject> suggestedItemsList = new ArrayList<>();
		logger.info("##### " + method + "STARTING LIST SIZE [" + typeOfDiet.toUpperCase() + "] -->> " + x.size());
		String custType = "";

		if (null != custDietItems && custDietItems.size() > 0) {
			custType = "[EXISTING-CUSTOMER] ";
			for (JsonObject json : x)
				if (filterByCustFoodType(json, foodType))
					if (custDietItems.contains(json.getString("itemCode"))) {
						selectedItemsList.add(json);
						json.put("isSelected", true);
					} else {
						suggestedItemsList.add(json);
					}
		} else {
			custType = "[NEW-CUSTOMER] ";
			for (JsonObject json : x)
				if (filterByCustFoodType(json, foodType))
					suggestedItemsList.add(json);
		}

		List<JsonObject> finalCustDietList = new ArrayList<>();
		List<JsonObject> selectedItemsFilteredList = new ArrayList<>();
		// Selected items by Customer
		selectedItemsFilteredList = suggestedItemsList.stream().filter(x2 -> filterByCustCommunity(x2, custCommunity))
				.collect(Collectors.toList());
		logger.debug("##### " + method + custType + "FILTERED BY COMMUNITY [" + typeOfDiet.toUpperCase() + "] -->> "
				+ selectedItemsFilteredList.size());
		selectedItemsFilteredList = selectedItemsFilteredList.stream().filter(x2 -> filterAvoidIn(x2, diseases))
				.collect(Collectors.toList());
		logger.debug("##### " + method + custType + "FILTERED BY AVOIDIN [" + typeOfDiet.toUpperCase() + "] -->> "
				+ selectedItemsFilteredList.size());
		selectedItemsFilteredList = sortedFilteredList(selectedItemsFilteredList, custCommunity, custType, traceId);
		// List<JsonObject> FinalFilteredList = filter
		// selectedItemsFilteredList =
		// FoodFilterUtils.sortByScore(selectedItemsFilteredList);
		logger.debug("##### " + method + custType + "SORT BY SCORE [" + typeOfDiet.toUpperCase() + "] -->> "
				+ selectedItemsFilteredList.size());
		// selectedItemsList = FoodFilterUtils.sortByDietScore(selectedItemsList);
		if (null != selectedItemsList && selectedItemsList.size() > 0)
			finalCustDietList.addAll(selectedItemsList);
		finalCustDietList.addAll(selectedItemsFilteredList);
		logger.info("##### " + method + custType + "FINAL LIST SIZE [" + typeOfDiet.toUpperCase() + "] -->> "
				+ finalCustDietList.size());
		logger.info("");
		return finalCustDietList;
	}

	/**
	 * Find the filtered items based on foodtype, slot, community etc
	 * 
	 * @param x
	 * @param custDietItems
	 * @param foodType
	 * @param custCommunity
	 * @param diseases
	 * 
	 * @return finalDietList
	 */
	private List<JsonObject> getNewUserSelectiveList(String email, List<JsonObject> x, String typeOfDiet,
			List<String> items, List<String> nonVegItems, List<JsonObject> resp, String foodType, String traceId) {
		String method = "MongoRepositoryWrapper getNewUserSelectiveList() " + traceId + "-[" + email
				+ "] [NEW CUSTOMER] ";
		logger.debug("##### " + method + "[" + typeOfDiet
				+ "] ##############################################################");
		logger.debug("##### " + method + "[" + typeOfDiet + "] USER IS -->> [" + foodType + "] FOR DIET AS -->> ["
				+ typeOfDiet + "]");
		List<JsonObject> dietItems = new ArrayList<>();
		List<JsonObject> listItems = new ArrayList<>();
		List<JsonObject> nonVegListItems = new ArrayList<>();
		Map<String, JsonObject> respMap = new HashMap<>();
		Map<String, JsonObject> xMap = new HashMap<>();
		for (JsonObject json : resp) {
			respMap.put(json.getString("itemCode"), json);
			logger.debug("##### " + method + "[" + typeOfDiet + "] COMPLETE DIET PLAN CODE (RESP) -->> "
					+ json.getString("itemCode"));
		}

		for (JsonObject json : x)
			xMap.put(json.getString("itemCode"), json);

		List<String> duplicateChecksList = new ArrayList<>();
		for (String itemCode : items) {
			if (xMap.containsKey(itemCode))
				xMap.remove(itemCode);

			if (respMap.containsKey(itemCode)) {
				JsonObject json = respMap.get(itemCode);
				json.put("isSelected", true);
				listItems.add(json);
				duplicateChecksList.add(itemCode);
			}
		}

		if (null != nonVegItems && !nonVegItems.isEmpty() && nonVegItems.size() > 0
				&& "NV".equalsIgnoreCase(foodType)) {
			for (String itemCode : nonVegItems) {
				if (xMap.containsKey(itemCode))
					xMap.remove(itemCode);

				if (respMap.containsKey(itemCode) && !duplicateChecksList.contains(itemCode)) {
					JsonObject json = respMap.get(itemCode);
					json.put("isSelected", true);
					nonVegListItems.add(json);
				}
			}

			dietItems.addAll(nonVegListItems);
		}

		dietItems.addAll(listItems);
		// EXISTING ITEMS LIST
		List<JsonObject> list = new ArrayList<JsonObject>(xMap.values());
		dietItems.addAll(list);

		logger.info("##### " + method + "[" + typeOfDiet + "]      DIET ITEMS (SIZE) -->> " + dietItems.size());
		logger.info("##### " + method + "[" + typeOfDiet
				+ "] ##############################################################");
		return dietItems;
	}

	/**
	 * Find the selective items to be displayed on top from community
	 * 
	 * @param x
	 * @param custDietItems
	 * @param foodType
	 * @param custCommunity
	 * @param diseases
	 * 
	 * @return finalDietList
	 */
	@SuppressWarnings("unused")
	private List<JsonObject> getSelectiveItemsOnTopAndThenOthers(List<JsonObject> x, String typeOfDiet,
			List<String> custDietItems, String foodType, List<String> custCommunity, List<String> diseases,
			String traceId) {
		String method = "MongoRepositoryWrapper getSelectiveItemsOnTopAndThenOthers() " + traceId;
		logger.info("##### " + method + typeOfDiet + "-->> " + typeOfDiet);
		List<JsonObject> selectedItemsList = new ArrayList<>();
		List<JsonObject> suggestedItemsList = new ArrayList<>();
		logger.info("##### " + method + "STARTING LIST SIZE [" + typeOfDiet + "] -->> " + x.size());
		String custType = "";

		if (null != custDietItems && custDietItems.size() > 0) {
			custType = "[EXISTING-CUSTOMER] ";
			for (JsonObject json : x)
				if (filterByCustFoodType(json, foodType))
					if (custDietItems.contains(json.getString("itemCode"))) {
						selectedItemsList.add(json);
						json.put("isSelected", true);
					} else {
						suggestedItemsList.add(json);
					}
		} else {
			custType = "[NEW-CUSTOMER] ";
			for (JsonObject json : x)
				if (filterByCustFoodType(json, foodType))
					suggestedItemsList.add(json);
		}

		List<JsonObject> finalCustDietList = new ArrayList<>();
		List<JsonObject> selectedItemsFilteredList = new ArrayList<>();
		// Selected items by Customer
		selectedItemsFilteredList = suggestedItemsList.stream().filter(x2 -> filterByCustCommunity(x2, custCommunity))
				.collect(Collectors.toList());
		logger.debug("##### " + method + custType + "FILTERED BY COMMUNITY [" + typeOfDiet + "] -->> "
				+ selectedItemsFilteredList.size());
		selectedItemsFilteredList = selectedItemsFilteredList.stream().filter(x2 -> filterAvoidIn(x2, diseases))
				.collect(Collectors.toList());
		logger.debug("##### " + method + custType + "FILTERED BY AVOIDIN [" + typeOfDiet + "] -->> "
				+ selectedItemsFilteredList.size());
		selectedItemsFilteredList = sortedFilteredList(selectedItemsFilteredList, custCommunity, custType, traceId);
		// List<JsonObject> FinalFilteredList = filter
		// selectedItemsFilteredList =
		// FoodFilterUtils.sortByScore(selectedItemsFilteredList);
		logger.debug("##### " + method + custType + "SORT BY SCORE [" + typeOfDiet + "] -->> "
				+ selectedItemsFilteredList.size());
		// selectedItemsList = FoodFilterUtils.sortByDietScore(selectedItemsList);
		if (null != selectedItemsList && selectedItemsList.size() > 0)
			finalCustDietList.addAll(selectedItemsList);
		finalCustDietList.addAll(selectedItemsFilteredList);
		logger.info(
				"##### " + method + custType + "FINAL LIST SIZE [" + typeOfDiet + "] -->> " + finalCustDietList.size());
		logger.info("");
		return finalCustDietList;
	}

	/**
	 * Find the filtered items based on foodtype, slot, community etc
	 * 
	 * @param x
	 * @param custDietItems
	 * @param foodType
	 * @param custCommunity
	 * @param diseases
	 * 
	 * @return finalDietList
	 */
	@SuppressWarnings("unused")
	private List<JsonObject> getFilteredDietList(List<JsonObject> x, String typeOfDiet, List<String> custDietItems,
			String foodType, List<String> custCommunity, List<String> diseases, String traceId) {
		String method = "MongoRepositoryWrapper getFilteredDietList() " + traceId + " ";
		List<JsonObject> selectedItemsList = new ArrayList<>();
		List<JsonObject> suggestedItemsList = new ArrayList<>();
		logger.debug("##### " + method + "STARTING LIST SIZE [" + typeOfDiet + "] -->> " + x.size());
		String custType = "";

		if (null != custDietItems && custDietItems.size() > 0) {
			custType = "[EXISTING-CUSTOMER] ";
			for (JsonObject json : x)
				if (filterByCustFoodType(json, foodType))
					if (custDietItems.contains(json.getString("itemCode"))) {
						selectedItemsList.add(json);
						json.put("isSelected", true);
					} else {
						suggestedItemsList.add(json);
					}
		} else {
			custType = "[NEW-CUSTOMER] ";
			for (JsonObject json : x)
				if (filterByCustFoodType(json, foodType))
					suggestedItemsList.add(json);
		}

		List<JsonObject> finalCustDietList = new ArrayList<>();
		List<JsonObject> selectedItemsFilteredList = new ArrayList<>();
		// Selected items by Customer
		selectedItemsFilteredList = suggestedItemsList.stream().filter(x2 -> filterByCustCommunity(x2, custCommunity))
				.collect(Collectors.toList());
		logger.debug("##### " + method + custType + "FILTERED BY COMMUNITY [" + typeOfDiet + "] -->> "
				+ selectedItemsFilteredList.size());
		selectedItemsFilteredList = selectedItemsFilteredList.stream().filter(x2 -> filterAvoidIn(x2, diseases))
				.collect(Collectors.toList());
		logger.debug("##### " + method + custType + "FILTERED BY AVOIDIN [" + typeOfDiet + "] -->> "
				+ selectedItemsFilteredList.size());
		selectedItemsFilteredList = sortedFilteredList(selectedItemsFilteredList, custCommunity, custType, traceId);
		// List<JsonObject> FinalFilteredList = filter
		// selectedItemsFilteredList =
		// FoodFilterUtils.sortByScore(selectedItemsFilteredList);
		logger.debug("##### " + method + custType + "SORT BY SCORE [" + typeOfDiet + "] -->> "
				+ selectedItemsFilteredList.size());
		// selectedItemsList = FoodFilterUtils.sortByDietScore(selectedItemsList);
		if (null != selectedItemsList && selectedItemsList.size() > 0)
			finalCustDietList.addAll(selectedItemsList);
		finalCustDietList.addAll(selectedItemsFilteredList);
		logger.info(
				"##### " + method + custType + "FINAL LIST SIZE [" + typeOfDiet + "] -->> " + finalCustDietList.size());
		logger.info("");
		return finalCustDietList;
	}

	/**
	 * Find the filtered items based on foodtype, slot, community etc
	 * 
	 * @param x
	 * @param custDietItems
	 * @param foodType
	 * @param custCommunity
	 * @param diseases
	 * 
	 * @return finalDietList
	 */
	@SuppressWarnings("unused")
	private List<JsonObject> getFilteredListForNewCust(List<JsonObject> x, List<String> custCommunity) {

		List<JsonObject> selectedItemsList = new ArrayList<>();
		List<JsonObject> selectedItemsAsUList = new ArrayList<>();
		List<JsonObject> suggestedItemsList = new ArrayList<>();
		List<JsonObject> suggestedItemsLasList = new ArrayList<>();
		for (JsonObject json : x) {
			JsonArray communityArray = json.getJsonArray("Community");
			if (communityArray == null || communityArray.isEmpty()) {
				suggestedItemsLasList.add(json);
				continue;
			}

			if (communityArray.size() <= 1) {
				if (communityArray.contains("U")) {
					selectedItemsAsUList.add(json);
					continue;
				} else {
					selectedItemsList.add(json);
					continue;
				}
			} else {
				suggestedItemsList.add(json);
			}
		}

		List<JsonObject> finalCustDietList = new ArrayList<>();
		selectedItemsList = FoodFilterUtils.sortByDietScore(selectedItemsList);
		finalCustDietList.addAll(selectedItemsList);

		// Suggested items - to be displayed to customer
		suggestedItemsList = FoodFilterUtils.sortByDietScore(suggestedItemsList);
		finalCustDietList.addAll(suggestedItemsList);

		selectedItemsAsUList = FoodFilterUtils.sortByDietScore(selectedItemsAsUList);
		finalCustDietList.addAll(selectedItemsAsUList);

		suggestedItemsLasList = FoodFilterUtils.sortByDietScore(suggestedItemsLasList);
		finalCustDietList.addAll(suggestedItemsLasList);

		logger.info("##### getFilteredListForNewCust() DIETLIST SIZE -->> " + finalCustDietList.size());
		return finalCustDietList;
	}

	/**
	 * Get customer lifestyle details.
	 * 
	 * @param email
	 * @return Future<FilterData>
	 */
	public Future<FilterData> getCustomerLifeStyleProfile(String email) {
		String method = "MongoRepositoryWrapper getCustomerLifeStyleProfile()" + "-[" + email + "]";
		Set<String> allPrefood = new HashSet<String>();
		Promise<FilterData> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);

		JsonObject fileds = new JsonObject().put("lifeStyle.foodType", "lifeStyle.foodType")
				.put("lifeStyle.communities", "lifeStyle.communities").put("lifeStyle.diseases", "lifeStyle.diseases")
				.put("lifeStyle.calories", "lifeStyle.calories");
		client.rxFindOne("CUST_PROFILE", query, fileds).subscribe(res -> {
			FilterData data = new FilterData();
			data.setEmail(email);
			try {
				if (res != null) {
					if (res.getJsonObject("lifeStyle").getJsonArray("communities") != null
							&& res.getJsonObject("lifeStyle").getJsonArray("communities").size() > 0) {

						JsonArray communities = res.getJsonObject("lifeStyle").getJsonArray("communities");
						List<String> ar = new ArrayList<String>();
						communities.forEach(action -> {
							ar.add(action.toString());
						});
						data.setCommunity(ar);
					}

					if (res.getJsonObject("lifeStyle").getString("foodType") != null) {
						data.setFoodType(res.getJsonObject("lifeStyle").getString("foodType"));
					}

					logger.info("##### " + method + " lifeStyle.calories -->> "
							+ res.getJsonObject("lifeStyle").getDouble("calories"));

					if (res.getJsonObject("lifeStyle").getDouble("calories") != null) {
						data.setCalories(res.getJsonObject("lifeStyle").getDouble("calories"));
					}

					if (res.getJsonObject("lifeStyle").getJsonArray("diseases") != null
							&& res.getJsonObject("lifeStyle").getJsonArray("diseases").size() > 0) {
						JsonArray diseases = res.getJsonObject("lifeStyle").getJsonArray("diseases");
						List<String> ar = new ArrayList<String>();
						diseases.forEach(action -> {
							ar.add(action.toString());
						});
						data.setDisease(ar);
					}
				}
			} catch (Exception e) {
				logger.error("##### " + method + " EXCEPTION -->> " + e.getMessage());
			}

			data.setAllPrefood(allPrefood);
			promise.complete(data);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get customer profile.
	 * 
	 * @param email
	 * @return Future<JsonObject>
	 */
	public Future<FilterData> getCustomerProfile(String email) {
		String method = "MongoRepositoryWrapper getCustomerProfile()" + "-[" + email + "]";
		Promise<FilterData> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);
		JsonObject fileds = new JsonObject().put("lifeStyle.communities", "lifeStyle.communities");
		client.rxFindOne("CUST_PROFILE", query, fileds).subscribe(res -> {
			FilterData data = new FilterData();
			data.setEmail(email);
			try {
				if (res != null) {
					if (res.getJsonObject("lifeStyle").getJsonArray("communities") != null
							&& res.getJsonObject("lifeStyle").getJsonArray("communities").size() > 0) {

						JsonArray communities = res.getJsonObject("lifeStyle").getJsonArray("communities");
						List<String> ar = new ArrayList<String>();
						communities.forEach(action -> {
							ar.add(action.toString());
						});
						data.setCommunity(ar);
					}
				}
			} catch (Exception e) {
				logger.error("##### " + method + " EXCEPTION -->> " + e.getMessage());
			}

			promise.complete(data);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get customer Diet Preferences.
	 * 
	 * @param query
	 * @param profile
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getCustDietPrefNew(String email, JsonObject query, FilterData profile, String traceId) {
		String method = "MongoRepositoryWrapper getCustDietPrefNew() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + " COMMUNITIES -->> " + profile.getCommunity());
		Promise<JsonObject> promise = Promise.promise();
		client.rxFind("DIET_PLAN", query).subscribe(res -> {
			res = res.stream().map(mapper -> {
				mapper.remove("_id");
				mapper.put("isSelected", false);
				return mapper;
			}).collect(Collectors.toList());

			JsonObject response = new JsonObject();
			try {
				List<JsonObject> drinks = new ArrayList<>();
				List<JsonObject> fruits = new ArrayList<>();
				List<JsonObject> plscurries = new ArrayList<>();
				List<JsonObject> snacks = new ArrayList<>();
				List<JsonObject> dishes = new ArrayList<>();
				List<JsonObject> rice = new ArrayList<>();

				if ((null != profile && null != profile.getDrinks() && profile.getDrinks().size() > Constants.ZERO)
						&& (null != profile && null != profile.getFruits()
								&& profile.getFruits().size() > Constants.ZERO)
						&& (null != profile && null != profile.getPules() && profile.getPules().size() > Constants.ZERO)
						&& (null != profile && null != profile.getSnacks()
								&& profile.getSnacks().size() > Constants.ZERO)
						&& (null != profile && null != profile.getDishes()
								&& profile.getDishes().size() > Constants.ZERO)
						&& (null != profile && null != profile.getRice()
								&& profile.getRice().size() > Constants.ZERO)) {

					// LOGGED-IN CUSTOMER
					client.rxFind("DIET_PLAN", query).subscribe(res1 -> {
						res1 = res1.stream().map(mapper -> {
							mapper.remove("_id");
							mapper.put("isSelected", false);
							return mapper;
						}).collect(Collectors.toList());

						try {
							// drinks
							List<JsonObject> drinksComplete = res1.stream().filter(x -> this.notSlot(x, Constants.ZERO))
									.filter(x -> x.getString("Type").equalsIgnoreCase("D")
											|| x.getString("Type").equalsIgnoreCase("DM"))
									.filter(this::getSnacksBySlots).filter(x -> this.notSlot(x, 0))
									.collect(Collectors.toList());
							List<JsonObject> drinks1 = getFilteredList(email, drinksComplete, "DRINKS",
									profile.getDrinks(), profile.getFoodType(), profile.getCommunity(),
									profile.getDisease(), traceId);

							// fruits
							List<JsonObject> fruitsComplete = res1.stream()
									.filter(x -> x.getString("Type").equalsIgnoreCase("F"))
									.filter(this::getSnacksBySlots).filter(x -> this.notSlot(x, 0))
									.collect(Collectors.toList());
							List<JsonObject> fruits1 = getFilteredList(email, fruitsComplete, "FRUITS",
									profile.getFruits(), profile.getFoodType(), profile.getCommunity(),
									profile.getDisease(), traceId);

							// pulses
							List<JsonObject> plscurriesComplete = res1.stream()
									.filter(x -> x.getString("Type").equalsIgnoreCase("C"))
									.filter(this::getSnacksBySlots).filter(x -> this.notSlot(x, 0))
									.collect(Collectors.toList());
							List<JsonObject> plscurries1 = getFilteredList(email, plscurriesComplete, "PULSES",
									profile.getPules(), profile.getFoodType(), profile.getCommunity(),
									profile.getDisease(), traceId);

							// snacks
							List<JsonObject> snacksComplete = res1.stream().filter(this::getSnacksBySlots)
									.filter(x -> this.notSlot(x, 0)).filter(this::getSnacks)
									.collect(Collectors.toList());
							List<JsonObject> snacks1 = getFilteredList(email, snacksComplete, "SNACKS",
									profile.getSnacks(), profile.getFoodType(), profile.getCommunity(),
									profile.getDisease(), traceId);

							// dishes
							List<JsonObject> dishesComplete = res1.stream()
									.filter(x -> x.getString("Type").equalsIgnoreCase("A"))
									.filter(this::getSnacksBySlots).filter(x -> this.notSlot(x, 0))
									.collect(Collectors.toList());
							List<JsonObject> dishes1 = getFilteredList(email, dishesComplete, "DISHES",
									profile.getDishes(), profile.getFoodType(), profile.getCommunity(),
									profile.getDisease(), traceId);

							// rice
							List<JsonObject> riceComplete = res1.stream()
									.filter(x -> x.getString("Type").equalsIgnoreCase("B"))
									.filter(this::getSnacksBySlots).filter(x -> this.notSlot(x, 0))
									.collect(Collectors.toList());
							List<JsonObject> rice1 = getFilteredList(email, riceComplete, "RICE", profile.getRice(),
									profile.getFoodType(), profile.getCommunity(), profile.getDisease(), traceId);

							response.put("drinks", drinks1);
							response.put("drinksCount", drinks1.size());
							response.put("fruits", fruits1);
							response.put("fruitsCount", fruits1.size());
							response.put("plscurries", plscurries1);
							response.put("plscurriesCount", plscurries1.size());
							response.put("snacks", snacks1);
							response.put("snacksCount", snacks1.size());
							response.put("dishes", dishes1);
							response.put("dishesCount", dishes1.size());
							response.put("rice", rice1);
							response.put("riceCount", rice1.size());
							response.put("community", getFilteredCommunity(profile.getCommunity()));

							promise.complete(response);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}, (ex) -> {
						logger.error("##### " + method + "  [EXISTING CUSTOMER] ERROR -->> " + ex.getMessage());
						promise.fail(ex.getMessage());
					});
				} else if ((null == profile.getDrinks()) || (null == profile.getFruits())
						|| (null == profile.getPules()) || (null == profile.getSnacks())
						|| (null == profile.getDishes()) || (null == profile.getRice())) {
					// drinks
					List<JsonObject> drinksFiltered = res.stream().filter(x -> this.notSlot(x, Constants.ZERO))
							.filter(x -> x.getString("Type").equalsIgnoreCase("D")
									|| x.getString("Type").equalsIgnoreCase("DM"))
							.filter(x -> this.notSlot(x, 0)).collect(Collectors.toList());
					drinks = getFilteredList(email, drinksFiltered, "drinks", null, profile.getFoodType(),
							profile.getCommunity(), profile.getDisease(), traceId);
					if (null != profile && null != profile.getSelectiveItems() && null != profile.getFoodType())
						drinks = getNewUserSelectiveList(email, drinks, "DRINKS",
								profile.getSelectiveItems().getDietItems().getDrinks(), null, res,
								profile.getFoodType(), traceId);
					// fruits
					List<JsonObject> fruitsFiltered = res.stream()
							.filter(x -> x.getString("Type").equalsIgnoreCase("F")).filter(x -> this.notSlot(x, 0))
							.collect(Collectors.toList());
					fruits = getFilteredList(email, fruitsFiltered, "fruits", null, profile.getFoodType(),
							profile.getCommunity(), profile.getDisease(), traceId);
					if (null != profile && null != profile.getSelectiveItems() && null != profile.getFoodType())
						fruits = getNewUserSelectiveList(email, fruits, "FRUITS",
								profile.getSelectiveItems().getDietItems().getFruits(), null, res,
								profile.getFoodType(), traceId);
					// pulses
					List<JsonObject> plscurriesFiltered = res.stream()
							.filter(x -> x.getString("Type").equalsIgnoreCase("C")).filter(x -> this.notSlot(x, 0))
							.collect(Collectors.toList());
					plscurries = getFilteredList(email, plscurriesFiltered, "pulses", null, profile.getFoodType(),
							profile.getCommunity(), profile.getDisease(), traceId);
					if (null != profile && null != profile.getSelectiveItems() && null != profile.getFoodType())
						plscurries = getNewUserSelectiveList(email, plscurries, "PULSES&CURRIES",
								profile.getSelectiveItems().getDietItems().getPulsesOrCurries(),
								profile.getSelectiveItems().getDietItems().getPulsesOrCurriesWithNV(), res,
								profile.getFoodType(), traceId);

					// snacks
					List<JsonObject> snacksFiltered = res.stream().filter(this::getSnacksBySlots)
							.filter(this::getSnacks).filter(x -> this.notSlot(x, 0)).collect(Collectors.toList());
					snacks = getFilteredList(email, snacksFiltered, "snacks", null, profile.getFoodType(),
							profile.getCommunity(), profile.getDisease(), traceId);
					if (null != profile && null != profile.getSelectiveItems() && null != profile.getFoodType())
						snacks = getNewUserSelectiveList(email, snacks, "SNACKS",
								profile.getSelectiveItems().getDietItems().getSnacks(), null, res,
								profile.getFoodType(), traceId);

					// dishes
					List<JsonObject> dishesFiltered = res.stream()
							.filter(x -> x.getString("Type").equalsIgnoreCase("A")).filter(x -> this.notSlot(x, 0))
							.collect(Collectors.toList());
					dishes = getFilteredList(email, dishesFiltered, "dishes", null, profile.getFoodType(),
							profile.getCommunity(), profile.getDisease(), traceId);
					if (null != profile && null != profile.getSelectiveItems() && null != profile.getFoodType())
						dishes = getNewUserSelectiveList(email, dishes, "DISHES&SABJIS",
								profile.getSelectiveItems().getDietItems().getDishes(),
								profile.getSelectiveItems().getDietItems().getDishesWithNV(), res,
								profile.getFoodType(), traceId);

					// rice
					List<JsonObject> riceFiltered = res.stream().filter(x -> x.getString("Type").equalsIgnoreCase("B"))
							.filter(x -> this.notSlot(x, 0)).collect(Collectors.toList());
					rice = getFilteredList(email, riceFiltered, "rice", null, profile.getFoodType(),
							profile.getCommunity(), profile.getDisease(), traceId);
					if (null != profile && null != profile.getSelectiveItems() && null != profile.getFoodType())
						rice = getNewUserSelectiveList(email, rice, "RICE&BREADS",
								profile.getSelectiveItems().getDietItems().getRice(), null, res, profile.getFoodType(),
								traceId);

					response.put("drinks", drinks);
					response.put("drinksCount", drinks.size());
					logger.info("##### " + method + "  [NEW CUSTOMER] DRINKS SIZE (AFTER) -->> " + drinks.size());

					response.put("fruits", fruits);
					response.put("fruitsCount", fruits.size());
					logger.info("##### " + method + "  [NEW CUSTOMER] FRUITS SIZE (AFTER) -->> " + fruits.size());
					response.put("plscurries", plscurries);

					response.put("plscurriesCount", plscurries.size());
					logger.info(
							"##### " + method + "  [NEW CUSTOMER] PLSCURRIES SIZE (AFTER) -->> " + plscurries.size());

					response.put("snacks", snacks);
					response.put("snacksCount", snacks.size());
					logger.info("##### " + method + "  [NEW CUSTOMER] SNAKCS SIZE (AFTER) -->> " + snacks.size());

					response.put("dishes", dishes);
					response.put("dishesCount", dishes.size());
					logger.info("##### " + method + "  [NEW CUSTOMER] DISHES SIZE (AFTER) -->> " + dishes.size());

					response.put("rice", rice);
					response.put("riceCount", rice.size());
					logger.info("##### " + method + "  [NEW CUSTOMER] RICE SIZE (AFTER)   -->> " + rice.size());

					if (null != profile && null != profile.getCommunity()) {
						response.put("community", getFilteredCommunity(profile.getCommunity()));
						logger.info("##### " + method + " [NEW CUSTOMER] COMMUNITY            -->> "
								+ getFilteredCommunity(profile.getCommunity()));
					}

					promise.complete(response);
				}
			} catch (Exception ex) {
				logger.error("##### " + method + " EXCEPTION -->> " + ex.getMessage());
				ex.printStackTrace();
			}
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get customer dietplan timings.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<FilterData> getCustDietPlanTimingsProfile(String email, String traceId) {
		String method = "MongoRepositoryWrapper getCustDietPlanTimingsProfile() " + traceId + "-[" + email + "]";
		Promise<FilterData> promise = Promise.promise();

		try {
			Set<String> allPrefood = new HashSet<String>();
			JsonObject query = new JsonObject().put("_id", email);
			JsonObject fields = new JsonObject().put("lifeStyle.foodType", "lifeStyle.foodType")
					.put("diet.food", "diet.food").put("diet.drinks", "diet.drinks").put("diet.snacks", "diet.snacks")
					.put("diet.fruits", "diet.fruits").put("diet.dishes", "diet.dishes").put("diet.pules", "diet.pules")
					.put("diet.rice", "diet.rice").put("lifeStyle.communities", "lifeStyle.communities")
					.put("lifeStyle.wakeup", "lifeStyle.wakeup").put("lifeStyle.diseases", "lifeStyle.diseases")
					.put("lifeStyle.calories", "lifeStyle.calories")
					.put("lifeStyle.leaveForOffice", "lifeStyle.leaveForOffice")
					.put("lifeStyle.comeBack", "lifeStyle.comeBack").put("lifeStyle.sleep", "lifeStyle.sleep")
					.put("lifeStyle.sleepType", "lifeStyle.sleepType");
			logger.info("##### " + method + " QUERY  -->> " + query);
			logger.info("##### " + method + " FIELDS -->> " + fields);
			client.rxFindOne("CUST_PROFILE", query, fields).subscribe(res -> {
				FilterData data = new FilterData();
				data.setEmail(email);
				try {
					if (res != null) {
						if (res.getJsonObject("lifeStyle").getJsonArray("communities") != null
								&& res.getJsonObject("lifeStyle").getJsonArray("communities").size() > 0) {

							JsonArray communities = res.getJsonObject("lifeStyle").getJsonArray("communities");
							List<String> ar = new ArrayList<String>();
							communities.forEach(action -> {
								ar.add(action.toString());
							});
							data.setCommunity(ar);
						}

						if (res.getJsonObject("lifeStyle").getString("foodType") != null)
							data.setFoodType(res.getJsonObject("lifeStyle").getString("foodType"));
						logger.debug("##### " + method + " FOODTYPE -->> " + data.getFoodType());
						if (res.getJsonObject("lifeStyle").getJsonObject("wakeup").getString("code") != null)
							data.setWakeup(res.getJsonObject("lifeStyle").getJsonObject("wakeup").getString("code"));
						logger.debug("##### " + method + " WAKEUP   -->> " + data.getWakeup());
						if (res.getJsonObject("lifeStyle").getJsonObject("leaveForOffice").getString("code") != null)
							data.setLeaveForOffice(
									res.getJsonObject("lifeStyle").getJsonObject("leaveForOffice").getString("code"));
						logger.debug("##### " + method + " LEAVEFOROFFICE -->> " + data.getLeaveForOffice());
//						if (res.getJsonObject("lifeStyle").getJsonObject("comeBack").getString("code") != null)
//							data.setComeBack(
//									res.getJsonObject("lifeStyle").getJsonObject("comeBack").getString("code"));
//						logger.info("##### " + method + " COMEBACK -->> " + data.getComeBack());
//						if (res.getJsonObject("lifeStyle").getJsonObject("sleep").getString("code") != null)
//							data.setSleep(res.getJsonObject("lifeStyle").getJsonObject("sleep").getString("code"));
//						logger.info("##### " + method + " SLEEP -->> " + data.getSleep());

//						if (res.getJsonObject("lifeStyle").getString("sleepType") != null)
//							data.setSleepType(res.getJsonObject("lifeStyle").getString("sleepType"));
//						logger.info("##### " + method + " SLEEPTYPE -->> " + data.getSleepType());

						logger.debug("##### " + method + " lifeStyle.calories -->> "
								+ res.getJsonObject("lifeStyle").getDouble("calories"));

						if (res.getJsonObject("lifeStyle").getDouble("calories") != null)
							data.setCalories(res.getJsonObject("lifeStyle").getDouble("calories"));

						if (res.getJsonObject("lifeStyle").getJsonArray("diseases") != null
								&& res.getJsonObject("lifeStyle").getJsonArray("diseases").size() > 0) {
							JsonArray diseases = res.getJsonObject("lifeStyle").getJsonArray("diseases");
							List<String> ar = new ArrayList<String>();
							diseases.forEach(action -> {
								ar.add(action.toString());
							});
							data.setDisease(ar);
						}
						if (res.getJsonObject("diet").getJsonArray("food") != null
								&& res.getJsonObject("diet").getJsonArray("food").size() > 0) {
							JsonArray diseases = res.getJsonObject("diet").getJsonArray("food");
							List<String> ar = new ArrayList<String>();
							diseases.forEach(action -> {
								ar.add(action.toString());
							});
							data.setFoods(ar);
						}
						if (res.getJsonObject("diet").getJsonArray("drinks") != null
								&& res.getJsonObject("diet").getJsonArray("drinks").size() > 0) {
							JsonArray diseases = res.getJsonObject("diet").getJsonArray("drinks");
							List<String> ar = new ArrayList<String>();
							diseases.forEach(action -> {
								ar.add(action.toString());
								allPrefood.add(action.toString());
							});
							data.setDrinks(ar);
						}

						if (res.getJsonObject("diet").getJsonArray("snacks") != null
								&& res.getJsonObject("diet").getJsonArray("snacks").size() > 0) {
							JsonArray diseases = res.getJsonObject("diet").getJsonArray("snacks");
							List<String> ar = new ArrayList<String>();
							diseases.forEach(action -> {
								ar.add(action.toString());
								allPrefood.add(action.toString());
							});

							data.setSnacks(ar);
						}
						if (res.getJsonObject("diet").getJsonArray("fruits") != null
								&& res.getJsonObject("diet").getJsonArray("fruits").size() > 0) {
							JsonArray diseases = res.getJsonObject("diet").getJsonArray("fruits");
							List<String> ar = new ArrayList<String>();
							diseases.forEach(action -> {
								ar.add(action.toString());
								allPrefood.add(action.toString());
							});
							data.setFruits(ar);
						}

						if (res.getJsonObject("diet").getJsonArray("dishes") != null
								&& res.getJsonObject("diet").getJsonArray("dishes").size() > 0) {
							JsonArray diseases = res.getJsonObject("diet").getJsonArray("dishes");
							List<String> ar = new ArrayList<String>();
							diseases.forEach(action -> {
								ar.add(action.toString());
								allPrefood.add(action.toString());
							});
							data.setDishes(ar);
						}
						if (res.getJsonObject("diet").getJsonArray("pules") != null
								&& res.getJsonObject("diet").getJsonArray("pules").size() > 0) {
							JsonArray diseases = res.getJsonObject("diet").getJsonArray("pules");
							List<String> ar = new ArrayList<String>();
							diseases.forEach(action -> {
								ar.add(action.toString());
								allPrefood.add(action.toString());
							});
							data.setPules(ar);
						}
						if (res.getJsonObject("diet").getJsonArray("rice") != null
								&& res.getJsonObject("diet").getJsonArray("rice").size() > 0) {
							JsonArray diseases = res.getJsonObject("diet").getJsonArray("rice");
							List<String> ar = new ArrayList<String>();
							diseases.forEach(action -> {
								ar.add(action.toString());
								allPrefood.add(action.toString());
							});
							data.setRice(ar);
						}

					}
				} catch (Exception e) {
					logger.error("##### " + method + " ERROR1 -->> " + e.getMessage());
				}
				logger.debug("##### " + method + " RES -->> " + res);
				data.setAllPrefood(allPrefood);
				promise.complete(data);
				logger.info("##### " + method + " COMPLETE PROFILE DATA -->> " + data);
			}, (ex) -> {
				logger.error("##### " + method + " ERROR2 -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return promise.future();
	}

	/**
	 * Get default profile for dietplan timings.
	 * 
	 * @param profile
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<FilterData> getDefaultProfileForDietPlanTimings(FilterData profile, String traceId) {
		String method = "MongoRepositoryWrapper getDefaultProfileForDietPlanTimings() " + traceId + "-["
				+ profile.getEmail() + "]";
		Promise<FilterData> promise = Promise.promise();
		JsonObject json = profile.getTimings();
		logger.info("##### " + method + " JSONARRAY TIMINGS -->> " + json);
		if (null == json || (null != json && null == json.getJsonArray("timings"))) {
			JsonObject query = new JsonObject();
			query.put("_id", "v1");
			JsonObject response = new JsonObject();
			client.rxFindOne("DEFAUL_PROFILE", query, null).subscribe(res -> {

				JsonObject otherMaster = res.getJsonObject("otherMaster");
				JsonArray wakeupInMorningMasterDataArr = otherMaster.getJsonObject("wakeup").getJsonArray("data");
				logger.debug("##### " + method + " wakeupInMorningMasterDataArr -->> " + wakeupInMorningMasterDataArr);
				JsonArray goToOfficeFromHomeInMorningMasterDataArr = otherMaster.getJsonArray("leaveForOffice");
				logger.debug("##### " + method + " goToOfficeFromHomeInMorningMasterDataArr -->> "
						+ goToOfficeFromHomeInMorningMasterDataArr);
//			JsonArray goToSleepMasterDataArr = otherMaster.getJsonObject("sleep").getJsonArray("data");
//			logger.debug("##### " + method + " goToSleepMasterDataArr -->> " + goToSleepMasterDataArr);

				// SLOT 0
				Iterator<Object> iter = wakeupInMorningMasterDataArr.iterator();
				JsonObject category0 = new JsonObject();
				while (iter.hasNext()) {
					JsonObject wakeupTimingObject = (JsonObject) iter.next();
					String wakeupCode = wakeupTimingObject.getString("code");
					if (wakeupCode.equalsIgnoreCase(profile.getWakeup())) {
						boolean isSelected = wakeupTimingObject.getBoolean("isSelected");
						String slot0Timing = wakeupTimingObject.getString("value").trim();
						String slot0Time = "";
						if (slot0Timing.contains("Other")) {
							logger.debug("##### " + method + " slot0Time CONTAINS -->> " + slot0Timing);
							slot0Time = "06:30 AM";
						} else {
							String startTimeBeggining = slot0Timing.substring(0, slot0Timing.indexOf("-")).trim();
							String timeFormat = slot0Timing.substring(slot0Timing.length() - 2, slot0Timing.length());
							String selectedTime = startTimeBeggining + " " + timeFormat;

							try {
								// TAKE MID OF THE SELECTED TIME
								slot0Time = getActualSlotTime(selectedTime, false, 30, false);
								if (null == slot0Time || "".equalsIgnoreCase(slot0Time))
									slot0Time = "09:30 AM"; // DEFAULT TO BE CHECKED WITH PUNNET SIR
							} catch (Exception ex) {
								slot0Time = "06:30 AM"; // DEFAULT TO BE CHECKED WITH PUNNET SIR
							}
						}
						profile.setSlot0(slot0Time.replace(" AM", ""));
						profile.setSlot0Message("When you wake-up");
						response.put("slot0time", slot0Time);
						logger.debug("##### " + method + " slot0Time -->> " + slot0Time);

						category0.put("slot", 0);
						category0.put("wakupCode", wakeupCode);
						category0.put("name", "When you wake-up");
						category0.put("time", slot0Time.replace(" AM", ""));
						category0.put("isSelected", isSelected);
						break;
					}
				}

				// SLOT 2
				iter = goToOfficeFromHomeInMorningMasterDataArr.iterator();
				String actualBreakfastTime = "";
				JsonObject category2 = new JsonObject();
				while (iter.hasNext()) {
					JsonObject leaveForOfficeObject = (JsonObject) iter.next();
					String leaveForOfficeCode = leaveForOfficeObject.getString("code");
					logger.debug("##### " + method + " leaveForOfficeCode slot2 -->> " + leaveForOfficeCode);
					logger.debug("##### " + method + " profile.getLeaveForOffice() slot2 -->> "
							+ profile.getLeaveForOffice());
					if (null == profile.getLeaveForOffice() || "".equalsIgnoreCase(profile.getLeaveForOffice())) {
						actualBreakfastTime = "09:30 AM";
						profile.setSlot2(actualBreakfastTime);
					} else {
						if (leaveForOfficeCode.equalsIgnoreCase(profile.getLeaveForOffice())) {
							boolean isSelected = leaveForOfficeObject.getBoolean("isSelected");
							String slot2Timing = leaveForOfficeObject.getString("value").trim();

							if (null != slot2Timing && slot2Timing.contains("Other")) {
								logger.debug("##### " + method + " slot8Time CONTAINS -->> " + slot2Timing);
								actualBreakfastTime = "09:30 AM";
							} else {
								String startTimeBeggining = slot2Timing.substring(0, slot2Timing.indexOf("-")).trim();
								String timeFormat = slot2Timing.substring(slot2Timing.length() - 2,
										slot2Timing.length());
								String selectedTime = startTimeBeggining + " " + timeFormat;
								// TAKE MID OF THE SELECTED TIME
								selectedTime = getActualSlotTime(selectedTime, false, 30, false);
								logger.debug("##### " + method + " selectedTime - SLOT 2 -->> " + selectedTime);

								try {
									actualBreakfastTime = getActualSlotTime(selectedTime, true, 0, false);
									if (null == actualBreakfastTime || "".equalsIgnoreCase(actualBreakfastTime))
										actualBreakfastTime = "09:30 AM"; // DEFAULT TO BE CHECKED WITH PUNNET SIR
								} catch (Exception ex) {
									actualBreakfastTime = "09:30 AM"; // DEFAULT TO BE CHECKED WITH PUNNET SIR
								}
							}

							profile.setSlot2(actualBreakfastTime);
							profile.setSlot2Message("BreakFast");
							response.put("slot2time", actualBreakfastTime);
							logger.debug(
									"##### " + method + " actualBreakfastTime - SLOT 2 -->> " + actualBreakfastTime);

							category2.put("slot", 2);
							category2.put("wakupCode", leaveForOfficeCode);
							category2.put("name", "BreakFast");
							category2.put("time", actualBreakfastTime.replace(" AM", ""));
							category2.put("isSelected", isSelected);
							break;
						} else {
							actualBreakfastTime = "09:30 AM";
						}
					}
				}

				// SLOT 1 (ie. SOLT 2 - 30 MINS)
				JsonObject category1 = new JsonObject();
				String slot1Timing = "";
				logger.debug("##### " + method + " actualBreakfastTime SLOT 1 -->> " + actualBreakfastTime);
				try {
					slot1Timing = getActualSlotTime(actualBreakfastTime, false, -30, false);
					if (null == slot1Timing || "".equalsIgnoreCase(slot1Timing))
						slot1Timing = "09:30 AM"; // DEFAULT TO BE CHECKED WITH PUNNET SIR
				} catch (Exception ex) {
					slot1Timing = "09:30 AM"; // DEFAULT TO BE CHECKED WITH PUNNET SIR
				}

				profile.setSlot1(slot1Timing.replace(" AM", ""));
				profile.setSlot1Message("30 Minutes before Breakfast");
				response.put("slot1time", slot1Timing);
				logger.debug("##### " + method + " slot1Timing -->> " + slot1Timing);
				category1.put("slot", 1);
				category1.put("wakupCode", "");
				category1.put("name", "30 Minutes before breakfast");
				category1.put("time", slot1Timing.replace(" AM", ""));
				category1.put("isSelected", true);

				// SLOT 3
				String slot3Time = "";
				try {
					if ("9:30 AM".equalsIgnoreCase(actualBreakfastTime)
							|| "9:30".equalsIgnoreCase(actualBreakfastTime)) {
						slot3Time = "11:00 AM";
					} else if ("10:00 AM".equalsIgnoreCase(actualBreakfastTime)
							|| "10:00".equalsIgnoreCase(actualBreakfastTime)) {
						slot3Time = "11:30 AM";
					} else if ("10:30 AM".equalsIgnoreCase(actualBreakfastTime)
							|| "10:30".equalsIgnoreCase(actualBreakfastTime)) {
						slot3Time = "12:00 AM";
					} else if ("11:00 AM".equalsIgnoreCase(actualBreakfastTime)
							|| "11:00".equalsIgnoreCase(actualBreakfastTime)) {
						slot3Time = "12:30 AM";
					}
				} catch (Exception ex) {
					slot3Time = "11:00 AM"; // DEFAULT TO BE CHECKED WITH PUNNET SIR
				}
				if (null == slot1Timing || "".equalsIgnoreCase(slot3Time))
					slot3Time = "11:00 AM"; // DEFAULT TO BE CHECKED WITH PUNNET SIR
				profile.setSlot3(slot3Time.replace(" AM", "").replace(" PM", ""));
				profile.setSlot3Message("Morning Snack");
				logger.debug("##### slot3Time -->> " + slot3Time);
				response.put("slot3Time", slot3Time);
				JsonObject category3 = new JsonObject();
				category3.put("slot", 3);
				category3.put("name", "Morning Snack");
				category3.put("time", slot3Time.replace(" AM", ""));
				category3.put("isSelected", true);

				// SLOT 4
				String slot4Time = "";
				try {
					if ("11:00 AM".equalsIgnoreCase(slot3Time) || "11:30 AM".equalsIgnoreCase(slot3Time)
							|| "11:00".equalsIgnoreCase(slot3Time) || "11:30".equalsIgnoreCase(slot3Time)
							|| "11:00 AM".equalsIgnoreCase(slot3Time) || "11:30 AM".equalsIgnoreCase(slot3Time))
						slot4Time = "13:30 PM";
					else if ("12:00 PM".equalsIgnoreCase(slot3Time) || "12:30 PM".equalsIgnoreCase(slot3Time)
							|| "12:00 AM".equalsIgnoreCase(slot3Time) || "12:30 AM".equalsIgnoreCase(slot3Time)
							|| "12:00".equalsIgnoreCase(slot3Time) || "12:30".equalsIgnoreCase(slot3Time))
						slot4Time = "14:00 PM";
					if (null == slot4Time || "".equalsIgnoreCase(slot4Time))
						slot4Time = "13:30 PM"; // DEFAULT TO BE CHECKED WITH PUNNET SIR
				} catch (Exception ex) {
					slot4Time = "13:30 PM";
				}

				if (null == slot4Time || "".equalsIgnoreCase(slot4Time))
					slot4Time = "13:30 PM"; // DEFAULT TO BE CHECKED WITH PUNNET SIR
				profile.setSlot4(slot4Time);
				profile.setSlot4Message("Lunch");
				logger.debug("##### " + method + " slot4Time -->> " + slot4Time);
				response.put("slot4Time", slot4Time);
				JsonObject category4 = new JsonObject();
				category4.put("slot", 4);
				category4.put("wakupCode", "");
				category4.put("name", "Lunch");
				category4.put("time", slot4Time);
				category4.put("isSelected", true);

				// SLOT 5
				JsonObject category5 = new JsonObject();
				String slot5Time = "";
				try {
					if (slot4Time.equalsIgnoreCase("13:30 PM") || slot4Time.equalsIgnoreCase("13:30")) {
						slot5Time = "15:00 PM";
					} else {
						slot5Time = "15:30 PM";
					}

					if (null == slot5Time || "".equalsIgnoreCase(slot5Time))
						slot5Time = "15:00 PM"; // DEFAULT TO BE CHECKED WITH PUNNET SIR
				} catch (Exception ex) {
					slot5Time = "15:00 PM"; // DEFAULT TO BE CHECKED WITH PUNNET SIR
				}
				profile.setSlot5(slot5Time);
				profile.setSlot5Message("Post Lunch");
				response.put("slot5Time", slot5Time);
				logger.debug("##### " + method + " slot5Time -->> " + slot5Time);
				category5.put("slot", 5);
				category5.put("wakupCode", "NA");
				category5.put("name", "Post Lunch");
				category5.put("time", slot5Time);
				category5.put("isSelected", true);

				// SLOT 6
				JsonObject category6 = new JsonObject();
				String slot6Time = "";
				try {
					if (slot5Time.equalsIgnoreCase("15:00 PM") || slot5Time.equalsIgnoreCase("15:00")) {
						slot6Time = "17:00 PM";
					} else {
						slot6Time = "17:30 PM";
					}
					if (null == slot6Time || "".equalsIgnoreCase(slot6Time))
						slot6Time = "05:00 PM"; // DEFAULT TO BE CHECKED WITH PUNNET SIR
				} catch (Exception ex) {
					slot6Time = "05:00 PM"; // DEFAULT TO BE CHECKED WITH PUNNET SIR
				}
				profile.setSlot6(slot6Time);
				response.put("slot6Time", slot6Time);
				profile.setSlot6Message("Evening Snack");
				logger.debug("##### " + method + " slot6Time -->> " + slot6Time);
				category6.put("slot", 6);
				category6.put("wakupCode", "NA");
				category6.put("name", "Evening Snack");
				category6.put("time", slot6Time);
				category6.put("isSelected", true);

				// SLOT 7
				JsonObject category7 = new JsonObject();
				String slot7Time = "19:00 PM";
				profile.setSlot7(slot7Time);
				profile.setSlot7Message("Dinner");
				response.put("slot7Time", slot7Time);
				logger.debug("##### " + method + " slot7Time -->> " + slot7Time);
				category7.put("slot", 7);
				category7.put("wakupCode", "NA");
				category7.put("name", "Dinner");
				category7.put("time", slot7Time);
				category7.put("isSelected", true);

				// SLOT 8
				JsonObject category8 = new JsonObject();
				String slot8Time = "20:00 PM";
				profile.setSlot8(slot8Time);
				profile.setSlot8Message("Night time");
				response.put("slot8Time", slot8Time);
				logger.debug("##### " + method + " slot8Time -->> " + slot8Time);
				category8.put("slot", 8);
				category8.put("wakupCode", "NA");
				category8.put("name", "Night time");
				category8.put("time", slot8Time);
				category8.put("isSelected", true);

				// SLOT 8
//			iter = goToSleepMasterDataArr.iterator();
//			JsonObject category8 = new JsonObject();
//			String actualSleepTime = "";
//			while (iter.hasNext()) {
//				JsonObject sleepObject = (JsonObject) iter.next();
//				String sleepCode = sleepObject.getString("code");
//				if (null == profile.getSleep() || "".equalsIgnoreCase(profile.getSleep())) {
//					actualBreakfastTime = "20:00";
//					profile.setSlot8(getActualSlotTimeUpdated(actualBreakfastTime, 8));
//				} else {
//					if (sleepCode.equalsIgnoreCase(profile.getSleep())) {
//						logger.info("");
//						boolean isSelected = sleepObject.getBoolean("isSelected");
//						String slot8Timing = sleepObject.getString("value").trim();
//
//						if (slot8Timing.contains("Other")) {
//							logger.info("##### " + method + " slot8Time CONTAINS -->> " + slot8Timing);
//							actualSleepTime = "20:00";
//							profile.setSlot8(actualSleepTime);
//							profile.setSlot8Message("At Bed-time");
//							logger.info("##### " + method + " slot8Time -->> " + actualSleepTime);
//							response.put("slot8Time", actualSleepTime);
//
//							category8.put("slot", 8);
//							category8.put("wakupCode", sleepCode);
//							category8.put("name", "At Bed-time");
//							category8.put("time", getActualSlotTimeUpdated(actualSleepTime, 8));
//							category8.put("isSelected", isSelected);
//						} else {
//							String startTimeBeggining = slot8Timing.substring(0, slot8Timing.indexOf("-")).trim();
//							String timeFormat = slot8Timing.substring(slot8Timing.length() - 2, slot8Timing.length());
//							String selectedTime = startTimeBeggining + " " + timeFormat;
//							try {
//								// TAKE MID OF THE SELECTED TIME
//								selectedTime = getActualSlotTime(selectedTime, false, 30, false);
//								// selectedTime = getActualSlotTime(selectedTime, false, -60, false);
//								logger.info("##### " + method + " selectedTime -->> " + selectedTime);
//								actualSleepTime = getActualSlotTime(selectedTime, true, 0, true);
//								if (actualSleepTime.equalsIgnoreCase("11:00 PM")
//										|| actualSleepTime.equalsIgnoreCase("11:30 PM")
//										|| actualSleepTime.equalsIgnoreCase("12:00 PM")
//										|| actualSleepTime.equalsIgnoreCase("12:30 PM")
//										|| actualSleepTime.equalsIgnoreCase("8:00 PM")
//										|| actualSleepTime.equalsIgnoreCase("08:00 PM")
//										|| actualSleepTime.equalsIgnoreCase("08:30 PM")
//										|| actualSleepTime.equalsIgnoreCase("8:30 PM")
//										|| actualSleepTime.equalsIgnoreCase("9:00 PM")
//										|| actualSleepTime.equalsIgnoreCase("09:00 PM")
//										|| actualSleepTime.equalsIgnoreCase("9:30 PM")
//										|| actualSleepTime.equalsIgnoreCase("09:30 PM")
//										|| actualSleepTime.equalsIgnoreCase("10:00 PM")
//										|| actualSleepTime.equalsIgnoreCase("10:30 PM")) {
//									actualSleepTime = "20:00 PM";
//								}
////								logger.info("##### " + method + " SLOT 8 ACTUAL SLEEP TIME -->> " + actualSleepTime);
//								if (null == actualSleepTime || "".equalsIgnoreCase(actualSleepTime))
//									actualSleepTime = "20:00 PM"; // DEFAULT TO BE CHECKED WITH PUNNET SIR
//
//								profile.setSlot8(actualSleepTime.replace(" PM", ""));
//								profile.setSlot8Message("At Bed-time");
//								logger.info("##### " + method + " slot8Time -->> " + actualSleepTime);
//								response.put("slot8Time", actualSleepTime);
//
//								category8.put("slot", 8);
//								category8.put("wakupCode", sleepCode);
//								category8.put("name", "At Bed-time");
//								category8.put("time", actualSleepTime);
//								category8.put("isSelected", isSelected);
//								break;
//
//							} catch (Exception ex) {
//								actualSleepTime = "21:00"; // DEFAULT TO BE CHECKED WITH PUNNET SIR
//							}
//						}
//					}
//				}
//
//			}
				// promise.complete(profile);
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		} else {
			List<String> timings = new ArrayList<>();
			JsonArray jsonArr = json.getJsonArray("timings");
			logger.info("##### " + method + " TIMINGS -->> " + jsonArr);
			jsonArr.forEach(action -> {
				JsonObject obj = (JsonObject) action;
				Integer slot = obj.getInteger("slot");
				String time = obj.getString("time");

				timings.add(slot, time);
			});

			logger.info("##### " + method + " CUSTOMER DIETPLAN TIMINGS -->> " + timings);

			profile.setSlot0(timings.get(0));
			profile.setSlot0Message("When you wake-up");

			profile.setSlot1(timings.get(1));
			profile.setSlot1Message("30 Minutes before Breakfast");

			profile.setSlot2(timings.get(2));
			profile.setSlot2Message("BreakFast");

			profile.setSlot3(timings.get(3));
			profile.setSlot3Message("Morning Snack");

			profile.setSlot4(timings.get(4));
			profile.setSlot4Message("Lunch");

			profile.setSlot5(timings.get(5));
			profile.setSlot5Message("Post Lunch");

			profile.setSlot6(timings.get(6));
			profile.setSlot6Message("Evening Snack");

			profile.setSlot7(timings.get(7));
			profile.setSlot7Message("Dinner");

			profile.setSlot8(timings.get(8));
			profile.setSlot8Message("Night time");
		}

		promise.complete(profile);
		return promise.future();
	}

	/**
	 * Get default profile for dietplan timings.
	 * 
	 * @param jsonObject
	 * @param profile
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getDefaultProfileForDietPlanTimings1(JsonObject jsonObject, FilterData profile,
			String traceId) {
		String method = "MongoRepositoryWrapper getDefaultProfileForDietPlanTimings1() " + traceId;
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject();
		List<JsonObject> categories = new ArrayList<JsonObject>();
		JsonObject slotTimingsOptions = new JsonObject();
		slotTimingsOptions.put("optionId", 1);
		slotTimingsOptions.put("optionName", "Slot wise Timings");
		query.put("_id", "v1");
		client.rxFindOne("DEFAUL_PROFILE", query, null).subscribe(res -> {
			JsonObject otherMaster = res.getJsonObject("otherMaster");
			JsonArray wakeupInMorningMasterDataArr = otherMaster.getJsonObject("wakeup").getJsonArray("data");
			logger.debug("##### " + method + " wakeupInMorningMasterDataArr -->> " + wakeupInMorningMasterDataArr);
			JsonArray goToOfficeFromHomeInMorningMasterDataArr = otherMaster.getJsonArray("leaveForOffice");
			logger.debug("##### " + method + " goToOfficeFromHomeInMorningMasterDataArr -->> "
					+ goToOfficeFromHomeInMorningMasterDataArr);
			JsonArray goToSleepMasterDataArr = otherMaster.getJsonObject("sleep").getJsonArray("data");
			logger.debug("##### " + method + " goToSleepMasterDataArr -->> " + goToSleepMasterDataArr);

			// SLOT 0
			Iterator<Object> iter = wakeupInMorningMasterDataArr.iterator();
			JsonObject category0 = new JsonObject();
			while (iter.hasNext()) {
				JsonObject wakeupTimingObject = (JsonObject) iter.next();
				String wakeupCode = wakeupTimingObject.getString("code");
				if (wakeupCode.equalsIgnoreCase(profile.getWakeup())) {
					boolean isSelected = wakeupTimingObject.getBoolean("isSelected");
					String slot0Timing = wakeupTimingObject.getString("value").trim();
					String startTimeBeggining = slot0Timing.substring(0, slot0Timing.indexOf("-")).trim();
					String timeFormat = slot0Timing.substring(slot0Timing.length() - 2, slot0Timing.length());
					String selectedTime = startTimeBeggining + " " + timeFormat;
					String slot0Time = getActualSlotTime(selectedTime, false, 30, false);
					logger.info("##### " + method + " slot0Time -->> " + slot0Time);

					category0.put("slot", 0);
					category0.put("wakupCode", wakeupCode);
					category0.put("name", "When you wake-up");
					category0.put("time", slot0Time.replace(" AM", ""));
					category0.put("isSelected", isSelected);
					break;
				}
			}

			// SLOT 2
			iter = goToOfficeFromHomeInMorningMasterDataArr.iterator();
			String actualBreakfastTime = "";
			JsonObject category2 = new JsonObject();
			while (iter.hasNext()) {
				JsonObject leaveForOfficeObject = (JsonObject) iter.next();
				String leaveForOfficeCode = leaveForOfficeObject.getString("code");
				if (leaveForOfficeCode.equalsIgnoreCase(profile.getLeaveForOffice())) {
					boolean isSelected = leaveForOfficeObject.getBoolean("isSelected");
					String slot2Timing = leaveForOfficeObject.getString("value").trim();
					String startTimeBeggining = slot2Timing.substring(0, slot2Timing.indexOf("-")).trim();
					String timeFormat = slot2Timing.substring(slot2Timing.length() - 2, slot2Timing.length());
					String selectedTime = startTimeBeggining + " " + timeFormat;
					actualBreakfastTime = getActualSlotTime(selectedTime, true, 0, false);
					logger.debug("##### " + method + " actualBreakfastTime -->> " + actualBreakfastTime);
					logger.debug("##### " + method + " slot2Time           -->> " + actualBreakfastTime);

					category2.put("slot", 2);
					category2.put("wakupCode", leaveForOfficeCode);
					category2.put("name", "BreakFast");
					category2.put("time", actualBreakfastTime.replace(" AM", ""));
					category2.put("isSelected", isSelected);
					break;
				}
			}

			// SLOT 1 (ie. SOLT 2 - 30 MINS)
			JsonObject category1 = new JsonObject();
			String slot1Timing = getActualSlotTime(actualBreakfastTime, false, -30, false);
			logger.info("##### " + method + " slot1Time -->> " + slot1Timing);
			category1.put("slot", 1);
			category1.put("wakupCode", "");
			category1.put("name", "30 Minutes before breakfast");
			category1.put("time", slot1Timing.replace(" AM", ""));
			category1.put("isSelected", true);

			logger.info("##### " + method + " slot2Time -->> " + actualBreakfastTime);

			// SLOT 3
			String slot3Time = "";
			if ("9:30 AM".equalsIgnoreCase(actualBreakfastTime) || "10:00 AM".equalsIgnoreCase(actualBreakfastTime))
				slot3Time = getActualSlotTime(actualBreakfastTime, false, 120, false);
			else if ("10:30 AM".equalsIgnoreCase(actualBreakfastTime)
					|| "11:00 AM".equalsIgnoreCase(actualBreakfastTime))
				slot3Time = getActualSlotTime(actualBreakfastTime, false, 90, false);
			logger.info("##### " + method + " slot3Time -->> " + slot3Time);

			JsonObject category3 = new JsonObject();
			category3.put("slot", 3);
			category3.put("wakupCode", "");
			category3.put("name", "Morning Snack");
			category3.put("time", slot3Time.replace(" AM", ""));
			category3.put("isSelected", true);

			// SLOT 4
			String slot4Time = "";
			if ("11:00 AM".equalsIgnoreCase(slot3Time))
				slot4Time = getActualSlotTime(slot3Time, false, 150, false);
			else if ("11:30 AM".equalsIgnoreCase(slot3Time) || "12:00 PM".equalsIgnoreCase(slot3Time))
				slot4Time = getActualSlotTime(slot3Time, false, 120, false);
			else if ("12:30 PM".equalsIgnoreCase(slot3Time))
				slot4Time = getActualSlotTime(slot3Time, false, 90, false);
			logger.info("##### " + method + " slot4Time -->> " + slot4Time);
			JsonObject category4 = new JsonObject();
			category4.put("slot", 4);
			category4.put("wakupCode", "");
			category4.put("name", "Lunch");
			category4.put("time", getActualSlotTimeUpdated(slot4Time, 4));
			category4.put("isSelected", true);

			// SLOT 5
			JsonObject category5 = new JsonObject();
			String slot5Time = getActualSlotTime(slot4Time, false, 90, false);
			logger.info("##### " + method + " slot5Time -->> " + slot5Time);
			category5.put("slot", 5);
			category5.put("wakupCode", "NA");
			category5.put("name", "Post Lunch");
			category5.put("time", getActualSlotTimeUpdated(slot5Time, 5));
			category5.put("isSelected", true);

			// SLOT 6
			JsonObject category6 = new JsonObject();
			String slot6Time = getActualSlotTime(slot5Time, false, 120, false);
			logger.info("##### " + method + " slot6Time -->> " + slot6Time);
			category6.put("slot", 6);
			category6.put("wakupCode", "NA");
			category6.put("name", "Evening Snack");
			category6.put("time", getActualSlotTimeUpdated(slot6Time, 6));
			category6.put("isSelected", true);

			// SLOT 7
			JsonObject category7 = new JsonObject();
			String slot7Time = "7:00 PM";
			logger.info("##### " + method + " slot7Time -->> " + slot7Time);
			category7.put("slot", 7);
			category7.put("wakupCode", "NA");
			category7.put("name", "Dinner");
			category7.put("time", getActualSlotTimeUpdated(slot7Time, 7));
			category7.put("isSelected", true);

			// SLOT 8
			iter = goToSleepMasterDataArr.iterator();
			JsonObject category8 = new JsonObject();
			String actualSleepTime = "";
			while (iter.hasNext()) {
				JsonObject sleepObject = (JsonObject) iter.next();
				String sleepCode = sleepObject.getString("code");
				if (sleepCode.equalsIgnoreCase(profile.getSleep())) {
					logger.debug("");
					boolean isSelected = sleepObject.getBoolean("isSelected");
					String slot8Timing = sleepObject.getString("value").trim();
					String startTimeBeggining = slot8Timing.substring(0, slot8Timing.indexOf("-")).trim();
					String timeFormat = slot8Timing.substring(slot8Timing.length() - 2, slot8Timing.length());
					String selectedTime = startTimeBeggining + " " + timeFormat;
					actualSleepTime = getActualSlotTime(selectedTime, true, 0, true);
					logger.info("##### " + method + " slot8Time -->> " + actualSleepTime);

					category8.put("slot", 8);
					category8.put("wakupCode", sleepCode);
					category8.put("name", "At Bed-time");
					category8.put("time", getActualSlotTimeUpdated(actualSleepTime, 8));
					category8.put("isSelected", isSelected);
					break;
				}
			}

			categories.add(category0);
			categories.add(category1);
			categories.add(category2);
			categories.add(category3);
			categories.add(category4);
			categories.add(category5);
			categories.add(category6);
			categories.add(category7);
			categories.add(category8);
			slotTimingsOptions.put("slots", categories);

			JsonObject slotTimings = new JsonObject();
			slotTimings.put("slotTimings", slotTimingsOptions);
			promise.complete(slotTimings);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Get actual slot time.
	 * 
	 * @param actualTime
	 * @param slot
	 * @return String
	 */
	private String getActualSlotTimeUpdated(String actualTime, int slot) {
		logger.debug("##### getActualSlotTimeUpdated() SLOT [" + slot + " --- ACTUAL TIME -->> " + actualTime);
		if (actualTime.contains("PM")) {
			actualTime = actualTime.replace("AM", "PM");
			if (actualTime.startsWith("01") || actualTime.startsWith("1")) {
				if (actualTime.startsWith("01"))
					actualTime = actualTime.replace("01", "13");
				else
					actualTime = actualTime.replace("1", "13");
			} else if (actualTime.startsWith("02") || actualTime.startsWith("2")) {
				if (actualTime.startsWith("02"))
					actualTime = actualTime.replace("02", "14");
				else
					actualTime = actualTime.replace("2", "14");
			} else if (actualTime.startsWith("03") || actualTime.startsWith("3")) {
				if (actualTime.startsWith("03"))
					actualTime = actualTime.replace("03", "15");
				else
					actualTime = actualTime.replace("3", "15");
			} else if (actualTime.startsWith("04") || actualTime.startsWith("4")) {
				if (actualTime.startsWith("04"))
					actualTime = actualTime.replace("04", "16");
				else
					actualTime = actualTime.replace("4", "16");
			} else if (actualTime.startsWith("05") || actualTime.startsWith("5")) {
				if (actualTime.startsWith("05"))
					actualTime = actualTime.replace("05", "17");
				else
					actualTime = actualTime.replace("5", "17");
			} else if (actualTime.startsWith("06") || actualTime.startsWith("6")) {
				if (actualTime.startsWith("06"))
					actualTime = actualTime.replace("06", "18");
				else
					actualTime = actualTime.replace("6", "18");
			} else if (actualTime.startsWith("07") || actualTime.startsWith("7")) {
				if (actualTime.startsWith("07"))
					actualTime = actualTime.replace("07", "19");
				else
					actualTime = actualTime.replace("7", "19");
			} else if (actualTime.startsWith("08") || actualTime.startsWith("8")) {
				if (actualTime.startsWith("08"))
					actualTime = actualTime.replace("08", "20");
				else
					actualTime = actualTime.replace("8", "20");
			} else if (actualTime.startsWith("09") || actualTime.startsWith("9")) {
				if (actualTime.startsWith("09"))
					actualTime = actualTime.replace("09", "21");
				else
					actualTime = actualTime.replace("9", "21");
			} else if (actualTime.startsWith("10")) {
				actualTime = actualTime.replace("10", "22");
			} else if (actualTime.startsWith("11")) {
				actualTime = actualTime.replace("11", "23");
			} else if (actualTime.startsWith("12")) {
				actualTime = actualTime.replace("12", "00");
			}

			if (actualTime.contains("8:00") || actualTime.contains("08:00")) {
				actualTime = "20:00";
			} else if (actualTime.contains("8:30") || actualTime.contains("08:30")) {
				actualTime = "20:30";
			} else if (actualTime.contains("9:00") || actualTime.contains("09:00")) {
				actualTime = "21:00";
			} else if (actualTime.contains("9:30") || actualTime.contains("09:30")) {
				actualTime = "21:30";
			} else if (actualTime.contains("10:00")) {
				actualTime = "22:00";
			} else if (actualTime.contains("10:30")) {
				actualTime = "22:30";
			} else if (actualTime.contains("11:00")) {
				actualTime = "23:00";
			} else if (actualTime.contains("11:30")) {
				actualTime = "23:30";
			} else if (actualTime.contains("01:00") || actualTime.contains("1:00")) {
				actualTime = "13:00";
			} else if (actualTime.contains("01:30") || actualTime.contains("1:30")) {
				actualTime = "13:30";
			} else if (actualTime.contains("02:00") || actualTime.contains("02:00")) {
				actualTime = "14:00";
			} else if (actualTime.contains("02:30") || actualTime.contains("2:30")) {
				actualTime = "14:30";
			} else if (actualTime.contains("03:00") || actualTime.contains("3:00")) {
				actualTime = "15:00";
			} else if (actualTime.contains("03:30") || actualTime.contains("3:30")) {
				actualTime = "15:30";
			} else if (actualTime.contains("04:00") || actualTime.contains("4:00")) {
				actualTime = "16:00";
			} else if (actualTime.contains("04:30") || actualTime.contains("4:30")) {
				actualTime = "16:30";
			} else if (actualTime.contains("05:00") || actualTime.contains("5:00")) {
				actualTime = "17:00";
			} else if (actualTime.contains("05:30") || actualTime.contains("5:30")) {
				actualTime = "17:30";
			} else if (actualTime.contains("06:00") || actualTime.contains("6:00")) {
				actualTime = "18:00";
			} else if (actualTime.contains("06:30") || actualTime.contains("6:30")) {
				actualTime = "18:30";
			} else if (actualTime.contains("07:00") || actualTime.contains("7:00")) {
				actualTime = "19:00";
			} else if (actualTime.contains("07:30") || actualTime.contains("7:30")) {
				actualTime = "19:30";
			} else if (actualTime.contains("12:30")) {
				actualTime = "12:30";
			}

			logger.debug("##### getActualSlotTimeUpdated()    SELECTED (PM) -->> " + actualTime);
			logger.debug("##### getActualSlotTimeUpdated() ACTUAL TIME (PM) -->> " + actualTime);
		} else {
			actualTime = actualTime.replace("AM", "");
			logger.debug("##### getActualSlotTimeUpdated() ACTUAL TIME (AM) -->> " + actualTime);
		}
		return actualTime;
	}

	/**
	 * Check if email is allowed to the customer today.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<Boolean>
	 */
	public Future<JsonObject> isEmailSendAllowed(String email, JsonObject request, String traceId) {
		String method = "MongoRepositoryWrapper isEmailSendAllowed() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		logger.info("##### " + method + " REQUEST -->> " + request);
		request.put("isEmailSendAllowed", true);
		JsonObject query = new JsonObject();
		query.put("_id", new JsonObject().put("email", email).put("date", ApiUtils.getCurrentDateInddMMMyyyyFormat(0)));
		logger.info("##### " + method + " QUERY -->> " + query);
		String category = request.getString("title");
		client.rxFindOne("CUST_MAIL_SENT_STATUS", query, null).subscribe(res -> {
			List<Boolean> list = new ArrayList<>();
			JsonArray categories = new JsonArray();
			if (res != null && !res.isEmpty()) {
				request.put("isEmailSendingRequestNew", false);
				JsonArray jsonArr = res.getJsonArray("categories");
				jsonArr.forEach(action -> {
					JsonObject json = (JsonObject) action;
					if (category.equalsIgnoreCase(json.getString("category"))) {
						if (json.getInteger("count") == this.config.getInteger("emailsAllowed")) {
							request.put("isEmailSendAllowed", false);
							list.add(false);
						} else {
							json.put("count", json.getInteger("count") + 1);
							list.add(true);
						}
					}

					categories.add(json);
				});

				if (null != list && list.size() <= 0) {
					JsonObject json = new JsonObject();
					json.put("category", category);
					json.put("count", 1);
					categories.add(json);
				}
			} else {
				request.put("isEmailSendingRequestNew", true);
				JsonObject json = new JsonObject();
				json.put("category", category);
				json.put("count", 1);
				categories.add(json);
			}

			request.put("categories", categories);

			if (null != list && list.size() > 0 && (list.get(0) == false))
				promise.fail("Email is not allowed to be sent - [" + category + "]");
			else
				promise.complete(request);

			logger.info("##### " + method + " REQUEST -->> " + request);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Send email foe customer concerns/help via gmail smtp.
	 * 
	 * @param email
	 * @param payload
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> sendEmail(String email, JsonObject request, String traceId) {
		String method = "MongoRepositoryWrapper sendEmail() " + traceId;
		Promise<JsonObject> promise = Promise.promise();
		Properties properties = new Properties();
		JsonObject jsonObject = new JsonObject();
		final String username = "admin@fightitaway.com";
		final String password = "sairam123!";
		final String adminEmail = "admin@fightitaway.com";

		properties.put("mail.smtp.host", "smtp.gmail.com");
		properties.put("mail.smtp.port", "465");
		properties.put("mail.smtp.socketFactory.port", "465");
		properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		properties.put("mail.smtp.auth", "true");
		properties.put("mail.smtp.starttls.enable", true); // TLS
		properties.put("mail.smtp.auth", true);
		properties.put("mail.smtp.connectiontimeout", 5000);
		properties.put("mail.smtp.timeout", 5000);
		Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		});

		String bodyContentType = "text/html";
		String subject = request.getString("title");
		String body = request.getString("description");
		body += "</ br> This email is From [" + email + "]";
		List<String> toEmailList = new ArrayList<String>();
		toEmailList.add(adminEmail);
		// toEmailList.add("d.raghvendra@gmail.com");
		List<String> ccEmailList = new ArrayList<String>();
		// ccEmailList.add("d.raghvendra@gmail.com");
		List<String> bccEmailList = new ArrayList<String>();
		List<String> replyToList = new ArrayList<String>();
		replyToList.add(email);
		EmailMessage emailMessage = new EmailMessage(email, toEmailList, ccEmailList, bccEmailList, replyToList,
				subject, body, bodyContentType);
		logger.debug("##### " + method + " EMAIL MESSAGE -->> " + emailMessage.toString());
		boolean isMailSent = true;
		try {
			// 0. Adding Recipients.
			Message message = setRecipients(emailMessage, session);
			message.setSubject(emailMessage.getSubject());
			message.setText(emailMessage.getBody());

			// 1. Create text for mail.
			BodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setContent(emailMessage.getBody(), emailMessage.getBodyContentType());

			// 2. Add Multipart.
			Multipart multipart = new MimeMultipart();
			message.setContent(multipart);

			// 2a. Add text into multipart.
			multipart.addBodyPart(messageBodyPart);
			logger.debug("##### " + method + " EMAIL SENDING IN PROCESS . . . . . .");

			try {
				// 4. Send message.
				Transport.send(message);
				logger.info("##### " + method + " EMAIL SENT SUCCESSFULLY.");
			} catch (Exception e) {
				isMailSent = false;
				logger.error("##### " + method + " EXCEPTION -->> " + e.getMessage());
				jsonObject.put("code", "0001");
				jsonObject.put("message", "Email Sending Failed...!!!.");
				jsonObject.put("exception", e.getMessage());
			}
			// return true;

		} catch (MessagingException | UnsupportedEncodingException ex) {
			isMailSent = false;
			logger.error("##### " + method + " EXCEPTION 1 -->> " + ex.getMessage());
			jsonObject.put("code", "0001");
			jsonObject.put("message", "Email Sending Failed...!!!.");
			jsonObject.put("exception", ex.getMessage());
			ex.printStackTrace();
		} catch (Exception ex) {
			isMailSent = false;
			logger.error("##### " + method + " EXCEPTION 2 -->> " + ex.getMessage());
			jsonObject.put("code", "0001");
			jsonObject.put("message", "Email Sending Failed...!!!.");
			jsonObject.put("exception", ex.getMessage());
			ex.printStackTrace();
		}

		if (isMailSent) {
			jsonObject.put("code", "0000");
			jsonObject.put("message", "Email Sent Successfully.\r\n \r\n \r\n \r\n This email is From [" + email + "]");
			request.put("mailSent", isMailSent);
		}

		// return jsonObject;
		promise.complete(request);
		return promise.future();
		///////////////////////////////////////////////////////////
	}

	/**
	 * Set the status as 'true' if mail is sent. [UNUSED]
	 * 
	 * @param email
	 * @param request
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> setMailSentStatus(String email, JsonObject request, String traceId) {
		String method = "MongoRepositoryWrapper setMailSentStatus() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject response = new JsonObject();
		response.put("code", "0000");
		response.put("message", "success");
		try {
			String currentDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
					.format(AppUtil.getCalendarInstance().getTime());
			if (request.getBoolean("isEmailSendingRequestNew")) {
				JsonObject query = new JsonObject();
				query.put("_id",
						new JsonObject().put("email", email).put("date", ApiUtils.getCurrentDateInddMMMyyyyFormat(0)));
				query.put("categories", request.getJsonArray("categories"));
				query.put("createDateTime", ApiUtils.getCurrentTime());
				query.put("mailSent", request.getBoolean("mailSent"));
				query.put("mailSentOn", ApiUtils.getCurrentTime());
				query.put("updatedDateTime", new JsonObject().put("$date", currentDate));
				logger.info("##### " + method + " QUERY -->> " + query);
				client.rxSave("CUST_MAIL_SENT_STATUS", query).subscribe(res -> {
					logger.info("##### " + method + " MAIL SENT STATUS SAVED SUCCESSFULLY.");
					response.put("categories", request.getJsonArray("categories"));
				}, (ex) -> {
					logger.error("##### " + method + " ERROR (SAVE) -->> " + ex.getMessage());
					promise.fail(ex.getMessage());
				});
			} else {
				JsonObject query = new JsonObject();
				JsonObject payload = new JsonObject();
				payload.put("$set", new JsonObject().put("categories", request.getJsonArray("categories"))
						.put("mailSent", request.getBoolean("mailSent")).put("mailSentOn", ApiUtils.getCurrentTime())
						.put("updatedDateTime", new JsonObject().put("$date", currentDate)));

				query.put("_id",
						new JsonObject().put("email", email).put("date", ApiUtils.getCurrentDateInddMMMyyyyFormat(0)));
				logger.info("##### " + method + " QUERY   -->> " + query);
				logger.info("##### " + method + " PAYLOAD -->> " + payload);
				client.rxUpdateCollection("CUST_MAIL_SENT_STATUS", query, payload).subscribe(res -> {
					logger.info("##### " + method + " MAIL SENT STATUS UPDATED SUCCESSFULLY.");
				}, (ex) -> {
					logger.error("##### " + method + " ERROR (UPDATE) -->> " + ex.getMessage());
					promise.fail(ex.getMessage());
				});
			}
		} catch (Exception e) {
			logger.error("##### " + method + " EXCEPTION -->> " + e.getMessage());
			e.printStackTrace();
		}

		logger.info("##### " + method + " RESPONSE -->> " + response);
		promise.complete(response);
		return promise.future();
	}

	/**
	 * Send email once demographic is updated via [SEND-IN-BLUE].
	 * 
	 * @param email
	 * @param data
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> sendEmailPostDemographicUpdateBySendInBlue(String email, JsonObject data,
			String traceId) {
		JsonObject jsonObject = new JsonObject();
		Promise<JsonObject> promise = Promise.promise();
		if (null != data && !data.containsKey("lifeStyle") && !data.containsKey("diet")) {
			final String name = data.getString("name");
			final String adminEmail = this.config.getString("adminEmail");
			final Double targetedWeight = data.getDouble("targetedWeight");
			final Integer regTemplateId = config.getInteger("regId", 25);
			try {
				OkHttpClient client = new OkHttpClient();
				MediaType mediaType = MediaType.parse("application/json");
				String requestBody = "{\"to\":[{\"email\":\"" + email + "\",\"name\":\"" + name
						+ "\"}],\"replyTo\":{\"email\":\"" + adminEmail + "\",\"name\":\"" + name
						+ "\"},\"params\":{\"user\":\"" + name + "\",\"targetedweight\":\"" + targetedWeight
						+ "\"},\"templateId\":" + regTemplateId + "}";
				RequestBody body = RequestBody.create(mediaType, requestBody);
				Request sendBlueInRequest = new Request.Builder().url(config.getString("siburlv3")).post(body)
						.addHeader("api-key", config.getString("sibapikeyv3")).build();

				Response response = null;
				response = client.newCall(sendBlueInRequest).execute();
				if (response.isSuccessful()) {
					jsonObject.put("code", "0000");
					jsonObject.put("message",
							"Email Sent Successfully.\r\n \r\n \r\n \r\n This email is for [" + email + "]");
				} else {
					jsonObject.put("code", "0001");
					jsonObject.put("message", "Email not sent to [" + email + "]");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			jsonObject.put("code", "0001");
			jsonObject.put("message",
					"Customer already existed.\r\n \r\n \r\n \r\n This email is From [" + email + "]");
		}

		promise.complete(jsonObject);
		return promise.future();
	}

	/**
	 * Send Email once payment is done via [SEND-IN-BLUE]
	 * 
	 * @param email
	 * @param data
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> sendEmailPostPaymentBySendInBlue(String email, JsonObject data, String traceId) {
		String method = "MongoRepositoryWrapper sendEmailPostPaymentBySendInBlue() " + traceId + "-[" + email + "]";
		JsonObject jsonObject = new JsonObject();
		Promise<JsonObject> promise = Promise.promise();
		logger.info("##### " + method + " REQUEST DATA -->> " + data);
		String name = data.getString("name");
		final String adminEmail = this.config.getString("adminEmail");
		final Double targetedWeight = data.getDouble("targetedWeight");
		final Integer paymentTemplateId = config.getInteger("paymentId", 22);
		try {
			OkHttpClient client = new OkHttpClient();
			MediaType mediaType = MediaType.parse("application/json");
			String requestBody = "{\"to\":[{\"email\":\"" + email + "\",\"name\":\"" + name
					+ "\"}],\"cc\":[{\"email\":\"" + adminEmail + "\",\"name\":\"Admin\"}],\"replyTo\":{\"email\":\""
					+ adminEmail + "\",\"name\":\"" + name + "\"},\"params\":{\"user\":\"" + name
					+ "\", \"targetedweight\":\"" + targetedWeight + "\", \"payment\":\""
					+ data.getDouble("recentPaidAmount") + "\",\"expirydate\":\"" + data.getString("planExpiryDate")
					+ "\"},\"templateId\":" + paymentTemplateId + "}";
			logger.info("##### " + method + " REQUESTBODY -->> " + requestBody);

			RequestBody body = RequestBody.create(mediaType, requestBody);
			Request sendBlueInRequest = new Request.Builder().url(config.getString("siburlv3")).post(body)
					.addHeader("api-key", config.getString("sibapikeyv3")).build();

			Response response = null;
			response = client.newCall(sendBlueInRequest).execute();
			if (response.isSuccessful()) {
				logger.info("##### " + method + " EMAIL SENT SUCCESSFULLY TO [" + email + "]");
				logger.info("##### " + method + " RESPONSE -->> " + response.body().string());
				jsonObject.put("code", "0000");
				jsonObject.put("message",
						"Email Sent Successfully.\r\n \r\n \r\n \r\n This email is for [" + email + "]");
			} else {
				logger.info("##### " + method + " EMAIL NOT SENT");
				jsonObject.put("code", "0001");
				jsonObject.put("message", "Email not sent to [" + email + "]");
			}
		} catch (IOException e) {
			logger.error("##### " + method + " EXCEPTION OCCURRED -->> " + e.getMessage());
			e.printStackTrace();
		}

		promise.complete(jsonObject);
		return promise.future();
	}

	/**
	 * Send email once demographic details are updated.
	 * 
	 * @param userEmail
	 * @param request
	 * @param data
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> sendEmailPostDemographicUpdateByGmail(String userEmail, JsonObject data, String traceId) {
		String method = "MongoRepositoryWrapper sendEmailPostDemographicUpdateByGmail() " + traceId + "-[" + userEmail
				+ "]";
		JsonObject jsonObject = new JsonObject();
		Promise<JsonObject> promise = Promise.promise();
		if (!data.containsKey("lifeStyle") && !data.containsKey("diet")) {
			///////////////////////////////////////////////////////////
			String name = data.getString("name");
			Double targetedweight = data.getDouble("targetedWeight");
			Properties properties = new Properties();
			final String username = "admin@fightitaway.com";
//			final String adminEmail = "admin@fightitaway.com";
			final String adminEmail = "admin@smartdietplanner.com";
			final String password = "sairam123!";

			properties.put("mail.smtp.host", "smtp.gmail.com");
			properties.put("mail.smtp.port", "465");
			properties.put("mail.smtp.socketFactory.port", "465");
			properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			properties.put("mail.smtp.auth", "true");
			properties.put("mail.smtp.starttls.enable", true); // TLS
			properties.put("mail.smtp.auth", true);
			properties.put("mail.smtp.connectiontimeout", 5000);
			properties.put("mail.smtp.timeout", 5000);
			Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(username, password);
				}
			});
			String bodyContentType = "text/html";
			String subject = "\"Smart Diet Planner\" Registration is successful";
			String emailBodyPostDemographic = config.getString("emailBodyPostDemographic");
			String body = String.format(emailBodyPostDemographic, name, targetedweight, targetedweight)
					.replace("percentage", "%");

			List<String> toEmailList = new ArrayList<String>();
			toEmailList.add(userEmail);
			List<String> ccEmailList = new ArrayList<String>();
			// ccEmailList.add("fakhre.alam101@gmail.com");
			// ccEmailList.add("d.raghvendra@gmail.com");
			List<String> bccEmailList = new ArrayList<String>();
			List<String> replyToList = new ArrayList<String>();
			replyToList.add(userEmail);
			EmailMessage emailMessage = new EmailMessage(adminEmail, toEmailList, ccEmailList, bccEmailList,
					replyToList, subject, body, bodyContentType);
			boolean isMailSent = true;
			try {
				// 0. Adding Recipients.
				Message message = setRecipients(emailMessage, session);
				message.setSubject(emailMessage.getSubject());
				message.setText(emailMessage.getBody());

				// 1. Create text for mail.
				BodyPart messageBodyPart = new MimeBodyPart();
				messageBodyPart.setContent(emailMessage.getBody(), emailMessage.getBodyContentType());

				// 2. Add Multipart.
				Multipart multipart = new MimeMultipart();
				message.setContent(multipart);

				// 2a. Add text into multipart.
				multipart.addBodyPart(messageBodyPart);

				try {
					// 4. Send message.
					Transport.send(message);
					logger.info("##### " + method + " EMAIL SENT SUCCESSFULLY.");
				} catch (Exception e) {
					isMailSent = false;
					logger.error("##### " + method + " CHECKING EXCEPTION -->> " + e.getMessage());
					jsonObject.put("code", "0001");
					jsonObject.put("message", "Email Sending Failed...!!!.");
					jsonObject.put("exception", e.getMessage());
				}
				// return true;

			} catch (MessagingException | UnsupportedEncodingException ex) {
				isMailSent = false;
				logger.error("##### " + method + " ERROR 1 -->> " + ex.getMessage());
				jsonObject.put("code", "0001");
				jsonObject.put("message", "Email Sending Failed...!!!.");
				jsonObject.put("exception", ex.getMessage());
				ex.printStackTrace();
			} catch (Exception ex) {
				isMailSent = false;
				logger.error("##### " + method + " ERROR 2 -->> " + ex.getMessage());
				jsonObject.put("code", "0001");
				jsonObject.put("message", "Email Sending Failed...!!!.");
				jsonObject.put("exception", ex.getMessage());
				ex.printStackTrace();
			}

			if (isMailSent) {
				jsonObject.put("code", "0000");
				jsonObject.put("message",
						"Email Sent Successfully.\r\n \r\n \r\n \r\n This email is From [" + userEmail + "]");
			}
		} else {
			jsonObject.put("code", "0001");
			jsonObject.put("message",
					"Customer is existing one only.\r\n \r\n \r\n \r\n This email is From [" + userEmail + "]");
			logger.info("##### " + method + " CUSTOMER [" + userEmail + "] IS EXISTING ONE ONLY");
		}

		// return jsonObject;
		promise.complete(jsonObject);
		return promise.future();
		///////////////////////////////////////////////////////////
	}

	/**
	 * Send email once payment is done.
	 * 
	 * @param userEmail
	 * @param request
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> sendEmailPostPaymentByGmail(String userEmail, JsonObject request, String traceId) {
		String method = "MongoRepositoryWrapper sendEmailPostPaymentByGmail() " + traceId + "-[" + userEmail + "]";
		JsonObject jsonObject = new JsonObject();
		Promise<JsonObject> promise = Promise.promise();
		logger.info("##### " + method + " REACHED TO EMAIL FOR GMAIL");
		///////////////////////////////////////////////////////////
		String name = request.getString("name");
		Double payment = request.getDouble("recentPaidAmount");
		String expiryDate = request.getString("planExpiryDate");
//		String targetedweight = request.getString("targetedweight");
		Properties properties = new Properties();
		final String username = "admin@fightitaway.com";
//		final String adminEmail = "admin@fightitaway.com";
		final String adminEmail = "admin@smartdietplanner.com";
		final String password = "sairam123!";

		properties.put("mail.smtp.host", "smtp.gmail.com");
		properties.put("mail.smtp.port", "465");
		properties.put("mail.smtp.socketFactory.port", "465");
		properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		properties.put("mail.smtp.auth", "true");
		properties.put("mail.smtp.starttls.enable", true); // TLS
		properties.put("mail.smtp.auth", true);
		properties.put("mail.smtp.connectiontimeout", 5000);
		properties.put("mail.smtp.timeout", 5000);
		Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		});
		String bodyContentType = "text/html";

		String subject = "Payment received for premium version of \"Smart Diet Planner\" !";
		logger.info("##### " + method + " TITLE -->> " + subject);
		String emailBodyPostPayment = config.getString("emailBodyPostPayment");
		logger.info("##### " + method + " emailBodyPostPayment -->> " + emailBodyPostPayment);

		String body = String.format(emailBodyPostPayment, name, payment, expiryDate);
		logger.info("##### " + method + " BODY -->> " + body);

		List<String> toEmailList = new ArrayList<String>();
		toEmailList.add(userEmail);
		// toEmailList.add("d.raghvendra@gmail.com");
		List<String> ccEmailList = new ArrayList<String>();
		ccEmailList.add(adminEmail);
		// ccEmailList.add("d.raghvendra@gmail.com");
		List<String> bccEmailList = new ArrayList<String>();
		List<String> replyToList = new ArrayList<String>();
		replyToList.add(userEmail);
		EmailMessage emailMessage = new EmailMessage(adminEmail, toEmailList, ccEmailList, bccEmailList, replyToList,
				subject, body, bodyContentType);
		logger.info("##### " + method + " EMAIL MESSAGE -->> " + emailMessage.toString());
		boolean isMailSent = true;
		try {
			// 0. Adding Recipients.
			Message message = setRecipients(emailMessage, session);
			message.setSubject(emailMessage.getSubject());
			message.setText(emailMessage.getBody());

			// 1. Create text for mail.
			BodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setContent(emailMessage.getBody(), emailMessage.getBodyContentType());

			// 2. Add Multipart.
			Multipart multipart = new MimeMultipart();
			message.setContent(multipart);

			// 2a. Add text into multipart.
			multipart.addBodyPart(messageBodyPart);
			logger.info("##### " + method + " EMAIL SENDING IN PROCESS . . . . . .");

			try {
				// 4. Send message.
				Transport.send(message);
				logger.info("##### " + method + " EMAIL SENT SUCCESSFULLY.");
			} catch (Exception e) {
				isMailSent = false;
				logger.error("##### " + method + " CHECKING EXCEPTION -->> " + e.getMessage());
				jsonObject.put("code", "0001");
				jsonObject.put("message", "Email Sending Failed...!!!.");
				jsonObject.put("exception", e.getMessage());
			}
			// return true;

		} catch (MessagingException | UnsupportedEncodingException ex) {
			isMailSent = false;
			logger.error("##### " + method + " ERROR 1 -->> " + ex.getMessage());
			jsonObject.put("code", "0001");
			jsonObject.put("message", "Email Sending Failed...!!!.");
			jsonObject.put("exception", ex.getMessage());
			ex.printStackTrace();
		} catch (Exception ex) {
			isMailSent = false;
			logger.error("##### " + method + " ERROR 2 -->> " + ex.getMessage());
			jsonObject.put("code", "0001");
			jsonObject.put("message", "Email Sending Failed...!!!.");
			jsonObject.put("exception", ex.getMessage());
			ex.printStackTrace();
		}

		if (isMailSent) {
			jsonObject.put("code", "0000");
			jsonObject.put("message",
					"Email Sent Successfully.\r\n \r\n \r\n \r\n This email is From [" + userEmail + "]");
		}

		promise.complete(jsonObject);
		return promise.future();
		///////////////////////////////////////////////////////////
	}

	/**
	 * Set receipietns - email.
	 * 
	 * @param emailMessage
	 * @param session
	 * @return Message
	 * 
	 * @throws MessagingException
	 * @throws UnsupportedEncodingException
	 */
	private Message setRecipients(EmailMessage emailMessage, Session session)
			throws MessagingException, UnsupportedEncodingException {
		String method = "MongoRepositoryWrapper setRecipients()";
		Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress(emailMessage.getMailSender()));
		logger.debug("##### " + method + " MAIL SENDER [" + emailMessage.getMailSender() + "]");
		// set to list in message
		for (String to : emailMessage.getToEmail())
			message.addRecipients(RecipientType.TO, InternetAddress.parse(to));

		// set cc list in message
		for (String cc : emailMessage.getCcEmail())
			message.addRecipients(Message.RecipientType.CC, InternetAddress.parse(cc));

		// set bcc list in message
		for (String bcc : emailMessage.getBccEmail())
			message.addRecipients(Message.RecipientType.BCC, InternetAddress.parse(bcc));

		// message.setReplyTo(new InternetAddress[] {replyTo});
		message.setReplyTo(new Address[] { new InternetAddress(emailMessage.getReplyToEmail().get(0)) });

		// set reply-to list in message
//		for (String replyTo : emailMessage.getReplyToEmail())
//			message.addRecipients(Message.RecipientType., InternetAddress.parse(replyTo));

		return message;
	}

	/**
	 * Sorting slot4 specific items.
	 * 
	 * @param slots4
	 * @return List<JsonObject>
	 */
	private List<JsonObject> sortingSlot4SpecificItems(List<JsonObject> slots4) {
		String[] fixedSlot4ItemsArray = { "B", "A", "C", "S" };
		List<JsonObject> slot4SortedItems = new ArrayList<>();
		List<JsonObject> itemsB = slots4.stream()
				.filter(x -> fixedSlot4ItemsArray[0].trim().equalsIgnoreCase(x.getString("Type")))
				.collect(Collectors.toList());
		List<JsonObject> itemsA = slots4.stream()
				.filter(x -> fixedSlot4ItemsArray[1].trim().equalsIgnoreCase(x.getString("Type")))
				.collect(Collectors.toList());
		List<JsonObject> itemsC = slots4.stream()
				.filter(x -> fixedSlot4ItemsArray[2].trim().equalsIgnoreCase(x.getString("Type")))
				.collect(Collectors.toList());
		List<JsonObject> itemsS = slots4.stream()
				.filter(x -> fixedSlot4ItemsArray[3].trim().equalsIgnoreCase(x.getString("Type")))
				.collect(Collectors.toList());
		List<JsonObject> itemsRemaining = slots4.stream()
				.filter(x -> (!fixedSlot4ItemsArray[0].trim().equalsIgnoreCase(x.getString("Type"))
						&& !fixedSlot4ItemsArray[1].trim().equalsIgnoreCase(x.getString("Type"))
						&& !fixedSlot4ItemsArray[2].trim().equalsIgnoreCase(x.getString("Type"))
						&& !fixedSlot4ItemsArray[3].trim().equalsIgnoreCase(x.getString("Type"))))
				.collect(Collectors.toList());
		if (null != itemsB && itemsB.size() > 0)
			slot4SortedItems.addAll(itemsB);
		if (null != itemsA && itemsA.size() > 0)
			slot4SortedItems.addAll(itemsA);
		if (null != itemsC && itemsC.size() > 0)
			slot4SortedItems.addAll(itemsC);
		if (null != itemsS && itemsS.size() > 0)
			slot4SortedItems.addAll(itemsS);
		if (null != itemsRemaining && itemsRemaining.size() > 0)
			slot4SortedItems.addAll(itemsRemaining);

		return slot4SortedItems;
	}

	/**
	 * Get salad on top and after than remaining items in slot4.
	 * 
	 * @param slots4
	 * @param itemCode
	 * @return List<JsonObject>
	 */
	private List<JsonObject> getSaladOnTopAndThenRemaingItems(List<JsonObject> slots4, String itemCode) {

		List<JsonObject> finalSlots4List = new ArrayList<JsonObject>();
		List<JsonObject> slots4With034 = slots4.stream().filter(x -> x.getString("itemCode").equalsIgnoreCase(itemCode))
				.collect(Collectors.toList());
		List<JsonObject> slots4Without034 = slots4.stream()
				.filter(x -> !x.getString("itemCode").equalsIgnoreCase(itemCode)).collect(Collectors.toList());

		finalSlots4List.addAll(slots4With034);
		finalSlots4List.addAll(slots4Without034);

		return finalSlots4List;
	}

	/**
	 * Check all slots total calories.
	 * 
	 * @param slots0
	 * @param slots1
	 * @param slots2
	 * @param slots3
	 * @param slots4
	 * @param slots5
	 * @param slots6
	 * @param slots7
	 * @param slots8
	 * @return Double
	 */
	private Double checkCalories(List<JsonObject> slots0, List<JsonObject> slots1, List<JsonObject> slots2,
			List<JsonObject> slots3, List<JsonObject> slots4, List<JsonObject> slots5, List<JsonObject> slots6,
			List<JsonObject> slots7, List<JsonObject> slots8) {
		List<JsonObject> allPlan = new ArrayList<JsonObject>();
		allPlan.addAll(slots0);
		allPlan.addAll(slots1);
		allPlan.addAll(slots2);
		allPlan.addAll(slots3);
		allPlan.addAll(slots4);
		allPlan.addAll(slots5);
		allPlan.addAll(slots6);
		allPlan.addAll(slots7);
		allPlan.addAll(slots8);

		Double tolalCalories = ApiUtils.getTotalCalories(allPlan);

		return tolalCalories;

	}

	/**
	 * Get complete plan subscription.
	 * 
	 * @param profiles
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getCompletePlanSubscription(JsonObject profiles) {
		String method = "MongoRepositoryWrapper getCompletePlanSubscription()";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject();
		JsonObject response = new JsonObject();
		try {
			JsonObject todaysss = new JsonObject();// profiles.getJsonObject("today");
			JsonObject yesterdaysss = new JsonObject();// profiles.getJsonObject("yesterday");
			JsonObject last7Daysss = new JsonObject();// profiles.getJsonObject("last7Days");
			JsonObject last15Daysss = new JsonObject();// profiles.getJsonObject("last15days");
			JsonObject last30Daysss = new JsonObject();// profiles.getJsonObject("last30Days");
			JsonObject last90Daysss = new JsonObject();// profiles.getJsonObject("last90Days");
			JsonObject tillDaysss = new JsonObject();// profiles.getJsonObject("tillDateDays");
			client.rxFind("PLAN_SUBCRIPTION_DETAIL", query).map(map -> {
				return map;
			}).subscribe(res -> {
				List<JsonObject> todayDonePlan = new ArrayList<JsonObject>();
				List<JsonObject> yesterdayDonePlan = new ArrayList<JsonObject>();
				List<JsonObject> last7daysDonePlan = new ArrayList<JsonObject>();
				List<JsonObject> last15daysDonePlan = new ArrayList<JsonObject>();
				List<JsonObject> last30daysDonePlan = new ArrayList<JsonObject>();
				List<JsonObject> last90daysDonePlan = new ArrayList<JsonObject>();
				List<JsonObject> tillDateDonePlan = new ArrayList<JsonObject>();
				DateFormat dateFormatISO = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
				Calendar cal1 = new GregorianCalendar();
				Calendar cal2 = new GregorianCalendar();
				res.forEach(action -> {
					JsonObject json = action;
					String paymentDoneDateString;
					try {
						paymentDoneDateString = dateFormatISO
								.format(dateFormatISO.parse(json.getString("createdDate")));
						Date createdDate = dateFormatISO.parse(paymentDoneDateString);
						cal1.setTime(createdDate);
						Date endDate = new Date();
						cal2.setTime(endDate);
						int noOfDaysPassed = daysPassed(cal1.getTime(), cal2.getTime());
						if (noOfDaysPassed == 0) {
							todayDonePlan.add(json);
							last7daysDonePlan.add(json);
							last15daysDonePlan.add(json);
							last30daysDonePlan.add(json);
							last90daysDonePlan.add(json);
							tillDateDonePlan.add(json);
						} else if (noOfDaysPassed == 1) {
							yesterdayDonePlan.add(action);
							last7daysDonePlan.add(json);
							last15daysDonePlan.add(json);
							last30daysDonePlan.add(json);
							last90daysDonePlan.add(json);
							tillDateDonePlan.add(json);
						} else if (noOfDaysPassed <= 7) {
							last7daysDonePlan.add(json);
							last15daysDonePlan.add(json);
							last30daysDonePlan.add(json);
							last90daysDonePlan.add(json);
							tillDateDonePlan.add(json);
						} else if (noOfDaysPassed <= 15) {
							last15daysDonePlan.add(json);
							last30daysDonePlan.add(json);
							last90daysDonePlan.add(json);
							tillDateDonePlan.add(json);
						} else if (noOfDaysPassed <= 30) {
							last30daysDonePlan.add(json);
							last90daysDonePlan.add(json);
							tillDateDonePlan.add(json);
						} else if (noOfDaysPassed <= 90) {
							last90daysDonePlan.add(json);
							tillDateDonePlan.add(json);
						} else {
							tillDateDonePlan.add(json);
						}
						logger.info("##### REACHED FINALLY");
					} catch (ParseException e) {
						logger.info("##### ERROR -->> " + e.getMessage());
						e.printStackTrace();
					}
				});

				JsonObject todayPaymentDone = new JsonObject();
				JsonObject yesterdayPaymentDone = new JsonObject();
				JsonObject last7DaysPaymentDone = new JsonObject();
				JsonObject last15DaysPaymentDone = new JsonObject();
				JsonObject last30DaysPaymentDone = new JsonObject();
				JsonObject last90DaysPaymentDone = new JsonObject();
				JsonObject tillDaysPaymentDone = new JsonObject();
				todayPaymentDone.put("todayPaymentDoneCount", todayDonePlan.size());
				todayPaymentDone.put("todayPaymentDone", todayDonePlan);
				yesterdayPaymentDone.put("yesterdayPaymentDoneCount", yesterdayPaymentDone.size());
				yesterdayPaymentDone.put("yesterdayPaymentDone", yesterdayPaymentDone);
				last7DaysPaymentDone.put("last7DaysPaymentDoneCount", last7DaysPaymentDone.size());
				last7DaysPaymentDone.put("last7DaysPaymentDone", last7DaysPaymentDone);
				last15DaysPaymentDone.put("last15DaysPaymentDoneCount", last15DaysPaymentDone.size());
				last15DaysPaymentDone.put("last15DaysPaymentDone", last15DaysPaymentDone);
				last30DaysPaymentDone.put("last30DaysPaymentDoneCount", last30DaysPaymentDone.size());
				last30DaysPaymentDone.put("last30DaysPaymentDone", last30DaysPaymentDone);
				last90DaysPaymentDone.put("last90DaysPaymentDoneCount", last90DaysPaymentDone.size());
				last90DaysPaymentDone.put("last90DaysPaymentDone", last90DaysPaymentDone);
				tillDaysPaymentDone.put("tillDaysPaymentDoneCount", tillDaysPaymentDone.size());
				tillDaysPaymentDone.put("tillDaysPaymentDone", tillDaysPaymentDone);
				todaysss.put("payment", todayPaymentDone);
				yesterdaysss.put("payment", yesterdayPaymentDone);
				last7Daysss.put("payment", last7DaysPaymentDone);
				last15Daysss.put("payment", last15DaysPaymentDone);
				last30Daysss.put("payment", last30DaysPaymentDone);
				last90Daysss.put("payment", last90DaysPaymentDone);
				tillDaysss.put("payment", tillDaysPaymentDone);
				response.put("today", todaysss);
				response.put("yesterday", yesterdaysss);
				response.put("last7Days", last7Daysss);
				response.put("last15days", last15Daysss);
				response.put("last30Days", last30Daysss);
				response.put("last90Days", last90Daysss);
				response.put("tillDateDays", tillDaysss);
				promise.complete(response);

			}, (ex) -> {
				logger.error("##### " + method + " ERROR 1 -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});

		} catch (Exception e) {
			logger.info("##### ERROR 2 -->> " + e.getMessage());
			e.printStackTrace();
		}
		return promise.future();
	}

	/**
	 * Get customer profiles.
	 * 
	 * @param query
	 * @param profile
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getCustProfiles(JsonObject query, CustProfile profile) {
		String method = "MongoRepositoryWrapper getCustProfiles()";
		Promise<JsonObject> promise = Promise.promise();

		JsonObject response = new JsonObject();
		JsonObject today = new JsonObject();
		JsonObject yesterday = new JsonObject();
		JsonObject last7Days = new JsonObject();
		JsonObject last15Days = new JsonObject();
		JsonObject last30Days = new JsonObject();
		JsonObject last90Days = new JsonObject();
		JsonObject tillDays = new JsonObject();
		try {
			List<JsonObject> tillProfileOnly = new ArrayList<JsonObject>();
			List<JsonObject> tillDemographicOnly = new ArrayList<JsonObject>();
			List<JsonObject> tillLifeStyleOnly = new ArrayList<JsonObject>();
			List<JsonObject> tillDietOnly = new ArrayList<JsonObject>();
			client.rxFind("CUST_PROFILE", query).map(map -> {
				map.forEach(action -> {

				});
				return map;
			}).subscribe(res -> {
				res.forEach(action -> {
					if (null != action.getJsonObject("profile")) {
						if (null != action.getJsonObject("demographic")) {
							if (null != action.getJsonObject("lifeStyle")) {
								if (null != action.getJsonObject("diet")) {
									tillDietOnly.add(action);
									return;
								} else {
									logger.debug("##### " + method + " DIET IS NOT AVAILABLE.");
								}
								tillLifeStyleOnly.add(action);
								return;
							} else {
								logger.debug("##### " + method + " LIFESTYLE IS NOT AVAILABLE.");
							}
							tillDemographicOnly.add(action);
							return;
						} else {
							logger.debug("##### " + method + " DEMOGRAPHIC IS NOT AVAILABLE.");
						}
						tillProfileOnly.add(action);
					} else {
						logger.debug("##### " + method + " DEMOGRAPHIC IS NOT AVAILABLE.");
					}
				});

				if (null != tillDemographicOnly)
					logger.debug("##### " + method + " TILL DEMOGRAHIC SIZE -->> " + tillDemographicOnly.size());
				if (null != tillLifeStyleOnly)
					logger.debug("##### " + method + " TILL LIFESTYLE SIZE  -->> " + tillLifeStyleOnly.size());
				if (null != tillDietOnly)
					logger.debug("##### " + method + " TILL DIET SIZE       -->> " + tillDietOnly.size());

				// DEMOGRAPH
				List<JsonObject> todayDemography = new ArrayList<JsonObject>();
				List<JsonObject> yesterdayDemography = new ArrayList<JsonObject>();
				List<JsonObject> last7daysDemography = new ArrayList<JsonObject>();
				List<JsonObject> last15daysDemography = new ArrayList<JsonObject>();
				List<JsonObject> last30daysDemography = new ArrayList<JsonObject>();
				List<JsonObject> last90daysDemography = new ArrayList<JsonObject>();
				List<JsonObject> tillDateDemography = new ArrayList<JsonObject>();
				SimpleDateFormat format1 = new SimpleDateFormat("ddMMyyyy");
				SimpleDateFormat format2 = new SimpleDateFormat("dd-MMM-yyyy");
				tillDemographicOnly.forEach(action -> {
					JsonObject json = action;
					String createdDateString = json.getJsonObject("profile").getString("createdDate");
					json.put("suggestedCalories", 0.0);
					json.put("suggestedWeight", json.getJsonObject("demographic").getDouble("suggestedWeight"));
					json.put("tragetedDate", "");
					try {
						Calendar cal1 = new GregorianCalendar();
						Calendar cal2 = new GregorianCalendar();
						Date createdDate = format2.parse(createdDateString);
						cal1.setTime(createdDate);
						Date endDate = new Date();
						cal2.setTime(endDate);
						int noOfDaysPassed = daysPassed(cal1.getTime(), cal2.getTime());
						if (noOfDaysPassed == 0) {
							todayDemography.add(json);
							last7daysDemography.add(json);
							last15daysDemography.add(json);
							last30daysDemography.add(json);
							last90daysDemography.add(json);
							tillDateDemography.add(json);
						} else if (noOfDaysPassed == 1) {
							yesterdayDemography.add(json);
							last7daysDemography.add(json);
							last15daysDemography.add(json);
							last30daysDemography.add(json);
							last90daysDemography.add(json);
							tillDateDemography.add(json);
						} else if (noOfDaysPassed <= 7) {
							last7daysDemography.add(json);
							last15daysDemography.add(json);
							last30daysDemography.add(json);
							last90daysDemography.add(json);
							tillDateDemography.add(json);
						} else if (noOfDaysPassed <= 15) {
							last15daysDemography.add(json);
							last30daysDemography.add(json);
							last90daysDemography.add(json);
							tillDateDemography.add(json);
						} else if (noOfDaysPassed <= 30) {
							last30daysDemography.add(json);
							last90daysDemography.add(json);
							tillDateDemography.add(json);
						} else if (noOfDaysPassed <= 90) {
							last90daysDemography.add(json);
							tillDateDemography.add(json);
						} else {
							tillDateDemography.add(json);
						}
					} catch (ParseException e) {
						e.printStackTrace();
					}
				});

				// LIFESTYLE
				List<JsonObject> todayLifeStyle = new ArrayList<JsonObject>();
				List<JsonObject> yesterdayLifeStyle = new ArrayList<JsonObject>();
				List<JsonObject> last7daysLifeStyle = new ArrayList<JsonObject>();
				List<JsonObject> last15daysLifeStyle = new ArrayList<JsonObject>();
				List<JsonObject> last30daysLifeStyle = new ArrayList<JsonObject>();
				List<JsonObject> last90daysLifeStyle = new ArrayList<JsonObject>();
				List<JsonObject> tillDateLifeStyle = new ArrayList<JsonObject>();
				tillLifeStyleOnly.forEach(action -> {
					JsonObject json = action;
					String createdDateString = json.getJsonObject("profile").getString("createdDate");
					json.put("suggestedCalories", json.getJsonObject("lifeStyle").getDouble("calories"));
					json.put("suggestedWeight", json.getJsonObject("demographic").getDouble("suggestedWeight"));
					json.put("tragetedDeight", "");
					try {
						Calendar cal1 = new GregorianCalendar();
						Calendar cal2 = new GregorianCalendar();
						Date createdDate = format2.parse(createdDateString);
						cal1.setTime(createdDate);
						Date endDate = new Date();
						cal2.setTime(endDate);
						int noOfDaysPassed = daysPassed(cal1.getTime(), cal2.getTime());
						if (noOfDaysPassed == 0) {
							todayLifeStyle.add(json);
							last7daysLifeStyle.add(json);
							last15daysLifeStyle.add(json);
							last30daysLifeStyle.add(json);
							last90daysLifeStyle.add(json);
							tillDateLifeStyle.add(json);
						} else if (noOfDaysPassed == 1) {
							yesterdayLifeStyle.add(json);
							last7daysLifeStyle.add(json);
							last15daysLifeStyle.add(json);
							last30daysLifeStyle.add(json);
							last90daysLifeStyle.add(json);
							tillDateLifeStyle.add(json);
						} else if (noOfDaysPassed <= 7) {
							last7daysLifeStyle.add(json);
							last15daysLifeStyle.add(json);
							last30daysLifeStyle.add(json);
							last90daysLifeStyle.add(json);
							tillDateLifeStyle.add(json);
						} else if (noOfDaysPassed <= 15) {
							last15daysLifeStyle.add(json);
							last30daysLifeStyle.add(json);
							last90daysLifeStyle.add(json);
							tillDateLifeStyle.add(json);
						} else if (noOfDaysPassed <= 30) {
							last30daysLifeStyle.add(json);
							last90daysLifeStyle.add(json);
							tillDateLifeStyle.add(json);
						} else if (noOfDaysPassed <= 90) {
							last90daysLifeStyle.add(json);
							tillDateLifeStyle.add(json);
						} else {
							tillDateLifeStyle.add(json);
						}
					} catch (ParseException e) {
						e.printStackTrace();
					}
					System.out.println();
				});

				// DIET
				List<JsonObject> todayDiet = new ArrayList<JsonObject>();
				List<JsonObject> yesterdayDiet = new ArrayList<JsonObject>();
				List<JsonObject> last7daysDiet = new ArrayList<JsonObject>();
				List<JsonObject> last15daysDiet = new ArrayList<JsonObject>();
				List<JsonObject> last30daysDiet = new ArrayList<JsonObject>();
				List<JsonObject> last90daysDiet = new ArrayList<JsonObject>();
				List<JsonObject> tillDateDiet = new ArrayList<JsonObject>();
				tillDietOnly.forEach(action -> {
					JsonObject json = action;
					CustProfile customer = profile;
					customer = getCustomerTargetedWeight(json, profile,
							json.getJsonObject("demographic").getInteger("weightInKg"),
							json.getJsonObject("demographic").getDouble("suggestedWeight"));
					json.put("suggestedCalories", json.getJsonObject("lifeStyle").getDouble("calories"));
					json.put("targetedDate", customer.getTargetedDate());
					json.put("suggestedWeight", json.getJsonObject("demographic").getDouble("suggestedWeight"));
					String createdDateString = json.getJsonObject("profile").getString("createdDate");
					try {
						Calendar cal1 = new GregorianCalendar();
						Calendar cal2 = new GregorianCalendar();
						Date createdDate = format2.parse(createdDateString);
						cal1.setTime(createdDate);
						Date endDate = new Date();
						cal2.setTime(endDate);
						int noOfDaysPassed = daysPassed(cal1.getTime(), cal2.getTime());
						if (noOfDaysPassed == 0) {
							todayDiet.add(json);
							last7daysDiet.add(json);
							last15daysDiet.add(json);
							last30daysDiet.add(json);
							last90daysDiet.add(json);
							tillDateDiet.add(json);
						} else if (noOfDaysPassed == 1) {
							yesterdayDiet.add(json);
							last7daysDiet.add(json);
							last15daysDiet.add(json);
							last30daysDiet.add(json);
							last90daysDiet.add(json);
							tillDateDiet.add(json);
						} else if (noOfDaysPassed <= 7) {
							last7daysDiet.add(json);
							last15daysDiet.add(json);
							last30daysDiet.add(json);
							last90daysDiet.add(json);
							tillDateDiet.add(json);
						} else if (noOfDaysPassed <= 15) {
							last15daysDiet.add(json);
							last30daysDiet.add(json);
							last90daysDiet.add(json);
							tillDateDiet.add(json);
						} else if (noOfDaysPassed <= 30) {
							last30daysDiet.add(json);
							last90daysDiet.add(json);
							tillDateDiet.add(json);
						} else if (noOfDaysPassed <= 90) {
							last90daysDiet.add(json);
							tillDateDiet.add(json);
						} else {
							tillDateDiet.add(json);
						}

					} catch (ParseException e) {
						e.printStackTrace();
					}
				});

				JsonObject todayDemographic = new JsonObject();
				JsonObject yesterdayDemographic = new JsonObject();
				JsonObject last7DaysDemographic = new JsonObject();
				JsonObject last15DaysDemographic = new JsonObject();
				JsonObject last30DaysDemographic = new JsonObject();
				JsonObject last90DaysDemographic = new JsonObject();
				JsonObject tillDaysDemographic = new JsonObject();

				JsonObject todayLifestyle = new JsonObject();
				JsonObject yesterdayLifestyle = new JsonObject();
				JsonObject last7DaysLifestyle = new JsonObject();
				JsonObject last15DaysLifestyle = new JsonObject();
				JsonObject last30DaysLifestyle = new JsonObject();
				JsonObject last90DaysLifestyle = new JsonObject();
				JsonObject tillDaysLifestyle = new JsonObject();

				JsonObject todayDiets = new JsonObject();
				JsonObject yesterdayDiets = new JsonObject();
				JsonObject last7DaysDiets = new JsonObject();
				JsonObject last15DaysDiets = new JsonObject();
				JsonObject last30DaysDiets = new JsonObject();
				JsonObject last90DaysDiets = new JsonObject();
				JsonObject tillDaysDiets = new JsonObject();

				todayDemographic.put("stage", "DEMOGRAPHIC");
				todayDemographic.put("todayDemographyCount", todayDemography.size());
				todayDemographic.put("todayDemography", todayDemography);
				yesterdayDemographic.put("stage", "DEMOGRAPHIC");
				yesterdayDemographic.put("yesterdayDemographicCount", yesterdayDemography.size());
				yesterdayDemographic.put("yesterdayDemographic", yesterdayDemography);
				last7DaysDemographic.put("stage", "DEMOGRAPHIC");
				last7DaysDemographic.put("last7DaysDemographyCount", last7daysDemography.size());
				last7DaysDemographic.put("last7DaysDemographic", last7daysDemography);
				last15DaysDemographic.put("stage", "DEMOGRAPHIC");
				last15DaysDemographic.put("last15DaysDemographicCount", last15daysDemography.size());
				last15DaysDemographic.put("last15DaysDemographic", last15daysDemography);
				last30DaysDemographic.put("stage", "DEMOGRAPHIC");
				last30DaysDemographic.put("last30DaysDemographicCount", last30daysDemography.size());
				last30DaysDemographic.put("last30DaysDemographic", last30daysDemography);
				last90DaysDemographic.put("stage", "DEMOGRAPHIC");
				last90DaysDemographic.put("last90DaysDemographicCount", last90daysDemography.size());
				last90DaysDemographic.put("last90DaysDemographic", last90daysDemography);
				tillDaysDemographic.put("stage", "DEMOGRAPHIC");
				tillDaysDemographic.put("tillDaysDemographicCount", tillDateDemography.size());
				tillDaysDemographic.put("tillDaysDemographic", tillDateDemography);

				todayLifestyle.put("stage", "LIFESTYLE");
				todayLifestyle.put("todayLifestyleCount", todayLifeStyle.size());
				todayLifestyle.put("todayLifestyle", todayLifeStyle);
				yesterdayLifestyle.put("stage", "LIFESTYLE");
				yesterdayLifestyle.put("yesterdayLifestyleCount", yesterdayLifeStyle.size());
				yesterdayLifestyle.put("yesterdayLifestyle", yesterdayLifeStyle);
				last7DaysLifestyle.put("stage", "LIFESTYLE");
				last7DaysLifestyle.put("last7DaysLifestyleCount", last7daysLifeStyle.size());
				last7DaysLifestyle.put("last7DaysLifestyle", last7daysLifeStyle);
				last15DaysLifestyle.put("stage", "LIFESTYLE");
				last15DaysLifestyle.put("last15DaysLifestyleCount", last15daysLifeStyle.size());
				last15DaysLifestyle.put("last15DaysLifestyle", last15daysLifeStyle);
				last30DaysLifestyle.put("stage", "LIFESTYLE");
				last30DaysLifestyle.put("last30DaysLifestyleCount", last30daysLifeStyle.size());
				last30DaysLifestyle.put("last30DaysLifestyle", last30daysLifeStyle);
				last90DaysLifestyle.put("stage", "LIFESTYLE");
				last90DaysLifestyle.put("last90DaysLifestyleCount", last90daysLifeStyle.size());
				last90DaysLifestyle.put("last90DaysLifestyle", last90daysLifeStyle);
				tillDaysLifestyle.put("stage", "LIFESTYLE");
				tillDaysLifestyle.put("tillDaysLifestyleCount", tillDateLifeStyle.size());
				tillDaysLifestyle.put("tillDaysLifestyle", tillDateLifeStyle);

				todayDiets.put("stage", "DIET CHOICES");
				todayDiets.put("todayDietsCount", todayDiet.size());
				todayDiets.put("todayDiets", todayDiet);
				yesterdayDiets.put("stage", "DIET CHOICES");
				yesterdayDiets.put("yesterdayDietsCount", yesterdayDiet.size());
				yesterdayDiets.put("yesterdayDiets", yesterdayDiet);
				last7DaysDiets.put("stage", "DIET CHOICES");
				last7DaysDiets.put("last7DaysDietsCount", last7daysDiet.size());
				last7DaysDiets.put("last7DaysDiets", last7daysDiet);
				last15DaysDiets.put("stage", "DIET CHOICES");
				last15DaysDiets.put("last15DaysDietsCount", last15daysDiet.size());
				last15DaysDiets.put("last15DaysDiets", last15daysDiet);
				last30DaysDiets.put("stage", "DIET CHOICES");
				last30DaysDiets.put("last30DaysDietsCount", last30daysDiet.size());
				last30DaysDiets.put("last30DaysDiets", last30daysDiet);
				last90DaysDiets.put("stage", "DIET CHOICES");
				last90DaysDiets.put("last90DaysDietsCount", last90daysDiet.size());
				last90DaysDiets.put("last90DaysDiets", last90daysDiet);
				tillDaysDiets.put("stage", "DIET CHOICES");
				tillDaysDiets.put("tillDaysDietsCount", tillDateDiet.size());
				tillDaysDiets.put("tillDaysDiets", tillDateDiet);

				// Plan Subscription Plan
				client.rxFind("PLAN_SUBCRIPTION_DETAIL", query).map(map -> {
					return map;
				}).subscribe(res1 -> {
					List<JsonObject> todayDonePlan = new ArrayList<JsonObject>();
					List<JsonObject> yesterdayDonePlan = new ArrayList<JsonObject>();
					List<JsonObject> last7daysDonePlan = new ArrayList<JsonObject>();
					List<JsonObject> last15daysDonePlan = new ArrayList<JsonObject>();
					List<JsonObject> last30daysDonePlan = new ArrayList<JsonObject>();
					List<JsonObject> last90daysDonePlan = new ArrayList<JsonObject>();
					List<JsonObject> tillDateDonePlan = new ArrayList<JsonObject>();
					List<JsonObject> todayDonePlanIsExpired = new ArrayList<JsonObject>();
					List<JsonObject> yesterdayDonePlanIsExpired = new ArrayList<JsonObject>();
					List<JsonObject> last7daysDonePlanIsExpired = new ArrayList<JsonObject>();
					List<JsonObject> last15daysDonePlanIsExpired = new ArrayList<JsonObject>();
					List<JsonObject> last30daysDonePlanIsExpired = new ArrayList<JsonObject>();
					List<JsonObject> last90daysDonePlanIsExpired = new ArrayList<JsonObject>();
					List<JsonObject> tillDateDonePlanIsExpired = new ArrayList<JsonObject>();
					List<JsonObject> todayDonePlanIsActive = new ArrayList<JsonObject>();
					List<JsonObject> yesterdayDonePlanIsActive = new ArrayList<JsonObject>();
					List<JsonObject> last7daysDonePlanIsActive = new ArrayList<JsonObject>();
					List<JsonObject> last15daysDonePlanIsActive = new ArrayList<JsonObject>();
					List<JsonObject> last30daysDonePlanIsActive = new ArrayList<JsonObject>();
					List<JsonObject> last90daysDonePlanIsActive = new ArrayList<JsonObject>();
					List<JsonObject> tillDateDonePlanIsActive = new ArrayList<JsonObject>();
					List<Integer> todayTotalAmount = new ArrayList<Integer>();
					List<Integer> yesterdayTotalAmount = new ArrayList<Integer>();
					List<Integer> last7daysTotalAmount = new ArrayList<Integer>();
					List<Integer> last15daysTotalAmount = new ArrayList<Integer>();
					List<Integer> last30daysTotalAmount = new ArrayList<Integer>();
					List<Integer> last90daysTotalAmount = new ArrayList<Integer>();
					List<Integer> tillDateTotalAmount = new ArrayList<Integer>();
					DateFormat dateFormatISO = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
					SimpleDateFormat format = new SimpleDateFormat("ddMMyyyy");
					Calendar cal1 = new GregorianCalendar();
					Calendar cal2 = new GregorianCalendar();
					res1.forEach(action -> {
						JsonObject json = action;
						json.put("stage", "PAID");
						String paymentDoneDateString;
						try {
							paymentDoneDateString = json.getString("createdDate");
							String expiredDateString = json.getString("expiryDate");
							Date expiredDate = format.parse(expiredDateString);
							try {
								Calendar calendar = Calendar.getInstance();
								calendar.setTime(format.parse(expiredDateString));
								json.put("activeTill", format1.format(calendar.getTime()));
							} catch (ParseException e) {
								e.printStackTrace();
							}

							Date createdDate = dateFormatISO.parse(paymentDoneDateString);
							cal1.setTime(createdDate);
							Date currentDate = new Date();
							cal2.setTime(currentDate);
							int noOfDaysPassed = daysPassed(cal1.getTime(), cal2.getTime());
							if (noOfDaysPassed == 0) {
								todayDonePlan.add(json);
								last7daysDonePlan.add(json);
								last15daysDonePlan.add(json);
								last30daysDonePlan.add(json);
								last90daysDonePlan.add(json);
								tillDateDonePlan.add(json);
								todayTotalAmount.add(json.getInteger("amount"));
								if (expiredDate.before(currentDate)) {
									json.put("stage", "PAYMENT PENDING");
									todayDonePlanIsExpired.add(json);
								} else {
									json.put("stage", "PAID");
									todayDonePlanIsActive.add(json);
								}
							} else if (noOfDaysPassed == 1) {
								yesterdayDonePlan.add(action);
								last7daysDonePlan.add(json);
								last15daysDonePlan.add(json);
								last30daysDonePlan.add(json);
								last90daysDonePlan.add(json);
								tillDateDonePlan.add(json);
								yesterdayTotalAmount.add(json.getInteger("amount"));
								if (expiredDate.before(currentDate)) {
									json.put("stage", "PAYMENT PENDING");
									yesterdayDonePlanIsExpired.add(json);
								} else {
									json.put("stage", "PAID");
									yesterdayDonePlanIsActive.add(json);
								}
							} else if (noOfDaysPassed <= 7) {
								last7daysDonePlan.add(json);
								last15daysDonePlan.add(json);
								last30daysDonePlan.add(json);
								last90daysDonePlan.add(json);
								tillDateDonePlan.add(json);
								last7daysTotalAmount.add(json.getInteger("amount"));
								if (expiredDate.before(currentDate)) {
									json.put("stage", "PAYMENT PENDING");
									last7daysDonePlanIsExpired.add(json);
								} else {
									json.put("stage", "PAID");
									last7daysDonePlanIsActive.add(json);
								}
							} else if (noOfDaysPassed <= 15) {
								last15daysDonePlan.add(json);
								last30daysDonePlan.add(json);
								last90daysDonePlan.add(json);
								tillDateDonePlan.add(json);
								last15daysTotalAmount.add(json.getInteger("amount"));
								if (expiredDate.before(currentDate)) {
									json.put("stage", "PAYMENT PENDING");
									last15daysDonePlanIsExpired.add(json);
								} else {
									json.put("stage", "PAID");
									last15daysDonePlanIsActive.add(json);
								}
							} else if (noOfDaysPassed <= 30) {
								last30daysDonePlan.add(json);
								last90daysDonePlan.add(json);
								tillDateDonePlan.add(json);
								last30daysTotalAmount.add(json.getInteger("amount"));
								if (expiredDate.before(currentDate)) {
									json.put("stage", "PAYMENT PENDING");
									last30daysDonePlanIsExpired.add(json);
								} else {
									json.put("stage", "PAID");
									last30daysDonePlanIsActive.add(json);
								}
							} else if (noOfDaysPassed <= 90) {
								last90daysDonePlan.add(json);
								tillDateDonePlan.add(json);
								last90daysTotalAmount.add(json.getInteger("amount"));
								if (expiredDate.before(currentDate)) {
									json.put("stage", "PAYMENT PENDING");
									last90daysDonePlanIsExpired.add(json);
								} else {
									json.put("stage", "PAID");
									last90daysDonePlanIsActive.add(json);
								}
							} else {
								tillDateDonePlan.add(json);
								tillDateTotalAmount.add(json.getInteger("amount"));
								if (expiredDate.before(currentDate)) {
									json.put("stage", "PAYMENT PENDING");
									tillDateDonePlanIsExpired.add(json);
								} else {
									json.put("stage", "PAID");
									tillDateDonePlanIsActive.add(json);
								}
							}
						} catch (ParseException e) {
							logger.error("##### " + method + " PARSEEXCEPTION -->> " + e.getMessage());
							e.printStackTrace();
						}
					});

					JsonObject todayPaymentObj = new JsonObject();
					JsonObject yesterdayPaymentObj = new JsonObject();
					JsonObject last7DaysPaymentObj = new JsonObject();
					JsonObject last15DaysPaymentObj = new JsonObject();
					JsonObject last30DaysPaymentObj = new JsonObject();
					JsonObject last90DaysPaymentObj = new JsonObject();
					JsonObject tillDaysPaymentObj = new JsonObject();

					todayPaymentObj.put("todayPaymentDoneCount", todayDonePlan.size());
					todayPaymentObj.put("todayPaymentDone", todayDonePlan);

					Double todayAmount = todayTotalAmount.stream().mapToDouble(x -> {
						return Double.valueOf(x);
					}).sum();
					todayPaymentObj.put("todayTotalAmount", todayAmount.intValue());
					todayPaymentObj.put("todayPaymentDoneIsPlanIsActiveCount", todayDonePlanIsActive.size());
					todayPaymentObj.put("todayPaymentDoneIsPlanIsActive", todayDonePlanIsActive);
					todayPaymentObj.put("todayPaymentDoneIsPlanIsExpiredCount", todayDonePlanIsExpired.size());
					todayPaymentObj.put("todayPaymentDoneIsPlanIsExpired", todayDonePlanIsExpired);
					yesterdayPaymentObj.put("yesterdayPaymentDoneCount", yesterdayDonePlan.size());
					yesterdayPaymentObj.put("yesterdayPaymentDone", yesterdayDonePlan);

					Double yesterdayAmount = yesterdayTotalAmount.stream().mapToDouble(x -> {
						return Double.valueOf(x);
					}).sum();
					yesterdayPaymentObj.put("yesterdayTotalAmount", yesterdayAmount.intValue());
					yesterdayPaymentObj.put("yesterdayPaymentDoneIsPlanIsActiveCount",
							yesterdayDonePlanIsActive.size());
					yesterdayPaymentObj.put("yesterdayPaymentDoneIsPlanIsActive", yesterdayDonePlanIsActive);
					yesterdayPaymentObj.put("yesterdayPaymentDoneIsPlanIsExpiredCount",
							yesterdayDonePlanIsExpired.size());
					yesterdayPaymentObj.put("yesterdayPaymentDoneIsPlanIsExpired", yesterdayDonePlanIsExpired);
					last7DaysPaymentObj.put("last7DaysPaymentDoneCount", last7daysDonePlan.size());
					last7DaysPaymentObj.put("last7DaysPaymentDone", last7daysDonePlan);

					Double last7daysAmount = last7daysTotalAmount.stream().mapToDouble(x -> {
						return Double.valueOf(x);
					}).sum();
					last7DaysPaymentObj.put("last7DaysTotalAmount", last7daysAmount.intValue());
					last7DaysPaymentObj.put("last7DaysPaymentDoneIsPlanIsActiveCount",
							last7daysDonePlanIsActive.size());
					last7DaysPaymentObj.put("last7DaysPaymentDoneIsPlanIsActive", last7daysDonePlanIsActive);
					last7DaysPaymentObj.put("last7DaysPaymentDoneIsPlanIsExpiredCount",
							last7daysDonePlanIsExpired.size());
					last7DaysPaymentObj.put("last7DaysPaymentDoneIsPlanIsExpired", last7daysDonePlanIsExpired);
					last15DaysPaymentObj.put("last15DaysPaymentDoneCount", last15daysDonePlan.size());
					last15DaysPaymentObj.put("last15DaysPaymentDone", last15daysDonePlan);

					Double last15daysAmount = last15daysTotalAmount.stream().mapToDouble(x -> {
						return Double.valueOf(x);
					}).sum();
					last15DaysPaymentObj.put("last15DaysTotalAmount", last15daysAmount.intValue());
					last15DaysPaymentObj.put("last15DaysPaymentDoneIsPlanIsActiveCount",
							last15daysDonePlanIsActive.size());
					last15DaysPaymentObj.put("last15DaysPaymentDoneIsPlanIsActive", last15daysDonePlanIsActive);
					last15DaysPaymentObj.put("last15DaysPaymentDoneIsPlanIsExpiredCount",
							last15daysDonePlanIsExpired.size());
					last15DaysPaymentObj.put("last15DaysPaymentDoneIsPlanIsExpired", last15daysDonePlanIsExpired);
					last30DaysPaymentObj.put("last30DaysPaymentDoneCount", last30daysDonePlan.size());
					last30DaysPaymentObj.put("last30DaysPaymentDone", last30daysDonePlan);

					Double last30daysAmount = last30daysTotalAmount.stream().mapToDouble(x -> {
						return Double.valueOf(x);
					}).sum();
					last30DaysPaymentObj.put("last30daysTotalAmount", last30daysAmount.intValue());
					last30DaysPaymentObj.put("last30DaysPaymentDoneIsPlanIsActiveCount",
							last30daysDonePlanIsActive.size());
					last30DaysPaymentObj.put("last30DaysPaymentDoneIsPlanIsActive", last30daysDonePlanIsActive);
					last30DaysPaymentObj.put("last30DaysPaymentDoneIsPlanIsExpiredCount",
							last30daysDonePlanIsExpired.size());
					last30DaysPaymentObj.put("last30DaysPaymentDoneIsPlanIsExpired", last30daysDonePlanIsExpired);
					last90DaysPaymentObj.put("last90DaysPaymentDoneCount", last90daysDonePlan.size());
					last90DaysPaymentObj.put("last90DaysPaymentDone", last90daysDonePlan);

					Double last90daysAmount = last90daysTotalAmount.stream().mapToDouble(x -> {
						return Double.valueOf(x);
					}).sum();
					last90DaysPaymentObj.put("last90DaysTotalAmount", last90daysAmount.intValue());
					last90DaysPaymentObj.put("last90DaysPaymentDoneIsPlanIsActiveCount",
							last90daysDonePlanIsActive.size());
					last90DaysPaymentObj.put("last90DaysPaymentDoneIsPlanIsActive", last90daysDonePlanIsActive);
					last90DaysPaymentObj.put("last90DaysPaymentDoneIsPlanIsExpiredCount",
							last90daysDonePlanIsExpired.size());
					last90DaysPaymentObj.put("last90DaysPaymentDoneIsPlanIsExpired", last90daysDonePlanIsExpired);
					tillDaysPaymentObj.put("tillDaysDietsCount", tillDateDonePlan.size());
					tillDaysPaymentObj.put("tillDaysDiets", tillDateDonePlan);
					Double tillDateAmount = tillDateTotalAmount.stream().mapToDouble(x -> {
						return Double.valueOf(x);
					}).sum();
					tillDaysPaymentObj.put("tillDateTotalAmount", tillDateAmount.intValue());
					tillDaysPaymentObj.put("tillPaymentDoneIsPlanIsActiveCount", tillDateDonePlanIsActive.size());
					tillDaysPaymentObj.put("tillPaymentDoneIsPlanIsActive", tillDateDonePlanIsActive);
					tillDaysPaymentObj.put("tillDaysPaymentDoneIsPlanIsExpiredCount", tillDateDonePlanIsExpired.size());
					tillDaysPaymentObj.put("tillDaysPaymentDoneIsPlanIsExpired", tillDateDonePlanIsExpired);

					today.put("demographic", todayDemographic);
					today.put("lifeStyle", todayLifestyle);
					today.put("diet", todayDiets);
					today.put("payment", todayPaymentObj);

					yesterday.put("demographic", yesterdayDemographic);
					yesterday.put("lifeStyle", yesterdayLifestyle);
					yesterday.put("diet", yesterdayDiets);
					yesterday.put("payment", yesterdayPaymentObj);

					last7Days.put("demographic", last7DaysDemographic);
					last7Days.put("lifeStyle", last7DaysLifestyle);
					last7Days.put("diet", last7DaysDiets);
					last7Days.put("payment", last7DaysPaymentObj);

					last15Days.put("demographic", last15DaysDemographic);
					last15Days.put("lifeStyle", last15DaysLifestyle);
					last15Days.put("diet", last15DaysDiets);
					last15Days.put("payment", last15DaysPaymentObj);

					last30Days.put("demographic", last30DaysDemographic);
					last30Days.put("lifeStyle", last30DaysLifestyle);
					last30Days.put("diet", last30DaysDiets);
					last30Days.put("payment", last30DaysPaymentObj);

					last90Days.put("demographic", last90DaysDemographic);
					last90Days.put("lifeStyle", last90DaysLifestyle);
					last90Days.put("diet", last90DaysDiets);
					last90Days.put("payment", last90DaysPaymentObj);

					tillDays.put("demographic", tillDaysDemographic);
					tillDays.put("lifeStyle", tillDaysLifestyle);
					tillDays.put("diet", tillDaysDiets);
					tillDays.put("payment", tillDaysPaymentObj);

					response.put("today", today);
					response.put("yesterday", yesterday);
					response.put("last7Days", last7Days);
					response.put("last15days", last15Days);
					response.put("last30Days", last30Days);
					response.put("last90Days", last90Days);
					response.put("tillDateDays", tillDays);

					promise.complete(response);
				}, (ex) -> {
					logger.error("##### " + method + " ERROR 1 -->> " + ex.getMessage());
					promise.fail(ex.getMessage());
				});
			}, (ex) -> {
				logger.error("##### " + method + " ERROR 2 -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		} catch (Exception e) {
			logger.error("##### " + method + " EXCEPTION -->> " + e.getMessage());
			e.printStackTrace();
		}
		return promise.future();
	}

	/**
	 * Add Terms and conditions.
	 * 
	 * @param payload
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> addTnC(JsonObject payload, String traceId) {
		String method = "MongoRepositoryWrapper addTnC() " + traceId;
		SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss a");
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		payload.put("createdDate", format.format(calendar.getTime()));
		Promise<JsonObject> promise = Promise.promise();
		client.rxSave("CUST_TNC", payload).subscribe(res -> {
			JsonObject jsonObject = new JsonObject();
			jsonObject.put("code", "0000");
			jsonObject.put("message", "success");
			jsonObject.put("response", res);
			promise.complete(payload);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Remove preferences.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> removePreferences(String email, String traceId) {
		String method = "MongoRepositoryWrapper removePreferences() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();

		JsonObject query = new JsonObject();
		query.put("_id", email);
		client.rxRemoveDocument("CUST_DIET_PREF_V2", query).subscribe(res -> {
			promise.complete(new JsonObject().put("code", "0000").put("message", "CUSTOMER PREFERENCES REMOVED"));
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.complete(new JsonObject().put("code", "0001").put("message", ex.getMessage()));

		});

		return promise.future();
	}

	/**
	 * Remove plan subscription detail.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> removePlanSubscriptionDetail(String email, String traceId) {
		String method = "MongoRepositoryWrapper removePlanSubscriptionDetail() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();

		JsonObject query = new JsonObject();
		query.put("emailId", email);
		client.rxRemoveDocument("PLAN_SUBCRIPTION_DETAIL", query).subscribe(res -> {
			promise.complete(
					new JsonObject().put("code", "0000").put("message", "CUSTOMER PLAN SUBSCRIPTION DETAIL REMOVED"));
		}, (ex) -> {
			logger.error("##### " + method + " EMAIL -->> " + ex.getMessage());
			promise.complete(new JsonObject().put("code", "0001").put("message", ex.getMessage()));

		});

		return promise.future();
	}

	/**
	 * Remove payment details.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> removePaymentDetail(String email, String traceId) {
		String method = "MongoRepositoryWrapper removePaymentDetail() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("emailId", email);
		client.rxRemoveDocuments("PAYMENT_DETAIL", query).subscribe(res -> {
			promise.complete(new JsonObject().put("code", "0000").put("message", "CUSTOMER PAYMENT DETAIL REMOVED"));
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.complete(new JsonObject().put("code", "0001").put("message", ex.getMessage()));

		});

		return promise.future();
	}

	/**
	 * Remove customer habit details.
	 * 
	 * @param email
	 * @param traceId
	 * @return
	 */
	public Future<JsonObject> removeCustomeHabitDetail(String email, String traceId) {
		String method = "MongoRepositoryWrapper removeCustomeHabitDetail() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("email", email);
		client.rxRemoveDocuments("CUST_HABIT_DETAIL_V2", query).subscribe(res -> {
			promise.complete(new JsonObject().put("code", "0000").put("message", "CUSTOMER HABIT DETAIL REMOVED"));
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.complete(new JsonObject().put("code", "0001").put("message", ex.getMessage()));

		});

		return promise.future();
	}

	/**
	 * Remove customer habit followed.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> removeCustomeHabitFollow(String email, String traceId) {
		String method = "MongoRepositoryWrapper removeCustomeHabitFollow() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("email", email);
		client.rxRemoveDocuments("CUST_HABIT_FOLLOW", query).subscribe(res -> {
			promise.complete(new JsonObject().put("code", "0000").put("message", "CUSTOMER HABIT FOLLOWED REMOVED"));
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.complete(new JsonObject().put("code", "0001").put("message", ex.getMessage()));

		});

		return promise.future();
	}

	/**
	 * Remove customer daily weight.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> removeCustomeDailyWeight(String email, String traceId) {
		String method = "MongoRepositoryWrapper removeCustomeDailyWeight() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("email", email);
		client.rxRemoveDocuments("CUSTOMER_DAILY_WEIGHT", query).subscribe(res -> {
			promise.complete(new JsonObject().put("code", "0000").put("message", "CUSTOMER DALIY WEIGHT REMOVED"));
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.complete(new JsonObject().put("code", "0001").put("message", ex.getMessage()));

		});

		return promise.future();
	}

	/**
	 * Remove customer daily diet cache.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> removeCustomeDailyDietCache(String email, String traceId) {
		String method = "MongoRepositoryWrapper removeCustomeDailyDietCache() " + traceId + "-[" + email + "]";

		Promise<JsonObject> promise = Promise.promise();

		JsonObject query = new JsonObject();
		query.put("_id.email", email);
		client.rxRemoveDocuments("CUST_DAILY_DIET", query).subscribe(res -> {
			promise.complete(new JsonObject().put("code", "0000").put("message", "CUSTOMER DAILY DIET CACHE REMOVED"));
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.complete(new JsonObject().put("code", "0001").put("message", ex.getMessage()));

		});

		return promise.future();
	}

	/**
	 * Remove customer daily diet cache.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> removeCustomeDailyDietCacheDetox(String email, String traceId) {
		String method = "MongoRepositoryWrapper removeCustomeDailyDietCacheDetox() " + traceId + "-[" + email + "]";

		Promise<JsonObject> promise = Promise.promise();

		JsonObject query = new JsonObject();
		query.put("_id.email", email);
		client.rxRemoveDocuments("CUST_DAILY_DIET_DETOX", query).subscribe(res -> {
			promise.complete(
					new JsonObject().put("code", "0000").put("message", "CUSTOMER DAILY DIET CACHE DETOX REMOVED"));
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.complete(new JsonObject().put("code", "0001").put("message", ex.getMessage()));

		});

		return promise.future();
	}

	/**
	 * Remove customer diet prefeference V2.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> removeCustomerDietPreV2(String email, String traceId) {
		String method = "MongoRepositoryWrapper removeCustomerDietPreV2() " + traceId + "-[" + email + "]";

		Promise<JsonObject> promise = Promise.promise();

		JsonObject query = new JsonObject();
		query.put("_id.email", email);
		client.rxRemoveDocuments("CUST_DIET_PREF_V2", query).subscribe(res -> {
			promise.complete(
					new JsonObject().put("code", "0000").put("message", "CUSTOMER DIET PREFERENCE V2 REMOVED"));
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.complete(new JsonObject().put("code", "0001").put("message", ex.getMessage()));

		});

		return promise.future();
	}

	/**
	 * Remove customer login date-time.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> removeCustomerLoginDateTime(String email, String traceId) {
		String method = "MongoRepositoryWrapper removeCustomerLoginDateTime() " + traceId + "-[" + email + "]";

		Promise<JsonObject> promise = Promise.promise();

		JsonObject query = new JsonObject();
		query.put("emailId", email);
		client.rxRemoveDocuments("CUST_LOGIN_DATETIME", query).subscribe(res -> {
			promise.complete(new JsonObject().put("code", "0000").put("message", "CUSTOMER LOGIN DATE/TIME REMOVED"));
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.complete(new JsonObject().put("code", "0001").put("message", ex.getMessage()));

		});

		return promise.future();
	}

	/**
	 * Remive customer water reminder.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> removeCustomerWaterReminderStatus(String email, String traceId) {
		String method = "MongoRepositoryWrapper removeCustomerWaterReminderStatus() " + traceId + "-[" + email + "]";

		Promise<JsonObject> promise = Promise.promise();

		JsonObject query = new JsonObject();
		query.put("emailId", email);
		client.rxRemoveDocuments("WATER_REMINDER_STATUS", query).subscribe(res -> {
			promise.complete(
					new JsonObject().put("code", "0000").put("message", "CUSTOMER WATER REMINDER STATUS REMOVED"));
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.complete(new JsonObject().put("code", "0001").put("message", ex.getMessage()));

		});

		return promise.future();
	}

	/**
	 * Remove customer water drank.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> removeCustomerWaterDrank(String email, String traceId) {
		String method = "MongoRepositoryWrapper removeCustomerWaterDrank() " + traceId + "-[" + email + "]";

		Promise<JsonObject> promise = Promise.promise();

		JsonObject query = new JsonObject();
		query.put("emailId", email);
		client.rxRemoveDocuments("CUST_WATER_DRANK", query).subscribe(res -> {
			promise.complete(new JsonObject().put("code", "0000").put("message", "CUSTOMER WATER DRANK REMOVED"));
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.complete(new JsonObject().put("code", "0001").put("message", ex.getMessage()));

		});

		return promise.future();
	}

	/**
	 * Remove customer calories burnt.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> removeCustomerCaloriesBurnt(String email, String traceId) {
		String method = "MongoRepositoryWrapper removeCustomerCaloriesBurnt() " + traceId + "-[" + email + "]";

		Promise<JsonObject> promise = Promise.promise();

		JsonObject query = new JsonObject();
		query.put("emailId", email);
		client.rxRemoveDocuments("CUST_CALORIES_BURNT", query).subscribe(res -> {
			promise.complete(new JsonObject().put("code", "0000").put("message", "CUSTOMER CALORIES BURNT REMOVED"));
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.complete(new JsonObject().put("code", "0001").put("message", ex.getMessage()));

		});

		return promise.future();
	}

	/**
	 * Remove analytics.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> removeAnalytics(String email, String traceId) {
		String method = "MongoRepositoryWrapper removeAnalytics() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();

		JsonObject query = new JsonObject();
		query.put("emailId", email);
		client.rxRemoveDocuments("CUSTOMER_ANALYTICS", query).subscribe(res -> {
			promise.complete(new JsonObject().put("code", "0000").put("message", "CUSTOMER ANALYTICS REMOVED"));
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.complete(new JsonObject().put("code", "0001").put("message", ex.getMessage()));

		});

		return promise.future();
	}

	/**
	 * Remove customer mail sent status.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> removeCustMailSentStatus(String email, String traceId) {
		String method = "MongoRepositoryWrapper removeCustMailSentStatus() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();

		JsonObject query = new JsonObject();
		query.put("_id", new JsonObject().put("email", email));
		client.rxRemoveDocuments("CUST_MAIL_SENT_STATUS", query).subscribe(res -> {
			promise.complete(new JsonObject().put("code", "0000").put("message", "CUSTOMER MAIL SENT STATAUS REMOVED"));
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.complete(new JsonObject().put("code", "0001").put("message", ex.getMessage()));

		});

		return promise.future();
	}

	/**
	 * Remove customer profile.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> removeProfile(String email, String traceId) {
		String method = "MongoRepositoryWrapper removeProfile() " + traceId + "-[" + email + "]";

		Promise<JsonObject> promise = Promise.promise();

		JsonObject query = new JsonObject();
		query.put("_id", email);
		client.rxRemoveDocument("CUST_PROFILE", query).subscribe(res -> {
			logger.debug("##### " + method + " CUSTOMER PROFILE REMOVED.");
			promise.complete(new JsonObject().put("code", "0000").put("message", "PROFILE REMOVED [" + email + "]"));
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.complete(new JsonObject().put("code", "0001").put("message", ex.getMessage()));

		});

		return promise.future();
	}

	/**
	 * Remove customer video food items.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> removeCustVideoFoodItems(String email, String traceId) {
		String method = "MongoRepositoryWrapper removeCustVideoFoodItems() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();

		JsonObject query = new JsonObject();
		query.put("_id.email", email);
		client.rxRemoveDocuments("CUST_VIDEO_FOODITEMS", query).subscribe(res -> {
			logger.info("##### " + method + " FINALLY CUSTOMER VIDEO FOODITEMS REMOVED.");
			promise.complete(
					new JsonObject().put("code", "0000").put("message", "FINALLY CUSTOMER VIDEO FOODITEMS REMOVED"));
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.complete(new JsonObject().put("code", "0001").put("message", ex.getMessage()));

		});

		return promise.future();
	}

	/**
	 * Get coupon details by coupon code.
	 * 
	 * @param email
	 * @param couponCode
	 * @param traceId
	 * @return Future<DiscountedCouponDetails>
	 */
	public Future<DiscountedCouponDetails> getCouponDetailsByCouponCode(String email, String couponCode,
			String traceId) {
		String method = "MongoRepositoryWrapper getCouponDetailsByCouponCode() " + traceId + "-[" + email + "]";
		Promise<DiscountedCouponDetails> promise = Promise.promise();
		DiscountedCouponDetails discountedCouponDetails = new DiscountedCouponDetails();
		discountedCouponDetails.setEmailId(email);
		JsonObject query = new JsonObject().put("_id", couponCode).put("isCouponActive", true);
		// Discounted Plan Subscription Detail
		client.rxFind("DISCOUNTED_PLAN_SUBCRIPTION_DETAIL", query).map(map -> {
			return map;
		}).subscribe(res -> {
			res.forEach(action -> {
				try {
					Date expiryDateTime = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss")
							.parse(action.getString("couponExpiryDateTime"));
					String currentDateTimeInString = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss")
							.format(Calendar.getInstance().getTime());
					Date currentDateTime = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").parse(currentDateTimeInString);
					if (!expiryDateTime.before(currentDateTime)) {
						discountedCouponDetails.setUniqueCouponCode(couponCode);
						discountedCouponDetails.setValidityInDays(action.getInteger("validityInDays"));
						discountedCouponDetails.setIsCouponActive(action.getBoolean("isCouponActive"));
						discountedCouponDetails.setGeneratedBy(action.getString("generatedBy"));
						discountedCouponDetails.setConsumedBy(email);
						discountedCouponDetails
								.setCouponExpiryDateTime(action.getString("couponExpiryDateTime").split(" ")[0]);
						discountedCouponDetails.setCreatedDateTime(action.getString("createdDateTime"));
						discountedCouponDetails.setConsumedDateTime(currentDateTimeInString);
						logger.info("##### " + method + " DISCOUNTED COUPON DETAILS -->> "
								+ discountedCouponDetails.toString());
					}
				} catch (ParseException e) {
					logger.error("##### " + method + " EXCEPTION -->> " + e.getMessage());
					e.printStackTrace();
				}
			});

			promise.complete(discountedCouponDetails);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			discountedCouponDetails.setErrorMessage(ex.getMessage());
			promise.fail(ex.getMessage());

		});

		return promise.future();
	}

	/**
	 * Subscribe plan for discounted coupon.
	 * 
	 * @param request
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<DiscountedCouponDetails> subcribePlanForDiscountedCoupon(DiscountedCouponDetails request,
			String traceId) {
		String method = "MongoRepositoryWrapper subcribePlanForDiscountedCoupon() " + traceId + "-["
				+ request.getEmailId() + "]";
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
		Calendar calll = Calendar.getInstance(); // creates calendar
		calll.setTime(new Date()); // sets calendar time/date
		calll.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		calll.add(Calendar.MINUTE, 30); // add 30 minutes
		String currentDate = dateFormat.format(calll.getTime());
		logger.info("##### " + method + " DICOUNTED DEATILS -->> " + request.toString());
		Promise<DiscountedCouponDetails> promise = Promise.promise();
		try {
			JsonObject query = new JsonObject();
			query.put("emailId", request.getEmailId());
			query.put("orderId", "NA");
			query.put("couponCode", request.getUniqueCouponCode());
			query.put("createdDate", currentDate);
			query.put("amount", 0);
			query.put("txnId", "NA");
			query.put("isActive", true);
			query.put("expiryDate", ApiUtils.getExpiryDate(this.config.getInteger("discountedCouponValidity")));
			client.rxSave("PLAN_SUBCRIPTION_DETAIL", query).subscribe(res -> {
				promise.complete(request);
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
		return promise.future();
	}

	/**
	 * Subscribe plan by coupon.
	 * 
	 * @param discountedCouponDetails
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> subscribePlanByCoupon(DiscountedCouponDetails discountedCouponDetails, String traceId) {
		String method = "MongoRepositoryWrapper subscribePlanByCoupon() " + traceId;
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", discountedCouponDetails.getUniqueCouponCode())
				.put("isCouponActive", true);
		JsonObject update = new JsonObject();
		update.put("$set",
				new JsonObject().put("isCouponActive", false)
						.put("uniqueCouponCode", discountedCouponDetails.getUniqueCouponCode())
						.put("validityInDays", discountedCouponDetails.getValidityInDays())
						.put("consumedBy", discountedCouponDetails.getGeneratedBy())
						.put("consumedDateTime", discountedCouponDetails.getConsumedDateTime())
						.put("generatedBy", discountedCouponDetails.getGeneratedBy())
						.put("consumedBy", discountedCouponDetails.getConsumedBy())
						.put("couponExpiryDateTime", discountedCouponDetails.getCouponExpiryDateTime())
						.put("createdDateTime", discountedCouponDetails.getCreatedDateTime())
						.put("consumedDateTime", discountedCouponDetails.getConsumedDateTime()));
		logger.info("##### " + method + " UPDATED DETAILS -->> " + update);
		client.rxUpdateCollection("DISCOUNTED_PLAN_SUBCRIPTION_DETAIL", query, update).subscribe(res -> {
			logger.info("##### " + method + " PLAN SUBSCRIBED FOR [" + discountedCouponDetails.getValidityInDays()
					+ "] Days. -->> " + update);
			promise.complete(new JsonObject().put("code", "0000").put("message",
					"Plan Subscribed for [" + discountedCouponDetails.getValidityInDays() + "] Days."));
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.complete(new JsonObject().put("code", "0001").put("message", ex.getMessage()));

		});

		return promise.future();
	}

	/**
	 * Save customer calories burnt.
	 * 
	 * @param email
	 * @param calBurnt
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> saveCustCaloriesBurnt(String email, Double calBurnt, String traceId) {
		String method = "MongoRepositoryWrapper saveCustCaloriesBurnt() " + traceId + "-[" + email + "]";
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.UK);
		Calendar cal = Calendar.getInstance(); // creates calendar
		cal.setTime(new Date()); // sets calendar time/date
		cal.add(Calendar.HOUR_OF_DAY, 10); // adds five hour //don't knopw where this server is
		cal.add(Calendar.MINUTE, 30); // add 30 minutes

		String currentDate = dateFormat.format(cal.getTime());
		Promise<JsonObject> promise = Promise.promise();
		JsonObject payload = new JsonObject();
		payload.put("emailId", email);
		payload.put("caloriesBurnt", calBurnt);
		payload.put("date", currentDate);
		logger.info("##### " + method + " CUSTOMER BURNT CALORIES PAYLOAD -->> " + payload);
		client.rxSave("CUST_CALORIES_BURNT", payload).subscribe(res -> {
			JsonObject response = new JsonObject();
			response.put("code", "0000");
			response.put("message", "Customer Burnt Calories Saved Successfully.");
			promise.complete(response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Water tips.
	 * 
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> waterTips(String traceId) {
		String method = "MongoRepositoryWrapper waterTips() " + traceId;
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject();
		client.rxFind("WATER_TIPS", query).map(map -> {
			return map;
		}).subscribe(res -> {
			JsonObject response = new JsonObject();
			response.put("code", "0000");
			response.put("tips", res);
			promise.complete(response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Save water drank.
	 * 
	 * @param email
	 * @param waterQuantity
	 * @param dateTime
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> saveWaterDrank(String email, Integer waterQuantity, String dateTime, String traceId) {
		String method = "MongoRepositoryWrapper saveWaterDrank() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.UK);
		Calendar cal = Calendar.getInstance(); // creates calendar
		cal.setTime(new Date()); // sets calendar time/date
		cal.add(Calendar.HOUR_OF_DAY, 10); // adds five hour - don't know where this server is
		cal.add(Calendar.MINUTE, 30); // add 30 minutes

		String currentDate = dateFormat.format(cal.getTime());
		Date waterDrankDate;
		String waterDrankDateTime = "";
		try {
			waterDrankDate = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.ENGLISH)
					.parse(new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss")
							.format(new SimpleDateFormat("ddMMyyyyHHmmss").parse(dateTime)));
			Calendar cal1 = Calendar.getInstance(); // creates calendar
			cal1.setTime(waterDrankDate); // sets calendar time/date
			cal1.add(Calendar.HOUR_OF_DAY, 10); // adds five hour - don't know where this server is
			cal1.add(Calendar.MINUTE, 30); // add 30 minutes
			waterDrankDateTime = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").format(waterDrankDate);
			JsonObject payload = new JsonObject();
			payload.put("emailId", email);
			payload.put("waterDrankQuantity", waterQuantity);
			payload.put("waterDrankDateTime", waterDrankDateTime);
			payload.put("createdDate", currentDate);
			logger.info("##### " + method + " WATER DRANK PAYLOAD -->> " + payload);
			client.rxSave("CUST_WATER_DRANK", payload).subscribe(res -> {
				JsonObject response = new JsonObject();
				response.put("code", "0000");
				response.put("message", "Customer Water Drank Saved Successfully.");
				promise.complete(response);
			}, (ex) -> {
				logger.error("##### " + method + " ERROR 1 -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		} catch (ParseException e) {
			e.printStackTrace();
		}

		return promise.future();
	}

	/**
	 * Find water drank.
	 * 
	 * @param email
	 * @param json
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> findWaterDrank(String email, JsonObject json, String traceId) {
		String method = "MongoRepositoryWrapper findWaterDrank() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		boolean isCustomerProfileFound = true;
		if ("0001".equals(json.getString("code")))
			isCustomerProfileFound = false;
		if (isCustomerProfileFound) {
			JsonObject query = new JsonObject();
			Calendar calll = Calendar.getInstance(); // creates calendar
			calll.setTime(new Date()); // sets calendar time/date
			calll.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
			calll.add(Calendar.MINUTE, 30); // add 30 minutes
			String currentDateTime = new SimpleDateFormat("yyyy-MM-dd").format(calll.getTime());
			query.put("emailId", email);
			query.put("createdDate", new JsonObject().put("$gte", currentDateTime));
			logger.info("##### " + method + " QUERY -->> " + query);
			client.rxFind("CUST_WATER_DRANK", query).map(map -> {
				return map;
			}).subscribe(res -> {
				int total = 0;
				if (res == null || res.isEmpty()) {
					logger.info("##### " + method + " NO RECORD FOUND");
					json.put("res", res);
				} else {
					total = res.size();
					JsonArray resArr = new JsonArray();
					res.forEach(action -> {
						action.remove("_id");
						action.remove("createdDate");
						resArr.add(action);
					});
					json.put("res", resArr);
				}

				json.put("total", total);
				logger.info("##### " + method + " JSON -->> " + json);
				promise.complete(json);
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		} else {
			promise.complete(json);
		}

		return promise.future();
	}

	/**
	 * Get total water drank till date.
	 * 
	 * @param email
	 * @param json
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> findTotalWaterDrank(String email, JsonObject json, String traceId) {
		String method = "MongoRepositoryWrapper findTotalWaterDrank() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + " EMAIL -->> " + email);
		Promise<JsonObject> promise = Promise.promise();

		promise.complete(json);
		return promise.future();
	}

	/**
	 * Water recommendations.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> waterRecommendation(String email, String traceId) {
		String method = "MongoRepositoryWrapper waterRecommendation() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);
		logger.info("##### " + method + " QUERY -->> " + query);
		JsonObject json = new JsonObject();
		client.rxFindOne("CUST_PROFILE", query, null).subscribe(res -> {
			json.put("code", "0001");
			json.put("message", "Customer profile not found.");
			if (res == null || res.isEmpty()) {
				logger.info("##### " + method + " CUSTOMER PROFILE IS UNAVAILABLE");
			} else {
				Integer weight = res.getJsonObject("demographic").getJsonObject("weight").getInteger("value");
				json.put("code", "0000");
				json.put("message", "success");
				if (weight >= 50 && weight <= 60)
					json.put("recommendedWater", "11 Glasses");
				else if (weight > 60 && weight <= 70)
					json.put("recommendedWater", "12 Glasses");
				else if (weight > 70 && weight <= 80)
					json.put("recommendedWater", "13 Glasses");
				else
					json.put("recommendedWater", "14 Glasses");
				json.put("weight", weight + " Kg");
			}

			promise.complete(json);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Fetch water reminder.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> fetchWaterReminder(String email, String traceId) {
		String method = "MongoRepositoryWrapper fetchWaterReminder() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		Calendar cal = Calendar.getInstance(); // creates calendar
		cal.setTime(new Date()); // sets calendar time/date
		cal.add(Calendar.HOUR_OF_DAY, 10); // adds five hour - don't know where this server is
		cal.add(Calendar.MINUTE, 30); // add 30 minutes

		JsonObject query = new JsonObject().put("emailId", email);
		JsonObject json = new JsonObject();
		client.rxFindOne("WATER_REMINDER_STATUS", query, null).subscribe(res -> {
			if (res == null || res.isEmpty()) {
				json.put("isRecordFound", false);
			} else {
				json.put("isRecordFound", true);
				json.put("waterReminderStatus",
						("A".equalsIgnoreCase(res.getString("waterReminderStatus")) ? "Active" : "DeActive"));
				json.put("createdDate", res.getString("createdDate"));
			}
			promise.complete(json);
			logger.info("##### " + method + " JSON -->> " + json);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Format water reminder.
	 * 
	 * @param email
	 * @param data
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> formatWaterReminder(String email, JsonObject data, String traceId) {
		Promise<JsonObject> promise = Promise.promise();
		data.remove("isRecordFound");
		data.remove("createdDate");

		promise.complete(data);
		return promise.future();
	}

	/**
	 * Save water reminder.
	 * 
	 * @param email
	 * @param waterReminderStatus
	 * @param json
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> saveUpdateWaterReminder(String email, String waterReminderStatus, JsonObject json,
			String traceId) {
		String method = "MongoRepositoryWrapper saveWaterReminder() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.UK);
		Calendar cal = Calendar.getInstance(); // creates calendar
		cal.setTime(new Date()); // sets calendar time/date
		cal.add(Calendar.HOUR_OF_DAY, 10); // adds five hour - don't know where this server is
		cal.add(Calendar.MINUTE, 30); // add 30 minutes

		String currentDate = dateFormat.format(cal.getTime());
		boolean isRecordFound = json.getBoolean("isRecordFound");
		if (!isRecordFound) {
			JsonObject payload = new JsonObject();
			payload.put("emailId", email);
			payload.put("waterReminderStatus", waterReminderStatus);
			payload.put("updatedDate", currentDate);
			payload.put("createdDate", currentDate);
			logger.debug("##### " + method + " PAYLOAD (SAVE) -->> " + payload);
			client.rxSave("WATER_REMINDER_STATUS", payload).subscribe(res -> {
				JsonObject response = new JsonObject();
				response.put("code", "0000");
				response.put("message",
						"Customer Water Reminder is "
								+ ("A".equalsIgnoreCase(waterReminderStatus) ? "Activated" : "De-Activated")
								+ " Successfully.");
				promise.complete(response);
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		} else {
			JsonObject payload = new JsonObject();
			payload.put("$set",
					new JsonObject().put("waterReminderStatus", waterReminderStatus).put("updatedDate", currentDate));

			JsonObject query = new JsonObject().put("emailId", email);
			logger.debug("##### " + method + " PAYLOAD (UPDATE) -->> " + payload);
			client.rxUpdateCollection("WATER_REMINDER_STATUS", query, payload).subscribe(res -> {
				JsonObject response = new JsonObject();
				response.put("code", "0000");
				response.put("message", "Customer Water Reminder is updated Successfully as ["
						+ ("A".equalsIgnoreCase(waterReminderStatus) ? "Active" : "DeActive") + "]");
				promise.complete(response);
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		}

		return promise.future();
	}

	/**
	 * Customer Diet Preferences.
	 * 
	 * @param query
	 * @param profile
	 * @return Future<JsonObject>
	 */
	public Future<FilterData> getSelectiveItemsByCommunity(String email, FilterData profile, String traceId) {
		String method = "MongoRepositoryWrapper getSelectiveItemsByCommunity() " + traceId + "-[" + email + "]";
		Promise<FilterData> promise = Promise.promise();
		if (null != profile && null == profile.getCommunity()) {
			promise.complete(profile);
		} else {
			logger.debug("##### " + method + " PROFILE -->> " + profile.toString());
			List<String> community = new ArrayList<>();
			for (String str : profile.getCommunity())
				if (!"U".equalsIgnoreCase(str)) {
					community.add(str);
					break;
				}

			JsonObject query = new JsonObject();
			query.put("community", community.get(0));
			client.rxFindOne("CUST_SELECTIVE_ITEMS", query, null).subscribe(res -> {
				if (res == null || res.isEmpty()) {
					promise.fail("Record not found");
				} else {
					String foodType = profile.getFoodType();
					SelectiveItems selectiveItems = new SelectiveItems();
					selectiveItems.setCommunity(res.getString("community"));
					selectiveItems.setFoodType(profile.getFoodType());
					DietItems dietItems = new DietItems();
					List<String> drinks = (List<String>) res.getJsonObject("data").getJsonArray("drinks").stream()
							.map(json -> json.toString()).collect(Collectors.toList());
					dietItems.setDrinks(drinks);
					List<String> fruits = (List<String>) res.getJsonObject("data").getJsonArray("fruits").stream()
							.map(json -> json.toString()).collect(Collectors.toList());
					dietItems.setFruits(fruits);
					List<String> pulsesOrCurries = (List<String>) res.getJsonObject("data")
							.getJsonArray("pulsesOrCurries").stream().map(json -> json.toString())
							.collect(Collectors.toList());
					List<String> fullPulsesOrCurries = new ArrayList<>();
					if ("NV".equalsIgnoreCase(foodType)) {
						List<String> pulsesOrCurriesWithNV = (List<String>) res.getJsonObject("data")
								.getJsonArray("pulsesOrCurriesWithNV").stream().map(json -> json.toString())
								.collect(Collectors.toList());
						fullPulsesOrCurries.addAll(pulsesOrCurriesWithNV);
						fullPulsesOrCurries.addAll(pulsesOrCurries);
						dietItems.setPulsesOrCurriesWithNV(pulsesOrCurriesWithNV);
					} else {
						fullPulsesOrCurries.addAll(pulsesOrCurries);
					}

					dietItems.setPulsesOrCurries(fullPulsesOrCurries);
					List<String> snacks = (List<String>) res.getJsonObject("data").getJsonArray("snacks").stream()
							.map(json -> json.toString()).collect(Collectors.toList());
					dietItems.setSnacks(snacks);
					List<String> dishes = (List<String>) res.getJsonObject("data").getJsonArray("dishes").stream()
							.map(json -> json.toString()).collect(Collectors.toList());
					List<String> fullDishesOrSubjies = new ArrayList<>();
					if ("NV".equalsIgnoreCase(foodType)) {
						List<String> dishesWithNV = (List<String>) res.getJsonObject("data")
								.getJsonArray("dishesWithNV").stream().map(json -> json.toString())
								.collect(Collectors.toList());
						fullDishesOrSubjies.addAll(dishesWithNV);
						fullDishesOrSubjies.addAll(dishes);
						dietItems.setDishesWithNV(dishesWithNV);
					} else {
						fullDishesOrSubjies.addAll(dishes);
					}
					dietItems.setDishes(fullDishesOrSubjies);

					List<String> rice = (List<String>) res.getJsonObject("data").getJsonArray("rice").stream()
							.map(json -> json.toString()).collect(Collectors.toList());
					dietItems.setRice(rice);
					selectiveItems.setDietItems(dietItems);
					profile.setSelectiveItems(selectiveItems);
				}

				logger.info("##### " + method + " PROFILES     -->> " + profile);
				promise.complete(profile);
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		}

		return promise.future();
	}

	/**
	 * Get customer habit (from V2 table)
	 * 
	 * @param email
	 * @param json
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getCustomerHabitV2(String email, JsonObject json, String traceId) {
		String method = "MongoRepositoryWrapper getCustomerHabitV2() " + traceId + "-[" + email + "]";
		JsonObject query = new JsonObject().put("email", email);
		logger.info("##### " + method + " QUERY -->> " + query);
		Promise<JsonObject> promise = Promise.promise();
		client.rxFind("CUST_HABIT_DETAIL_V2", query).subscribe(res -> {
			json.put("isAnyHabitSubscribed", true);
			if (res == null || res.isEmpty())
				json.put("isAnyHabitSubscribed", false);

			promise.complete(json);
		}, (ex) -> {
			logger.error("##### getCustomerHabit() ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Get customer profile creation date.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getProfileCreationDate(String email, String traceId) {
		String method = "MongoRepositoryWrapper getProfileCreationDate() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);
		JsonObject fields = new JsonObject().put("email", "email").put("profile.createdDate", "profileCreatedDate");
		client.rxFindOne("CUST_PROFILE", query, fields).subscribe(res -> {
			if (res == null || res.isEmpty()) {
				promise.fail("invalid customer");
			} else {
				JsonObject response = new JsonObject();
				response.put("email", res.getString("_id"));
				response.put("profileCreatedDate", res.getJsonObject("profile").getString("createdDate"));
				promise.complete(response);
			}
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Fetch calories history.
	 * 
	 * @param email
	 * @param noOfDays
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> fetchCaloriesHistory(String email, Integer noOfDays, String traceId) {
		String method = "MongoRepositoryWrapper fetchCaloriesHistory() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject();
		client.rxFind("CUST_DAILY_DIET", query).map(map -> {
			return map;
		}).subscribe(res1 -> {
			List<JsonObject> list = new ArrayList<>();
			JsonObject response = new JsonObject();
			JsonObject json1 = new JsonObject();
			json1.put("cumulative", 0.0);
			res1.forEach(action -> {
				String str = action.getJsonObject("_id").getString("email");
				Calendar calendar = Calendar.getInstance();
				calendar.add(Calendar.DAY_OF_MONTH, -noOfDays);
				String createdDateString = action.getJsonObject("_id").getString("date");
				Date createdDate;
				try {
					createdDate = new SimpleDateFormat("ddMMyyyy").parse(createdDateString);
					if (createdDate.after(calendar.getTime())) {

						if (email.equalsIgnoreCase(str)) {
							JsonObject json = new JsonObject();
							String recordDate = new SimpleDateFormat("dd-MMM-yyyy")
									.format(new SimpleDateFormat("ddMMyyyy")
											.parse(action.getJsonObject("_id").getString("date")));
							json.put("date", recordDate);
							json.put("calories", action.getJsonObject("data").getDouble("totalCal"));
							json.put("recommendedCal", action.getJsonObject("data").getDouble("recomended"));
							json1.put("cumulative", json1.getDouble("cumulative")
									+ action.getJsonObject("data").getDouble("recomended"));
							list.add(json);
						}
					}
				} catch (ParseException e) {
					e.printStackTrace();
				}
			});

			response.put("code", "0000");
			response.put("message", "Success");
			response.put("email", email);
			response.put("cumultativeCal", json1.getDouble("cumulative"));
			response.put("count", list.size());
			response.put("response", list);
			promise.complete(response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Fetch last calories history.
	 * 
	 * @param email
	 * @param noOfDays
	 * @param data
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> fetchLastCaloriesHistory(String email, Integer noOfDays, JsonObject data,
			String traceId) {
		String method = "MongoRepositoryWrapper fetchLastCaloriesHistory() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("_id.email", email);
		if ("0001".equals(data.getString("code"))) {
			promise.complete(data);
		} else {
			client.rxFind("CUST_DAILY_DIET", query).map(map -> {
				return map;
			}).subscribe(res1 -> {
				JsonObject response = new JsonObject();
				List<JsonObject> list = new ArrayList<>();
				JsonObject json1 = new JsonObject();
				json1.put("cumulative", 0.0);
				res1.forEach(action -> {
					Calendar calendar = Calendar.getInstance();
					calendar.setTime(new Date());
					calendar.add(Calendar.DAY_OF_MONTH, -noOfDays);
					String createdDateString = action.getJsonObject("_id").getString("date");
					Date createdDate;
					try {
						createdDate = new SimpleDateFormat("ddMMyyyy").parse(createdDateString);
						if (createdDate.after(calendar.getTime())) {
							JsonObject json = new JsonObject();
							String recordDate = new SimpleDateFormat("dd-MMM-yyyy")
									.format(new SimpleDateFormat("ddMMyyyy")
											.parse(action.getJsonObject("_id").getString("date")));
							json.put("date", recordDate);
							json.put("calories",
									ApiUtils.getDecimal(action.getJsonObject("data").getDouble("totalCal")));
							json.put("recommendedCal",
									ApiUtils.getDecimal(action.getJsonObject("data").getDouble("recomended")));
							json1.put("cumulative", json1.getDouble("cumulative")
									+ action.getJsonObject("data").getDouble("recomended"));
							list.add(json);
						}
					} catch (ParseException e) {
						e.printStackTrace();
					}
				});

				response.put("code", "0000");
				response.put("message", "Success");
				response.put("email", email);
				response.put("targetCaloriesToBurn", ApiUtils.getDecimal(data.getDouble("targetCaloriesToBurn")));
				response.put("activityLevels", data.getInteger("activityLevels"));
				response.put("cumultativeCal", ApiUtils.getDecimal(json1.getDouble("cumulative")));
				response.put("caloriesBurnRateDay",
						new DecimalFormat("##.0000").format(data.getDouble("caloriesBurnRateDay")));
				response.put("caloriesBurnRateNight",
						new DecimalFormat("##.0000").format(data.getDouble("caloriesBurnRateNight")));
				response.put("latestWeight", data.getDouble("weight"));
				response.put("count", list.size());
				response.put("response", list);
				promise.complete(response);
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		}

		return promise.future();
	}

	/**
	 * Get customer latest weight.
	 * 
	 * @param email
	 * @param data
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getLatestCustomerWeight(String email, JsonObject data, String traceId) {
		String method = "MongoRepositoryWrapper getLatestCustomerWeight() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		Calendar calll = Calendar.getInstance(); // creates calendar
		calll.setTime(new Date()); // sets calendar time/date
		calll.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		calll.add(Calendar.MINUTE, 30); // add 30 minutes
		JsonObject query = new JsonObject();
		query.put("_id", new JsonObject().put("email", email).put("date", ApiUtils.getCurrentDate(email, traceId)));
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFindOne("CUSTOMER_DAILY_WEIGHT", query, null).subscribe(res -> {
			if (res != null && !res.isEmpty()) {
				data.put("latestWeight", res.getDouble("weight"));
				data.put("currentDate", res.getString("date"));
			} else {
				logger.debug("##### " + method + " FAILED TO FETCH CACHE DIET");
			}
			promise.complete(data);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get Default profile.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getDefaulProfile(String email, String traceId) {
		String method = "MongoRepositoryWrapper getDefaulProfile() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", "v1");
		JsonObject response = new JsonObject();
		client.rxFindOne("DEFAUL_PROFILE", query, null).subscribe(res -> {
			JsonObject otherMaster = res.getJsonObject("otherMaster");
			JsonArray activitiesArr = otherMaster.getJsonArray("activities");
			response.put("activities", activitiesArr);
			promise.complete(response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get customer target calories.
	 * 
	 * @param email
	 * @param json
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getCustTargetCalories(String email, JsonObject json, String traceId) {
		String method = "MongoRepositoryWrapper getCustTargetCalories() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);
		logger.info("##### " + method + " JSON -->> " + json);
		logger.info("##### " + method + " QUERY -->> " + query);
		JsonObject fields = new JsonObject().put("email", "email").put("lifeStyle.calories", "lifeStyle.calories")
				.put("lifeStyle.activities", "lifeStyle.activities").put("demographic.gender", "demographic.gender")
				.put("demographic.height", "demographic.height").put("demographic.weight", "demographic.weight")
				.put("demographic.weightInKg", "demographic.weightInKg")
				.put("demographic.suggestedWeight", "demographic.suggestedWeight").put("demographic.gender", "gender")
				.put("demographic.age", "age");
		client.rxFindOne("CUST_PROFILE", query, fields).subscribe(res -> {
			JsonObject response = new JsonObject();
			HashMap<Float, Integer> activityMap = new HashMap<>();
			if (res == null || res.isEmpty()) {
				response.put("code", "0001");
				response.put("message", "Customer profile not found.");
			} else {
				response.put("code", "0000");
				response.put("message", "success");
				String activityCodeVal = res.getJsonObject("lifeStyle").getJsonObject("activities").getString("code");
				Float activity = res.getJsonObject("lifeStyle").getJsonObject("activities").getFloat("data");
				JsonArray activitiesArr = json.getJsonArray("activities");
				List<Integer> list = new ArrayList<>();
				activitiesArr.forEach(action -> {
					JsonObject jObj = (JsonObject) action;
					logger.info("##### " + method + " JOBJ -->> " + jObj);
					if (jObj.getString("code").equalsIgnoreCase(activityCodeVal)) {
						list.add(jObj.getInteger("steps"));
						activityMap.put(activity, jObj.getInteger("calories"));
					}
				});

				int baseMulti = 5;
				if (!"G1".equalsIgnoreCase(res.getJsonObject("demographic").getJsonObject("gender").getString("code")))
					baseMulti = -161;

//				Double recommendedCalories = res.getJsonObject("lifeStyle").getDouble("calories");
				// bmr + 0.04 * steps fecoomned
				int cals = 0;
				if ("AC4".equalsIgnoreCase(activityCodeVal))
					cals = 200;

				Double calories = ((((10 * res.getJsonObject("demographic").getJsonObject("weight").getDouble("value"))
						+ (6.25 * res.getJsonObject("demographic").getJsonObject("height").getDouble("value")))
						- (5 * res.getJsonObject("demographic").getJsonObject("age").getDouble("avg_age").intValue())
						+ baseMulti)) + (0.04 * list.get(0)) + cals + 50; // BMR + (0.04 x RECOMMENDED STEPS)
				response.put("targetCaloriesToBurn", Double.parseDouble(String.format("%.1f", calories)));

				// OLD IMPL
//				response.put("targetCaloriesToBurn",
//						Double.parseDouble(String.format("%.1f", recommendedCalories / 0.7)));
//				String gender = res.getJsonObject("demographic").getJsonObject("gender").getString("code");

				Double suggestedWeight = res.getJsonObject("demographic").getDouble("suggestedWeight");
				Double weight = json.getDouble("latestWeight");

				// OLD IMPL
//				double caloryFactor = 0.3;
				if (!json.containsKey("latestWeight")
						|| (json.containsKey("latestWeight") && null == json.getDouble("latestWeight"))) {
					weight = res.getJsonObject("demographic").getDouble("weightInKg");
					json.put("latestWeight", weight);

					// OLD IMPL
//					if (Double.compare(suggestedWeight, weight) > 0)
//						caloryFactor = 0.1d;
				}

				// OLD IMPL
//				Double height = res.getJsonObject("demographic").getJsonObject("height").getDouble("value");
//				String heightUnit = res.getJsonObject("demographic").getJsonObject("height").getString("unit");
//				if (!"cm".equalsIgnoreCase(heightUnit))
//					height = height * 2.54f;
//				Integer age = res.getJsonObject("demographic").getJsonObject("age").getDouble("avg_age").intValue();
//				if (!"G1".equalsIgnoreCase(gender))
//					baseMulti = -161;

				// OLD IMPL
//				Double activity = res.getJsonObject("lifeStyle").getJsonObject("activities").getDouble("data");
//				Double calories = (((((10 * weight) + (6.25 * height)) - (5 * age) + baseMulti)) * activity);
//				Double targetCalories = calories - (calories * caloryFactor);

				double calory = 450;
				if (Double.compare(suggestedWeight, weight) > 0)
					calory = 200;

				Double targetCalories = calories - calory;
				response.put("caloriesBurnRateDay", (targetCalories / (24 * 3600)) * 1.025);
				response.put("caloriesBurnRateNight", (targetCalories / (24 * 3600)) * 0.95);

				String activityCode = res.getJsonObject("lifeStyle").getJsonObject("activities").getString("code");
				json.getJsonArray("activities").forEach(action -> {
					JsonObject obj = (JsonObject) action;
					String code = obj.getString("code");
					if (code.equalsIgnoreCase(activityCode))
						response.put("activityLevels", obj.getInteger("steps"));
				});
			}

			response.put("weight", json.getDouble("latestWeight"));
			promise.complete(response);
			logger.info("##### " + method + " RESPONSE -->> " + response);
		}, (ex) -> {
			logger.error("##### ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Utility function to convert java Date to TimeZone format
	 * 
	 * @param date
	 * @param format
	 * @param timeZone
	 * @return
	 */
	public String convertServerDateTimeToRequestedTimeZone(Date date, String format, String timeZone) {
		if (date == null)
			return null;

		logger.info("##### convertServerDateTimeToRequestedTimeZone() REACHED");
		// create SimpleDateFormat object with input format
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		// default system timezone if passed null or empty
		if (timeZone == null || "".equalsIgnoreCase(timeZone.trim())) {
			timeZone = Calendar.getInstance().getTimeZone().getID();
			// set timezone to SimpleDateFormat
			sdf.setTimeZone(TimeZone.getTimeZone(timeZone));
			// return Date in required format with timezone as String
		}

		String returnedTimeZoneDate = sdf.format(date);
		logger.info("##### convertServerDateTimeToRequestedTimeZone() RETURNED TIMEZONE -->> " + returnedTimeZoneDate);
		return sdf.format(returnedTimeZoneDate);
	}

	/**
	 * Customer having already refresh dietitems into DB.
	 * 
	 * @param email
	 * @param data
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getCustAlreadyRefreshedDietItems(String email, JsonObject data, String traceId) {
		String method = "MongoRepositoryWrapper getCustAlreadyRefreshedDietItems() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id",
				new JsonObject().put("email", email).put("createdDate", ApiUtils.getCurrentDate(email, traceId)));
		client.rxFindOne("CUST_REFRESH_FOOD_ITEMS", query, null).subscribe(res -> {
			if (res == null || res.isEmpty())
				res = new JsonObject();
			else
				data.put("foodList", res.getJsonArray("foodList"));

			promise.complete(data);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Remove refreshed dietitems.
	 * 
	 * @param email
	 * @param data
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> removeRefreshedDietItems(String email, JsonObject data, String traceId) {
		String method = "MongoRepositoryWrapper removePreferences() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id",
				new JsonObject().put("email", email).put("createdDate", ApiUtils.getCurrentDate(email, traceId)));
		client.rxRemoveDocument("CUST_REFRESH_FOOD_ITEMS", query).subscribe(res -> {
			promise.complete(new JsonObject().put("code", "0000").put("message", "Sucess"));
		}, (ex) -> {
			logger.error("##### " + method + " DATA -->> " + ex.getMessage());
			promise.complete(new JsonObject().put("code", "0001").put("message", ex.getMessage()));
		});

		return promise.future();
	}

	/**
	 * Get refreshed diet item.
	 * 
	 * @param slot
	 * @param data
	 * @param categoryName
	 * @param foodItem
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getRefreshedDietItem(Integer slot, JsonObject data, String categoryName,
			FoodDetail foodItem, String email, String traceId) {
		String method = "MongoRepositoryWrapper getRefreshedDietItem() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		Map<String, List<String>> slotMap = getFoodItemsBySlot(slot, traceId);
		JsonObject response = new JsonObject();
		String itemCode = foodItem.getCode();
		logger.info("##### " + method + "  FOOD ITEM CODE -->> " + itemCode);
		logger.info("##### " + method + "        CATEGORY -->> " + categoryName);
		JsonArray mealOptionsArr = data.getJsonArray("mealOptions");
		JsonArray foodListArr = data.getJsonArray("foodList");
		List<String> alreadyChoosenFoodItems = new ArrayList<>();
		boolean isFoodItemAlreadyExist = false;
		if (null != foodListArr) {
			foodListArr.remove(null);
			isFoodItemAlreadyExist = true;
			foodListArr.forEach(action -> {
				if (null != action)
					alreadyChoosenFoodItems.add(action.toString());
			});
		}

		int count = 0;
		boolean isFoodItemFound = false;
		JsonObject providedFoodItem = new JsonObject();
		JsonObject firstFoodItem = new JsonObject();
		List<JsonObject> foodList = new ArrayList<>();
		List<String> foodItemsList = new ArrayList<>();
		Outer: for (int i = 0; i < mealOptionsArr.size(); ++i) {
			count = 0;
			JsonObject json = mealOptionsArr.getJsonObject(i);
			JsonArray categoriesArr = json.getJsonArray("categories");
			for (int j = 0; j < categoriesArr.size(); ++j) {
				JsonObject catJson = categoriesArr.getJsonObject(j);
				JsonArray foodArr = catJson.getJsonArray("food");
				JsonObject foodJson = new JsonObject();
				for (int k = 0; k < foodArr.size(); ++k) {
					foodJson = foodArr.getJsonObject(k);
					if (slotMap.get(categoryName).contains(foodJson.getString("Type"))) {
						foodItemsList.add(foodJson.getString("itemCode"));
						foodList.add(foodJson);
					}

					if (itemCode.equalsIgnoreCase(foodJson.getString("itemCode"))) {
						firstFoodItem = foodJson;
						providedFoodItem = foodJson;
					}

					++count;
				}

				if (providedFoodItem.toString().equals("{}") || providedFoodItem.isEmpty())
					providedFoodItem = data.getJsonObject("foodItem");

				String startItem = Character.toString(categoryName.charAt(0));
				if (count <= 1)
					alreadyChoosenFoodItems.clear();
				List<JsonObject> filterList = new ArrayList<>();
				for (int k = 0; k < foodArr.size(); k++) {
					JsonObject foodJson1 = foodArr.getJsonObject(k);
					if (!itemCode.equalsIgnoreCase(foodJson1.getString("itemCode"))
							&& ((null == alreadyChoosenFoodItems) || (null != alreadyChoosenFoodItems
									&& !alreadyChoosenFoodItems.contains(foodJson1.getString("itemCode"))))
							&& (foodJson1.getString("Type").startsWith(startItem) || slotMap.get(categoryName)
									.contains(Character.toString(foodJson1.getString("Type").charAt(0))))
							&& count > 1) {
						filterList.add(foodJson1);
						isFoodItemFound = true;
					}
				}

				if (isFoodItemFound) {
					response = filterList.get(0);
					break Outer;
				}
			}
		}

		boolean isItemToBeFlushed = false;
		if (!isFoodItemFound) {

			response = firstFoodItem;
			if (null != foodList && foodList.size() > 0) {
				foodList.forEach(action -> {
					if (null != action)
						foodListArr.remove(action.getString("itemCode"));
				});

				response = foodList.get(0);
			}

			isItemToBeFlushed = true;
		}

		boolean isResponseEmpty = false;
		if (response.toString().equals("{}") || response.isEmpty()) {
			response = providedFoodItem;
			isResponseEmpty = true;
		}

		logger.info("##### " + method + " COUNT         -->> " + count);
		logger.info("##### " + method + " RESPONSE ITEM -->> " + response);
		if (count > 1) {
			Calendar cal1 = Calendar.getInstance(); // creates calendar
			cal1.setTime(new Date()); // sets calendar time/date
			cal1.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
			cal1.add(Calendar.MINUTE, 30); // add 30 minutes
			String updatedDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
					.format(cal1.getTime());
			try {
				if ((!isFoodItemFound || !isFoodItemAlreadyExist) && null == foodListArr) {
					JsonArray foodArr = new JsonArray();
					foodArr.add(itemCode);

					JsonObject query = new JsonObject();
					query.put("_id", new JsonObject().put("email", email).put("createdDate",
							ApiUtils.getCurrentDate(email, traceId)));
					query.put("foodList", foodArr);
					query.put("updatedDateTime", new JsonObject().put("$date", updatedDate));
					client.rxSave("CUST_REFRESH_FOOD_ITEMS", query).subscribe(res -> {
					}, (ex) -> {
						logger.error("##### " + method + " ERROR MESSAGE -->> " + ex.getMessage());
						promise.fail(ex.getMessage());
					});
				} else {
					if (!isResponseEmpty)
						if (isResponseEmpty || isItemToBeFlushed) {
							foodListArr.clear();
							foodListArr.add(response.getString("itemCode"));
						} else if (!foodListArr.contains(response.getString("itemCode"))) {
							foodListArr.add(response.getString("itemCode"));
						}

					if (null != foodListArr && foodListArr.contains(null))
						foodListArr.remove(null);
					JsonObject payload = new JsonObject().put("$set",
							new JsonObject().put("foodList", foodListArr).put("updatedDateTime", updatedDate));
					JsonObject query = new JsonObject().put("_id", new JsonObject().put("email", email)
							.put("createdDate", ApiUtils.getCurrentDate(email, traceId)));
					client.rxUpdateCollection("CUST_REFRESH_FOOD_ITEMS", query, payload).subscribe(res -> {
					}, (ex) -> {
						logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
						promise.fail(ex.getMessage());
					});
				}
			} catch (Exception e) {
				logger.error("##### " + method + " EXCEPTION ERROR -->> " + e.getMessage());
				e.printStackTrace();
			}
		}

		JsonObject jsonResponse = new JsonObject();
		jsonResponse.put("newFoodItem", response);
		jsonResponse.put("oldFoodItem", data.getJsonObject("foodItem"));

		promise.complete(jsonResponse);
		return promise.future();
	}

	/**
	 * Get fooditems basis slot.
	 * 
	 * @param slot
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	private Map<String, List<String>> getFoodItemsBySlot(Integer slot, String traceId) {
		List<String> slotItems = new ArrayList<String>();
		Map<String, List<String>> slotMap = new HashMap<>();
		if (slot == 0) {
			slotItems.add("D");
			slotItems.add("DM");
			slotMap.put("D", slotItems);
			slotMap.put("DM", slotItems);
		} else if (slot == 1) {
			slotItems.add("F");
			slotItems.add("FS");
			slotMap.put("F", slotItems);
			slotMap.put("FS", slotItems);
		} else if (slot == 2) {
			List<String> slot2Items = new ArrayList<>();
			List<String> slot2DItems = new ArrayList<>();
			slotItems.add("W");
			slotItems.add("WC");
			slotItems.add("WCP");
			slotItems.add("WP");
			slotItems.add("WPP");
			slotItems.add("WE");
			slotItems.add("WCP");
			slotItems.add("WM");
			slot2Items.add("S");
			slot2Items.add("SM");
			slot2DItems.add("D");
			slot2DItems.add("DM");
			slotMap.put("W", slotItems);
			slotMap.put("WC", slotItems);
			slotMap.put("WCP", slotItems);
			slotMap.put("WP", slotItems);
			slotMap.put("WPP", slotItems);
			slotMap.put("WE", slotItems);
			slotMap.put("WCP", slotItems);
			slotMap.put("WM", slotItems);
			slotMap.put("S", slot2Items);
			slotMap.put("SM", slot2Items);
			slotMap.put("D", slot2DItems);
			slotMap.put("DM", slot2DItems);
		} else if (slot == 3) {
			List<String> slot3Items = new ArrayList<String>();
			slotItems.add("W");
			slotItems.add("WM");
			slot3Items.add("D");
			slot3Items.add("DM");
			slotMap.put("W", slotItems);
			slotMap.put("WM", slotItems);
			slotMap.put("D", slot3Items);
			slotMap.put("DM", slot3Items);
		} else if (slot == 4) {
			List<String> slot4ItemsAsS = new ArrayList<>();
			List<String> slotAItems = new ArrayList<>();
			List<String> slotBItems = new ArrayList<>();
			List<String> slotCItems = new ArrayList<>();
			slotAItems.add("A");
			slotCItems.add("C");
			slotBItems.add("B");
			slot4ItemsAsS.add("S");
			slot4ItemsAsS.add("SM");
			slotMap.put("A", slotAItems);
			slotMap.put("C", slotCItems);
			slotMap.put("B", slotBItems);
			slotMap.put("S", slot4ItemsAsS);
			slotMap.put("SM", slot4ItemsAsS);
		} else if (slot == 5) {
			slotItems.add("D");
			slotItems.add("DM");
			slotMap.put("D", slotItems);
			slotMap.put("DM", slotItems);
		} else if (slot == 6) {
			List<String> slot5Items = new ArrayList<>();
			slotItems.add("W");
			slotItems.add("WM");
			slot5Items.add("D");
			slotMap.put("W", slotItems);
			slotMap.put("WM", slotItems);
			slotMap.put("D", slot5Items);
			slotMap.put("DM", slot5Items);
		} else if (slot == 7) {
			List<String> slot7Items = new ArrayList<>();
			List<String> slot71Items = new ArrayList<>();
			List<String> slot7BItems = new ArrayList<>();
			slot7BItems.add("B");
			slotItems.add("W");
			slotItems.add("WC");
			slotItems.add("WCP");
			slotItems.add("WP");
			slotItems.add("WPP");
			slotItems.add("WE");
			slotItems.add("WM");
			slot71Items.add("F");
			slot71Items.add("FS");
			slot71Items.add("C");
			slot7Items.add("S");
			slot7Items.add("SM");
			slotMap.put("B", slot7BItems);
			slotMap.put("W", slotItems);
			slotMap.put("WC", slotItems);
			slotMap.put("WCP", slotItems);
			slotMap.put("WP", slotItems);
			slotMap.put("WPP", slotItems);
			slotMap.put("WE", slotItems);
			slotMap.put("WM", slotItems);
			slotMap.put("F", slot71Items);
			slotMap.put("FS", slot71Items);
			slotMap.put("C", slot71Items);
			slotMap.put("S", slot7Items);
			slotMap.put("SM", slot7Items);
		} else if (slot == 8) {
			slotItems.add("D");
			slotItems.add("DM");
			slotMap.put("D", slotItems);
			slotMap.put("DM", slotItems);
		}

		return slotMap;
	}

	/**
	 * Get dietplan for refresh foods for options.
	 * 
	 * @param email
	 * @param slot
	 * @param foodItem
	 * @param json
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getDietPlanForRefreshOptionAndFoodCode(String email, Integer slot, FoodDetail foodItem,
			JsonObject json, String traceId) {
		String method = "MongoRepositoryWrapper getDietPlanForRefreshOptionAndFoodCode() " + traceId + "-[" + email
				+ "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", foodItem.getCode());
		client.rxFindOne("DIET_PLAN", query, null).subscribe(res -> {
			json.put("foodItem", res);
			promise.complete(json);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get customer profile for fetching food itrms.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getCustomerProfileForFetchFoodItems(String email, String traceId) {
		String method = "MongoRepositoryWrapper getCustomerProfileForFetchFoodItems() " + traceId + "-[" + email + "]";
		JsonObject response = new JsonObject();
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);
		JsonObject fileds = new JsonObject().put("lifeStyle.foodType", "lifeStyle.foodType")
				.put("lifeStyle.communities", "lifeStyle.communities").put("lifeStyle.diseases", "lifeStyle.diseases")
				.put("lifeStyle.calories", "lifeStyle.calories");
		client.rxFindOne("CUST_PROFILE", query, fileds).subscribe(res -> {
			try {
				if (null != res && null != res.getJsonObject("lifeStyle")
						&& null != res.getJsonObject("lifeStyle").getString("foodType")) {
					response.put("foodType", res.getJsonObject("lifeStyle").getString("foodType"));
					response.put("customerDiseases", res.getJsonObject("lifeStyle").getJsonArray("diseases"));
				}
			} catch (Exception e) {
				logger.error("##### " + method + " EXCEPTION -->> " + e.getMessage());
			}

			promise.complete(response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get customer profile for fetching food itrms.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> updatedSlotDetailRecommendedFor(String email, JsonObject data, String traceId) {
		String method = "MongoRepositoryWrapper updatedSlotDetailRecommendedFor() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);
		JsonObject fileds = new JsonObject().put("lifeStyle.foodType", "lifeStyle.foodType")
				.put("lifeStyle.communities", "lifeStyle.communities").put("lifeStyle.diseases", "lifeStyle.diseases")
				.put("lifeStyle.calories", "lifeStyle.calories");
		client.rxFindOne("CUST_PROFILE", query, fileds).subscribe(res -> {
			try {
				JsonArray disease = res.getJsonObject("lifeStyle").getJsonArray("diseases");
				JsonArray items = data.getJsonArray("data");
				List<JsonObject> dietPlans = new ArrayList<>();
				if (null != disease && disease.size() > 0) {
					items.forEach(json -> {
						JsonObject action = (JsonObject) json;
						JsonArray jsonArr = action.getJsonArray("RecommendedIn");
						List<String> listAll = new ArrayList<>();
						if (null != action && null != jsonArr && !jsonArr.isEmpty() && jsonArr.size() > 0) {
							JsonArray jArr = new JsonArray();
							jsonArr.forEach(obj -> {
								if (null != obj) {
									String food = (String) obj;
									if (disease.contains(food) && !jArr.contains(food)) {
										listAll.add(food);
										jArr.add(food);
										action.put("recommendedFor", jArr);
									}
								}
							});
						}

						dietPlans.add(action);
					});

					if (null != dietPlans && dietPlans.size() > 0)
						data.put("data", dietPlans);
				}
			} catch (Exception e) {
				logger.error("##### " + method + " EXCEPTION -->> " + e.getMessage());
			}

			promise.complete(data);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * get customer video food items.
	 * 
	 * @param email
	 * @param json
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getCustVideoFoodItems(String email, JsonObject json, String traceId) {
		String method = "MongoRepositoryWrapper getCustVideoFoodItems() " + traceId + "-[" + email + "]";
		JsonObject query = new JsonObject();
		query.put("_id",
				new JsonObject().put("email", email).put("createdDate", ApiUtils.getCurrentDate(email, traceId)));
		Promise<JsonObject> promise = Promise.promise();
		client.rxFindOne("CUST_VIDEO_FOODITEMS", query, null).subscribe(res -> {
			if (res != null)
				json.put("oldFoodItems", res.getJsonArray("foodList"));

			promise.complete(json);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR 1 -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get food items.
	 * 
	 * @param email
	 * @param json
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getFoodItems(String email, JsonObject json, String traceId) {
		String method = "MongoRepositoryWrapper getFoodItems() " + traceId + "-[" + email + "]";
		String foodType = json.getString("foodType");
		JsonArray foodListArr = json.getJsonArray("oldFoodItems");
		List<String> alreadyDisplayedFoodItems = new ArrayList<>();
		JsonObject response = new JsonObject();
		if (null != foodListArr && foodListArr.size() > 0 && !foodListArr.isEmpty()) {
			foodListArr.remove(null);
			foodListArr.forEach(action -> {
				JsonObject jsonObject = (JsonObject) action;
				alreadyDisplayedFoodItems.add(jsonObject.getString("itemCode"));
			});
		}

		JsonObject query = new JsonObject();
		query.put("foodType", foodType);
		Promise<JsonObject> promise = Promise.promise();
		JsonArray foodArr = new JsonArray();
		client.rxFind("DIET_PLAN", query).map(map -> {
			return map;
		}).subscribe(res -> {
			res.forEach(action -> {
				try {
					if (null != action && !alreadyDisplayedFoodItems.contains(action.getString("itemCode"))) {
						if (null != foodArr && foodArr.size() < 10 && null != action.getString("video")
								&& "null" != action.getString("video")
								&& !"".equalsIgnoreCase(action.getString("video")))
							foodArr.add(action);
					}
				} catch (Exception e) {
					logger.error("##### " + method + " ERROR -->> " + e.getMessage());
					e.printStackTrace();
				}
			});

			response.put("oldFoodItems", foodListArr);
			response.put("newFoodItems", foodArr);
			logger.info("##### " + method + " NEW FOOD ITEMS -->> " + response);
			promise.complete(response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Persist customer video food items.
	 * 
	 * @param email
	 * @param json
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> saveCustVideoFoodItems(String email, JsonObject json, String traceId) {
		String method = "MongoRepositoryWrapper saveCustVideoFoodItems() " + traceId + "-[" + email + "]";
		JsonArray finalFoodToPersist = new JsonArray();
		JsonArray newFoodArr = new JsonArray();
		JsonArray oldFoodArr = new JsonArray();
		boolean isOldItemAvailable = false;
		if (json.containsKey("newFoodItems")) {
			newFoodArr = json.getJsonArray("newFoodItems");
		}

		if (json.containsKey("oldFoodItems")) {
			oldFoodArr = json.getJsonArray("oldFoodItems");
			if (null != oldFoodArr) {
				isOldItemAvailable = true;
				logger.debug("##### " + method + " COMPARISON OLD FOOD ARR -->> " + oldFoodArr.isEmpty());
				logger.debug("##### " + method + "   IS OLD ITEM AVAILABLE -->> " + !isOldItemAvailable);
			}
		}

		if (null != oldFoodArr)
			finalFoodToPersist.addAll(oldFoodArr);
		if (null != newFoodArr)
			finalFoodToPersist.addAll(newFoodArr);

		Promise<JsonObject> promise = Promise.promise();
		Calendar cal = Calendar.getInstance(); // creates calendar
		cal.setTime(new Date()); // sets calendar time/date
		cal.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		cal.add(Calendar.MINUTE, 30); // add 30 minutes
		String updatedDateTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH).format(cal.getTime());
		if (null == oldFoodArr || oldFoodArr.isEmpty() || !isOldItemAvailable) {
			JsonObject query = new JsonObject();
			query.put("_id",
					new JsonObject().put("email", email).put("createdDate", ApiUtils.getCurrentDate(email, traceId)));
			query.put("size", newFoodArr.size());
			query.put("foodList", newFoodArr);
			query.put("updatedDateTime", updatedDateTime);
			client.rxSave("CUST_VIDEO_FOODITEMS", query).subscribe(res -> {
				logger.debug("##### " + method + " CUST_REFRESH_FOOD_ITEMS SAVED SUCCESSFULLY. -->> " + res);
			}, (ex) -> {
				logger.error("##### " + method + " ERROR MESSAGE -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		} else {
			JsonObject payload = new JsonObject().put("$set", new JsonObject().put("size", finalFoodToPersist.size())
					.put("foodList", finalFoodToPersist).put("updatedDateTime", updatedDateTime));
			JsonObject query = new JsonObject().put("_id",
					new JsonObject().put("email", email).put("createdDate", ApiUtils.getCurrentDate(email, traceId)));
			client.rxUpdateCollection("CUST_VIDEO_FOODITEMS", query, payload).subscribe(res -> {
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		}

		JsonObject jsonResponse = new JsonObject();
		jsonResponse.put("code", "0000");
		jsonResponse.put("message", "success");
		jsonResponse.put("foodItems", newFoodArr);
		if (null == newFoodArr || newFoodArr.isEmpty()) {
			jsonResponse.put("code", "0001");
			jsonResponse.put("message", "No more records available.");
		}

		promise.complete(jsonResponse);
		return promise.future();
	}

	/**
	 * Fetch today analytics status if mail sent or not. [UNUSED]
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getTodayAnalyticStatus(String email, String traceId) {
		String method = "MongoRepositoryWrapper getTodayAnalyticStatus() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("_id", new JsonObject().put("email", email).put("date", ApiUtils.getCurrentDate(email, traceId)));
		client.rxFindOne("CUST_MAIL_SENT_STATUS", query, null).subscribe(res -> {
			JsonObject json = new JsonObject();
			if (res != null && !res.isEmpty()) {
				json.put("mailSent", res.getBoolean("mailSent"));
				promise.complete(res);
			} else {
				json.put("mailSent", false);
				promise.complete(json);
			}
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Gather customer analytics.
	 * 
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getCustAnalytics(String traceId) {
		String method = "MongoRepositoryWrapper getCustAnalytics() " + traceId + " ";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject response = new JsonObject();
		client.rxFind("CUSTOMER_ANALYTICS", new JsonObject()).map(mapper -> {
			mapper.forEach(action -> {
				action.remove("_id");
			});
			return mapper;
		}).subscribe(res -> {
			List<JsonObject> list = new ArrayList<>();
			if (res == null || res.isEmpty()) {
				response.put("code", "0001");
				response.put("message", "No customer found.");
				response.put("analytics", res);
			} else {
				response.put("code", "0000");
				response.put("message", "Customer analytics fetched successfully.");
				res.forEach(action -> {
					list.add(action);
				});

				response.put("analytics", list);
			}

			getCsvFile(response, list, traceId);
			response.remove("analytics");
			response.put("mailSent", true);

			promise.complete(response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			response.put("code", "0001");
			response.put("messsage", ex.getMessage());
			promise.complete(response);
		});
		return promise.future();
	}

	/**
	 * Create csv file.
	 * 
	 * @param response
	 * @param analyticsList
	 * @param traceId
	 * @return JsonObject
	 */
	private JsonObject getCsvFile(JsonObject response, List<JsonObject> analyticsList, String traceId) {
		String method = "MongoRepositoryWrapper getCsvFile()";
		CSVWriter writer = null;
		try {
			writer = new CSVWriter(new FileWriter("/home/ubuntu/fightitaway/analytics.csv"));
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		String[] header = { "EMAIL", "ACTION", "CREATED DATE" };
		List<String[]> records = analyticsList.stream().map(mapper -> {
			String[] record = { mapper.getString("emailId"), mapper.getString("action"),
					mapper.getString("createdDate") };
			return record;
		}).collect(Collectors.toList());

		List<String[]> analytics = new ArrayList<>();
		analytics.add(header);
		analytics.addAll(records);

		// Writing data to the csv file
		try {
			writer.writeAll(analytics);
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		response.put("code", "0000");
		response.put("message", "File saved successfully.");

		// message info
		String mailTo = "draghvendra3101@gmail.com";
		String subject = "New email with attachments";
		String message = "Customer Analytics";

		// attachments
		String[] attachFiles = new String[3];
		attachFiles[0] = "/home/ubuntu/fightitaway/analytics.csv";
		try {
			Integer analyticsSize = 0;
			if (null != analytics && analytics.size() > 0) {
				analyticsSize = analytics.size();
				response = sendEmailWithAttachments(response, mailTo, subject, message, attachFiles, analyticsSize,
						traceId);
			}
		} catch (Exception ex) {
			response.put("mailSent", false);
			logger.info("##### " + method + " UNABLE TO SEND EMAIL -->> " + ex.getMessage());
			ex.printStackTrace();
		}

		return response;
	}

	/**
	 * Sent email to Admin with Attachment(s).
	 * 
	 * @param response
	 * @param toAddress
	 * @param subject
	 * @param message
	 * @param attachFiles
	 * @param analyticsSize
	 * @param traceId
	 * @return JsonObject
	 * 
	 * @throws AddressException
	 * @throws MessagingException
	 */
	public JsonObject sendEmailWithAttachments(JsonObject response, String toAddress, String subject, String message,
			String[] attachFiles, Integer analyticsSize, String traceId) throws AddressException, MessagingException {
		String method = "MongoRepositoryWrapper sendEmailWithAttachments() " + traceId;
		// sets SMTP server properties
		Properties properties = new Properties();
		final String username = "admin@fightitaway.com";
		final String password = "sairam123!";
		final String adminEmail = "d.raghvendra@gmail.com";

		properties.put("mail.smtp.host", "smtp.gmail.com");
		properties.put("mail.smtp.port", "465");
		properties.put("mail.smtp.socketFactory.port", "465");
		properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		properties.put("mail.smtp.auth", "true");
		properties.put("mail.smtp.starttls.enable", true); // TLS
		properties.put("mail.smtp.auth", true);
		properties.put("mail.smtp.connectiontimeout", 5000);
		properties.put("mail.smtp.timeout", 5000);
		properties.put("mail.user", username);
		properties.put("mail.password", password);

		Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		});

		// creates a new e-mail message
		Message msg = new MimeMessage(session);

		msg.setFrom(new InternetAddress(username));
		// InternetAddress[] toAddresses = { new InternetAddress(username) };
		// InternetAddress[] toAddresses = { new InternetAddress(username), new
		// InternetAddress(adminEmail) };
		InternetAddress[] toAddresses = { new InternetAddress(adminEmail) };
		msg.setRecipients(Message.RecipientType.TO, toAddresses);
		msg.setSubject(subject);
		msg.setSentDate(new Date());

		// creates message part
		MimeBodyPart messageBodyPart = new MimeBodyPart();
		messageBodyPart.setContent(message, "text/html");

		// creates multi-part
		Multipart multipart = new MimeMultipart();
		multipart.addBodyPart(messageBodyPart);

		// adds attachments
		if (attachFiles != null && attachFiles.length > 0) {
			// for (String filePath : attachFiles) {
			MimeBodyPart attachPart = new MimeBodyPart();
			try {
				attachPart.attachFile(attachFiles[0]);
			} catch (IOException ex) {
				ex.printStackTrace();
			}

			multipart.addBodyPart(attachPart);
		}

		// sets the multi-part as e-mail's content
		msg.setContent(multipart);

		// sends the e-mail
		// Transport.send(msg);
		logger.debug("##### " + method + " MAIL SENT SUCCESSFULLY.");
		try {
			Path pathOfFile = Paths.get(attachFiles[0]);
			boolean result = Files.deleteIfExists(pathOfFile);
		} catch (NoSuchFileException x) {
			logger.error("##### " + method + " NO FILE FOUND     -->> " + x.getMessage());
		} catch (IOException x) {
			logger.error("##### " + method + " IOException FOUND -->> " + x.getMessage());
		}

		response.put("code", "0000");
		response.put("message", "MAIL SENT SUCCESSFULLY TO [" + username + "]");
		response.put("mailSent", true);
		response.put("analyticsize", analyticsSize);

		return response;
	}

	/**
	 * Analytics - Customer payment details.
	 * 
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getPaymentDetailAnalytics(Integer noOfDays, String traceId) {
		String method = "MongoRepositoryWrapper getPaymentDetailAnalytics() " + traceId;
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = null;

		Calendar cal = Calendar.getInstance(); // creates calendar
		cal.setTime(new Date()); // sets calendar time/date
		cal.add(Calendar.DAY_OF_MONTH, -30);
		cal.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		cal.add(Calendar.MINUTE, 30); // add 30 minutes

		try {
			query = new JsonObject()
					.put("createdDate",
							new JsonObject().put("$gte", new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime())))
					.put("amount", new JsonObject().put("$gt", 0));
		} catch (Exception ex) {
			logger.info("##### " + method + " EXCEPTION -->> " + ex.getMessage());
			ex.printStackTrace();
			query = new JsonObject();
		}
		List<JsonObject> list = new ArrayList<>();
		client.rxFind("PAYMENT_DETAIL", query).map(map -> {
			return map;
		}).subscribe(res -> {
			res.forEach(action -> {
				if (action.containsKey("emailId") && action.containsKey("durationInDays")
						&& action.containsKey("amount")) {
					if (null != action.getString("emailId") && !"null".equalsIgnoreCase(action.getString("emailId"))
							&& null != action.getInteger("durationInDays") && action.containsKey("paymentId")) {
						JsonObject json = new JsonObject();
						Integer amount = action.getInteger("amount");
						String emailId = action.getString("emailId");
						Integer durationValidInDays = action.getInteger("durationInDays");
						try {
							String createdDateTimeStr = new SimpleDateFormat("dd-MMM-yyyy")
									.format(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
											.parse(action.getString("createdDate")));
							Date createdDate = new SimpleDateFormat("dd-MMM-yyyy").parse(createdDateTimeStr);
							Calendar cal1 = Calendar.getInstance(); // creates calendar
							cal1.setTime(createdDate); // sets calendar time/date
							cal1.add(Calendar.DATE, durationValidInDays);
							cal1.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
							cal1.add(Calendar.MINUTE, 30); // add 30 minutes
							String expiryDateStr = new SimpleDateFormat("dd-MMM-yyyy").format(cal1.getTime());
							int diffInDays = (int) ((new Date().getTime() - createdDate.getTime())
									/ (1000 * 60 * 60 * 24));
							if (diffInDays <= 30) {
								json.put("email", emailId);
								json.put("amount", amount);
								json.put("duration", durationValidInDays);
								json.put("createdDate", createdDateTimeStr);
								json.put("expiryDate", expiryDateStr);
								list.add(json);
							}
						} catch (Exception e) {
							logger.debug("##### " + method + " ERROR -->> " + e.getMessage());
							e.printStackTrace();
						}
					}
				}
			});

			Collections.reverse(list);
			JsonObject response = new JsonObject();
			if (null != list && list.size() > 0)
				response = getCsvPaymentDetailFile(response, list, traceId);

			promise.complete(response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Gather customer payment details into csv.
	 * 
	 * @param response
	 * @param paymentanalytics
	 * @param traceId
	 * @return JsonObject
	 */
	private JsonObject getCsvPaymentDetailFile(JsonObject response, List<JsonObject> paymentanalytics, String traceId) {
		String method = "MongoRepositoryWrapper getCsvPaymentDetailFile() " + traceId;
		CSVWriter writer = null;
		try {
			writer = new CSVWriter(new FileWriter("/home/ubuntu/fightitaway/paymentanalytics.csv"));
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		String[] header = { "EMAIL", "AMOUNT", "DURATION", "CREATED DATE", "EXPIRY DATE" };
		List<String[]> records = paymentanalytics.stream().map(mapper -> {
			logger.debug("##### " + method + " MAPPER -->> " + mapper);
			String[] record = { mapper.getString("email"), mapper.getInteger("amount").toString(),
					mapper.getInteger("duration").toString(), mapper.getString("createdDate"),
					mapper.getString("expiryDate") };
			return record;
		}).collect(Collectors.toList());

		List<String[]> analytics = new ArrayList<>();
		analytics.add(header);
		analytics.addAll(records);

		// Writing data to the csv file
		try {
			writer.writeAll(analytics);
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		response.put("code", "0000");
		response.put("message", "File saved successfully.");

		// message info
		String mailTo = "draghvendra3101@gmail.com";
		String subject = "New payment details email with attachments";
		String message = "Customer Payment Analytics";

		// attachments
		String[] attachFiles = new String[3];
		attachFiles[0] = "/home/ubuntu/fightitaway/paymentanalytics.csv";
		try {
			logger.info("##### " + method + " EMAIL SEND [PAYMENT ANALYTICS] IN PROCESS......");
			response = sendEmailWithAttachments(response, mailTo, subject, message, attachFiles,
					paymentanalytics.size(), traceId);
		} catch (Exception ex) {
			response.put("mailSent", false);
			logger.error("##### " + method + " COULD NOT SEND EMAIL FOR PAYMENT ANALYTICS -->> " + ex.getMessage());
			ex.printStackTrace();
		}

		return response;
	}

	/**
	 * Dietplan analytics - Cache
	 * 
	 * @param data
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getDietPlanCacheAnalytics(JsonObject data, String traceId) {
		String method = "MongoRepositoryWrapper getDietPlanCacheAnalytics() " + traceId;
		Promise<JsonObject> promise = Promise.promise();
		JsonObject response = new JsonObject();
		List<String> list = new ArrayList<>();
		JsonObject query = new JsonObject();
		client.rxFind("CUST_DAILY_DIET", query).map(map -> {
			return map;
		}).subscribe(res1 -> {
			Map<String, Integer> cacheMap = new HashMap<>();
			List<JsonObject> dietCacheList = new ArrayList<JsonObject>();
			res1.forEach(action -> {
				String email = action.getJsonObject("_id").getString("email");
				Calendar calendar = Calendar.getInstance();
				calendar.add(Calendar.DAY_OF_MONTH, -30);
				String createdDateString = action.getJsonObject("_id").getString("date");
				Date createdDate;
				try {
					createdDate = new SimpleDateFormat("ddMMyyyy").parse(createdDateString);
					String createdDateStr = new SimpleDateFormat("dd-MMM-yyyy").format(createdDate);
					int diffInDays = (int) ((new Date().getTime() - createdDate.getTime()) / (1000 * 60 * 60 * 24));
					if (diffInDays <= 30) {
						String createDateStr = new SimpleDateFormat("dd-MMM-yyyy").format(createdDate);
						list.add(createdDateStr + ":::" + email);
						if (cacheMap.containsKey(createdDateStr)) {
							Integer count = cacheMap.get(createDateStr);
							cacheMap.put(createDateStr, count + 1);
						} else {
							cacheMap.put(createDateStr, 1);
						}
					}
				} catch (ParseException e) {
					e.printStackTrace();
				}
			});

			Map<Date, Integer> cacheMapDate = new HashMap<>();
			cacheMap.entrySet().stream().forEach(e -> {
				Date createdDate = null;
				try {
					createdDate = new SimpleDateFormat("dd-MMM-yyyy").parse(e.getKey());
				} catch (ParseException e1) {
					e1.printStackTrace();
				}
				cacheMapDate.put(createdDate, e.getValue());
			});

			Map<Date, Integer> sortedMap = new TreeMap<Date, Integer>(cacheMapDate);
			Map<Date, Integer> reverseSortedMap = new TreeMap<Date, Integer>(Collections.reverseOrder());
			reverseSortedMap.putAll(sortedMap);
			reverseSortedMap.entrySet().stream().forEach(e -> {
				String createdDateStr = null;
				createdDateStr = new SimpleDateFormat("dd-MMM-yyyy").format(e.getKey());

				JsonObject json = new JsonObject();
				json.put("date", createdDateStr);
				json.put("count", e.getValue());
				json.put("profilecount", 0);

				JsonArray jsonArr = data.getJsonArray("profiles");
				for (int k = 0; k < jsonArr.size(); k++) {
					JsonObject json1 = jsonArr.getJsonObject(k);
					String date = json1.getString("date");
					Integer count = json1.getInteger("count");
					if (createdDateStr.equalsIgnoreCase(date)) {
						json.put("profilecount", count);
						break;
					}
				}

				dietCacheList.add(json);
			});

			if (null != dietCacheList && dietCacheList.size() > 0)
				getCsvDietPlanCacheFile(response, dietCacheList, traceId);

			promise.complete(response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get customer profiles for dietplan.
	 * 
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getCustomerProfilesForDietPlans(String traceId) {
		String method = "MongoRepositoryWrapper getCustomerProfilesForDietPlans() " + traceId;
		JsonObject response = new JsonObject();
		Promise<JsonObject> promise = Promise.promise();
		JsonArray jArr = new JsonArray();
		List<String> list = new ArrayList<>();
		String fromDateToBefetched = ApiUtils.getFilteredDate(config.getInteger("fetchCustProfilesDays", 30));
		JsonObject query = new JsonObject()
				.put("profile.createdDate", new JsonObject().put("$gte", fromDateToBefetched))
				.put("diet", new JsonObject().put("$ne", JsonObject.mapFrom(null)));

		client.rxFind("CUST_PROFILE", query).map(map -> {
			return map;
		}).subscribe(res -> {
			Map<String, Integer> profilesMap = new HashMap<>();
			res.forEach(action -> {
				String createdDateStr = action.getJsonObject("profile").getString("createdDate");
				String profile = action.getString("_id");

				Calendar calendar = Calendar.getInstance();
				calendar.add(Calendar.DAY_OF_MONTH, -30);
				try {
					list.add(createdDateStr + ":::" + profile);
					if (profilesMap.containsKey(createdDateStr)) {
						Integer count = profilesMap.get(createdDateStr);
						profilesMap.put(createdDateStr, count + 1);
					} else {
						profilesMap.put(createdDateStr, 1);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			});

			if (null != profilesMap) {
				Map<Date, Integer> profilesMapDate = new HashMap<>();
				profilesMap.entrySet().stream().forEach(e -> {
					Date createdDate = null;
					try {
						createdDate = new SimpleDateFormat("dd-MMM-yyyy").parse(e.getKey());
					} catch (ParseException e1) {
						e1.printStackTrace();
					}
					profilesMapDate.put(createdDate, e.getValue());
				});

				Map<Date, Integer> sortedMap = new TreeMap<Date, Integer>(profilesMapDate);
				Map<Date, Integer> reverseSortedMap = new TreeMap<Date, Integer>(Collections.reverseOrder());
				reverseSortedMap.putAll(sortedMap);
				reverseSortedMap.entrySet().stream().forEach(e -> {
					String createdDateStr = null;
					createdDateStr = new SimpleDateFormat("dd-MMM-yyyy").format(e.getKey());

					JsonObject json = new JsonObject();
					json.put("date", createdDateStr);
					json.put("count", e.getValue());

					jArr.add(json);
				});
			}
			response.put("profiles", jArr);
			promise.complete(response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Gather dietplan via cache into csv.
	 * 
	 * @param response
	 * @param paymentanalytics
	 * @param traceId
	 * @return JsonObject
	 */
	private JsonObject getCsvDietPlanCacheFile(JsonObject response, List<JsonObject> paymentanalytics, String traceId) {
		String method = "MongoRepositoryWrapper getCsvDietPlanCacheFile() " + traceId;
		CSVWriter writer = null;
		try {
			writer = new CSVWriter(new FileWriter("/home/ubuntu/fightitaway/dietcacheanalytics.csv"));
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		String[] header = { "DATE", "COUNT" };
		List<String[]> records = paymentanalytics.stream().map(mapper -> {
			logger.debug("##### " + method + " MAPPER -->> " + mapper);
			String[] record = { mapper.getString("date"), mapper.getInteger("count").toString() };
			return record;
		}).collect(Collectors.toList());

		List<String[]> analytics = new ArrayList<>();
		analytics.add(header);
		analytics.addAll(records);

		// Writing data to the csv file
		try {
			writer.writeAll(analytics);
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		response.put("code", "0000");
		response.put("message", "File saved successfully.");

		// message info
		String mailTo = "draghvendra3101@gmail.com";
		String subject = "New diet cache analtyics email with attachments";
		String message = "Customer Diet Cache Analytics";

		// attachments
		String[] attachFiles = new String[3];
		attachFiles[0] = "/home/ubuntu/fightitaway/dietcacheanalytics.csv";
		try {
			Integer analyticsSize = 0;
			if (null != analytics && analytics.size() > 0) {
				analyticsSize = analytics.size();
				response = sendEmailWithAttachments(response, mailTo, subject, message, attachFiles, analyticsSize,
						traceId);
			}
		} catch (Exception ex) {
			response.put("mailSent", false);
			logger.debug(
					"##### " + method + " COULD NOT SEND EMAIL FOR DIETPLAN CACHE ANALYTICS -->> " + ex.getMessage());
			ex.printStackTrace();
		}

		return response;
	}

	/**
	 * Create empty document for customer profile if email received as null.
	 * 
	 * @param request
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> createEmptyDocument(JsonObject request, String traceId) {
		String method = "MongoRepositoryWrapper createEmptyDocument() " + traceId + "-[" + request.getString("name")
				+ "] ";
		Promise<JsonObject> promise = Promise.promise();
		if (null == request.getString("email") || "".equalsIgnoreCase(request.getString("email"))
				|| "null".equalsIgnoreCase(request.getString("email"))) {
			client.rxSave("CUST_EMPTY_PROFILE", request).subscribe(res -> {
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		}

		promise.complete(new JsonObject());
		return promise.future();
	}

	/**
	 * Logging customer request ie. which module customer is hitting.
	 * 
	 * @param email
	 * @param uri
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> logCustRequest(String email, String uri, String traceId) {
//		String method = "MongoRepositoryWrapper logCustRequest() " + traceId + "-[" + email + "]";
//		logger.debug("##### " + method + " URI -->> " + uri);
		Promise<JsonObject> promise = Promise.promise();
//		JsonObject logRequest = new JsonObject();
//		logRequest.put("email", email);
//		logRequest.put("request", uri);
//		try {
//			logRequest.put("createdDate", new SimpleDateFormat("dd-MMM-yyyy")
//					.format(new SimpleDateFormat("ddMMyyyy").parse(ApiUtils.getCurrentDate(email, traceId))));
//			logRequest.put("createdDateTime", ApiUtils.getCurrentDateTime());
//		} catch (ParseException ex) {
//			logger.error("##### " + method + " EXCEPTION -->> " + ex.getMessage());
//			logRequest.put("createdDate", new Date());
//			logRequest.put("createdDateTime", new Date());
//		}

//		logger.debug("##### " + method + " LOG REQUEST [" + uri + " ] -->> " + logRequest);
//		client.rxSave("CUST_REQUESTS", logRequest).subscribe(res -> {
//			logger.debug("##### " + method + " CUSTOMER REQUEST [" + uri + " ] LOGGED SUCCESSFULLY.");
//			promise.complete(new JsonObject().put("code", "0000").put("message", "success"));
//		}, (ex) -> {
//			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
//			promise.fail(ex.getMessage());
//		});

		promise.complete(new JsonObject().put("code", "0000").put("message", "success"));
		return promise.future();
	}

	/**
	 * Fetch customer profile.
	 * 
	 * @param email
	 * @param request
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> fetchCustomerProfile(String email, JsonObject request, String traceId) {
		String method = "MongoRepositoryWrapper fetchCustomerProfile() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);
		logger.info("##### " + method + " QUERY -->> " + query);
		JsonObject json = new JsonObject();
		client.rxFindOne("CUST_PROFILE", query, null).subscribe(res -> {
			json.put("code", "0001");
			json.put("message", "Customer profile not found.");
			if (res == null || res.isEmpty()) {
				logger.info("##### " + method + " CUSTOMER PROFILE IS NOT AVAILABLE");
			} else {
				json.put("code", "0000");
				json.put("message", "success");
			}

			promise.complete(json);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Update dietplan time.
	 * 
	 * @param email
	 * @param request
	 * @param traceId
	 * 
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> updateTime(String email, JsonObject request, String traceId) {
		String method = "MongoRepositoryWrapper updateTime() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);
		JsonObject response = new JsonObject();

		JsonObject update = new JsonObject().put("$set", new JsonObject().put("lifeStyle", request));
		logger.info("##### " + method + " UPDATE LIFESTYLE QUERY  -->> " + query);
		logger.info("##### " + method + " UPDATE LIFESTYLE UPDATE -->> " + update);
		client.rxUpdateCollection("CUST_PROFILE", query, update).subscribe(res -> {
			response.put("code", "0000");
			response.put("message", "Success");

			promise.complete(response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get default selective items basis community..
	 * 
	 * @param traceId
	 * @return Future<FilterData>
	 */
	public Future<FilterData> getDefaultSelectiveItemsByCommunity(String traceId) {
		String method = "MongoRepositoryWrapper getDefaultSelectiveItemsByCommunity() " + traceId;
		Promise<FilterData> promise = Promise.promise();
		FilterData profile = new FilterData();
		JsonObject query = new JsonObject();
		query.put("community", "P"); // DEFAULT COMMUNITY
		client.rxFindOne("CUST_SELECTIVE_ITEMS", query, null).subscribe(res -> {
			String foodType = "V"; // DEFAULT FOOD TYPE
			logger.debug("##### " + method + " FOOD TYPE -->> " + foodType);
			SelectiveItems selectiveItems = new SelectiveItems();
			selectiveItems.setCommunity(res.getString("community"));
			selectiveItems.setFoodType(profile.getFoodType());
			JsonObject data = res.getJsonObject("data");
			DietItems dietItems = new DietItems();
			List<String> drinks = (List<String>) data.getJsonArray("drinks").stream().map(json -> json.toString())
					.collect(Collectors.toList());
			dietItems.setDrinks(drinks);
			List<String> fruits = (List<String>) data.getJsonArray("fruits").stream().map(json -> json.toString())
					.collect(Collectors.toList());
			dietItems.setFruits(fruits);
			List<String> pulsesOrCurries = (List<String>) data.getJsonArray("pulsesOrCurries").stream()
					.map(json -> json.toString()).collect(Collectors.toList());
			List<String> fullPulsesOrCurries = new ArrayList<>();
			fullPulsesOrCurries.addAll(pulsesOrCurries);

			dietItems.setPulsesOrCurries(fullPulsesOrCurries);
			List<String> snacks = (List<String>) data.getJsonArray("snacks").stream().map(json -> json.toString())
					.collect(Collectors.toList());
			dietItems.setSnacks(snacks);
			List<String> dishes = (List<String>) data.getJsonArray("dishes").stream().map(json -> json.toString())
					.collect(Collectors.toList());
			List<String> fullDishesOrSubjies = new ArrayList<>();
			fullDishesOrSubjies.addAll(dishes);
			dietItems.setDishes(fullDishesOrSubjies);

			List<String> rice = (List<String>) data.getJsonArray("rice").stream().map(json -> json.toString())
					.collect(Collectors.toList());
			dietItems.setRice(rice);
			selectiveItems.setDietItems(dietItems);
			profile.setSelectiveItems(selectiveItems);

			promise.complete(profile);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Fetch Customer profiles who were not updated lifetyle/diet.
	 * 
	 * @param data    - FilterData
	 * @param traceId
	 * 
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> fetchCustomerProfilesForPendingRegistration(FilterData data, String traceId) {
		String method = "MongoRepositoryWrapper fetchCustomerProfilesForPendingRegistration() " + traceId;
		JsonObject response = new JsonObject();
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject()
				.put("profile.createdDate", ApiUtils.getFilteredDate(config.getInteger("days")))
				.put("demographic", new JsonObject().put("$ne", JsonObject.mapFrom(null)))
				.put("diet", new JsonObject().put("$eq", JsonObject.mapFrom(null)));
		client.rxFind("CUST_PROFILE", query).map(map -> {
			return map;
		}).subscribe(res -> {
			JsonObject json = new JsonObject();
			json.put("lifeStyleCount", 0);
			json.put("dietCount", 0);
			List<WriteModel<Document>> writes = new ArrayList<>();
			List<JsonObject> completedDiets = new ArrayList<>();
			JsonObject diet = new JsonObject();
			diet.put("drinks", data.getSelectiveItems().getDietItems().getDrinks());
			diet.put("snacks", data.getSelectiveItems().getDietItems().getSnacks());
			diet.put("fruits", data.getSelectiveItems().getDietItems().getFruits());
			diet.put("dishes", data.getSelectiveItems().getDietItems().getDishes());
			diet.put("pules", data.getSelectiveItems().getDietItems().getPulsesOrCurries());
			diet.put("rice", data.getSelectiveItems().getDietItems().getRice());

			JsonArray manualProfilesEmailsArr = new JsonArray();
			JsonArray completedProfilesEmailsArr = new JsonArray();

			Calendar cal = Calendar.getInstance();
			cal.setTime(new Date());
			cal.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
			cal.add(Calendar.MINUTE, 30); // add 30 minutes

			boolean isProfilesFound = false;
			if (null != res && !res.isEmpty()) {
				logger.info("##### " + method + " RES SIZE -->> " + res.size());
				res.forEach(action -> {

					Integer lifeStyleCount = 0;
					Integer dietCount = 0;
					// logger.info("##### " + method + " ACTION -->> " + action);
					Double suggestedWeight = action.getJsonObject("demographic").getDouble("suggestedWeight");
					Double currentWeight = action.getJsonObject("demographic").getJsonObject("weight")
							.getDouble("value");
					Integer numberDays = ApiUtils.getNumberOfDaysFromWeight(currentWeight, suggestedWeight);
					Calendar calendar = Calendar.getInstance();
					calendar.setTime(new Date());
					calendar.add(Calendar.DATE, Math.abs(numberDays));
					diet.put("dateBy", new SimpleDateFormat("dd-MMM-yyyy").format(calendar.getTime()));
					diet.put("suggestedWeight", suggestedWeight);
					if (null != action.getJsonObject("lifeStyle")) {
						if (null != action.getJsonObject("diet")) {
							completedDiets.add(action);
							completedProfilesEmailsArr.add(action.getString("_id"));
						} else {
							// logger.info("##### " + method + " INSIDE DIET ELSE -->> " + action);
							writes.add(new UpdateOneModel<Document>(new Document("_id", action.getString("_id")), // filter
									new Document("$set", new Document("profile", action.getJsonObject("profile"))
											.append("demographic", action.getJsonObject("demographic"))
											.append("lifeStyle", action.getJsonObject("lifeStyle")).append("diet", diet)
											.append("filledUpto", 3).append("isManuallyAdded", true)
											.append("manuallyAddedOn",
													new SimpleDateFormat("dd-MMM-yyyy").format(cal.getTime())))));

							++dietCount;
							json.put("dietCount", json.getInteger("dietCount") + 1);

							manualProfilesEmailsArr.add(action.getString("_id"));
						}
					} else {
						CalculationData cData = new CalculationData();
						cData.setAge(action.getJsonObject("demographic").getJsonObject("age").getInteger("avg_age"));
						cData.setGender(action.getJsonObject("demographic").getJsonObject("gender").getString("code"));
						cData.setHeight(action.getJsonObject("demographic").getJsonObject("height").getDouble("value"));
						cData.setWeight(currentWeight);
						cData.setSuggestedWeight(suggestedWeight);

						float activity = 1.3f;
						// logger.info("##### " + method + " ACTIVITY -->> " + activity);

						cData.setActivityUnit(activity);

						CalculationResult result = ApiUtils.calculatePercentage(cData, traceId);
						// logger.info("##### " + method + " CalculationResult GOT IT -->> " +
						// result.toString());

						JsonObject lifeStyle = new JsonObject();
						lifeStyle.put("diseases", new JsonArray());

						JsonObject activities = new JsonObject();
						activities.put("code", "AC3");
						activities.put("value", 1.3);
						lifeStyle.put("activities", activities);

						JsonObject wakeup = new JsonObject();
						wakeup.put("code", "W5");
						lifeStyle.put("wakeup", wakeup);

						JsonObject leaveForOffice = new JsonObject();
						leaveForOffice.put("code", "LFO3");
						leaveForOffice.put("value", "9:00 - 10:00 AM");
						leaveForOffice.put("otherValue", "");
						lifeStyle.put("leaveForOffice", leaveForOffice);

						JsonArray communities = new JsonArray();
						communities.add("U");
						communities.add("P");
						lifeStyle.put("communities", communities);

						lifeStyle.put("foodType", "V");
						lifeStyle.put("carb", result.getCarb());
						lifeStyle.put("protien", result.getProtien());
						lifeStyle.put("fat", result.getFat());
						lifeStyle.put("fiber", result.getFiber());
						lifeStyle.put("calories", result.getCalories().intValue());

						writes.add(new UpdateOneModel<Document>(new Document("_id", action.getString("_id")), // filter
								new Document("$set", new Document("profile", action.getJsonObject("profile"))
										.append("demographic", action.getJsonObject("demographic"))
										.append("lifeStyle", lifeStyle).append("diet", diet).append("filledUpto", 2)
										.append("isManuallyAdded", true).append("manuallyAddedOn",
												new SimpleDateFormat("dd-MMM-yyyy").format(cal.getTime())))));

						++lifeStyleCount;
						json.put("lifeStyleCount", json.getInteger("lifeStyleCount") + 1);
						manualProfilesEmailsArr.add(action.getString("_id"));
					}
				});

				if (null != writes && writes.size() > 0) {
					isProfilesFound = true;
					logger.info("##### " + method + " WRITES SIZE -->> " + writes.size());
				} else {
					logger.info("##### " + method + " WRITES -->> " + writes);
				}
			}

			response.put("code", "0000");
			response.put("message", "Success");
			response.put("isProfilesFound", isProfilesFound);
			response.put("action", writes);
			response.put("completedDiets", completedDiets);
			response.put("lifeStyleCount", json.getInteger("lifeStyleCount"));
			response.put("dietCount", json.getInteger("dietCount"));
			response.put("manualProfilesEmails", manualProfilesEmailsArr);
			response.put("completedProfilesEmails", completedProfilesEmailsArr);
			logger.debug("##### " + method + " RESPONSE -->> " + response);

			promise.complete(response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Bulk update - Customer profiles if either lifestyles or diet plan are not
	 * filled.
	 * 
	 * @param request
	 * @param traceId
	 * 
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> updateCustomerProfilesInBulk(JsonObject request, String traceId) {
		String method = "MongoRepositoryWrapper updateCustomerProfilesInBulk() " + traceId;
		Promise<JsonObject> promise = Promise.promise();
		if (null != request && request.getBoolean("isProfilesFound")) {
			JsonArray jsonArr = request.getJsonArray("action");
			List<BulkOperation> bulkOperations = new ArrayList<>();
			for (int i = 0; i < jsonArr.size(); i++) {
				JsonObject json = new JsonObject();
				@SuppressWarnings("unchecked")
				UpdateOneModel<Document> write = (UpdateOneModel<Document>) jsonArr.getList().get(i);
				String updateModel = write.getUpdate().toString().replace("Document", "").replace("$set=", "")
						.replace("profile=", "\"profile\":").replace("demographic=", "\"demographic\":")
						.replace("lifeStyle=", "\"lifeStyle\":").replace("diet=", "\"diet\":")
						.replace("filledUpto=", "\"filledUpto\":").replace("isManuallyAdded=", "\"isManuallyAdded\":")
						.replace("manuallyAddedOn=", "\"manuallyAddedOn\":\"");
				updateModel = updateModel.substring(3, updateModel.length() - 4) + "\"}";
				JsonObject jsonObject = new JsonObject(updateModel);
				String filter = write.getFilter().toString().replace("Document{", "").replace("_id=", "\"_id\":\"");
				filter = filter.substring(0, filter.length() - 2) + "\"}";
				JsonObject jsonObjectFilter = new JsonObject(filter);

				json.put("_id", jsonObjectFilter.getString("_id"));
				json.put("profile", jsonObject.getJsonObject("profile"));
				json.put("demographic", jsonObject.getJsonObject("demographic"));
				json.put("lifeStyle", jsonObject.getJsonObject("lifeStyle"));
				json.put("diet", jsonObject.getJsonObject("diet"));
				json.put("filledUpto", (jsonObject.getInteger("filledUpto") == 2 ? "demographic" : "lifeStyle"));
				json.put("isManuallyAdded", jsonObject.getBoolean("isManuallyAdded"));
				json.put("manuallyAddedOn", jsonObject.getString("manuallyAddedOn"));
				try {
					bulkOperations
							.add(BulkOperation.createReplace(jsonObjectFilter, json).setUpsert(true).setMulti(false));
				} catch (Exception ex) {
					logger.error("##### " + method + " EXCEPTION -->> " + ex.getMessage());
					ex.printStackTrace();
				}
			}

			if (null != bulkOperations && bulkOperations.size() > 0) {
				client.rxBulkWrite("CUST_PROFILE", bulkOperations).subscribe(res -> {
					logger.info("##### " + method + " [" + bulkOperations.size() + "] PROFILES UPDATED.");
				}, (ex) -> {
					logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
					promise.fail(ex.getMessage());
				});
			} else {
				logger.info("##### " + method + " NO RECORD FOUND FOR MANUAL UPDATE.");
			}
		} else {
			logger.info("##### " + method + " NO RECORD FOUND.");
		}

		promise.complete(request);
		return promise.future();
	}

	/**
	 * Fetch Customer profiles who were not updated lifestyle/diet.
	 * 
	 * @param data    - FilterData
	 * @param traceId
	 * 
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> fetchCustomerProfilesAlreadyUpdatedManually(String traceId) {
		String method = "MongoRepositoryWrapper fetchCustomerProfilesAlreadyUpdatedManually() " + traceId;
		JsonObject response = new JsonObject();
		Promise<JsonObject> promise = Promise.promise();

		JsonObject query = new JsonObject();
		query.put("isManuallyAdded", true);
		query.put("manuallyAddedOn", ApiUtils.getFilteredDate(config.getInteger("days")));
		JsonArray emails = new JsonArray();
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFind("CUST_PROFILE", query).map(map -> {
			return map;
		}).subscribe(res -> {
			if (null != res && !res.isEmpty())
				logger.info("##### " + method + " RES SIZE -->> " + res.size());

			res.forEach(action -> {
				String email = action.getString("_id");
				if (null != emails && !emails.contains(email))
					emails.add(email);
			});

			boolean isProfilesFound = false;
			response.put("code", "0000");
			response.put("message", "Success");
			Integer emailSize = 0;
			if (null != emails && emails.size() > 0) {
				isProfilesFound = true;
				emailSize = emails.size();
				logger.info("##### " + method + " EMAILS SIZE -->> " + emails.size());
			}

			response.put("isProfilesFound", isProfilesFound);
			response.put("manualProfilesEmails", emails);
			response.put("manualProfilesEmailsSize", emailSize);

			promise.complete(response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Bulk update - Payment detail for create orders.
	 * 
	 * @param request
	 * @param traceId
	 * 
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> updatePaymentDetailsInBulk(JsonObject request, String traceId) {
		String method = "MongoRepositoryWrapper updatePaymentDetailsInBulk() " + traceId;
		Promise<JsonObject> promise = Promise.promise();
		if (null != request && request.getBoolean("isProfilesFound")) {
			List<String> list = new ArrayList<>();
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
			SimpleDateFormat myFormat = new SimpleDateFormat("dd-MMM-yyyy");
			Integer freeAccountValidityInDays = this.config.getInteger("freeAccountValidity");
			Calendar cal = Calendar.getInstance(); // creates calendar
			cal.setTime(new Date()); // sets calendar time/date
			cal.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
			cal.add(Calendar.MINUTE, 30); // add 30 minutes

			JsonArray jsonArr = request.getJsonArray("manualProfilesEmails");
			JsonArray paymentRequestArr = new JsonArray();
			JsonArray emails = new JsonArray();
			List<BulkOperation> bulkOperations = new ArrayList<>();
			jsonArr.forEach(action -> {
				String emailId = action.toString();
				if (!list.contains(emailId)) {
					JsonObject payload = new JsonObject();
					payload.put("emailId", emailId);
					@SuppressWarnings("deprecation")
					String uniqueId = RandomStringUtils.randomAlphanumeric(14);
					String orderId = "order_" + uniqueId;
					payload.put("orderId", orderId);
					payload.put("amount", 0);
					payload.put("couponCode", "CU0000");
					String createdDate = dateFormat.format(cal.getTime());
					payload.put("createdDate", createdDate);
					String paymentId = "pay_" + uniqueId;
					payload.put("paymentId", paymentId);
					boolean isAccountFree = true;
					payload.put("isAccountFree", isAccountFree);
					payload.put("durationInDays", freeAccountValidityInDays);
					String txnId = UUID.randomUUID().toString();
					payload.put("txnId", txnId);

					String dateBeforeString2 = myFormat.format(cal.getTime());
					String dateAfterString2 = this.config.getString("cutOffDate");
					try {
						Date dateBefore2 = myFormat.parse(dateBeforeString2);
						Date dateAfter2 = myFormat.parse(dateAfterString2);
						long difference = dateAfter2.getTime() - dateBefore2.getTime();
						float noOfDaysBetween = (difference / (1000 * 60 * 60 * 24));
						payload.put("validityInDays", 0);
						if (noOfDaysBetween >= 0)
							payload.put("validityInDays", Math.round(noOfDaysBetween));
					} catch (Exception e) {
						e.printStackTrace();
					}

					@SuppressWarnings("deprecation")
					String signature = RandomStringUtils.randomAlphanumeric(65);
					payload.put("signature", signature);
					payload.put("status", "SUCCESS");
					payload.put("updatedDate", createdDate);
					payload.put("isManuallyAdded", true);
					payload.put("manuallyAddedOn", myFormat.format(cal.getTime()));
					bulkOperations.add(BulkOperation.createInsert(payload));

					paymentRequestArr.add(payload);
					emails.add(emailId);
				}
			});

			if (null != emails && emails.size() > 0) {
				try {
					request.put("paymentRequest", paymentRequestArr);
					request.put("manualProfilesEmails", emails);
					if (null != bulkOperations && bulkOperations.size() > 0) {
						client.rxBulkWrite("PAYMENT_DETAIL", bulkOperations).subscribe(res -> {
							logger.info(
									"##### " + method + " [" + bulkOperations.size() + "] PAYMENT DETAILS CREATED.");
						}, (ex) -> {
							logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
							promise.fail(ex.getMessage());
						});
					} else {
						logger.info("##### " + method + " NO RECORD FOUND FOR MANUAL UPDATE.");
					}
				} catch (Exception ex) {
					logger.error("##### " + method + " EXCEPTION -->> " + ex.getMessage());
					ex.printStackTrace();
				}
			}
		} else {
			logger.info("##### " + method + " NO RECORD FOUND.");
		}

		promise.complete(request);
		return promise.future();
	}

	/**
	 * Fetch Customer profiles who were not updated lifetyle/diet.
	 * 
	 * @param data    - FilterData
	 * @param traceId
	 * 
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> fetchPaymentDetailsAlreadyUpdatedManually(JsonObject data, String traceId) {
		String method = "MongoRepositoryWrapper fetchPaymentDetailsAlreadyUpdatedManually() " + traceId;
		JsonObject response = new JsonObject();
		Promise<JsonObject> promise = Promise.promise();

		JsonObject query = new JsonObject();
		query.put("isManuallyAdded", true);
		query.put("amount", 0);
		query.put("manuallyAddedOn", ApiUtils.getFilteredDate(0));
		JsonArray emails = new JsonArray();
		JsonArray ids = new JsonArray();

		if (data.getBoolean("isProfilesFound")) {
			logger.info("##### " + method + " QUERY -->> " + query);
			client.rxFind("PAYMENT_DETAIL", query).map(map -> {
				return map;
			}).subscribe(res -> {
				if (null != res && !res.isEmpty()) {
					logger.info("##### " + method + " RES SIZE -->> " + res.size());
					res.forEach(action -> {
						String email = action.getString("emailId");
						String _id = action.getString("_id");
						if (null != emails && !emails.contains(email)) {
							emails.add(email);
						} else {
							ids.add(_id);
						}
					});

					data.put("ids", ids);
					logger.debug("##### " + method + " RESPONSE 1  -->> " + data);
					logger.debug("##### " + method + " USUNSED IDS -->> " + response);
				}

				promise.complete(data);
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		}

		return promise.future();
	}

	/**
	 * Remove payment details.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> removePaymentDetailsAlreadyUpdatedManually(JsonObject data, String traceId) {
		String method = "MongoRepositoryWrapper removePaymentDetailsAlreadyUpdatedManually() " + traceId;
		Promise<JsonObject> promise = Promise.promise();

		JsonObject query = new JsonObject();
		query.put("isManuallyAdded", true);
		query.put("amount", 0);
		query.put("manuallyAddedOn", ApiUtils.getFilteredDate(0));
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxRemoveDocuments("PAYMENT_DETAIL", query).subscribe(res -> {
		}, (ex) -> {
			logger.info("##### " + method + " ERROR -->> " + ex.getMessage());

		});

		promise.complete(data);
		return promise.future();
	}

	/**
	 * Remove payment details.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> removePaymentDetailsAlreadyUpdatedManually2(JsonObject data, String traceId) {
		String method = "MongoRepositoryWrapper removePaymentDetailsAlreadyUpdatedManually2() " + traceId;
		Promise<JsonObject> promise = Promise.promise();
		JsonArray ids = data.getJsonArray("ids");
		if (null != ids && ids.size() > 0) {
			synchronized (ids) {
				ids.forEach(_id -> {
					JsonObject query = new JsonObject();
					query.put("_id", _id);
					query.put("isManuallyAdded", true);
					query.put("amount", 0);
					query.put("manuallyAddedOn", ApiUtils.getFilteredDate(0));
					client.rxFindOneAndDelete("PAYMENT_DETAIL", query).subscribe(res -> {
						logger.info("##### " + method + " [" + _id + "] REMOVED FROM PAYMENT_DETAILS.");
					}, (ex) -> {
						logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
						promise.fail(ex.getMessage());
					});
				});

			}
		}

		promise.complete(data);
		return promise.future();
	}

	/**
	 * Remove payment details.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> removeSubscriptionPlansAlreadyUpdatedManually(JsonObject data, String traceId) {
		String method = "MongoRepositoryWrapper removeSubscriptionPlansAlreadyUpdatedManually() " + traceId;
		Promise<JsonObject> promise = Promise.promise();
		JsonArray ids = data.getJsonArray("ids");
		if (null != ids && ids.size() > 0) {
			JsonObject query = new JsonObject();
			query.put("isManuallyAdded", true);
			query.put("amount", 0);
			query.put("manuallyAddedOn", ApiUtils.getFilteredDate(0));
			logger.info("##### " + method + " QUERY -->> " + query);
			client.rxRemoveDocuments("PLAN_SUBCRIPTION_DETAIL", query).subscribe(res -> {
			}, (ex) -> {
				logger.info("##### " + method + " ERROR -->> " + ex.getMessage());

			});
		}

		promise.complete(data);
		return promise.future();
	}

	/**
	 * Bulk update - Subscription plan.
	 * 
	 * @param request
	 * @param traceId
	 * 
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> updateSubscriptionPlansInBulk(JsonObject request, String traceId) {
		String method = "MongoRepositoryWrapper updateSubscriptionPlansInBulk() " + traceId + "] ";
		Promise<JsonObject> promise = Promise.promise();
		if (null != request && request.getBoolean("isProfilesFound")) {
			logger.info("##### " + method + " PROFILES FOUND");
			Calendar cal1 = Calendar.getInstance(); // creates calendar
			cal1.setTime(new Date()); // sets calendar time/date
			cal1.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
			cal1.add(Calendar.MINUTE, 30); // add 30 minutes

			JsonArray paymentRequestArr = request.getJsonArray("paymentRequest");
			logger.info("##### " + method + " PAYMENT REQUEST ARRAY -->> " + paymentRequestArr);
			List<BulkOperation> bulkOperations = new ArrayList<>();
			List<String> list = new ArrayList<>();
			if (null != paymentRequestArr && paymentRequestArr.size() > 0) {
				Calendar cal = Calendar.getInstance(); // creates calendar
				cal.setTime(new Date()); // sets calendar time/date
				cal.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
				cal.add(Calendar.MINUTE, 30); // add 30 minutes
				paymentRequestArr.forEach(action -> {
					JsonObject json = (JsonObject) action;
					if (!list.contains(json.getString("emailId"))) {
						JsonObject payload = new JsonObject();
						payload.put("emailId", json.getString("emailId"));
						payload.put("orderId", json.getString("orderId"));
						payload.put("paymentId", json.getString("paymentId"));
						payload.put("couponCode", json.getString("couponCode"));
						payload.put("createdDate", json.getString("createdDate"));
						payload.put("txnId", json.getString("txnId"));
						payload.put("isActive", true);
						payload.put("signature", json.getString("signature"));
						Integer validityInDays = json.getInteger("validityInDays");
						if (null == validityInDays)
							validityInDays = 0;
						Calendar calendar = Calendar.getInstance();
						calendar.add(Calendar.DAY_OF_MONTH, validityInDays);
						calendar.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
						calendar.add(Calendar.MINUTE, 30); // add 30 minutes
						payload.put("expiryDate", new SimpleDateFormat("dd-MMM-yyyy").format(calendar.getTime()));
						payload.put("upgradedDate", json.getString("createdDate"));
						payload.put("amount", json.getInteger("amount"));
						payload.put("isUpgraded", false);
						payload.put("isFreeAccountTaken", true);
						payload.put("isManuallyAdded", json.getBoolean("isManuallyAdded"));
						payload.put("manuallyAddedOn", json.getString("manuallyAddedOn"));

						bulkOperations.add(BulkOperation.createInsert(payload));
					}
				});

				try {
					if (null != bulkOperations && bulkOperations.size() > 0)
						client.rxBulkWrite("PLAN_SUBCRIPTION_DETAIL", bulkOperations).subscribe(res -> {
							logger.info(
									"##### " + method + " [" + bulkOperations.size() + "] PAYMENT DETAILS CREATED.");
						}, (ex) -> {
							logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
							promise.fail(ex.getMessage());
						});
					else
						logger.info("##### " + method + " NO RECORD FOUND FOR MANUAL UPDATE.");

				} catch (Exception ex) {
					logger.error("##### " + method + " EXCEPTION -->> " + ex.getMessage());
					ex.printStackTrace();
				}
			}
		} else {
			logger.info("##### " + method + " NO RECORD FOUND.");
		}

		promise.complete(request);
		return promise.future();
	}

	/**
	 * Fetch Customer profiles who were not updated lifetyle/diet.
	 * 
	 * @param data    - FilterData
	 * @param traceId
	 * 
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> fetchSubscriptionPlansAlreadyUpdatedManually(JsonObject data, String traceId) {
		String method = "MongoRepositoryWrapper fetchSubscriptionPlansAlreadyUpdatedManually() " + traceId;
		Promise<JsonObject> promise = Promise.promise();

		JsonObject query = new JsonObject();
		query.put("isManuallyAdded", true);
		query.put("amount", 0);
		query.put("manuallyAddedOn", ApiUtils.getFilteredDate(0));
		JsonArray emails = new JsonArray();
		JsonArray ids = new JsonArray();
		if (data.getBoolean("isProfilesFound")) {
			logger.info("##### " + method + " QUERY -->> " + query);
			client.rxFind("PLAN_SUBCRIPTION_DETAIL", query).map(map -> {
				return map;
			}).subscribe(res -> {
				if (null != res && !res.isEmpty()) {
					logger.info("##### " + method + " RES SIZE -->> " + res.size());
					res.forEach(action -> {
						String email = action.getString("emailId");
						String _id = action.getString("_id");
						if (null != emails && !emails.contains(email)) {
							emails.add(email);
						} else {
							ids.add(_id);
						}
					});

					data.put("ids", ids);
					logger.info("##### " + method + " RECORDS SIZE  -->> " + ids.size());
				}

				promise.complete(data);
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		}

		return promise.future();
	}

	/**
	 * Remove payment details.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> removeSubscriptionPlansAlreadyUpdatedManually2(JsonObject data, String traceId) {
		String method = "MongoRepositoryWrapper removeSubscriptionPlansAlreadyUpdatedManually2()" + traceId;
		Promise<JsonObject> promise = Promise.promise();
		JsonArray ids = data.getJsonArray("ids");
		if (null != ids && ids.size() > 0) {
			ids.forEach(_id -> {
				JsonObject query = new JsonObject();
				query.put("_id", _id);
				query.put("isManuallyAdded", true);
				query.put("amount", 0);
				query.put("manuallyAddedOn", ApiUtils.getFilteredDate(0));
				client.rxFindOneAndDelete("PLAN_SUBCRIPTION_DETAIL", query).subscribe(res -> {
					logger.info(
							"##### " + method + " [" + ids.size() + "] RECORDS REMOVED FROM PLAN_SUBCRIPTION_DETAIL.");
				}, (ex) -> {
					logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
					promise.fail(ex.getMessage());
				});
			});
		}

		promise.complete(data);
		return promise.future();
	}

	/**
	 * Move RecommendedIn Items to top
	 * 
	 * @param items
	 * @param dbResult
	 * @param dietList
	 * @param item
	 * @param slot
	 * @param traceId
	 * 
	 * @return finalList
	 */
	private List<JsonObject> getSlotOptions(List<JsonObject> items, DBResult dbResult, List<String> dietList,
			String item, int slot, String traceId) {
		String method = "MongoRepositoryWrapper getSlotOptions() " + traceId + "-["
				+ dbResult.getFilterData().getEmail() + "]";
		logger.info("####################################### [SLOT " + slot
				+ "]-OPTIONS ########################################");
		List<JsonObject> finalList = new ArrayList<>();
		List<String> disease = dbResult.getFilterData().getDisease();

		List<JsonObject> slotOptionsRecommendedInList = new ArrayList<>();
		List<JsonObject> slotRemainingOptionsList = new ArrayList<>();
		if (null != disease && disease.size() > 0) {
			items.forEach(action -> {
				JsonArray jsonArr = action.getJsonArray("RecommendedIn");
				List<String> listAll = new ArrayList<>();
				if (null != action && null != jsonArr && !jsonArr.isEmpty() && jsonArr.size() > 0) {
					JsonArray jArr = new JsonArray();
					jsonArr.forEach(obj -> {
						if (null != obj) {
							String food = (String) obj;
							if (disease.contains(food) && !jArr.contains(food)) {
								listAll.add(food);
								jArr.add(food);
								action.put("recommendedFor", jArr);
							}
						}
					});

					action.remove("Slots");
					action.remove("Season");
					action.remove("AvoidIn");
					action.remove("Remark");
					action.remove("Detox");
					action.remove("Special_diet");
					action.remove("courtesy");
					action.remove("recipe");
					action.remove("steps");
					action.remove("updated_by");
					action.remove("video");
					action.remove("Special_slot");
					action.remove("Ultra_special");

					if (null != listAll && listAll.size() >= 1) {
						slotOptionsRecommendedInList.add(action);
					} else {
						slotRemainingOptionsList.add(action);
					}
				} else {
					slotRemainingOptionsList.add(action);
				}
			});

			List<JsonObject> slot0OptionsRecommendedList = FoodFilterUtils.sortByCalories(slotOptionsRecommendedInList);
			List<JsonObject> slot0OptionsRemainingList = getFilteredListByOptions1(slotRemainingOptionsList, dbResult,
					dietList, item, slot);

			finalList.addAll(slot0OptionsRecommendedList);
			finalList.addAll(slot0OptionsRemainingList);

			if (null == finalList || (null != finalList && finalList.size() <= 0))
				finalList.addAll(getFilteredListByOptions1(items, dbResult, dietList, item, slot));
		} else {
			finalList.addAll(getFilteredListByOptions1(items, dbResult, dietList, item, slot));
		}

		logger.debug("##### " + method + " [SLOT " + slot + "-OPTIONS] ITEMS SIZE        -->> " + items.size());
		logger.debug("##### " + method + " [SLOT " + slot + "-OPTIONS] FINAL LIST SIZE   -->> " + finalList.size());

		logger.info("#########################################################################################");
		logger.info("");
		return finalList;
	}

	/**
	 * Update RecommendedFor attribute in the fiet food if applicable.
	 * 
	 * @param items
	 * @param dbResult
	 * @param traceId
	 * 
	 * @return finalList
	 */
	private List<JsonObject> getSlotDiets(List<JsonObject> items, DBResult dbResult, String traceId) {
		String method = "MongoRepositoryWrapper getSlotDiets() " + traceId + "-[" + dbResult.getFilterData().getEmail()
				+ "]";
		logger.info("###############################################################################");
		List<String> disease = dbResult.getFilterData().getDisease();
		List<JsonObject> dietPlans = new ArrayList<>();
		if (null != disease && disease.size() > 0) {
			items.forEach(action -> {
				JsonArray jsonArr = action.getJsonArray("RecommendedIn");
				List<String> listAll = new ArrayList<>();
				if (null != action && null != jsonArr && !jsonArr.isEmpty() && jsonArr.size() > 0) {
					JsonArray jArr = new JsonArray();
					jsonArr.forEach(obj -> {
						if (null != obj) {
							String food = (String) obj;
							if (disease.contains(food) && !jArr.contains(food)) {
								listAll.add(food);
								jArr.add(food);
								action.put("recommendedFor", jArr);
							}
						}
					});
				}

				dietPlans.add(action);
			});
		} else {
			dietPlans.addAll(items);
		}

		logger.debug("##### " + method + " ITEMS SIZE     -->> " + items.size());
		logger.debug("##### " + method + " DIETPLANS SIZE -->> " + dietPlans.size());

		logger.info("#########################################################################################");
		logger.info("");
		return dietPlans;
	}

	/**
	 * Update RecommendedFor attribute in the fiet food if applicable.
	 * 
	 * @param email
	 * @param item
	 * @param disease
	 * @param recommendedIn
	 * @param traceId
	 * 
	 * @return JsonObject
	 */
	private JsonObject updateFoodForRecommendedFor(String email, JsonObject item, JsonArray disease,
			JsonArray recommendedIn, String traceId) {
		String method = "MongoRepositoryWrapper updateFoodForRecommendedFor() " + traceId + "-[" + email + "]";
		logger.info("###############################################################################");
		if (null != disease && disease.size() > 0) {
			List<String> listAll = new ArrayList<>();
			if (null != item && null != recommendedIn && !recommendedIn.isEmpty() && recommendedIn.size() > 0) {
				JsonArray jArr = new JsonArray();
				recommendedIn.forEach(obj -> {
					if (null != obj) {
						String food = (String) obj;
						if (disease.contains(food) && !jArr.contains(food)) {
							listAll.add(food);
							jArr.add(food);
							item.put("recommendedFor", jArr);
						}
					}
				});
			}
		}

		logger.info("##### " + method + " FOOD -->> " + item);
		logger.info("#########################################################################################");
		logger.info("");
		return item;
	}

	/**
	 * Get the customer profile.
	 * 
	 * @param email
	 * @param data
	 * @param request
	 * @param traceId
	 * 
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getProfile(String email, JsonObject data, JsonObject request, String traceId) {
		String method = "MongoRepositoryWrapper getProfile() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);
		JsonObject fields = new JsonObject().put("email", "email").put("profile.name", "profile.name")
				.put("demographic.gender", "demographic.gender").put("demographic.weight", "demographic.weight")
				.put("demographic.suggestedWeight", "demographic.suggestedWeight")
				.put("lifeStyle.activities", "lifeStyle.activities").put("lifeStyle.foodType", "lifeStyle.foodType")
				.put("diet.drinks", "diet.drinks");

		client.rxFindOne("CUST_PROFILE", query, fields).subscribe(res -> {
			if (res == null || res.isEmpty()) {
				promise.fail("invalid customer");
			} else {
				data.put("email", res.getString("_id"));
				data.put("name", res.getJsonObject("profile").getString("name"));
				data.put("targetedWeight", res.getJsonObject("demographic").getDouble("suggestedWeight"));
				data.put("weight", res.getJsonObject("demographic").getJsonObject("weight").getDouble("value"));

				if (null != res.getJsonObject("lifeStyle"))
					data.put("lifeStyle", res.getJsonObject("lifeStyle").getString("foodType"));
				if (null != res.getJsonObject("diet"))
					data.put("diet", res.getJsonObject("diet").getJsonArray("drinks"));

				logger.info("##### " + method + " PROFILE DATA -->> " + data);
				promise.complete(data);
			}
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Save Dietplan if iteration reaches to 10 or more.
	 * 
	 * @param data
	 * @param email
	 * @param traceId
	 * 
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> persistDietPlanForBacktrace(JsonObject data, String email, String traceId) {
		String method = "MongoRepositoryWrapper persistDietPlanForBacktrace() " + traceId + "-[" + email + "]";
		Calendar cal1 = Calendar.getInstance(); // creates calendar
		cal1.setTime(new Date()); // sets calendar time/date
		cal1.add(Calendar.HOUR_OF_DAY, 10); // adds ten hour
		cal1.add(Calendar.MINUTE, 30); // add 30 minutes
		String currentDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(cal1.getTime());
		Promise<JsonObject> promise = Promise.promise();
		try {
			JsonObject query = new JsonObject();
			query.put("_id", new JsonObject().put("email", email).put("date", ApiUtils.getCurrentDate(email, traceId)));
			query.put("data", data);
			query.put("updatedTime", new JsonObject().put("$date", currentDate));
			client.rxSave("CUST_DAILY_DIET_LOG", query).subscribe(res -> {
				logger.info("##### " + method + " DIETPLAN SAVED SUCCESSFULLY");
				JsonObject response = new JsonObject();
				response.put("code", "0000");
				response.put("message", "success");
				promise.complete(response);
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		} catch (Exception e) {
			logger.error("##### " + method + " EXCEPTION -->> " + e.getMessage());
			e.printStackTrace();
		}

		return promise.future();
	}

	/**
	 * Fetch customer dietplan timings.
	 * 
	 * @param email
	 * @param profile
	 * @param traceId
	 * 
	 * @return Future<JsonObject>
	 */
	public Future<DBResult> fetchCustomerDietPlanTimingsForOptions(String email, DBResult dbResult, String traceId) {
		String method = "MongoRepositoryWrapper fetchCustomerDietPlanTimingsForOptions() " + traceId + "-[" + email
				+ "]";
		Promise<DBResult> promise = Promise.promise();
		dbResult.getFilterData().setEmail(email);
		JsonObject query = new JsonObject().put("_id", email);
		client.rxFindOne("CUST_DIETPLAN_TIMINGS", query, null).subscribe(res -> {
			if (res == null || res.isEmpty())
				dbResult.getFilterData().setTimings(null);
			else
				dbResult.getFilterData().setTimings(res);

			promise.complete(dbResult);
			logger.info(
					"##### " + method + " CUST_DIETPLAN_TIMINGS TIMINGS -->> " + dbResult.getFilterData().getTimings());
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Fetch customer dietplan timings.
	 * 
	 * @param email
	 * @param profile
	 * @param traceId
	 * 
	 * @return Future<JsonObject>
	 */
	public Future<FilterData> fetchCustomerDietPlanTimings(String email, FilterData profile, String traceId) {
		String method = "MongoRepositoryWrapper fetchCustomerDietPlanTimings() " + traceId + "-[" + email + "]";
		Promise<FilterData> promise = Promise.promise();
		profile.setEmail(email);
		JsonObject query = new JsonObject().put("_id", email);
		client.rxFindOne("CUST_DIETPLAN_TIMINGS", query, null).subscribe(res -> {
			if (res == null || res.isEmpty())
				profile.setTimings(null);
			else
				profile.setTimings(res);

			promise.complete(profile);
			logger.info("##### " + method + " CUST_DIETPLAN_TIMINGS TIMINGS -->> " + profile.getTimings());
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Fetch customer dietplan timings Detox.
	 * 
	 * @param email
	 * @param profile
	 * @param traceId
	 * 
	 * @return Future<JsonObject>
	 */
	public Future<FilterData> fetchCustomerDietPlanTimingsDetox(String email, FilterData profile, String traceId) {
		String method = "MongoRepositoryWrapper fetchCustomerDietPlanTimingsDetox() " + traceId + "-[" + email + "]";
		Promise<FilterData> promise = Promise.promise();
		profile.setEmail(email);
		JsonObject query = new JsonObject().put("_id", email);
		client.rxFindOne("CUST_DIETPLAN_TIMINGS_DETOX", query, null).subscribe(res -> {
			if (res == null || res.isEmpty())
				profile.setTimings(null);
			else
				profile.setTimings(res);

			promise.complete(profile);
			logger.info("##### " + method + " CUST_DIETPLAN_TIMINGS_DETOX TIMINGS -->> " + profile.getTimings());
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		logger.info("##### " + method + " REACHED AT END CUST_DIETPLAN_TIMINGS");
		return promise.future();
	}

	/**
	 * Save dietplan timings.
	 * 
	 * @param email
	 * @param data
	 * @param request
	 * @param traceId
	 * 
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> saveOrUpdateCustomerDietPlanTimings(String email, FilterData profile, JsonObject request,
			String traceId) {
		String method = "MongoRepositoryWrapper saveOrUpdateCustomerDietPlanTimings() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject oldTimings = profile.getTimings();
		JsonObject query = new JsonObject();
		Calendar cal1 = Calendar.getInstance(); // creates calendar
		cal1.setTime(new Date()); // sets calendar time/date
		cal1.add(Calendar.HOUR_OF_DAY, 10); // adds ten hour
		cal1.add(Calendar.MINUTE, 30); // add 30 minutes
		String dateTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(cal1.getTime());

		JsonObject response = new JsonObject();
		query.put("_id", email);
		if (null == oldTimings) {
			query.put("timings", request.getJsonArray("timings"));
			query.put("createdDate", ApiUtils.getFilteredDate(0));
			query.put("createdDateTime", new JsonObject().put("$date", dateTime));
			query.put("updatedDateTime", new JsonObject().put("$date", dateTime));
			logger.info("##### " + method + " (NEW TIMINGS) QUERY   -->> " + query);
			client.rxSave("CUST_DIETPLAN_TIMINGS", query).subscribe(res -> {
				response.put("code", "0000");
				response.put("message", "Success");
				response.put("timings", request.getJsonArray("timings"));

				promise.complete(response);
				logger.info("##### " + method + " SLOTS TIMINGS - SAVED RESPONSE -->> " + response);
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		} else {
			JsonObject update = new JsonObject().put("$set",
					new JsonObject().put("timings", request.getJsonArray("timings")).put("updatedDateTime",
							new JsonObject().put("$date", dateTime)));
			logger.info("##### " + method + " (EXISTING TIMINGS) QUERY   -->> " + query);
			logger.info("##### " + method + " (EXISTING TIMINGS) PAYLOAD -->> " + update);
			client.rxUpdateCollection("CUST_DIETPLAN_TIMINGS", query, update).subscribe(res -> {
				response.put("code", "0000").put("message", "success").put("timings", request.getJsonArray("timings"));
				promise.complete(response);
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		}

		return promise.future();
	}

	/**
	 * Save dietplan timings Detox.
	 * 
	 * @param email
	 * @param data
	 * @param request
	 * @param traceId
	 * 
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> saveOrUpdateCustomerDietPlanTimingsDetox(String email, FilterData profile,
			JsonObject request, String traceId) {
		String method = "MongoRepositoryWrapper saveOrUpdateCustomerDietPlanTimingsDetox() " + traceId + "-[" + email
				+ "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject oldTimings = profile.getTimings();
		JsonObject query = new JsonObject();
		Calendar cal1 = Calendar.getInstance(); // creates calendar
		cal1.setTime(new Date()); // sets calendar time/date
		cal1.add(Calendar.HOUR_OF_DAY, 10); // adds ten hour
		cal1.add(Calendar.MINUTE, 30); // add 30 minutes
		String dateTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(cal1.getTime());
		JsonObject response = new JsonObject();
		query.put("_id", email);
		if (null == oldTimings) {
			query.put("timings", request.getJsonArray("timings"));
			query.put("createdDate", ApiUtils.getFilteredDate(0));
			query.put("createdDateTime", new JsonObject().put("$date", dateTime));
			query.put("updatedDateTime", new JsonObject().put("$date", dateTime));
			logger.info("##### " + method + " (NEW TIMINGS) QUERY -->> " + query);
			client.rxSave("CUST_DIETPLAN_TIMINGS_DETOX", query).subscribe(res -> {
				response.put("code", "0000");
				response.put("message", "Success");
				response.put("timings", request.getJsonArray("timings"));

				promise.complete(response);
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		} else {
			JsonObject update = new JsonObject().put("$set",
					new JsonObject().put("timings", request.getJsonArray("timings")).put("updatedDateTime",
							new JsonObject().put("$date", dateTime)));
			logger.info("##### " + method + " (EXISTING TIMINGS) QUERY   -->> " + query);
			logger.info("##### " + method + " (EXISTING TIMINGS) PAYLOAD -->> " + update);
			client.rxUpdateCollection("CUST_DIETPLAN_TIMINGS", query, update).subscribe(res -> {
				response.put("code", "0000").put("message", "success").put("timings", request.getJsonArray("timings"));
				promise.complete(response);
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		}

		return promise.future();
	}

	/**
	 * Get today dietplan list Detox.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> geTodayDietListCacheDetox(String email, String traceId) {
		String method = "MongoRepositoryWrapper geTodayDietListCacheDetox() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("_id.email", email).put("_id.date", ApiUtils.getFilteredDateddMMyyyy(0));
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFindOne("CUST_DAILY_DIET_DETOX", query, null).subscribe(res -> {
			if (res == null || res.isEmpty())
				promise.fail("No Diet saved for today");
			else
				promise.complete(res.getJsonObject("data"));
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get today dietplan list Detox.
	 * 
	 * @param email
	 * @param data
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getTodayDietPlanCacheDetox(String email, JsonObject data, String date, String traceId) {
		Promise<JsonObject> promise = Promise.promise();
		boolean isTodayDietplanAvailable = data.getBoolean("isTodayDetoxTaken");
		boolean isDetoxAllowedToTake = data.getBoolean("isDetoxAllowedToTake");
		if (isTodayDietplanAvailable) {
			JsonObject json = new JsonObject();
			json.put("isDetoxAllowedToTake", isDetoxAllowedToTake);
			json.put("detoxTakenOn", data.getJsonArray("detoxTakenOn"));
			json.put("data", data.getJsonObject("data"));
			promise.complete(data.getJsonObject("data"));
		} else if (!isDetoxAllowedToTake) {
			promise.fail("Detox is not allowed to take.");
		} else {
			promise.fail("No Detox Diet saved for today.");
		}

		return promise.future();
	}

	/**
	 * Get today dietplan list Detox.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getYesterdayDietPlanListDetox(String email, String traceId) {
		String method = "MongoRepositoryWrapper getYesterdayDietPlanListDetox() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("_id", new JsonObject().put("email", email).put("date", ApiUtils.getFilteredDateddMMyyyy(1)));
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFind("CUST_DAILY_DIET_DETOX", query).subscribe(res -> {
			logger.debug(
					"##### " + method + " CUSTOMER DAILY DIET DETOX FETCHEDTODAY DIET LIST FROM CACHE -->> " + res);
			if (res != null && !res.isEmpty()) {
				// promise.complete(res.getJsonObject("data"));
			} else {
				logger.debug("##### " + method + " FAILED TO FETCH CACHE DIET");
				promise.fail("No Diet saved for today");
			}
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Get today dietplan list Detox.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getDietPlanCacheForProvidedDateDetox(String email, String date, String traceId) {
		String method = "MongoRepositoryWrapper getDietPlanCacheForProvidedDateDetox() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("_id.email", email).put("_id.date", date);
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFindOne("CUST_DAILY_DIET_DETOX", query, null).subscribe(res -> {
			if (res == null || res.isEmpty())
				promise.fail("No Diet saved for today");
			else
				promise.complete(res.getJsonObject("data"));
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get last week dietplan list Detox.
	 * 
	 * @param email
	 * @param jsonObj
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getLastAndFutureWeekDietPlanListDetox(String email, JsonObject jsonObj, String traceId) {
		String method = "MongoRepositoryWrapper getLastAndFutureWeekDietPlanListDetox() " + traceId + "-[" + email
				+ "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("_id.email", email).put("$or",
				new JsonArray().add(new JsonObject().put("_id.date", ApiUtils.getFilteredFutureDateddMMyyyy(0)))
						.add(new JsonObject().put("_id.date", ApiUtils.getFilteredFutureDateddMMyyyy(1)))
						.add(new JsonObject().put("_id.date", ApiUtils.getFilteredFutureDateddMMyyyy(2)))
						.add(new JsonObject().put("_id.date", ApiUtils.getFilteredFutureDateddMMyyyy(3)))
						.add(new JsonObject().put("_id.date", ApiUtils.getFilteredFutureDateddMMyyyy(4)))
						.add(new JsonObject().put("_id.date", ApiUtils.getFilteredFutureDateddMMyyyy(5)))
						.add(new JsonObject().put("_id.date", ApiUtils.getFilteredFutureDateddMMyyyy(6)))
						.add(new JsonObject().put("_id.date", ApiUtils.getFilteredFutureDateddMMyyyy(7)))
						.add(new JsonObject().put("_id.date", ApiUtils.getFilteredDateddMMyyyy(1)))
						.add(new JsonObject().put("_id.date", ApiUtils.getFilteredDateddMMyyyy(2)))
						.add(new JsonObject().put("_id.date", ApiUtils.getFilteredDateddMMyyyy(3)))
						.add(new JsonObject().put("_id.date", ApiUtils.getFilteredDateddMMyyyy(4)))
						.add(new JsonObject().put("_id.date", ApiUtils.getFilteredDateddMMyyyy(5)))
						.add(new JsonObject().put("_id.date", ApiUtils.getFilteredDateddMMyyyy(6)))
						.add(new JsonObject().put("_id.date", ApiUtils.getFilteredDateddMMyyyy(7))));
//		query.put("_id.email", email).put("$or",
//				new JsonArray().add(new JsonObject().put("_id.date", ApiUtils.getFilteredDateddMMyyyy(0)))
//						.add(new JsonObject().put("_id.date", ApiUtils.getFilteredDateddMMyyyy(1)))
//						.add(new JsonObject().put("_id.date", ApiUtils.getFilteredDateddMMyyyy(2)))
//						.add(new JsonObject().put("_id.date", ApiUtils.getFilteredDateddMMyyyy(3)))
//						.add(new JsonObject().put("_id.date", ApiUtils.getFilteredDateddMMyyyy(4)))
//						.add(new JsonObject().put("_id.date", ApiUtils.getFilteredDateddMMyyyy(5)))
//						.add(new JsonObject().put("_id.date", ApiUtils.getFilteredDateddMMyyyy(6)))
//						.add(new JsonObject().put("_id.date", ApiUtils.getFilteredDateddMMyyyy(7))));

		logger.info("##### " + method + " QUERY -->> " + query);
		JsonArray dietList = new JsonArray();
		client.rxFind("CUST_DAILY_DIET_DETOX", query).map(map -> {
			return map;
		}).subscribe(res -> {
			JsonObject response = new JsonObject();
			JsonObject obj = new JsonObject();
			if (null != res && !res.isEmpty()) {
				obj.put("isYesterdayDetoxTaken", false);
				List<Boolean> detoxCounts = new ArrayList<>();
				obj.put("canDetoxAllowedToTake", true);
				JsonArray jsonArr = new JsonArray();
				res.forEach(action -> {
					String date = action.getJsonObject("_id").getString("date");
					jsonArr.add(date);
					obj.put("detoxTakenOn", date);
					detoxCounts.add(true);
					if (date.equalsIgnoreCase(ApiUtils.getFilteredDateddMMyyyy(0))) {
						obj.put("isTodayDetoxTaken", true);
						obj.put("todayDietplanDetox", action.getJsonObject("data"));
					} else if (date.equalsIgnoreCase(ApiUtils.getFilteredDateddMMyyyy(1))) {
						obj.put("isYesterdayDetoxTaken", true);
					} else {
						obj.put("isDetoxTakenInLastWeek", true);
					}

					JsonObject json = new JsonObject();
					json.put("date", date);
					json.put("data", action.getJsonObject("data"));
					dietList.add(json);
				});

				boolean isDetoxAllowedToTake = true;
				if (obj.getBoolean("isYesterdayDetoxTaken") || detoxCounts.size() >= 2)
					isDetoxAllowedToTake = false;

				response.put("isDetoxAllowedToTake", isDetoxAllowedToTake);
				response.put("detoxTakenCounts", detoxCounts.size());
				response.put("isTodayDetoxTaken", obj.getBoolean("isTodayDetoxTaken"));
				response.put("isYesterdayDetoxTaken", obj.getBoolean("isYesterdayDetoxTaken"));
				response.put("isDetoxTakenInLastWeek", obj.getBoolean("isDetoxTakenInLastWeek"));
				response.put("detoxTakenOn", jsonArr);
				response.put("data", obj.getJsonObject("todayDietplanDetox"));
				response.put("detoxTakenOn", jsonArr);
				response.put("diets", dietList);
			}

			promise.complete(response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Create customer diet plan Detox.
	 * 
	 * @param dbResult
	 * @param prefList
	 * @param date
	 * @param traceId
	 * 
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> createDietplanDetox(DBResult dbResult, List<JsonObject> prefList, String date,
			String traceId) {
		String method = "MongoRepositoryWrapper createDietplanDetox() " + traceId + "-["
				+ dbResult.getFilterData().getEmail() + "]";
		JsonObject response = new JsonObject();
		Promise<JsonObject> promise = Promise.promise();
		try {
			Set<String> allFoods = dbResult.getFilterData().getAllPrefood();
			if (prefList != null && prefList.size() > 0) {
				prefList.forEach(pref -> {
					allFoods.add(pref.getString("code"));
				});
			}

			List<JsonObject> finalPrefList = allFoods.stream().map(code -> {
				final JsonObject obj = getMealByCode(code, dbResult.getData());
				if (obj != null) {
					if (null != prefList && prefList.size() > 0) {
						JsonObject prefObj = checkPrefFood(code, prefList);
						if (prefObj != null) {
							obj.put("portion", prefObj.getDouble("portion"));
							obj.put("originalPortion", obj.getDouble("portion"));
						}
					}
				}
				return obj;
			}).collect(Collectors.toList());

			List<JsonObject> allPlanList = dbResult.getData().stream().map(mapper -> {
				if (mapper != null) {
					if (null != prefList && prefList.size() > 0) {
						JsonObject prefObj = checkPrefFood(mapper.getString("code"), prefList);
						if (prefObj != null) {
							// mapper.put("portion", prefObj.getInteger("portion"));
							mapper.put("originalPortion", mapper.getDouble("portion"));
						}
					}
				}
				return mapper;
			}).collect(Collectors.toList());

			finalPrefList = finalPrefList.stream().filter(x -> x != null)
					.filter(x -> filterAvoidIn(x, dbResult.getFilterData().getDisease()))
					.filter(x -> filterByDietSeason(x)).filter(x -> filterByDietSeason(x))
					.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
					.collect(Collectors.toList());

			List<JsonObject> allPlan = new ArrayList<JsonObject>();

			List<JsonObject> slots0 = FoodFilterUtils.getSlot0Detox(allPlanList, dbResult,
					dbResult.getFilterData().getDisease(), dbResult.getFilterData().getCommunity(), 0, traceId);
			allPlan.addAll(slots0);

			List<JsonObject> slots1 = FoodFilterUtils.getSlotDetox(allPlanList, dbResult, finalPrefList, 1, traceId);
			allPlan.addAll(slots1);

			List<JsonObject> slots2 = FoodFilterUtils.getSlotDetox(allPlanList, dbResult, finalPrefList, 2, traceId);
			allPlan.addAll(slots2);

			List<JsonObject> slots3 = FoodFilterUtils.getSlotDetox(allPlanList, dbResult, finalPrefList, 3, traceId);
			allPlan.addAll(slots3);

			List<JsonObject> slots4 = FoodFilterUtils.getSlotDetox(allPlanList, dbResult, finalPrefList, 4, traceId);
			allPlan.addAll(slots4);

			List<JsonObject> slots5 = FoodFilterUtils.getSlotDetox(allPlanList, dbResult, finalPrefList, 5, traceId);
			allPlan.addAll(slots5);

			List<JsonObject> slots6 = FoodFilterUtils.getSlotDetox(allPlanList, dbResult, finalPrefList, 6, traceId);
			allPlan.addAll(slots6);

			List<JsonObject> slots7 = FoodFilterUtils.getSlotDetox(allPlanList, dbResult, finalPrefList, 7, traceId);
			allPlan.addAll(slots7);

			List<JsonObject> slots8 = FoodFilterUtils.getSlotDetox(allPlanList, dbResult, finalPrefList, 8, traceId);

			// IF CUSTOMER IS NV
//			if ("NV".equalsIgnoreCase(dbResult.getFilterData().getFoodType())) {
//				boolean isItemNV = false;
//				for (JsonObject json : slots7)
//					// IF NV ITEM SERVED IN DINNER (SLOT 7)
//					if ("NV".equalsIgnoreCase(json.getString("foodType")))
//						isItemNV = true;
//
//				if (isItemNV) {
//					List<JsonObject> drinks = allPlanList.stream().filter(x -> getDietBySlot(x, 8))
//							.filter(x -> filterAvoidIn(x, dbResult.getFilterData().getDisease()))
//							.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
//							.filter(x -> filterByDietSeason(x)).filter(x -> x.getString("Type").equalsIgnoreCase("D"))
//							.collect(Collectors.toList());
//
//					Collections.shuffle(drinks);
//					for (JsonObject jsonObj : slots8)
//						if ("DM".equalsIgnoreCase(jsonObj.getString("Type"))) {
//							slots8.remove(jsonObj); // REMOVE DM ie. 173
//							ApiUtils.addFoodItem(drinks.get(0), slots8);
//							break;
//						}
//				}
//			}

			allPlan.addAll(slots8);
			logger.info("##### " + method + " TOTAL FOOD ITEMS [DETOX] -->> " + allPlan.size());
			Double tolalCalories = ApiUtils.getTotalCalories(allPlan);
			List<JsonObject> result = new ArrayList<JsonObject>();
			Double slotWiseTotalCalories = 0d;

			// SLOT 0
			JsonObject slotObject0 = new JsonObject();
			slotObject0.put("time", dbResult.getFilterData().getSlot0().replace(" AM", ""));
			slotObject0.put("message", dbResult.getFilterData().getSlot0Message());
			slotObject0.put("habitCode", "H018");
			slotObject0.put("slot", 0);
			slotObject0.put("Locked", false);
			slotObject0.put("isLocked", false);
			slotObject0.put("Remark", "Start your day with atleast 2 glasses of warm water.");

			slotObject0.put("data", slots0);
			Double totalCalories = slots0.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Calories"))
					.sum();
			Double totalCarbs = slots0.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Carbs")).sum();
			Double totalFat = slots0.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fat")).sum();
			Double totalProtien = slots0.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Protien")).sum();
			Double totalFiber = slots0.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fiber")).sum();
			slotObject0.put("totalCalories", ApiUtils.getDecimal(totalCalories));
			slotObject0.put("totalCarbs", ApiUtils.getDecimal(totalCarbs));
			slotObject0.put("totalFat", ApiUtils.getDecimal(totalFat));
			slotObject0.put("totalProtien", ApiUtils.getDecimal(totalProtien));
			slotObject0.put("totalFiber", ApiUtils.getDecimal(totalFiber));
			result.add(slotObject0);
			slotWiseTotalCalories += totalCalories;
			logger.info("##### " + method + " [SLOT 0-DETOX] TOTAL CALORIES -->> [" + totalCalories + "]");

			// SLOT 1
			JsonObject slotObject1 = new JsonObject();
			slotObject1.put("time", dbResult.getFilterData().getSlot1().replace(" AM", ""));
			slotObject1.put("message", dbResult.getFilterData().getSlot1Message());
			slotObject1.put("habitCode", "H013");
			slotObject1.put("slot", 1);
			slotObject1.put("Locked", false);
			slotObject1.put("isLocked", false);
			slotObject1.put("data", slots1);
			slotObject1.put("Remark", "Break your fasting with fruits and not tea.");

			totalCalories = slots1.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Calories")).sum();
			totalCarbs = slots1.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Carbs")).sum();
			totalFat = slots1.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fat")).sum();
			totalProtien = slots1.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Protien")).sum();
			totalFiber = slots1.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fiber")).sum();
			slotObject1.put("totalCalories", ApiUtils.getDecimal(totalCalories));
			slotObject1.put("totalCarbs", ApiUtils.getDecimal(totalCarbs));
			slotObject1.put("totalFat", ApiUtils.getDecimal(totalFat));
			slotObject1.put("totalProtien", ApiUtils.getDecimal(totalProtien));
			slotObject1.put("totalFiber", ApiUtils.getDecimal(totalFiber));
			result.add(slotObject1);
			slotWiseTotalCalories += totalCalories;
			logger.info("##### " + method + " [SLOT 1-DETOX] TOTAL CALORIES -->> [" + totalCalories + "]");

			// SLOT 2
			List<String> tips = new ArrayList<String>();
			tips.add("H017");
			tips.add("H020");
			Map<String, String> map = new HashMap<>();
			map.put("H017", "By having heavy breakfast you will be aligning with circadian cycle.");
			map.put("H020", "Limit sugar intake to 2 tsp each day.");
			Collections.shuffle(tips);
			String tip = tips.get(0);
			JsonObject slotObject2 = new JsonObject();
			slotObject2.put("time", dbResult.getFilterData().getSlot2().replace(" AM", ""));
			slotObject2.put("message",
					(null != dbResult.getFilterData().getSlot2Message()
							&& !"".equalsIgnoreCase(dbResult.getFilterData().getSlot2Message()))
									? dbResult.getFilterData().getSlot2Message()
									: "BreakFast");
			slotObject2.put("habitCode", tip);
			slotObject2.put("slot", 2);
			slotObject2.put("data", slots2);
			slotObject2.put("Locked", false);
			slotObject2.put("isLocked", false);
			slotObject2.put("Remark", map.get(tip));

			totalCalories = slots2.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Calories")).sum();
			totalCarbs = slots2.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Carbs")).sum();
			totalFat = slots2.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fat")).sum();
			totalProtien = slots2.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Protien")).sum();
			totalFiber = slots2.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fiber")).sum();
			slotObject2.put("totalCalories", ApiUtils.getDecimal(totalCalories));
			slotObject2.put("totalCarbs", ApiUtils.getDecimal(totalCarbs));
			slotObject2.put("totalFat", ApiUtils.getDecimal(totalFat));
			slotObject2.put("totalProtien", ApiUtils.getDecimal(totalProtien));
			slotObject1.put("totalFiber", ApiUtils.getDecimal(totalFiber));
			result.add(slotObject2);
			slotWiseTotalCalories += totalCalories;
			logger.info("##### " + method + " [SLOT 2-DETOX] TOTAL CALORIES -->> [" + totalCalories + "]");

			// SLOT 3
			JsonObject slotObject3 = new JsonObject();
			slotObject3.put("time", dbResult.getFilterData().getSlot3().replace(" AM", ""));
			slotObject3.put("message", dbResult.getFilterData().getSlot3Message());
			slotObject3.put("habitCode", "H003");
			slotObject3.put("slot", 3);
			slotObject3.put("Locked", false);
			slotObject3.put("isLocked", true);
			slotObject3.put("data", slots3);
			slotObject3.put("Remark", "Drink coconut water or butter milk each day.");

			totalCalories = slots3.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Calories")).sum();
			totalCarbs = slots3.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Carbs")).sum();
			totalFat = slots3.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fat")).sum();
			totalProtien = slots3.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Protien")).sum();
			totalFiber = slots3.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fiber")).sum();
			slotObject3.put("totalCalories", ApiUtils.getDecimal(totalCalories));
			slotObject3.put("totalCarbs", ApiUtils.getDecimal(totalCarbs));
			slotObject3.put("totalFat", ApiUtils.getDecimal(totalFat));
			slotObject3.put("totalProtien", ApiUtils.getDecimal(totalProtien));
			slotObject3.put("totalFiber", ApiUtils.getDecimal(totalFiber));
			result.add(slotObject3);
			slotWiseTotalCalories += totalCalories;
			logger.info("##### " + method + " [SLOT 3-DETOX] TOTAL CALORIES -->> [" + totalCalories + "]");

			// SLOT 4
			tips.clear();
			tips.add("H012");
			tips.add("H011");
			tips.add("H025");
			map.clear();
			map.put("H012", "Eat bowl of salad before lunch.");
			map.put("H011", "Use Kachi ghani oil for cooking.");
			map.put("H025", "Eat 1 tsp tadka of Desi ghee for garnishing.");
			Collections.shuffle(tips);
			tip = tips.get(0);
			JsonObject slotObject4 = new JsonObject();
			slotObject4.put("time", dbResult.getFilterData().getSlot4().replace(" AM", "").replace(" PM", ""));
			slotObject4.put("message", dbResult.getFilterData().getSlot4Message());
			slotObject4.put("habitCode", tip);
			slotObject4.put("slot", 4);
			slotObject4.put("Locked", false);
			slotObject4.put("isLocked", true);
			slotObject4.put("data", slots4);
			slotObject4.put("Remark", map.get(tip));

			totalCalories = slots4.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Calories")).sum();
			totalCarbs = slots4.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Carbs")).sum();
			totalFat = slots4.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fat")).sum();
			totalProtien = slots4.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Protien")).sum();
			totalFiber = slots4.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fiber")).sum();
			slotObject4.put("totalCalories", ApiUtils.getDecimal(totalCalories));
			slotObject4.put("totalCarbs", ApiUtils.getDecimal(totalCarbs));
			slotObject4.put("totalFat", ApiUtils.getDecimal(totalFat));
			slotObject4.put("totalProtien", ApiUtils.getDecimal(totalProtien));
			slotObject4.put("totalFiber", ApiUtils.getDecimal(totalFiber));
			result.add(slotObject4);
			slotWiseTotalCalories += totalCalories;
			logger.info("##### " + method + " [SLOT 4-DETOX] TOTAL CALORIES -->> [" + totalCalories + "]");

			// SLOT 5
			JsonObject slotObject5 = new JsonObject();
			slotObject5.put("time", dbResult.getFilterData().getSlot5().replace(" AM", "").replace(" PM", ""));
			slotObject5.put("message", dbResult.getFilterData().getSlot5Message());
			slotObject5.put("habitCode", "H001");
			slotObject5.put("slot", 5);
			slotObject5.put("data", slots5);
			slotObject5.put("Remark", "Green tea is good deoxidation.");
			slotObject5.put("Locked", true);
			slotObject5.put("isLocked", true);

			totalCalories = slots5.stream().filter(x -> x != null).filter(x -> x != null)
					.mapToDouble(m -> m.getDouble("Calories")).sum();
			totalCarbs = slots5.stream().filter(x -> x != null).filter(x -> x != null)
					.mapToDouble(m -> m.getDouble("Carbs")).sum();
			totalFat = slots5.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fat")).sum();
			totalProtien = slots5.stream().filter(x -> x != null).filter(x -> x != null)
					.mapToDouble(m -> m.getDouble("Protien")).sum();
			totalFiber = slots5.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fiber")).sum();
			slotObject5.put("totalCalories", ApiUtils.getDecimal(totalCalories));
			slotObject5.put("totalCarbs", ApiUtils.getDecimal(totalCarbs));
			slotObject5.put("totalFat", ApiUtils.getDecimal(totalFat));
			slotObject5.put("totalProtien", ApiUtils.getDecimal(totalProtien));
			slotObject5.put("totalFiber", ApiUtils.getDecimal(totalFiber));
			result.add(slotObject5);
			slotWiseTotalCalories += totalCalories;
			logger.info("##### " + method + " [SLOT 5-DETOX] TOTAL CALORIES -->> [" + totalCalories + "]");

			// SLOT 6
			tips.clear();
			tips.add("H019");
			tips.add("H004");
			map.clear();
			map.put("H019", "Eat Frequently for mindful eating. So evening day snacks are good");
			map.put("H004", "Soup is a good healthy choice at this time.");
			Collections.shuffle(tips);
			tip = tips.get(0);
			JsonObject slotObject6 = new JsonObject();
			slotObject6.put("time", dbResult.getFilterData().getSlot6().replace(" AM", "").replace(" PM", ""));
			slotObject6.put("message", dbResult.getFilterData().getSlot6Message());
			slotObject6.put("habitCode", tip);
			slotObject6.put("slot", 6);
			slotObject6.put("data", slots6);
			slotObject6.put("Locked", false);
			slotObject6.put("isLocked", true);

			totalCalories = slots6.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Calories")).sum();
			totalCarbs = slots6.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Carbs")).sum();
			totalFat = slots6.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fat")).sum();
			totalProtien = slots6.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Protien")).sum();
			totalFiber = slots6.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fiber")).sum();
			slotObject6.put("totalCalories", ApiUtils.getDecimal(totalCalories));
			slotObject6.put("totalCarbs", ApiUtils.getDecimal(totalCarbs));
			slotObject6.put("totalFat", ApiUtils.getDecimal(totalFat));
			slotObject6.put("totalProtien", ApiUtils.getDecimal(totalProtien));
			slotObject6.put("totalFiber", ApiUtils.getDecimal(totalFiber));
			slotObject6.put("Remark", map.get(tip));
			result.add(slotObject6);
			slotWiseTotalCalories += totalCalories;
			logger.info("##### " + method + " [SLOT 6-DETOX] TOTAL CALORIES -->> [" + totalCalories + "]");

			// SLOT 7
			JsonObject slotObject7 = new JsonObject();
			slotObject7.put("time", dbResult.getFilterData().getSlot7().replace(" AM", "").replace(" PM", ""));
			slotObject7.put("message", dbResult.getFilterData().getSlot7Message());
			slotObject7.put("habitCode", "H016");
			slotObject7.put("slot", 7);
			slotObject7.put("Locked", false);
			slotObject7.put("isLocked", true);
			slotObject7.put("data", slots7);
			slotObject7.put("Remark", "Take light dinner by 7 PM to align with Circadian cycle.");

			totalCalories = slots7.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Calories")).sum();
			totalCarbs = slots7.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Carbs")).sum();
			totalFat = slots7.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fat")).sum();
			totalProtien = slots7.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Protien")).sum();
			totalFiber = slots7.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fiber")).sum();
			slotObject7.put("totalCalories", ApiUtils.getDecimal(totalCalories));
			slotObject7.put("totalCarbs", ApiUtils.getDecimal(totalCarbs));
			slotObject7.put("totalFat", ApiUtils.getDecimal(totalFat));
			slotObject7.put("totalProtien", ApiUtils.getDecimal(totalProtien));
			slotObject7.put("totalFiber", ApiUtils.getDecimal(totalFiber));
			result.add(slotObject7);
			slotWiseTotalCalories += totalCalories;
			logger.info("##### " + method + " [SLOT 7-DETOX] TOTAL CALORIES -->> [" + totalCalories + "]");

			// SLOT 8
			JsonObject slotObject8 = new JsonObject();
//			List<String> list = new ArrayList<>();
//			if (null != dbResult.getFilterData().getTimings())
//				dbResult.getFilterData().getTimings().getJsonArray("timings").forEach(action -> {
//					JsonObject json = (JsonObject) action;
//					if (json.getInteger("slot") == 8 && null == json.getValue("time"))
//						list.add(null);
//				});
//
//			if (null != list && list.size() > 0) {
//				slots8 = slots8.stream().filter(x -> !"173".equalsIgnoreCase(x.getString("_id")))
//						.filter(x -> ("D".equalsIgnoreCase(x.getString("Type")))).collect(Collectors.toList());
//				logger.debug("##### " + method + " SLOTS 8 IS EMPTY -->> " + slots8.isEmpty());
//				if (null == slots8 || (null != slots8 && slots8.size() <= 0) || slots8.isEmpty())
//					slots8 = FoodFilterUtils.getSlot8ForTimingsNull(allPlanList, dbResult,
//							dbResult.getFilterData().getDisease(), finalPrefList, traceId).getDataList();
//
//				slotObject8.put("time", JsonObject.mapFrom(null));
//			} else {
//				slotObject8.put("time",
//						dbResult.getFilterData().getSlot8().replace(" AM", "").replace(" PM", "").replace(" ", ""));
//			}

			logger.info("##### " + method + " [SLOT 7-DETOX] GET SLOT8S -->> [" + dbResult.getFilterData() + "]");
			if (null != dbResult.getFilterData().getSlot8())
				slotObject8.put("time",
						dbResult.getFilterData().getSlot8().replace(" AM", "").replace(" PM", "").replace(" ", ""));
			else
				slotObject8.put("time", JsonObject.mapFrom(null));

			slotObject8.put("message", dbResult.getFilterData().getSlot8Message());
			slotObject8.put("habitCode", "H016");
			slotObject8.put("slot", 8);
			slotObject8.put("Locked", true);
			slotObject8.put("isLocked", true);
			slotObject8.put("data", slots8);
			slotObject8.put("Remark", "Haldi milk is good for sound sleep n immunity.");

			totalCalories = slots8.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Calories")).sum();
			totalCarbs = slots8.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Carbs")).sum();
			totalFat = slots8.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fat")).sum();
			totalProtien = slots8.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Protien")).sum();
			totalFiber = slots8.stream().filter(x -> x != null).filter(x -> x != null)
					.mapToDouble(m -> m.getDouble("Fiber")).sum();
			slotObject8.put("totalCalories", ApiUtils.getDecimal(totalCalories));
			slotObject8.put("totalCarbs", ApiUtils.getDecimal(totalCarbs));
			slotObject8.put("totalFat", ApiUtils.getDecimal(totalFat));
			slotObject8.put("totalProtien", ApiUtils.getDecimal(totalProtien));
			slotObject8.put("totalFiber", ApiUtils.getDecimal(totalFiber));
			result.add(slotObject8);
			slotWiseTotalCalories += totalCalories;
			slotWiseTotalCalories = Double.parseDouble(ApiUtils.getDecimal(slotWiseTotalCalories));
			logger.info("##### " + method + " [SLOT 8-DETOX] TOTAL CALORIES -->> [" + totalCalories + "]");

			allPlan = allPlan.stream().filter(x -> x != null).collect(Collectors.toList());
			totalCalories = allPlan.stream().mapToDouble(x -> {
				return Double.valueOf(x.getDouble("Calories"));
			}).sum();

			logger.info("##### " + method + " TOTAL SLOTS CALORIES [DETOX]    -->> [" + slotWiseTotalCalories + "]");

			response.put("isDetox", true);
			response.put("email", dbResult.getFilterData().getEmail());
			response.put("totalCalories", slotWiseTotalCalories);
			response.put("recomended", dbResult.getFilterData().getCalories());
			Double totalCaloriesPer = ((slotWiseTotalCalories * 100) / dbResult.getFilterData().getCalories()) > 100.0
					? 100
					: ((slotWiseTotalCalories * 100) / dbResult.getFilterData().getCalories());
			response.put("totalCaloriesPer", totalCaloriesPer.intValue());

			totalCarbs = allPlan.stream().mapToDouble(x -> {
				return Double.valueOf(x.getDouble("Carbs"));
			}).sum();
			response.put("totalCarbs", totalCarbs.intValue());
			Double totalCarbsPer = ((totalCarbs * 4) * 100) / totalCalories;
			response.put("totalCarbsPer", totalCarbsPer.intValue());

			totalFat = allPlan.stream().mapToDouble(x -> {
				return Double.valueOf(x.getDouble("Fat"));
			}).sum();

			Double totalFatPer = ((totalFat * 9) * 100) / totalCalories;
			response.put("totalFat", totalFat.intValue());
			response.put("totalFatPer", totalFatPer.intValue());

			totalProtien = allPlan.stream().mapToDouble(x -> {
				return Double.valueOf(x.getDouble("Protien"));
			}).sum();

			Double totalProtienPer = ((totalProtien * 4) * 100) / totalCalories;
			response.put("totalProtien", totalProtien.intValue());
			response.put("totalProtienPer", totalProtienPer.intValue());

			totalFiber = allPlan.stream().mapToDouble(x -> {
				return Double.valueOf(x.getDouble("Fiber"));
			}).sum();
			Double totalFiberPer = totalCalories / totalFiber;

			response.put("totalFiber", totalFiber.intValue());
			response.put("totalFiberPer", totalFiberPer.intValue());

			Double calDistribution = (slots7.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Calories"))
					.sum()) + (slots8.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Calories")).sum());
			Double calDistributionPer = (100 / tolalCalories) * calDistribution;
			response.put("calDistribution", calDistribution.intValue());
			response.put("calDistributionPer", calDistributionPer.intValue());

			response.put("diets", result);
			logger.info("##### " + method + " RECOMMENDED CALORIES -->> " + dbResult.getFilterData().getCalories());
			// WRITE TO CACHE - DETOX
			if (slotWiseTotalCalories > ApiUtils.getMinus7point5Pper(dbResult.getFilterData().getCalories())
					|| slotWiseTotalCalories < ApiUtils.getPlus5Pper(dbResult.getFilterData().getCalories()))
				// UPDATE DIET PLAN - DETOX IN CACHE
				saveDietPlanDetox(response, dbResult.getFilterData().getEmail(), date, traceId);

			logger.info("##### " + method + " TOTAL SLOTS CALORIES [DETOX] -->> [" + slotWiseTotalCalories + "]");
			////////////////////////////////////////////////////////////
			JsonArray dietListArr = response.getJsonArray("diets");
			dietListArr.forEach(action -> {
				if (null != action) {
					JsonObject obj = (JsonObject) action;
					JsonArray dataArr = obj.getJsonArray("data");
					dataArr.forEach(mapper -> {
						if (null != mapper) {
							JsonObject food = (JsonObject) mapper;
							food.remove("Slots");
							food.remove("Season");
							food.remove("AvoidIn");
							food.remove("Remark");
							food.remove("Detox");
							food.remove("Special_diet");
							food.remove("courtesy");
							food.remove("recipe");
							food.remove("steps");
							food.remove("updated_by");
							food.remove("video");
							food.remove("Special_slot");
							food.remove("Ultra_special");
						}
					});
				}
			});
			////////////////////////////////////////////////////////////

			JsonObject json = new JsonObject();
			json.put("isDetoxAllowedToTake", true);
			json.put("detoxTakenOn", new JsonArray().add(ApiUtils.getFilteredDateddMMyyyy(0)));
			json.put("data", response);

			promise.complete(response);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return promise.future();
	}

	/**
	 * Move RecommendedIn Items to top
	 * 
	 * @param items
	 * @param dbResult
	 * @param dietList
	 * @param item
	 * @param slot
	 * @param traceId
	 * 
	 * @return finalList
	 */
	private List<JsonObject> getSlotOptionsDetox(List<JsonObject> items, int slot, String traceId) {
		items.forEach(action -> {
			action.remove("Slots");
			action.remove("Season");
			action.remove("AvoidIn");
			action.remove("Remark");
			action.remove("Detox");
			action.remove("Special_diet");
			action.remove("courtesy");
			action.remove("recipe");
			action.remove("steps");
			action.remove("updated_by");
			action.remove("video");
			action.remove("Special_slot");
			action.remove("Ultra_special");
		});

		return items;
	}

	/**
	 * Update daily diet list for Detox.
	 * 
	 * @param data
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> saveDietPlanDetox(JsonObject data, String email, String date, String traceId) {
		String method = "MongoRepositoryWrapper saveDietPlanDetox() " + traceId + "-[" + email + "]";
		Calendar cal1 = Calendar.getInstance(); // creates calendar
		cal1.setTime(new Date()); // sets calendar time/date
		cal1.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		cal1.add(Calendar.MINUTE, 30); // add 30 minutes
		String currentDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(cal1.getTime());
		Promise<JsonObject> promise = Promise.promise();
		try {
			JsonObject query = new JsonObject();
			if (null != date && !"".equals(date))
				query.put("_id", new JsonObject().put("email", email).put("date", date));
			else
				query.put("_id",
						new JsonObject().put("email", email).put("date", ApiUtils.getCurrentDate(email, traceId)));
			query.put("isDetox", true);
			query.put("data", data);
			query.put("updatedTime", new JsonObject().put("$date", currentDate));
			logger.info("##### " + method + " QUERY -->> " + query);
			client.rxSave("CUST_DAILY_DIET_DETOX", query).subscribe(res -> {
				JsonObject response = new JsonObject();
				response.put("code", "0000");
				response.put("message", "success");
				promise.complete(response);
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		} catch (Exception e) {
			logger.error("##### " + method + " EXCEPTION -->> " + e.getMessage());
			e.printStackTrace();
		}

		return promise.future();
	}

	/**
	 * Save dietplan if iteration reaches to 10 or more - Detox.
	 * 
	 * @param data
	 * @param email
	 * @param traceId
	 * 
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> persistDietPlanForBacktraceDetox(JsonObject data, String email, String traceId) {
		String method = "MongoRepositoryWrapper persistDietPlanForBacktraceDetox() " + traceId + "-[" + email + "]";
		Calendar cal1 = Calendar.getInstance(); // creates calendar
		cal1.setTime(new Date()); // sets calendar time/date
		cal1.add(Calendar.HOUR_OF_DAY, 10); // adds ten hour
		cal1.add(Calendar.MINUTE, 30); // add 30 minutes
		String currentDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(cal1.getTime());
		Promise<JsonObject> promise = Promise.promise();
		try {
			JsonObject query = new JsonObject();
			query.put("_id", new JsonObject().put("email", email).put("date", ApiUtils.getCurrentDate(email, traceId)));
			query.put("data", data);
			query.put("updatedTime", new JsonObject().put("$date", currentDate));
			logger.info("##### " + method + " QUERY -->> " + query);
			client.rxSave("CUST_DAILY_DIET_DETOX_LOG", query).subscribe(res -> {
				JsonObject response = new JsonObject();
				response.put("code", "0000");
				response.put("message", "success");
				promise.complete(response);
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		} catch (Exception e) {
			logger.error("##### " + method + " EXCEPTION -->> " + e.getMessage());
			e.printStackTrace();
		}

		return promise.future();
	}

	/**
	 * Get diets for option - Detox.
	 * 
	 * @param dbResult
	 * @param slot
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getDietsForOptionDetox(DBResult dbResult, Integer slot, String traceId) {
		String method = "MongoRepositoryWrapper getDietsForOptionDetox() " + traceId + "-["
				+ dbResult.getFilterData().getEmail() + "]";
		logger.info("##### " + method + " [SLOT " + slot + "-DETOX] OPTIONS");
		JsonObject suggestedPlan = new JsonObject();
		Promise<JsonObject> promise = Promise.promise();
		if (slot < 0 || slot > 8) {
			promise.fail("##### " + method + " SLOT [" + slot + "] IS INVALID.");
		} else {
			List<JsonObject> categories = new ArrayList<JsonObject>();
			List<JsonObject> mealOptions = new ArrayList<JsonObject>();

			// filter data having not related diseases, respective, diet,food type and
			// community
			List<JsonObject> data = dbResult.getData().stream()
					.filter(x -> FoodFilterUtils.getFoodItemByIfDetoxIsYes(x)).filter(x -> getDietBySlot(x, slot))
					.filter(x -> filterAvoidIn(x, dbResult.getFilterData().getDisease()))
					.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
					.filter(x -> filterByDietSeason(x)).collect(Collectors.toList());
			data = FoodFilterUtils.filterByCustCommunityDetox(data, dbResult.getFilterData().getCommunity(), slot,
					" DETOX ");
			List<JsonObject> drinks = new ArrayList<>();
			List<JsonObject> fruits = new ArrayList<>();
			List<JsonObject> meals = new ArrayList<>();
			List<JsonObject> salads = new ArrayList<>();
			Map<String, List<JsonObject>> itemObjMap = new LinkedHashMap<String, List<JsonObject>>();

			// DRINKS/FRUITS
			if (slot == 0 || slot == 2 || slot == 3 || slot == 6 || slot == 8) {
				drinks = data.stream().filter(
						x -> ("D".equalsIgnoreCase(x.getString("Type")) || "DM".equalsIgnoreCase(x.getString("Type"))))
						.collect(Collectors.toList());
				drinks = getSlotOptionsDetox(drinks, slot, traceId); // SUPPRESS FEW VALUES
				// DRINKS - SORT BY CALORIES
				drinks = FoodFilterUtils.sortByCalories(drinks);
				itemObjMap.put("Drinks", drinks);
			} else if (slot == 5) {
				drinks = data.stream().filter(x -> ("D".equalsIgnoreCase(x.getString("Type"))))
						.collect(Collectors.toList());
				drinks = getSlotOptionsDetox(drinks, slot, traceId); // SUPPRESS FEW VALUES
				// DRINKS - SORT BY CALORIES
				drinks = FoodFilterUtils.sortByCalories(drinks);
				itemObjMap.put("Drinks", drinks);
			} else if (slot == 1) {
				fruits = data.stream().filter(
						x -> (x.getString("Type").equalsIgnoreCase("F") || x.getString("Type").equalsIgnoreCase("FS")))
						.collect(Collectors.toList());
				fruits = getSlotOptionsDetox(fruits, slot, traceId); // SUPPRESS FEW VALUES
				// FRUITS - SORT BY CALORIES
				fruits = FoodFilterUtils.sortByCalories(fruits);
				itemObjMap.put("Fruits", fruits);
			}

			// MEAL/SALADS
			if (slot == 0) {
				meals = data.stream().filter(x -> (!"D".equalsIgnoreCase(x.getString("Type"))
						&& !"DM".equalsIgnoreCase(x.getString("Type")))).collect(Collectors.toList());
				meals = getSlotOptionsDetox(meals, slot, traceId); // SUPPRESS FEW VALUES
				// MEAL - SORT BY CALORIES
				meals = FoodFilterUtils.sortByCalories(meals);
				itemObjMap.put("Others", meals);
			} else if (slot == 2) {
				meals = data.stream().filter(x -> getSnacks(x)).collect(Collectors.toList());
				meals = getSlotOptionsDetox(meals, slot, traceId); // SUPPRESS FEW VALUES
				// MEAL - SORT BY CALORIES
				meals = FoodFilterUtils.sortByCalories(meals);
				itemObjMap.put("Meal", meals);

				salads = data.stream().filter(
						x -> (x.getString("Type").equalsIgnoreCase("S") || x.getString("Type").equalsIgnoreCase("SM")))
						.collect(Collectors.toList());
				salads = getSlotOptionsDetox(salads, slot, traceId); // SUPPRESS FEW VALUES
				// SALADS - SORT BY CALORIES
				salads = FoodFilterUtils.sortByCalories(salads);
				itemObjMap.put("Salads", salads);
			} else if (slot == 3) {
				meals = data.stream().filter(x -> getSnacks(x)).collect(Collectors.toList());
				meals = getSlotOptionsDetox(meals, slot, traceId); // SUPPRESS FEW VALUES
				// MEAL - SORT BY CALORIES
				meals = FoodFilterUtils.sortByCalories(meals);
				itemObjMap.put("Others", meals);
			} else if (slot == 4) {
				meals = data.stream().filter(
						x -> (x.getString("Type").equalsIgnoreCase("A") || x.getString("Type").equalsIgnoreCase("C")))
						.collect(Collectors.toList());
				meals = getSlotOptionsDetox(meals, slot, traceId); // SUPPRESS FEW VALUES
				// MEAL - SORT BY CALORIES
				meals = FoodFilterUtils.sortByCalories(meals);
				itemObjMap.put("Meal", meals);

				salads = data.stream().filter(
						x -> (x.getString("Type").equalsIgnoreCase("S") || x.getString("Type").equalsIgnoreCase("SM")))
						.collect(Collectors.toList());
				salads = getSlotOptionsDetox(salads, slot, traceId); // SUPPRESS FEW VALUES
				// SALADS - SORT BY CALORIES
				salads = FoodFilterUtils.sortByCalories(salads);
				itemObjMap.put("Salads", salads);
			} else if (slot == 7) {
				fruits = data.stream().filter(
						x -> (x.getString("Type").equalsIgnoreCase("F") || x.getString("Type").equalsIgnoreCase("FS")))
						.collect(Collectors.toList());
				List<JsonObject> curries = data.stream().filter(
						x -> (x.getString("Type").equalsIgnoreCase("A") || x.getString("Type").equalsIgnoreCase("C")))
						.collect(Collectors.toList());
				List<JsonObject> snacks = data.stream().filter(x -> getSnacks(x)).collect(Collectors.toList());
				meals.addAll(fruits);
				meals.addAll(curries);
				meals.addAll(snacks);
				meals = getSlotOptionsDetox(meals, slot, traceId); // SUPPRESS FEW VALUES
				// MEAL - SORT BY CALORIES
				meals = FoodFilterUtils.sortByCalories(meals);
				itemObjMap.put("Meal", meals);
			}

			JsonObject itemObject = new JsonObject();
			itemObject.put("optionId", 1);
			itemObject.put("isMandatory", true);
			itemObject.put("optionName", "Options");
			itemObject.put("isCategory", true);

			for (Map.Entry<String, List<JsonObject>> entry : itemObjMap.entrySet()) {
				JsonObject category = new JsonObject();
				category.put("name", entry.getKey());
				category.put("food", entry.getValue());
				categories.add(category);
			}

			itemObject.put("categories", categories);
			mealOptions.add(itemObject);
			suggestedPlan.put("mealOptions", mealOptions);

			promise.complete(suggestedPlan);
		}

		return promise.future();
	}

	/**
	 * Get diets for option - Detox.
	 * 
	 * @param dbResult
	 * @param slot
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getDietsForOptionDetoxBkp(DBResult dbResult, Integer slot, String traceId) {
		String method = "MongoRepositoryWrapper getDietsForOptionDetox() " + traceId + "-["
				+ dbResult.getFilterData().getEmail() + "]";
		logger.info("##### " + method + " [SLOT " + slot + "-DETOX] OPTIONS");
		JsonObject suggestedPlan = new JsonObject();
		List<JsonObject> categories = new ArrayList<JsonObject>();
		List<JsonObject> mealOptions = new ArrayList<JsonObject>();
		Promise<JsonObject> promise = Promise.promise();

		// filter data having not related diseases, respective, diet,food type and
		// community
		List<JsonObject> data = dbResult.getData().stream().filter(x -> FoodFilterUtils.getFoodItemByIfDetoxIsYes(x))
				.filter(x -> getDietBySlot(x, slot))
				.filter(x -> filterAvoidIn(x, dbResult.getFilterData().getDisease()))
				.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
				.filter(x -> filterByDietSeason(x)).collect(Collectors.toList());
		data = FoodFilterUtils.filterByCustCommunityDetox(data, dbResult.getFilterData().getCommunity(), slot,
				" DETOX ");
		List<JsonObject> drinks = new ArrayList<>();

		// DRINKS
		if (slot == 2 || slot == 6 || slot == 8)
			drinks = data.stream().filter(
					x -> ("D".equalsIgnoreCase(x.getString("Type")) || "DM".equalsIgnoreCase(x.getString("Type"))))
					.collect(Collectors.toList());
		else
			drinks = data.stream().filter(x -> ("D".equalsIgnoreCase(x.getString("Type"))))
					.collect(Collectors.toList());

		drinks = getSlotOptionsDetox(drinks, slot, traceId); // SUPPRESS FEW VALUES
		// DRINKS - SORT BY CALORIES
		drinks = FoodFilterUtils.sortByCalories(drinks);

		// MEALS
		List<JsonObject> meals = data.stream().filter(
				x -> (!"D".equalsIgnoreCase(x.getString("Type")) && !"DM".equalsIgnoreCase(x.getString("Type"))))
				.collect(Collectors.toList());
		meals = getSlotOptionsDetox(meals, slot, traceId); // SUPPRESS FEW VALUES
		// MEALS - SORT BY CALORIES
		meals = FoodFilterUtils.sortByCalories(meals);

		JsonObject itemObject = new JsonObject();
		itemObject.put("optionId", 1);
		itemObject.put("isMandatory", true);
		itemObject.put("optionName", "Options");
		itemObject.put("isCategory", true);
		Map<String, List<JsonObject>> itemObjMap = new LinkedHashMap<String, List<JsonObject>>();
		itemObjMap.put("Drinks", drinks);
		itemObjMap.put("Meal", meals);

		for (Map.Entry<String, List<JsonObject>> entry : itemObjMap.entrySet()) {
			JsonObject category = new JsonObject();
			category.put("name", entry.getKey());
			category.put("food", entry.getValue());
			categories.add(category);
		}

		itemObject.put("categories", categories);
		mealOptions.add(itemObject);
		suggestedPlan.put("mealOptions", mealOptions);

		promise.complete(suggestedPlan);

		return promise.future();
	}

	/**
	 * Get customer diet preference V2 Detox.
	 * 
	 * @param preference
	 * @param email
	 * @param traceId
	 * @return Future<Set<FoodDetail>>
	 */
	public Future<Set<FoodDetail>> getCustDietPrefV2Detox(CustDietPreference preference, String email, String traceId) {
		String method = "MongoRepositoryWrapper getCustDietPrefV2Detox() " + traceId + "-[" + email + "]";
		Promise<Set<FoodDetail>> promise = Promise.promise();
		Set<FoodDetail> foodDetails = new HashSet<FoodDetail>();
		Set<FoodDetail> prefDoodDetails = preference.getFoods();
		List<FoodDetail> prefFoodDetailsList = new ArrayList<FoodDetail>();
		prefFoodDetailsList.addAll(prefDoodDetails);

		Set<FoodDetail> finalFoodDetails = new HashSet<FoodDetail>();
		JsonObject query = new JsonObject().put("_id", email);
		client.rxFindOne("CUST_DIET_PREF_V2_DETOX", query, null).subscribe(res -> {
			List<String> items = new ArrayList<>();
			if (res != null) {
				JsonArray foodList = res.getJsonArray("foodList");
				if (foodList != null && !foodList.isEmpty()) {
					for (int i = 0; i < foodList.size(); i++) {
						JsonObject detail = foodList.getJsonObject(i);
						FoodDetail foodDetail = new FoodDetail();
						foodDetail.setCode(detail.getString("code"));
						foodDetail.setPortion(detail.getDouble("portion"));
						if (null == detail.getInteger("counter"))
							foodDetail.setCounter(1);
						else
							foodDetail.setCounter(detail.getInteger("counter"));
						foodDetails.add(foodDetail);
						items.add(detail.getString("code"));
					}
				}

				List<FoodDetail> foodDetailsList = new ArrayList<FoodDetail>();
				foodDetailsList.addAll(foodDetails);

				List<String> list = new ArrayList<>();
				Map<String, FoodDetail> map = new HashMap<String, FoodDetail>();
				for (FoodDetail fd : foodDetailsList) {
					list.add(fd.getCode());
					map.put(fd.getCode(), fd);
				}

				List<String> list1 = new ArrayList<>();
				Map<String, FoodDetail> map1 = new HashMap<String, FoodDetail>();
				for (FoodDetail fd : prefFoodDetailsList) {
					list1.add(fd.getCode());
					map1.put(fd.getCode(), fd);
				}

				// finalFoodDetails
				Iterator<Map.Entry<String, FoodDetail>> itr = map1.entrySet().iterator();
				while (itr.hasNext()) {
					Map.Entry<String, FoodDetail> entry = itr.next();
					if (list.contains(entry.getKey())) {
						FoodDetail fd = map.get(entry.getKey());
						if (null == fd.getCounter() || fd.getCounter() == 0)
							fd.setCounter(1);
						else
							fd.setCounter(fd.getCounter() + 2);
						map.put(entry.getKey(), fd);
					} else {
						FoodDetail fd = map1.get(entry.getKey());
						if (null == fd.getCounter() || fd.getCounter() == 0)
							fd.setCounter(1);
						map.put(entry.getKey(), fd);
					}
				}

				itr = map.entrySet().iterator();
				while (itr.hasNext()) {
					Map.Entry<String, FoodDetail> entry = itr.next();
					finalFoodDetails.add(entry.getValue());
				}
			}

			promise.complete(finalFoodDetails);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR 1 -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Update customer diet preferences V2 Detox.
	 * 
	 * @param dietCodeList
	 * @param request
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> updateDietPreferrenceV2Detox(Set<FoodDetail> dietCodeList, CustDietPreference request,
			String traceId) {
		String method = "MongoRepositoryWrapper updateDietPreferrenceV2Detox() " + traceId + "-[" + request.getEmail()
				+ "]";
		Calendar calll = Calendar.getInstance(); // creates calendar
		calll.setTime(new Date()); // sets calendar time/date
		calll.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		calll.add(Calendar.MINUTE, 30); // add 30 minutes
		String currentDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(calll.getTime());
		Promise<JsonObject> promise = Promise.promise();
		logger.info("##### " + method + " CURRENT DATE   -->> " + currentDate);
		logger.info("##### " + method + " DIET CODE LIST -->> " + dietCodeList);
		logger.info("##### " + method + " REQUEST FOOD   -->> " + request.getFoods());

		JsonArray jsonArray = new JsonArray();
		List<FoodDetail> foodDetailsList = new ArrayList<>();
		boolean isDietCodeListEmpty = false;
		if (null == dietCodeList || (null != dietCodeList && dietCodeList.size() <= 0)) {
			dietCodeList = request.getFoods();
			isDietCodeListEmpty = true;
		}

		foodDetailsList.addAll(dietCodeList);
		try {
			for (FoodDetail foodDetail : foodDetailsList)
				jsonArray.add(new JsonObject().put("code", foodDetail.getCode()).put("portion", foodDetail.getPortion())
						.put("counter", (null == foodDetail.getCounter() ? 1 : foodDetail.getCounter())));

			logger.info("##### " + method + " UPDATED FOODS -->> " + jsonArray);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (isDietCodeListEmpty) {
			JsonObject query = new JsonObject().put("_id", request.getEmail());
			query.put("createdDate", currentDate);
			query.put("foodList", jsonArray);
			logger.info("##### " + method + "   QUERY -->> " + query);
			client.rxSave("CUST_DIET_PREF_V2_DETOX", query).subscribe(res -> {
				JsonObject response = new JsonObject();
				response.put("code", "0000");
				response.put("message", "success");
				promise.complete(response);
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		} else {
			JsonObject query = new JsonObject().put("_id", request.getEmail());
			JsonObject update = new JsonObject().put("$set",
					new JsonObject().put("createdDate", currentDate).put("foodList", jsonArray));
			logger.info("##### " + method + "   QUERY -->> " + query);
			logger.info("##### " + method + " PAYLOAD -->> " + update);
			client.rxUpdateCollection("CUST_DIET_PREF_V2_DETOX", query, update).subscribe(res -> {
				JsonObject response = new JsonObject();
				response.put("code", "0000");
				response.put("message", "success");
				promise.complete(response);
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		}

		return promise.future();
	}

	/**
	 * Get todays dietplan list from Cache Detox.
	 * 
	 * @param email
	 * @param slotId
	 * @param foodCodeList
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getTodayDietListCacheDetox(String email, Integer slotId, Set<FoodDetail> foodCodeList, String date, 
			String traceId) {
		String method = "MongoRepositoryWrapper getTodayDietListCacheDetox() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + "     SLOTID -->> " + slotId);
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject();
		String filteredDate = ApiUtils.getCurrentDate(email, traceId);
		if (null != date && !"".equalsIgnoreCase(date))
			filteredDate = date;

		query.put("_id.email", email).put("_id.date", filteredDate);
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFindOne("CUST_DAILY_DIET_DETOX", query, null).subscribe(res -> {
			if (res != null && !res.isEmpty())
				promise.complete(res.getJsonObject("data"));
			else
				promise.fail("No Diet saved for today");
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Update today dietplan in Cache Detox.
	 * 
	 * @param email
	 * @param slot
	 * @param data
	 * @param slotObject
	 * @param foodCodeList
	 * @param date
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> updateTodayDietPlanCacheDetox(String email, Integer slot, JsonObject data,
			JsonObject slotObject, Set<FoodDetail> foodCodeList, String date, String traceId) {
		String method = "MongoRepositoryWrapper updateTodayDietPlanCacheDetox() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		List<FoodDetail> foodMainList = new ArrayList<FoodDetail>();
		foodMainList.addAll(foodCodeList);
		logger.info("##### " + method + " DATA         -->> " + data);

		JsonArray dietList = data.getJsonArray("diets");
		logger.info("###### " + method + " SLOT OBJECT -->> " + slotObject.encodePrettily());
		dietList.forEach(action -> {
			JsonObject jsonObj = (JsonObject) action;
			if (slot == jsonObj.getInteger("slot")) {
				JsonArray slotObjArray = slotObject.getJsonArray("data");
				jsonObj.put("data", slotObjArray);
			}
		});

		logger.info("##### " + method + " DATA (AFTER) -->> " + data);

		// CALCULATE CALORIES
		Calendar cal1 = Calendar.getInstance(); // creates calendar
		cal1.setTime(new Date()); // sets calendar time/date
		cal1.add(Calendar.HOUR_OF_DAY, 10); // adds ten hour
		cal1.add(Calendar.MINUTE, 30); // add 30 minutes
		String currentDateTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(cal1.getTime());
		FoodFilterUtils.calculateCalories(data);

		String filteredDate = ApiUtils.getCurrentDate(email, traceId);
		if (null != date && !"".equalsIgnoreCase(date))
			filteredDate = date;

		JsonObject query = new JsonObject().put("_id.email", email).put("_id.date", filteredDate);
		JsonObject update = new JsonObject();
		update.put("$set", new JsonObject().put("data", data).put("updatedTime", currentDateTime));
		logger.info("##### " + method + " QUERY         -->> " + query);
		client.rxUpdateCollection("CUST_DAILY_DIET_DETOX", query, update).subscribe(res -> {
			promise.complete(new JsonObject().put("code", "000").put("message", "Success")
					.put("date", ApiUtils.getSpecificDate(0, date)).put("dietplan", data));
		}, ex -> {
			promise.fail("unable to update");
		});

		return promise.future();
	}

	/**
	 * Update todays dietplan in Cache Detox - refresh module.
	 * 
	 * @param email
	 * @param slot
	 * @param data
	 * @param slotObject
	 * @param foodCodeList
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> updateTodayDietPlanCacheRefreshDetox(String email, Integer slot, JsonObject data,
			JsonObject slotObject, Set<FoodDetail> foodCodeList, String traceId) {
		String method = "MongoRepositoryWrapper updateTodayDietPlanCacheRefreshDetox() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		List<FoodDetail> foodMainList = new ArrayList<FoodDetail>();
		JsonObject json1 = slotObject.getJsonObject("oldFoodItem");
		String oldFoodType = json1.getString("Type");
		logger.info("##### " + method + " OLD FOOD TYPE -->> " + oldFoodType);
		logger.info("##### " + method + " OLD FOOD JSON -->> " + json1);
		foodMainList.addAll(foodCodeList);
		JsonArray dietList = data.getJsonArray("diets");
		logger.info("###### " + method + "      SLOT OBJECT -->> " + slotObject.encodePrettily());
		String code = foodCodeList.stream().findFirst().get().getCode();
		for (int i = 0; i < dietList.size(); i++) {
			JsonObject jsonObj = dietList.getJsonObject(i);
			JsonArray updatedJsonArr = new JsonArray();
			if (slot == jsonObj.getInteger("slot")) {
				JsonArray jsonObjArray = jsonObj.getJsonArray("data");
				for (int j = 0; j < jsonObjArray.size(); j++) {
					JsonObject jsonObject = jsonObjArray.getJsonObject(j);
					if (code.equalsIgnoreCase(jsonObject.getString("itemCode"))) {
						JsonObject json = slotObject.getJsonArray("data").getJsonObject(0);
						logger.info("##### " + method + " AFTER REMOVE OLDFOODITEM OBJECT -->> " + json);
						if (("B".equalsIgnoreCase(oldFoodType) && "B".equalsIgnoreCase(json.getString("Type")))
								|| ("WC".equalsIgnoreCase(oldFoodType) && "WC".equalsIgnoreCase(json.getString("Type")))
								|| ("WP".equalsIgnoreCase(oldFoodType)
										&& "WP".equalsIgnoreCase(json.getString("Type")))) {
							Double oldPortion = json.getDouble("portion");
							Double newPortion = jsonObject.getDouble("portion");

							Double updatedPortionDev = oldPortion / newPortion;
							Double updatedPortionModulo = oldPortion % newPortion;

							Double updatedPortion = updatedPortionDev;
							if (updatedPortionModulo < 1 && updatedPortionModulo >= 0.5)
								updatedPortion = updatedPortionDev + 0.5;

							if (updatedPortion < 1)
								updatedPortion = 1.0;

							Double updatedCalories = slotObject.getJsonObject("oldFoodItem").getDouble("Calories")
									* updatedPortion;

							// IF FOOD IS OF TYPE 'B'
							if ("B".equalsIgnoreCase(oldFoodType) && "B".equalsIgnoreCase(json.getString("Type"))) {
								updatedPortion = jsonObject.getDouble("portion");
								updatedCalories = json.getDouble("Calories") * updatedPortion;
							}

							json.put("portion", updatedPortion);
							json.put("Calories", Double.parseDouble(ApiUtils.getDecimal(updatedCalories)));
						} else {
							json.put("Calories", Double.parseDouble(ApiUtils.getDecimal(json.getDouble("Calories"))));
						}

						logger.info("###### " + method + "         UPDATED FOOD ITEM -->> " + json);
						updatedJsonArr.add(json);
						if (slotObject.containsKey("oldFoodItem"))
							slotObject.remove("oldFoodItem");
					} else {
						updatedJsonArr.add(jsonObject);
					}
				}

				jsonObj.put("data", updatedJsonArr);
			}
		}

		// CALCULATE CALORIES
		FoodFilterUtils.calculateCalories(data);
		JsonObject query = new JsonObject().put("_id",
				new JsonObject().put("email", email).put("date", ApiUtils.getCurrentDate(email, traceId)));
		JsonObject update = new JsonObject();
		update.put("$set", new JsonObject().put("data", data));
		client.rxUpdateCollection("CUST_DAILY_DIET_DETOX", query, update).subscribe(res -> {
			JsonArray dietListArr = data.getJsonArray("diets");
			dietListArr.forEach(action -> {
				if (null != action) {
					JsonObject obj = (JsonObject) action;
					JsonArray dataArr = obj.getJsonArray("data");
					dataArr.forEach(mapper -> {
						if (null != mapper) {
							JsonObject food = (JsonObject) mapper;
							food.remove("Slots");
							food.remove("Season");
							food.remove("AvoidIn");
							food.remove("Remark");
							food.remove("Detox");
							food.remove("Special_diet");
							food.remove("courtesy");
							food.remove("recipe");
							food.remove("steps");
							food.remove("updated_by");
							food.remove("video");
							food.remove("Special_slot");
							food.remove("Ultra_special");
						}
					});
				}
			});

			promise.complete(new JsonObject().put("code", "000").put("message", "Success").put("dietplan", data));
		}, ex -> {
			promise.fail("unable to update");
		});

		return promise.future();
	}

	/**
	 * Get today dietlist.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getTodayDietListTimings(String email, JsonObject data, String traceId) {
		String method = "MongoRepositoryWrapper getTodayDietListTimings() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("_id.email", email).put("_id.date", ApiUtils.getCurrentDate(email, traceId));
		logger.info("##### " + method + " QUERY -->> " + query);
		JsonObject jsonObj = new JsonObject();
		JsonArray timings = data.getJsonArray("timings");
		timings.forEach(action -> {
			JsonObject obj = (JsonObject) action;
			Integer slot = obj.getInteger("slot");
			String time = obj.getString("time");
			jsonObj.put(String.valueOf(slot), time);
		});

		logger.info("##### " + method + " TIMINGS -->> " + jsonObj);
		client.rxFindOne("CUST_DAILY_DIET", query, null).subscribe(res -> {
			if (res != null && !res.isEmpty()) {
				JsonObject json = res.getJsonObject("data");
				JsonArray dietListArr = json.getJsonArray("diets");
				dietListArr.forEach(action -> {
					if (null != action) {
						JsonObject obj = (JsonObject) action;
						obj.put("time", jsonObj.getString(String.valueOf(obj.getInteger("slot"))));
						JsonArray dataArr = obj.getJsonArray("data");
						dataArr.forEach(mapper -> {
							if (null != mapper) {
								JsonObject food = (JsonObject) mapper;
								food.remove("Slots");
								food.remove("Season");
								food.remove("AvoidIn");
								food.remove("Remark");
								food.remove("Detox");
								food.remove("Special_diet");
								food.remove("courtesy");
								food.remove("recipe");
								food.remove("steps");
								food.remove("updated_by");
								food.remove("video");
								food.remove("Special_slot");
								food.remove("Ultra_special");
							}
						});
					}
				});

				promise.complete(json);
			} else {
				logger.info("##### " + method + " FAILED TO FETCH CACHE DIETPLAN.");
				promise.fail("No Diet saved for today");
			}
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get today dietlist Detox.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getTodayDietListTimingsDetox(String email, JsonObject data, String traceId) {
		String method = "MongoRepositoryWrapper getTodayDietListTimingsDetox() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("_id.email", email).put("_id.date", ApiUtils.getCurrentDate(email, traceId));
		logger.info("##### " + method + " QUERY [DETOX] -->> " + query);
		JsonObject jsonObj = new JsonObject();
		JsonArray timings = data.getJsonArray("timings");
		timings.forEach(action -> {
			JsonObject obj = (JsonObject) action;
			Integer slot = obj.getInteger("slot");
			String time = obj.getString("time");
			jsonObj.put(String.valueOf(slot), time);
		});

		logger.info("##### " + method + "TIMINGS [DETOX] -->> " + jsonObj);
		client.rxFindOne("CUST_DAILY_DIET_DETOX", query, null).subscribe(res -> {
			if (res != null && !res.isEmpty()) {
				JsonObject json = res.getJsonObject("data");
				JsonArray dietListArr = json.getJsonArray("diets");
				dietListArr.forEach(action -> {
					if (null != action) {
						JsonObject obj = (JsonObject) action;
						obj.put("time", jsonObj.getString(String.valueOf(obj.getInteger("slot"))));
						JsonArray dataArr = obj.getJsonArray("data");
						dataArr.forEach(mapper -> {
							if (null != mapper) {
								JsonObject food = (JsonObject) mapper;
								food.remove("Slots");
								food.remove("Season");
								food.remove("AvoidIn");
								food.remove("Remark");
								food.remove("Detox");
								food.remove("Special_diet");
								food.remove("courtesy");
								food.remove("recipe");
								food.remove("steps");
								food.remove("updated_by");
								food.remove("video");
								food.remove("Special_slot");
								food.remove("Ultra_special");
							}
						});
					}
				});

				promise.complete(json);
			} else {
				logger.info("##### " + method + " FAILED TO FETCH CACHE DIETPLAN [DETOX]");
				promise.fail("No Detox Diet saved for today");
			}
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Update dietplan in Cache.
	 * 
	 * @param email
	 * @param data
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> updateDietPlanTimingsInCache(String email, JsonObject data, String traceId) {
		String method = "MongoRepositoryWrapper updateDietPlanTimingsInCache() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		Calendar cal1 = Calendar.getInstance(); // creates calendar
		cal1.setTime(new Date()); // sets calendar time/date
		cal1.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		cal1.add(Calendar.MINUTE, 30); // add 30 minutes
		String currentDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(cal1.getTime());

		JsonObject query = new JsonObject().put("_id.email", email).put("_id.date",
				ApiUtils.getCurrentDate(email, traceId));
		JsonObject update = new JsonObject();
		update.put("$set",
				new JsonObject().put("data", data).put("updatedTime", new JsonObject().put("$date", currentDate)));
		logger.info("##### " + method + " QUERY   -->> " + query);
		client.rxUpdateCollection("CUST_DAILY_DIET", query, update).subscribe(res -> {
			promise.complete(new JsonObject().put("code", "000").put("message", "Success"));
		}, ex -> {
			promise.fail("unable to update");
		});
		return promise.future();
	}

	/**
	 * Update dietplan in Cache Detox.
	 * 
	 * @param email
	 * @param data
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> updateDietPlanTimingsInCacheDetox(String email, JsonObject data, String traceId) {
		String method = "MongoRepositoryWrapper updateDietPlanTimingsInCacheDetox() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		Calendar cal1 = Calendar.getInstance(); // creates calendar
		cal1.setTime(new Date()); // sets calendar time/date
		cal1.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		cal1.add(Calendar.MINUTE, 30); // add 30 minutes
		String currentDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(cal1.getTime());

		JsonObject query = new JsonObject().put("_id.email", email).put("_id.date",
				ApiUtils.getCurrentDate(email, traceId));
		JsonObject update = new JsonObject();
		update.put("$set",
				new JsonObject().put("data", data).put("updatedTime", new JsonObject().put("$date", currentDate)));
		logger.info("##### " + method + " QUERY   -->> " + query);
		client.rxUpdateCollection("CUST_DAILY_DIET_DETOX", query, update).subscribe(res -> {
			promise.complete(new JsonObject().put("code", "000").put("message", "Success"));
		}, ex -> {
			promise.fail("unable to update");
		});
		return promise.future();
	}

	/**
	 * Get data for calculation.
	 * 
	 * @param email
	 * @param data
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> fetchCustProfileReg(String email, JsonObject data, String identifier, String traceId) {
		String method = "MongoRepositoryWrapper fetchCustProfileReg() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);
		logger.info("##### " + method + " IDENTIFIER -->> " + identifier);
		logger.info("##### " + method + " QUERY      -->> " + query);
		client.rxFindOne("CUST_PROFILE", query, null).subscribe(res -> {
			JsonObject jObj = res.getJsonObject("profile");
			Integer trialDuaration = 0;
			String planType = "";
			boolean isNewPlanTypeActive = this.config.getBoolean("isNewPlanTypeActive");
			if (!jObj.containsKey("freePlanExpiryDateTime")) {
				if (isNewPlanTypeActive) { // NEW IMPLEMENTATION
					List<JsonObject> list = new ArrayList<>();
					List<String> planTypes = Arrays.asList(this.config.getString("planType").split(","));
					logger.info("##### " + method + " PLAN TYPES -->> " + planTypes);
					planTypes.forEach(action -> {
						String[] type = action.split(":");
						String planTypeValue = type[0];
						Integer duration = Integer.parseInt(type[1]);
						JsonObject json = new JsonObject();
						json.put("type", planTypeValue);
						json.put("duration", duration);

						list.add(json);
					});

					logger.info("##### " + method + " LIST -->> " + list);
					Collections.shuffle(list);
					logger.info("##### " + method + " JSONOBJECT -->> " + list.get(0));
					planType = list.get(0).getString("type");
					trialDuaration = list.get(0).getInteger("duration");
				} else { // OLD IMPLEMENTATION
					String sourceType = (null != jObj.getValue("source", null)
							&& "apple".equalsIgnoreCase(jObj.getString("source", null))) ? "freeTypeApple"
									: "freeTypeAndroid";
					String durationType = (null != jObj.getValue("source", null)
							&& "apple".equalsIgnoreCase(jObj.getString("source", null))) ? "trialDurationApple"
									: "trialDurationAndroid";
					trialDuaration = this.config.getInteger(durationType);
					planType = this.config.getString(sourceType);
				}
			} else {
				planType = jObj.getString("planType");
				trialDuaration = jObj.getInteger("trialDuaration");
			}

			logger.info("##### " + method + " IS NEW PLANTYPE ACTIVE [" + isNewPlanTypeActive + "] - PLAN TYPE -->> "
					+ planType);
			logger.info("##### " + method + " IS NEW PLANTYPE ACTIVE [" + isNewPlanTypeActive + "] - DURATION  -->> "
					+ trialDuaration);

			if ("free".equalsIgnoreCase(identifier)) { // ACCESS FREE PLAN
				String currentdateTime = ApiUtils.getTrialDate(trialDuaration);
				jObj.put("freePlanExpiryDateTime", currentdateTime);
//				jObj.put("trialDuaration", this.config.getInteger(durationType)); // NOT IN USE
				jObj.put("trialDuaration", trialDuaration);
				jObj.put("planType", planType);
				jObj.put("isPaymentCancelOptionAvailable", this.config.getBoolean("isPaymentCancelOptionAvailable"));

				data.put("freePlanExpiryDateTime", currentdateTime);
				data.put("trialDuaration", trialDuaration);
				data.put("planType", planType);

				data.put("profile", jObj);
			} else if ("payment".equalsIgnoreCase(identifier)) { // ACCESS PAID PLAN
				data.put("freePlanExpiryDateTime", jObj.getString("freePlanExpiryDateTime"));
				data.put("trialDuaration", trialDuaration);
				data.put("planType", "Premium");
				try {
					if ((new Date()
							.after(new SimpleDateFormat("dd-MMM-yyyy").parse(data.getString("planExpiryDate"))))) {
						jObj.put("planType", "premiumEnd");
						jObj.put("planType", "premiumEnd");
					}

				} catch (ParseException e) {
					e.printStackTrace();
				}
				data.put("profile", jObj);
			} else { // ACCESS GENERAL
				Integer amount = 0;
				String planExpiryDate = null;
				boolean isPlanActive = false;
				if (null != data && "0000".equalsIgnoreCase(data.getString("code"))) {
					amount = data.getInteger("amount");
					isPlanActive = data.getBoolean("isPlanActive");
					planExpiryDate = data.getString("planExpiryDate") + " 23:59:59";
				}
				Calendar calendar = Calendar.getInstance(); // creates calendar
				calendar.setTime(new Date()); // sets calendar time/date
				calendar.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
				calendar.add(Calendar.MINUTE, 30); // add 30 minutes
				if (jObj.containsKey("freePlanExpiryDateTime") && null != planExpiryDate
						&& !"".equals(planExpiryDate)) {
					data.put("isPlanActive", true);
					data.put("freePlanExpiryDateTime", jObj.getString("freePlanExpiryDateTime"));
					if (isPlanActive && amount > 0) {
						logger.info("##### " + method + " HERE 1");
						data.put("trialDuaration", trialDuaration);
						data.put("planType", "premium");
						jObj.put("planType", "premium");
						try {
							if ((calendar.getTime()
									.after(new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").parse(planExpiryDate)))) {
								data.put("planType", "premiumEnd");
								jObj.put("planType", "premiumEnd");
								data.put("isPlanActive", false);
							}
						} catch (ParseException e) {
							e.printStackTrace();
						}
					} else {
						if (amount > 0) {
							logger.info("##### " + method + " HERE 2");
							data.put("freePlanExpiryDateTime", jObj.getString("freePlanExpiryDateTime"));
							data.put("trialDuaration", trialDuaration);
							jObj.put("trialDuaration", trialDuaration);
							data.put("planType", "premium");
							jObj.put("planType", "premium");
							try {
								data.put("planType", planType);
								if ((calendar.getTime()
										.after(new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").parse(planExpiryDate)))) {
									data.put("planType", "premiumEnd");
									jObj.put("planType", "premiumEnd");
									data.put("isPlanActive", false);
								}
							} catch (ParseException e) {
								e.printStackTrace();
							}
						} else {
							try {
								logger.info(
										"##### " + method + " HERE 3 -->> " + jObj.getString("freePlanExpiryDateTime"));
								data.put("isPlanActive", false);
								isPlanActive = calendar.getTime().before(new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss")
										.parse(jObj.getString("freePlanExpiryDateTime")));
								logger.info("##### " + method + " REACHED HERE 3 -->> " + isPlanActive);
								data.put("freePlanExpiryDateTime", jObj.getString("freePlanExpiryDateTime"));
								data.put("trialDuaration", jObj.getInteger("trialDuaration"));
								data.put("planType", jObj.getString("planType"));
								logger.info("##### " + method + " REACHED HERE 4 -->> " + isPlanActive);
								if (!isPlanActive) {
									logger.info("##### " + method + " REACHED HERE 5 -->> " + isPlanActive);
									data.put("planType", "Freemium");
									jObj.put("planType", "Freemium");
								}
								logger.info("##### " + method + " EXIT -->> " + data);
							} catch (ParseException e1) {
								e1.printStackTrace();
							}
						}
					}
				} else {
					data.put("isPlanActive", false);
					if (null != planExpiryDate && !"".equals(planExpiryDate)) {
						data.put("planType", "Freemium");
						jObj.put("planType", "Freemium");
						if (amount > 0) {
							try {
								data.put("planType", planType);
								if ((calendar.getTime()
										.before(new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").parse(planExpiryDate)))) {
									data.put("planType", "premium");
									jObj.put("planType", "premium");
									data.put("isPlanActive", true);
								}
							} catch (ParseException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}

			data.put("profile", jObj);
			logger.info("##### " + method + " PAYMENT CONFIRM ACCOUNT -->> " + data);
			promise.complete(data);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get data for calculation.
	 * 
	 * @param email
	 * @param data
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> fetchCustProfileRegBkp(String email, JsonObject data, String identifier, String traceId) {
		String method = "MongoRepositoryWrapper fetchCustProfileReg() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);
		logger.info("##### " + method + " IDENTIFIER -->> " + identifier);
		logger.info("##### " + method + " QUERY      -->> " + query);
		client.rxFindOne("CUST_PROFILE", query, null).subscribe(res -> {
			JsonObject jObj = res.getJsonObject("profile");
			String sourceType = (null != jObj.getValue("source", null)
					&& "apple".equalsIgnoreCase(jObj.getString("source", null))) ? "freeTypeApple" : "freeTypeAndroid";
			String durationType = (null != jObj.getValue("source", null)
					&& "apple".equalsIgnoreCase(jObj.getString("source", null))) ? "trialDurationApple"
							: "trialDurationAndroid";
			Integer trialDuaration = this.config.getInteger(durationType);
			String planType = this.config.getString(sourceType);
			if ("free".equalsIgnoreCase(identifier)) { // ACCESS FREE PLAN
				String currentdateTime = ApiUtils.getTrialDate(trialDuaration);
				jObj.put("freePlanExpiryDateTime", currentdateTime);
				jObj.put("trialDuaration", this.config.getInteger(durationType));
				jObj.put("planType", planType);

				data.put("freePlanExpiryDateTime", currentdateTime);
				data.put("trialDuaration", trialDuaration);
				data.put("planType", planType);

				data.put("profile", jObj);
			} else if ("payment".equalsIgnoreCase(identifier)) { // ACCESS PAID PLAN
				data.put("freePlanExpiryDateTime", jObj.getString("freePlanExpiryDateTime"));
				data.put("trialDuaration", trialDuaration);
				data.put("planType", "Premium");
				try {
					if ((new Date()
							.after(new SimpleDateFormat("dd-MMM-yyyy").parse(data.getString("planExpiryDate"))))) {
						jObj.put("planType", "premiumEnd");
						jObj.put("planType", "premiumEnd");
					}

				} catch (ParseException e) {
					e.printStackTrace();
				}
				data.put("profile", jObj);
			} else { // ACCESS GENERAL
				Integer amount = 0;
				String planExpiryDate = null;
				boolean isPlanActive = false;
				if (null != data && "0000".equalsIgnoreCase(data.getString("code"))) {
					amount = data.getInteger("amount");
					isPlanActive = data.getBoolean("isPlanActive");
					planExpiryDate = data.getString("planExpiryDate") + " 23:59:59";
				}
				Calendar calendar = Calendar.getInstance(); // creates calendar
				calendar.setTime(new Date()); // sets calendar time/date
				calendar.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
				calendar.add(Calendar.MINUTE, 30); // add 30 minutes
				if (jObj.containsKey("freePlanExpiryDateTime") && !"Freemium".equalsIgnoreCase(planType)
						&& null != planExpiryDate && !"".equals(planExpiryDate)) {
					data.put("isPlanActive", true);
					data.put("freePlanExpiryDateTime", jObj.getString("freePlanExpiryDateTime"));
					if (isPlanActive && amount > 0) {
						logger.info("##### " + method + " HERE 1");
						data.put("trialDuaration", trialDuaration);
						data.put("planType", "premium");
						jObj.put("planType", "premium");
						try {
							if ((calendar.getTime()
									.after(new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").parse(planExpiryDate)))) {
								data.put("planType", "premiumEnd");
								jObj.put("planType", "premiumEnd");
								data.put("isPlanActive", false);
							}
						} catch (ParseException e) {
							e.printStackTrace();
						}
					} else {
						if (amount > 0) {
							logger.info("##### " + method + " HERE 2");
							data.put("freePlanExpiryDateTime", jObj.getString("freePlanExpiryDateTime"));
							data.put("trialDuaration", trialDuaration);
							jObj.put("trialDuaration", trialDuaration);
							data.put("planType", "premium");
							jObj.put("planType", "premium");
							try {
								data.put("planType", planType);
								if ((calendar.getTime()
										.after(new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").parse(planExpiryDate)))) {
									data.put("planType", "premiumEnd");
									jObj.put("planType", "premiumEnd");
									data.put("isPlanActive", false);
								}
							} catch (ParseException e) {
								e.printStackTrace();
							}
						} else {
							try {
								logger.info(
										"##### " + method + " HERE 3 -->> " + jObj.getString("freePlanExpiryDateTime"));
								data.put("isPlanActive", false);
								isPlanActive = calendar.getTime().before(new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss")
										.parse(jObj.getString("freePlanExpiryDateTime")));
								logger.info("##### " + method + " REACHED HERE 3 -->> " + isPlanActive);
								data.put("freePlanExpiryDateTime", jObj.getString("freePlanExpiryDateTime"));
								data.put("trialDuaration", jObj.getInteger("trialDuaration"));
								data.put("planType", jObj.getString("planType"));
								logger.info("##### " + method + " REACHED HERE 4 -->> " + isPlanActive);
								if (!isPlanActive) {
									logger.info("##### " + method + " REACHED HERE 5 -->> " + isPlanActive);
									data.put("planType", "Freemium");
									jObj.put("planType", "Freemium");
								}
								logger.info("##### " + method + " EXIT -->> " + data);
							} catch (ParseException e1) {
								e1.printStackTrace();
							}
						}
					}
				} else {
					data.put("isPlanActive", false);
					if (null != planExpiryDate && !"".equals(planExpiryDate)) {
						data.put("planType", "Freemium");
						jObj.put("planType", "Freemium");
						if (amount > 0) {
							try {
								data.put("planType", planType);
								if ((calendar.getTime()
										.before(new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").parse(planExpiryDate)))) {
									data.put("planType", "premium");
									jObj.put("planType", "premium");
									data.put("isPlanActive", true);
								}
							} catch (ParseException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}

			data.put("profile", jObj);
			logger.info("##### " + method + " PAYMENT CONFIRM ACCOUNT -->> " + data);
			promise.complete(data);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	public Future<JsonObject> fetchCustProfileRegOld(String email, JsonObject data, String identifier, String traceId) {
		String method = "MongoRepositoryWrapper fetchCustProfileReg() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFindOne("CUST_PROFILE", query, null).subscribe(res -> {
			JsonObject json = res.getJsonObject("profile");
			Integer trialDuaration = res.getInteger("trialDuration");
			String planType = res.getString("freeType");
			if (json.containsKey("freePlanExpiryDateTime")) {
				Integer amount = data.getInteger("amount");
				boolean isPlanActive = data.getBoolean("isPlanActive");
				if (isPlanActive) {
					if (amount > 0) {
						data.put("freePlanExpiryDateTime", "NA");
						data.put("trialDuaration", 0);
						data.put("planType", "premium");
					} else {
						try {
							data.put("planType", planType);
							if ((new Date().after(new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss")
									.parse(res.getString("freePlanExpiryDateTime")))))
								data.put("planType", "Freemium");

							data.put("freePlanExpiryDateTime", res.getString("freePlanExpiryDateTime"));
							data.put("trialDuaration", trialDuaration);
						} catch (ParseException e) {
							e.printStackTrace();
						}
					}
				} else {
					data.put("freePlanExpiryDateTime", "NA");
					data.put("trialDuaration", 0);
					json.put("planType", "premiumEnd");
				}
			} else {
				data.put("planType", "Freemium");
			}

			promise.complete(data);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Create customer profile.
	 * 
	 * @param document
	 * @param request
	 * @param traceId
	 * @return Future<String>
	 */
	public Future<JsonObject> updateCustPofile(String email, JsonObject data, String traceId) {
		String method = "MongoRepositoryWrapper updateCustPofile() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject profile = data.getJsonObject("profile");
		JsonObject query = new JsonObject().put("_id", email);
		JsonObject payload = new JsonObject().put("$set", new JsonObject().put("profile", profile));
		logger.info("##### " + method + " QUERY   -->> " + query);
		logger.info("##### " + method + " PAYLOAD -->> " + payload);
		client.rxUpdateCollection("CUST_PROFILE", query, payload).subscribe(res -> {
			logger.info("##### " + method + " CUSTOMER PROFILE CREATED");
			promise.complete(data);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Fetch payment details for provided date else today.
	 * 
	 * @param email
	 * @param date
	 * @param traceId
	 * @return Future<DiscountedCouponDetails>
	 */
	public Future<JsonObject> fetchPaymentDetails(String email, String date, String traceId) {
		String method = "MongoRepositoryWrapper fetchPaymentDetails() " + traceId;
		Promise<JsonObject> promise = Promise.promise();
		JsonObject response = new JsonObject();
		JsonObject query = new JsonObject();
		if (null == email || "".equalsIgnoreCase(email)
				|| (null != email && (!email.contains("@") || !email.contains("."))))
			logger.info("##### " + method + " EMAIL IS EMPTY");
		else
			query.put("emailId", email);

		if (null == date || "".equalsIgnoreCase(date)
				|| (null != date && (!date.contains("-") || date.length() < 11 || date.length() > 11)))
			date = ApiUtils.getCurrentDateInddMMMyyyyFormat(0);

		try {
			query.put("createdDate", new JsonObject().put("$regex",
					new SimpleDateFormat("yyyy-MM-dd").format(new SimpleDateFormat("dd-MMM-yyyy").parse(date))));
		} catch (ParseException e) {
			e.printStackTrace();
		}

		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFind("PAYMENT_DETAIL", query).map(map -> {
			return map;
		}).subscribe(res -> {
			JsonArray jsonArr = new JsonArray();
			JsonArray paymentsMadeArr = new JsonArray();
			JsonArray zeroPaymentsMadeArr = new JsonArray();
			JsonArray noPaymentIdArr = new JsonArray();
			JsonArray multipleIdsArr = new JsonArray();
			List<Integer> amounts = new ArrayList<>();
			Map<String, JsonObject> map = new HashMap<>();
			Map<String, JsonObject> multipleMap = new HashMap<>();
			List<Integer> counts = new ArrayList<>();
			if (null != res && !res.isEmpty()) {
				res.forEach(action -> {
					if (null == jsonArr || (null != jsonArr && jsonArr.size() <= 0)
							|| !jsonArr.contains(action.getString("emailId"))) {
						if (action.containsKey("paymentId")) {
							jsonArr.add(action.getString("emailId"));
							map.put(action.getString("emailId"), action);
							if (action.getInteger("amount") <= 0) {
								zeroPaymentsMadeArr
										.add(action.getString("emailId") + " :: " + action.getInteger("amount"));
							} else {
								if (null != amounts && amounts.size() <= 0)
									amounts.add(0, action.getInteger("amount"));
								else
									amounts.add(0, amounts.get(0) + action.getInteger("amount"));

								paymentsMadeArr.add(action.getString("emailId") + " :: " + amounts.get(0));
							}
						} else {
							noPaymentIdArr.add(action);
						}
					} else {
						JsonObject jObj = multipleMap.get(action.getString("emailId"));
						JsonArray jArr = new JsonArray();
						if (null != jObj) {
							jObj = multipleMap.get(action.getString("emailId"));
							jArr = jObj.getJsonArray("ids");
						} else {
							jObj = new JsonObject();
							jArr.add(action);
						}

						jObj.put("ids", jArr);
						multipleMap.put(action.getString("emailId"), jObj);
					}
				});

				multipleMap.entrySet().stream().forEach(e -> {
					JsonObject jObj = e.getValue();
					JsonArray jArr = jObj.getJsonArray("ids");
					// jArr.add(map.get(e.getKey()));
					if (null != counts && counts.size() > 0)
						counts.add(0, counts.get(0) + jArr.size());
					else
						counts.add(0, jArr.size());
					multipleIdsArr.add(jObj);
				});
			}

			response.put("profiles", jsonArr);
			response.put("totalAmountPaid", (null != amounts && amounts.size() > 0 ? amounts.get(0) : 0));
			response.put("paymentsMadeArr", paymentsMadeArr);
			response.put("paymentsMadeCount", paymentsMadeArr.size());
			response.put("zeroPaymentsMadeArr", zeroPaymentsMadeArr);
			response.put("zeroPaymentsCount", zeroPaymentsMadeArr.size());
			response.put("duplicateIdsCount", (null != counts && counts.size() > 0 ? counts.get(0) : 0));
			response.put("multipleIdsArr", multipleIdsArr);
			response.put("multipleIdsArrCount", multipleIdsArr.size());
			response.put("noPaymentId", noPaymentIdArr);
			response.put("noPaymentIdCount", noPaymentIdArr.size());
			promise.complete(response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Fetch subscription plans for provided date else today.
	 * 
	 * @param email
	 * @param date
	 * @param data
	 * @param traceId
	 * @return Future<DiscountedCouponDetails>
	 */
	public Future<JsonObject> fetchSubscribedPlans(String email, String date, JsonObject data, String traceId) {
		String method = "MongoRepositoryWrapper fetchSubscribedPlans() " + traceId;
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject();

		// EMAIL
		if (null == email || "".equalsIgnoreCase(email)
				|| (null != email && (!email.contains("@") || !email.contains("."))))
			logger.info("##### " + method + " EMAIL IS EMPTY");
		else
			query.put("emailId", email);

		// DATE
		if (null == date || "".equalsIgnoreCase(date)
				|| (null != date && (!date.contains("-") || date.length() < 11 || date.length() > 11)))
			date = ApiUtils.getCurrentDateInddMMMyyyyFormat(0);

		JsonArray profiles = data.getJsonArray("profiles");
		query.put("upgradedDate", date);
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFind("PLAN_SUBCRIPTION_DETAIL", query).map(map -> {
			return map;
		}).subscribe(res -> {
			JsonArray jsonArr = new JsonArray();
			JsonArray noPaymentIdArr = new JsonArray();
			if (null != res && !res.isEmpty()) {
				res.forEach(action -> {
					if (action.containsKey("paymentId")) {
						if (null != profiles && null != profiles && profiles.size() > 0
								&& !profiles.contains(action.getString("emailId")))
							jsonArr.add(action.getString("emailId"));
					} else {
						noPaymentIdArr.add(action);
					}
				});
			}

			data.put("nonSubscribedCustomers", jsonArr);
			data.put("nonSubscribedCustomersSize", jsonArr.size());
			data.put("nonPaymentIdCustomers", noPaymentIdArr);
			data.put("nonPaymentIdCustomersSize", noPaymentIdArr.size());
			promise.complete(data);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Fetch customers for provided date else today.
	 * 
	 * @param email
	 * @param date
	 * @param data
	 * @param traceId
	 * @return Future<DiscountedCouponDetails>
	 */
	public Future<JsonObject> fetchCustomers(String email, String date, JsonObject data, String traceId) {
		String method = "MongoRepositoryWrapper fetchPendingCust() " + traceId;
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject();

		// EMAIL
		if (null == email || "".equalsIgnoreCase(email)
				|| (null != email && (!email.contains("@") || !email.contains("."))))
			logger.info("##### " + method + " EMAIL IS EMPTY");
		else
			query.put("_id", email);

		// DATE
		if (null == date || "".equalsIgnoreCase(date)
				|| (null != date && (!date.contains("-") || date.length() < 11 || date.length() > 11)))
			date = ApiUtils.getCurrentDateInddMMMyyyyFormat(0);

		JsonArray nonSubscribedCustomers = data.getJsonArray("nonSubscribedCustomers");
		query.put("profile.createdDate", date);
		query.put("diet", new JsonObject().put("$ne", JsonObject.mapFrom(null)));
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFind("CUST_PROFILE", query).map(map -> {
			return map;
		}).subscribe(res -> {
			JsonArray jsonArr = new JsonArray();
			JsonArray remainingArr = new JsonArray();
			if (null != res && !res.isEmpty()) {
				res.forEach(action -> {
					if (action.containsKey("diet") && null != nonSubscribedCustomers
							&& !nonSubscribedCustomers.contains(action.getString("_id")))
						jsonArr.add(action.getString("_id"));
					else
						remainingArr.add(action.getString("_id"));
				});

				data.put("nonSubscribedProfiles", jsonArr);
				data.put("nonSubscribedProfilesSize", jsonArr.size());
				data.put("remainingProfiles", remainingArr);
				data.put("remainingProfilesSize", remainingArr.size());
				logger.info("##### " + method + " PROFILES -->> " + jsonArr.size());
			}

			promise.complete(data);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	public Future<CalculationData> getActivities(CalculationData data, String traceId) {
		String method = "MongoRepositoryWrapper getCustDefaultProfile() " + traceId;
		Promise<CalculationData> promise = Promise.promise();
		// CalculationData data = new CalculationData();
		JsonObject query = new JsonObject();
		query.put("_id", "v1");
		client.rxFindOne("DEFAUL_PROFILE", query, null).subscribe(res -> {
			JsonArray activities = res.getJsonObject("otherMaster").getJsonArray("activities");
			HashMap<Float, Integer> map = new HashMap<>();
			activities.forEach(activity -> {
				JsonObject json = (JsonObject) activity;
				map.put(json.getFloat("data"), json.getInteger("calories"));
			});

			data.setActivities(map);
			promise.complete(data);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Fetch customer profile post demographic update.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> fetchCustomerProfilePostDemographicUpdate(String email, String traceId) {
		String method = "MongoRepositoryWrapper fetchCustomerProfilePostDemographicUpdate() " + traceId + "-[" + email
				+ "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);
		logger.info("##### " + method + " QUERY -->> " + query);
		JsonObject json = new JsonObject();
		client.rxFindOne("CUST_PROFILE", query, null).subscribe(res -> {
			json.put("code", "0000");
			json.put("message", "success");
			if (res != null && !res.isEmpty() && res.containsKey("lifeStyle")) {
				json.put("profile", res.getJsonObject("profile"));
				json.put("demographic", res.getJsonObject("demographic"));
				json.put("lifeStyle", res.getJsonObject("lifeStyle"));
				json.put("diet", res.getJsonObject("diet"));
			}

			promise.complete(json);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Fetch customer profile post demographic update.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> fetchCustomerProfilePostLifeStyleUpdate(String email, String traceId) {
		String method = "MongoRepositoryWrapper fetchCustomerProfilePostLifeStyleUpdate() " + traceId + "-[" + email
				+ "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);
		logger.info("##### " + method + " QUERY -->> " + query);
		JsonObject json = new JsonObject();
		client.rxFindOne("CUST_PROFILE", query, null).subscribe(res -> {
			json.put("code", "0000");
			json.put("message", "success");
			if (res != null && !res.isEmpty() && res.containsKey("diet")) {
				json.put("profile", res.getJsonObject("profile"));
				json.put("demographic", res.getJsonObject("demographic"));
				json.put("lifeStyle", res.getJsonObject("lifeStyle"));
				json.put("diet", res.getJsonObject("diet"));
			}

			promise.complete(json);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Fetch customer profile.
	 * 
	 * @param email
	 * @param request
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> fetchAdminCust(String email, String traceId) {
		String method = "MongoRepositoryWrapper fetchAdminCust() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);
		logger.info("##### " + method + " QUERY -->> " + query);
		JsonObject json = new JsonObject();
		client.rxFindOne("CUST_ADMIN_PROFILE", query, null).subscribe(res -> {
			json.put("code", "0001");
			json.put("message", "Customer admin profile not found.");
			if (res == null || res.isEmpty()) {
				logger.info("##### " + method + " CUSTOMER ADMIN PROFILE IS UNAVAILABLE");
			} else {
				json.put("code", "0000");
				json.put("message", "success");
			}

			promise.complete(json);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Create customer profile.
	 * 
	 * @param email
	 * @param name
	 * @param data
	 * @param traceId
	 * @return Future<String>
	 */
	public Future<JsonObject> fetchnSaveCust(String email, String name, JsonObject data, String traceId) {
		String method = "MongoRepositoryWrapper fetchnSaveCust() " + traceId + "-[" + data.getString("code") + "]";
		Promise<JsonObject> promise = Promise.promise();
		if ("0001".equalsIgnoreCase(data.getString("code"))) {
			Calendar calendar = Calendar.getInstance(); // creates calendar
			calendar.setTime(new Date()); // sets calendar time/date
			calendar.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
			calendar.add(Calendar.MINUTE, 30); // add 30 minutes
			String currentDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(calendar.getTime());
			JsonObject payload = new JsonObject().put("_id", email).put("name", name).put("updatedTime",
					new JsonObject().put("$date", currentDate));
			client.rxSave("CUST_ADMIN_PROFILE", payload).subscribe(res -> {
				logger.info("##### " + method + " CUSTOMER ADMIN PROFILE CREATED");
				promise.complete(
						new JsonObject().put("code", "0000").put("message", "[" + email + "] save successfully."));
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		} else {
			promise.complete(
					new JsonObject().put("code", "0001").put("message", "[" + email + "] already registered."));
		}

		return promise.future();
	}

	/**
	 * Ftech detox today's dietplan.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<Boolean> fetchTodaysDietListCacheDetox(String email, String date, String traceId) {
		String method = "MongoRepositoryWrapper fetchTodaysDietListCacheDetox() " + traceId + "-[" + email + "]";
		Promise<Boolean> promise = Promise.promise();
		JsonObject query = new JsonObject();
		if (null == date || "".equals(date))
			query.put("_id.email", email).put("_id.date", ApiUtils.getFilteredDateddMMyyyy(0));
		else
			try {
				query.put("_id.email", email).put("_id.date",
						new SimpleDateFormat("ddMMyyyy").format(new SimpleDateFormat("dd-MMM-yyyy").parse(date)));
			} catch (ParseException e) {
				e.printStackTrace();
			}
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFindOne("CUST_DAILY_DIET_DETOX", query, null).subscribe(res -> {
			if (res == null || res.isEmpty())
				promise.fail("No detox Dietplan available for today");
			else
				promise.complete(true);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Fetch detox today's dietplan status.
	 * 
	 * @param email
	 * @param isDetox
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<Boolean> fetchDetoxDietPlanStatus(String email, String date, String traceId) {
		String method = "MongoRepositoryWrapper fetchDetoxDietPlanStatus() " + traceId + "-[" + email + "]";
		Promise<Boolean> promise = Promise.promise();
		JsonObject query = new JsonObject();
		if (null == date || "".equals(date)) {
			query.put("_id.email", email).put("isDetox", true).put("$or",
					new JsonArray().add(new JsonObject().put("_id.date", ApiUtils.getFilteredFutureDateddMMMyyyy(0)))
							.add(new JsonObject().put("_id.date", ApiUtils.getFilteredFutureDateddMMMyyyy(1)))
							.add(new JsonObject().put("_id.date", ApiUtils.getFilteredFutureDateddMMMyyyy(2)))
							.add(new JsonObject().put("_id.date", ApiUtils.getFilteredFutureDateddMMMyyyy(3)))
							.add(new JsonObject().put("_id.date", ApiUtils.getFilteredFutureDateddMMMyyyy(4)))
							.add(new JsonObject().put("_id.date", ApiUtils.getFilteredFutureDateddMMMyyyy(5)))
							.add(new JsonObject().put("_id.date", ApiUtils.getFilteredFutureDateddMMMyyyy(6)))
							.add(new JsonObject().put("_id.date", ApiUtils.getFilteredFutureDateddMMMyyyy(7))));

			logger.info("##### " + method + " QUERY -->> " + query);
			client.rxFind("DETOX_DIETPLAN_STATUS", query).map(map -> {
				return map;
			}).subscribe(res -> {
				List<Boolean> list = new ArrayList<>();
				Boolean isDetoxrecordFound = false;
				if (null != res && !res.isEmpty()) {
					logger.info("##### " + method + " RES SIZE -->> " + res.size());
					res.forEach(action -> {
						JsonObject json = (JsonObject) action;
						logger.info("##### " + method + " JSON -->> " + json);
						if (null != json && json.getBoolean("isDetox")) {
							logger.info("##### " + method + " CHECK -->> " + json.getBoolean("isDetox"));
							list.add(Boolean.TRUE);
						}
					});
				}

				if (null != list && list.size() > 0)
					isDetoxrecordFound = Boolean.TRUE;

				logger.info("##### " + method + " ISDETOX RECORD FOUND -->> " + isDetoxrecordFound);
				promise.complete(isDetoxrecordFound);

			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		} else {
			query.put("_id.email", email).put("isDetox", true).put("_id.date", date);
			try {
				client.rxFindOne("DETOX_DIETPLAN_STATUS", query, null).subscribe(res -> {
					Boolean isDetoxrecordFound = false;
					if (null != res)
						isDetoxrecordFound = Boolean.TRUE;

					logger.info("##### " + method + " ISDETOX RECORD FOUND -->> " + isDetoxrecordFound);
					promise.complete(isDetoxrecordFound);
				}, (ex) -> {
					logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
					promise.fail(ex.getMessage());
				});
			} catch (Exception ex) {
				logger.error("##### " + method + " EXCEPTION -->> " + ex.getMessage());
			}
		}

		return promise.future();
	}

	/**
	 * Save/Update detox dietplan status.
	 * 
	 * @param email
	 * @param isDetox
	 * @param isDetoxDietplanStatusFound
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> saveOrUpdateDetoxDietPlanStatus(String email, boolean isDetox,
			boolean isDetoxDietplanStatusFound, String date, String traceId) {
		String method = "MongoRepositoryWrapper saveOrUpdateDetoxDietPlanStatus() " + traceId + "-[" + email + "]";
		Calendar cal1 = Calendar.getInstance(); // creates calendar
		cal1.setTime(new Date()); // sets calendar time/date
		cal1.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		cal1.add(Calendar.MINUTE, 30); // add 30 minutes
		String currentDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(cal1.getTime());
//		String currentDate = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.US).format(cal1.getTime());
		Promise<JsonObject> promise = Promise.promise();
		try {
			JsonObject query = new JsonObject();
			JsonObject response = new JsonObject();
			response.put("code", "0000");
			response.put("message", "success");
			if (!isDetoxDietplanStatusFound) {
				query.put("_id", new JsonObject().put("email", email).put("date", date));
				query.put("isDetox", isDetox);
				query.put("createdDateTime", new JsonObject().put("$date", currentDate));
				query.put("updatedDateTime", new JsonObject().put("$date", currentDate));
				logger.info("##### " + method + " QUERY (SAVE) -->> " + query);
				client.rxSave("DETOX_DIETPLAN_STATUS", query).subscribe(res -> {
					logger.info("##### " + method + " DETOX DIETPLAN STATUS SAVED SUCCESSFULLY.");
					promise.complete(response);
				}, (ex) -> {
					logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
					promise.fail(ex.getMessage());
				});
			} else {
				query.put("_id", new JsonObject().put("email", email).put("date", date));
				JsonObject update = new JsonObject().put("$set", new JsonObject().put("isDetox", isDetox)
						.put("updatedDateTime", new JsonObject().put("$date", currentDate)));
				logger.info("##### " + method + " QUERY (UPDATE)  -->> " + query);
				logger.info("##### " + method + " PAYLOAD (UPDATE -->> " + update);
				client.rxUpdateCollection("DETOX_DIETPLAN_STATUS", query, update).subscribe(res -> {
					logger.info("##### " + method + " DETOX DIETPLAN STATUS UPDATED SUCCESSFULLY.");
					promise.complete(response);
				}, ex -> {
					promise.fail("unable to update");
				});
			}
		} catch (Exception e) {
			logger.error("##### " + method + " EXCEPTION -->> " + e.getMessage());
			e.printStackTrace();
		}

		return promise.future();
	}

	/**
	 * Fetch last calories history.
	 * 
	 * @param email
	 * @param noOfDays
	 * @param data
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> fetchLastCaloriesHistoryDetox(String email, Integer noOfDays, JsonObject data,
			String traceId) {
		String method = "MongoRepositoryWrapper fetchLastCaloriesHistory() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("_id.email", email);
		if ("0001".equals(data.getString("code"))) {
			promise.complete(data);
		} else {
			client.rxFind("CUST_DAILY_DIET_DETOX", query).map(map -> {
				return map;
			}).subscribe(res1 -> {
				JsonObject response = new JsonObject();
				List<JsonObject> list = new ArrayList<>();
				JsonObject json1 = new JsonObject();
				json1.put("cumulative", 0.0);
				res1.forEach(action -> {
					Calendar calendar = Calendar.getInstance();
					calendar.setTime(new Date());
					calendar.add(Calendar.DAY_OF_MONTH, -noOfDays);
					String createdDateString = action.getJsonObject("_id").getString("date");
					Date createdDate;
					try {
						createdDate = new SimpleDateFormat("ddMMyyyy").parse(createdDateString);
						if (createdDate.after(calendar.getTime())) {
							JsonObject json = new JsonObject();
							String recordDate = new SimpleDateFormat("dd-MMM-yyyy")
									.format(new SimpleDateFormat("ddMMyyyy")
											.parse(action.getJsonObject("_id").getString("date")));
							json.put("date", recordDate);
							json.put("calories",
									ApiUtils.getDecimal(action.getJsonObject("data").getDouble("totalCalories")));
							json.put("recommendedCal",
									ApiUtils.getDecimal(action.getJsonObject("data").getDouble("recomended")));
							json1.put("cumulative", json1.getDouble("cumulative")
									+ action.getJsonObject("data").getDouble("recomended"));
							list.add(json);
						}
					} catch (ParseException e) {
						e.printStackTrace();
					}
				});

				response.put("code", "0000");
				response.put("message", "Success");
				response.put("email", email);
				response.put("targetCaloriesToBurn", ApiUtils.getDecimal(data.getDouble("targetCaloriesToBurn")));
				response.put("activityLevels", data.getInteger("activityLevels"));
				response.put("cumultativeCal", ApiUtils.getDecimal(json1.getDouble("cumulative")));
				response.put("caloriesBurnRateDay",
						new DecimalFormat("##.0000").format(data.getDouble("caloriesBurnRateDay")));
				response.put("caloriesBurnRateNight",
						new DecimalFormat("##.0000").format(data.getDouble("caloriesBurnRateNight")));
				response.put("latestWeight", data.getDouble("weight"));
				response.put("count", list.size());
				response.put("response", list);
				promise.complete(response);
				logger.info("##### " + method + " RESPONSE -->> " + response);
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		}

		return promise.future();
	}

	/**
	 * Get customer default profile.
	 * 
	 * @param email
	 * @param jsonObject
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> fetchHelp(String email, String traceId) {
		String method = "MongoRepositoryWrapper fetchHelp() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		try {
			JsonObject query = new JsonObject();
			query.put("_id", "cust_help_master");
			client.rxFindOne("CUST_HELP_MASTER", query, null).subscribe(res -> {
				JsonArray jsonArr = res.getJsonObject("help").getJsonArray("categories");
				jsonArr.forEach(json -> {
					JsonObject jObj = (JsonObject) json;
					JsonArray jArr = jObj.getJsonArray("qna");
					jArr.forEach(jsonObj -> {
						JsonObject jsonObject = (JsonObject) jsonObj;
						if (this.config.getBoolean("isPaymentCancelOptionAvailable")
								&& jsonObject.getInteger("code") == 12)
							jsonObject.put("answer",
									"You can cancel with in 24 hrs. Send us email at admin@smartdietplanner.com");
					});

				});

				promise.complete(new JsonObject().put("code", "0000").put("message", "success").put("categories",
						res.getJsonObject("help").getJsonArray("categories")));
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		} catch (Exception ex) {
			logger.error("##### " + method + " EXCEPTION -->> " + ex.getMessage());
		}

		return promise.future();
	}

	/**
	 * Customer payment details.
	 * 
	 * @param email
	 * @param noOfDays
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getCustPaymentDetails(String email, Integer noOfDays, String traceId) {
		String method = "MongoRepositoryWrapper getCustPaymentDetails() " + traceId;
		Promise<JsonObject> promise = Promise.promise();
		JsonArray jArr = new JsonArray();
		JsonObject query = new JsonObject().put("emailId", email)
				.put("paymentId", new JsonObject().put("$ne", JsonObject.mapFrom(null)))
				.put("amount", new JsonObject().put("$gt", 0));
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFind("PAYMENT_DETAIL", query).map(map -> {
			return map;
		}).subscribe(res -> {
			res.forEach(action -> {
				JsonObject json = new JsonObject();
				try {
					Integer amount = action.getInteger("amount");
					json.put("amount", amount);
					Integer durationValidInDays = action.getInteger("durationInDays");
					String createdDateTimeStr = new SimpleDateFormat("dd-MMM-yyyy").format(
							new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(action.getString("createdDate")));
					json.put("subscribedDate", createdDateTimeStr);
					Date createdDate = new SimpleDateFormat("dd-MMM-yyyy").parse(createdDateTimeStr);
					json.put("durationInDays", durationValidInDays);
					String expiryDateStr = new SimpleDateFormat("dd-MMM-yyyy")
							.format(AppUtil.getExpiryDate(createdDate, durationValidInDays).getTime());
					json.put("expiryDate", expiryDateStr);
					jArr.add(json);
				} catch (Exception e) {
					logger.debug("##### " + method + " ERROR -->> " + e.getMessage());
					e.printStackTrace();
				}
			});

			JsonObject response = new JsonObject();
			response.put("code", "0000");
			response.put("message", "success");
			response.put("email", email);
			response.put("paymentDetails", jArr);
			promise.complete(response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Fetch detox today's dietplan status.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> fetchDetoxDietPlanStatusFlagForLastWeek(String email, String traceId) {
		String method = "MongoRepositoryWrapper fetchDetoxDietPlanStatusFlagForLastWeek() " + traceId + "-[" + email
				+ "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id.email", email);
		JsonArray jArr = new JsonArray();
		IntStream.rangeClosed(0, config.getInteger("noOfDays")).forEach(day -> {
			jArr.add(new JsonObject().put("_id.date", ApiUtils.getFilteredDate(day)));
		});

		query.put("$or", jArr);
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFind("DETOX_DIETPLAN_STATUS", query).map(map -> {
			return map;
		}).subscribe(res -> {
			JsonArray jsonArr = new JsonArray();
			if (null != res && !res.isEmpty()) {
				logger.info("##### " + method + " RES SIZE -->> " + res.size());
				res.forEach(action -> {
					JsonObject json = (JsonObject) action;
					if (json.getBoolean("isDetox"))
						jsonArr.add(json.getJsonObject("_id").getString("date"));
				});
			}

			JsonObject response = new JsonObject();
			response.put("code", "0000");
			response.put("message", "success");
			response.put("detoxAvailableOn", jsonArr);

			promise.complete(response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * fetch dietplan - Detox.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> fetchDietPlansDetox(String email, JsonObject data, String traceId) {
		String method = "MongoRepositoryWrapper fetchDietPlansDetox() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id.email", email);
		JsonArray jArr = new JsonArray();
		IntStream.rangeClosed(0, config.getInteger("noOfDays")).forEach(day -> {
			jArr.add(new JsonObject().put("_id.date", ApiUtils.getFilteredDateddMMyyyy(day)));
		});

		query.put("$or", jArr);
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFind("CUST_DAILY_DIET_DETOX", query).map(map -> {
			return map;
		}).subscribe(res -> {
			JsonArray jsonArray = new JsonArray();
			JsonArray dates = new JsonArray();
			JsonArray detoxAvailableOn = data.getJsonArray("detoxAvailableOn");
			if (null != res && !res.isEmpty()) {
				res.forEach(action -> {
					JsonObject fObj = (JsonObject) action;
					JsonArray jsonArr = new JsonArray();
					try {
						JsonObject jObj = new JsonObject();
						JsonObject jj = fObj.getJsonObject("data");
						String date = new SimpleDateFormat("dd-MMM-yyyy").format(
								new SimpleDateFormat("ddMMyyyy").parse(fObj.getJsonObject("_id").getString("date")));
						if (!dates.contains(date) && detoxAvailableOn.contains(date)) {
							jObj.put("date", date);
							jObj.put("type", "detox");
							jObj.put("totalCalories", jj.getDouble("totalCalories"));
							jObj.put("recommendedCalories", jj.getDouble("recomended"));
							JsonArray slotsArr = jj.getJsonArray("diets");
							slotsArr.forEach(diet -> {
								JsonObject json1 = (JsonObject) diet;
								JsonObject json = new JsonObject();
								json.put("slot", json1.getInteger("slot"));
								json.put("time", json1.getString("time"));
								json.put("calories", json1.getString("totalCalories"));
								jsonArr.add(json);
							});

							jObj.put("slot", jsonArr);
							jsonArray.add(jObj);
							dates.add(fObj.getJsonObject("_id").getString("date"));
						}
					} catch (ParseException e) {
						e.printStackTrace();
					}
				});
			}

			data.put("email", email);
			data.put("detoxAvailableOn", detoxAvailableOn);
			data.put("detoxDiets", jsonArray);

			promise.complete(data);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get dietplan for a week.
	 * 
	 * @param email
	 * @param data
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> fetchDietPlans(String email, JsonObject data, String traceId) {
		String method = "MongoRepositoryWrapper fetchDietPlans() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id.email", email);
		Map<String, JsonObject> detoxDietMap = new HashMap<>();
		data.getJsonArray("detoxDiets").forEach(diet -> {
			JsonObject dietPlan = (JsonObject) diet;
			detoxDietMap.put(dietPlan.getString("date"), dietPlan);
		});

		Map<String, JsonObject> normalDietMap = new HashMap<>();
		JsonArray jArr = new JsonArray();
		IntStream.rangeClosed(0, config.getInteger("noOfDays")).forEach(day -> {
			jArr.add(new JsonObject().put("_id.date", ApiUtils.getFilteredDateddMMyyyy(day)));
		});

		query.put("$or", jArr);
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFind("CUST_DAILY_DIET", query).map(map -> {
			return map;
		}).subscribe(res -> {
			JsonObject response = new JsonObject();
			if (null != res && !res.isEmpty()) {
				res.forEach(action -> {
					JsonObject fObj = (JsonObject) action;
					JsonArray jsonArr = new JsonArray();
					try {
						JsonObject jObj = new JsonObject();
						JsonObject jj = fObj.getJsonObject("data");
						String date = new SimpleDateFormat("dd-MMM-yyyy").format(
								new SimpleDateFormat("ddMMyyyy").parse(fObj.getJsonObject("_id").getString("date")));
						jObj.put("date", date);
						jObj.put("type", "normal");
						jObj.put("totalCalories", jj.getDouble("totalCalories"));
						jObj.put("recommendedCalories", jj.getDouble("recomended"));
						JsonArray slotsArr = jj.getJsonArray("diets");
						slotsArr.forEach(diet -> {
							JsonObject json1 = (JsonObject) diet;
							JsonObject json = new JsonObject();
							json.put("slot", json1.getInteger("slot"));
							json.put("time", json1.getString("time"));
							json.put("calories", json1.getString("totalCalories"));
							jsonArr.add(json);
						});

						jObj.put("slot", jsonArr);
						normalDietMap.put(date, jObj);
					} catch (ParseException e) {
						e.printStackTrace();
					}
				});
			}

			JsonArray jsonArray = new JsonArray();
			IntStream.rangeClosed(0, config.getInteger("noOfDays")).forEach(day -> {
				String date = ApiUtils.getFilteredDate(day);
				if (null != detoxDietMap && detoxDietMap.containsKey(date))
					jsonArray.add(detoxDietMap.get(date));
				else if (null != normalDietMap && normalDietMap.containsKey(date))
					jsonArray.add(normalDietMap.get(date));
			});

			response.put("code", "0000");
			response.put("message", "success");
			response.put("email", email);
			response.put("size", (null != jsonArray && !jsonArray.isEmpty() ? jsonArray.size() : 0));
			response.put("data", jsonArray);

			promise.complete(response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get all customers subscription details.
	 * 
	 * @param data
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getAllCustomersSubscriptionDetails(String traceId) {
		String method = "MongoRepositoryWrapper getAllCustomersSubscriptionDetails() " + traceId;
		JsonObject query = new JsonObject();
		query.put("amount", new JsonObject().put("$gt", 0));
		logger.info("##### " + method + " QUERY -->> " + query);
		Promise<JsonObject> promise = Promise.promise();
		client.rxFind("PLAN_SUBCRIPTION_DETAIL", query).map(map -> {
			return map;
		}).subscribe(res -> {
			JsonObject response = new JsonObject();
			JsonArray jArr = new JsonArray();
			JsonArray jArrEmails = new JsonArray();
			response.put("code", "0000");
			response.put("message", "success");
			if (!res.isEmpty()) {
				res.forEach(action -> {
					JsonObject jObj = new JsonObject();
					try {
						JsonObject json = new JsonObject();
						String email = action.getString("emailId");
						if (!jArrEmails.contains(email)) {
							json.put("email", email);
							json.put("amount", action.getInteger("amount"));
							json.put("expiryDate", action.getString("expiryDate"));
							json.put("SubscribedDate", action.getString("upgradedDate"));
							json.put("paymentId", action.getString("paymentId"));

							Date expiryDateTime = new SimpleDateFormat("dd-MMM-yyyy")
									.parse(action.getString("expiryDate"));
							String currentDateTimeInString = new SimpleDateFormat("dd-MMM-yyyy")
									.format(Calendar.getInstance().getTime());
							Date currentDateTime = new SimpleDateFormat("dd-MMM-yyyy").parse(currentDateTimeInString);
							json.put("isSubscriptionExpired", false);
							if (expiryDateTime.before(currentDateTime))
								json.put("isSubscriptionExpired", true);

							jArr.add(json);
							jArrEmails.add(email);
						}
					} catch (ParseException e) {
						e.printStackTrace();
					}
				});
			}

			response.put("size", jArrEmails.size());
			response.put("emails", jArrEmails);
			response.put("customers", jArr);
			promise.complete(response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Fetch customers for provided date else today.
	 * 
	 * @param data
	 * @param traceId
	 * @return Future<DiscountedCouponDetails>
	 */
	public Future<JsonObject> fetchRegisteredCustomersHavingPlanSubscribed(JsonObject data, String traceId) {
		String method = "MongoRepositoryWrapper fetchRegisteredCustomersHavingPlanSubscribed() " + traceId;
		Promise<JsonObject> promise = Promise.promise();

		Map<String, JsonObject> jMap = new HashMap<>();
		data.getJsonArray("customers").forEach(action -> {
			JsonObject json = (JsonObject) action;
			jMap.put(json.getString("email"), json);
		});

		client.rxFind("CUST_PROFILE", new JsonObject()).map(map -> {
			return map;
		}).subscribe(res -> {
			JsonArray jArr = new JsonArray();
			res.forEach(action -> {
				String email = action.getString("_id");
				if (data.getJsonArray("emails").contains(email)) {

					JsonObject json = jMap.get(action.getString("_id"));
					JsonObject jObj = action.getJsonObject("demographic");
					json.put("weight", jObj.getJsonObject("weight").getDouble("value") + " "
							+ jObj.getJsonObject("weight").getString("unit"));
					jMap.put(action.getString("_id"), json);
					jArr.add(json);
				}
			});

			data.put("customers", jArr);
			promise.complete(data);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get customer latest weight(s).
	 * 
	 * @param data
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> fetchRegisteredCustomersLatestWeightHavingPlanSubscribed(JsonObject data,
			String traceId) {
		String method = "MongoRepositoryWrapper fetchRegisteredCustomersLatestWeightHavingPlanSubscribed() " + traceId;
		Promise<JsonObject> promise = Promise.promise();
		Map<String, JsonObject> jMap = new HashMap<>();
		data.getJsonArray("customers").forEach(action -> {
			JsonObject json = (JsonObject) action;
			jMap.put(json.getString("email"), json);
		});

		JsonObject response = new JsonObject();
		client.rxFind("CUSTOMER_DAILY_WEIGHT", new JsonObject()).map(mapper -> {
			return mapper;
		}).subscribe(res -> {
			if (res != null && !res.isEmpty()) {
				Map<String, JsonArray> map = new HashMap<>();
				res.forEach(action -> {
					try {
						JsonObject jObj = new JsonObject();
						jObj.put("date", action.getString("date"));
						jObj.put("weight", action.getDouble("weight"));

						JsonArray jsonArray = new JsonArray();
						if (null != action.getJsonObject("_id").getString("email") && null != map) {
							if (map.containsKey(action.getJsonObject("_id").getString("email")))
								jsonArray = map.get(action.getJsonObject("_id").getString("email"));

							jsonArray.add(jObj);
							map.put(action.getJsonObject("_id").getString("email"), jsonArray);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				});

				map.forEach((k, v) -> {
					JsonObject json = new JsonObject();
					json.put("email", k);
					json.put("latestweights", v);
					JsonArray jArr = new JsonArray();
					if (data.containsKey("latestweights"))
						jArr = data.getJsonArray("latestweights");

					jArr.add(json);
					data.put("latestweights", jArr);
				});
			}
			promise.complete(data);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			response.put("code", "0001");
			response.put("messsage", ex.getMessage());
			promise.complete(response);
		});
		return promise.future();
	}

	/**
	 * Ftech detox today's dietplan.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<Boolean> fetchDetoxDefaultDays(String email, String traceId) {
		String method = "MongoRepositoryWrapper fetchDetoxDefaultDays() " + traceId + "-[" + email + "]";
		Promise<Boolean> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("_id", email);
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFindOne("CUST_DETOX_DEFAULT_DAYS", query, null).subscribe(res -> {
			if (res == null || res.isEmpty())
				promise.complete(true);
			else
				promise.fail("No default detox dafault days available.");
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get diet plan from cache.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getCustomerAllDietPlanStatusDetox(String email, JsonObject response, String traceId) {
		String method = "MongoRepositoryWrapper getCustomerAllDietPlanStatusDetox() " + traceId;
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id.email", email);
		client.rxFind("DETOX_DIETPLAN_STATUS", query).map(map -> {
			return map;
		}).subscribe(res1 -> {
			JsonArray jArr = new JsonArray();
			res1.forEach(action -> {
				String date = action.getJsonObject("_id").getString("date");
				try {
					logger.info("##### " + method + " DATE       -->> " + date);
					logger.info("##### " + method + " DATE CHECK -->> " + new Date()
							.before(new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").parse(date + " 23:59:59")));
					if (action.getBoolean("isDetox") && new Date()
							.before(new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").parse(date + " 23:59:59")))
						jArr.add(date);
				} catch (ParseException e) {
					e.printStackTrace();
				}
			});

			response.put("detoxFollowedDates", jArr);
			logger.info("##### " + method + " JSON ARRAYYYYY -->> " + jArr);
			promise.complete(response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get future followed days Detox.
	 * 
	 * @param email
	 * @param response
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getFutureFollowedDaysDetox(String email, JsonObject data, String traceId) {
		String method = "MongoRepositoryWrapper getFutureFollowedDaysDetox() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject();
		JsonObject response = new JsonObject();
		query.put("_id.email", email).put("$or",
				new JsonArray().add(new JsonObject().put("_id.date", ApiUtils.getFutureDate(0)))
						.add(new JsonObject().put("_id.date", ApiUtils.getFutureDate(1)))
						.add(new JsonObject().put("_id.date", ApiUtils.getFutureDate(2)))
						.add(new JsonObject().put("_id.date", ApiUtils.getFutureDate(3)))
						.add(new JsonObject().put("_id.date", ApiUtils.getFutureDate(4)))
						.add(new JsonObject().put("_id.date", ApiUtils.getFutureDate(5)))
						.add(new JsonObject().put("_id.date", ApiUtils.getFutureDate(6)))
						.add(new JsonObject().put("_id.date", ApiUtils.getFutureDate(7))));
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFind("CUST_DAILY_DIET_DETOX", query).map(map -> {
			logger.info("##### " + method + " COUNT");
			return map;
		}).subscribe(res -> {
			JsonArray jsonArr = new JsonArray();
			JsonArray detoxFollowedDates = data.getJsonArray("detoxFollowedDates");
			JsonArray caloriesHistory = data.getJsonArray("response");
			if (null != res && !res.isEmpty())
				res.forEach(action -> {
					try {
						String date = new SimpleDateFormat("dd-MMM-yyyy").format(
								new SimpleDateFormat("ddMMyyyy").parse(action.getJsonObject("_id").getString("date")));
						if (null != detoxFollowedDates && !detoxFollowedDates.isEmpty()
								&& detoxFollowedDates.contains(date)) {
							jsonArr.add(date);
							data.put(date, action.getJsonObject("data").getDouble("totalCalories"));
						}
					} catch (ParseException e) {
						logger.error("##### " + method + " EXCEPTION -->> " + e.getMessage());
						e.printStackTrace();
					}
				});

			logger.info("##### " + method + " FINAL DATA -->> " + data);
			logger.info("##### " + method + " JSON ARRAY -->> " + detoxFollowedDates);
			caloriesHistory.forEach(action -> {
				JsonObject json = (JsonObject) action;
				String date = json.getString("date");
				if (detoxFollowedDates.contains(date) && data.containsKey(date)) {
					json.put("calories", String.valueOf(data.getDouble(date)));
					logger.info("##### " + method + " CALORIES [" + date + "] -->> " + json.getString("calories"));
				}
			});

			logger.info("##### " + method + " CALORIES HISTORY -->> " + caloriesHistory);
			response.put("code", data.getString("code"));
			response.put("message", data.getString("message"));
			response.put("email", data.getString("email"));
			response.put("targetCaloriesToBurn", data.getString("targetCaloriesToBurn"));
			response.put("activityLevels", data.getInteger("activityLevels"));
			response.put("cumultativeCal", data.getString("cumultativeCal"));
			response.put("caloriesBurnRateDay", data.getString("caloriesBurnRateDay"));
			response.put("caloriesBurnRateNight", data.getString("caloriesBurnRateNight"));
			response.put("latestWeight", data.getDouble("latestWeight"));
			response.put("detoxFollowedOn", data.getJsonArray("detoxFollowedDates"));
			response.put("count", data.getInteger("count"));
			response.put("response", caloriesHistory);
			logger.info("##### " + method + " RESPONSE (FINAL) -->> " + response);

			promise.complete(response);

		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get future followed days Detox.
	 * 
	 * @param email
	 * @param response
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getFutureFollowedDietPlansDetox(String email, JsonObject data, String traceId) {
		String method = "MongoRepositoryWrapper getFutureFollowedDietPlansDetox() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonArray caloriesHistory = data.getJsonArray("response");
		JsonArray jsonArray = data.getJsonArray("detoxFollowedOn");
		caloriesHistory.forEach(action -> {
			JsonObject json = (JsonObject) action;
			String date = json.getString("date");
			try {
				if (null != jsonArray && !jsonArray.isEmpty() && jsonArray.contains(date)) {
					logger.info("##### " + method + " DATE -->> " + date);
					date = new SimpleDateFormat("ddMMyyyy")
							.format(new SimpleDateFormat("dd-MMM-yyyy").parse(json.getString("date")));
					JsonObject query = new JsonObject().put("_id.email", email).put("_id.date", date);
					logger.info("##### " + method + " QUERY -->> " + query);
					client.rxFindOne("CUST_DAILY_DIET_DETOX", query, null).subscribe(ha -> {
						if (ha != null && !ha.isEmpty()) {
							JsonObject jsonObj = new JsonObject();
							logger.info("##### " + method + " RESPONSE RECEIVED -->> "
									+ ha.getJsonObject("data").getDouble("totalCalories"));
							json.put("calories", ha.getJsonObject("data").getDouble("totalCalories"));
							json.put("recommendedCal", ha.getJsonObject("data").getDouble("recomended"));
							jsonObj.put("calories", ha.getJsonObject("data").getDouble("totalCalories"));
							jsonObj.put("recommendedCal", ha.getJsonObject("data").getDouble("recomended"));
							jsonArray.add(jsonObj);
							logger.info("##### " + method + " JSONNNN -->> " + json);
						}
					}, (ex) -> {
						logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
						promise.fail(ex.getMessage());
					});
				}
			} catch (ParseException e) {
				e.printStackTrace();
			}
		});

		data.put("detoxFollowedCalories", jsonArray);

		promise.complete(data);
		logger.info("##### " + method + " RESPONSE (DATA) -->> " + data);

		return promise.future();
	}

	/**
	 * Get requested date dietplan detox.
	 * 
	 * @param email
	 * @param date
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getDietListForRequestedDateDetox(String email, String date, String traceId) {
		String method = "MongoRepositoryWrapper getDietListForRequestedDateDetox() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", new JsonObject().put("email", email).put("date", date));
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFindOne("CUST_DAILY_DIET_DETOX", query, null).subscribe(res -> {
			if (res != null && !res.isEmpty()) {
				Double calories = res.getJsonObject("data").getDouble("totalCalories");
				Double recommended = res.getJsonObject("data").getDouble("recomended");

//				promise.complete(res.getJsonObject("data"));
//				promise.complete(json);
			} else {
				logger.info("##### " + method + " FAILED TO FETCH CACHE DIET");
				promise.fail("No Diet saved for today");
			}
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Get plan subscription details.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getSubscribedDetails(String email, String traceId) {
		String method = "MongoRepositoryWrapper getSubscribedDetails() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("emailId", email);
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFindOne("PLAN_SUBCRIPTION_DETAIL", query, null).subscribe(res -> {
			if (res != null && !res.isEmpty()) {
				promise.complete(new JsonObject().put("amount", res.getInteger("amount")));
			} else {
				promise.fail("No subscription");
			}
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get plan subscription details.
	 * 
	 * @param email
	 * @param data
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getCustSubscribedDetails(String email, JsonObject data, String traceId) {
		String method = "MongoRepositoryWrapper getCustSubscribedDetails() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("emailId", email);
		logger.info("##### " + method + " QUERY -->> " + query);
		client.rxFindOne("PLAN_SUBCRIPTION_DETAIL", query, null).subscribe(res -> {
			if (res != null && !res.isEmpty())
				data.put("amount", res.getDouble("amount"));

			promise.complete(data);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Update payment details.
	 * 
	 * @param email
	 * @param request
	 * @param data    - subscribed amout detail
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> updatePaymentDetails(String email, JsonObject request, JsonObject data, String traceId) {
		String method = "MongoRepositoryWrapper updatePaymentDetails() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.HOUR_OF_DAY, 10); // adds ten hour
		calendar.add(Calendar.MINUTE, 30); // add 30 minutes
		JsonObject query = new JsonObject().put("emailId", email);
		logger.info("##### " + method + " DATA    -->> " + data);
		logger.info("##### " + method + " REQUEST -->> " + request);
		JsonObject payload = new JsonObject().put("$set",
				new JsonObject().put("expiryDate", request.getString("expiryDate")).put("source", "iOS")
						.put("upgradedDate", new SimpleDateFormat("dd-MMM-yyyy").format(calendar.getTime()))
						.put("amount", data.getDouble("amount") + request.getDouble("amount"))
						.put("recentPaidAmount", request.getDouble("amount")));
		logger.info("##### " + method + " QUERY   -->> " + query);
		logger.info("##### " + method + " PAYLOAD -->> " + payload);
		client.rxUpdateCollection("PLAN_SUBCRIPTION_DETAIL", query, payload).subscribe(res1 -> {
			logger.info("##### " + method + " SUCCESSFULLY UPDATED PLAN SUBSCRIPTION DETAIL.");
			data.put("code", "000").put("message", "Success")
					.put("upgradedDate", new SimpleDateFormat("dd-MMM-yyyy").format(calendar.getTime()))
					// .put("source", "iOS").put("amount", data.getInteger("amount") +
					// request.getInteger("amount"))
					.put("source", "iOS").put("amount", request.getInteger("amount"))
					.put("recentPaidAmount", request.getInteger("amount"))
					.put("planExpiryDate", request.getString("expiryDate"));

			promise.complete(data);
		}, ex -> {
			promise.fail("unable to update");
		});

		return promise.future();
	}

	/**
	 * Check if plan is active.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getSbscriptionDetails(String email, String traceId) {
		String method = "MongoRepositoryWrapper isPlanActive() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject();
		query.put("emailId", email);
		query.put("isActive", true);
		logger.debug("##### " + method + " QUERY -->> " + query);
		client.rxFindOne("PLAN_SUBCRIPTION_DETAIL", query, null).subscribe(res -> {
			JsonObject response = new JsonObject();
			if (res != null && !res.isEmpty()) {
				response.put("planExpiryDate", res.getString("expiryDate"));
				if (res.containsKey("recentPaidAmount"))
					response.put("recentPaidAmount", res.getString("recentPaidAmount"));
				promise.complete(response);
			} else {
				promise.fail("One plan already taken");
			}
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get the customer profile.
	 * 
	 * @param email
	 * @param request
	 * @param traceId
	 * 
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getCustomerProfile(String email, JsonObject request, String traceId) {
		String method = "MongoRepositoryWrapper getCustomerProfile() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);
		logger.info("##### " + method + " REQUEST -->> " + request);
		logger.info("##### " + method + " QUERY   -->> " + query);
		client.rxFindOne("CUST_PROFILE", query, null).subscribe(res -> {
			if (res == null || res.isEmpty()) {
				promise.fail("invalid customer");
			} else {
				Double targetedWeight = request.getDouble("targetedWeight");
				res.getJsonObject("demographic").put("suggestedWeight", targetedWeight);
				promise.complete(res);
			}
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Update customer profile.
	 * 
	 * @param document
	 * @param request
	 * @param traceId
	 * @return Future<String>
	 */
	public Future<JsonObject> updateCustomerPofile(String email, JsonObject data, String traceId) {
		String method = "MongoRepositoryWrapper updateCustomerPofile() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);
		JsonObject payload = new JsonObject().put("$set",
				new JsonObject().put("demographic", data.getJsonObject("demographic")));
		logger.info("##### " + method + " QUERY   -->> " + query);
		logger.info("##### " + method + " PAYLOAD -->> " + payload);
		client.rxUpdateCollection("CUST_PROFILE", query, payload).subscribe(res -> {
			logger.info("##### " + method + " CUSTOMER PROFILE UPDATED SUCCESSFULLY.");
			promise.complete(data);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get the customer profile.
	 * 
	 * @param email
	 * @param data
	 * @param traceId
	 * 
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getCustomerTargetedWeight(String email, JsonObject data, String traceId) {
		String method = "MongoRepositoryWrapper getCustomerTargetedWeight() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		Calendar calendar = Calendar.getInstance(); // creates calendar
		calendar.setTime(new Date()); // sets calendar time/date
		calendar.add(Calendar.HOUR_OF_DAY, 10); // adds ten hours
		calendar.add(Calendar.MINUTE, 30); // add 30 minutes
		String currentDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(calendar.getTime());
		JsonObject query = new JsonObject().put("_id", email);
		logger.info("##### " + method + " QUERY   -->> " + query);
		client.rxFindOne("CUST_TARGETED_WEIGHT", query, null).subscribe(res -> {
			JsonObject json = new JsonObject();
			JsonArray jsonArray = new JsonArray();
			json.put("counter", 1);
			JsonObject jsonObject = new JsonObject();
			jsonObject.put("isFirstRecord", true);
			if (null != res && !res.isEmpty()) {
				jsonArray = res.getJsonArray("targetedWeights");
				if (null != jsonArray && !jsonArray.isEmpty() && jsonArray.size() > 0)
					json.put("counter", jsonArray.size() + 1);

				jsonArray.forEach(action -> {
					JsonObject jObj = (JsonObject) action;
					jObj.put("isRecordValid", false);
				});

				jsonObject.put("isFirstRecord", false);
			}

			logger.info("##### " + method + " EXIT FOUND.");
			json.put("targetedWeight", data.getJsonObject("demographic").getDouble("suggestedWeight"));
			json.put("isRecordValid", true);
			json.put("createdDate", ApiUtils.getCurrentDateInddMMMyyyyFormat(0));
			json.put("createdDateTime", ApiUtils.getCurrentTime());
			json.put("date", currentDate);
			jsonArray.add(json);

			jsonObject.put("targetedWeights", jsonArray);
			logger.info("##### " + method + " RESPONSE   -->> " + jsonObject);
			promise.complete(jsonObject);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Save customer targeted weight.
	 * 
	 * @param email
	 * @param data
	 * @param traceId
	 * @return Future<String>
	 */
	public Future<JsonObject> saveOrUpdateCustomerTargetedWeightCounterAndDateTime(String email, JsonObject data,
			String traceId) {
		String method = "MongoRepositoryWrapper saveOrUpdateCustomerTargetedWeightCounterAndDateTime() " + traceId
				+ "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);
		if (data.getBoolean("isFirstRecord")) {
			query.put("targetedWeights", data.getJsonArray("targetedWeights"));
			logger.info("##### " + method + " QUERY   -->> " + query);
			client.rxSave("CUST_TARGETED_WEIGHT", query).subscribe(res -> {
				logger.info("##### " + method + " CUSTOMER TARGETED WEIGHT UPDATED SUCCESSFULLY.");
//				promise.complete(data);
				promise.complete(new JsonObject().put("code", "0000").put("message", "Success"));
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		} else {
			JsonObject payload = new JsonObject().put("$set",
					new JsonObject().put("targetedWeights", data.getJsonArray("targetedWeights")));
			client.rxUpdateCollection("CUST_TARGETED_WEIGHT", query, payload).subscribe(res -> {
				logger.info("##### " + method + " CUSTOMER PROFILE UPDATED SUCCESSFULLY.");
//				promise.complete(data);
				promise.complete(new JsonObject().put("code", "0000").put("message", "Success"));
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});

		}

		return promise.future();
	}

	/**
	 * Get customer current weight.
	 * 
	 * @param profile
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getCustomerCurrentWeights(String email, JsonObject request, String traceId) {
		String method = "MongoRepositoryWrapper getCustomerCurrentWeights() " + traceId + "-[" + email + "]";
		JsonObject response = new JsonObject();
		response.put("isCurrentWeightAvailable", false);
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date()); // sets calendar time/date
		calendar.add(Calendar.HOUR_OF_DAY, 10); // adds ten hours
		calendar.add(Calendar.MINUTE, 30); // add 30 minutes
		String currentDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(calendar.getTime());
		Promise<JsonObject> promise = Promise.promise();
		String date = new SimpleDateFormat("ddMMyyyy").format(calendar.getTime());
		JsonObject query = new JsonObject().put("_id.email", email).put("_id.date", date);
		client.rxFindOne("CUSTOMER_DAILY_WEIGHT", query, null).subscribe(res -> {
			if (null != res && !res.isEmpty()) {
				response.put("code", "0000");
				response.put("message", "Success");
				response.put("isCurrentWeightAvailable", true);
				response.put("weight", request.getDouble("currentWeight"));
				response.put("updatedDate", new JsonObject().put("$date", currentDate));
				promise.complete(res);
			} else {
				response.put("code", "0001");
				response.put("message", "Current weight is unavailable.");
			}
		}, (ex) -> {
			logger.error("##### " + method + " DATA -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Update customer current weight.
	 * 
	 * @param email
	 * @param updatedCurrentWeight
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> updateCustomerCurrentWeight(String email, JsonObject request, String traceId) {
		String method = "MongoRepositoryWrapper updateCustomerCurrentWeight() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		Calendar calendar = Calendar.getInstance(); // creates calendar
		calendar.setTime(new Date()); // sets calendar time/date
		calendar.add(Calendar.HOUR_OF_DAY, 10); // adds ten hours
		calendar.add(Calendar.MINUTE, 30); // add 30 minutes
		String date = new SimpleDateFormat("ddMMyyyy").format(calendar.getTime());
		JsonObject query = new JsonObject().put("_id.email", email).put("_id.date", date);
		JsonObject payload = new JsonObject().put("$set",
				new JsonObject().put("weight", request.getDouble("currentWeight")).put("isCurrentWeightUpdated", true));
		logger.info("##### " + method + " QUERY   -->> " + query);
		logger.info("##### " + method + " PAYLOAD -->> " + payload);
		client.rxUpdateCollection("CUSTOMER_DAILY_WEIGHT", query, payload).subscribe(res -> {
			logger.info("##### " + method + " CUSTOMER DAILY WEIGHT UPDATED SUCCESSFULLY.");
			promise.complete(new JsonObject().put("code", "0000").put("message", "Success"));
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get the customer profile.
	 * 
	 * @param email
	 * @param request
	 * @param traceId
	 * 
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> getCustomerDemographic(String email, JsonObject request, String traceId) {
		String method = "MongoRepositoryWrapper getCustomerDemographic() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);
		logger.info("##### " + method + " REQUEST -->> " + request);
		logger.info("##### " + method + " QUERY   -->> " + query);
		client.rxFindOne("CUST_PROFILE", query, null).subscribe(res -> {
			if (res == null || res.isEmpty()) {
				promise.fail("invalid customer");
			} else {
				JsonObject demographic = res.getJsonObject("demographic");
				demographic.put("currentWeight", request.getDouble("currentWeight"));
				promise.complete(demographic);
			}
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Update customer profile's current weight.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> updateCustomerProfilesCurrentWeight(String email, JsonObject request, JsonObject data,
			String traceId) {
		String method = "MongoRepositoryWrapper updateCustomerProfilesCurrentWeight() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);
		JsonObject payload = new JsonObject().put("$set", new JsonObject().put("demographic", data));
		logger.info("##### " + method + " QUERY   -->> " + query);
		logger.info("##### " + method + " PAYLOAD -->> " + payload);
		client.rxUpdateCollection("CUST_PROFILE", query, payload).subscribe(res -> {
			logger.info("##### " + method + " CUSTOMER PROFILE CURRENT WEIGHT UPDATED SUCCESSFULLY.");
			promise.complete(new JsonObject().put("code", "0000").put("message", "Success"));
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get customer profile.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<Boolean> getCustProfile(String email, String traceId) {
		String method = "MongoRepositoryWrapper getProfile() " + traceId + "-[" + email + "]";
		Promise<Boolean> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);
		client.rxFindOne("CUST_PROFILE", query, null).subscribe(res -> {
			if (res == null || res.isEmpty())
				promise.fail("Invalid customer");
			else {
				JsonObject profile = res.getJsonObject("profile");
				if (null != profile && profile.containsKey("country") && null != profile.getValue("country")
						&& !"IN".equalsIgnoreCase(profile.getString("country")))
					promise.complete(true);
				else
					promise.fail("Invalid customer");

			}
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Get actual slot time.
	 * 
	 * @param selectedTime
	 * @param isComparisonRequired
	 * @param time
	 * @param isSleepSlot
	 * @return String
	 */
	private String getActualSlotTime(String selectedTime, boolean isComparisonRequired, int time, boolean isSleepSlot) {
		SimpleDateFormat parseFormat = new SimpleDateFormat("hh:mm a");
		String breakfastStartTime = "9:30 AM";
		String breakfastCutoffTime = "11:00 AM";
		String sleepStartTime = "8:00 PM";
		String sleepCutoffTime = "9:30 PM";
		Calendar cal = Calendar.getInstance();
		Date date;
		String actualTime = "";
		if (!isComparisonRequired && !isSleepSlot) {
			try {
				date = parseFormat.parse(selectedTime);
				cal.setTime(date);
				cal.add(Calendar.MINUTE, time);
				actualTime = parseFormat.format(cal.getTime());
			} catch (ParseException e) {
				e.printStackTrace();
			}
		} else {
			try {
				Date d1 = parseFormat.parse(selectedTime);
				if (isComparisonRequired && !isSleepSlot) {
					Date d2 = parseFormat.parse(breakfastStartTime);
					Date d3 = parseFormat.parse(breakfastCutoffTime);
					if ((d1.equals(d2) || d1.after(d2)) && (d1.before(d3) || d1.equals(d3))) {
						return breakfastCutoffTime;
					} else if (d1.after(d3)) {
						return breakfastCutoffTime;
					} else if (d1.before(d2)) {
						return breakfastStartTime;
					} else {
						return breakfastStartTime;
					}
				} else {
					Date d4 = parseFormat.parse(sleepStartTime);
					Date d5 = parseFormat.parse(sleepCutoffTime);
					if (d1.after(d5)) {
						return sleepStartTime;
					} else if (d1.before(d4)) {
						return selectedTime;
					} else {
						return sleepStartTime;
					}
				}
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		return actualTime;
	}

	/**
	 * Get meal by food code.
	 * 
	 * @param code
	 * @param datas
	 * @return JsonObject
	 */
	private JsonObject getMealByCode(String code, List<JsonObject> datas) {
		for (JsonObject jsonObject : datas) {
			if (jsonObject.getString("code").equalsIgnoreCase(code)) {
				return jsonObject;
			}
		}
		return null;
	}

	/**
	 * Fetch customer survey.
	 * 
	 * @param email
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<Boolean> fetchCustSurvey(String email, String traceId) {
		String method = "MongoRepositoryWrapper fetchCustSurvey() " + traceId + "-[" + email + "]";
		Promise<Boolean> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);
		client.rxFindOne("CUST_SURVEY", query, null).subscribe(res -> {
			boolean isCustValid = false;
			if (null != res && !res.isEmpty())
				isCustValid = true;

			promise.complete(isCustValid);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Save/Update customer survey.
	 * 
	 * @param email
	 * @param data
	 * @param request
	 * @param traceId
	 * @return Future<String>
	 */
	public Future<JsonObject> saveOrUpdateCustSurvey(String email, boolean isRecordAvailable, JsonObject request,
			String traceId) {
		String method = "MongoRepositoryWrapper saveOrUpdateCustSurvey() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		String dateTime = ApiUtils.getCurrentTime();
		JsonObject query = new JsonObject().put("_id", email);
		if (!isRecordAvailable) {
			if (request.containsKey("reason")) {
				query.put("reason", request.getString("reason"));
				query.put("offerAvailed", false);
				query.put("createdOn", ApiUtils.getCurrentDateInddMMMyyyyFormat(0));
				query.put("createdDateTime", dateTime);
				logger.info("##### " + method + " QUERY   -->> " + query);
				client.rxSave("CUST_SURVEY", query).subscribe(res -> {
					logger.info("##### " + method + " CUSTOMER SURVEY SAVED SUCCESSFULLY.");
					promise.complete(new JsonObject().put("code", "0000").put("message", "Success"));
				}, (ex) -> {
					logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
					promise.fail(ex.getMessage());
				});
			} else {
				promise.complete(new JsonObject().put("code", "0001").put("message",
						"Request is invalid - 'reason' field is missing"));
			}
		} else {
			if (request.containsKey("offerAvailed")) {
				JsonObject payload = new JsonObject().put("$set", new JsonObject()
						.put("offerAvailed", request.getBoolean("offerAvailed")).put("offerAvailedOn", dateTime));
				logger.info("##### " + method + " QUERY   -->> " + query);
				logger.info("##### " + method + " PAYLOAD -->> " + payload);
				client.rxUpdateCollection("CUST_SURVEY", query, payload).subscribe(res -> {
					logger.info("##### " + method + " CUSTOMER SURVEY UPDATED SUCCESSFULLY.");
					promise.complete(new JsonObject().put("code", "0000").put("message", "Success"));
				}, (ex) -> {
					logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
					promise.fail(ex.getMessage());
				});
			} else {
				promise.complete(new JsonObject().put("code", "0001").put("message",
						"Request is invalid - 'offerAvailed' field is missing."));
			}
		}

		return promise.future();
	}

	/**
	 * Fetch customer profile.
	 * 
	 * @param email
	 * @param request
	 * @param traceId
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> fetchProfile(String email, JsonObject request, String traceId) {
		String method = "MongoRepositoryWrapper fetchProfile() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);
		logger.info("##### " + method + " QUERY -->> " + query);
		JsonObject json = new JsonObject();
		client.rxFindOne("CUST_PROFILE", query, null).subscribe(res -> {
			json.put("code", "0001");
			json.put("message", "Request is invalid - 'Customer profile' unavailable.");
			json.put("isRequestValid", false);
			if (res == null || res.isEmpty()) {
				logger.info("##### " + method + " CUSTOMER PROFILE IS NOT AVAILABLE");
			} else {
				JsonObject profile = res.getJsonObject("profile");
				if (request.containsKey("mobile")) {
					request.getString("mobile").replace("+91", "");
					if (request.getString("mobile").replace("+91", "").length() == 10) {
						if (request.getString("mobile").replace("+91", "").matches("[0-9]+")) {
							profile.put("mobile", request.getString("mobile").replace("+91", ""));
							profile.put("mobileUpdatedOn", ApiUtils.getCurrentTime());
							json.put("code", "0000");
							json.put("message", "success");
							json.put("isRequestValid", true);
							json.put("profile", profile);
						} else {
							json.put("code", "0002");
							json.put("message", "Request is invalid - 'mobile' field value ["
									+ request.getString("mobile") + "] is invalid, it must be of 10 digits only.");
						}
					} else {
						json.put("code", "0003");
						json.put("message", "Request is invalid - 'mobile' field value [" + request.getString("mobile")
								+ "] is invalid, it must be of 10 digits only.");
					}
				} else {
					json.put("code", "0004");
					json.put("message",
							"Request is invalid - 'mobile' field is missing, it must be of 10 digits only.");
				}
			}

			promise.complete(json);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});
		return promise.future();
	}

	/**
	 * Save/Update customer mobile.
	 * 
	 * @param email
	 * @param data
	 * @param request
	 * @param traceId
	 * @return Future<String>
	 */
	public Future<JsonObject> updateCustMobile(String email, JsonObject data, JsonObject request,
			String traceId) {
		String method = "MongoRepositoryWrapper updateCustMobile() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = new JsonObject().put("_id", email);
		if (data.getBoolean("isRequestValid")) {
			JsonObject payload = new JsonObject().put("$set",
					new JsonObject().put("profile", data.getJsonObject("profile")));
			logger.info("##### " + method + " QUERY   -->> " + query);
			logger.info("##### " + method + " PAYLOAD -->> " + payload);
			client.rxUpdateCollection("CUST_PROFILE", query, payload).subscribe(res -> {
				logger.info("##### " + method + " CUSTOMER MOBILE UPDATED SUCCESSFULLY.");
				promise.complete(new JsonObject().put("code", "0000").put("message", "Success"));
			}, (ex) -> {
				logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
				promise.fail(ex.getMessage());
			});
		} else {
			promise.complete(
					new JsonObject().put("code", data.getString("code")).put("message", data.getString("message")));
		}

		return promise.future();
	}

	/**
	 * Fetch customer dietplan timings.
	 * 
	 * @param email
	 * @param profile
	 * @param traceId
	 * 
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> fetchDietPlanTimings(String email, String traceId) {
		String method = "MongoRepositoryWrapper fetchDietPlanTimings() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject response = new JsonObject();
		response.put("email", email);
		response.put("timings", JsonObject.mapFrom(null));
		JsonObject query = new JsonObject().put("_id", email);
		logger.info("##### " + method + " QUERY [" + response.getString("type") + "] -->> " + query);
		client.rxFindOne("CUST_DIETPLAN_TIMINGS", query, null).subscribe(res -> {
			if (null != res && !res.isEmpty())
				response.put("timings", res.getJsonArray("timings"));

			promise.complete(response);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Fetch customer dietplan timings.
	 * 
	 * @param email
	 * @param profile
	 * @param traceId
	 * 
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> fetchTodaysDiets(String email, JsonObject data, String date, String traceId) {
		String method = "MongoRepositoryWrapper fetchTodaysDiets() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		Integer[] slots = {2, 4, 7};
		JsonObject response = new JsonObject();
		JsonArray dietsArr = data.getJsonArray("diets");
		JsonArray jArr = new JsonArray();
		dietsArr.forEach(diet -> {
			JsonObject item = (JsonObject) diet;
			Integer slot = item.getInteger("slot");
			String code = item.getString("code");
			if (Arrays.asList(slots).contains(slot)) {
				JsonArray dataArr = data.getJsonArray("data");
				dataArr.forEach(foodItem -> {
					JsonObject items = new JsonObject();
					JsonObject food = (JsonObject) foodItem;
					String type = food.getString("Type");
					items.put("slot", slot);
					items.put("code", item.getString("code"));
					items.put("type", type);
					if (slot == 2 && (type.startsWith("W") || type.startsWith("w"))) {
						jArr.add(items);
					} else if (slot == 4 && (type.startsWith("A") || type.startsWith("a") || type.startsWith("C")
							|| type.startsWith("c"))) {
						jArr.add(items);
					} else if (slot == 7 && (type.startsWith("W") || type.startsWith("w") || type.startsWith("A")
							|| type.startsWith("a") || type.startsWith("C") || type.startsWith("c"))) {
						jArr.add(items);
					}
				});

			}
		});
		
		JsonObject partitionObject = new JsonObject();
		partitionObject.put("_email", email);
		partitionObject.put("date", ApiUtils.getFilteredFutureDateddMMyyyy(0));
		response.put("_id", partitionObject);
		response.put("items", jArr);
		response.put("createdDate", ApiUtils.getCurrentDateInddMMMyyyyFormat(0));
		response.put("createdDateTime", ApiUtils.getTodaysDateTime());
		data.put("todaysDiets", response);
		
		promise.complete(data);
		
		
//		query.put("_id.email", email).put("$or",
//				new JsonArray().add(new JsonObject().put("_id.date", ApiUtils.getFilteredFutureDateddMMyyyy(0)))
//						.add(new JsonObject().put("_id.date", ApiUtils.getFilteredFutureDateddMMyyyy(1)))
//						.add(new JsonObject().put("_id.date", ApiUtils.getFilteredFutureDateddMMyyyy(2)))
//						.add(new JsonObject().put("_id.date", ApiUtils.getFilteredFutureDateddMMyyyy(3)))
//						.add(new JsonObject().put("_id.date", ApiUtils.getFilteredFutureDateddMMyyyy(4)))
//						.add(new JsonObject().put("_id.date", ApiUtils.getFilteredFutureDateddMMyyyy(5))));
//		client.rxFind("CUST_DAILY_DIET_ITEMS", query).subscribe(items -> {
//			JsonObject dietItems = new JsonObject();
//			items.forEach(action -> {
//				try {
//					JsonArray jsonArray = action.getJsonArray("items");
//					jsonArray.forEach(item -> {
//						JsonObject json = (JsonObject)item;
//						
//						if (json.getInteger("slot"))
//					});
//					
//				} catch (Exception e) {
//					logger.error("##### " + method + " ERROR 1 -->> " + e.getMessage());
//					e.printStackTrace();
//				}
//			});
//		}, (ex) -> {
//			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
//			promise.fail(ex.getMessage());
//		});

		return promise.future();
	}

	/**
	 * Fetch customer dietplan timings.
	 * 
	 * @param email
	 * @param profile
	 * @param traceId
	 * 
	 * @return Future<JsonObject>
	 */
	public Future<FilterData> fetchPreviousDaysItems(String email, FilterData data, String traceId) {
		String method = "MongoRepositoryWrapper fetchPreviousDaysItems() " + traceId + "-[" + email + "]";
		Promise<FilterData> promise = Promise.promise();

		JsonObject query = new JsonObject();
		JsonArray jArr = new JsonArray();
		query.put("_id.email", email).put("$or",
				new JsonArray().add(new JsonObject().put("_id.date", ApiUtils.getFilteredFutureDateddMMyyyy(0)))
						.add(new JsonObject().put("_id.date", ApiUtils.getFilteredFutureDateddMMyyyy(1)))
						.add(new JsonObject().put("_id.date", ApiUtils.getFilteredFutureDateddMMyyyy(2)))
						.add(new JsonObject().put("_id.date", ApiUtils.getFilteredFutureDateddMMyyyy(3)))
						.add(new JsonObject().put("_id.date", ApiUtils.getFilteredFutureDateddMMyyyy(4)))
						.add(new JsonObject().put("_id.date", ApiUtils.getFilteredFutureDateddMMyyyy(5))));
		client.rxFind("CUST_DAILY_DIET_ITEMS", query).subscribe(items -> {
			JsonObject dietItems = new JsonObject();
			items.forEach(action -> {
				try {
					JsonArray jsonArray = action.getJsonArray("items");
					jsonArray.forEach(item -> {
						JsonObject json = (JsonObject)item;
						jArr.add(json);
					});
					
				} catch (Exception e) {
					logger.error("##### " + method + " ERROR 1 -->> " + e.getMessage());
					e.printStackTrace();
				}
			});
			
//			JsonObject response = new JsonObject();
//			response.put("previousDietItems", jArr);
			data.setPreviousDiets(jArr);
			
			promise.complete(data);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}

	/**
	 * Fetch customer dietplan timings.
	 * 
	 * @param email
	 * @param profile
	 * @param traceId
	 * 
	 * @return Future<JsonObject>
	 */
	public Future<JsonObject> SaveTodaysItems(String email, JsonObject data, String date, String traceId) {
		String method = "MongoRepositoryWrapper SaveTodaysItems() " + traceId + "-[" + email + "]";
		Promise<JsonObject> promise = Promise.promise();
		JsonObject query = data.getJsonObject("todaysDiets");
		logger.info("##### " + method + " QUERY (SAVE) -->> " + query);
		client.rxSave("CUST_DAILY_DIET_ITEMS", query).subscribe(res -> {
			logger.info("##### " + method + " DETOX DIETPLAN STATUS SAVED SUCCESSFULLY.");
			data.remove("todaysDiets");
			promise.complete(data);
		}, (ex) -> {
			logger.error("##### " + method + " ERROR -->> " + ex.getMessage());
			promise.fail(ex.getMessage());
		});

		return promise.future();
	}
}