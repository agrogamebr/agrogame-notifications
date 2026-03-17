package br.com.agrogame.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NotificationEvent(String eventType, String recipientEmail, Map<String, Object> templateVariables) {

}