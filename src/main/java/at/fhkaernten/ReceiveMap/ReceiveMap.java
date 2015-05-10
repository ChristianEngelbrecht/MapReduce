package at.fhkaernten.ReceiveMap;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.net.NetServer;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.core.parsetools.RecordParser;
import org.vertx.java.platform.Verticle;

import java.util.Arrays;
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
    private Map<String,Integer> wordMap;
    private int check = 0;
    @Override
    public void start(){
        // Initialisieren der Variablen
        log = container.logger();
        bus = vertx.eventBus();
        port =container.config().getInteger("Port");
        free = true;
        charMap = new HashMap<>();
        wordMap = new HashMap<>();
        bus.registerHandler("receiveMap.set.free", new Handler<Message<Boolean>>() {
            @Override
            public void handle(Message<Boolean> message) {
                free = message.body();
            }
        });

        bus.registerHandler("map.data.char", new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message){
                message.body().chars().parallel().forEach(c -> countChar((char) c));
                // System.out.print(message.body());
                bus.send("pingVerticle.set.free", true);
            }
        });

        bus.registerHandler("map.data.word", new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message){
                System.out.println(message.body());
                if (message.body().startsWith("  Inconsistencies in Big Data")){
                    check++;
                }
                System.out.println(message.body().length());
                //message.body().chars().parallel().forEach(c -> countChar((char) c));
                /**String[] test = message.body().split(" ");
                Arrays.stream(test).parallel().forEach(s -> countWords((String) s));
                // System.out.print(message.body());
                bus.send("pingVerticle.set.free", true);**/
            }
        });

        NetServer dataServer = vertx.createNetServer();
        dataServer.connectHandler(new Handler<NetSocket>() {
            @Override
            public void handle(final NetSocket netSocket) {
                log.info("A data client has connected");
                System.out.println(dataServer.getReceiveBufferSize());
                netSocket.dataHandler(RecordParser.newDelimited("$END$", new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer buffer) { // Default Buffer is UTF-8 coded
                        bus.send(container.config().getString("address"), buffer.toString());
                        //log.warn("RECEIVED JUHUUUUHUUHUJ");
                        //netSocket.close();
                    }
                }));
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

    public void countWords(String s){
        if(wordMap.get(s) == null){
            wordMap.put(s,1);
        }else{
            int tmp = (wordMap.get(s));
            wordMap.put(s, ++tmp);
        }
    }
}
