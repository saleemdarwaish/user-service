package com.github.userservice.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.github.userservice.data.models.User;
import com.github.userservice.data.repositories.UserRepository;
import com.github.userservice.helpers.StubBuilder;
import com.github.userservice.views.UserRequest;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import java.util.Map;
import java.util.Optional;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EntityMapper entityMapper;

    @Mock
    private ViewMapper viewMapper;

    @Mock
    private ConstraintsValidator constraintsValidator;

    @InjectMocks
    private UserService userService;

    private Long userId = StubBuilder.randomId();

    @Test
    public void getUser_shouldReturnAUser() {
        // GIVEN
        User user = StubBuilder.user();
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // WHEN
        User actual = userService.getUser(userId);

        // THEN
        assertThat(actual).isEqualTo(user);
    }

    @Test
    public void getUser_shouldThrowException_whenUserIsNotFoundById() {
        // GIVEN
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // WHEN
        Throwable actual = catchThrowable(() -> userService.getUser(userId));

        // THEN
        assertThat(actual).isInstanceOf(EntityNotFoundException.class)
                .withFailMessage("Could not find user by id: " + userId);
    }

    @Test
    public void createUser_shouldPersistUserInDb() {
        // GIVEN
        UserRequest request = StubBuilder.userRequest();
        User user = StubBuilder.user();

        willDoNothing().given(constraintsValidator).validate(request);
        given(userRepository.save(any(User.class))).willReturn(user);
        given(viewMapper.map(request, User.class)).willReturn(user);

        // WHEN
        userService.createUser(request);

        // THEN
        verify(constraintsValidator).validate(request);
        verify(userRepository).save(user);
        verify(userRepository).findByEmail(request.getEmail());
    }

    @Test
    public void createUser_shouldThrowException_whenUserWithEMailAlreadyExists() {
        // GIVEN
        UserRequest request = StubBuilder.userRequest();
        User user = StubBuilder.user();

        willDoNothing().given(constraintsValidator).validate(request);
        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.of(user));

        // WHEN
        Throwable actual = catchThrowable(() -> userService.createUser(request));

        // THEN
        assertThat(actual).isInstanceOf(EntityExistsException.class)
                .withFailMessage("User with email address " + request.getEmail() + " already exists");
        verify(constraintsValidator).validate(request);
        verify(userRepository, never()).save(user);
    }

    @Test
    public void updateUser_shouldPartiallyUpdateUser() {
        // GIVEN
        User user = StubBuilder.user();
        user.setId(userId);
        UserRequest userRequest = StubBuilder.userRequest();
        Map<String, Object> request = ImmutableMap.of("email", "newEmail@test.co.uk");

        willDoNothing().given(constraintsValidator).validate(UserRequest.class, request);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(viewMapper.map(user, UserRequest.class)).willReturn(userRequest);
        given(entityMapper.mergeFieldsWithEntity(UserRequest.class, userRequest, request)).willReturn(userRequest);
        given(viewMapper.map(userRequest, User.class)).willReturn(user);

        // WHEN
        userService.updateUser(userId, request);

        // THEN
        verify(constraintsValidator).validate(UserRequest.class, request);
        verify(userRepository).save(user);
        verify(entityMapper).mergeFieldsWithEntity(UserRequest.class, userRequest, request);
    }

    @Test
    public void deleteUser_shouldDeleteTheUser() {
        // GIVEN
        User user = StubBuilder.user();
        user.setId(userId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // WHEN
        userService.deleteUser(userId);

        // THEN
        verify(userRepository).findById(userId);
        verify(userRepository).deleteById(userId);
    }

    @Test
    public void deleteUser_shouldThrowException_whenUserIsNotFoundById() {
        // GIVEN
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // WHEN
        Throwable actual = catchThrowable(() -> userService.deleteUser(userId));

        // THEN
        assertThat(actual).isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Could not find user by id: " + userId);
        verify(userRepository).findById(userId);
        verify(userRepository, never()).deleteById(userId);
    }

}
