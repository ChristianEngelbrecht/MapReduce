package at.fhkaernten.ReduceSend;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.net.NetClient;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.platform.Verticle;

/**
 * Created by Christian on 04.04.2015.
 */
public class ReduceSend extends Verticle {
    private EventBus bus;
    private Logger log;
    @Override
    public void start(){

        final NetClient client = vertx.createNetClient();
        bus = vertx.eventBus();
        log = container.logger();
        bus.registerHandler("reduceSend.address", new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                //log.info("Received data to reduce and send to output.");
                client.connect(container.config().getInteger("Port"), container.config().getString("IP"), new Handler<AsyncResult<NetSocket>>() {
                    @Override
                    public void handle(AsyncResult<NetSocket> socket) {
                        socket.result().write("Hello to Output");
                        socket.result().close();
                    }
                });
            }
        });
    }

}
