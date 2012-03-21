package org.openremote.android.console.util;

import android.content.Context;

public class PhoneInformationBase extends PhoneInformation {
	
	/**
	 * Returns a message that it can not retrieve a e-mail address, should maybe spawn a pop up to 
	 * ask the user for their e-mail address
	 * @param context The current application context
	 * @return "Not implemented"
	 */
	@Override
	public String getEmailAddress(Context context) {
		return "Not implemented";
	}
	
}
