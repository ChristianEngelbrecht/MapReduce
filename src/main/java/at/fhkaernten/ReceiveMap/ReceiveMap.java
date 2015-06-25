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
 * This verticle receives data packages from ReadData module and performs map and reduce operations
 */
public class ReceiveMap extends Verticle {
    private Logger log;
    private EventBus bus;
    private boolean free;
    private int port;
    private Map<String,Object> wordMap;
    private NetSocket socketToClose;
    private NetServer dataServer;
    private String address;
    int tmp = 0;

    @Override
    public void start(){
        initialize();

        bus.registerHandler("receiveMap.set.free", new Handler<Message<Boolean>>() {

            @Override
            public void handle(Message<Boolean> message) {
                free = message.body();
            }
        });

        bus.registerHandler("map.data.word", new Handler<Message<String>>() {

            @Override
            public void handle(Message<String> message){
                parseMetaDataAndInitializeDataSet(message.body());
                container.logger().info("finishedMap:" + wordMap.get("#ID#"));
                bus.send("reduceSend.address", new JsonObject(wordMap));
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
                        String[] tmp = buffer.toString().split("#UUID#");
                        container.logger().info("receiveData:" + tmp[1]);
                        bus.send(address, tmp[0]);
                        netSocket.close();
                    }
                }));
            }
        }).listen(++port);

    }

    /**
     * This methods is responsible for parsing the metadata and calls the map and reduce method (countWords)
     */
    private void parseMetaDataAndInitializeDataSet(String message){
        String[] metaData = message.split("#START#");
        String[] dataArray = metaData.length == 2 ? metaData[0].split(" ") : null;
        wordMap.clear();
        Arrays.stream(dataArray).parallel().forEach(s -> countWords((String) s));
        String[] meta = metaData[1].split("#TIME#");
        if(wordMap.get("#TIME#") == null){
            wordMap.put("#TIME#", meta[1]);
        } else{
            wordMap.put("ERROR", "Adding timestamp failed");
        }
        meta = meta[0].split("#SOURCE#");
        if(wordMap.get("#SOURCE#") == null){
            wordMap.put("#SOURCE#", meta[1]);
        } else{
            wordMap.put("ERROR", "Adding source failed");
        }
        if(wordMap.get("#ID#") == null){
            wordMap.put("#ID#",  meta[0].replace("#ID#", ""));
        } else{
            wordMap.put("ERROR", "Adding UUID failed");
        }
    }

    /**
     * This method counts the words and saves the result in a hashmap the word as a key and the amount of words as a value
     */
    public void countWords(String s){
        if(wordMap.get(s) == null){
            wordMap.put(s,1);
        }else{
            try {
                tmp = (Integer) wordMap.get(s);
                wordMap.put(s, ++tmp);
            } catch (Exception e){
                log.warn(s + " " + tmp);
            }
        } // if-else
    }

    private void initialize(){
        log = container.logger();
        bus = vertx.eventBus();
        address = container.config().getString("address");
        port =container.config().getInteger("port");
        free = true;
        wordMap = new HashMap<>();
    }

    @Override
    public void stop(){
        if (socketToClose != null){
            try{
                socketToClose.close();
            } catch (Exception e){}
        }
        try {
            dataServer.close();
        } finally {
            log.info("Stopping ReceiveMap-Verticle.");
        }
    }
}
