package com.fightitaway.service;

import java.util.Date;
import java.util.HashMap;

import com.fightitaway.common.CustomerLifeStyle;

public class CalculationData {
	private String gender;
	private Double height;
	private Double weight;
	private Double suggestedWeight;
	private Integer age;
	private String activityCode;
	private Float activityUnit;
	private Integer activityCalories;
	private HashMap<Float, Integer> activities;
	private Date planTakenDate;
	private CustomerLifeStyle customerLifeStyle;

	public Date getPlanTakenDate() {
		return planTakenDate;
	}

	public void setPlanTakenDate(Date planTakenDate) {
		this.planTakenDate = planTakenDate;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public Double getHeight() {
		return height;
	}

	public void setHeight(Double height) {
		this.height = height;
	}

	public Double getWeight() {
		return weight;
	}

	public void setWeight(Double weight) {
		this.weight = weight;
	}

	public Double getSuggestedWeight() {
		return suggestedWeight;
	}

	public void setSuggestedWeight(Double suggestedWeight) {
		this.suggestedWeight = suggestedWeight;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	/**
	 * @return the activityCode
	 */
	public String getActivityCode() {
		return activityCode;
	}

	/**
	 * @param activityCode the activityCode to set
	 */
	public void setActivityCode(String activityCode) {
		this.activityCode = activityCode;
	}

	public Float getActivityUnit() {
		return activityUnit;
	}

	public void setActivityUnit(Float activityUnit) {
		this.activityUnit = activityUnit;
	}

	/**
	 * @return the activityCalories
	 */
	public Integer getActivityCalories() {
		return activityCalories;
	}

	/**
	 * @param activityCalories the activityCalories to set
	 */
	public void setActivityCalories(Integer activityCalories) {
		this.activityCalories = activityCalories;
	}

	/**
	 * @return the activities
	 */
	public HashMap<Float, Integer> getActivities() {
		return activities;
	}

	/**
	 * @param activities the activities to set
	 */
	public void setActivities(HashMap<Float, Integer> activities) {
		this.activities = activities;
	}

	/**
	 * @return the customerLifeStyle
	 */
	public CustomerLifeStyle getCustomerLifeStyle() {
		return customerLifeStyle;
	}

	/**
	 * @param customerLifeStyle the customerLifeStyle to set
	 */
	public void setCustomerLifeStyle(CustomerLifeStyle customerLifeStyle) {
		this.customerLifeStyle = customerLifeStyle;
	}

	@Override
	public String toString() {
		return "CalculationData [gender=" + gender + ", height=" + height + ", weight=" + weight + ", suggestedWeight="
				+ suggestedWeight + ", age=" + age + ", activityCode=" + activityCode + ", activityUnit=" + activityUnit
				+ ", activityCalories=" + activityCalories + ", activities=" + activities + ", planTakenDate="
				+ planTakenDate + ", customerLifeStyle=" + customerLifeStyle + "]";
	}
}
