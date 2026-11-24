package view;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.function.Function;

final class DashboardStyle {

    private DashboardStyle() {
    }

    static HBox header(String workspace, String user, Runnable logout) {
        VBox brand = new VBox(3,
                label("Banking Management System", "brand-title"),
                label(workspace, "header-caption"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(14, brand, spacer,
                label(user, "header-caption"), button("Logout", "outline", logout));
        header.setAlignment(Pos.CENTER_LEFT);
        styleHeader(header);
        return header;
    }

    static void styleHeader(HBox header) {
        header.setPadding(new Insets(16, 24, 16, 24));
        header.setStyle("-fx-background-color: #102A3B;");
    }

    static Label label(String value, String style) {
        Label label = new Label(value);
        label.getStyleClass().add(style);

        label.setStyle(switch (style) {
            case "brand-title" ->
                    "-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;";
            case "header-caption" -> "-fx-text-fill: #C5DCE6;";
            case "login-title", "dashboard-title" ->
                    "-fx-font-size: 23px; -fx-font-weight: bold; -fx-text-fill: #102A3B;";
            case "section-title" ->
                    "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #18394A;";
            case "field-label" -> "-fx-font-weight: bold; -fx-text-fill: #18394A;";
            case "balance-value" ->
                    "-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #102A3B;";
            case "muted" -> "-fx-text-fill: #506B7D;";
            case "error-text" -> "-fx-text-fill: #A02632;";
            case "success-text" -> "-fx-text-fill: #07634D;";
            default -> "";
        });

        return label;
    }

    static VBox card(String title, Node... nodes) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.setStyle("""
                -fx-background-color: white;
                -fx-background-radius: 12;
                -fx-border-color: #D8E4EA;
                -fx-border-radius: 12;
                """);

        if (!title.isBlank()) {
            card.getChildren().add(label(title, "section-title"));
        }

        card.getChildren().addAll(nodes);
        return card;
    }

    static Button button(String text, String style, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add(style);

        String background = switch (style) {
            case "primary" -> "#0F6974";
            case "outline" -> "#254759";
            default -> "#E7EFF4";
        };

        String foreground = switch (style) {
            case "primary", "outline" -> "white";
            default -> "#153849";
        };

        button.setStyle(
                "-fx-background-color: " + background + ";"
                        + "-fx-text-fill: " + foreground + ";"
                        + "-fx-background-radius: 7;"
                        + "-fx-font-weight: bold;"
                        + "-fx-padding: 8 14;"
                        + "-fx-cursor: hand;"
        );

        button.setOnAction(event -> action.run());
        return button;
    }

    static void styleInput(TextField input) {
        input.setPrefHeight(38);
        input.setStyle("""
                -fx-background-color: #F5F8FA;
                -fx-background-radius: 7;
                -fx-border-color: #C9D7E1;
                -fx-border-radius: 7;
                -fx-padding: 7 10;
                """);
    }

    static String bwp(BigDecimal amount) {
        return "BWP " + String.format(Locale.US, "%,.2f", amount);
    }

    static <T> TableColumn<T, String> column(String name, Function<T, String> display) {
        TableColumn<T, String> column = new TableColumn<>(name);
        column.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(display.apply(cell.getValue())));
        column.setReorderable(false);
        return column;
    }

    static <T> TableView<T> table(String empty) {
        TableView<T> table = new TableView<>();
        table.setPlaceholder(new Label(empty));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(32);
        table.setStyle("-fx-border-color: #D8E4EA; -fx-background-color: white;");
        return table;
    }

    static ScrollPane scroll(Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #EDF3F7; -fx-background-color: #EDF3F7;");
        return scroll;
    }
}