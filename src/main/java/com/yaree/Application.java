package com.yaree;

import com.yaree.service.PositionService;
import com.yaree.service.QualificationService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class Application {

  public static void main(String[] args) throws IOException, InterruptedException {
    var port = 50051;

    Server server = ServerBuilder.forPort(port)
        .addService(new PositionService())
        .addService(new QualificationService())
        .build();

    System.out.println("Starting server on port " + port);
    server.start();

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("Shutting down server...");
      server.shutdown();
    }));

    server.awaitTermination();
  }

}
