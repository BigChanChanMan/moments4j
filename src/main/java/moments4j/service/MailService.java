package moments4j.service;

import jakarta.mail.internet.MimeMessage;
import moments4j.model.User;
import moments4j.settings.MomentsSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Port of moments/emails.py: renders emails/confirmation|reset_password|change_email(.html/.txt)
 * and sends asynchronously. Emails are skipped when no mail server is configured (debug mode).
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender sender;
    private final TemplateEngine templateEngine;
    private final MomentsSettings settings;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    public MailService(JavaMailSender sender, TemplateEngine templateEngine, MomentsSettings settings) {
        this.sender = sender;
        this.templateEngine = templateEngine;
        this.settings = settings;
    }

    /** Mirrors send_mail(to, subject, template, **kwargs). */
    public void sendMail(String to, String subject, String template, Map<String, Object> model) {
        if (!StringUtils.hasText(mailHost)) {
            log.debug("Skip sending email in debug mode.");
            log.debug("To: {}", to);
            log.debug("Subject: {}", subject);
            log.debug("Template: {}", template);
            return;
        }
        Context ctx = new Context();
        model.forEach(ctx::setVariable);
        String html = templateEngine.process(template + ".html", ctx);
        String text = templateEngine.process(template + ".txt", ctx);
        String fullSubject = settings.getMailSubjectPrefix() + subject;

        executor.submit(() -> {
            try {
                MimeMessage message = sender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom("Moments Admin <" + mailUsername + ">");
                helper.setTo(to);
                helper.setSubject(fullSubject);
                helper.setText(text, html);
                sender.send(message);
            } catch (Exception e) {
                log.error("Failed to send email to {}", to, e);
            }
        });
    }

    public void sendConfirmationEmail(User user, String token, String confirmUrl) {
        sendMail(user.getEmail(), "Email Confirmation", "emails/confirmation",
                Map.of("user", user, "token", token, "confirmUrl", confirmUrl));
    }

    public void sendResetPasswordEmail(User user, String token, String resetUrl) {
        sendMail(user.getEmail(), "Password Reset", "emails/reset_password",
                Map.of("user", user, "token", token, "resetUrl", resetUrl));
    }

    public void sendChangeEmailEmail(User user, String token, String to, String changeEmailUrl) {
        sendMail(to, "Change Email Confirmation", "emails/change_email",
                Map.of("user", user, "token", token, "changeEmailUrl", changeEmailUrl));
    }
}
