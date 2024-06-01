package edu.virginia.sde.reviews;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.function.UnaryOperator;

import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Text;

import static java.lang.Integer.parseInt;

public class CourseReviewsController {
    @FXML
    private Label courseLabel;
    @FXML
    private Label courseLabelLine2;
    @FXML
    private Label averageRatingLabel;
    @FXML
    private Label errorUpdatingReview;
    @FXML
    private Label errorAddingReview;
    private double averageRating;
    @FXML
    private TableView<Review> reviewTableView;
    @FXML
    private TableColumn<Review,Integer> ratingColumn;
    @FXML
    private TableColumn<Review,String> commentColumn;
    @FXML
    private TableColumn<Review, Timestamp> timestampColumn;
    private User currentUser;
    private Review currentUserReview;
    private List<Review> courseReviews;
    private DatabaseDriver driver = DatabaseSingleton.getInstance();;
    private Stage stage;
    private Course currentCourse;
    private int newRating;
    private String newComment;
    @FXML
    private TextField newReviewRating;
    @FXML
    private TextField newReviewComment;
    @FXML
    private TextField editReviewRating;
    @FXML
    private TextField editReviewComment;
    private int actionColumns;
    public void submitReview() {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        Review newReview = new Review(currentCourse, newRating, timestamp, newComment, currentUser.getUsername());
        ratingColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getRating()).asObject());
        commentColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getComment()));
        try {
            driver.addReview(newReview);
            driver.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public void editReview() {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        Review newReview = new Review(currentCourse, newRating, timestamp, newComment, currentUser.getUsername());
        ratingColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getRating()).asObject());
        commentColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getComment()));
        try {
            driver.editReview(newReview);
            driver.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public void initializer() throws RuntimeException {
        currentUser = UserSingleton.getCurrentUser();
        ratingColumn.setCellValueFactory(new PropertyValueFactory<>("rating"));
        commentColumn.setCellValueFactory(new PropertyValueFactory<>("comment"));
        timestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        commentColumn.setCellFactory(param -> {
            TableCell<Review, String> cell = new TableCell<>() {
                private Text text;

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item == null || empty) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        if (text == null) {
                            text = new Text();

                            text.setWrappingWidth(commentColumn.getWidth()-5);
                        }
                        text.setText(item);
                        setGraphic(text);
                    }
                }
            };

            return cell;
        });

        courseLabel.setText(currentCourse.getCourseSubject().toUpperCase() + " " + String.valueOf(currentCourse.getCourseNumber()) + " " + currentCourse.formatTitle(currentCourse.getCourseTitle()) + " Course Reviews");

        try {
            courseReviews = driver.getReviewsFromCourse(currentCourse);
            ObservableList<Review> reviewList = FXCollections.observableArrayList(courseReviews);
            reviewTableView.setItems(reviewList);
        } catch (SQLException e) {
            throw new RuntimeException("Runtime Exception");
        }
        averageRating = 0;
        if (courseReviews.isEmpty()) {
            averageRatingLabel.setText("N/A");
        }
        else {
            for (Review review : courseReviews) {
                averageRating += review.getRating();
            }
            averageRating = averageRating/courseReviews.size();
            averageRatingLabel.setText(String.format("%.2f",averageRating));
        }

        TableColumn<Review, Void> actionColumn = new TableColumn<>("Actions");
        actionColumn.setPrefWidth(118);
        actionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");

            {
                editButton.setOnAction(event -> {
                    // Add your edit logic here, e.g., open a dialog for editing
                    Dialog<String> dialog = new Dialog<>();
                    dialog.setTitle("Edit Review");
                    dialog.getDialogPane().getStylesheets().add(getClass().getResource("/Styles/CourseSearchController.css").toExternalForm());

                    editReviewRating.setTextFormatter(createTextFormat("[1-5]{0,1}"));
                    dialog.getDialogPane().setMinSize(400, 400);
                    errorUpdatingReview.getStyleClass().add("error-label");

                    VBox dialogContent = new VBox(10);

                    dialogContent.getChildren().addAll(
                            new Label("Rating (1-5): "),
                            editReviewRating,
                            new Label("Comment: "),
                            editReviewComment,
                            errorUpdatingReview
                    );

                    editReviewRating.setText(String.valueOf(currentUserReview.getRating()));
                    editReviewComment.setText(currentUserReview.getComment());

                    dialog.getDialogPane().setContent(dialogContent);
                    Platform.runLater(() -> editReviewRating.requestFocus());
                    ButtonType addButton = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
                    ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                    dialog.getDialogPane().getButtonTypes().addAll(addButton, cancelButton);

                    dialog.setResultConverter(dialogButton -> {
                        if (dialogButton == addButton) {
                            newRating = parseInt(editReviewRating.getText());
                            newComment = editReviewComment.getText();
                            editReview();
                            reviewTableView.getItems().clear();
                            initializer();
                            dialog.close();
                            editReviewRating.clear();
                            editReviewComment.clear();
                        }
                        return null;
                    });
                    Node addButtonNode = dialog.getDialogPane().lookupButton(addButton);
                    addButtonNode.addEventFilter(ActionEvent.ACTION, event2 -> {
                        // Check whether some conditions are fulfilled
                        String error = checkRatingField(editReviewRating);
                        if(error != null){
                            errorUpdatingReview.setText(error);
                            event2.consume();
                        }
                    });
                    dialog.showAndWait();
                });
                deleteButton.setOnAction(event -> {
                    Review review = getTableView().getItems().get(getIndex());
                    // Add your delete logic here, e.g., show a confirmation dialog
                    try {
                        driver.deleteReview(review);
                        driver.commit();
                    }
                    catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    reviewTableView.getItems().clear();
                    initializer();
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                Review review = (Review) getTableRow().getItem();
                if (review.getUser().equals(currentUser.getUsername())) {
                    setGraphic(new HBox(editButton, deleteButton));
                    currentUserReview = review;
                } else {
                    setGraphic(null);
                }
            }
        });
        actionColumns += 1;
        if (actionColumns<=1) {
            reviewTableView.getColumns().add(actionColumn);
        }
    }
    public void setStage(Stage stage){
        this.stage = stage;
    }
    public void setCurrentCourse(Course currentCourse) {
        this.currentCourse = currentCourse;
    }
    @FXML
    public void addReview() throws SQLException {
        boolean reviewExists = false;
        for (Review review : courseReviews) {
            if (review.getUser().equals(currentUser.getUsername())) {
                reviewExists = true;
            }
        }
        if (reviewExists) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Review already exists");
            alert.setHeaderText("Error");
            alert.setContentText("A review for this course by this user already exists");
            alert.showAndWait();
        }
        else {
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Add New Review");

            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/Styles/CourseSearchController.css").toExternalForm());
            dialog.getDialogPane().setMinSize(400, 400);
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/Styles/CourseSearchController.css").toExternalForm());

            errorAddingReview.getStyleClass().add("error-label");
            newReviewRating.setTextFormatter(createTextFormat("[1-5]{0,1}"));
            VBox dialogContent = new VBox(10);
            dialogContent.getChildren().addAll(
                    new Label("Rating (1-5): "),
                    newReviewRating,
                    new Label("Comment: "),
                    newReviewComment,
                    errorAddingReview
            );
            dialog.getDialogPane().setContent(dialogContent);
            Platform.runLater(() -> newReviewRating.requestFocus());
            ButtonType addButton = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(addButton, cancelButton);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == addButton) {
                    newRating = parseInt(newReviewRating.getText());
                    newComment = newReviewComment.getText();
                    submitReview();
                    reviewTableView.getItems().clear();
                    initializer();
                    dialog.close();
                    newReviewRating.clear();
                    newReviewComment.clear();
                    }
                return null;
            });
            Node addButtonNode = dialog.getDialogPane().lookupButton(addButton);
            addButtonNode.addEventFilter(ActionEvent.ACTION, event -> {
                // Check whether some conditions are fulfilled
                String error = checkRatingField(newReviewRating);
                if(error != null){
                    errorAddingReview.setText(error);
                    event.consume();
                }
            });
            dialog.showAndWait();
        }
    }
    @FXML
    public TextFormatter<String> createTextFormat (String pattern){
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if(newText.matches(pattern)){
                return change;
            }else{
                return null;
            }
        };
        return new TextFormatter<>(filter);
    }
    @FXML
    public String checkRatingField(TextField rating) {
        // All fields must not be empty
        if (rating.getText().isEmpty()) {
            return "Rating field cannot be empty";
        }
        else {
            if (parseInt(rating.getText()) < 1 || parseInt(rating.getText()) > 5) {
                return "Rating must be an integer from 1-5";
            }
        }
        return null;
    }
    @FXML
    public void logOut() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("login.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Login");

        LoginSceneController loginSceneController = fxmlLoader.getController();
        loginSceneController.setStage(stage);
    }
    @FXML
    public void backToCourseSearch() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("course-search-screen.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Course Search");

        CourseSearchController courseSearchController = fxmlLoader.getController();
        courseSearchController.setStage(stage);
    }
}
