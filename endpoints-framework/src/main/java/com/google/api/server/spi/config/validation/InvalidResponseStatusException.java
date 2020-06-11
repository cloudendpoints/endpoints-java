package com.google.api.server.spi.config.validation;

import com.google.api.server.spi.config.model.ApiMethodConfig;

/**
 * Exception for API methods with an invalid status code (not a '2xx Success' code).
 */
public class InvalidResponseStatusException extends ApiMethodConfigInvalidException {
	public InvalidResponseStatusException(ApiMethodConfig config, int responseStatus) {
		super(config, getErrorMessage(responseStatus));
	}

	private static String getErrorMessage(int responseStatus) {
		return String.format(
				"Invalid response status '%d'. The response status must be a 2xx success code.", responseStatus);
	}
}
