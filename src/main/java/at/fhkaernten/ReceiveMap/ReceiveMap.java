package at.fhkaernten.ReceiveMap;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
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
    private Map<Character,Object> charMap;
    private Map<String,Object> wordMap;
    private NetSocket socketToClose;
    private NetServer dataServer;
    private String address;
    int tmp = 0;
    @Override
    public void start(){
        // Initialisieren der Variablen
        log = container.logger();
        bus = vertx.eventBus();
        address = container.config().getString("address");
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
                bus.send("pingVerticle.set.free", true);
            }
        });

        bus.registerHandler("map.data.word", new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message){
                // Splitten des empfangenen Strings bei $START$ um Timestamp (Startzeit) sowie Quellhost Name zu bekommen
                String[] metaData = message.body().split("#START#");
                String[] dataArray = metaData.length == 2 ? metaData[0].split(" ") : null;
                wordMap.clear();
                Arrays.stream(dataArray).parallel().forEach(s -> countWords((String) s));
                // Anhängen der Metadaten
                String[] meta = metaData[1].split("#TIME#");
                if(wordMap.get("#TIME#") == null){
                    wordMap.put("#TIME#", meta[1]);
                } else{
                    wordMap.put("ERROR", "Adding timestamp failed");
                }
                if(wordMap.get("#SOURCE#") == null){
                    wordMap.put("#SOURCE#", meta[0].replace("#SOURCE#", ""));
                } else{
                    wordMap.put("ERROR", "Adding source failed");
                }

                bus.send("reduceSend.address", new JsonObject(wordMap));
                bus.send("notify", true);
            }
        });

        dataServer = vertx.createNetServer();
        dataServer.connectHandler(new Handler<NetSocket>() {
            @Override
            public void handle(final NetSocket netSocket) {
                socketToClose = netSocket;
                log.info("A data client has connected");
                netSocket.dataHandler(RecordParser.newDelimited("#END#", new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer buffer) { // Default Buffer is UTF-8 coded
                        bus.send(address, buffer.toString());
                        netSocket.close();
                    }
                }));
            }
        }).listen(++port);

    }

    public void countChar(char c){
        if(charMap.get(c) == null){
            charMap.put(c,1);
        }else{
            int tmp = (Integer) (charMap.get(c));
            charMap.put(c, ++tmp);
        }
        System.out.print(charMap.toString());

    }

    public void countWords(String s){
        if(wordMap.get(s) == null){
            wordMap.put(s,1);
        }else{
            try {
                tmp = (Integer) wordMap.get(s);
                wordMap.put(s, ++tmp);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public void stop(){
        if (socketToClose != null){
            try{
                socketToClose.close();
            } catch (Exception e){}
        } else {
            log.info("Stopping ReceiveMap-Verticle.");
        }
        try {
            dataServer.close();
        } finally {
            log.info("Stopping ReceiveMap-Verticle.");
        }
    }
}
