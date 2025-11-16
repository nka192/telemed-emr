package com.nayoung.telemed.notification.service;

import com.nayoung.telemed.notification.dto.NotificationDTO;
import com.nayoung.telemed.notification.entity.Notification;
import com.nayoung.telemed.notification.repo.NotificationRepo;
import com.nayoung.telemed.users.entity.User;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepo notificationRepo;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Override
    @Async
    public void sendEmail(NotificationDTO notificationDTO, User user) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );
            helper.setTo(notificationDTO.getRecipient());
            helper.setSubject(notificationDTO.getSubject());

            // use template if provided
            if (notificationDTO.getTemplateName() != null) {
                Context context = new Context();
                context.setVariables(notificationDTO.getTemplateVariables());
                String htmlContent = templateEngine.process(notificationDTO.getTemplateName(), context);

                helper.setText(htmlContent, true);
            } else {
                helper.setText(notificationDTO.getMessage(), true);
            }

            mailSender.send(mimeMessage);
            log.info("Email sent out");

            // save to database table
            Notification notificationToSave = Notification.builder()
                    .recipient(notificationDTO.getRecipient())
                    .subject(notificationDTO.getSubject())
                    .message(notificationDTO.getMessage())
                    .type(notificationDTO.getType())
                    .user(user)
                    .build();

            notificationRepo.save(notificationToSave);

        } catch (Exception e) {
            log.info(e.getMessage());
        }
    }
}
