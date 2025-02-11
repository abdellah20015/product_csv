package com.example.csv;

import com.example.csv.configuration.Conf;
import com.example.csv.configuration.Db;
import com.example.csv.constants.Fields;
import com.example.csv.constants.Services;
import com.example.csv.services.Product;
import com.example.csv.services.File;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.openapi.router.RequestExtractor;
import io.vertx.ext.web.openapi.router.RouterBuilder;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import io.vertx.openapi.contract.OpenAPIContract;

public class MainVerticle extends AbstractVerticle {
  private static final Integer PORT = 8888;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    String path = "src/main/api/openapi.json";
    Conf.createMongoClient(vertx);
    OpenAPIContract.from(vertx, path)
      .onSuccess(contract -> {
        RouterBuilder routerBuilder = RouterBuilder.create(vertx, contract, RequestExtractor.withBodyHandler());

        SessionStore sessionStore = LocalSessionStore.create(vertx);

        SessionHandler sessionHandler = SessionHandler.create(sessionStore);

        routerBuilder.rootHandler(sessionHandler);

        routerBuilder.rootHandler(
        CorsHandler.create()
          .addOrigin("*")
          .allowedMethod(HttpMethod.PUT)
          .allowedMethod(HttpMethod.DELETE)
          .allowedMethod(HttpMethod.GET)
          .allowedMethod(HttpMethod.POST)
          .allowedMethod(HttpMethod.OPTIONS)
          .allowedHeader("Authorization")
          .allowedHeader("Content-Type")
          .allowCredentials(true)
      );

      routerBuilder.rootHandler(BodyHandler.create().setUploadsDirectory("uploads").setBodyLimit(50 * 1024 * 1024));


      // Product operations
      routerBuilder.getRoute("listProducts").addHandler(ctx -> handleRequest("listProducts", ctx));
      routerBuilder.getRoute("insertProduct").addHandler(ctx -> handleRequest("insertProduct", ctx));
      routerBuilder.getRoute("removeProduct").addHandler(ctx -> handleRequest("removeProduct", ctx));
      routerBuilder.getRoute("removeAllProducts").addHandler(ctx -> handleRequest("removeAllProducts", ctx));
      routerBuilder.getRoute("getProductStatistics").addHandler(ctx -> handleRequest("getProductStatistics", ctx));

      // File operations
      routerBuilder.getRoute("uploadFile").addHandler(ctx -> handleRequest("uploadFile", ctx));
      routerBuilder.getRoute("listFiles").addHandler(ctx -> handleRequest("listFiles", ctx));


      Router router = routerBuilder.createRouter();

      router.route("/uploads/*").handler(StaticHandler.create("uploads"));

      vertx.createHttpServer().requestHandler(router).listen(PORT)
      .onComplete(http -> {
        if (http.succeeded()) {
          startPromise.complete();
          System.out.println("HTTP server started on port " + PORT);
        } else {
          startPromise.fail(http.cause());
        }
      });
    })
    .onFailure(err -> {
      startPromise.fail(err);
      System.err.println("Failed to load OpenAPI contract: " + err.getMessage());

});
}

private void handleRequest(String operationId, RoutingContext ctx) {
  try {
      switch (operationId) {
          case "listProducts":
              JsonObject query = ctx.getBodyAsJson().getJsonObject("query", new JsonObject());
              JsonObject options = ctx.getBodyAsJson().getJsonObject("options", new JsonObject());
              JsonObject message = new JsonObject()
                  .put("query", query)
                  .put("options", options);
              sendEventBusRequest(Services.PRODUCT_LIST, message, 200, ctx);
              break;

          case "insertProduct":
              JsonObject productBody = ctx.getBodyAsJson();
              sendEventBusRequest(Services.PRODUCT_INSERT, productBody, 201, ctx);
              break;

          case "removeProduct":
              JsonObject removeBody = ctx.getBodyAsJson();
              sendEventBusRequest(Services.PRODUCT_REMOVE, removeBody, 200, ctx);
              break;

          case "removeAllProducts":
              sendEventBusRequest(Services.PRODUCT_REMOVE_ALL, new JsonObject(), 200, ctx);
              break;

          case "getProductStatistics":
              sendEventBusRequest(Services.PRODUCT_STATISTICS, new JsonObject(), 200, ctx);
              break;

          case "uploadFile":
              if (!ctx.fileUploads().iterator().hasNext()) {
                  throw new IllegalArgumentException("No file uploaded");
              }
              String fileName = ctx.fileUploads().iterator().next().fileName();
              String uploadedFileName = ctx.fileUploads().iterator().next().uploadedFileName();
              long fileSize = ctx.fileUploads().iterator().next().size();

              JsonObject fileInfo = new JsonObject()
                  .put(Fields.FILE_NAME, fileName)
                  .put(Fields.FILE_PATH, uploadedFileName)
                  .put(Fields.FILE_SIZE, fileSize);
              sendEventBusRequest(Services.FILE_UPLOAD, fileInfo, 200, ctx);
              break;

          case "listFiles":
              sendEventBusRequest(Services.FILE_LIST, new JsonObject(), 200, ctx);
              break;

          default:
              ctx.response()
                  .setStatusCode(404)
                  .putHeader("content-type", "application/json")
                  .end(new JsonObject().put("error", "Operation not found").encode());
              break;
      }
  } catch (Exception e) {
      ctx.response()
          .setStatusCode(500)
          .putHeader("content-type", "application/json")
          .end("Internal server error: " + e.getMessage());
  }
}

private void sendEventBusRequest(String address, JsonObject message, int successStatusCode, RoutingContext ctx) {
  vertx.eventBus().request(address, message, res -> {
      if (res.succeeded()) {
          ctx.response()
              .setStatusCode(successStatusCode)
              .putHeader("content-type", "application/json")
              .end(res.result().body().toString());
      } else {
          ctx.response()
              .setStatusCode(500)
              .putHeader("content-type", "application/json")
              .end(res.cause().getMessage());
      }
  });
}

public static void main(String[] args) {
  Vertx vertx = Vertx.vertx();
  vertx.deployVerticle(new MainVerticle());
  vertx.deployVerticle(new Db());
  vertx.deployVerticle(new Product());
  vertx.deployVerticle(new File());

}




}
