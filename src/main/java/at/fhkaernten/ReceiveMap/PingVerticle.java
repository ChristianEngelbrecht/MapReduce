package at.fhkaernten.ReceiveMap;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.net.NetServer;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.platform.Verticle;

/**
 * Class PingVerticle is used to signal ReadData that the host is free by replying with its own port number
 */
public class PingVerticle extends Verticle {
    private Logger log;
    private EventBus bus;
    private boolean free;
    private JsonObject config;
    private NetSocket socketToClose;
    private NetServer server;

    @Override
    public void start(){
        initialize();


        server.connectHandler(new Handler<NetSocket>() {

            @Override
            public void handle(final NetSocket netSocket) {
                socketToClose = netSocket;

                netSocket.dataHandler(new Handler<Buffer>() {

                    @Override
                    public void handle(Buffer buffer) {
                        if (buffer.toString().equals("ping") && free == true){
                            free = false;
                            netSocket.write(String.valueOf(config.getInteger("port")));
                            bus.send("receiveMap.set.free", free);
                        } else {
                            if (!buffer.toString().equals("ping")) {
                                free = false;
                                netSocket.close();
                                bus.send("map.data", buffer.toString());
                            } else {
                                netSocket.close();

                            } // if-else
                        } // if-else
                    }
                });
            }
        }).listen(container.config().getInteger("port"));
    }

    public void initialize(){
        log = container.logger();
        bus = vertx.eventBus();
        free = true;
        config = container.config();
        server = vertx.createNetServer();
    }

    @Override
    public void stop(){
        if (socketToClose != null){
            try{
                socketToClose.close();
            } catch (Exception e){}
        }
        try {
            server.close();
        } finally {
            log.info("Stopping PingVerticle-Verticle.");
        }
    }
}
