package at.fhkaernten;


import org.apache.commons.io.IOUtils;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;


import java.io.IOException;
import java.io.InputStream;

/*
todo
 */
public class Main extends Verticle {
    @Override
      public void start(){

        deployVerticle("at.fhkaernten.ReceiveMap.ReceiveMap");
        deployVerticle("at.fhkaernten.ReceiveMap.PingVerticle");
        //deployVerticle("at.fhkaernten.ReduceSend.ReduceSend");

  }

    private void deployVerticle(final String classname) {
        try {
            container.deployVerticle(
                    classname,
                    getConfigs(classname),
                    1,
                    new AsyncResultHandler<String>() {
                        @Override
                        public void handle(AsyncResult<String> asyncResult) {
                            container.logger().info(String.format("Verticle %s has been deployed.", classname));
                        } // handle
                    } // handler
            );
        } catch (Exception e) {
            container.logger().error("Failed to deploy "+classname, e);
        }
    } // deployVerticle

    private static JsonObject getConfigs(String classname) throws IOException {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(classname.replaceAll("\\.", "/")+".json");

        JsonObject config = new JsonObject(IOUtils.toString(is, "UTF-8"));
        JsonObject c = config.getObject("config");

        return c;
    } // getConfigs
}