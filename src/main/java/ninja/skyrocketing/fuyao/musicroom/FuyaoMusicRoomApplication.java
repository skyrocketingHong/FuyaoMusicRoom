package ninja.skyrocketing.fuyao.musicroom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author skyrocketing Hong
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
//mvn clean && mvn package "-Dmaven.test.skip=true"
public class FuyaoMusicRoomApplication {
    public static void main(String[] args) {
        SpringApplication.run(FuyaoMusicRoomApplication.class);
    }
}
