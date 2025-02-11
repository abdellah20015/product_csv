package com.example.csv.configuration;

import java.util.stream.Collectors;

import com.example.csv.constants.Services;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.AggregateOptions;
import io.vertx.ext.mongo.MongoClient;

public class Db extends AbstractVerticle {

  private MongoClient mongoClient;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    mongoClient = Conf.createMongoClient(vertx);

    vertx.eventBus().consumer(Services.DB_INSERT, this::insertDocument);
    vertx.eventBus().consumer(Services.DB_REMOVE_DOCUMENT, this::deleteDocument);
    vertx.eventBus().consumer(Services.DB_REMOVE_DOCUMENTS, this::removeDocuments);
    vertx.eventBus().consumer(Services.DB_AGGREGATE, this::aggregate);

    startPromise.complete();
  }

  private void insertDocument(Message<JsonObject> message) {
      JsonObject payload = message.body();
      String collection = payload.getString("collection");
      JsonObject document = payload.getJsonObject("query");

      mongoClient.insert(collection, document, res -> {
          if (res.succeeded()) {
              JsonObject query = new JsonObject().put("_id", res.result());
              mongoClient.findOne(collection, query, null, findRes -> {
                  JsonObject response = new JsonObject();

                  if (findRes.succeeded() && findRes.result() != null) {
                      response.put("status", "success")
                             .put("message", "Document inséré avec succès")
                             .put("data", findRes.result());
                  } else {
                      response.put("status", "error")
                             .put("message", "Document inséré mais impossible de le récupérer")
                             .put("code", -1);
                  }

                  message.reply(response);
              });
          } else {
              JsonObject response = new JsonObject()
                  .put("status", "error")
                  .put("message", "Échec de l'insertion: " + res.cause().getMessage())
                  .put("code", -1);
              message.reply(response);
          }
      });
  }

  private void deleteDocument(Message<JsonObject> message) {
    JsonObject payload = message.body();
    String collection = payload.getString("collection");
    String id = payload.getString("id");

    JsonObject query = new JsonObject().put("_id", id);


    mongoClient.findOne(collection, query, null, findRes -> {
        if (findRes.succeeded() && findRes.result() != null) {
            JsonObject documentToDelete = findRes.result();


            mongoClient.removeDocument(collection, query, deleteRes -> {
                JsonObject response = new JsonObject();

                if (deleteRes.succeeded() && deleteRes.result().getRemovedCount() > 0) {
                    response.put("status", "success")
                           .put("message", "Document supprimé avec succès")
                           .put("data", documentToDelete);
                } else {
                    response.put("status", "error")
                           .put("message", "Échec de la suppression")
                           .put("code", -1);
                }

                message.reply(response);
            });
        } else {
            JsonObject response = new JsonObject()
                .put("status", "error")
                .put("message", "Document non trouvé")
                .put("code", 404);
            message.reply(response);
        }
    });
  }

  private void aggregate(Message<JsonObject> message) {
    JsonObject payload = message.body();
    String collection = payload.getString("collection");
    JsonArray pipeline = payload.getJsonArray("pipeline");
    JsonObject options = payload.getJsonObject("options", new JsonObject());

    // Handle pagination
    int page = options.getInteger("page", 1);
    int limit = options.getInteger("limit", 10);
    int skip = (page - 1) * limit;

    // Handle sorting
    JsonObject sort = options.getJsonObject("sort", new JsonObject());
    boolean reverse = options.getBoolean("reverse", false);
    if (!sort.isEmpty()) {
        if (reverse) {
            sort.forEach(entry -> sort.put(entry.getKey(), entry.getValue().equals(1) ? -1 : 1));
        }
        pipeline.add(new JsonObject().put("$sort", sort));
    }

    // Add pagination stages
    pipeline.add(new JsonObject().put("$skip", skip));
    pipeline.add(new JsonObject().put("$limit", limit));

    JsonArray countPipeline = new JsonArray();
    for (Object stage : pipeline) {
        JsonObject stageObj = (JsonObject) stage;
        if (!stageObj.containsKey("$skip") && !stageObj.containsKey("$limit")) {
            countPipeline.add(stage);
        }
    }
    countPipeline.add(new JsonObject().put("$count", "total"));

    // Rest of your existing aggregate method...
    mongoClient
        .aggregateWithOptions(collection, countPipeline, new AggregateOptions(options))
        .collect(Collectors.toList())
        .onComplete(countRes -> {
            if (countRes.succeeded()) {
                mongoClient
                    .aggregateWithOptions(collection, pipeline, new AggregateOptions(options))
                    .collect(Collectors.toList())
                    .onComplete(dataRes -> {
                        JsonObject response = new JsonObject();

                        if (dataRes.succeeded()) {
                            long total = countRes.result().isEmpty() ? 0 : countRes.result().get(0).getLong("total");
                            int totalPages = (int) Math.ceil((double) total / limit);

                            response.put("status", "success")
                                   .put("pagination", new JsonObject()
                                       .put("total", total)
                                       .put("page", page)
                                       .put("limit", limit)
                                       .put("totalPages", totalPages))
                                   .put("data", new JsonArray(dataRes.result()));
                        } else {
                            response.put("status", "error")
                                   .put("message", dataRes.cause().getMessage())
                                   .put("code", 500);
                        }

                        message.reply(response);
                    });
            } else {
                JsonObject response = new JsonObject()
                    .put("status", "error")
                    .put("message", countRes.cause().getMessage())
                    .put("code", 500);

                message.reply(response);
            }
        });
}

  private void removeDocuments(Message<JsonObject> message) {
    JsonObject payload = message.body();
    String collection = payload.getString("collection");
    JsonObject query = payload.getJsonObject("query");

    mongoClient.find(collection, query, findRes -> {
        if (findRes.succeeded() && !findRes.result().isEmpty()) {
            JsonArray documentsToDelete = new JsonArray(findRes.result());

            mongoClient.removeDocuments(collection, query, deleteRes -> {
                JsonObject response = new JsonObject();

                if (deleteRes.succeeded()) {
                    response.put("status", "success")
                           .put("message", "Documents supprimés avec succès")
                           .put("data", documentsToDelete);
                } else {
                    response.put("status", "error")
                           .put("message", "Échec de la suppression: " + deleteRes.cause().getMessage())
                           .put("code", -1);
                }

                message.reply(response);
            });
        } else {
            JsonObject response = new JsonObject()
                .put("status", "error")
                .put("message", "Aucun document trouvé")
                .put("code", 404);
            message.reply(response);
        }
    });
  }





}
