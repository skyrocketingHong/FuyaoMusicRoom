package ninja.skyrocketing.fuyao.musicroom.job;

/**
 * @author skyrocketing Hong
 * @create 2020-01-12 14:50
 */

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import lombok.extern.slf4j.Slf4j;
import ninja.skyrocketing.fuyao.musicroom.configuration.FuyaoMusicRoomProperties;
import ninja.skyrocketing.fuyao.musicroom.model.TopMusic;
import ninja.skyrocketing.fuyao.musicroom.util.FileOperater;
import ninja.skyrocketing.fuyao.musicroom.util.MusicSearchUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;

@Component
@Slf4j
public class MusicTopJob {
    @Autowired
    private FuyaoMusicRoomProperties fuyaoMusicRoomProperties;
    @Autowired
    private ResourceLoader resourceLoader;

    //每天0时30分更新
    @Scheduled(cron = "0 30 0 * * ? ")
    public void getMusicTopJob() {
        FuyaoMusicRoomProperties.setDefaultListByJob(getData());
    }

    public ArrayList<String> getMusicTop() {
        return getData();
    }

    public ArrayList<String> getData() {
        String neteaseMusicApi = fuyaoMusicRoomProperties.getMusicServeDomain();
        String qqMusicApi = fuyaoMusicRoomProperties.getMusicServeDomainQq();
        String neteaseMusicTopId = fuyaoMusicRoomProperties.getNeteaseTopId();
        String qqMusicTopId = fuyaoMusicRoomProperties.getQqMusicTopId();
        int failLimit = fuyaoMusicRoomProperties.getRetryCount();

        TopMusic neteaseMusicHotSonglist = MusicSearchUtils.getNeteaseMusicHotSonglistById(neteaseMusicApi, failLimit, neteaseMusicTopId);
        TopMusic qqMusicHotSonglist = MusicSearchUtils.getQQMusicHotSonglistById(qqMusicApi, failLimit, qqMusicTopId);

        String allMusicIdsStr = "";
        ArrayList<String> allMusicIdsList = new ArrayList<>();
        if (!"".equals(neteaseMusicHotSonglist.getTopMusicStrings())) {
            allMusicIdsStr += neteaseMusicHotSonglist.getTopMusicStrings();
            allMusicIdsList.addAll(neteaseMusicHotSonglist.getTopMusicList());
        }
        if (!"".equals(qqMusicHotSonglist.getTopMusicStrings())) {
            allMusicIdsStr += qqMusicHotSonglist.getTopMusicStrings();
            allMusicIdsList.addAll(qqMusicHotSonglist.getTopMusicList());
        }
        if (!"".equals(allMusicIdsStr)) {
            try {
                FileOperater.writefileinfo(allMusicIdsStr, resourceLoader.getResource(fuyaoMusicRoomProperties.getDefaultMusicFile()));
            } catch (IOException e) {
                log.error("写入热门歌曲id失败，IOException:[{}]", e.getMessage());
            }
        }
        return allMusicIdsList;
    }
}