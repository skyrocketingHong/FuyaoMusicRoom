package ninja.skyrocketing.fuyao.musicroom.service;

import ninja.skyrocketing.fuyao.musicroom.model.User;

/**
 * @author skyrocketing Hong
 */
public interface AuthService {

    /**
     * root 认证
     *
     * @param sessionId the client session id
     * @param password  password
     * @return 成功返回 true，失败返回 false
     */
    boolean authRoot(String sessionId, String password, String houseId);

    /**
     * admin 认证
     *
     * @param sessionId the client session id
     * @param password  password
     * @return 成功返回 true，失败返回 false
     */
    boolean authAdmin(String sessionId, String password, String houseId);

    void setAdminPassword(String password, String houseId);

    void setRootPassword(String password, String houseId);

    void updateUser(User user, String houseId);

}
