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
        final NetClient client = vertx.createNetClient();
        clientToClose = client;
        bus = vertx.eventBus();
        log = container.logger();
        bus.registerHandler("reduceSend.address", new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                log.info(message.body());
                /**client.connect(container.config().getInteger("Port"), container.config().getString("IP"), new Handler<AsyncResult<NetSocket>>() {
                    @Override
                    public void handle(AsyncResult<NetSocket> socket) {
                        socketToClose = socket.result();
                        socket.result().write("Hello to Output");
                        socket.result().close();
                    }
                });**/
            }
        });
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
