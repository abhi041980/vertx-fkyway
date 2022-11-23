package com.fightitaway.service;

import java.util.List;

public class CustProfile {

	private List<Customer> profile;

	private String subscribeDate;

	private String targetedWeight;

	private String targetedDate;

	private String email;

	private Integer weight;

	private Double suggestedWeight;

	/**
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * @param email the email to set
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * @return the targetedWeight
	 */
	public String getTargetedWeight() {
		return targetedWeight;
	}

	/**
	 * @param targetedWeight the targetedWeight to set
	 */
	public void setTargetedWeight(String targetedWeight) {
		this.targetedWeight = targetedWeight;
	}

	/**
	 * @return the targetedDate
	 */
	public String getTargetedDate() {
		return targetedDate;
	}

	/**
	 * @param targetedDate the targetedDate to set
	 */
	public void setTargetedDate(String targetedDate) {
		this.targetedDate = targetedDate;
	}

	/**
	 * @return the weight
	 */
	public Integer getWeight() {
		return weight;
	}

	/**
	 * @param weight the weight to set
	 */
	public void setWeight(Integer weight) {
		this.weight = weight;
	}

	/**
	 * @return the suggestedWeight
	 */
	public Double getSuggestedWeight() {
		return suggestedWeight;
	}

	/**
	 * @param suggestedWeight the suggestedWeight to set
	 */
	public void setSuggestedWeight(Double suggestedWeight) {
		this.suggestedWeight = suggestedWeight;
	}

	public String getSubscribeDate() {
		return subscribeDate;
	}

	public void setSubscribeDate(String subscribeDate) {
		this.subscribeDate = subscribeDate;
	}

	/**
	 * @return the profile
	 */
	public List<Customer> getProfile() {
		return profile;
	}

	/**
	 * @param profile the profile to set
	 */
	public void setProfile(List<Customer> profile) {
		this.profile = profile;
	}

	public CustProfile() {
	}

	@Override
	public String toString() {
		return targetedWeight;
	}
}
