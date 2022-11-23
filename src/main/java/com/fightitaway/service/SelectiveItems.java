package com.fightitaway.service;

public class SelectiveItems {

	private String community;
	private String foodType;
	private DietItems dietItems;

	/**
	 * @return the community
	 */
	public String getCommunity() {
		return community;
	}

	/**
	 * @param community the community to set
	 */
	public void setCommunity(String community) {
		this.community = community;
	}

	/**
	 * @return the foodType
	 */
	public String getFoodType() {
		return foodType;
	}

	/**
	 * @param foodType the foodType to set
	 */
	public void setFoodType(String foodType) {
		this.foodType = foodType;
	}

	/**
	 * @return the data
	 */
	public DietItems getDietItems() {
		return dietItems;
	}

	/**
	 * @param data the data to set
	 */
	public void setDietItems(DietItems dietItems) {
		this.dietItems = dietItems;
	}

	@Override
	public String toString() {
		return "SelectiveItems [community=" + community + ", foodType=" + foodType + ", dietItems=" + dietItems + "]";
	}
}
