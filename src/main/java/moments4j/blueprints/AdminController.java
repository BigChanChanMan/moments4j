package moments4j.blueprints;

import jakarta.servlet.http.HttpServletRequest;
import moments4j.core.Utils;
import moments4j.core.decorators.LoginRequired;
import moments4j.core.decorators.PermissionRequired;
import moments4j.core.security.CurrentUserService;
import moments4j.forms.EditProfileAdminForm;
import moments4j.model.*;
import moments4j.repo.*;
import moments4j.service.UserService;
import moments4j.settings.MomentsSettings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.util.HashMap;
import java.util.Map;

/**
 * Port of moments/blueprints/admin.py.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository users;
    private final RoleRepository roles;
    private final PhotoRepository photos;
    private final TagRepository tags;
    private final CommentRepository comments;
    private final UserService userService;
    private final CurrentUserService currentUserService;
    private final Utils utils;
    private final MomentsSettings settings;

    public AdminController(UserRepository users, RoleRepository roles, PhotoRepository photos,
                           TagRepository tags, CommentRepository comments, UserService userService,
                           CurrentUserService currentUserService, Utils utils, MomentsSettings settings) {
        this.users = users;
        this.roles = roles;
        this.photos = photos;
        this.tags = tags;
        this.comments = comments;
        this.userService = userService;
        this.currentUserService = currentUserService;
        this.utils = utils;
        this.settings = settings;
    }

    @GetMapping("/")
    @LoginRequired
    @PermissionRequired("MODERATE")
    public String index(Model model) {
        model.addAttribute("userCount", users.count());
        model.addAttribute("lockedUserCount", users.countByLocked(true));
        model.addAttribute("blockedUserCount", users.countByActive(false));
        model.addAttribute("photoCount", photos.count());
        model.addAttribute("reportedPhotosCount", photos.countByFlagGreaterThan(0));
        model.addAttribute("tagCount", tags.count());
        model.addAttribute("commentCount", comments.count());
        model.addAttribute("reportedCommentsCount", comments.countByFlagGreaterThan(0));
        return "admin/home";
    }

    @GetMapping("/profile/{userId}")
    @LoginRequired
    @PermissionRequired("ADMIN")
    public String editProfileAdminPage(@PathVariable Long userId, Model model) {
        User user = users.findById(userId).orElseThrow(
                () -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND));
        EditProfileAdminForm form = new EditProfileAdminForm();
        form.setName(user.getName());
        form.setRole(user.getRole() == null ? null : user.getRole().getId());
        form.setBio(user.getBio());
        form.setWebsite(user.getWebsite());
        form.setLocation(user.getLocation());
        form.setUsername(user.getUsername());
        form.setEmail(user.getEmail());
        form.setConfirmed(user.isConfirmed());
        form.setActive(user.isActive());
        model.addAttribute("form", form);
        model.addAttribute("user", user);
        model.addAttribute("roles", roles.findAll(Sort.by("name")));
        return "admin/edit_profile";
    }

    @PostMapping("/profile/{userId}")
    @LoginRequired
    @PermissionRequired("ADMIN")
    public RedirectView editProfileAdmin(@PathVariable Long userId,
                                         @ModelAttribute("form") EditProfileAdminForm form,
                                         BindingResult result, HttpServletRequest request,
                                         RedirectAttributes ra) {
        User user = users.findById(userId).orElseThrow(
                () -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND));
        if (!result.hasErrors()) {
            if (!form.getUsername().equals(user.getUsername())
                    && users.findByUsername(form.getUsername()).isPresent()) {
                result.rejectValue("username", "", "The username is already in use.");
            }
            if (!form.getEmail().equalsIgnoreCase(user.getEmail())
                    && users.findByEmailIgnoreCase(form.getEmail()).isPresent()) {
                result.rejectValue("email", "", "The email is already in use.");
            }
        }
        if (result.hasErrors()) {
            Utils.flashErrors(result, ra);
            return utils.redirectBack(request, "/admin/profile/" + userId);
        }
        user.setName(form.getName());
        Role role = form.getRole() == null ? null
                : roles.findById(form.getRole()).orElseThrow(
                        () -> new org.springframework.web.server.ResponseStatusException(
                                org.springframework.http.HttpStatus.NOT_FOUND));
        if ("Locked".equals(role.getName())) {
            userService.lock(user);
        }
        user.setRole(role);
        user.setBio(form.getBio());
        user.setWebsite(form.getWebsite());
        user.setConfirmed(form.isConfirmed());
        user.setActive(form.isActive());
        user.setLocation(form.getLocation());
        user.setUsername(form.getUsername());
        user.setEmail(form.getEmail());
        users.save(user);
        Utils.flash(ra, "success", "Profile updated.");
        return utils.redirectBack(request, "/admin/manage/user");
    }

    @PostMapping("/block/user/{userId}")
    @LoginRequired
    @PermissionRequired("MODERATE")
    public RedirectView blockUser(@PathVariable Long userId, HttpServletRequest request,
                                  RedirectAttributes ra) {
        User user = users.findById(userId).orElseThrow(
                () -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND));
        if (isAdminOrModerator(user)) {
            Utils.flash(ra, "warning", "Permission denied.");
        } else {
            userService.block(user);
            Utils.flash(ra, "info", "Account blocked.");
        }
        return utils.redirectBack(request, "/admin/manage/user");
    }

    @PostMapping("/unblock/user/{userId}")
    @LoginRequired
    @PermissionRequired("MODERATE")
    public RedirectView unblockUser(@PathVariable Long userId, HttpServletRequest request,
                                    RedirectAttributes ra) {
        User user = users.findById(userId).orElseThrow(
                () -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND));
        userService.unblock(user);
        Utils.flash(ra, "info", "Block canceled.");
        return utils.redirectBack(request, "/admin/manage/user");
    }

    @PostMapping("/lock/user/{userId}")
    @LoginRequired
    @PermissionRequired("MODERATE")
    public RedirectView lockUser(@PathVariable Long userId, HttpServletRequest request,
                                 RedirectAttributes ra) {
        User user = users.findById(userId).orElseThrow(
                () -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND));
        if (isAdminOrModerator(user)) {
            Utils.flash(ra, "warning", "Permission denied.");
        } else {
            userService.lock(user);
            Utils.flash(ra, "info", "Account locked.");
        }
        return utils.redirectBack(request, "/admin/manage/user");
    }

    @PostMapping("/unlock/user/{userId}")
    @LoginRequired
    @PermissionRequired("MODERATE")
    public RedirectView unlockUser(@PathVariable Long userId, HttpServletRequest request,
                                   RedirectAttributes ra) {
        User user = users.findById(userId).orElseThrow(
                () -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND));
        userService.unlock(user);
        Utils.flash(ra, "info", "Lock canceled.");
        return utils.redirectBack(request, "/admin/manage/user");
    }

    private boolean isAdminOrModerator(User user) {
        String name = user.getRole() == null ? "" : user.getRole().getName();
        return "Administrator".equals(name) || "Moderator".equals(name);
    }

    @RequestMapping(value = "/delete/tag/{tagId}", method = {RequestMethod.GET, RequestMethod.POST})
    @LoginRequired
    @PermissionRequired("MODERATE")
    public RedirectView deleteTag(@PathVariable Long tagId, HttpServletRequest request,
                                  RedirectAttributes ra) {
        Tag tag = tags.findById(tagId).orElseThrow(
                () -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND));
        tags.delete(tag);
        Utils.flash(ra, "info", "Tag deleted.");
        return utils.redirectBack(request, "/admin/manage/tag");
    }

    @GetMapping("/manage/user")
    @LoginRequired
    @PermissionRequired("MODERATE")
    public String manageUser(@RequestParam(defaultValue = "all") String filter,
                             @RequestParam(defaultValue = "1") int page, Model model) {
        Pageable pageable = PageRequest.of(page - 1, settings.getManageUserPerPage(),
                Sort.by(Sort.Direction.DESC, "memberSince"));
        Page<User> pagination = switch (filter) {
            case "locked" -> users.findByLocked(true, pageable);
            case "blocked" -> users.findByActive(false, pageable);
            case "administrator" -> users.findByRoleName("Administrator", pageable);
            case "moderator" -> users.findByRoleName("Moderator", pageable);
            default -> users.findAll(pageable);
        };
        model.addAttribute("filter", filter);
        model.addAttribute("pagination", pagination);
        model.addAttribute("users", pagination.getContent());
        return "admin/manage_user";
    }

    @GetMapping("/manage/photo")
    @LoginRequired
    @PermissionRequired("MODERATE")
    public RedirectView managePhotoDefault() {
        return new RedirectView("/admin/manage/photo/by_flag");
    }

    @GetMapping("/manage/photo/{order}")
    @LoginRequired
    @PermissionRequired("MODERATE")
    public Object managePhoto(@PathVariable String order, @RequestParam(defaultValue = "1") int page,
                              Model model) {
        boolean byTime = "by_time".equals(order);
        Pageable pageable = PageRequest.of(page - 1, settings.getManagePhotoPerPage());
        Page<Photo> pagination = byTime
                ? photos.findAll(PageRequest.of(page - 1, settings.getManagePhotoPerPage(),
                        Sort.by(Sort.Direction.DESC, "createdAt")))
                : photos.findAll(PageRequest.of(page - 1, settings.getManagePhotoPerPage(),
                        Sort.by(Sort.Direction.DESC, "flag")));
        String orderRule = byTime ? "time" : "flag";
        if (page > pagination.getTotalPages() && pagination.getTotalPages() > 0) {
            return new RedirectView("/admin/manage/photo/" + order + "?page=" + pagination.getTotalPages());
        }
        model.addAttribute("pagination", pagination);
        model.addAttribute("photos", pagination.getContent());
        model.addAttribute("orderRule", orderRule);
        return "admin/manage_photo";
    }

    @GetMapping("/manage/tag")
    @LoginRequired
    @PermissionRequired("MODERATE")
    public String manageTag(@RequestParam(defaultValue = "1") int page, Model model) {
        Page<Tag> pagination = tags.findAll(PageRequest.of(page - 1, settings.getManageTagPerPage(),
                Sort.by(Sort.Direction.DESC, "id")));
        model.addAttribute("pagination", pagination);
        model.addAttribute("tags", pagination.getContent());
        return "admin/manage_tag";
    }

    @GetMapping("/manage/comment")
    @LoginRequired
    @PermissionRequired("MODERATE")
    public RedirectView manageCommentDefault() {
        return new RedirectView("/admin/manage/comment/by_flag");
    }

    @GetMapping("/manage/comment/{order}")
    @LoginRequired
    @PermissionRequired("MODERATE")
    public Object manageComment(@PathVariable String order, @RequestParam(defaultValue = "1") int page,
                                Model model) {
        boolean byTime = "by_time".equals(order);
        Page<Comment> pagination = byTime
                ? comments.findAll(PageRequest.of(page - 1, settings.getManageCommentPerPage(),
                        Sort.by(Sort.Direction.DESC, "createdAt")))
                : comments.findAll(PageRequest.of(page - 1, settings.getManageCommentPerPage(),
                        Sort.by(Sort.Direction.DESC, "flag")));
        String orderRule = byTime ? "time" : "flag";
        if (page > pagination.getTotalPages() && pagination.getTotalPages() > 0) {
            return new RedirectView("/admin/manage/comment/" + order + "?page=" + pagination.getTotalPages());
        }
        model.addAttribute("pagination", pagination);
        model.addAttribute("comments", pagination.getContent());
        model.addAttribute("orderRule", orderRule);
        return "admin/manage_comment";
    }

    @PostMapping("/delete/photo/{photoId}")
    @LoginRequired
    @PermissionRequired("MODERATE")
    public RedirectView deletePhoto(@PathVariable Long photoId, HttpServletRequest request,
                                    RedirectAttributes ra) {
        Photo photo = photos.findById(photoId).orElseThrow(
                () -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND));
        photos.delete(photo);
        Utils.flash(ra, "info", "Photo deleted.");
        return utils.redirectBack(request, "/admin/manage/photo");
    }

    @PostMapping("/delete/comment/{commentId}")
    @LoginRequired
    @PermissionRequired("MODERATE")
    public RedirectView deleteComment(@PathVariable Long commentId, HttpServletRequest request,
                                      RedirectAttributes ra) {
        Comment comment = comments.findById(commentId).orElseThrow(
                () -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND));
        comments.delete(comment);
        Utils.flash(ra, "info", "Comment deleted.");
        return utils.redirectBack(request, "/admin/manage/comment");
    }
}
