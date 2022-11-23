package com.fightitaway.service;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class FoodDetail {

	private String code;

	//private Integer portion;

	private Double portion;

	private Integer counter;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Double getPortion() {
		return portion;
	}

	public void setPortion(Double portion) {
		this.portion = portion;
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return code.hashCode();
	}

	public FoodDetail() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean equals(Object obj) {
		FoodDetail detail = (FoodDetail) obj;
		return detail.getCode().equals(this.code);
	}

	public FoodDetail(JsonObject json) {
		FoodDetailConverter.fromJson(json, this);
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		FoodDetailConverter.toJson(this, json);
		return json;
	}

	/**
	 * @return the counter
	 */
	public Integer getCounter() {
		return counter;
	}

	/**
	 * @param counter the counter to set
	 */
	public void setCounter(Integer counter) {
		this.counter = counter;
	}

	@Override
	public String toString() {
		return "FoodDetail [code=" + code + ", portion=" + portion + "]";
	}
}
