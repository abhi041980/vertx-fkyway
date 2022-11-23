package com.fightitaway.service;

public class CalculationResult {
	
	private Integer carb;
	private Integer protien;
	private Integer fat;
	private Integer fiber;
	private Integer calories;
	private Integer bmr;
	private Integer activityCalories;
	
	public Integer getCalories() {
		return calories;
	}
	public void setCalories(Integer calories) {
		this.calories = calories;
	}
	public Integer getCarb() {
		return carb;
	}
	public void setCarb(Integer carb) {
		this.carb = carb;
	}
	public Integer getProtien() {
		return protien;
	}
	public void setProtien(Integer protien) {
		this.protien = protien;
	}
	public Integer getFat() {
		return fat;
	}
	public void setFat(Integer fat) {
		this.fat = fat;
	}
	public Integer getFiber() {
		return fiber;
	}
	public void setFiber(Integer fiber) {
		this.fiber = fiber;
	}
	/**
	 * @return the bmr
	 */
	public Integer getBmr() {
		return bmr;
	}
	/**
	 * @param bmr the bmr to set
	 */
	public void setBmr(Integer bmr) {
		this.bmr = bmr;
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
}
