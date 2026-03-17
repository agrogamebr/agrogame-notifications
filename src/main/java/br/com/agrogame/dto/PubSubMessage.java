package br.com.agrogame.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PubSubMessage(String data, Map<String, String> attributes, String messageId) {

}
