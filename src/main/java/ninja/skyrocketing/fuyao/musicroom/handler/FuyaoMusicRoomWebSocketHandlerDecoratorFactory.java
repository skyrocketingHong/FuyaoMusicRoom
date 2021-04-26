package ninja.skyrocketing.fuyao.musicroom.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

/**
 * @author skyrocketing Hong
 */
@Component
public class FuyaoMusicRoomWebSocketHandlerDecoratorFactory implements WebSocketHandlerDecoratorFactory {

    @Autowired
    @Lazy
    private FuyaoMusicRoomWebSocketHandler fuyaoMusicRoomWebSocketHandler;

    @Override
    public WebSocketHandler decorate(WebSocketHandler handler) {
        return this.fuyaoMusicRoomWebSocketHandler;
    }
}
