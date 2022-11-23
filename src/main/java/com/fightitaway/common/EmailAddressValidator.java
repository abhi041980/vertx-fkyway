package com.fightitaway.common;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailAddressValidator {

	private static final String EMAIL_REGEX = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
			+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

	private static final Pattern pattern = Pattern.compile(EMAIL_REGEX);

	public static boolean validateEmailAddress(List<String> emailAddresses) {
		if (AppUtil.isEmpty(emailAddresses))
			throw new IllegalArgumentException("email address list is null/blank");

		for (String email : emailAddresses)
			validateEmailAddress(email);

		return true;
	}

	public static boolean validateEmailAddress(String email) {
		if (AppUtil.isEmpty(email))
			throw new IllegalArgumentException("email address is null/blank");

		Matcher matcher = pattern.matcher(email);
		if (!matcher.matches())
			throw new IllegalArgumentException("email address [" + email + "] is invalid");

		return true;
	}
}
