package org.orange.oie.internship2025.conferenceroombooking.service.impl;

import org.orange.oie.internship2025.conferenceroombooking.dto.UserResponse;
import org.orange.oie.internship2025.conferenceroombooking.entity.User;
import org.orange.oie.internship2025.conferenceroombooking.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class  implements UserDetailsService {
  
    private final UserRepository userRepository;

    public UserDetailsServiceImplementation(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("email is not found: " + username));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles("USER").build();
    }
}
