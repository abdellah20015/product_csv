package com.example.csv.services;




import com.example.csv.constants.Fields;
import com.example.csv.constants.Services;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class File extends AbstractVerticle {
    private static final String COLLECTION = "files";
    private static final String UPLOADS_DIR = "uploads";

    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        if (!Files.exists(Paths.get(UPLOADS_DIR))) {
            Files.createDirectory(Paths.get(UPLOADS_DIR));
        }

        vertx.eventBus().consumer(Services.FILE_UPLOAD, this::handleFileUpload);
        vertx.eventBus().consumer(Services.FILE_LIST, this::handleFileList);

        startPromise.complete();
    }

    private void handleFileUpload(Message<JsonObject> message) {
        JsonObject fileInfo = message.body();
        String filePath = fileInfo.getString(Fields.FILE_PATH);
        String fileName = fileInfo.getString(Fields.FILE_NAME);
        long fileSize = fileInfo.getLong(Fields.FILE_SIZE, 0L);

        vertx.executeBlocking(promise -> {
            try {
                ProcessingResult result = processCsvFile(filePath);

                JsonObject document = new JsonObject()
                    .put(Fields.FILE_NAME, fileName)
                    .put(Fields.FILE_PATH, filePath)
                    .put(Fields.FILE_SIZE, fileSize)
                    .put(Fields.FILE_DATE_CREATION, Instant.now().toString())
                    .put(Fields.FILE_TOTAL_LINES, result.totalLines)
                    .put(Fields.FILE_SUCCESSFUL_LINES, result.successfulLines)
                    .put(Fields.FILE_ERROR_LINES, result.errorLines)
                    .put("headers", result.headers);

                JsonObject dbMessage = new JsonObject()
                    .put("collection", COLLECTION)
                    .put("query", document);

                promise.complete(dbMessage);
            } catch (Exception e) {
                promise.fail(e);
            }
        }, res -> {
            if (res.succeeded()) {
                JsonObject dbMessage = (JsonObject) res.result();
                vertx.eventBus().request(Services.DB_INSERT, dbMessage, dbRes -> {
                    if (dbRes.succeeded()) {
                        message.reply(dbRes.result().body());
                    } else {
                        message.reply(new JsonObject()
                            .put("status", "error")
                            .put("message", "Failed to save file information")
                            .put("code", 500));
                    }
                });
            } else {
                message.reply(new JsonObject()
                    .put("status", "error")
                    .put("message", "Failed to process CSV file: " + res.cause().getMessage())
                    .put("code", 500));
            }
        });
    }

    private static class ProcessingResult {
        int totalLines = 0;
        int successfulLines = 0;
        int errorLines = 0;
        List<String> headers = new ArrayList<>();
    }

    private ProcessingResult processCsvFile(String filePath) throws Exception {
        ProcessingResult result = new ProcessingResult();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String headerLine = br.readLine();
            if (headerLine == null) {
                throw new Exception("CSV file is empty");
            }
            result.totalLines++;


            String[] headers = headerLine.split(",");
            for (String header : headers) {
                result.headers.add(header.trim().toLowerCase());
            }


            String line;
            while ((line = br.readLine()) != null) {
                result.totalLines++;
                try {
                    JsonObject record = parseDataLine(line, result.headers);
                    record.put("created_date", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));


                    insertRecord(record);
                    result.successfulLines++;
                } catch (Exception e) {
                    result.errorLines++;
                    System.err.println("Error processing line " + result.totalLines + ": " + e.getMessage());
                }
            }
        }

        return result;
    }

    private JsonObject parseDataLine(String line, List<String> headers) throws Exception {
        String[] values = line.split(",");
        if (values.length != headers.size()) {
            throw new IllegalArgumentException("Number of values does not match number of headers");
        }

        JsonObject record = new JsonObject();
        for (int i = 0; i < headers.size(); i++) {
            String value = values[i].trim();
            String header = headers.get(i);


            try {

                record.put(header, Integer.parseInt(value));
            } catch (NumberFormatException e1) {
                try {

                    record.put(header, Double.parseDouble(value));
                } catch (NumberFormatException e2) {

                    record.put(header, value);
                }
            }
        }

        return record;
    }

    private void insertRecord(JsonObject record) {
        JsonObject message = new JsonObject()
            .put("collection", "products")
            .put("query", record);

        vertx.eventBus().request(Services.DB_INSERT, message, res -> {
            if (res.failed()) {
                System.err.println("Failed to insert record: " + res.cause().getMessage());
            }
        });
    }

    private void handleFileList(Message<JsonObject> message) {
        JsonArray pipeline = new JsonArray()
            .add(new JsonObject()
                .put("$sort", new JsonObject()
                    .put(Fields.FILE_DATE_CREATION, -1)));

        JsonObject aggregateMessage = new JsonObject()
            .put("collection", COLLECTION)
            .put("pipeline", pipeline)
            .put("options", new JsonObject());

        vertx.eventBus().request(Services.DB_AGGREGATE, aggregateMessage, res -> {
            if (res.succeeded()) {
                message.reply(res.result().body());
            } else {
                message.reply(new JsonObject()
                    .put("status", "error")
                    .put("message", "Failed to retrieve file list")
                    .put("code", 500));
            }
        });
    }

}
