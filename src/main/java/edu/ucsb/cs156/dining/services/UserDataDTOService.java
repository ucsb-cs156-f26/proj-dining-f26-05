package edu.ucsb.cs156.dining.services;

import edu.ucsb.cs156.dining.entities.Admin;
import edu.ucsb.cs156.dining.entities.Moderator;
import edu.ucsb.cs156.dining.entities.User;
import edu.ucsb.cs156.dining.models.UserDTO;
import edu.ucsb.cs156.dining.repositories.AdminRepository;
import edu.ucsb.cs156.dining.repositories.ModeratorRepository;
import edu.ucsb.cs156.dining.repositories.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Builds pages of {@link UserDTO}s (users plus their admin/moderator flags) for the admin users
 * page.
 *
 * <p>Modeled on UserDataDTOService in proj-frontiers. The admin and moderator emails are loaded
 * once per page instead of being looked up once per user.
 */
@Service
public class UserDataDTOService {
  private final UserRepository userRepository;
  private final AdminRepository adminRepository;
  private final ModeratorRepository moderatorRepository;
  private final List<String> adminEmails;

  public UserDataDTOService(
      UserRepository userRepository,
      AdminRepository adminRepository,
      ModeratorRepository moderatorRepository,
      @Value("#{'${app.admin.emails}'.split(',')}") List<String> adminEmails) {
    this.userRepository = userRepository;
    this.adminRepository = adminRepository;
    this.moderatorRepository = moderatorRepository;
    this.adminEmails = adminEmails;
  }

  /**
   * Returns one page of users, each with its admin and moderator flags.
   *
   * <p>A user is an admin if their email is in ADMIN_EMAILS or in the admins table, and a moderator
   * if their email is in the moderators table (the same rules as GET /api/admin/users).
   *
   * @param pageable which page to return, its size, and its sort order
   * @return the requested page of users
   */
  public Page<UserDTO> getUserDataDTOs(Pageable pageable) {
    Page<User> users = userRepository.findAll(pageable);

    Set<String> admins = new HashSet<>(adminEmails);
    for (Admin admin : adminRepository.findAll()) {
      admins.add(admin.getEmail());
    }
    Set<String> moderators = new HashSet<>();
    for (Moderator moderator : moderatorRepository.findAll()) {
      moderators.add(moderator.getEmail());
    }

    List<UserDTO> userDTOs =
        users.getContent().stream()
            .map(
                user ->
                    new UserDTO(
                        user,
                        admins.contains(user.getEmail()),
                        moderators.contains(user.getEmail())))
            .toList();
    return new PageImpl<>(userDTOs, pageable, users.getTotalElements());
  }
}
