package ninja.skyrocketing.fuyao.musicroom.controller;

import lombok.extern.slf4j.Slf4j;
import ninja.skyrocketing.fuyao.musicroom.common.message.Response;
import ninja.skyrocketing.fuyao.musicroom.configuration.RoomContainer;
import ninja.skyrocketing.fuyao.musicroom.model.House;
import ninja.skyrocketing.fuyao.musicroom.model.User;
import ninja.skyrocketing.fuyao.musicroom.repository.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author skyrocketing Hong
 * @create 2020-06-22 1:15
 */
@Controller
@Slf4j
public class UserController {
    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private RoomContainer houseContainer;

    @RequestMapping("/user/get")
    @ResponseBody
    public Response getUsers(@RequestBody User user, HttpServletRequest accessor) {
        if (user.getHouseId() == null || "".equals(user.getHouseId())) {
            List<House> houses = houseContainer.getHouses();
            Map<String, List> houseUser = new HashMap<>();
            for (House house : houses) {
                houseUser.put(house.getName() + ":" + house.getId(), sessionRepository.getSession(house.getId()));
            }
            return Response.success(houseUser, "获取用户成功");
        } else {
            return Response.success(sessionRepository.getSession(user.getHouseId()), "获取用户成功");
        }
    }
}
