package moments4j.blueprints;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import moments4j.core.Utils;
import moments4j.core.decorators.ConfirmRequired;
import moments4j.core.decorators.FreshLoginRequired;
import moments4j.core.decorators.LoginRequired;
import moments4j.core.decorators.PermissionRequired;
import moments4j.core.security.CurrentUserService;
import moments4j.forms.*;
import moments4j.model.Follow;
import moments4j.model.User;
import moments4j.repo.UserRepository;
import moments4j.service.AvatarService;
import moments4j.service.MailService;
import moments4j.service.UserService;
import moments4j.settings.MomentsSettings;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.time.Duration;
import java.util.Map;

/**
 * Port of moments/blueprints/user.py.
 */
@Controller
@RequestMapping("/user")
public class UserController {

    private final UserRepository users;
    private final UserService userService;
    private final MailService mail;
    private final AvatarService avatars;
    private final moments4j.service.NotificationService notificationService;
    private final CurrentUserService currentUserService;
    private final Utils utils;
    private final MomentsSettings settings;

    public UserController(UserRepository users, UserService userService, MailService mail,
                          AvatarService avatars, moments4j.service.NotificationService notificationService,
                          CurrentUserService currentUserService,
                          Utils utils, MomentsSettings settings) {
        this.users = users;
        this.userService = userService;
        this.mail = mail;
        this.avatars = avatars;
        this.notificationService = notificationService;
        this.currentUserService = currentUserService;
        this.utils = utils;
        this.settings = settings;
    }

    @GetMapping("/{username}")
    public String index(@PathVariable String username, @RequestParam(defaultValue = "1") int page,
                        HttpServletRequest request, RedirectAttributes ra, Model model) {
        User user = users.findByUsername(username).orElseThrow(
                () -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND));
        User current = currentUserService.get();
        if (current != null && current.getId().equals(user.getId())) {
            if (user.isLocked()) {
                Utils.flash(model, "danger", "Your account is locked.");
            }
            if (!user.isActive()) {
                // mirrors logout_user() in user.index
                request.getSession().invalidate();
                model.addAttribute("currentUser", null);
            }
        }
        Page<moments4j.model.Photo> pagination = userService.userPhotos(user, page, settings.getPhotoPerPage());
        model.addAttribute("user", user);
        model.addAttribute("pagination", pagination);
        model.addAttribute("photos", pagination.getContent());
        return "user/index";
    }

    @GetMapping("/{username}/collections")
    public String showCollections(@PathVariable String username, @RequestParam(defaultValue = "1") int page,
                                  Model model) {
        User user = users.findByUsername(username).orElseThrow(
                () -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND));
        Page<moments4j.model.Collection> pagination =
                userService.userCollections(user, page, settings.getPhotoPerPage());
        model.addAttribute("user", user);
        model.addAttribute("pagination", pagination);
        model.addAttribute("collections", pagination.getContent());
        return "user/collections";
    }

    @PostMapping("/follow/{username}")
    @LoginRequired
    @ConfirmRequired
    @PermissionRequired("FOLLOW")
    public RedirectView follow(@PathVariable String username, HttpServletRequest request,
                               RedirectAttributes ra) {
        User user = users.findByUsername(username).orElseThrow(
                () -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND));
        User current = currentUserService.get();
        if (userService.isFollowing(current, user)) {
            Utils.flash(ra, "info", "Already followed.");
            return new RedirectView("/user/" + username);
        }
        userService.follow(current, user);
        Utils.flash(ra, "success", "User followed.");
        notificationService.pushFollowNotification(current, user, "/user/" + current.getUsername());
        return utils.redirectBack(request, "/user/" + username);
    }

    @PostMapping("/unfollow/{username}")
    @LoginRequired
    public RedirectView unfollow(@PathVariable String username, HttpServletRequest request,
                                 RedirectAttributes ra) {
        User user = users.findByUsername(username).orElseThrow(
                () -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND));
        User current = currentUserService.get();
        if (!userService.isFollowing(current, user)) {
            Utils.flash(ra, "info", "Not following yet.");
            return new RedirectView("/user/" + username);
        }
        userService.unfollow(current, user);
        Utils.flash(ra, "info", "User unfollowed.");
        return utils.redirectBack(request, "/user/" + username);
    }

    @GetMapping("/{username}/followers")
    public String showFollowers(@PathVariable String username, @RequestParam(defaultValue = "1") int page,
                                Model model) {
        User user = users.findByUsername(username).orElseThrow(
                () -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND));
        Page<Follow> pagination = userService.userFollowers(user, page, settings.getUserPerPage());
        model.addAttribute("user", user);
        model.addAttribute("pagination", pagination);
        model.addAttribute("follows", pagination.getContent());
        return "user/followers";
    }

    @GetMapping("/{username}/following")
    public String showFollowing(@PathVariable String username, @RequestParam(defaultValue = "1") int page,
                                Model model) {
        User user = users.findByUsername(username).orElseThrow(
                () -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND));
        Page<Follow> pagination = userService.userFollowing(user, page, settings.getUserPerPage());
        model.addAttribute("user", user);
        model.addAttribute("pagination", pagination);
        model.addAttribute("follows", pagination.getContent());
        return "user/following";
    }

    // ---- settings ----

    @GetMapping("/settings/profile")
    @LoginRequired
    public String editProfilePage(Model model) {
        User current = currentUserService.get();
        EditProfileForm form = new EditProfileForm();
        form.setName(current.getName());
        form.setUsername(current.getUsername());
        form.setBio(current.getBio());
        form.setWebsite(current.getWebsite());
        form.setLocation(current.getLocation());
        model.addAttribute("form", form);
        return "user/settings/edit_profile";
    }

    @PostMapping("/settings/profile")
    @LoginRequired
    public RedirectView editProfile(@ModelAttribute("form") EditProfileForm form, BindingResult result,
                                    RedirectAttributes ra) {
        User current = currentUserService.get();
        if (!result.hasErrors()) {
            if (!form.getUsername().equals(current.getUsername())
                    && users.findByUsername(form.getUsername()).isPresent()) {
                result.rejectValue("username", "", "The username is already in use.");
            }
            if (form.getWebsite() != null && !form.getWebsite().isBlank()
                    && !isValidUrl(form.getWebsite())) {
                result.rejectValue("website", "", "Invalid URL.");
            }
        }
        if (result.hasErrors()) {
            Utils.flashErrors(result, ra);
            return new RedirectView("/user/settings/profile");
        }
        current.setName(form.getName());
        current.setUsername(form.getUsername());
        current.setBio(form.getBio());
        current.setWebsite(form.getWebsite());
        current.setLocation(form.getLocation());
        users.save(current);
        Utils.flash(ra, "success", "Profile updated.");
        return new RedirectView("/user/" + current.getUsername());
    }

    private boolean isValidUrl(String url) {
        try {
            java.net.URI uri = java.net.URI.create(url);
            return uri.getScheme() != null && uri.getHost() != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @GetMapping("/settings/avatar")
    @LoginRequired
    @ConfirmRequired
    public String changeAvatarPage(Model model) {
        model.addAttribute("cropForm", new CropAvatarForm());
        return "user/settings/change_avatar";
    }

    @PostMapping("/settings/avatar/upload")
    @LoginRequired
    @ConfirmRequired
    public RedirectView uploadAvatar(@RequestParam("image") MultipartFile image, RedirectAttributes ra) {
        User current = currentUserService.get();
        if (image == null || image.isEmpty()) {
            Utils.flash(ra, "message", "Error in the image field - This field is required.");
            return new RedirectView("/user/settings/avatar");
        }
        String original = image.getOriginalFilename() == null ? "" : image.getOriginalFilename().toLowerCase();
        if (!original.endsWith(".jpg") && !original.endsWith(".png") && !original.endsWith(".jpeg")) {
            Utils.flash(ra, "message", "Error in the image field - The file format should be .jpg or .png.");
            return new RedirectView("/user/settings/avatar");
        }
        try {
            String filename = avatars.saveAvatar(image.getInputStream(), original);
            current.setAvatarRaw(filename);
            users.save(current);
            Utils.flash(ra, "success", "Image uploaded, please crop.");
        } catch (Exception e) {
            Utils.flash(ra, "message", "Error in the image field - Invalid image file.");
        }
        return new RedirectView("/user/settings/avatar");
    }

    @PostMapping("/settings/avatar/crop")
    @LoginRequired
    @ConfirmRequired
    public RedirectView cropAvatar(@ModelAttribute CropAvatarForm form, BindingResult result,
                                   RedirectAttributes ra) {
        User current = currentUserService.get();
        if (current.getAvatarRaw() == null) {
            Utils.flash(ra, "message", "Error in the crop field - Upload an image first.");
            return new RedirectView("/user/settings/avatar");
        }
        String[] filenames = avatars.cropAvatar(current.getAvatarRaw(), form.getX(), form.getY(),
                form.getW(), form.getH());
        current.setAvatarS(filenames[0]);
        current.setAvatarM(filenames[1]);
        current.setAvatarL(filenames[2]);
        users.save(current);
        Utils.flash(ra, "success", "Avatar updated.");
        return new RedirectView("/user/settings/avatar");
    }

    @GetMapping("/settings/change-password")
    @FreshLoginRequired
    public String changePasswordPage(Model model) {
        model.addAttribute("form", new ChangePasswordForm());
        return "user/settings/change_password";
    }

    @PostMapping("/settings/change-password")
    @FreshLoginRequired
    public RedirectView changePassword(@ModelAttribute("form") ChangePasswordForm form, BindingResult result,
                                       RedirectAttributes ra) {
        User current = currentUserService.get();
        if (!result.hasErrors() && !form.getPassword().equals(form.getPassword2())) {
            result.rejectValue("password2", "", "Field must be equal to password.");
        }
        if (!result.hasErrors()) {
            if (userService.validatePassword(current, form.getOldPassword())) {
                userService.setPassword(current, form.getPassword());
                users.save(current);
                Utils.flash(ra, "success", "Password updated.");
                return new RedirectView("/user/" + current.getUsername());
            }
            Utils.flash(ra, "warning", "Old password is incorrect.");
            return new RedirectView("/user/settings/change-password");
        }
        Utils.flashErrors(result, ra);
        return new RedirectView("/user/settings/change-password");
    }

    @GetMapping("/settings/change-email")
    @FreshLoginRequired
    public String changeEmailRequestPage(Model model) {
        model.addAttribute("form", new ChangeEmailForm());
        return "user/settings/change_email";
    }

    @PostMapping("/settings/change-email")
    @FreshLoginRequired
    public RedirectView changeEmailRequest(@ModelAttribute("form") ChangeEmailForm form, BindingResult result,
                                           HttpServletRequest request, RedirectAttributes ra) {
        User current = currentUserService.get();
        if (!result.hasErrors() && users.findByEmailIgnoreCase(form.getEmail()).isPresent()) {
            result.rejectValue("email", "", "The email is already in use.");
        }
        if (result.hasErrors()) {
            Utils.flashErrors(result, ra);
            return new RedirectView("/user/settings/change-email");
        }
        String token = utils.generateToken(current, MomentsSettings.Operation.CHANGE_EMAIL, Duration.ofHours(1),
                Map.of("new_email", form.getEmail().toLowerCase()));
        String changeEmailUrl = baseUrl(request) + "/user/change-email/" + token;
        mail.sendChangeEmailEmail(current, token, form.getEmail(), changeEmailUrl);
        Utils.flash(ra, "info", "Confirmation email sent, please check your inbox.");
        return new RedirectView("/user/" + current.getUsername());
    }

    @GetMapping("/change-email/{token}")
    @LoginRequired
    public RedirectView changeEmail(@PathVariable String token, HttpServletRequest request,
                                    RedirectAttributes ra) {
        User current = currentUserService.get();
        Map<String, Object> payload = utils.parseToken(current, token, MomentsSettings.Operation.CHANGE_EMAIL);
        String newEmail = (String) payload.get("new_email");
        if (!payload.isEmpty() && newEmail != null) {
            current.setEmail(newEmail);
            users.save(current);
            Utils.flash(ra, "success", "Email updated.");
            return new RedirectView("/user/" + current.getUsername());
        }
        Utils.flash(ra, "warning", "Invalid or expired token.");
        return new RedirectView("/user/settings/change-email");
    }

    @GetMapping("/settings/notification")
    @LoginRequired
    public String notificationSettingPage(Model model) {
        User current = currentUserService.get();
        NotificationSettingForm form = new NotificationSettingForm();
        form.setReceiveCollectNotification(current.isReceiveCollectNotification());
        form.setReceiveCommentNotification(current.isReceiveCommentNotification());
        form.setReceiveFollowNotification(current.isReceiveFollowNotification());
        model.addAttribute("form", form);
        return "user/settings/edit_notification";
    }

    @PostMapping("/settings/notification")
    @LoginRequired
    public RedirectView notificationSetting(@ModelAttribute("form") NotificationSettingForm form,
                                            RedirectAttributes ra) {
        User current = currentUserService.get();
        current.setReceiveCollectNotification(form.isReceiveCollectNotification());
        current.setReceiveCommentNotification(form.isReceiveCommentNotification());
        current.setReceiveFollowNotification(form.isReceiveFollowNotification());
        users.save(current);
        Utils.flash(ra, "success", "Notification settings updated.");
        return new RedirectView("/user/" + current.getUsername());
    }

    @GetMapping("/settings/privacy")
    @LoginRequired
    public String privacySettingPage(Model model) {
        PrivacySettingForm form = new PrivacySettingForm();
        form.setPublicCollections(currentUserService.get().isPublicCollections());
        model.addAttribute("form", form);
        return "user/settings/edit_privacy";
    }

    @PostMapping("/settings/privacy")
    @LoginRequired
    public RedirectView privacySetting(@ModelAttribute("form") PrivacySettingForm form, RedirectAttributes ra) {
        User current = currentUserService.get();
        current.setPublicCollections(form.isPublicCollections());
        users.save(current);
        Utils.flash(ra, "success", "Privacy settings updated.");
        return new RedirectView("/user/" + current.getUsername());
    }

    @GetMapping("/settings/account/delete")
    @FreshLoginRequired
    public String deleteAccountPage(Model model) {
        model.addAttribute("form", new DeleteAccountForm());
        return "user/settings/delete_account";
    }

    @PostMapping("/settings/account/delete")
    @FreshLoginRequired
    public RedirectView deleteAccount(@ModelAttribute("form") DeleteAccountForm form, BindingResult result,
                                      HttpServletRequest request, HttpServletResponse response,
                                      RedirectAttributes ra) {
        User current = currentUserService.get();
        if (!result.hasErrors() && !form.getUsername().equals(current.getUsername())) {
            result.rejectValue("username", "", "Wrong username.");
        }
        if (result.hasErrors()) {
            Utils.flashErrors(result, ra);
            return new RedirectView("/user/settings/account/delete");
        }
        userService.delete(current);
        new org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler()
                .logout(request, response, null);
        Utils.flash(ra, "success", "Your are free, goodbye!");
        return new RedirectView("/");
    }

    private String baseUrl(HttpServletRequest request) {
        String url = request.getRequestURL().toString();
        String context = request.getContextPath();
        int idx = url.indexOf(context.isEmpty() ? "" : context);
        return idx >= 0 ? url.substring(0, idx + context.length()) : url;
    }
}
