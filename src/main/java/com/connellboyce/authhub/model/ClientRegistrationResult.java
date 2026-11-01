package com.connellboyce.authhub.model;

import com.connellboyce.authhub.model.dao.MongoRegisteredClient;

public record ClientRegistrationResult(MongoRegisteredClient client, String clientSecret) {
}
