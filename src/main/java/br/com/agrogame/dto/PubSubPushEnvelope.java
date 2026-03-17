package br.com.agrogame.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PubSubPushEnvelope(PubSubMessage message, String subscription) {
}
