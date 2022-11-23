package com.fightitaway.service;

import java.util.List;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class CreatCustomerHabitPlanRequest {
	
	private List<HabitDetailDetail> habits;

	
	
	public List<HabitDetailDetail> getHabits() {
		return habits;
	}

	public void setHabits(List<HabitDetailDetail> habits) {
		this.habits = habits;
	}

	public CreatCustomerHabitPlanRequest() {
		// TODO Auto-generated constructor stub
	}

	public CreatCustomerHabitPlanRequest(JsonObject json) {
		System.out.println("##### CUSTOMER HABIT UPDATE -->> " + json);
		CreatCustomerHabitPlanRequestConverter.fromJson(json, this);
	  }

	  public JsonObject toJson() {
	    JsonObject json = new JsonObject();
	    CreatCustomerHabitPlanRequestConverter.toJson(this, json);
		System.out.println("##### CONVERT CUSTOMER HABIT UPDATE -->> " + json);
	    return json;
	  }

	@Override
	public String toString() {
		return "##### CreatCustomerHabitPlanRequest [habits=" + habits + "]";
	}
}
