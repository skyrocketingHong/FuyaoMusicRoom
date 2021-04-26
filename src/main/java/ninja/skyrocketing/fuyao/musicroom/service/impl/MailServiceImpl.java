package ninja.skyrocketing.fuyao.musicroom.service.impl;

import lombok.extern.slf4j.Slf4j;
import ninja.skyrocketing.fuyao.musicroom.configuration.FuyaoMusicRoomProperties;
import ninja.skyrocketing.fuyao.musicroom.service.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * @author skyrocketing Hong
 */
@Service
@Slf4j
public class MailServiceImpl implements MailService {
    @Autowired
    private FuyaoMusicRoomProperties fuyaoMusicRoomProperties;
    @Autowired
    private JavaMailSender javaMailSender;

    @Override
    public boolean sendSimpleMail(String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fuyaoMusicRoomProperties.getMailSendFrom());
        message.setTo(fuyaoMusicRoomProperties.getMailSendTo());
        message.setSubject(subject);
        message.setText(content);

        try {
            javaMailSender.send(message);
            return true;
        } catch (Exception e) {
            log.info("邮件发送异常: {}", e.getMessage());
            return false;
        }
    }

}
