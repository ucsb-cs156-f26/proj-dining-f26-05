package edu.ucsb.cs156.dining.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.ucsb.cs156.dining.entities.User;
import edu.ucsb.cs156.dining.services.wiremock.WiremockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@DataJpaTest
public class UserRepositoryTests {

  @Autowired UserRepository userRepository;

  @MockBean WiremockService wiremockService;

  @Test
  public void saves_user_with_picture_url_longer_than_255_characters() {
    String longPictureUrl =
        "https://lh3.googleusercontent.com/a-/ALV-Uj" + "x".repeat(1500) + "=s96-c";

    userRepository.save(
        User.builder()
            .email("longpicture@ucsb.edu")
            .googleSub("123")
            .pictureUrl(longPictureUrl)
            .build());

    User saved = userRepository.findByEmail("longpicture@ucsb.edu").get();
    assertEquals(longPictureUrl, saved.getPictureUrl());
  }
}
