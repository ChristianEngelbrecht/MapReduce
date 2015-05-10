package at.fhkaernten.ReceiveMap;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.net.NetClient;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.platform.Verticle;

/**
 * Created by Christian on 07.05.2015.
 * This verticle notifies ReadText module that Host is available again
 */
public class NotifyVerticle extends Verticle {
    private EventBus bus;

    @Override
    public void start(){
        NetClient client = vertx.createNetClient();
        bus.registerHandler("notify", new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                client.connect(666, new AsyncResultHandler<NetSocket>() {
                    @Override
                    public void handle(AsyncResult<NetSocket> socket) {
                        if (socket.succeeded()){
                            socket.result().write("1234");
                            socket.result().close();
                    }
                    }
                });
            }
        });
    }
}
