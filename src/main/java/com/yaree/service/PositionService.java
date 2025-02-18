package com.yaree.service;

import com.google.protobuf.Empty;
import com.yaree.proto.Position;
import com.yaree.proto.PositionServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class PositionService extends PositionServiceGrpc.PositionServiceImplBase {

  private final List<Position.PositionMessage> repository = new ArrayList<>();

  @Override
  public void createPosition(Position.CreatePositionMessage request, StreamObserver<Position.PositionMessage> responseObserver) {
    var response = Position.PositionMessage.newBuilder()
        // TODO: uuid should be provided by db.
        .setId(UUID.randomUUID().toString())
        .setTitle(request.getTitle())
        .setHourRate(request.getHourRate())
        .build();
    this.repository.add(response);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void getPosition(Position.GetPositionMessage request, StreamObserver<Position.PositionMessage> responseObserver) {
    if (request.getId().isEmpty()) {
      responseObserver.onError(new StatusRuntimeException(Status.INVALID_ARGUMENT));
      return;
    }
    this.repository.stream()
        .filter(getPositionMessage -> getPositionMessage.getId().equals(request.getId()))
        .findFirst()
        .ifPresentOrElse(responseObserver::onNext, () -> responseObserver
            .onError(Status.NOT_FOUND
                .withDescription("Position with ID: " + request.getId() + "was not found")
                .asRuntimeException()));
    responseObserver.onCompleted();
  }

  @Override
  public void getPositions(Position.GetPositionsMessage request, StreamObserver<Position.PositionsMessage> responseObserver) {
    var response = this.repository.stream()
        .filter(getPositionsMessage -> {
              var result = true;
              if (request.hasTitle()) result = request.getTitle().equals(getPositionsMessage.getTitle());
              if (request.hasMinRate()) result = request.getMinRate() < getPositionsMessage.getHourRate();
              if (request.hasMaxRate()) result = request.getMaxRate() > getPositionsMessage.getHourRate();
              return result;
            }
        ).collect(Collectors.toList());
    Position.PositionsMessage positionsMessage = Position.PositionsMessage.newBuilder().addAllPositions(response).build();
    responseObserver.onNext(positionsMessage);
    responseObserver.onCompleted();
  }

  @Override
  public void updatePosition(Position.UpdatePositionMessage request, StreamObserver<Position.PositionMessage> responseObserver) {
    var builder = Position.PositionMessage.newBuilder()
        .setId(request.getId());
    if (request.hasTitle()) builder.setTitle(request.getTitle());
    if (request.hasHourRate()) builder.setHourRate(request.getHourRate());
    var response = builder.build();
    this.repository.stream()
        .map(position -> {
          if (position.getId().equals(request.getId())) {
            return response;
          }
          return position;
        }).filter(position -> position.getId().equals(request.getId()))
        .findFirst()
        .ifPresentOrElse(responseObserver::onNext, () -> responseObserver
            .onError(Status.NOT_FOUND
                .withDescription("Position with ID: " + request.getId() + "was not found")
                .asRuntimeException()));
    responseObserver.onCompleted();
  }

  @Override
  public void deletePosition(Position.GetPositionMessage request, StreamObserver<Empty> responseObserver) {
    Optional<Position.PositionMessage> positionToDelete = this.repository.stream()
        .filter(position -> position.getId().equals(request.getId()))
        .findFirst();
    positionToDelete.ifPresentOrElse(position -> {
      this.repository.remove(position);
      responseObserver.onNext(Empty.getDefaultInstance());
      responseObserver.onCompleted();
    }, () -> responseObserver
        .onError(Status.NOT_FOUND
            .withDescription("Position with ID: " + request.getId() + "not found")
            .asRuntimeException()));
  }
}
