package com.selimhorri.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.selimhorri.app.domain.Credential;
import com.selimhorri.app.domain.RoleBasedAuthority;
import com.selimhorri.app.domain.User;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.UserObjectNotFoundException;
import com.selimhorri.app.repository.UserRepository;
import com.selimhorri.app.service.impl.UserServiceImpl;

/**
 * Pruebas unitarias para UserServiceImpl
 * Valida componentes individuales del servicio de usuario
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserDto testUserDto;
    private Credential testCredential;
    private CredentialDto testCredentialDto;

    @BeforeEach
    void setUp() {
        // Setup test credential
        testCredential = Credential.builder()
                .credentialId(1)
                .username("nramirez")
                .password("password123")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();

        testCredentialDto = CredentialDto.builder()
                .credentialId(1)
                .username("nramirez")
                .password("password123")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();

        // Setup test user
        testUser = User.builder()
                .userId(1)
                .firstName("Natalia")
                .lastName("Ramirez")
                .email("natalia.ramirez@example.com")
                .phone("3001234567")
                .imageUrl("http://example.com/natalia-avatar.jpg")
                .credential(testCredential)
                .build();

        testUserDto = UserDto.builder()
                .userId(1)
                .firstName("Natalia")
                .lastName("Ramirez")
                .email("natalia.ramirez@example.com")
                .phone("3001234567")
                .imageUrl("http://example.com/natalia-avatar.jpg")
                .credentialDto(testCredentialDto)
                .build();
    }

    /**
     * Prueba 1: Verificar que findAll() retorna lista de usuarios correctamente
     */
    @Test
    void testFindAll_ShouldReturnAllUsers() {
        // Given
        User user2 = User.builder()
                .userId(2)
                .firstName("Carlos")
                .lastName("Mendoza")
                .email("carlos.mendoza@example.com")
                .phone("3109876543")
                .credential(testCredential)
                .build();

        List<User> users = Arrays.asList(testUser, user2);
        when(userRepository.findAll()).thenReturn(users);

        // When
        List<UserDto> result = userService.findAll();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Natalia", result.get(0).getFirstName());
        assertEquals("Carlos", result.get(1).getFirstName());
        verify(userRepository, times(1)).findAll();
    }

    /**
     * Prueba 2: Verificar que findById() retorna usuario correcto cuando existe
     */
    @Test
    void testFindById_WhenUserExists_ShouldReturnUser() {
        // Given
        Integer userId = 1;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When
        UserDto result = userService.findById(userId);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals("Natalia", result.getFirstName());
        assertEquals("Ramirez", result.getLastName());
        assertEquals("natalia.ramirez@example.com", result.getEmail());
        verify(userRepository, times(1)).findById(userId);
    }

    /**
     * Prueba 3: Verificar que findById() lanza excepción cuando usuario no existe
     */
    @Test
    void testFindById_WhenUserNotExists_ShouldThrowException() {
        // Given
        Integer userId = 999;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        UserObjectNotFoundException exception = assertThrows(
                UserObjectNotFoundException.class, 
                () -> userService.findById(userId)
        );
        
        assertTrue(exception.getMessage().contains("User with id: 999 not found"));
        verify(userRepository, times(1)).findById(userId);
    }

    /**
     * Prueba 4: Verificar que save() guarda usuario correctamente
     */
    @Test
    void testSave_ShouldSaveUserSuccessfully() {
        // Given
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDto result = userService.save(testUserDto);

        // Then
        assertNotNull(result);
        assertEquals("Natalia", result.getFirstName());
        assertEquals("Ramirez", result.getLastName());
        assertEquals("natalia.ramirez@example.com", result.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    /**
     * Prueba 5: Verificar que findByUsername() retorna usuario correcto
     */
    @Test
    void testFindByUsername_WhenUserExists_ShouldReturnUser() {
        // Given
        String username = "nramirez";
        when(userRepository.findByCredentialUsername(username)).thenReturn(Optional.of(testUser));

        // When
        UserDto result = userService.findByUsername(username);

        // Then
        assertNotNull(result);
        assertEquals("Natalia", result.getFirstName());
        assertEquals("nramirez", result.getCredentialDto().getUsername());
        verify(userRepository, times(1)).findByCredentialUsername(username);
    }

    /**
     * Prueba 6: Verificar que findByUsername() lanza excepción cuando usuario no existe
     */
    @Test
    void testFindByUsername_WhenUserNotExists_ShouldThrowException() {
        // Given
        String username = "usuarioinexistente";
        when(userRepository.findByCredentialUsername(username)).thenReturn(Optional.empty());

        // When & Then
        UserObjectNotFoundException exception = assertThrows(
                UserObjectNotFoundException.class,
                () -> userService.findByUsername(username)
        );

        assertTrue(exception.getMessage().contains("User with username: usuarioinexistente not found"));
        verify(userRepository, times(1)).findByCredentialUsername(username);
    }

    /**
     * Prueba 7: Verificar que deleteById() elimina usuario correctamente
     */
    @Test
    void testDeleteById_ShouldDeleteUserSuccessfully() {
        // Given
        Integer userId = 1;
        doNothing().when(userRepository).deleteById(userId);

        // When
        assertDoesNotThrow(() -> userService.deleteById(userId));

        // Then
        verify(userRepository, times(1)).deleteById(userId);
    }
} 