package moments4j.service;

import moments4j.model.User;
import moments4j.settings.MomentsSettings;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/**
 * Port of flask-avatars (Avatars extension): Identicon generation, save_avatar, crop_avatar.
 */
@Service
public class AvatarService {

    private final MomentsSettings settings;

    public AvatarService(MomentsSettings settings) {
        this.settings = settings;
    }

    /**
     * Mirrors Identicon().generate(text=username): draws a GitHub-style symmetric identicon
     * and saves it in the three configured sizes. Returns [s, m, l] filenames.
     */
    public String[] generateIdenticon(User user) {
        String text = user.getUsername() == null ? "" : user.getUsername();
        BufferedImage image = drawIdenticon(text, 200);
        int[] sizes = settings.getAvatarsSizeTuple();
        String[] filenames = new String[3];
        for (int i = 0; i < 3; i++) {
            int size = sizes[i];
            String suffix = size == 30 ? "_s" : size == 100 ? "_m" : "_l";
            String filename = "identicon_" + uuid() + suffix + ".png";
            BufferedImage scaled = scale(image, size);
            save(scaled, avatarsDir().resolve(filename));
            filenames[i] = filename;
        }
        user.setAvatarS(filenames[0]);
        user.setAvatarM(filenames[1]);
        user.setAvatarL(filenames[2]);
        return filenames;
    }

    private BufferedImage drawIdenticon(String text, int size) {
        Random random = new Random(text.hashCode());
        // background color like flask-avatars
        float hue = random.nextFloat();
        Color background = Color.getHSBColor(hue, 0.40f, 0.95f);
        Color foreground = Color.WHITE;

        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(background);
        g.fillRect(0, 0, size, size);
        // 5x5 grid, left half mirrored
        int cell = size / 5;
        g.setColor(foreground);
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 5; y++) {
                if (random.nextBoolean()) {
                    g.fillRect(x * cell, y * cell, cell, cell);
                    if (x < 3) {
                        g.fillRect((4 - x) * cell, y * cell, cell, cell);
                    }
                }
            }
        }
        g.dispose();
        return image;
    }

    /** Mirrors avatars.save_avatar(image): stores the raw upload, returns the filename. */
    public String saveAvatar(InputStream content, String originalFilename) {
        String ext = originalFilename != null && originalFilename.toLowerCase().endsWith(".png") ? ".png" : ".jpg";
        String filename = "avatar_" + uuid() + ext;
        try {
            BufferedImage img = ImageIO.read(content);
            if (img == null) {
                throw new IllegalArgumentException("Not a readable image");
            }
            save(img, avatarsDir().resolve(filename));
            return filename;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Mirrors avatars.crop_avatar(filename, x, y, w, h): returns [s, m, l] filenames. */
    public String[] cropAvatar(String filename, int x, int y, int w, int h) {
        try {
            BufferedImage raw = ImageIO.read(avatarsDir().resolve(filename).toFile());
            w = Math.max(1, Math.min(w, raw.getWidth() - x));
            h = Math.max(1, Math.min(h, raw.getHeight() - y));
            BufferedImage cropped = raw.getSubimage(x, y, w, h);
            int[] sizes = settings.getAvatarsSizeTuple();
            String[] filenames = new String[3];
            for (int i = 0; i < 3; i++) {
                int size = sizes[i];
                String suffix = size == 30 ? "_s" : size == 100 ? "_m" : "_l";
                String newFilename = "avatar_" + uuid() + suffix + ".png";
                save(scale(cropped, size), avatarsDir().resolve(newFilename));
                filenames[i] = newFilename;
            }
            return filenames;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private BufferedImage scale(BufferedImage source, int size) {
        BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, size, size, null);
        g.dispose();
        return scaled;
    }

    private void save(BufferedImage image, Path target) {
        try {
            ImageIO.write(image, "png", target.toFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path avatarsDir() {
        try {
            Files.createDirectories(Path.of(settings.getAvatarsSavePath()));
            return Path.of(settings.getAvatarsSavePath());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String uuid() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }
}
