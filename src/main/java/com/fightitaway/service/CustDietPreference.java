package com.fightitaway.service;

import java.util.Set;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class CustDietPreference {

	private String email;

	private Integer slot;

	private String code;

	private String category;

	private Set<FoodDetail> foods;

	public Set<FoodDetail> getFoods() {
		return foods;
	}

	public void setFoods(Set<FoodDetail> foods) {
		this.foods = foods;
	}

	private JsonArray foodCodeList;

	public Integer getSlot() {
		return slot;
	}

	public void setSlot(Integer slot) {
		this.slot = slot;
	}

	public JsonArray getFoodCodeList() {
		return foodCodeList;
	}

	public void setFoodCodeList(JsonArray foodCodeList) {
		this.foodCodeList = foodCodeList;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public CustDietPreference() {
	}

	public CustDietPreference(JsonObject json) {
		CustDietPreferenceConverter.fromJson(json, this);
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		CustDietPreferenceConverter.toJson(this, json);
		return json;
	}

	@Override
	public String toString() {
		return "CustDietPreference [email=" + email + ", slot=" + slot + ", foods=" + foods + ", foodCodeList=" + foodCodeList + "]";
	}
}
