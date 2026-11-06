package moments4j.blueprints;

import jakarta.servlet.http.HttpServletRequest;
import moments4j.core.Utils;
import moments4j.core.decorators.ConfirmRequired;
import moments4j.core.decorators.LoginRequired;
import moments4j.core.decorators.PermissionRequired;
import moments4j.core.security.CurrentUserService;
import moments4j.forms.CommentForm;
import moments4j.forms.DescriptionForm;
import moments4j.forms.TagForm;
import moments4j.model.*;
import moments4j.repo.*;
import moments4j.service.NotificationService;
import moments4j.service.UserService;
import moments4j.settings.MomentsSettings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Port of moments/blueprints/main.py.
 */
@Controller
public class MainController {

    private final PhotoRepository photos;
    private final TagRepository tags;
    private final CommentRepository comments;
    private final FollowRepository follows;
    private final CollectionRepository collections;
    private final NotificationRepository notifications;
    private final UserRepository users;
    private final UserService userService;
    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;
    private final Utils utils;
    private final MomentsSettings settings;

    public MainController(PhotoRepository photos, TagRepository tags, CommentRepository comments,
                          FollowRepository follows, CollectionRepository collections,
                          NotificationRepository notifications, UserRepository users,
                          UserService userService, NotificationService notificationService,
                          CurrentUserService currentUserService, Utils utils, MomentsSettings settings) {
        this.photos = photos;
        this.tags = tags;
        this.comments = comments;
        this.follows = follows;
        this.collections = collections;
        this.notifications = notifications;
        this.users = users;
        this.userService = userService;
        this.notificationService = notificationService;
        this.currentUserService = currentUserService;
        this.utils = utils;
        this.settings = settings;
    }

    @GetMapping("/")
    public String index(@RequestParam(defaultValue = "1") int page, Model model) {
        User current = currentUserService.get();
        Page<Photo> pagination = null;
        List<Photo> photoList = null;
        if (current != null) {
            List<Long> followedIds = follows.findFollowedIds(current.getId());
            Pageable pageable = PageRequest.of(page - 1, settings.getPhotoPerPage(),
                    Sort.by(Sort.Direction.DESC, "createdAt"));
            pagination = photos.findByAuthorIdIn(followedIds, pageable);
            photoList = pagination.getContent();
        }
        List<Tag> tagList = this.tags.findTopTags(PageRequest.of(0, 10));
        model.addAttribute("pagination", pagination);
        model.addAttribute("photos", photoList);
        model.addAttribute("tags", tagList);
        return "main/index";
    }

    @GetMapping("/explore")
    public String explore(Model model) {
        // ponytail: mirrors order_by(func.random()).limit(12) — shuffle ids in Java,
        // switch to native order-by-random if the photo table grows large
        List<Long> ids = new java.util.ArrayList<>();
        photos.findAll().forEach(p -> ids.add(p.getId()));
        java.util.Collections.shuffle(ids);
        List<Photo> randomPhotos = ids.subList(0, Math.min(12, ids.size())).stream()
                .map(id -> photos.findById(id).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
        model.addAttribute("photos", randomPhotos);
        return "main/explore";
    }

    @GetMapping("/search")
    public Object search(@RequestParam(required = false) String q,
                         @RequestParam(defaultValue = "photo") String category,
                         @RequestParam(defaultValue = "1") int page,
                         HttpServletRequest request, Model model, RedirectAttributes ra) {
        if (q == null || q.isBlank()) {
            Utils.flash(ra, "warning", "Enter keyword about photo, user or tag.");
            return utils.redirectBack(request, "/");
        }
        Pageable pageable = PageRequest.of(page - 1, settings.getSearchResultPerPage());
        Page<?> pagination = switch (category) {
            case "user" -> users.search(q, pageable);
            case "tag" -> tags.search(q, pageable);
            default -> photos.search(q, pageable);
        };
        model.addAttribute("q", q);
        model.addAttribute("results", pagination.getContent());
        model.addAttribute("pagination", pagination);
        model.addAttribute("category", category);
        return "main/search";
    }

    @GetMapping("/notifications")
    @LoginRequired
    public String showNotifications(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(required = false) String filter, Model model) {
        User current = currentUserService.get();
        Pageable pageable = PageRequest.of(page - 1, settings.getNotificationPerPage());
        Page<Notification> pagination;
        if ("unread".equals(filter)) {
            pagination = notifications.findByReceiverIdAndReadFalseOrderByCreatedAtDesc(current.getId(), pageable);
        } else {
            pagination = notifications.findByReceiverIdOrderByCreatedAtDesc(current.getId(), pageable);
        }
        model.addAttribute("filter", filter);
        model.addAttribute("pagination", pagination);
        model.addAttribute("notifications", pagination.getContent());
        return "main/notifications";
    }

    @PostMapping("/notifications/read/{notificationId}")
    @LoginRequired
    public RedirectView readNotification(@PathVariable Long notificationId, RedirectAttributes ra) {
        Notification notification = notifications.findById(notificationId)
                .orElseThrow(() -> notFound());
        User current = currentUserService.get();
        if (!current.getId().equals(notification.getReceiver().getId())) {
            throw forbidden();
        }
        notification.setRead(true);
        notifications.save(notification);
        Utils.flash(ra, "success", "Notification archived.");
        return new RedirectView("/notifications");
    }

    @PostMapping("/notifications/read/all")
    @LoginRequired
    public RedirectView readAllNotification(RedirectAttributes ra) {
        User current = currentUserService.get();
        Page<Notification> unread = notifications.findByReceiverIdAndReadFalseOrderByCreatedAtDesc(
                current.getId(), Pageable.unpaged());
        unread.forEach(n -> n.setRead(true));
        notifications.saveAll(unread);
        Utils.flash(ra, "success", "All notifications archived.");
        return new RedirectView("/notifications");
    }

    // ---- image/avatar serving (send_from_directory) ----

    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<byte[]> getImage(@PathVariable String filename) {
        return serveFile(utils.uploadDir(), filename);
    }

    @GetMapping("/avatars/{filename:.+}")
    public ResponseEntity<byte[]> getAvatar(@PathVariable String filename) {
        return serveFile(utils.avatarsDir(), filename);
    }

    private ResponseEntity<byte[]> serveFile(Path dir, String filename) {
        Path file = dir.resolve(filename).normalize();
        if (!file.startsWith(dir) || !Files.exists(file)) {
            throw notFound();
        }
        try {
            String contentType = Files.probeContentType(file);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                    .body(Files.readAllBytes(file));
        } catch (IOException e) {
            throw notFound();
        }
    }

    // ---- upload ----

    @GetMapping("/upload")
    @LoginRequired
    @ConfirmRequired
    @PermissionRequired("UPLOAD")
    public String uploadPage(Model model) {
        model.addAttribute("dropzoneAction", "/upload");
        return "main/upload";
    }

    @PostMapping("/upload")
    @LoginRequired
    @ConfirmRequired
    @PermissionRequired("UPLOAD")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> upload(
            @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return org.springframework.http.ResponseEntity.badRequest().body("No image.");
        }
        if (!utils.validateImage(file.getOriginalFilename())) {
            return org.springframework.http.ResponseEntity.badRequest().body("Invalid image.");
        }
        User current = currentUserService.get();
        String filename = utils.renameImage(file.getOriginalFilename());
        Path saved = utils.uploadDir().resolve(filename);
        file.transferTo(saved.toAbsolutePath());
        String filenameS = utils.resizeImage(saved, filename, settings.getPhotoSizeSmall());
        String filenameM = utils.resizeImage(saved, filename, settings.getPhotoSizeMedium());
        Photo photo = new Photo();
        photo.setFilename(filename);
        photo.setFilenameS(filenameS);
        photo.setFilenameM(filenameM);
        photo.setAuthor(current);
        photos.save(photo);
        return org.springframework.http.ResponseEntity.ok("ok");  // dropzone only cares about a 2xx response
    }

    // ---- photo pages ----

    @GetMapping("/photo/{photoId}")
    public String showPhoto(@PathVariable Long photoId, @RequestParam(defaultValue = "1") int page,
                            @RequestParam(required = false) Long reply,
                            @RequestParam(required = false) String author, Model model) {
        Photo photo = photos.findById(photoId).orElseThrow(this::notFound);
        Pageable pageable = PageRequest.of(page - 1, settings.getCommentPerPage(),
                Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<Comment> pagination = comments.findByPhotoIdOrderByCreatedAtAsc(photo.getId(), pageable);

        DescriptionForm descriptionForm = new DescriptionForm();
        descriptionForm.setDescription(photo.getDescription());

        model.addAttribute("photo", photo);
        model.addAttribute("commentForm", new CommentForm());
        model.addAttribute("descriptionForm", descriptionForm);
        model.addAttribute("tagForm", new TagForm());
        model.addAttribute("pagination", pagination);
        model.addAttribute("comments", pagination.getContent());
        model.addAttribute("reply", reply);
        model.addAttribute("author", author);
        return "main/photo";
    }

    @GetMapping("/photo/n/{photoId}")
    public RedirectView getNextPhoto(@PathVariable Long photoId, RedirectAttributes ra) {
        Photo photo = photos.findById(photoId).orElseThrow(this::notFound);
        Photo nextPhoto = first(photos.findNextOf(photo.getAuthor().getId(), photo.getCreatedAt(),
                PageRequest.of(0, 1)));
        if (nextPhoto == null) {
            Utils.flash(ra, "info", "This is already the last one.");
            return new RedirectView("/photo/" + photoId);
        }
        return new RedirectView("/photo/" + nextPhoto.getId());
    }

    @GetMapping("/photo/p/{photoId}")
    public RedirectView getPreviousPhoto(@PathVariable Long photoId, RedirectAttributes ra) {
        Photo photo = photos.findById(photoId).orElseThrow(this::notFound);
        Photo previousPhoto = first(photos.findPreviousOf(photo.getAuthor().getId(), photo.getCreatedAt(),
                PageRequest.of(0, 1)));
        if (previousPhoto == null) {
            Utils.flash(ra, "info", "This is already the first one.");
            return new RedirectView("/photo/" + photoId);
        }
        return new RedirectView("/photo/" + previousPhoto.getId());
    }

    private Photo first(List<Photo> list) {
        return list.isEmpty() ? null : list.get(0);
    }

    // ---- collect ----

    @PostMapping("/collect/{photoId}")
    @LoginRequired
    @ConfirmRequired
    @PermissionRequired("COLLECT")
    public RedirectView collect(@PathVariable Long photoId, RedirectAttributes ra) {
        Photo photo = photos.findById(photoId).orElseThrow(this::notFound);
        User current = currentUserService.get();
        if (userService.isCollecting(current, photo)) {
            Utils.flash(ra, "info", "Already collected.");
            return new RedirectView("/photo/" + photoId);
        }
        userService.collect(current, photo);
        Utils.flash(ra, "success", "Photo collected.");
        if (!current.getId().equals(photo.getAuthor().getId())
                && photo.getAuthor().isReceiveCollectNotification()) {
            notificationService.pushCollectNotification(current, photoId, photo.getAuthor(),
                    "/user/" + current.getUsername(), "/photo/" + photoId);
        }
        return new RedirectView("/photo/" + photoId);
    }

    @PostMapping("/uncollect/{photoId}")
    @LoginRequired
    public RedirectView uncollect(@PathVariable Long photoId, RedirectAttributes ra) {
        Photo photo = photos.findById(photoId).orElseThrow(this::notFound);
        User current = currentUserService.get();
        if (!userService.isCollecting(current, photo)) {
            Utils.flash(ra, "info", "Not collect yet.");
            return new RedirectView("/photo/" + photoId);
        }
        userService.uncollect(current, photo);
        Utils.flash(ra, "info", "Photo uncollected.");
        return new RedirectView("/photo/" + photoId);
    }

    // ---- report ----

    @PostMapping("/report/comment/{commentId}")
    @LoginRequired
    @ConfirmRequired
    public RedirectView reportComment(@PathVariable Long commentId, RedirectAttributes ra) {
        Comment comment = comments.findById(commentId).orElseThrow(this::notFound);
        comment.setFlag(comment.getFlag() + 1);
        comments.save(comment);
        Utils.flash(ra, "success", "Comment reported.");
        return new RedirectView("/photo/" + comment.getPhoto().getId());
    }

    @PostMapping("/report/photo/{photoId}")
    @LoginRequired
    @ConfirmRequired
    public RedirectView reportPhoto(@PathVariable Long photoId, RedirectAttributes ra) {
        Photo photo = photos.findById(photoId).orElseThrow(this::notFound);
        photo.setFlag(photo.getFlag() + 1);
        photos.save(photo);
        Utils.flash(ra, "success", "Photo reported.");
        return new RedirectView("/photo/" + photo.getId());
    }

    @GetMapping("/photo/{photoId}/collectors")
    public String showCollectors(@PathVariable Long photoId, @RequestParam(defaultValue = "1") int page,
                                 Model model) {
        Photo photo = photos.findById(photoId).orElseThrow(this::notFound);
        Page<Collection> pagination = collections.findCollectors(photoId,
                PageRequest.of(page - 1, settings.getUserPerPage()));
        model.addAttribute("collections", pagination.getContent());
        model.addAttribute("photo", photo);
        model.addAttribute("pagination", pagination);
        return "main/collectors";
    }

    // ---- description / comment / tag management ----

    @PostMapping("/photo/{photoId}/description")
    @LoginRequired
    public RedirectView editDescription(@PathVariable Long photoId,
                                        @ModelAttribute DescriptionForm form, BindingResult result,
                                        RedirectAttributes ra) {
        Photo photo = photos.findById(photoId).orElseThrow(this::notFound);
        User current = currentUserService.get();
        if (!current.getId().equals(photo.getAuthor().getId()) && !userService.can(current, "MODERATE")) {
            throw forbidden();
        }
        if (!result.hasErrors()) {
            photo.setDescription(form.getDescription());
            photos.save(photo);
            Utils.flash(ra, "success", "Description updated.");
        }
        Utils.flashErrors(result, ra);
        return new RedirectView("/photo/" + photoId);
    }

    @PostMapping("/photo/{photoId}/comment/new")
    @LoginRequired
    @PermissionRequired("COMMENT")
    public RedirectView newComment(@PathVariable Long photoId,
                                   @RequestParam(defaultValue = "1") int page,
                                   @RequestParam(required = false) Long reply,
                                   @ModelAttribute CommentForm form, BindingResult result,
                                   RedirectAttributes ra) {
        Photo photo = photos.findById(photoId).orElseThrow(this::notFound);
        User current = currentUserService.get();
        if (!result.hasErrors()) {
            Comment comment = new Comment();
            comment.setBody(form.getBody());
            comment.setAuthor(current);
            comment.setPhoto(photo);
            if (reply != null) {
                Comment replied = comments.findById(reply).orElseThrow(this::notFound);
                comment.setReplied(replied);
                notificationService.pushCommentNotification(photo.getId(), replied.getAuthor(), page,
                        "/photo/" + photo.getId() + "?page=" + page);
            }
            comments.save(comment);
            Utils.flash(ra, "success", "Comment published.");
            if (!current.getId().equals(photo.getAuthor().getId())) {
                notificationService.pushCommentNotification(photo.getId(), photo.getAuthor(), page,
                        "/photo/" + photo.getId() + "?page=" + page);
            }
        }
        Utils.flashErrors(result, ra);
        return new RedirectView("/photo/" + photoId + "?page=" + page);
    }

    @PostMapping("/photo/{photoId}/tag/new")
    @LoginRequired
    public RedirectView newTag(@PathVariable Long photoId,
                               @ModelAttribute TagForm form, BindingResult result,
                               RedirectAttributes ra) {
        Photo photo = photos.findById(photoId).orElseThrow(this::notFound);
        User current = currentUserService.get();
        if (!current.getId().equals(photo.getAuthor().getId()) && !userService.can(current, "MODERATE")) {
            throw forbidden();
        }
        if (!result.hasErrors()) {
            for (String name : form.getTag().split("\\s+")) {
                if (name.isBlank()) {
                    continue;
                }
                Tag tag = tags.findByName(name).orElseGet(() -> {
                    Tag t = new Tag();
                    t.setName(name);
                    return tags.save(t);
                });
                if (!photo.getTags().contains(tag)) {
                    photo.getTags().add(tag);
                    photos.save(photo);
                }
            }
            Utils.flash(ra, "success", "Tag added.");
        }
        Utils.flashErrors(result, ra);
        return new RedirectView("/photo/" + photoId);
    }

    @PostMapping("/set-comment/{photoId}")
    @LoginRequired
    public RedirectView setComment(@PathVariable Long photoId, RedirectAttributes ra) {
        Photo photo = photos.findById(photoId).orElseThrow(this::notFound);
        User current = currentUserService.get();
        if (!current.getId().equals(photo.getAuthor().getId())) {
            throw forbidden();
        }
        if (photo.isCanComment()) {
            photo.setCanComment(false);
            Utils.flash(ra, "info", "Comment disabled");
        } else {
            photo.setCanComment(true);
            Utils.flash(ra, "info", "Comment enabled.");
        }
        photos.save(photo);
        return new RedirectView("/photo/" + photoId);
    }

    @GetMapping("/reply/comment/{commentId}")
    @LoginRequired
    @PermissionRequired("COMMENT")
    public RedirectView replyComment(@PathVariable Long commentId) {
        Comment comment = comments.findById(commentId).orElseThrow(this::notFound);
        return new RedirectView("/photo/" + comment.getPhoto().getId()
                + "?reply=" + commentId + "&author=" + comment.getAuthor().getName() + "#comment-form");
    }

    @PostMapping("/delete/photo/{photoId}")
    @LoginRequired
    public RedirectView deletePhoto(@PathVariable Long photoId, RedirectAttributes ra) {
        Photo photo = photos.findById(photoId).orElseThrow(this::notFound);
        User current = currentUserService.get();
        if (!current.getId().equals(photo.getAuthor().getId()) && !userService.can(current, "MODERATE")) {
            throw forbidden();
        }
        photos.delete(photo);
        Utils.flash(ra, "info", "Photo deleted.");

        Long authorId = photo.getAuthor().getId();
        Photo nextPhoto = first(photos.findNextOf(authorId, photo.getCreatedAt(), PageRequest.of(0, 1)));
        if (nextPhoto == null) {
            Photo previousPhoto = first(photos.findPreviousOf(authorId, photo.getCreatedAt(),
                    PageRequest.of(0, 1)));
            if (previousPhoto == null) {
                return new RedirectView("/user/" + photo.getAuthor().getUsername());
            }
            return new RedirectView("/photo/" + previousPhoto.getId());
        }
        return new RedirectView("/photo/" + nextPhoto.getId());
    }

    @PostMapping("/delete/comment/{commentId}")
    @LoginRequired
    public RedirectView deleteComment(@PathVariable Long commentId, RedirectAttributes ra) {
        Comment comment = comments.findById(commentId).orElseThrow(this::notFound);
        User current = currentUserService.get();
        boolean isAuthor = current.getId().equals(comment.getAuthor().getId());
        boolean isPhotoAuthor = current.getId().equals(comment.getPhoto().getAuthor().getId());
        if (!isAuthor && !isPhotoAuthor && !userService.can(current, "MODERATE")) {
            throw forbidden();
        }
        comments.delete(comment);
        Utils.flash(ra, "info", "Comment deleted.");
        return new RedirectView("/photo/" + comment.getPhoto().getId());
    }

    @GetMapping("/tag/{tagId}")
    public String showTag(@PathVariable Long tagId, @RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "time") String orderRule, Model model) {
        Tag tag = tags.findById(tagId).orElseThrow(this::notFound);
        Page<Photo> pagination = photos.findByTagsIdOrderByCreatedAtDesc(tagId,
                PageRequest.of(page - 1, settings.getPhotoPerPage()));
        List<Photo> photoList = new java.util.ArrayList<>(pagination.getContent());
        if ("collections".equals(orderRule)) {
            photoList.sort((a, b) -> Long.compare(userService.photoCollectorsCount(b),
                    userService.photoCollectorsCount(a)));
        }
        model.addAttribute("tag", tag);
        model.addAttribute("pagination", pagination);
        model.addAttribute("photos", photoList);
        model.addAttribute("orderRule", orderRule);
        return "main/tag";
    }

    @PostMapping("/delete/tag/{photoId}/{tagId}")
    @LoginRequired
    public RedirectView deleteTag(@PathVariable Long photoId, @PathVariable Long tagId,
                                  RedirectAttributes ra) {
        Photo photo = photos.findById(photoId).orElseThrow(this::notFound);
        Tag tag = tags.findById(tagId).orElseThrow(this::notFound);
        User current = currentUserService.get();
        if (!current.getId().equals(photo.getAuthor().getId()) && !userService.can(current, "MODERATE")) {
            throw forbidden();
        }
        photo.getTags().remove(tag);
        photos.save(photo);

        if (tag.getPhotos().isEmpty()) {
            tags.delete(tag);
        }
        Utils.flash(ra, "info", "Tag deleted.");
        return new RedirectView("/photo/" + photoId);
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
    }

    private ResponseStatusException forbidden() {
        return new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
    }
}
