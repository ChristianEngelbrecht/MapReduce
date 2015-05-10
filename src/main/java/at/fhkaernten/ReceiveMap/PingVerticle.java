package at.fhkaernten.ReceiveMap;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.net.NetClient;
import org.vertx.java.core.net.NetServer;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.platform.Verticle;

/**
 * Created by Christian
 */
public class PingVerticle extends Verticle {
    private Logger log;
    private EventBus bus;
    private NetClient client;
    private boolean free;
    private JsonObject config;
    @Override
    public void start(){
        log = container.logger();
        bus = vertx.eventBus();
        free = true;
        config = container.config();

        NetServer server = vertx.createNetServer();
        int port = config.getInteger("Port");
        server.connectHandler(new Handler<NetSocket>() {
            @Override
            public void handle(final NetSocket netSocket) {
                //log.info("A new client is connected");

                netSocket.dataHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer buffer) {
                        //log.info("I received " + buffer.toString());
                        if (buffer.toString().equals("ping") && free == true){
                            free = false;
                            //log.info("MapReduce client with port " + config.getInteger("Port") + " is available");
                            // Port des aktuell verfügbaren MapReduce Clients wird retourniert
                            netSocket.write(String.valueOf(config.getInteger("Port")));
                            bus.send("receiveMap.set.free", free);
                        } else {
                            if (!buffer.toString().equals("ping")) {
                                free = false;
                                netSocket.close();
                                //log.info("Received data for mapReduce task");
                                bus.send("map.data", buffer.toString());
                            } else {
                                netSocket.close();

                            }
                        }
                    }
                });
            }
        }).listen(container.config().getInteger("Port"), container.config().getString("IP"));
    }
}
