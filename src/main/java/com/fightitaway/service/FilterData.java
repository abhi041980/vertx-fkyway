package com.fightitaway.service;

import java.util.List;
import java.util.Set;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class FilterData {
	private String foodType;
	private Double calories;
	private String email;
	private String specialDiet;;

	private String wakeup;
	private String leaveForOffice;
	private String comeBack;
	private String sleep;
	private String sleepType;
	private String slot0;
	private String slot1;
	private String slot2;
	private String slot3;
	private String slot4;
	private String slot5;
	private String slot6;
	private String slot7;
	private String slot8;
	private String slot0Message;
	private String slot1Message;
	private String slot2Message;
	private String slot3Message;
	private String slot4Message;
	private String slot5Message;
	private String slot6Message;
	private String slot7Message;
	private String slot8Message;

	private List<String> community;
	private List<String> foods;
	private List<String> disease;
	private List<String> drinks;
	private List<String> snacks;
	private List<String> fruits;
	private List<String> dishes;
	private List<String> pules;
	private List<String> rice;
	private List<String> slots;
	private Set<String> allPrefood;
	private SelectiveItems selectiveItems;
	private JsonObject timings;
	private JsonArray previousDiets;

	public Double getCalories() {
		return calories;
	}

	public void setCalories(Double calories) {
		this.calories = calories;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Set<String> getAllPrefood() {
		return allPrefood;
	}

	public void setAllPrefood(Set<String> allPrefood) {
		this.allPrefood = allPrefood;
	}

	public List<String> getFoods() {
		return foods;
	}

	public void setFoods(List<String> foods) {
		this.foods = foods;
	}

	public List<String> getDrinks() {
		return drinks;
	}

	public void setDrinks(List<String> drinks) {
		this.drinks = drinks;
	}

	public List<String> getSnacks() {
		return snacks;
	}

	public void setSnacks(List<String> snacks) {
		this.snacks = snacks;
	}

	public List<String> getFruits() {
		return fruits;
	}

	public void setFruits(List<String> fruits) {
		this.fruits = fruits;
	}

	public List<String> getDishes() {
		return dishes;
	}

	public void setDishes(List<String> dishes) {
		this.dishes = dishes;
	}

	public List<String> getPules() {
		return pules;
	}

	public void setPules(List<String> pules) {
		this.pules = pules;
	}

	public List<String> getRice() {
		return rice;
	}

	public void setRice(List<String> rice) {
		this.rice = rice;
	}

	public String getFoodType() {
		return foodType;
	}

	public void setFoodType(String foodType) {
		this.foodType = foodType;
	}

	public List<String> getCommunity() {
		return community;
	}

	public void setCommunity(List<String> community) {
		this.community = community;
	}

	public List<String> getDisease() {
		return disease;
	}

	public void setDisease(List<String> disease) {
		this.disease = disease;
	}

	/**
	 * @return the slots
	 */
	public List<String> getSlots() {
		return slots;
	}

	/**
	 * @param slots the slots to set
	 */
	public void setSlots(List<String> slots) {
		this.slots = slots;
	}

	/**
	 * @return the wakeup
	 */
	public String getWakeup() {
		return wakeup;
	}

	/**
	 * @param wakeup the wakeup to set
	 */
	public void setWakeup(String wakeup) {
		this.wakeup = wakeup;
	}

	/**
	 * @return the leaveForOffice
	 */
	public String getLeaveForOffice() {
		return leaveForOffice;
	}

	/**
	 * @param leaveForOffice the leaveForOffice to set
	 */
	public void setLeaveForOffice(String leaveForOffice) {
		this.leaveForOffice = leaveForOffice;
	}

	/**
	 * @return the comeBack
	 */
	public String getComeBack() {
		return comeBack;
	}

	/**
	 * @param comeBack the comeBack to set
	 */
	public void setComeBack(String comeBack) {
		this.comeBack = comeBack;
	}

	/**
	 * @return the sleep
	 */
	public String getSleep() {
		return sleep;
	}

	/**
	 * @param sleep the sleep to set
	 */
	public void setSleep(String sleep) {
		this.sleep = sleep;
	}

	/**
	 * @return the sleepType
	 */
	public String getSleepType() {
		return sleepType;
	}

	/**
	 * @param sleepType the sleepType to set
	 */
	public void setSleepType(String sleepType) {
		this.sleepType = sleepType;
	}

	/**
	 * @return the slot0
	 */
	public String getSlot0() {
		return slot0;
	}

	/**
	 * @param slot0 the slot0 to set
	 */
	public void setSlot0(String slot0) {
		this.slot0 = slot0;
	}

	/**
	 * @return the slot1
	 */
	public String getSlot1() {
		return slot1;
	}

	/**
	 * @param slot1 the slot1 to set
	 */
	public void setSlot1(String slot1) {
		this.slot1 = slot1;
	}

	/**
	 * @return the slot2
	 */
	public String getSlot2() {
		return slot2;
	}

	/**
	 * @param slot2 the slot2 to set
	 */
	public void setSlot2(String slot2) {
		this.slot2 = slot2;
	}

	/**
	 * @return the slot3
	 */
	public String getSlot3() {
		return slot3;
	}

	/**
	 * @param slot3 the slot3 to set
	 */
	public void setSlot3(String slot3) {
		this.slot3 = slot3;
	}

	/**
	 * @return the slot4
	 */
	public String getSlot4() {
		return slot4;
	}

	/**
	 * @param slot4 the slot4 to set
	 */
	public void setSlot4(String slot4) {
		this.slot4 = slot4;
	}

	/**
	 * @return the slot5
	 */
	public String getSlot5() {
		return slot5;
	}

	/**
	 * @param slot5 the slot5 to set
	 */
	public void setSlot5(String slot5) {
		this.slot5 = slot5;
	}

	/**
	 * @return the slot6
	 */
	public String getSlot6() {
		return slot6;
	}

	/**
	 * @param slot6 the slot6 to set
	 */
	public void setSlot6(String slot6) {
		this.slot6 = slot6;
	}

	/**
	 * @return the slot7
	 */
	public String getSlot7() {
		return slot7;
	}

	/**
	 * @param slot7 the slot7 to set
	 */
	public void setSlot7(String slot7) {
		this.slot7 = slot7;
	}

	/**
	 * @return the slot8
	 */
	public String getSlot8() {
		return slot8;
	}

	/**
	 * @param slot8 the slot8 to set
	 */
	public void setSlot8(String slot8) {
		this.slot8 = slot8;
	}

	/**
	 * @return the slot0Message
	 */
	public String getSlot0Message() {
		return slot0Message;
	}

	/**
	 * @param slot0Message the slot0Message to set
	 */
	public void setSlot0Message(String slot0Message) {
		this.slot0Message = slot0Message;
	}

	/**
	 * @return the slot1Message
	 */
	public String getSlot1Message() {
		return slot1Message;
	}

	/**
	 * @param slot1Message the slot1Message to set
	 */
	public void setSlot1Message(String slot1Message) {
		this.slot1Message = slot1Message;
	}

	/**
	 * @return the slot2Message
	 */
	public String getSlot2Message() {
		return slot2Message;
	}

	/**
	 * @param slot2Message the slot2Message to set
	 */
	public void setSlot2Message(String slot2Message) {
		this.slot2Message = slot2Message;
	}

	/**
	 * @return the slot3Message
	 */
	public String getSlot3Message() {
		return slot3Message;
	}

	/**
	 * @param slot3Message the slot3Message to set
	 */
	public void setSlot3Message(String slot3Message) {
		this.slot3Message = slot3Message;
	}

	/**
	 * @return the slot4Message
	 */
	public String getSlot4Message() {
		return slot4Message;
	}

	/**
	 * @param slot4Message the slot4Message to set
	 */
	public void setSlot4Message(String slot4Message) {
		this.slot4Message = slot4Message;
	}

	/**
	 * @return the slot5Message
	 */
	public String getSlot5Message() {
		return slot5Message;
	}

	/**
	 * @param slot5Message the slot5Message to set
	 */
	public void setSlot5Message(String slot5Message) {
		this.slot5Message = slot5Message;
	}

	/**
	 * @return the slot6Message
	 */
	public String getSlot6Message() {
		return slot6Message;
	}

	/**
	 * @param slot6Message the slot6Message to set
	 */
	public void setSlot6Message(String slot6Message) {
		this.slot6Message = slot6Message;
	}

	/**
	 * @return the slot7Message
	 */
	public String getSlot7Message() {
		return slot7Message;
	}

	/**
	 * @param slot7Message the slot7Message to set
	 */
	public void setSlot7Message(String slot7Message) {
		this.slot7Message = slot7Message;
	}

	/**
	 * @return the slot8Message
	 */
	public String getSlot8Message() {
		return slot8Message;
	}

	/**
	 * @param slot8Message the slot8Message to set
	 */
	public void setSlot8Message(String slot8Message) {
		this.slot8Message = slot8Message;
	}

	/**
	 * @return the selectiveItems
	 */
	public SelectiveItems getSelectiveItems() {
		return selectiveItems;
	}

	/**
	 * @param selectiveItems the selectiveItems to set
	 */
	public void setSelectiveItems(SelectiveItems selectiveItems) {
		this.selectiveItems = selectiveItems;
	}

	/**
	 * @return the timings
	 */
	public JsonObject getTimings() {
		return timings;
	}

	/**
	 * @param timings the timings to set
	 */
	public void setTimings(JsonObject timings) {
		this.timings = timings;
	}

	/**
	 * @return the specialDiet
	 */
	public String getSpecialDiet() {
		return specialDiet;
	}

	/**
	 * @param specialDiet the specialDiet to set
	 */
	public void setSpecialDiet(String specialDiet) {
		this.specialDiet = specialDiet;
	}

	/**
	 * @return the previousDiets
	 */
	public JsonArray getPreviousDiets() {
		return previousDiets;
	}

	/**
	 * @param previousDiets the previousDiets to set
	 */
	public void setPreviousDiets(JsonArray previousDiets) {
		this.previousDiets = previousDiets;
	}

	@Override
	public String toString() {
		return "FilterData [foodType=" + foodType + ", calories=" + calories + ", email=" + email + ", specialDiet="
				+ specialDiet + ", wakeup=" + wakeup + ", leaveForOffice=" + leaveForOffice + ", comeBack=" + comeBack
				+ ", sleep=" + sleep + ", sleepType=" + sleepType + ", slot0=" + slot0 + ", slot1=" + slot1 + ", slot2="
				+ slot2 + ", slot3=" + slot3 + ", slot4=" + slot4 + ", slot5=" + slot5 + ", slot6=" + slot6 + ", slot7="
				+ slot7 + ", slot8=" + slot8 + ", slot0Message=" + slot0Message + ", slot1Message=" + slot1Message
				+ ", slot2Message=" + slot2Message + ", slot3Message=" + slot3Message + ", slot4Message=" + slot4Message
				+ ", slot5Message=" + slot5Message + ", slot6Message=" + slot6Message + ", slot7Message=" + slot7Message
				+ ", slot8Message=" + slot8Message + ", community=" + community + ", foods=" + foods + ", disease="
				+ disease + ", drinks=" + drinks + ", snacks=" + snacks + ", fruits=" + fruits + ", dishes=" + dishes
				+ ", pules=" + pules + ", rice=" + rice + ", slots=" + slots + ", allPrefood=" + allPrefood
				+ ", selectiveItems=" + selectiveItems + ", timings=" + timings + "]";
	}
}
