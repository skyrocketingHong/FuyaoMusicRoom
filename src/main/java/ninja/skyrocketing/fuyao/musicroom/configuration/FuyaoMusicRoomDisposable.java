package ninja.skyrocketing.fuyao.musicroom.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

/**
 * @author skyrocketing Hong
 */
@Component
@Slf4j
public class FuyaoMusicRoomDisposable implements DisposableBean {

    private final RoomContainer houseContainer;

    public FuyaoMusicRoomDisposable(RoomContainer houseContainer) {
        this.houseContainer = houseContainer;
    }

    @Override
    public void destroy() throws Exception {
        log.info("销毁工作开始");
        houseContainer.destroy();
        log.info("销毁工作完成");
    }

}
