package com.fightitaway.service;

public class RefundRequest {

	private Double amount;

	private String couponCode;

	private String email;

	private String paymentId;

	private String statusCode;

	private Boolean isRefunded;

	private Integer refundedAmount;

	private String refundedDate;

	private String description;

	public Double getAmount() {
		return amount;
	}

	public void setAmount(Double amount) {
		this.amount = amount;
	}

	public String getCouponCode() {
		return couponCode;
	}

	public void setCouponCode(String couponCode) {
		this.couponCode = couponCode;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPaymentId() {
		return paymentId;
	}

	public void setPaymentId(String paymentId) {
		this.paymentId = paymentId;
	}

	/**
	 * @return the statusCode
	 */
	public String getStatusCode() {
		return statusCode;
	}

	/**
	 * @param statusCode the statusCode to set
	 */
	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}

	/**
	 * @return the isRefunded
	 */
	public Boolean getIsRefunded() {
		return isRefunded;
	}

	/**
	 * @param isRefunded the isRefunded to set
	 */
	public void setIsRefunded(Boolean isRefunded) {
		this.isRefunded = isRefunded;
	}

	/**
	 * @return the refundedAmount
	 */
	public Integer getRefundedAmount() {
		return refundedAmount;
	}

	/**
	 * @param refundedAmount the refundedAmount to set
	 */
	public void setRefundedAmount(Integer refundedAmount) {
		this.refundedAmount = refundedAmount;
	}

	/**
	 * @return the refundedDate
	 */
	public String getRefundedDate() {
		return refundedDate;
	}

	/**
	 * @param refundedDate the refundedDate to set
	 */
	public void setRefundedDate(String refundedDate) {
		this.refundedDate = refundedDate;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}
}
