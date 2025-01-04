package com.pcs.vcms.security;

import org.springframework.security.core.userdetails.UserDetails; // Spring Security 6.1.x
import org.springframework.security.core.GrantedAuthority; // Spring Security 6.1.x
import org.springframework.security.core.authority.SimpleGrantedAuthority; // Spring Security 6.1.x
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Custom implementation of Spring Security's UserDetails interface for the Vessel Call Management System.
 * Provides comprehensive user authentication, authorization, and account management capabilities.
 * Supports role-based access control through flexible authority collection system.
 */
public class UserPrincipal implements UserDetails {
    
    private final String id;
    private final String username;
    private final String password;
    private final String email;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * Creates a new UserPrincipal with complete user details and security configuration.
     *
     * @param id Unique identifier for the user
     * @param username User's login username
     * @param password User's encrypted password
     * @param email User's email address
     * @param enabled Account enabled status
     * @param authorities Collection of granted authorities and roles
     * @throws IllegalArgumentException if required parameters are null or empty
     */
    public UserPrincipal(String id, 
                        String username, 
                        String password, 
                        String email, 
                        boolean enabled,
                        Collection<? extends GrantedAuthority> authorities) {
        // Validate input parameters
        Objects.requireNonNull(id, "User ID cannot be null");
        Objects.requireNonNull(username, "Username cannot be null");
        Objects.requireNonNull(password, "Password cannot be null");
        Objects.requireNonNull(email, "Email cannot be null");
        Objects.requireNonNull(authorities, "Authorities cannot be null");

        if (id.trim().isEmpty()) throw new IllegalArgumentException("User ID cannot be empty");
        if (username.trim().isEmpty()) throw new IllegalArgumentException("Username cannot be empty");
        if (password.trim().isEmpty()) throw new IllegalArgumentException("Password cannot be empty");
        if (email.trim().isEmpty()) throw new IllegalArgumentException("Email cannot be empty");

        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.enabled = enabled;
        this.authorities = Collections.unmodifiableCollection(authorities);
    }

    /**
     * Retrieves the user's unique identifier.
     *
     * @return Unique user identifier used for database references and audit logging
     */
    public String getId() {
        return id;
    }

    /**
     * Retrieves the username used for authentication.
     *
     * @return User's login username
     */
    @Override
    public String getUsername() {
        return username;
    }

    /**
     * Retrieves the user's encrypted password.
     *
     * @return Encrypted password string
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Retrieves the user's email address.
     *
     * @return User's email address for notifications and communication
     */
    public String getEmail() {
        return email;
    }

    /**
     * Retrieves the complete collection of user's granted authorities and roles.
     *
     * @return Immutable collection of user's granted authorities
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /**
     * Checks if the user account is currently enabled.
     *
     * @return true if the account is enabled, false otherwise
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Verifies if the user account has not expired.
     * Current implementation always returns true as account expiration is not implemented.
     *
     * @return true indicating account never expires
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Verifies if the user account is not locked.
     * Current implementation always returns true as account locking is not implemented.
     *
     * @return true indicating account is never locked
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Verifies if the user's credentials have not expired.
     * Current implementation always returns true as credential expiration is not implemented.
     *
     * @return true indicating credentials never expire
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserPrincipal)) return false;
        UserPrincipal that = (UserPrincipal) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "UserPrincipal{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", enabled=" + enabled +
                ", authorities=" + authorities +
                '}';
    }
}