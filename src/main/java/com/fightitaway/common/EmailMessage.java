package com.fightitaway.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EmailMessage {

	private String mailSender;
	private List<String> toEmail;
	private List<String> ccEmail;
	private List<String> bccEmail;
	private List<String> replyToEmail;
	private String subject;
	private String body;
	private String bodyContentType;
	private String filePath;
	private byte[] attachmentFileContent;
	private String attachmentMimeType;
	private String fileNameForAttachment;
	private String mailTitle;

	public EmailMessage(String mailSender, List<String> toEmail, List<String> ccEmail, List<String> bccEmail,
			List<String> replyToEmail, String subject, String body, String bodyContentType) {
		this.mailSender = mailSender;
		this.toEmail = toEmail;
		EmailAddressValidator.validateEmailAddress(toEmail);
		this.ccEmail = setAddressList(ccEmail);
		this.bccEmail = setAddressList(bccEmail);
		System.out.println("##### REPLY TO EMAIL -->> " + replyToEmail);
		this.replyToEmail = setAddressList(replyToEmail);
		this.subject = subject;
		this.body = body;
		this.bodyContentType = bodyContentType;
	}

	private List<String> setAddressList(List<String> emailAddresses) {
		if (AppUtil.isEmpty(emailAddresses) || emailAddresses.size() == 0) {
			emailAddresses = new ArrayList<String>();
		} else {
			System.out.println("##### setAddressList() -->> " + emailAddresses);
			EmailAddressValidator.validateEmailAddress(emailAddresses);
		}
		return emailAddresses;
	}

	public String getMailSender() {
		return mailSender;
	}

	public List<String> getToEmail() {
		return toEmail;
	}

	public List<String> getCcEmail() {
		return ccEmail;
	}

	public List<String> getBccEmail() {
		return bccEmail;
	}

	/**
	 * @return the replyToEmail
	 */
	public List<String> getReplyToEmail() {
		return replyToEmail;
	}

	/**
	 * @param replyToEmail the replyToEmail to set
	 */
	public void setReplyToEmail(List<String> replyToEmail) {
		this.replyToEmail = replyToEmail;
	}

	public String getSubject() {
		return subject;
	}

	public String getBody() {
		return body;
	}

	public String getBodyContentType() {
		return bodyContentType;
	}

	public String getFilePath() {
		return filePath;
	}

	public String getFileNameForAttachment() {
		return fileNameForAttachment;
	}

	@Override
	public String toString() {
		return "EmailMessage [mailSender=" + mailSender + ", toEmail=" + toEmail + ", ccEmail=" + ccEmail
				+ ", bccEmail=" + bccEmail + ", replyToEmail=" + replyToEmail + ", subject=" + subject + ", body="
				+ body + ", bodyContentType=" + bodyContentType + ", filePath=" + filePath + ", attachmentFileContent="
				+ Arrays.toString(attachmentFileContent) + ", attachmentMimeType=" + attachmentMimeType
				+ ", fileNameForAttachment=" + fileNameForAttachment + ", mailTitle=" + mailTitle + "]";
	}

	public String getMailTitle() {
		return mailTitle;
	}

	public void setMailTitle(String mailTitle) {
		this.mailTitle = mailTitle;
	}

	public byte[] getAttachmentFileContent() {
		return attachmentFileContent;
	}

	public void setAttachmentFileContent(byte[] attachmentFileContent) {
		this.attachmentFileContent = attachmentFileContent;
	}

	public String getAttachmentMimeType() {
		return attachmentMimeType;
	}

	public void setAttachmentMimeType(String attachmentMimeType) {
		this.attachmentMimeType = attachmentMimeType;
	}
}
