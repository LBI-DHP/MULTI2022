package util;

import javafx.scene.shape.SVGPath;

public class SVGUtil {

    private static SVGPath initializeSVGPath(String value) {
        SVGPath svgPath = new SVGPath();
        svgPath.setContent(value);
        return svgPath;
    }

    public static SVGPath FILE_OUTLINE() {
        return initializeSVGPath("""
                M 11.2 1.6 H 4.8 A 1.6 1.6 90 0 0 3.2 3.2 V 16 A 1.6 1.6 90 0 0 4.8 17.6 H 14.4 A 1.6 1.6 90 0 0 16 16 V
                6.4 L 11.2 1.6 M 14.4 16 H 4.8 V 3.2 H 10.4 V 7.2 H 14.4 V 16 Z
                """);
    }

    public static SVGPath FOLDER_OUTLINE() {
        return initializeSVGPath("""
                M 16 14.4 H 3.2 V 6.4 H 16 M 16 4.8 H 9.6 L 8 3.2 H 3.2 C 2.312 3.2 1.6 3.912 1.6 4.8 V 14.4 A 1.6 1.6
                90 0 0 3.2 16 H 16 A 1.6 1.6 90 0 0 17.6 14.4 V 6.4 C 17.6 5.512 16.88 4.8 16 4.8 Z
                """);
    }

    public static SVGPath FOLDER_MULTIPLE_OUTLINE() {
        return initializeSVGPath("""
                M 17.6 3.2 A 1.6 1.6 90 0 1 19.2 4.8 V 12.8 A 1.6 1.6 90 0 1 17.6 14.4 H 4.8 A 1.6 1.6 90 0 1 3.2 12.8 V
                3.2 A 1.6 1.6 90 0 1 4.8 1.6 H 9.6 L 11.2 3.2 H 17.6 M 1.6 4.8 V 16 H 16 V 17.6 H 1.6 A 1.6 1.6 90 0 1 0
                16 V 8.8 H 0 V 4.8 H 1.6 M 4.8 4.8 V 12.8 H 17.6 V 4.8 H 4.8 Z
                """);
    }

    public static SVGPath ARROW_RIGHT() {
        return initializeSVGPath("""
                M 3.2 8.8 V 10.4 H 12.8 L 8.4 14.8 L 9.536 15.936 L 15.872 9.6 L 9.536 3.264 L 8.4 4.4 L 12.8 8.8 H 3.2
                Z""");
    }

    public static SVGPath ARROW_LEFT() {
        return initializeSVGPath("""
                M 16 8.8 V 10.4 H 6.4 L 10.8 14.8 L 9.664 15.936 L 3.328 9.6 L 9.664 3.264 L 10.8 4.4 L 6.4 8.8 H 16 Z
                """);
    }
}
