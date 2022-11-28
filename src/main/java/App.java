import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

public class App extends Application {

    public static void main(String[] args) {
        Locale.setDefault(Locale.ENGLISH);
        org.apache.jena.query.ARQ.init();
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/view/view.fxml"));

        Scene scene = new Scene(fxmlLoader.load(),1600,900);
        scene.getStylesheets().add(Objects.requireNonNull(App.class.getResource("/css/style.css")).toExternalForm());

        stage.setScene(scene);
        stage.setTitle("Multi-level Modeling Playground");
        stage.getIcons().add(new Image(Objects.requireNonNull(App.class.getResourceAsStream("/images/icon.png"))));
        stage.setMaximized(true);

        stage.show();
    }
}
