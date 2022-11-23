package com.fightitaway.service;

import java.util.List;

import io.vertx.core.json.JsonObject;

public class HabitData {
	
	private List<JsonObject> masterList;
	
	private List<JsonObject> customerHabitList;
	
	private List<String> followedList;
	
	private List<JsonObject> customerFollowedList;
	
	
	
	
	public List<String> getFollowedList() {
		return followedList;
	}

	public void setFollowedList(List<String> followedList) {
		this.followedList = followedList;
	}

	private List<String> habitCode;

	public List<JsonObject> getMasterList() {
		return masterList;
	}

	public void setMasterList(List<JsonObject> masterList) {
		this.masterList = masterList;
	}

	public List<JsonObject> getCustomerHabitList() {
		return customerHabitList;
	}

	public void setCustomerHabitList(List<JsonObject> customerHabitList) {
		this.customerHabitList = customerHabitList;
	}

	public List<String> getHabitCode() {
		return habitCode;
	}

	public void setHabitCode(List<String> habitCode) {
		this.habitCode = habitCode;
	}

	/**
	 * @return the customerFollowedList
	 */
	public List<JsonObject> getCustomerFollowedList() {
		return customerFollowedList;
	}

	/**
	 * @param customerFollowedList the customerFollowedList to set
	 */
	public void setCustomerFollowedList(List<JsonObject> customerFollowedList) {
		this.customerFollowedList = customerFollowedList;
	}
}
