package moments4j.core;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import moments4j.model.User;
import moments4j.settings.MomentsSettings;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import javax.crypto.SecretKey;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Port of moments/utils.py (tokens, image helpers, redirect_back, flash_errors).
 */
@Component
public class Utils {

    public static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg");

    private final MomentsSettings settings;

    public Utils(MomentsSettings settings) {
        this.settings = settings;
    }

    // ---- flash (flask.flash equivalent) ----

    /** Adds a flash message rendered as an alert on the next page. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void flash(RedirectAttributes ra, String category, String message) {
        Map attrs = (Map) ra.getFlashAttributes();
        List<Object[]> flashes = (List<Object[]>) attrs.computeIfAbsent("flashes", k -> new ArrayList<>());
        flashes.add(new Object[] {category, message});
    }

    public static void flash(Model model, String category, String message) {
        // used for non-redirect renders; template reads the same variable
        List<Object[]> flashes = new ArrayList<>();
        flashes.add(new Object[] {category, message});
        model.addAttribute("flashes", flashes);
    }

    /** Mirrors flash_errors(form). */
    public static void flashErrors(BindingResult result, RedirectAttributes ra) {
        result.getFieldErrors().forEach(fe ->
                flash(ra, "message", "Error in the " + fe.getField() + " field - " + fe.getDefaultMessage()));
    }

    // ---- tokens (generate_token / parse_token) ----

    public String generateToken(User user, MomentsSettings.Operation operation, Duration expiration) {
        return generateToken(user, operation, expiration, null);
    }

    public String generateToken(User user, MomentsSettings.Operation operation, Duration expiration,
                                Map<String, Object> extra) {
        var builder = Jwts.builder()
                .claim("id", user.getId())
                .claim("operation", operation.value())
                .expiration(Date.from(Instant.now().plus(expiration)));
        if (extra != null) {
            extra.forEach(builder::claim);
        }
        return builder.signWith(secretKey()).compact();
    }

    /** Returns the payload claims, or an empty map if the token is invalid/expired/mismatched. */
    public Map<String, Object> parseToken(User user, String token, MomentsSettings.Operation operation) {
        try {
            Claims claims = Jwts.parser().verifyWith(secretKey()).build()
                    .parseSignedClaims(token).getPayload();
            if (!operation.value().equals(claims.get("operation", String.class))
                    || !user.getId().equals(claims.get("id", Long.class))) {
                return Map.of();
            }
            return new HashMap<>(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return Map.of();
        }
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(secret().getBytes());
    }

    private String secret() {
        // mirrors SECRET_KEY = os.getenv('SECRET_KEY', 'secret string')
        String env = System.getProperty("SECRET_KEY", System.getenv().getOrDefault("SECRET_KEY", "secret string"));
        StringBuilder padded = new StringBuilder(env);
        while (padded.toString().getBytes().length < 32) {
            padded.append(padded);  // ensure >= 32 bytes for HS256
        }
        return padded.substring(0, Math.max(32, padded.length()));
    }

    // ---- image helpers (rename_image / resize_image / validate_image) ----

    public String renameImage(String oldFilename) {
        String ext = extOf(oldFilename);
        return UUID.randomUUID().toString().replace("-", "") + ext;
    }

    public boolean validateImage(String filename) {
        String ext = extOf(filename).toLowerCase();
        return filename.contains(".") && ALLOWED_IMAGE_EXTENSIONS.contains(ext);
    }

    /** Resizes to baseWidth if wider, saves as <name>_s/_m.<ext>, returns the new filename. */
    public String resizeImage(Path source, String filename, int baseWidth) throws IOException {
        BufferedImage img = ImageIO.read(source.toFile());
        if (img == null || img.getWidth() <= baseWidth) {
            return filename;
        }
        int height = (int) Math.round(img.getHeight() * (baseWidth / (double) img.getWidth()));
        String suffix = baseWidth == settings.getPhotoSizeSmall() ? "_s" : "_m";
        String newFilename = stripExt(filename) + suffix + extOf(filename);
        Thumbnails.of(img).size(baseWidth, height)
                .outputQuality(0.85)
                .toFile(uploadDir().resolve(newFilename).toFile());
        return newFilename;
    }

    // ---- files ----

    public Path uploadDir() {
        return ensureDir(Path.of(settings.getUploadPath()));
    }

    public Path avatarsDir() {
        return ensureDir(Path.of(settings.getAvatarsSavePath()));
    }

    private Path ensureDir(Path dir) {
        try {
            Files.createDirectories(dir);
            return dir;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ---- redirect_back / is_safe_url ----

    public boolean isSafeUrl(HttpServletRequest request, String target) {
        if (target == null || target.isBlank()) {
            return false;
        }
        try {
            if (!target.startsWith("http")) {
                // relative target: allow site-relative paths only
                return target.startsWith("/") && !target.startsWith("//");
            }
            URI base = URI.create(request.getRequestURL().toString());
            URI test = URI.create(target);
            String scheme = test.getScheme();
            return ("http".equals(scheme) || "https".equals(scheme))
                    && Objects.equals(base.getHost(), test.getHost())
                    && base.getPort() == test.getPort();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Mirrors redirect_back(default='main.index'). Returns the redirect view to return from a handler. */
    public RedirectView redirectBack(HttpServletRequest request, String defaultUrl) {
        String next = request.getParameter("next");
        String referrer = request.getHeader("Referer");
        if (next != null && isSafeUrl(request, next)) {
            return new RedirectView(next);
        }
        if (referrer != null && isSafeUrl(request, referrer)) {
            return new RedirectView(referrer);
        }
        return new RedirectView(defaultUrl);
    }

    private static String extOf(String filename) {
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot);
    }

    private static String stripExt(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? filename : filename.substring(0, dot);
    }
}
