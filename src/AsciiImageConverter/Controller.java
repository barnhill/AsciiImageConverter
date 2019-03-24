package AsciiImageConverter;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;

public class Controller {
    @FXML
    Pane drawPane;
    @FXML
    Button btnLoad;
    @FXML
    ImageView imgOriginal;
    @FXML
    ImageView imgGreyscale;
    @FXML
    TextArea txtAsciiImage;
    @FXML
    ProgressBar progressGreyscale;
    @FXML
    ProgressBar progressCharacter;

    @FXML
    public void initialize() {
        txtAsciiImage.setStyle("-fx-font-family: 'monospaced';");
        txtAsciiImage.prefColumnCountProperty().bind(txtAsciiImage.textProperty().length());
    }

    public void handle(ActionEvent actionEvent) {
        if (actionEvent.getTarget().equals(btnLoad)) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open Resource File");

            ImageConversionWatcher watcher = new ImageConversionWatcher() {
                @Override
                public void originalImage(BufferedImage bufferedImage) {
                    imgOriginal.setImage(SwingFXUtils.toFXImage(bufferedImage, null));
                }

                @Override
                public void greyScaleImage(BufferedImage bufferedImage) {
                    imgGreyscale.setImage(SwingFXUtils.toFXImage(bufferedImage, null));
                }

                @Override
                public void characters(String returnedCharacters) {
                    txtAsciiImage.setText(returnedCharacters);
                }
            };

            try {
                convertImage(fileChooser.showOpenDialog(Main.stage), watcher);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    interface ImageConversionWatcher {
        void originalImage(BufferedImage bufferedImage);
        void greyScaleImage(BufferedImage bufferedImage);
        void characters(String returnedCharacters);
    }

    /**
     * Load the image file specified and convert to a ASCII string representation of the image.
     * @param file File which to open and perform the ASCII conversion on.
     * @param watcher Image watcher that will receive progress updates with the original and greyscaled images after processing
     */
    public void convertImage(File file, ImageConversionWatcher watcher) throws IllegalArgumentException, IOException {
        if (watcher == null) {
            throw new IllegalArgumentException("Watcher must be specified to accept return values");
        }
        if (file == null) {
            throw new IllegalArgumentException("File must be specified to convert");
        }

        BufferedImage origImage;
        origImage = ImageIO.read(file);
        watcher.originalImage(origImage);

        BufferedImage greyScaledImage = toBufferedImage(greyScaleConvert(origImage));
        watcher.greyScaleImage(greyScaledImage);

        new Thread(() -> watcher.characters(characterize(createTiles(greyScaledImage)))).run();
    }

    /**
     * Creates a string representing the intensity array using the character set.
     * @param tiledImage 2d array respresentation of the image as tiled intensities
     * @return String representation of the image as characters
     */
    private String characterize(int[][] tiledImage) {
        String charLookup = "$@B%8&WM#*oahkbdpqwmZO0QLCJUYXzcvunxrjft/\\|()1{}[]?-_+~<>i!lI;:,\"^`'. ";
        StringBuilder charBuilder = new StringBuilder();

        for (int y = 0; y < tiledImage[0].length; y++) {
            for (int[] ints : tiledImage) {
                charBuilder.append(charLookup.charAt((ints[y] * (charLookup.length() - 1)) / 255));
            }
            charBuilder.append(System.getProperty("line.separator"));
        }

        return charBuilder.toString();
    }

    /**
     * Takes the greyscale image and breaks it into rectangular tiles.  Then averages the intensity of that tile to create an intensity array of the tiles.
     * @param greyScaledImage Greyscale image to break into tiles
     * @return 2d array of intensities to use later to determine which character to use for representing the tile
     */
    private int [][] createTiles(BufferedImage greyScaledImage) {
        int factor = (int) Math.round(0.015 * greyScaledImage.getWidth());
        int tilesWide = greyScaledImage.getWidth() / factor;
        int tilesTall = greyScaledImage.getHeight() / factor;
        int tileHeight = greyScaledImage.getHeight() / tilesTall;
        int tileWidth = greyScaledImage.getWidth() / tilesWide;

        int [][] tiledArray = new int[tilesWide][tilesTall];

        for (int row = 0; row < tilesTall; row++) {
            for (int col = 0; col < tilesWide; col++) {
                int sum = 0;
                for (int y = 0; y < tileHeight; y++) {
                    for (int x = 0; x < tileWidth; x++) {
                        sum += greyScaledImage.getRGB((col * tileWidth) + x, (row * tileHeight) + y) & 0xff;
                    }
                }

                tiledArray[col][row] = sum / (tileWidth * tileHeight);
            }
        }

        return tiledArray;
    }

    /**
     * Convert image to greyscale image
     * @param orig Original image to convert
     * @return Convert greyscale version of the original image
     */
    private Image greyScaleConvert(BufferedImage orig) {
        BufferedImage greyScaleImage = new BufferedImage(orig.getWidth(), orig.getHeight(), orig.getType());

        //get image width and height
        int width = orig.getWidth();
        int height = orig.getHeight();

        //convert to grayscale
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int p = orig.getRGB(x, y);

                int a = (p >> 24) & 0xff;
                int r = (p >> 16) & 0xff;
                int g = (p >> 8) & 0xff;
                int b = p & 0xff;

                //calculate average
                int avg = (r + g + b) / 3;

                //replace RGB value with avg
                p = (a << 24) | (avg << 16) | (avg << 8) | avg;

                greyScaleImage.setRGB(x, y, p);
            }
        }

        return greyScaleImage;
    }

    /**
     * Converts a given Image into a BufferedImage
     *
     * @param img The Image to be converted
     * @return The converted BufferedImage
     */
    private static BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage)
        {
            return (BufferedImage) img;
        }

        // Create a buffered image with transparency
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        // Draw the image on to the buffered image
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        // Return the buffered image
        return bimage;
    }
}
