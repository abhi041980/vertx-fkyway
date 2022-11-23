package com.fightitaway.common;

import io.vertx.core.json.JsonArray;

public class CustomerLifeStyle {
	private String wakeup;
	private String leaveForOffice;
	private String leaveForOfficeValue;
	private String comeBack;
	private String comeBackValue;
	private String sleep;
	private String marital;
	private String stress;
	private String foodType;
	private String sleepType;
	private String alcohal;
	private String waterDrink;
	private JsonArray communities;
	private JsonArray diseases;

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
	 * @return the leaveForOfficeValue
	 */
	public String getLeaveForOfficeValue() {
		return leaveForOfficeValue;
	}

	/**
	 * @param leaveForOfficeValue the leaveForOfficeValue to set
	 */
	public void setLeaveForOfficeValue(String leaveForOfficeValue) {
		this.leaveForOfficeValue = leaveForOfficeValue;
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
	 * @return the comeBackValue
	 */
	public String getComeBackValue() {
		return comeBackValue;
	}

	/**
	 * @param comeBackValue the comeBackValue to set
	 */
	public void setComeBackValue(String comeBackValue) {
		this.comeBackValue = comeBackValue;
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
	 * @return the marital
	 */
	public String getMarital() {
		return marital;
	}

	/**
	 * @param marital the marital to set
	 */
	public void setMarital(String marital) {
		this.marital = marital;
	}

	/**
	 * @return the stress
	 */
	public String getStress() {
		return stress;
	}

	/**
	 * @param stress the stress to set
	 */
	public void setStress(String stress) {
		this.stress = stress;
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
	 * @return the alchohal
	 */
	public String getAlcohal() {
		return alcohal;
	}

	/**
	 * @param alchohal the alchohal to set
	 */
	public void setAlcohal(String alcohal) {
		this.alcohal = alcohal;
	}

	/**
	 * @return the waterDrink
	 */
	public String getWaterDrink() {
		return waterDrink;
	}

	/**
	 * @param waterDrink the waterDrink to set
	 */
	public void setWaterDrink(String waterDrink) {
		this.waterDrink = waterDrink;
	}

	/**
	 * @return the communities
	 */
	public JsonArray getCommunities() {
		return communities;
	}

	/**
	 * @param communities the communities to set
	 */
	public void setCommunities(JsonArray communities) {
		this.communities = communities;
	}

	/**
	 * @return the diseases
	 */
	public JsonArray getDiseases() {
		return diseases;
	}

	/**
	 * @param diseases the diseases to set
	 */
	public void setDiseases(JsonArray diseases) {
		this.diseases = diseases;
	}
}
