package com.fightitaway.api;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import com.fightitaway.common.Constants;
import com.fightitaway.common.ListUtils;
import com.fightitaway.common.MongoRepositoryWrapper;
import com.fightitaway.service.DBResult;
import com.fightitaway.service.FilterData;
import com.fightitaway.service.SlotFilter;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Food filters activities.
 * 
 * @author Smartdietplanner
 * @since 08-Aug-2020
 *
 */
public class FoodFilterUtils {
	
	protected static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MongoRepositoryWrapper.class);

	public static List<JsonObject> filterByDiseaseRecommendedIn(List<JsonObject> data, List<String> disease) {
		data = data.stream().filter(x -> {
			JsonArray recommendedIn = x.getJsonArray("RecommendedIn");
			if (recommendedIn != null) {
				for (String d : disease) {
					if (recommendedIn.contains(d)) {
						return true;
					}
				}
			} else {
				return false;
			}
			return false;
		}).collect(Collectors.toList());

		return data;
	}
	
	public static List<JsonObject> sortByScore(List<JsonObject> data) {
		if (data == null) {
			return new ArrayList<JsonObject>();
		}
		List<JsonObject> filterList = data.stream()
				.sorted((p1, p2) -> p1.getDouble("Calories").compareTo(p2.getDouble("Calories")))
				.collect(Collectors.toList());
		return filterList;
	}
	
	public static List<JsonObject> sortByDietScore(List<JsonObject> data) {
		if (null == data || (null != data && data.size() <= 0))
			return new ArrayList<JsonObject>();

		List<JsonObject> filterList = data.stream()
				.sorted((p1, p2) -> p1.getDouble("Score").compareTo(p2.getDouble("Score")))
				.collect(Collectors.toList());
		Collections.reverse(filterList);
		
		return filterList;
	}
	
	public static List<JsonObject> sortByCalories(List<JsonObject> data) {
		if (null == data || (null != data && data.size() <= 0))
			return new ArrayList<JsonObject>();

		List<JsonObject> filterList = data.stream()
				.sorted((p1, p2) -> p1.getDouble("Calories").compareTo(p2.getDouble("Calories")))
				.collect(Collectors.toList());
		//Collections.reverse(filterList);
		
		return filterList;
	}public static List<JsonObject> sortByCaloriesAsc(List<JsonObject> data) {
		if (null == data || (null != data && data.size() <= 0))
			return new ArrayList<JsonObject>();

		List<JsonObject> filterList = data.stream()
				.sorted((p1, p2) -> p1.getDouble("Calories").compareTo(p2.getDouble("Calories")))
				.collect(Collectors.toList());
		//Collections.reverse(filterList);
		
		return filterList;
	}
	
	
	public static JsonObject geFilteredData(List<JsonObject> data) {
		if (data == null)
			return null;
		List<JsonObject> filterList = data.stream().collect(Collectors.toList());
		if (filterList != null && !filterList.isEmpty())
			Collections.shuffle(filterList);

		return filterList.size() > 0 ? filterList.get(0) : null;
	}
	
	public static JsonObject geFilteredData(List<JsonObject> data, List<JsonObject> addedList) {

		if (data == null)
			return null;

		data = data.stream().filter(x -> {

			for (JsonObject jsonObject : addedList)
				if (x.getString("code").equals(jsonObject.getString("code")))
					return false;

			return true;
		}).collect(Collectors.toList());
		
		logger.info("##### FoodFilterUtils geFilteredData() DATA -->> " + data);
		List<JsonObject> filterList = data.stream().collect(Collectors.toList());
		
		
		Integer randomNo = 0;
		if (filterList != null && !filterList.isEmpty()) {
			Collections.shuffle(filterList);
			randomNo = new Random().nextInt(filterList.size() - 1);
		}

		return filterList.size() > 0 ? filterList.get(randomNo) : null;
	}
	
	public static JsonObject getFinalData(List<JsonObject> data) {

		if (data == null)
			return null;

//		List<JsonObject> filterList = data.stream().collect(Collectors.toList());
		Integer randomNo = 0;
		if (data != null && !data.isEmpty()) {
			Collections.shuffle(data);
			if (data.size() > 1)
				randomNo = new Random().nextInt(data.size() - 1);
		}

		return data.size() > 0 ? data.get(randomNo) : null;
	}
	
	public static void addItem(JsonObject x,List<JsonObject> planlist) {
		for (JsonObject jsonObject : planlist) {
			if(jsonObject.getString("code").equalsIgnoreCase(x.getString("code"))) {
				return ;
			}
		}
		
		planlist.add(x);
	}
	
	public static boolean geFilteredItemChecked(JsonObject x, List<JsonObject> addedList) {
		boolean isItemFound = false;
		for (int i = 0; i < addedList.size(); ++i)
			if (null != x && x.getString("code").equalsIgnoreCase(addedList.get(i).getString("code"))) {
				isItemFound = true;
				break;
			}

		return isItemFound;
	}
	
	public static JsonObject getItemAfterShuffle(List<JsonObject> data) {

		logger.info("##### FoodFilterUtils getItemAfterShuffle() SHUFFLING THE COLLECTION -->> " + data.size());
		if (data != null && !data.isEmpty())
			Collections.shuffle(data);

		return data.size() > 0 ? data.get(0) : null;
	}
	
	public static JsonObject getTeaOrCoffeeItems1(List<JsonObject> filteredData, boolean isSouthIndian,
			List<JsonObject> masterList) {
		String[] teaCoffeeItems = { "060", "061", "063" };
		List<String> list = new ArrayList<String>();
		for (JsonObject json : filteredData)
			list.add(json.getString("itemCode"));
		List<String> notAvailableTeaCoffeeList = new ArrayList<String>();
		for (String itemCode : Arrays.asList(teaCoffeeItems))
			if (!list.contains(itemCode))
				notAvailableTeaCoffeeList.add(itemCode);

		if (null == notAvailableTeaCoffeeList || notAvailableTeaCoffeeList.size() <= 0) {
			for (String itemCode : Arrays.asList(teaCoffeeItems)) {
				if (isSouthIndian) {
					if ("063".equalsIgnoreCase(itemCode))
						filteredData.add(getMealByCode(itemCode, masterList));
				} else {
					if (!"063".equalsIgnoreCase(itemCode))
						filteredData.add(getMealByCode(itemCode, masterList));
				}
			}
		} else {
			for (String itemCode : notAvailableTeaCoffeeList) {
				if (isSouthIndian) {
					if ("063".equalsIgnoreCase(itemCode))
						filteredData.add(getMealByCode(itemCode, masterList));
				} else {
					if (!"063".equalsIgnoreCase(itemCode))
						filteredData.add(getMealByCode(itemCode, masterList));
				}
			}
		}

		if (filteredData != null && !filteredData.isEmpty())
			Collections.shuffle(filteredData);

		return filteredData.size() > 0 ? filteredData.get(0) : null;
	}
	
	public static JsonObject getTeaOrCoffeeItems(List<JsonObject> filteredData, boolean isSouthIndian,
			List<JsonObject> masterList) {

		logger.info("##### FoodFilterItils getTeaOrCoffeeItems() SLOTS 2 FILTERED DATA SIZE (BEFORE) -->> "
				+ filteredData.size());
		filteredData = filteredData.stream().filter(x -> !x.getString("Type").equalsIgnoreCase("DM"))
				.collect(Collectors.toList());
		logger.info("##### FoodFilterItils geTeaOrCoffeeItems() SLOTS 2 FILTERED DATA SIZE (BEFORE) -->> "
				+ filteredData.size());
		String[] teaCoffeeItems = { "060", "061", "063" };

		filteredData.add(getMealByCode(teaCoffeeItems[0], masterList));
		filteredData.add(getMealByCode(teaCoffeeItems[1], masterList));
		if (isSouthIndian)
			filteredData.add(getMealByCode(teaCoffeeItems[2], masterList));
		if (filteredData != null && !filteredData.isEmpty())
			Collections.shuffle(filteredData);

		return filteredData.size() > 0 ? filteredData.get(0) : null;
	}
	
	public static JsonObject geFilteredItems(List<JsonObject> data, List<JsonObject> addedList) {
		if (data == null)
			return null;

		List<JsonObject> filterList = data.stream().collect(Collectors.toList());
		List<JsonObject> dataList = data.stream().collect(Collectors.toList());
		String[] teaCoffeeItems = { "060", "061", "063" };

		for (JsonObject dataObj : data)
			if (Arrays.asList(teaCoffeeItems).contains(dataObj.getString("code")))
				dataList.add(dataObj);

		if (null == dataList || dataList.size() <= 0)
			for (JsonObject jsonObject : addedList)
				if (Arrays.asList(teaCoffeeItems).contains(jsonObject.getString("code")))
					filterList.add(jsonObject);

		if (filterList != null && !filterList.isEmpty())
			Collections.shuffle(filterList);

		return filterList.size() > 0 ? filterList.get(0) : null;
	}
	
	public static List<JsonObject> getTop5(List<JsonObject> list) {
		if (list.size() >= 5)
			return list.subList(0, 5);
		
		return list;
	}
	
	
	public static List<JsonObject> getSlot0(List<JsonObject> datas, DBResult dbResult, List<String> disease,
			List<String> communities, String traceId) {
		String method = "FoodFilterUtils getSlot0() " + traceId + "-[" + dbResult.getFilterData().getEmail() + "]";
		logger.info("#########################################################################################");
		logger.info("");
		List<JsonObject> result = new ArrayList<JsonObject>();
		datas = datas.stream().filter(x -> getDietBySlot(x, 0)).filter(x -> filterByDietSeason(x))
				.collect(Collectors.toList());
		datas = datas.stream().filter(x -> filterAvoidIn(x, disease))
				.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
				.collect(Collectors.toList());
		datas = filterByCustCommunity(datas, dbResult.getFilterData().getCommunity(), 0, "");
		JsonObject drinkObj = null;

		/////////////////////////////////////////////////////////////////////////////////////
		// DATAS
		// RECOMMENDED-IN
		List<JsonObject> allList = new ArrayList<>();
		boolean isRecommendedItemFound = false;
		logger.info("##### " + method + " [SLOT 0] DISEASE -->> " + disease);
		if (null != disease && disease.size() > 0) {
			List<JsonObject> slot0SpecialSlotsRecommendedIn = datas.stream().filter(x -> getFruits(x))
					.filter(x -> filterSpecialItem(x, 0)).collect(Collectors.toList());
			if (null == slot0SpecialSlotsRecommendedIn
					|| (null != slot0SpecialSlotsRecommendedIn && slot0SpecialSlotsRecommendedIn.size() <= 0))
				slot0SpecialSlotsRecommendedIn = datas.stream().filter(x -> getFruits(x)).collect(Collectors.toList());

			slot0SpecialSlotsRecommendedIn.forEach(action -> {
				JsonArray jsonArr = action.getJsonArray("RecommendedIn");
				List<String> listAll = new ArrayList<>();
				if (null != action && null != jsonArr) {
					jsonArr.forEach(item -> {
						if (null != item) {
							String food = (String) item;
							if (disease.contains(food))
								listAll.add(food);
						}
					});

					if (null != listAll && listAll.size() >= 1)
						allList.add(action);
				}
			});

			if (null != allList && allList.size() > 0) {
				Collections.shuffle(allList);
				result.add(allList.get(0));
				isRecommendedItemFound = true;
			}
		}
		/////////////////////////////////////////////////////////////////////////////////////

		if (!isRecommendedItemFound) {
			if (disease != null) {
				List<JsonObject> diseaseDiets = FoodFilterUtils.filterByDiseaseRecommendedIn(datas, disease);
				// DRINKS
				List<JsonObject> drink = diseaseDiets.stream().filter(x -> x.getString("Type").equalsIgnoreCase("D"))
						.filter(x -> filterByDietSeason(x)).collect(Collectors.toList());
				if (null != drink && drink.size() > 0) {
					Collections.shuffle(drink);
					drinkObj = drink.get(0);
					if (null != drinkObj)
						result.add(drinkObj);
				}
				// DISHES
				List<JsonObject> dishes = diseaseDiets.stream().filter(x -> x.getString("Type").equalsIgnoreCase("A"))
						.filter(x -> filterByDietSeason(x)).filter(x -> filterByDietSeason(x))
						.collect(Collectors.toList());

				if (null != dishes && dishes.size() > 0) {
					Collections.shuffle(dishes);
					JsonObject jsonObject = dishes.get(0);
					if (null != jsonObject)
						result.add(jsonObject);
				}
			} else {
				List<JsonObject> drinks = datas.stream().filter(x -> x.getString("Type").equalsIgnoreCase("D"))
						.filter(x -> filterByDietSeason(x)).collect(Collectors.toList());
				if (null != drinks && drinks.size() > 0) {
					Collections.shuffle(drinks);
					JsonObject jsonObject = drinks.get(0);
					result.add(jsonObject);
				}
			}

			if (disease != null && drinkObj == null) {
				List<JsonObject> drinks = datas.stream().filter(x -> x.getString("Type").equalsIgnoreCase("D"))
						.filter(x -> filterByDietSeason(x)).collect(Collectors.toList());
				if (null != drinks && drinks.size() > 0) {
					Collections.shuffle(drinks);
					JsonObject jsonObject = drinks.get(0);
					result.add(jsonObject);
				}
			}
		}
		result = result.stream().map(x -> {
			updateCalories(x, x.getDouble("portion"));
			return x;
		}).collect(Collectors.toList());

//		addedList.addAll(result);

		result.forEach(res -> logger.info("##### " + method + " [SLOT 0] FINAL RESULT DATA -->> " + res));
		logger.info("#########################################################################################");
		logger.info("");
		return result;
	}

	public static List<JsonObject> getSlot1(List<JsonObject> datas, DBResult dbResult, List<String> disease,
			List<String> communities, List<JsonObject> selectedOptions, String traceId) {
		String method = "FoodFilterUtils getSlot1() " + traceId + "-[" + dbResult.getFilterData().getEmail() + "]";
		logger.info("#########################################################################################");
		logger.info("");
		List<JsonObject> result = new ArrayList<JsonObject>();
		datas = datas.stream().filter(x -> getDietBySlot(x, 1)).filter(x -> filterAvoidIn(x, disease))
				.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
				.filter(x -> filterByDietSeason(x)).collect(Collectors.toList());
		if (null == selectedOptions)
			selectedOptions = new ArrayList<JsonObject>();
		else
			selectedOptions = selectedOptions.stream().filter(x -> getDietBySlot(x, 1))
					.filter(x -> filterByDietSeason(x)).collect(Collectors.toList());
		selectedOptions = filterByCustCommunity(selectedOptions, dbResult.getFilterData().getCommunity(), 1,
				" NON SPECIAL SLOT ");

		/////////////////////////////////////////////////////////////////////////////////////
		// DATAS
		// RECOMMENDED-IN
		List<JsonObject> diabetesList = new ArrayList<>();
		List<JsonObject> otherOptions = new ArrayList<>();
		boolean isRecommendedItemFound = false;
		logger.info("##### " + method + " [SLOT 1] DISEASE -->> " + disease);
		if (null != disease && disease.size() > 0) {
			List<JsonObject> slot1ListRecommendedIn = datas.stream().filter(x -> getFruits(x))
					.filter(x -> filterSpecialItem(x, 1)).collect(Collectors.toList());
			if (null == slot1ListRecommendedIn
					|| (null != slot1ListRecommendedIn && slot1ListRecommendedIn.size() <= 0))
				slot1ListRecommendedIn = datas.stream().filter(x -> getFruits(x)).collect(Collectors.toList());

			slot1ListRecommendedIn.forEach(action -> {
				boolean isDItemsToBeRecommended = false;
				if (disease.contains("D"))
					isDItemsToBeRecommended = true;

				JsonArray jsonArr = action.getJsonArray("RecommendedIn");
				List<String> listD = new ArrayList<>();
				List<String> listOther = new ArrayList<>();
				if (null != action && null != jsonArr) {
					jsonArr.forEach(item -> {
						if (null != item) {
							String food = (String) item;
							if ("D".equalsIgnoreCase(food))
								listD.add(food);
							else
								listOther.add(food);
						}
					});

					if (isDItemsToBeRecommended && null != listD && listD.size() >= 1) {
						diabetesList.add(action);
					} else {
						listOther.forEach(item -> {
							String food = (String) item;
							if (disease.contains(food))
								otherOptions.add(action);
						});
					}
				}
			});

			if (null != diabetesList && diabetesList.size() > 0) {
				Collections.shuffle(diabetesList);
				result.add(diabetesList.get(0));
				isRecommendedItemFound = true;
			} else if (null != otherOptions && otherOptions.size() > 0) {
				Collections.shuffle(otherOptions);
				result.add(otherOptions.get(0));
				isRecommendedItemFound = true;
			}

			// SELECTEDOPTIONS
			if (!isRecommendedItemFound) {
				List<JsonObject> selectedOptionsRecommendedIn = selectedOptions.stream().filter(x -> getFruits(x))
						.filter(x -> filterSpecialItem(x, 1)).collect(Collectors.toList());
				if (null == selectedOptionsRecommendedIn
						|| (null != selectedOptionsRecommendedIn && selectedOptionsRecommendedIn.size() <= 0))
					selectedOptionsRecommendedIn = selectedOptions.stream().filter(x -> getFruits(x))
							.collect(Collectors.toList());

				selectedOptionsRecommendedIn.forEach(action -> {
					boolean isDItemsToBeRecommended = false;
					if (disease.contains("D"))
						isDItemsToBeRecommended = true;
					JsonArray jsonArr = action.getJsonArray("RecommendedIn");
					List<String> listD = new ArrayList<>();
					List<String> listOther = new ArrayList<>();
					if (null != action && null != jsonArr) {
						jsonArr.forEach(item -> {
							if (null != item) {
								String food = (String) item;
								if ("D".equalsIgnoreCase(food))
									listD.add(food);
								else
									listOther.add(food);
							}
						});

						if (isDItemsToBeRecommended && null != listD && listD.size() >= 1) {
							diabetesList.add(action);
						} else {
							listOther.forEach(item -> {
								String food = (String) item;
								if (disease.contains(food))
									otherOptions.add(action);
							});

						}
					}
				});

				if (null != diabetesList && diabetesList.size() > 0) {
					Collections.shuffle(diabetesList);
					result.add(diabetesList.get(0));
					isRecommendedItemFound = true;
				} else if (null != otherOptions && otherOptions.size() > 0) {
					Collections.shuffle(otherOptions);
					result.add(otherOptions.get(0));
					isRecommendedItemFound = true;
				}
			}
		}
		/////////////////////////////////////////////////////////////////////////////////////

		if (!isRecommendedItemFound) {
			List<JsonObject> fruites = datas.stream().filter(x -> filterSpecialItem(x, 1)).collect(Collectors.toList());
			fruites = filterByCustCommunity(fruites, dbResult.getFilterData().getCommunity(), 1, " SPECIAL SLOT ");
			if (ListUtils.isNotEmpty(fruites)) {
//				result.add(FoodFilterUtils.geFilteredData(fruites, addedList));
				result.add(FoodFilterUtils.getFinalData(fruites));
			} else {
				if (null != selectedOptions) {
//					result.add(FoodFilterUtils.geFilteredData(selectedOptions, addedList));
					result.add(FoodFilterUtils.getFinalData(selectedOptions));
				}
			}
		}
		result = result.stream().map(x -> {
			updateCalories(x, x.getDouble("portion"));
			return x;
		}).collect(Collectors.toList());

//		addedList.addAll(result);
		result.forEach(res -> logger.info("##### " + method + " [SLOT 1] FINAL RESULT -->> " + res));
		logger.info("#########################################################################################");
		logger.info("");
		return result;
	}
	
	public static List<JsonObject> getSlot2(Map<String, String> dietMap, List<JsonObject> datas, DBResult dbResult, List<String> disease,
			List<JsonObject> selectedOptions, FilterData filterData, String traceId) {
		String method = "FoodFilterUtils getSlot2() " + traceId + "-[" + dbResult.getFilterData().getEmail() + "]";
		logger.info("#########################################################################################");
		logger.info("");
		List<JsonObject> result = new ArrayList<JsonObject>();
		List<JsonObject> list = new ArrayList<JsonObject>();
		list.add(getMealByCode("112", datas)); // DM
		list.add(getMealByCode("371", datas)); // DM
		
		// THIS CONDITION PUNEET SIR HAS ASKED TO DO ON Jan 24 '2021
		if (dbResult.getFilterData().getCalories() > 1300)
			result.add(getFinalData(list));
		List<JsonObject> allW = new ArrayList<JsonObject>();
		JsonObject filterObject = new JsonObject();
		if (null == selectedOptions) {
			selectedOptions = new ArrayList<JsonObject>();
			List<JsonObject> datas1 = datas.stream().filter(x -> getDietBySlot(x, 2))
					.filter(x -> filterAvoidIn(x, disease))
					.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
					.filter(x -> filterByDietSeason(x)).collect(Collectors.toList());
			allW = datas1.stream().filter(x -> getSnacks(x)).collect(Collectors.toList());
			allW = filterByCustCommunity(allW, dbResult.getFilterData().getCommunity(), 2, "");
//			filterObject = FoodFilterUtils.geFilteredData(allW, datas1);
			filterObject = FoodFilterUtils.getFinalData(allW);
		} else {
			selectedOptions = selectedOptions.stream().filter(x -> getDietBySlot(x, 2)).collect(Collectors.toList());
			allW = selectedOptions.stream().filter(x -> getSnacks(x)).collect(Collectors.toList());
			allW = filterByCustCommunity(allW, dbResult.getFilterData().getCommunity(), 2, "");
//			filterObject = FoodFilterUtils.geFilteredData(allW, addedList);
			filterObject = FoodFilterUtils.getFinalData(allW);
		}
		if (null == filterObject) {
			selectedOptions = new ArrayList<JsonObject>();
			List<JsonObject> datas1 = datas.stream().filter(x -> getDietBySlot(x, 2))
					.filter(x -> filterAvoidIn(x, disease))
					.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
					.filter(x -> filterByDietSeason(x)).collect(Collectors.toList());
			allW = datas1.stream().filter(x -> getSnacks(x)).collect(Collectors.toList());
			allW = filterByCustCommunity(allW, dbResult.getFilterData().getCommunity(), 2, "");
//			filterObject = FoodFilterUtils.geFilteredData(allW, datas1);
			filterObject = FoodFilterUtils.getFinalData(allW);
		}

		if (null != filterObject) {
			if ("WP".equalsIgnoreCase(filterObject.getString("Type"))
					|| "WPP".equalsIgnoreCase(filterObject.getString("Type"))) {
				result.clear();
				result.add(getMealByCode("165", datas)); // CURD
				logger.info("##### " + method + " [SLOT 2] - CURD (165) ADDED");
			} else if ("WC".equalsIgnoreCase(filterObject.getString("Type"))
					|| "WCP".equalsIgnoreCase(filterObject.getString("Type"))) {
				filterObject.put("portion", 2);
				result.add(getMealByCode("055", datas)); // GREEN CHATNI
				logger.info("##### " + method + " [SLOT 2] - GREEN CHATNI (055) ADDED");
			} else if ("WE".equalsIgnoreCase(filterObject.getString("Type"))) {
				JsonObject chapati = getMealByCode("008", datas); // CHAPATI
				result.add(chapati);
				logger.info("##### " + method + " [SLOT 2] - CHAPATI (008) ADDED");
			} else if ("WM".equalsIgnoreCase(filterObject.getString("Type"))
					&& null != result && result.size() > 0 && "DM".equalsIgnoreCase(result.get(0).getString("Type"))) {
				result.clear();
			}

			result.add(filterObject);
//			addedList.addAll(result);

			result = result.stream().map(x -> {
				updateCalories(x, x.getDouble("portion"));
				return x;
			}).collect(Collectors.toList());

			result.forEach(res -> logger.info("##### " + method + " [SLOT 2] FINAL RESULT -->> " + res));
		}
		
		logger.info("#########################################################################################");
		logger.info("");
		return result;
	}
	
	public static List<JsonObject> getSlot3(List<JsonObject> datas, DBResult dbResult, List<String> disease,
			List<JsonObject> selectedOptions, String traceId) {
		String method = "FoodFilterUtils getSlot3() " + traceId + "-[" + dbResult.getFilterData().getEmail() + "]";
		List<JsonObject> result = new ArrayList<JsonObject>();
		logger.info("#########################################################################################");
		logger.info("");

		List<JsonObject> datas1 = datas.stream().filter(x -> getDietBySlot(x, 3)).filter(x -> filterAvoidIn(x, disease))
				.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
				.filter(x -> filterByDietSeason(x)).collect(Collectors.toList());

		// 'W' ITEMS
		List<JsonObject> allWs = datas1.stream().filter(x -> getSnacks(x)).collect(Collectors.toList());
		allWs = filterByCustCommunity(allWs, dbResult.getFilterData().getCommunity(), 3, "ALLWs");
		JsonObject oneWItem = getSpecialSlotItemOnly(3, allWs, dbResult);
		if (null != oneWItem) {
			result.add(oneWItem);
		} else {
			if (null != selectedOptions) {
				datas1 = selectedOptions.stream().filter(x -> getDietBySlot(x, 3))
						.filter(x -> filterAvoidIn(x, disease))
						.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
						.filter(x -> filterByDietSeason(x)).collect(Collectors.toList());
				allWs = datas1.stream().filter(x -> getSnacks(x)).collect(Collectors.toList());
				allWs = filterByCustCommunity(allWs, dbResult.getFilterData().getCommunity(), 3, "ALLWs");
				oneWItem = getSpecialSlotItemOnly(3, allWs, dbResult);
				if (null != oneWItem)
					result.add(oneWItem);
			}
		}

		// 'D' ITEMS
		List<JsonObject> allDs = datas.stream().filter(x -> getDietBySlot(x, 3)).filter(x -> filterAvoidIn(x, disease))
				.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
				.filter(x -> filterByDietSeason(x)).filter(x -> getDrinks(x)).collect(Collectors.toList());
		allDs = filterByCustCommunity(allDs, dbResult.getFilterData().getCommunity(), 3, "ALLDs");
		JsonObject oneDItem = getSpecialSlotItemOnly(3, allDs, dbResult);
		if (null != oneDItem) {
			result.add(oneDItem);
		} else {
			if (null != selectedOptions) {
				datas1 = selectedOptions.stream().filter(x -> getDietBySlot(x, 3))
						.filter(x -> filterAvoidIn(x, disease))
						.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
						.filter(x -> filterByDietSeason(x)).collect(Collectors.toList());
				allDs = datas1.stream().filter(x -> getDrinks(x)).collect(Collectors.toList());
				allDs = filterByCustCommunity(allDs, dbResult.getFilterData().getCommunity(), 3, "ALLDs");
				oneDItem = getSpecialSlotItemOnly(3, allDs, dbResult);
				if (null != oneDItem)
					result.add(oneWItem);
			}
		}

		if (null != result && result.size() > 0)
			result = result.stream().map(x -> {
				if (null != x && null != x.getDouble("portion") && 0 != x.getDouble("portion")
						&& 0.0 != x.getDouble("portion"))
					updateCalories(x, x.getDouble("portion"));
				return x;
			}).collect(Collectors.toList());

		result.forEach(res -> logger.info("#####" + method + " [SLOT 3] FINAL RESULT -->> " + res));

		logger.info("#########################################################################################");
		logger.info("");
		return result;
	}
	
	public static List<JsonObject> getSlot3Old(List<JsonObject> datas, DBResult dbResult, List<String> disease,
			List<JsonObject> selectedOptions, List<JsonObject> addedList, String traceId) {
		String method = "FoodFilterUtils getSlot3() " + traceId + "-[" + dbResult.getFilterData().getEmail() + "]";
		List<JsonObject> result = new ArrayList<JsonObject>();
		logger.info("#########################################################################################");
		logger.info("");
		/**
		 * 
		 * datas=datas.stream().filter(x->getDietBySlot(x,
		 * 3)).collect(Collectors.toList());
		 * selectedOptions=selectedOptions.stream().filter(x->getDietBySlot(x,
		 * 3)).collect(Collectors.toList()); List<JsonObject> slot3ListUltra =
		 * datas.stream().filter(x-> filterUltraItem(x,
		 * 3)).collect(Collectors.toList()); if(ListUtils.isNotEmpty(slot3ListUltra)) {
		 * result.add(FoodFilterUtils.geFilteredData(slot3ListUltra,addedList)); }else {
		 * List<JsonObject> slot3List = datas.stream().filter(x-> filterSpecialItem(x,
		 * 3)).collect(Collectors.toList()); if(ListUtils.isNotEmpty(slot3List)) {
		 * result.add(FoodFilterUtils.geFilteredData(slot3List,addedList)); }else {
		 * result.add(FoodFilterUtils.geFilteredData(selectedOptions,addedList)); } }
		 */

		// result=datas.stream().filter(x->getDietBySlot(x, 3)).filter(x
		// ->filterAvoidIn(x, disease)).filter(x->filterByCustFoodType(x,
		// dbResult.getFilterData().getFoodType())).collect(Collectors.toList());
		// result = filterByCustCommunity(result,
		// dbResult.getFilterData().getCommunity(), 2, "");

		String[] recF = { "033", "037" };
		//int index = ThreadLocalRandom.current().nextInt(recF.length);
		
		//result.add(getMealByCode(recF[index], datas));
		JsonObject meal = getMealByCode(recF[0], datas);
		if (null != meal) {
			result.add(meal);
		} else {
			meal = getMealByCode(recF[1], datas);
			if (null != meal)
				result.add(meal);
		}
		
		result.add(getMealByCode("032", datas));

		result = result.stream().map(x -> {
			updateCalories(x, x.getDouble("portion"));
			return x;
		}).collect(Collectors.toList());

		result.forEach(res -> logger.info("#####" + method + " [SLOT 3] FINAL RESULT -->> " + res));

		logger.info("#########################################################################################");
		logger.info("");
		return result;
	}
	
	/**
	 * Add Nuts (032) and Chach (033) in Ultra special,
	 * @param datas
	 * @param disease
	 * @param selectedOptions
	 * @param addedList
	 * @return
	 */
//	public static List<JsonObject> getSlot3OLD(List<JsonObject> datas, DBResult dbResult, List<String> disease,
//			List<JsonObject> selectedOptions, List<JsonObject> addedList, String traceId) {
//		String method = "FoodFilterUtils getSlot3() " + traceId + "-[" + dbResult.getFilterData().getEmail() + "]";
//		List<JsonObject> result = new ArrayList<JsonObject>();
//		logger.debug("#########################################################################################");
//		logger.debug("");
//		selectedOptions.forEach(res -> logger.debug("#####" + method + " [SLOT 3] SELECTED OPTIONS -->> "
//				+ res.getString("itemCode") + "---" + res.getString("Type")));
//		
//		Map<String, JsonObject> map = new HashMap<>();
//		datas.forEach(item -> {
//			map.put(item.getString("itemCode"), item);
//		});
//		
//		
//		logger.info("##### " + method + " [SLOT 3] - SELECTEDOPTIONS FIRST -->> " + selectedOptions);
//		logger.debug("##### " + method + " [SLOT 3] - DATAS -->> " + datas);
//		/**
//		 * 
//		 * datas=datas.stream().filter(x->getDietBySlot(x,
//		 * 3)).collect(Collectors.toList());
//		 * selectedOptions=selectedOptions.stream().filter(x->getDietBySlot(x,
//		 * 3)).collect(Collectors.toList()); List<JsonObject> slot3ListUltra =
//		 * datas.stream().filter(x-> filterUltraItem(x,
//		 * 3)).collect(Collectors.toList()); if(ListUtils.isNotEmpty(slot3ListUltra)) {
//		 * result.add(FoodFilterUtils.geFilteredData(slot3ListUltra,addedList)); }else {
//		 * List<JsonObject> slot3List = datas.stream().filter(x-> filterSpecialItem(x,
//		 * 3)).collect(Collectors.toList()); if(ListUtils.isNotEmpty(slot3List)) {
//		 * result.add(FoodFilterUtils.geFilteredData(slot3List,addedList)); }else {
//		 * result.add(FoodFilterUtils.geFilteredData(selectedOptions,addedList)); } }
//		 */
//
//		// result=datas.stream().filter(x->getDietBySlot(x, 3)).filter(x
//		// ->filterAvoidIn(x, disease)).filter(x->filterByCustFoodType(x,
//		// dbResult.getFilterData().getFoodType())).collect(Collectors.toList());
//		// result = filterByCustCommunity(result,
//		// dbResult.getFilterData().getCommunity(), 2, "");
//
//		String[] recF = { "033", "037" };
//		int index = ThreadLocalRandom.current().nextInt(recF.length);
//		List<String> items = new ArrayList<>();
//		items.add("033");
//		items.add("037");
//		// result.add(getMealByCode(recF[index], datas));
//		/////////////////////////////////////////////////////////////////////////////////////
//		// ITEMS
//		// RECOMMENDED-IN
//		List<JsonObject> thyroidList = new ArrayList<>();
//		List<JsonObject> otherOptions = new ArrayList<>();
//		boolean isRecommendedItemFound = false;
//		logger.info("##### " + method + " [[SLOT 3]] DISEASE -->> " + disease);
//		if (null != disease && disease.size() > 0) {
//
//			items.forEach(action -> {
//				if (null != action) {
//					boolean isTItemsToBeRecommended = false;
//					if (disease.contains("T"))
//						isTItemsToBeRecommended = true;
//
//					//JsonObject json = getMealByCode(action, datas);
//					JsonObject json = map.get(action);
//					logger.info("##### " + method + " [[SLOT 3]] - JSON -->> " + json);
//					JsonArray jsonArr = json.getJsonArray("RecommendedIn");
//					List<String> listT = new ArrayList<>();
//					List<String> listOther = new ArrayList<>();
//					if (null != jsonArr && jsonArr.size() > 0) {
//						jsonArr.forEach(item -> {
//							if (null != item) {
//								String food = (String) item;
//								logger.info("##### " + method + " [[SLOT 3]] FOOD -->> " + food);
//								if ("T".equalsIgnoreCase(food)) {
//									listT.add(food);
//								} else {
//									listOther.add(food);
//								}
//							}
//						});
//					}
//
//					if (isTItemsToBeRecommended && null != listT && listT.size() >= 1) {
//						thyroidList.add(json);
//						logger.info("##### " + method + " [[SLOT 3]] THYROID LIST -->> " + thyroidList);
//					} else {
//						listOther.forEach(item -> {
//							String food = (String) item;
//							if (disease.contains(food)) {
//								otherOptions.add(json);
//								logger.info("##### " + method + " [[SLOT 3]] ITEMS OTHER OPTIONS -->> " + otherOptions);
//							}
//						});
//					}
//				}
//			});
//
//			List<String> itemsAdded = new ArrayList<>();
//			if (null != thyroidList && thyroidList.size() > 0) {
//				Collections.shuffle(thyroidList);
//				JsonObject json = thyroidList.get(0);
//				result.add(json);
//				itemsAdded.add(json.getString("code"));
//				//result.add(FoodFilterUtils.geFilteredData(thyroidList, addedList));
//				isRecommendedItemFound = true;
//			} else if (null != otherOptions && otherOptions.size() > 0) {
//				Collections.shuffle(otherOptions);
//				JsonObject json = otherOptions.get(0);
//				result.add(json);
//				itemsAdded.add(json.getString("code"));
//			    //result.add(FoodFilterUtils.geFilteredData(otherOptions, addedList));
//				isRecommendedItemFound = true;
//			}
//
//			if (isRecommendedItemFound)
//				logger.info("##### " + method + " [[SLOT 3]] ITEMS RESULT -->> " + result);
//
//			// 032 ITEM
//			boolean isTItemsToBeRecommended = false;
//			thyroidList.clear();
//			otherOptions.clear();
//			if (disease.contains("T"))
//				isTItemsToBeRecommended = true;
//
//			//JsonObject json = getMealByCode("032", datas);
//			JsonObject json = map.get("032");
//			logger.info("##### " + method + " [[SLOT 3]] - 032 JSON -->> " + json);
//			JsonArray jsonArr = json.getJsonArray("RecommendedIn");
//			List<String> listT = new ArrayList<>();
//			List<String> listOther = new ArrayList<>();
//			if (null != jsonArr && jsonArr.size() > 0) {
//				jsonArr.forEach(item -> {
//					if (null != item) {
//						String food = (String) item;
//						if ("T".equalsIgnoreCase(food)) {
//							listT.add(food);
//						} else {
//							listOther.add(food);
//							logger.info("##### " + method + " [[SLOT 3]] FOOD LISTOTHER -->> " + listOther);
//						}
//					}
//				});
//			}
//
//			if (isTItemsToBeRecommended && null != listT && listT.size() >= 1) {
//				thyroidList.add(json);
//				logger.info("##### " + method + " [[SLOT 3]] 032 THYROID LIST -->> " + thyroidList);
//			} else {
//				listOther.forEach(item -> {
//					String food = (String) item;
//					if (disease.contains(food)) {
//						otherOptions.add(json);
//						logger.info("##### " + method + " [[SLOT 3]] 032 OTHER OPTIONS -->> " + otherOptions);
//					}
//				});
//			}
//
//			boolean is032RecommendedItemFound = false;
//			if (isRecommendedItemFound) {
//				logger.info("##### " + method + " [[SLOT 3]] isRecommendedItemFound -->> " + isRecommendedItemFound);
//				if (null != thyroidList && thyroidList.size() > 0) {
//					Collections.shuffle(thyroidList);
//					if (!itemsAdded.contains(thyroidList.get(0).getString("code"))) {
//						result.add(json);
//						// result.add(FoodFilterUtils.geFilteredData(thyroidList, addedList));
//						logger.info("##### " + method + " [[SLOT 3]] JSON 1 -->> " + json);
//						is032RecommendedItemFound = true;
//					}
//				} else if (null != otherOptions && otherOptions.size() > 0) {
//					Collections.shuffle(otherOptions);
//					if (!itemsAdded.contains(otherOptions.get(0).getString("code"))) {
//						result.add(json);
//						logger.info("##### " + method + " [[SLOT 3]] JSON 2 -->> " + json);
//						is032RecommendedItemFound = true;
//					}
//					//result.add(FoodFilterUtils.geFilteredData(otherOptions, addedList));
//				} else {
//					//result.add(getMealByCode("032", datas));
//					result.add(map.get("032"));
//					//logger.info("##### " + method + " [[SLOT 3]] JSON 3 -->> " + getMealByCode("032", datas));
//					is032RecommendedItemFound = true;
//				}
//
//				if (!is032RecommendedItemFound)
//					//result.add(getMealByCode("032", datas));
//					result.add(map.get("032"));
//			} else {
//				logger.info("##### " + method + " [[SLOT 3]] isRecommendedItemFound -->> " + isRecommendedItemFound);
//				if (null != thyroidList && thyroidList.size() > 0) {
//					Collections.shuffle(thyroidList);
//					if (!itemsAdded.contains(thyroidList.get(0).getString("code"))) {
//						result.add(json);
//						logger.info("##### " + method + " [[SLOT 3]] JSON 4 -->> " + json);
//						is032RecommendedItemFound = true;
//					}
//					//result.add(FoodFilterUtils.geFilteredData(thyroidList, addedList));
//				} else if (null != otherOptions && otherOptions.size() > 0) {
//					Collections.shuffle(otherOptions);
//					if (!itemsAdded.contains(otherOptions.get(0).getString("code"))) {
//						result.add(json);
//						logger.info("##### " + method + " [[SLOT 3]] JSON 5 -->> " + json);
//						is032RecommendedItemFound = true;
//					}
//					//result.add(FoodFilterUtils.geFilteredData(otherOptions, addedList));
//				}
//
//				if (!is032RecommendedItemFound) {
//					result.add(getMealByCode(recF[index], datas));
//					//result.add(getMealByCode("032", datas));
//					result.add(map.get("032"));
//					logger.info("##### " + method + " [[SLOT 3]] JSON 6");
//				} else {
//					result.add(getMealByCode(recF[index], datas));
//					logger.info("##### " + method + " [[SLOT 3]] JSON 7");
//				}
//			}
//		} else {
//			result.add(getMealByCode(recF[index], datas));
//			//result.add(getMealByCode("032", datas));
//			result.add(map.get("032"));
//		}
//
//		logger.info("##### " + method + " [[SLOT 3]] RESULT -->> " + result);
//		/////////////////////////////////////////////////////////////////////////////////////
//
//		result = result.stream().map(x -> {
//			updateCalories(x, x.getDouble("portion"));
//			return x;
//		}).collect(Collectors.toList());
//
//		result.forEach(res -> logger.info("#####" + method + " [SLOT 3] FINAL RESULT -->> " + res.getString("itemCode")
//				+ " ---- " + res.getDouble("Calories")));
//
//		logger.info("##### " + method + " [[SLOT 3]] SELECTEDOPTIONS LAST -->> " + selectedOptions);
//		logger.debug("#########################################################################################");
//		logger.debug("");
//		return result;
//	}
	
	public static List<JsonObject> getSlot4(Map<String, String> dietMap, List<JsonObject> datas, DBResult dbResult, List<String> disease,
			List<JsonObject> selectedOptions, FilterData filterData, String traceId) {
		String method = "FoodFilterUtils getSlot4() " + traceId + "-[" + dbResult.getFilterData().getEmail() + "]";
		logger.info("#########################################################################################");
		logger.info("");
		List<JsonObject> result = new ArrayList<JsonObject>();

//		result.add(getMealByCode("034", datas)); // OLD CODE (GREN SALAD)
		List<JsonObject> list = new ArrayList<JsonObject>();
		list.add(getMealByCode("091", datas)); // ADDED BY DEFAULT - CUCUMBUR RAITA
		list.add(getMealByCode("092", datas)); // ADDED BY DEFAULT - GHIA RAITA
		result.add(getFinalData(list));

		//////////////////////////////
		// RECOMMENDATIONS
		JsonObject json = getMealByCode("034", datas); // GREN SALAD
		if (null != disease)
			if (disease.contains("T"))
				json = getMealByCode("377", datas); // Cucumber, carrot and seeds Salad
			else if (disease.contains("A") || disease.contains("D"))
				json = getMealByCode("373", datas); // Carrot and Cabbage salad (coleslaw)

		result.add(json);
		//////////////////////////////

		if (null == selectedOptions) {
			selectedOptions = new ArrayList<JsonObject>();
		}

		selectedOptions = selectedOptions.stream().filter(x -> getDietBySlot(x, 4)).collect(Collectors.toList());

		// C or A
		List<JsonObject> slot4ListC = selectedOptions.stream()
				.filter(x -> x.getString("Type").equalsIgnoreCase("C") || x.getString("Type").equalsIgnoreCase("A"))
				.collect(Collectors.toList());
		List<JsonObject> SpecialSlotListC = slot4ListC.stream().filter(x -> filterSpecialItem(x, 4))
				.collect(Collectors.toList());
		SpecialSlotListC = filterByCustCommunity(SpecialSlotListC, dbResult.getFilterData().getCommunity(), 4,
				" SPECIAL SLOT ");
		if (null != SpecialSlotListC && SpecialSlotListC.size() > 0) {
//			JsonObject resObjC = FoodFilterUtils.geFilteredData(SpecialSlotListC, addedList);
			JsonObject resObjC = FoodFilterUtils.getFinalData(SpecialSlotListC);
			if (null != resObjC)
				result.add(resObjC);
		} else {
			SpecialSlotListC = filterByCustCommunity(slot4ListC, dbResult.getFilterData().getCommunity(), 4,
					" SPECIAL SLOT ");
			JsonObject resObjC = FoodFilterUtils.getFinalData(SpecialSlotListC);
			if (null != resObjC)
				result.add(resObjC);
		}

		// B
		List<JsonObject> slot4ListB = selectedOptions.stream().filter(x -> x.getString("Type").equalsIgnoreCase("B"))
				.filter(x -> filterByDietSeason(x)).collect(Collectors.toList());
		List<JsonObject> SpecialSlot4ListB = slot4ListB.stream().filter(x -> filterSpecialItem(x, 4))
				.collect(Collectors.toList());
		SpecialSlot4ListB = filterByCustCommunity(SpecialSlot4ListB, dbResult.getFilterData().getCommunity(), 4,
				" SPECIAL SLOT ");
		if (null != SpecialSlot4ListB && SpecialSlot4ListB.size() > 0) {
//			JsonObject resObjB = FoodFilterUtils.geFilteredData(SpecialSlot4ListB, addedList);
			JsonObject resObjB = FoodFilterUtils.getFinalData(SpecialSlotListC);
			logger.info("##### " + method + " [SLOT 4] RESOBJB 1111 -->> " + resObjB);
			if (null != resObjB)
				result.add(resObjB);
		} else {
			SpecialSlot4ListB = filterByCustCommunity(slot4ListB, dbResult.getFilterData().getCommunity(), 4,
					" SPECIAL SLOT ");
//			JsonObject resObjB = FoodFilterUtils.geFilteredData(SpecialSlot4ListB, addedList);
			JsonObject resObjB = FoodFilterUtils.getFinalData(SpecialSlot4ListB);
			if (null != resObjB)
				result.add(resObjB);
		}

//		addedList.addAll(result);

		result = result.stream().map(x -> {
			updateCalories(x, x.getDouble("portion"));
			return x;
		}).collect(Collectors.toList());

		result.forEach(res -> logger.info("##### " + method + " [SLOT 4] RESULT -->> " + res));
		logger.info("#########################################################################################");
		logger.info("");
		return result;
	}
	
	// UNUSED
	public static List<JsonObject> getSlot4_bkp(List<JsonObject> datas, DBResult dbResult, List<String> disease,List<JsonObject> selectedOptions, FilterData filterData, List<JsonObject> addedList) {
		String method = "FoodFilterUtils getSlot4()";
		logger.info("#########################################################################################");
		logger.info("");
		List<JsonObject> result = new ArrayList<JsonObject>();

		result.add(getMealByCode("034", datas));
		logger.debug("##### " + method + " SLOT 4 GREEN SLAD (034) ADDED");
		if (null == selectedOptions) {
			selectedOptions = new ArrayList<JsonObject>();
		}
		selectedOptions = selectedOptions.stream().filter(x -> getDietBySlot(x, 4))
				.filter(x -> filterAvoidIn(x, disease))
				.filter(x->filterByDietSeason(x))
				.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
				.collect(Collectors.toList());
		
		//selectedOptions = getPrefListFromCommunity("SLOT 4", selectedOptions, dbResult.getFilterData().getCommunity());
		logger.debug("##### " + method + " SLOT 4 PREFERENCES (SELECTEDOPTIONS) -->> " + selectedOptions);

		
		// C or A
		List<JsonObject> slot4ListC = selectedOptions.stream()
				.filter(x -> x.getString("Type").equalsIgnoreCase("C") || x.getString("Type").equalsIgnoreCase("A"))
				.collect(Collectors.toList());
		logger.debug("##### " + method + " SLOT 4 slot4ListC -->> "
				+ ((null != slot4ListC && slot4ListC.size() > 0) ? slot4ListC.size() : 0));
		List<JsonObject> SpecialSlotListC = slot4ListC.stream().filter(x -> filterSpecialItem(x, 4))
				.collect(Collectors.toList());
		SpecialSlotListC = filterByCustCommunity(SpecialSlotListC, dbResult.getFilterData().getCommunity(), 4,
				" SPECIAL SLOT ");
		logger.debug("##### " + method + " SLOT 4 SpecialSlotListC SIZE -->> "
				+ ((null != SpecialSlotListC && SpecialSlotListC.size() > 0) ? SpecialSlotListC.size() : 0));
		logger.debug("##### " + method + " SLOT 4 SpecialSlotListC -->> " + SpecialSlotListC);
		if (null != SpecialSlotListC && SpecialSlotListC.size() > 0) {
			JsonObject resObjC = FoodFilterUtils.geFilteredData(SpecialSlotListC, addedList);
			if (null != resObjC) {
				result.add(resObjC);
				logger.debug("##### " + method + " SLOT 4 resObjC (SPECIAL ITEM) -->> " + resObjC);
			}
		}

		// B
		List<JsonObject> slot4ListB = selectedOptions.stream().filter(x -> x.getString("Type").equalsIgnoreCase("B"))
				.filter(x -> filterByDietSeason(x)).collect(Collectors.toList());
		logger.info("##### " + method + " SLOT 4 slot4ListB -->> "
				+ ((null != slot4ListB && slot4ListB.size() > 0) ? slot4ListB.size() : 0));
		List<JsonObject> SpecialSlot4ListB = slot4ListB.stream().filter(x -> filterSpecialItem(x, 4))
				.collect(Collectors.toList());
		SpecialSlot4ListB = filterByCustCommunity(SpecialSlot4ListB, dbResult.getFilterData().getCommunity(), 4,
				" SPECIAL SLOT ");
		logger.debug("##### " + method + " SLOT 4 SpecialSlot4ListB -->> "
				+ ((null != SpecialSlot4ListB && SpecialSlot4ListB.size() > 0) ? SpecialSlot4ListB.size() : 0));
		if (null != SpecialSlot4ListB && SpecialSlot4ListB.size() > 0) {
			JsonObject resObjC = FoodFilterUtils.geFilteredData(SpecialSlot4ListB, addedList);
			logger.debug("##### " + method + " SLOT 4 resObjC (SPECIAL SLOT) -->> " + resObjC);
			if (null != resObjC)
				result.add(resObjC);
		} else {
			JsonObject resObjB = FoodFilterUtils.geFilteredData(slot4ListB, addedList);
			logger.debug("##### " + method + " SLOT 4 resObjB -->> " + resObjB);
			if (null != resObjB)
				result.add(resObjB);
		}

		logger.debug("##### " + method + " SLOT 4 RESULT SIZE -->> "
				+ ((null != result && result.size() > 0) ? result.size() : 0));
		logger.debug("##### " + method + " SLOT 4 RESULT -->> " + result);
		addedList.addAll(result);

		result = result.stream().map(x -> {
			if (null != x.getDouble("portion"))
				updateCalories(x, x.getDouble("portion"));
			return x;
		}).collect(Collectors.toList());

		result.forEach(res -> logger.info("##### " + method + " SLOT 4 FINAL RESULT DATA -->> "
				+ res.getString("itemCode") + " ---- " + res.getDouble("Calories")));
		logger.info("#########################################################################################");
		logger.info("");
		return result;
	}
	
	public static SlotFilter getSlot5(List<JsonObject> datas, DBResult dbResult, List<String> disease,
			List<JsonObject> selectedOptions, String traceId) {
		String method = "FoodFilterUtils getSlot5() " + traceId + "-[" + dbResult.getFilterData().getEmail() + "]";
		logger.info("#########################################################################################");
		logger.info("");
		SlotFilter slotFilter = new SlotFilter();
		List<JsonObject> result = new ArrayList<JsonObject>();
		if (null == selectedOptions) {
			selectedOptions = new ArrayList<JsonObject>();
		} else {

			selectedOptions = selectedOptions.stream().filter(x -> getDietBySlot(x, 5)).collect(Collectors.toList());
			selectedOptions = selectedOptions.stream().filter(x -> filterAvoidIn(x, disease))
					.filter(x -> filterByDietSeason(x))
					.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
					.collect(Collectors.toList());
			selectedOptions = filterByCustCommunity(selectedOptions, dbResult.getFilterData().getCommunity(), 5,
					" NORMAL ");
		}

		List<JsonObject> slot5List = datas.stream().filter(x -> filterAvoidIn(x, disease))
				.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
				.filter(x -> getDietBySlot(x, 5)).filter(x -> filterByDietSeason(x)).collect(Collectors.toList());
		logger.info("##### " + method + " [SLOT 5] 000000 -->> " + slot5List);

		/////////////////////////////////////////////////////////////////////////////////////
		// DATAS
		// RECOMMENDED-IN
		List<JsonObject> acidityList = new ArrayList<>();
		List<JsonObject> otherOptions = new ArrayList<>();
		boolean isRecommendedItemFound = false;
		logger.info("##### " + method + " [SLOT 5] DISEASE -->> " + disease);
		if (null != disease && disease.size() > 0) {
			List<JsonObject> slot5ListRecommendedIn = slot5List.stream().filter(x -> getDrinks(x))
					.filter(x -> filterSpecialItem(x, 5)).collect(Collectors.toList());
			if (null == slot5ListRecommendedIn
					|| (null != slot5ListRecommendedIn && slot5ListRecommendedIn.size() <= 0))
				slot5ListRecommendedIn = slot5List.stream().filter(x -> getDrinks(x)).collect(Collectors.toList());

			slot5ListRecommendedIn.forEach(action -> {
				boolean isAItemsToBeRecommended = false;
				if (disease.contains("A"))
					isAItemsToBeRecommended = true;

				JsonArray jsonArr = action.getJsonArray("RecommendedIn");
				List<String> listA = new ArrayList<>();
				List<String> listOther = new ArrayList<>();
				if (null != action && null != jsonArr) {
					jsonArr.forEach(item -> {
						if (null != item) {
							String food = (String) item;
							if ("A".equalsIgnoreCase(food))
								listA.add(food);
							else
								listOther.add(food);
						}
					});

					if (isAItemsToBeRecommended && null != listA && listA.size() >= 1) {
						acidityList.add(action);
					} else {
						listOther.forEach(item -> {
							String food = (String) item;
							if (disease.contains(food))
								otherOptions.add(action);
						});
					}
				}
			});

			if (null != acidityList && acidityList.size() > 0) {
				Collections.shuffle(acidityList);
				result.add(acidityList.get(0));
				isRecommendedItemFound = true;
			} else if (null != otherOptions && otherOptions.size() > 0) {
				Collections.shuffle(otherOptions);
				result.add(otherOptions.get(0));
				isRecommendedItemFound = true;
			}

			// SELECTEDOPTIONS
			if (!isRecommendedItemFound) {
				List<JsonObject> selectedOptionsRecommendedIn = selectedOptions.stream().filter(x -> getDrinks(x))
						.filter(x -> filterSpecialItem(x, 5)).collect(Collectors.toList());
				if (null == selectedOptionsRecommendedIn
						|| (null != selectedOptionsRecommendedIn && selectedOptionsRecommendedIn.size() <= 0))
					selectedOptionsRecommendedIn = selectedOptions.stream().filter(x -> getDrinks(x))
							.collect(Collectors.toList());

				selectedOptionsRecommendedIn.forEach(action -> {
					boolean isAItemsToBeRecommended = false;
					if (disease.contains("A"))
						isAItemsToBeRecommended = true;
					JsonArray jsonArr = action.getJsonArray("RecommendedIn");
					List<String> listA = new ArrayList<>();
					List<String> listOther = new ArrayList<>();
					if (null != action && null != jsonArr) {
						jsonArr.forEach(item -> {
							if (null != item) {
								String food = (String) item;
								if ("A".equalsIgnoreCase(food))
									listA.add(food);
								else
									listOther.add(food);
							}
						});

						if (isAItemsToBeRecommended && null != listA && listA.size() >= 1) {
							acidityList.add(action);
						} else {
							listOther.forEach(item -> {
								String food = (String) item;
								if (disease.contains(food))
									otherOptions.add(action);
							});
						}
					}
				});

				if (null != acidityList && acidityList.size() > 0) {
					Collections.shuffle(acidityList);
					result.add(acidityList.get(0));
					isRecommendedItemFound = true;
				} else if (null != otherOptions && otherOptions.size() > 0) {
					Collections.shuffle(otherOptions);
					result.add(otherOptions.get(0));
					isRecommendedItemFound = true;
				}
			}
		}
		/////////////////////////////////////////////////////////////////////////////////////

		if (!isRecommendedItemFound) {

			slot5List = slot5List.stream().filter(x -> filterUltraItem(x, 5)).collect(Collectors.toList());
			if (ListUtils.isNotEmpty(slot5List)) {
				slot5List = filterByCustCommunity(slot5List, dbResult.getFilterData().getCommunity(), 5,
						" ULTRA SPECIAL SLOT ");
//				result.add(FoodFilterUtils.geFilteredData(slot5List, addedList));
				result.add(FoodFilterUtils.getFinalData(slot5List));
				slotFilter.setLocked(true);
			} else {
				List<JsonObject> Special_slotList = datas.stream().filter(x -> getDietBySlot(x, 5))
						.filter(x -> filterSpecialItem(x, 5)).filter(x -> filterAvoidIn(x, disease))
						.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
						.collect(Collectors.toList());
				slot5List = filterByCustCommunity(Special_slotList, dbResult.getFilterData().getCommunity(), 5,
						" SPECIAL SLOT ");
				if (ListUtils.isNotEmpty(slot5List))
//					result.add(FoodFilterUtils.geFilteredData(Special_slotList, addedList));
					result.add(FoodFilterUtils.getFinalData(Special_slotList));
				else
//					result.add(FoodFilterUtils.geFilteredData(selectedOptions, addedList));
					result.add(FoodFilterUtils.getFinalData(selectedOptions));
			}
		}

		logger.info("##### " + method + " [SLOT 5] 888888");
		result = result.stream().map(x -> {
			updateCalories(x, x.getDouble("portion"));
			return x;
		}).collect(Collectors.toList());

//		addedList.addAll(result);
		slotFilter.setDataList(result);

		result.forEach(res -> logger.info("##### " + method + " [SLOT 5] FINAL RESULT DATA -->> " + res));
		logger.info("#########################################################################################");
		logger.info("");
		return slotFilter;
	}
	
	public static List<JsonObject> getSlot6(List<JsonObject> datas, DBResult dbResult, List<String> disease,
			List<JsonObject> selectedOptions, String traceId) {
		String method = "FoodFilterUtils getSlot6() " + traceId + "-[" + dbResult.getFilterData().getEmail() + "]";
		logger.info("#########################################################################################");
		logger.info("");
		List<JsonObject> result = new ArrayList<JsonObject>();
		if (null == selectedOptions) {
			selectedOptions = new ArrayList<JsonObject>();
		}
		selectedOptions = selectedOptions.stream().filter(x -> getDietBySlot(x, 6)).filter(x -> getSnacks(x))
				.filter(x -> filterAvoidIn(x, disease)).filter(x -> filterByDietSeason(x))
				.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
				.collect(Collectors.toList());

		List<JsonObject> slot6List = datas.stream().filter(x -> getDietBySlot(x, 6))
				.filter(x -> filterAvoidIn(x, disease)).filter(x -> filterByDietSeason(x))
				.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
				.collect(Collectors.toList());

		/////////////////////////////////////////////////////////////////////////////////////
		// DATAS
		// RECOMMENDED-IN
		List<JsonObject> bpOrHeartOrCholestrolList = new ArrayList<>();
		List<JsonObject> otherOptions = new ArrayList<>();
		boolean isRecommendedItemFound = false;
		logger.info("##### " + method + " [SLOT 6] DISEASE -->> " + disease);
		// AS PER PUNEET SIR (Jan 30 '2021), BELOW CODE GOT COMMENTED
//		if (null != disease && disease.size() > 0) {
//			List<JsonObject> slot6ListRecommendedIn = slot6List.stream().filter(x -> getSnacks(x))
//					.filter(x -> filterSpecialItem(x, 6)).collect(Collectors.toList());
//			if (null == slot6ListRecommendedIn
//					|| (null != slot6ListRecommendedIn && slot6ListRecommendedIn.size() <= 0))
//				slot6List.stream().filter(x -> getSnacks(x)).collect(Collectors.toList());
//
//			slot6ListRecommendedIn.forEach(action -> {
//				boolean isBItemsToBeRecommended = false;
//				if (disease.contains("B"))
//					isBItemsToBeRecommended = true;
//
//				JsonArray jsonArr = action.getJsonArray("RecommendedIn");
//				List<String> listB = new ArrayList<>();
//				List<String> listOther = new ArrayList<>();
//				if (null != action && null != jsonArr) {
//					jsonArr.forEach(item -> {
//						if (null != item) {
//							String food = (String) item;
//							if ("B".equalsIgnoreCase(food))
//								listB.add(food);
//							else
//								listOther.add(food);
//						}
//					});
//
//					if (isBItemsToBeRecommended && null != listB && listB.size() >= 1) {
//						bpOrHeartOrCholestrolList.add(action);
//					} else {
//						listOther.forEach(item -> {
//							String food = (String) item;
//							if (disease.contains(food)) {
//								otherOptions.add(action);
//							}
//						});
//					}
//				}
//			});
//
//			if (null != bpOrHeartOrCholestrolList && bpOrHeartOrCholestrolList.size() > 0) {
//				Collections.shuffle(bpOrHeartOrCholestrolList);
//				result.add(bpOrHeartOrCholestrolList.get(0));
//				isRecommendedItemFound = true;
//			} else if (null != otherOptions && otherOptions.size() > 0) {
//				Collections.shuffle(otherOptions);
//				result.add(otherOptions.get(0));
//				isRecommendedItemFound = true;
//			}
//
//			// SELECTEDOPTIONS
//			if (!isRecommendedItemFound) {
//				List<JsonObject> selectedOptionsRecommendedIn = selectedOptions.stream().filter(x -> getSnacks(x))
//						.filter(x -> filterSpecialItem(x, 6)).collect(Collectors.toList());
//				if (null == selectedOptionsRecommendedIn
//						|| (null != selectedOptionsRecommendedIn && selectedOptionsRecommendedIn.size() <= 0))
//					selectedOptionsRecommendedIn = selectedOptions.stream().filter(x -> getSnacks(x))
//							.collect(Collectors.toList());
//
//				selectedOptionsRecommendedIn.forEach(action -> {
//					boolean isBItemsToBeRecommended = false;
//					if (disease.contains("B"))
//						isBItemsToBeRecommended = true;
//					JsonArray jsonArr = action.getJsonArray("RecommendedIn");
//					List<String> listB = new ArrayList<>();
//					List<String> listOther = new ArrayList<>();
//					if (null != action && null != jsonArr) {
//						jsonArr.forEach(item -> {
//							if (null != item) {
//								String food = (String) item;
//								if ("B".equalsIgnoreCase(food))
//									listB.add(food);
//								else
//									listOther.add(food);
//							}
//						});
//
//						if (isBItemsToBeRecommended && null != listB && listB.size() >= 1) {
//							bpOrHeartOrCholestrolList.add(action);
//						} else {
//							listOther.forEach(item -> {
//								String food = (String) item;
//								if (disease.contains(food))
//									otherOptions.add(action);
//							});
//						}
//					}
//				});
//
//				if (null != bpOrHeartOrCholestrolList && bpOrHeartOrCholestrolList.size() > 0) {
//					Collections.shuffle(bpOrHeartOrCholestrolList);
//					result.add(bpOrHeartOrCholestrolList.get(0));
//					isRecommendedItemFound = true;
//				} else if (null != otherOptions && otherOptions.size() > 0) {
//					Collections.shuffle(otherOptions);
//					result.add(otherOptions.get(0));
//					isRecommendedItemFound = true;
//				}
//			}
//		}
		/////////////////////////////////////////////////////////////////////////////////////

		if (!isRecommendedItemFound) {
			slot6List = slot6List.stream().filter(x -> filterSpecialItem(x, 6)).collect(Collectors.toList());
			if (ListUtils.isNotEmpty(slot6List)) {
				slot6List = filterByCustCommunity(slot6List, dbResult.getFilterData().getCommunity(), 6,
						" SPECIAL SLOT ");
//				result.add(FoodFilterUtils.geFilteredData(slot6List, addedList));
				result.add(FoodFilterUtils.getFinalData(slot6List));
			} else {
				List<JsonObject> slot6PrefList = selectedOptions.stream().filter(x -> getSnacks(x))
						.collect(Collectors.toList());
				slot6PrefList = filterByCustCommunity(slot6PrefList, dbResult.getFilterData().getCommunity(), 6,
						" NORMAL ");
//				result.add(FoodFilterUtils.geFilteredData(slot6PrefList, addedList));
				result.add(FoodFilterUtils.getFinalData(slot6PrefList));
			}
		}

		result = result.stream().map(x -> {
			updateCalories(x, x.getDouble("portion"));
			return x;
		}).collect(Collectors.toList());

		result.forEach(res -> logger.info("##### " + method + " [SLOT 6] FINAL RESULT DATA -->> " + res));
		logger.info("#########################################################################################");
		logger.info("");
		return result;
	}
	
	public static List<JsonObject> getSlot7(Map<String, String> dietMap, List<JsonObject> datas, DBResult dbResult, List<String> disease,
			List<JsonObject> selectedOptions, String traceId) {
		String method = "FoodFilterUtils getSlot7() " + traceId + "-[" + dbResult.getFilterData().getEmail() + "]";
		logger.info("#########################################################################################");
		logger.info("");
		List<JsonObject> result = new ArrayList<JsonObject>();
		JsonObject resObjB = null;
		
		
		

		//////////////////////////////
		// RECOMMENDATIONS
		JsonObject salad = getMealByCode("034", datas);
		if (null != disease)
			if (disease.contains("T"))
				salad = getMealByCode("377", datas);
			else if (disease.contains("A") || disease.contains("D"))
				salad = getMealByCode("373", datas);

		result.add(salad);
		//////////////////////////////

		if (null == selectedOptions) {
			selectedOptions = new ArrayList<JsonObject>();
		}
		List<JsonObject> datas1 = datas.stream().filter(x -> getDietBySlot(x, 7)).filter(x -> filterAvoidIn(x, disease))
				.filter(x -> filterByDietSeason(x))
				.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
				.collect(Collectors.toList());
		datas1 = getPrefListFromCommunity("SLOT 7", datas1, dbResult.getFilterData().getCommunity());
		selectedOptions = selectedOptions.stream().filter(x -> getDietBySlot(x, 7))
				.filter(x -> filterAvoidIn(x, disease)).filter(x -> filterByDietSeason(x))
				.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
				.collect(Collectors.toList());
		selectedOptions = getPrefListFromCommunity("SLOT 7", selectedOptions, dbResult.getFilterData().getCommunity());

		List<JsonObject> slotB7List = datas1.stream().filter(x -> filterSpecialItem(x, 7)).collect(Collectors.toList());
		slotB7List = filterByCustCommunity(slotB7List, dbResult.getFilterData().getCommunity(), 7, " SPECIAL SLOT ");

		if (ListUtils.isNotEmpty(slotB7List)) {
			resObjB = FoodFilterUtils.getFinalData(slotB7List);
			result.add(resObjB); // A or C

			// WORKAROUND FOR 'B' (FOR TESTING ONLY, WILL BE REMOVED LATER)
			if ("A".equalsIgnoreCase(resObjB.getString("Type")) || "C".equalsIgnoreCase(resObjB.getString("Type"))) {
				List<JsonObject> list = new ArrayList<JsonObject>();
				list.add(getMealByCode("009", datas)); // ADDED BY DEFAULT - BROWN RICE
				list.add(getMealByCode("219", datas)); // ADDED BY DEFAULT - MULTIGRAIN ROTI
				result.add(geFilteredData(list));
			}

//			// ACTUAL 'B' IMPLEMENTATION
//			if ("A".equalsIgnoreCase(resObjB.getString("Type")) || "C".equalsIgnoreCase(resObjB.getString("Type"))) {
//				List<JsonObject> listB = selectedOptions.stream().filter(x -> "B".equalsIgnoreCase(x.getString("Type")))
//						.collect(Collectors.toList());
//				JsonObject objectB = FoodFilterUtils.getFinalData(listB);
//				if (null != listB & listB.size() > 0) {
//					objectB.put("portion", 1);
//					result.add(objectB);
//				}
//			}
		} else { // THIS BLOCK UN-USED
			// ACTUAL 'A' or 'C'
			slotB7List = selectedOptions.stream()
					.filter(x -> x.getString("Type").equalsIgnoreCase("A") || x.getString("Type").equalsIgnoreCase("C"))
					.collect(Collectors.toList());
			slotB7List = filterByCustCommunity(slotB7List, dbResult.getFilterData().getCommunity(), 7,
					" NON SPECIAL SLOT (A/C) ");
			JsonObject json = FoodFilterUtils.getFinalData(slotB7List);
			result.add(json); // A or C

			// WORKAROUND FOR 'B' (FOR TESTING ONLY, WILL BE REMOVED LATER)
			if ("A".equalsIgnoreCase(json.getString("Type")) || "C".equalsIgnoreCase(json.getString("Type"))) {
				List<JsonObject> list = new ArrayList<JsonObject>();
				list.add(getMealByCode("009", datas)); // ADDED BY DEFAULT - BROWN RICE
				list.add(getMealByCode("219", datas)); // ADDED BY DEFAULT - MULTIGRAIN ROTI
				result.add(geFilteredData(list));
			}

//			List<JsonObject> listB = selectedOptions.stream().filter(x -> x.getString("Type").equalsIgnoreCase("B"))
//					.collect(Collectors.toList());
//			listB = filterByCustCommunity(listB, dbResult.getFilterData().getCommunity(), 7, " NON SPECIAL SLOT (B) ");
//			JsonObject objectB = FoodFilterUtils.getFinalData(listB);
//			if (objectB != null) {
//				objectB.put("portion", 1);
//				result.add(objectB);
//			}
		}

//		addedList.addAll(result);
		result = result.stream().map(x -> {
			updateCalories(x, x.getDouble("portion"));
			return x;
		}).collect(Collectors.toList());

		result.forEach(res -> logger.info("##### " + method + " [SLOT 7] FINAL RESULT DATA -->> " + res));
		logger.info("#########################################################################################");
		logger.info("");
		return result;
	}

	public static SlotFilter getSlot8(List<JsonObject> datas, DBResult dbResult, List<String> disease,
			List<JsonObject> selectedOptions, String traceId) {
		String method = "FoodFilterUtils getSlot8() " + traceId + "-[" + dbResult.getFilterData().getEmail() + "]";
		logger.info("#########################################################################################");
		logger.info("");
		SlotFilter slotFilter = new SlotFilter();
		List<JsonObject> result = new ArrayList<JsonObject>();
		///// NEW CHANGE /////
		List<JsonObject> slot8List = datas.stream().filter(x -> getDietBySlot(x, 8))
				.filter(x -> filterAvoidIn(x, disease)).filter(x -> filterByDietSeason(x))
				.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
				.filter(x -> (x.getString("Type").equalsIgnoreCase("DM"))).collect(Collectors.toList());
		if (null == slot8List || (null != slot8List && slot8List.size() <= 0)) {
			if (null != selectedOptions)
				slot8List = selectedOptions.stream().filter(x -> getDietBySlot(x, 8))
						.filter(x -> filterAvoidIn(x, disease)).filter(x -> filterByDietSeason(x))
						.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
						.filter(x -> (x.getString("Type").equalsIgnoreCase("DM"))).collect(Collectors.toList());
		}
		
		result.add(getFinalData(slot8List));
		/////////////////////
		
		
		
		
//		result.add(getMealByCode("173", datas));
//		if (null == selectedOptions)
//			selectedOptions = new ArrayList<JsonObject>();
//		else
//			selectedOptions = selectedOptions.stream().filter(x -> getDietBySlot(x, 8))
//					.filter(x -> filterAvoidIn(x, disease)).filter(x -> filterByDietSeason(x))
//					.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
//					.collect(Collectors.toList());
//
//		List<JsonObject> slot8List = datas.stream().filter(x -> getDietBySlot(x, 8))
//				.filter(x -> filterAvoidIn(x, disease)).filter(x -> filterByDietSeason(x))
//				.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
//				.collect(Collectors.toList());
//
//		/////////////////////////////////////////////////////////////////////////////////////
//		// DATAS
//		// RECOMMENDED-IN
//		List<JsonObject> sleepDisorderList = new ArrayList<>();
//		List<JsonObject> otherOptions = new ArrayList<>();
//		boolean isRecommendedItemFound = false;
//		logger.info("##### " + method + " [SLOT 8] DISEASE -->> " + disease);
//		if (null != disease && disease.size() > 0) {
//			List<JsonObject> slot8ListRecommendedIn = slot8List.stream().filter(x -> getDItems(x))
//					.filter(x -> filterSpecialItem(x, 8)).collect(Collectors.toList());
//			if (null == slot8ListRecommendedIn
//					|| (null != slot8ListRecommendedIn && slot8ListRecommendedIn.size() <= 0))
//				slot8ListRecommendedIn = slot8List.stream().filter(x -> getDItems(x)).collect(Collectors.toList());
//
//			slot8ListRecommendedIn.forEach(action -> {
//				boolean isSItemsToBeRecommended = false;
//				if (disease.contains("S"))
//					isSItemsToBeRecommended = true;
//
//				JsonArray jsonArr = action.getJsonArray("RecommendedIn");
//				List<String> listS = new ArrayList<>();
//				List<String> listOther = new ArrayList<>();
//				if (null != action && null != jsonArr) {
//					jsonArr.forEach(item -> {
//						if (null != item) {
//							String food = (String) item;
//							if ("S".equalsIgnoreCase(food))
//								listS.add(food);
//							else
//								listOther.add(food);
//						}
//					});
//
//					if (isSItemsToBeRecommended && null != listS && listS.size() >= 1) {
//						sleepDisorderList.add(action);
//					} else {
//						listOther.forEach(item -> {
//							String food = (String) item;
//							if (disease.contains(food))
//								otherOptions.add(action);
//						});
//					}
//				}
//			});
//
//			if (null != sleepDisorderList && sleepDisorderList.size() > 0) {
//				String res = Character.toString(result.get(0).getString("Type").charAt(0));
//				String sdo = Character.toString(sleepDisorderList.get(0).getString("Type").charAt(0));
//				if (!res.equalsIgnoreCase(sdo) && (!geFilteredItemChecked(result.get(0), addedList)
//						|| !geFilteredItemChecked(sleepDisorderList.get(0), addedList))) {
//					Collections.shuffle(sleepDisorderList);
//					result.add(sleepDisorderList.get(0));
//					isRecommendedItemFound = true;
//				}
//
//			} else if (null != otherOptions && otherOptions.size() > 0) {
//				String res = Character.toString(result.get(0).getString("Type").charAt(0));
//				String oth = Character.toString(otherOptions.get(0).getString("Type").charAt(0));
//				if (!res.equalsIgnoreCase(oth) && (!geFilteredItemChecked(result.get(0), addedList)
//						|| !geFilteredItemChecked(otherOptions.get(0), addedList))) {
//					Collections.shuffle(otherOptions);
//					result.add(otherOptions.get(0));
//					isRecommendedItemFound = true;
//				}
//			}
//
//			// SELECTEDOPTIONS
//			if (!isRecommendedItemFound) {
//				List<JsonObject> selectedOptionsRecommendedIn = selectedOptions.stream().filter(x -> getSnacks(x))
//						.filter(x -> filterSpecialItem(x, 8)).collect(Collectors.toList());
//				if (null == selectedOptionsRecommendedIn
//						|| (null != selectedOptionsRecommendedIn && selectedOptionsRecommendedIn.size() <= 0))
//					selectedOptionsRecommendedIn = selectedOptions.stream().filter(x -> getSnacks(x))
//							.collect(Collectors.toList());
//
//				selectedOptionsRecommendedIn.forEach(action -> {
//					boolean isSItemsToBeRecommended = false;
//					if (disease.contains("S"))
//						isSItemsToBeRecommended = true;
//					JsonArray jsonArr = action.getJsonArray("RecommendedIn");
//					List<String> listS = new ArrayList<>();
//					List<String> listOther = new ArrayList<>();
//					if (null != action && null != jsonArr) {
//						jsonArr.forEach(item -> {
//							if (null != item) {
//								String food = (String) item;
//								if ("S".equalsIgnoreCase(food))
//									listS.add(food);
//								else
//									listOther.add(food);
//							}
//						});
//
//						if (isSItemsToBeRecommended && null != listS && listS.size() >= 1) {
//							sleepDisorderList.add(action);
//						} else {
//							listOther.forEach(item -> {
//								String food = (String) item;
//								if (disease.contains(food))
//									otherOptions.add(action);
//							});
//						}
//					}
//				});
//
//				if (null != sleepDisorderList && sleepDisorderList.size() > 0) {
//					String res = Character.toString(result.get(0).getString("Type").charAt(0));
//					String sdo = Character.toString(sleepDisorderList.get(0).getString("Type").charAt(0));
//					if (!res.equalsIgnoreCase(sdo) && (!geFilteredItemChecked(result.get(0), addedList)
//							|| !geFilteredItemChecked(sleepDisorderList.get(0), addedList))) {
//						Collections.shuffle(sleepDisorderList);
//						result.add(sleepDisorderList.get(0));
//					}
//					isRecommendedItemFound = true;
//				} else if (null != otherOptions && otherOptions.size() > 0) {
//					String res = Character.toString(result.get(0).getString("Type").charAt(0));
//					String sdo = Character.toString(otherOptions.get(0).getString("Type").charAt(0));
//					if (!res.equalsIgnoreCase(sdo) && (!geFilteredItemChecked(result.get(0), addedList)
//							|| !geFilteredItemChecked(otherOptions.get(0), addedList))) {
//						Collections.shuffle(otherOptions);
//						result.add(otherOptions.get(0));
//					}
//					isRecommendedItemFound = true;
//				}
//			}
//		}
//		/////////////////////////////////////////////////////////////////////////////////////
//
//		if (!isRecommendedItemFound) {
//			List<JsonObject> ultraList = datas.stream().filter(x -> filterUltraItem(x, 8))
//					.filter(x -> filterAvoidIn(x, disease)).filter(x -> filterByDietSeason(x))
//					.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
//					.collect(Collectors.toList());
//			ultraList = filterByCustCommunity(ultraList, dbResult.getFilterData().getCommunity(), 8,
//					" ULTRA SPECIAL SLOT ");
//
//			slotFilter.setLocked(false);
//			// ULTRA SPECIAL
//			if (ListUtils.isNotEmpty(ultraList)) {
//				JsonObject jsonObject = FoodFilterUtils.geFilteredData(ultraList, datas);
//				if (null != jsonObject) {
//					if (!result.get(0).getString("Type").equalsIgnoreCase(jsonObject.getString("Type")))
//						if (!geFilteredItemChecked(result.get(0), addedList)
//								|| !geFilteredItemChecked(jsonObject, addedList))
//							result.add(jsonObject);
//					// slotFilter.setLocked(true);
//				}
//			} else { // SPECIAL SLOT
//				List<JsonObject> specialList = datas.stream().filter(x -> getDietBySlot(x, 8))
//						.filter(x -> filterSpecialItem(x, 8)).filter(x -> filterAvoidIn(x, disease))
//						.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
//						.collect(Collectors.toList());
//				specialList = filterByCustCommunity(specialList, dbResult.getFilterData().getCommunity(), 8,
//						" SPECIAL SLOT ");
//				if (ListUtils.isNotEmpty(specialList)) { // SPECIAL SLOT
//					JsonObject jsonObject = FoodFilterUtils.geFilteredData(specialList, datas);
//					if (!result.get(0).getString("Type").equalsIgnoreCase(jsonObject.getString("Type"))) {
//						if (!geFilteredItemChecked(result.get(0), addedList)
//								|| !geFilteredItemChecked(jsonObject, addedList))
//							result.add(jsonObject);
//					}
//					slotFilter.setLocked(false);
//				} else { // NON-SPECIAL SLOT
//					selectedOptions = filterByCustCommunity(selectedOptions, dbResult.getFilterData().getCommunity(), 8,
//							" NORMAL ");
//					JsonObject jsonObject = FoodFilterUtils.geFilteredData(selectedOptions, datas);
//					if (!result.get(0).getString("Type").equalsIgnoreCase(jsonObject.getString("Type")))
//						if (!geFilteredItemChecked(result.get(0), addedList)
//								|| !geFilteredItemChecked(jsonObject, addedList))
//							result.add(jsonObject);
//
//					slotFilter.setLocked(false);
//				}
//			}
//		}

		result = result.stream().map(x -> {
//			if ("173".equalsIgnoreCase(x.getString("_id")))
//				x.put("portion", 0.5);
			updateCalories(x, x.getDouble("portion"));
			return x;
		}).collect(Collectors.toList());

		slotFilter.setDataList(result);

		result.forEach(res -> logger.info("##### " + method + " [SLOT 8] FINAL RESULT DATA -->> " + res));
		logger.info("#########################################################################################");
		logger.info("");
		return slotFilter;
	}
	
	public static SlotFilter getSlot8Old(List<JsonObject> datas, DBResult dbResult, List<String> disease,
			List<JsonObject> selectedOptions, List<JsonObject> addedList, String traceId) {
		String method = "FoodFilterUtils getSlot8() " + traceId + "-[" + dbResult.getFilterData().getEmail() + "]";
		logger.info("#########################################################################################");
		logger.info("");
		SlotFilter slotFilter = new SlotFilter();
		List<JsonObject> result = new ArrayList<JsonObject>();
		result.add(getMealByCode("173", datas));
		if (null == selectedOptions)
			selectedOptions = new ArrayList<JsonObject>();
		else
			selectedOptions = selectedOptions.stream().filter(x -> getDietBySlot(x, 8))
					.filter(x -> filterAvoidIn(x, disease)).filter(x -> filterByDietSeason(x))
					.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
					.collect(Collectors.toList());

		List<JsonObject> slot8List = datas.stream().filter(x -> getDietBySlot(x, 8))
				.filter(x -> filterAvoidIn(x, disease)).filter(x -> filterByDietSeason(x))
				.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
				.collect(Collectors.toList());

		/////////////////////////////////////////////////////////////////////////////////////
		// DATAS
		// RECOMMENDED-IN
		List<JsonObject> sleepDisorderList = new ArrayList<>();
		List<JsonObject> otherOptions = new ArrayList<>();
		boolean isRecommendedItemFound = false;
		logger.info("##### " + method + " [SLOT 8] DISEASE -->> " + disease);
		if (null != disease && disease.size() > 0) {
			List<JsonObject> slot8ListRecommendedIn = slot8List.stream().filter(x -> getDItems(x))
					.filter(x -> filterSpecialItem(x, 8)).collect(Collectors.toList());
			if (null == slot8ListRecommendedIn
					|| (null != slot8ListRecommendedIn && slot8ListRecommendedIn.size() <= 0))
				slot8ListRecommendedIn = slot8List.stream().filter(x -> getDItems(x)).collect(Collectors.toList());

			slot8ListRecommendedIn.forEach(action -> {
				boolean isSItemsToBeRecommended = false;
				if (disease.contains("S"))
					isSItemsToBeRecommended = true;

				JsonArray jsonArr = action.getJsonArray("RecommendedIn");
				List<String> listS = new ArrayList<>();
				List<String> listOther = new ArrayList<>();
				if (null != action && null != jsonArr) {
					jsonArr.forEach(item -> {
						if (null != item) {
							String food = (String) item;
							if ("S".equalsIgnoreCase(food))
								listS.add(food);
							else
								listOther.add(food);
						}
					});

					if (isSItemsToBeRecommended && null != listS && listS.size() >= 1) {
						sleepDisorderList.add(action);
					} else {
						listOther.forEach(item -> {
							String food = (String) item;
							if (disease.contains(food))
								otherOptions.add(action);
						});
					}
				}
			});

			if (null != sleepDisorderList && sleepDisorderList.size() > 0) {
				String res = Character.toString(result.get(0).getString("Type").charAt(0));
				String sdo = Character.toString(sleepDisorderList.get(0).getString("Type").charAt(0));
				if (!res.equalsIgnoreCase(sdo) && (!geFilteredItemChecked(result.get(0), addedList)
						|| !geFilteredItemChecked(sleepDisorderList.get(0), addedList))) {
					Collections.shuffle(sleepDisorderList);
					result.add(sleepDisorderList.get(0));
					isRecommendedItemFound = true;
				}

			} else if (null != otherOptions && otherOptions.size() > 0) {
				String res = Character.toString(result.get(0).getString("Type").charAt(0));
				String oth = Character.toString(otherOptions.get(0).getString("Type").charAt(0));
				if (!res.equalsIgnoreCase(oth) && (!geFilteredItemChecked(result.get(0), addedList)
						|| !geFilteredItemChecked(otherOptions.get(0), addedList))) {
					Collections.shuffle(otherOptions);
					result.add(otherOptions.get(0));
					isRecommendedItemFound = true;
				}
			}

			// SELECTEDOPTIONS
			if (!isRecommendedItemFound) {
				List<JsonObject> selectedOptionsRecommendedIn = selectedOptions.stream().filter(x -> getSnacks(x))
						.filter(x -> filterSpecialItem(x, 8)).collect(Collectors.toList());
				if (null == selectedOptionsRecommendedIn
						|| (null != selectedOptionsRecommendedIn && selectedOptionsRecommendedIn.size() <= 0))
					selectedOptionsRecommendedIn = selectedOptions.stream().filter(x -> getSnacks(x))
							.collect(Collectors.toList());

				selectedOptionsRecommendedIn.forEach(action -> {
					boolean isSItemsToBeRecommended = false;
					if (disease.contains("S"))
						isSItemsToBeRecommended = true;
					JsonArray jsonArr = action.getJsonArray("RecommendedIn");
					List<String> listS = new ArrayList<>();
					List<String> listOther = new ArrayList<>();
					if (null != action && null != jsonArr) {
						jsonArr.forEach(item -> {
							if (null != item) {
								String food = (String) item;
								if ("S".equalsIgnoreCase(food))
									listS.add(food);
								else
									listOther.add(food);
							}
						});

						if (isSItemsToBeRecommended && null != listS && listS.size() >= 1) {
							sleepDisorderList.add(action);
						} else {
							listOther.forEach(item -> {
								String food = (String) item;
								if (disease.contains(food))
									otherOptions.add(action);
							});
						}
					}
				});

				if (null != sleepDisorderList && sleepDisorderList.size() > 0) {
					String res = Character.toString(result.get(0).getString("Type").charAt(0));
					String sdo = Character.toString(sleepDisorderList.get(0).getString("Type").charAt(0));
					if (!res.equalsIgnoreCase(sdo) && (!geFilteredItemChecked(result.get(0), addedList)
							|| !geFilteredItemChecked(sleepDisorderList.get(0), addedList))) {
						Collections.shuffle(sleepDisorderList);
						result.add(sleepDisorderList.get(0));
					}
					isRecommendedItemFound = true;
				} else if (null != otherOptions && otherOptions.size() > 0) {
					String res = Character.toString(result.get(0).getString("Type").charAt(0));
					String sdo = Character.toString(otherOptions.get(0).getString("Type").charAt(0));
					if (!res.equalsIgnoreCase(sdo) && (!geFilteredItemChecked(result.get(0), addedList)
							|| !geFilteredItemChecked(otherOptions.get(0), addedList))) {
						Collections.shuffle(otherOptions);
						result.add(otherOptions.get(0));
					}
					isRecommendedItemFound = true;
				}
			}
		}
		/////////////////////////////////////////////////////////////////////////////////////

		if (!isRecommendedItemFound) {
			List<JsonObject> ultraList = datas.stream().filter(x -> filterUltraItem(x, 8))
					.filter(x -> filterAvoidIn(x, disease)).filter(x -> filterByDietSeason(x))
					.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
					.collect(Collectors.toList());
			ultraList = filterByCustCommunity(ultraList, dbResult.getFilterData().getCommunity(), 8,
					" ULTRA SPECIAL SLOT ");

			slotFilter.setLocked(false);
			// ULTRA SPECIAL
			if (ListUtils.isNotEmpty(ultraList)) {
				JsonObject jsonObject = FoodFilterUtils.geFilteredData(ultraList, datas);
				if (null != jsonObject) {
					if (!result.get(0).getString("Type").equalsIgnoreCase(jsonObject.getString("Type")))
						if (!geFilteredItemChecked(result.get(0), addedList)
								|| !geFilteredItemChecked(jsonObject, addedList))
							result.add(jsonObject);
					// slotFilter.setLocked(true);
				}
			} else { // SPECIAL SLOT
				List<JsonObject> specialList = datas.stream().filter(x -> getDietBySlot(x, 8))
						.filter(x -> filterSpecialItem(x, 8)).filter(x -> filterAvoidIn(x, disease))
						.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
						.collect(Collectors.toList());
				specialList = filterByCustCommunity(specialList, dbResult.getFilterData().getCommunity(), 8,
						" SPECIAL SLOT ");
				if (ListUtils.isNotEmpty(specialList)) { // SPECIAL SLOT
					JsonObject jsonObject = FoodFilterUtils.geFilteredData(specialList, datas);
					if (!result.get(0).getString("Type").equalsIgnoreCase(jsonObject.getString("Type"))) {
						if (!geFilteredItemChecked(result.get(0), addedList)
								|| !geFilteredItemChecked(jsonObject, addedList))
							result.add(jsonObject);
					}
					slotFilter.setLocked(false);
				} else { // NON-SPECIAL SLOT
					selectedOptions = filterByCustCommunity(selectedOptions, dbResult.getFilterData().getCommunity(), 8,
							" NORMAL ");
					JsonObject jsonObject = FoodFilterUtils.geFilteredData(selectedOptions, datas);
					if (!result.get(0).getString("Type").equalsIgnoreCase(jsonObject.getString("Type")))
						if (!geFilteredItemChecked(result.get(0), addedList)
								|| !geFilteredItemChecked(jsonObject, addedList))
							result.add(jsonObject);

					slotFilter.setLocked(false);
				}
			}
		}

		result = result.stream().map(x -> {
			if ("173".equalsIgnoreCase(x.getString("_id")))
				x.put("portion", 0.5);
			return x;
		}).collect(Collectors.toList());

		slotFilter.setDataList(result);

		result.forEach(res -> logger.info("##### " + method + " [SLOT 8] FINAL RESULT DATA -->> " + res));
		logger.info("#########################################################################################");
		logger.info("");
		return slotFilter;
	}
	
	public static SlotFilter getSlot8ForTimingsNull(List<JsonObject> datas, DBResult dbResult, List<String> disease,
			List<JsonObject> selectedOptions, String traceId) {
		String method = "FoodFilterUtils getSlot8ForTimingsNull() " + traceId + "-["
				+ dbResult.getFilterData().getEmail() + "]";
		logger.info("#########################################################################################");
		logger.info("");
		SlotFilter slotFilter = new SlotFilter();
		List<JsonObject> result = new ArrayList<JsonObject>();
		if (null == selectedOptions) {
			selectedOptions = new ArrayList<JsonObject>();
		} else {
			selectedOptions = selectedOptions.stream().filter(x -> getDietBySlot(x, 8))
					.filter(x -> filterAvoidIn(x, disease)).filter(x -> filterByDietSeason(x))
					.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
					.filter(x -> (x.getString("Type").equalsIgnoreCase("D"))).collect(Collectors.toList());
		}

		List<JsonObject> ultraList = datas.stream().filter(x -> filterUltraItem(x, 8))
				.filter(x -> filterAvoidIn(x, disease)).filter(x -> filterByDietSeason(x))
				.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
				.filter(x -> (x.getString("Type").equalsIgnoreCase("D"))).collect(Collectors.toList());
		ultraList = filterByCustCommunity(ultraList, dbResult.getFilterData().getCommunity(), 8,
				" ULTRA SPECIAL SLOT ");

		// slotFilter.setLocked(false);
		// ULTRA SPECIAL
		if (ListUtils.isNotEmpty(ultraList)) {
			JsonObject jsonObject = FoodFilterUtils.geFilteredDataForSlot8TimingsAsNull(ultraList);
			if (null != jsonObject) {
				result.add(jsonObject);
				// slotFilter.setLocked(true);
			}
		} else { // SPECIAL SLOT
			List<JsonObject> specialList = datas.stream().filter(x -> getDietBySlot(x, 8))
					.filter(x -> filterSpecialItem(x, 8)).filter(x -> filterAvoidIn(x, disease))
					.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
					.filter(x -> (x.getString("Type").equalsIgnoreCase("D"))).collect(Collectors.toList());
			specialList = filterByCustCommunity(specialList, dbResult.getFilterData().getCommunity(), 8,
					" SPECIAL SLOT ");
			if (ListUtils.isNotEmpty(specialList)) { // SPECIAL SLOT
				JsonObject jsonObject = FoodFilterUtils.geFilteredDataForSlot8TimingsAsNull(specialList);
				result.add(jsonObject);
				slotFilter.setLocked(false);
			} else { // NON-SPECIAL SLOT
				selectedOptions = filterByCustCommunity(selectedOptions, dbResult.getFilterData().getCommunity(), 8,
						" NORMAL ");

				JsonObject jsonObject = FoodFilterUtils.geFilteredDataForSlot8TimingsAsNull(selectedOptions);
				result.add(jsonObject);
				slotFilter.setLocked(false);
			}
		}

		result = result.stream().map(x -> {
			updateCalories(x, x.getDouble("portion"));
			return x;
		}).collect(Collectors.toList());

		slotFilter.setDataList(result);

		result.forEach(res -> logger.info("##### " + method + " [SLOT 8] FINAL RESULT DATA -->> " + res));
		logger.info("#########################################################################################");
		logger.info("");
		return slotFilter;
	}
	
	public static SlotFilter getSlot8ForTimingsNullOld(List<JsonObject> datas, DBResult dbResult, List<String> disease,
			List<JsonObject> selectedOptions, String traceId) {
		String method = "FoodFilterUtils getSlot8ForTimingsNull() " + traceId + "-["
				+ dbResult.getFilterData().getEmail() + "]";
		logger.info("#########################################################################################");
		logger.info("");
		SlotFilter slotFilter = new SlotFilter();
		List<JsonObject> result = new ArrayList<JsonObject>();
		if (null == selectedOptions) {
			selectedOptions = new ArrayList<JsonObject>();
		} else {
			selectedOptions = selectedOptions.stream().filter(x -> getDietBySlot(x, 8))
					.filter(x -> filterAvoidIn(x, disease)).filter(x -> filterByDietSeason(x))
					.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
					.filter(x -> (x.getString("Type").equalsIgnoreCase("D"))).collect(Collectors.toList());
		}

		List<JsonObject> ultraList = datas.stream().filter(x -> filterUltraItem(x, 8))
				.filter(x -> filterAvoidIn(x, disease)).filter(x -> filterByDietSeason(x))
				.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
				.filter(x -> (x.getString("Type").equalsIgnoreCase("D"))).collect(Collectors.toList());
		ultraList = filterByCustCommunity(ultraList, dbResult.getFilterData().getCommunity(), 8,
				" ULTRA SPECIAL SLOT ");

		// slotFilter.setLocked(false);
		// ULTRA SPECIAL
		if (ListUtils.isNotEmpty(ultraList)) {
			JsonObject jsonObject = FoodFilterUtils.geFilteredDataForSlot8TimingsAsNull(ultraList);
			if (null != jsonObject) {
				result.add(jsonObject);
				// slotFilter.setLocked(true);
			}
		} else { // SPECIAL SLOT
			List<JsonObject> specialList = datas.stream().filter(x -> getDietBySlot(x, 8))
					.filter(x -> filterSpecialItem(x, 8)).filter(x -> filterAvoidIn(x, disease))
					.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
					.filter(x -> (x.getString("Type").equalsIgnoreCase("D"))).collect(Collectors.toList());
			specialList = filterByCustCommunity(specialList, dbResult.getFilterData().getCommunity(), 8,
					" SPECIAL SLOT ");
			if (ListUtils.isNotEmpty(specialList)) { // SPECIAL SLOT
				JsonObject jsonObject = FoodFilterUtils.geFilteredDataForSlot8TimingsAsNull(specialList);
				result.add(jsonObject);
				slotFilter.setLocked(false);
			} else { // NON-SPECIAL SLOT
				selectedOptions = filterByCustCommunity(selectedOptions, dbResult.getFilterData().getCommunity(), 8,
						" NORMAL ");

				JsonObject jsonObject = FoodFilterUtils.geFilteredDataForSlot8TimingsAsNull(selectedOptions);
				result.add(jsonObject);
				slotFilter.setLocked(false);
			}
		}

		result = result.stream().map(x -> {
			updateCalories(x, x.getDouble("portion"));
			return x;
		}).collect(Collectors.toList());

		slotFilter.setDataList(result);

		result.forEach(res -> logger.info("##### " + method + " [SLOT 8] FINAL RESULT DATA -->> " + res));
		logger.info("#########################################################################################");
		logger.info("");
		return slotFilter;
	}
	
	
	public static List<JsonObject> getSlot0Config(List<JsonObject> datas, DBResult dbResult, List<String> disease,
			List<String> communities, List<JsonObject> addedList) {
		String method = "getSlot0Config()";
		logger.info("#########################################################################################");
		logger.info("");
		List<JsonObject> result = new ArrayList<JsonObject>();
		datas = datas.stream().filter(x -> getDietBySlot(x, 0)).filter(x -> filterByDietSeason(x))
				.collect(Collectors.toList());
		datas = datas.stream().filter(x -> filterAvoidIn(x, disease))
				.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
				.collect(Collectors.toList());
		datas = filterByCustCommunity(datas, dbResult.getFilterData().getCommunity(), 0, "");
		logger.info("##### " + method + " SLOT 0 DATAS -->> " + datas);
		JsonObject drinkObj = null;

		if (disease != null) {
			List<JsonObject> diseaseDiets = FoodFilterUtils.filterByDiseaseRecommendedIn(datas, disease);
			// D/DM
			List<JsonObject> drink = diseaseDiets.stream().filter(
					x -> (x.getString("Type").equalsIgnoreCase("D") || x.getString("Type").equalsIgnoreCase("DM")))
					.filter(x -> filterByDietSeason(x)).collect(Collectors.toList());
			logger.info("##### " + method + " SLOT 0 DRINK - D -->> " + drink);

			drinkObj = getUltraSpecialItem(0, drink, dbResult, addedList);
			// CHECK ULTRA SPECIAL ITEM
			if (null != drinkObj) {
				result.add(drinkObj);
				logger.info("##### " + method + " SLOT 0 DRINKOBJ - D (ULTRA SPECIAL) -->> " + drinkObj);
			} else {
				// CHECK SPECIAL SLOT
				drinkObj = getSpecialSlotItem(0, drink, dbResult, addedList);
				if (null != drinkObj) {
					result.add(drinkObj);
					logger.info("##### " + method + " SLOT 0 DRINKOBJ - D (SPECIAL SLOT) -->> " + drinkObj);
				} else {
					// CHECK REMAINING ITEMS (FOODTYPE, AVOID DISEASES ITEMS, COMMUNITY TYPE)
					drinkObj = checkRemainingItems(0, drink, dbResult, addedList);
					if (null != drinkObj) {
						result.add(drinkObj);
						logger.info("##### " + method + " SLOT 0 DRINKOBJ - D (NORMAL) -->> " + drinkObj);
					} else {
						List<JsonObject> drinks = datas.stream().filter(x -> x.getString("Type").equalsIgnoreCase("D"))
								.filter(x -> filterByDietSeason(x)).collect(Collectors.toList());
						// CHECK ULTRA SPECIAL ITEM
						drinkObj = getUltraSpecialItem(0, drinks, dbResult, addedList);
						if (null != drinkObj) {
							result.add(drinkObj);
							logger.info(
									"##### " + method + " SLOT 0 DRINKOBJ - D 2ND (ULTRA SPECIAL) -->> " + drinkObj);
						} else {
							// CHECK SPECIAL SLOT
							drinkObj = getSpecialSlotItem(0, drinks, dbResult, addedList);
							if (null != drinkObj) {
								result.add(drinkObj);
								logger.info(
										"##### " + method + " SLOT 0 DRINKOBJ - D 2ND (SPECIAL SLOT) -->> " + drinkObj);
							} else {
								// CHECK REMAINING ITEMS (FOODTYPE, AVOID DISEASES ITEMS, COMMUNITY TYPE)
								drinkObj = checkRemainingItems(0, drinks, dbResult, addedList);
								if (null != drinkObj) {
									result.add(drinkObj);
									logger.info(
											"##### " + method + " SLOT 0 DRINKOBJ - D 2ND (NORMAL) -->> " + drinkObj);
								}
							}
						}
					}
				}
			}

			// A
			List<JsonObject> dishes = diseaseDiets.stream().filter(x -> x.getString("Type").equalsIgnoreCase("A"))
					.filter(x -> filterByDietSeason(x)).filter(x -> filterByDietSeason(x)).collect(Collectors.toList());
			logger.info("##### " + method + " SLOT 0 DISKES - A -->> " + dishes);
			JsonObject dishesObj = getUltraSpecialItem(0, dishes, dbResult, addedList);
			// CHECK ULTRA SPECIAL ITEM
			if (null != dishesObj) {
				result.add(dishesObj);
				logger.info("##### " + method + " SLOT 0 DISHESOBJ - A (ULTRA SPECIAL) -->> " + dishesObj);
			} else {
				// CHECK SPECIAL SLOT
				dishesObj = getSpecialSlotItem(0, dishes, dbResult, addedList);
				if (null != dishesObj) {
					result.add(dishesObj);
					logger.info("##### " + method + " SLOT 0 DISHESOBJ - A (SPECIAL SLOT) -->> " + dishesObj);
				} else {
					// CHECK REMAINING ITEMS (FOODTYPE, AVOID DISEASES ITEMS, COMMUNITY TYPE)
					dishesObj = checkRemainingItems(0, dishes, dbResult, addedList);
					if (null != dishesObj) {
						result.add(dishesObj);
						logger.info("##### " + method + " SLOT 0 DISHESOBJ - A (NORMAL) -->> " + dishesObj);
					}
				}
			}
		} else {
			List<JsonObject> drinks = datas.stream().filter(x -> x.getString("Type").equalsIgnoreCase("D"))
					.filter(x -> filterByDietSeason(x)).collect(Collectors.toList());
			logger.info("##### " + method + " SLOT 0 DRINKS 2ND - D -->> " + drinks);

			drinkObj = getUltraSpecialItem(0, drinks, dbResult, addedList);
			// CHECK ULTRA SPECIAL ITEM
			if (null != drinkObj) {
				result.add(drinkObj);
				logger.info("##### " + method + " SLOT 0 DRINKSOBJ - D (ULTRA SPECIAL) -->> " + drinkObj);
			} else {
				// CHECK SPECIAL SLOT
				drinkObj = getSpecialSlotItem(0, drinks, dbResult, addedList);
				if (null != drinkObj) {
					result.add(drinkObj);
					logger.info("##### " + method + " SLOT 0 DRINKSOBJ - D (SPECIAL SLOT) -->> " + drinkObj);
				} else {
					// CHECK REMAINING ITEMS (FOODTYPE, AVOID DISEASES ITEMS, COMMUNITY TYPE)
					drinkObj = checkRemainingItems(0, drinks, dbResult, addedList);
					if (null != drinkObj) {
						result.add(checkRemainingItems(0, drinks, dbResult, addedList));
						logger.info("##### " + method + " SLOT 0 DRINKSOBJ - D (NORMAL) -->> " + drinkObj);
					}
				}
			}
		}

		result = result.stream().map(x -> {
			updateCalories(x, x.getDouble("portion"));
			return x;
		}).collect(Collectors.toList());

		addedList.addAll(result);

		result.forEach(res -> logger.info("##### " + method + " SLOT 0 RESULT -->> " + res));
		logger.info("#########################################################################################");
		logger.info("");
		return result;
	}
	
	public static Map<String, List<JsonObject>> getSlotsDiets(List<JsonObject> datas, DBResult dbResult, List<String> disease,
			List<String> communities, List<JsonObject> selectedOptions, List<JsonObject> addedList,
			JsonArray slotsJsonObjectArray) {
		String method = "FoodFilterUtils getSlotsDiets()";
		logger.info("##### " + method + "TESTING.");
		Map<String, List<JsonObject>> map = new HashMap<String, List<JsonObject>>();
		Map<Integer, String> map1 = new HashMap<Integer, String>();
		for (int i = 0; i < slotsJsonObjectArray.size(); i++) {
			logger.info("##### " + method + " [" + 1 + "]");
			JsonObject json = slotsJsonObjectArray.getJsonObject(i);
			logger.info("##### " + method + "2 [" + 1 + "] -->> " + json);
			map1.put(json.getInteger("slot"), json.getString("value"));
			logger.info("##### " + method + "   3 -->> " + +json.getInteger("slot"));
			logger.info("##### " + method + "3333 -->> " + json.getString("value"));
//			logger.info(
//					"##### " + method + " SLOT [" + json.getInteger("slot") + "] :: VALUE [" + json.getInteger("value"));
		}

		// ITERATE SLOTS (ONE-BY-ONE)
		for (Map.Entry<Integer, String> entry : map1.entrySet()) {
			Integer slot = entry.getKey();
			logger.info("##### " + method + "4");
			String slotName = "SLOT " + entry.getKey();
			String slotOptions = entry.getValue();
			logger.info("###################################################");
			logger.info("# 			      " + slotName + "	                  #");
			logger.info("###################################################");

			String[] slotfoodArr = slotOptions.split(",");
			logger.info(
					"##### " + method + " SLOT [" + slot + "] CONFIGURED FOODS -->> " + Arrays.toString(slotfoodArr));
			List<String> list = new ArrayList<String>();
			for (String str : slotfoodArr)
				list.add(str);

			List<JsonObject> result = new ArrayList<JsonObject>();
			List<String> presentList = new ArrayList<String>();
			int index = 0;
			if (slot == 4 || slot == 7) {
				result.add(getMealByCode("034", datas));
				presentList.add("034");
			}
			
			// ITERATE SLOT WISE OPTIONS
			for (String type : list) {

				List<JsonObject> datas1 = datas.stream().filter(x -> getDietBySlot(x, slot))
						.filter(x -> getMultipleFoodItems(x, type)).filter(x -> filterAvoidIn(x, disease))
						.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
						.filter(x -> filterByCommunityFormulae(x, dbResult.getFilterData().getCommunity()))
						.filter(x -> filterByDietSeason(x)).collect(Collectors.toList());
				List<JsonObject> dietList = filterByCustCommunity(datas1, dbResult.getFilterData().getCommunity(), 1,
						"");
				logger.info("##### " + method + " SLOT [" + slot + "] DIET LIST            -->> " + dietList);

				List<JsonObject> selectedOptions1 = selectedOptions.stream().filter(x -> getMultipleFoodItems(x, type))
						.filter(x -> filterAvoidIn(x, disease)).filter(x -> getDietBySlot(x, 1))
						.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
						.filter(x -> filterByDietSeason(x)).collect(Collectors.toList());
				logger.info("##### " + method + " SLOT [" + slot + "] SELECTED OPTIONS 1 -->> " + selectedOptions1);

				JsonObject filteredObj = new JsonObject();
				if (ListUtils.isNotEmpty(dietList)) {
					filteredObj = getUltraSpecialItem(slot, dietList, dbResult, addedList);
					// CHECK ULTRA SPECIAL ITEM
					if (null != filteredObj) {
//						String mustItem = filteredObj.getString("mustItem");
//						if (null != mustItem && !"".equalsIgnoreCase(mustItem)) {
//							// GREEN SALAD
//							JsonObject json = getMealByCode(mustItem, datas);
//							if (!presentList.contains(json.getString("itemCode"))) {
//								result.add(json);
//								presentList.add(json.getString("itemCode"));
//							logger.info("##### " + method + " SLOT [" + slot + "]  [" + index
//									+ "] FILTERED OBJECT (ULTRA SPECIAL) MUST ITEM ADDED TO RESULT -->> " + json);
//							}
//						}

						String addon = filteredObj.getString("addon");
						if (null != addon && !"".equalsIgnoreCase(addon)) {
							JsonObject json = getMealByCode(addon, datas);
							if (!presentList.contains(json.getString("itemCode"))) {
								result.add(json);
								presentList.add(json.getString("itemCode"));
								logger.info("##### " + method + " SLOT [" + slot + "]  [" + index
										+ "] FILTERED OBJECT (ULTRA SPECIAL) ADDON ITEM ADDED TO RESULT -->> " + json);
							}
						}
						result.add(filteredObj);
						logger.info("##### " + method + " SLOT [" + slot + "]  [" + index
								+ "] FILTERED OBJECT (ULTRA SPECIAL) ADDED TO RESULT -->> " + filteredObj);
					} else {
						// CHECK SPECIAL SLOT
						filteredObj = getSpecialSlotItem(slot, dietList, dbResult, addedList);
						if (null != filteredObj) {
//							String mustItem = filteredObj.getString("mustItem");
//							if (null != mustItem && !"".equalsIgnoreCase(mustItem)) {
//								// GREEN SALAD
//								JsonObject json = getMealByCode(mustItem, datas);
//								if (null != json) {
//									if (!presentList.contains(json.getString("itemCode"))) {
//										result.add(json);
//										presentList.add(json.getString("itemCode"));
//										logger.info("##### " + method + " SLOT [" + slot + "]  [" + index
//												+ "] FILTERED OBJECT (SPECIAL SLOT) MUST ITEM ADDED TO RESULT -->> "
//												+ json);
//									}
//								}
//							}

							String addon = filteredObj.getString("addon");
							if (null != addon && !"".equalsIgnoreCase(addon)) {
								JsonObject json = getMealByCode(addon, datas);
								if (!presentList.contains(json.getString("itemCode"))) {
									result.add(json);
									presentList.add(json.getString("itemCode"));
									logger.info("##### " + method + " SLOT [" + slot + "]  [" + index
											+ "] FILTERED OBJECT (SPECIAL SLOT) ADDON ITEM ADDED TO RESULT -->> "
											+ json);
								}
							}
							result.add(filteredObj);
							logger.info("##### " + method + " SLOT [" + slot + "]  [" + index
									+ "] FILTERED OBJECT (SPECIAL SLOT) ADDED TO RESULT -->> " + filteredObj);
						} else {
							// CHECK REMAINING ITEMS (FOODTYPE, AVOID DISEASES ITEMS, COMMUNITY TYPE)
							filteredObj = checkRemainingItems(slot, dietList, dbResult, addedList);
							if (null != filteredObj) {
//								String mustItem = filteredObj.getString("mustItem");
//								if (null != mustItem && !"".equalsIgnoreCase(mustItem)) {
//									// GREEN SALAD
//									JsonObject json = getMealByCode(mustItem, datas);
//									if (null != json) {
//										if (!presentList.contains(json.getString("itemCode"))) {
//											result.add(json);
//											presentList.add(json.getString("itemCode"));
//											logger.info("##### " + method + " SLOT [" + slot + "]  [" + index
//													+ "] FILTERED OBJECT (NORMAL) MUST ITEM ADDED TO RESULT -->> "
//													+ json);
//										}
//									}
//								}

								String addon = filteredObj.getString("addon");
								if (null != addon && !"".equalsIgnoreCase(addon)) {
									JsonObject json = getMealByCode(addon, datas);
									if (null != json) {
										if (!presentList.contains(json.getString("itemCode"))) {
											result.add(json);
											presentList.add(json.getString("itemCode"));
											logger.info("##### " + method + " SLOT [" + slot + "]  [" + index
													+ "] FILTERED OBJECT (SPECIAL SLOT) ADDON ITEM ADDED TO RESULT -->> "
													+ json);
										}
									}
								}

								if (!presentList.contains(filteredObj.getString("itemCode"))) {
									presentList.add(filteredObj.getString("itemCode"));
									result.add(filteredObj);
									logger.info("##### " + method + " SLOT [" + slot + "]  [" + index
											+ "] FILTERED OBJECT (NORMAL) ADDED TO RESULT -->> " + filteredObj);
								}
							}
						}
					}
				} else {
					filteredObj = FoodFilterUtils.geFilteredData(selectedOptions1, addedList);
					if (null != filteredObj) {
//						String mustItem = filteredObj.getString("mustItem");
//						if (null != mustItem && !"".equalsIgnoreCase(mustItem)) {
//							// GREEN SALAD
//							JsonObject json = getMealByCode(mustItem, datas);
//							if (null != json) {
//								result.add(json);
//								logger.info("##### " + method + " SLOT [" + slot + "]  [" + index
//										+ "] FILTERED OBJECT (PREFERENCES) MUST ITEM ADDED TO RESULT -->> "
//										+ filteredObj);
//							}
//						}

						String addon = filteredObj.getString("addon");
						if (null != addon && !"".equalsIgnoreCase(addon)) {
							JsonObject json = getMealByCode(addon, datas);
							if (null != json) {
								if (!presentList.contains(filteredObj.getString("itemCode"))) {
									result.add(json);
									presentList.add(filteredObj.getString("itemCode"));
									logger.info("##### " + method + " SLOT [" + slot + "]  [" + index
											+ "] FILTERED OBJECT (PREFERENCES) ADDON ITEM ADDED TO RESULT -->> "
											+ filteredObj);
								}
							}
						}
						if (!presentList.contains(filteredObj.getString("itemCode"))) {
							result.add(filteredObj);
							presentList.add(filteredObj.getString("itemCode"));
							logger.info("##### " + method + " SLOT [" + slot + "] [" + index
									+ "]  FILTERED OBJECT (PREFERENCES) ADDED TO RESULT -->> " + filteredObj);
						}
					}
				}

				++index;
			}

			if (null != result && result.size() > 0)
				logger.info("##### " + method + " SLOT [" + slot + "] RESULT -->> " + result);

			if (null != result && result.size() > 0)
				map.put("slot" + slot, result);

			logger.info("##### " + method + " SLOT [" + slot + "] RESULT BEFORE -->> " + map.get("slot" + slot));

			addedList.addAll(map.get("slot" + slot));
			map.get("slot" + slot)
					.forEach(res -> logger.info("##### " + method + " SLOT [" + slot + "] RESULT -->> " + res));
		}

		logger.info("");
		return map;
	}
	

	public static List<JsonObject> getSlot1Config(List<JsonObject> datas, DBResult dbResult, List<String> disease,
			List<String> communities, List<JsonObject> selectedOptions, List<JsonObject> addedList, String slot1foods) {
		String method = "FoodFilterUtils getSlot1Config()";
		logger.info("#########################################################################################");
		logger.info("");
		String[] slot1foodsArr = slot1foods.split(",");
		logger.info("##### " + method + " SLOT 1 CONFIGURED FOODS -->> " + Arrays.toString(slot1foodsArr));
		List<String> list = new ArrayList<String>();
		for (String str : slot1foodsArr)
			list.add(str);
		
		List<JsonObject> result = new ArrayList<JsonObject>();
		for (String slotItem : list) {
			logger.info("##### " + method + " SLOT 1 [" + slotItem + "] DATAS -->> " + datas);
			datas = datas.stream().filter(x -> getDietBySlot(x, 1)).filter(x -> getMultipleFoodItems(x, slotItem))
					.filter(x -> filterAvoidIn(x, disease))
					.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
					.filter(x -> slot1foodsArr[0].trim().equalsIgnoreCase(x.getString("Type")))
					.filter(x -> filterByDietSeason(x)).collect(Collectors.toList());
			logger.info("##### " + method + " SLOT 1 [" + slotItem + "] DATAS -->> " + datas);

			selectedOptions = selectedOptions.stream().filter(x -> getMultipleFoodItems(x, slotItem))
					.filter(x -> filterAvoidIn(x, disease)).filter(x -> getDietBySlot(x, 1))
					.filter(x -> filterByDietSeason(x)).collect(Collectors.toList());
			logger.info("##### " + method + " SLOT 1 [" + slotItem + "] SELECTED OPTIONS -->> " + selectedOptions);

			List<JsonObject> fruites = datas.stream().filter(x -> filterSpecialItem(x, 1)).collect(Collectors.toList());
			fruites = filterByCustCommunity(fruites, dbResult.getFilterData().getCommunity(), 1, " SPECIAL SLOT ");
			if (ListUtils.isNotEmpty(fruites)) {
				JsonObject fruitesObj = getUltraSpecialItem(1, fruites, dbResult, addedList);
				// CHECK ULTRA SPECIAL ITEM
				if (null != fruitesObj) {
					result.add(fruitesObj);
					logger.info("##### " + method + " SLOT 1 [" + slotItem + "] FRUITS OBJECT (ULTRA SPECIAL) -->> "
							+ fruitesObj);
				} else {
					// CHECK SPECIAL SLOT
					fruitesObj = getSpecialSlotItem(1, fruites, dbResult, addedList);
					if (null != fruitesObj) {
						result.add(fruitesObj);
						logger.info("##### " + method + " SLOT 1 [" + slotItem + "] FRUITS OBJECT (SPECIAL SLOT) -->> "
								+ fruitesObj);
					} else {
						// CHECK REMAINING ITEMS (FOODTYPE, AVOID DISEASES ITEMS, COMMUNITY TYPE)
						fruitesObj = checkRemainingItems(1, fruites, dbResult, addedList);
						if (null != fruitesObj) {
							result.add(fruitesObj);
							logger.info("##### " + method + " SLOT 1 [" + slotItem + "] FRUITS OBJECT (NORMAL) -->> "
									+ fruitesObj);
						}
					}
				}
			} else {
				JsonObject json = checkRemainingItems(1, selectedOptions, dbResult, addedList);
				if (null != json) {
					result.add(json);
					logger.info("##### " + method + " SLOT 1 [" + slotItem
							+ "] FRUITS SELECTED OPTIONS JSON (ULTRA SPECIAL) -->> " + json);
				}
			}
		}
		
//		datas = datas.stream().filter(x -> getFoodItems(x, slot1foods)).filter(x -> getDietBySlot(x, 1))
//				.filter(x -> filterAvoidIn(x, disease))
//				.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
//				.filter(x -> slot1foodsArr[0].trim().equalsIgnoreCase(x.getString("Type")))
//				.filter(x -> filterByDietSeason(x)).collect(Collectors.toList());
//		logger.info("##### " + method + " SLOT 1 DATAS -->> " + datas);

//		selectedOptions = selectedOptions.stream().filter(x -> getFoodItems(x, slot1foods))
//				.filter(x -> filterAvoidIn(x, disease)).filter(x -> getDietBySlot(x, 1))
//				.filter(x -> filterByDietSeason(x)).collect(Collectors.toList());
//		selectedOptions = filterByCustCommunity(selectedOptions, dbResult.getFilterData().getCommunity(), 1,
//				" NON SPECIAL SLOT ");
//		logger.info("##### " + method + " SLOT 1 SELECTED OPTIONS -->> " + selectedOptions);

//		List<JsonObject> fruites = datas.stream().filter(x -> filterSpecialItem(x, 1)).collect(Collectors.toList());
//		fruites = filterByCustCommunity(fruites, dbResult.getFilterData().getCommunity(), 1, " SPECIAL SLOT ");
//		if (ListUtils.isNotEmpty(fruites)) {
//			JsonObject fruitesObj = getUltraSpecialItem(1, fruites, dbResult, addedList);
//			// CHECK ULTRA SPECIAL ITEM
//			if (null != fruitesObj) {
//				result.add(fruitesObj);
//				logger.info("##### " + method + " SLOT 1 FRUITS OBJECT (ULTRA SPECIAL) -->> " + fruitesObj);
//			} else {
//				// CHECK SPECIAL SLOT
//				fruitesObj = getSpecialSlotItem(1, fruites, dbResult, addedList);
//				if (null != fruitesObj) {
//					result.add(fruitesObj);
//					logger.info("##### " + method + " SLOT 1 FRUITS OBJECT (SPECIAL SLOT) -->> " + fruitesObj);
//				} else {
//					// CHECK REMAINING ITEMS (FOODTYPE, AVOID DISEASES ITEMS, COMMUNITY TYPE)
//					fruitesObj = checkRemainingItems(1, fruites, dbResult, addedList);
//					if (null != fruitesObj) {
//						result.add(fruitesObj);
//						logger.info("##### " + method + " SLOT 1 FRUITS OBJECT (NORMAL) -->> " + fruitesObj);
//					}
//				}
//			}
//		} else {
//			JsonObject json = checkRemainingItems(1, selectedOptions, dbResult, addedList);
//			if (null != json) {
//				result.add(json);
//				logger.info("##### " + method + " SLOT 1 FRUITS SELECTED OPTIONS JSON (ULTRA SPECIAL) -->> " + json);
//			}
//		}
		result = result.stream().map(x -> {
			updateCalories(x, x.getDouble("portion"));
			return x;
		}).collect(Collectors.toList());

		addedList.addAll(result);

		result.forEach(res -> logger.info("##### " + method + " SLOT 1 RESULT -->> " + res));
		logger.info("#########################################################################################");
		logger.info("");
		return result;
	}
	
	public static List<JsonObject> getSlot2Config(List<JsonObject> datas, DBResult dbResult, List<String> disease,
			List<JsonObject> selectedOptions, FilterData filterData, List<JsonObject> addedList, String slot2foods) {
		String method = "FoodFilterUtils getSlot2Config()";
		logger.info("#########################################################################################");
		logger.info("");		
		String[] slot2foodsArr = slot2foods.split(",");
		logger.info("##### " + method + " [SLOT 2] CONFIGURED FOODS -->> " + Arrays.toString(slot2foodsArr));
		List<String> list = new ArrayList<String>();
		for (String str : slot2foodsArr)
			list.add(str);

		List<JsonObject> result = new ArrayList<JsonObject>();
		for (String slotItem : list) {
			logger.info("##### " + method + " [SLOT 2] [" + slotItem + "] DATAS -->> " + datas);
			selectedOptions = selectedOptions.stream().filter(x -> getMultipleFoodItems(x, slotItem))
					.filter(x -> getDietBySlot(x, 2)).filter(x -> filterAvoidIn(x, disease))
					.filter(x -> filterByDietSeason(x))
					.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
					.collect(Collectors.toList());
			logger.info("##### " + method + "      [SLOT 2] [" + slotItem + "] SELECTED OPTIONS -->> " + selectedOptions);

			List<JsonObject> allW = selectedOptions.stream().filter(x -> getSnacks(x)).collect(Collectors.toList());
			allW = filterByCustCommunity(allW, dbResult.getFilterData().getCommunity(), 2, "");
			logger.info(
					"##### " + method + " [SLOT 2] [" + slotItem + "] PREFERENCES SELECTED OPTIONS (ALLW) -->> " + allW);
			JsonObject filterObject = getUltraSpecialItem(2, allW, dbResult, addedList);
			// CHECK ULTRA SPECIAL ITEM
			if (null != filterObject) {
				// ULTRA SPECIAL ITEMS
				logger.info("##### " + method + " [SLOT 2] [" + slotItem + "] FILTER OBJECT (ULTRA SPECIAL) -->> "
						+ filterObject);
			} else {
				// CHECK SPECIAL SLOT
				filterObject = getSpecialSlotItem(2, allW, dbResult, addedList);
				if (null != filterObject) {
					// SPECIAL SLOT ITEMS
					logger.info("##### " + method + " [SLOT 2] [" + slotItem + "] FILTER OBJECT (SPECIAL SLOT) -->> "
							+ filterObject);
				} else {
					// CHECK REMAINING ITEMS (FOODTYPE, AVOID DISEASES ITEMS, COMMUNITY TYPE)
					filterObject = checkRemainingItems(2, allW, dbResult, addedList);
					logger.info("##### " + method + " [SLOT 2] [" + slotItem + "] FILTER OBJECT (NORMAL) -->> "
							+ filterObject);
				}
			}

			logger.info("##### " + method + " [SLOT 2] slot2foodsArr[0]-->> " + slot2foodsArr[0].equalsIgnoreCase("W"));
			if (slot2foodsArr[2].trim().equalsIgnoreCase(filterObject.getString("Type"))
					|| slot2foodsArr[3].trim().equalsIgnoreCase(filterObject.getString("Type"))) {
				logger.info("##### " + method + " [SLOT 2] - " + filterObject.getString("Type") + " FOUND");
				String addon = filterObject.getString("addon");
				logger.info("##### " + method + " [SLOT 2] - ITEM 1 -->> " + addon);
				if (null != addon) {
					result.add(getMealByCode(addon, datas));
					logger.info("##### " + method + " [SLOT 2] - CURD (" + addon + ") ADDED");
				}
//					result.add(getMealByCode("165", datas));
			} else if (slot2foodsArr[1].trim().equalsIgnoreCase(filterObject.getString("Type"))
					|| slot2foodsArr[5].trim().equalsIgnoreCase(filterObject.getString("Type"))) {
				logger.info("##### " + method + " [SLOT 2] - " + filterObject.getString("Type") + " FOUND");
				filterObject.put("portion", 2);
				String addon = filterObject.getString("addon");
				logger.info("##### " + method + " [SLOT 2] - ITEM 2 -->> " + addon);
				if (null != addon) {
					result.add(getMealByCode(addon, datas));
					logger.info("##### " + method + " [SLOT 2] - GREEN CHATNI (" + addon + ") ADDED");
				}
//					result.add(getMealByCode("055", datas));
			} else if (slot2foodsArr[4].trim().equalsIgnoreCase(filterObject.getString("Type"))) {
				logger.info("##### " + method + " [SLOT 2] - " + filterObject.getString("Type") + " FOUND");
				String addon = filterObject.getString("addon");
				logger.info("##### " + method + " [SLOT 2] - ITEM 3 -->> " + addon);
				if (null != addon) {
					result.add(getMealByCode(addon, datas));
					logger.info("##### " + method + " [SLOT 2] - CHAPATI (" + addon + ") ADDED");
				}
//					result.add(getMealByCode("008", datas));

			}

			logger.info("##### " + method + " [SLOT 2] - FILTEREDOBJECT ADDED -->> " + filterObject);
			result.add(filterObject);

		}
		
		
		

//		selectedOptions = selectedOptions.stream().filter(x -> getDietBySlot(x, 2))
//				.filter(x -> filterAvoidIn(x, disease)).filter(x -> filterByDietSeason(x))
//				.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
//				.collect(Collectors.toList());
//		logger.info("##### " + method + "      SLOT 2 SELECTED OPTIONS -->> " + selectedOptions);
//
//		List<JsonObject> allW = selectedOptions.stream().filter(x -> getSnacks(x)).collect(Collectors.toList());
//		allW = filterByCustCommunity(allW, dbResult.getFilterData().getCommunity(), 2, "");
//		logger.info("##### " + method + " SLOT 2 PREFERENCES SELECTED OPTIONS (ALLW) -->> " + allW);
//		JsonObject filterObject = getUltraSpecialItem(2, allW, dbResult, addedList);
//		// CHECK ULTRA SPECIAL ITEM
//		if (null != filterObject) {
//			// ULTRA SPECIAL ITEMS
//			logger.info("##### " + method + " SLOT 2 FILTER OBJECT (ULTRA SPECIAL) -->> " + filterObject);
//		} else {
//			// CHECK SPECIAL SLOT
//			filterObject = getSpecialSlotItem(2, allW, dbResult, addedList);
//			if (null != filterObject) {
//				// SPECIAL SLOT ITEMS
//				logger.info("##### " + method + " SLOT 2 FILTER OBJECT (SPECIAL SLOT) -->> " + filterObject);
//			} else {
//				// CHECK REMAINING ITEMS (FOODTYPE, AVOID DISEASES ITEMS, COMMUNITY TYPE)
//				filterObject = checkRemainingItems(2, allW, dbResult, addedList);
//				logger.info("##### " + method + " SLOT 2 FILTER OBJECT (NORMAL) -->> " + filterObject);
//			}
//		}		
//
//		logger.info("##### " + method + " SLOT 2 slot2foodsArr[0]-->> " + slot2foodsArr[0].equalsIgnoreCase("W"));
//		if (slot2foodsArr[2].trim().equalsIgnoreCase(filterObject.getString("Type"))
//				|| slot2foodsArr[3].trim().equalsIgnoreCase(filterObject.getString("Type"))) {
//			logger.info("##### " + method + " SLOT 2 - " + filterObject.getString("Type") + " FOUND");
//			String addon = filterObject.getString("addon");
//			logger.info("##### " + method + " SLOT 2 - ITEM 1 -->> " + addon);
//			if (null != addon) {
//				result.add(getMealByCode(addon, datas));
//				logger.info("##### " + method + " SLOT 2 - CURD (" + addon + ") ADDED");
//			}
////				result.add(getMealByCode("165", datas));
//		} else if (slot2foodsArr[1].trim().equalsIgnoreCase(filterObject.getString("Type"))
//				|| slot2foodsArr[5].trim().equalsIgnoreCase(filterObject.getString("Type"))) {
//			logger.info("##### " + method + " SLOT 2 - " + filterObject.getString("Type") + " FOUND");
//			filterObject.put("portion", 2);
//			String addon = filterObject.getString("addon");
//			logger.info("##### " + method + " SLOT 2 - ITEM 2 -->> " + addon);
//			if (null != addon) {
//				result.add(getMealByCode(addon, datas));
//				logger.info("##### " + method + " SLOT 2 - GREEN CHATNI (" + addon + ") ADDED");
//			}
////				result.add(getMealByCode("055", datas));
//		} else if (slot2foodsArr[4].trim().equalsIgnoreCase(filterObject.getString("Type"))) {
//			logger.info("##### " + method + " SLOT 2 - " + filterObject.getString("Type") + " FOUND");
//			String addon = filterObject.getString("addon");
//			logger.info("##### " + method + " SLOT 2 - ITEM 3 -->> " + addon);
//			if (null != addon) {
//				result.add(getMealByCode(addon, datas));
//				logger.info("##### " + method + " SLOT 2 - CHAPATI (" + addon + ") ADDED");
//			}
////				result.add(getMealByCode("008", datas));
//
//		}
//
//		logger.info("##### " + method + " SLOT 2 - FILTEREDOBJECT ADDED -->> " + filterObject);
//		result.add(filterObject);
		addedList.addAll(result);

		result = result.stream().map(x -> {
			updateCalories(x, x.getDouble("portion"));
			return x;
		}).collect(Collectors.toList());

		result.forEach(res -> logger.info("##### " + method + " SLOT 2 RESULT -->> " + res));
		logger.info("#########################################################################################");
		logger.info("");
		return result;
	}
	
	/**
	 * Add Nuts (032) and Chach (033) in Ultra special,
	 * @param datas
	 * @param disease
	 * @param selectedOptions
	 * @param addedList
	 * @return
	 */
	public static List<JsonObject> getSlot3Config(List<JsonObject> datas1, DBResult dbResult, List<String> disease,
			List<JsonObject> selectedOptions, List<JsonObject> addedList, String slot3foods) {
		String method = "FoodFilterUtils getSlot3Config()";
		List<JsonObject> result = new ArrayList<JsonObject>();
		logger.info("#########################################################################################");
		logger.info("");
		String[] slot3foodsArr = slot3foods.split("/");
		String[] slot3foods2Arr = slot3foodsArr[0].split(",");
		logger.info(
				"##### " + method + "                [SLOT 3] CONFIGURED FOOD -->> " + Arrays.toString(slot3foodsArr));
		logger.info(
				"##### " + method + "              [SLOT 3] CONFIGURED FOOD 2 -->> " + Arrays.toString(slot3foods2Arr));

		selectedOptions.forEach(
				res -> logger.info("#####" + method + " [SLOT 3] SELECTED OPTIONS -->> " + res.getString("itemCode")));

		//////////////////// TO BE COMMENTED - START /////////////////////
		List<JsonObject> datas2 = datas1.stream().filter(x -> getDietBySlot(x, 3)).collect(Collectors.toList());
		logger.info("##### " + method + "                         [SLOT 3] DATAS2 -->> " + datas2);
		selectedOptions = selectedOptions.stream().filter(x -> getDietBySlot(x, 3)).collect(Collectors.toList());
		logger.info("##### " + method + "               [SLOT 3] SELECTED OPTIONS -->> " + selectedOptions);

		// D/DM
		List<JsonObject> datas = datas2.stream()
				.filter(x -> (slot3foods2Arr[1].trim().equalsIgnoreCase(x.getString("Type"))
						|| slot3foodsArr[0].trim().equalsIgnoreCase(x.getString("Type"))))
				.collect(Collectors.toList());
		logger.info("##### " + method + "                   [SLOT 3] DATAS [D/DM] -->> " + datas2);
		List<JsonObject> slot3ListUltra = datas.stream().filter(x -> filterUltraItem(x, 3))
				.collect(Collectors.toList());
		if (ListUtils.isNotEmpty(slot3ListUltra)) {
			result.add(FoodFilterUtils.geFilteredData(slot3ListUltra, addedList));
			logger.info("##### " + method + "         [SLOT 3] ULTRA SPECIAL ITEM -->> "
					+ FoodFilterUtils.geFilteredData(slot3ListUltra, addedList));
		} else {
			List<JsonObject> slot3List = datas.stream().filter(x -> filterSpecialItem(x, 3))
					.collect(Collectors.toList());
			if (ListUtils.isNotEmpty(slot3List)) {
				result.add(FoodFilterUtils.geFilteredData(slot3List, addedList));
				logger.info("##### " + method + "      [SLOT 3] SPECIAL SLOT ITEM -->> "
						+ FoodFilterUtils.geFilteredData(slot3List, addedList));
			} else {
				// D/DM
				List<JsonObject> selectedOptions1 = selectedOptions.stream()
						.filter(x -> slot3foods2Arr[0].trim().equalsIgnoreCase(x.getString("Type")))
						.collect(Collectors.toList());
				JsonObject filterObject = getUltraSpecialItem(2, selectedOptions1, dbResult, addedList);
				// CHECK ULTRA SPECIAL ITEM
				if (null != filterObject) {
					// ULTRA SPECIAL ITEMS
					result.add(filterObject);
					logger.info("##### " + method + " [SLOT 3] FILTER OBJECT (ULTRA SPECIAL) -->> " + filterObject);
				} else {
					// CHECK SPECIAL SLOT
					filterObject = getSpecialSlotItem(2, selectedOptions1, dbResult, addedList);
					if (null != filterObject) {
						// SPECIAL SLOT ITEMS
						result.add(filterObject);
						logger.info("##### " + method + " [SLOT 3] FILTER OBJECT (SPECIAL SLOT) -->> " + filterObject);
					} else {
						// CHECK REMAINING ITEMS (FOODTYPE, AVOID DISEASES ITEMS, COMMUNITY TYPE)
						filterObject = checkRemainingItems(2, selectedOptions1, dbResult, addedList);
						result.add(filterObject);
						logger.info("##### " + method + " [SLOT 3] FILTER OBJECT (NORMAL) -->> " + filterObject);
					}
				}

				logger.info("##### " + method + "            [SLOT 3] NORMAL ITEM -->> "
						+ FoodFilterUtils.geFilteredData(selectedOptions, addedList));
			}
		}

		// W
		List<JsonObject> datas3 = datas1.stream()
				.filter(x -> slot3foods2Arr[0].trim().equalsIgnoreCase(x.getString("Type")))
				.collect(Collectors.toList());
		logger.info("##### " + method + "                     [SLOT 3] DATAS3 [W] -->> " + datas3);
		List<JsonObject> slot3WListUltra = datas3.stream().filter(x -> filterUltraItem(x, 3))
				.collect(Collectors.toList());
		if (ListUtils.isNotEmpty(slot3WListUltra)) {
			result.add(FoodFilterUtils.geFilteredData(slot3WListUltra, addedList));
			logger.info("##### " + method + "     [SLOT 3] ULTRA SPECIAL ITEM [W] -->> "
					+ FoodFilterUtils.geFilteredData(slot3ListUltra, addedList));
		} else {
			List<JsonObject> slot3List = datas3.stream().filter(x -> filterSpecialItem(x, 3))
					.collect(Collectors.toList());
			if (ListUtils.isNotEmpty(slot3List)) {
				result.add(FoodFilterUtils.geFilteredData(slot3List, addedList));
				logger.info("##### " + method + "  [SLOT 3] SPECIAL SLOT ITEM [W] -->> "
						+ FoodFilterUtils.geFilteredData(slot3List, addedList));
			} else {
				// D/DM
				List<JsonObject> selectedOptions1 = selectedOptions.stream()
						.filter(x -> slot3foods2Arr[0].trim().equalsIgnoreCase(x.getString("Type")))
						.collect(Collectors.toList());
				JsonObject filterObject = getUltraSpecialItem(2, selectedOptions1, dbResult, addedList);
				// CHECK ULTRA SPECIAL ITEM
				if (null != filterObject) {
					// ULTRA SPECIAL ITEMS
					result.add(filterObject);
					logger.info("##### " + method + " [SLOT 3] FILTER OBJECT [W] (ULTRA SPECIAL) -->> " + filterObject);
				} else {
					// CHECK SPECIAL SLOT
					filterObject = getSpecialSlotItem(2, selectedOptions1, dbResult, addedList);
					if (null != filterObject) {
						// SPECIAL SLOT ITEMS
						result.add(filterObject);
						logger.info(
								"##### " + method + " [SLOT 3] FILTER OBJECT [W] (SPECIAL SLOT) -->> " + filterObject);
					} else {
						// CHECK REMAINING ITEMS (FOODTYPE, AVOID DISEASES ITEMS, COMMUNITY TYPE)
						filterObject = checkRemainingItems(2, selectedOptions1, dbResult, addedList);
						result.add(filterObject);
						logger.info("##### " + method + " [SLOT 3] FILTER OBJECT [W] (NORMAL) -->> " + filterObject);
					}
				}

				// result.add(FoodFilterUtils.geFilteredData(selectedOptions, addedList));
				logger.info("##### " + method + "        [SLOT 3] NORMAL ITEM [W] -->> "
						+ FoodFilterUtils.geFilteredData(selectedOptions, addedList));
			}
		}

		//////////////////// TO BE COMMENTED - END /////////////////////

		// result=datas.stream().filter(x->getDietBySlot(x, 3)).filter(x
		// ->filterAvoidIn(x, disease)).filter(x->filterByCustFoodType(x,
		// dbResult.getFilterData().getFoodType())).collect(Collectors.toList());
		// result = filterByCustCommunity(result,
		// dbResult.getFilterData().getCommunity(), 2, "");

//		String[] recF = { "033", "037" };
//		int index = ThreadLocalRandom.current().nextInt(recF.length);
//		result.add(getMealByCode(recF[index], datas));
//		result.add(getMealByCode("032", datas));

		result = result.stream().map(x -> {
			updateCalories(x, x.getDouble("portion"));
			return x;
		}).collect(Collectors.toList());

		result.forEach(res -> logger.info("##### " + method + "   [SLOT 3] RESULT -->> " + res));

		logger.info("#########################################################################################");
		logger.info("");
		return result;
	}
	
	public static List<JsonObject> getSlot4Config(List<JsonObject> datas, DBResult dbResult, List<String> disease,
			List<JsonObject> selectedOptions, FilterData filterData, List<JsonObject> addedList, String slot4foods) {
		String method = "FoodFilterUtils getSlot4Config()";
		logger.info("#########################################################################################");
		logger.info("");
		String[] slot4foodsArr = slot4foods.split("/");
		String[] slot4foodsArr2 = slot4foods.split(",");
		logger.info("##### " + method + "  SLOT 4 CONFIGURED FOOD -->> " + Arrays.toString(slot4foodsArr));
		List<JsonObject> result = new ArrayList<JsonObject>();

		// GREEN SALAD
		//result.add(getMealByCode("034", datas));

		String mustItem = "";
		boolean isMustItemAdded = false;
		
		selectedOptions = selectedOptions.stream().filter(x -> getDietBySlot(x, 4))
				.filter(x -> filterAvoidIn(x, disease)).filter(x -> filterByDietSeason(x))
				.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
				.collect(Collectors.toList());
		logger.info("##### " + method + " SLOT 4 SELECTED OPTIONS -->> " + selectedOptions);

		// C or A
		List<JsonObject> slot4ListC = selectedOptions.stream()
				.filter(x -> x.getString("Type").equalsIgnoreCase(slot4foodsArr2[0].trim()) || x.getString("Type").equalsIgnoreCase(slot4foodsArr[0].trim()))
				.collect(Collectors.toList());
		logger.info("##### " + method + "  SLOT 4 slot4ListC SIZE -->> "
				+ ((null != slot4ListC && slot4ListC.size() > 0) ? slot4ListC.size() : 0));
		logger.info("##### " + method + "       SLOT 4 slot4ListC -->> " + slot4ListC);
		JsonObject resObjC = getUltraSpecialItem(4, slot4ListC, dbResult, addedList);
		// CHECK ULTRA SPECIAL ITEM
		if (null != resObjC) {
			// ULTRA SPECIAL ITEMS
			mustItem = resObjC.getString("mustItem");
			// GREEN SALAD
			result.add(getMealByCode(mustItem, datas));
			result.add(resObjC);
			isMustItemAdded = true;
			result.add(resObjC);
			logger.info("##### " + method + " SLOT 4 RES-OBJ-C (ULTRA SPECIAL) -->> " + resObjC);
		} else {
			// CHECK SPECIAL SLOT
			resObjC = getSpecialSlotItem(4, slot4ListC, dbResult, addedList);
			if (null != resObjC) {
				// SPECIAL SLOT ITEMS
				mustItem = resObjC.getString("mustItem");
				// GREEN SALAD
				result.add(getMealByCode(mustItem, datas));
				result.add(resObjC);
				isMustItemAdded = true;
				result.add(resObjC);
				logger.info("##### " + method + " SLOT 4 RES-OBJ-C (SPECIAL SLOT) -->> " + resObjC);
			} else {
				// CHECK REMAINING ITEMS (FOODTYPE, AVOID DISEASES ITEMS, COMMUNITY TYPE)
				resObjC = checkRemainingItems(4, slot4ListC, dbResult, addedList);
				mustItem = resObjC.getString("mustItem");
				// GREEN SALAD
				result.add(getMealByCode(mustItem, datas));
				result.add(resObjC);
				isMustItemAdded = true;
				result.add(resObjC);
				logger.info("##### " + method + " SLOT 4 RES-OBJ-C (NORMAL) -->> " + resObjC);
			}
		}
		
		// B
		logger.info("##### " + method + " STREAMMMMMMMMMMM -->> " + slot4foodsArr2[1].trim());
		List<JsonObject> slot4ListB = selectedOptions.stream().filter(x -> x.getString("Type").equalsIgnoreCase(slot4foodsArr2[1].trim()))
				.filter(x -> filterByDietSeason(x)).collect(Collectors.toList());
		logger.info("##### " + method + " SLOT 4 slot4ListB SIZE -->> "
				+ ((null != slot4ListB && slot4ListB.size() > 0) ? slot4ListB.size() : 0));
		logger.info("##### " + method + "      SLOT 4 slot4ListB -->> " + slot4ListB);
		JsonObject resObjB = getUltraSpecialItem(4, slot4ListB, dbResult, addedList);
		logger.info("##### " + method + " SLOT 4 RES-OBJ-B (ULTRA SPECIAL) -->> " + resObjB);
		// CHECK ULTRA SPECIAL ITEM
		if (null != resObjB) {
			// ULTRA SPECIAL ITEMS
			if (!isMustItemAdded) {
				mustItem = resObjB.getString("mustItem");
				// GREEN SALAD
				result.add(getMealByCode(mustItem, datas));
			}
			result.add(resObjB);
		} else {
			// CHECK SPECIAL SLOT
			resObjB = getSpecialSlotItem(4, slot4ListB, dbResult, addedList);
			if (null != resObjB) {
				// SPECIAL SLOT ITEMS
				if (!isMustItemAdded) {
					mustItem = resObjB.getString("mustItem");
					// GREEN SALAD
					result.add(getMealByCode(mustItem, datas));
				}
				result.add(resObjB);
				logger.info("##### " + method + " SLOT 4 RES-OBJ-B (SPECIAL SLOT) -->> " + resObjB);
			} else {
				// CHECK REMAINING ITEMS (FOODTYPE, AVOID DISEASES ITEMS, COMMUNITY TYPE)
				resObjB = checkRemainingItems(4, slot4ListB, dbResult, addedList);
				if (!isMustItemAdded) {
					mustItem = resObjB.getString("mustItem");
					// GREEN SALAD
					result.add(getMealByCode(mustItem, datas));
				}
				result.add(resObjB);
				logger.info("##### " + method + " SLOT 4 RES-OBJ-B (NORMAL) -->> " + resObjB);
			}
		}

		addedList.addAll(result);

		result = result.stream().map(x -> {
			updateCalories(x, x.getDouble("portion"));
			return x;
		}).collect(Collectors.toList());

		result.forEach(res -> logger.info("##### " + method + " SLOT 4 RESULT -->> " + res));
		logger.info("#########################################################################################");
		logger.info("");
		return result;
	}
	
	public static SlotFilter getSlot5Config(List<JsonObject> datas, DBResult dbResult, List<String> disease,
			List<JsonObject> selectedOptions, List<JsonObject> addedList, String slot5foods) {
		String method = "FoodFilterUtils getSlot5Config()";
		logger.info("#########################################################################################");
		logger.info("");
		String[] slot5foodsArr = slot5foods.split("/");
		logger.info("##### " + method + " SLOT 5 CONFIGURED FOOD -->> " + Arrays.toString(slot5foodsArr));
		SlotFilter slotFilter = new SlotFilter();
		List<JsonObject> result = new ArrayList<JsonObject>();
		selectedOptions = selectedOptions.stream().filter(x -> getFoodItems(x, slot5foods))
				.filter(x -> getDietBySlot(x, 5)).collect(Collectors.toList());
		selectedOptions = selectedOptions.stream().filter(x -> filterAvoidIn(x, disease))
				.filter(x -> filterByDietSeason(x))
				.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
				.collect(Collectors.toList());
		selectedOptions = filterByCustCommunity(selectedOptions, dbResult.getFilterData().getCommunity(), 5,
				" NORMAL ");
		logger.info("##### " + method + " SLOT 5 PREFERENCES (SELECTED OPTIONS) -->> " + selectedOptions);

		List<JsonObject> slot5List = datas.stream().filter(x -> getDietBySlot(x, 5)).filter(x -> filterByDietSeason(x))
				.collect(Collectors.toList());
		slot5List = slot5List.stream().filter(x -> slot5foods.equalsIgnoreCase(x.getString("Type"))).filter(x -> filterAvoidIn(x, disease))
				.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
				.collect(Collectors.toList());
		logger.info("##### " + method + " SLOT 5 SLOT5LIST -->> " + slot5List);
		
		JsonObject filteredData = getUltraSpecialItem(5, slot5List, dbResult, addedList);
		// CHECK ULTRA SPECIAL ITEM
		if (null != filteredData) {
			// ULTRA SPECIAL ITEMS
			result.add(filteredData);
			logger.info("##### " + method + " SLOT 5 FILTERED DATA (ULTRA SPECIAL) -->> " + filteredData);
			slotFilter.setLocked(true);
		} else {
			// CHECK SPECIAL SLOT
			filteredData = getSpecialSlotItem(5, slot5List, dbResult, addedList);
			if (null != filteredData) {
				// SPECIAL SLOT ITEMS
				result.add(filteredData);
				logger.info("##### " + method + " SLOT 5 FILTERED DATA (SPECIAL SLOT) -->> " + filteredData);
			} else {
				// CHECK REMAINING ITEMS (FOODTYPE, AVOID DISEASES ITEMS, COMMUNITY TYPE)
				filteredData = checkRemainingItems(5, selectedOptions, dbResult, addedList);
				result.add(filteredData);
				logger.info("##### " + method + " SLOT 5 FILTERED DATA (NORMAL) -->> " + filteredData);
			}
		}

		result = result.stream().map(x -> {
			updateCalories(x, x.getDouble("portion"));
			return x;
		}).collect(Collectors.toList());

		addedList.addAll(result);
		slotFilter.setDataList(result);

		result.forEach(res -> logger.info("##### " + method + " SLOT 5 RESULT -->> " + res));
		logger.info("#########################################################################################");
		logger.info("");
		return slotFilter;
	}
	
	public static List<JsonObject> getSlot6Config(List<JsonObject> datas, DBResult dbResult, List<String> disease,
			List<JsonObject> selectedOptions, List<JsonObject> addedList, String slot6foods) {
		String method = "FoodFilterUtils getSlot6Config()";
		logger.info("#########################################################################################");
		logger.info("");
		String[] slot6foodsArr = slot6foods.split("/");
		logger.info("##### " + method + " SLOT 6 CONFIGURED FOOD -->> " + Arrays.toString(slot6foodsArr));
		List<JsonObject> result = new ArrayList<JsonObject>();
		selectedOptions = selectedOptions.stream().filter(x -> getDietBySlot(x, 6)).filter(x -> getSnacks(x))
				.filter(x -> filterAvoidIn(x, disease)).filter(x -> filterByDietSeason(x))
				.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
				.collect(Collectors.toList());
		logger.info("##### " + method + " SLOT 6 SELECTED OPTIONS -->> " + selectedOptions);
		List<JsonObject> slot6List = datas.stream().filter(x -> getDietBySlot(x, 6))
				.filter(x -> filterAvoidIn(x, disease)).filter(x -> filterByDietSeason(x))
				.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
				.filter(x -> (slot6foodsArr[0].trim().equalsIgnoreCase(x.getString("Type"))
						|| slot6foodsArr[1].trim().equalsIgnoreCase(x.getString("Type"))))
				.collect(Collectors.toList());
		logger.info("##### " + method + " SLOT 6 SLOT6LIST -->> " + slot6List);
		
		//.filter(x -> (slot6foodsArr[0].trim().equalsIgnoreCase(x.getString("Type")) || slot6foodsArr[1].trim().equalsIgnoreCase(x.getString("Type")))
		//////////
		JsonObject filteredData = getUltraSpecialItem(6, slot6List, dbResult, addedList);
		// CHECK ULTRA SPECIAL ITEM
		if (null != filteredData) {
			// ULTRA SPECIAL ITEMS
			result.add(filteredData);
			logger.info("##### " + method + " SLOT 6 FILTERED DATA (ULTRA SPECIAL) -->> " + filteredData);
		} else {
			// CHECK SPECIAL SLOT
			filteredData = getSpecialSlotItem(6, slot6List, dbResult, addedList);
			if (null != filteredData) {
				// SPECIAL SLOT ITEMS
				result.add(filteredData);
				logger.info("##### " + method + " SLOT 6 FILTERED DATA (SPECIAL SLOT) -->> " + filteredData);
			} else {
				// CHECK REMAINING ITEMS (FOODTYPE, AVOID DISEASES ITEMS, COMMUNITY TYPE)
				// CHECK ULTRA SPECIAL ITEM
				filteredData = getUltraSpecialItem(6, selectedOptions, dbResult, addedList);
				if (null != filteredData) {
					// ULTRA SPECIAL ITEMS
					result.add(filteredData);
					logger.info("##### " + method + " SLOT 6 SELECTED OPTIONS FILTERED DATA (ULTRA SPECIAL) -->> "
							+ filteredData);
				} else {
					// CHECK SPECIAL SLOT
					filteredData = getSpecialSlotItem(6, selectedOptions, dbResult, addedList);
					if (null != filteredData) {
						// SPECIAL SLOT ITEMS
						result.add(filteredData);
						logger.info("##### " + method + " SLOT 6 SELECTED OPTIONS FILTERED DATA (SPECIAL SLOT) -->> "
								+ filteredData);
					} else {
						// CHECK REMAINING ITEMS (FOODTYPE, AVOID DISEASES ITEMS, COMMUNITY TYPE)
						filteredData = checkRemainingItems(6, selectedOptions, dbResult, addedList);
						result.add(filteredData);
						logger.info("##### " + method + " SLOT 6 SELECTED OPTIONS FILTERED DATA (NORMAL) -->> "
								+ filteredData);
					}
				}
			}
		}

		result = result.stream().map(x -> {
			updateCalories(x, x.getDouble("portion"));
			return x;
		}).collect(Collectors.toList());

		result.forEach(res -> logger.info("##### " + method + " SLOT 6 RESULT -->> " + res));
		logger.info("#########################################################################################");
		logger.info("");
		return result;
	}
	
	public static List<JsonObject> getSlot7Config(List<JsonObject> datas1, DBResult dbResult, List<String> disease,
			List<JsonObject> selectedOptions, List<JsonObject> addedList, String slot7foods) {
		String method = "FoodFilterUtils getSlot7Config()";
		logger.info("#########################################################################################");
		logger.info("");
		String[] slot7foodsArr = slot7foods.split("/");
		logger.info("##### " + method + " SLOT 7 CONFIGURED FOOD -->> " + Arrays.toString(slot7foodsArr));
		String mustItem = "";
		boolean isMustItemAdded = false;
		List<JsonObject> result = new ArrayList<JsonObject>();

		//JsonObject salad = getMealByCode("034", datas);
		//result.add(salad);
		//logger.info("##### " + method + " SLOT 7 SALAD ADDED -->> " + salad);

		// W/WC/WCP/WE/F/C
		logger.info("##### " + method + " SLOT 7 BEFORE INITIAL -->> " + datas1);
		List<JsonObject> datas = datas1.stream().filter(x -> getDietBySlot(x, 7)).filter(x -> filterAvoidIn(x, disease))
				.filter(x -> filterByDietSeason(x))
				.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
				.collect(Collectors.toList());
		logger.info("##### " + method + " SLOT 7 BEFORE -->> " + datas);
		datas = getPrefListFromCommunity("SLOT 7", datas, dbResult.getFilterData().getCommunity());
		logger.info("##### " + method + " SLOT 7 DATAS -->> " + datas);
		selectedOptions = selectedOptions.stream().filter(x -> getDietBySlot(x, 7))
				.filter(x -> filterAvoidIn(x, disease)).filter(x -> filterByDietSeason(x))
				.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
				.collect(Collectors.toList());
		selectedOptions = getPrefListFromCommunity("SLOT 7", selectedOptions, dbResult.getFilterData().getCommunity());
		logger.info("##### " + method + " SLOT 7 SELECTED OPTIONS -->> " + selectedOptions);

		JsonObject filteredData = getUltraSpecialItem(7, datas, dbResult, addedList);
		// CHECK ULTRA SPECIAL ITEM
		if (null != filteredData) {
			mustItem = filteredData.getString("mustItem");
			// GREEN SALAD
			JsonObject json = getMealByCode(mustItem, datas1);
			if (null != json) {
				result.add(json);
				isMustItemAdded = true;
			}
			// ULTRA SPECIAL ITEMS
			result.add(filteredData);
			logger.info("##### " + method + " SLOT 7 DATAS FILTERED OBJECT (ULTRA SPECIAL) -->> " + filteredData);
		} else {
			// CHECK SPECIAL SLOT
			filteredData = getSpecialSlotItem(7, datas, dbResult, addedList);
			if (null != filteredData) {
				mustItem = filteredData.getString("mustItem");
				logger.info("##### " + method + " SLOT 7 MUST ITEM -->> " + mustItem);
				// GREEN SALAD
				JsonObject json = getMealByCode(mustItem, datas1);
				if (null != json) {
					result.add(json);
					isMustItemAdded = true;
				}
				logger.info("##### " + method + " SLOT 7 getMealByCode(mustItem, datas) -->> " + getMealByCode(mustItem, datas));
				//isMustItemAdded = true;
				// SPECIAL SLOT ITEMS
				result.add(filteredData);
				logger.info("##### " + method + " SLOT 7 DATAS FILTERED OBJECT (SPECIAL SLOT) -->> " + filteredData);
			} else {
				// CHECK REMAINING ITEMS (FOODTYPE, AVOID DISEASES ITEMS, COMMUNITY TYPE)
				// A/C
				List<JsonObject> listC = selectedOptions.stream().filter(
						x -> x.getString("Type").equalsIgnoreCase("A") || x.getString("Type").equalsIgnoreCase("C"))
						.collect(Collectors.toList());
				listC = filterByCustCommunity(listC, dbResult.getFilterData().getCommunity(), 7,
						" NON SPECIAL SLOT (A/C) ");
				logger.info("##### " + method + " SLOT 7 LIST-C -->> " + listC);

				filteredData = getUltraSpecialItem(7, listC, dbResult, addedList);
				// CHECK ULTRA SPECIAL ITEM
				if (null != filteredData) {
					mustItem = filteredData.getString("mustItem");
					// GREEN SALAD
					JsonObject json = getMealByCode(mustItem, datas1);
					if (null != json) {
						result.add(json);
						isMustItemAdded = true;
					}
					// ULTRA SPECIAL ITEMS
					result.add(filteredData);
					logger.info(
							"##### " + method + " SLOT 7 LIST-C FILTERED OBJECT (ULTRA SPECIAL) -->> " + filteredData);
				} else {
					// CHECK SPECIAL SLOT
					filteredData = getSpecialSlotItem(7, listC, dbResult, addedList);
					if (null != filteredData) {
						mustItem = filteredData.getString("mustItem");
						// GREEN SALAD
						JsonObject json = getMealByCode(mustItem, datas1);
						if (null != json) {
							result.add(json);
							isMustItemAdded = true;
						}
						// SPECIAL SLOT ITEMS
						result.add(filteredData);
						logger.info("##### " + method + " SLOT 7 LIST-C FILTERED OBJECT (SPECIAL SLOT) -->> "
								+ filteredData);
					} else {
						// CHECK REMAINING ITEMS (FOODTYPE, AVOID DISEASES ITEMS, COMMUNITY TYPE)
						filteredData = checkRemainingItems(7, listC, dbResult, addedList);
						mustItem = filteredData.getString("mustItem");
						// GREEN SALAD
						JsonObject json = getMealByCode(mustItem, datas1);
						if (null != json) {
							result.add(json);
							isMustItemAdded = true;
						}
						result.add(filteredData);
						logger.info("##### " + method + " SLOT 7 LIST-C FILTERED DATA (NORMAL) -->> " + filteredData);
					}
				}

				// B
				List<JsonObject> listB = selectedOptions.stream().filter(x -> x.getString("Type").equalsIgnoreCase("B"))
						.collect(Collectors.toList());
				listB = filterByCustCommunity(listC, dbResult.getFilterData().getCommunity(), 7,
						" NON SPECIAL SLOT TYPE - B ");
				logger.info("##### " + method + " SLOT 7 LIST-B -->> " + listB);
				filteredData = getUltraSpecialItem(7, listB, dbResult, addedList);
				// CHECK ULTRA SPECIAL ITEM
				if (null != filteredData) {
					if (!isMustItemAdded) {
						mustItem = filteredData.getString("mustItem");
						// GREEN SALAD
						JsonObject json = getMealByCode(mustItem, datas1);
						if (null != mustItem) {
							result.add(json);
							isMustItemAdded = true;
						}
					}
					// ULTRA SPECIAL ITEMS
					result.add(filteredData);
					logger.info(
							"##### " + method + " SLOT 7 LIST-B FILTERED DATA (ULTRA SPECIAL) -->> " + filteredData);
				} else {
					// CHECK SPECIAL SLOT
					filteredData = getSpecialSlotItem(7, listB, dbResult, addedList);
					if (null != filteredData) {
						if (!isMustItemAdded) {
							mustItem = filteredData.getString("mustItem");
							// GREEN SALAD
							JsonObject json = getMealByCode(mustItem, datas1);
							if (null != json) {
								result.add(json);
								isMustItemAdded = true;
							}
						}
						// SPECIAL SLOT ITEMS
						result.add(filteredData);
						logger.info(
								"##### " + method + " SLOT 7 LIST-B FILTERED DATA (SPECIAL SLOT) -->> " + filteredData);
					} else {
						// CHECK REMAINING ITEMS (FOODTYPE, AVOID DISEASES ITEMS, COMMUNITY TYPE)
						filteredData = checkRemainingItems(7, listB, dbResult, addedList);
						if (!isMustItemAdded) {
							mustItem = filteredData.getString("mustItem");
							// GREEN SALAD
							JsonObject json = getMealByCode(mustItem, datas1);
							if (null != json) {
								result.add(json);
								isMustItemAdded = true;
							}
						}
						result.add(filteredData);
						logger.info("##### " + method + " SLOT 7 LIST-B FILTERED DATA (NORMAL) -->> " + filteredData);
					}
				}
			}
		}

		logger.info("##### " + method + " SLOT 7 DONE 1");
		addedList.addAll(result);
		logger.info("##### " + method + " SLOT 7 DONE 2");
		
		result.forEach(res -> logger.info("##### " + method + " SLOT 7 RESULT -->> " + res));
		logger.info("##### " + method + " SLOT 7 DONE 33333");
		

		result = result.stream().map(x -> {
			if (null != x)
				updateCalories(x, x.getDouble("portion"));
			return x;
		}).collect(Collectors.toList());
		logger.info("##### " + method + " SLOT 7 DONE 3");

		result.forEach(res -> logger.info("##### " + method + " SLOT 7 RESULT -->> " + res));
		logger.info("#########################################################################################");
		logger.info("");
		return result;
	}
	
	public static SlotFilter getSlot8Config(List<JsonObject> datas, DBResult dbResult, List<String> disease,
			List<JsonObject> selectedOptions, List<JsonObject> addedList, String slot8foods) {
		String method = "FoodFilterUtils getSlot8Config()";
		logger.info("#########################################################################################");
		logger.info("");
		String[] slot8foodsArr = slot8foods.split("/");
		logger.info("##### " + method + " SLOT 8 CONFIGURED FOOD -->> " + Arrays.toString(slot8foodsArr));
		SlotFilter slotFilter = new SlotFilter();
		List<JsonObject> result = new ArrayList<JsonObject>();
		selectedOptions = selectedOptions.stream().filter(x -> getFoodItems(x, slot8foodsArr[0].trim()))
				.filter(x -> getDietBySlot(x, 8)).filter(x -> filterAvoidIn(x, disease))
				.filter(x -> filterByDietSeason(x))
				.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
				.collect(Collectors.toList());
		logger.info("##### " + method + " SLOT 8 SELECTED OPTIONS -->> " + selectedOptions);

		List<JsonObject> slot8List = datas.stream().filter(x -> getDietBySlot(x, 8))
				.filter(x -> filterAvoidIn(x, disease)).filter(x -> filterByDietSeason(x))
				.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
				//.filter(x -> slot8foods.equalsIgnoreCase(x.getString("Type")))
				.collect(Collectors.toList());
		slot8List = filterByCustCommunity(slot8List, dbResult.getFilterData().getCommunity(), 8,
				" ULTRA SPECIAL SLOT ");
		logger.info("##### " + method + " SLOT 8 SLOT8LIST -->> " + slot8List);

		//////////
		JsonObject filteredData = getUltraSpecialItem(8, slot8List, dbResult, addedList);
		// CHECK ULTRA SPECIAL ITEM
		if (null != filteredData) {
			// ULTRA SPECIAL ITEMS
			result.add(filteredData);
			logger.info("##### " + method + " SLOT 8 SLOT8LIST - FILTERED DATA (ULTRA SPECIAL) -->> " + filteredData);
		} else {
			// CHECK SPECIAL SLOT
			filteredData = getSpecialSlotItem(8, slot8List, dbResult, addedList);
			if (null != filteredData) {
				// SPECIAL SLOT ITEMS
				result.add(filteredData);
				logger.info(
						"##### " + method + " SLOT 8 SLOT8LIST - FILTERED DATA (SPECIAL SLOT) -->> " + filteredData);
			} else {
				// CHECK REMAINING ITEMS (FOODTYPE, AVOID DISEASES ITEMS, COMMUNITY TYPE)
				filteredData = getUltraSpecialItem(8, selectedOptions, dbResult, addedList);
				// CHECK ULTRA SPECIAL ITEM
				if (null != filteredData) {
					// ULTRA SPECIAL ITEMS
					result.add(filteredData);
					logger.info("##### " + method + " SLOT 8 SELECTED OPTIONS - FILTERED DATA (ULTRA SPECIAL) -->> "
							+ filteredData);
				} else {
					// CHECK SPECIAL SLOT
					filteredData = getSpecialSlotItem(8, selectedOptions, dbResult, addedList);
					if (null != filteredData) {
						// SPECIAL SLOT ITEMS
						result.add(filteredData);
						logger.info("##### " + method + " SLOT 8 SELECTED OPTIONS - FILTERED DATA (SPECIAL SLOT) -->> "
								+ filteredData);
					} else {
						// CHECK REMAINING ITEMS (FOODTYPE, AVOID DISEASES ITEMS, COMMUNITY TYPE)
						filteredData = checkRemainingItems(8, selectedOptions, dbResult, addedList);
						result.add(filteredData);
						logger.info("##### " + method + " SLOT 8 SELECTED OPTIONS - FILTERED DATA (NORMAL) -->> "
								+ filteredData);
					}
				}
			}
		}

		result = result.stream().map(x -> {
			updateCalories(x, x.getDouble("portion"));
			return x;
		}).collect(Collectors.toList());

		slotFilter.setDataList(result);

		result.forEach(res -> logger.info("##### " + method + " SLOT 8 RESULT -->> " + res));
		logger.info("#########################################################################################");
		logger.info("");
		return slotFilter;
	}
	
	public static List<JsonObject> getPrefListFromCommunity(String slot, List<JsonObject> selectedOptions, List<String> communities) {
		List<JsonObject> preferencesList = new ArrayList<JsonObject>();
		String method = "FoodFilterUtils getPrefListFromCommunity() ";
		String community = "";
		for (String str : communities)
			if (!"U".equalsIgnoreCase(str))
				community = str;
		
		for (JsonObject json : selectedOptions) {
			JsonArray communityArray = json.getJsonArray("Community");
			if ((communityArray == null || communityArray.isEmpty())
					|| (communityArray.size() > 1 && communityArray.contains("U") && communityArray.contains(community))
					|| (communityArray.size() <= 1 && communityArray.contains("U"))
					|| (communityArray.size() <= 1 && communityArray.contains(community))) {
				logger.debug("##### " + method + slot + " ITEM -->> " + json);
				preferencesList.add(json);
			}
		}
		
		return preferencesList;
	}
	
	
	private static boolean getDietBySlot(JsonObject x,int slot) {
		if(x==null) {
			return false;
		}
		JsonArray slots = x.getJsonArray("Slots");
		if (slots == null || slots.isEmpty()) {
			return false;
		}
		return slots.contains(slot);

	}
	
	private static boolean isDetox(JsonObject x) {
		boolean isDetox = "Y".equalsIgnoreCase(x.getString("Detox")) ? true : false;
		if (isDetox)
			return true;

		return false;
	}
	
	private static boolean getFoodItemBySpecialDiet(JsonObject x, int slot) {
		String specialDiet = x.getString("Special_diet");
		if ("".equals(specialDiet) || "N".equalsIgnoreCase(specialDiet) || "-".equals(specialDiet))
			return false;

		return String.valueOf(slot).equalsIgnoreCase(x.getString("Special_diet"));
	}
	
	public static boolean getFoodItemByIfDetoxIsYes(JsonObject x) {
		String detox = x.getString("Detox");
		if ("".equals(detox) || "N".equalsIgnoreCase(detox) || "-".equals(detox))
			return false;

		return "Y".equalsIgnoreCase(detox);
	}
	
	private  static JsonObject getMealByCode(String code,List<JsonObject> datas) {
		for (JsonObject jsonObject : datas) {
			if(jsonObject.getString("code").equalsIgnoreCase(code)) {
				return jsonObject;
			}
		}
		return null;
	}
	
	public static boolean getSnacks(JsonObject x) {
		if (x == null) {
			return false;
		}

		if (x.getString("Type").equalsIgnoreCase("W") || x.getString("Type").equalsIgnoreCase("wp")
				|| x.getString("Type").equalsIgnoreCase("wc") || x.getString("Type").equalsIgnoreCase("wpp")
				|| x.getString("Type").equalsIgnoreCase("wcp") || x.getString("Type").equalsIgnoreCase("we")
				|| x.getString("Type").equalsIgnoreCase("wm")) {
			return true;
		} else {
			return false;
		}
	}
	
	@SuppressWarnings("unused")
	public static boolean getDrinks(JsonObject x) {
		if(x==null) {
			return false;
		}
		if (x.getString("Type").equalsIgnoreCase("D") || x.getString("Type").equalsIgnoreCase("DM")) {
			return true;
		} else {
			return false;
		}
	}
	
	@SuppressWarnings("unused")
	public static boolean getFruits(JsonObject x) {
		if(x==null) {
			return false;
		}
		if (x.getString("Type").equalsIgnoreCase("F") || (x.getString("Type").equalsIgnoreCase("FS"))) {
			return true;
		} else {
			return false;
		}
	}
	
	@SuppressWarnings("unused")
	public static boolean getDItems(JsonObject x) {
		if(x==null) {
			return false;
		}
		if (x.getString("Type").equalsIgnoreCase("D")) {
			return true;
		} else {
			return false;
		}
	}
	
	@SuppressWarnings("unused")
	private static boolean checkSpecial(JsonObject x,int slot) {
		if(x==null) {
			return false;
		}
		if (x.getInteger("Special_slot",0)==slot) {
			return true;
		} else {
			return false;
		}
	}
	
	@SuppressWarnings("unused")
	private static boolean checkUltra(JsonObject x,int slot) {
		if(x==null) {
			return false;
		}
		if (x.getInteger("Ultra_special",0)==slot) {
			return true;
		} else {
			return false;
		}
	}
	
	private static boolean getFoodItems(JsonObject x, String slot1foods) {
		if (null != x && x.getString("Type").equalsIgnoreCase(slot1foods))
			return true;

		return false;
	}
	
	private static boolean getMultipleFoodItems(JsonObject x, String food) {
		String[] foodArr = { food };
		if (food.contains("/"))
			foodArr = food.split("/");

		for (String type : Arrays.asList(foodArr)) {
			// if (null != x && Arrays.asList(foodArr).contains(x.getString("Type"))) {
			if (null != x && type.equalsIgnoreCase(x.getString("Type"))) {
				logger.info("##### getMultipleFoodItems() TYPE -->> " + type);
				return true;
			}
		}

		return false;
	}
	
	@SuppressWarnings("unused")
	private static boolean getSM(JsonObject x) {
		if(x==null) {
			return false;
		}
		if (x.getString("Type").equalsIgnoreCase("S") || x.getString("Type").equalsIgnoreCase("SM")) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean filterSpecialItem(JsonObject x,int slot) {
		if(x==null) {
			return false;
		}
		return (x.getInteger("Special_slot",-1)==slot);
	}
	
	public static boolean filterUltraItem(JsonObject x,int slot) {
		if(x==null) {
			return false;
		}
		return (x.getInteger("Ultra_special",-1)==slot);
	}
	
	private  static boolean filterAvoidIn(JsonObject x,List<String> diseases) {
		if(diseases==null) {
			return true;
		}
		//logger.info("**communities**"+diseases.toString());
		JsonArray avoidInArray = x.getJsonArray("AvoidIn");
		if (avoidInArray == null || avoidInArray.isEmpty()) {
			return true;
		}
		
		for (String diseas : diseases) {
			if(avoidInArray.contains(diseas)) {
				return false;
			}
		}
		return true;

	}
	
	@SuppressWarnings("unused")
	private static boolean filterByCommunity(JsonObject x,List<String> communities) {
		
		logger.info("**communities**"+communities.toString());
		JsonArray communityArray = x.getJsonArray("Community");
		if (communityArray == null || communityArray.isEmpty()) {
			return false;
		}
		
		for (String comunity : communities) {
			if(communityArray.contains(comunity)) {
				return true;
			}
		}
		return false;
	}
	
	private static boolean filterByCommunityFormulae(JsonObject x, List<String> communities) {

		logger.info("**communities**" + communities.toString());
		JsonArray communityArray = x.getJsonArray("Community");
		if (communityArray == null || communityArray.isEmpty()) {
			return false;
		}

		if (communities.size() <= 1) {
			if ("U".equalsIgnoreCase(communities.get(0)) || communityArray.contains(communities.get(0))) {
				return true;
			}
		} else {
		for (String comunity : communities) {
			if ("U".equalsIgnoreCase(comunity)) {
				continue;
			} else if (communityArray.contains(comunity)) {
				return true;
			}
		}
	}
		return false;
	}
	
  public static void updateCalories(JsonObject json,int multiplier ) {
	    Double Calories=json.getDouble("Calories") * multiplier;
		json.put("Calories", Double.parseDouble(ApiUtils.getDecimal(Calories)));
		Double Carbs=json.getDouble("Carbs") * multiplier;
		json.put("Carbs", Double.parseDouble(ApiUtils.getDecimal(Carbs)));
		Double Fat=json.getDouble("Fat") * multiplier;
		json.put("Fat", Double.parseDouble(ApiUtils.getDecimal(Fat)));
		Double Protien=json.getDouble("Protien") * multiplier;
		json.put("Protien", Double.parseDouble(ApiUtils.getDecimal(Protien)));
		Double Fiber=json.getDouble("Fiber") * multiplier;
		json.put("Fiber", Double.parseDouble(ApiUtils.getDecimal(Fiber)));
		
	}
  public static void updateCaloriesOriginal(JsonObject json,Double multiplier ) {
		Double Calories=json.getDouble("Calories") * multiplier;
		json.put("Calories", Double.parseDouble(ApiUtils.getDecimal(Calories)));
		logger.info("##### FoodFilterUtils updateCaloriesOriginal() CALORIES ->> " + Calories);
		Double Carbs=json.getDouble("Carbs") * multiplier;
		json.put("Carbs", Double.parseDouble(ApiUtils.getDecimal(Carbs)));
		Double Fat=json.getDouble("Fat") * multiplier;
		json.put("Fat", Double.parseDouble(ApiUtils.getDecimal(Fat)));
		Double Protien=json.getDouble("Protien") * multiplier;
		json.put("Protien", Double.parseDouble(ApiUtils.getDecimal(Protien)));
		Double Fiber=json.getDouble("Fiber") * multiplier;
		json.put("Fiber", Double.parseDouble(ApiUtils.getDecimal(Fiber)));
		json.put("portion", multiplier);
	}
  public static void updateDietCalories(JsonObject json,Double multiplier ) {
		Double Calories=json.getDouble("Calories") * multiplier;
		json.put("Calories", Double.parseDouble(ApiUtils.getDecimal(Calories)));
		logger.debug("##### FoodFilterUtils updateDietCalories() CALORIES ->> " + Calories);
		Double Carbs=json.getDouble("Carbs") * multiplier;
		json.put("Carbs", Double.parseDouble(ApiUtils.getDecimal(Carbs)));
		Double Fat=json.getDouble("Fat") * multiplier;
		json.put("Fat", Double.parseDouble(ApiUtils.getDecimal(Fat)));
		Double Protien=json.getDouble("Protien") * multiplier;
		json.put("Protien", Double.parseDouble(ApiUtils.getDecimal(Protien)));
		Double Fiber=json.getDouble("Fiber") * multiplier;
		json.put("Fiber", Double.parseDouble(ApiUtils.getDecimal(Fiber)));
		json.put("portion", multiplier);
	}
//  public static void updateDietCalories(JsonObject json,Double multiplier ) {
//		Double Calories=json.getDouble("Calories")* multiplier;
//		json.put("Calories", Calories);
//		logger.info("##### updateDietCalories() CALORIES ->> " + Calories);
//		Double Carbs=json.getDouble("Carbs")* multiplier;
//		json.put("Carbs", Carbs);
//		Double Fat=json.getDouble("Fat")* multiplier;
//		json.put("Fat", Fat);
//		Double Protien=json.getDouble("Protien")* multiplier;
//		json.put("Protien", Protien);
//		Double Fiber=json.getDouble("Fiber")* multiplier;
//		json.put("Fiber", Fiber);
//		json.put("portion", multiplier);
//	}
  public static void updateCalories(JsonObject json,Double multiplier ) {
		Double Calories=json.getDouble("Calories") * multiplier;
		json.put("Calories", Double.parseDouble(ApiUtils.getDecimal(Calories)));
		logger.info("##### FoodFilterUtils updateCalories() CALORIES ->> " + Calories);
		Double Carbs=json.getDouble("Carbs") * multiplier;
		json.put("Carbs", Double.parseDouble(ApiUtils.getDecimal(Carbs)));
		Double Fat=json.getDouble("Fat") * multiplier;
		json.put("Fat", Double.parseDouble(ApiUtils.getDecimal(Fat)));
		Double Protien=json.getDouble("Protien") * multiplier;
		json.put("Protien", Double.parseDouble(ApiUtils.getDecimal(Protien)));
		Double Fiber=json.getDouble("Fiber") * multiplier;
		json.put("Fiber", Double.parseDouble(ApiUtils.getDecimal(Fiber)));
		json.put("portion", multiplier);
	}
  
  public static void addPortion(String code,Double portion,List<JsonObject> dietList) {	
	  for (JsonObject diet : dietList) {
		  if(code.equalsIgnoreCase(diet.getString("code"))) {
			  diet.put("portion", portion);
			  //updateCalories(diet, portion);
			  updateCaloriesOriginal(diet, portion);
				logger.info("##### addPortion() UPDATED JSON ->> " + diet);
			  break;
		  }
	}
  }
  
	private static boolean filterByCustFoodType(JsonObject x, String foodType) {

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

	public static List<JsonObject> filterByCustCommunity(List<JsonObject> x, List<String> communities, int slot,
			String type) {

		List<JsonObject> topMostCommunityCustDietList = new ArrayList<>();
		List<JsonObject> secondMostCommunityCustDietList = new ArrayList<>();
		List<JsonObject> thirdMostCommunityCustDietList = new ArrayList<>();
		List<JsonObject> topCommunityCustDietList = new ArrayList<>();
		List<JsonObject> communityCustDietAsUList = new ArrayList<>();
		List<JsonObject> emptyCommunityCustDietList = new ArrayList<>();
		List<JsonObject> sortedFilteredList = new ArrayList<>();
		for (JsonObject json : x) {
			JsonArray communityArray = json.getJsonArray("Community");

			if (communityArray == null || communityArray.isEmpty()) {
				logger.debug("##### FoodFilterUtils SLOT " + slot + " - " + type + " EMPTY SIZE 1");
				emptyCommunityCustDietList.add(json);
				continue;
			}

			if (communityArray.size() <= 1) {
				if (communityArray.contains("U")) {
					communityCustDietAsUList.add(json);
					continue;
				}
			} else {
				Iterator<Object> iter = communityArray.iterator();
				while (iter.hasNext()) {
					String community = (String) iter.next();
					if (communityArray.size() <= 1 && !communityArray.contains("U")) {
						if (communities.contains(community)) {
							topMostCommunityCustDietList.add(json);
							break;
						} else {
							topCommunityCustDietList.add(json);
							break;
						}
					}

					if (communityArray.size() > 1) {
						if (community.equalsIgnoreCase("U")) {
							continue;
						} else if (communities.contains(community)) {
							secondMostCommunityCustDietList.add(json);
							break;
						} else if (!communities.contains(community)) {
							topCommunityCustDietList.add(json);
							break;
						} else {
							if (communities.contains("U") && !communities.contains(community))
								thirdMostCommunityCustDietList.add(json);
							logger.debug("##### FoodFilterUtils SLOT " + slot + type
									+ "NEED TO CHECK WHAT IS ITEM -->> " + json.getString("itemCode"));
							break;
						}
					}
				}
			}
		}

		// SORTING TOPMOST ITEMS
		logger.debug("##### FoodFilterUtils SLOT " + slot + type + " topMostCommunityCustDietList -->> "
				+ topMostCommunityCustDietList.size() + "--" + communities);
		topMostCommunityCustDietList = FoodFilterUtils.sortByDietScore(topMostCommunityCustDietList);
		sortedFilteredList.addAll(topMostCommunityCustDietList);
		// SORTING SECONDMOST ITEMS
		logger.debug("##### FoodFilterUtils SLOT " + slot + type + " secondMostCommunityCustDietList -->> "
				+ secondMostCommunityCustDietList.size() + "--" + communities);
		secondMostCommunityCustDietList = FoodFilterUtils.sortByDietScore(secondMostCommunityCustDietList);
		sortedFilteredList.addAll(secondMostCommunityCustDietList);
		// SORTING TOP ITEMS
		logger.debug("##### FoodFilterUtils SLOT " + slot + type + " topCommunityCustDietList -->> "
				+ topCommunityCustDietList.size() + "--" + communities);
		topCommunityCustDietList = FoodFilterUtils.sortByDietScore(topCommunityCustDietList);
		sortedFilteredList.addAll(topCommunityCustDietList);

		thirdMostCommunityCustDietList = FoodFilterUtils.sortByDietScore(thirdMostCommunityCustDietList);
		sortedFilteredList.addAll(thirdMostCommunityCustDietList);
		// SORTING 'U' AND ONE MORE COMMUNITY ITEM
		logger.debug("##### FoodFilterUtils AS 'U' AND ONE MORE ITEM LIST              -->> "
				+ thirdMostCommunityCustDietList.size() + " -- " + communities);
		// SORTING 'U' ITEMS
		logger.debug("##### FoodFilterUtils SLOT " + slot + type + " communityCustDietAsUList -->> "
				+ communityCustDietAsUList.size() + "--" + communities);
		communityCustDietAsUList = FoodFilterUtils.sortByDietScore(communityCustDietAsUList);
		sortedFilteredList.addAll(communityCustDietAsUList);
		// SORTING EMPTY ITEMS
		logger.debug("##### FoodFilterUtils SLOT " + slot + type + " emptyCommunityCustDietList -->> "
				+ emptyCommunityCustDietList.size() + "--" + communities);
		emptyCommunityCustDietList = FoodFilterUtils.sortByDietScore(emptyCommunityCustDietList);
		sortedFilteredList.addAll(emptyCommunityCustDietList);

		return sortedFilteredList;
	}

	public static List<JsonObject> filterByCustCommunityDetox(List<JsonObject> x, List<String> communities, int slot,
			String type) {
		String method = "FoodFilterUtils filterByCustCommunityDetox()";
		List<JsonObject> topMostCommunityCustDietList = new ArrayList<>();
		List<JsonObject> secondMostCommunityCustDietList = new ArrayList<>();
		List<JsonObject> communityCustDietAsUList = new ArrayList<>();
		List<JsonObject> emptyCommunityCustDietList = new ArrayList<>();
		List<JsonObject> sortedFilteredList = new ArrayList<>();
		for (JsonObject json : x) {
			JsonArray communityArray = json.getJsonArray("Community");

			if (communityArray == null || communityArray.isEmpty()) {
				logger.debug("##### " + method + " [SLOT " + slot + "-DETOX]" + type + " EMPTY SIZE.");
				emptyCommunityCustDietList.add(json);
				continue;
			}

			Iterator<Object> iter = communityArray.iterator();
			if (communityArray.size() <= 1) {
				if (communityArray.contains("U")) {
					communityCustDietAsUList.add(json);
					continue;
				} else {
					while (iter.hasNext()) {
						String community = (String) iter.next();
						if (communities.contains(community))
							topMostCommunityCustDietList.add(json);
					}
				}
			} else {
				while (iter.hasNext()) {
					String community = (String) iter.next();
					if (communityArray.size() > 1) {
						if (community.equalsIgnoreCase("U")) {
							continue;
						} else if (communities.contains(community)) {
							secondMostCommunityCustDietList.add(json);
							break;
						} else {
							logger.info("##### " + method + " [SLOT " + slot + "-DETOX]" + type
									+ " NEED TO CHECK WHAT IS ITEM -->> " + json.getString("itemCode"));
							break;
						}
					}
				}
			}
		}

		// SORTING TOPMOST ITEMS
		logger.info("##### " + method + " [SLOT " + slot + "-DETOX]" + slot + type
				+ " topMostCommunityCustDietList -->> " + topMostCommunityCustDietList.size() + "--" + communities);
		topMostCommunityCustDietList = FoodFilterUtils.sortByCalories(topMostCommunityCustDietList);
		sortedFilteredList.addAll(topMostCommunityCustDietList);
		// SORTING SECONDMOST ITEMS
		logger.info("##### " + method + " [SLOT " + slot + "-DETOX]" + slot + type
				+ " secondMostCommunityCustDietList -->> " + secondMostCommunityCustDietList.size() + "--"
				+ communities);
		secondMostCommunityCustDietList = FoodFilterUtils.sortByCalories(secondMostCommunityCustDietList);
		sortedFilteredList.addAll(secondMostCommunityCustDietList);
		// SORTING 'U' ITEMS
		logger.info("##### " + method + " [SLOT " + slot + "-DETOX]" + slot + type + " communityCustDietAsUList -->> "
				+ communityCustDietAsUList.size() + "--" + communities);
		communityCustDietAsUList = FoodFilterUtils.sortByCalories(communityCustDietAsUList);
		sortedFilteredList.addAll(communityCustDietAsUList);
		// SORTING EMPTY ITEMS
		logger.info("##### " + method + " [SLOT " + slot + "-DETOX]" + slot + type
				+ " emptyCommunityCustDietList -->> " + emptyCommunityCustDietList.size() + "--" + communities);
		emptyCommunityCustDietList = FoodFilterUtils.sortByCalories(emptyCommunityCustDietList);
		sortedFilteredList.addAll(emptyCommunityCustDietList);

		return sortedFilteredList;
	}

	private static boolean filterByDietSeason(JsonObject x) {

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
	
	private static JsonObject getUltraSpecialItem(int slot, List<JsonObject> list, DBResult dbResult,
			List<JsonObject> addedList) {
		String method = "FoodFilterUtils getUltraSpecialItem()";
		List<JsonObject> selectedUltraSpecialItemsList = new ArrayList<>();
		for (JsonObject json : list)
			if (json.containsKey("Ultra_special")) {
				int ultraSpecialItem = json.getInteger("Ultra_special");
				if (ultraSpecialItem > 0 && ultraSpecialItem == slot) {
					selectedUltraSpecialItemsList.add(json);
					logger.debug("##### " + method + " SLOT [" + slot + "] ULTRA SPECIAL ITEM ADDED -->> "
							+ json.getString("itemCode") + " ---- " + json.getString("Food"));
				}
			}

		//JsonObject ultraSpecialItem = FoodFilterUtils.geFilteredData(selectedUltraSpecialItemsList, addedList);
		JsonObject ultraSpecialItem = checkRemainingItems(slot, selectedUltraSpecialItemsList, dbResult, addedList);
		logger.debug("##### " + method + " SLOT [" + slot + "] FINAL ULTRA SPECIAL ITEM -->> " + ultraSpecialItem);
		return ultraSpecialItem;
	}

	private static JsonObject getSpecialSlotItem(int slot, List<JsonObject> list, DBResult dbResult,
			List<JsonObject> addedList) {
		String method = "FoodFilterUtils getSpecialSlotItem()";
		List<JsonObject> selectedSpecialSlotItemsList = new ArrayList<>();
		for (JsonObject json : list)
			if (json.containsKey("Special_slot")) {
				int specialSlotItem = json.getInteger("Special_slot");
				if (specialSlotItem > 0 && specialSlotItem == slot) {
					selectedSpecialSlotItemsList.add(json);
					logger.info("##### " + method + " SLOT [" + slot + "] SPECIAL SLOT ITEM ADDED -->> "
							+ json.getString("itemCode") + " ---- " + json.getString("Food"));
				}
			}

		//JsonObject specialSlotItem = FoodFilterUtils.geFilteredData(selectedSpecialSlotItemsList, addedList);
		JsonObject specialSlotItem = checkRemainingItems(slot, selectedSpecialSlotItemsList, dbResult, addedList);
		logger.info("##### " + method + " SLOT [" + slot + "] FINAL SPECIAL SLOT ITEM -->> " + specialSlotItem);
		return specialSlotItem;
	}

	private static JsonObject getSpecialSlotItemOnly(int slot, List<JsonObject> list, DBResult dbResult) {
		String method = "FoodFilterUtils getSpecialSlotItemOnly()";
		List<JsonObject> selectedSpecialSlotItemsList = new ArrayList<>();
		for (JsonObject json : list)
			if (json.containsKey("Special_slot")) {
				int specialSlotItem = json.getInteger("Special_slot");
				if (specialSlotItem > 0 && specialSlotItem == slot) {
					selectedSpecialSlotItemsList.add(json);
					logger.debug("##### " + method + " SLOT [" + slot + "] SPECIAL SLOT ITEM ADDED -->> "
							+ json.getString("itemCode") + " ---- " + json.getString("Food"));
				}
			}

		JsonObject specialSlotItem = geFilteredData(selectedSpecialSlotItemsList);
		logger.debug("##### " + method + " SLOT [" + slot + "] FINAL SPECIAL SLOT ITEM -->> " + specialSlotItem);
		return specialSlotItem;
	}
	
	private static JsonObject checkRemainingItems(int slot, List<JsonObject> remainingItemsList, DBResult dbResult,
			List<JsonObject> addedList) {
		String method = "FoodFilterUtils checkRemainingItems()";
		remainingItemsList = remainingItemsList.stream()
				.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
				.collect(Collectors.toList());
		remainingItemsList = sortedFilteredList(remainingItemsList, dbResult.getFilterData().getCommunity(), " NORMAL");
		remainingItemsList = remainingItemsList.stream()
				.filter(x2 -> filterAvoidIn(x2, dbResult.getFilterData().getDisease())).collect(Collectors.toList());
		remainingItemsList = sortedFilteredList(remainingItemsList, dbResult.getFilterData().getCommunity(), " NORMAL");

		logger.info("##### " + method + "           NORMAL - FILTERED BY COMMUNITY -->> " + remainingItemsList.size());
		logger.info("##### " + method + "               NORMAL FILTERED BY AVOIDIN -->> " + remainingItemsList.size());

		JsonObject remainingItem = FoodFilterUtils.geFilteredData(remainingItemsList, addedList);
		logger.info("##### " + method + " SLOT [" + slot + "] FINAL REMAINING ITEM -->> " + remainingItem);
		return remainingItem;
	}
	


	private static List<JsonObject> sortedFilteredList(List<JsonObject> x, List<String> communities, String custType) {
		String method = "FoodFilterUtils sortedFilteredList() ";
		List<JsonObject> topMostCommunityCustDietList = new ArrayList<>();
		List<JsonObject> secondMostCommunityCustDietList = new ArrayList<>();
		List<JsonObject> topCommunityCustDietList = new ArrayList<>();
		List<JsonObject> communityCustDietAsUList = new ArrayList<>();
		List<JsonObject> emptyCommunityCustDietList = new ArrayList<>();
		List<JsonObject> sortedFilteredList = new ArrayList<>();
		for (JsonObject json : x) {
			JsonArray communityArray = json.getJsonArray("Community");

			if (communityArray == null || communityArray.isEmpty()) {
				logger.debug("##### " + method + custType + " EMPTY SIZE 1");
				emptyCommunityCustDietList.add(json);
				continue;
			}

			if (communityArray.size() <= 1) {
				if (communityArray.contains("U")) {
					communityCustDietAsUList.add(json);
					continue;
				}
			} else {
				Iterator<Object> iter = communityArray.iterator();
				while (iter.hasNext()) {
					String community = (String) iter.next();
					if (communityArray.size() <= 1 && !communityArray.contains("U")) {
						if (communities.contains(community)) {
							topMostCommunityCustDietList.add(json);
							break;
						} else {
							topCommunityCustDietList.add(json);
							break;
						}
					}

					if (communityArray.size() > 1) {
						if (community.equalsIgnoreCase("U")) {
							continue;
						} else if (communities.contains(community)) {
							secondMostCommunityCustDietList.add(json);
							break;
						} else if (!communities.contains(community)) {
							topCommunityCustDietList.add(json);
							break;
						} else {
							logger.debug ("##### " + method + custType + " NEED TO CHECK WHAT IS ITEM -->> "
									+ json.getString("itemCode"));
							break;
						}
					}
				}
			}
		}

		// SORTING TOPMOST ITEMS
		logger.debug("##### " + method + custType + " topMostCommunityCustDietList -->> "
				+ topMostCommunityCustDietList.size() + "--" + communities);
		topMostCommunityCustDietList = FoodFilterUtils.sortByDietScore(topMostCommunityCustDietList);
		sortedFilteredList.addAll(topMostCommunityCustDietList);
		// SORTING SECONDMOST ITEMS
		logger.debug("##### " + method + custType + " secondMostCommunityCustDietList -->> "
				+ secondMostCommunityCustDietList.size() + "--" + communities);
		secondMostCommunityCustDietList = FoodFilterUtils.sortByDietScore(secondMostCommunityCustDietList);
		sortedFilteredList.addAll(secondMostCommunityCustDietList);
		// SORTING TOP ITEMS
		logger.debug("##### " + method + custType + " topCommunityCustDietList -->> " + topCommunityCustDietList.size()
				+ "--" + communities);
		topCommunityCustDietList = FoodFilterUtils.sortByDietScore(topCommunityCustDietList);
		sortedFilteredList.addAll(topCommunityCustDietList);
		// SORTING 'U' ITEMS
		logger.debug("##### " + method + custType + " communityCustDietAsUList -->> " + communityCustDietAsUList.size()
				+ "--" + communities);
		communityCustDietAsUList = FoodFilterUtils.sortByDietScore(communityCustDietAsUList);
		sortedFilteredList.addAll(communityCustDietAsUList);
		// SORTING EMPTY ITEMS
		logger.debug("##### " + method + custType + " emptyCommunityCustDietList -->> "
				+ emptyCommunityCustDietList.size() + "--" + communities);
		emptyCommunityCustDietList = FoodFilterUtils.sortByDietScore(emptyCommunityCustDietList);
		sortedFilteredList.addAll(emptyCommunityCustDietList);

		return sortedFilteredList;
	}
	
	public static JsonObject calculateCalories(JsonObject data) {
		String method = "FoodFilterUtils calculateCalories()";
		// CALCULATE CALORIES
		JsonArray dietArray = data.getJsonArray("diets");
		Double finalCalories = 0d;
		Double finalCarbs = 0d;
		Double finalFat = 0d;
		Double finalProtien = 0d;
		Double finalFiber = 0d;
		logger.debug("##### " + method + " DIET ARRAY SIZE -->> " + dietArray.size());
		for (int i = 0; i < dietArray.size(); ++i) {
			logger.debug("##### " + method + " I [" + i + "]");
			JsonObject json = dietArray.getJsonObject(i);
			logger.debug("##### " + method + " JSONOBJECT -->> " + json);
			JsonArray dataArray = dietArray.getJsonObject(i).getJsonArray("data");
			logger.debug("##### " + method + " DATA ARARY SIZE -->> " + dataArray.size());
			List<JsonObject> list = new ArrayList<JsonObject>();
			for (int j = 0; j < dataArray.size(); ++j)
				list.add(dataArray.getJsonObject(j));

			logger.debug("##### " + method + " KKKKKK -->> " + dataArray.size());
			Double totalCalories = list.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Calories")).sum();
			Double totalCarbs = list.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Carbs")).sum();
			Double totalFat = list.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fat")).sum();
			Double totalProtien = list.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Protien")).sum();
			Double totalFiber = list.stream().filter(x -> x != null).mapToDouble(m -> m.getDouble("Fiber")).sum();

			finalCalories += totalCalories;
			finalCarbs += totalCarbs;
			finalFat += totalFat;
			finalProtien += totalProtien;
			finalFiber += totalFiber;

			dietArray.getJsonObject(i).put("totalCalories", ApiUtils.getDecimal(totalCalories));
			dietArray.getJsonObject(i).put("totalCarbs", ApiUtils.getDecimal(totalCarbs));
			dietArray.getJsonObject(i).put("totalFat", ApiUtils.getDecimal(totalFat));
			dietArray.getJsonObject(i).put("totalProtien", ApiUtils.getDecimal(totalProtien));
			dietArray.getJsonObject(i).put("totalFiber", ApiUtils.getDecimal(totalFiber));

			logger.debug("##### " + method + " SLOT [" + json.getInteger("slot") + "] TOTAL CALORIES -->> ["
					+ ApiUtils.getDecimal(totalCalories) + "]");
		}

		logger.debug("##### " + method + "          FINAL CALORIES -->> [" + finalCalories + "]");
		if (finalCalories > 1800) {
			logger.info("##### " + method + " FINAL CALORIES IS MADE 1800 ONLY. EARLIER FINAL CALORIES ["
					+ finalCalories + "]");
			//finalCalories = 1800d;
		}

		data.put("totalCal", finalCalories);
		data.put("tolalCalories", Double.parseDouble(ApiUtils.getDecimal(finalCalories)));
		data.put("totalCalories", Double.parseDouble(ApiUtils.getDecimal(finalCalories)));
		Double totalCaloriesPer = ((finalCalories * 100) / data.getDouble("recomended") > 100.0 ? 100
				: ((finalCalories * 100) / data.getDouble("recomended")));
		data.put("totalCaloriesPer", totalCaloriesPer.intValue());
		data.put("totalCarbs", finalCarbs.intValue());
		Double totalCarbsPer = ((finalCarbs * 4) * 100) / finalCalories;
		data.put("totalCarbsPer", totalCarbsPer.intValue());
		data.put("totalFat", finalFat.intValue());
		Double totalFatPer = ((finalFat * 9) * 100) / finalCalories;
		data.put("totalFatPer", totalFatPer.intValue());
		data.put("totalProtien", finalProtien.intValue());
		Double totalProtienPer = ((finalProtien * 4) * 100) / finalCalories;
		data.put("totalProtienPer", totalProtienPer.intValue());
		data.put("totalFiber", finalFiber.intValue());
		Double totalFiberPer = finalCalories / finalFiber;
		data.put("totalFiberPer", totalFiberPer.intValue());

		return data;
	}
	
	
	
															// DETOX
	/**
	 * Get slot 0 - detox.
	 * 
	 * @param datas
	 * @param dbResult
	 * @param disease
	 * @param communities
	 * @param addedList
	 * @param traceId
	 * @return List<JsonObject>
	 */
	public static List<JsonObject> getSlot0Detox(List<JsonObject> datas, DBResult dbResult, List<String> disease,
			List<String> communities, Integer slot, String traceId) {
		String method = "FoodFilterUtils getSlot0Detox() " + traceId + "-[" + dbResult.getFilterData().getEmail() + "]";
		logger.info("############################# [SLOT " + slot + "-DETOX] STARTED #############################");
		logger.info("");
		List<JsonObject> result = new ArrayList<JsonObject>();
		datas = datas.stream().filter(x -> getDietBySlot(x, 0)).filter(x -> getFoodItemByIfDetoxIsYes(x))
				.filter(x -> filterByDietSeason(x)).collect(Collectors.toList());
		datas = datas.stream().filter(x -> filterAvoidIn(x, disease))
				.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType())).filter(x -> isDetox(x))
				.collect(Collectors.toList());
		datas = filterByCustCommunity(datas, dbResult.getFilterData().getCommunity(), 0, "");
		JsonObject drinkObj = null;

		if (null != disease) { // DISEASE(S) AVAILABLE
			List<JsonObject> diseaseDiets = FoodFilterUtils.filterByDiseaseRecommendedIn(datas, disease);
			// DRINKS
			List<JsonObject> drink = diseaseDiets.stream().filter(x -> x.getString("Type").equalsIgnoreCase("D"))
					.filter(x -> filterByDietSeason(x)).collect(Collectors.toList());
			if (null != drink && drink.size() > 0) {
				Collections.shuffle(drink);
				drinkObj = drink.get(0);
				if (null != drinkObj)
					result.add(drinkObj);
			}
			// DISHES
			List<JsonObject> dishes = diseaseDiets.stream().filter(x -> x.getString("Type").equalsIgnoreCase("A"))
					.filter(x -> filterByDietSeason(x)).filter(x -> filterByDietSeason(x)).collect(Collectors.toList());

			if (null != dishes && dishes.size() > 0) {
				Collections.shuffle(dishes);
				JsonObject jsonObject = dishes.get(0);
				if (null != jsonObject)
					result.add(jsonObject);
			}
		} else { // NO DISEASE(S) AVAILABLE
			// DRINKS
			List<JsonObject> drinks = datas.stream().filter(x -> x.getString("Type").equalsIgnoreCase("D"))
					.filter(x -> filterByDietSeason(x)).collect(Collectors.toList());
			if (null != drinks && drinks.size() > 0) {
				Collections.shuffle(drinks);
				JsonObject jsonObject = drinks.get(0);
				result.add(jsonObject);
			}
		}

		result = result.stream().map(x -> {
			if (null != x.getDouble("portion"))
				updateCalories(x, x.getDouble("portion"));
			return x;
		}).collect(Collectors.toList());

		result.forEach(
				res -> logger.info("##### " + method + " [SLOT " + slot + "-DETOX] FINAL RESULT DATA -->> " + res));
		logger.info("############################### [SLOT " + slot + "-DETOX] END ###############################");
		logger.info("");
		return result;
	}

	/**
	 * Get slot for detox.
	 * 
	 * @param datas
	 * @param dbResult
	 * @param customerPreferences
	 * @param slot
	 * @param traceId
	 * 
	 * @return List<JsonObject>
	 */
	public static List<JsonObject> getSlotDetox(List<JsonObject> datas, DBResult dbResult,
			List<JsonObject> customerPreferences, Integer slot, String traceId) {
		String method = "FoodFilterUtils getSlotDetox() " + traceId + "-[" + dbResult.getFilterData().getEmail() + "]";
		logger.info("############################# [SLOT " + slot + "-DETOX] STARTED #############################");
		List<JsonObject> result = new ArrayList<>();
		List<String> itemsChosen = new ArrayList<>();
		List<String> codes = new ArrayList<>();
		if (slot == 2) {
			JsonObject json = getMealByCode("036", datas); // SPROUT SALAD WITH PANEER
			updateCalories(json, 0.5);
			result.add(json);
			itemsChosen.add("036");
			codes.add("036");
		} else if (slot == 4) {  // SLOT 7 REMOVED
			result.add(getMealByCode("034", datas)); // GREEN SALAD
			itemsChosen.add("034");
			codes.add("034");
		} else if (slot == 8) {
//			JsonObject json = getMealByCode("173", datas);
//			if (null != json) {
//				itemsChosen.add("173"); // TURMERIC MILK
//				updateCalories(json, 0.5);
//				result.add(json);
//				codes.add("173");
//			}
			
			// DRINKS
			List<JsonObject> drinks = datas.stream().filter(x -> getDietBySlot(x, slot))
					.filter(x -> getFoodItemByIfDetoxIsYes(x))
					.filter(x -> filterAvoidIn(x, dbResult.getFilterData().getDisease()))
					.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
					.filter(x -> filterByDietSeason(x)).filter(x -> x.getString("Type").equalsIgnoreCase("DM"))
					.collect(Collectors.toList());
			logger.info("##### " + method + " [SLOT " + slot + "-DETOX] DRINKS -->> "
					+ drinks);
			if ((null == drinks || (null != drinks && drinks.size() <= 0)) && null != customerPreferences) {
				drinks = customerPreferences.stream().filter(x -> getDietBySlot(x, slot))
						.filter(x -> getFoodItemByIfDetoxIsYes(x))
						.filter(x -> filterAvoidIn(x, dbResult.getFilterData().getDisease()))
						.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
						.filter(x -> filterByDietSeason(x)).filter(x -> x.getString("Type").equalsIgnoreCase("DM"))
						.collect(Collectors.toList());
			}
			
			Collections.shuffle(drinks);
			JsonObject json = drinks.get(0);
			updateCalories(json, json.getDouble("portion"));
			result.add(json);
		}

		if (slot != 8) {
			if (null != result && result.size() > 0)
				logger.info("##### " + method + " [SLOT " + slot + "-DETOX] FOOD ITEM ["
						+ result.get(0).getString("_id") + "] ADDED.");

			List<JsonObject> items = new ArrayList<>();
			if (ApiUtils.getTotalCalories(result) < 100) {
				items = datas.stream().filter(x -> getDietBySlot(x, slot)).filter(x -> getFoodItemByIfDetoxIsYes(x))
						.filter(x -> filterAvoidIn(x, dbResult.getFilterData().getDisease()))
						.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
						.filter(x -> filterByDietSeason(x)).collect(Collectors.toList());

				if (slot != 8)
					items = datas.stream().filter(x -> isDetox(x)).filter(x -> getFoodItemBySpecialDiet(x, slot))
							.collect(Collectors.toList());
				List<JsonObject> foods = filterByCustCommunity(items, dbResult.getFilterData().getCommunity(), slot,
						" SPECIAL DIET ");

				if (null == customerPreferences) {
					customerPreferences = new ArrayList<JsonObject>();
				} else {
					customerPreferences = customerPreferences.stream().filter(x -> getDietBySlot(x, slot))
							.filter(x -> getFoodItemByIfDetoxIsYes(x))
							.filter(x -> filterAvoidIn(x, dbResult.getFilterData().getDisease()))
							.filter(x -> filterByCustFoodType(x, dbResult.getFilterData().getFoodType()))
							.filter(x -> filterByDietSeason(x)).collect(Collectors.toList());

					if (slot != 8)
						customerPreferences = customerPreferences.stream().filter(x -> isDetox(x))
								.filter(x -> getFoodItemBySpecialDiet(x, slot)).collect(Collectors.toList());

					if (null != codes && codes.size() > 0)
						customerPreferences = customerPreferences.stream()
								.filter(x -> !codes.get(0).equalsIgnoreCase(x.getString("_id")))
								.collect(Collectors.toList());

					customerPreferences = filterByCustCommunity(customerPreferences,
							dbResult.getFilterData().getCommunity(), slot, " CUSTOMER PREFERENCES");
				}

				if (null != foods && foods.size() > 0 && null != codes && codes.size() > 0)
					foods = foods.stream().filter(x -> !codes.get(0).equalsIgnoreCase(x.getString("_id")))
							.collect(Collectors.toList());

				List<JsonObject> itemsPref = new ArrayList<>();
				if (null != foods && foods.size() > 0 && null != customerPreferences
						&& customerPreferences.size() > 0) {
					List<String> preferences = customerPreferences.stream().map(mapper -> {
						return mapper.getString("_id");
					}).collect(Collectors.toList());

					itemsPref = foods.stream().filter(x -> preferences.contains(x.getString("_id")))
							.collect(Collectors.toList());
					if (null != itemsPref && itemsPref.size() > 2) {
						Collections.shuffle(itemsPref);
						result.add(itemsPref.get(0));
					} else {
						Collections.shuffle(foods);
						result.add(foods.get(0));
					}
				} else if (null != foods && foods.size() > 0 && (null == customerPreferences
						|| (null != customerPreferences && customerPreferences.size() <= 0))) {
					Collections.shuffle(foods);
					result.add(foods.get(0));
				} else if (null != customerPreferences && customerPreferences.size() > 0
						&& (null == foods || (null != foods && foods.size() <= 0))) {
					Collections.shuffle(customerPreferences);
					result.add(customerPreferences.get(0));
				}
			}
		}

		if (null != result && result.size() > 0)
			result = result.stream().map(x -> {
				if (null != x.getDouble("portion") && !"036".equalsIgnoreCase(x.getString("_id")) && slot != 8)
					updateCalories(x, x.getDouble("portion"));
				return x;
			}).collect(Collectors.toList());

		result.forEach(res -> logger.info("##### " + method + " [SLOT " + slot + "-DETOX] FINAL RESULT -->> " + res));
		logger.info("############################### [SLOT " + slot + "-DETOX] END ###############################");
		logger.info("");
		return result;
	}

	public static JsonObject geFilteredDataForSlot8TimingsAsNull(List<JsonObject> filterList) {
		if (filterList != null && !filterList.isEmpty())
			Collections.shuffle(filterList);

		return filterList.size() > 0 ? filterList.get(0) : null;
	}

	/**
	 * Sorting slot4 specific items.
	 * 
	 * @param slots4
	 * @return List<JsonObject>
	 */
	@SuppressWarnings("unused")
	private static List<JsonObject> sortingSlot4SpecificItems(List<JsonObject> slots4) {
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
}
