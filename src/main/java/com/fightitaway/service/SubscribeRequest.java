package com.fightitaway.service;

public class SubscribeRequest {

	private String paymentId;

	private String orderId;

	private String couponCode;

	private String emailId;

	private String expiryDate;

	private String txnId;

	private Double amount;

	private String signature;

	public Double getAmount() {
		return amount;
	}

	public void setAmount(Double amount) {
		this.amount = amount;
	}

	private Integer validityInMonth;
	private Integer ValidityInDays;

	public Integer getValidityInMonth() {
		return validityInMonth;
	}

	public void setValidityInMonth(Integer validityInMonth) {
		this.validityInMonth = validityInMonth;
	}

	public String getTxnId() {
		return txnId;
	}

	public void setTxnId(String txnId) {
		this.txnId = txnId;
	}

	public String getExpiryDate() {
		return expiryDate;
	}

	public void setExpiryDate(String expiryDate) {
		this.expiryDate = expiryDate;
	}

	public String getPaymentId() {
		return paymentId;
	}

	public void setPaymentId(String paymentId) {
		this.paymentId = paymentId;
	}

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public String getCouponCode() {
		return couponCode;
	}

	public void setCouponCode(String couponCode) {
		this.couponCode = couponCode;
	}

	public String getEmailId() {
		return emailId;
	}

	public void setEmailId(String emailId) {
		this.emailId = emailId;
	}

	/**
	 * @return the signature
	 */
	public String getSignature() {
		return signature;
	}

	/**
	 * @param signature the signature to set
	 */
	public void setSignature(String signature) {
		this.signature = signature;
	}

	/**
	 * @return the validityInDays
	 */
	public Integer getValidityInDays() {
		return ValidityInDays;
	}

	/**
	 * @param validityInDays the validityInDays to set
	 */
	public void setValidityInDays(Integer validityInDays) {
		ValidityInDays = validityInDays;
	}

	@Override
	public String toString() {
		return "SubscribeRequest [paymentId=" + paymentId + ", orderId=" + orderId + ", couponCode=" + couponCode
				+ ", emailId=" + emailId + ", expiryDate=" + expiryDate + ", txnId=" + txnId + ", amount=" + amount
				+ ", signature=" + signature + ", validityInMonth=" + validityInMonth + ", ValidityInDays="
				+ ValidityInDays + "]";
	}
}
