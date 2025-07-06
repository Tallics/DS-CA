package ds.service1;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

// Service1 (Whiteboard Service) class extends the generated Service1ImplBase
public class Service1 extends Service1Grpc.Service1ImplBase {

    // Simple in-memory storage for whiteboard sessions and notes
    // In a real application, this would be a database
    private static final Map<String, List<RetrievedNote>> whiteboardNotes = new HashMap<>();
    private static final Map<String, String> sessionUrls = new HashMap<>(); // Store dummy URLs

    // Main method to start the server
    public static void main(String[] args) throws IOException, InterruptedException {
        int port = 50051; // Port for Whiteboard Service

        // Create a new Service1 instance
        Service1 service1 = new Service1();

        // Build the gRPC server
        Server server = ServerBuilder.forPort(port)
                .addService(service1) // Add the service implementation
                .build();

        // Start the server
        server.start();
        System.out.println("Whiteboard Service started, listening on " + port);

        // Add a shutdown hook for graceful termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            try {
                server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                System.err.println("Server shutdown interrupted: " + e.getMessage());
            }
            System.err.println("*** server shut down");
        }));

        // Await termination to keep the server running
        server.awaitTermination();
    }

    // gRPC method implementation for startWhiteboardSession
    @Override
    public void startWhiteboardSession(StartSessionRequest request, StreamObserver<StartSessionResponse> responseObserver) {
        String sessionId = request.getSessionId();
        String creatorId = request.getCreatorId();
        String topic = request.getTopic();

        System.out.println("Received startWhiteboardSession request for Session ID: " + sessionId + ", Creator: " + creatorId + ", Topic: " + topic);

        StartSessionResponse.Builder responseBuilder = StartSessionResponse.newBuilder();

        if (whiteboardNotes.containsKey(sessionId)) {
            responseBuilder.setSuccess(false)
                    .setMessage("Session ID " + sessionId + " already exists.")
                    .setSessionUrl(sessionUrls.get(sessionId)); // Return existing URL if any
        } else {
            whiteboardNotes.put(sessionId, new ArrayList<>()); // Initialize empty list for notes
            String newSessionUrl = "https://virtualwhiteboard.com/session/" + sessionId; // Dummy URL
            sessionUrls.put(sessionId, newSessionUrl);

            responseBuilder.setSuccess(true)
                    .setMessage("Whiteboard session " + sessionId + " started successfully.")
                    .setSessionUrl(newSessionUrl);
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    // gRPC method implementation for publishWhiteboardNote
    @Override
    public void publishWhiteboardNote(PublishNoteRequest request, StreamObserver<PublishNoteResponse> responseObserver) {
        String sessionId = request.getSessionId();
        String authorId = request.getAuthorId();
        String noteContent = request.getNoteContent();
        long timestamp = request.getTimestamp();

        System.out.println("Received publishWhiteboardNote request for Session ID: " + sessionId + ", Author: " + authorId + ", Content: " + noteContent.substring(0, Math.min(noteContent.length(), 20)) + "...");

        PublishNoteResponse.Builder responseBuilder = PublishNoteResponse.newBuilder();

        if (whiteboardNotes.containsKey(sessionId)) {
            RetrievedNote note = RetrievedNote.newBuilder()
                    .setAuthorId(authorId)
                    .setNoteContent(noteContent)
                    .setTimestamp(timestamp)
                    .build();
            whiteboardNotes.get(sessionId).add(note); // Add note to the session's list
            responseBuilder.setSuccess(true)
                    .setMessage("Note published successfully to session " + sessionId);
        } else {
            responseBuilder.setSuccess(false)
                    .setMessage("Session ID " + sessionId + " does not exist. Please start a session first.");
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    // gRPC method implementation for retrieveSessionNotes
    @Override
    public void retrieveSessionNotes(RetrieveSessionNotesRequest request, StreamObserver<RetrieveSessionNotesResponse> responseObserver) {
        String sessionId = request.getSessionId();
        System.out.println("Received retrieveSessionNotes request for Session ID: " + sessionId);

        RetrieveSessionNotesResponse.Builder responseBuilder = RetrieveSessionNotesResponse.newBuilder()
                .setSessionId(sessionId);

        if (whiteboardNotes.containsKey(sessionId)) {
            responseBuilder.addAllNotes(whiteboardNotes.get(sessionId))
                    .setSuccess(true)
                    .setMessage("Notes retrieved successfully for session " + sessionId);
        } else {
            responseBuilder.setSuccess(false)
                    .setMessage("Session ID " + sessionId + " not found.");
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }
}