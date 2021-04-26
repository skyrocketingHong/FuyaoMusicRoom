package ninja.skyrocketing.fuyao.musicroom.configuration;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * @author skyrocketing Hong
 */
@Component
public class FuyaoMusicRoomEnvironment {

    private final Environment env;

    public FuyaoMusicRoomEnvironment(Environment env) {
        this.env = env;
    }

    /**
     * 获取服务端口
     *
     * @return port
     */
    public Integer getServerPort() {
        String result = env.getProperty("server.port");
        if (null == result) {
            return 8080;
        }
        return Integer.valueOf(result);
    }
}
