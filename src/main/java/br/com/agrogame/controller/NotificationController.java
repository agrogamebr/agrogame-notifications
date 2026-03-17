package br.com.agrogame.controller;

import br.com.agrogame.dto.NotificationEvent;
import br.com.agrogame.dto.PubSubPushEnvelope;
import br.com.agrogame.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RestController
@RequestMapping("/api/webhook")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    public NotificationController(ObjectMapper objectMapper, NotificationService notificationService) {
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
    }

    @PostMapping("/pubsub")
    public ResponseEntity<Void> receivePubSubMessage(@RequestBody PubSubPushEnvelope envelope) {
        try {
            // 1. Validação de sanidade para não dar NullPointerException
            if (envelope == null || envelope.message() == null || envelope.message().data() == null) {
                log.warn("Payload recebido do Pub/Sub está vazio ou sem o campo 'data'.");
                return ResponseEntity.ok().build();
            }

            // 2. Extrair e decodificar a String Base64 do campo "data"
            String base64Data = envelope.message().data();
            byte[] decodedBytes = Base64.getDecoder().decode(base64Data);
            String jsonPayload = new String(decodedBytes, StandardCharsets.UTF_8);

            log.debug("Payload decodificado: {}", jsonPayload);

            // 3. Converter o JSON decodificado para nosso NotificationEvent
            NotificationEvent event = objectMapper.readValue(jsonPayload, NotificationEvent.class);

            // 4. Mandar para o serviço processar e enviar o email
            notificationService.processAndSend(event);

        } catch (Exception e) {
            // Capturamos qualquer erro (como JSON malformado) e logamos.
            log.error("Erro processando webhook do Pub/Sub: {}", e.getMessage(), e);
            // É CRÍTICO retornar 200 OK no catch.
            // Se retornarmos 500, o Pub/Sub vai ficar tentando reenviar a mensagem eternamente.
        }

        // Retornamos 200 OK rapidamente para o Pub/Sub dar "Acknowledge" (ACK) na mensagem
        return ResponseEntity.ok().build();
    }
}
