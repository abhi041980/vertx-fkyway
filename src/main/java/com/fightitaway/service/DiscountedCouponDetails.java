package com.fightitaway.service;

public class DiscountedCouponDetails {

	private String emailId;

	private String uniqueCouponCode;

	private Integer validityInDays;

	private Boolean isCouponActive;

	private String generatedBy;

	private String consumedBy;

	private String couponExpiryDateTime;

	private String createdDateTime;

	private String consumedDateTime;

	private String errorMessage;

	/**
	 * @return the emailId
	 */
	public String getEmailId() {
		return emailId;
	}

	/**
	 * @param emailId the emailId to set
	 */
	public void setEmailId(String emailId) {
		this.emailId = emailId;
	}

	/**
	 * @return the uniqueCouponCode
	 */
	public String getUniqueCouponCode() {
		return uniqueCouponCode;
	}

	/**
	 * @param uniqueCouponCode the uniqueCouponCode to set
	 */
	public void setUniqueCouponCode(String uniqueCouponCode) {
		this.uniqueCouponCode = uniqueCouponCode;
	}

	/**
	 * @return the validityInDays
	 */
	public Integer getValidityInDays() {
		return validityInDays;
	}

	/**
	 * @param validityInDays the validityInDays to set
	 */
	public void setValidityInDays(Integer validityInDays) {
		this.validityInDays = validityInDays;
	}

	/**
	 * @return the isCouponActive
	 */
	public Boolean getIsCouponActive() {
		return isCouponActive;
	}

	/**
	 * @param isCouponActive the isCouponActive to set
	 */
	public void setIsCouponActive(Boolean isCouponActive) {
		this.isCouponActive = isCouponActive;
	}

	/**
	 * @return the generatedBy
	 */
	public String getGeneratedBy() {
		return generatedBy;
	}

	/**
	 * @param generatedBy the generatedBy to set
	 */
	public void setGeneratedBy(String generatedBy) {
		this.generatedBy = generatedBy;
	}

	/**
	 * @return the consumedBy
	 */
	public String getConsumedBy() {
		return consumedBy;
	}

	/**
	 * @param consumedBy the consumedBy to set
	 */
	public void setConsumedBy(String consumedBy) {
		this.consumedBy = consumedBy;
	}

	/**
	 * @return the couponExpiryDateTime
	 */
	public String getCouponExpiryDateTime() {
		return couponExpiryDateTime;
	}

	/**
	 * @param couponExpiryDateTime the couponExpiryDateTime to set
	 */
	public void setCouponExpiryDateTime(String couponExpiryDateTime) {
		this.couponExpiryDateTime = couponExpiryDateTime;
	}

	/**
	 * @return the createdDateTime
	 */
	public String getCreatedDateTime() {
		return createdDateTime;
	}

	/**
	 * @param createdDateTime the createdDateTime to set
	 */
	public void setCreatedDateTime(String createdDateTime) {
		this.createdDateTime = createdDateTime;
	}

	/**
	 * @return the consumedDateTime
	 */
	public String getConsumedDateTime() {
		return consumedDateTime;
	}

	/**
	 * @param consumedDateTime the consumedDateTime to set
	 */
	public void setConsumedDateTime(String consumedDateTime) {
		this.consumedDateTime = consumedDateTime;
	}

	/**
	 * @return the errorMessage
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * @param errorMessage the errorMessage to set
	 */
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	@Override
	public String toString() {
		return "DiscountedCouponDetails [emailId=" + emailId + ", uniqueCouponCode=" + uniqueCouponCode
				+ ", validityInDays=" + validityInDays + ", isCouponActive=" + isCouponActive + ", generatedBy="
				+ generatedBy + ", consumedBy=" + consumedBy + ", couponExpiryDateTime=" + couponExpiryDateTime
				+ ", createdDateTime=" + createdDateTime + ", consumedDateTime=" + consumedDateTime + ", errorMessage="
				+ errorMessage + "]";
	}
}
