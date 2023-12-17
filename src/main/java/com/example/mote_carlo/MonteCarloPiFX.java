package com.example.mote_carlo;

import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class MonteCarloPiFX extends Application {

    private final Canvas drawingCanvas = new Canvas(550, 550);
    private final GraphicsContext gc = drawingCanvas.getGraphicsContext2D();
    private final Label outputLabel = new Label();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Monte Carlo Pi Estimation");

        BorderPane borderPane = new BorderPane();
        borderPane.setPadding(new Insets(10, 20, 10, 20));

        HBox controlBox = new HBox(10);
        controlBox.setPadding(new Insets(10, 0, 10, 0));

        Label pointsLabel = new Label("Enter the number of points:");
        TextField pointsTextField = new TextField();
        Button startButton = new Button("Start");
        Button resetButton = new Button("Reset");

        controlBox.getChildren().addAll(pointsLabel, pointsTextField, startButton, resetButton);

        resetButton.setOnAction(event -> {
            resetCanvasAndFrame();
            outputLabel.setText("");
        });

        borderPane.setTop(controlBox);
        borderPane.setCenter(drawingCanvas);
        borderPane.setBottom(outputLabel);

        drawInitialFrame(); // Draw initial frame

        startButton.setOnAction(event -> {
            int totalPoints = Integer.parseInt(pointsTextField.getText());
            performMonteCarlo(totalPoints);
        });

        Scene scene = new Scene(borderPane, 750, 850);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void drawInitialFrame() {
        double centerX = drawingCanvas.getWidth() / 2.0;
        double centerY = drawingCanvas.getHeight() / 2.0;

        // Draw square outline
        double squareSide = Math.min(drawingCanvas.getWidth(), drawingCanvas.getHeight()) - 5;
        double squareX = (drawingCanvas.getWidth() - squareSide) / 2.0;
        double squareY = (drawingCanvas.getHeight() - squareSide) / 2.0;
        gc.setStroke(Color.rgb(35, 75, 25));
        gc.setLineWidth(6);
        gc.strokeRect(squareX, squareY, squareSide, squareSide);

        // Draw circle outline with adjusted radius
        double halfSquareSide = squareSide / 2.0;
        double radius = halfSquareSide - 1.5; // Adjusted radius
        gc.setStroke(Color.rgb(0, 0, 0));
        gc.setLineWidth(3);
        gc.strokeOval(centerX - radius, centerY - radius, 2 * radius, 2 * radius);
    }

    private void resetCanvasAndFrame() {
        gc.clearRect(0, 0, drawingCanvas.getWidth(), drawingCanvas.getHeight());
        drawInitialFrame(); // Redraw the initial frame
    }

    private void drawMonteCarlo(double x, double y, boolean insideCircleOutline) {
        Platform.runLater(() -> {
            double pointSize = 4;
            double radius = ((Math.min(drawingCanvas.getWidth(), drawingCanvas.getHeight()) - 5) / 2.0) - 1.5; // Adjusted radius
            boolean insideCircle = isInsideCircle(x, y, drawingCanvas.getWidth() / 2.0, drawingCanvas.getHeight() / 2.0, radius);

            if (insideCircle) {
                // Draw blue circle
                gc.setFill(Color.rgb(235,180,99));
                gc.fillOval(x * drawingCanvas.getWidth() - pointSize / 2, y * drawingCanvas.getHeight() - pointSize / 2, pointSize, pointSize);
            } else {
                // Draw red square
                gc.setFill(Color.rgb(121,112,98));
                gc.fillRect(x * drawingCanvas.getWidth() - pointSize / 2, y * drawingCanvas.getHeight() - pointSize / 2, pointSize, pointSize);
            }
        });
    }

    private boolean isInsideCircle(double x, double y, double centerX, double centerY, double radius) {
        double distance = Math.sqrt(Math.pow(x * drawingCanvas.getWidth() - centerX, 2) +
                Math.pow(y * drawingCanvas.getHeight() - centerY, 2));
        return distance <= radius;
    }

    private void performMonteCarlo(int totalPoints) {
        int numTasks = 5;
        int pointsPerTask = totalPoints / numTasks;

        ExecutorService executorService = Executors.newFixedThreadPool(numTasks);
        List<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < numTasks; i++) {
            Callable<Integer> task = new MonteCarloTask(pointsPerTask);
            Future<Integer> future = executorService.submit(task);
            futures.add(future);
        }

        int totalPointsInsideCircle = 0;
        int simulatedPoints = 0;

        try {
            for (Future<Integer> future : futures) {
                int pointsInsideCircle = future.get();
                totalPointsInsideCircle += pointsInsideCircle;
                simulatedPoints += pointsPerTask;

                double pi = 4.0 * ((double) totalPointsInsideCircle / simulatedPoints);
                String labelText = String.format("Estimation of Pi: %.4f (%d points simulated)", pi, simulatedPoints);

                // Increase the font size (adjust the size as needed)
                Font largeFont = Font.font(outputLabel.getFont().getFamily(), FontWeight.BOLD, 24);
                outputLabel.setFont(largeFont);
                outputLabel.setText(labelText);

                // Update progress bar if implemented
                // ...

            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    class MonteCarloTask implements Callable<Integer> {
        private final int numPoints;

        public MonteCarloTask(int numPoints) {
            this.numPoints = numPoints;
        }

        @Override
        public Integer call() {
            int pointsInsideCircle = 0;
            for (int i = 0; i < numPoints; i++) {
                double x = ThreadLocalRandom.current().nextDouble();
                double y = ThreadLocalRandom.current().nextDouble();
                double distance = x * x + y * y;
                if (distance <= 1) {
                    pointsInsideCircle++;
                    drawMonteCarlo(x, y, true);
                } else {
                    drawMonteCarlo(x, y, false);
                }
            }
            return pointsInsideCircle;
        }
    }
}