package br.com.agrogame.service;

import br.com.agrogame.dto.NotificationEvent;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final TemplateEngine templateEngine;
    private final Resend resend;
    private final String fromEmail;

    // A API Key será lida do application.properties ou Variáveis de Ambiente do GCP
    public NotificationService(
            TemplateEngine templateEngine,
            @Value("${resend.api.key}") String apiKey,
            @Value("${resend.from.email}") String fromEmail) {
        this.templateEngine = templateEngine;
        this.resend = new Resend(apiKey);
        this.fromEmail = fromEmail;
    }

    public void processAndSend(NotificationEvent event) {
        String templateName = resolveTemplateName(event);

        log.info("Processando evento: [{}] -> Template: [{}] para o destinatário: {}",
                event.eventType(), templateName, event.recipientEmail());

        try {
            Context context = new Context();
            if (event.templateVariables() != null) {
                context.setVariables(event.templateVariables());
            }

            String htmlBody = templateEngine.process(templateName, context);
            String subject = resolveSubject(event.eventType());

            // 4. Montar o payload do provedor Resend
            CreateEmailOptions sendEmailRequest = CreateEmailOptions.builder()
                    .from("Agrogame <"+this.fromEmail+">") //
                    .to(event.recipientEmail())
                    .subject(subject)
                    .html(htmlBody)
                    .build();

            // 5. Disparar
            CreateEmailResponse response = resend.emails().send(sendEmailRequest);
            log.info("E-mail do tipo [{}] disparado com sucesso para {}! Resend ID: {}",templateName, event.recipientEmail(), response.getId());


        } catch (ResendException e) {
            log.error("Falha na API do Resend ao enviar para {}: {}", event.recipientEmail(), e.getMessage());
        } catch (Exception e) {
            log.error("Erro interno ao montar/enviar e-mail para {}: {}", event.recipientEmail(), e.getMessage());
        }
    }

    private String resolveTemplateName(NotificationEvent event) {
        String eventType = event.eventType().toUpperCase();

        if (eventType.equals("RECUPERACAO_SENHA")) {
            return "agrogame-email-recuperacao-senha";
        }

        String tipoPerfil = "PRODUTOR";
        if (event.templateVariables() != null && event.templateVariables().containsKey("tipoPerfil")) {
            tipoPerfil = String.valueOf(event.templateVariables().get("tipoPerfil"));
        }

        String sufixo = tipoPerfil.equalsIgnoreCase("EMPRESA") ? "_empresa" : "_produtor";

        // 4. Retornamos o nome do template concatenado com o sufixo (ex: cadastro_aprovado_empresa)
        return switch (eventType) {
            case "CADASTRO_APROVADO" -> "cadastro_aprovado" + sufixo;
            case "AGUARDANDO_APROVACAO" -> "aguardando_aprovacao" + sufixo;
            default -> "hello-world";
        };
    }

    private String resolveSubject(String eventType) {
        return switch (eventType.toUpperCase()) {
            case "CADASTRO_APROVADO" -> "Bem-vindo ao Agrogame! Seu cadastro foi aprovado.";
            case "AGUARDANDO_APROVACAO" -> "Recebemos seu cadastro no Agrogame";
            case "RECUPERACAO_SENHA" -> "Instruções de acesso - Agrogame";
            default -> "Notificação Agrogame";
        };
    }
}
