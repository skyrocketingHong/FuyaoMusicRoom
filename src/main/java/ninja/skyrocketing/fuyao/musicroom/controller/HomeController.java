package ninja.skyrocketing.fuyao.musicroom.controller;

import lombok.extern.slf4j.Slf4j;
import ninja.skyrocketing.fuyao.musicroom.common.message.Response;
import ninja.skyrocketing.fuyao.musicroom.configuration.FuyaoMusicRoomProperties;
import ninja.skyrocketing.fuyao.musicroom.configuration.RoomContainer;
import ninja.skyrocketing.fuyao.musicroom.model.House;
import ninja.skyrocketing.fuyao.musicroom.model.RetainKey;
import ninja.skyrocketing.fuyao.musicroom.util.IPUtils;
import ninja.skyrocketing.fuyao.musicroom.util.StringUtils;
import ninja.skyrocketing.fuyao.musicroom.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author skyrocketing Hong
 * @create 2020-06-10 10:36
 */
@Controller
@Slf4j
public class HomeController {
    @Autowired
    private RoomContainer houseContainer;
    @Autowired
    private FuyaoMusicRoomProperties fuyaoMusicRoomProperties;

    public static String inputStreamToBase64(InputStream inputStream) throws IOException {
        return Base64.getEncoder().encodeToString(inputStreamToBytes(inputStream));
    }

    /**
     * inputStream 转化成 bytes
     *
     * @param inputStream
     * @return
     * @throws IOException
     */
    public static byte[] inputStreamToBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buff = new byte[4096];
        int index = 0;
        while ((index = inputStream.read(buff, 0, 4096)) > 0) {
            baos.write(buff, 0, index);
        }
        return baos.toByteArray();
    }

    @RequestMapping("/house/add")
    @ResponseBody
    public Response addHouse(@RequestBody House house, HttpServletRequest accessor) {
        String sessionId = UUIDUtils.getUUID8Len(accessor.getSession().getId());
        if (house.getName() == null || house.getName() == "") {
            return Response.failure((Object) null, "房间名称不能为空");
        }
        if (house.getNeedPwd() != null && house.getNeedPwd()) {
            if (house.getPassword() == null || "".equals(house.getPassword().trim())) {
                return Response.failure((Object) null, "房间密码不能为空");
            } else if (StringUtils.isUrlSpecialCharacter(house.getPassword())) {
                return Response.failure((Object) null, "密码不能有如下字符：空格、?、%、#、&、=、+");
            }
            house.setPassword(house.getPassword().trim());
        }
        if (houseContainer.contains(sessionId)) {
            return Response.failure((Object) null, "你已经创建过一个房间，待其被自动腾空方可再创建");
        }
        if (houseContainer.size() >= fuyaoMusicRoomProperties.getHouseSize()) {
            return Response.failure((Object) null, "暂时不能新增房间，待其他空房间被自动腾空方可创建。");
        }
        String ip = IPUtils.getRemoteAddress(accessor);
        if (houseContainer.isBeyondIpHouse(ip, fuyaoMusicRoomProperties.getIpHouse())) {
            return Response.failure((Object) null, "该网络暂时不能新增房间，待其他空房间被自动腾空方可创建。");
        }
        if (house.getEnableStatus() != null && house.getEnableStatus()) {
            if (house.getRetainKey() == null || "".equals(house.getRetainKey().trim())) {
                return Response.failure((Object) null, "订单号不能为空");
            }
            RetainKey key = houseContainer.getRetainKey(house.getRetainKey());
            if (key == null) {
                return Response.failure((Object) null, "订单号不存在或须等待3分钟后系统才能生效，如3分钟后还不存在，请加q群：672905926");
            } else if (key.getIsUsed()) {
                return Response.failure((Object) null, "订单号已被使用");
            } else if (key.getExpireTime() != null && key.getExpireTime() < System.currentTimeMillis()) {
                return Response.failure((Object) null, "订单号已过期");
            }
            key.setHouseId(sessionId);
            key.setRemoteAddress(ip);
            key.setUsedTime(System.currentTimeMillis());
            key.setIsUsed(true);
            houseContainer.updateRetainKey(key);
        }
        house.setId(sessionId);
        house.setCreateTime(System.currentTimeMillis());
        house.setSessionId(sessionId);
        house.setRemoteAddress(ip);
        houseContainer.add(house);
        return Response.success(sessionId, "创建房间成功");
    }

    @RequestMapping("/house/enter")
    @ResponseBody
    public Response enterHouse(@RequestBody House house, HttpServletRequest accessor) {
        String sessionId = UUIDUtils.getUUID8Len(accessor.getSession().getId());
        if (!houseContainer.contains(house.getId())) {
            return Response.failure((Object) null, "房间已经不存在");
        } else {
            House matchHouse = houseContainer.get(house.getId());
            if (matchHouse.getNeedPwd() && !matchHouse.getPassword().equals(house.getPassword())) {// || !matchHouse.getSessionId().equals(sessionId)
                return Response.failure((Object) null, "请输入正确的房间密码");
            }
        }
        return Response.success(sessionId, "进入房间成功");
    }

    @RequestMapping("/house/search")
    @ResponseBody
    public Response searchHouse() {
        CopyOnWriteArrayList<House> houses = houseContainer.getHouses();
        ArrayList<House> housesSimple = new ArrayList<>();
        for (House house : houses) {
            House houseSimple = new House();
            houseSimple.setName(house.getName());
            houseSimple.setId(house.getId());
            houseSimple.setDesc(house.getDesc());
            houseSimple.setCreateTime(house.getCreateTime());
            houseSimple.setNeedPwd(house.getNeedPwd());
            houseSimple.setPopulation(fuyaoMusicRoomProperties.getSessions(house.getId()).size());
            housesSimple.add(houseSimple);
        }
        return Response.success(housesSimple, "房间列表");
    }

    @RequestMapping("/house/edit")
    @ResponseBody
    public Response edit(@RequestBody House house, HttpServletRequest accessor) {
        // TODO  权限认证
        House housePrimitive = houseContainer.get(house.getId());
        if (housePrimitive == null) {
            return Response.failure((Object) null, "当前房间不存在");
        }
        if (house.getCanDestroy() != null && house.getCanDestroy()) {
            houseContainer.destroy(house.getId());
            if (housePrimitive.getEnableStatus() != null && housePrimitive.getEnableStatus()) {
                houseContainer.refreshHouses();
            }
            return Response.success(housePrimitive, "销毁房间成功");
        }
        if (house.getForbiddenModiPwd() != null) {
            housePrimitive.setForbiddenModiPwd(house.getForbiddenModiPwd());
        }
        if (house.getNeedPwd() != null) {
            housePrimitive.setNeedPwd(house.getNeedPwd());
        }
        if (house.getPassword() != null) {
            housePrimitive.setPassword(house.getPassword());
        }
        if (house.getName() != null) {
            housePrimitive.setName(house.getName());
        }
        if (house.getDesc() != null) {
            housePrimitive.setDesc(house.getDesc());
        }
        if (house.getEnableStatus() != null) {
            housePrimitive.setEnableStatus(house.getEnableStatus());
        }
        if (housePrimitive.getEnableStatus() != null && housePrimitive.getEnableStatus()) {
            houseContainer.refreshHouses();
        }
        return Response.success(housePrimitive, "修改房间成功");
    }

    @RequestMapping("/house/setSize/{size}")
    @ResponseBody
    public Response setSize(@PathVariable Integer size, HttpServletRequest accessor) {
        // TODO  权限认证
        if (size == null) {
            return Response.failure((Object) fuyaoMusicRoomProperties.getHouseSize(), "当前可创建房间总数" + (fuyaoMusicRoomProperties.getHouseSize() - houseContainer.size()));
        } else {
            fuyaoMusicRoomProperties.setHouseSize(size);
            return Response.success(fuyaoMusicRoomProperties.getHouseSize(), "当前可创建房间总数" + (fuyaoMusicRoomProperties.getHouseSize() - houseContainer.size()));
        }
    }

    @RequestMapping("/house/addRetainKey/{key}")
    @ResponseBody
    public Response addRetainKey(@PathVariable String key, HttpServletRequest accessor) {
        if (key == null) {
            return Response.failure((Object) null, "订单号不能为空");
        } else {
            RetainKey retainKey = houseContainer.getRetainKey(key);
            if (retainKey != null) {
                return Response.failure(retainKey, "订单号已经存在");
            }
            RetainKey retainKeyNew = new RetainKey();
            retainKeyNew.setIsUsed(false);
            retainKeyNew.setCreateTime(System.currentTimeMillis());
            retainKeyNew.setKey(key);
            houseContainer.addRetainKey(retainKeyNew);
            return Response.success((Object) null, "添加成功");
        }
    }

    @RequestMapping("/house/showRetainKey")
    @ResponseBody
    public Response showRetainKey(HttpServletRequest accessor) {
        return Response.success(houseContainer.showRetainKey(), "所有订单号");
    }

    @RequestMapping("/house/removeRetainKey/{key}")
    @ResponseBody
    public Response removeRetainKey(@PathVariable String key, HttpServletRequest accessor) {
        if (key == null) {
            return Response.failure((Object) null, "订单号不能为空");
        } else {
            houseContainer.removeRetainKey(key);
            return Response.success((Object) null, "移除成功");
        }
    }

    @RequestMapping("/house/get")
    @ResponseBody
    public Response get(@RequestBody House house, StompHeaderAccessor accessor) {
        House housePrimitive = houseContainer.get(house.getId());
        House houseSimple = new House();
        if (housePrimitive != null) {
            houseSimple.setName(housePrimitive.getName());
            houseSimple.setId(housePrimitive.getId());
            houseSimple.setDesc(housePrimitive.getDesc());
            houseSimple.setCreateTime(housePrimitive.getCreateTime());
            houseSimple.setNeedPwd(housePrimitive.getNeedPwd());
            return Response.success(houseSimple, "房间详情");
        } else {
            return Response.failure(houseSimple, "房间已经不存在了。");
        }
    }

}
