package ninja.skyrocketing.fuyao.musicroom.service.impl;

import lombok.extern.slf4j.Slf4j;
import ninja.skyrocketing.fuyao.musicroom.configuration.FuyaoMusicRoomProperties;
import ninja.skyrocketing.fuyao.musicroom.model.User;
import ninja.skyrocketing.fuyao.musicroom.repository.ConfigRepository;
import ninja.skyrocketing.fuyao.musicroom.repository.SessionRepository;
import ninja.skyrocketing.fuyao.musicroom.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author skyrocketing Hong
 */
@Service
@Slf4j
public class AuthServiceImpl implements AuthService {
    @Autowired
    private FuyaoMusicRoomProperties fuyaoMusicRoomProperties;
    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private ConfigRepository configRepository;

    @Override
    public boolean authRoot(String sessionId, String password, String houseId) {
        if (null == password) {
            return false;
        }
        String rootPassword = configRepository.getRootPassword(houseId);
        if (null == rootPassword) {
            rootPassword = fuyaoMusicRoomProperties.getRoleRootPassword();
            configRepository.initRootPassword(houseId);
        }
        User user = sessionRepository.getSession(sessionId, houseId);
        if (password.equals(rootPassword)) {
            // update role
            user.setRole("root");
            sessionRepository.setSession(user, houseId);
            return true;
        } else {
            user.setRole("default");
            sessionRepository.setSession(user, houseId);
            return false;
        }
    }

    @Override
    public boolean authAdmin(String sessionId, String password, String houseId) {
        if (null == password) {
            return false;
        }
        String adminPassword = configRepository.getAdminPassword(houseId);
        if (null == adminPassword) {
            adminPassword = fuyaoMusicRoomProperties.getRoleRootPassword();
            configRepository.initAdminPassword(houseId);
        }
        User user = sessionRepository.getSession(sessionId, houseId);
        if (password.equals(adminPassword)) {
            // update role
            user.setRole("admin");
            sessionRepository.setSession(user, houseId);
            return true;
        } else {
            user.setRole("default");
            sessionRepository.setSession(user, houseId);
            return false;
        }
    }

    @Override
    public void setAdminPassword(String password, String houseId) {
        configRepository.setAdminPassword(password, houseId);
    }

    @Override
    public void setRootPassword(String password, String houseId) {
        configRepository.setRootPassword(password, houseId);
    }

    @Override
    public void updateUser(User user, String houseId) {
        sessionRepository.setSession(user, houseId);
    }
}
