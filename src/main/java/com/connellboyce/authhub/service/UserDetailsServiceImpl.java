package com.connellboyce.authhub.service;

import com.connellboyce.authhub.model.dao.CBUser;
import com.connellboyce.authhub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
	@Autowired
	private UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		CBUser foundUser = userRepository.findByUsername(username).orElse(null);
		if (foundUser == null) {
			throw new UsernameNotFoundException("User not found");
		}

		return new User(foundUser.getUsername(), foundUser.getPassword(), foundUser.getRoles().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).collect(Collectors.toList()));
	}
}
