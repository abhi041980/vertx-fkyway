package com.fightitaway.service;

import java.util.List;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class CreatCustomerDietPlanRequest {
	
	private List<DietDetail> meals;

	private List<DietDetail> drinks;
	
	

	public List<DietDetail> getMeals() {
		return meals;
	}

	public void setMeals(List<DietDetail> meals) {
		this.meals = meals;
	}

	public List<DietDetail> getDrinks() {
		return drinks;
	}

	public void setDrink(List<DietDetail> drinks) {
		this.drinks = drinks;
	}

	public CreatCustomerDietPlanRequest() {
		// TODO Auto-generated constructor stub
	}

	public CreatCustomerDietPlanRequest(JsonObject json) {
		CreatCustomerDietPlanRequestConverter.fromJson(json, this);
	  }

	  public JsonObject toJson() {
	    JsonObject json = new JsonObject();
	    CreatCustomerDietPlanRequestConverter.toJson(this, json);
	    return json;
	  }
	
	
}
