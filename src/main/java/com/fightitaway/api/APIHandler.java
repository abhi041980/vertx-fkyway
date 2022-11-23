package com.fightitaway.api;

import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.time.StopWatch;

import com.fightitaway.service.CreatCustomerDietPlanRequest;
import com.fightitaway.service.CreatCustomerHabitPlanRequest;
import com.fightitaway.service.CreateProfileRequest;
import com.fightitaway.service.CustDietPreference;
import com.fightitaway.service.FightitawayService;
import com.fightitaway.service.FoodDetail;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;

/**
 * Request handler.
 * 
 * @author Smartdietplanner
 * @since 08-Aug-2020
 *
 */
public class APIHandler {

	public static final Logger logger = LoggerFactory.getLogger(APIHandler.class);

	private int index = 0;
	private String indexAndCalories = "";

	public FightitawayService flightitwayService;
	StopWatch watch;

	static final Integer LENGTH = 15;
	static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	static SecureRandom rnd = new SecureRandom();

	public APIHandler(FightitawayService flightitwayService) {
		this.flightitwayService = flightitwayService;
		this.watch = new StopWatch();
	}

	public String randomString() {
		StringBuilder sb = new StringBuilder(LENGTH);
		for (int i = 0; i < LENGTH; i++)
			sb.append(AB.charAt(rnd.nextInt(AB.length())));

		return "[" + sb.toString() + "]";
	}

	public void profile(RoutingContext context) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler profile() " + traceId;
		String email = context.user().principal().getString("email");
		logger.debug("##### " + method + " URI [" + email + "] -->> " + context.request().uri());
		logger.info("##### " + method + " EMAIL -->> " + email);
		flightitwayService.getProfile(email, traceId, res -> {

			if (res.succeeded()) {
				JsonObject response = res.result();
				logger.debug("##### " + method + " RESPONSE -->> " + response.toString());

				if (response != null && !response.isEmpty()) {
					response.remove("_id");
				}
				context.response().end(response.encodePrettily());
			} else {
				context.response().end(new JsonObject().put("message", res.cause().toString()).encodePrettily());
			}
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " PROFILE [" + timeElapsed + " milliseconds]");
	}

	public void updateDemographic(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler updateDemographic() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.debug("##### " + method + " URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "        EMAIL -->> " + email);
		logger.info("##### " + method + " BODY AS JSON -->> " + rc.getBodyAsJson().encodePrettily());
		flightitwayService.updateDemographic(email, rc.getBodyAsJson(), rc.request().uri(), traceId, h -> {
			rc.response().end(h.result().encode());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " UPDATE DEMOGRAPHIC [" + timeElapsed + " milliseconds]");
	}

	public void updateLifeStyle(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler updateLifeStyle() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.debug("##### " + method + " URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "        EMAIL -->> " + email);
		logger.info("##### " + method + " BODY AS JSON -->> " + rc.getBodyAsJson().encodePrettily());
		flightitwayService.updateLifeStyle(email, rc.getBodyAsJson(), rc.request().uri(), traceId, h -> {

			logger.info("##### " + method + " UPDATE LIFESTYLE RESPONSE -->> " + h.result());
			rc.response().end(h.result().encode());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " UPDATE LIFESTYLE [" + timeElapsed + " milliseconds]");
	}

	public void updateDiet(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler updateDiet() " + traceId;
		logger.info(rc.getBodyAsJson().encodePrettily());
		String email = rc.user().principal().getString("email");
		logger.debug("##### " + method + " URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "        EMAIL -->> " + email);
		logger.info("##### " + method + " BODY AS JSON -->> " + rc.getBodyAsJson().encodePrettily());
		flightitwayService.updateDiet(email, rc.getBodyAsJson(), rc.request().uri(), traceId, h -> {
			rc.response().end(h.result().encode());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " UPDATE DIET [" + timeElapsed + " milliseconds]");
	}

	public void getTest(RoutingContext rc) {
		String traceId = randomString();
		String method = "APIHandler getTest() " + traceId;
		logger.info("##### " + method + "        TYPE -->> " + rc.request().getParam("type"));
		flightitwayService.getTest(rc.request().getParam("type"), traceId, h -> {
			rc.response().end(Json.encodePrettily(h.result()));
		});
	}

	public void getTestV2(RoutingContext rc) {
		String traceId = randomString();

		flightitwayService.getCustomerWeightGraphData("d.raghvendra@gmail.com", traceId, h -> {
			rc.response().end(Json.encodePrettily(h.result()));
		});
	}

	public void test(RoutingContext rc) {
		String email = "d.raghvendra@gmail.com";
		String traceId = randomString();
		flightitwayService.getCustomerHabitForUpdate(email, traceId, h -> {
			if (h.succeeded()) {
				JsonObject jsonObject = h.result();
				jsonObject.put("code", "0000");
				jsonObject.put("message", "success");
				rc.response().end(jsonObject.encodePrettily());
			} else {
				rc.response().end(new JsonObject().put("code", "1001").encodePrettily());
			}
		});
	}

	public void habitsForUpdate(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler habitsForUpdate() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.debug("##### " + method + " URI  [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "        EMAIL -->> " + email);
		flightitwayService.getCustomerHabitForUpdate(email, traceId, h -> {
			if (h.succeeded()) {
//				JsonObject jsonObject = h.result();
//				jsonObject.put("code", "0000");
//				jsonObject.put("message", "success");
//				rc.response().end(jsonObject.encodePrettily());
				rc.response().end(h.result().toString());
			} else {
				rc.response().end(new JsonObject().put("code", "1001").encodePrettily());
			}
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " HABITS FOR UPDATE [" + timeElapsed + " milliseconds]");
	}

	public void dietPlansV2(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler dietPlansV2() " + traceId;
		String email = rc.user().principal().getString("email");
//		String date= ApiUtils.getCurrentDate(email, traceId);
		String date = rc.request().getParam("date");
		boolean isDateValid = true;
		List<String> list = new ArrayList<>();
		if (null != date && !"".equalsIgnoreCase(date) && date.length() == 8
				&& ApiUtils.isDateValidBetweenTodayAndGivenDate(date))
			isDateValid = true;
		else
			date = ApiUtils.getCurrentDate(email, traceId);
		
		list.add(date);
		
		logger.debug("##### " + method + " URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "        EMAIL -->> " + email);
		flightitwayService.getTodayDietList(email, list.get(0), rc.request().uri(), traceId, res -> {

			if (res.succeeded()) {
				rc.response().end(res.result().encodePrettily());
			} else {
				flightitwayService.getDietPlanByEmailv2(email, list.get(0), traceId, h -> {
					if (h.succeeded()) {
						JsonObject jsonObject = h.result();
						jsonObject.put("code", "0000");
						jsonObject.put("message", "success");
						rc.response().end(jsonObject.encodePrettily());
					} else {
						rc.response().end(new JsonObject().put("code", "1001").encodePrettily());
					}

				});
			}
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " DIET PLANS V2 [" + timeElapsed + " milliseconds]");
	}

	public void dietPlansV3(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler dietPlansV3() " + traceId;
		String email = rc.user().principal().getString("email");

//		boolean isDetox = Boolean.parseBoolean(rc.request().getParam("detox"));
		boolean isDetox = (null != rc.request().getParam("detox") && !"".equals(rc.request().getParam("detox"))
				&& "true".equalsIgnoreCase(rc.request().getParam("detox"))) ? true : false;
		boolean isDateValid = true;
		List<String> list = new ArrayList<>();
		String date = rc.request().getParam("date");
		if (null != date && !"".equalsIgnoreCase(date) && date.length() == 8
				&& ApiUtils.isDateValidBetweenTodayAndGivenDate(date))
			isDateValid = true;
		else
			date = ApiUtils.getCurrentDate(email, traceId);

		list.add(date);
		if (!isDetox) {
			if (isDateValid)
				flightitwayService.getDietPlanByEmailv3(email, date, rc.request().uri(), traceId, h -> {
					if (h.succeeded()) {
						JsonObject jsonObject = h.result();
						try {
							Double totalCal = jsonObject.getInteger("totalCalories").doubleValue();
							Double recommended = jsonObject.getInteger("recomended").doubleValue();
							Double fatPer = jsonObject.getInteger("totalFatPer").doubleValue();
							Double calDistributionPer = jsonObject.getInteger("calDistributionPer").doubleValue();
							logger.info("##### " + method + " TOTAL CALORIES (BEFORE) -->> " + totalCal);
							logger.info("##### " + method + " RECOMMENDED CALORIES (BEFORE) -->> " + recommended);
							logger.info("##### " + method + " FAT % (BEFORE) -->> " + fatPer);
							logger.info(
									"##### " + method + " CALORIES DISTRIBUTION % (BEFORE) -->> " + calDistributionPer);
							logger.debug("##### " + method + " INDEX -->> " + index);
							indexAndCalories += index + "::" + totalCal + "<<-->>";
							jsonObject.put("indexAndCalories",
									indexAndCalories.substring(0, indexAndCalories.length() - 6));
							logger.info("##### " + method + " getMinus7point5Pper(recommended) -->> "
									+ ApiUtils.getMinus7point5Pper(recommended));
							logger.info("##### " + method + " getPlus5Pper(recommended) -->> "
									+ ApiUtils.getPlus5Pper(recommended));
							logger.info("##### dietPlansV3() COMPARISON -->> "
									+ (index < 10 && (totalCal < ApiUtils.getMinus7point5Pper(recommended)
											|| totalCal > ApiUtils.getPlus5Pper(recommended))));
							if (index < 10 && (totalCal < ApiUtils.getMinus7point5Pper(recommended)
									|| totalCal > ApiUtils.getPlus5Pper(recommended))) {
								++index;
								dietPlansV3(rc);
							} else {
								indexAndCalories = "";
								index = 0;
								jsonObject.put("code", "0000");
								jsonObject.put("message", "success");
								rc.response().end(jsonObject.encodePrettily());
							}
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					} else {
						rc.response().end(new JsonObject().put("code", "1001").encodePrettily());
					}
				});
			else
				rc.response().end(new JsonObject().put("code", "0001")
						.put("message", "Requested date [" + date + "] is invalid.").encodePrettily());
		} else {
			flightitwayService.getDietPlanByEmailv3Detox(email, date, rc.request().uri(), traceId, h -> {
				if (h.succeeded()) {
					JsonObject jsonObject = h.result();
					try {
						jsonObject.put("code", "0000");
						jsonObject.put("message", "success");
						rc.response().end(jsonObject.encodePrettily());
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				} else {
					rc.response().end(new JsonObject().put("code", "1001").encodePrettily());
				}
			});
		}

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " DIET PLANS V3 " + (isDetox ? "DETOX" : "") + " [" + timeElapsed
				+ " milliseconds]");
	}

	public void returnDietPlansV3Cache(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler returnDietPlansV3()";
		String email = rc.user().principal().getString("email");
		logger.debug("##### " + method + " URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### returnDietPlansV3(" + index);
		
		String date = rc.request().getParam("date");
		boolean isDateValid = true;
		List<String> list = new ArrayList<>();
		if (null != date && !"".equalsIgnoreCase(date) && date.length() == 8
				&& ApiUtils.isDateValidBetweenTodayAndGivenDate(date))
			isDateValid = true;
		else
			date = ApiUtils.getCurrentDate(email, traceId);
		
		list.add(date);
		
		
//		flightitwayService.getTodayDietList(email, res -> {
//
//			if (res.succeeded()) {
//				logger.info("##### " + method + " REACHES TO CACHE");
//				rc.response().end(res.result().encodePrettily());
//			} else {
//				logger.info("##### " + method + " REACHES TO DB HIT.");
		flightitwayService.getDietPlanByEmailv3Config(email, list.get(0), traceId, h -> {
			if (h.succeeded()) {
				JsonObject jsonObject = h.result();
				try {
					Double totalCal = jsonObject.getInteger("totalCalories").doubleValue();
					Double recommended = jsonObject.getInteger("recomended").doubleValue();
					Double fatPer = jsonObject.getInteger("totalFatPer").doubleValue();
					Double calDistributionPer = jsonObject.getInteger("calDistributionPer").doubleValue();

					logger.info("##### " + method + " TOTAL CALORIES (BEFORE) -->> " + totalCal);
					logger.info("##### " + method + " RECOMMENDED CALORIES (BEFORE) -->> " + recommended);
					logger.info("##### " + method + " FAT % (BEFORE) -->> " + fatPer);
					logger.info("##### " + method + " CALORIES DISTRIBUTION % (BEFORE) -->> " + calDistributionPer);
					logger.info("##### " + method + " INDEX -->> " + index);
					indexAndCalories += index + "::" + totalCal + "<<-->>";
					jsonObject.put("indexAndCalories", indexAndCalories.substring(0, indexAndCalories.length() - 6));
					logger.info("##### " + method + " getMinus7point5Pper(recommended) -->> "
							+ ApiUtils.getMinus7point5Pper(recommended));
					logger.info("##### " + method + " getPlus5Pper(recommended) -->> "
							+ ApiUtils.getPlus5Pper(recommended));
					logger.info("##### dietPlansV3() COMPARISON -->> "
							+ (index < 10 && (totalCal < ApiUtils.getMinus7point5Pper(recommended)
									|| totalCal > ApiUtils.getPlus5Pper(recommended))));
					if (index < 10 && (totalCal < ApiUtils.getMinus7point5Pper(recommended)
							|| totalCal > ApiUtils.getPlus5Pper(recommended))) {
						++index;
						dietPlansV3(rc);
					} else {
						indexAndCalories = "";
						index = 0;
						jsonObject.put("code", "0000");
						jsonObject.put("message", "success");
						rc.response().end(jsonObject.encodePrettily());
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			} else {
				rc.response().end(new JsonObject().put("code", "1001").encodePrettily());
			}

			Instant finish = Instant.now();
			long timeElapsed = Duration.between(start, finish).toMillis();
			logger.info("##### " + method + " RETRUN DIET PLANS V3 [" + timeElapsed + " milliseconds]");
		});
//			}
//		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " DIET PLANS V3 CACHE [" + timeElapsed + " milliseconds]");
	}

	// USED FOR CACHING PURPOSE
	public void dietPlansV3Cache(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler dietPlansV3Cache() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.info("##### " + method + "                EMAIL -->> " + email);
		String date = rc.request().getParam("date");
		boolean isDateValid = true;
		List<String> list = new ArrayList<>();
		if (null != date && !"".equalsIgnoreCase(date) && date.length() == 8
				&& ApiUtils.isDateValidBetweenTodayAndGivenDate(date))
			isDateValid = true;
		else
			date = ApiUtils.getCurrentDate(email, traceId);

		list.add(date);

		boolean isDetox = (null != rc.request().getParam("detox") && !"".equals(rc.request().getParam("detox"))
				&& "true".equalsIgnoreCase(rc.request().getParam("detox"))) ? true : false;
//		boolean isDetox = Boolean.parseBoolean(rc.request().getParam("detox"));
		if (!isDetox) {
			flightitwayService.getTodayDietList(email, list.get(0), rc.request().uri(), traceId, res -> {
				if (res.succeeded()) {
					rc.response().end(res.result().encodePrettily());
				} else {
					flightitwayService.getDietPlanByEmailv3(email, list.get(0), rc.request().uri(), traceId, h -> {
						if (h.succeeded()) {

							JsonObject jsonObject = h.result();
							try {
								Double totalCal = jsonObject.getInteger("totalCalories").doubleValue();
								Double recommended = jsonObject.getInteger("recomended").doubleValue();
								indexAndCalories += "<<-->>" + index + "::" + totalCal;
								jsonObject.put("identifier", indexAndCalories);
								if (index < 10 && (totalCal < ApiUtils.getMinus7point5Pper(recommended)
										|| totalCal > ApiUtils.getPlus5Pper(recommended))) {
									++index;
									dietPlansV3Cache(rc);
								} else {
									if (index >= 10 && (totalCal < ApiUtils.getMinus7point5Pper(recommended)
											|| totalCal > ApiUtils.getPlus5Pper(recommended))) {
										flightitwayService.saveDietPlansV3PersistIfIndexIsGreaterThan10(jsonObject,
												email, rc.request().uri(), traceId, h1 -> {
													if (h1.succeeded())
														logger.info("##### " + method
																+ " JSONOBJECT (INDEX == 10) PERSISTED SUCCESSFULLY");
												});
									}

									indexAndCalories = "";
									index = 0;
									jsonObject.put("code", "0000");
									jsonObject.put("message", "success");
									rc.response().end(jsonObject.encodePrettily());
								}
							} catch (Exception ex) {
								ex.printStackTrace();
							}
						} else {
							rc.response().end(new JsonObject().put("code", "1001").encodePrettily());
						}
					});
				}
			});
		} else {
			logger.info("##### " + method + " DATE -->> " + list.get(0));
			flightitwayService.getDietListDetox(email, list.get(0), rc.request().uri(), traceId, res -> {
				logger.info("##### " + method + " INSIDE DETOX ELSE");
				if (res.succeeded()) {
					logger.info("##### " + method + " INSIDE DETOX SUCCEEDED");
					rc.response().end(res.result().encodePrettily());
				} else {
					flightitwayService.getDietPlanByEmailv3Detox(email, list.get(0), rc.request().uri(), traceId, h -> {
						if (h.succeeded()) {
							JsonObject jsonObject = h.result();
							try {
								jsonObject.put("code", "0000");
								jsonObject.put("message", "success");
								rc.response().end(jsonObject.encodePrettily());
							} catch (Exception ex) {
								ex.printStackTrace();
							}
						} else {
							rc.response().end(new JsonObject().put("code", "1001").encodePrettily());
						}
					});
				}
			});
		}

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " DIET PLANS " + (isDetox ? "DETOX " : "") + "V3 CACHE [" + timeElapsed
				+ " milliseconds]");
	}

	public void dietPlanForOption(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler dietPlanForOption() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.debug("##### " + method + " URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "        EMAIL -->> " + email);
		Integer slot = Integer.parseInt(rc.request().getParam("slot"));
		boolean isDetox = (null != rc.request().getParam("detox") && !"".equals(rc.request().getParam("detox"))
				&& "true".equalsIgnoreCase(rc.request().getParam("detox"))) ? true : false;
		if (!isDetox)
			flightitwayService.getDietPlanByEmailForOption(email, slot, traceId, h -> {
				if (h.succeeded()) {
					JsonObject jsonObject = h.result();
					jsonObject.put("code", "0000");
					jsonObject.put("message", "success");
					rc.response().end(jsonObject.encodePrettily());
				} else {
					rc.response().end(new JsonObject().put("code", "1001").encodePrettily());
				}
			});
		else
			flightitwayService.getDietPlanByEmailForOptionDetox(email, slot, traceId, h -> {
				if (h.succeeded()) {
					JsonObject jsonObject = h.result();
					jsonObject.put("code", "0000");
					jsonObject.put("message", "success");
					rc.response().end(jsonObject.encodePrettily());
				} else {
					rc.response().end(new JsonObject().put("code", "1001").encodePrettily());
				}
			});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " DIET PLAN FOR OPTION [" + timeElapsed + " milliseconds]");
	}

	public void addCustDietPref(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler addCustDietPref() " + traceId;
		String email = rc.user().principal().getString("email");
		Set<FoodDetail> foodDetails = new HashSet<FoodDetail>();
		JsonArray foodCodeList = rc.getBodyAsJson().getJsonArray("foodCodeList");
		Integer slot = rc.getBodyAsJson().getInteger("slot", 0);
		String date = rc.getBodyAsJson().getString("date", ApiUtils.getCurrentDate(email, traceId));
		boolean isDateValid = true;
		List<String> list = new ArrayList<>();
		if (null != date && !"".equalsIgnoreCase(date) && date.length() == 8
				&& ApiUtils.isDateValidBetweenTodayAndGivenDate(date))
			isDateValid = true;
		else
			date = ApiUtils.getCurrentDate(email, traceId);
		
		list.add(date);

		logger.info("##### " + method + "        EMAIL -->> " + email);
		logger.info("##### " + method + "     RESPONSE -->> " + rc.getBodyAsString());
		CustDietPreference preference = new CustDietPreference();
		preference.setFoodCodeList(foodCodeList);
		preference.setEmail(email);
		for (int i = 0; i < foodCodeList.size(); i++) {
			FoodDetail detail = new FoodDetail();
			detail.setCode(foodCodeList.getString(i));
			detail.setPortion(1d);
			foodDetails.add(detail);
		}

		if (isDateValid)
			flightitwayService.updateCustDietPref(preference, rc.request().uri(), traceId, h -> {
				if (h.succeeded()) {
					flightitwayService.updateCacheData(email, slot, foodDetails, list.get(0), rc.request().uri(), traceId,
							h1 -> {
								if (h1.succeeded()) {
									rc.response().end(h1.result().toString());
								} else {
									rc.response()
											.end(new JsonObject().put("code", h1.cause().toString()).encodePrettily());
								}
							});
				} else {
					rc.response().end(new JsonObject().put("code", "1001").encodePrettily());
				}
			});
		else
			rc.response().end(new JsonObject().put("code", "0001")
					.put("message", "Requested date [" + date + "] is invalid.").encodePrettily());

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " ADD CUST DIET PREF [" + timeElapsed + " milliseconds]");
	}

	public void addCustDietPrefV2(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String email = rc.user().principal().getString("email");
		String method = "APIHandler addCustDietPrefV2() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + " URI -->> " + rc.request().uri());
		Integer slot = rc.getBodyAsJson().getInteger("slot", 0);
		logger.info("##### " + method + "         SLOT -->> " + slot);
		logger.info("##### " + method + "     RESPONSE -->> " + rc.getBodyAsString());
		Set<FoodDetail> foodDetails = new HashSet<FoodDetail>();
		JsonArray foodCodeList = null;
		foodCodeList = rc.getBodyAsJson().getJsonArray("foodCodeList");
		for (int i = 0; i < foodCodeList.size(); i++) {
			JsonObject jsonObject = foodCodeList.getJsonObject(i);
			FoodDetail foodDetail = new FoodDetail();
			foodDetail.setCode(jsonObject.getString("code"));
			foodDetail.setPortion(jsonObject.getDouble("portion"));
			foodDetails.add(foodDetail);
		}
		String date = rc.getBodyAsJson().getString("date", "");
		CustDietPreference preference = new CustDietPreference();
		preference.setFoods(foodDetails);
		preference.setEmail(email);
		preference.setSlot(slot);
		boolean isDetox = rc.getBodyAsJson().getBoolean("detox", false);
		logger.info("##### " + method + "         ISDETOX -->> " + isDetox);
		if (!isDetox)
			flightitwayService.updateCustDietPrefV2(preference, rc.request().uri(), traceId, h -> {
				if (h.succeeded()) {
					flightitwayService.updateCacheData(email, slot, foodDetails, date, rc.request().uri(), traceId,
							h1 -> {
								if (h1.succeeded()) {
									rc.response().end(h1.result().toString());
								} else {
									rc.response()
											.end(new JsonObject().put("code", h1.cause().toString()).encodePrettily());
								}
							});
				} else {
					rc.response()
							.end(new JsonObject().put("code", "1001")
									.put("message",
											"[" + ApiUtils.getSpecificDate(0, date) + "] dietplan is unavailable.")
									.encodePrettily());
				}
			});
		else
			flightitwayService.updateCustDietPrefV2Detox(preference, rc.request().uri(), traceId, h -> {
				if (h.succeeded()) {
					flightitwayService.updateCacheDataDetox(email, slot, foodDetails, date, rc.request().uri(), traceId,
							h1 -> {
								if (h1.succeeded()) {
									rc.response().end(h1.result().toString());
								} else {
									rc.response()
											.end(new JsonObject().put("code", h1.cause().toString()).encodePrettily());
								}
							});
				} else {
					rc.response()
							.end(new JsonObject().put("code", "1001")
									.put("message",
											"[" + ApiUtils.getSpecificDate(0, date) + "] dietplan is unavailable.")
									.encodePrettily());
				}
			});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " ADD CUST DIET PREF V2 [" + timeElapsed + " milliseconds]");
	}

	public void getCustDietPref(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler getCustDietPref() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.debug("##### " + method + " URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "      EMAIL -->> " + email);
		flightitwayService.getCustDietPref(email, traceId, h -> {
			if (h.succeeded()) {
				JsonArray jsonObject = h.result();
				JsonObject response = new JsonObject();
				response.put("foodList", jsonObject);
				response.put("code", "0000");
				response.put("message", "success");
				rc.response().end(response.encodePrettily());
			} else {
				rc.response().end(new JsonObject().put("code", "1001").encodePrettily());
			}
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " GET CUST DIET PREF [" + timeElapsed + " milliseconds]");
	}

	public void getCustomerDiets(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler getCustomerDiets() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.debug("##### " + method + " URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "      EMAIL -->> " + email);

		flightitwayService.getCustomerDietList(email, traceId, h -> {
			if (h.succeeded()) {
				logger.debug("##### " + method + " H.RESULT() -->> " + h.result().size());
				JsonObject jsonObject = h.result();
				jsonObject.put("code", "0000");
				jsonObject.put("message", "success");
				rc.response().end(jsonObject.encodePrettily());
			} else {
				rc.response().end(new JsonObject().put("code", "1001").encodePrettily());
			}
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " GET CUSTOMER DIETS [" + timeElapsed + " milliseconds]");
	}

	public void createCustomerHabit(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler createCustomerHabit() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.debug("##### " + method + " URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "      EMAIL -->> " + email);
		try {
			CreatCustomerHabitPlanRequest request = Json.decodeValue(rc.getBodyAsString(),
					CreatCustomerHabitPlanRequest.class);
			flightitwayService.createCustomerHabit(email, request, traceId, h -> {
				if (h.succeeded()) {
					JsonObject jsonObject = new JsonObject();
					jsonObject.put("code", "0000");
					jsonObject.put("message", "success");
					rc.response().end(jsonObject.encodePrettily());
				} else {
					rc.response().end(new JsonObject().put("code", "1001").encodePrettily());
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " CREATE CUSTOMER HABIT [" + timeElapsed + " milliseconds]");
	}

	public void updateCustomerHabit(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler updateCustomerHabit() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.debug("##### " + method + " URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "      EMAIL -->> " + email);
		try {
			CreatCustomerHabitPlanRequest request = Json.decodeValue(rc.getBodyAsString(),
					CreatCustomerHabitPlanRequest.class);
			flightitwayService.updateCustomerHabit(email, traceId, request, h -> {
				if (h.succeeded()) {
					JsonObject jsonObject = new JsonObject();
					jsonObject.put("code", "0000");
					jsonObject.put("message", "success");
					rc.response().end(jsonObject.encodePrettily());
				} else {
					rc.response().end(new JsonObject().put("code", "1001").encodePrettily());
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " UPDATE CUSTOMER HABIT [" + timeElapsed + " milliseconds]");
	}

	public void deleteCustomerHabit(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler updateCustomerHabit() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.debug("##### " + method + " URI [" + email + "] -->> " + rc.request().uri());
		String code = rc.getBodyAsJson().getString("code");
		logger.info("##### " + method + "      EMAIL -->> " + email);
		logger.info("##### " + method + "       CODE -->> " + code);
		try {
			flightitwayService.deleteCustomerHabit(email, code, traceId, h -> {
				if (h.succeeded()) {
					JsonObject jsonObject = new JsonObject();
					jsonObject.put("code", "0000");
					jsonObject.put("message", "success");
					rc.response().end(jsonObject.encodePrettily());
				} else {
					rc.response().end(new JsonObject().put("code", "1001").encodePrettily());
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " DELETE CUSTOMER HABIT [" + timeElapsed + " milliseconds]");
	}

	public void getCouponListFromMaster(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler getCouponListFromMaster() " + traceId;
		logger.debug("##### " + method + " URI -->> " + rc.request().uri());
		try {
			flightitwayService.getCouponListFromMaster(traceId, h -> {
				if (h.succeeded()) {
					rc.response().end(h.result().encodePrettily());
				} else {
					rc.response().end(new JsonObject().put("code", "1001").encodePrettily());
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " GET COUPON LIST FROM MASTER [" + timeElapsed + " milliseconds]");
	}

	public void getCustomerHabit(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler getCustomerHabit() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.debug("##### " + method + " URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "      EMAIL -->> " + email);
		flightitwayService.getCustomerHabitList(email, traceId, h -> {
			logger.debug("##### " + method + " H -->> " + h.succeeded());
			if (h.succeeded()) {
				logger.debug("##### " + method + " H.RESULT() -->> " + h.result().size());
				JsonObject jsonObject = new JsonObject();
				List<JsonObject> list = h.result();
				jsonObject.put("code", "0000");
				jsonObject.put("message", "success");
				jsonObject.put("habits", list);
				rc.response().end(jsonObject.encodePrettily());
			} else {
				rc.response().end(
						new JsonObject().put("code", "0404").put("message", h.cause().getMessage()).encodePrettily());
			}
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " GET CUSTOMER HABIT [" + timeElapsed + " milliseconds]");
	}

	public void createHabit(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler createHabit() " + traceId;
		logger.debug("##### " + method + " URI -->> " + rc.request().uri());
		JsonObject request = rc.getBodyAsJson();
		logger.info("##### " + method + "      REQUEST -->> " + request);
		flightitwayService.createHabitMaster(request, traceId, h -> {
			if (h.succeeded()) {
				logger.debug("##### " + method + "      H.RESULT() -->>  -->> " + h.result().size());
				JsonObject jsonObject = h.result();
				jsonObject.put("code", "0000");
				jsonObject.put("message", "success");
				rc.response().end(jsonObject.encodePrettily());
			} else {
				rc.response().end(new JsonObject().put("code", "1001").encodePrettily());
			}
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " CRETAE HABIT [" + timeElapsed + " milliseconds]");
	}

	public void getHabit(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler createHabit() " + traceId;
		logger.debug("##### " + method + " URI -->> " + rc.request().uri());
		flightitwayService.getHabitMaster(traceId, h -> {
			if (h.succeeded()) {
				rc.response().end(h.result().encodePrettily());
			} else {
				rc.response().end(new JsonObject().put("code", "1001").encodePrettily());
			}
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " GET HABIT [" + timeElapsed + " milliseconds]");
	}

	public void createCustomerDiet(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler createHabit()";
		String email = rc.user().principal().getString("email");
		logger.debug("##### " + method + " URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "      EMAIL -->> " + email);
		try {
			CreatCustomerDietPlanRequest request = Json.decodeValue(rc.getBodyAsString(),
					CreatCustomerDietPlanRequest.class);
			flightitwayService.createCustomerDietPlan(email, request, traceId, h -> {
				if (h.succeeded()) {
					JsonObject jsonObject = new JsonObject();
					jsonObject.put("code", "0000");
					jsonObject.put("message", "success");
					rc.response().end(jsonObject.encodePrettily());
				} else {
					rc.response().end(new JsonObject().put("code", "1001").encodePrettily());
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " CREATE CUSTOMER DIET [" + timeElapsed + " milliseconds]");
	}

	public void getDefaultProfile(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler getDefaultProfile() " + traceId;
		logger.debug("##### " + method + " URI -->> " + rc.request().uri());
		flightitwayService.getDefaultDeatil(traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " GET DEFAULT PROFILE [" + timeElapsed + " milliseconds]");
	}

	@SuppressWarnings("unused")
	public void validateUser(RoutingContext context, BiConsumer<RoutingContext, JsonObject> biHandler) {
		User user = context.user();
		biHandler.accept(context, context.getBodyAsJson());
	}

	public String extract_email(JsonObject json) {
		try {
			String id_token = String.valueOf(json.getString("id_token"));
			String[] jwtParts = id_token.split("\\.");
			String name;
			name = new String(Base64.getDecoder().decode((jwtParts[1])));
			return new JsonObject(name).getString("email");
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public void updateCustomerWeight(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler updateCustomerWeight() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.debug("##### " + method + "        URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "      EMAIL -->> " + email);
		Double weight = rc.getBodyAsJson().getDouble("weight");
		logger.info("##### " + method + "     WEIGHT -->> " + weight);

		try {
			flightitwayService.updateCustomerWeight(email, weight, traceId, h -> {
				if (h.succeeded()) {
					JsonObject jsonObject = new JsonObject();
					jsonObject.put("code", "0000");
					jsonObject.put("message", "success");
					rc.response().end(jsonObject.encodePrettily());
				} else {
					rc.response().end(new JsonObject().put("code", "1001").encodePrettily());
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " UPDATE CUSTOMER WEIGHT [" + timeElapsed + " milliseconds]");
	}

	public void getCustomerCurrentWeight(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler getCustomerCurrentWeight() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.debug("##### " + method + " URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "      EMAIL -->> " + email);
		try {
			flightitwayService.getLatestWeight(email, traceId, h -> {
				if (h.succeeded()) {
					rc.response().end(h.result().encodePrettily());
				} else {
					rc.response().end(new JsonObject().put("code", "1001").encodePrettily());
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " GET CUSTOMER CURRENT WEIGHT [" + timeElapsed + " milliseconds]");
	}

	public void getCustomerWeightGrphData(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler getCustomerWeightGrphData() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.debug("##### " + method + " URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "                EMAIL -->> " + email);
		try {
			flightitwayService.getCustomerWeightGraphData(email, traceId, h -> {
				if (h.succeeded()) {
					rc.response().end(h.result().encodePrettily());
				} else {
					rc.response().end(new JsonObject().put("code", "1001").encodePrettily());
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " GET CUSTOMER WEIGHT GRAPH DATA [" + timeElapsed + " milliseconds]");
	}

	public void dietPlansBySlot(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler dietPlansBySlot() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.debug("##### " + method + "        URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "      EMAIL -->> " + email);
		String slotId = rc.request().getParam("slotId");
		logger.info("##### " + method + "       SLOT -->> " + slotId);
		try {
			flightitwayService.getDietPlanBySlot(email, Integer.parseInt(slotId), traceId, h -> {
				if (h.succeeded()) {
					rc.response().end(h.result().encodePrettily());
				} else {
					rc.response().end(new JsonObject().put("code", "1001").encodePrettily());
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " DIET PLANS BY SLOT [" + timeElapsed + " milliseconds]");
	}

	// For Customer details on edit
	public void getCustDietDetailsOnEdit(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler getCustDietDetailsOnEdit() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.debug("##### " + method + "        URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "      EMAIL -->> " + email);
		flightitwayService.getCustDietDetailsOnEdit(email, traceId, h -> {
			rc.response().end(h.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " GET CUST DIET DETAILS ON EDIT [" + timeElapsed + " milliseconds]");
	}

	public void getDefaultDetail(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler getDefaultDetail() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.debug("##### " + method + "        URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "      EMAIL -->> " + email);
		flightitwayService.getDefaultDetail(email, traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " GET DEFAULT DETAIL [" + timeElapsed + " milliseconds]");
	}

	public void getDietPlanTimings(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler getDietPlanTimings() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.debug("##### " + method + "        URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "      EMAIL -->> " + email);
		flightitwayService.getCustDietPlanTimingsProfile(email, traceId, h -> {
			rc.response().end(h.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " GET DIET PLAN TIMINGS [" + timeElapsed + " milliseconds]");
	}

	public void sendEmail(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler sendEmail() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.debug("##### " + method + "                URI [" + email + "] -->> " + rc.request().uri());
		JsonObject request = rc.getBodyAsJson();
		logger.info("##### " + method + "            REQUEST -->> " + request);
		logger.info("##### " + method + "              EMAIL -->> " + email);
		flightitwayService.sendEmail(email, request, rc.request().uri(), traceId, h -> {
			if (h.succeeded()) {
				logger.info("##### " + method + " H.RESULT() -->> " + h.result().size());
				JsonObject jsonObject = h.result();
				rc.response().end(jsonObject.encodePrettily());
			} else {
				rc.response().end(new JsonObject().put("code", "0000").put("message", h.result()).encodePrettily());
			}
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " SEND EMAIL [" + timeElapsed + " milliseconds]");
	}

	public void getDashboard(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler getDashboard " + traceId;
		logger.debug("##### " + method + " URI -->> " + rc.request().uri());
		flightitwayService.getDashboard(traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " GET DASHBOARD [" + timeElapsed + " milliseconds]");
	}

	public void getDietPlanCachedata(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler getDietPlanCachedata() " + traceId;
		logger.debug("##### " + method + " URI -->> " + rc.request().uri());
		flightitwayService.getDietPlanCachedata(traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " GET CUSTOMER HABIT [" + timeElapsed + " milliseconds]");
	}

	public void addTnC(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler addTnC() " + traceId;
		logger.debug("##### " + method + " URI -->> " + rc.request().uri());
		JsonObject request = rc.getBodyAsJson();
		flightitwayService.addTnC(request, traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " GET CUSTOMER HABIT [" + timeElapsed + " milliseconds]");
	}

	public void profileRemove(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler profileRemove() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.debug("##### " + method + "                URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "              EMAIL -->> " + email);
		flightitwayService.profileRemove(email, traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " PROFILE [" + email + "] REMOVED [" + timeElapsed + " milliseconds]");
	}

	public void getOnePlan(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler getOnePlan() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.info("##### " + method + " URI [" + email + "] -->> " + rc.request().uri());
		flightitwayService.getOnePlan(email, traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info(
				"##### " + method + " GETONE PLAN [" + email + "] - SUBSCRIPTION [" + timeElapsed + " milliseconds]");
	}

	public void subscribePlanByCoupon(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler subscribePlanByCoupon() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.debug("##### " + method + " URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "              EMAIL -->> " + email);
		String couponCode = rc.getBodyAsJson().getString("couponCode");
		logger.info("##### " + method + "        COUPON CODE -->> " + couponCode);
		flightitwayService.subscribePlanByCoupon(email, couponCode, traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " SUBSCRIBE PLAN BY COUPON [" + email + "] UPDATE DETAILS [" + timeElapsed
				+ " milliseconds]");
	}

	public void fetchFood(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler fetchFood() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.debug("##### " + method + "    URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "  EMAIL -->> " + email);
		String foodId = rc.getBodyAsJson().getString("foodId");
		logger.info("##### " + method + "  FOOD ID -->> " + email);
		flightitwayService.fetchFood(email, foodId, rc.request().uri(), traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " FETCH FOOD [" + email + "] - [" + timeElapsed + " milliseconds]");
	}

	public void saveCustCaloriesBurnt(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler saveCustCaloriesBurnt() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.debug("##### " + method + "      URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "    EMAIL -->> " + email);
		Double calBurnt = rc.getBodyAsJson().getDouble("caloriesBurnt");
		logger.info("##### " + method + "  FOOD ID -->> " + email);
		flightitwayService.saveCustCaloriesBurnt(email, calBurnt, traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " SAVE CUSTOMER CALORIES BURNT [" + email + "] - [" + timeElapsed
				+ " milliseconds]");
	}

	public void waterTips(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler waterTips() " + traceId;
		logger.debug("##### " + method + " URI -->> " + rc.request().uri());
		flightitwayService.waterTips(traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " WATER TIPS - [" + timeElapsed + " milliseconds]");
	}

	public void saveWaterDrank(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler saveWaterDrank() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.debug("##### " + method + "             URI [" + email + "] -->> " + rc.request().uri());
		Integer waterQuantity = rc.getBodyAsJson().getInteger("quantity");
		String dateTime = rc.getBodyAsJson().getString("datetime");
		logger.info("##### " + method + "           EMAIL -->> " + email);
		logger.info("##### " + method + "  WATER QUANTITY -->> " + waterQuantity);
		logger.info("##### " + method + "       DATE-TIME -->> " + dateTime);
		flightitwayService.saveWaterDrank(email, waterQuantity, dateTime, traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " SAVE WATER DRANK [" + email + "] - [" + timeElapsed + " milliseconds]");
	}

	public void waterRecommendation(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler waterRecommendation() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.debug("##### " + method + "    URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "  EMAIL -->> " + email);
		flightitwayService.waterRecommendation(email, traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " WATER RECOMMENDATION [" + email + "] - [" + timeElapsed + " milliseconds]");
	}

	public void saveWaterReminder(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler saveWaterReminder() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.debug("##### " + method + "    URI  [" + email + "]-->> " + rc.request().uri());
		logger.info("##### " + method + "  EMAIL -->> " + email);
		String waterReminderStatus = rc.getBodyAsJson().getString("waterReminderStatusToBe");
		logger.info("##### " + method + "  WATER REMINDER STATUS TO BE -->> " + waterReminderStatus);
		flightitwayService.custWaterReminder(email, waterReminderStatus, traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info(
				"##### " + method + " SAVE/UPDATE WATER REMINDER [" + email + "] - [" + timeElapsed + " milliseconds]");
	}

	public void fetchWaterReminder(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler fetchWaterReminder() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.debug("##### " + method + "    URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "  EMAIL -->> " + email);
		flightitwayService.fetchWaterReminder(email, traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " FETCH WATER REMINDER [" + email + "] - [" + timeElapsed + " milliseconds]");
	}

	public void getSpecificDetailsByCommunity(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler getSpecificDetailsByCommunity() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.debug("##### " + method + "        URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "      EMAIL -->> " + email);
		flightitwayService.getSpecificDetailsByCommunity(email, traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " GET SPECIFIC DETAILS BY COMMUNITY [" + timeElapsed + " milliseconds]");
	}

	public void fetchCaloriesHistory(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler fetchCaloriesHistory() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.debug("##### " + method + "    URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "  EMAIL -->> " + email);
		// Integer noOfDays = rc.getBodyAsJson().getInteger("days");
		Integer noOfDays = 30;
		logger.info("##### " + method + "  NO OF DAYS FOR CACHE DATA TO BE FETCHED -->> " + noOfDays);
		flightitwayService.fetchCaloriesHistory(email, noOfDays, traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " FETCH CALORIES HISTORY [" + email + "] - [" + timeElapsed + " milliseconds]");
	}

	public void fetchTargetCalories(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler fetchTargetCalories() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.debug("##### " + method + "        URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "      EMAIL -->> " + email);
		Integer noOfDays = 90;
		flightitwayService.fetchTargetCalories(email, noOfDays, traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " FETCH TARGET CALORIES [" + timeElapsed + " milliseconds]");
	}

	public void dietPlanRefreshOption(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler dietPlanRefreshOption() " + traceId;
		String email = rc.user().principal().getString("email");
		String date = rc.getBodyAsJson().getString("date", ApiUtils.getCurrentDate(email, traceId));
		boolean isDateValid = true;
		List<String> list = new ArrayList<>();
		if (null != date && !"".equalsIgnoreCase(date) && date.length() == 8
				&& ApiUtils.isDateValidBetweenTodayAndGivenDate(date))
			isDateValid = true;
		else
			date = ApiUtils.getCurrentDate(email, traceId);
		
		list.add(date);

		boolean isDetox = rc.getBodyAsJson().getBoolean("detox", false);
//		boolean isDetox = (null != rc.request().getParam("detox") && !"".equals(rc.request().getParam("detox"))
//				&& "true".equalsIgnoreCase(rc.request().getParam("detox"))) ? true : false;
		List<FoodDetail> foodDetails = new ArrayList<FoodDetail>();
		JsonArray foodCodeList = null;
		foodCodeList = rc.getBodyAsJson().getJsonArray("foodCodeList");
		for (int i = 0; i < foodCodeList.size(); i++) {
			JsonObject jsonObject = foodCodeList.getJsonObject(i);
			FoodDetail foodDetail = new FoodDetail();
			foodDetail.setCode(jsonObject.getString("code"));
			foodDetail.setPortion(1.0);
			foodDetails.add(foodDetail);
		}
		Integer slot = rc.getBodyAsJson().getInteger("slot", 0);
		String category = rc.getBodyAsJson().getString("category");

		if (!isDetox)
			if (isDateValid)
				flightitwayService.dietPlanRefreshOption(email, slot, category, list.get(0), rc.request().uri(), traceId,
						foodDetails.get(0), h -> {
							if (h.succeeded()) {
								rc.response().end(h.result().encodePrettily());
							} else {
								rc.response().end(new JsonObject().put("code", "1001").encodePrettily());
							}
						});
			else
				rc.response().end(new JsonObject().put("code", "0001")
						.put("message", "Requested date [" + date + "] is invalid.").encodePrettily());
		else
			flightitwayService.dietPlanRefreshOptionDetox(email, slot, category, rc.request().uri(), traceId,
					foodDetails.get(0), h -> {
						if (h.succeeded()) {
							rc.response().end(h.result().encodePrettily());
						} else {
							rc.response().end(new JsonObject().put("code", "1001").encodePrettily());
						}
					});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " GET DIET PLAN REFRESH OPTIONS [" + timeElapsed + " milliseconds]");
	}

	public void getCustAnalytics(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler getCustAnalytics() " + traceId;
		String email = rc.request().getParam("email");
		logger.debug("##### " + method + "        URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "      EMAIL -->> " + email);
		flightitwayService.getCustAnalytics(traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " GET CUSTOMER ALAYTICS [" + timeElapsed + " milliseconds]");
	}

	public void refreshFoods(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler refreshFoods() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.debug("##### " + method + "        URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "        EMAIL               -->> " + email);
		flightitwayService.refreshFoods(email, rc.request().uri(), traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " REFRESH FOODS [" + timeElapsed + " milliseconds]");
	}

	public void getCustPaymentAnalytics(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler getCustPaymentAnalytics() " + traceId;
		String email = rc.request().getParam("email");
		Integer noOfDays = 0;
		logger.debug("##### " + method + "        URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "        EMAIL               -->> " + email);
		flightitwayService.getCustPaymentAnalytics(noOfDays, traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " GET CUSTOMER PAYMENT ALANYTICS [" + timeElapsed + " milliseconds]");
	}

	public void getDietPlanCacheAnalytics(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler getDietPlanCacheAnalytics() " + traceId;
		String email = rc.request().getParam("email");
		logger.debug("##### " + method + "        URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "        EMAIL               -->> " + email);
		flightitwayService.getDietPlanCacheAnalytics(traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " GET CUSTOMER DIET PLAN CACHE ANALYTICS [" + timeElapsed + " milliseconds]");
	}

	public void downloadDietPlanCacheAnalytics(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler downloadDietPlanCacheAnalytics() " + traceId;
		String email = rc.request().getParam("email");
		logger.debug("##### " + method + "        URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "        EMAIL               -->> " + email);
		flightitwayService.getDietPlanCacheAnalytics(traceId, resultHandler -> {

			rc.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/plain")
					.putHeader("Content-Disposition", "attachment; filename=\"dietcacheanalytics.csv\"")
					.putHeader(HttpHeaders.TRANSFER_ENCODING, "chunked")
					.sendFile("\\home\\ubuntu\\fightitaway\\dietcacheanalytics.csv")
					.end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " DOWNLOAD DIET PLAN CACHE ANALYTICS [" + timeElapsed + " milliseconds]");
	}

	public void createEmptyProfile(RoutingContext rc) {
		String traceId = randomString();
		String method = "APIHandler createEmptyProfile() " + traceId;
		Instant start = Instant.now();
		//logger.info("##### " + method + " BODY AS JSON -->> " + rc.getBodyAsJson().encodePrettily());
		CreateProfileRequest request = new CreateProfileRequest();
		request.setName("Raghvendra");
		request.setFamily_name("Dwivedi");
		request.setGiven_name("Raghav");
		request.setEmail(null);
		request.setLoginType("GMAIL");
		flightitwayService.createEmptyProfile(request, traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " CREATE EMPTY  PROFILE [" + timeElapsed + " milliseconds]");
	}

	public void dietPlanSaveTime(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler dietPlanSaveTime() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.info("##### " + method + "      URI [" + email + "] -->> " + rc.request().uri());
		Integer slot = rc.getBodyAsJson().getInteger("slot", 0);
		logger.info("##### " + method + "  SLOT -->> " + slot);		
		
		JsonObject request = rc.getBodyAsJson();
		flightitwayService.dietPlanSaveTime(email, request, rc.request().uri(), traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " DIET PLAN SAVE TIME [" + email + "] - [" + timeElapsed
				+ " milliseconds]");
	}

	public void saveOrUpdateCustomerDietPlanTimings(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler saveOrUpdateCustomerDietPlanTimings() " + traceId;
		String email = rc.user().principal().getString("email");
		boolean isDetox = (null != rc.request().getParam("detox") && !"".equals(rc.request().getParam("detox"))
				&& "true".equalsIgnoreCase(rc.request().getParam("detox"))) ? true : false;
		logger.info("##### " + method + "      URI [" + email + "] -->> " + rc.request().uri());

		JsonObject request = rc.getBodyAsJson();
		logger.info("##### " + method + " REQUEST -->> " + request);
		if (!isDetox)
			flightitwayService.saveOrUpdateCustomerDietPlanTimings(email, request, rc.request().uri(), traceId,
					resultHandler -> {
						rc.response().end(resultHandler.result().encodePrettily());
					});
		else
			flightitwayService.saveOrUpdateCustomerDietPlanTimingsDetox(email, request, rc.request().uri(), traceId,
					resultHandler -> {
						rc.response().end(resultHandler.result().encodePrettily());
					});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " SAVE/UPDATE DIET PLAN TIMINGS " + (isDetox ? "DETOX" : "") + "[" + email + "] - ["
				+ timeElapsed + " milliseconds]");
	}

	public void dietPlansV3Detox(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler dietPlansV3Detox() " + traceId;
		String email = rc.user().principal().getString("email");
		String date = rc.request().getParam("date");
		logger.debug("##### " + method + " URI [" + email + "] -->> " + rc.request().uri());
		flightitwayService.getDietPlanByEmailv3(email, date, rc.request().uri(), traceId, h -> {
			if (h.succeeded()) {
				JsonObject jsonObject = h.result();
				try {
					Double totalCal = jsonObject.getInteger("totalCalories").doubleValue();
					Double recommended = jsonObject.getInteger("recomended").doubleValue();
					Double fatPer = jsonObject.getInteger("totalFatPer").doubleValue();
					Double calDistributionPer = jsonObject.getInteger("calDistributionPer").doubleValue();

					logger.debug("##### " + method + " TOTAL CALORIES (BEFORE) -->> " + totalCal);
					logger.debug("##### " + method + " RECOMMENDED CALORIES (BEFORE) -->> " + recommended);
					logger.debug("##### " + method + " FAT % (BEFORE) -->> " + fatPer);
					logger.debug("##### " + method + " CALORIES DISTRIBUTION % (BEFORE) -->> " + calDistributionPer);
					logger.debug("##### " + method + " INDEX -->> " + index);
					indexAndCalories += index + "::" + totalCal + "<<-->>";
					jsonObject.put("indexAndCalories", indexAndCalories.substring(0, indexAndCalories.length() - 6));
					logger.debug("##### " + method + " getMinus7point5Pper(recommended) -->> "
							+ ApiUtils.getMinus7point5Pper(recommended));
					logger.debug("##### " + method + " getPlus5Pper(recommended) -->> "
							+ ApiUtils.getPlus5Pper(recommended));
					logger.debug("##### dietPlansV3() COMPARISON -->> "
							+ (index < 10 && (totalCal < ApiUtils.getMinus7point5Pper(recommended)
									|| totalCal > ApiUtils.getPlus5Pper(recommended))));
					if (index < 10 && (totalCal < ApiUtils.getMinus7point5Pper(recommended)
							|| totalCal > ApiUtils.getPlus5Pper(recommended))) {
						++index;
						dietPlansV3(rc);
					} else {
						indexAndCalories = "";
						index = 0;
						jsonObject.put("code", "0000");
						jsonObject.put("message", "success");
						rc.response().end(jsonObject.encodePrettily());
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			} else {
				rc.response().end(new JsonObject().put("code", "1001").encodePrettily());
			}
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " DIET PLANS V3 - DETOX [" + timeElapsed + " milliseconds]");
	}

	// TO BE USED FOR CACHING PURPOSE
	public void dietPlansV3DetoxCache(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler dietPlansV3DetoxCache() " + traceId;
		String email = rc.user().principal().getString("email");
		String date = rc.request().getParam("date");
		boolean isDateValid = true;
		List<String> list = new ArrayList<>();
		if (null != date && !"".equalsIgnoreCase(date) && date.length() == 8
				&& ApiUtils.isDateValidBetweenTodayAndGivenDate(date))
			isDateValid = true;
		else
			date = ApiUtils.getCurrentDate(email, traceId);

		list.add(date);

		logger.debug("##### " + method + " URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "                EMAIL -->> " + email);
		flightitwayService.getTodayDietList(email, list.get(0), rc.request().uri(), traceId, res -> {
			if (res.succeeded()) {
				logger.info("##### " + method + " REACHES TO CACHE");
				rc.response().end(res.result().encodePrettily());
			} else {
				logger.info("##### " + method + " REACHES TO DB HIT.");
				flightitwayService.getDietPlanByEmailv3Detox(email, list.get(0), rc.request().uri(), traceId, h -> {
					if (h.succeeded()) {

						JsonObject jsonObject = h.result();
						try {
							Double totalCal = jsonObject.getInteger("totalCalories").doubleValue();
							Double recommended = jsonObject.getInteger("recomended").doubleValue();
							Double fatPer = jsonObject.getInteger("totalFatPer").doubleValue();
							Double calDistributionPer = jsonObject.getInteger("calDistributionPer").doubleValue();

							logger.debug("##### " + method + " TOTAL CALORIES (BEFORE) -->> " + totalCal);
							logger.debug("##### " + method + " RECOMMENDED CALORIES (BEFORE) -->> " + recommended);
							logger.debug("##### " + method + " FAT % (BEFORE) -->> " + fatPer);
							logger.debug(
									"##### " + method + " CALORIES DISTRIBUTION % (BEFORE) -->> " + calDistributionPer);
							logger.debug("##### " + method + " INDEX -->> " + index);
							indexAndCalories += "<<-->>" + index + "::" + totalCal;
							jsonObject.put("identifier", indexAndCalories);
							logger.debug("##### " + method + " getMinus7point5Pper(recommended) -->> "
									+ ApiUtils.getMinus7point5Pper(recommended));
							logger.debug("##### " + method + " getPlus5Pper(recommended) -->> "
									+ ApiUtils.getPlus5Pper(recommended));
							logger.debug("##### " + method + " COMPARISON -->> "
									+ (index < 10 && (totalCal < ApiUtils.getMinus7point5Pper(recommended)
											|| totalCal > ApiUtils.getPlus5Pper(recommended))));
							if (index < 10 && (totalCal < ApiUtils.getMinus7point5Pper(recommended)
									|| totalCal > ApiUtils.getPlus5Pper(recommended))) {
								++index;
								dietPlansV3Cache(rc);
							} else {
								if (index >= 10 && (totalCal < ApiUtils.getMinus7point5Pper(recommended)
										|| totalCal > ApiUtils.getPlus5Pper(recommended))) {
									logger.info("##### " + method + " COMPARISON (INDEX == 10) -->> "
											+ (index < 10 && (totalCal < ApiUtils.getMinus7point5Pper(recommended)
													|| totalCal > ApiUtils.getPlus5Pper(recommended))));
									flightitwayService.saveDietPlansV3PersistIfIndexIsGreaterThan10(jsonObject, email,
											rc.request().uri(), traceId, h1 -> {
												if (h1.succeeded())
													logger.info("##### " + method
															+ " JSONOBJECT (INDEX == 10) PERSISTED SUCCESSFULLY");
											});
								}

								indexAndCalories = "";
								index = 0;
								jsonObject.put("code", "0000");
								jsonObject.put("message", "success");
								rc.response().end(jsonObject.encodePrettily());
							}
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					} else {
						rc.response().end(new JsonObject().put("code", "1001").encodePrettily());
					}
				});
			}
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " DIETPLANS V3 CACHE - DETOX [" + timeElapsed + " milliseconds]");
	}

	public void fetchnSaveCust(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler fetchnSaveCust() " + traceId;
		String email = rc.request().getParam("email");
		String name = rc.request().getParam("name");
		flightitwayService.fetchnSaveCust(email, name, traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " FETCH AND SAVE CUSTOMER [" + timeElapsed + " milliseconds]");
	}

	public void fetchPendingCust(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler fetchupdateplan() " + traceId;
		String email = rc.request().getParam("email");
		String date = rc.request().getParam("date");
//		String email = rc.user().principal().getString("email");
		flightitwayService.fetchPendingCust(email, date, traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " FETCH PENDING CUSTOMERS [" + timeElapsed + " milliseconds]");
	}

	public void saveDetoxDietplanStatus(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler saveDetoxDietplanStatus() " + traceId;
		String email = rc.user().principal().getString("email");
		boolean isDetox = rc.getBodyAsJson().getBoolean("detox", false);
//		boolean isDetox = (null != rc.request().getParam("detox") && !"".equals(rc.request().getParam("detox"))
//				&& "true".equalsIgnoreCase(rc.request().getParam("detox"))) ? true : false;
		//if (null != date && !"".equals(date))
//		String date = (null != rc.getBodyAsJson().getString("date", null) && !"".equals(rc.getBodyAsJson().getString("date"))) ? rc.getBodyAsJson().getString("date", ApiUtils.getFilteredDateddMMyyyy(0)) : ApiUtils.getFilteredDateddMMyyyy(0);
		String date = rc.getBodyAsJson().getString("date", ApiUtils.getFilteredDateddMMyyyy(0));
		logger.info("##### " + method + " DATE -->> " + date);
		flightitwayService.saveDetoxDietplanStatus(email, isDetox, date, rc.request().uri(), traceId, h -> {
			if (h.succeeded()) {
				rc.response().end(h.result().encodePrettily());
			} else {
				rc.response().end(new JsonObject().put("code", "1001").encodePrettily());
			}
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " SAVE DETOX DIET PLAN STATUS [" + timeElapsed + " milliseconds]");
	}

	public void fetchHelp(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler fetchHelp() " + traceId;
		String email = rc.request().getParam("email");
		String date = rc.request().getParam("date");
//		String email = rc.user().principal().getString("email");
		flightitwayService.fetchHelp(email, traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " FETCH HELP [" + timeElapsed + " milliseconds]");
	}

	public void getCustPaymentDetails(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler getCustPaymentDetails() " + traceId;
		String email = rc.user().principal().getString("email");
		Integer noOfDays = 30;
		flightitwayService.getCustPaymentDetails(email, noOfDays, traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " GET CUSTOMER PAYMENT DETAILS [" + timeElapsed + " milliseconds]");
	}

	public void fetchDietPlans(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler fetchDietPlans() " + traceId;
		logger.info("##### " + method + " ------ STARTED ------");
		String email = rc.user().principal().getString("email");
		logger.info("##### " + method + " EMAIL -->> " + email);
//		boolean isDetox = (null != rc.request().getParam("detox") && !"".equals(rc.request().getParam("detox"))
//				&& "true".equalsIgnoreCase(rc.request().getParam("detox"))) ? true : false;
		
		
		flightitwayService.fetchDietPlans(email, traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " FETCH DIETPLANS [" + timeElapsed + " milliseconds]");
	}

	public void fetchAllCustomersDetails(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler fetchAllCustomersDetails() " + traceId;
		logger.info("##### " + method + " ------ STARTED ------");
		flightitwayService.fetchAllCustomersDetails(traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " FETCH ALL CUSTOMERS DETAILS [" + timeElapsed + " milliseconds]");
	}

	public void saveDetoxDefaultDays(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler saveDetoxDefaultDays() " + traceId;
		String email = rc.user().principal().getString("email");
		JsonObject request = rc.getBodyAsJson();
		flightitwayService.saveDetoxDefaultDays(email, request, rc.request().uri(), traceId, h -> {
			if (h.succeeded()) {
				rc.response().end(h.result().encodePrettily());
			} else {
				rc.response().end(new JsonObject().put("code", "1001").encodePrettily());
			}
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " SAVE DETOX DEFAULT DAYS  [" + timeElapsed + " milliseconds]");
	}

	public void updatePaymentDetails(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler updatePaymentDetails() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.info("##### " + method + " EMAIL -->> " + email);
		JsonObject request = rc.getBodyAsJson();
		
		
		flightitwayService.updatePaymentDetails(email, request, rc.request().uri(), traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " UPDATE PAYMENT DETAILS [" + timeElapsed + " milliseconds]");
	}

	public void updateTargetWeight(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler updateTargetWeight() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.info("##### " + method + " EMAIL -->> " + email);
		JsonObject request = rc.getBodyAsJson();
		
		
		flightitwayService.updateTargetWeight(email, request, rc.request().uri(), traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " UPDATE TARGETED WEIGHT [" + timeElapsed + " milliseconds]");
	}

	public void updateCurrentWeight(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler updateCurrentWeight() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.info("##### " + method + " EMAIL -->> " + email);
		JsonObject request = rc.getBodyAsJson();
		
		
		flightitwayService.updateCurrentWeight(email, request, rc.request().uri(), traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " UPDATE CURRENT WEIGHT [" + timeElapsed + " milliseconds]");
	}

	public void saveOrUpdateCustSurvey(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler saveCustSurvey() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.info("##### " + method + " EMAIL -->> " + email);
		JsonObject request = rc.getBodyAsJson();
		
		
		flightitwayService.saveOrUpdateCustSurvey(email, request, rc.request().uri(), traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " UPDATE CURRENT WEIGHT [" + timeElapsed + " milliseconds]");
	}

	public void updateCustMobile(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler updateCustMobile() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.info("##### " + method + " EMAIL -->> " + email);
		JsonObject request = rc.getBodyAsJson();
		
		
		flightitwayService.updateCustMobile(email, request, rc.request().uri(), traceId, resultHandler -> {
			rc.response().end(resultHandler.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " UPDATE CURRENT WEIGHT [" + timeElapsed + " milliseconds]");
	}

	public void fetchDietPlanTimings(RoutingContext rc) {
		Instant start = Instant.now();
		String traceId = randomString();
		String method = "APIHandler fetchDietPlanTimings() " + traceId;
		String email = rc.user().principal().getString("email");
		logger.debug("##### " + method + "        URI [" + email + "] -->> " + rc.request().uri());
		logger.info("##### " + method + "      EMAIL -->> " + email);
		flightitwayService.fetchDietPlanTimings(email, traceId, h -> {
			rc.response().end(h.result().encodePrettily());
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " GET DIET PLAN TIMINGS [" + timeElapsed + " milliseconds]");
	}
}
