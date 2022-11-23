package com.fightitaway.service;

import java.util.Date;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class CreateProfileRequest {

	private String name;

	private String id;

	private String email;

	private String given_name;

	private String family_name;
	private String country;

	private String picture;
	private String loginType;
	private String region;
	private String appsource;
	private String device;
	private String os;

	private String locale;

	private Boolean verified_email;

	private Date createdDate;

	public Boolean getVerified_email() {
		return verified_email;
	}

	public void setVerified_email(Boolean verified_email) {
		this.verified_email = verified_email;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getGiven_name() {
		return given_name;
	}

	public void setGiven_name(String given_name) {
		this.given_name = given_name;
	}

	public String getFamily_name() {
		return family_name;
	}

	public void setFamily_name(String family_name) {
		this.family_name = family_name;
	}

	public String getPicture() {
		return picture;
	}

	public void setPicture(String picture) {
		this.picture = picture;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public CreateProfileRequest() {
	}

	public CreateProfileRequest(JsonObject json) {
		CreateProfileRequestConverter.fromJson(json, this);
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		CreateProfileRequestConverter.toJson(this, json);
		return json;
	}

	/**
	 * @return the createdDate
	 */
	public Date getCreatedDate() {
		return createdDate;
	}

	/**
	 * @param createdDate the createdDate to set
	 */
	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	/**
	 * @return the loginType
	 */
	public String getLoginType() {
		return loginType;
	}

	/**
	 * @param loginType the loginType to set
	 */
	public void setLoginType(String loginType) {
		this.loginType = loginType;
	}

	/**
	 * @return the region
	 */
	public String getRegion() {
		return region;
	}

	/**
	 * @param region the region to set
	 */
	public void setRegion(String region) {
		this.region = region;
	}

	/**
	 * @return the appsource
	 */
	public String getAppsource() {
		return appsource;
	}

	/**
	 * @param appsource the appsource to set
	 */
	public void setAppsource(String appsource) {
		this.appsource = appsource;
	}

	/**
	 * @return the device
	 */
	public String getDevice() {
		return device;
	}

	/**
	 * @param device the device to set
	 */
	public void setDevice(String device) {
		this.device = device;
	}

	/**
	 * @return the os
	 */
	public String getOs() {
		return os;
	}

	/**
	 * @param os the os to set
	 */
	public void setOs(String os) {
		this.os = os;
	}

	/**
	 * @return the country
	 */
	public String getCountry() {
		return country;
	}

	/**
	 * @param country the country to set
	 */
	public void setCountry(String country) {
		this.country = country;
	}

	@Override
	public String toString() {
		return "CreateProfileRequest [name=" + name + ", id=" + id + ", email=" + email + ", given_name=" + given_name
				+ ", family_name=" + family_name + ", country=" + country + ", picture=" + picture + ", loginType="
				+ loginType + ", region=" + region + ", appsource=" + appsource + ", device=" + device + ", os=" + os
				+ ", locale=" + locale + ", verified_email=" + verified_email + ", createdDate=" + createdDate + "]";
	}
}
