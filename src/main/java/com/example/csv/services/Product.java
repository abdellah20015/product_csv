package com.example.csv.services;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import com.example.csv.constants.Collections;
import com.example.csv.constants.Fields;
import com.example.csv.constants.Services;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class Product extends AbstractVerticle {
  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    vertx.eventBus().consumer(Services.PRODUCT_LIST, this::getListProductHandler);
    vertx.eventBus().consumer(Services.PRODUCT_INSERT, this::createProductHandler);
    vertx.eventBus().consumer(Services.PRODUCT_REMOVE, this::removeProductHandler);
    vertx.eventBus().consumer(Services.PRODUCT_REMOVE_ALL, this::removeAllProductsHandler);
    vertx.eventBus().consumer(Services.PRODUCT_STATISTICS, this::getProductStatisticsHandler);


    startPromise.complete();
  }

  private void getListProductHandler(Message<JsonObject> message) {
    try {
        JsonObject body = (JsonObject) message.body();
        JsonObject query = body.getJsonObject("query", new JsonObject());
        JsonObject options = body.getJsonObject("options", new JsonObject());

        JsonObject filters = options.getJsonObject("filters", new JsonObject());
        if (!filters.isEmpty()) {
            filters.forEach(entry -> {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof String) {
                    query.put(key, new JsonObject()
                        .put("$regex", value.toString())
                        .put("$options", "i"));
                } else {
                    query.put(key, value);
                }
            });
        }

        JsonArray pipeline = new JsonArray()
            .add(new JsonObject().put("$match", query));

        JsonObject msg = new JsonObject()
            .put("collection", Collections.Products)
            .put("pipeline", pipeline)
            .put("options", options);

        vertx.eventBus().request(Services.DB_AGGREGATE, msg, res -> {
            if (res.failed()) {
                message.fail(500, res.cause().getMessage());
            } else {
                message.reply(res.result().body());
            }
        });
    } catch (Exception e) {
        message.fail(500, "Internal server error: " + e.getMessage());
    }
}

  private void createProductHandler(Message<JsonObject> message) {
    try {
      JsonObject body = (JsonObject) message.body();
      String formattedDate = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
      JsonObject query = new JsonObject()
        .put(Fields.PRODUCT_NAME, body.getString("name"))
        .put(Fields.PRODUCT_QUANTITY, body.getInteger("quantity"))
        .put(Fields.PRODUCT_PRICE, body.getNumber("price"))
        .put(Fields.PRODUCT_CATEGORY, body.getString("category"))
        .put(Fields.PRODUCT_CLIENT, body.getString("client"))
        .put(Fields.PRODUCT_REGION, body.getString("region"))
        .put(Fields.PRODUCT_DATE_CREATION, formattedDate);

      JsonObject msg = new JsonObject()
        .put("collection", Collections.Products)
        .put("query", query);

      vertx.eventBus().request(Services.DB_INSERT, msg, res -> {
        if (res.failed()) {
          message.fail(500, res.cause().getMessage());
        } else {
          message.reply(res.result().body());
        }
      });
    } catch (Exception e) {
      message.fail(500, "Internal server error: " + e.getMessage());
    }
  }

  private void removeProductHandler(Message<JsonObject> message) {
    try {
      JsonObject body = (JsonObject) message.body();
      String productId = body.getString("productId");

      JsonObject msg = new JsonObject()
        .put("collection", Collections.Products)
        .put("id", productId);

      vertx.eventBus().request(Services.DB_REMOVE_DOCUMENT, msg, res -> {
        if (res.failed()) {
          message.fail(500, res.cause().getMessage());
        } else {
          message.reply(res.result().body());
        }
      });
    } catch (Exception e) {
      message.fail(500, "Internal server error: " + e.getMessage());
    }
  }

  private void removeAllProductsHandler(Message<JsonObject> message) {
    try {
      JsonObject msg = new JsonObject()
        .put("collection", Collections.Products)
        .put("query", new JsonObject());

      vertx.eventBus().request(Services.DB_REMOVE_DOCUMENTS, msg, res -> {
        if (res.failed()) {
          message.fail(500, res.cause().getMessage());
        } else {
          message.reply(res.result().body());
        }
      });
    } catch (Exception e) {
      message.fail(500, "Internal server error: " + e.getMessage());
    }
  }

  private void getProductStatisticsHandler(Message<JsonObject> message) {
    try {
      // Pipeline pour les statistiques générales
      JsonArray generalStatsPipeline = new JsonArray()
        .add(new JsonObject()
          .put("$facet", new JsonObject()
            // Nombre total de produits
            .put("totalProducts", new JsonArray()
              .add(new JsonObject()
                .put("$count", "total")))

            // Statistiques par région détaillées
            .put("regionStats", new JsonArray()
              .add(new JsonObject()
                .put("$group", new JsonObject()
                  .put("_id", "$" + Fields.PRODUCT_REGION)
                  .put("count", new JsonObject().put("$sum", 1))
                  .put("totalQuantity", new JsonObject().put("$sum", "$" + Fields.PRODUCT_QUANTITY))
                  .put("averagePrice", new JsonObject().put("$avg", "$" + Fields.PRODUCT_PRICE))
                  .put("totalValue", new JsonObject()
                    .put("$sum", new JsonObject()
                      .put("$multiply", new JsonArray()
                        .add("$" + Fields.PRODUCT_QUANTITY)
                        .add("$" + Fields.PRODUCT_PRICE))))))
              .add(new JsonObject()
                .put("$sort", new JsonObject()
                  .put("count", -1)))
              .add(new JsonObject()
                .put("$project", new JsonObject()
                  .put("_id", 1)
                  .put("count", 1)
                  .put("totalQuantity", 1)
                  .put("averagePrice", new JsonObject().put("$round", new JsonArray().add("$averagePrice").add(2)))
                  .put("totalValue", new JsonObject().put("$round", new JsonArray().add("$totalValue").add(2)))
                  .put("percentageOfTotal", new JsonObject()
                    .put("$multiply", new JsonArray()
                      .add(new JsonObject()
                        .put("$divide", new JsonArray()
                          .add("$count")
                          .add(new JsonObject().put("$sum", "$count"))))
                      .add(100))))))

            // Top régions par nombre de produits
            .put("topRegions", new JsonArray()
              .add(new JsonObject()
                .put("$group", new JsonObject()
                  .put("_id", "$" + Fields.PRODUCT_REGION)
                  .put("count", new JsonObject().put("$sum", 1))))
              .add(new JsonObject()
                .put("$sort", new JsonObject()
                  .put("count", -1)))
              .add(new JsonObject()
                .put("$limit", 5)))

            // Statistiques temporelles (par mois/année)
            .put("timeStats", new JsonArray()
              .add(new JsonObject()
                .put("$addFields", new JsonObject()
                  .put("date", new JsonObject()
                    .put("$dateFromString", new JsonObject()
                      .put("dateString", "$" + Fields.PRODUCT_DATE_CREATION)))))
              .add(new JsonObject()
                .put("$group", new JsonObject()
                  .put("_id", new JsonObject()
                    .put("year", new JsonObject().put("$year", "$date"))
                    .put("month", new JsonObject().put("$month", "$date")))
                  .put("count", new JsonObject().put("$sum", 1))
                  .put("totalQuantity", new JsonObject().put("$sum", "$" + Fields.PRODUCT_QUANTITY))
                  .put("totalValue", new JsonObject()
                    .put("$sum", new JsonObject()
                      .put("$multiply", new JsonArray()
                        .add("$" + Fields.PRODUCT_QUANTITY)
                        .add("$" + Fields.PRODUCT_PRICE))))))
              .add(new JsonObject()
                .put("$sort", new JsonObject()
                  .put("_id.year", -1)
                  .put("_id.month", -1))))

            // Statistiques par catégorie
            .put("categoryStats", new JsonArray()
              .add(new JsonObject()
                .put("$group", new JsonObject()
                  .put("_id", "$" + Fields.PRODUCT_CATEGORY)
                  .put("count", new JsonObject().put("$sum", 1))
                  .put("averagePrice", new JsonObject().put("$avg", "$" + Fields.PRODUCT_PRICE))
                  .put("totalValue", new JsonObject()
                    .put("$sum", new JsonObject()
                      .put("$multiply", new JsonArray()
                        .add("$" + Fields.PRODUCT_QUANTITY)
                        .add("$" + Fields.PRODUCT_PRICE)))))))

            // Statistiques par client
            .put("clientStats", new JsonArray()
              .add(new JsonObject()
                .put("$group", new JsonObject()
                  .put("_id", "$" + Fields.PRODUCT_CLIENT)
                  .put("count", new JsonObject().put("$sum", 1))
                  .put("totalQuantity", new JsonObject().put("$sum", "$" + Fields.PRODUCT_QUANTITY))
                  .put("totalValue", new JsonObject()
                    .put("$sum", new JsonObject()
                      .put("$multiply", new JsonArray()
                        .add("$" + Fields.PRODUCT_QUANTITY)
                        .add("$" + Fields.PRODUCT_PRICE))))))
              .add(new JsonObject()
                .put("$sort", new JsonObject()
                  .put("totalValue", -1))))

            // Statistiques globales
            .put("globalStats", new JsonArray()
              .add(new JsonObject()
                .put("$group", new JsonObject()
                  .put("_id", null)
                  .put("totalProducts", new JsonObject().put("$sum", 1))
                  .put("totalQuantity", new JsonObject().put("$sum", "$" + Fields.PRODUCT_QUANTITY))
                  .put("averagePrice", new JsonObject().put("$avg", "$" + Fields.PRODUCT_PRICE))
                  .put("minPrice", new JsonObject().put("$min", "$" + Fields.PRODUCT_PRICE))
                  .put("maxPrice", new JsonObject().put("$max", "$" + Fields.PRODUCT_PRICE))
                  .put("totalValue", new JsonObject()
                    .put("$sum", new JsonObject()
                      .put("$multiply", new JsonArray()
                        .add("$" + Fields.PRODUCT_QUANTITY)
                        .add("$" + Fields.PRODUCT_PRICE))))))
              .add(new JsonObject()
                .put("$project", new JsonObject()
                  .put("_id", 0)
                  .put("totalProducts", 1)
                  .put("totalQuantity", 1)
                  .put("averagePrice", new JsonObject().put("$round", new JsonArray().add("$averagePrice").add(2)))
                  .put("minPrice", 1)
                  .put("maxPrice", 1)
                  .put("totalValue", new JsonObject().put("$round", new JsonArray().add("$totalValue").add(2))))))
          ));

      JsonObject msg = new JsonObject()
        .put("collection", Collections.Products)
        .put("pipeline", generalStatsPipeline)
        .put("options", new JsonObject());

      vertx.eventBus().request(Services.DB_AGGREGATE, msg, res -> {
        if (res.failed()) {
          message.fail(500, res.cause().getMessage());
        } else {
          message.reply(res.result().body());
        }
      });
    } catch (Exception e) {
      message.fail(500, "Internal server error: " + e.getMessage());
    }
  }





}
