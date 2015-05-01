package at.fhkaernten.ReceiveMap;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.net.NetServer;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.platform.Verticle;

import java.util.HashMap;
import java.util.Map;


/**
 * Created by Christian on 04.04.2015.
 */
public class ReceiveMap extends Verticle {
    // Deklarieren der notwendigen Variablen
    private Logger log;
    private EventBus bus;
    private boolean free;
    private int port;
    private Map<Character,Integer> charMap;
    @Override
    public void start(){
        // Initialisieren der Variablen
        log = container.logger();
        bus = vertx.eventBus();
        port =container.config().getInteger("Port");
        free = true;
        charMap = new HashMap<>();
        bus.registerHandler("receiveMap.set.free", new Handler<Message<Boolean>>() {
            @Override
            public void handle(Message<Boolean> message) {
                free = message.body();
            }
        });

        bus.registerHandler("map.data", new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message){
                message.body().chars().parallel().forEach(c -> countChar((char) c));

                // System.out.print(message.body());
                bus.send("pingVerticle.set.free", true);
            }
        });

        NetServer dataServer = vertx.createNetServer();
        dataServer.connectHandler(new Handler<NetSocket>() {
            @Override
            public void handle(final NetSocket netSocket) {
                log.info("A data client has connected");
                netSocket.dataHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer buffer) {
                        bus.send("map.data", buffer.toString());
                        //log.warn("RECEIVED JUHUUUUHUUHUJ");
                        netSocket.close();
                    }
                });
            }
        }).listen(++port);

    }

    public void countChar(char c){
        if(charMap.get(c) == null){
            charMap.put(c,1);
        }else{
            int tmp = (charMap.get(c));
            charMap.put(c, ++tmp);
        }
        System.out.print(charMap.toString());

    }

}
