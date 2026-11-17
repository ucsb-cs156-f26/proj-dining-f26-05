package edu.ucsb.cs156.dining.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.ucsb.cs156.dining.entities.Admin;
import edu.ucsb.cs156.dining.entities.Moderator;
import edu.ucsb.cs156.dining.entities.User;
import edu.ucsb.cs156.dining.models.UserDTO;
import edu.ucsb.cs156.dining.repositories.AdminRepository;
import edu.ucsb.cs156.dining.repositories.ModeratorRepository;
import edu.ucsb.cs156.dining.repositories.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class UserDataDTOServiceTests {

  private UserRepository userRepository;
  private AdminRepository adminRepository;
  private ModeratorRepository moderatorRepository;
  private UserDataDTOService service;

  @BeforeEach
  public void setUp() {
    userRepository = mock(UserRepository.class);
    adminRepository = mock(AdminRepository.class);
    moderatorRepository = mock(ModeratorRepository.class);
    service =
        new UserDataDTOService(
            userRepository, adminRepository, moderatorRepository, List.of("envadmin@ucsb.edu"));
  }

  @Test
  public void sets_admin_and_moderator_flags_for_each_user() {
    User envAdmin = User.builder().id(1L).email("envadmin@ucsb.edu").build();
    User tableAdmin = User.builder().id(2L).email("tableadmin@ucsb.edu").build();
    User moderator = User.builder().id(3L).email("mod@ucsb.edu").build();
    User both = User.builder().id(4L).email("both@ucsb.edu").build();
    User plain = User.builder().id(5L).email("plain@ucsb.edu").build();

    Pageable pageable = PageRequest.of(0, 10);
    when(userRepository.findAll(eq(pageable)))
        .thenReturn(
            new PageImpl<>(List.of(envAdmin, tableAdmin, moderator, both, plain), pageable, 5));
    when(adminRepository.findAll())
        .thenReturn(List.of(new Admin("tableadmin@ucsb.edu"), new Admin("both@ucsb.edu")));
    when(moderatorRepository.findAll())
        .thenReturn(List.of(new Moderator("mod@ucsb.edu"), new Moderator("both@ucsb.edu")));

    Page<UserDTO> result = service.getUserDataDTOs(pageable);

    assertEquals(
        List.of(
            new UserDTO(envAdmin, true, false),
            new UserDTO(tableAdmin, true, false),
            new UserDTO(moderator, false, true),
            new UserDTO(both, true, true),
            new UserDTO(plain, false, false)),
        result.getContent());
    verify(userRepository).findAll(eq(pageable));
  }

  @Test
  public void keeps_page_number_size_sort_and_total_from_repository() {
    User u3 = User.builder().id(3L).email("u3@ucsb.edu").build();
    User u4 = User.builder().id(4L).email("u4@ucsb.edu").build();

    Pageable pageable = PageRequest.of(1, 2, Sort.by("id"));
    when(userRepository.findAll(eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(u3, u4), pageable, 7));
    when(adminRepository.findAll()).thenReturn(List.of());
    when(moderatorRepository.findAll()).thenReturn(List.of());

    Page<UserDTO> result = service.getUserDataDTOs(pageable);

    assertEquals(
        List.of(new UserDTO(u3, false, false), new UserDTO(u4, false, false)), result.getContent());
    assertEquals(1, result.getNumber());
    assertEquals(2, result.getSize());
    assertEquals(Sort.by("id"), result.getSort());
    assertEquals(7, result.getTotalElements());
    assertEquals(4, result.getTotalPages());
  }
}
