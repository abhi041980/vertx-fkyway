package com.fightitaway.service;

import java.util.List;
import java.util.Set;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Fightitaway service.
 * 
 * @author Smartdietplanner
 * @since 08-Aug-2020
 *
 */
@VertxGen
@ProxyGen
public interface FightitawayService {

	String SERVICE_NAME = "fightitway-service";

	/**
	 * The address on which the service is published
	 */
	String SERVICE_ADDRESS = "fightitway-service.address";

	@Fluent
	public FightitawayService getDefaultDeatil(String traceId, Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService getDefaultDetail(String email, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService createProfile(CreateProfileRequest request, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService getProfile(String email, String traceId, Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService updateDemographic(String email, JsonObject reqiest, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService updateLifeStyle(String email, JsonObject reqiest, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService updateDiet(String email, JsonObject request, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService getDataForCalculation(String email, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService getTest(String email, String traceId,
			Handler<AsyncResult<List<JsonObject>>> resultHandler);

	@Fluent
	public FightitawayService getDietPlanByEmailv2(String email, String date, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService createCustomerDietPlan(String email, CreatCustomerDietPlanRequest request, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService getCustomerDietList(String email, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService createHabitMaster(JsonObject request, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService getHabitMaster(String traceId, Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService createCustomerHabit(String email, CreatCustomerHabitPlanRequest request, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService getCustomerHabitList(String email, String traceId,
			Handler<AsyncResult<List<JsonObject>>> resultHandler);

	@Fluent
	public FightitawayService updateCustomerHabit(String email, String traceId, CreatCustomerHabitPlanRequest request,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService getCouponListFromMaster(String traceId, Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService getCustomerHabitForUpdate(String email, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService deleteCustomerHabit(String email, String code, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService getCustomerWeightGraphData(String email, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService updateCustomerWeight(String email, Double weight, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService getLatestWeight(String email, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService getDietPlanByEmailForOption(String email, Integer slot, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService updateCustDietPref(CustDietPreference request, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService getCustDietPref(String email, String traceId,
			Handler<AsyncResult<JsonArray>> resultHandler);

	@Fluent
	public FightitawayService removePlan(String email, String traceId, Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService getCustomerPrefList(String email, String traceId,
			Handler<AsyncResult<List<JsonObject>>> resultHandler);

	@Fluent
	public FightitawayService getDietByCode(String code, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService getDietPlanByEmailv3(String email, String date, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService getDietPlanBySlot(String email, int slot, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService getTodayDietList(String email, String date, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService updateTodaDietPlan(String email, Integer slotId, JsonObject slotDetail, String date,
			String traceId, Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService updateCacheData(String email, Integer slotId, Set<FoodDetail> foodCodeList, String date,
			String uri, String traceId, Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService updateCustDietPrefV2(CustDietPreference request, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService getCustDietDetailsOnEdit(String email, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService getCustDietPlanTimingsProfile(String email, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService sendEmail(String email, JsonObject request, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService getDashboard(String traceId, Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService getDietPlanCachedata(String traceId, Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService getDietPlanByEmailv3Config(String email, String date, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService addTnC(JsonObject request, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService profileRemove(String email, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService getOnePlan(String email, String traceId, Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService subscribePlanByCoupon(String email, String couponCode, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService fetchFood(String email, String foodId, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService saveCustCaloriesBurnt(String email, Double calBurnt, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService waterTips(String traceId, Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService saveWaterDrank(String email, Integer WaterQuantity, String dateTime, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService waterRecommendation(String email, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService custWaterReminder(String email, String waterReminderStatus, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService fetchWaterReminder(String email, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService getSpecificDetailsByCommunity(String email, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService fetchCaloriesHistory(String email, Integer noOfDays, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService fetchTargetCalories(String email, Integer noOfDays, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService dietPlanRefreshOption(String email, Integer slotId, String categoryNamefoodItem,
			String date, String uri, String traceId, FoodDetail foodItem,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService getCustAnalytics(String traceId, Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService refreshFoods(String email, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService getCustPaymentAnalytics(Integer noOfDays, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService getDietPlanCacheAnalytics(String traceId, Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService createEmptyProfile(CreateProfileRequest request, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService dietPlanSaveTime(String email, JsonObject request, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService updateCustomerProfilesInBulk(String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService updateProfilesManuallyInBulk(String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService saveDietPlansV3PersistIfIndexIsGreaterThan10(JsonObject jsonObject, String email,
			String uri, String traceId, Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService saveOrUpdateCustomerDietPlanTimings(String email, JsonObject request, String uri,
			String traceId, Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService saveOrUpdateCustomerDietPlanTimingsDetox(String email, JsonObject request, String uri,
			String traceId, Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService getDietPlanByEmailv3Detox(String email, String date, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService getDietListDetox(String email, String date, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService saveDietPlansV3PersistIfIndexIsGreaterThan10Detox(JsonObject jsonObject, String email,
			String uri, String traceId, Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService getDietPlanByEmailForOptionDetox(String email, Integer slot, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService dietPlanRefreshOptionDetox(String email, Integer slotId, String categoryNamefoodItem,
			String uri, String traceId, FoodDetail foodItem, Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService getTodayDietListDetox(String email, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService updateCustDietPrefV2Detox(CustDietPreference request, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService updateCacheDataDetox(String email, Integer slotId, Set<FoodDetail> foodCodeList,
			String date, String uri, String traceId, Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService fetchnSaveCust(String email, String name, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService fetchPendingCust(String email, String date, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService saveDetoxDietplanStatus(String email, boolean isDetox, String date, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService fetchHelp(String email, String traceId, Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService getCustPaymentDetails(String email, Integer noOfDays, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService fetchDietPlans(String email, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService fetchAllCustomersDetails(String traceId, Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService saveDetoxDefaultDays(String email, JsonObject request, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService updatePaymentDetails(String email, JsonObject request, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService updateTargetWeight(String email, JsonObject request, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService updateCurrentWeight(String email, JsonObject request, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService getCustProfile(String email, String traceId,
			Handler<AsyncResult<Boolean>> resultHandler);

	@Fluent
	public FightitawayService saveOrUpdateCustSurvey(String email, JsonObject request, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService updateCustMobile(String email, JsonObject request, String uri, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);

	@Fluent
	public FightitawayService fetchDietPlanTimings(String email, String traceId,
			Handler<AsyncResult<JsonObject>> resultHandler);
}
