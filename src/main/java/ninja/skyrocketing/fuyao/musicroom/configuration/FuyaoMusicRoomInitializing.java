package ninja.skyrocketing.fuyao.musicroom.configuration;

import lombok.extern.slf4j.Slf4j;
import ninja.skyrocketing.fuyao.musicroom.job.MusicTopJob;
import ninja.skyrocketing.fuyao.musicroom.model.House;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author skyrocketing Hong
 */
@Component
@Slf4j
public class FuyaoMusicRoomInitializing implements InitializingBean {
    private final FuyaoMusicRoomProperties fuyaoMusicRoomProperties;
    private final ResourceLoader resourceLoader;
    private final RoomContainer houseContainer;

    @Autowired
    private MusicTopJob musicTopJob;

    public FuyaoMusicRoomInitializing(FuyaoMusicRoomProperties fuyaoMusicRoomProperties, ResourceLoader resourceLoader, RoomContainer houseContainer) {
        this.fuyaoMusicRoomProperties = fuyaoMusicRoomProperties;
        this.resourceLoader = resourceLoader;
        this.houseContainer = houseContainer;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        initialize(houseContainer.clearSurvive());
    }

    /**
     * 读取默认列表
     */
    private void initDefaultMusicId() {
        try {
            ArrayList<String> musicList = musicTopJob.getMusicTop();
            if (musicList == null || musicList.size() == 0) {
                InputStream inputStream = resourceLoader.getResource(fuyaoMusicRoomProperties.getDefaultMusicFile()).getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String musicId;
                // 逐行读取
                while ((musicId = bufferedReader.readLine()) != null) {
                    musicList.add(musicId);
                }
            }
            FuyaoMusicRoomProperties.setDefaultListByJob(musicList);
        } catch (Exception e) {
            log.info("读取默认音乐列表失败，Exception：[{}]", e.getMessage());
        }
    }

    /**
     * 初始化 config
     * 初始化 default
     */
    private void initialize(CopyOnWriteArrayList<House> houses) throws IOException {
        log.info("初始化工作开始");
        this.initDefaultMusicId();
        houseContainer.initialize(houses);
        log.info("初始化工作完成");
    }
}
