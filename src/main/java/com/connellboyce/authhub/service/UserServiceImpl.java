package com.connellboyce.authhub.service;

import com.connellboyce.authhub.model.dao.CBUser;
import com.connellboyce.authhub.repository.UserRepository;
import com.connellboyce.authhub.util.CBRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	public UserDetails createUser(String username, String password, String email, String firstName, String lastName) {
		CBUser newUser = new CBUser(
				String.valueOf(UUID.randomUUID()),
				username,
				passwordEncoder.encode(password),
				Set.of(CBRole.ROLE_USER.withoutPrefix()),
				email,
				firstName,
				lastName
		);

		userRepository.save(newUser);

		return new User(newUser.getUsername(), newUser.getPassword(), newUser.getRoles().stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList()));

	}

	public CBUser getCBUserByUsername(String username) {
		return userRepository.findByUsername(username).orElse(null);
	}
}
