package ninja.skyrocketing.fuyao.musicroom.service;

/**
 * @author skyrocketing Hong
 */
public interface MailService {
    /**
     * send simple mail
     *
     * @param subject subject
     * @param content content
     * @return boolean
     */
    boolean sendSimpleMail(String subject, String content);
}
