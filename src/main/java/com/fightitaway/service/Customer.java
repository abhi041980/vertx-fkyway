package com.fightitaway.service;

import java.util.List;

public class Customer {
	

	private String email;
	
	private String gender;
	
	private Integer height;
	

	private String dateBy;
	
	
	private Double suggestedWeight;
	

	private Integer weight;
	
	
	private String subscribeDate;
	

	private String foodType;
	

	private List<String> communities;
	
	private List<String> diseases;
	
	public String getSubscribeDate() {
		return subscribeDate;
	}

	public void setSubscribeDate(String subscribeDate) {
		this.subscribeDate = subscribeDate;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public Integer getHeight() {
		return height;
	}

	public void setHeight(Integer height) {
		this.height = height;
	}

	public String getDateBy() {
		return dateBy;
	}

	public void setDateBy(String dateBy) {
		this.dateBy = dateBy;
	}

	public Double getSuggestedWeight() {
		return suggestedWeight;
	}

	public void setSuggestedWeight(Double suggestedWeight) {
		this.suggestedWeight = suggestedWeight;
	}

	public Integer getWeight() {
		return weight;
	}

	public void setWeight(Integer weight) {
		this.weight = weight;
	}

	public Customer() {
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
	 * @return the communities
	 */
	public List<String> getCommunities() {
		return communities;
	}

	/**
	 * @param communities the communities to set
	 */
	public void setCommunities(List<String> communities) {
		this.communities = communities;
	}

	/**
	 * @return the diseases
	 */
	public List<String> getDiseases() {
		return diseases;
	}

	/**
	 * @param diseases the diseases to set
	 */
	public void setDiseases(List<String> diseases) {
		this.diseases = diseases;
	}	
	
	@Override
	public String toString() {
		return super.toString();
	}
}
