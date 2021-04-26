package ninja.skyrocketing.fuyao.musicroom.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.extern.slf4j.Slf4j;
import ninja.skyrocketing.fuyao.musicroom.common.page.HulkPage;
import ninja.skyrocketing.fuyao.musicroom.job.MusicTopJob;
import ninja.skyrocketing.fuyao.musicroom.model.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author skyrocketing Hong
 * @version 0.0.1
 * @date 2021-04-24 15:53:28
 */
@Slf4j
public class MusicSearchUtils {
    /**
     * 根据歌手json数组拼接歌手名称
     */
    private static String getSingerNameFromArray(JSONArray singerArray) {
        int singerSize = singerArray.size();
        StringBuilder singerNames = new StringBuilder();
        for (int j = 0; j < singerSize; j++) {
            singerNames.append(singerArray.getJSONObject(j).getString("name")).append(",");
        }
        if (singerNames.toString().endsWith(",")) {
            singerNames = new StringBuilder(singerNames.substring(0, singerNames.length() - 1));
        }
        return singerNames.toString();
    }

    //QQ音乐

    /**
     * 根据关键字获取QQ音乐歌曲
     */
    public static Music getQQMusicByIdOrKeyword(String api, int failLimit, String keyword, boolean isId) {
        Music music;
        int failCount = 0;
        HttpResponse<String> response;
        while (failCount < failLimit) {
            try {
                if (isId) {
                    response = Unirest.get(api + "/song?songmid=" + StringUtils.encodeString(keyword)).asString();
                } else {
                    response = Unirest.get(api + "/search?key=" + StringUtils.encodeString(keyword)).asString();
                }
                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                    if (jsonObject.get("result").equals(100)) {
                        JSONObject data;
                        if (isId) {
                            data = jsonObject.getJSONObject("data").getJSONObject("track_info");
                            // 获取歌曲mid
                            String mid = data.getString("mid");
                            // 获取歌手
                            String singerNames = getSingerNameFromArray(data.getJSONArray("singer"));
                            // 获取专辑mid
                            String albummid = data.getJSONObject("album").getString("mid");
                            // 获取歌曲链接
                            String url = getQQMusicLinkById(api, failLimit, mid);
                            Album album = Album.builder()
                                    .id(data.getJSONObject("album").getInteger("id")).name(data.getJSONObject("album").getString("name"))
                                    .artist(singerNames).pictureUrl("https://y.gtimg.cn/music/photo_new/T002R300x300M000" + albummid + ".jpg").build();
                            // 构建音乐
                            music = Music.builder()
                                    .source("QQMusic").id(mid).lyric(getQQLyricsById(api, failLimit, mid)).name(data.getString("name")).artist(singerNames)
                                    .duration(data.getLong("interval") * 1000).url(url).album(album)
                                    .pictureUrl("https://y.gtimg.cn/music/photo_new/T002R300x300M000" + albummid + ".jpg").build();
                        } else {
                            data = jsonObject.getJSONObject("data").getJSONArray("list").getJSONObject(0);
                            // 获取歌曲mid
                            String mid = data.getString("songmid");
                            // 获取歌手
                            String singerNames = getSingerNameFromArray(data.getJSONArray("singer"));
                            // 获取专辑mid
                            String albummid = data.getString("albummid");
                            // 获取歌曲链接
                            String url = getQQMusicLinkById(api, failLimit, mid);
                            // 构建专辑
                            Album album = Album.builder()
                                    .id(data.getInteger("albumid")).name(data.getString("albumname")).artist(singerNames).id(data.getInteger("albumid"))
                                    .pictureUrl("https://y.gtimg.cn/music/photo_new/T002R300x300M000" + albummid + ".jpg").build();
                            // 构建音乐
                            music = Music.builder()
                                    .source("QQMusic").id(mid).lyric(getQQLyricsById(api, failLimit, mid)).name(data.getString("songname")).artist(singerNames)
                                    .duration(data.getLong("interval") * 1000).url(url).album(album)
                                    .pictureUrl("https://y.gtimg.cn/music/photo_new/T002R300x300M000" + albummid + ".jpg").build();
                        }
                        return music;
                    } else {
                        return null;
                    }
                }
            } catch (Exception e) {
                failCount++;
                log.error("QQ音乐根据关键字获取异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            }
        }
        return null;
    }

    /**
     * 根据QQ音乐mid获取歌词
     */
    private static String getQQLyricsById(String api, int failLimit, String id) {
        HttpResponse<String> response;
        int failCount = 0;
        while (failCount < failLimit) {
            try {
                response = Unirest.get(api + "/lyric?songmid=" + id).asString();
                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                    if (jsonObject.get("result").equals(100)) {
                        JSONObject data = jsonObject.getJSONObject("data");
                        return data.getString("lyric");
                    } else {
                        return null;
                    }
                }
            } catch (Exception e) {
                failCount++;
                log.error("QQ音乐获取歌词异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            }
        }
        return "";
    }

    /**
     * 根据QQ音乐mid获取链接
     */
    public static String getQQMusicLinkById(String api, int failLimit, String id) {
        int failCount = 0;
        HttpResponse<String> response;
        while (failCount < failLimit) {
            try {
                response = Unirest.get(api + "/song/url?id=" + StringUtils.encodeString(id)).asString();
                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                    log.info("获取QQ音乐链接结果：{}", jsonObject);
                    return jsonObject.getString("data");
                }
            } catch (Exception e) {
                failCount++;
                log.error("QQ音乐链接获取异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            }
        }
        return null;
    }

    private static JSONObject getJsonArrayFromQQMusicSearchResult(JSONArray data, int i) {
        JSONObject jsonObject = data.getJSONObject(i);

        JSONObject buildJSONObject = new JSONObject();
        buildJSONObject.put("picture_url", "https://y.gtimg.cn/music/photo_new/T002R300x300M000" + jsonObject.getString("albummid") + ".jpg");
        buildJSONObject.put("artist", getSingerNameFromArray(jsonObject.getJSONArray("singer")));
        buildJSONObject.put("name", jsonObject.getString("songname"));
        buildJSONObject.put("id", jsonObject.getString("songmid"));
        buildJSONObject.put("duration", jsonObject.getInteger("interval") * 1000);

        JSONObject privilege = new JSONObject();
        privilege.put("st", 1);
        privilege.put("fl", 1);
        buildJSONObject.put("privilege", privilege);

        JSONObject album = new JSONObject();
        album.put("picture_url", "https://y.gtimg.cn/music/photo_new/T002R300x300M000" + jsonObject.getString("albummid") + ".jpg");
        album.put("id", jsonObject.getString("albumid"));
        album.put("name", jsonObject.getString("albumname"));
        buildJSONObject.put("album", album);

        return buildJSONObject;
    }

    /**
     * 根据歌曲id搜索QQ音乐歌单
     */
    public static HulkPage getQQMusicMusicSonglistById(String api, String id, HulkPage hulkPage) {
        StringBuilder url = new StringBuilder().append(api).append("/song?id=").append(id);
        HttpResponse<String> response;
        try {
            response = Unirest.get(url.toString()).asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("result") == 100) {
                JSONArray data = responseJsonObject.getJSONObject("data").getJSONArray("songlist");
                int size = data.size();
                JSONArray buildJSONArray = new JSONArray();
                int offset = (hulkPage.getPageIndex() - 1) * hulkPage.getPageSize();
                int pages = (size + hulkPage.getPageSize() - 1) / hulkPage.getPageSize();
                if (hulkPage.getPageIndex() > pages) {
                    List list = JSONObject.parseObject(JSONObject.toJSONString(new JSONArray()), List.class);
                    hulkPage.setData(list);
                    hulkPage.setTotalSize(size);
                    return hulkPage;
                }
                for (int i = offset; i < (hulkPage.getPageIndex() == pages ? size : hulkPage.getPageIndex() * hulkPage.getPageSize()); i++) {
                    buildJSONArray.add(getJsonArrayFromQQMusicSearchResult(data, i));
                }
                List list = JSONObject.parseObject(JSONObject.toJSONString(buildJSONArray), List.class);
                hulkPage.setData(list);
                hulkPage.setTotalSize(size);
            } else {
                log.info("QQ音乐搜索接口异常, 请检查音乐服务");
                return null;
            }
        } catch (Exception e) {
            log.error("QQ音乐搜索接口异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
        }
        return hulkPage;
    }

    /**
     * 根据关键字在QQ音乐中查询所有歌曲
     */
    public static HulkPage getQQMusicByKeyword(String api, String keyword, HulkPage hulkPage) {
        StringBuilder url = new StringBuilder()
                .append(api)
                .append("/search?key=")
                .append(StringUtils.encodeString(keyword))
                .append("&pageNo=").append(hulkPage.getPageIndex())
                .append("&pageSize=").append(hulkPage.getPageSize());
        HttpResponse<String> response;
        try {
            response = Unirest.get(url.toString()).asString();
            System.out.println(url);
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("result") == 100) {
                JSONArray data = responseJsonObject.getJSONObject("data").getJSONArray("list");
                int size = data.size();
                JSONArray buildJSONArray = new JSONArray();
                for (int i = 0; i < size; i++) {
                    buildJSONArray.add(getJsonArrayFromQQMusicSearchResult(data, i));
                }
                Integer count = responseJsonObject.getJSONObject("data").getInteger("total");
                List list = JSONObject.parseObject(JSONObject.toJSONString(buildJSONArray), List.class);
                hulkPage.setData(list);
                hulkPage.setTotalSize(count);
            } else {
                log.info("QQ音乐歌单搜索接口异常, 请检查音乐服务");
                return null;
            }
        } catch (Exception e) {
            log.error("QQ音乐歌单搜索接口异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
        }
        return hulkPage;
    }

    /**
     * 根据关键字搜索QQ音乐歌单
     */
    public static HulkPage getQQMusicSonglistByKeyword(String api, String keyword, HulkPage hulkPage) {
        StringBuilder url = new StringBuilder()
                .append(api)
                .append("/search?t=2&key=")
                .append(StringUtils.encodeString(keyword))
                .append("&pageNo=").append(hulkPage.getPageIndex())
                .append("&pageSize=").append(hulkPage.getPageSize());
        HttpResponse<String> response;
        try {
            response = Unirest.get(url.toString()).asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("result") == 100) {
                JSONArray data = responseJsonObject.getJSONObject("data").getJSONArray("list");
                int size = data.size();
                JSONArray buildJSONArray = new JSONArray();
                for (int i = 0; i < size; i++) {
                    JSONObject jsonObject = data.getJSONObject(i);
                    JSONObject buildJSONObject = new JSONObject();
                    buildJSONObject.put("name", jsonObject.getString("dissname"));
                    buildJSONObject.put("desc", jsonObject.getString("introduction"));
                    buildJSONObject.put("id", jsonObject.getString("dissid"));
                    buildJSONObject.put("pictureUrl", jsonObject.getString("imgurl"));
                    buildJSONObject.put("playCount", jsonObject.getInteger("listennum"));
                    buildJSONObject.put("bookCount", null);
                    JSONObject creator = jsonObject.getJSONObject("creator");
                    buildJSONObject.put("creator", creator.getString("name"));
                    buildJSONObject.put("creatorUid", creator.getString("qq"));
                    buildJSONObject.put("songCount", jsonObject.getInteger("song_count"));
                    buildJSONArray.add(buildJSONObject);
                }
                Integer count = responseJsonObject.getJSONObject("data").getInteger("total");
                List list = JSONObject.parseObject(JSONObject.toJSONString(buildJSONArray), List.class);
                hulkPage.setData(list);
                hulkPage.setTotalSize(count);
            } else {
                log.info("QQ音乐歌单搜索接口异常, 请检查音乐服务");
                return null;
            }
        } catch (Exception e) {
            log.error("QQ音乐歌单搜索接口异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
        }
        return hulkPage;
    }

    public static String[] getQQMusicSonglistByKeyword(String api, String id) {
        StringBuilder url = new StringBuilder()
                .append(api)
                .append("/songlist?id=")
                .append(id);
        HttpResponse<String> response;
        ArrayList<String> ids = new ArrayList<>();
        try {
            response = Unirest.get(url.toString())
                    .asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("result") == 100) {
                JSONArray data = responseJsonObject.getJSONObject("data").getJSONArray("songlist");
                int size = data.size();
                if (size != 0) {
                    for (int i = 0; i < size; i++) {
                        JSONObject jsonObject = data.getJSONObject(i);
                        String songmid = jsonObject.getString("songmid");
                        ids.add(songmid);
                    }
                } else {
                    return new String[]{};
                }
            } else {
                log.info("QQ音乐歌单搜索接口异常, 请检查音乐服务");
                return null;
            }
        } catch (Exception e) {
            log.error("QQ音乐歌单搜索接口异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            return null;
        }
        String[] idsStr = new String[ids.size()];
        ids.toArray(idsStr);
        return idsStr;
    }

    /**
     * 根据QQ音乐用户搜索歌单
     */
    public static HulkPage getQQMusicSonglistByUser(String api, String keyword, HulkPage hulkPage) {
        StringBuilder url = new StringBuilder().append(api).append("/user/songlist?id=").append(keyword);
        HttpResponse<String> response;
        try {
            response = Unirest.get(url.toString())
                    .asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("result") == 100) {
                JSONArray data = responseJsonObject.getJSONObject("data").getJSONArray("list");
                int size = data.size();
                int offset = (hulkPage.getPageIndex() - 1) * hulkPage.getPageSize();
                int pages = (size + hulkPage.getPageSize() - 1) / hulkPage.getPageSize();
                if (hulkPage.getPageIndex() > pages) {
                    List list = JSONObject.parseObject(JSONObject.toJSONString(new JSONArray()), List.class);
                    hulkPage.setData(list);
                    hulkPage.setTotalSize(size);
                    return hulkPage;
                }
                JSONArray buildJSONArray = new JSONArray();
                JSONObject creatorJson = responseJsonObject.getJSONObject("data").getJSONObject("creator");
                String creator = creatorJson.getString("hostname");
                String creatorUid = creatorJson.getString("hostuin");
                for (int i = offset; i < (hulkPage.getPageIndex() == pages ? size : hulkPage.getPageIndex() * hulkPage.getPageSize()); i++) {
                    JSONObject jsonObject = data.getJSONObject(i);
                    JSONObject buildJSONObject = new JSONObject();
                    buildJSONObject.put("name", jsonObject.getString("diss_name"));
                    buildJSONObject.put("desc", "");
                    buildJSONObject.put("id", jsonObject.getString("tid"));
                    buildJSONObject.put("pictureUrl", jsonObject.getString("diss_cover"));
                    buildJSONObject.put("playCount", jsonObject.getInteger("listen_num"));
                    buildJSONObject.put("bookCount", null);
                    buildJSONObject.put("creator", creator);
                    buildJSONObject.put("creatorUid", creatorUid);
                    buildJSONObject.put("songCount", jsonObject.getInteger("song_cnt"));
                    buildJSONArray.add(buildJSONObject);
                }
                List list = JSONObject.parseObject(JSONObject.toJSONString(buildJSONArray), List.class);
                hulkPage.setData(list);
                hulkPage.setTotalSize(size);
            } else {
                log.info("QQ音乐用户搜索歌单接口异常, 请检查音乐服务");
                return null;
            }
        } catch (Exception e) {
            log.error("QQ音乐用户搜索歌单接口异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
        }
        return hulkPage;
    }

    /**
     * 根据分类id获取QQ音乐所有歌单
     */
    public static HulkPage getQQMusicSonglistByCategoryId(String api, HulkPage hulkPage) {
        StringBuilder url = new StringBuilder()
                .append(api)
                .append("/songlist/list?category=10000000")
                .append("&pageNo=").append(hulkPage.getPageIndex())
                .append("&pageSize=").append(hulkPage.getPageSize());
        HttpResponse<String> response;
        try {
            response = Unirest.get(url.toString()).asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("result") == 100) {
                JSONArray data = responseJsonObject.getJSONObject("data").getJSONArray("list");
                int size = data.size();
                JSONArray buildJSONArray = new JSONArray();
                for (int i = 0; i < size; i++) {
                    JSONObject jsonObject = data.getJSONObject(i);
                    JSONObject buildJSONObject = new JSONObject();
                    buildJSONObject.put("name", jsonObject.getString("dissname"));
                    buildJSONObject.put("desc", jsonObject.getString("introduction"));
                    buildJSONObject.put("id", jsonObject.getString("dissid"));
                    buildJSONObject.put("pictureUrl", jsonObject.getString("imgurl"));
                    buildJSONObject.put("playCount", jsonObject.getInteger("listennum"));
                    buildJSONObject.put("bookCount", null);
                    JSONObject creator = jsonObject.getJSONObject("creator");
                    buildJSONObject.put("creator", creator.getString("name"));
                    buildJSONObject.put("creatorUid", creator.getString("qq"));
                    buildJSONObject.put("songCount", null);
                    buildJSONArray.add(buildJSONObject);
                }
                Integer count = responseJsonObject.getJSONObject("data").getInteger("total");
                List list = JSONObject.parseObject(JSONObject.toJSONString(buildJSONArray), List.class);
                hulkPage.setData(list);
                hulkPage.setTotalSize(count);
            } else {
                log.info("QQ音乐歌单分类搜索接口异常, 请检查音乐服务");
                return null;
            }
        } catch (Exception e) {
            log.error("QQ音乐歌单分类搜索接口异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
        }
        return hulkPage;
    }

    /**
     * 根据关键字查找QQ音乐用户
     */
    public static HulkPage getQQMusicUserByKeyword(String api, MusicUser musicUser, HulkPage hulkPage) {
        return hulkPage;
    }

    /**
     * 根据QQ音乐排行榜id获取歌单内容
     * */
    public static TopMusic getQQMusicHotSonglistById(String api, int failLimit, String id) {
        HttpResponse<String> response;
        StringBuilder musicIds = new StringBuilder();
        ArrayList<String> topList = new ArrayList<>();
        int failCount = 0;
        while (failCount < failLimit) {
            try {
                response = Unirest.get(api + "/top?id=" + id + "&pageSize=300")
                        .asString();
                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                    if (jsonObject.get("result").equals(100)) {
                        JSONArray data = jsonObject.getJSONObject("data").getJSONArray("list");
                        int size = data.size();
                        String musicId;
                        for (int i = 0; i < size; i++) {
                            musicId = data.getJSONObject(i).getString("mid");
                            if (musicId != null && !"".equals(musicId)) {
                                musicIds.append(musicId).append("\n");
                                topList.add(musicId);
                            }
                        }
                        break;
                    } else {
                        return new TopMusic(topList, "");
                    }
                }
            } catch (Exception e) {
                failCount++;
                log.error("QQ音乐热门歌曲获取失败; Exception: [{}]", e.getMessage());
            }
        }
        return new TopMusic(topList, musicIds.toString());
    }

    //网易云音乐

    /**
     * 根据id搜索网易云音乐歌单
     */
    public static HulkPage getNeteaseMusicSonglistHulkPageById(String api, String id, int failLimit, HulkPage hulkPage) {
        StringBuilder url = new StringBuilder()
                .append(api)
                .append("/playlist/detail?id=")
                .append(id);
        HttpResponse<String> response;
        try {
            response = Unirest.get(url.toString()).asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("code") == 200) {
                JSONArray data = responseJsonObject.getJSONObject("playlist").getJSONArray("trackIds");
                int size = data.size();
                int offset = (hulkPage.getPageIndex() - 1) * hulkPage.getPageSize();
                int pages = (size + hulkPage.getPageSize() - 1) / hulkPage.getPageSize();
                if (hulkPage.getPageIndex() > pages) {
                    List list = JSONObject.parseObject(JSONObject.toJSONString(new JSONArray()), List.class);
                    hulkPage.setData(list);
                    hulkPage.setTotalSize(size);
                    return hulkPage;
                }
                Set<String> ids = new LinkedHashSet<>();
                for (int i = offset; i < (hulkPage.getPageIndex() == pages ? size : hulkPage.getPageIndex() * hulkPage.getPageSize()); i++) {
                    JSONObject jsonObject = data.getJSONObject(i);
                    ids.add(jsonObject.getString("id"));
                }
                if (ids.size() > 0) {
                    String idsStr = String.join(",",ids);
                    List list = JSONObject.parseObject(JSONObject.toJSONString(getNeteaseMusicSonglistJsonArrayById(api, idsStr, failLimit, hulkPage)), List.class);
                    hulkPage.setData(list);
                    hulkPage.setTotalSize(size);
                }
            } else {
                log.info("根据id搜索网易云音乐歌单接口异常, 请检查音乐服务");
                return null;
            }
        } catch (Exception e) {
            log.error("根据id搜索网易云音乐歌单接口异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
        }
        return hulkPage;
    }

    public static String[] getNeteaseMusicSonglistStringArrayById(String api, String id) {
        StringBuilder url = new StringBuilder()
                .append(api)
                .append("/playlist/detail?id=")
                .append(id);
        HttpResponse<String> response;
        ArrayList<String> ids = new ArrayList();
        try {
            response = Unirest.get(url.toString())
                    .asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("code") == 200) {
                JSONArray data = responseJsonObject.getJSONObject("playlist").getJSONArray("trackIds");
                if (data == null) {
                    return new String[]{};
                }
                int size = data.size();
                for (int i = 0; i < size; i++) {
                    JSONObject jsonObject = data.getJSONObject(i);
                    ids.add(jsonObject.getString("id"));
                }
            } else {
                return null;
            }
        } catch (Exception e) {
            log.error("根据id搜索网易云音乐歌单异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            return null;
        }
        String[] idsStr = new String[ids.size()];
        ids.toArray(idsStr);
        return idsStr;
    }

    public static JSONArray getNeteaseMusicSonglistJsonArrayById(String api, String ids, int failLimit, HulkPage hulkPage) {
        HttpResponse<String> response;
        JSONArray buildJSONArray = new JSONArray();
        int failCount = 0;
        while (failCount < failLimit) {
            try {
                response = Unirest.get(api + "/song/detail?ids=" + ids).asString();
                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject json = JSONObject.parseObject(response.getBody());
                    if (json.get("code").equals(200)) {
                        buildJSONArray = new JSONArray();
                        JSONArray songs = json.getJSONArray("songs");
                        int size = songs.size();
                        for(int i = 0; i < size; i++){
                            JSONObject jsonObject = songs.getJSONObject(i);
                            JSONObject buildJSONObject = new JSONObject();
                            JSONObject albumJSON = jsonObject.getJSONObject("al");
                            JSONObject album = new JSONObject();
                            String albumid = albumJSON.getString("id");
                            String albumname = albumJSON.getString("name");
                            album.put("id",albumid);
                            album.put("name",albumname);
                            album.put("picture_url",albumJSON.getString("picUrl"));
                            JSONArray singerArray = jsonObject.getJSONArray("ar");
                            String singerNames = getSingerNameFromArray(singerArray);
                            buildJSONObject.put("picture_url","");
                            buildJSONObject.put("artist", singerNames.toString());
                            String songname = jsonObject.getString("name");
                            buildJSONObject.put("name",songname);
                            String songmid = jsonObject.getString("id");
                            buildJSONObject.put("id",songmid);
                            int interval = jsonObject.getInteger("dt");
                            buildJSONObject.put("duration",interval);
                            JSONObject privilege = new JSONObject();
                            privilege.put("st",1);
                            privilege.put("fl",1);
                            buildJSONObject.put("privilege",privilege);
                            buildJSONObject.put("album",album);
                            buildJSONArray.add(buildJSONObject);
                        }
                    }else{
                        log.error("根据id搜索网易云音乐歌单异常, 请检查音乐服务");
                    }
                    return buildJSONArray;
                }
            } catch (Exception e) {
                failCount++;
                log.error("根据id搜索网易云音乐歌单异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            }
        }
        return buildJSONArray;
    }

    /**
     * 根据关键字在网易云音乐中查找所有歌曲
     */
    public static HulkPage getNeteaseMusicByKeyword(String api, String keyword, HulkPage hulkPage) {
        StringBuilder url = new StringBuilder().append(api).append("/search");
        HttpResponse<String> response;
        try {
            response = Unirest.post(url.toString())
                    .queryString("keywords", keyword)
                    .queryString("offset", (hulkPage.getPageIndex() - 1) * hulkPage.getPageSize())
                    .queryString("limit", hulkPage.getPageSize())
                    .asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("code") == 200) {
                JSONArray data = responseJsonObject.getJSONObject("result").getJSONArray("songs");
                int size = data.size();
                JSONArray buildJSONArray = new JSONArray();
                for (int i = 0; i < size; i++) {
                    JSONObject jsonObject = data.getJSONObject(i);
                    JSONObject buildJSONObject = new JSONObject();
                    JSONObject albumObject = jsonObject.getJSONObject("album");
                    buildJSONObject.put("picture_url", "");
                    buildJSONObject.put("artist", getSingerNameFromArray(jsonObject.getJSONArray("artists")));
                    buildJSONObject.put("name", jsonObject.getString("name"));
                    buildJSONObject.put("id", jsonObject.getString("id"));
                    buildJSONObject.put("duration", jsonObject.getInteger("duration"));
                    JSONObject privilege = new JSONObject();
                    privilege.put("st", 1);
                    privilege.put("fl", 1);
                    buildJSONObject.put("privilege", privilege);
                    JSONObject album = new JSONObject();
                    album.put("picture_url", "");
                    album.put("id", albumObject.getString("id"));
                    album.put("name", jsonObject.getString("name"));
                    buildJSONObject.put("album", album);
                    buildJSONArray.add(buildJSONObject);
                }
                Integer count = responseJsonObject.getJSONObject("result").getInteger("songCount");
                List list = JSONObject.parseObject(JSONObject.toJSONString(buildJSONArray), List.class);
                hulkPage.setData(list);
                hulkPage.setTotalSize(count);
            } else {
                log.info("根据关键字在网易云音乐中查找接口异常, 请检查音乐服务");
                return null;
            }
        } catch (Exception e) {
            log.error("根据关键字在网易云音乐中查找接口异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
        }
        return hulkPage;
    }

    /**
     * 根据关键字获取网易云音乐
     */
    public static Music getNeteaseMusicByIdOrKeyword(String api, int failLimit, String keyword, boolean isId) {
        Music music;
        int failCount = 0;
        HttpResponse<String> response;
        while (failCount < failLimit) {
            try {
                if (isId) {
                    response = Unirest.get(api + "/song/detail?ids=" + keyword).asString();
                } else {
                    response = Unirest.post(api + "/search").queryString("limit", 1).queryString("offset", 0).queryString("keywords", keyword).asString();
                }
                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                    if (isId) {
                        JSONArray songs = jsonObject.getJSONArray("songs");
                        JSONObject song = songs.getJSONObject(0);
                        String artist = getSingerNameFromArray(song.getJSONArray("ar"));
                        JSONObject albumJson = song.getJSONObject("al");
                        Album album = Album.builder()
                                .id(albumJson.getInteger("id")).name(albumJson.getString("name")).artist(artist)
                                .pictureUrl(albumJson.getString("picUrl")).build();
                        music = Music.builder()
                                .source("Netease").id(keyword).lyric(getNeteaseMusicLyricsById(api, failLimit, keyword)).name(song.getString("name"))
                                .artist(artist).url(getNeteaseMusicUrlById(api, failLimit, keyword)).duration(song.getLong("dt")).album(album)
                                .pictureUrl(album.getPictureUrl()).build();
                        return music;
                    } else {
                        JSONObject result = jsonObject.getJSONObject("result");
                        if (result.getInteger("songCount") > 0) {
                            JSONObject data = result.getJSONArray("songs").getJSONObject(0);
                            String id = data.getString("id");
                            music = getNeteaseMusicByIdOrKeyword(api, failLimit, id, true);
                            return music;
                        } else {
                            log.error("网易云音乐根据关键字获取异常, 请检查音乐服务");
                            return null;
                        }
                    }
                }
            } catch (UnirestException e) {
                failCount++;
                log.error("网易云音乐根据关键字获取异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            }
        }
        return null;
    }

    /**
     * 根据网易云id获取歌词
     */
    private static String getNeteaseMusicLyricsById(String api, int failLimit, String id) {
        HttpResponse<String> response;
        int failCount = 0;
        while (failCount < failLimit) {
            try {
                Unirest.setTimeouts(10000, 15000);
                response = Unirest.get(api + "/lyric?id=" + id).asString();
                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                    if (jsonObject.get("code").equals(200)) {
                        JSONObject data = jsonObject.getJSONObject("lrc");
                        return data.getString("lyric");
                    } else {
                        return null;
                    }
                }
            } catch (Exception e) {
                failCount++;
                log.error("网易音乐获取歌词异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            }
        }
        return "";
    }

    /**
     * 根据网易云歌曲id获取链接
     */
    public static String getNeteaseMusicUrlById(String api, int failLimit, String id) {
        HttpResponse<String> response;
        String result = null;
        int failCount = 0;
        while (failCount < failLimit) {
            try {
                response = Unirest.post(api + "/song/url").queryString("br", 128000).queryString("id", id).asString();
                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                    if (jsonObject.get("code").equals(200)) {
                        JSONObject data = jsonObject.getJSONArray("data").getJSONObject(0);
                        result = data.getString("url");
                        break;
                    } else {
                        return null;
                    }
                }
            } catch (Exception e) {
                failCount++;
                log.error("获取网易云音乐链接异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
            }
        }
        return result;
    }

    /**
     * 根据关键字搜索网易云音乐歌单
     */
    public static HulkPage getNeteaseSonglistByKeyword(String api, String keyword, HulkPage hulkPage) {
        StringBuilder url = new StringBuilder().append(api).append("/search");
        HttpResponse<String> response;
        try {
            response = Unirest.post(url.toString()).queryString("type", 1000)
                    .queryString("keywords", keyword)
                    .queryString("offset", (hulkPage.getPageIndex() - 1) * hulkPage.getPageSize())
                    .queryString("limit", hulkPage.getPageSize())
                    .asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("code") == 200) {
                JSONArray data = responseJsonObject.getJSONObject("result").getJSONArray("playlists");
                int size = data.size();
                JSONArray buildJSONArray = new JSONArray();
                for (int i = 0; i < size; i++) {
                    JSONObject jsonObject = data.getJSONObject(i);
                    JSONObject buildJSONObject = new JSONObject();
                    buildJSONObject.put("name", jsonObject.getString("name"));
                    buildJSONObject.put("desc", jsonObject.getString("description"));
                    buildJSONObject.put("id", jsonObject.getString("id"));
                    buildJSONObject.put("pictureUrl", jsonObject.getString("coverImgUrl"));
                    buildJSONObject.put("playCount", jsonObject.getInteger("playCount"));
                    buildJSONObject.put("bookCount", jsonObject.getInteger("bookCount"));
                    JSONObject creator = jsonObject.getJSONObject("creator");
                    buildJSONObject.put("creator", creator.getString("nickname"));
                    buildJSONObject.put("creatorUid", creator.getString("userId"));
                    buildJSONObject.put("songCount", jsonObject.getInteger("trackCount"));
                    buildJSONArray.add(buildJSONObject);
                }
                Integer count = responseJsonObject.getJSONObject("result").getInteger("playlistCount");
                List list = JSONObject.parseObject(JSONObject.toJSONString(buildJSONArray), List.class);
                hulkPage.setData(list);
                hulkPage.setTotalSize(count);
            } else {
                log.info("网易云音乐歌单搜索接口异常, 请检查音乐服务");
                return null;
            }
        } catch (Exception e) {
            log.error("网易云音乐歌单搜索接口异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
        }
        return hulkPage;
    }

    /**
     * 根据网易云用户搜索歌单
     */
    public static HulkPage getNeteaseSonglistByUser(String api, String keyword, HulkPage hulkPage) {
        StringBuilder url = new StringBuilder().append(api).append("/user/playlist?uid=").append(keyword);
        HttpResponse<String> response;
        try {
            response = Unirest.get(url.toString())
                    .asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("code") == 200) {
                JSONArray data = responseJsonObject.getJSONArray("playlist");
                int size = data.size();
                int offset = (hulkPage.getPageIndex() - 1) * hulkPage.getPageSize();
                int pages = (size + hulkPage.getPageSize() - 1) / hulkPage.getPageSize();
                if (hulkPage.getPageIndex() > pages) {
                    List list = JSONObject.parseObject(JSONObject.toJSONString(new JSONArray()), List.class);
                    hulkPage.setData(list);
                    hulkPage.setTotalSize(size);
                    return hulkPage;
                }
                JSONArray buildJSONArray = new JSONArray();
                for (int i = offset; i < (hulkPage.getPageIndex() == pages ? size : hulkPage.getPageIndex() * hulkPage.getPageSize()); i++) {
                    JSONObject jsonObject = data.getJSONObject(i);
                    JSONObject buildJSONObject = new JSONObject();
                    buildJSONObject.put("name", jsonObject.getString("name"));
                    buildJSONObject.put("desc", jsonObject.getString("description"));
                    buildJSONObject.put("id", jsonObject.getString("id"));
                    buildJSONObject.put("pictureUrl", jsonObject.getString("coverImgUrl"));
                    buildJSONObject.put("playCount", jsonObject.getInteger("playCount"));
                    buildJSONObject.put("bookCount", jsonObject.getInteger("subscribedCount"));
                    JSONObject creator = jsonObject.getJSONObject("creator");
                    buildJSONObject.put("creator", creator.getString("nickname"));
                    buildJSONObject.put("creatorUid", creator.getString("userId"));
                    buildJSONObject.put("songCount", jsonObject.getInteger("trackCount"));
                    buildJSONArray.add(buildJSONObject);
                }
                List list = JSONObject.parseObject(JSONObject.toJSONString(buildJSONArray), List.class);
                hulkPage.setData(list);
                hulkPage.setTotalSize(size);
            } else {
                log.info("网易云音乐搜索用户接口异常, 请检查音乐服务");
                return null;
            }
        } catch (Exception e) {
            log.error("网易云音乐搜索用户接口异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
        }
        return hulkPage;
    }

    /**
     * 获取网易云热门歌单
     */
    public static HulkPage getNeteaseHotSonglist(String api, HulkPage hulkPage) {
        StringBuilder url = new StringBuilder()
                .append(api)
                .append("/top/playlist?order=hot")
                .append("&cat=").append(StringUtils.encodeString("欧美"))
                .append("&offset=").append((hulkPage.getPageIndex() - 1) * hulkPage.getPageSize())
                .append("&limit=").append(hulkPage.getPageSize());
        HttpResponse<String> response;
        try {
            response = Unirest.get(url.toString())
                    .asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("code") == 200) {
                JSONArray data = responseJsonObject.getJSONArray("playlists");
                int size = data.size();
                JSONArray buildJSONArray = new JSONArray();
                for (int i = 0; i < size; i++) {
                    JSONObject jsonObject = data.getJSONObject(i);
                    JSONObject buildJSONObject = new JSONObject();
                    buildJSONObject.put("name", jsonObject.getString("name"));
                    buildJSONObject.put("desc", jsonObject.getString("description"));
                    buildJSONObject.put("id", jsonObject.getString("id"));
                    buildJSONObject.put("pictureUrl", jsonObject.getString("coverImgUrl"));
                    buildJSONObject.put("playCount", jsonObject.getInteger("playCount"));
                    buildJSONObject.put("bookCount", jsonObject.getInteger("subscribedCount"));
                    JSONObject creator = jsonObject.getJSONObject("creator");
                    buildJSONObject.put("creator", creator.getString("nickname"));
                    buildJSONObject.put("creatorUid", creator.getString("userId"));
                    buildJSONObject.put("songCount", jsonObject.getInteger("trackCount"));
                    buildJSONArray.add(buildJSONObject);
                }
                Integer count = responseJsonObject.getInteger("total");
                List list = JSONObject.parseObject(JSONObject.toJSONString(buildJSONArray), List.class);
                hulkPage.setData(list);
                hulkPage.setTotalSize(count);
            } else {
                log.info("网易云热门歌单搜索接口异常, 请检查音乐服务");
                return null;
            }
        } catch (Exception e) {
            log.error("网易云热门歌单搜索接口异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
        }
        return hulkPage;
    }

    /**
     * 网易云用户搜索
     */
    public static HulkPage getNeteaseUserByKeyword(String api, String keyword, HulkPage hulkPage) {
        StringBuilder url = new StringBuilder().append(api).append("/search");
        HttpResponse<String> response;
        try {
            response = Unirest.post(url.toString())
                    .queryString("type", 1002)
                    .queryString("keywords", keyword)
                    .queryString("offset", (hulkPage.getPageIndex() - 1) * hulkPage.getPageSize())
                    .queryString("limit", hulkPage.getPageSize())
                    .asString();
            JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
            if (responseJsonObject.getInteger("code") == 200) {
                JSONArray data = responseJsonObject.getJSONObject("result").getJSONArray("userprofiles");
                Integer count = responseJsonObject.getJSONObject("result").getInteger("userprofileCount");
                List list = JSONObject.parseObject(JSONObject.toJSONString(data), List.class);
                hulkPage.setData(list);
                hulkPage.setTotalSize(count);
            } else {
                log.info("网易用户搜索接口异常, 请检查音乐服务");
                return null;
            }
        } catch (Exception e) {
            log.error("网易用户搜索接口异常, 请检查音乐服务; Exception: [{}]", e.getMessage());
        }
        return hulkPage;
    }

    /**
     * 根据网易云音乐排行榜id获取歌单内容
     * */
    public static TopMusic getNeteaseMusicHotSonglistById(String api, int failLimit, String id) {
        HttpResponse<String> response;
        StringBuilder musicIds = new StringBuilder();
        ArrayList<String> topList = new ArrayList<>();
        int failCount = 0;
        StringBuilder url = new StringBuilder()
                .append(api)
                .append("/playlist/detail?id=")
                .append(id);
        while (failCount < failLimit) {
            try {
                response = Unirest.get(url.toString()).asString();
                JSONObject responseJsonObject = JSONObject.parseObject(response.getBody());
                if (responseJsonObject.getInteger("code") != 200) {
                    failCount++;
                } else {
                    JSONArray data = responseJsonObject.getJSONObject("playlist").getJSONArray("trackIds");
                    int size = data.size();
                    String musicId = "";
                    for (int i = 0; i < size; i++) {
                        musicId = data.getJSONObject(i).getString("id");
                        if (musicId != null && !musicId.equals("")) {
                            musicIds.append(musicId).append("\n");
                            topList.add(musicId);
                        }
                    }
                    break;
                }
            } catch (Exception e) {
                failCount++;
                log.error("网易音乐热门歌曲获取失败; Exception: [{}]", e.getMessage());
            }
        }
        return new TopMusic(topList, musicIds.toString());
    }
}
