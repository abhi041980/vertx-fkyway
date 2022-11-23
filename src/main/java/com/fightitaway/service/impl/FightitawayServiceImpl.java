package com.fightitaway.service.impl;

import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fightitaway.api.ApiUtils;
import com.fightitaway.common.MongoRepositoryWrapper;
import com.fightitaway.service.CreatCustomerDietPlanRequest;
import com.fightitaway.service.CreatCustomerHabitPlanRequest;
import com.fightitaway.service.CreateProfileRequest;
import com.fightitaway.service.CustDietPreference;
import com.fightitaway.service.DBResult;
import com.fightitaway.service.FightitawayService;
import com.fightitaway.service.FilterData;
import com.fightitaway.service.FoodDetail;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;

/**
 * Fightitaway service implementation.
 * 
 * @author Smartdietplanner
 * @since 08-Aug-2020
 *
 */
public class FightitawayServiceImpl extends MongoRepositoryWrapper implements FightitawayService {
	private final static String CUST_PROFILE = "CUST_PROFILE";

	boolean isSubs = false;

	static final Integer LENGTH = 15;
	static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	static SecureRandom rnd = new SecureRandom();

	public String randomString() {
		StringBuilder sb = new StringBuilder(LENGTH);
		for (int i = 0; i < LENGTH; i++)
			sb.append(AB.charAt(rnd.nextInt(AB.length())));

		return "[" + sb.toString() + "]";
	}

	@SuppressWarnings({ "unused" })
	private void createPlan() {

		String traceId = randomString();
		vertx.getDelegate().fileSystem().open("/Users/b56521/Desktop/fightitaway/json/new.json",
				new OpenOptions().setWrite(false).setCreate(false), result -> {
					if (result.succeeded()) {

						AsyncFile asyncFile = result.result();
						io.vertx.core.parsetools.RecordParser recordParser = io.vertx.core.parsetools.RecordParser
								.newDelimited("\n");
						asyncFile.handler(recordParser);

						recordParser.setOutput(bufferedLine -> {
							JsonObject jsonObject = new JsonObject(bufferedLine);
							jsonObject.put("_id", jsonObject.getString("itemCode"));
							jsonObject.put("code", jsonObject.getString("itemCode"));
							jsonObject.put("Food", jsonObject.getString("Food").trim());
							jsonObject.put("Community", getJSonArray(jsonObject.getString("Community")));
							jsonObject.put("Slots", getSlots(jsonObject.getString("Slots")));
							jsonObject.put("RecommendedIn", getJSonArray(jsonObject.getString("RecommendedIn")));
							jsonObject.put("AvoidIn", getJSonArray(jsonObject.getString("AvoidIn")));
							jsonObject.put("Season", getJSonArray(jsonObject.getString("Season")));
							createDocument("DIET_PLAN_NEW", jsonObject, traceId);

						});
						asyncFile.endHandler(v -> logger.info("done"));
					} else {
						System.err.println("Oh oh ..." + result.cause());
					}
				});

	}

	private JsonArray getJSonArray(String data) {
		if (data == null || data.isEmpty()) {
			return null;
		}
		JsonArray array = new JsonArray();
		String[] sp = data.split(",");
		for (int i = 0; i < sp.length; i++) {
			array.add(sp[i]);
		}
		return array;
	}

	private JsonArray getSlots(String data) {
		if (data == null || data.isEmpty()) {
			return null;
		}
		JsonArray array = new JsonArray();
		String[] sp = data.split(",");
		for (int i = 0; i < sp.length; i++) {
			array.add(Integer.parseInt(sp[i]));
		}
		return array;
	}

	public FightitawayServiceImpl(Vertx vertx, JsonObject config) {
		super(vertx, config);
		// createPlan();
	}

	@Override
	public FightitawayService getDefaultDeatil(String traceId, Handler<AsyncResult<JsonObject>> resultHandler) {
		super.getDietPref(new JsonObject(), traceId).compose(plan -> getCommunity("", plan, traceId))
				.compose(json -> getDiseases("", json, traceId)).compose(json -> getPortions("", json, traceId))
				.compose(json -> getDefaultProfile("", json, traceId)).onComplete(resultHandler);

		return this;
	}
	
	/**
	 * Create profile.
	 * 
	 * @param request
	 * @param uri
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService createProfile(CreateProfileRequest request, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl createProfile() " + traceId + "-[" + request.getEmail() + "]";
		Instant start = Instant.now();
		logger.info("##### " + method + "          REQUEST -->> " + request);
		JsonObject document = new JsonObject();
		JsonObject profile = new JsonObject();

		profile.put("email", request.getEmail()).put("family_name", request.getFamily_name()).put("given_name",
				request.getGiven_name()).put("country", request.getCountry());
		//.put("country", request.getAppsource());
		profile.put("name", request.getName()).put("login_type", request.getLoginType())
				.put("region", request.getRegion()).put("source", request.getAppsource())
				.put("device", request.getDevice()).put("os_version", request.getOs());
		logger.info("##### " + method + " PROFILE -->> " + profile);

		getOne(CUST_PROFILE, "_id", request.getEmail(), traceId).onComplete(ha -> {
			logger.info("##### " + method + " HA           -->> " + ha);
			logger.info("##### " + method + " HA.SUCCEEDED -->> " + ha.succeeded());
			Calendar calll = Calendar.getInstance(); // creates calendar
			calll.setTime(new Date()); // sets calendar time/date
			calll.add(Calendar.HOUR_OF_DAY, 10); // adds ten hours
			calll.add(Calendar.MINUTE, 30); // add 30 minutes
			profile.put("createdDate", new SimpleDateFormat("dd-MMM-yyyy").format(calll.getTime()));
			if (ha.succeeded() && null != request.getEmail() && !"".equalsIgnoreCase(request.getEmail())
					&& !"null".equalsIgnoreCase(request.getEmail())) {
				logger.info("##### " + method + " INSIDE UPDATE PROFILE");
				super.removeCustVideoFoodItems(request.getEmail(), traceId)
						.compose(data -> updateProfile(request.getEmail(), profile, traceId))
						.compose(data -> logCustRequest(request.getEmail(), uri, traceId)).onComplete(h -> {
							resultHandler.handle(Future.succeededFuture(h.result()));
						});
			} else {
				logger.info("##### " + method + " REACHED FOR NEW PROFILE CREATION.");
				profile.put("createdDateTime", new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").format(calll.getTime()));
				document.put("profile", profile);
				document.put("_id", request.getEmail());
				super.logCustRequest(request.getEmail(), uri, traceId)
						.compose(data -> super.createEmptyDocument(profile, traceId))
						.compose(data -> createDocument(CUST_PROFILE, document, traceId)).onComplete(h -> {
							if (h.succeeded()) {
								resultHandler.handle(Future.succeededFuture(new JsonObject()));
							}
						});
//				createDocument(CUST_PROFILE, document, traceId).onComplete(h -> {
//					if (h.succeeded()) {
//						resultHandler.handle(Future.succeededFuture(new JsonObject()));
//					}
//				});
			}
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " CREATE/UPDATE PROFILE [" + timeElapsed + " milliseconds]");

		return this;
	}
	
	/**
	 * Get profile.
	 * 
	 * @param email
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService getProfile(String email, String traceId, Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl getProfile() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + " EMAIL -->> " + email);
		JsonObject response = new JsonObject();
		super.checkPlanSubscription(email, traceId).onComplete(res -> {
			if (res.succeeded()) {
				isSubs = res.result();
			}
			getOne(CUST_PROFILE, "_id", email, traceId).onComplete(ha -> {
				if (ha.succeeded()) {
					JsonObject resp = ha.result();
					resp.put("isPlanSubsCribe", isSubs);
					resultHandler.handle(Future.succeededFuture(ha.result()));
				} else {
					resultHandler.handle(
							Future.succeededFuture(response.put("code", "0001").put("message", ha.cause().toString())));
				}
			});
		});

		return this;
	}
	
	/**
	 * Update demographic.
	 * 
	 * @param request
	 * @param uri
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService updateDemographic(String email, JsonObject request, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl updateDemographic() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + " EMAIL   -->> " + email);
		logger.info("##### " + method + " REQUEST -->> " + request);

		JsonObject height = request.getJsonObject("height");
		String hUnit = height.getString("unit");
		Double hValue = height.getDouble("value");
		if (!"cm".equalsIgnoreCase(hUnit)) {
			hValue = hValue * 2.54f;
		}

		logger.info("##### " + method + " hValue -->> " + hValue);
		JsonObject weight = request.getJsonObject("weight");
		String wUnit = weight.getString("unit");
		Double wValue = weight.getDouble("value");
		logger.info("##### " + method + " wUnit -->> " + wUnit);

		if (!"kg".equalsIgnoreCase(wUnit))
			wValue = wValue * .45f;
		logger.info("##### " + method + " wValue -->> " + wValue);

		List<Double> bmi = new ArrayList<>();
		List<Double> suggestedWeight = new ArrayList<>();
		Double bmiValue = (wValue * 1000 * 10) / (hValue * hValue);
		Double recommendedWeight = (24 * hValue * hValue) / (10000);
		logger.info("##### " + method + " RECOMMENDED WEIGHT -->> " + recommendedWeight);
		if (recommendedWeight > (wValue - 3))
			recommendedWeight = (22 * hValue * hValue) / (10000);

		logger.info("##### " + method + " BMI -->> " + bmiValue);
		suggestedWeight.add(recommendedWeight);
		bmi.add(bmiValue);
		logger.info("##### " + method + " SUGGESTED WEIGHT -->> " + recommendedWeight);

		DecimalFormat decimalFormat = new DecimalFormat("#.00");
		JsonObject response = new JsonObject();
		request.put("bmi", new Double(decimalFormat.format(bmi.get(0))).doubleValue());
		request.put("suggestedWeight", new Double(decimalFormat.format(suggestedWeight.get(0))).doubleValue());
		request.put("currentWeight", wValue);
		request.put("heightInCM", hValue);
		request.put("weightInKg", wValue);
		request.put("category", ApiUtils.getCategoryForBMI(bmi.get(0).doubleValue()));
		request.put("updatedDate", ApiUtils.getCurrentDateInddMMMyyyyFormat(0));
		request.put("updatedDateTime", ApiUtils.getCurrentTime());
		logger.info("##### " + method + " DEMOGRAPHIC REQUEST -->> " + request.toString());
		super.logCustRequest(email, uri, traceId)
				.compose(data -> super.updateByKey("demographic", email, request, traceId)).onComplete(ha -> {
					if (ha.succeeded()) {
						super.fetchCustomerProfilePostDemographicUpdate(email, traceId).onComplete(h -> {
							if (h.succeeded()) {
								if (!h.result().containsKey("lifeStyle")) {
									///////////////////////////////////////////////////////
									JsonObject payload = new JsonObject();
									super.getOne(CUST_PROFILE, "_id", email, traceId)
											.compose(data -> super.getProfile(email, payload, request, traceId))
//											.compose(data -> super.sendEmailPostDemographicUpdateBySendInBlue(email,data, traceId))
											.compose(data -> super.sendEmailPostDemographicUpdateByGmail(email,
													data, traceId))
											.onComplete(h1 -> {
												response.put("bmi", bmi.get(0));
												response.put("category",
														ApiUtils.getCategoryForBMI(bmi.get(0).doubleValue()));
												response.put("suggestedWeight", suggestedWeight.get(0));
												resultHandler.handle(Future.succeededFuture(
														response.put("code", "0000").put("message", "success")));
												// resultHandler.handle(Future.succeededFuture(h.result()));
											});
									///////////////////////////////////////////////////////
								} else {
									JsonObject lifeStylerequest = h.result().getJsonObject("lifeStyle");
									super.getCalcDataForLifeStyle(email, traceId)
											.compose(data -> super.getActivities(data, traceId))
											.compose(data -> super.updateLifeStyle(email, lifeStylerequest, data,
													traceId))
											.compose(data -> super.getCustDietPrefCode(email, new DBResult(), traceId))
											.compose(data -> super.getCustDietPreferencesFromDB(data, traceId))
											.onComplete(ha1 -> {
												super.getCustDietPlanTimingsProfile(email, traceId)
														.compose(data -> super.fetchCustomerDietPlanTimings(email, data,
																traceId))
														.compose(data -> super.getDefaultProfileForDietPlanTimings(data,
																traceId))
														.compose(data -> super.getDietListForFood(email, data, traceId))
														.compose(data -> createDietplan(data, ha1.result(),
																ApiUtils.getCurrentDate(email, traceId), traceId))
														.onComplete(resultHandler);
											});
								}
							}
						});
//						response.put("bmi", bmi.get(0));
//						response.put("category", ApiUtils.getCategoryForBMI(bmi.get(0).doubleValue()));
//						response.put("suggestedWeight", suggestedWeight.get(0));
//						resultHandler
//								.handle(Future.succeededFuture(response.put("code", "0000").put("message", "success")));
					} else {
						resultHandler.handle(Future.succeededFuture(
								response.put("code", "0001").put("message", "unable to update demographic ")));
					}
				});

		return this;
	}
	
	/**
	 * Update lifestyle.
	 * 
	 * @param email
	 * @param request
	 * @param uri
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService updateLifeStyle(String email, JsonObject request, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl updateLifeStyle() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + " EMAIL -->> " + email);

		logger.info("##### " + method + " UPDATE LIFESTYLE REQUEST [" + email + "] -->> " + request);
		super.logCustRequest(email, uri, traceId).compose(data -> super.getCalcDataForLifeStyle(email, traceId))
				.compose(data -> super.getActivities(data, traceId))
				.compose(data -> super.updateLifeStyle(email, request, data, traceId)).onComplete(ha -> {
					super.getCustDietPrefCode(email, new DBResult(), traceId)
							.compose(data -> super.getCustDietPreferencesFromDB(data, traceId)).onComplete(ha1 -> {
								super.getCustDietPlanTimingsProfile(email, traceId)
										.compose(data -> super.fetchCustomerDietPlanTimings(email, data, traceId))
										.compose(data -> super.getDefaultProfileForDietPlanTimings(data, traceId))
										.compose(data -> super.getDietListForFood(email, data, traceId))
										.onComplete(ha2 -> {
											super.fetchCustomerProfilePostLifeStyleUpdate(email, traceId)
													.onComplete(ha3 -> {
														if (ha3.succeeded() && ha3.result().containsKey("diet")) {
															createDietplan(ha2.result(), ha1.result(),
																	ApiUtils.getCurrentDate(email, traceId), traceId)
																			.onComplete(resultHandler);
														}
													});
										});
//							
							});

					logger.info("##### " + method + " UPDATE LIFESTYLE RESPONSE -->> " + ha.result());
					resultHandler.handle(Future.succeededFuture(ha.result()));
				});

//				.onComplete(resultHandler);

		return this;
	}
	
	/**
	 * Update diet.
	 * 
	 * @param email
	 * @param request
	 * @param uri
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService updateDiet(String email, JsonObject request, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl updateDiet() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + " EMAIL -->> " + email);
		logger.info("##### " + method + " REQUEST -->> " + request);
		super.logCustRequest(email, uri, traceId).compose(data -> this.getCalcDataForDiet(email, traceId))
				.compose(data -> super.getPlanActivatedDate(email, data, traceId))
				.compose(data -> this.updateDiet(email, request, data, traceId))
				//.compose(data -> this.fetchCustProfileReg(email, data, "reg", traceId))
				//.compose(data -> super.updateCustPofile(email, data, traceId))
				.onComplete(resultHandler);

		return this;
	}
	
	/**
	 * Get data for calculation.
	 * 
	 * @param email
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService getDataForCalculation(String email, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl getDataForCalculation() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + " EMAIL -->> " + email);
		super.getCalcDataForDiet(email, traceId).onComplete(h -> {
			JsonObject jsonObject = new JsonObject(Json.encode(h.result()));
			resultHandler.handle(Future.succeededFuture(jsonObject));
		});
		return this;
	}
	
	/**
	 * get dietplan v2.
	 * 
	 * @param email
	 * @param date
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService getDietPlanByEmailv2(String email, String date, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl getDietPlanByEmailv2() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + " EMAIL -->> " + email);
		super.getCustDietPrefCode(email, new DBResult(), traceId)
				.compose(data -> super.getCustDietPrefList(data, traceId)).onComplete(ha -> {
					this.getDataForFilterDiets(email, traceId)
							.compose(data -> super.getDietListForFood(email, data, traceId))
							.compose(data -> getDietDrinFromSream2(data, ha.result(), date, traceId))
							.onComplete(resultHandler);
				});

		return this;
	}
	
	/**
	 * Get dietplan by slot.
	 * 
	 * @param email
	 * @param slot
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService getDietPlanBySlot(String email, int slot, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl getDietPlanBySlot() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + " EMAIL -->> " + email);
		logger.info("##### " + method + " SLOT  -->> " + slot);
		super.getCustDietPrefCode(email, new DBResult(), traceId)
				.compose(data -> super.getCustDietPrefList(data, traceId)).onComplete(ha -> {
					this.getDataForFilterDiets(email, traceId)
							.compose(data -> super.getDietListForFood(email, data, traceId))
							.compose(data -> getDietDrinFromSreamBySlot(data, ha.result(), slot, traceId))
							.onComplete(resultHandler);
				});
		return this;
	}
	
	/**
	 * Get dietplan (v3).
	 * 
	 * @param email
	 * @param date
	 * @param uri
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService getDietPlanByEmailv3(String email, String date, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl getDietPlanByEmailv3() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + " EMAIL -->> " + email);
//		super.logCustRequest(email, uri, traceId)
//				.compose(data -> super.getCustDietPrefCode(email, new DBResult(), traceId))
//				.compose(data -> super.getCustDietPreferencesFromDB(data, traceId)).onComplete(ha -> {
//					super.getCustDietPlanTimingsProfile(email, traceId)
//							.compose(data -> super.fetchCustomerDietPlanTimings(email, data, traceId))
//							.compose(data -> super.getDefaultProfileForDietPlanTimings(data, traceId))
//							.compose(data -> super.getDietListForFood(email, data, traceId))
//							.compose(data -> createDietplan(data, ha.result(), date, traceId))
//							.onComplete(resultHandler);
//				});

				super.getCustDietPrefCode(email, new DBResult(), traceId)
				.compose(data -> super.getCustDietPreferencesFromDB(data, traceId)).onComplete(ha -> {
					super.getCustDietPlanTimingsProfile(email, traceId)
							.compose(data -> super.fetchCustomerDietPlanTimings(email, data, traceId))
							.compose(data -> super.getDefaultProfileForDietPlanTimings(data, traceId))
							.compose(data -> super.fetchPreviousDaysItems(email, data, traceId))
							.compose(data -> super.getDietListForFood(email, data, traceId))
							.compose(data -> createDietplan(data, ha.result(), date, traceId)).onComplete(ha1 -> {
								super.fetchTodaysDiets(email, ha1.result(), date, traceId).compose(
										data -> super.SaveTodaysItems(email, ha1.result(), date, traceId))
										.onComplete(resultHandler);

							});
				});

		return this;
	}
	
	/**
	 * Get dietplan (v3-configured).
	 * 
	 * @param email
	 * @param date
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService getDietPlanByEmailv3Config(String email, String date, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl getDietPlanByEmailv3Config() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + " EMAIL -->> " + email);
		super.getCustDietPrefCode(email, new DBResult(), traceId)
				.compose(data -> super.getCustDietPrefList(data, traceId)).onComplete(ha -> {
					this.getCustDietPlanTimingsProfile(email, traceId)
							.compose(data -> super.getDefaultProfileForDietPlanTimings(data, traceId))
							.compose(data -> super.getDietListForFood(email, data, traceId))
							.compose(data -> getDietDrinFromSreamv3Config(data, ha.result(), date, traceId))
							.onComplete(resultHandler);
				});
		return this;
	}
	
	/**
	 * Get dietplan via option.
	 * 
	 * @param email
	 * @param slot
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService getDietPlanByEmailForOption(String email, Integer slot, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl getDietPlanByEmailForOption() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + " EMAIL -->> " + email);
		logger.info("##### " + method + " SLOT -->> " + slot);
		this.getDataForFilterDiets(email, traceId).compose(data -> super.getDietListForFood(email, data, traceId))
				.compose(data -> super.fetchCustomerDietPlanTimingsForOptions(email, data, traceId))
				.compose(data -> getDietsForOption(data, slot, traceId)).onComplete(resultHandler);
		return this;
	}
	
	/**
	 * Get test.
	 * 
	 * @param email
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService getTest(String email, String traceId,
			Handler<AsyncResult<List<JsonObject>>> resultHandler) {
		String method = "FightitawayServiceImpl getTest() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + " EMAIL -->> " + email);
		super.getDieltPlanBySlots(email, traceId).onComplete(resultHandler);
		return this;
	}

	
	/**
	 * Create customer dietplan.
	 * 
	 * @param email
	 * @param request
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */	@Override
	public FightitawayService createCustomerDietPlan(String email, CreatCustomerDietPlanRequest request, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl createCustomerDietPlan() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + " EMAIL -->> " + email);
		super.createCustDietPlan(email, request, traceId).onComplete(resultHandler);
		return this;
	}
		
		/**
		 * Get customer diet list.
		 * 
		 * @param email
		 * @param traceId
		 * @param resultHandler
		 * @return FightitawayService
		 *
		 */
	@Override
	public FightitawayService getCustomerDietList(String email, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl getCustomerDietList() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + " EMAIL -->> " + email);
		super.getCustomerDietPlan(email, traceId).onComplete(resultHandler);
		return this;
	}
	
	/**
	 * Create habit master.
	 * 
	 * @param request
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService createHabitMaster(JsonObject request, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl createHabitMaster( ) " + traceId;
		logger.info("##### " + method + " REQUEST -->> " + request);
		super.createHabitMaster(request, traceId).onComplete(resultHandler);
		return this;
	}
	
	/**
	 * Get habit master.
	 * 
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService getHabitMaster(String traceId, Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl getHabitMaster() " + traceId;
		logger.info("##### " + method);
		super.getDefaultHabits(traceId).onComplete(resultHandler);
		return this;
	}
	
	/**
	 * Create customer habit.
	 * 
	 * @param email
	 * @param request
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService createCustomerHabit(String email, CreatCustomerHabitPlanRequest request, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl createCustomerHabit() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + " EMAIL -->> " + email);
		super.createCustHabit(email, request, traceId).onComplete(resultHandler);
		return this;
	}
	
	/**
	 * Get customer habits.
	 * 
	 * @param email
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService getCustomerHabitList(String email, String traceId,
			Handler<AsyncResult<List<JsonObject>>> resultHandler) {
		String method = "FightitawayServiceImpl getCustomerHabitList() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + " EMAIL -->> " + email);
		super.getCustomerHabit(email, traceId).compose(data -> super.getCustFollowedHabit(email, data, traceId))
				.compose(data -> super.getHabitMaster(data, traceId))
				.compose(data -> super.getCustomerHabit(data, traceId)).onComplete(resultHandler);
		return this;
	}
	
	/**
	 * Update customer habit.
	 * 
	 * @param email
	 * @param traceId
	 * @param request
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService updateCustomerHabit(String email, String traceId, CreatCustomerHabitPlanRequest request,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl updateCustomerHabit() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + " EMAIL -->> " + email);
		super.updateCustHabitStaus(email, request, traceId).onComplete(resultHandler);
		return this;
	}
	
	/**
	 * Get coupon list via coupon master.
	 * 
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService getCouponListFromMaster(String traceId, Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl getCouponListFromMaster() " + traceId;
		logger.info("##### " + method);
		super.getCouponList(traceId).onComplete(resultHandler);
		return this;
	}
	
	/**
	 * Get customer habit for update.
	 * 
	 * @param email
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService getCustomerHabitForUpdate(String email, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl getCustomerHabitForUpdate() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + " EMAIL -->> " + email);
		// super.getCustomerHabitForToday(email).compose(data ->
		// super.getCustomerHabitForUpdate(data, email)).onComplete(resultHandler);
		super.getProfileCreationDate(email, traceId).compose(data -> super.getCustomerHabitV2(email, data, traceId))
				.compose(data -> super.getCustomerHabitForToday(email, data, traceId))
				.compose(data -> super.getCustomerPaymentDetailForToday(email, data, traceId))
				.compose(data -> super.getCustomerHabitForUpdate(data, email, traceId)).onComplete(resultHandler);
		return this;
	}
	
	/**
	 * remove customer habit.
	 * 
	 * @param email
	 * @param code
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService deleteCustomerHabit(String email, String code, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl deleteCustomerHabit() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + " EMAIL -->> " + email);
		super.deleteCustHabitStaus(email, code, traceId).onComplete(resultHandler);
		return this;
	}
	
	/**
	 * Get custome weight graph data.
	 * 
	 * @param email
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService getCustomerWeightGraphData(String email, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl getCustomerWeightGraphData() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + " EMAIL -->> " + email);
		super.getProfileDetail(email, traceId).compose(data -> super.getSubscribePlanDate(email, data, traceId))
				.compose(profile -> super.getCustomerWeightGraphData(profile, traceId)).onComplete(resultHandler);
		return this;
	}
	
	/**
	 * Update customer weight graph data.
	 * 
	 * @param email
	 * @param weight
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService updateCustomerWeight(String email, Double weight, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl updateCustomerWeight() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + " EMAIL -->> " + email);
		logger.info("##### " + method + " WEIGHT -->> " + weight);
		super.updateCustomerWeight(email, weight, traceId).onComplete(resultHandler);
		return this;
	}
	
	/**
	 * get latest weight.
	 * 
	 * @param email
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService getLatestWeight(String email, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl getLatestWeight() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + " EMAIL -->> " + email);
		super.getCustomerLatestWeight(email, traceId).onComplete(resultHandler);
		return this;
	}
	
	/**
	 * Update customer diet preferences.
	 * 
	 * @param request
	 * @param uri
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService updateCustDietPref(CustDietPreference request, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl updateCustDietPref() " + traceId + "-[" + request.getEmail() + "]";
		logger.info("##### " + method + "   EMAIL -->> " + request.getEmail());
		logger.info("##### " + method + " REQUEST -->> " + request);
		super.logCustRequest(request.getEmail(), uri, traceId)
				.compose(data -> super.getCustDietPref(request.getEmail(), traceId))
				.compose(data -> super.updateDietPreferrence(data, request, traceId)).onComplete(resultHandler);
		return this;
	}
	
	/**
	 * Update customer diet preferences (v2).
	 * 
	 * @param request
	 * @param uri
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService updateCustDietPrefV2(CustDietPreference request, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl updateCustDietPrefV2() " + traceId + "-[" + request.getEmail() + "]";
		logger.info("##### " + method + "   EMAIL -->> " + request.getEmail());
		logger.info("##### " + method + " REQUEST -->> " + request);

		super.logCustRequest(request.getEmail(), uri, traceId)
				.compose(data -> super.getCustDietPrefV2(request, request.getEmail(), traceId))
				.compose(data -> super.updateOrSaveDietPreferrenceV2(data, request, traceId)).onComplete(resultHandler);
		return this;
	}
	
	/**
	 * Get customer diet preferences.
	 * 
	 * @param email
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService getCustDietPref(String email, String traceId,
			Handler<AsyncResult<JsonArray>> resultHandler) {
		String method = "FightitawayServiceImpl getCustDietPref() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + "]  EMAIL -->> " + email);
		super.getCustDietPref(email, traceId).onComplete(resultHandler);
		return this;
	}
	
	/**
	 * Remove plan.
	 * 
	 * @param email
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService removePlan(String email, String traceId, Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl removePlan() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + "   EMAIL -->> " + email);
		super.removeRemovePlans(email, traceId).onComplete(resultHandler);
		return this;
	}
	
	/**
	 * get customer diet preference List.
	 * 
	 * @param email
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService getCustomerPrefList(String email, String traceId,
			Handler<AsyncResult<List<JsonObject>>> resultHandler) {
		String method = "FightitawayServiceImpl getCustomerPrefList() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + "   EMAIL -->> " + email);
		super.getCustDietPrefCode(email, new DBResult(), traceId)
				.compose(data -> super.getCustDietPrefList(data, traceId)).onComplete(resultHandler);
		return this;
	}
	
	/**
	 * Get diet by code.
	 * 
	 * @param code
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService getDietByCode(String code, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl getDietByCode() " + traceId;
		logger.info("##### " + method + "    CODE -->> " + code);
		super.getFoodByCode(code, traceId).onComplete(resultHandler);
		return this;
	}
	
	/**
	 * Get today diet list.
	 * 
	 * @param email
	 * @param date
	 * @param uri
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService getTodayDietList(String email, String date, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl getTodayDietList() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + "   EMAIL -->> " + email);
		super.logCustRequest(email, uri, traceId).compose(data -> super.getTodayDietList(email, date, traceId))
				.onComplete(resultHandler);
		return this;
	}
	
	/**
	 * Update today diet plan. [NOT IN USE]
	 * 
	 * @param email
	 * @param slotId
	 * @param slotDetail
	 * @param date
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService updateTodaDietPlan(String email, Integer slotId, JsonObject slotDetail, String date, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl updateTodaDietPlan() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + "   EMAIL -->> " + email);
		super.getTodayDietList(email, date, traceId)
				.compose(data -> super.updateTodayDietPlan(email, slotId, data, slotDetail, traceId))
				.onComplete(resultHandler);
		return this;
	}
	
	/**
	 * Update cache data.
	 * 
	 * @param email
	 * @param slotId
	 * @param foodCodeList
	 * @param date
	 * @param uri
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService updateCacheData(String email, Integer slotId, Set<FoodDetail> foodCodeList,
			String date, String uri, String traceId, Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl updateCacheData() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + "     EMAIL -->> " + email);
		logger.info("##### " + method + "    SLOTID -->> " + slotId);
		logger.info("##### " + method + " FOOD LIST -->> " + foodCodeList);
		super.logCustRequest(email, uri, traceId)
				.compose(data -> super.getDietPlanFromCache(email, slotId, foodCodeList, date, traceId)).onComplete(res -> {
					super.getDietListForFoodCode(slotId, foodCodeList, traceId)
							.compose(data -> super.createSlotDetail(slotId, data, traceId))
							.compose(data -> super.updatedSlotDetailRecommendedFor(email, data, traceId))
							.compose(data -> super.updateTodayDietPlanCache(email, slotId, res.result(), data,
									foodCodeList, date, traceId))
							.onComplete(resultHandler);
				});
		logger.info("##### " + method + " RETURN FROM UPDATE CACHE DATA.");
		return this;
	}
	
	/**
	 * Get customer diet details for edit functionality.
	 * 
	 * @param email
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService getCustDietDetailsOnEdit(String email, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl getCustDietDetailsOnEdit() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + "   EMAIL -->> " + email);
		super.getCustDietProfile(email, traceId)
				.compose(filteredCustdata -> super.getCustDietPref(email, new JsonObject(), filteredCustdata, traceId))
				.compose(plan -> getCustCommunity(plan, traceId)).compose(json -> getCustDiseases(json, traceId))
				.compose(json -> getCustPortions(json, traceId)).compose(json -> getCustDefaultProfile(json, traceId))
				.onComplete(resultHandler);

		return this;
	}
	
	/**
	 * get default details.
	 * 
	 * @param email
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService getDefaultDetail(String email, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl getDefaultDetail() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + "   EMAIL -->> " + email);
		super.getCustDietProfile(email, traceId)
				.compose(filteredCustdata -> getSelectiveItemsByCommunity(email, filteredCustdata, traceId))
				.compose(filteredCustdata -> getCustDietPrefNew(email, new JsonObject(), filteredCustdata, traceId))
				.compose(plan -> getCommunity(email, plan, traceId)).compose(json -> getDiseases(email, json, traceId))
				.compose(json -> getPortions(email, json, traceId))
				.compose(json -> getDefaultProfile(email, json, traceId)).onComplete(resultHandler);

		return this;
	}
	
	/**
	 * get customer dietplan timings profile.
	 * 
	 * @param email
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService getCustDietPlanTimingsProfile(String email, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl getCustDietPlanTimingsProfile() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + "   EMAIL -->> " + email);

		super.getCustDietPlanTimingsProfile(email, traceId).compose(
				filteredCustdata -> getDefaultProfileForDietPlanTimings1(new JsonObject(), filteredCustdata, traceId))
				.onComplete(resultHandler);
		return this;
	}
	
	/**
	 * Send email.
	 * 
	 * @param email
	 * @param request
	 * @param uri
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService sendEmail(String email, JsonObject request, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl sendEmail() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + "   EMAIL -->> " + email);
		logger.info("##### " + method + " REQUEST -->> " + request);

		super.isEmailSendAllowed(email, request, traceId).onComplete(map -> {
			if (map.succeeded()) {
				super.sendEmail(email, map.result(), traceId)
				.compose(json -> setMailSentStatus(email, json, traceId))
				.onComplete(h -> {
					if (h.succeeded())
						resultHandler.handle(Future.succeededFuture(new JsonObject(Json.encode(h.result()))));
				});
			} else {
				resultHandler.handle(Future.succeededFuture(
						new JsonObject().put("code", "0000").put("message", map.cause().toString())));
			}
		});

		return this;
	}
	
	/**
	 * Get dashboard.
	 * 
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService getDashboard(String traceId, Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl getDashboard() " + traceId;
		logger.info("##### " + method + " FETCHING DASHBOARD.");
		super.getCompleteSubscribedPlanDate(traceId).compose(data -> getCustProfiles(new JsonObject(), data))
				.onComplete(resultHandler);
		return this;
	}
	
	/**
	 * get dietplan via cache.
	 * 
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService getDietPlanCachedata(String traceId, Handler<AsyncResult<JsonObject>> resultHandler) {
		super.getDietPlanCachedata(traceId).onComplete(resultHandler);
		return this;
	}
	
	/**
	 * Add TnC.
	 * 
	 * @param request
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService addTnC(JsonObject request, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl addTnC() " + traceId;
		logger.info("##### " + method + " REQUEST -->> " + request);
		super.addTnC(request, traceId).onComplete(resultHandler);
		return this;
	}
	
	/**
	 * Remove profile.
	 * 
	 * @param email
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService profileRemove(String email, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl profileRemove() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + " EMAIL -->> " + email);
		super.removePreferences(email, traceId).compose(data -> removePlanSubscriptionDetail(email, traceId))
				.compose(data -> removePaymentDetail(email, traceId))
				.compose(data -> removeCustomeHabitDetail(email, traceId))
				.compose(data -> removeCustomeHabitFollow(email, traceId))
				.compose(data -> removeCustomeDailyWeight(email, traceId))
				.compose(data -> removeCustomeDailyDietCache(email, traceId))
				.compose(data -> removeCustomeDailyDietCacheDetox(email, traceId))
				.compose(data -> removeCustomerDietPreV2(email, traceId))
				.compose(data -> removeCustomerLoginDateTime(email, traceId))
				.compose(data -> removeCustomerWaterReminderStatus(email, traceId))
				.compose(data -> removeCustomerWaterDrank(email, traceId))
				.compose(data -> removeCustomerCaloriesBurnt(email, traceId))
				.compose(data -> removeAnalytics(email, traceId))
				.compose(data -> removeCustVideoFoodItems(email, traceId))
				.compose(data -> removeProfile(email, traceId)).onComplete(resultHandler);
		logger.info("##### " + method + " EMAIL -->> " + email);
		return this;
	}
	
	/**
	 * Get customer plan details.
	 * 
	 * @param email
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService getOnePlan(String email, String traceId, Handler<AsyncResult<JsonObject>> resultHandler) {
		super.getOnePlan(email, traceId).compose(data -> this.fetchCustProfileReg(email, data, "", traceId))
				.onComplete(resultHandler);
		return this;
	}
	
	/**
	 * Verify if plan is subscribed.
	 * 
	 * @param email
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	public JsonObject isPlanSubscribed(String email, String traceId, Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl isPlanSubscribed() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + " EMAIL -->> " + email);
		JsonObject response = new JsonObject();
		super.checkPlanSubscription(email, traceId).onComplete(res -> {
			if (res.succeeded()) {
				logger.info("##### " + method + " IS PLAN ACTIVE [" + res.result());
				response.put("code", "0000");
				response.put("isPlanSubsCribed", true);
				resultHandler.handle(Future.succeededFuture(response));
			} else {
				logger.info("##### " + method + " IS PLAN ACTIVE 2 [" + res.result());
				response.put("code", "0001").put("isPlanSubsCribed", false);
				resultHandler
						.handle(Future.succeededFuture(response.put("code", "0001").put("isPlanSubsCribed", false)));
			}
		});

		return response;
	}
	
	/**
	 * Subscribe plan via coupon.
	 * 
	 * @param email
	 * @param couponCode
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService subscribePlanByCoupon(String email, String couponCode, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl subscribePlanByCoupon() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + "              EMAIL -->> " + email);
		logger.info("##### " + method + "        COUPON CODE -->> " + couponCode);

		super.getCouponDetailsByCouponCode(email, couponCode, traceId)
				.compose(data -> subcribePlanForDiscountedCoupon(data, traceId))
				.compose(data -> super.subscribePlanByCoupon(data, traceId)).onComplete(resultHandler);
		return this;
	}
	
	/**
	 * Fetch food.
	 * 
	 * @param email
	 * @param foodId
	 * @param uri
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService fetchFood(String email, String foodId, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl fetchFood() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + "              EMAIL -->> " + email);
		logger.info("##### " + method + "            FOOD ID -->> " + foodId);
		super.logCustRequest(email, uri, traceId)
				.compose(data -> super.getCustomerProfileForFetchFoodItems(email, traceId))
				.compose(data -> super.getCustDiseases(data, traceId))
				.compose(data -> super.fetchFood(email, foodId, data, traceId)).onComplete(resultHandler);

		return this;
	}
	
	/**
	 * Save customer calories burnt.
	 * 
	 * @param email
	 * @param calBurnt
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService saveCustCaloriesBurnt(String email, Double calBurnt, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl saveCustCaloriesBurnt() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + "              EMAIL -->> " + email);
		logger.info("##### " + method + "            FOOD ID -->> " + calBurnt);
		super.saveCustCaloriesBurnt(email, calBurnt, traceId).onComplete(resultHandler);

		return this;
	}
	
	/**
	 * Customer water tips.
	 * 
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService waterTips(String traceId, Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl waterTips() " + traceId;
		logger.info("##### " + method + " WATER TIPS");
		super.waterTips(traceId).onComplete(resultHandler);

		return this;
	}
	
	/**
	 * Save water drank.
	 * 
	 * @param email
	 * @param waterQuantity
	 * @param dateTime
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService saveWaterDrank(String email, Integer waterQuantity, String dateTime, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl saveWaterDrank() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + "          EMAIL -->> " + email);
		logger.info("##### " + method + " WATER QUANTITY -->> " + waterQuantity);
		logger.info("##### " + method + "      DATE-TIME -->> " + dateTime);
		super.saveWaterDrank(email, waterQuantity, dateTime, traceId)
				.compose(data -> findWaterDrank(email, data, traceId)).onComplete(resultHandler);

		return this;
	}
	
	/**
	 * Customer water rcommendations.
	 * 
	 * @param email
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService waterRecommendation(String email, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl waterRecommendation() " + traceId + "-[" + email + "]";
		logger.debug("##### " + method + "              EMAIL -->> " + email);
		super.waterRecommendation(email, traceId).compose(data -> findWaterDrank(email, data, traceId))
				.onComplete(resultHandler);

		return this;
	}
	
	/**
	 * Fetch customer water reminder.
	 * 
	 * @param email
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService fetchWaterReminder(String email, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl fetchWaterReminder() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + "                 EMAIL -->> " + email);

		super.fetchWaterReminder(email, traceId).compose(data -> super.formatWaterReminder(email, data, traceId))
				.onComplete(resultHandler);
		return this;
	}
	
	/**
	 * Fetch customer water reminder.
	 * 
	 * @param email
	 * @param waterReminderStatus
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService custWaterReminder(String email, String waterReminderStatus, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl custWaterReminder() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + "                 EMAIL -->> " + email);
		logger.info("##### " + method + " WATER REMINDER STATUS -->> " + waterReminderStatus);

		super.fetchWaterReminder(email, traceId)
				.compose(data -> super.saveUpdateWaterReminder(email, waterReminderStatus, data, traceId))
				.onComplete(resultHandler);
		return this;
	}
	
	/**
	 * Get customer specific details basis community.
	 * 
	 * @param email
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService getSpecificDetailsByCommunity(String email, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl getSpecificDetailsByCommunity() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + " EMAIL -->> " + email);

		super.getCustDietProfile(email, traceId)
				.compose(filteredCustdata -> getSelectiveItemsByCommunity(email, filteredCustdata, traceId))
				.compose(filteredCustdata -> getCustDietPrefNew(email, new JsonObject(), filteredCustdata, traceId))
				.compose(plan -> getCommunity(email, plan, traceId)).compose(json -> getDiseases(email, json, traceId))
				.compose(json -> getPortions(email, json, traceId))
				.compose(json -> getDefaultProfile(email, json, traceId)).onComplete(resultHandler);
		return this;
	}
	
	/**
	 * Fetch calories history.
	 * 
	 * @param email
	 * @param noOfDays
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService fetchCaloriesHistory(String email, Integer noOfDays, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl fetchCaloriesHistory() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + " EMAIL -->> " + email);
		super.fetchCaloriesHistory(email, noOfDays, traceId).onComplete(resultHandler);

		return this;
	}
	
	/**
	 * fetch target calories.
	 * 
	 * @param email
	 * @param noOfDays
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService fetchTargetCalories(String email, Integer noOfDays, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl fetchTargetCalories() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + " NO OF DAYS -->> " + noOfDays);
		super.fetchTodaysDietListCacheDetox(email, "", traceId).onComplete(ha -> {
			if (!ha.succeeded()) {
				logger.info("##### " + method + " INSIDE 1ST");
				super.getDefaulProfile(email, traceId).compose(data -> getLatestCustomerWeight(email, data, traceId))
						.compose(data -> super.getCustTargetCalories(email, data, traceId))
						.compose(data -> super.fetchLastCaloriesHistory(email, noOfDays, data, traceId))
						.compose(data -> super.getCustomerAllDietPlanStatusDetox(email, data, traceId))
						.compose(data -> super.getFutureFollowedDaysDetox(email, data, traceId))
//						.compose(data -> super.getFutureFollowedDietPlansDetox(email, data, traceId))
						.onComplete(resultHandler);
			} else {
				super.fetchDetoxDietPlanStatus(email, "", traceId).onComplete(h -> {
					logger.info("##### " + method + " INSIDE ELSE -->> " + h.result());
					if (h.succeeded() && h.result()) {
						logger.info("##### " + method + " INSIDE 2ND");
						super.getDefaulProfile(email, traceId)
								.compose(data -> getLatestCustomerWeight(email, data, traceId))
								.compose(data -> super.getCustTargetCalories(email, data, traceId))
//								.compose(data -> super.fetchLastCaloriesHistoryDetox(email, noOfDays, data, traceId))
								.compose(data -> super.fetchLastCaloriesHistory(email, noOfDays, data, traceId))
								.compose(data -> super.getCustomerAllDietPlanStatusDetox(email, data, traceId))
								.compose(data -> super.getFutureFollowedDaysDetox(email, data, traceId))
//								.compose(data -> super.getFutureFollowedDietPlansDetox(email, data, traceId))
								.onComplete(resultHandler);
					} else {
						logger.info("##### " + method + " INSIDE 3RD");
						super.getDefaulProfile(email, traceId)
								.compose(data -> getLatestCustomerWeight(email, data, traceId))
								.compose(data -> super.getCustTargetCalories(email, data, traceId))
								.compose(data -> super.fetchLastCaloriesHistory(email, noOfDays, data, traceId))
								.compose(data -> super.getCustomerAllDietPlanStatusDetox(email, data, traceId))
								.compose(data -> super.getFutureFollowedDaysDetox(email, data, traceId))
//								.compose(data -> super.getFutureFollowedDietPlansDetox(email, data, traceId))
								.onComplete(resultHandler);
					}
				});
			}
		});

		return this;
	}
	
	/**
	 * Dietplan refresh options.
	 * 
	 * @param email
	 * @param slot
	 * @param categoryNamefoodItem
	 * @param date
	 * @param uri
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService dietPlanRefreshOption(String email, Integer slot, String categoryNamefoodItem, String date, String uri, 
			String traceId, FoodDetail foodItem, Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl dietPlanRefreshOption() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + "      SLOT -->> " + slot);
		logger.info("##### " + method + "      DATE -->> " + date);
		logger.info("##### " + method + "  CATEGORY -->> " + categoryNamefoodItem);
		logger.info("##### " + method + " FOOD CODE -->> " + foodItem.getCode());
		Set<FoodDetail> foodList = new HashSet<FoodDetail>();
		foodList.add(foodItem);
		super.getDietPlanFromCache(email, slot, null, date, traceId).onComplete(res -> {
			super.logCustRequest(email, uri, traceId).compose(data -> super.getDataForFilterDiets(email, traceId))
					.compose(data -> super.getDietListRefreshFood(email, data, traceId))
					.compose(data -> super.fetchCustomerDietPlanTimingsForOptions(email, data, traceId))
					.compose(data -> getFoodForRefreshOption(data, slot, traceId))
					.compose(data -> getCustAlreadyRefreshedDietItems(email, data, traceId))
					.compose(data -> getDietPlanForRefreshOptionAndFoodCode(email, slot, foodItem, data, traceId))
					.compose(data -> getRefreshedDietItem(slot, data, categoryNamefoodItem, foodItem, email, traceId))
					.compose(data -> super.getDietListForRefreshFoodCode(slot, foodItem, data, traceId))
					.compose(data -> super.createSlotRefreshDetail(slot, data, traceId))
					.compose(data -> super.updateTodayDietPlanCacheRefresh(email, slot, res.result(), data, foodList,
							date, traceId))
					.onComplete(resultHandler);
		});

		return this;
	}
	
	/**
	 * Get customer analytics.
	 * 
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService getCustAnalytics(String traceId, Handler<AsyncResult<JsonObject>> resultHandler) {
		super.getCustAnalytics(traceId).onComplete(resultHandler);

		return this;
	}
	
	/**
	 * refresh dietplan food.
	 * 
	 * @param email
	 * @param uri
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService refreshFoods(String email, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl refreshFoods() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + "     EMAIL -->> " + email);
		super.logCustRequest(email, uri, traceId)
				.compose(data -> super.getCustomerProfileForFetchFoodItems(email, traceId))
				.compose(data -> getCustVideoFoodItems(email, data, traceId))
				.compose(data -> getFoodItems(email, data, traceId))
				.compose(data -> saveCustVideoFoodItems(email, data, traceId)).onComplete(resultHandler);

		return this;
	}
	
	/**
	 * get customer payment analytics.
	 * 
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService getCustPaymentAnalytics(Integer noOfDays, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl getCustPaymentAnalytics() " + traceId;
		logger.info("##### " + method);
		super.getPaymentDetailAnalytics(noOfDays, traceId).onComplete(resultHandler);

		return this;
	}
	
	/**
	 * get diet plan cache analytics.
	 * 
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService getDietPlanCacheAnalytics(String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl getDietPlanCacheAnalytics() " + traceId;
		logger.info("##### " + method);
		super.getCustomerProfilesForDietPlans(traceId).compose(data -> super.getDietPlanCacheAnalytics(data, traceId))
				.onComplete(resultHandler);

		return this;
	}
	
	/**
	 * Create empty profile.
	 * 
	 * @param request
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService createEmptyProfile(CreateProfileRequest request, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl createEmptyProfile() " + traceId;
		Instant start = Instant.now();
		JsonObject emptyDocument = new JsonObject();
		JsonObject profile = new JsonObject();

		profile.put("email", request.getEmail()).put("family_name", request.getFamily_name()).put("given_name",
				request.getGiven_name());
		profile.put("name", request.getName()).put("login_type", request.getLoginType());
		logger.info("##### " + method + " PROFILE -->> " + profile);
		Calendar calll = Calendar.getInstance(); // creates calendar
		calll.setTime(new Date()); // sets calendar time/date
		calll.add(Calendar.HOUR_OF_DAY, 10); // adds five hour
		calll.add(Calendar.MINUTE, 30); // add 30 minutes
		profile.put("createdDate", new SimpleDateFormat("dd-MMM-yyyy").format(calll.getTime()));
		logger.info("##### " + method + " REACHED FOR EMPTY PROFILE CREATION.");
		profile.put("createdDate", new SimpleDateFormat("dd-MMM-yyyy").format(calll.getTime()));
		profile.put("createdDateTime", new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").format(calll.getTime()));
		emptyDocument.put("profile", profile);
		createEmptyDocument(emptyDocument, traceId).onComplete(h -> {
			if (h.succeeded()) {
				resultHandler.handle(Future.succeededFuture(new JsonObject()));
			}
		});

		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("##### " + method + " CREATE EMPTY  PROFILE [" + timeElapsed + " milliseconds]");

		return this;
	}
	
	/**
	 * Save dietplan time.
	 * 
	 * @param email
	 * @param request
	 * @param uri
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService dietPlanSaveTime(String email, JsonObject request, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl dietPlanSaveTime() " + traceId + "-[" + email + "]";
		logger.info("##### " + method);
		super.logCustRequest(email, uri, traceId).compose(data -> super.fetchCustomerProfile(email, request, traceId))
		.compose(data -> super.updateTime(email, data, traceId)).onComplete(resultHandler);
		
		return this;
	}
	
	/**
	 * Update customer profiles in bulk.
	 * 
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService updateCustomerProfilesInBulk(String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl updateCustomerProfilesInBulk() " + traceId;
		logger.info("##### " + method);

		super.getDefaultSelectiveItemsByCommunity(traceId)
				.compose(data -> super.fetchCustomerProfilesForPendingRegistration(data, traceId))
				.compose(data -> super.updateCustomerProfilesInBulk(data, traceId))
				//.compose(data -> super.removePaymentDetailsAlreadyUpdatedManually(data, traceId))
				.compose(data -> super.updatePaymentDetailsInBulk(data, traceId))
				.compose(data -> super.fetchPaymentDetailsAlreadyUpdatedManually(data, traceId))
				.compose(data -> super.removePaymentDetailsAlreadyUpdatedManually2(data, traceId))
				//.compose(data -> super.removeSubscriptionPlansAlreadyUpdatedManually(data, traceId))
				.compose(data -> super.updateSubscriptionPlansInBulk(data, traceId))
				.compose(data -> super.fetchSubscriptionPlansAlreadyUpdatedManually(data, traceId))
				.compose(data -> super.removeSubscriptionPlansAlreadyUpdatedManually2(data, traceId))
				.onComplete(resultHandler);

		return this;
	}
	
	/**
	 * Update profiles manually in bulk.
	 * 
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService updateProfilesManuallyInBulk(String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl updateProfilesManuallyInBulk() " + traceId;
		logger.info("##### " + method);

		super.fetchCustomerProfilesAlreadyUpdatedManually(traceId)
				.compose(data -> super.removePaymentDetailsAlreadyUpdatedManually(data, traceId))
				.compose(data -> super.updatePaymentDetailsInBulk(data, traceId))
				.compose(data -> super.fetchPaymentDetailsAlreadyUpdatedManually(data, traceId))
				.compose(data -> super.removePaymentDetailsAlreadyUpdatedManually2(data, traceId))
				.compose(data -> super.removeSubscriptionPlansAlreadyUpdatedManually(data, traceId))
				.compose(data -> super.updateSubscriptionPlansInBulk(data, traceId))
				.compose(data -> super.fetchSubscriptionPlansAlreadyUpdatedManually(data, traceId))
				.compose(data -> super.removeSubscriptionPlansAlreadyUpdatedManually2(data, traceId))
				.onComplete(resultHandler);

		return this;
	}
	
	/**
	 * Save dietplans persist if index is greater than 10 (v3)
	 * 
	 * @param json
	 * @param email
	 * @param uri
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService saveDietPlansV3PersistIfIndexIsGreaterThan10(JsonObject json, String email, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl saveDietPlansV3PersistIfIndexIsGreaterThan10() " + traceId + "-["
				+ email + "]";
		logger.info("##### " + method + " EMAIL -->> " + email);
		super.logCustRequest(email, uri, traceId)
				.compose(data -> super.persistDietPlanForBacktrace(json, email, traceId)).onComplete(resultHandler);
		return this;
	}
	
	/**
	 * Save or update customer dietplan timings.
	 * 
	 * @param email
	 * @param request
	 * @param uri
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService saveOrUpdateCustomerDietPlanTimings(String email, JsonObject request, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl saveOrUpdateCustomerDietPlanTimings() " + traceId + "-[" + email + "]";
		logger.info("##### " + method);
		super.logCustRequest(email, uri, traceId)
				.compose(data -> super.fetchCustomerDietPlanTimings(email, new FilterData(), traceId))
				.compose(data -> super.saveOrUpdateCustomerDietPlanTimings(email, data, request, traceId))
				.compose(data -> super.getTodayDietListTimings(email, data, traceId))
				.compose(data -> super.updateDietPlanTimingsInCache(email, data, traceId)).onComplete(resultHandler);

		return this;
	}
	
	/**
	 * Save or update customer dietplan timings [DETOX].
	 * 
	 * @param email
	 * @param request
	 * @param uri
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService saveOrUpdateCustomerDietPlanTimingsDetox(String email, JsonObject request, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl saveOrUpdateCustomerDietPlanTimingsDetox() " + traceId + "-[" + email + "]";
		logger.info("##### " + method);
		super.logCustRequest(email, uri, traceId)
				.compose(data -> super.fetchCustomerDietPlanTimingsDetox(email, new FilterData(), traceId))
				.compose(data -> super.saveOrUpdateCustomerDietPlanTimingsDetox(email, data, request, traceId))
				.compose(data -> super.getTodayDietListTimingsDetox(email, data, traceId))
				.compose(data -> super.updateDietPlanTimingsInCacheDetox(email, data, traceId)).onComplete(resultHandler);

		return this;
	}
	
	/**
	 * Get dietplan [DETOX].
	 * 
	 * @param email
	 * @param date
	 * @param uri
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService getDietPlanByEmailv3Detox(String email, String date, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl getDietPlanByEmailv3Detox() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + " DATE -->> " + date);
		super.logCustRequest(email, uri, traceId)
				.compose(data -> super.getCustDietPrefCode(email, new DBResult(), traceId))
				.compose(data -> super.getCustDietPreferencesFromDB(data, traceId)).onComplete(ha -> {
					super.getCustDietPlanTimingsProfile(email, traceId)
							.compose(data -> super.fetchCustomerDietPlanTimings(email, data, traceId))
							.compose(data -> super.getDefaultProfileForDietPlanTimings(data, traceId))
							.compose(data -> super.getDietListForFood(email, data, traceId))
							.compose(data -> createDietplanDetox(data, ha.result(), date, traceId))
							.onComplete(resultHandler);
				});

		return this;
	}
	
	/**
	 *Get diet list [DETOX]
	 * 
	 * @param email
	 * @param date
	 * @param uri
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService getDietListDetox(String email, String date, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl getDietListDetox() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + "   EMAIL -->> " + email);
		super.getDietPlanCacheForProvidedDateDetox(email, date, traceId).onComplete(resultHandler);
		return this;
	}
	
	/**
	 *Get diet list [DETOX]
	 * 
	 * @param email
	 * @param date
	 * @param uri
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
//	@Override
//	public FightitawayService getDietListDetoxBkp(String email, String date, String uri, String traceId,
//			Handler<AsyncResult<JsonObject>> resultHandler) {
//		String method = "FightitawayServiceImpl getDietListDetox() " + traceId + "-[" + email + "]";
//		logger.info("##### " + method + "   EMAIL -->> " + email);
//		super.logCustRequest(email, uri, traceId)
//				.compose(data -> super.getLastAndFutureWeekDietPlanListDetox(email, new JsonObject(), traceId))
//				.compose(data -> super.getTodayDietPlanCacheDetox(email, data, date, traceId)).onComplete(resultHandler);
//		return this;
//	}
	
	/**
	 * get today diet list [DETOX].
	 * 
	 * @param email
	 * @param uri
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService getTodayDietListDetox(String email, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl getTodayDietListDetox() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + "   EMAIL -->> " + email);
		super.logCustRequest(email, uri, traceId).compose(data -> super.geTodayDietListCacheDetox(email, traceId))
				.onComplete(resultHandler);
		return this;
	}
	
	/**
	 * Save diet plans persist if index is greater than 10 [DETOX].
	 * 
	 * @param json
	 * @param email
	 * @param uri
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService saveDietPlansV3PersistIfIndexIsGreaterThan10Detox(JsonObject json, String email,
			String uri, String traceId, Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl saveDietPlansV3PersistIfIndexIsGreaterThan10Detox() " + traceId + "-["
				+ email + "]";
		logger.info("##### " + method + " EMAIL -->> " + email);
		super.logCustRequest(email, uri, traceId)
				.compose(data -> super.persistDietPlanForBacktraceDetox(json, email, traceId))
				.onComplete(resultHandler);
		return this;
	}
	
	/**
	 * Get dietplan via email for option [DETOX].
	 * 
	 * @param email
	 * @param slot
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService getDietPlanByEmailForOptionDetox(String email, Integer slot, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl getDietPlanByEmailForOptionDetox() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + " EMAIL -->> " + email);
		logger.info("##### " + method + " SLOT -->> " + slot);
		this.getDataForFilterDiets(email, traceId).compose(data -> super.getDietListForFood(email, data, traceId))
				.compose(data -> getDietsForOptionDetox(data, slot, traceId)).onComplete(resultHandler);
		return this;
	}
	
	/**
	 * Update customer diet preferences v2 [DETOX].
	 * 
	 * @param request
	 * @param uri
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService updateCustDietPrefV2Detox(CustDietPreference request, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl updateCustDietPrefV2Detox() " + traceId + "-[" + request.getEmail() + "]";
		logger.info("##### " + method + " REQUEST -->> " + request);
		super.logCustRequest(request.getEmail(), uri, traceId)
				.compose(data -> super.getCustDietPrefV2Detox(request, request.getEmail(), traceId))
				.compose(data -> super.updateDietPreferrenceV2Detox(data, request, traceId)).onComplete(resultHandler);
		return this;
	}
	
	/**
	 * Update dietplan cache data [DETOX].
	 * 
	 * @param email
	 * @param slotId
	 * @param foodCodeList
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService updateCacheDataDetox(String email, Integer slotId, Set<FoodDetail> foodCodeList, String date, 
			String uri, String traceId, Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl updateCacheDataDetox() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + "    SLOTID -->> " + slotId);
		logger.info("##### " + method + " FOOD LIST -->> " + foodCodeList);
		super.logCustRequest(email, uri, traceId)
				.compose(data -> super.getTodayDietListCacheDetox(email, slotId, foodCodeList, date, traceId))
				.onComplete(res -> {
					if (res.succeeded()) {
						super.getDietListForFoodCode(slotId, foodCodeList, traceId)
								.compose(data -> super.createSlotDetail(slotId, data, traceId))
								.compose(data -> super.updateTodayDietPlanCacheDetox(email, slotId, res.result(), data,
										foodCodeList, date, traceId))
								.onComplete(resultHandler);
					} else {
						resultHandler.handle(Future.succeededFuture(new JsonObject().put("code", "0001").put("message",
								"[" + ApiUtils.getSpecificDate(0, date) + "] - dietplan is unavailable.")));
					}
				});

		return this;
	}
	
	/**
	 * get dietplan refresh options [DETOX].
	 * 
	 * @param email
	 * @param slot
	 * @param categoryNamefoodItem
	 * @param uri
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService dietPlanRefreshOptionDetox(String email, Integer slot, String categoryNamefoodItem, String uri, 
			String traceId, FoodDetail foodItem, Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl dietPlanRefreshOptionDetox() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + "     EMAIL -->> " + email);
		logger.info("##### " + method + "      SLOT -->> " + slot);
		logger.info("##### " + method + "  CATEGORY -->> " + categoryNamefoodItem);
		logger.info("##### " + method + " FOOD CODE -->> " + foodItem.getCode());
		Set<FoodDetail> foodList = new HashSet<FoodDetail>();
		foodList.add(foodItem);
		super.geTodayDietListCacheDetox(email, traceId).onComplete(res -> {
			super.logCustRequest(email, uri, traceId).compose(data -> super.getDataForFilterDiets(email, traceId))
					.compose(data -> super.getDietListRefreshFood(email, data, traceId))
					.compose(data -> super.fetchCustomerDietPlanTimingsForOptions(email, data, traceId))
					.compose(data -> getDietsForOptionDetox(data, slot, traceId))
					.compose(data -> getCustAlreadyRefreshedDietItems(email, data, traceId))
					.compose(data -> getDietPlanForRefreshOptionAndFoodCode(email, slot, foodItem, data, traceId))
					.compose(data -> getRefreshedDietItem(slot, data, categoryNamefoodItem, foodItem, email, traceId))
					.compose(data -> super.getDietListForRefreshFoodCode(slot, foodItem, data, traceId))
					.compose(data -> super.createSlotRefreshDetail(slot, data, traceId))
					.compose(data -> super.updateTodayDietPlanCacheRefreshDetox(email, slot, res.result(), data, foodList,
							traceId))
					.onComplete(resultHandler);
		});

		return this;
	}

	@Override
	public FightitawayService fetchnSaveCust(String email, String name, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		super.fetchAdminCust(email, traceId).compose(data -> fetchnSaveCust(email, name, data, traceId))
				.onComplete(resultHandler);
		
		return this;
	}

	@Override
	public FightitawayService fetchPendingCust(String email, String date, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		super.fetchPaymentDetails(email, date, traceId)
				.compose(data -> fetchSubscribedPlans(email, date, data, traceId))
				.compose(data -> fetchCustomers(email, date, data, traceId)).onComplete(resultHandler);

		return this;
	}

	@Override
	public FightitawayService saveDetoxDietplanStatus(String email, boolean isDetox, String date, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		super.fetchTodaysDietListCacheDetox(email, date, traceId).onComplete(ha -> {
			if (ha.succeeded()) {
				super.fetchDetoxDietPlanStatus(email, date, traceId)
						.compose(data -> saveOrUpdateDetoxDietPlanStatus(email, isDetox, data, date, traceId))
						.onComplete(resultHandler);
			} else {
				resultHandler.handle(Future
						.succeededFuture(new JsonObject().put("code", "0001").put("message", "no dietplan available")));
			}
		});

		return this;
	}

	@Override
	public FightitawayService fetchHelp(String email, String traceId, Handler<AsyncResult<JsonObject>> resultHandler) {
		super.fetchHelp(email, traceId).onComplete(resultHandler);
		
		return this;
	}
	
	/**
	 * get customer payment analytics.
	 * 
	 * @param email
	 * @param noOfDays
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService getCustPaymentDetails(String email, Integer noOfDays, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl getCustPaymentDetails() " + traceId;
		logger.info("##### " + method);
		super.getCustPaymentDetails(email, noOfDays, traceId).onComplete(resultHandler);

		return this;
	}
	
	/**
	 * Get dietplan for a week.
	 * 
	 * @param email
	 * @param isDetox
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService fetchDietPlans(String email, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		String method = "FightitawayServiceImpl fetchDietPlans() " + traceId + "-[" + email + "]";
		logger.info("##### " + method + "   EMAIL -->> " + email);
		super.fetchDetoxDietPlanStatusFlagForLastWeek(email, traceId)
				.compose(data -> super.fetchDietPlansDetox(email, data, traceId))
				.compose(data -> super.fetchDietPlans(email, data, traceId)).onComplete(resultHandler);

		return this;
	}
	
	/**
	 * Get dietplan for a week.
	 * 
	 * @param email
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService fetchAllCustomersDetails(String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		logger.info("##### REACHED");
		super.getAllCustomersSubscriptionDetails(traceId)
				.compose(data -> fetchRegisteredCustomersHavingPlanSubscribed(data, traceId))
				.compose(data -> fetchRegisteredCustomersLatestWeightHavingPlanSubscribed(data, traceId))
				.onComplete(resultHandler);

		return this;
	}

	@Override
	public FightitawayService saveDetoxDefaultDays(String email, JsonObject request, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		super.fetchDetoxDefaultDays(email, traceId).onComplete(ha -> {
			if (ha.succeeded()) {
				System.out.println("SAVE CODE TO BE DONE.");
//				super.fetchDetoxDietPlanStatus(email, traceId)
//						.compose(data -> saveOrUpdateDetoxDietPlanStatus(email, isDetox, data, traceId))
//						.onComplete(resultHandler);
			} else {
				System.out.println("UPDATE CODE TO BE DONE.");
			}
		});

		return this;
	}

	@Override
	public FightitawayService updatePaymentDetails(String email, JsonObject request, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {

		if (request.getInteger("amount") > 0)
			super.getProfile(email, new JsonObject(), null, traceId)
			.compose(data -> super.getCustSubscribedDetails(email, data, traceId))
					.compose(data -> super.updatePaymentDetails(email, request, data, traceId))
					.compose(data -> super.sendEmailPostPaymentBySendInBlue(email, data, traceId))
					.onComplete(resultHandler);
		else
			resultHandler.handle(Future.succeededFuture(
					new JsonObject().put("code", "0001").put("message", "amount should be greater than 0.")));

		return this;
	}

	@Override
	public FightitawayService updateTargetWeight(String email, JsonObject request, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {

		super.getCustomerProfile(email, request, traceId).compose(data -> updateCustomerPofile(email, data, traceId))
				.compose(data -> getCustomerTargetedWeight(email, data, traceId))
				.compose(data -> saveOrUpdateCustomerTargetedWeightCounterAndDateTime(email, data, traceId))
				.onComplete(resultHandler);

		return this;
	}

	@Override
	public FightitawayService updateCurrentWeight(String email, JsonObject request, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		super.updateCustomerCurrentWeight(email, request, traceId)
				.compose(data -> super.getCustomerDemographic(email, request, traceId))
				.compose(data -> super.updateCustomerProfilesCurrentWeight(email, request, data, traceId))
				.onComplete(resultHandler);

		return this;
	}

	@Override
	public FightitawayService getCustProfile(String email, String traceId,
			Handler<AsyncResult<Boolean>> resultHandler) {
		super.getCustProfile(email, traceId).onComplete(resultHandler);

		return this;
	}

	@Override
	public FightitawayService saveOrUpdateCustSurvey(String email, JsonObject request, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {

		super.fetchCustSurvey(email, traceId)
				.compose(data -> super.saveOrUpdateCustSurvey(email, data, request, traceId)).onComplete(resultHandler);

		return this;
	}

	@Override
	public FightitawayService updateCustMobile(String email, JsonObject request, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {

		super.fetchProfile(email, request, traceId)
				.compose(data -> super.updateCustMobile(email, data, request, traceId)).onComplete(resultHandler);

		return this;
	}
	
	/**
	 * Fetch dietplan timings for either normal or detox.
	 * 
	 * @param email
	 * @param traceId
	 * @param resultHandler
	 * @return FightitawayService
	 *
	 */
	@Override
	public FightitawayService fetchDietPlanTimings(String email, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		super.fetchDietPlanTimings(email, traceId).onComplete(resultHandler);

		return this;

	}
}