package at.fhkaernten.ReceiveMap;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.net.NetClient;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.platform.Verticle;

/**
 * Created by Christian on 07.05.2015.
 * This verticle notifies ReadText module that Host is available again
 */
public class NotifyVerticle extends Verticle {
    private EventBus bus;
    private Logger log;
    private NetSocket socketToClose;
    private NetClient client;
    private int remotePort;
    private int ownPort;

    @Override
    public void start(){
        client = vertx.createNetClient();
        bus = vertx.eventBus();
        log = container.logger();
        remotePort = container.config().getInteger("remote_port");
        ownPort = container.config().getInteger("own_port");

        final JsonObject config = container.config();
        bus.registerHandler("notify", new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                log.info("Free again - Give me more data");
                client.connect(remotePort, new AsyncResultHandler<NetSocket>() {
                    @Override
                    public void handle(AsyncResult<NetSocket> socket) {
                        if (socket.succeeded()){
                            socketToClose = socket.result();
                            socket.result().write(String.valueOf(ownPort));
                            socket.result().close();
                    }
                    }
                });
            }
        });
    }

    @Override
    public void stop(){
        if (socketToClose != null){
            try{
                socketToClose.close();
            } catch (Exception e){}
        } else {
            log.info("Stopping NotifyVerticle-Verticle.");
        }
        try {
            client.close();
        } finally {
            log.info("Stopping NotifyVerticle-Verticle.");
        }
    }
}
