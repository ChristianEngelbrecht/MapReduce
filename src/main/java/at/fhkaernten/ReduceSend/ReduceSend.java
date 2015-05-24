package at.fhkaernten.ReduceSend;

import jdk.nashorn.internal.runtime.JSONFunctions;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
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
    private NetSocket socketToClose;
    private NetClient clientToClose;
    @Override
    public void start(){
        initialize();

        final NetClient client = vertx.createNetClient();
        clientToClose = client;
        bus.registerHandler("reduceSend.address", new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                client.connect(container.config().getInteger("portOutput"), container.config().getString("IP"), new Handler<AsyncResult<NetSocket>>() {
                    @Override
                    public void handle(AsyncResult<NetSocket> socket) {
                        socketToClose = socket.result();
                        container.logger().trace("sendResult:" + message.body().getString("ID"));
                        socket.result().write(message.body().encode() + "#END#");
                        socket.result().close();
                    }
                });
            }
        });
    }

    private void initialize(){
        bus = vertx.eventBus();
        log = container.logger();
    }

    @Override
    public void stop(){
        if (socketToClose != null){
            try{
                socketToClose.close();
            } catch (Exception e){}
        }
        try {
            clientToClose.close();
        } finally {
            log.info("Stopping ReceiveMap-Verticle.");
        }
    }
}
