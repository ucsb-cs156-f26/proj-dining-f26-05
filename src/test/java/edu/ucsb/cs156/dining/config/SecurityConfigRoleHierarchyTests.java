package edu.ucsb.cs156.dining.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class SecurityConfigRoleHierarchyTests {

  private final RoleHierarchy roleHierarchy = SecurityConfig.roleHierarchy();

  private Set<String> reachable(String role) {
    return roleHierarchy
        .getReachableGrantedAuthorities(List.of(new SimpleGrantedAuthority(role)))
        .stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toSet());
  }

  @Test
  public void admin_implies_moderator_and_user() {
    assertEquals(Set.of("ROLE_ADMIN", "ROLE_MODERATOR", "ROLE_USER"), reachable("ROLE_ADMIN"));
  }

  @Test
  public void moderator_implies_user_but_not_admin() {
    assertEquals(Set.of("ROLE_MODERATOR", "ROLE_USER"), reachable("ROLE_MODERATOR"));
  }

  @Test
  public void user_implies_nothing_else() {
    assertEquals(Set.of("ROLE_USER"), reachable("ROLE_USER"));
  }

  @Test
  public void member_is_not_part_of_hierarchy() {
    assertEquals(Set.of("ROLE_MEMBER"), reachable("ROLE_MEMBER"));
  }
}
